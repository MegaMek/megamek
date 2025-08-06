/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.Coords;

/**
 * Represents structure CF warnings for entities during deployment and movement phase that will collapse if the entity
 * lands-on or is deployed on that structure.
 * <p>
 * From TW: If a units tonnage exceeds the CF of a building or bridge, it will collapse.  (Or the sum of tonnage of
 * stacked units if multiple units occupy the hex)
 */
public class CollapseWarningSprite extends HexSprite {

    private static final int TEXT_SIZE = HexTileset.HEX_H / 2;
    private static final Color TEXT_COLOR = new Color(255, 255, 40, 128);
    private static final Color OUTLINE_COLOR = new Color(40, 40, 40, 200);

    private static final int HEX_CENTER_X = HexTileset.HEX_W / 2;
    private static final int HEX_CENTER_Y = HexTileset.HEX_H / 2;

    // Draw a special character 'warning sign'.
    private final StringDrawer xWriter = new StringDrawer("\ue160")
          .at(HEX_CENTER_X, HEX_CENTER_Y)
          .color(TEXT_COLOR)
          .fontSize(TEXT_SIZE)
          .absoluteCenter().outline(OUTLINE_COLOR, 2.5f);

    /**
     * @param boardView1 - parent BoardView object this sprite will be displayed on.
     * @param loc        - Hex location coordinates of building or bridge where warning will be visible.
     */
    public CollapseWarningSprite(BoardView boardView1, Coords loc) {
        super(boardView1, loc);
    }

    @Override
    public void prepare() {
        Graphics2D graph = spriteSetup();
        xWriter.draw(graph);
        graph.dispose();
    }

    /*
     * Standard Hex Sprite 2D Graphics setup.  Creates the context, base hex image
     * settings, scale, and fonts.
     */
    private Graphics2D spriteSetup() {
        updateBounds();
        image = createNewHexImage();
        Graphics2D graph = (Graphics2D) image.getGraphics();
        UIUtil.setHighQualityRendering(graph);
        graph.scale(bv.getScale(), bv.getScale());
        graph.setFont(FontHandler.symbolFont());
        return graph;
    }

    /*
     * The Collapse Warning sprite should be displayed on top of bridges and buildings in
     * isometric view.
     */
    @Override
    public boolean isBehindTerrain() {
        return false;
    }
}
