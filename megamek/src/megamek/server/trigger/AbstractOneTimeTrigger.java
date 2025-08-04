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

package megamek.server.trigger;

import megamek.common.IGame;

/**
 * This is a base class for Triggers that should only ever react once, regardless of how often its prerequisites are
 * satisfied.
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
     * Regardless of how these are defined, this trigger will remember if it ever returned true before. If it did, it
     * will not return true a second time and it will not even call this method again.
     *
     * @param event The type of event that caused the trigger to be called
     *
     * @return True when this Trigger considers itself triggered, false otherwise
     *
     * @see #isTriggered(IGame, TriggerSituation)
     */
    protected abstract boolean isTriggeredImpl(IGame game, TriggerSituation event);
}
