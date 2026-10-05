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
package megamek.common.weapons.infantry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Vector;

import megamek.common.CalledShot;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.weapons.DamageType;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A non-penetrating infantry attack (needlers and the like) does no damage to anything but a conventional infantry
 * platoon. Example: Beast Infantry (Elephant) (Needler/SRM) hits an Agrotera for 5 damage split into three groupings;
 * the attack should say once that it cannot penetrate armor, not three times.
 */
class InfantryNonPenetratingReportTest {

    private static final int NON_PENETRATING_REPORT = 6051;

    private BipedMek mekTarget;
    private TWGameManager gameManager;
    private InfantryWeaponHandler handler;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws Exception {
        BipedMek attacker = mock(BipedMek.class);
        mekTarget = mock(BipedMek.class);
        Game game = mock(Game.class);
        gameManager = mock(TWGameManager.class);
        WeaponAttackAction action = mock(WeaponAttackAction.class);
        WeaponMounted weapon = mock(WeaponMounted.class);
        WeaponType weaponType = mock(WeaponType.class);

        GameOptions options = mock(GameOptions.class);
        doReturn(false).when(options).booleanOption(any(String.class));
        doReturn(options).when(game).getOptions();

        doReturn(1).when(attacker).getId();
        doReturn(attacker).when(game).getEntity(1);
        doReturn(attacker).when(attacker).getAttackingEntity();
        doReturn(new Coords(0, 0)).when(attacker).getPosition();
        doReturn(2).when(mekTarget).getId();
        doReturn(new Coords(1, 0)).when(mekTarget).getPosition();
        doReturn(Targetable.TYPE_ENTITY).when(mekTarget).getTargetType();
        doReturn(mekTarget).when(game).getTarget(Targetable.TYPE_ENTITY, 2);

        CalledShot calledShot = mock(CalledShot.class);
        doReturn(CalledShot.CALLED_NONE).when(calledShot).getCall();
        doReturn(calledShot).when(weapon).getCalledShot();
        doReturn(weaponType).when(weapon).getType();
        doReturn("InfantryNeedlerRifle").when(weaponType).getInternalName();
        doReturn(weapon).when(attacker).getEquipment(0);
        doReturn(1).when(action).getEntityId();
        doReturn(attacker).when(action).getEntity(game);
        doReturn(0).when(action).getWeaponId();
        when(attacker.getWeapon(0)).thenReturn(weapon);
        doReturn(Targetable.TYPE_ENTITY).when(action).getTargetType();
        doReturn(2).when(action).getTargetId();
        doReturn(Mek.LOC_NONE).when(action).getAimedLocation();

        handler = new InfantryWeaponHandler(mock(ToHitData.class), action, game, gameManager);
        setField(handler, "damageType", DamageType.NONPENETRATING);
    }

    private static void setField(Object object, String name, Object value) throws Exception {
        Class<?> type = object.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(object, value);
                return;
            } catch (NoSuchFieldException exception) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static int countReports(Vector<Report> reports, int messageId) {
        int count = 0;
        for (Report report : reports) {
            if (report.messageId == messageId) {
                count++;
            }
        }
        return count;
    }

    @Test
    @DisplayName("a non-penetrating attack on a Mek says it cannot penetrate armor once, over three groupings")
    void reportsOnceAcrossGroupings() throws Exception {
        Vector<Report> reports = new Vector<>();

        // The weapon handler calls this once per damage grouping, clearing firstHit after the first
        handler.handleEntityDamage(mekTarget, reports, null, 5, 2, 0);
        setField(handler, "firstHit", false);
        handler.handleEntityDamage(mekTarget, reports, null, 3, 2, 0);
        handler.handleEntityDamage(mekTarget, reports, null, 1, 2, 0);

        assertEquals(1, countReports(reports, NON_PENETRATING_REPORT),
              "The cannot-penetrate message should appear once for the whole attack");
        verify(gameManager, never()).damageEntity(any(), any(), anyInt(), anyBoolean(), any(DamageType.class),
              anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("only armored targets stop a non-penetrating attack; platoons still take it")
    void onlyArmoredTargetsStopTheAttack() throws Exception {
        assertTrue(handler.isStoppedByArmor(mekTarget), "A Mek stops a non-penetrating attack");
        assertFalse(handler.isStoppedByArmor(mock(ConvInfantry.class)),
              "A conventional infantry platoon still takes non-penetrating damage");

        setField(handler, "damageType", DamageType.NONE);
        assertFalse(handler.isStoppedByArmor(mekTarget), "An ordinary attack is not stopped");
    }
}
