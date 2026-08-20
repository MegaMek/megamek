/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server.sbf;

import java.util.List;
import java.util.Optional;

import megamek.common.board.BoardLocation;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFMovePath;
import megamek.common.strategicBattleSystems.SBFMoveStep;
import megamek.common.strategicBattleSystems.SurfaceSBFMoveStep;
import megamek.logging.MMLogger;

record SBFMovementProcessor(SBFGameManager gameManager) implements SBFGameManagerHelper {
    private static final MMLogger logger = MMLogger.create(SBFMovementProcessor.class);

    boolean processMovement(SBFMovePath submittedPath, SBFFormation formation) {
        if (!validatePermitted(formation)) {
            return false;
        }

        Optional<SBFMovePath> rebuiltPath = rebuildPath(submittedPath, formation);
        if (rebuiltPath.isEmpty() || rebuiltPath.get().isIllegal()) {
            logger.error("Illegal move path!");
            return false;
        }

        SBFMovePath authoritativePath = rebuiltPath.get();
        formation.setPosition(authoritativePath.getLastPosition());
        formation.setJumpUsedThisTurn(authoritativePath.getJumpUsed());
        formation.setSprintedThisTurn(authoritativePath.getMpUsed() > formation.getMovement());
        formation.setDone(true);
        gameManager.sendUnitUpdate(formation);
        gameManager.endCurrentTurn(formation);
        return true;
    }

    private boolean validatePermitted(SBFFormation formation) {
        if (!game().getPhase().isMovement()) {
            logger.error("Server got movement packet in wrong phase!");
            return false;
        } else if (formation.isDone()) {
            logger.error("Formation already done!");
            return false;
        }
        return true;
    }

    private Optional<SBFMovePath> rebuildPath(SBFMovePath submittedPath, SBFFormation formation) {
        try {
            BoardLocation currentLocation = formation.getPosition();
            int jumpUsed = submittedPath.getJumpUsed();
            if (!game().hasBoardLocation(currentLocation) || (jumpUsed < 0) || (jumpUsed > formation.getJumpMove())) {
                return Optional.empty();
            }

            List<?> submittedSteps = submittedPath.getSteps();
            if (submittedSteps.size() > SBFMovePath.maximumMovementPoints(formation, game())) {
                return Optional.empty();
            }

            SBFMovePath rebuiltPath = new SBFMovePath(formation.getId(), currentLocation, game());
            for (Object submittedObject : submittedSteps) {
                if (!(submittedObject instanceof SBFMoveStep submittedStep)) {
                    return Optional.empty();
                }

                BoardLocation destination = submittedStep.getDestination();
                if (!game().hasBoardLocation(destination)
                      || (destination.boardId() != currentLocation.boardId())
                      || (currentLocation.coords().distance(destination.coords()) != 1)) {
                    return Optional.empty();
                }

                rebuiltPath.addStep(SurfaceSBFMoveStep.createSurfaceMoveStep(game(), formation.getId(),
                      currentLocation, destination));
                currentLocation = destination;
            }
            rebuiltPath.setJumpUsed(jumpUsed);
            return Optional.of(rebuiltPath);
        } catch (RuntimeException exception) {
            logger.warn("Rejected malformed SBF movement path", exception);
            return Optional.empty();
        }
    }
}
