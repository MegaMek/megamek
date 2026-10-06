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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Enumeration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Consistency checks between {@link QuirkCatalog}, the live {@link Quirks} and {@link WeaponQuirks} option lists, and
 * the quirk metadata in {@code common/options/messages.properties}. These tests catch drift: a quirk added without a
 * {@code .working} status, a placeholder that has since been implemented, or a renamed option code.
 */
@DisplayName("Quirk catalog consistency")
class QuirkCatalogTest {

    private static final int UNIT_QUIRK_COUNT = 93;
    private static final int WEAPON_QUIRK_COUNT = 18;
    private static final int UNIT_IMPLEMENTED_COUNT = 57;
    private static final int UNIT_PARTIAL_COUNT = 3;
    private static final int UNIT_NOT_IMPLEMENTED_COUNT = 33;
    private static final int WEAPON_IMPLEMENTED_COUNT = 12;
    private static final int WEAPON_NOT_IMPLEMENTED_COUNT = 6;

    private static long countWithStatus(QuirkKind kind, QuirkImplementationStatus status) {
        return QuirkCatalog.getEntries(kind).stream().filter(entry -> entry.status() == status).count();
    }

    @Test
    @DisplayName("Every registered quirk option has a catalog status")
    void everyRegisteredQuirkHasAStatus() {
        assertOptionsAreCatalogued(QuirkKind.UNIT, new Quirks());
        assertOptionsAreCatalogued(QuirkKind.WEAPON, new WeaponQuirks());
    }

    private static void assertOptionsAreCatalogued(QuirkKind kind, AbstractOptions quirkOptions) {
        for (Enumeration<IOption> options = quirkOptions.getOptions(); options.hasMoreElements(); ) {
            String code = options.nextElement().getName();
            assertTrue(QuirkCatalog.getEntry(kind, code).isPresent(),
                  "Quirk '" + code + "' (" + kind + ") has no implementation status. Add a "
                        + kind.resourceKey(code, "working") + " entry to common/options/messages.properties "
                        + "with 1, 0 or partial.");
        }
    }

    @Test
    @DisplayName("The catalog holds the audited status distribution")
    void catalogMatchesAuditCounts() {
        assertEquals(UNIT_QUIRK_COUNT, QuirkCatalog.getEntries(QuirkKind.UNIT).size());
        assertEquals(WEAPON_QUIRK_COUNT, QuirkCatalog.getEntries(QuirkKind.WEAPON).size());

        assertEquals(UNIT_IMPLEMENTED_COUNT,
              countWithStatus(QuirkKind.UNIT, QuirkImplementationStatus.IMPLEMENTED));
        assertEquals(UNIT_PARTIAL_COUNT, countWithStatus(QuirkKind.UNIT, QuirkImplementationStatus.PARTIAL));
        assertEquals(UNIT_NOT_IMPLEMENTED_COUNT,
              countWithStatus(QuirkKind.UNIT, QuirkImplementationStatus.NOT_IMPLEMENTED));

        assertEquals(WEAPON_IMPLEMENTED_COUNT,
              countWithStatus(QuirkKind.WEAPON, QuirkImplementationStatus.IMPLEMENTED));
        assertEquals(WEAPON_NOT_IMPLEMENTED_COUNT,
              countWithStatus(QuirkKind.WEAPON, QuirkImplementationStatus.NOT_IMPLEMENTED));
    }

    @Test
    @DisplayName("Every catalogued quirk cites a rule book and page")
    void everyEntryHasARulesReference() {
        for (QuirkKind kind : QuirkKind.values()) {
            for (QuirkCatalogEntry entry : QuirkCatalog.getEntries(kind)) {
                assertNotNull(entry.getRulesReference(),
                      "Quirk '" + entry.code() + "' (" + kind + ") has no rules reference. Add "
                            + kind.resourceKey(entry.code(), "rulesBook") + " and "
                            + kind.resourceKey(entry.code(), "rulesPage") + ".");
            }
        }
    }

    @Test
    @DisplayName("Chassis and weapon quirks are catalogued independently")
    void kindsAreLookedUpSeparately() {
        // atmo_flyer is a chassis quirk only; accurate is a weapon quirk only
        assertTrue(QuirkCatalog.getEntry(QuirkKind.UNIT, OptionsConstants.QUIRK_POS_ATMOSPHERE_FLYER).isPresent());
        assertTrue(QuirkCatalog.getEntry(QuirkKind.WEAPON, OptionsConstants.QUIRK_WEAPON_POS_ACCURATE).isPresent());
        assertTrue(QuirkCatalog.getEntry(QuirkKind.WEAPON, OptionsConstants.QUIRK_POS_ATMOSPHERE_FLYER).isEmpty());
        assertTrue(QuirkCatalog.getEntry(QuirkKind.UNIT, OptionsConstants.QUIRK_WEAPON_POS_ACCURATE).isEmpty());
    }

    @Test
    @DisplayName("Codes shared by a chassis and a weapon quirk resolve to their own entry")
    void sharedCodesResolveToTheirOwnKind() {
        // fast_reload names both a chassis quirk and a weapon quirk, so kind is part of the identity
        String sharedCode = OptionsConstants.QUIRK_POS_FAST_RELOAD;
        assertEquals(sharedCode, OptionsConstants.QUIRK_WEAPON_POS_FAST_RELOAD,
              "This test assumes fast_reload is shared between the two quirk sets");

        QuirkCatalogEntry unitEntry = QuirkCatalog.getEntry(QuirkKind.UNIT, sharedCode).orElseThrow();
        QuirkCatalogEntry weaponEntry = QuirkCatalog.getEntry(QuirkKind.WEAPON, sharedCode).orElseThrow();
        assertEquals(QuirkKind.UNIT, unitEntry.kind());
        assertEquals(QuirkKind.WEAPON, weaponEntry.kind());
    }

    @Test
    @DisplayName("No placeholder shadows a real quirk option")
    void placeholdersDoNotResolveInQuirks() {
        Quirks liveQuirks = new Quirks();
        WeaponQuirks liveWeaponQuirks = new WeaponQuirks();
        for (QuirkPlaceholder placeholder : QuirkCatalog.getAllPlaceholders()) {
            assertNull(liveQuirks.getOption(placeholder.key()),
                  "Placeholder '" + placeholder.key() + "' now exists as a real chassis quirk. Remove the "
                        + "placeholder from QuirkCatalog and give the option a .working resource entry.");
            assertNull(liveWeaponQuirks.getOption(placeholder.key()),
                  "Placeholder '" + placeholder.key() + "' now exists as a real weapon quirk. Remove the "
                        + "placeholder from QuirkCatalog and give the option a .working resource entry.");
        }
    }

    @Test
    @DisplayName("Every placeholder has its resource strings")
    void placeholderResourceKeysResolve() {
        for (QuirkPlaceholder placeholder : QuirkCatalog.getAllPlaceholders()) {
            assertFalse(placeholder.getDisplayableName().startsWith("!"),
                  "Missing display name resource for placeholder '" + placeholder.key() + "'");
            assertFalse(placeholder.getDescription().startsWith("!"),
                  "Missing description resource for placeholder '" + placeholder.key() + "'");
            assertFalse(placeholder.getRulesReference().startsWith("!"),
                  "Missing rules reference for placeholder '" + placeholder.key() + "'");
        }
    }

    @Test
    @DisplayName("Placeholders sit in a real quirk group and are sorted by name")
    void placeholdersAreGroupedAndSorted() {
        for (QuirkPlaceholder placeholder : QuirkCatalog.getAllPlaceholders()) {
            assertTrue(Quirks.POS_QUIRKS.equals(placeholder.groupKey())
                        || Quirks.NEG_QUIRKS.equals(placeholder.groupKey()),
                  "Placeholder '" + placeholder.key() + "' has unknown group '" + placeholder.groupKey() + "'");
        }

        for (String groupKey : List.of(Quirks.POS_QUIRKS, Quirks.NEG_QUIRKS)) {
            List<QuirkPlaceholder> placeholders = QuirkCatalog.getPlaceholders(groupKey);
            for (int index = 1; index < placeholders.size(); index++) {
                String previousName = placeholders.get(index - 1).getDisplayableName();
                String currentName = placeholders.get(index).getDisplayableName();
                assertFalse(previousName.compareTo(currentName) > 0,
                      "Placeholders out of order in " + groupKey + ": '" + previousName
                            + "' before '" + currentName + "'");
            }
        }
    }

    @Test
    @DisplayName("Every placeholder is accounted for in exactly one group")
    void placeholdersAppearInOneGroup() {
        int groupedCount = QuirkCatalog.getPlaceholders(Quirks.POS_QUIRKS).size()
              + QuirkCatalog.getPlaceholders(Quirks.NEG_QUIRKS).size();
        assertEquals(QuirkCatalog.getAllPlaceholders().size(), groupedCount);
    }

    @Test
    @DisplayName("The working resource value parses to the right status")
    void workingValuesParse() {
        assertEquals(QuirkImplementationStatus.IMPLEMENTED, QuirkImplementationStatus.parse("1"));
        assertEquals(QuirkImplementationStatus.NOT_IMPLEMENTED, QuirkImplementationStatus.parse("0"));
        assertEquals(QuirkImplementationStatus.PARTIAL, QuirkImplementationStatus.parse("partial"));
        assertEquals(QuirkImplementationStatus.PARTIAL, QuirkImplementationStatus.parse(" Partial "));
        assertNull(QuirkImplementationStatus.parse(null));
        assertNull(QuirkImplementationStatus.parse(""));
        assertNull(QuirkImplementationStatus.parse("yes"));
        // NOT_IN_MEGAMEK belongs to placeholders only and must never come from a resource value
        assertNull(QuirkImplementationStatus.parse("notInMegaMek"));
    }

    @Test
    @DisplayName("Only the statuses with no game effect report as inert")
    void inertStatusesAreReported() {
        assertFalse(QuirkImplementationStatus.IMPLEMENTED.hasNoGameEffect());
        assertFalse(QuirkImplementationStatus.PARTIAL.hasNoGameEffect());
        assertTrue(QuirkImplementationStatus.NOT_IMPLEMENTED.hasNoGameEffect());
        assertTrue(QuirkImplementationStatus.NOT_IN_MEGAMEK.hasNoGameEffect());
    }

    @Test
    @DisplayName("Every status has a localized name")
    void statusNamesResolve() {
        for (QuirkImplementationStatus status : QuirkImplementationStatus.values()) {
            assertFalse(status.getDisplayableName().startsWith("!"),
                  "Missing display name resource for status " + status);
        }
    }
}
