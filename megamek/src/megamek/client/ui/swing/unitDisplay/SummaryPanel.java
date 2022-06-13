package megamek.client.ui.swing.unitDisplay;

import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.tooltip.PilotToolTip;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.common.Entity;
import megamek.common.Hex;

import javax.swing.*;
import java.awt.*;

public class SummaryPanel extends JPanel {

    private UnitDisplay unitDisplay;
    private JLabel unitInfo, pilotInfo;

    SummaryPanel(UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
//        this.setLayout(new BorderLayout());
//        JPanel panel = new JPanel();
//        this.add(panel, BorderLayout.PAGE_START);
        JPanel panel = this;

        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        pilotInfo = new JLabel("<html>UnitInfo</html>", SwingConstants.CENTER );
        panel.add(pilotInfo, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        unitInfo = new JLabel("<html>HexInfo</html>", SwingConstants.LEFT );
        panel.add(unitInfo, c);
    }

    public void displayMech(Entity entity) {
        Hex mhex = entity.getGame().getBoard().getHex(entity.getPosition());
        BoardView bv = unitDisplay.getClientGUI().getBoardView();
        StringBuffer txt = new StringBuffer("<HTML>");

        int slot = 0;
        pilotInfo.setIcon(new ImageIcon(entity.getCrew().getPortrait(slot).getImage()));
        pilotInfo.setText("<html>"+PilotToolTip.getPilotTipDetailed(entity).toString()+"</html>");
        if (bv != null) {
            if (entity != null) {
                bv.appendEntityTooltip(txt, entity);
            } else {
                txt.append("<br>No unit");
            }

            if (mhex != null) {
                bv.appendTerrainTooltip(txt, mhex);
                bv.appendBuildingsTooltip(txt, mhex);
            } else {
                txt.append("<br>No hex");
            }
        } else {
            txt.append(UnitToolTip.getEntityTipGame(entity,
                    unitDisplay.getClientGUI().getClient().getLocalPlayer()));
        }
        txt.append("</HTML>");
        unitInfo.setText(txt.toString());

    }


}
