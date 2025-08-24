/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.interfaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import megamek.common.game.GameReports;

/**
 * This interface is implemented by objects that represent a collection of reports representing the course of a game.
 *
 * @param <T> The type of individual report entry used
 *
 * @see ReportEntry
 */
public interface FullGameReport<T extends ReportEntry> extends Serializable {

    /**
     * Adds the given list of report entries for the given game round to this FullGameReport. Note that in the present
     * implementation {@link GameReports}, rounds 0 (deployment) and 1 are lumped together as round 1.
     *
     * @param round   The round the report is for
     * @param reports The list of reports to add
     *
     * @see #add(int, ReportEntry)
     */
    void add(int round, List<T> reports);

    /**
     * Adds the given report entry for the given game round to this FullGameReport. Note that in the present
     * implementation {@link GameReports}, rounds 0 (deployment) and 1 are lumped together as round 1.
     *
     * @param round  The round the report is for
     * @param report The report to add
     *
     * @see #add(int, List)
     */
    default void add(int round, T report) {
        add(round, new ArrayList<>(List.of(report)));
    }

    /**
     * Returns false when no reports have been added for the given round at all. Returns true when at least one report
     * for the given round is present even if it is empty.
     *
     * @param round The game round to test
     *
     * @return False when no report has been added for the given round, true otherwise
     */
    boolean hasReportsForRound(int round);

    /**
     * Returns the reports for the given game round. Note that this method should not return null but rather an empty
     * list.
     *
     * @param round The game round to get the reports for
     *
     * @see #hasReportsForRound(int)
     */
    List<T> get(int round);

    /**
     * Clears this FullGameReport, removing all present reports for all rounds.
     */
    void clear();
}
