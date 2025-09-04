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

import megamek.MegaMek;
import megamek.common.enums.GamePhase;
import megamek.common.game.InGameObject;
import megamek.common.options.OptionsConstants;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.logging.MMLogger;

record SBFPhasePreparationManager(SBFGameManager gameManager) implements SBFGameManagerHelper {
    private static final MMLogger logger = MMLogger.create(SBFPhasePreparationManager.class);

    void managePhase() {
        clearActions();
        switch (game().getPhase()) {
            case LOUNGE:
                gameManager.clearPendingReports();
                gameManager.transmitAllPlayerUpdates();
                break;
            case INITIATIVE:
                game().clearActions();
                gameManager.clearPendingReports();
                resetEntityPhase(game().getPhase());
                gameManager.transmitAllPlayerUpdates();
                gameManager.resetActivePlayersDone();

                // new round
                gameManager.rollInitiative();

                gameManager.incrementAndSendGameRound();
                gameManager.getAutoSaveService().performRollingAutosave();

                gameManager.initiativeHelper.determineTurnOrder(game().getPhase());
                gameManager.initiativeHelper.writeInitiativeReport();
                logger.info("Round {} memory usage: {}", game().getCurrentRound(), MegaMek.getMemoryUsed());
                break;
            case DEPLOY_MINEFIELDS:
                gameManager.transmitAllPlayerUpdates();
                gameManager.initiativeHelper.determineTurnOrder(game().getPhase());
                break;
            case SET_ARTILLERY_AUTO_HIT_HEXES:
                gameManager.transmitAllPlayerUpdates();
                game().resetTurnIndex();
                gameManager.sendCurrentTurns();
                break;
            case MOVEMENT:
            case PREMOVEMENT:
            case DEPLOYMENT:
            case PRE_FIRING:
            case FIRING:
            case PHYSICAL:
            case TARGETING:
            case OFFBOARD:
                // IO BF p204 offboard is a thing in SBF
                resetEntityPhase(game().getPhase());
                gameManager.transmitAllPlayerUpdates();
                gameManager.initiativeHelper.determineTurnOrder(game().getPhase());
                gameManager.unitUpdateHelper.sendAllUnitUpdate();
                gameManager.clearPendingReports();
                break;
            case END:
                resetEntityPhase(game().getPhase());
                gameManager.clearPendingReports();
                gameManager.transmitAllPlayerUpdates();
                gameManager.entityAllUpdate();
                break;
            case SBF_DETECTION:
                gameManager.detectionHelper.performSensorDetection();
                gameManager.unitUpdateHelper.sendAllUnitUpdate();
            case SBF_DETECTION_REPORT:
            case INITIATIVE_REPORT:
                gameManager.autoSave();
            case TARGETING_REPORT:
            case MOVEMENT_REPORT:
            case OFFBOARD_REPORT:
            case FIRING_REPORT:
            case PHYSICAL_REPORT:
            case END_REPORT:
                gameManager.resetActivePlayersDone();
                gameManager.sendReport();
                if (game().getOptions().booleanOption(OptionsConstants.BASE_PARANOID_AUTOSAVE)) {
                    gameManager.autoSave();
                }
                break;
            case VICTORY:
                gameManager.clearPendingReports();
                gameManager.addPendingReportsToGame();
                break;
            default:
                break;
        }
    }

    private void resetEntityPhase(GamePhase phase) {
        for (InGameObject unit : game().getInGameObjects()) {
            if (unit instanceof SBFFormation formation) {
                formation.setDone(false);
            }
        }
    }

    private void clearActions() {
        game().clearActions();
        gameManager.sendPendingActions();
    }
}
