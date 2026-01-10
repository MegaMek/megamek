/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is attackingPoint non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.clientGUI.boardview.sprite;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.ImageObserver;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.tooltip.EntityActionLog;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.StraightArrowPolygon;
import megamek.client.ui.util.UIUtil;
import megamek.common.IdealHex;
import megamek.common.actions.AttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.exceptions.AttackingEntityIsNullException;
import megamek.common.exceptions.TargetedEntityIsNullException;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

/**
 * Sprite and info for angle attack. Does not actually use the image buffer as this can be horribly inefficient for long
 * diagonal lines. Appears as angle arrow. Arrow becoming cut in half when two Meks attacking each other.
 */
public class AttackSprite extends Sprite {
    private final BoardView boardView1;

    private Point attackingPoint;

    private Point targetPoint;

    private final double angle;

    private StraightArrowPolygon attackPoly;

    private final Color attackColor;

    private final int entityId;

    private final int targetId;

    private final String attackerDesc;

    private final String targetDesc;

    EntityActionLog attacks;

    private final Entity attackingEntity;

    private final Targetable targetedEntity;

    private final Coords attackCoord;
    private final Coords targetCoord;
    private final IdealHex attackHex;
    private final IdealHex targetHex;


    public AttackSprite(BoardView boardView1, final AttackAction attack) {
        super(boardView1);

        attacks = new EntityActionLog(boardView1.getClientgui().getClient().getGame());
        this.boardView1 = boardView1;
        entityId = attack.getEntityId();
        int targetType = attack.getTargetType();
        targetId = attack.getTargetId();
        Entity weaponEntity = this.boardView1.game.getEntity(attack.getEntityId());
        attackingEntity = weaponEntity != null ? weaponEntity.getAttackingEntity() : null;
        targetedEntity = this.boardView1.game.getTarget(targetType, targetId);

        if (attackingEntity == null) {
            throw new AttackingEntityIsNullException("AttackSprite");
        }

        if (targetedEntity == null) {
            throw new TargetedEntityIsNullException("AttackSprite");
        }

        attackCoord = attackingEntity.getPosition();
        targetCoord = targetedEntity.getPosition();
        attackHex = new IdealHex(attackCoord);
        targetHex = new IdealHex(targetCoord);

        // color?
        attackColor = attackingEntity.getOwner().getColour().getColour();
        // angle of line connecting two hexes
        Coords targetPosition;
        if (Compute.isGroundToAir(attackingEntity, targetedEntity)) {
            targetPosition = Compute.getClosestFlightPath(attackingEntity.getId(),
                  attackingEntity.getPosition(), (Entity) targetedEntity);
        } else {
            targetPosition = targetedEntity.getPosition();
        }
        angle = (attackingEntity.getPosition().radian(targetPosition) + (Math.PI * 1.5))
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
        attackerDesc = attackingEntity.getDisplayName();
        targetDesc = targetedEntity.getDisplayName();
        addEntityAction(attack);

        // nullify image
        image = null;
    }

    public void addEntityAction(EntityAction entityAction) {
        attacks.add(entityAction);
    }

    /**
     * reuild the text descriptions to reflect changes in ToHits from adding or removing other attacks such as
     * secondaryTarget
     */
    public void rebuildDescriptions() {
        attacks.rebuildDescriptions();
    }

    private void makePoly() {
        // make attackingPoint polygon
        attackingPoint = this.boardView1.getHexLocation(attackingEntity.getPosition());
        Coords targetPosition;
        if (Compute.isGroundToAir(attackingEntity, targetedEntity)) {
            targetPosition = Compute.getClosestFlightPath(attackingEntity.getId(),
                  attackingEntity.getPosition(), (Entity) targetedEntity);
        } else {
            targetPosition = targetedEntity.getPosition();
        }
        targetPoint = this.boardView1.getHexLocation(targetPosition);
        // OK, that is actually not good. I do not like hard coded figures.
        // HEX_W/2 - x distance in pixels from origin of hex bounding box to the center of hex.
        // HEX_H/2 - y distance in pixels from origin of hex bounding box to the center of hex.
        // 18 - is actually 36/2 - we do not want arrows to start and end directly in the centers of hex and hiding
        // mek under.

        attackingPoint.x = attackingPoint.x + (int) Math.floor((HexTileset.HEX_W / 2.0f) * this.boardView1.getScale())
              + (int) Math.round(Math.cos(angle) * (int) (18 * this.boardView1.getScale()));
        targetPoint.x = (targetPoint.x + (int) ((HexTileset.HEX_W / 2.0f) * this.boardView1.getScale()))
              - (int) Math.round(Math.cos(angle) * (int) (18 * this.boardView1.getScale()));
        attackingPoint.y = attackingPoint.y + (int) ((HexTileset.HEX_H / 2.0f) * this.boardView1.getScale())
              + (int) Math.round(Math.sin(angle) * (int) (18 * this.boardView1.getScale()));
        targetPoint.y = (targetPoint.y + (int) ((HexTileset.HEX_H / 2.0f) * this.boardView1.getScale()))
              - (int) Math.round(Math.sin(angle) * (int) (18 * this.boardView1.getScale()));

        // Checking if given attack is mutual. In this case we're building halved arrow
        if (isMutualAttack()) {
            attackPoly = new StraightArrowPolygon(attackingPoint, targetPoint, (int) (8 * this.boardView1.getScale()),
                  (int) (12 * this.boardView1.getScale()), true);
        } else {
            attackPoly = new StraightArrowPolygon(attackingPoint, targetPoint, (int) (4 * this.boardView1.getScale()),
                  (int) (8 * this.boardView1.getScale()), false);
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
     * If we have build full arrow already with single attack and have got counter attack from our targetedEntity lately
     * - lets change arrow to halved.
     */
    public void rebuildToHalvedPolygon() {
        attackPoly = new StraightArrowPolygon(attackingPoint, targetPoint, (int) (8 * this.boardView1.getScale()),
              (int) (12 * this.boardView1.getScale()), true);
        // set bounds
        bounds = new Rectangle(attackPoly.getBounds());
        bounds.setSize(bounds.getSize().width + 1,
              bounds.getSize().height + 1);
        // move poly to upper right of image
        attackPoly.translate(-bounds.getLocation().x,
              -bounds.getLocation().y);
    }

    /**
     * Cheking if attack is mutual and changing targetedEntity arrow to half-arrow
     */
    private boolean isMutualAttack() {
        for (AttackSprite sprite : this.boardView1.getAttackSprites()) {
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
    public void drawOnto(Graphics graphics, int x, int y, ImageObserver observer) {
        createPolygon(graphics, x, y, attackPoly, attackColor);
    }

    static void createPolygon(Graphics graphics, int x, int y, StraightArrowPolygon attackPoly, Color attackColor) {
        Polygon drawPoly = new Polygon(attackPoly.xpoints,
              attackPoly.ypoints, attackPoly.npoints);
        drawPoly.translate(x, y);

        graphics.setColor(attackColor);
        graphics.fillPolygon(drawPoly);
        graphics.setColor(Color.gray.darker());
        graphics.drawPolygon(drawPoly);
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

        return ((mHex.isIntersectedBy(attackHex.cx, attackHex.cy, targetHex.cx, targetHex.cy)) && (mcoords.between(
              attackCoord,
              targetCoord)));
    }

    public int getEntityId() {
        return entityId;
    }

    public int getTargetId() {
        return targetId;
    }

    @Override
    public StringBuffer getTooltip() {
        GamePhase phase = this.boardView1.game.getPhase();
        String result;
        String sAttacherDesc;

        sAttacherDesc = attackerDesc + "<BR>&nbsp;&nbsp;" + Messages.getString("BoardView1.on") + " " + targetDesc;
        result = UIUtil.fontHTML(attackColor) + sAttacherDesc + "</FONT>";
        StringBuilder sAttacks = new StringBuilder();
        if ((phase.isFiring()) || (phase.isPhysical())) {
            for (String wpD : attacks.getDescriptions()) {
                sAttacks.append("<BR>").append(wpD);
            }
            result += sAttacks;
        }
        return new StringBuffer().append(result);
    }
}
