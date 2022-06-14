package megamek.client.ui.swing.unitDisplay;

import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.tooltip.PilotToolTip;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.common.Entity;
import megamek.common.EntityVisibilityUtils;
import megamek.common.Hex;
import megamek.common.Player;

import javax.swing.*;
import java.awt.*;

public class SummaryPanel extends JPanel {

    private UnitDisplay unitDisplay;
    private JLabel pilotInfo, unitInfo, hexInfo;

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
            //TODO add Force label
            pilotInfo.setText("<html>" + PilotToolTip.getPilotTipDetailed(entity, false) + "</html>");

            StringBuffer unitTxt = new StringBuffer("<HTML>");
            unitTxt.append(UnitToolTip.getEntityTipUnitDisplay(entity, localPlayer));
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
