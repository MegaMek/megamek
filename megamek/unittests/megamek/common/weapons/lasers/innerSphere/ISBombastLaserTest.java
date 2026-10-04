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
package megamek.common.weapons.lasers.innerSphere;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.enums.ChargeLevel;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link ISBombastLaser#isExplosive(Mounted, boolean)} (issue #8705).
 *
 * <p>The AlphaStrike ENE conversion asks a weapon type whether it is explosive without supplying a specific mount,
 * passing {@code null} instead. {@link megamek.common.equipment.EquipmentType} handles that, but the Bombast Laser
 * override used to dereference the parameter straight away, so every unit whose Bombast Laser was reached by that
 * check threw a {@link NullPointerException} while the unit cache was being built and was dropped from the game
 * entirely.</p>
 */
public class ISBombastLaserTest {

    private static final String BOMBAST_LASER_INTERNAL_NAME = "Bombast Laser";

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static EquipmentType bombastLaserType() {
        EquipmentType bombastLaser = EquipmentType.get(BOMBAST_LASER_INTERNAL_NAME);
        assertNotNull(bombastLaser, "Bombast Laser should be a registered equipment type");
        return bombastLaser;
    }

    private static Mounted<?> bombastLaserMount() {
        Entity carrier = new BipedMek();
        // Mounted.setUsedThisRound() reads the phase off the carrier's game, so the carrier needs one.
        carrier.setGame(new Game());
        Mounted<?> bombastLaser = Mounted.createMounted(carrier, bombastLaserType());
        assertNotNull(bombastLaser, "Bombast Laser should be mountable");
        return bombastLaser;
    }

    @Test
    @DisplayName("Asking the type without a mount does not throw (issue #8705)")
    void isExplosiveWithoutMountDoesNotThrow() {
        EquipmentType bombastLaser = bombastLaserType();
        assertDoesNotThrow(() -> bombastLaser.isExplosive(null),
              "Asking a Bombast Laser whether it is explosive without a mount must not throw");
    }

    @Test
    @DisplayName("A Bombast Laser with no mount to inspect is not explosive")
    void isExplosiveWithoutMountIsFalse() {
        assertFalse(bombastLaserType().isExplosive(null),
              "A Bombast Laser with no charge state to read should not count as explosive");
    }

    @Test
    @DisplayName("An uncharged Bombast Laser is not explosive")
    void unchargedBombastLaserIsNotExplosive() {
        Mounted<?> bombastLaser = bombastLaserMount();
        bombastLaser.setChargeState(ChargeLevel.CHARGE_NONE);
        assertFalse(bombastLaser.getType().isExplosive(bombastLaser),
              "An uncharged Bombast Laser should not be explosive");
    }

    @Test
    @DisplayName("A charged Bombast Laser is explosive")
    void chargedBombastLaserIsExplosive() {
        Mounted<?> bombastLaser = bombastLaserMount();
        bombastLaser.setChargeState(ChargeLevel.CHARGED);
        assertTrue(bombastLaser.getType().isExplosive(bombastLaser),
              "A Bombast Laser holding a charge should be explosive");
    }

    @Test
    @DisplayName("A charged Bombast Laser that already fired this round is not explosive")
    void chargedBombastLaserFiredThisRoundIsNotExplosive() {
        Mounted<?> bombastLaser = bombastLaserMount();
        bombastLaser.setChargeState(ChargeLevel.CHARGED);
        bombastLaser.setUsedThisRound(true);
        assertFalse(bombastLaser.getType().isExplosive(bombastLaser),
              "A Bombast Laser that has discharged into an attack should no longer be explosive");
    }
}
