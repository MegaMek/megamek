/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.weapons.autocannons;

/**
 * @author Andrew Hunter
 * @since Sep 25, 2004
 */
public class ISAC10 extends ACWeapon {
	private static final long serialVersionUID = 814114264108820161L;

	public ISAC10() {
		super();
		name = "AC/10";
		setInternalName("Autocannon/10");
		addLookupName("IS Auto Cannon/10");
		addLookupName("Auto Cannon/10");
		addLookupName("AutoCannon/10");
		addLookupName("AC/10");
		addLookupName("ISAC10");
		addLookupName("IS Autocannon/10");
		heat = 3;
		damage = 10;
		rackSize = 10;
		shortRange = 5;
		mediumRange = 10;
		longRange = 15;
		extremeRange = 20;
		tonnage = 12.0;
		criticals = 7;
		bv = 123;
		cost = 200000;
		shortAV = 10;
		medAV = 10;
		maxRange = RANGE_MED;
		explosionDamage = damage;
		rulesRefs = "208,TM";
		techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_D, RATING_D, RATING_D)
		        .setISAdvancement(2443, 2460, 2465, DATE_NONE, DATE_NONE)
		        .setISApproximate(false, false, false, false, false)
		        .setClanAdvancement(2443, 2460, 2465, 2850, DATE_NONE)
		        .setClanApproximate(false, false, false, true, false).setPrototypeFactions(F_TH)
		        .setProductionFactions(F_TH);
	}
}
