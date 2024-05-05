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

import megamek.MegaMek;
import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.util.EmailService;
import org.apache.logging.log4j.LogManager;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

public class TWPhasePreparationManager {

    private final GameManager gameManager;

    public TWPhasePreparationManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    void managePhase() {
        GamePhase phase = gameManager.getGame().getPhase();
        switch (phase) {
            case LOUNGE:
                gameManager.clearReports();
                MapSettings mapSettings = gameManager.getGame().getMapSettings();
                mapSettings.setBoardsAvailableVector(ServerBoardHelper.scanForBoards(mapSettings));
                mapSettings.setNullBoards(GameManager.DEFAULT_BOARD);
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
                //Cockpit command consoles that switched crew on the previous round are ineligible for force
                // commander initiative bonus. Now that initiative is rolled, clear the flag.
                gameManager.getGame().getEntities().forEachRemaining(e -> e.getCrew().resetActedFlag());

                if (!gameManager.getGame().shouldDeployThisRound()) {
                    gameManager.incrementAndSendGameRound();
                    gameManager.autoSaveService.performRollingAutosave();
                }

                // setIneligible(phase);
                gameManager.determineTurnOrder(phase);
                gameManager.writeInitiativeReport(false);

                // checks for environmental survival
                gameManager.checkForConditionDeath();

                gameManager.checkForBlueShieldDamage();
                if (gameManager.getGame().getBoard().inAtmosphere()) {
                    gameManager.checkForAtmosphereDeath();
                }
                if (gameManager.getGame().getBoard().inSpace()) {
                    gameManager.checkForSpaceDeath();
                }

                gameManager.bvReports(true);

                LogManager.getLogger().info("Round " + gameManager.getGame().getRoundCount() + " memory usage: " + MegaMek.getMemoryUsed());
                break;
            case DEPLOY_MINEFIELDS:
                gameManager.checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                gameManager.resetActivePlayersDone();
                gameManager.setIneligible(phase);

                Enumeration<Player> e = gameManager.getGame().getPlayers();
                Vector<GameTurn> turns = new Vector<>();
                while (e.hasMoreElements()) {
                    Player p = e.nextElement();
                    if (p.hasMinefields() && gameManager.getGame().getBoard().onGround()) {
                        GameTurn gt = new GameTurn(p.getId());
                        turns.addElement(gt);
                    }
                }
                gameManager.getGame().setTurnVector(turns);
                gameManager.getGame().resetTurnIndex();
                gameManager.sendCurrentTurns();
                break;
            case SET_ARTILLERY_AUTOHIT_HEXES:
                gameManager.deployOffBoardEntities();
                gameManager.checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                gameManager.resetActivePlayersDone();
                gameManager.setIneligible(phase);

                Enumeration<Player> players = gameManager.getGame().getPlayers();
                Vector<GameTurn> turn = new Vector<>();

                // Walk through the players of the game, and add
                // a turn for all players with artillery weapons.
                while (players.hasMoreElements()) {
                    // Get the next player.
                    final Player p = players.nextElement();

                    // Does the player have any artillery-equipped units?
                    EntitySelector playerArtySelector = new EntitySelector() {
                        private Player owner = p;

                        @Override
                        public boolean accept(Entity entity) {
                            return owner.equals(entity.getOwner()) && entity.isEligibleForArtyAutoHitHexes();
                        }
                    };

                    if (gameManager.getGame().getSelectedEntities(playerArtySelector).hasNext()) {
                        // Yes, the player has arty-equipped units.
                        GameTurn gt = new GameTurn(p.getId());
                        turn.addElement(gt);
                    }
                }
                gameManager.getGame().setTurnVector(turn);
                gameManager.getGame().resetTurnIndex();
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
                gameManager.deployOffBoardEntities();

                // Check for activating hidden units
                if (gameManager.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) {
                    for (Entity ent : gameManager.getGame().getEntitiesVector()) {
                        if (ent.getHiddenActivationPhase() == phase) {
                            ent.setHidden(false);
                        }
                    }
                }
                // Update visibility indications if using double blind.
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
                gameManager.resolveHeat();
                PlanetaryConditions conditions = gameManager.getGame().getPlanetaryConditions();
                if (conditions.isBlowingSandActive()) {
                    gameManager.addReport(gameManager.resolveBlowingSandDamage());
                }
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
                gameManager.send(gameManager.packetHelper.createPlanetaryConditionsPacket());

                gameManager.applyBuildingDamage();
                gameManager.addReport(gameManager.getGame().ageFlares());
                gameManager.send(gameManager.createFlarePacket());
                gameManager.resolveAmmoDumps();
                gameManager.resolveCrewWakeUp();
                gameManager.resolveConsoleCrewSwaps();
                gameManager.resolveSelfDestruct();
                gameManager.resolveShutdownCrashes();
                gameManager.checkForIndustrialEndOfTurn();
                gameManager.resolveMechWarriorPickUp();
                gameManager.resolveVeeINarcPodRemoval();
                gameManager.resolveFortify();

                gameManager.entityStatusReport();

                // Moved this to the very end because it makes it difficult to see
                // more important updates when you have 300+ messages of smoke filling
                // whatever hex. Please don't move it above the other things again.
                // Thanks! Ralgith - 2018/03/15
                gameManager.clearHexUpdateSet();
                for (DynamicTerrainProcessor tp : gameManager.getTerrainProcessors()) {
                    tp.doEndPhaseChanges(gameManager.getvPhaseReport());
                }
                gameManager.sendChangedHexes();

                gameManager.checkForObservers();
                gameManager.transmitAllPlayerUpdates();
                gameManager.entityAllUpdate();
                break;
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
                gameManager.getGame().addReports(gameManager.getvPhaseReport());
                // Before we send the full entities packet we need to loop
                // through the fighters in squadrons and damage them.
                for (Iterator<Entity> ents = gameManager.getGame().getEntities(); ents.hasNext(); ) {
                    Entity entity = ents.next();
                    if ((entity.isFighter()) && !(entity instanceof FighterSquadron)) {
                        if (entity.isPartOfFighterSquadron() || entity.isCapitalFighter()) {
                            ((IAero) entity).doDisbandDamage();
                        }
                    }
                    // fix the armor and SI of aeros if using aero sanity rules for
                    // the MUL
                    if (gameManager.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)
                            && (entity instanceof Aero)) {
                        // need to rescale SI and armor
                        int scale = 1;
                        if (entity.isCapitalScale()) {
                            scale = 10;
                        }
                        Aero a = (Aero) entity;
                        int currentSI = a.getSI() / (2 * scale);
                        a.set0SI(a.get0SI() / (2 * scale));
                        if (currentSI > 0) {
                            a.setSI(currentSI);
                        }
                        //Fix for #587. MHQ tracks fighters at standard scale and doesn't (currently)
                        //track squadrons. Squadrons don't save to MUL either, so... only convert armor for JS/WS/SS?
                        //Do we ever need to save capital fighter armor to the final MUL or entityStatus?
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
                    for (var player: mailer.getEmailablePlayers(gameManager.getGame())) {
                        try {
                            var message = mailer.newReportMessage(
                                    gameManager.getGame(), gameManager.getvPhaseReport(), player
                            );
                            mailer.send(message);
                        } catch (Exception ex) {
                            LogManager.getLogger().error("Error sending email" + ex);
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
