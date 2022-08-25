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

import java.io.Serializable;

import static megamek.common.alphaStrike.BattleForceSUA.*;

/**
 * This class holds the AlphaStrike information for one arc of a multi-arc unit such as a
 * Dropship or Warship as well as for a turret of a ground unit.
 * It includes an ASSpecialAbilityCollection that includes everything that can occur in an arc
 * or turret such as PNT#, IF#, AC#/#/#/#, TSEMP etc. as well as the standard damage (which is stored
 * as a special ability STD) and the MSL, SCAP and CAP damages, also stored as special abilities.
 * 
 * @author Simon (Juliez)
 */
public class ASArcSummary implements Serializable {

    /** When true, this doesn't use the CAP, SCAP and MSL damages. */
    private final boolean isTurret;

    /** Contains all Special Abilities such as PNT, TELE or LRM of this arc or turret. */
    private ASSpecialAbilityCollection arcSpecials;

    //-------------- Methods

    public static ASArcSummary createTurretSummary(AlphaStrikeElement element) {
        return new ASArcSummary(true, element);
    }

    public static ASArcSummary createArcSummary(AlphaStrikeElement element) {
        return new ASArcSummary(false, element);
    }

    /** Returns the Special Unit Abilities of this arc or turret. */
    public ASSpecialAbilityCollection getSpecials() {
        return arcSpecials;
    }

    private ASArcSummary(boolean forTurret, AlphaStrikeElement element) {
        arcSpecials = new ASSpecialAbilityCollection(element);
        isTurret = forTurret;
    }

    /** @return The standard damage of this turret or the STD damage of this arc. */
    public ASDamageVector getStdDamage() {
        return hasSUA(STD) ? (ASDamageVector) getSUA(STD) : ASDamageVector.ZERO;
    }

    /** @return The capital weapon (CAP) damage of this arc. */
    public ASDamageVector getCAPDamage() {
        return hasSUA(CAP) ? (ASDamageVector) getSUA(CAP) : ASDamageVector.ZERO;
    }

    /** @return The sub-capital weapon (SCAP) damage of this arc. */
    public ASDamageVector getSCAPDamage() {
        return hasSUA(SCAP) ? (ASDamageVector) getSUA(SCAP) : ASDamageVector.ZERO;
    }

    /** @return The capital missile weapon (MSL) damage of this arc. */
    public ASDamageVector getMSLDamage() {
        return hasSUA(MSL) ? (ASDamageVector) getSUA(MSL) : ASDamageVector.ZERO;
    }

    public void setStdDamage(ASDamageVector stdDamage) {
        arcSpecials.replaceSPA(STD, stdDamage);
    }

    public void setCAPDamage(ASDamageVector capDamage) {
        arcSpecials.replaceSPA(CAP, capDamage);
    }

    public void setSCAPDamage(ASDamageVector scapDamage) {
        arcSpecials.replaceSPA(SCAP, scapDamage);
    }

    public void setMSLDamage(ASDamageVector mslDamage) {
        arcSpecials.replaceSPA(MSL, mslDamage);
    }

    public boolean isEmpty() {
        return arcSpecials.isEmpty();
    }

    public boolean hasSUA(BattleForceSUA sua) {
        return arcSpecials.hasSPA(sua);
    }

    public Object getSUA(BattleForceSUA sua) {
        return arcSpecials.getSPA(sua);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (isTurret) {
            if (getStdDamage().hasDamage()) {
                result.append(getStdDamage());
            }
            if (!arcSpecials.isEmpty()) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                result.append(arcSpecials.getSpecialsString());
            }
        } else {
            result.append(getStdDamage()).append(", ");
            result.append("CAP").append(getCAPDamage()).append(", ");
            result.append("SCAP").append(getSCAPDamage()).append(", ");
            result.append("MSL").append(getMSLDamage());
            if (!arcSpecials.isEmpty()) {
                result.append(", ").append(arcSpecials.getSpecialsString());
            }
        }
        return result.toString();
    }

}