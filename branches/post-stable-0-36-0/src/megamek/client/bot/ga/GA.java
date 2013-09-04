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

import java.util.Arrays;

public abstract class GA {
    final double mutationProb; // probability of a mutation occuring during
                               // genetic mating. For example, 0.03 means 3% chance
    final int maxGenerations; // maximum generations to evolve
    final double randomSelectionChance;
    final double crossoverProb; // probability that a crossover will occur
                                // during
    // genetic mating
    final protected int chromosomeDim; // dimension of chromosome (number of
                                        // genes)
    final protected int populationDim;
    final protected Chromosome[] chromosomes;
    Chromosome[] chromNextGen;
    double[] genAvgDeviation; // statistics--average deviation of current
    double[] genAvgFitness; // statistics--average fitness of current
    protected final int best;

    abstract protected void initPopulation();

    abstract protected void doRandomMutation(int index);

    abstract protected double getFitness(int index);

    protected void doHeuristicPass() {
        // no default
    }

    public GA(int chromosomeDim, int populationDim, double crossoverProb,
            double randomSelectionChance, int maxGenerations,
            double mutationProb) {
        this.randomSelectionChance = randomSelectionChance;
        this.chromosomeDim = chromosomeDim;
        this.populationDim = populationDim;

        best = populationDim - 1;
        this.chromosomes = new Chromosome[populationDim];
        this.chromNextGen = new Chromosome[populationDim];
        this.genAvgDeviation = new double[maxGenerations];
        this.genAvgFitness = new double[maxGenerations];

        this.crossoverProb = crossoverProb;
        this.maxGenerations = maxGenerations;
        this.mutationProb = mutationProb;

        for (int i = 0; i < populationDim; i++) {
            this.chromosomes[i] = new Chromosome(chromosomeDim);
            this.chromNextGen[i] = new Chromosome(chromosomeDim);
        }
    }

    public double getFittestChromosomesFitness() {
        return chromosomes[best].fitness;
    }

    int getRandom(int upperBound) {
        return (int) (Math.random() * upperBound);
    }

    double getRandom(double upperBound) {
        return Math.random() * upperBound;
    }

    protected boolean shouldDoExhaustive() {
        return false;
    }

    protected void doExhaustiveSearch() {
        // TODO: add something here
    }

    public int evolve() {
        int iGen = 0;
        initPopulation();
        if (this.shouldDoExhaustive()) {
            this.doExhaustiveSearch();
            return 0;
        }

        boolean converged = false;
        while (iGen < maxGenerations && !converged) {
            computeFitnessRankings();
            doGeneticMating();
            copyNextGenToThisGen();

            this.genAvgFitness[iGen] = getAvgFitness();

            if (iGen != 0 && iGen % 5 == 0) {
                this.doHeuristicPass();
            }

            if (// test for improvement and convergence
            (iGen > 5)
                    && (getESquared()
                            - (genAvgFitness[iGen] * genAvgFitness[iGen]) < .2 * genAvgFitness[iGen])
                    && (genAvgFitness[iGen] - genAvgFitness[iGen - 1] <= 0)) {
                converged = true;
            }
            iGen++;
        }

        computeFitnessRankings();
        return iGen;
    }

    protected double getAvgFitness() {
        double rSumFitness = 0.0;
        for (int i = 0; i < populationDim; i++) {
            rSumFitness += this.chromosomes[i].fitness;
        }
        return (rSumFitness / populationDim);
    }

    protected double getESquared() {
        double square_sum = 0;
        for (int i = 0; i < populationDim; i++) {
            square_sum += this.chromosomes[i].fitness
                    * this.chromosomes[i].fitness;
        }
        return (square_sum / populationDim);
    }

    protected int[] selectTwoParents() {
        boolean found = false;
        int[] parents = new int[2];
        parents[0] = -1;

        for (int i = 0; i < 2; i++) {
            found = false;
            while (!found) {
                int index = getRandom(populationDim);
                if (index == parents[0]) {
                    continue;
                }
                if (randomSelectionChance > getRandom(1.0)
                        || index + 1 > getRandom(populationDim)) {
                    parents[i] = index;
                    found = true;
                }
            }
        }
        return parents;
    }

    void computeFitnessRankings() {
        for (int i = 0; i < populationDim; i++)
            this.chromosomes[i].fitness = getFitness(i);

        Arrays.sort(chromosomes);
    }

    void doGeneticMating() {
        int max = populationDim - 1;
        for (int i = 0; i < max; i += 2) {
            int indexes[] = selectTwoParents();

            chromNextGen[i].copyChromGenes(chromosomes[indexes[0]]);
            chromNextGen[i + 1].copyChromGenes(chromosomes[indexes[1]]);

            if (getRandom(1.0) < crossoverProb) {
                doUniformCrossover(chromNextGen[i], chromNextGen[i + 1]);
            }
        }
        chromNextGen[populationDim - 1].copyChromGenes(chromosomes[best]);
    }

    void copyNextGenToThisGen() {
        for (int i = 0; i < populationDim; i++) {
            this.chromosomes[i].copyChromGenes(this.chromNextGen[i]);

            if (i != best && (i == 0 || getRandom(1.0) < mutationProb)) {
                doRandomMutation(i);
            }
        }
    }

    void doUniformCrossover(Chromosome c1, Chromosome c2) {
        for (int iGene = 0; iGene < chromosomeDim; iGene++) {
            if (getRandom(1.0) < .5) {
                int gene = c1.genes[iGene];
                c1.genes[iGene] = c2.genes[iGene];
                c2.genes[iGene] = gene;
            }
        }
    }
}