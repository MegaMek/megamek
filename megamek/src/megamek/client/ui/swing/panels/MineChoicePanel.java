package megamek.client.ui.swing.panels;

import java.awt.GridBagLayout;
import java.io.Serial;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.equipment.MiscMounted;

public class MineChoicePanel extends JPanel {
    @Serial
    private static final long serialVersionUID = -1868675102440527538L;

    private final JComboBox<String> m_choice;

    private final MiscMounted m_mounted;

    public MineChoicePanel(MiscMounted miscMounted, Entity entity) {
        m_mounted = miscMounted;
        m_choice = new JComboBox<>();
        m_choice.addItem(Messages.getString("CustomMekDialog.Conventional"));
        m_choice.addItem(Messages.getString("CustomMekDialog.Vibrabomb"));
        int loc;
        loc = miscMounted.getLocation();
        String sDesc = '(' + entity.getLocationAbbr(loc) + ')';
        JLabel lLoc = new JLabel(sDesc);
        GridBagLayout gbl = new GridBagLayout();
        setLayout(gbl);
        add(lLoc, GBC.std());
        m_choice.setSelectedIndex(miscMounted.getMineType());
        add(m_choice, GBC.eol());
    }

    public void applyChoice() {
        m_mounted.setMineType(m_choice.getSelectedIndex());
    }

    @Override
    public void setEnabled(boolean enabled) {
        m_choice.setEnabled(enabled);
    }
}
