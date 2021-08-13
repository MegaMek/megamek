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
package megamek.client.ui.panels;

import megamek.client.generator.enums.SkillGeneratorMethod;
import megamek.client.generator.enums.SkillGeneratorType;
import megamek.client.ui.baseComponents.AbstractPanel;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.MMToggleButton;
import megamek.common.enums.SkillLevel;

import javax.swing.*;
import java.awt.*;

public class SkillGenerationOptionsPanel extends AbstractPanel {
    //region Variable Declarations
    private final ClientGUI clientGUI;
    private MMComboBox<SkillGeneratorMethod> comboMethod;
    private MMComboBox<SkillGeneratorType> comboType;
    private MMComboBox<SkillLevel> comboSkillLevel;
    private MMToggleButton tglForceClose;
    //endregion Variable Declarations

    //region Constructors
    public SkillGenerationOptionsPanel(final JFrame frame, final ClientGUI clientGUI) {
        super(frame, "SkillGenerationOptionsPanel");
        this.clientGUI = clientGUI;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public ClientGUI getClientGUI() {
        return clientGUI;
    }

    public MMComboBox<SkillGeneratorMethod> getComboMethod() {
        return comboMethod;
    }

    public SkillGeneratorMethod getMethod() {
        return getComboMethod().getSelectedItem();
    }

    public void setComboMethod(final MMComboBox<SkillGeneratorMethod> comboMethod) {
        this.comboMethod = comboMethod;
    }

    public MMComboBox<SkillGeneratorType> getComboType() {
        return comboType;
    }

    public SkillGeneratorType getType() {
        return getComboType().getSelectedItem();
    }

    public void setComboType(final MMComboBox<SkillGeneratorType> comboType) {
        this.comboType = comboType;
    }

    public MMComboBox<SkillLevel> getComboSkillLevel() {
        return comboSkillLevel;
    }

    public SkillLevel getSkillLevel() {
        return getComboSkillLevel().getSelectedItem();
    }

    public void setComboSkillLevel(final MMComboBox<SkillLevel> comboSkillLevel) {
        this.comboSkillLevel = comboSkillLevel;
    }

    public MMToggleButton getTglForceClose() {
        return tglForceClose;
    }

    public boolean isForceClose() {
        return getTglForceClose().isSelected();
    }

    public void setTglForceClose(final MMToggleButton tglForceClose) {
        this.tglForceClose = tglForceClose;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected void initialize() {
        // Create Panel Components
        final JLabel lblMethod = new JLabel(resources.getString("lblMethod.text"));
        lblMethod.setToolTipText(resources.getString("lblMethod.toolTipText"));
        lblMethod.setName("lblMethod");

        setComboMethod(new MMComboBox<>("comboMethod", SkillGeneratorMethod.values()));
        getComboMethod().setToolTipText(resources.getString("lblMethod.toolTipText"));
        getComboMethod().setName("comboMethod");
        getComboMethod().setSelectedItem(getClientGUI().getClient().getSkillGenerator().getMethod());
        getComboMethod().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SkillGeneratorMethod) {
                    list.setToolTipText(((SkillGeneratorMethod) value).getToolTipText());
                }
                return this;
            }
        });

        final JLabel lblType = new JLabel(resources.getString("lblType.text"));
        lblType.setToolTipText(resources.getString("lblType.toolTipText"));
        lblType.setName("lblType");

        setComboType(new MMComboBox<>("comboType", SkillGeneratorType.values()));
        getComboType().setToolTipText(resources.getString("lblType.toolTipText"));
        getComboType().setName("comboType");
        getComboType().setSelectedItem(getClientGUI().getClient().getSkillGenerator().getType());
        getComboType().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SkillGeneratorType) {
                    list.setToolTipText(((SkillGeneratorType) value).getToolTipText());
                }
                return this;
            }
        });

        final JLabel lblSkillLevel = new JLabel(resources.getString("lblSkillLevel.text"));
        lblSkillLevel.setToolTipText(resources.getString("lblSkillLevel.toolTipText"));
        lblSkillLevel.setName("lblSkillLevel");

        final DefaultComboBoxModel<SkillLevel> skillLevelModel = new DefaultComboBoxModel<>();
        skillLevelModel.addAll(SkillLevel.getGeneratableValues());
        setComboSkillLevel(new MMComboBox<>("comboSkillLevel", skillLevelModel));
        getComboSkillLevel().setToolTipText(resources.getString("lblSkillLevel.toolTipText"));
        getComboSkillLevel().setName("comboSkillLevel");
        getComboSkillLevel().setSelectedItem(getClientGUI().getClient().getSkillGenerator().getLevel());
        getComboSkillLevel().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SkillLevel) {
                    list.setToolTipText(((SkillLevel) value).getToolTipText());
                }
                return this;
            }
        });

        setTglForceClose(new MMToggleButton(resources.getString("tglForceClose.text")));
        getTglForceClose().setToolTipText(resources.getString("tglForceClose.toolTipText"));
        getTglForceClose().setName("tglForceClose");
        getTglForceClose().setSelected(getClientGUI().getClient().getSkillGenerator().isForceClose());

        // Programmatically Assign Accessibility Labels
        lblMethod.setLabelFor(getComboMethod());
        lblType.setLabelFor(getComboType());
        lblSkillLevel.setLabelFor(getComboSkillLevel());

        // Layout the Panel
        final GroupLayout layout = new GroupLayout(this);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        setLayout(layout);

        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblMethod)
                                        .addComponent(getComboMethod()))
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblType)
                                        .addComponent(getComboType()))
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblSkillLevel)
                                        .addComponent(getComboSkillLevel()))
                                .addComponent(getTglForceClose()))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblMethod)
                                        .addComponent(lblType)
                                        .addComponent(lblSkillLevel))
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(getComboMethod())
                                        .addComponent(getComboType())
                                        .addComponent(getComboSkillLevel())
                                        .addComponent(getTglForceClose())))
        );
    }
    //endregion Initialization
}
