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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class SettingsRouteTest {
    @Test
    void routeMatchesPathIdAndAliases() {
        SettingsRoute route = new SettingsRoute("operations.stratcon",
              List.of("Operations", "Digital GM"), List.of("strategicOperations", "stratcon"), true);

        assertTrue(route.matches(SettingsRoute.normalizeSearchText("Digital GM")));
        assertTrue(route.matches(SettingsRoute.normalizeSearchText("operations")));
        assertTrue(route.matches(SettingsRoute.normalizeSearchText("strategicOperations")));
        assertFalse(route.matches(SettingsRoute.normalizeSearchText("personnel")));
    }

    @Test
    void internalRouteIdIsNotImplicitlySearchable() {
        SettingsRoute route = new SettingsRoute("internal.machine.key", List.of("Visible Page"));

        assertFalse(route.matches(SettingsRoute.normalizeSearchText("machine")));
        assertTrue(route.matches(SettingsRoute.normalizeSearchText("visible")));
    }

    @Test
    void routeIndexesRenderedPathTextWithoutHtmlTokens() {
        SettingsRoute route = new SettingsRoute("display", List.of("<html><b>Display &amp; UI</b></html>"));

        assertTrue(route.matches(SettingsRoute.normalizeSearchText("display ui")));
        assertFalse(route.matches(SettingsRoute.normalizeSearchText("html")));
        assertFalse(route.matches(SettingsRoute.normalizeSearchText("amp")));
    }

    @Test
    void routeRequiresEveryFilterToken() {
        SettingsRoute route = new SettingsRoute("markets.contracts", List.of("Markets", "Contract Market"));

        assertTrue(route.matches(SettingsRoute.normalizeSearchText("market contract")));
        assertFalse(route.matches(SettingsRoute.normalizeSearchText("market personnel")));
    }

    @Test
    void routeMatchesHarvestedSectionText() {
        SettingsRoute route = new SettingsRoute("newDay", List.of("New Day"));
        route.setSectionSearchText("Personnel Pools Automatic Assignment");

        assertTrue(route.matches(SettingsRoute.normalizeSearchText("personnel pools")));
        assertFalse(route.matches(SettingsRoute.normalizeSearchText("personnel salaries")));
    }

    @Test
    void searchNormalizationPreservesUnicodeAndIgnoresAccents() {
        assertEquals("uber настройки 設定", SettingsRoute.normalizeSearchText("Über — Настройки / 設定"));
    }

    @Test
    void sectionMatchingCombinesRouteAndSectionTokens() {
        SettingsRoute route = new SettingsRoute("newDay", List.of("New Day"));

        assertTrue(route.sectionMatches("Personnel Pools", SettingsRoute.normalizeSearchText("new pool")));
        assertFalse(route.sectionMatches("Personnel Pools", SettingsRoute.normalizeSearchText("new salary")));
    }

    @Test
    void routeDerivesHierarchyFromDisplayPath() {
        assertTrue(new SettingsRoute("display", List.of("Display")).isTopLevelRoute());
        SettingsRoute nested = new SettingsRoute("display.units", List.of("Display", "Units"));
        assertFalse(nested.isTopLevelRoute());
        assertEquals(List.of("display", "display.units"), nested.getPathIds());
    }

    @Test
    void routeRejectsMissingIdentityAndPath() {
        assertThrows(IllegalArgumentException.class, () -> new SettingsRoute(" ", List.of("Display")));
        assertThrows(IllegalArgumentException.class, () -> new SettingsRoute("display", List.of()));
        assertThrows(IllegalArgumentException.class,
              () -> new SettingsRoute("display", List.of("Display"), List.of(), List.of(), true));
        assertThrows(IllegalArgumentException.class,
              () -> new SettingsRoute("display", List.of(" "), List.of("display"), List.of(), true));
        assertThrows(IllegalArgumentException.class,
              () -> new SettingsRoute("display", List.of("Display"), List.of(" "), List.of(), true));
    }
}
