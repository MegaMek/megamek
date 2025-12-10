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

import java.util.Vector;
import java.util.stream.Collectors;

import megamek.MegaMek;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.enums.GamePhase;
import megamek.common.game.GameTurn;
import megamek.common.loaders.MapSettings;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.EntitySelector;
import megamek.common.units.FighterSquadron;
import megamek.common.units.IAero;
import megamek.common.util.EmailService;
import megamek.logging.MMLogger;
import megamek.server.DynamicTerrainProcessor;
import megamek.server.Server;
import megamek.server.ServerBoardHelper;

public record TWPhasePreparationManager(TWGameManager gameManager) {
    private static final MMLogger LOGGER = MMLogger.create(TWPhasePreparationManager.class);

    void managePhase() {
        GamePhase phase = gameManager.getGame().getPhase();
        switch (phase) {
            case LOUNGE:
                gameManager.clearReports();
                MapSettings mapSettings = gameManager.getGame().getMapSettings();
                mapSettings.setBoardsAvailableVector(ServerBoardHelper.scanForBoards(mapSettings));
                mapSettings.setNullBoards(TWGameManager.DEFAULT_BOARD);
                gameManager.send(gameManager.createMapSettingsPacket());
                gameManager.send(gameManager.createMapSizesPacket());
                gameManager.checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                break;
            case INITIATIVE:
                // remove the last traces of last round
                gameManager.getGame().handleInitiativeCompensation();
                gameManager.getGame().clearActions();
                gameManager.getGame().resetTagInfo();
                gameManager.sendTagInfoReset();
                gameManager.clearReports();
                gameManager.resetEntityRound();
                gameManager.resetEntityPhase(phase);
                gameManager.checkForObservers();
                gameManager.transmitAllPlayerUpdates();

                // roll 'em
                gameManager.resetActivePlayersDone();
                gameManager.rollInitiative();
                // Cockpit command consoles that switched crew on the previous round are
                // ineligible for force
                // commander initiative bonus. Now that initiative is rolled, clear the flag.
                gameManager.getGame().getEntitiesVector().forEach(e -> e.getCrew().resetActedFlag());

                gameManager.incrementAndSendGameRound();
                gameManager.getAutoSaveService().performRollingAutosave();

                // setIneligible(phase);
                gameManager.determineTurnOrder(phase);
                gameManager.writeInitiativeReport(false);

                // checks for environmental survival
                gameManager.checkForConditionDeath();

                gameManager.checkForBlueShieldDamage();
                gameManager.checkForAtmosphereDeath();
                gameManager.checkForSpaceDeath();

                gameManager.bvReports(true);

                LOGGER.info("Round {} memory usage: {}",
                      gameManager.getGame().getRoundCount(),
                      MegaMek.getMemoryUsed());
                break;
            case DEPLOY_MINEFIELDS:
                gameManager.checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                gameManager.resetActivePlayersDone();
                gameManager.setIneligible(phase);

                if (gameManager.getGame().getBoard().isGround()) {
                    gameManager.getGame().setTurnVector(gameManager.getGame().getPlayersList().stream()
                          .filter(Player::hasMinefields)
                          .map(p -> new GameTurn(p.getId()))
                          .collect(Collectors.toList()));
                }

                gameManager.getGame().resetTurnIndex();
                gameManager.sendCurrentTurns();
                break;
            case SET_ARTILLERY_AUTO_HIT_HEXES:
                gameManager.deployOffBoardEntities();
                gameManager.checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                gameManager.resetActivePlayersDone();
                gameManager.setIneligible(phase);

                Vector<GameTurn> artyTurns = new Vector<>();
                for (Player player : gameManager.getGame().getPlayersList()) {
                    // Does the player have any artillery-equipped units?
                    EntitySelector playerArtySelector = entity -> player.equals(entity.getOwner())
                          && entity.isEligibleForArtyAutoHitHexes();

                    if (gameManager.getGame().getSelectedEntities(playerArtySelector).hasNext()) {
                        artyTurns.addElement(new GameTurn(player.getId()));
                    }
                }
                gameManager.getGame().setTurnVector(artyTurns);
                gameManager.getGame().resetTurnIndex();
                gameManager.sendCurrentTurns();
                break;
            case PREMOVEMENT:
            case MOVEMENT:
            case DEPLOYMENT:
            case PRE_FIRING:
            case FIRING:
            case PHYSICAL:
            case TARGETING:
            case OFFBOARD:
                gameManager.deployOffBoardEntities();

                // Check for activating hidden units
                if (gameManager.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) {
                    for (Entity ent : gameManager.getGame().getEntitiesVector()) {
                        if (ent.getHiddenActivationPhase() == phase) {
                            ent.setHidden(false);
                        }
                    }
                }
                // Update visibility indications if using double-blind.
                if (gameManager.doBlind()) {
                    gameManager.updateVisibilityIndicator(null);
                }
                gameManager.resetEntityPhase(phase);
                gameManager.checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                gameManager.resetActivePlayersDone();
                gameManager.setIneligible(phase);
                gameManager.determineTurnOrder(phase);
                gameManager.entityAllUpdate();
                gameManager.clearReports();
                gameManager.doTryUnstuck();
                break;
            case END:
                gameManager.resetEntityPhase(phase);
                gameManager.clearReports();
                gameManager.heatResolver.resolveHeat();
                gameManager.resolveWeather();
                gameManager.addReport(gameManager.handleFlyOffs());
                gameManager.addReport(gameManager.resolveControlRolls());
                gameManager.addReport(gameManager.checkForTraitors());
                // write End Phase header
                gameManager.addReport(new Report(5005, Report.PUBLIC));
                gameManager.addReport(gameManager.resolveInternalBombHits());
                gameManager.checkLayExplosives();
                gameManager.resolveHarJelRepairs();
                gameManager.resolveEmergencyCoolantSystem();
                gameManager.checkForSuffocation();
                gameManager.getGame().getPlanetaryConditions().determineWind();
                gameManager.send(gameManager.getPacketHelper().createPlanetaryConditionsPacket());

                gameManager.applyBuildingDamage();
                gameManager.addReport(gameManager.getGame().ageFlares());
                gameManager.send(gameManager.createFlarePacket());
                gameManager.resolveAmmoDumps();
                gameManager.resolveCrewWakeUp();
                gameManager.resolveConsoleCrewSwaps();
                gameManager.resolveSelfDestruct();
                gameManager.resolveShutdownCrashes();
                gameManager.checkForIndustrialEndOfTurn();
                gameManager.resolveMekWarriorPickUp();
                gameManager.resolveVeeINarcPodRemoval();
                gameManager.resolveFortify();

                gameManager.entityStatusReport();

                // Moved this to the very end because it makes it difficult to see
                // more important updates when you have 300+ messages of smoke filling
                // whatever hex. Please don't move it above the other things again.
                // Thanks! Ralgith - 2018/03/15
                gameManager.clearHexUpdateSet();
                for (DynamicTerrainProcessor tp : gameManager.getTerrainProcessors()) {
                    tp.doEndPhaseChanges(gameManager.getMainPhaseReport());
                }
                gameManager.sendChangedHexes();

                gameManager.checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                gameManager.entityAllUpdate();
                break;
            case INITIATIVE_REPORT:
                gameManager.autoSave();
                // We need to send out player updates so everyone knows what their initiative is!
                gameManager.transmitAllPlayerUpdates();
            case TARGETING_REPORT:
            case MOVEMENT_REPORT:
            case OFFBOARD_REPORT:
            case FIRING_REPORT:
            case PHYSICAL_REPORT:
            case END_REPORT:
                gameManager.resetActivePlayersDone();
                gameManager.sendReport();
                gameManager.entityAllUpdate();
                if (gameManager.getGame().getOptions().booleanOption(OptionsConstants.BASE_PARANOID_AUTOSAVE)) {
                    gameManager.autoSave();
                }
                break;
            case VICTORY:
                gameManager.resetPlayersDone();
                gameManager.clearReports();
                gameManager.send(gameManager.createAllReportsPacket());
                gameManager.prepareVictoryReport();
                gameManager.getGame().addReports(gameManager.getMainPhaseReport());
                // Before we send the full entities packet we need to loop
                // through the fighters in squadrons and damage them.
                for (Entity entity : gameManager.getGame().getEntitiesVector()) {
                    if ((entity.isFighter()) && !(entity instanceof FighterSquadron)) {
                        if (entity.isPartOfFighterSquadron() || entity.isCapitalFighter()) {
                            ((IAero) entity).doDisbandDamage();
                        }
                    }
                    // fix the armor and SI of aerospace if using aero sanity rules for
                    // the MUL
                    if (gameManager.getGame()
                          .getOptions()
                          .booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)
                          && (entity instanceof Aero a)) {
                        // need to rescale SI and armor
                        int scale = 1;
                        if (entity.isCapitalScale()) {
                            scale = 10;
                        }
                        int currentSI = a.getSI() / (2 * scale);
                        a.setOSI(a.getOSI() / (2 * scale));
                        if (currentSI > 0) {
                            a.setSI(currentSI);
                        }
                        // Fix for #587. MHQ tracks fighters at standard scale and doesn't (currently)
                        // track squadrons. Squadrons don't save to MUL either, so... only convert armor
                        // for JS/WS/SS?
                        // Do we ever need to save capital fighter armor to the final MUL or
                        // entityStatus?
                        if (!entity.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                            scale = 1;
                        }
                        if (scale > 1) {
                            for (int loc = 0; loc < entity.locations(); loc++) {
                                int currentArmor = entity.getArmor(loc) / scale;
                                if (entity.getOArmor(loc) > 0) {
                                    entity.initializeArmor(entity.getOArmor(loc) / scale, loc);
                                }
                                if (entity.getArmor(loc) > 0) {
                                    entity.setArmor(currentArmor, loc);
                                }
                            }
                        }
                    }
                }
                EmailService mailer = Server.getServerInstance().getEmailService();
                if (mailer != null) {
                    for (var player : mailer.getEmailablePlayers(gameManager.getGame())) {
                        try {
                            var message = mailer.newReportMessage(
                                  gameManager.getGame(), gameManager.getMainPhaseReport(), player);
                            mailer.send(message);
                        } catch (Exception ex) {
                            LOGGER.error("Error sending email{}", String.valueOf(ex));
                        }
                    }
                }
                gameManager.send(gameManager.createFullEntitiesPacket());
                gameManager.send(gameManager.createReportPacket(null));
                gameManager.send(gameManager.createEndOfGamePacket());
                break;
            default:
                break;
        }
    }
}
