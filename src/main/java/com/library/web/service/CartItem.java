package com.library.web.service;

import java.math.BigDecimal;

import com.library.web.model.Book;
import com.library.web.model.OrderType;

/** A cart line resolved against the database, ready to show in a template. */
public record CartItem(Book book, OrderType type, int quantity,
                       BigDecimal unitPrice, BigDecimal lineTotal, int availableStock) {

    public boolean moreThanStock() {
        return quantity > availableStock;
    }
}
