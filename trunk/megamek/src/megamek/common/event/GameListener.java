/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common.event;

/**
 * Classes which implement this interface provide methods that deal with the
 * events that are generated when the Game is changed.
 * <p>
 * After creating an instance of a class that implements this interface it can
 * be added to a Game using the <code>addGameListener</code> method and
 * removed using the <code>removeGameListener</code> method. When Game is
 * changed the appropriate method will be invoked.
 * </p>
 * 
 * @see GameListenerAdapter
 * @see GameEvent
 */
public interface GameListener extends java.util.EventListener {
    public void gamePlayerConnected(GamePlayerConnectedEvent e);

    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e);

    public void gamePlayerChange(GamePlayerChangeEvent e);

    public void gamePlayerChat(GamePlayerChatEvent e);

    public void gameTurnChange(GameTurnChangeEvent e);

    public void gamePhaseChange(GamePhaseChangeEvent e);

    public void gameReport(GameReportEvent e);

    public void gameEnd(GameEndEvent e);

    public void gameBoardNew(GameBoardNewEvent e);

    public void gameBoardChanged(GameBoardChangeEvent e);

    public void gameSettingsChange(GameSettingsChangeEvent e);

    public void gameMapQuery(GameMapQueryEvent e);

    public void gameEntityNew(GameEntityNewEvent e);

    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e);

    public void gameEntityRemove(GameEntityRemoveEvent e);

    public void gameEntityChange(GameEntityChangeEvent e);

    public void gameNewAction(GameNewActionEvent e);
}
