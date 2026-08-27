/*
 * Copyright (C) 2002-2026 The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.overlay.ToastLevel;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Entity;
import megamek.common.units.TrainLayout;
import megamek.logging.MMLogger;

public class DeploymentHelper {

    private final ClientGUI clientgui;
    private static final MMLogger logger = MMLogger.create(DeploymentHelper.class);

    public DeploymentHelper(ClientGUI clientGUI) {
        this.clientgui = clientGUI;
    }

    public DeploymentHelper() {
        this.clientgui = null;
    }

    public boolean checkDeployment(Board board,
                                   Entity entity,
                                   Coords coords,
                                   boolean assaultDropPreference) {
        BoardValidationResult validationResult = validateDeploymentBoard(entity, board, coords, assaultDropPreference);
        if (validationResult == BoardValidationResult.WRONG_BOARD_TYPE) {
            showWrongBoardTypeMessage(board, entity);
            return false;
        } else if (validationResult == BoardValidationResult.OUTSIDE_DEPLOYMENT_AREA) {
            showOutsideDeployAreaMessage();
            return false;
        } else if (validationResult == BoardValidationResult.HIDDEN_IN_FORTIFIED) {
            showHiddenInFortifiedMessage();
            return false;
        } else if (validationResult == BoardValidationResult.HULL_DOWN_NEEDS_FORTIFIED) {
            showHullDownNeedsFortifiedMessage();
            return false;
        } else if (validationResult == BoardValidationResult.TRAIN_DOES_NOT_FIT) {
            showTrainDoesNotFitMessage();
            return false;
        }
        return true;
    }

    /**
     * Validates whether an entity can deploy on the given board at the specified coordinates.
     *
     * @param entity The entity to deploy
     * @param board  The board to deploy on
     * @param coords The coordinates for deployment
     * @return VALID if deployment can proceed, WRONG_BOARD_TYPE or OUTSIDE_DEPLOYMENT_AREA otherwise
     */
    BoardValidationResult validateDeploymentBoard(Entity entity,
                                                  Board board,
                                                  Coords coords,
                                                  boolean assaultDropPreference) {
        if (entity.isBoardProhibited(board)) {
            return BoardValidationResult.WRONG_BOARD_TYPE;
        }
        if (!(board.isLegalDeployment(coords, entity) || assaultDropPreference)) {
            return BoardValidationResult.OUTSIDE_DEPLOYMENT_AREA;
        }
        // A train deploys as one piece, so every hex it would occupy has to be legal, not just the tractor's.
        if (!entity.getAllTowedUnits().isEmpty() && !assaultDropPreference) {
            for (Coords trainHex : TrainLayout.deploymentFootprint(entity.getGame(),
                                                                   entity,
                                                                   coords,
                                                                   entity.getFacing())) {
                if (!board.isLegalDeployment(trainHex, entity)) {
                    logger.info("[Train] {} cannot deploy at {} facing {}: trailer hex {} is outside the "
                                + "deployment area", entity.getShortName(), coords, entity.getFacing(), trainHex);
                    return BoardValidationResult.TRAIN_DOES_NOT_FIT;
                }
            }
        }
        // A hidden unit cannot start in a fortified hex - the fortification is visible terrain that would give
        // the position away. (A unit may, however, deploy both dug in and hidden in concealing terrain.)
        megamek.common.Hex deployHex = board.getHex(coords);
        if (entity.isHidden() && (deployHex != null) && deployHex.containsTerrain(megamek.common.units.Terrains.FORTIFIED)) {
            return BoardValidationResult.HIDDEN_IN_FORTIFIED;
        }
        // A vehicle set to deploy hull-down must start in a fortified ("infantry-built") hex; only that terrain lets
        // a vehicle take cover, and Large Vehicles cannot use it at all (TO:AR p.19).
        if ((entity instanceof megamek.common.units.Tank deployingVehicle) && entity.isHullDown()) {
            boolean fortifiedHex = (deployHex != null) && deployHex.containsTerrain(megamek.common.units.Terrains.FORTIFIED);
            if (deployingVehicle.isLargeVehicleForHullDown() || !fortifiedHex) {
                return BoardValidationResult.HULL_DOWN_NEEDS_FORTIFIED;
            }
        }
        return BoardValidationResult.VALID;
    }

    private void showWrongBoardTypeMessage(Board board,
                                           Entity entity) {
        String title = Messages.getString("DeploymentDisplay.alertDialog.title");
        String boardType = switch (board.getBoardType()) {
            case CAPITAL_RADAR, RADAR -> "Radar";
            case SKY, SKY_WITH_TERRAIN -> "Atmospheric";
            case FAR_SPACE, NEAR_SPACE -> "Space";
            case GROUND -> "Ground";
        };
        String msg = Messages.getString("DeploymentDisplay.wrongMapType",
                                        entity.getShortName(),
                                        boardType);
        clientgui.addToast(ToastLevel.ERROR, msg, entity);
    }

    private void showOutsideDeployAreaMessage() {
        String msg = Messages.getString("DeploymentDisplay.outsideDeployArea");
        clientgui.addToast(ToastLevel.ERROR, msg);
    }

    private void showHiddenInFortifiedMessage() {
        String msg = Messages.getString("DeploymentDisplay.hiddenInFortified");
        clientgui.addToast(ToastLevel.WARNING, msg);
    }

    private void showTrainDoesNotFitMessage() {
        String msg = Messages.getString("DeploymentDisplay.trainDoesNotFit");
        clientgui.addToast(ToastLevel.WARNING, msg);
    }

    private void showHullDownNeedsFortifiedMessage() {
        String msg = Messages.getString("DeploymentDisplay.hullDownNeedsFortified");
        clientgui.addToast(ToastLevel.WARNING, msg);
    }

    /**
     * Represents the result of validating deployment on a board.
     */
    enum BoardValidationResult {
        /**
         * Deployment is valid and can proceed
         */
        VALID,
        /**
         * Entity cannot deploy on this board type (e.g., space unit on ground board)
         */
        WRONG_BOARD_TYPE,
        /**
         * Coordinates are outside the allowed deployment area
         */
        OUTSIDE_DEPLOYMENT_AREA,
        /**
         * A hidden unit cannot deploy onto a fortified hex (the fortification would reveal the position)
         */
        HIDDEN_IN_FORTIFIED,
        /**
         * A vehicle set to deploy hull-down must start in a fortified hex and must not be a Large Vehicle
         */
        HULL_DOWN_NEEDS_FORTIFIED,
        /**
         * A tractor's trailers would land outside the deployment area; the whole train has to fit
         */
        TRAIN_DOES_NOT_FIT
    }
}
