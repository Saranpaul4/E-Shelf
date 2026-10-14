package com.library.web.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.library.web.model.OrderType;

/**
 * A customer's shopping basket. One instance lives in the HTTP session, so the
 * contents survive navigation between pages but disappear when the session ends.
 */
public class Cart {

    /** How many copies of a book the customer wants, and whether to buy or rent them. */
    public record Line(OrderType type, int quantity) {
    }

    private final Map<Long, Line> lines = new LinkedHashMap<>();

    public void add(Long bookId, OrderType type, int quantity) {
        Line current = lines.get(bookId);
        if (current == null) {
            lines.put(bookId, new Line(type, Math.max(1, quantity)));
        } else if (current.type() == type) {
            lines.put(bookId, new Line(type, current.quantity() + Math.max(1, quantity)));
        } else {
            // Switching the same book from buy to rent (or back) starts that line again.
            lines.put(bookId, new Line(type, Math.max(1, quantity)));
        }
    }

    public void setQuantity(Long bookId, int quantity) {
        Line current = lines.get(bookId);
        if (current == null) {
            return;
        }
        if (quantity <= 0) {
            lines.remove(bookId);
        } else {
            lines.put(bookId, new Line(current.type(), quantity));
        }
    }

    public void remove(Long bookId) {
        lines.remove(bookId);
    }

    public void clear() {
        lines.clear();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public int totalItems() {
        return lines.values().stream().mapToInt(Line::quantity).sum();
    }

    public Map<Long, Line> lines() {
        return lines;
    }
}
