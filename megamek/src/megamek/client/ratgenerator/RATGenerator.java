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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Configuration;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.UnitType;

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
    private static boolean interrupted = false;
    private static boolean dispose = false;
    private Thread loader;
    private boolean initialized;
    private boolean initializing;

    private ArrayList<ActionListener> listeners;
    
    protected RATGenerator() {
    	models = new HashMap<String,ModelRecord>();
    	chassis = new HashMap<String,ChassisRecord>();
    	factions = new HashMap<String,FactionRecord>();
    	modelIndex = new HashMap<>();
    	chassisIndex = new HashMap<>();
    	eraSet = new TreeSet<Integer>();
    	
    	listeners = new ArrayList<ActionListener>();
    }

	public static RATGenerator getInstance() {
		if (rg == null) {
			rg = new RATGenerator();
		}
        if (!rg.initialized && !rg.initializing) {
            rg.initializing = true;
            interrupted = false;
            dispose = false;
            rg.loader = new Thread(new Runnable() {
                public void run() {
                    rg.initialize();
                }
            }, "RAT Generator unit populator");
            rg.loader.setPriority(Thread.NORM_PRIORITY - 1);
            rg.loader.start();
        }
		return rg;
	}

    public boolean isInitialized() {
        return initialized;
    }

	public AvailabilityRating findChassisAvailabilityRecord(int era, String unit, String faction,
			int year) {
		if (factions.containsKey(faction)) {
			return findChassisAvailabilityRecord(era, unit, factions.get(faction), year);
		}
		if (chassisIndex.containsKey(era) && chassisIndex.get(era).containsKey(unit)) {
			AvailabilityRating av = chassisIndex.get(era).get(unit).get("General");
			if (av != null && year >= av.getStartYear()) {
				return av;
			}
		}
		return null;
	}

	public AvailabilityRating findChassisAvailabilityRecord(int era, String unit, FactionRecord fRec,
			int year) {
		if (fRec == null) {
			return null;
		}
		AvailabilityRating retVal = null;
		if (chassisIndex.containsKey(era) && chassisIndex.get(era).containsKey(unit)) {
			if (chassisIndex.get(era).get(unit).containsKey(fRec.getKey())) {
				retVal = chassisIndex.get(era).get(unit).get(fRec.getKey());
			} else if (fRec.getParentFactions().size() == 1) {
				retVal = findChassisAvailabilityRecord(era, unit, fRec.getParentFactions().get(0), year);
			} else if (fRec.getParentFactions().size() > 0) {
				ArrayList<AvailabilityRating> list = new ArrayList<>();
				for (String alt : fRec.getParentFactions()) {
					AvailabilityRating ar = findChassisAvailabilityRecord(era, unit, alt, year);
					if (ar != null) {
						list.add(ar);
					}
				}
				retVal = mergeFactionAvailability(fRec.getKey(), list);
			} else {
				retVal = chassisIndex.get(era).get(unit).get("General");
			}
		}
		if (retVal != null && year >= retVal.getStartYear()) {
			return retVal;
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
	
	public int eraForYear(int year) {
		if (year < eraSet.first()) {
			return eraSet.first();
		}
		return eraSet.floor(year);
	}
	
	public boolean eraIsLoaded(int era) {
		return chassisIndex.containsKey(era);
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
	
	private double interpolateDouble(double av1, double av2, int year1, int year2, int now) {
		if (year1 == year2) {
			return av1;
		}
		return av1 + (av2 - av1) * (now - year1) / (year2 - year1);
	}
	
	public List<UnitTable.TableEntry> generateTable(FactionRecord fRec, int unitType, int year,
			int rating, Collection<Integer> weightClasses, int networkMask, Collection<String> subtypes,
			Collection<MissionRole> roles, int roleStrictness,
			FactionRecord user) {
		HashMap<ModelRecord, Double> unitWeights = new HashMap<ModelRecord, Double>();
		HashMap<FactionRecord, Double> salvageWeights = new HashMap<FactionRecord, Double>();
		
		loadYear(year);
		
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
			
			if (cRec.getUnitType() != unitType &&
					!(unitType == UnitType.TANK
						&& cRec.getUnitType() == UnitType.VTOL
						&& subtypes.contains("VTOL"))) {
				continue;
			}

			AvailabilityRating ar = findChassisAvailabilityRecord(early,
						cRec.getChassisKey(), fRec, year);
			if (ar == null) {
				continue;
			}
			double cAv = cRec.calcAvailability(ar, rating, fRec.getRatingLevels().size(), early);
			if (late != null) {
				cAv = interpolateDouble(cAv,
						cRec.calcAvailability(ar, rating, fRec.getRatingLevels().size(), late),
						Math.max(early, cRec.getIntroYear()), late, year);
			}
			if (cAv > 0) {
				double totalModelWeight = cRec.totalModelWeight(early,
						cRec.isOmni()?user : fRec);
				for (ModelRecord mRec : cRec.getModels()) {
					if (mRec.getIntroYear() > year
							|| (weightClasses.size() > 0
									&& !weightClasses.contains(mRec.getWeightClass()))
							|| (networkMask & mRec.getNetworkMask()) != networkMask) {
						continue;
					}
					if (subtypes.size() > 0 && !subtypes.contains(mRec.getMechSummary().getUnitSubType())) {
						continue;
					}
					ar = findModelAvailabilityRecord(early,
							mRec.getKey(), fRec);
					if (ar == null || ar.getAvailability() == 0) {
						continue;
					}
					double mAv = mRec.calcAvailability(ar, rating, fRec.getRatingLevels().size(), early);
					if (late != null) {
						mAv = interpolateDouble(mAv,
								mRec.calcAvailability(ar, rating, fRec.getRatingLevels().size(), late),
								Math.max(early, mRec.getIntroYear()), late, year);
					}
					Double adjMAv = MissionRole.adjustAvailabilityByRole(mAv, roles, mRec, year, roleStrictness);
					if (adjMAv != null) {
						double mWt = AvailabilityRating.calcWeight(adjMAv) / totalModelWeight
								* AvailabilityRating.calcWeight(cAv);

						if (mWt > 0) {
							unitWeights.put(mRec, mWt);
						}
					}
				}
			}						
		}

		if (unitWeights.size() == 0) {
			return new ArrayList<UnitTable.TableEntry>();
		}
		
		/* If there is more than one weight class and the faction record (or parent)
		 * indicates a certain distribution of weight classes, adjust the weight value
		 * to conform to the given ratio.
		 */

		if (weightClasses.size() > 0) {
			ArrayList<Integer> wcd = fRec.getWeightDistribution(early, unitType);
			if (wcd != null && wcd.size() > 0) {
				double total = unitWeights.values().stream().mapToDouble(Double::doubleValue).sum();
				List<Integer> wcOffsets = weightClasses.stream()
						.map(wc -> wc - ModelRecord.getWeightClassOffset(unitType))
						.collect(Collectors.toList());
				HashMap<Integer,ArrayList<ModelRecord>> wcMap = new HashMap<>();				
				for (ModelRecord mRec : unitWeights.keySet()) {
					Integer key = mRec.getWeightClass() - ModelRecord.getWeightClassOffset(unitType);
					wcMap.computeIfAbsent(key, k -> new ArrayList<ModelRecord>())
						.add(mRec);
				}
				int wdTotal = wcOffsets.stream()
						.mapToInt(wc -> wcd.get(wc)).sum();
				for (int wc : wcMap.keySet()) {
					double ratio = wcMap.get(wc).stream()
							.mapToDouble(mKey -> unitWeights.get(mKey)).sum() / total;
					double adj = wcd.get(wc) / (ratio * wdTotal);
					for (ModelRecord mRec : wcMap.get(wc)) {
						unitWeights.put(mRec, unitWeights.get(mRec) * adj);
					}
				}
			}
		}
		
		double total = 0.0;
		double totalOmni = 0.0;
		double totalClan = 0.0;
		double totalSL = 0.0;
		for (Map.Entry<ModelRecord,Double> entry : unitWeights.entrySet()) {
			if (entry.getKey().isOmni()) {
				totalOmni += entry.getValue();
			}
			if (entry.getKey().isClan()) {
				totalClan += entry.getValue();
			}
			if (entry.getKey().isSL()) {
				totalSL += entry.getValue();
			}
			total += entry.getValue();
		}

		if (fRec.getPctSalvage(early) != null) {
			HashMap<String,Double> salvageEntries = new HashMap<String,Double>();
			for (Map.Entry<String,Integer> entry : fRec.getSalvage(early).entrySet()) {
				if (late == null) {
					salvageEntries.put(entry.getKey(), entry.getValue().doubleValue());
				} else {
					salvageEntries.put(entry.getKey(),
							interpolateDouble(entry.getValue().doubleValue(),
									fRec.getSalvage(late).containsKey(entry.getKey())?
											fRec.getSalvage(late).get(entry.getKey()).doubleValue() : 0,
											early, late, year));
				}
			}
			if (late != null) {
				for (Map.Entry<String,Integer> entry : fRec.getSalvage(late).entrySet()) {
					if (!salvageEntries.containsKey(entry.getKey())) {
						salvageEntries.put(entry.getKey(), interpolateDouble(0,
								entry.getValue().doubleValue(), early, late, year));
					}
				}
			}			
			
			double salvage = fRec.getPctSalvage(early);
			if (salvage >= 100) {
				salvage = total;
				unitWeights.clear();
			} else {
				salvage = salvage * total / (100 - salvage);
			}
			double totalFactionWeight = salvageEntries.values().stream()
					.mapToDouble(Double::doubleValue).sum();
			for (String fKey : salvageEntries.keySet()) {
				FactionRecord salvageFaction = factions.get(fKey);
				if (salvageFaction == null) {
					System.err.println("Could not locate faction " + fKey + " for " + fRec.getKey() + " salvage");
				} else {
					double wt = salvage * salvageEntries.get(fKey) / totalFactionWeight;
					salvageWeights.put(salvageFaction, wt);
				}
			}
		}
		
		if (rating >= 0) {
			Integer pctOmni = null;
			Integer pctNonOmni = null;
			Integer pctSL = null;
			Integer pctClan = null;
			if (unitType == UnitType.MEK) {
				pctOmni = user.getPctOmni(early, rating);
				pctClan = user.getPctClan(early, rating);
				pctSL = user.getPctSL(early, rating);
			}
			if (unitType == UnitType.AERO) {
				pctOmni = user.getPctOmniAero(early, rating);
				pctClan = user.getPctClanAero(early, rating);
				pctSL = user.getPctSLAero(early, rating);
			}
			if (unitType == UnitType.TANK || unitType == UnitType.AERO) {
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
			/* For non-Clan factions, the amount of salvage from Clan factions is
			 * part of the overall Clan percentage.
			 */
			if (!fRec.isClan() && pctClan != null && totalClan > 0) {
				double clanSalvage = salvageWeights.keySet().stream().filter(fr -> fr.isClan())
						.mapToDouble(fr -> salvageWeights.get(fr)).sum();
				total += clanSalvage;
				totalClan += clanSalvage;
				for (FactionRecord fr : salvageWeights.keySet()) {
					if (fr.isClan()) {
						salvageWeights.put(fr, salvageWeights.get(fr)
								* (pctClan / 100.0) * (total / totalClan));
					}
				}
			}
			for (ModelRecord mRec : unitWeights.keySet()) {
				if (pctOmni != null && mRec.isOmni() && totalOmni < total) {
					unitWeights.put(mRec, unitWeights.get(mRec) * (pctOmni / 100.0) * (total / totalOmni));
				}
				if (pctNonOmni != null && !mRec.isOmni() && totalOmni > 0) {
					unitWeights.put(mRec, unitWeights.get(mRec) * (pctNonOmni / 100.0) * (total / (total - totalOmni)));						
				}
				if (pctSL != null && mRec.isSL()
						&& totalSL > 0) {
					unitWeights.put(mRec, unitWeights.get(mRec) * (pctSL / 100.0) * (total / totalSL));
				}
				if (pctClan != null && mRec.isClan()
						&& totalClan > 0) {
					unitWeights.put(mRec, unitWeights.get(mRec) * (pctClan / 100.0) * (total / totalClan));
				}
				if (pctOther != null && !mRec.isClan() && !mRec.isSL()
						&& totalClan > 0) {
					unitWeights.put(mRec, unitWeights.get(mRec) * (pctOther / 100.0)
							* (total / pctOther));
				}
			}
		}
		
		/* Increase weights if necessary to keep smallest from rounding down to zero */
		
		double adj = 1.0;
		DoubleSummaryStatistics stats = Stream.concat(salvageWeights.values().stream(),
				unitWeights.values().stream())
				.mapToDouble(Double::doubleValue)
				.filter(d -> d > 0)
				.summaryStatistics();
		if (stats.getMin() < 0.5) {
			adj = 0.5 / stats.getMin();
			if (stats.getMax() * adj > 1000.0) {
				adj = 1000.0 / (stats.getMax() * adj);
			}
		}
		
		List<UnitTable.TableEntry> retVal = new ArrayList<UnitTable.TableEntry>();
		for (FactionRecord faction : salvageWeights.keySet()) {
			int wt = (int)(salvageWeights.get(faction) * adj + 0.5);
			if (wt > 0) {
				retVal.add(new UnitTable.TableEntry(wt, faction));
			}
		}
		for (ModelRecord mRec : unitWeights.keySet()) {
			int wt = (int)(unitWeights.get(mRec) * adj + 0.5);
			if (wt > 0) {
				retVal.add(new UnitTable.TableEntry(wt, mRec.getMechSummary()));
			}
		}
		return retVal;
	}

    public void dispose() {
        interrupted = true;
        dispose = true;
        if (initialized){
            clear();
        }
    }

    public void clear() {
        rg = null;
        models = null;
        chassis = null;
        factions = null;
        chassisIndex = null;
        modelIndex = null;
        eraSet = null;
        initialized = false;
        initializing = false;
    }

	private synchronized void initialize() {
        // Give the MSC some time to initialize
        MechSummaryCache msc = MechSummaryCache.getInstance();
        long waitLimit = System.currentTimeMillis() + 3000; /* 3 seconds */
        while( !interrupted && !msc.isInitialized() && waitLimit > System.currentTimeMillis() ) {
            try {
                Thread.sleep(50);
            } catch(InterruptedException e) {
                // Ignore
            }
        }
        
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
        
		File dir = new File(Configuration.armyTablesDir(), DATA_DIR);
		for (File f : dir.listFiles()) {
			if (f.getName().matches("\\d+\\.xml")) {
				eraSet.add(Integer.parseInt(f.getName().replace(".xml", "")));
			}
		}

        if (!interrupted) {
            rg.initialized = true;
            rg.notifyListenersOfInitialization();
        }

        if (dispose) {
            clear();
            dispose = false;
        }
	}
	
	/**
	 * If year is equal to one of the era marks, loads that era. If it is between,
	 * loads eras on both sides.
	 */
	public void loadYear(int year) {
		if (eraSet.contains(year)) {
			loadEra(year);
		}
		if (year > eraSet.first()) {
			loadEra(eraSet.floor(year));
		}
		if (year < eraSet.last()) {
			loadEra(eraSet.ceiling(year));
		}
	}
	
	private void loadEra(int era) {
		if (eraIsLoaded(era)) {
			return;
		}
		chassisIndex.put(era, new HashMap<String,HashMap<String,AvailabilityRating>>());
		modelIndex.put(era, new HashMap<String,HashMap<String,AvailabilityRating>>());
		File dir = new File(Configuration.armyTablesDir(), DATA_DIR);
		File file = new File(dir, era + ".xml");
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			System.err.println("Unable to read RAT generator file for era " + era); //$NON-NLS-1$
			return;
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
						if (wn.getAttributes().getNamedItem("omni") != null) {
							if (wn.getAttributes().getNamedItem("omni").getTextContent().equalsIgnoreCase("IS")) {
								key += "ISOmni";
							} else {
								key += "ClanOmni";
							}
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
		notifyListenersEraLoaded();
	}

    public synchronized void registerListener(ActionListener l){
        listeners.add(l);
    }

    public synchronized void removeListener(ActionListener l){
        listeners.remove(l);
    }

    /**
     * Notifies all the listeners that initialization is finished
     */
    public void notifyListenersOfInitialization(){
        if (initialized){
            for (ActionListener l : listeners){
                l.actionPerformed(new ActionEvent(
                        this,ActionEvent.ACTION_PERFORMED,"ratGenInitialized"));
            }
        }
    }

    /**
     * Notifies all the listeners that era is loaded
     */
    public void notifyListenersEraLoaded(){
        if (initialized){
            for (ActionListener l : listeners){
                l.actionPerformed(new ActionEvent(
                        this,ActionEvent.ACTION_PERFORMED,"ratGenEraLoaded"));
            }
        }
    }
}
