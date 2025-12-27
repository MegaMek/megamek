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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArtilleryTrackerTest {

    protected ArtilleryTracker artilleryTracker = new ArtilleryTracker();
    protected Tank tank;
    protected WeaponType sniperType = (WeaponType) EquipmentType.get("IS Sniper");

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws LocationFullException {
        tank = new Tank();
        tank.addEquipment(sniperType, Tank.LOC_FRONT);
        tank.aTracker = artilleryTracker;
    }

    /**
     * No-op when no weapons are registered
     */
    @Test
    void test_clearHitHexMods_with_empty_artillery_tracker() {
        // This should be a no-op and not fail
        artilleryTracker.clearHitHexMods();
    }

    /**
     * No-op when there are no mods
     */
    @Test
    void test_clearHitHexMods_with_no_mods_on_one_weapon() {
        WeaponMounted weapon = tank.getWeapon(0);
        artilleryTracker.addWeapon(weapon);

        // Should report one weapon with no mods
        assertEquals(1, artilleryTracker.getSize());
        assertEquals(0, artilleryTracker.getWeaponModifiers(weapon).size());

        artilleryTracker.clearHitHexMods();

        // No change is expected
        assertEquals(1, artilleryTracker.getSize());
        assertEquals(0, artilleryTracker.getWeaponModifiers(weapon).size());
    }

    /**
     * Test to show that standard to-hit mods are not removed
     */
    @Test
    void test_clearHitHexMods_with_standard_mod_on_one_weapon() {
        WeaponMounted weapon = tank.getWeapon(0);
        artilleryTracker.addWeapon(weapon);

        artilleryTracker.setModifier(7, new Coords(8, 9));

        // Should report one weapon with base mod
        assertEquals(1, artilleryTracker.getSize());
        assertEquals(1, artilleryTracker.getWeaponModifiers(weapon).size());

        artilleryTracker.clearHitHexMods();

        // No change is expected
        assertEquals(1, artilleryTracker.getSize());
        assertEquals(1, artilleryTracker.getWeaponModifiers(weapon).size());
    }

    /**
     * Test to show that normal mods are left in place after automatic hit mods are removed
     */
    @Test
    void test_clearHitHexMods_with_standard_and_autohit_mods_on_one_weapon() {
        WeaponMounted weapon = tank.getWeapon(0);
        artilleryTracker.addWeapon(weapon);

        artilleryTracker.setModifier(7, new Coords(8, 9));
        artilleryTracker.setModifier(TargetRoll.AUTOMATIC_SUCCESS, new Coords(12, 15));

        // Should report one weapon with base mod and autohit mod
        assertEquals(1, artilleryTracker.getSize());
        assertEquals(2, artilleryTracker.getWeaponModifiers(weapon).size());

        artilleryTracker.clearHitHexMods();

        // Autohit mod is removed
        assertEquals(1, artilleryTracker.getSize());
        assertEquals(1, artilleryTracker.getWeaponModifiers(weapon).size());
    }

    /**
     * This test shows that any automatic hit mods are removed from _all_ weapons at the same time.
     *
     */
    @Test
    void test_clearHitHexMods_with_standard_and_autohit_mods_on_two_weapons() throws LocationFullException {
        tank.addEquipment(sniperType, Tank.LOC_REAR);
        WeaponMounted weapon1 = tank.getWeapon(0);
        WeaponMounted weapon2 = tank.getWeapon(0);
        artilleryTracker.addWeapon(weapon1);
        artilleryTracker.addWeapon(weapon2);

        artilleryTracker.setModifier(7, new Coords(8, 9));
        artilleryTracker.setModifier(TargetRoll.AUTOMATIC_SUCCESS, new Coords(12, 15));

        // Should report two weapons with base mod and autohit mod each
        assertEquals(2, artilleryTracker.getSize());
        assertEquals(2, artilleryTracker.getWeaponModifiers(weapon1).size());
        assertEquals(2, artilleryTracker.getWeaponModifiers(weapon2).size());

        artilleryTracker.clearHitHexMods();

        // Autohit mod is removed from both weapons
        assertEquals(2, artilleryTracker.getSize());
        assertEquals(1, artilleryTracker.getWeaponModifiers(weapon1).size());
        assertEquals(1, artilleryTracker.getWeaponModifiers(weapon2).size());
    }

    // Comm Implant Flag Tests

    /**
     * Test that spotterHasCommImplant defaults to false
     */
    @Test
    void test_spotterHasCommImplant_defaultFalse() {
        assertFalse(artilleryTracker.getSpotterHasCommImplant());
    }

    /**
     * Test setting spotterHasCommImplant to true
     */
    @Test
    void test_setSpotterHasCommImplant_true() {
        artilleryTracker.setSpotterHasCommImplant(true);
        assertTrue(artilleryTracker.getSpotterHasCommImplant());
    }

    /**
     * Test setting spotterHasCommImplant back to false
     */
    @Test
    void test_setSpotterHasCommImplant_false() {
        artilleryTracker.setSpotterHasCommImplant(true);
        artilleryTracker.setSpotterHasCommImplant(false);
        assertFalse(artilleryTracker.getSpotterHasCommImplant());
    }

    /**
     * Test that spotterHasForwardObs defaults to false
     */
    @Test
    void test_spotterHasForwardObs_defaultFalse() {
        assertFalse(artilleryTracker.getSpotterHasForwardObs());
    }

    /**
     * Test setting spotterHasForwardObs to true
     */
    @Test
    void test_setSpotterHasForwardObs_true() {
        artilleryTracker.setSpotterHasForwardObs(true);
        assertTrue(artilleryTracker.getSpotterHasForwardObs());
    }
}
