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
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LocationFullException;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import megamek.common.equipment.WeaponMounted;
import megamek.logging.MMLogger;

/**
 * A panel that houses a label and a combo box that allows for selecting which anti-personnel weapon is mounted in an AP
 * mount.
 *
 * @author arlith
 */
public class APWeaponChoicePanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 6189888202192403704L;

    private static final MMLogger LOGGER = MMLogger.create(APWeaponChoicePanel.class);

    private final Entity entity;

    private final ArrayList<WeaponType> m_APWeapons;

    private final JComboBox<String> m_choice;

    private final Mounted<?> m_APmounted;

    public APWeaponChoicePanel(Entity entity, Mounted<?> mounted, ArrayList<WeaponType> weapons) {
        this.entity = entity;
        m_APWeapons = weapons;
        m_APmounted = mounted;
        EquipmentType curType = null;
        if ((mounted != null) && (mounted.getLinked() != null)) {
            curType = mounted.getLinked().getType();
        }
        m_choice = new JComboBox<>();
        m_choice.addItem("None");
        m_choice.setSelectedIndex(0);
        Iterator<WeaponType> it = m_APWeapons.iterator();
        for (int x = 1; it.hasNext(); x++) {
            WeaponType weaponType = it.next();
            m_choice.addItem(weaponType.getName());
            if ((curType != null) && Objects.equals(weaponType.getInternalName(), curType.getInternalName())) {
                m_choice.setSelectedIndex(x);
            }
        }

        String sDesc = "";
        if ((mounted != null) && (mounted.getBaMountLoc() != BattleArmor.MOUNT_LOC_NONE)) {
            sDesc += " (" + BattleArmor.MOUNT_LOC_NAMES[mounted.getBaMountLoc()] + ')';
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
        WeaponType apType = null;
        if ((n > 0) && (n <= m_APWeapons.size())) {
            // Need to account for the "None" selection
            apType = m_APWeapons.get(n - 1);
        }

        // Remove any currently mounted AP weapon
        if (m_APmounted.getLinked() != null && m_APmounted.getLinked().getType() != apType) {
            Mounted<?> apWeapon = m_APmounted.getLinked();
            entity.getEquipment().remove(apWeapon);

            if (apWeapon instanceof WeaponMounted weaponMounted) {
                entity.getWeaponList().remove(weaponMounted);
                entity.getTotalWeaponList().remove(weaponMounted);
            }

            // We need to make sure that the weapon has been removed from the critical slots, otherwise it can cause
            // issues
            for (int loc = 0; loc < entity.locations(); loc++) {
                for (int c = 0; c < entity.getNumberOfCriticals(loc); c++) {
                    CriticalSlot criticalSlot = entity.getCritical(loc, c);
                    if (criticalSlot != null &&
                              criticalSlot.getMount() != null &&
                              criticalSlot.getMount().equals(apWeapon)) {
                        entity.setCritical(loc, c, null);
                    }
                }
            }
        }

        // Did the selection not change, or no weapon was selected
        if ((m_APmounted.getLinked() != null && m_APmounted.getLinked().getType() == apType) || n == 0) {
            return;
        }

        // Add the newly mounted weapon
        try {
            Mounted<?> newWeapon = entity.addEquipment(apType, m_APmounted.getLocation());
            m_APmounted.setLinked(newWeapon);
            newWeapon.setLinked(m_APmounted);
            newWeapon.setAPMMounted(true);
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
