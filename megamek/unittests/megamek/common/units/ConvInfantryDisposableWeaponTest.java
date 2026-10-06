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

import java.lang.reflect.Field;
import java.util.List;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ConvInfantry#equipDisposableWeapon} - the in-place loadout change used by the lobby configuration dialog
 * - keeps the disposable weapon field and the fireable mount in sync when adding, replacing and removing a Disposable
 * Weapon (TO:AuE p.116, Corrected Sixth Printing).
 */
class ConvInfantryDisposableWeaponTest {

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
    }

    private ConvInfantry createInfantry() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setGame(game);
        infantry.setId(game.getNextEntityId());
        infantry.setChassis("Test Platoon");
        infantry.setOwner(game.getPlayer(0));
        infantry.setPrimaryWeapon((InfantryWeapon) EquipmentType.get("InfantryAssaultRifle"));
        infantry.autoSetInternal();
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        return infantry;
    }

    private static List<WeaponMounted> disposableMounts(ConvInfantry infantry) {
        return infantry.getWeaponList().stream().filter(WeaponMounted::isDisposableWeapon).toList();
    }

    @Test
    @DisplayName("equipping a Disposable Weapon sets the field and adds one marked mount")
    void equippingAddsMarkedMount() {
        ConvInfantry infantry = createInfantry();
        InfantryWeapon law = (InfantryWeapon) EquipmentType.get("Rocket Launcher (LAW)");

        infantry.equipDisposableWeapon(law);

        assertTrue(infantry.hasDisposableWeapon());
        assertEquals(law, infantry.getDisposableWeapon());
        List<WeaponMounted> mounts = disposableMounts(infantry);
        assertEquals(1, mounts.size(), "Exactly one marked disposable mount should exist");
        assertEquals(law, mounts.getFirst().getType());
    }

    @Test
    @DisplayName("replacing a Disposable Weapon leaves exactly one mount, of the new type")
    void replacingSwapsMount() {
        ConvInfantry infantry = createInfantry();
        infantry.equipDisposableWeapon((InfantryWeapon) EquipmentType.get("Rocket Launcher (LAW)"));
        InfantryWeapon grenade = (InfantryWeapon) EquipmentType.get("InfantryGrenade");

        infantry.equipDisposableWeapon(grenade);

        assertEquals(grenade, infantry.getDisposableWeapon());
        List<WeaponMounted> mounts = disposableMounts(infantry);
        assertEquals(1, mounts.size(), "Replacing should not leave a stale mount");
        assertEquals(grenade, mounts.getFirst().getType());
    }

    @Test
    @DisplayName("equipping null removes the Disposable Weapon and its mount")
    void equippingNullRemovesMount() {
        ConvInfantry infantry = createInfantry();
        infantry.equipDisposableWeapon((InfantryWeapon) EquipmentType.get("Rocket Launcher (LAW)"));

        infantry.equipDisposableWeapon(null);

        assertFalse(infantry.hasDisposableWeapon());
        assertTrue(disposableMounts(infantry).isEmpty(), "No disposable mount should remain");
    }

    @Test
    @DisplayName("the Disposable Weapon is reconstructed by name after client/server serialization")
    void disposableWeaponSurvivesSerialization() throws Exception {
        ConvInfantry infantry = createInfantry();
        InfantryWeapon law = (InfantryWeapon) EquipmentType.get("Rocket Launcher (LAW)");
        infantry.equipDisposableWeapon(law);

        // Simulate the transient weapon reference being dropped by entity serialization (the name is kept).
        Field disposableWeaponField = ConvInfantry.class.getDeclaredField("disposableWeapon");
        disposableWeaponField.setAccessible(true);
        disposableWeaponField.set(infantry, null);

        assertTrue(infantry.hasDisposableWeapon(), "Disposable Weapon should be restored from its saved name");
        assertEquals(law, infantry.getDisposableWeapon());
        assertEquals("Rocket Launcher (LAW)", infantry.getDisposableWeapon().getInternalName());
    }

    @Test
    @DisplayName("primary and secondary weapons are reconstructed by internal name after client/server serialization")
    void infantryWeaponsSurviveSerialization() throws Exception {
        ConvInfantry infantry = createInfantry();
        InfantryWeapon primaryWeapon = infantry.getPrimaryWeapon();
        InfantryWeapon secondaryWeapon = (InfantryWeapon) EquipmentType.get("InfantryGrenade");
        infantry.setSecondaryWeapon(secondaryWeapon);

        clearWeaponReference(infantry, "primaryWeapon");
        clearWeaponReference(infantry, "secondaryWeapon");

        infantry.restore();

        assertEquals(primaryWeapon, infantry.getPrimaryWeapon());
        assertEquals(secondaryWeapon, infantry.getSecondaryWeapon());
    }

    private static void clearWeaponReference(ConvInfantry infantry, String fieldName) throws Exception {
        Field weaponField = ConvInfantry.class.getDeclaredField(fieldName);
        weaponField.setAccessible(true);
        weaponField.set(infantry, null);
    }
}
