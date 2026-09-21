/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Vector3;

/** The actual topography of one hex. Rendering, road continuity, banks and picking share it. */
final class BoardSurface {
    /** GL-view-owned derived geometry. Pose snapshots share tiles; a board edit or tuning change invalidates it. */
    static final class Cache {
        private List<BoardScene.Tile> tiles;
        private int revision = -1;
        private final java.util.Map<BoardScene.Tile, BoardSurface> surfaces = new java.util.IdentityHashMap<>();

        BoardSurface get(BoardScene scene, BoardScene.Tile tile) {
            if (tiles != scene.tiles() || revision != BoardGeometry.revision()) {
                clear();
                tiles = scene.tiles();
                revision = BoardGeometry.revision();
            }
            return surfaces.computeIfAbsent(tile, key -> new BoardSurface(scene, key));
        }

        void clear() { surfaces.clear(); tiles = null; }
    }
    private static final int SHORE_SEGMENTS = 6;
    enum Finish { TOP, SHORE, BED, BANK, ICE }
    /** landEdge identifies the adjoining dry hex for bank artwork; other faces use -1. */
    record Face(Vector3 a, Vector3 b, Vector3 c, Finish finish, int landEdge) {
        Face(Vector3 a, Vector3 b, Vector3 c, Finish finish) {
            this(a, b, c, finish, -1);
        }
    }
    record Side(Vector3 a, Vector3 b, float lowA, float lowB, int edge) { }

    final BoardScene.Tile tile;
    final List<Face> faces = new ArrayList<>();
    final List<Vector3> water = new ArrayList<>();
    final List<Face> waterFaces = new ArrayList<>();
    final List<Side> waterfalls = new ArrayList<>();
    final int ramps;
    private final Vector3 center;
    private final Vector3[] corners = new Vector3[6];

    BoardSurface(BoardScene scene, BoardScene.Tile tile) {
        this.tile = tile;
        center = BoardGeometry.center(tile.coords(), tile.elevation());
        for (int edge = 0; edge < 6; edge++) {
            corners[edge] = BoardGeometry.corner(tile.coords(), tile.elevation(), edge);
        }
        int exits = 0;
        for (int direction = 0; direction < 6; direction++) {
            BoardScene.Tile neighbor = scene.tile(tile.coords().translated(direction));
            if (roadEdgeElevation(tile, neighbor, direction) != tile.elevation()) {
                exits |= 1 << direction;
            }
        }
        ramps = exits;
        if (tile.liquid().present()) {
            river(scene);
        } else if (ramps != 0) {
            road(scene);
        } else {
            fan(corners, center.z, Finish.TOP);
        }
    }

    /** A bridge approach reaches the deck at the edge; ordinary roads share their height change across both hexes. */
    static float roadEdgeElevation(BoardScene.Tile tile, BoardScene.Tile neighbor, int direction) {
        if (neighbor == null || tile.liquid().present()) {
            return tile.elevation();
        }
        var bridge = connectingBridge(tile, neighbor, direction);
        if (bridge != null) {
            return neighbor.elevation() + bridge.elevation();
        }
        return hasRoadApproach(tile, neighbor, direction)
              ? (tile.elevation() + neighbor.elevation()) / 2f : tile.elevation();
    }

    /** Presentation only: a road end can meet unpaved ground across at most two levels. */
    static boolean hasRoadApproach(BoardScene.Tile tile, BoardScene.Tile neighbor, int direction) {
        if (neighbor == null || tile.liquid().present() || neighbor.liquid().present()) {
            return false;
        }
        boolean exit = (tile.roadExits() & (1 << direction)) != 0;
        int reverse = (direction + 3) % 6;
        boolean continuation = (neighbor.roadExits() & (1 << reverse)) != 0;
        // A bridge approach changes only the road hex, leaving the ground beneath the deck intact.
        if (connectingBridge(tile, neighbor, direction) != null
              || connectingBridge(neighbor, tile, reverse) != null) {
            return false;
        }
        return (exit && continuation)
              || ((exit || continuation) && Math.abs(tile.elevation() - neighbor.elevation()) <= 2);
    }

    private static BoardScene.Feature connectingBridge(BoardScene.Tile road, BoardScene.Tile bridge, int direction) {
        if (road.liquid().present() || (road.roadExits() & (1 << direction)) == 0) {
            return null;
        }
        int reverse = (direction + 3) % 6;
        boolean continuation = !bridge.liquid().present() && (bridge.roadExits() & (1 << reverse)) != 0;
        if (continuation && road.elevation() == bridge.elevation()) {
            return null;
        }
        // A level deck wins over a sloped ground road; a sloped deck is the last connected-road fallback.
        // Captured bridge arms point north before rotation; hex directions run clockwise.
        return bridge.features().stream().filter(feature -> feature.asset().equals("bridge")
              && Math.abs(bridge.elevation() + feature.elevation() - road.elevation()) <= 1
              && (!continuation || bridge.elevation() + feature.elevation() == road.elevation())
              && Math.floorMod(Math.round(-feature.rotation() / 60), 6) == reverse).findFirst().orElse(null);
    }

    private void river(BoardScene scene) {
        float[] shore = new float[6];
        List<Integer> mouths = new ArrayList<>();
        for (int edge = 0; edge < 6; edge++) {
            BoardScene.Tile neighbor = scene.tile(tile.coords().translated(BoardGeometry.edgeDirection(edge)));
            shore[edge] = neighbor == null || !tile.liquid().connects(neighbor.liquid()) ? 8 * BoardGeometry.HEX_SCALE : 0;
            if (shore[edge] == 0) {
                mouths.add(edge);
            }
        }
        boolean channel = mouths.size() == 2 && mouths.getLast() - mouths.getFirst() >= 2
              && mouths.getLast() - mouths.getFirst() <= 4;
        List<Integer> channelMouths = channel ? mouths : List.of();
        Vector3[] waterline = curvedContour(shore, BoardGeometry.waterZ(tile), channelMouths);
        float[] submerged = shore.clone();
        for (int i = 0; i < 6; i++) {
            if (submerged[i] > 0) {
                // Narrow channels need steeper submerged banks to retain full depth beneath their hex centre.
                submerged[i] += (channel ? 2 : 6) * BoardGeometry.HEX_SCALE;
            }
        }
        Vector3[] bed = curvedContour(submerged, BoardGeometry.groundZ(tile), channelMouths);
        polygon(bed, Finish.BED, faces);
        for (int edge = 0; edge < 6; edge++) {
            if (shore[edge] == 0) {
                continue; // An open river mouth has no bank or wall across it.
            }
            for (int segment = 0; segment < SHORE_SEGMENTS; segment++) {
                int index = edge * SHORE_SEGMENTS + segment;
                int next = (index + 1) % waterline.length;
                Vector3 a = new Vector3(corners[edge]).lerp(corners[(edge + 1) % 6], segment / (float) SHORE_SEGMENTS);
                Vector3 b = new Vector3(corners[edge]).lerp(corners[(edge + 1) % 6], (segment + 1f) / SHORE_SEGMENTS);
                Vector3 lipA = shoreLip(waterline[index], a);
                Vector3 lipB = shoreLip(waterline[next], b);
                quad(a, b, lipB, lipA, Finish.TOP, edge);
                quad(lipA, lipB, waterline[next], waterline[index], Finish.SHORE, edge);
                quad(waterline[index], waterline[next], bed[next], bed[index], Finish.BANK);
            }
        }
        if (tile.frozen()) {
            fan(corners, center.z, Finish.ICE);
        } else {
            water.addAll(List.of(waterline));
            polygon(waterline, Finish.TOP, waterFaces);
            for (int edge = 0; edge < 6; edge++) {
                BoardScene.Tile neighbor = scene.tile(tile.coords().translated(BoardGeometry.edgeDirection(edge)));
                if (shore[edge] == 0 && !neighbor.frozen() && tile.elevation() > neighbor.elevation()) {
                    float bottom = BoardGeometry.waterZ(neighbor);
                    waterfalls.add(new Side(waterline[edge * SHORE_SEGMENTS],
                          waterline[((edge + 1) % 6) * SHORE_SEGMENTS], bottom, bottom, edge));
                }
            }
        }
    }

    private Vector3 shoreLip(Vector3 waterline, Vector3 boundary) {
        Vector3 lip = new Vector3(waterline).lerp(boundary,
              Math.min(1, 5 * BoardGeometry.HEX_SCALE / waterline.dst(boundary)));
        lip.z = center.z;
        return lip;
    }

    /** Rounded bays and irregular banks, with fixed mouths that meet the next hex exactly. */
    private Vector3[] curvedContour(float[] inset, float z, List<Integer> channelMouths) {
        Vector3[] anchors = contour(inset, z);
        Vector3[] result = new Vector3[6 * SHORE_SEGMENTS];
        for (int edge = 0; edge < 6; edge++) {
            int previous = (edge + 5) % 6, next = (edge + 1) % 6;
            Vector3 a = anchors[edge], b = anchors[next];
            Vector3 c1 = new Vector3(a).mulAdd(new Vector3(b).sub(anchors[previous]), 1f / 6);
            Vector3 c2 = new Vector3(b).mulAdd(new Vector3(anchors[(edge + 2) % 6]).sub(a), -1f / 6);
            if (inset[previous] == 0) {
                Vector3 inward = new Vector3(corners[edge]).sub(corners[previous]).crs(Vector3.Z).nor().scl(-1);
                c1.set(a).mulAdd(inward, a.dst(b) / 3);
            }
            if (inset[next] == 0) {
                Vector3 outward = new Vector3(corners[(edge + 2) % 6]).sub(corners[next]).crs(Vector3.Z).nor();
                c2.set(b).mulAdd(outward, -a.dst(b) / 3);
            }
            float phase = tile.coords().getX() * 1.73f + tile.coords().getY() * 2.41f + edge;
            for (int segment = 0; segment < SHORE_SEGMENTS; segment++) {
                float t = segment / (float) SHORE_SEGMENTS, s = 1 - t;
                Vector3 point;
                if (inset[edge] == 0) {
                    point = new Vector3(a).lerp(b, t);
                } else {
                    point = new Vector3(a).scl(s * s * s).mulAdd(c1, 3 * s * s * t)
                          .mulAdd(c2, 3 * s * t * t).mulAdd(b, t * t * t);
                    Vector3 inward = new Vector3(center.x - point.x, center.y - point.y, 0).nor();
                    float ripple = (float) Math.sin(phase + t * Math.PI * 3) * 16 * t * t * s * s;
                    point.mulAdd(inward, ripple * 1.4f * BoardGeometry.HEX_SCALE);
                }
                point.z = z;
                result[edge * SHORE_SEGMENTS + segment] = point;
            }
        }
        if (!channelMouths.isEmpty()) {
            channelBank(result, anchors, channelMouths.getFirst(), channelMouths.getLast());
            channelBank(result, anchors, channelMouths.getLast(), channelMouths.getFirst());
        }
        return result;
    }

    /** One continuous bank between river mouths, without a separate bay at each hex centre. */
    private void channelBank(Vector3[] contour, Vector3[] anchors, int from, int to) {
        Vector3 a = new Vector3(corners[from]).lerp(corners[(from + 1) % 6], 0.5f);
        Vector3 b = new Vector3(corners[to]).lerp(corners[(to + 1) % 6], 0.5f);
        a.z = b.z = anchors[from].z;
        Vector3 inA = new Vector3(corners[(from + 1) % 6]).sub(corners[from]).crs(Vector3.Z).nor().scl(-1);
        Vector3 inB = new Vector3(corners[(to + 1) % 6]).sub(corners[to]).crs(Vector3.Z).nor().scl(-1);
        // A gentle bend keeps the inner bank from folding back across itself.
        float handle = a.dst(b) * 0.4f;
        Vector3 c1 = new Vector3(a).mulAdd(inA, handle);
        Vector3 c2 = new Vector3(b).mulAdd(inB, handle);
        float widthA = a.dst(anchors[(from + 1) % 6]), widthB = b.dst(anchors[to]);
        int segments = Math.floorMod(to - from - 1, 6) * SHORE_SEGMENTS;
        for (int index = 0; index < segments; index++) {
            float t = index / (float) segments, s = 1 - t;
            Vector3 tangent = new Vector3(c1).sub(a).scl(3 * s * s)
                  .mulAdd(new Vector3(c2).sub(c1), 6 * s * t).mulAdd(new Vector3(b).sub(c2), 3 * t * t);
            Vector3 point = new Vector3(a).scl(s * s * s).mulAdd(c1, 3 * s * s * t)
                  .mulAdd(c2, 3 * s * t * t).mulAdd(b, t * t * t);
            contour[((from + 1) * SHORE_SEGMENTS + index) % contour.length] = point
                  .mulAdd(tangent.crs(Vector3.Z).nor(), widthA + (widthB - widthA) * t);
        }
    }

    /** Curved channels can be concave; a centre fan would fill parts of their banks with water. */
    private static void polygon(Vector3[] contour, Finish finish, List<Face> destination) {
        float[] xy = new float[contour.length * 2];
        for (int i = 0; i < contour.length; i++) {
            xy[i * 2] = contour[i].x;
            xy[i * 2 + 1] = contour[i].y;
        }
        var indices = new EarClippingTriangulator().computeTriangles(xy);
        for (int i = 0; i < indices.size; i += 3) {
            destination.add(new Face(contour[indices.get(i)], contour[indices.get(i + 2)],
                  contour[indices.get(i + 1)], finish));
        }
    }

    /** Intersections of independently offset edge planes preserve matching river mouths between neighbors. */
    private Vector3[] contour(float[] inset, float z) {
        Vector3[] result = new Vector3[6];
        for (int vertex = 0; vertex < 6; vertex++) {
            int before = (vertex + 5) % 6;
            Vector3 a = corners[before];
            Vector3 b = corners[vertex];
            Vector3 c = corners[(vertex + 1) % 6];
            Vector3 n1 = new Vector3(b).sub(a).crs(Vector3.Z).nor();
            Vector3 n2 = new Vector3(c).sub(b).crs(Vector3.Z).nor();
            float d1 = n1.dot(a) - inset[before];
            float d2 = n2.dot(b) - inset[vertex];
            float determinant = n1.x * n2.y - n1.y * n2.x;
            result[vertex] = new Vector3((d1 * n2.y - n1.y * d2) / determinant,
                  (n1.x * d2 - d1 * n2.x) / determinant, z);
        }
        // The 84x72 artwork is not a mathematically regular hex. Use the same
        // distance along both sides of a shared mouth instead of intersecting
        // two slightly different corner angles independently.
        for (int edge = 0; edge < 6; edge++) {
            if (inset[edge] == 0) {
                int next = (edge + 1) % 6;
                float length = corners[edge].dst(corners[next]);
                // Open mouths use four units less land on each side than a rounded basin bank.
                // The wider channel occupies the old water-and-shore width; its sand fade reaches the corners.
                float from = Math.max(0, inset[(edge + 5) % 6] - 4 * BoardGeometry.HEX_SCALE) * 1.1547005f / length;
                float to = Math.max(0, inset[next] - 4 * BoardGeometry.HEX_SCALE) * 1.1547005f / length;
                result[edge].set(corners[edge]).lerp(corners[next], from).z = z;
                result[next].set(corners[next]).lerp(corners[edge], to).z = z;
            }
        }
        return result;
    }

    private void road(BoardScene scene) {
        Vector3[] hub = new Vector3[6];
        for (int i = 0; i < 6; i++) {
            hub[i] = new Vector3(corners[i]).lerp(center, 0.5f);
        }
        fan(hub, center.z, Finish.TOP);
        for (int edge = 0; edge < 6; edge++) {
            int next = (edge + 1) % 6;
            int direction = BoardGeometry.edgeDirection(edge);
            BoardScene.Tile neighbor = scene.tile(tile.coords().translated(direction));
            if ((ramps & (1 << direction)) == 0) {
                quad(hub[edge], corners[edge], corners[next], hub[next], Finish.TOP);
                continue;
            }
            float half = 9 * BoardGeometry.HEX_SCALE / corners[edge].dst(corners[next]);
            Vector3 left = new Vector3(corners[edge]).lerp(corners[next], 0.5f - half);
            Vector3 right = new Vector3(corners[edge]).lerp(corners[next], 0.5f + half);
            Vector3 innerLeft = new Vector3(hub[edge]).lerp(hub[next], 0.5f - half * 2);
            Vector3 innerRight = new Vector3(hub[edge]).lerp(hub[next], 0.5f + half * 2);
            quad(hub[edge], corners[edge], left, innerLeft, Finish.TOP);
            quad(innerRight, right, corners[next], hub[next], Finish.TOP);
            Vector3 roadLeft = new Vector3(left);
            Vector3 roadRight = new Vector3(right);
            roadLeft.z = roadRight.z = roadEdgeElevation(tile, neighbor, direction) * BoardGeometry.LEVEL;
            quad(innerLeft, roadLeft, roadRight, innerRight, Finish.TOP);
            triangle(innerLeft, left, roadLeft, Finish.BANK);
            triangle(innerRight, roadRight, right, Finish.BANK);
        }
    }

    private void fan(Vector3[] polygon, float z, Finish finish) {
        Vector3 middle = new Vector3(center.x, center.y, z);
        for (int i = 0; i < polygon.length; i++) {
            triangle(middle, polygon[i], polygon[(i + 1) % polygon.length], finish);
        }
    }

    private void quad(Vector3 a, Vector3 b, Vector3 c, Vector3 d, Finish finish) {
        quad(a, b, c, d, finish, -1);
    }

    private void quad(Vector3 a, Vector3 b, Vector3 c, Vector3 d, Finish finish, int landEdge) {
        triangle(a, b, c, finish, landEdge);
        triangle(a, c, d, finish, landEdge);
    }

    private void triangle(Vector3 a, Vector3 b, Vector3 c, Finish finish) {
        triangle(a, b, c, finish, -1);
    }

    private void triangle(Vector3 a, Vector3 b, Vector3 c, Finish finish, int landEdge) {
        if (new Vector3(b).sub(a).crs(new Vector3(c).sub(a)).len2() > 0.000001f) {
            faces.add(new Face(new Vector3(a), new Vector3(b), new Vector3(c), finish, landEdge));
        }
    }

    float height(float x, float y) {
        float height = Float.NEGATIVE_INFINITY;
        for (Face face : faces) {
            if (face.finish() == Finish.ICE) {
                continue;
            }
            Vector3 a = face.a(), b = face.b(), c = face.c();
            float denominator = (b.y - c.y) * (a.x - c.x) + (c.x - b.x) * (a.y - c.y);
            if (Math.abs(denominator) < 0.00001f) {
                continue;
            }
            float u = ((b.y - c.y) * (x - c.x) + (c.x - b.x) * (y - c.y)) / denominator;
            float v = ((c.y - a.y) * (x - c.x) + (a.x - c.x) * (y - c.y)) / denominator;
            float w = 1 - u - v;
            // World-space tolerance keeps rounded edge samples on thin bank triangles even far from the origin.
            // A fixed barycentric tolerance can miss the bank and drop the exposed wall to the riverbed.
            float tolerance = 0.001f * BoardGeometry.HEX_SCALE / Math.abs(denominator);
            if (u >= -tolerance * Math.hypot(b.x - c.x, b.y - c.y)
                  && v >= -tolerance * Math.hypot(c.x - a.x, c.y - a.y)
                  && w >= -tolerance * Math.hypot(a.x - b.x, a.y - b.y)) {
                u = Math.max(0, u);
                v = Math.max(0, v);
                w = Math.max(0, w);
                height = Math.max(height, (u * a.z + v * b.z + w * c.z) / (u + v + w));
            }
        }
        return Float.isFinite(height) ? height : BoardGeometry.groundZ(tile);
    }

    private void cuts(Vector3 a, Vector3 b, TreeSet<Float> cuts) {
        float dx = b.x - a.x, dy = b.y - a.y;
        float length2 = dx * dx + dy * dy;
        for (Face face : faces) {
            for (Vector3 point : List.of(face.a(), face.b(), face.c())) {
                if (Math.abs(dx * (point.y - a.y) - dy * (point.x - a.x)) < 0.02f) {
                    float t = ((point.x - a.x) * dx + (point.y - a.y) * dy) / length2;
                    if (t > 0.001f && t < 0.999f) {
                        cuts.add(Math.round(t * 100000) / 100000f);
                    }
                }
            }
        }
    }

    /** Only the higher column contributes a shared wall; road gates meet at the same height. */
    List<Side> sides(BoardScene scene, float floor) {
        List<Side> result = new ArrayList<>();
        for (int edge = 0; edge < 6; edge++) {
            Vector3 a = corners[edge], b = corners[(edge + 1) % 6];
            BoardScene.Tile neighbor = scene.tile(tile.coords().translated(BoardGeometry.edgeDirection(edge)));
            BoardSurface adjacent = neighbor == null ? null : new BoardSurface(scene, neighbor);
            TreeSet<Float> cuts = new TreeSet<>(List.of(0f, 1f));
            cuts(a, b, cuts);
            if (adjacent != null) {
                adjacent.cuts(a, b, cuts);
            }
            List<Float> points = new ArrayList<>(cuts);
            for (int i = 1; i < points.size(); i++) {
                float from = points.get(i - 1), to = points.get(i);
                if (to - from < 0.0001f) {
                    continue;
                }
                Vector3 start = new Vector3(a).lerp(b, from);
                Vector3 end = new Vector3(a).lerp(b, to);
                Vector3 sampleA = new Vector3(start).lerp(end, 0.001f);
                Vector3 sampleB = new Vector3(end).lerp(start, 0.001f);
                start.z = height(sampleA.x, sampleA.y);
                end.z = height(sampleB.x, sampleB.y);
                float lowA = adjacent == null ? floor : adjacent.height(sampleA.x, sampleA.y);
                float lowB = adjacent == null ? floor : adjacent.height(sampleB.x, sampleB.y);
                float dA = start.z - lowA, dB = end.z - lowB;
                if (dA < -0.001f && dB > 0.001f) {
                    float t = -dA / (dB - dA);
                    start.lerp(end, t);
                    lowA = start.z;
                } else if (dB < -0.001f && dA > 0.001f) {
                    float t = dA / (dA - dB);
                    end.set(new Vector3(start).lerp(end, t));
                    lowB = end.z;
                }
                if (Math.max(start.z - lowA, end.z - lowB) > 0.01f) {
                    result.add(new Side(start, end, Math.min(start.z, lowA), Math.min(end.z, lowB), edge));
                }
            }
        }
        return result;
    }
}
