/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.util.List;
import java.util.Objects;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.StringDrawer;
import megamek.client.ui.util.UIUtil;
import megamek.codeUtilities.MathUtility;
import megamek.common.Player;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFGame;

/**
 * Sprite for an entity. Changes whenever the entity changes. Consists of an image, drawn from the Tile Manager; facing
 * and possibly secondary facing arrows; armor and internal bars; and an identification label.
 */
public class SBFFormationSprite extends Sprite {

    private static final int INSET = 10;

    private final SBFFormation formation;
    private final Player owner;
    private final int positionInHex;
    private final int formationCountInHex;

    /** The area actually covered by the icon */
    private Rectangle hitBox;

    /** Used to color the label when this unit is selected for movement etc. */
    private boolean isSelected;


    public SBFFormationSprite(BoardView boardView, SBFFormation formation, Player owner, SBFGame game) {
        super(boardView);
        this.formation = Objects.requireNonNull(formation);
        this.owner = owner;
        List<SBFFormation> formationsInHex = game.getActiveFormationsAt(formation.getPosition());
        formationCountInHex = formationsInHex.size();
        positionInHex = MathUtility.clamp(formationsInHex.indexOf(formation), 0, 3);
        getBounds();
    }

    @Override
    public Rectangle getBounds() {
        bounds = new Rectangle(0, 0, bv.getHexSize().width, bv.getHexSize().height);
        Point ePos = bv.getHexLocation(formation.getPosition().coords());
        bounds.setLocation(ePos.x, ePos.y);

        hitBox = new Rectangle(bounds.x + INSET, bounds.y + INSET,
              bounds.width - 2 * INSET, bounds.height - 2 * INSET);

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

        graph.scale(bv.getScale(), bv.getScale());
        graph.translate(positionInHex > 1 ? 42 : 0, (positionInHex % 2 == 1) ? 36 : 0);
        double scaling = formationCountInHex > 1 ? 0.5 : 1;
        graph.scale(scaling, scaling);
        if (isSelected) {
            graph.setColor(Color.WHITE);
        } else if (formation.isDone()) {
            graph.setColor(Color.DARK_GRAY);
        } else {
            graph.setColor(Color.GREEN);
        }
        graph.setStroke(new BasicStroke(2));
        graph.drawImage(owner.getCamouflage().getImage(), INSET + INSET / 2, INSET + INSET / 2,
              84 - 3 * INSET, 72 - 3 * INSET, null);
        graph.drawRoundRect(INSET, INSET, 84 - 2 * INSET, 72 - 2 * INSET,
              INSET / 2, INSET / 2);
        graph.setColor(owner.getColour().getColour());
        graph.fillRoundRect(INSET, INSET, 84 - 2 * INSET, 72 - 2 * INSET,
              INSET / 2, INSET / 2);
        new StringDrawer(formation.getType().toString()).at(42, 36).absoluteCenter().color(Color.DARK_GRAY).draw(graph);
        graph.dispose();
    }

    @Override
    public boolean isInside(Point point) {
        return hitBox.contains(point.x, point.y);
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

    public SBFFormation getFormation() {
        return formation;
    }
}
