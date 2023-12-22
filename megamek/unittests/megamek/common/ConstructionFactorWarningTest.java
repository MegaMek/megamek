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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import megamek.client.ui.swing.GUIPreferences;

public class ConstructionFactorWarningTest {

	@Test
	public void testDefaultPreferenceForCFWarningIndicator() {
		// The default setting for Construction Factor warning indicators should be true.
		GUIPreferences GUIP = GUIPreferences.getInstance();
		assertTrue(GUIP.getDefaultBoolean(GUIPreferences.CONSTRUCTOR_FACTOR_WARNING));
	}

	@Test
	public void testSetPreferenceForCFWarningIndicator() {
		// Set the preference for CF warning indicator to false and true.
		GUIPreferences GUIP = GUIPreferences.getInstance();

		// Set preference to false and test that is retrieved as false.
		GUIP.setShowCFWarnings(false);
		assertFalse(GUIP.getShowCFWarnings());

		// Set back to true and test again.
		GUIP.setShowCFWarnings(true);
		assertTrue(GUIP.getShowCFWarnings());
	}
}