/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.defensivepods;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class CLMPod extends MPodWeapon {
    private static final long serialVersionUID = 1428507917582780048L;

    public CLMPod() {
        super();
        this.name = "M-Pod";
        this.setInternalName("CLMPod");
        this.addLookupName("CLM-Pod");
        rulesRefs = "Unofficial";
        techAdvancement.setTechBase(TechBase.CLAN)
                .setIntroLevel(false)
                .setUnofficial(true)
                .setTechRating(TechRating.C)
                .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
                .setISAdvancement(3060, 3064, 3099, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(Faction.LC)
                .setProductionFactions(Faction.LC);
    }
}
