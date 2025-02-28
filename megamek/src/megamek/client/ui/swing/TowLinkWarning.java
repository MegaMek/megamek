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
import java.util.Set;
import java.util.HashSet;

/**
 * When deploying units that are towing units, let's mark hexes that would break the tow linkage
 * with warning. or something
 */
public class TowLinkWarning {

    /**
     * We should stop searching for a tractor or trailer at some point.
     * If a search reaches 10000 let's stop.
      */
    private static final int MAX_SEARCH_DEPTH = 10000;

    /**
     * This is used by the {@link MovementDisplay} class.
     *
     *
     * @param game {@link Game} provided by the phase display class
     * @param entity {@link Entity} currently selected in the deployment phase.
     * @param board {@link Board} board object with hex data.
     * @throws StackOverflowError if there is a loop in the towing configuration, or it is longer than MAX_SEARCH_DEPTH
     *
     * @return returns a list of {@link Coords} that where warning flags
     *         should be placed.
     */
    public static List<Coords> findTowLinkIssues(Game game, Entity entity, Board board) throws StackOverflowError {
        List<Coords> warnList = new ArrayList<>();

        if (entity.getTowing() == Entity.NONE && entity.getTowedBy() == Entity.NONE) {
            return warnList;
        }

        List<Coords> validTowCoords = findValidDeployCoordsForTractorTrailer(game, entity, board);

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
     * For the provided entity, find its associated tractor and trailer and
     * find what coords this unit is able to deploy to. Will return an empty
     * list for units that aren't tractors/trailers, so don't sue them for them!
     * @param game {@link Game} provided by the phase display class
     * @param deployingEntity {@link Entity} currently selected in the deployment phase.
     * @param board {@link Board} board object with hex data.
     * @return a list of {@link Coords} that this unit could deploy into that would let it connect with its
     *          tow connections. Empty if there are no valid locations or if it's not towing.
     * @throws StackOverflowError if there is a loop in the towing configuration, or it is longer than MAX_SEARCH_DEPTH
     */
    public static List<Coords> findValidDeployCoordsForTractorTrailer(Game game, Entity deployingEntity, Board board) throws StackOverflowError {
        List<Coords> validCoords = new ArrayList<>();
        Entity towingEnt = deployingEntity;
        Entity closestDeployedTractor = null;
        Entity closestDeployedTrailer = null;
        boolean useTractorCoords = false;
        boolean useTrailerCoords = false;
        boolean useAdjacentHexForTractor = (towingEnt instanceof LargeSupportTank);
        if (deployingEntity.getTowedBy() != Entity.NONE && game.hasEntity(deployingEntity.getTowedBy())) {
            towingEnt = game.getEntity(deployingEntity.getTowedBy());
            if (towingEnt != null) {
                // Intentionally different - we want to use the last useAdjacentHex
                // value unless it's a large support tank
                useAdjacentHexForTractor = useAdjacentHexForTractor || towingEnt instanceof LargeSupportTank;
                if (towingEnt.isDeployed()) {
                    closestDeployedTractor = towingEnt;
                }
                int count = 0;
                while ((towingEnt != null) && (towingEnt.getTowedBy() != Entity.NONE) && count < MAX_SEARCH_DEPTH) {
                    towingEnt = game.getEntity(towingEnt.getTowedBy());
                    useAdjacentHexForTractor = !useAdjacentHexForTractor || towingEnt instanceof LargeSupportTank;
                    if ((closestDeployedTractor == null) && (towingEnt != null) && (towingEnt.isDeployed())) {
                        closestDeployedTractor = towingEnt;
                    }
                    count++;
                }
                if (count >= MAX_SEARCH_DEPTH) {
                    throw new StackOverflowError("Towing length too deep, too many tractors");
                }
            }
        }

        // Did we find a tractor?
        Set<Coords> validTractorCoords = new HashSet<>();;
        boolean useAdjacentHex = useAdjacentHexForTractor;
        if (closestDeployedTractor != null) {
            Set<Coords> possibleCoords = new HashSet<>();
            possibleCoords.add(closestDeployedTractor.getPosition());
            Entity testEntity = game.getEntity(closestDeployedTractor.getTowing());
            int count = 0;
            do{
                possibleCoords = getValidDeploymentCoords(testEntity, board, useAdjacentHex, possibleCoords);
                testEntity = game.getEntity(testEntity.getTowing());
                useAdjacentHex = !useAdjacentHex || testEntity instanceof LargeSupportTank;
                count++;
            }
            while((testEntity != null) && (!testEntity.equals(deployingEntity)) && (count < MAX_SEARCH_DEPTH));
            if (count >= MAX_SEARCH_DEPTH) {
                throw new StackOverflowError("Towing length too deep, could not path back to trailer");
            }

            validTractorCoords = possibleCoords;
            useTractorCoords = true;
        } else {
            if (validAttachedTractor(deployingEntity, game)) {
                List<Coords> list = findCoordsForTrailer(game, deployingEntity, board);
                validTractorCoords = new HashSet<>(list);
                useTractorCoords = true;
            }
        }

        // Now let's do the trailers.
        if (deployingEntity.getTowing() != Entity.NONE && game.hasEntity(deployingEntity.getTowing())) {
            towingEnt = game.getEntity(deployingEntity.getTowing());
            if (towingEnt != null) {
                if (towingEnt.isDeployed()) {
                    closestDeployedTrailer = towingEnt;
                } else {
                    int count = 0;
                    while ((towingEnt != null) && (towingEnt.getTowing() != Entity.NONE) && (count < MAX_SEARCH_DEPTH))  {
                        towingEnt = game.getEntity(towingEnt.getTowing());
                        if ((towingEnt != null) && (closestDeployedTrailer == null) && (towingEnt.isDeployed())) {
                            closestDeployedTrailer = towingEnt;
                            break;
                        }
                        count++;
                    }
                    if (count >= MAX_SEARCH_DEPTH) {
                        throw new StackOverflowError("Towing length too deep, too many trailers");
                    }
                }
            }
        }

        Set<Coords> validTrailerCoords = new HashSet<>();;
        // Did we find a trailer?
        if (closestDeployedTrailer != null) {
            Set<Coords> possibleCoords = new HashSet<>();
            possibleCoords.add(closestDeployedTrailer.getPosition());
            // Intentionally different - we want to use the last useAdjacentHex
            // value unless it's a large support tank
            useAdjacentHex = useAdjacentHex || closestDeployedTrailer instanceof LargeSupportTank;
            Entity testEntity = game.getEntity(closestDeployedTrailer.getTowedBy());
            int count = 0;
            do {
                possibleCoords = getValidDeploymentCoords(testEntity, board, useAdjacentHex, possibleCoords);
                useAdjacentHex = !useAdjacentHex || testEntity instanceof LargeSupportTank;
                testEntity = game.getEntity(testEntity.getTowedBy());
                count++;
            } while ((testEntity != null) && (deployingEntity.getTowedBy() != testEntity.getId()) && (count < MAX_SEARCH_DEPTH));
            if (count >= MAX_SEARCH_DEPTH) {
                throw new StackOverflowError("Towing length too deep, could not path back to tractor");
            }
            useTrailerCoords = true;
            validTrailerCoords = possibleCoords;
        } else {
            if (validAttachedTrailer(deployingEntity, game)) {
                List<Coords> list = findCoordsForTractor(game, deployingEntity, board);
                validTrailerCoords = new HashSet<>(list);
                useTrailerCoords = true;
            }
        }

        // If there's valid tractor and/or trailer coords,
        // let's get them and intersect them if needed.
        if (useTrailerCoords && useTractorCoords) {
            validCoords.addAll(validTractorCoords);
            validCoords.retainAll(validTrailerCoords);
        } else if (useTrailerCoords) {
            validCoords.addAll(validTrailerCoords);
        } else if (useTractorCoords) {
            validCoords.addAll(validTractorCoords);
        }

        return validCoords;
    }

    private static Set<Coords> getValidDeploymentCoords(Entity deployingEntity, Board board, boolean useAdjacentHex, Set<Coords> possibleCoords) {
        Set<Coords> validCoords = new HashSet<>();
        for (Coords coords : possibleCoords) {
            if (useAdjacentHex) {
                for (Coords adjCoords : coords.allAdjacent()) {
                    if (!validCoords.contains(adjCoords) && board.isLegalDeployment(adjCoords, deployingEntity) && !deployingEntity.isLocationProhibited(adjCoords)) {
                        validCoords.add(adjCoords);
                    }
                }
            }
            else {
                if (board.isLegalDeployment(coords, deployingEntity) && !deployingEntity.isLocationProhibited(coords)) {
                    validCoords.add(coords);
                }
            }
        }

        return validCoords;
    }

    /**
     * When deploying a tractor, return the coords that would let it attach to its assigned trailer.
     * @param game
     * @param tractor
     * @param board
     * @return List of coords that a tractor could go, empty if there are none or if the tractor isn't a tractor or pulling anything
     */
    private static List<Coords> findCoordsForTractor(Game game, Entity tractor, Board board) {
        List<Coords> validCoords = new ArrayList<>();

        int attachedTrailerId = tractor.getTowing();
        Entity attachedTrailer = game.getEntity(attachedTrailerId);
        if (attachedTrailerId == Entity.NONE || attachedTrailer == null || attachedTrailer.getDeployRound() != tractor.getDeployRound()) {
            return validCoords;
        }

        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Coords coords = new Coords(x, y);
                if (board.isLegalDeployment(coords, tractor)) {
                    if (attachedTrailer instanceof LargeSupportTank) {
                        // Can our trailer deploy in any of the adjacent hexes?
                        if (coords.allAdjacent().stream().anyMatch(c -> board.isLegalDeployment(c, attachedTrailer) && !attachedTrailer.isLocationProhibited(c))) {
                            validCoords.add(coords);
                        }
                    }
                    else {
                        if (board.isLegalDeployment(coords, attachedTrailer) && !attachedTrailer.isLocationProhibited(coords)) {
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
     * @return List of coords that a trailer could go, empty if there are none or if the trailer isn't a trailer or being pulled
     */
    private static List<Coords> findCoordsForTrailer(Game game, Entity trailer, Board board) {
        List<Coords> validCoords = new ArrayList<>();
        int tractorId = trailer.getTowedBy();
        Entity tractor = game.getEntity(tractorId);
        if (tractorId == Entity.NONE || tractor == null || tractor.getDeployRound() != trailer.getDeployRound()) {
            return validCoords;
        }

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

        return validCoords;
    }

    private static boolean validAttachedTractor(Entity trailer, Game game) {
        int tractorId = trailer.getTowedBy();
        Entity tractor = game.getEntity(tractorId);
        return !((tractorId == Entity.NONE)
                || (tractor == null)
                || (tractor.getDeployRound() != trailer.getDeployRound()));
    }

    private static boolean validAttachedTrailer(Entity tractor, Game game) {
        int attachedTrailerId = tractor.getTowing();

        Entity attachedTrailer = game.getEntity(attachedTrailerId);
        return !((attachedTrailerId == Entity.NONE)
                || (attachedTrailer == null)
                || (attachedTrailer.getDeployRound() != tractor.getDeployRound()));
    }
}
