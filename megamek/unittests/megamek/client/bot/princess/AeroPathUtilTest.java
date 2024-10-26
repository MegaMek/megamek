/*
 * Copyright (c) 2000-2011 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import megamek.common.Entity;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.utils.MockGenerators;

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
		when(mockPath.contains(MoveStepType.VLAND)).thenReturn(true);

		boolean result = AeroPathUtil.willStall(mockPath);
		assertFalse(result);
	}

	@Test
	void testAssertWillCrashWithOutLandOrVland() {
		final Entity mockEntity = MockGenerators.generateMockAerospace(0, 0);

		final MovePath mockPath = MockGenerators.generateMockPath(16, 16, mockEntity);
		when(mockPath.getFinalVelocity()).thenReturn(1);

		boolean result = AeroPathUtil.willCrash(mockPath);
		assertTrue(result);
	}

	@Test
	void testAssertWillCrashWithLandOrVland() {
		final Entity mockEntity = MockGenerators.generateMockAerospace(0, 0);

		final MovePath mockPath = MockGenerators.generateMockPath(16, 16, mockEntity);
		when(mockPath.getFinalVelocity()).thenReturn(0);
		when(mockPath.contains(MoveStepType.VLAND)).thenReturn(true);
		when(mockPath.contains(MoveStepType.LAND)).thenReturn(true);

		boolean result = AeroPathUtil.willCrash(mockPath);
		assertFalse(result);
	}
}
