/*
 * Copyright (c) 2004 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2004-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.common.board.Coords;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.rolls.TargetRoll;

/**
 * ArtilleryTracker - one held by every entity, it holds a list of the artillery weapons an entity controls, and the
 * mods they get to hit certain hexes.
 */
public class ArtilleryTracker implements Serializable {
    @Serial
    private static final long serialVersionUID = -6913144265531983734L;

    /**
     * Maps WeaponID's of artillery weapons to a Vector of ArtilleryModifiers, for all the different coords it's got
     * mods to.
     */
    private final Map<Mounted<?>, Vector<ArtilleryModifier>> weapons;

    private boolean spotterIsForwardObs;
    private boolean spotterHasCommImplant;

    /**
     * Creates new instance of the tracker
     */
    public ArtilleryTracker() {
        weapons = new HashMap<>();
    }

    /**
     * Adds new weapon
     *
     * @param mounted new weapon
     */
    public void addWeapon(Mounted<?> mounted) {
        weapons.put(mounted, new Vector<>());
    }

    /**
     * Removes a weapon - needed when capital missile bays change modes
     *
     * @param mounted existing weapon
     */
    public void removeWeapon(Mounted<?> mounted) {
        weapons.remove(mounted);
    }

    /**
     * @return the size of the weapons hashtable
     */
    public int getSize() {
        return weapons.size();
    }

    /**
     * Remove all auto hit mods from hexes that were hit previously; used when artillery unit moves. This _should_ be
     * thread-safe. Only auto hit mods are lost when an artillery unit spends MPs (TO:AR pg. 150)
     */
    public void clearHitHexMods() {
        for (Vector<ArtilleryModifier> modVector : weapons.values()) {
            List<ArtilleryModifier> elementsToBeRemoved = new ArrayList<>();
            for (ArtilleryModifier mod : modVector) {
                if (mod.getModifier() == TargetRoll.AUTOMATIC_SUCCESS) {
                    elementsToBeRemoved.add(mod);
                }
            }
            modVector.removeAll(elementsToBeRemoved);
        }
    }

    public boolean weaponInList(Mounted<?> mounted) {
        return (weapons.containsKey(mounted));
    }

    public boolean ammoTypeInList(AmmoTypeEnum ammoType) {
        for (Mounted<?> mounted : weapons.keySet()) {
            if (((WeaponType) mounted.getType()).getAmmoType() == ammoType) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the modifier for artillery weapons on this unit. All weapons use the same modifier due to artillery fire
     * adjustment being handled on a per-unit basis.
     *
     */
    public void setModifier(int modifier, Coords coords) {
        for (Mounted<?> weapon : weapons.keySet()) {
            Vector<ArtilleryModifier> weaponMods = getWeaponModifiers(weapon);
            ArtilleryModifier artilleryModifier = getModifierByCoords(weaponMods, coords);
            if (artilleryModifier != null) {
                artilleryModifier.setModifier(modifier);
            } else {
                artilleryModifier = new ArtilleryModifier(coords, modifier);
                weaponMods.addElement(artilleryModifier);
            }
        }
    }

    /**
     * @param weapon weapon to get modifier for
     *
     * @return the modifier for the given weapon
     */
    public int getModifier(Mounted<?> weapon, Coords coords) {
        Vector<ArtilleryModifier> weaponMods = getWeaponModifiers(weapon);
        ArtilleryModifier am = getModifierByCoords(weaponMods, coords);
        return (am == null) ? 0 : am.getModifier();
    }

    /**
     * @param mounted weapon to get modifiers for
     *
     * @return the <code>Vector</code> of the modifiers for the given weapon
     */
    public Vector<ArtilleryModifier> getWeaponModifiers(Mounted<?> mounted) {
        return weapons.computeIfAbsent(mounted, k -> new Vector<>());
    }

    /**
     * Search the given vector of modifiers for the modifier which coords equals to the given coords
     *
     * @param modifiers <code>Vector</code> of the modifiers to process
     * @param coords    coordinates of the modifier looked for
     *
     * @return modifier with coords equals to the given on <code>null</code> if not found
     */
    protected ArtilleryModifier getModifierByCoords(Vector<ArtilleryModifier> modifiers, Coords coords) {
        return modifiers.stream().filter(mod -> mod.getCoords().equals(coords)).findFirst().orElse(null);
    }

    public boolean getSpotterHasForwardObs() {
        return spotterIsForwardObs;
    }

    public void setSpotterHasForwardObs(boolean forwardObserver) {
        spotterIsForwardObs = forwardObserver;
    }

    public boolean getSpotterHasCommImplant() {
        return spotterHasCommImplant;
    }

    public void setSpotterHasCommImplant(boolean hasCommImplant) {
        spotterHasCommImplant = hasCommImplant;
    }
}
