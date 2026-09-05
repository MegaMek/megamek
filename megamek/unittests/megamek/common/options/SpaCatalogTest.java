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

package megamek.common.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Consistency checks between {@link SpaCatalog} (generated from the CamOps SPA audit spreadsheet), the live
 * {@link PilotOptions}, and the {@code SpaCatalog.*} resource keys. These tests catch drift: an option rename in
 * {@link OptionsConstants}, a placeholder that has since been implemented, or a missing resource entry.
 */
@DisplayName("SPA catalog consistency")
class SpaCatalogTest {

    private static final int BOOK_SPA_COUNT = 55;
    private static final int FULL_COUNT = 13;
    private static final int FULL_MINOR_GAPS_COUNT = 4;
    private static final int PARTIAL_COUNT = 17;
    private static final int NOT_IMPLEMENTED_COUNT = 21;

    private static long countWithStatus(SpaImplementationStatus status) {
        return SpaCatalog.getAllEntries().stream().filter(entry -> entry.status() == status).count();
    }

    @Test
    @DisplayName("The catalog holds every book SPA, with the audit's status distribution")
    void catalogMatchesAuditCounts() {
        assertEquals(BOOK_SPA_COUNT, SpaCatalog.getAllEntries().size());
        assertEquals(FULL_COUNT, countWithStatus(SpaImplementationStatus.FULL));
        assertEquals(FULL_MINOR_GAPS_COUNT, countWithStatus(SpaImplementationStatus.FULL_MINOR_GAPS));
        assertEquals(PARTIAL_COUNT, countWithStatus(SpaImplementationStatus.PARTIAL));
        assertEquals(NOT_IMPLEMENTED_COUNT, countWithStatus(SpaImplementationStatus.NOT_IMPLEMENTED));
        assertEquals(NOT_IMPLEMENTED_COUNT, SpaCatalog.getPlaceholders().size());
    }

    @Test
    @DisplayName("Every implemented catalog entry resolves to a real pilot option")
    void implementedEntriesResolveInPilotOptions() {
        PilotOptions pilotOptions = new PilotOptions();
        for (SpaCatalogEntry entry : SpaCatalog.getAllEntries()) {
            if (!entry.isPlaceholder()) {
                assertNotNull(pilotOptions.getOption(entry.key()),
                      "Catalog key '" + entry.key() + "' does not resolve to a PilotOptions option. "
                            + "Was the option renamed? Update SpaCatalog.");
            }
        }
    }

    @Test
    @DisplayName("No placeholder shadows a real pilot option")
    void placeholdersDoNotResolveInPilotOptions() {
        PilotOptions pilotOptions = new PilotOptions();
        for (SpaCatalogEntry placeholder : SpaCatalog.getPlaceholders()) {
            assertNull(pilotOptions.getOption(placeholder.key()),
                  "Placeholder '" + placeholder.key() + "' now exists as a real pilot option. "
                        + "Change its SpaCatalog status and remove the placeholder name resource.");
        }
    }

    @Test
    @DisplayName("Every catalog entry has its resource strings")
    void resourceKeysResolve() {
        for (SpaCatalogEntry entry : SpaCatalog.getAllEntries()) {
            assertFalse(entry.getAbbreviatedEffect().startsWith("!"),
                  "Missing effect resource for '" + entry.key() + "'");
            if (entry.isPlaceholder()) {
                assertFalse(entry.getDisplayableName().startsWith("!"),
                      "Missing display name resource for placeholder '" + entry.key() + "'");
            }
        }
    }

    @Test
    @DisplayName("Every entry with gaps says what is missing")
    void gappedEntriesHaveMissingText() {
        for (SpaCatalogEntry entry : SpaCatalog.getAllEntries()) {
            boolean hasGaps = (entry.status() == SpaImplementationStatus.PARTIAL)
                  || (entry.status() == SpaImplementationStatus.FULL_MINOR_GAPS);
            if (hasGaps) {
                assertNotNull(entry.getMissingText(),
                      "Entry '" + entry.key() + "' is " + entry.status()
                            + " but has no SpaCatalog." + entry.key() + ".missing resource");
            }
        }
    }

    @Test
    @DisplayName("Placeholders are sorted by display name")
    void placeholdersAreSorted() {
        List<SpaCatalogEntry> placeholders = SpaCatalog.getPlaceholders();
        for (int i = 1; i < placeholders.size(); i++) {
            String previousName = placeholders.get(i - 1).getDisplayableName();
            String currentName = placeholders.get(i).getDisplayableName();
            assertFalse(previousName.compareTo(currentName) > 0,
                  "Placeholders out of order: '" + previousName + "' before '" + currentName + "'");
        }
    }

    @Test
    @DisplayName("Every entry cites a page in the CamOps SPA chapter")
    void pagesAreInSpaChapter() {
        for (SpaCatalogEntry entry : SpaCatalog.getAllEntries()) {
            assertFalse((entry.camOpsPage() < 71) || (entry.camOpsPage() > 82),
                  "Entry '" + entry.key() + "' cites CamOps p." + entry.camOpsPage()
                        + ", outside the SPA chapter (pp. 71-82)");
        }
    }
}
