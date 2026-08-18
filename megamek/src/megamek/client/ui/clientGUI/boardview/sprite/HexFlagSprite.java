/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import java.awt.Graphics2D;

import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.StringDrawer;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.Coords;

/**
 * Marks a hex with a flag symbol. Used for hexes designated as objectives / victory hexes, drawn in the owning
 * player's color so each side can tell whose designation it is. The flag is a Material Symbols glyph, so it scales
 * cleanly with the board zoom and needs no image file.
 */
public class HexFlagSprite extends HexSprite {

    /** The Material Symbols code point for the filled flag glyph. */
    private static final String FLAG_GLYPH = "\ue153";

    private static final int TEXT_SIZE = HexTileset.HEX_H / 2;
    private static final Color OUTLINE_COLOR = new Color(40, 40, 40, 200);

    private static final int HEX_CENTER_X = HexTileset.HEX_W / 2;
    private static final int HEX_CENTER_Y = HexTileset.HEX_H / 2;

    private final StringDrawer flagWriter;

    /**
     * @param boardView the board view this sprite is displayed on
     * @param location  the hex to mark with the flag
     * @param flagColor the color of the flag, usually the owning player's color
     */
    public HexFlagSprite(BoardView boardView, Coords location, Color flagColor) {
        super(boardView, location);
        flagWriter = new StringDrawer(FLAG_GLYPH)
              .at(HEX_CENTER_X, HEX_CENTER_Y)
              .color(flagColor)
              .fontSize(TEXT_SIZE)
              .absoluteCenter().outline(OUTLINE_COLOR, 2.5f);
    }

    @Override
    public void prepare() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);
        graph.scale(bv.getScale(), bv.getScale());
        graph.setFont(FontHandler.symbolFont());
        flagWriter.draw(graph);
        graph.dispose();
    }

    /** The flag should be displayed on top of buildings and bridges in isometric view. */
    @Override
    public boolean isBehindTerrain() {
        return false;
    }
}
