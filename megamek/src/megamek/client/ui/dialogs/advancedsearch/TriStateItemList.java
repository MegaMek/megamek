/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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

import com.formdev.flatlaf.extras.components.FlatTriStateCheckBox;
import megamek.common.options.IGameOptions;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TriStateItemList {

    private final List<FlatTriStateCheckBox> checkBoxes = new ArrayList<>();
    private final Map<FlatTriStateCheckBox, Integer> codes = new HashMap<>();
    private final JComponent listPanel = new ScrollableBox();

    private final int visibleRows;

    TriStateItemList(IGameOptions opts, int visibleRows) {
        this(visibleRows);
        List<String> quirks = new ArrayList<>();
        for (final Enumeration<IOptionGroup> optionGroups = opts.getGroups(); optionGroups.hasMoreElements(); ) {
            final IOptionGroup group = optionGroups.nextElement();
            for (final Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                final IOption option = options.nextElement();
                if (option != null) {
                    quirks.add(option.getDisplayableNameWithValue());
                }
            }
        }
        quirks.sort(String.CASE_INSENSITIVE_ORDER);
        initializeList(quirks);
    }

    TriStateItemList(List<String> content, int visibleRows) {
        this(visibleRows);
        initializeList(content);
    }

    TriStateItemList(Map<Integer, String> namesAndCodes, int visibleRows) {
        this(visibleRows);
        for (Map.Entry<Integer, String> nameAndCode : namesAndCodes.entrySet()) {
            var checkBox = new SearchTriStateCheckBox(nameAndCode.getValue());
            checkBoxes.add(checkBox);
            codes.put(checkBox, nameAndCode.getKey());
        }
        checkBoxes.forEach(listPanel::add);
    }

    public TriStateItemList(int visibleRows) {
        this.visibleRows = visibleRows;
    }

    private void initializeList(Collection<String> content) {
        content.stream().map(SearchTriStateCheckBox::new).forEach(checkBoxes::add);
        checkBoxes.forEach(listPanel::add);
    }

    void clear() {
        checkBoxes.forEach(cb -> cb.setSelected(false));
    }

    public JComponent getComponent() {
        return listPanel;
    }

    public void toStringResultLists(List<String> include, List<String> exclude) {
        for (FlatTriStateCheckBox ms : checkBoxes) {
            if (ms.getState() == FlatTriStateCheckBox.State.SELECTED) {
                include.add(ms.getText());
            } else if (ms.getState() == FlatTriStateCheckBox.State.INDETERMINATE) {
                exclude.add(ms.getText());
            }
        }
    }

    public void toIntegerResultLists(List<Integer> include, List<Integer> exclude) {
        for (FlatTriStateCheckBox ms : checkBoxes) {
            if (ms.getState() == FlatTriStateCheckBox.State.SELECTED) {
                include.add(codes.get(ms));
            } else if (ms.getState() == FlatTriStateCheckBox.State.INDETERMINATE) {
                exclude.add(codes.get(ms));
            }
        }
    }

    private class ScrollableBox extends JPanel implements Scrollable {

        public ScrollableBox() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            var originalSize = getPreferredSize();
            if (!checkBoxes.isEmpty() && visibleRows > 0) {
                var checkBox = checkBoxes.get(0);
                return new Dimension(originalSize.width,
                      Math.min(visibleRows, checkBoxes.size()) * checkBox.getPreferredSize().height);
            } else {
                return originalSize;
            }
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            if (!checkBoxes.isEmpty()) {
                var checkBox = checkBoxes.get(0);
                return checkBox.getPreferredSize().height;
            } else {
                return 0;
            }
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return getScrollableUnitIncrement(visibleRect, orientation, direction) * 3;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    AdvSearchState.TriStateItemListState getState() {
        var state = new AdvSearchState.TriStateItemListState();
        for (FlatTriStateCheckBox ms : checkBoxes) {
            if (ms.getState() != FlatTriStateCheckBox.State.UNSELECTED) {
                state.items.put(ms.getText(), ms.getState());
            }
        }
        return state;
    }

    void applyState(AdvSearchState.TriStateItemListState state) {
        for (FlatTriStateCheckBox ms : checkBoxes) {
            if (state.items.containsKey(ms.getText())) {
                ms.setState(state.items.get(ms.getText()));
            }
        }
    }
}
