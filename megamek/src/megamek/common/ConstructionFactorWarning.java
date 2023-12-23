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

import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.common.enums.GamePhase;

/**
 *  Construction Factor Warning Logic.  Handles events, help
 *  methods and logic related to CF Warning in a way that
 *  can be unit tested and encapsulated from BoardView and
 *  ClientGUI and other actors.
 */
public class ConstructionFactorWarning {
	/*
	 *  Handler for ClientGUI actionPerformed event. Encapsulates
	 *  as much Construction Factory Warning logic possible.
	 */
	public static void handleActionPerformed(Game g, BoardView bv) {
		toggleCFWarning();
	}

	/*
	 *  Return true if the passed in phase is a phase that should allow
	 *  Construction Factor Warnings such as Deploy and Movement.
	 */
	public static boolean isCFWarningPhase(GamePhase gp) {
		return (gp == GamePhase.DEPLOYMENT || gp == GamePhase.MOVEMENT);
	}

	public static boolean shouldShow(GamePhase gp) {
		return shouldShow(gp, true);
	}

	public static boolean shouldShow(GamePhase gp, boolean isEnabled) {
		return (isEnabled && isCFWarningPhase(gp));
	}

	private static boolean toggleCFWarning() {
		//Toggle the GUI Preference setting for CF Warning setting.
		GUIPreferences GUIP = GUIPreferences.getInstance();
		GUIP.setShowCFWarnings(!GUIP.getShowCFWarnings());
		return (GUIP.getShowCFWarnings());
	}

	public static List<Coords> findCFWarnings(Game g, Entity e, Board b) {
		List<Coords> warnList = new ArrayList<Coords>();

		// test - put logic here that actually finds the hexes to put the warning based on the
		// selected entity and surrounding building / bridges.
		warnList.add(new Coords(10, 10));
		warnList.add(new Coords(12, 12));

		return warnList;
	}
}