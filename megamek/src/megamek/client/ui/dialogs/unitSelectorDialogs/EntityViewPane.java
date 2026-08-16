/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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
import javax.swing.JTabbedPane;

import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.MenuButton;
import megamek.client.ui.clientGUI.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.panels.alphaStrike.ConfigurableASCardPanel;
import megamek.client.ui.panels.battlefieldSupport.ConfigurableBFSCardPanel;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.annotations.Nullable;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
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
    private final ConfigurableMekViewPanel assetSummaryPanel = new ConfigurableMekViewPanel();
    private final EntityReadoutPanel assetTroPanel = new EntityReadoutPanel();
    private final ConfigurableBFSCardPanel bfsCardPanel = new ConfigurableBFSCardPanel();
    private final ConfigurableBFSCardPanel linkedBfsCardPanel = new ConfigurableBFSCardPanel();
    private final JTabbedPane bfsViewPane = new JTabbedPane();
    private final AvailabilityPanel factionPanel = new AvailabilityPanel();
    private final DamageAnalysisPanel analysisPanel = new DamageAnalysisPanel();
    private boolean menuVisible = true;

    /** Which set of tabs is currently shown, so tabs are only rebuilt when the configuration actually changes. */
    private TabConfiguration tabConfiguration;

    /** The tab layouts for a standard unit, a linked unit/Asset pair, or a standalone Asset. */
    private enum TabConfiguration { STANDARD, WITH_ASSET_CARD, STANDALONE_ASSET }

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
        assetSummaryPanel.setName("assetEntityPanel");
        assetTroPanel.setName("assetTroPanel");
        bfsViewPane.setName("bfsViewPane");
        bfsViewPane.addTab(Messages.getString("Summary.title"), assetSummaryPanel);
        bfsViewPane.addTab(Messages.getString("TRO.title"), assetTroPanel);
        bfsViewPane.addTab(Messages.getString("BFSCard.title"), linkedBfsCardPanel);

        applyTabConfiguration(TabConfiguration.STANDARD);
    }

    /** Adds the standard unit tabs (Summary, TRO, AS Card, Faction Availability) in order. */
    private void addStandardTabs() {
        addTab(Messages.getString("Summary.title"), summaryPanel);
        addTab(Messages.getString("TRO.title"), troPanel);
        addTab(Messages.getString("ASCard.title"), cardPanel);
        addTab(Messages.getString("FactionAvailability.title"), factionPanel.getPanel());
        addTab(Messages.getString("DamageAnalysis.title"), analysisPanel);
    }

    /**
     * Rebuilds the tab set to the given configuration, but only when it differs from the current one (so switching
     * between units of the same kind keeps the selected tab). The selected tab is restored by title where possible.
     */
    private void applyTabConfiguration(TabConfiguration configuration) {
        if (configuration == tabConfiguration) {
            return;
        }
        String selectedTitle = (getSelectedIndex() >= 0) ? getTitleAt(getSelectedIndex()) : null;
        removeAll();
        switch (configuration) {
            case STANDALONE_ASSET -> {
                addTab(Messages.getString("Summary.title"), summaryPanel);
                addTab(Messages.getString("TRO.title"), troPanel);
                addTab(Messages.getString("BFSCard.title"), bfsCardPanel);
            }
            case WITH_ASSET_CARD -> {
                addStandardTabs();
                addTab(Messages.getString("BFSViews.title"), bfsViewPane);
            }
            case STANDARD -> addStandardTabs();
        }
        tabConfiguration = configuration;
        if (selectedTitle != null) {
            int index = indexOfTab(selectedTitle);
            if (index >= 0) {
                setSelectedIndex(index);
            }
        }
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
        updateDisplayedEntity(entity, asUnit, null);
    }

    /**
     * Updates the pane's displayed unit, including a Battlefield Support Asset for the BFS Card tab. The BFS Card tab
     * is shown whenever an asset is present. A standalone Asset uses its Entity for Summary and TRO while omitting the
     * standard-unit AS Card, Faction Availability and Damage Analysis tabs.
     *
     * @param entity      the entity for the standard Summary/TRO tabs; the Asset itself for a standalone Asset
     * @param asUnit      the Alpha Strike unit corresponding to entity (maybe a MekSummary), or null
     * @param assetEntity the Battlefield Support Asset to show in the Asset views, or null if the unit has no asset
     */
    public void updateDisplayedEntity(@Nullable Entity entity, @Nullable ASCardDisplayable asUnit,
          @Nullable BattlefieldSupportAsset assetEntity) {
        boolean hasAsset = assetEntity != null;
        boolean standaloneAsset = hasAsset && (entity instanceof BattlefieldSupportAsset);
        if (standaloneAsset) {
            applyTabConfiguration(TabConfiguration.STANDALONE_ASSET);
        } else if (hasAsset) {
            applyTabConfiguration(TabConfiguration.WITH_ASSET_CARD);
        } else {
            applyTabConfiguration(TabConfiguration.STANDARD);
        }

        if (entity == null) {
            troPanel.reset();
            factionPanel.reset();
        } else {
            troPanel.showEntity(entity, TROView.createView(entity, ViewFormatting.HTML));
            if (standaloneAsset) {
                factionPanel.reset();
            } else {
                factionPanel.setUnit(entity.getModel(), entity.getFullChassis());
            }
        }

        summaryPanel.setEntity(entity);
        cardPanel.setASElement(ASConverter.canConvert(entity) ? asUnit : null);
        assetSummaryPanel.setEntity(assetEntity);
        if (assetEntity == null) {
            assetTroPanel.reset();
        } else {
            assetTroPanel.showEntity(assetEntity, TROView.createView(assetEntity, ViewFormatting.HTML));
        }
        bfsCardPanel.setAsset(assetEntity);
        linkedBfsCardPanel.setAsset(assetEntity);
        analysisPanel.setEntity(standaloneAsset ? null : entity);
    }

    /**
     * Sets the gunnery skill the Analysis tab displays its expected-damage curves at, so the tab
     * can follow a live control such as the unit selector's BV gunnery field.
     *
     * @param gunnery the gunnery skill for the analysis charts
     */
    public void setAnalysisGunnery(int gunnery) {
        analysisPanel.setGunnery(gunnery);
    }

    private void toggleMenus() {
        menuVisible = !menuVisible;
        summaryPanel.toggleMenu(menuVisible);
        cardPanel.toggleMenu(menuVisible);
        assetSummaryPanel.toggleMenu(menuVisible);
        bfsCardPanel.toggleMenu(menuVisible);
        linkedBfsCardPanel.toggleMenu(menuVisible);
    }
}
