/**
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
    public int            x;
    public int            y;
    
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
     * Constructs a new coordinate pair that is a duplicate of
     * the parameter.
     */
    public Coords(Coords c) {
        this(c.x, c.y);
    }
    
    /**
     * Returns a new coordinate that represents the coordinate
     * 1 unit in the specified direction.
     * 
     * @return the new coordinate, if the direction is valid;
     *  otherwise, a new copy of this coordinate.
     * @param direction the direction.
     */
    public Coords translated(int direction) {
        switch(direction) {
        case 0:
            return new Coords(x, y - 1);
        case 1:
            return new Coords(x + 1, y - (isXOdd() ? 0 : 1));
        case 2:
            return new Coords(x + 1, y + (isXOdd() ? 1 : 0));
        case 3:
            return new Coords(x, y + 1);
        case 4:
            return new Coords(x - 1, y + (isXOdd() ? 1 : 0));
        case 5:
            return new Coords(x - 1, y - (isXOdd() ? 0 : 1));
        default:
            return new Coords(this);
        }
    }
    
    /**
     * Tests whether the x coordinate of this coordinate is
     * odd.  This is significant in determining where this
     * coordinate lies in relation to other coordinates.
     */
    public boolean isXOdd() {
        return (x & 1) == 1;
    }
    
    /**
     * Returns the direction in which another 
     * coordinate lies; 0 if the coordinates are equal.
     * 
     * @param d the destination coordinate.
     */
    public int direction(Coords d) {
        int dir = degree(d);
        if (dir > 330 || dir < 30) {
            return 0;
        } else if (dir >= 30 && dir < 90) {
            return 1;
        } else if (dir >= 90 && dir <= 150) {
            return 2;
        } else if (dir > 150 && dir < 210) {
            return 3;
        } else if (dir >= 210 && dir <= 270) {
            return 4;
        } else if (dir > 270 && dir < 330) {
            return 5;
        } else {
            return 0;
        }
    }
    
    /**
     * Old version.
     * 
     * Returns the approximate direction in which another 
     * coordinate lies; 0 if the coordinates are equal.
     * 
     * @param d the destination coordinate.
     */
    public int direction1(Coords d) {
        int deltaX, deltaY;
        if (x < d.x) {
            if (y < d.y || (y == d.y && !isXOdd())) {
                return 2;
            } else {
                return 1;
            }
        } else if (x == d.x) {
            if (y < d.y) {
                return 3;
            } else {
                return 0;
            }
        } else {
            // x > d.x
            if (y < d.y || (y == d.y && !isXOdd())) {
                return 4;
            } else {
                return 5;
            }
        }
    }
    
    /**
     * Returns the radian direction of another Coords.
     * 
     * TODO: this has some precision errors
     * 
     * @param d the destination coordinate.
     */
    public double radian(Coords d) {
        final IdealHex src = new IdealHex(this);
        final IdealHex dst = new IdealHex(d);
        
        double r = Math.atan((double)(dst.cx - src.cx) / (double)(src.cy - dst.cy));
        // flip if we're upside down
        if (src.cy < dst.cy) {
            r = (r + Math.PI) % (Math.PI * 2 );
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
     * Returns the distance to another coordinate.
     * 
     * My old, inaccurate formula (what's wrong?)
     */
    public int distance1(Coords c) {
        int dx, dy, xf;
        dx = Math.abs(this.x - c.x);
        dy = Math.abs(this.y - c.y);
        xf = (dx / 2) + (isXOdd() == c.isXOdd() ? 0 : 1);
        if (dy < xf) {
            return dx;
        } else {
            return dx + dy - xf;
        }
    }
    
    /**
     * Returns a string representing a coordinate in "board number" format.
     */
    public String getBoardNum() {
        StringBuffer num = new StringBuffer();
        
        num.append((x < 9 ? "0" : "") + (x + 1));
        num.append((y < 9 ? "0" : "") + (y + 1));

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
    
    public String toString() {
        return "Coords (" + x + ", " + y + "); " + getBoardNum();
    }
}
