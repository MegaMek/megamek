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
package megamek.server;

import megamek.common.Player;
import megamek.common.enums.GamePhase;

public class SBFPhaseEndManager {
    
    private final SBFGameManager gameManager;

    public SBFPhaseEndManager(SBFGameManager gameManager) {
        this.gameManager = gameManager;
    }
    
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
//                gameManager.changePhase(GamePhase.SET_ARTILLERY_AUTOHIT_HEXES);
                gameManager.changePhase(GamePhase.INITIATIVE); // <- only for testing to get past arty auto and minefields
                break;
            case SET_ARTILLERY_AUTOHIT_HEXES:
//                sendSpecialHexDisplayPackets();
                gameManager.addPendingReportsToGame();
                boolean hasMinesToDeploy = gameManager.getGame().getPlayersList().stream().anyMatch(Player::hasMinefields);
                if (hasMinesToDeploy) {
                    gameManager.changePhase(GamePhase.DEPLOY_MINEFIELDS);
                } else {
                    gameManager.changePhase(GamePhase.INITIATIVE);
                }
                break;
            case DEPLOY_MINEFIELDS:
                gameManager.changePhase(GamePhase.INITIATIVE);
                break;
            case DEPLOYMENT:
                gameManager.getGame().clearDeploymentThisRound();
                if (gameManager.getGame().getCurrentRound() == 0) {
                    gameManager.changePhase(GamePhase.INITIATIVE);
                } else {
                    //FIXME there appears to be no separate artillery targeting phase in SBF
                    gameManager.changePhase(GamePhase.TARGETING);
                }
                break;
            case INITIATIVE:
//                resolveWhatPlayersCanSeeWhatUnits();
//                detectSpacecraft();
                gameManager.addPendingReportsToGame();
                gameManager.changePhase(GamePhase.INITIATIVE_REPORT);
                break;
            case INITIATIVE_REPORT:
                gameManager.getGame().setupDeployment();
                if (gameManager.getGame().shouldDeployThisRound()) {
                    gameManager.changePhase(GamePhase.DEPLOYMENT);
                } else {
                    gameManager.changePhase(GamePhase.TARGETING);
                }
                break;
            case PREMOVEMENT:
                gameManager.changePhase(GamePhase.MOVEMENT);
                break;
            case MOVEMENT:
//                detectHiddenUnits();
//                ServerHelper.detectMinefields(game, vPhaseReport, this);
//                updateSpacecraftDetection();
//                detectSpacecraft();
//                resolveWhatPlayersCanSeeWhatUnits();
//                doAllAssaultDrops();
//                addMovementHeat();
//                applyBuildingDamage();
//                checkForPSRFromDamage();
//                addReport(resolvePilotingRolls()); // Skids cause damage in
//                // movement phase
//                checkForFlamingDamage();
//                checkForTeleMissileAttacks();
//                cleanupDestroyedNarcPods();
//                checkForFlawedCooling();
//                resolveCallSupport();
//                // check phase report
//                if (vPhaseReport.size() > 1) {
//                    gameManager.getGame().addReports(vPhaseReport);
//                    gameManager.changePhase(GamePhase.MOVEMENT_REPORT);
//                } else {
//                    // just the header, so we'll add the <nothing> label
//                    addReport(new Report(1205, Report.PUBLIC));
//                    gameManager.getGame().addReports(vPhaseReport);
//                    sendReport();
//                    gameManager.changePhase(GamePhase.OFFBOARD);
//                }
                break;
            case MOVEMENT_REPORT:
                gameManager.changePhase(GamePhase.OFFBOARD);
                break;
            case PREFIRING:
                gameManager.changePhase(GamePhase.FIRING);
                break;
            case FIRING:
//                // write Weapon Attack Phase header
//                addReport(new Report(3000, Report.PUBLIC));
//                resolveWhatPlayersCanSeeWhatUnits();
//                resolveAllButWeaponAttacks();
//                resolveSelfDestructions();
//                reportGhostTargetRolls();
//                reportLargeCraftECCMRolls();
//                resolveOnlyWeaponAttacks();
//                assignAMS();
//                handleAttacks();
//                resolveScheduledNukes();
//                applyBuildingDamage();
//                checkForPSRFromDamage();
//                cleanupDestroyedNarcPods();
//                addReport(resolvePilotingRolls());
//                checkForFlawedCooling();
//                // check phase report
//                if (vPhaseReport.size() > 1) {
//                    gameManager.getGame().addReports(vPhaseReport);
//                    gameManager.changePhase(GamePhase.FIRING_REPORT);
//                } else {
//                    // just the header, so we'll add the <nothing> label
//                    addReport(new Report(1205, Report.PUBLIC));
//                    sendReport();
//                    gameManager.getGame().addReports(vPhaseReport);
//                    gameManager.changePhase(GamePhase.PHYSICAL);
//                }
                break;
            case FIRING_REPORT:
                gameManager.changePhase(GamePhase.PHYSICAL);
                break;
            case PHYSICAL:
//                resolveWhatPlayersCanSeeWhatUnits();
//                resolvePhysicalAttacks();
//                applyBuildingDamage();
//                checkForPSRFromDamage();
//                addReport(resolvePilotingRolls());
//                resolveSinkVees();
//                cleanupDestroyedNarcPods();
//                checkForFlawedCooling();
//                checkForChainWhipGrappleChecks();
//                // check phase report
//                if (vPhaseReport.size() > 1) {
//                    gameManager.getGame().addReports(vPhaseReport);
//                    gameManager.changePhase(GamePhase.PHYSICAL_REPORT);
//                } else {
//                    // just the header, so we'll add the <nothing> label
//                    addReport(new Report(1205, Report.PUBLIC));
//                    gameManager.getGame().addReports(vPhaseReport);
//                    sendReport();
//                    gameManager.changePhase(GamePhase.END);
//                }
                break;
            case PHYSICAL_REPORT:
                gameManager.changePhase(GamePhase.END);
                break;
            case TARGETING:
//                vPhaseReport.addElement(new Report(1035, Report.PUBLIC));
//                resolveAllButWeaponAttacks();
//                resolveOnlyWeaponAttacks();
//                handleAttacks();
//                // check reports
//                if (vPhaseReport.size() > 1) {
//                    gameManager.getGame().addReports(vPhaseReport);
//                    gameManager.changePhase(GamePhase.TARGETING_REPORT);
//                } else {
//                    // just the header, so we'll add the <nothing> label
//                    vPhaseReport.addElement(new Report(1205, Report.PUBLIC));
//                    gameManager.getGame().addReports(vPhaseReport);
//                    sendReport();
//                    gameManager.changePhase(GamePhase.PREMOVEMENT);
//                }
//
//                sendSpecialHexDisplayPackets();
//                for (Enumeration<Player> i = gameManager.getGame().getPlayers(); i.hasMoreElements(); ) {
//                    Player player = i.nextElement();
//                    int connId = player.getId();
//                    send(connId, createArtilleryPacket(player));
//                }
//
                break;
            case OFFBOARD:
//                // write Offboard Attack Phase header
//                addReport(new Report(1100, Report.PUBLIC));
//                resolveAllButWeaponAttacks(); // torso twist or flip arms
//                // possible
//                resolveOnlyWeaponAttacks(); // should only be TAG at this point
//                handleAttacks();
//                for (Enumeration<Player> i = gameManager.getGame().getPlayers(); i.hasMoreElements(); ) {
//                    Player player = i.nextElement();
//                    int connId = player.getId();
//                    send(connId, createArtilleryPacket(player));
//                }
//                applyBuildingDamage();
//                checkForPSRFromDamage();
//                addReport(resolvePilotingRolls());
//
//                cleanupDestroyedNarcPods();
//                checkForFlawedCooling();
//
//                sendSpecialHexDisplayPackets();
//                sendTagInfoUpdates();
//
//                // check reports
//                if (vPhaseReport.size() > 1) {
//                    gameManager.getGame().addReports(vPhaseReport);
//                    gameManager.changePhase(GamePhase.OFFBOARD_REPORT);
//                } else {
//                    // just the header, so we'll add the <nothing> label
//                    addReport(new Report(1205, Report.PUBLIC));
//                    gameManager.getGame().addReports(vPhaseReport);
//                    sendReport();
//                    gameManager.changePhase(GamePhase.PREFIRING);
//                }
                break;
            case OFFBOARD_REPORT:
//                sendSpecialHexDisplayPackets();
                gameManager.changePhase(GamePhase.PREFIRING);
                break;
            case TARGETING_REPORT:
                gameManager.changePhase(GamePhase.PREMOVEMENT);
                break;
            case END:
//                // remove any entities that died in the heat/end phase before
//                // checking for victory
//                resetEntityPhase(GamePhase.END);
//                boolean victory = victory(); // note this may add reports
//                // check phase report
//                // HACK: hardcoded message ID check
//                if ((vPhaseReport.size() > 3) || ((vPhaseReport.size() > 1)
//                        && (vPhaseReport.elementAt(1).messageId != 1205))) {
//                    gameManager.getGame().addReports(vPhaseReport);
//                    gameManager.changePhase(GamePhase.END_REPORT);
//                } else {
//                    // just the heat and end headers, so we'll add
//                    // the <nothing> label
//                    addReport(new Report(1205, Report.PUBLIC));
//                    gameManager.getGame().addReports(vPhaseReport);
//                    sendReport();
//                    if (victory) {
//                        gameManager.changePhase(GamePhase.VICTORY);
//                    } else {
//                        gameManager.changePhase(GamePhase.INITIATIVE);
//                    }
//                }
//                // Decrement the ASEWAffected counter
//                decrementASEWTurns();

                break;
            case END_REPORT:
//                if (changePlayersTeam) {
//                    processTeamChangeRequest();
//                }
//                if (victory()) {
//                    gameManager.changePhase(GamePhase.VICTORY);
//                } else {
//                    gameManager.changePhase(GamePhase.INITIATIVE);
//                }
                break;
            case VICTORY:
//                GameVictoryEvent gve = new GameVictoryEvent(this, game);
//                gameManager.getGame().processGameEvent(gve);
//                transmitGameVictoryEventToAll();
//                resetGame();
                break;
            default:
                break;
        }

        // Any hidden units that activated this phase, should clear their
        // activating phase
//        for (Entity ent : gameManager.getGame().getEntitiesVector()) {
//            if (ent.getHiddenActivationPhase() == gameManager.getGame().getPhase()) {
//                ent.setHiddenActivationPhase(GamePhase.UNKNOWN);
//            }
//        }
    }
}
