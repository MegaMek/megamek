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

package megamek.common.planetaryConditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.game.Game;
import megamek.common.planetaryConditions.TaintedAtmosphereRules.VehicleBreachEffect;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the Tainted and Toxic Atmospheres Table of TO:AR p.54, one row at a time.
 */
class TaintedAtmosphereRulesTest {

    @Test
    @DisplayName("Caustic and radiological air are dangerous to people; flammable air is dangerous to the ground")
    void onlyCausticAndRadiologicalAirHarmsPeople() {
        assertTrue(TaintedAtmosphereRules.isHarmfulToPersonnel(AtmosphericTaint.TAINTED_CAUSTIC));
        assertTrue(TaintedAtmosphereRules.isHarmfulToPersonnel(AtmosphericTaint.TOXIC_CAUSTIC));
        assertTrue(TaintedAtmosphereRules.isHarmfulToPersonnel(AtmosphericTaint.TAINTED_POISON));
        assertTrue(TaintedAtmosphereRules.isHarmfulToPersonnel(AtmosphericTaint.TOXIC_POISON));

        assertFalse(TaintedAtmosphereRules.isHarmfulToPersonnel(AtmosphericTaint.TAINTED_FLAME));
        assertFalse(TaintedAtmosphereRules.isHarmfulToPersonnel(AtmosphericTaint.TOXIC_FLAME));
        assertFalse(TaintedAtmosphereRules.isHarmfulToPersonnel(AtmosphericTaint.BREATHABLE));
    }

    @Test
    @DisplayName("Caustic and radiological air demand XCT infantry; flammable air demands nothing")
    void xctInfantryIsRequiredOnlyWhereTheAirHarmsPeople() {
        assertTrue(TaintedAtmosphereRules.requiresXctInfantry(AtmosphericTaint.TAINTED_CAUSTIC));
        assertTrue(TaintedAtmosphereRules.requiresXctInfantry(AtmosphericTaint.TOXIC_POISON));

        assertFalse(TaintedAtmosphereRules.requiresXctInfantry(AtmosphericTaint.TAINTED_FLAME));
        assertFalse(TaintedAtmosphereRules.requiresXctInfantry(AtmosphericTaint.TOXIC_FLAME));
        assertFalse(TaintedAtmosphereRules.requiresXctInfantry(AtmosphericTaint.BREATHABLE));
    }

    @Test
    @DisplayName("Only toxic caustic or radiological air keeps unsealed vehicles off the field")
    void unsealedVehiclesAreBarredOnlyByToxicHarmfulAir() {
        assertTrue(TaintedAtmosphereRules.barsUnsealedVehicles(AtmosphericTaint.TOXIC_CAUSTIC));
        assertTrue(TaintedAtmosphereRules.barsUnsealedVehicles(AtmosphericTaint.TOXIC_POISON));

        assertFalse(TaintedAtmosphereRules.barsUnsealedVehicles(AtmosphericTaint.TAINTED_CAUSTIC));
        assertFalse(TaintedAtmosphereRules.barsUnsealedVehicles(AtmosphericTaint.TAINTED_POISON));
        assertFalse(TaintedAtmosphereRules.barsUnsealedVehicles(AtmosphericTaint.TOXIC_FLAME));
        assertFalse(TaintedAtmosphereRules.barsUnsealedVehicles(AtmosphericTaint.BREATHABLE));
    }

    @Test
    @DisplayName("Vehicles roll for breaches in caustic air and in toxic radiological air, and nowhere else")
    void vehicleBreachesHappenOnlyWhereTheRulesGiveThemAnEffect() {
        assertTrue(TaintedAtmosphereRules.causesVehicleBreaches(AtmosphericTaint.TAINTED_CAUSTIC));
        assertTrue(TaintedAtmosphereRules.causesVehicleBreaches(AtmosphericTaint.TOXIC_CAUSTIC));
        assertTrue(TaintedAtmosphereRules.causesVehicleBreaches(AtmosphericTaint.TOXIC_POISON));

        assertFalse(TaintedAtmosphereRules.causesVehicleBreaches(AtmosphericTaint.BREATHABLE));
        assertFalse(TaintedAtmosphereRules.causesVehicleBreaches(AtmosphericTaint.TAINTED_POISON));
        assertFalse(TaintedAtmosphereRules.causesVehicleBreaches(AtmosphericTaint.TAINTED_FLAME));
        assertFalse(TaintedAtmosphereRules.causesVehicleBreaches(AtmosphericTaint.TOXIC_FLAME));
    }

    @Test
    @DisplayName("A breached vehicle is stunned in caustic tainted air and killed in toxic air")
    void vehicleBreachEffectMatchesTheTable() {
        assertEquals(VehicleBreachEffect.CREW_STUNNED,
              TaintedAtmosphereRules.getVehicleBreachEffect(AtmosphericTaint.TAINTED_CAUSTIC));
        assertEquals(VehicleBreachEffect.CREW_KILLED,
              TaintedAtmosphereRules.getVehicleBreachEffect(AtmosphericTaint.TOXIC_CAUSTIC));
        assertEquals(VehicleBreachEffect.CREW_KILLED,
              TaintedAtmosphereRules.getVehicleBreachEffect(AtmosphericTaint.TOXIC_POISON));
        assertEquals(VehicleBreachEffect.NONE,
              TaintedAtmosphereRules.getVehicleBreachEffect(AtmosphericTaint.TAINTED_POISON));
        assertEquals(VehicleBreachEffect.NONE,
              TaintedAtmosphereRules.getVehicleBreachEffect(AtmosphericTaint.TOXIC_FLAME));
        assertEquals(VehicleBreachEffect.NONE,
              TaintedAtmosphereRules.getVehicleBreachEffect(AtmosphericTaint.BREATHABLE));
    }

    @Test
    @DisplayName("A Mek only rolls for a cockpit breach in toxic air, and never for any other location")
    void mekBreachesAreCockpitOnlyAndToxicOnly() {
        Mek mek = mock(Mek.class);

        assertTrue(TaintedAtmosphereRules.isLocationExposedToTaint(mek, Mek.LOC_HEAD,
              AtmosphericTaint.TOXIC_CAUSTIC));
        assertTrue(TaintedAtmosphereRules.isLocationExposedToTaint(mek, Mek.LOC_HEAD,
              AtmosphericTaint.TOXIC_POISON));

        assertFalse(TaintedAtmosphereRules.isLocationExposedToTaint(mek, Mek.LOC_CENTER_TORSO,
              AtmosphericTaint.TOXIC_CAUSTIC));
        assertFalse(TaintedAtmosphereRules.isLocationExposedToTaint(mek, Mek.LOC_HEAD,
              AtmosphericTaint.TAINTED_CAUSTIC));
        assertFalse(TaintedAtmosphereRules.isLocationExposedToTaint(mek, Mek.LOC_HEAD,
              AtmosphericTaint.TOXIC_FLAME));
    }

    @Test
    @DisplayName("A vehicle exposes every location wherever vehicle breaches are rolled at all")
    void vehicleExposesEveryLocation() {
        Tank tank = mock(Tank.class);

        assertTrue(TaintedAtmosphereRules.isLocationExposedToTaint(tank, Tank.LOC_FRONT,
              AtmosphericTaint.TAINTED_CAUSTIC));
        assertTrue(TaintedAtmosphereRules.isLocationExposedToTaint(tank, Tank.LOC_REAR,
              AtmosphericTaint.TAINTED_CAUSTIC));
        assertFalse(TaintedAtmosphereRules.isLocationExposedToTaint(tank, Tank.LOC_FRONT,
              AtmosphericTaint.TAINTED_FLAME));
    }

    @Test
    @DisplayName("Breathable air exposes nothing at all")
    void breathableAirExposesNothing() {
        Tank tank = mock(Tank.class);
        Mek mek = mock(Mek.class);

        assertFalse(TaintedAtmosphereRules.isLocationExposedToTaint(tank, Tank.LOC_FRONT,
              AtmosphericTaint.BREATHABLE));
        assertFalse(TaintedAtmosphereRules.isLocationExposedToTaint(mek, Mek.LOC_HEAD,
              AtmosphericTaint.BREATHABLE));
    }

    @Test
    @DisplayName("Caustic air burns a pilot a second time through a damaged cockpit, at either strength")
    void extraCockpitCrewHitIsCausticAtEitherStrength() {
        assertTrue(TaintedAtmosphereRules.causesExtraCockpitCrewHit(AtmosphericTaint.TAINTED_CAUSTIC));
        assertTrue(TaintedAtmosphereRules.causesExtraCockpitCrewHit(AtmosphericTaint.TOXIC_CAUSTIC),
              "toxic air is the same taint at a worse level, so it does everything the tainted row does");

        assertFalse(TaintedAtmosphereRules.causesExtraCockpitCrewHit(AtmosphericTaint.TAINTED_POISON));
        assertFalse(TaintedAtmosphereRules.causesExtraCockpitCrewHit(AtmosphericTaint.BREATHABLE));
    }

    @Test
    @DisplayName("Caustic air adds 1D6 to a weapon attack on infantry, at either strength")
    void extraInfantryDamageDiceIsCausticAtEitherStrength() {
        assertEquals(1, TaintedAtmosphereRules.getExtraInfantryAttackDamageDice(AtmosphericTaint.TAINTED_CAUSTIC));
        assertEquals(1, TaintedAtmosphereRules.getExtraInfantryAttackDamageDice(AtmosphericTaint.TOXIC_CAUSTIC),
              "toxic air is the same taint at a worse level, so it does everything the tainted row does");

        assertEquals(0, TaintedAtmosphereRules.getExtraInfantryAttackDamageDice(
              AtmosphericTaint.TAINTED_POISON));
        assertEquals(0, TaintedAtmosphereRules.getExtraInfantryAttackDamageDice(AtmosphericTaint.BREATHABLE));
    }

    @Test
    @DisplayName("Radiological air doubles damage to infantry, at either strength")
    void doubledInfantryDamageIsRadiologicalAtEitherStrength() {
        assertTrue(TaintedAtmosphereRules.doublesInfantryDamage(AtmosphericTaint.TAINTED_POISON));
        assertTrue(TaintedAtmosphereRules.doublesInfantryDamage(AtmosphericTaint.TOXIC_POISON),
              "toxic air is the same taint at a worse level, so it does everything the tainted row does");

        assertFalse(TaintedAtmosphereRules.doublesInfantryDamage(AtmosphericTaint.TAINTED_CAUSTIC));
        assertFalse(TaintedAtmosphereRules.doublesInfantryDamage(AtmosphericTaint.BREATHABLE));
    }

    @Test
    @DisplayName("Flammable air shifts an attack on infantry two rows down the damage table, at either strength")
    void infantryDamageClassShiftIsFlammableAtEitherStrength() {
        assertEquals(2, TaintedAtmosphereRules.getInfantryDamageClassShift(AtmosphericTaint.TAINTED_FLAME));
        assertEquals(2, TaintedAtmosphereRules.getInfantryDamageClassShift(AtmosphericTaint.TOXIC_FLAME),
              "the shift carries up to toxic air, which is what stops it resolving a cluster missile attack "
                    + "more gently than tainted air would");

        assertEquals(0, TaintedAtmosphereRules.getInfantryDamageClassShift(AtmosphericTaint.TAINTED_CAUSTIC));
        assertEquals(0, TaintedAtmosphereRules.getInfantryDamageClassShift(AtmosphericTaint.BREATHABLE));
    }

    @Test
    @DisplayName("Flammable toxic air turns attacks on infantry into infantry-on-infantry damage")
    void infantryOriginDamageIsToxicFlameOnly() {
        assertTrue(TaintedAtmosphereRules.treatsAttacksOnInfantryAsInfantryDamage(AtmosphericTaint.TOXIC_FLAME));

        assertFalse(TaintedAtmosphereRules.treatsAttacksOnInfantryAsInfantryDamage(
              AtmosphericTaint.TAINTED_FLAME));
        assertFalse(TaintedAtmosphereRules.treatsAttacksOnInfantryAsInfantryDamage(AtmosphericTaint.BREATHABLE));
    }

    @Test
    @DisplayName("Jump jets light a flammable toxic hex automatically, but only risk it in tainted air")
    void jumpJetIgnition() {
        assertTrue(TaintedAtmosphereRules.jumpJetsAlwaysIgnite(AtmosphericTaint.TOXIC_FLAME));
        assertFalse(TaintedAtmosphereRules.jumpJetsAlwaysIgnite(AtmosphericTaint.TAINTED_FLAME));

        assertEquals(7, TaintedAtmosphereRules.getJumpIgnitionTarget(false));
        assertEquals(9, TaintedAtmosphereRules.getJumpIgnitionTarget(true));
    }

    @Test
    @DisplayName("A damaged battle armor suit is only lethal in toxic caustic or radiological air")
    void battleArmorSuitBreachIsToxicOnly() {
        assertTrue(TaintedAtmosphereRules.killsBattleArmorInDamagedSuits(AtmosphericTaint.TOXIC_CAUSTIC));
        assertTrue(TaintedAtmosphereRules.killsBattleArmorInDamagedSuits(AtmosphericTaint.TOXIC_POISON));

        assertFalse(TaintedAtmosphereRules.killsBattleArmorInDamagedSuits(AtmosphericTaint.TOXIC_FLAME));
        assertFalse(TaintedAtmosphereRules.killsBattleArmorInDamagedSuits(AtmosphericTaint.TAINTED_CAUSTIC));
        assertFalse(TaintedAtmosphereRules.killsBattleArmorInDamagedSuits(AtmosphericTaint.BREATHABLE));
    }

    @Test
    @DisplayName("HarJel gives a battle armor trooper one more point of grace on the suit breach roll")
    void harJelRaisesTheSuitBreachTarget() {
        assertEquals(9, TaintedAtmosphereRules.getBattleArmorSuitBreachTarget(false));
        assertEquals(10, TaintedAtmosphereRules.getBattleArmorSuitBreachTarget(true));
    }

    @Test
    @DisplayName("Prop, Ultra-Light and VSTOL Fixed-Wing Support vehicles are the exception to jet propulsion")
    void jetPropulsionExemptsTheThreeFixedWingSupportChassis() {
        Entity aerospaceFighter = mock(Entity.class);
        when(aerospaceFighter.isAero()).thenReturn(true);
        assertTrue(TaintedAtmosphereRules.isJetPropelled(aerospaceFighter),
              "an ordinary aerospace unit launches on jet thrust");

        for (String chassisModification : List.of(EquipmentTypeLookup.PROP_CHASSIS_MOD,
              EquipmentTypeLookup.ULTRALIGHT_CHASSIS_MOD,
              EquipmentTypeLookup.VSTOL_CHASSIS_MOD)) {
            Entity fixedWingSupport = mock(Entity.class);
            when(fixedWingSupport.isAero()).thenReturn(true);
            when(fixedWingSupport.hasMisc(chassisModification)).thenReturn(true);
            assertFalse(TaintedAtmosphereRules.isJetPropelled(fixedWingSupport),
                  chassisModification + " is one of the three chassis the rules exempt");
        }

        // A VTOL is a vehicle rather than an aerospace unit, so its rotors never reach this check at all.
        Entity vtol = mock(Entity.class);
        when(vtol.isAero()).thenReturn(false);
        assertFalse(TaintedAtmosphereRules.isJetPropelled(vtol), "a VTOL flies on rotors, not a jet");
    }

    @Test
    @DisplayName("Flammable toxic air keeps jet-propelled craft off the field entirely")
    void jetCraftCannotBeFielded() {
        // Barred outright rather than only when grounded: a fighter that could be fielded but never move, whether
        // deployed that way or left there by a landing, is a puzzle for everyone looking at it.
        Entity fighter = mock(Entity.class);
        when(fighter.isAero()).thenReturn(true);

        assertTrue(TaintedAtmosphereRules.barsJetPropelledCraft(fighter, AtmosphericTaint.TOXIC_FLAME),
              "nothing on jet thrust can operate in flammable toxic air");
        assertFalse(TaintedAtmosphereRules.barsJetPropelledCraft(fighter, AtmosphericTaint.TAINTED_FLAME),
              "only toxic air bars launching");
        assertFalse(TaintedAtmosphereRules.barsJetPropelledCraft(fighter, AtmosphericTaint.BREATHABLE),
              "breathable air bars nothing");

        Entity propellerCraft = mock(Entity.class);
        when(propellerCraft.isAero()).thenReturn(true);
        when(propellerCraft.hasMisc(EquipmentTypeLookup.PROP_CHASSIS_MOD)).thenReturn(true);
        assertFalse(TaintedAtmosphereRules.barsJetPropelledCraft(propellerCraft, AtmosphericTaint.TOXIC_FLAME),
              "a propeller-driven support vehicle may still fly");
    }

    @Test
    @DisplayName("Only flammable toxic air bars jet-propelled units from launching")
    void launchProhibitionIsToxicFlameOnly() {
        assertTrue(TaintedAtmosphereRules.prohibitsLaunching(AtmosphericTaint.TOXIC_FLAME));

        assertFalse(TaintedAtmosphereRules.prohibitsLaunching(AtmosphericTaint.TAINTED_FLAME));
        assertFalse(TaintedAtmosphereRules.prohibitsLaunching(AtmosphericTaint.BREATHABLE));
    }

    @Test
    @DisplayName("Only flammable toxic air spreads an explosive fire to every adjacent hex")
    void explosiveFireSpreadIsToxicFlameOnly() {
        assertTrue(TaintedAtmosphereRules.spreadsExplosiveFiresInstantly(AtmosphericTaint.TOXIC_FLAME));

        assertFalse(TaintedAtmosphereRules.spreadsExplosiveFiresInstantly(AtmosphericTaint.TAINTED_FLAME));
        assertFalse(TaintedAtmosphereRules.spreadsExplosiveFiresInstantly(AtmosphericTaint.BREATHABLE));
    }

    @Test
    @DisplayName("The exposure limits are the 30 and 90 turns the rules give")
    void exposureLimits() {
        assertEquals(30, TaintedAtmosphereRules.INFANTRY_FIELD_EXPOSURE_LIMIT_TURNS);
        assertEquals(90, TaintedAtmosphereRules.VEHICLE_FIELD_EXPOSURE_LIMIT_TURNS);
    }

    @Test
    @DisplayName("A unit that is neither carried nor in a building is out in the open")
    void anUncarriedUnitIsNotSheltered() {
        Entity entity = mock(Entity.class);
        when(entity.getTransportId()).thenReturn(Entity.NONE);
        when(entity.getGame()).thenReturn(null);

        assertFalse(TaintedAtmosphereRules.isShelteredFromAtmosphere(entity));
    }

    @Test
    @DisplayName("A unit riding in a sealed transport is sheltered; an unsealed one is not")
    void aSealedTransportShelters() {
        Game game = mock(Game.class);
        Entity sealedTransport = mock(Entity.class);
        when(sealedTransport.hasEnvironmentalSealing()).thenReturn(true);
        Entity unsealedTransport = mock(Entity.class);
        when(unsealedTransport.hasEnvironmentalSealing()).thenReturn(false);
        when(game.getEntity(1)).thenReturn(sealedTransport);
        when(game.getEntity(2)).thenReturn(unsealedTransport);

        Entity passengerOfSealed = mock(Entity.class);
        when(passengerOfSealed.getTransportId()).thenReturn(1);
        when(passengerOfSealed.getGame()).thenReturn(game);
        assertTrue(TaintedAtmosphereRules.isShelteredFromAtmosphere(passengerOfSealed));

        Entity passengerOfUnsealed = mock(Entity.class);
        when(passengerOfUnsealed.getTransportId()).thenReturn(2);
        when(passengerOfUnsealed.getGame()).thenReturn(game);
        assertFalse(TaintedAtmosphereRules.isShelteredFromAtmosphere(passengerOfUnsealed));
    }

    @Test
    @DisplayName("A hot unit can set its own hex alight in flammable air at either strength")
    void spontaneousIgnitionAppliesToBothFlammableStrengths() {
        // Printed on the tainted row only, like the exhaust wash, and read here the same way.
        assertTrue(TaintedAtmosphereRules.causesSpontaneousIgnition(AtmosphericTaint.TAINTED_FLAME));
        assertTrue(TaintedAtmosphereRules.causesSpontaneousIgnition(AtmosphericTaint.TOXIC_FLAME));

        assertFalse(TaintedAtmosphereRules.causesSpontaneousIgnition(AtmosphericTaint.BREATHABLE));
        assertFalse(TaintedAtmosphereRules.causesSpontaneousIgnition(AtmosphericTaint.TAINTED_CAUSTIC));
        assertFalse(TaintedAtmosphereRules.causesSpontaneousIgnition(AtmosphericTaint.TOXIC_POISON));
    }

    @Test
    @DisplayName("A craft's exhaust can set the ground alight in flammable air at either strength")
    void exhaustWashAppliesToBothFlammableStrengths() {
        // The book gives this rule on the tainted row only and then bars launching on the toxic row, which would
        // leave a craft landing in the worse air scorching nothing. Treated here as an oversight.
        assertTrue(TaintedAtmosphereRules.causesExhaustWashIgnition(AtmosphericTaint.TAINTED_FLAME));
        assertTrue(TaintedAtmosphereRules.causesExhaustWashIgnition(AtmosphericTaint.TOXIC_FLAME));

        assertFalse(TaintedAtmosphereRules.causesExhaustWashIgnition(AtmosphericTaint.BREATHABLE));
        assertFalse(TaintedAtmosphereRules.causesExhaustWashIgnition(AtmosphericTaint.TAINTED_CAUSTIC));
        assertFalse(TaintedAtmosphereRules.causesExhaustWashIgnition(AtmosphericTaint.TOXIC_POISON));
    }

    @Test
    @DisplayName("A craft's exhaust washes over the hexes behind it, out to two, and never its own hex")
    void exhaustWashCoversTheRearArcOnly() {
        Coords origin = new Coords(5, 5);
        List<Coords> washed = TaintedAtmosphereRules.getExhaustWashCoords(origin, 0);

        assertFalse(washed.contains(origin), "the craft's own hex is not washed by its own exhaust");
        for (Coords washedCoords : washed) {
            assertTrue(origin.distance(washedCoords) <= 2,
                  washedCoords + " should be within two hexes of the craft");
            assertTrue(ComputeArc.isInArc(origin, 0, washedCoords, Compute.ARC_REAR),
                  washedCoords + " should be behind a craft facing north");
        }
        assertFalse(washed.isEmpty(), "a craft on open ground should wash some hexes behind it");
    }

    @Test
    @DisplayName("The hex directly behind a craft is washed and the hex directly ahead of it is not")
    void exhaustWashGoesBackwardsNotForwards() {
        Coords origin = new Coords(5, 5);
        // Facing 0 is north, so hexside 3 is directly astern and hexside 0 is dead ahead.
        Coords directlyBehind = origin.translated(3);
        Coords directlyAhead = origin.translated(0);

        List<Coords> washed = TaintedAtmosphereRules.getExhaustWashCoords(origin, 0);

        assertTrue(washed.contains(directlyBehind), "the hex immediately astern should be washed");
        assertFalse(washed.contains(directlyAhead), "the hex dead ahead should not be washed");
        assertTrue(washed.contains(directlyBehind.translated(3)),
              "the wash should reach two hexes astern as well");
    }

    @Test
    @DisplayName("Turning the craft turns the patch of ground its exhaust washes")
    void exhaustWashFollowsTheFacing() {
        Coords origin = new Coords(5, 5);

        List<Coords> facingNorth = TaintedAtmosphereRules.getExhaustWashCoords(origin, 0);
        List<Coords> facingSouth = TaintedAtmosphereRules.getExhaustWashCoords(origin, 3);

        assertEquals(facingNorth.size(), facingSouth.size(),
              "the shape of the wash should not change with the facing, only its direction");
        assertTrue(facingNorth.contains(origin.translated(3)), "facing north washes the hex to the south");
        assertTrue(facingSouth.contains(origin.translated(0)), "facing south washes the hex to the north");
    }

    @Test
    @DisplayName("A unit's exposure clock starts at zero and counts up one turn at a time")
    void exposureClockCountsUp() {
        Tank tank = new Tank();

        assertEquals(0, tank.getTaintedAtmosphereExposureTurns());
        assertEquals(1, tank.advanceTaintedAtmosphereExposure());
        assertEquals(2, tank.advanceTaintedAtmosphereExposure());
        assertEquals(2, tank.getTaintedAtmosphereExposureTurns());
    }
}
