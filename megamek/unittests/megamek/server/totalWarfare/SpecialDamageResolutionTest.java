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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Vector;

import megamek.common.DamageInfo;
import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.HitDamageType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.IArmorState;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.rules.RulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.weapons.DamageType;
import megamek.common.weapons.TaserEffects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SpecialDamageResolutionTest {
    private RulesManager previousRules;
    private TWGameManager manager;
    private TWDamageManager damageManager;
    private Game game;
    private Mek mek;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        previousRules = Game.rulesManager;
        manager = spy(new TWGameManager());
        doNothing().when(manager).send(any(Packet.class));
        // Inspect the generated critical checks without letting random equipment damage affect the result.
        doReturn(new Vector<Report>()).when(manager).criticalEntity(any(Entity.class), anyInt(), anyBoolean(),
              anyInt(), anyInt(), any(DamageType.class));
        game = manager.getGame();
        damageManager = new TWDamageManager(manager, game);
        game.setBoard(new Board(1, 1, new Hex[] { new Hex() }));
        Player owner = new Player(1, "Test");
        game.addPlayer(1, owner);
        mek = new BipedMek();
        mek.setWeight(50);
        mek.setOwner(owner);
        mek.setId(1);
        mek.setAutoEject(false);
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeInternal(15, location);
            mek.initializeArmor(10, location);
            if (mek.hasRearArmor(location)) {
                mek.initializeRearArmor(10, location);
            }
        }
        game.addEntity(mek);
        mek.setPosition(new Coords(0, 0));
    }

    @AfterEach
    void restoreRules() {
        Game.rulesManager = previousRules;
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void caseIIStopsExplosionTransferFromAnAlreadyDestroyedTorso(boolean core) throws LocationFullException {
        game.initializeRulesManager(core ? OptionsConstants.RULES_CORE : OptionsConstants.RULES_TW);
        mek.addEquipment(EquipmentType.get("ISCASEII"), Mek.LOC_LEFT_TORSO);
        mek.setInternal(IArmorState.ARMOR_DOOMED, Mek.LOC_LEFT_TORSO);
        damageManager.damageEntity(new DamageInfo(mek, new HitData(Mek.LOC_LEFT_TORSO), 12, true,
              DamageType.NONE, false));
        assertEquals(15, mek.getInternal(Mek.LOC_CENTER_TORSO));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void reengineeredLaserTransfersAllDamageInExcessOfHardenedArmor(boolean core) {
        game.initializeRulesManager(core ? OptionsConstants.RULES_CORE : OptionsConstants.RULES_TW);
        int arm = Mek.LOC_LEFT_ARM;
        mek.setArmorType(EquipmentType.T_ARMOR_HARDENED, arm);
        mek.initializeArmor(3, arm);
        HitData hit = new HitData(arm);
        hit.setGeneralDamageType(HitDamageType.DAMAGE_IGNORES_DMG_REDUCTION);
        damageManager.damageEntity(new DamageInfo(mek, hit, 9));
        assertEquals(9, mek.getInternal(arm));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void reengineeredLaserThroughArmorCriticalTreatsHardenedAsStandard(boolean core) {
        game.initializeRulesManager(core ? OptionsConstants.RULES_CORE : OptionsConstants.RULES_TW);
        int arm = Mek.LOC_LEFT_ARM;
        mek.setArmorType(EquipmentType.T_ARMOR_HARDENED, arm);
        mek.initializeArmor(7, arm);
        HitData hit = new HitData(arm, false, HitData.EFFECT_TAC);
        hit.setGeneralDamageType(HitDamageType.DAMAGE_IGNORES_DMG_REDUCTION);
        damageManager.damageEntity(new DamageInfo(mek, hit, 7));
        verify(manager).criticalEntity(eq(mek), eq(arm), eq(false), eq(0), anyInt(), any(DamageType.class));
    }

    @ParameterizedTest
    @CsvSource({ "true, 1, 0, 14, 0", "false, 1, 0, 15, 0", "true, 2, 0, 12, 0",
          "false, 2, 0, 13, 0", "true, 1, 4, 15, 4", "true, 2, 4, 15, 3" })
    void excessCompositeDamageDependsOnTheRulesAndTheReceivingFacing(boolean core, int damage, int torsoArmor,
          int expectedInternal, int expectedArmor) {
        game.initializeRulesManager(core ? OptionsConstants.RULES_CORE : OptionsConstants.RULES_TW);
        mek.setStructureType(EquipmentType.T_STRUCTURE_COMPOSITE);
        mek.initializeInternal(1, Mek.LOC_LEFT_ARM);
        mek.initializeArmor(0, Mek.LOC_LEFT_ARM);
        mek.initializeArmor(torsoArmor, Mek.LOC_LEFT_TORSO);
        damageManager.damageEntity(new DamageInfo(mek, new HitData(Mek.LOC_LEFT_ARM), damage));
        assertEquals(expectedInternal, mek.getInternal(Mek.LOC_LEFT_TORSO));
        assertEquals(expectedArmor, mek.getArmor(Mek.LOC_LEFT_TORSO));
    }

    @ParameterizedTest
    @CsvSource({ "MEK, 0", "MEK, 10", "BATTLE_ARMOR, 0", "BATTLE_ARMOR, 10" })
    void taserHitRemovesExactlyOneInfantryTrooperInTheOpen(TaserEffects taser, int armor) {
        game.initializeRulesManager(OptionsConstants.RULES_TW);
        ConvInfantry platoon = new ConvInfantry();
        platoon.setOwner(mek.getOwner());
        platoon.setId(2);
        platoon.setSquadSize(10);
        platoon.setSquadCount(1);
        platoon.initializeInternal(10, ConvInfantry.LOC_INFANTRY);
        platoon.initializeArmor(armor, ConvInfantry.LOC_INFANTRY);
        game.addEntity(platoon);
        platoon.setPosition(mek.getPosition());
        HitData hit = new HitData(ConvInfantry.LOC_INFANTRY);
        taser.resolve(manager, platoon, hit, 1,
              points -> damageManager.damageEntity(new DamageInfo(platoon, hit, points)), new Vector<>());
        assertEquals(9, platoon.getInternal(hit));
        assertEquals(armor, platoon.getArmor(hit));
    }

    @ParameterizedTest
    @CsvSource({ "false, 1, 14", "false, 2, 12", "true, 1, 14", "true, 2, 13" })
    void compositeExplosionCanShareAPointBehindArmorAndRespectsReceivingCaseII(boolean receivingCaseII,
          int explosionDamage, int expectedTorsoInternal) throws LocationFullException {
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
        mek.setStructureType(EquipmentType.T_STRUCTURE_COMPOSITE);
        mek.initializeInternal(1, Mek.LOC_LEFT_ARM);
        if (receivingCaseII) {
            mek.addEquipment(EquipmentType.get("ISCASEII"), Mek.LOC_LEFT_TORSO);
        }
        damageManager.damageEntity(new DamageInfo(mek, new HitData(Mek.LOC_LEFT_ARM), explosionDamage, true,
              DamageType.NONE, false));
        assertEquals(expectedTorsoInternal, mek.getInternal(Mek.LOC_LEFT_TORSO));
        assertEquals(10, mek.getArmor(Mek.LOC_LEFT_TORSO));
    }
}
