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

/**
 * This class holds the AlphaStrike information for one arc of a multi-arc unit such as a
 * Dropship or Warship as well as for a turret of a ground unit.
 * It includes an ASDamageVector that holds the standard damage of the turret
 * or the STD damage of the arc, three more ASDamageVectors for CAP, SCAP and MSL damages,
 * and an ASSpecialAbilityCollection that includes everything that can occur in an arc
 * or turret such as PNT#, IF#, AC#/#/#/#, TSEMP etc.
 * 
 * @author Simon (Juliez)
 */
public class ASArcSummary implements Serializable {

    /** When true, this doesn't use the CAP, SCAP and MSL damages. */
    private final boolean isTurret;

    /** Represents the standard-type, non-special damage of this arc or turret, including arc STD damage. */
    private ASDamageVector stdDamage = ASDamageVector.ZERO;

    /** Represents the Capital Weapon (CAP) damage of this arc on a multi-arc element. */
    private ASDamageVector CAPDamage = ASDamageVector.ZEROSPECIAL;

    /** Represents the Subcapital Weapon (SCAP) damage of this arc on a multi-arc element. */
    private ASDamageVector SCAPDamage = ASDamageVector.ZEROSPECIAL;

    /** Represents the Capital Missile Weapon (MSL) damage of this arc on a multi-arc element. */
    private ASDamageVector MSLDamage = ASDamageVector.ZEROSPECIAL;

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
        return stdDamage;
    }

    /** @return The capital weapon (CAP) damage of this arc. */
    public ASDamageVector getCAPDamage() {
        return CAPDamage;
    }

    /** @return The sub-capital weapon (SCAP) damage of this arc. */
    public ASDamageVector getSCAPDamage() {
        return SCAPDamage;
    }

    /** @return The capital missile weapon (MSL) damage of this arc. */
    public ASDamageVector getMSLDamage() {
        return MSLDamage;
    }

    public void setStdDamage(ASDamageVector stdDamage) {
        this.stdDamage = stdDamage;
    }

    public void setCAPDamage(ASDamageVector capDamage) {
        this.CAPDamage = capDamage;
    }

    public void setSCAPDamage(ASDamageVector scapDamage) {
        this.SCAPDamage = scapDamage;
    }

    public void setMSLDamage(ASDamageVector mslDamage) {
        this.MSLDamage = mslDamage;
    }

    public boolean isEmpty() {
        return arcSpecials.isEmpty() && !stdDamage.hasDamage() && !SCAPDamage.hasDamage()
                && !CAPDamage.hasDamage() && !MSLDamage.hasDamage();
    }

    public boolean hasSPA(BattleForceSUA spa) {
        return arcSpecials.hasSPA(spa);
    }

    public Object getSPA(BattleForceSUA spa) {
        return arcSpecials.getSPA(spa);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (isTurret) {
            if (stdDamage.hasDamage()) {
                result.append(stdDamage);
            }
            if (!arcSpecials.isEmpty()) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                result.append(arcSpecials.getSpecialsString());
            }
        } else {
            result.append(stdDamage).append(", ");
            result.append("CAP").append(CAPDamage).append(", ");
            result.append("SCAP").append(SCAPDamage).append(", ");
            result.append("MSL").append(MSLDamage);
            if (!arcSpecials.isEmpty()) {
                result.append(", ").append(arcSpecials.getSpecialsString());
            }
        }
        return result.toString();
    }

}