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

import java.util.Arrays;
import java.util.Iterator;

import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.logging.MMLogger;

/**
 * This is a convex area on the board made up of 6 lines lying along one of the 3 primary directions of a hex map
 */
public class ConvexBoardArea {
    private final static MMLogger LOGGER = MMLogger.create(ConvexBoardArea.class);

    // left/right indicates whether it's the small x or large x line
    // HexLine[] left = new HexLine[3];
    // HexLine[] right = new HexLine[3];
    // edge points to the previous lines in the right order
    private HexLine[] edges = new HexLine[6];
    private Coords[] vertices = new Coords[6];

    public ConvexBoardArea() {

    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof ConvexBoardArea)) {
            return false;
        }

        ConvexBoardArea that = (ConvexBoardArea) object;

        // noinspection RedundantIfStatement
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

    public void addCoordFacingCombos(Iterator<CoordFacingCombo> coordFacingComboIterator, Board board) {
        while (coordFacingComboIterator.hasNext()) {
            CoordFacingCombo coordFacingCombo = coordFacingComboIterator.next();
            if ((coordFacingCombo != null) && board.contains(coordFacingCombo.getCoords())) {
                expandToInclude(coordFacingCombo.getCoords());
            }
        }
    }

    /**
     * returns true if a point is inside the area false if it is not
     */
    public boolean contains(Coords coords) {
        HexLine[] edges = getEdges();
        if (edges[0] == null) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            if (edges[i].judgePoint(coords) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * expands the board area to include point coords
     */
    public void expandToInclude(Coords coords) {
        HexLine[] edges = getEdges();
        for (int i = 0; i < 6; i++) {
            if ((edges[i] == null) || (edges[i].judgePoint(coords) > 0)) {
                edges[i] = new HexLine(coords, i);
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
            LOGGER.error("Edge[{}] is NULL.", i);
            return null;
        }

        vertices[i] = edges[i].getIntersection(edges[(i + 1) % 6]);
        return vertices[i];
    }

    /**
     * returns the closest coord in the area to the given coord
     */
    public Coords getClosestCoordsTo(Coords coords) {
        Coords closest = null;
        int closest_dist = 0;
        HexLine[] edges = getEdges();
        for (int i = 0; i < 6; i++) {
            if (edges[i] == null) {
                continue;
            }
            if (edges[i].judgePoint(coords) > 0) {
                Coords vert = getVertexNum(i);
                int verticalDistance = vert.distance(coords);
                if ((closest == null) || (verticalDistance < closest_dist)) {
                    closest = vert;
                    closest_dist = verticalDistance;
                }
                Coords online = edges[i].getClosestPoint(coords);
                if (contains(online)) {
                    int distance = online.distance(coords);
                    if (distance < closest_dist) {
                        closest = online;
                        closest_dist = distance;
                    }
                }
            }
        }
        if (closest == null) {
            return new Coords(coords.getX(), coords.getY());
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
