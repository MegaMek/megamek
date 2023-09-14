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
    void gamePlayerConnected(GamePlayerConnectedEvent e);

    void gamePlayerDisconnected(GamePlayerDisconnectedEvent e);

    void gamePlayerChange(GamePlayerChangeEvent e);

    void gamePlayerChat(GamePlayerChatEvent e);

    void gameTurnChange(GameTurnChangeEvent e);

    void gamePhaseChange(GamePhaseChangeEvent e);

    void gameReport(GameReportEvent e);

    void gameEnd(GameEndEvent e);

    void gameBoardNew(GameBoardNewEvent e);

    void gameBoardChanged(GameBoardChangeEvent e);

    void gameSettingsChange(GameSettingsChangeEvent e);

    void gameMapQuery(GameMapQueryEvent e);

    void gameEntityNew(GameEntityNewEvent e);

    void gameEntityNewOffboard(GameEntityNewOffboardEvent e);

    void gameEntityRemove(GameEntityRemoveEvent e);

    void gameEntityChange(GameEntityChangeEvent e);

    void gameNewAction(GameNewActionEvent e);
    
    void gameClientFeedbackRequest(GameCFREvent e);
    
    void gameVictory(GameVictoryEvent e);

    void gameUnitDied(GameUnitDiedEvent e);
}
