/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */

package megamek.client.ui.swing.minimap;

import megamek.client.IClient;
import megamek.client.ui.swing.overlay.OverlayPainter;
import megamek.client.ui.swing.overlay.OverlayPanel;
import megamek.codeUtilities.MathUtility;
import megamek.common.*;
import megamek.common.actions.AttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.enums.GamePhase;
import megamek.common.event.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class BoardviewlessMinimap extends JPanel implements OverlayPainter {
    private final IClient client;
    private final IGame game;
    private final List<Blip> blips;
    private final List<Blip> removedUnits;
    private final List<Line> lines;
    private final List<Line> attackLines;
    private final List<OverlayPanel> overlays;

    private enum IFF {
        OWN_FORCES,
        ALLIE,
        ENEMY;

        public static IFF getIFFStatus(Entity entity, Player player) {
            if (entity.getOwner().isEnemyOf(player)) {
                return ENEMY;
            } else if (entity.getOwner().equals(player) || entity.getOwner().getName().contains("@AI")) {
                return OWN_FORCES;
            } else {
                return ALLIE;
            }
        }
    }
    private record Blip(int x, int y, String code, IFF iff , Color color, int round) {};
    private record Line(int x1, int y1, int x2, int y2, Color color, int round) {};

    // add listener for mouse click and drag, so it changes the value of the xOffset and yOffset  for painting the map
    private int initialClickX;
    private int initialClickY;
    private int startingOffset = 50;
    private int xOffset = 50;
    private int yOffset = 50;
    private int mSize = -1;

    public BoardviewlessMinimap(IClient client) {
        super(new BorderLayout(), true);
        this.client = client;
        this.game = client.getGame();
        this.overlays = new ArrayList<>();
        this.blips = new ArrayList<>();
        this.removedUnits = new ArrayList<>();
        this.lines = new ArrayList<>();
        this.attackLines = new ArrayList<>();

        this.game.addGameListener(new GameListenerAdapter() {
            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                if (e.getNewPhase() == GamePhase.MOVEMENT_REPORT) {
                    update();
                } else if (e.getNewPhase() == GamePhase.FIRING_REPORT) {
                    update();
                } else if (e.getNewPhase() == GamePhase.END) {
                    update();
                }
            }

            @Override
            public void gameEntityRemove(GameEntityRemoveEvent e) {
                var coords = e.getEntity().getPosition();
                if (coords == null) {
                    return;
                }
                var code = e.getEntity().getBlipID() + ":" + e.getEntity().getId();
                removedUnits.add(
                    new Blip(coords.getX(),
                        coords.getY(),
                        "x" + code + "x",
                        IFF.getIFFStatus(e.getEntity(), client.getLocalPlayer()),
                        e.getEntity().getOwner().getColour().getColour(),
                        game.getCurrentRound()));
            }

            @Override
            public void gameTurnChange(GameTurnChangeEvent e) {
                update();
            }

            @Override
            public void gameEntityChange(GameEntityChangeEvent e) {
                var movePath = e.getMovePath();
                if (movePath != null && !movePath.isEmpty()) {
                    addMovePath(new ArrayList<UnitLocation>(movePath), e.getOldEntity());
                }
            }

            @Override
            public void gameNewAction(GameNewActionEvent e) {
                EntityAction entityAction = e.getAction();
                if (entityAction instanceof AttackAction attackAction) {
                    addAttack(attackAction);
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClickX = e.getX();
                initialClickY = e.getY();
            }
        });
        addMouseWheelListener(e -> {
            if (e.getWheelRotation() > 0) {
                mSize = Math.min(mSize + 150, 50_000);
            } else if (e.getWheelRotation() < 0) {
                mSize = Math.max(5_000, mSize - 150);
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int deltaX = e.getX() - initialClickX;
                int deltaY = e.getY() - initialClickY;
                xOffset += deltaX;
                yOffset += deltaY;
                initialClickX = e.getX();
                initialClickY = e.getY();
                repaint();
            }
        });
    }

    public void update() {
        blips.clear();
        for (var inGameObject : game.getInGameObjects()) {
            if (inGameObject instanceof Entity entity) {
                if (!entity.isActive() || entity.getPosition() == null) {
                    continue;
                }
                var coord = entity.getPosition();
                blips.add(
                    new Blip(
                        coord.getX(),
                        coord.getY(),
                        entity.getDisplayName(),
                        IFF.getIFFStatus(entity, client.getLocalPlayer()),
                        entity.getOwner().getColour().getColour(),
                        game.getCurrentRound()));
            }
        }
        updateUI();
    }

    static public void drawArrowHead(Graphics g, int x0, int y0, int x1,
                                     int y1, int headLength, int headAngle) {
        double offs = headAngle * Math.PI / 180.0;
        double angle = Math.atan2(y0 - y1, x0 - x1);
        int[] xs = {x1 + (int) (headLength * Math.cos(angle + offs)), x1,
            x1 + (int) (headLength * Math.cos(angle - offs))};
        int[] ys = {y1 + (int) (headLength * Math.sin(angle + offs)), y1,
            y1 + (int) (headLength * Math.sin(angle - offs))};
        g.drawPolyline(xs, ys, 3);
    }

    private static final Color BG_COLOR = new Color(0x151a15);
    private static final Color DESTROYED_YELLOW = new Color(0x7f7f00);
    private static final Color DESTROYED_RED = new Color(0x7f0000);
    private static final Color DESTROYED_GREEN = new Color(0x007f00);

    private static final Color[] MapLevelColors = new Color[] {
        new Color(0x235524),
        new Color(0x224e23),
        new Color(0x214722),
        new Color(0x204021),
        new Color(0x1f3920),
        new Color(0x1e341f),
        new Color(0x1d311e),
        new Color(0x1c2f1d),
        new Color(0x1b2d1c),
        new Color(0x1a261b),
        new Color(0x191f1a),
    };

    private static final Color[] MapDepthColors = new Color[] {
        new Color(0x1a6699),
        new Color(0x1a4455),
        new Color(0x1a3344),
        new Color(0x1a1144),
        new Color(0x1a0533)
    };


    private BufferedImage boardImage = null;
    private boolean boardNeedsRedraw = true;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        var height = getHeight();
        var width = getWidth();

        var boardHeight = Math.max(game.getBoard().getHeight() - 1, 1);
        var boardWidth = Math.max(game.getBoard().getWidth() - 1, 1);
        if (mSize == -1) {
            mSize = Math.min(width / boardWidth, height / boardHeight) * 1000 / 2;
        }
        var size = mSize / 1000;

        var board = client.getGame().getBoard();
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, width, height);

        // 1) Create or re-create the cached board image if needed
        if (boardImage == null
            || boardImage.getWidth() != width
            || boardImage.getHeight() != height
            || boardNeedsRedraw) {
            boardNeedsRedraw = false;
            var boardSize = projectToView(boardWidth + 10, boardHeight + 10, size);
            boardImage = new BufferedImage(boardSize[0] + size, boardSize[1] + size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D bg = boardImage.createGraphics();
            // You can turn on anti-aliasing if you like
            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int maxHeight = Integer.MIN_VALUE;
            int minHeight = Integer.MAX_VALUE;
            int maxDepth = Integer.MIN_VALUE;
            int minDepth = Integer.MAX_VALUE;
            for (int hx = 0; hx <= boardWidth; hx++) {
                for (int hy = 0; hy <= boardHeight; hy++) {
                    var hex = board.getHex(hx, hy);
                    if (hex == null) {
                        continue;
                    }
                    maxHeight = Math.max(maxHeight, hex.getLevel());
                    minHeight = Math.min(minHeight, hex.getLevel());
                    if (hex.hasDepth1WaterOrDeeper()) {
                        minDepth = Math.min(minDepth, hex.depth());
                        maxDepth = Math.max(maxDepth, hex.depth());
                    }
                }
            }
            int lowerHeight = - minHeight;

            // Fill the background
            bg.setColor(BG_COLOR);
            bg.fillRect(0, 0, width, height);

            // 2) Draw each hex cell as a filled polygon

            for (int hx = 0; hx <= boardWidth; hx++) {
                for (int hy = 0; hy <= boardHeight; hy++) {
                    var hex = board.getHex(hx, hy);
                    if (hex == null) {
                        continue;
                    }
                    Color fillColor;
                    if (hex.hasDepth1WaterOrDeeper()) {
                        int levelIndex = MathUtility.clamp(hex.depth() - minDepth,
                            0, MapDepthColors.length - 1);
                        fillColor = MapDepthColors[levelIndex];
                    } else {
                        // Determine color from the hex "level"
                        int levelIndex = MathUtility.clamp(hex.getLevel() - lowerHeight,
                            0, MapLevelColors.length - 1);
                        fillColor = MapLevelColors[levelIndex];
                    }

                    // Build a polygon representing this hex in screen coords
                    Polygon poly = createHexPolygon(hx, hy, size, startingOffset, startingOffset);
                    bg.setColor(fillColor);
                    bg.fillPolygon(poly);
                    var pos = projectToView(hx, hy, size);
                    if (hex.hasDepth1WaterOrDeeper()) {
                        bg.setColor(new Color(0x3a86a9));
                        bg.drawString("~", pos[0] + startingOffset - (size / 3), pos[1] + startingOffset + (size / 3));
                        bg.drawString("~", pos[0] + startingOffset, pos[1] + startingOffset - (size / 3));
                    }
                    if (hex.hasPavementOrRoad()) {
                        bg.setColor(Color.BLACK);
                        bg.drawLine(pos[0] + startingOffset - 5, pos[1] + startingOffset, pos[0] + startingOffset + 11, pos[1] + startingOffset);
                    }
                    if (hex.hasVegetation()) {
                        bg.setColor(new Color(0x337755));
                        bg.drawString("',',", pos[0] + startingOffset - 10, pos[1] + startingOffset + 10);
                    }
                    if (hex.containsAnyTerrainOf(Terrains.BLDG_CF, Terrains.BLDG_ELEV, Terrains.BLDG_ARMOR, Terrains.BLDG_BASEMENT_TYPE,
                        Terrains.BLDG_FLUFF, Terrains.BLDG_CLASS)) {
                        bg.setColor(Color.BLACK);
                        bg.drawRect(pos[0] + startingOffset - 5, pos[1] + startingOffset - 5, 11, 11);
                    }
                    if (hex.containsAnyTerrainOf(Terrains.FUEL_TANK, Terrains.FUEL_TANK_CF, Terrains.FUEL_TANK_ELEV, Terrains.FUEL_TANK_MAGN)) {
                        bg.setColor(Color.ORANGE);
                        bg.drawRect(pos[0] + startingOffset - 5, pos[1] + startingOffset - 5, 11, 11);
                        bg.drawLine(pos[0] + startingOffset - 5, pos[1] + startingOffset - 5, pos[0] + startingOffset + 11, pos[1] + startingOffset + 11);
                        bg.drawLine(pos[0] + startingOffset - 5, pos[1] + startingOffset + 5, pos[0] + startingOffset + 11, pos[1] + startingOffset - 11);
                    }
                    if (hex.containsTerrain(Terrains.BLDG_BASE_COLLAPSED)) {
                        bg.setColor(Color.BLACK);
                        bg.drawLine(pos[0] + startingOffset - 5, pos[1] + startingOffset - 5, pos[0] + startingOffset + 11, pos[1] + startingOffset + 11);
                        bg.drawLine(pos[0] + startingOffset - 5, pos[1] + startingOffset + 5, pos[0] + startingOffset + 11, pos[1] + startingOffset - 11);
                    }
                }
            }
            bg.dispose();
        }

        // 3) Draw the pre-rendered board image
        g.drawImage(boardImage, xOffset - startingOffset, yOffset - startingOffset, null);

        // removedUnits
        for (var blip : removedUnits) {
            var color = switch (blip.iff()) {
                case ALLIE -> DESTROYED_YELLOW;
                case ENEMY -> DESTROYED_RED;
                case OWN_FORCES -> DESTROYED_GREEN;
            };

            var delta = Math.max(1, game.getCurrentRound() + 1 - blip.round);
            var newColor = new Color(color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int) (color.getAlpha() / (double) delta));
            var p1 = this.projectToView(blip.x, blip.y, size);
            g.setColor(newColor);
            g.drawRect(p1[0] - 4 + xOffset, p1[1] - 4 + yOffset, 9, 9);
            g.drawString(blip.code, p1[0] - 4 + xOffset, p1[1] - 7 + yOffset);
        }

        // lines
        for (var line : lines) {
            var delta =  Math.max(1, game.getCurrentRound() - line.round + 1);
            var newColor = new Color(line.color.getRed(),
                line.color.getGreen(),
                line.color.getBlue(),
                (int) (line.color.getAlpha() / (double) delta));

            g.setColor(newColor);
            var p1 = this.projectToView(line.x1, line.y1, size);
            var p2 = this.projectToView(line.x2, line.y2, size);
            g.drawLine(p1[0] + xOffset, p1[1] + yOffset, p2[0] + xOffset, p2[1] + yOffset);
        }

        // attackLines with a dashed stroke
        Graphics2D g2d = (Graphics2D) g.create();
        Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            0, new float[]{14}, 0);
        g2d.setStroke(dashed);
        for (var line : attackLines) {
            var delta =  Math.max(1, game.getCurrentRound() - line.round + 1);
            var newColor = new Color(line.color.getRed(),
                line.color.getGreen(),
                line.color.getBlue(),
                (int) (line.color.getAlpha() / (double) delta));
            if (!g2d.getColor().equals(newColor)) {
                g2d.setColor(newColor);
            }
            var p1 = this.projectToView(line.x1, line.y1, size);
            var p2 = this.projectToView(line.x2, line.y2, size);
            g2d.drawLine(p1[0] + xOffset, p1[1] + yOffset, p2[0] + xOffset, p2[1] + yOffset);
            drawArrowHead(g2d, p1[0] + xOffset, p1[1] + yOffset, p2[0] + xOffset, p2[1] + yOffset, 15, 30);
        }
        g2d.dispose();

        // blips
        for (var blip : blips) {
            switch (blip.iff()) {
                case ALLIE -> g.setColor(Color.YELLOW);
                case ENEMY -> g.setColor(Color.RED);
                case OWN_FORCES -> g.setColor(Color.GREEN);
            }
            var p1 = this.projectToView(blip.x, blip.y, size);
            g.fillRect(p1[0] - 6 + xOffset, p1[1] - 6 + yOffset, 13, 13);
            g.setColor(new Color(blip.color.getRed(),
                blip.color.getGreen(),
                blip.color.getBlue(),
                255));
            g.fillRect(p1[0] - 4 + xOffset, p1[1] - 4 + yOffset, 9, 9);
            g.drawString(blip.code, p1[0] - 4 + xOffset, p1[1] - 7 + yOffset);
        }

        paintOverlays(g); // your existing overlays
    }

    /**
     * Create a flat-topped hex polygon for the board cell (hx, hy).
     * Uses the same coordinate transformation as projectToView(...).
     */
    private Polygon createHexPolygon(int hx, int hy, double size, int xOffset, int yOffset) {
        Polygon poly = new Polygon();

        // 1) Find the *center* of the hex in pixel coordinates
        int[] center = projectToView(hx, hy, size);
        int cx = center[0];
        int cy = center[1];

        // 2) Construct the 6 corners around that center.
        //    For a flat-topped hex, angles are 0°, 60°, 120°, 180°, 240°, 300°
        double[] angles = {0, Math.PI/3, 2*Math.PI/3, Math.PI, 4*Math.PI/3, 5*Math.PI/3};

        // Typically, the "radius" or "distance from center to any corner"
        // in X-direction is sizeX.  If sizeX == sizeY, it will be a regular hex.
        // If sizeX != sizeY, the hex will be “stretched.”

        for (double angle : angles) {
            int cornerX = (int) Math.round(cx + size * 0.9 * Math.cos(angle));
            int cornerY = (int) Math.round(cy + size * 0.9 * Math.sin(angle));
            poly.addPoint(cornerX + xOffset, cornerY + yOffset);
        }

        return poly;
    }

    public void forceBoardRedraw() {
        this.boardNeedsRedraw = true;
        repaint();
    }

    private int[] hexRound(int x, int y) {
        return cubeToAxial(cubeRound(axialToCube(x, y)));
    }

    private int[] cubeRound(int[] args) {
        return cubeRound(args[0], args[1], args[2]);
    }

    private int[] cubeRound(double x, double y, double z) {
        var rx = (int) Math.round(x);
        var ry = (int) Math.round(y);
        var rz = (int) Math.round(z);

        var x_diff = Math.abs(rx - x);
        var y_diff = Math.abs(ry - y);
        var z_diff = Math.abs(rz - z);

        if (x_diff > y_diff && x_diff > z_diff) {
            rx = -ry - rz;
        } else if (y_diff > z_diff) {
            ry = -rx - rz;
        } else {
            rz = -rx - ry;
        }

        return new int[]{rx, ry, rz};
    }
    private int[] cubeToAxial(int[] args) {
        return cubeToAxial(args[0], args[1], args[2]);
    }

    private int[] cubeToAxial(int x, int y, int z) {
        return new int[] {x, z};
    }

    private int[] axialToCube(int[] args) {
        return axialToCube(args[0], args[1]);
    }

    private int[] axialToCube(int x, int y) {
        var cy = -x- y;
        return new int[] {x, cy, y};
    }

    private int[] pixelToHex(int x, int y, double size) {
        var q = (int) (x * 2.0 / 3.0 / size);
        var r = (int) ((-x / 3.0 + Math.sqrt(3.0) / 3.0 * y) / size);
        return hexRound(q, r);
    }

    private int[] projectToView(int x, int y, double size) {
        var nx = (int) size * 3 / 2 * x;
        var ny = (int) (size * Math.sqrt(3) * (y + 0.5 * (x & 1)));
        return new int[]{nx, ny};
    }

    public void addAttack(AttackAction ea) {
        var attacker = ea.getEntityId();
        var target = ea.getTargetId();
        if (game.getInGameObject(attacker).isPresent() && game.getInGameObject(target).isPresent()) {
            var attackerEntity = (Entity) game.getInGameObject(attacker).get();
            var targetEntity = (Entity) game.getInGameObject(target).get();
            var attackerPos = attackerEntity.getPosition();
            var targetPos = targetEntity.getPosition();
            if (attackerPos != null && targetPos != null) {
                attackLines.add(
                    new Line(attackerPos.getX(), attackerPos.getY(),
                        targetPos.getX(), targetPos.getY(),
                        attackerEntity.getOwner().getColour().getColour(),
                        game.getCurrentRound()));
            }
        }
    }

    public void addMovePath(List<UnitLocation> unitLocations, Entity entity) {
        Coords previousCoords = entity.getPosition();
        for (var unitLocation : unitLocations) {
            var coords = unitLocation.getCoords();
            lines.add(new Line(previousCoords.getX(), previousCoords.getY(),
                coords.getX(), coords.getY(),
                Color.GREEN,
                game.getCurrentRound()));
            previousCoords = coords;
        }
    }

    @Override
    public void paintOverlays(Graphics g) {
        for (var overlay : overlays) {
            overlay.paint(g);
        }
    }

    @Override
    public void addOverlay(OverlayPanel overlayPanel) {
        overlays.add(overlayPanel);
    }

    @Override
    public void addOverlay(OverlayPanel overlayPanel, int index) {
        overlays.add(index, overlayPanel);
    }

    @Override
    public void removeOverlay(OverlayPanel overlayPanel) {
        overlays.remove(overlayPanel);
    }

    @Override
    public void removeOverlay(int index) {
        overlays.remove(index);
    }

    @Override
    public void clearAllOverlays() {
        overlays.clear();
    }
}
