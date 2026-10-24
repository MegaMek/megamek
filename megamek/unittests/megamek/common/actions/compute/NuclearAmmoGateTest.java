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
package megamek.common.actions.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Nuclear ammo already loaded on a unit cannot be fired while the nuclear weapons game option is off.
 */
class NuclearAmmoGateTest {
    private Game game;
    private Entity attacker;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test"));
        attacker = new BipedMek();
        attacker.setChassis("Test Mek");
        attacker.setGame(game);
    }

    private void setNuclearWeapons(boolean allowed) {
        game.getOptions().getOption(OptionsConstants.ALLOWED_NUCLEAR_WEAPONS).setValue(allowed);
    }

    private static AmmoType longTomMunition(Munitions munition) {
        for (AmmoType ammoType : AmmoType.getMunitionsFor(AmmoTypeEnum.LONG_TOM)) {
            if (ammoType.getMunitionType().contains(munition)) {
                return ammoType;
            }
        }
        throw new IllegalStateException("Long Tom has no " + munition);
    }

    @Test
    void davyCrockettCannotFireWithNuclearWeaponsOff() {
        setNuclearWeapons(false);

        assertEquals(Messages.getString("WeaponAttackAction.NoNuclearWeapons"),
              ComputeToHitIsImpossible.nuclearAmmoGateReason(game, attacker,
                    longTomMunition(Munitions.M_DAVY_CROCKETT_M)));
    }

    @Test
    void nuclearCapitalMissileCannotFireWithNuclearWeaponsOff() {
        setNuclearWeapons(false);

        assertEquals(Messages.getString("WeaponAttackAction.NoNuclearWeapons"),
              ComputeToHitIsImpossible.nuclearAmmoGateReason(game, attacker,
                    (AmmoType) EquipmentType.get("Ammo Alamo")));
    }

    @Test
    void davyCrockettFiresWithNuclearWeaponsOn() {
        setNuclearWeapons(true);

        assertNull(ComputeToHitIsImpossible.nuclearAmmoGateReason(game, attacker,
              longTomMunition(Munitions.M_DAVY_CROCKETT_M)));
    }

    @Test
    void ordinaryAmmoIsNeverBlocked() {
        setNuclearWeapons(false);

        assertNull(ComputeToHitIsImpossible.nuclearAmmoGateReason(game, attacker,
              longTomMunition(Munitions.M_STANDARD)));
    }
}
