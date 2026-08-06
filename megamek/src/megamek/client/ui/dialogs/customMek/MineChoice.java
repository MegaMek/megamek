/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.dialogs.customMek;

import java.awt.Component;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import megamek.client.ui.GBC2;
import megamek.client.ui.Messages;
import megamek.common.equipment.MiscMounted;
import megamek.common.units.Entity;

/**
 * A per-dispenser choice of mine type shown in the unit configuration dialog. Mine dispensers may carry any land-type
 * mine (TO:AUE p.176), so all implemented land mine types are offered here. Command-detonated mines are listed but
 * disabled because dispenser deployment is not yet implemented for them.
 */
public class MineChoice {

    /**
     * A selectable mine type. {@code code} is the {@link MiscMounted} {@code MINE_*} constant; {@code implemented} is
     * false for placeholder types that cannot yet be deployed by a dispenser.
     */
    private record MineOption(int code, String label, boolean implemented) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final List<MineOption> MINE_OPTIONS = List.of(
          new MineOption(MiscMounted.MINE_CONVENTIONAL, Messages.getString("CustomMekDialog.Conventional"), true),
          new MineOption(MiscMounted.MINE_VIBRABOMB, Messages.getString("CustomMekDialog.Vibrabomb"), true),
          new MineOption(MiscMounted.MINE_ACTIVE, Messages.getString("CustomMekDialog.Active"), true),
          new MineOption(MiscMounted.MINE_INFERNO, Messages.getString("CustomMekDialog.Inferno"), true),
          new MineOption(MiscMounted.MINE_EMP, Messages.getString("CustomMekDialog.EMP"), true),
          new MineOption(MiscMounted.MINE_COMMAND_DETONATED,
                Messages.getString("CustomMekDialog.CommandDetonatedNotImplemented"), false));

    private final JComboBox<MineOption> comboChoices;

    private final MiscMounted miscMounted;

    /** Tracks the last selection that maps to an implemented mine type, so disabled rows can be reverted. */
    private int lastImplementedIndex;

    public MineChoice(MiscMounted miscMounted, Entity entity, JPanel parentPanel, GBC2 gbc) {
        this.miscMounted = miscMounted;
        comboChoices = new JComboBox<>();
        for (MineOption option : MINE_OPTIONS) {
            comboChoices.addItem(option);
        }
        comboChoices.setRenderer(new MineOptionRenderer());
        selectMineType(miscMounted.getMineType());
        lastImplementedIndex = comboChoices.getSelectedIndex();

        // Prevent the user from settling on a placeholder (unimplemented) mine type.
        comboChoices.addActionListener(event -> {
            MineOption selected = (MineOption) comboChoices.getSelectedItem();
            if ((selected != null) && !selected.implemented()) {
                comboChoices.setSelectedIndex(lastImplementedIndex);
            } else {
                lastImplementedIndex = comboChoices.getSelectedIndex();
            }
        });

        parentPanel.add(new JLabel(entity.getLocationName(miscMounted.getLocation()) + ":"), gbc.forLabel());
        parentPanel.add(comboChoices, gbc.eol());
    }

    /**
     * Selects the combo row whose mine code matches the given type. Unimplemented placeholder types (and unknown codes)
     * fall back to the first row, which is always an implemented type, so the dialog never starts on a disabled row.
     */
    private void selectMineType(int mineType) {
        for (int index = 0; index < MINE_OPTIONS.size(); index++) {
            MineOption option = MINE_OPTIONS.get(index);
            if ((option.code() == mineType) && option.implemented()) {
                comboChoices.setSelectedIndex(index);
                return;
            }
        }
        comboChoices.setSelectedIndex(0);
    }

    public void applyChoice() {
        MineOption selected = (MineOption) comboChoices.getSelectedItem();
        if ((selected != null) && selected.implemented()) {
            miscMounted.setMineType(selected.code());
        }
    }

    public void setEnabled(boolean enabled) {
        comboChoices.setEnabled(enabled);
    }

    /** Greys out placeholder (unimplemented) mine types so they read as unavailable. */
    private static class MineOptionRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof MineOption option) {
                component.setEnabled(option.implemented());
            }
            return component;
        }
    }
}
