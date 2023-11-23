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

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.tooltip.PilotToolTip;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.widget.*;
import megamek.common.*;
import megamek.common.util.fileUtils.MegaMekFile;

import javax.swing.*;
import java.awt.*;

import static megamek.client.ui.swing.tooltip.TipUtil.*;

/**
 * Displays a summary info for a unit, using the same html formatting as use by the board view map tooltips.
 * It is intended to be a tab in the UnitDisplay panel.
 */
public class SummaryPanel extends PicMap {

    private UnitDisplay unitDisplay;
    private JLabel unitInfo;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    /**
     * @param unitDisplay the UnitDisplay UI to attach to
     */
    SummaryPanel(UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
        setBackGround();

        JComponent panel = this;
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS ));
        panel.add(Box.createRigidArea(new Dimension(0,10)));

        unitInfo = new JLabel("<HTML>UnitInfo</HTML>", SwingConstants.LEFT );
        panel.add(unitInfo);
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
        String htmlStyle = "style=\"color:" + GUIP.hexColor(GUIP.getUnitToolTipFGColor()) + "; ";
        htmlStyle += "background-color:" + GUIP.hexColor(GUIP.getUnitToolTipBGColor()) + ";\"";

        if (entity == null) {
            unitInfo.setText("<HTML><BODY " + htmlStyle + ">" + padLeft("No Unit") + "</BODY></HTML>");
            return;
        }

        if (EntityVisibilityUtils.onlyDetectedBySensors(localPlayer, entity)) {
            unitInfo.setText("<HTML><BODY " + htmlStyle + ">" + padLeft( Messages.getString("BoardView1.sensorReturn")) + "</BODY></HTML>");
        } else {
            // This is html tables inside tables to maintain transparency to the bg image but
            // also allow cells do have bg colors
            StringBuffer hexTxt = new StringBuffer("");
            hexTxt.append(PilotToolTip.getPilotTipDetailed(entity, true));
            hexTxt.append(UnitToolTip.getEntityTipUnitDisplay(entity, localPlayer));

            String col = "";
            String row = "";

            BoardView bv = unitDisplay.getClientGUI().getBoardView();
            Hex mhex = entity.getGame().getBoard().getHex(entity.getPosition());
            if (bv != null && mhex != null) {
                StringBuffer sb = new StringBuffer();
                bv.appendTerrainTooltip(sb, mhex);
                col = "<TD>" + sb + "</TD>";
                row = "<TR>" + col + "</TR>";
                hexTxt.append("<TABLE BORDER=0 BGCOLOR=" + GUIP.hexColor(GUIP.getUnitToolTipTerrainBGColor()) + " width=100%>" + row + "</TABLE>");
                bv.appendBuildingsTooltip(hexTxt, mhex);
            }

            String t = PilotToolTip.getCrewAdvs(entity, true).toString();
            col = "<TD>" + t + "</TD>";
            row = "<TR>" + col + "</TR>";
            hexTxt.append("<TABLE width=100%>" + row + "</TABLE>");

            unitInfo.setText("<HTML><BODY " + htmlStyle + ">" + padLeft(hexTxt.toString()) + "</BODY></HTML>");
        }
        unitInfo.setOpaque(false);
    }

    private String padLeft(String html) {
        int dist = (int) (GUIPreferences.getInstance().getGUIScale() * 5);
        String col = "";
        String row = "";
        String tbody = "";
        String table = "";
        col = "<TD>" + html + "</TD>";
        row = "<TR>" + col + "</TR>";
        tbody = "<TBODY>" + row + "</TBODY>";
        table = "<TABLE CELLSPACING=" + dist + " CELLPADDING=" + dist + " WIDTH=100%>" + tbody + "</TABLE>";
        return table;
    }

    @Override
    public void onResize() {

    }
}
