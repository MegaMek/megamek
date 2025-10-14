/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/6/13 6:50 PM
 */
public class BehaviorSettingsFactory {
    private final static MMLogger logger = MMLogger.create(BehaviorSettingsFactory.class);

    private static final String PRINCESS_BEHAVIOR_PATH = "mmconf" + File.separator + "princessBehaviors.xml";

    final Map<String, BehaviorSettings> behaviorMap = new HashMap<>();
    private static final BehaviorSettingsFactory instance = new BehaviorSettingsFactory();

    private BehaviorSettingsFactory() {
        init(true);
    }

    public static BehaviorSettingsFactory getInstance() {
        return instance;
    }

    /**
     * Initializes the {@link megamek.client.bot.princess.BehaviorSettings} cache. If the cache is empty, it will load
     * from mmconf/princessBehaviors.xml. Also, if the "DEFAULT" behavior is missing, it will be added.
     *
     * @param reinitialize Set TRUE to force the cache to be completely rebuilt.
     */
    public void init(boolean reinitialize) {
        synchronized (behaviorMap) {
            if (reinitialize) {
                behaviorMap.clear();
            }
            if (behaviorMap.isEmpty()) {
                loadBehaviorSettings(buildPrincessBehaviorDoc());
            }
            addDefaultBehaviors();
        }
    }

    private void addDefaultBehaviors() {
        if (!behaviorMap.containsKey(DEFAULT_BEHAVIOR_DESCRIPTION)) {
            addBehavior(DEFAULT_BEHAVIOR);
        }
        if (BERSERK_BEHAVIOR != null && !behaviorMap.containsKey(BERSERK_BEHAVIOR.getDescription())) {
            addBehavior(BERSERK_BEHAVIOR);
        }
        if (COWARDLY_BEHAVIOR != null && !behaviorMap.containsKey(COWARDLY_BEHAVIOR.getDescription())) {
            addBehavior(COWARDLY_BEHAVIOR);
        }
        if (ESCAPE_BEHAVIOR != null && !behaviorMap.containsKey(ESCAPE_BEHAVIOR.getDescription())) {
            addBehavior(ESCAPE_BEHAVIOR);
        }
        if (RUTHLESS_BEHAVIOR != null && !behaviorMap.containsKey(RUTHLESS_BEHAVIOR.getDescription())) {
            addBehavior(RUTHLESS_BEHAVIOR);
        }
        if (PIRATE_BEHAVIOR != null && !behaviorMap.containsKey(PIRATE_BEHAVIOR.getDescription())) {
            addBehavior(PIRATE_BEHAVIOR);
        }
        if (CONVOY_BEHAVIOR != null && !behaviorMap.containsKey(CONVOY_BEHAVIOR.getDescription())) {
            addBehavior(CONVOY_BEHAVIOR);
        }
    }

    /**
     * Adds a {@link megamek.client.bot.princess.BehaviorSettings} to the cache. If a behavior with the same name is
     * already in the cache, it will be overwritten.
     *
     * @param behaviorSettings The {@link megamek.client.bot.princess.BehaviorSettings} to be added to the cache.
     */
    public void addBehavior(BehaviorSettings behaviorSettings) {
        synchronized (behaviorMap) {
            behaviorMap.put(behaviorSettings.getDescription().trim(), behaviorSettings);
        }
    }

    /**
     * Removes the behavior setting with the given name from the cache. Returns the BehaviorSettings that was removed
     * (or null if there was no such BehaviorSettings).
     */
    public void removeBehavior(String settingName) {
        synchronized (behaviorMap) {
            behaviorMap.remove(settingName);
        }
    }

    /**
     * Returns the named {@link megamek.client.bot.princess.BehaviorSettings}.
     *
     * @param desc The name of the behavior; matched to
     *             {@link megamek.client.bot.princess.BehaviorSettings#getDescription()}.
     *
     * @return The named behavior or NULL if no match is found.
     */
    public BehaviorSettings getBehavior(String desc) {
        return behaviorMap.get(desc);
    }

    /**
     * Returns the named {@link megamek.client.bot.princess.BehaviorSettings}.
     *
     * @param desc The name of the behavior; matched to
     *             {@link megamek.client.bot.princess.BehaviorSettings#getDescription()}.
     *
     * @return The named behavior or default if no match is found.
     */
    public BehaviorSettings getBehaviorOrDefault(String desc, BehaviorSettings defaultBehavior) {
        return behaviorMap.getOrDefault(desc, defaultBehavior);
    }

    private @Nullable Document buildPrincessBehaviorDoc() {
        try {
            File behaviorFile = new File(PRINCESS_BEHAVIOR_PATH);
            if (!behaviorFile.exists() || !behaviorFile.isFile()) {
                String message = String.format("Could not load %s", PRINCESS_BEHAVIOR_PATH);
                logger.error(message);
                return null;
            }

            try (InputStream is = new FileInputStream(behaviorFile)) {
                return MMXMLUtility.newSafeDocumentBuilder().parse(is);
            }
        } catch (Exception ex) {
            logger.error(ex, "Build Princess Exception");
            return null;
        }
    }

    /**
     * Loads the contents of the mmconf/princessBehaviors.xml file into the cache. If the "DEFAULT" behavior is missing
     * it will be automatically added.
     *
     * @return TRUE if the load completes successfully.
     */
    boolean loadBehaviorSettings(Document princessBehaviorDoc) {
        synchronized (behaviorMap) {
            addDefaultBehaviors();

            if (princessBehaviorDoc == null) {
                return false;
            }

            Element root = princessBehaviorDoc.getDocumentElement();
            BehaviorSettings behaviorSettings;

            try {
                for (int i = 0; i < root.getChildNodes().getLength(); i++) {
                    Node child = root.getChildNodes().item(i);

                    if (!"behavior".equalsIgnoreCase(child.getNodeName())) {
                        continue;
                    }

                    behaviorSettings = new BehaviorSettings((Element) child);
                    addBehavior(behaviorSettings);
                }
            } catch (Exception e) {
                logger.error(e, "Load Behavior Settings Exception");
                return false;
            }

            return true;
        }
    }

    /**
     * Saves the contents of the cache to the mmconf/princessBehaviors.xml file.
     *
     * @param includeTargets Set TRUE to include the contents of the Strategic Targets list.
     */
    public void saveBehaviorSettings(boolean includeTargets) {
        init(false);

        try {
            File behaviorFile = new File(PRINCESS_BEHAVIOR_PATH);

            if (!behaviorFile.exists()) {
                if (!behaviorFile.createNewFile()) {
                    String message = String.format("Could not create %s", PRINCESS_BEHAVIOR_PATH);
                    logger.error(message);
                    return;
                }
            }

            if (!behaviorFile.canWrite()) {
                String message = String.format("Could not write to %s", PRINCESS_BEHAVIOR_PATH);
                logger.error(message);
                return;
            }

            Document behaviorDoc = MMXMLUtility.newSafeDocumentBuilder().newDocument();
            Node rootNode = behaviorDoc.createElement("princessBehaviors");

            synchronized (behaviorMap) {
                for (String key : behaviorMap.keySet()) {
                    BehaviorSettings settings = behaviorMap.get(key);
                    rootNode.appendChild(settings.toXml(behaviorDoc, includeTargets));
                }
            }
            behaviorDoc.appendChild(rootNode);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{https://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(behaviorDoc);

            Writer writer = new FileWriter(behaviorFile);
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
        } catch (Exception e) {
            logger.error(e, "Save Behavior Settings Exception");
        }

    }

    /**
     * @return an array of the names of all the {@link megamek.client.bot.princess.BehaviorSettings} in the cache.
     */
    public String[] getBehaviorNames() {
        init(false);
        List<String> names;

        synchronized (behaviorMap) {
            names = new ArrayList<>(behaviorMap.keySet());
        }

        Collections.sort(names);
        return names.toArray(new String[0]);
    }

    /**
     * Returns a list of the names of all the available
     * {@link megamek.client.bot.princess.BehaviorSettings BehaviorSettings}.
     */
    public List<String> getBehaviorNameList() {
        init(false);
        synchronized (behaviorMap) {
            return new ArrayList<>(behaviorMap.keySet());
        }
    }

    // ******************
    // DEFAULT BEHAVIORS
    // ******************
    /**
     * Destination Edge: {@link CardinalEdge#NONE} <br> Retreat Edge: {@link CardinalEdge#NONE} <br> Forced Withdrawal:
     * False <br> Go Home: False <br> Auto Flee: False <br> Fall Shame: 2 <br> Hyper Aggression: 10 <br> Self
     * Preservation: 2 <br> Herd Mentality: 5 <br> Bravery: 9 <br> Anti-Crowding: 0 <br> Favor Higher TMM: 0 <br>
     * Strategic Targets: None
     */
    // Used by MekHQ
    public final BehaviorSettings BERSERK_BEHAVIOR = buildBerserkBehavior();
    public static final String BERSERK_BEHAVIOR_DESCRIPTION = "BERSERK";

    private BehaviorSettings buildBerserkBehavior() {
        try {
            BehaviorSettings berserkBehavior = new BehaviorSettings();
            berserkBehavior.setDescription(BERSERK_BEHAVIOR_DESCRIPTION);
            berserkBehavior.setDestinationEdge(CardinalEdge.NONE);
            berserkBehavior.setRetreatEdge(CardinalEdge.NONE);
            berserkBehavior.setForcedWithdrawal(false);
            berserkBehavior.setAutoFlee(false);
            berserkBehavior.setFallShameIndex(2);
            berserkBehavior.setHyperAggressionIndex(10);
            berserkBehavior.setSelfPreservationIndex(2);
            berserkBehavior.setHerdMentalityIndex(5);
            berserkBehavior.setBraveryIndex(9);
            berserkBehavior.setAntiCrowding(0);
            berserkBehavior.setFavorHigherTMM(0);
            berserkBehavior.setNumberOfEnemiesToConsiderFacing(BehaviorSettings.DEFAULT_NUMBER_OF_ENEMIES_TO_CONSIDER_FACING);
            berserkBehavior.setAllowFacingTolerance(BehaviorSettings.DEFAULT_ALLOW_FACING_TOLERANCE);
            berserkBehavior.setExclusiveHerding(false);
            return berserkBehavior;
        } catch (Exception e) {
            logger.error(e, "Berserker Behavior Exception");
            return null;
        }
    }

    /**
     * Destination Edge: {@link CardinalEdge#NONE} <br> Retreat Edge: {@link CardinalEdge#NEAREST} <br> Forced
     * Withdrawal: True <br> Go Home: False <br> Auto Flee: False <br> Fall Shame: 8 <br> Hyper Aggression: 1 <br> Self
     * Preservation: 10 <br> Herd Mentality: 8 <br> Bravery: 2 <br> Anti-Crowding: 0 <br> Favor Higher TMM: 0 <br>
     * Strategic Targets: None
     */
    public final BehaviorSettings COWARDLY_BEHAVIOR = buildCowardlyBehavior();
    public static final String COWARDLY_BEHAVIOR_DESCRIPTION = "COWARDLY";

    private BehaviorSettings buildCowardlyBehavior() {
        try {
            BehaviorSettings cowardlyBehavior = new BehaviorSettings();
            cowardlyBehavior.setDescription(COWARDLY_BEHAVIOR_DESCRIPTION);
            cowardlyBehavior.setDestinationEdge(CardinalEdge.NONE);
            cowardlyBehavior.setRetreatEdge(CardinalEdge.NEAREST);
            cowardlyBehavior.setForcedWithdrawal(true);
            cowardlyBehavior.setAutoFlee(false);
            cowardlyBehavior.setFallShameIndex(8);
            cowardlyBehavior.setHyperAggressionIndex(1);
            cowardlyBehavior.setSelfPreservationIndex(10);
            cowardlyBehavior.setHerdMentalityIndex(8);
            cowardlyBehavior.setBraveryIndex(2);
            cowardlyBehavior.setAntiCrowding(0);
            cowardlyBehavior.setFavorHigherTMM(0);
            cowardlyBehavior.setNumberOfEnemiesToConsiderFacing(BehaviorSettings.DEFAULT_NUMBER_OF_ENEMIES_TO_CONSIDER_FACING);
            cowardlyBehavior.setAllowFacingTolerance(BehaviorSettings.DEFAULT_ALLOW_FACING_TOLERANCE);
            cowardlyBehavior.setExclusiveHerding(false);
            return cowardlyBehavior;
        } catch (Exception e) {
            logger.error(e, "Cowardly Behavior Exception");
            return null;
        }
    }

    /**
     * Destination Edge: {@link CardinalEdge#NONE} <br> Retreat Edge: {@link CardinalEdge#NEAREST} <br> Forced
     * Withdrawal: True <br> Go Home: True <br> Auto Flee: True <br> Fall Shame: 7 <br> Hyper Aggression: 3 <br> Self
     * Preservation: 10 <br> Herd Mentality: 5 <br> Bravery: 2 <br> Anti-Crowding: 0 <br> Favor Higher TMM: 0 <br>
     * Strategic Targets: None
     */
    // Used by MekHQ
    public final BehaviorSettings ESCAPE_BEHAVIOR = buildEscapeBehavior();
    public static final String ESCAPE_BEHAVIOR_DESCRIPTION = "ESCAPE";

    private BehaviorSettings buildEscapeBehavior() {
        try {
            BehaviorSettings escapeBehavior = new BehaviorSettings();
            escapeBehavior.setDescription(ESCAPE_BEHAVIOR_DESCRIPTION);
            escapeBehavior.setDestinationEdge(CardinalEdge.NONE);
            escapeBehavior.setRetreatEdge(CardinalEdge.NEAREST);
            escapeBehavior.setForcedWithdrawal(true);
            escapeBehavior.setAutoFlee(true);
            escapeBehavior.setFallShameIndex(7);
            escapeBehavior.setHyperAggressionIndex(3);
            escapeBehavior.setSelfPreservationIndex(10);
            escapeBehavior.setHerdMentalityIndex(5);
            escapeBehavior.setBraveryIndex(2);
            escapeBehavior.setAntiCrowding(0);
            escapeBehavior.setFavorHigherTMM(0);
            escapeBehavior.setNumberOfEnemiesToConsiderFacing(BehaviorSettings.DEFAULT_NUMBER_OF_ENEMIES_TO_CONSIDER_FACING);
            escapeBehavior.setAllowFacingTolerance(BehaviorSettings.DEFAULT_ALLOW_FACING_TOLERANCE);
            escapeBehavior.setExclusiveHerding(false);
            return escapeBehavior;
        } catch (Exception e) {
            logger.error(e, "Escape Behavior Exception");
            return null;
        }
    }

    /**
     * Destination Edge: {@link CardinalEdge#NONE} <br> Retreat Edge: {@link CardinalEdge#NEAREST} <br> Forced
     * Withdrawal: True <br> Go Home: True <br> Auto Flee: True <br> Fall Shame: 7 <br> Hyper Aggression: 3 <br> Self
     * Preservation: 10 <br> Herd Mentality: 5 <br> Bravery: 2 <br> Anti-Crowding: 0 <br> Favor Higher TMM: 0 <br>
     * Strategic Targets: None
     */
    // Used by MekHQ
    public final BehaviorSettings RUTHLESS_BEHAVIOR = buildRuthlessBehavior();
    public static final String RUTHLESS_BEHAVIOR_DESCRIPTION = "RUTHLESS";

    private BehaviorSettings buildRuthlessBehavior() {
        try {
            BehaviorSettings ruthlessBehavior = new BehaviorSettings();
            ruthlessBehavior.setDescription(RUTHLESS_BEHAVIOR_DESCRIPTION);
            ruthlessBehavior.setDestinationEdge(CardinalEdge.NONE);
            ruthlessBehavior.setRetreatEdge(CardinalEdge.NEAREST);
            ruthlessBehavior.setForcedWithdrawal(true);
            ruthlessBehavior.setAutoFlee(false);
            ruthlessBehavior.setFallShameIndex(6);
            ruthlessBehavior.setHyperAggressionIndex(9);
            ruthlessBehavior.setSelfPreservationIndex(10);
            ruthlessBehavior.setHerdMentalityIndex(1);
            ruthlessBehavior.setBraveryIndex(7);
            ruthlessBehavior.setAntiCrowding(10);
            ruthlessBehavior.setFavorHigherTMM(8);
            ruthlessBehavior.setNumberOfEnemiesToConsiderFacing(2);
            ruthlessBehavior.setAllowFacingTolerance(0);
            ruthlessBehavior.setExclusiveHerding(true);
            return ruthlessBehavior;
        } catch (Exception e) {
            logger.error(e, "Ruthless Behavior Exception");
            return null;
        }
    }

    /**
     * Destination Edge: {@link CardinalEdge#NONE} <br> Retreat Edge: {@link CardinalEdge#NEAREST} <br> Forced
     * Withdrawal: True <br> Go Home: True <br> Auto Flee: True <br> Fall Shame: 7 <br> Hyper Aggression: 3 <br> Self
     * Preservation: 10 <br> Herd Mentality: 5 <br> Bravery: 2 <br> Anti-Crowding: 0 <br> Favor Higher TMM: 0 <br>
     * Strategic Targets: None
     */
    // Used by MekHQ
    public final BehaviorSettings PIRATE_BEHAVIOR = buildPirateBehavior();
    public static final String PIRATE_BEHAVIOR_DESCRIPTION = "PIRATE";

    private BehaviorSettings buildPirateBehavior() {
        try {
            BehaviorSettings pirateBehavior = new BehaviorSettings();
            pirateBehavior.setDescription(PIRATE_BEHAVIOR_DESCRIPTION);
            pirateBehavior.setDestinationEdge(CardinalEdge.NONE);
            pirateBehavior.setRetreatEdge(CardinalEdge.NEAREST);
            pirateBehavior.setForcedWithdrawal(true);
            pirateBehavior.setAutoFlee(false);
            pirateBehavior.setFallShameIndex(3);
            pirateBehavior.setHyperAggressionIndex(10);
            pirateBehavior.setSelfPreservationIndex(6);
            pirateBehavior.setHerdMentalityIndex(9);
            pirateBehavior.setBraveryIndex(10);
            pirateBehavior.setAntiCrowding(5);
            pirateBehavior.setFavorHigherTMM(5);
            pirateBehavior.setIAmAPirate(true);
            pirateBehavior.setNumberOfEnemiesToConsiderFacing(1);
            pirateBehavior.setAllowFacingTolerance(2);
            pirateBehavior.setExclusiveHerding(true);
            return pirateBehavior;
        } catch (Exception e) {
            logger.error(e, "Pirate Behavior Exception");
            return null;
        }
    }

    /**
     * Destination Edge: {@link CardinalEdge#NONE} <br> Retreat Edge: {@link CardinalEdge#NEAREST} <br> Forced
     * Withdrawal: True <br> Go Home: True <br> Auto Flee: True <br> Fall Shame: 6 <br> Hyper Aggression: 3 <br> Self
     * Preservation: 10 <br> Herd Mentality: 5 <br> Bravery: 2 <br> Anti-Crowding: 5 <br> Favor Higher TMM: 10 <br>
     * Ignore Damage Output: True <br> Strategic Targets: None
     */
    // Used by MekHQ
    public final BehaviorSettings CONVOY_BEHAVIOR = buildConvoyBehavior();
    public static final String CONVOY_BEHAVIOR_DESCRIPTION = "CONVOY";

    private BehaviorSettings buildConvoyBehavior() {
        try {
            BehaviorSettings convoyBehavior = new BehaviorSettings();
            convoyBehavior.setDescription(CONVOY_BEHAVIOR_DESCRIPTION);
            convoyBehavior.setDestinationEdge(CardinalEdge.NONE);
            convoyBehavior.setRetreatEdge(CardinalEdge.NEAREST);

            convoyBehavior.setIgnoreDamageOutput(true);
            convoyBehavior.setForcedWithdrawal(true);
            convoyBehavior.setAutoFlee(true);
            convoyBehavior.setExclusiveHerding(true);

            convoyBehavior.setFallShameIndex(6);
            convoyBehavior.setHyperAggressionIndex(3);
            convoyBehavior.setBraveryIndex(2);
            convoyBehavior.setSelfPreservationIndex(10);
            convoyBehavior.setHerdMentalityIndex(5);
            convoyBehavior.setAntiCrowding(5);
            convoyBehavior.setFavorHigherTMM(10);
            convoyBehavior.setNumberOfEnemiesToConsiderFacing(0);
            convoyBehavior.setAllowFacingTolerance(2);

            return convoyBehavior;
        } catch (Exception e) {
            logger.error(e, "Convoy Behavior Exception");
            return null;
        }
    }

    // ******************
    // DEFAULT BEHAVIORS
    // ******************

    /**
     * Destination Edge: {@link CardinalEdge#NONE} <br> Retreat Edge: {@link CardinalEdge#NEAREST} <br> Forced
     * Withdrawal: True <br> Go Home: False <br> Auto Flee: False <br> Fall Shame: 5 <br> Hyper Aggression: 5 <br> Self
     * Preservation: 5 <br> Herd Mentality: 5 <br> Bravery: 5 <br> Anti-Crowding: 0 <br> Favor Higher TMM: 0 <br>
     * Strategic Targets: None <br>
     */
    public final BehaviorSettings DEFAULT_BEHAVIOR = buildDefaultBehavior();
    public static final String DEFAULT_BEHAVIOR_DESCRIPTION = "DEFAULT";

    private BehaviorSettings buildDefaultBehavior() {
        try {
            BehaviorSettings defaultBehavior = new BehaviorSettings();
            defaultBehavior.setDescription(DEFAULT_BEHAVIOR_DESCRIPTION);
            defaultBehavior.setDestinationEdge(CardinalEdge.NONE);
            defaultBehavior.setRetreatEdge(CardinalEdge.NEAREST);
            defaultBehavior.setForcedWithdrawal(true);
            defaultBehavior.setAutoFlee(false);
            defaultBehavior.setFallShameIndex(5);
            defaultBehavior.setHyperAggressionIndex(5);
            defaultBehavior.setSelfPreservationIndex(5);
            defaultBehavior.setHerdMentalityIndex(5);
            defaultBehavior.setBraveryIndex(5);
            defaultBehavior.setAntiCrowding(0);
            defaultBehavior.setFavorHigherTMM(0);
            defaultBehavior.setNumberOfEnemiesToConsiderFacing(BehaviorSettings.DEFAULT_NUMBER_OF_ENEMIES_TO_CONSIDER_FACING);
            defaultBehavior.setAllowFacingTolerance(BehaviorSettings.DEFAULT_ALLOW_FACING_TOLERANCE);
            defaultBehavior.setExclusiveHerding(false);
            return defaultBehavior;
        } catch (Exception e) {
            logger.error(e, "Default Behavior Exception");
            return new BehaviorSettings();
        }
    }
}
