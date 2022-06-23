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

import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.tooltip.PilotToolTip;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.common.Entity;
import megamek.common.EntityVisibilityUtils;
import megamek.common.Hex;
import megamek.common.Player;

import javax.swing.*;

/**
 * Displays a summary info for a unit, using the same html formatting as use by the board view map tooltips.
 * It is intended to be a tab in the UnitDisplay panel.
 */
public class SummaryPanel extends JPanel {

    private UnitDisplay unitDisplay;
    private JLabel pilotInfo, unitInfo, hexInfo;

    /**
     * @param unitDisplay the UnitDisplay UI to attach to
     */
    SummaryPanel(UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;

        JPanel panel = this;
        panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS ));

        pilotInfo = new JLabel("<html>UnitInfo</html>", SwingConstants.LEFT );
        panel.add(pilotInfo);

        unitInfo = new JLabel("<html>HexInfo</html>", SwingConstants.LEFT );
        panel.add(unitInfo);

        hexInfo = new JLabel("<html>HexInfo</html>", SwingConstants.LEFT );
        panel.add(hexInfo);
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
            int slot = 0;
            pilotInfo.setIcon(new ImageIcon(entity.getCrew().getPortrait(slot).getImage()));
            pilotInfo.setText("<html>" + PilotToolTip.getPilotTipDetailed(entity, false) + "</html>");

            StringBuffer unitTxt = new StringBuffer("<HTML>");
            unitTxt.append(UnitToolTip.getEntityTipNoPilot(entity, localPlayer));
            unitTxt.append("</HTML>");
            unitInfo.setText(unitTxt.toString());
        }

        BoardView bv = unitDisplay.getClientGUI().getBoardView();
        Hex mhex = entity.getGame().getBoard().getHex(entity.getPosition());
        if (bv != null && mhex != null) {
            StringBuffer hexTxt = new StringBuffer("<HTML>");
            bv.appendTerrainTooltip(hexTxt, mhex, "#999999");
            bv.appendBuildingsTooltip(hexTxt, mhex, "#999999");
            hexTxt.append("</HTML>");
            hexInfo.setText(hexTxt.toString());
        }
    }
}
