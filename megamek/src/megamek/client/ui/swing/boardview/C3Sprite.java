package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;

import megamek.common.Entity;

/**
 * Sprite and info for a C3 network. Does not actually use the image buffer
 * as this can be horribly inefficient for long diagonal lines.
 */
class C3Sprite extends Sprite {

    private Polygon c3Poly;

    protected int entityId;

    protected int masterId;

    protected Entity entityE;

    protected Entity entityM;

    Color spriteColor;

    public C3Sprite(BoardView boardView1, final Entity e, final Entity m) {
        super(boardView1);
        entityE = e;
        entityM = m;
        entityId = e.getId();
        masterId = m.getId();
        spriteColor = e.getOwner().getColour().getColour();

        if ((e.getPosition() == null) || (m.getPosition() == null)) {
            c3Poly = new Polygon();
            c3Poly.addPoint(0, 0);
            c3Poly.addPoint(1, 0);
            c3Poly.addPoint(0, 1);
            bounds = new Rectangle(c3Poly.getBounds());
            bounds.setSize(bounds.getSize().width + 1,
                    bounds.getSize().height + 1);
            image = null;
            return;
        }

        makePoly();

        // set bounds
        bounds = new Rectangle(c3Poly.getBounds());
        bounds.setSize(bounds.getSize().width + 1,
                bounds.getSize().height + 1);

        // move poly to upper right of image
        c3Poly.translate(-bounds.getLocation().x, -bounds.getLocation().y);

        // set names & stuff

        // nullify image
        image = null;
    }

    @Override
    public void prepare() {
    }

    private void makePoly() {
        // make a polygon
        final Point a = bv.getHexLocation(entityE.getPosition());
        final Point t = this.bv.getHexLocation(entityM.getPosition());

        final double an = (entityE.getPosition().radian(
                entityM.getPosition()) + (Math.PI * 1.5))
                % (Math.PI * 2); // angle
        final double lw = this.bv.scale * BoardView.C3_LINE_WIDTH; // line width

        c3Poly = new Polygon();
        c3Poly.addPoint(
                a.x
                        + (int) ((this.bv.scale * (BoardView.HEX_W / 2)) - (int) Math
                                .round(Math.sin(an) * lw)),
                a.y
                        + (int) ((this.bv.scale * (BoardView.HEX_H / 2)) + (int) Math
                                .round(Math.cos(an) * lw)));
        c3Poly.addPoint(
                a.x
                        + (int) ((this.bv.scale * (BoardView.HEX_W / 2)) + (int) Math
                                .round(Math.sin(an) * lw)),
                a.y
                        + (int) ((this.bv.scale * (BoardView.HEX_H / 2)) - (int) Math
                                .round(Math.cos(an) * lw)));
        c3Poly.addPoint(
                t.x
                        + (int) ((this.bv.scale * (BoardView.HEX_W / 2)) + (int) Math
                                .round(Math.sin(an) * lw)),
                t.y
                        + (int) ((this.bv.scale * (BoardView.HEX_H / 2)) - (int) Math
                                .round(Math.cos(an) * lw)));
        c3Poly.addPoint(
                t.x
                        + (int) ((this.bv.scale * (BoardView.HEX_W / 2)) - (int) Math
                                .round(Math.sin(an) * lw)),
                t.y
                        + (int) ((this.bv.scale * (BoardView.HEX_H / 2)) + (int) Math
                                .round(Math.cos(an) * lw)));
    }

    @Override
    public Rectangle getBounds() {
        makePoly();
        // set bounds
        bounds = new Rectangle(c3Poly.getBounds());
        bounds.setSize(bounds.getSize().width + 1,
                bounds.getSize().height + 1);

        // move poly to upper right of image
        c3Poly.translate(-bounds.getLocation().x, -bounds.getLocation().y);
        image = null;

        return bounds;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {

        Polygon drawPoly = new Polygon(c3Poly.xpoints, c3Poly.ypoints,
                c3Poly.npoints);
        drawPoly.translate(x, y);

        g.setColor(spriteColor);
        g.fillPolygon(drawPoly);
        g.setColor(Color.black);
        g.drawPolygon(drawPoly);
    }

    /**
     * Return true if the point is inside our polygon
     */
    @Override
    public boolean isInside(Point point) {
        return c3Poly.contains(point.x - bounds.x, point.y - bounds.y);
    }

}