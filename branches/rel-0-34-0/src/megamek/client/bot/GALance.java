/*
 * MegaMek - Copyright (C) 2002,2003 Ben Mazur (bmazur@sev.org)
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

public class GALance extends GA {

    protected ArrayList<MoveOption[]> moves;
    protected TestBot tb;
    protected Object[] enemy_array;

    public GALance(TestBot tb, ArrayList<MoveOption[]> moves, int population,
            int generations) {
        super(moves.size(), population, .7, .05, generations, .5);
        System.gc();
        System.out.println("Generated move lance with population=" + population
                + " and generations=" + generations);
        this.tb = tb;
        this.moves = moves;
        this.enemy_array = tb.getEnemyEntities().toArray();
    }

    protected void initPopulation() {
        // promote max
        try {
            for (int iGene = 0; iGene < chromosomeDim; iGene++) {
                (this.chromosomes[0]).genes[iGene] = 0;
            }
            for (int i = 1; i < populationDim; i++) {
                for (int iGene = 0; iGene < chromosomeDim; iGene++) {
                    (this.chromosomes[i]).genes[iGene] = Compute
                            .randomInt(moves.get(iGene).length);
                }
                this.chromosomes[i].fitness = getFitness(i);
            }
        } catch (Exception e) {
            System.out
                    .println("Error occured with " + populationDim + " pop " + chromosomeDim + " chromDim"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            Iterator<MoveOption[]> i = moves.iterator();
            while (i.hasNext()) {
                System.out.println(i.next());
            }
        }
    }

    // now they have a hard-coded hoard metality
    protected double getFitness(int iChromIndex) {
        Chromosome chrom = this.chromosomes[iChromIndex];
        ArrayList<MoveOption> possible = new ArrayList<MoveOption>();
        for (int iGene = 0; iGene < chromosomeDim; iGene++) {
            possible.add(new MoveOption(
                    this.moves.get(iGene)[chrom.genes[iGene]]));
        }
        Object[] move_array = possible.toArray();
        for (int e = 0; e < enemy_array.length; e++) { // for each enemy
            CEntity enemy = tb.centities.get((Entity) enemy_array[e]);
            MoveOption max = (MoveOption) move_array[0];
            for (int m = 1; m < move_array.length; m++) {
                if (((MoveOption) move_array[m]).getThreat(enemy) > max
                        .getThreat(enemy)) {
                    max = (MoveOption) move_array[m];
                }
                MoveOption next = (MoveOption) move_array[m];
                if (next.getThreat(enemy) > 0) {
                    if (next.getThreat(enemy) < .25 * max.getThreat(enemy)) {
                        next.setThreat(enemy, 0);
                    } else {
                        next.setThreat(enemy, Math.pow(next.getThreat(enemy)
                                / max.getThreat(enemy), 2)
                                * next.getThreat(enemy));
                    }
                }
            }
        }
        // total damage delt, and rescaling of threat
        double damages[] = new double[enemy_array.length];
        for (int m = 0; m < move_array.length; m++) {
            MoveOption next = (MoveOption) move_array[m];
            next.threat = 0;
            for (int e = 0; e < enemy_array.length; e++) {
                CEntity enemy = tb.centities.get((Entity) enemy_array[e]);
                next.threat += next.getThreat(enemy);
                damages[e] = (next.getMinDamage(enemy) + next.getDamage(enemy)) / 2;
            }
        }
        // sacrificial lamb check
        double result = 0;
        for (int m = 0; m < move_array.length; m++) {
            MoveOption next = (MoveOption) move_array[m];
            if (moves.get(m).length > 1) {
                MoveOption min = moves.get(m)[0];
                if (min.damage > 2 * next.damage
                        && min.getUtility() < .5 * next.getUtility()) {
                    result += next.getCEntity().bv; // it is being endangered
                    // in the future
                    if (m > 0)
                        chrom.genes[m]--; // go so far as to mutate the
                    // gene
                }
            }
        }
        // int difference = this.tb.NumEnemies - this.tb.NumFriends;
        double distance_mod = 0;
        // if outnumbered and loosing, clump together.
        try {
            int target_distance = 4;
            for (int m = 0; m < move_array.length; m++) {
                MoveOption next = (MoveOption) move_array[m];
                next.getUtility();
                for (int j = 0; j < move_array.length; j++) {
                    MoveOption other = (MoveOption) move_array[j];
                    if (m != j) {
                        int distance = other.getFinalCoords().distance(
                                next.getFinalCoords());
                        if (distance > target_distance) {
                            distance_mod += Math.pow(
                                    distance - target_distance, 2);
                        } else if (distance <= 3) {
                            CEntity target = null;
                            for (int e = 0; e < enemy_array.length; e++) {
                                CEntity cen = tb.centities
                                        .get((Entity) this.enemy_array[e]);
                                if (!cen.canMove()) {
                                    if ((next.getFinalCoords() != null)
                                            && (other.getFinalCoords() != null)
                                            && (cen.current.getFinalCoords() != null)
                                            && ((cen.current
                                                    .getFinalCoords()
                                                    .distance(
                                                            next
                                                                    .getFinalCoords()) == 1 && cen.current
                                                    .getFinalCoords()
                                                    .distance(
                                                            other
                                                                    .getFinalCoords()) == 1) || (cen.current
                                                    .getFinalCoords()
                                                    .distance(
                                                            next
                                                                    .getFinalCoords()) <= 3
                                                    && cen.current
                                                            .getFinalCoords()
                                                            .distance(
                                                                    other
                                                                            .getFinalCoords()) <= 3 && cen.current
                                                    .getFinalProne())
                                                    && !(next.inDanger || next
                                                            .getFinalProne()))) {
                                        target = cen;
                                    }
                                }
                            }
                            if (target != null) {
                                if (target.entity.isProne()) {
                                    distance_mod -= target.bv / 100;
                                }
                                distance_mod -= target.bv / 50;
                                next.setDamage(target,
                                        next.getDamage(target) * 1.2);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        double max = 0;
        // bonuses for endangering or dooming opponent mechs
        for (int e = 0; e < enemy_array.length; e++) {
            CEntity cen = tb.centities.get((Entity) this.enemy_array[e]);
            if (damages[e] > cen.avg_armor) {
                if (damages[e] > 4 * cen.avg_armor) {
                    max += cen.bv / 5; // likely to die
                } else {
                    max += cen.bv / 50; // in danger
                }
            } else if (damages[e] > 40) {
                max += (1 - cen.base_psr_odds) * cen.entity.getWeight();
            }
        }
        // if noone is in danger at least give a bonus for clustering
        if (max == 0) {
            for (int e = 0; e < enemy_array.length; e++) {
                if (damages[e] > max) {
                    max = damages[e];
                }
            }
        }
        distance_mod /= move_array.length * move_array.length;

        for (int m = 0; m < move_array.length; m++) {
            MoveOption next = (MoveOption) move_array[m];
            if (next.inDanger) {
                if (next.doomed) {
                    if (next.getCEntity().last != null
                            && next.getCEntity().last.doomed) {
                        result -= next.damage - .5 * next.getUtility(); // should
                        // be
                        // dead
                    } else if (next.getCEntity().last != null
                            && !next.getCEntity().last.doomed) {
                        result += next.getUtility() + 2 * next.damage; // don't
                        // like
                        // this
                        // case
                    } else {
                        result += next.getUtility();
                    }
                } else {
                    if (next.getCEntity().last != null
                            && !next.getCEntity().last.inDanger) {
                        result += next.getUtility() + next.damage; // not so
                        // good
                        // either
                    } else {
                        result += next.getUtility();
                    }
                }
            } else {
                result += next.getUtility();
            }
        }
        return -result + (max - distance_mod);
    }

    public MoveOption getResult() {
        Chromosome r = this.chromosomes[best];
        ArrayList<MoveOption> possible = new ArrayList<MoveOption>();
        for (int iGene = 0; iGene < chromosomeDim; iGene++) {
            possible.add(new MoveOption(this.moves.get(iGene)[r.genes[iGene]]));
        }
        Object[] move_array = possible.toArray();
        MoveOption result = null;
        for (int m = 0; m < move_array.length; m++) {
            MoveOption next = (MoveOption) move_array[m];
            CEntity cen = tb.centities.get(next.getEntity());
            if (!cen.moved
                    && (result == null || (next.getUtility() < result
                            .getUtility()))) {
                result = next;
            }
        }
        return result;
    }

    protected void doRandomMutation(int iChromIndex) {
        Chromosome c1 = this.chromosomes[iChromIndex];
        // I don't think we need to mutate an empty chromosome
        if (c1.genes.length < 1) {
            return;
        }
        int r1 = (c1.genes.length > 2) ? Compute.randomInt(c1.genes.length - 1)
                : 0;
        if (r1 % 2 == 1) {
            c1.genes[r1] = Compute.randomInt(this.moves.get(r1).length);
            return;
        }
        for (int i = 1; i < c1.genes.length; i++) {
            int iGene = (i + r1 - 1) % (c1.genes.length - 1);
            if (this.moves.get(iGene).length > 1) {
                c1.genes[iGene] = Compute
                        .randomInt(this.moves.get(iGene).length);
                return;
            }
        }
    }
}
