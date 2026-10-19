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
package megamek.client.ui.entityreadout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.NumberFormat;

import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the readout's cost override, which lets a host application such as MekHQ show its own price for a unit in
 * place of the construction cost.
 *
 * @author Illiani
 * @since 0.51.01
 */
class GeneralEntityReadoutCostOverrideTest {
    private static final double CONSTRUCTION_COST = 1_000_000;
    private static final double OVERRIDE_COST = 5_500_000;

    private Entity entity;

    @BeforeEach
    void setUp() {
        entity = mock(Entity.class);
        when(entity.getCost(false)).thenReturn(CONSTRUCTION_COST);
    }

    /**
     * @param cost the cost to format
     *
     * @return the cost as the readout formats it
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static String formatted(double cost) {
        NumberFormat costFormatter = NumberFormat.getInstance();
        costFormatter.setMinimumFractionDigits(2);
        costFormatter.setMaximumFractionDigits(2);
        return costFormatter.format(cost) + " C-bills";
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    void withoutAnOverrideTheConstructionCostIsShown() {
        GeneralEntityReadout readout = new GeneralEntityReadout(entity, false, false, false);

        String costLine = readout.createCostElement().toPlainText();

        assertTrue(costLine.contains(formatted(CONSTRUCTION_COST)), costLine);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    void anOverrideReplacesTheConstructionCost() {
        GeneralEntityReadout readout = new GeneralEntityReadout(entity, false, false, false);
        readout.setCostOverride(OVERRIDE_COST);

        String costLine = readout.createCostElement().toPlainText();

        assertTrue(costLine.contains(formatted(OVERRIDE_COST)), costLine);
        assertFalse(costLine.contains(formatted(CONSTRUCTION_COST)), costLine);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    @Test
    void clearingTheOverrideRestoresTheConstructionCost() {
        GeneralEntityReadout readout = new GeneralEntityReadout(entity, false, false, false);
        readout.setCostOverride(OVERRIDE_COST);
        readout.setCostOverride(null);

        String costLine = readout.createCostElement().toPlainText();

        assertTrue(costLine.contains(formatted(CONSTRUCTION_COST)), costLine);
    }
}
