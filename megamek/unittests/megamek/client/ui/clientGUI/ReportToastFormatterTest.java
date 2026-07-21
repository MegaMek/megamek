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
package megamek.client.ui.clientGUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ReportToastFormatter}, with emphasis on the already-toasted history that stops the server's
 * whole-phase special reports from replaying events the player has already seen.
 */
class ReportToastFormatterTest {

    private static final String DEFAULT_PREFIX = "Movement Report";

    /**
     * Builds report HTML in the shape the server sends: one {@code <span class='report-entry'>} block per event.
     *
     * @param entries the plain-text event lines to wrap
     *
     * @return the assembled report HTML
     */
    private static String reportOf(String... entries) {
        StringBuilder report = new StringBuilder();
        for (String entry : entries) {
            report.append("<span class='report-entry'>").append(entry).append("</span>");
        }
        return report.toString();
    }

    @Test
    @DisplayName("Each report entry becomes its own toast, with the first one labelled")
    void splitsEntriesAndPrefixesTheFirst() {
        List<String> toasts = ReportToastFormatter.formatReport(DEFAULT_PREFIX,
              reportOf("Fenrir FNR-4 must make a piloting skill roll.", "Fenrir FNR-4 rolls 7, needs 5: succeeds."));

        assertEquals(2, toasts.size());
        assertTrue(toasts.getFirst().startsWith(DEFAULT_PREFIX + ": "),
              "first toast should carry the report label, was: " + toasts.getFirst());
        assertFalse(toasts.getLast().startsWith(DEFAULT_PREFIX),
              "later toasts should not repeat the label, was: " + toasts.getLast());
    }

    @Test
    @DisplayName("Re-sending the same report produces no further toasts")
    void suppressesEntriesAlreadyToasted() {
        Set<String> alreadyToasted = new HashSet<>();
        String report = reportOf("Fenrir FNR-4 must make a piloting skill roll.",
              "Fenrir FNR-4 rolls 7, needs 5: succeeds.");

        List<String> firstBurst = ReportToastFormatter.formatReport(DEFAULT_PREFIX, report, alreadyToasted);
        List<String> secondBurst = ReportToastFormatter.formatReport(DEFAULT_PREFIX, report, alreadyToasted);

        assertEquals(2, firstBurst.size());
        assertTrue(secondBurst.isEmpty(), "a repeated report should produce no toasts, was: " + secondBurst);
    }

    @Test
    @DisplayName("A growing report only toasts the entries added since the last burst")
    void toastsOnlyTheNewTailOfAGrowingReport() {
        Set<String> alreadyToasted = new HashSet<>();
        ReportToastFormatter.formatReport(DEFAULT_PREFIX,
              reportOf("Fenrir FNR-4 must make a piloting skill roll."), alreadyToasted);

        // The server re-sends the whole accumulated phase report, first entry included.
        List<String> secondBurst = ReportToastFormatter.formatReport(DEFAULT_PREFIX,
              reportOf("Fenrir FNR-4 must make a piloting skill roll.", "Hidden unit revealed at 0304."),
              alreadyToasted);

        assertEquals(1, secondBurst.size());
        assertTrue(secondBurst.getFirst().contains("Hidden unit revealed at 0304."),
              "only the newly added event should toast, was: " + secondBurst.getFirst());
    }

    @Test
    @DisplayName("Already-seen entries do not consume the per-burst cap or inflate the overflow count")
    void seenEntriesDoNotConsumeTheBurstCap() {
        Set<String> alreadyToasted = new HashSet<>();
        String[] firstEntries = new String[ReportToastFormatter.MAX_TOASTS_PER_BURST];
        for (int entryIndex = 0; entryIndex < firstEntries.length; entryIndex++) {
            firstEntries[entryIndex] = "Old event " + entryIndex + " resolves.";
        }
        ReportToastFormatter.formatReport(DEFAULT_PREFIX, reportOf(firstEntries), alreadyToasted);

        // Re-send those capped entries plus one genuinely new event.
        String[] secondEntries = new String[firstEntries.length + 1];
        System.arraycopy(firstEntries, 0, secondEntries, 0, firstEntries.length);
        secondEntries[firstEntries.length] = "New event resolves.";
        List<String> secondBurst = ReportToastFormatter.formatReport(DEFAULT_PREFIX, reportOf(secondEntries),
              alreadyToasted);

        assertEquals(1, secondBurst.size(),
              "the new event should toast rather than be summarised as overflow, was: " + secondBurst);
        assertTrue(secondBurst.getFirst().contains("New event resolves."));
    }

    @Test
    @DisplayName("Bursts beyond the cap collapse the surplus into a single overflow toast")
    void collapsesSurplusIntoOneOverflowToast() {
        int overflowCount = 3;
        String[] entries = new String[ReportToastFormatter.MAX_TOASTS_PER_BURST + overflowCount];
        for (int entryIndex = 0; entryIndex < entries.length; entryIndex++) {
            entries[entryIndex] = "Event " + entryIndex + " resolves.";
        }

        List<String> toasts = ReportToastFormatter.formatReport(DEFAULT_PREFIX, reportOf(entries));

        assertEquals(ReportToastFormatter.MAX_TOASTS_PER_BURST + 1, toasts.size());
        assertTrue(toasts.getLast().startsWith("+" + overflowCount + " more events"),
              "last toast should summarise the surplus, was: " + toasts.getLast());
    }

    @Test
    @DisplayName("The two-argument overload keeps no history between calls")
    void historyFreeOverloadRepeatsEveryCall() {
        String report = reportOf("Fenrir FNR-4 must make a piloting skill roll.");

        assertEquals(1, ReportToastFormatter.formatReport(DEFAULT_PREFIX, report).size());
        assertEquals(1, ReportToastFormatter.formatReport(DEFAULT_PREFIX, report).size());
    }

    @Test
    @DisplayName("Report markup and embedded newlines are flattened to a single plain-text line")
    void normalizesMarkupToPlainText() {
        String normalized = ReportToastFormatter.normalizeToastText(
              "<B>Fenrir FNR-4</B><br>falls&nbsp;&amp; takes\n  damage");

        assertEquals("Fenrir FNR-4 falls & takes damage", normalized);
    }
}
