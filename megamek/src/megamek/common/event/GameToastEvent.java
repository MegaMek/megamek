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
package megamek.common.event;

/**
 * A game event raised when the server wants the client to show a transient toast notification on the board view - for
 * example, "Fortification complete" when Trench/Fieldworks Engineers finish a fortified hex.
 *
 * <p>The severity is expressed with the layer-neutral {@link Level} enum so that this common event carries no
 * dependency on the client UI's toast rendering type; the client maps {@link Level} to its own toast styling when it
 * shows the message.</p>
 */
public class GameToastEvent extends GameEvent {

    /**
     * Layer-neutral severity for a toast, mirrored by the client UI's own toast styling. INFO is routine, SUCCESS marks
     * a completed action, WARNING flags a rejected or risky situation, and ERROR marks a failure.
     */
    public enum Level {
        INFO, SUCCESS, WARNING, ERROR
    }

    private final Level level;
    private final String message;
    private final int entityId;

    /**
     * @param source   the event source (usually the client)
     * @param level    the severity of the toast
     * @param message  the (already localized) text to display
     * @param entityId the id of the acting unit whose icon should accompany the toast, or
     *                 {@link megamek.common.units.Entity#NONE} for a text-only toast
     */
    public GameToastEvent(Object source, Level level, String message, int entityId) {
        super(source);
        this.level = level;
        this.message = message;
        this.entityId = entityId;
    }

    public Level level() {
        return level;
    }

    public String message() {
        return message;
    }

    public int entityId() {
        return entityId;
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gameToast(this);
    }

    @Override
    public String getEventName() {
        return "Toast";
    }
}
