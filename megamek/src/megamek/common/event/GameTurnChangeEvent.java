/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.Player;

/**
 * Instances of this class are sent when Game turn changes.  This even keeps track of the player who will be taking the
 * new turn as well as the player who took the turn that triggered this event.
 */
public class GameTurnChangeEvent extends GamePlayerEvent {
    private static final long serialVersionUID = -6812056631576383917L;

    /**
     * Track the ID of the player who took the turn that triggered this even.
     */
    private int prevPlayerId;

    /**
     * @param source       The Game instance
     * @param currPlayer   The player for whom the new turn is for.
     * @param prevPlayerId The id of the player who took the turn that triggered this event.
     */
    public GameTurnChangeEvent(Object source, Player currPlayer, int prevPlayerId) {
        super(source, currPlayer);
        this.prevPlayerId = prevPlayerId;
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gameTurnChange(this);
    }

    @Override
    public String getEventName() {
        return "Turn Change";
    }

    public int getPreviousPlayerId() {
        return prevPlayerId;
    }
}
