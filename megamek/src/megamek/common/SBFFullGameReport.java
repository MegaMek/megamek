/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import java.util.*;

/**
 * This class gathers the full game report for an SBF game.
 */
public class SBFFullGameReport implements FullGameReport<Report> {

    private final Map<Integer, List<Report>> fullReport = new HashMap<>();

    @Override
    public void add(int round, List<Report> reports) {
        if ((reports != null) && !reports.isEmpty()) {
            fullReport.computeIfAbsent(round, k -> new ArrayList<>()).addAll(reports);
        }
    }

    @Override
    public boolean hasReportsforRound(int round) {
        return fullReport.containsKey(round);
    }

    @Override
    public List<Report> get(int round) {
        return fullReport.getOrDefault(round, new ArrayList<>());
    }

    @Override
    public void clear() {
        fullReport.clear();
    }

    public void replaceAllReports(Map<Integer, List<Report>> newReports) {
        clear();
        fullReport.putAll(newReports);
    }


    public Map<Integer, List<Report>> createFilteredReport(Player recipient) {
        // In SBF, double blind is always in effect, presenting radar blips;
        //TODO  But it is optional to hide units entirely; may check this game option
        Map<Integer, List<Report>> filteredReports = new HashMap<>();
        for (int round : fullReport.keySet()) {
            List<Report> filteredRoundReports = new ArrayList<>();
            for (Report r : fullReport.get(round)) {
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
