package megamek.client.bot.ga;

/** The valid genetic mating crossover types */
public interface Crossover
{
  public static final int ctOnePoint = 0;
  public static final int ctTwoPoint = 1;
  public static final int ctUniform = 2;
  public static final int ctRoulette = 3;
}
