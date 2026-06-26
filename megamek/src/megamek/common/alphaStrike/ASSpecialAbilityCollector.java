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

package megamek.common.alphaStrike;

import static megamek.common.alphaStrike.BattleForceSUA.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import megamek.common.strategicBattleSystems.BattleForceSUAFormatter;

/**
 * This interface is implemented by classes that represent anything that holds AlphaStrike Special Unit Abilities. This
 * is the AlphaStrike element itself (its central abilities such as MHQ, CK23-D2 or ARM). It is also implemented by the
 * ASSpecialAbilityCollection (and subclasses) that represent the abilities inside the TUR special as well as the
 * abilities and damage to a large aero's arcs. The interface contains various methods to retrieve the abilities in a
 * type-safe and convenient way. The default methods should be correct and not be overridden in implementing classes.
 */
public interface ASSpecialAbilityCollector {

    /**
     * Returns true when the element, turret or arc has the given Special Unit Ability. When it has and the SUA is
     * associated with some value, this value can be assumed to be non-empty or greater than zero. For example, if an
     * element has MHQ, then MHQ >= 1. If it has IF, then the IF damage is at least 0*.
     *
     * @param sua The Special Unit Ability to check
     *
     * @return True when the given Special Unit Ability is present
     */
    boolean hasSUA(BattleForceSUA sua);

    /**
     * Returns true when this element, turret or arc has any of the given Special Unit Abilities. See
     * {@link #hasSUA(BattleForceSUA)}
     *
     * @param sua The Special Unit Ability to check
     *
     * @return True when this AS element has the given Special Unit Ability
     */
    default boolean hasAnySUAOf(BattleForceSUA sua, BattleForceSUA... furtherSUAs) {
        return hasSUA(sua) || Arrays.stream(furtherSUAs).anyMatch(this::hasSUA);
    }

    /**
     * @return The value associated with the given Special Unit Ability. Depending on the given sua, this value can be
     *       null or of different types. Preferably use the type-safe specific methods such as getLRM() instead of this
     *       method.
     */
    Object getSUA(BattleForceSUA sua);

    /**
     * Returns a formatted SUA string for this AS element. The string is formatted in the way SUAs are printed on an AS
     * element's card or summary with a ', ' between SUAs.
     *
     * @return A formatted Special Unit Ability string
     */
    default String getSpecialsDisplayString(BattleForceSUAFormatter element) {
        return getSpecialsDisplayString(", ", element);
    }

    /**
     * Returns a formatted SUA string suitable for gameplay display such as on an AS Card. The given delimiter is
     * inserted between SUAs.
     *
     * @return A formatted Special Unit Ability string
     */
    String getSpecialsDisplayString(String delimiter, BattleForceSUAFormatter element);

    /** @return The IF ability value of this collection or {@link ASDamage#ZERO}, if there is no IF. */
    default ASDamage getIF() {
        return hasSUA(IF) ? (ASDamage) getSUA(IF) : ASDamage.ZERO;
    }

    /** @return The LRM ability value of this collection or {@link ASDamageVector#ZERO_SPECIAL}, if there is no LRM. */
    default ASDamageVector getLRM() {
        return hasSUA(LRM) ? (ASDamageVector) getSUA(LRM) : ASDamageVector.ZERO_SPECIAL;
    }

    /** @return The IATM ability value of this collection or {@link ASDamageVector#ZERO_SPECIAL}, if there is no IATM. */
    default ASDamageVector getIATM() {
        return hasSUA(IATM) ? (ASDamageVector) getSUA(IATM) : ASDamageVector.ZERO_SPECIAL;
    }

    /** @return The REAR ability value of this collection or {@link ASDamageVector#ZERO_SPECIAL}, if there is no REAR. */
    default ASDamageVector getREAR() {
        return hasSUA(REAR) ? (ASDamageVector) getSUA(REAR) : ASDamageVector.ZERO_SPECIAL;
    }

    /** @return The AC ability value of this collection or {@link ASDamageVector#ZERO_SPECIAL}, if there is no AC. */
    default ASDamageVector getAC() {
        return hasSUA(AC) ? (ASDamageVector) getSUA(AC) : ASDamageVector.ZERO_SPECIAL;
    }

    /** @return The SRM ability value of this collection or {@link ASDamageVector#ZERO_SPECIAL}, if there is no SRM. */
    default ASDamageVector getSRM() {
        return hasSUA(SRM) ? (ASDamageVector) getSUA(SRM) : ASDamageVector.ZERO_SPECIAL;
    }

    /** @return The TOR ability value of this collection or {@link ASDamageVector#ZERO_SPECIAL}, if there is no TOR. */
    default ASDamageVector getTOR() {
        return hasSUA(TOR) ? (ASDamageVector) getSUA(TOR) : ASDamageVector.ZERO_SPECIAL;
    }

    /** @return The HT ability value of this collection or {@link ASDamageVector#ZERO_SPECIAL}, if there is no HT. */
    default ASDamageVector getHT() {
        return hasSUA(HT) ? (ASDamageVector) getSUA(HT) : ASDamageVector.ZERO_SPECIAL;
    }

    /** @return The FLK ability value of this collection or {@link ASDamageVector#ZERO_SPECIAL}, if there is no FLK. */
    default ASDamageVector getFLK() {
        return hasSUA(FLK) ? (ASDamageVector) getSUA(FLK) : ASDamageVector.ZERO_SPECIAL;
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

    /** @return The FUEL ability value of this element or 0 if it doesn't have FUEL. */
    default int getFUEL() {
        return hasSUA(FUEL) ? (Integer) getSUA(FUEL) : 0;
    }

    /** @return The standard damage to this element or turret or the STD damage to this arc. */
    default ASDamageVector getStdDamage() {
        return hasSUA(STD) ? (ASDamageVector) getSUA(STD) : ASDamageVector.ZERO_SPECIAL;
    }

    /** @return The capital weapon (CAP) damage to this arc. */
    default ASDamageVector getCAP() {
        return hasSUA(CAP) ? (ASDamageVector) getSUA(CAP) : ASDamageVector.ZERO_SPECIAL;
    }

    /** @return The sub-capital weapon (SCAP) damage to this arc. */
    default ASDamageVector getSCAP() {
        return hasSUA(SCAP) ? (ASDamageVector) getSUA(SCAP) : ASDamageVector.ZERO_SPECIAL;
    }

    /** @return The capital missile weapon (MSL) damage to this arc. */
    default ASDamageVector getMSL() {
        return hasSUA(MSL) ? (ASDamageVector) getSUA(MSL) : ASDamageVector.ZERO_SPECIAL;
    }

    /** @return The CT transport ability value of this element or 0 if it doesn't have CT. */
    default double getCT() {
        if (hasSUA(CT)) {
            Object ctValue = getSUA(CT);
            if (ctValue instanceof Integer) {
                return (Integer) ctValue;
            } else {
                return (Double) ctValue;
            }
        } else {
            return 0;
        }
    }

    /** @return The IT transport ability value of this element or 0 if it doesn't have IT. */
    default double getIT() {
        if (hasSUA(IT)) {
            Object itValue = getSUA(IT);
            if (itValue instanceof Integer) {
                return (Integer) itValue;
            } else {
                return (Double) itValue;
            }
        } else {
            return 0;
        }
    }

    /** @return The CAR ability value of this element or 0 if it doesn't have CAR. */
    default int getCAR() {
        return hasSUA(CAR) ? (Integer) getSUA(CAR) : 0;
    }

}
