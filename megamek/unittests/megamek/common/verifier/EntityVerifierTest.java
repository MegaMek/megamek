/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk)
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
package megamek.common.verifier;

import megamek.common.verifier.TestEntity.Ceil;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
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

        assertEquals(Ceil.HALFTON, result.aeroOption.getWeightCeilingEngine());
        assertEquals(Ceil.HALFTON, result.aeroOption.getWeightCeilingStructure());
        assertEquals(Ceil.HALFTON, result.aeroOption.getWeightCeilingArmor());
        assertEquals(Ceil.HALFTON, result.aeroOption.getWeightCeilingControls());
        assertEquals(Ceil.TON, result.aeroOption.getWeightCeilingWeapons());
        assertEquals(Ceil.TON, result.aeroOption.getWeightCeilingTargComp());
        assertEquals(Ceil.HALFTON, result.aeroOption.getWeightCeilingTurret());
        assertEquals(Ceil.HALFTON, result.aeroOption.getWeightCeilingLifting());
        assertEquals(Ceil.HALFTON, result.aeroOption.getWeightCeilingPowerAmp());
        assertEquals(Ceil.HALFTON, result.aeroOption.getWeightCeilingGyro());
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
