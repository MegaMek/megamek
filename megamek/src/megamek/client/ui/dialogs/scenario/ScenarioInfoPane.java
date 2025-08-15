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
package megamek.client.ui.dialogs.scenario;

import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import megamek.common.annotations.Nullable;
import megamek.common.scenario.Scenario;

/**
 * This panel displays a scrollable vertical list of ScenarioInfo panels.
 */
class ScenarioInfoPane extends JScrollPane {

    private JList<Scenario> presets;
    private final List<Scenario> scenarioInfoList = new ArrayList<>();

    public ScenarioInfoPane(List<Scenario> scenarioInfoList) {
        this.scenarioInfoList.addAll(scenarioInfoList);
        setBorder(null);
        getVerticalScrollBar().setUnitIncrement(16);
        initialize();
    }

    public @Nullable Scenario getSelectedPreset() {
        return presets.getSelectedValue();
    }

    protected void initialize() {
        final DefaultListModel<Scenario> listModel = new DefaultListModel<>();
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
