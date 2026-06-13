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
package megamek.client.ui.entityreadout;

import javax.swing.JButton;
import javax.swing.JFrame;

import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.MenuButton;
import megamek.client.ui.clientGUI.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.dialogs.unitSelectorDialogs.AvailabilityPanel;
import megamek.client.ui.dialogs.unitSelectorDialogs.EntityReadoutPanel;
import megamek.client.ui.panels.alphaStrike.ConfigurableASCardPanel;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.game.Game;
import megamek.common.templates.TROView;
import megamek.common.ui.EnhancedTabbedPane;
import megamek.common.units.Entity;

/**
 * The EntityViewPane displays the entity summary, TRO and AS card panels within a TabbedPane.
 */
class LiveEntityViewPane extends EnhancedTabbedPane {

    private final LiveEntityReadoutPanel readoutPanel;
    private final EntityReadoutPanel troPanel = new EntityReadoutPanel();
    private final ConfigurableASCardPanel alphaStrikeCardPanel;
    private final AvailabilityPanel factionPanel = new AvailabilityPanel();
    private boolean menuVisible = true;

    private final int entityId;
    private final Game game;

    public LiveEntityViewPane(JFrame frame, Game game, int entityId) {
        super(false, true);
        alphaStrikeCardPanel = new ConfigurableASCardPanel(frame);
        this.entityId = entityId;
        this.game = game;
        readoutPanel = new LiveEntityReadoutPanel(game, entityId);
    }

    /**
     * This purposefully does not set preferences, as it may be used on differing panes for differing uses, and thus you
     * don't want to remember the selected tab between the different locations.
     */
    protected void initialize() {
        JButton menuButton = new MenuButton();
        menuButton.setToolTipText("Show/hide menus");
        menuButton.addActionListener(ev -> toggleMenus());
        addActionButton(menuButton);

        readoutPanel.setName("entityPanel");
        readoutPanel.initialize();

        troPanel.setName("troPanel");

        addTab(Messages.getString("Summary.title"), readoutPanel);
        addTab(Messages.getString("TRO.title"), troPanel);
        addTab(Messages.getString("ASCard.title"), alphaStrikeCardPanel);
        addTab(Messages.getString("FactionAvailability.title"), factionPanel.getPanel());

        updateDisplayedEntity();
    }

    /**
     * Updates the pane's currently displayed entity in all tabs. Performs Alpha Strike conversion if possible.
     */
    private void updateDisplayedEntity() {
        Entity entity = game.getEntityFromAllSources(entityId);
        if (entity == null) {
            troPanel.reset();
            factionPanel.reset();
            alphaStrikeCardPanel.setASElement(null);
        } else {
            troPanel.showEntity(entity, TROView.createView(entity, ViewFormatting.HTML));
            factionPanel.setUnit(entity.getModel(), entity.getFullChassis());
            if (ASConverter.canConvert(entity)) {
                alphaStrikeCardPanel.setASElement(ASConverter.convert(entity, new FlexibleCalculationReport()));
            }
        }
    }

    private void toggleMenus() {
        menuVisible = !menuVisible;
        readoutPanel.toggleMenu(menuVisible);
        alphaStrikeCardPanel.toggleMenu(menuVisible);
    }

    @Override
    public void dispose() {
        readoutPanel.dispose();
    }
}
