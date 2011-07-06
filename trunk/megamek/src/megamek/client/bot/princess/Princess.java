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
import java.util.Vector;

import megamek.client.bot.BotClient;
import megamek.client.bot.PhysicalOption;
import megamek.client.bot.princess.FireControl.PhysicalAttackType;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Minefield;
import megamek.common.MovePath;
import megamek.common.containers.PlayerIDandList;
import megamek.common.event.GamePlayerChatEvent;

public class Princess extends BotClient {
	
	public boolean verbose_errorlog;

    public String properties_file_name;

    public Princess(String name, String host, int port) {
        super(name, host, port);

        properties_file_name = new String("mmconf/princess_bot.properties"); // default
                                                                             // properties
                                                                             // file
        verbose_errorlog=true;
    }

    @Override
    protected Vector<Coords> calculateArtyAutoHitHexes() { // currently returns
                                                           // no artillery hit
                                                           // spots
        PlayerIDandList<Coords> artyAutoHitHexes = new PlayerIDandList<Coords>(); // make
                                                                                  // an
                                                                                  // empty
                                                                                  // list
        artyAutoHitHexes.setPlayerID(getLocalPlayer().getId()); // attach my
                                                                // player id to
                                                                // it
        return artyAutoHitHexes;
    }

    @Override
    protected void calculateDeployment() {
        int entNum = game.getFirstDeployableEntityNum(); // get the first unit
                                                         // on the list to be
                                                         // deployed
        Coords[] cStart = getStartingCoordsArray(); // get a set of all the
                                                    // coordinates I can deploy
                                                    // on
        Coords cDeploy = getCoordsAround(getEntity(entNum), cStart); // get the
                                                                     // first
                                                                     // coordinate
                                                                     // that is
                                                                     // legal to
                                                                     // put this
                                                                     // unit on
        // now find some sort of reasonable facing. If there are deployed
        // enemies, face them
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
    	if(verbose_errorlog) System.err.println("calculateFiringTurn called");

        Entity shooter = game.getFirstEntity(getMyTurn()); // get the first
                                                           // entity that can
                                                           // act this turn
        FireControl.FiringPlan plan = fire_control.getBestFiringPlan(shooter,
                game);
        System.err.println(plan.getDebugDescription(false));
        sendAttackData(shooter.getId(), plan.getEntityActionVector(game)); // tell
                                                                           // the
                                                                           // game
                                                                           // I
                                                                           // want
                                                                           // to
                                                                           // fire
        if(verbose_errorlog) System.err.println("calculateFiringTurn returning");
    }

    @Override
    protected Vector<Minefield> calculateMinefieldDeployment() { // currently
                                                                 // returns no
                                                                 // minefields
        Vector<Minefield> deployedMinefields = new Vector<Minefield>(); // make
                                                                        // an
                                                                        // empty
                                                                        // vector
        return deployedMinefields;
    }

    @Override
    protected MovePath calculateMoveTurn() {
    	if(verbose_errorlog) System.err.println("calculateMoveTurn called");
        Entity e = game.getFirstEntity(getMyTurn()); // get the first entity
                                                     // that can act this turn
        MovePath ret=continueMovementFor(e); // move it
        if(verbose_errorlog) System.err.println("calculateMoveTurn returning");
        return ret;
    }

    @Override
    protected PhysicalOption calculatePhysicalTurn() {
    	if(verbose_errorlog) System.err.println("calculatePhysicalTurn called");
        Entity first_entity = game.getFirstEntity(getMyTurn()); // get the first
                                                                // entity that
                                                                // can act this
                                                                // turn
        Entity hitter = first_entity;
        FireControl.PhysicalInfo best_attack = null;
        do {
            System.err.println("Calculating physical attacks for "
                    + hitter.getChassis());
            ArrayList<Entity> enemies = getEnemyEntities(); // this is an array
                                                            // of all my enemies
            for (Entity e : enemies) { // cycle through potential enemies
                FireControl.PhysicalInfo right_punch = new FireControl.PhysicalInfo(
                        hitter, e, PhysicalAttackType.RIGHT_PUNCH, game);
                fire_control.calculateUtility(right_punch);
                if (right_punch.utility > 0) {
                    if ((best_attack == null)
                            || (right_punch.utility > best_attack
                                    .utility)) {
                        best_attack = right_punch;
                    }
                }
                FireControl.PhysicalInfo left_punch = new FireControl.PhysicalInfo(
                        hitter, e, PhysicalAttackType.LEFT_PUNCH, game);
                fire_control.calculateUtility(left_punch);
                if (left_punch.utility > 0) {
                    if ((best_attack == null)
                            || (left_punch.utility > best_attack
                                    .utility)) {
                        best_attack = left_punch;
                    }
                }
                FireControl.PhysicalInfo right_kick = new FireControl.PhysicalInfo(
                        hitter, e, PhysicalAttackType.RIGHT_KICK, game);
                if (right_kick.utility > 0) {
                    if ((best_attack == null)
                            || (right_kick.utility > best_attack
                                    .utility)) {
                        best_attack = right_kick;
                    }
                }
                FireControl.PhysicalInfo left_kick = new FireControl.PhysicalInfo(
                        hitter, e, PhysicalAttackType.LEFT_KICK, game);
                if (left_kick.getExpectedDamage() > 0) {
                    if ((best_attack == null)
                            || (left_kick.utility > best_attack
                                    .utility)) {
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
            	if(verbose_errorlog) System.err.println("calculatePhysicalTurn returning (with an attack)");
                return best_attack.getAsPhysicalOption();
            }
            hitter = game.getNextEntity(hitter.getId() + 1); // otherwise, check
                                                             // if the next
                                                             // entity can hit
                                                             // something
            if (hitter == first_entity) {
                hitter = null; // getNextEntity is incorrect, it does not return
                               // null at the end, it returns the first entity
            }
        } while (hitter != null);
        if(verbose_errorlog) System.err.println("calculatePhysicalTurn returning (no attacks)");
        return null; // no one can hit anything anymore, so give up
    }

    @Override
    protected MovePath continueMovementFor(Entity entity) { // moves this entity
                                                            // during movement
                                                            // phase
        // MovePath ret = new MovePath(game,entity); //generate a path
        // consisting of doing nothing
        // ret.addStep(MovePath.MoveStepType.FORWARDS); //add one step forwards
        // to that
        // return ret; //and go
        return path_searcher.getBestPath(entity, game);
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
    }

    @Override
    public void initialize() {
        Properties configfile = new Properties();
        try {
            configfile.load(new FileInputStream(properties_file_name));
            System.err.println("loading behavior from " + properties_file_name);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            System.err.println("Error!  Princess config file not found!");
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        path_searcher = new PathSearcher();
        BasicPathRanker ranker = new BasicPathRanker(configfile);
        path_searcher.ranker = ranker;
        fire_control = new FireControl();
        ranker.firecontrol = fire_control;
    }

    @Override
    protected void processChat(GamePlayerChatEvent ge) {
        // TODO Auto-generated method stub

    }

    PathSearcher path_searcher;
    FireControl fire_control;

}
