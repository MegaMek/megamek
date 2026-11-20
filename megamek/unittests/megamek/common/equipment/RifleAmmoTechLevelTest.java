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

import megamek.common.SimpleTechLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Regression tests for the Light, Medium and Heavy Rifle (Cannon) ammunition. The shells share the gun's prototype
 * and production dates (Interstellar Operations: Alternate Eras, 3rd printing, pp. 34 and 55), so before the 3084
 * common date the ammo must show the same rules level as the gun. It used to have no production date and showed as
 * Experimental while the gun showed as Advanced.
 */
class RifleAmmoTechLevelTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @ParameterizedTest
    @CsvSource({
          "ISLightRifle, IS Ammo Light Rifle",
          "ISMediumRifle, IS Ammo Medium Rifle",
          "ISHeavyRifle, IS Ammo Heavy Rifle"
    })
    void rifleAmmoIsAdvancedBeforeCommonDate(String weaponName, String ammoName) {
        EquipmentType weapon = EquipmentType.get(weaponName);
        EquipmentType ammo = EquipmentType.get(ammoName);
        assertNotNull(weapon, weaponName + " was not found");
        assertNotNull(ammo, ammoName + " was not found");

        assertEquals(SimpleTechLevel.ADVANCED, weapon.getSimpleLevel(3050, false));
        assertEquals(SimpleTechLevel.ADVANCED, ammo.getSimpleLevel(3050, false),
              ammoName + " should match its gun's rules level in 3050");
    }

    @ParameterizedTest
    @CsvSource({
          "ISLightRifle, IS Ammo Light Rifle",
          "ISMediumRifle, IS Ammo Medium Rifle",
          "ISHeavyRifle, IS Ammo Heavy Rifle"
    })
    void rifleAmmoMatchesGunProductionDate(String weaponName, String ammoName) {
        EquipmentType weapon = EquipmentType.get(weaponName);
        EquipmentType ammo = EquipmentType.get(ammoName);

        assertEquals(weapon.getProductionDate(false), ammo.getProductionDate(false),
              ammoName + " should enter production with its gun");
    }
}
