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
package megamek.common.moves;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.units.CraneRules;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;

/**
 * Lets a player board a transport by clicking on it. A unit boards a transport with a MOUNT step taken from a hex
 * next to it, but the natural thing for a player to click is the transport itself, which plots a path that walks into
 * the transport's hexes and can never be legal. This helper cuts such a path back to the last hex outside the
 * transport, so the Mount button can then board it from there.
 */
public final class MountPathHelper {

    private MountPathHelper() {}

    /**
     * Why a unit may not mount a grounded Small Craft or DropShip under its own power at this point in its move.
     */
    public enum MountRestriction {
        /** The unit may mount. */
        NONE,
        /** VTOLs, fighters and Small Craft cannot mount under their own power; cranes load them (TW p.90). */
        CRANE_ONLY,
        /** The unit jumped this turn; entering a transport takes Walking/Cruising MP (TW p.90). */
        JUMPED,
        /** Infantry must spend all their MP to mount, so they cannot move first (TW p.223, errata v11.01). */
        INFANTRY_ALREADY_MOVED,
        /** The MP already spent plus the mounting cost would exceed Walking/Cruising MP (TW p.90). */
        NOT_ENOUGH_WALKING_MP
    }

    /**
     * Checks whether a unit may mount a grounded Small Craft or DropShip after the movement it has already made this
     * turn. Non-infantry pay half their Walking/Cruising MP (round cost up) on top of what they have spent, and may not
     * run; a unit that has not spent any MP may still mount through the Minimum Movement rule (TW p.90 and p.49).
     * Infantry mount as though the carrier were a Large Support Vehicle and must spend all their MP doing so (TW p.89 and
     * p.223, errata v11.01).
     *
     * @param mountingUnit         the unit that wants to mount
     * @param walkingMp            the unit's current Walking/Cruising MP
     * @param mpUsedBeforeMounting the MP the unit has spent this turn before mounting
     * @param isJumping            {@code true} if the unit jumped this turn
     *
     * @return {@link MountRestriction#NONE} if the unit may mount, otherwise the reason it may not
     */
    public static MountRestriction mountRestriction(Entity mountingUnit, int walkingMp, int mpUsedBeforeMounting,
          boolean isJumping) {
        if (CraneRules.isCraneOnlyUnit(mountingUnit)) {
            return MountRestriction.CRANE_ONLY;
        }
        if (isJumping) {
            return MountRestriction.JUMPED;
        }
        if (mountingUnit.isInfantry()) {
            return (mpUsedBeforeMounting > 0) ? MountRestriction.INFANTRY_ALREADY_MOVED : MountRestriction.NONE;
        }
        if (mpUsedBeforeMounting == 0) {
            // Minimum Movement: a unit that has not moved may always enter the adjacent transport
            return MountRestriction.NONE;
        }
        int mpNeeded = mpUsedBeforeMounting + mountOrDismountMpCost(walkingMp);
        return (mpNeeded > walkingMp) ? MountRestriction.NOT_ENOUGH_WALKING_MP : MountRestriction.NONE;
    }

    /**
     * Returns the MP a unit spends to mount or dismount a grounded Small Craft or DropShip under its own power: half
     * its Walking MP, with the cost rounded up (TW p.90 for mounting, TW p.91 for dismounting, errata v11.01).
     *
     * @param walkingMp the unit's Walking MP
     *
     * @return the MP cost to mount or dismount
     */
    public static int mountOrDismountMpCost(int walkingMp) {
        return (int) Math.ceil(walkingMp / 2.0);
    }

    /**
     * Returns the facing a dismounted unit is placed with. A unit dismounting a Small Craft or DropShip chooses its
     * facing (TW p.91); when no valid choice was made, it faces away from the carrier, as before.
     *
     * @param carrierPosition the carrier's hex
     * @param unloadPosition  the hex the unit is placed in
     * @param chosenFacing    the facing the player chose, from 0 (north) to 5, or {@code null} if none was chosen
     *
     * @return the facing to place the unit with
     */
    public static int dismountFacing(Coords carrierPosition, Coords unloadPosition, @Nullable Integer chosenFacing) {
        if ((chosenFacing != null) && (chosenFacing >= 0) && (chosenFacing <= 5)) {
            return chosenFacing;
        }
        return carrierPosition.direction(unloadPosition);
    }

    /**
     * If the path ends inside a transport standing in the clicked hex, and the moving unit could mount that transport
     * from the last hex before it, removes the steps inside the transport.
     *
     * @param movePath   the plotted path; trimmed in place when a mountable transport is found
     * @param clickedHex the hex the player clicked or dragged to
     * @param boardId    the board of the clicked hex
     * @param game       the game
     *
     * @return the transport the path now ends next to, or {@code null} if the path was left unchanged
     */
    public static @Nullable Entity trimToMountableTransport(MovePath movePath, Coords clickedHex, int boardId,
          Game game) {
        Entity movingUnit = movePath.getEntity();
        if ((movingUnit == null) || (clickedHex == null) || !game.hasBoardLocation(clickedHex, boardId)) {
            return null;
        }

        Set<Coords> transportHexes = new HashSet<>();
        for (Entity unitInHex : game.getEntitiesVector(clickedHex, boardId)) {
            if (!unitInHex.equals(movingUnit)) {
                transportHexes.addAll(unitInHex.getOccupiedCoords());
            }
        }
        // Only a path that cannot legally end where it does is trimmed; infantry may stand in a DropShip's hex, and a
        // vehicle may share a hex with a trailer, so those moves are left alone. Checking the movement type as the last
        // step covers both an illegal end position (stacking) and a step that is illegal in itself.
        MoveStep lastStep = movePath.getLastStep();
        if (!transportHexes.contains(movePath.getFinalCoords()) || (lastStep == null)
              || (lastStep.getMovementType(true) != EntityMovementType.MOVE_ILLEGAL)) {
            return null;
        }

        MovePath trimmedPath = movePath.clone();
        while ((trimmedPath.length() > 0) && transportHexes.contains(trimmedPath.getFinalCoords())) {
            trimmedPath.removeLastStep();
        }
        Coords mountFromHex = trimmedPath.getFinalCoords();
        if (transportHexes.contains(mountFromHex)) {
            return null;
        }

        int mountFromLevel = trimmedPath.getFinalElevation()
              + game.getBoard(boardId).getHex(mountFromHex).getLevel();
        List<Entity> mountableUnits = Compute.getMountableUnits(movingUnit, mountFromHex, boardId, mountFromLevel,
              game);
        Entity clickedTransport = null;
        for (Entity mountableUnit : mountableUnits) {
            if (mountableUnit.getOccupiedCoords().contains(clickedHex)) {
                clickedTransport = mountableUnit;
                break;
            }
        }
        if (clickedTransport == null) {
            return null;
        }

        while ((movePath.length() > 0) && transportHexes.contains(movePath.getFinalCoords())) {
            movePath.removeLastStep();
        }
        return clickedTransport;
    }
}
