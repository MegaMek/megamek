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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import megamek.common.loaders.MekSummary;
import megamek.common.units.EntityMovementMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Every movement mode a conventional infantry unit file uses lands in exactly one class (#9200 follow-up, infantry
 * classes). The movement text is what the unit cache stores, so the cases use the strings found in the unit files.
 */
class InfantryClassTest {

    @ParameterizedTest
    @CsvSource({ "Leg, LIGHT", "Motorized, MOTORIZED", "Tracked, MECHANIZED", "Wheeled, MECHANIZED",
                 "Hover, MECHANIZED", "Jump, JUMP", "Microcopter, JUMP", "Microlite, JUMP",
                 "Motorized SCUBA, AQUATIC", "SCUBA, AQUATIC", "Submarine, AQUATIC" })
    void everyInfantryMovementInTheUnitFilesHasAClass(String movementText, InfantryClass expected) {
        assertEquals(expected, InfantryClass.classify(infantry("Unit", movementText, false)));
    }

    @ParameterizedTest
    @CsvSource({ "Leg", "VTOL", "Submarine" })
    void aMountedPlatoonIsBeastWhateverItsAnimalMovesLike(String mountMovementText) {
        assertEquals(InfantryClass.BEAST, InfantryClass.classify(infantry("Beast Infantry", mountMovementText, true)));
    }

    @Test
    void battleArmorHasNoInfantryClass() {
        MekSummary battleArmor = infantry("Elemental", "Jump", false);
        battleArmor.setUnitType("BattleArmor");

        assertNull(InfantryClass.classify(battleArmor));
    }

    @Test
    void aClassMatchesOnlyItsOwnMovement() {
        assertTrue(InfantryClass.JUMP.matches(infantry("Jump Infantry", "Jump", false), Set.of()));
        assertFalse(InfantryClass.JUMP.matches(infantry("Foot Infantry", "Leg", false), Set.of()));
        assertFalse(InfantryClass.LIGHT.matches(infantry("Beast Infantry (Horse)", "Leg", true), Set.of()),
              "horse cavalry moves as Leg but is Beast, not Light");
    }

    @Test
    void beastOptionsNarrowTheAnimal() {
        MekSummary horse = infantry("Beast Infantry (Horse)", "Leg", true);
        MekSummary branth = infantry("Beast Infantry (Branth)", "VTOL", true);
        MekSummary orca = infantry("Beast Infantry (Orca)", "Submarine", true);

        assertTrue(InfantryClass.BEAST.matches(horse, Set.of()), "no option means any animal");
        assertTrue(InfantryClass.BEAST.matches(horse, Set.of(InfantryClass.FLAG_BEAST_LAND)));
        assertFalse(InfantryClass.BEAST.matches(branth, Set.of(InfantryClass.FLAG_BEAST_LAND)));
        assertTrue(InfantryClass.BEAST.matches(branth, Set.of(InfantryClass.FLAG_BEAST_FLYING)));
        assertTrue(InfantryClass.BEAST.matches(orca, Set.of(InfantryClass.FLAG_BEAST_SWIMMING)));
        assertFalse(InfantryClass.BEAST.matches(horse, Set.of(InfantryClass.FLAG_BEAST_SWIMMING)));
    }

    @Test
    void movementNoClassCoversIsUnclassified() {
        assertNull(InfantryClass.classify(EntityMovementMode.WIGE, false));
        assertNull(InfantryClass.classify(null, false));
    }

    /** A cache entry for a conventional infantry unit with the given movement text, as the cache stores it. */
    static MekSummary infantry(String chassis, String movementText, boolean isMounted) {
        MekSummary unit = new MekSummary();
        unit.setChassis(chassis);
        unit.setName(chassis + " " + movementText);
        unit.setUnitType("Infantry");
        unit.setUnitSubType(movementText);
        unit.setMountedInfantry(isMounted);
        return unit;
    }
}
