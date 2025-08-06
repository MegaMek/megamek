/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 8/31/13 1:10 PM
 */
class BehaviorSettingsTestConstants {
    static final String GOOD_BEHAVIOR_NAME = "DEFAULT";
    static final CardinalEdge GOOD_HOME_EDGE = CardinalEdge.NORTH;
    static final CardinalEdge GOOD_DESTINATION_EDGE = CardinalEdge.NORTH;
    static final boolean GOOD_FORCED_WITHDRAWAL = true;
    static final boolean GOOD_AUTO_FLEE = false;
    static final int GOOD_FALL_SHAME_INDEX = 5;
    static final int GOOD_HYPER_AGGRESSION_INDEX = 5;
    static final int GOOD_SELF_PRESERVATION_INDEX = 5;
    static final int GOOD_HERD_MENTALITY_INDEX = 5;
    static final int GOOD_BRAVERY_INDEX = 5;
    static final String STRATEGIC_TARGET_1 = "1234";
    static final String STRATEGIC_TARGET_2 = "9876";
    static final int PRIORITY_TARGET = 100;
    static final int BAD_INDEX_BIG = Integer.MAX_VALUE;
    static final int BAD_INDEX_SMALL = Integer.MIN_VALUE;
    static final String GOOD_BEHAVIOR_XML = "<behavior>\n" +
          "    <name>" + GOOD_BEHAVIOR_NAME + "</name>\n" +
          "    <retreatEdge>" + GOOD_HOME_EDGE + "</retreatEdge>\n" +
          "    <destinationEdge>" + GOOD_DESTINATION_EDGE + "</destinationEdge>\n" +
          "    <forcedWithdrawal>" + GOOD_FORCED_WITHDRAWAL + "</forcedWithdrawal>\n" +
          "    <autoFlee>" + GOOD_AUTO_FLEE + "</autoFlee>\n" +
          "    <fallShameIndex>" + GOOD_FALL_SHAME_INDEX + "</fallShameIndex>\n" +
          "    <hyperAggressionIndex>" + GOOD_HYPER_AGGRESSION_INDEX + "</hyperAggressionIndex>\n" +
          "    <selfPreservationIndex>" + GOOD_SELF_PRESERVATION_INDEX + "</selfPreservationIndex>\n" +
          "    <herdMentalityIndex>" + GOOD_HERD_MENTALITY_INDEX + "</herdMentalityIndex>\n" +
          "    <braveryIndex>" + GOOD_BRAVERY_INDEX + "</braveryIndex>\n" +
          "    <strategicTargets>\n" +
          "        <target>" + STRATEGIC_TARGET_1 + "</target>\n" +
          "        <target>" + STRATEGIC_TARGET_2 + "</target>\n" +
          "        <unit>" + PRIORITY_TARGET + "</unit>\n" +
          "    </strategicTargets>\n" +
          "</behavior>";
    static final String BEHAVIOR_XML_NULL_NAME = "<behavior>\n" +
          "    <name>" + null + "</name>\n" +
          "    <retreatEdge>" + GOOD_HOME_EDGE + "</retreatEdge>\n" +
          "    <destinationEdge>" + GOOD_DESTINATION_EDGE + "</destinationEdge>\n" +
          "    <forcedWithdrawal>" + GOOD_FORCED_WITHDRAWAL + "</forcedWithdrawal>\n" +
          "    <autoFlee>" + GOOD_AUTO_FLEE + "</autoFlee>\n" +
          "    <fallShameIndex>" + GOOD_FALL_SHAME_INDEX + "</fallShameIndex>\n" +
          "    <hyperAggressionIndex>" + GOOD_HYPER_AGGRESSION_INDEX + "</hyperAggressionIndex>\n" +
          "    <selfPreservationIndex>" + GOOD_SELF_PRESERVATION_INDEX + "</selfPreservationIndex>\n" +
          "    <herdMentalityIndex>" + GOOD_HERD_MENTALITY_INDEX + "</herdMentalityIndex>\n" +
          "    <braveryIndex>" + GOOD_BRAVERY_INDEX + "</braveryIndex>\n" +
          "    <strategicTargets>\n" +
          "        <target>" + STRATEGIC_TARGET_1 + "</target>\n" +
          "        <target>" + STRATEGIC_TARGET_2 + "</target>\n" +
          "        <unit>" + PRIORITY_TARGET + "</unit>\n" +
          "    </strategicTargets>\n" +
          "</behavior>";
    static final String BEHAVIOR_XML_EMPTY_NAME = "<behavior>\n" +
          "    <name></name>\n" +
          "    <retreatEdge>" + GOOD_HOME_EDGE + "</retreatEdge>\n" +
          "    <destinationEdge>" + GOOD_DESTINATION_EDGE + "</destinationEdge>\n" +
          "    <forcedWithdrawal>" + GOOD_FORCED_WITHDRAWAL + "</forcedWithdrawal>\n" +
          "    <autoFlee>" + GOOD_AUTO_FLEE + "</autoFlee>\n" +
          "    <fallShameIndex>" + GOOD_FALL_SHAME_INDEX + "</fallShameIndex>\n" +
          "    <hyperAggressionIndex>" + GOOD_HYPER_AGGRESSION_INDEX + "</hyperAggressionIndex>\n" +
          "    <selfPreservationIndex>" + GOOD_SELF_PRESERVATION_INDEX + "</selfPreservationIndex>\n" +
          "    <herdMentalityIndex>" + GOOD_HERD_MENTALITY_INDEX + "</herdMentalityIndex>\n" +
          "    <braveryIndex>" + GOOD_BRAVERY_INDEX + "</braveryIndex>\n" +
          "    <strategicTargets>\n" +
          "        <target>" + STRATEGIC_TARGET_1 + "</target>\n" +
          "        <target>" + STRATEGIC_TARGET_2 + "</target>\n" +
          "        <unit>" + PRIORITY_TARGET + "</unit>\n" +
          "    </strategicTargets>\n" +
          "</behavior>";
    static final String BEHAVIOR_XML_NULL_HOME_EDGE = "<behavior>\n" +
          "    <name>" + GOOD_BEHAVIOR_NAME + "</name>\n" +
          "    <retreatEdge>" + null + "</retreatEdge>\n" +
          "    <destinationEdge>" + GOOD_DESTINATION_EDGE + "</destinationEdge>\n" +
          "    <forcedWithdrawal>" + GOOD_FORCED_WITHDRAWAL + "</forcedWithdrawal>\n" +
          "    <autoFlee>" + GOOD_AUTO_FLEE + "</autoFlee>\n" +
          "    <fallShameIndex>" + GOOD_FALL_SHAME_INDEX + "</fallShameIndex>\n" +
          "    <hyperAggressionIndex>" + GOOD_HYPER_AGGRESSION_INDEX + "</hyperAggressionIndex>\n" +
          "    <selfPreservationIndex>" + GOOD_SELF_PRESERVATION_INDEX + "</selfPreservationIndex>\n" +
          "    <herdMentalityIndex>" + GOOD_HERD_MENTALITY_INDEX + "</herdMentalityIndex>\n" +
          "    <braveryIndex>" + GOOD_BRAVERY_INDEX + "</braveryIndex>\n" +
          "    <strategicTargets>\n" +
          "        <target>" + STRATEGIC_TARGET_1 + "</target>\n" +
          "        <target>" + STRATEGIC_TARGET_2 + "</target>\n" +
          "        <unit>" + PRIORITY_TARGET + "</unit>\n" +
          "    </strategicTargets>\n" +
          "</behavior>";
    static final String BEHAVIOR_XML_NULL_FORCED_WITHDRAWAL = "<behavior>\n" +
          "    <name>" + GOOD_BEHAVIOR_NAME + "</name>\n" +
          "    <retreatEdge>" + GOOD_HOME_EDGE + "</retreatEdge>\n" +
          "    <destinationEdge>" + GOOD_DESTINATION_EDGE + "</destinationEdge>\n" +
          "    <forcedWithdrawal>" + null + "</forcedWithdrawal>\n" +
          "    <autoFlee>" + GOOD_AUTO_FLEE + "</autoFlee>\n" +
          "    <fallShameIndex>" + GOOD_FALL_SHAME_INDEX + "</fallShameIndex>\n" +
          "    <hyperAggressionIndex>" + GOOD_HYPER_AGGRESSION_INDEX + "</hyperAggressionIndex>\n" +
          "    <selfPreservationIndex>" + GOOD_SELF_PRESERVATION_INDEX + "</selfPreservationIndex>\n" +
          "    <herdMentalityIndex>" + GOOD_HERD_MENTALITY_INDEX + "</herdMentalityIndex>\n" +
          "    <braveryIndex>" + GOOD_BRAVERY_INDEX + "</braveryIndex>\n" +
          "    <strategicTargets>\n" +
          "        <target>" + STRATEGIC_TARGET_1 + "</target>\n" +
          "        <target>" + STRATEGIC_TARGET_2 + "</target>\n" +
          "        <unit>" + PRIORITY_TARGET + "</unit>\n" +
          "    </strategicTargets>\n" +
          "</behavior>";
    static final String BEHAVIOR_XML_NULL_AUTO_FLEE = "<behavior>\n" +
          "    <name>" + GOOD_BEHAVIOR_NAME + "</name>\n" +
          "    <retreatEdge>" + GOOD_HOME_EDGE + "</retreatEdge>\n" +
          "    <destinationEdge>" + GOOD_DESTINATION_EDGE + "</destinationEdge>\n" +
          "    <forcedWithdrawal>" + GOOD_FORCED_WITHDRAWAL + "</forcedWithdrawal>\n" +
          "    <autoFlee>" + null + "</autoFlee>\n" +
          "    <fallShameIndex>" + GOOD_FALL_SHAME_INDEX + "</fallShameIndex>\n" +
          "    <hyperAggressionIndex>" + GOOD_HYPER_AGGRESSION_INDEX + "</hyperAggressionIndex>\n" +
          "    <selfPreservationIndex>" + GOOD_SELF_PRESERVATION_INDEX + "</selfPreservationIndex>\n" +
          "    <herdMentalityIndex>" + GOOD_HERD_MENTALITY_INDEX + "</herdMentalityIndex>\n" +
          "    <braveryIndex>" + GOOD_BRAVERY_INDEX + "</braveryIndex>\n" +
          "    <strategicTargets>\n" +
          "        <target>" + STRATEGIC_TARGET_1 + "</target>\n" +
          "        <target>" + STRATEGIC_TARGET_2 + "</target>\n" +
          "        <unit>" + PRIORITY_TARGET + "</unit>\n" +
          "    </strategicTargets>\n" +
          "</behavior>";
    static final String BEHAVIOR_XML_TOO_BIG_FALL_SHAME = "<behavior>\n" +
          "    <name>" + GOOD_BEHAVIOR_NAME + "</name>\n" +
          "    <retreatEdge>" + GOOD_HOME_EDGE + "</retreatEdge>\n" +
          "    <destinationEdge>" + GOOD_DESTINATION_EDGE + "</destinationEdge>\n" +
          "    <forcedWithdrawal>" + GOOD_FORCED_WITHDRAWAL + "</forcedWithdrawal>\n" +
          "    <autoFlee>" + GOOD_AUTO_FLEE + "</autoFlee>\n" +
          "    <fallShameIndex>" + BAD_INDEX_BIG + "</fallShameIndex>\n" +
          "    <hyperAggressionIndex>" + GOOD_HYPER_AGGRESSION_INDEX + "</hyperAggressionIndex>\n" +
          "    <selfPreservationIndex>" + GOOD_SELF_PRESERVATION_INDEX + "</selfPreservationIndex>\n" +
          "    <herdMentalityIndex>" + GOOD_HERD_MENTALITY_INDEX + "</herdMentalityIndex>\n" +
          "    <braveryIndex>" + GOOD_BRAVERY_INDEX + "</braveryIndex>\n" +
          "    <strategicTargets>\n" +
          "        <target>" + STRATEGIC_TARGET_1 + "</target>\n" +
          "        <target>" + STRATEGIC_TARGET_2 + "</target>\n" +
          "        <unit>" + PRIORITY_TARGET + "</unit>\n" +
          "    </strategicTargets>\n" +
          "</behavior>";
    static final String BEHAVIOR_XML_TOO_SMALL_FALL_SHAME = "<behavior>\n" +
          "    <name>" + GOOD_BEHAVIOR_NAME + "</name>\n" +
          "    <retreatEdge>" + GOOD_HOME_EDGE + "</retreatEdge>\n" +
          "    <destinationEdge>" + GOOD_DESTINATION_EDGE + "</destinationEdge>\n" +
          "    <forcedWithdrawal>" + GOOD_FORCED_WITHDRAWAL + "</forcedWithdrawal>\n" +
          "    <autoFlee>" + GOOD_AUTO_FLEE + "</autoFlee>\n" +
          "    <fallShameIndex>" + BAD_INDEX_SMALL + "</fallShameIndex>\n" +
          "    <hyperAggressionIndex>" + GOOD_HYPER_AGGRESSION_INDEX + "</hyperAggressionIndex>\n" +
          "    <selfPreservationIndex>" + GOOD_SELF_PRESERVATION_INDEX + "</selfPreservationIndex>\n" +
          "    <herdMentalityIndex>" + GOOD_HERD_MENTALITY_INDEX + "</herdMentalityIndex>\n" +
          "    <braveryIndex>" + GOOD_BRAVERY_INDEX + "</braveryIndex>\n" +
          "    <strategicTargets>\n" +
          "        <target>" + STRATEGIC_TARGET_1 + "</target>\n" +
          "        <target>" + STRATEGIC_TARGET_2 + "</target>\n" +
          "        <unit>" + PRIORITY_TARGET + "</unit>\n" +
          "    </strategicTargets>\n" +
          "</behavior>";
    static final String BEHAVIOR_XML_NULL_STRATEGIC_TARGET = "<behavior>\n" +
          "    <name>" + GOOD_BEHAVIOR_NAME + "</name>\n" +
          "    <retreatEdge>" + GOOD_HOME_EDGE + "</retreatEdge>\n" +
          "    <destinationEdge>" + GOOD_DESTINATION_EDGE + "</destinationEdge>\n" +
          "    <forcedWithdrawal>" + GOOD_FORCED_WITHDRAWAL + "</forcedWithdrawal>\n" +
          "    <autoFlee>" + GOOD_AUTO_FLEE + "</autoFlee>\n" +
          "    <fallShameIndex>" + GOOD_FALL_SHAME_INDEX + "</fallShameIndex>\n" +
          "    <hyperAggressionIndex>" + GOOD_HYPER_AGGRESSION_INDEX + "</hyperAggressionIndex>\n" +
          "    <selfPreservationIndex>" + GOOD_SELF_PRESERVATION_INDEX + "</selfPreservationIndex>\n" +
          "    <herdMentalityIndex>" + GOOD_HERD_MENTALITY_INDEX + "</herdMentalityIndex>\n" +
          "    <braveryIndex>" + GOOD_BRAVERY_INDEX + "</braveryIndex>\n" +
          "    <strategicTargets>\n" +
          "        <target>" + null + "</target>\n" +
          "    </strategicTargets>\n" +
          "</behavior>";
    static final String BEHAVIOR_XML_EMPTY_STRATEGIC_TARGET = "<behavior>\n" +
          "    <name>" + GOOD_BEHAVIOR_NAME + "</name>\n" +
          "    <retreatEdge>" + GOOD_HOME_EDGE + "</retreatEdge>\n" +
          "    <destinationEdge>" + GOOD_DESTINATION_EDGE + "</destinationEdge>\n" +
          "    <forcedWithdrawal>" + GOOD_FORCED_WITHDRAWAL + "</forcedWithdrawal>\n" +
          "    <autoFlee>" + GOOD_AUTO_FLEE + "</autoFlee>\n" +
          "    <fallShameIndex>" + GOOD_FALL_SHAME_INDEX + "</fallShameIndex>\n" +
          "    <hyperAggressionIndex>" + GOOD_HYPER_AGGRESSION_INDEX + "</hyperAggressionIndex>\n" +
          "    <selfPreservationIndex>" + GOOD_SELF_PRESERVATION_INDEX + "</selfPreservationIndex>\n" +
          "    <herdMentalityIndex>" + GOOD_HERD_MENTALITY_INDEX + "</herdMentalityIndex>\n" +
          "    <braveryIndex>" + GOOD_BRAVERY_INDEX + "</braveryIndex>\n" +
          "    <strategicTargets>\n" +
          "        <target></target>\n" +
          "    </strategicTargets>\n" +
          "</behavior>";
    static final String GOOD_BEHAVIOR_XML_NO_TARGETS = "<behavior>\n" +
          "    <name>" + GOOD_BEHAVIOR_NAME + "</name>\n" +
          "    <retreatEdge>" + GOOD_HOME_EDGE + "</retreatEdge>\n" +
          "    <destinationEdge>" + GOOD_DESTINATION_EDGE + "</destinationEdge>\n" +
          "    <forcedWithdrawal>" + GOOD_FORCED_WITHDRAWAL + "</forcedWithdrawal>\n" +
          "    <autoFlee>" + GOOD_AUTO_FLEE + "</autoFlee>\n" +
          "    <fallShameIndex>" + GOOD_FALL_SHAME_INDEX + "</fallShameIndex>\n" +
          "    <hyperAggressionIndex>" + GOOD_HYPER_AGGRESSION_INDEX + "</hyperAggressionIndex>\n" +
          "    <selfPreservationIndex>" + GOOD_SELF_PRESERVATION_INDEX + "</selfPreservationIndex>\n" +
          "    <herdMentalityIndex>" + GOOD_HERD_MENTALITY_INDEX + "</herdMentalityIndex>\n" +
          "    <braveryIndex>" + GOOD_BRAVERY_INDEX + "</braveryIndex>\n" +
          "    <strategicTargets>\n" +
          "    </strategicTargets>\n" +
          "</behavior>";

}
