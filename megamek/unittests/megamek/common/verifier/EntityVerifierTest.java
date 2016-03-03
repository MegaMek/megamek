/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.verifier;

import java.io.File;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author nderwin
 */
public class EntityVerifierTest {

    @Test
    public void testEmpty() {
        File file = new File(getClass().getResource("empty-verifier-options.xml").getFile());

        EntityVerifier result = EntityVerifier.getInstance(file);

        assertNotNull(result.aeroOption);
        assertNotNull(result.baOption);
        assertNotNull(result.mechOption);
        assertNotNull(result.tankOption);

        assertEquals(TestEntity.CEIL_HALFTON, result.aeroOption.getWeightCeilingEngine(), 0.0f);
        assertEquals(TestEntity.CEIL_HALFTON, result.aeroOption.getWeightCeilingStructure(), 0.0f);
        assertEquals(TestEntity.CEIL_HALFTON, result.aeroOption.getWeightCeilingArmor(), 0.0f);
        assertEquals(TestEntity.CEIL_HALFTON, result.aeroOption.getWeightCeilingControls(), 0.0f);
        assertEquals(TestEntity.CEIL_TON, result.aeroOption.getWeightCeilingWeapons(), 0.0f);
        assertEquals(TestEntity.CEIL_TON, result.aeroOption.getWeightCeilingTargComp(), 0.0f);
        assertEquals(TestEntity.CEIL_HALFTON, result.aeroOption.getWeightCeilingTurret(), 0.0f);
        assertEquals(TestEntity.CEIL_HALFTON, result.aeroOption.getWeightCeilingLifting(), 0.0f);
        assertEquals(TestEntity.CEIL_HALFTON, result.aeroOption.getWeightCeilingPowerAmp(), 0.0f);
        assertEquals(TestEntity.CEIL_HALFTON, result.aeroOption.getWeightCeilingGyro(), 0.0f);
        assertEquals(0.25f, result.aeroOption.getMaxOverweight(), 0.0f);
        assertTrue(result.aeroOption.showOverweightedEntity());
        assertEquals(1.0f, result.aeroOption.getMinUnderweight(), 0.0f);
        assertFalse(result.aeroOption.showUnderweightedEntity());
        assertFalse(result.aeroOption.ignoreFailedEquip("Claw (THB)"));
        assertFalse(result.aeroOption.skip());
        assertTrue(result.aeroOption.showCorrectArmor());
        assertTrue(result.aeroOption.showCorrectCritical());
        assertTrue(result.aeroOption.showFailedEquip());
        assertEquals(0, result.aeroOption.getTargCompCrits());
        assertEquals(70, result.aeroOption.getPrintSize());
    }

    @Test
    public void testBuild() {
        File file = new File(getClass().getResource("test-verifier-options.xml").getFile());

        EntityVerifier result = EntityVerifier.getInstance(file);
        
        assertNotNull(result.mechOption);

        float halfTon = 1/0.75f;
        float ton = 1/0.99f;
        
        assertEquals(halfTon, result.mechOption.getWeightCeilingEngine(), 0.0f);
        assertEquals(halfTon, result.mechOption.getWeightCeilingStructure(), 0.0f);
        assertEquals(halfTon, result.mechOption.getWeightCeilingArmor(), 0.0f);
        assertEquals(halfTon, result.mechOption.getWeightCeilingControls(), 0.0f);
        assertEquals(ton, result.mechOption.getWeightCeilingWeapons(), 0.0f);
        assertEquals(ton, result.mechOption.getWeightCeilingTargComp(), 0.0f);
        assertEquals(TestEntity.CEIL_HALFTON, result.mechOption.getWeightCeilingTurret(), 0.0f);
        assertEquals(TestEntity.CEIL_HALFTON, result.mechOption.getWeightCeilingLifting(), 0.0f);
        assertEquals(TestEntity.CEIL_HALFTON, result.mechOption.getWeightCeilingPowerAmp(), 0.0f);
        assertEquals(halfTon, result.mechOption.getWeightCeilingGyro(), 0.0f);
        assertEquals(1.0f, result.mechOption.getMaxOverweight(), 0.0f);
        assertFalse(result.mechOption.showOverweightedEntity());
        assertEquals(0.99f, result.mechOption.getMinUnderweight(), 0.0f);
        assertTrue(result.mechOption.showUnderweightedEntity());
        assertTrue(result.mechOption.ignoreFailedEquip("The Claw"));
        assertTrue(result.mechOption.ignoreFailedEquip("BFG-2000"));
        assertTrue(result.mechOption.ignoreFailedEquip("Stereo Console"));
        assertTrue(result.mechOption.ignoreFailedEquip("Dual Speakers"));
        assertFalse(result.mechOption.skip());
        assertFalse(result.mechOption.showCorrectArmor());
        assertFalse(result.mechOption.showCorrectCritical());
        assertFalse(result.mechOption.showFailedEquip());
        assertEquals(1, result.mechOption.getTargCompCrits());
        assertEquals(75, result.mechOption.getPrintSize());
    }
}
