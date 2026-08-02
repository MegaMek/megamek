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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RoleSlotSurvey}, which defines the set of unit slots a {@link megamek.common.units.UnitRole}
 * percentage mix is free to govern.
 *
 * <p>The governed set is the shared contract between this survey and the role budget allocator that will consume it,
 * so each exclusion reason is asserted independently.</p>
 */
class RoleSlotSurveyTest {

    /**
     * Builds a leaf node - one that produces a single unit - of the given type.
     *
     * @param unitType the unit type, see {@link UnitType}
     *
     * @return a childless force descriptor
     */
    private static ForceDescriptor leaf(int unitType) {
        ForceDescriptor leafDescriptor = new ForceDescriptor();
        leafDescriptor.setUnitType(unitType);
        return leafDescriptor;
    }

    /**
     * Builds a parent node holding the given children as subforces.
     *
     * @param children the subforces to attach
     *
     * @return a force descriptor with the given children
     */
    private static ForceDescriptor parentOf(ForceDescriptor... children) {
        ForceDescriptor parentDescriptor = new ForceDescriptor();
        parentDescriptor.setUnitType(UnitType.MEK);
        ArrayList<ForceDescriptor> subForces = new ArrayList<>();
        for (ForceDescriptor child : children) {
            subForces.add(child);
        }
        parentDescriptor.setSubForces(subForces);
        return parentDescriptor;
    }

    @Test
    void plainMekLanceIsFullyGoverned() {
        ForceDescriptor lance = parentOf(leaf(UnitType.MEK), leaf(UnitType.MEK),
              leaf(UnitType.MEK), leaf(UnitType.MEK));

        RoleCoverageReport report = RoleSlotSurvey.survey(lance);

        assertEquals(4, report.totalUnitSlots());
        assertEquals(4, report.governedSlots());
        assertEquals(0, report.slotsSetByFormation());
        assertTrue(report.hasGovernedSlots());
        assertEquals(100, report.governedPercent());
    }

    @Test
    void formationOnTheLanceExcludesItsSlots() {
        ForceDescriptor lance = parentOf(leaf(UnitType.MEK), leaf(UnitType.MEK),
              leaf(UnitType.MEK), leaf(UnitType.MEK));
        FormationType battleLance = FormationType.getFormationType("Battle");
        assertNotNull(battleLance, "the Battle formation should be registered");
        lance.setFormationType(battleLance);

        RoleCoverageReport report = RoleSlotSurvey.survey(lance);

        assertEquals(4, report.totalUnitSlots());
        assertEquals(0, report.governedSlots());
        assertEquals(4, report.slotsSetByFormation());
        assertFalse(report.hasGovernedSlots());
    }

    @Test
    void formationOnAnAncestorExcludesDescendantSlots() {
        // A formation binds to the node that declares it, but every unit slot beneath that node belongs to
        // the formation's own selection, however deep the tree goes.
        ForceDescriptor innerLance = parentOf(leaf(UnitType.MEK), leaf(UnitType.MEK));
        ForceDescriptor company = parentOf(innerLance);
        FormationType battleLance = FormationType.getFormationType("Battle");
        assertNotNull(battleLance, "the Battle formation should be registered");
        company.setFormationType(battleLance);

        RoleCoverageReport report = RoleSlotSurvey.survey(company);

        assertEquals(2, report.totalUnitSlots());
        assertEquals(0, report.governedSlots());
        assertEquals(2, report.slotsSetByFormation());
    }

    @Test
    void attachedSupportForcesAreExcluded() {
        ForceDescriptor battalion = parentOf(leaf(UnitType.MEK), leaf(UnitType.MEK));
        ArrayList<ForceDescriptor> attached = new ArrayList<>();
        attached.add(leaf(UnitType.TANK));
        attached.add(leaf(UnitType.TANK));
        attached.add(leaf(UnitType.TANK));
        battalion.setAttached(attached);

        RoleCoverageReport report = RoleSlotSurvey.survey(battalion);

        assertEquals(5, report.totalUnitSlots());
        assertEquals(2, report.governedSlots());
        assertEquals(3, report.slotsInAttachedForces());
    }

    @Test
    void unitTypesWithoutAWeightClassAreExcluded() {
        // Role targeting needs weight bands to route quotas between, and useWeightClass() covers only
        // Mek, ASF, Tank and Battle Armor. Infantry and ProtoMeks have no weight class.
        ForceDescriptor mixed = parentOf(leaf(UnitType.MEK), leaf(UnitType.TANK),
              leaf(UnitType.INFANTRY), leaf(UnitType.PROTOMEK));

        RoleCoverageReport report = RoleSlotSurvey.survey(mixed);

        assertEquals(4, report.totalUnitSlots());
        assertEquals(2, report.governedSlots());
        assertEquals(2, report.slotsExcludedByUnitType());
        assertEquals(0, report.slotsExcludedByArtillery());
    }

    @Test
    void artilleryRoleExcludesTheSlotAndIsCountedSeparately() {
        // An artillery mission role switches useWeightClass() off entirely, so weight-class handling and
        // therefore role targeting cannot apply. It must be reported distinctly from a unit-type exclusion.
        ForceDescriptor artilleryMek = leaf(UnitType.MEK);
        artilleryMek.getRoles().add(MissionRole.ARTILLERY);
        ForceDescriptor missileArtilleryMek = leaf(UnitType.MEK);
        missileArtilleryMek.getRoles().add(MissionRole.MISSILE_ARTILLERY);
        ForceDescriptor lance = parentOf(leaf(UnitType.MEK), artilleryMek, missileArtilleryMek);

        RoleCoverageReport report = RoleSlotSurvey.survey(lance);

        assertEquals(3, report.totalUnitSlots());
        assertEquals(1, report.governedSlots());
        assertEquals(2, report.slotsExcludedByArtillery());
        assertEquals(0, report.slotsExcludedByUnitType());
    }

    @Test
    void exclusionCountsSumToTheTotal() {
        ForceDescriptor artilleryMek = leaf(UnitType.MEK);
        artilleryMek.getRoles().add(MissionRole.ARTILLERY);
        ForceDescriptor formationLance = parentOf(leaf(UnitType.MEK), leaf(UnitType.MEK));
        formationLance.setFormationType(FormationType.getFormationType("Battle"));
        ForceDescriptor battalion = parentOf(leaf(UnitType.MEK), leaf(UnitType.INFANTRY), artilleryMek);
        battalion.getSubForces().add(formationLance);
        ArrayList<ForceDescriptor> attached = new ArrayList<>();
        attached.add(leaf(UnitType.TANK));
        battalion.setAttached(attached);

        RoleCoverageReport report = RoleSlotSurvey.survey(battalion);

        int accountedFor = report.governedSlots() + report.slotsSetByFormation()
              + report.slotsInAttachedForces() + report.slotsExcludedByUnitType()
              + report.slotsExcludedByArtillery();
        assertEquals(report.totalUnitSlots(), accountedFor,
              "every slot must be counted exactly once");
        assertEquals(6, report.totalUnitSlots());
        assertEquals(1, report.governedSlots());
    }

    @Test
    void nullRootYieldsTheEmptyReport() {
        RoleCoverageReport report = RoleSlotSurvey.survey(null);

        assertEquals(0, report.totalUnitSlots());
        assertFalse(report.hasGovernedSlots());
        assertEquals(0, report.governedPercent());
    }
}
