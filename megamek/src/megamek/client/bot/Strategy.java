/*
 * Strategy.java
 *
 * Created on May 19, 2002, 12:01 PM
 */

package megamek.client.bot;

import megamek.common.*;
import megamek.client.*;
import megamek.server.*;

/**
 * Container for strategy modifiers -- will be expanded upon
 * @author  Steve Hawkins
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
  
  //not yet implemented
  public double movement = 1;
  
  //where should I go?
  Coords target_coords = null;
  
  static CEntity MainTarget;
  
  static int Turns = 0;
  
}

