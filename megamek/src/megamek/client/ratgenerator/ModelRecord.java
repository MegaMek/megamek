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

import java.util.ArrayList;

import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.WeaponType;
import megamek.common.loaders.EntityLoadingException;

/**
 * Specific unit variants; analyzes equipment to determine suitability for certain types
 * of missions in addition to what is formally declared in the data files.
 * 
 * @author Neoancient
 *
 */
public class ModelRecord extends AbstractUnitRecord {
	private String model;
	private boolean clan;
	private boolean starLeague;
	private int weightClass;
	private ArrayList<MissionRole> roles;
	private ArrayList<String> deployedWith;
	private ArrayList<String> requiredUnits;
	private ArrayList<String> excludedFactions;
	private int network;
	private double flak; //proportion of weapon BV that can fire flak ammo
	private double longRange; //proportion of weapon BV with range >= 20 hexes
	private int speed;
	private double ammoRequirement; //used to determine suitability for raider role
	private boolean flamer; //used to determine suitability for incindiary role
	private boolean apWeapons; //used to determine suitability for anti-infantry role
	
	/* There are some occasions when we need to know whether it's a quad chassis
	 * (e.g. determining whether BA is eligible to be used as mechanized BA) but
	 * we can't tell whether it's a quad based on the MechSummary. Rather than
	 * load every unit to check, we only check when the info is needed, then
	 * store the result for future reference.
	 */
	protected Boolean quad;

	public ModelRecord(String chassis, String model) {
		super(chassis);
		this.model = model;
		roles = new ArrayList<MissionRole>();
		deployedWith = new ArrayList<String>();
		requiredUnits = new ArrayList<String>();
		excludedFactions = new ArrayList<String>();
		network = NETWORK_NONE;
		flak = 0.0;
		longRange = 0.0;
		quad = null;
	}
	
	public ModelRecord(MechSummary ms) {
		this(ms.getChassis(), ms.getModel());
		unitType = ms.getUnitType();
		movementType = findMovementType(ms.getUnitSubType());
		introYear = ms.getYear();
    	switch (ms.getUnitType()) {
    	case "Mek":
    		omni = ms.getUnitSubType().equals("Omni");
    		break;
    	case "Tank":
    	case "VTOL":
    	case "Naval":
    		omni = ms.getModel().equals("Prime") ||
    				(ms.getModel().matches("[A-Z]") &&
    						MechSummaryCache.getInstance().getMech(ms.getChassis() + " Prime") != null);
    		break;
    	case "Aero":
    		if (ms.isClan()) {
        		omni = ms.getModel().equals("Prime") ||
        				ms.getModel().equals("(Sealed)") ||
        				(ms.getModel().matches("[A-Z]") &&
        						MechSummaryCache.getInstance().getMech(ms.getChassis() + " Prime") != null);
    		} else {
    			omni = ms.getModel().matches(".*\\-O[A-Z]?\\s?.*") ||
    					ms.getModel().startsWith("DARO-1") ||
    					ms.getModel().startsWith("MR-1S");
    		}
    		break;
    	case "BattleArmor":
    		omni = true;
    		break;
    	}

    	double totalBV = 0.0;
    	double flakBV = 0.0;
    	double lrBV = 0.0;
    	double ammoBV = 0.0;
    	boolean losTech = false;
    	for (int i = 0; i < ms.getEquipmentNames().size(); i++) {
    		EquipmentType eq = EquipmentType.get(ms.getEquipmentNames().get(i));
    		if (eq == null) {
    			continue;
    		}
    		if (!eq.isAvailableIn(3000)) {
    			//FIXME: needs to filter out primitive
    			losTech = true;
    		}
    		if (eq instanceof megamek.common.weapons.Weapon) {
    			totalBV += eq.getBV(null) * ms.getEquipmentQuantities().get(i);
        		if (eq instanceof megamek.common.weapons.LBXACWeapon ||
        				eq instanceof megamek.common.weapons.ArtilleryWeapon ||
        				eq instanceof megamek.common.weapons.HAGWeapon ||
        				eq instanceof megamek.common.weapons.ISSilverBulletGauss) {
        			flakBV += eq.getBV(null) * ms.getEquipmentQuantities().get(i);	        			
        		}
        		if (eq instanceof megamek.common.weapons.FlamerWeapon) {
        			flamer = true;
        			apWeapons = true;
        		}
        		if (eq instanceof megamek.common.weapons.MGWeapon ||
        				eq instanceof megamek.common.weapons.BPodWeapon) {
        			apWeapons = true;
        		}
        		if (((WeaponType) eq).getAmmoType() > megamek.common.AmmoType.T_NA) {
        			ammoBV += eq.getBV(null) * ms.getEquipmentQuantities().get(i);
        		}
        		if (((WeaponType)eq).getLongRange() >= 20) {
        			lrBV += eq.getBV(null) * ms.getEquipmentQuantities().get(i);
        		}
    		} else {
    			switch (eq.getInternalName()) {
    			case "ISC3MasterUnit":
    			case "ISC3MasterBoostedSystemUnit":
    				network = AbstractUnitRecord.NETWORK_C3_MASTER;
    				break;
    			case "ISC3SlaveUnit":
    			case "ISC3EmergencyMaster":
    			case "ISC3BoostedSystemSlaveUnit":
    				network = AbstractUnitRecord.NETWORK_C3_SLAVE;
    				break;
    			case "ISC3iUnit":
    				network = AbstractUnitRecord.NETWORK_C3I;
    				break;
    			case "ISNC3Unit":
    				network = AbstractUnitRecord.NETWORK_NAVAL_C3;
    				break;
    			case "BattleArmorC3":
    				network = AbstractUnitRecord.NETWORK_BA_C3;
    				break;
    			case "ISBC3i":
    				network = AbstractUnitRecord.NETWORK_BA_C3I;
    				break;
    			case megamek.common.Sensor.NOVA:
    				network = AbstractUnitRecord.NETWORK_NOVA;
    				break;
    			}
    		}
    	}
		if (totalBV > 0 &&
				(ms.getUnitType().equals("Mek") ||
						ms.getUnitType().equals("Tank") ||
						ms.getUnitType().equals("BattleArmor") ||
						ms.getUnitType().equals("Infantry") ||
						ms.getUnitType().equals("ProtoMek") ||
						ms.getUnitType().equals("Naval") ||
						ms.getUnitType().equals("Gun Emplacement"))) {
			flak = flakBV / totalBV;
			longRange = lrBV / totalBV;
			ammoRequirement = ammoBV / totalBV;
		}
    	weightClass = ms.getWeightClass();
    	if (weightClass >= EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
    		if (ms.getTons() <= 39) {
    			weightClass = EntityWeightClass.WEIGHT_LIGHT;
    		} else if (ms.getTons() <= 59) {
    			weightClass = EntityWeightClass.WEIGHT_MEDIUM;
    		} else if (ms.getTons() <= 79) {
    			weightClass = EntityWeightClass.WEIGHT_HEAVY;
    		} else if (ms.getTons() <= 100) {
    			weightClass = EntityWeightClass.WEIGHT_ASSAULT;
    		} else {
    			weightClass = EntityWeightClass.WEIGHT_COLOSSAL;
    		}
    	}
    	clan = ms.isClan();
    	if (megamek.common.Engine.getEngineTypeByString(ms.getEngineName()) == megamek.common.Engine.XL_ENGINE
    			|| ms.getArmorType().contains(EquipmentType.T_ARMOR_FERRO_FIBROUS)
    			|| ms.getInternalsType() == EquipmentType.T_STRUCTURE_ENDO_STEEL) {
    		losTech = true;
    	}
    	starLeague = losTech && !clan;
    	speed = ms.getWalkMp();
    	if (ms.getJumpMp() > 0) {
    		speed++;
    	}
	}
	
	private int findMovementType(String subtype) {
		if (unitType.equals("Conventional Fighter")) {
			return MOVEMENT_ATMOSPHERIC;
		}
		if (unitType.equals("Jumpship") || unitType.equals("Warship")) {
			return MOVEMENT_SPACE;
		}
		switch (subtype) {
		case "BattleMech":
		case "QuadMech":
		case "Biped":
		case "Quad":
		case "Leg":
		case "Industrial":
		case "Omni":
			return AbstractUnitRecord.MOVEMENT_LEG;
		case "Tracked":
			return AbstractUnitRecord.MOVEMENT_TRACKED;
		case "Wheeled":
			return AbstractUnitRecord.MOVEMENT_WHEELED;
		case "Hover":
			return AbstractUnitRecord.MOVEMENT_HOVER;
		case "WiGE":
			return AbstractUnitRecord.MOVEMENT_WIGE;
		case "VTOL":
			return AbstractUnitRecord.MOVEMENT_VTOL;
		case "Naval":
		case "Hydrofoil":
			return AbstractUnitRecord.MOVEMENT_NAVAL;
		case "Submarine":
		case "UMU":
			return AbstractUnitRecord.MOVEMENT_UW;
		case "Jump":
			return AbstractUnitRecord.MOVEMENT_JUMP;
		case "Motorized":
			return AbstractUnitRecord.MOVEMENT_MOTORIZED;
		case "Aerodyne":
		case "Spheroid":
			return AbstractUnitRecord.MOVEMENT_AEROSPACE;
		case "None":
			return MOVEMENT_NONE;
		default:
			System.err.println("Could not find movement constant for " + subtype);
			return 0;
		}
	}
	
	public ChassisRecord createChassisRec() {
		ChassisRecord retVal = new ChassisRecord(chassis);
		retVal.introYear = introYear;
		retVal.omni = omni;
		retVal.unitType = unitType;
		retVal.movementType = movementType;
		retVal.addModel(this);
		return retVal;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}
	
	public int getWeightClass() {
		return weightClass;
	}
	
	public boolean isClan() {
		return clan;
	}
	
	public boolean isSL() {
		return starLeague;
	}
	
	public ArrayList<MissionRole> getRoles() {
		return roles;
	}
	public ArrayList<String> getDeployedWith() {
		return deployedWith;
	}
	public ArrayList<String> getRequiredUnits() {
		return requiredUnits;
	}
	public ArrayList<String> getExcludedFactions() {
		return excludedFactions;
	}
	public int getNetwork() {
		return network;
	}
	public void setNetwork(int network) {
		this.network = network;
	}
	public double getFlak() {
		return flak;
	}
	public void setFlak(double flak) {
		this.flak = flak;
	}
	
	public double getLongRange() {
		return longRange;
	}
	
	public int getSpeed() {
		return speed;
	}
	
	public double getAmmoRequirement() {
		return ammoRequirement;
	}
	
	public boolean hasFlamer() {
		return flamer;
	}
	
	public boolean hasAPWeapons() {
		return apWeapons;
	}

	public MechSummary getMechSummary() {
		return MechSummaryCache.getInstance().getMech(getKey());
	}
	
	public void setRoles(String str) {
		roles.clear();
		String[] fields = str.split(",");
		for (String role : fields) {
			roles.add(MissionRole.parseRole(role));
		}
	}
	
	public void setRequiredUnits(String str) {
		String [] subfields = str.split(",");
		for (String unit : subfields) {
			if (unit.startsWith("req:")) {
				requiredUnits.add(unit.replace("req:", ""));
			} else {
				deployedWith.add(unit);
			}
		}		
	}

	public void setExcludedFactions(String str) {
		excludedFactions.clear();
		String[] fields = str.split(",");
		for (String faction : fields) {
			excludedFactions.add(faction);
		}
	}
	
	public boolean factionIsExcluded(FactionRecord fRec) {
		return excludedFactions.contains(fRec.getKey());
	}
	
	public boolean factionIsExcluded(String faction, String subfaction) {
		if (subfaction == null) {
			return excludedFactions.contains(faction);
		} else {
			return excludedFactions.contains(faction + "." + subfaction);
		}
	}
	
	public String getRolesAsString() {
		String retVal = "";
		for (int i = 0; i < roles.size(); i++) {
				if (roles.get(i) != null) {
				retVal += roles.get(i).toString();
				if (i < roles.size() - 1) {
					retVal += ",";
				}
			}
		}
		return retVal;
	}

	public String getDeployedWithAsString() {
		String retVal = "";
		for (int i = 0; i < deployedWith.size(); i++) {
			retVal += deployedWith.get(i);
			if (i < deployedWith.size() - 1 || requiredUnits.size() > 0) {
				retVal += ",";
			}
		}
		for (int i = 0; i < requiredUnits.size(); i++) {
			retVal += "req:" + requiredUnits.get(i);
			if (i < requiredUnits.size() - 1) {
				retVal += ",";
			}
		}
		return retVal;
	}
	
	public String getExcludedFactionsAsString() {
		String retVal = "";
		for (int i = 0; i < excludedFactions.size(); i++) {
			retVal += excludedFactions.get(i);
			if (i < excludedFactions.size() - 1) {
				retVal += ",";
			}
		}
		return retVal;
	}
	
	@Override
	public String getKey() {
		if (model != null && model.length() > 0) {
			return chassis + " " + model;
		} else {
			return chassis;
		}		
	}
	
	public Boolean isQuad() {
		if (quad == null) {
			MechSummary ms = MechSummaryCache.getInstance().getMech(getChassis() + " " + getModel());
			if (ms != null) {
				try {
					Entity en = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
					if (en != null) {
						quad = en instanceof megamek.common.QuadMech ||
								(en instanceof megamek.common.BattleArmor &&
										((megamek.common.BattleArmor)en).getChassisType() == 
										megamek.common.BattleArmor.CHASSIS_TYPE_QUAD);
						for (ModelRecord mRec : RATGenerator.getInstance().getChassisRecord(getChassisKey()).getModels()) {
							mRec.quad = quad;
						}
					}
				} catch (EntityLoadingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return quad;
	}
}

