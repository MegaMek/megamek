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

import java.util.List;

/**
 * The names a route's waypoints go by on the radio and on the map: Nav Point Alpha, Beta, Gamma and on through the
 * Greek alphabet, so they never read like a lance's name (Alpha, Bravo, Charlie Lance). "Charlie Lance, proceeding to
 * Nav Point Gamma."
 */
public final class NavPoint {

    /** A waypoint with no name: one from a save made before waypoints had them, or one added by a typed order. */
    public static final int UNNAMED = 0;

    private static final List<String> NAMES = List.of("Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta",
          "Theta", "Iota", "Kappa", "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma", "Tau", "Upsilon",
          "Phi", "Chi", "Psi", "Omega");

    private NavPoint() {}

    /**
     * @param navNumber the waypoint's number in the order it was given, from 1
     *
     * @return its name, e.g. {@code Gamma} for 3; past Omega the names start again with a number, {@code Alpha 2}
     */
    public static String name(int navNumber) {
        int index = Math.max(0, navNumber - 1);
        String name = NAMES.get(index % NAMES.size());
        int round = (index / NAMES.size()) + 1;
        return (round == 1) ? name : name + ' ' + round;
    }
}
