/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.panels.SkillGenerationOptionsPanel;

import javax.swing.*;
import java.awt.*;

public class SkillGenerationDialog extends AbstractButtonDialog {
    //region Variable Declarations
    private final ClientGUI clientGUI;
    private SkillGenerationOptionsPanel skillGenerationOptionsPanel;
    //endregion Variable Declarations

    //region Constructors
    public SkillGenerationDialog(final JFrame frame, final ClientGUI clientGUI) {
        super(frame, "SkillGenerationDialog", "SkillGenerationDialog.title");
        this.clientGUI = clientGUI;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public ClientGUI getClientGUI() {
        return clientGUI;
    }

    public SkillGenerationOptionsPanel getSkillGenerationOptionsPanel() {
        return skillGenerationOptionsPanel;
    }

    public void setSkillGenerationOptionsPanel(final SkillGenerationOptionsPanel skillGenerationOptionsPanel) {
        this.skillGenerationOptionsPanel = skillGenerationOptionsPanel;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        setSkillGenerationOptionsPanel(new SkillGenerationOptionsPanel(getFrame(), getClientGUI()));

        final JScrollPane scrollPane = new JScrollPane(getSkillGenerationOptionsPanel());
        scrollPane.setName("skillGenerationPane");
        return scrollPane;
    }
    //endregion Initialization
}
