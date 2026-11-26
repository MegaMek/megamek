/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.event.player;

import java.io.Serial;

import megamek.common.event.GameEvent;
import megamek.common.event.GameListener;

/**
 * Raised on a client when another player pings it with {@code /ping}, so the client can play its ping sound and show
 * who pinged it.
 */
public class GamePlayerPingEvent extends GameEvent {

    @Serial
    private static final long serialVersionUID = 4127094312596733181L;

    private final int senderId;
    private final boolean toEveryone;

    /**
     * @param source     the event source (usually the client)
     * @param senderId   the player id of whoever sent the ping
     * @param toEveryone {@code true} when the ping went to every player rather than only to this one
     */
    public GamePlayerPingEvent(Object source, int senderId, boolean toEveryone) {
        super(source);
        this.senderId = senderId;
        this.toEveryone = toEveryone;
    }

    /**
     * @return the player id of whoever sent the ping
     */
    public int senderId() {
        return senderId;
    }

    /**
     * @return {@code true} when the ping went to every player rather than only to this one
     */
    public boolean isToEveryone() {
        return toEveryone;
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gamePlayerPing(this);
    }

    @Override
    public String getEventName() {
        return "Player Ping";
    }
}
