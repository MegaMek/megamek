/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
import java.awt.Font;
import java.awt.Graphics2D;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.StringDrawer;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Coords;

/**
 *  Represents structure CF warnings for entities during deployment
 *  and movement phase that will collapse if the entity lands-on 
 *  or is deployed on that structure.
 * 
 *  From TW: If a units tonnage exceeds the CF of a building
 *  or bridge, it will collapse.  (Or the sum of tonnage of stacked
 *  units if multiple units occupy the hex)
 */
public class ConstructionWarningSprite extends HexSprite {

	private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    private static final int TEXT_SIZE = 40;
    private static final Color TEXT_COLOR = new Color(255, 40, 40, 230);
    private static final Color OUTLINE_COLOR = new Color(40, 40,40,200);
    
    private static final int HEX_CENTER_X = BoardView.HEX_W / 2;
    private static final int HEX_CENTER_Y = BoardView.HEX_H / 2;

    //Draw a special character 'warning sign'.
    private final StringDrawer xWriter = new StringDrawer("(\u26A0)")
    		.at(HEX_CENTER_X, HEX_CENTER_Y)
    		.color(TEXT_COLOR)
            .fontSize(TEXT_SIZE)
            .center().outline(OUTLINE_COLOR, 1.5f);

	/**
	 * @param boardView1 - parent BoardView object this sprite will be displayed on.
	 * @param loc - Hex location coordinates of building or bridge where warning will be visible.
	 */
	public ConstructionWarningSprite(BoardView boardView1, Coords loc) {
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
        graph.scale(bv.scale, bv.scale);
        
        fontSetup(graph);

        return graph;
	}
	
	/*
	 * Sets the font name, style, and size from configured default parameters.
	 */
	private void fontSetup(Graphics2D graph) {
        String fontName = GUIP.getMoveFontType();
        int fontStyle = GUIP.getMoveFontStyle();
        graph.setFont(new Font(fontName, fontStyle, TEXT_SIZE));		
	}
}