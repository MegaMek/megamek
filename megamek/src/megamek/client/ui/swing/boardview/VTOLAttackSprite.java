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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IBomber;
import megamek.common.VTOL;

/**
 * @author Neoancient
 */
class VTOLAttackSprite extends Sprite {
    private BoardView bv;
    private Entity entity;
    private List<Coords> targets;
    private Color spriteColor;

    VTOLAttackSprite(BoardView boardView, Entity en) {
        super(boardView);
        this.bv = boardView;
        this.entity = en;
        spriteColor = en.getOwner().getColour().getColour();
        image = null;
        prepare();
    }

    @Override
    public void prepare() {
        if ((entity instanceof IBomber) && ((IBomber) entity).isVTOLBombing()) {
            targets = Collections.singletonList(((IBomber) entity).getVTOLBombTarget().getPosition());
        } else if (entity instanceof VTOL) {
            targets = new ArrayList<>(((VTOL) entity).getStrafingCoords());
        } else {
            targets = Collections.emptyList();
        }
        int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
        if (!targets.isEmpty()) {
            x1 = x2 = (int) bv.getHexLocation(targets.get(0)).getX();
            y1 = y2 = (int) bv.getHexLocation(targets.get(0)).getX();
        }

        if (targets.size() > 1) {
            for (int i = 1; i < targets.size(); i++) {
                x1 = Math.min(x1, (int) bv.getHexLocation(targets.get(i)).getX());
                y1 = Math.min(y1, (int) bv.getHexLocation(targets.get(i)).getY());
                x2 = Math.max(x2, (int) bv.getHexLocation(targets.get(i)).getX());
                y2 = Math.max(y2, (int) bv.getHexLocation(targets.get(i)).getY());
            }
        }
        Shape hex = HexDrawUtilities.getHexFullBorderArea(3);
        bounds = new Rectangle(x1 - 1,
              y1 - 1,
              x2 + (int) hex.getBounds().getWidth() + 1,
              y2 + (int) hex.getBounds().getHeight() + 1);
    }

    @Override
    public boolean isReady() {
        return targets != null;
    }

    @Override
    public void drawOnto(Graphics g, int x, int y, ImageObserver observer) {
        if (g instanceof Graphics2D graphics2D) {
            for (Coords c : targets) {
                bv.drawHexBorder(graphics2D, bv.getHexLocation(c), spriteColor, 0, 3);
            }
        }
    }

    public Entity getEntity() {
        return entity;
    }
}
