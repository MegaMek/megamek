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
package megamek.common.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #9048: cancelling a queued shot from a handheld weapon left that weapon marked as fired.
 *
 * <p>A handheld weapon is its own unit, so its shot is filed under the handheld weapon, with the weapon's number on
 * the handheld weapon. Cancelling looked that number up on the Mek holding it instead, and reset the wrong weapon.</p>
 */
class WeaponAttackActionGetWeaponTest {

    private static final int MEK_ID = 1;
    private static final int HANDHELD_WEAPON_ID = 2;
    private static final int TARGET_ID = 3;

    private Game game;
    private Mounted<?> mekLaser;
    private Mounted<?> handheldLaser;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws Exception {
        game = new Game();

        Mek mek = new BipedMek();
        mek.setId(MEK_ID);
        mekLaser = mek.addEquipment(EquipmentType.get("ISMediumLaser"), Mek.LOC_RIGHT_ARM);
        game.addEntity(mek, false);

        HandheldWeapon handheldWeapon = new HandheldWeapon();
        handheldWeapon.setId(HANDHELD_WEAPON_ID);
        handheldLaser = handheldWeapon.addEquipment(EquipmentType.get("ISMediumLaser"), HandheldWeapon.LOC_GUN);
        game.addEntity(handheldWeapon, false);
    }

    @Test
    void handheldWeaponShotResolvesToTheHandheldWeaponNotTheMek() {
        // Both lasers are weapon number 0 on their own unit, which is what made the wrong lookup look plausible.
        assertEquals(mekLaser.getEquipmentNum(), handheldLaser.getEquipmentNum());

        WeaponAttackAction handheldShot = new WeaponAttackAction(HANDHELD_WEAPON_ID, Targetable.TYPE_ENTITY,
              TARGET_ID, handheldLaser.getEquipmentNum());

        assertSame(handheldLaser, handheldShot.getWeapon(game));
        assertNotSame(mekLaser, handheldShot.getWeapon(game));
    }

    @Test
    void mekShotStillResolvesToTheMeksOwnWeapon() {
        WeaponAttackAction mekShot = new WeaponAttackAction(MEK_ID, Targetable.TYPE_ENTITY, TARGET_ID,
              mekLaser.getEquipmentNum());

        assertSame(mekLaser, mekShot.getWeapon(game));
    }

    @Test
    void shotFromAnUnknownUnitHasNoWeapon() {
        WeaponAttackAction orphanShot = new WeaponAttackAction(99, Targetable.TYPE_ENTITY, TARGET_ID, 0);

        assertNull(orphanShot.getWeapon(game));
    }
}
