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

import java.io.Serializable;

/**
 * This class implements the Distractable interface. It is intended to be the underlying implementation
 * for any class that implements the interface.
 *
 * @author James Damour
 */
public class DistractableDelegate implements Distractable, Serializable {

    /** The current state of distraction. */
    private boolean isDistracted;

    /**
     * Creates a delegate object to handle the distraction state of another object.
     */
    public DistractableDelegate() {
        isDistracted = false;
    }

    @Override
    public boolean isIgnoringEvents() {
        return isDistracted;
    }

    @Override
    public void setIgnoringEvents(boolean distracted) {
        isDistracted = distracted;
    }

    @Override
    public void removeAllListeners() { }
}
