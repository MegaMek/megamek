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

package megamek.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.interfaces.FullGameReport;
import megamek.common.strategicBattleSystems.SBFReportEntry;

/**
 * This class gathers the full game report for an SBF game.
 */
public class SBFFullGameReport implements FullGameReport<SBFReportEntry> {

    private final Map<Integer, List<SBFReportEntry>> fullReport = new HashMap<>();

    @Override
    public void add(int round, List<SBFReportEntry> reports) {
        if ((reports != null) && !reports.isEmpty()) {
            fullReport.computeIfAbsent(round, k -> new ArrayList<>()).addAll(reports);
        }
    }

    @Override
    public boolean hasReportsForRound(int round) {
        return fullReport.containsKey(round);
    }

    @Override
    public List<SBFReportEntry> get(int round) {
        return fullReport.getOrDefault(round, new ArrayList<>());
    }

    @Override
    public void clear() {
        fullReport.clear();
    }

    public void replaceAllReports(Map<Integer, List<SBFReportEntry>> newReports) {
        clear();
        fullReport.putAll(newReports);
    }


    public Map<Integer, List<SBFReportEntry>> createFilteredReport(Player recipient) {
        // In SBF, double blind is always in effect, presenting radar blips;
        //TODO  But it is optional to hide units entirely; may check this game option
        Map<Integer, List<SBFReportEntry>> filteredReports = new HashMap<>();
        for (int round : fullReport.keySet()) {
            List<SBFReportEntry> filteredRoundReports = new ArrayList<>();
            for (SBFReportEntry r : fullReport.get(round)) {
                //TODO cannot filter at this time, as Report uses Entity
                //                if (r.isObscuredRecipient(recipient.getName())) {
                //                    r = filterReport(r, null, true);
                //                }
                //                if (r != null) {
                filteredRoundReports.add(r);
                //                }
            }
            filteredReports.put(round, filteredRoundReports);
        }
        return filteredReports;
    }
}
