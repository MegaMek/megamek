/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

package megamek.test;

import java.util.ArrayList;
import java.util.List;
import megamek.common.Coords;

/**
 * This class will display various constants and the output of some methods in
 * the <code>Coords</code> class. This will allow a knowledgeable programmer
 * to determine that the <code>Coords</code> class is operating correctly.
 * TODO: integrate JUnit into this class.
 */
public class TestCoords {
    private static final String OUTFORMAT = "The hash of %s is 0x%08X";
    private static final String NBFORMAT = "%s translated in dir %s should be %s and is %s";
    private static final String FARFORMAT = "%s translated by %s in dir %s should be %s and is %s";
    private static final String DISTFORMAT = "Distance between %s and %s should be %s and is %s";
    
    public static void main(String[] args) {

        for (int x = 1; x < 10; ++ x) {
            Coords coords = new Coords(x, 2);
            System.out.println(String.format(OUTFORMAT, coords, coords.hashCode()));
        }
        
        for (int y = 10; y < 19; ++ y) {
            Coords coords = new Coords(1, y);
            System.out.println(String.format(OUTFORMAT, coords, coords.hashCode()));
        }

        Coords neg_coords = new Coords(-11, -22);
        System.out.println(String.format(OUTFORMAT, neg_coords, neg_coords.hashCode()));

        neg_coords = new Coords(42, -68);
        System.out.println(String.format(OUTFORMAT, neg_coords, neg_coords.hashCode()));

        neg_coords = new Coords(-668, 42);
        System.out.println(String.format(OUTFORMAT, neg_coords, neg_coords.hashCode()));
        
        Coords origin = new Coords(0,0);
        int dir = 2;
        Coords neighbor = origin.translated(dir);
        String correct = "(2,1)";
        System.out.println(String.format(NBFORMAT, origin.toFriendlyString(), dir, correct, neighbor.toFriendlyString()));
        
        origin = new Coords(1,0);        
        dir = 2;
        neighbor = origin.translated(dir);
        correct = "(3,2)";
        System.out.println(String.format(NBFORMAT, origin.toFriendlyString(), dir, correct, neighbor.toFriendlyString()));
        
        origin = new Coords(2,1);        
        dir = 2;
        neighbor = origin.translated(dir);
        correct = "(4,2)";
        System.out.println(String.format(NBFORMAT, origin.toFriendlyString(), dir, correct, neighbor.toFriendlyString()));
        
        origin = new Coords(0,0);
        dir = 3;
        neighbor = origin.translated(dir);
        correct = "(1,2)";
        System.out.println(String.format(NBFORMAT, origin.toFriendlyString(), dir, correct, neighbor.toFriendlyString()));

        origin = new Coords(7,-2);        
        dir = 5;
        neighbor = origin.translated(dir);
        correct = "(7,-1)";
        System.out.println(String.format(NBFORMAT, origin.toFriendlyString(), dir, correct, neighbor.toFriendlyString()));
        
        origin = new Coords(0,0);        
        dir = 2;
        int dist = 3;
        neighbor = origin.translated(dir, dist);
        correct = "(4,2)";
        System.out.println(String.format(FARFORMAT, origin.toFriendlyString(), dist, dir, correct, neighbor.toFriendlyString()));

        origin = new Coords(10,4);        
        dir = 0;
        dist = 2;
        neighbor = origin.translated(dir, dist);
        correct = "(11,3)";
        System.out.println(String.format(FARFORMAT, origin.toFriendlyString(), dist, dir, correct, neighbor.toFriendlyString()));
        
        origin = new Coords(10,4);        
        dir = 3;
        dist = 5;
        neighbor = origin.translated(dir, dist);
        correct = "(11,10)";
        System.out.println(String.format(FARFORMAT, origin.toFriendlyString(), dist, dir, correct, neighbor.toFriendlyString()));
        
        origin = new Coords(10,4);        
        dir = 4;
        dist = 3;
        neighbor = origin.translated(dir, dist);
        correct = "(8,6)";
        System.out.println(String.format(FARFORMAT, origin.toFriendlyString(), dist, dir, correct, neighbor.toFriendlyString()));
        
        
        
        origin = new Coords(10,4); // displays as 11,5        
        neighbor = origin.translated(dir, dist);
        correct = "(11, 4); (12, 4); (12, 5); (11, 6); (10, 5); (10, 4)";
        System.out.println("All neighbors of "+origin.toFriendlyString()+": ");
        for (int di: Coords.ALL_DIRECTIONS) {
            System.out.print(origin.translated(di).toFriendlyString()+"; ");
        }
        System.out.println("\nShould be: ");
        System.out.println(correct);
        
        origin = new Coords(13,6); // displays as 14,7        
        dist = 3;
        neighbor = origin.translated(dir, dist);
        correct = "(14, 4); (17, 6); (17, 9); (14, 10); (11, 9); (11, 6)";
        System.out.println("All distance 3 translated hexes of "+origin.toFriendlyString()+": ");
        for (int di: Coords.ALL_DIRECTIONS) {
            System.out.print(origin.translated(di, dist).toFriendlyString()+"; ");
        }
        System.out.println("\nShould be: ");
        System.out.println(correct);
        
        origin = new Coords(13,6);
        Coords dest = new Coords(15,1);
        correct = "6"; 
        System.out.println(String.format(DISTFORMAT, origin.toFriendlyString(), dest.toFriendlyString(), correct, dest.distance(origin)));
        
        origin = new Coords(12,2);
        dest = new Coords(9,2);
        correct = "3"; 
        System.out.println(String.format(DISTFORMAT, origin.toFriendlyString(), dest.toFriendlyString(), correct, dest.distance(origin)));
        
        Coords testCoords = new Coords(0, 0);
        
        List<Coords> resultingCoords = testCoords.allAtDistance(0);
        if (resultingCoords.size() == 1 && resultingCoords.contains(testCoords)) {
            System.out.println("AllAtDistance(0) working correctly.");
        } else {
            System.out.println("AllAtDistance(0) not working correctly.");
        }
        
        // for a radius 1 donut, we expect to see 6 hexes.
        resultingCoords = testCoords.allAtDistance(1);
        
        List<Coords> expectedCoords = new ArrayList<>();
        expectedCoords.add(new Coords(1, -1));
        expectedCoords.add(new Coords(1, 0));
        expectedCoords.add(new Coords(0, -1));
        expectedCoords.add(new Coords(0, 1));
        expectedCoords.add(new Coords(-1, 0));
        expectedCoords.add(new Coords(-1, -1));
        
        boolean iscorrect = true;
        if (resultingCoords.size() != 6) {
            iscorrect = false;
        }
        for (int x = 0; x < expectedCoords.size(); x++) {
            if (! resultingCoords.contains(expectedCoords.get(x))) {
                iscorrect = false;
            }
        }
        if (iscorrect) {
            System.out.println("AllAtDistance(1) working correctly.");
        } else {
            System.out.println("AllAtDistance(1) not working correctly.");
        }
        
        // for a radius 2 donut we expect to see 12 hexes.
        resultingCoords = testCoords.allAtDistance(2);
        
        expectedCoords = new ArrayList<>();
        expectedCoords.add(new Coords(-2, 0));
        expectedCoords.add(new Coords(0, -2));
        expectedCoords.add(new Coords(1, 1));
        expectedCoords.add(new Coords(-2, 1));
        expectedCoords.add(new Coords(1, -2));
        expectedCoords.add(new Coords(-2, -1));
        expectedCoords.add(new Coords(2, 1));
        expectedCoords.add(new Coords(-1, -2));
        expectedCoords.add(new Coords(2, -1));
        expectedCoords.add(new Coords(2, 0));
        expectedCoords.add(new Coords(0, 2));
        expectedCoords.add(new Coords(-1, 1));
        iscorrect = true;
        if (resultingCoords.size() != 12) {
            iscorrect = false;
        }
        for (int x = 0; x < expectedCoords.size(); x++) {
            if (! resultingCoords.contains(expectedCoords.get(x))) {
                iscorrect = false;
            }
        }
        if (iscorrect) {
            System.out.println("AllAtDistance(2) working correctly.");
        } else {
            System.out.println("AllAtDistance(2) not working correctly.");
        }
    }

}
