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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import megamek.client.Client;
import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.GameTurn;
import megamek.common.IEntityMovementMode;
import megamek.common.IGame;
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

public abstract class BotClient extends Client {

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
                if (isMyTurn()) {
                    calculateMyTurn();
                    flushConn();
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

    public ArrayList<Entity> getEntitiesOwned() {
        ArrayList<Entity> result = new ArrayList<Entity>();
        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = i.nextElement();
            if (entity.getOwner().equals(getLocalPlayer())
                    && entity.getPosition() != null && !entity.isOffBoard()) {
                result.add(entity);
            }
        }
        return result;
    }

    public ArrayList<Entity> getEnemyEntities() {
        ArrayList<Entity> result = new ArrayList<Entity>();
        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = i.nextElement();
            if (entity.getOwner().isEnemyOf(getLocalPlayer())
                    && entity.getPosition() != null && !entity.isOffBoard()) {
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
                        && game.getEntitiesOwnedBy(getLocalPlayer())
                                - game.getNoOfEntities() == 0) {
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
                break;
            }
        } catch (Throwable t) {
            t.printStackTrace();
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

    protected void calculateMyTurn() {
        try {
            if (game.getPhase() == IGame.Phase.PHASE_MOVEMENT) {
                MovePath mp = null;
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
                    sendAttackData(getLocalPlayer().getId(),
                            new Vector<EntityAction>(0));
                }
            } else if (game.getPhase() == IGame.Phase.PHASE_DEPLOYMENT) {
                calculateDeployment();
            } else if (game.getPhase() == IGame.Phase.PHASE_DEPLOY_MINEFIELDS) {
                Vector<Minefield> mines = calculateMinefieldDeployment();
                for (int i = 0; i < mines.size(); i++) {
                    game.addMinefield(mines.get(i));
                }
                sendDeployMinefields(mines);
                sendPlayerInfo();
            } else if (game.getPhase() == IGame.Phase.PHASE_SET_ARTYAUTOHITHEXES) {
                // For now, declare no autohit hexes.
                Vector<Coords> autoHitHexes = calculateArtyAutoHitHexes();
                sendArtyAutoHitHexes(autoHitHexes);
            } else if (game.getPhase() == IGame.Phase.PHASE_TARGETING
                    || game.getPhase() == IGame.Phase.PHASE_OFFBOARD) {
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
    protected Coords getCoordsAround(Entity deploy_me, Coords[] c) {
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
                    .hasMoreElements();) {
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
        Coords[] calc = getStartingCoordsArray();
        if (calc != null) {
            if (calc.length > 0) {
                return calc[0];
            }
        }
        return null;
    }

    protected Coords[] getStartingCoordsArray() {
        int test_x, test_y, highest_elev, lowest_elev;
        int counter, valid_arr_index, arr_x_index;
        int weapon_count;

        double av_range, best_fitness, ideal_elev;
        // double[] fitness;
        double adjusted_damage, max_damage, total_damage;

        Coords highest_hex = new Coords();
        Coords test_hex = new Coords();
        Coords[] valid_array;

        Entity test_ent, deployed_ent;

        Vector<Entity> valid_attackers;

        deployed_ent = getEntity(game.getFirstDeployableEntityNum());

        WeaponAttackAction test_attack;

        // Create array of hexes in the deployment zone that can be deployed to
        // Check for prohibited terrain, stacking limits

        switch (getLocalPlayer().getStartingPos()) {
        case 1:
        case 3:
        case 5:
        case 7:
            valid_array = new Coords[(3 * game.getBoard().getWidth())
                    + (3 * game.getBoard().getHeight()) - 9];
            // fitness = new
            // double[(3*game.getBoard().getWidth())+(3*game.getBoard().getHeight())-9];
            break;
        case 2:
        case 6:
            valid_array = new Coords[game.getBoard().getWidth() * 3];
            // fitness = new double[game.getBoard().getWidth()*3];
            break;
        case 4:
        case 8:
            valid_array = new Coords[game.getBoard().getHeight() * 3];
            // fitness = new double[game.getBoard().getHeight()*3];
            break;
        case 0:
        default:
            valid_array = new Coords[game.getBoard().getWidth()
                    * game.getBoard().getHeight()];
            // fitness = new
            // double[game.getBoard().getWidth()*game.getBoard().getHeight()];
            break;
        }

        counter = 0;
        for (test_x = 0; test_x <= game.getBoard().getWidth(); test_x++) {
            for (test_y = 0; test_y <= game.getBoard().getHeight(); test_y++) {
                test_hex.x = test_x;
                test_hex.y = test_y;
                if (game.getBoard().isLegalDeployment(test_hex,
                        getLocalPlayer())) {
                    if (!deployed_ent.isHexProhibited(game.getBoard().getHex(
                            test_hex.x, test_hex.y))) {
                        valid_array[counter] = new Coords(test_hex);
                        counter++;
                    }
                }
            }
        }

        // Randomize hexes so hexes are not in order
        // This is to prevent clumping at the upper-left corner on very flat
        // maps

        for (valid_arr_index = 0; valid_arr_index < counter; valid_arr_index++) {
            arr_x_index = Compute.randomInt(counter);
            if (arr_x_index < 0) {
                arr_x_index = 0;
            }
            test_hex = valid_array[valid_arr_index];
            valid_array[valid_arr_index] = valid_array[arr_x_index];
            valid_array[arr_x_index] = test_hex;
        }
        // copy valid hexes into a new array of the correct size,
        // so we don't return an array that contains null Coords
        Coords[] valid_new = new Coords[counter];
        for (int i = 0; i < counter; i++) {
            valid_new[i] = valid_array[i];
        }
        valid_array = valid_new;

        // Now get minimum and maximum elevation levels for these hexes

        highest_elev = -100;
        lowest_elev = 100;
        for (valid_arr_index = 0; valid_arr_index < counter; valid_arr_index++) {
            if (game.getBoard().getHex(valid_array[valid_arr_index].x,
                    valid_array[valid_arr_index].y).getElevation() > highest_elev) {
                highest_elev = game.getBoard().getHex(
                        valid_array[valid_arr_index].x,
                        valid_array[valid_arr_index].y).getElevation();
            }
            if (game.getBoard().getHex(valid_array[valid_arr_index].x,
                    valid_array[valid_arr_index].y).getElevation() < lowest_elev) {
                lowest_elev = game.getBoard().getHex(
                        valid_array[valid_arr_index].x,
                        valid_array[valid_arr_index].y).getElevation();
            }
        }

        // Calculate average range of all weapons
        // Do not include ATMs, but DO include each bin of ATM ammo
        // Increase average range if the unit has an active c3 link

        av_range = 0.0;
        weapon_count = 0;
        for (Mounted mounted : deployed_ent.getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            if ((wtype.getName() != "ATM 3") && (wtype.getName() != "ATM 6")
                    && (wtype.getName() != "ATM 9")
                    && (wtype.getName() != "ATM 12")) {
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
        // highest elevation

        ideal_elev = lowest_elev
                + ((av_range / 18) * (highest_elev - lowest_elev));
        if (ideal_elev > highest_elev) {
            ideal_elev = highest_elev;
        }

        best_fitness = -100.0;
        for (valid_arr_index = 0; valid_arr_index < counter; valid_arr_index++) {

            // Calculate the fitness factor for each hex and save it to the
            // array
            // -> Absolute difference between hex elevation and ideal elevation
            // decreases fitness

            valid_array[valid_arr_index].fitness = -1
                    * (Math.abs(ideal_elev
                            - game.getBoard().getHex(
                                    valid_array[valid_arr_index].x,
                                    valid_array[valid_arr_index].y)
                                    .getElevation()));

            // -> Approximate total damage taken in the current position; this
            // keeps units from deploying into x-fires
            total_damage = 0.0;
            deployed_ent.setPosition(valid_array[valid_arr_index]);
            valid_attackers = game.getValidTargets(deployed_ent);
            for (Enumeration<Entity> i = valid_attackers.elements(); i
                    .hasMoreElements();) {
                test_ent = i.nextElement();
                if (test_ent.isDeployed() == true && !test_ent.isOffBoard()) {
                    for (Mounted mounted : test_ent.getWeaponList()) {
                        test_attack = new WeaponAttackAction(test_ent.getId(),
                                deployed_ent.getId(), test_ent
                                        .getEquipmentNum(mounted));
                        adjusted_damage = getDeployDamage(game, test_attack);
                        total_damage += adjusted_damage;
                    }

                }

            }

            valid_array[valid_arr_index].fitness -= (total_damage / 10);

            // -> Find the best target for each weapon and approximate the
            // damage; maybe we can kill stuff without moving!
            // -> Conventional infantry ALWAYS come out on the short end of the
            // stick in damage given/taken... solutions?

            total_damage = 0.0;
            for (Mounted mounted : deployed_ent.getWeaponList()) {
                max_damage = 0.0;
                for (Enumeration<Entity> j = valid_attackers.elements(); j
                        .hasMoreElements();) {
                    test_ent = j.nextElement();
                    if (test_ent.isDeployed() == true && !test_ent.isOffBoard()) {
                        test_attack = new WeaponAttackAction(deployed_ent
                                .getId(), test_ent.getId(), deployed_ent
                                .getEquipmentNum(mounted));
                        adjusted_damage = getDeployDamage(game, test_attack);
                        if (adjusted_damage > max_damage) {
                            max_damage = adjusted_damage;
                        }
                    }
                }
                total_damage += max_damage;
            }
            valid_array[valid_arr_index].fitness += (total_damage / 10);

            // Mech

            if (deployed_ent instanceof Mech) {
                // -> Trees are good
                // -> Water isn't that great below depth 1 -> this saves actual
                // ground space for infantry/vehicles (minor)

                if (game.getBoard().getHex(valid_array[valid_arr_index].x,
                        valid_array[valid_arr_index].y).containsTerrain(
                        Terrains.WOODS)) {
                    valid_array[valid_arr_index].fitness += 1;
                }
                if (game.getBoard().getHex(valid_array[valid_arr_index].x,
                        valid_array[valid_arr_index].y).containsTerrain(
                        Terrains.WATER)) {
                    if (game.getBoard().getHex(valid_array[valid_arr_index].x,
                            valid_array[valid_arr_index].y).depth() > 1) {
                        valid_array[valid_arr_index].fitness -= game.getBoard()
                                .getHex(valid_array[valid_arr_index].x,
                                        valid_array[valid_arr_index].y).depth();
                    }
                }
            }

            // Infantry

            if (deployed_ent instanceof Infantry) {
                // -> Trees and buildings make good cover, esp for conventional
                // infantry
                // rough is nice, to
                // -> Massed infantry is more effective, so try to cluster them

                if (game.getBoard().getHex(valid_array[valid_arr_index].x,
                        valid_array[valid_arr_index].y).containsTerrain(
                        Terrains.ROUGH)) {
                    valid_array[valid_arr_index].fitness += 1.5;
                }
                if (game.getBoard().getHex(valid_array[valid_arr_index].x,
                        valid_array[valid_arr_index].y).containsTerrain(
                        Terrains.WOODS)) {
                    valid_array[valid_arr_index].fitness += 2;
                }
                if (game.getBoard().getHex(valid_array[valid_arr_index].x,
                        valid_array[valid_arr_index].y).containsTerrain(
                        Terrains.BUILDING)) {
                    valid_array[valid_arr_index].fitness += 4;
                }
                highest_hex = valid_array[valid_arr_index];
                Enumeration<Entity> ent_list = game.getEntities(highest_hex);
                while (ent_list.hasMoreElements()) {
                    test_ent = ent_list.nextElement();
                    if (deployed_ent.getOwner() == test_ent.getOwner()
                            && !deployed_ent.equals(test_ent)) {
                        if (test_ent instanceof Infantry) {
                            valid_array[valid_arr_index].fitness += 2;
                            break;
                        }
                    }
                }
                outer_loop: for (int x = 0; x < 6; x++) {
                    highest_hex = valid_array[valid_arr_index];
                    highest_hex = highest_hex.translated(x);
                    Enumeration<Entity> adj_ents = game
                            .getEntities(highest_hex);
                    while (adj_ents.hasMoreElements()) {
                        test_ent = adj_ents.nextElement();
                        if (deployed_ent.getOwner() == test_ent.getOwner()
                                && !deployed_ent.equals(test_ent)) {
                            if (test_ent instanceof Infantry) {
                                valid_array[valid_arr_index].fitness += 1;
                                break outer_loop;
                            }
                        }
                    }
                }

                // Not sure why bot tries to deploy infantry in water, it SHOULD
                // be caught by the isHexProhibited method when
                // selecting hexes, but sometimes it has a mind of its own so...
                if (game.getBoard().getHex(valid_array[valid_arr_index].x,
                        valid_array[valid_arr_index].y).containsTerrain(
                        Terrains.WATER)) {
                    valid_array[valid_arr_index].fitness -= 10;
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
                if (deployed_ent.getMovementMode() == IEntityMovementMode.TRACKED) {
                    if (game.getBoard().getHex(valid_array[valid_arr_index].x,
                            valid_array[valid_arr_index].y).containsTerrain(
                            Terrains.WOODS)) {
                        valid_array[valid_arr_index].fitness += 2;
                    }
                }

                // Wheeled vehicle
                // -> Not sure what any benefits wheeled vehicles can get; for
                // now, just elevation and damage taken/given
                // Hover vehicle
                // -> Water in hex increases fitness, hover vehicles have an
                // advantage in water areas
                if (deployed_ent.getMovementMode() == IEntityMovementMode.HOVER) {
                    if (game.getBoard().getHex(valid_array[valid_arr_index].x,
                            valid_array[valid_arr_index].y).containsTerrain(
                            Terrains.WATER)) {
                        valid_array[valid_arr_index].fitness += 2;
                    }
                }

            }
            // ProtoMech
            // ->
            // -> Trees increase fitness by +2 (minor)

            if (deployed_ent instanceof Protomech) {
                if (game.getBoard().getHex(valid_array[valid_arr_index].x,
                        valid_array[valid_arr_index].y).containsTerrain(
                        Terrains.WOODS)) {
                    valid_array[valid_arr_index].fitness += 2;
                }
            }

            // Record the highest fitness factor

            if (valid_array[valid_arr_index].fitness > best_fitness) {
                best_fitness = valid_array[valid_arr_index].fitness;
            }
        }

        // Now sort the valid array.
        // We're just going to trust Java to not suck at this.
        Arrays.sort(valid_array, new FitnessComparator());

        return valid_array;
    }

    class FitnessComparator implements Comparator<Coords> {
        public int compare(Coords d1, Coords d2) {
            return -1 * Double.compare(d1.fitness, d2.fitness);
        }
    }

    // Missile hits table
    // Some of these are interpolated for odd weapons sizes found in Protos and
    // new BAs
    private static float[] expectedHitsByRackSize = { 0.0f, 1.0f, 1.58f, 2.0f,
            2.63f, 3.17f, 4.0f, 4.49f, 4.98f, 5.47f, 6.31f, 7.23f, 8.14f,
            8.59f, 9.04f, 9.5f, 0.0f, 0.0f, 0.0f, 0.0f, 12.7f };

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
        if (hitData.getValue() == TargetRoll.IMPOSSIBLE
                || hitData.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            return 0.0f;
        }

        float fChance = 0.0f;
        if (hitData.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            fChance = 1.0f;
        } else {
            fChance = (float) Compute.oddsAbove(hitData.getValue()) / 100.0f;
        }

        // TODO : update for BattleArmor.

        float fDamage = 0.0f;
        WeaponType wt = (WeaponType) weapon.getType();
        if (wt.getDamage() == WeaponType.DAMAGE_MISSILE) {
            if (weapon.getLinked() == null) {
                return 0.0f;
            }
            AmmoType at = (AmmoType) weapon.getLinked().getType();

            float fHits = 0.0f;
            if ((wt.getAmmoType() == AmmoType.T_SRM_STREAK)
                    || (wt.getAmmoType() == AmmoType.T_MRM_STREAK)
                    || (wt.getAmmoType() == AmmoType.T_LRM_STREAK)) {
                fHits = wt.getRackSize();
            } else if (wt.getRackSize() == 40 || wt.getRackSize() == 30) {
                fHits = 2.0f * expectedHitsByRackSize[wt.getRackSize() / 2];
            } else {
                fHits = expectedHitsByRackSize[wt.getRackSize()];
            }
            // adjust for previous AMS
            ArrayList<Mounted> vCounters = waa.getCounterEquipment();
            if (vCounters != null) {
                for (int x = 0; x < vCounters.size(); x++) {
                    EquipmentType type = vCounters.get(x).getType();
                    if (type instanceof WeaponType
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

        for (Enumeration<Entity> i = game.getEntities(); i.hasMoreElements();) {
            Entity check_ent = i.nextElement();
            if ((check_ent.getOwnerId() == local_pn)
                    && (check_ent instanceof Mech)) {
                if (((Mech) check_ent).hasStealth()) {
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
                                        .hasMoreElements();) {
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
}
