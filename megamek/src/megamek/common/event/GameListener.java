/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.event;

import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.event.board.GameBoardNewEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.event.entity.GameEntityNewEvent;
import megamek.common.event.entity.GameEntityNewOffboardEvent;
import megamek.common.event.entity.GameEntityRemoveEvent;
import megamek.common.event.player.GamePlayerChangeEvent;
import megamek.common.event.player.GamePlayerChatEvent;
import megamek.common.event.player.GamePlayerConnectedEvent;
import megamek.common.event.player.GamePlayerDisconnectedEvent;
import megamek.common.event.player.GamePlayerStrategicActionEvent;

/**
 * Classes which implement this interface provide methods that deal with the events that are generated when the Game is
 * changed.
 * <p>
 * After creating an instance of a class that implements this interface it can be added to a Game using the
 * <code>addGameListener</code> method and removed using the <code>removeGameListener</code> method. When Game is
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

    void gameVictory(PostGameResolution e);

    default void gameScriptedEvent(GameScriptedEvent event) {}

    /**
     * This event is used in SBF games when a unit (formation) changes.
     *
     */
    default void gameUnitChange(GameEvent event) {}

    default void gamePlayerStrategicAction(GamePlayerStrategicActionEvent event) {}
}
