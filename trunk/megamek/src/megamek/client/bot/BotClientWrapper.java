/*
 * BotClientWrapper.java
 *
 * Created on May 19, 2002, 11:24 AM
 */

package megamek.client.bot;


import megamek.common.*;
import megamek.client.*;
import megamek.common.actions.*;

/**
 *
 * @author  Administrator
 */
public abstract class BotClientWrapper extends BotClient {
  
  public int winner = 0;
  
  /** Create a new instance that won't call System.exit() on death */
  public BotClientWrapper(String name) {
    super(name);
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
        if (!(game.getOptions().booleanOption("double_blind")) &&
                game.getEntitiesOwnedBy(this.getLocalPlayer()) - game.getNoOfEntities() == 0) {
          this.winner = 1;
          this.die();
        }
        initMovement();
//        sendReady(true); BAD BAD BAD!
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

  public abstract void initialize();
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
    
  public void retrieveServerInfo()
  {
        super.retrieveServerInfo();
        initialize();
  }
    
}
