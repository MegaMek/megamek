/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 */
package megamek.client.bot;

import java.util.Enumeration;

import com.sun.java.util.collections.ArrayList;

import megamek.client.AlertDialog;
import megamek.client.Client;
import megamek.client.DeploymentDisplay;
import megamek.client.FiringDisplay;
import megamek.client.GameEvent;
import megamek.client.MovementDisplay;
import megamek.client.PhysicalDisplay;
import megamek.client.ConfirmDialog;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.GameTurn;
import megamek.common.MovePath;
import megamek.common.Packet;
import megamek.common.Settings;
import megamek.common.actions.AttackAction;
import megamek.common.actions.FlipArmsAction;
import megamek.common.actions.TorsoTwistAction;

public abstract class BotClient extends Client {

    public BotClient(String playerName) {
        super(playerName);
    }

    BotConfiguration config = new BotConfiguration();

    public abstract void initialize();
    protected abstract void processChat(GameEvent ge);
    protected abstract void initMovement();
    protected abstract void initFiring();
    protected abstract MovePath calculateMoveTurn();
    protected abstract void calculateFiringTurn();
    protected abstract void calculateDeployment();
    protected abstract PhysicalOption calculatePhysicalTurn();
    protected abstract MovePath continueMovementFor(Entity entity);

    public ArrayList getEntitiesOwned() {
        ArrayList result = new ArrayList();
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity) i.nextElement();
            if (entity.getOwner().equals(this.getLocalPlayer())) {
                result.add(entity);
            }
        }
        return result;
    }

    public ArrayList getEnemyEntities() {
        ArrayList result = new ArrayList();
        for (Enumeration i = game.getEntities(); i.hasMoreElements();) {
            Entity entity = (Entity) i.nextElement();
            if (entity.getOwner().isEnemyOf(this.getLocalPlayer())) {
                result.add(entity);
            }
        }
        return result;
    }

    //TODO: move initMovement to be called on phase end
    protected void changePhase(int phase) {
        super.changePhase(phase);

        try {
            switch (phase) {
                case Game.PHASE_LOUNGE :
                    notifyOfBot();
                    break;
                case Game.PHASE_DEPLOYMENT :
                    initialize();
                    break;
                case Game.PHASE_MOVEMENT :
                    if (game.getEntitiesOwnedBy(this.getLocalPlayer()) == 0) {
                        sendChat("How about a nice game of chess?");
                        this.die();
                    }
                    if (!(game.getOptions().booleanOption("double_blind"))
                        && game.getEntitiesOwnedBy(this.getLocalPlayer()) - game.getNoOfEntities() == 0) {
                        this.die();
                    }
                    initMovement();
                    break;
                case Game.PHASE_FIRING :
                    initFiring();
                    break;
                case Game.PHASE_PHYSICAL :
                    break;
                case Game.PHASE_INITIATIVE :
                case Game.PHASE_MOVEMENT_REPORT :
                case Game.PHASE_FIRING_REPORT :
                case Game.PHASE_END :
                    sendDone(true);
                case Game.PHASE_VICTORY :
                    break;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected void notifyOfBot() {
        if (true==Settings.nagForBotReadme) {
            String title = "Please read the ai-readme.txt";
            String body = 
                "The bot does not work with all units or game options.\n"+
                "Please read the ai-readme.txt file before using the bot.\n"+
                " \nWould you like to read the AI documentation now?\n";
            ConfirmDialog confirm = new ConfirmDialog(frame,title,body,true);
            confirm.show();

            if ( !confirm.getShowAgain() ) {
                Settings.nagForBotReadme = false;
                Settings.save();
            };

            if ( confirm.getAnswer() ) {
                megamek.MegaMek.showHelp(this.frame, "ai-readme.txt").show();
            };
        };
        sendChat("Hi, I'm a bot client!");
    }

    protected void processGameEvent(GameEvent ge) {
        super.processGameEvent(ge);

        switch (ge.getType()) {
            case GameEvent.GAME_PLAYER_CHAT :
                processChat(ge);
                break;
            case GameEvent.GAME_PLAYER_STATUSCHANGE :
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
                break;
        }
    }

    protected void calculateMyTurn() {
        try {
            if (curPanel instanceof MovementDisplay) {
				MovePath mp = null;
                if (game.getTurn() instanceof GameTurn.SpecificEntityTurn) {
                    GameTurn.SpecificEntityTurn turn = (GameTurn.SpecificEntityTurn) game.getTurn();
                    Entity mustMove = game.getEntity(turn.getEntityNum());
                    mp = continueMovementFor(mustMove);
                } else {
                    mp = calculateMoveTurn();
                }
                moveEntity(mp.getEntity().getId(), mp);
            } else if (curPanel instanceof FiringDisplay) {
                if (game.getTurn() instanceof GameTurn.SpecificEntityTurn) {
                    GameTurn.SpecificEntityTurn turn = (GameTurn.SpecificEntityTurn) game.getTurn();
                    MovePath mp = continueMovementFor(game.getEntity(turn.getEntityNum()));
                    moveEntity(mp.getEntity().getId(), mp);
                }
                calculateFiringTurn();
            } else if (curPanel instanceof PhysicalDisplay) {
                PhysicalOption po = calculatePhysicalTurn();
                sendAttackData(po.attacker.getId(), po.getVector());
            } else if (curPanel instanceof DeploymentDisplay) {
                calculateDeployment();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
	 * Gets valid & empty starting coords around the specified point
	 */
    protected Coords getCoordsAround(Coords c) {
        // check the requested coords
        if (game.board.isLegalDeployment(c, this.getLocalPlayer()) && game.getFirstEntity(c) == null) {
            return c;
        }

        // check the surrounding coords
        for (int x = 0; x < 6; x++) {
            Coords c2 = c.translated(x);
            if (game.board.isLegalDeployment(c2, this.getLocalPlayer()) && game.getFirstEntity(c2) == null) {
                return c2;
            }
        }

        // recurse in a random direction
        return getCoordsAround(c.translated(Compute.randomInt(6)));
    }

    protected Coords getStartingCoords() {
        switch (getLocalPlayer().getStartingPos()) {
            default :
            case 0 :
                return new Coords(game.board.width / 2, game.board.height / 2);
            case 1 :
                return new Coords(1, 1);
            case 2 :
                return new Coords(game.board.width / 2, 1);
            case 3 :
                return new Coords(game.board.width - 2, 1);
            case 4 :
                return new Coords(game.board.width - 2, game.board.height / 2);
            case 5 :
                return new Coords(game.board.width - 2, game.board.height - 2);
            case 6 :
                return new Coords(game.board.width / 2, game.board.height - 2);
            case 7 :
                return new Coords(1, game.board.height - 2);
            case 8 :
                return new Coords(1, game.board.height / 2);
        }
    }

    protected void receiveAttack(Packet c) {
        Object o = c.getObject(0);
        if (o instanceof TorsoTwistAction) {
            TorsoTwistAction tta = (TorsoTwistAction) o;
            if (game.getEntity(tta.getEntityId()) != null) {
                game.getEntity(tta.getEntityId()).setSecondaryFacing(tta.getFacing());
            }
        } else if (o instanceof FlipArmsAction) {
            FlipArmsAction faa = (FlipArmsAction) o;
            if (game.getEntity(faa.getEntityId()) != null) {
                game.getEntity(faa.getEntityId()).setArmsFlipped(faa.getIsFlipped());
            }
        } else if (o instanceof AttackAction) {
            //
        }
    }

    public void retrieveServerInfo() {
        super.retrieveServerInfo();
        initialize();
    }
}
