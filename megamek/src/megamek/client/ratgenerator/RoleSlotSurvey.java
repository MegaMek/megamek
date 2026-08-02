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

import java.util.ArrayList;
import java.util.List;

import megamek.logging.MMLogger;

/**
 * Counts how many of a built force tree's unit slots a {@link megamek.common.units.UnitRole} percentage mix would be
 * free to govern.
 *
 * <p>Run after the force tree is built and before any unit is picked, so every node's weight class and formation
 * assignment are final but nothing has been rolled yet. That is the same window the role budget allocator will use, so
 * this survey defines the governed set both features share.</p>
 *
 * <p>A slot is governed unless one of the following applies, tested in this order:</p>
 * <ol>
 *     <li>it sits under a node carrying a Campaign Operations formation, which owns its own unit choices;</li>
 *     <li>it sits inside an {@code <attachedForces>} support detachment, which keeps its canon character;</li>
 *     <li>its unit type has no weight class, so there are no weight bands to route role quotas between;</li>
 *     <li>it carries an artillery mission role, which switches weight-class handling off entirely.</li>
 * </ol>
 */
final class RoleSlotSurvey {

    private static final MMLogger LOGGER = MMLogger.create(RoleSlotSurvey.class);

    private RoleSlotSurvey() {}

    /**
     * The governed leaves of a force tree together with the coverage they represent.
     *
     * @param governedLeaves the unit slots a role mix is free to assign a role to, in tree order
     * @param report         the coverage those leaves represent, including why the rest were excluded
     */
    record SurveyResult(List<ForceDescriptor> governedLeaves, RoleCoverageReport report) {}

    /**
     * Surveys the force tree rooted at the given descriptor.
     *
     * @param root the root of a built force tree
     *
     * @return the slot coverage a role mix would have over this force, never {@code null}
     */
    static RoleCoverageReport survey(ForceDescriptor root) {
        return collect(root).report();
    }

    /**
     * Surveys the force tree and returns the governed leaves alongside the coverage.
     *
     * <p>The role budget allocator works from the same list this produces, so the definition of a governed slot lives
     * here only and the two features cannot drift apart.</p>
     *
     * @param root the root of a built force tree
     *
     * @return the governed leaves and their coverage, never {@code null}
     */
    static SurveyResult collect(ForceDescriptor root) {
        if (root == null) {
            return new SurveyResult(List.of(), RoleCoverageReport.EMPTY);
        }
        SlotTally tally = new SlotTally();
        walk(root, false, false, tally);
        RoleCoverageReport report = tally.toReport();
        LOGGER.debug("[ForceGen][RoleTarget] slot survey: {} unit slots, {} governed ({}%);"
                    + " excluded {} by formation, {} attached, {} by unit type, {} by artillery",
              report.totalUnitSlots(), report.governedSlots(), report.governedPercent(),
              report.slotsSetByFormation(), report.slotsInAttachedForces(),
              report.slotsExcludedByUnitType(), report.slotsExcludedByArtillery());
        return new SurveyResult(tally.governedLeaves, report);
    }

    /**
     * Recursively classifies every unit-producing leaf beneath the given node.
     *
     * <p>A node is a unit slot when it has no subforces. Leaves are not flagged as elements until
     * {@code ForceDescriptor.setUnit} runs during generation, which is after this survey, so the structural test is
     * the only one available here.</p>
     *
     * @param node            the node being classified
     * @param underFormation  whether any ancestor carries a formation
     * @param withinAttached  whether any ancestor is an attached support detachment
     * @param tally           running counts, mutated in place
     */
    private static void walk(ForceDescriptor node, boolean underFormation, boolean withinAttached, SlotTally tally) {
        if (node.getSubForces().isEmpty()) {
            tally.classify(node, underFormation, withinAttached);
            return;
        }
        // A formation binds only to the node that declares it - createChild deliberately does not copy
        // formationType - so the flag has to be carried down by this walk rather than read off each child.
        boolean childrenUnderFormation = underFormation || (node.getFormation() != null);
        for (ForceDescriptor subForce : node.getSubForces()) {
            walk(subForce, childrenUnderFormation, withinAttached, tally);
        }
        for (ForceDescriptor attachedForce : node.getAttached()) {
            walk(attachedForce, childrenUnderFormation, true, tally);
        }
    }

    /** Mutable counters assembled into an immutable {@link RoleCoverageReport}. */
    private static final class SlotTally {
        private final List<ForceDescriptor> governedLeaves = new ArrayList<>();
        private int total;
        private int byFormation;
        private int attached;
        private int byUnitType;
        private int byArtillery;

        private void classify(ForceDescriptor leaf, boolean underFormation, boolean withinAttached) {
            total++;
            if (underFormation) {
                byFormation++;
            } else if (withinAttached) {
                attached++;
            } else if (!leaf.useWeightClass()) {
                // useWeightClass() covers both the unit-type restriction and the artillery override, so the
                // artillery case is separated out first to keep the two reasons distinct in the report.
                if (carriesArtilleryRole(leaf)) {
                    byArtillery++;
                } else {
                    byUnitType++;
                }
            } else {
                governedLeaves.add(leaf);
            }
        }

        private static boolean carriesArtilleryRole(ForceDescriptor leaf) {
            return leaf.getRoles().contains(MissionRole.ARTILLERY)
                  || leaf.getRoles().contains(MissionRole.MISSILE_ARTILLERY);
        }

        private RoleCoverageReport toReport() {
            return new RoleCoverageReport(total, governedLeaves.size(), byFormation, attached, byUnitType,
                  byArtillery);
        }
    }
}
