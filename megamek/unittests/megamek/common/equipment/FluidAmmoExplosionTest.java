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
package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the special ammo-explosion damage of Flamer / Fluid Gun fluid ammunition (TO:AUE pp.124,
 * 172-174): a Fluid Gun ammo explosion is a flat 2 points (Oil Slick / Paint add 1 per remaining shot),
 * Water never explodes, and Heavy Flamer ammo does 5 points per unfired shot.
 */
class FluidAmmoExplosionTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static AmmoMounted fluidBin(AmmoTypeEnum ammoTypeEnum, Munitions munition) {
        AmmoType ammoType = AmmoType.getMunitionsFor(ammoTypeEnum).stream()
              .filter(candidate -> candidate.getMunitionType().contains(munition))
              .findFirst()
              .orElse(null);
        assertNotNull(ammoType, "expected " + munition + " ammo for " + ammoTypeEnum);
        Mek mek = new BipedMek();
        return (AmmoMounted) Mounted.createMounted(mek, ammoType);
    }

    @Test
    void waterAmmoNeverExplodes() {
        assertEquals(0, fluidBin(AmmoTypeEnum.FLUID_GUN, Munitions.M_WATER).getExplosionDamage(),
              "Water Ammo never explodes");
    }

    @Test
    void fluidGunExplosionIsFlatTwoPoints() {
        assertEquals(2, fluidBin(AmmoTypeEnum.FLUID_GUN, Munitions.M_COOLANT).getExplosionDamage(),
              "Coolant Fluid Gun ammo explodes for a flat 2 points");
        assertEquals(2, fluidBin(AmmoTypeEnum.FLUID_GUN, Munitions.M_CORROSIVE).getExplosionDamage(),
              "Corrosive Fluid Gun ammo explodes for a flat 2 points (base)");
        assertEquals(2, fluidBin(AmmoTypeEnum.FLUID_GUN, Munitions.M_ANTI_FLAME_FOAM).getExplosionDamage(),
              "Flame-Retardant Foam Fluid Gun ammo explodes for a flat 2 points");
    }

    @Test
    void oilSlickAndPaintAddOnePerRemainingShot() {
        AmmoMounted oilSlick = fluidBin(AmmoTypeEnum.FLUID_GUN, Munitions.M_OIL_SLICK);
        assertEquals(2 + oilSlick.getBaseShotsLeft(), oilSlick.getExplosionDamage(),
              "Oil Slick adds 1 point per unfired shot to the 2-point explosion");

        AmmoMounted paint = fluidBin(AmmoTypeEnum.FLUID_GUN, Munitions.M_PAINT_OBSCURANT);
        assertEquals(2 + paint.getBaseShotsLeft(), paint.getExplosionDamage(),
              "Paint/Obscurant adds 1 point per unfired shot to the 2-point explosion");
    }

    @Test
    void heavyFlamerAmmoIsFivePerShot() {
        AmmoMounted heavyFlamer = fluidBin(AmmoTypeEnum.HEAVY_FLAMER, Munitions.M_COOLANT);
        assertEquals(5 * heavyFlamer.getBaseShotsLeft(), heavyFlamer.getExplosionDamage(),
              "Heavy Flamer ammo explodes for 5 points per unfired shot");
    }
}
