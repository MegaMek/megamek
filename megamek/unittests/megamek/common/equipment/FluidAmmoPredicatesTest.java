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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.AmmoType.Munitions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the fluid-ammunition classification predicates used by the to-hit and firefighting code:
 * which fluids can put out fires ({@link AmmoType#isFireSuppressantFluid()}) and which are stable in a
 * unit's own heat ({@link AmmoType#isHeatStableFluid()}) (TO:AUE pp.172-174).
 */
class FluidAmmoPredicatesTest {

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
    void onlyWaterCoolantAndFoamSuppressFires() {
        assertTrue(fluidGun(Munitions.M_WATER).isFireSuppressantFluid(), "Water suppresses fires");
        assertTrue(fluidGun(Munitions.M_COOLANT).isFireSuppressantFluid(), "Coolant suppresses fires");
        assertTrue(fluidGun(Munitions.M_ANTI_FLAME_FOAM).isFireSuppressantFluid(), "Foam suppresses fires");

        assertFalse(fluidGun(Munitions.M_CORROSIVE).isFireSuppressantFluid(), "Corrosive does not");
        assertFalse(fluidGun(Munitions.M_OIL_SLICK).isFireSuppressantFluid(), "Oil Slick does not");
        assertFalse(fluidGun(Munitions.M_PAINT_OBSCURANT).isFireSuppressantFluid(), "Paint does not");
        assertFalse(fluidGun(Munitions.M_INFERNO_FUEL).isFireSuppressantFluid(), "Inferno Fuel does not");
    }

    @Test
    void oilSlickAndInfernoFuelAreNotHeatStable() {
        assertTrue(fluidGun(Munitions.M_WATER).isHeatStableFluid(), "Water does not cook off");
        assertTrue(fluidGun(Munitions.M_COOLANT).isHeatStableFluid(), "Coolant does not cook off");
        assertTrue(fluidGun(Munitions.M_CORROSIVE).isHeatStableFluid(), "Corrosive does not cook off");
        assertTrue(fluidGun(Munitions.M_ANTI_FLAME_FOAM).isHeatStableFluid(), "Foam does not cook off");
        assertTrue(fluidGun(Munitions.M_PAINT_OBSCURANT).isHeatStableFluid(), "Paint does not cook off");

        assertFalse(fluidGun(Munitions.M_OIL_SLICK).isHeatStableFluid(), "Oil Slick cooks off normally");
        assertFalse(fluidGun(Munitions.M_INFERNO_FUEL).isHeatStableFluid(), "Inferno Fuel cooks off normally");
    }

    @Test
    void onlyInfernoFuelIsTreatedAsInfernoForCookOff() {
        assertTrue(fluidGun(Munitions.M_INFERNO_FUEL).isInfernoFuel(), "Inferno Fuel cooks off as Inferno SRMs");

        assertFalse(fluidGun(Munitions.M_WATER).isInfernoFuel(), "Water is not Inferno Fuel");
        assertFalse(fluidGun(Munitions.M_COOLANT).isInfernoFuel(), "Coolant is not Inferno Fuel");
        assertFalse(fluidGun(Munitions.M_CORROSIVE).isInfernoFuel(), "Corrosive is not Inferno Fuel");
        assertFalse(fluidGun(Munitions.M_ANTI_FLAME_FOAM).isInfernoFuel(), "Foam is not Inferno Fuel");
        assertFalse(fluidGun(Munitions.M_OIL_SLICK).isInfernoFuel(), "Oil Slick is not Inferno Fuel");
        assertFalse(fluidGun(Munitions.M_PAINT_OBSCURANT).isInfernoFuel(), "Paint is not Inferno Fuel");
    }
}
