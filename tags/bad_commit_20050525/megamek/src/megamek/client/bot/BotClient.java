/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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
import java.util.Vector;

import megamek.client.Client;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.GameTurn;
import megamek.common.IGame;
import megamek.common.Minefield;
import megamek.common.MovePath;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GameReportEvent;
import megamek.common.event.GameTurnChangeEvent;

import com.sun.java.util.collections.ArrayList;

public abstract class BotClient extends Client {

    public BotClient(String playerName, String host, int port) {
        super(playerName, host, port);
        game.addGameListener(new GameListenerAdapter(){

            public void gamePlayerChat(GamePlayerChatEvent e) {
                processChat(e);
            }

            public void gameTurnChange(GameTurnChangeEvent e) {
                if (isMyTurn()) {
                    calculateMyTurn();
                }
            }

            public void gameReport(GameReportEvent e) {
                if (game.getPhase() == IGame.PHASE_INITIATIVE) {
                    //Opponent has used tactical genius, must press
                    // "Done" again to advance past initiative report.
                    sendDone(true);
                }
            }
            
        });
    }

    BotConfiguration config = new BotConfiguration();

    public abstract void initialize();
    protected abstract void processChat(GamePlayerChatEvent ge);
    protected abstract void initMovement();
    protected abstract void initFiring();
    protected abstract MovePath calculateMoveTurn();
    protected abstract void calculateFiringTurn();
    protected abstract void calculateDeployment();
    protected abstract PhysicalOption calculatePhysicalTurn();
    protected abstract MovePath continueMovementFor(Entity entity);
    protected abstract Vector calculateMinefieldDeployment();

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
                case IGame.PHASE_LOUNGE :
                    sendChat(Messages.getString("BotClient.Hi")); //$NON-NLS-1$
                    break;
                case IGame.PHASE_DEPLOYMENT :
                    initialize();
                    break;
                case IGame.PHASE_MOVEMENT :
                    if (game.getEntitiesOwnedBy(this.getLocalPlayer()) == 0) {
                        sendChat(Messages.getString("BotClient.HowAbout")); //$NON-NLS-1$
                        this.die();
                    }
                    if (!(game.getOptions().booleanOption("double_blind")) //$NON-NLS-1$
                        && game.getEntitiesOwnedBy(this.getLocalPlayer()) - game.getNoOfEntities() == 0) {
                        this.die();
                    }
                    initMovement();
                    break;
                case IGame.PHASE_FIRING :
                    initFiring();
                    break;
                case IGame.PHASE_PHYSICAL :
                    break;
                case IGame.PHASE_INITIATIVE :
                case IGame.PHASE_MOVEMENT_REPORT :
                case IGame.PHASE_FIRING_REPORT :
                case IGame.PHASE_END :/*
                case IGame.PHASE_OFFBOARD_REPORT :
                case IGame.PHASE_SET_ARTYAUTOHITHEXES:
                case IGame.PHASE_OFFBOARD:
                case IGame.PHASE_TARGETING:*/
                    sendDone(true);
                    break;
                case IGame.PHASE_VICTORY :
                    break;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    protected void calculateMyTurn() {
        try {
            if (game.getPhase() == IGame.PHASE_MOVEMENT) {
                MovePath mp = null;
                if (game.getTurn() instanceof GameTurn.SpecificEntityTurn) {
                    GameTurn.SpecificEntityTurn turn = (GameTurn.SpecificEntityTurn) game.getTurn();
                    Entity mustMove = game.getEntity(turn.getEntityNum());
                    mp = continueMovementFor(mustMove);
                } else {
                    mp = calculateMoveTurn();
                }
                moveEntity(mp.getEntity().getId(), mp);
            } else if (game.getPhase() == IGame.PHASE_FIRING) {
                if (game.getTurn() instanceof GameTurn.SpecificEntityTurn) {
                    GameTurn.SpecificEntityTurn turn = (GameTurn.SpecificEntityTurn) game.getTurn();
                    MovePath mp = continueMovementFor(game.getEntity(turn.getEntityNum()));
                    moveEntity(mp.getEntity().getId(), mp);
                }
                calculateFiringTurn();
            } else if (game.getPhase() == IGame.PHASE_PHYSICAL) {
                PhysicalOption po = calculatePhysicalTurn();
                // Bug #1072137: don't crash if the bot can't find a physical.
                if (null != po) {
                    sendAttackData(po.attacker.getId(), po.getVector());
                }
                else {
                    // Send a "no attack" to clear the game turn, if any.
                    sendAttackData( getLocalPlayer().getId(), new Vector(0) );
                }
            } else if (game.getPhase() == IGame.PHASE_DEPLOYMENT) {
                calculateDeployment();
            } else if (game.getPhase() == IGame.PHASE_DEPLOY_MINEFIELDS) {
                Vector mines = calculateMinefieldDeployment();
                for (int i = 0; i < mines.size(); i++) {
                    game.addMinefield((Minefield)mines.elementAt(i));
                }
                sendDeployMinefields(mines);
                sendPlayerInfo();
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
        if (game.getBoard().isLegalDeployment(c, this.getLocalPlayer()) && game.getFirstEntity(c) == null) {
            return c;
        }

        // check the surrounding coords
        for (int x = 0; x < 6; x++) {
            Coords c2 = c.translated(x);
            if (game.getBoard().isLegalDeployment(c2, this.getLocalPlayer()) && game.getFirstEntity(c2) == null) {
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
                return new Coords(game.getBoard().getWidth() / 2, game.getBoard().getHeight() / 2);
            case 1 :
                return new Coords(1, 1);
            case 2 :
                return new Coords(game.getBoard().getWidth() / 2, 1);
            case 3 :
                return new Coords(game.getBoard().getWidth() - 2, 1);
            case 4 :
                return new Coords(game.getBoard().getWidth() - 2, game.getBoard().getHeight() / 2);
            case 5 :
                return new Coords(game.getBoard().getWidth() - 2, game.getBoard().getHeight() - 2);
            case 6 :
                return new Coords(game.getBoard().getWidth() / 2, game.getBoard().getHeight() - 2);
            case 7 :
                return new Coords(1, game.getBoard().getHeight() - 2);
            case 8 :
                return new Coords(1, game.getBoard().getHeight() / 2);
        }
    }

    public void retrieveServerInfo() {
        super.retrieveServerInfo();
        initialize();
    }
}
