/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.orders;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;

/**
 * What a unit does at one waypoint of its route: the way it faces on arrival and how many turns it holds there before
 * moving on. A waypoint with no hold is passed through; its facing applies only if the unit ends a turn on it.
 *
 * <p>Example: {@code 1706/NE/2} in a route order - arrive at 1706, face northeast, hold two full turns after the
 * arrival turn, then move on to the next waypoint.</p>
 *
 * <p>This is a plain class and not a record on purpose: the save format uses XStream, which cannot read records
 * without a custom converter.</p>
 */
public final class WaypointOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 6203851840318724119L;

    /** A waypoint the unit passes through, facing as the bot decides. */
    public static final WaypointOrder PASS_THROUGH = new WaypointOrder(UnitOrders.FACING_AUTO, 0);

    /** The letter codes for the facings 0-5, as a route order writes them. */
    private static final List<String> FACING_CODES = List.of("N", "NE", "SE", "S", "SW", "NW");

    /** The code for a facing left to the bot. */
    private static final String AUTO_CODE = "A";

    private final int facing;
    private final int holdTurns;

    /**
     * @param facing    the facing 0-5 on arrival, or {@link UnitOrders#FACING_AUTO}
     * @param holdTurns the full turns to hold after arriving; 0 passes through
     */
    public WaypointOrder(int facing, int holdTurns) {
        if ((facing != UnitOrders.FACING_AUTO) && ((facing < 0) || (facing >= FACING_CODES.size()))) {
            throw new IllegalArgumentException("Facing must be 0-5 or FACING_AUTO, was " + facing);
        }
        if (holdTurns < 0) {
            throw new IllegalArgumentException("Hold turns may not be negative, was " + holdTurns);
        }
        this.facing = facing;
        this.holdTurns = holdTurns;
    }

    /**
     * @return the facing 0-5 on arrival, or {@link UnitOrders#FACING_AUTO}
     */
    public int getFacing() {
        return facing;
    }

    /**
     * @return the full turns to hold after arriving; 0 passes through
     */
    public int getHoldTurns() {
        return holdTurns;
    }

    /**
     * @return {@code true} if the unit stops and holds at this waypoint
     */
    public boolean isHold() {
        return holdTurns > 0;
    }

    /**
     * @return the waypoint's settings as a route order writes them after the hex, e.g. {@code /NE/2}; empty for a
     *       waypoint passed through with the bot choosing the facing
     */
    public String toCommandSuffix() {
        StringBuilder suffix = new StringBuilder();
        if (facing != UnitOrders.FACING_AUTO) {
            suffix.append('/').append(FACING_CODES.get(facing));
        }
        if (holdTurns > 0) {
            suffix.append('/').append(holdTurns);
        }
        return suffix.toString();
    }

    /**
     * Reads the settings a route order writes after a hex: letters are a facing (N, NE, SE, S, SW, NW, or A for the
     * bot's choice), digits are the turns to hold, in either order.
     *
     * @param segments the parts after the hex, e.g. {@code ["NE", "2"]}; none for a plain waypoint
     *
     * @return the waypoint's settings
     *
     * @throws IllegalArgumentException when a part is neither a facing nor a number of turns
     */
    public static WaypointOrder parse(List<String> segments) {
        int parsedFacing = UnitOrders.FACING_AUTO;
        int parsedHold = 0;
        for (String segment : segments) {
            String code = segment.trim().toUpperCase(Locale.ROOT);
            if (code.isEmpty()) {
                continue;
            }
            if (Character.isDigit(code.charAt(0))) {
                parsedHold = Integer.parseInt(code);
            } else if (code.equals(AUTO_CODE)) {
                parsedFacing = UnitOrders.FACING_AUTO;
            } else if (FACING_CODES.contains(code)) {
                parsedFacing = FACING_CODES.indexOf(code);
            } else {
                throw new IllegalArgumentException("Not a facing or a number of turns: " + segment);
            }
        }
        return new WaypointOrder(parsedFacing, parsedHold);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return (other instanceof WaypointOrder otherOrder) && (facing == otherOrder.facing)
              && (holdTurns == otherOrder.holdTurns);
    }

    @Override
    public int hashCode() {
        return (31 * facing) + holdTurns;
    }

    @Override
    public String toString() {
        String suffix = toCommandSuffix();
        return suffix.isEmpty() ? "-" : suffix.substring(1);
    }
}
