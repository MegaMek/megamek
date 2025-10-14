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

/**
 * This adapter class provides default implementations for the methods described by the <code>GameListener</code>
 * interface.
 * <p>
 * Classes that wish to deal with <code>GamedEvent</code>s can extend this class and override only the methods which
 * they are interested in.
 * </p>
 *
 * @see GameListener
 * @see GameEvent
 */
public class GameListenerAdapter implements GameListener {

    @Override
    public void gamePlayerConnected(GamePlayerConnectedEvent e) {
    }

    @Override
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
    }

    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) {
    }

    @Override
    public void gamePlayerChat(GamePlayerChatEvent e) {
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
    }

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
    }

    @Override
    public void gameReport(GameReportEvent e) {
    }

    @Override
    public void gameEnd(GameEndEvent e) {
    }

    @Override
    public void gameBoardNew(GameBoardNewEvent e) {
    }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) {
    }

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent e) {
    }

    @Override
    public void gameMapQuery(GameMapQueryEvent e) {
    }

    @Override
    public void gameEntityNew(GameEntityNewEvent e) {
    }

    @Override
    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
    }

    @Override
    public void gameEntityRemove(GameEntityRemoveEvent e) {
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) {
    }

    @Override
    public void gameNewAction(GameNewActionEvent e) {
    }

    @Override
    public void gameClientFeedbackRequest(GameCFREvent evt) {
    }

    @Override
    public void gameVictory(PostGameResolution e) {
    }

}
