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
import megamek.codeUtilities.MathUtility;
import megamek.common.Player;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFSomethingOutThereUnitPlaceHolder;
import megamek.common.strategicBattleSystems.SBFUnitPlaceHolder;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public class SBFPlaceHolderSprite extends Sprite {

    private static final int INSET = 10;

    private final SBFUnitPlaceHolder placeHolder;
    private final Player owner;
//    private final int positionInHex;
//    private final int formationCountInHex;

    /** The area actually covered by the icon */
    private Rectangle hitBox;

    /** Used to color the label when this unit is selected for movement etc. */
//    private boolean isSelected;


    public SBFPlaceHolderSprite(BoardView boardView, SBFUnitPlaceHolder placeHolder, Player owner, SBFGame game) {
        super(boardView);
        this.placeHolder = Objects.requireNonNull(placeHolder);
        this.owner = owner;
//        List<SBFFormation> formationsInHex = game.getActiveFormationsAt(placeHolder.getPosition());
//        formationCountInHex = formationsInHex.size();
//        positionInHex = MathUtility.clamp(formationsInHex.indexOf(placeHolder), 0, 3);
        getBounds();
    }

    @Override
    public Rectangle getBounds() {
        bounds = new Rectangle(0, 0, bv.hex_size.width, bv.hex_size.height);
        Point ePos = bv.getHexLocation(placeHolder.getPosition().getCoords());
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

        graph.scale(bv.scale, bv.scale);
//        graph.translate(positionInHex > 1 ? 42 : 0, (positionInHex % 2 == 1) ? 36 : 0);
//        double scaling = formationCountInHex > 1 ? 0.5 : 1;
//        graph.scale(scaling, scaling);
        graph.setColor(Color.MAGENTA);
//        if (isSelected) {
//            graph.setColor(Color.WHITE);
//        } else if (formation.isDone()) {
//            graph.setColor(Color.DARK_GRAY);
//        } else {
//            graph.setColor(Color.GREEN);
//        }
        graph.setStroke(new BasicStroke(2));
        graph.drawImage(owner.getCamouflage().getImage(), INSET + INSET / 2, INSET + INSET / 2,
                84 - 3 * INSET, 72 - 3 * INSET, null);
        graph.drawRoundRect(INSET, INSET, 84 - 2 * INSET, 72 - 2 * INSET,
                INSET / 2, INSET / 2);
        graph.setColor(owner.getColour().getColour());
        graph.fillRoundRect(INSET, INSET, 84 - 2 * INSET, 72 - 2 * INSET,
                INSET / 2, INSET / 2);
        if (placeHolder instanceof SBFSomethingOutThereUnitPlaceHolder hasType) {
            new StringDrawer(hasType.getType().toString()).at(42, 36).absoluteCenter().color(Color.DARK_GRAY).draw(graph);
        }
        graph.dispose();
    }

    @Override
    public boolean isInside(Point point) {
        return hitBox.contains(point.x, point.y);
    }

    /** Marks the entity as selected for movement etc., recoloring the label */
//    public void setSelected(boolean status) {
//        if (isSelected != status) {
//            isSelected = status;
//            prepare();
//        }
//    }

    /** Returns if the entity is marked as selected for movement etc., recoloring the label */
//    public boolean getSelected() {
//        return isSelected;
//    }

//    public SBFFormation getFormation() {
//        return formation;
}
