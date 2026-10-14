package com.library.web.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.library.web.model.BookOrder;
import com.library.web.model.BookReview;
import com.library.web.model.OrderStatus;
import com.library.web.model.ProfileAvatar;
import com.library.web.model.SiteUser;
import com.library.web.repository.BookReviewRepository;
import com.library.web.repository.OrderRepository;
import com.library.web.repository.SiteUserRepository;
import com.library.web.service.PhotoStorage;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The reader's own page: their picture, their details, and everything they have
 * bought or rented. There is no /profile/{username} on purpose — this always shows
 * whoever is signed in, so nobody can peek at anybody else.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final SiteUserRepository userRepository;
    private final OrderRepository orderRepository;
    private final BookReviewRepository reviewRepository;
    private final PhotoStorage photoStorage;

    public ProfileController(SiteUserRepository userRepository, OrderRepository orderRepository,
                             BookReviewRepository reviewRepository, PhotoStorage photoStorage) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.photoStorage = photoStorage;
    }

    @GetMapping
    public String profile(@RequestParam(required = false) String edit,
                          Authentication me, Model model) {
        SiteUser user = userRepository.findByUsername(me.getName()).orElseThrow();
        List<BookOrder> orders = orderRepository.findByUsernameOrderByOrderDateDesc(user.getUsername());

        model.addAttribute("user", user);
        model.addAttribute("characters", ProfileAvatar.values());
        boolean hasPicture = user.getAvatar() != null || user.getPhotoPath() != null;
        boolean editing = "1".equals(edit);
        model.addAttribute("hasPicture", hasPicture);
        model.addAttribute("editing", editing);
        // One card holds both the picture picker and the details. It stays closed until the
        // reader asks for it with "?edit=1" — except for a brand new account, which has
        // nothing to show yet, so it opens on its own.
        model.addAttribute("showEditor", editing || !hasPicture);
        model.addAttribute("orders", orders);
        model.addAttribute("reviews", reviewRepository.findByUsernameOrderByCreatedAtDesc(user.getUsername()));
        model.addAttribute("buyCount", countType(orders, "BUY"));
        model.addAttribute("rentCount", countType(orders, "RENT"));
        model.addAttribute("totalSpend", orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .map(BookOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return "profile";
    }

    @PostMapping("/details")
    public String saveDetails(@RequestParam String fullName,
                              @RequestParam String email,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String address,
                              Authentication me,
                              RedirectAttributes flash) {
        SiteUser user = userRepository.findByUsername(me.getName()).orElseThrow();

        String name = trim(fullName);
        String mail = trim(email).toLowerCase();
        String telephone = trim(phone);
        String places = trim(address);

        if (name.length() < 2 || name.length() > 60) {
            flash.addFlashAttribute("profileError", "Please give a full name between 2 and 60 characters.");
            return "redirect:/profile?edit=1";
        }
        if (!mail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")) {
            flash.addFlashAttribute("profileError", "That does not look like an email address.");
            return "redirect:/profile?edit=1";
        }
        // Someone else may already own this address, but re-saving your own is fine.
        if (!mail.equals(user.getEmail()) && userRepository.existsByEmail(mail)) {
            flash.addFlashAttribute("profileError", "That email belongs to another account.");
            return "redirect:/profile?edit=1";
        }
        if (telephone.length() > 40 || places.length() > 160) {
            flash.addFlashAttribute("profileError", "Phone and address are a bit too long.");
            return "redirect:/profile?edit=1";
        }

        user.setFullName(name);
        user.setEmail(mail);
        user.setPhone(telephone.isEmpty() ? null : telephone);
        user.setAddress(places.isEmpty() ? null : places);
        userRepository.save(user);

        flash.addFlashAttribute("profileMessage", "Your details are saved.");
        return "redirect:/profile";
    }

    @PostMapping("/avatar")
    public String chooseCharacter(@RequestParam String character,
                                  Authentication me,
                                  RedirectAttributes flash) {
        SiteUser user = userRepository.findByUsername(me.getName()).orElseThrow();
        ProfileAvatar picked = ProfileAvatar.fromFileKey(character);

        if (picked == null) {
            flash.addFlashAttribute("profileError", "Pick one of the characters on the shelf.");
            return "redirect:/profile?edit=1";
        }

        // A character and an uploaded photo cannot both win, so the photo goes.
        photoStorage.remove(user.getPhotoPath());
        user.setPhotoPath(null);
        user.setAvatar(picked);
        userRepository.save(user);

        flash.addFlashAttribute("profileMessage", picked.getCharacter() + " is now your profile picture.");
        return "redirect:/profile";
    }

    @PostMapping("/photo")
    public String uploadPhoto(@RequestParam("photo") MultipartFile photo,
                              Authentication me,
                              RedirectAttributes flash) {
        SiteUser user = userRepository.findByUsername(me.getName()).orElseThrow();
        try {
            String path = photoStorage.store(user.getUsername(), photo);
            photoStorage.remove(user.getPhotoPath());
            // The character stays underneath: take the photo away and it comes back.
            user.setPhotoPath(path);
            userRepository.save(user);
            flash.addFlashAttribute("profileMessage", "Your own photo is on the profile.");
            return "redirect:/profile";
        } catch (IllegalArgumentException problem) {
            flash.addFlashAttribute("profileError", problem.getMessage());
        } catch (IOException problem) {
            flash.addFlashAttribute("profileError", "We could not save that picture. Try again.");
        }
        // A rejected picture leaves the editor open, so the reader can pick another one.
        return "redirect:/profile?edit=1";
    }

    @PostMapping("/photo/remove")
    public String removePhoto(Authentication me, RedirectAttributes flash) {
        SiteUser user = userRepository.findByUsername(me.getName()).orElseThrow();
        photoStorage.remove(user.getPhotoPath());
        user.setPhotoPath(null);
        userRepository.save(user);
        flash.addFlashAttribute("profileMessage", "Photo removed.");
        return "redirect:/profile";
    }

    private long countType(List<BookOrder> orders, String type) {
        return orders.stream().filter(order -> order.getType().name().equals(type))
                .count();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
