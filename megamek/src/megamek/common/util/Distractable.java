/*
 * Copyright (c) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2018, 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.util;

import java.util.EventListener;

/**
 * This interface represents an event listener that can be "distracted" to ignore any event
 * notifications.
 *
 * @author James Damour
 */
public interface Distractable extends EventListener {

    /**
     * @return true if the listener is currently distracted, i.e. ignoring events.
     */
    boolean isIgnoringEvents();

    /**
     * Specify if the listener should be distracted. An event that occurs while the listener is distracted
     * is completely ignored, i.e. it is not processed, neither when it arrives nor at any later point.
     *
     * @param distracted true if the listener should ignore events, false if the listener should pay attention
     */
    void setIgnoringEvents(boolean distracted);

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    void removeAllListeners();
}
