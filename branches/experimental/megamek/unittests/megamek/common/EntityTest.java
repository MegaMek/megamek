/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur
 * (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

import junit.framework.TestCase;
import megamek.common.Entity;
import megamek.common.MechFileParser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.io.File;

/**
 * Created with IntelliJ IDEA.
 *
 * @version $Id$
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/3/13 8:48 AM
 */
@RunWith(JUnit4.class)
public class EntityTest {

    private Entity setupGunEmplacement() {
        Entity testEntity = Mockito.mock(GunEmplacement.class);
        Mockito.when(testEntity.calculateBattleValue()).thenCallRealMethod();
        Mockito.when(testEntity.calculateBattleValue(Mockito.anyBoolean(), Mockito.anyBoolean())).thenCallRealMethod();
        Mockito.when(testEntity.getTotalArmor()).thenReturn(100);
        ArrayList<Mounted> equipment = new ArrayList<Mounted>(2);
        WeaponType ppcType = Mockito.mock(WeaponType.class);
        Mockito.when(ppcType.getBV(Mockito.any(Entity.class))).thenReturn(50.0);
        Mounted ppc = Mockito.mock(Mounted.class);
        Mockito.when(ppc.getType()).thenReturn(ppcType);
        Mockito.when(ppc.isDestroyed()).thenReturn(false);
        equipment.add(ppc);
        equipment.add(ppc);
        Mockito.when(testEntity.getEquipment()).thenReturn(equipment);
        Mockito.when(testEntity.getWeaponList()).thenReturn(equipment);
        Mockito.when(testEntity.getAmmo()).thenReturn(new ArrayList<Mounted>(0));
        return testEntity;
    }

    @Test
    public void testCalculateBattleValue() {
        // Test a gun emplacement.
        Entity testEntity = setupGunEmplacement();
        Mockito.when(testEntity.useGeometricMeanBV()).thenReturn(false);
        int expected = 94;
        int actual = testEntity.calculateBattleValue(true, true);
        TestCase.assertEquals(expected, actual);
        Mockito.when(testEntity.useGeometricMeanBV()).thenReturn(true);
        expected = 94;
        actual = testEntity.calculateBattleValue(true, true);
        TestCase.assertEquals(expected, actual);
        Mockito.when(testEntity.getTotalArmor()).thenReturn(0); // Gun Emplacement with no armor.
        Mockito.when(testEntity.useGeometricMeanBV()).thenReturn(false);
        expected = 44;
        actual = testEntity.calculateBattleValue(true, true);
        TestCase.assertEquals(expected, actual);
        Mockito.when(testEntity.useGeometricMeanBV()).thenReturn(true);
        expected = 44;
        actual = testEntity.calculateBattleValue(true, true);
        TestCase.assertEquals(expected, actual);
    }
    
    @Test
    public void testCalculateWeight() {
        File f; 
        MechFileParser mfp;
        Entity e;
        int expectedWeight, computedWeight;
        
        // Test 1/1
        try {
            f = new File("data/mechfiles/mechs/3039/Exterminator EXT-4A.MTF");
            mfp  = new MechFileParser(f);
            e = mfp.getEntity();
            expectedWeight = 65;
            computedWeight = (int)e.getWeight();
            TestCase.assertEquals(expectedWeight, computedWeight);
        } catch (Exception exc){
            TestCase.fail(exc.getMessage());
        }
    }
    
}
