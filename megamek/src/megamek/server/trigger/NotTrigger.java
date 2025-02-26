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
package megamek.server.trigger;

import megamek.common.IGame;

/**
 * This "Trigger" is used to invert another Trigger. It reacts (returns true) when the given subtrigger
 * returns false.
 */
public class NotTrigger implements Trigger {

    private final Trigger trigger;

    public NotTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        return !trigger.isTriggered(game, event);
    }

    @Override
    public String toString() {
        return "[not] " + trigger;
    }
}
