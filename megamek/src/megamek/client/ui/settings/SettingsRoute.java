/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.settings;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Describes one destination in a searchable settings navigation tree. A route owns its stable identifier, resolved
 * display path, optional search aliases, details-panel policy, and searchable section text harvested from its page.
 */
public final class SettingsRoute {
    private final String id;
    private final List<String> path;
    private final List<String> pathIds;
    private final List<String> searchAliases;
    private final boolean showDetailsPanel;
    private final String searchableText;
    private String sectionSearchText = "";

    /**
     * Creates a top-level or nested route with no additional search aliases and a visible details panel.
     *
     * @param id   the stable route identifier
     * @param path the resolved display path from the navigation root to this route
     */
    public SettingsRoute(String id, List<String> path) {
        this(id, path, List.of(), true);
    }

    /**
     * @param id               the stable route identifier
     * @param path             the resolved display path from the navigation root to this route
     * @param searchAliases    additional text that should match this route, such as resource keys or legacy names
     * @param showDetailsPanel whether the route normally shows the settings details panel
     */
    public SettingsRoute(String id, List<String> path, List<String> searchAliases, boolean showDetailsPanel) {
        this(id, path, derivePathIds(id, path.size()), searchAliases, showDetailsPanel);
    }

    /**
     * @param id               the stable route identifier
     * @param path             the resolved display path from the navigation root to this route
     * @param pathIds          stable identity for every element of {@code path}
     * @param searchAliases    additional text that should match this route
    * @param showDetailsPanel fallback details-panel policy for content that is not a {@link SettingsPagePanel}
     */
    public SettingsRoute(String id, List<String> path, List<String> pathIds, List<String> searchAliases,
          boolean showDetailsPanel) {
        this.id = Objects.requireNonNull(id);
        this.path = List.copyOf(path);
        this.pathIds = List.copyOf(pathIds);
        this.searchAliases = List.copyOf(searchAliases);
        this.showDetailsPanel = showDetailsPanel;
        if (this.id.isBlank()) {
            throw new IllegalArgumentException("A settings route id cannot be blank");
        }
        if (this.path.isEmpty()) {
            throw new IllegalArgumentException("A settings route must have at least one path element");
        }
        if (this.path.size() != this.pathIds.size()) {
            throw new IllegalArgumentException("A settings route requires one path ID per display-path element");
        }
        if (this.path.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("A settings route path cannot contain blank labels");
        }
        if (this.pathIds.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("A settings route path cannot contain blank IDs");
        }
        searchableText = normalizeSearchText(String.join(" ", this.path) + ' ' + this.id + ' '
              + String.join(" ", this.searchAliases));
    }

    public String getId() {
        return id;
    }

    public List<String> getPath() {
        return path;
    }

    public List<String> getPathIds() {
        return pathIds;
    }

    public List<String> getSearchAliases() {
        return searchAliases;
    }

    public boolean shouldShowDetailsPanel() {
        return showDetailsPanel;
    }

    public boolean isTopLevelRoute() {
        return path.size() == 1;
    }

    /**
     * Adds resolved section title and summary text to this route's search index.
     *
     * @param text the raw section text, or {@code null} to clear it
     */
    public void setSectionSearchText(String text) {
        sectionSearchText = text == null ? "" : normalizeSearchText(text);
    }

    /**
     * @param normalizedFilter a filter already normalized by {@link #normalizeSearchText(String)}
     *
     * @return {@code true} when every non-blank token occurs in the route or section search text
     */
    public boolean matches(String normalizedFilter) {
        if (normalizedFilter.isBlank()) {
            return true;
        }

        for (String token : normalizedFilter.split("\\s+")) {
            if (!token.isBlank() && !searchableText.contains(token) && !sectionSearchText.contains(token)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests one section against a filter while allowing tokens already satisfied by the route itself.
     *
     * @param rawSectionText  the section title and summary text
     * @param normalizedFilter the normalized active filter
     *
     * @return {@code true} when every token occurs in either the route or this section
     */
    public boolean sectionMatches(String rawSectionText, String normalizedFilter) {
        String normalizedSectionText = normalizeSearchText(rawSectionText);
        for (String token : normalizedFilter.split("\\s+")) {
            if (!token.isBlank() && !searchableText.contains(token) && !normalizedSectionText.contains(token)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Normalizes text for case-insensitive token matching.
     *
     * @param text the text to normalize
     *
    * @return lowercase Unicode letters and digits separated by single spaces, with Latin combining accents removed
     */
    public static String normalizeSearchText(String text) {
        String decomposed = Normalizer.normalize(Objects.requireNonNull(text), Normalizer.Form.NFD);
        StringBuilder normalized = new StringBuilder(decomposed.length());
        Character.UnicodeScript baseScript = Character.UnicodeScript.COMMON;
        for (int offset = 0; offset < decomposed.length();) {
            int codePoint = decomposed.codePointAt(offset);
            offset += Character.charCount(codePoint);
            int type = Character.getType(codePoint);
            boolean combiningMark = type == Character.NON_SPACING_MARK
                  || type == Character.COMBINING_SPACING_MARK
                  || type == Character.ENCLOSING_MARK;
            if (!combiningMark || baseScript != Character.UnicodeScript.LATIN) {
                normalized.appendCodePoint(codePoint);
            }
            if (!combiningMark) {
                baseScript = Character.UnicodeScript.of(codePoint);
            }
        }
        String lowerCaseText = Normalizer.normalize(normalized, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        StringBuilder searchableText = new StringBuilder(lowerCaseText.length());
        boolean separatorPending = false;
        for (int offset = 0; offset < lowerCaseText.length();) {
            int codePoint = lowerCaseText.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isLetterOrDigit(codePoint)) {
                if (separatorPending && !searchableText.isEmpty()) {
                    searchableText.append(' ');
                }
                searchableText.appendCodePoint(codePoint);
                separatorPending = false;
            } else if (!searchableText.isEmpty()) {
                separatorPending = true;
            }
        }
        return searchableText.toString();
    }

    private static List<String> derivePathIds(String id, int pathSize) {
        String[] segments = Objects.requireNonNull(id).split("\\.");
        List<String> ids = new ArrayList<>(pathSize);
        if (segments.length == pathSize) {
            StringBuilder current = new StringBuilder();
            for (String segment : segments) {
                if (!current.isEmpty()) {
                    current.append('.');
                }
                current.append(segment);
                ids.add(current.toString());
            }
        } else {
            for (int index = 0; index < pathSize; index++) {
                ids.add(id + '#' + index);
            }
        }
        return ids;
    }

    @Override
    public String toString() {
        return id;
    }
}
