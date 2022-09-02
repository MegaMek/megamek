/*
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
package megamek.common.alphaStrike;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static megamek.common.alphaStrike.BattleForceSUA.*;

/**
 * This interface is implemented by classes that represent anything that holds AlphaStrike Special
 * Unit Abilities. This is the AlphaStrike element itself (its central abilities such as MHQ, CK23-D2 or
 * ARM). It is also implemented by the ASSpecialAbilityCollection (and subclasses) that represent the
 * abilities inside the TUR special as well as the abilities and damage of large aero's arcs.
 * The interface contains various methods to retrieve the abilities in a type-safe and convenient way.
 * The default methods should be correct and not be overridden in implementing classes.
 */
public interface ASSpecialAbilityCollector {

    /**
     * Returns true when the element, turret or arc has the given Special Unit Ability. When it has
     * and the SUA is associated with some value, this value can be assumed to be non-empty
     * or greater than zero. For example, if an element has MHQ, then MHQ >= 1. If it has IF, then
     * the IF damage is at least 0*.
     *
     * @param sua The Special Unit Ability to check
     * @return True when the given Special Unit Ability is present
     */
    boolean hasSUA(BattleForceSUA sua);

    /**
     * Returns true when this element, turret or arc has any of the given Special Unit Abilities.
     * See {@link #hasSUA(BattleForceSUA)}
     *
     * @param sua The Special Unit Ability to check
     * @return True when this AS element has the given Special Unit Ability
     */
    default boolean hasAnySUAOf(BattleForceSUA sua, BattleForceSUA... furtherSuas) {
        return hasSUA(sua) || Arrays.stream(furtherSuas).anyMatch(this::hasSUA);
    }

    /**
     * @return The value associated with the given Special Unit Ability. Depending on the given sua, this
     * value can be null or of different types. Preferably use the type-safe specific methods such as getLRM()
     * instead of this method.
     */
    Object getSUA(BattleForceSUA sua);

    /**
     * Returns a formatted SUA string for this AS element. The string is formatted in the way SUAs are
     * printed on an AS element's card or summary with a ', ' between SUAs.
     *
     * @return A formatted Special Unit Ability string
     */
    default String getSpecialsDisplayString(ASCardDisplayable element) {
        return getSpecialsDisplayString(", ", element);
    }

    /**
     * Returns a formatted SUA string suitable for gameplay display such as on an AS Card.
     * The given delimiter is inserted between SUAs.
     *
     * @return A formatted Special Unit Ability string
     */
    String getSpecialsDisplayString(String delimiter, ASCardDisplayable element);

    /**
     * Convenience method to obtain the element's IF damage.
     *
     * @return The ASDamage that represents the element's IF value. If the element does not
     * have IF, this will return {@link ASDamageVector#ZERO}.
     */
    default ASDamage getIF() {
        return hasSUA(IF) ? (ASDamage) getSUA(IF) : ASDamage.ZERO;
    }

    /**
     * Convenience method to obtain the damage value of the LRM ability. Note that when this is called
     * on an AlphaStrikeElement directly, it will not obtain the LRM value inside a TUR() ability, only
     * the central LRM ability.
     *
     * @return The ASDamageVector that represents the element's LRM ability. If the element does not
     * have LRM, this will return {@link ASDamageVector#ZERO}.
     */
    default ASDamageVector getLRM() {
        return hasSUA(LRM) ? (ASDamageVector) getSUA(LRM) : ASDamageVector.ZERO;
    }

    /**
     * Convenience method to obtain the element's SRM ability. Note that when this is called
     * on an AlphaStrikeElement directly, it will not obtain the SRM value inside a TUR() ability, only
     * the central SRM ability.
     *
     * @return The ASDamageVector that represents the SRM ability. If the element does not
     * have SRM, this will return {@link ASDamageVector#ZERO}.
     */
    default ASDamageVector getSRM() {
        return hasSUA(SRM) ? (ASDamageVector) getSUA(SRM) : ASDamageVector.ZERO;
    }

    /**
     * Convenience method to obtain the element's TOR ability.
     *
     * @return The ASDamageVector that represents the element's TOR ability. If the element does not
     * have TOR, this will return {@link ASDamageVector#ZERO}.
     */
    default ASDamageVector getTOR() {
        return hasSUA(TOR) ? (ASDamageVector) getSUA(TOR) : ASDamageVector.ZERO;
    }

    /**
     * Convenience method to obtain the element's TOR ability.
     *
     * @return The ASDamageVector that represents the element's TOR ability. If the element does not
     * have TOR, this will return {@link ASDamageVector#ZERO}.
     */
    default ASDamageVector getHT() {
        return hasSUA(HT) ? (ASDamageVector) getSUA(HT) : ASDamageVector.ZERO;
    }

    /**
     * Convenience method to obtain the element's FLK ability.
     *
     * @return The ASDamageVector that represents the element's FLK ability. If the element does not
     * have FLK, this will return {@link ASDamageVector#ZERO}.
     */
    default ASDamageVector getFLK() {
        return hasSUA(FLK) ? (ASDamageVector) getSUA(FLK) : ASDamageVector.ZERO;
    }

    /**
     * Convenience method to obtain the element's TUR ability. Note that this method will return null
     * when there is no TUR ability!
     *
     * @return The ASDamageVector that represents the element's TUR ability or null.
     */
    default ASTurretSummary getTUR() {
        return hasSUA(TUR) ? (ASTurretSummary) getSUA(TUR) : new ASTurretSummary();
    }

    /**
     * Convenience method to obtain the element's LAM ability.
     *
     * @return The Map that contains the element's LAM movement ability or an empty map.
     */
    @SuppressWarnings("unchecked")
    default Map<String, Integer> getLAM() {
        return hasSUA(LAM) ? (Map<String, Integer>) getSUA(LAM) : Collections.EMPTY_MAP;
    }

    /** @return The Map that contains the element's LAM movement ability or an empty map. */
    @SuppressWarnings("unchecked")
    default Map<String, Integer> getBIM() {
        return hasSUA(BIM) ? (Map<String, Integer>) getSUA(BIM) : Collections.EMPTY_MAP;
    }

    /**
     * Convenience method to obtain the element's MHQ ability.
     *
     * @return The MHQ value or 0 if there is no MHQ.
     */
    default int getMHQ() {
        return hasSUA(MHQ) ? (Integer) getSUA(MHQ) : 0;
    }

    /** @return The standard damage of this element or turret or the STD damage of this arc. */
    default ASDamageVector getStdDamage() {
        return hasSUA(STD) ? (ASDamageVector) getSUA(STD) : ASDamageVector.ZERO;
    }

    /** @return The capital weapon (CAP) damage of this arc. */
    default ASDamageVector getCAP() {
        return hasSUA(CAP) ? (ASDamageVector) getSUA(CAP) : ASDamageVector.ZERO;
    }

    /** @return The sub-capital weapon (SCAP) damage of this arc. */
    default ASDamageVector getSCAP() {
        return hasSUA(SCAP) ? (ASDamageVector) getSUA(SCAP) : ASDamageVector.ZERO;
    }

    /** @return The capital missile weapon (MSL) damage of this arc. */
    default ASDamageVector getMSL() {
        return hasSUA(MSL) ? (ASDamageVector) getSUA(MSL) : ASDamageVector.ZERO;
    }

}
