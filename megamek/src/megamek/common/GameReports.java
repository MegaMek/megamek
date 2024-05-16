/*
 * MegaMek - Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * This class is a container for the various reports created by the server
 * during a game.
 */
public class GameReports implements FullGameReport<Report> {
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
        if (!hasReportsforRound(round)) {
            // First reports for the round.
            reports.add(new ArrayList<>(v));
        } else {
            // Already have some reports for this round, so we'll append these new ones.
            reports.get(round - 1).addAll(new Vector<>(v));
        }
    }

    @Override
    public boolean hasReportsforRound(int round) {
        return round <= reports.size();
    }

    @Override
    public List<Report> get(int round) {
        if (round == 0) {
            // Round 0 (deployment) reports are lumped in with round one.
            round = 1;
        }
        if (hasReportsforRound(round)) {
            return reports.get(round - 1);
        }

        LogManager.getLogger().error("GameReports.get() was asked for reports of round {} which it does not have",
                round, new RuntimeException());
        return null;
    }

    /**
     *  Returns the full set of reports. Note that the lists are fully modifiable and no copies.
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

    public void clear() {
        reports.clear();
    }

}
