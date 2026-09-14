/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.phaseDisplay;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.units.Entity;
import megamek.common.units.SmallCraft;

/**
 * Asks the player the questions needed to declare crane loading or unloading at a grounded Small Craft or DropShip (TW
 * p.90-91), so that {@link MovementDisplay} only has to add the resulting step.
 */
final class CraneCommandDialogs {

    /** Message keys for the six facings, in facing order starting at north. */
    private static final String[] FACING_KEYS = {
          "DeploymentDisplay.facingNorth",
          "DeploymentDisplay.facingNorthEast",
          "DeploymentDisplay.facingSouthEast",
          "DeploymentDisplay.facingSouth",
          "DeploymentDisplay.facingSouthWest",
          "DeploymentDisplay.facingNorthWest"
    };

    private CraneCommandDialogs() {}

    /**
     * Picks the carrier that should load a unit, asking only when more than one is in reach.
     *
     * @param frame    the parent window
     * @param unit     the unit to be loaded
     * @param carriers the carriers in reach
     *
     * @return the chosen carrier, or {@code null} if there is none or the player cancelled
     */
    static @Nullable SmallCraft chooseCarrier(JFrame frame, Entity unit, List<SmallCraft> carriers) {
        if (carriers.isEmpty()) {
            return null;
        }
        if (carriers.size() == 1) {
            return carriers.getFirst();
        }
        List<Entity> choices = new ArrayList<>(carriers);
        String input = (String) JOptionPane.showInputDialog(frame,
              Messages.getString("MovementDisplay.LoadByCraneDialog.message", unit.getShortName()),
              Messages.getString("MovementDisplay.LoadByCraneDialog.title"),
              JOptionPane.QUESTION_MESSAGE,
              null,
              SharedUtility.getDisplayArray(choices),
              null);
        return (SharedUtility.getTargetPicked(choices, input) instanceof SmallCraft carrier) ? carrier : null;
    }

    /**
     * Picks the carried unit the cranes should unload, asking only when there is more than one.
     *
     * @param frame   the parent window
     * @param carrier the carrier doing the unloading
     * @param units   the units the cranes could unload
     *
     * @return the chosen unit, or {@code null} if there is none or the player cancelled
     */
    static @Nullable Entity chooseUnit(JFrame frame, SmallCraft carrier, List<Entity> units) {
        if (units.isEmpty()) {
            return null;
        }
        if (units.size() == 1) {
            return units.getFirst();
        }
        String input = (String) JOptionPane.showInputDialog(frame,
              Messages.getString("MovementDisplay.UnloadByCraneDialog.message", carrier.getShortName()),
              Messages.getString("MovementDisplay.UnloadByCraneDialog.title"),
              JOptionPane.QUESTION_MESSAGE,
              null,
              SharedUtility.getDisplayArray(units),
              null);
        return (SharedUtility.getTargetPicked(units, input) instanceof Entity unit) ? unit : null;
    }

    /**
     * Picks the hex a unit is unloaded into.
     *
     * @param frame     the parent window
     * @param carrier   the carrier doing the unloading
     * @param positions the legal unloading hexes, not empty
     *
     * @return the chosen hex, or {@code null} if the player cancelled
     */
    static @Nullable Coords chooseUnloadHex(JFrame frame, SmallCraft carrier, List<Coords> positions) {
        String[] choices = new String[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            choices[i] = positions.get(i).getBoardNum();
        }
        String selected = (String) JOptionPane.showInputDialog(frame,
              Messages.getString("MovementDisplay.ChooseHex.message", carrier.getShortName(),
                    carrier.getUnusedString()),
              Messages.getString("MovementDisplay.ChooseHex.title"),
              JOptionPane.QUESTION_MESSAGE,
              null,
              choices,
              null);
        if (selected == null) {
            return null;
        }
        for (Coords position : positions) {
            if (selected.equals(position.getBoardNum())) {
                return position;
            }
        }
        return null;
    }

    /**
     * Picks the facing a unit is unloaded with.
     *
     * @param frame the parent window
     * @param unit  the unit being unloaded
     *
     * @return the chosen facing from 0 (north) to 5, or {@code null} if the player cancelled
     */
    static @Nullable Integer chooseFacing(JFrame frame, Entity unit) {
        String[] choices = new String[FACING_KEYS.length];
        for (int facing = 0; facing < FACING_KEYS.length; facing++) {
            choices[facing] = Messages.getString(FACING_KEYS[facing]);
        }
        String selected = (String) JOptionPane.showInputDialog(frame,
              Messages.getString("MovementDisplay.CraneFacingDialog.message", unit.getShortName()),
              Messages.getString("MovementDisplay.CraneFacingDialog.title"),
              JOptionPane.QUESTION_MESSAGE,
              null,
              choices,
              choices[0]);
        if (selected == null) {
            return null;
        }
        for (int facing = 0; facing < choices.length; facing++) {
            if (selected.equals(choices[facing])) {
                return facing;
            }
        }
        return null;
    }
}
