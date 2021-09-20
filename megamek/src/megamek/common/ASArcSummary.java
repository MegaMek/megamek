/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import java.util.EnumMap;
import java.util.List;

import static megamek.common.BattleForceSPA.*;

/** 
 * This class holds the AlphaStrike information for one arc of a multi-arc unit such as a
 * Dropship or Warship as well as for a turret of a ground unit.
 * It includes an ASDamageVector that holds the standard damage of the turret
 * or the STD damage of the arc, three more ASDamageVectors for CAP, SCAP and MSL damages,
 * and a Map of specials that includes everything that can occur in an arc
 * or turret such as PNT#, IF#, AC#/#/#/#, TSEMP etc.
 * 
 * @author Simon (Juliez)
 *
 */
public class ASArcSummary {

    /** When true, this doesn't use the CAP, SCAP and MSL damages. */
    private final boolean isTurret;

    /** Represents the standard-type, non-special damage of this arc or turret, including arc STD damage. */
    private ASDamageVector stdDamage = ASDamageVector.createUpRndDmg(0,0,0,0);

    /** Represents the Capital Weapon (CAP) damage of this arc on a multi-arc element. */
    private ASDamageVector CAPDamage = ASDamageVector.createUpRndDmg(0,0,0,0);

    /** Represents the Subcapital Weapon (SCAP) damage of this arc on a multi-arc element. */
    private ASDamageVector SCAPDamage = ASDamageVector.createUpRndDmg(0,0,0,0);

    /** Represents the Capital Missile Weapon (MSL) damage of this arc on a multi-arc element. */
    private ASDamageVector MSLDamage = ASDamageVector.createUpRndDmg(0,0,0,0);

    /** Contains all Special Unit Abilities of this arc or turret. */
    public EnumMap<BattleForceSPA, Object> arcSpecials = new EnumMap<>(BattleForceSPA.class);

    //-------------- Methods

    public static ASArcSummary createTurretSummary() {
        var result = new ASArcSummary(true);
        return result;
    }

    public static ASArcSummary createArcSummary() {
        var result = new ASArcSummary(false);
        return result;
    }

    /** Returns the standard damage of this turret or the STD damage of this arc. */
    public ASDamageVector getStdDamage() {
        return stdDamage;
    }

    /** Returns the Special Unit Abilities of this arc or turret. */
    public EnumMap<BattleForceSPA, Object> getArcSpecials() {
        return arcSpecials;
    }

    private ASArcSummary(boolean forTurret) {
        isTurret = forTurret;
    }

    public void setStdDamage(ASDamageVector stdDamage) {
        this.stdDamage = stdDamage;
    }

    /**
     * Adds a Special Unit Ability that is not associated with any
     * additional information or number, e.g. RCN.
     */
    public void addSPA(BattleForceSPA spa) {
        arcSpecials.put(spa, null);
    }

    /**
     * Adds a Special Unit Ability associated with an integer number such as C3M#. If
     * that SPA is already present, the given number is added to the one already present. If the present
     * number is a Double type value, that Double type is preserved for this spa.
     */
    public void addSPA(BattleForceSPA spa, int number) {
        if (!arcSpecials.containsKey(spa)) {
            arcSpecials.put(spa, number);
        } else {
            if (arcSpecials.get(spa) instanceof Integer) {
                arcSpecials.put(spa, (int) arcSpecials.get(spa) + number);
            } else if (arcSpecials.get(spa) instanceof Double) {
                arcSpecials.put(spa, (double) arcSpecials.get(spa) + number);
            }
        }
    }

    /**
     * Adds a Special Unit Ability associated with a possibly non-integer number such
     * as MHQ2. If that SPA is already present, the given number is added to the one already present.
     * If the previously present number was an Integer, it will be converted to a Double type value.
     */
    public void addSPA(BattleForceSPA spa, double number) {
        if (!arcSpecials.containsKey(spa)) {
            arcSpecials.put(spa, number);
        } else {
            if (arcSpecials.get(spa) instanceof Integer) {
                arcSpecials.put(spa, (int)arcSpecials.get(spa) + number);
            } else if (arcSpecials.get(spa) instanceof Double) {
                arcSpecials.put(spa, (double)arcSpecials.get(spa) + number);
            }
        }
    }

    /**
     * Replaces the value associated with a Special Unit Ability with the given Object.
     * The previously present associated Object, if any, is discarded. If the ability was not present,
     * it is added.
     */
    public void replaceSPA(BattleForceSPA spa, Object newValue) {
        arcSpecials.put(spa, newValue);
    }

    /**
     * Adds a Special Unit Ability associated with a single AlphaStrike damage value such as IF2. If
     * that SPA is already present, the new damage value replaces the former.
     */
    public void addSPA(BattleForceSPA spa, ASDamage damage) {
        arcSpecials.put(spa, damage);
    }

    /**
     * Adds a Special Unit Ability associated with a full damage vector such as LRM1/2/2. If
     * that SPA is already present, the new damage value replaces the former.
     */
    public void addSPA(BattleForceSPA spa, ASDamageVector damage) {
        arcSpecials.put(spa, damage);
    }

    /**
     * Adds the TUR Special Unit Ability with a List<ASArcSummary>. This list represents multiple
     * turrets; if only one turret is shown as in most units the list has only one entry.
     */
    public void addTurSPA(List<ASArcSummary> turAbility) {
        arcSpecials.put(TUR, turAbility);
    }

    public boolean hasSPA(BattleForceSPA spa) {
        return arcSpecials.containsKey(spa);
    }

    public boolean isEmpty() {
        return arcSpecials.isEmpty() && !stdDamage.hasDamage() && !SCAPDamage.hasDamage()
                && !CAPDamage.hasDamage() && !MSLDamage.hasDamage();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (!isTurret || stdDamage.hasDamage()) {
            result.append(stdDamage);
        }
        for (BattleForceSPA spa : arcSpecials.keySet()) {
            result.append(",").append(AlphaStrikeElement.formatSPAString(spa, arcSpecials.get(spa)));
        }
        String res = result.toString();
        if (res.startsWith(",")) {
            res = res.substring(1);
        }
        return res;
    }

    public Object getSPA(BattleForceSPA spa) {
        return arcSpecials.get(spa);
    }


}
