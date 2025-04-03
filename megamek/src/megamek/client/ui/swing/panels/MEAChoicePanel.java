package megamek.client.ui.swing.panels;

import java.awt.GridBagLayout;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LocationFullException;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.equipment.MiscMounted;
import megamek.logging.MMLogger;

/**
 * A panel that houses a label and a combo box that allows for selecting which manipulator is mounted in a modular
 * equipment adaptor.
 *
 * @author arlith
 */
public class MEAChoicePanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 6189888202192403704L;

    private static final MMLogger LOGGER = MMLogger.create(MEAChoicePanel.class);

    private final Entity entity;

    private final ArrayList<MiscType> m_Manipulators;

    private final JComboBox<String> m_choice;

    /**
     * The manipulator currently mounted by a modular equipment adaptor.
     */
    private Mounted<?> m_ManipulatorMounted;

    /**
     * The BattleArmor mount location of the modular equipment adaptor.
     */
    private final int baMountLoc;

    public MEAChoicePanel(Entity entity, int mountLoc, Mounted<?> mounted, ArrayList<MiscType> manipulators) {
        this.entity = entity;
        m_Manipulators = manipulators;
        m_ManipulatorMounted = mounted;
        baMountLoc = mountLoc;
        EquipmentType curType = null;

        if (mounted != null) {
            curType = mounted.getType();
        }

        m_choice = new JComboBox<>();
        m_choice.addItem("None");
        m_choice.setSelectedIndex(0);
        Iterator<MiscType> it = m_Manipulators.iterator();
        for (int x = 1; it.hasNext(); x++) {
            MiscType manipulator = it.next();
            String manipulatorName = manipulator.getName() + " (" + manipulator.getTonnage(this.entity) + "kg)";
            m_choice.addItem(manipulatorName);
            if (curType != null && Objects.equals(manipulator.getInternalName(), curType.getInternalName())) {
                m_choice.setSelectedIndex(x);
            }
        }

        String sDesc = "";
        if (baMountLoc != BattleArmor.MOUNT_LOC_NONE) {
            sDesc += " (" + BattleArmor.MOUNT_LOC_NAMES[baMountLoc] + ')';
        } else {
            sDesc = "None";
        }

        JLabel lLoc = new JLabel(sDesc);
        GridBagLayout g = new GridBagLayout();
        setLayout(g);
        add(lLoc, GBC.std());
        add(m_choice, GBC.std());

    }

    public void applyChoice() {
        int n = m_choice.getSelectedIndex();

        // If there's no selection, there's nothing we can do
        if (n == -1) {
            return;
        }

        MiscType manipulatorType = null;
        if (n > 0 && n <= m_Manipulators.size()) {
            // Need to account for the "None" selection
            manipulatorType = m_Manipulators.get(n - 1);
        }

        int location = 0;

        if (m_ManipulatorMounted != null) {
            location = m_ManipulatorMounted.getLocation();
            entity.getEquipment().remove(m_ManipulatorMounted);

            if (m_ManipulatorMounted instanceof MiscMounted miscMounted) {
                entity.getMisc().remove(miscMounted);
            }
        }

        // Was no manipulator selected?
        if (n == 0) {
            return;
        }

        // Add the newly mounted manipulator
        // Adjusts to use the location variable with a default of a location of 0 to account for when the
        // m_ManipulatorMounted is null at this point.
        try {
            m_ManipulatorMounted = entity.addEquipment(manipulatorType, location);
            m_ManipulatorMounted.setBaMountLoc(baMountLoc);
        } catch (LocationFullException ex) {
            // This shouldn't happen for BA...
            LOGGER.error(ex, "");
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        m_choice.setEnabled(enabled);
    }
}
