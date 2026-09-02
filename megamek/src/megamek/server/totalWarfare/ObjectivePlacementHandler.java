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
package megamek.server.totalWarfare;

import java.util.List;
import java.util.Map;

import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.logging.MMLogger;

/**
 * Places the objective markers that players designated in the lobby onto the board when the game starts. A marker
 * designated in the lobby carries its board position (see {@link ObjectiveMarker#getLobbyPosition()}) and rides its
 * owner's ground-objects-to-place list to the server; at the start of the game this handler turns each one into a
 * placed ground object. Markers whose position is off-board or already occupied by another objective are left in the
 * to-place list (with a logged warning), so their owner can still place them by hand during the Deploy Minefields
 * phase like other carryable objects.
 */
class ObjectivePlacementHandler extends AbstractTWRuleHandler {

    /** Feature logger for the victory hex designation diagnostics; enabled via the log4j2.xml VictoryHex block. */
    private static final MMLogger VICTORY_HEX_LOGGER = MMLogger.create("megamek.feature.VictoryHex");

    ObjectivePlacementHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Places every objective marker that was given a board position in the lobby, then broadcasts the updated ground
     * objects to all clients. Called once when the game starts (the EXCHANGE phase), when the real game board exists.
     */
    void placeLobbyObjectives() {
        Board board = getGame().getBoard();
        boolean anyPlaced = false;
        for (Player player : getGame().getPlayersList()) {
            List<ICarryable> groundObjectsToPlace = player.getGroundObjectsToPlace();
            for (ICarryable groundObject : List.copyOf(groundObjectsToPlace)) {
                if (!(groundObject instanceof ObjectiveMarker marker) || (marker.getLobbyPosition() == null)) {
                    continue;
                }
                Coords position = marker.getLobbyPosition();
                if ((board == null) || !board.contains(position)) {
                    VICTORY_HEX_LOGGER.warn("[Objective] {} of {} has the off-board lobby position {} - not placed, it can "
                          + "be placed during the Deploy Minefields phase", marker.generalName(), player, position);
                    continue;
                }
                if (findOtherObjectiveAt(position, marker) != null) {
                    VICTORY_HEX_LOGGER.warn("[Objective] {} of {} cannot be placed at {} - only one objective can be in a "
                          + "single hex; it can be placed during the Deploy Minefields phase",
                          marker.generalName(), player, position);
                    continue;
                }
                groundObjectsToPlace.remove(marker);
                marker.setLobbyPosition(null);
                getGame().placeGroundObject(position, marker);
                anyPlaced = true;
                VICTORY_HEX_LOGGER.info(
                      "[Objective] Placed lobby objective {} (owner ID {}, radius {}, {} scheme) at {}",
                      marker.generalName(), marker.getOwnerId(), marker.getControlRadius(),
                      marker.getScoringScheme().getPreset(), position);
            }
        }
        if (anyPlaced) {
            gameManager.sendGroundObjectUpdate();
        }
    }

    /**
     * Returns every objective marker on the board to its owner's ground-objects-to-place list, restoring the board
     * position as the marker's lobby position. Called when the game is reset back to the lobby, before the game
     * (and with it the ground object map) is cleared - without this, returning to the lobby would silently lose all
     * designated victory hexes. The reset then sends player updates to every client, so the restored designations
     * show up in everyone's lobby board preview again. A marker whose owner no longer exists is dropped with a
     * logged warning.
     */
    void returnObjectivesToLobby() {
        int returnedCount = 0;
        for (Map.Entry<Coords, List<ICarryable>> hexObjects : getGame().getGroundObjects().entrySet()) {
            for (ICarryable groundObject : hexObjects.getValue()) {
                if (!(groundObject instanceof ObjectiveMarker marker)) {
                    continue;
                }
                Player owner = getGame().getPlayer(marker.getOwnerId());
                if (owner == null) {
                    VICTORY_HEX_LOGGER.warn("[Objective] {} at {} has no owner (player ID {}) - dropped on the reset "
                          + "to the lobby", marker.generalName(), hexObjects.getKey(), marker.getOwnerId());
                    continue;
                }
                marker.setLobbyPosition(hexObjects.getKey());
                // a fresh game starts with fresh counters - the setup values on the scheme remain - and
                // with nobody holding the point. The controller lives on the marker, not the scheme, and
                // it was being carried across the reset: a lobby save then preserved it, and the next
                // game began with the zone already "held" before anyone had stood in it
                marker.getScoringScheme().resetState();
                marker.setController(ObjectiveMarker.NO_CONTROLLER, ObjectiveMarker.NO_CONTROLLER);
                owner.getGroundObjectsToPlace().add(marker);
                returnedCount++;
                VICTORY_HEX_LOGGER.debug("[Objective] Returned {} at {} to the lobby designations of {}",
                      marker.generalName(), hexObjects.getKey(), owner);
            }
        }
        if (returnedCount > 0) {
            VICTORY_HEX_LOGGER.info("[Objective] Returned {} objective marker(s) to the lobby on the game reset",
                  returnedCount);
        }
    }

    /**
     * @param position       the hex to check
     * @param excludedMarker the marker being placed, excluded from the check
     *
     * @return another objective marker already in the given hex, or {@code null} when the hex holds none
     */
    @Nullable
    private ObjectiveMarker findOtherObjectiveAt(Coords position, ObjectiveMarker excludedMarker) {
        for (ICarryable groundObject : getGame().getGroundObjects(position)) {
            if ((groundObject instanceof ObjectiveMarker marker) && (marker != excludedMarker)) {
                return marker;
            }
        }
        return null;
    }
}
