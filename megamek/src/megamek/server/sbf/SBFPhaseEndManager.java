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

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.strategicBattleSystems.SBFReportEntry;

public record SBFPhaseEndManager(SBFGameManager gameManager) implements SBFGameManagerHelper {

    void managePhase() {
        switch (gameManager.getGame().getPhase()) {
            case LOUNGE:
                gameManager.addPendingReportsToGame();
                gameManager.changePhase(GamePhase.EXCHANGE);
                break;
            case EXCHANGE:
            case STARTING_SCENARIO:
                gameManager.addPendingReportsToGame();
                // IO BF p.103: Arty auto is a thing in SBF
                gameManager.changePhase(GamePhase.INITIATIVE); // FIXME <- only for testing to get past arty auto and
                // minefields
                break;
            case SET_ARTILLERY_AUTO_HIT_HEXES:
                gameManager.addPendingReportsToGame();
                boolean hasMinesToDeploy = gameManager.getGame().getPlayersList().stream()
                      .anyMatch(Player::hasMinefields);
                if (hasMinesToDeploy) {
                    gameManager.changePhase(GamePhase.DEPLOY_MINEFIELDS);
                } else {
                    gameManager.changePhase(GamePhase.INITIATIVE);
                }
                break;
            case DEPLOY_MINEFIELDS, END:
                gameManager.changePhase(GamePhase.INITIATIVE);
                break;
            case DEPLOYMENT:
                gameManager.getGame().clearDeploymentThisRound();
                if (gameManager.getGame().getCurrentRound() == 0) {
                    gameManager.changePhase(GamePhase.INITIATIVE);
                } else {
                    // FIXME there appears to be no separate artillery targeting phase in SBF
                    gameManager.changePhase(GamePhase.TARGETING);
                }
                break;
            case INITIATIVE:
                gameManager.addPendingReportsToGame();
                gameManager.changePhase(GamePhase.INITIATIVE_REPORT);
                break;
            case INITIATIVE_REPORT:
                gameManager.getGame().setupDeployment();
                if (gameManager.getGame().shouldDeployThisRound()) {
                    // TODO: could be handled in SBFGame.isCurrentPhasePlayable
                    gameManager.changePhase(GamePhase.DEPLOYMENT);
                } else if (gameManager.usesDoubleBlind()) {
                    // TODO: Problem: phase "execution" always works with turns. Phases without turns are skipped
                    //  and must do their thing in prep or end. Means they must be skipped here explicitly
                    gameManager.changePhase(GamePhase.SBF_DETECTION);
                } else {
                    gameManager.changePhase(GamePhase.TARGETING);
                }
                break;
            case SBF_DETECTION:
                gameManager.changePhase(GamePhase.SBF_DETECTION_REPORT);
            case SBF_DETECTION_REPORT:
                gameManager.changePhase(GamePhase.DEPLOYMENT);
            case PREMOVEMENT:
                gameManager.changePhase(GamePhase.MOVEMENT);
                break;
            case MOVEMENT:
                goToDependingOnReport(GamePhase.MOVEMENT_REPORT, GamePhase.OFFBOARD);
                break;
            case MOVEMENT_REPORT:
                gameManager.changePhase(GamePhase.OFFBOARD);
                break;
            case PRE_FIRING:
                gameManager.changePhase(GamePhase.FIRING);
                break;
            case FIRING:
                addReport(new SBFReportEntry(3000));
                gameManager.actionsProcessor.handleActions();
                goToDependingOnReport(GamePhase.FIRING_REPORT, GamePhase.END);
                break;
            case FIRING_REPORT:
                gameManager.changePhase(GamePhase.END);
                break;
            case TARGETING:
                goToDependingOnReport(GamePhase.TARGETING_REPORT, GamePhase.PREMOVEMENT);
                gameManager.changePhase(GamePhase.PREMOVEMENT);
                break;
            case OFFBOARD:
                if (gameManager.getPendingReports().size() > 1) {
                    gameManager.getGame().addReports(gameManager.getPendingReports());
                    gameManager.changePhase(GamePhase.OFFBOARD_REPORT);
                } else {
                    // just the header, so we'll add the <nothing> label
                    gameManager.addReport(new SBFReportEntry(1205));
                    gameManager.getGame().addReports(gameManager.getPendingReports());
                    gameManager.sendReport();
                    gameManager.changePhase(GamePhase.PRE_FIRING);
                }
                break;
            case OFFBOARD_REPORT:
                // sendSpecialHexDisplayPackets();
                gameManager.changePhase(GamePhase.PRE_FIRING);
                break;
            case TARGETING_REPORT:
                gameManager.changePhase(GamePhase.PREMOVEMENT);
                break;
            default:
                break;
        }
    }

    private void goToDependingOnReport(GamePhase reportPhase, GamePhase afterReportPhase) {
        if (gameManager.getPendingReports().size() > 1) {
            gameManager.getGame().addReports(gameManager.getPendingReports());
            gameManager.changePhase(reportPhase);
        } else {
            // just the header, so we'll add the <nothing> label
            gameManager.addReport(new SBFReportEntry(1205));
            gameManager.getGame().addReports(gameManager.getPendingReports());
            gameManager.sendReport();
            gameManager.changePhase(afterReportPhase);
        }
    }
}
