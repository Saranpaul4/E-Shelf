package com.library.web.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Keeps the pictures readers upload for their profile, in a folder next to the app.
 * Only this class builds the file name, so nothing a browser sends can decide where
 * a file lands on the disk.
 */
@Service
public class PhotoStorage {

    /** Anything bigger than this is not a profile picture, it is a mistake. */
    public static final long MAX_BYTES = 2L * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private static final Logger log = LoggerFactory.getLogger(PhotoStorage.class);

    private final Path folder;

    public PhotoStorage() throws IOException {
        this.folder = Paths.get("uploads").toAbsolutePath().normalize();
        Files.createDirectories(this.folder);
    }

    /** Where the files live, so Spring can hand them back out over HTTP. */
    public Path folder() {
        return folder;
    }

    /**
     * Saves the upload and returns the address a page can put in an img tag.
     * Throws IllegalArgumentException with a message meant for the customer.
     */
    public String store(String username, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Pick a picture first.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("That picture is over 2 MB — please choose a smaller one.");
        }

        String contentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Upload a JPG, PNG or WEBP picture.");
        }

        String extension = extensionOf(file.getOriginalFilename());
        String name = slug(username) + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        Path target = folder.resolve(name).normalize();
        if (!target.startsWith(folder)) {
            throw new IllegalArgumentException("That file name is not one we can use.");
        }

        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return "/uploads/" + name;
    }

    /**
     * Removes a picture we stored earlier. Anything that is not a file this class
     * named, or that sits outside the uploads folder, is left well alone.
     */
    public void remove(String webPath) {
        if (webPath == null || !webPath.startsWith("/uploads/")) {
            return;
        }
        String fileName = webPath.substring("/uploads/".length());
        if (!fileName.matches("[a-z0-9-]{3,60}\\.(jpg|jpeg|png|webp)")) {
            return;
        }
        try {
            Files.deleteIfExists(folder.resolve(fileName).normalize());
        } catch (IOException e) {
            log.warn("Could not delete profile picture {}", fileName, e);
        }
    }

    /** "My Book.PDF" to "pdf" — only the letters we actually allow get through. */
    private String extensionOf(String originalName) {
        String dot = originalName == null ? "" : originalName.toLowerCase(Locale.ROOT);
        int lastDot = dot.lastIndexOf('.');
        String extension = lastDot < 0 ? "" : dot.substring(lastDot + 1);
        return ALLOWED_EXTENSIONS.contains(extension) ? extension : "jpg";
    }

    /** A username turned into something safe to put in a file name. */
    private String slug(String username) {
        String cleaned = username == null ? "reader" : username.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return cleaned.isEmpty() ? "reader" : cleaned.substring(0, Math.min(cleaned.length(), 20));
    }
}
