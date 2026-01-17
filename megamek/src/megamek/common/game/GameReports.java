/*
  Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.game;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.interfaces.FullGameReport;

/**
 * This class is a container for the various reports created by the server during a game.
 */
public class GameReports implements FullGameReport<Report> {
    @Serial
    private static final long serialVersionUID = -2388197938278797669L;

    private List<List<Report>> reports;

    public GameReports() {
        reports = new ArrayList<>();
    }

    @Override
    public void add(int round, List<Report> v) {
        if (round == 0) {
            // Combine round 0 (deployment) with round one's reports.
            round = 1;
        }
        if (!hasReportsForRound(round)) {
            // First reports for the round.
            reports.add(new ArrayList<>(v));
        } else {
            // Already have some reports for this round, so we'll append these new ones.
            reports.get(round - 1).addAll(new Vector<>(v));
        }
    }

    @Override
    public boolean hasReportsForRound(int round) {
        return round >= 0 && round <= reports.size();
    }

    @Override
    public List<Report> get(int round) {
        // Rounds prior to 1 (initial deployment) are lumped in with round 1
        round = Math.max(1, round);
        if (hasReportsForRound(round)) {
            return reports.get(round - 1);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Returns the full set of reports. Note that the lists are fully modifiable and no copies.
     */
    public List<List<Report>> get() {
        return reports;
    }

    /**
     * Replaces the entire contents of this FullGameReport with the given List of report lists.
     *
     * @param v The new contents
     */
    public void set(List<List<Report>> v) {
        reports = v;
    }

    @Override
    public void clear() {
        reports.clear();
    }

}
