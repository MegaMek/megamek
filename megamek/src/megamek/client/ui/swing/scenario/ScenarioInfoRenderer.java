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
package megamek.client.ui.swing.scenario;

import megamek.common.scenario.ScenarioInfo;
import megamek.common.scenario.ScenarioShortInfo2;

import javax.swing.*;
import java.awt.*;

/**
 * This is a JList renderer for {@link ScenarioInfoPanel}.
 */
public class ScenarioInfoRenderer extends ScenarioInfoPanel implements ListCellRenderer<ScenarioShortInfo2> {

    @Override
    public Component getListCellRendererComponent(final JList<? extends ScenarioShortInfo2> list,
                                                  final ScenarioShortInfo2 value, final int index,
                                                  final boolean isSelected,
                                                  final boolean cellHasFocus) {
        final Color foreground = new Color((isSelected
                ? list.getSelectionForeground() : list.getForeground()).getRGB());
        final Color background = new Color((isSelected
                ? list.getSelectionBackground() : list.getBackground()).getRGB());
        setForeground(foreground);
        setBackground(background);
        updateFromPreset(value);
        return this;
    }
}