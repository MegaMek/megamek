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
package megamek.common.actions.compute;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponType;
import megamek.common.weapons.artillery.ArtilleryCannonWeapon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the area-effect classification used to exclude the dug-in / fortified / hit-the-deck cover bonus (TO:AR
 * p.106 / TO:AUE p.153).
 */
class ComputeTerrainModsTest {

    @Test
    @DisplayName("Artillery is area-effect against infantry")
    void artilleryIsAreaEffect() {
        WeaponType artillery = mock(WeaponType.class);
        when(artillery.hasFlag(WeaponType.F_ARTILLERY)).thenReturn(true);
        assertTrue(ComputeTerrainMods.isAreaEffectAgainstInfantry(artillery, null));
    }

    @Test
    @DisplayName("Artillery cannons are area-effect against infantry")
    void artilleryCannonIsAreaEffect() {
        WeaponType cannon = mock(ArtilleryCannonWeapon.class);
        assertTrue(ComputeTerrainMods.isAreaEffectAgainstInfantry(cannon, null));
    }

    @Test
    @DisplayName("Bombs are area-effect against infantry")
    void bombIsAreaEffect() {
        WeaponType bomb = mock(WeaponType.class);
        when(bomb.hasAnyFlag(WeaponType.F_ALT_BOMB, WeaponType.F_DIVE_BOMB, WeaponType.F_SPACE_BOMB))
              .thenReturn(true);
        assertTrue(ComputeTerrainMods.isAreaEffectAgainstInfantry(bomb, null));
    }

    @Test
    @DisplayName("Fuel-air explosive munitions are area-effect against infantry")
    void fuelAirIsAreaEffect() {
        WeaponType weapon = mock(WeaponType.class);
        AmmoType fuelAir = mock(AmmoType.class);
        when(fuelAir.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_FAE));
        assertTrue(ComputeTerrainMods.isAreaEffectAgainstInfantry(weapon, fuelAir));
    }

    @Test
    @DisplayName("A standard direct-fire weapon is not area-effect")
    void standardWeaponIsNotAreaEffect() {
        WeaponType laser = mock(WeaponType.class);
        AmmoType standardAmmo = mock(AmmoType.class);
        when(standardAmmo.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_STANDARD));
        assertFalse(ComputeTerrainMods.isAreaEffectAgainstInfantry(laser, standardAmmo));
        assertFalse(ComputeTerrainMods.isAreaEffectAgainstInfantry(laser, null));
    }
}
