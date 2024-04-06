/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.boardview;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.ImageCache;
import megamek.common.Board;
import megamek.common.Coords;
import megamek.common.Hex;
import megamek.common.Terrains;
import megamek.common.annotations.Nullable;
import megamek.common.planetaryconditions.Light;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.util.ImageUtil;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.util.List;
import java.util.*;

/**
 * This class calculates the shadow map image used to display terrain shadows in the BoardView.
 */
class TerrainShadowHelper {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final BufferedImageOp BLUR_OP = new ConvolveOp(ImageUtil.getGaussKernel(5, 2),
            ConvolveOp.EDGE_NO_OP, null);

    private final BoardView boardView;
    private final ImageCache<Integer, BufferedImage> shadowImageCache = new ImageCache<>();

    /**
     * Creates a shadow map helper for the given BoardView.
     *
     * @param boardView The BoardView for which the shadow map is generated
     */
    TerrainShadowHelper(BoardView boardView) {
        this.boardView = boardView;
    }

    /**
     * Draws and returns the shadow map image for the board, containing shadows for hills/trees/buildings.
     * The shadow map is an overlay image that is mostly transparent but darker where shadows lie. It's
     * size is equal to the size of the entire board image at zoom 1 (i.e., it may be big).
     *
     * @return The shadow map image for the board
     */
    @Nullable
    BufferedImage updateShadowMap() {
        // Issues:
        // Bridge shadows show a gap towards connected hexes. I don't know why.
        // More than one super image on a hex (building + road) doesn't work. how do I get
        //   the super for a hex for a specific terrain? This would also help
        //   with building shadowing other buildings.
        // AO shadows might be handled by this too. But:
        // this seems to need a lot of additional copying (paint shadow on a clean map for this level alone; soften up;
        // copy to real shadow
        // map with clipping area active; get new clean shadow map for next shadowed level;
        // too much hassle currently; it works so beautifully
        if (!GUIP.getShadowMap()) {
            return null;
        }

        Board board = boardView.game.getBoard();
        if ((board == null) || board.inSpace()) {
            return null;
        }

        if (boardView.getBoardSize() == null) {
            boardView.updateBoardSize();
        }

        if (!boardView.isTileImagesLoaded()) {
            return null;
        }

        // Map editor? No shadows
        if (boardView.game.getPhase().isUnknown()) {
            return null;
        }

        PlanetaryConditions conditions = boardView.game.getPlanetaryConditions();
        long stT = System.nanoTime();

        // 1) create or get the hex shadow
        Image hexShadow = createBlurredShadow(boardView.getTilesetManager().getHexMask());
        if (hexShadow == null) {
            boardView.repaint(1000);
            return null;
        }

        // the shadowmap needs to be painted as if scale == 1
        // therefore some of the methods of boardview1 cannot be used
        int width = board.getWidth() * BoardView.HEX_WC + BoardView.HEX_W / 4;
        int height = board.getHeight() * BoardView.HEX_H + BoardView.HEX_H / 2;

        GraphicsConfiguration config = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration();

        BufferedImage shadowMap = config.createCompatibleImage(width, height,
                Transparency.TRANSLUCENT);

        Graphics2D g = shadowMap.createGraphics();

        // Compute shadow angle based on planentary conditions.
        double[] lightDirection;
        if (conditions.getLight().isDarkerThan(Light.FULL_MOON)) {
            lightDirection = new double[]{0, 0};
        } else if (conditions.getLight().isDusk()) {
            // TODO: replace when made user controlled
            lightDirection = new double[]{-38, 14};
        } else {
            lightDirection = new double[]{-19, 7};
        }

        // Shadows for elevation
        // 1a) Sort the board hexes by elevation
        // 1b) Create a reduced list of shadowcasting hexes
        double angle = Math.atan2(-lightDirection[1], lightDirection[0]);
        int mDir = (int) (0.5 + 1.5 - angle / Math.PI * 3); // +0.5 to counter the (int)
        int[] sDirs = {mDir % 6, (mDir + 1) % 6, (mDir + 5) % 6};
        HashMap<Integer, Set<Coords>> sortedHexes = new HashMap<>();
        HashMap<Integer, Set<Coords>> shadowCastingHexes = new HashMap<>();
        for (Coords c : allBoardHexes(board)) {
            Hex hex = board.getHex(c);
            int level = hex.getLevel();
            if (!sortedHexes.containsKey(level)) { // no hexes yet for this height
                sortedHexes.put(level, new HashSet<>());
            }
            if (!shadowCastingHexes.containsKey(level)) { // no hexes yet for this height
                shadowCastingHexes.put(level, new HashSet<>());
            }
            sortedHexes.get(level).add(c);
            // add a hex to the shadowcasting hexes only
            // if it is nor surrounded by same height hexes
            boolean surrounded = true;
            for (int dir : sDirs) {
                if (!board.contains(c.translated(dir))) {
                    surrounded = false;
                } else {
                    Hex nhex = board.getHex(c.translated(dir));
                    int lv = nhex.getLevel();
                    if (lv < level) {
                        surrounded = false;
                    }
                }
            }

            if (!surrounded) {
                shadowCastingHexes.get(level).add(c);
            }
        }

        // 2) Create clipping areas
        HashMap<Integer, Shape> levelClips = new HashMap<>();
        for (Integer h : sortedHexes.keySet()) {
            Path2D path = new Path2D.Float();
            for (Coords c : sortedHexes.get(h)) {
                Point p = BoardView.getHexLocationLargeTile(c.getX(), c.getY(), 1);
                AffineTransform t = AffineTransform.getTranslateInstance(p.x + BoardView.HEX_W / 2.0, p.y + BoardView.HEX_H / 2.0);
                t.scale(1.02, 1.02);
                t.translate(-BoardView.HEX_W / 2.0, -BoardView.HEX_H / 2.0);
                path.append(t.createTransformedShape(BoardView.hexPoly), false);
            }
            levelClips.put(h, path);
        }


        // 3) Find all level differences
        final int maxDiff = 35; // limit all diffs to this value
        Set<Integer> lDiffs = new TreeSet<>();
        for (int shadowed = board.getMinElevation(); shadowed < board.getMaxElevation(); shadowed++) {
            if (levelClips.get(shadowed) == null) {
                continue;
            }

            for (int shadowcaster = shadowed + 1; shadowcaster <= board.getMaxElevation(); shadowcaster++) {
                if (levelClips.get(shadowcaster) == null) {
                    continue;
                }

                lDiffs.add(Math.min(shadowcaster - shadowed, maxDiff));
            }
        }

        // 4) Elevation Shadow images for all level differences present
        int n = 10;
        double deltaX = lightDirection[0] / n;
        double deltaY = lightDirection[1] / n;
        Map<Integer, BufferedImage> hS = new HashMap<>();
        for (int lDiff : lDiffs) {
            Dimension eSize = new Dimension(
                    (int) (Math.abs(lightDirection[0]) * lDiff + BoardView.HEX_W) * 2,
                    (int) (Math.abs(lightDirection[1]) * lDiff + BoardView.HEX_H) * 2);

            BufferedImage elevShadow = config.createCompatibleImage(eSize.width, eSize.height,
                    Transparency.TRANSLUCENT);
            Graphics gS = elevShadow.getGraphics();
            Point2D p1 = new Point2D.Double(eSize.width / 2.0, eSize.height / 2.0);
            if (GUIP.getHexInclines()) {
                // With inclines, the level 1 shadows are only very slight
                int beg = 4;
                p1.setLocation(p1.getX() + deltaX * beg, p1.getY() + deltaY * beg);
                for (int i = beg; i < n * (lDiff - 0.4); i++) {
                    gS.drawImage(hexShadow, (int) p1.getX(), (int) p1.getY(), null);
                    p1.setLocation(p1.getX() + deltaX, p1.getY() + deltaY);
                }
            } else {
                for (int i = 0; i < n * lDiff; i++) {
                    gS.drawImage(hexShadow, (int) p1.getX(), (int) p1.getY(), null);
                    p1.setLocation(p1.getX() + deltaX, p1.getY() + deltaY);
                }
            }
            gS.dispose();
            hS.put(lDiff, elevShadow);
        }

        // 5) Actually draw the elevation shadows
        for (int shadowed = board.getMinElevation(); shadowed < board.getMaxElevation(); shadowed++) {
            if (levelClips.get(shadowed) == null) {
                continue;
            }

            Shape saveClip = g.getClip();
            g.setClip(levelClips.get(shadowed));

            for (int shadowcaster = shadowed + 1; shadowcaster <= board.getMaxElevation(); shadowcaster++) {
                if (levelClips.get(shadowcaster) == null) {
                    continue;
                }
                int lDiff = shadowcaster - shadowed;

                for (Coords c : shadowCastingHexes.get(shadowcaster)) {
                    Point2D p0 = BoardView.getHexLocationLargeTile(c.getX(), c.getY(), 1);
                    g.drawImage(hS.get(Math.min(lDiff, maxDiff)),
                            (int) p0.getX() - (int) (Math.abs(lightDirection[0]) * Math.min(lDiff, maxDiff) + BoardView.HEX_W),
                            (int) p0.getY() - (int) (Math.abs(lightDirection[1]) * Math.min(lDiff, maxDiff) + BoardView.HEX_H), null);
                }
            }
            g.setClip(saveClip);
        }

        n = 5;
        deltaX = lightDirection[0] / n;
        deltaY = lightDirection[1] / n;
        // 4) woods and building shadows
        for (int shadowed = board.getMinElevation(); shadowed <= board.getMaxElevation(); shadowed++) {
            if (levelClips.get(shadowed) == null) {
                continue;
            }

            Shape saveClip = g.getClip();
            g.setClip(levelClips.get(shadowed));

            for (int shadowcaster = board.getMinElevation(); shadowcaster <= board.getMaxElevation(); shadowcaster++) {
                if (levelClips.get(shadowcaster) == null) {
                    continue;
                }

                for (Coords c : sortedHexes.get(shadowcaster)) {
                    Point2D p0 = BoardView.getHexLocationLargeTile(c.getX(), c.getY(), 1);
                    Point2D p1 = new Point2D.Double();

                    // Woods Shadow
                    Hex hex = board.getHex(c);
                    List<Image> supers = boardView.getTilesetManager().supersFor(hex);

                    if (!supers.isEmpty()) {
                        Image lastSuper = createBlurredShadow(supers.get(supers.size() - 1));
                        if (lastSuper == null) {
                            return null;
                        }
                        if (hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.JUNGLE)) {
                            // Woods are 2 levels high, but then shadows
                            // appear very extreme, therefore only
                            // 1.5 levels: (shadowcaster + 1.5 - shadowed)
                            double shadowHeight = 0.75 * hex.terrainLevel(Terrains.FOLIAGE_ELEV);
                            p1.setLocation(p0);
                            if ((shadowcaster + shadowHeight - shadowed) > 0) {
                                for (int i = 0; i < n * (shadowcaster + shadowHeight - shadowed); i++) {
                                    g.drawImage(lastSuper, (int) p1.getX(), (int) p1.getY(), null);
                                    p1.setLocation(p1.getX() + deltaX, p1.getY() + deltaY);
                                }
                            }
                        }

                        // Buildings Shadow
                        if (hex.containsTerrain(Terrains.BUILDING)) {
                            int h = hex.terrainLevel(Terrains.BLDG_ELEV);
                            if ((shadowcaster + h - shadowed) > 0) {
                                p1.setLocation(p0);
                                for (int i = 0; i < (n * (shadowcaster + h - shadowed)); i++) {
                                    g.drawImage(lastSuper, (int) p1.getX(), (int) p1.getY(), null);
                                    p1.setLocation(p1.getX() + deltaX, p1.getY() + deltaY);
                                }
                            }
                        }
                    }
                    // Bridge Shadow
                    if (hex.containsTerrain(Terrains.BRIDGE)) {
                        supers = boardView.getTilesetManager().orthoFor(hex);
                        if (supers.isEmpty()) {
                            break;
                        }
                        Image maskB = createBlurredShadow(supers.get(supers.size() - 1));
                        if (maskB == null) {
                            return null;
                        }
                        int h = hex.terrainLevel(Terrains.BRIDGE_ELEV);
                        p1.setLocation(p0.getX() + deltaX * n * (shadowcaster + h - shadowed),
                                p0.getY() + deltaY * n * (shadowcaster + h - shadowed));
                        // the shadowmask is translucent, therefore draw n times
                        // stupid hack
                        for (int i = 0; i < n; i++) {
                            g.drawImage(maskB, (int) p1.getX(), (int) p1.getY(), null);
                        }
                    }
                }
            }
            g.setClip(saveClip);
        }

        long tT5 = System.nanoTime() - stT;
        LogManager.getLogger().info("Time to prepare the shadow map: " + tT5 / 1e6 + " ms");
        return shadowMap;
    }

    /**
     *  @return a list of Coords of all hexes on the board. Returns ONLY hexes where board.getHex() != null.
     */
    private List<Coords> allBoardHexes(Board board) {
        if (board == null) {
            return Collections.emptyList();
        }

        List<Coords> coordList = new ArrayList<>();
        for (int i = 0; i < board.getWidth(); i++) {
            for (int j = 0; j < board.getHeight(); j++) {
                if (board.getHex(i, j) != null) {
                    coordList.add(new Coords(i, j));
                }
            }
        }
        return coordList;
    }

    private Image createBlurredShadow(Image orig) {
        if ((orig == null) || (orig.getWidth(null) < 0) || (orig.getHeight(null) < 0)) {
            return null;
        }
        BufferedImage mask = shadowImageCache.get(orig.hashCode());
        if (mask == null) {
            GraphicsConfiguration config = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();

            // a slightly bigger image to give room for blurring
            mask = config.createCompatibleImage(orig.getWidth(null) + 4,
                    orig.getHeight(null) + 4, Transparency.TRANSLUCENT);
            Graphics g = mask.getGraphics();
            g.drawImage(orig, 2, 2, null);
            g.dispose();
            mask = boardView.createShadowMask(mask);
            mask = BLUR_OP.filter(mask, null);
            PlanetaryConditions conditions = boardView.game.getPlanetaryConditions();
            if (!conditions.getLight().isDay()) {
                mask = BLUR_OP.filter(mask, null);
            }
            shadowImageCache.put(orig.hashCode(), mask);
        }
        return mask;
    }
}
