/**
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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
/*
 * TestBot.java
 *
 * Created on April 30, 2002, 4:42 PM
 */

package megamek.client.bot;

import java.awt.Frame;
import com.sun.java.util.collections.*;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.io.*;

import megamek.*;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.client.bot.ga.*;
import megamek.client.*;

/**
 * Another Bot implementation
 * @author  Steve Hawkins
 */

public class TestBot extends BotClientWrapper {
  
  public static class AttackOptionSorter implements Comparator {
    CEntity primary = null;
    
    public AttackOptionSorter(CEntity primary_target) {
      this.primary = primary_target;
    }
    //this is a fancy way of saying do all your primary damage first
    //then do all your non-missle attacks, etc, etc.
    public int compare(Object obj, Object obj1) {
      AttackOption a = (AttackOption)obj;
      AttackOption a1 = (AttackOption)obj1;
      if (a.target.getKey().intValue() == a1.target.getKey().intValue()) {
        WeaponType w = (WeaponType)a.weapon.getType();
        WeaponType w1 = (WeaponType)a1.weapon.getType();
        if (w.getDamage() == WeaponType.DAMAGE_MISSILE) {
          if (w1.getDamage() == WeaponType.DAMAGE_MISSILE) {
            if (a.expected > a1.expected) {
              return -1;
            }
            return 1;
          }
          return 1;
        } else if (w.getDamage() == WeaponType.DAMAGE_MISSILE) {
          return -1;
        } else if (a.expected > a1.expected) {
          return -1;
        } else {
          return 1;
        }
      } else if (a.target.getKey().equals(this.primary.getKey())) {
        return -1;
      }
      return 1;
    }
  }
  
  public static class AttackOption extends ToHitData {
    public CEntity target;
    public double value;
    public Mounted weapon;
    public ToHitData toHit;
    public double odds; // secondary odds
    public double primary_odds; // primary odds
    public int heat;
    public double expected;
    public double primary_expected;
    public int ammoLeft = -1; //-1 doesn't use ammo
    
    public AttackOption(CEntity target, Mounted weapon, double value, ToHitData toHit) {
      this.target = target;
      this.weapon = weapon;
      this.toHit = toHit;
      this.value = value;
      if (target != null) {
        WeaponType w = (WeaponType)weapon.getType();
        this.primary_odds = Compute.oddsAbove(toHit.getValue())/ 100.0;
        this.odds = Compute.oddsAbove(toHit.getValue() + 1)/ 100.0;
        this.heat = w.getHeat();
        this.expected = this.odds*this.value;
        this.primary_expected = this.primary_odds*this.value;
	final boolean isInfantryWeapon = 
	    ((w.getFlags() & WeaponType.F_INFANTRY) == WeaponType.F_INFANTRY);
        final boolean usesAmmo = 
	    (!isInfantryWeapon && w.getAmmoType() != AmmoType.T_NA);
        final Mounted ammo = usesAmmo ? weapon.getLinked() : null;
        if (usesAmmo && (ammo == null || ammo.getShotsLeft() == 0)) {
          this.value = 0; //should have already been caught...
        } else if (usesAmmo) {
          this.ammoLeft = ammo.getShotsLeft();
        }
      }
    }
  }
  
  public static CEntity.Table enemies = new CEntity.Table();
  public LinkedList unit_values = new LinkedList();
  public LinkedList enemy_values = new LinkedList();
  
  public static java.util.Properties BotProperties = new java.util.Properties();
  public static int Ignore = 10;
  
  /** Creates a new instance of TestBot */
  public TestBot(Frame frame, String name) {
    super(frame, name);
    setup();
  }
  
  public TestBot(String name) {
    super(name);
    setup();
  }
  
  private void setup()
  {
    try {
      BotProperties.load(new FileInputStream("bot.properties"));
    } catch (Exception e) {
      System.out.println("Bot properties could not be loaded, will use defaults");
    }
    int difficulty = 3;
    try {
      difficulty = Integer.parseInt(BotProperties.getProperty("difficulty","3"));
    } catch (Exception e) {}
    
    switch (difficulty) {
      case 1:
        this.Ignore = 8;
        break;
      case 2:
        this.Ignore = 9;
        break;
      case 3:
        this.Ignore = 10;
        break;  
    }
  }
  
  public void initialize() {
    EntityState.game = this.game;
    EntityState.tb = this;
    CEntity.game = this.game;
    CEntity.tb = this;
  }
  
  public void calculatePhysicalTurn() {
    int entNum = game.getFirstEntityNum();
    int first = entNum;
    do {
      // take the first entity that can do an attack
      Entity en = game.getEntity(entNum);
      CEntity cen = this.enemies.get(en);
      PhysicalOption bestAttack = getBestPhysical(en);
      if (bestAttack != null) {
        if (bestAttack.type == PhysicalOption.KICK_LEFT || bestAttack.type == PhysicalOption.KICK_RIGHT) {
          int side = Compute.getThreatHitArc(bestAttack.target.getPosition(), bestAttack.target.getFacing(), en.getPosition());
          double odds = 1.0 - (double)Compute.oddsAbove(Compute.toHitKick(game, entNum, bestAttack.target.getId(), bestAttack.type - 3).getValue())/100;

          // Meks can kick Vehicles and Infantry, too!
          double mod = 1.0;
          if ( bestAttack.target instanceof Mech ) {
              double llarmor = bestAttack.target.getArmor(Mech.LOC_LLEG) / 
                  bestAttack.target.getOArmor(Mech.LOC_LLEG);
              double rlarmor = bestAttack.target.getArmor(Mech.LOC_RLEG) / 
                  bestAttack.target.getOArmor(Mech.LOC_RLEG);
              switch (side) {
              case CEntity.SIDE_FRONT:
                  mod = (llarmor + rlarmor)/2;
                  break;
              case CEntity.SIDE_LEFT:
                  mod = llarmor;
                  break;
              case CEntity.SIDE_RIGHT:
                  mod = rlarmor;
                  break;
              }
          }
          else if ( bestAttack.target instanceof Infantry ) {
              mod = 0.0;
          }
          else if ( bestAttack.target instanceof Tank ) {
              switch (side) {
              case CEntity.SIDE_FRONT:
                  mod = bestAttack.target.getArmor(Tank.LOC_FRONT) / 
                      bestAttack.target.getOArmor(Tank.LOC_FRONT);
                  break;
              case CEntity.SIDE_LEFT:
                  mod = bestAttack.target.getArmor(Tank.LOC_LEFT) / 
                      bestAttack.target.getOArmor(Tank.LOC_LEFT);
                  break;
              case CEntity.SIDE_RIGHT:
                  mod = bestAttack.target.getArmor(Tank.LOC_RIGHT) / 
                      bestAttack.target.getOArmor(Tank.LOC_RIGHT);
                  break;
              case CEntity.SIDE_REAR:
                  mod = bestAttack.target.getArmor(Tank.LOC_REAR) / 
                      bestAttack.target.getOArmor(Tank.LOC_REAR);
                  break;
              }
          }
          double damage = 2/(1 + mod)*bestAttack.expectedDmg;
          double threat = .2*en.getWeight()*odds*(1-cen.base_psr_odds);
          //check for head kick
          Hex h = game.board.getHex(bestAttack.target.getPosition());
          Hex h1 = game.board.getHex(en.getPosition());
          if (h1.getElevation() > h.getElevation()) {
            damage *= 2;
          }
          Enumeration e = game.getEntities();
          double temp_threat = 0;
          int number = 0;
          while (e.hasMoreElements()) {
            Entity enemy = (Entity)e.nextElement();
            if (!enemy.isProne() && enemy.getPosition().distance(en.getPosition()) < 3) {
              if (enemy.isEnemyOf(en)) {
                number++;
              } else {
                number--;
              }
            }
          }
          if (number > 0) {
            threat += number*temp_threat;
          }
          //take a kick if it is in your favor and you not healthy and it is risky
          if (!((damage > threat && !(cen.overall_armor_percent > .8 && odds > .9)) || (odds < .5 && cen.base_psr_odds > .5))) {
            boolean left = false;
            boolean right = false;
            ToHitData toHit = Compute.toHitPunch(game, en.getId(), bestAttack.target.getId(), PunchAttackAction.LEFT);
            if (toHit.getValue() != ToHitData.IMPOSSIBLE) {
              left = true;
            }
            toHit = Compute.toHitPunch(game, en.getId(), bestAttack.target.getId(), PunchAttackAction.RIGHT);
            if (toHit.getValue() != ToHitData.IMPOSSIBLE) {
              right  = true;
            }
            if (left) {
              if (right) {
                bestAttack.type = PhysicalOption.PUNCH_BOTH;
              } else {
                bestAttack.type = PhysicalOption.PUNCH_LEFT;
              }
            } else if (right) {
              bestAttack.type = PhysicalOption.PUNCH_RIGHT;
            } else {
              sendAttackData(entNum, new java.util.Vector(0));
              return;
            }
          }
        }
        java.util.Vector v = new java.util.Vector();
        v.addElement(bestAttack.toAction(game, entNum));
        sendAttackData(entNum, v);
        return;
      }
      entNum = game.getNextEntityNum(entNum);
    } while (entNum != -1 && entNum != first);
  }
  
  public Vector getEntitiesOwned() {
    Vector result = new Vector();
    for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
      Entity entity = (Entity)i.nextElement();
      if (entity.getOwner().equals(this.getLocalPlayer())) {
        result.add(entity);
      }
    }
    return result;
  }
  
  protected Vector getEnemyEntities() {
    Vector result = new Vector();
    Entity mine = null;
    Iterator i = this.getEntitiesOwned().iterator();
    if (i.hasNext()) {
      mine = (Entity)i.next();
    } else {
      return result;
    }
    java.util.Vector targets = game.getValidTargets(mine);
    for (Enumeration j = targets.elements(); j.hasMoreElements();) {
      result.add(j.nextElement());
    }
    return result;
  }
  
  int enemies_moved = 0;
  GALance old_moves = null;
  int my_mechs_moved = 0;
  
  public void calculateMoveTurn() {
    long enter = System.currentTimeMillis();
    int initiative = 0;
    EntityState min = null;
    
    System.out.println("beginning movement calculations...");

    // if we're ordered to move a specific entity, assume that's because it
    // fell and reset it so that we recalculate its movement
    if (game.getTurn() instanceof GameTurn.SpecificEntityTurn) {
        GameTurn.SpecificEntityTurn turn = (GameTurn.SpecificEntityTurn)game.getTurn();
        Entity mustMove = game.getEntity(turn.getEntityNum());
        CEntity centity = this.enemies.get(mustMove);
        // do some resetting
        centity.refresh = true;
        centity.moved = false;
        centity.reset();
        // clear old moves
        this.old_moves = null;
        System.out.println("reset entity " + mustMove.getId());
    }
    //first check and make sure that someone else has moved so that we don't replan
    Object[] enemy_array = this.getEnemyEntities().toArray();
    for (int j = 0; j < enemy_array.length; j++) {
      if (!((Entity)enemy_array[j]).isSelectable()) {
        initiative++;
      }
    }
    // if nobody's moved and we have a valid move waiting, use that
    if (initiative == enemies_moved && old_moves != null) {
      min = this.old_moves.getResult();
      if (min == null || !min.isMoveLegal() || (min.isPhysical && min.PhysicalTarget.isPhysicalTarget)) {
        this.old_moves = null;
        System.out.println("recalculating moves since the old move was invalid");
        this.calculateMoveTurn();
        return;
      }
    } else {
      // otherwise calculate some moves
      enemies_moved = initiative;
      Vector possible = new Vector();
      Iterator i = this.getEntitiesOwned().iterator();
      boolean short_circuit = false;
      
      while(i.hasNext()) {    
        Entity entity = (Entity)i.next();
        CEntity cen = this.enemies.get(entity);
        
        // if we can't move this entity right now, ignore it
        if (!game.getTurn().isValidEntity(entity)) {
            continue;
        }
        
        MoveThread mt = new MoveThread(entity); //so things don't slow down too much, use a thread
        System.out.println("Contemplating movement of "+entity.getShortName()+" "+entity.getId());
        mt.start();
        try {
          mt.join();
        } catch (Exception e) {
          e.printStackTrace();
        }

	// If this entity can still move, add its
	// move result to the list of possible moves.
	if ( !cen.moved ) {
	    possible.add(mt.result);
	}

        if (cen.entity.isImmobile() && !cen.moved) {
          cen.moved = true;
          System.out.println("recalculating moves since the unit is immobile");
          this.calculateMoveTurn();
          return;
        } else if (!cen.moved && mt.result.length < 6 && mt.result.length > 0) {
          //move to the head of the class...
          min = (EntityState)mt.result[0];
          short_circuit = true;
        }
      }

      //should ignore mechs that are not engaged
      //and only do the below when there are 2 or mechs left to move
      if (!short_circuit) {
        if (this.getEntitiesOwned().size() > 1) {
          try {
            GALance lance = new GALance(this, possible, 50, 80);
            Thread lanceThread = new Thread(lance);
            lanceThread.start();
            lanceThread.join();
            min = lance.getResult();
            this.old_moves = lance;
          } catch (GAException gae) {
            System.out.println(gae.getMessage());
          } catch (Exception e) {
            e.printStackTrace();
          }
        } else if ( ((EntityState[])possible.elementAt(0)).length > 0 ) {
          min = ((EntityState[])possible.elementAt(0))[0];
        }
      }
    }
    for (int d = 0; min != null && d < enemy_array.length; d++) {
      Entity en = (Entity)enemy_array[d];
      CEntity enemy = this.enemies.get(en);
      int enemy_hit_arc = Compute.getThreatHitArc(enemy.old.curPos, enemy.old.curFacing, min.curPos);
      if ( min.damages.length > d ) {
          enemy.expected_damage[enemy_hit_arc] += min.damages[d];
      }
      if (enemy.expected_damage[enemy_hit_arc] > 0) {
        enemy.hasTakenDamage = true;
      }
    }
    if (min.isPhysical) {
      min.PhysicalTarget.isPhysicalTarget = true;
    }
    Iterator k = min.tv.iterator();
    String threat = "";
    while(k.hasNext()) threat += k.next()+" ";
    System.out.println(min.entity.getShortName()+" "+min.entity.getId()+" to "+min.getKey()+" from "+this.enemies.get(min.entity).old.getKey()+" "+min+"\n Utility: "+min.getUtility()+" \n"+threat+"\n");
    sendChat("Moved " + min.entity.getShortName()+" to "+min.curPos);
    this.my_mechs_moved++;

    moveEntity(min.entity.getId(), min.getMovementData());
    min.centity.moved = true;
    min.centity.old = min;
    min.centity.last = min;
    
    long exit = System.currentTimeMillis();
    System.out.println("move turn took " + (exit - enter) + " ms");
  }
  
  class MoveThread extends Thread {
    Entity myEntity;
    EntityState[] result;
    
    public MoveThread(Entity entity) {
      this.myEntity = entity;
      this.setPriority(Thread.MIN_PRIORITY);
    }
    
    public void run() {
      result = calculateMove(myEntity);
    }
  }
  
  //calculate top moves!
  public EntityState[] calculateMove(Entity entity) {
    Object[] enemy_array = Compute.vectorToArray(game.getValidTargets(entity));
    CEntity self = this.enemies.get(entity);
    EntityState current = self.old;
    Object[] move_array;
    if (entity.isSelectable() && !self.moved) {
      move_array = self.getAllMoves().toArray();
    } else {
      move_array = new Object[] {current};
    }
    Compute.randomize(move_array); //this helps produce a better mix of things to do
    EntityState.Table pass = new EntityState.Table();
    
    Vector fm = new Vector();
    for (Iterator i = this.getEntitiesOwned().iterator(); i.hasNext();){
      Entity en = (Entity)i.next();
      if (en.getId() != entity.getId()) {
        fm.add(new EntityState((this.enemies.get(en)).old));
      }
    }
    Object[] friend_move_array = fm.toArray();
    
    /***************************************************
     * first pass, filter moves based upon present case
     ****************************************************/
    for (int i = 0; i < move_array.length + friend_move_array.length; i++) { // for each state (could some prefiltering be done?)
      EntityState option = null;
      if (i >= move_array.length) {
        option = (EntityState)friend_move_array[i - move_array.length];
      } else {
        option = (EntityState)move_array[i];
      }
      option.setState();

      // 2002-10-28 Suvarov454 : Discard impossible locations.
      if ( !game.getBoard().contains(option.entity.getPosition()) ) {
	  continue;
      }

      if (option.damages == null) option.damages = new double[enemy_array.length];
      if (option.threats == null) option.threats = new double[enemy_array.length];
      if (option.max_threats == null) option.max_threats = new double[enemy_array.length];
      if (option.min_damages == null) option.min_damages = new double[enemy_array.length];
      
      for (int e = 0; e < enemy_array.length; e++) { // for each enemy
        Entity en = (Entity)enemy_array[e];
        CEntity enemy = this.enemies.get(en);
        int enemy_hit_arc = Compute.getThreatHitArc(enemy.old.curPos, enemy.old.curFacing, option.curPos);
        int self_hit_arc = Compute.getThreatHitArc(option.curPos, option.curFacing, enemy.old.curPos);
        int[] modifiers = option.getModifiers(enemy.entity);
        if (!enemy.entity.isImmobile() && modifiers[EntityState.DEFENCE_MOD] != ToHitData.IMPOSSIBLE) {
          self.engaged = true;
          int mod = modifiers[EntityState.DEFENCE_MOD];
          double max = option.getMaxModifiedDamage(enemy.old, this.enemies.get(en), mod, modifiers[EntityState.DEFENCE_PC]);
          if (en.isSelectable()) { // let him turn a little
            enemy.old.curFacing = (enemy.old.curFacing+1)%6;
            max = Math.max(option.getMaxModifiedDamage(enemy.old, this.enemies.get(en), mod+1, modifiers[EntityState.DEFENCE_PC]),max);
            enemy.old.curFacing = (enemy.old.curFacing+4)%6;
            max = Math.max(option.getMaxModifiedDamage(enemy.old, this.enemies.get(en), mod+1, modifiers[EntityState.DEFENCE_PC]),max);
            //return to original facing
            enemy.old.curFacing = (enemy.old.curFacing+1)%6;
          }
          max = self.getThreatUtility(max, self_hit_arc);
          if (enemy.entity.isProne()) max *= .6;
          option.threats[e] = max;
          option.max_threats[e] = max;
          option.threat += max;
          option.tv.add(max+" Threat "+e+"\n");
        }
        
        //damage reasoning
        /* As a first approximation, take the maximum to a single target */
        if (!option.isPhysical) {
          if (modifiers[EntityState.ATTACK_MOD] != ToHitData.IMPOSSIBLE) {
            self.engaged = true;
            double max = enemy.old.getMaxModifiedDamage(option, self, modifiers[0], modifiers[EntityState.ATTACK_PC]);
            max = enemy.getThreatUtility(max, enemy_hit_arc);
            option.damages[e] = max;
            option.min_damages[e] = max;
            option.tv.add(max+" Damage "+e+"\n");
            option.damage = Math.max(max, option.damage);
          }
        } else {
          try {
            if (option.PhysicalTarget.entity.getId() == enemy.entity.getId()) {
              if (!option.PhysicalTarget.isPhysicalTarget) {
                ToHitData toHit = null;
                double self_threat = 0;
                double damage = 0;
                if (option.isJumping) {
                  self.old.setState();
                  MovementData md = option.getMovementData();
                  toHit = Compute.toHitDfa(game, option.entity.getId(), option.PhysicalTarget.entity.getId(), md);
                  damage = 2*Compute.getDfaDamageFor(option.entity);
                  self_threat = option.centity.getThreatUtility(Compute.getDfaDamageTakenBy(option.entity), CEntity.SIDE_REAR)*Compute.oddsAbove(toHit.getValue())/100;
                  self_threat += option.centity.getThreatUtility(.1*self.entity.getWeight(), CEntity.SIDE_REAR);
                  self_threat *= 100/option.centity.entity.getWeight(); //small mechs shouldn't do this...
                } else {
                  self.old.setState();
                  MovementData md = option.getMovementData();
                  toHit = Compute.toHitCharge(game, option.entity.getId(), option.PhysicalTarget.entity.getId(), md);
                  damage = Compute.getChargeDamageFor(option.entity, md.getHexesMoved());
                  self_threat = option.centity.getThreatUtility(Compute.getChargeDamageTakenBy(option.entity, option.PhysicalTarget.entity), CEntity.SIDE_FRONT)*(Compute.oddsAbove(toHit.getValue())/100);
                  option.setState();
                }
                damage = option.PhysicalTarget.getThreatUtility(damage, toHit.getSideTable())*Compute.oddsAbove(toHit.getValue())/100;
                //charging is a good tactic against larger mechs
                if (!option.isJumping) damage *= Math.sqrt((double)enemy.bv/(double)self.bv);
                //these are always risky, just don't on 11 or 12
                if (toHit.getValue() > 10) damage = 0;
                //7 or less is good
                if (toHit.getValue() < 8) damage *= 1.5;
                //this is all you are good for
                if (self.RangeDamages[self.RANGE_SHORT] < 5) damage *= 2;
                //System.out.println(option + " " + damage + " "+ self_threat + " " + toHit.getValue());
                option.damages[e] = damage;
                option.min_damages[e] = damage;
                option.damage = damage;
                option.movement_threat += self_threat;
              } else {
                option.threat += Integer.MAX_VALUE;
              }
            }
          } catch (Exception e1) {
            e1.printStackTrace();
            option.threat += Integer.MAX_VALUE;
          }
        }
      } //-- end while of each enemy
      self.old.setState();
    } //-- end while of first pass
    
    Arrays.sort(move_array);
    
    int filter = 50;
    if (friend_move_array.length > 1) {
      filter = 100;
    }
    //top 100 utility, mostly conservative
    for (int i = 0; i < filter && i < move_array.length ; i++) {
      pass.put((EntityState)move_array[i]);
    }
    
    Arrays.sort(move_array, new Comparator() {
      public int compare(Object obj, Object obj1) {
        if (((EntityState)obj).damage - .5*((EntityState)obj).getUtility() >
        ((EntityState)obj1).damage - .5*((EntityState)obj1).getUtility()) {
          return -1;
        }
        return 1;
      }
    });
    
    //top 100 damage
    for (int i = 0; i < filter && i < move_array.length; i++) {
      pass.put((EntityState)move_array[i]);
    }
    
    //pass now contains 100 ~ 200 moves
    /*********************************************************
     *  New second pass, combination moves/firing
     *    based only on the present case, since only one mech moves
     *    at a time
     ********************************************************/
    if (friend_move_array.length > 1) {
      move_array = pass.values().toArray();
      pass.clear();
      for (int j = 0; j < move_array.length; j++) {
        EntityState option = (EntityState)move_array[j];
        for (int e = 0; e < enemy_array.length; e++) { // for each enemy
          Entity en = (Entity)enemy_array[e];
          CEntity enemy = this.enemies.get(en);
          if (option.damages[e] > 0) {
            for (int f = 0; f < friend_move_array.length; f++) {
              EntityState foption = (EntityState)friend_move_array[f];
              double threat_divisor = 1;
              if (foption.damages[e] > 0) {
                option.damage += (enemy.canMove()?.1:.2)*option.damages[e] ;
                threat_divisor += foption.centity.canMove()?.4:.6;
              }
              option.threat -= option.threats[e];
                //this is most important
              option.threats[e] /= threat_divisor;
              option.threat += option.threats[e];
            }
          }
        }
      }
  
      Arrays.sort(move_array);
      
      filter = 50;
      
      //top utility, mostly conservative
      for (int i = 0; i < filter && i < move_array.length ; i++) {
        pass.put((EntityState)move_array[i]);
      }
      
      Arrays.sort(move_array, new Comparator() {
        public int compare(Object obj, Object obj1) {
          if (((EntityState)obj).damage - .5*((EntityState)obj).getUtility() >
          ((EntityState)obj1).damage - .5*((EntityState)obj1).getUtility()) {
            return -1;
          }
          return 1;
        }
      });
      
      //top 50 damage
      for (int i = 0; i < filter && i < move_array.length; i++) {
        pass.put((EntityState)move_array[i]);
      }
    }
    
    /**********************************************************
     * third pass, (not so bad) oppurtunistic planner
     * gives preference to good ranges/defensive positions
     * based upon the mech characterization
     ***********************************************************/
    move_array = pass.values().toArray();
    pass.clear();
    
    for (int j = 0; j < move_array.length; j++) {
      EntityState option = (EntityState)move_array[j];
      option.setState();
      double adjustment = 0;
      double temp_adjustment = 0;
      for (int e = 0; e < enemy_array.length; e++) { // for each enemy
        Entity en = (Entity)enemy_array[e];
        CEntity enemy = this.enemies.get(en);
        int current_range = self.old.curPos.distance(enemy.old.curPos);
        int range = option.curPos.distance(enemy.old.curPos);
        if (range > self.long_range) {
          temp_adjustment += (!(range < enemy.long_range)?.5:1)*(1+self.RangeDamages[self.Range])*(Math.max(range - self.long_range - .5*Math.max(self.jumpMP, .8*self.runMP),0));
        }
        //this is wrong on larger maps...
        //a mech must choose which other mechs to be engaged with or stay in the middle
        if ((self.Range == self.RANGE_SHORT && (current_range > 5 || range > 9)) || (self.RangeDamages[self.RANGE_SHORT] < 4 && current_range > 10)) {
          temp_adjustment += ((enemy.Range > self.RANGE_SHORT)?.5:1)*(Math.max(1+self.RangeDamages[self.RANGE_SHORT], 5))*Math.max(range -.5*Math.max(self.jumpMP, .8*self.runMP),0);
        } else if (self.Range == self.RANGE_MEDIUM) {
          temp_adjustment += ((current_range < 6 || current_range > 12)?1:.25)*((enemy.Range > self.RANGE_SHORT)?.5:1)*(1+self.RangeDamages[self.RANGE_MEDIUM])*Math.abs(range - .5*Math.max(self.jumpMP, .8*self.runMP));
        } else if (option.damage < .25*self.RangeDamages[self.RANGE_LONG]) {
          temp_adjustment += ((range < 10)?.25:1)*(Math.max(1+self.RangeDamages[self.RANGE_LONG],3))*(1/(1+option.threat));
        }
        adjustment += Math.sqrt(temp_adjustment*enemy.bv/self.bv);
        //I would always like to face the opponent (the most open direction)
        if (!(enemy.entity.isProne() || enemy.entity.isImmobile()) && Compute.getThreatHitArc(option.curPos, option.curFacing, enemy.entity.getPosition()) != CEntity.SIDE_FRONT) {
          int fa = Compute.getFiringAngle(option.curPos, option.curFacing, enemy.entity.getPosition());
          if (fa > 90 && fa < 270) {
            int distance = option.curPos.distance(enemy.old.curPos);
            double mod = 1;
            if (fa > 130 && fa < 240) mod = 2;
            //big formula that says don't do it
            mod *= ((Math.max(self.jumpMP, .8*self.runMP) < 5)?2:1)*
                    ((double)self.bv/(double)50)*Math.sqrt(((double)self.bv)/enemy.bv)/((double)distance/6 + 1);
            option.self_threat += mod;
            option.tv.add(mod + " " + fa + " Back to enemy\n");
          }
        }
      }
      adjustment *= self.overall_armor_percent*self.strategy.attack/enemy_array.length;
      //fix for hiding in level 2 water
      //To a greedy bot, it always seems nice to stay in here...
      Hex h = game.board.getHex(option.curPos);
      if (h.contains(Terrain.WATER) && h.surface() > (self.entity.getElevation() + ((option.isProne)?0:1))) {
        double mod = (self.entity.heat + option.getMovementheatBuildup() <= 7)?100:30;
        adjustment += self.bv/mod;
      }
      //add them in now, then re-add them later
      if (self.Range > self.RANGE_SHORT) {
        int ele_dif = game.board.getHex(option.curPos).getElevation() - game.board.getHex(self.old.curPos).getElevation();
        adjustment -= (Math.max(ele_dif, 0) + 1)*((double)Compute.getTargetTerrainModifier(game, option.entity).getValue() + 1);
      }
      
      //close the range if nothing else and healthy
      if (option.damage < .25*self.RangeDamages[self.Range] && adjustment < self.RangeDamages[self.Range]) {
        for (int e = 0; e < enemy_array.length; e++) { // for each enemy
          Entity en = (Entity)enemy_array[e];
          CEntity enemy = this.enemies.get(en);
          int range = option.curPos.distance(enemy.old.curPos);
          if (range > 5)
          adjustment += Math.pow(self.overall_armor_percent, 2)*Math.sqrt((double)(range-4)*enemy.bv/(double)self.bv)/enemy_array.length;  
        }
      } 
        
      if (option.damage < .25*(1+self.RangeDamages[self.Range])) {
        option.self_threat += 2*adjustment;
      } else if (option.damage < .5*(1+self.RangeDamages[self.Range])) {
        option.self_threat += adjustment;
      }
      option.tv.add(option.self_threat+" Initial Damage Adjustment " +"\n");
    }
    Arrays.sort(move_array);
    
    //top 30 utility
    for (int j = 0; j < 30 && j < move_array.length; j++) {
      pass.put((EntityState)move_array[j]);
    }
    
    Arrays.sort(move_array, new Comparator() {
      public int compare(Object obj, Object obj1) {
        if (((EntityState)obj).damage - ((EntityState)obj).getUtility() >
        ((EntityState)obj1).damage - ((EntityState)obj1).getUtility()) {
          return -1;
        }
        return 1;
      }
    });
    
    //top 30 damage
    for (int i = 0; i < 30 && i < move_array.length ; i++) {
      pass.put((EntityState)move_array[i]);
    }
    
    //reduce self threat, and add bonus for terrain
    for (com.sun.java.util.collections.Iterator i = pass.values().iterator(); i.hasNext();) {
      EntityState option = (EntityState)i.next();
      option.setState();
      option.self_damage *= .5;
      option.self_threat *= .5;
      double terrain = 2*((double)Compute.getTargetTerrainModifier(game, option.entity).getValue());
      option.tv.add(terrain+" Terrain Adjusment " +"\n");  
      option.self_threat -= terrain;
    }
    
    move_array = pass.values().toArray();
    pass.clear();
    
    //pass should contains 30 ~ 60
    /******************************************
     * fourth pass, speculation on top moves
     * use averaging to filter
     ********************************************/
    for (int e = 0; e < enemy_array.length; e++) { // for each enemy
      Entity en = (Entity)enemy_array[e];
      CEntity enemy = this.enemies.get(en);
      //engage in speculation on "best choices" when you have lost the iniative
      if (enemy.canMove()) {
        Object[] enemy_move_array = enemy.calculateCounterMoves().toArray();
        Vector to_check = new Vector();
        //check some enemy moves
        for (int j = 0; j < move_array.length; j++) {
          EntityState option = null;
          to_check.clear();
          option = (EntityState)move_array[j];
          option.setState();
          //check for damning hexes specifically
          //could also look at intervening defensive
          Vector coord = new Vector();
          Coords back = option.curPos.translated((option.curFacing + 3) % 6);
          coord.add(back);
          coord.add(back.translated((option.curFacing + 2) % 6));
          coord.add(back.translated((option.curFacing + 4) % 6));
          coord.add(option.curPos.translated((option.curFacing)));
          coord.add(option.curPos.translated((option.curFacing + 1)%6));
          coord.add(option.curPos.translated((option.curFacing + 2)%6));
          coord.add(option.curPos.translated((option.curFacing + 4)%6));
          coord.add(option.curPos.translated((option.curFacing + 5)%6));
          Iterator ci = coord.iterator();
          while (ci.hasNext()) {
            Coords test = (Coords)ci.next();
            Vector c = enemy.findMoves(test);
            if (c.size() != 0) to_check.addAll(c);
          }
          int range = option.curPos.distance(enemy.old.curPos);
          int compare=0;
          if ((enemy.long_range) > range - Math.max(enemy.jumpMP, enemy.runMP)) {
            compare = 30;
          } else if (enemy.long_range > range) {
            compare = 10;
          }
          double mod = this.enemies_moved/this.getEnemyEntities().size();
          compare *= (1 + mod);
          for (int k = 0; k <= compare && k < enemy_move_array.length; k++) {
            if (enemy_move_array.length < compare) {
              to_check.add(enemy_move_array[k]);
            } else {
              int value = Compute.randomInt(enemy_move_array.length);
              if (value%2 == 1) {
                to_check.add(enemy_move_array[value]);
              } else {
                to_check.add(enemy_move_array[k]);
              }
            }
          }
          Iterator eo = to_check.iterator();
          while (eo.hasNext()) {
            EntityState enemy_option = (EntityState)eo.next();
            double max_threat = 0;
            double max_damage = 0;
            enemy_option.setState();
            int enemy_hit_arc = Compute.getThreatHitArc(enemy_option.curPos, enemy_option.curFacing, option.curPos);
            int self_hit_arc = Compute.getThreatHitArc(enemy_option.curPos, enemy_option.curFacing, option.curPos);
            if (enemy_option.isJumping) {
              enemy_hit_arc = Compute.ARC_FORWARD; //assume they will choose forward, but really should be the strongest
            }
            int[] modifiers = option.getModifiers(enemy_option.entity);
            if (modifiers[1] != ToHitData.IMPOSSIBLE) {
              self.engaged = true;
              if (!enemy_option.isJumping) {
                max_threat = option.getMaxModifiedDamage(enemy_option, this.enemies.get(en), modifiers[1], modifiers[EntityState.DEFENCE_PC]);
              } else {
                max_threat = .8*enemy.getModifiedDamage((modifiers[EntityState.DEFENCE_PC]==1)?CEntity.TT:CEntity.SIDE_FRONT, enemy_option.curPos.distance(option.curPos), modifiers[1]);
              }
              max_threat = self.getThreatUtility(max_threat, self_hit_arc);
            }
            if (modifiers[0] != ToHitData.IMPOSSIBLE) {
              self.engaged = true;
              max_damage = enemy_option.getMaxModifiedDamage(option, self, modifiers[0], modifiers[EntityState.ATTACK_PC]);
              max_damage = enemy.getThreatUtility(max_damage, enemy_hit_arc);
              if (option.isPhysical) {
                if (option.PhysicalTarget.entity.getId() == enemy.entity.getId()) {
                  max_damage = option.damages[e];
                } else {
                  max_damage = 0;
                }
              }
            }
            option.max_threats[e] = Math.max(max_threat, option.max_threats[e]);
            option.min_damages[e] = Math.min(option.min_damages[e], max_damage);
            if (max_threat - max_damage > option.threats[e] - option.damages[e]) {
              option.threats[e] = max_threat;
              option.damages[e] = max_damage;
              option.tv.add(max_threat+" Spec Threat "+e+"\n");
              option.tv.add(max_damage+" Spec Damage "+e+"\n");
            }
          }
          //update estimates
          option.damage = 0;
          option.threat = 0;
          for (int d = 0; d < option.damages.length; d++) {
            //my damage is the average of expected and min
            CEntity cen = this.enemies.get((Entity)enemy_array[e]);
            //rescale
            option.min_damages[e] /= cen.strategy.target;
            option.damages[e] /= cen.strategy.target;
            option.damage += (option.min_damages[e] + option.damages[e])/2;
            
            //my threat is average of absolute worst, and expected
            option.threat = Math.max(option.threat, option.max_threats[e] + option.threats[e])/2;
            option.threats[e] = (option.max_threats[e] + 2*option.threats[e])/3;
          }
        }
        //restore enemy
        enemy.old.setState();
      }
      self.old.setState();
    } //--end move speculation
    
    Arrays.sort(move_array);
    
    //top 30 utility
    for (int i = 0; i < 30 && i < move_array.length ; i++) {
      pass.put((EntityState)move_array[i]);
    }
/*    
    Arrays.sort(move_array, new Comparator() {
      public int compare(Object obj, Object obj1) {
        if (((EntityState)obj).damage*.5 - ((EntityState)obj).getUtility() >
        ((EntityState)obj1).damage*.5 - ((EntityState)obj1).getUtility()) {
          return -1;
        }
        return 1;
      }
    });
    
    //top 10 damage
    for (int i = 0; i < 10 && i < move_array.length ; i++) {
      pass.put((EntityState)move_array[i]);
    }
*/    
    for (com.sun.java.util.collections.Iterator i = pass.values().iterator(); i.hasNext();) {
      EntityState option = (EntityState)i.next();
      option.self_threat *= .5;
      option.self_damage *= .5;
    }
    
    move_array = pass.values().toArray();
    pass.clear();
    
    //pass should now be 20 ~ 40
    /**************************************************
     * fourth pass, final damage and threat approximation
     *  --prevents moves that from the previous pass would
     *    cause the mech to die
     ************************************************/
    if (self.engaged) {
      //int remaining = game.getNoOfEntities() - game.getEntitiesOwnedBy(getLocalPlayer());
      for (int j = 0; j < move_array.length; j++) {
        EntityState option = (EntityState)move_array[j];
        option.setState();
        //if (this.enemies_moved > 0) {
          GAAttack temp = this.bestAttack(option);
          if (temp != null) {
            //will increase the utility of states that all for overheat cooling, etc.
            option.damage = (option.damage + temp.getFittestChromosomesFitness())/2;
          } else {
            option.damage /= 2;
          }
          for (int e = 0; e < enemy_array.length; e++) { // for each enemy
            Entity en = (Entity)enemy_array[e];
            CEntity enemy = this.enemies.get(en);
            if (!enemy.canMove()) {
              option.threats[e] = (option.threats[e] + this.attackUtility(enemy.old, self))/2;
              option.tv.add(option.threats[e]+" Revised Threat "+e+" \n");
              if (!option.isPhysical) {
                if (temp != null) {
                  option.damages[e] = (option.damages[e] + temp.getDamageUtility(enemy))/2;
                  option.tv.add(option.damages[e]+" Revised Damage "+e+" \n");
                } else {
                  //probably zero, but just in case
                  option.damages[e] = option.min_damages[e];
                  option.tv.add(option.damages[e]+" Revised Damage "+e+" \n");
                }
                //this needs to be reworked
                if (option.curPos.distance(enemy.old.curPos) == 1) {
                  PhysicalOption p = this.getBestPhysicalAttack(option.entity.getId(), enemy.entity.getId());
                  if (p != null) {
                    option.damages[e] += p.expectedDmg;
                    option.tv.add(p.expectedDmg+" Physical Damage "+e+" \n");
                  }
                  p = this.getBestPhysicalAttack(enemy.entity.getId(),option.entity.getId());
                  if (p != null) {
                    option.threats[e] += .5*p.expectedDmg;
                    option.tv.add(.5*p.expectedDmg+" Physical Threat "+e+" \n");
                  }
                }
              }
            } else if (!option.isPhysical) { //enemy can move (not physical check just in case)
              if (temp != null) {
                option.damages[e] = (2*option.damages[e] + temp.getDamageUtility(enemy))/3;
              } else {
                option.damages[e] = option.min_damages[e];
              }
            } else {
              //get a more accurate estimate
              option.damages[e] /= Math.sqrt((double)enemy.bv/(double)self.bv);
              option.damage = option.damages[e];
            }
          }
          option.threat = 0;
          for (int d = 0; d < option.damages.length; d++) {
            option.threat += option.threats[d];
          }
          option.tv.add(option.threat+" Revised Threat Utility\n");
          option.tv.add(option.damage+" Revised Damage Utility\n");
        //}
      }
    }
    
    Arrays.sort(move_array);
    self.old.setState(); //just to make sure we are back to where we started
    
    /**********************************************
     * Return top twenty moves to the lance algorithm
     **********************************************/
    EntityState[] result = new EntityState[Math.min(move_array.length,20)];
    int offset = 0;
    for (int i = 0; i < Math.min(move_array.length,20); i++) {
      EntityState next = (EntityState)move_array[i];
      if (next.isPhysical && self.RangeDamages[self.RANGE_SHORT] > 5 && next.Doomed) {
        if (offset + 20 < move_array.length) {
          next = (EntityState)move_array[offset + 20];
          offset++;
        }
      }
      result[i] = next;
    }
    return result;
  }
  
  protected Vector calculateWeaponAttacks(Entity en, Mounted mw) {
    return this.calculateWeaponAttacks(en, mw, false);
  }
  
  protected void initFiring() {
    Object[] entities = Compute.vectorToArray(game.getEntitiesVector());
    for (int i = 0; i < entities.length; i++) {
      Entity entity = (Entity)entities[i];
      CEntity centity = this.enemies.get(entity);
      centity.reset();
      centity.enemy_num = i;
    }
    for (Iterator i = this.getEnemyEntities().iterator(); i.hasNext();) {
      Entity entity = (Entity)i.next();
      CEntity centity = this.enemies.get(entity);
      if (entity.isMakingDfa() || entity.isCharging()) {
        //need to get the toHit values
        centity.strategy.target = 2.5;
      }
    }
  }
  
  /* returns a collection of all possible destination hexes */
  protected Vector calculateWeaponAttacks(Entity en, Mounted mw, boolean best_only) {
    int from = en.getId();
    int weaponID = en.getEquipmentNum(mw);
    Vector result = new Vector();
    Enumeration ents = game.getValidTargets(en).elements();
    AttackOption a = null;
    AttackOption max = new AttackOption(null,null,0,null);
    while (ents.hasMoreElements()) {
      Entity e = (Entity)ents.nextElement();
      CEntity enemy = enemies.get(e);
      ToHitData th = Compute.toHitWeapon(game, from, e, weaponID);
      if (th.getValue() != ToHitData.IMPOSSIBLE && !(th.getValue() >= 13)) {
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
		  Compute.getExpectedDamage((WeaponType)mw.getType());
	  }

	  // Infantry in the open suffer double damage.
	  if ( e instanceof Infantry ) {
	      Hex e_hex = game.getBoard().getHex( e.getPosition() );
	      if ( !e_hex.contains(Terrain.WOODS) &&
		   !e_hex.contains(Terrain.BUILDING) ) {
		  expectedDmg *= 2;
	      }
	  }

        a = new AttackOption(enemy, mw, expectedDmg, th);
        if (a.value > max.value) {
          if (best_only) {
            max = a;
          } else {
            result.add(0, a);
          }
        } else {
          result.add(a);
        }
      }
    }
    if (best_only && max.target != null) {
      result.add(max);
    }
    if (result.size() > 0) {
      result.add(new AttackOption(null,mw,0,null));
    }
    return result;
  }
  
  public double attackUtility(EntityState es) {
    return this.attackUtility(es, null);
  }
  
  public GAAttack bestAttack(EntityState es) {
    Entity en = es.entity;
    int attacks[] = new int[3];
    Vector front = new Vector();
    Vector left = new Vector();
    Vector right = new Vector();
    GAAttack result = null;
    int o_facing = en.getFacing();
    for (Enumeration i = en.getWeapons();i.hasMoreElements();) {
      Mounted mw = (Mounted)i.nextElement();
      Vector c = this.calculateWeaponAttacks(en, mw,true);
      if (c.size() > 0) {
        front.add(c);
        attacks[0] = Math.max(attacks[0], c.size());
      }
      if (!en.isProne()) {
        en.setSecondaryFacing((o_facing + 5)%6);
        c = this.calculateWeaponAttacks(en, mw,true);
        if (c.size() > 0) {
          left.add(c);
          attacks[1] = Math.max(attacks[1], c.size());
        }
        en.setSecondaryFacing((o_facing + 1)%6);
        c = this.calculateWeaponAttacks(en, mw,true);
        if (c.size() > 0) {
          right.add(c);
          attacks[2] = Math.max(attacks[2], c.size());
        }
      }
      en.setSecondaryFacing(o_facing);
    }
    Vector arcs = new Vector();
    arcs.add(front);
    arcs.add(left);
    arcs.add(right);
    double max = 0;
    for (int i = 0; i < arcs.size(); i++) {
      Vector v = (Vector)arcs.elementAt(i);
      if (v.size() > 0) {
        try {
          GAAttack test = new GAAttack(game, this.enemies.get(en), v,Math.max((v.size()+attacks[i])*2, 40),30, en.isEnemyOf((Entity)getEntitiesOwned().elementAt(0)));
          Thread threadTest = new Thread(test);
          threadTest.start();
          threadTest.join();
          if (test.getFittestChromosomesFitness() > max) {
            max = test.getFittestChromosomesFitness();
            result = test;
          }
        } catch (GAException gae) {
          System.out.println(gae.getMessage());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return result;
  }
  
  /* could use best of best strategy instead of expensive ga */
  public double attackUtility(EntityState es, CEntity target) {
    Entity en = es.entity;
    int attacks[] = new int[3];
    Vector front = new Vector();
    Vector left = new Vector();
    Vector right = new Vector();
    
    int o_facing = en.getFacing();
    for (Enumeration i = en.getWeapons();i.hasMoreElements();) {
      Mounted mw = (Mounted)i.nextElement();
      Vector c = this.calculateWeaponAttacks(en, mw,true);
      if (c.size() > 0) {
        front.add(c);
        attacks[0] = Math.max(attacks[0], c.size());
      }
      if (!en.isProne()) {
        en.setSecondaryFacing((o_facing + 5)%6);
        c = this.calculateWeaponAttacks(en, mw,true);
        if (c.size() > 0) {
          left.add(c);
          attacks[1] = Math.max(attacks[1], c.size());
        }
        en.setSecondaryFacing((o_facing + 1)%6);
        c = this.calculateWeaponAttacks(en, mw,true);
        if (c.size() > 0) {
          right.add(c);
          attacks[2] = Math.max(attacks[2], c.size());
        }
      }
      en.setSecondaryFacing(o_facing);
    }
    Vector arcs = new Vector();
    arcs.add(front);
    arcs.add(left);
    arcs.add(right);
    double max = 0;
    for (int i = 0; i < arcs.size(); i++) {
      Vector v = (Vector)arcs.elementAt(i);
      if (v.size() > 0) {
        try {
          GAAttack test = new GAAttack(game, this.enemies.get(en), v,Math.max((v.size()+attacks[i])*2, 20),30, en.isEnemyOf((Entity)getEntitiesOwned().elementAt(0)));
          Thread threadTest = new Thread(test);
          threadTest.start();
          threadTest.join();
          if (target != null) {
            max = Math.max(max, test.getDamageUtility(target));
          } else if (test.getFittestChromosomesFitness() > max) {
            max = test.getFittestChromosomesFitness();
          }
        } catch (GAException gae) {
          System.out.println(gae.getMessage());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return max;
  }
  
  public void calculateFiringTurn() {
    int first_entity = game.getFirstEntityNum();
    int entity_num = first_entity;
    int best_entity = first_entity;
    double max = java.lang.Double.MIN_VALUE;
    int[] results = null;
    Vector winner = null;
    int arc = 0;
    
    if (entity_num == -1) { return; }
    
    do {
      Entity en = game.getEntity(entity_num);
      CEntity cen = this.enemies.get(en);

      //if (en.canFlipArms())  //more logic
      
      int attacks[] = new int[3];
      Vector front = new Vector();
      Vector left = new Vector();
      Vector right = new Vector();
      
      int o_facing = en.getFacing();
      for (Enumeration i = en.getWeapons();i.hasMoreElements();) {
        Mounted mw = (Mounted)i.nextElement();
        Vector c = this.calculateWeaponAttacks(en, mw);
        if (c.size() > 0) {
          front.add(c);
          attacks[0] = Math.max(attacks[0], c.size());
        }
        en.setSecondaryFacing((o_facing + 5)%6);
        c = this.calculateWeaponAttacks(en, mw);
        if (c.size() > 0) {
          left.add(c);
          attacks[1] = Math.max(attacks[1], c.size());
        }
        en.setSecondaryFacing((o_facing + 1)%6);
        c = this.calculateWeaponAttacks(en, mw);
        if (c.size() > 0) {
          right.add(c);
          attacks[2] = Math.max(attacks[2], c.size());
        }
        en.setSecondaryFacing(o_facing);
      }
      Vector arcs = new Vector();
      arcs.add(front);
      if (!cen.entity.isProne()){
        arcs.add(left);
        arcs.add(right);
      }
      for (int i = 0; i < arcs.size(); i++) {
        Vector v = (Vector)arcs.elementAt(i);
        if (v.size() > 0) {
          try {
            GAAttack test = new GAAttack(game, this.enemies.get(en), v, Math.max((v.size()+attacks[i])*4,50), 100, en.isEnemyOf((Entity)getEntitiesOwned().elementAt(0)));
            Thread threadTest = new Thread(test);
            threadTest.start();
            threadTest.join();
            if (test.getFittestChromosomesFitness() > max) {
              max = test.getFittestChromosomesFitness();
              results = test.getResultChromosome();
              arc = i;
              best_entity = entity_num;
              winner = (Vector)arcs.elementAt(arc);
            }
          } catch (GAException gae) {
            System.out.println(gae.getMessage());
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      entity_num = game.getNextEntityNum(entity_num);
    } while (entity_num != first_entity && entity_num != -1);
    
    java.util.Vector av = new java.util.Vector();
    //maximum already selected (or default)
    Entity en = game.getEntity(best_entity);
    if (results != null) {
      Entity primary_target = (Entity)(Compute.vectorToArray(game.getEntitiesVector())[results[results.length - 1]]);
      TreeMap tm = new TreeMap(new TestBot.AttackOptionSorter(this.enemies.get(primary_target)));
      for (int i = 0; i < results.length - 1; i++) {
        AttackOption a = (AttackOption)((Vector)winner.elementAt(i)).elementAt(results[i]);
        if (a.target != null) {
          a.target.expected_damage[a.toHit.getSideTable()] += a.value;
          a.target.hasTakenDamage = true;
          tm.put(a, a);
        }
      }
      com.sun.java.util.collections.Iterator i = tm.values().iterator();
      while (i.hasNext()) {
        AttackOption a = (AttackOption)i.next();
        av.addElement(new WeaponAttackAction(en.getId(),a.target.entity.getId(), en.getEquipmentNum(a.weapon)));
      }
    }
    switch (arc) {
      case 1:
        av.insertElementAt(new TorsoTwistAction(en.getId(), (en.getFacing() + 5)%6),0);
        break;
      case 2:
        av.insertElementAt(new TorsoTwistAction(en.getId(), (en.getFacing() + 1)%6),0);
        break;
    }
    //System.out.println(max);
    sendAttackData(best_entity, av);
  }
  
  boolean initMovement = false;
  
  class InitMoveThread extends Thread {
    public InitMoveThread() {
      this.setPriority(Thread.MIN_PRIORITY);
    }
    public void run() {
      preMovement();
    }
  }
  
  protected void initMovement() {
    InitMoveThread imt = new InitMoveThread();
    imt.start();
    try {
      imt.join();
    } catch (Exception e) {}
  }
  
  int NumEnemies = 0;
  int NumFriends = 0;
  
  protected void preMovement() {
    this.my_mechs_moved = 0;
    this.old_moves = null;
    this.enemies_moved = 0;
    double max_modifier = 1.4;
    Object[] entities = Compute.vectorToArray(game.getEntitiesVector());
    double num_entities = Math.sqrt(entities.length)/100;
    Vector friends = new Vector();
    Vector foes = new Vector();
    double friend_sum = 0;
    double foe_sum = 0;
    double max_foe_bv = 0;
    CEntity max_foe = null;
    for (int i = 0; i < entities.length; i++) {
      Entity entity = (Entity)entities[i];
      CEntity centity = this.enemies.get(entity);
      centity.enemy_num = i;
      double old_value = centity.bv*(centity.overall_armor_percent + 1);
      centity.reset(); //should get fresh values
      double new_value = centity.bv*(centity.overall_armor_percent + 1);
      double percent = 1 + (new_value - old_value)/old_value;
      if (entity.getOwner().equals(getLocalPlayer())) {
        friends.add(centity);
        friend_sum += new_value;
        if (percent < .85) {
          //small retreat
          centity.strategy.attack = .85;
        } else if (percent < .95) {
          centity.strategy.attack = 1;
        } else if (percent <= 1 && centity.strategy.attack < max_modifier) {
          if (percent == 1) {
            if (centity.strategy.attack < 1) {
              centity.strategy.attack = Math.min(1.4*centity.strategy.attack,1);
            } else {
              centity.strategy.attack *= (1.0 + num_entities);
            }
          } else {
            centity.strategy.attack *= (1.0 + 2*num_entities);
          }
        }
      } else if(!entity.getOwner().isEnemyOf(getLocalPlayer())) {
        friend_sum += new_value;
      } else {
        foes.add(centity);
        foe_sum += new_value;
        if (new_value > max_foe_bv) {
          max_foe_bv = new_value;
          max_foe = centity;
        }
        if (this.getEntitiesOwned().size() > 2) {
          if (centity.strategy.target > 2) {
            centity.strategy.target = 1 + .5*(centity.strategy.target - 2);
          }
          if (percent < .85 && centity.strategy.target < max_modifier) {
            centity.strategy.target *= (1.0 + 6*num_entities);
          } else if (percent < .95 && centity.strategy.target < max_modifier) {
            centity.strategy.target *= (1.0 + 4*num_entities);
          } else if (percent <= 1) {
            if (percent == 1) {
              centity.strategy.target /= (1.0 + 2*num_entities);
            } else {
              centity.strategy.target /= (1.0 + num_entities);
            }
          }
          //don't go below one
          if (centity.strategy.target < 1) centity.strategy.target = 1;
          //go after good pilots
          //if (centity.gunnery + centity.piloting < 8) centity.strategy.target += 1;
        }
      }
    }
    this.NumFriends = friends.size();    
    this.NumEnemies = foes.size();
    System.out.println("Us "+friend_sum+" Them "+foe_sum);
    //do some more reasoning...
    if (this.unit_values.size() == 0) {
      this.unit_values.add(new Double(friend_sum));
      this.enemy_values.add(new Double(foe_sum));
      return;
    }
    Iterator i = foes.iterator();

    if (this.NumFriends > 1) {
      if (Strategy.MainTarget == null || null == game.getEntity(Strategy.MainTarget.entity.getId())) {
        Strategy.MainTarget = max_foe;
      }
      Strategy.MainTarget.strategy.target += .2;
      while(i.hasNext()) {
        CEntity centity = (CEntity)i.next();
        // good turn, keep up the work, but randomize to reduce predictability
        if (friend_sum - foe_sum >=
        .9*(((Double)this.unit_values.getLast()).doubleValue() -
        ((Double)this.enemy_values.getLast()).doubleValue())) {
          if (Compute.randomInt(2) == 1) {
            centity.strategy.target += .3;
          }
        //lost that turn, but still in the fight, just get a little more aggressive
        } else if (friend_sum > .9*foe_sum) {
          centity.strategy.target += .15;
        //lost that turn and loosing
        } else if (centity.strategy.target < 2) { //go for the gusto
          centity.strategy.target += .3;  
        }
        System.out.println(centity.entity.getShortName() + " " + centity.strategy.target);
      }
    }
        
        
    double ratio = friend_sum/foe_sum;
    double mod = 1;
    if (ratio < .9) {
      mod = .95;
    } else if (ratio < 1) {
      //no change
    } else { //attack
      mod = (1.0 + num_entities);
    }
    i = friends.iterator();
    while (i.hasNext()) {
      CEntity centity = (CEntity)i.next();
      if (!(mod < 1 && centity.strategy.attack < .6) && !(mod > 1 && centity.strategy.attack >= max_modifier))
        centity.strategy.attack *= mod;
      System.out.println(centity.strategy.attack);
    }
    System.gc(); //just to make sure
  }
  
  protected void processChat(GameEvent ge) {
    //this should be in a thread...
    if (ge.getType() != GameEvent.GAME_PLAYER_CHAT) return;
    if (this.getLocalPlayer() == null) return;
    StringTokenizer st = new StringTokenizer(ge.getMessage(), ":");
    if (st.hasMoreTokens()) {
      String name = st.nextToken().trim();
      //who is the message from?
      Enumeration e = game.getPlayers();
      boolean found = false;
      Player p = null;
      while (e.hasMoreElements() && !found) {
        p = (Player)e.nextElement();
        if (name.equalsIgnoreCase(p.getName())) {
          found = true;
        }
      }
      if (found) {
        try {
        if (st.hasMoreTokens() && st.nextToken().trim().equalsIgnoreCase(this.getLocalPlayer().getName())) {
          if (!p.isEnemyOf(this.getLocalPlayer())) {
            if (st.hasMoreTokens()) {
              String command = st.nextToken().trim();
              boolean understood = false;
              //should create a command factory and a command object...
              if (command.equalsIgnoreCase("echo")) {
                understood = true;
              } if (command.equalsIgnoreCase("calm down")) {
                Iterator i = this.getEntitiesOwned().iterator();
                while (i.hasNext()) {
                  CEntity cen = this.enemies.get((Entity)i.next());
                  if (cen.strategy.attack > 1) {
                    cen.strategy.attack = 1;
                  }
                }
                understood = true;
              } else if (command.equalsIgnoreCase("be aggressive")) {
                Iterator i = this.getEntitiesOwned().iterator();
                while (i.hasNext()) {
                  CEntity cen = this.enemies.get((Entity)i.next());
                  cen.strategy.attack = Math.min(cen.strategy.attack*1.2, 1.5);
                }
                understood = true;
              } else if (command.equalsIgnoreCase("attack")) {
                int x = Integer.parseInt(st.nextToken().trim());
                int y = Integer.parseInt(st.nextToken().trim());
                Entity en = game.getFirstEntity(new Coords(x - 1, y - 1));
                if (en != null) {
                  if (en.isEnemyOf((Entity)this.getEntitiesOwned().elementAt(0))) {
                    CEntity cen = this.enemies.get(en);
                    cen.strategy.target += 3;
                    System.out.println(cen.entity.getShortName() + " " +  cen.strategy.target);
                    understood = true;
                  }
                }
              }
              if (understood) sendChat("Understood "+p.getName());
            }
          } else {
            sendChat("I can't do that, "+p.getName());
          }
        }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }
  }
  
  public static void main(String args[]) {
   /* Frame frame1 = new Frame() {
      public void paintAll(java.awt.Graphics g) {
        return;
      }
      public void repaint() {
        return;
      }
      public void update() {
        return;
      }
      public void setVisible(boolean b) {
        super.setVisible(false);
      }
    };
    Frame frame2 = new Frame() {
      public void paintAll(java.awt.Graphics g) {
        return;
      }
      public void repaint() {
        return;
      }
      public void update() {
        return;
      }
      public void setVisible(boolean b) {
        super.setVisible(false);
      }
    };*/
    Frame frame1 = new Frame();
    Frame frame2 = new Frame();
    int total1 = 0;
    int total2 = 0;
    for (int run = 1; run <= 5; run++) {
      megamek.server.Server s = new megamek.server.Server("hello", 2348);
      ConnectionThread c1 = new ConnectionThread("Player 1", BotFactory.TEST, 0, frame1);
      ConnectionThread c2 = new ConnectionThread("Player 2", BotFactory.HUMAN, 1, frame2);
      System.gc();
      c1.start();
      c2.start();
      try {
        c2.join();
        c1.join();
      } catch (Exception e) {}
      s.die();
      total1 += ((BotClientWrapper)c1.result.client).winner;
      total2 += ((BotClientWrapper)c2.result.client).winner;
      System.out.println("Trial "+run+" "+total1+" "+total2);
      System.gc();
    }
    System.out.println("Player 1: "+total1);
    System.out.println("Player 2: "+total2);
  }
  
}

class PlayBot {
  public static void main(String[] args) {
    Frame frame1 = new Frame();
    Frame frame2 = new Frame();
    megamek.server.Server s = new megamek.server.Server("hello", 2348);
    ConnectionThread c1 = new ConnectionThread("Player 0", BotFactory.TEST, 0, frame1);
    ConnectionThread c2 = new ConnectionThread("Player 1", BotFactory.HUMAN, 2, frame2);
    c1.start();
    c2.start();
  }
}

class ConnectionThread extends Thread {
  String name;
  int type;
  int location;
  MegaMek result;
  Frame frame;
  public ConnectionThread(String name,int type,int location, Frame frame) {
    this.name = name;
    this.type = type;
    this.location = location;
    this.frame = frame;
  }
  public void run() {
    boolean die = false;
    try {
      MegaMek mm = new MegaMek(frame);
      mm.client = BotFactory.getBot(type,frame, name);
      if(!mm.client.connect("localhost", 2348)) {
        return;
      }
      sleep(500);
      mm.client.retrieveServerInfo();
      /*File f = new java.io.File("d:/Projects/megamek/data/mep/");
      File[] reqs = f.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.endsWith(".MEP");
        }
      });
      f = reqs[Compute.randomInt(reqs.length)];
      Mech mech = new MepFile(f.getAbsolutePath()).getMech();*/
      //Mech mech = new MepFile("d:/Projects/current_megamek/data/mep/more/EXT-4A Exterminator.MEP").getMech();
      //Mech mech = (Mech)(new BLKMechFile("d:/Projects/current_megamek/data/blk/Warhammer WHM-6K.blk").getEntity());
      //Mech mech = new MepFile("d:/Projects/current_megamek/data/mep/ASN-21 Assassin.MEP").getMech();
      
      // um, this is sure to fail...
      Mech mech = null;
      
      mech.setOwner(mm.client.getLocalPlayer());
      mm.client.sendAddEntity(mech);
      mm.client.sendAddEntity(mech);
      mm.client.sendAddEntity(mech);
      sleep(500);
      mm.client.getLocalPlayer().setStartingPos(location);
      mm.client.sendPlayerInfo();
      sleep(500);
      this.result = mm;
      //result.client.sendReady(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
    while (!die) {
      try {
        sleep(2000);
        if (((BotClientWrapper)result.client).winner == -1 || ((BotClientWrapper)result.client).winner == 1) {
          die = true;
        }
      } catch (Exception e) {}
    }
  }
}
