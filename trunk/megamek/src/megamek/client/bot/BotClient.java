/**
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

package megamek.client.bot;

import megamek.client.*;
import java.util.*;

import megamek.common.*;
import megamek.common.actions.*;

/**
 * Megamek client that implements an AI player.
 * 
 * @author hanson
 */

 /*
     Turn calculation strategy:
    
     the basic technique is a limited minimax, with some heuristics
     to keep the game moving.
     
     the point of each turn is to maximize the amount of
     damage inflicted on enemy mechs while minimizing
     the amount of damage received.  the relative weight
     given to each of these (damage potential vs. threat potential)
     can be adjusted to change the aggressiveness of the bot.
     
     to select a move for a mech, the algorithm is to calculate a threat
     and damage potential for every possible move the mech could make.
     (note that because there are multiple ways of moving to each
     hex, e.g. walking and jumping, it's not enough to just calculate
     the threat for each hex).  might also need to check for every
     torso twist combo.
     
     threat potential is calculated as:
      - for each enemy that has already moved, the expected damage of
        an optimally damaging attack against me (ignoring their heat
        calculations for now -- this would be a worst case scenario)
      - for each enemy that has not already moved, the expected damage
        of the optimally damaging move-and-attack, assuming that I took
        the current move under consideration.
    
     damage potential is calculated as:
      - for every weapon I have, calculate the optimally damaging attack
        that I could make with it (that is, the target that has the best
        expected value of damage)
      - rank order all of my weapons according to damage potential
      - select weapons until my heat limit is reached (counting in
        heat acquired through the move I would have made for this hex,
        minus heat sunk through normal and water-enhanced cooling);
        keep running the list in case there are zero-heat weapons
     
    the output of CalculateMoveOptions is a list of moves, each with
     a threat and damage potential.
     
     
     potential tweaks:
      - damage potential for an attack could be enhanced if the target
        was particularly vulnerable; this would encourage finishing off
        a wounded target
     
     
     potential problems:
      - the damage potential algorithm assumes that not-yet-moved enemies
        will make the move that causes optimal damage to me, rather than
        the move that is safest for them; this could lead to lots of ineffective
        moves.     
     */
     
public class BotClient extends Client
{
    public BotClient(String playerName)
    {
        super(playerName);
    }
    
    protected void changePhase(int phase) {
        super.changePhase(phase);
        
        switch(phase) {
            case Game.PHASE_LOUNGE :
                notifyOfBot();
                // let the human set the bot up
                break;
                
                // just skip over the report screens
            case Game.PHASE_INITIATIVE :
            case Game.PHASE_MOVEMENT_REPORT :
            case Game.PHASE_FIRING_REPORT :
            case Game.PHASE_END :
                sendDone(true);
                break;
        }
    }
    
    private void notifyOfBot() {
        new AlertDialog(frame, "Please read the ai-readme.txt", "The bot does not work with all units or game options.  Please read the ai-readme.txt file before using the bot.").show();
        sendChat("Hi, I'm a bot client!");
    }
    
    protected void processGameEvent(GameEvent ge) {
        super.processGameEvent(ge);
        
        switch(ge.getType()) {
            case GameEvent.GAME_PLAYER_CHAT :
                break;
            case GameEvent.GAME_PLAYER_STATUSCHANGE :
                break;
            case GameEvent.GAME_PHASE_CHANGE :
                break;
            case GameEvent.GAME_TURN_CHANGE :
                if (isMyTurn()) {
                    calculateMyTurn();
                }
                break;
            case GameEvent.GAME_NEW_ENTITIES :
                break;
            case GameEvent.GAME_NEW_SETTINGS :
                break;
        }
    }
    
     protected void calculateMyTurn() {
         // gross hack: create a TurnCalculator object in changePhase instead
        if (curPanel instanceof MovementDisplay) {
            // sendChat("Calculating my movement turn.");
            calculateMoveTurn();
        } else if (curPanel instanceof FiringDisplay) {
            // sendChat("Calculating my firing turn.");
            calculateFiringTurn();
        } else if (curPanel instanceof PhysicalDisplay) {
            // sendChat("Calculating my physical turn.");
            calculatePhysicalTurn();
        } else if (curPanel instanceof DeploymentDisplay) {
            calculateDeployment();
        }     
     }
     
     private void calculateDeployment() {
        // Use the old clumping algorithm until someone puts AI in here
        int entNum = game.getFirstDeployableEntityNum();
        Coords cStart = getStartingCoords(getLocalPlayer().getStartingPos());
        Coords cDeploy = getCoordsAround(cStart);
        
        Coords cCenter = new Coords(game.board.width / 2, game.board.height / 2);
        int nDir;
        if (getLocalPlayer().getStartingPos() != 0) {
            // face towards center if you aren't already there
            nDir = cDeploy.direction(cCenter);
        } else {
            // otherwise, face away
            nDir = cCenter.direction(cDeploy);
        }
        
        deploy(entNum, cDeploy, nDir);
     }
        
     /**
      * Gets valid & empty starting coords around the specified point
      */
     private Coords getCoordsAround(Coords c)
     {
        // check the requested coords
        if (game.board.isLegalDeployment(c, this.getLocalPlayer())
        && game.getFirstEntity(c) == null) {
            return c;
        }
        
        // check the surrounding coords
        for (int x = 0; x < 6; x++) {
            Coords c2 = c.translated(x);
            if (game.board.isLegalDeployment(c2, this.getLocalPlayer())
            && game.getFirstEntity(c2) == null) {
                return c2;
            }
        }
        
        // recurse in a random direction
        return getCoordsAround(c.translated(Compute.randomInt(6)));
     }
     
     private Coords getStartingCoords(int nPos) {
        switch (nPos) {
            default :
            case 0 :
                return new Coords(game.board.width / 2, game.board.height / 2);
            case 1 :
                return new Coords(1, 1);
            case 2 :
                return new Coords(game.board.width / 2, 1);
            case 3 :
                return new Coords(game.board.width - 2, 1);
            case 4 :
                return new Coords(game.board.width - 2, game.board.height / 2);
            case 5 :
                return new Coords(game.board.width - 2, game.board.height - 2);
            case 6 :
                return new Coords(game.board.width / 2, game.board.height - 2);
            case 7 :
                return new Coords(1, game.board.height - 2);
            case 8 :
                return new Coords(1, game.board.height / 2);
        }
    }
     
    //-----------------------------------------------------------------
    // Helper Classes
    //-----------------------------------------------------------------
    public static class MoveOption {
        public MoveOption() {
            moves = new MovementData();    
        }
        public MoveOption(MoveOption base) {
            moves = new MovementData(base.moves);            
        }
        public double value() {
            return damagePotential - threat;
        }
        public MovementData moves;
        public double threat;
        public double damagePotential;
    }
     
    public static class FiringOption {
        public FiringOption(Entity target, double value, Mounted weapon) {
            this.target = target;
            this.value = value;
            this.weapon = weapon;
        }
        public Entity target;
        public double value;
        public Mounted weapon;
    }

    public static class PhysicalOption {
        public final static int PUNCH_LEFT = 1;
        public final static int PUNCH_RIGHT = 2;
        public final static int PUNCH_BOTH = 3;
        public final static int KICK_LEFT = 4;
        public final static int KICK_RIGHT = 5;

        public PhysicalOption(Entity target, double dmg, int type) {
            this.target = target;
            this.expectedDmg = dmg;
            this.type = type;
        }
        
        public AbstractAttackAction toAction(Game game, int attacker) {
            switch (type) {
            case PUNCH_LEFT:
                return new PunchAttackAction(attacker, game.getEntityID(target), PunchAttackAction.LEFT);
            case PUNCH_RIGHT:
                return new PunchAttackAction(attacker, game.getEntityID(target), PunchAttackAction.RIGHT);
            case PUNCH_BOTH:
                return new PunchAttackAction(attacker, game.getEntityID(target), PunchAttackAction.BOTH);
            case KICK_LEFT:
                return new KickAttackAction(attacker, game.getEntityID(target), KickAttackAction.LEFT);
            case KICK_RIGHT:
                return new KickAttackAction(attacker, game.getEntityID(target), KickAttackAction.RIGHT);
            }
            return null;
        }
        public Entity target;
        public double expectedDmg;
        public int type;
    }
         
    //-----------------------------------------------------------------
    // Physical turn calculation logic
    //-----------------------------------------------------------------
     public void calculatePhysicalTurn()
     {
        int entNum = game.getFirstEntityNum();
        int first = entNum;
         do {
             // take the first entity that can do an attack
            Entity en = game.getEntity(entNum);

            PhysicalOption bestAttack = getBestPhysical(en);
            if (bestAttack != null) {
                Vector v = new Vector();
                v.addElement(bestAttack.toAction(game, entNum));
                sendAttackData(entNum, v);
                return;    
            }
            entNum = game.getNextEntityNum(entNum);
         } while (entNum != -1 && entNum != first);
     }
     
     // mostly ripped from Server
     public PhysicalOption getBestPhysical(Entity entity) {

        // Infantry can't conduct physical attacks.
  if ( entity instanceof Infantry ) {
      return null;
  }

        // if you're charging, it's already declared
        if (entity.isCharging() || entity.isMakingDfa()) {
            return null;
        }

        PhysicalOption best = null;
        for (Enumeration e = game.getEntities(); e.hasMoreElements();) {
            Entity target = (Entity)e.nextElement();

            if (target.equals(entity)) continue;
            if (!target.isEnemyOf(entity)) continue;
            if ( null == target.getPosition() ) continue;
            PhysicalOption one = getBestPhysicalAttack(game.getEntityID(entity),
                                                        game.getEntityID(target));
            if (one != null) {
                if (best == null) best = one;
                else if (one.expectedDmg > best.expectedDmg) {
                    best = one;    
                }
            }
        }
        return best;
    }

    protected PhysicalOption getBestPhysicalAttack(int from, int to)
    {
        Entity ten = game.getEntity(to);
        Entity fen = game.getEntity(from);
        double bestDmg=0, dmg;
        int damage;
        int bestType = PhysicalOption.PUNCH_LEFT;
        
        // Infantry can't conduct physical attacks.
  if ( fen instanceof Infantry ) {
            return null;
        }
        ToHitData odds = Compute.toHitPunch(game, from, to, PunchAttackAction.LEFT);
        if (odds.getValue() != ToHitData.IMPOSSIBLE) {
            damage = Compute.getPunchDamageFor(fen, PunchAttackAction.LEFT);
            bestDmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
        }

        odds = Compute.toHitPunch(game, from, to, PunchAttackAction.RIGHT);
        if (odds.getValue() != ToHitData.IMPOSSIBLE) {
            damage = Compute.getPunchDamageFor(fen, PunchAttackAction.RIGHT);
            dmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
            if (dmg > 0 && bestDmg > 0) {
                bestType = PhysicalOption.PUNCH_BOTH;
                bestDmg += dmg;
            } else {
                bestType = PhysicalOption.PUNCH_RIGHT;
                bestDmg = dmg;
            }
        }
        
        odds = Compute.toHitKick(game, from, to, KickAttackAction.LEFT);
        if (odds.getValue() != ToHitData.IMPOSSIBLE) {
            damage = Compute.getKickDamageFor(fen, KickAttackAction.LEFT);
            dmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
            if (dmg > bestDmg) {
                bestType = PhysicalOption.KICK_LEFT;
                bestDmg = dmg;
            }
        }

        odds = Compute.toHitKick(game, from, to, KickAttackAction.RIGHT);
        if (odds.getValue() != ToHitData.IMPOSSIBLE) {
            damage = Compute.getKickDamageFor(fen, KickAttackAction.RIGHT);
            dmg = Compute.oddsAbove(odds.getValue()) / 100.0 * damage;
            if (dmg > bestDmg) {
                bestType = PhysicalOption.KICK_RIGHT;
                bestDmg = dmg;
            }
        }

  // Infantry in the open suffer double damage.
  if ( ten instanceof Infantry ) {
      Hex e_hex = game.getBoard().getHex( ten.getPosition() );
      if ( !e_hex.contains(Terrain.WOODS) &&
     !e_hex.contains(Terrain.BUILDING) ) {
    bestDmg *= 2;
      }
  }

        if (bestDmg > 0) {
            return new PhysicalOption(ten, bestDmg, bestType);    
        }
        return null;
    }
     
    //-----------------------------------------------------------------
    // Move turn calculation logic
    //-----------------------------------------------------------------
     public void calculateMoveTurn()
     {
        // for each of my units that haven't moved yet,
        // figure out what the best move would be.
         
         // then take the move with the lowest absolute value
         // and do it first.
         int entNum = game.getFirstEntityNum();
         int first = entNum;

         MoveOption opt = null;
         int theEnt = -1;
         do {

            MoveOption mo = calculateBestMove(entNum);
            sendChat("Could move " + game.getEntity(entNum).getShortName() + " with " + mo.moves + ": dmg " + mo.damagePotential + ", threat " + mo.threat);

            if (opt == null) {
                opt = mo;
                theEnt = entNum;
            } else if (Math.abs(mo.value()) < Math.abs(opt.value())) {
                opt = mo;
                theEnt = entNum;
            }

            entNum = game.getNextEntityNum(entNum);
         } while (entNum != -1 && entNum != first);

         // okay, now we've got a move -- submit it
         sendChat("Move " + game.getEntity(theEnt).getShortName() + ": " + opt.moves);
         
         moveEntity(theEnt, opt.moves);
     }
     

    
     /** Determine the best move for entity "entity"
      */
     protected MoveOption calculateBestMove(int entity)
     {
        Vector v = calculateMoveOptions(entity);
        MoveOption best = null;
        for (Enumeration i =v.elements();i.hasMoreElements();) {
            MoveOption chk = (MoveOption)i.nextElement();
            if (best == null) best = chk;
            else if (chk.damagePotential - chk.threat > best.damagePotential - best.threat) 
                best = chk;
        }
        return best;
     }


     /** Determine all of the possible moves for entity "entity".
      * For each move, calculate a threat and damage potential,
      * and create a MoveOption object, which will be added to
      * the return vector.
      */
     protected Vector calculateMoveOptions(int entity)
     {
        Vector v = new Vector();
        MoveOption opt = new MoveOption();
        calculateRunWalkMoveOptions(entity, v, opt, game.getEntity(entity).mpUsed);
        
        return v;
     }
     

    
     protected void calculateRunWalkMoveOptions(int entity, Vector options, 
                                                MoveOption current, int mpsUsed)
     {
        Entity en = game.getEntity(entity);

        if (options.size() > 0 && (options.size() % 200) == 0) 
            sendChat("... thinking about my move...");
        // evaluate the current move
        // System.out.println("Evaluating " + current.moves + " to " + en.getPosition() +" facing " + en.getFacing());

        if (mpsUsed >= en.getRunMP()) 
            en.moved = Entity.MOVE_RUN;
        else if (mpsUsed > 0)
            en.moved = Entity.MOVE_WALK;
        else en.moved = Entity.MOVE_NONE;
        
        current.threat = calculateThreat(entity);
        current.damagePotential = calculateDamagePotential(entity);
        options.addElement(current);
        // System.out.println(" -> dmg " + current.damagePotential + "; threat " + current.threat + " = " + (current.damagePotential - current.threat));
                 
        //    sendChat("Move " + current.moves + " to " + en.getPosition() +" facing " + en.getFacing() + ": " + 
    //             ((int)current.damagePotential * 10000) / 10000.0);

        // do we have MPs left?
        if (mpsUsed == 0) en.heatBuildup += 1;
        if (mpsUsed < en.getRunMP()) {
            // try all of my moves, recursively adjusting state in and out

            // am I prone?  if so, all I can do is get up
            if (en.isProne()) {
                MoveOption opt = new MoveOption(current);
                opt.moves.addStep(MovementData.STEP_GET_UP);
                int mp = en.getWalkMP() == 1 ? 1 : 2;
                en.setProne(false); // assume we get up successfully
                calculateRunWalkMoveOptions(entity, options, opt, mpsUsed + mp);
                en.setProne(true); // undo it on the way out
            } else {
                // go forward
                Coords targetHex = en.getPosition().translated(en.getFacing());
                if (game.board.getHex(targetHex) != null) {
                    int cost = Compute.getMovementCostFor(game, entity, en.getPosition(),
                            targetHex, false);
        
                    // make sure we can afford the move
                    if (mpsUsed + cost <= en.getRunMP()) {
                        // make sure there's no mech there
                        if (game.getFirstEntity(targetHex) == null) {
                            MoveOption opt = new MoveOption(current);
                            opt.moves.addStep(MovementData.STEP_FORWARDS);
                            if (mpsUsed <= en.getWalkMP() && mpsUsed + cost > en.getWalkMP())
                                    en.heatBuildup += 1;
                            Coords oldPosition = en.getPosition();
                            en.setPosition(targetHex);
                            calculateRunWalkMoveOptions(entity, options, opt, mpsUsed + cost);
                            en.setPosition(oldPosition);
                            if (mpsUsed <= en.getWalkMP() && mpsUsed + cost > en.getWalkMP())
                                    en.heatBuildup -= 1;
                        }
                    }
                }
                // turn left
                int oldFacing = en.getFacing();
                MoveOption opt = new MoveOption(current);
                // sendChat(" -> step left");
                en.setFacing(MovementData.getAdjustedFacing(oldFacing, MovementData.STEP_TURN_LEFT));
                opt.moves.addStep(MovementData.STEP_TURN_LEFT);
                calculateRunWalkMoveOptions(entity, options, opt, mpsUsed + 1);

                // turn right
                opt = new MoveOption(current);
                // sendChat(" -> step right");
                en.setFacing(MovementData.getAdjustedFacing(oldFacing, MovementData.STEP_TURN_RIGHT));
                opt.moves.addStep(MovementData.STEP_TURN_RIGHT);
                calculateRunWalkMoveOptions(entity, options, opt, mpsUsed + 1);
                en.setFacing(oldFacing);
            }
        }
        if (mpsUsed == 0) en.heatBuildup -= 1;
     }
     
     /** Calculates the expected value of all optimal attacks from
      * every enemy entity against entity "to" using all possible weapons
      */
     protected double calculateThreat(int to)
     {
        double total = 0.0;
        
        Enumeration ents = game.getEntities();
        while (ents.hasMoreElements()) {
            Entity e = (Entity)ents.nextElement();
            if (e.getOwner().isEnemyOf(game.getPlayer(this.local_pn)) && (null != e.getPosition())) {
                // sendChat("Calculating threat from entity " + e.getName());
                total += calculateOneThreat(game.getEntityID(e), to);
            }
        }
        // sendChat(" -> total threat: " + total);
        return total;
     }
     
     
     /** Calculates the expected value of all damage inflictable by
      * entity "from" on entity "to"
      */
     protected double calculateOneThreat(int from, int to)
     {
         double total = 0.0;
         //sendChat("Calculating threat from entity " + from);
         total += calculateOneWeaponThreat(from, to);
         //sendChat(" -> entity threat: " + total);
         
         return total;
     }
     
    
     /** Calculates the expected value of an optimal attack from
      * entity "from" against entity "to" using all possible weapons
      */
     protected double calculateOneWeaponThreat(int from, int to)
     {
        Entity fen = game.getEntity(from);
        Entity ten = game.getEntity(to);
        // for each weapon mounted on From...
        int first = fen.getFirstWeapon();
        if (first == -1) return 0; // no weapons
        
        int weap = first;
        double total = 0.0;
        do {
            // if it can target To...
            Mounted w = fen.getEquipment(weap);
            ToHitData th = Compute.toHitWeapon(game, from, ten, weap);    
            // TODO: try all secondary facings for firer; take the best
            
             // calculate expected value of attack
            if (th.getValue() != ToHitData.IMPOSSIBLE) {
                double odds = Compute.oddsAbove(th.getValue())/ 100.0;
    double expectedDmg;

    // Is the atttacker an Infantry platoon?
    if ( fen instanceof Infantry ) {
        // Get the expected damage, given its current 
        // manpower level.
        Infantry inf = (Infantry) fen;
        expectedDmg = 
      inf.getDamage(inf.getShootingStrength());
    } else {
        // Get the expected damage of the weapon.
        expectedDmg = 
      getExpectedDamage((WeaponType)w.getType());
    }

    // Infantry in the open suffer double damage.
    if ( ten instanceof Infantry ) {
        Hex e_hex = game.getBoard().getHex( ten.getPosition() );
        if ( !e_hex.contains(Terrain.WOODS) &&
       !e_hex.contains(Terrain.BUILDING) ) {
      expectedDmg *= 2;
        }
    }

    // Modify the expected damage by the odds.
                total += odds * expectedDmg;

//                sendChat(" -> threat " + w.getType().getName() + " needs " + th.getValue() + ": " + (odds*expectedDmg));                                   
            }
            
            weap = fen.getNextWeapon(weap);
        } while (weap != first);
        return total;
     }
     
    
     
    //-----------------------------------------------------------------
    // Firing turn calculation logic
    //-----------------------------------------------------------------
    public void calculateFiringTurn()
    {
        // just take the first unit that hasn't fired yet and do it
        int entNum = game.getFirstEntityNum();
        Entity en = game.getEntity(entNum);

        Vector firV = new Vector();
        for (Enumeration i = en.getWeapons();i.hasMoreElements();) {
            Mounted mw = (Mounted)i.nextElement();
             FiringOption fo = calculateOneWeaponDamagePotential(entNum, mw);
             if (fo != null) firV.addElement(fo);
        }        
        FiringOption[] firOpts = new FiringOption[firV.size()];
        firV.copyInto(firOpts);

        // rank order firing options according to damage potential,
        // and fire them in order, skipping weapons tht would exceed
        // the heat limit.
        sortFiringOptions(firOpts);
        int capacity = en.getHeatCapacityWithWater();
        int currentHeat = en.heatBuildup;
        
        int allowableHeatLevel = 5; // dunno.  pick a number.

        Vector attacks = new Vector();
        for (int i = 0;i<firOpts.length;i++) {
            if (currentHeat + ((WeaponType)firOpts[i].weapon.getType()).getHeat() - capacity <= allowableHeatLevel) {
                attacks.addElement(new WeaponAttackAction(
                        entNum, 
                        game.getEntityID(firOpts[i].target), en.getEquipmentNum(firOpts[i].weapon)));
                currentHeat += ((WeaponType)firOpts[i].weapon.getType()).getHeat();
            }
        }
        sendAttackData(entNum, attacks);
    }

    protected void sortFiringOptions(FiringOption[] fo)
    {    
        // stupid insertion sort for now -- please please please can
        // we use collections.jar?
        for (int i=0;i<fo.length;i++) {
            for (int j=i+1;j<fo.length;j++) {
                if (fo[j].value < fo[i].value) {
                    FiringOption tmp = fo[i];
                    fo[i] = fo[j];
                    fo[j] = tmp;
                }
            }
        }
    }
             
    
    
    //-----------------------------------------------------------------
    // Shared calculation logic
    //-----------------------------------------------------------------
     /** Calculates the optimal damage potential of attacks
      * that could be made by entity "from" from its current location.
      */
     protected double calculateDamagePotential(int from) {
        double total = 0.0;        
        Entity en = game.getEntity(from);

         // TODO: try my torso twists
         en.setSecondaryFacing(en.getFacing());
         
        // for each weapon
        for (Enumeration i = en.getWeapons();i.hasMoreElements();) {
            Mounted mw = (Mounted)i.nextElement();
            FiringOption val = calculateOneWeaponDamagePotential(from, mw);
            if (val != null)
                total += val.value;            
        }        
        // TODO: only apply weapons up to heatMax; use them all for now
        return total;
     }

     /** Calculates the optimal damage potential of attacks
      * that could be made by entity "from" from its current location.
      */
     protected FiringOption calculateOneWeaponDamagePotential(int from, Mounted mw)
     {
         // for now assume I'm facing front
         Entity en = game.getEntity(from);
         
        int weaponID = en.getEquipmentNum(mw);
            
        // for each target I could attack
        // which has the highest expected damage?
        Entity bestTarget = null;
        double bestValue = 0;
        Enumeration ents = game.getEntities();
        while (ents.hasMoreElements()) {
            Entity e = (Entity)ents.nextElement();
            if (e.getOwner().isEnemyOf(game.getPlayer(this.local_pn)) && (null != e.getPosition())) {
                ToHitData th = Compute.toHitWeapon(game, from, e, weaponID);    
                if (th.getValue() != ToHitData.IMPOSSIBLE) {
                    double odds = Compute.oddsAbove(th.getValue())/ 100.0;
                    double expectedDmg;

        // Are we an Infantry platoon?
        if ( en instanceof Infantry ) {
      // Get the expected damage, given our current 
      // manpower level.
      Infantry inf = (Infantry) en;
      expectedDmg = 
          inf.getDamage(inf.getShootingStrength());
        } else {
      // Get the expected damage of the weapon.
      expectedDmg = 
          getExpectedDamage((WeaponType)mw.getType());
        }

        // Infantry in the open suffer double damage.
        if ( e instanceof Infantry ) {
      Hex e_hex = game.getBoard().getHex( e.getPosition() );
      if ( !e_hex.contains(Terrain.WOODS) &&
           !e_hex.contains(Terrain.BUILDING) ) {
          expectedDmg *= 2;
      }
        }

        // Modify the expected damage by the odds.
        expectedDmg = odds * expectedDmg;
                    if (expectedDmg > bestValue) {
                        bestTarget = e;
                        bestValue = expectedDmg;
                    }
                }
            }
        }
        if (bestTarget == null) return null;
        return new FiringOption(bestTarget, bestValue, mw);
     }
          
     
     protected double getExpectedDamage(WeaponType weap)
     {
        if (weap.getDamage() != WeaponType.DAMAGE_MISSILE) {
            // normal weapon
            return  weap.getDamage();
        } else {
            // hard-coded expected missile numbers, based on 
            // missile-hit chart
            
            // AIOPT: penalize missile damage because it has less penetrative power?
            //        or enhance it, if the enemy has holes in their armor?  hmm.
            if (weap.getAmmoType() == AmmoType.T_SRM) {
                switch (weap.getRackSize()) {
                    case 2:return 1.41666*2;
                    case 4:return 2.63888*2;
                    case 6:return 4*2;
                }
            } else {
                switch (weap.getRackSize()) {
                    case 5:return 3.16666;
                    case 10:return 6.30555;
                    case 15:return 9.5;
                    case 20:return 12.69444;
                }
            }
        }        
        return 0;
    }    
    
}
