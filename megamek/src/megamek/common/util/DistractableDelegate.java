/*
 * Copyright (c) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.Serializable;

/**
 * This class implements the Distractable interface. It is intended to be the underlying implementation for any class
 * that implements the interface.
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
    public void removeAllListeners() {}
}
