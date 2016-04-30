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

import megamek.common.Coords;

/**
 * This class will display various constants and the output of some methods in
 * the <code>Coords</code> class. This will allow a knowledgeable programmer
 * to determine that the <code>Coords</code> class is operating correctly.
 * TODO: integrate JUnit into this class.
 */
public class TestCoords {
    private static final String OUTFORMAT = "The hash of %s is 0x%08X"; //$NON-NLS-1$
    
    public static void main(String[] args) {

        System.out.println("The maximum board height: " + Coords.MAX_BOARD_HEIGHT); //$NON-NLS-1$

        System.out.println("The maximum board width: " + Coords.MAX_BOARD_WIDTH); //$NON-NLS-1$

        for(int x = 1; x < 10; ++ x) {
            Coords coords = new Coords(x, 2);
            System.out.println(String.format(OUTFORMAT, coords, coords.hashCode()));
        }
        
        for(int y = 10; y < 19; ++ y) {
            Coords coords = new Coords(1, y);
            System.out.println(String.format(OUTFORMAT, coords, coords.hashCode()));
        }

        Coords neg_coords = new Coords(-11, -22);
        System.out.println(String.format(OUTFORMAT, neg_coords, neg_coords.hashCode()));

        neg_coords = new Coords(42, -68);
        System.out.println(String.format(OUTFORMAT, neg_coords, neg_coords.hashCode()));

        neg_coords = new Coords(-668, 42);
        System.out.println(String.format(OUTFORMAT, neg_coords, neg_coords.hashCode()));
    }

}
