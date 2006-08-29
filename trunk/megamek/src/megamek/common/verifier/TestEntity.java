/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

/*
 * Author: Reinhard Vicinus
 */

package megamek.common.verifier;

import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.TechConstants;
import megamek.common.CriticalSlot;
import megamek.common.Mounted;
import megamek.common.EquipmentType;
import megamek.common.WeaponType;
import megamek.common.AmmoType;
import megamek.common.MiscType;
import megamek.common.util.StringUtil;
import megamek.common.Engine;

import java.util.Enumeration;
import java.lang.StringBuffer;

public abstract class TestEntity implements TestEntityOption {
	
	public final static float CEIL_TON = 1.0f;
	public final static float CEIL_HALFTON = 2.0f;
	public final static float CEIL_QUARTERTON = 4.0f;
	public final static String[] MOVEMENT_CHASSIS_NAMES = { "Building",
			"Biped Mech", "Quad Mech", "Tracked Vehicle", "Wheeled Vehicle",
			"Hovercraft", "VTOL", "Naval Vehicle", "Hydrofoil Vehicle",
			"Submarine" };

	protected Engine engine = null;
	protected Armor armor = null;
	protected Structure structure = null;
	private TestEntityOption options = null;

    public abstract Entity getEntity();
	public abstract boolean isTank();
	public abstract boolean isMech();
	public abstract float getWeightControls();
	public abstract float getWeightMisc();
	public abstract int getWeightHeatSinks();
	public abstract boolean hasDoubleHeatSinks();
	public abstract int getCountHeatSinks();
	public abstract String printWeightMisc();
	public abstract String printWeightControls();
	public abstract boolean correctEntity(StringBuffer buff);
	public abstract boolean correctEntity(StringBuffer buff, boolean ignoreAmmo);

	public abstract StringBuffer printEntity();
	public abstract String getName();
	public String fileString = null; // where the unit came from

	public TestEntity(TestEntityOption options, Engine engine, Armor armor,
			Structure structure) {
		this.options = options;
		this.engine = engine;
		this.armor = armor;
		this.structure = structure;
	}

	public boolean isClan() {
		return getEntity().isClan();
	}

	public boolean isClanArmor() {
		return getEntity().isClanArmor();
	}

	public float getWeight() {
		return getEntity().getWeight();
	}

	public int getTotalOArmor() {
		return getEntity().getTotalOArmor();
	}

	public String getLocationAbbr(int location) {
		return getEntity().getLocationAbbr(location);
	}

	public float getWeightCeilingEngine() {
		return options.getWeightCeilingEngine();
	}

	public float getWeightCeilingStructure() {
		return options.getWeightCeilingStructure();
	}

	public float getWeightCeilingArmor() {
		return options.getWeightCeilingArmor();
	}

	public float getWeightCeilingControls() {
		return options.getWeightCeilingControls();
	}

	public float getWeightCeilingWeapons() {
		return options.getWeightCeilingWeapons();
	}

	public float getWeightCeilingTargComp() {
		return options.getWeightCeilingTargComp();
	}

	public float getWeightCeilingGyro() {
		return options.getWeightCeilingGyro();
	}

	public float getWeightCeilingTurret() {
		return options.getWeightCeilingTurret();
	}

	public float getWeightCeilingPowerAmp() {
		return options.getWeightCeilingPowerAmp();
	}

	public float getMaxOverweight() {
		return options.getMaxOverweight();
	}

	public boolean showOverweightedEntity() {
		return options.showOverweightedEntity();
	}

	public float getMinUnderweight() {
		return options.getMinUnderweight();
	}

	public boolean showUnderweightedEntity() {
		return options.showUnderweightedEntity();
	}

	public boolean showCorrectArmor() {
		return options.showCorrectArmor();
	}

	public boolean showCorrectCritical() {
		return options.showCorrectCritical();
	}

	public boolean showFailedEquip() {
		return options.showFailedEquip();
	}

	public boolean ignoreFailedEquip(String name) {
		return options.ignoreFailedEquip(name);
	}

	public boolean skip() {
		return options.skip();
	}

	public int getTargCompCrits() {
		return options.getTargCompCrits();
	}

	public int getPrintSize() {
		return options.getPrintSize();
	}

	protected static float ceil(float f, float type) {
		return (float) Math.ceil(f * type) / type;
	}

	public static float ceilMaxHalf(float f, float type) {
		if (type == CEIL_TON)
			return ceil(f, CEIL_HALFTON);
		return ceil(f, type);
	}

	protected static String makeWeightString(float weight) {
		return (weight < 100 ? " " : "") + (weight < 10 ? " " : "")
				+ Float.toString(weight)
				+ ((Math.ceil(weight * 10) == weight * 10) ? "0" : "");
	}

	private boolean hasMASC() {
		if (getEntity() instanceof Mech)
			return ((Mech) getEntity()).hasMASC();
		return false;
	}

	public String printShortMovement() {
		return "Movement: "
				+ Integer.toString(getEntity().getOriginalWalkMP())
				+ "/"
				+ Integer.toString((int) Math.ceil(getEntity()
						.getOriginalWalkMP() * 1.5))
				+ (hasMASC() ? "("
						+ Integer.toString(getEntity().getOriginalWalkMP() * 2)
						+ ")" : "")
				+ (getEntity().getOriginalJumpMP() != 0 ? "/"
						+ Integer.toString(getEntity().getOriginalJumpMP())
						: "") + "\n";
	}

	public String printWeightHeatSinks() {
		return StringUtil.makeLength(
				"Heat Sinks: "
						+ Integer.toString(getCountHeatSinks())
						+ (hasDoubleHeatSinks() ? " ["
								+ Integer.toString(2 * getCountHeatSinks())
								+ "]" : ""), getPrintSize() - 5)
				+ makeWeightString(getWeightHeatSinks()) + "\n";
	}

	public String printWeightEngine() {
		return StringUtil.makeLength("Engine: " + engine.getEngineName(),
				getPrintSize() - 5)
				+ makeWeightString(getWeightEngine()) + "\n";
	}

	public float getWeightEngine() {
		return engine.getWeightEngine(getWeightCeilingEngine());
	}

	public String printWeightStructure() {
		return StringUtil.makeLength("Structure: "
				+ Integer.toString(getEntity().getTotalOInternal()) + " "
				+ structure.getShortName(), getPrintSize() - 5)
				+ makeWeightString(getWeightStructure()) + "\n";
	}

	public float getWeightStructure() {
		return structure.getWeightStructure(getWeight(),
				getWeightCeilingStructure());
	}

	public String printWeightArmor() {
		return StringUtil.makeLength("Armor: "
				+ Integer.toString(getTotalOArmor()) + " "
				+ armor.getShortName(), getPrintSize() - 5)
				+ makeWeightString(getWeightArmor()) + "\n";
	}

	public float getWeightArmor() {
		return armor.getWeightArmor(getTotalOArmor(), getWeightCeilingArmor());
	}

	public float getWeightMiscEquip(MiscType mt) {
		if (mt.hasFlag(MiscType.F_HEAT_SINK)
				|| mt.hasFlag(MiscType.F_DOUBLE_HEAT_SINK))
			return 0f;
		if (mt.hasFlag(MiscType.F_FERRO_FIBROUS))
			return 0f;
		if (mt.hasFlag(MiscType.F_ENDO_STEEL))
			return 0f;

		if (mt.hasFlag(MiscType.F_JUMP_JET)) {
			return mt.getTonnage(getEntity());
		} else if (mt.hasFlag(MiscType.F_CLUB)
				&& (mt.hasSubType(MiscType.S_HATCHET) || mt
						.hasSubType(MiscType.S_MACE_THB))) {
			return ceil(getWeight() / 15.0f, getWeightCeilingWeapons());
		} else if (mt.hasFlag(MiscType.F_CLUB)
				&& mt.hasSubType(MiscType.S_SWORD)) {
			return ceilMaxHalf(getWeight() / 20.0f, getWeightCeilingWeapons());
		} else if (mt.hasFlag(MiscType.F_CLUB)
				&& mt.hasSubType(MiscType.S_MACE)) {
			return ceilMaxHalf(getWeight() / 10.0f, getWeightCeilingWeapons());
		} else if (mt.hasFlag(MiscType.F_CLUB)
				&& mt.hasSubType(MiscType.S_PILE_DRIVER)) {
			return ceilMaxHalf(10, getWeightCeilingWeapons());
		} else if (mt.hasFlag(MiscType.F_CLUB)
				&& mt.hasSubType(MiscType.S_CHAINSAW)) {
			return ceilMaxHalf(5, getWeightCeilingWeapons());
		} else if (mt.hasFlag(MiscType.F_CLUB)
				&& mt.hasSubType(MiscType.S_DUAL_SAW)) {
			return ceilMaxHalf(7, getWeightCeilingWeapons());
		} else if (mt.hasFlag(MiscType.F_CLUB)
				&& mt.hasSubType(MiscType.S_BACKHOE)) {
			return ceilMaxHalf(5, getWeightCeilingWeapons());
		} else if (mt.hasFlag(MiscType.F_MASC)) {
			if (mt.getInternalName().equals("ISMASC"))
				return Math.round(getWeight() / 20.0f);
			else if (mt.getInternalName().equals("CLMASC"))
				return Math.round(getWeight() / 25.0f);
		} else if (mt.hasFlag(MiscType.F_TARGCOMP)) {
			float fTons = 0.0f;
			for (Mounted mo : getEntity().getWeaponList()) {
				WeaponType wt = (WeaponType) mo.getType();
				if (wt.hasFlag(WeaponType.F_DIRECT_FIRE))
					fTons += wt.getTonnage(getEntity());
			}
			if (mt.getInternalName().equals("ISTargeting Computer"))
				return ceil(fTons / 4.0f, getWeightCeilingTargComp());
			else if (mt.getInternalName().equals("CLTargeting Computer"))
				return ceil(fTons / 5.0f, getWeightCeilingTargComp());
		} else if (mt.hasFlag(MiscType.F_VACUUM_PROTECTION)) {
			return Math.round(getWeight() / 10.0f);
		} else
			return mt.getTonnage(getEntity());
		return 0f;
	}

	public float getWeightMiscEquip() {
		float weightSum = 0.0f;
		for (Mounted m : getEntity().getMisc()) {
			MiscType mt = (MiscType) m.getType();
			weightSum += getWeightMiscEquip(mt);
		}
		return weightSum;
	}

	public StringBuffer printMiscEquip() {
		return printMiscEquip(new StringBuffer());
	}

	public StringBuffer printMiscEquip(StringBuffer buff) {
		return printMiscEquip(buff, 20, getPrintSize());
	}

	public StringBuffer printMiscEquip(StringBuffer buff, int posLoc,
			int posWeight) {
		for (Mounted m : getEntity().getMisc()) {
			MiscType mt = (MiscType) m.getType();

			if (m.getLocation() == Entity.LOC_NONE)
				continue;

			if (getWeightMiscEquip(mt) == 0f)
				continue;

			buff.append(StringUtil.makeLength(mt.getName(), 20));
			buff.append(
					StringUtil.makeLength(getLocationAbbr(m.getLocation()),
							getPrintSize() - 5 - 20)).append(
					makeWeightString(getWeightMiscEquip(mt)));
			buff.append("\n");
		}
		return buff;
	}

	public float getWeightWeapon() {
		float weight = 0.0f;
		for (Mounted m : getEntity().getWeaponList()) {
			WeaponType mt = (WeaponType) m.getType();
			weight += mt.getTonnage(getEntity());
		}
		return weight;
	}

	public StringBuffer printWeapon() {
		return printWeapon(new StringBuffer());
	}

	public StringBuffer printWeapon(StringBuffer buff) {
		return printWeapon(buff, 20, getPrintSize());
	}

	public StringBuffer printWeapon(StringBuffer buff, int posLoc, int posWeight) {
		for (Mounted m : getEntity().getWeaponList()) {
			WeaponType mt = (WeaponType) m.getType();

			// Don't think this can happen, but ...
			if (m.getLocation() == Entity.LOC_NONE)
				continue;

			buff.append(StringUtil.makeLength(mt.getName(), 20));
			buff.append(
					StringUtil.makeLength(getLocationAbbr(m.getLocation()),
							getPrintSize() - 5 - 20)).append(
					makeWeightString(mt.getTonnage(getEntity()))).append("\n");
		}
		return buff;
	}

	public float getWeightAmmo() {
		float weight = 0.0f;
		for (Mounted m : getEntity().getAmmo()) {

			// One Shot Ammo
			if (m.getLocation() == Entity.LOC_NONE)
				continue;

			AmmoType mt = (AmmoType) m.getType();
			weight += mt.getTonnage(getEntity());
		}
		return weight;
	}

	public StringBuffer printAmmo() {
		return printAmmo(new StringBuffer());
	}

	public StringBuffer printAmmo(StringBuffer buff) {
		return printAmmo(buff, 20, getPrintSize());
	}

	public StringBuffer printAmmo(StringBuffer buff, int posLoc, int posWeight) {
		for (Mounted m : getEntity().getAmmo()) {
			AmmoType mt = (AmmoType) m.getType();

			// Don't think this can happen, but ...
			if (m.getLocation() == Entity.LOC_NONE)
				continue;

			buff.append(StringUtil.makeLength(mt.getName(), 20));
			buff.append(
					StringUtil.makeLength(getLocationAbbr(m.getLocation()),
							getPrintSize() - 5 - 20)).append(
					makeWeightString(mt.getTonnage(getEntity()))).append("\n");
		}
		return buff;
	}

	public String printLocations() {
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < getEntity().locations(); i++) {
			String locationName = getEntity().getLocationName(i);
			buff.append(locationName + ":");
			buff.append("\n");
			for (int j = 0; j < getEntity().getNumberOfCriticals(i); j++) {
				CriticalSlot slot = getEntity().getCritical(i, j);
				if (slot == null) {
					buff.append(Integer.toString(j) + ". -Emtpy-");
					buff.append("\n");
				} else if (slot.getType() == CriticalSlot.TYPE_SYSTEM) {
					if (isMech()) {
						buff.append(Integer.toString(j));
						buff.append(". ");
						buff.append(((Mech) getEntity()).getSystemName(slot
								.getIndex()));
						buff.append("\n");
					} else {
						buff.append(Integer.toString(j)
								+ ". UNKNOWN SYSTEM NAME");
						buff.append("\n");
					}
				} else if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
					Mounted m = getEntity().getEquipment(slot.getIndex());

					buff.append(Integer.toString(j) + ". "
							+ m.getType().getInternalName());
					buff.append("\n");
				}
			}
		}
		return buff.toString();
	}

	public int calcMiscCrits(MiscType mt) {
		if (mt.hasFlag(MiscType.F_CLUB)
				&& (mt.hasSubType(MiscType.S_HATCHET)
						|| mt.hasSubType(MiscType.S_SWORD) || mt
						.hasSubType(MiscType.S_MACE_THB))) {
			return (int) Math.ceil(getWeight() / 15.0);
		} else if (mt.hasFlag(MiscType.F_CLUB)
				&& mt.hasSubType(MiscType.S_MACE)) {
			return (int) Math.ceil(getWeight() / 10.0);
		} else if (mt.hasFlag(MiscType.F_CLUB)
				&& mt.hasSubType(MiscType.S_PILE_DRIVER)) {
			return 8;
		} else if (mt.hasFlag(MiscType.F_CLUB)
				&& mt.hasSubType(MiscType.S_CHAINSAW)) {
			return 5;
		} else if (mt.hasFlag(MiscType.F_CLUB)
				&& mt.hasSubType(MiscType.S_DUAL_SAW)) {
			return 7;
		} else if (mt.hasFlag(MiscType.F_CLUB)
				&& mt.hasSubType(MiscType.S_BACKHOE)) {
			return 6;
		} else if (mt.hasFlag(MiscType.F_MASC)) {
			if (mt.getInternalName().equals("ISMASC"))
				return Math.round(getWeight() / 20.0f);
			else if (mt.getInternalName().equals("CLMASC"))
				return Math.round(getWeight() / 25.0f);
		} else if (mt.hasFlag(MiscType.F_TARGCOMP)) {
			float fTons = 0.0f;
			for (Mounted mo : getEntity().getWeaponList()) {
				WeaponType wt = (WeaponType) mo.getType();
				if (wt.hasFlag(WeaponType.F_DIRECT_FIRE))
					fTons += wt.getTonnage(getEntity());
			}
			float weight = 0.0f;
			if (mt.getInternalName().equals("ISTargeting Computer"))
				weight = ceil(fTons / 4.0f, getWeightCeilingTargComp());
			else if (mt.getInternalName().equals("CLTargeting Computer"))
				weight = ceil(fTons / 5.0f, getWeightCeilingTargComp());
			switch (getTargCompCrits()) {
			case CEIL_TARGCOMP_CRITS:
				return (int) Math.ceil(weight);
			case ROUND_TARGCOMP_CRITS:
				return Math.round(weight);
			case FLOOR_TARGCOMP_CRITS:
				return (int) Math.floor(weight);
			}
		} else if (EquipmentType.getArmorTypeName(
				EquipmentType.T_ARMOR_FERRO_FIBROUS).equals(
				mt.getInternalName())) {
			if (isClanArmor())
				return 7;
			return 14;
		} else if (EquipmentType.getArmorTypeName(
				EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO).equals(
				mt.getInternalName())) {
			return 16;
		} else if (EquipmentType.getArmorTypeName(
				EquipmentType.T_ARMOR_LIGHT_FERRO).equals(mt.getInternalName())) {
			return 7;
		} else if (EquipmentType.getArmorTypeName(
				EquipmentType.T_ARMOR_HEAVY_FERRO).equals(mt.getInternalName())) {
			return 21;
		} else if (EquipmentType.getStructureTypeName(
				EquipmentType.T_STRUCTURE_ENDO_STEEL).equals(
				mt.getInternalName())) {
			if (isClan())
				return 7;
			return 14;
		} else if (EquipmentType.getStructureTypeName(
				EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE).equals(
				mt.getInternalName())) {
			return 16;
		}
		return mt.getCriticals(getEntity());
	}

	public float calculateWeight() {
		float weight = 0;
		weight += getWeightEngine();
		weight += getWeightStructure();
		weight += getWeightControls();
		weight += getWeightHeatSinks();
		weight += getWeightArmor();
		weight += getWeightMisc();

		weight += getWeightMiscEquip();
		weight += getWeightWeapon();
		weight += getWeightAmmo();

		weight += getWeightCarryingSpace();
		return weight;
	}

	public String printWeightCalculation() {
		return printWeightEngine() + printWeightStructure()
				+ printWeightControls() + printWeightHeatSinks()
				+ printWeightArmor() + printWeightMisc()
				+ printWeightCarryingSpace() + "Equipment:\n"
				+ printMiscEquip() + printWeapon() + printAmmo();
	}

	public boolean correctWeight(StringBuffer buff) {
		return correctWeight(buff, showOverweightedEntity(),
				showUnderweightedEntity());
	}

	public boolean correctWeight(StringBuffer buff, boolean showO, boolean showU) {
		float weightSum = calculateWeight();
		float weight = getWeight();

		if (showO && weight + getMaxOverweight() < weightSum) {
			buff.append("Weight: ").append(calculateWeight()).append(
					" is greater then ").append(getWeight()).append("\n");
			// buff.append(printWeightCalculation()).append("\n");
			return false;
		}
		if (showU && weight - getMinUnderweight() > weightSum) {
			buff.append("Weight: ").append(calculateWeight()).append(
					" is lesser then ").append(getWeight()).append("\n");
			// buff.append(printWeightCalculation()).append("\n");
			return false;
		}
		return true;
	}

	public boolean hasIllegalTechLevels(StringBuffer buff) {
		return hasIllegalTechLevels(buff, true);
	}

	public boolean hasIllegalTechLevels(StringBuffer buff, boolean ignoreAmmo) {
		boolean retVal = false;
		int eTechLevel = getEntity().getTechLevel();
		for (Mounted mounted : getEntity().getEquipment()) {
			EquipmentType nextE = mounted.getType();
			if ((ignoreAmmo) && (nextE instanceof AmmoType)) {
				continue;
			} else if (!(TechConstants.isLegal(eTechLevel,
					nextE.getTechLevel(), true))) {
				if (!retVal)
					buff.append("Equipment illegal at unit's tech level:\n");
				retVal = true;
				buff.append(nextE.getName()).append("\n");
			}
		}
		return retVal;
	}

	public boolean hasFailedEquipment(StringBuffer buff) {
		boolean hasFailedEquipment = false;
		for (Enumeration e = getEntity().getFailedEquipment(); e
				.hasMoreElements();) {
			String name = (String) e.nextElement();
			if (!ignoreFailedEquip(name)) {
				if (!hasFailedEquipment)
					buff.append("Equipment that Failed to Load:\n");
				buff.append(name).append("\n");
				hasFailedEquipment = true;
			}
		}

		return hasFailedEquipment;
	}

	public StringBuffer printFailedEquipment(StringBuffer buff) {
		if (getEntity().getFailedEquipment().hasMoreElements())
			buff.append("Equipment that Failed to Load:\n");
		for (Enumeration e = getEntity().getFailedEquipment(); e
				.hasMoreElements();)
			buff.append(e.nextElement()).append("\n");
		return buff;
	}

	public int getWeightCarryingSpace() {
		return getEntity().getTroopCarryingSpace();
	}

	public String printWeightCarryingSpace() {
		if (getEntity().getTroopCarryingSpace() != 0)
			return StringUtil.makeLength("Carrying Capacity:",
					getPrintSize() - 5)
					+ makeWeightString(getEntity().getTroopCarryingSpace())
					+ "\n";
		return "";
	}

	public String printArmorLocation(int loc) {
		if (getEntity().hasRearArmor(loc))
			return StringUtil.makeLength(
					getEntity().getLocationAbbr(loc) + ":", 5)
					+ StringUtil.makeLength(getEntity().getOInternal(loc), 4)
					+ StringUtil.makeLength(getEntity().getOArmor(loc), 3)
					+ " / "
					+ StringUtil
							.makeLength(getEntity().getOArmor(loc, true), 2);
		return StringUtil.makeLength(
				getEntity().getLocationAbbr(loc) + ":", 5)
				+ StringUtil.makeLength(getEntity().getOInternal(loc), 4)
				+ StringUtil.makeLength(getEntity().getOArmor(loc), 6)
				+ "  ";
	}

	public String printArmorPlacement() {
		StringBuffer buff = new StringBuffer();
		buff.append("Armor Placement:\n");
		for (int loc = 0; loc < getEntity().locations(); loc++) {
			buff.append(printArmorLocation(loc)).append("\n");
		}
		return buff.toString();
	}

	public String printTechLevel() {
		return "Chassis: "
				+ MOVEMENT_CHASSIS_NAMES[getEntity().getMovementMode()] + " - "
				+ TechConstants.getLevelName(getEntity().getTechLevel()) + " ("
				+ Integer.toString(getEntity().getYear()) + ")\n";
	}

} // End class TestEntity

class Armor {
	public final static int CLAN_ARMOR = 0x01;

	private int armorType;

	private int armorFlags;

	public Armor(int armorType, int armorFlags) {
		this.armorType = armorType;
		this.armorFlags = armorFlags;
	}

	public float getWeightArmor(int totalOArmor, float roundWeight) {
		return getWeightArmor(armorType, armorFlags, totalOArmor, roundWeight);
	}

	public static float getWeightArmor(int armorType, int armorFlags,
			int totalOArmor, float roundWeight) {
		float points = totalOArmor;
		if (armorType == EquipmentType.T_ARMOR_FERRO_FIBROUS
				|| armorType == EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO) {
			if ((armorFlags & CLAN_ARMOR) != 0)
				points /= 1.2f;
			else
				points /= 1.12f;
		} else if (armorType == EquipmentType.T_ARMOR_LIGHT_FERRO) {
			points /= 1.06f;
		} else if (armorType == EquipmentType.T_ARMOR_HEAVY_FERRO) {
			points /= 1.24f;
		}
		float pointsPerTon = 16.0f;
		if (armorType == EquipmentType.T_ARMOR_HARDENED)
			pointsPerTon = 8.0f;
		float armorWeight = Math.round(points) / pointsPerTon;
		return TestEntity.ceilMaxHalf(armorWeight, roundWeight);
	}

	public String getShortName() {
		return "(" + EquipmentType.getArmorTypeName(armorType) + ")";
	}

} // end class Armor

class Structure {
	public final static int CLAN_STRUCTURE = 0x01;

	private int structureType;

	private int structureFlags;

	public Structure(int structureType, int structureFlags) {
		this.structureType = structureType;
		this.structureFlags = structureFlags;
	}

	public float getWeightStructure(float weight, float roundWeight) {
		return getWeightStructure(structureType, weight, roundWeight);
	}

	public static float getWeightStructure(int structureType, float weight,
			float roundWeight) {
		if (structureType == EquipmentType.T_STRUCTURE_ENDO_STEEL) {
			return TestEntity.ceilMaxHalf(weight / 20.0f, roundWeight);
		} else if (structureType == EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE) {
			return TestEntity.ceilMaxHalf(weight / 20.0f, roundWeight);
		} else if (structureType == EquipmentType.T_STRUCTURE_REINFORCED) {
			return TestEntity.ceilMaxHalf(weight / 5.0f, roundWeight);
		} else if (structureType == EquipmentType.T_STRUCTURE_COMPOSITE) {
			return TestEntity.ceilMaxHalf(weight / 20.0f, roundWeight);
		}
		return weight / 10.0f;
	}

	public String getShortName() {
		return "(" + EquipmentType.getStructureTypeName(structureType) + ")";
	}

} // End class Structure

