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
package megamek.client.ui.panes;

import megamek.client.ui.baseComponents.AbstractTabbedPane;
import megamek.client.ui.swing.MechViewPanel;
import megamek.client.ui.swing.alphaStrike.ConfigurableASCardPanel;
import megamek.client.ui.swing.calculationReport.FlexibleCalculationReport;
import megamek.common.Entity;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.annotations.Nullable;
import megamek.common.templates.TROView;

import javax.swing.*;

/**
 * The EntityViewPane displays the Entity Summary and the TRO panels within a Tabbed Pane.
 */
public class EntityViewPane extends AbstractTabbedPane {
    //region Variable Declarations
    private ConfigurableMechViewPanel entityPanel;
    private MechViewPanel troPanel;
    private final ConfigurableASCardPanel cardPanel = new ConfigurableASCardPanel(getFrame());
    //endregion Variable Declarations

    //region Constructors
    public EntityViewPane(final JFrame frame, final @Nullable Entity entity) {
        super(frame, "EntityViewPane");
        initialize();
        updateDisplayedEntity(entity);
    }
    //endregion Constructors

    //region Getters/Setters
    public ConfigurableMechViewPanel getEntityPanel() {
        return entityPanel;
    }

    public void setEntityPanel(final ConfigurableMechViewPanel entityPanel) {
        this.entityPanel = entityPanel;
    }

    public MechViewPanel getTROPanel() {
        return troPanel;
    }

    public void setTROPanel(final MechViewPanel troPanel) {
        this.troPanel = troPanel;
    }
    //endregion Getters/Setters

    //region Initialization
    /**
     * This purposefully does not set preferences, as it may be used on differing panes for
     * differing uses and thus you don't want to remember the selected tab between the different
     * locations.
     */
    @Override
    protected void initialize() {
        setEntityPanel(new ConfigurableMechViewPanel());
        getEntityPanel().setName("entityPanel");
        addTab(resources.getString("Summary.title"), getEntityPanel());

        setTROPanel(new MechViewPanel());
        getTROPanel().setName("troPanel");
        addTab(resources.getString("TRO.title"), getTROPanel());

        addTab("AS Card", cardPanel);
    }
    //endregion Initialization

    /**
     * This updates the pane's currently displayed entity
     * @param entity the entity to update to, or null if the panels are to be reset.
     */
    public void updateDisplayedEntity(final @Nullable Entity entity) {
        // Null entity, which means to reset the panels
        if (entity == null) {
            getEntityPanel().reset();
            getTROPanel().reset();
        } else {
            getEntityPanel().setEntity(entity);
            getTROPanel().setMech(entity, TROView.createView(entity, true));
        }
        if (ASConverter.canConvert(entity)) {
            cardPanel.setASElement(ASConverter.convert(entity, new FlexibleCalculationReport()));
        } else {
            cardPanel.setASElement(null);
        }
    }
}
