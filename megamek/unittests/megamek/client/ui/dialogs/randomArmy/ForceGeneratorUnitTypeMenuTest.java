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
package megamek.client.ui.dialogs.randomArmy;

import static megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView.subFactionToPreselect;
import static megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView.unitTypesWithUnits;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import megamek.client.ratgenerator.FactionRecord;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * The Unit Type menu lists only the types the faction's unit tables can fill in that year. Clan Wolf in 3150 offered
 * ProtoMek with no ProtoMeks in its tables, so a ProtoMek formation came out empty (MegaMek/mekhq#10376 item 1).
 * Combined Arms needs Meks, and a faction with no Meks of its own opens on a sub-faction that has them
 * (MegaMek/megamek#9480).
 */
class ForceGeneratorUnitTypeMenuTest {

    private static final List<Integer> CLAN_MENU = Arrays.asList(null, UnitType.MEK, UnitType.PROTOMEK,
          UnitType.BATTLE_ARMOR, UnitType.WARSHIP);

    @Test
    void aTypeWithNoUnitsIsLeftOut() {
        Set<Integer> stocked = Set.of(UnitType.MEK, UnitType.BATTLE_ARMOR, UnitType.WARSHIP);

        List<Integer> menu = unitTypesWithUnits(CLAN_MENU, stocked::contains);

        assertEquals(Arrays.asList(null, UnitType.MEK, UnitType.BATTLE_ARMOR, UnitType.WARSHIP), menu);
    }

    @Test
    void combinedArmsIsOfferedWhenTheTablesHoldMeks() {
        List<Integer> menu = unitTypesWithUnits(CLAN_MENU, unitType -> unitType == UnitType.MEK);

        assertEquals(Arrays.asList(null, UnitType.MEK), menu,
              "every combined-arms force is built around Meks, so Meks in the tables keep it on the menu");
    }

    /**
     * MegaMek/megamek#9480: All Clans on its own has no Meks after 3049, yet Combined Arms was still offered, and a
     * combined-arms Star (five Meks) then came out with nothing in it.
     */
    @Test
    void combinedArmsIsLeftOutWhenTheTablesHoldNoMeks() {
        Set<Integer> stocked = Set.of(UnitType.BATTLE_ARMOR, UnitType.WARSHIP);

        List<Integer> menu = unitTypesWithUnits(CLAN_MENU, stocked::contains);

        assertEquals(List.of(UnitType.BATTLE_ARMOR, UnitType.WARSHIP), menu);
    }

    @Test
    void aCombinedArmsOnlyMenuIsKeptWhenTheTablesHoldMeks() {
        List<Integer> combinedArmsOnly = Collections.singletonList(null);

        List<Integer> menu = unitTypesWithUnits(combinedArmsOnly, unitType -> unitType == UnitType.MEK);

        assertEquals(combinedArmsOnly, menu, "ComStar and the Word of Blake offer only Combined Arms");
    }

    @Test
    void aFactionWithMeksOfItsOwnKeepsGeneral() {
        FactionRecord innerSphere = new FactionRecord("CLAN.IS", "Clan - Inner Sphere");

        assertNull(subFactionToPreselect(true, List.of(innerSphere), subFaction -> true));
    }

    @Test
    void aFactionWithoutMeksSelectsTheFirstSubFactionThatHasThem() {
        FactionRecord grandCouncil = new FactionRecord("CLAN.GC", "Grand Council (Ebon Keshik)");
        FactionRecord homeworld = new FactionRecord("CLAN.HW", "Clan - Homeworld");
        FactionRecord innerSphere = new FactionRecord("CLAN.IS", "Clan - Inner Sphere");
        Set<FactionRecord> withMeks = Set.of(homeworld, innerSphere);

        FactionRecord preselected = subFactionToPreselect(false, List.of(grandCouncil, homeworld, innerSphere),
              withMeks::contains);

        assertSame(homeworld, preselected);
    }

    @Test
    void whenNoSubFactionHasMeksGeneralStays() {
        FactionRecord innerSphere = new FactionRecord("CLAN.IS", "Clan - Inner Sphere");

        assertNull(subFactionToPreselect(false, List.of(innerSphere), subFaction -> false));
    }

    @Test
    void whenNoTypeHasUnitsTheMenuIsKept() {
        List<Integer> menu = unitTypesWithUnits(CLAN_MENU, unitType -> false);

        assertEquals(CLAN_MENU, menu,
              "no units for any type means the tables are not loaded yet, not that the faction has nothing");
    }
}
