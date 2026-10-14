package com.library.web.controller;

import java.util.List;

import com.library.web.model.Book;
import com.library.web.model.Genre;
import com.library.web.repository.BookRepository;
import com.library.web.repository.OrderRepository;
import com.library.web.service.ReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The back office: only an ADMIN can reach these paths (see SecurityConfig). */
@Controller
@RequestMapping("/admin")
public class BookController {

    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final ReviewService reviewService;

    public BookController(BookRepository bookRepository, OrderRepository orderRepository,
                          ReviewService reviewService) {
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.reviewService = reviewService;
    }

    @GetMapping("/books")
    public String listBooks(@RequestParam(required = false) String q,
                            @RequestParam(required = false) String genre,
                            Model model) {
        List<Book> found = (q == null || q.isBlank())
                ? bookRepository.findByArrivalDateIsNull()
                : bookRepository.searchArrived(q.trim());

        Genre picked = Genre.fromSlug(genre);
        List<Book> books = picked == null
                ? found
                : found.stream().filter(book -> book.getGenre() == picked).toList();

        model.addAttribute("books", books);
        model.addAttribute("q", q);
        model.addAttribute("genres", Genre.values());
        model.addAttribute("activeGenre", picked);
        model.addAttribute("totalBooks", bookRepository.countByArrivalDateIsNull());
        model.addAttribute("availableBooks", bookRepository.countByArrivalDateIsNullAndStockQuantityGreaterThan(0));
        model.addAttribute("soldOutBooks", bookRepository.countByArrivalDateIsNullAndStockQuantity(0));
        model.addAttribute("upcomingCount", bookRepository.countByArrivalDateNotNull());
        return "index";
    }

    @GetMapping("/books/upcoming")
    public String upcomingBooks(Model model) {
        model.addAttribute("upcoming", bookRepository.findByArrivalDateNotNullOrderByArrivalDateAsc());
        return "upcoming";
    }

    @GetMapping("/books/new")
    public String newBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("genres", Genre.values());
        return "form";
    }

    @GetMapping("/books/edit/{id}")
    public String editBookForm(@PathVariable Long id, Model model) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid book id: " + id));
        model.addAttribute("book", book);
        model.addAttribute("genres", Genre.values());
        return "form";
    }

    @PostMapping("/books/save")
    public String saveBook(Book book) {
        bookRepository.save(book);
        return "redirect:/admin/books";
    }

    @PostMapping("/books/{id}/arrive")
    public String markAsArrived(@PathVariable Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid book id: " + id));
        book.setArrivalDate(null);
        bookRepository.save(book);
        return "redirect:/admin/books/upcoming";
    }

    @PostMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable Long id, RedirectAttributes flash) {
        long orderCount = orderRepository.countByBookId(id);
        if (orderCount > 0) {
            flash.addFlashAttribute("adminError",
                    "This book has " + orderCount + " order(s) against it, so it stays in the catalog.");
            return "redirect:/admin/books";
        }
        // Reviews and Likes point at the book, so they have to go with it.
        reviewService.clearOpinionsFor(id);
        bookRepository.deleteById(id);
        return "redirect:/admin/books";
    }
}
