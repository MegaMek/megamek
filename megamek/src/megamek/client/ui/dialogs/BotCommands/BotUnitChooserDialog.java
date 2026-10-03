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
package megamek.client.ui.dialogs.BotCommands;

import java.awt.Container;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;

/**
 * Lets the player pick any mix of one bot's units to give an order to: each unit has a checkbox under its lance, and
 * each lance a box that ticks all of its units. MegaMek's board selects one unit at a time, so this is how a player
 * orders, say, the Atlas and one Champion from another lance together.
 */
public class BotUnitChooserDialog extends AbstractButtonDialog {

    private static final int GAP = 8;
    private static final int INDENT = 20;
    private static final int SCROLL_INCREMENT = 16;
    private static final int PREFERRED_WIDTH = 360;
    private static final int PREFERRED_HEIGHT = 420;

    private final Map<String, List<BotOrdersMenuBuilder.OrderGroup>> unitsByLance;
    private final Map<Integer, JCheckBox> unitBoxes = new LinkedHashMap<>();

    /**
     * @param frame        the parent frame
     * @param unitsByLance the bot's units, each a one-unit group, keyed by lance name in display order
     */
    public BotUnitChooserDialog(JFrame frame, Map<String, List<BotOrdersMenuBuilder.OrderGroup>> unitsByLance) {
        super(frame, "BotUnitChooserDialog", "BotCommandPanel.Orders.chooseUnits.title");
        this.unitsByLance = unitsByLance;
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(UIUtil.scaleForGUI(GAP), UIUtil.scaleForGUI(GAP), UIUtil.scaleForGUI(GAP),
              UIUtil.scaleForGUI(GAP)));
        for (Map.Entry<String, List<BotOrdersMenuBuilder.OrderGroup>> lance : unitsByLance.entrySet()) {
            JCheckBox lanceBox = new JCheckBox(lance.getKey());
            lanceBox.setFont(lanceBox.getFont().deriveFont(Font.BOLD));
            list.add(lanceBox);
            List<JCheckBox> lanceUnitBoxes = new ArrayList<>();
            for (BotOrdersMenuBuilder.OrderGroup unit : lance.getValue()) {
                JCheckBox unitBox = new JCheckBox(unit.label());
                unitBox.setBorder(new EmptyBorder(0, UIUtil.scaleForGUI(INDENT), 0, 0));
                unitBoxes.put(unit.unitIds().get(0), unitBox);
                lanceUnitBoxes.add(unitBox);
                list.add(unitBox);
            }
            lanceBox.addActionListener(event -> {
                for (JCheckBox unitBox : lanceUnitBoxes) {
                    unitBox.setSelected(lanceBox.isSelected());
                }
            });
        }
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.getVerticalScrollBar().setUnitIncrement(UIUtil.scaleForGUI(SCROLL_INCREMENT));
        scrollPane.setPreferredSize(UIUtil.scaleForGUI(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        return scrollPane;
    }

    /**
     * @return the ticked units as one group, or {@code null} when none is ticked
     */
    public @Nullable BotOrdersMenuBuilder.OrderGroup getChosenGroup() {
        List<Integer> chosenIds = new ArrayList<>();
        for (Map.Entry<Integer, JCheckBox> unitBox : unitBoxes.entrySet()) {
            if (unitBox.getValue().isSelected()) {
                chosenIds.add(unitBox.getKey());
            }
        }
        if (chosenIds.isEmpty()) {
            return null;
        }
        return new BotOrdersMenuBuilder.OrderGroup(
              Messages.getString("BotCommandPanel.Orders.chosenUnits", chosenIds.size()), chosenIds);
    }
}
