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
package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.ConvInfantry;
import megamek.common.util.BuildingBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests round-tripping a conventional infantry unit's Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing)
 * through the BLK loader and writer, and verifies that the disposable mount is created and marked.
 */
class BLKInfantryDisposableWeaponTest {

    private static final String PRIMARY_WEAPON = "InfantryGrenade";
    private static final String DISPOSABLE_WEAPON = "Rocket Launcher (LAW)";

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    private static BuildingBlock baseInfantryBlock() {
        BuildingBlock block = new BuildingBlock();
        block.writeBlockData("UnitType", "Infantry");
        block.writeBlockData("Name", "Test Disposable Platoon");
        block.writeBlockData("Model", "");
        block.writeBlockData("year", 3145);
        block.writeBlockData("type", "IS Level 3");
        block.writeBlockData("motion_type", "Leg");
        block.writeBlockData("squad_size", 7);
        block.writeBlockData("squadn", 4);
        block.writeBlockData("Primary", PRIMARY_WEAPON);
        return block;
    }

    @Test
    @DisplayName("a disposableWeapon block loads a marked, fireable disposable mount")
    void loadsDisposableWeaponMount() throws Exception {
        BuildingBlock block = baseInfantryBlock();
        block.writeBlockData("disposableWeapon", DISPOSABLE_WEAPON);

        ConvInfantry infantry = (ConvInfantry) new BLKInfantryFile(block).getEntity();

        assertTrue(infantry.hasDisposableWeapon(), "Platoon should report carrying a Disposable Weapon");
        assertEquals(DISPOSABLE_WEAPON, infantry.getDisposableWeapon().getInternalName(),
              "Disposable Weapon type should match the BLK block");

        List<WeaponMounted> disposableMounts = infantry.getWeaponList().stream()
              .filter(WeaponMounted::isDisposableWeapon)
              .toList();
        assertEquals(1, disposableMounts.size(), "Exactly one mounted weapon should be marked disposable");
        assertTrue(disposableMounts.getFirst().getType().hasFlag(WeaponType.F_INF_DISPOSABLE),
              "The marked mount's weapon type should carry the disposable flag");
    }

    @Test
    @DisplayName("a unit without a disposableWeapon block has no disposable weapon")
    void noDisposableWeaponByDefault() throws Exception {
        ConvInfantry infantry = (ConvInfantry) new BLKInfantryFile(baseInfantryBlock()).getEntity();

        assertTrue(!infantry.hasDisposableWeapon(), "Platoon should not carry a Disposable Weapon by default");
        assertTrue(infantry.getWeaponList().stream().noneMatch(WeaponMounted::isDisposableWeapon),
              "No mounted weapon should be marked disposable");
    }

    @Test
    @DisplayName("a non-disposable weapon in the disposableWeapon block is rejected")
    void rejectsNonDisposableWeapon() {
        BuildingBlock block = baseInfantryBlock();
        // The auto-rifle is a normal infantry weapon without the (1-D) disposable listing.
        block.writeBlockData("disposableWeapon", "InfantryAssaultRifle");

        assertThrows(EntityLoadingException.class, () -> new BLKInfantryFile(block).getEntity(),
              "Loading a non-disposable weapon as a Disposable Weapon should fail");
    }

    @Test
    @DisplayName("the Disposable Weapon round-trips through the BLK writer")
    void writesDisposableWeaponBlock() throws Exception {
        BuildingBlock block = baseInfantryBlock();
        block.writeBlockData("disposableWeapon", DISPOSABLE_WEAPON);
        ConvInfantry infantry = (ConvInfantry) new BLKInfantryFile(block).getEntity();

        BuildingBlock written = BLKFile.getBlock(infantry);

        assertTrue(written.exists("disposableWeapon"), "Written BLK should contain a disposableWeapon block");
        assertEquals(DISPOSABLE_WEAPON, written.getDataAsString("disposableWeapon")[0],
              "Written disposable weapon should match the original");
    }
}
