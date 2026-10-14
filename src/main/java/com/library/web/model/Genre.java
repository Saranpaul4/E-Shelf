package com.library.web.model;

import java.util.Locale;

/**
 * The shelves the catalogue is split into. Saved as text (@Enumerated(EnumType.STRING)),
 * so re-ordering the constants later cannot shuffle the numbers under existing rows.
 */
public enum Genre {
    COMIC("Comics"),
    NOVEL("Novels"),
    NON_FICTION("Non-fiction"),
    BIOGRAPHY("Biography"),
    TECHNOLOGY("Programming & Tech");

    private final String label;

    Genre(String label) {
        this.label = label;
    }

    /** What the shop prints above the shelf and inside the filter chips. */
    public String getLabel() {
        return label;
    }

    /** Lower-case form used in links: /shop?genre=comic */
    public String getSlug() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Reads a link slug back into a genre. Unknown or blank means "show every shelf". */
    public static Genre fromSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        String wanted = slug.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        for (Genre genre : values()) {
            if (genre.name().equals(wanted)) {
                return genre;
            }
        }
        return null;
    }
}
