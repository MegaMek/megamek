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

import java.util.ArrayList;
import java.util.List;

/**
 * When deploying units that are towing units, let's mark hexes that would break the tow linkage
 * with warning. or something
 */
public class TowLinkWarning {

    /**
     * This is used by the {@link MovementDisplay} class.
     *
     *
     * @param game {@link Game} provided by the phase display class
     * @param entity {@link Entity} currently selected in the deployment phase.
     * @param board {@link Board} board object with hex data.
     *
     * @return returns a list of {@link Coords} that where warning flags
     *         should be placed.
     */
    public static List<Coords> findTowLinkIssues(Game game, Entity entity, Board board) {
        List<Coords> warnList = new ArrayList<>();

        List<Coords> validTractorCoords = findCoordsForTractor(game, entity, board);
        List<Coords> validTrailerCoords = findCoordsForTrailer(game, entity, board);


        // Null list means the unit is not a tractor/trailer - if both
        // are null then there are no warnings to display, return the
        // empty warn list.
        if (validTractorCoords == null && validTrailerCoords == null) {
            return warnList;
        }

        List<Coords> validTowCoords = new ArrayList<>();

        if (validTractorCoords == null) {
            // We've established both aren't null -
            // So if validTractorCoords is null, we can add
            // validTrailerCoords as the only valid coords
            validTowCoords.addAll(validTrailerCoords);
        } else if (validTrailerCoords == null) {
            // On the other hand, if validTrailerCoords is null,
            // we can add validTractorCoords as the only valid coords
            validTowCoords.addAll(validTractorCoords);
        } else {
            // If neither is null then we need to get the coords in common
            // So add one, then retainAll with the other.
            validTowCoords.addAll(validTractorCoords);
            validTowCoords.retainAll(validTrailerCoords);
        }

        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Coords coords = new Coords(x, y);
                // We still need to check if it's a legal deployment
                // again so we don't mark hexes that aren't even
                // valid to deploy in as warning.
                if (board.isLegalDeployment(coords, entity)) {
                    if (!validTowCoords.contains(coords)) {
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

        List<Coords> validCoords = new ArrayList<>();

        if (trailer.isDeployed()) {
            validCoords.addAll(getCoordsForTractorGivenTrailer(game, tractor, trailer));
        } else {
            for (int x = 0; x < board.getWidth(); x++) {
                for (int y = 0; y < board.getHeight(); y++) {
                    Coords coords = new Coords(x, y);
                    if (board.isLegalDeployment(coords, tractor)) {
                        if (trailer instanceof LargeSupportTank) {
                            // Can our trailer deploy in any of the adjacent hexes?
                            if (coords.allAdjacent().stream().anyMatch(c -> board.isLegalDeployment(c, trailer) && !trailer.isLocationProhibited(c))) {
                                validCoords.add(coords);
                            }
                        }
                        else {
                            if (board.isLegalDeployment(coords, trailer) && !trailer.isLocationProhibited(coords)) {
                                validCoords.add(coords);
                            }
                        }
                    }
                }
            }
        }

        return validCoords;
    }

    public static List<Coords> getCoordsForTractorGivenTrailer(Game game, Entity tractor, Entity trailer) {
        List<Coords> validCoords = new ArrayList<>();

        //Can they stack? If so, add the trailer's hex as valid
        if (Compute.stackingViolation(game, tractor.getId(), trailer.getPosition(), false) == null) {
            validCoords.add(trailer.getPosition());
        } else {
            // Let's add the typical adjacent hexes
            validCoords.addAll(trailer.getPosition().allAdjacent());

            //Except the one behind the trailer, a tractor can't be there!
            validCoords.remove(trailer.getPosition().translated(trailer.getFacing(), -1));
        }

        return validCoords;
    }

    /**
     * When deploying a tractor, return the coords that would let it attach to its assigned trailer.
     * @param game
     * @param trailer
     * @param board
     * @return List of coords that a trailer could go, empty if there are none. Null if the trailer isn't a trailer or being pulled
     */
    protected static List<Coords> findCoordsForTrailer(Game game, Entity trailer, Board board) {
        int tractorId = trailer.getTowedBy();
        Entity tractor = game.getEntity(tractorId);
        if (tractorId == Entity.NONE || tractor == null || tractor.getDeployRound() != trailer.getDeployRound()) {
            return null;
        }

        List<Coords> validCoords = new ArrayList<>();

        if (tractor.isDeployed()) {
            validCoords.addAll(getCoordsForTrailerGivenTractor(game, trailer, tractor));
        } else {
            for (int x = 0; x < board.getWidth(); x++) {
                for (int y = 0; y < board.getHeight(); y++) {
                    Coords coords = new Coords(x, y);
                    if (board.isLegalDeployment(coords, trailer)) {
                        if (trailer instanceof LargeSupportTank) {
                            // Can the tractor deploy in any of the adjacent hexes?
                            if (coords.allAdjacent().stream().anyMatch(c -> board.isLegalDeployment(c, tractor) && !tractor.isLocationProhibited(c))) {
                                validCoords.add(coords);
                            }
                        }
                        else {
                            if (board.isLegalDeployment(coords, tractor) && !tractor.isLocationProhibited(coords)) {
                                validCoords.add(coords);
                            }
                        }
                    }
                }
            }
        }

        return validCoords;
    }

    public static List<Coords> getCoordsForTrailerGivenTractor(Game game, Entity trailer, Entity tractor) {
        List<Coords> validCoords = new ArrayList<Coords>();
        //Can they stack? If so, add the tractor's hex as valid
        if (Compute.stackingViolation(game, trailer.getId(), tractor.getPosition(), false) == null) {
            validCoords.add(tractor.getPosition());
        } else {
            // Let's add all the adjacent hexes - even the spot in front of the tractor is valid, afaik it's
            // possible for the tractor to turn any orientation at the end of its movement.
            validCoords.addAll(tractor.getPosition().allAdjacent());
        }
        return validCoords;
    }
}
