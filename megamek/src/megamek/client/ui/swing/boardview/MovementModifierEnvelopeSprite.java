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

import static megamek.client.ui.swing.boardview.HexDrawUtilities.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import megamek.client.ui.swing.util.StringDrawer;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Compute;
import megamek.common.CrewType;
import megamek.common.Facing;
import megamek.common.MovePath;
import megamek.common.VTOL;

/**
 * Sprite for displaying information about movement modifier that can be
 * achieved by provided MovePath. Multiple MovementModifierEnvelopeSprite can be
 * drawn on a single hex, one for each final facing.
 * 
 * @author Saginatio
 */
public class MovementModifierEnvelopeSprite extends HexSprite {
    
    private static final Color fontColor = Color.BLACK;
    private static final float fontSize = 9;
    private static final double borderW = 15;
    private static final double inset = 1;

    private final Color color;
    private final Facing facing;
    private final String modifier;
    private final StringDrawer modifierText;

    /**
     * @param boardView The BoardView
     * @param mp The movepath to the present hex
     */
    public MovementModifierEnvelopeSprite(BoardView boardView, MovePath mp) {
        super(boardView, mp.getFinalCoords());

        facing = Facing.valueOfInt(mp.getFinalFacing());
        
        int modi = Compute.getTargetMovementModifier(mp.getHexesMoved(),
                mp.isJumping(),
                mp.getEntity() instanceof VTOL,
                boardView.game).getValue();
        //Add evasion bonus for 'Mech with dual cockpit
        if (mp.getEntity().getCrew().getCrewType().equals(CrewType.DUAL)
                && mp.getEntity().getCrew().hasDedicatedPilot()
                && !mp.isJumping() && mp.getHexesMoved() > 0) {
            modi++;
        }
        float hue = 0.7f - 0.15f * modi;
        color = new Color(Color.HSBtoRGB(hue, 1, 1));
        modifier = String.format("%+d", modi);
        modifierText = new StringDrawer(modifier).center().color(fontColor);
    }

    @Override
    public void prepare() {
        // adjust bounds (image size) to board zoom
        updateBounds();
        
        // create image for buffer
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);

        // scale the following draws according to board zoom
        graph.scale(bv.scale, bv.scale);

        // colored polygon at the hex border
        graph.setColor(color);
        graph.fill(getHexBorderArea(facing.getIntValue(), CUT_INSIDE, borderW, inset));

        // draw the movement modifier if it's readable
        if (fontSize * bv.scale > 4) {
            graph.setFont(graph.getFont().deriveFont(fontSize));
            Point2D.Double pos = getHexBorderAreaMid(facing.getIntValue(), borderW, inset);
            modifierText.at((int) pos.x, (int) pos.y).draw(graph);
        }

        graph.dispose();
    }
}
