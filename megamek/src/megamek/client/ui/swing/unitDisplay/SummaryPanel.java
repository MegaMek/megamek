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
        setLayout(new GridBagLayout());

        int gridy = 0;
        tooltip = new JLabel("TOOLTIP");

        tooltip = new JLabel();
        add(tooltip);
    }

//    @Override
//    public void onResize() {
//
//    }

    public void displayMech(Entity en) {
        tooltip.setText("<html>" + UnitToolTip.getEntityTipGame(en,
                unitDisplay.getClientGUI().getClient().getLocalPlayer()) + "</html>");
    }
}
