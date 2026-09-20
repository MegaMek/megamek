/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import megamek.client.ui.tileset.HexTileset;
import org.apache.batik.ext.awt.g2d.AbstractGraphics2D;
import org.apache.batik.ext.awt.g2d.GraphicContext;

/**
 * Records the existing tactical painters as vectors instead of allocating their bitmap buffers.
 * Batik supplies the ordinary Graphics2D state/primitive operations; no SVG or board image is produced.
 */
public final class BoardTacticalGraphics extends AbstractGraphics2D {
    private final List<BoardTactical.Fill> fills;
    private final List<BoardTactical.Label> labels;
    private final Graphics2D metrics;
    private BoardTactical.Point anchor;
    private BoardTactical.Playback playback = BoardTactical.Playback.LIVE;

    public BoardTacticalGraphics() {
        super(false);
        gc = new GraphicContext();
        fills = new ArrayList<>();
        labels = new ArrayList<>();
        metrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        setFont(metrics.getFont());
    }

    private BoardTacticalGraphics(BoardTacticalGraphics parent) {
        super(parent);
        fills = parent.fills;
        labels = parent.labels;
        metrics = (Graphics2D) parent.metrics.create();
        anchor = parent.anchor;
        playback = parent.playback;
    }

    public BoardTactical snapshot() {
        return new BoardTactical(fills, labels);
    }

    /** Tags only the native capture; classic painters keep their existing graphics state and behavior. */
    public static void draw(Graphics2D graphics, BoardTactical.Playback playback, Consumer<Graphics2D> painter) {
        if (!(graphics instanceof BoardTacticalGraphics)) {
            painter.accept(graphics);
            return;
        }
        BoardTacticalGraphics local = (BoardTacticalGraphics) graphics.create();
        try {
            local.playback = playback;
            painter.accept(local);
        } finally {
            local.dispose();
        }
    }

    /** Local hex coordinates, with a common anchor for camera-facing text. */
    public static Graphics2D at(Graphics2D graph, java.awt.Point location) {
        Graphics2D copy = (Graphics2D) graph.create();
        copy.translate(location.x, location.y);
        if (copy instanceof BoardTacticalGraphics tactical) {
            tactical.anchor = new BoardTactical.Point(location.x + HexTileset.HEX_W / 2f,
                  location.y + HexTileset.HEX_H / 2f);
        }
        return copy;
    }

    @Override
    public Graphics create() {
        return new BoardTacticalGraphics(this);
    }

    @Override
    public void dispose() {
        metrics.dispose();
    }

    private int argb() {
        if (!(getPaint() instanceof Color color) || !(getComposite() instanceof AlphaComposite composite)
              || composite.getRule() != AlphaComposite.SRC_OVER) {
            throw new IllegalStateException("Native tactical shapes require solid SRC_OVER paint");
        }
        return (Math.round(color.getAlpha() * composite.getAlpha()) << 24) | (color.getRGB() & 0xFFFFFF);
    }

    @Override
    public void draw(Shape shape) {
        fill(getStroke().createStrokedShape(shape));
    }

    @Override
    public void fill(Shape shape) {
        int color = argb();
        if ((color >>> 24) == 0) {
            return;
        }
        if (getClip() != null) {
            Area clipped = new Area(shape);
            clipped.intersect(new Area(getClip()));
            shape = clipped;
        }
        PathIterator path = shape.getPathIterator(getTransform(), 0.25);
        int winding = path.getWindingRule();
        List<BoardTactical.Contour> contours = new ArrayList<>();
        List<BoardTactical.Point> points = new ArrayList<>();
        float[] xy = new float[6];
        while (!path.isDone()) {
            int segment = path.currentSegment(xy);
            if (segment == PathIterator.SEG_MOVETO && !points.isEmpty()) {
                contours.add(new BoardTactical.Contour(points));
                points = new ArrayList<>();
            }
            if (segment != PathIterator.SEG_CLOSE) {
                points.add(new BoardTactical.Point(xy[0], xy[1]));
            }
            path.next();
        }
        if (!points.isEmpty()) {
            contours.add(new BoardTactical.Contour(points));
        }
        if (!contours.isEmpty()) {
            fills.add(new BoardTactical.Fill(contours, winding, color, playback));
        }
    }

    @Override
    public void drawString(String text, float x, float y) {
        if (text.isEmpty()) {
            return;
        }
        Point2D point = getTransform().transform(new Point2D.Float(x, y), null);
        BoardTactical.Point origin = anchor == null
              ? new BoardTactical.Point((float) point.getX(), (float) point.getY()) : anchor;
        float scale = (float) Math.hypot(getTransform().getScaleY(), getTransform().getShearX());
        labels.add(new BoardTactical.Label(origin, new BoardTactical.Text(text,
              getFont().deriveFont(getFont().getSize2D() * scale),
              (float) point.getX() - origin.x(), (float) point.getY() - origin.y(), argb()), playback));
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        new TextLayout(iterator, getFontRenderContext()).draw(this, x, y);
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        return metrics.getFontMetrics(font);
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return metrics.getDeviceConfiguration();
    }

    @Override
    public void setXORMode(Color color) {
        throw new UnsupportedOperationException("Tactical vectors do not use XOR");
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        throw new UnsupportedOperationException("Tactical vectors do not copy raster areas");
    }

    @Override
    public boolean drawImage(Image image, int x, int y, ImageObserver observer) {
        throw new UnsupportedOperationException("Raster artwork belongs in the compatibility layer");
    }

    @Override
    public boolean drawImage(Image image, int x, int y, int width, int height, ImageObserver observer) {
        throw new UnsupportedOperationException("Raster artwork belongs in the compatibility layer");
    }

    @Override
    public void drawRenderedImage(RenderedImage image, AffineTransform transform) {
        throw new UnsupportedOperationException("Raster artwork belongs in the compatibility layer");
    }

    @Override
    public void drawRenderableImage(RenderableImage image, AffineTransform transform) {
        throw new UnsupportedOperationException("Raster artwork belongs in the compatibility layer");
    }
}
