/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for MissileWeaponHandler.
 * <p>
 * Test to verify that with Playtest 3, AMS can engage twice
 *
 * @since 2025-12-14
 */
class MissileWeaponHandlerTest {

    private BattleArmor mockAttacker;
    private Game mockGame;
    private TWGameManager mockGameManager;
    private ToHitData mockToHit;
    private WeaponAttackAction mockAction;
    private Mek mockMekTarget;
    private Coords targetCoords;
    private HitData mockHitData;
    private WeaponMounted mockWeapon;
    private WeaponMounted mockWeaponTwo;
    private WeaponType mockWeaponType;
    private WeaponType mockAMSWeaponType;
    private boolean mockPlaytest3;
    private WeaponMounted mockAMS;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        mockAttacker = mock(BattleArmor.class);
        mockGame = mock(Game.class);
        mockGameManager = mock(TWGameManager.class);
        mockToHit = mock(ToHitData.class);
        mockAction = mock(WeaponAttackAction.class);
        mockMekTarget = mock(BipedMek.class);
        targetCoords = new Coords(5, 5);
        mockHitData = new HitData(Mek.LOC_CENTER_TORSO, false);
        mockAMS = mock(WeaponMounted);
        mockPlaytest3 = true;

        // Configure basic mocks
        Board mockBoard = mock(Board.class);
        GameOptions mockOptions = mock(GameOptions.class);
        Hex mockHex = mock(Hex.class);

        doReturn(mockBoard).when(mockGame).getBoard();
        doReturn(mockOptions).when(mockGame).getOptions();
        doReturn(mockPlaytest3).when(mockGame).getOptions().booleanOption(OptionsConstants.PLAYTEST_3);
        doReturn(false).when(mockOptions).booleanOption(any(String.class));
        doReturn(mockHex).when(mockGame).getHexOf(any(Entity.class));
        doReturn(mockHex).when(mockGame).getHexOf(any(Targetable.class));

        // Configure attacker as both weapon entity and attacking entity
        doReturn(1).when(mockAttacker).getId();
        doReturn(mockAttacker).when(mockGame).getEntity(1);
        doReturn(mockGame).when(mockAttacker).getGame();
        doReturn(targetCoords).when(mockAttacker).getPosition();
        doReturn(mockAttacker).when(mockAttacker).getAttackingEntity();

        // Configure weapon
        mockWeapon = mock(WeaponMounted.class);
        mockWeaponType = mock(WeaponType.class);

        doReturn(mockWeaponType).when(mockWeapon).getType();
        doReturn("ISLRM20").when(mockWeaponType).getInternalName();
        doReturn(null).when(mockWeapon).getLinked();
        doReturn(false).when(mockWeapon).hasModes();
        doReturn(mockWeapon).when(mockAttacker).getEquipment(0);

        doReturn(mockWeaponType).when(mockWeaponTwo).getType();
        doReturn(mockWeaponTwo).when(mockAttacker).getEquipment(1);

        // Configure action
        doReturn(1).when(mockAction).getEntityId();
        doReturn(0).when(mockAction).getWeaponId();
        doReturn(mockAttacker).when(mockAction).getEntity(mockGame);
        doReturn(Targetable.TYPE_ENTITY).when(mockAction).getTargetType();
        doReturn(2).when(mockAction).getTargetId();
        doReturn(Mek.LOC_NONE).when(mockAction).getAimedLocation();
        doReturn(AimingMode.NONE).when(mockAction).getAimingMode();

        // Configure Mek target
        doReturn(2).when(mockMekTarget).getId();
        doReturn(targetCoords).when(mockMekTarget).getPosition();
        doReturn(mockHitData).when(mockMekTarget).rollHitLocation(anyInt(), anyInt(), anyInt(), any(), anyInt());
        doReturn(mockHitData).when(mockMekTarget).rollHitLocation(anyInt(), anyInt());
        doReturn("CT").when(mockMekTarget).getLocationAbbr(any(HitData.class));
        doReturn(Targetable.TYPE_ENTITY).when(mockMekTarget).getTargetType();
        // doReturn(mockAMS).when(mockAction).getCounterEquipment();
        doReturn(false).when(mockMekTarget).removePartialCoverHits(anyInt(), anyInt(), anyInt());

        // Configure toHit
        doReturn(TargetRoll.AUTOMATIC_SUCCESS).when(mockToHit).getValue();
        doReturn(0).when(mockToHit).getCover();

        // Configure gameManager to return empty reports
        doReturn(new Vector<Report>()).when(mockGameManager).damageEntity(any(), any(), anyInt());
        doReturn(new Vector<Report>()).when(mockGameManager).damageEntity(
              any(),
              any(),
              anyInt(),
              anyBoolean(),
              any(),
              anyBoolean(),
              anyBoolean(),
              anyBoolean(),
              anyBoolean(),
              anyBoolean());
        doReturn(new Vector<Report>()).when(mockGameManager).criticalEntity(
              any(Entity.class), anyInt(), anyBoolean(), anyInt(), anyInt());
    }

    /**
     * Test that zero-damage swarm attacks against Meks still roll for critical hits. Per BattleTech rules, "critical
     * hits are a separate outcome from damage".
     */
    @Test
    void testAMSWorksForPlaytestThreek() throws EntityLoadingException {

        // Configure target
        doReturn(mockMekTarget).when(mockGame).getTarget(Targetable.TYPE_ENTITY, 2);
        
        MissileWeaponHandler handler = new MissileWeaponHandler(mockToHit, mockAction, mockGame, mockGameManager);

        Vector<Report> reports = new Vector<>();

        Mounted<?> amsEquipment = createMockAMS();
        List<Mounted<?>> AMSweapons = new ArrayList<>();
        AMSweapons.add(amsEquipment);
        doReturn(AMSweapons).when(mockAction).getCounterEquipment();
        
        doReturn(false).when(handler).multiAMS;
        doReturn(false).when(handler).amsEngaged;
        
        // Call handle for getAMSHitsMod.
        int AMSmod = handler.getAMSHitsMod(reports);
        
        // Verify AMS was called twice.
        verify();
    }

    private Mounted<?> createMockAMS() {
        Mounted<?> weaponMounted = mock(WeaponMounted.class);
        mockAMSWeaponType = mock(WeaponType.class);

        doReturn("ISAntiMissileSystem").when(mockAMSWeaponType).getInternalName();
        doReturn(null).when(mockAMS).getLinked();
        doReturn(false).when(mockAMS).hasModes();
        doReturn(true).when(mockAMSWeaponType).hasFlag(WeaponType.F_AMS);
        doReturn(true).when(mockAMS).isReady();
        doReturn(mockAMSWeaponType).when(WeaponMounted).getType();
        doReturn(mockAMS).when(WeaponMounted).getEquipment(0);

        return weaponMounted;
        

    }
}
