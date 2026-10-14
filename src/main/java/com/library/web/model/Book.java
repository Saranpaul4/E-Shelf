package com.library.web.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    private String isbn;
    private Integer publishedYear;

    /** Which shelf the book sits on in the shop. */
    @Enumerated(EnumType.STRING)
    private Genre genre;

    /** The language this edition is printed in. Drives the "By language" shelf. */
    private String language = "English";

    /**
     * Editions of the same story share this key, whatever language they are printed in.
     * Example: "norwegian-wood" on both the English and the Japanese row. Left empty,
     * the book stands on its own and nothing is suggested beside it.
     */
    private String workKey;

    /** What one copy costs to buy. */
    private BigDecimal price;

    /** What one copy costs to rent for the rental period. */
    private BigDecimal rentalPrice;

    /** How many physical copies the shop still has on the shelf. */
    private int stockQuantity;

    /** Optional cover picture an admin can point at. Left empty, the ISBN lookup is used. */
    private String coverUrl;

    private LocalDate arrivalDate;

    /**
     * A short blurb shown before someone commits to buying: what the book is about.
     * Left empty, the shop says so rather than inventing one.
     */
    @Column(length = 2000)
    private String summary;

    public Book() {
    }

    public Book(String title, String author, String isbn, int publishedYear, Genre genre,
                BigDecimal price, BigDecimal rentalPrice, int stockQuantity) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publishedYear = publishedYear;
        this.genre = genre;
        this.price = price;
        this.rentalPrice = rentalPrice;
        this.stockQuantity = stockQuantity;
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    /** Cover for the shop grid and tables. */
    public String getCoverMedium() {
        return coverForSize("M");
    }

    /** Bigger cover for the single-book page. */
    public String getCoverLarge() {
        return coverForSize("L");
    }

    public boolean isHasCover() {
        return getCoverMedium() != null;
    }

    /**
     * The admin's own URL wins. Otherwise Open Library serves a free cover for any
     * valid ISBN, and if we have neither, the page shows the letter tile instead.
     */
    private String coverForSize(String size) {
        if (coverUrl != null && !coverUrl.isBlank()) {
            return coverUrl.trim();
        }
        String digits = isbn == null ? "" : isbn.replaceAll("[^0-9Xx]", "");
        if (digits.length() != 10 && digits.length() != 13) {
            return null;
        }
        // default=false makes Open Library answer 404 instead of its "No cover"
        // placeholder image, so the onerror handler can reveal the letter tile.
        return "https://covers.openlibrary.org/b/isbn/" + digits + "-" + size + ".jpg?default=false";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Integer getPublishedYear() {
        return publishedYear;
    }

    public void setPublishedYear(Integer publishedYear) {
        this.publishedYear = publishedYear;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getWorkKey() {
        return workKey;
    }

    public void setWorkKey(String workKey) {
        this.workKey = workKey;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getRentalPrice() {
        return rentalPrice;
    }

    public void setRentalPrice(BigDecimal rentalPrice) {
        this.rentalPrice = rentalPrice;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public LocalDate getArrivalDate() {
        return arrivalDate;
    }

    public void setArrivalDate(LocalDate arrivalDate) {
        this.arrivalDate = arrivalDate;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
