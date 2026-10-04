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

package megamek.common.weapons.handlers.plasma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Vector;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Mek;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the plasma half of issue #8450.
 *
 * <p>The plasma rifle applied the Artificial Pain Shunt reduction (IO p. 78) to the cluster size rather than the
 * damage. Cluster size does not change the total: the damage loop applies {@code damagePerHit * min(nCluster,
 * hits)} per group and subtracts {@code nCluster} from the remaining hits, so the total is always
 * {@code hits * damagePerHit}. Halving the cluster only split the same damage into more, smaller groups, which
 * against battle armor spreads it over more troopers and wastes less overkill - the shunt was a net penalty.</p>
 */
class PlasmaPainShuntTest {

    private static final String PLASMA_RIFLE = "ISPlasmaRifle";
    private static final String PLASMA_RIFLE_AMMO = "ISPlasmaRifleAmmo";

    /** A plasma rifle deals 10 + 2d6 damage to a target that does not track heat, so 12 to 22. */
    private static final int MINIMUM_UNSHUNTED_DAMAGE = 12;
    private static final int MAXIMUM_UNSHUNTED_DAMAGE = 22;
    /** The cluster size the plasma rifle uses against battle armor. */
    private static final int BATTLE_ARMOR_CLUSTER_SIZE = 5;

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
        attacker.setChassis("Plasma Carrier");
        attacker.setModel("Attacker");
        attacker.setCrew(new Crew(CrewType.SINGLE));
        attacker.setOwner(game.getPlayer(0));
        attacker.setWeight(50.0);
        attacker.setOriginalWalkMP(5);
        attacker.setPosition(new Coords(1, 1));
        attacker.setFacing(0);
        return attacker;
    }

    private WeaponMounted mountPlasmaRifle(Entity attacker) throws EntityLoadingException {
        try {
            WeaponType weaponType = (WeaponType) EquipmentType.get(PLASMA_RIFLE);
            WeaponMounted weapon = (WeaponMounted) attacker.addEquipment(weaponType, Mek.LOC_RIGHT_ARM);
            Mounted<?> ammo = attacker.addEquipment(EquipmentType.get(PLASMA_RIFLE_AMMO), Mek.LOC_RIGHT_TORSO);
            weapon.setLinked(ammo);
            return weapon;
        } catch (Exception exception) {
            throw new EntityLoadingException(exception.getMessage(), exception);
        }
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

    private PlasmaRifleHandler handlerAgainst(BattleArmor target) throws EntityLoadingException {
        BipedMek attacker = createAttacker();
        WeaponMounted plasmaRifle = mountPlasmaRifle(attacker);
        game.addEntity(attacker);
        game.addEntity(target);

        WeaponAttackAction weaponAttack = new WeaponAttackAction(attacker.getId(), target.getId(),
              attacker.getEquipmentNum(plasmaRifle));
        return new PlasmaRifleHandler(new ToHitData(), weaponAttack, game, gameManager);
    }

    @Test
    @DisplayName("Plasma rifle damage against battle armor is halved by a pain shunt")
    void plasmaRifleHalvesDamageAgainstPainShuntedBattleArmor() throws EntityLoadingException {
        PlasmaRifleHandler handler = handlerAgainst(createBattleArmorTarget(true));

        int hits = handler.calcHits(new Vector<Report>());

        // The upper bound of the halved band is MAXIMUM_UNSHUNTED_DAMAGE / 2, which is 11, and the first
        // assertion has already required hits to be under 12. Asserting it a second time can never fail, so
        // only the lower bound is checked here and the two together still pin the 6-to-11 band.
        assertTrue(hits < MINIMUM_UNSHUNTED_DAMAGE,
              "A pain shunt must actually reduce the damage, not just regroup it. Unshunted damage is at least "
                    + MINIMUM_UNSHUNTED_DAMAGE + "; got " + hits + " - regression for #8450");
        assertTrue(hits >= MINIMUM_UNSHUNTED_DAMAGE / 2,
              "Half of 10 + 2d6 is 6 to 11; got " + hits);
    }

    @Test
    @DisplayName("Plasma rifle damage against battle armor without pain shunts is unchanged")
    void plasmaRifleDealsFullDamageToUnshuntedBattleArmor() throws EntityLoadingException {
        PlasmaRifleHandler handler = handlerAgainst(createBattleArmorTarget(false));

        int hits = handler.calcHits(new Vector<Report>());

        assertTrue((hits >= MINIMUM_UNSHUNTED_DAMAGE) && (hits <= MAXIMUM_UNSHUNTED_DAMAGE),
              "A plasma rifle deals 10 + 2d6 to battle armor; got " + hits);
    }

    @Test
    @DisplayName("A pain shunt no longer shrinks the plasma cluster size")
    void painShuntDoesNotChangeClusterSize() throws EntityLoadingException {
        PlasmaRifleHandler shuntedHandler = handlerAgainst(createBattleArmorTarget(true));

        assertEquals(BATTLE_ARMOR_CLUSTER_SIZE, shuntedHandler.calculateNumCluster(),
              "Halving the cluster size never reduced the total damage, and against battle armor it spread the "
                    + "damage over more troopers. The reduction belongs on the damage - regression for #8450");
    }
}
