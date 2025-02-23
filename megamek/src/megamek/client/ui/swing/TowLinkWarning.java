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
import megamek.common.annotations.Nullable;

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

        List<Coords> validTowCoords = findValidDeployCoordsForTractorTrailer(game, entity, board);

        if (validTowCoords == null) {
            return warnList;
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
     * For the provided entity, find its associated tractor and trailer and
     * find what coords this unit is able to deploy to. If a tractor or trailer
     * is already deployed
     * @param game {@link Game} provided by the phase display class
     * @param deployingEntity {@link Entity} currently selected in the deployment phase.
     * @param board {@link Board} board object with hex data.
     * @return a list of {@link Coords} that this unit could deploy into that would let it connect with its
     *          tow connections. Empty if there are no valid locations. Null if it's not towing.
     */
    public static @Nullable List<Coords> findValidDeployCoordsForTractorTrailer(Game game, Entity deployingEntity, Board board) {
        List<Coords> validCoords = new ArrayList<>();
        Entity towingEnt = deployingEntity;
        Entity closestDeployedTractor = null;
        Entity closestDeployedTrailer = null;
        boolean useAdjacentHexForTractor = (towingEnt instanceof LargeSupportTank);
        if (deployingEntity.getTowedBy() != Entity.NONE && game.hasEntity(deployingEntity.getTowedBy())) {
            towingEnt = game.getEntity(deployingEntity.getTowedBy());
            if (towingEnt != null) {
                useAdjacentHexForTractor = !useAdjacentHexForTractor || towingEnt instanceof LargeSupportTank;
                if (towingEnt.isDeployed()) {
                    closestDeployedTractor = towingEnt;
                }
                while (towingEnt != null && towingEnt.getTowedBy() != Entity.NONE) {
                    towingEnt = game.getEntity(towingEnt.getTowedBy());
                    useAdjacentHexForTractor = !useAdjacentHexForTractor || towingEnt instanceof LargeSupportTank;
                    if (towingEnt != null) {
                        if (closestDeployedTractor == null) {
                            if (towingEnt.isDeployed()){
                                closestDeployedTractor = towingEnt;
                            }
                        }
                    }
                }
            }
        }

        // Did we find a tractor?
        Set<Coords> validTractorCoords = null;
        boolean useAdjacentHex = useAdjacentHexForTractor;
        if (closestDeployedTractor != null) {
            Set<Coords> possibleCoords = new HashSet<>();
            possibleCoords.add(closestDeployedTractor.getPosition());
            Entity testEntity = game.getEntity(closestDeployedTractor.getTowing());;
            do{
                possibleCoords = getValidDeploymentCoords(testEntity, board, useAdjacentHex, possibleCoords);
                testEntity = game.getEntity(testEntity.getTowing());
                useAdjacentHex = !useAdjacentHex || testEntity instanceof LargeSupportTank;
            }
            while(testEntity != null && !testEntity.equals(deployingEntity));

            validTractorCoords = possibleCoords;
        } else {
            List<Coords> list = findCoordsForTrailer(game, deployingEntity, board);
            if (list != null) {
                validTractorCoords = new HashSet<>(list);
            }
        }

        // Now let's do the trailers.
        if (deployingEntity.getTowing() != Entity.NONE && game.hasEntity(deployingEntity.getTowing())) {
            towingEnt = game.getEntity(deployingEntity.getTowing());
            if (towingEnt != null) {
                if (towingEnt.isDeployed()) {
                    closestDeployedTrailer = towingEnt;
                } else {
                    while (towingEnt != null && towingEnt.getTowing() != Entity.NONE) {
                        towingEnt = game.getEntity(towingEnt.getTowing());
                        if (towingEnt != null) {
                            if (closestDeployedTrailer == null) {
                                if (towingEnt.isDeployed()){
                                    closestDeployedTrailer = towingEnt;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        Set<Coords> validTrailerCoords = null;
        // Did we find a trailer?
        if (closestDeployedTrailer != null) {
            Set<Coords> possibleCoords = new HashSet<>();
            possibleCoords.add(closestDeployedTrailer.getPosition());
            // Intentionally different - we want to use the last useAdjacentHex
            // value unless it's a large support tank
            useAdjacentHex = useAdjacentHex || closestDeployedTrailer instanceof LargeSupportTank;
            Entity testEntity = game.getEntity(closestDeployedTrailer.getTowedBy());
            do  {
                possibleCoords = getValidDeploymentCoords(testEntity, board, useAdjacentHex, possibleCoords);
                useAdjacentHex = !useAdjacentHex || testEntity instanceof LargeSupportTank;
                testEntity = game.getEntity(testEntity.getTowedBy());
            } while ((testEntity != null) && (deployingEntity.getTowedBy() != testEntity.getId()));

            validTrailerCoords = possibleCoords;
        } else {
            List<Coords> list = findCoordsForTractor(game, deployingEntity, board);
            if (list != null) {
                validTrailerCoords = new HashSet<>(list);
            }
        }

        if (validTractorCoords == null && validTrailerCoords == null) {
            return null;
        }

        if (validTractorCoords == null) {
            // We've established both aren't null -
            // So if validTractorCoords is null, we can add
            // validTrailerCoords as the only valid coords
            validCoords.addAll(validTrailerCoords);
        } else if (validTrailerCoords == null) {
            // On the other hand, if validTrailerCoords is null,
            // we can add validTractorCoords as the only valid coords
            validCoords.addAll(validTractorCoords);
        } else {
            // If neither is null then we need to get the coords in common
            // So add one, then retainAll with the other.
            validCoords.addAll(validTractorCoords);
            validCoords.retainAll(validTrailerCoords);
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
     * @return List of coords that a tractor could go, empty if there are none. Null if the tractor isn't a tractor or pulling anything
     */
    private static @Nullable List<Coords> findCoordsForTractor(Game game, Entity tractor, Board board) {
        int attachedTrailerId = tractor.getTowing();
        Entity attachedTrailer = game.getEntity(attachedTrailerId);
        if (attachedTrailerId == Entity.NONE || attachedTrailer == null || attachedTrailer.getDeployRound() != tractor.getDeployRound()) {
            return null;
        }

        List<Coords> validCoords = new ArrayList<>();

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
     * @return List of coords that a trailer could go, empty if there are none. Null if the trailer isn't a trailer or being pulled
     */
    private static @Nullable List<Coords> findCoordsForTrailer(Game game, Entity trailer, Board board) {
        int tractorId = trailer.getTowedBy();
        Entity tractor = game.getEntity(tractorId);
        if (tractorId == Entity.NONE || tractor == null || tractor.getDeployRound() != trailer.getDeployRound()) {
            return null;
        }

        List<Coords> validCoords = new ArrayList<>();

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

}
