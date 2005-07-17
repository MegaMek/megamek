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

/* Do not use the Sun collections (com.sun.java.util.collections.*) framework
 * in this class until Java 1.1 compatibility is abandoned or a
 * non-serialization based save feature is implemented.
 */
import java.util.Vector;
import java.io.Serializable;

/**
 * This class is a container for the various reports created by the server
 * during a game.
 */
public class GameReports implements Serializable {

    private Vector reports;

    GameReports() {
        reports = new Vector();
    }

    public void add(int round, Vector v) {
        if (round < 1) {
            System.err.println("ERROR: GameReports.add() called with round argument of less than 1, which is invalid.");
            return;
        }
        if (round > reports.size()) {
            //First reports for the round.
            reports.addElement(v.clone());
        } else {
            //Already have some reports for this round, so we'll append these
            // new ones.
            GameReports.combineVectors((Vector)reports.elementAt(round - 1), (Vector)v.clone());
        }
    }

    //Get a single round's reports.
    public Vector get(int round) {
        if (round < 1) {
            System.err.println("ERROR: GameReports.get() called with argument of less than 1, which is invalid.");
            return null;
        }
        if (round <= reports.size()) {
            return (Vector)reports.elementAt(round - 1);
        } else {
            System.err.println("ERROR: GameReports.get() was asked for reports of a round which it does not posses.");
            return null;
        }
    }

    //Get all the reports.
    public Vector get() {
        return reports;
    }

    //Set the reports vector from outside all at once.
    public void set(Vector v) {
        reports = v;
    }

    private static void combineVectors(Vector first, Vector second) {
        if (second == null || second.size() == 0) {
            //Hmm...no second vector, no work to do then.
            return;
        }

        for (int i = 0; i < second.size(); i++) {
            first.addElement(second.elementAt(i));
        }

        //Java pass-by-reference means no return value needed.
    }
}
