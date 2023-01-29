package megamek.client.ui.swing.boardview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.StraightArrowPolygon;
import megamek.common.*;
import megamek.common.actions.AttackAction;
import megamek.common.actions.ChargeAttackAction;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.DfaAttackAction;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.ProtomechPhysicalAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.actions.PushAttackAction;
import megamek.common.actions.SearchlightAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;
import static megamek.client.ui.swing.util.UIUtil.uiBlack;

/**
 * Sprite and info for an attack. Does not actually use the image buffer as
 * this can be horribly inefficient for long diagonal lines. Appears as an
 * arrow. Arrow becoming cut in half when two Meks attacking each other.
 */
class AttackSprite extends Sprite {
    private final BoardView boardView1;

    private Point a;

    private Point t;

    private double an;

    private StraightArrowPolygon attackPoly;

    private Color attackColor;

    private int entityId;

    private int targetType;

    private int targetId;

    private String attackerDesc;

    private String targetDesc;

    ArrayList<String> weaponDescs = new ArrayList<>();

    private final Entity ae;

    private final Targetable target;

    private Coords aCoord;
    private Coords tCoord;
    private IdealHex aHex;
    private IdealHex tHex;


    public AttackSprite(BoardView boardView1, final AttackAction attack) {
        super(boardView1);
        this.boardView1 = boardView1;
        entityId = attack.getEntityId();
        targetType = attack.getTargetType();
        targetId = attack.getTargetId();
        ae = this.boardView1.game.getEntity(attack.getEntityId());
        target = this.boardView1.game.getTarget(targetType, targetId);
        aCoord = ae.getPosition();
        tCoord = target.getPosition();
        aHex = new IdealHex(aCoord);
        tHex = new IdealHex(tCoord);

        // color?
        attackColor = ae.getOwner().getColour().getColour();
        // angle of line connecting two hexes
        Coords targetPosition;
        if (Compute.isGroundToAir(ae, target)) {
            targetPosition = Compute.getClosestFlightPath(ae.getId(),
                    ae.getPosition(), (Entity) target);
        } else {
            targetPosition = target.getPosition();
        }
        an = (ae.getPosition().radian(targetPosition) + (Math.PI * 1.5))
                % (Math.PI * 2); // angle
        makePoly();

        // set bounds
        bounds = new Rectangle(attackPoly.getBounds());
        bounds.setSize(bounds.getSize().width + 1,
                bounds.getSize().height + 1);
        // move poly to upper right of image
        attackPoly.translate(-bounds.getLocation().x,
                -bounds.getLocation().y);

        // set names & stuff
        attackerDesc = ae.getDisplayName();
        targetDesc = target.getDisplayName();
        if (attack instanceof WeaponAttackAction) {
            addWeapon((WeaponAttackAction) attack);
        }
        if (attack instanceof KickAttackAction) {
            addWeapon((KickAttackAction) attack);
        }
        if (attack instanceof PunchAttackAction) {
            addWeapon((PunchAttackAction) attack);
        }
        if (attack instanceof PushAttackAction) {
            addWeapon((PushAttackAction) attack);
        }
        if (attack instanceof ClubAttackAction) {
            addWeapon((ClubAttackAction) attack);
        }
        if (attack instanceof ChargeAttackAction) {
            addWeapon((ChargeAttackAction) attack);
        }
        if (attack instanceof DfaAttackAction) {
            addWeapon((DfaAttackAction) attack);
        }
        if (attack instanceof ProtomechPhysicalAttackAction) {
            addWeapon((ProtomechPhysicalAttackAction) attack);
        }
        if (attack instanceof SearchlightAttackAction) {
            addWeapon((SearchlightAttackAction) attack);
        }

        // nullify image
        image = null;
    }

    private void makePoly() {
        // make a polygon
        a = this.boardView1.getHexLocation(ae.getPosition());
        Coords targetPosition;
        if (Compute.isGroundToAir(ae, target)) {
            targetPosition = Compute.getClosestFlightPath(ae.getId(),
                    ae.getPosition(), (Entity) target);
        } else {
            targetPosition = target.getPosition();
        }
        t = this.boardView1.getHexLocation(targetPosition);
        // OK, that is actually not good. I do not like hard coded figures.
        // HEX_W/2 - x distance in pixels from origin of hex bounding box to
        // the center of hex.
        // HEX_H/2 - y distance in pixels from origin of hex bounding box to
        // the center of hex.
        // 18 - is actually 36/2 - we do not want arrows to start and end
        // directly
        // in the centes of hex and hiding mek under.

        a.x = a.x + (int) ((BoardView.HEX_W / 2) * this.boardView1.scale)
                + (int) Math.round(Math.cos(an) * (int) (18 * this.boardView1.scale));
        t.x = (t.x + (int) ((BoardView.HEX_W / 2) * this.boardView1.scale))
                - (int) Math.round(Math.cos(an) * (int) (18 * this.boardView1.scale));
        a.y = a.y + (int) ((BoardView.HEX_H / 2) * this.boardView1.scale)
                + (int) Math.round(Math.sin(an) * (int) (18 * this.boardView1.scale));
        t.y = (t.y + (int) ((BoardView.HEX_H / 2) * this.boardView1.scale))
                - (int) Math.round(Math.sin(an) * (int) (18 * this.boardView1.scale));

        // Checking if given attack is mutual. In this case we building
        // halved arrow
        if (isMutualAttack()) {
            attackPoly = new StraightArrowPolygon(a, t, (int) (8 * this.boardView1.scale),
                    (int) (12 * this.boardView1.scale), true);
        } else {
            attackPoly = new StraightArrowPolygon(a, t, (int) (4 * this.boardView1.scale),
                    (int) (8 * this.boardView1.scale), false);
        }
    }

    @Override
    public Rectangle getBounds() {
        makePoly();
        // set bounds
        bounds = new Rectangle(attackPoly.getBounds());
        bounds.setSize(bounds.getSize().width + 1,
                bounds.getSize().height + 1);
        // move poly to upper right of image
        attackPoly.translate(-bounds.getLocation().x,
                -bounds.getLocation().y);

        return bounds;
    }

    /**
     * If we have build full arrow already with single attack and have got
     * counter attack from our target lately - lets change arrow to halved.
     */
    public void rebuildToHalvedPolygon() {
        attackPoly = new StraightArrowPolygon(a, t, (int) (8 * this.boardView1.scale),
                (int) (12 * this.boardView1.scale), true);
        // set bounds
        bounds = new Rectangle(attackPoly.getBounds());
        bounds.setSize(bounds.getSize().width + 1,
                bounds.getSize().height + 1);
        // move poly to upper right of image
        attackPoly.translate(-bounds.getLocation().x,
                -bounds.getLocation().y);
    }

    /**
     * Cheking if attack is mutual and changing target arrow to half-arrow
     */
    private boolean isMutualAttack() {
        for (AttackSprite sprite : this.boardView1.attackSprites) {
            if ((sprite.getEntityId() == targetId)
                    && (sprite.getTargetId() == entityId)) {
                sprite.rebuildToHalvedPolygon();
                return true;
            }
        }
        return false;
    }

    @Override
    public void prepare() {
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
        Polygon drawPoly = new Polygon(attackPoly.xpoints,
                attackPoly.ypoints, attackPoly.npoints);
        drawPoly.translate(x, y);

        g.setColor(attackColor);
        g.fillPolygon(drawPoly);
        g.setColor(Color.gray.darker());
        g.drawPolygon(drawPoly);
    }

    /**
     * Return true if the point is inside our polygon
     */
    @Override
    public boolean isInside(Point point) {
        return attackPoly.contains(point.x - bounds.x, point.y - bounds.y);
    }

    public boolean isInside(Coords mcoords) {
        IdealHex mHex = new IdealHex(mcoords);

        return ((mHex.isIntersectedBy(aHex.cx, aHex.cy, tHex.cx, tHex.cy)) && (mcoords.between(aCoord, tCoord)));
    }

    public int getEntityId() {
        return entityId;
    }

    public int getTargetId() {
        return targetId;
    }

    /**
     * Adds a weapon to this attack
     */
    public void addWeapon(WeaponAttackAction attack) {
        final Entity entity = this.boardView1.game.getEntity(attack.getEntityId());
        final WeaponType wtype = (WeaponType) entity.getEquipment(
                attack.getWeaponId()).getType();
        final String roll = attack.toHit(this.boardView1.game).getValueAsString();
        String table = attack.toHit(this.boardView1.game).getTableDesc();
        if (!table.isEmpty()) {
            table = " " + table;
        }
        boolean b = false;
        ListIterator<String> i = weaponDescs.listIterator();
        while (i.hasNext()) {
             String s = i.next();
            if (s.contains(wtype.getName())) {
                i.set(s + ", " + roll + table);
                b = true;
            }
        }

        if (!b) {
            weaponDescs.add(wtype.getName() + Messages.getString("BoardView1.needs") + roll + table);
        }
    }

    public void addWeapon(KickAttackAction attack) {
        String bufer = "";
        String rollLeft = "";
        String rollRight = "";
        final int leg = attack.getLeg();
        switch (leg) {
            case KickAttackAction.BOTH:
                rollLeft = KickAttackAction.toHit(
                        this.boardView1.game,
                        attack.getEntityId(),
                        this.boardView1.game.getTarget(attack.getTargetType(),
                                attack.getTargetId()),
                        KickAttackAction.LEFT).getValueAsString();
                rollRight = KickAttackAction.toHit(
                        this.boardView1.game,
                        attack.getEntityId(),
                        this.boardView1.game.getTarget(attack.getTargetType(),
                                attack.getTargetId()),
                        KickAttackAction.RIGHT).getValueAsString();
                bufer = Messages.getString("BoardView1.kickBoth", rollLeft, rollRight);
                break;
            case KickAttackAction.LEFT:
                rollLeft = KickAttackAction.toHit(
                        this.boardView1.game,
                        attack.getEntityId(),
                        this.boardView1.game.getTarget(attack.getTargetType(),
                                attack.getTargetId()),
                        KickAttackAction.LEFT).getValueAsString();
                bufer = Messages.getString("BoardView1.kickLeft", rollLeft);
                break;
            case KickAttackAction.RIGHT:
                rollRight = KickAttackAction.toHit(
                        this.boardView1.game,
                        attack.getEntityId(),
                        this.boardView1.game.getTarget(attack.getTargetType(),
                                attack.getTargetId()),
                        KickAttackAction.RIGHT).getValueAsString();
                bufer = Messages.getString("BoardView1.kickRight", rollRight);
                break;
        }
        weaponDescs.add(bufer);
    }

    public void addWeapon(PunchAttackAction attack) {
        String bufer = "";
        String rollLeft;
        String rollRight;
        final int arm = attack.getArm();
        switch (arm) {
            case PunchAttackAction.BOTH:
                rollLeft = PunchAttackAction.toHit(
                        this.boardView1.game,
                        attack.getEntityId(),
                        this.boardView1.game.getTarget(attack.getTargetType(), attack.getTargetId()),
                        PunchAttackAction.LEFT, false).getValueAsString();
                rollRight = PunchAttackAction.toHit(
                        this.boardView1.game,
                        attack.getEntityId(),
                        this.boardView1.game.getTarget(attack.getTargetType(), attack.getTargetId()),
                        PunchAttackAction.RIGHT, false).getValueAsString();
                bufer = Messages.getString("BoardView1.punchBoth", rollLeft, rollRight);
                break;
            case PunchAttackAction.LEFT:
                rollLeft = PunchAttackAction.toHit(
                        this.boardView1.game,
                        attack.getEntityId(),
                        this.boardView1.game.getTarget(attack.getTargetType(), attack.getTargetId()),
                        PunchAttackAction.LEFT, false).getValueAsString();
                bufer = Messages.getString("BoardView1.punchLeft", rollLeft);
                break;
            case PunchAttackAction.RIGHT:
                rollRight = PunchAttackAction.toHit(
                        this.boardView1.game,
                        attack.getEntityId(),
                        this.boardView1.game.getTarget(attack.getTargetType(), attack.getTargetId()),
                        PunchAttackAction.RIGHT, false).getValueAsString();
                bufer = Messages.getString("BoardView1.punchRight", rollRight);
                break;
        }
        weaponDescs.add(bufer);
    }

    public void addWeapon(PushAttackAction attack) {
        final String roll = attack.toHit(this.boardView1.game).getValueAsString();
        weaponDescs.add(Messages.getString("BoardView1.push", roll));
    }

    public void addWeapon(ClubAttackAction attack) {
        final String roll = attack.toHit(this.boardView1.game).getValueAsString();
        final String club = attack.getClub().getName();
        weaponDescs.add(Messages.getString("BoardView1.hit", club, roll));
    }

    public void addWeapon(ChargeAttackAction attack) {
        final String roll = attack.toHit(this.boardView1.game).getValueAsString();
        weaponDescs.add(Messages.getString("BoardView1.charge", roll));
    }

    public void addWeapon(DfaAttackAction attack) {
        final String roll = attack.toHit(this.boardView1.game).getValueAsString();
        weaponDescs.add(Messages.getString("BoardView1.DFA", roll));
    }

    public void addWeapon(ProtomechPhysicalAttackAction attack) {
        final String roll = attack.toHit(this.boardView1.game).getValueAsString();
        weaponDescs.add(Messages.getString("BoardView1.proto", roll));
    }

    public void addWeapon(SearchlightAttackAction attack) {
        weaponDescs.add(Messages.getString("BoardView1.Searchlight"));
    }

    @Override
    public StringBuffer getTooltip() {
        GamePhase phase = this.boardView1.game.getPhase();
        String result = "";
        String f = "";

        f = attackerDesc + "<BR>&nbsp;&nbsp;" + Messages.getString("BoardView1.on") + " " + targetDesc;
        result = guiScaledFontHTML(attackColor) + f + "</FONT>";
        f = "";
        if ((phase.isFiring()) || (phase.isPhysical())) {
            for (String wpD : weaponDescs) {
                f += "<BR>" + wpD;
            }
            result += guiScaledFontHTML(uiBlack()) + f + "</FONT>";
        }
        return new StringBuffer().append(result);
    }
}