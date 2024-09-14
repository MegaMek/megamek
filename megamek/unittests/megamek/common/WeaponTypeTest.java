/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Enumeration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import megamek.common.equipment.WeaponMounted;

class WeaponTypeTest {

    private Entity mockEntity = mock(Entity.class);

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testArtemisCompatibleFlag() {
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType equipmentType = e.nextElement();
            if (equipmentType instanceof WeaponType weaponType) {
                int ammoType = weaponType.getAmmoType();

                assertEquals(equipmentType.hasFlag(WeaponType.F_ARTEMIS_COMPATIBLE),
                        (ammoType == AmmoType.T_LRM)
                                || (ammoType == AmmoType.T_LRM_IMP)
                                || (ammoType == AmmoType.T_MML)
                                || (ammoType == AmmoType.T_SRM)
                                || (ammoType == AmmoType.T_SRM_IMP)
                                || (ammoType == AmmoType.T_NLRM)
                                || (ammoType == AmmoType.T_LRM_TORPEDO)
                                || (ammoType == AmmoType.T_SRM_TORPEDO)
                                || (ammoType == AmmoType.T_LRM_TORPEDO_COMBO));
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
