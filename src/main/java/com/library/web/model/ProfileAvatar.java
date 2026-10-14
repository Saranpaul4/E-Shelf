package com.library.web.model;

import java.util.Locale;

/**
 * The story characters a reader can wear as their profile picture. Every one of them
 * comes from a book whose illustrations are out of copyright, and the pictures live in
 * static/img/avatars, so the shop is not borrowing anybody's modern film likeness.
 * Saved as text (@Enumerated(EnumType.STRING)), exactly like {@link Genre}.
 */
public enum ProfileAvatar {

    HOLMES("holmes", "Sherlock Holmes", "Sidney Paget, 1893"),
    ALICE("alice", "Alice", "John Tenniel, 1871"),
    PETER_PAN("peter-pan", "Peter Pan", "Peter and Wendy, 1911"),
    MOWGLI("mowgli", "Mowgli", "The Jungle Book, 1895"),
    DOROTHY("dorothy", "Dorothy", "W. W. Denslow, 1900"),
    QUIXOTE("quixote", "Don Quixote", "Gustave Dore, 1863"),
    ROBIN_HOOD("robin-hood", "Robin Hood", "Howard Pyle, 1883"),
    SILVER("silver", "Long John Silver", "Treasure Island, 1911"),
    DORIAN("dorian", "Dorian Gray", "Le portrait, 1928");

    private final String fileKey;
    private final String character;
    private final String credit;

    ProfileAvatar(String fileKey, String character, String credit) {
        this.fileKey = fileKey;
        this.character = character;
        this.credit = credit;
    }

    public String getFileKey() {
        return fileKey;
    }

    /** The name a reader sees under the picture. */
    public String getCharacter() {
        return character;
    }

    /** Who drew it, shown small so the credit travels with the art. */
    public String getCredit() {
        return credit;
    }

    public String getImageUrl() {
        return "/img/avatars/" + fileKey + ".jpg";
    }

    /** Reads the value a form posts back. Unknown or blank means "no character picked". */
    public static ProfileAvatar fromFileKey(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return null;
        }
        String wanted = fileKey.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (ProfileAvatar choice : values()) {
            if (choice.fileKey.equals(wanted)) {
                return choice;
            }
        }
        return null;
    }
}
