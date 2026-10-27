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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.EMI;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Electromagnetic interference costs 2 on the cluster roll, from planetary EMI or from a nuclear strike's secondary
 * effects (IO:AE p.166), and never more than 2.
 */
class EmiClusterPenaltyTest {
    private Game game;
    private Entity attacker;
    private MissileWeaponHandler handler;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws Exception {
        TWGameManager gameManager = new TWGameManager();
        game = gameManager.getGame();
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
        game.addPlayer(0, new Player(0, "Attacker"));
        game.addPlayer(1, new Player(1, "Defender"));

        attacker = createMek(1, 0, "Attacker");
        Mounted<?> lrm = attacker.addEquipment((WeaponType) EquipmentType.get("ISLRM20"), Mek.LOC_RIGHT_TORSO);
        Entity target = createMek(2, 1, "Target");
        game.addEntity(attacker);
        game.addEntity(target);
        attacker.setPosition(new Coords(1, 1));
        target.setPosition(new Coords(1, 8));

        WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), target.getId(),
              attacker.getEquipmentNum(lrm));
        handler = new MissileWeaponHandler(new ToHitData(), attack, game, gameManager);
    }

    private Entity createMek(int id, int playerId, String model) {
        Entity entity = new BipedMek();
        entity.setGame(game);
        entity.setId(id);
        entity.setChassis("Test Mek");
        entity.setModel(model);
        entity.setCrew(new Crew(CrewType.SINGLE));
        entity.setOwner(game.getPlayer(playerId));
        entity.setWeight(50.0);
        entity.setOriginalWalkMP(5);
        return entity;
    }

    @Test
    void noInterferenceNoPenalty() {
        assertFalse(handler.isClusterRollUnderEMI());
    }

    @Test
    void attackerHitByNuclearSecondaryEffectsTakesTheClusterPenalty() {
        int clearModifier = handler.getClusterModifiers(false);

        attacker.setEMI(true);

        assertTrue(handler.isClusterRollUnderEMI());
        assertEquals(clearModifier - 2, handler.getClusterModifiers(false));
    }

    @Test
    void planetaryAndNuclearInterferenceCostTwoNotFour() {
        int clearModifier = handler.getClusterModifiers(false);

        game.getPlanetaryConditions().setEMI(EMI.EMI);
        attacker.setEMI(true);

        assertEquals(clearModifier - 2, handler.getClusterModifiers(false), "the same effect from two sources");
    }
}
