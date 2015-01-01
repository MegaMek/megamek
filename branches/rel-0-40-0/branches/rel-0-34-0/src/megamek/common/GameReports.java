/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common;


import java.io.Serializable;
import java.util.Vector;

/**
 * This class is a container for the various reports created by the server
 * during a game.
 */
public class GameReports implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -2388197938278797669L;
    private Vector<Vector<Report>> reports;

    GameReports() {
        reports = new Vector<Vector<Report>>();
    }

    public void add(int round, Vector<Report> v) {
        if (round == 0) {
            // Combine round 0 (deployment) with round one's reports.
            round = 1;
        }
        if (round > reports.size()) {
            // First reports for the round.
            reports.addElement(new Vector<Report>(v));
        } else {
            // Already have some reports for this round, so we'll append these
            // new ones.
            reports.elementAt(round - 1).addAll(new Vector<Report>(v));
        }
    }

    /**
     *  Get a single round's reports.
     */
    public Vector<Report> get(int round) {
        if (round == 0) {
            // Round 0 (deployment) reports are lumped in with round one.
            round = 1;
        }
        if (round <= reports.size()) {
            return reports.elementAt(round - 1);
        }
        System.err
                .println("ERROR: GameReports.get() was asked for reports of a round which it does not posses.");
        return null;
    }

    /**
     *  Get all the reports.
     */
    public Vector<Vector<Report>> get() {
        return reports;
    }

    /**
     * Set the reports vector from outside all at once.
     * @param v
     */
    public void set(Vector<Vector<Report>> v) {
        reports = v;
    }

    public void clear() {
        reports = new Vector<Vector<Report>>();
    }

}
