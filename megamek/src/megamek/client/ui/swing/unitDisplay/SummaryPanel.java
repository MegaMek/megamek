/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.unitDisplay;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.tooltip.PilotToolTip;
import megamek.client.ui.swing.tooltip.TipUtil;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.widget.*;
import megamek.common.*;
import megamek.common.util.fileUtils.MegaMekFile;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Displays a summary info for a unit, using the same html formatting as use by the board view map tooltips.
 * It is intended to be a tab in the UnitDisplay panel.
 */
public class SummaryPanel extends PicMap {

    private UnitDisplay unitDisplay;
    private JLabel pilotInfo, unitInfo, hexInfo;

    /**
     * @param unitDisplay the UnitDisplay UI to attach to
     */
    SummaryPanel(UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
        setBackGround();

        JComponent panel = this;
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS ));
        panel.add(Box.createRigidArea(new Dimension(0,10)));

        pilotInfo = new JLabel("<html>UnitInfo</html>", SwingConstants.LEFT );
        panel.add(pilotInfo);

        unitInfo = new JLabel("<html>HexInfo</html>", SwingConstants.LEFT );
        panel.add(unitInfo);

        hexInfo = new JLabel("<html>HexInfo</html>", SwingConstants.LEFT );
        panel.add(hexInfo);
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
    public void displayMech(Entity entity) {

        Player localPlayer = unitDisplay.getClientGUI().getClient().getLocalPlayer();

        if (entity == null) {
            pilotInfo.setIcon(null);
            pilotInfo.setText("<html>No Pilot</html>");
            unitInfo.setText("<html>No Unit</html>");
            hexInfo.setText("<html>No Hex</html>");
            return;
        }

        if (EntityVisibilityUtils.onlyDetectedBySensors(localPlayer, entity)) {
            pilotInfo.setIcon(null);
            pilotInfo.setText("<html>Sensor Return</html>");
            unitInfo.setText("<html>?</html>");
        } else {
            pilotInfo.setText( "<html> " + padLeft(PilotToolTip.getPilotTipDetailed(entity, true).toString()) + "</html>");
            unitInfo.setText("<html>" + padLeft(UnitToolTip.getEntityTipNoPilot(entity, localPlayer).toString()) + "</html>");
        }

        BoardView bv = unitDisplay.getClientGUI().getBoardView();
        Hex mhex = entity.getGame().getBoard().getHex(entity.getPosition());
        if (bv != null && mhex != null) {
            StringBuffer hexTxt = new StringBuffer("");//<HTML>");
            bv.appendTerrainTooltip(hexTxt, mhex, "#999999");
            bv.appendBuildingsTooltip(hexTxt, mhex, "#999999");
            hexInfo.setText("<html>" + padLeft(hexTxt.toString()) + "</html>");
        }

        pilotInfo.setOpaque(false);
        unitInfo.setOpaque(false);
        hexInfo.setOpaque(false);
    }

    public final static String TABLE_BEGIN = "<TABLE CELLSPACING=0 CELLPADDING=5 width=100%><TBODY><TR><TD VALIGN=TOP>";

    private String padLeft(String html) {
        int dist = (int) (GUIPreferences.getInstance().getGUIScale() * 10);
        return TABLE_BEGIN + "<td width=" + dist + "></td><td>"+html+"</td>"+"<td width=" + dist + ">" +TipUtil.TABLE_END;
    }

    @Override
    public void onResize() {

    }
}
