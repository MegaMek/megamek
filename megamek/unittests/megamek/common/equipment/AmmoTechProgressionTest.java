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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import megamek.common.SimpleTechLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Regression tests for issue #9644: standard ammo uses the tech progression of the gun that fires it, so the two
 * always show the same rules level. Before the fix, some ammo reached production or common use years after its gun
 * (or never), so a game could allow a gun but not its ammo.
 */
class AmmoTechProgressionTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @ParameterizedTest(name = "{1} ({2}) follows {0}")
    @CsvSource({
          "ISC3RemoteSensorLauncher, ISC3Sensors, false",
          "CLHAG20, Hyper-Assault Gauss Rifle/20 Ammo, true",
          "CLHAG30, Hyper-Assault Gauss Rifle/30 Ammo, true",
          "CLHAG40, Hyper-Assault Gauss Rifle/40 Ammo, true",
          "Heavy Flamer, IS Heavy Flamer Ammo, false",
          "CLHeavyFlamer, CL Heavy Flamer Ammo, true",
          "ISVehicularGrenadeLauncher, IS Ammo VGL, false",
          "ISVehicularGrenadeLauncher, IS Ammo VGL, true",
          "ISVehicularGrenadeLauncher, CL Ammo VGL, false",
          "ISVehicularGrenadeLauncher, CL Ammo VGL, true",
          "IS LAAMissile, LAAMissile Ammo, false",
          "Clan LAAMissile, LAAMissile Ammo, true",
          "SRT 2, IS Ammo SRTorpedo-2, false",
          "SRT 4, IS Ammo SRTorpedo-4, false",
          "SRT 6, IS Ammo SRTorpedo-6, false",
          "LRT 10, IS Ammo LRTorpedo-10, false",
          "LRT 15, IS Ammo LRTorpedo-15, false",
          "LRT 20, IS Ammo LRTorpedo-20, false",
          "RL10, IS Ammo RL-10, false",
          "RL10, IS Ammo RL-10, true",
          "RL15, IS Ammo RL-15, false",
          "RL20, IS Ammo RL-20, false",
          "Naval Autocannon (NAC/10), Ammo NAC/10, false",
          "Naval Autocannon (NAC/10), Ammo NAC/10, true",
          "Naval Autocannon (NAC/20), Ammo NAC/20, false",
          "Naval Autocannon (NAC/20), Ammo NAC/20, true"
    })
    void ammoHasSameDatesAsGun(String weaponName, String ammoName, boolean clan) {
        EquipmentType weapon = EquipmentType.get(weaponName);
        EquipmentType ammo = EquipmentType.get(ammoName);
        assertNotNull(weapon, weaponName + " was not found");
        assertNotNull(ammo, ammoName + " was not found");

        assertAll(
              () -> assertEquals(weapon.getPrototypeDate(clan), ammo.getPrototypeDate(clan), "prototype date"),
              () -> assertEquals(weapon.getProductionDate(clan), ammo.getProductionDate(clan), "production date"),
              () -> assertEquals(weapon.getCommonDate(clan), ammo.getCommonDate(clan), "common date"),
              () -> assertEquals(weapon.getExtinctionDate(clan), ammo.getExtinctionDate(clan), "extinction date"),
              () -> assertEquals(weapon.getReintroductionDate(clan), ammo.getReintroductionDate(clan),
                    "reintroduction date"));
    }

    @Test
    void hyperAssaultGaussAmmoIsAdvancedWithItsGunIn3060() {
        EquipmentType weapon = EquipmentType.get("CLHAG30");
        EquipmentType ammo = EquipmentType.get("Hyper-Assault Gauss Rifle/30 Ammo");

        assertEquals(SimpleTechLevel.ADVANCED, weapon.getSimpleLevel(3060, true));
        assertEquals(SimpleTechLevel.ADVANCED, ammo.getSimpleLevel(3060, true));
    }

    @Test
    void remoteSensorAmmoHasSameStaticLevelAsLauncher() {
        EquipmentType weapon = EquipmentType.get("ISC3RemoteSensorLauncher");
        EquipmentType ammo = EquipmentType.get("ISC3Sensors");

        assertEquals(weapon.getStaticTechLevel(), ammo.getStaticTechLevel());
    }
}
