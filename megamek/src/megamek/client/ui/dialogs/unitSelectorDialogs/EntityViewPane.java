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
package megamek.client.ui.dialogs.unitSelectorDialogs;

import javax.swing.JButton;
import javax.swing.JFrame;

import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.MenuButton;
import megamek.client.ui.clientGUI.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.panels.alphaStrike.ConfigurableASCardPanel;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.annotations.Nullable;
import megamek.common.templates.TROView;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.units.Entity;

/**
 * The EntityViewPane displays the entity summary, TRO and AS card panels within a TabbedPane.
 */
public class EntityViewPane extends EnhancedTabbedPane {

    private final ConfigurableMekViewPanel summaryPanel = new ConfigurableMekViewPanel();
    private final EntityReadoutPanel troPanel = new EntityReadoutPanel();
    private final ConfigurableASCardPanel cardPanel;
    private final AvailabilityPanel factionPanel = new AvailabilityPanel();
    private boolean menuVisible = true;

    public EntityViewPane(final JFrame frame, final @Nullable Entity entity) {
        super(false, true);
        cardPanel = new ConfigurableASCardPanel(frame);
        initialize();
        updateDisplayedEntity(entity);
    }

    /**
     * This purposefully does not set preferences, as it may be used on differing panes for differing uses, and thus you
     * don't want to remember the selected tab between the different locations.
     */
    protected void initialize() {
        setName("EntityViewPane");

        JButton menuButton = new MenuButton();
        menuButton.setToolTipText("Show/hide menus");
        menuButton.addActionListener(ev -> toggleMenus());
        addActionButton(menuButton);

        summaryPanel.setName("entityPanel");
        troPanel.setName("troPanel");

        addTab(Messages.getString("Summary.title"), summaryPanel);
        addTab(Messages.getString("TRO.title"), troPanel);
        addTab(Messages.getString("ASCard.title"), cardPanel);
        addTab(Messages.getString("FactionAvailability.title"), factionPanel.getPanel());
    }

    /**
     * Updates the pane's currently displayed entity in all tabs. Performs Alpha Strike conversion if possible.
     *
     * @param entity the entity to update to, or null if the panels are to be emptied.
     */
    public void updateDisplayedEntity(@Nullable Entity entity) {
        AlphaStrikeElement asUnit = null;
        if (ASConverter.canConvert(entity)) {
            asUnit = ASConverter.convert(entity, new FlexibleCalculationReport());
        }
        updateDisplayedEntity(entity, asUnit);
    }

    /**
     * Updates the pane's currently displayed entity / AS unit to the respective given units. The method assumes that
     * asUnit corresponds to entity and does no conversion. When the AS Element or MekSummary is available, passing it
     * in as asUnit saves the time for AS conversion.
     *
     * @param entity the entity to update to, or null if the panels are to be emptied.
     * @param asUnit the Alpha Strike unit corresponding to entity (maybe a MekSummary)
     */
    public void updateDisplayedEntity(@Nullable Entity entity, @Nullable ASCardDisplayable asUnit) {
        if (entity == null) {
            troPanel.reset();
            factionPanel.reset();
        } else {
            troPanel.showEntity(entity, TROView.createView(entity, ViewFormatting.HTML));
            factionPanel.setUnit(entity.getModel(), entity.getFullChassis());
        }

        summaryPanel.setEntity(entity);
        cardPanel.setASElement(ASConverter.canConvert(entity) ? asUnit : null);
    }

    private void toggleMenus() {
        menuVisible = !menuVisible;
        summaryPanel.toggleMenu(menuVisible);
        cardPanel.toggleMenu(menuVisible);
    }
}
