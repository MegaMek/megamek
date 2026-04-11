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
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static megamek.testUtilities.MMTestUtilities.getEntityForUnitTesting;

import megamek.common.units.Jumpship;
import megamek.common.units.SmallCraft;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for bay ammo validation in {@link TestSmallCraft} and
 * {@link TestAdvancedAerospace}. Verifies that design validity is based on
 * design-spec ammo counts ({@code getOriginalShots()}) rather than current
 * remaining shots ({@code getUsableShotsLeft()}), preventing campaign units
 * with partially expended ammo from being flagged as illegal.
 *
 * @see <a href="https://github.com/MegaMek/mekhq/issues/6606">MekHQ #6606</a>
 */
class BayAmmoValidationTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    /**
     * A Leopard DropShip with full ammo should pass bay validation.
     */
    @Test
    void leopardWithFullAmmoPassesValidation() {
        Entity entity = getEntityForUnitTesting("Leopard (2537)", true);
        assertNotNull(entity, "Failed to load Leopard test entity");
        assertTrue(entity instanceof SmallCraft, "Leopard should be a SmallCraft");

        TestSmallCraft verifier = new TestSmallCraft((SmallCraft) entity,
              EntityVerifier.getInstance(null).aeroOption, null);

        StringBuffer buff = new StringBuffer();
        boolean illegal = verifier.hasIllegalEquipmentCombinations(buff);
        assertFalse(illegal, "Leopard with full ammo should not be illegal: " + buff);
    }

    /**
     * A Leopard DropShip with partially expended ammo (simulating a campaign battle)
     * should still pass bay validation. This is the regression test for MekHQ #6606:
     * the validator was using current shots instead of design-spec shots, causing
     * campaign units to be rejected as illegal after firing some ammo.
     */
    @Test
    void leopardWithDepletedAmmoStillPassesValidation() {
        Entity entity = getEntityForUnitTesting("Leopard (2537)", true);
        assertNotNull(entity, "Failed to load Leopard test entity");
        assertTrue(entity instanceof SmallCraft, "Leopard should be a SmallCraft");

        // Simulate ammo expenditure from a campaign battle
        for (AmmoMounted ammo : entity.getAmmo()) {
            int original = ammo.getOriginalShots();
            if (original > 0) {
                ammo.setShotsLeft(original / 2);
            }
        }

        TestSmallCraft verifier = new TestSmallCraft((SmallCraft) entity,
              EntityVerifier.getInstance(null).aeroOption, null);

        StringBuffer buff = new StringBuffer();
        boolean illegal = verifier.hasIllegalEquipmentCombinations(buff);
        assertFalse(illegal,
              "Leopard with partially expended ammo should not be illegal: " + buff);
    }

    /**
     * An Explorer JumpShip with full ammo should pass bay validation.
     */
    @Test
    void explorerJumpShipWithFullAmmoPassesValidation() {
        Entity entity = getEntityForUnitTesting("Explorer JumpShip", true);
        assertNotNull(entity, "Failed to load Explorer JumpShip test entity");
        assertTrue(entity instanceof Jumpship, "Explorer should be a Jumpship");

        TestAdvancedAerospace verifier = new TestAdvancedAerospace((Jumpship) entity,
              EntityVerifier.getInstance(null).aeroOption, null);

        StringBuffer buff = new StringBuffer();
        boolean illegal = verifier.hasIllegalEquipmentCombinations(buff);
        assertFalse(illegal, "Explorer JumpShip with full ammo should not be illegal: " + buff);
    }

    /**
     * An Explorer JumpShip with partially expended ammo should still pass bay validation.
     * Same regression scenario as the Leopard test but for {@link TestAdvancedAerospace}.
     */
    @Test
    void explorerJumpShipWithDepletedAmmoStillPassesValidation() {
        Entity entity = getEntityForUnitTesting("Explorer JumpShip", true);
        assertNotNull(entity, "Failed to load Explorer JumpShip test entity");
        assertTrue(entity instanceof Jumpship, "Explorer should be a Jumpship");

        // Simulate ammo expenditure from a campaign battle
        for (AmmoMounted ammo : entity.getAmmo()) {
            int original = ammo.getOriginalShots();
            if (original > 0) {
                ammo.setShotsLeft(original / 2);
            }
        }

        TestAdvancedAerospace verifier = new TestAdvancedAerospace((Jumpship) entity,
              EntityVerifier.getInstance(null).aeroOption, null);

        StringBuffer buff = new StringBuffer();
        boolean illegal = verifier.hasIllegalEquipmentCombinations(buff);
        assertFalse(illegal,
              "Explorer JumpShip with partially expended ammo should not be illegal: " + buff);
    }
}
