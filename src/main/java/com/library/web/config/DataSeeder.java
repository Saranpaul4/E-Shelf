package com.library.web.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.library.web.model.Book;
import com.library.web.model.BookLike;
import com.library.web.model.BookOrder;
import com.library.web.model.BookReview;
import com.library.web.model.Genre;
import com.library.web.model.OrderStatus;
import com.library.web.model.OrderType;
import com.library.web.model.ProfileAvatar;
import com.library.web.model.SiteUser;
import com.library.web.model.SupportMessage;
import com.library.web.repository.BookLikeRepository;
import com.library.web.repository.BookRepository;
import com.library.web.repository.BookReviewRepository;
import com.library.web.repository.OrderRepository;
import com.library.web.repository.SiteUserRepository;
import com.library.web.repository.SupportMessageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Fills the empty in-memory database on every start so the app has something to show. */
@Configuration
public class DataSeeder {

    @Bean
    @Order(1)
    CommandLineRunner seedUsers(SiteUserRepository repository, PasswordEncoder encoder) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.saveAll(List.of(
                    withProfile(new SiteUser("admin", encoder.encode("admin123"), "ADMIN",
                            "Ada Librarian", "ada@eshelf.test"), ProfileAvatar.HOLMES, 400,
                            "+91 98400 11223", "1 Rose Street, Chennai 600001"),
                    withProfile(new SiteUser("sofia", encoder.encode("admin123"), "ADMIN",
                            "Sofia Reyes", "sofia@eshelf.test"), ProfileAvatar.ALICE, 210,
                            "+34 600 123 456", "Calle Mar 22, Madrid 28005"),
                    withProfile(new SiteUser("omar", encoder.encode("admin123"), "ADMIN",
                            "Omar Iqbal", "omar@eshelf.test"), null, 150,
                            "+971 50 778 990", "Alserkal Avenue, Dubai"),
                    withRecovery(withProfile(new SiteUser("reader", encoder.encode("library123"), "USER",
                            "Ravi Kumar", "ravi@example.test"), ProfileAvatar.MOWGLI, 90,
                            "+91 90000 55443", "Flatchen House, Bengaluru 560001"),
                            "What city were you born in?", "chennai"),
                    withRecovery(withProfile(new SiteUser("maya", encoder.encode("library123"), "USER",
                            "Maya Sen", "maya@example.test"), ProfileAvatar.DORIAN, 30,
                            null, null),
                            "What is your favourite book?", "the Hobbit")));
        };
    }

    /**
     * Gives a seeded reader a forgotten-password question, so the reset flow can be tried
     * on the demo accounts. Answers are stored lower case, the way the controller compares them.
     */
    private static SiteUser withRecovery(SiteUser user, String question, String answer) {
        user.setSecurityQuestion(question);
        user.setSecurityAnswer(answer);
        return user;
    }

    @Bean
    @Order(2)
    CommandLineRunner seedBooks(BookRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.saveAll(List.of(
                    book("Clean Code", "Robert C. Martin", "978-0132350884", 2008, Genre.TECHNOLOGY, 44.99, 6.99, 4),
                    book("Effective Java", "Joshua Bloch", "978-0134685991", 2018, Genre.TECHNOLOGY, 49.99, 7.50, 3),
                    book("Head First Java", "Kathy Sierra", "978-0596009205", 2005, Genre.TECHNOLOGY, 39.99, 6.00, 0),
                    book("The Pragmatic Programmer", "Andrew Hunt", "978-0201616224", 1999, Genre.TECHNOLOGY, 42.50, 6.50, 5),
                    book("Spring in Action", "Craig Walls", "978-1617294945", 2018, Genre.TECHNOLOGY, 46.99, 7.00, 2),
                    book("Java Concurrency in Practice", "Brian Goetz", "978-0321349606", 2006, Genre.TECHNOLOGY, 51.99, 8.00, 2),
                    book("Refactoring", "Martin Fowler", "978-0134757599", 2018, Genre.TECHNOLOGY, 54.99, 8.50, 3),
                    book("Design Patterns", "Erich Gamma", "978-0201633610", 1994, Genre.TECHNOLOGY, 48.99, 7.50, 0),
                    book("Introduction to Algorithms", "Thomas H. Cormen", "978-0262046305", 2022, Genre.TECHNOLOGY, 89.99, 12.00, 1),
                    book("The Mythical Man-Month", "Frederick P. Brooks", "978-0201835953", 1995, Genre.TECHNOLOGY, 34.99, 5.50, 4),
                    book("Code Complete", "Steve McConnell", "978-0735619678", 2004, Genre.TECHNOLOGY, 42.99, 6.50, 3),
                    book("Clean Architecture", "Robert C. Martin", "978-0134494166", 2017, Genre.TECHNOLOGY, 41.99, 6.50, 0),
                    book("1984", "George Orwell", "978-0451524935", 1949, Genre.NOVEL, 12.99, 2.00, 6),
                    book("Animal Farm", "George Orwell", "978-0451526342", 1945, Genre.NOVEL, 10.99, 1.80, 5),
                    book("Brave New World", "Aldous Huxley", "978-0060850524", 1932, Genre.NOVEL, 13.99, 2.20, 0),
                    book("Fahrenheit 451", "Ray Bradbury", "978-1451673319", 1953, Genre.NOVEL, 14.99, 2.40, 4),
                    book("The Great Gatsby", "F. Scott Fitzgerald", "978-0743273565", 1925, Genre.NOVEL, 11.99, 2.00, 3),
                    book("To Kill a Mockingbird", "Harper Lee", "978-0061120084", 1960, Genre.NOVEL, 12.99, 2.10, 4),
                    book("Pride and Prejudice", "Jane Austen", "978-0141439518", 1813, Genre.NOVEL, 9.99, 1.60, 0),
                    book("The Hobbit", "J.R.R. Tolkien", "978-0547928227", 1937, Genre.NOVEL, 16.99, 2.80, 5),
                    book("The Catcher in the Rye", "J.D. Salinger", "978-0316769488", 1951, Genre.NOVEL, 12.50, 2.00, 2),
                    book("Lord of the Flies", "William Golding", "978-0399506437", 1954, Genre.NOVEL, 11.50, 1.90, 0),
                    ofWork(book("The Alchemist", "Paulo Coelho", "978-0061122415", 1988, Genre.NOVEL, 13.99, 2.20, 5), "alchemist"),
                    ofWork(book("Norwegian Wood", "Haruki Murakami", "978-0375704024", 1987, Genre.NOVEL, 15.99, 2.50, 0), "norwegian-wood"),
                    book("The Little Prince", "Antoine de Saint-Exupéry", "978-0156012195", 1943, Genre.NOVEL, 10.99, 1.70, 7),
                    ofWork(book("The Stranger", "Albert Camus", "978-0521338516", 1988, Genre.NOVEL, 12.99, 2.10, 4), "stranger"),
                    ofWork(book("The Metamorphosis", "Franz Kafka", "0028180518", 2000, Genre.NOVEL, 11.50, 1.90, 5), "metamorphosis"),
                    ofWork(book("Snow Country", "Yasunari Kawabata", "0241540534", 2011, Genre.NOVEL, 13.99, 2.30, 3), "snow-country"),
                    ofWork(book("One Hundred Years of Solitude", "Gabriel García Márquez", "0241972353", 2014, Genre.NOVEL, 16.99, 2.80, 4), "hundred-years"),
                    ofWork(book("Crime and Punishment", "Fyodor Dostoevsky", "978-0141192802", 2014, Genre.NOVEL, 15.99, 2.60, 3), "crime-punishment"),
                    book("Watchmen", "Alan Moore", "978-8467473278", 1986, Genre.COMIC, 19.99, 3.20, 5),
                    book("Maus I", "Art Spiegelman", "978-8439504207", 1986, Genre.COMIC, 17.50, 2.90, 4),
                    book("Persepolis", "Marjane Satrapi", "978-0375714665", 2004, Genre.COMIC, 16.99, 2.80, 6),
                    book("Smile", "Raina Telgemeier", "978-0545132060", 2010, Genre.COMIC, 12.99, 2.10, 8),
                    book("Asterix Omnibus 1", "René Goscinny", "978-1545805664", 2000, Genre.COMIC, 24.99, 4.00, 3),
                    book("Amar Chitra Katha: Krishna", "Anant Pai", "978-8175081420", 2001, Genre.COMIC, 9.99, 1.50, 10),
                    book("Sapiens", "Yuval Noah Harari", "978-0062316097", 2011, Genre.NON_FICTION, 22.99, 3.60, 6),
                    book("Thinking, Fast and Slow", "Daniel Kahneman", "978-0374533557", 2011, Genre.NON_FICTION, 24.99, 3.90, 0),
                    book("Atomic Habits", "James Clear", "978-0735211292", 2018, Genre.NON_FICTION, 18.99, 3.00, 8),
                    book("Wings of Fire", "A. P. J. Abdul Kalam", "978-8173711466", 1999, Genre.BIOGRAPHY, 14.99, 2.40, 6),
                    book("The Diary of a Young Girl", "Anne Frank", "978-0553296983", 1944, Genre.BIOGRAPHY, 13.50, 2.20, 4),
                    book("Becoming", "Michelle Obama", "978-0241982976", 2018, Genre.BIOGRAPHY, 21.99, 3.40, 3)));

            // Original-language editions, so the "By language" shelf has something on it.
            // Each ISBN is the edition Open Library has a real cover picture for.
            repository.saveAll(List.of(
                    inLanguage(book("பொன்னியின் செல்வன் (Ponniyin Selvan)", "Kalki Krishnan", "978-1639570515",
                            1950, Genre.NOVEL, 15.99, 2.60, 5), "Tamil"),
                    inLanguage(book("திருக்குறள் (Thirukkural)", "Thiruvalluvar", "0140445838",
                            1800, Genre.NON_FICTION, 11.99, 1.90, 6), "Tamil"),
                    inLanguage(book("गोदान (Godan)", "Munshi Premchand", "978-0948924071",
                            1950, Genre.NOVEL, 12.99, 2.10, 4), "Hindi"),
                    inLanguage(book("मधुशाला (Madhushala)", "Harivansh Rai Bachchan", "978-0140120097",
                            1935, Genre.NOVEL, 9.99, 1.60, 7), "Hindi"),
                    inLanguage(book("গীতাঞ্জলি (Gitanjali)", "Rabindranath Tagore", "978-1533358622",
                            1910, Genre.NOVEL, 10.99, 1.80, 3), "Bengali"),
                    inLanguage(ofWork(book("L'Étranger", "Albert Camus", "978-2218004520",
                            1942, Genre.NOVEL, 12.50, 2.10, 5), "stranger"), "French"),
                    inLanguage(ofWork(book("Cien años de soledad", "Gabriel García Márquez", "978-9502513072",
                            1967, Genre.NOVEL, 14.99, 2.50, 4), "hundred-years"), "Spanish"),
                    inLanguage(ofWork(book("Die Verwandlung", "Franz Kafka", "978-1092787130",
                            1915, Genre.NOVEL, 10.50, 1.70, 6), "metamorphosis"), "German"),
                    inLanguage(ofWork(book("雪国 (Yukiguni)", "Yasunari Kawabata", "978-4101001012",
                            1948, Genre.NOVEL, 13.50, 2.30, 3), "snow-country"), "Japanese"),
                    inLanguage(ofWork(book("ノルウェイの森 (Noruwei no Mori)", "Haruki Murakami", "978-9871210725",
                            1987, Genre.NOVEL, 16.99, 2.80, 4), "norwegian-wood"), "Japanese"),
                    inLanguage(ofWork(book("Преступление и наказание", "Фёдор Достоевский", "978-0679405573",
                            1866, Genre.NOVEL, 15.50, 2.60, 2), "crime-punishment"), "Russian"),
                    inLanguage(book("Мастер и Маргарита", "Михаил Булгаков", "8375108553",
                            1967, Genre.NOVEL, 14.50, 2.40, 0), "Russian"),
                    inLanguage(book("Se una notte d'inverno un viaggiatore", "Italo Calvino", "0749399236",
                            1979, Genre.NOVEL, 13.99, 2.30, 3), "Italian"),
                    inLanguage(ofWork(book("O Alquimista", "Paulo Coelho", "978-2290258064",
                            1988, Genre.NOVEL, 12.99, 2.20, 5), "alchemist"), "Portuguese")));

            repository.saveAll(List.of(
                    arriving(book("Learning SQL", "Alan Beaulieu", "978-1492057611", 2020, Genre.TECHNOLOGY, 39.99, 6.00, 0), 5),
                    arriving(book("Spring Boot 4 in Action", "Craig Walls", "978-1617299123", 2027, Genre.TECHNOLOGY, 47.99, 7.20, 0), 14),
                    arriving(book("Domain-Driven Design Distilled", "Vaughn Vernon", "978-0134434421", 2016, Genre.TECHNOLOGY, 29.99, 4.80, 0), 21),
                    arriving(book("The Rust Programming Language", "Steve Klabnik", "978-1718500440", 2023, Genre.TECHNOLOGY, 44.99, 7.00, 0), 35),
                    arriving(book("Kotlin in Action", "Dmitry Jemerov", "978-1617293290", 2017, Genre.TECHNOLOGY, 43.99, 6.80, 0), 48)));
        };
    }

    /**
     * A couple of past orders so the admin's shop screens are not empty on day one.
     * The stock numbers above already account for these copies leaving the shelf.
     */
    @Bean
    @Order(3)
    CommandLineRunner seedOrders(OrderRepository orders, BookRepository books) {
        return args -> {
            if (orders.count() > 0) {
                return;
            }
            Book cleanCode = books.findByArrivalDateIsNull().stream()
                    .filter(b -> b.getTitle().equals("Clean Code"))
                    .findFirst().orElseThrow();
            Book sapiens = books.findByArrivalDateIsNull().stream()
                    .filter(b -> b.getTitle().equals("Sapiens"))
                    .findFirst().orElseThrow();

            BookOrder bought = order("reader", cleanCode, OrderType.BUY, 1, cleanCode.getPrice());
            BookOrder renting = order("maya", sapiens, OrderType.RENT, 1, sapiens.getRentalPrice());
            renting.setDueDate(LocalDate.now().plusDays(9));

            // A rental that is already past its due date, so the "Return & pay" late-fee path
            // has something to show on day one. Reader holds 2 copies, 3 days late.
            BookOrder late = order("reader", sapiens, OrderType.RENT, 2, sapiens.getRentalPrice());
            late.setDueDate(LocalDate.now().minusDays(3));

            orders.saveAll(List.of(bought, renting, late));
        };
    }

    /**
     * Sample reader opinions so the Suggestions tab has something to rank on day one.
     * The words below are invented notes for the demo, not quotes from any of the books.
     */
    @Bean
    @Order(4)
    CommandLineRunner seedOpinions(BookRepository books, BookReviewRepository reviews, BookLikeRepository likes) {
        return args -> {
            if (reviews.count() > 0) {
                return;
            }
            Map<String, Book> byTitle = books.findByArrivalDateIsNull().stream()
                    .collect(Collectors.toMap(Book::getTitle, book -> book));

            List<String[]> notes = List.of(
                    new String[] {"Clean Code", "reader", "5", "The chapter on naming changed how I write code the very next day."},
                    new String[] {"Clean Code", "maya", "4", "Dense in places, but the pages about small functions are worth it."},
                    new String[] {"Clean Code", "omar", "5", "I re-read the opening chapters twice. Better the second time."},
                    new String[] {"Atomic Habits", "maya", "5", "Short practical chapters — I finished it over one weekend."},
                    new String[] {"Atomic Habits", "reader", "4", "A few ideas repeat, still the most useful thing I lent out this month."},
                    new String[] {"Atomic Habits", "sofia", "5", "Easy to turn into your own checklist."},
                    new String[] {"Sapiens", "reader", "4", "Big ideas told simply. Good if you like arguing with a book."},
                    new String[] {"Sapiens", "sofia", "5", "Five stars, and then I lent my copy away, so buy two."},
                    new String[] {"The Hobbit", "maya", "5", "Easy to pick up again after a week off. A good first read-aloud."},
                    new String[] {"1984", "omar", "4", "Slow start, then it does not let go."},
                    new String[] {"1984", "reader", "5", "Worth reading once before you decide you dislike it."},
                    new String[] {"Wings of Fire", "sofia", "5", "Plain and honest, and it makes you want to work harder."},
                    new String[] {"Maus I", "reader", "5", "Heavy going at times and impossible to put down."},
                    new String[] {"Smile", "maya", "4", "Light and fun, exactly what I wanted on a boring Sunday."},
                    new String[] {"Effective Java", "omar", "5", "Not a starter book. Keep it by the keyboard once you know some Java."},
                    new String[] {"Becoming", "sofia", "4", "Warm and easy to read. Two copies were out at once in my house."},
                    new String[] {"The Little Prince", "maya", "5", "One sitting, and it stays with you for weeks."},
                    new String[] {"Introduction to Algorithms", "reader", "3", "Fine reference, terrible bedtime read. Go chapter by chapter."});

            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < notes.size(); i++) {
                String[] note = notes.get(i);
                Book book = byTitle.get(note[0]);
                if (book == null) {
                    continue;
                }
                BookReview review = new BookReview(book, note[1], Integer.parseInt(note[2]), note[3]);
                review.setCreatedAt(now.minusHours(notes.size() - i));
                reviews.save(review);
            }

            List<String[]> favourites = List.of(
                    new String[] {"Atomic Habits", "reader"}, new String[] {"Atomic Habits", "sofia"},
                    new String[] {"Atomic Habits", "omar"}, new String[] {"Atomic Habits", "maya"},
                    new String[] {"Clean Code", "maya"}, new String[] {"Clean Code", "omar"},
                    new String[] {"Clean Code", "sofia"},
                    new String[] {"Sapiens", "maya"}, new String[] {"Sapiens", "omar"},
                    new String[] {"Sapiens", "reader"},
                    new String[] {"1984", "maya"}, new String[] {"1984", "sofia"},
                    new String[] {"The Hobbit", "omar"}, new String[] {"The Hobbit", "reader"},
                    new String[] {"Maus I", "sofia"}, new String[] {"Smile", "reader"},
                    new String[] {"Effective Java", "reader"}, new String[] {"Wings of Fire", "maya"},
                    new String[] {"The Little Prince", "omar"}, new String[] {"Becoming", "reader"});

            for (String[] favourite : favourites) {
                Book book = byTitle.get(favourite[0]);
                if (book != null) {
                    likes.save(new BookLike(book, favourite[1]));
                }
            }
        };
    }

    /**
     * A demo account's profile: which story character they wore, how long ago they
     * joined, and the contact details a reader can edit on their own page.
     */
    private static SiteUser withProfile(SiteUser user, ProfileAvatar character, int daysAgo,
                                        String phone, String address) {
        user.setAvatar(character);
        user.setJoinedAt(LocalDateTime.now().minusDays(daysAgo));
        user.setPhone(phone);
        user.setAddress(address);
        return user;
    }

    /**
     * One short blurb per seeded title, keyed by the exact title string. Everything here
     * is our own description of what the book is about, written for the shop page.
     * {@link #book} looks the title up, so adding a book without a line here simply
     * leaves its summary blank and the shop says "No summary added yet".
     */
    private static final Map<String, String> SUMMARIES = Map.ofEntries(
            Map.entry("Clean Code", "A handbook of agile software craftsmanship. Robert C. Martin walks through naming, "
                    + "functions, comments and formatting, using Java examples that are then tidied up line by line. "
                    + "It is the book most teams hand to a developer who can write code that works but is hard to read."),
            Map.entry("Effective Java", "Ninety item-by-item rules for the language, each one a short essay: builders over "
                    + "telescoping constructors, trying to avoid streams where they do not fit, and knowing when to use an "
                    + "interface instead of a class. Joshua Bloch wrote much of java.util himself, and it shows."),
            Map.entry("Head First Java", "A visual, deliberately chatty introduction for people who have never written a "
                    + "program. Puzzles, diagrams and half-finished code exercises carry you from variables and loops "
                    + "through objects, collections, threads and a small networked game."),
            Map.entry("The Pragmatic Programmer", "Twenty-odd short pieces of advice about taking responsibility for your "
                    + "own craft: don't live with broken windows, keep your knowledge portfolio funded, and automate "
                    + "yourself out of a job. More about how to think than about any one language."),
            Map.entry("Spring in Action", "A practical tour of Spring Boot starting from a single application class and "
                    + "growing outwards: dependency injection, web MVC, data access, security, and finishing with reactive "
                    + "endpoints and microservice concerns. Each chapter builds on the same running example."),
            Map.entry("Java Concurrency in Practice", "The reference text for shared-memory code on the JVM. Brian Goetz "
                    + "explains visibility and atomicity, the java.util.concurrent collections, and how to judge when a "
                    + "lock is too coarse, too fine, or simply the wrong tool."),
            Map.entry("Refactoring", "A catalogue of small, safe transformations - extract method, replace conditional "
                    + "with polymorphism, move field - each one with a motivation, a mechanics checklist and a before/after "
                    + "code sample. The point is changing a program's design without changing what it does."),
            Map.entry("Design Patterns", "The original Gang of Four book: twenty-three patterns for object-oriented design, "
                    + "grouped as creational, structural and behavioural, each described with sample code in C++ and Smalltalk. "
                    + "Read it to learn the vocabulary, not to go and use all twenty-three."),
            Map.entry("Introduction to Algorithms", "The standard university text on algorithms. Sorting, graphs, "
                    + "dynamic programming, amortised analysis and NP-completeness, with pseudocode and proofs in a form "
                    + "that has made it a shelf reference for decades rather than a book you read straight through."),
            Map.entry("The Mythical Man-Month", "Fred Brooks on why adding people to a late software project makes it "
                    + "later, why the second system is the hardest, and why writing a specification is a contract. "
                    + "Essays from "
                    + "managing OS/360 that every project manager re-discovers for themselves eventually."),
            Map.entry("Code Complete", "A full-length treatment of construction: variable and routine design, control "
                    + "structure, defensive programming, and the up-front planning that decides whether a build turns out "
                    + "coherent. Enormous, and worth the weight for the checklists alone."),
            Map.entry("Clean Architecture", "Martin's follow-up on structure rather than code: the dependency rule, "
                    + "SOLID at component scale, and why a web framework belongs at the outer edge of an application "
                    + "rather than in the middle of its business rules."),
            Map.entry("1984", "Winston Smith rewrites history for the Ministry of Truth in a London that has not "
                    + "stopped the war. His affair with Julia and the diary he keeps are small acts of rebellion until "
                    + "O'Brien invites him into the Thought Police. Orwell wrote it as a warning about language and power."),
            Map.entry("Animal Farm", "The animals of Manor Farm drive out their farmer and run the place themselves, "
                    + "with rules chalked on a wall and honest equality at the start. Orwell's short fable follows how "
                    + "the pigs rewrite those rules one clause at a time."),
            Map.entry("Brave New World", "A World State where people are decanted into castes, kept cheerful with "
                    + "soma, and entertained out of wanting anything. Huxley's answer to Orwell is a society that "
                    + "controls its citizens by giving them exactly what they ask for."),
            Map.entry("Fahrenheit 451", "Guy Montag burns books for the fire service and has never once asked why, "
                    + "until a neighbour asks him whether he is happy. Bradbury's novel is about the appetite for "
                    + "easy distraction that makes censorship unnecessary."),
            Map.entry("The Great Gatsby", "Nick Carraway rents a small house beside a mansion where enormous parties "
                    + "are thrown for a man nobody actually knows. Fitzgerald's short novel is about Gatsby's plan to "
                    + "buy back a summer from five years ago."),
            Map.entry("To Kill a Mockingbird", "Scout Finch grows up in Maycomb, Alabama, watching her father Atticus "
                    + "defend a Black man accused of a crime the town has already decided. The children's summer games "
                    + "about their neighbour Boo Radlay run underneath the trial."),
            Map.entry("Pride and Prejudice", "Mrs Bennet has five daughters and no estate to pass them on, so "
                    + "marriage is a business. Elizabeth Bennet and the proud Mr Darcy misread each other for most of "
                    + "the novel before a second letter puts the record straight."),
            Map.entry("The Hobbit", "Bilbo Baggins is dragged out of his hole by Gandalf and thirteen dwarves on a "
                    + "quest to reclaim a mountain from the dragon Smaug. Along the way he finds a ring in the dark "
                    + "and meets Gollum, which is where the story starts getting serious."),
            Map.entry("The Catcher in the Rye", "Holden Caulfield leaves Pencey Prep early and spends two days "
                    + "wandering New York, calling almost everyone he meets a phony. Salinger's novel is his voice "
                    + "and nothing else - arguing with a city he cannot quite leave."),
            Map.entry("Lord of the Flies", "A plane crash leaves a group of British schoolboys on an uninhabited "
                    + "island with no adults. Ralph and Piggy try to keep a signal fire going while Jack builds a "
                    + "hunt, and the novel watches how quickly that turns."),
            Map.entry("The Alchemist", "Santiago, an Andalusian shepherd, sells his flock to follow a recurring "
                    + "dream of treasure at the Egyptian pyramids. Coelho's fable is about reading omens and the "
                    + "cost of pretending you never had a wish."),
            Map.entry("Norwegian Wood", "Toru Watanabe arrives in Tokyo in the late 1960s and falls for Naoko, a "
                    + "girl carrying the death of a mutual friend. Murakami's novel is quieter than his later work - "
                    + "a straightforward story about loyalty and grief."),
            Map.entry("The Little Prince", "A pilot crash-landed in the Sahara meets a small boy who has left his "
                    + "own tiny planet because of a difficult rose. Saint-Exupéry's fable visits grown-ups who count "
                    + "stars and switch on lamps, and is read differently at ten and at forty."),
            Map.entry("The Stranger", "Meursault attends his mother's funeral without crying, is shot dead a man on "
                    + "a beach, and is then put on trial for showing no emotion at the funeral. Camus' flat, first-person "
                    + "novel is the clearest introduction to his idea of the absurd."),
            Map.entry("The Metamorphosis", "Gregor Samsa wakes to find he has become a giant insect, and his first "
                    + "worry is that he will miss the train to work. Kafka spends the rest of the story on the family "
                    + "slowly learning to live around him."),
            Map.entry("Snow Country", "Shimamura, a Tokyo dilettante, makes repeated visits to a rural hot-spring "
                    + "resort to see a geisha named Komako, who knows the relationship has no future. Kawabata's "
                    + "novel is short, and mostly silence and weather."),
            Map.entry("One Hundred Years of Solitude", "The Buendía family founds the town of Macondo, and over seven "
                    + "generations repeats the same names, the same wars and the same stubborn loneliness. García "
                    + "Márquez tells it as if every miracle were an everyday occurrence."),
            Map.entry("Crime and Punishment", "Raskolnikov, a destitute former student in St Petersburg, murders a "
                    + "pawnbroker to prove to himself that he is not an ordinary man. Most of the novel is the "
                    + "aftermath: the hiding, the fever and the investigator who already knows."),
            Map.entry("Watchmen", "A murder mystery set in an alternate 1985 where retired costumed agents exist and "
                    + "the Cold War has never cooled. Alan Moore and Dave Gibbons use a nine-panel grid to tell a story "
                    + "about power, and whether the people who hold it can be trusted."),
            Map.entry("Maus I", "Art Spiegelman interviews his father Vladek about surviving Auschwitz, drawing Jews "
                    + "as mice, Germans as cats and Poles as pigs. The book never lets you forget it is a son "
                    + "recording an old, difficult man telling the truth."),
            Map.entry("Persepolis", "Marjane Satrapi's childhood in Tehran during the Islamic Revolution, drawn in "
                    + "plain black and white: a veil at ten, punk rock at fourteen, and a family that argues about "
                    + "politics at dinner."),
            Map.entry("Smile", "Raina Telgemeier's own school years: a fall that wrecks her front teeth, two years "
                    + "of braces, and the friend group that makes the whole thing unbearable. A middle-grade graphic "
                    + "memoir that is honest without being cruel."),
            Map.entry("Asterix Omnibus 1", "Five early volumes of the indomitable Gauls: the magic potion, a Roman "
                    + "camp in their village, and a journey across Europe to collect ingredients Goscinny and Uderzo "
                    + "turn into jokes."),
            Map.entry("Amar Chitra Katha: Krishna", "The classic Indian comics series retells the childhood stories of "
                    + "Krishna - butter stolen from the neighbours, the serpent Kaliya, lifting Govardhan - for a young "
                    + "reader, in the house drawing style the series is known for."),
            Map.entry("Sapiens", "Harari runs through the whole of human history in one long argument: the cognitive "
                    + "revolution, farming as a trap, money and empire as stories large numbers of strangers can "
                    + "agree on. A big-history book aimed at general readers."),
            Map.entry("Thinking, Fast and Slow", "Daniel Kahneman separates the mind into an quick, intuitive system "
                    + "and a slow, deliberate one, and uses that to explain anchoring, loss aversion and why we are "
                    + "confident exactly when we should not be."),
            Map.entry("Atomic Habits", "James Clear's case for changing behaviour by making it obvious, attractive, "
                    + "easy and satisfying - one percent at a time. Practical, heavily signposted, and built around "
                    + "habit loops rather than willpower."),
            Map.entry("Wings of Fire", "A. P. J. Abdul Kalam's autobiography: growing up in Rameswaram, selling "
                    + "newspapers, building India's launch vehicles and missile programme, and the decisions around "
                    + "the Pokhran tests. Written for young readers in India."),
            Map.entry("The Diary of a Young Girl", "Anne Frank's two years in a sealed Amsterdam attic, written as "
                    + "letters to Kitty: arguments with her mother, the burglary downstairs, and a teenager thinking "
                    + "hard about who she is while nowhere to go."),
            Map.entry("Becoming", "Michelle Obama's own account of being a lawyer who did not want the job, a mother "
                    + "in two presidential houses, and the girl from the South Side of Chicago who was never "
                    + "particularly ambitious until she was."),

            // Original-language editions. Blurbs are given in the language of the edition where
            // the title is not English, with a short English line so the shop stays usable.
            Map.entry("பொன்னியின் செல்வன் (Ponniyin Selvan)", "Kalki's historical novel set in the tenth-century Chola "
                    + "empire, following five friends drawn into a succession struggle. Widely read as the great Tamil "
                    + "serial novel."),
            Map.entry("திருக்குறள் (Thirukkural)", "Thiruvalluvar's 1,330 couplets on virtue, wealth and love - "
                    + "two lines each, and still quoted in Tamil argument and writing today."),
            Map.entry("गोदान (Godan)", "Premchand's last novel: a poor peasant named Hori promises to give a cow in "
                    + "charity, and the whole village's caste politics closes in around that promise."),
            Map.entry("मधुशाला (Madhushala)", "Harivansh Rai Bachchan's long poem of the wine-hall, where drinking is "
                    + "a way of talking about fate, time and letting go. Still read aloud at mushairas."),
            Map.entry("গীতাঞ্জলি (Gitanjali)", "Tagore's offering of songs - devotional, quiet, and the collection for "
                    + "which he was awarded the 1913 Nobel Prize in Literature."),
            Map.entry("L'Étranger", "Camus' novel, in the original French: Meursault, sa mère est morte, le beach, "
                    + "the trial. The English shelf has it as The Stranger."),
            Map.entry("Cien años de soledad", "García Márquez in Spanish: the Buendía family, Macondo, and the "
                    + "hundred years of the title. English readers have it as One Hundred Years of Solitude."),
            Map.entry("Die Verwandlung", "Kafka's original German - 'Als Gregor Samsa eines Morgens aus unruhigen "
                    + "Träumen erwachte' - the first sentence that made the story. English: The Metamorphosis."),
            Map.entry("雪国 (Yukiguni)", "Kawabata's novel in Japanese, the trip to the snow country hot springs and "
                    + "the geisha Komako. The English shelf carries it as Snow Country."),
            Map.entry("ノルウェイの森 (Noruwei no Mori)", "Murakami's novel in the original Japanese - the Beatles song "
                    + "the title comes from is 'Norwegian Wood' in English editions."),
            Map.entry("Преступление и наказание", "Dostoevsky's Raskolnikov in Russian, the full length of the "
                    + "St Petersburg novel. English readers know it as Crime and Punishment."),
            Map.entry("Мастер и Маргарита", "Bulgakov's devil visits Soviet Moscow, and a novelist who cannot finish "
                    + "his work. Written in the 1930s, published in the 1960s, and read in Russian ever since."),
            Map.entry("Se una notte d'inverno un viaggiatore", "Calvino's novel about you, the reader, trying to get "
                    + "through ten beginnings of books - in Italian, which matters, since the book is about voice."),
            Map.entry("O Alquimista", "Coelho's Santiago in the Brazilian Portuguese original, before the translations "
                    + "made it the most-read Portuguese novel in the world."),

            Map.entry("Learning SQL", "A first course in querying: SELECT, JOIN, grouping, and then constraints, "
                    + "indexes and transactions, each chapter ending with exercises you run against a sample database."),
            Map.entry("Spring Boot 4 in Action", "Build a Spring Boot application from the command line up, covering "
                    + "web endpoints, Spring Data, security and deployment, and finishing with reactive services."),
            Map.entry("Domain-Driven Design Distilled", "A short, readable introduction to DDD: ubiquitous language, "
                    + "bounded contexts and aggregates, for people who do not want to read Eric Evans' 500 pages first."),
            Map.entry("The Rust Programming Language", "The official book, by the people who wrote the language. "
                    + "Ownership and borrowing explained properly, then errors, concurrency, and a command-line tool "
                    + "built as you go."),
            Map.entry("Kotlin in Action", "The Kotlin reference for people who already know a JVM language: nullability "
                    + "by types, data classes, and coroutines written out in full rather than waved at."));

    /**
     * Builds one seeded book and attaches its blurb if the catalogue above has one.
     * Keeping the lookup here means the long {@code List.of(...)} blocks never have to
     * mention summaries at all.
     */
    private static Book book(String title, String author, String isbn, int year, Genre genre,
                             double price, double rentalPrice, int stockQuantity) {
        Book book = new Book(title, author, isbn, year, genre,
                BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(rentalPrice).setScale(2, RoundingMode.HALF_UP),
                stockQuantity);
        book.setSummary(SUMMARIES.get(title));
        return book;
    }

    private static Book arriving(Book book, int daysFromNow) {
        book.setArrivalDate(LocalDate.now().plusDays(daysFromNow));
        return book;
    }

    /**
     * One answered thread and one still-open question, so the admin Support desk has
     * something to show on day one. The wording here is invented demo chatter.
     */
    @Bean
    @Order(5)
    CommandLineRunner seedSupport(SupportMessageRepository support) {
        return args -> {
            if (support.count() > 0) {
                return;
            }
            support.saveAll(List.of(
                    new SupportMessage("reader", "Hi, my rental of Sapiens is overdue — how do I pay the late fee?", false),
                    new SupportMessage("reader", "Open your Cart and use Pay on return; the fee is added automatically.", true),
                    new SupportMessage("maya", "Do you have The Alchemist in Tamil?", false)));
        };
    }

    /** Which language edition the library actually holds. Defaults to English. */
    private static Book inLanguage(Book book, String language) {
        book.setLanguage(language);
        return book;
    }

    /**
     * Marks the book as one printing of a larger story. Two rows sharing a key are
     * sold as language editions of the same work, which is what the shop's
     * "Also available in" strip reads.
     */
    private static Book ofWork(Book book, String workKey) {
        book.setWorkKey(workKey);
        return book;
    }

    private static BookOrder order(String username, Book book, OrderType type, int quantity, BigDecimal unitPrice) {
        BookOrder order = new BookOrder();
        order.setUsername(username);
        order.setBook(book);
        order.setType(type);
        order.setStatus(OrderStatus.PAID);
        order.setQuantity(quantity);
        order.setUnitPrice(unitPrice);
        order.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        order.setOrderDate(LocalDateTime.now().minusDays(2));
        return order;
    }
}
