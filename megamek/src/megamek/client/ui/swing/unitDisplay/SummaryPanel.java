package megamek.client.ui.swing.unitDisplay;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.widget.PicMap;
import megamek.common.Entity;
import megamek.common.Game;

import javax.swing.*;
import java.awt.*;

public class SummaryPanel extends JPanel {

    private UnitDisplay unitDisplay;
    private JLabel tooltip;

    SummaryPanel(UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
        setLayout(new BorderLayout());
        tooltip = new JLabel("<html>TOOLTIP</html>", SwingConstants.LEFT );
        add(tooltip, BorderLayout.NORTH);
    }

    public void displayMech(Entity en) {
        tooltip.setText("<html>" + UnitToolTip.getEntityTipGame(en,
                unitDisplay.getClientGUI().getClient().getLocalPlayer()) + "</html>");
    }
}
