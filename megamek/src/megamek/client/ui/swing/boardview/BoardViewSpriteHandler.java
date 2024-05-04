/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.boardview;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.event.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a base class for SpriteHandlers that create and remove sprites in an attached BoardView.
 * Note that SpriteHandlers should not assume that the BoardView still has and displays all the
 * sprites they added to it. The BoardView may clear sprites without giving notification to the
 * SpriteHandlers. Thus, when sprites are to be renewed, SpriteHandlers should just remove all their
 * sprites from the BoardView (regardless of whether the BoardView still uses them) and create new ones.
 */
public abstract class BoardViewSpriteHandler implements GameListener {

    protected static final GUIPreferences GUIP = GUIPreferences.getInstance();

    protected final BoardView boardView;
    protected final List<Sprite> currentSprites = new ArrayList<>();

    public BoardViewSpriteHandler(BoardView boardView) {
        this.boardView = boardView;
    }

    /**
     * Removes any current sprites of this handler from the attached BoardView and clears this handler's
     * internal sprite list.
     *
     * When overriding this, call super.clear() or remember to perform clean up in the overriding method.
     */
    public void clear() {
        boardView.removeSprites(currentSprites);
        currentSprites.clear();
    }

    /**
     * Override this method to perform start up steps such as registering it as a listener (doing this in
     * the constructor is considered improper as the object is not fully constructed at that point).
     */
    public abstract void initialize();

    /**
     * Override this method to perform any clean up steps when this handler and its associated sprites are
     * no longer needed, such as removing the handler as listener.
     */
    public abstract void dispose();

    //region GameListener Methods
    @Override
    public void gamePlayerConnected(GamePlayerConnectedEvent e) { }

    @Override
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) { }

    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) { }

    @Override
    public void gamePlayerChat(GamePlayerChatEvent e) { }

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) { }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) { }

    @Override
    public void gameReport(GameReportEvent e) { }

    @Override
    public void gameEnd(GameEndEvent e) { }

    @Override
    public void gameBoardNew(GameBoardNewEvent e) { }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) { }

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent e) { }

    @Override
    public void gameMapQuery(GameMapQueryEvent e) { }

    @Override
    public void gameEntityNew(GameEntityNewEvent e) { }

    @Override
    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) { }

    @Override
    public void gameEntityRemove(GameEntityRemoveEvent e) { }

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) { }

    @Override
    public void gameNewAction(GameNewActionEvent e) { }

    @Override
    public void gameClientFeedbackRequest(GameCFREvent e) { }

    @Override
    public void gameVictory(GameVictoryEvent e) { }
    //endregion
}
