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

package megamek.client.ui.clientGUI.tooltip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that the crew-advantages tooltip only lists the Edge group (Edge points and triggers) when the Edge game option
 * is enabled (issue #7142).
 */
class PilotToolTipEdgeTest {

    /** A specific Edge trigger the test explicitly enables, rather than relying on option defaults. */
    private static final String SAMPLE_EDGE_TRIGGER = OptionsConstants.EDGE_WHEN_HEAD_HIT;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private Entity mekWithEdgeOption(boolean edgeEnabled) {
        BipedMek mek = new BipedMek();
        // Explicitly enable a known Edge trigger so the test does not depend on option defaults.
        mek.getCrew().getOptions().getOption(SAMPLE_EDGE_TRIGGER).setValue(true);
        Game game = new Game();
        GameOptions options = new GameOptions();
        options.getOption(OptionsConstants.EDGE).setValue(edgeEnabled);
        game.setOptions(options);
        mek.setGame(game);
        return mek;
    }

    /**
     * The trigger's localized display name, taken from the option itself. For a boolean option this is exactly what the
     * tooltip renders, so it stays correct across localization and wording changes.
     */
    private String edgeTriggerDisplayName(Entity mek) {
        return mek.getCrew().getOptions().getOption(SAMPLE_EDGE_TRIGGER).getDisplayableName();
    }

    @Test
    @DisplayName("Edge triggers are hidden on the unit card when the Edge option is disabled")
    void edgeGroupHiddenWhenEdgeDisabled() {
        Entity mek = mekWithEdgeOption(false);
        String tip = PilotToolTip.getCrewAdvantages(mek, true).toString();
        assertFalse(tip.contains(edgeTriggerDisplayName(mek)),
              "Edge triggers should not be listed when the Edge game option is disabled");
    }

    @Test
    @DisplayName("Edge triggers are shown on the unit card when the Edge option is enabled")
    void edgeGroupShownWhenEdgeEnabled() {
        Entity mek = mekWithEdgeOption(true);
        String tip = PilotToolTip.getCrewAdvantages(mek, true).toString();
        assertTrue(tip.contains(edgeTriggerDisplayName(mek)),
              "Edge triggers should be listed when the Edge game option is enabled");
    }
}
