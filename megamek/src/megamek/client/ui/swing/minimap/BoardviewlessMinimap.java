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

import megamek.MMConstants;
import megamek.client.Client;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.overlay.IFF;
import megamek.client.ui.swing.overlay.OverlayPainter;
import megamek.client.ui.swing.overlay.OverlayPanel;
import megamek.common.*;
import megamek.common.actions.AttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.event.*;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import static megamek.client.ui.swing.minimap.MinimapUnitSymbols.*;
import static megamek.common.Terrains.BUILDING;
import static megamek.common.Terrains.FUEL_TANK;

/**
 * This class is WIP, commiting just to have an artifact.
 */
public class BoardviewlessMinimap extends JPanel implements OverlayPainter {
    private final Client client;
    private final IGame game;
    private final List<Blip> removedUnits;
    private final List<Line> lines;
    private final List<Line> attackActions;
    private final List<OverlayPanel> overlays;
    private final static int unitSize = 10;
    private record Blip(int x, int y, String code, IFF iff , Color color, int round) {};
    private record Line(int x1, int y1, int x2, int y2, Color color, int round) {};

    // add listener for mouse click and drag, so it changes the value of the xOffset and yOffset  for painting the map
    private int initialClickX;
    private int initialClickY;
    private int xOffset = 50;
    private int yOffset = 50;
    private int mSize = -1;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final ClientPreferences CLIENT_PREFERENCES = PreferenceManager.getClientPreferences();
    private static final int[] UNIT_SCALE = { 7, 8, 9, 11, 12, 14, 16 };
    private final int heightDisplayMode = GUIP.getMinimapHeightDisplayMode();

    private static final int SHOW_NO_HEIGHT = 0;
    private static final int SHOW_GROUND_HEIGHT = 1;
    private static final int SHOW_BUILDING_HEIGHT = 2;
    private static final int SHOW_TOTAL_HEIGHT = 3;

    private int currentRound = -1;

    private final Map<Coords, Integer> multiUnits = new HashMap<>();
    private static final String[] STRAT_WEIGHTS = { "L", "L", "M", "H", "A", "A" };
    private static final int[] HEX_SIDE = { 2, 3, 5, 6, 8, 10, 12 };
    private static final int[] HEX_SIDE_BY_COS30 = { 2, 3, 4, 5, 7, 9, 10 };
    private static final int[] HEX_SIDE_BY_SIN30 = { 1, 2, 2, 3, 4, 5, 6 };
    private static final int[] FONT_SIZE = { 6, 6, 8, 10, 12, 14, 16 };

    private final int zoom = 6;
    private final int topMargin = 3;
    private final int leftMargin = 3;


    public BoardviewlessMinimap(Client client) {
        super(new BorderLayout(), true);
        this.client = client;
        this.game = client.getGame();
        this.overlays = new ArrayList<>();
        this.removedUnits = new ArrayList<>();
        this.lines = new ArrayList<>();
        this.attackActions = new ArrayList<>();

        this.game.addGameListener(new GameListenerAdapter() {
            @Override
            public void gameBoardChanged(GameBoardChangeEvent e) {
                forceBoardRedraw();
            }

            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                update();
            }

            @Override
            public void gameEntityRemove(GameEntityRemoveEvent e) {
                var coords = e.getEntity().getPosition();
                if (coords == null) {
                    return;
                }
                removedUnits.add(
                    new Blip(coords.getX(),
                            coords.getY(),
                            e.getEntity().getDisplayName() + " ID:" + e.getEntity().getId(),
                            IFF.getIFFStatus(e.getEntity(), client.getLocalPlayer()),
                            e.getEntity().getOwner().getColour().getColour(),
                            game.getCurrentRound()));
                update();
            }

            @Override
            public void gameTurnChange(GameTurnChangeEvent e) {
                currentRound = game.getCurrentRound();
                update();
            }

            @Override
            public void gameEntityChange(GameEntityChangeEvent e) {
                var movePath = e.getMovePath();
                if (movePath != null && !movePath.isEmpty()) {
                    addMovePath(new ArrayList<>(movePath), e.getOldEntity());
                }
            }

            @Override
            public void gameNewAction(GameNewActionEvent e) {
                EntityAction entityAction = e.getAction();
                if (entityAction instanceof AttackAction attackAction) {
                    Entity source = ((Game)game).getEntity(attackAction.getEntityId());
                    Targetable target = ((Game)game).getTarget(attackAction.getTargetType(), attackAction.getTargetId());
                    // sanity check...
                    if ((null == source) || (null == target)) {
                        return;
                    }

                    if (attackAction.getTargetType() == Targetable.TYPE_INARC_POD) {
                        // iNarc pods don't have a position
                        return;
                    }
                    attackActions.add(new Line(source.getPosition().getX(), source.getPosition().getY(),
                        target.getPosition().getX(), target.getPosition().getY(),
                        source.getOwner().getColour().getColour(),
                        game.getCurrentRound()));
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

    private BufferedImage boardImage = null;
    private boolean boardNeedsRedraw = true;
    int size = -1;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        var height = getHeight();
        var width = getWidth();
        var board = client.getGame().getBoard();
        g.setColor(UIManager.getColor("Table.background"));
        g.fillRect(0, 0, width, height);

        // 1) Create or re-create the cached board image if needed
        if (boardImage == null
            || boardNeedsRedraw
            || size == -1) {
            if (game.getBoard().getHeight() == 0 || game.getBoard().getWidth() == 0) {
                return;
            }
            boardNeedsRedraw = false;

            boardImage = Minimap.getMinimapImageMaxZoom(board, CLIENT_PREFERENCES.getStrategicViewTheme());
            size = Math.min(
                boardImage.getHeight() / game.getBoard().getHeight(),
                boardImage.getWidth() / game.getBoard().getWidth());
        }

        // 2) Draw the pre-rendered board image
        g.drawImage(boardImage, xOffset, yOffset, null);

        for (int j = 0; j < board.getWidth(); j++) {
            for (int k = 0; k < board.getHeight(); k++) {
                Hex h = board.getHex(j, k);
                paintHeight(g, h, j, k);
            }
        }

        // 3) Movement lines
        Graphics2D g2d = (Graphics2D) g.create();
        Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            0, new float[]{6}, 3);
        g2d.setStroke(dashed);
        for (var line : lines) {
            var delta = Math.max(1, (game.getCurrentRound() + 1 - line.round) * 2);

            g.setColor(fadeColor(line.color.darker(), delta));
            g2d.setColor(fadeColor(line.color.darker(), delta));
            var p1 = this.projectToView(line.x1, line.y1);
            var p2 = this.projectToView(line.x2, line.y2);
            g2d.drawLine(p1[0] + xOffset, p1[1] + yOffset, p2[0] + xOffset, p2[1] + yOffset);
        }
        lines.removeIf(line -> line.round < currentRound - 4);


        g2d.dispose();



        for (var attackAction : attackActions) {
            paintAttack(g, attackAction);
        }

        attackActions.removeIf(timedAction -> timedAction.round < currentRound - 2);

        // 5) Draw destroyed units
        for (var blip : removedUnits) {
            var delta = Math.max(1, (game.getCurrentRound() + 1 - blip.round) * 2);
            var p1 = this.projectToView(blip.x, blip.y);
            g.setColor(fadeColor(blip.iff().getDarkColor(), delta));
            g.drawRect(p1[0] - 4 + xOffset, p1[1] - 4 + yOffset, 9, 9);
            g.drawString(blip.code, p1[0] - 10 + xOffset, p1[1] - 15 + yOffset);
        }

        // 7) Draw unit symbols
        multiUnits.clear();
        for (var inGameObject : game.getInGameObjects()) {
            if (inGameObject instanceof Entity entity) {
                if (entity.getPosition() != null) {
                    paintUnit(g, entity);
                }
                if (entity.getPosition() != null) {
                    paintSensor(g, entity);
                }
            }
        }

        // 8) Draw artillery autohit hexes
        if (client.getArtilleryAutoHit() != null) {
            for (int i = 0; i < client.getArtilleryAutoHit().size(); i++) {
                drawAutoHit(g, client.getArtilleryAutoHit().get(i));
            }
        }

        paintOverlays(g);
    }

    private void paintHeight(Graphics g, Hex h, int x, int y) {
        if (heightDisplayMode == SHOW_NO_HEIGHT) {
            return;
        }

        int height = 0;
        if ((heightDisplayMode == SHOW_BUILDING_HEIGHT) && h.containsTerrain(BUILDING)) {
            height = h.ceiling();
        } else if (heightDisplayMode == SHOW_GROUND_HEIGHT) {
            height = h.floor();
        } else if (heightDisplayMode == SHOW_TOTAL_HEIGHT) {
            height = (h.containsAnyTerrainOf(BUILDING, FUEL_TANK)) ? h.ceiling() : h.floor();
        }
        if (height != 0) {
            String sHeight = ((height > -1) && (height < 10)) ? " " + height : height + "";
            int baseX = (x * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom])) + leftMargin;
            int baseY = (((2 * y) + 1 + (x % 2)) * HEX_SIDE_BY_COS30[zoom]) + topMargin;
            g.setColor(Color.white);
            g.drawString(sHeight, baseX + 5 + xOffset, baseY + 5 + yOffset);
        }
    }
    private void paintAttack(Graphics g, Line attackAction) {
        int[] xPoints = new int[4];
        int[] yPoints = new int[4];

        xPoints[0] = ((attackAction.x1 * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]))
            + leftMargin + ((int) (1.5 * HEX_SIDE[zoom]))) - 2 +xOffset;
        yPoints[0] = (((2 * attackAction.y1) + 1 + (attackAction.x1 % 2))
            * HEX_SIDE_BY_COS30[zoom]) + topMargin + yOffset;
        xPoints[1] = ((attackAction.x2 * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]))
            + leftMargin + ((int) (1.5 * HEX_SIDE[zoom]))) - 2 + xOffset;
        yPoints[1] = (((2 * attackAction.y2) + 1 + (attackAction.x2 % 2))
            * HEX_SIDE_BY_COS30[zoom]) + topMargin + yOffset;
        xPoints[2] = xPoints[1] + 2;
        xPoints[3] = xPoints[0] + 2;
        if (((attackAction.x1 > attackAction.x2) && (attackAction.y1 < attackAction.y2))
            || ((attackAction.x1 < attackAction.x2) && (attackAction.y1 > attackAction.y2))) {
            yPoints[3] = yPoints[0] + 2;
            yPoints[2] = yPoints[1] + 2;
        } else {
            yPoints[3] = yPoints[0] - 2;
            yPoints[2] = yPoints[1] - 2;
        }
        var delta = Math.max(1, (game.getCurrentRound() + 1 - attackAction.round) * 2);
        g.setColor(fadeColor(attackAction.color, delta));
        g.fillPolygon(xPoints, yPoints, 4);
        g.setColor(fadeColor(Color.black, delta));
        g.drawPolygon(xPoints, yPoints, 4);
    }

    private static Color fadeColor(Color color, double alphaDivisor) {
        return new Color(color.getRed(),
            color.getGreen(),
            color.getBlue(),
            (int) (color.getAlpha() / alphaDivisor));
    }

    /** Draws a red crosshair for artillery autohit hexes (predesignated only). */
    private void drawAutoHit(Graphics g, Coords hex) {
        int baseX = (hex.getX() * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom])) + leftMargin + HEX_SIDE[zoom] + xOffset;
        int baseY = (((2 * hex.getY()) + 1 + (hex.getX() % 2)) * HEX_SIDE_BY_COS30[zoom]) + topMargin + yOffset;

        g.setColor(Color.RED);
        g.drawOval(baseX - (unitSize - 1), baseY - (unitSize - 1), (2 * unitSize) - 2, (2 * unitSize) - 2);
        g.drawLine(baseX - unitSize - 1, baseY, (baseX - unitSize) + 3, baseY);
        g.drawLine(baseX + unitSize + 1, baseY, (baseX + unitSize) - 3, baseY);
        g.drawLine(baseX, baseY - unitSize - 1, baseX, (baseY - unitSize) + 3);
        g.drawLine(baseX, baseY + unitSize + 1, baseX, (baseY + unitSize) - 3);
    }

    /** Draws the symbol for a single entity. Checks visibility in double blind. */
    private void paintUnit(Graphics g, Entity entity) {
        int x = entity.getPosition().getX();
        int y = entity.getPosition().getY();
        int facing = entity.getFacing();
        int baseX = x * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]) + leftMargin + HEX_SIDE[zoom] + xOffset;
        int baseY = (2 * y + 1 + (x % 2)) * HEX_SIDE_BY_COS30[zoom] + topMargin + yOffset;

        if (EntityVisibilityUtils.onlyDetectedBySensors(client.getLocalPlayer(), entity)) {
            // This unit is visible only as a sensor Return
            String sensorReturn = "?";
            Font font = new Font(MMConstants.FONT_SANS_SERIF, Font.BOLD, FONT_SIZE[zoom]);
            int width = getFontMetrics(font).stringWidth(sensorReturn) / 2;
            int height = getFontMetrics(font).getHeight() / 2 - 2;
            g.setFont(font);
            g.setColor(Color.RED);
            g.drawString(sensorReturn, baseX - width, baseY + height);
            return;
        } else if (game instanceof Game twGame && !EntityVisibilityUtils.detectedOrHasVisual(client.getLocalPlayer(), twGame, entity)) {
            // This unit is not visible, don't draw it
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        Stroke saveStroke = g2.getStroke();
        AffineTransform saveTransform = g2.getTransform();
        // Choose player or team color depending on preferences
        Color iconColor = entity.getOwner().getColour().getColour(false);
        if (GUIP.getTeamColoring()) {
            boolean isLocalTeam = entity.getOwner().getTeam() == client.getLocalPlayer().getTeam();
            boolean isLocalPlayer = entity.getOwner().equals(client.getLocalPlayer());
            if (isLocalPlayer) {
                iconColor = GUIP.getMyUnitColor();
            } else if (isLocalTeam) {
                iconColor = GUIP.getAllyUnitColor();
            } else {
                iconColor = GUIP.getEnemyUnitColor();
            }
        }
        Color iconColorSemiTransparent = new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), 200);


        // Transform for placement and scaling
        var placement = AffineTransform.getTranslateInstance(baseX, baseY);
        placement.scale(UNIT_SCALE[zoom] / 100.0d, UNIT_SCALE[zoom] / 100.0d);
        g2.transform(placement);

        // Add a position shift if multiple units are present in this hex
        Coords p = entity.getPosition();
        int eStack = multiUnits.getOrDefault(p, 0) + 1;
        multiUnits.put(p, eStack);
        g2.translate(20 * (eStack - 1), -20 * (eStack - 1));

        Path2D form = MinimapUnitSymbols.getForm(entity);

        Color borderColor = entity.moved != EntityMovementType.MOVE_NONE ? Color.BLACK : Color.WHITE;
        Color fontColor = Color.BLACK;

        float outerBorderWidth = 30f;
        float innerBorderWidth = 10f;
        float formStrokeWidth = 20f;

        // White border to set off the icon from the background
        g2.setStroke(new BasicStroke(outerBorderWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL));
        g2.setColor(Color.BLACK);
        g2.draw(STRAT_BASERECT);

        // Black background to fill forms like the DropShip
        g2.setColor(fontColor);
        g2.fill(STRAT_BASERECT);

        // Set a thin brush for filled areas (leave a thick brush for line symbols
        if ((entity instanceof Mek) || (entity instanceof ProtoMek)
                || (entity instanceof VTOL) || (entity.isAero())) {
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        } else {
            g2.setStroke(new BasicStroke(formStrokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        }

        // Fill the form in player color / team color
        g.setColor(iconColor);
        g2.fill(form);

        // Add the weight class or other lettering for certain units
        g.setColor(fontColor);
        if ((entity instanceof ProtoMek) || (entity instanceof Mek) || (entity instanceof Aero)) {
            String s = "";
            if (entity instanceof ProtoMek) {
                s = "P";
            } else if ((entity instanceof Mek) && ((Mek) entity).isIndustrial()) {
                s = "I";
            } else if (entity.getWeightClass() < 6) {
                s = STRAT_WEIGHTS[entity.getWeightClass()];
            }
            if (!s.isBlank()) {
                var fontContext = new FontRenderContext(null, true, true);
                var font = new Font(MMConstants.FONT_SANS_SERIF, Font.BOLD, 100);
                FontMetrics currentMetrics = getFontMetrics(font);
                int stringWidth = currentMetrics.stringWidth(s);
                GlyphVector gv = font.createGlyphVector(fontContext, s);
                g2.fill(gv.getOutline((int) STRAT_CX - (float) stringWidth / 2,
                        (float) STRAT_SYMBOLSIZE.getHeight() / 3.0f));
            }
        } else if (entity instanceof MekWarrior) {
            g2.setColor(fontColor);
            g2.fillOval(-25, -25, 50, 50);
        }
        // Draw the unit icon in black
        g2.draw(form);
        g2.setStroke(new BasicStroke(innerBorderWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));

       int sensorRange = Optional.ofNullable(entity.getActiveSensor()).map(Sensor::getRangeByBracket).orElse(0);
        if (sensorRange > 0) {
            sensorRange *= 130;
            g2.setColor(iconColorSemiTransparent);
            g2.drawOval(-sensorRange, -sensorRange, sensorRange * 2, sensorRange * 2);
        }

        // Rectangle border for all units
        g2.setColor(borderColor);
        g2.draw(STRAT_BASERECT);

        // Draw Facing Arrow
        if (facing > -1) {
            g2.setColor(Color.BLACK);
            g2.rotate(Math.toRadians(facing * 60));
            g2.draw(FACING_ARROW);
            g.setColor(iconColor);
            g2.fill(FACING_ARROW);
        }

        g2.setTransform(saveTransform);
        g2.setStroke(saveStroke);
    }


    /** Draws the symbol for a single entity. Checks visibility in double blind. */
    private void paintSensor(Graphics g, Entity entity) {

        if (EntityVisibilityUtils.onlyDetectedBySensors(client.getLocalPlayer(), entity)) {
            // This unit is visible only as a sensor Return
            return;
        } else if (game instanceof Game twGame && !EntityVisibilityUtils.detectedOrHasVisual(client.getLocalPlayer(), twGame, entity)) {
            // This unit is not visible, don't draw it
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        Stroke saveStroke = g2.getStroke();
        Color iconColor = entity.getOwner().getColour().getColour(false);
        if (GUIP.getTeamColoring()) {
            boolean isLocalTeam = entity.getOwner().getTeam() == client.getLocalPlayer().getTeam();
            boolean isLocalPlayer = entity.getOwner().equals(client.getLocalPlayer());
            if (isLocalPlayer) {
                iconColor = GUIP.getMyUnitColor();
            } else if (isLocalTeam) {
                iconColor = GUIP.getAllyUnitColor();
            } else {
                iconColor = GUIP.getEnemyUnitColor();
            }
        }
        Color iconColorSemiTransparent = new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), 200);
        // White border to set off the icon from the background
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL));
        g2.setColor(iconColorSemiTransparent);


        int maxSensorRange = 0;
        int minSensorRange = 0;

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_SENSORS)) {
            int bracket = Compute.getSensorRangeBracket(entity, null, null);
            // noinspection ConstantConditions
            int range = Compute.getSensorRangeByBracket((Game) game, entity, null, null);

            maxSensorRange = bracket * range;
            minSensorRange = Math.max((bracket - 1) * range, 0);
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_INCLUSIVE_SENSOR_RANGE)) {
                minSensorRange = 0;
            }
        }
        Coords origin = entity.getPosition();

        for (var sensorRange : List.of(minSensorRange, maxSensorRange)) {
            if (sensorRange <= 0) {
                continue;
            }

            int xo;
            int yo;
            var sensor = new Path2D.Double();

            var internalOrExternal = (sensorRange == minSensorRange) && (maxSensorRange != 0) ? -1 : 1;
            for (int i = 0; i < 6; i++) {
                var movingCoord = origin.translated(i, sensorRange + internalOrExternal);
                xo = coordsXToPixel(movingCoord.getX());
                yo = coordsYtoPixel(movingCoord.getY(), movingCoord.getX());
                if (i == 0) {
                    sensor.moveTo(xo+xOffset, yo+yOffset);
                } else {
                    sensor.lineTo(xo+xOffset, yo+yOffset);
                }
            }
            sensor.closePath();
            g2.draw(sensor);
        }
        g2.setStroke(saveStroke);
    }

    private int coordsYtoPixel(int y, int x) {
        return (2 * y + 1 + (x % 2)) * HEX_SIDE_BY_COS30[zoom] + topMargin;
    }

    private int coordsXToPixel(int x) {
        return x * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]) + leftMargin + HEX_SIDE[zoom];
    }

    public void forceBoardRedraw() {
        this.boardNeedsRedraw = true;
        repaint();
    }

    private int[] projectToView(int x, int y) {
        int baseX = x * (HEX_SIDE[zoom] + HEX_SIDE_BY_SIN30[zoom]) + leftMargin + HEX_SIDE[zoom];
        int baseY = (2 * y + 1 + (x % 2)) * HEX_SIDE_BY_COS30[zoom] + topMargin;
        return new int[]{baseX, baseY};
    }

    public void addMovePath(List<UnitLocation> unitLocations, Entity entity) {
        Coords previousCoords = entity.getPosition();
        for (var unitLocation : unitLocations) {
            var coords = unitLocation.getCoords();
            lines.add(new Line(previousCoords.getX(), previousCoords.getY(),
                coords.getX(), coords.getY(),
                Color.BLACK,
                game.getCurrentRound()));
            previousCoords = coords;
        }
    }

    @Override
    public void paintOverlays(Graphics g) {
        for (var overlay : overlays) {
            overlay.paint(g, getWidth(), getHeight());
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
