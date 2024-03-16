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

import megamek.common.annotations.Nullable;
import megamek.common.scenario.ScenarioShortInfo2;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This panel displays a scrollable vertical list of ScenarioInfo panels.
 */
class ScenarioInfoPane extends JScrollPane {

    private JList<ScenarioShortInfo2> presets;
    private final List<ScenarioShortInfo2> scenarioInfoList = new ArrayList<>();

    public ScenarioInfoPane(List<ScenarioShortInfo2> scenarioInfoList) {
        this.scenarioInfoList.addAll(scenarioInfoList);
        setBorder(null);
        getVerticalScrollBar().setUnitIncrement(16);
        initialize();
    }

    public @Nullable ScenarioShortInfo2 getSelectedPreset() {
        return presets.getSelectedValue();
    }

    protected void initialize() {
        final DefaultListModel<ScenarioShortInfo2> listModel = new DefaultListModel<>();
        listModel.addAll(scenarioInfoList);
        presets = new JList<>(listModel);
        presets.setName("ScenarioInfoList");
        presets.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        presets.setLayoutOrientation(JList.VERTICAL);
        presets.setCellRenderer(new ScenarioInfoRenderer());

        var panel = new JPanel();
        panel.add(presets);
        setViewportView(panel);
    }
}