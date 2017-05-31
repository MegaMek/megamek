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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.UnitType;
import megamek.common.loaders.EntityLoadingException;

/**
 * Describes the characteristics of a force. May be changed during generation.
 * 
 * @author Neoancient
 *
 */
public class ForceDescriptor {
	
	public static final int REINFORCED = 1;
	public static final int UNDERSTRENGTH = -1;
	
	public static final int EXP_GREEN = 0;
	public static final int EXP_REGULAR = 1;
	public static final int EXP_VETERAN = 2;
	
	public static final String[] ORDINALS = {
		"First", "Second", "Third", "Fourth", "Fifth",
		"Sixth", "Seventh", "Eighth", "Ninth", "Tenth"
	};
	
	public static final String[] PHONETIC = {
		"Alpha", "Bravo", "Charlie", "Delta", "Echo",
		"Foxtrot", "Golf", "Hotel", "India", "Juliett",
		"Kilo", "Lima", "Mike", "November", "Oscar",
		"Papa", "Quebec", "Romeo", "Sierra", "Tango",
		"Uniform", "Victor", "Whiskey", "X-ray", "Yankee",
		"Zulu"
	};
	
	public static final String[] GREEK = {
		"Alpha", "Beta", "Gamma", "Delta", "Epsilon",
		"Zeta", "Eta", "Theta", "Iota", "Kappa",
		"Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi",
		"Rho", "Sigma", "Tau", "Upsilon", "Phi",
		"Chi", "Psi", "Omega"
	};
	
	public static final String[] LATIN = {
		"Prima", "Secunda", "Tertia", "Quarta", "Quinta",
		"Sexta", "Septima", "Octava", "Nona", "Decima"
	};
	
	public static final String[] ROMAN = {
		"I", "II", "III", "IV", "V", "VI", "VIII", "IX", "X"
	};
	
	private int index;
	private String name;
	private String faction;
	private Integer year;
	private Integer eschelon;
	private int sizeMod;
	private boolean augmented;
	private Integer weightClass;
	private Integer unitType;
	private HashSet<EntityMovementMode> movementModes;
	private HashSet<MissionRole> roles;
	private String rating;
	private Integer experience;
	private Integer rankSystem;
	private Integer coRank;
	private HashSet<String> models;
	private HashSet<String> chassis;
	private HashSet<String> variants;
	private CrewDescriptor co;
	private CrewDescriptor xo;
	private String camo;
	
	private HashSet<String> flags;
	
    private FormationType formationType;
    private String generationRule;
	private boolean topLevel;
	private boolean element;
	private int positionIndex;
	private int nameIndex;
	private String fluffName;
	private Entity entity;
	
	private ForceDescriptor parent;
	private ArrayList<ForceDescriptor> subforces;
	private ArrayList<ForceDescriptor> attached;
	
	public ForceDescriptor() {
		faction = "IS";
		year = 3067;
		movementModes = new HashSet<EntityMovementMode>();
		roles = new HashSet<MissionRole>();
		formationType = null;
		experience = EXP_REGULAR;
		models = new HashSet<String>();
		chassis = new HashSet<String>();
		variants = new HashSet<String>();
		parent = null;
		subforces = new ArrayList<ForceDescriptor>();
		attached = new ArrayList<ForceDescriptor>();
		flags = new HashSet<String>();
		topLevel = false;
		element = false;
		positionIndex = -1;
		nameIndex = -1;
		fluffName = null;
	}
	
	public boolean matches(ChassisRecord cRec) {
		if (cRec.getUnitType() != unitType) {
			return false;
		}
		if (chassis.size() > 0 && !chassis.contains(cRec.getChassis())) {
			return false;
		}
		return true;
	}
	
	public boolean matches(ModelRecord mRec) {
		if (chassis.size() > 0 && !chassis.contains(mRec.getChassis())) {
			return false;
		}
		if (variants.size() > 0 && !variants.contains(mRec.getModel())) {
			return false;
		}
		if (models.size() > 0 && !models.contains(mRec.getKey())) {
			return false;
		}
		return true;
	}

	/**
	 * Goes through the force tree structure and generates units for all leaf nodes.
	 */
	public void generateUnits() {
	    //If the parent node has a chassis or model assigned, it carries through to the children.
	    if (null != parent) {
	        chassis.addAll(parent.getChassis());
	        models.addAll(parent.getModels());
	    }
	    //First see if a formation has been assigned. If unable to fulfill the formation requirements, generate using default parameters.
	    if (isElement()) {
	        setUnit(generate());
	    } else {
    	    if (null != formationType) {
    	        if (!generateFormation(subforces, ModelRecord.NETWORK_NONE)) {
    	            formationType = null;
    	        }
    	    }
    	    if (null == formationType) {
    	        if (null != generationRule) {
    	            switch(generationRule) {
    	            case "chassis":
    	            case "model":
    	                generate(generationRule);
    	                break;
    	            case "group":
    	                generateLance(subforces);
    	                break;
    	            }
    	        } else {
    	            subforces.forEach(fd -> fd.generateUnits());
    	        }
    	    }
	    }
	    attached.forEach(fd -> fd.generateUnits());
	}
	
	public boolean generateFormation(List<ForceDescriptor> subs, int networkMask) {
	    if (null == formationType) {
	        return false;
	    }
	    
        Map<UnitTable.Parameters, Integer> paramCount = new HashMap<>();
	    for (ForceDescriptor sub : subs) {
	        paramCount.merge(new UnitTable.Parameters(sub.getFactionRec(),
                    sub.getUnitType(), sub.getYear(), sub.getRating(), null, networkMask,
                    java.util.EnumSet.noneOf(EntityMovementMode.class),
                    java.util.EnumSet.noneOf(MissionRole.class), 0, sub.getFactionRec()), 1, Integer::sum);
	    }
	    
	    List<UnitTable.Parameters> params = new ArrayList<>();
	    List<Integer> numUnits = new ArrayList<>();
	    for (Map.Entry<UnitTable.Parameters, Integer> e : paramCount.entrySet()) {
	        params.add(e.getKey());
	        numUnits.add(e.getValue());
	    }
	    List<MechSummary> unitList = formationType.generateFormation(params, numUnits, networkMask, false);
	    if (unitList.isEmpty()) {
	        return false;
	    } else {
	        assert unitList.size() == subs.size();
	        for (int i = 0; i < unitList.size(); i++) {
	            ModelRecord mr = RATGenerator.getInstance().getModelRecord(unitList.get(i).getName());
	            subs.get(i).setUnit(mr);
	        }
	    }
	    return true;
	}
	
	public void generateLance(List<ForceDescriptor> subs) {
		if (subs.size() == 0) {
			return;
		}
		//If the formation type is set, use it instead. If the formation cannot be generated,
		//continue with the generic method.
		if (null != formationType && generateFormation(subs, ModelRecord.NETWORK_NONE)) {
		    return;
		}
		formationType = null;
		
		ModelRecord unit = null;
		if (chassis.size() > 0 || models.size() > 0) {
			for (ForceDescriptor sub : subs) {
				unit = sub.generate();
				if (unit != null) {
					sub.setUnit(unit);
				}
			}
			return;
		}
		
		/* This method can be used to generate pieces of a combined arms
		 * unit, so we need to get the unit type from one of the subforces
		 * rather than the current. */
		
		Integer ut = subs.get(0).getUnitType();
		
		boolean useWeights = useWeightClass(ut);
		ArrayList<Integer> weights = new ArrayList<Integer>();
		if (useWeights) {
			for (ForceDescriptor sub : subs) {
				weights.add(sub.getWeightClass());
			}
		} else {
			weights.add(-1);
			weights.add(0);
			weights.add(1);
			weights.add(2);
			weights.add(3);
			weights.add(4);
			weights.add(5);
			weights.add(null);
		}
		int ratingLevel = getRatingLevel();
		int totalLevels = 5;
/*		
 * 		Using the rating level relative to the total number of levels throws the results
 * 		off for ComStar, which should behave as A-B out of A-F rather than A-B out of A-B.
 * 
 * 		int totalLevels = RATGenerator.getInstance().getFaction(faction.split(",")[0]).getRatingLevels().size();
 */
		int target = 12 - ratingLevel;
		if (ratingLevel < 0) {
			target = 10;
		}
		Integer era = RATGenerator.getInstance().eraForYear(getYear());
		AvailabilityRating av = null;
		ModelRecord baseModel = null;
		/* Generate base model using weight class of entire formation */
		if (ut != null) {
			if (!(ut == UnitType.MEK || (ut == UnitType.AERO && subs.size() > 3))) {
				baseModel = subs.get(0).generate();
			}
			if (ut == UnitType.AERO || ut == UnitType.CONV_FIGHTER) {
				target -= 3;
			}
			if (roles.contains(MissionRole.ARTILLERY)) {
				if (baseModel != null &&
						baseModel.getRoles().contains(MissionRole.MISSILE_ARTILLERY)) {
					roles.remove(MissionRole.ARTILLERY);
					roles.add(MissionRole.MISSILE_ARTILLERY);
				} else {
					target -= 4;
				}
			}
		}
		for (ForceDescriptor sub : subs) {
			boolean foundUnit = false;
			if (baseModel == null || (ut != null && !ut.equals(sub.getUnitType()))) {
				unit = sub.generate();
				if (unit != null) {
					sub.setUnit(unit);
					baseModel = unit;
					if (useWeights) {
						weights.remove((Object)sub.getWeightClass());
					}
					foundUnit = true;
				}
			} else {
				for (String model : baseModel.getDeployedWith()) {
					String chassisKey = model + "[" + ut + "]";
					ChassisRecord cRec = RATGenerator.getInstance().getChassisRecord(chassisKey);
					if (cRec == null) {
						cRec = RATGenerator.getInstance().getChassisRecord(chassisKey + "Omni");
					}
					if (cRec != null) {
						av = RATGenerator.getInstance().
								findChassisAvailabilityRecord(era, model, faction, getYear());
						if (av == null) {
							for (String alt : RATGenerator.getInstance().getFaction(faction).getParentFactions()) {
								av = RATGenerator.getInstance().findChassisAvailabilityRecord(era, model, alt, getYear());
								if (av != null) {
									break;
								}
							}
						}
						if (Compute.d6(2) >= target - ((av == null)?0:av.adjustForRating(ratingLevel, totalLevels))) {
							sub.getChassis().clear();
							sub.getChassis().add(model);
							int oldWt = sub.getWeightClass();
							sub.setWeightClass(-1);
							unit = sub.generate();
							if (unit != null && weights.contains(unit.getWeightClass())) {
								sub.setUnit(unit);
								if (useWeights) {
									weights.remove((Object)sub.getWeightClass());
								}
								foundUnit = true;
								break;
							} else {
								sub.setWeightClass(oldWt);
							}
						}
					} else {
						ModelRecord mRec = RATGenerator.getInstance().getModelRecord(model);
						if (mRec != null && weights.contains(mRec.getWeightClass())
								&& RATGenerator.getInstance().findModelAvailabilityRecord(era, model, faction) != null) {
							av = RATGenerator.getInstance().findChassisAvailabilityRecord(era, mRec.getChassisKey(), faction, getYear());
							if (av == null) {
								for (String alt : RATGenerator.getInstance().getFaction(faction).getParentFactions()) {
									av = RATGenerator.getInstance().findChassisAvailabilityRecord(era, mRec.getChassisKey(), alt, getYear());
									if (av != null) {
										break;
									}
								}
							}
							if (Compute.d6(2) >= target - ((av == null)?0:av.adjustForRating(ratingLevel, totalLevels))) {
								sub.setUnit(mRec);
								if (useWeights) {
									weights.remove((Object)mRec.getWeightClass());
								}
								foundUnit = true;
								break;
							}
						}
					}
				}
				if (!foundUnit && weights.contains(baseModel.getWeightClass())) {
					av = RATGenerator.getInstance().findChassisAvailabilityRecord(era, baseModel.getChassisKey(), faction, getYear());
					if (av == null) {
						for (String alt : RATGenerator.getInstance().getFaction(faction).getParentFactions()) {
							av = RATGenerator.getInstance().findChassisAvailabilityRecord(era, baseModel.getChassisKey(), alt, getYear());
							if (av != null) {
								break;
							}
						}
					}
					if (Compute.d6(2) >= target - ((av == null)?0:av.adjustForRating(ratingLevel, totalLevels))) {
						sub.getChassis().add(baseModel.getChassis());
						sub.setWeightClass(-1);
						unit = sub.generate();
						if (unit != null) {
							sub.setUnit(unit);
							if (useWeights) {
								weights.remove((Object)sub.getWeightClass());
							}
							foundUnit = true;
						}
					} else if (ut == UnitType.TANK && Compute.d6(2) >= target - 6) {
						if (useWeights) {
							switch (baseModel.getMechSummary().getUnitSubType()) {
							case "Hover":
								if (weights.contains(EntityWeightClass.WEIGHT_HEAVY)) {
									break;
								}
								/* fall through */
							case "Wheeled":
								if (weights.contains(EntityWeightClass.WEIGHT_ASSAULT)) {
									break;
								}
								sub.getMovementModes().add(baseModel.getMovementMode());
							}
						}
					} else if (ut == UnitType.INFANTRY) {
						sub.getMovementModes().add(baseModel.getMovementMode());
					}
				}
			}
			if (!foundUnit) {
				if (!weights.contains(sub.getWeightClass())) {
					sub.setWeightClass(weights.get(0));
				}
				unit = sub.generate();
				if (unit == null) {
					sub.getMovementModes().clear();
					unit = sub.generate();
				}
				if (unit != null) {
					sub.setUnit(unit);
					if (useWeights) {
						weights.remove((Object)sub.getWeightClass());
					}
				}
			}
			if (ut == null || ut == UnitType.MEK) {
				baseModel = null;
			}
		}
	}
	
	public void setUnit(ModelRecord unit) {
		chassis.clear();
		variants.clear();
		models.clear();
		models.add(unit.getKey());
		if (useWeightClass()) {
			weightClass = unit.getWeightClass();
		}
		element = true;
		movementModes.clear();
		movementModes.add(unit.getMovementMode());
		if ((unitType.equals("Mek") || unitType.equals("Aero")
				|| unitType.equals("Tank"))
				&&unit.isOmni()) {
			flags.add("omni");
		}
		if (unit.getRoles().contains(MissionRole.ARTILLERY)) {
			roles.add(MissionRole.ARTILLERY);
		}
		if (unit.getRoles().contains(MissionRole.MISSILE_ARTILLERY)) {
			roles.add(MissionRole.MISSILE_ARTILLERY);
		}
		if (unit.getRoles().contains(MissionRole.ANTI_MEK)) {
			roles.add(MissionRole.ANTI_MEK);
		}
		if (unit.getRoles().contains(MissionRole.FIELD_GUN)) {
			roles.add(MissionRole.FIELD_GUN);
		}
	}
	
	public void generate(String level) {
		ModelRecord mRec = generate();
		if (mRec != null) {
			if (level.equals("chassis")) {
				getChassis().add(mRec.getChassis());
			} else {
				getModels().add(mRec.getKey());
			}
		}		
	}
	
	public ModelRecord generate() {
		/* If the criteria cannot be matched, first try the next closest weight class,
		 * then ignore mission role, then the next weight class, then ignore motive types,
		 * then remaining weight classes.
		 */
		final int[][] altWeights = {
				{1, 2, 3, 4, 5}, //UL
				{2, 0, 3, 4, 5}, //L
				{3, 1, 4, 0, 5}, //M
				{2, 4, 1, 5, 0}, //H
				{3, 2, 5, 1, 0}, //A
				{4, 3, 2, 1, 0}  //SH
		};
		/* Work with a copy */
		ForceDescriptor fd = createChild(index);
		fd.setEschelon(eschelon);
		fd.setCoRank(coRank);
		fd.getRoles().clear();
		fd.getRoles().addAll(roles.stream().filter(r -> r.fitsUnitType(unitType)).collect(Collectors.toList()));
		
		int wtIndex = (useWeightClass() && weightClass != null && weightClass != -1)?0:4;
		
		while (wtIndex < 5) {
			for (int roleStrictness = 3; roleStrictness >= 0; roleStrictness--) {
				List<Integer> wcs = new ArrayList<>();
				if (useWeightClass() && fd.getWeightClass() >= 0) {
					wcs.add(fd.getWeightClass());
				}
/*				System.out.println("Getting table: " + fd.getFaction() + ","
						+ fd.getUnitType() + "," + fd.getYear() + ","
						+ fd.getRating() + ","
							+ wcs.stream().map(i -> String.valueOf(i))
								.collect(Collectors.joining("|"))
						+ "," + roles.stream().collect(Collectors.joining("|"))
						+ "," + roleStrictness);
*/
				//TODO: Add cache to UnitTable
				String ratGenRating = null;
				int ratingLevel = getRatingLevel();
				if (ratingLevel >= 0) {
					List<String> ratings = getFactionRec().getRatingLevelSystem();
					ratGenRating = ratings.get(Math.min(ratingLevel, ratings.size() - 1));
				}
				UnitTable table = UnitTable.findTable(fd.getFactionRec(), fd.getUnitType(),
						fd.getYear(), ratGenRating, wcs, ModelRecord.NETWORK_NONE,
						fd.getMovementModes(), fd.getRoles(), roleStrictness);
				MechSummary ms = null;
				if (fd.getMovementModes().isEmpty() && fd.getChassis().isEmpty() && fd.getModels().isEmpty()) {
					ms = table.generateUnit();
				} else {
					ms = table.generateUnit(u -> (fd.getMovementModes().isEmpty() || fd.getMovementModes().contains(u.getUnitSubType()))
							&& (fd.getChassis().isEmpty() || fd.getChassis().contains(u.getChassis()))
							&& (fd.getModels().isEmpty() || fd.getModels().contains(u.getName())));
				}
				if (ms != null && RATGenerator.getInstance().getModelRecord(ms.getName()) != null) {
					return RATGenerator.getInstance().getModelRecord(ms.getName());
				}

				if ((!useWeightClass() || wtIndex == 2) && fd.getRoles().size() > 0) {
					fd.getRoles().clear();
				} else if ((!useWeightClass() || wtIndex == 1)
						&& fd.getMovementModes().size() > 0) {
					fd.getMovementModes().clear();
				} else {
					if (useWeightClass() && weightClass != -1 && weightClass < altWeights.length && wtIndex < altWeights[weightClass].length) {
						fd.setWeightClass(altWeights[weightClass][wtIndex]);
					}
					wtIndex++;
				}
			}
		};
		
		System.err.println("Could not find unit for " + unitType);
		return null;
	}
	
	public void loadEntities() {
		if (element) {
			MechSummary ms = MechSummaryCache.getInstance().getMech(getModelName());
			if (ms != null) {
				try {
					entity = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
					entity.setCrew(getCo().createCrew());
				} catch (EntityLoadingException ex) {
					System.err.println("Error loading " + ms.getName() + " from file " + ms.getSourceFile().getPath());
				}
			}
		}
		subforces.stream().forEach(sf -> sf.loadEntities());
		attached.stream().forEach(sf -> sf.loadEntities());
	}
	
	public void assignCommanders() {
		for (ForceDescriptor fd : subforces) {
			fd.assignCommanders();
		}
		Ruleset rules = Ruleset.findRuleset(this);
		CommanderNode coNode = null;
		CommanderNode xoNode = null;

		while (coNode == null && rules != null) {
			coNode = rules.getCoNode(this);
			xoNode = rules.getXoNode(this);
			if (coNode == null) {
				if (rules.getParent() == null) {
					setCo(new CrewDescriptor(this));
					return;
				}
				rules = Ruleset.findRuleset(rules.getParent());
			}
		}
		//If none is found, assign crew without assigning rank or title.
		if (coNode == null) {
			setCo(new CrewDescriptor(this));
			return;
		}
		
		if (subforces.size() > 0) {
			int coPos = 0;
			if (coNode != null) {
				coPos = (coNode.getPosition() == null)?1:
					Math.min(coNode.getPosition(), 1);
			}
			int xoPos = 0;
			if (xoNode != null && (xoNode.getPosition() == null || xoNode.getPosition() > 0)) {
				xoPos = (xoNode.getPosition() == null)?coPos + 1:
					Math.max(coPos, xoNode.getPosition());
			}
			if (coPos + xoPos > 0) {
				ForceDescriptor [] forces = subforces.toArray(new ForceDescriptor[subforces.size()]);
				Arrays.sort(forces, forceSorter);
				if (coPos != 0) {
					ForceDescriptor coFound = null;
					if (coNode.getUnitType() != null) {
						for (ForceDescriptor fd : forces) {
							if (fd.getUnitType() != null && fd.getUnitType().equals(coNode.getUnitType())) {
								coFound = fd;
							}
						}
					}
					if (coFound == null) {
						coFound = forces[0];
					}
					setCo(coFound.getCo());
					subforces.remove(coFound);
					subforces.add(0, coFound);												
				}
				if (xoPos != 0) {
					/* If the XO is a field officer, the position is assigned to the first subforce that doesn't contain the CO
					 * (which is the first if the CO is not a field officer). If the CO and XO positions are the same, the
					 * XO is assigned to the same subforce as the CO, but the second subforce of that one.
					 */
					ForceDescriptor xoFound = null;
					ArrayList<ForceDescriptor> subforces = this.subforces;
					if (coPos == xoPos) {
						subforces = this.subforces.get(0).getSubforces();
					}
					if (subforces.size() > coPos) {
						if (xoNode.getUnitType() != null) {
							for (int i = coPos; i < subforces.size(); i++) {
								if (subforces.get(i).getUnitType() != null &&
										(xoNode.getUnitType().equals(subforces.get(i).getUnitType())
										|| (xoNode.getUnitType().equals("other")
												&& !subforces.get(i).getUnitType().equals(co.getAssignment().getUnitType())))) {
									xoFound = subforces.get(i);
									break;
								}
							}
						}
						if (xoFound == null) {
							xoFound = subforces.get(1);
						}
					}
					
					if (xoFound != null) {
						setXo(xoFound.getCo());
						getXo().setRank(xoNode.getRank());
					}
				}
			}
		}

		if (getCo() == null) {
			setCo(new CrewDescriptor(this));
		}
		getCo().setRank(coNode.getRank());
		getCo().setTitle(coNode.getTitle());

		if (xoNode != null) {
			if (getXo() == null) {
				setXo(new CrewDescriptor(this));
			}
			getXo().setRank(xoNode.getRank());
			getXo().setTitle(xoNode.getTitle());
		}
		if (!element) {
			movementModes.clear();
			boolean isOmni = true;
			boolean isArtillery = true;
			boolean isMissileArtillery = true;
			boolean isFieldGun = true;
			for (ForceDescriptor fd : subforces) {
				movementModes.addAll(fd.getMovementModes());
				if ((fd.getUnitType() == null ||
						!(fd.getUnitType().equals("Mek") || fd.getUnitType().equals("Aero")
						|| fd.getUnitType().equals("Tank"))) ||
						!fd.getFlags().contains("omni")) {
					isOmni = false;
				}
				if (!fd.getRoles().contains(MissionRole.MISSILE_ARTILLERY)) {
					isMissileArtillery = false;
				}
				if (!fd.getRoles().contains(MissionRole.ARTILLERY)
						&& !fd.getFlags().contains(MissionRole.MISSILE_ARTILLERY)) {
					isArtillery = false;
				}
				if (!fd.getRoles().contains(MissionRole.FIELD_GUN)) {
					isFieldGun = false;
				}
			}
			if (isOmni) {
				flags.add("omni");
			}
			if (isArtillery) {
				roles.add(MissionRole.ARTILLERY);
			}
			if (isMissileArtillery) {
				roles.add(MissionRole.MISSILE_ARTILLERY);
			}
			if (isFieldGun) {
				roles.add(MissionRole.FIELD_GUN);
			}
			
			float wt = 0;
			int c = 0;
			for (ForceDescriptor sub : subforces) {
				if (sub.useWeightClass()) {
					if (sub.getWeightClass() == null) {
						System.err.println("Weight class == null for " + sub.getUnitType() + " with " + sub.getSubforces().size() + " subforces.");
					} else {
						wt += sub.getWeightClass();
						c++;
					}
				}
			}
			if (c > 0) {
				weightClass = (int)(wt / c + 0.5);
			}
		}
		
//		setIcon();
	}
	
	public void assignPositions() {
		int index = 0;
		HashMap<String,Integer> uniqueCount = new HashMap<String,Integer>();
		for (int i = 0; i < subforces.size(); i++) {
			subforces.get(i).positionIndex = i + 1;
			if (subforces.get(i).name == null) {
				continue;
			}
			if (subforces.get(i).name.contains(":distinct}")) {
				if (uniqueCount.containsKey(subforces.get(i).name)) {
					uniqueCount.put(subforces.get(i).name, uniqueCount.get(subforces.get(i).name) + 1);
				} else {
					uniqueCount.put(subforces.get(i).name, 1);
				}
			} else if (subforces.get(i).name.matches(".*\\{[^:]*\\}.*")) {
				subforces.get(i).nameIndex = index++;
			}
		}
		HashMap<String,Integer> indexCount = new HashMap<String,Integer>();
		for (ForceDescriptor sub : subforces) {
			if (uniqueCount.containsKey(sub.name)) {
				if (uniqueCount.get(sub.name) > 1) {
					if (indexCount.containsKey(sub.name)) {
						indexCount.put(sub.name, indexCount.get(sub.name) + 1);
					} else {
						indexCount.put(sub.name, 1);
					}
					sub.nameIndex = indexCount.get(sub.name) - 1;
				} else {
					sub.nameIndex = -1;
				}
				sub.name = sub.name.replace(":distinct", "");
			}
			sub.assignPositions();
		}
	}
	
	private Comparator<? super ForceDescriptor> forceSorter = new Comparator<ForceDescriptor>() {
		/* Rank by difference in experience + difference in unit/eschelon weights */
		private int rank(ForceDescriptor fd) {
			int retVal = 0;
			if (fd.getWeightClass() != null) {
				retVal += fd.getWeightClass();
			}
			if (fd.getUnitType() != null) {
				switch (fd.getUnitType()) {
				case UnitType.MEK:
					retVal += 2;
					break;
				case UnitType.INFANTRY:
					retVal -= 2;
				}
			}
			if (fd.getCo() != null) {
				retVal -= fd.getCo().getGunnery() + fd.getCo().getPiloting();
				ModelRecord mRec = RATGenerator.getInstance().getModelRecord(fd.getCo().getAssignment().getModelName());
				if (mRec != null) {
					if (mRec.isSL()) {
						retVal += 2;
					}
					if (mRec.isClan()) {
						retVal += 5;
					}
				}
			}
			return retVal;
		}
		
		public int compare(ForceDescriptor arg0, ForceDescriptor arg1) {
			if (arg0.getRoles().contains(MissionRole.COMMAND) && !arg1.getRoles().contains(MissionRole.COMMAND)) {
				return -1;
			}
			if (!arg0.getRoles().contains(MissionRole.COMMAND) && arg1.getRoles().contains(MissionRole.COMMAND)) {
				return 1;
			}
			if (arg0.getRatingLevel() != arg1.getRatingLevel()) {
				return arg1.getRatingLevel() - arg0.getRatingLevel();
			}
			return rank(arg1) - rank(arg0);
		}
	};
	
	/*
	public void assignBloodnames() {
		assignBloodnames(getFactionRec());
	}
	
	public void assignBloodnames(FactionRecord fRec) {
		if (fRec != null && fRec.isClan()) {
			for (ForceDescriptor fd : subforces) {
				fd.assignBloodnames();
			}
			for (ForceDescriptor fd : attached) {
				fd.assignBloodnames();
			}

			if (co != null && (element || !(co.getAssignment() != null && co.getAssignment().isElement()))) {
				co.assignBloodname();
			}
			if (xo != null && !(xo.getAssignment() != null && xo.getAssignment().isElement())) {
				xo.assignBloodname();
			}
		}
	}
	*/
	
	public static int decodeWeightClass(String code) {
		switch (code) {
		case "UL":return EntityWeightClass.WEIGHT_ULTRA_LIGHT;
		case "L":return EntityWeightClass.WEIGHT_LIGHT;
		case "M":return EntityWeightClass.WEIGHT_MEDIUM;
		case "H":return EntityWeightClass.WEIGHT_HEAVY;
		case "A":return EntityWeightClass.WEIGHT_ASSAULT;
		case "SH":case "C":return EntityWeightClass.WEIGHT_COLOSSAL;
		}
		return -1;
	}
	
	public String getWeightClassCode() {
		final String[] codes = {"UL", "L", "M", "H", "A", "SH"};
		if (weightClass == null || weightClass == -1) {
			return "";
		}
		return codes[weightClass];
	}
	   // AeroSpace Units
    public static final int WEIGHT_SMALL_CRAFT = 6; // Only a single weight class for Small Craft
    public static final int WEIGHT_SMALL_DROP = 7;
    public static final int WEIGHT_MEDIUM_DROP = 8;
    public static final int WEIGHT_LARGE_DROP = 9;
    public static final int WEIGHT_SMALL_WAR = 10;
    public static final int WEIGHT_LARGE_WAR = 11;
    
    // Support Vehicles
    public static final int WEIGHT_SMALL_SUPPORT = 12;
    public static final int WEIGHT_MEDIUM_SUPPORT = 13;
    public static final int WEIGHT_LARGE_SUPPORT = 14;
	
    public boolean useWeightClass() {
    	return useWeightClass(unitType);
    }
    
	private boolean useWeightClass(Integer ut) {
		return ut != null &&
				 !(roles.contains(MissionRole.ARTILLERY) || roles.contains(MissionRole.MISSILE_ARTILLERY)) &&
				(ut == UnitType.MEK ||
						ut == UnitType.AERO ||
						ut == UnitType.TANK ||
						ut == UnitType.BATTLE_ARMOR);
	}
	
	public ArrayList<Object> getAllChildren() {
		ArrayList<Object> retVal = new ArrayList<Object>();
		retVal.addAll(subforces);
		retVal.addAll(attached);
		return retVal;
	}
	
    public int getIndex() {
        return index;
    }
    
    public void setIndex(int index) {
        this.index = index;
    }
    
	public String getName() {
		return name;
	}
	
	public String parseName() {
		String retVal = name;
		if (name == null) {
			String eschName = Ruleset.findRuleset(this).getEschelonName(this);
			if (eschName == null) {
				return "";
			}
			retVal = "{ordinal} " + eschName;
		}
		if (getParent() != null && getParent().getNameIndex() >= 0) {
			retVal = retVal.replace("{ordinal:parent}", ORDINALS[getParent().getNameIndex()]);
			retVal = retVal.replace("{greek:parent}", GREEK[getParent().getNameIndex()]);
			retVal = retVal.replace("{phonetic:parent}", PHONETIC[getParent().getNameIndex()]);
			retVal = retVal.replace("{latin:parent}", LATIN[getParent().getNameIndex()]);
			retVal = retVal.replace("{roman:parent}", ROMAN[getParent().getNameIndex()]);
			retVal = retVal.replace("{cardinal:parent}", Integer.toString(getParent().getNameIndex() + 1));
		}
		if (getParent() != null && retVal.contains("{name:parent}")) {
			String parentName = getParent().getName().replaceAll(".*\\[", "").replaceAll("\\].*", "");
			retVal = retVal.replace("{name:parent}", parentName);
		}
		if (nameIndex < 0) {
			retVal = retVal.replaceAll("\\{.*?\\}\\s?", "");
		} else {
			retVal = retVal.replace("{ordinal}", ORDINALS[getNameIndex()]);
			retVal = retVal.replace("{greek}", GREEK[getNameIndex()]);
			retVal = retVal.replace("{phonetic}", PHONETIC[getNameIndex()]);
			retVal = retVal.replace("{latin}", LATIN[getNameIndex()]);
			retVal = retVal.replace("{roman}", ROMAN[getNameIndex()]);
			retVal = retVal.replace("{cardinal}", Integer.toString(getNameIndex() + 1));
		}
		retVal = retVal.replaceAll("\\{.*?\\}", "");
		retVal = retVal.replaceAll("[\\[\\]]", "").replaceAll("\\s+", " ");
		return retVal.trim();
	}
	
	public String getDescription() {
		StringBuilder retVal = new StringBuilder();
		if (unitType != null) {
//			if (useWeightClass() && weightClass != null && weightClass >= 0) {
			if (weightClass != null && weightClass >= 0) {
				retVal.append(EntityWeightClass.getClassName(weightClass)).append(" ");
			}
			
			if (roles.contains("artillery") || roles.contains("missile_artillery")) {
				retVal.append(unitType.equals("Infantry")?"Field":"Mobile").append(" ");
			} else {
				retVal.append(UnitType.getTypeName(unitType)).append(" ");
			}
		}
		
		if (roles.contains("recon")) {
			retVal.append("Recon");
		} else if (roles.contains("fire support")) {
			retVal.append("Fire Support");
		} else if (roles.contains("artillery")) {
			retVal.append("Artillery");
		} else if (roles.contains("urban")) {
			retVal.append("Urban");
		}
		Ruleset rules = Ruleset.findRuleset(this);
		String eschName = null;

		while (eschName == null && rules != null) {
			eschName = rules.getEschelonName(this);
			if (eschName == null) {
				if (rules.getParent() == null) {
					rules = null;
				} else {
					rules = Ruleset.findRuleset(rules.getParent());
				}
			}
		}
		
		if (eschName != null) {
			retVal.append(" ").append(eschName);
		}
		if (null != formationType) {
		    retVal.append(" (").append(formationType.getName()).append(")");
		}
		return retVal.toString();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFaction() {
		return faction;
	}

	public void setFaction(String faction) {
		this.faction = faction;
	}
	
	public FactionRecord getFactionRec() {
		return RATGenerator.getInstance().getFaction(faction.split(",")[0]);
	}
	
	public Integer getYear() {
		return year;
	}

	public void setYear(Integer year) {
		this.year = year;
	}

	public Integer getEschelon() {
		return eschelon;
	}
	
	public String getEschelonCode() {
		String retVal = eschelon.toString();
		if (augmented) {
			retVal += "*";
		}
		return retVal;
	}

	public void setEschelon(Integer eschelon) {
		this.eschelon = eschelon;
	}

	public int getSizeMod() {
		return sizeMod;
	}

	public void setSizeMod(int sizeMod) {
		this.sizeMod = sizeMod;
	}

	public boolean isAugmented() {
		return augmented;
	}
	
	public void setAugmented(boolean augmented) {
		this.augmented = augmented;
	}
	
	public Integer getWeightClass() {
		return weightClass;
	}

	public void setWeightClass(Integer weightClass) {
		this.weightClass = weightClass;
	}

	public Integer getUnitType() {
		return unitType;
	}

	public void setUnitType(Integer unitType) {
		this.unitType = unitType;
	}

	public HashSet<EntityMovementMode> getMovementModes() {
		return movementModes;
	}

	public String getRating() {
		return rating;
	}

	public void setRating(String rating) {
		this.rating = rating;
	}
	
	public int getRatingLevel() {
		if (rating != null) {
			Ruleset rs = Ruleset.findRuleset(this);
			if (rs != null) {
				return rs.getRatingIndex(rating);
			}
		}
		return -1;
	}
	
	public FormationType getFormation() {
	    return formationType;
	}
	
	public void setFormationType(FormationType ft) {
	    formationType = ft;
	}
	
	public String getGenerationRule() {
	    return generationRule;
	}
	
	public void setGenerationRule(String rule) {
	    generationRule = rule;
	}

	public Set<MissionRole> getRoles() {
		return roles;
	}
	
	public Set<String> getModels() {
		return models;
	}
	
	public String getModelName() {
		if (models.size() != 1) {
			return "";
		}
		return models.iterator().next();
	}

	public Set<String> getChassis() {
		return chassis;
	}

	public Set<String> getVariants() {
		return variants;
	}

	public Integer getExperience() {
		return experience;
	}

	public void setExperience(Integer experience) {
		this.experience = experience;
	}

	public Integer getCoRank() {
		return coRank;
	}

	public void setCoRank(Integer coRank) {
		this.coRank = coRank;
	}
	
	public Integer getRankSystem() {
		return rankSystem;
	}

	public void setRankSystem(Integer rankSystem) {
		this.rankSystem = rankSystem;
	}

	public CrewDescriptor getCo() {
		return co;
	}

	public void setCo(CrewDescriptor co) {
		this.co = co;
	}

	public CrewDescriptor getXo() {
		return xo;
	}

	public void setXo(CrewDescriptor xo) {
		this.xo = xo;
	}

	public String getCamo() {
		return camo;
	}

	public void setCamo(String camo) {
		this.camo = camo;
	}

	public ForceDescriptor getParent() {
		return parent;
	}

	public void setParent(ForceDescriptor parent) {
		this.parent = parent;
	}
	public ArrayList<ForceDescriptor> getSubforces() {
		return subforces;
	}

	public void setSubforces(ArrayList<ForceDescriptor> subforces) {
		this.subforces = subforces;
	}
	
	public void addSubforce(ForceDescriptor fd) {
		subforces.add(fd);
		fd.setParent(this);
	}

	public ArrayList<ForceDescriptor> getAttached() {
		return attached;
	}

	public void setAttached(ArrayList<ForceDescriptor> attached) {
		this.attached = attached;
	}

	public void addAttached(ForceDescriptor fd) {
		attached.add(fd);
	}

	public Set<String> getFlags() {
		return flags;
	}

	public boolean isTopLevel() {
		return topLevel;
	}

	public void setTopLevel(boolean topLevel) {
		this.topLevel = topLevel;
	}
	
	public boolean isElement() {
		return element;
	}

	public void setElement(boolean element) {
		this.element = element;
	}
	
	public int getPositionIndex() {
		return positionIndex;
	}
	
	public int getNameIndex() {
		return nameIndex;
	}
	
	public String getFluffName() {
		return fluffName;
	}
	
	public void setFluffName(String fluffName) {
		this.fluffName = fluffName;
	}
	
	public Entity getEntity() {
		return entity;
	}
	
	public void addAllEntities(List<Entity> list) {
		if (isElement()) {
			if (entity != null) {
				list.add(entity);
			}
		}
		subforces.stream().forEach(sf -> sf.addAllEntities(list));
		attached.stream().forEach(sf -> sf.addAllEntities(list));
	}

	public ForceDescriptor createChild(int index) {
		ForceDescriptor retVal = new ForceDescriptor();
		retVal.index = index;
		retVal.name = null;
		retVal.faction = faction;
		retVal.year = year;
		retVal.weightClass = weightClass;
		retVal.unitType = unitType;
		retVal.movementModes.addAll(movementModes);
		retVal.roles.addAll(roles);
		retVal.roles.remove("command");
		retVal.models.addAll(models);
		retVal.chassis.addAll(chassis);
		retVal.variants.addAll(variants);
		retVal.augmented = augmented;
		retVal.rating = rating;
		retVal.experience = experience;
		retVal.camo = camo;
		retVal.flags.addAll(flags);
		retVal.topLevel = false;
		retVal.rankSystem = rankSystem;
		
		return retVal;
	}
	
	public void show(String indent) {
		final String[] eschelonNames = {
				"Element", "Squad", "(2)", "Lance", "Company", "Battalion",
				"Regiment", "Brigade", "Division", "Corps", "Army"
		};
		final String[] airEschelonNames = {
				"Element", "(1)", "(2)", "Flight", "Squadron", "Group", "Wing", "Regiment"
		};

		System.out.println(indent + weightClass + " " + unitType + " "
				+ ((unitType == UnitType.AERO || unitType == UnitType.CONV_FIGHTER)?airEschelonNames[eschelon]:eschelonNames[eschelon]));
		for (ForceDescriptor sub : subforces) {
			sub.show(indent + "  ");
		}
		for (ForceDescriptor sub : attached) {
			sub.show(indent + " +");
		}
	}
}

