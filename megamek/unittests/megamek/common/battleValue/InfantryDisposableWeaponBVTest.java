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
package megamek.common.battleValue;

import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a conventional infantry platoon carrying a Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing)
 * has a higher Battle Value than an otherwise identical platoon without one, i.e. the disposable offensive capability
 * is not free.
 */
class InfantryDisposableWeaponBVTest {

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
    }

    private ConvInfantry createInfantry(boolean withDisposable) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setGame(game);
        infantry.setId(game.getNextEntityId());
        infantry.setChassis("Test Platoon");
        infantry.setModel(withDisposable ? "Disposable" : "Standard");
        infantry.setCrew(new Crew(CrewType.INFANTRY_CREW));
        infantry.setOwner(game.getPlayer(0));
        infantry.setPrimaryWeapon((InfantryWeapon) EquipmentType.get("InfantryAssaultRifle"));
        if (withDisposable) {
            infantry.setDisposableWeapon((InfantryWeapon) EquipmentType.get("Rocket Launcher (LAW)"));
        }
        infantry.autoSetInternal();
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        return infantry;
    }

    @Test
    @DisplayName("a Disposable Weapon adds 0.2 x (weapon BV x troopers) to BV")
    void disposableWeaponAddsBattleValue() {
        ConvInfantry withDisposable = createInfantry(true);
        ConvInfantry withoutDisposable = createInfantry(false);

        int bvWith = withDisposable.calculateBattleValue();
        int bvWithout = withoutDisposable.calculateBattleValue();
        int delta = bvWith - bvWithout;

        assertTrue(delta > 0, "Disposable Weapon should raise BV. With: " + bvWith + ", Without: " + bvWithout);

        // TO:AuE p.116, Corrected Sixth Printing: Disposable Weapons add only 0.2 x (weapon BV x troopers) to the
        // offensive value, so the BV increase is a small fraction of counting them at full BV. For this platoon
        // (Rocket Launcher (LAW), 28 troopers) the increase is modest (~6 BV); counting at full BV would add roughly
        // 5x as much. This upper bound guards against a regression to full-BV counting.
        assertTrue(delta < 15, "Disposable BV should be reduced by the 0.2 factor, got delta " + delta);
    }
}
