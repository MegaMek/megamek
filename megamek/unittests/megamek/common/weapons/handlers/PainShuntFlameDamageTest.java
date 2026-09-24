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

import java.util.Vector;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Mek;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #8450 and RFE #8435.
 *
 * <p>Conventional infantry and battle armor units made up of pain-shunted warriors halve any damage from
 * flame-based weapons (IO p. 78; TO:AR p. 171 defines flame-based as plasma, flamer and Firedrake). Before the fix
 * the flamer handlers applied that reduction only when the target was conventional infantry, so pain-shunted
 * battle armor took full damage.</p>
 *
 * <p>The heavy flamer tests also cover the reporting half of #8435: anti-infantry damage now goes through the
 * shared handler, which writes the step-by-step damage breakdown added by PR #8198.</p>
 */
class PainShuntFlameDamageTest {

    private static final String FLAMER = "ISFlamer";
    private static final String HEAVY_FLAMER = "ISHeavyFlamer";
    private static final String HEAVY_FLAMER_AMMO = "IS Heavy Flamer Ammo";

    /** Damage of a standard flamer against a target that is not conventional infantry. */
    private static final int FLAMER_DAMAGE = 2;
    /** Damage of a heavy flamer against a target that is not conventional infantry. */
    private static final int HEAVY_FLAMER_DAMAGE = 4;

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

    private BipedMek createAttacker() {
        BipedMek attacker = new BipedMek();
        attacker.setGame(game);
        attacker.setId(1);
        attacker.setChassis("Flame Carrier");
        attacker.setModel("Attacker");
        attacker.setCrew(new Crew(CrewType.SINGLE));
        attacker.setOwner(game.getPlayer(0));
        attacker.setWeight(50.0);
        attacker.setOriginalWalkMP(5);
        attacker.setPosition(new Coords(1, 1));
        attacker.setFacing(0);
        return attacker;
    }

    private WeaponMounted mountFlamer(Entity attacker, String weaponName) throws EntityLoadingException {
        try {
            WeaponType weaponType = (WeaponType) EquipmentType.get(weaponName);
            WeaponMounted weapon = (WeaponMounted) attacker.addEquipment(weaponType, Mek.LOC_RIGHT_ARM);
            if (weaponType.getAmmoType() != AmmoType.AmmoTypeEnum.NA) {
                Mounted<?> ammo = attacker.addEquipment(EquipmentType.get(HEAVY_FLAMER_AMMO), Mek.LOC_RIGHT_TORSO);
                weapon.setLinked(ammo);
            }
            return weapon;
        } catch (Exception exception) {
            throw new EntityLoadingException(exception.getMessage(), exception);
        }
    }

    private BattleArmor createFireResistantBattleArmorTarget() {
        BattleArmor battleArmor = createBattleArmorTarget(false);
        // The all-locations overloads; the per-location ones recalculate tech advancement, which needs a fully
        // built armor entry this bare test unit does not have. Fire-Resistant armor is Clan-only, so the tech
        // level has to say Clan or the battle value calculator cannot resolve the armor by name.
        battleArmor.setArmorType(EquipmentType.T_ARMOR_BA_FIRE_RESIST);
        battleArmor.setArmorTechLevel(TechConstants.T_CLAN_TW);
        return battleArmor;
    }

    private BattleArmor createBattleArmorTarget(boolean painShunted) {
        BattleArmor battleArmor = new BattleArmor();
        battleArmor.setGame(game);
        battleArmor.setId(2);
        battleArmor.setChassis("Asura");
        battleArmor.setModel("Test");
        battleArmor.setSquadSize(4);
        battleArmor.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        battleArmor.setCrew(crew);
        if (painShunted) {
            crew.getOptions().getOption(OptionsConstants.MD_PAIN_SHUNT).setValue(true);
        }

        battleArmor.setOwner(game.getPlayer(1));
        for (int trooper = 1; trooper <= 4; trooper++) {
            battleArmor.initializeArmor(4, trooper);
        }
        battleArmor.autoSetInternal();
        battleArmor.setPosition(new Coords(1, 2));
        return battleArmor;
    }

    private ConvInfantry createInfantryTarget() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setGame(game);
        infantry.setId(3);
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        infantry.setSquadSize(7);
        infantry.setSquadCount(4);
        infantry.setCrew(new Crew(CrewType.INFANTRY_CREW));
        infantry.autoSetInternal();
        infantry.setOwner(game.getPlayer(1));
        infantry.setPosition(new Coords(1, 2));
        return infantry;
    }

    private WeaponAttackAction attack(Entity attacker, Entity target, WeaponMounted weapon) {
        return new WeaponAttackAction(attacker.getId(), target.getId(), attacker.getEquipmentNum(weapon));
    }

    @Test
    @DisplayName("A standard flamer halves its damage against pain-shunted battle armor")
    void flamerHalvesDamageAgainstPainShuntedBattleArmor() throws EntityLoadingException {
        BipedMek attacker = createAttacker();
        WeaponMounted flamer = mountFlamer(attacker, FLAMER);
        BattleArmor target = createBattleArmorTarget(true);
        game.addEntity(attacker);
        game.addEntity(target);

        FlamerHandler handler = new FlamerHandler(new ToHitData(), attack(attacker, target, flamer), game,
              gameManager);

        assertEquals(FLAMER_DAMAGE / 2, handler.calcDamagePerHit(),
              "Pain-shunted battle armor halves flame damage (IO p. 78) - regression for #8450");
    }

    @Test
    @DisplayName("A standard flamer deals full damage to battle armor without pain shunts")
    void flamerDealsFullDamageToUnshuntedBattleArmor() throws EntityLoadingException {
        BipedMek attacker = createAttacker();
        WeaponMounted flamer = mountFlamer(attacker, FLAMER);
        BattleArmor target = createBattleArmorTarget(false);
        game.addEntity(attacker);
        game.addEntity(target);

        FlamerHandler handler = new FlamerHandler(new ToHitData(), attack(attacker, target, flamer), game,
              gameManager);

        assertEquals(FLAMER_DAMAGE, handler.calcDamagePerHit(),
              "Battle armor without a pain shunt takes the flamer's full damage");
    }

    @Test
    @DisplayName("A heavy flamer halves its damage against pain-shunted battle armor")
    void heavyFlamerHalvesDamageAgainstPainShuntedBattleArmor() throws EntityLoadingException {
        BipedMek attacker = createAttacker();
        WeaponMounted heavyFlamer = mountFlamer(attacker, HEAVY_FLAMER);
        BattleArmor target = createBattleArmorTarget(true);
        game.addEntity(attacker);
        game.addEntity(target);

        VehicleFlamerHandler handler = new VehicleFlamerHandler(new ToHitData(),
              attack(attacker, target, heavyFlamer), game, gameManager);

        assertEquals(HEAVY_FLAMER_DAMAGE / 2, handler.calcDamagePerHit(),
              "Pain-shunted battle armor halves heavy flamer damage too - regression for #8450");
    }

    @Test
    @DisplayName("A heavy flamer deals full damage to battle armor without pain shunts")
    void heavyFlamerDealsFullDamageToUnshuntedBattleArmor() throws EntityLoadingException {
        BipedMek attacker = createAttacker();
        WeaponMounted heavyFlamer = mountFlamer(attacker, HEAVY_FLAMER);
        BattleArmor target = createBattleArmorTarget(false);
        game.addEntity(attacker);
        game.addEntity(target);

        VehicleFlamerHandler handler = new VehicleFlamerHandler(new ToHitData(),
              attack(attacker, target, heavyFlamer), game, gameManager);

        assertEquals(HEAVY_FLAMER_DAMAGE, handler.calcDamagePerHit(),
              "Battle armor without a pain shunt takes the heavy flamer's full damage");
    }

    @Test
    @DisplayName("A standard flamer does nothing to fire-resistant battle armor")
    void flamerDoesNoDamageToFireResistantBattleArmor() throws EntityLoadingException {
        BipedMek attacker = createAttacker();
        WeaponMounted flamer = mountFlamer(attacker, FLAMER);
        BattleArmor target = createFireResistantBattleArmorTarget();
        game.addEntity(attacker);
        game.addEntity(target);

        FlamerHandler handler = new FlamerHandler(new ToHitData(), attack(attacker, target, flamer), game,
              gameManager);

        assertEquals(0, handler.calcDamagePerHit(),
              "A Fire-Resistant suit ignores damage from heat-causing weapons (TM p. 170)");
    }

    @Test
    @DisplayName("A heavy flamer does nothing to fire-resistant battle armor")
    void heavyFlamerDoesNoDamageToFireResistantBattleArmor() throws EntityLoadingException {
        BipedMek attacker = createAttacker();
        WeaponMounted heavyFlamer = mountFlamer(attacker, HEAVY_FLAMER);
        BattleArmor target = createFireResistantBattleArmorTarget();
        game.addEntity(attacker);
        game.addEntity(target);

        VehicleFlamerHandler handler = new VehicleFlamerHandler(new ToHitData(),
              attack(attacker, target, heavyFlamer), game, gameManager);

        assertEquals(0, handler.calcDamagePerHit(),
              "The heavy flamer is heat-causing too, so a Fire-Resistant suit ignores it (TM p. 170). Before the "
                    + "fix only the standard flamer honoured this");
    }

    @Test
    @DisplayName("A heavy flamer still rolls 6d6 against conventional infantry, and now reports the breakdown")
    void heavyFlamerRollsBurstDamageAndReportsAgainstConventionalInfantry() throws EntityLoadingException {
        BipedMek attacker = createAttacker();
        WeaponMounted heavyFlamer = mountFlamer(attacker, HEAVY_FLAMER);
        ConvInfantry target = createInfantryTarget();
        game.addEntity(attacker);
        game.addEntity(target);

        VehicleFlamerHandler handler = new VehicleFlamerHandler(new ToHitData(),
              attack(attacker, target, heavyFlamer), game, gameManager);

        int damage = handler.calcDamagePerHit();

        assertTrue((damage >= 6) && (damage <= 36),
              "The heavy flamer's infantry damage class is 6D6, so damage must stay in 6-36; got " + damage);
        assertFalse(handler.calcDmgPerHitReport.isEmpty(),
              "Heavy flamer anti-infantry damage must now write the damage breakdown report - RFE #8435");
    }

    @Test
    @DisplayName("A heavy flamer halves its 6d6 roll against pain-shunted conventional infantry")
    void heavyFlamerHalvesDamageAgainstPainShuntedConventionalInfantry() throws EntityLoadingException {
        BipedMek attacker = createAttacker();
        WeaponMounted heavyFlamer = mountFlamer(attacker, HEAVY_FLAMER);
        ConvInfantry target = createInfantryTarget();
        target.getCrew().getOptions().getOption(OptionsConstants.MD_PAIN_SHUNT).setValue(true);
        game.addEntity(attacker);
        game.addEntity(target);

        VehicleFlamerHandler handler = new VehicleFlamerHandler(new ToHitData(),
              attack(attacker, target, heavyFlamer), game, gameManager);

        int damage = handler.calcDamagePerHit();

        assertTrue((damage >= 3) && (damage <= 18),
              "Half of a 6D6 burst is 3-18; got " + damage);
    }
}
