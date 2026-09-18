/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import megamek.client.ui.tileset.HexTileset;
import megamek.common.board.Coords;

/** One world coordinate convention for terrain, sprites, movement, and picking in either camera view. */
final class BoardGeometry {
    /**
     * The values the GPU board tunes while it runs; everything else in this class derives from them. A fresh
     * session starts on {@link #DEFAULTS}, and changing one only takes effect through {@link #tune(Tuning)},
     * which recomputes the derived values and bumps {@link #revision()}.
     */
    record Tuning(float padding, float hexScale, float interpolationInset, float cornerBevel,
          float bandNormalFlatness, float unitScale, float unitHeightScale, int baseLevelHeight,
          float hexFrameShade) { }

    static final Tuning DEFAULTS = new Tuning(0f, 1f, 0.0f, 0.1f, 0.5f, 0.6f, 0.87f, 18, 0.8f);

    /** Fraction of the hex width left as padding between two neighboring hexes; must be positive. */
    public static float PADDING = DEFAULTS.padding();
    /** Hex size relative to the captured tile artwork. Unit tokens keep their own captured size. */
    public static float HEX_SCALE = DEFAULTS.hexScale();
    /**
     * Where padding interpolation starts, as a fraction from the hex edge to the hex center: 0 joins
     * neighbors at their edges, 1 has a slope run from center to center. Cliffs ignore this inset.
     */
    public static float INTERPOLATION_INSET = DEFAULTS.interpolationInset();
    /** Level difference at or above which neighbors join with a cliff instead of a slope. */
    public static final int CLIFF_LEVELS = 3;
    /** Fraction of a cell edge where a corner is closed by a slanted cap instead of a vertical face. */
    public static float CORNER_BEVEL = DEFAULTS.cornerBevel();
    /** How far padding normals lean toward flat. */
    public static float BAND_NORMAL_FLATNESS = DEFAULTS.bandNormalFlatness();
    /**
     * How strongly the hex frame darkens the artwork it repeats; 1 draws the frame the surface's own shade,
     * which is invisible, and {@link GpuTerrain} leaves the frame geometry out then, exactly as if the hex
     * frame were off.
     */
    public static float HEX_FRAME_SHADE = DEFAULTS.hexFrameShade();
    /** Captured artwork size in pixels; a hex surface is this multiplied by HEX_SCALE. */
    public static final float TILE_WIDTH = HexTileset.HEX_W;
    public static final float TILE_HEIGHT = HexTileset.HEX_H;
    /** Flat hex surface: the captured artwork scaled so unit tokens keep their own size. */
    public static float WIDTH;
    public static float HEIGHT;
    /** Gap between neighboring hex edges; the padding bands fill it, half from each tile. */
    public static float GAP;
    /**
     * True when the padding exists at all: hexes spaced apart, or interpolation pulled inside their edges.
     * Without it the hex surfaces tile edge to edge and level steps are closed by faces at the shared edge.
     */
    public static boolean HAS_PADDING;
    /** Lattice spacing: the tight tiling of the scaled hexes pushed apart by the gap. */
    private static float SPACING;
    public static float CELL_WIDTH;
    public static float CELL_HEIGHT;
    /** Level height scales with the hexes, so elevation steps keep their proportions. */
    /** How tall is a level (unscaled and scaled) */
    public static int BASE_LEVEL_HEIGHT = DEFAULTS.baseLevelHeight();
    public static float LEVEL;
    /** Unit token footprint scale in the hex plane; 1 keeps the captured artwork size. */
    public static float UNIT_SCALE = DEFAULTS.unitScale();
    /** Unit token height per occupied height level, as a fraction of a terrain level. */
    public static float UNIT_HEIGHT_SCALE = DEFAULTS.unitHeightScale();
    private static int revision;

    static {
        refresh();
    }

    private BoardGeometry() { }

    /** Recomputes every value derived from the tuning; the tuning entry point and the initializer call it. */
    private static void refresh() {
        WIDTH = TILE_WIDTH * HEX_SCALE;
        HEIGHT = TILE_HEIGHT * HEX_SCALE;
        GAP = PADDING * WIDTH;
        HAS_PADDING = GAP > 0 || INTERPOLATION_INSET > 0;
        SPACING = HEX_SCALE + GAP / TILE_HEIGHT;
        CELL_WIDTH = TILE_WIDTH * SPACING;
        CELL_HEIGHT = TILE_HEIGHT * SPACING;
        LEVEL = BASE_LEVEL_HEIGHT * HEX_SCALE;
    }

    /** Applies a tuning and tells the renderers that their meshes are built for the previous one. */
    public static void tune(Tuning tuning) {
        PADDING = tuning.padding();
        HEX_SCALE = tuning.hexScale();
        INTERPOLATION_INSET = tuning.interpolationInset();
        CORNER_BEVEL = tuning.cornerBevel();
        BAND_NORMAL_FLATNESS = tuning.bandNormalFlatness();
        HEX_FRAME_SHADE = tuning.hexFrameShade();
        UNIT_SCALE = tuning.unitScale();
        UNIT_HEIGHT_SCALE = tuning.unitHeightScale();
        BASE_LEVEL_HEIGHT = tuning.baseLevelHeight();
        refresh();
        revision++;
    }

    /** Bumped by every {@link #tune(Tuning)}; renderers rebuild from the scene when it changes. */
    public static int revision() {
        return revision;
    }

    public static float centerX(Coords coords) {
        return coords.getX() * CELL_WIDTH * 0.75f + WIDTH / 2;
    }

    public static float centerY(Coords coords) {
        return -(coords.getY() * CELL_HEIGHT + (coords.getX() & 1) * CELL_HEIGHT / 2 + HEIGHT / 2);
    }

    public static Vector3 center(Coords coords, float elevation) {
        return new Vector3(centerX(coords), centerY(coords), elevation * LEVEL);
    }

    /** Flat hex surface corner: where the padding band starts interpolating toward each neighbor. */
    public static Vector3 corner(Coords coords, float elevation, int corner) {
        return corner(new Vector3(), coords, elevation, corner, WIDTH, HEIGHT);
    }

    /** Writes the hex surface corner into {@code out} without allocating. */
    public static Vector3 corner(Vector3 out, Coords coords, float elevation, int corner) {
        return corner(out, coords, elevation, corner, WIDTH, HEIGHT);
    }

    /** The corner where padding interpolation starts: the hex corner pulled toward the hex center. */
    public static Vector3 insetCorner(Coords coords, float elevation, int corner) {
        return insetCorner(new Vector3(), coords, elevation, corner, INTERPOLATION_INSET);
    }

    /** Writes the interpolation corner into {@code out} without allocating. */
    public static Vector3 insetCorner(Vector3 out, Coords coords, float elevation, int corner) {
        return insetCorner(out, coords, elevation, corner, INTERPOLATION_INSET);
    }

    /** The corner an interpolation starting at {@code inset} uses; the padding tests pin down this math. */
    static Vector3 insetCorner(Vector3 out, Coords coords, float elevation, int corner, float inset) {
        corner(out, coords, elevation, corner);
        if (inset > 0) {
            out.x = centerX(coords) + (out.x - centerX(coords)) * (1 - inset);
            out.y = centerY(coords) + (out.y - centerY(coords)) * (1 - inset);
        }
        return out;
    }

    /** Lattice cell corner: the midline of the padding band, shared with the neighbor's half. */
    public static Vector3 cellCorner(Coords coords, float elevation, int corner) {
        return cellCorner(new Vector3(), coords, elevation, corner);
    }

    /** Writes the lattice cell corner into {@code out} without allocating. */
    public static Vector3 cellCorner(Vector3 out, Coords coords, float elevation, int corner) {
        return corner(out, coords, elevation, corner, CELL_WIDTH, CELL_HEIGHT);
    }

    private static Vector3 corner(Vector3 out, Coords coords, float elevation, int corner, float width, float height) {
        out.set(centerX(coords), centerY(coords), elevation * LEVEL);
        return switch (Math.floorMod(corner, 6)) {
            case 0 -> out.add(width / 2, 0, 0);
            case 1 -> out.add(width / 4, height / 2, 0);
            case 2 -> out.add(-width / 4, height / 2, 0);
            case 3 -> out.add(-width / 2, 0, 0);
            case 4 -> out.add(-width / 4, -height / 2, 0);
            default -> out.add(width / 4, -height / 2, 0);
        };
    }

    /** The neighbor sharing this edge; edges run counter-clockwise from the east corner. */
    public static int edgeDirection(int edge) {
        return Math.floorMod(1 - edge, 6);
    }

    /** The edge of a tile that faces the neighbor in {@code direction}; the reverse of edgeDirection. */
    public static int facingEdge(int direction) {
        return Math.floorMod(1 - Math.floorMod(direction + 3, 6), 6);
    }

    /** Level this tile's band reaches at the midline on one edge; board edges stay at the tile's level. */
    public static float padLevel(BoardScene scene, Coords coords, int elevation, int edge) {
        BoardScene.Tile neighbor = scene.tile(coords.translated(edgeDirection(edge)));
        return neighbor == null ? elevation : padHeight(elevation, neighbor.elevation());
    }

    /**
     * Level the two halves of a band reach at the midline: same-level neighbors keep that level, a slope
     * meets at the average, and a cliff keeps its own level so the raised side stays flat.
     */
    public static float padHeight(int elevation, int neighbor) {
        return isSlope(elevation, neighbor) ? (elevation + neighbor) / 2f : elevation;
    }

    /** True when the three tiles meeting at this corner include a cliff pair rather than only ramps. */
    private static boolean cliffAtCorner(BoardScene scene, Coords coords, int elevation, int corner) {
        BoardScene.Tile first = scene.tile(coords.translated(edgeDirection(corner - 1)));
        BoardScene.Tile second = scene.tile(coords.translated(edgeDirection(corner)));
        return (first != null && isCliffPair(elevation, first.elevation()))
              || (second != null && isCliffPair(elevation, second.elevation()))
              || (first != null && second != null && isCliffPair(first.elevation(), second.elevation()));
    }

    /**
     * Fraction of the cell edge where this corner is closed by a slanted cap rather than a vertical face,
     * so the bands stop short of it and the cap covers the difference. Cliffs keep the full edge because
     * their faces must meet at the corner, and board edges keep it because their wall does.
     */
    public static float cornerBevel(BoardScene scene, Coords coords, int elevation, int corner) {
        if (cliffAtCorner(scene, coords, elevation, corner)
              || (scene.tile(coords.translated(edgeDirection(corner - 1))) == null)
              || (scene.tile(coords.translated(edgeDirection(corner))) == null)) {
            return 0;
        }
        return CORNER_BEVEL;
    }

    /** Level the corner cap meets at; the three tiles sharing the corner average their levels. */
    public static float cornerHeight(BoardScene scene, Coords coords, int elevation, int corner) {
        BoardScene.Tile first = scene.tile(coords.translated(edgeDirection(corner - 1)));
        BoardScene.Tile second = scene.tile(coords.translated(edgeDirection(corner)));
        float sum = elevation;
        int count = 1;
        if (first != null) {
            sum += first.elevation();
            count++;
        }
        if (second != null) {
            sum += second.elevation();
            count++;
        }
        return sum / count;
    }

    /**
     * A point on this tile's half of the padding band: {@code u} runs along the edge, {@code t} from where
     * interpolation starts (0) to the midline (1). Both tiles sharing an edge agree on their {@code t = 1}.
     * The band is one quad per half: it ramps straight from the hex edge to the midline, so its surface has
     * no rows to break the shading into bands.
     */
    public static Vector3 padPoint(Vector3 out, BoardScene scene, Coords coords, int elevation, int edge,
          float u, float t) {
        Vector3 innerA = insetCorner(new Vector3(), coords, elevation, edge);
        Vector3 innerB = insetCorner(new Vector3(), coords, elevation, edge + 1);
        Vector3 outerA = cellCorner(new Vector3(), coords, elevation, edge);
        Vector3 outerB = cellCorner(new Vector3(), coords, elevation, edge + 1);
        float level = elevation * LEVEL;
        float pad = padLevel(scene, coords, elevation, edge) * LEVEL;
        // The outer edge stops short of a capped corner; both tiles sharing the edge shorten it alike.
        float first = cornerBevel(scene, coords, elevation, edge);
        float second = cornerBevel(scene, coords, elevation, edge + 1);
        float outer = first + u * (1 - first - second);
        float innerX = MathUtils.lerp(innerA.x, innerB.x, u);
        float innerY = MathUtils.lerp(innerA.y, innerB.y, u);
        float outerX = MathUtils.lerp(outerA.x, outerB.x, outer);
        float outerY = MathUtils.lerp(outerA.y, outerB.y, outer);
        return out.set(MathUtils.lerp(innerX, outerX, t), MathUtils.lerp(innerY, outerY, t),
              level + (pad - level) * t);
    }

    /**
     * Upward normal of the band surface; both tiles sharing a band derive the same midline normals. The legacy
     * board draws its level transitions with flat artwork, so the normal leans toward flat too: a steep band
     * would otherwise shade its south side almost to black under the directional light.
     */
    public static Vector3 padNormal(Vector3 out, BoardScene scene, Coords coords, int elevation, int edge,
          float u, float t) {
        float step = 0.01f;
        Vector3 along = padPoint(new Vector3(), scene, coords, elevation, edge, Math.min(1, u + step), t);
        along.sub(padPoint(new Vector3(), scene, coords, elevation, edge, Math.max(0, u - step), t));
        Vector3 across = padPoint(new Vector3(), scene, coords, elevation, edge, u, Math.min(1, t + step));
        across.sub(padPoint(new Vector3(), scene, coords, elevation, edge, u, Math.max(0, t - step)));
        out.set(along).crs(across).nor();
        if (out.z < 0) {
            out.scl(-1);
        }
        return out.lerp(Vector3.Z, BAND_NORMAL_FLATNESS).nor();
    }

    /** True when the two elevations are joined by a vertical face instead of a ramp. */
    public static boolean isCliffPair(int elevation, int neighbor) {
        return !isSlope(elevation, neighbor) && elevation != neighbor;
    }

    /** True when this tile drops to the neighbor as a vertical face at the midline. */
    public static boolean hasCliff(int elevation, int neighbor) {
        return elevation > neighbor && isCliffPair(elevation, neighbor);
    }

    /** Ramps need padding geometry; without it every step falls back to a face so nothing is left open. */
    private static boolean isSlope(int elevation, int neighbor) {
        return HAS_PADDING && Math.abs(elevation - neighbor) < CLIFF_LEVELS;
    }

    /** Selects the nearest rendered surface, including padding bands and cliff faces. */
    public static Coords pick(BoardScene scene, Ray ray) {
        Coords result = null;
        float nearest = Float.POSITIVE_INFINITY;
        Vector3 hit = new Vector3();
        Vector3 a = new Vector3();
        Vector3 b = new Vector3();
        Vector3 lowA = new Vector3();
        Vector3 lowB = new Vector3();
        float floor = floor(scene);
        for (BoardScene.Tile tile : scene.tiles()) {
            float z = tile.elevation() * LEVEL;
            if (Math.abs(ray.direction.z) > 0.00001f) {
                float t = (z - ray.origin.z) / ray.direction.z;
                float dx = Math.abs(ray.origin.x + ray.direction.x * t - centerX(tile.coords()));
                float dy = Math.abs(ray.origin.y + ray.direction.y * t - centerY(tile.coords()));
                if (t >= 0 && t * t < nearest && dy <= CELL_HEIGHT / 2
                      && (CELL_HEIGHT / 2) * dx + (CELL_WIDTH / 4) * dy <= CELL_WIDTH * CELL_HEIGHT / 4) {
                    nearest = t * t;
                    result = tile.coords();
                }
            }
            // Avoid triangle tests unless the ray crosses this hex's column.
            a.set(centerX(tile.coords()), centerY(tile.coords()), (z + floor) / 2);
            b.set(CELL_WIDTH, CELL_HEIGHT, z - floor);
            if (!Intersector.intersectRayBoundsFast(ray, a, b)) {
                continue;
            }
            for (int edge = 0; edge < 6; edge++) {
                BoardScene.Tile neighbor = scene.tile(tile.coords().translated(edgeDirection(edge)));
                float distance = Float.NaN;
                if (neighbor == null || hasCliff(tile.elevation(), neighbor.elevation())) {
                    float bottom = neighbor == null ? floor : neighbor.elevation() * LEVEL;
                    if (bottom >= z) {
                        continue;
                    }
                    // Vertical face at the midline: the board wall, or this tile's cliff below the plateau.
                    padPoint(a, scene, tile.coords(), tile.elevation(), edge, 0, 1);
                    padPoint(b, scene, tile.coords(), tile.elevation(), edge, 1, 1);
                    lowA.set(a.x, a.y, bottom);
                    lowB.set(b.x, b.y, bottom);
                    distance = quadDistance(ray, a, lowA, lowB, b, hit);
                } else if (padLevel(scene, tile.coords(), tile.elevation(), edge) != tile.elevation()) {
                    // Sloping half of the band: the same quad the renderer builds, hex edge to midline.
                    padPoint(a, scene, tile.coords(), tile.elevation(), edge, 0, 0);
                    padPoint(b, scene, tile.coords(), tile.elevation(), edge, 1, 0);
                    padPoint(lowB, scene, tile.coords(), tile.elevation(), edge, 1, 1);
                    padPoint(lowA, scene, tile.coords(), tile.elevation(), edge, 0, 1);
                    distance = quadDistance(ray, a, b, lowB, lowA, hit);
                } else {
                    continue;
                }
                if (!Float.isNaN(distance) && distance < nearest) {
                    nearest = distance;
                    result = tile.coords();
                }
            }
            for (int corner = 0; corner < 6; corner++) {
                if (cornerBevel(scene, tile.coords(), tile.elevation(), corner) <= 0) {
                    continue;
                }
                // Slanted corner cap: the quad between the two band halves plus the tip at the corner.
                padPoint(a, scene, tile.coords(), tile.elevation(), corner - 1, 1, 0);
                padPoint(b, scene, tile.coords(), tile.elevation(), corner, 0, 0);
                padPoint(lowA, scene, tile.coords(), tile.elevation(), corner - 1, 1, 1);
                padPoint(lowB, scene, tile.coords(), tile.elevation(), corner, 0, 1);
                float distance = quadDistance(ray, a, b, lowB, lowA, hit);
                padPoint(a, scene, tile.coords(), tile.elevation(), corner - 1, 1, 1);
                padPoint(lowA, scene, tile.coords(), tile.elevation(), corner, 0, 1);
                cellCorner(b, tile.coords(), tile.elevation(), corner);
                b.z = cornerHeight(scene, tile.coords(), tile.elevation(), corner) * LEVEL;
                if (Intersector.intersectRayTriangle(ray, a, b, lowA, hit)) {
                    distance = closer(distance, ray.origin.dst2(hit));
                }
                if (!Float.isNaN(distance) && distance < nearest) {
                    nearest = distance;
                    result = tile.coords();
                }
            }
        }
        return result;
    }

    /** The nearer of two hit distances, keeping whichever one found a hit. */
    private static float closer(float first, float second) {
        if (Float.isNaN(first)) {
            return second;
        }
        return Float.isNaN(second) ? first : Math.min(first, second);
    }

    /** Nearest intersection of the ray with a quad's two triangles, or NaN. */
    private static float quadDistance(Ray ray, Vector3 a, Vector3 b, Vector3 c, Vector3 d, Vector3 hit) {
        float nearest = Float.NaN;
        if (Intersector.intersectRayTriangle(ray, a, b, c, hit)) {
            nearest = ray.origin.dst2(hit);
        }
        if (Intersector.intersectRayTriangle(ray, a, c, d, hit)) {
            float distance = ray.origin.dst2(hit);
            nearest = Float.isNaN(nearest) ? distance : Math.min(nearest, distance);
        }
        return nearest;
    }

    public static float floor(BoardScene scene) {
        return (scene.tiles().stream().mapToInt(BoardScene.Tile::elevation).min().orElse(0) - 1) * LEVEL;
    }
}
