/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.srms;

/**
 * @author Sebastian Brocks
 */
public class ISSRM6OS extends SRMWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -1926715836221080572L;

    /**
     *
     */
    public ISSRM6OS() {
        super();
        name = "SRM 6 (OS)";
        setInternalName("ISSRM6OS");
        addLookupName("ISSRM6 (OS)"); // mtf
        addLookupName("IS SRM 6 (OS)"); // tdb
        addLookupName("OS SRM-6"); // mep
        heat = 4;
        rackSize = 6;
        shortRange = 3;
        mediumRange = 6;
        longRange = 9;
        extremeRange = 12;
        tonnage = 3.5;
        criticals = 2;
        bv = 12;
        flags = flags.or(F_NO_FIRES).or(F_ONESHOT).andNot(F_PROTO_WEAPON);
        cost = 40000;
        shortAV = 8;
        maxRange = RANGE_SHORT;
        rulesRefs = "230, TM";
        techAdvancement.setTechBase(TechBase.IS)
        	.setIntroLevel(false)
        	.setUnofficial(false)
            .setTechRating(TechRating.C)
            .setAvailability(TechRating.C, TechRating.C, TechRating.D, TechRating.C)
            //From TM pg 230 - "curious concept that did not so much go extinct in the 
            //Succession Wars as fall into general disuse"
            .setISAdvancement(2665, 2676, 3045, DATE_NONE, 3030)
            .setISApproximate(true, false, false,false, false)
            .setPrototypeFactions(Faction.TH)
            .setProductionFactions(Faction.TH)
            .setReintroductionFactions(Faction.FW);
    }
}
