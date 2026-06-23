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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.BipedMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the per-fluid critical-hit explosion side-effects of Fluid Gun ammunition (TO:AUE p.173):
 * Coolant/Foam cool the carrier and explode for a flat 2, Corrosive deals extra and delayed structural
 * damage, and the following-turn corrosive damage is promoted correctly.
 */
class FluidAmmoExplosionEffectsTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static AmmoType fluidGun(Munitions munition) {
        AmmoType ammoType = AmmoType.getMunitionsFor(AmmoTypeEnum.FLUID_GUN).stream()
              .filter(candidate -> candidate.getMunitionType().contains(munition))
              .findFirst()
              .orElse(null);
        assertNotNull(ammoType, "expected " + munition + " Fluid Gun ammo");
        return ammoType;
    }

    @Test
    void coolantExplosionCoolsByThreeAndDealsTwo() {
        BipedMek mek = new BipedMek();
        int damage = FluidAmmoExplosion.applyCriticalEffects(mek, fluidGun(Munitions.M_COOLANT), 99);
        assertEquals(2, damage, "Coolant ammo explodes for a flat 2 points");
        assertEquals(3, mek.coolFromExternal, "Coolant ammo explosion removes 3 heat");
    }

    @Test
    void foamExplosionCoolsByTwoAndDealsTwo() {
        BipedMek mek = new BipedMek();
        int damage = FluidAmmoExplosion.applyCriticalEffects(mek, fluidGun(Munitions.M_ANTI_FLAME_FOAM), 99);
        assertEquals(2, damage, "Flame-Retardant Foam ammo explodes for a flat 2 points");
        assertEquals(2, mek.coolFromExternal, "Foam ammo explosion removes 2 heat");
    }

    @Test
    void corrosiveExplosionAddsImmediateAndFollowingTurnDamage() {
        BipedMek mek = new BipedMek();
        int damage = FluidAmmoExplosion.applyCriticalEffects(mek, fluidGun(Munitions.M_CORROSIVE), 2);
        // Base 2 plus 1D6 immediate internal damage.
        assertTrue((damage >= 3) && (damage <= 8), "Corrosive explosion deals 2 + 1D6 (was " + damage + ")");
        // 1D6/2 (round up) queued for the following turn.
        int followingTurn = mek.getNextTurnCorrosiveDamage();
        assertTrue((followingTurn >= 1) && (followingTurn <= 3),
              "Corrosive queues 1D6/2 for the following turn (was " + followingTurn + ")");
    }

    @Test
    void waterAndOtherFluidsKeepBaseExplosionDamage() {
        BipedMek mek = new BipedMek();
        int damage = FluidAmmoExplosion.applyCriticalEffects(mek, fluidGun(Munitions.M_OIL_SLICK), 7);
        assertEquals(7, damage, "Oil Slick keeps its base (per-shot) explosion damage here");
        assertEquals(0, mek.coolFromExternal, "Oil Slick does not cool the carrier");
    }

    @Test
    void followingTurnDamageIsPromotedToPending() {
        BipedMek mek = new BipedMek();
        mek.addNextTurnCorrosiveDamage(3);
        mek.promoteNextTurnCorrosiveDamage();
        assertEquals(3, mek.getPendingCorrosiveDamage(), "Following-turn corrosive moves into the pending pool");
        assertEquals(0, mek.getNextTurnCorrosiveDamage(), "The following-turn queue is cleared after promotion");
    }
}
