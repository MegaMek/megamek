package megamek.client.bot;

import megamek.*;
import megamek.common.*;
import megamek.common.actions.*;

import com.sun.java.util.collections.*;
import java.util.Enumeration;

/** This class was created to iteratively compile
 * movement data
 */
public class EntityState extends MovementData implements com.sun.java.util.collections.Comparable {
  
  public static Game game;
  public static TestBot tb;
  
  public static final int ATTACK_MOD = 0;
  public static final int DEFENCE_MOD = 1;
  public static final int ATTACK_PC = 2;
  public static final int DEFENCE_PC = 3;
  
  CEntity centity = null;
  Entity entity = null;
  
  Vector Steps = new Vector();
  
  int mpUsed = 0;
  Coords curPos = null;
  int curFacing;
  boolean isProne;
  boolean isJumping = false;
  boolean isRunProhibited = false;
  boolean isMovementLegal = true;
  boolean isDanger = false;
  int overallMoveType = Entity.MOVE_NONE;
  boolean hasJustStood = false;
  boolean firstStep = true;
  double threat = 0;
  
  boolean inDanger = false;
  boolean Doomed = false;
  
  double self_threat = 0;
  double movement_threat = 0;
  double self_damage = 0;
  
  double damage = 0;
  
  double[] threats;
  double[] damages;
  double[] max_threats;
  double[] min_damages;
  
  int delta_distance = 0;
  int heatBuildup;
  //int defensive_mod = 0;
  //int offensive_mod = 0;
  
  CEntity PhysicalTarget;
  boolean isPhysical = false;
  
  Vector tv = new Vector();
  
  public EntityState(EntityState base) {
    super(base);
    this.Steps = new Vector(base.Steps);
    this.centity = base.centity;
    this.entity = base.entity;
    this.isProne = base.isProne;
    this.mpUsed = base.mpUsed;
    this.curPos = new Coords(base.curPos);
    this.curFacing = base.curFacing;
    this.isJumping = base.isJumping;
    this.isRunProhibited = base.isRunProhibited;
    this.isMovementLegal = base.isMovementLegal;
    this.isDanger = base.isDanger;
    this.overallMoveType = base.overallMoveType;
    this.hasJustStood = base.hasJustStood;
    this.firstStep = base.firstStep;
    this.delta_distance = base.delta_distance;
    this.heatBuildup = base.heatBuildup;
    this.threat = base.threat;
    this.damage = base.damage;
    this.movement_threat = base.movement_threat;
    this.tv = new Vector(base.tv);
    if (base.damages != null) {
      this.damages = new double[base.damages.length];
      System.arraycopy(base.damages,0,this.damages,0,base.damages.length);
    }
    if (base.threats != null) {
      this.threats = new double[base.threats.length];
      System.arraycopy(base.threats,0,this.threats,0,base.threats.length);
    }
    if (base.min_damages != null) {
      this.min_damages = new double[base.threats.length];
      System.arraycopy(base.min_damages,0,this.min_damages,0,base.threats.length);
    }
    if (base.max_threats != null) {
      this.max_threats = new double[base.max_threats.length];
      System.arraycopy(base.max_threats,0,this.max_threats,0,base.threats.length);
    }
    this.self_threat = base.self_threat;
    this.inDanger = base.inDanger;
    this.Doomed = base.Doomed;
    this.isPhysical = base.isPhysical;
    this.PhysicalTarget = base.PhysicalTarget;
    this.self_damage = base.self_damage;
  }
  public EntityState(CEntity en) {
    this.centity = en;
    this.entity = this.centity.entity;
    this.curPos = new Coords(entity.getPosition());
    this.curFacing = entity.getFacing();
    this.isProne = entity.isProne();
    this.heatBuildup = entity.heatBuildup;
  }
  public void addStep(int step_type) {
    try {
      if (firstStep && step_type == MovementData.STEP_START_JUMP) {
        this.overallMoveType = Entity.MOVE_JUMP;
        this.isJumping = true;
      }
      //check to make sure we're not jumping after the first move.
      if (!firstStep && step_type == MovementData.STEP_START_JUMP) {
        this.overallMoveType = Entity.MOVE_ILLEGAL;
      }
      super.addStep(step_type);
      Steps.add(new Integer(step_type));
      int entityId = entity.getId();
      Coords lastPos = new Coords(curPos);
      int stepMp = 0;
      //calculate mps used
      switch(step_type) {
        case MovementData.STEP_TURN_LEFT :
        case MovementData.STEP_TURN_RIGHT :
          stepMp = (isJumping || hasJustStood) ? 0 : 1;
          curFacing = MovementData.getAdjustedFacing(curFacing,step_type);
          break;
        case MovementData.STEP_BACKWARDS :
          isRunProhibited = true;
        case MovementData.STEP_FORWARDS :
        case MovementData.STEP_CHARGE :
        case MovementData.STEP_DFA :
          // step forwards or backwards
          if (step_type == MovementData.STEP_BACKWARDS) {
            curPos = curPos.translated((curFacing + 3) % 6);
          } else {
            curPos = curPos.translated(curFacing);
          }
          if (game.board.getHex(curPos) == null) {
            this.overallMoveType = Entity.MOVE_ILLEGAL;
            return;
          }
          stepMp = Compute.getMovementCostFor(game, this.entity.getId(), lastPos, curPos,
          overallMoveType);
          // check for water
          if (game.board.getHex(curPos).contains(Terrain.WATER)) {
            isRunProhibited = true;
          }
          //should check for heatBuildup related to movement
          hasJustStood = false;
          break;
        case MovementData.STEP_GET_UP :
          // mechs with 1 MP are allowed to get up
          stepMp = entity.getWalkMP() == 1 ? 1 : 2;
          heatBuildup += 1;
          hasJustStood = true;
          isProne = false;
          break;
        default :
          stepMp = 0;
      }
      isDanger = false;
      boolean runningDanger = false;
      mpUsed += stepMp;
      // check for running
      if (overallMoveType == Entity.MOVE_WALK && mpUsed > entity.getWalkMP()) {
        overallMoveType = Entity.MOVE_RUN;
        // running with gyro or hip hit is dangerous!
        if (!isRunProhibited && (entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM,
        Mech.SYSTEM_GYRO,Mech.LOC_CT) > 0
        || entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM,
        Mech.ACTUATOR_HIP, Mech.LOC_RLEG) > 0
        || entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM,
        Mech.ACTUATOR_HIP, Mech.LOC_LLEG) > 0)) {
          isDanger = true;
          runningDanger = true;
        }
      } else if (overallMoveType != Entity.MOVE_JUMP && overallMoveType != Entity.MOVE_ILLEGAL) {
        overallMoveType = Entity.MOVE_WALK; //walk before you run
      }
      // second pass: set moveType, illegal, trouble flags
      // guilty until proven innocent
      int moveType = Entity.MOVE_ILLEGAL;
      // check for valid jump mp
      if (overallMoveType == Entity.MOVE_JUMP
      && mpUsed <= entity.getJumpMP() && !isProne) {
        moveType = Entity.MOVE_JUMP;
      }
      // check for valid walk/run mp
      if ((overallMoveType == Entity.MOVE_WALK || overallMoveType == Entity.MOVE_RUN)
      && (!isProne || step_type == MovementData.STEP_TURN_LEFT
      || step_type == MovementData.STEP_TURN_RIGHT)) {
        if (mpUsed <= entity.getWalkMP()) {
          moveType = Entity.MOVE_WALK;
        } else if (mpUsed <= entity.getRunMP() && !isRunProhibited) {
          moveType = Entity.MOVE_RUN;
        }
      }
      // mechs with 1 MP are allowed to get up
      if (step_type == MovementData.STEP_GET_UP && entity.getWalkMP() == 1) {
        moveType = Entity.MOVE_RUN;
      }
      // amnesty for the first step
      if (firstStep && moveType == Entity.MOVE_ILLEGAL && entity.getWalkMP() > 0 && !entity.isProne() && step_type == MovementData.STEP_FORWARDS) {
        moveType = Entity.MOVE_RUN;
      }
      // check if this movement is illegal for reasons other than points
      if (!this.isMovementPossible(game, entity.getId(), lastPos, curPos, moveType, step_type)) {
        moveType = Entity.MOVE_ILLEGAL;
      }
      // no legal moves past an illegal one
      if (moveType == Entity.MOVE_ILLEGAL || overallMoveType == Entity.MOVE_ILLEGAL) {
        isMovementLegal = false;
      }
      // check for danger
      isDanger = (Compute.isPilotingSkillNeeded(game, entityId, lastPos, curPos, moveType) || step_type == MovementData.STEP_GET_UP);
      
      //this should be a more exact calculation, but for now it just serves as a deterent
      //for example it should actaully be a proportion of the threat if you end up stuck in
      //this state
      if (isDanger) {
        double mod = 1;
        if (centity.base_psr_odds < .1) {
          moveType = Entity.MOVE_ILLEGAL;
        } else if (runningDanger) {
          if (centity.base_psr_odds < .5) {
            moveType = Entity.MOVE_ILLEGAL; 
          } else {
            mod = 5;
          }
        } else {
          double threat = mod*this.centity.getThreatUtility(.2*this.entity.getWeight(),CEntity.SIDE_REAR)*(1-Math.pow(this.centity.base_psr_odds, 2));
          this.movement_threat += threat;
          this.tv.add(threat +" Movement Threat \n");
        }
      }
      this.overallMoveType = isMovementLegal ? moveType : Entity.MOVE_ILLEGAL;
      firstStep = false;
      delta_distance = curPos.distance(entity.getPosition());
    } catch (Exception e) {
      this.overallMoveType = Entity.MOVE_ILLEGAL;
    }
  }
  
  public int getMovementheatBuildup() {
    if (overallMoveType == Entity.MOVE_NONE)
      return 0;
    if (overallMoveType == Entity.MOVE_WALK)
      return 1;
    if (overallMoveType == Entity.MOVE_RUN)
      return 2;
    if (overallMoveType == Entity.MOVE_JUMP)
      return Math.max(3,mpUsed);
    return 1000; // illegal?
  }
  
  public int getheatBuildup() {
    return (this.getMovementheatBuildup() + this.heatBuildup);
  }
  
  //should combine the next two functions
  public boolean changeToPhysical() {
    boolean found = false;
    //this caused a big error... when unchecked
    if (this.getLastStep() != null) {
      if (this.getLastStep().getType() == STEP_BACKWARDS) return false;
    }
    if (this.overallMoveType != Entity.MOVE_ILLEGAL) {
      for (Enumeration i = game.getEntities(); i.hasMoreElements() && !found;) {
        final Entity en = (Entity)i.nextElement();
        CEntity cen = TestBot.enemies.get(en);
        if (!en.isSelectable() && this.curPos.equals(en.getPosition()) && en.isEnemyOf(this.entity)) {
          found = true;
          this.PhysicalTarget = cen;
        }
      }
    }
    if (found) {
      this.isPhysical = true;
      this.Steps.remove(Steps.size() -1);
      if (this.overallMoveType == Entity.MOVE_JUMP) {
        this.Steps.add(new Integer(STEP_DFA));
      } else {
        this.Steps.add(new Integer(STEP_CHARGE));
      }
      //move back to distance one
      this.curPos = this.curPos.translated((this.curFacing + 3)%6);
      return true;
    }
    return false;
  }
  
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (Enumeration i =Steps.elements();i.hasMoreElements();) {
      sb.append(new Step(((Integer)i.nextElement()).intValue()));
      sb.append(' ');
    }
    return sb.toString();
  }
  
  /**
   *  Also sets the isMovementLegal flag
   */
  public boolean isMoveLegal() {
    // third pass (sigh) : avoid stacking violations
    this.isMovementLegal = false;
    //seems to have been a change, can't stack and do dfa/charge
    /*if (this.getLastStep() != null) {
      if (this.getLastStep().getType() == STEP_DFA || this.getLastStep().getType() == STEP_CHARGE)
        return true;
    }*/
    if (this.overallMoveType != Entity.MOVE_ILLEGAL) {
      boolean found = false;
      for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
        final Entity en = (Entity)i.nextElement();
        CEntity cen = TestBot.enemies.get(en);
        if (this.curPos.equals(cen.old.curPos) && (en.getId() != this.entity.getId())) {
          found = true;
        }
      }
      if (!found) {
        this.isMovementLegal = true;
      }
    }
    return this.isMovementLegal;
  }
  
  public boolean isStepLegal() {
    if (game.board.getHex(this.curPos) == null) return false;
    if (this.overallMoveType != Entity.MOVE_JUMP
    && game.getEntity(this.curPos) != null
    && this.entity.isEnemyOf(game.getEntity(this.curPos))) {
      return false;
    }
    return (this.overallMoveType != Entity.MOVE_ILLEGAL);
  }
  
  public Step getLastStep() {
    int l = Steps.size();
    if (l <= 0) return null;
    return new Step(((Integer)Steps.elementAt(l-1)).intValue());
  }
  
  public String getKey() {
    if (this.isJumping) {
      return curPos.toString();
    }
    return new String(this.curPos.toString()+ " " +this.curFacing);
  }
  
  public void setState() {
    this.entity = this.centity.entity;
    this.entity.setPosition(curPos);
    this.entity.setFacing(curFacing);
    this.entity.setSecondaryFacing(curFacing);
    this.entity.moved = this.overallMoveType;
    this.entity.heatBuildup = this.getheatBuildup();
    this.entity.setProne(this.isProne);
    this.entity.delta_distance = this.delta_distance;
  }
  
  /** sets a pair of integer objects as to the attack and defencive modifies
   *  assumes that set state has been called
   */
  public int[] getModifiers(final Entity te) {
    //set them at the appropriate positions
    final Entity ae = this.entity;
    
    int attHeight = ae.isProne() ? 0 : 1;
    int targHeight = te.isProne() ? 0 : 1;
    int attEl = 0;
    int targEl = 0;
    try {
      attEl = ae.elevation() + attHeight;
      targEl = te.elevation() + targHeight;
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(this);
      System.out.println(ae.getName() + " " + ae.getPosition());
      System.out.println(te.getName() + " " + te.getPosition());
      System.out.println(this.tb.enemies.get(ae).old.curPos);
    }
    int ilw = 0;  // intervening light woods
    int ihw = 0;  // intervening heavy woods
    Coords in[] = Compute.intervening(this.curPos, te.getPosition());
    
    boolean pc = false;
    boolean apc = false;
    
    //get all relevent modifiers
    ToHitData toHita = new ToHitData();
    ToHitData toHitd = new ToHitData();
    
    toHita.append(Compute.getAttackerMovementModifier(game, ae.getId()));
    //this.offensive_mod = Compute.getAttackerMovementModifier(game, ae.getId()).getValue();
    toHita.append(Compute.getTargetMovementModifier(game, te.getId()));
    toHita.append(Compute.getTargetTerrainModifier(game, te.getId()));
    toHita.append(Compute.getAttackerTerrainModifier(game, ae.getId()));
    
    toHitd.append(Compute.getAttackerMovementModifier(game, te.getId()));
    toHitd.append(Compute.getTargetMovementModifier(game, ae.getId()));
    //this.defensive_mod = Compute.getTargetMovementModifier(game, ae.getId()).getValue();
    if (!this.isPhysical) {
      toHitd.append(Compute.getTargetTerrainModifier(game, ae.getId()));
      //this.defensive_mod += Compute.getTargetTerrainModifier(game, ae.getId()).getValue();
    }
    toHitd.append(Compute.getAttackerTerrainModifier(game, te.getId()));
    
    Hex attHex = game.board.getHex(ae.getPosition());
    if (attHex.contains(Terrain.WATER) && attHex.surface() > attEl) {
      toHita.addModifier(ToHitData.IMPOSSIBLE, "Attacker in depth 2+ water");
      toHitd.addModifier(ToHitData.IMPOSSIBLE, "Defender in depth 2+ water");
      //this.offensive_mod += ToHitData.IMPOSSIBLE;
      //this.defensive_mod += ToHitData.IMPOSSIBLE;
    } else if (attHex.surface() == attEl && ae.height() > 0) {
      apc = true;
    }
    Hex targHex = game.board.getHex(te.getPosition());
    if (targHex.contains(Terrain.WATER)) {
      if (targHex.surface() == targEl && te.height() > 0) {
        pc = true;
      } else if (targHex.surface() > targEl) {
        toHita.addModifier(ToHitData.IMPOSSIBLE, "Attacker in depth 2+ water");
        toHitd.addModifier(ToHitData.IMPOSSIBLE, "Defender in depth 2+ water");
        //this.offensive_mod += ToHitData.IMPOSSIBLE;
        //this.defensive_mod += ToHitData.IMPOSSIBLE;
      }
    }
    
    for (int i = 0; i < in.length; i++) {
      // don't count attacker or target hexes
      if (in[i].equals(this.curPos) || in[i].equals(te.getPosition())) {
        continue;
      }
      final Hex h = game.board.getHex(in[i]);
      if (h == null) continue;
      final int hexEl = h.getElevation();
      // check for block by terrain
      if ((hexEl > attEl && hexEl > targEl)
      || (hexEl > attEl && this.curPos.distance(in[i]) <= 1)
      || (hexEl > targEl && te.getPosition().distance(in[i]) <= 1)) {
        toHita.addModifier(ToHitData.IMPOSSIBLE, "Terrain");
        toHitd.addModifier(ToHitData.IMPOSSIBLE, "Terrain");
      }
      // determine number of woods hexes in the way
      if (h.levelOf(Terrain.WOODS) > 0) {
        if ((hexEl + 2 > attEl && hexEl + 2 > targEl)
        || (hexEl + 2 > attEl && ae.getPosition().distance(in[i]) <= 1)
        || (hexEl + 2 > targEl && te.getPosition().distance(in[i]) <= 1)) {
          ilw += (h.levelOf(Terrain.WOODS) == 1 ? 1 : 0);
          ihw += (h.levelOf(Terrain.WOODS) > 1 ? 1 : 0);
        }
      }
      // check for partial cover
      if (te.getPosition().distance(in[i]) <= 1 && hexEl == targEl && attEl <= targEl && targHeight > 0) {
        pc = true;
      }
      // check for attacker partial cover
      if (this.curPos.distance(in[i]) <= 1 && hexEl == attEl && attEl >= targEl && attHeight > 0) {
        apc = true;
      }
    }
    // more than 1 heavy woods or more than two light woods block LOS
    if (ilw + ihw * 2 >= 3) {
      toHita.addModifier(ToHitData.IMPOSSIBLE, "Terrain");
      toHitd.addModifier(ToHitData.IMPOSSIBLE, "Terrain");
    }
    // partial cover
    if (pc) {
      toHita.addModifier(3, "target has partial cover");
    }
    if (apc) {
      toHitd.addModifier(3, "attacker has partial cover");
    }
    // intervening terrain
    if (ilw > 0) {
      toHita.addModifier(ilw, ilw + " light woods intervening");
      toHitd.addModifier(ilw, ilw + " light woods intervening");
    }
    if (ihw > 0) {
      toHita.addModifier(ihw * 2, ihw + " heavy woods intervening");
      toHitd.addModifier(ihw * 2, ihw + " heavy woods intervening");
    }
    // heatBuildup
    if (ae.getHeatFiringModifier() != 0) {
      toHita.addModifier(ae.getHeatFiringModifier(), "heatBuildup");
      //this.offensive_mod += ae.getHeatFiringModifier();
    }
    if (te.getHeatFiringModifier() != 0) {
      toHitd.addModifier(te.getHeatFiringModifier(), "heatBuildup");
    }
    // target immobile
    if (te.isImmobile()) {
      toHita.addModifier(-4, "target immobile");
    }
    if (ae.isImmobile()) {
      toHitd.addModifier(-4, "target immobile");
      //this.defensive_mod -= 4;
    }
    final int range = ae.getPosition().distance(te.getPosition());
    // target prone
    if (te.isProne()) {
      // easier when point-blank
      if (range == 1) {
        toHita.addModifier(-2, "target prone and adjacent");
      }
      // harder at range
      if (range > 1) {
        toHita.addModifier(1, "target prone and at range");
      }
    }
    if (ae.isProne()) {
      // easier when point-blank
      if (range == 1) {
        toHitd.addModifier(-2, "target prone and adjacent");
      }
      // harder at range
      if (range > 1) {
        toHitd.addModifier(1, "target prone and at range");
      }
    }
    return new int[] {toHita.getValue(), toHitd.getValue(), apc?1:0, pc?1:0};
  }
  
  public MovementData getMovementData() {
    MovementData transmit = new MovementData();
    for (int j = 0; j < Steps.size(); j++) {
      transmit.addStep(((Integer)Steps.elementAt(j)).intValue());
    }
    return transmit;
  }
  
  public double getUtility() {
    //self threat and self damage are considered transient
    double temp_threat = (this.threat + this.movement_threat + this.self_threat + (double)this.getMovementheatBuildup()/20)/this.centity.strategy.attack;
    double temp_damage = (this.damage + this.self_damage)*this.centity.strategy.attack;
    double ratio = (this.threat + this.movement_threat)/(this.centity.avg_armor + .25*this.centity.avg_iarmor);
    if (this.threat + this.movement_threat > 4*this.centity.avg_armor) {
      if (ratio > 2) {
        temp_threat += this.centity.bv/15.0; //likely to die
        this.Doomed = true;
        this.inDanger = true;
      } else if (ratio > 1) {
        temp_threat += this.centity.bv/25.0; //in danger
        this.inDanger = true;
      } else {
        temp_threat += this.centity.bv/70.0; //in danger
        this.inDanger = true;        
      }
    } else if (this.threat + this.movement_threat > 30) {
      temp_threat += this.centity.entity.getWeight();
    }
    return (temp_threat - temp_damage);
  }
  
  /** Assumes the safest moves for now
   */
  public int compareTo(Object obj) {
    if (this.getUtility() < ((EntityState)obj).getUtility())
      return -1;
    if (this.getUtility() == ((EntityState)obj).getUtility())
      if (this.damage < ((EntityState)obj).damage)
        return -1;
    return 1;
  }
  
  /** get maximum damage in this current state from enemy
   *  accounting for torso twisting and slightly for heat
   *   -- the ce passed in is supposed to be the enemy mech
   */
  public double getMaxModifiedDamage(EntityState enemy, CEntity ce, int modifier, int apc) {
    double max = 0;
    int distance = curPos.distance(enemy.curPos);
    double mod = 1;
    // heat effect modifiers
    if (enemy.isJumping || (enemy.entity.heat + enemy.entity.heatBuildup > 4)) {
      if (ce.overheat == CEntity.OVERHEAT_LOW) {
        mod = .75;
      } else if (ce.overheat == CEntity.OVERHEAT_HIGH) {
        mod = .5;
      } else {
        mod = .9;
      }
    }
    int self_hit_arc = Compute.getThreatHitArc(this.curPos, this.curFacing, enemy.curPos);
    int enemy_firing_arcs[] = new int[3];
    for (int i = 0; i < 3; i++) {
      enemy_firing_arcs[i] = Compute.getThreatHitArc(enemy.curPos, Compute.getAdjustedFacing(enemy.curFacing, i), this.curPos);
    }
    max = ce.getModifiedDamage((apc==1)?CEntity.TT:enemy_firing_arcs[0], distance, modifier);
    
    if (enemy_firing_arcs[1] == CEntity.SIDE_FRONT) {
      max = Math.max(max, ce.getModifiedDamage(CEntity.TT, distance, modifier));
    } else {
      max = Math.max(max, ce.getModifiedDamage(enemy_firing_arcs[1], distance, modifier));
    }
    if (enemy_firing_arcs[2] == CEntity.SIDE_FRONT) {
      max = Math.max(max, ce.getModifiedDamage(CEntity.TT, distance, modifier));
    } else {
      max = Math.max(max, ce.getModifiedDamage(enemy_firing_arcs[2], distance, modifier));
    }
    //this is not quit right, but good enough for now...
    //ideally the pa charaterization should be in centity
    max*=mod;
    if (!enemy.isProne && distance == 1 && enemy_firing_arcs[0] != CEntity.SIDE_REAR) {
      Hex h = game.board.getHex(this.curPos);
      Hex h1 = game.board.getHex(enemy.curPos);
      if (Math.abs(h.getElevation() - h1.getElevation()) < 2) {
        max += ((h1.getElevation() - h.getElevation() == 1 || this.isProne)?2:1)*((enemy_firing_arcs[0]==CEntity.SIDE_FRONT)?.2:.05)*ce.entity.getWeight()*Compute.oddsAbove(3+modifier)/100
            + (1 - enemy.centity.base_psr_odds)*enemy.entity.getWeight()/10.0;
      }
    }
    return max;
  }
  
  public static boolean isMovementPossible(Game game, int entityId,
  Coords src, Coords dest,
  int entityMoveType,
  int stepType) {
    final Entity entity = game.getEntity(entityId);
    final Hex srcHex = game.board.getHex(src);
    final Hex destHex = game.board.getHex(dest);
    
    // arguments valid?
    if (entity == null) {
      throw new IllegalArgumentException("Entity invalid.");
    }
    if (game.board.getHex(dest) == null) {
      return false;
    }
    if (game.board.getHex(src) == null) {
      return false;
    }
    if (src.distance(dest) > 1) {
      throw new IllegalArgumentException("Coordinates must be adjacent.");
    }
    
    if (entityMoveType == Entity.MOVE_ILLEGAL) {
      // that was easy
      return false;
    }
    // another easy check
    if (!game.board.contains(dest)) {
      return false;
    }
    // check elevation difference > 2
    if (entityMoveType != Entity.MOVE_JUMP
    && Math.abs(srcHex.floor() - destHex.getElevation()) > 2) {
      return false;
    }
    // units moving backwards may not change elevation levels (I think this rule's dumb)
    if (stepType == MovementData.STEP_BACKWARDS
    && srcHex.floor() != destHex.floor()) {
      return false;
    }
    // can't run into water
    if (entityMoveType == Entity.MOVE_RUN
    && destHex.levelOf(Terrain.WATER) > 0) {
      return false;
    }
    // can't jump out of water
    if (entityMoveType == Entity.MOVE_JUMP
    && entity.getPosition().equals(src)
    && srcHex.levelOf(Terrain.WATER) > 0) {
      return false;
    }
    // can't jump over too-high terrain
    if (entityMoveType == Entity.MOVE_JUMP
    && destHex.getElevation()
    > (entity.elevation() +
    entity.getJumpMP())) {
      return false;
    }
    
    return true;
  }
  
  
  static class Table extends Hashtable {
    
    public void put(EntityState es) {
      this.put(es.getKey(), es);
    }
    
    public EntityState get(EntityState es) {
      return (EntityState)super.get(es.getKey());
    }
    
    public EntityState remove(EntityState es) {
      return (EntityState)super.remove(es.getKey());
    }
    
    /** Extracts the "best" shortest move found so far */
    public EntityState extractMin() {
      Iterator states = values().iterator();
      EntityState min = (EntityState)states.next();
      EntityState next = null;
      while (states.hasNext()) {
        next = (EntityState)states.next();
        if (next.mpUsed + next.movement_threat*100/next.centity.bv < min.mpUsed + min.movement_threat*100/min.centity.bv) {
          min = next;
        }
      }
      return remove(min);
    }
    
    public void update(EntityState next) {
      remove(next);
      put(next);
    }
  }
}