/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import javax.swing.JTabbedPane;

import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.battlefieldSupport.BattlefieldSupportAssetData;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.HandheldWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EntityViewPaneBattlefieldSupportTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void standaloneAssetShowsThreeTopLevelAssetViews() {
        BattlefieldSupportAsset asset = testAsset();
        EntityViewPane pane = new EntityViewPane(null, null);

        pane.updateDisplayedEntity(asset, null, asset);

        assertTabTitles(pane, "Summary", "TRO", "BFS Card");
    }

    @Test
    void linkedAssetGroupsItsThreeViewsUnderOneBfsTab() {
        BattlefieldSupportAsset asset = testAsset();
        HandheldWeapon base = new HandheldWeapon();
        base.setChassis("Base Unit");
        base.setModel("");
        EntityViewPane pane = new EntityViewPane(null, null);

        pane.updateDisplayedEntity(base, null, asset);

        assertTabTitles(pane, "Summary", "TRO", "AS Card", "Faction Availability", "Analysis", "BFS");
        JTabbedPane bfsTabs = assertInstanceOf(JTabbedPane.class, pane.getComponentAt(pane.indexOfTab("BFS")));
        assertTabTitles(bfsTabs, "Summary", "TRO", "BFS Card");
    }

    @Test
    void switchingBetweenStandaloneAndLinkedAssetsKeepsAllBfsSubtabs() {
        BattlefieldSupportAsset asset = testAsset();
        HandheldWeapon base = new HandheldWeapon();
        base.setChassis("Base Unit");
        base.setModel("");
        EntityViewPane pane = new EntityViewPane(null, null);

        pane.updateDisplayedEntity(asset, null, asset);
        pane.updateDisplayedEntity(base, null, asset);
        pane.updateDisplayedEntity(asset, null, asset);
        pane.updateDisplayedEntity(base, null, asset);

        JTabbedPane bfsTabs = assertInstanceOf(JTabbedPane.class, pane.getComponentAt(pane.indexOfTab("BFS")));
        assertTabTitles(bfsTabs, "Summary", "TRO", "BFS Card");
    }

    private static void assertTabTitles(JTabbedPane pane, String... titles) {
        assertEquals(titles.length, pane.getTabCount());
        for (int i = 0; i < titles.length; i++) {
            assertEquals(titles[i], pane.getTitleAt(i));
        }
    }

    private static BattlefieldSupportAsset testAsset() {
        BattlefieldSupportAssetData data = new BattlefieldSupportAssetData();
        data.setChassis("Test Asset");
        return new BattlefieldSupportAsset(data);
    }
}
