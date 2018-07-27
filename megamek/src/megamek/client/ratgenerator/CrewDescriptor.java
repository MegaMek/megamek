/*
 * MegaMek - Copyright (C) 2016 The MegaMek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.ratgenerator;

import java.util.Iterator;

import megamek.client.RandomNameGenerator;
import megamek.common.Compute;
import megamek.common.Crew;
import megamek.common.CrewType;
import megamek.common.UnitType;

/**
 * Description of crew.
 * 
 * @author Neoancient
 *
 */
public class CrewDescriptor {
	public static final int SKILL_WB = 0;
	public static final int SKILL_RG = 1;
	public static final int SKILL_GREEN = 2;
	public static final int SKILL_REGULAR = 3;
	public static final int SKILL_VETERAN = 4;
	public static final int SKILL_ELITE = 5;
	public static final int SKILL_HEROIC = 6;
	public static final int SKILL_LEGENDARY = 7;
//	public static final String[] skillLevelNames = {
//		"Legendary", "Heroic", "Elite", "Veteran", "Regular", "Green", "Really Green", "Wet Behind the Ears"
//	};
	
	private String name;
	private String bloodname;
	private int rank;
	private ForceDescriptor assignment;
	private int gunnery;
	private int piloting;
	private String title;
	
	public CrewDescriptor(ForceDescriptor assignment) {
		this.assignment = assignment;
		name = generateName();
		rank = assignment.getCoRank() == null?0:assignment.getCoRank();
		title = null;
		setSkills();
	}
	
	private String generateName() {
		if (assignment.getFactionRec().isClan()) {
			RandomNameGenerator.getInstance().setChosenFaction("Clan");
			return RandomNameGenerator.getInstance().generate();
		} else if (!assignment.getFaction().contains(".")) {
			// Try to match our faction to one of the rng settings.
			for (Iterator<String> iter = RandomNameGenerator.getInstance().getFactions(); iter.hasNext();) {
				final String f = iter.next();
				if (assignment.getFaction().equalsIgnoreCase(f)) {
					RandomNameGenerator.getInstance().setChosenFaction(f);
					return RandomNameGenerator.getInstance().generate();
				}
			}
		}
		// Go up one parent level and try again
		for (String parent : assignment.getFactionRec().getParentFactions()) {
			for (Iterator<String> iter = RandomNameGenerator.getInstance().getFactions(); iter.hasNext();) {
				final String f = iter.next();
				if (parent.equalsIgnoreCase(f)) {
					RandomNameGenerator.getInstance().setChosenFaction(f);
					return RandomNameGenerator.getInstance().generate();
				}
			}
		}
		//Give up and use general
		RandomNameGenerator.getInstance().setChosenFaction("General");
		return RandomNameGenerator.getInstance().generate();
	}
	
	private void setSkills() {
		boolean clan = RATGenerator.getInstance().getFaction(assignment.getFaction()).isClan();
		int bonus = assignment.getExperience() - 1;
		
		int gExp = randomExperienceLevel(Compute.d6() + bonus);
		int pExp = randomExperienceLevel(Compute.d6() + bonus);
			
		int ratingLevel = assignment.getRatingLevel();
		if (clan) {
			bonus += ratingLevel >= 0?assignment.getRatingLevel() - 1:0;
			if (assignment.getUnitType() != null) {
				switch (assignment.getUnitType()) {
				case UnitType.MEK:
				case UnitType.BATTLE_ARMOR:
					bonus += 2;
					break;
				case UnitType.TANK:
				case UnitType.VTOL:
				case UnitType.NAVAL:
				case UnitType.INFANTRY:
				case UnitType.CONV_FIGHTER:
					bonus--;
					break;
				}
			}
			if (assignment.getRoles().contains(MissionRole.SUPPORT)) {
				bonus--;
			}
		} else {
			if (ratingLevel == 0) {
				bonus--;
			}
			if (ratingLevel >= 5) {
				bonus++;
			}
			if (assignment.getRoles().contains(MissionRole.SUPPORT)) {
				bonus--;
			}
			if (assignment.getFaction().equals("WOB.SD")) {
				bonus++;
			}
		}
		
		gunnery = randomSkillRating(gExp, bonus);
		if (assignment.getUnitType() != null && assignment.getUnitType().equals(UnitType.INFANTRY)
				&& !assignment.getRoles().contains(MissionRole.ANTI_MEK)) {
			piloting = 8;
		} else {
			piloting = randomSkillRating(pExp, bonus);
		}
	}
	
	private int randomExperienceLevel(int roll) {
		final int [] table = {
				SKILL_WB, SKILL_RG, SKILL_GREEN, SKILL_GREEN,
				SKILL_REGULAR, SKILL_REGULAR, SKILL_VETERAN,
				SKILL_VETERAN, SKILL_ELITE, SKILL_LEGENDARY,
				SKILL_HEROIC
		};
		if (roll < 2) {
			return SKILL_WB;
		}
		if (roll > 12) {
			return SKILL_HEROIC;
		}
		return table[roll - 2];
	}
	
	private int randomSkillRating(int baseRating, int mod) {
		final int[] table = {7, 7, 6, 5, 4, 4, 3, 2, 1, 0, 0, 0};
		int roll = Compute.d6(2) + mod;
		if (roll < 1) {
			return table[baseRating];
		}
		if (roll < 3) {
			return table[baseRating + 1];
		}
		if (roll < 5) {
			return table[baseRating + 2];
		}
		if (roll < 7) {
			return table[baseRating + 3];
		}
		return table[baseRating + 4];
	}

	/*
	public void assignBloodname() {
		final int[] ratingMods = {-3, -2, -1, 1, 4};
		int mod = 0;
		if (assignment.getRatingLevel() >= 0) {
			mod = ratingMods[assignment.getRatingLevel()];
		}
		if (assignment.getFaction().equals("BAN")) {
			mod -= 2;
		}

		int type = Bloodname.P_GENERAL;
		if (assignment.isElement()) {
			switch (assignment.getUnitType()) {
			case "Mek":
				type = Bloodname.P_MECHWARRIOR;
				break;
			case "Aero":
				type = Bloodname.P_AEROSPACE;
				break;
			case "Conventional Fighter":
				type = Bloodname.P_AEROSPACE;
				mod -= 2;
				break;
			case "BattleArmor":
				type = Bloodname.P_ELEMENTAL;
				break;
			case "Infantry":
				type = Bloodname.P_ELEMENTAL;
				mod -= 2;
				break;
			case "ProtoMek":
				type = Bloodname.P_PROTOMECH;
				break;
			case "Dropship":
			case "Warship":
				if (assignment.getFaction().startsWith("CSR")) {
					type = Bloodname.P_NAVAL;
				} else {
					mod -= 2;
				}
				break;
			case "Tank":
			case "VTOL":
			case "Jumpship":
				return;
			}
		}
		int roll = Compute.d6(2) + mod -
				getGunnery() -
				getPiloting();
		if (assignment.getYear() <= 2950) roll++;
		if (assignment.getYear() > 3055) roll--;
		if (assignment.getYear() > 3065) roll--;
		if (assignment.getYear() > 3080) roll--;
		if (getRank() >= 30) {
			roll += getRank() - 30;
		}
		if (roll >= 6) {
			setBloodname(Bloodname.randomBloodname(assignment.getFaction().split("\\.")[0],
					type, assignment.getYear()));
		}
	}
	*/
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBloodname() {
		return bloodname;
	}

	public void setBloodname(String bloodname) {
		this.bloodname = bloodname;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		if (rank > this.rank) { 
			this.rank = rank;
		}
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public ForceDescriptor getAssignment() {
		return assignment;
	}

	public void setAssignment(ForceDescriptor assignment) {
		this.assignment = assignment;
	}

	public int getGunnery() {
		return gunnery;
	}

	public void setGunnery(int gunnery) {
		this.gunnery = gunnery;
	}

	public int getPiloting() {
		return piloting;
	}

	public void setPiloting(int piloting) {
		this.piloting = piloting;
	}

	public Crew createCrew(CrewType crewType) {
		return new Crew(crewType, name, 1, gunnery, piloting);
	}
}
