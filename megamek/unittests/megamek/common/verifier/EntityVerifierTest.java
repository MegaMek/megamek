/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * @author nderwin
 */
class EntityVerifierTest {

    @Test
    void testEmpty() {
        File file = new File(getClass().getResource("empty-verifier-options.xml").getFile());

        EntityVerifier result = EntityVerifier.getInstance(file);

        assertNotNull(result.aeroOption);
        assertNotNull(result.baOption);
        assertNotNull(result.mekOption);
        assertNotNull(result.tankOption);

        assertEquals(Ceil.HALF_TON, result.aeroOption.getWeightCeilingEngine());
        assertEquals(Ceil.HALF_TON, result.aeroOption.getWeightCeilingStructure());
        assertEquals(Ceil.HALF_TON, result.aeroOption.getWeightCeilingArmor());
        assertEquals(Ceil.HALF_TON, result.aeroOption.getWeightCeilingControls());
        assertEquals(Ceil.TON, result.aeroOption.getWeightCeilingWeapons());
        assertEquals(Ceil.TON, result.aeroOption.getWeightCeilingTargComp());
        assertEquals(Ceil.HALF_TON, result.aeroOption.getWeightCeilingTurret());
        assertEquals(Ceil.HALF_TON, result.aeroOption.getWeightCeilingLifting());
        assertEquals(Ceil.HALF_TON, result.aeroOption.getWeightCeilingPowerAmp());
        assertEquals(Ceil.HALF_TON, result.aeroOption.getWeightCeilingGyro());
        assertEquals(0.25f, result.aeroOption.getMaxOverweight(), 0.0f);
        assertTrue(result.aeroOption.showOverweightedEntity());
        assertEquals(1.0f, result.aeroOption.getMinUnderweight(), 0.0f);
        assertFalse(result.aeroOption.showUnderweightEntity());
        assertFalse(result.aeroOption.ignoreFailedEquip("Claw (THB)"));
        assertFalse(result.aeroOption.skip());
        assertTrue(result.aeroOption.showCorrectArmor());
        assertTrue(result.aeroOption.showCorrectCritical());
        assertTrue(result.aeroOption.showFailedEquip());
        assertEquals(0, result.aeroOption.getTargetingComputerCrits());
        assertEquals(70, result.aeroOption.getPrintSize());
    }
}
