/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * MegaMek - Copyright (C) 2020 - The MegaMek Team  
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

import jakarta.xml.bind.annotation.XmlElement;
import megamek.client.bot.princess.BotGeometry.HexLine;
import megamek.common.annotations.Nullable;

/**
 * Coords stores x and y values. Since these are hexes, coordinates with odd x
 * values are a half-hex down. Directions work clockwise around the hex,
 * starting with zero at the top. For a hex with an even x, the hexes in directions
 * 2 and 4 (left and right downward) have the same y.
 *      -y
 *       0
 *     _____
 *  5 /     \ 1
 *-x /       \ +x 
 *   \       / 
 *  4 \_____/ 2 
 *       3
 *      +y
 */
public class Coords implements Serializable {

    private static final long serialVersionUID = -4451256806040563030L;

    public static final double HEXSIDE = Math.PI / 3.0;
    public static final int[] ALL_DIRECTIONS = {0, 1, 2, 3, 4, 5};

    @XmlElement(name="x")
    private final int x;
    
    @XmlElement(name="y")
    private final int y;

    @XmlElement(name="hash")
    private int hash;

    /** Constructs a new coordinate pair at (x, y). Note: Coords are immutable. */
    public Coords(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the coordinate 1 unit in the specified direction dir.
     */
    public Coords translated(int dir) {
        return translated(dir, 1);
    }

    /**
     * Returns the coordinate the given distance away in the
     * specified direction dir.
     */
    public Coords translated(int dir, int distance) {
        int newx = xInDir(dir, distance);
        int newy = yInDir(dir, distance);
        return new Coords(newx, newy);
    }

    public Coords translated(String dir) {
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
    
    // The instance methods xInDir etc. make for convenient calls
    // while the static xInDir etc. can be called to avoid Coords construction

    /** Returns the x value of the adjacent Coords in the direction dir. */
    public static int xInDir(int x, int y, int dir) {
        return xInDir(x, y, dir, 1);
    }

    /** Returns the x value of the Coords the given distance in the direction dir. */
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

    /** Returns the y value of the adjacent Coords in the direction dir. */
    public static int yInDir(int x, int y, int dir) {
        return yInDir(x, y, dir, 1);
    }

    /** Returns the x value of the Coords the given distance in the direction dir. */
    public static int yInDir(int x, int y, int dir, int distance) {
        switch (dir) {
            case 0:
                return y - distance;
            case 1:
            case 5:
                return y - ((distance + 1 - (x & 1)) / 2);
            case 2:
            case 4:
                return y + ((distance + (x & 1)) / 2);
            default:
                return y + distance;
        }
    }
    
    /** Returns the x value of the adjacent Coords in the direction dir. */
    public int xInDir(int dir) {
        return Coords.xInDir(x, y, dir, 1);
    }

    /** Returns the x value of the Coords the given distance in the direction dir. */
    public int xInDir(int dir, int distance) {
        return Coords.xInDir(x, y, dir, distance);
    }
    
    /** Returns the y value of the adjacent Coords in the direction dir. */
    public int yInDir(int dir) {
        return Coords.yInDir(x, y, dir, 1);
    }

    /** Returns the y value of the Coords the given distance in the direction dir. */
    public int yInDir(int dir, int distance) {
        return Coords.yInDir(x, y, dir, distance);    
    }

    /**
     * Returns true when the x coordinate of this Coords is odd. This is
     * significant in determining where this coordinate lies in relation to
     * other coordinates.
     */
    public boolean isXOdd() {
        return (x & 1) == 1;
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
     * Returns an approximate direction in which another coordinate lies; 
     * 0 if the coordinates are equal
     */
    public int approximateDirection(Coords second, int initialDirection, int previousDirection) {
        if (this.equals(second)) {
            return 0;
        }
        
        int direction = initialDirection;
        
        HexLine startLine = new HexLine(this, direction);
        int directionIncrement = 0;
        int pointJudgement = startLine.judgePoint(second);
        if (pointJudgement == 0) {
            // we are either directly above or below
            switch (direction) {
                case 0:
                    direction = (getY() > second.getY()) ? 0 : 3;
                    break;
                case 3:
                    direction = (getY() < second.getY()) ? 0 : 3;
                    break;
            }
            return direction;
        } else if (pointJudgement < 0) {
            directionIncrement = 5;
        } else if (pointJudgement > 0) {
            directionIncrement = 1;
        }
        
        int newDirection = (initialDirection + directionIncrement) % 6;
        if (newDirection == previousDirection) {
            return newDirection;
        } else {
            return approximateDirection(second, newDirection, initialDirection);
        }
        
        // draw hexline in "direction".
        // if dest is on hexline (judgePoint == 0), destDir = "direction"
        // if judgepoint < 0, repeat with hexline in (direction - 1) % 6
        // if judgepoint > 0, repeat with hexline in (direction + 1) % 6
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
     * @param d the destination coordinate.
     * @return the degree direction of another Coords
     */
    public int degree(Coords d) {
        return (int) Math.round((180 / Math.PI) * radian(d));
    }

    /**
     * @param coordinates the coordinates to get the distance to, or null
     * @return the distance from these coordinates to the provided coordinates, or Integer.MAX_VALUE
     * if the provided coordinates are null
     */
    public int distance(final @Nullable Coords coordinates) {
        return (coordinates == null) ? Integer.MAX_VALUE : distance(coordinates.getX(), coordinates.getY());
    }

    /** Returns the distance to the coordinate given as distx, disty. */
    public int distance(int distx, int disty) {
        // based on
        // http://www.rossmack.com/ab/RPG/traveller/AstroHexDistance.asp
        int xd = Math.abs(x - distx);
        int yo = (xd / 2) + (!isXOdd() && ((distx & 1) == 1) ? 1 : 0);
        int ymin = y - yo;
        int ymax = ymin + xd;
        int ym = 0;
        if (disty < ymin) {
            ym = ymin - disty;
        }
        if (disty > ymax) {
            ym = disty - ymax;
        }
        return xd + ym;
    }

    /**
     * Returns a string representing a coordinate in "board number" format.
     */
    public String getBoardNum() {
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
        return (other.getX() == x) && (other.getY() == y);
    }

    /** Returns the hash code for these coords. */
    @Override
    public int hashCode() {
        if (hash == 0) {
            hash = Objects.hash(x, y); 
        }
        return hash;
    }

    @Override
    public String toString() {
        return "Coords (" + getX() + ", " + getY() + "); " + getBoardNum();
    }

    /**
     * Returns an array of the Coords of hexes that are crossed by a straight
     * line from the center of src to the center of dest, including src and dest.
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
     * line from the center of src to the center of dest, including src and dest.
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
        if (current == destination) {
            return current;
        }
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
     * Returns a list of all adjacent coordinates (distance = 1), 
     * regardless of whether they're on the board or not.
     */
    public ArrayList<Coords> allAdjacent() {
        return (allAtDistance(1));
    }
    
    /**
     * Returns a list of all coordinates at the given distance dist 
     * and anything less than dist as well.
     */
    public ArrayList<Coords> allAtDistanceOrLess(int dist) {
        ArrayList<Coords> retval = new ArrayList<>();
        
        for (int radius = 0; radius < dist; radius++) {
            retval.addAll(allAtDistance(radius));
        }
        
        return retval;
    }
    
    /**
     * Returns a list of all coordinates at the given distance dist, 
     * regardless of whether they're on the board or not. Returns an 
     * empty Set for dist &lt; 0 and the calling Coords itself for dist == 0.
     */
    public ArrayList<Coords> allAtDistance(int dist) { 
        ArrayList<Coords> retval = new ArrayList<>();
        
        if (dist == 0) {
            retval.add(this);
        } else if (dist > 0) {
            // algorithm outline: travel to the southwest a number of hexes equal to the radius
            // then, "draw" the hex sides in sequence, moving north first to draw the west side, 
            // then rotating clockwise and moving northeast to draw the northwest side and so on, 
            // until we circle around. The length of a hex side is equivalent to the radius
            Coords currentHex = translated(4, dist);

            for (int direction = 0; direction < 6; direction++) {
                for (int translation = 0; translation < dist; translation++) {
                    currentHex = currentHex.translated(direction);
                    retval.add(currentHex);
                }
            }
        }
        return retval;
    }
    
    /**
     * this makes the coordinates 1 based instead of 0 based to match the tiles
     * diaplayed on the grid.
     */
    public String toFriendlyString() {
        return "(" + (x + 1) + ", " + (y + 1) + ")";
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
