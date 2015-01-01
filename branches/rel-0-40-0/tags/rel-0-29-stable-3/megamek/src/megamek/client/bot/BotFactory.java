/*
 * BotFactory.java
 *
 * Created on April 30, 2002, 4:30 PM
 */

package megamek.client.bot;

import megamek.client.*; 

import java.awt.Frame;

/**
 *
 * @author  Administrator
 */
public class BotFactory {
  public static final int DEFAULT = 0;
  public static final int TEST = 1;
  public static final int HUMAN = 2;
  
  /** Creates a new instance of BotFactory */
  public BotFactory() {
  }
  
  public static Client getBot(int type, Frame frame, String name) {
    switch (type) {
      case TEST:
        return new TestBot(frame,name);
      case HUMAN:
        return new Client(frame,name);
      default:
        return new BotClient(frame, name);
    }
  }
  
    public static Client getBot(int type, String name) {
        switch (type) {
            case TEST :
                return new TestBot(name);
            case HUMAN :
                return new Client(name);
            default :
                return new BotClient(name);
        }
    }
  
}
