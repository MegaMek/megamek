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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Configuration;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;

/**
 * Generates a random assignment table (RAT) dynamically based on a variety of criteria,
 * including faction, era, unit type, weight class, equipment rating, faction subcommand, vehicle
 * movement mode, and mission role.
 * 
 * @author Neoancient
 *
 */
public class RATGenerator {
	
	private final static String DATA_DIR = "RAT Generator";
	
	private HashMap<String, ModelRecord> models = new HashMap<String,ModelRecord>();
	private HashMap<String, ChassisRecord> chassis = new HashMap<String,ChassisRecord>();
	private HashMap<String, FactionRecord> factions = new HashMap<String,FactionRecord>();
	private HashMap<Integer, HashMap<String, HashMap<String, AvailabilityRating>>> modelIndex =
			new HashMap<>();
	private HashMap<Integer, HashMap<String, HashMap<String, AvailabilityRating>>> chassisIndex =
			new HashMap<>();

	private TreeSet<Integer> eraSet = new TreeSet<Integer>();

	private static RATGenerator rg = null;

	public static RATGenerator getInstance() {
		if (rg == null) {
			rg = new RATGenerator();
			rg.initialize();
		}
		return rg;
	}

	public AvailabilityRating findChassisAvailabilityRecord(int era, String unit, String faction) {
		if (factions.containsKey(faction)) {
			return findChassisAvailabilityRecord(era, unit, factions.get(faction));
		}
		if (chassisIndex.containsKey(era) && chassisIndex.get(era).containsKey(unit)) {
			return chassisIndex.get(era).get(unit).get("General");
		}
		return null;
	}

	public AvailabilityRating findChassisAvailabilityRecord(int era, String unit, FactionRecord fRec) {
		if (fRec == null) {
			return null;
		}
		if (chassisIndex.containsKey(era) && chassisIndex.get(era).containsKey(unit)) {
			if (chassisIndex.get(era).get(unit).containsKey(fRec.getKey())) {
				return chassisIndex.get(era).get(unit).get(fRec.getKey());
			}
			if (fRec.getParentFactions().size() == 1) {
				return findChassisAvailabilityRecord(era, unit, fRec.getParentFactions().get(0));
			} else if (fRec.getParentFactions().size() > 0) {
				ArrayList<AvailabilityRating> list = new ArrayList<>();
				for (String alt : fRec.getParentFactions()) {
					AvailabilityRating ar = findChassisAvailabilityRecord(era, unit, alt);
					if (ar != null) {
						list.add(ar);
					}
				}
				return mergeFactionAvailability(fRec.getKey(), list);
			}
			return chassisIndex.get(era).get(unit).get("General");
		}
		return null;
	}

	public AvailabilityRating findModelAvailabilityRecord(int era, String unit, String faction) {
		if (factions.containsKey(faction)) {
			return findModelAvailabilityRecord(era, unit, factions.get(faction));
		}
		if (modelIndex.containsKey(era) && modelIndex.get(era).containsKey(unit)) {
			return modelIndex.get(era).get(unit).get("General");
		}
		return null;
	}

	public AvailabilityRating findModelAvailabilityRecord(int era, String unit, FactionRecord fRec) {
		if (fRec == null || models.get(unit).factionIsExcluded(fRec)) {
			return null;
		}
		if (modelIndex.containsKey(era) && modelIndex.get(era).containsKey(unit)) {
			if (modelIndex.get(era).get(unit).containsKey(fRec.getKey())) {
				return modelIndex.get(era).get(unit).get(fRec.getKey());
			}
			if (fRec.getParentFactions().size() == 1) {
				return findModelAvailabilityRecord(era, unit, fRec.getParentFactions().get(0));
			} else if (fRec.getParentFactions().size() > 0) {
				ArrayList<AvailabilityRating> list = new ArrayList<>();
				for (String alt : fRec.getParentFactions()) {
					AvailabilityRating ar = findModelAvailabilityRecord(era, unit, alt);
					if (ar != null) {
						list.add(ar);
					}
				}
				return mergeFactionAvailability(fRec.getKey(), list);
			}
			return modelIndex.get(era).get(unit).get("General");
		}
		return null;
	}

	public TreeSet<Integer> getEraSet() {
		return eraSet;
	}
	
	public Collection<ModelRecord> getModelList() {
		return models.values();
	}

	public ModelRecord getModelRecord(String key) {
		return models.get(key);
	}

	public Collection<ChassisRecord> getChassisList() {
		return chassis.values();
	}

	public ChassisRecord getChassisRecord(String key) {
		return chassis.get(key);
	}

	public Collection<FactionRecord> getFactionList() {
		return factions.values();
	}
	
	public FactionRecord getFaction(String key) {
		return factions.get(key);
	}

	public void addFaction(FactionRecord rec) {
		factions.put(rec.getKey(), rec);
	}

	public void removeFaction(FactionRecord rec) {
		factions.remove(rec.getKey());
	}

	public void removeFaction(String key) {
		factions.remove(key);
	}

	public Collection<String> getFactionSet() {
		return factions.keySet();
	}

	/**
	 * Used for a faction with multiple parent factions (e.g. FC == FS + LA) to find the average
	 * availability among the parents. Based on average weight rather than av rating.
	 * 
	 * @param faction The faction code to use for the new AvailabilityRecord
	 * @param list A list of ARs for the various parent factions
	 * @return A new AR with the average availability code from the various factions.
	 */
	private AvailabilityRating mergeFactionAvailability(String faction, List<AvailabilityRating> list) {
		if (list.size() == 0) {
			return null;
		}
		double totalWt = 0;
		int totalAdj = 0;
		for (AvailabilityRating ar : list) {
			totalWt += AvailabilityRating.calcWeight(ar.availability);
			totalAdj += ar.ratingAdjustment;
		}
		AvailabilityRating retVal = list.get(0).makeCopy(faction);
		
		retVal.availability = (int)(AvailabilityRating.calcAvRating(totalWt / list.size()));
		if (totalAdj < 0) {
			retVal.ratingAdjustment = (int)((totalAdj - 1)/ list.size());			
		} else {
			retVal.ratingAdjustment = (int)((totalAdj + 1)/ list.size());
		}
		return retVal;
	}
	
	/**
	 * Determines availability rating between two era points by interpolation.
	 * 
	 * @param year The year for which to find an availability rating.
	 * @param early AvailabilityRecord for the earlier date.
	 * @param late AvailabilityRecord for the later date.
	 * @return generated AvailabilityRecord
	 */
	
	private double interpolateAvRating(double av1, double av2, int year1, int year2, int now) {
		/* If no record is found for the earlier date, the unit is not available to the faction.
		 * If none is found for the later date, use the early one as the final value. 
		 */
		if (av1 == 0) {
			return 0;
		}
		return av1 + (av2 - av1) * (now - year1) / (year2 - year1);
	}
	
	
	public Map<String, Double> generateTable(String fKey, String unitType, int year,
			int rating, Collection<MissionRole> roles, int roleStrictness,
			FactionRecord user) {
		TreeMap<String, Double> retVal = new TreeMap<String, Double>();
		ArrayList<String> omnis = new ArrayList<String>();
		ArrayList<String> sl = new ArrayList<String>();
		ArrayList<String> clan = new ArrayList<String>();
		ArrayList<String> other = new ArrayList<String>();
		double total = 0.0;
		double totalOmni = 0.0;
		double totalSL = 0.0;
		double totalClan = 0.0;
		double totalOther = 0.0;
		
		FactionRecord fRec = factions.get(fKey);
		if (fRec == null) {
			fRec = new FactionRecord();
		}
		
		Integer early = eraSet.floor(year);
		if (early == null) {
			early = eraSet.first();
		}
		Integer late = null;
		if (!eraSet.contains(year)) {
			late = eraSet.ceiling(year);
		}
		
		for (String chassisKey : chassisIndex.get(early).keySet()) {
			ChassisRecord cRec = chassis.get(chassisKey);
			if (cRec == null) {
				System.err.println("Could not locate chassis " + chassisKey);
				continue;
			}
			if (!cRec.getUnitType().equals(unitType)) {
				continue;
			}

			AvailabilityRating ar = findChassisAvailabilityRecord(early,
						cRec.getChassisKey(), fRec);
			if (ar == null) {
				continue;
			}
			double cAv = cRec.calcAvailability(ar, rating, fRec.getRatingLevels().size(), early);
			if (late != null) {
				cAv = interpolateAvRating(cAv,
						cRec.calcAvailability(ar, rating, fRec.getRatingLevels().size(), late),
						Math.max(early, cRec.getIntroYear()), late, year);
			}
			if (cAv > 0) {
				double totalModelWeight = cRec.totalModelWeight(early,
						cRec.isOmni()?user : fRec);
				for (ModelRecord mRec : cRec.getModels()) {
					ar = findModelAvailabilityRecord(early,
							mRec.getKey(), fRec);
					if (ar == null) {
						continue;
					}
					double mAv = mRec.calcAvailability(ar, rating, fRec.getRatingLevels().size(), early);
					if (late != null) {
						mAv = interpolateAvRating(mAv,
								mRec.calcAvailability(ar, rating, fRec.getRatingLevels().size(), late),
								Math.max(early, mRec.getIntroYear()), late, year);
					}
					double adjCAv = MissionRole.adjustAvailabilityByRole(cAv, roles, mRec, year, roleStrictness);
					double mWt = AvailabilityRating.calcWeight(mAv) / totalModelWeight
							* AvailabilityRating.calcWeight(adjCAv);

					if (mWt > 0) {
						retVal.put(mRec.getKey(), mWt);
						total += mWt;
						if (mRec.isOmni()) {
							totalOmni += mWt;
							omnis.add(mRec.getKey());
						}
						if (mRec.isClan()) {
							totalClan += mWt;
							clan.add(mRec.getKey());
						} else if (mRec.isSL()) {
							totalSL += mWt;
							sl.add(mRec.getKey());
						} else {
							totalOther += mWt;
							other.add(mRec.getKey());
						}
					}
				}
			}						
		}

		if (retVal.size() == 0) {
			return retVal;
		}

		if (rating >= 0) {
			Integer pctOmni = null;
			Integer pctNonOmni = null;
			Integer pctSL = null;
			Integer pctClan = null;
			if (unitType.equals("Mek")) {
				pctOmni = user.getPctOmni(early, rating);
				pctClan = user.getPctClan(early, rating);
				pctSL = user.getPctSL(early, rating);
			}
			if (unitType.equals("Aero")) {
				pctOmni = user.getPctOmniAero(early, rating);
				pctClan = user.getPctClanAero(early, rating);
				pctSL = user.getPctSLAero(early, rating);
			}
			if (unitType.equals("Tank") || unitType.equals("VTOL")) {
				pctClan = user.getPctClanVee(early, rating);
				pctSL = user.getPctSLVee(early, rating);
			}
			Integer pctOther = 100;
			if (pctOmni != null) {
				pctNonOmni = 100 - pctOmni;
			}
			if (pctClan != null) {
				pctOther -= pctClan;
			}
			if (pctSL != null) {
				pctOther -= pctSL;
			}
			for (String key : retVal.keySet()) {
				if (pctOmni != null && omnis.contains(key) && totalOmni < total) {
					retVal.put(key, retVal.get(key) * (pctOmni / 100.0) * (total / totalOmni));
				}
				if (pctNonOmni != null && !omnis.contains(key) && totalOmni > 0) {
					retVal.put(key, retVal.get(key) * (pctNonOmni / 100.0) * (total / (total - totalOmni)));						
				}
				if (pctSL != null && sl.contains(key)
						&& totalSL > 0) {
					retVal.put(key, retVal.get(key) * (pctSL / 100.0) * (total / totalSL));
				}
				if (fRec.isClan() && pctClan != null && clan.contains(key)
						&& totalClan > 0) {
					//Clan % for IS is handled through salvage mechanism
					retVal.put(key, retVal.get(key) * (pctClan / 100.0) * (total / totalClan));
				}
				if (pctOther != null && other.contains(key)
						&& totalClan > 0) {
					retVal.put(key, retVal.get(key) * (pctOther / 100.0) * (total / totalOther));
				}
			}
		}
		
		/* Increase weights if necessary to keep smallest from rounding down to zero */
		Double smallest = null;
		Double largest = null;
		for (String key : retVal.keySet()) {
			if (retVal.get(key) > 0
					&&  (smallest == null || retVal.get(key) < smallest)) {
				smallest = retVal.get(key);
			}
			if (retVal.get(key) > 0
					&& (largest == null || retVal.get(key) > largest)) {
				largest = retVal.get(key);
			}
		}
		if ((smallest != null && smallest < 0.5)
				|| (largest != null && largest > 100.0)) {
			double adj = 100.0 / largest;
			for (String key : retVal.keySet()) {
				retVal.put(key, retVal.get(key) * adj);
			}			
		}

		return retVal;
	}

	private void initialize() {
		for (MechSummary ms : MechSummaryCache.getInstance().getAllMechs()) {
			ModelRecord mr = new ModelRecord(ms);

			models.put(mr.getKey(), mr);
			String chassisKey = mr.getChassisKey();
			if (chassis.containsKey(chassisKey)) {
				if (chassis.get(chassisKey).getIntroYear() == 0 ||
						chassis.get(chassisKey).getIntroYear() > ms.getYear()) {
					chassis.get(chassisKey).setIntroYear(ms.getYear());
				}
				chassis.get(chassisKey).addModel(mr);
			} else {
				chassis.put(chassisKey, mr.createChassisRec());
			}
		}		
	}

	public void loadEra(int era) {
		File dir = new File(Configuration.armyTablesDir(), DATA_DIR);
		File file = new File(dir, era + ".xml");
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.err.println("Unable to read RAT generator file for year " + era); //$NON-NLS-1$
		}

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document xmlDoc = null;

		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			xmlDoc = db.parse(fis);
		} catch (Exception ex) {
			System.err.println(ex.getMessage());
		}

		Element element = xmlDoc.getDocumentElement();
		NodeList nl = element.getChildNodes();

		element.normalize();

		for (int x = 0; x < nl.getLength(); x++) {
			Node mainNode = nl.item(x);
			if (mainNode.getNodeName().equalsIgnoreCase("factions")) {
				for (int i = 0; i < mainNode.getChildNodes().getLength(); i++) {
					Node wn = mainNode.getChildNodes().item(i);
					if (wn.getNodeName().equalsIgnoreCase("faction")) {
						if (wn.getAttributes().getNamedItem("key") != null) {
							FactionRecord rec = factions.get(
									wn.getAttributes().getNamedItem("key").getTextContent());
							if (rec == null) {
								rec = FactionRecord.createFromXml(wn, era);
								factions.put(rec.getKey(), rec);
							} else {
								rec.loadEra(wn, era);
							}
						} else {
							System.err.println("Faction key not found in " + file.getPath());
						}
					}
				}
			} else if (mainNode.getNodeName().equalsIgnoreCase("units")) {
				for (int i = 0; i < mainNode.getChildNodes().getLength(); i++) {
					Node wn = mainNode.getChildNodes().item(i);
					if (wn.getNodeName().equalsIgnoreCase("chassis")) {
						String key = wn.getAttributes().getNamedItem("name").getTextContent()
								+ "[" + wn.getAttributes().getNamedItem("unitType").getTextContent()
								+ "]";
						if (Boolean.parseBoolean(wn.getAttributes().getNamedItem("omni").getTextContent())) {
							key += " Omni";
						}
						ChassisRecord cr = chassis.get(key);
						if (cr == null) {
							System.err.println("Could not find chassis " + key);
							continue;
						}
						for (int j = 0; j < wn.getChildNodes().getLength(); j++) {
							Node wn2 = wn.getChildNodes().item(j);
							if (wn2.getNodeName().equalsIgnoreCase("availability")) {
								chassisIndex.get(era).put(cr.getKey(), new HashMap<String, AvailabilityRating>());
								String [] codes = wn2.getTextContent().trim().split(",");
								for (String code : codes) {
									AvailabilityRating ar = new AvailabilityRating(cr.getKey(), era, code);
									cr.getIncludedFactions().add(code.split(":")[0]);
									chassisIndex.get(era).get(cr.getKey()).put(ar.getFactionCode(), ar);
								}
							} else if (wn2.getNodeName().equalsIgnoreCase("model")) {
								ModelRecord mr = models.get((cr.getChassis() + " " + wn2.getAttributes().getNamedItem("name").getTextContent()).trim());
								if (mr == null) {
									System.err.println(cr.getChassis() + " " + wn2.getAttributes().getNamedItem("name").getTextContent() + " not found.");
									continue;
								}
								for (int k = 0; k < wn2.getChildNodes().getLength(); k++) {
									Node wn3 = wn2.getChildNodes().item(k);
									if (wn3.getNodeName().equalsIgnoreCase("roles")) {
										mr.setRoles(wn3.getTextContent().trim());
									} else if (wn3.getNodeName().equalsIgnoreCase("deployedWith")) {
										mr.setRequiredUnits(wn3.getTextContent().trim());            							
									} else if (wn3.getNodeName().equalsIgnoreCase("availability")) {
										modelIndex.get(era).put(mr.getKey(), new HashMap<String, AvailabilityRating>());
										String [] codes = wn3.getTextContent().trim().split(",");
										for (String code : codes) {
											AvailabilityRating ar = new AvailabilityRating(mr.getKey(), era, code);
											mr.getIncludedFactions().add(code.split(":")[0]);
											modelIndex.get(era).get(mr.getKey()).put(ar.getFactionCode(), ar);
										}
									} 
								}
							}
						}
					}
				}
			}
		}
	}
}
