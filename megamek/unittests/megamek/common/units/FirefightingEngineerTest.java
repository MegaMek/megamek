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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the firefighting engineer (FIRE_ENGINEERS) consecutive-turn streak logic on {@link ConvInfantry}.
 *
 * <p>Per TO:AuE p.153, a firefighting platoon reduces the target number by 1 for every consecutive turn it
 * keeps fighting a blaze in a single hex (to a minimum target number of 3). A platoon that stops fighting a fire before
 * it is out must start over.</p>
 */
class FirefightingEngineerTest {

    private static final Coords HEX_A = new Coords(3, 4);
    private static final Coords HEX_B = new Coords(7, 9);

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private ConvInfantry firefighters() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setId(1);
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        infantry.setSquadSize(7);
        infantry.setSquadCount(4);
        infantry.autoSetInternal();
        infantry.setSpecializations(ConvInfantry.FIRE_ENGINEERS);
        return infantry;
    }

    @Test
    void firefighterFlagReflectsSpecialization() {
        assertTrue(firefighters().isFirefighter(), "FIRE_ENGINEERS platoon should be a firefighter");

        ConvInfantry plain = new ConvInfantry();
        plain.setId(2);
        assertFalse(plain.isFirefighter(), "Platoon with no specialization is not a firefighter");
    }

    @Test
    void firefightersAreAutoEquippedWithAFireExtinguisher() {
        boolean hasExtinguisher = firefighters().getWeaponList().stream()
              .anyMatch(w -> w.getType().hasFlag(WeaponType.F_EXTINGUISHER));
        assertTrue(hasExtinguisher, "FIRE_ENGINEERS platoon should carry a Fire Extinguisher weapon");
    }

    @Test
    void firefighterExtinguisherIsNotClanTechSoItDoesNotForceMixedTech() {
        // The fire extinguisher is a single TechBase.ALL weapon, so adding it to an Inner Sphere platoon must
        // not flag the platoon as Clan/mixed tech.
        boolean hasClanExtinguisher = firefighters().getWeaponList().stream()
              .filter(w -> w.getType().hasFlag(WeaponType.F_EXTINGUISHER))
              .anyMatch(w -> w.getType().isClan());
        assertFalse(hasClanExtinguisher, "The merged Fire Extinguisher should not be Clan tech");
    }

    @Test
    void nonFirefightersHaveNoFireExtinguisher() {
        ConvInfantry plain = new ConvInfantry();
        plain.setId(3);
        boolean hasExtinguisher = plain.getWeaponList().stream()
              .anyMatch(w -> w.getType().hasFlag(WeaponType.F_EXTINGUISHER));
        assertFalse(hasExtinguisher, "A platoon with no specialization should not carry a Fire Extinguisher");
    }

    @Test
    void firstTurnFightingHasNoPriorStreak() {
        ConvInfantry platoon = firefighters();
        assertEquals(0, platoon.getPriorFirefightStreak(HEX_A, 1),
              "A platoon that has not fought before has no prior streak");
    }

    @Test
    void consecutiveTurnsAccumulateStreak() {
        ConvInfantry platoon = firefighters();

        // Round 1: first turn fighting hex A -> no prior streak (target number stays 8).
        assertEquals(0, platoon.getPriorFirefightStreak(HEX_A, 1));
        platoon.recordFirefight(HEX_A, 1);

        // Round 2: second consecutive turn -> prior streak 1 (target number 7).
        assertEquals(1, platoon.getPriorFirefightStreak(HEX_A, 2));
        platoon.recordFirefight(HEX_A, 2);

        // Round 3: third consecutive turn -> prior streak 2 (target number 6).
        assertEquals(2, platoon.getPriorFirefightStreak(HEX_A, 3));
    }

    @Test
    void fightingDifferentHexResetsStreak() {
        ConvInfantry platoon = firefighters();
        platoon.recordFirefight(HEX_A, 1);
        platoon.recordFirefight(HEX_A, 2);

        // Switching to a new hex on round 3 starts over.
        assertEquals(0, platoon.getPriorFirefightStreak(HEX_B, 3));
        platoon.recordFirefight(HEX_B, 3);
        assertEquals(1, platoon.getPriorFirefightStreak(HEX_B, 4));
    }

    @Test
    void skippingARoundResetsStreak() {
        ConvInfantry platoon = firefighters();
        platoon.recordFirefight(HEX_A, 1);
        platoon.recordFirefight(HEX_A, 2);

        // The platoon stops fighting on round 3 and resumes on round 4 -> streak is broken.
        assertEquals(0, platoon.getPriorFirefightStreak(HEX_A, 4),
              "A non-consecutive round breaks the streak");
        platoon.recordFirefight(HEX_A, 4);
        assertEquals(1, platoon.getPriorFirefightStreak(HEX_A, 5));
    }

    @Test
    void recordingAfterABreakRestartsAtOne() {
        ConvInfantry platoon = firefighters();
        platoon.recordFirefight(HEX_A, 5);
        // Immediately fighting a different hex the very next round restarts the count.
        platoon.recordFirefight(HEX_B, 6);
        assertEquals(1, platoon.getPriorFirefightStreak(HEX_B, 7));
    }
}
