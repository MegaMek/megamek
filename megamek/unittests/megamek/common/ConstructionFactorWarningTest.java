/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.common.enums.GamePhase;

public class ConstructionFactorWarningTest {

	final private GUIPreferences GUIP = GUIPreferences.getInstance();

	@Test
	public void testDefaultPreferenceForCFWarningIndicator() {
		// The default setting for Construction Factor warning indicators should be true.
		assertTrue(GUIP.getDefaultBoolean(GUIPreferences.CONSTRUCTOR_FACTOR_WARNING));
	}

	@Test
	public void testSetPreferenceForCFWarningIndicator() {
		// Set the preference for CF warning indicator to false and true.
		GUIP.setShowCFWarnings(false);
		assertFalse(GUIP.getShowCFWarnings());

		// Set back to true and test again.
		GUIP.setShowCFWarnings(true);
		assertTrue(GUIP.getShowCFWarnings());
	}

	@Test
	public void testActionPerformedHandlerTogglesCFWarningPref() {
		// Test logic used when an action is performed.
		// When the action to toggle CF Warning is called, the state of the toggle should flip.
		BoardView bv = mock(BoardView.class);
		Game g = mock(Game.class);
		when(g.getPhase()).thenReturn(GamePhase.DEPLOYMENT);
		boolean expected = !GUIP.getShowCFWarnings();

		ConstructionFactorWarning.handleActionPerformed(g, bv);

		//Expect to see the toggle state for CF Warning toggled after the handler is called.
		assertEquals(expected, GUIP.getShowCFWarnings());
	}

	@Test
	public void testShouldShowCFWarning () {
		// should return false if feature is disabled.
		boolean actual = ConstructionFactorWarning.shouldShow(GamePhase.DEPLOYMENT, false);	
		assertFalse(actual);

		// should return true if enabled and in the deployment phase.
		actual = ConstructionFactorWarning.shouldShow(GamePhase.DEPLOYMENT, true);
		assertTrue(actual);

		// should return true if enabled and in the movement phase.
		actual = ConstructionFactorWarning.shouldShow(GamePhase.MOVEMENT, true);
		assertTrue(actual);

		// should return false if enabled and NOT in the movement phase.
		actual = ConstructionFactorWarning.shouldShow(GamePhase.FIRING, true);
		assertFalse(actual);
	}

//	@Test
//	public void testActionPerformedHandleToggleCFWarningFiring() {
//		// If the current phase is Movement or Deploy, then signal BoardView to trigger
//		// calculating and adding the sprites.
//		BoardView bv = mock(BoardView.class);
//		Game g = mock(Game.class);
//		when(g.getPhase()).thenReturn(GamePhase.FIRING);
//		GUIP.setShowCFWarnings(false);
//
//		ConstructionFactorWarning.handleActionPerformed(g, bv);
//
//		// ensure we are clearing CF warning data when in a non-valid phase.
//		verify(bv, times(1)).clearCFWarningData();
//	}

	@Test
	public void testMovementDisplayEntititySelectedHandler() {
		// Given an entity in the movement phase, calculate the hexes with 
		// buildings within a movement radius of the entity that have buildings
		// with CF lower than the sum of the entities tonnage and all other entities
		// on that hex.

		Entity e = mock(Entity.class);
		when(e.getPosition()).thenReturn(new Coords(3, 3));
		Game g = mock(Game.class);
		Board b = mock(Board.class);
		when(b.getBuildingsVector()).thenReturn(null);

		List<Coords> warnList = ConstructionFactorWarning.findCFWarnings(g, e, b);

		assertNotNull(warnList);
	}
}