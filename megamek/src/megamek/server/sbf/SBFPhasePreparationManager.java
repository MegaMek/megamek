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
import megamek.common.enums.GamePhase;
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
                // MapSettings mapSettings = game.getMapSettings();
                // mapSettings.setBoardsAvailableVector(ServerBoardHelper.scanForBoards(mapSettings));
                // mapSettings.setNullBoards(DEFAULT_BOARD);
                // send(createMapSettingsPacket());
                // send(createMapSizesPacket());
                // checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                break;
            case INITIATIVE:
                // remove the last traces of last round
                // game.handleInitiativeCompensation();
                game().clearActions();
                // game.resetTagInfo();
                // sendTagInfoReset();
                gameManager.clearPendingReports();
                // resetEntityRound();
                resetEntityPhase(game().getPhase());
                // checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                gameManager.resetActivePlayersDone();

                // new round
                gameManager.rollInitiative();

                gameManager.incrementAndSendGameRound();
                gameManager.getAutoSaveService().performRollingAutosave();

                gameManager.initiativeHelper.determineTurnOrder(game().getPhase());
                gameManager.initiativeHelper.writeInitiativeReport();
                //
                // // checks for environmental survival
                // checkForConditionDeath();
                //
                // checkForBlueShieldDamage();
                // if (game.getBoard().inAtmosphere()) {
                // checkForAtmosphereDeath();
                // }
                // if (game.getBoard().inSpace()) {
                // checkForSpaceDeath();
                // }
                //
                // bvReports(true);

                logger.info("Round {} memory usage: {}",
                        game().getCurrentRound(), MegaMek.getMemoryUsed());
                break;
            case DEPLOY_MINEFIELDS:
                // checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                // resetActivePlayersDone();
                // setIneligible(phase);
                gameManager.initiativeHelper.determineTurnOrder(game().getPhase());
                break;
            case SET_ARTILLERY_AUTOHIT_HEXES:
                // deployOffBoardEntities();
                // checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                game().resetTurnIndex();
                gameManager.sendCurrentTurns();
                break;
            case MOVEMENT:
                // doTryUnstuck();
            case PREMOVEMENT:
            case DEPLOYMENT:
            case PREFIRING:
            case FIRING:
            case PHYSICAL:
            case TARGETING:
            case OFFBOARD:
                // IO BF p204 offboard is a thing in SBF
                // deployOffBoardEntities();
                //
                // // Check for activating hidden units
                // if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS))
                // {
                // for (Entity ent : game.getEntitiesVector()) {
                // if (ent.getHiddenActivationPhase() == phase) {
                // ent.setHidden(false);
                // }
                // }
                // }
                resetEntityPhase(game().getPhase());
                // checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                // resetActivePlayersDone();
                // setIneligible(phase);
                gameManager.initiativeHelper.determineTurnOrder(game().getPhase());
                gameManager.unitUpdateHelper.sendAllUnitUpdate();
                gameManager.clearPendingReports();
                break;
            case END:
                resetEntityPhase(game().getPhase());
                gameManager.clearPendingReports();
                // resolveHeat();
                // PlanetaryConditions conditions = game.getPlanetaryConditions();
                // if (conditions.isBlowingSandActive()) {
                // addReport(resolveBlowingSandDamage());
                // }
                // addReport(resolveControlRolls());
                // addReport(checkForTraitors());
                // // write End Phase header
                // addReport(new Report(5005, Report.PUBLIC));
                // addReport(resolveInternalBombHits());
                // checkLayExplosives();
                // resolveHarJelRepairs();
                // resolveEmergencyCoolantSystem();
                // checkForSuffocation();
                // game.getPlanetaryConditions().determineWind();
                // send(packetHelper.createPlanetaryConditionsPacket());
                //
                // applyBuildingDamage();
                // addReport(game.ageFlares());
                // send(createFlarePacket());
                // resolveAmmoDumps();
                // resolveCrewWakeUp();
                // resolveConsoleCrewSwaps();
                // resolveSelfDestruct();
                // resolveShutdownCrashes();
                // checkForIndustrialEndOfTurn();
                // resolveMekWarriorPickUp();
                // resolveVeeINarcPodRemoval();
                // resolveFortify();

                // entityStatusReport();
                //
                // // Moved this to the very end because it makes it difficult to see
                // // more important updates when you have 300+ messages of smoke filling
                // // whatever hex. Please don't move it above the other things again.
                // // Thanks! Ralgith - 2018/03/15
                // hexUpdateSet.clear();
                // for (DynamicTerrainProcessor tp : terrainProcessors) {
                // tp.doEndPhaseChanges(vPhaseReport);
                // }
                // sendChangedHexes(hexUpdateSet);
                //
                // checkForObservers();
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
                // gameManager.entityAllUpdate(); //TODO really needed in report phase?
                if (game().getOptions().booleanOption(OptionsConstants.BASE_PARANOID_AUTOSAVE)) {
                    gameManager.autoSave();
                }
                break;
            case VICTORY:
                // resetPlayersDone();
                gameManager.clearPendingReports();
                // gameManager.send(gameManager.createAllReportsPacket());
                // prepareVictoryReport();
                gameManager.addPendingReportsToGame();
                // EmailService mailer = Server.getServerInstance().getEmailService();
                // if (mailer != null) {
                // for (var player: mailer.getEmailablePlayers(game())) {
                // try {
                // var message = mailer.newReportMessage(
                // game(), gameManager.getPendingReports(), player
                // );
                // mailer.send(message);
                // } catch (Exception ex) {
                // logger.error("Error sending email" + ex);
                // }
                // }
                // }
                // send(createFullEntitiesPacket());
                // send(createReportPacket(null));
                // send(createEndOfGamePacket());
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
