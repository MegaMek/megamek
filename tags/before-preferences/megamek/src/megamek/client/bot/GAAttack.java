/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import megamek.client.bot.ga.Chromosome;
import megamek.client.bot.ga.GA;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.ToHitData;

import com.sun.java.util.collections.Vector;
;

/**
 * Need to test the function that moves all firing to a single target
 */
public class GAAttack extends GA {

    protected Vector attack;
    protected CEntity attacker;
    protected Game game;
    protected CEntity.Table targets;
    protected java.util.Vector target_array = null;
    protected Vector valid_target_indexes = null;
    protected boolean overheat_eligible = false;
    protected int firing_arc = 0;
    double[] damages = null;

    public GAAttack(TestBot tb, CEntity attacker, Vector attack, int population, int generations, boolean isEnemy) {
        super(attack.size() + 1, population, .7, .05, generations, .4);
        this.attack = attack;
        this.attacker = attacker;
        this.game = tb.game;
        this.target_array = game.getEntitiesVector();
        Vector temp = new Vector();
        for (int i = 0; i < target_array.size(); i++) {
            Entity entity = (Entity) target_array.elementAt(i);
            if (entity.isEnemyOf(attacker.entity) && entity.isDeployed()) {
                temp.add(new Integer(i));
            }
        }
        targets = new CEntity.Table(tb);
        this.valid_target_indexes = temp;
        if (isEnemy || (attacker.last != null && (!attacker.last.inDanger || attacker.last.doomed))) {
            this.overheat_eligible = true;
        }
    }

    public int[] getResultChromosome() {
        return ((chromosomes[populationDim - 1]).genes);
    }
    
    public double getDamageUtility(CEntity to) {
        if (damages == null)
            damages = this.getDamageUtilities();
        for (int k = 0; k < this.target_array.size(); k++) {
            Entity enemy = (Entity) this.target_array.elementAt(k);
            if (enemy.getId() == to.entity.getId()) {
                return damages[k];
            }
        }
        return 0;
    }

    public double[] getDamageUtilities() {
        int iChromIndex = populationDim - 1;
        targets.clear(); //could use vector and not hashtable
        double[] result = new double[this.target_array.size()];
        Chromosome chromVector = this.chromosomes[iChromIndex];
        int heat_total = 0;
        if (chromVector.genes[chromosomeDim - 1] >= this.target_array.size()) {
            chromVector.genes[chromosomeDim - 1] = ((Integer) this.valid_target_indexes.elementAt(0)).intValue();
        }
        Entity target = (Entity) this.target_array.elementAt(chromVector.genes[chromosomeDim - 1]);
        for (int iGene = 0; iGene < chromosomeDim - 1; iGene++) {
            AttackOption a = (AttackOption) (((Vector) (attack.elementAt(iGene))).elementAt(chromVector.genes[iGene]));
            if (a.target != null) { //if not the no fire option
                targets.put(a.target);
                double mod = 1;
                if (a.target.entity.getId() == target.getId()) {
                    a.target.possible_damage[a.toHit.getSideTable()] += mod * a.primary_expected;
                } else {
                    a.target.possible_damage[a.toHit.getSideTable()] += mod * a.expected;
                }
                heat_total += a.heat;
            }
        }

        for (int k = 0; k < this.target_array.size(); k++) {
            Entity en = (Entity) this.target_array.elementAt(k);
            CEntity enemy = null;
            result[k] = 0;
            if ((enemy = (CEntity) this.targets.get(new Integer(en.getId()))) != null) {
                result[k] = getThreadUtility(enemy);
                enemy.resetPossibleDamage();
            }
        }
        return result;
    }

    private double getThreadUtility(CEntity enemy) {
        if (enemy.possible_damage[ToHitData.SIDE_FRONT] > 0) {
            return enemy.getThreatUtility(enemy.possible_damage[ToHitData.SIDE_FRONT], ToHitData.SIDE_FRONT);
        } else if (enemy.possible_damage[ToHitData.SIDE_REAR] > 0) {
            return enemy.getThreatUtility(enemy.possible_damage[ToHitData.SIDE_REAR], ToHitData.SIDE_REAR);
        } else if (enemy.possible_damage[ToHitData.SIDE_LEFT] > 0) {
            return enemy.getThreatUtility(enemy.possible_damage[ToHitData.SIDE_LEFT], ToHitData.SIDE_LEFT);
        } else if (enemy.possible_damage[ToHitData.SIDE_RIGHT] > 0) {
            return enemy.getThreatUtility(enemy.possible_damage[ToHitData.SIDE_RIGHT], ToHitData.SIDE_RIGHT);
        }
        return 0;
    }

    protected double getFitness(int iChromIndex) {
        return this.getFitness(this.chromosomes[iChromIndex]);
    }

    protected double getFitness(Chromosome chromVector) {
        targets.clear(); //could use vector and not hashtable
        int heat_total = 0;
        Entity target = null;
        try {
            target = (Entity) this.target_array.elementAt(chromVector.genes[chromosomeDim - 1]);
        } catch (Exception e) {
            System.out.println(chromosomeDim + " " + chromVector.genes.length); //$NON-NLS-1$
            System.out.println(this.target_array.size());
            target = (Entity) this.target_array.elementAt(((Integer) this.valid_target_indexes.get(0)).intValue());
        }
        for (int iGene = 0; iGene < chromosomeDim - 1; iGene++) {
            final int[] genes = chromVector.genes;
            AttackOption a = (AttackOption) (((Vector) (attack.elementAt(iGene))).elementAt(genes[iGene]));
            if (a.target != null) { //if not the no fire option
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
                            mod = a.primary_odds; //low percentage shots will
                            // be frowned upon
                        }
                    }
                }
                if (a.target.entity.getId() == target.getId()) {
                    a.target.possible_damage[a.toHit.getSideTable()] += mod * a.primary_expected;
                } else {
                    a.target.possible_damage[a.toHit.getSideTable()] += mod * a.expected;
                }
                heat_total += a.heat;
            }
        }
        double total_utility = 0;
        com.sun.java.util.collections.Iterator j = targets.values().iterator();
        while (j.hasNext()) {
            CEntity enemy = (CEntity) j.next();
            total_utility+=getThreadUtility(enemy);
            enemy.resetPossibleDamage();
        }
        //should be moved
        int capacity = attacker.entity.getHeatCapacityWithWater();
        int currentHeat = attacker.entity.heatBuildup + attacker.entity.heat;
        int overheat = currentHeat + heat_total - capacity;
        if (attacker.entity.heat > 0 && overheat < 0) {
            //always perfer smaller heat numbers
            total_utility -= attacker.bv / 1000 * overheat;
            //but add clear deliniations at the breaks
            if (attacker.entity.heat > 4) {
                total_utility *= 1.2;
            }
            if (attacker.entity.heat > 7) {
                total_utility += attacker.bv / 50;
            }
            if (attacker.entity.heat > 12) {
                total_utility += attacker.bv / 20;
            }
            if (attacker.entity.heat > 16) {
                total_utility += attacker.bv / 10;
            }
        } else if (overheat > 0) {
            if (overheat > 4) {
                total_utility *= (this.overheat_eligible && attacker.jumpMP > 2) ? .9 : .85;
            }
            if (overheat > 7) {
                double mod = this.overheat_eligible ? + ((attacker.jumpMP > 2) ? 0 : 10) : 40;
                if (this.attacker.overheat > CEntity.OVERHEAT_LOW) {
                    total_utility -= attacker.bv / mod;
                } else {
                    total_utility -= attacker.bv / (mod + 10);
                }
            }
            if (overheat > 12) {
                total_utility -= attacker.bv / (this.overheat_eligible ? 45 : 30);
            }
            if (overheat > 16) {
                //only if I am going to die?
                total_utility -= attacker.bv / 5;
            }
            total_utility -= overheat / 100; //small preference for less
            // overheat opposed to more
        }
        return total_utility;
    }

    /**
     * since the low fitness members have the least chance of getting selected,
     * but the highest chance of mutation, this is where we use the primary
     * target heuristic to drive convergence
     */
    protected void doRandomMutation(int iChromIndex) {
        Chromosome c1 = this.chromosomes[iChromIndex];
        // skip if it's an empty chomosome
        if (c1.genes.length < 1)
            return;
        int r1 = (c1.genes.length > 2) ? Compute.randomInt(c1.genes.length - 1) : 0;
        CEntity target = null;
        boolean done = false;
        if (r1 % 2 == 1) {
            c1.genes[r1]--;
            if (c1.genes[r1] < 0 && attack.size() > r1) {
                c1.genes[r1] = ((Vector) this.attack.elementAt(r1)).size() - 1;
            } else {
                c1.genes[r1] = 0; // TODO : what is a good value here?
            }
            return;
        }
        //else try to move all to one target
        for (int i = 0;(i < c1.genes.length - 1) && !done; i++) {
            int iGene = (i + r1) % (c1.genes.length - 1);
            AttackOption a = (AttackOption) ((Vector) (attack.elementAt(iGene))).elementAt(c1.genes[iGene]);
            if (a.target != null) {
                target = a.target;
                done = true;
            }
        }
        if (target == null) { //then not shooting, so shoot something
            if (attack.size() > r1 && r1 > 1) {
                c1.genes[r1] = Compute.randomInt(((Vector) (attack.elementAt(r1))).size() - 1);
            } else {
                // TODO : Is this the correct action to take?
                c1.genes[r1] = Compute.randomInt(((Vector) (attack.elementAt(0))).size() - 1);
            }
            AttackOption a = (AttackOption) ((Vector) (attack.elementAt(r1))).elementAt(c1.genes[r1]);
            if (a.target != null) {
                c1.genes[c1.genes.length - 1] = a.target.enemy_num;
            }
        } else { //let's switch as many attacks as we can to this guy
            for (int i = 0;(i < (c1.genes.length - 1)) && (i < attack.size()); i++) {
                Object[] weapon = ((Vector) (attack.elementAt(i))).toArray();
                if (c1.genes[i] != weapon.length - 1) {
                    done = false;
                    for (int w = 0;(w < weapon.length - 1) && !done; w++) {
                        AttackOption a = (AttackOption) weapon[w];
                        if (a.target.enemy_num == target.enemy_num) {
                            c1.genes[i] = w;
                            done = true;
                        }
                    }
                }
            }
            (this.chromosomes[0]).genes[chromosomeDim - 1] = target.enemy_num;
        }
    }

    protected void initPopulation() {
        //promote max
        for (int iGene = 0; iGene < chromosomeDim - 1; iGene++) {
            (this.chromosomes[0]).genes[iGene] = 0;
        }

        //use first weapon target as primary, not smart but good enough...
        AttackOption a = (AttackOption) ((Vector) (attack.elementAt(0))).elementAt(0);
        (this.chromosomes[0]).genes[chromosomeDim - 1] = a.target.enemy_num;

        for (int i = 1; i < populationDim; i++) {
            Chromosome cv = this.chromosomes[i];
            for (int iGene = 0; iGene < chromosomeDim - 1; iGene++) {
                cv.genes[iGene] = Compute.randomInt(((Vector) (attack.elementAt(iGene))).size());
                if (i <= this.attack.size()) {
                    if (iGene + 1 == i)
                        cv.genes[iGene] = 0; //fire
                    else
                        cv.genes[iGene] = ((Vector) (attack.elementAt(iGene))).size() - 1; 
                }
            }
            cv.genes[chromosomeDim - 1] =
                ((Integer) this.valid_target_indexes.elementAt(Compute.randomInt(this.valid_target_indexes.size())))
                    .intValue();
            this.chromosomes[i].fitness = getFitness(i);
        }
    }
    
    public int getFiringArc() {
        return firing_arc;
    }

    public void setFiringArc(int firing_arc) {
        this.firing_arc = firing_arc;
    }

    public Vector getAttack() {
        return attack;
    }

}