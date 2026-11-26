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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.rules.RulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.common.weapons.handlers.lrm.LRMMagneticPulseHandler;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Magnetic Pulse missiles use the standard missile rules (TO:AUE p.182), so each group of an LRM volley rolls its own
 * hit location and partial cover stops a group that rolls a leg (TW p.102).
 */
class MagneticPulseGroupTest {
    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void aGroupThatStrikesPartialCoverAddsNoHeat() throws Exception {
        // Ten warheads hit in two groups of five: the first rolls a leg behind the cover
        assertEquals(1, heatOfTwoGroups(0, new HitData(Mek.LOC_LEFT_LEG)));
    }

    /** MP missiles inflict no damage, so the building around the target has none to absorb (TW p.171) */
    @Test
    void aBuildingAbsorbsNoMagneticPulseWarheads() throws Exception {
        assertEquals(2, heatOfTwoGroups(2, new HitData(Mek.LOC_LEFT_TORSO)));
    }

    /**
     * @return The heat ten Magnetic Pulse warheads add in two groups of five, the first striking the given location
     *       (a leg is behind partial cover) and the second the center torso
     */
    private static int heatOfTwoGroups(int bldgAbsorbs, HitData firstGroup) throws Exception {
        RulesManager originalRules = Game.rulesManager;
        try {
            TWGameManager manager = spy(new TWGameManager());
            // No server runs to receive the toast
            doNothing().when(manager).sendMagneticPulseToast(any(), anyBoolean(), anyBoolean());
            Game game = manager.getGame();
            Game.rulesManager = new TWRulesManager();
            game.setBoard(Board.createEmptyBoard(2, 1));
            Mek attacker = deployMek(game, 0, new BipedMek());
            Mek target = deployMek(game, 1, spy(new BipedMek()));
            doReturn(firstGroup, new HitData(Mek.LOC_CENTER_TORSO)).when(target)
                  .rollHitLocation(anyInt(), anyInt(), anyInt(), any(), anyInt());
            doReturn(false).when(target).removePartialCoverHits(anyInt(), anyInt(), anyInt());
            doReturn(true).when(target).removePartialCoverHits(eq(Mek.LOC_LEFT_LEG), anyInt(), anyInt());

            WeaponMounted launcher = (WeaponMounted) attacker.addEquipment(EquipmentType.get("ISLRM10"),
                  Mek.LOC_LEFT_ARM);
            launcher.setLinked(attacker.addEquipment(magneticPulseAmmo(), Mek.LOC_LEFT_TORSO));
            WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), target.getId(),
                  launcher.getEquipmentNum());
            WeaponHandler handler = new LRMMagneticPulseHandler(new ToHitData(), attack, game, manager);
            Vector<Report> reports = new Vector<>();

            handler.handleEntityDamage(target, reports, null, 10, 5, bldgAbsorbs);
            handler.firstHit = false;
            handler.handleEntityDamage(target, reports, null, 5, 5, bldgAbsorbs);

            assertTrue(target.getMagneticPulseRounds() > 0);
            return target.heatFromExternal;
        } finally {
            Game.rulesManager = originalRules;
        }
    }

    private static AmmoType magneticPulseAmmo() {
        return EquipmentType.allTypes().stream()
              .filter(type -> type instanceof AmmoType ammo
                    && (ammo.getAmmoType() == AmmoType.AmmoTypeEnum.LRM) && (ammo.getRackSize() == 10)
                    && !ammo.isClan() && ammo.getMunitionType().contains(AmmoType.Munitions.M_MAGNETIC_PULSE))
              .map(AmmoType.class::cast)
              .findFirst()
              .orElseThrow();
    }

    private static Mek deployMek(Game game, int x, Mek mek) {
        Player player = new Player(x, "Player " + x);
        game.addPlayer(x, player);
        mek.setOwner(player);
        mek.setPosition(new Coords(x, 0));
        mek.setWeight(50);
        mek.setEngine(new Engine(200, Engine.NORMAL_ENGINE, 0));
        mek.autoSetInternal();
        game.addEntity(mek);
        return mek;
    }
}
