/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.swing.calculationReport.CalculationReport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/3/13 8:48 AM
 */
public class EntityTest {

    private Entity setupGunEmplacement() {
        Entity testEntity = mock(GunEmplacement.class);
        when(testEntity.calculateBattleValue()).thenCallRealMethod();
        when(testEntity.calculateBattleValue(anyBoolean(), anyBoolean())).thenCallRealMethod();
        when(testEntity.doBattleValueCalculation(anyBoolean(), anyBoolean(),
                any(CalculationReport.class))).thenCallRealMethod();
        when(testEntity.getTotalArmor()).thenReturn(100);
        ArrayList<Mounted> equipment = new ArrayList<>(2);
        WeaponType ppcType = mock(WeaponType.class);
        when(ppcType.getBV(any(Entity.class))).thenReturn(50.0);
        Mounted ppc = mock(Mounted.class);
        when(ppc.getType()).thenReturn(ppcType);
        when(ppc.isDestroyed()).thenReturn(false);
        equipment.add(ppc);
        equipment.add(ppc);
        when(testEntity.getEquipment()).thenReturn(equipment);
        when(testEntity.getWeaponList()).thenReturn(equipment);
        when(testEntity.getAmmo()).thenReturn(new ArrayList<>(0));
        return testEntity;
    }

    @Test
    public void testCalculateBattleValue() {
        // Test a gun emplacement.
        Entity testEntity = setupGunEmplacement();
        int expected = 94;
        int actual = testEntity.calculateBattleValue(true, true);
        assertEquals(expected, actual);
        when(testEntity.getTotalArmor()).thenReturn(0); // Gun Emplacement with no armor.
        expected = 44;
        actual = testEntity.calculateBattleValue(true, true);
        assertEquals(expected, actual);
    }
    
    @Test
    public void testCalculateWeight() {
        File f; 
        MechFileParser mfp;
        Entity e;
        int expectedWeight, computedWeight;
        
        // Test 1/1
        try {
            f = new File("data/mechfiles/mechs/3050U/Exterminator EXT-4A.mtf");
            mfp  = new MechFileParser(f);
            e = mfp.getEntity();
            expectedWeight = 65;
            computedWeight = (int) e.getWeight();
            assertEquals(expectedWeight, computedWeight);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
