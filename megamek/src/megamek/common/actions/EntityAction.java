/*
 * MegaMek - Copyright (c) 2000-2002 Ben Mazur (bmazur@sev.org)
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
package megamek.common.actions;

import megamek.client.Client;
import megamek.common.*;
import megamek.client.ui.swing.AccessibilityWindow;
import megamek.client.ui.swing.tooltip.EntityActionLog;
import megamek.client.ui.swing.boardview.TurnDetailsOverlay;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.strategicBattleSystems.SBFFormation;

/**
 * This interface is implemented by all actions that game units - not restricted to Entity! - can
 * perform, such as attacks, charges or spotting. Basic movement is currently not represented by this
 * interface.
 * @see Entity
 * @see AlphaStrikeElement
 * @see SBFFormation
 */
public interface EntityAction {

    /**
     * @return The ID of the acting game unit. Note that when an entity is destroyed, it may no longer be
     * available from {@link Game#getEntity(int)} but rather only from {@link Game#getOutOfGameEntity(int)}
     * or {@link Game#getEntityFromAllSources(int)}.
     * As this can happen in the middle of resolving complicated situations in the GameManager, this
     * is a potential cause for bugs.
     * <BR>Note that this is not restricted to {@link Entity}; it can be used for all {@link InGameObject}s
     * that are handled in the game.
     */
    int getEntityId();

    /**
     * Returns a full description of the action that is (only) to be used in the
     * {@link AccessibilityWindow} as a textual representation of the action.
     * By default, this method returns the value of toString().
     *
     * @param client The local client to obtain any necessary information for the description
     * @return A string describing the action
     * @see AccessibilityWindow
     */
    default String toAccessibilityDescription(Client client) {
        return toString();
    }

    /**
     * Returns a short one-line description of the action that is used in the UI, e.g. on attack arrows
     * in the BoardView and in the action summary in {@link TurnDetailsOverlay}.
     *
     * @param game The game object to get information from
     * @return A short String describing the action
     * @see EntityActionLog
     * @see TurnDetailsOverlay
     */
    default String toSummaryString(Game game) {
        String typeName = this.getClass().getTypeName();
        return typeName.substring(typeName.lastIndexOf('.') + 1);
    }
}
