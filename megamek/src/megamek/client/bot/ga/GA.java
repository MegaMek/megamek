package megamek.client.bot.ga;

public abstract class GA {
    double mutationProb; //probability of a mutation occuring during genetic
    // mating. For example, 0.03 means 3% chance
    int maxGenerations; //maximum generations to evolve
    int randomSelectionChance; //1-100 (e.g. 10 = 10% chance of random
							   // selection--not based on fitness)
    //Setting nonzero randomSelectionChance helps maintain genetic diversity
    //during evolution
    double crossoverProb; //probability that a crossover will occur during
						  // genetic mating
    protected int chromosomeDim; //dimension of chromosome (number of genes)
    protected int populationDim; //number of chromosomes to evolve. A larger
								 // population dim will result in
    //a better evolution but will slow the process down
    public Chromosome[] chromosomes; //storage for pool of chromosomes for
									 // current generation
    Chromosome[] chromNextGen; //storage for temporary holding pool for next
							   // generation chromosomes
    public int bestFitnessChromIndex; //index of fittest chromosome in current
    int worstFitnessChromIndex; //index of least fit chromosome in current
    double[] genAvgDeviation; //statistics--average deviation of current
    double[] genAvgFitness; //statistics--average fitness of current
    abstract protected void initPopulation();
    abstract protected void doRandomMutation(int iChromIndex);
    abstract protected double getFitness(int iChromIndex);

    protected void doHeuristicPass() {
        //no default
    }

    public GA(
        int chromosomeDim,
        int populationDim,
        double crossoverProb,
        int randomSelectionChance,
        int maxGenerations,
        double mutationProb) {
        this.randomSelectionChance = randomSelectionChance;
        this.chromosomeDim = chromosomeDim;
        this.populationDim = populationDim;

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

    public double getAvgFitness(int iGeneration) {
        return (this.genAvgFitness[iGeneration]);
    }

    public double getMutationProb() {
        return mutationProb;
    }

    public int getMaxGenerations() {
        return maxGenerations;
    }

    public double getCrossoverProb() {
        return crossoverProb;
    }

    public double getFittestChromosomesFitness() {
        return (this.chromosomes[bestFitnessChromIndex].fitness);
    }

    /** return a integer random number between 0 and upperBound */
    int getRandom(int upperBound) {
        int iRandom = (int) (Math.random() * upperBound);
        return (iRandom);
    }

    /** return a double random number between 0 and upperBound */
    double getRandom(double upperBound) {
        double dRandom = (Math.random() * upperBound);
        return (dRandom);
    }

    protected boolean shouldDoExhaustive() {
        return false;
    }

    protected void doExhaustiveSearch() {
        //TODO: add something here
    }

    /**
	 * Do genetic evolution of this population of chromosomes. Returns: number
	 * of generations
	 */
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

            if (//test for improvement and convergence
             (iGen > 5)
                && (this.getESquared() - (this.genAvgFitness[iGen] * this.genAvgFitness[iGen])
                    < .2 * this.genAvgFitness[iGen])
                && (this.genAvgFitness[iGen] - this.genAvgFitness[iGen - 1] <= 0)) {
                converged = true;
            }
            iGen++;
        }

        computeFitnessRankings();
        return (iGen);
    }

    /** Go thru all chromosomes and calc the avg fitness of this generation */
    public double getAvgFitness() {
        double rSumFitness = 0.0;
        for (int i = 0; i < populationDim; i++) {
            rSumFitness += this.chromosomes[i].fitness;
        }
        return (rSumFitness / populationDim);
    }

    public double getESquared() {
        double square_sum = 0;
        for (int i = 0; i < populationDim; i++) {
            square_sum += this.chromosomes[i].fitness * this.chromosomes[i].fitness;
        }
        return (square_sum / populationDim);
    }

    /**
	 * Select two parents from population, giving highly fit individuals a
	 * greater chance of being selected.
	 */
    public void selectTwoParents(int[] indexParents) {
        int indexParent1 = indexParents[0];
        int indexParent2 = indexParents[1];
        boolean bFound = false;
        int index;

        while (bFound == false) {
            index = getRandom(populationDim); 
            if (randomSelectionChance > getRandom(100)) {
                indexParent1 = index;
                bFound = true;
            } else {
                if (this.chromosomes[index].fitnessRank + 1 > getRandom(populationDim)) {
                    indexParent1 = index;
                    bFound = true;
                }
            }
        }

        bFound = false;
        while (bFound == false) {
            index = getRandom(populationDim); 
            if (randomSelectionChance > getRandom(100)) {
                if (index != indexParent1) {
                    indexParent2 = index;
                    bFound = true;
                }
            } else {
                if ((index != indexParent1) && (this.chromosomes[index].fitnessRank + 1 > getRandom(populationDim))) {
                    indexParent2 = index;
                    bFound = true;
                }
            }
        }

        indexParents[0] = indexParent1;
        indexParents[1] = indexParent2;
    }

    /**
	 * calculate the ranking of the parameter "fitness" with respect to the
	 * current generation. If the fitness is high, the corresponding fitness
	 * ranking will be high, too. For example, if the fitness passed in is
	 * higher than any fitness value for any chromosome in the current
	 * generation, the fitnessRank will equal the populationDim. And if the
	 * fitness is lower than any fitness value for any chromosome in the
	 * current generation, the fitnessRank will equal zero.
	 */
    int getFitnessRank(double fitness) {
        int fitnessRank = -1;
        for (int i = 0; i < populationDim; i++) {
            if (fitness >= this.chromosomes[i].fitness)
                fitnessRank++;
        }

        return (fitnessRank);
    }

    /**
	 * Calculate rankings for all chromosomes. High ranking numbers denote very
	 * fit chromosomes.
	 */
    void computeFitnessRankings() {

        // recalc the fitness of each chromosome
        for (int i = 0; i < populationDim; i++)
            this.chromosomes[i].fitness = getFitness(i);

        for (int i = 0; i < populationDim; i++)
            this.chromosomes[i].fitnessRank = getFitnessRank(this.chromosomes[i].fitness);

        for (int i = 0; i < populationDim; i++) {
            if (this.chromosomes[i].fitnessRank == populationDim - 1) {
                // rBestFitnessVal = this.chromosomes[i].fitness;
                this.bestFitnessChromIndex = i;
            }
            if (this.chromosomes[i].fitnessRank == 0) {
                // rWorstFitnessVal = this.chromosomes[i].fitness;
                this.worstFitnessChromIndex = i;
            }
        }
    }

    /**
	 * Create the next generation of chromosomes by genetically mating fitter
	 * individuals of the current generation. Also employ elitism (so the
	 * fittest 2 chromosomes always survive to the next generation). This way
	 * an extremely fit chromosome is never lost from our chromosome pool.
	 */
    void doGeneticMating() {
        int iCnt;
        int indexParent1 = -1, indexParent2 = -1;
        Chromosome Chrom1, Chrom2;

        iCnt = 0;

        //Elitism--fittest chromosome automatically go on to next gen (in 2
		// offspring)
        this.chromNextGen[iCnt].copyChromGenes(this.chromosomes[this.bestFitnessChromIndex]);
        iCnt++;
        this.chromNextGen[iCnt].copyChromGenes(this.chromosomes[this.bestFitnessChromIndex]);
        iCnt++;

        Chrom1 = new Chromosome(chromosomeDim);
        Chrom2 = new Chromosome(chromosomeDim);

        do {
            int indexes[] = { indexParent1, indexParent2 };
            selectTwoParents(indexes);
            indexParent1 = indexes[0];
            indexParent2 = indexes[1];

            Chrom1.copyChromGenes(this.chromosomes[indexParent1]);
            Chrom2.copyChromGenes(this.chromosomes[indexParent2]);

            if (getRandom(1.0) < crossoverProb) //do crossover
                {
                doUniformCrossover(Chrom1, Chrom2);

                this.chromNextGen[iCnt].copyChromGenes(Chrom1);
                iCnt++;
                this.chromNextGen[iCnt].copyChromGenes(Chrom2);
                iCnt++;
            } else //if no crossover, copy parent chromosome "as is" into the
				   // offspring
                {
                // CREATE OFFSPRING ONE
                this.chromNextGen[iCnt].copyChromGenes(Chrom1);
                iCnt++;

                // CREATE OFFSPRING TWO
                this.chromNextGen[iCnt].copyChromGenes(Chrom2);
                iCnt++;
            }
        } while (iCnt < populationDim);
    }

    /**
	 * Copy the chromosomes previously created and stored in the "next"
	 * generation into the main chromsosome memory pool. Perform random
	 * mutations where appropriate.
	 */
    void copyNextGenToThisGen() {
        for (int i = 0; i < populationDim; i++) {
            this.chromosomes[i].copyChromGenes(this.chromNextGen[i]);

            //only mutate chromosomes if it is NOT the best
            if (i != this.bestFitnessChromIndex) {
                //always mutate the chromosome with the lowest fitness
                if ((i == this.worstFitnessChromIndex) || (getRandom(1.0) < mutationProb))
                    doRandomMutation(i);
            }
        }
    }

    protected void doUniformCrossover(Chromosome c1, Chromosome c2) {
        int gene = 0;
        for (int iGene = 0; iGene < chromosomeDim; iGene++) {
            if (Math.random() < .5) {
                gene = c1.genes[iGene];
                c1.genes[iGene] = c2.genes[iGene];
                c2.genes[iGene] = gene;
            }
        }
    }
}