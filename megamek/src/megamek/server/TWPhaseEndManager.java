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

import megamek.common.Entity;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameVictoryEvent;

class TWPhaseEndManager {

    private final GameManager gameManager;

    public TWPhaseEndManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    void managePhase() {
        switch (gameManager.getGame().getPhase()) {
            case LOUNGE:
                gameManager.getGame().addReports(gameManager.getvPhaseReport());
                gameManager.changePhase(GamePhase.EXCHANGE);
                break;
            case EXCHANGE:
            case STARTING_SCENARIO:
                gameManager.getGame().addReports(gameManager.getvPhaseReport());
                gameManager.changePhase(GamePhase.SET_ARTILLERY_AUTOHIT_HEXES);
                break;
            case SET_ARTILLERY_AUTOHIT_HEXES:
                gameManager.sendSpecialHexDisplayPackets();
                gameManager.getGame().addReports(gameManager.getvPhaseReport());
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
                gameManager.getGame().getPlayersList().forEach(Player::adjustStartingPosForReinforcements);
                if (gameManager.getGame().getRoundCount() < 1) {
                    gameManager.changePhase(GamePhase.INITIATIVE);
                } else {
                    gameManager.changePhase(GamePhase.TARGETING);
                }
                break;
            case INITIATIVE:
                gameManager.resolveWhatPlayersCanSeeWhatUnits();
                gameManager.detectSpacecraft();
                gameManager.getGame().addReports(gameManager.getvPhaseReport());
                gameManager.changePhase(GamePhase.INITIATIVE_REPORT);
                break;
            case INITIATIVE_REPORT:
                // NOTE: now that aeros can come and go from the battlefield, I need to update the
                // deployment table every round. I think this it is OK to go here. (Taharqa)
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
                gameManager.detectHiddenUnits();
                ServerHelper.detectMinefields(gameManager.getGame(), gameManager.getvPhaseReport(), gameManager);
                gameManager.updateSpacecraftDetection();
                gameManager.detectSpacecraft();
                gameManager.resolveWhatPlayersCanSeeWhatUnits();
                gameManager.doAllAssaultDrops();
                gameManager.addMovementHeat();
                gameManager.applyBuildingDamage();
                gameManager.checkForPSRFromDamage();
                gameManager.addReport(gameManager.resolvePilotingRolls()); // Skids cause damage in
                // movement phase
                gameManager.checkForFlamingDamage();
                gameManager.checkForTeleMissileAttacks();
                gameManager.cleanupDestroyedNarcPods();
                gameManager.checkForFlawedCooling();
                gameManager.resolveCallSupport();
                // check phase report
                if (gameManager.getvPhaseReport().size() > 1) {
                    gameManager.getGame().addReports(gameManager.getvPhaseReport());
                    gameManager.changePhase(GamePhase.MOVEMENT_REPORT);
                } else {
                    // just the header, so we'll add the <nothing> label
                    gameManager.addReport(new Report(1205, Report.PUBLIC));
                    gameManager.getGame().addReports(gameManager.getvPhaseReport());
                    gameManager.sendReport();
                    gameManager.changePhase(GamePhase.OFFBOARD);
                }
                break;
            case MOVEMENT_REPORT:
                gameManager.changePhase(GamePhase.OFFBOARD);
                break;
            case PREFIRING:
                gameManager.changePhase(GamePhase.FIRING);
                break;
            case FIRING:
                // write Weapon Attack Phase header
                gameManager.addReport(new Report(3000, Report.PUBLIC));
                gameManager.resolveWhatPlayersCanSeeWhatUnits();
                gameManager.resolveAllButWeaponAttacks();
                gameManager.resolveSelfDestructions();
                gameManager.reportGhostTargetRolls();
                gameManager.reportLargeCraftECCMRolls();
                gameManager.resolveOnlyWeaponAttacks();
                gameManager.assignAMS();
                gameManager.handleAttacks();
                gameManager.resolveScheduledNukes();
                gameManager.applyBuildingDamage();
                gameManager.checkForPSRFromDamage();
                gameManager.cleanupDestroyedNarcPods();
                gameManager.addReport(gameManager.resolvePilotingRolls());
                gameManager.checkForFlawedCooling();
                // check phase report
                if (gameManager.getvPhaseReport().size() > 1) {
                    gameManager.getGame().addReports(gameManager.getvPhaseReport());
                    gameManager.changePhase(GamePhase.FIRING_REPORT);
                } else {
                    // just the header, so we'll add the <nothing> label
                    gameManager.addReport(new Report(1205, Report.PUBLIC));
                    gameManager.sendReport();
                    gameManager.getGame().addReports(gameManager.getvPhaseReport());
                    gameManager.changePhase(GamePhase.PHYSICAL);
                }
                break;
            case FIRING_REPORT:
                gameManager.changePhase(GamePhase.PHYSICAL);
                break;
            case PHYSICAL:
                gameManager.resolveWhatPlayersCanSeeWhatUnits();
                gameManager.resolvePhysicalAttacks();
                gameManager.applyBuildingDamage();
                gameManager.checkForPSRFromDamage();
                gameManager.addReport(gameManager.resolvePilotingRolls());
                gameManager.resolveSinkVees();
                gameManager.cleanupDestroyedNarcPods();
                gameManager.checkForFlawedCooling();
                gameManager.checkForChainWhipGrappleChecks();
                // check phase report
                if (gameManager.getvPhaseReport().size() > 1) {
                    gameManager.getGame().addReports(gameManager.getvPhaseReport());
                    gameManager.changePhase(GamePhase.PHYSICAL_REPORT);
                } else {
                    // just the header, so we'll add the <nothing> label
                    gameManager.addReport(new Report(1205, Report.PUBLIC));
                    gameManager.getGame().addReports(gameManager.getvPhaseReport());
                    gameManager.sendReport();
                    gameManager.changePhase(GamePhase.END);
                }
                break;
            case PHYSICAL_REPORT:
                gameManager.changePhase(GamePhase.END);
                break;
            case TARGETING:
                gameManager.getvPhaseReport().addElement(new Report(1035, Report.PUBLIC));
                gameManager.resolveAllButWeaponAttacks();
                gameManager.resolveOnlyWeaponAttacks();
                gameManager.handleAttacks();
                // check reports
                if (gameManager.getvPhaseReport().size() > 1) {
                    gameManager.getGame().addReports(gameManager.getvPhaseReport());
                    gameManager.changePhase(GamePhase.TARGETING_REPORT);
                } else {
                    // just the header, so we'll add the <nothing> label
                    gameManager.getvPhaseReport().addElement(new Report(1205, Report.PUBLIC));
                    gameManager.getGame().addReports(gameManager.getvPhaseReport());
                    gameManager.sendReport();
                    gameManager.changePhase(GamePhase.PREMOVEMENT);
                }

                gameManager.sendSpecialHexDisplayPackets();
                gameManager.getGame().getPlayersList()
                        .forEach(player -> gameManager.send(player.getId(), gameManager.createArtilleryPacket(player)));
                break;
            case OFFBOARD:
                // write Offboard Attack Phase header
                gameManager.addReport(new Report(1100, Report.PUBLIC));
                gameManager.resolveAllButWeaponAttacks(); // torso twist or flip arms
                // possible
                gameManager.resolveOnlyWeaponAttacks(); // should only be TAG at this point
                gameManager.handleAttacks();
                gameManager.getGame().getPlayersList()
                        .forEach(player -> gameManager.send(player.getId(), gameManager.createArtilleryPacket(player)));
                gameManager.applyBuildingDamage();
                gameManager.checkForPSRFromDamage();
                gameManager.addReport(gameManager.resolvePilotingRolls());

                gameManager.cleanupDestroyedNarcPods();
                gameManager.checkForFlawedCooling();

                gameManager.sendSpecialHexDisplayPackets();
                gameManager.sendTagInfoUpdates();

                // check reports
                if (gameManager.getvPhaseReport().size() > 1) {
                    gameManager.getGame().addReports(gameManager.getvPhaseReport());
                    gameManager.changePhase(GamePhase.OFFBOARD_REPORT);
                } else {
                    // just the header, so we'll add the <nothing> label
                    gameManager.addReport(new Report(1205, Report.PUBLIC));
                    gameManager.getGame().addReports(gameManager.getvPhaseReport());
                    gameManager.sendReport();
                    gameManager.changePhase(GamePhase.PREFIRING);
                }
                break;
            case OFFBOARD_REPORT:
                gameManager.sendSpecialHexDisplayPackets();
                gameManager.changePhase(GamePhase.PREFIRING);
                break;
            case TARGETING_REPORT:
                gameManager.changePhase(GamePhase.PREMOVEMENT);
                break;
            case END:
                // remove any entities that died in the heat/end phase before
                // checking for victory
                gameManager.resetEntityPhase(GamePhase.END);
                boolean victory = gameManager.victory(); // note this may add reports
                // check phase report
                // HACK: hardcoded message ID check
                if ((gameManager.getvPhaseReport().size() > 3) || ((gameManager.getvPhaseReport().size() > 1)
                        && (gameManager.getvPhaseReport().elementAt(1).messageId != 1205))) {
                    gameManager.getGame().addReports(gameManager.getvPhaseReport());
                    gameManager.changePhase(GamePhase.END_REPORT);
                } else {
                    // just the heat and end headers, so we'll add
                    // the <nothing> label
                    gameManager.addReport(new Report(1205, Report.PUBLIC));
                    gameManager.getGame().addReports(gameManager.getvPhaseReport());
                    gameManager.sendReport();
                    if (victory) {
                        gameManager.changePhase(GamePhase.VICTORY);
                    } else {
                        gameManager.changePhase(GamePhase.INITIATIVE);
                    }
                }
                // Decrement the ASEWAffected counter
                gameManager.decrementASEWTurns();

                break;
            case END_REPORT:
                if (gameManager.changePlayersTeam()) {
                    gameManager.processTeamChangeRequest();
                }
                if (gameManager.victory()) {
                    gameManager.changePhase(GamePhase.VICTORY);
                } else {
                    gameManager.changePhase(GamePhase.INITIATIVE);
                }
                break;
            case VICTORY:
                GameVictoryEvent gve = new GameVictoryEvent(this, gameManager.getGame());
                gameManager.getGame().processGameEvent(gve);
                gameManager.transmitGameVictoryEventToAll();
                gameManager.resetGame();
                break;
            default:
                break;
        }

        // Any hidden units that activated this phase, should clear their
        // activating phase
        for (Entity ent : gameManager.getGame().getEntitiesVector()) {
            if (ent.getHiddenActivationPhase() == gameManager.getGame().getPhase()) {
                ent.setHiddenActivationPhase(GamePhase.UNKNOWN);
            }
        }
    }
}
