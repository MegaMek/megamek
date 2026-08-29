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

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.overlay.ToastLevel;
import megamek.client.ui.dialogs.phaseDisplay.DeployElevationChoiceDialog;
import megamek.client.ui.dialogs.phaseDisplay.DeployFacingChoiceDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.panels.phaseDisplay.DeploymentDisplay.DeploymentPosition;
import megamek.common.Hex;
import megamek.common.annotations.Nullable;
import megamek.common.board.AllowedDeploymentHelper;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.DeploymentElevationType;
import megamek.common.board.ElevationOption;
import megamek.common.board.FacingOption;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.common.units.Terrains;
import megamek.common.units.TrainLayout;
import megamek.logging.MMLogger;

import java.util.List;
import java.util.Set;

public class DeploymentHelper {

    private final ClientGUI clientgui;
    private Entity currentEntity;

    private static final MMLogger logger = MMLogger.create(DeploymentHelper.class);

    public DeploymentHelper(ClientGUI clientGUI) {
        this.clientgui = clientGUI;
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
        Hex deployHex = board.getHex(coords);
        if (entity.isHidden() && (deployHex != null) && deployHex.containsTerrain(Terrains.FORTIFIED)) {
            return BoardValidationResult.HIDDEN_IN_FORTIFIED;
        }
        // A vehicle set to deploy hull-down must start in a fortified ("infantry-built") hex; only that terrain lets
        // a vehicle take cover, and Large Vehicles cannot use it at all (TO:AR p.19).
        if ((entity instanceof Tank deployingVehicle) && entity.isHullDown()) {
            boolean fortifiedHex = (deployHex != null) && deployHex.containsTerrain(Terrains.FORTIFIED);
            if (deployingVehicle.isLargeVehicleForHullDown() || !fortifiedHex) {
                return BoardValidationResult.HULL_DOWN_NEEDS_FORTIFIED;
            }
        }
        return BoardValidationResult.VALID;
    }

    private void showWrongBoardTypeMessage(Board board,
                                           Entity entity) {
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

    /**
     * Determines the deployment position (elevation and facing) for an entity at the given coordinates. Handles user
     * interaction for elevation and facing choices when multiple options are available.
     *
     * @param entity The entity being deployed
     * @param coords The coordinates where deployment is attempted
     * @param board  The board on which deployment is occurring
     * @return DeploymentPosition with elevation and facing, or null if deployment was cancelled or invalid
     */
    public @Nullable DeploymentPosition determineDeploymentPosition(Entity entity,
                                                                    Coords coords,
                                                                    Board board,
                                                                    Set<ElevationOption> lastHexDeploymentOptions,
                                                                    ElevationOption lastDeploymentOption) {
        currentEntity = entity;
        int finalElevation;
        int finalFacing = entity.getFacing();
        var deploymentHelper = new AllowedDeploymentHelper(entity, coords, board,
                                                           board.getHex(coords), entity.getGame());
        java.util.List<ElevationOption> elevationOptions = deploymentHelper.findAllowedElevations();
        int FACING_ELEVATION = 0; // If we care about facing at other altitudes or elevations ever...
        FacingOption facingOptions = deploymentHelper.findAllowedFacings(FACING_ELEVATION);
        boolean validFacings = facingOptions != null && facingOptions.hasValidFacings();

        if (elevationOptions.isEmpty() && !validFacings) {
            showCannotDeployHereMessage(coords);
            return null;
        } else if (elevationOptions.size() == 1) {
            finalElevation = elevationOptions.getFirst().elevation();
            lastHexDeploymentOptions.clear();
            lastHexDeploymentOptions.addAll(elevationOptions);
            lastDeploymentOption = elevationOptions.getFirst();
            finalFacing = promptForFacingIfNeeded(facingOptions, finalFacing);
        } else if (useLastDeployElevation(elevationOptions,
                                          lastDeploymentOption,
                                          lastHexDeploymentOptions) && !coords.equals(entity.getPosition())) {
            // When the player clicks the same hex again, always ask for the elevation
            finalElevation = entity.isAero() ? entity.getAltitude() : entity.getElevation();
        } else if (elevationOptions.isEmpty() && validFacings) {
            finalElevation = FACING_ELEVATION; // Only option in current implementation
            finalFacing = promptForFacingIfNeeded(facingOptions, finalFacing);
        } else {
            ElevationOption elevationOption = showElevationChoiceDialog(elevationOptions);
            if (elevationOption != null) {
                lastHexDeploymentOptions.clear();
                lastHexDeploymentOptions.addAll(elevationOptions);
                lastDeploymentOption = elevationOption;
                finalElevation = elevationOption.elevation();
                finalFacing = promptForFacingIfNeeded(facingOptions, finalFacing);
            } else {
                return null;
            }
        }

        return new DeploymentPosition(finalElevation, finalFacing);
    }

    private @Nullable ElevationOption showElevationChoiceDialog(List<ElevationOption> elevationOptions) {
        var dlg = new DeployElevationChoiceDialog(clientgui.getFrame(),
                                                  elevationOptions);
        DialogResult result = dlg.showDialog();
        if ((result == DialogResult.CONFIRMED) && (dlg.getFirstChoice() != null)) {
            if (dlg.getFirstChoice().type() == DeploymentElevationType.ELEVATIONS_ABOVE) {
                int elevation = showHighElevationChoiceDialog();
                return (elevation == -1) ?
                       null :
                       new ElevationOption(elevation, DeploymentElevationType.ELEVATIONS_ABOVE);
            } else {
                return dlg.getFirstChoice();
            }
        } else {
            return null;
        }
    }

    /**
     * Shows a dialog allowing the user to choose a facing from the valid facings. For facing-dependent entities (like
     * non-symmetrical multi-hex buildings), this allows the user to select which facing to deploy with.
     *
     * @param facingOption The FacingOption containing valid facings for the position
     * @return The chosen facing (0-5), or -1 if cancelled or no valid facings
     */
    private int showFacingChoiceDialog(FacingOption facingOption) {
        if (facingOption == null || !facingOption.hasValidFacings()) {
            return -1;
        }

        var dlg = new DeployFacingChoiceDialog(clientgui.getFrame(),
                                               facingOption);
        DialogResult result = dlg.showDialog();
        if ((result == DialogResult.CONFIRMED) && (dlg.getChosenFacing() != -1)) {
            return dlg.getChosenFacing();
        } else {
            return -1;
        }
    }

    private int showHighElevationChoiceDialog() {
        String msg = Messages.getString("DeploymentDisplay.elevationChoice");
        String input = javax.swing.JOptionPane.showInputDialog(clientgui.getFrame(), msg);
        try {
            return Integer.parseInt(input);
        } catch (Exception ex) {
            return -1;
        }
    }

    /**
     * @return True when the last chosen elevation can be re-used without asking again. This is true when the options
     * for the current hex have no option that the previous hex didn't and the previous deployment option is
     * available in the new hex.
     */
    private boolean useLastDeployElevation(List<ElevationOption> currentOptions,
                                           ElevationOption lastDeploymentOption,
                                           Set<ElevationOption> lastHexDeploymentOptions) {
        return ((lastDeploymentOption != null) &&
                (lastDeploymentOption.type() == DeploymentElevationType.ELEVATIONS_ABOVE) &&
                isHighElevationAvailable(currentOptions, lastDeploymentOption.elevation())) ||
               ((currentOptions.size() <= lastHexDeploymentOptions.size()) &&
                lastHexDeploymentOptions.containsAll(currentOptions) &&
                currentOptions.contains(lastDeploymentOption));
    }

    private boolean isHighElevationAvailable(List<ElevationOption> currentOptions,
                                             int elevation) {
        return currentOptions.stream()
                             .filter(o -> o.type() == DeploymentElevationType.ELEVATIONS_ABOVE)
                             .anyMatch(o -> o.elevation() <= elevation);
    }

    private void showCannotDeployHereMessage(Coords coords) {
        String msg = Messages.getString("DeploymentDisplay.cantDeployInto",
                                        currentEntity.getShortName(),
                                        coords.getBoardNum());
        clientgui.addToast(ToastLevel.ERROR, msg, currentEntity);
    }

    /**
     * Prompts the user to select a facing if needed, based on the available facing options. If all 6 facings are valid,
     * no prompt is shown and the current facing is returned. If some facings are restricted, shows a dialog to let the
     * user choose.
     *
     * @param facingOption  The FacingOption containing valid facings, or null if not applicable
     * @param currentFacing The entity's current facing
     * @return The chosen facing (0-5), or currentFacing if no selection was made
     */
    private int promptForFacingIfNeeded(FacingOption facingOption,
                                        int currentFacing) {
        if (facingOption == null || !facingOption.hasValidFacings()) {
            return currentFacing;
        }

        // All 6 facings valid? Skip the dialog
        if (facingOption.getValidFacingCount() == 6) {
            return currentFacing;
        }

        // Only one choice? Pick it.
        if (facingOption.getValidFacingCount() == 1) {
            return (int) facingOption.getValidFacings().toArray()[0];
        }

        // Show facing choice dialog
        int chosenFacing = showFacingChoiceDialog(facingOption);
        return (chosenFacing != -1) ? chosenFacing : currentFacing;
    }
}
