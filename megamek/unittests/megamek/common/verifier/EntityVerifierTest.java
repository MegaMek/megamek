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

        assertEquals(TestEntity.Ceil.HALFTON, result.aeroOption.getWeightCeilingEngine());
        assertEquals(TestEntity.Ceil.HALFTON, result.aeroOption.getWeightCeilingStructure());
        assertEquals(TestEntity.Ceil.HALFTON, result.aeroOption.getWeightCeilingArmor());
        assertEquals(TestEntity.Ceil.HALFTON, result.aeroOption.getWeightCeilingControls());
        assertEquals(TestEntity.Ceil.TON, result.aeroOption.getWeightCeilingWeapons());
        assertEquals(TestEntity.Ceil.TON, result.aeroOption.getWeightCeilingTargComp());
        assertEquals(TestEntity.Ceil.HALFTON, result.aeroOption.getWeightCeilingTurret());
        assertEquals(TestEntity.Ceil.HALFTON, result.aeroOption.getWeightCeilingLifting());
        assertEquals(TestEntity.Ceil.HALFTON, result.aeroOption.getWeightCeilingPowerAmp());
        assertEquals(TestEntity.Ceil.HALFTON, result.aeroOption.getWeightCeilingGyro());
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
}
