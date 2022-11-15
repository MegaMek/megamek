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
 * abilities inside the TUR special as well as the abilities and damage of a large aero's arcs.
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

    /** @return The IF ability value of this collection or {@link ASDamage#ZERO}, if there is no IF. */
    default ASDamage getIF() {
        return hasSUA(IF) ? (ASDamage) getSUA(IF) : ASDamage.ZERO;
    }

    /** @return The LRM ability value of this collection or {@link ASDamageVector#ZEROSPECIAL}, if there is no LRM. */
    default ASDamageVector getLRM() {
        return hasSUA(LRM) ? (ASDamageVector) getSUA(LRM) : ASDamageVector.ZEROSPECIAL;
    }

    /** @return The IATM ability value of this collection or {@link ASDamageVector#ZEROSPECIAL}, if there is no IATM. */
    default ASDamageVector getIATM() {
        return hasSUA(IATM) ? (ASDamageVector) getSUA(IATM) : ASDamageVector.ZEROSPECIAL;
    }

    /** @return The REAR ability value of this collection or {@link ASDamageVector#ZEROSPECIAL}, if there is no REAR. */
    default ASDamageVector getREAR() {
        return hasSUA(REAR) ? (ASDamageVector) getSUA(REAR) : ASDamageVector.ZEROSPECIAL;
    }

    /** @return The AC ability value of this collection or {@link ASDamageVector#ZEROSPECIAL}, if there is no AC. */
    default ASDamageVector getAC() {
        return hasSUA(AC) ? (ASDamageVector) getSUA(AC) : ASDamageVector.ZEROSPECIAL;
    }

    /** @return The SRM ability value of this collection or {@link ASDamageVector#ZEROSPECIAL}, if there is no SRM. */
    default ASDamageVector getSRM() {
        return hasSUA(SRM) ? (ASDamageVector) getSUA(SRM) : ASDamageVector.ZEROSPECIAL;
    }

    /** @return The TOR ability value of this collection or {@link ASDamageVector#ZEROSPECIAL}, if there is no TOR. */
    default ASDamageVector getTOR() {
        return hasSUA(TOR) ? (ASDamageVector) getSUA(TOR) : ASDamageVector.ZEROSPECIAL;
    }

    /** @return The HT ability value of this collection or {@link ASDamageVector#ZEROSPECIAL}, if there is no HT. */
    default ASDamageVector getHT() {
        return hasSUA(HT) ? (ASDamageVector) getSUA(HT) : ASDamageVector.ZEROSPECIAL;
    }

    /** @return The FLK ability value of this collection or {@link ASDamageVector#ZEROSPECIAL}, if there is no FLK. */
    default ASDamageVector getFLK() {
        return hasSUA(FLK) ? (ASDamageVector) getSUA(FLK) : ASDamageVector.ZEROSPECIAL;
    }

    /** @return The TUR contents of this element (empty if there is no TUR). */
    default ASTurretSummary getTUR() {
        return hasSUA(TUR) ? (ASTurretSummary) getSUA(TUR) : new ASTurretSummary();
    }

    /** @return The Map that contains the element's LAM movement ability (or an empty map). */
    @SuppressWarnings("unchecked")
    default Map<String, Integer> getLAM() {
        return hasSUA(LAM) ? (Map<String, Integer>) getSUA(LAM) : Collections.EMPTY_MAP;
    }

    /** @return The Map that contains the element's BIM movement ability (or an empty map). */
    @SuppressWarnings("unchecked")
    default Map<String, Integer> getBIM() {
        return hasSUA(BIM) ? (Map<String, Integer>) getSUA(BIM) : Collections.EMPTY_MAP;
    }

    /** @return The MHQ ability value of this element or 0 if it doesn't have MHQ. */
    default int getMHQ() {
        return hasSUA(MHQ) ? (Integer) getSUA(MHQ) : 0;
    }

    /** @return The JPMS ability value of this element or 0 if it doesn't have JMPS. */
    default int getJMPS() {
        return hasSUA(JMPS) ? (Integer) getSUA(JMPS) : 0;
    }

    /** @return The SUBS ability value of this element or 0 if it doesn't have SUBS. */
    default int getSUBS() {
        return hasSUA(SUBS) ? (Integer) getSUA(SUBS) : 0;
    }

    /** @return The standard damage of this element or turret or the STD damage of this arc. */
    default ASDamageVector getStdDamage() {
        return hasSUA(STD) ? (ASDamageVector) getSUA(STD) : ASDamageVector.ZEROSPECIAL;
    }

    /** @return The capital weapon (CAP) damage of this arc. */
    default ASDamageVector getCAP() {
        return hasSUA(CAP) ? (ASDamageVector) getSUA(CAP) : ASDamageVector.ZEROSPECIAL;
    }

    /** @return The sub-capital weapon (SCAP) damage of this arc. */
    default ASDamageVector getSCAP() {
        return hasSUA(SCAP) ? (ASDamageVector) getSUA(SCAP) : ASDamageVector.ZEROSPECIAL;
    }

    /** @return The capital missile weapon (MSL) damage of this arc. */
    default ASDamageVector getMSL() {
        return hasSUA(MSL) ? (ASDamageVector) getSUA(MSL) : ASDamageVector.ZEROSPECIAL;
    }

}
