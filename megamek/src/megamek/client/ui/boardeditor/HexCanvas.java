/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.boardeditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.tileset.HexTileset;
import megamek.client.ui.tileset.TilesetManager;
import megamek.client.ui.util.FontHandler;
import megamek.client.ui.util.StringDrawer;
import megamek.client.ui.util.UIUtil;
import megamek.common.Hex;

/**
 * Displays the currently selected hex picture, in component form
 */
class HexCanvas extends JPanel {

    private final BoardEditorPanel boardEditorPanel;
    private final TilesetManager tm;

    private final StringDrawer invalidString = new StringDrawer(Messages.getString("BoardEditor.INVALID"))
          .at(HexTileset.HEX_W / 2, HexTileset.HEX_H / 2)
          .color(GUIPreferences.getInstance().getWarningColor())
          .outline(Color.WHITE, 1)
          .font(FontHandler.notoFont().deriveFont(Font.BOLD))
          .center();

    public HexCanvas(BoardEditorPanel boardEditorPanel) {
        this.boardEditorPanel = boardEditorPanel;
        tm = boardEditorPanel.bv.getTilesetManager();
    }

    /** Returns list or an empty list when list is null. */
    private List<Image> safeList(List<Image> list) {
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Hex curHex = boardEditorPanel.curHex;
        if (curHex != null) {
            // draw the terrain images
            g.drawImage(tm.baseFor(curHex), 0, 0, HexTileset.HEX_W, HexTileset.HEX_H, this);
            for (final Image newVar : safeList(tm.supersFor(curHex))) {
                g.drawImage(newVar, 0, 0, this);
            }
            for (final Image newVar : safeList(tm.orthographicFor(curHex))) {
                g.drawImage(newVar, 0, 0, this);
            }
            UIUtil.setHighQualityRendering(g);
            // add level and INVALID if necessary
            g.setColor(getForeground());
            g.setFont(new Font(MMConstants.FONT_SANS_SERIF, Font.PLAIN, 9));
            g.drawString(Messages.getString("BoardEditor.LEVEL") + curHex.getLevel(), 24, 70);
            List<String> errors = new ArrayList<>();
            if (!curHex.isValid(errors)) {
                invalidString.draw(g);
                String tooltip = Messages.getString("BoardEditor.invalidHex") + String.join("<BR>", errors);
                setToolTipText(tooltip);
            } else {
                setToolTipText(null);
            }
        } else {
            g.clearRect(0, 0, 72, 72);
        }
    }

    // Make the hex stubborn when resizing the frame
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(90, 90);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(90, 90);
    }
}
