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

import megamek.common.game.IGame;

/**
 * This Trigger can be used to turn a given sub-trigger into a one-time-only trigger. This is useful for scenarios or
 * creating triggers programmatically (e.g. in MHQ). Regardless of how often the given sub-trigger would react, this
 * trigger will remember if it ever returned true before. If it did, it will not return true a second time, and it will
 * not even call its sub-trigger again.
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

    @Override
    public String toString() {
        return trigger + " [once]";
    }
}
