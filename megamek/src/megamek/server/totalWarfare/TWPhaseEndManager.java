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

package megamek.server.totalWarfare;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameVictoryEvent;
import megamek.common.units.Entity;
import megamek.server.ServerHelper;

record TWPhaseEndManager(TWGameManager gameManager) {

    void managePhase() {
        switch (gameManager.getGame().getPhase()) {
            case LOUNGE:
                gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                gameManager.changePhase(GamePhase.EXCHANGE);
                break;
            case EXCHANGE:
            case STARTING_SCENARIO:
                gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                gameManager.changePhase(GamePhase.SET_ARTILLERY_AUTO_HIT_HEXES);
                break;
            case SET_ARTILLERY_AUTO_HIT_HEXES:
                gameManager.sendSpecialHexDisplayPackets();
                gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                boolean hasMinesToDeploy = gameManager.getGame()
                      .getPlayersList()
                      .stream()
                      .anyMatch(Player::hasMinefields);
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
                gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                gameManager.changePhase(GamePhase.INITIATIVE_REPORT);
                break;
            case INITIATIVE_REPORT:
                // NOTE: now that aerospace can come and go from the battlefield, I need to update the
                // deployment table every round. I think this it is OK to go here. (Taharqa)
                gameManager.getGame().setupDeployment();
                if (gameManager.getGame().shouldDeployThisRound()) {
                    gameManager.changePhase(GamePhase.DEPLOYMENT);
                } else {
                    gameManager.changePhase(GamePhase.TARGETING);
                }
                // Clean up bomb icons
                gameManager.clearBombIcons();
                gameManager.sendSpecialHexDisplayPackets();
                break;
            case PREMOVEMENT:
                gameManager.changePhase(GamePhase.MOVEMENT);
                break;
            case MOVEMENT:
                gameManager.detectHiddenUnits();
                ServerHelper.detectMinefields(gameManager.getGame(), gameManager.getMainPhaseReport(), gameManager);
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
                if (gameManager.getMainPhaseReport().size() > 1) {
                    gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                    gameManager.changePhase(GamePhase.MOVEMENT_REPORT);
                } else {
                    // just the header, so we'll add the <nothing> label
                    gameManager.addReport(new Report(1205, Report.PUBLIC));
                    gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                    gameManager.sendReport();
                    gameManager.changePhase(GamePhase.OFFBOARD);
                }
                break;
            case MOVEMENT_REPORT:
                gameManager.changePhase(GamePhase.OFFBOARD);
                break;
            case PRE_FIRING:
                gameManager.changePhase(GamePhase.FIRING);
                break;
            case FIRING:
                // write Weapon Attack Phase header
                gameManager.addReport(new Report(3000, Report.PUBLIC));
                gameManager.resolveWhatPlayersCanSeeWhatUnits();
                gameManager.resolveAllButWeaponAttacks();
                gameManager.resolveSelfDestruction();
                gameManager.reportGhostTargetRolls();
                gameManager.reportLargeCraftECCMRolls();
                gameManager.resolveOnlyWeaponAttacks();
                gameManager.assignAMS();
                gameManager.handleAttacks();
                gameManager.resolveBoobyTraps();  // booby trap says it resolves "immediately"... could be problematic
                gameManager.resolveScheduledNukes();
                gameManager.resolveScheduledOrbitalBombardments();
                gameManager.applyBuildingDamage();
                gameManager.checkForPSRFromDamage();
                gameManager.cleanupDestroyedNarcPods();
                gameManager.addReport(gameManager.resolvePilotingRolls());
                gameManager.checkForFlawedCooling();
                // check phase report
                if (gameManager.getMainPhaseReport().size() > 1) {
                    gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                    gameManager.changePhase(GamePhase.FIRING_REPORT);
                } else {
                    // just the header, so we'll add the <nothing> label
                    gameManager.addReport(new Report(1205, Report.PUBLIC));
                    gameManager.sendReport();
                    gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                    gameManager.changePhase(GamePhase.PHYSICAL);
                }
                gameManager.sendGroundObjectUpdate();
                // For bomb markers
                gameManager.sendSpecialHexDisplayPackets();
                break;
            case FIRING_REPORT:
                gameManager.changePhase(GamePhase.PHYSICAL);
                break;
            case PHYSICAL:
                gameManager.resolveWhatPlayersCanSeeWhatUnits();
                gameManager.resolvePhysicalAttacks();
                gameManager.resolveBoobyTraps(); // booby trap says it resolves "immediately"... could be problematic
                gameManager.applyBuildingDamage();
                gameManager.checkForPSRFromDamage();
                gameManager.addReport(gameManager.resolvePilotingRolls());
                gameManager.resolveSinkVees();
                gameManager.cleanupDestroyedNarcPods();
                gameManager.checkForFlawedCooling();
                gameManager.checkForChainWhipGrappleChecks();
                // check phase report
                if (gameManager.getMainPhaseReport().size() > 1) {
                    gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                    gameManager.changePhase(GamePhase.PHYSICAL_REPORT);
                } else {
                    // just the header, so we'll add the <nothing> label
                    gameManager.addReport(new Report(1205, Report.PUBLIC));
                    gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                    gameManager.sendReport();
                    gameManager.changePhase(GamePhase.END);
                }
                gameManager.sendGroundObjectUpdate();
                break;
            case PHYSICAL_REPORT:
                gameManager.changePhase(GamePhase.END);
                break;
            case TARGETING:
                gameManager.getMainPhaseReport().addElement(new Report(1035, Report.PUBLIC));
                gameManager.resolveAllButWeaponAttacks();
                gameManager.resolveOnlyWeaponAttacks();
                gameManager.handleAttacks();
                // check reports
                if (gameManager.getMainPhaseReport().size() > 1) {
                    gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                    gameManager.changePhase(GamePhase.TARGETING_REPORT);
                } else {
                    // just the header, so we'll add the <nothing> label
                    gameManager.getMainPhaseReport().addElement(new Report(1205, Report.PUBLIC));
                    gameManager.getGame().addReports(gameManager.getMainPhaseReport());
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
                if (gameManager.getMainPhaseReport().size() > 1) {
                    gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                    gameManager.changePhase(GamePhase.OFFBOARD_REPORT);
                } else {
                    // just the header, so we'll add the <nothing> label
                    gameManager.addReport(new Report(1205, Report.PUBLIC));
                    gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                    gameManager.sendReport();
                    gameManager.changePhase(GamePhase.PRE_FIRING);
                }
                break;
            case OFFBOARD_REPORT:
                gameManager.sendSpecialHexDisplayPackets();
                gameManager.changePhase(GamePhase.PRE_FIRING);
                break;
            case TARGETING_REPORT:
                gameManager.changePhase(GamePhase.PREMOVEMENT);
                break;
            case END:
                // remove any entities that died in the heat/end phase before
                // checking for victory
                gameManager.resetEntityPhase(GamePhase.END);

                // Remove expired temporary ECM fields (from EMP mines, etc.)
                gameManager.getGame().removeExpiredECMFields(
                      gameManager.getGame().getRoundCount(),
                      GamePhase.END
                );
                // Sync remaining ECM fields to clients
                gameManager.sendSyncTemporaryECMFields();

                boolean victory = gameManager.victory(); // note this may add reports
                // check phase report
                // HACK: hardcoded message ID check
                if ((gameManager.getMainPhaseReport().size() > 3) || ((gameManager.getMainPhaseReport().size() > 1)
                      && (gameManager.getMainPhaseReport().elementAt(1).messageId != 1205))) {
                    gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                    gameManager.changePhase(GamePhase.END_REPORT);
                } else {
                    // just the heat and end headers, so we'll add
                    // the <nothing> label
                    gameManager.addReport(new Report(1205, Report.PUBLIC));
                    gameManager.getGame().addReports(gameManager.getMainPhaseReport());
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
                gameManager.processTeamChangeRequest();
                if (gameManager.victory()) {
                    gameManager.changePhase(GamePhase.VICTORY);
                } else {
                    gameManager.changePhase(GamePhase.INITIATIVE);
                }
                break;
            case VICTORY:
                // FIXME: Sending this event here in the Server makes no sense at all, there are no listeners:
                GameVictoryEvent gve = new GameVictoryEvent(this, gameManager.getGame());
                gameManager.getGame().processGameEvent(gve);

                gameManager.transmitGameVictoryEventToAll();

                // FIXME: Why force-reset the game? Just let it stand
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
