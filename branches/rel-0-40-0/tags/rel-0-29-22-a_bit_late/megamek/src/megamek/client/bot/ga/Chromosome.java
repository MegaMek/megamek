package megamek.client.bot.ga;

public class Chromosome {
    public double fitness; //absolute (not relative) fitness value
    public int fitnessRank; //0 = worst fit, PopDim = best fit
    public int[] genes;

    public Chromosome(int iGenesDim) {
        genes = new int[iGenesDim];
    }
    
    public String toString() {
        return genes.toString();
    }

    public void copyChromGenes(Chromosome chromosome) {
        for (int iGene = 0; iGene < genes.length; iGene++)
            this.genes[iGene] = chromosome.genes[iGene];
    }
}
