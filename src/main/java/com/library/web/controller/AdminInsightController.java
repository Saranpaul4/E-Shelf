package com.library.web.controller;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.library.web.model.BookOrder;
import com.library.web.model.OrderStatus;
import com.library.web.model.OrderType;
import com.library.web.model.SiteUser;
import com.library.web.model.SupportMessage;
import com.library.web.repository.OrderRepository;
import com.library.web.repository.SiteUserRepository;
import com.library.web.repository.SupportMessageRepository;
import com.library.web.service.PresenceService;
import com.library.web.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** What the shop is doing: every order, and one row per customer. */
@Controller
@RequestMapping("/admin")
public class AdminInsightController {

    /** An admin account plus whether they are at the desk right now. */
    public record TeamRow(SiteUser user, boolean online, LocalDateTime lastSeen) {
    }

    /** A copy still out of the shop: which book, in whose hands, and how late it is. */
    public record LoanRow(BookOrder order, String holder, long daysOverdue, BigDecimal lateFee) {
    }

    /**
     * One reader's support conversation summarized for the admin desk. {@code awaitingReply}
     * is true when the last line came from the reader, or from the shop's own helper - an
     * automatic reply is not an answer a librarian gave, so the thread still needs one.
     */
    public record ThreadRow(String username, String name, String lastBody, LocalDateTime lastAt,
                            long messageCount, boolean awaitingReply) {
    }

    private final OrderRepository orderRepository;
    private final SiteUserRepository userRepository;
    private final PresenceService presence;
    private final ShopService shopService;
    private final SupportMessageRepository supportRepository;

    public AdminInsightController(OrderRepository orderRepository, SiteUserRepository userRepository,
                                  PresenceService presence, ShopService shopService,
                                  SupportMessageRepository supportRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.presence = presence;
        this.shopService = shopService;
        this.supportRepository = supportRepository;
    }

    @GetMapping("/orders")
    public String allOrders(Model model) {
        List<BookOrder> orders = orderRepository.findAllByOrderByOrderDateDesc();

        model.addAttribute("orders", orders);
        model.addAttribute("orderCount", orders.size());
        model.addAttribute("buyCount", orders.stream().filter(o -> o.getType() == OrderType.BUY).count());
        model.addAttribute("rentCount", orders.stream().filter(BookOrder::isLiveRental).count());
        model.addAttribute("revenue", orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(BookOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return "admin-orders";
    }

    /** The lending desk: every copy still out on rent and the reader holding it. */
    @GetMapping("/loans")
    public String loans(Model model) {
        List<BookOrder> live = orderRepository.findByStatus(OrderStatus.PAID).stream()
                .filter(BookOrder::isLiveRental)
                .sorted(Comparator.comparing(BookOrder::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Map<String, String> names = userRepository.findAll().stream()
                .collect(Collectors.toMap(SiteUser::getUsername, SiteUser::getDisplayName, (a, b) -> a));

        List<LoanRow> rows = live.stream()
                .map(order -> new LoanRow(order,
                        names.getOrDefault(order.getUsername(), order.getUsername()),
                        shopService.daysOverdue(order),
                        shopService.lateFeeFor(order)))
                .toList();

        model.addAttribute("loans", rows);
        model.addAttribute("loanCount", live.stream().mapToInt(BookOrder::getQuantity).sum());
        model.addAttribute("overdueCount", rows.stream().filter(r -> r.daysOverdue() > 0).count());
        model.addAttribute("feeDue", rows.stream()
                .map(LoanRow::lateFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return "admin-loans";
    }

    /** The support desk: every reader conversation, with the ones that still need an answer on top. */
    @GetMapping("/support")
    public String supportThreads(Model model) {
        List<ThreadRow> threads = new ArrayList<>();
        for (String username : supportRepository.distinctUsernames()) {
            String name = userRepository.findByUsername(username)
                    .map(SiteUser::getDisplayName).orElse(username);
            SupportMessage last = supportRepository.findFirstByUsernameOrderByCreatedAtDesc(username)
                    .orElse(null);
            threads.add(new ThreadRow(username, name,
                    last == null ? "" : last.getBody(),
                    last == null ? null : last.getCreatedAt(),
                    supportRepository.countByUsername(username),
                    last != null && (!last.isFromAdmin() || last.isAutomated())));
        }
        // Awaiting-reply first, then most recent activity.
        threads.sort(Comparator.comparing(ThreadRow::awaitingReply).reversed()
                .thenComparing(ThreadRow::lastAt, Comparator.nullsLast(Comparator.reverseOrder())));

        model.addAttribute("threads", threads);
        model.addAttribute("threadCount", threads.size());
        model.addAttribute("awaitingCount", threads.stream().filter(ThreadRow::awaitingReply).count());
        return "admin-support";
    }

    /** One conversation in full, with the box to answer it from. */
    @GetMapping("/support/thread")
    public String supportThread(@RequestParam String user, Model model) {
        model.addAttribute("messages", supportRepository.findByUsernameOrderByCreatedAtAsc(user));
        model.addAttribute("customer", userRepository.findByUsername(user)
                .map(SiteUser::getDisplayName).orElse(user));
        model.addAttribute("username", user);
        return "admin-support-thread";
    }

    /** A librarian answers. Blank replies are dropped so the thread stays clean. */
    @PostMapping("/support/reply")
    public String reply(@RequestParam String user, @RequestParam String body, RedirectAttributes flash) {
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) {
            flash.addFlashAttribute("adminError", "Write a reply before you send it.");
        } else {
            supportRepository.save(new SupportMessage(user, text, true));
        }
        return "redirect:/admin/support/thread?user=" + URLEncoder.encode(user, StandardCharsets.UTF_8);
    }

    /** The librarian desk: every admin account and who is signed in right now. */
    @GetMapping("/team")
    public String team(Model model) {
        List<TeamRow> rows = new ArrayList<>();
        for (SiteUser admin : userRepository.findByRoleOrderByUsernameAsc("ADMIN")) {
            Instant seen = presence.lastSeenOf(admin.getUsername());
            LocalDateTime localSeen = seen == null ? null : LocalDateTime.ofInstant(seen, ZoneId.systemDefault());
            rows.add(new TeamRow(admin, presence.isOnline(admin.getUsername()), localSeen));
        }

        long onlineCount = rows.stream().filter(TeamRow::online).count();

        model.addAttribute("team", rows);
        model.addAttribute("totalAdmins", rows.size());
        model.addAttribute("onlineAdmins", onlineCount);
        model.addAttribute("offlineAdmins", rows.size() - onlineCount);
        model.addAttribute("onlineWindowMinutes", PresenceService.ONLINE_WINDOW.toMinutes());
        return "admin-team";
    }
}
