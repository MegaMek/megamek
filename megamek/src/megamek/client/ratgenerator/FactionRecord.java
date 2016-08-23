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
	
	private String key;
	private boolean minor;
	private boolean clan;
	private boolean periphery;
	private String name;
	private TreeMap<Integer, String> altNames;
	private ArrayList<DateRange> yearsActive;
	private ArrayList<String> ratingLevels;
	private HashMap<Integer, Integer> pctSalvage;
	private HashMap<Integer, ArrayList<Integer>> pctOmni;
	private HashMap<Integer, ArrayList<Integer>> pctClan;
	private HashMap<Integer, ArrayList<Integer>> pctSL;
	private HashMap<Integer, ArrayList<Integer>> pctOmniAero;
	private HashMap<Integer, ArrayList<Integer>> pctClanAero;
	private HashMap<Integer, ArrayList<Integer>> pctSLAero;
	private HashMap<Integer, ArrayList<Integer>> pctClanVee;
	private HashMap<Integer, ArrayList<Integer>> pctSLVee;
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
		ratingLevels = new ArrayList<String>();
		altNames = new TreeMap<Integer, String>();
		yearsActive = new ArrayList<DateRange>();
		pctSalvage = new HashMap<Integer, Integer>();
		pctOmni = new HashMap<Integer, ArrayList<Integer>>();
		pctClan = new HashMap<Integer, ArrayList<Integer>>();
		pctSL = new HashMap<Integer, ArrayList<Integer>>();
		pctOmniAero = new HashMap<Integer, ArrayList<Integer>>();
		pctClanAero = new HashMap<Integer, ArrayList<Integer>>();
		pctSLAero = new HashMap<Integer, ArrayList<Integer>>();
		pctClanVee = new HashMap<Integer, ArrayList<Integer>>();
		pctSLVee = new HashMap<Integer, ArrayList<Integer>>();
		omniMargin = new HashMap<Integer,Integer>();
		upgradeMargin = new HashMap<Integer,Integer>();
		techMargin = new HashMap<Integer,Integer>();
		salvage = new HashMap<Integer, HashMap<String, Integer>>();
		weightDistribution = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();
		parentFactions = new ArrayList<String>();
		ratingLevels.add("F");
		ratingLevels.add("D");
		ratingLevels.add("C");
		ratingLevels.add("B");
		ratingLevels.add("A");
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
	
	public ArrayList<String> getRatingLevels() {
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
	
	public Integer getPctOmni(int era, int rating) {
		if (!pctOmni.containsKey(era) || pctOmni.get(era).size() == 0) {
			return null;
		}
		if (pctOmni.get(era).size() <= rating) {
			return pctOmni.get(era).get(pctOmni.get(era).size() - 1);
		}
		return pctOmni.get(era).get(rating);
	}
	
	public void setPctOmni(int era, int rating, Integer pct) {
		if (!pctOmni.containsKey(era)) {
			pctOmni.put(era,  new ArrayList<Integer>());
		}
		while (pctOmni.get(era).size() <= rating) {
			pctOmni.get(era).add(0);
		}
		pctOmni.get(era).set(rating, pct);
	}
	
	public Integer getPctClan(int era, int rating) {
		if (!pctClan.containsKey(era) || pctClan.get(era).size() == 0) {
			return null;
		}
		if (pctClan.get(era).size() <= rating) {
			return pctClan.get(era).get(pctClan.get(era).size() - 1);
		}
		return pctClan.get(era).get(rating);
	}
	
	public void setPctClan(int era, int rating, Integer pct) {
		if (!pctClan.containsKey(era)) {
			pctClan.put(era,  new ArrayList<Integer>());
		}
		while (pctClan.get(era).size() <= rating) {
			pctClan.get(era).add(0);
		}
		pctClan.get(era).set(rating, pct);
	}
	
	public Integer getPctSL(int era, int rating) {
		if (!pctSL.containsKey(era) || pctSL.get(era).size() == 0) {
			return null;
		}
		if (pctSL.get(era).size() <= rating) {
			return pctSL.get(era).get(pctSL.get(era).size() - 1);
		}
		return pctSL.get(era).get(rating);
	}
	
	public void setPctSL(int era, int rating, Integer pct) {
		if (!pctSL.containsKey(era)) {
			pctSL.put(era,  new ArrayList<Integer>());
		}
		while (pctSL.get(era).size() <= rating) {
			pctSL.get(era).add(0);
		}
		pctSL.get(era).set(rating, pct);
	}
	
	public Integer getPctOmniAero(int era, int rating) {
		return getPctOmniAero(era, rating, true);
	}
	
	public Integer getPctOmniAero(int era, int rating, boolean fallThrough) {
		if (!pctOmniAero.containsKey(era) || pctOmniAero.get(era).size() == 0) {
			return fallThrough?getPctOmni(era, rating):null;
		}
		if (pctOmniAero.get(era).size() <= rating) {
			return pctOmniAero.get(era).get(pctOmniAero.get(era).size() - 1);
		}
		return pctOmniAero.get(era).get(rating);
	}
	
	public void setPctOmniAero(int era, int rating, Integer pct) {
		if (!pctOmniAero.containsKey(era)) {
			pctOmniAero.put(era,  new ArrayList<Integer>());
		}
		while (pctOmniAero.get(era).size() <= rating) {
			pctOmniAero.get(era).add(0);
		}
		pctOmniAero.get(era).set(rating, pct);
	}
	
	public Integer getPctClanAero(int era, int rating) {
		return getPctClanAero(era, rating, true);
	}
	
	public Integer getPctClanAero(int era, int rating, boolean fallThrough) {
		if (!pctClanAero.containsKey(era) || pctClanAero.get(era).size() == 0) {
			return fallThrough?getPctClan(era, rating):null;
		}
		if (pctClanAero.get(era).size() <= rating) {
			return pctClanAero.get(era).get(pctClanAero.get(era).size() - 1);
		}
		return pctClanAero.get(era).get(rating);
	}
	
	public void setPctClanAero(int era, int rating, Integer pct) {
		if (!pctClanAero.containsKey(era)) {
			pctClanAero.put(era,  new ArrayList<Integer>());
		}
		while (pctClanAero.get(era).size() <= rating) {
			pctClanAero.get(era).add(0);
		}
		pctClanAero.get(era).set(rating, pct);
	}
	
	public Integer getPctSLAero(int era, int rating) {
		if (!pctSLAero.containsKey(era) || pctSLAero.get(era).size() == 0) {
			return null;
		}
		if (pctSLAero.get(era).size() <= rating) {
			return pctSLAero.get(era).get(pctSLAero.get(era).size() - 1);
		}
		return pctSLAero.get(era).get(rating);
	}
	
	public void setPctSLAero(int era, int rating, Integer pct) {
		if (!pctSLAero.containsKey(era)) {
			pctSLAero.put(era,  new ArrayList<Integer>());
		}
		while (pctSLAero.get(era).size() <= rating) {
			pctSLAero.get(era).add(0);
		}
		pctSLAero.get(era).set(rating, pct);
	}
	
	public Integer getPctClanVee(int era, int rating) {
		return getPctClanVee(era, rating, true);
	}
	
	public Integer getPctClanVee(int era, int rating, boolean fallThrough) {
		if (!pctClanVee.containsKey(era) || pctClanVee.get(era).size() == 0) {
			return fallThrough?getPctClan(era, rating):null;
		}
		if (pctClanVee.get(era).size() <= rating) {
			return pctClanVee.get(era).get(pctClanVee.get(era).size() - 1);
		}
		return pctClanVee.get(era).get(rating);
	}
	
	public void setPctClanVee(int era, int rating, Integer pct) {
		if (!pctClanVee.containsKey(era)) {
			pctClanVee.put(era,  new ArrayList<Integer>());
		}
		while (pctClanVee.get(era).size() <= rating) {
			pctClanVee.get(era).add(0);
		}
		pctClanVee.get(era).set(rating, pct);
	}
	
	public Integer getPctSLVee(int era, int rating) {
		if (!pctSLVee.containsKey(era) || pctSLVee.get(era).size() == 0) {
			return null;
		}
		if (pctSLVee.get(era).size() <= rating) {
			return pctSLVee.get(era).get(pctSLVee.get(era).size() - 1);
		}
		return pctSLVee.get(era).get(rating);
	}
	
	public void setPctSLVee(int era, int rating, Integer pct) {
		if (!pctSLVee.containsKey(era)) {
			pctSLVee.put(era,  new ArrayList<Integer>());
		}
		while (pctSLVee.get(era).size() <= rating) {
			pctSLVee.get(era).add(0);
		}
		pctSLVee.get(era).set(rating, pct);
	}
	
	public void setRatings(String str) {
		ratingLevels.clear();
		String[] fields = str.split(",");
		for (String rating : fields) {
			ratingLevels.add(rating);
		}
	}
	
	public void setPctOmni(int era, String str) {
		if (pctOmni.containsKey(era)) {
			pctOmni.get(era).clear();
		} else {
			pctOmni.put(era, new ArrayList<Integer>());
		}
		if (str != null && str.length() > 0) {
			String[] fields = str.split(",");
			for (String pct : fields) {
				if (pct.equals("null")) {
					pctOmni.get(era).add(null);
				} else {
					pctOmni.get(era).add(Integer.parseInt(pct));
				}
			}
		}
	}
	
	public void setPctClan(int era, String str) {
		if (pctClan.containsKey(era)) {
			pctClan.get(era).clear();
		} else {
			pctClan.put(era, new ArrayList<Integer>());
		}
		if (str != null && str.length() > 0) {
			String[] fields = str.split(",");
			for (String pct : fields) {
				if (pct.equals("null")) {
					pctClan.get(era).add(null);
				} else {
					pctClan.get(era).add(Integer.parseInt(pct));
				}
			}
		}
	}
	
	public void setPctSL(int era, String str) {
		if (pctSL.containsKey(era)) {
			pctSL.get(era).clear();
		} else {
			pctSL.put(era, new ArrayList<Integer>());
		}
		if (str != null && str.length() > 0) {
			String[] fields = str.split(",");
			for (String pct : fields) {
				if (pct.equals("null")) {
					pctSL.get(era).add(null);
				} else {
					pctSL.get(era).add(Integer.parseInt(pct));
				}
			}
		}
	}
	
	public void setPctOmniAero(int era, String str) {
		if (pctOmniAero.containsKey(era)) {
			pctOmniAero.get(era).clear();
		} else {
			pctOmniAero.put(era, new ArrayList<Integer>());
		}
		if (str != null && str.length() > 0) {
			String[] fields = str.split(",");
			for (String pct : fields) {
				if (pct.equals("null")) {
					pctOmniAero.get(era).add(null);
				} else {
					pctOmniAero.get(era).add(pct.length() > 0?Integer.parseInt(pct):0);
				}
			}
		}
	}
	
	public void setPctClanAero(int era, String str) {
		if (pctClanAero.containsKey(era)) {
			pctClanAero.get(era).clear();
		} else {
			pctClanAero.put(era, new ArrayList<Integer>());
		}
		if (str != null && str.length() > 0) {
			String[] fields = str.split(",");
			for (String pct : fields) {
				if (pct.equals("null")) {
					pctClanAero.get(era).add(null);
				} else {
					pctClanAero.get(era).add(Integer.parseInt(pct));
				}
			}
		}
	}
	
	public void setPctSLAero(int era, String str) {
		if (pctSLAero.containsKey(era)) {
			pctSLAero.get(era).clear();
		} else {
			pctSLAero.put(era, new ArrayList<Integer>());
		}
		if (str != null && str.length() > 0) {
			String[] fields = str.split(",");
			for (String pct : fields) {
				if (pct.equals("null")) {
					pctSLAero.get(era).add(null);
				} else {
					pctSLAero.get(era).add(Integer.parseInt(pct));
				}
			}
		}
	}
	
	public void setPctClanVee(int era, String str) {
		if (pctClanVee.containsKey(era)) {
			pctClanVee.get(era).clear();
		} else {
			pctClanVee.put(era, new ArrayList<Integer>());
		}
		if (str != null && str.length() > 0) {
			String[] fields = str.split(",");
			for (String pct : fields) {
				if (pct.equals("null")) {
					pctClanVee.get(era).add(null);
				} else {
					pctClanVee.get(era).add(Integer.parseInt(pct));
				}
			}
		}
	}
	
	public void setPctSLVee(int era, String str) {
		if (pctSLVee.containsKey(era)) {
			pctSLVee.get(era).clear();
		} else {
			pctSLVee.put(era, new ArrayList<Integer>());
		}
		if (str != null && str.length() > 0) {
			String[] fields = str.split(",");
			for (String pct : fields) {
				if (pct.equals("null")) {
					pctSLVee.get(era).add(null);
				} else {
					pctSLVee.get(era).add(Integer.parseInt(pct));
				}
			}
		}
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
	
	public static FactionRecord createFromXml(Node node, int era) {
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
		retVal.loadEra(node, era);
		return retVal;
	}
	
	public void loadEra(Node node, int era) {
		for (int i = 0; i < node.getChildNodes().getLength(); i++) {
			Node wn = node.getChildNodes().item(i);
			switch(wn.getNodeName()) {
			case "pctOmni":
				if (wn.getAttributes().getNamedItem("unitType") != null
						&& wn.getAttributes().getNamedItem("unitType").getTextContent().equalsIgnoreCase("Aero")) {
					setPctOmniAero(era, wn.getTextContent());
				} else {
					setPctOmni(era, wn.getTextContent());
				}
				break;
			case "pctClan":
				if (wn.getAttributes().getNamedItem("unitType") != null
						&& wn.getAttributes().getNamedItem("unitType").getTextContent().equalsIgnoreCase("Aero")) {
					setPctClanAero(era, wn.getTextContent());
				} else if (wn.getAttributes().getNamedItem("unitType") != null
							&& wn.getAttributes().getNamedItem("unitType").getTextContent().equalsIgnoreCase("Vehicle")) {
					setPctClanVee(era, wn.getTextContent());
				} else {
					setPctClan(era, wn.getTextContent());
				}
				break;
			case "pctSL":
				if (wn.getAttributes().getNamedItem("unitType") != null
						&& wn.getAttributes().getNamedItem("unitType").getTextContent().equalsIgnoreCase("Aero")) {
					setPctSLAero(era, wn.getTextContent());
				} else if (wn.getAttributes().getNamedItem("unitType") != null
							&& wn.getAttributes().getNamedItem("unitType").getTextContent().equalsIgnoreCase("Vehicle")) {
					setPctSLVee(era, wn.getTextContent());
				} else {
					setPctSL(era, wn.getTextContent());
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
					String[] subfields = field.split(":");
					if (subfields.length == 2) {
						salvage.get(era).put(subfields[0], Integer.parseInt(subfields[1]));
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
