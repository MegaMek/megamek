/*
 * MegaMek - Copyright (C) 2018 - The MegaMek Team
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
package megamek.common.templates;

import java.text.NumberFormat;
import java.util.StringJoiner;

import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LandAirMech;
import megamek.common.Mech;
import megamek.common.QuadVee;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestMech;

/**
 * Creates a TRO template model for BattleMechs, OmniMechs, and IndustrialMechs of all leg configurations.
 * 
 * @author Neoancient
 *
 */
public class MechTROView extends TROView {
	
	private final Mech mech;
	
	public MechTROView(Mech mech) {
		this.mech = mech;
	}
	
	@Override
	protected String getTemplateFileName(boolean html) {
		if (html) {
			return "mech.ftlh";
		}
		return "mech.ftl";
	}

	@Override
	protected void initModel(EntityVerifier verifier) {
		setModelData("formatArmorRow", new FormatTableRowMethod(new int[] { 20, 10, 10},
				new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER }));
		addBasicData(mech);
		addArmorAndStructure();
		int nameWidth = addEquipment(mech);
		setModelData("formatEquipmentRow", new FormatTableRowMethod(new int[] { nameWidth, 12, 8, 10, 8},
				new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER,
						Justification.CENTER, Justification.CENTER}));
		addFluff();
		mech.setConversionMode(0);
		setModelData("isOmni", mech.isOmni());
		setModelData("isQuad", mech.hasETypeFlag(Entity.ETYPE_QUAD_MECH));
		setModelData("isTripod", mech.hasETypeFlag(Entity.ETYPE_TRIPOD_MECH));
		TestMech testMech = new TestMech(mech, verifier.mechOption, null);
		setModelData("structureName", mech.getStructureType() == EquipmentType.T_STRUCTURE_STANDARD?
				"" : EquipmentType.getStructureTypeName(mech.getStructureType()));
		setModelData("isMass", NumberFormat.getInstance().format(testMech.getWeightStructure()));
		setModelData("engineName", stripNotes(mech.getEngine().getEngineName()));
		setModelData("engineMass", NumberFormat.getInstance().format(testMech.getWeightEngine()));
		setModelData("walkMP", mech.getWalkMP());
		setModelData("runMP", mech.getRunMPasString());
		setModelData("jumpMP", mech.getJumpMP());
		setModelData("hsType", mech.getHeatSinkTypeName());
		setModelData("hsCount", mech.hasDoubleHeatSinks()?
				mech.heatSinks() + " [" + (mech.heatSinks() * 2) + "]" : mech.heatSinks());
		setModelData("hsMass", NumberFormat.getInstance().format(testMech.getWeightHeatSinks()));
		if (mech.getGyroType() == Mech.GYRO_STANDARD) {
			setModelData("gyroType", mech.getRawSystemName(Mech.SYSTEM_GYRO));
		} else {
			setModelData("gyroType", Mech.getGyroDisplayString(mech.getGyroType())); 
		}
		setModelData("gyroMass", NumberFormat.getInstance().format(testMech.getWeightGyro()));
		if ((mech.getCockpitType() == Mech.COCKPIT_STANDARD)
				|| (mech.getCockpitType() == Mech.COCKPIT_INDUSTRIAL)) {
			setModelData("cockpitType", mech.getRawSystemName(Mech.SYSTEM_COCKPIT));
		} else {
			setModelData("cockpitType", Mech.getCockpitDisplayString(mech.getCockpitType()));
		}
		setModelData("cockpitMass", NumberFormat.getInstance().format(testMech.getWeightCockpit()));
		String atName = formatArmorType(mech, true);
		if (atName.length() > 0) {
			setModelData("armorType", " (" + atName + ")");
		} else {
			setModelData("armorType", "");
		}
		setModelData("armorFactor", mech.getTotalOArmor());
		setModelData("armorMass", NumberFormat.getInstance().format(testMech.getWeightArmor()));
		if (mech.isOmni()) {
			addFixedOmni(mech);
		}
		if (mech.hasETypeFlag(Entity.ETYPE_LAND_AIR_MECH)) {
			final LandAirMech lam = (LandAirMech) mech;
			setModelData("lamConversionMass", testMech.getWeightMisc());
			if (lam.getLAMType() == LandAirMech.LAM_STANDARD) {
				setModelData("airmechCruise", lam.getAirMechCruiseMP());
				setModelData("airmechFlank", lam.getAirMechFlankMP());
			} else {
				setModelData("airmechCruise", "N/A");
				setModelData("airmechFlank", "N/A");
			}
			lam.setConversionMode(LandAirMech.CONV_MODE_FIGHTER);
			setModelData("safeThrust", lam.getWalkMP());
			setModelData("maxThrust", lam.getRunMP());
		} else if (mech.hasETypeFlag(Entity.ETYPE_QUADVEE)) {
			final QuadVee qv = (QuadVee) mech;
			qv.setConversionMode(QuadVee.CONV_MODE_VEHICLE);
			setModelData("qvConversionMass", testMech.getWeightMisc());
			setModelData("qvType", Messages.getString("MovementType." + qv.getMovementModeAsString()));
			setModelData("qvCruise", qv.getWalkMP());
			setModelData("qvFlank", qv.getRunMPasString());
		}
		setModelData("rightArmActuators", countArmActuators(Mech.LOC_RARM));
		setModelData("leftArmActuators", countArmActuators(Mech.LOC_LARM));
	}
	
	private String countArmActuators(int location) {
		StringJoiner sj = new StringJoiner(", ");
		for (int act = Mech.ACTUATOR_SHOULDER; act <= Mech.ACTUATOR_HAND; act++) {
			if (mech.hasSystem(act, location)) {
				sj.add(mech.getRawSystemName(act));
			}
		}
		return sj.toString();
	}
	
	protected void addFluff() {
		addMechVeeAeroFluff(mech);
		// If we had a fluff field for chassis type we would put it here
		String chassisDesc = EquipmentType.getStructureTypeName(mech.getStructureType());
		if (mech.isIndustrial()) {
			chassisDesc += Messages.getString("TROView.chassisIndustrial");
		}
		if (mech.isSuperHeavy()) {
			chassisDesc += Messages.getString("TROView.chassisSuperheavy");
		}
		if (mech.hasETypeFlag(Entity.ETYPE_QUADVEE)) {
			chassisDesc += Messages.getString("TROView.chassisQuadVee");
		} else if (mech.hasETypeFlag(Entity.ETYPE_QUAD_MECH)) {
			chassisDesc += Messages.getString("TROView.chassisQuad");
		} else if (mech.hasETypeFlag(Entity.ETYPE_TRIPOD_MECH)) {
			chassisDesc += Messages.getString("TROView.chassisTripod");
		} else if (mech.hasETypeFlag(Entity.ETYPE_LAND_AIR_MECH)) {
			chassisDesc += Messages.getString("TROView.chassisLAM");
		} else {
			chassisDesc += Messages.getString("TROView.chassisBiped");
		}
		setModelData("chassisDesc", chassisDesc);
		setModelData("jjDesc", formatJJDesc());
		setModelData("jumpCapacity", mech.getJumpMP() * 30);
	}
	
	private static final int[][] MECH_ARMOR_LOCS = {
			{Mech.LOC_HEAD}, {Mech.LOC_CT}, {Mech.LOC_RT, Mech.LOC_LT},
			{Mech.LOC_RARM, Mech.LOC_LARM}, {Mech.LOC_RLEG, Mech.LOC_CLEG, Mech.LOC_LLEG}
	};
	
	private static final int[][] MECH_ARMOR_LOCS_REAR = {
			{Mech.LOC_CT}, {Mech.LOC_RT, Mech.LOC_LT}
	};
	
	private void addArmorAndStructure() {
		setModelData("structureValues", addArmorStructureEntries(mech,
				(en, loc) -> en.getOInternal(loc),
				MECH_ARMOR_LOCS));
		setModelData("armorValues", addArmorStructureEntries(mech,
				(en, loc) -> en.getOArmor(loc),
				MECH_ARMOR_LOCS));
		setModelData("rearArmorValues", addArmorStructureEntries(mech,
				(en, loc) -> en.getOArmor(loc, true),
				MECH_ARMOR_LOCS_REAR));
		if (mech.hasPatchworkArmor()) {
			setModelData("patchworkByLoc", addPatchworkATs(mech, MECH_ARMOR_LOCS));
		}
	}
	
	private String formatJJDesc() {
		switch (mech.getJumpType()) {
			case Mech.JUMP_STANDARD:
				return Messages.getString("TROView.jjStandard");
			case Mech.JUMP_IMPROVED:
				return Messages.getString("TROView.jjImproved");
			case Mech.JUMP_PROTOTYPE:
				return Messages.getString("TROView.jjPrototype");
			case Mech.JUMP_PROTOTYPE_IMPROVED:
				return Messages.getString("TROView.jjImpPrototype");
			case Mech.JUMP_BOOSTER:
				return Messages.getString("TROView.jjBooster");
			default:
				return Messages.getString("TROView.jjNone");
		}
	}
	
}
