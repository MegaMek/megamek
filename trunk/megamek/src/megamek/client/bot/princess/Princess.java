/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot.princess;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import megamek.client.bot.BotClient;
import megamek.client.bot.ChatProcessor;
import megamek.client.bot.PhysicalOption;
import megamek.client.bot.princess.FireControl.PhysicalAttackType;
import megamek.client.bot.princess.PathRanker.RankedPath;
import megamek.common.BuildingTarget;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.MechWarrior;
import megamek.common.Minefield;
import megamek.common.MovePath;
import megamek.common.Targetable;
import megamek.common.containers.PlayerIDandList;
import megamek.common.event.GameEvent;
import megamek.common.event.GamePlayerChatEvent;

public class Princess extends BotClient {

    private boolean initialized = false;
    public boolean verbose_errorlog;
    public int verbosity; // controls how many messages are sent to chat

    public String properties_file_name;

    double move_evaluation_time_estimate;
    Precognition precognition;
    Thread precognition_thread;
    // PathEnumerator path_enumerator;

    // ----These have to do with the goals or victiory conditions for the
    // bot----
    /*
     * A list of hexes in which the bot wants to destroy buildings
     */
    public ArrayList<Coords> strategic_targets = new ArrayList<Coords>();
    /*
     * Should the rules for forced withdrawal be implemented
     */
    public boolean forced_withdrawal = true;
    /*
     * Should the bot be running away
     */
    public boolean should_flee = false;
    // ---------------------------------------------------------------------------

    protected ChatProcessor chatp = new ChatProcessor();

    public Princess(String name, String host, int port) {
        super(name, host, port);
        // default properties file
        properties_file_name = new String("mmconf/princess_bot.properties");
        verbose_errorlog = true;
        verbosity = 1;
    }

    @Override
    protected Vector<Coords> calculateArtyAutoHitHexes() {
        // currently returns no artillery hit spots
        // make an empty list
        PlayerIDandList<Coords> artyAutoHitHexes = new PlayerIDandList<Coords>();
        // attach my player id to it
        artyAutoHitHexes.setPlayerID(getLocalPlayer().getId());
        return artyAutoHitHexes;
    }

    @Override
    protected void calculateDeployment() {

        // get the first unit
        int entNum = game.getFirstDeployableEntityNum();
        if (verbosity > 0) {
            sendChat("deploying unit " + getEntity(entNum).getChassis());
        }
        // on the list to be deployed
        // get a set of all the
        Coords[] cStart = getStartingCoordsArray();
        if (cStart.length == 0) {
            System.err.println("Error, no valid locations to deploy "
                    + getEntity(entNum).getChassis());
        }
        // get the coordinates I can deploy on
        Coords cDeploy = getCoordsAround(getEntity(entNum), cStart);
        if (cDeploy == null) {
            System.err.println("Error, getCoordsAround gave no location for "
                    + getEntity(entNum).getChassis());
        }
        // first coordinate that is legal to put this unit on now find some sort
        // of reasonable facing. If there are deployed enemies, face them
        int decent_facing = -1;
        for (Entity e : getEnemyEntities()) {
            if (e.isDeployed() && (!e.isOffBoard())) {
                decent_facing = cDeploy.direction(e.getPosition());
                break;
            }
        }
        // if I haven't found a decent facing, then at least face towards the
        // center of the board
        if (decent_facing == -1) {
            Coords center = new Coords(game.getBoard().getWidth() / 2, game
                    .getBoard().getHeight() / 2);
            decent_facing = cDeploy.direction(center);
        }
        deploy(entNum, cDeploy, decent_facing, 0);
    }

    @Override
    protected void calculateFiringTurn() {
        if (verbose_errorlog) {
            System.err.println("calculateFiringTurn called");
        }

        Entity shooter = game.getFirstEntity(getMyTurn()); // get the first
        // entity that can
        // act this turn
        // make sure weapons are loaded
        fire_control.loadAmmo(shooter, game);
        FireControl.FiringPlan plan = fire_control.getBestFiringPlan(shooter,
                game);
        if (plan != null) {
            System.err.println(plan.getDebugDescription(false));
            // tell the game I want to fire
            sendAttackData(shooter.getId(), plan.getEntityActionVector(game));

        } else {
            sendAttackData(shooter.getId(), null);
        }
        if (verbose_errorlog) {
            System.err.println("calculateFiringTurn returning");
        }
    }

    @Override
    protected Vector<Minefield> calculateMinefieldDeployment() {
        // currently returns no minefields
        // make an empty vector
        Vector<Minefield> deployedMinefields = new Vector<Minefield>();
        return deployedMinefields;
    }

    @Override
    protected MovePath calculateMoveTurn() {
        if (verbose_errorlog) {
            System.err.println("calculateMoveTurn called");
        }
        // first move useless units: immobile units, ejected mechwarrior, etc
        Entity moving_entity = null;
        Entity e = game.getFirstEntity();
        do {
            if (e.isImmobile()) {
                moving_entity = e;
                break;
            }
            if (e instanceof MechWarrior) {
                moving_entity = e;
                break;
            }
        } while ((e = game.getNextEntity(e.getId() + 1)) != game
                .getFirstEntity());
        // after that, moving farthest units first
        if (moving_entity == null) {
            double furthest_dist = 0;
            e = game.getFirstEntity();
            do {
                double dist = BasicPathRanker.distanceToClosestEnemy(e,
                        e.getPosition(), game);
                if ((moving_entity == null) || (dist > furthest_dist)) {
                    moving_entity = e;
                    furthest_dist = dist;
                }
            } while ((e = game.getNextEntity(e.getId() + 1)) != game
                    .getFirstEntity());
        }

        MovePath ret = continueMovementFor(moving_entity); // move it
        if (verbose_errorlog) {
            System.err.println("calculateMoveTurn returning");
        }
        return ret;
    }

    @Override
    protected PhysicalOption calculatePhysicalTurn() {
        if (verbose_errorlog) {
            System.err.println("calculatePhysicalTurn called");
        }
        // get the first entity that can act this turn
        Entity first_entity = game.getFirstEntity(getMyTurn());
        Entity hitter = first_entity;
        FireControl.PhysicalInfo best_attack = null;
        do {
            System.err.println("Calculating physical attacks for "
                    + hitter.getChassis());
            // this is an array of all my enemies
            ArrayList<Entity> enemies = getEnemyEntities();
            // cycle through potential enemies
            for (Entity e : enemies) {
                FireControl.PhysicalInfo right_punch = new FireControl.PhysicalInfo(
                        hitter, e, PhysicalAttackType.RIGHT_PUNCH, game);
                fire_control.calculateUtility(right_punch);
                if (right_punch.utility > 0) {
                    if ((best_attack == null)
                            || (right_punch.utility > best_attack.utility)) {
                        best_attack = right_punch;
                    }
                }
                FireControl.PhysicalInfo left_punch = new FireControl.PhysicalInfo(
                        hitter, e, PhysicalAttackType.LEFT_PUNCH, game);
                fire_control.calculateUtility(left_punch);
                if (left_punch.utility > 0) {
                    if ((best_attack == null)
                            || (left_punch.utility > best_attack.utility)) {
                        best_attack = left_punch;
                    }
                }
                FireControl.PhysicalInfo right_kick = new FireControl.PhysicalInfo(
                        hitter, e, PhysicalAttackType.RIGHT_KICK, game);
                if (right_kick.utility > 0) {
                    if ((best_attack == null)
                            || (right_kick.utility > best_attack.utility)) {
                        best_attack = right_kick;
                    }
                }
                FireControl.PhysicalInfo left_kick = new FireControl.PhysicalInfo(
                        hitter, e, PhysicalAttackType.LEFT_KICK, game);
                if (left_kick.getExpectedDamage() > 0) {
                    if ((best_attack == null)
                            || (left_kick.utility > best_attack.utility)) {
                        best_attack = left_kick;
                    }
                }

            }
            if (best_attack != null) {
                System.err.println("Attack is a "
                        + best_attack.attack_type.name());
            } else {
                System.err.println("No useful attack to be made");
            }
            if (best_attack != null) {
                if (verbose_errorlog) {
                    System.err
                            .println("calculatePhysicalTurn returning (with an attack)");
                }
                return best_attack.getAsPhysicalOption();
            }
            hitter = game.getNextEntity(hitter.getId() + 1);
            // otherwise, check if the next entity can hit something
            if (hitter == first_entity) {
                hitter = null; // getNextEntity is incorrect, it does not return
                // null at the end, it returns the first entity
            }
        } while (hitter != null);
        if (verbose_errorlog) {
            System.err.println("calculatePhysicalTurn returning (no attacks)");
        }
        // no one can hit anything anymore, so give up
        return null;
    }

    @Override
    protected MovePath continueMovementFor(Entity entity) {
        // figure out who moved last, and who's move lists need to be updated

        // moves this entity during movement phase
        System.err.println("Moving " + entity.getDisplayName() + " (ID "
                + entity.getId() + ")");
        precognition.insureUpToDate();

        if (entity.isCrippled()) {
            System.err.println(entity.getDisplayName()
                    + " is crippled and withdrawing");
            sendChat(entity.getDisplayName()
                    + " is crippled and withdrawing.");
        }
        // precognition.path_enumerator.debugPrintContents();

        ArrayList<MovePath> paths = precognition.path_enumerator.unit_paths
                .get(entity.getId());

        if (paths == null) {
            System.err.println("Warning: no valid paths found");
            return new MovePath(game, entity);
        }
        double this_time_estimate = (paths.size() * move_evaluation_time_estimate) / 1e3;
        if (verbosity > 0) {
            String timeestimate = "unknown.";
            if (this_time_estimate != 0) {
                timeestimate = Integer.toString((int) this_time_estimate)
                        + " seconds";
            }
            String message = "Moving " + entity.getChassis() + ". "
                    + Long.toString(paths.size())
                    + " paths to consider.  Estimated time to completion: "
                    + timeestimate;
            sendChat(message);
        }
        long start_time = System.currentTimeMillis();
        path_ranker.initUnitTurn(entity, game);
        ArrayList<RankedPath> rankedpaths = path_ranker.rankPaths(paths, game);
        long stop_time = System.currentTimeMillis();
        // update path evaluation time estimate
        double updated_estimate = ((double) (stop_time - start_time))
                / ((double) paths.size());
        if (move_evaluation_time_estimate == 0) {
            move_evaluation_time_estimate = updated_estimate;
        }
        move_evaluation_time_estimate = 0.5 * (updated_estimate + move_evaluation_time_estimate);
        if (rankedpaths.size() == 0) {
            return new MovePath(game, entity);
        }
        System.err.println("Path ranking took "
                + Long.toString(stop_time - start_time) + " milliseconds");
        precognition.unpause();
        RankedPath bestpath = PathRanker.getBestPath(rankedpaths);
        // bestpath.path.printAllSteps();
        return bestpath.path;
    }

    @Override
    protected void initFiring() {

        // ----Debugging: print out any errors made in guessing to hit
        // values-----
        Vector<Entity> ents = game.getEntitiesVector();
        for (Entity ent : ents) {
            String errors = fire_control.checkAllGuesses(ent, game);
            if (errors != null) {
                System.err.println(errors);
            }
        }
        // -----------------------------------------------------------------------
    }

    @Override
    protected void initMovement() {
        // reset strategic targets
        fire_control.additional_targets = new ArrayList<Targetable>();
        for (int i = 0; i < strategic_targets.size(); i++) {
            if (game.getBoard().getBuildingAt(strategic_targets.get(i)) == null) {
                sendChat("No building to target in Hex "
                        + strategic_targets.get(i).toFriendlyString()
                        + ", ignoring.");
            } else {
                fire_control.additional_targets.add(new BuildingTarget(
                        strategic_targets.get(i), game.getBoard(), false));
                sendChat("Building in Hex "
                        + strategic_targets.get(i).toFriendlyString()
                        + " designated strategic target.");
            }
        }
    }

    @Override
    public void initialize() {
        if (initialized)
         {
            return; // no need to initialize twice
        }
        Properties configfile = new Properties();
        try {
            configfile.load(new FileInputStream(properties_file_name));
            System.err.println("loading behavior from " + properties_file_name);
        } catch (FileNotFoundException e) {
            System.err.println("Error!  Princess config file not found!");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO Error in Princess config file!");
            e.printStackTrace();
        }
        path_searcher = new PathSearcher();
        path_ranker = new BasicPathRanker(configfile);
        path_ranker.botbase = this;
        path_searcher.ranker = path_ranker;
        fire_control = new FireControl();
        path_ranker.firecontrol = fire_control;
        precognition = new Precognition();
        precognition.setGame(game);
        path_ranker.path_enumerator = precognition.path_enumerator;

        System.err.println("princess initialize called");
        precognition_thread = new Thread(precognition, "Princess-precognition");
        precognition_thread.start();
        // precognition.pause();
        initialized = true;
        BotGeometry.debugSelfTest();
    }

    @Override
    protected void processChat(GamePlayerChatEvent ge) {
        System.err.println("received message: \"" + ge.getMessage() + "\"");
        System.err.println("message type: " + ge.getType());
        if (ge.getType() == GameEvent.GAME_PLAYER_CHAT) {
            StringTokenizer st = new StringTokenizer(ge.getMessage(), ":"); //$NON-NLS-1$
            String name = st.nextToken();
            String message = st.nextToken();
            if (message == null) {
                return;
            }
            if (message.contains("flee")) {
                System.err.println("received flee order");
                sendChat("Run Away!");
                should_flee = true;
            }
        }

        chatp.processChat(ge, this);

    }

    PathSearcher path_searcher;
    BasicPathRanker path_ranker;
    FireControl fire_control;

}
