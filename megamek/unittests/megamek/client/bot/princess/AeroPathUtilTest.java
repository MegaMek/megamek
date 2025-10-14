/*
 * Copyright (c) 2000-2011 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.units.Entity;
import megamek.utils.MockGenerators;
import org.junit.jupiter.api.Test;

/**
 * @author Richard J Hancock
 * @since 07/24/2024
 */
class AeroPathUtilTest {
    @Test
    void testAssertWillStallOnAtmosphereGroundMap() {
        final Entity mockEntity = MockGenerators.generateMockAerospace(0, 0);
        final MovePath mockPath = MockGenerators.generateMockPath(16, 16, mockEntity);
        when(mockPath.isOnAtmosphericGroundMap()).thenReturn(false);

        boolean result = AeroPathUtil.willStall(mockPath);
        assertFalse(result);
    }

    @Test
    void testAssertWillStallAsSpheroidDropshipWithVLAND() {
        final Entity mockEntity = MockGenerators.generateMockAerospace(0, 0);
        when(mockEntity.isSpheroid()).thenReturn(true);

        final MovePath mockPath = MockGenerators.generateMockPath(16, 16, mockEntity);
        when(mockPath.isOnAtmosphericGroundMap()).thenReturn(true);
        when(mockPath.getFinalVelocity()).thenReturn(0);
        when(mockPath.getFinalNDown()).thenReturn(0);
        when(mockPath.getMpUsed()).thenReturn(0);
        when(mockPath.contains(MoveStepType.VERTICAL_LAND)).thenReturn(true);

        boolean result = AeroPathUtil.willStall(mockPath);
        assertFalse(result);
    }

    @Test
    void testAssertWillCrashWithOutLandOrVerticalLand() {
        final Entity mockEntity = MockGenerators.generateMockAerospace(0, 0);

        final MovePath mockPath = MockGenerators.generateMockPath(16, 16, mockEntity);
        when(mockPath.getFinalVelocity()).thenReturn(1);
        when(mockPath.isOnAtmosphericGroundMap()).thenReturn(true);

        boolean result = AeroPathUtil.willCrash(mockPath);
        assertTrue(result);
    }

    @Test
    void testAssertWillCrashWithLandOrVerticalLand() {
        final Entity mockEntity = MockGenerators.generateMockAerospace(0, 0);

        final MovePath mockPath = MockGenerators.generateMockPath(16, 16, mockEntity);
        when(mockPath.getFinalVelocity()).thenReturn(0);
        when(mockPath.contains(MoveStepType.VERTICAL_LAND)).thenReturn(true);
        when(mockPath.contains(MoveStepType.LAND)).thenReturn(true);

        boolean result = AeroPathUtil.willCrash(mockPath);
        assertFalse(result);
    }

    @Test
    void testAssertWillCrashNoAtmosphere() {
        final Entity mockEntity = MockGenerators.generateMockAerospace(0, 0);

        final MovePath mockPath = MockGenerators.generateMockPath(16, 16, mockEntity);
        when(mockPath.getFinalVelocity()).thenReturn(1);
        when(mockPath.isOnAtmosphericGroundMap()).thenReturn(false);

        boolean result = AeroPathUtil.willCrash(mockPath);
        assertFalse(result);
    }

}
