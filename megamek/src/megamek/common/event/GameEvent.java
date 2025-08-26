/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.Serial;

/**
 * Instances of descendant classes are sent as a result of Game change
 *
 * @see GameListener
 */
public abstract class GameEvent extends java.util.EventObject {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -6199335692173395907L;

    /**
     * Construct game event
     */
    public GameEvent(Object source) {
        super(source);
    }

    /**
     * Sub-classed events implement this method to call their specific method on a GameListener instance that their
     * event has been fired.
     *
     * @param gl GameListener recipient.
     */
    abstract public void fireEvent(GameListener gl);

    abstract public String getEventName();

    @Override
    public String toString() {
        return getEventName() + " game event ";
    }
}
