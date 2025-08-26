/*
 * Copyright (c) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2004-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.util;

import java.util.EventListener;

/**
 * This interface represents an event listener that can be "distracted" to ignore any event notifications.
 *
 * @author James Damour
 */
public interface Distractable extends EventListener {

    /**
     * @return true if the listener is currently distracted, i.e. ignoring events.
     */
    boolean isIgnoringEvents();

    /**
     * Specify if the listener should be distracted. An event that occurs while the listener is distracted is completely
     * ignored, i.e. it is not processed, neither when it arrives nor at any later point.
     *
     * @param distracted true if the listener should ignore events, false if the listener should pay attention
     */
    void setIgnoringEvents(boolean distracted);

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    void removeAllListeners();
}
