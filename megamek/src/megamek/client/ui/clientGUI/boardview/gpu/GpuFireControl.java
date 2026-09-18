/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.List;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/** Unlit, depth-tested tactical volumes shared by both cameras. Owns its meshes and batch on the GL thread. */
final class GpuFireControl implements Disposable {
    private static final long ATTRIBUTES = VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked;
    /** Beam width as a fraction of its offset from the hex edge. */
    private static final float FRAME_RATIO = 0.6f;
    /** tan(30°): how far two adjoining offset edges reach to meet at the hex tiling's ~120° corners. */
    private static final float MITER = 0.5774f;
    private final ModelBatch batch = new ModelBatch();
    private final Material material = new Material(ColorAttribute.createDiffuse(Color.WHITE),
          new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
          new DepthTestAttribute(GL20.GL_LEQUAL, false), IntAttribute.createCullFace(GL20.GL_NONE));
    private List<BoardScene.FiringLine> firingLines = List.of();
    private List<BoardScene.RangeBorder> rangeBorders = List.of();
    private List<BoardScene.Tile> tiles = List.of();
    private ModelInstance instance;
    private int tuning = -1;

    void update(BoardScene scene) {
        boolean changed = tuning != BoardGeometry.revision() || !firingLines.equals(scene.firingLines())
              || !rangeBorders.equals(scene.rangeBorders()) || !sameTerrain(scene.tiles());
        tiles = scene.tiles();
        if (!changed) {
            return;
        }
        tuning = BoardGeometry.revision();
        firingLines = scene.firingLines();
        rangeBorders = scene.rangeBorders();
        if (instance != null) {
            instance.model.dispose();
            instance = null;
        }
        if (firingLines.isEmpty() && rangeBorders.isEmpty()) {
            return;
        }
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        int part = 0;
        for (BoardScene.RangeBorder border : rangeBorders) {
            if (scene.tile(border.coords()) == null) {
                continue;
            }
            MeshPartBuilder mesh = builder.part("range-" + part++, GL20.GL_TRIANGLES, ATTRIBUTES, material);
            Color color = color(border.rgb(), 0.18f);
            int mask = border.edges();
            Vector3 center = BoardGeometry.center(border.coords(), 0);
            for (int edge = 0; edge < 6; edge++) {
                if ((mask & (1 << BoardGeometry.edgeDirection(edge))) != 0) {
                    range(mesh, BoardFiringGeometry.rangeEdge(scene, border.coords(), edge), center,
                          (mask & (1 << BoardGeometry.edgeDirection(edge + 5))) != 0,
                          (mask & (1 << BoardGeometry.edgeDirection(edge + 1))) != 0, color);
                }
            }
        }
        for (BoardScene.FiringLine line : firingLines) {
            MeshPartBuilder mesh = builder.part("attack-" + part++, GL20.GL_TRIANGLES, ATTRIBUTES, material);
            Color color = color(line.rgb(), 1).lerp(Color.WHITE, 0.22f);
            List<Vector3> path = BoardFiringGeometry.trajectory(scene, line);
            for (int i = 1; i < path.size(); i++) {
                tube(mesh, path.get(i - 1), path.get(i), 1.45f * BoardGeometry.HEX_SCALE, color);
            }
            Vector3 end = path.getLast();
            Vector3 direction = new Vector3(end).sub(path.get(path.size() - 2)).nor();
            float length = Math.min(12 * BoardGeometry.HEX_SCALE, path.getFirst().dst(end) * 0.2f);
            cone(mesh, new Vector3(end).mulAdd(direction, -length), end, length * 0.4f, color);
        }
        Model model = builder.end();
        instance = new ModelInstance(model);
    }

    /** Ground labels and deployment highlights do not invalidate this geometry. */
    private boolean sameTerrain(List<BoardScene.Tile> next) {
        if (tiles == next) {
            return true;
        }
        if (tiles.size() != next.size()) {
            return false;
        }
        for (int i = 0; i < tiles.size(); i++) {
            BoardScene.Tile a = tiles.get(i), b = next.get(i);
            if (!a.coords().equals(b.coords()) || a.elevation() != b.elevation() || !a.features().equals(b.features())) {
                return false;
            }
        }
        return true;
    }

    private static Color color(int rgb, float alpha) {
        return new Color(((rgb >>> 16) & 255) / 255f, ((rgb >>> 8) & 255) / 255f, (rgb & 255) / 255f, alpha);
    }

    /**
     * The beam is offset inward perpendicular to its edge, never sharing the terrain's edge plane, where it is
     * coplanar with the terrain and flickers. Its ends meet the adjoining boundary edges: at a convex corner
     * (the hex's own next or previous edge continues the border) both segments stop at the join, at a reflex
     * corner (the border turns across to another hex) both continue past it. Both cases reach the same tan(30°)
     * mitre, so the outline stays closed across hexes.
     */
    private static void range(MeshPartBuilder mesh, BoardFiringGeometry.RangeEdge edge, Vector3 center,
          boolean convexStart, boolean convexEnd, Color color) {
        Vector3 bottomA = edge.bottomA(), bottomB = edge.bottomB();
        Vector3 along = new Vector3(bottomB.x - bottomA.x, bottomB.y - bottomA.y, 0).nor();
        Vector3 inward = new Vector3(Vector3.Z).crs(along);
        if (inward.x * (center.x - bottomA.x) + inward.y * (center.y - bottomA.y) < 0) {
            inward.scl(-1);
        }
        float offset = BoardGeometry.MARKER_INSET * BoardGeometry.WIDTH / 2;
        float miter = offset * MITER;
        Vector3 start = new Vector3(inward).scl(offset).mulAdd(along, convexStart ? miter : -miter);
        Vector3 end = new Vector3(inward).scl(offset).mulAdd(along, convexEnd ? -miter : miter);
        Vector3 a = bottomA.add(start);
        Vector3 b = bottomB.add(end);
        Vector3 c = edge.topB().add(end);
        Vector3 d = edge.topA().add(start);
        float width = offset * FRAME_RATIO;
        Vector3 innerA = new Vector3(a).mulAdd(inward, width);
        Vector3 innerB = new Vector3(b).mulAdd(inward, width);
        Vector3 innerC = new Vector3(c).mulAdd(inward, width);
        Vector3 innerD = new Vector3(d).mulAdd(inward, width);
        quad(mesh, a, b, c, d, color);
        quad(mesh, innerB, innerA, innerD, innerC, color);
        quad(mesh, a, innerA, innerB, b, color);
        quad(mesh, a, d, innerD, innerA, color);
        quad(mesh, b, innerB, innerC, c, color);
        Color cap = new Color(color).mul(1, 1, 1, 1.5f);
        quad(mesh, d, c, innerC, innerD, cap);
        tube(mesh, d, c, 0.65f * BoardGeometry.HEX_SCALE, new Color(color.r, color.g, color.b, 0.85f));
    }

    private static void quad(MeshPartBuilder mesh, Vector3 a, Vector3 b, Vector3 c, Vector3 d, Color color) {
        mesh.rect(vertex(a, color), vertex(b, color), vertex(c, color), vertex(d, color));
    }

    private static MeshPartBuilder.VertexInfo vertex(Vector3 point, Color color) {
        return new MeshPartBuilder.VertexInfo().setPos(point).setCol(color);
    }

    private static Vector3[] ring(Vector3 start, Vector3 end, float radius) {
        Vector3 direction = new Vector3(end).sub(start).nor();
        Vector3 side = new Vector3(direction).crs(Math.abs(direction.z) > 0.95f ? Vector3.Y : Vector3.Z).nor().scl(radius);
        Vector3 up = new Vector3(direction).crs(side).nor().scl(radius);
        return new Vector3[] { side, up, new Vector3(side).scl(-1), new Vector3(up).scl(-1) };
    }

    private static void tube(MeshPartBuilder mesh, Vector3 start, Vector3 end, float radius, Color color) {
        if (start.dst2(end) < 0.0001f) {
            return;
        }
        Vector3[] ring = ring(start, end, radius);
        for (int i = 0; i < ring.length; i++) {
            Vector3 a = ring[i], b = ring[(i + 1) % ring.length];
            quad(mesh, new Vector3(start).add(a), new Vector3(start).add(b),
                  new Vector3(end).add(b), new Vector3(end).add(a), color);
        }
    }

    private static void cone(MeshPartBuilder mesh, Vector3 base, Vector3 tip, float radius, Color color) {
        Vector3[] ring = ring(base, tip, radius);
        for (int i = 0; i < ring.length; i++) {
            mesh.triangle(vertex(new Vector3(base).add(ring[i]), color),
                  vertex(new Vector3(base).add(ring[(i + 1) % ring.length]), color), vertex(tip, color));
        }
    }

    void render(Camera camera) {
        if (instance != null) {
            batch.begin(camera);
            batch.render(instance);
            batch.end();
        }
    }

    @Override
    public void dispose() {
        if (instance != null) {
            instance.model.dispose();
            instance = null;
        }
        batch.dispose();
    }
}
