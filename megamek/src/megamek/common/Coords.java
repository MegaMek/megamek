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
import java.util.ArrayList;

import megamek.common.util.HashCodeUtil;

/**
 * Coords stores x and y values. Since these are hexes, coordinates with odd x
 * values are a half-hex down. Directions work clockwise around the hex,
 * starting with zero at the top. 
 *      -y
 *       0
 *     _____
 *  5 /     \ 1
  -x /       \ +x 
 *   \       / 
 *  4 \_____/ 2 
 *       3
 *      +y
 */
public class Coords implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -4451256806040563030L;

    public static final double HEXSIDE = Math.PI / 3.0;

    /**
     * Allow at most 30 boards (510 hexes) in the 'y' direction.
     */
    private static final int SHIFT = 9;
    private static final int MASK = (1 << Coords.SHIFT) - 1;

    /**
     * The maximum height of a board in number of hexes.
     */
    public static final int MAX_BOARD_HEIGHT = Integer.MAX_VALUE & Coords.MASK;

    /**
     * The maximum width of a board in number of hexes. Also need to make room
     * for the sign bits.
     */
    public static final int MAX_BOARD_WIDTH = (Integer.MAX_VALUE - Coords.MAX_BOARD_HEIGHT) >> (Coords.SHIFT + 2);

    private final int x;
    private final int y;
    private final int hash;

    /**
     * Constructs a new coordinate pair at (x, y).
     */
    public Coords(int x, int y) {
        this.x = x;
        this.y = y;
        // Make sure the hash is positive
        this.hash = (HashCodeUtil.hash1(x + 1337) ^ HashCodeUtil.hash1(y + 97331)) & 0x7FFFFFFF;
    }

    /**
     * Constructs a new coordinate pair at (0, 0).
     */
    public Coords() {
        this(0, 0);
    }

    /**
     * Constructs a new coordinate pair that is a duplicate of the parameter.
     */
    public Coords(Coords c) {
        this(c.getX(), c.getY());
    }

    /**
     * Returns a new coordinate that represents the coordinate 1 unit in the
     * specified direction.
     * 
     * @return the new coordinate, if the direction is valid; otherwise, a new
     *         copy of this coordinate.
     * @param dir the direction.
     */
    public final Coords translated(int dir) {
        return translated(dir, 1);
    }

    public final Coords translated(int dir, int distance) {
        int newx = xInDir(getX(), getY(), dir, distance);
        int newy = yInDir(getX(), getY(), dir, distance);
        return new Coords(newx, newy);
    }

    public final Coords translated(String dir) {
        int intDir = 0;

        try {
            intDir = Integer.parseInt(dir);
        } catch (NumberFormatException nfe) {
            if (dir.equalsIgnoreCase("N")) {
                intDir = 0;
            } else if (dir.equalsIgnoreCase("NE")) {
                intDir = 1;
            } else if (dir.equalsIgnoreCase("SE")) {
                intDir = 2;
            } else if (dir.equalsIgnoreCase("S")) {
                intDir = 3;
            } else if (dir.equalsIgnoreCase("SW")) {
                intDir = 4;
            } else if (dir.equalsIgnoreCase("NW")) {
                intDir = 5;
            }
        }

        return translated(intDir);
    }

    /**
     * Returns the x parameter of the coordinates in the direction
     */
    public static int xInDir(int x, int y, int dir) {
        switch (dir) {
            case 1:
            case 2:
                return x + 1;
            case 4:
            case 5:
                return x - 1;
            default:
                return x;
        }
    }

    public static int xInDir(int x, int y, int dir, int distance) {
        switch (dir) {
            case 1:
            case 2:
                return x + distance;
            case 4:
            case 5:
                return x - distance;
            default:
                return x;
        }
    }

    /**
     * Returns the y parameter of the coordinates in the direction
     */
    public static int yInDir(int x, int y, int dir) {
        switch (dir) {
            case 0:
                return y - 1;
            case 1:
            case 5:
                return y - ((x + 1) & 1);
            case 2:
            case 4:
                return y + (x & 1);
            case 3:
                return y + 1;
            default:
                return y;
        }
    }

    public static int yInDir(int x, int y, int dir, int distance) {
        switch (dir) {
            case 0:
                return y - distance;
            case 1:
            case 5:
                if ((x & 1) == 1)
                    return y - (distance / 2);
                return y - ((distance + 1) / 2);
            case 2:
            case 4:
                if ((x & 1) == 0)
                    return y + (distance / 2);
                return y + ((distance + 1) / 2);
            case 3:
                return y + distance;
            default:
                return y;
        }
    }

    /**
     * Tests whether the x coordinate of this coordinate is odd. This is
     * significant in determining where this coordinate lies in relation to
     * other coordinates.
     */
    public final boolean isXOdd() {
        return (getX() & 1) == 1;
    }

    /**
     * Returns the direction in which another coordinate lies; 0 if the
     * coordinates are equal.
     * 
     * @param d the destination coordinate.
     */
    public int direction(Coords d) {
        return (int) Math.round(radian(d) / HEXSIDE) % 6;
    }

    /**
     * Returns the radian direction of another Coords.
     * 
     * @param d the destination coordinate.
     */
    public final double radian(Coords d) {
        final IdealHex src = IdealHex.get(this);
        final IdealHex dst = IdealHex.get(d);

        // don't divide by 0
        if (src.cy == dst.cy) {
            return (src.cx < dst.cx) ? Math.PI / 2 : Math.PI * 1.5;
        }

        double r = Math.atan((dst.cx - src.cx) / (src.cy - dst.cy));
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
    public final int degree(Coords d) {
        return (int) Math.round((180 / Math.PI) * radian(d));
    }

    /**
     * Returns the distance to another coordinate.
     */
    public final int distance(Coords c) {
        // based off of
        // http://www.rossmack.com/ab/RPG/traveller/AstroHexDistance.asp
        // since I'm too dumb to make my own
        int xd, ym, ymin, ymax, yo;
        xd = Math.abs(this.getX() - c.getX());
        yo = (xd / 2) + (!isXOdd() && c.isXOdd() ? 1 : 0);
        ymin = this.getY() - yo;
        ymax = ymin + xd;
        ym = 0;
        if (c.getY() < ymin) {
            ym = ymin - c.getY();
        }
        if (c.getY() > ymax) {
            ym = c.getY() - ymax;
        }
        return xd + ym;
    }

    public final int distance(int distx, int disty) {
        return distance(new Coords(distx, disty));
    }

    /**
     * Returns a string representing a coordinate in "board number" format.
     */
    public final String getBoardNum() {
        StringBuilder num = new StringBuilder();

        num.append(getX() > -1 && getX() < 9 ? "0" : "").append(getX() + 1);
        num.append(getY() > -1 && getY() < 9 ? "0" : "").append(getY() + 1);

        return num.toString();
    }

    /**
     * Coords are equal if their x and y components are equal
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Coords other = (Coords) object;
        return other.getX() == this.getX() && other.getY() == this.getY();
    }

    /**
     * Get the hash code for these coords.
     * 
     * @return The <code>int</code> hash code for these coords.
     */
    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "Coords (" + getX() + ", " + getY() + "); " + getBoardNum();
    }

    /**
     * Returns an array of the Coords of hexes that are crossed by a straight
     * line from the center of src to the center of dest, including src & dest.
     * The returned coordinates are in line order, and if the line passes
     * directly between two hexes, it returns them both. Based on the degree of
     * the angle, the next hex is going to be one of three hexes. We check those
     * three hexes, sides first, add the first one that intersects and continue
     * from there. Based off of some of the formulas at Amit's game programming
     * site. (http://www-cs-students.stanford.edu/~amitp/gameprog.html)
     * 
     * Note: this function can return Coordinates that are not on the board.
     *
     * @param src Starting point.
     * @param dest Ending Point.
     * @return The list of intervening coordinates.
     */
    public static ArrayList<Coords> intervening(Coords src, Coords dest) {
        return intervening(src, dest, false);
    }

    /**
     * Returns an array of the Coords of hexes that are crossed by a straight
     * line from the center of src to the center of dest, including src & dest.
     * The returned coordinates are in line order, and if the line passes
     * directly between two hexes, it returns them both. Based on the degree of
     * the angle, the next hex is going to be one of three hexes. We check those
     * three hexes, sides first, add the first one that intersects and continue
     * from there. Based off of some of the formulas at Amit's game programming
     * site. (http://www-cs-students.stanford.edu/~amitp/gameprog.html)
     *
     * Note: this function can return Coordinates that are not on the board.
     *
     * @param src Starting point.
     * @param dest Ending Point.
     * @param split Set TRUE to make left appear before right in the sequence reliably
     * @return The list of intervening coordinates.
     */
    public static ArrayList<Coords> intervening(Coords src, Coords dest, boolean split) {
        IdealHex iSrc = IdealHex.get(src);
        IdealHex iDest = IdealHex.get(dest);

        int[] directions = new int[3];
        int centerDirection = src.direction(dest);
        if (split) {
            // HACK to make left appear before right in the sequence reliably
            centerDirection = (int) Math.round(src.radian(dest) + 0.0001
                    / HEXSIDE) % 6;
        }
        directions[2] = centerDirection; // center last
        directions[1] = (centerDirection + 5) % 6;
        directions[0] = (centerDirection + 1) % 6;

        ArrayList<Coords> hexes = new ArrayList<>();
        Coords current = src;

        hexes.add(current);
        while (!dest.equals(current)) {
            current = Coords.nextHex(current, iSrc, iDest, directions);
            hexes.add(current);
        }

        return hexes;
    }

    /**
     * Returns the first further hex found along the line from the centers of
     * src to dest. Checks the three directions given and returns the closest.
     * This relies on the side directions being given first. If it checked the
     * center first, it would end up missing the side hexes sometimes. Not the
     * most elegant solution, but it works.
     */
    public static Coords nextHex(Coords current, IdealHex iSrc, IdealHex iDest, int[] directions) {
        for (int direction : directions) {
            Coords testing = current.translated(direction);
            if (IdealHex.get(testing).isIntersectedBy(iSrc.cx, iSrc.cy,
                                                      iDest.cx, iDest.cy)) {
                return testing;
            }
        }
        // if we're here then something's fishy!
        throw new RuntimeException("Couldn't find the next hex!");
    }

    /**
     * Pass-thru version of the above that assumes current = iSrc.
     */
    public static Coords nextHex(Coords current, Coords destination) {
        if (current == destination)
            return current;
        int[] directions;
        if (current.getX() == destination.getX()) {
            if (current.getY() > destination.getY()) {
                directions = new int[1];
                directions[0] = 0;
            } else {
                directions = new int[1];
                directions[0] = 3;
            }
        } else if (current.getX() > destination.getX()) {
            if (current.getY() > destination.getY()) {
                directions = new int[3];
                directions[0] = 4;
                directions[1] = 5;
                directions[2] = 0;
            } else {
                directions = new int[3];
                directions[0] = 3;
                directions[1] = 4;
                directions[2] = 5;
            }
        } else {
            if (current.getY() > destination.getY()) {
                directions = new int[3];
                directions[0] = 0;
                directions[1] = 1;
                directions[2] = 2;
            } else {
                directions = new int[3];
                directions[0] = 1;
                directions[1] = 2;
                directions[2] = 3;
            }
        }
        return nextHex(current, new IdealHex(current),
                new IdealHex(destination), directions);
    }

    /**
     * this makes the coordinates 1 based instead of 0 based to match the tiles
     * diaplayed on the grid.
     */
    public final String toFriendlyString() {
        return "(" + (getX() + 1) + ", " + (getY() + 1) + ")";
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }
}
