package megamek.client.bot.ga;

import com.sun.java.util.collections.Comparable;

public class Chromosome implements Comparable {
    public double fitness; //absolute fitness value
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
    
    public int compareTo(Object o) {
        double delta = fitness - ((Chromosome)o).fitness;
        return delta>0?1:delta<0?-1:0;
    }

}
