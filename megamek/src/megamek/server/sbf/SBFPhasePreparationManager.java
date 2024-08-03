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
package megamek.server.sbf;

import megamek.MegaMek;
import megamek.common.InGameObject;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.common.strategicBattleSystems.SBFFormation;
import org.apache.logging.log4j.LogManager;

record SBFPhasePreparationManager(SBFGameManager gameManager) implements SBFGameManagerHelper {

    void managePhase() {
        switch (game().getPhase()) {
            case LOUNGE:
                gameManager.clearPendingReports();
//                MapSettings mapSettings = game.getMapSettings();
//                mapSettings.setBoardsAvailableVector(ServerBoardHelper.scanForBoards(mapSettings));
//                mapSettings.setNullBoards(DEFAULT_BOARD);
//                send(createMapSettingsPacket());
//                send(createMapSizesPacket());
//                checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                break;
            case INITIATIVE:
                // remove the last traces of last round
//                game.handleInitiativeCompensation();
                game().clearActions();
//                game.resetTagInfo();
//                sendTagInfoReset();
                gameManager.clearPendingReports();
//                resetEntityRound();
                resetEntityPhase(game().getPhase());
//                checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                gameManager.resetActivePlayersDone();

                // new round
                gameManager.rollInitiative();

                if (!game().shouldDeployThisRound()) {
                    gameManager.incrementAndSendGameRound();
                    gameManager.getAutoSaveService().performRollingAutosave();
                }

                gameManager.initiativeHelper.determineTurnOrder(game().getPhase());
                gameManager.initiativeHelper.writeInitiativeReport();
//
//                // checks for environmental survival
//                checkForConditionDeath();
//
//                checkForBlueShieldDamage();
//                if (game.getBoard().inAtmosphere()) {
//                    checkForAtmosphereDeath();
//                }
//                if (game.getBoard().inSpace()) {
//                    checkForSpaceDeath();
//                }
//
//                bvReports(true);

                LogManager.getLogger().info("Round {} memory usage: {}",
                        game().getCurrentRound(), MegaMek.getMemoryUsed());
                break;
            case DEPLOY_MINEFIELDS:
//                checkForObservers();
                gameManager.transmitAllPlayerUpdates();
//                resetActivePlayersDone();
//                setIneligible(phase);
                gameManager.initiativeHelper.determineTurnOrder(game().getPhase());
                break;
            case SET_ARTILLERY_AUTOHIT_HEXES:
//                deployOffBoardEntities();
//                checkForObservers();
                gameManager.transmitAllPlayerUpdates();
//                resetActivePlayersDone();
//                setIneligible(phase);
//
//                Enumeration<Player> players = game.getPlayers();
//                Vector<GameTurn> turn = new Vector<>();
//
//                // Walk through the players of the game, and add
//                // a turn for all players with artillery weapons.
//                while (players.hasMoreElements()) {
//                    // Get the next player.
//                    final Player p = players.nextElement();
//
//                    // Does the player have any artillery-equipped units?
//                    EntitySelector playerArtySelector = new EntitySelector() {
//                        private Player owner = p;
//
//                        @Override
//                        public boolean accept(Entity entity) {
//                            return owner.equals(entity.getOwner()) && entity.isEligibleForArtyAutoHitHexes();
//                        }
//                    };
//
//                    if (game.getSelectedEntities(playerArtySelector).hasNext()) {
//                        // Yes, the player has arty-equipped units.
//                        GameTurn gt = new GameTurn(p.getId());
//                        turn.addElement(gt);
//                    }
//                }
//                game.setTurnVector(turn);
                game().resetTurnIndex();
                gameManager.sendCurrentTurns();
                break;
            case PREMOVEMENT:
            case MOVEMENT:
            case DEPLOYMENT:
            case PREFIRING:
            case FIRING:
            case PHYSICAL:
            case TARGETING:
            case OFFBOARD:
                //IO BF p204 offboard is a thing in SBF
//                deployOffBoardEntities();
//
//                // Check for activating hidden units
//                if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) {
//                    for (Entity ent : game.getEntitiesVector()) {
//                        if (ent.getHiddenActivationPhase() == phase) {
//                            ent.setHidden(false);
//                        }
//                    }
//                }
//                // Update visibility indications if using double blind.
//                if (doBlind()) {
//                    updateVisibilityIndicator(null);
//                }
                resetEntityPhase(game().getPhase());
//                checkForObservers();
                gameManager.transmitAllPlayerUpdates();
//                resetActivePlayersDone();
//                setIneligible(phase);
                gameManager.initiativeHelper.determineTurnOrder(game().getPhase());
                gameManager.unitUpdateHelper.sendAllUnitUpdate();
                gameManager.clearPendingReports();
//                doTryUnstuck();
                break;
            case END:
                resetEntityPhase(game().getPhase());
                gameManager.clearPendingReports();
//                resolveHeat();
//                PlanetaryConditions conditions = game.getPlanetaryConditions();
//                if (conditions.isBlowingSandActive()) {
//                    addReport(resolveBlowingSandDamage());
//                }
//                addReport(resolveControlRolls());
//                addReport(checkForTraitors());
//                // write End Phase header
//                addReport(new Report(5005, Report.PUBLIC));
//                addReport(resolveInternalBombHits());
//                checkLayExplosives();
//                resolveHarJelRepairs();
//                resolveEmergencyCoolantSystem();
//                checkForSuffocation();
//                game.getPlanetaryConditions().determineWind();
//                send(packetHelper.createPlanetaryConditionsPacket());
//
//                applyBuildingDamage();
//                addReport(game.ageFlares());
//                send(createFlarePacket());
//                resolveAmmoDumps();
//                resolveCrewWakeUp();
//                resolveConsoleCrewSwaps();
//                resolveSelfDestruct();
//                resolveShutdownCrashes();
//                checkForIndustrialEndOfTurn();
//                resolveMechWarriorPickUp();
//                resolveVeeINarcPodRemoval();
//                resolveFortify();

//                entityStatusReport();
//
//                // Moved this to the very end because it makes it difficult to see
//                // more important updates when you have 300+ messages of smoke filling
//                // whatever hex. Please don't move it above the other things again.
//                // Thanks! Ralgith - 2018/03/15
//                hexUpdateSet.clear();
//                for (DynamicTerrainProcessor tp : terrainProcessors) {
//                    tp.doEndPhaseChanges(vPhaseReport);
//                }
//                sendChangedHexes(hexUpdateSet);
//
//                checkForObservers();
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
//                gameManager.entityAllUpdate();  //TODO really needed in report phase?
                if (game().getOptions().booleanOption(OptionsConstants.BASE_PARANOID_AUTOSAVE)) {
                    gameManager.autoSave();
                }
                break;
            case VICTORY:
//                resetPlayersDone();
                gameManager.clearPendingReports();
//                gameManager.send(gameManager.createAllReportsPacket());
//                prepareVictoryReport();
                gameManager.addPendingReportsToGame();
//                EmailService mailer = Server.getServerInstance().getEmailService();
//                if (mailer != null) {
//                    for (var player: mailer.getEmailablePlayers(game())) {
//                        try {
//                            var message = mailer.newReportMessage(
//                                    game(), gameManager.getPendingReports(), player
//                            );
//                            mailer.send(message);
//                        } catch (Exception ex) {
//                            LogManager.getLogger().error("Error sending email" + ex);
//                        }
//                    }
//                }
//                send(createFullEntitiesPacket());
//                send(createReportPacket(null));
//                send(createEndOfGamePacket());
                break;
            default:
                break;
        }
    }

    private void resetEntityPhase(GamePhase phase) {
        for (InGameObject unit : game().getInGameObjects()) {
            if (unit instanceof SBFFormation formation) {
                formation.setDone(formation.isDone());
            }
        }
    }


}
