package com.library.web.repository;

import java.util.List;

import com.library.web.model.BookOrder;
import com.library.web.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<BookOrder, Long> {

    List<BookOrder> findByUsernameOrderByOrderDateDesc(String username);

    List<BookOrder> findAllByOrderByOrderDateDesc();

    List<BookOrder> findByUsernameAndStatus(String username, OrderStatus status);

    List<BookOrder> findByStatus(OrderStatus status);

    long countByStatus(OrderStatus status);

    long countByType(com.library.web.model.OrderType type);

    long countByUsername(String username);

    long countByBookId(Long bookId);

    boolean existsByBookIdAndUsernameAndStatus(Long bookId, String username, OrderStatus status);
}
