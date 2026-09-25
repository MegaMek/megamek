/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
 * Catalyst Game Labs and the Catalyst Games Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.moves;

import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.units.MobileStructure;

/** Footprint geometry for TO:AUE p.35, including hexes touched between the ends of a pivot. */
public final class MobileStructureGeometry {
    private MobileStructureGeometry() { }

    public record Contact(CubeCoords hex, Coords boardHex, double time) { }

    public static List<CubeCoords> pivots(MobileStructure unit) {
        List<CubeCoords> hexes = unit.getPosition() == null ? unit.getInternalBuilding().getCoordsList()
              : MobileStructureLinkage.group(unit).stream().flatMap(m -> m.getCoordsList().stream())
                    .map(c -> MobileStructureLinkage.rotate(c.toCube().subtract(unit.getPosition().toCube()), -unit.getFacing()))
                    .distinct().toList();
        CubeCoords center = CubeCoords.mean(hexes).orElse(CubeCoords.ZERO);
        double nearest = hexes.stream().mapToDouble(h -> distanceSquared(h, center)).min().orElse(0);
        return hexes.stream().filter(h -> distanceSquared(h, center) <= nearest + 1e-8)
              .sorted(Comparator.comparingDouble(CubeCoords::q).thenComparingDouble(CubeCoords::r)).toList();
    }

    private static double distanceSquared(CubeCoords a, CubeCoords b) {
        CubeCoords d = a.subtract(b);
        return d.q() * d.q() + d.r() * d.r() + d.s() * d.s();
    }

    public static Coords pivotOrigin(MobileStructure unit, Coords from, int facing, int newFacing, CubeCoords pivot) {
        return from.toCube().add(MobileStructureLinkage.rotate(pivot, facing)
              .subtract(MobileStructureLinkage.rotate(pivot, newFacing))).toOffset();
    }

    public static CubeCoords pivotFor(MobileStructure unit, Coords from, int facing, Coords to, int newFacing) {
        return pivots(unit).stream().filter(h -> pivotOrigin(unit, from, facing, newFacing, h).equals(to))
              .findFirst().orElse(null);
    }

    public static List<Contact> contacts(MobileStructure unit, Coords from, int facing, Coords to, int newFacing) {
        Map<CubeCoords, Coords> start = unit.computeLayoutForPositionAndFacing(from, facing);
        Map<CubeCoords, Coords> end = unit.computeLayoutForPositionAndFacing(to, newFacing);
        if (facing == newFacing) {
            return end.entrySet().stream().filter(e -> !e.getValue().equals(start.get(e.getKey())))
                  .map(e -> new Contact(e.getKey(), e.getValue(), 1)).toList();
        }
        CubeCoords pivot = pivotFor(unit, from, facing, to, newFacing);
        if (pivot == null) {
            return List.of();
        }
        Point2D.Double center = point(from.toCube().add(MobileStructureLinkage.rotate(pivot, facing)));
        double angle = Math.floorMod(newFacing - facing, 6) == 1 ? Math.PI / 3 : -Math.PI / 3;
        List<Contact> result = new ArrayList<>();
        for (var entry : start.entrySet()) {
            Point2D.Double source = point(entry.getValue().toCube());
            Area swept = sweep(source, center, angle);
            var bounds = swept.getBounds2D();
            int qMin = (int) Math.floor(bounds.getMinX() / 1.5) - 1;
            int qMax = (int) Math.ceil(bounds.getMaxX() / 1.5) + 1;
            for (int q = qMin; q <= qMax; q++) {
                int rMin = (int) Math.floor(bounds.getMinY() / Math.sqrt(3) - q / 2.0) - 1;
                int rMax = (int) Math.ceil(bounds.getMaxY() / Math.sqrt(3) - q / 2.0) + 1;
                for (int r = rMin; r <= rMax; r++) {
                    Coords candidate = new CubeCoords(q, r, -q - r).toOffset();
                    if (candidate.equals(entry.getValue())) {
                        continue;
                    }
                    Area cell = hex(point(candidate.toCube()), .999999);
                    if (!intersects(swept, cell)) {
                        continue;
                    }
                    // Find first contact with the continuous sweep, rather than testing a few rotated templates.
                    double lo = 0;
                    double hi = 1;
                    for (int n = 0; n < 16; n++) {
                        double mid = (lo + hi) / 2;
                        if (intersects(sweep(source, center, angle * mid), cell)) {
                            hi = mid;
                        } else {
                            lo = mid;
                        }
                    }
                    result.add(new Contact(entry.getKey(), candidate, hi));
                }
            }
        }
        result.sort(Comparator.comparingDouble(Contact::time));
        return result;
    }

    private static boolean intersects(Area a, Area b) {
        Area intersection = new Area(a);
        intersection.intersect(b);
        return !intersection.isEmpty();
    }

    private static Point2D.Double point(CubeCoords h) {
        return new Point2D.Double(1.5 * h.q(), Math.sqrt(3) * (h.r() + h.q() / 2));
    }

    private static Area hex(Point2D.Double center, double radius) {
        Path2D.Double path = new Path2D.Double();
        for (int vertex = 0; vertex < 6; vertex++) {
            double x = center.x + radius * Math.cos(vertex * Math.PI / 3);
            double y = center.y + radius * Math.sin(vertex * Math.PI / 3);
            if (vertex == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.closePath();
        return new Area(path);
    }

    private static Area sweep(Point2D.Double source, Point2D.Double center, double angle) {
        Area result = hex(source, 1);
        for (int vertex = 0; vertex < 6; vertex++) {
            Point2D.Double a = new Point2D.Double(source.x + Math.cos(vertex * Math.PI / 3),
                  source.y + Math.sin(vertex * Math.PI / 3));
            Point2D.Double b = new Point2D.Double(source.x + Math.cos((vertex + 1) * Math.PI / 3),
                  source.y + Math.sin((vertex + 1) * Math.PI / 3));
            Path2D.Double edge = new Path2D.Double();
            edge.moveTo(a.x, a.y);
            edge.lineTo(b.x, b.y);
            appendArc(edge, b, center, 0, angle);
            double dx = a.x - center.x;
            double dy = a.y - center.y;
            edge.lineTo(center.x + dx * Math.cos(angle) - dy * Math.sin(angle),
                  center.y + dx * Math.sin(angle) + dy * Math.cos(angle));
            appendArc(edge, a, center, angle, -angle);
            edge.closePath();
            result.add(new Area(edge));
        }
        return result;
    }

    private static void appendArc(Path2D.Double path, Point2D.Double vertex, Point2D.Double center,
          double start, double angle) {
        double radius = vertex.distance(center);
        if (radius > 1e-10) {
            path.append(new Arc2D.Double(center.x - radius, center.y - radius, 2 * radius, 2 * radius,
                  -Math.toDegrees(Math.atan2(vertex.y - center.y, vertex.x - center.x) + start),
                  -Math.toDegrees(angle), Arc2D.OPEN), true);
        }
    }
}
