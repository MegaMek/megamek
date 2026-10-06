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

package megamek.client.ui.dialogs.lobby;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.units.Entity;

/**
 * Asks the player what order a set of units should be connected in when forming a tractor-and-trailer train.
 * <p>
 * The order is not cosmetic. Trailers are always hitched at the tail, and a unit can only draw ammunition from the
 * units directly ahead of and behind it, so the carriage placed immediately behind the gun is the only one that can
 * feed it. Getting that wrong is invisible until a weapon has nothing to fire, which is why the order is shown and
 * editable rather than inferred from the selection.
 * </p>
 */
public class TrainOrderDialog extends JDialog {

    private final DefaultListModel<Entity> trainModel = new DefaultListModel<>();
    private final JList<Entity> trainList = new JList<>(trainModel);
    private final JLabel resultLabel = new JLabel();
    private boolean confirmed = false;

    /**
     * @param frame the parent frame
     * @param head  the tractor that will head the train
     * @param trailers the trailers to hitch behind it, in their initial suggested order
     */
    public TrainOrderDialog(JFrame frame, Entity head, List<Entity> trailers) {
        super(frame, Messages.getString("TrainOrderDialog.title"), true);

        for (Entity trailer : trailers) {
            trainModel.addElement(trailer);
        }

        trainList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trainList.setCellRenderer((list, entity, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel((index + 2) + ". " + entity.getShortName());
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            }
            label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            return label;
        });
        if (!trainModel.isEmpty()) {
            trainList.setSelectedIndex(0);
        }

        JPanel headPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headPanel.add(new JLabel("1. " + head.getShortName() + "   "
              + Messages.getString("TrainOrderDialog.tractor")));

        JButton moveUpButton = new JButton(Messages.getString("TrainOrderDialog.moveUp"));
        moveUpButton.addActionListener(event -> moveSelected(-1));
        JButton moveDownButton = new JButton(Messages.getString("TrainOrderDialog.moveDown"));
        moveDownButton.addActionListener(event -> moveSelected(1));
        JPanel reorderPanel = new JPanel();
        reorderPanel.add(moveUpButton);
        reorderPanel.add(moveDownButton);

        JPanel orderPanel = new JPanel(new BorderLayout());
        orderPanel.setBorder(BorderFactory.createTitledBorder(Messages.getString("TrainOrderDialog.order")));
        orderPanel.add(headPanel, BorderLayout.NORTH);
        orderPanel.add(new JScrollPane(trainList), BorderLayout.CENTER);
        orderPanel.add(reorderPanel, BorderLayout.SOUTH);

        JButton connectButton = new JButton(Messages.getString("TrainOrderDialog.connect"));
        connectButton.addActionListener(event -> {
            confirmed = true;
            setVisible(false);
        });
        JButton cancelButton = new JButton(Messages.getString("TrainOrderDialog.cancel"));
        cancelButton.addActionListener(event -> setVisible(false));
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);

        JPanel resultPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        resultPanel.add(resultLabel);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(orderPanel, BorderLayout.CENTER);
        content.add(resultPanel, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        updateResultLabel(head);
        trainList.addListSelectionListener(event -> updateResultLabel(head));

        setMinimumSize(UIUtil.scaleForGUI(420, 320));
        pack();
        setLocationRelativeTo(frame);
    }

    private void moveSelected(int offset) {
        int index = trainList.getSelectedIndex();
        int target = index + offset;
        if ((index < 0) || (target < 0) || (target >= trainModel.size())) {
            return;
        }
        Entity moved = trainModel.remove(index);
        trainModel.add(target, moved);
        trainList.setSelectedIndex(target);
    }

    private void updateResultLabel(Entity head) {
        StringBuilder result = new StringBuilder("<html>");
        result.append(head.getShortName());
        for (int index = 0; index < trainModel.size(); index++) {
            result.append(" -&gt; ").append(trainModel.get(index).getShortName());
        }
        result.append("</html>");
        resultLabel.setText(result.toString());
    }

    /** @return true when the player confirmed the train rather than cancelling */
    public boolean wasConfirmed() {
        return confirmed;
    }

    /** @return the trailers in the order chosen, front to back */
    public List<Entity> getOrderedTrailers() {
        List<Entity> ordered = new ArrayList<>();
        for (int index = 0; index < trainModel.size(); index++) {
            ordered.add(trainModel.get(index));
        }
        return ordered;
    }
}
