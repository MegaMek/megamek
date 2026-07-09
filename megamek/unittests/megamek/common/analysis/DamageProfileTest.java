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
package megamek.common.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link DamageProfile} against real canon units with known loadouts, so the assertions
 * exercise the actual curve math rather than mocked constants.
 *
 * <p>Reference loadouts (all at the default crew gunnery of 4, standard range rules unless
 * stated):</p>
 * <ul>
 *   <li><b>Barghest BGS-1T:</b> 2x ER Large Laser (8 dmg, 7/14/19) + LB 20-X AC (20 dmg, 4/8/12).
 *       Max 36 close in, exactly 16 past 12 hexes, nothing past 19.</li>
 *   <li><b>Atlas AS7-D:</b> AC/20 + SRM-6 + 2x Medium Laser close in, LRM-20 out to 21. The
 *       close-range peak dwarfs the long-range tail.</li>
 *   <li><b>Archer ARC-2R:</b> 2x LRM-20 (min range 6) + 4x Medium Laser. Peak expected damage sits
 *       outside the LRM minimum range.</li>
 *   <li><b>LRM Carrier:</b> 3x LRM-20 and nothing else; a vehicle, so it does not track heat.</li>
 * </ul>
 */
class DamageProfileTest {

    private static final String RESOURCES_PATH = "testresources/megamek/common/units/";
    private static final double TOLERANCE = 0.001;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private Entity loadUnit(String filename) throws EntityLoadingException {
        return new MekFileParser(new File(RESOURCES_PATH + filename)).getEntity();
    }

    @Test
    void testBarghestMaxDamageCurveShape() throws EntityLoadingException {
        Entity barghest = loadUnit("Barghest BGS-1T.mtf");
        DamageProfile profile = DamageProfile.of(barghest, false);

        assertTrue(profile.hasWeapons(), "Barghest has functioning weapons");
        assertEquals(19, profile.maxRange(), "ER Large Laser long range bounds the curve");

        // Inside 12 hexes all three weapons reach: 8 + 8 + 20
        assertEquals(36.0, profile.maxDamage(4), TOLERANCE, "Both lasers plus LB 20-X inside 12");
        // Past the LB 20-X (long 12), only the two lasers remain: 8 + 8
        assertEquals(16.0, profile.maxDamage(13), TOLERANCE, "Only the lasers past 12");
        assertEquals(16.0, profile.maxDamage(19), TOLERANCE, "Lasers still reach at 19");
        // Beyond every weapon, the curve is zero
        assertEquals(0.0, profile.maxDamage(20), TOLERANCE, "Nothing reaches past 19");
    }

    @Test
    void testBarghestExpectedDamageIsHitWeighted() throws EntityLoadingException {
        Entity barghest = loadUnit("Barghest BGS-1T.mtf");
        DamageProfile profile = DamageProfile.of(barghest, false);

        // At range 4 everything is in its short bracket: to-hit 4, odds 91.6%
        double shortBracketOdds = 0.916;
        assertEquals(36.0 * shortBracketOdds, profile.expectedDamage(4), 0.5,
              "Expected damage at 4 is the full 36 weighted by short-bracket odds");

        // Expected damage can never exceed max damage anywhere on the curve
        for (int range = 1; range <= profile.maxRange(); range++) {
            assertTrue(profile.expectedDamage(range) <= profile.maxDamage(range) + TOLERANCE,
                  "Expected <= max at range " + range);
        }
    }

    @Test
    void testBarghestPeakSitsAtEndOfShortBrackets() throws EntityLoadingException {
        Entity barghest = loadUnit("Barghest BGS-1T.mtf");
        DamageProfile profile = DamageProfile.of(barghest, false);

        // Ranges 1-4 have identical expected damage (everything short); ties go long, so peak = 4
        assertEquals(4, profile.peakExpectedRange(), "Peak at the end of the shared short bracket");
        assertEquals(profile.expectedDamage(4), profile.peakExpectedDamage(), TOLERANCE,
              "Peak value matches the curve at the peak range");
    }

    @Test
    void testBarghestSustainedDamageRespectsHeat() throws EntityLoadingException {
        Entity barghest = loadUnit("Barghest BGS-1T.mtf");
        DamageProfile profile = DamageProfile.of(barghest, false);

        // Alpha heat is 12 + 12 + 6 = 30 against far less dissipation; something must stay cold
        assertTrue(profile.sustainedDamage(4) < profile.expectedDamage(4),
              "Barghest cannot sustain its alpha strike");
        assertTrue(profile.sustainedDamage(4) > 0, "Sustained damage is still positive");
    }

    @Test
    void testExtremeRangeExtendsTheCurve() throws EntityLoadingException {
        Entity barghest = loadUnit("Barghest BGS-1T.mtf");
        DamageProfile standard = DamageProfile.of(barghest, false);
        DamageProfile extreme = DamageProfile.of(barghest, true);

        assertTrue(extreme.maxRange() > standard.maxRange(),
              "Extreme range rules extend the curve past long range");
        assertTrue(extreme.expectedDamage(standard.maxRange() + 1) > 0,
              "The extreme bracket contributes expected damage past standard long range");
        assertTrue(extreme.expectedDamage(standard.maxRange() + 1)
                    < extreme.expectedDamage(standard.maxRange() - 1),
              "The +6 extreme bracket modifier keeps the tail below the long bracket");
    }

    @Test
    void testAtlasCloseRangePeakDwarfsLongRangeTail() throws EntityLoadingException {
        Entity atlas = loadUnit("Atlas AS7-D.mtf");
        DamageProfile profile = DamageProfile.of(atlas, false);

        assertEquals(21, profile.maxRange(), "LRM-20 long range bounds the Atlas curve");
        assertTrue(profile.peakExpectedRange() <= 6,
              "The AC/20, SRM-6 and lasers put the Atlas peak close in");
        assertTrue(profile.expectedDamage(21) < profile.peakExpectedDamage() / 2,
              "The LRM tail is a fraction of the close-range punch - the value the damage pool "
                    + "must capture");
    }

    @Test
    void testArcherPeaksOutsideItsMinimumRange() throws EntityLoadingException {
        Entity archer = loadUnit("Archer ARC-2R.mtf");
        DamageProfile profile = DamageProfile.of(archer, false);

        // The peak lands at 6, not 7: at 6 the medium lasers are still in their medium bracket and
        // the LRMs take only a +1 minimum-range penalty, which together beat range 7 where the
        // lasers fall to their long bracket. The point is that minimum range pushes the peak well
        // out of the point-blank band.
        assertEquals(6, profile.peakExpectedRange(),
              "LRM minimum range and the laser brackets put the Archer's peak at 6 hexes");
        assertTrue(profile.expectedDamage(2) < profile.peakExpectedDamage(),
              "Point-blank is penalized by the LRM minimum range");
        assertTrue(profile.expectedDamage(1) < profile.peakExpectedDamage() * 0.75,
              "Deep inside minimum range the LRMs contribute almost nothing");
    }

    @Test
    void testLrmCarrierVehicleDoesNotTrackHeat() throws EntityLoadingException {
        Entity carrier = loadUnit("LRM Carrier.blk");
        DamageProfile profile = DamageProfile.of(carrier, false);

        assertTrue(profile.hasWeapons(), "LRM Carrier has weapons");
        assertEquals(7, profile.peakExpectedRange(),
              "Peak at 7: the first range outside LRM minimum, still in the short bracket");
        for (int range = 1; range <= profile.maxRange(); range++) {
            assertEquals(profile.expectedDamage(range), profile.sustainedDamage(range), TOLERANCE,
                  "No heat tracking means sustained equals expected at range " + range);
        }
    }

    @Test
    void testConventionalInfantryPlatoonDamage() throws EntityLoadingException {
        Entity platoon = loadUnit("Foot Platoon (AFFS) (Laser 3067+).blk");
        DamageProfile profile = DamageProfile.of(platoon, false);

        assertTrue(profile.hasWeapons(), "A rifle platoon is not weaponless");

        // TW p.215: one attack dealing ceil(per-trooper damage x shooting troopers), reaching the
        // infantry weapon's 3R long bracket. Derive the expectation from the unit's own weapon so
        // the test follows the data file.
        Infantry infantry = (Infantry) platoon;
        InfantryWeapon rifle = (InfantryWeapon) infantry.getWeaponList().get(0).getType();
        double attackDamage = Math.ceil(rifle.getInfantryDamage() * infantry.getShootingStrength());
        assertTrue(attackDamage > 1, "A full platoon deals meaningful damage");
        assertEquals(attackDamage, profile.maxDamage(1), TOLERANCE,
              "Platoon max damage is per-trooper damage times shooting troopers, rounded up");
        assertEquals(rifle.getInfantryRange() * 3, profile.maxRange(),
              "Platoon reach is the infantry weapon's 3R long bracket");
        assertTrue(profile.expectedDamage(1) > profile.expectedDamage(profile.maxRange()),
              "Expected damage falls off across the infantry brackets");
    }

    @Test
    void testBattleArmorSquadMultipliesByTroopers() throws EntityLoadingException {
        Entity elemental = loadUnit("Elemental BA [Laser] (Sqd5).blk");
        DamageProfile profile = DamageProfile.of(elemental, false);

        // 5 suits: small lasers (3 dmg each) fire as one attack with a cluster roll on 5 suits;
        // the SRM-2s pool into one 10-missile cluster roll at 2 damage per missile (TW p.218); and
        // the AP mounts carry auto-filled Infantry Assault Rifles firing as infantry small arms:
        // ceil(0.52 per trooper x 5 suits) = 3.
        assertEquals(9, profile.maxRange(), "BA SRM-2 long range bounds the squad's curve");
        assertEquals((3 * 5) + (2 * 5 * 2) + 3, profile.maxDamage(1), TOLERANCE,
              "Max damage counts every suit's laser, the pooled missile rack, and the AP rifles");
        assertTrue(profile.expectedDamage(1) > 3 + (2 * 2),
              "Expected damage reflects the squad, not a single suit");
        for (int range = 1; range <= profile.maxRange(); range++) {
            assertEquals(profile.expectedDamage(range), profile.sustainedDamage(range), TOLERANCE,
                  "Battle armor does not track heat at range " + range);
        }
    }

    @Test
    void testProtoMekResolvesAsIndividualWeapons() throws EntityLoadingException {
        Entity roc = loadUnit("Roc 2.blk");
        DamageProfile profile = DamageProfile.of(roc, false);

        // Roc 2 carries a single Clan Heavy Medium Laser (10 damage, 3/6/9) in the main gun; no
        // trooper multiplication may apply to ProtoMeks.
        assertTrue(profile.hasWeapons(), "The Roc has a main gun");
        assertEquals(9, profile.maxRange(), "Heavy Medium Laser long range bounds the curve");
        assertEquals(10.0, profile.maxDamage(1), TOLERANCE,
              "One laser, one contribution - no squad multiplier");
        assertEquals(profile.expectedDamage(2), profile.sustainedDamage(2), TOLERANCE,
              "ProtoMeks do not track heat");
    }

    @Test
    void testWeaponlessUnitHasEmptyProfile() {
        Entity empty = new BipedMek();
        DamageProfile profile = DamageProfile.of(empty, false);

        assertFalse(profile.hasWeapons(), "No weapons, no profile");
        assertEquals(0, profile.maxRange(), "Empty curve has no reach");
        assertEquals(0, profile.peakExpectedRange(), "No peak range without weapons");
        assertEquals(0.0, profile.peakExpectedDamage(), TOLERANCE, "No peak damage without weapons");
        assertEquals(0.0, profile.maxDamage(5), TOLERANCE, "Any range query returns 0");
    }

    @Test
    void testArcherRearLasersCoverTheRearSectors() throws EntityLoadingException {
        Entity archer = loadUnit("Archer ARC-2R.mtf");
        DamageProfile profile = DamageProfile.of(archer, false);

        // The ARC-2R carries two rear-mounted medium lasers (long range 9). The TW rear arc spans
        // all three rear hexsides, so every rear sector must register them - this is exactly the
        // case a spine-only arc probe misses, because the rear arc's boundaries run along spines.
        assertEquals(9, profile.arcSummary(3).reach(), "Rear sector sees the rear lasers");
        assertTrue(profile.arcSummary(2).reach() >= 9, "Rear-right sector sees the rear lasers");
        assertTrue(profile.arcSummary(4).reach() >= 9, "Rear-left sector sees the rear lasers");
        assertTrue(profile.arcSummary(3).maximumAverage() > 0, "Rear arc carries real damage");
    }

    @Test
    void testAtlasArcSummariesReflectRearLasers() throws EntityLoadingException {
        Entity atlas = loadUnit("Atlas AS7-D.mtf");
        DamageProfile profile = DamageProfile.of(atlas, false);

        // Front: everything but the two rear-mounted CT medium lasers; LRM-20 bounds the reach
        DamageProfile.ArcSummary front = profile.arcSummary(0);
        assertEquals(21, front.reach(), "Front reach is the LRM-20 long range");
        assertTrue(front.maximumAverage() > 0, "Front arc carries the main battery");

        // Rear: only the two rear CT medium lasers (long range 9)
        DamageProfile.ArcSummary rear = profile.arcSummary(3);
        assertEquals(9, rear.reach(), "Rear reach is the rear medium lasers' long range");
        assertTrue(rear.maximumAverage() > 0, "Rear lasers register");
        assertTrue(rear.maximumAverage() < front.maximumAverage() / 3,
              "Rear firepower is a small fraction of the front battery");
        assertEquals(0.0, rear.longRangeAverage(), 0.001,
              "Nothing in the rear arc reaches past 12 hexes");
    }

    @Test
    void testBarghestQuadTorsoWeaponsAreFrontOnly() throws EntityLoadingException {
        Entity barghest = loadUnit("Barghest BGS-1T.mtf");
        DamageProfile profile = DamageProfile.of(barghest, false);

        assertEquals(19, profile.arcSummary(0).reach(),
              "All torso weapons bear forward; lasers bound the front reach");
        assertEquals(0, profile.arcSummary(3).reach(), "No weapon bears to the rear");
        assertEquals(0.0, profile.arcSummary(3).maximumAverage(), 0.001,
              "Rear arc is completely empty");
    }

    @Test
    void testGunneryOverrideShiftsTheExpectedCurve() throws EntityLoadingException {
        Entity barghest = loadUnit("Barghest BGS-1T.mtf");
        DamageProfile elite = DamageProfile.of(barghest, false, 2);
        DamageProfile green = DamageProfile.of(barghest, false, 5);

        assertEquals(2, elite.gunnery(), "Override is baked into the profile");
        assertTrue(elite.expectedDamage(4) > green.expectedDamage(4),
              "Better gunnery raises expected damage at every range");
        assertEquals(elite.maxDamage(4), green.maxDamage(4), TOLERANCE,
              "Maximum damage is gunnery-independent");
    }

    @Test
    void testExpectedClusterHitsMatchesTableExpectation() {
        // LRM-20 column of the cluster hits table, probability-weighted, lands between 12 and 13 -
        // well above the 12 that the common "roll a 7" shortcut gives.
        double expectedTwenty = DamageProfile.expectedClusterHits(20);
        assertTrue(expectedTwenty > 12.0 && expectedTwenty < 13.0,
              "Expected cluster hits for a 20-rack should be between 12 and 13, was " + expectedTwenty);

        // A 2-rack averages between 1 and 2, and expectation grows with rack size
        double expectedTwo = DamageProfile.expectedClusterHits(2);
        assertTrue(expectedTwo > 1.0 && expectedTwo < 2.0,
              "Expected cluster hits for a 2-rack should be between 1 and 2, was " + expectedTwo);
        assertTrue(expectedTwenty > DamageProfile.expectedClusterHits(10),
              "Expectation grows with rack size");
    }
}
