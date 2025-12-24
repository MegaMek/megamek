/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.common.annotations.Nullable;

/**
 * Represents a structured reference to a rulebook page. Contains the page number, book abbreviation, and optionally the
 * rules text itself.
 *
 * <p>This class can parse legacy format strings like "205, TM" or "94, TO: AU&amp;E"
 * and provides methods to look up the associated {@link SourceBook} for display.
 *
 * @param pageNumber       The page number or range (e.g., "205" or "204-206")
 * @param bookAbbreviation The book abbreviation (e.g., "TM", "TO: AU&amp;E")
 * @param rulesText        Optional text of the actual rules (for future use)
 */
public record RulesRef(
      String pageNumber,
      String bookAbbreviation,
      @Nullable String rulesText
) {

    // Pattern to parse legacy format: "page, book" where page can be a number or range
    // Examples: "205, TM", "94, TO: AU&E", "204-206, IO"
    private static final Pattern LEGACY_PATTERN = Pattern.compile(
          "^\\s*(\\d+(?:-\\d+)?)\\s*,\\s*(.+?)\\s*$"
    );

    /**
     * Creates a RulesRef with just page number and book abbreviation.
     *
     * @param pageNumber       The page number or range
     * @param bookAbbreviation The book abbreviation
     */
    public RulesRef(String pageNumber, String bookAbbreviation) {
        this(pageNumber, bookAbbreviation, null);
    }

    /**
     * Parses a single legacy format rules reference string.
     *
     * @param legacy The legacy format string (e.g., "205, TM")
     *
     * @return The parsed RulesRef, or empty if the string cannot be parsed
     */
    public static Optional<RulesRef> parse(@Nullable String legacy) {
        if ((legacy == null) || legacy.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = LEGACY_PATTERN.matcher(legacy);
        if (matcher.matches()) {
            String pageNumber = matcher.group(1);
            String bookAbbreviation = matcher.group(2);
            return Optional.of(new RulesRef(pageNumber, bookAbbreviation));
        }

        return Optional.empty();
    }

    /**
     * Parses multiple rules references from a legacy format string. References can be separated by semicolons or be a
     * single reference.
     *
     * @param legacy The legacy format string (e.g., "205, TM" or "205, TM; 94, TO: AU&amp;E")
     *
     * @return A list of parsed RulesRef objects (may be empty if none can be parsed)
     */
    public static List<RulesRef> parseMultiple(@Nullable String legacy) {
        if ((legacy == null) || legacy.isBlank()) {
            return Collections.emptyList();
        }

        List<RulesRef> results = new ArrayList<>();

        // Split on semicolons for multiple references
        String[] parts = legacy.split(";");
        for (String part : parts) {
            parse(part.trim()).ifPresent(results::add);
        }

        return results;
    }

    /**
     * Attempts to find the SourceBook associated with this rules reference.
     *
     * @return The SourceBook if found, empty otherwise
     */
    public Optional<SourceBook> getSourceBook() {
        SourceBooks sourceBooks = new SourceBooks();
        return sourceBooks.findByAbbreviation(bookAbbreviation);
    }

    /**
     * Formats this reference for display, using the full book title if available.
     *
     * @return A display string like "TechManual, p. 205" or "TM, p. 205" if book not found
     */
    public String toDisplayString() {
        Optional<SourceBook> sourceBook = getSourceBook();
        String bookName = sourceBook.map(SourceBook::getTitle).orElse(bookAbbreviation);
        return bookName + ", p. " + pageNumber;
    }

    /**
     * Formats this reference back to legacy format for storage.
     *
     * @return The legacy format string (e.g., "205, TM")
     */
    public String toLegacyString() {
        return pageNumber + ", " + bookAbbreviation;
    }

    /**
     * Converts a list of RulesRef objects to a legacy format string.
     *
     * @param refs The list of references to convert
     *
     * @return The legacy format string with semicolon separators
     */
    public static String toMultipleLegacyString(List<RulesRef> refs) {
        if ((refs == null) || refs.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < refs.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(refs.get(i).toLegacyString());
        }
        return sb.toString();
    }

    /**
     * Returns the MUL URL for this reference if the associated SourceBook has one.
     *
     * @return The MUL URL, or empty if not available
     */
    public Optional<String> getMulUrl() {
        return getSourceBook().map(SourceBook::getMul_url);
    }

    @Override
    public String toString() {
        return toLegacyString();
    }

    // ==================== YAML Serialization ====================

    /** YAML field name for page number */
    public static final String YAML_PAGE = "page";
    /** YAML field name for book abbreviation */
    public static final String YAML_BOOK = "book";
    /** YAML field name for rules text */
    public static final String YAML_TEXT = "text";

    /**
     * Converts this RulesRef to a Map suitable for YAML serialization.
     *
     * @return A map containing the structured rules reference data
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(YAML_PAGE, pageNumber);
        map.put(YAML_BOOK, bookAbbreviation);
        if ((rulesText != null) && !rulesText.isBlank()) {
            map.put(YAML_TEXT, rulesText);
        }
        return map;
    }

    /**
     * Creates a RulesRef from a Map (YAML deserialization).
     *
     * @param map The map containing the rules reference data
     *
     * @return The RulesRef, or empty if the map is invalid
     */
    @SuppressWarnings("unchecked")
    public static Optional<RulesRef> fromMap(@Nullable Object mapObject) {
        if (!(mapObject instanceof Map)) {
            return Optional.empty();
        }

        Map<String, Object> map = (Map<String, Object>) mapObject;
        Object pageObj = map.get(YAML_PAGE);
        Object bookObj = map.get(YAML_BOOK);

        if ((pageObj == null) || (bookObj == null)) {
            return Optional.empty();
        }

        String page = pageObj.toString();
        String book = bookObj.toString();
        String text = null;

        Object textObj = map.get(YAML_TEXT);
        if (textObj != null) {
            text = textObj.toString();
        }

        return Optional.of(new RulesRef(page, book, text));
    }

    /**
     * Converts a list of RulesRef objects to a list of Maps for YAML serialization.
     *
     * @param refs The list of references to convert
     *
     * @return A list of maps suitable for YAML serialization
     */
    public static List<Map<String, Object>> toMapList(@Nullable List<RulesRef> refs) {
        if ((refs == null) || refs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (RulesRef ref : refs) {
            result.add(ref.toMap());
        }
        return result;
    }

    /**
     * Creates a list of RulesRef objects from a list of Maps (YAML deserialization). Also handles legacy string format
     * for backward compatibility.
     *
     * @param data The data to parse - can be a List of Maps, a single Map, or a legacy String
     *
     * @return A list of RulesRef objects (may be empty)
     */
    @SuppressWarnings("unchecked")
    public static List<RulesRef> fromYamlData(@Nullable Object data) {
        if (data == null) {
            return Collections.emptyList();
        }

        // Handle legacy string format
        if (data instanceof String legacyString) {
            return parseMultiple(legacyString);
        }

        // Handle list of maps (new structured format)
        if (data instanceof List<?> list) {
            List<RulesRef> result = new ArrayList<>();
            for (Object item : list) {
                fromMap(item).ifPresent(result::add);
            }
            return result;
        }

        // Handle single map
        if (data instanceof Map) {
            return fromMap(data).map(List::of).orElse(Collections.emptyList());
        }

        return Collections.emptyList();
    }
}
