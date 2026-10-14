package com.library.web.controller;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.library.web.model.Book;
import com.library.web.model.BookOrder;
import com.library.web.model.BookReview;
import com.library.web.model.Genre;
import com.library.web.model.OrderStatus;
import com.library.web.model.OrderType;
import com.library.web.model.SupportMessage;
import com.library.web.repository.BookRepository;
import com.library.web.repository.OrderRepository;
import com.library.web.repository.SupportMessageRepository;
import com.library.web.service.Cart;
import com.library.web.service.RazorpayService;
import com.library.web.service.ReviewService;
import com.library.web.service.ShopService;
import com.library.web.service.SupportBot;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shop")
public class ShopController {

    public static final String CART_SESSION_KEY = "cart";

    private static final int MAX_PER_LINE = 10;

    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final ShopService shopService;
    private final ReviewService reviewService;
    private final RazorpayService razorpayService;
    private final SupportMessageRepository supportRepository;
    private final SupportBot supportBot;

    public ShopController(BookRepository bookRepository, OrderRepository orderRepository,
                          ShopService shopService, ReviewService reviewService,
                          RazorpayService razorpayService, SupportMessageRepository supportRepository,
                          SupportBot supportBot) {
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.shopService = shopService;
        this.reviewService = reviewService;
        this.razorpayService = razorpayService;
        this.supportRepository = supportRepository;
        this.supportBot = supportBot;
    }

    /** One section of the shop: its heading plus the books sitting on that shelf. */
    public record Shelf(String label, List<Book> books) {
    }

    /**
     * The other language editions of the same story that the shop stocks. An empty
     * Editions means this book is a one-off: nothing to suggest beside it.
     * The methods a page calls have to be public, or Thymeleaf cannot see them.
     */
    public record Editions(List<Book> others) {

        public static final Editions NONE = new Editions(List.of());

        public boolean isMultilingual() {
            return !others.isEmpty();
        }

        /** This edition plus its siblings, counted as separate languages. */
        public int languageCount() {
            return others.size() + 1;
        }

        /** "Tamil, French" — the languages on offer other than the one being read. */
        public String otherLanguages() {
            return others.stream().map(ShopController::languageLabel).collect(Collectors.joining(", "));
        }

        /** Siblings a customer could actually take home today. */
        public List<Book> inStockOthers() {
            return others.stream().filter(Book::isInStock).toList();
        }

        public boolean hasStockElsewhere() {
            return !inStockOthers().isEmpty();
        }
    }

    @GetMapping
    public String browse(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String genre,
                         @RequestParam(required = false) String author,
                         @RequestParam(required = false) String language,
                         @RequestParam(required = false) String by,
                         Model model) {
        List<Book> found = (q == null || q.isBlank())
                ? bookRepository.findByArrivalDateIsNull()
                : bookRepository.searchArrived(q.trim());

        String mode = groupMode(by);
        Genre pickedGenre = "genre".equals(mode) ? Genre.fromSlug(genre) : null;
        String pickedAuthor = "author".equals(mode) ? blankToNull(author) : null;
        String pickedLanguage = "language".equals(mode) ? blankToNull(language) : null;

        List<Shelf> shelves = shelvesFor(found, mode, pickedGenre, pickedAuthor, pickedLanguage);
        List<Book> shown = shelves.stream().flatMap(shelf -> shelf.books().stream()).toList();

        model.addAttribute("shelves", shelves);
        model.addAttribute("opinions", reviewService.opinionsFor(shown));
        model.addAttribute("editions", editionsByBook(stockEditions()));
        model.addAttribute("groupBy", mode);

        model.addAttribute("authors", labels(ShopController::authorLabel));
        model.addAttribute("languages", labels(ShopController::languageLabel));
        model.addAttribute("activeAuthor", pickedAuthor);
        model.addAttribute("activeLanguage", pickedLanguage);
        model.addAttribute("genres", Genre.values());
        model.addAttribute("activeGenre", pickedGenre);
        model.addAttribute("shownBooks", shelves.stream().mapToLong(shelf -> shelf.books().size()).sum());
        model.addAttribute("q", q);
        model.addAttribute("totalBooks", bookRepository.countByArrivalDateIsNull());
        return "shop";
    }

    /** Which headings to group under: "author", "language", or "genre" for anything else. */
    private static String groupMode(String by) {
        return switch (by == null ? "" : by.toLowerCase(Locale.ROOT)) {
            case "author" -> "author";
            case "language" -> "language";
            default -> "genre";
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Groups books under a heading, alphabetically, so the shop reads as a row of
     * shelves instead of one long list. Passing null for "onlyGenre", "onlyAuthor"
     * or "onlyLanguage" keeps every shelf.
     */
    private List<Shelf> shelvesFor(List<Book> books, String mode, Genre onlyGenre,
                                   String onlyAuthor, String onlyLanguage) {
        Comparator<Shelf> alphabetical = Comparator.comparing(Shelf::label, String.CASE_INSENSITIVE_ORDER);

        if ("genre".equals(mode)) {
            return books.stream()
                    .filter(book -> onlyGenre == null || book.getGenre() == onlyGenre)
                    .collect(Collectors.groupingBy(Book::getGenre))
                    .entrySet().stream()
                    .map(entry -> new Shelf(entry.getKey().getLabel(), sortedByTitle(entry.getValue())))
                    .sorted(alphabetical)
                    .toList();
        }

        // Author and language shelves are plain text, so they share one code path and
        // only differ in which piece of the book is used as the heading.
        Function<Book, String> key = "author".equals(mode)
                ? ShopController::authorLabel
                : ShopController::languageLabel;
        String only = "author".equals(mode) ? onlyAuthor : onlyLanguage;

        return books.stream()
                .filter(book -> only == null || only.equalsIgnoreCase(key.apply(book)))
                .collect(Collectors.groupingBy(key))
                .entrySet().stream()
                .map(entry -> new Shelf(entry.getKey(), sortedByTitle(entry.getValue())))
                .sorted(alphabetical)
                .toList();
    }

    /** Every distinct heading for a grouping, A to Z, for the dropdown beside Search. */
    private List<String> labels(Function<Book, String> key) {
        return bookRepository.findByArrivalDateIsNull().stream()
                .map(key)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static String authorLabel(Book book) {
        String author = book.getAuthor();
        return author == null || author.isBlank() ? "Unknown author" : author.trim();
    }

    /** Older rows have no language yet, and every one of them is an English edition. */
    private static String languageLabel(Book book) {
        String language = book.getLanguage();
        return language == null || language.isBlank() ? "English" : language.trim();
    }

    /**
     * Maps every book id to the other language editions of the same story, so a shelf
     * can say "also in French". Books with no work key, and stories we only stock in one
     * language, are left out of the map entirely.
     */
    private Map<Long, Editions> editionsByBook(List<Book> onShelf) {
        Map<Long, Editions> indexed = new HashMap<>();

        onShelf.stream()
                .filter(book -> blankToNull(book.getWorkKey()) != null)
                .collect(Collectors.groupingBy(book -> blankToNull(book.getWorkKey()).toLowerCase(Locale.ROOT)))
                .values()
                .forEach(group -> {
                    List<Book> editions = distinctLanguages(group);
                    if (editions.size() < 2) {
                        return;
                    }
                    for (Book book : editions) {
                        List<Book> others = editions.stream()
                                .filter(sibling -> !sibling.getId().equals(book.getId()))
                                .sorted(Comparator.comparing(ShopController::languageLabel))
                                .toList();
                        indexed.put(book.getId(), new Editions(others));
                    }
                });

        return indexed;
    }

    /**
     * One row per language. If a story has two English printings on the shelf, the
     * customer is shown the cheapest of them rather than the same word twice.
     */
    private List<Book> distinctLanguages(List<Book> group) {
        return group.stream()
                .sorted(Comparator.comparing(Book::getPrice))
                .collect(Collectors.toMap(ShopController::languageLabel, book -> book, (cheapest, other) -> cheapest,
                        LinkedHashMap::new))
                .values().stream()
                .toList();
    }

    /** Everything the shop is stocking today, for the edition lookups. */
    private List<Book> stockEditions() {
        return bookRepository.findByArrivalDateIsNull();
    }

    private static List<Book> sortedByTitle(List<Book> books) {
        return books.stream().sorted(Comparator.comparing(Book::getTitle)).toList();
    }

    @GetMapping("/book/{id}")
    public String bookDetail(@PathVariable Long id, Authentication me, Model model) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid book id: " + id));

        model.addAttribute("book", book);
        model.addAttribute("editions", editionsByBook(stockEditions())
                .getOrDefault(book.getId(), Editions.NONE));
        model.addAttribute("rentedAlready", hasLiveRental(username(me), id));
        model.addAttribute("reviews", reviewService.reviewsFor(id));
        model.addAttribute("opinion", reviewService.opinionFor(id));
        model.addAttribute("maxRating", BookReview.MAX_RATING);
        return "shop-book";
    }

    @PostMapping("/book/{id}/review")
    public String addReview(@PathVariable Long id,
                            @RequestParam(required = false) Integer rating,
                            @RequestParam(required = false) String comment,
                            Authentication me,
                            RedirectAttributes flash) {
        if (rating == null) {
            flash.addFlashAttribute("shopError", "Pick a star rating before posting.");
            return "redirect:/shop/book/" + id;
        }
        reviewService.addReview(id, username(me), rating, comment);
        flash.addFlashAttribute("shopMessage", "Thanks — your review is on the page.");
        return "redirect:/shop/book/" + id;
    }

    @PostMapping("/review/{id}/delete")
    public String deleteReview(@PathVariable Long id,
                               @RequestParam Long bookId,
                               Authentication me,
                               RedirectAttributes flash) {
        boolean removed = reviewService.deleteReview(id, username(me));
        if (!removed) {
            flash.addFlashAttribute("shopError", "That review is not yours to remove.");
        } else {
            flash.addFlashAttribute("shopMessage", "Your review is gone.");
        }
        return "redirect:/shop/book/" + bookId;
    }

    /**
     * A starting point for new readers: what other people rated highly, what they
     * liked most, and what they said most recently.
     */
    @GetMapping("/suggestions")
    public String suggestions(Model model) {
        List<Book> onShelf = bookRepository.findByArrivalDateIsNull();

        List<Book> favourites = reviewService.topRated(onShelf, 2, 6);
        List<Book> popular = reviewService.mostLiked(onShelf, 6);
        List<BookReview> recent = reviewService.latestReviews(6);

        Set<Book> pool = new LinkedHashSet<>();
        pool.addAll(favourites);
        pool.addAll(popular);
        recent.forEach(review -> pool.add(review.getBook()));

        model.addAttribute("favourites", favourites);
        model.addAttribute("popular", popular);
        model.addAttribute("recent", recent);
        model.addAttribute("opinions", reviewService.opinionsFor(pool));
        model.addAttribute("ratedCount", favourites.size() + popular.size());
        model.addAttribute("totalBooks", bookRepository.countByArrivalDateIsNull());
        model.addAttribute("inStockBooks", bookRepository.countByArrivalDateIsNullAndStockQuantityGreaterThan(0));
        return "suggestions";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long bookId,
                            @RequestParam String action,
                            @RequestParam(defaultValue = "1") int quantity,
                            HttpSession session,
                            RedirectAttributes flash) {
        Cart cart = cart(session);
        cart.add(bookId, parseType(action), Math.min(Math.max(quantity, 1), MAX_PER_LINE));
        flash.addFlashAttribute("shopMessage", "Added to your cart.");
        return "redirect:/shop/cart";
    }

    @GetMapping("/cart")
    public String viewCart(Authentication me, HttpSession session, Model model) {
        Cart cart = cart(session);
        var items = shopService.describe(cart);

        model.addAttribute("items", items);
        model.addAttribute("grandTotal", shopService.totalOf(items));
        model.addAttribute("empty", items.isEmpty());
        model.addAttribute("rentalDays", ShopService.RENTAL_DAYS);
        model.addAttribute("rentalRows", shopService.liveRentals(username(me)));
        model.addAttribute("lateFeePerDay", ShopService.LATE_FEE_PER_DAY);
        model.addAttribute("keyId", razorpayService.keyId());
        return "cart";
    }

    @PostMapping("/cart/update")
    public String updateLine(@RequestParam Long bookId,
                             @RequestParam int quantity,
                             HttpSession session) {
        cart(session).setQuantity(bookId, Math.min(quantity, MAX_PER_LINE));
        return "redirect:/shop/cart";
    }

    @PostMapping("/cart/remove")
    public String removeLine(@RequestParam Long bookId, HttpSession session, RedirectAttributes flash) {
        cart(session).remove(bookId);
        flash.addFlashAttribute("shopMessage", "Line removed from your cart.");
        return "redirect:/shop/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session, RedirectAttributes flash) {
        cart(session).clear();
        flash.addFlashAttribute("shopMessage", "Cart emptied.");
        return "redirect:/shop/cart";
    }

    @GetMapping("/checkout")
    public String checkoutPage(HttpSession session, Model model) {
        Cart cart = cart(session);
        var items = shopService.describe(cart);

        model.addAttribute("items", items);
        model.addAttribute("grandTotal", shopService.totalOf(items));
        model.addAttribute("empty", items.isEmpty());
        model.addAttribute("rentalDays", ShopService.RENTAL_DAYS);
        model.addAttribute("keyId", razorpayService.keyId());
        return "checkout";
    }

    /** Book the cart total with Razorpay and hand the order id to the browser. */
    @PostMapping(value = "/checkout/create-order", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Object> createOrder(Authentication me, HttpSession session) {
        var items = shopService.describe(cart(session));
        if (items.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("success", false);
            empty.put("message", "Your cart is empty.");
            return empty;
        }
        var result = razorpayService.createOrder(shopService.totalOf(items), username(me));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result.success());
        response.put("orderId", result.orderId());
        response.put("amount", result.amountPaise());
        response.put("message", result.message());
        return response;
    }

    /**
     * The browser only reaches here once Razorpay says "paid". We re-check the signature
     * ourselves before trusting it, and only then take the copies off the shelf.
     */
    @PostMapping(value = "/checkout/verify", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Object> verifyAndPay(Authentication me, HttpSession session,
                                            @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new LinkedHashMap<>();
        boolean genuine = razorpayService.verifySignature(
                payload.get("razorpay_order_id"),
                payload.get("razorpay_payment_id"),
                payload.get("razorpay_signature"));
        if (!genuine) {
            response.put("success", false);
            response.put("message", "We could not confirm that payment with Razorpay.");
            return response;
        }
        ShopService.ActionResult result = shopService.checkout(username(me), cart(session));
        response.put("success", result.success());
        response.put("message", result.message());
        response.put("redirect", result.success() ? "/shop/orders" : "/shop/cart");
        return response;
    }

    @GetMapping("/orders")
    public String myOrders(Authentication me, Model model) {
        List<BookOrder> orders = orderRepository.findByUsernameOrderByOrderDateDesc(username(me));

        model.addAttribute("orders", orders);
        model.addAttribute("rentingCount", orders.stream().filter(BookOrder::isLiveRental).count());
        model.addAttribute("overdueCount", orders.stream().filter(BookOrder::isOverdue).count());
        model.addAttribute("totalSpend", orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .map(BookOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return "my-orders";
    }

    @PostMapping("/orders/{id}/return")
    public String returnRental(@PathVariable Long id, Authentication me, RedirectAttributes flash) {
        flashResult(shopService.returnOrder(username(me), id), flash);
        return "redirect:/shop/orders";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, Authentication me, RedirectAttributes flash) {
        flashResult(shopService.cancelOrder(username(me), id), flash);
        return "redirect:/shop/orders";
    }

    /** Book a late-fee amount with Razorpay for returning one rental. */
    @PostMapping(value = "/rentals/{id}/create-order", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Object> createReturnOrder(@PathVariable Long id, Authentication me) {
        Map<String, Object> response = new LinkedHashMap<>();
        BookOrder order = liveRental(username(me), id);
        if (order == null) {
            response.put("success", false);
            response.put("message", "That rental is not yours, or is already back on the shelf.");
            return response;
        }
        BigDecimal fee = shopService.lateFeeFor(order);
        if (fee.signum() <= 0) {
            response.put("success", false);
            response.put("message", "No late fee is due for that one — just press Return.");
            return response;
        }
        var result = razorpayService.createOrder(fee, "latefee-" + id);
        response.put("success", result.success());
        response.put("orderId", result.orderId());
        response.put("amount", result.amountPaise());
        response.put("message", result.message());
        return response;
    }

    /** Once the fee payment checks out, hand the rental back and record what was paid. */
    @PostMapping(value = "/rentals/{id}/confirm-return", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Object> confirmReturn(@PathVariable Long id, Authentication me,
                                             @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new LinkedHashMap<>();
        boolean genuine = razorpayService.verifySignature(
                payload.get("razorpay_order_id"),
                payload.get("razorpay_payment_id"),
                payload.get("razorpay_signature"));
        if (!genuine) {
            response.put("success", false);
            response.put("message", "We could not confirm that payment with Razorpay.");
            return response;
        }
        ShopService.ActionResult result = shopService.returnWithLateFee(username(me), id);
        response.put("success", result.success());
        response.put("message", result.message());
        response.put("redirect", result.success() ? "/shop/orders" : "/shop/cart");
        return response;
    }

    private void flashResult(ShopService.ActionResult result, RedirectAttributes flash) {
        flash.addFlashAttribute(result.success() ? "shopMessage" : "shopError", result.message());
    }

    private BookOrder liveRental(String username, Long id) {
        return orderRepository.findById(id)
                .filter(order -> order.getUsername().equals(username) && order.isLiveRental())
                .orElse(null);
    }

    private boolean hasLiveRental(String username, Long bookId) {
        return orderRepository.findByUsernameAndStatus(username, OrderStatus.PAID).stream()
                .filter(BookOrder::isLiveRental)
                .anyMatch(order -> order.getBook().getId().equals(bookId));
    }

    /**
     * The support thread bubbles, as a fragment. The floating chat widget fetches this the
     * first time it is opened. It reads like a bot, but a librarian answers from the admin desk.
     */
    @GetMapping("/support/thread")
    public String supportThread(Authentication me, Model model) {
        return renderThread(me, model);
    }

    /**
     * A reader asks something. Blank lines are ignored. The helper answers immediately in
     * its own voice, and the message also waits on the admin desk for a librarian. The
     * fresh thread is returned so the widget updates in place, never a whole page.
     */
    @PostMapping("/support/send")
    public String sendQuery(Authentication me, @RequestParam String body, Model model) {
        String text = body == null ? "" : body.trim();
        if (!text.isEmpty()) {
            supportRepository.save(new SupportMessage(username(me), text, false));
            supportRepository.save(SupportMessage.automatedReply(username(me), supportBot.reply(text)));
        }
        return renderThread(me, model);
    }

    private String renderThread(Authentication me, Model model) {
        model.addAttribute("messages", supportRepository.findByUsernameOrderByCreatedAtAsc(username(me)));
        return "fragments/support :: thread";
    }

    private Cart cart(HttpSession session) {
        Object stored = session.getAttribute(CART_SESSION_KEY);
        if (stored instanceof Cart existing) {
            return existing;
        }
        Cart fresh = new Cart();
        session.setAttribute(CART_SESSION_KEY, fresh);
        return fresh;
    }

    private OrderType parseType(String action) {
        return action != null && action.toUpperCase(Locale.ROOT).equals(OrderType.RENT.name())
                ? OrderType.RENT
                : OrderType.BUY;
    }

    private String username(Authentication me) {
        return me.getName();
    }
}
