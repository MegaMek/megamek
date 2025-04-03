package megamek.client.ui.swing.panels;

import java.awt.GridBagLayout;
import java.awt.event.ItemListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.codeUtilities.MathUtility;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.ProtoMek;
import megamek.common.equipment.AmmoMounted;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

public class MunitionChoicePanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 3401106035583965326L;

    private final List<AmmoType> m_vTypes;

    private final JComboBox<AmmoType> m_choice;

    private final JComboBox<String> m_num_shots;
    private final ItemListener numShotsListener;

    boolean numShotsChanged = false;

    private final GameOptions m_gameOptions;

    private final AmmoMounted m_mounted;

    JLabel labDump = new JLabel(Messages.getString("CustomMekDialog.labDump"));

    JCheckBox chDump = new JCheckBox();

    JLabel labHotLoad = new JLabel(Messages.getString("CustomMekDialog.switchToHotLoading"));

    JCheckBox chHotLoad = new JCheckBox();

    public MunitionChoicePanel(AmmoMounted ammoMounted, ArrayList<AmmoType> vTypes,
          List<WeaponAmmoChoicePanel> weaponAmmoChoicePanels, Entity entity, ClientGUI clientGUI) {
        m_vTypes = vTypes;
        m_mounted = ammoMounted;
        m_gameOptions = clientGUI.getClient().getGame().getOptions();

        AmmoType curType = ammoMounted.getType();
        m_choice = new JComboBox<>();
        Iterator<AmmoType> e = m_vTypes.iterator();
        for (int x = 0; e.hasNext(); x++) {
            AmmoType at = e.next();
            m_choice.addItem(at);
            if (at.equals(curType)) {
                m_choice.setSelectedIndex(x);
            }
        }

        numShotsListener = evt -> numShotsChanged = true;
        m_num_shots = new JComboBox<>();
        int shotsPerTon = curType.getShots();
        // BattleArmor always have a certain number of shots per slot
        int stepSize = 1;
        // ProtoMeks and BattleArmor are limited to the number of shots allocated in construction
        if ((entity instanceof BattleArmor) || (entity instanceof ProtoMek)) {
            shotsPerTon = ammoMounted.getOriginalShots();
            // BA tube artillery always comes in pairs
            if (curType.getAmmoType() == AmmoType.T_BA_TUBE) {
                stepSize = 2;
            }
        }
        for (int i = 0; i <= shotsPerTon; i += stepSize) {
            m_num_shots.addItem(String.valueOf(i));
        }
        m_num_shots.setSelectedItem(m_mounted.getBaseShotsLeft());
        m_num_shots.addItemListener(numShotsListener);

        m_choice.addItemListener(evt -> {
            m_num_shots.removeItemListener(numShotsListener);

            int currShots = 0;

            if (m_num_shots.getSelectedItem() instanceof String value) {
                currShots = MathUtility.parseInt(value, currShots);
            }

            m_num_shots.removeAllItems();
            int numberOfShotsPerTon = m_vTypes.get(m_choice.getSelectedIndex()).getShots();

            // ProtoMeks are limited to number of shots added during construction
            if ((entity instanceof BattleArmor) || (entity instanceof ProtoMek)) {
                numberOfShotsPerTon = ammoMounted.getOriginalShots();
            }
            for (int i = 0; i <= numberOfShotsPerTon; i++) {
                m_num_shots.addItem(String.valueOf(i));
            }
            // If the shots selection was changed, try to set that value, unless it's too
            // large
            if (numShotsChanged && currShots <= numberOfShotsPerTon) {
                m_num_shots.setSelectedItem(currShots);
            } else {
                m_num_shots.setSelectedItem(numberOfShotsPerTon);
            }

            for (WeaponAmmoChoicePanel weaponAmmoChoicePanel : weaponAmmoChoicePanels) {
                weaponAmmoChoicePanel.refreshAmmoBinName(m_mounted, m_vTypes.get(m_choice.getSelectedIndex()));
            }

            m_num_shots.addItemListener(numShotsListener);
        });

        int loc = ammoMounted.getLocation();
        boolean isOneShot = false;
        if (loc == Entity.LOC_NONE) {
            // one shot weapons don't have a location of their own some weapons (e.g. fusillade) use the one-shot
            // mechanic but have an extra reload which is chained to the first
            Mounted<?> linkedBy = ammoMounted.getLinkedBy();
            while (linkedBy.getLinkedBy() != null) {
                linkedBy = linkedBy.getLinkedBy();
            }
            loc = linkedBy.getLocation();
            isOneShot = linkedBy.isOneShot();
        } else {
            loc = ammoMounted.getLocation();
        }
        m_num_shots.setVisible(!isOneShot);
        String sDesc = '(' + entity.getLocationAbbr(loc) + ')';
        JLabel lLoc = new JLabel(sDesc);
        GridBagLayout g = new GridBagLayout();
        setLayout(g);
        add(lLoc, GBC.std());
        add(m_choice, GBC.std());
        add(m_num_shots, GBC.eol());
        chHotLoad.setSelected(m_mounted.isHotLoaded());
        if (m_gameOptions.booleanOption(OptionsConstants.BASE_LOBBY_AMMO_DUMP)) {
            add(labDump, GBC.std());
            add(chDump, GBC.eol());
        }

        if (m_gameOptions.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD) &&
                  curType.hasFlag(AmmoType.F_HOTLOAD)) {
            add(labHotLoad, GBC.std());
            add(chHotLoad, GBC.eol());
        }
    }

    public void applyChoice() {
        int n = m_choice.getSelectedIndex();
        // If there's no selection, there's nothing we can do
        if (n == -1) {
            return;
        }
        AmmoType at = m_vTypes.get(n);
        m_mounted.changeAmmoType(at);

        // set # shots only for non-one shot weapons
        if (m_mounted.getLocation() != Entity.LOC_NONE && m_num_shots.getSelectedItem() instanceof String value) {
            m_mounted.setShotsLeft(MathUtility.parseInt(value, 0));
        }

        if (chDump.isSelected()) {
            m_mounted.setShotsLeft(0);
        }

        if (m_gameOptions.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD)) {
            if (chHotLoad.isSelected() != m_mounted.isHotLoaded()) {
                m_mounted.setHotLoad(chHotLoad.isSelected());
                // Set the mode too, so vehicles can switch back
                int numModes = m_mounted.getModesCount();
                for (int m = 0; m < numModes; m++) {
                    if (m_mounted.getType().getMode(m).getName().equals("HotLoad")) {
                        m_mounted.setMode(m);
                    }
                }
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        m_choice.setEnabled(enabled);
    }

    /**
     * Get the number of shots in the mount.
     *
     * @return the <code>int</code> number of shots in the mount.
     *
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    int getShotsLeft() {
        return m_mounted.getBaseShotsLeft();
    }

    /**
     * Set the number of shots in the mount.
     *
     * @param shots the <code>int</code> number of shots for the mount.
     *
     * @deprecated no indicated uses.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    void setShotsLeft(int shots) {
        m_mounted.setShotsLeft(shots);
    }
}
