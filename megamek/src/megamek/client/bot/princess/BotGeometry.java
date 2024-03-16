/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;

import megamek.common.Board;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.MovePath;
import org.apache.logging.log4j.LogManager;

import java.util.Arrays;
import java.util.Iterator;

/**
 * This contains useful classes and functions for geometric questions
 * the bot algorithm might have
 */
public class BotGeometry {

    /**
     * The combination of a coordinate and a facing
     */
    public static class CoordFacingCombo {

        private Coords coords;
        private int facing;

        private CoordFacingCombo(Coords c, int f) {
            setCoords(c);
            setFacing(f);
        }

        static CoordFacingCombo createCoordFacingCombo(Coords c, int f) {
            return new CoordFacingCombo(c, f);
        }

        static CoordFacingCombo createCoordFacingCombo(Entity e) {
            if (e == null) {
                return null;
            }
            return createCoordFacingCombo(e.getPosition(), e.getFacing());
        }

        static CoordFacingCombo createCoordFacingCombo(MovePath p) {
            if (p == null) {
                return null;
            }
            return createCoordFacingCombo(p.getFinalCoords(), p.getFinalFacing());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CoordFacingCombo)) {
                return false;
            }
            CoordFacingCombo c = (CoordFacingCombo) o;
            return !(getCoords() == null || !getCoords().equals(c.getCoords())) && getFacing() == c.getFacing();
        }

        @Override
        public int hashCode() {
            return (getCoords().hashCode() * 6) + getFacing();
        }

        Coords getCoords() {
            return coords;
        }

        void setCoords(Coords coords) {
            this.coords = coords;
        }

        int getFacing() {
            return facing;
        }

        void setFacing(int facing) {
            this.facing = facing;
        }

        @Override
        public String toString() {
            return "Facing " + getFacing() + "; " + (getCoords() == null ? "null" : getCoords().toString());
        }
    }

    /**
     * This describes a line in one of the 6 directions in board space
     * ---copied from Coords---
     * Coords stores x and y values. Since these are hexes, coordinates with odd x
     * values are a half-hex down. Directions work clockwise around the hex,
     * starting with zero at the top.
     *        -y
     *        0
     *      _____
     *   5 /     \ 1
     * -x /       \ +x
     *    \       /
     *   4 \_____/ 2
     *        3
     *        +y
     * ------------------------------
     * Direction is stored as above, but the meaning of 'intercept' depends
     * on the direction.  For directions 0, 3, intercept means the y=0 intercept
     * for directions 1, 2, 4, 5 intercept is the x=0 intercept
     */
    public static class HexLine {
        private int intercept;
        private int direction;

        /**
         * Create a hexline from a point and direction
         */
        public HexLine(Coords c, int dir) {
            setDirection(dir);
            if ((getDirection() == 0) || (getDirection() == 3)) {
                setIntercept(c.getX());
            } else if ((getDirection() == 1) || (getDirection() == 4)) {
                setIntercept(c.getY() + ((c.getX() + 1) / 2));
            } else {// direction==2||direction==5
                setIntercept(c.getY() - ((c.getX()) / 2));
            }
        }

        /**
         * returns -1 if the point is to the left of the line
         * +1 if the point is to the right of the line
         * and 0 if the point is on the line
         */
        public int judgePoint(Coords c) {
            HexLine comparor = new HexLine(c, getDirection());
            if (comparor.getIntercept() < getIntercept()) {
                return (getDirection() < 3) ? -1 : 1;
            } else if (comparor.getIntercept() > getIntercept()) {
                return (getDirection() < 3) ? 1 : -1;
            }
            
            return 0;
        }

        /**
         * returns -1 if the area is entirely to the left of the line
         * returns +1 if the area is entirely to the right of the line
         * returns 0 if the area is divided by the line
         */
        public int judgeArea(ConvexBoardArea a) {
            boolean flip = getDirection() > 2;
            HexLine[] edges = a.getEdges();
            if ((edges[getDirection()] == null) || (edges[(getDirection() + 3) % 6] == null)) {
                LogManager.getLogger().error("Detection of NULL edges in ConvexBoardArea: " + a);
                return 0;
            }
            if (edges[getDirection()].getIntercept() == getIntercept()) {
                return 0;
            }
            if (edges[(getDirection() + 3) % 6].getIntercept() == getIntercept()) {
                return 0;
            }
            boolean edgeone = (edges[getDirection()].getIntercept() < getIntercept()) ^ flip;
            boolean edgetwo = (edges[(getDirection() + 3) % 6].getIntercept() < getIntercept()) ^ flip;
            if (edgeone && edgetwo) {
                return 1;
            }
            if ((!edgeone) && (!edgetwo)) {
                return -1;
            }
            return 0;
        }

        /**
         * This function only makes sense for directions 1, 2, 4, 5
         * Note that the function getXfromY would be multvalued
         */
        public int getYfromX(int x) {
            if ((getDirection() == 0) || (getDirection() == 3)) {
                return 0;
            }
            if ((getDirection() == 1) || (getDirection() == 4)) {
                return getIntercept() - ((x + 1) / 2); //halfs round down
            }
            // direction==5||direction==2
            return getIntercept() + ((x) / 2);     //halfs round down
        }

        /**
         * Returns the intersection point with another line
         * if lines are parallel (even if they are coincident) returns null
         */
        public Coords getIntersection(HexLine h) {
            if ((h.getDirection() % 3) == (getDirection() % 3)) {
                return null;
            }
            if (h.getDirection() == 0) {
                return h.getIntersection(this);
            }
            if (getDirection() == 2) {
                return h.getIntersection(this);
            }
            if (getDirection() == 0 || getDirection() == 3) {
                return new Coords(getIntercept(), h.getYfromX(getIntercept()));
            }
            // direction must be 1 here, and h.direction=2
            return new Coords(getIntercept() - h.getIntercept(), getYfromX(getIntercept() - h.getIntercept()));
        }

        /**
         * Returns the (euclidian distance) closest point on this
         * line to another point
         */
        public Coords getClosestPoint(Coords c) {
            if ((getDirection() == 0) || (getDirection() == 3)) { //technically two points are equidistant,
                // but who's counting
                return new Coords(getIntercept(), c.getY());
            } else if ((getDirection() == 1) || (getDirection() == 4)) {
                double myx = (-2.0 / 3.0) * (getIntercept() - 0.5 - c.getY() - (2.0 * c.getX()));
                return new Coords((int) myx, getYfromX((int) myx));
            }
            double myx = (-5.0 / 3.0) * (getIntercept() - (double) c.getY() - (2.0 * c.getX()));
            return new Coords((int) myx, getYfromX((int) myx));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof HexLine)) {
                return false;
            }

            HexLine hexLine = (HexLine) o;

            if (getDirection() != hexLine.getDirection()) {
                return false;
            }
            //noinspection RedundantIfStatement
            if (getIntercept() != hexLine.getIntercept()) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = getIntercept();
            result = 31 * result + getDirection();
            return result;
        }

        int getIntercept() {
            return intercept;
        }

        void setIntercept(int intercept) {
            this.intercept = intercept;
        }

        int getDirection() {
            return direction;
        }

        void setDirection(int direction) {
            this.direction = direction;
        }

        @Override
        public String toString() {
            return "Intercept " + getIntercept() + ", Direction " + getDirection();
        }
    }

    /**
     * This is a convex area on the board made up of 6 lines lying along one of the
     * 3 primary directions of a hex map
     */
    public static class ConvexBoardArea {
        // left/right indicates whether it's the small x or large x line
        //     HexLine[] left = new HexLine[3];
        //     HexLine[] right = new HexLine[3];
        // edge points to the previous lines in the right order
        private HexLine[] edges = new HexLine[6];
        private Coords[] vertices = new Coords[6];
        
        ConvexBoardArea() {

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof ConvexBoardArea)) {
                return false;
            }

            ConvexBoardArea that = (ConvexBoardArea) o;

            //noinspection RedundantIfStatement
            if (!Arrays.equals(edges, that.edges)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(edges);
        }

        @Override
        public String toString() {
            StringBuilder msg = new StringBuilder("Edges:");
            HexLine[] edges = getEdges();
            for (int i = 0; i < edges.length; i++) {
                if (i != 0) {
                    msg.append("; ");
                }
                if (edges[i] == null) {
                    msg.append("null");
                } else {
                    msg.append(edges[i].toString());
                }
            }
            return msg.toString();
        }

        void addCoordFacingCombos(Iterator<CoordFacingCombo> cfit, Board board) {
            while (cfit.hasNext()) {
                CoordFacingCombo cf = cfit.next();
                if ((cf != null) && board.contains(cf.coords)) {
                    expandToInclude(cf.getCoords());
                }
            }
        }

        /**
         * returns true if a point is inside the area
         * false if it is not
         */
        public boolean contains(Coords c) {
            HexLine[] edges = getEdges();
            if (edges[0] == null) {
                return false;
            }
            for (int i = 0; i < 6; i++) {
                if (edges[i].judgePoint(c) > 0) {
                    return false;
                }
            }
            return true;
        }

        /**
         * expands the board area to include point onc
         */
        void expandToInclude(Coords onc) {
            HexLine[] edges = getEdges();
            for (int i = 0; i < 6; i++) {
                if ((edges[i] == null) || (edges[i].judgePoint(onc) > 0)) {
                    edges[i] = new HexLine(onc, i);
                }
            }
            setEdges(edges);
        }

        /**
         * Returns a vertex, with zero starting at the upper left of the hex
         */
        public Coords getVertexNum(int i) {
            if (vertices[i] != null) {
                return vertices[i];
            }
            
            HexLine[] edges = getEdges();
            if (edges[i] == null || edges[(i + 1) % 6] == null) {
                LogManager.getLogger().error("Edge[" + i + "] is NULL.");
                return null;
            }
            
            vertices[i] = edges[i].getIntersection(edges[(i + 1) % 6]);
            return vertices[i];
        }

        /**
         * returns the closest coord in the area to the given coord
         */
        public Coords getClosestCoordsTo(Coords c) {
            Coords closest = null;
            int closest_dist = 0;
            HexLine[] edges = getEdges();
            for (int i = 0; i < 6; i++) {
                if (edges[i] == null) {
                    continue;
                }
                if (edges[i].judgePoint(c) > 0) {
                    Coords vert = getVertexNum(i);
                    int vdist = vert.distance(c);
                    if ((closest == null) || (vdist < closest_dist)) {
                        closest = vert;
                        closest_dist = vdist;
                    }
                    Coords online = edges[i].getClosestPoint(c);
                    if (contains(online)) {
                        int ldist = online.distance(c);
                        if (ldist < closest_dist) {
                            closest = online;
                            closest_dist = ldist;
                        }
                    }
                }
            }
            if (closest == null) {
                return new Coords(c.getX(), c.getY());
            }
            return closest;
        }

        public HexLine[] getEdges() {
            return edges;
        }

        void setEdges(HexLine[] edges) {
            if (edges == null) {
                throw new IllegalArgumentException("Edges cannot be NULL, but it's members can.");
            }
            if (edges.length != 6) {
                throw new IllegalArgumentException("Edges must have exactly 6 members.");
            }

            this.edges = edges;
            vertices = new Coords[6];
        }

        void clearEdges() {
            setEdges(new HexLine[6]);
        }
    }

    /**
     * runs a series of self tests to make sure geometry is done correctly
     */
    static void debugSelfTest(Princess owner) {
        final String PASSED = "passed";
        final String FAILED = "failed";

        StringBuilder msg = new StringBuilder("Performing self test of geometry");

        try {
            Coords center = new Coords(4, 6);
            HexLine[] lines = new HexLine[6];
            for (int i = 0; i < 6; i++) {
                lines[i] = new HexLine(center, i);
            }

            msg.append("\n\tTesting that center lies in lines... ");
            boolean passed = true;
            for (int i = 0; i < 6; i++) {
                if (lines[i].judgePoint(center) != 0) {
                    passed = false;
                }
            }
            msg.append(passed ? PASSED : FAILED);

            msg.append("\n\tTesting more points that should lie on lines... ");
            passed = true;
            for (int i = 0; i < 6; i++) {
                if ((lines[i].judgePoint(center.translated(i)) != 0) || (lines[i].judgePoint(center.translated((i +
                                                                                                                3) %
                                                                                                               6)) !=
                                                                         0)) {
                    passed = false;
                }
            }
            msg.append(passed ? PASSED : FAILED);

            passed = true;
            msg.append("\n\tTesting points to left and right of lines... ");
            for (int i = 0; i < 6; i++) {
                if (-1 != lines[i].judgePoint(center.translated((i + 5) % 6))) {
                    passed = false;
                }
                if (-1 != lines[i].judgePoint(center.translated((i + 4) % 6))) {
                    passed = false;
                }
                if (1 != lines[i].judgePoint(center.translated((i + 1) % 6))) {
                    passed = false;
                }
                if (1 != lines[i].judgePoint(center.translated((i + 2) % 6))) {
                    passed = false;
                }
            }
            msg.append(passed ? PASSED : FAILED);

            passed = true;
            Coords areapt1 = new Coords(1, 1);
            Coords areapt2 = new Coords(3, 1);
            Coords areapt3 = new Coords(2, 3);
            ConvexBoardArea area = new ConvexBoardArea();
            area.expandToInclude(areapt1);
            area.expandToInclude(areapt2);
            area.expandToInclude(areapt3);
            LogManager.getLogger().debug("Checking area contains proper points... ");
            msg.append("\n\tChecking area contains proper points... ");
            if (!area.contains(new Coords(1, 1))) {
                passed = false;
            }
            if (!area.contains(new Coords(2, 1))) {
                passed = false;
            }
            if (!area.contains(new Coords(3, 1))) {
                passed = false;
            }
            if (!area.contains(new Coords(1, 2))) {
                passed = false;
            }
            if (!area.contains(new Coords(2, 2))) {
                passed = false;
            }
            if (!area.contains(new Coords(3, 2))) {
                passed = false;
            }
            if (!area.contains(new Coords(2, 3))) {
                passed = false;
            }
            msg.append(passed ? PASSED : FAILED);

            passed = true;
            msg.append("\n\tChecking area doesn't contain extra points... ");
            if (area.contains(new Coords(0, 1))) {
                passed = false;
            }
            if (area.contains(new Coords(1, 0))) {
                passed = false;
            }
            if (area.contains(new Coords(2, 0))) {
                passed = false;
            }
            if (area.contains(new Coords(3, 0))) {
                passed = false;
            }
            if (area.contains(new Coords(4, 1))) {
                passed = false;
            }
            if (area.contains(new Coords(4, 2))) {
                passed = false;
            }
            if (area.contains(new Coords(4, 3))) {
                passed = false;
            }
            if (area.contains(new Coords(3, 3))) {
                passed = false;
            }
            if (area.contains(new Coords(2, 4))) {
                passed = false;
            }
            if (area.contains(new Coords(1, 3))) {
                passed = false;
            }
            if (area.contains(new Coords(0, 3))) {
                passed = false;
            }
            if (area.contains(new Coords(0, 2))) {
                passed = false;
            }
            msg.append(passed ? PASSED : FAILED);

        } finally {
            LogManager.getLogger().debug(msg.toString());
        }
    }
}
