/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.Coords;

/**
 * Instances of this class are sent as a result of Board change
 *
 * @see BoardListener
 */
public class BoardEvent extends java.util.EventObject {
    /**
     *
     */
    private static final long serialVersionUID = 6895134212472497607L;
    public static final int BOARD_NEW_BOARD = 0;
    public static final int BOARD_CHANGED_HEX = 1;
    public static final int BOARD_CHANGED_ALL_HEXES = 2;

    private Coords coords;
    private int type;

    public BoardEvent(Object source, Coords coords, int type) {
        super(source);
        this.coords = coords;
        this.type = type;
    }

    /**
     * @return the type of event that this is
     */
    public int getType() {
        return type;
    }

    /**
     * @return the coordinate where this event occurred, if applicable;
     *       <code>null</code> otherwise.
     */
    public Coords getCoords() {
        return coords;
    }
}
