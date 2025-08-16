/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.advancedsearch;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import megamek.common.options.IGameOptions;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;

class TriStateItemList {

    private final JList<TriStateItem> list = new JList<>(new DefaultListModel<>());

    TriStateItemList(IGameOptions opts, int visibleRows) {
        List<String> qs = new ArrayList<>();
        for (final Enumeration<IOptionGroup> optionGroups = opts.getGroups(); optionGroups.hasMoreElements(); ) {
            final IOptionGroup group = optionGroups.nextElement();
            for (final Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                final IOption option = options.nextElement();
                if (option != null) {
                    qs.add(option.getDisplayableNameWithValue());
                }
            }
        }
        qs = qs.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();

        DefaultListModel<TriStateItem> dlm = new DefaultListModel<>();

        for (String q : qs) {
            dlm.addElement(new TriStateItem("\u2610", q));
        }
        list.setModel(dlm);
        initializeList(visibleRows);
    }

    TriStateItemList(List<String> content, int visibleRows) {
        DefaultListModel<TriStateItem> defaultListModel = new DefaultListModel<>();
        for (String desc : content) {
            defaultListModel.addElement(new TriStateItem("\u2610", desc));
        }
        list.setModel(defaultListModel);
        initializeList(visibleRows);
    }

    TriStateItemList(Map<Integer, String> s, int visibleRows) {
        DefaultListModel<TriStateItem> defaultListModel = new DefaultListModel<>();

        for (Map.Entry<Integer, String> desc : s.entrySet()) {
            defaultListModel.addElement(new TriStateItem("\u2610", desc.getKey(), desc.getValue()));
        }

        list.setModel(defaultListModel);
        initializeList(visibleRows);
    }

    private void initializeList(int visibleRows) {
        list.setVisibleRowCount(visibleRows);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectionModel(new NoSelectionModel());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    toggleText(list.locationToIndex(e.getPoint()));
                }
            }
        });
    }

    private void toggleText(int index) {
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
        ListModel<TriStateItem> m = list.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            TriStateItem ms = m.getElementAt(i);
            ms.state = "\u2610";
        }

        list.setModel(m);
        list.repaint();
    }

    public JList<TriStateItem> getComponent() {
        return list;
    }

    public void toStringResultLists(List<String> include, List<String> exclude) {
        ListModel<TriStateItem> m = list.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            TriStateItem ms = m.getElementAt(i);
            if (ms.state.contains("\u2611")) {
                include.add(ms.text);
            } else if (ms.state.contains("\u2612")) {
                exclude.add(ms.text);
            }
        }
    }

    public void toIntegerResultLists(List<Integer> include, List<Integer> exclude) {
        ListModel<TriStateItem> m = list.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            TriStateItem ms = m.getElementAt(i);
            if (ms.state.contains("\u2611")) {
                include.add(ms.code);

            } else if (ms.state.contains("\u2612")) {
                exclude.add(ms.code);
            }
        }
    }


}
