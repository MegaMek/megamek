/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import org.w3c.dom.Node;

/**
 * Stores data about factions used for building RATs, including
 * parent factions, factions to salvage from, and percentages of
 * Omni/SL/Clan tech. Keys are compatible with those used by MekHQ,
 * with various subcommands indicated by a period and an additional
 * key (e.g. DC.SL for Draconis Combine/Sword of Light).
 *
 * @author Neoancient
 * 
 */
public class FactionRecord {
	
	/**
	 * Proportions of omni/Clan/upgraded tech are given for each faction
	 * by the field manual series.  
	 */
	public enum TechCategory {
		OMNI, CLAN, IS_ADVANCED, // Used for Meks, and also used for ASFs or vees if no other value is present.
		OMNI_AERO, CLAN_AERO, IS_ADVANCED_AERO,
		CLAN_VEE, IS_ADVANCED_VEE;
		/* omni vees do not see the widespread use of Meks and ASFs and do not get
		 * a separate percentage value
		 */

		/**
		 * If no value is provided for ASFs or Vees, use the base value.
		 * @return The base category if the desired one has no value, or null if this is the base.
		 */
		TechCategory fallthrough() {
			switch (this) {
			case OMNI_AERO:
				return OMNI;
			case CLAN_AERO:
			case CLAN_VEE:
				return CLAN;
			case IS_ADVANCED_AERO:
			case IS_ADVANCED_VEE:
				return IS_ADVANCED;
			default:
				return null;
			}
		}
	};
	
	private String key;
	private boolean minor;
	private boolean clan;
	private boolean periphery;
	private String name;
	private TreeMap<Integer, String> altNames;
	private ArrayList<DateRange> yearsActive;
	private ArrayList<String> ratingLevels;
	private HashMap<Integer, Integer> pctSalvage;
	// pctTech.get(category).get(era).get(ratingLevel)
	private HashMap<TechCategory, HashMap<Integer, ArrayList<Integer>>> pctTech;
	private HashMap<Integer, HashMap<String, Integer>> salvage;
	/*
	 * FM:Updates gives percentage values for omni, Clan, and SL tech. Later manuals are
	 * less precise, giving omni percentages for Clans and (in FM:3085) upgrade percentage
	 * for IS and Periphary factions. In order to use the values that are available without
	 * either forcing conformity to guesses for later eras or suddenly removing constraints,
	 * we extrapolate some values but provide a margin of conformity that grows as we
	 * get farther from known values. upgradeMargin applies the percentage of units that
	 * are late-SW IS tech. techMargin applies to both Clan and advanced (SL and post-Clan) tech.
	 */
	private HashMap<Integer, Integer> omniMargin;
	private HashMap<Integer, Integer> techMargin;
	private HashMap<Integer, Integer> upgradeMargin;
	
	//weightDistribution.get(era).get(unitType)
	private HashMap<Integer, HashMap<Integer,ArrayList<Integer>>> weightDistribution;
	private ArrayList<String> parentFactions;
	
	public FactionRecord() {
		this("Periphery", "Periphery");
	}
	
	public FactionRecord(String key) {
		this(key, key);
	}
		
	public FactionRecord(String key, String name) {
		this.key = key;
		this.name = name;
		minor = clan = periphery = false;
		ratingLevels = new ArrayList<>();
		altNames = new TreeMap<>();
		yearsActive = new ArrayList<>();
		pctSalvage = new HashMap<>();
		pctTech = new HashMap<>();
		omniMargin = new HashMap<>();
		upgradeMargin = new HashMap<>();
		techMargin = new HashMap<>();
		salvage = new HashMap<>();
		weightDistribution = new HashMap<>();
		parentFactions = new ArrayList<>();
	}
	
	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return other != null && other instanceof FactionRecord
				&& ((FactionRecord)other).getKey().equals(getKey());
	}
	
	public String getKey() {
		return key;
	}
	
	public boolean isMinor() {
		return minor;
	}
	
	public void setMinor(boolean minor) {
		this.minor = minor;
	}
	
	public boolean isClan() {
		return clan;
	}
	
	public void setClan(boolean clan) {
		this.clan = clan;
	}
	
	public boolean isPeriphery() {
		return periphery;
	}
	
	public void setPeriphery(boolean periphery) {
		this.periphery = periphery;
	}
	
	/**
	 * 
	 * @return value of ratingLevels
	 */
	public ArrayList<String> getRatingLevels() {
		return ratingLevels;
	}
	
	/**
	 * Checks the faction parent hierarchy as necessary to find a set of ratings with at
	 * least two values. If ratingLevels is empty, this faction inherits the parent's system.
	 * If ratingLevels has one member, it indicates a set value for this faction within
	 * the parent faction's system.
	 * 
	 * @return The list of available equipment ratings for the faction.
	 */
	
	public ArrayList<String> getRatingLevelSystem() {
		if (ratingLevels.size() < 2) {
			for (String parent : parentFactions) {
				FactionRecord fr = RATGenerator.getInstance().getFaction(parent);
				if (fr != null) {
					ArrayList<String> retVal = fr.getRatingLevelSystem();
					if (retVal.size() > 1 &&
							(ratingLevels.isEmpty() || retVal.contains(ratingLevels.get(0)))) {
						return retVal;
					}
				}
			}
		}
		return ratingLevels;
	}
	
	public String getName() {
		return name;
	}
	
	public String getName(int year) {
		String retVal = name;
		for (Integer y : altNames.keySet()) {
			if (y <= year) {
				retVal = altNames.get(y);
			} else {
				break;
			}
		}
		return retVal;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setName(int era, String name) {
		altNames.put(era, name);
	}
	
	public void setNames(String names) {
		String[] fields = names.split(",");
		name = fields[0];
		altNames.clear();
		for (int i = 1; i < fields.length; i++) {
			String[] entry = fields[i].split(":");
			altNames.put(Integer.parseInt(entry[0]), entry[1]);
		}
	}
	
	public boolean isActiveInYear(int year) {
		for (DateRange dr : yearsActive) {
			if (dr.isInRange(year)) {
				return true;
			}
		}
		return false;
	}
	
	public void setYears(String str) throws ParseException {
		yearsActive.clear();
		String[] ranges = str.split(",");
		int offset = 0;
		try {
			for (String range : ranges) {
				if (range.equals("-")) {
					yearsActive.add(new DateRange(null, null));
				} else if (range.startsWith("-")) {
					yearsActive.add(new DateRange(null, Integer.parseInt(range.substring(1))));
				} else if (range.endsWith("-")) {
					yearsActive.add(new DateRange(Integer.parseInt(range.substring(0, range.length() - 1)), null));
				} else {
					String[] termini = range.split("\\-");
					if (termini.length == 2) {
						yearsActive.add(new DateRange(Integer.parseInt(termini[0]),
								Integer.parseInt(termini[1])));
					}
				}
				offset += range.length();
			}
		} catch (Exception ex) {
			throw new ParseException("Could not parse year ranges for faction " + key, offset);
		}
	}
	
	public Integer getPctSalvage(int era) {
		return pctSalvage.get(era);
	}
	
	public void setPctSalvage(int era, Integer pct) {
		pctSalvage.put(era, pct);
	}
	
	public HashMap<String, Integer> getSalvage(int era) {
		if (salvage.containsKey(era) && salvage.get(era).size() > 0) {
			return salvage.get(era);
		}
		HashMap<String,Integer> retVal = new HashMap<String, Integer>();
		if (retVal.size() == 0 && parentFactions.size() > 0) {
			for (String pKey : parentFactions) {
				FactionRecord fRec = RATGenerator.getInstance().getFaction(pKey);
				if (fRec != null) {
					for (String fKey : fRec.getSalvage(era).keySet()) {
						retVal.merge(fKey, fRec.getSalvage(era).get(fKey), Integer::sum);
					}					
				} else {
					System.err.println("RATGenerator: could not locate salvage faction " + pKey
							+ " for " + key);
				}
			}
		}
		salvage.put(era, retVal);
		return retVal;
	}
	
	public void setSalvage(int era, String faction, Integer wt) {
		salvage.get(era).put(faction, wt);
	}
	
	public void removeSalvage(int era, String faction) {
		salvage.get(era).remove(faction);
	}
	
	public Integer getPctTech(TechCategory category, int era, int rating) {
		if (!pctTech.containsKey(category) || !pctTech.get(category).containsKey(era)
				|| pctTech.get(category).get(era).isEmpty()
				|| pctTech.get(category).get(era).size() <= rating) {
			return null;
		}
		return pctTech.get(category).get(era).get(rating);
	}
	
	public Integer findPctTech(TechCategory category, int era, int rating) {
		Integer retVal = getPctTech(category, era, rating);
		if (retVal != null) {
			return retVal;
		}
		retVal = getPctTech(category.fallthrough(), era, rating);
		if (retVal != null) {
			return retVal;
		}
		int total = 0;
		int count = 0;
		for (String parent : parentFactions) {
			FactionRecord pfr = RATGenerator.getInstance().getFaction(parent);
			if (pfr != null) {
				Integer pct = pfr.findPctTech(category, era, rating);
				if (pct != null) {
					total += pct;
					count++;
				}
			}
		}
		if (count > 0) {
			return (int)((total / count + 0.5));
		} else {
			return null;
		}
	}

	public void setRatings(String str) {
		ratingLevels.clear();
		if (str.length() > 0) {
			String[] fields = str.split(",");
			for (String rating : fields) {
				ratingLevels.add(rating);
			}
		}
	}
	
	public void setPctTech(TechCategory category, int era, String str) {
		if (!pctTech.containsKey(category)) {
			pctTech.put(category, new HashMap<Integer,ArrayList<Integer>>());
		}
		ArrayList<Integer> list = new ArrayList<>();
		if (str != null && str.length() > 0) {
			for (String pct : str.split(",")) {
				try {
					list.add(Integer.parseInt(pct));
				} catch (NumberFormatException ex) {
					System.err.println("While loading faction data for " + key);
					System.err.println(ex.getMessage());
				}
			}
		}
		pctTech.get(category).put(era, list);
	}
	
	public int getOmniMargin(int era) {
		if (omniMargin.containsKey(era)) {
			return omniMargin.get(era);
		}
		return 0;
	}
	
	public int getTechMargin(int era) {
		if (techMargin.containsKey(era)) {
			return techMargin.get(era);
		}
		return 0;
	}
	
	public int getUpgradeMargin(int era) {
		if (upgradeMargin.containsKey(era)) {
			return upgradeMargin.get(era);
		}
		return 0;
	}
	
	public ArrayList<Integer> getWeightDistribution(int era, int unitType) {
		if (weightDistribution.containsKey(era)
				&& weightDistribution.get(era).containsKey(unitType)) {
			return weightDistribution.get(era).get(unitType);
		}
		if (parentFactions.size() > 0) {
			ArrayList<Integer> retVal = new ArrayList<>();
			for (String fKey : parentFactions) {
				FactionRecord fRec = RATGenerator.getInstance().getFaction(fKey);
				if (fRec != null) {
					ArrayList<Integer> wd = fRec.getWeightDistribution(era, unitType);
					if (wd != null) {
						if (retVal.size() == 0) {
							retVal.addAll(wd);
						} else {
							for (int i = 0; i < retVal.size(); i++) {
								retVal.set(i, retVal.get(i) + wd.get(i));
							}
						}
					}
				}
			}
			return retVal;
		}
		return null;
	}
	
	public void setWeightDistribution(int era, int unitType, String dist) {
		if (dist == null && weightDistribution.containsKey(era)) {
			weightDistribution.get(era).remove(unitType);
		}
		ArrayList<Integer> list = new ArrayList<>();
		for (String s : dist.split(",")) {
			list.add(Integer.valueOf(s));
		}
		if (!weightDistribution.containsKey(era)) {
			weightDistribution.put(era, new HashMap<Integer,ArrayList<Integer>>());
		}
		weightDistribution.get(era).put(unitType, list);
	}
	
	public ArrayList<String> getParentFactions() {
		return parentFactions;
	}
	
	public void setParentFactions(String factions) {
		parentFactions.clear();
		for (String faction : factions.split(",")) {
			parentFactions.add(faction);
		}
	}
	
	public static FactionRecord createFromXml(Node node) {
		FactionRecord retVal = new FactionRecord();
		retVal.key = node.getAttributes().getNamedItem("key").getTextContent();
		retVal.name = node.getAttributes().getNamedItem("name").getTextContent();
		if (node.getAttributes().getNamedItem("minor") != null) {
			retVal.minor = Boolean.parseBoolean(node.getAttributes().getNamedItem("minor").getTextContent());
		} else {
			retVal.minor = false;
		}
		if (node.getAttributes().getNamedItem("clan") != null) {
			retVal.clan = Boolean.parseBoolean(node.getAttributes().getNamedItem("clan").getTextContent());
		} else {
			retVal.clan = false;
		}
		if (node.getAttributes().getNamedItem("periphery") != null) {
			retVal.periphery = Boolean.parseBoolean(node.getAttributes().getNamedItem("periphery").getTextContent());
		} else {
			retVal.periphery = false;
		}
		for (int i = 0; i < node.getChildNodes().getLength(); i++) {
			Node wn = node.getChildNodes().item(i);
			if (wn.getNodeName().equalsIgnoreCase("nameChange")) {
				retVal.altNames.put(Integer.parseInt(wn.getAttributes().getNamedItem("year").getTextContent()),
						wn.getTextContent());
			} else if (wn.getNodeName().equalsIgnoreCase("years")) {
				try {
					retVal.setYears(wn.getTextContent());
				} catch (ParseException ex) {
					System.err.println(ex.getMessage());
				}
			} else if (wn.getNodeName().equalsIgnoreCase("ratingLevels")) {
				retVal.setRatings(wn.getTextContent());
			} else if (wn.getNodeName().equalsIgnoreCase("parentFaction")) {
				retVal.setParentFactions(wn.getTextContent());
			}
		}
		return retVal;
	}

	public void loadEra(Node node, int era) {
		for (int i = 0; i < node.getChildNodes().getLength(); i++) {
			Node wn = node.getChildNodes().item(i);
			switch(wn.getNodeName()) {
			case "pctOmni":
				if (wn.getAttributes().getNamedItem("unitType") != null
						&& wn.getAttributes().getNamedItem("unitType").getTextContent().equalsIgnoreCase("Aero")) {
					setPctTech(TechCategory.OMNI_AERO, era, wn.getTextContent());
				} else {
					setPctTech(TechCategory.OMNI, era, wn.getTextContent());
				}
				break;
			case "pctClan":
				if (wn.getAttributes().getNamedItem("unitType") != null
						&& wn.getAttributes().getNamedItem("unitType").getTextContent().equalsIgnoreCase("Aero")) {
					setPctTech(TechCategory.CLAN_AERO, era, wn.getTextContent());
				} else if (wn.getAttributes().getNamedItem("unitType") != null
							&& wn.getAttributes().getNamedItem("unitType").getTextContent().equalsIgnoreCase("Vehicle")) {
					setPctTech(TechCategory.CLAN_VEE, era, wn.getTextContent());
				} else {
					setPctTech(TechCategory.CLAN, era, wn.getTextContent());
				}
				break;
			case "pctSL":
				if (wn.getAttributes().getNamedItem("unitType") != null
						&& wn.getAttributes().getNamedItem("unitType").getTextContent().equalsIgnoreCase("Aero")) {
					setPctTech(TechCategory.IS_ADVANCED_AERO, era, wn.getTextContent());
				} else if (wn.getAttributes().getNamedItem("unitType") != null
							&& wn.getAttributes().getNamedItem("unitType").getTextContent().equalsIgnoreCase("Vehicle")) {
					setPctTech(TechCategory.IS_ADVANCED_VEE, era, wn.getTextContent());
				} else {
					setPctTech(TechCategory.IS_ADVANCED, era, wn.getTextContent());
				}
				break;
			case "omniMargin":
				omniMargin.put(era, Integer.parseInt(wn.getTextContent()));
				break;
			case "techMargin":
				techMargin.put(era, Integer.parseInt(wn.getTextContent()));
				break;
			case "upgradeMargin":
				upgradeMargin.put(era, Integer.parseInt(wn.getTextContent()));
				break;
			case "salvage":
				pctSalvage.put(era,
						Integer.parseInt(wn.getAttributes().getNamedItem("pct").getTextContent()));
				salvage.put(era, new HashMap<String,Integer>());
				String [] fields = wn.getTextContent().trim().split(",");
				for (String field : fields) {
					if (field.length() > 0) {
						String[] subfields = field.split(":");
						if (subfields.length == 2) {
							salvage.get(era).put(subfields[0], Integer.parseInt(subfields[1]));
						}
					}
				}				
				break;
			case "weightDistribution":
				try {
					int unitType = ModelRecord.parseUnitType(wn.getAttributes().getNamedItem("unitType").getTextContent());
					setWeightDistribution(era, unitType, wn.getTextContent());
				} catch (Exception ex) {
					System.err.println("RATGenerator: error parsing weight distributions for " + key
							+ ", " + era);
				}
				break;
			}
		}
	}
	
	public String toString() {
		return key;
	}
	
	private static class DateRange {
		public Integer start = null;
		public Integer end = null;
		
		public DateRange(Integer start, Integer end) {
			this.start = start;
			this.end = end;
		}
		
		public boolean isInRange(int year) {
			return (start == null || start <= year)
					&& (end == null || end >= year);
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (start != null) {
				sb.append(String.valueOf(start));
			}
			sb.append("-");
			if (end != null) {
				sb.append(String.valueOf(end));
			}
			return sb.toString();
		}
	}
}
