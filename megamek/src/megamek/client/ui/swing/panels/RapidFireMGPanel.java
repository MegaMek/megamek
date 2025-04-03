package megamek.client.ui.swing.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.Serial;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.Mounted;

public class RapidFireMGPanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 5261919826318225201L;

    private final Mounted<?> m_mounted;

    JCheckBox chRapid = new JCheckBox();

    public RapidFireMGPanel(Mounted<?> mounted, Entity entity) {
        m_mounted = mounted;
        int loc = mounted.getLocation();
        String sDesc = Messages.getString("CustomMekDialog.gridBagLayout", entity.getLocationAbbr(loc));
        JLabel labRapid = new JLabel(sDesc);
        GridBagLayout g = new GridBagLayout();
        setLayout(g);
        add(labRapid, GBC.std().anchor(GridBagConstraints.EAST));
        chRapid.setSelected(mounted.isRapidfire());
        add(chRapid, GBC.eol());
    }

    public void applyChoice() {
        boolean b = chRapid.isSelected();
        m_mounted.setRapidfire(b);
    }

    @Override
    public void setEnabled(boolean enabled) {
        chRapid.setEnabled(enabled);
    }
}
