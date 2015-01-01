/**
 * MegaMek -
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.ga;

public class Chromosome implements Comparable<Chromosome> {
    public double fitness; // absolute fitness value
    public int[] genes;

    public Chromosome(int iGenesDim) {
        genes = new int[iGenesDim];
    }

    public String toString() {
        return genes.toString();
    }

    public void copyChromGenes(Chromosome chromosome) {
        System.arraycopy(chromosome.genes, 0, genes, 0, genes.length);
    }

    public int compareTo(Chromosome o) {
        double delta = fitness - o.fitness;
        return delta > 0 ? 1 : delta < 0 ? -1 : 0;
    }

}
