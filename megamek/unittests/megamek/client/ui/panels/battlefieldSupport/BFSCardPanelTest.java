/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.client.ui.panels.battlefieldSupport;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Font;
import java.io.File;

import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link BFSCardPanel} renders a Battlefield Support Asset (and the empty state) without error, exercising
 * the panel -> {@code BattlefieldSupportCard} rendering path used by the unit selector's BFS Card tab.
 */
class BFSCardPanelTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    private static BattlefieldSupportAsset loadMaxim() throws Exception {
        return (BattlefieldSupportAsset) new MekFileParser(
              new File("testresources/data/mekfiles/Maxim Heavy Hover Transport.bfs")).getEntity();
    }

    @Test
    void rendersAssetCard() throws Exception {
        BFSCardPanel panel = new BFSCardPanel();
        panel.setAsset(loadMaxim());
        assertNotNull(panel.getCardImage());
    }

    @Test
    void rendersEmptyCardWithNoAsset() {
        BFSCardPanel panel = new BFSCardPanel();
        panel.setAsset(null);
        assertNotNull(panel.getCardImage());
    }

    @Test
    void appliesFontAndScaleWithoutError() throws Exception {
        BFSCardPanel panel = new BFSCardPanel();
        panel.setAsset(loadMaxim());
        panel.setScale(0.5f);
        panel.setCardFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        assertNotNull(panel.getCardImage());
    }
}
