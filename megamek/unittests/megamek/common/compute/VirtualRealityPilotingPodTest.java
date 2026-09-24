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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.MMConstants;
import megamek.common.CriticalSlot;
import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.autoResolve.damage.EntityFinalState;
import megamek.common.autoResolve.damage.MekDamageApplier;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.VirtualRealityPilotingPod.Interference;
import megamek.common.compute.VirtualRealityPilotingPod.InterferenceState;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.INarcPod;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.EMI;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Game rules for the Virtual Reality Piloting Pod cockpit (IO:AE p.63): its gunnery and piloting bonuses, blindness
 * under hostile interference, the ECCM fallback, and the torso-mounted cockpit protections it inherits.
 */
class VirtualRealityPilotingPodTest {

    private static final Coords POD_HEX = new Coords(7, 6);
    private static final Coords ENEMY_HEX = new Coords(7, 5);
    private static final Coords FAR_HEX = new Coords(1, 1);

    private Game game;
    private Player friendlyPlayer;
    private Player enemyPlayer;
    private Mek podMek;
    private Tank target;
    private WeaponMounted laser;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws Exception {
        friendlyPlayer = new Player(1, "Friendly");
        friendlyPlayer.setTeam(1);
        enemyPlayer = new Player(2, "Enemy");
        enemyPlayer.setTeam(2);

        game = new Game();
        game.addPlayer(1, friendlyPlayer);
        game.addPlayer(2, enemyPlayer);

        Hex[] hexes = new Hex[17 * 16];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        game.setBoard(new Board(17, 16, hexes));

        podMek = createMek(Mek.COCKPIT_VRRP, friendlyPlayer, 1);
        laser = (WeaponMounted) WeaponMounted.createMounted(podMek, EquipmentType.get("ISMediumLaser"));
        podMek.addEquipment(laser, Mek.LOC_CENTER_TORSO, false);
        game.addEntity(podMek, false);
        podMek.setPosition(POD_HEX);

        target = new Tank();
        target.setId(2);
        target.setOwner(enemyPlayer);
        target.setCrew(new Crew(CrewType.SINGLE));
        game.addEntity(target, false);
        target.setPosition(ENEMY_HEX);
    }

    private Mek createMek(int cockpitType, Player owner, int id) {
        Mek mek = new BipedMek();
        mek.setId(id);
        mek.setChassis("Test");
        mek.setModel("VRPP");
        mek.setWeight(50.0);
        mek.setOriginalWalkMP(5);
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.setOwner(owner);
        mek.setCockpitType(cockpitType);
        mek.setDeployed(true);
        return mek;
    }

    /** Adds a Mek carrying a Guardian ECM suite in the given mode for the given owner at the given hex. */
    private Mek addEcmCarrier(Player owner, int id, Coords position, String mode) throws Exception {
        Mek carrier = createMek(Mek.COCKPIT_STANDARD, owner, id);
        MiscMounted ecm = (MiscMounted) MiscMounted.createMounted(carrier, EquipmentType.get("ISGuardianECMSuite"));
        carrier.addEquipment(ecm, Mek.LOC_LEFT_TORSO, false);
        game.addEntity(carrier, false);
        carrier.setPosition(position);
        carrier.setGameOptions();
        assertTrue(ecm.setModeImmediately(mode) >= 0, "Guardian ECM should offer the " + mode + " mode");
        return carrier;
    }

    private void enableEccmRules() {
        game.getOptions().getOption(OptionsConstants.ADVANCED_TAC_OPS_ECCM).setValue(true);
    }

    private ToHitData weaponAttack() {
        return WeaponAttackAction.toHit(game, podMek.getId(), target, podMek.getEquipmentNum(laser), false);
    }

    private PilotingRollData pilotingRoll() {
        return podMek.addEntityBonuses(new PilotingRollData(podMek.getId(), 0, "test"));
    }

    // ---- Undisturbed pod ----

    @Test
    void undisturbedPodIsNormal() {
        assertEquals(Interference.NONE, VirtualRealityPilotingPod.getInterference(podMek));
    }

    @Test
    void undisturbedPodGivesMinusOneGunnery() {
        ToHitData toHit = weaponAttack();
        assertNotEquals(ToHitData.IMPOSSIBLE, toHit.getValue());
        assertTrue(toHit.getDesc().contains("Virtual Reality Piloting Pod"), toHit.getDesc());
        assertEquals(VirtualRealityPilotingPod.GUNNERY_MODIFIER, modifierNamed(toHit, "Virtual Reality Piloting Pod"));
    }

    @Test
    void undisturbedPodGivesMinusTwoPiloting() {
        PilotingRollData roll = pilotingRoll();
        assertEquals(VirtualRealityPilotingPod.PILOTING_MODIFIER, roll.getValue(), roll.getDesc());
        assertFalse(roll.getDesc().contains("Torso-Mounted Cockpit"),
              "the pod's own modifier replaces the torso-mounted penalty: " + roll.getDesc());
    }

    @Test
    void standardCockpitGetsNoPodModifiers() {
        Mek standard = createMek(Mek.COCKPIT_STANDARD, friendlyPlayer, 3);
        game.addEntity(standard, false);
        standard.setPosition(FAR_HEX);
        assertEquals(Interference.NONE, VirtualRealityPilotingPod.getInterference(standard));
        PilotingRollData roll = standard.addEntityBonuses(new PilotingRollData(standard.getId(), 0, "test"));
        assertEquals(0, roll.getValue(), roll.getDesc());
    }

    // ---- Blindness sources ----

    @Test
    void hostileEcmOverHexBlindsThePod() throws Exception {
        addEcmCarrier(enemyPlayer, 10, ENEMY_HEX, MiscType.MODE_ECM);

        Interference interference = VirtualRealityPilotingPod.getInterference(podMek);
        assertEquals(InterferenceState.BLINDED, interference.state());
        assertEquals("hostile ECM", interference.source());
    }

    @Test
    void hostileEcmOutOfRangeDoesNotBlind() throws Exception {
        addEcmCarrier(enemyPlayer, 10, FAR_HEX, MiscType.MODE_ECM);
        assertEquals(Interference.NONE, VirtualRealityPilotingPod.getInterference(podMek));
    }

    @Test
    void friendlyEcmDoesNotBlind() throws Exception {
        addEcmCarrier(friendlyPlayer, 10, ENEMY_HEX, MiscType.MODE_ECM);
        assertEquals(Interference.NONE, VirtualRealityPilotingPod.getInterference(podMek));
    }

    @Test
    void nuclearEmiBlindsThePod() {
        podMek.setEMI(true);
        Interference interference = VirtualRealityPilotingPod.getInterference(podMek);
        assertEquals(InterferenceState.BLINDED, interference.state());
        assertEquals("nuclear EMI", interference.source());
    }

    @Test
    void planetaryEmiBlindsThePod() {
        game.getPlanetaryConditions().setEMI(EMI.EMI);
        Interference interference = VirtualRealityPilotingPod.getInterference(podMek);
        assertEquals(InterferenceState.BLINDED, interference.state());
        assertEquals("EMI conditions", interference.source());
    }

    @Test
    void empMineInterferenceBlindsThePod() {
        podMek.setEMPInterference(2, true);
        assertEquals(InterferenceState.BLINDED, VirtualRealityPilotingPod.getInterference(podMek).state());
    }

    @Test
    void taserInterferenceBlindsThePod() {
        podMek.setTaserInterference(1, 2, false);
        Interference interference = VirtualRealityPilotingPod.getInterference(podMek);
        assertEquals(InterferenceState.BLINDED, interference.state());
        assertEquals("taser hit", interference.source());
    }

    @Test
    void tsempInterferenceBlindsThePod() {
        podMek.setTsempEffect(MMConstants.TSEMP_EFFECT_INTERFERENCE);
        Interference interference = VirtualRealityPilotingPod.getInterference(podMek);
        assertEquals(InterferenceState.BLINDED, interference.state());
        assertEquals("TSEMP hit", interference.source());
    }

    @Test
    void iNarcEcmPodBlindsThePod() {
        podMek.attachINarcPod(new INarcPod(enemyPlayer.getTeam(), INarcPod.ECM, Mek.LOC_CENTER_TORSO));
        // Pods attach at the start of the next round
        podMek.newRound(1);
        assertTrue(podMek.isINarcedWith(INarcPod.ECM));

        Interference interference = VirtualRealityPilotingPod.getInterference(podMek);
        assertEquals(InterferenceState.BLINDED, interference.state());
        assertEquals("iNarc ECM pod", interference.source());
    }

    // ---- Effects of blindness ----

    @Test
    void blindedPodCannotFireWeapons() {
        podMek.setTsempEffect(MMConstants.TSEMP_EFFECT_INTERFERENCE);
        ToHitData toHit = weaponAttack();
        assertEquals(ToHitData.IMPOSSIBLE, toHit.getValue());
        assertTrue(toHit.getDesc().contains("blinded by TSEMP hit"), toHit.getDesc());
    }

    @Test
    void blindedPodGetsPlusThreePiloting() {
        podMek.setTsempEffect(MMConstants.TSEMP_EFFECT_INTERFERENCE);
        PilotingRollData roll = pilotingRoll();
        assertEquals(VirtualRealityPilotingPod.BLINDED_PILOTING_MODIFIER, roll.getValue(), roll.getDesc());
    }

    // ---- ECCM fallback ----

    @Test
    void friendlyEccmNegatingHostileEcmDegradesInsteadOfBlinding() throws Exception {
        enableEccmRules();
        addEcmCarrier(enemyPlayer, 10, ENEMY_HEX, MiscType.MODE_ECM);
        addEcmCarrier(friendlyPlayer, 11, POD_HEX, "ECCM");

        Interference interference = VirtualRealityPilotingPod.getInterference(podMek);
        assertEquals(InterferenceState.DEGRADED, interference.state());

        ToHitData toHit = weaponAttack();
        assertNotEquals(ToHitData.IMPOSSIBLE, toHit.getValue());
        assertEquals(VirtualRealityPilotingPod.DEGRADED_MODIFIER,
              modifierNamed(toHit, "Virtual Reality Piloting Pod degraded"));

        PilotingRollData roll = pilotingRoll();
        assertEquals(VirtualRealityPilotingPod.DEGRADED_MODIFIER, roll.getValue(), roll.getDesc());
    }

    @Test
    void friendlyEccmAlsoDegradesNonEcmInterference() throws Exception {
        enableEccmRules();
        addEcmCarrier(friendlyPlayer, 11, POD_HEX, "ECCM");
        podMek.setTaserInterference(1, 2, false);

        assertEquals(InterferenceState.DEGRADED, VirtualRealityPilotingPod.getInterference(podMek).state());
    }

    @Test
    void friendlyEccmWithoutTheRuleDoesNotHelp() throws Exception {
        addEcmCarrier(enemyPlayer, 10, ENEMY_HEX, MiscType.MODE_ECM);
        // Without the ECCM rule the suite offers no ECCM mode; a friendly ECM suite is no help either.
        addEcmCarrier(friendlyPlayer, 11, POD_HEX, MiscType.MODE_ECM);

        assertEquals(InterferenceState.BLINDED, VirtualRealityPilotingPod.getInterference(podMek).state());
    }

    @Test
    void friendlyEccmThatFailsToNegateAngelEcmStillBlinds() throws Exception {
        enableEccmRules();
        Mek angelCarrier = createMek(Mek.COCKPIT_STANDARD, enemyPlayer, 10);
        MiscMounted angel = (MiscMounted) MiscMounted.createMounted(angelCarrier,
              EquipmentType.get("ISAngelECMSuite"));
        angelCarrier.addEquipment(angel, Mek.LOC_LEFT_TORSO, false);
        game.addEntity(angelCarrier, false);
        angelCarrier.setPosition(ENEMY_HEX);
        angelCarrier.setGameOptions();
        assertTrue(angel.setModeImmediately(MiscType.MODE_ECM) >= 0);
        addEcmCarrier(friendlyPlayer, 11, POD_HEX, "ECCM");

        assertTrue(ComputeECM.isAffectedByECM(podMek, POD_HEX, POD_HEX), "standard ECCM cannot negate Angel ECM");
        assertEquals(InterferenceState.BLINDED, VirtualRealityPilotingPod.getInterference(podMek).state());
    }

    // ---- Torso-mounted cockpit protections ----

    @Test
    void podSharesTorsoMountedProtections() {
        assertTrue(podMek.hasTorsoMountedCockpit());
        assertTrue(podMek.hasVirtualRealityPilotingPod());
        assertFalse(podMek.hasEjectSeat(), "a VRPP MekWarrior cannot eject");
        assertFalse(podMek.isEjectionPossible(), "a VRPP MekWarrior cannot eject");

        HitData transferred = podMek.getTransferLocation(new HitData(Mek.LOC_HEAD));
        assertEquals(Entity.LOC_NONE, transferred.getLocation(), "losing the head does not destroy a VRPP Mek");
    }

    @Test
    void torsoMountedCockpitKeepsItsOwnBehaviour() {
        Mek torsoMounted = createMek(Mek.COCKPIT_TORSO_MOUNTED, friendlyPlayer, 3);
        assertTrue(torsoMounted.hasTorsoMountedCockpit());
        assertFalse(torsoMounted.hasVirtualRealityPilotingPod());
        assertFalse(torsoMounted.hasEjectSeat());
    }

    @Test
    void standardCockpitStillDiesWithTheHead() {
        Mek standard = createMek(Mek.COCKPIT_STANDARD, friendlyPlayer, 3);
        assertFalse(standard.hasTorsoMountedCockpit());
        assertTrue(standard.hasEjectSeat());
        HitData transferred = standard.getTransferLocation(new HitData(Mek.LOC_HEAD));
        assertEquals(Entity.LOC_DESTROYED, transferred.getLocation());
    }

    @Test
    void autoResolveCountsLifeSupportHitsInEveryTorsoOfThePod() {
        podMek.addTorsoMountedCockpit(true);
        destroyLifeSupport(podMek, Mek.LOC_LEFT_TORSO);
        destroyLifeSupport(podMek, Mek.LOC_RIGHT_TORSO);
        destroyLifeSupport(podMek, Mek.LOC_CENTER_TORSO);
        assertEquals(3, new MekDamageApplier(podMek, EntityFinalState.ANY).getLifeSupportHits());

        Mek torsoMounted = createMek(Mek.COCKPIT_TORSO_MOUNTED, friendlyPlayer, 3);
        torsoMounted.addTorsoMountedCockpit(false);
        destroyLifeSupport(torsoMounted, Mek.LOC_LEFT_TORSO);
        destroyLifeSupport(torsoMounted, Mek.LOC_RIGHT_TORSO);
        assertEquals(2, new MekDamageApplier(torsoMounted, EntityFinalState.ANY).getLifeSupportHits());
    }

    /** Marks the life support slot in the given location as destroyed. */
    private static void destroyLifeSupport(Mek mek, int location) {
        for (int slotIndex = 0; slotIndex < mek.getNumberOfCriticalSlots(location); slotIndex++) {
            CriticalSlot slot = mek.getCritical(location, slotIndex);
            boolean isLifeSupport = (slot != null) && (slot.getType() == CriticalSlot.TYPE_SYSTEM)
                  && (slot.getIndex() == Mek.SYSTEM_LIFE_SUPPORT);
            if (isLifeSupport) {
                slot.setDestroyed(true);
                return;
            }
        }
        throw new AssertionError("No life support slot in location " + location);
    }

    @Test
    void twoHeadSensorHitsBlindThePodLikeStandardSensors() {
        podMek.addTorsoMountedCockpit(true);
        podMek.getCritical(Mek.LOC_HEAD, 0).setDestroyed(true);
        podMek.getCritical(Mek.LOC_HEAD, 1).setDestroyed(true);

        ToHitData toHit = weaponAttack();
        assertEquals(ToHitData.IMPOSSIBLE, toHit.getValue());
        assertTrue(toHit.getDesc().contains("sensors destroyed"), toHit.getDesc());
    }

    @Test
    void oneHeadSensorHitLeavesThePodFiring() {
        podMek.addTorsoMountedCockpit(true);
        podMek.getCritical(Mek.LOC_HEAD, 0).setDestroyed(true);

        ToHitData toHit = weaponAttack();
        assertNotEquals(ToHitData.IMPOSSIBLE, toHit.getValue());
        assertNull(VirtualRealityPilotingPod.getBlindedReason(podMek));
    }

    /** Extracts the value of the first to-hit modifier whose description starts with the given text. */
    private static int modifierNamed(ToHitData toHit, String descriptionPrefix) {
        for (int index = 0; index < toHit.getModifiers().size(); index++) {
            if (toHit.getModifiers().get(index).getDesc().startsWith(descriptionPrefix)) {
                return toHit.getModifiers().get(index).value();
            }
        }
        throw new AssertionError("No modifier starting with '" + descriptionPrefix + "' in: " + toHit.getDesc());
    }
}
