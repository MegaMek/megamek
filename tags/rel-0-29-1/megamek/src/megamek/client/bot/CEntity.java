/*
 * CEntity.java
 *
 * Created on May 12, 2002, 11:56 AM
 */

package megamek.client.bot;

import megamek.*;
import megamek.common.*;
import megamek.common.actions.*;

import com.sun.java.util.collections.*;
import java.util.Enumeration;

/**
 *
 * @author  Steve Hawkins
 */
public class CEntity  {
  
  //reference to the current game
  public static Game game;
  public static TestBot tb;
  
  //some helpful constants
  public static final int MAX_RANGE = 24;
  
  public static final int OVERHEAT_NONE = 0;
  public static final int OVERHEAT_LOW = 1;
  public static final int OVERHEAT_HIGH = 2;
  
  public static final int RANGE_SHORT = 0;
  public static final int RANGE_MEDIUM = 1;
  public static final int RANGE_LONG = 2;
  public static final int RANGE_ALL = 3;
  
  public static final int FIRST_ARC = 0;
  public static final int LAST_PRIMARY_ARC = 3;
  public static final int LAST_ARC = 4;
  
  public static final int SIDE_FRONT      = 0;
  public static final int SIDE_REAR       = 1;
  public static final int SIDE_LEFT       = 2;
  public static final int SIDE_RIGHT      = 3;
  public static final int TT              = 4;
  //there are more tt arcs, but I don't care about them
  
  public static final int LEFT_LEG = 0;
  public static final int RIGHT_LEG = 1;
  
  Entity entity;
  EntityState old = null;
  
  Vector moves = null;
  EntityState.Table run_walk_moves = null;
  EntityState.Table jump_hexes = null;
  public int runMP;
  public int jumpMP;
  
  boolean moved = false;
  boolean isPhysicalTarget = false;
  
  //will change based upon the pilot
  int gunnery = 4;
  int piloting = 5;
  
  int overheat = OVERHEAT_NONE;
  int Range = RANGE_ALL;
  int long_range = 0;
  double RangeDamages[] = new double[4];
  
  double base_psr_odds = 1.0;
  
  boolean hasTakenDamage = false;
  public Strategy strategy = new Strategy();
  
  //a subjective measure of the armor quality
  double[] armor_health = {0,0,0,0};
  double[] armor_percent = {0,0,0,0};
  double avg_armor = 0;
  double avg_iarmor = 0;
  
  //used to determine the utility of combining attacks
  double[] expected_damage = {0,0,0,0};
  double[] possible_damage = {0,0,0,0};
  
  double[] leg_health = {0,0};
  
  double overall_armor_percent = 0;
  double[][] damages = new double[6][MAX_RANGE+1];
  
  //the battle value of the mech
  int bv;
  
  //relative position in the enemy array
  int enemy_num;
  
  boolean engaged = false; //can I fight
  
  EntityState last; // set only after movement
  
  public CEntity(Entity en) {
    this.entity = en;
    this.reset();
  }
  
  public boolean canMove() {
    return (entity.isSelectable() && !(entity.isProne() && base_psr_odds < .2) && !entity.isImmobile());
  }
  
  public void reset() {
    //clear it out just in case
    for (int a = this.FIRST_ARC; a <= this.LAST_ARC; a++) {
      for (int r = 1; r <= this.MAX_RANGE; r++) {
        this.damages[a][r] = 0;
      }
    }
    this.characterize();
    this.resetPossibleDamage();
    this.moves = null;
    this.jump_hexes = null;
    this.run_walk_moves = null;
    this.hasTakenDamage = false;
    this.expected_damage = new double[] {0,0,0,0};
    this.refresh = true;
    this.engaged = false;
    this.moved = false;
    this.isPhysicalTarget = false;
  }
  
  public void refresh() {
    this.entity = game.getEntity(this.entity.getId()); //fresh entity object ensured
    if (this.refresh && !this.entity.isSelectable()) {
      //clear it out just in case
      for (int a = this.FIRST_ARC; a <= this.LAST_ARC; a++) {
        for (int r = 1; r <= this.MAX_RANGE; r++) {
          this.damages[a][r] = 0;
        }
      }
      this.characterize();
      this.resetPossibleDamage();
      this.jump_hexes = null;
      this.run_walk_moves = null;
      this.refresh = false;
    }
  }
  
  public void resetPossibleDamage() {
    for (int i = 0; i < this.possible_damage.length; i++) {
      this.possible_damage[i] = 0;
    }
  }
  
   int[] MinRanges = new int[7];
  
  public void characterize() {
    this.entity = game.getEntity(this.entity.getId());
    this.old = new EntityState(this);
    this.bv = entity.calculateBattleValue();
    this.gunnery = entity.getCrew().getGunnery();
    this.piloting = entity.getCrew().getGunnery();
    this.runMP = entity.getRunMP();
    this.jumpMP = entity.getJumpMP();
    this.overall_armor_percent = entity.getArmorRemainingPercent();
    this.base_psr_odds = Compute.oddsAbove(Compute.getBasePilotingRoll(game,entity.getId()).getValue())/100;
    //begin weapons characterization
    double heat_mod = .9; //these estimates are consistently too high
    if (entity.heat > 7) heat_mod = .8; //reduce effectiveness
    if (entity.heat > 12) heat_mod = .5;
    if (entity.heat > 16) heat_mod = .35;
    int capacity = entity.getHeatCapacity();
    int heat_total = 0;
    Enumeration weapons = entity.getWeapons();
    int num_weapons = 0;
    this.MinRanges = new int[7];
    while (weapons.hasMoreElements()) {
      num_weapons++;
      Mounted m = (Mounted)weapons.nextElement();
      int arc = entity.getWeaponArc(entity.getEquipmentNum(m));
      WeaponType weapon = (WeaponType)m.getType();
      final boolean usesAmmo = weapon.getAmmoType() != AmmoType.T_NA;
      final Mounted ammo = usesAmmo ? m.getLinked() : null;
      if (m.isDestroyed()) continue;
      if (usesAmmo && (ammo == null || ammo.getShotsLeft() == 0)) continue;
      heat_total += weapon.getHeat();
      int min = weapon.getMinimumRange();
      int sr = weapon.getShortRange();
      int mr = weapon.getMediumRange();
      int lr = weapon.getLongRange();
      double ed = Compute.getExpectedDamage(weapon);
      double odds = 0;
      for (int range = 1; range <= lr && range <= MAX_RANGE; range++) {
        if (range <= min) {
          if (range < 7) this.MinRanges[range] += 1 + min - range;
          odds = Compute.oddsAbove(this.gunnery + 1 + min - range)/100.0;
        } else if (range <= sr) {
          odds = Compute.oddsAbove(this.gunnery)/100.0;
        } else if (range <= mr) {
          odds = Compute.oddsAbove(this.gunnery + 2)/100.0;
        } else if (range <= lr) {
          odds = Compute.oddsAbove(this.gunnery + 4)/100.0;
        }
        //weapons unaffected by heat don't get penalized
        this.addDamage(arc, entity.isSecondaryArcWeapon(entity.getEquipmentNum(m)), range, ed*odds*((weapon.getHeat() > 0)?heat_mod:1));
        this.long_range = Math.max(this.long_range,range);
      }
    }
    for (int r = 1; r < this.MinRanges.length; r++) {
      if (num_weapons > 0) this.MinRanges[r] = (int)Math.round(((double)this.MinRanges[r])/(double)num_weapons);
      //System.out.println(this.MinRanges[r]);
    }
    /*for (int a = this.FIRST_ARC; a <= this.LAST_ARC; a++) {
      for (int r = 1; r <= this.MAX_RANGE; r++) {
        System.out.print(this.damages[a][r]+" ");
      }
      System.out.println();
    }*/
    //what type of overheater am I
    int heat = heat_total - capacity ;
    if (heat < 8 && heat > 3) {
      this.overheat = this.OVERHEAT_LOW;
    } else if (heat > 12) {
      this.overheat = this.OVERHEAT_HIGH;
    }
    //only worries about external armor
    double max = 1;
    for(int arc = this.FIRST_ARC; arc <= this.LAST_PRIMARY_ARC; arc++) {
      int total = 0;
      int points = 0;
      int temp[] = null;
      boolean rear = false;
      if (this.entity instanceof Tank) {
        switch (arc) {
            case ToHitData.SIDE_FRONT :
                temp = this.getArmorValues(Tank.LOC_FRONT, false);
                total += 2*temp[0];
                points += 2*temp[1];
                break;
            case ToHitData.SIDE_REAR :
                temp = this.getArmorValues(Tank.LOC_REAR, false);
                total += 2*temp[0];
                points += 2*temp[1];
                break;
            case ToHitData.SIDE_RIGHT :
                temp = this.getArmorValues(Tank.LOC_RIGHT, false);
                total += 2*temp[0];
                points += 2*temp[1];
                break;
            case ToHitData.SIDE_LEFT :
                temp = this.getArmorValues(Tank.LOC_LEFT, false);
                total += 2*temp[0];
                points += 2*temp[1];
                break;
        }
      }
      else if (this.entity instanceof Infantry) {
          for (int i = 0; i < this.entity.locations(); i++) {
               temp = this.getArmorValues(i, false);
               total += 2*temp[0];
               points += 2*temp[1];
          }
      }
      else {
           switch (arc) {
             case ToHitData.SIDE_REAR:
               rear = true;
             case ToHitData.SIDE_FRONT:
               temp = this.getArmorValues(Mech.LOC_CT,rear);
               total += 2*temp[0];
               points += 2*temp[1];
               temp = this.getArmorValues(Mech.LOC_RARM,rear);
               total += 2*temp[0];
               points += 2*temp[1];
               temp = this.getArmorValues(Mech.LOC_RLEG,rear);
               total += temp[0];
               points += temp[1];
               temp = this.getArmorValues(Mech.LOC_RT,rear);
               total += temp[0];
               points += temp[1];
               temp = this.getArmorValues(Mech.LOC_LT,rear);
               total += temp[0];
               points += temp[1];
               temp = this.getArmorValues(Mech.LOC_LLEG,rear);
               total += temp[0];
               points += temp[1];
               temp = this.getArmorValues(Mech.LOC_LARM,rear);
               total += 2*temp[0];
               points += 2*temp[1];
               temp = this.getArmorValues(Mech.LOC_HEAD,rear);
               total += temp[0];
               points += temp[1];
               break;
             case ToHitData.SIDE_LEFT:
               temp = this.getArmorValues(Mech.LOC_CT,rear);
               total += temp[0];
               points += temp[1];
               temp = this.getArmorValues(Mech.LOC_RARM,rear);
               total += temp[0];
               points += temp[1];
               temp = this.getArmorValues(Mech.LOC_RLEG,rear);
               this.leg_health[this.RIGHT_LEG] = temp[0]/entity.getOArmor(Mech.LOC_RLEG);
               total += temp[0];
               points += temp[1];
               temp = this.getArmorValues(Mech.LOC_RT,rear);
               total += temp[0];
               points += temp[1];
               temp = this.getArmorValues(Mech.LOC_LT,rear);
               total += 2*temp[0];
               points += 2*temp[1];
               temp = this.getArmorValues(Mech.LOC_LLEG,rear);
               this.leg_health[this.LEFT_LEG] = temp[0]/entity.getOArmor(Mech.LOC_LLEG);
               total += 2*temp[0];
               points += 2*temp[1];
               temp = this.getArmorValues(Mech.LOC_LARM,rear);
               total += 2*temp[0];
               points += 2*temp[1];
               temp = this.getArmorValues(Mech.LOC_HEAD,rear);
               total += temp[0];
               points += temp[1];
               break;
             case ToHitData.SIDE_RIGHT:
               temp = this.getArmorValues(Mech.LOC_CT,rear);
               total += temp[0];
               points += temp[1];
               temp = this.getArmorValues(Mech.LOC_RARM,rear);
               total += 2*temp[0];
               points += 2*temp[1];
               temp = this.getArmorValues(Mech.LOC_RLEG,rear);
               total += 2*temp[0];
               points += 2*temp[1];
               temp = this.getArmorValues(Mech.LOC_RT,rear);
               total += 2*temp[0];
               points += 2*temp[1];
               temp = this.getArmorValues(Mech.LOC_LT,rear);
               total += temp[0];
               points += temp[1];
               temp = this.getArmorValues(Mech.LOC_LLEG,rear);
               total += temp[0];
               points += temp[1];
               temp = this.getArmorValues(Mech.LOC_LARM,rear);
               total += temp[0];
               points += temp[1];
               temp = this.getArmorValues(Mech.LOC_HEAD,rear);
               total += temp[0];
               points += temp[1];
               break;
           }
      }
      this.armor_health[arc] = ((double)total*points)/242;
      if (this.armor_health[arc] > max) {
        max = this.armor_health[arc];
      }
      this.avg_armor = (armor_health[0] + armor_health[1] + armor_health[2] + armor_health[3])/4;
      this.avg_iarmor = this.entity.getTotalInternal()/5;
    }
    for(int arc = this.FIRST_ARC; arc <= this.LAST_PRIMARY_ARC; arc++) {
      this.armor_percent[arc] = this.armor_health[arc]/max;
    }
    
    this.computeRange();
  }
  
  /** Add a statistical damage into the damage table
   *  the arc is based upon firing arc Compute.XXXX
   *    --this is not yet exact, rear tt not accounted for, and
   *      arm flipping is ignored
   */
  protected void addDamage(int arc, boolean secondary, int range, double ed) {
    this.damages[Compute.firingArcToHitArc(arc)][range] += ed;
    if (arc != Compute.ARC_REAR && arc != Compute.ARC_360) {
      this.damages[this.SIDE_FRONT][range] += ed;
      if (!secondary) return;
      this.damages[this.TT][range] += ed;
      return;
    }
    if (arc == Compute.ARC_360) {
      for (int i = this.FIRST_ARC; i <= this.LAST_ARC; i++)
        this.damages[i][range] += ed;
      return;
    }
  }
  
  protected void computeRange() {
    double[] values = new double[3];
    this.RangeDamages[3] = 0;
    for (int base = 0; base < 3; base++) {
      for (int i = 1 + 6*base; i < 8 + 6*base; i++) {
        values[base] += this.damages[this.SIDE_FRONT][i];
      }
      values[base] /= 8;
      this.RangeDamages[base] = values[base];
      this.RangeDamages[3] += values[base];
    }
    //should use fuzzy membership...
    //but this works for now
    this.RangeDamages[3] /= 3;
    if (values[0] > 2.5*values[1]) {
      this.Range = this.RANGE_SHORT;
    } else if (values[1] > 2.5*values[2]) {
      this.Range = this.RANGE_MEDIUM;
    } else if (values[2] > .25*values[0]) {
      this.Range = this.RANGE_LONG;
    } else {
      this.Range = this.RANGE_ALL;
    }
    /*for (int r = 0; r < 4; r++) {
      System.out.print(this.RangeDamages[r]+" ");
    }
    System.out.println("\n"+this.entity.getName() +" "+ this.Range);*/
  }
  
  /** Helper method to return point and actual values for armor
   */
  protected int[] getArmorValues(int loc, boolean rear) {
    int[] result = new int[2];
    double percent = 0;
    result[0] = this.entity.getArmor(loc, rear);
    percent = result[0]/this.entity.getOArmor(loc, rear);
    if (percent < .25) {
      result[1] = 0;
    } else if (percent < .60) {
      result[1] = 1;
    } else {
      result[1] = 2;
    }
    return result;
  }
  
  /** The utility of something done against me.
   *   -- uses the arcs defined by ToHitData
   */
  
  /* --needs to be adjusted to understand too much damage 
   *   and useless damage.
   */
  public double getThreatUtility(double threat, int arc) {
    double t1 = threat;
    double t2 = threat;
    //relative bonus for weak side
    if (armor_percent[arc] < .75) {
      t1 *= 1.1;
    } else if (armor_percent[arc] < .5) {
      t1 *= 1.3;
    } else if (armor_percent[arc] < .25) {
      t1 *= 1.5;
    }
    //absolute bonus for damage that is likely to do critical
    if (t2 + this.expected_damage[arc] > this.armor_health[arc]){
      //damage saturation check, only if we have more mechs 
      if ((t2 + this.expected_damage[0] + this.expected_damage[1] + this.expected_damage[2] + this.expected_damage[3] > 3*(this.avg_armor + this.avg_iarmor)
       || (this.entity.isProne() && this.base_psr_odds < .1 && !this.entity.isImmobile())) && !(this.tb.NumFriends > this.tb.NumEnemies)
      ) {
        if (this.tb.NumEnemies == 1) {
          return t2 *= 2;
          //check and make sure this is an enemy
        } else if (this.entity.isEnemyOf((Entity)this.tb.getEntitiesOwned().elementAt(0))) {
          return Math.sqrt(t2)*this.strategy.target;
        }
      }
      t2 *= 1.5;
    } else if (this.expected_damage[arc] > 0) {
      t2 *= 1.3; //for clustering damage
    } else if (this.hasTakenDamage) {
      t2 *= 1.1; //for coordinating fire
    }
    return Math.max(t1,t2)*this.strategy.target;
  }
  
  public Integer getKey() {
    return new Integer(this.entity.getId());
  }
  
  public Vector getAllMoves() {
    this.run_walk_moves = null;
    Vector ground = this.calculateRunWalkOptions();
    Iterator i = ground.iterator();
    while (i.hasNext()) {
      EntityState next = (EntityState)i.next();
      if (!next.isMoveLegal()) {
        i.remove();
      }
    }
    this.jump_hexes = null;
    Vector jumps = this.calculateJumpOptions();
    i = jumps.iterator();
    while (i.hasNext()) {
      EntityState next = (EntityState)i.next();
      if (!next.isMoveLegal()) {
        i.remove();
      }
    }
    Object j_array[] = jumps.toArray();
    for(int j = 0; j < j_array.length; j++) {
      EntityState next = new EntityState((EntityState)j_array[j]);
      if (next.isPhysical) continue;
      for(int f = 0; f < 6; f++) {
        next.addStep(MovementData.STEP_TURN_RIGHT);
        jumps.add(next);
        next = new EntityState(next);
      }
    }
    ground.addAll(jumps);
    return ground;
  }
  
  /** Enumerates a set of "best" movements
   *
   * The processing of all move options may not be the best policy,
   * rather an enumeration of all the legal hexes would cut the
   * initial search space down by ~4, then "good" hexes could be
   * examined for all facings.
   */
  protected Vector calculateRunWalkOptions() {
    if (this.run_walk_moves != null) {
      Vector result = new Vector();
      com.sun.java.util.collections.Iterator i = this.run_walk_moves.values().iterator();
      while (i.hasNext()) {
        result.add(new EntityState((EntityState)i.next()));
      }
      return result;
    }
    Entity en = entity;
    EntityState next = null;
    
    EntityState.Table possible = new EntityState.Table();
    EntityState.Table discovered = new EntityState.Table();
    
    EntityState current = new EntityState(old);
    
    //always have the option to stand still
    possible.put(current);
    discovered.put(current);
    
    //if prone get up;
    if (en.isProne()) {
      next = new EntityState(current);
      next.addStep(MovementData.STEP_TURN_LEFT);
      discovered.put(next);
      next = new EntityState(current);
      next.addStep(MovementData.STEP_TURN_RIGHT);
      discovered.put(next);  
      if (this.base_psr_odds < .25) { //change facing if that helps
        possible.clear();
      } else {
        next = new EntityState(current);
        next.addStep(MovementData.STEP_GET_UP);
        //if legal to stand, add all facings
        if (next.isStepLegal()) {
          possible.put(next);
          discovered.put(next);
          for (int i = 0; i < 5; i++) {
            next = new EntityState(next);
            next.addStep(MovementData.STEP_TURN_RIGHT);
            discovered.put(next);
            possible.put(next);
          }
        }
      }
    }
    while (possible.size() > 0) {
      EntityState min = possible.extractMin();
      Vector adjacent = new Vector();
      //forward
      if (min.firstStep || min.getLastStep().getType() != MovementData.STEP_BACKWARDS) {
        next = new EntityState(min);
        next.addStep(MovementData.STEP_FORWARDS);
        adjacent.add(next);
      }
      //turn left
      if (min.firstStep || min.getLastStep().getType() != MovementData.STEP_TURN_RIGHT) {
        next = new EntityState(min);
        next.addStep(MovementData.STEP_TURN_LEFT);
        adjacent.add(next);
      }
      //turn right
      if (min.firstStep || min.getLastStep().getType() != MovementData.STEP_TURN_LEFT) {
        next = new EntityState(min);
        next.addStep(MovementData.STEP_TURN_RIGHT);
        adjacent.add(next);
      }
      //move backward
      if (min.firstStep || min.getLastStep().getType() != MovementData.STEP_FORWARDS
      && min.overallMoveType != Entity.MOVE_RUN) {
        next = new EntityState(min);
        next.addStep(MovementData.STEP_BACKWARDS);
        adjacent.add(next);
      }
      for (int i = 0; i < adjacent.size(); i++) {
        next = (EntityState)adjacent.elementAt(i);
        if (next.changeToPhysical()) {
          discovered.put(next);
        } else if (next.isStepLegal()) {
          //relax edges;
          if (discovered.get(next) == null) {
            discovered.put(next);
            possible.put(next);
          } else if (next.mpUsed < discovered.get(next).mpUsed) {
            possible.update(next);
            discovered.update(next);
          }
        }
      }
    }
    com.sun.java.util.collections.Iterator i = discovered.values().iterator();
    Vector result = new Vector();
    while (i.hasNext()) {
      next = (EntityState)i.next();
      if (entity.heat > 4) {
        next.movement_threat += this.bv/1000*next.getMovementheatBuildup();
        if (entity.heat > 7) {
          next.movement_threat += this.bv/500*next.getMovementheatBuildup();
        }
        if (entity.heat > 12) {
          next.movement_threat += this.bv/100*next.getMovementheatBuildup();
        }
      }
      result.add(new EntityState(next));
    }
    this.run_walk_moves = discovered;
    return result;
  }
  
  /** Creates a hashtable of jump hexes, made available at this.jump_hexes
   *  and also returns
   *  doesn't check for legality, that is done in getAllMoves
   */
  protected Vector calculateJumpOptions() {
    if (this.jump_hexes != null) {
      Vector result = new Vector();
      com.sun.java.util.collections.Iterator i = this.jump_hexes.values().iterator();
      while (i.hasNext()) {
        result.add(new EntityState((EntityState)i.next()));
      }
      return result;
    }
    Entity en = entity;
    Vector result = new Vector();
    //mech has no jump ability
    EntityState.Table possible = new EntityState.Table();
    EntityState.Table discovered = new EntityState.Table();
    this.jump_hexes = discovered;
    
    if (en.getJumpMP() == 0) return result;
    
    EntityState current = new EntityState(this);
    EntityState next = null;
    
    current.addStep(MovementData.STEP_START_JUMP);
    if (!current.isStepLegal()) return result;
    
    possible.put(current);
    discovered.put(current);
    
    if (entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO,Mech.LOC_CT) > 0 || entity.hasLegActuatorCrit()) {
      if (this.base_psr_odds > .2) { 
        double mod = 1;
        if (this.base_psr_odds < .5) {
          mod = 3;
        }
        current.movement_threat += mod*.1*entity.getWeight()*(1 - 1/2*this.base_psr_odds);
        current.tv.add(mod*.1*entity.getWeight()*(1 - 1/2*this.base_psr_odds)+" Jump Threat");
      } else {
        possible.clear();
        discovered.clear();
      }
    }
    
    while (possible.size() > 0) {
      EntityState min = possible.extractMin();
      if (min.entity.getJumpMP() > min.mpUsed) {
        //create neighbors
        Vector adjacent = new Vector();
        adjacent.add(new EntityState(min));
        for (int i = 0; i < 5; i++) {
          next = new EntityState((EntityState)adjacent.elementAt(i));
          next.addStep(MovementData.STEP_TURN_RIGHT);
          adjacent.add(next);
        }
        //visit neighbors
        for (int i = 0; i < 6; i++) {
          next = (EntityState)adjacent.elementAt(i);
          next.addStep(MovementData.STEP_FORWARDS);
          if (next.changeToPhysical()) {
            discovered.put(next);
          } else if (next.isStepLegal()) {
            //relax edges;
            if (discovered.get(next) == null) {
              discovered.put(next);
              possible.put(next);
            } else if (next.mpUsed < discovered.get(next).mpUsed) {
              possible.update(next);
              discovered.update(next);
            }
          }
        }
      }
    }
    discovered.remove(old); //doesn't need to be considered
    com.sun.java.util.collections.Iterator i = discovered.values().iterator();
    while (i.hasNext()) {
      next = (EntityState)i.next();
      if (entity.heat > 4) {
        next.movement_threat += this.bv/1000*next.getMovementheatBuildup();
        if (entity.heat > 7) {
          next.movement_threat += this.bv/500*next.getMovementheatBuildup();
        }
        if (entity.heat > 12) {
          next.movement_threat += this.bv/100*next.getMovementheatBuildup();
        }
      }
      Hex h = game.board.getHex(next.curPos);
      //jumping into water is basically as bad as walking into it until the jj
      //bug is fixed
      if (h.contains(Terrain.WATER) && h.surface() - h.floor() > 0) {
        next.movement_threat += this.getThreatUtility(.2*this.entity.getWeight()*(1 - this.base_psr_odds), this.SIDE_REAR);
        next.tv.add(this.getThreatUtility(.2*this.entity.getWeight()*(1 - this.base_psr_odds), this.SIDE_REAR) +" Spec Jump Threat \n");
      }
      result.add(new EntityState(next));
    }
    this.jump_hexes = discovered;
    return result;
  }
  
  /* Helper method to return the set of enemy moves
   * filtered for small moves
   */

  int MechsMoved = 1;
  Vector CounterMoves;
  
  public Vector calculateCounterMoves() {
    if (this.moves == null) {
      moves = this.calculateRunWalkOptions();
      moves.addAll(this.calculateJumpOptions());
    } else if (this.MechsMoved == this.tb.my_mechs_moved) {
      return this.CounterMoves;
    }
    this.MechsMoved = this.tb.my_mechs_moved;
    Object[] move_array = moves.toArray();
    Object[] enemy_array = Compute.vectorToArray(game.getValidTargets(entity));
    CEntity self = this;
    
    for (int i = 0; i < move_array.length; i++) { // for each state (could some prefiltering be done?)
      EntityState option = (EntityState)move_array[i];
      option.setState();
      option.damages = new double[enemy_array.length];
      option.threats = new double[enemy_array.length];
      option.max_threats = new double[enemy_array.length];
      option.min_damages = new double[enemy_array.length];
      
      for (int e = 0; e < enemy_array.length; e++) { // for each enemy
        Entity en = (Entity)enemy_array[e];
        CEntity enemy = this.tb.enemies.get(en);
        int enemy_hit_arc = Compute.getThreatHitArc(enemy.old.curPos, enemy.old.curFacing, option.curPos);
        int self_hit_arc = Compute.getThreatHitArc(option.curPos, option.curFacing, enemy.old.curPos);
        int[] modifiers = option.getModifiers(enemy.entity);
        if (!enemy.entity.isImmobile() && modifiers[EntityState.DEFENCE_MOD] != ToHitData.IMPOSSIBLE) {
          self.engaged = true;
          int mod = modifiers[EntityState.DEFENCE_MOD];
          double max = option.getMaxModifiedDamage(enemy.old, this.tb.enemies.get(en), mod, modifiers[EntityState.DEFENCE_PC]);
          if (en.isSelectable()) { // let him turn a little
            enemy.old.curFacing = (enemy.old.curFacing+1)%6;
            max = Math.max(option.getMaxModifiedDamage(enemy.old, this.tb.enemies.get(en), mod+1, modifiers[EntityState.DEFENCE_PC]),max);
            enemy.old.curFacing = (enemy.old.curFacing+4)%6;
            max = Math.max(option.getMaxModifiedDamage(enemy.old, this.tb.enemies.get(en), mod+1, modifiers[EntityState.DEFENCE_PC]),max);
            //return to original facing
            enemy.old.curFacing = (enemy.old.curFacing+1)%6;
          }
          max = self.getThreatUtility(max, self_hit_arc);
          if (enemy.entity.isProne()) max *= .6;
          option.threats[e] = max;
          option.max_threats[e] = max;
          option.threat += max;
        }
        //I would always like to face the opponent (the most open direction)
        if (Compute.getThreatHitArc(option.curPos, option.curFacing, enemy.entity.getPosition()) != CEntity.SIDE_FRONT) {
          int fa = Compute.getFiringAngle(option.curPos, option.curFacing, enemy.entity.getPosition());
          option.movement_threat += enemy.bv/500*(1-(double)Math.abs(180 - fa)/180); //need to also account for distance?
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
                  toHit = Compute.toHitDfa(game, option.entity.getId(), option.PhysicalTarget.entity.getId(), option.curPos);
                  damage = 2*Compute.getDfaDamageFor(option.entity);
                  self_threat = option.centity.getThreatUtility(Compute.getDfaDamageTakenBy(option.entity), CEntity.SIDE_REAR)*Compute.oddsAbove(toHit.getValue())/100;
                  self_threat += option.centity.getThreatUtility(.1*self.entity.getWeight(), CEntity.SIDE_REAR)*(1 - self.base_psr_odds);
                } else {
                  self.old.setState();
                  MovementData md = option.getMovementData();
                  toHit = Compute.toHitCharge(game, option.entity.getId(), option.PhysicalTarget.entity.getId(), md);
                  damage = Compute.getChargeDamageFor(option.entity, md.getHexesMoved());
                  self_threat = option.centity.getThreatUtility(Compute.getChargeDamageTakenBy(option.entity, option.PhysicalTarget.entity), CEntity.SIDE_FRONT)*(1-Compute.oddsAbove(toHit.getValue())/100);
                  option.setState();
                }
                damage = Math.sqrt(option.PhysicalTarget.bv/self.bv)*option.PhysicalTarget.getThreatUtility(damage, toHit.getSideTable())*Compute.oddsAbove(toHit.getValue())/100;
                option.damages[e] = damage;
                option.min_damages[e] = damage;
                option.damage = damage;
                option.threat += self_threat;
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
    
    // people like to do more damage
    Arrays.sort(move_array, new Comparator() {
      public int compare(Object obj, Object obj1) {
        if (((EntityState)obj).damage - .5*((EntityState)obj).getUtility() >
        ((EntityState)obj1).damage - .5*((EntityState)obj1).getUtility()) {
          return -1;
        }
        return 1;
      }
    });
    Vector result = new Vector();
    for (int i = 0; i < move_array.length; i++) {
      result.add(move_array[i]);
    }
    this.CounterMoves = result;
    return result;
  }
  
  /* find all moves that get into that destination */
  public Vector findMoves(Coords dest) {
    if (moves == null) this.calculateCounterMoves();
    String key = dest.toString();
    Vector matches = new Vector();
    Object obj;
    if (this.jump_hexes != null)
      if ((obj = this.jump_hexes.get(key)) != null)
        matches.add(obj);
    if (this.run_walk_moves != null) {    
       for (int i = 0; i < 6; i++) {
         if ((obj = this.run_walk_moves.get(key+" "+i)) != null)
           matches.add(obj);
       }
    }
    return matches;
  }
  
  /** given my skill and the present modifiers, what is
   *  a better estimate of my damage dealing
   *   -- actual and not utility
   */
  // should base the ranges off of some internal characterization...
  // simple linearlization formulas that aren't too good at the extremes
  public double getModifiedDamage(int arc, int range, int modifier) {
    if (range > this.MAX_RANGE) return 0;
    double damage = this.damages[arc][range];
    int base = this.gunnery;
    int dist_mod = 0;
    if (range < 7) {
      dist_mod = this.MinRanges[range];
    } else if (range < 13) {
      dist_mod = 2;
    } else if (range < this.MAX_RANGE) {
      dist_mod = 4;
    } else { //will need to be changed for extended range weapons
      dist_mod = 20;
    }
    if (base + dist_mod + modifier > TestBot.Ignore) return 0;
    if (base + dist_mod + modifier == TestBot.Ignore) damage *= .5;
    return (damage/Compute.oddsAbove(base+dist_mod)*Compute.oddsAbove(dist_mod+modifier+base));
  }
  
  boolean refresh = true;
  
  static class Table extends Hashtable {
    
    public void put(CEntity es) {
      this.put(es.getKey(), es);
    }
    
    public CEntity get(Entity es) {
      CEntity result = null;
      if ((result = (CEntity)super.get(new Integer(es.getId())))==null) {
        result = new CEntity(es);
        this.put(result);
      }
      result.refresh();
      return result;
    }
    
    public CEntity remove(CEntity es) {
      return (CEntity)super.remove(es.getKey());
    }
  }
}
