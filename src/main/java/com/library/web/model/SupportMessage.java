package com.library.web.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One line in a support conversation. Every message is filed under the customer's
 * username, so a thread is just all the rows for one username in time order.
 * {@code fromAdmin} tells us which side of the chat bubble it sits on: false means the
 * reader asked it, true means a librarian answered.
 */
@Entity
@Table(name = "support_message")
public class SupportMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The reader this conversation belongs to. */
    @Column(nullable = false)
    private String username;

    @Column(nullable = false, length = 1000)
    private String body;

    private boolean fromAdmin;

    /**
     * True when the shop's own helper wrote this line rather than a person. It is stored
     * so the chat can label it, and so the admin desk can tell an automatic reply apart
     * from an answer a librarian actually typed.
     */
    private boolean automated;

    private LocalDateTime createdAt;

    public SupportMessage() {
    }

    public SupportMessage(String username, String body, boolean fromAdmin) {
        this.username = username;
        this.body = body;
        this.fromAdmin = fromAdmin;
        this.createdAt = LocalDateTime.now();
    }

    /** A line written by the helper, not by a librarian. */
    public static SupportMessage automatedReply(String username, String body) {
        SupportMessage message = new SupportMessage(username, body, true);
        message.automated = true;
        return message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isFromAdmin() {
        return fromAdmin;
    }

    public void setFromAdmin(boolean fromAdmin) {
        this.fromAdmin = fromAdmin;
    }

    public boolean isAutomated() {
        return automated;
    }

    public void setAutomated(boolean automated) {
        this.automated = automated;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
