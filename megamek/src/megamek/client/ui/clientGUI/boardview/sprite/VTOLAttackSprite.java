/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.HexDrawUtilities;
import megamek.common.board.Coords;
import megamek.common.units.Entity;
import megamek.common.units.IBomber;
import megamek.common.units.VTOL;

/**
 * @author Neoancient
 */
public class VTOLAttackSprite extends Sprite {
    private final BoardView boardView;
    private final Entity entity;
    private List<Coords> targets;
    private final Color spriteColor;

    public VTOLAttackSprite(BoardView boardView, Entity entity) {
        super(boardView);
        this.boardView = boardView;
        this.entity = entity;
        spriteColor = entity.getOwner().getColour().getColour();
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
            x1 = x2 = (int) boardView.getHexLocation(targets.get(0)).getX();
            y1 = y2 = (int) boardView.getHexLocation(targets.get(0)).getX();
        }

        if (targets.size() > 1) {
            for (int i = 1; i < targets.size(); i++) {
                x1 = Math.min(x1, (int) boardView.getHexLocation(targets.get(i)).getX());
                y1 = Math.min(y1, (int) boardView.getHexLocation(targets.get(i)).getY());
                x2 = Math.max(x2, (int) boardView.getHexLocation(targets.get(i)).getX());
                y2 = Math.max(y2, (int) boardView.getHexLocation(targets.get(i)).getY());
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
    public void drawOnto(Graphics graphics, int x, int y, ImageObserver observer) {
        if (graphics instanceof Graphics2D graphics2D) {
            for (Coords c : targets) {
                boardView.drawHexBorder(graphics2D, boardView.getHexLocation(c), spriteColor, 0, 3);
            }
        }
    }

    public Entity getEntity() {
        return entity;
    }
}
