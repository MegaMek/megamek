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
 * MekWarrior, BattleMek, `Mek and AeroTek are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MekWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.clientGUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.boardview.overlay.BoardToastOverlay;

/**
 * Formats server-side report HTML for display in the {@link BoardToastOverlay}. Owns the regex/i18n filters and the
 * report-splitting logic used to turn a single {@code gameReport} burst into an ordered list of per-event toast
 * bodies.
 *
 * <p>This class is pure formatting/text logic and has no Swing or game-state dependencies, which keeps it cheap to
 * test and lets {@link ClientGUI} keep responsibility for the UI lifecycle (drip-feed Swing {@code Timer}, queue
 * management, and the actual {@code addToast} calls).</p>
 */
public final class ReportToastFormatter {

    /** Maximum number of report-event toasts produced from a single {@code gameReport} burst before truncation. */
    public static final int MAX_TOASTS_PER_BURST = 8;

    private static final Pattern REPORT_PHASE_HEADER = Pattern.compile("<[Bb]>([^<]+)</[Bb]>");
    private static final Pattern REPORTING_ERROR_PREVIEW = Pattern.compile("\\[Reporting Error for message ID \\d+]");
    /** Empty team/player summary "labels" that precede BV detail lines (which are themselves filtered as noise). */
    private static final Pattern TEAM_SUMMARY_LABEL = Pattern.compile(".*Team \\d+\\)?:");

    /**
     * Substrings that mark a report entry as routine round-summary "noise" (planetary conditions, end-of-round BV
     * snapshots, turn order). These show up in every initiative report alongside the actual events and would otherwise
     * dominate the kill-feed. Full content remains visible in the report panel.
     *
     * <p>Loaded from the {@code ClientGUI.toastNoisePrefixes} i18n key (pipe-separated). Translators can override the
     * list per locale to match their localized report text; if the resource is missing or empty the filter no-ops
     * (entries pass through as normal toasts) rather than crashing.</p>
     */
    private static final String[] REPORT_NOISE_SUBSTRINGS = loadNoisePrefixes();

    private ReportToastFormatter() {
    }

    private static String[] loadNoisePrefixes() {
        String key = "ClientGUI.toastNoisePrefixes";
        String raw = Messages.getString(key);
        // Messages.getString returns "!key!" when the resource is missing - treat that as "no filter" rather than
        // letting the sentinel become a literal needle that nothing will ever match.
        if (raw.isBlank() || raw.equals("!" + key + "!")) {
            return new String[0];
        }
        return raw.split("\\|");
    }

    /**
     * Flattens an arbitrary message to a single line of plain text suitable for the toast overlay. The overlay renders
     * text via {@link megamek.client.ui.util.StringDrawer} which has no HTML support and no line-wrapping, so
     * server-side report markup (such as {@code <span class='report-entry'>}, {@code <br>}, {@code <B>}, and
     * base64-embedded portrait {@code <img>} tags) and embedded newlines would otherwise appear as literal glyphs.
     * Plain-text inputs without markup or whitespace anomalies pass through unchanged.
     */
    public static String normalizeToastText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?i)<br\\s*/?>", " ")
              .replaceAll("(?i)</?p\\s*[^>]*>", " ")
              .replaceAll("<[^>]*>", "")
              .replace("&nbsp;", " ")
              .replace("&lt;", "<")
              .replace("&gt;", ">")
              .replace("&quot;", "\"")
              .replace("&#39;", "'")
              .replace("&apos;", "'")
              .replace("&amp;", "&")
              .replaceAll("\\s+", " ")
              .trim();
    }

    /**
     * Splits a server-side report HTML stream into one toast body per individual {@code <span class='report-entry'>}
     * block, so each game event can scroll past the player as its own kill-feed-style notification. Returns the bodies
     * in the order they appear; the caller is responsible for queueing/drip-feeding them to the overlay.
     *
     * <p>The first toast body in a burst is prefixed with the phase name extracted from the report's own header (e.g.,
     * "Movement Phase: Building #10010 collapses ..."). The phase-header entry itself - which would otherwise render as
     * "Movement Phase --------------------" - is suppressed in favor of using its phase name as the prefix. If the
     * report contains no extractable phase header, the supplied {@code defaultPrefix} (an i18n string such as "Movement
     * Report") is used instead. Subsequent toasts in the burst are returned bare to avoid label repetition.</p>
     *
     * <p>Three additional filters trim the kill-feed to actual gameplay events:</p>
     * <ul>
     *   <li>Entries containing only a {@code [Reporting Error for message ID NNN]} marker (internal diagnostic output)
     *       are suppressed.</li>
     *   <li>Entries matching any {@link #REPORT_NOISE_SUBSTRINGS} pattern (planetary conditions, BV summaries, turn
     *       order) are suppressed.</li>
     *   <li>If a single burst would exceed {@link #MAX_TOASTS_PER_BURST}, the surplus entries are collapsed into one
     *       final overflow toast like {@code "+N more events - see report panel"}.</li>
     * </ul>
     *
     * @return ordered list of toast bodies; empty if the report is null/empty or fully filtered out
     */
    public static List<String> formatReport(String defaultPrefix, String report) {
        List<String> toasts = new ArrayList<>();
        if (report == null || report.isEmpty()) {
            return toasts;
        }
        String[] entries = report.split("<span class=['\"]report-entry['\"]>");
        String phasePrefix = null;
        boolean firstQueued = false;
        int suppressedOverflowCount = 0;
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String preview = entry.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").trim();
            if (preview.isEmpty()) {
                continue;
            }
            // Suppress diagnostic markers from the report layer (e.g., when a message ID is missing from
            // report-messages.properties).
            if (REPORTING_ERROR_PREVIEW.matcher(preview).matches()) {
                continue;
            }
            // Capture the phase name from header entries. A header entry is one whose only content is the bolded
            // phase name plus (optionally) the dashed divider - e.g. "<B>Movement Phase</B><br>------" or
            // "<B>Initiative Phase for Round #2</B>". Detection is structural rather than text-based so it works in
            // any locale (e.g. German "Bewegungsphase") without hard-coding the word "phase".
            Matcher header = REPORT_PHASE_HEADER.matcher(entry);
            if (header.find()) {
                String boldText = header.group(1).trim();
                String remainder = preview.replace(boldText, "").replace("-", "").trim();
                if (remainder.isEmpty()) {
                    phasePrefix = boldText;
                    continue;
                }
            }
            // Pure-divider entries (just dashes) - skip.
            if (preview.replace("-", "").trim().isEmpty()) {
                continue;
            }
            // Drop routine round-summary entries (planetary conditions, BV snapshots, turn order). Players don't need
            // these scrolling past as toasts; they remain in the report panel for reference.
            if (Arrays.stream(REPORT_NOISE_SUBSTRINGS).anyMatch(preview::contains)) {
                continue;
            }
            // Drop empty "Team N:" / "Player (Team N):" labels - they precede BV detail lines that we already filter,
            // so the label on its own conveys nothing.
            if (TEAM_SUMMARY_LABEL.matcher(preview).matches()) {
                continue;
            }
            // Cap toasts per burst. Once full, count remaining qualifying entries so we can summarize them in a single
            // overflow toast rather than firing dozens of notifications for one event chain.
            if (toasts.size() >= MAX_TOASTS_PER_BURST) {
                suppressedOverflowCount++;
                continue;
            }
            String prefixForFirst = (phasePrefix != null) ? phasePrefix : defaultPrefix;
            toasts.add(firstQueued ? entry : (prefixForFirst + ": " + entry));
            firstQueued = true;
        }
        if (suppressedOverflowCount > 0) {
            toasts.add("+" + suppressedOverflowCount + " more events - see report panel");
        }
        return toasts;
    }
}
