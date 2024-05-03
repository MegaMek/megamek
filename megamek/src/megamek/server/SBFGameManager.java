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
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.server.commands.ServerCommand;
import org.apache.logging.log4j.LogManager;

import java.util.*;

/**
 * This class manages an SBF game on the server side. As of 2024, this is under construction.
 */
public final class SBFGameManager extends AbstractGameManager {

    private SBFGame game;

    @Override
    public IGame getGame() {
        return game;
    }

    @Override
    public void setGame(IGame g) {
        if (!(g instanceof SBFGame)) {
            LogManager.getLogger().error("Attempted to set game to incorrect class.");
            return;
        }
        game = (SBFGame) g;
    }

    @Override
    public void resetGame() {

    }

    @Override
    public void disconnect(Player player) {

    }

    @Override
    public void saveGame(String fileName) {

    }

    @Override
    public void sendSaveGame(int connId, String fileName, String localPath) {

    }

    @Override
    public void removeAllEntitiesOwnedBy(Player player) {

    }

    @Override
    public void handleCfrPacket(Server.ReceivedPacket rp) {

    }

    @Override
    public void requestGameMaster(Player player) {

    }

    @Override
    public void requestTeamChange(int teamId, Player player) {

    }

    @Override
    public List<ServerCommand> getCommandList(Server server) {
        return Collections.emptyList();
    }

    @Override
    public void addReport(Report r) {

    }

    @Override
    public void calculatePlayerInitialCounts() {

    }

    @Override
    public void sendCurrentInfo(int connId) {
        send(connId, packetHelper.createGameSettingsPacket());

        Player player = getGame().getPlayer(connId);
        if (null != player) {
            send(connId, new Packet(PacketCommand.SENDING_MINEFIELDS, player.getMinefields()));

            if (getGame().getPhase().isLounge()) {
//                send(connId, createMapSettingsPacket());
//                send(createMapSizesPacket());
                // LOUNGE triggers a Game.reset() on the client!
                // Send Entities *after* the Lounge Phase Change
                send(connId, packetHelper.createPhaseChangePacket());
//                if (doBlind()) {
//                    send(connId, createFilteredFullEntitiesPacket(player, null));
//                } else {
//                    send(connId, createFullEntitiesPacket());
//                }
            } else {
                send(connId, packetHelper.createCurrentRoundNumberPacket());
                send(connId, packetHelper.createBoardsPacket());
//                send(connId, createAllReportsPacket(player));
//
//                // Send entities *before* other phase changes.
//                if (doBlind()) {
//                    send(connId, createFilteredFullEntitiesPacket(player, null));
//                } else {
//                    send(connId, createFullEntitiesPacket());
//                }
//
//                setPlayerDone(player, getGame().getEntitiesOwnedBy(player) <= 0);
                send(connId, packetHelper.createPhaseChangePacket());
            }

            send(connId, packetHelper.createPlanetaryConditionsPacket());
//
            if (game.getPhase().isFiring() || game.getPhase().isTargeting()
                    || game.getPhase().isOffboard() || game.getPhase().isPhysical()) {
                // can't go above, need board to have been sent
//                send(connId, packetHelper.createAttackPacket(getGame().getActionsVector(), false));
//                send(connId, packetHelper.createAttackPacket(getGame().getChargesVector(), true));
//                send(connId, packetHelper.createAttackPacket(getGame().getRamsVector(), true));
//                send(connId, packetHelper.createAttackPacket(getGame().getTeleMissileAttacksVector(), true));
            }
//
            if (getGame().getPhase().usesTurns() && getGame().hasMoreTurns()) {
//                send(connId, createTurnVectorPacket());
//                send(connId, createTurnIndexPacket(connId));
            } else if (!getGame().getPhase().isLounge() && !getGame().getPhase().isStartingScenario()) {
                endCurrentPhase();
            }
//
//            send(connId, createArtilleryPacket(player));
//            send(connId, createFlarePacket());
//            send(connId, createSpecialHexDisplayPacket(connId));
//            send(connId, new Packet(PacketCommand.PRINCESS_SETTINGS, getGame().getBotSettings()));
        }
    }

    @Override
    protected void endCurrentPhase() {
        switch (game.getPhase()) {
            case LOUNGE:
//                game.addReports(vPhaseReport);
                changePhase(GamePhase.EXCHANGE);
                break;
            case EXCHANGE:
            case STARTING_SCENARIO:
//                game.addReports(vPhaseReport);
                // IO BF p.103:
//                changePhase(GamePhase.SET_ARTILLERY_AUTOHIT_HEXES);
                changePhase(GamePhase.INITIATIVE); // <- only for testing to get past arty auto and minefields
                break;
            case SET_ARTILLERY_AUTOHIT_HEXES:
//                sendSpecialHexDisplayPackets();
                boolean mines = game.getPlayersList().stream().anyMatch(Player::hasMinefields);
//                game.addReports(vPhaseReport);
                if (mines) {
                    changePhase(GamePhase.DEPLOY_MINEFIELDS);
                } else {
                    changePhase(GamePhase.INITIATIVE);
                }
                break;
            case DEPLOY_MINEFIELDS:
                changePhase(GamePhase.INITIATIVE);
                break;
            case DEPLOYMENT:
                game.clearDeploymentThisRound();
                if (game.getCurrentRound() == 0) {
                    changePhase(GamePhase.INITIATIVE);
                } else {
                    // there is no separate artillery targeting phase in SBF
                    changePhase(GamePhase.TARGETING);
                }
                break;
            case INITIATIVE:
//                resolveWhatPlayersCanSeeWhatUnits();
//                detectSpacecraft();
//                game.addReports(vPhaseReport);
                changePhase(GamePhase.INITIATIVE_REPORT);
                break;
            case INITIATIVE_REPORT:
                game.setupRoundDeployment();
                if (game.shouldDeployThisRound()) {
                    changePhase(GamePhase.DEPLOYMENT);
                } else {
                    changePhase(GamePhase.TARGETING);
                }
                break;
            case PREMOVEMENT:
                changePhase(GamePhase.MOVEMENT);
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
//                    game.addReports(vPhaseReport);
//                    changePhase(GamePhase.MOVEMENT_REPORT);
//                } else {
//                    // just the header, so we'll add the <nothing> label
//                    addReport(new Report(1205, Report.PUBLIC));
//                    game.addReports(vPhaseReport);
//                    sendReport();
//                    changePhase(GamePhase.OFFBOARD);
//                }
                break;
            case MOVEMENT_REPORT:
                changePhase(GamePhase.OFFBOARD);
                break;
            case PREFIRING:
                changePhase(GamePhase.FIRING);
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
//                    game.addReports(vPhaseReport);
//                    changePhase(GamePhase.FIRING_REPORT);
//                } else {
//                    // just the header, so we'll add the <nothing> label
//                    addReport(new Report(1205, Report.PUBLIC));
//                    sendReport();
//                    game.addReports(vPhaseReport);
//                    changePhase(GamePhase.PHYSICAL);
//                }
                break;
            case FIRING_REPORT:
                changePhase(GamePhase.PHYSICAL);
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
//                    game.addReports(vPhaseReport);
//                    changePhase(GamePhase.PHYSICAL_REPORT);
//                } else {
//                    // just the header, so we'll add the <nothing> label
//                    addReport(new Report(1205, Report.PUBLIC));
//                    game.addReports(vPhaseReport);
//                    sendReport();
//                    changePhase(GamePhase.END);
//                }
                break;
            case PHYSICAL_REPORT:
                changePhase(GamePhase.END);
                break;
            case TARGETING:
//                vPhaseReport.addElement(new Report(1035, Report.PUBLIC));
//                resolveAllButWeaponAttacks();
//                resolveOnlyWeaponAttacks();
//                handleAttacks();
//                // check reports
//                if (vPhaseReport.size() > 1) {
//                    game.addReports(vPhaseReport);
//                    changePhase(GamePhase.TARGETING_REPORT);
//                } else {
//                    // just the header, so we'll add the <nothing> label
//                    vPhaseReport.addElement(new Report(1205, Report.PUBLIC));
//                    game.addReports(vPhaseReport);
//                    sendReport();
//                    changePhase(GamePhase.PREMOVEMENT);
//                }
//
//                sendSpecialHexDisplayPackets();
//                for (Enumeration<Player> i = game.getPlayers(); i.hasMoreElements(); ) {
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
//                for (Enumeration<Player> i = game.getPlayers(); i.hasMoreElements(); ) {
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
//                    game.addReports(vPhaseReport);
//                    changePhase(GamePhase.OFFBOARD_REPORT);
//                } else {
//                    // just the header, so we'll add the <nothing> label
//                    addReport(new Report(1205, Report.PUBLIC));
//                    game.addReports(vPhaseReport);
//                    sendReport();
//                    changePhase(GamePhase.PREFIRING);
//                }
                break;
            case OFFBOARD_REPORT:
//                sendSpecialHexDisplayPackets();
                changePhase(GamePhase.PREFIRING);
                break;
            case TARGETING_REPORT:
                changePhase(GamePhase.PREMOVEMENT);
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
//                    game.addReports(vPhaseReport);
//                    changePhase(GamePhase.END_REPORT);
//                } else {
//                    // just the heat and end headers, so we'll add
//                    // the <nothing> label
//                    addReport(new Report(1205, Report.PUBLIC));
//                    game.addReports(vPhaseReport);
//                    sendReport();
//                    if (victory) {
//                        changePhase(GamePhase.VICTORY);
//                    } else {
//                        changePhase(GamePhase.INITIATIVE);
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
//                    changePhase(GamePhase.VICTORY);
//                } else {
//                    changePhase(GamePhase.INITIATIVE);
//                }
                break;
            case VICTORY:
//                GameVictoryEvent gve = new GameVictoryEvent(this, game);
//                game.processGameEvent(gve);
//                transmitGameVictoryEventToAll();
//                resetGame();
                break;
            default:
                break;
        }
    }

    @Override
    protected void changePhase(GamePhase newPhase) {
        game.setLastPhase(game.getPhase());
        game.setPhase(newPhase);

        prepareForCurrentPhase();

        if (game.shouldSkipCurrentPhase()) {
            endCurrentPhase();
        } else {
            // tell the players about the new phase
            send(packetHelper.createPhaseChangePacket());

            executePhase(newPhase);
        }
    }

    /**
     * Prepares for, presumably, the next phase. This typically involves
     * resetting the states of entities in the game and making sure the client
     * has the information it needs for the new phase.
     *
     * @param phase the <code>int</code> id of the phase to prepare for
     */
    private void prepareForCurrentPhase() {
        switch (game.getPhase()) {
            case LOUNGE:
//                clearReports();
//                MapSettings mapSettings = game.getMapSettings();
//                mapSettings.setBoardsAvailableVector(ServerBoardHelper.scanForBoards(mapSettings));
//                mapSettings.setNullBoards(DEFAULT_BOARD);
//                send(createMapSettingsPacket());
//                send(createMapSizesPacket());
//                checkForObservers();
//                transmitAllPlayerUpdates();
                break;
            case INITIATIVE:
                // remove the last traces of last round
//                game.handleInitiativeCompensation();
                game.clearActions();
//                game.resetTagInfo();
//                sendTagInfoReset();
//                clearReports();
//                resetEntityRound();
//                resetEntityPhase(phase);
//                checkForObservers();
//                transmitAllPlayerUpdates();

                // roll 'em
                resetActivePlayersDone();
                rollInitiative();
                //Cockpit command consoles that switched crew on the previous round are ineligible for force
                // commander initiative bonus. Now that initiative is rolled, clear the flag.
//                game.getEntities().forEachRemaining(e -> e.getCrew().resetActedFlag());

                if (!game.shouldDeployThisRound()) {
//                    incrementAndSendGameRound();
//                    asService.performRollingAutosave(this);
                }

                // setIneligible(phase);
//                determineTurnOrder(phase);
//                writeInitiativeReport(false);
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

                LogManager.getLogger().info("Round {} memory usage: {}", game.getCurrentRound(), MegaMek.getMemoryUsed());
                break;
            case DEPLOY_MINEFIELDS:
//                checkForObservers();
//                transmitAllPlayerUpdates();
//                resetActivePlayersDone();
//                setIneligible(phase);
//
//                Enumeration<Player> e = game.getPlayers();
//                Vector<GameTurn> turns = new Vector<>();
//                while (e.hasMoreElements()) {
//                    Player p = e.nextElement();
//                    if (p.hasMinefields() && game.getBoard().onGround()) {
//                        GameTurn gt = new GameTurn(p.getId());
//                        turns.addElement(gt);
//                    }
//                }
//                game.setTurnVector(turns);
//                game.resetTurnIndex();
//
//                // send turns to all players
//                send(createTurnVectorPacket());
                break;
            case SET_ARTILLERY_AUTOHIT_HEXES:
//                deployOffBoardEntities();
//                checkForObservers();
//                transmitAllPlayerUpdates();
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
//                game.resetTurnIndex();
//
//                // send turns to all players
//                send(createTurnVectorPacket());
                break;
            case PREMOVEMENT:
            case MOVEMENT:
            case DEPLOYMENT:
            case PREFIRING:
            case FIRING:
            case PHYSICAL:
            case TARGETING:
            case OFFBOARD:
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
//                resetEntityPhase(phase);
//                checkForObservers();
//                transmitAllPlayerUpdates();
//                resetActivePlayersDone();
//                setIneligible(phase);
//                determineTurnOrder(phase);
//                entityAllUpdate();
//                clearReports();
//                doTryUnstuck();
                break;
            case END:
//                resetEntityPhase(phase);
//                clearReports();
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
//                transmitAllPlayerUpdates();
//                entityAllUpdate();
                break;
            case INITIATIVE_REPORT: {
//                autoSave();
            }
            case TARGETING_REPORT:
            case MOVEMENT_REPORT:
            case OFFBOARD_REPORT:
            case FIRING_REPORT:
            case PHYSICAL_REPORT:
            case END_REPORT:
//                resetActivePlayersDone();
//                sendReport();
//                entityAllUpdate();
//                if (game.getOptions().booleanOption(OptionsConstants.BASE_PARANOID_AUTOSAVE)) {
//                    autoSave();
//                }
                break;
            case VICTORY:
//                resetPlayersDone();
//                clearReports();
//                send(createAllReportsPacket());
//                prepareVictoryReport();
//                game.addReports(vPhaseReport);
//                // Before we send the full entities packet we need to loop
//                // through the fighters in squadrons and damage them.
//                for (Iterator<Entity> ents = game.getEntities(); ents.hasNext(); ) {
//                    Entity entity = ents.next();
//                    if ((entity.isFighter()) && !(entity instanceof FighterSquadron)) {
//                        if (entity.isPartOfFighterSquadron() || entity.isCapitalFighter()) {
//                            ((IAero) entity).doDisbandDamage();
//                        }
//                    }
//                    // fix the armor and SI of aeros if using aero sanity rules for
//                    // the MUL
//                    if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)
//                            && (entity instanceof Aero)) {
//                        // need to rescale SI and armor
//                        int scale = 1;
//                        if (entity.isCapitalScale()) {
//                            scale = 10;
//                        }
//                        Aero a = (Aero) entity;
//                        int currentSI = a.getSI() / (2 * scale);
//                        a.set0SI(a.get0SI() / (2 * scale));
//                        if (currentSI > 0) {
//                            a.setSI(currentSI);
//                        }
//                        //Fix for #587. MHQ tracks fighters at standard scale and doesn't (currently)
//                        //track squadrons. Squadrons don't save to MUL either, so... only convert armor for JS/WS/SS?
//                        //Do we ever need to save capital fighter armor to the final MUL or entityStatus?
//                        if (!entity.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
//                            scale = 1;
//                        }
//                        if (scale > 1) {
//                            for (int loc = 0; loc < entity.locations(); loc++) {
//                                int currentArmor = entity.getArmor(loc) / scale;
//                                if (entity.getOArmor(loc) > 0) {
//                                    entity.initializeArmor(entity.getOArmor(loc) / scale, loc);
//                                }
//                                if (entity.getArmor(loc) > 0) {
//                                    entity.setArmor(currentArmor, loc);
//                                }
//                            }
//                        }
//                    }
//                }
//                EmailService mailer = Server.getServerInstance().getEmailService();
//                if (mailer != null) {
//                    for (var player: mailer.getEmailablePlayers(game)) {
//                        try {
//                            var message = mailer.newReportMessage(
//                                    game, vPhaseReport, player
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

    /**
     * Do anything we seed to start the new phase, such as give a turn to the
     * first player to play.
     */
    private void executePhase(GamePhase phase) {
        switch (phase) {
            case EXCHANGE:
//                resetPlayersDone();
//                // Update initial BVs, as things may have been modified in lounge
//                for (Entity e : game.getEntitiesVector()) {
//                    e.setInitialBV(e.calculateBattleValue(false, false));
//                }
//                calculatePlayerInitialCounts();
//                // Build teams vector
//                game.setupTeams();
//                applyBoardSettings();
//                game.getPlanetaryConditions().determineWind();
//                send(packetHelper.createPlanetaryConditionsPacket());
//                // transmit the board to everybody
//                send(packetHelper.createBoardsPacket());
//                game.setupRoundDeployment();
//                game.setVictoryContext(new HashMap<>());
//                game.createVictoryConditions();
//                // some entities may need to be checked and updated
//                checkEntityExchange();
                break;
            case MOVEMENT:
                // write Movement Phase header to report
                addReport(new Report(2000, Report.PUBLIC));
            case PREMOVEMENT:
            case SET_ARTILLERY_AUTOHIT_HEXES:
            case DEPLOY_MINEFIELDS:
            case DEPLOYMENT:
            case PREFIRING:
            case FIRING:
            case PHYSICAL:
            case TARGETING:
            case OFFBOARD:
//                changeToNextTurn(-1);
//                if (game.getOptions().booleanOption(OptionsConstants.BASE_PARANOID_AUTOSAVE)) {
//                    autoSave();
//                }
                break;
            default:
                break;
        }
    }

    /**
     * Called at the beginning of certain phases to make every player not ready.
     */
    private void resetPlayersDone() {
        //FIXME This is highly unclear why not in report but in victory
        if ((getGame().getPhase().isReport()) && (!getGame().getPhase().isVictory())) {
            return;
        }

        for (Player player : game.getPlayersList()) {
            setPlayerDone(player, false);
        }

        transmitAllPlayerDones();
    }

    private void setPlayerDone(Player player, boolean normalDone) {
        //FIXME This is highly specialized and very arcane!!
        if (getGame().getPhase().isReport()
                && getGame().getOptions().booleanOption(OptionsConstants.BASE_GM_CONTROLS_DONE_REPORT_PHASE)
                && getGame().getPlayersList().stream().filter(p -> p.isGameMaster()).count() > 0) {
            if (player.isGameMaster()) {
                player.setDone(false);
            } else {
                player.setDone(true);
            }
        } else {
            player.setDone(normalDone);
        }
    }

    /**
     * Called at the beginning of certain phases to make every active player not
     * ready.
     */
    private void resetActivePlayersDone() {
        for (Player player : game.getPlayersList()) {
            //FIXME This is highly specialized and very arcane!!
            setPlayerDone(player, getGame().getEntitiesOwnedBy(player) <= 0);
        }
        transmitAllPlayerDones();
    }

    /**
     * Rolls initiative for all teams.
     */
    private void rollInitiative() {
        TurnOrdered.rollInitiative(game.getTeams(), false);
        transmitAllPlayerUpdates();
    }
}
