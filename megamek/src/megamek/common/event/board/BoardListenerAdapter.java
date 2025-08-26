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


package megamek.common.event.board;

/**
 * This adapter class provides default implementations for the methods described by the <code>BoardListener</code>
 * interface.
 * <p>
 * Classes that wish to deal with <code>BoardEvent</code>s can extend this class and override only the methods which
 * they are interested in.
 * </p>
 *
 * @see BoardListener
 * @see BoardEvent
 */
public class BoardListenerAdapter implements BoardListener {

    /**
     * Sent when Board completely changed The default behavior is to do nothing.
     *
     * @param b an event containing information about the change
     */
    @Override
    public void boardNewBoard(BoardEvent b) {
    }

    /**
     * Sent when Hex on the Board changed The default behavior is to do nothing.
     *
     * @param b an event containing information about the change
     */
    @Override
    public void boardChangedHex(BoardEvent b) {
    }

    /**
     * Sent when all Hexes on the Board changed The default behavior is to do nothing.
     *
     * @param b an event containing information about the change
     */
    @Override
    public void boardChangedAllHexes(BoardEvent b) {
    }

}
