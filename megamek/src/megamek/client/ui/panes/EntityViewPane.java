/*
 * Copyright (c) 2021-2023 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.annotations.Nullable;
import megamek.common.templates.TROView;

import javax.swing.*;

/**
 * The EntityViewPane displays the entity summary, TRO and AS card panels within a TabbedPane.
 */
public class EntityViewPane extends AbstractTabbedPane {
    private final ConfigurableMechViewPanel summaryPanel = new ConfigurableMechViewPanel();
    private final MechViewPanel troPanel = new MechViewPanel();
    private final ConfigurableASCardPanel cardPanel = new ConfigurableASCardPanel(getFrame());

    public EntityViewPane(final JFrame frame, final @Nullable Entity entity) {
        super(frame, "EntityViewPane");
        initialize();
        updateDisplayedEntity(entity);
    }

    /**
     * This purposefully does not set preferences, as it may be used on differing panes for
     * differing uses and thus you don't want to remember the selected tab between the different
     * locations.
     */
    @Override
    protected void initialize() {
        summaryPanel.setName("entityPanel");
        troPanel.setName("troPanel");

        addTab(resources.getString("Summary.title"), summaryPanel);
        addTab(resources.getString("TRO.title"), troPanel);
        addTab(resources.getString("ASCard.title"), cardPanel);
    }

    /**
     * Updates the pane's currently displayed entity in all tabs. Performs Alpha Strike conversion if possible.
     *
     * @param entity the entity to update to, or null if the panels are to be emptied.
     */
    public void updateDisplayedEntity(final @Nullable Entity entity) {
        AlphaStrikeElement asUnit = null;
        if (ASConverter.canConvert(entity)) {
            asUnit = ASConverter.convert(entity, new FlexibleCalculationReport());
        }
        updateDisplayedEntity(entity, asUnit);
    }

    /**
     * Updates the pane's currently displayed entity / AS unit to the respective given units. The method
     * assumes that asUnit corresponds to entity and does no conversion. When the AS Element or MechSummary
     * is available, passing it in as asUnit saves the time for AS conversion.
     *
     * @param entity the entity to update to, or null if the panels are to be emptied.
     * @param asUnit the Alpha Strike unit corresponding to entity (may be a MechSummary)
     */
    public void updateDisplayedEntity(final @Nullable Entity entity, @Nullable ASCardDisplayable asUnit) {
        if (entity == null) {
            troPanel.reset();
        } else {
            troPanel.setMech(entity, TROView.createView(entity, true));
        }
        summaryPanel.setEntity(entity);
        cardPanel.setASElement(asUnit);
    }
}