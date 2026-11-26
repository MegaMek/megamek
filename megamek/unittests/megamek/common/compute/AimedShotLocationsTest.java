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
package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests which locations a leg or swarm attack may aim at, and when those attacks may be aimed at all (TW p.220).
 */
@DisplayName("Aimed leg and swarm attacks")
class AimedShotLocationsTest {

    private static final int ATTACKER_ID = 1;
    private static final int TARGET_ID = 2;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static WeaponType weaponType(String internalName) {
        return (WeaponType) EquipmentType.get(internalName);
    }

    private static WeaponMounted mountOn(Entity entity, String internalName) {
        return (WeaponMounted) Mounted.createMounted(entity, EquipmentType.get(internalName));
    }

    private static <T extends Entity> T withId(T entity, int id) {
        entity.setId(id);
        return entity;
    }

    private static BattleArmor squadSwarming(Entity target) {
        BattleArmor squad = withId(new BattleArmor(), ATTACKER_ID);
        squad.setSwarmTargetId(target.getId());
        return squad;
    }

    @Test
    @DisplayName("A leg attack can aim only at a leg")
    void legAttackAimsOnlyAtLegs() {
        BattleArmor squad = withId(new BattleArmor(), ATTACKER_ID);
        BipedMek target = withId(new BipedMek(), TARGET_ID);
        WeaponType legAttack = weaponType(Infantry.LEG_ATTACK);

        assertTrue(AimedShotLocations.canAimAt(squad, legAttack, target, Mek.LOC_LEFT_LEG));
        assertTrue(AimedShotLocations.canAimAt(squad, legAttack, target, Mek.LOC_RIGHT_LEG));
        assertFalse(AimedShotLocations.canAimAt(squad, legAttack, target, Mek.LOC_HEAD),
              "a leg attack must never be able to land on the head");
        assertFalse(AimedShotLocations.canAimAt(squad, legAttack, target, Mek.LOC_CENTER_TORSO));
    }

    @Test
    @DisplayName("Battle armor swarming a biped Mek cannot aim at a leg, which is not on the swarm table")
    void swarmingBattleArmorSkipsBipedLegs() {
        BipedMek target = withId(new BipedMek(), TARGET_ID);
        BattleArmor squad = squadSwarming(target);
        WeaponType swarmAttack = weaponType(Infantry.SWARM_WEAPON_MEK);

        assertFalse(AimedShotLocations.canAimAt(squad, swarmAttack, target, Mek.LOC_LEFT_LEG));
        assertTrue(AimedShotLocations.canAimAt(squad, swarmAttack, target, Mek.LOC_HEAD));
        assertTrue(AimedShotLocations.canAimAt(squad, swarmAttack, target, Mek.LOC_LEFT_ARM));
    }

    @Test
    @DisplayName("Conventional infantry swarming a biped Mek cannot aim its rifles at a leg")
    void swarmingConventionalInfantrySkipsBipedLegs() {
        BipedMek target = withId(new BipedMek(), TARGET_ID);
        ConvInfantry platoon = withId(new ConvInfantry(), ATTACKER_ID);
        platoon.setSwarmTargetId(target.getId());
        WeaponType rifle = weaponType("InfantryAutoRifle");

        assertFalse(AimedShotLocations.canAimAt(platoon, rifle, target, Mek.LOC_RIGHT_LEG),
              "conventional swarm attacks roll on the same swarm table, which has no legs");
        assertTrue(AimedShotLocations.canAimAt(platoon, rifle, target, Mek.LOC_CENTER_TORSO));
    }

    @Test
    @DisplayName("A swarming attack on a quad Mek or a tank can aim anywhere")
    void swarmingQuadOrTankAimsAnywhere() {
        WeaponType swarmAttack = weaponType(Infantry.SWARM_WEAPON_MEK);
        QuadMek quad = withId(new QuadMek(), TARGET_ID);
        Tank tank = withId(new Tank(), TARGET_ID);

        assertTrue(AimedShotLocations.canAimAt(squadSwarming(quad), swarmAttack, quad, Mek.LOC_LEFT_LEG),
              "the quad swarm table includes the legs");
        assertTrue(AimedShotLocations.canAimAt(squadSwarming(tank), swarmAttack, tank, Tank.LOC_TURRET));
    }

    @Test
    @DisplayName("A unit that is not swarming can aim an ordinary weapon at any location")
    void ordinaryWeaponAimsAnywhere() {
        BattleArmor squad = withId(new BattleArmor(), ATTACKER_ID);
        BipedMek target = withId(new BipedMek(), TARGET_ID);
        WeaponType mediumLaser = weaponType("ISMediumLaser");

        assertTrue(AimedShotLocations.canAimAt(squad, mediumLaser, target, Mek.LOC_HEAD));
        assertTrue(AimedShotLocations.canAimAt(squad, mediumLaser, target, Mek.LOC_LEFT_LEG));
    }

    @Test
    @DisplayName("A leg attack can be aimed at an immobile target, but not with a targeting computer")
    void legAttackAimsOnlyAtImmobileTargets() {
        WeaponMounted legAttack = mountOn(new BattleArmor(), Infantry.LEG_ATTACK);

        assertTrue(Compute.allowAimedShotWith(legAttack, AimingMode.IMMOBILE),
              "TW p.220 allows an aimed leg attack against an immobile target");
        assertFalse(Compute.allowAimedShotWith(legAttack, AimingMode.TARGETING_COMPUTER));
        assertFalse(Compute.allowAimedShotWith(legAttack, AimingMode.NONE));
    }

    @Test
    @DisplayName("A swarm attempt still cannot be aimed, because it deals no damage")
    void swarmAttemptCannotAim() {
        WeaponMounted swarmAttempt = mountOn(new BattleArmor(), Infantry.SWARM_MEK);

        assertFalse(Compute.allowAimedShotWith(swarmAttempt, AimingMode.IMMOBILE));
    }

    @Test
    @DisplayName("A swarming attack can be aimed at an immobile target")
    void swarmingAttackAimsAtImmobileTarget() {
        WeaponMounted swarmAttack = mountOn(new BattleArmor(), Infantry.SWARM_WEAPON_MEK);

        assertTrue(Compute.allowAimedShotWith(swarmAttack, AimingMode.IMMOBILE));
    }
}
