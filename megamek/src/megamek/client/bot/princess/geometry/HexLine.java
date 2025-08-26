/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.bot.princess.geometry;

import megamek.common.board.Coords;
import megamek.logging.MMLogger;

/**
 * This describes a line in one of the 6 directions in board space ---copied from Coords--- Coords stores x and y
 * values. Since these are hexes, coordinates with odd x values are a half-hex down. Directions work clockwise around
 * the hex, starting with zero at the top.
 * <pre>
 *       -y
 *        0
 *      _____
 *   5 /     \ 1
 * -x /       \ +x
 *    \       /
 *   4 \_____/ 2
 *        3
 *       +y
 * </pre>
 * ------------------------------
 * <BR>Direction is stored as above, but the meaning of 'intercept' depends
 * on the direction. For directions 0, 3, intercept means the y=0 intercept for directions 1, 2, 4, 5 intercept is the
 * x=0 intercept
 */
public class HexLine {
    private final static MMLogger LOGGER = MMLogger.create(HexLine.class);

    private int intercept;
    private int direction;

    /**
     * Create a {@link HexLine} from a point and direction
     */
    public HexLine(Coords coords, int dir) {
        setDirection(dir);
        if ((getDirection() == 0) || (getDirection() == 3)) {
            setIntercept(coords.getX());
        } else if ((getDirection() == 1) || (getDirection() == 4)) {
            setIntercept(coords.getY() + ((coords.getX() + 1) / 2));
        } else {// direction==2||direction==5
            setIntercept(coords.getY() - ((coords.getX()) / 2));
        }
    }

    /**
     * returns -1 if the point is to the left of the line +1 if the point is to the right of the line and 0 if the point
     * is on the line Note that this evaluation depends on the "view" direction of this line. The result is reversed for
     * HexLines of opposite directions, e.g. directions 0 and 3.
     */
    public int judgePoint(Coords coords) {
        HexLine comparor = new HexLine(coords, getDirection());
        if (comparor.getIntercept() < getIntercept()) {
            return (getDirection() < 3) ? -1 : 1;
        } else if (comparor.getIntercept() > getIntercept()) {
            return (getDirection() < 3) ? 1 : -1;
        }

        return 0;
    }

    /**
     * @return -1 if the point is to the left of the line, +1 if the point is to the right of the line and 0 if the
     *       point is on the line Note that this evaluation is independent of the "view" direction of this line. The
     *       result is the same for HexLines of opposite directions, e.g. directions 0 and 3.
     */
    public int isAbsoluteLeftOrRight(Coords coords) {
        HexLine comparor = new HexLine(coords, getDirection());
        if (comparor.getIntercept() == getIntercept()) {
            return 0;
        } else if (comparor.getIntercept() < getIntercept()) {
            return ((getDirection() == 2) || (getDirection() == 5)) ? 1 : -1;
        } else {
            return ((getDirection() == 2) || (getDirection() == 5)) ? -1 : 1;
        }
    }

    /**
     * returns -1 if the area is entirely to the left of the line returns +1 if the area is entirely to the right of the
     * line returns 0 if the area is divided by the line
     */
    public int judgeArea(ConvexBoardArea convexBoardArea) {
        boolean flip = getDirection() > 2;
        HexLine[] edges = convexBoardArea.getEdges();
        if ((edges[getDirection()] == null) || (edges[(getDirection() + 3) % 6] == null)) {
            LOGGER.error("Detection of NULL edges in ConvexBoardArea: {}", convexBoardArea);
            return 0;
        }
        if (edges[getDirection()].getIntercept() == getIntercept()) {
            return 0;
        }
        if (edges[(getDirection() + 3) % 6].getIntercept() == getIntercept()) {
            return 0;
        }
        boolean edgeOne = (edges[getDirection()].getIntercept() < getIntercept()) ^ flip;
        boolean edgeTwo = (edges[(getDirection() + 3) % 6].getIntercept() < getIntercept()) ^ flip;
        if (edgeOne && edgeTwo) {
            return 1;
        }
        if ((!edgeOne) && (!edgeTwo)) {
            return -1;
        }
        return 0;
    }

    /**
     * This function only makes sense for directions 1, 2, 4, 5 Note that the function getXFromY would be multivalued
     */
    public int getYFromX(int x) {
        if ((getDirection() == 0) || (getDirection() == 3)) {
            return 0;
        }
        if ((getDirection() == 1) || (getDirection() == 4)) {
            return getIntercept() - ((x + 1) / 2); // half's round down
        }
        // direction==5||direction==2
        return getIntercept() + ((x) / 2); // half's round down
    }

    /**
     * Returns the intersection point with another line if lines are parallel (even if they are coincident) returns
     * null
     */
    public Coords getIntersection(HexLine hexLine) {
        if ((hexLine.getDirection() % 3) == (getDirection() % 3)) {
            return null;
        }
        if (hexLine.getDirection() == 0) {
            return hexLine.getIntersection(this);
        }
        if (getDirection() == 2) {
            return hexLine.getIntersection(this);
        }
        if (getDirection() == 0 || getDirection() == 3) {
            return new Coords(getIntercept(), hexLine.getYFromX(getIntercept()));
        }
        // direction must be 1 here, and hexLine.direction=2
        return new Coords(getIntercept() - hexLine.getIntercept(),
              getYFromX(getIntercept() - hexLine.getIntercept()));
    }

    /**
     * Returns the (Euclidean distance) the closest point on this line to another point
     */
    public Coords getClosestPoint(Coords coords) {
        if ((getDirection() == 0) || (getDirection() == 3)) { // technically two points are equidistant,
            // but who's counting
            return new Coords(getIntercept(), coords.getY());
        } else if ((getDirection() == 1) || (getDirection() == 4)) {
            double myx = (-2.0 / 3.0) * (getIntercept() - 0.5 - coords.getY() - (2.0 * coords.getX()));
            return new Coords((int) myx, getYFromX((int) myx));
        }
        double myx = (-5.0 / 3.0) * (getIntercept() - (double) coords.getY() - (2.0 * coords.getX()));
        return new Coords((int) myx, getYFromX((int) myx));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof HexLine hexLine)) {
            return false;
        }

        if (getDirection() != hexLine.getDirection()) {
            return false;
        }
        // noinspection RedundantIfStatement
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
