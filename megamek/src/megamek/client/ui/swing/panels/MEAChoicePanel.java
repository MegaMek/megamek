/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
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

    private final ArrayList<MiscType> manipulators;

    private final JComboBox<String> comboChoices;
    /**
     * The BattleArmor mount location of the modular equipment adaptor.
     */
    private final int battleArmorMountLocation;
    /**
     * The manipulator currently mounted by a modular equipment adaptor.
     */
    private Mounted<?> mountedManipulator;

    public MEAChoicePanel(Entity entity, int mountLoc, Mounted<?> mounted, ArrayList<MiscType> manipulators) {
        this.entity = entity;
        this.manipulators = manipulators;

        mountedManipulator = mounted;
        battleArmorMountLocation = mountLoc;
        EquipmentType equipmentType = null;

        if (mounted != null) {
            equipmentType = mounted.getType();
        }

        comboChoices = new JComboBox<>();
        comboChoices.addItem("None");
        comboChoices.setSelectedIndex(0);
        Iterator<MiscType> it = this.manipulators.iterator();
        for (int x = 1; it.hasNext(); x++) {
            MiscType manipulator = it.next();
            String manipulatorName = manipulator.getName() + " (" + manipulator.getTonnage(this.entity) + "kg)";
            comboChoices.addItem(manipulatorName);
            if (equipmentType != null &&
                      Objects.equals(manipulator.getInternalName(), equipmentType.getInternalName())) {
                comboChoices.setSelectedIndex(x);
            }
        }

        String labelDescription = "";
        if (battleArmorMountLocation != BattleArmor.MOUNT_LOC_NONE) {
            labelDescription += " (" + BattleArmor.MOUNT_LOC_NAMES[battleArmorMountLocation] + ')';
        } else {
            labelDescription = "None";
        }

        JLabel labelLocation = new JLabel(labelDescription);
        GridBagLayout gridBagLayout = new GridBagLayout();
        setLayout(gridBagLayout);
        add(labelLocation, GBC.std());
        add(comboChoices, GBC.std());
    }

    public void applyChoice() {
        int selectedIndex = comboChoices.getSelectedIndex();

        // If there's no selection, there's nothing we can do
        if (selectedIndex == -1) {
            return;
        }

        MiscType manipulatorType = null;
        if (selectedIndex > 0 && selectedIndex <= manipulators.size()) {
            // Need to account for the "None" selection
            manipulatorType = manipulators.get(selectedIndex - 1);
        }

        int location = 0;

        if (mountedManipulator != null) {
            location = mountedManipulator.getLocation();
            entity.getEquipment().remove(mountedManipulator);

            if (mountedManipulator instanceof MiscMounted miscMounted) {
                entity.getMisc().remove(miscMounted);
            }
        }

        // Was no manipulator selected?
        if (selectedIndex == 0) {
            return;
        }

        // Add the newly mounted manipulator
        // Adjusts to use the location variable with a default of a location of 0 to account for when the
        // mountedManipulator is null at this point.
        try {
            mountedManipulator = entity.addEquipment(manipulatorType, location);
            mountedManipulator.setBaMountLoc(battleArmorMountLocation);
        } catch (LocationFullException ex) {
            // This shouldn't happen for BA...
            LOGGER.error(ex, "Location Full Exception");
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        comboChoices.setEnabled(enabled);
    }
}
