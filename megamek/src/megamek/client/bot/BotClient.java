/*
 * MegaMek - Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 */
package megamek.client.bot;

import megamek.client.Client;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.ReportDisplay;
import megamek.common.*;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.event.*;
import megamek.common.net.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.pathfinder.BoardClusterTracker;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.BoardUtilities;
import megamek.common.util.StringUtil;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.io.*;
import java.util.*;

public abstract class BotClient extends Client {
    public static final int BOT_TURN_RETRY_COUNT = 3;

    private List<Entity> currentTurnEnemyEntities;
    private List<Entity> currentTurnFriendlyEntities;
    
    // a frame, to show stuff in
    public JFrame frame;
    
    /**
     * Keeps track of whether this client has started to calculate a turn this phase.
     */
    boolean calculatedTurnThisPhase = false;
    int calculatedTurnsThisPhase = 0;

    /**
     * Store a reference to the ClientGUI for the client who created this bot.
     * This is used to ensure keep the ClientGUI synchronized with changes to
     * this BotClient (particularly the bot's name).
     */
    private ClientGUI clientgui = null;

    public class CalculateBotTurn implements Runnable {
        @Override
        public void run() {
            calculateMyTurn();
            flushConn();
        }
    }

    public BotClient(String playerName, String host, int port) {
        super(playerName, host, port);
        
        boardClusterTracker = new BoardClusterTracker();
        
        game.addGameListener(new GameListenerAdapter() {

            @Override
            public void gamePlayerChat(GamePlayerChatEvent e) {
                processChat(e);
                flushConn();
            }

            @Override
            public void gameTurnChange(GameTurnChangeEvent e) {
                // On simultaneous phases, each player ending their turn will generate a turn change
                // We want to ignore turns from other players and only listen to events we generated
                boolean ignoreSimTurn = getGame().getPhase().isSimultaneous(getGame())
                        && (e.getPreviousPlayerId() != localPlayerNumber)
                        && calculatedTurnThisPhase;

                if (isMyTurn() && !ignoreSimTurn) {
                    calculatedTurnThisPhase = true;
                    // Run bot's turn processing in a separate thread.
                    // So calling thread is free to process the other actions.
                    Thread worker = new Thread(new CalculateBotTurn(),
                            getName() + " Turn " + game.getTurnIndex() + " Calc Thread"
                    );
                    worker.start();
                    calculatedTurnsThisPhase++;
                }

                // unloading "stranded" units happens as part of a game turn change, so that's where we do it.
                if (canUnloadStranded()) {
                    sendUnloadStranded(getStrandedEntities());
                }
            }

            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                calculatedTurnThisPhase = false;
                if (e.getOldPhase().isSimultaneous(getGame())) {
                    int numOwnedEntities = game.getEntitiesOwnedBy(getLocalPlayer());
                    System.out.println("BotClient calculated turns, " + getName() + " phase " + e.getOldPhase()
                            + " " + calculatedTurnsThisPhase + "/" + numOwnedEntities);
                }
                calculatedTurnsThisPhase = 0;
            }

            @Override
            public void gameReport(GameReportEvent e) {
                if (game.getPhase() == GamePhase.INITIATIVE_REPORT) {
                    // Opponent has used tactical genius, must press
                    // "Done" again to advance past initiative report.
                    sendDone(true);
                    flushConn();
                }
            }

            @Override
            public void gameClientFeedbackRequest(GameCFREvent evt) {
                switch (evt.getCFRType()) {
                    case Packet.COMMAND_CFR_DOMINO_EFFECT:
                        // This will always send a "no action" response.
                        // In effect, it works the way it did before.  However..
                        // TODO: Bots should figure out how to step out of a
                        //   domino effect
                        sendDominoCFRResponse(null);
                        break;
                    case Packet.COMMAND_CFR_AMS_ASSIGN:
                        // Picks the WAA with the highest expected damage,
                        //  essentially same as if the auto_ams option was on
                        WeaponAttackAction waa = Compute.getHighestExpectedDamage(game, evt.getWAAs(), true);
                        sendAMSAssignCFRResponse(evt.getWAAs().indexOf(waa));
                        break;
                    case Packet.COMMAND_CFR_APDS_ASSIGN:
                        // Picks the WAA with the highest expected damage,
                        //  essentially same as if the auto_ams option was on
                        waa =
                            Compute.getHighestExpectedDamage(game,
                                    evt.getWAAs(), true);
                        sendAPDSAssignCFRResponse(evt.getWAAs().indexOf(waa));
                        break;
                    case Packet.COMMAND_CFR_HIDDEN_PBS:
                        try {
                            Vector<EntityAction> pointBlankShots = calculatePointBlankShot(evt.getEntityId(), evt.getTargetId());
                            
                            if (pointBlankShots.isEmpty()) {
                                sendHiddenPBSCFRResponse(null);
                            } else {
                                // we send two packets because the server will ignore the first one
                                sendHiddenPBSCFRResponse(new Vector<>());
                                sendHiddenPBSCFRResponse(pointBlankShots);
                            }
                        } catch (Exception e) {
                            // if we screw up, don't keep everyone else waiting
                            sendHiddenPBSCFRResponse(null);
                            throw e;
                        }

                        break;
                    case Packet.COMMAND_CFR_TAG_TARGET:
                        sendTAGTargetCFRResponse(pickTagTarget(evt));
                        break;
                }
            }

        });
    }

    @Override
    public boolean isBot() {
        return true;
    }

    BotConfiguration config = new BotConfiguration();

    protected BoardClusterTracker boardClusterTracker;

    public abstract void initialize();

    protected abstract void processChat(GamePlayerChatEvent ge);

    protected abstract void initMovement();

    protected abstract void initFiring();

    /**
     * Determines which entity should be moved next and then calls to {@link #continueMovementFor(Entity)} with
     * that entity.
     *
     * @return The calculated move path.
     * @throws NullPointerException if no entity can be found to move.
     */
    protected abstract MovePath calculateMoveTurn();

    protected abstract void calculateFiringTurn();

    protected abstract void calculateDeployment();

    protected void initTargeting() { }
    
    /**
     * Calculates the targeting/offboard turn
     * This includes firing TAG and non-direct-fire artillery
     * Does nothing in this implementation.
     */
    protected void calculateTargetingOffBoardTurn() {
        sendAttackData(game.getFirstEntityNum(getMyTurn()),
                new Vector<>(0));
        sendDone(true);
    }
    
    @Nullable
    protected abstract PhysicalOption calculatePhysicalTurn();
    
    protected Vector<EntityAction> calculatePointBlankShot(int firingEntityID, int targetID) { 
        return new Vector<>();
    }
    
    protected int pickTagTarget(GameCFREvent evt) {
        return 0;
    }

    /**
     * Calculates the full {@link MovePath} for the given {@link Entity}.
     *
     * @param entity The entity who is to move.
     * @return The calculated move path.
     * @throws NullPointerException if entity is NULL.
     */
    protected abstract MovePath continueMovementFor(Entity entity);

    protected abstract Vector<Minefield> calculateMinefieldDeployment();

    protected abstract Vector<Coords> calculateArtyAutoHitHexes();

    protected abstract void checkMorale();

    @Override
    protected boolean keepGameLog() {
        return false;
    }

    /**
     * Helper function that determines which of this bot's entities are stranded inside immobilized transports. 
     * @return Array of entity IDs.
     */
    public int[] getStrandedEntities() {
        List<Integer> entitiesToUnload = new ArrayList<>();
        
        // Basically, we loop through all entities owned by the current player
        // And if the entity happens to be in a disabled transport, then we unload it
        // unless doing so would kill it or be illegal due to stacking violation
        for (Entity currentEntity : getGame().getPlayerEntities(getLocalPlayer(), true)) {
            Entity transport = currentEntity.getTransportId() != Entity.NONE ? getGame().getEntity(currentEntity.getTransportId()) : null;
            
            if (transport != null && transport.isPermanentlyImmobilized(true)) {
                boolean stackingViolation = null != Compute.stackingViolation(game, currentEntity.getId(), transport.getPosition());
                boolean unloadFatal = currentEntity.isBoardProhibited(getGame().getBoard().getType()) ||
                        currentEntity.isLocationProhibited(transport.getPosition());
                        
                if (!stackingViolation && !unloadFatal) {
                    entitiesToUnload.add(currentEntity.getId());
                }
            }
        }
        
        int[] entityIDs = new int[entitiesToUnload.size()];
        for (int x = 0; x < entitiesToUnload.size(); x++) {
            entityIDs[x] = entitiesToUnload.get(x);
        }
        
        return entityIDs;
    }
    
    public List<Entity> getEntitiesOwned() {
        ArrayList<Entity> result = new ArrayList<>();
        for (Entity entity : game.getEntitiesVector()) {
            if (entity.getOwner().equals(getLocalPlayer())
                && (entity.getPosition() != null) && !entity.isOffBoard()) {
                result.add(entity);
            }
        }
        return result;
    }
    
    protected Entity getArbitraryEntity() {
        for (Entity entity : game.getEntitiesVector()) {
            if (entity.getOwner().equals(getLocalPlayer())) {
                return entity;
            }
        }
        
        return null;
    }

    /**
     * Lazy-loaded list of enemy entities that we should consider firing at.
     * Only good for the current entity turn calculation, as this list can change between individual entity turns. 
     */
    public List<Entity> getEnemyEntities() {
        if (currentTurnEnemyEntities == null) {
            currentTurnEnemyEntities = new ArrayList<>();
            for (Entity entity : game.getEntitiesVector()) {
                if (entity.getOwner().isEnemyOf(getLocalPlayer())
                    && (entity.getPosition() != null) && !entity.isOffBoard()
                    && (entity.getCrew() != null) && !entity.getCrew().isDead()) {
    
                    currentTurnEnemyEntities.add(entity);
                }
            }
        }
        
        return currentTurnEnemyEntities;
    }

    /**
     * Lazy-loaded list of friendly entities.
     * Only good for the current entity turn calculation, as this list can change between individual entity turns. 
     */
    public List<Entity> getFriendEntities() {
        if (currentTurnFriendlyEntities == null) {
            currentTurnFriendlyEntities = new ArrayList<>();
            for (Entity entity : game.getEntitiesVector()) {
                if (!entity.getOwner().isEnemyOf(getLocalPlayer()) && (entity.getPosition() != null)
                    && !entity.isOffBoard()) {
                    currentTurnFriendlyEntities.add(entity);
                }
            }
        }
        
        return currentTurnFriendlyEntities;
    }

    // TODO: move initMovement to be called on phase end
    @Override
    public void changePhase(GamePhase phase) {
        super.changePhase(phase);

        try {
            switch (phase) {
                case LOUNGE:
                    sendChat(Messages.getString("BotClient.Hi"));
                    break;
                case DEPLOYMENT:
                    initialize();
                    break;
                case MOVEMENT:
                    /* Do not uncomment this. It is so that bots stick around till end of game
                     * for proper salvage. If the bot dies out here, the salvage for all but the
                     * last bot disappears for some reason
                    if (game.getEntitiesOwnedBy(getLocalPlayer()) == 0) {
                        sendChat(Messages.getString("BotClient.HowAbout"));
                        die();
                    }
                     */
                    // if the game is not double blind and I can't see anyone
                    // else on the board I should kill myself.
                    if (!(game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND))
                        && ((game.getEntitiesOwnedBy(getLocalPlayer())
                             - game.getNoOfEntities()) == 0)) {
                        die();
                    }

                    if (Compute.randomInt(4) == 1) {
                        String message = getRandomBotMessage();
                        if (message != null) {
                            sendChat(message);
                        }
                    }
                    initMovement();
                    break;
                case FIRING:
                    initFiring();
                    break;
                case PHYSICAL:
                    break;
                case TARGETING:
                    initTargeting();
                    break;
                case END_REPORT:
                    // Check if stealth armor should be switched on/off
                    // Kinda cheap leaving this until the end phase, players
                    // can't do this
                    toggleStealth();
                    endOfTurnProcessing();
                    // intentional fallthrough: all reports must click "done", otherwise the game never moves on.
                case TARGETING_REPORT:
                case INITIATIVE_REPORT:
                case MOVEMENT_REPORT:
                case OFFBOARD_REPORT:
                case FIRING_REPORT:
                case PHYSICAL_REPORT:
                    sendDone(true);
                    break;
                case VICTORY:
                    runEndGame();
                    sendChat(Messages.getString("BotClient.Bye"));
                    die();
                    break;
                default:
                    break;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void runEndGame() {
        // Make a list of the player's living units.
        ArrayList<Entity> living = game.getPlayerEntities(getLocalPlayer(), false);

        // Be sure to include all units that have retreated.
        for (Enumeration<Entity> iter = game.getRetreatedEntities(); iter.hasMoreElements(); ) {
            Entity ent = iter.nextElement();
            if (ent.getOwnerId() == getLocalPlayer().getId()) {
                living.add(ent);
            }
        }

        if (living.isEmpty()) {
            return;
        }

        String sLogDir = PreferenceManager.getClientPreferences().getLogDirectory();
        File logDir = new File(sLogDir);
        if (!logDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            logDir.mkdir();
        }
        String fileName = "Bot_" + getLocalPlayer().getName() + ".mul";
        if (PreferenceManager.getClientPreferences().stampFilenames()) {
            fileName = StringUtil.addDateTimeStamp(fileName);
        }
        File unitFile = new File(sLogDir + File.separator + fileName);
        try {
            // Save the entities to the file.
            EntityListFile.saveTo(unitFile, living);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            doAlertDialog(Messages.getString("ClientGUI.errorSavingFile"), ex.getMessage());
        }
    }

    private Entity getRandomUnmovedEntity() {
        List<Entity> owned = getEntitiesOwned();
        List<Entity> unMoved = new ArrayList<>();
        for (Entity e : owned) {
            if (e.isSelectableThisTurn()) {
                unMoved.add(e);
            }
        }
        return unMoved.get(Compute.randomInt(unMoved.size()));
    }
    
    /**
     * Calculate what to do on my turn.
     * Has a retry mechanism for when the turn calculation fails due to concurrency issues
     */
    private synchronized void calculateMyTurn() {
        int retryCount = 0;
        boolean success = false;
        
        while ((retryCount < BOT_TURN_RETRY_COUNT) && !success) {
            success = calculateMyTurnWorker();

            if (!success) {
                // if we fail, take a nap for 500-1500 milliseconds, then try again
                // as it may be due to some kind of thread-related issue
                // limit number of retries, so we're not endlessly spinning
                // if we can't recover from the error
                retryCount++;
                try {
                    Thread.sleep(Compute.randomInt(1000) + 500);
                } catch (InterruptedException e) {
                    LogManager.getLogger().error("", e);
                }
            }
        }
    }

    /**
     * Worker function for a single attempt to calculate the bot's turn.
     */
    private synchronized boolean calculateMyTurnWorker() {
        // clear out transient data
        currentTurnEnemyEntities = null;
        currentTurnFriendlyEntities = null;
        
        try {
            if (game.getPhase() == GamePhase.MOVEMENT) {
                MovePath mp;
                if (game.getTurn() instanceof GameTurn.SpecificEntityTurn) {
                    GameTurn.SpecificEntityTurn turn = (GameTurn.SpecificEntityTurn) game
                            .getTurn();
                    Entity mustMove = game.getEntity(turn.getEntityNum());
                    mp = continueMovementFor(mustMove);
                } else {
                    if (config.isForcedIndividual()) {
                        Entity mustMove = getRandomUnmovedEntity();
                        mp = continueMovementFor(mustMove);
                    } else {
                        mp = calculateMoveTurn();
                    }
                }
                moveEntity(mp.getEntity().getId(), mp);
            } else if (game.getPhase() == GamePhase.FIRING) {
                calculateFiringTurn();
            } else if (game.getPhase() == GamePhase.PHYSICAL) {
                PhysicalOption po = calculatePhysicalTurn();
                // Bug #1072137: don't crash if the bot can't find a physical.
                if (null != po) {
                    sendAttackData(po.attacker.getId(), po.getVector());
                } else {
                    // Send a "no attack" to clear the game turn, if any.
                    sendAttackData(game.getFirstEntityNum(getMyTurn()),
                                   new Vector<>(0));
                }
            } else if (game.getPhase() == GamePhase.DEPLOYMENT) {
                calculateDeployment();
            } else if (game.getPhase() == GamePhase.DEPLOY_MINEFIELDS) {
                Vector<Minefield> mines = calculateMinefieldDeployment();
                for (Minefield mine : mines) {
                    game.addMinefield(mine);
                }
                sendDeployMinefields(mines);
                sendPlayerInfo();
            } else if (game.getPhase() == GamePhase.SET_ARTILLERY_AUTOHIT_HEXES) {
                // For now, declare no autohit hexes.
                Vector<Coords> autoHitHexes = calculateArtyAutoHitHexes();
                sendArtyAutoHitHexes(autoHitHexes);
            } else if ((game.getPhase() == GamePhase.TARGETING)
                       || (game.getPhase() == GamePhase.OFFBOARD)) {
                // Princess implements arty targeting; no plans to do so for testbod
                calculateTargetingOffBoardTurn();
            }
            
            return true;
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            return false;
        }
    }

    public double getMassOfAllInBuilding(final Game game, final Coords coords) {
        double mass = 0;

        // Add the mass of anyone else standing in/on this building.
        final Hex hex = game.getBoard().getHex(coords);
        final int buildingElevation = hex.terrainLevel(Terrains.BLDG_ELEV);
        final int bridgeElevation = hex.terrainLevel(Terrains.BRIDGE_ELEV);
        Iterator<Entity> crowd = game.getEntities(coords);
        while (crowd.hasNext()) {
            Entity e = crowd.next();

            if (buildingElevation >= e.getElevation() || bridgeElevation >= e.getElevation()) {
                mass += e.getWeight();
            }
        }

        return mass;
    }
    
    /**
     * Gets valid & empty starting coords around the specified point. This
     * method iterates through the list of Coords and returns the first Coords
     * that does not have a stacking violation.
     */
    protected Coords getFirstValidCoords(Entity deployedUnit, List<Coords> possibleDeployCoords) {
        // Check all of the hexes in order.
        for (Coords dest : possibleDeployCoords) {
            Entity violation = Compute.stackingViolation(game, deployedUnit,
                    dest, deployedUnit.getElevation(), dest, null);
            // Ignore coords that could cause a stacking violation
            if (violation != null) {
                continue;
            }

            // Make sure we don't overload any buildings in this hex.
            Building building = game.getBoard().getBuildingAt(dest);
            if (null != building) {
                double mass = getMassOfAllInBuilding(game, dest) + deployedUnit.getWeight();
                if (mass > building.getCurrentCF(dest)) {
                    continue;
                }
            }

            return dest;
        }

        System.out.println("Returning no deployment position; THIS IS BAD!");
        // If NONE of them are acceptable, then just return null.
        return null;
    }

    protected List<Coords> getStartingCoordsArray(Entity deployed_ent) {
        int highest_elev, lowest_elev, weapon_count;
        double av_range, ideal_elev;
        double adjusted_damage, max_damage, total_damage;
        Board board = game.getBoard();
        Coords highestHex;
        List<RankedCoords> validCoords = new LinkedList<>();
        Vector<Entity> valid_attackers;
        WeaponAttackAction test_attack;
        List<ECMInfo> allECMInfo = ComputeECM.computeAllEntitiesECMInfo(game.getEntitiesVector());

        // Create array of hexes in the deployment zone that can be deployed to
        // Check for prohibited terrain, stacking limits
        for (int x = 0; x <= board.getWidth(); x++) {
            for (int y = 0; y <= board.getHeight(); y++) {
                Coords c = new Coords(x, y);
                if (board.isLegalDeployment(c, deployed_ent.getStartingPos())
                    && !deployed_ent.isLocationProhibited(c)) {
                    validCoords.add(new RankedCoords(c, 0));
                }
            }
        }

        // Randomize hexes to prevent clumping at the upper-left corner on
        // very flat maps
        Collections.shuffle(validCoords);

        // Now get minimum and maximum elevation levels for these hexes
        highest_elev = Integer.MIN_VALUE;

        lowest_elev = Integer.MAX_VALUE;
        for (RankedCoords c : validCoords) {
            int elev = board.getHex(c.getX(), c.getY()).getLevel();
            if (elev > highest_elev) {
                highest_elev = board.getHex(c.getX(), c.getY()).getLevel();
            }
            if (elev < lowest_elev) {
                lowest_elev = board.getHex(c.getX(), c.getY()).getLevel();
            }
        }

        // Calculate average range of all weapons
        // Do not include ATMs, but DO include each bin of ATM ammo
        // Increase average range if the unit has an active c3 link
        av_range = 0.0;
        weapon_count = 0;
        for (Mounted mounted : deployed_ent.getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            if ((!wtype.getName().equals("ATM 3"))
                && (!wtype.getName().equals("ATM 6"))
                && (!wtype.getName().equals("ATM 9"))
                && (!wtype.getName().equals("ATM 12"))) {
                if (deployed_ent.getC3Master() != null) {
                    av_range += wtype.getLongRange() * 1.25;
                } else {
                    av_range += wtype.getLongRange();
                }
                weapon_count++;
            }
        }
        for (Mounted mounted : deployed_ent.getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();
            if (atype.getAmmoType() == AmmoType.T_ATM) {
                weapon_count++;
                av_range += 15.0;
                if (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                    av_range -= 6;
                }
                if (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                    av_range += 12.0;
                }
            } else if (atype.getAmmoType() == AmmoType.T_MML) {
                weapon_count++;
                if (atype.hasFlag(AmmoType.F_MML_LRM)) {
                    av_range = 9.0;
                } else {
                    av_range = 21.0;
                }
            }
        }
        av_range = av_range / weapon_count;

        // Calculate ideal elevation as a factor of average range of 18 being
        // highest elevation.  Fast, non-jumping units should deploy towards
        // the middle elevations to avoid getting stuck up a cliff.
        if ((deployed_ent.getJumpMP() == 0) &&
            (deployed_ent.getWalkMP() > 5)) {
            ideal_elev = lowest_elev + ((highest_elev - lowest_elev) / 3.0);
        } else {
            ideal_elev = lowest_elev
                         + ((av_range / 18) * (highest_elev - lowest_elev));
        }
        if (ideal_elev > highest_elev) {
            ideal_elev = highest_elev;
        }

        double highestFitness = -5000;
        
        for (RankedCoords coord : validCoords) {

            // Calculate the fitness factor for each hex and save it to the array
            // -> Absolute difference between hex elevation and ideal elevation decreases fitness
            coord.setFitness(-1 * (Math.abs(ideal_elev - board.getHex(coord.getX(), coord.getY()).getLevel())));

            total_damage = 0.0;
            deployed_ent.setPosition(coord.getCoords());

            // Create a list of potential attackers/targets for this location
            List<Entity> potentialAttackers =
                    game.getValidTargets(deployed_ent);
            valid_attackers = new Vector<>(potentialAttackers.size());
            for (Entity e : potentialAttackers) {

                // Unit must be deployed and not off board, with valid position
                if ((e.isDeployed()) && !e.isOffBoard()
                    && e.getPosition() != null) {
                    int dist = deployed_ent.getPosition().distance(
                            e.getPosition());
                    // Approximation of effective range, we could use av_range,
                    //  however that could bad if deploy_ent is short ranged
                    //  and a potential  target is long range
                    if (dist < 18) {
                        valid_attackers.add(e);
                    }
                }
            }

            // -> Approximate total damage taken in the current position; this
            // keeps units from deploying into x-fires
            for (Entity test_ent : valid_attackers) {
                for (Mounted mounted : test_ent.getWeaponList()) {
                    test_attack = new WeaponAttackAction(test_ent.getId(),
                                                         deployed_ent.getId(),
                                                         test_ent.getEquipmentNum(mounted));
                    adjusted_damage = BotClient.getDeployDamage(game,
                                                                test_attack, allECMInfo);
                    total_damage += adjusted_damage;
                }
            }
            coord.fitness -= (total_damage / 10);

            // -> Find the best target for each weapon and approximate the
            // damage; maybe we can kill stuff without moving!
            // -> Conventional infantry ALWAYS come out on the short end of the
            // stick in damage given/taken... solutions?
            total_damage = 0.0;
            for (Mounted mounted : deployed_ent.getWeaponList()) {
                max_damage = 0.0;
                for (Entity test_ent : valid_attackers) {
                    test_attack = new WeaponAttackAction(deployed_ent.getId(),
                                                         test_ent.getId(),
                                                         deployed_ent.getEquipmentNum(mounted));
                    adjusted_damage = BotClient.getDeployDamage(game,
                                                                test_attack, allECMInfo);
                    if (adjusted_damage > max_damage) {
                        max_damage = adjusted_damage;
                    }
                }
                total_damage += max_damage;
            }
            coord.fitness += (total_damage / 10);

            // Mech
            if (deployed_ent.hasETypeFlag(Entity.ETYPE_MECH)) {
                // -> Trees are good, when they're tall enough
                // -> Water isn't that great below depth 1 -> this saves actual
                // ground space for infantry/vehicles (minor)
                int x = coord.getX();
                int y = coord.getY();
                if (board.getHex(x, y).containsTerrain(Terrains.WOODS)
                        && board.getHex(x, y).terrainLevel(Terrains.FOLIAGE_ELEV) > 1) {
                    coord.fitness += 1;
                }
                if (board.getHex(x, y).containsTerrain(Terrains.WATER)) {
                    if (board.getHex(x, y).depth() > 1) {
                        coord.fitness -= board.getHex(x, y).depth();
                    }
                }
                //If building, make sure not too heavy to safely move out of
                coord.fitness -= potentialBuildingDamage(coord.getX(), coord.getY(),
                                                         deployed_ent);
            }

            // Infantry

            if (deployed_ent.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
                // -> Trees and buildings make good cover, esp for conventional
                // infantry
                // rough is nice, too
                // -> Massed infantry is more effective, so try to cluster them
                if (board.getHex(coord.getX(), coord.getY()).containsTerrain(
                        Terrains.ROUGH)) {
                    coord.fitness += 1.5;
                }
                if (board.getHex(coord.getX(), coord.getY()).containsTerrain(
                        Terrains.WOODS)) {
                    coord.fitness += 2;
                }
                if (board.getHex(coord.getX(), coord.getY()).containsTerrain(
                        Terrains.BUILDING)) {
                    coord.fitness += 4;
                }
                highestHex = coord.getCoords();
                for (Entity test_ent : game.getEntitiesVector(highestHex)) {
                    if ((deployed_ent.getOwner().equals(test_ent.getOwner()))
                        && !deployed_ent.equals(test_ent)) {
                        if (test_ent instanceof Infantry) {
                            coord.fitness += 2;
                            break;
                        }
                    }
                }
                boolean foundAdj = false;
                Player owner = deployed_ent.getOwner();
                for (int x = 0; x < 6 && !foundAdj; x++) {
                    highestHex = coord.getCoords().translated(x);
                    for (Entity test_ent : game.getEntitiesVector(highestHex)) {
                        if ((owner.equals(test_ent.getOwner()))
                            && !deployed_ent.equals(test_ent)
                            && (test_ent instanceof Infantry)) {

                            coord.fitness += 1;
                            foundAdj = true;

                        }
                    }
                }

                // Not sure why bot tries to deploy infantry in water, it SHOULD
                // be caught by the isHexProhibited method when
                // selecting hexes, but sometimes it has a mind of its own so...
                if (board.getHex(coord.getX(), coord.getY()).containsTerrain(
                        Terrains.WATER)) {
                    coord.fitness -= 10;
                }
            }

            // some criteria for deploying non-vtol tanks
            if (deployed_ent.hasETypeFlag(Entity.ETYPE_TANK) &&
                    !deployed_ent.hasETypeFlag(Entity.ETYPE_VTOL)) {
                // Tracked vehicle
                // -> Trees increase fitness
                if (deployed_ent.getMovementMode() == EntityMovementMode.TRACKED) {
                    if (board.getHex(coord.getX(), coord.getY()).containsTerrain(Terrains.WOODS)) {
                        coord.fitness += 2;
                    }
                }

                // Wheeled vehicle
                // -> Not sure what any benefits wheeled vehicles can get; for
                // now, just elevation and damage taken/given
                // Hover vehicle
                // -> Water in hex increases fitness, hover vehicles have an
                // advantage in water areas
                if (deployed_ent.getMovementMode() == EntityMovementMode.HOVER) {
                    if (board.getHex(coord.getX(), coord.getY()).containsTerrain(
                            Terrains.WATER)) {
                        coord.fitness += 2;
                    }
                }
                // If building, make sure not too heavy to safely move out of.
                coord.fitness -= potentialBuildingDamage(coord.getX(), coord.getY(),
                                                         deployed_ent);
            }
            
            // ProtoMech
            // ->
            // -> Trees increase fitness by +2 (minor)
            if (deployed_ent instanceof Protomech) {
                if (board.getHex(coord.getX(), coord.getY()).containsTerrain(
                        Terrains.WOODS)) {
                    coord.fitness += 2;
                }
            }

            // Make sure I'm not stuck in a dead-end.
            coord.fitness += calculateEdgeAccessFitness(deployed_ent, board);
            
            if (coord.fitness > highestFitness) {
                highestFitness = coord.fitness;
            }
        }

        // now, we double check: did we get a bunch of coordinates with a value way below 0?
        // This indicates that we do not have a way of getting to the opposite board edge,
        // even when considering terrain destruction
        // attempt to deploy in the biggest area this unit can access instead
        if (highestFitness < -10) {
            for (RankedCoords rc : validCoords) {
                rc.fitness += getClusterTracker().getBoardClusterSize(deployed_ent, rc.coords, false);
            }
        }
        
        // Now sort the valid array.
        Collections.sort(validCoords);

        List<Coords> result = new ArrayList<>(validCoords.size());
        for (RankedCoords rc : validCoords) {
            result.add(rc.getCoords());
        }

        return result;
    }

    /**
     * Determines if the given entity has reasonable access to the "opposite" edge of the board from its
     * current position. Returns 0 if this can be accomplished without destroying any terrain, 
     * -50 if this can be accomplished but terrain must be destroyed,
     * -100 if this cannot be accomplished at all
     */
    private int calculateEdgeAccessFitness(Entity entity, Board board) {
        // Flying units can always get anywhere
        if (entity.isAirborne() || entity instanceof VTOL) {
            return 0;
        }
        
        CardinalEdge destinationEdge = BoardUtilities.determineOppositeEdge(entity);
        
        int noReductionZoneSize = getClusterTracker().getDestinationCoords(entity, destinationEdge, false).size();
        int reductionZoneSize = getClusterTracker().getDestinationCoords(entity, destinationEdge, true).size();
        
        if (noReductionZoneSize > 0) {
            return 0;
        } else if (reductionZoneSize > 0) {
            return -50;
        } else {
            return -100;
        }
    }

    private double potentialBuildingDamage(int x, int y, Entity entity) {
        Coords coords = new Coords(x, y);
        Building building = game.getBoard().getBuildingAt(coords);
        if (building == null) {
            return 0;
        }
        int potentialDmg = (int) Math.ceil((double) building.getCurrentCF(coords) / 10);
        boolean aptGunnery = entity.hasAbility(OptionsConstants.PILOT_APTITUDE_GUNNERY);
        double oddsTakeDmg = 1 - (Compute.oddsAbove(entity.getCrew().getPiloting(), aptGunnery) / 100);
        return potentialDmg * oddsTakeDmg;
    }

    // Missile hits table
    // Some of these are interpolated for odd weapons sizes found in Protos and
    // new BAs
    private static float[] expectedHitsByRackSize = {0.0f, 1.0f, 1.58f, 2.0f,
                                                     2.63f, 3.17f, 4.0f, 4.49f, 4.98f, 5.47f, 6.31f, 7.23f, 8.14f,
                                                     8.59f, 9.04f, 9.5f, 0.0f, 0.0f, 0.0f, 0.0f, 12.7f};

    /**
     * Determines the expected damage of a weapon attack, based on to-hit, salvo
     * sizes, etc. This has been copied almost wholesale from
     * Compute.getExpectedDamage; the logfile print commands were removed due to
     * excessive data generated
     */
    private static float getDeployDamage(Game g, WeaponAttackAction waa, List<ECMInfo> allECMInfo) {
        Entity attacker = g.getEntity(waa.getEntityId());
        boolean naturalAptGunnery = attacker.hasAbility(OptionsConstants.PILOT_APTITUDE_GUNNERY);
        Mounted weapon = attacker.getEquipment(waa.getWeaponId());
        ToHitData hitData = waa.toHit(g, allECMInfo);
        if (hitData.getValue() > 12) {
            return 0.0f;
        }

        float fChance;
        if (hitData.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            fChance = 1.0f;
        } else {
            fChance = (float) Compute.oddsAbove(hitData.getValue(), naturalAptGunnery) / 100.0f;
        }

        // TODO : update for BattleArmor.

        float fDamage;
        WeaponType wt = (WeaponType) weapon.getType();
        if (wt.getDamage() == WeaponType.DAMAGE_BY_CLUSTERTABLE) {
            if (weapon.getLinked() == null) {
                return 0.0f;
            }
            AmmoType at = (AmmoType) weapon.getLinked().getType();

            float fHits;
            if ((wt.getAmmoType() == AmmoType.T_SRM_STREAK)
                || (wt.getAmmoType() == AmmoType.T_LRM_STREAK)) {
                fHits = wt.getRackSize();
            } else if ((wt.getRackSize() == 40) || (wt.getRackSize() == 30)) {
                fHits = 2.0f * expectedHitsByRackSize[wt.getRackSize() / 2];
            } else {
                fHits = expectedHitsByRackSize[wt.getRackSize()];
            }
            // adjust for previous AMS
            ArrayList<Mounted> vCounters = waa.getCounterEquipment();
            if (wt.hasFlag(WeaponType.F_MISSILE) && vCounters != null) {
                for (Mounted vCounter : vCounters) {
                    EquipmentType type = vCounter.getType();
                    if ((type instanceof WeaponType)
                        && type.hasFlag(WeaponType.F_AMS)) {
                        float fAMS = 3.5f * ((WeaponType) type).getDamage();
                        fHits = Math.max(0.0f, fHits - fAMS);
                    }
                }
            }
            // damage is expected missiles * damage per missile
            fDamage = fHits * at.getDamagePerShot();
        } else {
            fDamage = wt.getDamage();
        }

        fDamage *= fChance;
        return fDamage;
    }

    /**
     * If the unit has stealth armor, turning it off is probably a good idea if
     * most of the enemy force is at 'short' range or if in danger of
     * overheating
     */

    private void toggleStealth() {

        initialize();

        int total_bv, known_bv, known_range, known_count, trigger_range;
        int new_stealth;

        for (Entity check_ent : game.getEntitiesVector()) {
            if ((check_ent.getOwnerId() == localPlayerNumber)
                && (check_ent instanceof Mech)) {
                if (check_ent.hasStealth()
                        && (check_ent.getPosition() != null)) {
                    for (Mounted mEquip : check_ent.getMisc()) {
                        MiscType mtype = (MiscType) mEquip.getType();
                        if (mtype.hasFlag(MiscType.F_STEALTH)) {

                            // If the Mech is in danger of shutting down (14+
                            // heat), consider shutting
                            // off the armor

                            trigger_range = 13 + Compute.randomInt(7);
                            if (check_ent.heat > trigger_range) {
                                new_stealth = 0;
                            } else {

                                // Mech is not in danger of shutting down soon;
                                // if most of the
                                // enemy is right next to the Mech deactivate
                                // armor to free up
                                // heatsinks for weapons fire

                                total_bv = 0;
                                known_bv = 0;
                                known_range = 0;
                                known_count = 0;

                                for (Entity test_ent : game.getEntitiesVector()) {
                                    if (check_ent.isEnemyOf(test_ent)) {
                                        total_bv += test_ent
                                                .calculateBattleValue();
                                        if (test_ent.isVisibleToEnemy()) {
                                            known_count++;
                                            known_bv += test_ent
                                                    .calculateBattleValue();
                                            known_range += Compute
                                                    .effectiveDistance(game,
                                                                       check_ent, test_ent);
                                        }
                                    }
                                }

                                // If no or few enemy units are visible, they're
                                // hiding;
                                // Default to stealth armor on in this case

                                if ((known_count == 0)
                                    || (known_bv < (total_bv / 2))) {
                                    new_stealth = 1;
                                } else {
                                    if ((known_range / known_count) <= (5 + Compute
                                            .randomInt(5))) {
                                        new_stealth = 0;
                                    } else {
                                        new_stealth = 1;
                                    }
                                }
                            }
                            mEquip.setMode(new_stealth);
                            sendModeChange(check_ent.getId(), check_ent
                                    .getEquipmentNum(mEquip), new_stealth);
                            break;
                        }
                    }
                }
            }
        }
    }

    private String getRandomBotMessage() {
        String message = "";

        try {
            String scrapFile = "./mmconf/botmessages.txt";
            FileInputStream fis = new FileInputStream(scrapFile);
            BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
            while (dis.ready()) {
                message = dis.readLine();
                if (Compute.randomInt(10) == 1) {
                    break;
                }
            }
            dis.close();
            fis.close();
        }// File not found don't do anything just return a null and allow the
        // bot to remain silent
        catch (FileNotFoundException fnfe) {
            // no chat message found continue on.
            return null;
        }// CYA exception
        catch (Exception ex) {
            System.err.println("Error while reading ./mmconf/botmessages.txt.");
            ex.printStackTrace();
            return null;
        }
        return message;
    }

    /**
     * Pops up a dialog box showing an alert
     */
    public void doAlertDialog(String title, String message) {
        JTextPane textArea = new JTextPane();
        ReportDisplay.setupStylesheet(textArea);

        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        textArea.setText("<pre>" + message + "</pre>");
        JOptionPane.showMessageDialog(frame, scrollPane, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    protected void correctName(Packet inP) {
        // If we have a clientgui, it keeps track of a Name -> Client map, and
        //  we need to update that map with this name change.
        if (getClientGUI() != null) {
            Map<String, Client> bots = getClientGUI().getBots();
            String oldName = getName();
            String newName = (String) (inP.getObject(0));
            assert (equals(bots.get(oldName)));
            bots.remove(oldName);
            bots.put(newName, this);
        }
        setName((String) (inP.getObject(0)));
    }

    private ClientGUI getClientGUI() {
        return clientgui;
    }

    public void setClientGUI(ClientGUI clientgui) {
        this.clientgui = clientgui;
    }

    public void endOfTurnProcessing() {
        // Do nothing;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void receiveBuildingCollapse(Packet packet) {
        game.getBoard().collapseBuilding((Vector<Coords>) packet.getObject(0));
    }
    
    /**
     * The bot client doesn't really need a text report
     * Let's save ourselves a little processing time and not deal with any of it
     */
    @Override
    public String receiveReport(Vector<Report> v) {
        return "";
    }
    
    /**
     * The bot client has no need of image tag caching
     * Let's save ourselves some CPU and memory and not deal with it
     */
    @Override
    protected void cacheImgTag(Entity entity) {
        
    }

    public BoardClusterTracker getClusterTracker() {
        return boardClusterTracker;
    }

    private static class RankedCoords implements Comparable<RankedCoords> {
        private Coords coords;
        private double fitness;

        RankedCoords(Coords coords, double fitness) {
            if (coords == null) {
                throw new IllegalArgumentException("Coords cannot be null.");
            }
            this.coords = coords;
            this.fitness = fitness;
        }

        public Coords getCoords() {
            return coords;
        }

        @SuppressWarnings("unused")
        public void setCoords(Coords coords) {
            if (coords == null) {
                throw new IllegalArgumentException("Coords cannot be null.");
            }
            this.coords = coords;
        }

        public double getFitness() {
            return fitness;
        }

        public void setFitness(double fitness) {
            this.fitness = fitness;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RankedCoords)) {
                return false;
            }

            RankedCoords coords1 = (RankedCoords) o;

            if (Double.compare(coords1.fitness, fitness) != 0) {
                return false;
            }
            //noinspection RedundantIfStatement
            if (!coords.equals(coords1.coords)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = coords.hashCode();
            temp = Double.doubleToLongBits(fitness);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "RankedCoords{" +
                   "coords=" + coords +
                   ", fitness=" + fitness +
                   '}';
        }

        int getX() {
            return coords.getX();
        }

        int getY() {
            return coords.getY();
        }

        @Override
        public int compareTo(RankedCoords o) {
            return -Double.compare(getFitness(), o.getFitness());
        }
    }
}
