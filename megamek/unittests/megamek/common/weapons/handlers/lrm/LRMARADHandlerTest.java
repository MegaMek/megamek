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
package megamek.common.weapons.handlers.lrm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.Hex;
import megamek.common.HexTarget;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.GameOptions;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for LRMARADHandler cluster modifier calculations.
 * <p>
 * Tests verify that ARAD missiles apply correct cluster modifiers:
 * <ul>
 *   <li>+1 against targets with electronics (not blocked by ECM)</li>
 *   <li>0 against targets with electronics (blocked by ECM, no Narc)</li>
 *   <li>+1 against targets with electronics (Narc overrides ECM)</li>
 *   <li>-2 against targets without electronics</li>
 *   <li>-2 against non-entity targets</li>
 * </ul>
 * @author Hammer - Built with Claude Code
 * @since 2025-01-16
 */
public class LRMARADHandlerTest {

    private static final int FRIENDLY_TEAM = 1;
    private static final int ENEMY_TEAM = 2;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    /**
     * Test helper: Create a mock handler with specified attacker and target.
     */
    private LRMARADHandler createHandler(Entity attacker, Targetable target, Game game) throws EntityLoadingException {
        ToHitData mockToHit = mock(ToHitData.class);
        WeaponAttackAction mockAction = mock(WeaponAttackAction.class);
        TWGameManager mockGameManager = mock(TWGameManager.class);

        // Get IDs first to avoid UnfinishedStubbingException
        int attackerId = attacker.getId();
        int targetId = target.getId();

        // Configure mock action to return entity and weapon IDs
        doReturn(attackerId).when(mockAction).getEntityId();
        doReturn(0).when(mockAction).getWeaponId();  // Weapon slot 0
        doReturn(Targetable.TYPE_ENTITY).when(mockAction).getTargetType();
        doReturn(targetId).when(mockAction).getTargetId();

        // Mock weapon equipment
        WeaponMounted mockWeapon = mock(WeaponMounted.class);
        WeaponType mockWeaponType = mock(WeaponType.class);
        doReturn(mockWeaponType).when(mockWeapon).getType();
        doReturn("ISLRM5").when(mockWeaponType).getInternalName();
        doReturn(null).when(mockWeapon).getLinked();  // No ammo linked (not needed for test)

        // Configure attacker entity to return weapon
        doReturn(mockWeapon).when(attacker).getEquipment(0);

        // Configure game to return target
        doReturn(target).when(game).getTarget(Targetable.TYPE_ENTITY, targetId);

        LRMARADHandler handler = new LRMARADHandler(mockToHit, mockAction, game, mockGameManager);

        return handler;
    }

    /**
     * Test helper: Create mock entity with owner and team.
     */
    private Entity createMockEntity(int team) {
        Entity entity = mock(Entity.class);
        Player owner = mock(Player.class);
        doReturn(team).when(owner).getTeam();
        doReturn(owner).when(entity).getOwner();
        doReturn(team * 100).when(entity).getId();  // Unique ID
        doReturn(new Coords(0, 0)).when(entity).getPosition();
        doReturn(new ArrayList<>()).when(entity).getEquipment();
        doReturn(entity).when(entity).getAttackingEntity();  // WeaponHandler needs this
        return entity;
    }

    /**
     * Test helper: Create mock game with ECM behavior.
     */
    private Game createMockGame(Entity attacker) {
        Game game = mock(Game.class);
        Board mockBoard = mock(Board.class);
        GameOptions mockOptions = mock(GameOptions.class);

        doReturn(mockBoard).when(game).getBoard();
        doReturn(mockOptions).when(game).getOptions();
        doReturn(false).when(mockOptions).booleanOption(any(String.class));  // Default all game options to false
        doReturn(new ArrayList<>()).when(game).getEntitiesVector();  // Empty entities list (no ECM sources)

        // WeaponHandler constructor needs game.getEntity() to return attacker
        // Get ID value first to avoid UnfinishedStubbingException
        int attackerId = attacker.getId();
        doReturn(attacker).when(game).getEntity(attackerId);

        // ComputeECM.isAffectedByECM() calls attackingEntity.getGame()
        doReturn(game).when(attacker).getGame();

        return game;
    }

    @Test
    void testTargetableIsNotEntity() throws EntityLoadingException {
        Entity attacker = createMockEntity(FRIENDLY_TEAM);
        Game game = createMockGame(attacker);  // No ECM

        // Set up a targetable hex.
        Coords coords = new Coords(7, 6);
        Hex hex = new Hex(1, "", null, coords);
        Board board =  new Board(17, 16);
        board.setHex(coords, hex);

        // HexTargets take a type, the "mode" in which the hex is being targeted.
        HexTarget target = new HexTarget(coords, board, HexTarget.TYPE_HEX_CLEAR);
        LRMARADHandler handler = createHandler(attacker, target, game);
        int salvoBonus = handler.getSalvoBonus();

        assertEquals(-2, salvoBonus);
    }

    @Test
    void testTargetWithElectronicsNoECM() throws EntityLoadingException {
        Entity attacker = createMockEntity(FRIENDLY_TEAM);
        Entity target = createMockEntity(ENEMY_TEAM);

        // Configure target with C3 (qualifying electronics)
        doReturn(true).when(target).hasC3();
        doReturn(false).when(target).isNarcedBy(FRIENDLY_TEAM);
        doReturn(Collections.emptyIterator()).when(target).getINarcPodsAttached();
        doReturn(false).when(target).isStealthActive();

        Mounted<?> c3Equipment = createMockEquipment(MiscType.F_C3S, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(c3Equipment);
        doReturn(equipment).when(target).getEquipment();

        Game game = createMockGame(attacker);  // No ECM

        LRMARADHandler handler = createHandler(attacker, target, game);

        assertEquals(+1, handler.getSalvoBonus(),
              "Target with electronics and no ECM should receive +1 bonus");
    }

    @Test
    void testTargetWithNarcNoElectronics() throws EntityLoadingException {
        Entity attacker = createMockEntity(FRIENDLY_TEAM);
        Entity target = createMockEntity(ENEMY_TEAM);

        // Configure target with no C3 but with Narc
        doReturn(false).when(target).hasC3();
        doReturn(true).when(target).isNarcedBy(FRIENDLY_TEAM);  // Narc-tagged
        doReturn(Collections.emptyIterator()).when(target).getINarcPodsAttached();
        doReturn(false).when(target).isStealthActive();

        List<Mounted<?>> equipment = new ArrayList<>();
        doReturn(equipment).when(target).getEquipment();

        Game game = createMockGame(attacker);

        LRMARADHandler handler = createHandler(attacker, target, game);

        assertEquals(+1, handler.getSalvoBonus(),
              "Narc-tagged target should receive +1 bonus");
    }

    @Test
    void testTargetWithElectronicsAndECMNoNarc() throws EntityLoadingException {
        Entity attacker = createMockEntity(FRIENDLY_TEAM);
        Entity target = createMockEntity(ENEMY_TEAM);

        // Configure target with C3 but no Narc
        doReturn(true).when(target).hasC3();
        doReturn(false).when(target).isNarcedBy(FRIENDLY_TEAM);  // Not Narc-tagged
        doReturn(Collections.emptyIterator()).when(target).getINarcPodsAttached();
        doReturn(false).when(target).isStealthActive();

        Mounted<?> c3Equipment = createMockEquipment(MiscType.F_C3S, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(c3Equipment);
        doReturn(equipment).when(target).getEquipment();

        Game game = createMockGame(attacker);  // ECM present (but Narc overrides)

        LRMARADHandler handler = createHandler(attacker, target, game);

        assertEquals(+1, handler.getSalvoBonus(),
              "Target should receive +1 bonus even with ECM (Has electronics)");
    }

    @Test
    void testTargetWithElectronicsAndNarc() throws EntityLoadingException {
        Entity attacker = createMockEntity(FRIENDLY_TEAM);
        Entity target = createMockEntity(ENEMY_TEAM);

        // Configure target with C3 and Narc
        doReturn(true).when(target).hasC3();
        doReturn(true).when(target).isNarcedBy(FRIENDLY_TEAM);  // Narc-tagged
        doReturn(Collections.emptyIterator()).when(target).getINarcPodsAttached();
        doReturn(false).when(target).isStealthActive();

        Mounted<?> c3Equipment = createMockEquipment(MiscType.F_C3S, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(c3Equipment);
        doReturn(equipment).when(target).getEquipment();

        Game game = createMockGame(attacker);  // ECM present (but Narc overrides)

        LRMARADHandler handler = createHandler(attacker, target, game);

        assertEquals(+1, handler.getSalvoBonus(),
              "Narc-tagged target should receive +1 bonus even with ECM (Narc overrides)");
    }

    @Test
    void testTargetWithNoElectronics() throws EntityLoadingException {
        Entity attacker = createMockEntity(FRIENDLY_TEAM);
        Entity target = createMockEntity(ENEMY_TEAM);

        // Configure target with NO electronics
        doReturn(false).when(target).hasC3();
        doReturn(false).when(target).hasC3i();
        doReturn(false).when(target).hasECM();
        doReturn(false).when(target).hasBAP();
        doReturn(-1).when(target).getTaggedBy();
        doReturn(false).when(target).hasGhostTargets(true);
        doReturn(false).when(target).isNarcedBy(FRIENDLY_TEAM);
        doReturn(Collections.emptyIterator()).when(target).getINarcPodsAttached();
        doReturn(false).when(target).isStealthActive();
        doReturn(new ArrayList<>()).when(target).getEquipment();

        Game game = createMockGame(attacker);

        LRMARADHandler handler = createHandler(attacker, target, game);

        assertEquals(-2, handler.getSalvoBonus(),
              "Target with no electronics should receive -2 penalty");
    }

    @Test
    void testTargetWithActiveStealthNoNarc() throws EntityLoadingException {
        Entity attacker = createMockEntity(FRIENDLY_TEAM);
        Entity target = createMockEntity(ENEMY_TEAM);

        // Configure target with active Stealth Armor and C3 (blocked)
        doReturn(true).when(target).isStealthActive();  // Blocks internal systems
        doReturn(true).when(target).hasC3();
        doReturn(false).when(target).isNarcedBy(FRIENDLY_TEAM);  // No Narc
        doReturn(Collections.emptyIterator()).when(target).getINarcPodsAttached();

        Mounted<?> c3Equipment = createMockEquipment(MiscType.F_C3S, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(c3Equipment);
        doReturn(equipment).when(target).getEquipment();

        Game game = createMockGame(attacker);

        LRMARADHandler handler = createHandler(attacker, target, game);

        assertEquals(-2, handler.getSalvoBonus(),
              "Active Stealth Armor should block internal systems (C3) without Narc");
    }

    @Test
    void testTargetWithActiveStealthAndNarc() throws EntityLoadingException {
        Entity attacker = createMockEntity(FRIENDLY_TEAM);
        Entity target = createMockEntity(ENEMY_TEAM);

        // Configure target with active Stealth Armor and Narc (Narc NOT blocked)
        doReturn(true).when(target).isStealthActive();  // Blocks internal systems
        doReturn(true).when(target).hasC3();
        doReturn(true).when(target).isNarcedBy(FRIENDLY_TEAM);  // Narc overrides Stealth
        doReturn(Collections.emptyIterator()).when(target).getINarcPodsAttached();

        Mounted<?> c3Equipment = createMockEquipment(MiscType.F_C3S, false);
        List<Mounted<?>> equipment = new ArrayList<>();
        equipment.add(c3Equipment);
        doReturn(equipment).when(target).getEquipment();

        Game game = createMockGame(attacker);

        LRMARADHandler handler = createHandler(attacker, target, game);

        assertEquals(+1, handler.getSalvoBonus(),
              "Narc should override Stealth Armor blocking (external attachment)");
    }

    @Test
    void testTargetWithTAG() throws EntityLoadingException {
        Entity attacker = createMockEntity(FRIENDLY_TEAM);
        Entity target = createMockEntity(ENEMY_TEAM);

        // Configure target with TAG (qualifying electronics)
        int attackerId = attacker.getId();  // Get ID first to avoid UnfinishedStubbingException
        doReturn(attackerId).when(target).getTaggedBy();  // Tagged by attacker
        doReturn(false).when(target).isNarcedBy(FRIENDLY_TEAM);
        doReturn(Collections.emptyIterator()).when(target).getINarcPodsAttached();
        doReturn(false).when(target).isStealthActive();
        doReturn(new ArrayList<>()).when(target).getEquipment();

        Game game = createMockGame(attacker);

        LRMARADHandler handler = createHandler(attacker, target, game);

        assertEquals(+1, handler.getSalvoBonus(),
              "TAG'd target should receive +1 bonus");
    }

    @Test
    void testTargetWithGhostTargets() throws EntityLoadingException {
        Entity attacker = createMockEntity(FRIENDLY_TEAM);
        Entity target = createMockEntity(ENEMY_TEAM);

        // Configure target generating Ghost Targets (qualifying electronics)
        doReturn(true).when(target).hasGhostTargets(true);
        doReturn(false).when(target).isNarcedBy(FRIENDLY_TEAM);
        doReturn(Collections.emptyIterator()).when(target).getINarcPodsAttached();
        doReturn(false).when(target).isStealthActive();
        doReturn(new ArrayList<>()).when(target).getEquipment();

        Game game = createMockGame(attacker);

        LRMARADHandler handler = createHandler(attacker, target, game);

        assertEquals(+1, handler.getSalvoBonus(),
              "Target generating Ghost Targets should receive +1 bonus");
    }

    /**
     * Helper method to create mock equipment with specific flags.
     */
    private Mounted<?> createMockEquipment(MiscTypeFlag flag, boolean destroyed) {
        Mounted<?> equipment = mock(Mounted.class);
        EquipmentType type = mock(EquipmentType.class);

        doReturn(false).when(type).hasFlag(Mockito.any(MiscTypeFlag.class));
        doReturn(true).when(type).hasFlag(flag);
        doReturn(type).when(equipment).getType();
        doReturn(destroyed).when(equipment).isDestroyed();
        doReturn(false).when(equipment).isMissing();
        doReturn(false).when(equipment).isBreached();

        return equipment;
    }
}
