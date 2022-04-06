/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.bot.princess;

import megamek.utils.MegaMekXmlUtil;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 9/6/13 6:50 PM
 */
public class BehaviorSettingsFactory {

    private static final String PRINCESS_BEHAVIOR_PATH = "mmconf" + File.separator + "princessBehaviors.xml";

    final Map<String, BehaviorSettings> behaviorMap = new HashMap<>();
    private static BehaviorSettingsFactory instance = new BehaviorSettingsFactory();

    private BehaviorSettingsFactory() {
        init(true);
    }

    public static BehaviorSettingsFactory getInstance() {
        return instance;
    }
    
    /**
     * Initializes the {@link megamek.client.bot.princess.BehaviorSettings} cache. If the cache is empty, it will load from
     * mmconf/princessBehaviors.xml. Also, if the "DEFAULT behavior is missing, it will be added.
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
        if (!behaviorMap.keySet().contains(DEFAULT_BEHAVIOR_DESCRIPTION)) {
            addBehavior(DEFAULT_BEHAVIOR);
        }
        if (!behaviorMap.keySet().contains(BERSERK_BEHAVIOR.getDescription())) {
            addBehavior(BERSERK_BEHAVIOR);
        }
        if (!behaviorMap.keySet().contains(COWARDLY_BEHAVIOR.getDescription())) {
            addBehavior(COWARDLY_BEHAVIOR);
        }
        if (!behaviorMap.keySet().contains(ESCAPE_BEHAVIOR.getDescription())) {
            addBehavior(ESCAPE_BEHAVIOR);
        }
    }

    /**
     * Adds a {@link megamek.client.bot.princess.BehaviorSettings} to the cache.  If a behavior with the same name is already in the cache, it will
     * be overwritten.
     *
     * @param behaviorSettings The {@link megamek.client.bot.princess.BehaviorSettings} to be added to the cache.
     */
    public void addBehavior(BehaviorSettings behaviorSettings) {
        synchronized (behaviorMap) {
            behaviorMap.put(behaviorSettings.getDescription().trim(), behaviorSettings);
        }
    }
    
    /**
     * Removes the behavior setting with the given name from the cache. Returns the BehaviorSettings that was 
     * removed (or null if there was no such BehaviorSettings). 
     */
    public BehaviorSettings removeBehavior(String settingName) {
        synchronized (behaviorMap) {
            return behaviorMap.remove(settingName);
        }
    }

    /**
     * Returns the named {@link megamek.client.bot.princess.BehaviorSettings}.
     *
     * @param desc The name of the behavior; matched to {@link megamek.client.bot.princess.BehaviorSettings#getDescription()}.
     * @return The named behavior or NULL if no match is found.
     */
    public BehaviorSettings getBehavior(String desc) {
        return behaviorMap.get(desc);
    }

    private Document buildPrincessBehaviorDoc() {
        
        try {
            File behaviorFile = new File(PRINCESS_BEHAVIOR_PATH);
            if (!behaviorFile.exists() || !behaviorFile.isFile()) {
                LogManager.getLogger().error("Could not load " + PRINCESS_BEHAVIOR_PATH);
                return null;
            }
            try (InputStream is = new FileInputStream(behaviorFile)) {
                return MegaMekXmlUtil.newSafeDocumentBuilder().parse(is);
            }
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            return null;
        }
    }

    /**
     * Loads the contents of the mmconf/princessBehaviors.xml file into the cache.  If the "DEFAULT" behavior is
     * missing it will be automatically added.
     *
     * @return TRUE if the load completes successfully.
     */
    boolean loadBehaviorSettings(Document princessBehaviorDoc) {
        synchronized (behaviorMap) {
            try {
                if (princessBehaviorDoc == null) {
                    return false;
                }
                Element root = princessBehaviorDoc.getDocumentElement();
                BehaviorSettings behaviorSettings;
                for (int i = 0; i < root.getChildNodes().getLength(); i++) {
                    Node child = root.getChildNodes().item(i);
                    if (!"behavior".equalsIgnoreCase(child.getNodeName())) {
                        continue;
                    }
                    behaviorSettings = new BehaviorSettings((Element) child);
                    addBehavior(behaviorSettings);
                }
                return true;
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
                return false;
            } finally {
                addDefaultBehaviors();
            }
        }
    }

    /**
     * Saves the contents of the cache to the mmconf/princessBehaviors.xml file.
     *
     * @param includeTargets Set TRUE to include the contents of the Strategic Targets list.
     * @return TRUE if the save is successful.
     */
    public boolean saveBehaviorSettings(boolean includeTargets) {
        init(false);

        try {
            File behaviorFile = new File(PRINCESS_BEHAVIOR_PATH);
            if (!behaviorFile.exists()) {
                if (!behaviorFile.createNewFile()) {
                    LogManager.getLogger().error("Could not create " + PRINCESS_BEHAVIOR_PATH);
                    return false;
                }
            }
            if (!behaviorFile.canWrite()) {
                LogManager.getLogger().error("Could not write to " + PRINCESS_BEHAVIOR_PATH);
                return false;
            }

            Document behaviorDoc = MegaMekXmlUtil.newSafeDocumentBuilder().newDocument();
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
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(behaviorDoc);
            try (Writer writer = new FileWriter(behaviorFile)) {
                StreamResult result = new StreamResult(writer);
                transformer.transform(source, result);
                return true;
            }
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            return false;
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

    //******************
    // DEFAULT BEHAVIORS
    //******************
    /**
     * Destination Edge: {@link CardinalEdge#NONE} <br>
     * Retreat Edge: {@link CardinalEdge#NONE} <br>
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
    // Used by MekHQ
    @SuppressWarnings("WeakerAccess")
    public final BehaviorSettings BERSERK_BEHAVIOR = buildBerserkBehavior();
    @SuppressWarnings("WeakerAccess")
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
            return berserkBehavior;
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            return null;
        }
    }

    /**
     * Destination Edge: {@link CardinalEdge#NONE} <br>
     * Retreat Edge: {@link CardinalEdge#NEAREST} <br>
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
            return cowardlyBehavior;
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            return null;
        }
    }

    /**
     * Destination Edge: {@link CardinalEdge#NONE} <br>
     * Retreat Edge: {@link CardinalEdge#NEAREST} <br>
     * Forced Withdrawal: True <br>
     * Go Home: True <br>
     * Auto Flee: True <br>
     * Fall Shame: 7 <br>
     * Hyper Aggression: 3 <br>
     * Self Preservation: 10 <br>
     * Herd Mentality: 5 <br>
     * Bravery: 2 <br>
     * Strategic Targets: None
     */
    // Used by MekHQ
    @SuppressWarnings("WeakerAccess")
    public final BehaviorSettings ESCAPE_BEHAVIOR = buildEscapeBehavior();
    @SuppressWarnings("WeakerAccess")
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
            return escapeBehavior;
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            return null;
        }
    }

    /**
     * Destination Edge: {@link CardinalEdge#NONE} <br>
     * Retreat Edge: {@link CardinalEdge#NEAREST} <br>
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
            return defaultBehavior;
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            return null;
        }
    }
    //******************
    // DEFAULT BEHAVIORS
    //******************

}
