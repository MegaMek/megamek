/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import megamek.codeUtilities.MathUtility;
import megamek.codeUtilities.StringUtility;
import megamek.common.annotations.Nullable;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 17-Aug-2013 10:47 PM
 */
public class BehaviorSettings implements Serializable {
    private static final MMLogger logger = MMLogger.create(BehaviorSettings.class);

    // region Variable Declarations
    @Serial
    private static final long serialVersionUID = -1895924639830817372L;

    static final double[] SELF_PRESERVATION_VALUES = {
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
            30 };

    static final int[] FALL_SHAME_VALUES = {
            10,
            40,
            80,
            100,
            160,
            500,
            500,
            500,
            500,
            500,
            500 };

    protected static final double[] BRAVERY = {
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
            3.0 };

    static final double[] HYPER_AGGRESSION_VALUES = {
            0.25,
            0.5,
            1,
            1.5,
            2,
            2.5,
            3,
            3.5,
            10,
            50,
            500 };

    static final double[] HERD_MENTALITY_VALUES = {
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
            2.0 };

    public static final int MAX_NUMBER_OF_ENEMIES_TO_CONSIDER_FACING = 12;
    public static final int MIN_NUMBER_OF_ENEMIES_TO_CONSIDER_FACING = 1;
    public static final int DEFAULT_NUMBER_OF_ENEMIES_TO_CONSIDER_FACING = 4;
    public static final int MAX_ALLOW_FACING_TOLERANCE = 2;
    public static final int MIN_ALLOW_FACING_TOLERANCE = 0;
    public static final int DEFAULT_ALLOW_FACING_TOLERANCE = 1;

    private String description = BehaviorSettingsFactory.DEFAULT_BEHAVIOR_DESCRIPTION;

    private boolean forcedWithdrawal = true; // Will I follow the Forced Withdrawal rules?
    private boolean autoFlee = false; // Should I flee even if I'm not crippled?
    private int selfPreservationIndex = 5; // How worried about enemy damage am I?
    private int fallShameIndex = 5; // How much do I want to avoid failed Piloting Rolls?
    private int hyperAggressionIndex = 5; // How close to I want to get to my enemies?
    private CardinalEdge destinationEdge = CardinalEdge.NONE; // Which edge am I trying to reach?
    private CardinalEdge retreatEdge = CardinalEdge.NEAREST; // To which edge will my units flee when crippled?
    private final Set<String> strategicBuildingTargets = new HashSet<>(); // What (besides enemy units) do I want to
                                                                          // blow up?
    private final Set<Integer> priorityUnitTargets = new HashSet<>(); // What units do I especially want to blow up?
    private int herdMentalityIndex = 5; // How close do I want to stick to my teammates?
    private int braveryIndex = 5; // How quickly will I try to escape once damaged?
    private int antiCrowding = 0; // How much do I want to avoid crowding my teammates?
    private int favorHigherTMM = 0; // How much do I want to favor moving in my turn?
    private int numberOfEnemiesToConsiderFacing = DEFAULT_NUMBER_OF_ENEMIES_TO_CONSIDER_FACING; // How many enemies do I want to consider when calculating facing?
    private int allowFacingTolerance = DEFAULT_ALLOW_FACING_TOLERANCE; // How much tolerance do I want to allow for facing?
    private boolean exclusiveHerding = false; // should I only herd with my units or consider also friends?
    private boolean iAmAPirate = false; // Am I a pirate?
    private boolean experimental = false; // running experimental features?
    private final Set<Integer> ignoredUnitTargets = new HashSet<>();
    // endregion Variable Declarations

    public BehaviorSettings() {

    }

    public BehaviorSettings(final Element behavior) throws PrincessException {
        fromXml(behavior);
    }

    public BehaviorSettings getCopy() throws PrincessException {
        final BehaviorSettings copy = new BehaviorSettings();
        copy.setDestinationEdge(getDestinationEdge());
        copy.setRetreatEdge(getRetreatEdge());
        copy.setForcedWithdrawal(isForcedWithdrawal());
        copy.setAutoFlee(shouldAutoFlee());
        copy.setDescription(getDescription());
        copy.setFallShameIndex(getFallShameIndex());
        copy.setBraveryIndex(getBraveryIndex());
        copy.setHerdMentalityIndex(getHerdMentalityIndex());
        copy.setHyperAggressionIndex(getHyperAggressionIndex());
        copy.setSelfPreservationIndex(getSelfPreservationIndex());
        copy.setAntiCrowding(getAntiCrowding());
        copy.setFavorHigherTMM(getFavorHigherTMM());
        copy.setNumberOfEnemiesToConsiderFacing(getNumberOfEnemiesToConsiderFacing());
        copy.setAllowFacingTolerance(getAllowFacingTolerance());
        copy.setExclusiveHerding(isExclusiveHerding());
        copy.setIAmAPirate(iAmAPirate());
        copy.setExperimental(isExperimental());
        for (final String t : getStrategicBuildingTargets()) {
            copy.addStrategicTarget(t);
        }
        for (final Integer p : getPriorityUnitTargets()) {
            copy.addPriorityUnit(p);
        }
        for (final Integer i : getIgnoredUnitTargets()) {
            copy.addIgnoredUnitTarget(i);
        }

        return copy;
    }

    /**
     * @return TRUE if I am running experimental features.
     */
    public boolean isExperimental() {
        return experimental;
    }

    /**
     * @param experimental Set TRUE if I am running experimental features.
     */
    public void setExperimental(boolean experimental) {
        this.experimental = experimental;
    }
    /**
     * @param experimental Set TRUE if I am running experimental features.
     */
    public void setExperimental(String experimental) {
        this.experimental = Boolean.parseBoolean(experimental);
    }

    /**
     * @return TRUE if I am a bloody pirate. Ignores the dishonored enemies list and just attacks.
     */
     public boolean iAmAPirate() {
        return iAmAPirate;
    }

    /**
     * @param iAmAPirate Set TRUE if I am a bloody pirate.
     */
    public void setIAmAPirate(boolean iAmAPirate) {
        this.iAmAPirate = iAmAPirate;
    }

    /**
     * @param iAmAPirate Set TRUE if I am a bloody pirate.
     */
    public void setIAmAPirate(String iAmAPirate) {
        setIAmAPirate(Boolean.parseBoolean(iAmAPirate));
    }

    /**
     * @return TRUE if I should only herd with my units.
     */
    public boolean isExclusiveHerding() {
        return exclusiveHerding;
    }

    /**
     * @param exclusiveHerding Set TRUE if I should only herd with my units.
     */
    public void setExclusiveHerding(boolean exclusiveHerding) {
        this.exclusiveHerding = exclusiveHerding;
    }

    public void setExclusiveHerding(String exclusiveHerding) {
        setExclusiveHerding(Boolean.parseBoolean(exclusiveHerding));
    }

    /**
     * @return TRUE if I should immediately proceed to my home board edge.
     */
    public boolean shouldGoHome() {
        return destinationEdge != CardinalEdge.NONE;
    }

    /**
     * @return TRUE if I should flee off the board even if I am not crippled or
     *         Forced Withdrawal is not in effect.
     */
    public boolean shouldAutoFlee() {
        return autoFlee;
    }

    /**
     * @param autoFlee Set TRUE if I should flee off the board even if I am not
     *                 crippled or Forced Withdrawal is not in
     *                 effect.
     */
    public void setAutoFlee(final boolean autoFlee) {
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
     * Sets the name for this type of behavior. Must be unique in order to save.
     * Throws a PrincessException when the description is empty.
     *
     * @param description The name to be used.
     */
    public void setDescription(final String description) throws PrincessException {
        if (StringUtility.isNullOrBlank(description)) {
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
    public void addStrategicTarget(final String target) {
        if (StringUtility.isNullOrBlank(target)) {
            return;
        }
        strategicBuildingTargets.add(target);
    }

    /**
     * Removes a target that Princess will attempt to move to and attack.
     *
     * @param target The target to be removed.
     */
    void removeStrategicTarget(final String target) {
        strategicBuildingTargets.remove(target);
    }

    /**
     * @return A list of enemy units that Princess will ignore completely.
     */
    public Set<Integer> getIgnoredUnitTargets() {
        return new HashSet<>(ignoredUnitTargets);
    }

    /**
     * Add the given unit ID to the ignored target list.
     */
    public void addIgnoredUnitTarget(int unitID) {
        ignoredUnitTargets.add(unitID);
    }

    /**
     * Remove the given unit ID from the ignored target list.
     */
    @SuppressWarnings("unused")
    public void removeIgnoredUnitTarget(int unitID) {
        ignoredUnitTargets.remove(unitID);
    }

    /**
     * Empty out the ignored target list.
     */
    public void clearIgnoredUnitTargets() {
        ignoredUnitTargets.clear();
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
    public void addPriorityUnit(final int id) {
        priorityUnitTargets.add(id);
    }

    /**
     * Add an enemy unit to the priority list.
     *
     * @param id The ID of the unit to be added.
     */
    public void addPriorityUnit(final String id) {
        if (!StringUtil.isPositiveInteger(id)) {
            return;
        }
        try {
            addPriorityUnit(Integer.parseInt(id));
        } catch (final NumberFormatException ex) {
            logger.error(ex, "Invalid unit ID: {}", id);
        }
    }

    /**
     * Remove a unit from the priority target list.
     *
     * @param id The ID of the unit to be removed.
     */
    void removePriorityUnit(final int id) {
        priorityUnitTargets.remove(id);
    }

    /**
     * Remove a unit from the priority target list.
     *
     * @param id The ID of the unit to be removed.
     */
    void removePriorityUnit(final String id) {
        if (!StringUtil.isPositiveInteger(id)) {
            return;
        }
        try {
            removePriorityUnit(Integer.parseInt(id));
        } catch (final NumberFormatException ex) {
            logger.error(ex, "Invalid unit ID: {}", id);
        }
    }

    /**
     * When this is true, Princess will follow the Forced Withdrawal rules from
     * Total Warfare.
     *
     * @return Should Princess follow the Forced Withdrawal rules?
     */
    public boolean isForcedWithdrawal() {
        return forcedWithdrawal;
    }

    /**
     * When this is true, Princess will follow the Forced Withdrawal rules from
     * Total Warfare.
     *
     * @param forcedWithdrawal Should Princess follow the Forced Withdrawal rules?
     */
    public void setForcedWithdrawal(final boolean forcedWithdrawal) {
        this.forcedWithdrawal = forcedWithdrawal;
    }

    /**
     * When this is true, Princess will follow the Forced Withdrawal rules from
     * Total Warfare.
     *
     * @param forcedWithdrawal Should Princess follow the Forced Withdrawal rules?
     */
    public void setForcedWithdrawal(final String forcedWithdrawal) {
        setForcedWithdrawal("true".equalsIgnoreCase(forcedWithdrawal));
    }

    private int validateIndex(final int index) {
        return MathUtility.clamp(index, 0, 10);
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
    public double getBraveryValue(final int index) {
        return BRAVERY[validateIndex(index)];
    }

    /**
     * How quickly will I try to escape once damaged?
     *
     * @param index The index of the Bravery modifier to be used.
     */
    public void setBraveryIndex(final int index) {
        braveryIndex = validateIndex(index);
    }

    /**
     * How quickly will I try to escape once damaged?
     *
     * @param index The index of the Bravery modifier to be used.
     */
    public void setBraveryIndex(final String index) throws PrincessException {
        try {
            setBraveryIndex(Integer.parseInt(index));
        } catch (final NumberFormatException ex) {
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
     * @return The value at the given index. Indexes less than 0 are treated as 0
     *         and indexes greater than 10 are
     *         treated as 10.
     */
    public int getFallShameValue(final int index) {
        return FALL_SHAME_VALUES[validateIndex(index)];
    }

    /**
     * @param index The index of my current {@link #FALL_SHAME_VALUES}.
     */
    public void setFallShameIndex(final int index) {
        this.fallShameIndex = validateIndex(index);
    }

    /**
     * @param index The index of my current {@link #FALL_SHAME_VALUES}.
     */
    public void setFallShameIndex(final String index) throws PrincessException {
        try {
            setFallShameIndex(Integer.parseInt(index));
        } catch (final NumberFormatException ex) {
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
     * @param index The index [0-10] of the herd mentality value that should be
     *              used.
     * @return The herd mentality value at the specified index.
     */
    public double getHerdMentalityValue(final int index) {
        return HERD_MENTALITY_VALUES[validateIndex(index)];
    }

    /**
     * How close do I want to stick to my teammates?
     *
     * @param herdMentalityIndex The index [0-10] of the herd mentality that should
     *                           be used.
     */
    public void setHerdMentalityIndex(final int herdMentalityIndex) {
        this.herdMentalityIndex = validateIndex(herdMentalityIndex);
    }

    /**
     * How close do I want to stick to my teammates?
     *
     * @param index The index ["0"-"10"] of the herd mentality value that should be
     *              used.
     */
    public void setHerdMentalityIndex(final String index) throws PrincessException {
        try {
            setHerdMentalityIndex(Integer.parseInt(index));
        } catch (final NumberFormatException ex) {
            throw new PrincessException(ex);
        }
    }

    /**
     * Princess's home edge.
     *
     * @return The {@link CardinalEdge} princess will flee to.
     */
    public CardinalEdge getDestinationEdge() {
        return destinationEdge;
    }

    /**
     * Princess's home edge.
     *
     * @param destinationEdge The {@link CardinalEdge} princess should flee to.
     */
    public void setDestinationEdge(final @Nullable CardinalEdge destinationEdge) {
        if (null == destinationEdge) {
            return;
        }

        this.destinationEdge = destinationEdge;
    }

    /**
     * Princess's home edge.
     *
     * @param destinationEdge the index of the {@link CardinalEdge} princess should
     *                        flee to. See {@link CardinalEdge#getIndex()}
     */
    public void setDestinationEdge(final int destinationEdge) {
        setDestinationEdge(CardinalEdge.getCardinalEdge(destinationEdge));
    }

    /**
     * Princess's home edge.
     *
     * @param destinationEdge the index of the {@link CardinalEdge} princess should
     *                        flee to. See {@link CardinalEdge#getIndex()}
     * @deprecated unused
     */
    @Deprecated(since="0.50.06", forRemoval = true)
    public void setDestinationEdge(final String destinationEdge) throws PrincessException {
        try {
            setDestinationEdge(Integer.parseInt(destinationEdge.trim()));
        } catch (final NumberFormatException e) {
            throw new PrincessException("Invalid destinationEdge value.", e);
        }
    }

    /**
     * Princess's home edge.
     *
     * @return The {@link CardinalEdge} princess will flee to.
     */
    public CardinalEdge getRetreatEdge() {
        return retreatEdge;
    }

    /**
     * Princess's home edge.
     *
     * @param retreatEdge The {@link CardinalEdge} princess should flee to.
     */
    public void setRetreatEdge(final @Nullable CardinalEdge retreatEdge) {
        if (null == retreatEdge) {
            return;
        }

        this.retreatEdge = retreatEdge;
    }

    /**
     * Princess's home edge.
     *
     * @param retreatEdge the index of the {@link CardinalEdge} princess should flee
     *                    to. See {@link CardinalEdge#getIndex()}
     */
    public void setRetreatEdge(final int retreatEdge) {
        setRetreatEdge(CardinalEdge.getCardinalEdge(retreatEdge));
    }

    /**
     * Princess's home edge.
     *
     * @param retreatEdge the index of the {@link CardinalEdge} princess should flee
     *                    to. See {@link CardinalEdge#getIndex()}
     */
    public void setRetreatEdge(final String retreatEdge) throws PrincessException {
        try {
            setRetreatEdge(Integer.parseInt(retreatEdge.trim()));
        } catch (final NumberFormatException e) {
            throw new PrincessException("Invalid retreatEdge value.", e);
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
    public double getHyperAggressionValue(final int index) {
        return HYPER_AGGRESSION_VALUES[validateIndex(index)];
    }

    /**
     * How close to I want to get to my enemies?
     *
     * @param hyperAggressionIndex The index [0-10] of the hyper aggression value to
     *                             be used.
     */
    public void setHyperAggressionIndex(final int hyperAggressionIndex) {
        this.hyperAggressionIndex = validateIndex(hyperAggressionIndex);
    }

    public int getAntiCrowding() {
        return antiCrowding;
    }

    public void setAntiCrowding(int antiCrowding) {
        this.antiCrowding = validateIndex(antiCrowding);
    }

    public void setAntiCrowding(String antiCrowding) throws PrincessException {
        try {
            this.antiCrowding = validateIndex(Integer.parseInt(antiCrowding));
        } catch (final NumberFormatException e) {
            this.antiCrowding = 0;
            throw new PrincessException(e);
        }
    }

    public int getNumberOfEnemiesToConsiderFacing() {
        return numberOfEnemiesToConsiderFacing;
    }

    public void setNumberOfEnemiesToConsiderFacing(String numberOfEnemiesToConsiderFacing) throws PrincessException {
        try {
            setNumberOfEnemiesToConsiderFacing(Integer.parseInt(numberOfEnemiesToConsiderFacing));
        } catch (final NumberFormatException e) {
            throw new PrincessException(e);
        }
    }

    public void setNumberOfEnemiesToConsiderFacing(int numberOfEnemiesToConsiderFacing) {
        this.numberOfEnemiesToConsiderFacing = MathUtility.clamp(numberOfEnemiesToConsiderFacing,
              MIN_NUMBER_OF_ENEMIES_TO_CONSIDER_FACING,
              MAX_NUMBER_OF_ENEMIES_TO_CONSIDER_FACING);
    }

    public int getAllowFacingTolerance() {
        return allowFacingTolerance;
    }

    public void setAllowFacingTolerance(int allowFacingTolerance) {
        this.allowFacingTolerance = MathUtility.clamp(
              allowFacingTolerance, MIN_ALLOW_FACING_TOLERANCE, MAX_ALLOW_FACING_TOLERANCE);
    }

    public void setAllowFacingTolerance(String allowFacingTolerance) throws PrincessException {
        try {
            setAllowFacingTolerance(Integer.parseInt(allowFacingTolerance));
        } catch (final NumberFormatException ex) {
            throw new PrincessException(ex);
        }
    }

    public int getFavorHigherTMM() {
        return favorHigherTMM;
    }

    public void setFavorHigherTMM(int favorHigherTMM) {
        this.favorHigherTMM = validateIndex(favorHigherTMM);
    }

    public void setFavorHigherTMM(String favorHigherTMM) throws PrincessException {
        try {
            this.favorHigherTMM = validateIndex(Integer.parseInt(favorHigherTMM));
        } catch (final NumberFormatException e) {
            throw new PrincessException(e);
        }
    }

    /**
     * How close to I want to get to my enemies?
     *
     * @param index The index ["0"-"10"] of the hyper aggression value to be used.
     */
    public void setHyperAggressionIndex(final String index) throws PrincessException {
        try {
            setHyperAggressionIndex(Integer.parseInt(index));
        } catch (final NumberFormatException ex) {
            throw new PrincessException(ex);
        }
    }

    /**
     * How worried about enemy damage am I?
     *
     * @return Index of the current self-preservation value.
     */
    public int getSelfPreservationIndex() {
        return selfPreservationIndex;
    }

    /**
     * How worried about enemy damage am I?
     *
     * @return The current self-preservation value.
     */
    public double getSelfPreservationValue() {
        return getSelfPreservationValue(selfPreservationIndex);
    }

    /**
     * How worried about enemy damage am I?
     *
     * @param index The index [0-10] of the self-preservation value desired.
     * @return The self-preservation value at the specified index.
     */
    public double getSelfPreservationValue(int index) {
        return SELF_PRESERVATION_VALUES[validateIndex(index)];
    }

    /**
     * How worried about enemy damage am I?
     *
     * @param selfPreservationIndex The index [0-10] of the self-preservation value
     *                              to be used.
     */
    public void setSelfPreservationIndex(final int selfPreservationIndex) {
        this.selfPreservationIndex = validateIndex(selfPreservationIndex);
    }

    /**
     * How worried about enemy damage am I?
     *
     * @param index The index ["0"-"10"] of the self-preservation value to be used.
     */
    public void setSelfPreservationIndex(final String index) throws PrincessException {
        try {
            setSelfPreservationIndex(Integer.parseInt(index));
        } catch (final NumberFormatException ex) {
            throw new PrincessException(ex);
        }
    }

    /**
     * Sets up the behavior parameters based on the passed in XML.
     *
     * @param behavior The XML element containing the behavior parameters.
     * @return TRUE if the XML was successfully parsed.
     */
    @SuppressWarnings("UnusedReturnValue")
    boolean fromXml(final Element behavior) throws PrincessException {
        final NodeList children = behavior.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if ("name".equalsIgnoreCase(child.getNodeName())) {
                setDescription(child.getTextContent());
            } else if ("forcedWithdrawal".equalsIgnoreCase(child.getNodeName())) {
                setForcedWithdrawal(child.getTextContent());
            } else if ("autoFlee".equalsIgnoreCase(child.getNodeName())) {
                setAutoFlee("true".equalsIgnoreCase(child.getTextContent()));
            } else if ("fallShameIndex".equalsIgnoreCase(child.getNodeName())) {
                setFallShameIndex(child.getTextContent());
            } else if ("hyperAggressionIndex".equalsIgnoreCase(child.getNodeName())) {
                setHyperAggressionIndex(child.getTextContent());
            } else if ("selfPreservationIndex".equalsIgnoreCase(child.getNodeName())) {
                setSelfPreservationIndex(child.getTextContent());
            } else if ("destinationEdge".equalsIgnoreCase(child.getNodeName())) {
                setDestinationEdge(CardinalEdge.parseFromString(child.getTextContent()));
            } else if ("retreatEdge".equalsIgnoreCase(child.getNodeName())) {
                setRetreatEdge(CardinalEdge.parseFromString(child.getTextContent()));
            } else if ("herdMentalityIndex".equalsIgnoreCase(child.getNodeName())) {
                setHerdMentalityIndex(child.getTextContent());
            } else if ("braveryIndex".equalsIgnoreCase(child.getNodeName())) {
                setBraveryIndex(child.getTextContent());
            } else if ("antiCrowding".equalsIgnoreCase(child.getNodeName())) {
                setAntiCrowding(child.getTextContent());
            } else if ("favorHigherTMM".equalsIgnoreCase(child.getNodeName())) {
                setFavorHigherTMM(child.getTextContent());
            } else if ("numberOfEnemiesToConsiderFacing".equalsIgnoreCase(child.getNodeName())) {
                setNumberOfEnemiesToConsiderFacing(child.getTextContent());
            } else if ("allowFacingTolerance".equalsIgnoreCase(child.getNodeName())) {
                setAllowFacingTolerance(child.getTextContent());
            } else if ("exclusiveHerding".equalsIgnoreCase(child.getNodeName())) {
                setExclusiveHerding(child.getTextContent());
            } else if ("iAmAPirate".equalsIgnoreCase(child.getNodeName())) {
                setIAmAPirate(child.getTextContent());
            } else if ("experimental".equalsIgnoreCase(child.getNodeName())) {
                setExperimental(child.getTextContent());
            } else if ("strategicTargets".equalsIgnoreCase(child.getNodeName())) {
                final NodeList targets = child.getChildNodes();
                for (int j = 0; j < targets.getLength(); j++) {
                    final Node t = targets.item(j);
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
     * @return An XML {@link org.w3c.dom.Element} describing this behavior settings
     *         object.
     */
    Element toXml(final Document doc,
            final boolean includeTargets) {
        try {
            final Element behavior = doc.createElement("behavior");

            final Element nameNode = doc.createElement("name");
            nameNode.setTextContent(MMXMLUtility.escape(getDescription()));
            behavior.appendChild(nameNode);

            final Element destinationEdgeNode = doc.createElement("destinationEdge");
            destinationEdgeNode.setTextContent(getDestinationEdge().toString());
            behavior.appendChild(destinationEdgeNode);

            final Element retreatEdgeNode = doc.createElement("retreatEdge");
            retreatEdgeNode.setTextContent(getRetreatEdge().toString());
            behavior.appendChild(retreatEdgeNode);

            final Element forcedWithdrawalNode = doc.createElement("forcedWithdrawal");
            forcedWithdrawalNode.setTextContent("" + isForcedWithdrawal());
            behavior.appendChild(forcedWithdrawalNode);

            // Unnecessary ============ to delete
            final Element goHomeNode = doc.createElement("goHome");
            goHomeNode.setTextContent("" + shouldGoHome());
            behavior.appendChild(goHomeNode);
            // ========================

            final Element autoFleeNode = doc.createElement("autoFlee");
            autoFleeNode.setTextContent("" + shouldAutoFlee());
            behavior.appendChild(autoFleeNode);

            final Element fallShameNode = doc.createElement("fallShameIndex");
            fallShameNode.setTextContent("" + getFallShameIndex());
            behavior.appendChild(fallShameNode);

            final Element hyperAggressionNode = doc.createElement("hyperAggressionIndex");
            hyperAggressionNode.setTextContent("" + getHyperAggressionIndex());
            behavior.appendChild(hyperAggressionNode);

            final Element selfPreservationNode = doc.createElement("selfPreservationIndex");
            selfPreservationNode.setTextContent("" + getSelfPreservationIndex());
            behavior.appendChild(selfPreservationNode);

            final Element herdMentalityNode = doc.createElement("herdMentalityIndex");
            herdMentalityNode.setTextContent("" + getHerdMentalityIndex());
            behavior.appendChild(herdMentalityNode);

            final Element braveryNode = doc.createElement("braveryIndex");
            braveryNode.setTextContent("" + getBraveryIndex());
            behavior.appendChild(braveryNode);

            final Element antiCrowdingNode = doc.createElement("antiCrowding");
            antiCrowdingNode.setTextContent("" + getAntiCrowding());
            behavior.appendChild(antiCrowdingNode);

            final Element favorHigherTMMNode = doc.createElement("favorHigherTMM");
            favorHigherTMMNode.setTextContent("" + getFavorHigherTMM());
            behavior.appendChild(favorHigherTMMNode);

            final Element numberOfEnemiesToConsiderFacingNode = doc.createElement("numberOfEnemiesToConsiderFacing");
            numberOfEnemiesToConsiderFacingNode.setTextContent("" + getNumberOfEnemiesToConsiderFacing());
            behavior.appendChild(numberOfEnemiesToConsiderFacingNode);

            final Element allowFacingToleranceNode = doc.createElement("allowFacingTolerance");
            allowFacingToleranceNode.setTextContent("" + getAllowFacingTolerance());
            behavior.appendChild(allowFacingToleranceNode);

            final Element iAmAPirateNode = doc.createElement("iAmAPirate");
            iAmAPirateNode.setTextContent("" + iAmAPirate());
            behavior.appendChild(iAmAPirateNode);

            final Element exclusiveHerdingNode = doc.createElement("exclusiveHerding");
            exclusiveHerdingNode.setTextContent("" + isExclusiveHerding());
            behavior.appendChild(exclusiveHerdingNode);

            final Element experimentalNode = doc.createElement("experimental");
            experimentalNode.setTextContent("" + isExperimental());
            behavior.appendChild(experimentalNode);

            final Element targetsNode = doc.createElement("strategicBuildingTargets");
            if (includeTargets) {
                for (final String t : getStrategicBuildingTargets()) {
                    final Element targetElement = doc.createElement("target");
                    targetElement.setTextContent(MMXMLUtility.escape(t));
                    targetsNode.appendChild(targetElement);
                }
                for (final int id : getPriorityUnitTargets()) {
                    final Element unitElement = doc.createElement("unit");
                    unitElement.setTextContent(String.valueOf(id));
                    targetsNode.appendChild(unitElement);
                }
                for (final int id : getIgnoredUnitTargets()) {
                    final Element ignoredUnitElement = doc.createElement("ignoredUnit");
                    ignoredUnitElement.setTextContent(String.valueOf(id));
                    targetsNode.appendChild(ignoredUnitElement);
                }
            }
            behavior.appendChild(targetsNode);

            return behavior;
        } catch (final Exception e) {
            logger.error(e, "Exception Occurred.");
        }

        return null;
    }

    /**
     * @return A string log of these behavior settings.
     */
    public String toLog() {
        final StringBuilder out = new StringBuilder("Princess Behavior: ").append(getDescription());
        out.append("\n\t Destination Edge: ").append(getDestinationEdge());
        out.append("\n\t Retreat Edge: ").append(getRetreatEdge());
        out.append("\n\t Forced Withdrawal: ").append(isForcedWithdrawal());
        out.append("\n\t Flee: ").append(autoFlee);
        out.append("\n\t Self Preservation: ").append(getSelfPreservationIndex()).append(":")
                .append(getSelfPreservationValue(getSelfPreservationIndex()));
        out.append("\n\t Hyper Aggression: ").append(getHyperAggressionIndex()).append(":")
                .append(getHyperAggressionValue(getHyperAggressionIndex()));
        out.append("\n\t Fall Shame: ").append(getFallShameIndex()).append(":")
                .append(getFallShameValue(getFallShameIndex()));
        out.append("\n\t Bravery: ").append(getBraveryIndex()).append(":").append(getBraveryValue(getBraveryIndex()));
        out.append("\n\t AntiCrowding: ").append(getAntiCrowding());
        out.append("\n\t FavorHigherTMM: ").append(getFavorHigherTMM());
        out.append("\n\t NumberOfEnemiesToConsiderFacing: ").append(getNumberOfEnemiesToConsiderFacing());
        out.append("\n\t AllowFacingTolerance: ").append(getAllowFacingTolerance());
        out.append("\n\t Herd Mentality: ").append(getHerdMentalityIndex()).append(":")
                .append(getHerdMentalityValue(getHerdMentalityIndex()));
        out.append("\n\t Exclusive Herding: ").append(isExclusiveHerding());
        out.append("\n\t I am a Pirate: ").append(iAmAPirate());
        out.append("\n\t Experimental: ").append(isExperimental());
        out.append("\n\t Targets:");
        out.append("\n\t\t Priority Coords: ");
        for (final String t : getStrategicBuildingTargets()) {
            out.append("  ").append(t);
        }
        out.append("\n\t\t Priority Units:");
        for (final int id : getPriorityUnitTargets()) {
            out.append("  ").append(id);
        }
        out.append("\n\t\t Ignored Units:");
        for (final int id : getIgnoredUnitTargets()) {
            out.append("  ").append(id);
        }
        return out.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof BehaviorSettings)) {
            return false;
        }

        final BehaviorSettings that = (BehaviorSettings) o;
        if (autoFlee != that.autoFlee) {
            return false;
        } else if (braveryIndex != that.braveryIndex) {
            return false;
        } else if (fallShameIndex != that.fallShameIndex) {
            return false;
        } else if (forcedWithdrawal != that.forcedWithdrawal) {
            return false;
        } else if (herdMentalityIndex != that.herdMentalityIndex) {
            return false;
        } else if (hyperAggressionIndex != that.hyperAggressionIndex) {
            return false;
        } else if (selfPreservationIndex != that.selfPreservationIndex) {
            return false;
        } else if (antiCrowding != that.antiCrowding) {
            return false;
        } else if (favorHigherTMM != that.favorHigherTMM) {
            return false;
        } else if (numberOfEnemiesToConsiderFacing != that.numberOfEnemiesToConsiderFacing) {
            return false;
        } else if (allowFacingTolerance != that.allowFacingTolerance) {
            return false;
        } else if (!description.equals(that.description)) {
            return false;
        } else if (destinationEdge != that.destinationEdge) {
            return false;
        } else if (retreatEdge != that.retreatEdge) {
            return false;
        } else if (!strategicBuildingTargets.equals(that.strategicBuildingTargets)) {
            return false;
        } else if (!priorityUnitTargets.equals(that.priorityUnitTargets)) {
            return false;
        } else if (!ignoredUnitTargets.equals(that.ignoredUnitTargets)) {
            return false;
        } else if (exclusiveHerding != that.exclusiveHerding) {
            return false;
        } else if (iAmAPirate != that.iAmAPirate) {
            return false;
        }
        return experimental == that.experimental;
    }

    @Override
    public int hashCode() {
        int result = description.hashCode();
        result = 31 * result + (forcedWithdrawal ? 1 : 0);
        result = 31 * result + (autoFlee ? 1 : 0);
        result = 31 * result + selfPreservationIndex;
        result = 31 * result + fallShameIndex;
        result = 31 * result + hyperAggressionIndex;
        result = 31 * result + antiCrowding;
        result = 31 * result + favorHigherTMM;
        result = 31 * result + destinationEdge.hashCode();
        result = 31 * result + retreatEdge.hashCode();
        result = 31 * result + strategicBuildingTargets.hashCode();
        result = 31 * result + priorityUnitTargets.hashCode();
        result = 31 * result + ignoredUnitTargets.hashCode();
        result = 31 * result + numberOfEnemiesToConsiderFacing;
        result = 31 * result + allowFacingTolerance;
        result = 31 * result + herdMentalityIndex;
        result = 31 * result + braveryIndex;
        result = 31 * result + (exclusiveHerding ? 1 : 0);
        result = 31 * result + (iAmAPirate ? 1 : 0);
        result = 31 * result + (experimental ? 1 : 0);
        return result;
    }
}
