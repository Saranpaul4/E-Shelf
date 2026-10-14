package com.library.web.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * The little helper that answers inside the support chat the moment a reader types.
 *
 * <p>This is not machine learning and it does not pretend to be. It is a short list of
 * topics, each one holding the words that usually mean that topic. A reader's message is
 * lower-cased, and the first topic whose words appear in it wins and replies. If nothing
 * matches, the helper says so honestly and tells the reader a librarian will pick it up -
 * which is true, because the message is stored on the admin Support desk either way.</p>
 *
 * <p>Keeping the answers in one place means a librarian who changes how the shop works
 * only has to edit the sentence below, not a template.</p>
 */
@Service
public class SupportBot {

    /**
     * One topic: the words that flag it, and the reply to send when they appear.
     * Order matters - the first matching topic answers, so specific topics
     * (late fees) are listed before general ones (orders).
     *
     * <p>The phrases are turned into regular expressions once, when the class loads.
     * A record's compact constructor is the neat place to do that: it takes the plain
     * list and stores an extra, ready-compiled field beside it.</p>
     */
    private record Topic(List<String> triggers, String answer, List<Pattern> patterns) {

        Topic(List<String> triggers, String answer) {
            this(triggers, answer, compile(triggers));
        }

        /** "late fee" becomes the pattern \blate\ fee\b - whole words only. */
        private static List<Pattern> compile(List<String> triggers) {
            return triggers.stream()
                    .map(trigger -> Pattern.compile("\\b" + Pattern.quote(trigger) + "\\b"))
                    .toList();
        }
    }

    private static final String GREETING =
            "Hello! I'm the E-Shelf helper. Ask me about renting, buying, returns or your "
                    + "orders, or just say what went wrong and a librarian will take over.";

    private static final String OPENING =
            "the shop counter is open whenever the site is, and a librarian reads the Support "
                    + "desk regularly and replies in this chat.";

    private static final List<Topic> TOPICS = List.of(
            new Topic(List.of("late fee", "overdue", "due date", "returned late", "extra charge"),
                    "Rentals are due after " + ShopService.RENTAL_DAYS + " days. Every day past that adds "
                            + "$" + ShopService.LATE_FEE_PER_DAY.setScale(2).toPlainString()
                            + " per book. Open your Cart - rentals that are due back are listed there with "
                            + "the fee already worked out, and you settle it when you mark the book returned."),

            new Topic(List.of("return", "give back", "hand back", "send back"),
                    "To give a rental back, go to Cart, find the 'Rentals to return' section and press "
                            + "Return. Stock goes back on the shelf as soon as you do, and any late fee is "
                            + "charged at that moment."),

            new Topic(List.of("rent", "borrow", "for how long", "how long", "deposit"),
                    "Renting borrows one copy for " + ShopService.RENTAL_DAYS + " days. Press Rent on a book "
                            + "card, or Rent on its details page, and it joins your cart like a purchase would. "
                            + "The rent price shown on the card is the whole price for those "
                            + ShopService.RENTAL_DAYS + " days."),

            new Topic(List.of("buy", "price", "cost", "pay", "payment", "card", "discount", "refund"),
                    "Buy prices are on every card and on the details page - press Buy and read the summary "
                            + "before it goes in your cart. Checkout takes you through a confirmation page and "
                            + "a test payment. Nothing is charged for real here; this shop runs on Razorpay "
                            + "test keys, so any card will do."),

            new Topic(List.of("deliver", "shipping", "post", "courier", "home"),
                    "There is no courier. Orders are collected from the library counter: pay on the "
                            + "confirmation page, then pick the book up at the shelf."),

            new Topic(List.of("stock", "available", "sold out", "in stock", "copies", "out of stock"),
                    "Each card shows how many copies are ready, and a sold-out title says so plainly. "
                            + "If a book has language editions, the sold-out line links to the editions that "
                            + "are still on the shelf - open its details page to see them."),

            new Topic(List.of("language", "tamil", "hindi", "telugu", "bengali", "french", "spanish",
                    "german", "japanese", "russian", "portuguese", "italian"),
                    "Yes. Use the 'By language' filter on the Shop page - the same story printed in "
                            + "different languages sits on the shelves separately, and a book's details page "
                            + "lists the other editions under 'Also available in'."),

            new Topic(List.of("order", "my orders", "history", "receipt", "track"),
                    "Your orders live on your Profile page under 'Manage orders'. Each one shows the book, "
                            + "whether it was a buy or a rent, the amount and the status."),

            new Topic(List.of("review", "rating", "stars", "like"),
                    "Any reader can leave a review on a book's details page - pick one to five stars and "
                            + "write a line if you like. Your own review has a 'Remove my review' link beside it."),

            new Topic(List.of("account", "password", "login", "log in", "sign up", "register", "email"),
                    "Your name, avatar, phone and address are on the Profile page. Password and email "
                            + "changes need a librarian, so say so here and someone will handle it."),

            new Topic(List.of("recommend", "suggest", "what should i read", "idea", "for me"),
                    "The Suggestions tab picks up titles that suit a new reader, and the shop's genre and "
                            + "language shelves are the fastest way to browse. Tell me a mood or an author and "
                            + "a librarian will answer with something specific."),

            new Topic(List.of("human", "person", "librarian", "staff", "manager", "someone", "talk to"),
                    "I've flagged this for a librarian and left your message on the Support desk. They "
                            + "answer in this same chat - keep the window open or come back later."),

            new Topic(List.of("hello", "hi", "hey", "good morning", "good evening", "yo"), GREETING),

            new Topic(List.of("thanks", "thank you", "ok", "okay", "great", "cheers", "nice"),
                    "Any time. Anything else I can help with, or shall I leave the thread open for a librarian?"),

            new Topic(List.of("bye", "goodbye", "see you", "that's all", "thats all"),
                    "Happy reading. If something comes up later, this chat is in the corner of every shop page."));

    /**
     * The answer for one reader message. Never returns null - an unmatched question gets
     * an honest hand-off line instead of a guess.
     */
    public String reply(String message) {
        String text = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return GREETING;
        }
        for (Topic topic : TOPICS) {
            if (matches(topic, text)) {
                return topic.answer();
            }
        }
        return "I don't have a ready answer for that one, so I've left your message on the Support "
                + "desk for a librarian. Meanwhile, " + OPENING;
    }

    /**
     * A topic matches when any one of its phrases appears as whole words in the message.
     * Matching on word boundaries is what stops "hi" from answering a question that merely
     * contains the word white or which.
     */
    private static boolean matches(Topic topic, String text) {
        for (Pattern trigger : topic.patterns()) {
            if (trigger.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
