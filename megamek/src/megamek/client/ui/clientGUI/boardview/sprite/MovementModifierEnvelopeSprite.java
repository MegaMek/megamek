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

import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.CUT_INSIDE;
import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.getHexBorderArea;
import static megamek.client.ui.clientGUI.boardview.HexDrawUtilities.getHexBorderAreaMid;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.util.StringDrawer;
import megamek.client.ui.util.UIUtil;
import megamek.common.Compute;
import megamek.common.CrewType;
import megamek.common.EntityMovementType;
import megamek.common.Facing;
import megamek.common.VTOL;
import megamek.common.moves.MovePath;

/**
 * Sprite for displaying information about movement modifier that can be achieved by provided MovePath. Multiple
 * MovementModifierEnvelopeSprite can be drawn on a single hex, one for each final facing.
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
    private final StringDrawer modifierText;

    /**
     * @param boardView The {@link BoardView}
     * @param movePath  The {@link MovePath} to the present hex
     */
    public MovementModifierEnvelopeSprite(BoardView boardView, MovePath movePath) {
        super(boardView, movePath.getFinalCoords());

        facing = Facing.valueOfInt(movePath.getFinalFacing());

        int movementModifier = Compute.getTargetMovementModifier(movePath.getHexesMoved(),
              movePath.isJumping(),
              movePath.getEntity() instanceof VTOL
                    || (movePath.getLastStepMovementType() == EntityMovementType.MOVE_VTOL_WALK)
                    || (movePath.getLastStepMovementType() == EntityMovementType.MOVE_VTOL_RUN)
                    || (movePath.getLastStepMovementType() == EntityMovementType.MOVE_VTOL_SPRINT),
              boardView.game).getValue();
        //Add evasion bonus for 'Mek with dual cockpit
        if (movePath.getEntity().getCrew().getCrewType().equals(CrewType.DUAL)
              && movePath.getEntity().getCrew().hasDedicatedPilot()
              && !movePath.isJumping() && movePath.getHexesMoved() > 0) {
            movementModifier++;
        }
        float hue = 0.7f - 0.15f * movementModifier;
        color = new Color(Color.HSBtoRGB(hue, 1, 1));
        String modifier = String.format("%+d", movementModifier);
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
        graph.scale(bv.getScale(), bv.getScale());

        // colored polygon at the hex border
        graph.setColor(color);
        graph.fill(getHexBorderArea(facing.getIntValue(), CUT_INSIDE, borderW, inset));

        // draw the movement modifier if it's readable
        if (fontSize * bv.getScale() > 4) {
            graph.setFont(graph.getFont().deriveFont(fontSize));
            Point2D.Double pos = getHexBorderAreaMid(facing.getIntValue(), borderW, inset);
            modifierText.at((int) pos.x, (int) pos.y).draw(graph);
        }

        graph.dispose();
    }
}
