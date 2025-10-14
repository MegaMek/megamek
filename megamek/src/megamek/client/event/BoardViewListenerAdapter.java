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

package megamek.client.event;

/**
 * This adapter class provides default implementations for the methods described by the <code>BoardViewListener</code>
 * interface.
 * <p>
 * Classes that wish to deal with <code>BoardViewEvent</code>s can extend this class and override only the methods which
 * they are interested in.
 * </p>
 *
 * @see BoardViewListener
 * @see BoardViewEvent
 */
public class BoardViewListenerAdapter implements BoardViewListener {

    /**
     * Sent when user clicks, double clicks or drags hex. The default behavior is to do nothing.
     *
     * @param b an event
     */
    @Override
    public void hexMoused(BoardViewEvent b) {
    }

    /**
     * Sent when BoardView 'cursor' is set to Hex. The default behavior is to do nothing.
     *
     * @param b an event
     */
    @Override
    public void hexCursor(BoardViewEvent b) {
    }

    /**
     * Sent when BoardView 'cursor' is set to Hex. The default behavior is to do nothing.
     *
     * @param b an event
     */
    @Override
    public void boardHexHighlighted(BoardViewEvent b) {
    }

    /**
     * Sent when Hex is selected. The default behavior is to do nothing.
     *
     * @param b an event
     */
    @Override
    public void hexSelected(BoardViewEvent b) {
    }

    /**
     * Sent when firstLOS is set. The default behavior is to do nothing.
     *
     * @param b an event
     */
    @Override
    public void firstLOSHex(BoardViewEvent b) {
    }

    /**
     * Sent when secondLOS is set. The default behavior is to do nothing.
     *
     * @param b an event
     */
    @Override
    public void secondLOSHex(BoardViewEvent b) {
    }

    /**
     * Sent when moving units is finished. The default behavior is to do nothing.
     *
     * @param b an event
     */
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
    }

    /**
     * Sent when Unit is selected. The default behavior is to do nothing.
     *
     * @param b an event
     */
    @Override
    public void unitSelected(BoardViewEvent b) {
    }

}
