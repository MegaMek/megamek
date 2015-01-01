package megamek.client.bot.ga;

public class ChromVector extends Chromosome {
  
  public int[] genes;
  
  public ChromVector(int iGenesDim) {
    genes = new int[iGenesDim];
  }
  
  public String getGenesAsStr() {
    String sGenes = "";
    for (int i=0; i < genes.length - 1; i++)
      sGenes += this.genes[i] + ",";
    sGenes += this.genes[genes.length - 1];
    return(sGenes);
  }
  
  public void copyChromGenes(Chromosome chromosome) {
    ChromVector chromVector = (ChromVector)chromosome;
    
    for (int iGene = 0; iGene < genes.length; iGene++)
      this.genes[iGene] = chromVector.genes[iGene];
  }
  
}

