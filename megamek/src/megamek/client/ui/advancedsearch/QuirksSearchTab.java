/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.advancedsearch;

import megamek.client.ui.Messages;
import megamek.common.options.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

class QuirksSearchTab extends JPanel {

    // Quirks
    JButton btnQuirksClear = new JButton(Messages.getString("MekSelectorDialog.ClearTab"));
    JLabel lblQuirkInclude = new JLabel("\u2611");
    JComboBox<String> cQuirkInclue = new JComboBox<>();
    JLabel lblQuirkExclude = new JLabel("\u2612");
    JComboBox<String> cQuirkExclude = new JComboBox<>();
    JLabel lblQuirkType = new JLabel(Messages.getString("MekSelectorDialog.Search.Quirk"));
    JList<TriStateItem> listQuirkType = new JList<>(new DefaultListModel<>());
    JScrollPane spQuirkType = new JScrollPane(listQuirkType);
    JLabel lblWeaponQuirkInclude = new JLabel("\u2611");
    JComboBox<String> cWeaponQuirkInclue = new JComboBox<>();
    JLabel lblWeaponQuirkExclude = new JLabel("\u2612");
    JComboBox<String> cWeaponQuirkExclude = new JComboBox<>();
    JLabel lblWeaponQuirkType = new JLabel(Messages.getString("MekSelectorDialog.Search.WeaponQuirk"));
    JList<TriStateItem> listWeaponQuirkType = new JList<>(new DefaultListModel<>());
    JScrollPane spWeaponQuirkType = new JScrollPane(listWeaponQuirkType);

    QuirksSearchTab() {
        btnQuirksClear.addActionListener(e -> clear());

        loadAndOr(cQuirkInclue, 0);
        loadAndOr(cQuirkExclude, 1);
        loadTriStateItem(new Quirks(), listQuirkType, 25);

        loadAndOr(cWeaponQuirkInclue, 0);
        loadAndOr(cWeaponQuirkExclude, 1);
        cWeaponQuirkExclude.setSelectedIndex(1);

        loadTriStateItem(new WeaponQuirks(), listWeaponQuirkType, 17);

        GridBagConstraints c = new GridBagConstraints();
        setLayout(new GridBagLayout());

        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(20, 10, 0, 0);
        c.gridx = 0; c.gridy++;
        JPanel quirkPanel = new JPanel(new BorderLayout());
        JPanel quirkIEPanel = new JPanel(new FlowLayout());
        quirkIEPanel.add(lblQuirkType);
        quirkIEPanel.add(Box.createHorizontalStrut(15));
        quirkIEPanel.add(lblQuirkInclude);
        quirkIEPanel.add(cQuirkInclue);
        quirkIEPanel.add(lblQuirkExclude);
        quirkIEPanel.add(cQuirkExclude);
        quirkPanel.add(quirkIEPanel, BorderLayout.NORTH);
        quirkPanel.add(spQuirkType, BorderLayout.CENTER);
        add(quirkPanel, c);
        c.gridx = 1;
        JPanel weaponQuirkPanel = new JPanel(new BorderLayout());
        JPanel weaponQuirkIEPanel = new JPanel(new FlowLayout());
        weaponQuirkIEPanel.add(lblWeaponQuirkType);
        weaponQuirkIEPanel.add(Box.createHorizontalStrut(15));
        weaponQuirkIEPanel.add(lblWeaponQuirkInclude);
        weaponQuirkIEPanel.add(cWeaponQuirkInclue);
        weaponQuirkIEPanel.add(lblWeaponQuirkExclude);
        weaponQuirkIEPanel.add(cWeaponQuirkExclude);
        weaponQuirkPanel.add(weaponQuirkIEPanel, BorderLayout.NORTH);
        weaponQuirkPanel.add(spWeaponQuirkType, BorderLayout.CENTER);
        add(weaponQuirkPanel, c);
        c.weighty = 1;
        JPanel blankPanel = new JPanel();
        c.gridx = 0; c.gridy++;
        blankPanel.add(btnQuirksClear, c);
        add(blankPanel, c);
    }

    private void loadAndOr(JComboBox<String> cb, int index) {
        cb.addItem(Messages.getString("MekSelectorDialog.Search.and"));
        cb.addItem(Messages.getString("MekSelectorDialog.Search.or"));
        cb.setSelectedIndex(index);
    }

    private void loadTriStateItem(AbstractOptions s, JList<TriStateItem> l, int count) {
        List<String> qs = new ArrayList<>();
        for (final Enumeration<IOptionGroup> optionGroups = s.getGroups(); optionGroups.hasMoreElements(); ) {
            final IOptionGroup group = optionGroups.nextElement();
            for (final Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                final IOption option = options.nextElement();
                if (option != null) {
                    qs.add(option.getDisplayableNameWithValue());
                }
            }
        }
        qs = qs.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());

        DefaultListModel<TriStateItem> dlm  = new DefaultListModel<>();

        for (String q : qs) {
            dlm.addElement(new TriStateItem("\u2610", q));
        }
        l.setModel(dlm);

        l.setVisibleRowCount(count);
        jListSetup(l);
    }

    private void jListSetup(JList<TriStateItem> l) {
        l.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        l.setSelectionModel(new NoSelectionModel());
        l.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    JList<TriStateItem> list = (JList<TriStateItem>) e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    toggleText(list, index);
                }
            }
        });
    }

    private void toggleText(JList<TriStateItem> list, int index) {
        ListModel<TriStateItem> m = list.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            TriStateItem ms = m.getElementAt(i);

            if (index == i) {
                if (ms.state.contains("\u2610")) {
                    ms.state = "\u2611";
                } else if (ms.state.contains("\u2611")) {
                    ms.state = "\u2612";
                } else if (ms.state.contains("\u2612")) {
                    ms.state = "\u2610";
                }
            }
        }

        list.setModel(m);
        list.repaint();
    }

    void clear() {
        cQuirkInclue.setSelectedIndex(0);
        cQuirkExclude.setSelectedIndex(1);
        clearTriStateItem(listQuirkType);

        cWeaponQuirkInclue.setSelectedIndex(0);
        cWeaponQuirkExclude.setSelectedIndex(1);
        clearTriStateItem(listWeaponQuirkType);
    }

    void clearTriStateItem(JList<TriStateItem> l) {
        ListModel<TriStateItem> m = l.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            TriStateItem ms = m.getElementAt(i);
            ms.state = "\u2610";
        }

        l.setModel(m);
        l.repaint();
    }
}
