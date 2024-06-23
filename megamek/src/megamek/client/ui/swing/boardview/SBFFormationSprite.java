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

import megamek.client.ui.swing.util.StringDrawer;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.strategicBattleSystems.SBFFormation;

import java.awt.*;

/**
 * Sprite for an entity. Changes whenever the entity changes. Consists of an
 * image, drawn from the Tile Manager; facing and possibly secondary facing
 * arrows; armor and internal bars; and an identification label.
 */
class SBFFormationSprite extends Sprite {

    private final SBFFormation formation;
    private final Player owner;

    private Rectangle entityRect;
    /** Used to color the label when this unit is selected for movement etc. */
    private boolean isSelected;

    public SBFFormationSprite(BoardView boardView, SBFFormation formation, Player owner) {
        super(boardView);
        this.formation = formation;
        this.owner = owner;
        getBounds();
    }

    @Override
    public Rectangle getBounds() {
        bounds = new Rectangle(0, 0, bv.hex_size.width, bv.hex_size.height);
        Point ePos = bv.getHexLocation(formation.getPosition().getCoords());
        bounds.setLocation(ePos.x, ePos.y);

        entityRect = new Rectangle(bounds.x + (int) (20 * bv.scale), bounds.y
                + (int) (14 * bv.scale), (int) (44 * bv.scale),
                (int) (44 * bv.scale));

        return bounds;
    }

    @Override
    public void prepare() {
        getBounds();

        // create image for buffer
        GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration();
        image = config.createCompatibleImage(bounds.width, bounds.height, Transparency.TRANSLUCENT);
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);

        graph.scale(bv.scale, bv.scale);
        if (isSelected) {
            graph.setColor(Color.WHITE);
        } else {
            graph.setColor(Color.DARK_GRAY);
        }
        graph.setStroke(new BasicStroke(2));
        graph.drawImage(owner.getCamouflage().getImage(), 15, 15, 84-30, 72-30, null);
        graph.drawRoundRect(10, 10, 84-20, 72-20, 5, 5);
        graph.setColor(owner.getColour().getColour());
        graph.fillRoundRect(10, 10, 84-20, 72-20, 5, 5);
        new StringDrawer(formation.getType().toString()).at(42, 36).absoluteCenter().color(Color.DARK_GRAY).draw(graph);
        graph.dispose();
    }

    @Override
    public boolean isInside(Point point) {
        return entityRect.contains(point.x, point.y);
    }

    /** Marks the entity as selected for movement etc., recoloring the label */
    public void setSelected(boolean status) {
        if (isSelected != status) {
            isSelected = status;
            prepare();
        }
    }

    /** Returns if the entity is marked as selected for movement etc., recoloring the label */
    public boolean getSelected() {
        return isSelected;
    }
}
