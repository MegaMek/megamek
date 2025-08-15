/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.abstractPanels;

import static megamek.client.ui.util.UIUtil.TipCombo;
import static megamek.client.ui.util.UIUtil.TipLabel;
import static megamek.client.ui.util.UIUtil.TipMMToggleButton;
import static megamek.client.ui.util.UIUtil.formatSideTooltip;

import java.awt.Component;
import java.util.Objects;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;

import megamek.client.Client;
import megamek.client.generator.enums.SkillGeneratorMethod;
import megamek.client.generator.enums.SkillGeneratorType;
import megamek.client.ui.buttons.MMToggleButton;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;

public class SkillGenerationOptionsPanel extends AbstractPanel {
    //region Variable Declarations
    private final ClientGUI clientGUI;
    private Client client;
    private TipCombo<SkillGeneratorMethod> comboMethod;
    private TipCombo<SkillGeneratorType> comboType;
    private TipCombo<SkillLevel> comboSkillLevel;
    private MMToggleButton tglForceClose;
    //endregion Variable Declarations

    //region Constructors
    public SkillGenerationOptionsPanel(final JFrame frame, final ClientGUI clientGUI,
          final @Nullable Client client) {
        super(frame, "SkillGenerationOptionsPanel");
        this.clientGUI = clientGUI;
        setClient((client == null) ? getClientGUI().getClient() : client);
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public ClientGUI getClientGUI() {
        return clientGUI;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(final Client client) {
        this.client = client;
    }

    public MMComboBox<SkillGeneratorMethod> getComboMethod() {
        return comboMethod;
    }

    public void setComboMethod(final TipCombo<SkillGeneratorMethod> comboMethod) {
        this.comboMethod = comboMethod;
    }

    public MMComboBox<SkillGeneratorType> getComboType() {
        return comboType;
    }

    public void setComboType(final TipCombo<SkillGeneratorType> comboType) {
        this.comboType = comboType;
    }

    public MMComboBox<SkillLevel> getComboSkillLevel() {
        return comboSkillLevel;
    }

    public void setComboSkillLevel(final TipCombo<SkillLevel> comboSkillLevel) {
        this.comboSkillLevel = comboSkillLevel;
    }

    public MMToggleButton getTglForceClose() {
        return tglForceClose;
    }

    public void setTglForceClose(final MMToggleButton tglForceClose) {
        this.tglForceClose = tglForceClose;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected void initialize() {
        // Create Panel Components
        final JLabel lblMethod = new TipLabel(resources.getString("lblMethod.text"));
        lblMethod.setToolTipText(resources.getString("lblMethod.toolTipText"));
        lblMethod.setName("lblMethod");

        setComboMethod(new TipCombo<>("comboMethod", SkillGeneratorMethod.values()));
        getComboMethod().setToolTipText(resources.getString("lblMethod.toolTipText"));
        getComboMethod().setSelectedItem(getClient().getSkillGenerator().getMethod());
        getComboMethod().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                  final int index, final boolean isSelected,
                  final boolean cellHasFocus) {
                if (value instanceof SkillGeneratorMethod) {
                    list.setToolTipText(formatSideTooltip(((SkillGeneratorMethod) value).getToolTipText()));
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        final JLabel lblType = new TipLabel(resources.getString("lblType.text"));
        lblType.setToolTipText(resources.getString("lblType.toolTipText"));
        lblType.setName("lblType");

        setComboType(new TipCombo<>("comboType", SkillGeneratorType.values()));
        getComboType().setToolTipText(resources.getString("lblType.toolTipText"));
        getComboType().setSelectedItem(getClient().getSkillGenerator().getType());
        getComboType().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                  final int index, final boolean isSelected,
                  final boolean cellHasFocus) {
                if (value instanceof SkillGeneratorType) {
                    list.setToolTipText(formatSideTooltip(((SkillGeneratorType) value).getToolTipText()));
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        final JLabel lblSkillLevel = new TipLabel(resources.getString("lblSkillLevel.text"));
        lblSkillLevel.setToolTipText(resources.getString("lblSkillLevel.toolTipText"));
        lblSkillLevel.setName("lblSkillLevel");

        final DefaultComboBoxModel<SkillLevel> skillLevelModel = new DefaultComboBoxModel<>();
        skillLevelModel.addAll(SkillLevel.getGeneratableValues());
        setComboSkillLevel(new TipCombo<>("comboSkillLevel", skillLevelModel));
        getComboSkillLevel().setToolTipText(resources.getString("lblSkillLevel.toolTipText"));
        getComboSkillLevel().setSelectedItem(getClient().getSkillGenerator().getLevel());
        getComboSkillLevel().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                  final int index, final boolean isSelected,
                  final boolean cellHasFocus) {
                if (value instanceof SkillLevel) {
                    list.setToolTipText(formatSideTooltip(((SkillLevel) value).getToolTipText()));
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        setTglForceClose(new TipMMToggleButton(resources.getString("tglForceClose.text")));
        getTglForceClose().setToolTipText(resources.getString("tglForceClose.toolTipText"));
        getTglForceClose().setName("tglForceClose");
        getTglForceClose().setSelected(getClient().getSkillGenerator().isForceClose());

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

    public void changeClient(final @Nullable Client client) {
        if ((client == null) || client.getName().equalsIgnoreCase(getClient().getName())) {
            return;
        }
        updateClient();
        setClient(client);
        setValuesFromClient();
    }

    public void updateClient() {
        final SkillGeneratorMethod method = Objects.requireNonNull(getComboMethod().getSelectedItem());
        if (method != getClient().getSkillGenerator().getMethod()) {
            getClient().setSkillGenerator(method.getGenerator());
        }
        getClient().getSkillGenerator().setType(getComboType().getSelectedItem());
        getClient().getSkillGenerator().setLevel(getComboSkillLevel().getSelectedItem());
        getClient().getSkillGenerator().setForceClose(getTglForceClose().isSelected());
    }

    private void setValuesFromClient() {
        getComboMethod().setSelectedItem(getClient().getSkillGenerator().getMethod());
        getComboType().setSelectedItem(getClient().getSkillGenerator().getType());
        getComboSkillLevel().setSelectedItem(getClient().getSkillGenerator().getLevel());
        getTglForceClose().setSelected(getClient().getSkillGenerator().isForceClose());
    }
}
