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
package megamek.client.bot.princess;

import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;

/**
 * Shapes a withdrawal, so that pulling back is a manoeuvre rather than a rout.
 *
 * <p>A withdrawing unit is exempt from the fighting line's cohesion pull, and rightly so: that line is what it is
 * leaving. But left at that it is governed by nothing at all - it runs on its own, and units that run on their own
 * are caught on their own.</p>
 *
 * <p>Two forces apply, and neither of them slows the retreat. Getting away is driven by the self-preservation pull
 * elsewhere; these only choose between ways of getting away.</p>
 *
 * <ul>
 *     <li><b>Keep with the others pulling back.</b> Ending outside the group costs, so a withdrawal leaves as a body
 *     that can still cover itself.</li>
 *     <li><b>But not in a heap.</b> Ending on top of another withdrawing unit costs too, so one attack cannot catch
 *     several of them at once. A retreat bunched into a single hex cluster is how a fighting withdrawal becomes a
 *     massacre.</li>
 * </ul>
 *
 * <p>Separate from {@link MutualSupportDeployment} and {@link MutualSupportPathRanker} because it answers a different
 * question. Those two ask where the fighting force should be; this asks what the units leaving it should do, and the
 * answers do not share their reasoning.</p>
 */
public final class WithdrawalFormation {

    /**
     * Closest a withdrawing unit will willingly move to another, in hexes.
     *
     * <p>Wider than the spacing a fighting formation keeps. A force in contact wants mutual support and accepts the
     * risk of being close; a force pulling back has already given up on supporting each other and only wants not to
     * be caught together.</p>
     */
    static final int SEPARATION_HEXES = 3;

    private WithdrawalFormation() {
    }

    /**
     * The point the withdrawing elements are gathered on.
     *
     * @param withdrawingPositions where the units pulling back currently are
     *
     * @return their centre of mass, or {@code null} when nobody else is pulling back
     */
    static @Nullable Coords centre(List<Coords> withdrawingPositions) {
        return MutualSupportDeployment.centroid(withdrawingPositions);
    }

    /**
     * How far outside the withdrawing group a destination lies; zero inside it.
     */
    static int hexesOutOfGroup(Coords destination, Coords centre, int groupRadius) {
        return Math.max(0, centre.distance(destination) - groupRadius);
    }

    /**
     * How far inside the separation distance the nearest other withdrawing unit is; zero once there is room.
     */
    static int crowding(Coords destination, List<Coords> withdrawingPositions) {
        int worst = 0;
        for (Coords position : withdrawingPositions) {
            worst = Math.max(worst, SEPARATION_HEXES - position.distance(destination));
        }
        return Math.max(0, worst);
    }

    /**
     * The penalty for a withdrawing unit ending at the given destination.
     *
     * <p>The two terms are added rather than ranked: breaking away from the group and piling onto it are both bad,
     * and a withdrawal has no equivalent of the closing advance that has to be protected from being outbid.</p>
     *
     * @param destination          where the path being judged ends
     * @param withdrawingPositions where the other units pulling back currently are
     * @param groupRadius          how far from the group's centre a unit may be before it counts as broken away
     * @param weight               utility cost per hex, from the caller's cohesion weighting
     *
     * @return the penalty, to be subtracted from path utility; zero when nobody else is pulling back
     */
    static double penalty(Coords destination, List<Coords> withdrawingPositions, int groupRadius, double weight) {
        Coords centre = centre(withdrawingPositions);
        if (centre == null) {
            return 0;
        }
        return (hexesOutOfGroup(destination, centre, groupRadius) + crowding(destination, withdrawingPositions))
              * weight;
    }
}
