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
import java.util.EnumSet;
import java.util.Set;

import megamek.common.AmmoType;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.MechSummary;
import megamek.common.MiscType;
import megamek.common.UnitType;
import megamek.common.WeaponType;

/**
 * Specific unit variants; analyzes equipment to determine suitability for certain types
 * of missions in addition to what is formally declared in the data files.
 * 
 * @author Neoancient
 *
 */
public class ModelRecord extends AbstractUnitRecord {
	public static final int NETWORK_NONE = 0;
	public static final int NETWORK_C3_SLAVE = 1;
	public static final int NETWORK_BA_C3 = 1;
	public static final int NETWORK_C3_MASTER = 1 << 1;
	public static final int NETWORK_C3I = 1 << 2;
	public static final int NETWORK_NAVAL_C3 = 1 << 2;
	public static final int NETWORK_NOVA = 1 << 3;
	
	public static final int NETWORK_BOOSTED = 1 << 4;
	public static final int NETWORK_COMPANY_COMMAND = 1 << 5;
	
	public static final int NETWORK_BOOSTED_SLAVE = NETWORK_C3_SLAVE | NETWORK_BOOSTED;
	public static final int NETWORK_BOOSTED_MASTER = NETWORK_C3_MASTER | NETWORK_BOOSTED;

	private MechSummary mechSummary;
	private boolean starLeague;
	private int weightClass;
	private EntityMovementMode movementMode;
	private EnumSet<MissionRole> roles;
	private ArrayList<String> deployedWith;
	private ArrayList<String> requiredUnits;
	private ArrayList<String> excludedFactions;
	private int networkMask;
	private double flak; //proportion of weapon BV that can fire flak ammo
	private double longRange; //proportion of weapon BV with range >= 20 hexes
	private int speed;
	private double ammoRequirement; //used to determine suitability for raider role
	private boolean incendiary; //used to determine suitability for incindiary role
	private boolean apWeapons; //used to determine suitability for anti-infantry role
	
	private boolean mechanizedBA;

	public ModelRecord(String chassis, String model) {
		super(chassis);
		roles = EnumSet.noneOf(MissionRole.class);
		deployedWith = new ArrayList<String>();
		requiredUnits = new ArrayList<String>();
		excludedFactions = new ArrayList<String>();
		networkMask = NETWORK_NONE;
		flak = 0.0;
		longRange = 0.0;
	}
	
	public ModelRecord(MechSummary ms) {
		this(ms.getChassis(), ms.getModel());
		mechSummary = ms;
		unitType = parseUnitType(ms.getUnitType());
		introYear = ms.getYear();
		if (unitType == UnitType.MEK) {
			//TODO: id quads and tripods
			movementMode = EntityMovementMode.BIPED;
		} else {
			movementMode = EntityMovementMode.getMode(ms.getUnitSubType().toLowerCase());
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
    			switch (((megamek.common.weapons.Weapon)eq).getAmmoType()) {
    				case AmmoType.T_AC_LBX:
    				case AmmoType.T_HAG:
    				case AmmoType.T_SBGAUSS:
        				flakBV += eq.getBV(null) * ms.getEquipmentQuantities().get(i);
    			}
    			if (eq.hasFlag(WeaponType.F_ARTILLERY)) {
    				flakBV += eq.getBV(null) * ms.getEquipmentQuantities().get(i) / 2.0;
    			}
        		if (eq.hasFlag(WeaponType.F_FLAMER) || eq.hasFlag(WeaponType.F_INFERNO)) {
        			incendiary = true;
        			apWeapons = true;
        		}
        		incendiary |= ((WeaponType)eq).getAmmoType() == AmmoType.T_SRM
        				|| ((WeaponType)eq).getAmmoType() == AmmoType.T_MRM;
        		
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
        		if (eq.hasFlag(WeaponType.F_TAG)) {
        			roles.add(MissionRole.SPOTTER);
        		}
        		if (eq.hasFlag(WeaponType.F_C3M)) {
   					networkMask |= NETWORK_C3_MASTER;
   					if (ms.getEquipmentQuantities().get(i) > 1) {
   						networkMask |= NETWORK_COMPANY_COMMAND;
   					}
        		}
        		if (eq.hasFlag(WeaponType.F_C3MBS)) {
					networkMask |= NETWORK_BOOSTED_MASTER;
   					if (ms.getEquipmentQuantities().get(i) > 1) {
   						networkMask |= NETWORK_COMPANY_COMMAND;
    				}        			
        		}
    		} else if (eq.hasFlag(MiscType.F_UMU)){
   				movementMode = EntityMovementMode.BIPED_SWIM;
    		} else if (eq.hasFlag(MiscType.F_C3S)) {
    			networkMask |= NETWORK_C3_SLAVE;
    		} else if (eq.hasFlag(MiscType.F_C3I)) {
    			networkMask |= NETWORK_C3I;
    		} else if (eq.hasFlag(MiscType.F_C3SBS)) {
    			networkMask |= NETWORK_BOOSTED_SLAVE;
    		} else if (eq.hasFlag(MiscType.F_NOVA)) {
    			networkMask |= NETWORK_NOVA;
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
	
	public String getModel() {
		return mechSummary.getModel();
	}

	public int getWeightClass() {
		return weightClass;
	}
	
	public EntityMovementMode getMovementMode() {
		return movementMode;
	}
	
	public boolean isClan() {
		return clan;
	}
	
	public boolean isSL() {
		return starLeague;
	}
	
	public Set<MissionRole> getRoles() {
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
	public int getNetworkMask() {
		return networkMask;
	}
	public void setNetwork(int network) {
		this.networkMask = network;
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
	
	public boolean hasIncendiaryWeapon() {
		return incendiary;
	}
	
	public boolean hasAPWeapons() {
		return apWeapons;
	}
	
	public MechSummary getMechSummary() {
		return mechSummary;
	}
	
	public void setRoles(String str) {
		roles.clear();
		String[] fields = str.split(",");
		for (String role : fields) {
			MissionRole mr = MissionRole.parseRole(role);
			if (mr != null) {
				roles.add(mr);
			} else {
				System.err.println("Could not parse mission role for "
						+ getChassis() + " " + getModel() + ": " + role);
			}
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
	
	@Override
	public String getKey() {
		return mechSummary.getName();
	}
	
	public boolean canDoMechanizedBA() {
		return mechanizedBA;
	}
	
	public void setMechanizedBA(boolean mech) {
		mechanizedBA = mech;
	}
}

