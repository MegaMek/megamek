package megamek.client.bot.ga;

/**
  Chromosome is the base class for all chromosomes. It defines each
  chromosome's genes, fitness, fitness rank, and provides simple methods
  for copying and returning chromosome values as strings.

  ChromString and ChromFloat both extend Chromosome and model individual
  candidate solutions. You will probably never need to subclass these classes.
*/


/** abstract basetype for all chromosomes */
public abstract class Chromosome
{
  public double fitness;   //absolute (not relative) fitness value
  public int fitnessRank;  //0 = worst fit, PopDim = best fit
  public abstract String getGenesAsStr();
  public abstract void copyChromGenes(Chromosome chromosome);
}

