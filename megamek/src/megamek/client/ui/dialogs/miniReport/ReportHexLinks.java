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
package megamek.client.ui.dialogs.miniReport;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nullable;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.game.IGame;

/**
 * Turns hex numbers in report HTML into clickable links, and reads those links back into a board location. A hex
 * number is linked only when the word "hex" comes right before it, as in "Skids into Woods in hex 0612" or "Hex:
 * 0808". A trailing "(Board Id: 2)" is carried into the link so multi-board games center on the right board.
 */
public final class ReportHexLinks {

    /** Prefix for hex hyperlinks, followed by the hex number and, optionally, ":" and the board id. */
    public static final String HEX_LINK = "#hex:";

    private static final Pattern TAG = Pattern.compile("<[^>]*>");

    /**
     * Group 1 is the leading word, group 2 the hex number, group 3 the optional board id suffix and group 4 the board
     * id itself.
     */
    private static final Pattern HEX_REFERENCE = Pattern.compile(
          "\\b([Hh]ex(?:es|\\(es\\))?:?\\s+)(\\d{4,6})(?!\\d)(\\s+\\(Board Id: (\\d+)\\))?");

    private ReportHexLinks() {
    }

    /**
     * Wraps every "hex 1234" reference in the given report HTML in a hex link. Text inside tags and inside existing
     * links (unit names, tooltips) is left untouched.
     *
     * @param html the report HTML, may be {@code null}
     *
     * @return the HTML with hex numbers linked, or {@code null} when {@code html} is {@code null}
     */
    public static @Nullable String linkHexes(@Nullable String html) {
        if ((html == null) || html.isEmpty()) {
            return html;
        }
        StringBuilder result = new StringBuilder(html.length() + 64);
        Matcher tagMatcher = TAG.matcher(html);
        boolean insideLink = false;
        int textStart = 0;
        while (tagMatcher.find()) {
            appendText(result, html.substring(textStart, tagMatcher.start()), insideLink);
            String tag = tagMatcher.group();
            result.append(tag);
            if (isLinkOpen(tag)) {
                insideLink = true;
            } else if (tag.equalsIgnoreCase("</a>")) {
                insideLink = false;
            }
            textStart = tagMatcher.end();
        }
        appendText(result, html.substring(textStart), insideLink);
        return result.toString();
    }

    private static boolean isLinkOpen(String tag) {
        return (tag.length() > 2)
              && (tag.charAt(1) == 'a' || tag.charAt(1) == 'A')
              && (tag.charAt(2) == ' ' || tag.charAt(2) == '>');
    }

    private static void appendText(StringBuilder result, String text, boolean insideLink) {
        if (insideLink) {
            result.append(text);
            return;
        }
        Matcher hexMatcher = HEX_REFERENCE.matcher(text);
        while (hexMatcher.find()) {
            String hexNumber = hexMatcher.group(2);
            String boardIdSuffix = hexMatcher.group(3);
            String href = HEX_LINK + hexNumber;
            if (boardIdSuffix != null) {
                href += ":" + hexMatcher.group(4);
            }
            String replacement = hexMatcher.group(1)
                  + "<a href='" + href + "'>" + hexNumber + "</a>"
                  + ((boardIdSuffix == null) ? "" : boardIdSuffix);
            hexMatcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        hexMatcher.appendTail(result);
    }

    /**
     * Reads a hex link (without the {@link #HEX_LINK} prefix) back into a location on one of the game's boards. Hex
     * numbers on maps wider or taller than 99 hexes can be split more than one way ("10106" is 101,06 or 10,106), so
     * each split is tried and the first one that both prints back as the same number and lies on the board wins.
     *
     * @param linkBody the link text after the prefix, such as "3834" or "3834:2"; may be {@code null}
     * @param game     the game whose boards are checked; may be {@code null}
     *
     * @return the location, or empty if the link does not name a hex on a board of this game
     */
    public static Optional<BoardLocation> parseHexLink(@Nullable String linkBody, @Nullable IGame game) {
        if ((linkBody == null) || (game == null)) {
            return Optional.empty();
        }
        String hexNumber = linkBody;
        int boardId = 0;
        int separator = linkBody.indexOf(':');
        if (separator >= 0) {
            hexNumber = linkBody.substring(0, separator);
            try {
                boardId = Integer.parseInt(linkBody.substring(separator + 1));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }
        for (int split = 2; split <= hexNumber.length() - 2; split++) {
            Coords coords;
            try {
                coords = new Coords(Integer.parseInt(hexNumber.substring(0, split)) - 1,
                      Integer.parseInt(hexNumber.substring(split)) - 1);
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
            if (coords.getBoardNum().equals(hexNumber) && game.hasBoardLocation(coords, boardId)) {
                return Optional.of(BoardLocation.of(coords, boardId));
            }
        }
        return Optional.empty();
    }
}
