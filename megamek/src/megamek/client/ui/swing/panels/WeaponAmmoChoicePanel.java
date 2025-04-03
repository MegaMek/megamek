package megamek.client.ui.swing.panels;

import java.awt.GridBagLayout;
import java.io.Serial;
import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.GBC;
import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * A panel representing the option to choose a particular ammo bin for an individual weapon.
 *
 * @author NickAragua
 */
public class WeaponAmmoChoicePanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 604670659251519188L;
    // the weapon being displayed in this row
    private final WeaponMounted m_mounted;
    private final ArrayList<AmmoMounted> matchingAmmoBins;

    private final JComboBox<String> ammoBins;

    private final Entity entity;

    /**
     * Constructor
     *
     * @param weapon The mounted weapon. Assumes that the weapon uses ammo.
     */
    public WeaponAmmoChoicePanel(WeaponMounted weapon, Entity entity) {
        this.entity = entity;
        m_mounted = weapon;

        this.setLayout(new GridBagLayout());

        ammoBins = new JComboBox<>();
        matchingAmmoBins = new ArrayList<>();

        if (m_mounted.isOneShot() || (entity.isSupportVehicle() && (m_mounted.getType() instanceof InfantryWeapon))) {
            // One-shot weapons can only access their own bin
            matchingAmmoBins.add(m_mounted.getLinkedAmmo());
            // Fusillade and some small SV weapons are treated like one-shot weapons but may have a second
            // munition type available.
            if ((m_mounted.getLinked().getLinked() != null) &&
                      (((AmmoType) m_mounted.getLinked().getType()).getMunitionType() !=
                             (((AmmoType) m_mounted.getLinked().getLinked().getType()).getMunitionType()))) {
                matchingAmmoBins.add((AmmoMounted) m_mounted.getLinked().getLinked());
            }
        } else {
            for (AmmoMounted ammoBin : weapon.getEntity().getAmmo()) {
                if ((ammoBin.getLocation() != Entity.LOC_NONE) && AmmoType.canSwitchToAmmo(weapon, ammoBin.getType())) {
                    matchingAmmoBins.add(ammoBin);
                }
            }
        }

        // don't bother displaying the row if there's no ammo to be swapped
        if (matchingAmmoBins.isEmpty()) {
            return;
        }

        JLabel weaponName = new JLabel();
        weaponName.setText("(" + weapon.getEntity().getLocationAbbr(weapon.getLocation()) + ") " + weapon.getName());
        add(weaponName, GBC.std());

        add(ammoBins, GBC.eol());
        refreshAmmoBinNames();
    }

    /**
     * Worker function that refreshes the combo box with "up-to-date" ammo names.
     */
    public void refreshAmmoBinNames() {
        int selectedIndex = ammoBins.getSelectedIndex();
        ammoBins.removeAllItems();

        int currentIndex = 0;
        for (Mounted<?> ammoBin : matchingAmmoBins) {
            ammoBins.addItem("(" +
                                   ammoBin.getEntity().getLocationAbbr(ammoBin.getLocation()) +
                                   ") " +
                                   ammoBin.getName());
            if (m_mounted.getLinked() == ammoBin) {
                selectedIndex = currentIndex;
            }

            currentIndex++;
        }

        if (selectedIndex >= 0) {
            ammoBins.setSelectedIndex(selectedIndex);
        }

        validate();
    }

    /**
     * Refreshes a single item in the ammo type combo box to display the correct ammo type name. Because the underlying
     * ammo bin hasn't been updated yet, we carry out the name swap "in-place".
     *
     * @param ammoBin          The ammo bin whose ammo type has probably changed.
     * @param selectedAmmoType The new ammo type.
     */
    public void refreshAmmoBinName(Mounted<?> ammoBin, AmmoType selectedAmmoType) {
        int index;
        boolean matchFound = false;

        for (index = 0; index < matchingAmmoBins.size(); index++) {
            if (matchingAmmoBins.get(index) == ammoBin) {
                matchFound = true;
                break;
            }
        }

        if (matchFound) {
            int currentBinIndex = ammoBins.getSelectedIndex();

            ammoBins.removeItemAt(index);
            ammoBins.insertItemAt("(" +
                                        ammoBin.getEntity().getLocationAbbr(ammoBin.getLocation()) +
                                        ") " +
                                        selectedAmmoType.getName(), index);

            if (currentBinIndex == index) {
                ammoBins.setSelectedIndex(index);
            }

            validate();
        }
    }

    /**
     * Common functionality that applies the panel's current ammo bin choice to the panel's weapon.
     */
    public void applyChoice() {
        int selectedIndex = ammoBins.getSelectedIndex();
        if ((selectedIndex >= 0) && (selectedIndex < matchingAmmoBins.size())) {
            entity.loadWeapon(m_mounted, matchingAmmoBins.get(selectedIndex));
        }
    }
}
