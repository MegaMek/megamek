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
 * This is a base class for Triggers that should only ever react once, regardless of how often its
 * prerequisites are satisfied.
 */
public abstract class AbstractOneTimeTrigger implements Trigger {

    private boolean wasTriggered = false;

    @Override
    public final boolean isTriggered(IGame game, TriggerSituation event) {
        if (wasTriggered) {
            return false;
        } else if (isTriggeredImpl(game, event)) {
            wasTriggered = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Override this to define the trigger conditions as for {@link Trigger#isTriggered(IGame, TriggerSituation)}.
     * Regardless of how these are defined, this trigger will remember if it ever returned true before. If it
     * did, it will not return true a second time and it will not even call this method again.
     *
     * @param event The type of event that caused the trigger to be called
     * @return True when this Trigger considers itself triggered, false otherwise
     * @see #isTriggered(IGame, TriggerSituation)
     */
    protected abstract boolean isTriggeredImpl(IGame game, TriggerSituation event);
}
