package com.library.web.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.library.web.model.Book;
import com.library.web.model.BookOrder;
import com.library.web.model.OrderStatus;
import com.library.web.model.OrderType;
import com.library.web.repository.BookRepository;
import com.library.web.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Everything a customer can do: look at the shop, fill a cart, pay, return, cancel. */
@Service
public class ShopService {

    public static final int RENTAL_DAYS = 14;

    /** Charged for every day, per copy, a rental is handed back late. */
    public static final BigDecimal LATE_FEE_PER_DAY = new BigDecimal("0.50");

    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;

    public ShopService(BookRepository bookRepository, OrderRepository orderRepository) {
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
    }

    /** The outcome of a shop action, shown to the customer as a banner. */
    public record ActionResult(boolean success, String message) {

        static ActionResult ok(String message) {
            return new ActionResult(true, message);
        }

        static ActionResult fail(String message) {
            return new ActionResult(false, message);
        }
    }

    /**
     * A live rental listed on the cart page, with how late it is and what that costs.
     * needsPayment() decides whether the page shows a plain Return or a Pay-and-return button.
     */
    public record RentalRow(BookOrder order, long daysOverdue, BigDecimal lateFee) {

        public boolean needsPayment() {
            return lateFee.signum() > 0;
        }
    }

    @Transactional(readOnly = true)
    public List<CartItem> describe(Cart cart) {
        List<CartItem> items = new ArrayList<>();
        for (var entry : cart.lines().entrySet()) {
            Book book = bookRepository.findById(entry.getKey()).orElse(null);
            if (book == null) {
                continue;
            }
            Cart.Line line = entry.getValue();
            BigDecimal unit = priceFor(book, line.type());
            BigDecimal total = unit.multiply(BigDecimal.valueOf(line.quantity()));
            items.add(new CartItem(book, line.type(), line.quantity(), unit, total, book.getStockQuantity()));
        }
        return items;
    }

    public BigDecimal totalOf(List<CartItem> items) {
        return items.stream()
                .map(CartItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Turn every cart line into an order and take the copies out of stock.
     * The whole thing happens in one transaction: if any line is not possible,
     * nothing is saved and the cart stays as it was.
     */
    @Transactional
    public ActionResult checkout(String username, Cart cart) {
        if (cart.isEmpty()) {
            return ActionResult.fail("Your cart is empty.");
        }

        List<CartItem> items = describe(cart);
        for (CartItem item : items) {
            if (item.moreThanStock()) {
                return ActionResult.fail("Only " + item.availableStock()
                        + " copy/copies of \"" + item.book().getTitle() + "\" left on the shelf.");
            }
            if (item.type() == OrderType.RENT && orderRepository.existsByBookIdAndUsernameAndStatus(
                    item.book().getId(), username, OrderStatus.PAID)) {
                return ActionResult.fail("You are already renting \"" + item.book().getTitle()
                        + "\". Return it before renting it again.");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<BookOrder> orders = new ArrayList<>();

        for (CartItem item : items) {
            BookOrder order = new BookOrder();
            order.setUsername(username);
            order.setBook(item.book());
            order.setType(item.type());
            order.setStatus(OrderStatus.PAID);
            order.setQuantity(item.quantity());
            order.setUnitPrice(item.unitPrice());
            order.setTotalAmount(item.lineTotal().setScale(2, RoundingMode.HALF_UP));
            order.setOrderDate(now);
            if (item.type() == OrderType.RENT) {
                order.setDueDate(LocalDate.now().plusDays(RENTAL_DAYS));
            }
            orders.add(order);

            item.book().setStockQuantity(item.book().getStockQuantity() - item.quantity());
        }

        orderRepository.saveAll(orders);
        cart.clear();

        return ActionResult.ok("Payment of $" + money(totalOf(items))
                + " received. " + orders.size() + " order line(s) confirmed.");
    }

    @Transactional
    public ActionResult returnOrder(String username, Long orderId) {
        BookOrder order = ownedOrder(username, orderId);
        if (order == null) {
            return ActionResult.fail("That order is not yours.");
        }
        if (!order.isLiveRental()) {
            return ActionResult.fail("Only a confirmed rental can be returned.");
        }

        order.setStatus(OrderStatus.RETURNED);
        order.setReturnedAt(LocalDateTime.now());
        order.setLateFee(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        restock(order.getBook(), order.getQuantity());

        return ActionResult.ok("\"" + order.getBook().getTitle() + "\" is returned. Thank you!");
    }

    /** How many days past its due date this rental is (0 when it is on time or already back). */
    public long daysOverdue(BookOrder order) {
        if (!order.isOverdue()) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(order.getDueDate(), LocalDate.now());
    }

    /** The fee owed for handing this rental back today: per day, per copy. */
    public BigDecimal lateFeeFor(BookOrder order) {
        long days = daysOverdue(order);
        if (days <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return LATE_FEE_PER_DAY
                .multiply(BigDecimal.valueOf(days))
                .multiply(BigDecimal.valueOf(Math.max(order.getQuantity(), 1)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Every live rental this reader holds, ready to show on the cart page. */
    @Transactional(readOnly = true)
    public List<RentalRow> liveRentals(String username) {
        return orderRepository.findByUsernameAndStatus(username, OrderStatus.PAID).stream()
                .filter(BookOrder::isLiveRental)
                .map(order -> new RentalRow(order, daysOverdue(order), lateFeeFor(order)))
                .toList();
    }

    /**
     * Hand a rental back once its late fee has been paid through the gateway. Same effect as a
     * free return, but the amount actually charged is remembered on the order.
     */
    @Transactional
    public ActionResult returnWithLateFee(String username, Long orderId) {
        BookOrder order = ownedOrder(username, orderId);
        if (order == null) {
            return ActionResult.fail("That order is not yours.");
        }
        if (!order.isLiveRental()) {
            return ActionResult.fail("Only a confirmed rental can be returned.");
        }

        BigDecimal fee = lateFeeFor(order);
        order.setStatus(OrderStatus.RETURNED);
        order.setReturnedAt(LocalDateTime.now());
        order.setLateFee(fee);
        restock(order.getBook(), order.getQuantity());

        return ActionResult.ok("\"" + order.getBook().getTitle() + "\" is returned."
                + (fee.signum() > 0 ? " Late fee of $" + money(fee) + " received." : ""));
    }

    @Transactional
    public ActionResult cancelOrder(String username, Long orderId) {
        BookOrder order = ownedOrder(username, orderId);
        if (order == null) {
            return ActionResult.fail("That order is not yours.");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            return ActionResult.fail("Only a confirmed order can be cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        restock(order.getBook(), order.getQuantity());

        return ActionResult.ok("Order #" + order.getId() + " cancelled and put back on the shelf.");
    }

    private BookOrder ownedOrder(String username, Long orderId) {
        return orderRepository.findById(orderId)
                .filter(order -> order.getUsername().equals(username))
                .orElse(null);
    }

    private void restock(Book book, int quantity) {
        book.setStockQuantity(book.getStockQuantity() + quantity);
        bookRepository.save(book);
    }

    private BigDecimal priceFor(Book book, OrderType type) {
        BigDecimal unit = type == OrderType.BUY ? book.getPrice() : book.getRentalPrice();
        return unit == null ? BigDecimal.ZERO : unit.setScale(2, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
