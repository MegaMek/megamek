package megamek.client.bot;

/**
 * Container for strategy modifiers
 * 
 * TODO: alot...
 */
public class Strategy {
  
  /* 0 full out retreat (damage shy)
   * +inf full out attack (damage preference)
   */
  public double attack = 1;
  
  /* Modify attacks against me based upon this.
   * i.e. how strongly do people want to attack me
   */
  public double target = 1;
  
  static CEntity MainTarget;
}