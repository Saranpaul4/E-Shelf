package com.library.web.repository;

import com.library.web.model.BookLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookLikeRepository extends JpaRepository<BookLike, Long> {

    void deleteByBookId(Long bookId);
}
