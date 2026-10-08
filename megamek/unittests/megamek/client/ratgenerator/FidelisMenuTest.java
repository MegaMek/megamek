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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The Fidelis Battle Group is three combined-arms Umbras, so it is offered only with no unit type picked; a Mek "Battle
 * Group" used to come out combined arms anyway (MegaMek/mekhq#10376 item 14). The Fidelis had one WarShip, the Flatus,
 * in service 3083-3149: the Naval Detachment builds it, and the WarShip Star that built five is gone (item 17).
 */
class FidelisMenuTest {

    private static final int ECHELON_STAR = 3;
    private static final int ECHELON_TRINARY = 5;

    @TempDir
    Path temporaryDirectory;

    private Ruleset fidelis;

    @BeforeEach
    void loadFidelis() throws Exception {
        fidelis = ShippedRulesetLoader.load("FID.xml", "FID", temporaryDirectory);
    }

    @Test
    void aCombinedArmsRequestStaysCombinedArms() {
        // The defaults run before the rule lookup. A default unit type turned a blank request into Meks, so a Battle
        // Group or Century came out as Clan Mek Trinaries.
        assertNull(fidelis.getDefaultUnitType(request(null, 3150, ECHELON_TRINARY)));
    }

    @Test
    void theBattleGroupIsCombinedArmsOnly() {
        ForceNode combinedArms = fidelis.findForceNode(request(null, 3150, ECHELON_TRINARY));
        assertNotNull(combinedArms);
        assertEquals("Battle Group", combinedArms.getEchelonName());

        assertNull(fidelis.findForceNode(request(UnitType.MEK, 3150, ECHELON_TRINARY)),
              "a Mek request must not be caught by the combined-arms Battle Group");
        assertFalse(sizes(UnitType.MEK, 3150).contains(Integer.toString(ECHELON_TRINARY)),
              "single unit types should top out below the Battle Group");
    }

    @Test
    void warShipsAreOfferedOnlyWhileTheFlatusServed() {
        assertTrue(unitTypes(3140).contains("Warship"));
        assertFalse(unitTypes(3150).contains("Warship"), "the Flatus was lost in 3149");
        assertFalse(unitTypes(3070).contains("Warship"), "the Flatus joined the Fidelis in 3083");
    }

    @Test
    void theOnlyWarShipSizeIsTheNavalDetachment() {
        assertFalse(sizes(UnitType.WARSHIP, 3140).contains(Integer.toString(ECHELON_STAR)),
              "a WarShip Star would build five WarShips");
    }

    private List<String> sizes(Integer unitType, int year) {
        ValueNode echelons = fidelis.getTOCNode().findEchelons(request(unitType, year, ECHELON_STAR));
        assertNotNull(echelons);
        return List.of(echelons.getContent().replaceAll("[^0-9,]", "").split(","));
    }

    private List<String> unitTypes(int year) {
        ValueNode unitTypes = fidelis.getTOCNode().findUnitTypes(request(null, year, ECHELON_STAR));
        assertNotNull(unitTypes);
        return List.of(unitTypes.getContent().split(","));
    }

    private static ForceDescriptor request(Integer unitType, int year, int echelon) {
        ForceDescriptor request = new ForceDescriptor();
        request.setFaction("FID");
        request.setYear(year);
        request.setUnitType(unitType);
        request.setEchelon(echelon);
        request.setRating("A");
        return request;
    }
}
