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
import java.util.List;
import java.util.Map;

import megamek.common.Messages;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.SuperHeavyTank;
import megamek.common.Tank;
import megamek.common.Transporter;
import megamek.common.VTOL;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestTank;

/**
 * Creates a TRO template model for combat vehicles.
 * 
 * @author Neoancient
 *
 */
public class VehicleTROView extends TROView {
	
	private final Tank tank;
	
	public VehicleTROView(Tank tank) {
		this.tank = tank;
	}

	@Override
	protected String getTemplateFileName(boolean html) {
		if (html) {
			return "vehicle.ftlh";
		}
		return "vehicle.ftl";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void initModel(EntityVerifier verifier) {
		setModelData("formatArmorRow", new FormatTableRowMethod(new int[] { 20, 10, 10},
				new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER }));
		addBasicData(tank);
		addArmorAndStructure();
		int nameWidth = addEquipment(tank);
		setModelData("formatEquipmentRow", new FormatTableRowMethod(new int[] { nameWidth, 12, 12},
				new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER,
						Justification.CENTER, Justification.CENTER}));
		addFluff();
		setModelData("isOmni", tank.isOmni());
		setModelData("isVTOL", tank.hasETypeFlag(Entity.ETYPE_VTOL));
		setModelData("isSuperheavy", tank.isSuperHeavy());
		setModelData("isSupport", tank.isSupportVehicle());
		setModelData("hasTurret", !tank.hasNoTurret());
		setModelData("hasTurret2", !tank.hasNoDualTurret());
		setModelData("moveType", Messages.getString("MovementType." + tank.getMovementModeAsString()));
		TestTank testTank = new TestTank(tank, verifier.tankOption, null);
		setModelData("isMass", NumberFormat.getInstance().format(testTank.getWeightStructure()));
		setModelData("engineName", stripNotes(tank.getEngine().getEngineName()));
		setModelData("engineMass", NumberFormat.getInstance().format(testTank.getWeightEngine()));
		setModelData("walkMP", tank.getWalkMP());
		setModelData("runMP", tank.getRunMPasString());
		if (tank.getJumpMP() > 0) {
			setModelData("jumpMP", tank.getJumpMP());
		}
		setModelData("hsCount", Math.max(testTank.getCountHeatSinks(),
				tank.getEngine().getWeightFreeEngineHeatSinks()));
		setModelData("hsMass", NumberFormat.getInstance().format(testTank.getWeightHeatSinks()));
		setModelData("controlMass", testTank.getWeightControls());
		setModelData("liftMass", testTank.getTankWeightLifting());
		setModelData("amplifierMass", testTank.getWeightPowerAmp());
		setModelData("turretMass", testTank.getTankWeightTurret());
		setModelData("turretMass2", testTank.getTankWeightDualTurret());
		String atName = formatArmorType(tank, true);
		if (atName.length() > 0) {
			setModelData("armorType", " (" + atName + ")");
		} else {
			setModelData("armorType", "");
		}
		setModelData("armorFactor", tank.getTotalOArmor());
		setModelData("armorMass", NumberFormat.getInstance().format(testTank.getWeightArmor()));
		if (tank.isOmni()) {
			addFixedOmni(tank);
		}
		for (Transporter t : tank.getTransports()) {
			Map<String, Object> row = this.formatTransporter(t, tank.getLocationName(Tank.LOC_BODY));
			if (null == row) {
				continue;
			}
			if (tank.isOmni() && !tank.isPodMountedTransport(t)) {
				((List<Map<String, Object>>) getModelData("fixedEquipment")).add(row);
				setModelData("fixedTonnage", ((double) getModelData("fixedTonnage")) + (double) row.get("tonnage"));
			} else {
				((List<Map<String, Object>>) getModelData("equipment")).add(row);
			}
		}
	}
	
	private void addFluff() {
		addMechVeeAeroFluff(tank);
		if (tank.getJumpMP() > 0) {
			setModelData("jjDesc", Messages.getString("TROView.jjVehicle"));
			setModelData("jumpCapacity", tank.getJumpMP() * 30);
		}
	}
	
	private static final int[][] TANK_ARMOR_LOCS = {
			{Tank.LOC_FRONT}, {Tank.LOC_RIGHT, Tank.LOC_LEFT}, {Tank.LOC_REAR},
			{Tank.LOC_TURRET}, {Tank.LOC_TURRET_2}, {VTOL.LOC_ROTOR}
	};
	
	private static final int[][] SH_TANK_ARMOR_LOCS = {
			{SuperHeavyTank.LOC_FRONT},
			{SuperHeavyTank.LOC_FRONTRIGHT, SuperHeavyTank.LOC_FRONTLEFT},
			{SuperHeavyTank.LOC_REARRIGHT, SuperHeavyTank.LOC_REARLEFT},
			{SuperHeavyTank.LOC_REAR},
			{SuperHeavyTank.LOC_TURRET}, {SuperHeavyTank.LOC_TURRET_2}
	};
	
	private void addArmorAndStructure() {
		if (tank.hasETypeFlag(Entity.ETYPE_SUPER_HEAVY_TANK)) {
			setModelData("structureValues", addArmorStructureEntries(tank,
					(en, loc) -> en.getOInternal(loc),
					SH_TANK_ARMOR_LOCS));
			setModelData("armorValues", addArmorStructureEntries(tank,
					(en, loc) -> en.getOArmor(loc),
					SH_TANK_ARMOR_LOCS));
			if (tank.hasPatchworkArmor()) {
				setModelData("patchworkByLoc", addPatchworkATs(tank, SH_TANK_ARMOR_LOCS));
			}
		} else {
			setModelData("structureValues", addArmorStructureEntries(tank,
					(en, loc) -> en.getOInternal(loc),
					TANK_ARMOR_LOCS));
			setModelData("armorValues", addArmorStructureEntries(tank,
					(en, loc) -> en.getOArmor(loc),
					TANK_ARMOR_LOCS));
			if (tank.hasPatchworkArmor()) {
				setModelData("patchworkByLoc", addPatchworkATs(tank, TANK_ARMOR_LOCS));
			}
		}
	}
	
	@Override
	protected String formatLocationTableEntry(Entity entity, Mounted mounted) {
		return entity.getLocationName(mounted.getLocation());
	}
}
