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
package megamek.common.weapons.sprayers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.bays.LiquidCargoBay;
import megamek.common.compute.Compute;
import megamek.common.exceptions.LocationFullException;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.SupportTank;
import megamek.common.units.Tank;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestSupportVehicle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Phase 2 regression coverage for the Sprayer (TM pp.248-249) now that it is a fireable WeaponType
 * (converted from MiscType) using Fluid Gun ammunition.
 */
class SprayerWeaponTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static WeaponType sprayer(String internalName) {
        EquipmentType equipmentType = EquipmentType.get(internalName);
        assertNotNull(equipmentType, internalName + " should be a registered equipment type");
        assertFalse(equipmentType instanceof MiscType, internalName + " should no longer be a MiscType");
        return assertInstanceOf(WeaponType.class, equipmentType,
              internalName + " should now be a WeaponType");
    }

    @Test
    void mekSprayerIsAFluidGunWeapon() {
        WeaponType mekSprayer = sprayer(EquipmentTypeLookup.SPRAYER_MEK);
        assertTrue(mekSprayer.hasFlag(WeaponType.F_SPRAYER), "Mek Sprayer should carry F_SPRAYER");
        assertTrue(mekSprayer.hasFlag(WeaponType.F_MEK_WEAPON), "Mek Sprayer should be a Mek weapon");
        assertEquals(AmmoTypeEnum.FLUID_GUN, mekSprayer.getAmmoType(),
              "Sprayers fire Fluid Gun ammunition");
        assertEquals(1, mekSprayer.getLongRange(), "Sprayer range is 1 hex");
        assertEquals(0.5, mekSprayer.getTonnage(null), 0.0001,
              "IndustrialMek Sprayer weighs 0.5 tons");
    }

    @Test
    void vehicularSprayerIsAFluidGunWeapon() {
        WeaponType vehicularSprayer = sprayer(EquipmentTypeLookup.SPRAYER_VEE);
        assertTrue(vehicularSprayer.hasFlag(WeaponType.F_SPRAYER),
              "Vehicular Sprayer should carry F_SPRAYER");
        assertTrue(vehicularSprayer.hasFlag(WeaponType.F_TANK_WEAPON),
              "Vehicular Sprayer should be a tank weapon");
        assertEquals(AmmoTypeEnum.FLUID_GUN, vehicularSprayer.getAmmoType(),
              "Sprayers fire Fluid Gun ammunition");
        assertEquals(0.015, vehicularSprayer.getTonnage(null), 0.0001,
              "Vehicular Sprayer weighs 15 kg");
    }

    @Test
    void sprayerCannotBenefitFromATargetingComputer() {
        // Targeting-computer bonuses require F_DIRECT_FIRE; Sprayers must not have it (TM pp.248-249).
        WeaponType mekSprayer = sprayer(EquipmentTypeLookup.SPRAYER_MEK);
        assertFalse(mekSprayer.hasFlag(WeaponType.F_DIRECT_FIRE),
              "Sprayers gain no targeting-computer benefit, so must not be F_DIRECT_FIRE");
    }

    @Test
    void sprayerDoesNotRequireAGunnerOnSupportVehicles() throws LocationFullException {
        // Sprayers do not count as weapons and require no gunner (TM pp.248-249), so adding one to a
        // support vehicle must not change its gunner requirement. (Holds in every weight-class branch.)
        SupportTank tank = new SupportTank();
        tank.setMovementMode(EntityMovementMode.WHEELED);
        tank.setWeight(3);
        tank.addEquipment(EquipmentType.get("Flamer (Vehicle)"), Tank.LOC_FRONT);
        int withoutSprayer = Compute.getSupportVehicleGunnerNeeds(tank);

        tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.SPRAYER_VEE), Tank.LOC_FRONT);
        int withSprayer = Compute.getSupportVehicleGunnerNeeds(tank);

        assertEquals(withoutSprayer, withSprayer, "A Sprayer must not add to gunner requirements");
    }

    @Test
    void sprayerRequiresLiquidCargoStorage() throws LocationFullException {
        SupportTank tank = new SupportTank();
        tank.setMovementMode(EntityMovementMode.WHEELED);
        tank.setWeight(5);
        tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.SPRAYER_VEE), Tank.LOC_FRONT);

        TestSupportVehicle verifier = new TestSupportVehicle(tank,
              EntityVerifier.getInstance(null).tankOption, null);

        StringBuffer withoutCargo = new StringBuffer();
        verifier.hasIllegalEquipmentCombinations(withoutCargo);
        assertTrue(withoutCargo.toString().contains("Sprayer requires liquid cargo"),
              "A Sprayer with no liquid storage should be flagged as illegal");

        // Providing a liquid cargo bay satisfies the requirement (TM pp.248-249).
        tank.addTransporter(new LiquidCargoBay(1, 0, 1));
        StringBuffer withCargo = new StringBuffer();
        verifier.hasIllegalEquipmentCombinations(withCargo);
        assertFalse(withCargo.toString().contains("Sprayer requires liquid cargo"),
              "A liquid cargo bay satisfies the Sprayer's fluid-storage requirement");
    }
}
