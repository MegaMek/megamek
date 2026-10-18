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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #9231: the indirect-fire and homing weapons that could follow up a TAG shot must come
 * from the shooter's own side. When the bot estimates an enemy's TAG shot at one of its units, the enemy's LRMs count
 * and the bot's own LRMs do not.
 */
class PrincessGuidedWeaponsTest {

    private static final int BOT_TEAM = 1;
    private static final int ENEMY_TEAM = 2;

    private static WeaponType lrmTwenty;

    private Princess princess;
    private Player botPlayer;
    private Player enemyPlayer;

    private Entity botTagger;
    private Entity botLrmCarrier;
    private WeaponMounted botLrm;
    private Entity enemyTagger;
    private Entity enemyLrmCarrier;
    private WeaponMounted enemyLrm;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
        lrmTwenty = (WeaponType) EquipmentType.get("ISLRM20");
    }

    @BeforeEach
    void beforeEach() {
        botPlayer = new Player(1, "Bot");
        botPlayer.setTeam(BOT_TEAM);
        enemyPlayer = new Player(2, "Enemy");
        enemyPlayer.setTeam(ENEMY_TEAM);

        botTagger = mockUnit(1, botPlayer, new Coords(5, 5));
        botLrm = mockLrm();
        botLrmCarrier = mockUnit(2, botPlayer, new Coords(5, 7), botLrm);
        enemyTagger = mockUnit(3, enemyPlayer, new Coords(5, 15));
        enemyLrm = mockLrm();
        enemyLrmCarrier = mockUnit(4, enemyPlayer, new Coords(5, 17), enemyLrm);

        markEnemies(botTagger, enemyTagger, enemyLrmCarrier);
        markEnemies(botLrmCarrier, enemyTagger, enemyLrmCarrier);
        markEnemies(enemyTagger, botTagger, botLrmCarrier);
        markEnemies(enemyLrmCarrier, botTagger, botLrmCarrier);

        princess = spy(new Princess("TestPrincess", UUID.randomUUID().toString(), 1));
        doReturn(botPlayer).when(princess).getLocalPlayer();
        doReturn(List.of(botTagger, botLrmCarrier)).when(princess).getEntitiesOwned();
        doReturn(List.of(enemyTagger, enemyLrmCarrier)).when(princess).getEnemyEntities();
    }

    @Test
    void enemyTagShotCountsTheEnemyLrmAndNotOurs() {
        List<WeaponMounted> supportingWeapons = princess.computeGuidedWeapons(enemyTagger,
              botTagger.getPosition());

        assertTrue(supportingWeapons.contains(enemyLrm), "the enemy's LRM supports the enemy's TAG");
        assertFalse(supportingWeapons.contains(botLrm), "our own LRM must not support the enemy's TAG");
    }

    @Test
    void ownTagShotCountsOurLrmAndNotTheEnemyLrm() {
        List<WeaponMounted> supportingWeapons = princess.computeGuidedWeapons(botTagger,
              enemyTagger.getPosition());

        assertTrue(supportingWeapons.contains(botLrm), "our LRM supports our TAG");
        assertFalse(supportingWeapons.contains(enemyLrm), "the enemy's LRM must not support our TAG");
    }

    private static WeaponMounted mockLrm() {
        WeaponMounted lrm = mock(WeaponMounted.class);
        when(lrm.getType()).thenReturn(lrmTwenty);
        return lrm;
    }

    private static Entity mockUnit(int id, Player owner, Coords position, WeaponMounted... weapons) {
        Entity unit = mock(Entity.class);
        when(unit.getId()).thenReturn(id);
        when(unit.getOwner()).thenReturn(owner);
        when(unit.getOwnerId()).thenReturn(owner.getId());
        when(unit.getPosition()).thenReturn(position);
        when(unit.getRunMP()).thenReturn(5);
        when(unit.getTotalWeaponList()).thenReturn(List.of(weapons));
        return unit;
    }

    private static void markEnemies(Entity unit, Entity... hostileUnits) {
        for (Entity hostileUnit : hostileUnits) {
            when(unit.isEnemyOf(hostileUnit)).thenReturn(true);
        }
    }
}
