/*
 * MegaMek
 * Copyright (C) 2021 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class WeaponTypeTest {
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
}