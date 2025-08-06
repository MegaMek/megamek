/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.clientGUI.boardview.spriteHandler;

import java.util.ArrayList;
import java.util.Collection;

import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.sprite.Sprite;
import megamek.common.event.*;

/**
 * This is a base class for SpriteHandlers that create and remove sprites in an attached BoardView. Note that
 * SpriteHandlers should not assume that the BoardView still has and displays all the sprites they added to it. The
 * BoardView may clear sprites without giving notification to the SpriteHandlers. Thus, when sprites are to be renewed,
 * SpriteHandlers should just remove all their sprites from the BoardView (regardless of whether the BoardView still
 * uses them) and create new ones.
 */
public abstract class BoardViewSpriteHandler implements GameListener {

    protected static final GUIPreferences GUIP = GUIPreferences.getInstance();

    protected final AbstractClientGUI clientGUI;
    protected final Collection<Sprite> currentSprites = new ArrayList<>();

    public BoardViewSpriteHandler(AbstractClientGUI clientGUI) {
        this.clientGUI = clientGUI;
    }

    /**
     * Removes any current sprites of this handler from all BoardViews and clears this handler's internal sprite list.
     * <p>
     * When overriding this, call super.clear() or remember to perform clean up in the overriding method.
     */
    public void clear() {
        clientGUI.boardViews().forEach(boardView -> boardView.removeSprites(currentSprites));
        currentSprites.clear();
    }

    /**
     * Override this method to perform start up steps such as registering it as a listener (doing this in the
     * constructor is considered improper as the object is not fully constructed at that point).
     */
    public abstract void initialize();

    /**
     * Override this method to perform any clean up steps when this handler and its associated sprites are no longer
     * needed, such as removing the handler as listener.
     */
    public abstract void dispose();

    //region GameListener Methods
    @Override
    public void gamePlayerConnected(GamePlayerConnectedEvent e) {}

    @Override
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {}

    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) {}

    @Override
    public void gamePlayerChat(GamePlayerChatEvent e) {}

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {}

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {}

    @Override
    public void gameReport(GameReportEvent e) {}

    @Override
    public void gameEnd(GameEndEvent e) {}

    @Override
    public void gameBoardNew(GameBoardNewEvent e) {}

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) {}

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent e) {}

    @Override
    public void gameMapQuery(GameMapQueryEvent e) {}

    @Override
    public void gameEntityNew(GameEntityNewEvent e) {}

    @Override
    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {}

    @Override
    public void gameEntityRemove(GameEntityRemoveEvent e) {}

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) {}

    @Override
    public void gameNewAction(GameNewActionEvent e) {}

    @Override
    public void gameClientFeedbackRequest(GameCFREvent e) {}

    @Override
    public void gameVictory(PostGameResolution e) {}
    //endregion
}
