package megamek.client.bot;

import megamek.client.bot.ga.*;
import megamek.common.*;
import megamek.common.actions.*;

import com.sun.java.util.collections.*;

/**
 *  Need to test the function that moves all firing to a single target
 *
 *
 **/
public class GAAttack extends GA {
  
  protected Vector attack;
  protected CEntity attacker;
  protected Game game;
  protected CEntity.Table targets = new CEntity.Table();
  protected Object[] target_array = null;
  protected Vector valid_target_indexes = null;
  protected boolean overheat_eligible = false;
  
  public GAAttack(Game game, CEntity attacker, Vector attack, int population, int generations, boolean isEnemy) throws GAException {
    super(attack.size()+1, population, .7, 5, generations, 0, 0, .4,
    Crossover.ctUniform, false, false);
    this.attack = attack;
    this.attacker = attacker;
    this.game = game;
    this.target_array = Compute.vectorToArray(game.getEntitiesVector());
    Vector temp = new Vector();
    for (int i = 0; i < target_array.length; i++) {
      Entity entity = (Entity)target_array[i];
      if (entity.isEnemyOf(attacker.entity)) {
        temp.add(new Integer(i));
      }
    }
    this.valid_target_indexes = temp;
    this.initPopulation();
    if (isEnemy || (attacker.last != null && (!attacker.last.inDanger || attacker.last.Doomed))) {
      this.overheat_eligible = true;
    }
  }
  
  public int[] getResultChromosome() {
    return(((ChromVector)chromosomes[bestFitnessChromIndex]).genes);
  }
  
  double[] damages = null;
  
  public double getDamageUtility(CEntity to) {
    if (damages == null) damages = this.getDamageUtilities();
    for (int k = 0; k < this.target_array.length; k++) {
      Entity enemy = (Entity)this.target_array[k];
      if (enemy.getId() == to.entity.getId()) {
        return damages[k];
      }
    }
    return 0;
  }
  
  public double[] getDamageUtilities() {
    return this.getDamageUtilities(this.bestFitnessChromIndex);
  }
  
  public double[] getDamageUtilities(int iChromIndex) {
    targets.clear(); //could use vector and not hashtable
    double[] result = new double[this.target_array.length];
    ChromVector chromVector = (ChromVector)this.chromosomes[iChromIndex];
    int heat_total = 0;
    Iterator i = attack.iterator();
    if (chromVector.genes[chromosomeDim - 1] >= this.target_array.length) {
      chromVector.genes[chromosomeDim - 1] = ((Integer)this.valid_target_indexes.elementAt(0)).intValue();
    }
    Entity target = (Entity)this.target_array[chromVector.genes[chromosomeDim - 1]];
    boolean fired = false;
    for (int iGene=0; iGene < chromosomeDim - 1; iGene++) {
      TestBot.AttackOption a = (TestBot.AttackOption)(((Vector)(attack.elementAt(iGene))).elementAt(chromVector.genes[iGene]));
      if (a.target != null) { //if not the no fire option
        fired = true;
        targets.put(a.target);
        double mod = 1;
        if (a.target.entity.getId() == target.getId()) {
          a.target.possible_damage[a.toHit.getSideTable()] += mod*a.primary_expected;
        } else {
          a.target.possible_damage[a.toHit.getSideTable()] += mod*a.expected;
        }
        heat_total += a.heat;
      }
    }
    
    for (int k = 0; k < this.target_array.length; k++) {
      double total_utility = 0;
      Entity en = (Entity)this.target_array[k];
      CEntity enemy = null;
      if ((enemy = (CEntity)this.targets.get(new Integer(en.getId()))) != null) {
        if(enemy.possible_damage[ToHitData.SIDE_FRONT] > 0) {
          total_utility += enemy.getThreatUtility(enemy.possible_damage[ToHitData.SIDE_FRONT], CEntity.SIDE_FRONT);
        } else if(enemy.possible_damage[ToHitData.SIDE_REAR] > 0) {
          total_utility += enemy.getThreatUtility(enemy.possible_damage[ToHitData.SIDE_REAR], CEntity.SIDE_REAR);
        } else if(enemy.possible_damage[ToHitData.SIDE_LEFT] > 0) {
          total_utility += enemy.getThreatUtility(enemy.possible_damage[ToHitData.SIDE_LEFT], CEntity.SIDE_LEFT);
        } else if(enemy.possible_damage[ToHitData.SIDE_RIGHT] > 0) {
          total_utility += enemy.getThreatUtility(enemy.possible_damage[ToHitData.SIDE_RIGHT], CEntity.SIDE_RIGHT);
        }
        enemy.resetPossibleDamage();
      }
      result[k] = total_utility;
    }
    return result;
  }
  
  protected double getFitness(int iChromIndex) {
    return this.getFitness((ChromVector)this.chromosomes[iChromIndex]);
  }
  
  protected double getFitness(ChromVector chromVector) {
    targets.clear(); //could use vector and not hashtable
    int heat_total = 0;
    Iterator i = attack.iterator();
    Entity target = null;
    try {
      target = (Entity)this.target_array[chromVector.genes[chromosomeDim - 1]];
    } catch (Exception e) {
      System.out.println(chromosomeDim + " " + chromVector.genes.length);
      System.out.println(this.target_array.length);
      target = (Entity)this.target_array[((Integer)this.valid_target_indexes.get(0)).intValue()]; 
    }
    boolean hasPrimary = false;
    boolean fired = false;
    for (int iGene=0; iGene < chromosomeDim - 1; iGene++) {
      final int[] genes = chromVector.genes;
      TestBot.AttackOption a = (TestBot.AttackOption)(((Vector)(attack.elementAt(iGene))).elementAt(genes[iGene]));
      if (a.target != null) { //if not the no fire option
        fired = true;
        targets.put(a.target);
        double mod = 1;
        if (a.ammoLeft != -1) {
          if (attacker.overall_armor_percent < .5) {
            mod = 1.5; //get rid of it
          } else if (a.ammoLeft < 12 && attacker.overall_armor_percent > .75) {
            if (a.primary_odds < .1) {
              mod = 0;
            } else if (a.ammoLeft < 6 && a.primary_odds < .25) {
              mod = 0;
            } else {
              mod = a.primary_odds; //low percentage shots will be frowned upon
            }
          }
        }
        if (a.target.entity.getId() == target.getId()) {
          hasPrimary = true;
          a.target.possible_damage[a.toHit.getSideTable()] += mod*a.primary_expected;
        } else {
          a.target.possible_damage[a.toHit.getSideTable()] += mod*a.expected;
        }
        heat_total += a.heat;
      }
    }
    double total_utility = 0;
    com.sun.java.util.collections.Iterator j = targets.values().iterator();
    while (j.hasNext()) {
      CEntity enemy = (CEntity)j.next();
      if(enemy.possible_damage[ToHitData.SIDE_FRONT] > 0) {
        total_utility += enemy.getThreatUtility(enemy.possible_damage[ToHitData.SIDE_FRONT], CEntity.SIDE_FRONT);
      } else if(enemy.possible_damage[ToHitData.SIDE_REAR] > 0) {
        total_utility += enemy.getThreatUtility(enemy.possible_damage[ToHitData.SIDE_REAR], CEntity.SIDE_REAR);
      } else if(enemy.possible_damage[ToHitData.SIDE_LEFT] > 0) {
        total_utility += enemy.getThreatUtility(enemy.possible_damage[ToHitData.SIDE_LEFT], CEntity.SIDE_LEFT);
      } else if(enemy.possible_damage[ToHitData.SIDE_RIGHT] > 0) {
        total_utility += enemy.getThreatUtility(enemy.possible_damage[ToHitData.SIDE_RIGHT], CEntity.SIDE_RIGHT);
      }
      enemy.resetPossibleDamage();
    }
    //should be moved
    int capacity = attacker.entity.getHeatCapacityWithWater();
    int currentHeat = attacker.entity.heatBuildup + attacker.entity.heat;
    int overheat = currentHeat + heat_total - capacity;
    //need to determine the situation -- readlining will help in certian situations
    //bonus for cooling down
    if (attacker.entity.heat > 0 && overheat < 0) {
      //always perfer smaller heat numbers
      total_utility -= attacker.bv/1000*overheat;
      //but add clear deliniations at the breaks
      if (attacker.entity.heat > 4) {
        total_utility *= 1.2;
      }
      if (attacker.entity.heat > 7) {
        total_utility += attacker.bv/50;
      }
      if (attacker.entity.heat > 12) {
        total_utility += attacker.bv/20;
      }
      if (attacker.entity.heat > 16) {
        total_utility += attacker.bv/10;
      }
    } else if (overheat > 0) {
      if (overheat > 4) {
        total_utility *= (this.overheat_eligible && attacker.jumpMP > 2)?.9:.85;
      }
      if (overheat > 7) {
        double mod = this.overheat_eligible?+((attacker.jumpMP > 2)?0:10):40;
        if (this.attacker.overheat > CEntity.OVERHEAT_LOW) {
          total_utility -= attacker.bv/mod;        
        } else {
          total_utility -= attacker.bv/(mod + 10);          
        }
      }
      if (overheat > 12) {
        total_utility -= attacker.bv/(this.overheat_eligible?45:30);
      }
      if (overheat > 16) {
        //only if I am going to die?
        total_utility -= attacker.bv/5;
      }
      total_utility -= overheat/100; //small preference for less overheat opposed to more
    }
    return total_utility;
  }
  
  /** since the low fitness members have the least chance of getting selected,
   *  but the highest chance of mutation, this is where we use the primary
   *  target heuristic to drive convergence
   */
  protected void doRandomMutation(int iChromIndex) {
    ChromVector c1 = (ChromVector)this.chromosomes[iChromIndex];
    int r1 = Compute.random.nextInt(c1.genes.length - 1);
    CEntity target = null;
    boolean done = false;
    if (r1%2 == 1) {
      c1.genes[r1]--;
      if (c1.genes[r1] < 0) c1.genes[r1] = ((Vector)this.attack.elementAt(r1)).size() - 1;
      return;
    }
    //else try to move all to one target
    for (int i = 0; (i < c1.genes.length - 1) && !done; i++) {
      int iGene = (i + r1)%(c1.genes.length - 1);
      TestBot.AttackOption a = (TestBot.AttackOption)((Vector)(attack.elementAt(iGene))).elementAt(c1.genes[iGene]);
      if (a.target != null) {
        target = a.target;
        done = true;
      }
    }
    if (target == null) { //then not shooting, so shoot something
      c1.genes[r1] = Compute.random.nextInt(((Vector)(attack.elementAt(r1))).size() - 1);
      TestBot.AttackOption a = (TestBot.AttackOption)((Vector)(attack.elementAt(r1))).elementAt(c1.genes[r1]);
      if (a.target != null) {
        c1.genes[c1.genes.length - 1] = a.target.enemy_num;
      }
    } else { //let's switch as many attacks as we can to this guy
      for (int i = 0; (i < c1.genes.length - 1); i++) {
        Object[] weapon = ((Vector)(attack.elementAt(i))).toArray();
        if (c1.genes[i] != weapon.length - 1) {
          done = false;
          for (int w = 0; (w < weapon.length - 1) && !done; w++) {
            TestBot.AttackOption a = (TestBot.AttackOption)weapon[w];
            if (a.target.enemy_num == target.enemy_num) {
              c1.genes[i] = w;
              done = true;
            }
          }
        }
      }
      ((ChromVector)this.chromosomes[0]).genes[chromosomeDim -1] = target.enemy_num;
    }
  }
  
  protected void doUniformCrossover(Chromosome Chrom1, Chromosome Chrom2) {
    int gene = 0;
    ChromVector c1 = (ChromVector)Chrom1;
    ChromVector c2 = (ChromVector)Chrom2;
    for (int iGene=0; iGene < chromosomeDim; iGene++) {
      if (Compute.random.nextInt(2) == 1) {
        gene = c1.genes[iGene];
        c1.genes[iGene] = c2.genes[iGene];
        c2.genes[iGene] = gene;
      }
    }
  }
  
  protected void initPopulation() {
    //promote max
    for (int iGene=0; iGene < chromosomeDim - 1; iGene++) {
      ((ChromVector)this.chromosomes[0]).genes[iGene] = 0;
    }
 
    //use first weapon target as primary, not smart but good enough...
    TestBot.AttackOption a = (TestBot.AttackOption)((Vector)(attack.elementAt(0))).elementAt(0);
    ((ChromVector)this.chromosomes[0]).genes[chromosomeDim -1] = a.target.enemy_num;
    
    for (int i=1; i < populationDim; i++) {
      ChromVector cv = (ChromVector)this.chromosomes[i];
      for (int iGene=0; iGene < chromosomeDim - 1; iGene++) {
        cv.genes[iGene] = Compute.random.nextInt(((Vector)(attack.elementAt(iGene))).size());
        if (i <= this.attack.size()) {
          if (iGene + 1 == i) cv.genes[iGene] = 0; //fire
          else cv.genes[iGene] = ((Vector)(attack.elementAt(iGene))).size() - 1; //don't fire
        } 
      }
      cv.genes[chromosomeDim -1] =
      ((Integer)this.valid_target_indexes.elementAt(Compute.random.nextInt(this.valid_target_indexes.size()))).intValue();
      this.chromosomes[i].fitness = getFitness(i);
    }
  }
  
  protected Chromosome getNewChrom(int chromDim) {
    return new ChromVector(chromDim);
  }
}

