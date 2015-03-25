package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;

import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Coords;
import megamek.common.Entity;

/**
 * Sprite and info for an aero flyover route. Does not actually use the
 * image buffer as this can be horribly inefficient for long diagonal lines.
 */
class FlyOverSprite extends Sprite {

    private Polygon flyOverPoly;

    protected Entity en;

    Color spriteColor;

    public FlyOverSprite(BoardView1 boardView1, final Entity e) {
        super(boardView1);
        en = e;
        spriteColor = PlayerColors.getColor(e.getOwner().getColorIndex());

        if ((e.getPosition() == null) || (e.getPassedThrough().size() < 2)) {
            flyOverPoly = new Polygon();
            flyOverPoly.addPoint(0, 0);
            flyOverPoly.addPoint(1, 0);
            flyOverPoly.addPoint(0, 1);
            bounds = new Rectangle(flyOverPoly.getBounds());
            bounds.setSize(bounds.getSize().width + 1,
                    bounds.getSize().height + 1);
            image = null;
            return;
        }

        makePoly();

        // set bounds
        bounds = new Rectangle(flyOverPoly.getBounds());
        bounds.setSize(bounds.getSize().width + 1,
                bounds.getSize().height + 1);

        // move poly to upper right of image
        flyOverPoly.translate(-bounds.getLocation().x,
                -bounds.getLocation().y);

        // set names & stuff

        // nullify image
        image = null;
    }

    @Override
    public void prepare() {
    }

    private void makePoly() {
        // make a polygon
        flyOverPoly = new Polygon();
        for (Coords c : en.getPassedThrough()) {
            Coords prev = en.passedThroughPrevious(c);
            if (prev.equals(c)) {
                continue;
            }
            Point a = this.bv.getHexLocation(prev);
            Point t = this.bv.getHexLocation(c);

            final double an = (prev.radian(c) + (Math.PI * 1.5))
                    % (Math.PI * 2); // angle
            final double lw = this.bv.scale * BoardView1.FLY_OVER_LINE_WIDTH; // line width

            flyOverPoly.addPoint(
                    a.x
                            + (int) ((this.bv.scale * (BoardView1.HEX_W / 2)) - (int) Math
                                    .round(Math.sin(an) * lw)),
                    a.y
                            + (int) ((this.bv.scale * (BoardView1.HEX_H / 2)) + (int) Math
                                    .round(Math.cos(an) * lw)));
            // flyOverPoly.addPoint(
            // a.x + (int) (scale * (HEX_W / 2) + (int)
            // Math.round(Math.sin(an) * lw)), a.y
            // + (int) (scale * (HEX_H / 2) - (int) Math.round(Math.cos(an)
            // * lw)));
            // flyOverPoly.addPoint(
            // t.x + (int) (scale * (HEX_W / 2) + (int)
            // Math.round(Math.sin(an) * lw)), t.y
            // + (int) (scale * (HEX_H / 2) - (int) Math.round(Math.cos(an)
            // * lw)));
            flyOverPoly.addPoint(
                    t.x
                            + (int) ((this.bv.scale * (BoardView1.HEX_W / 2)) - (int) Math
                                    .round(Math.sin(an) * lw)),
                    t.y
                            + (int) ((this.bv.scale * (BoardView1.HEX_H / 2)) + (int) Math
                                    .round(Math.cos(an) * lw)));

        }

        // now loop through backwards
        for (int i = (en.getPassedThrough().size() - 1); i > 0; i--) {
            Coords c = en.getPassedThrough().elementAt(i);
            Coords next = en.getPassedThrough().elementAt(i - 1);
            Point a = this.bv.getHexLocation(c);
            Point t = this.bv.getHexLocation(next);

            final double an = (c.radian(next) + (Math.PI * 1.5))
                    % (Math.PI * 2); // angle
            final double lw = this.bv.scale * BoardView1.FLY_OVER_LINE_WIDTH; // line width
            // flyOverPoly.addPoint(
            // a.x + (int) (scale * (HEX_W / 2) + (int)
            // Math.round(Math.sin(an) * lw)), a.y
            // + (int) (scale * (HEX_H / 2) - (int) Math.round(Math.cos(an)
            // * lw)));
            // flyOverPoly.addPoint(
            // t.x + (int) (scale * (HEX_W / 2) + (int)
            // Math.round(Math.sin(an) * lw)), t.y
            // + (int) (scale * (HEX_H / 2) - (int) Math.round(Math.cos(an)
            // * lw)));

            flyOverPoly.addPoint(
                    a.x
                            + (int) ((this.bv.scale * (BoardView1.HEX_W / 2)) - (int) Math
                                    .round(Math.sin(an) * lw)),
                    a.y
                            + (int) ((this.bv.scale * (BoardView1.HEX_H / 2)) + (int) Math
                                    .round(Math.cos(an) * lw)));
            flyOverPoly.addPoint(
                    t.x
                            + (int) ((this.bv.scale * (BoardView1.HEX_W / 2)) - (int) Math
                                    .round(Math.sin(an) * lw)),
                    t.y
                            + (int) ((this.bv.scale * (BoardView1.HEX_H / 2)) + (int) Math
                                    .round(Math.cos(an) * lw)));

        }

    }

    @Override
    public Rectangle getBounds() {
        makePoly();
        // set bounds
        bounds = new Rectangle(flyOverPoly.getBounds());
        bounds.setSize(bounds.getSize().width + 1,
                bounds.getSize().height + 1);

        // move poly to upper right of image
        flyOverPoly.translate(-bounds.getLocation().x,
                -bounds.getLocation().y);
        image = null;

        return bounds;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    public int getEntityId() {
        return en.getId();
    }
    
    public Entity getEntity() {
        return en;
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {

        Polygon drawPoly = new Polygon(flyOverPoly.xpoints,
                flyOverPoly.ypoints, flyOverPoly.npoints);
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
        return flyOverPoly.contains(point.x - bounds.x, point.y - bounds.y);
    }

}