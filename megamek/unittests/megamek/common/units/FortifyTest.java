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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import megamek.common.GameBoardTestCase;
import megamek.common.Hex;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.exceptions.LocationFullException;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Trench/Fieldworks Engineers and digging-in rules (TO:AR p.106 / TO:AUE p.153): the damage-extension of
 * the fortify effort, the terrain restrictions, and the fieldworks-equipment gate.
 */
public class FortifyTest extends GameBoardTestCase {

    static {
        initializeBoard("FORTIFY_BOARD", """
              size 2 2
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              end""");
        initializeBoard("FORTIFY_WATER_BOARD", """
              size 2 2
              hex 0101 0 "water:1" ""
              hex 0201 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              end""");
    }

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @SuppressWarnings("unchecked")
    private static <T> T serializeRoundTrip(T object) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(object);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    @Nested
    @DisplayName("FortifyState damage tracker")
    class FortifyStateLogic {

        @Test
        @DisplayName("The first checkpoint never reports damage")
        void firstCheckpointIsNeverDamage() {
            FortifyState state = new FortifyState();
            state.begin(10);
            assertFalse(state.checkpointWasDamaged(10), "An undamaged first checkpoint is not damage");
        }

        @Test
        @DisplayName("A drop in the health signature is reported as damage exactly once")
        void dropIsReportedAsDamage() {
            FortifyState state = new FortifyState();
            state.begin(10);
            assertTrue(state.checkpointWasDamaged(8), "A reduced signature means the unit took damage");
            assertFalse(state.checkpointWasDamaged(8), "A steady signature next turn is not new damage");
        }

        @Test
        @DisplayName("Resetting clears the baseline so the next checkpoint is treated as the first")
        void resetClearsBaseline() {
            FortifyState state = new FortifyState();
            state.begin(10);
            state.reset();
            assertFalse(state.checkpointWasDamaged(5), "After a reset there is no baseline to compare against");
        }

        @Test
        @DisplayName("The extended flag mirrors the most recent checkpoint result")
        void extendedFlagMirrorsLastCheckpoint() {
            FortifyState state = new FortifyState();
            state.begin(10);
            assertFalse(state.wasExtendedAtLastCheckpoint(), "No checkpoint has run yet");

            state.checkpointWasDamaged(8);
            assertTrue(state.wasExtendedAtLastCheckpoint(), "The last checkpoint detected damage");

            state.checkpointWasDamaged(8);
            assertFalse(state.wasExtendedAtLastCheckpoint(), "The last checkpoint detected no new damage");
        }
    }

    @Nested
    @DisplayName("Infantry fortification")
    class InfantryFortification {

        private ConvInfantry fortifyingInfantry() {
            ConvInfantry infantry = new ConvInfantry();
            infantry.setId(2);
            infantry.setWeight(2.0f);
            infantry.initializeInternal(10, ConvInfantry.LOC_INFANTRY);
            infantry.setGame(getGame());
            infantry.setCrew(new Crew(CrewType.INFANTRY_CREW));
            return infantry;
        }

        @Test
        @DisplayName("With no damage, fortification advances to completion in three turns")
        void undamagedFortifyAdvances() {
            ConvInfantry infantry = fortifyingInfantry();
            infantry.beginFortify();
            assertEquals(Infantry.DUG_IN_FORTIFYING1, infantry.getDugIn(), "First turn enters stage 1");

            infantry.newRound(1);
            assertEquals(Infantry.DUG_IN_FORTIFYING2, infantry.getDugIn(), "Second turn advances to stage 2");

            infantry.newRound(2);
            assertEquals(Infantry.DUG_IN_FORTIFYING3, infantry.getDugIn(),
                  "Third turn reaches stage 3 (completion is resolved at end phase)");
        }

        @Test
        @DisplayName("getFortifyStage reports 1..3 across the build and 0 otherwise (drives the hex indicator)")
        void fortifyStageTracksProgress() {
            ConvInfantry infantry = fortifyingInfantry();
            assertFalse(infantry.isFortifying(), "Not fortifying before it starts");
            assertEquals(0, infantry.getFortifyStage(), "Stage is 0 when not fortifying");
            assertEquals(3, infantry.getFortifyTotalStages(), "A fortified hex takes three stages");

            infantry.beginFortify();
            assertTrue(infantry.isFortifying(), "Fortifying after starting");
            assertEquals(1, infantry.getFortifyStage(), "Stage 1 after starting");

            infantry.newRound(1);
            assertEquals(2, infantry.getFortifyStage(), "Stage 2 after one clean turn");

            infantry.newRound(2);
            assertEquals(3, infantry.getFortifyStage(), "Stage 3 on the final turn");
        }

        @Test
        @DisplayName("Damage during a fortifying turn extends the effort by one turn")
        void damageExtendsFortify() {
            ConvInfantry infantry = fortifyingInfantry();
            infantry.beginFortify();

            // Took casualties this turn: the next round must NOT advance the progress counter.
            infantry.setInternal(8, ConvInfantry.LOC_INFANTRY);
            infantry.newRound(1);
            assertEquals(Infantry.DUG_IN_FORTIFYING1, infantry.getDugIn(),
                  "A damaged turn holds the unit at stage 1");

            // No further damage: progress resumes.
            infantry.newRound(2);
            assertEquals(Infantry.DUG_IN_FORTIFYING2, infantry.getDugIn(),
                  "With no further damage, the effort resumes advancing");
        }

        @Test
        @DisplayName("The extended-this-round flag tracks whether the effort was held")
        void extendedThisRoundFlagTracksDamage() {
            ConvInfantry infantry = fortifyingInfantry();
            infantry.beginFortify();

            infantry.setInternal(8, ConvInfantry.LOC_INFANTRY);
            infantry.newRound(1);
            assertTrue(infantry.isFortifyExtendedThisRound(), "The damaged turn should be flagged as extended");

            infantry.newRound(2);
            assertFalse(infantry.isFortifyExtendedThisRound(), "A clean turn should not be flagged as extended");
        }

        @Test
        @DisplayName("Damage every turn never advances the fortification")
        void repeatedDamageNeverAdvances() {
            ConvInfantry infantry = fortifyingInfantry();
            infantry.beginFortify();

            infantry.setInternal(9, ConvInfantry.LOC_INFANTRY);
            infantry.newRound(1);
            infantry.setInternal(8, ConvInfantry.LOC_INFANTRY);
            infantry.newRound(2);
            infantry.setInternal(7, ConvInfantry.LOC_INFANTRY);
            infantry.newRound(3);

            assertEquals(Infantry.DUG_IN_FORTIFYING1, infantry.getDugIn(),
                  "Taking damage every turn never lets the effort advance");
        }

        @Test
        @DisplayName("Self digging in completes in one turn, but a damaged turn delays it")
        void selfDigInRespectsDamage() {
            ConvInfantry undamaged = fortifyingInfantry();
            undamaged.beginDigIn(false);
            assertEquals(Infantry.DUG_IN_WORKING, undamaged.getDugIn(), "Digging in starts in the working state");
            undamaged.newRound(1);
            assertEquals(Infantry.DUG_IN_COMPLETE, undamaged.getDugIn(), "One clean turn completes the dig-in");

            ConvInfantry damaged = fortifyingInfantry();
            damaged.beginDigIn(false);
            damaged.setInternal(8, ConvInfantry.LOC_INFANTRY);
            damaged.newRound(1);
            assertEquals(Infantry.DUG_IN_WORKING, damaged.getDugIn(),
                  "A unit attacked while digging in is not considered dug in yet");
        }
    }

    @Nested
    @DisplayName("Vehicle fortification")
    class VehicleFortification {

        private Tank fortifyingTank() {
            Tank tank = new Tank();
            tank.setId(3);
            tank.setCrew(new Crew(CrewType.CREW));
            tank.initializeArmor(10, Tank.LOC_FRONT);
            tank.setGame(getGame());
            return tank;
        }

        @Test
        @DisplayName("With no damage, vehicle fortification advances each turn")
        void undamagedVehicleFortifyAdvances() {
            Tank tank = fortifyingTank();
            tank.beginFortify();
            assertEquals(1, tank.getFortifyStage(), "First turn enters stage 1");

            tank.newRound(1);
            assertEquals(2, tank.getFortifyStage(), "Second turn advances to stage 2");

            tank.newRound(2);
            assertEquals(3, tank.getFortifyStage(), "Third turn reaches stage 3");
            assertTrue(tank.isFortifyOnFinalStage(), "Stage 3 is the final stage (completed at end phase)");
        }

        @Test
        @DisplayName("Damage during a fortifying turn extends the vehicle's effort by one turn")
        void damageExtendsVehicleFortify() {
            Tank tank = fortifyingTank();
            tank.beginFortify();

            tank.setArmor(4, Tank.LOC_FRONT);
            tank.newRound(1);
            assertEquals(1, tank.getFortifyStage(), "A damaged turn holds the vehicle at stage 1");

            tank.newRound(2);
            assertEquals(2, tank.getFortifyStage(), "With no further damage, the effort resumes");
        }

        @Test
        @DisplayName("A deserialized vehicle can still fortify (regression: null FortifyState NPE)")
        void deserializedVehicleCanFortify() throws Exception {
            Tank restored = serializeRoundTrip(fortifyingTank());
            assertNotNull(restored.getFortifyState(), "Fortify state is recreated after deserialization");
            restored.beginFortify();
            assertEquals(1, restored.getFortifyStage(), "A deserialized vehicle begins fortifying without an NPE");
        }
    }

    @Nested
    @DisplayName("Mek fortification (backhoe / fieldworks, TO:AUE p.153, Corrected Sixth Printing)")
    class MekFortification {

        private BipedMek fortifyingMek() {
            BipedMek mek = new BipedMek();
            mek.setId(4);
            mek.setCrew(new Crew(CrewType.SINGLE));
            for (int location = 0; location < mek.locations(); location++) {
                mek.initializeArmor(10, location);
                mek.initializeInternal(5, location);
            }
            mek.setGame(getGame());
            return mek;
        }

        @Test
        @DisplayName("A Mek shares the vehicle-style fortify machine: it advances each clean turn")
        void mekFortifyAdvances() {
            BipedMek mek = fortifyingMek();
            mek.beginFortify();
            assertEquals(1, mek.getFortifyStage(), "First turn enters stage 1");

            mek.newRound(1);
            assertEquals(2, mek.getFortifyStage(), "Second turn advances to stage 2");

            mek.newRound(2);
            assertEquals(3, mek.getFortifyStage(), "Third turn reaches stage 3");
            assertTrue(mek.isFortifyOnFinalStage(), "Stage 3 is the final stage");
        }

        @Test
        @DisplayName("Damage during a fortifying turn extends the Mek's effort by one turn")
        void damageExtendsMekFortify() {
            BipedMek mek = fortifyingMek();
            mek.beginFortify();

            mek.setArmor(4, Mek.LOC_CENTER_TORSO);
            mek.newRound(1);
            assertEquals(1, mek.getFortifyStage(), "A damaged turn holds the Mek at stage 1");

            mek.newRound(2);
            assertEquals(2, mek.getFortifyStage(), "With no further damage, the effort resumes");
        }

        @Test
        @DisplayName("A deserialized Mek can still fortify (regression: null FortifyState NPE)")
        void deserializedMekCanFortify() throws Exception {
            BipedMek restored = serializeRoundTrip(fortifyingMek());
            assertNotNull(restored.getFortifyState(), "Fortify state is recreated after deserialization");
            restored.beginFortify();
            assertEquals(1, restored.getFortifyStage(), "A deserialized Mek begins fortifying without an NPE");
        }
    }

    @Nested
    @DisplayName("Movement legality (terrain and equipment gates)")
    class MovementLegality {

        private ConvInfantry plainInfantry() {
            ConvInfantry infantry = new ConvInfantry();
            infantry.setId(2);
            infantry.setWeight(2.0f);
            infantry.initializeInternal(10, ConvInfantry.LOC_INFANTRY);
            return infantry;
        }

        private ConvInfantry fieldworksInfantry() {
            ConvInfantry infantry = plainInfantry();
            // Trench/Fieldworks Engineers carry a vibro-shovel, which provides F_TRENCH_CAPABLE.
            infantry.setSpecializations(ConvInfantry.TRENCH_ENGINEERS);
            return infantry;
        }

        @Test
        @DisplayName("Non-mechanized infantry may dig in on clear terrain")
        void digInOnClearIsLegal() {
            setBoard("FORTIFY_BOARD");
            MovePath path = getMovePathFor(plainInfantry(), MoveStepType.DIG_IN);
            assertTrue(path.isMoveLegal(), "Digging in on clear terrain should be legal");
        }

        @Test
        @DisplayName("A unit may not dig in while standing in a water hex")
        void digInInWaterIsIllegal() {
            setBoard("FORTIFY_WATER_BOARD");
            MovePath path = getMovePathFor(plainInfantry(), MoveStepType.DIG_IN);
            assertFalse(path.isMoveLegal(), "Digging in is not allowed in water (TO:AR p.106)");
        }

        @Test
        @DisplayName("Fieldworks-equipped infantry may fortify clear terrain")
        void fortifyWithEquipmentIsLegal() {
            setBoard("FORTIFY_BOARD");
            MovePath path = getMovePathFor(fieldworksInfantry(), MoveStepType.FORTIFY);
            assertTrue(path.isMoveLegal(), "A unit with fieldworks equipment may fortify clear terrain");
        }

        @Test
        @DisplayName("Infantry without fieldworks equipment may not fortify")
        void fortifyWithoutEquipmentIsIllegal() {
            setBoard("FORTIFY_BOARD");
            MovePath path = getMovePathFor(plainInfantry(), MoveStepType.FORTIFY);
            assertFalse(path.isMoveLegal(),
                  "Building a fortified hex requires fieldworks-capable equipment (TO:AUE p.153)");
        }

        @Test
        @DisplayName("Fieldworks-equipped infantry may not fortify a water hex")
        void fortifyInWaterIsIllegal() {
            setBoard("FORTIFY_WATER_BOARD");
            MovePath path = getMovePathFor(fieldworksInfantry(), MoveStepType.FORTIFY);
            assertFalse(path.isMoveLegal(), "A fortified hex may not be created in water (TO:AUE p.153)");
        }

        private BipedMek plainMek() {
            BipedMek mek = new BipedMek();
            mek.setId(6);
            mek.setCrew(new Crew(CrewType.SINGLE));
            for (int location = 0; location < mek.locations(); location++) {
                mek.initializeArmor(10, location);
                mek.initializeInternal(5, location);
            }
            return mek;
        }

        private BipedMek backhoeMek() throws LocationFullException {
            BipedMek mek = plainMek();
            mek.addEquipment(EquipmentType.get(EquipmentTypeLookup.BACKHOE), Mek.LOC_RIGHT_ARM);
            return mek;
        }

        @Test
        @DisplayName("A Mek with a backhoe may fortify clear terrain (Vehicles and Fieldworks, TO:AUE p.153, "
              + "Corrected Sixth Printing)")
        void mekWithBackhoeMayFortify() throws LocationFullException {
            setBoard("FORTIFY_BOARD");
            MovePath path = getMovePathFor(backhoeMek(), MoveStepType.FORTIFY);
            assertTrue(path.isMoveLegal(), "A Mek with fieldworks equipment may build a fortified hex");
        }

        @Test
        @DisplayName("A Mek without fieldworks equipment may not fortify")
        void mekWithoutEquipmentMayNotFortify() {
            setBoard("FORTIFY_BOARD");
            MovePath path = getMovePathFor(plainMek(), MoveStepType.FORTIFY);
            assertFalse(path.isMoveLegal(), "Building a fortified hex requires fieldworks-capable equipment");
        }

        @Test
        @DisplayName("A Mek may not dig a foxhole (DIG_IN remains infantry/vehicle only)")
        void mekMayNotDigIn() throws LocationFullException {
            setBoard("FORTIFY_BOARD");
            MovePath path = getMovePathFor(backhoeMek(), MoveStepType.DIG_IN);
            assertFalse(path.isMoveLegal(), "Meks build fortified hexes but do not dig foxholes");
        }
    }

    @Nested
    @DisplayName("Fortifiable terrain rule")
    class FortifiableTerrain {

        private Hex hexWith(int terrainType) {
            Hex hex = new Hex();
            hex.addTerrain(new Terrain(terrainType, 1));
            return hex;
        }

        @Test
        @DisplayName("Clear terrain is fortifiable")
        void clearIsFortifiable() {
            assertTrue(MoveStep.isFortifiableTerrain(new Hex()), "Clear terrain allows digging in / fortifying");
        }

        @Test
        @DisplayName("Water, pavement, building, road and already-fortified hexes are not fortifiable")
        void restrictedTerrainIsNotFortifiable() {
            assertFalse(MoveStep.isFortifiableTerrain(hexWith(Terrains.WATER)), "Water is excluded");
            assertFalse(MoveStep.isFortifiableTerrain(hexWith(Terrains.PAVEMENT)), "Pavement is excluded");
            assertFalse(MoveStep.isFortifiableTerrain(hexWith(Terrains.BUILDING)), "Buildings are excluded");
            assertFalse(MoveStep.isFortifiableTerrain(hexWith(Terrains.ROAD)), "Roads are excluded");
            assertFalse(MoveStep.isFortifiableTerrain(hexWith(Terrains.FORTIFIED)), "Already fortified is excluded");
        }

        @Test
        @DisplayName("A null hex is not fortifiable")
        void nullHexIsNotFortifiable() {
            assertFalse(MoveStep.isFortifiableTerrain(null), "A null hex must be treated as not fortifiable");
        }
    }
}
