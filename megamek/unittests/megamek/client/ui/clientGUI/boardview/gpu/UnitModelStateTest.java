/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.client.ui.tileset.UnitModelEquipment;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.icons.Camouflage;
import megamek.common.options.OptionsConstants;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.common.units.ProneCause;
import megamek.common.units.Tank;
import org.junit.jupiter.api.Test;

class UnitModelStateTest {
    @Test
    void generatedFighterGroupsRetainLogicalIdentityWithoutAddingGunGeometry() throws Exception {
        var fighter = new AeroSpaceFighter();
        var game = new Game();
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_CAPITAL_FIGHTER).setValue(true);
        fighter.setGame(game);
        var gun = fighter.addEquipment(EquipmentType.get("ISMediumLaser"), AeroSpaceFighter.LOC_NOSE);
        fighter.updateWeaponGroups();
        var group = fighter.getWeaponGroupList().getFirst();
        var captured = UnitModelState.capture(fighter).structure().equipment();
        assertEquals(List.of(gun.getEquipmentNum()), captured.stream().filter(mount -> mount.policy().allowsFallback())
              .map(UnitModelEquipment.Mount::index).toList());
        assertEquals(EquipmentModelPolicy.MEMBERS, captured.stream().filter(mount -> mount.index() == group.getEquipmentNum())
              .findFirst().orElseThrow().policy());
        group.setNWeapons(0);
        group.setDestroyed(true);
        assertEquals(1, UnitModelState.capture(fighter).structure().equipment().stream()
              .filter(mount -> mount.policy().allowsFallback()).count());
        assertEquals(2, fighter.getEquipment().size(), "Presentation must retain the game's logical group");
    }

    @Test
    void automaticRulesSearchlightsDoNotCreateLampGeometry() throws Exception {
        for (var entity : List.of(new BipedMek(), new Tank())) {
            assertTrue(entity.getsAutoExternalSearchlight());
            // Match the server's game-start state, which was missing from the original bare-body review.
            entity.setExternalSearchlight(true);
            entity.setSearchlightState(true);
            var automatic = UnitModelState.capture(entity);
            assertFalse(automatic.structure().externalSearchlight());
            assertTrue(automatic.appearance().searchlightOn());
            assertTrue(entity.hasExternalSearchlight(), "Capturing art must not remove the rules capability");

            var lamp = entity.addEquipment(EquipmentType.get("Searchlight"), entity instanceof Mek ? Mek.LOC_CENTER_TORSO : Tank.LOC_FRONT);
            var mounted = UnitModelState.capture(entity);
            assertFalse(mounted.structure().externalSearchlight(), "A mounted lamp must not create a second external housing");
            assertEquals(List.of(UnitModelEquipment.describe(entity, lamp)), mounted.structure().equipment());

            entity.getQuirks().getOption(OptionsConstants.QUIRK_POS_SEARCHLIGHT).setValue(true);
            var game = new Game();
            game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(false);
            entity.setGame(game);
            assertFalse(entity.hasQuirk(OptionsConstants.QUIRK_POS_SEARCHLIGHT));
            assertTrue(UnitModelState.capture(entity).structure().externalSearchlight(),
                  "The design's lamp remains physical hardware when quirk effects are disabled");
            entity.setExternalSearchlight(false);
            assertFalse(UnitModelState.capture(entity).structure().externalSearchlight(), "A lost external lamp stays removed");
        }
    }

    @Test
    void explicitExternalSearchlightsOnOtherFamiliesRemainVisible() {
        var aero = new AeroSpaceFighter();
        assertFalse(aero.getsAutoExternalSearchlight());
        assertFalse(UnitModelState.capture(aero).structure().externalSearchlight());
        aero.setExternalSearchlight(true);
        assertTrue(UnitModelState.capture(aero).structure().externalSearchlight());
        aero.setExternalSearchlight(false);
        assertFalse(UnitModelState.capture(aero).structure().externalSearchlight());
    }

    @Test
    void duplicateWeaponsHaveStableIdentityAndPoseDamageCamoDoNotChangeStructure() throws Exception {
        var mek = new BipedMek();
        var left = mek.addEquipment(EquipmentType.get("PPC"), Mek.LOC_LEFT_ARM);
        var right = mek.addEquipment(EquipmentType.get("PPC"), Mek.LOC_RIGHT_ARM, true);
        var camo = new Camouflage("example", "paint.png");
        mek.setCamouflage(camo);
        var before = UnitModelState.capture(mek);
        var mounts = before.structure().equipment();
        assertEquals(2, mounts.size());
        assertNotEquals(mounts.get(0).index(), mounts.get(1).index());
        assertEquals(UnitModelEquipment.describe(mek, right), mounts.get(1));
        assertEquals("RA", mounts.get(1).location());
        assertEquals(true, mounts.get(1).rear());
        mek.setSecondaryFacing(1);
        mek.setProne(ProneCause.FORCED);
        left.setDestroyed(true);
        camo.setRotationAngle(90);
        var after = UnitModelState.capture(mek);
        assertEquals(before.structure(), after.structure());
        assertNotEquals(before.pose(), after.pose());
        assertNotEquals(before.appearance(), after.appearance());
        assertEquals(0, before.appearance().camo().rotation());
        assertEquals(ProneCause.NONE, before.pose().proneCause());
        assertThrows(UnsupportedOperationException.class, () -> mounts.clear());
    }

    @Test
    void battleArmorMembershipTracksCasualtiesWithoutReassigningSurvivors() {
        var armor = new BattleArmor();
        armor.setSquadSize(6);
        armor.initializeInternal(1, BattleArmor.LOC_SQUAD);
        for (int location = 1; location <= 6; location++) {
            armor.initializeInternal(1, location);
            armor.initializeArmor(5, location);
        }
        var before = UnitModelState.capture(armor);
        assertEquals(List.of(1, 2, 3, 4, 5, 6), before.structure().members());
        armor.setArmor(0, 2);
        assertEquals(before.structure(), UnitModelState.capture(armor).structure());
        armor.setInternal(0, 2);
        assertEquals(List.of(1, 3, 4, 5, 6), UnitModelState.capture(armor).structure().members());
        for (int location = 1; location < 6; location++) {
            armor.setInternal(0, location);
        }
        armor.setProne(ProneCause.FORCED);
        var last = UnitModelState.capture(armor);
        assertEquals(List.of(6), last.structure().members());
        assertEquals(1, last.structure().activeTroopers());
        assertEquals(ProneCause.NONE, last.pose().proneCause());
        assertEquals(6, before.structure().activeTroopers());
        armor.setInternal(0, 6);
        assertEquals(0, UnitModelState.capture(armor).structure().activeTroopers());
    }
}
