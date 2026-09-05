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
package megamek.server.victory;

import java.io.Serializable;

/**
 * One band of a scenario's graded victory scale: a winner whose final victory point total is at most
 * {@link #getUpTo()} achieves the victory named {@link #getName()} - for example "Pyrrhic victory" up to 10,
 * "Minor victory" up to 20. The last band of a scale may be unbounded (see {@link #isUnbounded()}) to catch
 * every higher total. A deliberate plain class rather than a record: it is part of the saved {@code Game}, and
 * save games cannot restore records without a hand-written XStream converter.
 */
public class VictoryPointLevel implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The upper bound marking a band without one - the last band of a scale catches everything above. */
    public static final int NO_UPPER_BOUND = Integer.MAX_VALUE;

    private final int upTo;
    private final String name;

    /**
     * @param upTo the highest winner total this band covers, or {@link #NO_UPPER_BOUND} for an unbounded band
     * @param name the victory name this band awards, e.g. "Major victory"
     */
    public VictoryPointLevel(int upTo, String name) {
        this.upTo = upTo;
        this.name = name;
    }

    /** @return The highest winner total this band covers */
    public int getUpTo() {
        return upTo;
    }

    /** @return The victory name this band awards */
    public String getName() {
        return name;
    }

    /** @return {@code true} when this band has no upper bound and catches every higher total */
    public boolean isUnbounded() {
        return upTo == NO_UPPER_BOUND;
    }
}
