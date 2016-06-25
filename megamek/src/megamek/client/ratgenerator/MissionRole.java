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

import java.util.Collection;
import java.util.HashSet;

import megamek.common.EntityWeightClass;

/**
 * Used to adjust availability to conform to a particular mission role.
 * 
 * @author Neoancient
 *
 */
public enum MissionRole {
	/*General combat roles */
	RECON, RAIDER, INCINDIARY, EW_SUPPORT, ARTILLERY, MISSILE_ARTILLERY, APC, TRAINING,
	/* Non-combat roles */
	CARGO, SUPPORT, CIVILIAN,
	/* Ground forces */
	FIRE_SUPPORT, SR_FIRE_SUPPORT, URBAN, SPOTTER, ANTI_AIRCRAFT, ANTI_INFANTRY, INF_SUPPORT, CAVALRY,
	/* Specialized ground support roles */
	SPECOPS, ENGINEER, MINESWEEPER, MINELAYER,
	/* ASF roles */
	BOMBER, ESCORT, INTERCEPTOR, GROUND_SUPPORT, STRIKE,
	/* DropShip roles */
	ASSAULT, MECH_CARRIER, ASF_CARRIER, VEE_CARRIER, INFANTRY_CARRIER, BA_CARRIER, TROOP_CARRIER,
	TUG, POCKET_WARSHIP,
	/* WarShip roles */
	CORVETTE, DESTROYER, FRIGATE, CRUISER, BATTLESHIP,
	/* Battle armor */
	MECHANIZED_BA,
	/* Infantry roles */
	MARINE, MOUNTAINEER, XCT, PARATROOPER, ANTI_MEK, FIELD_GUN; 
	
	public boolean fitsUnitType(String unitType) {
		if (ordinal() <= CIVILIAN.ordinal()) {
			return true;
		}
		switch (unitType) {
		case "Mek":
		case "Tank":
		case "ProtoMek":
			return ordinal() < BOMBER.ordinal();
		case "VTOL":
		case "Aero":
		case "Conventional Fighter":
			return ordinal() >= BOMBER.ordinal() && ordinal() < ASSAULT.ordinal();
		case "Dropship":
			return ordinal() >= ASSAULT.ordinal() && ordinal() < CORVETTE.ordinal();
		case "Warship":
			return ordinal() >= CORVETTE.ordinal() && ordinal() < MARINE.ordinal();
		case "Battle Armor":
			return ordinal() >= MECHANIZED_BA.ordinal();
		case "Infantry":
			return ordinal() >= MARINE.ordinal();
		}
		return false;
	}
	
	public static double adjustAvailabilityByRole(double avRating, Collection<MissionRole> desiredRoles,
			ModelRecord mRec, int year, int strictness) {
		boolean roleApplied = false;
		if (desiredRoles == null) {
			desiredRoles = new HashSet<MissionRole>();
		}
		if (desiredRoles.size() > 0) {
			roleApplied = true;
			for (MissionRole role : desiredRoles) {
				switch (role) {
				case ARTILLERY:
					if (!mRec.getRoles().contains(ARTILLERY)
							&& !mRec.getRoles().contains(MISSILE_ARTILLERY)) {
						avRating = 0;
					}
					break;
				case MISSILE_ARTILLERY:
					if (!mRec.getRoles().contains(MISSILE_ARTILLERY)) {
						avRating = 0;
					}
					break;
				case ENGINEER:
					if (!mRec.getRoles().contains(ENGINEER)) {
						avRating = 0;
					}
					break;
				case RECON:
					if (mRec.getRoles().contains(RECON)){
							avRating += strictness;
					} else if (mRec.getRoles().contains(EW_SUPPORT)) {
						avRating += Math.max(1, strictness - 1);
					} else if (mRec.getRoles().contains(SPOTTER)) {
						avRating += Math.max(0, strictness - 2);
					} else {
						if ((mRec.getRoles().contains(SUPPORT) && !desiredRoles.contains("support")) ||
								(mRec.getRoles().contains(CARGO) && !desiredRoles.contains("cargo")) ||
								(mRec.getRoles().contains(TUG) && !desiredRoles.contains("tug")) ||
								(mRec.getRoles().contains(CIVILIAN) && !desiredRoles.contains("civilian")) ||
								(mRec.getRoles().contains(TRAINING) && !desiredRoles.contains("training")) ||
								(mRec.getRoles().contains(ARTILLERY) && !desiredRoles.contains("artillery"))) {
							avRating = 0;
						} else if (!mRec.getUnitType().equals("Infantry")
								&& !mRec.getUnitType().equals("BattleArmor")
									&& mRec.getSpeed() < 4 + strictness - mRec.getWeightClass()) {
							avRating = 0;
						} else {
							avRating += mRec.getSpeed() - (4 + strictness - mRec.getWeightClass());
							if (mRec.getRoles().contains(URBAN) ||
									mRec.getRoles().contains(INF_SUPPORT) ||
									mRec.getRoles().contains(ANTI_INFANTRY) ||
									mRec.getRoles().contains(APC) ||
									mRec.getRoles().contains(MOUNTAINEER) ||
									mRec.getRoles().contains(PARATROOPER) ||
									mRec.getRoles().contains(MARINE) ||
									mRec.getRoles().contains(XCT)) {
								avRating -= strictness + 1;
							} else if (mRec.getRoles().contains(SPECOPS)) {
								avRating -= strictness + 2;
							} else {
								avRating -= Math.min(0, strictness - 1);
							}
						}
					}
					break;
				case FIELD_GUN:
					if (!mRec.getRoles().contains(FIELD_GUN)) {
						return 0;
					}
					break;
				case FIRE_SUPPORT:
					if (mRec.getRoles().contains(FIRE_SUPPORT)
							|| mRec.getLongRange() > 0.75) {
						avRating += strictness;
					} else if (mRec.getLongRange() > 0.5) {
						avRating += Math.max(1, strictness - 1);
					} else if (mRec.getLongRange() > 0.2) {
						avRating += Math.max(0, strictness - 2);
					} else if (mRec.getRoles().contains(SR_FIRE_SUPPORT) ||
						mRec.getRoles().contains(ANTI_AIRCRAFT)) {
						avRating += Math.max(0, strictness - 2);
					} else {
						avRating = 0;
					}
					break;
				case INF_SUPPORT:
					if (mRec.getRoles().contains(INF_SUPPORT)) {
						avRating += strictness;
					} else if (mRec.getRoles().contains("APC")) {
						avRating += Math.max(0, strictness - 1);
					} else if (mRec.getRoles().contains(ANTI_INFANTRY)
							|| mRec.getRoles().contains(URBAN)) {
						avRating += Math.max(0, strictness - 2);
					} else {
						if ((mRec.getRoles().contains(SUPPORT) && !desiredRoles.contains("support")) ||
								(mRec.getRoles().contains(CARGO) && !desiredRoles.contains("cargo")) ||
								(mRec.getRoles().contains(TUG) && !desiredRoles.contains("tug")) ||
								(mRec.getRoles().contains(CIVILIAN) && !desiredRoles.contains("civilian")) ||
								(mRec.getRoles().contains(TRAINING) && !desiredRoles.contains("training")) ||
								(mRec.getRoles().contains(ARTILLERY) && !desiredRoles.contains("artillery"))) {
							avRating = 0;
						} else {
							if (mRec.getRoles().contains(MOUNTAINEER) ||
									mRec.getRoles().contains(PARATROOPER)) {
								avRating -= strictness;
							} else if (mRec.getRoles().contains(INCINDIARY) ||
									mRec.getRoles().contains(MARINE) ||
									mRec.getRoles().contains(XCT)) {
								avRating -= strictness + 1;
							} else if (mRec.getRoles().contains(SPECOPS)) {
								avRating -= strictness + 2;
							} else {
								avRating -= Math.min(0, strictness - 1);
							}
						}
					}
					break;
				case MECHANIZED_BA:
					if ((mRec.getUnitType().equals("Mek") || mRec.getUnitType().equals("Tank"))
							&& !mRec.isOmni()) {
						return 0;
					}
					if (mRec.getUnitType().equals("BattleArmor")
							&& (mRec.getWeightClass() > EntityWeightClass.WEIGHT_HEAVY
							|| (mRec.isQuad() != null && mRec.isQuad()))) {
						return 0;						
					}
					break;
				case URBAN:
					if (mRec.getRoles().contains(URBAN)) {
						avRating += strictness;
					} else if (mRec.getRoles().contains(ANTI_INFANTRY)) {
						avRating += Math.max(0, strictness - 1);
					} else if (mRec.getRoles().contains(INF_SUPPORT)) {
						avRating += Math.max(0, strictness - 2);
					} else {
						if ((mRec.getRoles().contains(SUPPORT) && !desiredRoles.contains("support")) ||
								(mRec.getRoles().contains(CARGO) && !desiredRoles.contains("cargo")) ||
								(mRec.getRoles().contains(TUG) && !desiredRoles.contains("tug")) ||
								(mRec.getRoles().contains(CIVILIAN) && !desiredRoles.contains("civilian")) ||
								(mRec.getRoles().contains(TRAINING) && !desiredRoles.contains("training")) ||
								(mRec.getRoles().contains(ARTILLERY) && !desiredRoles.contains("artillery"))) {
							avRating = 0;
						}
						if (mRec.getRoles().contains(FIRE_SUPPORT) ||
								mRec.getRoles().contains(SR_FIRE_SUPPORT) ||
								mRec.getRoles().contains(ANTI_AIRCRAFT)) {
							avRating -= Math.max(0, strictness - 2);
						}
						if (mRec.getRoles().contains(MARINE) ||
								mRec.getRoles().contains(XCT)) {
							avRating -= strictness;
						}
						if (mRec.getRoles().contains(MOUNTAINEER) ||
								mRec.getRoles().contains(PARATROOPER) ||
								mRec.getRoles().contains(SPECOPS)) {
							avRating -= strictness + 1;
						}
					}
					if (avRating > 0 && mRec.getUnitType().equals("Tank")) {
						if (mRec.getMovementType() == AbstractUnitRecord.MOVEMENT_WHEELED) {
							avRating++;
						} else if (mRec.getMovementType() == AbstractUnitRecord.MOVEMENT_TRACKED) {
							avRating--;
						}
					}
					break;
					
				case RAIDER:
					if (mRec.getRoles().contains(RAIDER)) {
						avRating += strictness;
					} else {
						if ((mRec.getRoles().contains(SUPPORT) && !desiredRoles.contains("support")) ||
								(mRec.getRoles().contains(CARGO) && !desiredRoles.contains("cargo")) ||
								(mRec.getRoles().contains(TUG) && !desiredRoles.contains("tug")) ||
								(mRec.getRoles().contains(CIVILIAN) && !desiredRoles.contains("civilian")) ||
								(mRec.getRoles().contains(TRAINING) && !desiredRoles.contains("training")) ||
								(mRec.getRoles().contains(ARTILLERY) && !desiredRoles.contains("artillery"))) {
							avRating = 0;
						} else if (mRec.getAmmoRequirement() < 0.2) {
							avRating += Math.max(1, strictness - 1);
						} else if (mRec.getAmmoRequirement() < 0.5) {
							avRating += Math.max(0, strictness - 2);
						}
						if (!mRec.getUnitType().equals("Infantry")
								&& !mRec.getUnitType().equals("BattleArmor")
									&& mRec.getSpeed() < 3 + strictness - mRec.getWeightClass()) {
							avRating -= 3 + strictness - mRec.getWeightClass() - mRec.getSpeed();
						}
					}
					break;
				case INCINDIARY:
					if (mRec.getRoles().contains(INCINDIARY)) {
						avRating += strictness;
					} else {
						if ((mRec.getRoles().contains(SUPPORT) && !desiredRoles.contains("support")) ||
								(mRec.getRoles().contains(CARGO) && !desiredRoles.contains("cargo")) ||
								(mRec.getRoles().contains(TUG) && !desiredRoles.contains("tug")) ||
								(mRec.getRoles().contains(CIVILIAN) && !desiredRoles.contains("civilian")) ||
								(mRec.getRoles().contains(TRAINING) && !desiredRoles.contains("training")) ||
								(mRec.getRoles().contains(ARTILLERY) && !desiredRoles.contains("artillery"))) {
							avRating = 0;
						} else if (mRec.hasFlamer()) {
							avRating += Math.max(1, strictness - 1);
						} else {
							avRating -= Math.max(1, strictness - 1);
						}
					}
					break;
				case ANTI_AIRCRAFT:
					if (mRec.getRoles().contains(ANTI_AIRCRAFT) ||
							mRec.getFlak() > 0.75) {
						avRating += strictness;
					} else if (mRec.getFlak() > 0.5) {
						avRating += Math.max(1, strictness - 1);
					} else if (mRec.getFlak() > 0.2) {
						avRating += Math.max(0, strictness - 2);
					} else {
						if ((mRec.getRoles().contains(SUPPORT) && !desiredRoles.contains("support")) ||
								(mRec.getRoles().contains(CARGO) && !desiredRoles.contains("cargo")) ||
								(mRec.getRoles().contains(TUG) && !desiredRoles.contains("tug")) ||
								(mRec.getRoles().contains(CIVILIAN) && !desiredRoles.contains("civilian")) ||
								(mRec.getRoles().contains(TRAINING) && !desiredRoles.contains("training")) ||
								(mRec.getRoles().contains(ARTILLERY) && !desiredRoles.contains("artillery"))) {
							avRating = 0;
						} else {
							avRating -= Math.max(0, strictness - 1);
						}
					}
					break;
				case ANTI_INFANTRY:
					if (mRec.getRoles().contains(ANTI_INFANTRY)) {
						avRating += strictness;
					} else 	if ((mRec.getRoles().contains(SUPPORT) && !desiredRoles.contains("support")) ||
							(mRec.getRoles().contains(CARGO) && !desiredRoles.contains("cargo")) ||
							(mRec.getRoles().contains(TUG) && !desiredRoles.contains("tug")) ||
							(mRec.getRoles().contains(CIVILIAN) && !desiredRoles.contains("civilian")) ||
							(mRec.getRoles().contains(TRAINING) && !desiredRoles.contains("training")) ||
							(mRec.getRoles().contains(ARTILLERY) && !desiredRoles.contains("artillery"))) {
						avRating = 0;
					} else if (!mRec.hasAPWeapons()) {
						avRating -= strictness;
					}
					break;
					
				case GROUND_SUPPORT:
					if (mRec.getRoles().contains(GROUND_SUPPORT)) {
						avRating += strictness;
					} else {
						avRating -= strictness;
					}
					break;
				case INTERCEPTOR:
					if (mRec.getRoles().contains(INTERCEPTOR)) {
						avRating += strictness;
					} else {
						avRating -= strictness;
					}
					break;
				case ASSAULT:
					if (!mRec.getRoles().contains(ASSAULT)) {
						return 0;
					}
					break;
					
				case BOMBER:
					if (mRec.getRoles().contains(BOMBER)) {
						avRating += strictness;
					} else {
						avRating -= strictness;
					}
					break;
				case ESCORT:
					if (mRec.getRoles().contains(ESCORT)) {
						avRating += strictness;
					} else {
						avRating -= strictness;
					}
					break;
				default:
					roleApplied = false;
				}
			}
		}
		if (!roleApplied) {
			if (desiredRoles.contains(CAVALRY)) {
				if (mRec.getUnitType().equals("Infantry") || mRec.getUnitType().equals("BattleArmor")) {
					avRating += strictness * (mRec.getSpeed() - 3);
				} else {
					avRating += strictness * (mRec.getSpeed() - (7 - mRec.getWeightClass()));					
				}
			}
			if ((mRec.getRoles().contains(SUPPORT) && !desiredRoles.contains(SUPPORT)) ||
					(mRec.getRoles().contains(CARGO) && !(desiredRoles.contains(CARGO) || desiredRoles.size() > 1 || mRec.getUnitType().equals("Warship"))) ||
					(mRec.getRoles().contains(TUG) && !desiredRoles.contains(TUG)) ||
					(mRec.getRoles().contains(CIVILIAN) && !desiredRoles.contains(CIVILIAN)) ||
					(mRec.getRoles().contains(TRAINING) && !desiredRoles.contains(TRAINING)) ||
					(mRec.getRoles().contains(FIELD_GUN) && !desiredRoles.contains(FIELD_GUN)) ||
					(mRec.getRoles().contains(ARTILLERY))) {
				avRating = 0;
			}
			if (mRec.getRoles().contains(MISSILE_ARTILLERY) &&
					!desiredRoles.contains(ARTILLERY)
					&& !desiredRoles.contains(MISSILE_ARTILLERY)
					&& !desiredRoles.contains(FIRE_SUPPORT)) {
				return 0;
			}
			if (mRec.getRoles().contains(MISSILE_ARTILLERY)
					&& desiredRoles.contains(FIRE_SUPPORT)) {
				avRating -= strictness;
			}
			if (mRec.getRoles().contains(FIRE_SUPPORT) ||
					mRec.getRoles().contains(SR_FIRE_SUPPORT) ||
					mRec.getRoles().contains(ANTI_AIRCRAFT)) {
				avRating -= Math.max(0, strictness - 2);
			}
			if (mRec.getRoles().contains(RECON) ||
					mRec.getRoles().contains(EW_SUPPORT)) {
				avRating -= Math.max(0, strictness - 2);
			}
			if (mRec.getRoles().contains(URBAN) ||
					mRec.getRoles().contains(INF_SUPPORT) ||
					mRec.getRoles().contains(ANTI_INFANTRY) ||
					mRec.getRoles().contains(APC) ||
					mRec.getRoles().contains(MOUNTAINEER) ||
					mRec.getRoles().contains(PARATROOPER)) {
				avRating -= Math.max(1, strictness - 1);
			}
			if (mRec.getRoles().contains(INCINDIARY) ||
					mRec.getRoles().contains(MARINE) ||
					mRec.getRoles().contains(XCT)) {
				avRating -= strictness;
			}
			if (mRec.getRoles().contains(SPECOPS)) {
				avRating -= strictness + 1;
			}
		}
		if (avRating < 0) {
			return 0;
		}
		return Math.min(avRating, AvailabilityRating.MAX_AV_RATING);
	}
	
	public static MissionRole parseRole(String role) {
		switch (role.toLowerCase()) {
		case "recon":
			return RECON;
		case "fire support":
		case "fire_support":
			return FIRE_SUPPORT;
		case "sr fire support":
			return SR_FIRE_SUPPORT;
		case "spotter":
			return SPOTTER;
		case "urban":
			return URBAN;
		case "infantry support":
			return INF_SUPPORT;
		case "raider":
			return RAIDER;
		case "incindiary":
			return INCINDIARY;
		case "ew support":
			return EW_SUPPORT;
		case "artillery":
			return ARTILLERY;
		case "missile artillery":
			return MISSILE_ARTILLERY;
		case "anti-aircraft":
		case "anti aircraft":
			return ANTI_AIRCRAFT;
		case "anti-infantry":
		case "anti infantry":
			return ANTI_INFANTRY;
		case "apc":
			return APC;
		case "spec ops":
		case "specops":
			return SPECOPS;
		case "cargo":
			return CARGO;
		case "support":
			return SUPPORT;
		case "bomber":
			return BOMBER;
		case "escort":
			return ESCORT;
		case "interceptor":
			return INTERCEPTOR;
		case "ground support":
			return GROUND_SUPPORT;
		case "strike":
			return STRIKE;
		case "training":
			return TRAINING;
		case "assault":
			return ASSAULT;
		case "mech carrier":
			return MECH_CARRIER;
		case "asf carrier":
			return ASF_CARRIER;
		case "vee carrier":
			return VEE_CARRIER;
		case "infantry carrier":
			return INFANTRY_CARRIER;
		case "ba carrier":
			return BA_CARRIER;
		case "tug":
			return TUG;
		case "troop carrier":
			return TROOP_CARRIER;
		case "pocket ws":
			return POCKET_WARSHIP;
		case "corvette":
			return CORVETTE;
		case "destroyer":
			return DESTROYER;
		case "frigate":
			return FRIGATE;
		case "cruiser":
			return CRUISER;
		case "battleship":
			return BATTLESHIP;
		case "engineer":
			return ENGINEER;
		case "marine":
			return MARINE;
		case "mountaineer":
			return MOUNTAINEER;
		case "xct":
			return XCT;
		case "paratrooper":
			return PARATROOPER;
		case "anti-mek":
		case "anti mek":
			return ANTI_MEK;
		case "mechanized ba":
		case "omni":
			return MECHANIZED_BA;
		case "field gun":
			return FIELD_GUN;
		case "civilian":
			return CIVILIAN;
		case "minesweeper":
			return MINESWEEPER;
		case "minelayer":
			return MINELAYER;
		}
		return null;
	}
	
	@Override
	public String toString() {
		return name().toLowerCase().replace("_", " ");
	}
}
