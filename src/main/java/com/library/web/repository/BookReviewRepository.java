package com.library.web.repository;

import java.util.List;

import com.library.web.model.BookReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookReviewRepository extends JpaRepository<BookReview, Long> {

    List<BookReview> findByBookIdOrderByCreatedAtDesc(Long bookId);

    List<BookReview> findTop6ByOrderByCreatedAtDesc();

    List<BookReview> findByUsernameOrderByCreatedAtDesc(String username);

    long countByBookId(Long bookId);

    long countByUsername(String username);

    void deleteByBookId(Long bookId);
}
