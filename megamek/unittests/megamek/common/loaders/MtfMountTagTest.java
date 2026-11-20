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

package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import megamek.common.CriticalSlot;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The mount tags of MTF critical rows: {@code Autocannon/20#1} tells which mount of its equipment a row belongs to,
 * for equipment that is split over two locations.
 */
class MtfMountTagTest {
    private static final String AC20 = "Autocannon/20";
    private static final String MEDIUM_LASER = "Medium Laser";
    private static final String UNITS = "testresources/megamek/common/units/";

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    private static Mek newMek() {
        Mek mek = new BipedMek();
        mek.setWeight(100.0);
        mek.setEngine(new Engine(300, Engine.NORMAL_ENGINE, 0));
        return mek;
    }

    private static Mek load(String mtf) throws Exception {
        return (Mek) new MekFileParser(mtf).getEntity();
    }

    /** Adds the given number of critical rows of a mount to a location. */
    private static void addRows(Mek mek, Mounted<?> mount, int location, int rows) throws Exception {
        for (int row = 0; row < rows; row++) {
            if (mek.getEquipment().contains(mount)) {
                mek.addCritical(location, new CriticalSlot(mount));
            } else {
                mek.addEquipment(mount, location, false);
            }
        }
    }

    private static Mounted<?> mount(Mek mek, String name) {
        return Mounted.createMounted(mek, EquipmentType.get(name));
    }

    private static Mounted<?> add(Mek mek, String name, int location) throws Exception {
        return mek.addEquipment(EquipmentType.get(name), location);
    }

    private static List<Integer> slots(Mek mek, int location, Mounted<?> mount) {
        return IntStream.range(0, mek.getNumberOfCriticalSlots(location))
              .filter(slot -> (mek.getCritical(location, slot) != null)
                    && (mek.getCritical(location, slot).getMount() == mount))
              .boxed().toList();
    }

    /** @return The critical rows of a location as they are written, without the empty ones */
    private static List<String> rows(String mtf, String location) {
        List<String> lines = mtf.lines().toList();
        List<String> rows = new ArrayList<>();
        for (int line = lines.indexOf(location + ":") + 1; !lines.get(line).isBlank(); line++) {
            if (!lines.get(line).equals(MtfFile.EMPTY)) {
                rows.add(lines.get(line));
            }
        }
        return rows;
    }

    private static List<String> repeat(String row, int count) {
        return IntStream.range(0, count).mapToObj(i -> row).toList();
    }

    private static <T> List<T> join(List<T> first, List<T> second) {
        List<T> joined = new ArrayList<>(first);
        joined.addAll(second);
        return joined;
    }

    @Test
    void twoWeaponsSplitOverTheSameLocationsKeepTheirRows() throws Exception {
        Mek mek = newMek();
        mek.setCritical(Mek.LOC_LEFT_ARM, 2, null);
        mek.setCritical(Mek.LOC_LEFT_ARM, 3, null);
        Mounted<?> first = mount(mek, AC20);
        Mounted<?> second = mount(mek, AC20);
        addRows(mek, first, Mek.LOC_LEFT_ARM, 5);
        addRows(mek, second, Mek.LOC_LEFT_ARM, 5);
        addRows(mek, first, Mek.LOC_LEFT_TORSO, 5);
        addRows(mek, second, Mek.LOC_LEFT_TORSO, 5);

        String mtf = mek.getMtf();
        List<String> tagged = join(repeat(AC20 + "#1", 5), repeat(AC20 + "#2", 5));
        assertEquals(join(List.of("Shoulder", "Upper Arm Actuator"), tagged), rows(mtf, "Left Arm"));
        assertEquals(tagged, rows(mtf, "Left Torso"));

        Mek loaded = load(mtf);
        Mounted<?> loadedFirst = loaded.getCritical(Mek.LOC_LEFT_ARM, 2).getMount();
        Mounted<?> loadedSecond = loaded.getCritical(Mek.LOC_LEFT_ARM, 7).getMount();
        assertEquals(2, loaded.getWeaponList().size());
        assertEquals(List.of(2, 3, 4, 5, 6), slots(loaded, Mek.LOC_LEFT_ARM, loadedFirst));
        assertEquals(List.of(0, 1, 2, 3, 4), slots(loaded, Mek.LOC_LEFT_TORSO, loadedFirst));
        assertEquals(List.of(7, 8, 9, 10, 11), slots(loaded, Mek.LOC_LEFT_ARM, loadedSecond));
        assertEquals(List.of(5, 6, 7, 8, 9), slots(loaded, Mek.LOC_LEFT_TORSO, loadedSecond));
        for (Mounted<?> weapon : loaded.getWeaponList()) {
            assertTrue(weapon.isSplit());
            assertEquals(Mek.LOC_LEFT_TORSO, weapon.getLocation());
            assertEquals(Mek.LOC_LEFT_ARM, weapon.getSecondLocation());
        }
        assertEquals(rows(mtf, "Left Arm"), rows(loaded.getMtf(), "Left Arm"));
        assertEquals(rows(mtf, "Left Torso"), rows(loaded.getMtf(), "Left Torso"));

        // Without the tags the rows of the arm are one weapon, as they have always been read
        Mek untagged = load(mtf.replace("#1", "").replace("#2", ""));
        assertEquals(10, slots(untagged, Mek.LOC_LEFT_ARM, untagged.getCritical(Mek.LOC_LEFT_ARM, 2).getMount())
              .size());
    }

    /** The split weapon shares the torso with the whole one, so its rows are tagged whether they are on top or not */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void splitWeaponKeepsItsRowsNextToAWholeOneInTheSameTorso(boolean splitRowsOnTop) throws Exception {
        Mek mek = newMek();
        Mounted<?> split = mount(mek, AC20);
        Mounted<?> whole = mount(mek, AC20);
        addRows(mek, split, Mek.LOC_RIGHT_ARM, 8);
        if (splitRowsOnTop) {
            addRows(mek, split, Mek.LOC_RIGHT_TORSO, 2);
            addRows(mek, whole, Mek.LOC_RIGHT_TORSO, 10);
        } else {
            addRows(mek, whole, Mek.LOC_RIGHT_TORSO, 10);
            addRows(mek, split, Mek.LOC_RIGHT_TORSO, 2);
        }

        String mtf = mek.getMtf();
        assertEquals(splitRowsOnTop ? join(repeat(AC20 + "#1", 2), repeat(AC20, 10))
              : join(repeat(AC20, 10), repeat(AC20 + "#1", 2)), rows(mtf, "Right Torso"));

        Mek loaded = load(mtf);
        Mounted<?> loadedSplit = loaded.getCritical(Mek.LOC_RIGHT_ARM, 4).getMount();
        Mounted<?> loadedWhole = loaded.getCritical(Mek.LOC_RIGHT_TORSO, splitRowsOnTop ? 2 : 0).getMount();
        int wholeFirstSlot = splitRowsOnTop ? 2 : 0;
        assertEquals(2, loaded.getWeaponList().size());
        assertEquals(List.of(4, 5, 6, 7, 8, 9, 10, 11), slots(loaded, Mek.LOC_RIGHT_ARM, loadedSplit));
        assertEquals(splitRowsOnTop ? List.of(0, 1) : List.of(10, 11), slots(loaded, Mek.LOC_RIGHT_TORSO, loadedSplit));
        assertEquals(IntStream.range(wholeFirstSlot, wholeFirstSlot + 10).boxed().toList(),
              slots(loaded, Mek.LOC_RIGHT_TORSO, loadedWhole));
        assertTrue(loadedSplit.isSplit());
        assertFalse(loadedWhole.isSplit());
        assertEquals(rows(mtf, "Right Arm"), rows(loaded.getMtf(), "Right Arm"));
        assertEquals(rows(mtf, "Right Torso"), rows(loaded.getMtf(), "Right Torso"));
    }

    @Test
    void enhancementFollowsTheRowsOfAWeaponSplitInFromAnotherLocation() throws Exception {
        // The left torso lists an LRM 5, the rest of an Extended LRM 20 that starts in the left arm, then Artemis IV.
        // The left arm is read first, but the Artemis follows the Extended LRM 20's rows.
        Mek mek = newMek();
        Mounted<?> extendedLrm = mount(mek, "Extended LRM 20");
        addRows(mek, extendedLrm, Mek.LOC_LEFT_ARM, 3);
        add(mek, "LRM 5", Mek.LOC_LEFT_TORSO);
        addRows(mek, extendedLrm, Mek.LOC_LEFT_TORSO, extendedLrm.getNumCriticalSlots() - 3);
        add(mek, "ISArtemisIV", Mek.LOC_LEFT_TORSO);

        Mek loaded = load(mek.getMtf());
        Mounted<?> artemis = loaded.getMisc().stream()
              .filter(misc -> misc.getType().hasFlag(MiscType.F_ARTEMIS)).findFirst().orElseThrow();
        assertEquals("Extended LRM 20", artemis.getLinked().getName());
    }

    /** A split launcher is in both its locations, so Artemis IV next to its arm rows is its own (TM p. 207) */
    @Test
    void enhancementNextToTheOtherRowsOfASplitWeaponIsItsOwn() throws Exception {
        Mek mek = newMek();
        Mounted<?> extendedLrm = mount(mek, "Extended LRM 20");
        addRows(mek, extendedLrm, Mek.LOC_LEFT_ARM, 3);
        add(mek, "ISArtemisIV", Mek.LOC_LEFT_ARM);
        addRows(mek, extendedLrm, Mek.LOC_LEFT_TORSO, extendedLrm.getNumCriticalSlots() - 3);

        Mek loaded = load(mek.getMtf());
        Mounted<?> artemis = loaded.getMisc().stream()
              .filter(misc -> misc.getType().hasFlag(MiscType.F_ARTEMIS)).findFirst().orElseThrow();
        assertEquals("Extended LRM 20", artemis.getLinked().getName());
    }

    @Test
    void mountTagOfOtherEquipmentIsIgnored() throws Exception {
        Mek mek = newMek();
        add(mek, MEDIUM_LASER, Mek.LOC_RIGHT_TORSO);
        add(mek, MEDIUM_LASER, Mek.LOC_RIGHT_TORSO);
        String rows = MEDIUM_LASER + "\n" + MEDIUM_LASER + "\n";
        String mtf = mek.getMtf();
        assertTrue(mtf.contains(rows));

        Mek loaded = load(mtf.replace(rows, MEDIUM_LASER + "#2\n" + MEDIUM_LASER + "#2\n"));

        assertEquals(load(mtf).getMtf(), loaded.getMtf());
        assertFalse(loaded.getFailedEquipment().hasNext());
    }

    @Test
    void untaggedSplitWeaponLoadsAndSavesWithoutTags() throws Exception {
        // The Barghest BGS-1T's LB 20-X AC has 9 rows in the right torso and 2 in the center torso
        Mek loaded = (Mek) new MekFileParser(new File(UNITS + "Barghest BGS-1T.mtf")).getEntity();
        Mounted<?> lbx = loaded.getWeaponList().stream().filter(Mounted::isSplit).findFirst().orElseThrow();

        assertEquals("ISLBXAC20", lbx.getType().getInternalName());
        assertEquals(Mek.LOC_CENTER_TORSO, lbx.getLocation());
        assertEquals(Mek.LOC_RIGHT_TORSO, lbx.getSecondLocation());
        assertEquals(lbx.getNumCriticalSlots(), slots(loaded, Mek.LOC_CENTER_TORSO, lbx).size()
              + slots(loaded, Mek.LOC_RIGHT_TORSO, lbx).size());
        assertTrue(MtfFile.mountTags(loaded).isEmpty());
        assertEquals(loaded.getMtf(), load(loaded.getMtf()).getMtf());
    }
}
