/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Enumeration;

import megamek.common.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.WeaponMounted;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WeaponTypeTest {

    private Entity mockEntity = mock(Entity.class);

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testArtemisCompatibleFlag() {
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements(); ) {
            EquipmentType equipmentType = e.nextElement();
            if (equipmentType instanceof WeaponType weaponType) {
                AmmoTypeEnum ammoType = weaponType.getAmmoType();

                assertEquals(equipmentType.hasFlag(WeaponType.F_ARTEMIS_COMPATIBLE),
                      (ammoType == AmmoType.AmmoTypeEnum.LRM)
                            || (ammoType == AmmoType.AmmoTypeEnum.LRM_IMP)
                            || (ammoType == AmmoType.AmmoTypeEnum.MML)
                            || (ammoType == AmmoType.AmmoTypeEnum.SRM)
                            || (ammoType == AmmoType.AmmoTypeEnum.SRM_IMP)
                            || (ammoType == AmmoType.AmmoTypeEnum.NLRM)
                            || (ammoType == AmmoType.AmmoTypeEnum.LRM_TORPEDO)
                            || (ammoType == AmmoType.AmmoTypeEnum.SRM_TORPEDO)
                            || (ammoType == AmmoType.AmmoTypeEnum.LRM_TORPEDO_COMBO));
            }
        }
    }

    private WeaponMounted setupBayWeapon(String name) {
        EquipmentType etype = EquipmentType.get(name);
        WeaponMounted weapon = new WeaponMounted(mockEntity, (WeaponType) etype);
        WeaponMounted bWeapon = new WeaponMounted(mockEntity, (WeaponType) weapon.getType().getBayType());
        bWeapon.addWeaponToBay(mockEntity.getEquipmentNum(weapon));
        when(mockEntity.getWeapon(anyInt())).thenReturn(weapon);

        return bWeapon;
    }

    @Test
    void testWeaponBaysGetCorrectMaxRanges() {
        WeaponMounted ppcBay = setupBayWeapon("ISERPPC");
        WeaponType weaponType = ppcBay.getType();
        assertEquals(RangeType.RANGE_LONG, weaponType.getMaxRange(ppcBay));

        WeaponMounted clanERPulseLargeLaserBay = setupBayWeapon("CLERLargePulseLaser");
        weaponType = clanERPulseLargeLaserBay.getType();
        assertEquals(RangeType.RANGE_LONG, weaponType.getMaxRange(clanERPulseLargeLaserBay));

        WeaponMounted isLargePulseLaserBay = setupBayWeapon("ISLargePulseLaser");
        weaponType = isLargePulseLaserBay.getType();
        assertEquals(RangeType.RANGE_MEDIUM, weaponType.getMaxRange(isLargePulseLaserBay));

        WeaponMounted clanERSmallLaserBay = setupBayWeapon("CLERSmallLaser");
        weaponType = clanERSmallLaserBay.getType();
        assertEquals(RangeType.RANGE_SHORT, weaponType.getMaxRange(clanERSmallLaserBay));

        WeaponMounted isLightGaussRifleBay = setupBayWeapon("ISLightGaussRifle");
        weaponType = isLightGaussRifleBay.getType();
        assertEquals(RangeType.RANGE_EXTREME, weaponType.getMaxRange(isLightGaussRifleBay));
    }
}
