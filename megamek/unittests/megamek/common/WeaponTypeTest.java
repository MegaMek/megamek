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

import megamek.common.equipment.WeaponMounted;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.bayweapons.PPCBayWeapon;
import megamek.common.weapons.ppc.ISERPPC;
import megamek.common.weapons.ppc.PPCWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Enumeration;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WeaponTypeTest {

    private Entity mockEntity = mock(Entity.class);

    @BeforeAll
    static void before(){
        EquipmentType.initializeTypes();
    }

    @Test
    public void testArtemisCompatibleFlag() {
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements(); ) {
            EquipmentType etype = e.nextElement();
            if (etype instanceof WeaponType) {
                int atype = ((WeaponType) etype).getAmmoType();

                assertEquals(etype.hasFlag(WeaponType.F_ARTEMIS_COMPATIBLE),
                        (atype == AmmoType.T_LRM)
                        || (atype == AmmoType.T_LRM_IMP)
                        || (atype == AmmoType.T_MML)
                        || (atype == AmmoType.T_SRM)
                        || (atype == AmmoType.T_SRM_IMP)
                        || (atype == AmmoType.T_NLRM)
                        || (atype == AmmoType.T_LRM_TORPEDO)
                        || (atype == AmmoType.T_SRM_TORPEDO)
                        || (atype == AmmoType.T_LRM_TORPEDO_COMBO));
            }
        }
    }

    private WeaponMounted setupBayWeapon(String name){
        EquipmentType etype = EquipmentType.get(name);
        WeaponMounted weapon = new WeaponMounted(mockEntity, (WeaponType) etype);
        WeaponMounted bWeapon = new WeaponMounted(mockEntity, (WeaponType) weapon.getType().getBayType());
        bWeapon.addWeaponToBay(mockEntity.getEquipmentNum(weapon));
        when(mockEntity.getEquipment(anyInt())).thenReturn((Mounted) weapon);

        return bWeapon;
    }

    @Test
    public void testWeaponBaysGetCorrectMaxRanges() {
        WeaponMounted ppcbay = setupBayWeapon("ISERPPC");
        WeaponType wtype = (WeaponType) ppcbay.getType();
        assertEquals(RangeType.RANGE_LONG, wtype.getMaxRange(ppcbay));

        WeaponMounted erplasbay = setupBayWeapon("CLERLargePulseLaser");
        wtype = (WeaponType) erplasbay.getType();
        assertEquals(RangeType.RANGE_LONG, wtype.getMaxRange(erplasbay ));

        WeaponMounted islplasbay = setupBayWeapon("ISLargePulseLaser");
        wtype = (WeaponType) islplasbay.getType();
        assertEquals(RangeType.RANGE_MEDIUM, wtype.getMaxRange(islplasbay));

        WeaponMounted ersmlasbay = setupBayWeapon("CLERSmallLaser");
        wtype = (WeaponType) ersmlasbay.getType();
        assertEquals(RangeType.RANGE_SHORT, wtype.getMaxRange(ersmlasbay));

        WeaponMounted islgaussbay = setupBayWeapon("ISLightGaussRifle");
        wtype = (WeaponType) islgaussbay .getType();
        assertEquals(RangeType.RANGE_EXTREME, wtype.getMaxRange(islgaussbay ));
    }
}