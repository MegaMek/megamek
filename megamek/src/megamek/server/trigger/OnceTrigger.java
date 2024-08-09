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
 * This Trigger can be used to turn a given sub-trigger into a one-time-only trigger. This is useful for
 * scenarios or creating triggers programmatically (e.g. in MHQ). Regardless of how often the given sub-trigger
 * would react, this trigger will remember if it ever returned true before. If it
 * did, it will not return true a second time and it will not even call its sub-trigger again.
 */
public final class OnceTrigger implements Trigger {

    private final Trigger trigger;
    private boolean wasTriggered = false;

    public OnceTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        if (wasTriggered) {
            return false;
        } else if (trigger.isTriggered(game, event)) {
            wasTriggered = true;
            return true;
        } else {
            return false;
        }
    }
}
