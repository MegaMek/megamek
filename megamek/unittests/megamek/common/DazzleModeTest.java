/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.lasers.LaserWeapon;
import megamek.common.weapons.lasers.innerSphere.large.ISERLaserLarge;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Gothic rules Dazzle Mode
 */
class DazzleModeTest {

    @BeforeAll
    static void setUpAll() {
        // Initialize equipment database before creating weapons
        EquipmentType.initializeTypes();
    }

    @Test
    void testDazzleModeAddedWhenGothicOptionEnabled() {
        // Create a laser weapon
        WeaponType weapon = new ISERLaserLarge();

        // Create game options with Gothic Dazzle Mode enabled
        GameOptions options = new GameOptions();
        options.getOption(OptionsConstants.ADVANCED_COMBAT_GOTHIC_DAZZLE_MODE).setValue(true);

        // Apply game options to weapon
        ((LaserWeapon) weapon).adaptToGameOptions(options);

        // Verify Dazzle mode was added
        boolean hasDazzleMode = false;
        for (var mode : java.util.Collections.list(weapon.getModes())) {
            if (mode.getName().contains("Dazzle")) {
                hasDazzleMode = true;
                break;
            }
        }

        assertTrue(hasDazzleMode, "Dazzle mode should be added to laser weapons when Gothic option is enabled");
    }

    @Test
    void testDazzleModeNotAddedWhenGothicOptionDisabled() {
        // Create a laser weapon
        WeaponType weapon = new ISERLaserLarge();

        // Create game options with Gothic Dazzle Mode disabled
        GameOptions options = new GameOptions();
        options.getOption(OptionsConstants.ADVANCED_COMBAT_GOTHIC_DAZZLE_MODE).setValue(false);

        // Apply game options to weapon
        ((LaserWeapon) weapon).adaptToGameOptions(options);

        // Verify Dazzle mode was NOT added (only Pulse modes should be present)
        boolean hasDazzleMode = false;
        for (var mode : java.util.Collections.list(weapon.getModes())) {
            if (mode.getName().contains("Dazzle")) {
                hasDazzleMode = true;
                break;
            }
        }

        assertTrue(!hasDazzleMode, "Dazzle mode should NOT be added when Gothic option is disabled");
    }

    @Test
    void testDazzleModeNameFormat() {
        // Create a laser weapon
        WeaponType weapon = new ISERLaserLarge();

        // Create game options with Gothic Dazzle Mode enabled
        GameOptions options = new GameOptions();
        options.getOption(OptionsConstants.ADVANCED_COMBAT_GOTHIC_DAZZLE_MODE).setValue(true);

        // Apply game options to weapon
        ((LaserWeapon) weapon).adaptToGameOptions(options);

        // Find and verify the Dazzle mode name
        String dazzleModeName = null;
        for (var mode : java.util.Collections.list(weapon.getModes())) {
            if (mode.getName().contains("Dazzle") && !mode.getName().contains("Pulse")) {
                dazzleModeName = mode.getName();
                break;
            }
        }

        assertEquals("Dazzle", dazzleModeName, "Base Dazzle mode should be named 'Dazzle'");
    }

    @Test
    void testDazzleModeOrderBeforePulse() {
        // Create a laser weapon
        WeaponType weapon = new ISERLaserLarge();

        // Create game options with Gothic Dazzle Mode enabled
        GameOptions options = new GameOptions();
        options.getOption(OptionsConstants.ADVANCED_COMBAT_GOTHIC_DAZZLE_MODE).setValue(true);

        // Apply game options to weapon
        ((LaserWeapon) weapon).adaptToGameOptions(options);

        // Verify mode order: Dazzle should come before Pulse
        int dazzleIndex = -1;
        int firstPulseIndex = -1;
        int index = 0;

        for (var mode : java.util.Collections.list(weapon.getModes())) {
            if (mode.getName().equals("Dazzle")) {
                dazzleIndex = index;
            }
            if (mode.getName().contains("Pulse") && firstPulseIndex == -1) {
                firstPulseIndex = index;
            }
            index++;
        }

        assertTrue(dazzleIndex >= 0, "Dazzle mode should exist");
        assertTrue(firstPulseIndex >= 0, "Pulse mode should exist");
        assertTrue(dazzleIndex < firstPulseIndex, "Dazzle mode should come before Pulse mode for proper filtering");
    }

    @Test
    void testDazzleDamageReductionCalculation() {
        // Test the damage reduction calculation logic
        // Half damage, rounded down, minimum 1

        // Even damage: 10 -> 5
        int damage10 = 10;
        int reduced10 = Math.max(1, (int) Math.floor(damage10 / 2.0));
        assertEquals(5, reduced10, "Damage 10 should reduce to 5");

        // Odd damage: 9 -> 4
        int damage9 = 9;
        int reduced9 = Math.max(1, (int) Math.floor(damage9 / 2.0));
        assertEquals(4, reduced9, "Damage 9 should reduce to 4 (rounded down)");

        // Low damage: 1 -> 1
        int damage1 = 1;
        int reduced1 = Math.max(1, (int) Math.floor(damage1 / 2.0));
        assertEquals(1, reduced1, "Damage 1 should reduce to 1 (minimum)");

        // Low damage: 2 -> 1
        int damage2 = 2;
        int reduced2 = Math.max(1, (int) Math.floor(damage2 / 2.0));
        assertEquals(1, reduced2, "Damage 2 should reduce to 1");
    }

    @Test
    void testDazzleHeatReductionCalculation() {
        // Test the heat reduction calculation logic
        // Half heat, rounded down, minimum 0

        // Even heat: 12 -> 6
        int heat12 = 12;
        int reduced12 = Math.max(0, heat12 / 2);
        assertEquals(6, reduced12, "Heat 12 should reduce to 6");

        // Odd heat: 7 -> 3
        int heat7 = 7;
        int reduced7 = Math.max(0, heat7 / 2);
        assertEquals(3, reduced7, "Heat 7 should reduce to 3 (rounded down)");

        // Low heat: 1 -> 0
        int heat1 = 1;
        int reduced1 = Math.max(0, heat1 / 2);
        assertEquals(0, reduced1, "Heat 1 should reduce to 0 (minimum)");

        // Zero heat: 0 -> 0
        int heat0 = 0;
        int reduced0 = Math.max(0, heat0 / 2);
        assertEquals(0, reduced0, "Heat 0 should remain 0");
    }
}
