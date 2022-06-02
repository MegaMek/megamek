/*
 * Copyright (c) 2000-2011 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.bot.princess;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/6/13 10:51 PM
 */
public class BehaviorSettingsFactoryTestConstants {

    public static final String NM_RECKLESS = "reckless";
    /**
     * Home Edge: {@link CardinalEdge#NORTH} <br>
     * Forced Withdrawal: False <br>
     * Go Home: False <br>
     * Auto Flee: False <br>
     * Fall Shame: 2 <br>
     * Hyper Aggression: 10 <br>
     * Self Preservation: 2 <br>
     * Herd Mentality: 5 <br>
     * Bravery: 9 <br>
     * Strategic Targets: None
     */
    public static final String RECKLESS =
            "    <behavior>\n" +
                    "        <name>" + NM_RECKLESS + "</name>\n" +
                    "        <retreatEdge>0</retreatEdge>\n" +
                    "        <destinationEdge>0</destinationEdge>\n" +
                    "        <forcedWithdrawal>false</forcedWithdrawal>\n" +
                    "        <autoFlee>false</autoFlee>\n" +
                    "        <fallShameIndex>2</fallShameIndex>\n" +
                    "        <hyperAggressionIndex>10</hyperAggressionIndex>\n" +
                    "        <selfPreservationIndex>2</selfPreservationIndex>\n" +
                    "        <herdMentalityIndex>5</herdMentalityIndex>\n" +
                    "        <braveryIndex>9</braveryIndex>\n" +
                    "        <strategicTargets/>\n" +
                    "    </behavior>\n";

    public static final String NM_COWARDLY = BehaviorSettingsFactory.COWARDLY_BEHAVIOR_DESCRIPTION;
    /**
     * Home Edge: {@link CardinalEdge#NORTH} <br>
     * Forced Withdrawal: True <br>
     * Go Home: False <br>
     * Auto Flee: False <br>
     * Fall Shame: 8 <br>
     * Hyper Aggression: 1 <br>
     * Self Preservation: 10 <br>
     * Herd Mentality: 8 <br>
     * Bravery: 2 <br>
     * Strategic Targets: None
     */
    public static final String COWARDLY =
            "    <behavior>\n" +
                    "        <name>" + NM_COWARDLY + "</name>\n" +
                    "        <retreatEdge>0</retreatEdge>\n" +
                    "        <destinationEdge>4</destinationEdge>\n" +
                    "        <forcedWithdrawal>true</forcedWithdrawal>\n" +
                    "        <autoFlee>false</autoFlee>\n" +
                    "        <fallShameIndex>8</fallShameIndex>\n" +
                    "        <hyperAggressionIndex>1</hyperAggressionIndex>\n" +
                    "        <selfPreservationIndex>10</selfPreservationIndex>\n" +
                    "        <herdMentalityIndex>8</herdMentalityIndex>\n" +
                    "        <braveryIndex>2</braveryIndex>\n" +
                    "        <strategicTargets/>\n" +
                    "    </behavior>\n";

    public static final String NM_ESCAPE = BehaviorSettingsFactory.ESCAPE_BEHAVIOR_DESCRIPTION;
    /**
     * Home Edge: {@link CardinalEdge#NORTH} <br>
     * Forced Withdrawal: True <br>
     * Go Home: True <br>
     * Auto Flee: True <br>
     * Fall Shame: 7 <br>
     * Hyper Aggression: 1 <br>
     * Self Preservation: 10 <br>
     * Herd Mentality: 5 <br>
     * Bravery: 2 <br>
     * Strategic Targets: None
     */
    public static final String ESCAPE =
            "    <behavior>\n" +
                    "        <name>" + NM_ESCAPE + "</name>\n" +
                    "        <retreatEdge>0</retreatEdge>\n" +
                    "        <destinationEdge>0</destinationEdge>\n" +
                    "        <forcedWithdrawal>true</forcedWithdrawal>\n" +
                    "        <autoFlee>true</autoFlee>\n" +
                    "        <fallShameIndex>7</fallShameIndex>\n" +
                    "        <hyperAggressionIndex>1</hyperAggressionIndex>\n" +
                    "        <selfPreservationIndex>10</selfPreservationIndex>\n" +
                    "        <herdMentalityIndex>5</herdMentalityIndex>\n" +
                    "        <braveryIndex>2</braveryIndex>\n" +
                    "        <strategicTargets/>\n" +
                    "    </behavior>\n";

    public static final String NM_DEFAULT = BehaviorSettingsFactory.DEFAULT_BEHAVIOR_DESCRIPTION;
    /**
     * Home Edge: {@link megamek.client.bot.princess.HomeEdge#NORTH} <br>
     * Forced Withdrawal: True <br>
     * Go Home: False <br>
     * Auto Flee: False <br>
     * Fall Shame: 5 <br>
     * Hyper Aggression: 5 <br>
     * Self Preservation: 5 <br>
     * Herd Mentality: 5 <br>
     * Bravery: 5 <br>
     * Strategic Targets: None <br>
     */
    public static final String DEFAULT =
            "    <behavior>\n" +
                    "        <name>" + NM_DEFAULT + "</name>\n" +
                    "        <retreatEdge>0</retreatEdge>\n" +
                    "        <destinationEdge>4</destinationEdge>\n" +
                    "        <forcedWithdrawal>true</forcedWithdrawal>\n" +
                    "        <autoFlee>false</autoFlee>\n" +
                    "        <fallShameIndex>5</fallShameIndex>\n" +
                    "        <hyperAggressionIndex>5</hyperAggressionIndex>\n" +
                    "        <selfPreservationIndex>5</selfPreservationIndex>\n" +
                    "        <herdMentalityIndex>5</herdMentalityIndex>\n" +
                    "        <braveryIndex>5</braveryIndex>\n" +
                    "        <strategicTargets/>\n" +
                    "    </behavior>\n";


    /**
     * Contains 4 behaviors: {@link #DEFAULT}, {@link #RECKLESS}, {@link #COWARDLY} and {@link #ESCAPE}.
     */
    public static final String GOOD_BEHAVIOR_SETTINGS_FILE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                    "<princessBehaviors>\n" + RECKLESS + COWARDLY + ESCAPE + DEFAULT + "</princessBehaviors>";
}
