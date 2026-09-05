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
package megamek.common.weapons.handlers.lrm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HexTarget;
import megamek.common.Player;
import megamek.common.TagInfo;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.GameOptions;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the Semi-Guided cluster modifier in {@link LRMHandler}.
 * <p>
 * A Semi-Guided salvo aimed at a hex or a building used to read the TAG designation straight off the target unit, which
 * does not exist for those targets. That crashed the server in the middle of resolving the firing phase and left the
 * game unable to advance (issue #8723). The designation is now looked up through the game, which knows about TAG'd
 * hexes and buildings as well as TAG'd units.
 * </p>
 *
 * @author Hammer - Built with Claude Code
 * @since 2026-08-16
 */
public class LRMHandlerSemiGuidedTest {

    private static final int FRIENDLY_TEAM = 1;
    private static final int TAGGING_UNIT_ID = 42;

    private final RulesManager originalRulesManager = Game.rulesManager;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @AfterEach
    void restoreRulesManager() {
        Game.rulesManager = originalRulesManager;
    }

    @Test
    void untaggedHexTargetUnderCoreRulesLosesOneMissile() throws EntityLoadingException {
        Game.rulesManager = new CoreRulesManager();
        Entity attacker = createMockEntity();
        Game game = createMockGame(attacker);
        HexTarget target = createHexTarget();
        doReturn(new Vector<TagInfo>()).when(game).getTagInfo();

        LRMHandler handler = createHandler(attacker, target, game);

        assertEquals(-1, handler.getSemiGuidedClusterModifier(),
              "Semi-Guided missiles fired at a hex nobody has designated should lose a missile, not crash");
    }

    @Test
    void taggedHexTargetUnderCoreRulesGainsTwoMissiles() throws EntityLoadingException {
        Game.rulesManager = new CoreRulesManager();
        Entity attacker = createMockEntity();
        Game game = createMockGame(attacker);
        HexTarget target = createHexTarget();
        Vector<TagInfo> tagInfo = new Vector<>();
        tagInfo.add(new TagInfo(TAGGING_UNIT_ID, target.getTargetType(), target, false));
        doReturn(tagInfo).when(game).getTagInfo();

        LRMHandler handler = createHandler(attacker, target, game);

        assertEquals(+2, handler.getSemiGuidedClusterModifier(),
              "A hex designated by TAG should give the Core Rules Semi-Guided bonus");
    }

    @Test
    void hexTargetUnderTotalWarfareRulesIsUnmodified() throws EntityLoadingException {
        Game.rulesManager = new TWRulesManager();
        Entity attacker = createMockEntity();
        Game game = createMockGame(attacker);
        HexTarget target = createHexTarget();
        doReturn(new Vector<TagInfo>()).when(game).getTagInfo();

        LRMHandler handler = createHandler(attacker, target, game);

        assertEquals(0, handler.getSemiGuidedClusterModifier(),
              "Total Warfare Semi-Guided ammunition improves the to-hit roll, not the number of missiles");
    }

    @Test
    void taggedUnitTargetUnderCoreRulesGainsTwoMissiles() throws EntityLoadingException {
        Game.rulesManager = new CoreRulesManager();
        Entity attacker = createMockEntity();
        Game game = createMockGame(attacker);
        Entity target = createMockEntity();
        doReturn(200).when(target).getId();
        doReturn(TAGGING_UNIT_ID).when(target).getTaggedBy();

        LRMHandler handler = createHandler(attacker, target, game);

        assertEquals(+2, handler.getSemiGuidedClusterModifier(),
              "A unit designated by TAG should give the Core Rules Semi-Guided bonus");
    }

    /**
     * Test helper: builds a handler for an LRM launcher loaded with Semi-Guided ammunition, firing directly at the
     * given target.
     */
    private LRMHandler createHandler(Entity attacker, Targetable target, Game game) throws EntityLoadingException {
        ToHitData toHit = mock(ToHitData.class);
        WeaponAttackAction attackAction = mock(WeaponAttackAction.class);
        TWGameManager gameManager = mock(TWGameManager.class);

        // Read the identifiers up front to avoid Mockito's UnfinishedStubbingException
        int attackerId = attacker.getId();
        int targetId = target.getId();
        int targetType = target.getTargetType();

        doReturn(attackerId).when(attackAction).getEntityId();
        doReturn(0).when(attackAction).getWeaponId();
        doReturn(targetType).when(attackAction).getTargetType();
        doReturn(targetId).when(attackAction).getTargetId();
        doReturn("").when(toHit).getDesc();

        AmmoType ammoType = mock(AmmoType.class);
        doReturn(AmmoType.AmmoTypeEnum.LRM).when(ammoType).getAmmoType();
        doReturn(EnumSet.of(AmmoType.Munitions.M_SEMIGUIDED)).when(ammoType).getMunitionType();
        AmmoMounted ammo = mock(AmmoMounted.class);
        doReturn(ammoType).when(ammo).getType();
        doReturn(12).when(ammo).getUsableShotsLeft();
        doReturn(12).when(ammo).getBaseShotsLeft();

        WeaponType weaponType = mock(WeaponType.class);
        doReturn("ISLRM5").when(weaponType).getInternalName();
        doReturn(5).when(weaponType).getRackSize();
        WeaponMounted weapon = mock(WeaponMounted.class);
        doReturn(weaponType).when(weapon).getType();
        doReturn(ammo).when(weapon).getLinked();
        doReturn(1).when(weapon).getNWeapons();
        doReturn(1).when(weapon).getCurrentShots();
        doReturn(EquipmentMode.getMode("")).when(weapon).curMode();

        doReturn(weapon).when(attacker).getEquipment(0);
        doReturn(target).when(game).getTarget(targetType, targetId);

        return new LRMHandler(toHit, attackAction, game, gameManager);
    }

    /**
     * Test helper: a hex targeted for clearing, which resolves to no target unit at all.
     */
    private HexTarget createHexTarget() {
        Coords coords = new Coords(7, 6);
        Board board = new Board(17, 16);
        board.setHex(coords, new Hex(1, "", null, coords));
        return new HexTarget(coords, board, HexTarget.TYPE_HEX_CLEAR);
    }

    /**
     * Test helper: a mock unit with just enough state to build a weapon handler.
     */
    private Entity createMockEntity() {
        Entity entity = mock(Entity.class);
        Player owner = mock(Player.class);
        doReturn(FRIENDLY_TEAM).when(owner).getTeam();
        doReturn(owner).when(entity).getOwner();
        doReturn(100).when(entity).getId();
        doReturn(new Coords(0, 0)).when(entity).getPosition();
        doReturn(new ArrayList<>()).when(entity).getEquipment();
        doReturn(entity).when(entity).getAttackingEntity();
        return entity;
    }

    /**
     * Test helper: a mock game with all optional rules switched off.
     */
    private Game createMockGame(Entity attacker) {
        Game game = mock(Game.class);
        Board board = mock(Board.class);
        GameOptions options = mock(GameOptions.class);

        doReturn(board).when(game).getBoard();
        doReturn(options).when(game).getOptions();
        doReturn(false).when(options).booleanOption(any(String.class));
        doReturn(new ArrayList<>()).when(game).getEntitiesVector();

        int attackerId = attacker.getId();
        doReturn(attacker).when(game).getEntity(attackerId);
        doReturn(game).when(attacker).getGame();

        return game;
    }
}
