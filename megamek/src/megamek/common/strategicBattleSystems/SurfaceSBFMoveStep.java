/*
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
package megamek.common.strategicBattleSystems;

import megamek.client.commands.ClientCommand;
import megamek.common.BoardLocation;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Terrains;

/**
 * This is an SBF move step that happens on the surface of a hex, i.e. on the ground or, if the destination
 * has water, on the water surface. This means that the elevation at start and end is considered to be 0.
 */
public class SurfaceSBFMoveStep extends SBFMoveStep {

    protected SurfaceSBFMoveStep(int formationId) {
        super(formationId);
    }

    public static SBFMoveStep createSurfaceMoveStep(SBFGame game, int formationId,
                                                    BoardLocation startingPoint, BoardLocation destination) {
        SBFMoveStep step = new SurfaceSBFMoveStep(formationId);
        step.startingPoint = startingPoint;
        step.destination = destination;
        step.compile(game);
        return step;
    }

    public static SBFMoveStep createSurfaceMoveStep(SBFGame game, int formationId,
                                                    BoardLocation startingPoint, int direction) {
        return createSurfaceMoveStep(game, formationId, startingPoint, startingPoint.translated(direction));
    }

    @Override
    protected void compile(SBFGame game) {
        super.compile(game);
        if (isIllegal) {
            // A step that is already illegal from basic tests does not need any further treatment
            return;
        }

        SBFFormation formation = game.getFormation(formationId).orElseThrow();
        boolean isNaval = formation.getMovementMode().isDeepWater();
        boolean isHover = formation.getMovementMode().isHover();
        boolean isWige = formation.getMovementMode() == SBFMovementMode.WIGE;
        boolean wheeledOrHover = isHover || formation.getMovementMode().isWheeled();

        boolean isInfantry = formation.isAnyTypeOf(SBFElementType.CI, SBFElementType.BA);
        boolean isMek = formation.isType(SBFElementType.BM);
        boolean isVehicle = formation.isType(SBFElementType.V);

        mpUsed = startingPoint.getCoords().distance(destination.getCoords());
        Hex startingHex = game.getBoard(startingPoint.getBoardId()).getHex(startingPoint.getCoords());
        Hex destinationHex = game.getBoard(destination.getBoardId()).getHex(destination.getCoords());
        int levelDifference = Math.abs(destinationHex.getLevel() - startingHex.getLevel());

        if (destinationHex.containsAnyTerrainOf(Terrains.WOODS)) {
            int woodsLevel = destinationHex.terrainLevel(Terrains.WOODS);
            mpUsed += wheeledOrHover ? 2 : woodsLevel;
            mpUsed -= isInfantry ? 1 : 0;
            isIllegal |= (woodsLevel > 1) && isVehicle;
            isIllegal |= (woodsLevel == 3) && !isInfantry;
        }

        if (destinationHex.containsAnyTerrainOf(Terrains.JUNGLE)) {
            int jungleLevel = destinationHex.terrainLevel(Terrains.JUNGLE);
            mpUsed += wheeledOrHover ? 2 : jungleLevel + 1;
            isIllegal |= (jungleLevel > 1) && isVehicle;
            isIllegal |= (jungleLevel == 3) && !isInfantry;
        }

        if (destinationHex.containsAnyTerrainOf(Terrains.RUBBLE)) {
            mpUsed++;
        }

        if (destinationHex.containsAnyTerrainOf(Terrains.ROUGH)) {
            mpUsed += (wheeledOrHover) ? 2 : 1;
        }

        if ((destinationHex.terrainLevel(Terrains.WATER) == 0) && isNaval) {
            isIllegal = true;
        }

        if (destinationHex.terrainLevel(Terrains.WATER) >= 1) {
            isIllegal = !isHover && !isNaval && !isWige;
        }

        mpUsed += levelDifference;
        isIllegal |= (levelDifference > 2) || ((levelDifference == 2) && !isMek);

        final Player owner = game.getPlayer(formation.getOwnerId());
        if (game.getActiveFormationsAt(startingPoint).stream().anyMatch(f -> game.areHostile(f, owner))) {
            mpUsed++;
        }
    }

    @Override
    public SBFMoveStep copy() {
        SBFMoveStep step = new SurfaceSBFMoveStep(formationId);
        step.startingPoint = startingPoint;
        step.destination = destination;
        step.mpUsed = mpUsed;
        step.isIllegal = isIllegal;
        return step;
    }

    @Override
    public String toString() {
        return ClientCommand.getDirection(startingPoint.getCoords().direction(destination.getCoords()));
    }

    @Override
    public int getMovementDirection() {
        return startingPoint.getCoords().direction(destination.getCoords());
    }
}
