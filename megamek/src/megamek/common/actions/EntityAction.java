/*
 * Copyright (c) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.actions;

import megamek.client.Client;
import megamek.client.ui.clientGUI.boardview.overlay.TurnDetailsOverlay;
import megamek.client.ui.clientGUI.tooltip.EntityActionLog;
import megamek.client.ui.dialogs.AccessibilityDialog;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.game.Game;
import megamek.common.game.InGameObject;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.units.Entity;

/**
 * This interface is implemented by all actions that game units - not restricted to Entity! - can perform, such as
 * attacks, charges or spotting. Basic movement is currently not represented by this interface.
 *
 * @see Entity
 * @see AlphaStrikeElement
 * @see SBFFormation
 */
public interface EntityAction {

    /**
     * @return The ID of the acting game unit. Note that when an entity is destroyed, it may no longer be available from
     *       {@link Game#getEntity(int)} but rather only from {@link Game#getOutOfGameEntity(int)} or
     *       {@link Game#getEntityFromAllSources(int)}. As this can happen in the middle of resolving complicated
     *       situations in the GameManager, this is a potential cause for bugs.
     *       <BR>Note that this is not restricted to {@link Entity}; it can be used for all {@link InGameObject}s
     *       that are handled in the game.
     */
    int getEntityId();

    /**
     * Returns a full description of the action that is (only) to be used in the {@link AccessibilityDialog} as a
     * textual representation of the action. By default, this method returns the value of toString().
     *
     * @param client The local client to obtain any necessary information for the description
     *
     * @return A string describing the action
     *
     * @see AccessibilityDialog
     */
    default String toAccessibilityDescription(Client client) {
        return toString();
    }

    /**
     * Returns a short one-line description of the action that is used in the UI, e.g. on attack arrows in the BoardView
     * and in the action summary in {@link TurnDetailsOverlay}.
     *
     * @param game The game object to get information from
     *
     * @return A short String describing the action
     *
     * @see EntityActionLog
     * @see TurnDetailsOverlay
     */
    default String toSummaryString(Game game) {
        String typeName = this.getClass().getTypeName();
        return typeName.substring(typeName.lastIndexOf('.') + 1);
    }
}
