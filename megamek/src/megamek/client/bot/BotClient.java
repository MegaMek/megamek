/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;

import megamek.client.Client;
import megamek.client.ui.swing.ReportDisplay;
import megamek.common.AmmoType;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.GameTurn;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Minefield;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GameReportEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;

public abstract class BotClient extends Client {

    // a frame, to show stuff in
    public JFrame frame;

    public class CalculateBotTurn implements Runnable {
        public void run() {
            calculateMyTurn();
            flushConn();
        }
    }

    public BotClient(String playerName, String host, int port) {
        super(playerName, host, port);
        game.addGameListener(new GameListenerAdapter() {

            @Override
            public void gamePlayerChat(GamePlayerChatEvent e) {
                processChat(e);
                flushConn();
            }

            @Override
            public void gameTurnChange(GameTurnChangeEvent e) {
                if (isMyTurn() 
                        && (e.getPlayer().getId() == localPlayerNumber)) {
                    // Run bot's turn processing in a separate thread.
                    // So calling thread is free to process the other actions.
                    Thread worker = new Thread(new CalculateBotTurn(),
                            getName() + " Turn " + game.getTurnIndex()
                                    + " Calc Thread");
                    worker.start();
                }
            }

            @Override
            public void gameReport(GameReportEvent e) {
                if (game.getPhase() == IGame.Phase.PHASE_INITIATIVE_REPORT) {
                    // Opponent has used tactical genius, must press
                    // "Done" again to advance past initiative report.
                    sendDone(true);
                    flushConn();
                }
            }

        });
    }

    BotConfiguration config = new BotConfiguration();

    public abstract void initialize();

    protected abstract void processChat(GamePlayerChatEvent ge);

    protected abstract void initMovement();

    protected abstract void initFiring();

    protected abstract MovePath calculateMoveTurn();

    protected abstract void calculateFiringTurn();

    protected abstract void calculateDeployment();

    protected abstract PhysicalOption calculatePhysicalTurn();

    protected abstract MovePath continueMovementFor(Entity entity);

    protected abstract Vector<Minefield> calculateMinefieldDeployment();

    protected abstract Vector<Coords> calculateArtyAutoHitHexes();

    public List<Entity> getEntitiesOwned() {
        ArrayList<Entity> result = new ArrayList<Entity>();
        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements(); ) {
            Entity entity = i.nextElement();
            if (entity.getOwner().equals(getLocalPlayer())
                    && (entity.getPosition() != null) && !entity.isOffBoard()) {
                result.add(entity);
            }
        }
        return result;
    }

    public List<Entity> getEnemyEntities() {
        ArrayList<Entity> result = new ArrayList<Entity>();
        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements(); ) {
            Entity entity = i.nextElement();
            if (entity.getOwner().isEnemyOf(getLocalPlayer())
                    && (entity.getPosition() != null) && !entity.isOffBoard()) {
                result.add(entity);
            }
        }
        return result;
    }

    public List<Entity> getFriendEntities() {
        List<Entity> result = new ArrayList<Entity>();
        Enumeration<Entity> i = game.getEntities();
        while (i.hasMoreElements()) {
            Entity entity = i.nextElement();
            if (!entity.getOwner().isEnemyOf(getLocalPlayer()) && (entity.getPosition() != null)
                    && !entity.isOffBoard()) {
                result.add(entity);
            }
        }
        return result;
    }

    // TODO: move initMovement to be called on phase end
    @Override
    public void changePhase(IGame.Phase phase) {
        super.changePhase(phase);

        try {
            switch (phase) {
                case PHASE_LOUNGE:
                    sendChat(Messages.getString("BotClient.Hi")); //$NON-NLS-1$
                    break;
                case PHASE_DEPLOYMENT:
                    initialize();
                    break;
                case PHASE_MOVEMENT:
                    if (game.getEntitiesOwnedBy(getLocalPlayer()) == 0) {
                        sendChat(Messages.getString("BotClient.HowAbout")); //$NON-NLS-1$
                        die();
                    }
                    // if the game is not double blind and I can't see anyone
                    // else on the board I should kill myself.
                    if (!(game.getOptions().booleanOption("double_blind")) //$NON-NLS-1$
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
                case PHASE_FIRING:
                    initFiring();
                    break;
                case PHASE_PHYSICAL:
                    break;
                case PHASE_END_REPORT:
                    // Check if stealth armor should be switched on/off
                    // Kinda cheap leaving this until the end phase, players
                    // can't do this
                    toggleStealth();
                case PHASE_INITIATIVE_REPORT:
                case PHASE_TARGETING_REPORT:
                case PHASE_MOVEMENT_REPORT:
                case PHASE_OFFBOARD_REPORT:
                case PHASE_FIRING_REPORT:
                case PHASE_PHYSICAL_REPORT:
                    sendDone(true);
                    break;
                case PHASE_VICTORY:
                    runEndGame();
                    sendChat(Messages.getString("BotClient.Bye")); //$NON-NLS-1$
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
        } catch (IOException excep) {
            excep.printStackTrace(System.err);
            doAlertDialog(Messages.getString("ClientGUI.errorSavingFile"), excep.getMessage()); //$NON-NLS-1$
        }
    }

    private Entity getRandomUnmovedEntity() {
        List<Entity> owned = getEntitiesOwned();
        List<Entity> unMoved = new ArrayList<Entity>();
        for (Entity e : owned) {
            if (e.isSelectableThisTurn()) {
                unMoved.add(e);
            }
        }
        return unMoved.get(Compute.randomInt(unMoved.size()));
    }

    synchronized protected void calculateMyTurn() {
        try {
            if (game.getPhase() == IGame.Phase.PHASE_MOVEMENT) {
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
            } else if (game.getPhase() == IGame.Phase.PHASE_FIRING) {
                calculateFiringTurn();
            } else if (game.getPhase() == IGame.Phase.PHASE_PHYSICAL) {
                PhysicalOption po = calculatePhysicalTurn();
                // Bug #1072137: don't crash if the bot can't find a physical.
                if (null != po) {
                    sendAttackData(po.attacker.getId(), po.getVector());
                } else {
                    // Send a "no attack" to clear the game turn, if any.
                    sendAttackData(game.getFirstEntityNum(getMyTurn()),
                                   new Vector<EntityAction>(0));
                }
            } else if (game.getPhase() == IGame.Phase.PHASE_DEPLOYMENT) {
                calculateDeployment();
            } else if (game.getPhase() == IGame.Phase.PHASE_DEPLOY_MINEFIELDS) {
                Vector<Minefield> mines = calculateMinefieldDeployment();
                for (Minefield mine : mines) {
                    game.addMinefield(mine);
                }
                sendDeployMinefields(mines);
                sendPlayerInfo();
            } else if (game.getPhase() == IGame.Phase.PHASE_SET_ARTYAUTOHITHEXES) {
                // For now, declare no autohit hexes.
                Vector<Coords> autoHitHexes = calculateArtyAutoHitHexes();
                sendArtyAutoHitHexes(autoHitHexes);
            } else if ((game.getPhase() == IGame.Phase.PHASE_TARGETING)
                    || (game.getPhase() == IGame.Phase.PHASE_OFFBOARD)) {
                // Send a "no attack" to clear the game turn, if any.
                // TODO: Fix for real arty stuff
                sendAttackData(game.getFirstEntityNum(getMyTurn()),
                               new Vector<EntityAction>(0));
                sendDone(true);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Gets valid & empty starting coords around the specified point
     */
    protected Coords getCoordsAround(Entity deploy_me, LinkedList<Coords> c) {
        int mech_count;
        int conv_fcount; // Friendly conventional units
        int conv_ecount; // Enemy conventional units
        // Check all of the hexes in order.
        for (Coords element : c) {
            // Verify stacking limits. Gotta do this the long way, as
            // Compute.stackingViolation references the entity's CURRENT
            // position as well as the hex being checked; because its not
            // deployed yet it doesn't have a location!
            mech_count = 0;
            conv_fcount = 0;
            conv_ecount = 0;
            for (Enumeration<Entity> stacked_ents = game.getEntities(element); stacked_ents
                    .hasMoreElements(); ) {
                Entity test_ent = stacked_ents.nextElement();
                if (test_ent instanceof Mech) {
                    mech_count++;
                } else {
                    if (deploy_me.isEnemyOf(test_ent)) {
                        conv_ecount++;
                    } else {
                        conv_fcount++;
                    }
                }
            }
            if (deploy_me instanceof Mech) {
                mech_count++;
            } else {
                conv_fcount++;
            }
            if (mech_count > 1) {
                continue;
            }
            if ((conv_fcount + mech_count) > 2) {
                continue;
            }
            if ((conv_fcount + conv_ecount) > 4) {
                continue;
            }
            return element;
        }

        System.out.println("Returning no deployment position; THIS IS BAD!");
        // If NONE of them are acceptable, then just return null.
        return null;
        /*
         * // check the requested coords if
         * (game.getBoard().isLegalDeployment(c, this.getLocalPlayer()) &&
         * game.getFirstEntity(c) == null) { // Verify that the unit can be
         * placed in this hex if
         * (!deploy_me.isHexProhibited(game.getBoard().getHex(c.x, c.y))) {
         * return c; } } // check the rest of the list. for (int x = 0; x < 6;
         * x++) { Coords c2 = c.translated(x); if
         * (game.getBoard().isLegalDeployment(c2, this.getLocalPlayer()) &&
         * game.getFirstEntity(c2) == null) { if
         * (!deploy_me.isHexProhibited(game.getBoard().getHex(c2.x, c2.y))) {
         * return c2; } } } // recurse in a random direction return
         * getCoordsAround(deploy_me, c.translated(Compute.randomInt(6)));
         */
    }

    // New bot deploy algorithm
    // Screens out invalid hexes then rates them
    // Highest rating wins out; if this applies to multiple hexes then randomly
    // select among them
    protected Coords getStartingCoords() {
        LinkedList<Coords> calc = getStartingCoordsArray();
        if (calc != null && calc.size() > 0) {
            return calc.peek();
        }
        return null;
    }

    protected LinkedList<Coords> getStartingCoordsArray() {
        int highest_elev, lowest_elev, weapon_count;
        double av_range, ideal_elev;
        double adjusted_damage, max_damage, total_damage;
        IBoard board = game.getBoard();
        Coords highestHex;
        LinkedList<Coords> validCoords = new LinkedList<Coords>();
        Vector<Entity> valid_attackers;
        Entity deployed_ent = getEntity(game.getFirstDeployableEntityNum());
        WeaponAttackAction test_attack;

        // Create array of hexes in the deployment zone that can be deployed to
        // Check for prohibited terrain, stacking limits
        for (int x = 0; x <= board.getWidth(); x++) {
            for (int y = 0; y <= board.getHeight(); y++) {
                Coords c = new Coords(x, y);
                if (board.isLegalDeployment(c, deployed_ent.getStartingPos())
                        && !deployed_ent.isLocationProhibited(c)) {
                    validCoords.add(new Coords(c));
                }
            }
        }

        // Randomize hexes to prevent clumping at the upper-left corner on 
        // very flat maps
        Collections.shuffle(validCoords);

        // Now get minimum and maximum elevation levels for these hexes
        highest_elev = Integer.MIN_VALUE;;
        lowest_elev = Integer.MAX_VALUE;
        for (Coords c : validCoords) {
            int elev = board.getHex(c.x, c.y).getElevation();
            if (elev > highest_elev) {
                highest_elev = board.getHex(c.x, c.y).getElevation();
            }
            if (elev < lowest_elev) {
                lowest_elev = board.getHex(c.x, c.y).getElevation();
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

        for (Coords coord : validCoords) {
            // Calculate the fitness factor for each hex and save it to the
            // array
            // -> Absolute difference between hex elevation and ideal elevation
            // decreases fitness
            coord.fitness = -1
                    * (Math.abs(ideal_elev
                            - board.getHex(coord.x, coord.y).getElevation()));

            total_damage = 0.0;
            deployed_ent.setPosition(coord);
            // Create a list of potential attackers/targets for this location
            Vector<Entity> potentialAttackers = 
                    game.getValidTargets(deployed_ent); 
            valid_attackers = new Vector<Entity>(potentialAttackers.size());
            for (Entity e : potentialAttackers){
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
                            test_attack);
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
                            test_attack);
                    if (adjusted_damage > max_damage) {
                        max_damage = adjusted_damage;
                    }
                }
                total_damage += max_damage;
            }
            coord.fitness += (total_damage / 10);

            // Mech
            if (deployed_ent instanceof Mech) {
                // -> Trees are good
                // -> Water isn't that great below depth 1 -> this saves actual
                // ground space for infantry/vehicles (minor)
                int x = coord.x;
                int y = coord.y;
                if (board.getHex(x, y).containsTerrain(Terrains.WOODS)) {
                    coord.fitness += 1;
                }
                if (board.getHex(x, y).containsTerrain(Terrains.WATER)) {
                    if (board.getHex(x, y).depth() > 1) {
                        coord.fitness -= board.getHex(x, y).depth();
                    }
                }
                //If building, make sure not too heavy to safely move out of
                coord.fitness -= potentialBuildingDamage(coord.x, coord.y,
                        deployed_ent);
            }

            // Infantry

            if (deployed_ent instanceof Infantry) {
                // -> Trees and buildings make good cover, esp for conventional
                // infantry
                // rough is nice, too
                // -> Massed infantry is more effective, so try to cluster them
                if (board.getHex(coord.x, coord.y).containsTerrain(
                        Terrains.ROUGH)) {
                    coord.fitness += 1.5;
                }
                if (board.getHex(coord.x, coord.y).containsTerrain(
                        Terrains.WOODS)) {
                    coord.fitness += 2;
                }
                if (board.getHex(coord.x, coord.y).containsTerrain(
                        Terrains.BUILDING)) {
                    coord.fitness += 4;
                }
                highestHex = coord;
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
                IPlayer owner = deployed_ent.getOwner();
                for (int x = 0; x < 6 && !foundAdj; x++) {
                    highestHex = coord.translated(x);
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
                if (board.getHex(coord.x, coord.y).containsTerrain(
                        Terrains.WATER)) {
                    coord.fitness -= 10;
                }
            }

            // VTOL *PLACEHOLDER*
            // Currently, VTOLs are deployed as tanks, because they're a
            // sub-class.
            // This isn't correct in the long run, and eventually should be
            // fixed.
            // FIXME
            if (deployed_ent instanceof Tank) {
                // Tracked vehicle
                // -> Trees increase fitness
                if (deployed_ent.getMovementMode() == EntityMovementMode.TRACKED) {
                    if (board.getHex(coord.x, coord.y).containsTerrain(
                            Terrains.WOODS)) {
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
                    if (board.getHex(coord.x, coord.y).containsTerrain(
                            Terrains.WATER)) {
                        coord.fitness += 2;
                    }
                }
                // If building, make sure not too heavy to safely move out of.
                coord.fitness -= potentialBuildingDamage(coord.x, coord.y,
                        deployed_ent);
            }
            // ProtoMech
            // ->
            // -> Trees increase fitness by +2 (minor)
            if (deployed_ent instanceof Protomech) {
                if (board.getHex(coord.x, coord.y).containsTerrain(
                        Terrains.WOODS)) {
                    coord.fitness += 2;
                }
            }
        }
        // Now sort the valid array.
        Collections.sort(validCoords, new FitnessComparator());

        return validCoords;
    }

    private double potentialBuildingDamage(int x, int y, Entity entity) {
        Coords coords = new Coords(x, y);
        Building building = game.getBoard().getBuildingAt(coords);
        if (building == null) {
            return 0;
        }
        int potentialDmg = (int) Math.ceil((double) building.getCurrentCF(coords) / 10);
        double oddsTakeDmg = 1 - (Compute.oddsAbove(entity.getCrew().getPiloting()) / 100);
        return potentialDmg * oddsTakeDmg;
    }

    class FitnessComparator implements Comparator<Coords> {
        public int compare(Coords d1, Coords d2) {
            return -1 * Double.compare(d1.fitness, d2.fitness);
        }
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
    private static float getDeployDamage(IGame g, WeaponAttackAction waa) {
        Entity attacker = g.getEntity(waa.getEntityId());
        Mounted weapon = attacker.getEquipment(waa.getWeaponId());
        ToHitData hitData = waa.toHit(g);
        if (hitData.getValue() > 12) {
            return 0.0f;
        }

        float fChance;
        if (hitData.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            fChance = 1.0f;
        } else {
            fChance = (float) Compute.oddsAbove(hitData.getValue()) / 100.0f;
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
                    || (wt.getAmmoType() == AmmoType.T_MRM_STREAK)
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

        int total_bv, known_bv, known_range, known_count, trigger_range;
        int new_stealth = 1;
        Entity test_ent;

        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements(); ) {
            Entity check_ent = i.nextElement();
            if ((check_ent.getOwnerId() == localPlayerNumber)
                    && (check_ent instanceof Mech)) {
                if (check_ent.hasStealth()) {
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
                                trigger_range = 5;

                                for (Enumeration<Entity> all_units = game
                                        .getEntities(); all_units
                                             .hasMoreElements(); ) {
                                    test_ent = all_units.nextElement();
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
                                    if (known_count != 0) {
                                        if ((known_range / known_count) <= (5 + Compute
                                                .randomInt(5))) {
                                            new_stealth = 0;
                                        } else {
                                            new_stealth = 1;
                                        }
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

    public String getRandomBotMessage() {
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

    @Override
    public void retrieveServerInfo() {
        super.retrieveServerInfo();
        initialize();
    }

    /**
     * Pops up a dialog box showing an alert
     */
    public void doAlertDialog(String title, String message) {
        JTextPane textArea = new JTextPane();
        ReportDisplay.setupStylesheet(textArea);

        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        textArea.setText("<pre>" + message + "</pre>");
        JOptionPane.showMessageDialog(frame, scrollPane, title, JOptionPane.ERROR_MESSAGE);
    }
}
