/*
 * BotClientWrapper.java
 *
 * Created on May 19, 2002, 11:24 AM
 */

package megamek.client.bot;


import java.awt.Frame;
import com.sun.java.util.collections.*;
import java.io.*;

import megamek.*;
import megamek.common.*;
import megamek.client.*;
import megamek.common.actions.*;

/**
 *
 * @author  Administrator
 */
public abstract class BotClientWrapper extends BotClient {
  
  public int winner = 0;
  
  /** Creates a new instance of TestBot */
  public BotClientWrapper(Frame frame, String name) {
    super(frame, name);
  }
  /** Will need to pick up events here later
   */
  protected void changePhase(int phase) {
    super.changePhase(phase);
    switch(phase) {
      case Game.PHASE_LOUNGE :
        initialize();
        break;
      case Game.PHASE_MOVEMENT :
        if (game.getEntitiesOwnedBy(this.getLocalPlayer()) == 0) {
          this.winner = -1;
          sendChat("How about a nice game of chess?");
          this.die();
        }
        if (game.getEntitiesOwnedBy(this.getLocalPlayer()) - game.getNoOfEntities() == 0) {
          this.winner = 1;
          this.die();
        }
        initMovement();
        sendReady(true);
        break;
      case Game.PHASE_FIRING :
        initFiring();
        break;
      case Game.PHASE_PHYSICAL :
        break;
      case Game.PHASE_FIRING_REPORT :
      case Game.PHASE_END :
      case Game.PHASE_MOVEMENT_REPORT :
      case Game.PHASE_INITIATIVE :
        break;
    }
  }
  
  protected void processGameEvent(GameEvent ge) {
    super.processGameEvent(ge);
    
    switch(ge.getType()) {
      case GameEvent.GAME_PLAYER_CHAT :
        this.processChat(ge);
        break;
    /*case GameEvent.GAME_PLAYER_STATUSCHANGE :
        break;
      case GameEvent.GAME_PHASE_CHANGE :
        break;
      case GameEvent.GAME_TURN_CHANGE :
        if (isMyTurn()) {
          calculateMyTurn();
        }
        break;
      case GameEvent.GAME_NEW_ENTITIES :
        break;
      case GameEvent.GAME_NEW_SETTINGS :
        break;*/
    }
  }

  protected abstract void initialize();
  protected abstract void initMovement();
  protected abstract void initFiring();
  protected abstract void processChat(GameEvent ge);
  
  protected void receiveAttack(Packet c) {
        Object o = c.getObject(0);
        if (o instanceof TorsoTwistAction) {
            TorsoTwistAction tta = (TorsoTwistAction)o;
            if (game.getEntity(tta.getEntityId()) != null) {
            game.getEntity(tta.getEntityId()).setSecondaryFacing(tta.getFacing());
            }
        } else if (o instanceof FlipArmsAction) {
            FlipArmsAction faa = (FlipArmsAction)o;
            if (game.getEntity(faa.getEntityId()) != null) {
            game.getEntity(faa.getEntityId()).setArmsFlipped(faa.getIsFlipped());
            }
        } else if (o instanceof AttackAction) {
            //bv.addAttack((AttackAction)o);
        }
    }
}
