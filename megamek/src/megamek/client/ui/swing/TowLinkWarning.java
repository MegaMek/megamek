/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.swing;

import megamek.common.*;
import megamek.logging.MMLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * When deploying units that are towing units, let's mark hexes that would break the tow linkage
 * with warning. or something
 */
public class TowLinkWarning {
    private final static MMLogger logger = MMLogger.create(TowLinkWarning.class);

    /**
     *
     * This is
     * used by the {@link MovementDisplay} class.
     *
     * @param game {@link Game} provided by the phase display class
     * @param entity {@link Entity} currently selected in the deployment phase.
     * @param board {@link Board} board object with building data.
     *
     * @return returns a list of {@link Coords} that where warning flags
     *         should be placed.
     */
    public static List<Coords> findTowLinkIssues(Game game, Entity entity, Board board) {
        List<Coords> warnList = new ArrayList<Coords>();

        List<Coords> validTractorCoords = findCoordsForTractor(game, entity, board);
        List<Coords> validTrailerCoords = findCoordsForTrailer(game, entity, board);

        //int tractorId = entity.getTowedBy();
        //Entity tractor = game.getEntity(tractorId);
        //if (tractorId != Entity.NONE && tractor != null) {
        //    validTrailerCoords = findCoordsForTrailer(game, tractor, board);
        //}

        if (validTractorCoords == null && validTrailerCoords == null) {
            return warnList;
        }
        var boardHeight = board.getHeight();
        var boardWidth = board.getWidth();
        for (int x = 0; x < boardWidth; x++) {
            for (int y = 0; y < boardHeight; y++) {
                Coords coords = new Coords(x, y);
                if (board.isLegalDeployment(coords, entity)) {
                    if (validTractorCoords != null && !validTractorCoords.contains(coords)) {
                        warnList.add(coords);
                    } else if (validTrailerCoords != null && !validTrailerCoords.contains(coords)) {
                        warnList.add(coords);
                    }
                }
            }
        }

        return warnList;
    }

    /**
     * When deploying a tractor, return the coords that would let it attach to its assigned trailer.
     * @param game
     * @param tractor
     * @param board
     * @return List of coords that a tractor could go, empty if there are none. Null if the tractor isn't a tractor or pulling anything
     */
    protected static List<Coords> findCoordsForTractor(Game game, Entity tractor, Board board) {
        int trailerId = tractor.getTowing();
        Entity trailer = game.getEntity(trailerId);
        if (trailerId == Entity.NONE || trailer == null || trailer.getDeployRound() != tractor.getDeployRound()) {
            return null;
        }

        List<Coords> validCoords = new ArrayList<Coords>();

        if (trailer.isDeployed()) {
            //Can they stack? If so, add the trailer's hex as valid
            if (Compute.stackingViolation(game, tractor.getId(), tractor.getPosition(), false) == null) {
                validCoords.add(trailer.getPosition());
            }
            validCoords.add(trailer.getPosition().translated(trailer.getFacing(), 1));
            // Let's add the typical adjacent hexes
            validCoords.addAll(trailer.getPosition().allAdjacent());

            //Except the one behind the trailer, a tractor can't be there!
            validCoords.remove(trailer.getPosition().translated(trailer.getFacing(), -1));

        } else {
            var boardHeight = board.getHeight();
            var boardWidth = board.getWidth();
            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight; y++) {
                    Coords coords = new Coords(x, y);
                    if (board.isLegalDeployment(coords, tractor)) {
                        int facing = tractor.getFacing();
                        // Can our trailer deploy in any of the adjacent hexes?
                        if (coords.allAdjacent().stream().anyMatch(c -> board.isLegalDeployment(c, trailer) && !trailer.isLocationProhibited(c))) {
                            validCoords.add(coords);
                        }
                    }
                }
            }
        }

        return validCoords;
    }

    /**
     * When deploying a tractor, return the coords that would let it attach to its assigned trailer.
     * @param game
     * @param trailer
     * @param board
     * @return List of coords that a tractor could go, empty if there are none. Null if the tractor isn't a tractor or pulling anything
     */
    protected static List<Coords> findCoordsForTrailer(Game game, Entity trailer, Board board) {
        int tractorId = trailer.getTractor();
        Entity tractor = game.getEntity(tractorId);
        if (tractorId == Entity.NONE || tractor == null || tractor.getDeployRound() != trailer.getDeployRound()) {
            return null;
        }

        List<Coords> validCoords = new ArrayList<Coords>();

        if (tractor.isDeployed()) {
            //Can they stack? If so, add the trailer's hex as valid
            if (Compute.stackingViolation(game, trailer.getId(), trailer.getPosition(), false) == null) {
                validCoords.add(tractor.getPosition());
            }
            // Let's add the hex behind us
            validCoords.add(tractor.getPosition().translated(tractor.getFacing(), -1));
            //validCoords.addAll(tractor.getPosition().allAdjacent());

        } else {
            var boardHeight = board.getHeight();
            var boardWidth = board.getWidth();
            for (int x = 0; x < boardWidth; x++) {
                for (int y = 0; y < boardHeight; y++) {
                    Coords coords = new Coords(x, y);
                    if (board.isLegalDeployment(coords, trailer)) {
                        // Can our trailer deploy in any of the adjacent hexes?
                        if (coords.allAdjacent().stream().anyMatch(c -> board.isLegalDeployment(c, tractor) && !tractor.isLocationProhibited(c))) {
                            validCoords.add(coords);
                        }
                    }
                }
            }
        }

        return validCoords;
    }

    private HashSet<Coords> getAllCoords(Board board) {
        var boardHeight = board.getHeight();
        var boardWidth = board.getWidth();
        var coordsSet = new HashSet<Coords>();
        for (int x = 0; x < boardWidth; x++) {
            for (int y = 0; y < boardHeight; y++) {
                coordsSet.add(new Coords(x, y));
            }
        }
        return coordsSet;
    }
}
