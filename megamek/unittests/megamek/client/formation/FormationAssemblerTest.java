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
package megamek.client.formation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import megamek.client.formation.AssemblyUnit.Family;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitRole;
import org.junit.jupiter.api.Test;

/**
 * Pins the assembly algorithm against the two design-test rosters (2026-08-08) and the structural rules
 * around them: atoms are never split, aerospace never mixes down, the size ladder never orphans a pair,
 * and the same roster always assembles the same way.
 *
 * <p>The rosters use synthetic {@link MekSummary} data carrying the real units' catalog stats, because
 * the CI test environment has no unit cache to look them up in. What the fixtures pin is the
 * ALGORITHM's choices on realistic data - role purity beating weight grouping, the C3 pair staying
 * together - not the catalog itself.</p>
 */
class FormationAssemblerTest {

    private int nextId = 1;

    // ======================== Fixture building ========================

    private AssemblyUnit mek(String name, UnitRole role, int weightClass, int walkMp, int battleValue) {
        return unit(name, role, weightClass, walkMp, battleValue, false, false, null, Family.MEK);
    }

    private AssemblyUnit clanMek(String name, UnitRole role, int weightClass, int walkMp, int battleValue) {
        return unit(name, role, weightClass, walkMp, battleValue, false, true, null, Family.MEK);
    }

    private AssemblyUnit unit(String name, UnitRole role, int weightClass, int walkMp, int battleValue,
          boolean ecm, boolean clan, String c3Network, Family family) {
        MekSummary summary = new MekSummary();
        summary.setName(name);
        // qualifies() groups by chassis; the full fixture name stands in for one.
        summary.setChassis(name);
        summary.setModel("");
        summary.setUnitType(switch (family) {
            case MEK -> "Mek";
            case VEHICLE -> "Tank";
            case INFANTRY -> "BattleArmor";
            case AERO -> "AeroSpaceFighter";
        });
        summary.setUnitRole(role);
        summary.setWeightClass(weightClass);
        summary.setWalkMp(walkMp);
        summary.setBV(battleValue);
        // Cache-loaded summaries always carry an equipment list; several formation criteria stream it.
        summary.setEquipment(List.of());
        return new AssemblyUnit(nextId++, name, role, weightClass, walkMp, battleValue, ecm, clan,
              c3Network, Entity.NONE, Entity.NONE, family, summary);
    }

    private static AssembledFormation formationContaining(List<AssembledFormation> formations, String unitName) {
        for (AssembledFormation formation : formations) {
            for (AssemblyUnit unit : formation.units()) {
                if (unit.displayName().equals(unitName)) {
                    return formation;
                }
            }
        }
        throw new AssertionError("No formation contains " + unitName);
    }

    // ======================== The two design-test rosters ========================

    /**
     * The Clan design roster: five unanimous-Striker OmniMeks against five 65-75 ton 5/8 movers. The
     * pin is the Loki call - by weight it belongs with the heavies, by role with the strikers, and role
     * purity must win.
     */
    private List<AssemblyUnit> clanRoster() {
        List<AssemblyUnit> roster = new ArrayList<>();
        // The heavy star: 65-75 tons, all walk 5 - speed spread zero.
        roster.add(clanMek("Mad Cat Prime", UnitRole.BRAWLER, EntityWeightClass.WEIGHT_HEAVY, 5, 2737));
        roster.add(clanMek("Mad Cat A", UnitRole.BRAWLER, EntityWeightClass.WEIGHT_HEAVY, 5, 2606));
        roster.add(clanMek("Thor Prime", UnitRole.SKIRMISHER, EntityWeightClass.WEIGHT_HEAVY, 5, 2374));
        roster.add(clanMek("Crossbow Prime", UnitRole.MISSILE_BOAT, EntityWeightClass.WEIGHT_HEAVY, 5, 2283));
        roster.add(clanMek("Crossbow A", UnitRole.MISSILE_BOAT, EntityWeightClass.WEIGHT_HEAVY, 5, 2058));
        // The striker star: unanimous STRIKER role across mixed weights and speeds.
        roster.add(clanMek("Loki Prime", UnitRole.STRIKER, EntityWeightClass.WEIGHT_HEAVY, 6, 2378));
        roster.add(clanMek("Ryoken Prime", UnitRole.STRIKER, EntityWeightClass.WEIGHT_MEDIUM, 6, 2244));
        roster.add(clanMek("Shadow Cat Prime", UnitRole.STRIKER, EntityWeightClass.WEIGHT_MEDIUM, 6, 2049));
        roster.add(clanMek("Fenris Prime", UnitRole.STRIKER, EntityWeightClass.WEIGHT_MEDIUM, 8, 1889));
        roster.add(clanMek("Pouncer Prime", UnitRole.STRIKER, EntityWeightClass.WEIGHT_MEDIUM, 7, 1998));
        return roster;
    }

    @Test
    void clanRosterSplitsIntoStrikerAndBattleStarsByRolePurity() {
        List<AssembledFormation> formations =
              FormationAssembler.assemble(clanRoster(), Organization.AUTO, Set.of());

        assertEquals(2, formations.size());
        AssembledFormation strikerStar = formationContaining(formations, "Loki Prime");
        assertEquals(Set.of("Loki Prime", "Ryoken Prime", "Shadow Cat Prime", "Fenris Prime", "Pouncer Prime"),
              new HashSet<>(strikerStar.units().stream().map(AssemblyUnit::displayName).toList()),
              "role purity must pull the heavy Loki into the striker star, not the weight-matched heavies");
        assertTrue(strikerStar.name().contains("Star"),
              "majority Clan tech must auto-detect Star doctrine, got: " + strikerStar.name());
        AssembledFormation battleStar = formationContaining(formations, "Mad Cat Prime");
        assertEquals(5, battleStar.units().size());
    }

    /**
     * The IS design roster: a fire-support core, a battle core, and a C3-slave pair that must never be
     * separated whatever the score says.
     */
    private List<AssemblyUnit> innerSphereRoster() {
        List<AssemblyUnit> roster = new ArrayList<>();
        roster.add(mek("Dervish DV-6M", UnitRole.MISSILE_BOAT, EntityWeightClass.WEIGHT_MEDIUM, 5, 1146));
        roster.add(mek("Trebuchet TBT-5N", UnitRole.MISSILE_BOAT, EntityWeightClass.WEIGHT_MEDIUM, 5, 1191));
        roster.add(mek("Hellspawn HSN-7D", UnitRole.MISSILE_BOAT, EntityWeightClass.WEIGHT_MEDIUM, 6, 1119));
        roster.add(mek("Bushwacker BSW-X1", UnitRole.SNIPER, EntityWeightClass.WEIGHT_MEDIUM, 5, 1174));
        roster.add(mek("Black Knight BL-6-KNT", UnitRole.BRAWLER, EntityWeightClass.WEIGHT_HEAVY, 4, 1443));
        roster.add(mek("Maelstrom MTR-5K", UnitRole.BRAWLER, EntityWeightClass.WEIGHT_HEAVY, 4, 1602));
        roster.add(mek("Enforcer ENF-4R", UnitRole.SKIRMISHER, EntityWeightClass.WEIGHT_MEDIUM, 4, 1032));
        roster.add(mek("Centurion CN9-A", UnitRole.SKIRMISHER, EntityWeightClass.WEIGHT_MEDIUM, 4, 945));
        roster.add(mek("Shadow Hawk IIC", UnitRole.SKIRMISHER, EntityWeightClass.WEIGHT_MEDIUM, 5, 1358));
        // The C3 pair: a fast striker and a scout on one network - an atom, never separated.
        roster.add(unit("Shadow Hawk SHD-9D", UnitRole.STRIKER, EntityWeightClass.WEIGHT_MEDIUM, 6,
              1520, false, false, "C3i|net-1", Family.MEK));
        roster.add(unit("Firestarter FS9-S", UnitRole.SCOUT, EntityWeightClass.WEIGHT_LIGHT, 6,
              1043, true, false, "C3i|net-1", Family.MEK));
        roster.add(mek("Blackjack BJ-3", UnitRole.SNIPER, EntityWeightClass.WEIGHT_MEDIUM, 4, 1148));
        return roster;
    }

    @Test
    void innerSphereRosterKeepsTheC3PairTogetherInThreeLances() {
        List<AssembledFormation> formations =
              FormationAssembler.assemble(innerSphereRoster(), Organization.AUTO, Set.of());

        assertEquals(3, formations.size());
        for (AssembledFormation formation : formations) {
            assertEquals(4, formation.units().size());
            assertTrue(formation.name().contains("Lance"),
                  "majority IS tech must auto-detect Lance doctrine, got: " + formation.name());
        }
        AssembledFormation pairLance = formationContaining(formations, "Shadow Hawk SHD-9D");
        assertEquals(pairLance, formationContaining(formations, "Firestarter FS9-S"),
              "the C3 network is an atom: its members must land in the same formation");
        // The fire-support core seeds one lance around the modal MISSILE_BOAT role.
        AssembledFormation fireLance = formationContaining(formations, "Dervish DV-6M");
        long boats = fireLance.units().stream().filter(u -> u.role() == UnitRole.MISSILE_BOAT).count();
        assertEquals(3, boats, "the three missile boats must stay together as the fire lance core; got "
              + formations);
    }

    // ======================== Structural rules ========================

    @Test
    void sameRosterAlwaysAssemblesTheSameWay() {
        List<AssembledFormation> first =
              FormationAssembler.assemble(innerSphereRoster(), Organization.INNER_SPHERE, Set.of());
        nextId = 1;
        List<AssembledFormation> second =
              FormationAssembler.assemble(innerSphereRoster(), Organization.INNER_SPHERE, Set.of());
        assertEquals(first.toString(), second.toString(),
              "assembly must be deterministic: same roster, same formations, same names");
    }

    @Test
    void tinyForcesFormOneFormationAndOversizeSingletonsStandAlone() {
        // 1-2 units: one formation, today's behavior untouched.
        List<AssemblyUnit> pair = List.of(
              mek("Atlas AS7-D", UnitRole.JUGGERNAUT, EntityWeightClass.WEIGHT_ASSAULT, 3, 1897),
              mek("Stalker STK-3F", UnitRole.JUGGERNAUT, EntityWeightClass.WEIGHT_ASSAULT, 3, 1559));
        assertEquals(1, FormationAssembler.assemble(pair, Organization.INNER_SPHERE, Set.of()).size());

        // 3 to element size: still one understrength formation.
        List<AssemblyUnit> trio = new ArrayList<>(pair);
        trio.add(mek("Awesome AWS-8Q", UnitRole.SNIPER, EntityWeightClass.WEIGHT_ASSAULT, 3, 1605));
        assertEquals(1, FormationAssembler.assemble(trio, Organization.INNER_SPHERE, Set.of()).size());
    }

    @Test
    void sevenUnitsSplitFourAndThreeNeverAnOrphanPair() {
        List<AssemblyUnit> seven = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            seven.add(mek("Griffin GRF-1N " + i, UnitRole.SKIRMISHER, EntityWeightClass.WEIGHT_MEDIUM,
                  5, 1272));
        }
        List<AssembledFormation> formations =
              FormationAssembler.assemble(seven, Organization.INNER_SPHERE, Set.of());
        List<Integer> sizes = formations.stream().map(f -> f.units().size()).sorted().toList();
        assertEquals(List.of(3, 4), sizes, "7 units under lance doctrine split 4+3, never 5+2 or 6+1");
    }

    @Test
    void aerospaceNeverMixesDownAndFormsItsOwnAirElement() {
        List<AssemblyUnit> mixed = new ArrayList<>(innerSphereRoster());
        mixed.add(unit("Stuka STU-K5", UnitRole.INTERCEPTOR, EntityWeightClass.WEIGHT_HEAVY, 5, 1500,
              false, false, null, Family.AERO));
        mixed.add(unit("Slayer SL-15", UnitRole.INTERCEPTOR, EntityWeightClass.WEIGHT_HEAVY, 5, 1400,
              false, false, null, Family.AERO));

        List<AssembledFormation> formations =
              FormationAssembler.assemble(mixed, Organization.INNER_SPHERE, Set.of());
        AssembledFormation airElement = formationContaining(formations, "Stuka STU-K5");
        assertEquals(2, airElement.units().size(), "fighters form their own air element, never a ground lance");
        assertTrue(airElement.units().stream().allMatch(u -> u.family() == Family.AERO));
        assertTrue(airElement.name().startsWith("Air "), "the air element is named as such: " + airElement.name());
    }

    @Test
    void aSmallVehicleFamilyFoldsIntoTheMekPoolInsteadOfOrphaning() {
        List<AssemblyUnit> force = new ArrayList<>(innerSphereRoster());
        force.add(unit("Vedette Medium Tank", UnitRole.BRAWLER, EntityWeightClass.WEIGHT_MEDIUM, 5, 475,
              false, false, null, Family.VEHICLE));
        force.add(unit("Scorpion Light Tank", UnitRole.AMBUSHER, EntityWeightClass.WEIGHT_LIGHT, 4, 306,
              false, false, null, Family.VEHICLE));

        List<AssembledFormation> formations =
              FormationAssembler.assemble(force, Organization.INNER_SPHERE, Set.of());
        int assigned = formations.stream().mapToInt(f -> f.units().size()).sum();
        assertEquals(14, assigned, "two lonely tanks fold into the ground pool rather than orphaning");
        AssembledFormation tankFormation = formationContaining(formations, "Vedette Medium Tank");
        assertTrue(tankFormation.units().stream().anyMatch(u -> u.family() == Family.MEK),
              "the folded tanks share a formation with Meks instead of standing alone as a pair");
    }

    @Test
    void transportPairIsAnAtomBattleArmorStaysWithItsRide() {
        List<AssemblyUnit> force = new ArrayList<>(clanRoster());
        AssemblyUnit carrier = force.get(0);
        MekSummary baSummary = new MekSummary();
        baSummary.setName("Elemental [Laser]");
        baSummary.setChassis("Elemental");
        baSummary.setModel("[Laser]");
        baSummary.setUnitType("BattleArmor");
        baSummary.setUnitRole(UnitRole.AMBUSHER);
        baSummary.setWeightClass(EntityWeightClass.WEIGHT_LIGHT);
        baSummary.setWalkMp(1);
        baSummary.setBV(322);
        baSummary.setEquipment(List.of());
        force.add(new AssemblyUnit(nextId++, "Elemental [Laser]", UnitRole.AMBUSHER,
              EntityWeightClass.WEIGHT_LIGHT, 1, 322, false, true, null, carrier.entityId(),
              Entity.NONE, Family.INFANTRY, baSummary));

        List<AssembledFormation> formations =
              FormationAssembler.assemble(force, Organization.CLAN, Set.of());
        assertEquals(formationContaining(formations, carrier.displayName()),
              formationContaining(formations, "Elemental [Laser]"),
              "mechanized battle armor lands in its carrier's formation by construction");
    }

    @Test
    void namesSkipTakenSuffixesSoReassemblyNeverCollides() {
        List<AssemblyUnit> seven = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            seven.add(mek("Wolverine WVR-6R " + i, UnitRole.SKIRMISHER, EntityWeightClass.WEIGHT_MEDIUM,
                  5, 1101));
        }
        Set<String> firstNames = FormationAssembler.assemble(seven, Organization.INNER_SPHERE, Set.of())
              .stream().map(AssembledFormation::name).collect(Collectors.toSet());
        assertEquals(2, firstNames.size());

        nextId = 100;
        List<AssemblyUnit> secondSeven = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            secondSeven.add(mek("Wolverine WVR-6R " + (7 + i), UnitRole.SKIRMISHER,
                  EntityWeightClass.WEIGHT_MEDIUM, 5, 1101));
        }
        List<AssembledFormation> second =
              FormationAssembler.assemble(secondSeven, Organization.INNER_SPHERE, firstNames);
        for (AssembledFormation formation : second) {
            assertFalse(firstNames.contains(formation.name()),
                  "re-assembly must skip names already in the lobby, got: " + formation.name());
        }
        assertEquals(2, second.stream().map(AssembledFormation::name).distinct().count());
    }

    @Test
    void comStarDoctrineNamesLevelTwosWithoutTypePrefix() {
        List<AssemblyUnit> six = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            six.add(mek("Crockett CRK-5003-1 " + i, UnitRole.JUGGERNAUT, EntityWeightClass.WEIGHT_ASSAULT,
                  4, 1725));
        }
        List<AssembledFormation> formations =
              FormationAssembler.assemble(six, Organization.COMSTAR, Set.of());
        assertEquals(1, formations.size());
        assertEquals("Level II Alpha", formations.getFirst().name());
        assertEquals(6, formations.getFirst().units().size());
    }

    @Test
    void aUnitWithoutACatalogEntryStillAssembles() {
        List<AssemblyUnit> force = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            force.add(mek("Marauder MAD-3R " + i, UnitRole.SNIPER, EntityWeightClass.WEIGHT_HEAVY, 4, 1363));
        }
        force.add(new AssemblyUnit(nextId++, "Custom Frankenmek", UnitRole.UNDETERMINED,
              EntityWeightClass.WEIGHT_HEAVY, 4, 1200, false, false, null, Entity.NONE, Entity.NONE,
              Family.MEK, null));

        List<AssembledFormation> formations =
              FormationAssembler.assemble(force, Organization.INNER_SPHERE, Set.of());
        assertEquals(1, formations.size());
        assertEquals(4, formations.getFirst().units().size(), "a cache miss never drops a unit");
        assertNotNull(formations.getFirst().name());
    }

    @Test
    void everyInputUnitLandsInExactlyOneFormation() {
        List<AssemblyUnit> roster = innerSphereRoster();
        List<AssembledFormation> formations =
              FormationAssembler.assemble(roster, Organization.AUTO, Set.of());
        List<Integer> assignedIds = formations.stream()
              .flatMap(f -> f.units().stream()).map(AssemblyUnit::entityId).sorted().toList();
        assertEquals(roster.stream().map(AssemblyUnit::entityId).sorted().toList(), assignedIds);
    }
}
