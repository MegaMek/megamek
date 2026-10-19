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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.Player;
import megamek.common.TechConstants;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.bombs.clan.CLAAAMissileWeapon;
import megamek.common.weapons.bombs.clan.CLASEWMissileWeapon;
import megamek.common.weapons.bombs.clan.CLASMissileWeapon;
import megamek.common.weapons.bombs.clan.CLLAAMissileWeapon;
import megamek.common.weapons.bombs.innerSphere.ISAAAMissileWeapon;
import megamek.common.weapons.bombs.innerSphere.ISASEWMissileWeapon;
import megamek.common.weapons.bombs.innerSphere.ISASMissileWeapon;
import megamek.common.weapons.bombs.innerSphere.ISLAAMissileWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Loading external ordnance missiles onto a fighter (issue #2084 follow-up). The LAA and ASEW weapons could not be
 * found by their bomb weapon name, so loading them threw and every bomb after them was dropped from the fighter.
 */
class BombMissileLoadingTest {

    private Game game;
    private Player owner;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        owner = new Player(0, "Bomber side");
        game.addPlayer(0, owner);
        game.getOptions().getOption(OptionsConstants.ALLOWED_TECH_LEVEL).setValue("Experimental");
    }

    private AeroSpaceFighter loadedFighter(int techLevel) {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setGame(game);
        fighter.setId(game.getNextEntityId());
        fighter.setCrew(new Crew(CrewType.SINGLE));
        fighter.setOwner(owner);
        fighter.setTechLevel(techLevel);
        fighter.setWeight(100);
        fighter.autoSetMaxBombPoints();

        BombLoadout loadout = new BombLoadout();
        loadout.put(BombTypeEnum.LAA, 1);
        loadout.put(BombTypeEnum.ASEW, 1);
        loadout.put(BombTypeEnum.AAA, 1);
        loadout.put(BombTypeEnum.AS, 1);
        fighter.setExtBombChoices(loadout);
        fighter.applyBombs();
        return fighter;
    }

    private static int countBombWeapons(Entity entity, Class<?> weaponClass) {
        int count = 0;
        for (WeaponMounted weapon : entity.getWeaponList()) {
            if (weaponClass.isInstance(weapon.getType())) {
                count++;
            }
        }
        return count;
    }

    @Test
    void innerSphereFighterGetsEveryMissileItCarries() {
        AeroSpaceFighter fighter = loadedFighter(TechConstants.T_IS_EXPERIMENTAL);

        assertEquals(1, countBombWeapons(fighter, ISLAAMissileWeapon.class), "LAA");
        assertEquals(1, countBombWeapons(fighter, ISASEWMissileWeapon.class), "ASEW");
        assertEquals(1, countBombWeapons(fighter, ISAAAMissileWeapon.class), "AAA");
        assertEquals(1, countBombWeapons(fighter, ISASMissileWeapon.class), "AS");
    }

    @Test
    void clanFighterGetsTheClanVersionOfEachMissile() {
        AeroSpaceFighter fighter = loadedFighter(TechConstants.T_CLAN_EXPERIMENTAL);

        assertEquals(1, countBombWeapons(fighter, CLLAAMissileWeapon.class), "LAA");
        assertEquals(1, countBombWeapons(fighter, CLASEWMissileWeapon.class), "ASEW");
        assertEquals(1, countBombWeapons(fighter, CLAAAMissileWeapon.class), "AAA");
        assertEquals(1, countBombWeapons(fighter, CLASMissileWeapon.class), "AS");
    }
}
