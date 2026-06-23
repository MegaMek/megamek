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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Vector;

import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.AmmoType.Munitions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Phase 1 regression coverage for the Flamer / Fluid Gun / Sprayer fluid ammunition definitions
 * (TO:AUE pp.172-175). Verifies that each fluid is available to the weapons whose bracket allows it,
 * that the Fluid Gun base round is Water, and that the Corrosive and Inferno Fuel BV x2.0 modifier
 * is applied.
 */
class FluidMunitionTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static boolean hasMunition(AmmoTypeEnum ammoTypeEnum, Munitions munition) {
        Vector<AmmoType> munitions = AmmoType.getMunitionsFor(ammoTypeEnum);
        if (munitions == null) {
            return false;
        }
        return munitions.stream().anyMatch(ammoType -> ammoType.getMunitionType().contains(munition));
    }

    private static AmmoType firstWith(AmmoTypeEnum ammoTypeEnum, Munitions munition) {
        return AmmoType.getMunitionsFor(ammoTypeEnum).stream()
              .filter(ammoType -> ammoType.getMunitionType().contains(munition))
              .findFirst()
              .orElse(null);
    }

    @Test
    void fluidGunOffersEveryFluid() {
        for (Munitions munition : new Munitions[] { Munitions.M_WATER, Munitions.M_COOLANT,
              Munitions.M_CORROSIVE, Munitions.M_ANTI_FLAME_FOAM, Munitions.M_OIL_SLICK,
              Munitions.M_PAINT_OBSCURANT, Munitions.M_INFERNO_FUEL }) {
            assertTrue(hasMunition(AmmoTypeEnum.FLUID_GUN, munition),
                  "Fluid Gun should offer " + munition);
        }
    }

    @Test
    void flamersOfferOnlyWaterCoolantAndInfernoFuel() {
        for (AmmoTypeEnum flamer : new AmmoTypeEnum[] { AmmoTypeEnum.VEHICLE_FLAMER,
              AmmoTypeEnum.HEAVY_FLAMER }) {
            assertTrue(hasMunition(flamer, Munitions.M_WATER), flamer + " should offer Water");
            assertTrue(hasMunition(flamer, Munitions.M_COOLANT), flamer + " should offer Coolant");
            assertTrue(hasMunition(flamer, Munitions.M_INFERNO_FUEL),
                  flamer + " should offer Inferno Fuel");
            // Fluid-gun-only fluids must not appear on flamers.
            assertTrue(!hasMunition(flamer, Munitions.M_CORROSIVE), flamer + " must not offer Corrosive");
            assertTrue(!hasMunition(flamer, Munitions.M_OIL_SLICK), flamer + " must not offer Oil Slick");
            assertTrue(!hasMunition(flamer, Munitions.M_PAINT_OBSCURANT),
                  flamer + " must not offer Paint/Obscurant");
            assertTrue(!hasMunition(flamer, Munitions.M_ANTI_FLAME_FOAM),
                  flamer + " must not offer Flame-Retardant Foam");
        }
    }

    @Test
    void fluidGunBaseRoundIsWater() {
        AmmoType base = (AmmoType) EquipmentType.get("ISFluidGun Ammo");
        assertTrue(base.getMunitionType().contains(Munitions.M_WATER),
              "The default Fluid Gun round should be Water (TO:AUE p.172)");
    }

    @Test
    void corrosiveAndInfernoFuelDoubleBaseBattleValue() {
        AmmoType water = firstWith(AmmoTypeEnum.FLUID_GUN, Munitions.M_WATER);
        AmmoType corrosive = firstWith(AmmoTypeEnum.FLUID_GUN, Munitions.M_CORROSIVE);
        AmmoType infernoFuel = firstWith(AmmoTypeEnum.FLUID_GUN, Munitions.M_INFERNO_FUEL);
        assertEquals(water.getBV(null) * 2.0, corrosive.getBV(null), 0.001,
              "Corrosive ammo should carry double the base ammo BV");
        assertEquals(water.getBV(null) * 2.0, infernoFuel.getBV(null), 0.001,
              "Inferno Fuel ammo should carry double the base ammo BV");
    }
}
