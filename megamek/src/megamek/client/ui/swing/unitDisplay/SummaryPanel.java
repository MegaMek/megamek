package megamek.client.ui.swing.unitDisplay;

import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.common.Entity;
import megamek.common.Hex;
import megamek.common.Terrains;

import javax.swing.*;
import java.awt.*;

public class SummaryPanel extends JPanel {

    private UnitDisplay unitDisplay;
    private JLabel hexInfo, unitInfo;

    SummaryPanel(UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
        setLayout(new BorderLayout());
        hexInfo = new JLabel("<html>HexInfo</html>", SwingConstants.LEFT );
        add(hexInfo, BorderLayout.CENTER);

//        unitInfo = new JLabel("<html>UnitInfo</html>", SwingConstants.LEFT );
//        add(unitInfo, BorderLayout.CENTER);
    }

    public void displayMech(Entity entity) {
        Hex mhex = entity.getGame().getBoard().getHex(entity.getPosition());
        BoardView bv = unitDisplay.getClientGUI().getBoardView();
        StringBuffer txt = new StringBuffer("<HTML>");
        txt.append("<TABLE BORDER=0 BGCOLOR=#DDFFDD width=100%><TR><TD><FONT color=\"black\">");
        if (bv != null && mhex != null) {
            bv.appendTerrainTooltip(txt, mhex);
        } else {
            txt.append("No hex");
        }
        txt.append("</FONT></TD></TR></TABLE>");

        if (entity != null) {
            txt.append(UnitToolTip.getEntityTipGame(entity,
                    unitDisplay.getClientGUI().getClient().getLocalPlayer()));
        } else {
            txt.append("No unit");
        }
        txt.append("</HTML>");
        hexInfo.setText(txt.toString());

    }
}
