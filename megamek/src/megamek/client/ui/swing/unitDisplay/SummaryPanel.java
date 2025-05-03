/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.unitDisplay;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.tooltip.HexTooltip;
import megamek.client.ui.swing.tooltip.PilotToolTip;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.BackGroundDrawer;
import megamek.client.ui.swing.widget.PMUtil;
import megamek.client.ui.swing.widget.PicMap;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.client.ui.swing.widget.UnitDisplaySkinSpecification;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.EntityVisibilityUtils;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Displays a summary info for a unit, using the same html formatting as use by the board view map tooltips.
 * It is intended to be a tab in the UnitDisplay panel.
 */
public class SummaryPanel extends PicMap {

    private final UnitDisplay unitDisplay;
    private final JLabel unitInfo;
    private final JPanel contentPanel;

    private static final GUIPreferences GUI_PREFERENCES = GUIPreferences.getInstance();

    /**
     * @param unitDisplay the UnitDisplay UI to attach to
     */
    SummaryPanel(UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
        setBackGround();

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Add spacing at top
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Create the unit info label
        unitInfo = new JLabel("<HTML>UnitInfo</HTML>", SwingConstants.LEFT);
        unitInfo.setOpaque(false);
        contentPanel.add(unitInfo);

        // Use BorderLayout to ensure the content panel is properly sized
        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.NORTH);

        // Add a "push" component to allow proper scrolling
        add(Box.createVerticalGlue(), BorderLayout.CENTER);
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();

        Image tile = unitDisplay.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getBackgroundTile()).toString());
        PMUtil.setImage(tile, unitDisplay);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_TOP;
        tile = unitDisplay.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getTopLine()).toString());
        PMUtil.setImage(tile, unitDisplay);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_BOTTOM;
        tile = unitDisplay.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getBottomLine()).toString());
        PMUtil.setImage(tile, unitDisplay);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
        tile = unitDisplay.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getLeftLine()).toString());
        PMUtil.setImage(tile, unitDisplay);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_RIGHT;
        tile = unitDisplay.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getRightLine()).toString());
        PMUtil.setImage(tile, unitDisplay);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_LEFT;
        tile = unitDisplay.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getTopLeftCorner()).toString());
        PMUtil.setImage(tile, unitDisplay);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_LEFT;
        tile = unitDisplay.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, unitDisplay);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = unitDisplay.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getTopRightCorner()).toString());
        PMUtil.setImage(tile, unitDisplay);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = unitDisplay.getToolkit().getImage(new MegaMekFile(Configuration.widgetsDir(),
                udSpec.getBottomRightCorner()).toString());
        PMUtil.setImage(tile, unitDisplay);
        addBgDrawer(new BackGroundDrawer(tile, b));
    }

    /**
     * @param entity The Entity to display info for
     */
    public void displayMek(Entity entity) {
        Player localPlayer = unitDisplay.getClientGUI().getClient().getLocalPlayer();
        String txt = "";

        if (entity == null) {
            txt = padLeft("No Unit");
        } else if (EntityVisibilityUtils.onlyDetectedBySensors(localPlayer, entity)) {
            txt = padLeft(Messages.getString("BoardView1.sensorReturn"));
        } else {
            String col;
            String row;
            String rows = "";
            // This is html tables inside tables to maintain transparency to the bg image but
            // also allow cells do have bg colors
            String pilotTip = PilotToolTip.getPilotTipDetailed(entity, true).toString();
            col = UIUtil.tag("TD", "", pilotTip);
            row = UIUtil.tag("TR", "", col);
            rows += row;

            String unitTip = UnitToolTip.getEntityTipUnitDisplay(entity, localPlayer).toString();
            col = UIUtil.tag("TD", "", unitTip);
            row = UIUtil.tag("TR", "", col);
            rows += row;

            Hex mhex = entity.getGame().getHex(entity.getBoardLocation());
            if (mhex != null) {
                String terrainTip = HexTooltip.getTerrainTip(mhex, entity.getBoardId(), entity.getGame());
                String attr = String.format("FACE=Dialog BGCOLOR=%s", UIUtil.toColorHexString(GUIP.getUnitToolTipTerrainBGColor()));
                col = UIUtil.tag("TD", attr, terrainTip);
                row = UIUtil.tag("TR", "", col);
                rows += row;

                String hexTip = HexTooltip.getHexTip(mhex, unitDisplay.getClientGUI().getClient(), entity.getBoardId());
                if (!hexTip.isEmpty()) {
                    attr = String.format("FACE=Dialog BGCOLOR=%s", UIUtil.toColorHexString(GUI_PREFERENCES.getUnitToolTipTerrainBGColor()));
                    col = UIUtil.tag("TD", attr, hexTip);
                    row = UIUtil.tag("TR", "", col);
                    rows += row;
                }
            }

            String edgeTip = PilotToolTip.getCrewAdvs(entity, true).toString();
            col = UIUtil.tag("TD", "", edgeTip);
            row = UIUtil.tag("TR", "", col);
            rows += row;

            String table = UIUtil.tag("TABLE", "CELLSPACING=0 CELLPADDING=0 width=100%", rows);
            txt = padLeft(table);
        }

        unitInfo.setText(UnitToolTip.wrapWithHTML(txt));

        // Force a revalidation to ensure proper sizing
        contentPanel.revalidate();
        contentPanel.setSize(contentPanel.getPreferredSize());
        // Request focus to ensure scrollbars update
        SwingUtilities.invokeLater(() -> {
            revalidate();
            getParent().revalidate();
            if (getParent().getParent() instanceof JScrollPane) {
                getParent().getParent().revalidate();
            }
        });

    }

    private String padLeft(String html) {
        int dist = 4;
        String col = UIUtil.tag("TD", "", html);
        String row = UIUtil.tag("TR", "", col);
        String attr = String.format("CELLSPACING=0 CELLPADDING=%s width=100%%", dist);
        return UIUtil.tag("TABLE", attr, row);
    }

    @Override
    public void onResize() {
        revalidate();
    }

    @Override
    public Dimension getPreferredSize() {
        // Make sure the preferred size is at least as tall as the content
        Dimension dimension = super.getPreferredSize();
        if (contentPanel != null) {
            Dimension contentSize = contentPanel.getPreferredSize();
            dimension.height = Math.max(dimension.height, contentSize.height + 20); // Add padding
        }
        return dimension;
    }
}
