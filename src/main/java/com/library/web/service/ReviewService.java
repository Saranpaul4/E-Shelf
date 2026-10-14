package com.library.web.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.library.web.model.Book;
import com.library.web.model.BookReview;
import com.library.web.repository.BookLikeRepository;
import com.library.web.repository.BookRepository;
import com.library.web.repository.BookReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Everything readers can say about a book: star reviews and the likes they collected. */
@Service
public class ReviewService {

    private static final int MAX_COMMENT_LENGTH = 400;

    /**
     * The reader verdict for one book. Bundling the rating, the counts and the likes
     * into one record keeps the templates down to a single lookup per book.
     */
    public record Opinion(long reviewCount, double average, long likes) {

        private static final Opinion NONE = new Opinion(0, 0, 0);

        public boolean hasRatings() {
            return reviewCount > 0;
        }

        /** Stars to draw, rounded, so 4.5 reads as five on the card. */
        public long fullStars() {
            return Math.round(average);
        }

        /** One decimal is enough to compare books, and never prints "4.333333". */
        public String averageText() {
            return String.format(java.util.Locale.ROOT, "%.1f", average);
        }
    }

    private final BookRepository bookRepository;
    private final BookReviewRepository reviewRepository;
    private final BookLikeRepository likeRepository;

    public ReviewService(BookRepository bookRepository,
                         BookReviewRepository reviewRepository,
                         BookLikeRepository likeRepository) {
        this.bookRepository = bookRepository;
        this.reviewRepository = reviewRepository;
        this.likeRepository = likeRepository;
    }

    @Transactional
    public BookReview addReview(Long bookId, String username, int rating, String comment) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid book id: " + bookId));

        String cleaned = comment == null ? "" : comment.trim();
        if (cleaned.length() > MAX_COMMENT_LENGTH) {
            cleaned = cleaned.substring(0, MAX_COMMENT_LENGTH);
        }

        BookReview review = new BookReview(book, username,
                BookReview.clampRating(rating), cleaned.isEmpty() ? null : cleaned);
        return reviewRepository.save(review);
    }

    /** A reader may take back their own words, and nobody else's. */
    @Transactional
    public boolean deleteReview(Long reviewId, String username) {
        Optional<BookReview> found = reviewRepository.findById(reviewId)
                .filter(review -> review.getUsername().equals(username));
        found.ifPresent(reviewRepository::delete);
        return found.isPresent();
    }

    public List<BookReview> reviewsFor(Long bookId) {
        return reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId);
    }

    public List<BookReview> latestReviews(int limit) {
        return reviewRepository.findTop6ByOrderByCreatedAtDesc().stream()
                .limit(limit)
                .toList();
    }

    public Opinion opinionFor(Long bookId) {
        return opinionsFor(List.of(bookRepository.findById(bookId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid book id: " + bookId))))
                .getOrDefault(bookId, Opinion.NONE);
    }

    /**
     * Reads the two opinion tables once and groups them in memory, so a page with 37
     * books costs three queries instead of 37 x 2. Every book in the input gets an
     * entry, even a book nobody has reviewed yet, so the template never sees a null.
     */
    public Map<Long, Opinion> opinionsFor(Collection<Book> books) {
        Map<Long, List<BookReview>> reviewsByBook = reviewRepository.findAll().stream()
                .collect(Collectors.groupingBy(review -> review.getBook().getId()));

        Map<Long, Long> likesByBook = likeRepository.findAll().stream()
                .collect(Collectors.groupingBy(like -> like.getBook().getId(), Collectors.counting()));

        Map<Long, Opinion> result = new HashMap<>();
        for (Book book : books) {
            List<BookReview> found = reviewsByBook.getOrDefault(book.getId(), List.of());
            long reviewCount = found.size();
            double average = found.stream().mapToInt(BookReview::getRating).average().orElse(0);
            result.put(book.getId(), new Opinion(reviewCount, average,
                    likesByBook.getOrDefault(book.getId(), 0L)));
        }
        return result;
    }

    /** Books ordered by how readers rated them, using a minimum number of reviews. */
    public List<Book> topRated(Collection<Book> books, int minReviews, int limit) {
        Map<Long, Opinion> opinions = opinionsFor(books);

        List<Book> ranked = new ArrayList<>(books);
        ranked.removeIf(book -> !book.isInStock()
                || opinions.get(book.getId()).reviewCount() < minReviews);
        ranked.sort((left, right) -> {
            Opinion a = opinions.get(left.getId());
            Opinion b = opinions.get(right.getId());
            int byAverage = Double.compare(b.average(), a.average());
            return byAverage != 0 ? byAverage : Long.compare(b.reviewCount(), a.reviewCount());
        });
        return ranked.stream().limit(limit).toList();
    }

    /** Books ordered by Likes, for the "everyone is reading this" shelf. */
    public List<Book> mostLiked(Collection<Book> books, int limit) {
        Map<Long, Opinion> opinions = opinionsFor(books);

        List<Book> ranked = new ArrayList<>(books);
        ranked.removeIf(book -> opinions.get(book.getId()).likes() == 0);
        ranked.sort((left, right) -> {
            Opinion a = opinions.get(left.getId());
            Opinion b = opinions.get(right.getId());
            int byLikes = Long.compare(b.likes(), a.likes());
            return byLikes != 0 ? byLikes : Double.compare(b.average(), a.average());
        });
        return ranked.stream().limit(limit).toList();
    }

    /** Drops a book's reviews and Likes so the admin can delete the book itself. */
    @Transactional
    public void clearOpinionsFor(Long bookId) {
        reviewRepository.deleteByBookId(bookId);
        likeRepository.deleteByBookId(bookId);
    }
}
