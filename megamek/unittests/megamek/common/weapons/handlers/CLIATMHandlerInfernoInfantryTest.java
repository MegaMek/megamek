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

import java.util.Vector;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression test for issue #8534: Improved Inferno (IIW) iATM missiles must deliver one inferno missile per missile in
 * the rack against conventional infantry, so that the standard Inferno rule (WoR p.202 -> TW p.141: every inferno
 * missile that strikes eliminates three troopers) applies to the whole salvo. Before the fix the salvo was collapsed
 * into a single inferno missile, killing only three troopers regardless of rack size.
 */
class CLIATMHandlerInfernoInfantryTest {

    private static final String IATM6_WEAPON = "CLiATM6";
    private static final String IATM6_IIW_AMMO = "Clan Ammo iATM-6 IIW";

    private Game game;
    private TWGameManager gameManager;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        gameManager = new TWGameManager();
        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Attacker"));
        game.addPlayer(1, new Player(1, "Defender"));
    }

    private WeaponMounted addImprovedInfernoLauncher(Entity attacker) throws Exception {
        WeaponType launcherType = (WeaponType) EquipmentType.get(IATM6_WEAPON);
        AmmoType improvedInfernoAmmo = (AmmoType) EquipmentType.get(IATM6_IIW_AMMO);
        WeaponMounted launcher = (WeaponMounted) attacker.addEquipment(launcherType, Mek.LOC_RIGHT_TORSO);
        Mounted<?> ammo = attacker.addEquipment(improvedInfernoAmmo, Mek.LOC_RIGHT_TORSO);
        launcher.setLinked(ammo);
        return launcher;
    }

    private Entity createAttacker() throws Exception {
        BipedMek attacker = new BipedMek();
        attacker.setGame(game);
        attacker.setId(1);
        attacker.setChassis("iATM Carrier");
        attacker.setModel("Attacker");
        attacker.setCrew(new Crew(CrewType.SINGLE));
        attacker.setOwner(game.getPlayer(0));
        attacker.setWeight(50.0);
        attacker.setOriginalWalkMP(5);
        attacker.setPosition(new Coords(1, 1));
        attacker.setFacing(0);
        return attacker;
    }

    private ConvInfantry createInfantryTarget(int squadSize, int squadCount) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setGame(game);
        infantry.setId(2);
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        infantry.setSquadSize(squadSize);
        infantry.setSquadCount(squadCount);
        infantry.autoSetInternal();
        infantry.setOwner(game.getPlayer(1));
        infantry.setPosition(new Coords(1, 2));
        return infantry;
    }

    @Test
    @DisplayName("IIW iATM delivers one inferno missile per rack missile against conventional infantry")
    void improvedInfernoDeliversRackSizeMissilesToInfantry() throws EntityLoadingException {
        Entity attacker;
        WeaponMounted launcher;
        try {
            attacker = createAttacker();
            launcher = addImprovedInfernoLauncher(attacker);
        } catch (Exception exception) {
            throw new EntityLoadingException(exception.getMessage(), exception);
        }
        ConvInfantry target = createInfantryTarget(7, 4);
        game.addEntity(attacker);
        game.addEntity(target);

        WeaponAttackAction weaponAttack = new WeaponAttackAction(attacker.getId(), target.getId(),
              attacker.getEquipmentNum(launcher));
        CLIATMHandler handler = new CLIATMHandler(new ToHitData(), weaponAttack, game, gameManager);

        int infernoMissiles = handler.calcHits(new Vector<Report>());

        assertEquals(launcher.getType().getRackSize(), infernoMissiles,
              "IIW iATM must deliver one inferno missile per missile in the rack against conventional infantry "
                    + "(regression for #8534, previously collapsed to a single missile)");
        assertEquals(6, infernoMissiles,
              "An iATM/6 firing Improved Inferno at infantry should deliver 6 inferno missiles (18 troopers)");
    }
}
