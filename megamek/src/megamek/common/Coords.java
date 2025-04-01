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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import jakarta.xml.bind.annotation.XmlElement;
import megamek.client.bot.princess.BotGeometry.HexLine;
import megamek.common.annotations.Nullable;

/**
 * Coords stores x and y values. Since these are hexes, coordinates with odd x values are a half-hex down. Directions
 * work clockwise around the hex, starting with zero at the top. For a hex with an even x, the hexes in directions 2 and
 * 4 (left and right downward) have the same y.
 * <pre>
 *      -y
 *       0
 *     _____
 *  5 /     \ 1
 * -x /       \ +x
 *   \       /
 *  4 \_____/ 2
 *       3
 *      +y
 * </pre>
 */
public class Coords implements Serializable {
    @Serial
    private static final long serialVersionUID = -4451256806040563030L;

    // region Constants
    static final double EPSILON = 1e-7;
    static final int MAX_ITERATIONS = 1000; // for median logic
    public static final double HEXSIDE = Math.PI / 3.0;
    public static final int[] ALL_DIRECTIONS = { 0, 1, 2, 3, 4, 5 };
    // endregion Constants

    @XmlElement(name = "x")
    private final int x;

    @XmlElement(name = "y")
    private final int y;

    @XmlElement(name = "hash")
    private int hash;

    /** Constructs a new coordinate pair at (x, y). Note: Coords are immutable. */
    public Coords(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Constructs a new coordinate pair at Coords(x, y). Note: Coords are immutable. */
    public Coords(Coords other) {
        this.x = other.x;
        this.y = other.y;
        this.hash = other.hash;
    }

    /**
     * Parses a string into a Coords object. The string can be in the format x,y or HexNumber. HexNumbers are offset by
     * 1, so we have to reduce it here.
     * <pre>{@code String hexNUmber = "0423";
     * Coords coords = Coords.parse(hexNumber);
     * assert coords.getX() == 3;
     * assert coords.getY() == 22;}</pre>
     * <p>Using X and Y is also easy</p>
     * <pre>{@code String xy = "4,23";
     * Coords coords = Coords.parse(xy);
     * assert coords.getX() == 3;
     * assert coords.getY() == 22;}</pre>
     *
     * @param input the string to parse
     *
     * @return the Coords object
     */
    public static Coords parseHexNumber(String input) {
        return parse(input, -1);
    }

    /**
     * Parses a string into a Coords object. The string can be in the format x,y or HexNumber. You can also apply any
     * offset you want to compensate different starting points or uses.
     * <pre>{@code String hexNUmber = "0423";
     * Coords coords = Coords.parse(hexNumber, -1);
     * assert coords.getX() == 3;
     * assert coords.getY() == 22;}</pre>
     * <p>Using X and Y is also easy</p>
     * <pre>{@code String xy = "4,23";
     * Coords coords = Coords.parse(xy, 0);
     * assert coords.getX() == 4;
     * assert coords.getY() == 23;}</pre>
     *
     * @param input the string to parse
     *
     * @return the Coords object
     *
     * @throws IllegalArgumentException if the input is not in the correct format or is null
     */
    public static Coords parse(String input, int offset) {
        if (input == null) {
            throw new IllegalArgumentException("Coords require a value.");
        }
        String[] parts = getParts(input);
        try {
            // hexNumbers are offset by 1, so we have to reduce it here
            int x = Integer.parseInt(parts[0]) + offset;
            int y = Integer.parseInt(parts[1]) + offset;
            return new Coords(x, y);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Coords must be in the format x,y.");
        }
    }

    private static String[] getParts(String input) {
        String[] parts;

        if (input.contains(",")) {
            parts = input.split(",");
        } else {
            // split in half
            if (input.length() % 2 == 1) {
                throw new IllegalArgumentException(
                      "Coords must be in the format x,y or hex number. Hex number always has an even number of digits.");
            }
            int half = input.length() / 2;
            parts = new String[] { input.substring(0, half), input.substring(half) };
        }

        if (parts.length != 2) {
            throw new IllegalArgumentException("Coords must be in the format x,y or hex number.");
        }

        return parts;
    }

    public @Nullable Coords closestCoords(List<Coords> coords) {
        if (coords.isEmpty()) {
            return null;
        }

        Coords closest = null;
        int closestDistance = Integer.MAX_VALUE;

        for (Coords c : coords) {
            int distance = distance(c);

            if (distance < closestDistance) {
                closest = c;
                closestDistance = distance;
            }
        }

        return closest;
    }

    public static @Nullable Coords average(List<Coords> positions) {
        if (positions.isEmpty()) {
            return null;
        }

        int x = 0;
        int y = 0;

        for (Coords pos : positions) {
            x += pos.x;
            y += pos.y;
        }

        return new Coords(x / positions.size(), y / positions.size());
    }


    /**
     * Returns the median of the given list of positions. The median is the point that minimizes the sum of the
     * distances to all other points in the list. The algorithm is based on the Weiszfeld algorithm.
     *
     * @param positions list of positions
     *
     * @return the median of the given list of positions
     */
    public static @Nullable Coords median(Collection<Coords> positions) {
        if (positions.isEmpty()) {
            return null;
        }

        int n = positions.size();

        if (n == 1) {
            return positions.stream().findAny().orElse(null);
        }

        double x0 = 0.0;
        double y0 = 0.0;

        for (Coords p : positions) {
            x0 += p.x;
            y0 += p.y;
        }

        x0 /= n;
        y0 /= n;

        double currentX = x0;
        double currentY = y0;

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            double numeratorX = 0.0;
            double numeratorY = 0.0;
            double denominator = 0.0;

            for (Coords p : positions) {
                double dx = p.x - currentX;
                double dy = p.y - currentY;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < 1e-15) {
                    return p;
                }

                double w = 1.0 / dist;
                numeratorX += p.x * w;
                numeratorY += p.y * w;
                denominator += w;
            }

            double newX = numeratorX / denominator;
            double newY = numeratorY / denominator;

            // Check for convergence
            double shift = Math.sqrt((currentX - newX) * (currentX - newX) + (currentY - newY) * (currentY - newY));
            currentX = newX;
            currentY = newY;

            if (shift < EPSILON) {
                break;
            }
        }

        return new Coords((int) currentX, (int) currentY);
    }


    /**
     * Returns the coordinate 1 unit in the specified direction dir.
     */
    public Coords translated(int dir) {
        return translated(dir, 1);
    }

    /**
     * Returns the coordinate the given distance away in the specified direction dir.
     */
    public Coords translated(int dir, int distance) {
        int newX = xInDir(dir, distance);
        int newY = yInDir(dir, distance);

        return new Coords(newX, newY);
    }

    public Coords translated(String dir) {
        int intDir = 0;

        if (Character.isDigit(dir.charAt(0))) {
            intDir = Integer.parseInt(dir);
        } else {
            if (dir.equalsIgnoreCase("NE")) {
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

    // The instance methods xInDir etc. make for convenient calls while the static xInDir etc. can be called to avoid
    // Coords construction

    /** Returns the x value of the adjacent Coords in the direction dir. */
    public static int xInDir(int x, int y, int dir) {
        return xInDir(x, y, dir, 1);
    }

    /** Returns the x value of the Coords the given distance in the direction dir. */
    public static int xInDir(int x, int y, int dir, int distance) {
        return switch (dir) { // NE
            case 1, 2 -> // SE
                  x + distance; // NW
            case 4, 5 -> // SW
                  x - distance; // North
            // South
            default -> x;
        };
    }

    /** Returns the y value of the adjacent Coords in the direction dir. */
    public static int yInDir(int x, int y, int dir) {
        return yInDir(x, y, dir, 1);
    }

    /** Returns the x value of the Coords the given distance in the direction dir. */
    public static int yInDir(int x, int y, int dir, int distance) {
        return switch (dir) {
            case 0 -> y - distance;
            case 1, 5 -> y - ((distance + 1 - (x & 1)) / 2);
            case 2, 4 -> y + ((distance + (x & 1)) / 2);
            default -> y + distance;
        };
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
     * Returns true when the x coordinate of this Coords is odd. This is significant in determining where this
     * coordinate lies in relation to other coordinates.
     */
    public boolean isXOdd() {
        return (x & 1) == 1;
    }

    /**
     * Returns the direction in which another coordinate lies; 0 if the coordinates are equal.
     *
     * @param d the destination coordinate.
     */
    public int direction(Coords d) {
        return (int) Math.round(radian(d) / HEXSIDE) % 6;
    }

    /**
     * Returns an approximate direction in which another coordinate lies; 0 if the coordinates are equal
     */
    public int approximateDirection(Coords second, int initialDirection, int previousDirection) {
        if (this.equals(second)) {
            return 0;
        }

        int direction = initialDirection;

        HexLine startLine = new HexLine(this, direction);
        int directionIncrement;
        int pointJudgement = startLine.judgePoint(second);
        if (pointJudgement == 0) {
            // we are either directly above or below
            direction = switch (direction) {
                case 0 -> (getY() > second.getY()) ? 0 : 3;
                case 3 -> (getY() < second.getY()) ? 0 : 3;
                default -> direction;
            };
            return direction;
        } else if (pointJudgement < 0) {
            directionIncrement = 5;
        } else {
            directionIncrement = 1;
        }

        int newDirection = (initialDirection + directionIncrement) % 6;

        if (newDirection == previousDirection) {
            return newDirection;
        } else {
            return approximateDirection(second, newDirection, initialDirection);
        }

        // draw hex line in "direction".
        // if dest is on hex line (judgePoint == 0), destDir = "direction"
        // if judge point < 0, repeat with hex line in (direction - 1) % 6
        // if judge point > 0, repeat with hex line in (direction + 1) % 6
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
     *
     * @return the degree direction of another Coords
     */
    public int degree(Coords d) {
        return (int) Math.round((180 / Math.PI) * radian(d));
    }

    /**
     * @param coordinates the coordinates to get the distance to, or null
     *
     * @return the distance from these coordinates to the provided coordinates, or Integer.MAX_VALUE if the provided
     *       coordinates are null
     */
    public int distance(final @Nullable Coords coordinates) {
        return (coordinates == null) ? Integer.MAX_VALUE : distance(coordinates.getX(), coordinates.getY());
    }

    /** Returns the distance to the coordinate given as distx, disty. */
    public int distance(int distx, int disty) {
        return distance(x, y, distx, disty);
    }

    /** Returns the distance to the coordinate given as distx, disty. */
    public static int distance(int originX, int originY, int distX, int distY) {
        // based on
        // http://www.rossmack.com/ab/RPG/traveller/AstroHexDistance.asp
        int xd = Math.abs(originX - distX);
        int yo = (xd / 2) + (!((originX & 1) == 1) && ((distX & 1) == 1) ? 1 : 0);
        int yMin = originY - yo;
        int yMax = yMin + xd;
        int ym = 0;
        if (distY < yMin) {
            ym = yMin - distY;
        }
        if (distY > yMax) {
            ym = distY - yMax;
        }
        return xd + ym;
    }

    /**
     * Returns a string representing a coordinate in "board number" format.
     */
    public String getBoardNum() {

        return (getX() > -1 && getX() < 9 ? "0" : "") +
                     (getX() + 1) +
                     (getY() > -1 && getY() < 9 ? "0" : "") +
                     (getY() + 1);
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
     * Returns an array of the Coords of hexes that are crossed by a straight line from the center of src to the center
     * of dest, including src and dest. The returned coordinates are in line order, and if the line passes directly
     * between two hexes, it returns them both. Based on the degree of the angle, the next hex is going to be one of
     * three hexes. We check those three hexes, sides first, add the first one that intersects and continue from there.
     * Based off of some of the formulas at Amit's game programming site.
     * <p>
     * <a href="http://www-cs-students.stanford.edu/~amitp/gameprog.html">Amit’s Game Programming Information</a>
     * <p>
     * Note: this function can return Coordinates that are not on the board.
     *
     * @param src  Starting point.
     * @param dest Ending Point.
     *
     * @return The list of intervening coordinates.
     */
    public static ArrayList<Coords> intervening(Coords src, Coords dest) {
        return intervening(src, dest, false);
    }

    /**
     * Returns an array of the Coords of hexes that are crossed by a straight line from the center of src to the center
     * of dest, including src and dest. The returned coordinates are in line order, and if the line passes directly
     * between two hexes, it returns them both. Based on the degree of the angle, the next hex is going to be one of
     * three hexes. We check those three hexes, sides first, add the first one that intersects and continue from there.
     * Based off of some of the formulas at Amit's game programming site.
     * <p>
     * <a href="http://www-cs-students.stanford.edu/~amitp/gameprog.html">Amit’s Game Programming Information</a>
     * <p>
     * Note: this function can return Coordinates that are not on the board.
     *
     * @param src   Starting point.
     * @param dest  Ending Point.
     * @param split Set TRUE to make left appear before right in the sequence reliably
     *
     * @return The list of intervening coordinates.
     */
    public static ArrayList<Coords> intervening(Coords src, Coords dest, boolean split) {
        IdealHex iSrc = IdealHex.get(src);
        IdealHex iDest = IdealHex.get(dest);

        int[] directions = new int[3];
        int centerDirection = src.direction(dest);

        if (split) {
            // HACK to make left appear before right in the sequence reliably
            centerDirection = (int) Math.round(src.radian(dest) + 0.0001 / HEXSIDE) % 6;
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
     * Returns the first further hex found along the line from the centers of src to dest. Checks the three directions
     * given and returns the closest. This relies on the side directions being given first. If it checked the center
     * first, it would end up missing the side hexes sometimes. Not the most elegant solution, but it works.
     */
    public static Coords nextHex(Coords current, IdealHex iSrc, IdealHex iDest, int[] directions) {
        for (int direction : directions) {
            Coords testing = current.translated(direction);
            if (IdealHex.get(testing).isIntersectedBy(iSrc.cx, iSrc.cy, iDest.cx, iDest.cy)) {
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
            } else {
                directions = new int[1];
                directions[0] = 3;
            }
        } else if (current.getX() > destination.getX()) {
            if (current.getY() > destination.getY()) {
                directions = new int[3];
                directions[0] = 4;
                directions[1] = 5;
            } else {
                directions = new int[3];
                directions[0] = 3;
                directions[1] = 4;
                directions[2] = 5;
            }
        } else {
            if (current.getY() > destination.getY()) {
                directions = new int[3];
                directions[1] = 1;
                directions[2] = 2;
            } else {
                directions = new int[3];
                directions[0] = 1;
                directions[1] = 2;
                directions[2] = 3;
            }
        }
        return nextHex(current, new IdealHex(current), new IdealHex(destination), directions);
    }

    /**
     * Returns true when the given other Coords are exactly on the hex row (line) from this Coords in the given
     * direction. For example, if the direction is 0 (north), returns true only for Coords that are above this Coords at
     * the same x. Returns false when the other Coords are null, the other Coords are equal to this or the direction is
     * outside 0 to 5.
     *
     * @param direction The direction, 0 = N, 2 = SE ...
     * @param other     The Coords to test
     *
     * @return True when the other Coords are on the hex row from this Coords in the given direction
     */
    public boolean isOnHexRow(int direction, @Nullable Coords other) {
        if ((other == null) || this.equals(other)) {
            return false;
        }
        HexLine line = new HexLine(this, direction);
        if (line.judgePoint(other) != 0) {
            return false;
        } else {
            return switch (direction) {
                case 0 -> other.y < y;
                case 1, 2 -> other.x > x;
                case 3 -> other.y > y;
                case 4, 5 -> other.x < x;
                default -> false;
            };
        }
    }

    /**
     * Returns a list of all adjacent coordinates (distance = 1), regardless of whether they're on the board or not.
     */
    public ArrayList<Coords> allAdjacent() {
        return (allAtDistance(1));
    }

    /**
     * Returns a list of all coordinates at the given distance (dist - 1) and anything less than dist as well.
     */
    public ArrayList<Coords> allLessThanDistance(int dist) {
        return allAtDistanceOrLess(dist - 1);
    }

    /**
     * Returns a list of all coordinates at the given distance dist and anything less than dist as well.
     */
    public ArrayList<Coords> allAtDistanceOrLess(int dist) {
        ArrayList<Coords> retVal = new ArrayList<>();

        for (int radius = 0; radius <= dist; radius++) {
            retVal.addAll(allAtDistance(radius));
        }

        return retVal;
    }

    /**
     * Returns a list of all coordinates at the given distance dist, regardless of whether they're on the board or not.
     * Returns an empty Set for dist &lt; 0 and the calling Coords itself for dist == 0.
     */
    public ArrayList<Coords> allAtDistance(int dist) {
        ArrayList<Coords> retVal = new ArrayList<>();

        if (dist == 0) {
            retVal.add(this);
        } else if (dist > 0) {
            // algorithm outline: travel to the southwest a number of hexes equal to the radius then, "draw" the hex
            // sides in sequence, moving north first to draw the west side, then rotating clockwise and moving
            // northeast to draw the northwest side and so on, until we circle around. The length of a hex side is
            // equivalent to the radius
            Coords currentHex = translated(4, dist);
            for (int direction = 0; direction < 6; direction++) {
                for (int translation = 0; translation < dist; translation++) {
                    currentHex = currentHex.translated(direction);
                    retVal.add(currentHex);
                }
            }
        }
        return retVal;
    }

    /**
     * this makes the coordinates 1 based instead of 0 based to match the tiles displayed on the grid.
     */
    public String toFriendlyString() {
        return "(" + (x + 1) + ", " + (y + 1) + ")";
    }

    /**
     * Returns the coordinates in TSV format for logging purposes
     *
     * @return the coordinates in TSV format `x`\t`y`
     */
    public String toTSV() {
        return x + "\t" + y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /**
     * return true if this is between s and e based on distance
     */
    public boolean between(Coords s, Coords e) {
        return (s.distance(e) == s.distance(this) + this.distance(e));
    }

    /**
     * @return CubeCoords representation of this Coords
     */
    public CubeCoords toCube() {
        int offset = -1;
        int q = x;
        int r = y - (int) ((x + offset * (x & 1)) / 2.0);
        int s = -q - r;
        return new CubeCoords(q, r, s);
    }

    public Coords subtract(Coords centroid) {
        return new Coords(x - centroid.x, y - centroid.y);
    }

    public Coords add(Coords centroid) {
        return new Coords(x + centroid.x, y + centroid.y);
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y);
    }

    /**
     * @deprecated No indicated uses.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public double cosineSimilarity(Coords other) {
        double dot = getX() * other.getX() + getY() * other.getY();
        double magA = magnitude();
        double magB = other.magnitude();
        if (magA == 0 || magB == 0) {
            return 0;
        }
        return dot / (magA * magB);
    }

    /**
     * Returns the hex code for this coordinate on the given board.
     *
     * @param board the board
     *
     * @return the hex code for this coordinate
     */
    public String hexCode(Board board) {
        return hexCode(this, board);
    }

    /**
     * Returns the hex code for the given coordinates on the given board.
     *
     * @param coords the coordinates
     * @param board  the board
     *
     * @return the hex code for the given coordinates
     */
    public static String hexCode(Coords coords, Board board) {
        return hexCode(coords.getX() + 1, coords.getY() + 1, board);
    }

    /**
     * Returns the hex code for the given coordinates.
     *
     * @param x     the x coordinate
     * @param y     the y coordinate
     * @param board the board
     *
     * @return the hex code for the given coordinates
     */
    public static String hexCode(int x, int y, Board board) {
        int maxSize = Math.max(board.getWidth(), board.getHeight());
        if (maxSize + 1 > 99) {
            return String.format("%03d%03d", x, y);
        }
        return String.format("%02d%02d", x, y);
    }
}
