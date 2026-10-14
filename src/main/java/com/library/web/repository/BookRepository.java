package com.library.web.repository;

import java.util.Collection;
import java.util.List;

import com.library.web.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByArrivalDateIsNull();

    List<Book> findByArrivalDateNotNullOrderByArrivalDateAsc();

    long countByArrivalDateIsNull();

    long countByArrivalDateNotNull();

    long countByArrivalDateIsNullAndStockQuantityGreaterThan(int quantity);

    long countByArrivalDateIsNullAndStockQuantity(int quantity);

    List<Book> findByIdIsIn(Collection<Long> ids);

    @Query("select b from Book b where b.arrivalDate is null " +
            "and (lower(b.title) like lower(concat('%', :q, '%')) " +
            "or lower(b.author) like lower(concat('%', :q, '%')))")
    List<Book> searchArrived(@Param("q") String q);
}
