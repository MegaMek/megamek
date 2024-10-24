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

import megamek.common.options.AbstractOptions;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

class TriStateItemList {

    private final JList<TriStateItem> list = new JList<>(new DefaultListModel<>());

    TriStateItemList(AbstractOptions opts, int visibleRows) {
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
        inititalizeList(visibleRows);
    }

    TriStateItemList(List<String> content, int visibleRows) {
        DefaultListModel<TriStateItem> dlma = new DefaultListModel<>();
        for (String desc : content) {
            dlma.addElement(new TriStateItem("\u2610", desc));
        }
        list.setModel(dlma);
        inititalizeList(visibleRows);
    }

    TriStateItemList(Map<Integer, String> s, int visibleRows) {
        DefaultListModel<TriStateItem> dlma = new DefaultListModel<>();

        for (Map.Entry<Integer, String> desc : s.entrySet()) {
            dlma.addElement(new TriStateItem("\u2610", desc.getKey(), desc.getValue()));
        }

        list.setModel(dlma);
        inititalizeList(visibleRows);
    }

    private void inititalizeList(int visibleRows) {
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
