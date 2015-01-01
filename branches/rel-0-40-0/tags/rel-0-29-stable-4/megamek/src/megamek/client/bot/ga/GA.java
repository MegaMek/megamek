package megamek.client.bot.ga;
import java.util.*;


/**
 * Package ga
 * ----------
 * The GAFloat, GAString, and GASequenceList classes all extend the GA class and
 * can be used to model different populations of candidate solutions. You will
 * generally have to extend one of these classes every time you create a new GA.
 * In the simplest cases, you will subclass one of these classes and then just
 * override and implement your own getFitness() function. The three main
 * subclasses of GA are:
 * GAString (chromosomes are stored as strings)
 * GAFloat (chromosomes are stored as floating point numbers)
 * GASequenceList (chromosomes are stored as strings, additional methods in
 * this class handle sorting sequences. For example, the
 * GASalesman class extends GASequenceList)
 *
 * For example:
 * If your chromosomes are floating point numbers, you should subclass GAFloat
 * and override the getFitness() function with your own.
 *
 * If your chromosomes are strings, you should subclass GAString and override
 * the getFitness() function with your own.
 *
 * If your chromosomes are characters in a sequence (or list) that needs to
 * be rearranged, you should use GASequenceList and override the getFitness()
 * function with your own.
 */

public abstract class GA implements Runnable {
  double mutationProb;       //probability of a mutation occuring during genetic
  // mating. For example, 0.03 means 3% chance
  int maxGenerations;        //maximum generations to evolve
  int numPrelimRuns;         //number of prelim generations to evolve. Set to zero to disable
  //prelim generations. Prelim runs are useful for building fitter
  //"starting" chromosome stock before the main evolution run.
  int maxPrelimGenerations;  //maximum prelim generations to evolve
  int randomSelectionChance; //1-100 (e.g. 10 = 10% chance of random selection--not based on fitness)
  //Setting nonzero randomSelectionChance helps maintain genetic diversity
  //during evolution
  double crossoverProb;      //probability that a crossover will occur during genetic mating
  protected int chromosomeDim;  //dimension of chromosome (number of genes)
  protected int populationDim;  //number of chromosomes to evolve. A larger population dim will result in
  //a better evolution but will slow the process down
  public Chromosome[] chromosomes;   //storage for pool of chromosomes for current generation
  Chromosome[] chromNextGen;  //storage for temporary holding pool for next generation chromosomes
  Chromosome[] prelimChrom;   //storage for pool of prelim generation chromosomes
  public int bestFitnessChromIndex;  //index of fittest chromosome in current generation
  int worstFitnessChromIndex; //index of least fit chromosome in current generation
  protected int crossoverType;//type of crossover to be employed during genetic mating
  double[] genAvgDeviation;   //statistics--average deviation of current generation
  double[] genAvgFitness;     //statistics--average fitness of current generation
  boolean computeStatistics;  //compute statistics for each generation during evolution?
  boolean verbose;            //display System.out.println() messages during run?
  
  abstract protected void initPopulation();
  abstract protected void doRandomMutation(int iChromIndex);
  abstract protected void doUniformCrossover(Chromosome Chrom1, Chromosome Chrom2);
  abstract protected double getFitness(int iChromIndex);
  abstract protected Chromosome getNewChrom(int chromDim);
  protected void doHeuristicPass() {}; // override if you want
  
  public void run() {
    evolve();
  }
  
  public GA(int chromosomeDim,
  int populationDim,
  double crossoverProb,
  int randomSelectionChance,
  int maxGenerations,
  int numPrelimRuns,
  int maxPrelimGenerations,
  double mutationProb,
  int crossoverType,
  boolean computeStatistics,
  boolean verbose) {
    this.randomSelectionChance = randomSelectionChance;
    this.crossoverType = crossoverType;
    this.chromosomeDim = chromosomeDim;
    this.populationDim = populationDim;
    this.computeStatistics = computeStatistics;
    this.verbose = verbose;
    
    this.chromosomes = new Chromosome[populationDim];
    this.chromNextGen = new Chromosome[populationDim];
    this.prelimChrom = new Chromosome[populationDim];
    this.genAvgDeviation = new double[maxGenerations];
    this.genAvgFitness = new double[maxGenerations];
    
    this.crossoverProb = crossoverProb;
    this.maxGenerations = maxGenerations;
    this.numPrelimRuns = numPrelimRuns;
    this.maxPrelimGenerations = maxPrelimGenerations;
    this.mutationProb = mutationProb;
    
    for (int i=0; i < populationDim; i++) {
      this.chromosomes[i] = this.getNewChrom(chromosomeDim);
      this.chromNextGen[i] = this.getNewChrom(chromosomeDim);
      this.prelimChrom[i] = this.getNewChrom(chromosomeDim);
    }
  }
  
  public double getAvgFitness(int iGeneration) {
    return(this.genAvgFitness[iGeneration]);
  }
  
  public double getMutationProb() {
    return mutationProb;
  }
  
  public int getMaxGenerations() {
    return maxGenerations;
  }
  
  public int getNumPrelimRuns() {
    return numPrelimRuns;
  }
  
  public int getMaxPrelimGenerations() {
    return maxPrelimGenerations;
  }
  
  public int getRandomSelectionChance() {
    return randomSelectionChance;
  }
  
  public double getCrossoverProb() {
    return crossoverProb;
  }
  
  public int getChromosomeDim() {
    return chromosomeDim;
  }
  
  public int getPopulationDim() {
    return populationDim;
  }
  
  public int getCrossoverType() {
    return crossoverType;
  }
  
  
  public boolean getComputeStatistics() {
    return computeStatistics;
  }
  
  public String getFittestChromosome() {
    return(this.chromosomes[bestFitnessChromIndex].getGenesAsStr());
  }
  
  public double getFittestChromosomesFitness() {
    return(this.chromosomes[bestFitnessChromIndex].fitness);
  }
  
  /** return a integer random number between 0 and upperBound */
  int getRandom(int upperBound) {
    int iRandom = (int)(Math.random() * upperBound);
    return(iRandom);
  }
  
  /** return a double random number between 0 and upperBound */
  double getRandom(double upperBound) {
    double dRandom = (Math.random() * upperBound);
    return(dRandom);
  }
  
  public void log(String s) {
    if (verbose == true)
      System.out.println(s);
  }
  
  protected boolean shouldDoExhaustive() {
    return false;
  }
  
  protected void doExhaustiveSearch() {
    
  }
  
  /**
   * Do genetic evolution of this population of chromosomes.
   * Returns: number of generations
   */
  public int evolve() {
    int iGen;
    //    int iPrelimChrom, iPrelimChromToUsePerRun;
    
    if (this.shouldDoExhaustive()) {
      this.doExhaustiveSearch();
      return 0;
    }
    
    log("GA start time: " + new Date().toString());
    
    /*if (numPrelimRuns > 0) {
      iPrelimChrom = 0;
      //number of fittest prelim chromosomes to use with final run
      iPrelimChromToUsePerRun = populationDim / numPrelimRuns;
      
      for (int iPrelimRuns = 1; iPrelimRuns <= numPrelimRuns; iPrelimRuns++) {
        iGen = 0;
        initPopulation();
        
        //create a somewhat fit chromosome population for this prelim run
        while (iGen < maxPrelimGenerations) {
          log(iPrelimRuns + " of " + numPrelimRuns + " prelim runs --> " + (iGen+1) + " of " + maxPrelimGenerations + " generations");
          
          computeFitnessRankings();
          doGeneticMating();
          copyNextGenToThisGen();
          
          if (computeStatistics == true) {
            this.genAvgFitness[iGen] = getAvgFitness();
          }
          iGen++;
        }
        
        computeFitnessRankings();
        
        //copy these somewhat fit chromosomes to the main chromosome pool
        int iNumPrelimSaved = 0;
        for (int i=0; i < populationDim && iNumPrelimSaved < iPrelimChromToUsePerRun; i++)
          if (this.chromosomes[i].fitnessRank >= populationDim-iPrelimChromToUsePerRun) {
            this.prelimChrom[iPrelimChrom + iNumPrelimSaved].copyChromGenes(this.chromosomes[i]);  //store (remember) these fit chroms
            iNumPrelimSaved++;
          }
        iPrelimChrom += iNumPrelimSaved;
      }
      for (int i=0; i < iPrelimChrom; i++)
        this.chromosomes[i].copyChromGenes(this.prelimChrom[i]);
      log("INITIAL POPULATION AFTER PRELIM RUNS:");
    }
    else
      log("INITIAL POPULATION (NO PRELIM RUNS):");
    
    //Add Preliminary Chromosomes to list box
    addChromosomesToLog(0, 10);
    */
    
    iGen = 0;
    boolean converged = false;
    while (iGen < maxGenerations && !converged) {
      computeFitnessRankings();
      doGeneticMating();
      copyNextGenToThisGen();
      
      if (computeStatistics == true) {
        this.genAvgFitness[iGen] = getAvgFitness();
      }
      
      if (iGen != 0 && iGen%5 == 0) {
        this.doHeuristicPass();
      }
      
      if ( //test for improvement and convergence
      (iGen > 5) && 
      (this.getESquared() - (this.genAvgFitness[iGen]*this.genAvgFitness[iGen]) < .2*this.genAvgFitness[iGen]) && 
      (this.genAvgFitness[iGen] - this.genAvgFitness[iGen - 1] <= 0)
      ) {
        converged = true;
      }
      iGen++;
    }
    
    log("GEN " + (iGen+1) + " AVG FITNESS = " + this.getAvgFitness());
    
    addChromosomesToLog(iGen, 10);  //display Chromosomes to system.out
    
    computeFitnessRankings();
    log("Best Chromosome Found: ");
    log(this.chromosomes[this.bestFitnessChromIndex].getGenesAsStr() + " Fitness= " + this.chromosomes[this.bestFitnessChromIndex].fitness);
    
    log("GA end time: " + new Date().toString());
    return(iGen);
  }
  
  /** Go thru all chromosomes and calc the avg fitness of this generation */
  public double getAvgFitness() {
    double rSumFitness = 0.0;
    for (int i=0; i < populationDim; i++) {
      rSumFitness += this.chromosomes[i].fitness;
    }
    return(rSumFitness / populationDim);
  }
  
  public double getESquared() {
    double square_sum = 0;
    for (int i=0; i < populationDim; i++) {
      square_sum += this.chromosomes[i].fitness*this.chromosomes[i].fitness;
    }
    return(square_sum / populationDim);
  }
  
  /**
   * Select two parents from population, giving highly fit individuals a greater
   * chance of being selected.
   */
  public void selectTwoParents(int[] indexParents) {
    int indexParent1 = indexParents[0];
    int indexParent2 = indexParents[1];
    boolean bFound = false;
    int index;
    
    while (bFound == false) {
      index = getRandom(populationDim);  //get random member of population
      
      if (randomSelectionChance > getRandom(100)) {
        indexParent1 = index;
        bFound = true;
      }
      else {
        //the greater a chromosome's fitness rank, the higher prob that
        // it will be selected to reproduce
        if (this.chromosomes[index].fitnessRank+1 > getRandom(populationDim)) {
          indexParent1 = index;
          bFound = true;
        }
      }
    }
    
    bFound = false;
    while (bFound == false) {
      index = getRandom(populationDim);  //get random member of population
      
      if (randomSelectionChance > getRandom(100)) {
        if (index != indexParent1) {
          indexParent2 = index;
          bFound = true;
        }
      }
      else {
        //the greater a chromosome's fitness rank, the higher prob that it
        // will be selected to reproduce
        if ((index != indexParent1) && (this.chromosomes[index].fitnessRank+1 > getRandom(populationDim))) {
          indexParent2 = index;
          bFound = true;
        }
      }
    }
    
    indexParents[0] = indexParent1;
    indexParents[1] = indexParent2;
  }
  
  /** calculate the ranking of the parameter "fitness" with respect to the
   *  current generation. If the fitness is high, the corresponding fitness
   *  ranking will be high, too. For example, if the fitness passed in is
   *  higher than any fitness value for any chromosome in the current
   *  generation, the fitnessRank will equal the populationDim. And if the
   *  fitness is lower than any fitness value for any chromosome in the
   *  current generation, the fitnessRank will equal zero.
   */
  int getFitnessRank(double fitness) {
    int fitnessRank = -1;
    for (int i=0; i < populationDim; i++) {
      if (fitness >= this.chromosomes[i].fitness)
        fitnessRank++;
    }
    
    return(fitnessRank);
  }
  
  /**
   * Calculate rankings for all chromosomes. High ranking numbers denote
   * very fit chromosomes.
   */
  void computeFitnessRankings() {
    
    // recalc the fitness of each chromosome
    for (int i=0; i < populationDim; i++)
      this.chromosomes[i].fitness = getFitness(i);
    
    for (int i=0; i < populationDim; i++)
      this.chromosomes[i].fitnessRank = getFitnessRank(this.chromosomes[i].fitness);
    
    // double rBestFitnessVal; // not read
    // double rWorstFitnessVal; // not read
    for (int i=0; i < populationDim; i++) {
      if (this.chromosomes[i].fitnessRank == populationDim-1) {
          // rBestFitnessVal = this.chromosomes[i].fitness;
          this.bestFitnessChromIndex = i;
      }
      if (this.chromosomes[i].fitnessRank == 0) {
          // rWorstFitnessVal = this.chromosomes[i].fitness;
          this.worstFitnessChromIndex = i;
      }
    }
  }
  
  /** Create the next generation of chromosomes by genetically mating fitter
   *  individuals of the current generation. Also employ elitism (so the
   *  fittest 2 chromosomes always survive to the next generation). This way
   *  an extremely fit chromosome is never lost from our chromosome pool.
   */
  void doGeneticMating() {
    int iCnt;
    int indexParent1 = -1, indexParent2 = -1;
    Chromosome Chrom1, Chrom2;
    
    iCnt = 0;
    
    //Elitism--fittest chromosome automatically go on to next gen (in 2 offspring)
    this.chromNextGen[iCnt].copyChromGenes(this.chromosomes[this.bestFitnessChromIndex]);
    iCnt++;
    this.chromNextGen[iCnt].copyChromGenes(this.chromosomes[this.bestFitnessChromIndex]);
    iCnt++;
    
    Chrom1 = this.getNewChrom(chromosomeDim);
    Chrom2 = this.getNewChrom(chromosomeDim);
    
    do {
      int indexes[] = { indexParent1, indexParent2 };
      selectTwoParents(indexes);
      indexParent1 = indexes[0];
      indexParent2 = indexes[1];
      
      Chrom1.copyChromGenes(this.chromosomes[indexParent1]);
      Chrom2.copyChromGenes(this.chromosomes[indexParent2]);
      
      if (getRandom(1.0) < crossoverProb)  //do crossover
      {
        doUniformCrossover(Chrom1, Chrom2);
        
        this.chromNextGen[iCnt].copyChromGenes(Chrom1);
        iCnt++;
        this.chromNextGen[iCnt].copyChromGenes(Chrom2);
        iCnt++;
      }
      else  //if no crossover, copy parent chromosome "as is" into the offspring
      {
        // CREATE OFFSPRING ONE
        this.chromNextGen[iCnt].copyChromGenes(Chrom1);
        iCnt++;
        
        // CREATE OFFSPRING TWO
        this.chromNextGen[iCnt].copyChromGenes(Chrom2);
        iCnt++;
      }
    }
    while (iCnt < populationDim);
  }
  
  /** Copy the chromosomes previously created and stored in the "next"
   *  generation into the main chromsosome memory pool. Perform random
   *  mutations where appropriate.
   */
  void copyNextGenToThisGen() {
    for (int i=0; i < populationDim; i++) {
      this.chromosomes[i].copyChromGenes(this.chromNextGen[i]);
      
      //only mutate chromosomes if it is NOT the best
      if (i != this.bestFitnessChromIndex) {
        //always mutate the chromosome with the lowest fitness
        if ((i == this.worstFitnessChromIndex) || (getRandom(1.0) < mutationProb))
          doRandomMutation(i);
      }
    }
  }
  
  
  /** Display chromosome information to System.out */
  void addChromosomesToLog(int iGeneration, int iNumChromosomesToDisplay) {
    if (verbose == false)
      return;
    
    String sGen, sChrom;
    
    if (iNumChromosomesToDisplay > this.populationDim)
      iNumChromosomesToDisplay = this.chromosomeDim;
    
    //Display Chromosomes
    for (int i=0; i < iNumChromosomesToDisplay; i++) {
      this.chromosomes[i].fitness = getFitness(i);
      sGen = "" + iGeneration;
      if (sGen.length() < 2)
        sGen = sGen + " ";
      sChrom = "" + i;
      if (sChrom.length() < 2)
        sChrom = sChrom + " ";
      System.out.println("Gen " + sGen + ": Chrom" + sChrom + " = " + this.chromosomes[i].getGenesAsStr()
      + ", fitness = " + this.chromosomes[i].fitness);
    }
  }
  
  /** Take a binary string and convert it to the long integer.
   *  For example, '1101' --> 13 */
  long binaryStrToInt(String sBinary) {
    long digit, iResult = 0;
    
    int iLen = sBinary.length();
    for (int i=iLen-1; i >= 0; i--) {
      if (sBinary.charAt(i) == '1')
        digit = 1;
      else
        digit = 0;
      iResult += (digit << (iLen-i-1));
    }
    return(iResult);
  } 
}
