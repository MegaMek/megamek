/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot.princess;

import megamek.common.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 *
 * @version $Id$
 * @lastModifiedBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 8/17/13 10:47 PM
 */
public class BehaviorSettings {

    protected static final double[] SELF_PRESERVATION_VALUES = new double[]{
            2.5,
            5,
            7.5,
            10,
            12.5,
            15,
            17.5,
            20,
            22.5,
            25,
            30};
    protected static final int[] FALL_SHAME_VALUES = new int[]{
            10,
            20,
            40,
            60,
            80,
            100,
            120,
            140,
            160,
            180,
            200};
    protected static final double[] BRAVERY = new double[]{
            0.1,
            0.3,
            0.6,
            0.9,
            1.2,
            1.5,
            1.8,
            2.1,
            2.4,
            2.7,
            3.0};
    protected static final double[] HYPER_AGGRESSION_VALUES = new double[]{
            0.25,
            0.5,
            1,
            1.5,
            2,
            2.5,
            3,
            3.5,
            4,
            4.5,
            5};
    protected static final double[] HERD_MENTALITY_VALUES = new double[]{
            0.1,
            0.2,
            0.4,
            0.6,
            0.8,
            1.0,
            1.2,
            1.4,
            1.6,
            1.8,
            2.0};

    private String description = BehaviorSettingsFactory.DEFAULT_BEHAVIOR_DESCRIPTION;

    private boolean forcedWithdrawal = true; // Will I follow the Forced Withdrawal rules?
    private boolean goHome = false; // Should I immediately proceed to my home board edge?
    private boolean autoFlee = false; // Should I flee even if I'm not crippled?
    private int selfPreservationIndex = 5; // How worried about enemy damage am I?
    private int fallShameIndex = 5; // How much do I want to avoid failed Piloting Rolls?
    private int hyperAggressionIndex = 5; // How close to I want to get to my enemies?
    private HomeEdge homeEdge = HomeEdge.NORTH; // In which direction will I flee?
    private final Set<String> strategicBuildingTargets = new HashSet<>(); // What (besides enemy units) do I want to
    // blow up?
    private final Set<Integer> priorityUnitTargets = new HashSet<>(); // What units do I especially want to blow up?
    private int herdMentalityIndex = 5; // How close do I want to stick to my teammates?
    private int braveryIndex = 5; // How quickly will I try to escape once damaged?

    public BehaviorSettings() {
    }

    public BehaviorSettings(Element behavior) throws PrincessException {
        fromXml(behavior);
    }

    public BehaviorSettings getCopy() throws PrincessException {
        BehaviorSettings copy = new BehaviorSettings();
        copy.setHomeEdge(getHomeEdge());
        copy.setForcedWithdrawal(isForcedWithdrawal());
        copy.setAutoFlee(shouldAutoFlee());
        copy.setDescription(getDescription());
        copy.setGoHome(shouldGoHome());
        copy.setFallShameIndex(getFallShameIndex());
        copy.setBraveryIndex(getBraveryIndex());
        copy.setHerdMentalityIndex(getHerdMentalityIndex());
        copy.setHyperAggressionIndex(getHyperAggressionIndex());
        copy.setSelfPreservationIndex(getSelfPreservationIndex());
        return copy;
    }

    /**
     * @return TRUE if I should immediately proceed to my home board edge.
     */
    public boolean shouldGoHome() {
        return goHome;
    }

    /**
     * @param goHome Set TRUE if I should immediately proceed to my home board edge.
     */
    public void setGoHome(boolean goHome) {
        this.goHome = goHome;
    }

    /**
     * @return TRUE if I should flee off the board even if I am not crippled or Forced Withdrawal is not in effect.
     */
    public boolean shouldAutoFlee() {
        return autoFlee;
    }

    /**
     * @param autoFlee Set TRUE if I should flee off the board even if I am not crippled or Forced Withdrawal is not in
     *                 effect.
     */
    public void setAutoFlee(boolean autoFlee) {
        this.autoFlee = autoFlee;
    }

    /**
     * Returns the name for this type of behavior.
     *
     * @return the name for this type of behavior.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the name for this type of behavior.  Must be unique in order to save.
     *
     * @param description The name to be used.
     */
    public void setDescription(String description) throws PrincessException {
        if (StringUtil.isNullOrEmpty(description)) {
            throw new PrincessException("Description is required!");
        }
        this.description = description.trim();
    }

    /**
     * A list of hexes that Princess will attempt to move to and attack.
     *
     * @return A list of hexes that Princess will attempt to move to and attack.
     */
    public Set<String> getStrategicBuildingTargets() {
        return new HashSet<>(strategicBuildingTargets);
    }

    /**
     * Adds a target that Princess will attempt to move to and attack.
     *
     * @param target The target to be added.
     */
    public void addStrategicTarget(String target) {
        if (StringUtil.isNullOrEmpty(target)) {
            return;
        }
        strategicBuildingTargets.add(target);
    }

    /**
     * Removes a target that Princess will attempt to move to and attack.
     *
     * @param target The target to be removed.
     */
    public void removeStrategicTarget(String target) {
        strategicBuildingTargets.remove(target);
    }

    /**
     * @return A list of enemy units that Princess will prioritize over others.
     */
    public Set<Integer> getPriorityUnitTargets() {
        return new HashSet<>(priorityUnitTargets);
    }

    /**
     * Add an enemy unit to the priority list.
     *
     * @param id The ID of the unit to be added.
     */
    public void addPriorityUnit(int id) {
        priorityUnitTargets.add(id);
    }

    /**
     * Add an enemy unit to the priority list.
     *
     * @param id The ID of the unit to be added.
     */
    public void addPriorityUnit(String id) {
        if (!StringUtil.isPositiveInteger(id)) {
            return;
        }
        addPriorityUnit(Integer.parseInt(id));
    }

    /**
     * Remove a unit from the priority target list.
     *
     * @param id The ID of the unit to be removed.
     */
    public void removePriorityUnit(int id) {
        priorityUnitTargets.remove(id);
    }

    /**
     * Remove a unit from the priority target list.
     *
     * @param id The ID of the unit to be removed.
     */
    public void removePriorityUnit(String id) {
        if (!StringUtil.isPositiveInteger(id)) {
            return;
        }
        removePriorityUnit(Integer.parseInt(id));
    }

    /**
     * When this is true, Princess will follow the Forced Withdrawal rules from Total Warfare.
     *
     * @return Should Princess follow the Forced Withdrawal rules?
     */
    public boolean isForcedWithdrawal() {
        return forcedWithdrawal;
    }

    /**
     * When this is true, Princess will follow the Forced Withdrawal rules from Total Warfare.
     *
     * @param forcedWithdrawal Should Princess follow the Forced Withdrawal rules?
     */
    public void setForcedWithdrawal(boolean forcedWithdrawal) {
        this.forcedWithdrawal = forcedWithdrawal;
    }

    /**
     * When this is true, Princess will follow the Forced Withdrawal rules from Total Warfare.
     *
     * @param forcedWithdrawal Should Princess follow the Forced Withdrawal rules?
     */
    public void setForcedWithdrawal(String forcedWithdrawal) {
        setForcedWithdrawal("true".equalsIgnoreCase(forcedWithdrawal));
    }

    private int validateIndex(int index) {
        if (index < 0) {
            return 0;
        } else if (index > 10) {
            return 10;
        }
        return index;
    }

    /**
     * How quickly will I try to escape once damaged?
     *
     * @return Index of the Bravery value.
     */
    public int getBraveryIndex() {
        return braveryIndex;
    }

    /**
     * How quickly will I try to escape once damaged?
     *
     * @return Bravery modifier value.
     */
    public double getBraveryValue() {
        return getBraveryValue(braveryIndex);
    }

    /**
     * How quickly will I try to escape once damaged?
     *
     * @param index The index of the Bravery modifier to retrieve.
     * @return Bravery modifier value at given index.
     */
    public double getBraveryValue(int index) {
        return BRAVERY[validateIndex(index)];
    }

    /**
     * How quickly will I try to escape once damaged?
     *
     * @param index The index of the Bravery modifier to be used.
     */
    public void setBraveryIndex(int index) {
        braveryIndex = validateIndex(index);
    }

    /**
     * How quickly will I try to escape once damaged?
     *
     * @param index The index of the Bravery modifier to be used.
     */
    public void setBraveryIndex(String index) throws PrincessException {
        try {
            setBraveryIndex(Integer.parseInt(index));
        } catch (NumberFormatException ex) {
            throw new PrincessException(ex);
        }
    }

    /**
     * @return The index of my current {@link #FALL_SHAME_VALUES}.
     */
    public int getFallShameIndex() {
        return fallShameIndex;
    }

    /**
     * @return How much do I want to avoid failed Piloting Rolls?
     */
    public int getFallShameValue() {
        return getFallShameValue(getFallShameIndex());
    }

    /**
     * @param index The index of the {@link #FALL_SHAME_VALUES} sought.
     * @return The value at the given index.  Indexes less than 0 are treated as 0 and indexes greater than 10 are
     *         treated as 10.
     */
    public int getFallShameValue(int index) {
        return FALL_SHAME_VALUES[validateIndex(index)];
    }

    /**
     * @param index The index of my current {@link #FALL_SHAME_VALUES}.
     */
    public void setFallShameIndex(int index) {
        this.fallShameIndex = validateIndex(index);
    }

    /**
     * @param index The index of my current {@link #FALL_SHAME_VALUES}.
     */
    public void setFallShameIndex(String index) throws PrincessException {
        try {
            setFallShameIndex(Integer.parseInt(index));
        } catch (NumberFormatException ex) {
            throw new PrincessException(ex);
        }
    }

    /**
     * How close do I want to stick to my teammates?
     *
     * @return Index of the current herd mentality value.
     */
    public int getHerdMentalityIndex() {
        return herdMentalityIndex;
    }

    /**
     * How close do I want to stick to my teammates?
     *
     * @return Current herd mentality value.
     */
    public double getHerdMentalityValue() {
        return getHerdMentalityValue(herdMentalityIndex);
    }

    /**
     * How close do I want to stick to my teammates?
     *
     * @param index The index [0-10] of the herd mentality value that should be used.
     * @return The herd mentality value at the specified index.
     */
    public double getHerdMentalityValue(int index) {
        return HERD_MENTALITY_VALUES[validateIndex(index)];
    }

    /**
     * How close do I want to stick to my teammates?
     *
     * @param herdMentalityIndex The index [0-10] of the herd mentality that should be used.
     */
    public void setHerdMentalityIndex(int herdMentalityIndex) {
        this.herdMentalityIndex = validateIndex(herdMentalityIndex);
    }

    /**
     * How close do I want to stick to my teammates?
     *
     * @param index The index ["0"-"10"] of the herd mentality value that should be used.
     */
    public void setHerdMentalityIndex(String index) throws PrincessException {
        try {
            setHerdMentalityIndex(Integer.parseInt(index));
        } catch (NumberFormatException ex) {
            throw new PrincessException(ex);
        }
    }

    /**
     * Princess's home edge.
     *
     * @return The {@link HomeEdge} princess will flee to.
     */
    public HomeEdge getHomeEdge() {
        return homeEdge;
    }

    /**
     * Princess's home edge.
     *
     * @param homeEdge The {@link HomeEdge} princess should flee to.
     */
    public void setHomeEdge(HomeEdge homeEdge) {
        if (homeEdge == null)
            return;

        this.homeEdge = homeEdge;
    }

    /**
     * Princess's home edge.
     *
     * @param homeEdge the index of the {@link HomeEdge} princess should flee to.  See {@link HomeEdge#getIndex()}
     */
    public void setHomeEdge(int homeEdge) {
        setHomeEdge(HomeEdge.getHomeEdge(homeEdge));
    }

    /**
     * Princess's home edge.
     *
     * @param homeEdge the index of the {@link HomeEdge} princess should flee to.  See {@link HomeEdge#getIndex()}
     */
    public void setHomeEdge(String homeEdge) throws PrincessException {
        try {
            setHomeEdge(Integer.parseInt(homeEdge.trim()));
        } catch (NumberFormatException e) {
            throw new PrincessException("Invalid homeEdge value.", e);
        }
    }

    /**
     * How close to I want to get to my enemies?
     *
     * @return Index of the current hyper aggression value.
     */
    public int getHyperAggressionIndex() {
        return hyperAggressionIndex;
    }

    /**
     * How close to I want to get to my enemies?
     *
     * @return Current hyper aggression value.
     */
    public double getHyperAggressionValue() {
        return getHyperAggressionValue(hyperAggressionIndex);
    }

    /**
     * How close to I want to get to my enemies?
     *
     * @param index The index[0-10] of the hyper aggression value desired.
     * @return The hyper aggression value at the given index.
     */
    public double getHyperAggressionValue(int index) {
        return HYPER_AGGRESSION_VALUES[validateIndex(index)];
    }

    /**
     * How close to I want to get to my enemies?
     *
     * @param hyperAggressionIndex The index [0-10] of the hyper aggression value to be used.
     */
    public void setHyperAggressionIndex(int hyperAggressionIndex) {
        this.hyperAggressionIndex = validateIndex(hyperAggressionIndex);
    }

    /**
     * How close to I want to get to my enemies?
     *
     * @param index The index ["0"-"10"] of the hyper aggression value to be used.
     */
    public void setHyperAggressionIndex(String index) throws PrincessException {
        try {
            setHyperAggressionIndex(Integer.parseInt(index));
        } catch (NumberFormatException ex) {
            throw new PrincessException(ex);
        }
    }

    /**
     * How worried about enemy damage am I?
     *
     * @return Index of the current self preservation value.
     */
    public int getSelfPreservationIndex() {
        return selfPreservationIndex;
    }

    /**
     * How worried about enemy damage am I?
     *
     * @return The current self preservation value.
     */
    public double getSelfPreservationValue() {
        return getSelfPreservationValue(selfPreservationIndex);
    }

    /**
     * How worried about enemy damage am I?
     *
     * @param index The index [0-10] of the self preservation value desired.
     * @return The self preservation value at the specified index.
     */
    public double getSelfPreservationValue(int index) {
        if (index < 0) {
            index = 0;
        } else if (index > 10) {
            index = 10;
        }
        return SELF_PRESERVATION_VALUES[index];
    }

    /**
     * How worried about enemy damage am I?
     *
     * @param selfPreservationIndex The index [0-10] of the self preservation value to be used.
     */
    public void setSelfPreservationIndex(int selfPreservationIndex) {
        this.selfPreservationIndex = validateIndex(selfPreservationIndex);
    }

    /**
     * How worried about enemy damage am I?
     *
     * @param index The index ["0"-"10"] of the self preservation value to be used.
     */
    public void setSelfPreservationIndex(String index) throws PrincessException {
        try {
            setSelfPreservationIndex(Integer.parseInt(index));
        } catch (NumberFormatException ex) {
            throw new PrincessException(ex);
        }
    }

    /**
     * Sets up the behavior parameters based on the passed in XML.
     *
     * @param behavior The XML element containing the behavior parameters.
     * @return TRUE if the XML was successfully parsed.
     */
    public boolean fromXml(Element behavior) throws PrincessException {
        NodeList children = behavior.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("name".equalsIgnoreCase(child.getNodeName())) {
                setDescription(child.getTextContent());
            } else if ("forcedWithdrawal".equalsIgnoreCase(child.getNodeName())) {
                setForcedWithdrawal(child.getTextContent());
            } else if ("goHome".equalsIgnoreCase(child.getNodeName())) {
                setGoHome("true".equalsIgnoreCase(child.getTextContent()));
            } else if ("autoFlee".equalsIgnoreCase(child.getNodeName())) {
                setAutoFlee("true".equalsIgnoreCase(child.getTextContent()));
            } else if ("fallShameIndex".equalsIgnoreCase(child.getNodeName())) {
                setFallShameIndex(child.getTextContent());
            } else if ("hyperAggressionIndex".equalsIgnoreCase(child.getNodeName())) {
                setHyperAggressionIndex(child.getTextContent());
            } else if ("selfPreservationIndex".equalsIgnoreCase(child.getNodeName())) {
                setSelfPreservationIndex(child.getTextContent());
            } else if ("homeEdge".equalsIgnoreCase(child.getNodeName())) {
                setHomeEdge(child.getTextContent());
            } else if ("herdMentalityIndex".equalsIgnoreCase(child.getNodeName())) {
                setHerdMentalityIndex(child.getTextContent());
            } else if ("braveryIndex".equalsIgnoreCase(child.getNodeName())) {
                setBraveryIndex(child.getTextContent());
            } else if ("strategicTargets".equalsIgnoreCase(child.getNodeName())) {
                NodeList targets = child.getChildNodes();
                for (int j = 0; j < targets.getLength(); j++) {
                    Node t = targets.item(j);
                    if ("target".equalsIgnoreCase(t.getNodeName())) {
                        addStrategicTarget(t.getTextContent());
                    }
                    if ("unit".equalsIgnoreCase(t.getNodeName())) {
                        addPriorityUnit(t.getTextContent());
                    }
                }
            }
        }
        return true;
    }

    /**
     * @return TRUE if this is the default behavior settings for a princess bot.
     */
    public boolean isDefault() {
        return BehaviorSettingsFactory.DEFAULT_BEHAVIOR_DESCRIPTION.equalsIgnoreCase(description);
    }

    /**
     * Returns an XML representation of the behavior settings.
     *
     * @return An XML {@link org.w3c.dom.Element} describing this behavior settings object.
     */
    public Element toXml(Document doc, boolean includeTargets) {
        try {
            Element behavior = doc.createElement("behavior");

            Element nameNode = doc.createElement("name");
            nameNode.setTextContent(StringUtil.makeXmlSafe(getDescription()));
            behavior.appendChild(nameNode);

            Element homeEdgeNode = doc.createElement("homeEdge");
            homeEdgeNode.setTextContent("" + getHomeEdge().getIndex());
            behavior.appendChild(homeEdgeNode);

            Element forcedWithdrawalNode = doc.createElement("forcedWithdrawal");
            forcedWithdrawalNode.setTextContent("" + isForcedWithdrawal());
            behavior.appendChild(forcedWithdrawalNode);

            Element goHomeNode = doc.createElement("goHome");
            goHomeNode.setTextContent("" + shouldGoHome());
            behavior.appendChild(goHomeNode);

            Element autoFleeNode = doc.createElement("autoFlee");
            autoFleeNode.setTextContent("" + shouldAutoFlee());
            behavior.appendChild(autoFleeNode);

            Element fallShameNode = doc.createElement("fallShameIndex");
            fallShameNode.setTextContent("" + getFallShameIndex());
            behavior.appendChild(fallShameNode);

            Element hyperAggressionNode = doc.createElement("hyperAggressionIndex");
            hyperAggressionNode.setTextContent("" + getHyperAggressionIndex());
            behavior.appendChild(hyperAggressionNode);

            Element selfPreservationNode = doc.createElement("selfPreservationIndex");
            selfPreservationNode.setTextContent("" + getSelfPreservationIndex());
            behavior.appendChild(selfPreservationNode);

            Element herdMentalityNode = doc.createElement("herdMentalityIndex");
            herdMentalityNode.setTextContent("" + getHerdMentalityIndex());
            behavior.appendChild(herdMentalityNode);

            Element braveryNode = doc.createElement("braveryIndex");
            braveryNode.setTextContent("" + getBraveryIndex());
            behavior.appendChild(braveryNode);

            Element targetsNode = doc.createElement("strategicBuildingTargets");
            if (includeTargets) {
                for (String t : getStrategicBuildingTargets()) {
                    Element targetElement = doc.createElement("target");
                    targetElement.setTextContent(StringUtil.makeXmlSafe(t));
                    targetsNode.appendChild(targetElement);
                }
                for (int id : getPriorityUnitTargets()) {
                    Element unitElement = doc.createElement("unit");
                    unitElement.setTextContent(String.valueOf(id));
                    targetsNode.appendChild(unitElement);
                }
            }
            behavior.appendChild(targetsNode);

            return behavior;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @return A string log of these behavior settings.
     */
    public String toLog() {
        String out = "Princess Behavior: " + getDescription();
        out += "\n\tHome Edge: " + getHomeEdge().toString();
        out += "\n\tForced Withdrawal: " + isForcedWithdrawal();
        out += "\n\tSelf Preservation: " + getSelfPreservationIndex();
        out += "\n\tHyper Aggression: " + getHyperAggressionIndex();
        out += "\n\tFall Shame: " + getFallShameIndex();
        out += "\n\tBravery: " + getBraveryIndex();
        out += "\n\tHerd Mentality: " + getHerdMentalityIndex();
        out += "\n\tTargets:";
        out += "\n\t\tCoords: ";
        for (String t : getStrategicBuildingTargets()) {
            out += "  " + t;
        }
        out += "\n\t\tUnits:";
        for (int id : getPriorityUnitTargets()) {
            out += "  " + id;
        }
        return out;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BehaviorSettings)) return false;

        BehaviorSettings that = (BehaviorSettings) o;

        if (autoFlee != that.autoFlee) return false;
        if (braveryIndex != that.braveryIndex) return false;
        if (fallShameIndex != that.fallShameIndex) return false;
        if (forcedWithdrawal != that.forcedWithdrawal) return false;
        if (goHome != that.goHome) return false;
        if (herdMentalityIndex != that.herdMentalityIndex) return false;
        if (hyperAggressionIndex != that.hyperAggressionIndex) return false;
        if (selfPreservationIndex != that.selfPreservationIndex) return false;
        if (!description.equals(that.description)) return false;
        if (homeEdge != that.homeEdge) return false;
        if (strategicBuildingTargets != null ? !strategicBuildingTargets.equals(that.strategicBuildingTargets) : that
                                                                                                                         .strategicBuildingTargets != null) {
            return false;
        }
        if (priorityUnitTargets != null ? !priorityUnitTargets.equals(that.priorityUnitTargets) : that
                                                                                                          .priorityUnitTargets != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = description.hashCode();
        result = 31 * result + (forcedWithdrawal ? 1 : 0);
        result = 31 * result + (goHome ? 1 : 0);
        result = 31 * result + (autoFlee ? 1 : 0);
        result = 31 * result + selfPreservationIndex;
        result = 31 * result + fallShameIndex;
        result = 31 * result + hyperAggressionIndex;
        result = 31 * result + homeEdge.hashCode();
        result = 31 * result + (strategicBuildingTargets != null ? strategicBuildingTargets.hashCode() : 0);
        result = 31 * result + (priorityUnitTargets != null ? priorityUnitTargets.hashCode() : 0);
        result = 31 * result + herdMentalityIndex;
        result = 31 * result + braveryIndex;
        return result;
    }
}
