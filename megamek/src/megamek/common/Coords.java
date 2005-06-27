/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
 * Coords stores x and y values.  Since these are hexes,
 * coordinates with odd x values are a half-hex down.
 * 
 * Directions work clockwise around the hex, starting with
 * zero at the top.
 * 
 *          -y
 *          0
 *        _____
 *     5 /     \ 1
 * -x   /       \   +x
 *      \       /
 *     4 \_____/ 2
 *          3
 *          +y
 * 
 */
public class Coords
    implements Serializable
{
    public static final double HEXSIDE = Math.PI / 3.0;
    
    public int            x;
    public int            y;

    /**
     * Allow at most 30 boards (510 hexes) in the 'y' direction.
     */
    private static final int SHIFT = 9;
    private static final int MASK = ( 1 << Coords.SHIFT ) - 1;

    /**
     * The maximum height of a board in number of hexes.
     */
    public static final int MAX_BOARD_HEIGHT =
        Integer.MAX_VALUE & Coords.MASK;

    /**
     * The maximum width of a board in number of hexes.
     *
     * Also need to make room for the sign bits.
     */
    public static final int MAX_BOARD_WIDTH =
        ( Integer.MAX_VALUE - Coords.MAX_BOARD_HEIGHT ) >> (Coords.SHIFT + 2);

    /**
     * Constructs a new coordinate pair at (0, 0).
     */
    public Coords() {
        this(0, 0);
    }

    /**
     * Constructs a new coordinate pair at (x, y).
     */
    public Coords(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Constructs a new coordinate pair that is a duplicate of the
     * parameter.
     */
    public Coords(Coords c) {
        this(c.x, c.y);
    }
    
    /**
     * Returns a new coordinate that represents the coordinate 1 unit
     * in the specified direction.
     * 
     * @return the new coordinate, if the direction is valid;
     *  otherwise, a new copy of this coordinate.
     * @param direction the direction.
     */
    public final Coords translated(int dir) {
        return new Coords(xInDir(x, y, dir), yInDir(x, y, dir));
        /*
        switch(direction) {
            case 0 : return new Coords(x, y - 1);
            case 1 : return new Coords(x + 1, y - (x + 1 & 1));
            case 2 : return new Coords(x + 1, y + (x & 1));
            case 3 : return new Coords(x, y + 1);
            case 4 : return new Coords(x - 1, y + (x & 1));
            case 5 : return new Coords(x - 1, y - (x + 1 & 1));
            default: return new Coords(x, y);
        }
         */
    }
    
    /**
     * Returns the x parameter of the coordinates in the direction
     */
    public final static int xInDir(int x, int y, int dir) {
         switch (dir) {
             case 1 :
             case 2 :
                 return x + 1;
             case 4 :
             case 5 :
                 return x - 1;
             default :
                 return x;
         }
    }
    
    /**
     * Returns the y parameter of the coordinates in the direction
     */
    public final static int yInDir(int x, int y, int dir) {
        switch (dir) {
            case 0 : 
                return y - 1;
            case 1 : 
            case 5 :
                return y - ((x + 1) & 1);
            case 2 : 
            case 4 : 
                return y + (x & 1);
            case 3 : 
                return y + 1;
            default :
                return y;
        }
    }
    
    /**
     * Tests whether the x coordinate of this coordinate is odd.  This
     * is significant in determining where this coordinate lies in
     * relation to other coordinates.
     */
    public boolean isXOdd() {
        return (x & 1) == 1;
    }
    
    /**
     * Returns the direction in which another coordinate lies; 0 if
     * the coordinates are equal.
     * 
     * @param d the destination coordinate.
     */
    public int direction(Coords d) {
        return (int)Math.round(radian(d) / HEXSIDE) % 6;
    }
    
    /**
     * Returns the radian direction of another Coords.
     * 
     * @param d the destination coordinate.
     */
    public double radian(Coords d) {
        final IdealHex src = IdealHex.get(this);
        final IdealHex dst = IdealHex.get(d);
        
        // don't divide by 0
        if (src.cy == dst.cy) {
            return (src.cx < dst.cx) ? Math.PI / 2 : Math.PI * 1.5;
        }
        
        double r = Math.atan((double)(dst.cx - src.cx) / (double)(src.cy - dst.cy));
        // flip if we're upside down
        if (src.cy < dst.cy) {
            r = (r + Math.PI) % (Math.PI * 2);
        }
        // account for negative angles
        if (r < 0) {
            r += Math.PI * 2;
        }

        return r;
    }
    
    /**
     * Returns the degree direction of another Coords.
     * 
     * @param d the destination coordinate.
     */
    public int degree(Coords d) {
        return (int)Math.round((180 / Math.PI) * radian(d));
    }
    
    /**
     * Returns the distance to another coordinate.
     */
    public int distance(Coords c) {
        // based off of http://www.rossmack.com/ab/RPG/traveller/AstroHexDistance.asp
        // since I'm too dumb to make my own
        int xd, ym, ymin, ymax, yo;
        xd = Math.abs(this.x - c.x);
        yo = (xd / 2) + (!isXOdd() && c.isXOdd() ? 1 : 0);
        ymin = this.y - yo;
        ymax = ymin + xd;
        ym = 0;
        if (c.y < ymin) {
            ym = ymin - c.y;
        }
        if (c.y > ymax) {
            ym = c.y - ymax;
        }
        return xd + ym;
    }
    
    /**
     * Returns a string representing a coordinate in "board number" format.
     */
    public final String getBoardNum() {
        StringBuffer num = new StringBuffer();
        
        num.append((x > -1 && x < 9 ? "0" : "") + (x + 1));
        num.append((y > -1 && y < 9 ? "0" : "") + (y + 1));

        return num.toString(); 
    }
    
    /**
     * Coords are equal if their x and y components are equal
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Coords other = (Coords)object;
        return other.x == this.x && other.y == this.y;
    }

    /**
     * Get the hash code for these coords.
     *
     * @return  The <code>int</code> hash code for these coords.
     */
    public int hashCode() {
        // Record the signs of X and Y separately from their values.
        boolean negy = ( y < 0 );
        boolean negx = ( x < 0 );
        int signbits = 0;
        int absx = x;
        int absy = y;
        if (negy) {
            signbits += 0x1;
            absy = -y;
        }
        if (negx) {
            signbits += 0x2;
            absx = -x;
        }
        return (((absx << Coords.SHIFT) ^ absy) << 2) + signbits;
    }

    /**
     * Get the coordinates object for a given hash code.
     *
     * @param   hash - the hash code for the desired object.
     * @return  the <code>Coords</code> that match the hash code.
     */
    public static Coords getFromHashCode( int hash ) {
        // The signs of X and Y are recorded separately from their values.
        boolean negy = ( hash & 0x1 ) > 0;
        boolean negx = ( hash & 0x2 ) > 0;
        int signless = hash >>> 2;
        int hashy = ( signless & Coords.MASK );
        int hashx = ( signless ^ hashy ) >>> Coords.SHIFT;
        if (negx) hashx = -hashx;
        if (negy) hashy = -hashy;
        return new Coords (hashx, hashy);
    }

    public String toString() {
        return "Coords (" + x + ", " + y + "); " + getBoardNum();
    }

    /**
     * Returns an array of the Coords of hexes that are crossed by a straight
     * line from the center of src to the center of dest, including src & dest.
     *
     * The returned coordinates are in line order, and if the line passes
     * directly between two hexes, it returns them both.
     *
     * Based on the degree of the angle, the next hex is going to be one of
     * three hexes.  We check those three hexes, sides first, add the first one
     * that intersects and continue from there.
     *
     * Based off of some of the formulas at Amit's game programming site.
     * (http://www-cs-students.stanford.edu/~amitp/gameprog.html)
     */
    public static Coords[] intervening(Coords src, Coords dest) {
        return intervening(src,dest,false);
    }

    public static Coords[] intervening(Coords src, Coords dest, boolean split) {
        IdealHex iSrc = IdealHex.get(src);
        IdealHex iDest = IdealHex.get(dest);
    
        int[] directions = new int[3];
        int centerDirection = src.direction(dest);
        if(split) {
            // HACK to make left appear before right in the sequence reliably
            centerDirection = (int)Math.round(src.radian(dest) + 0.0001 / HEXSIDE) % 6;
        }
        directions[2] = centerDirection; // center last
        directions[1] = (centerDirection + 5) % 6;
        directions[0] = (centerDirection + 1) % 6;
    
        Vector hexes = new Vector();
        Coords current = src;
    
        hexes.addElement(current);
        while(!dest.equals(current)) {
            current = Coords.nextHex(current, iSrc, iDest, directions);
            hexes.addElement(current);
        }
    
        Coords[] hexArray = new Coords[hexes.size()];
        hexes.copyInto(hexArray);
        return hexArray;
    }

    /**
     * Returns the first further hex found along the line from the centers of
     * src to dest.  Checks the three directions given and returns the closest.
     *
     * This relies on the side directions being given first.  If it checked the
     * center first, it would end up missing the side hexes sometimes.
     *
     * Not the most elegant solution, but it works.
     */
    private static Coords nextHex(Coords current, IdealHex iSrc, IdealHex iDest, int[] directions) {
        for (int i = 0; i < directions.length; i++) {
            Coords testing = current.translated(directions[i]);
            if (IdealHex.get(testing).isIntersectedBy(iSrc.cx, iSrc.cy, iDest.cx, iDest.cy)) {
                return testing;
            }
        }
        // if we're here then something's fishy!
        throw new RuntimeException("Couldn't find the next hex!");
    }
}
