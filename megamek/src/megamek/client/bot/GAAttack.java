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

import java.util.ArrayList;
import java.util.Iterator;

import megamek.client.bot.ga.Chromosome;
import megamek.client.bot.ga.GA;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mech;
import megamek.common.Terrains;
import megamek.common.ToHitData;

/**
 * Need to test the function that moves all firing to a single target
 */
public class GAAttack extends GA {

    protected ArrayList<ArrayList<AttackOption>> attack;
    protected CEntity attacker;
    protected IGame game;
    protected CEntity.Table targets;
    protected ArrayList<Entity> target_array = null;
    protected ArrayList<Integer> valid_target_indexes = null;
    protected boolean overheat_eligible = false;
    protected int firing_arc = 0;
    double[] damages = null;

    public GAAttack(TestBot tb, CEntity attacker,
            ArrayList<ArrayList<AttackOption>> attack, int population,
            int generations, boolean isEnemy) {
        super(attack.size() + 1, population, .7, .05, generations, .4);
        this.attack = attack;
        this.attacker = attacker;
        game = tb.game;
        target_array = new ArrayList<Entity>(game.getEntitiesVector());
        ArrayList<Integer> temp = new ArrayList<Integer>();
        for (int i = 0; i < target_array.size(); i++) {
            Entity entity = target_array.get(i);
            if (entity.isEnemyOf(attacker.entity) && entity.isDeployed()) {
                temp.add(new Integer(i));
            }
        }
        targets = new CEntity.Table(tb);
        valid_target_indexes = temp;
        if (attacker.tsm_offset) {
            overheat_eligible = true;
        }
        if (isEnemy
                || (attacker.last != null && (!attacker.last.inDanger || attacker.last.doomed))) {
            overheat_eligible = true;
        }
    }

    public int[] getResultChromosome() {
        return ((chromosomes[populationDim - 1]).genes);
    }

    public double getDamageUtility(CEntity to) {
        if (damages == null) {
            damages = getDamageUtilities();
        }
        for (int k = 0; k < target_array.size(); k++) {
            Entity enemy = target_array.get(k);
            if (enemy.getId() == to.entity.getId()) {
                return damages[k];
            }
        }
        return 0;
    }

    public double[] getDamageUtilities() {
        int iChromIndex = populationDim - 1;
        targets.clear(); // could use ArrayList and not hashtable
        double[] result = new double[target_array.size()];
        Chromosome chromArrayList = chromosomes[iChromIndex];
        // TODO should account for high heat?
        int heat_total = 0;
        if (chromArrayList.genes[chromosomeDim - 1] >= target_array.size()) {
            chromArrayList.genes[chromosomeDim - 1] = valid_target_indexes.get(
                    0).intValue();
        }
        Entity target = target_array
                .get(chromArrayList.genes[chromosomeDim - 1]);
        for (int iGene = 0; iGene < chromosomeDim - 1; iGene++) {
            AttackOption a = attack.get(iGene).get(chromArrayList.genes[iGene]);
            if (a.target != null) { // if not the no fire option
                targets.put(a.target);
                double mod = 1;
                if (a.target.entity.getId() == target.getId()) {
                    a.target.possible_damage[a.toHit.getSideTable()] += mod
                            * a.primary_expected;
                } else {
                    a.target.possible_damage[a.toHit.getSideTable()] += mod
                            * a.expected;
                }
                heat_total += a.heat;
            }
        }

        for (int k = 0; k < target_array.size(); k++) {
            Entity en = target_array.get(k);
            CEntity enemy = null;
            result[k] = 0;
            if ((enemy = targets.get(new Integer(en.getId()))) != null) {
                result[k] = getThreadUtility(enemy);
                enemy.resetPossibleDamage();
            }
        }
        return result;
    }

    private double getThreadUtility(CEntity enemy) {
        if (enemy.possible_damage[ToHitData.SIDE_FRONT] > 0) {
            return enemy.getThreatUtility(
                    enemy.possible_damage[ToHitData.SIDE_FRONT],
                    ToHitData.SIDE_FRONT);
        } else if (enemy.possible_damage[ToHitData.SIDE_REAR] > 0) {
            return enemy.getThreatUtility(
                    enemy.possible_damage[ToHitData.SIDE_REAR],
                    ToHitData.SIDE_REAR);
        } else if (enemy.possible_damage[ToHitData.SIDE_LEFT] > 0) {
            return enemy.getThreatUtility(
                    enemy.possible_damage[ToHitData.SIDE_LEFT],
                    ToHitData.SIDE_LEFT);
        } else if (enemy.possible_damage[ToHitData.SIDE_RIGHT] > 0) {
            return enemy.getThreatUtility(
                    enemy.possible_damage[ToHitData.SIDE_RIGHT],
                    ToHitData.SIDE_RIGHT);
        }
        return 0;
    }

    @Override
    protected double getFitness(int iChromIndex) {
        return this.getFitness(chromosomes[iChromIndex]);
    }

    protected double getFitness(Chromosome chromArrayList) {
        targets.clear(); // could use ArrayList and not hashtable
        int heat_total = 0;
        Entity target = null;
        try {
            target = target_array.get(chromArrayList.genes[chromosomeDim - 1]);
        } catch (Exception e) {
            System.out.println(chromosomeDim
                    + " " + chromArrayList.genes.length); //$NON-NLS-1$
            System.out.println(target_array.size());
            target = target_array.get(valid_target_indexes.get(0).intValue());
        }
        for (int iGene = 0; iGene < chromosomeDim - 1; iGene++) {
            final int[] genes = chromArrayList.genes;
            AttackOption a = attack.get(iGene).get(genes[iGene]);
            if (a.target != null) { // if not the no fire option
                targets.put(a.target);
                double mod = 1;
                if (a.primary_odds <= 0) {
                    mod = 0; // If there's no chance to hit at all...
                } else if (a.ammoLeft != -1) {
                    if (attacker.overall_armor_percent < .5) {
                        mod = 1.5; // get rid of it
                    } else if (a.ammoLeft < 12
                            && attacker.overall_armor_percent > .75) {
                        if (a.primary_odds < .1) {
                            mod = 0;
                        } else if (a.ammoLeft < 6 && a.primary_odds < .25) {
                            mod = 0;
                        } else {
                            mod = a.primary_odds; // low percentage shots will
                            // be frowned upon
                        }
                    }
                }
                if (a.target.entity.getId() == target.getId()) {
                    a.target.possible_damage[a.toHit.getSideTable()] += mod
                            * a.primary_expected;
                } else {
                    a.target.possible_damage[a.toHit.getSideTable()] += mod
                            * a.expected;
                }
                heat_total += a.heat;
            }
        }
        double total_utility = 0;
        Iterator<CEntity> j = targets.values().iterator();
        while (j.hasNext()) {
            CEntity enemy = j.next();
            total_utility += getThreadUtility(enemy);
            enemy.resetPossibleDamage();
        }
        // should be moved
        int capacity = attacker.entity.getHeatCapacityWithWater();
        int currentHeat = attacker.entity.heatBuildup + attacker.entity.heat;
        int overheat = currentHeat + heat_total - capacity;
        // Don't forget heat from stealth armor...
        if (attacker.entity instanceof Mech
                && (attacker.entity.isStealthActive()
                        || attacker.entity.isNullSigActive() || attacker.entity
                        .isVoidSigActive())) {
            overheat += 10;
        }
        // ... or chameleon lps...
        if (attacker.entity instanceof Mech
                && attacker.entity.isChameleonShieldActive()) {
            overheat += 6;
        }
        // ... or infernos...
        if (attacker.entity.infernos.isStillBurning()) {
            overheat += 6;
        }
        // ... or standing in fire...
        if (game.getBoard().getHex(attacker.entity.getPosition()) != null) {
            if (game.getBoard().getHex(attacker.entity.getPosition())
                    .containsTerrain(Terrains.FIRE)
                    && game.getBoard().getHex(attacker.entity.getPosition())
                            .getFireTurn() > 0) {
                overheat += 5;
            }
        }
        // ... or from engine hits
        if (attacker.entity instanceof Mech) {
            overheat += attacker.entity.getEngineCritHeat();
        }
        // ... or ambient temperature
        overheat += game.getPlanetaryConditions().getTemperatureDifference(50,
                -30);
        if (attacker.entity.heat > 0 && overheat < 0) {
            // always perfer smaller heat numbers
            total_utility -= attacker.bv / 1000 * overheat;
            // but add clear deliniations at the breaks
            if (attacker.entity.heat > 4) {
                total_utility *= 1.2;
            }
            if (attacker.entity.heat > 7) {
                total_utility += attacker.bv / 50;
            }
            if (attacker.tsm_offset) {
                if (attacker.entity.heat == 9) {
                    total_utility -= attacker.bv / 10;
                }
                if (attacker.entity.heat < 12 && attacker.entity.heat > 9) {
                    total_utility -= attacker.bv / 20;
                }
            }
            if (attacker.entity.heat > 12) {
                total_utility += attacker.bv / 20;
            }
            if (attacker.entity.heat > 16) {
                total_utility += attacker.bv / 10;
            }
        } else if (overheat > 0) {
            if (overheat > 4 && !attacker.tsm_offset) {
                total_utility *= (overheat_eligible && attacker.jumpMP > 2) ? .9
                        : .85;
            }
            if (overheat > 7 && !attacker.tsm_offset) {
                double mod = overheat_eligible ? +((attacker.jumpMP > 2) ? 0
                        : 10) : 40;
                if (attacker.overheat > CEntity.OVERHEAT_LOW) {
                    total_utility -= attacker.bv / mod;
                } else {
                    total_utility -= attacker.bv / (mod + 10);
                }
            }
            if (attacker.tsm_offset) {
                if (overheat == 9) {
                    total_utility += attacker.bv / 10;
                }
                if (attacker.entity.heat < 12 && attacker.entity.heat > 9) {
                    total_utility += attacker.bv / 20;
                }
            }
            if (overheat > 12) {
                total_utility -= attacker.bv / (overheat_eligible ? 45 : 30);
            }
            if (overheat > 16) {
                // only if I am going to die?
                total_utility -= attacker.bv / 5;
            }
            total_utility -= overheat / 100; // small preference for less
            // overheat opposed to more
        }
        return total_utility;
    }

    /**
     * since the low fitness members have the least chance of getting selected,
     * but the highest chance of mutation, this is where we use the primary
     * target heuristic to drive convergence
     */
    @Override
    protected void doRandomMutation(int iChromIndex) {
        Chromosome c1 = chromosomes[iChromIndex];
        // skip if it's an empty chromosome
        if (c1.genes.length < 1) {
            return;
        }
        int r1 = (c1.genes.length > 2) ? Compute.randomInt(c1.genes.length - 1)
                : 0;
        CEntity target = null;
        boolean done = false;
        if (r1 % 2 == 1) {
            c1.genes[r1]--;
            if (c1.genes[r1] < 0 && attack.size() > r1) {
                c1.genes[r1] = attack.get(r1).size() - 1;
            } else {
                c1.genes[r1] = 0; // TODO : what is a good value here?
            }
            return;
        }
        // else try to move all to one target
        for (int i = 0; (i < c1.genes.length - 1) && !done; i++) {
            int iGene = (i + r1) % (c1.genes.length - 1);
            AttackOption a = attack.get(iGene).get(c1.genes[iGene]);
            if (a.target != null) {
                target = a.target;
                done = true;
            }
        }
        if (target == null) { // then not shooting, so shoot something
            if (attack.size() > r1 && r1 > 1) {
                c1.genes[r1] = Compute.randomInt(attack.get(r1).size() - 1);
            } else {
                // TODO : Is this the correct action to take?
                c1.genes[r1] = Compute.randomInt(attack.get(0).size() - 1);
            }
            AttackOption a = attack.get(r1).get(c1.genes[r1]);
            if (a.target != null) {
                c1.genes[c1.genes.length - 1] = a.target.enemy_num;
            }
        } else { // let's switch as many attacks as we can to this guy
            for (int i = 0; (i < (c1.genes.length - 1)) && (i < attack.size()); i++) {
                Object[] weapon = attack.get(i).toArray();
                if (c1.genes[i] != weapon.length - 1) {
                    done = false;
                    for (int w = 0; (w < weapon.length - 1) && !done; w++) {
                        AttackOption a = (AttackOption) weapon[w];
                        if (a.target.enemy_num == target.enemy_num) {
                            c1.genes[i] = w;
                            done = true;
                        }
                    }
                }
            }
            (chromosomes[0]).genes[chromosomeDim - 1] = target.enemy_num;
        }
    }

    @Override
    protected void initPopulation() {
        // promote max
        for (int iGene = 0; iGene < chromosomeDim - 1; iGene++) {
            (chromosomes[0]).genes[iGene] = 0;
        }

        // use first weapon target as primary, not smart but good enough...
        AttackOption a = attack.get(0).get(0);
        (chromosomes[0]).genes[chromosomeDim - 1] = a.target.enemy_num;

        for (int i = 1; i < populationDim; i++) {
            Chromosome cv = chromosomes[i];
            for (int iGene = 0; iGene < chromosomeDim - 1; iGene++) {
                cv.genes[iGene] = Compute.randomInt(attack.get(iGene).size());
                if (i <= attack.size()) {
                    if (iGene + 1 == i) {
                        cv.genes[iGene] = 0; // fire
                    } else {
                        cv.genes[iGene] = attack.get(iGene).size() - 1;
                    }
                }
            }
            cv.genes[chromosomeDim - 1] = valid_target_indexes.get(
                    Compute.randomInt(valid_target_indexes.size())).intValue();
            chromosomes[i].fitness = getFitness(i);
        }
    }

    public int getFiringArc() {
        return firing_arc;
    }

    public void setFiringArc(int firing_arc) {
        this.firing_arc = firing_arc;
    }

    public ArrayList<ArrayList<AttackOption>> getAttack() {
        return attack;
    }

}
