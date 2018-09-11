/**
 * 
 */
package megamek.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import megamek.client.ui.Messages;
import megamek.common.annotations.Nullable;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.Quirks;
import megamek.common.util.MegaMekFile;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestMech;
import megamek.common.verifier.TestTank;

/**
 * Fills in a template to produce a unit summary in TRO format.
 * 
 * @author Neoancient
 *
 */
public class TROView {
	
	private Template template;
	private Map<String, Object> model = new HashMap<>();
    private EntityVerifier verifier = EntityVerifier.getInstance(new MegaMekFile(
            Configuration.unitsDir(), EntityVerifier.CONFIG_FILENAME).getFile());

	public TROView(Entity entity, boolean html) {
		String templateFileName = null;
		if (entity.hasETypeFlag(Entity.ETYPE_MECH)) {
			templateFileName = "mech_tro.ftl";
			addMechData((Mech) entity);
		} else if (entity.hasETypeFlag(Entity.ETYPE_TANK)) {
			templateFileName = "vehicle_tro.ftl";
			addVehicleData((Tank) entity);
		}
		if (null != templateFileName) {
			if (html) {
				templateFileName += "h";
			}
			try {
				template = TemplateConfiguration.getInstance().getTemplate(templateFileName);
			} catch (IOException e) {
				DefaultMmLogger.getInstance().error(getClass(), "TROView(Entity)", e);
			}
		}
	}
	
	/**
	 * Uses the template and supplied {@link Entity} to generate a TRO document
	 * 
	 * @return The generated document. Returns {@code null} if there was an error that prevented
	 *         the document from being generated. Check logs for reason.
	 */
	@Nullable public String processTemplate() {
		if (null != template) {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			Writer out = new OutputStreamWriter(os);
			try {
				template.process(model, out);
				
			} catch (TemplateException | IOException e) {
				DefaultMmLogger.getInstance().error(getClass(), "processTemplate()", e);
				e.printStackTrace();
			}
			return os.toString();
		}
		return null;
	}
	
	private void addBasicData(Entity entity) {
		model.put("formatBasicDataRow", new FormatTableRowMethod(new int[] { 30, 20, 5},
				new Justification[] { Justification.LEFT, Justification.LEFT, Justification.RIGHT }));
		model.put("fullName", entity.getShortNameRaw());
		model.put("chassis", entity.getChassis());
		model.put("techBase", formatTechBase(entity));
		model.put("tonnage", NumberFormat.getInstance().format(entity.getWeight()));
		model.put("battleValue", NumberFormat.getInstance().format(entity.calculateBattleValue()));
		
        StringJoiner quirksList = new StringJoiner(", ");
        Quirks quirks = entity.getQuirks();
        for (Enumeration<IOptionGroup> optionGroups = quirks.getGroups(); optionGroups.hasMoreElements();) {
            IOptionGroup group = optionGroups.nextElement();
            if (quirks.count(group.getKey()) > 0) {
                for (Enumeration<IOption> options = group.getOptions(); options.hasMoreElements();) {
                    IOption option = options.nextElement();
                    if (option != null && option.booleanValue()) {
                        quirksList.add(option.getDisplayableNameWithValue());
                    }
                }
            }
        }
        if (quirksList.length() > 0) {
        	model.put("quirks", quirksList.toString());
        }
        
	}

	private void addMechData(Mech mech) {
		model.put("formatArmorRow", new FormatTableRowMethod(new int[] { 20, 10, 10},
				new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER }));
		model.put("formatEquipmentRow", new FormatTableRowMethod(new int[] { 30, 12, 8, 10, 8},
				new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER,
						Justification.CENTER, Justification.CENTER}));
		addBasicData(mech);
		addArmorAndStructure(mech);
		addEquipment(mech);
		addMechFluff(mech);
		mech.setConversionMode(0);
		model.put("isOmni", mech.isOmni());
		model.put("isQuad", mech.hasETypeFlag(Entity.ETYPE_QUAD_MECH));
		model.put("isTripod", mech.hasETypeFlag(Entity.ETYPE_TRIPOD_MECH));
		TestMech testMech = new TestMech(mech, verifier.mechOption, null);
		model.put("structureName", mech.getStructureType() == EquipmentType.T_STRUCTURE_STANDARD?
				"" : EquipmentType.getStructureTypeName(mech.getStructureType()));
		model.put("isMass", NumberFormat.getInstance().format(testMech.getWeightStructure()));
		model.put("engineName", stripNotes(mech.getEngine().getEngineName()));
		model.put("engineMass", NumberFormat.getInstance().format(testMech.getWeightEngine()));
		model.put("walkMP", mech.getWalkMP());
		model.put("runMP", mech.getRunMPasString());
		model.put("jumpMP", mech.getJumpMP());
		model.put("hsType", mech.getHeatSinkTypeName());
		model.put("hsCount", mech.hasDoubleHeatSinks()?
				mech.heatSinks() + " [" + (mech.heatSinks() * 2) + "]" : mech.heatSinks());
		model.put("hsMass", NumberFormat.getInstance().format(testMech.getWeightHeatSinks()));
		if (mech.getGyroType() == Mech.GYRO_STANDARD) {
			model.put("gyroType", mech.getRawSystemName(Mech.SYSTEM_GYRO));
		} else {
			model.put("gyroType", Mech.getGyroDisplayString(mech.getGyroType())); 
		}
		model.put("gyroMass", NumberFormat.getInstance().format(testMech.getWeightGyro()));
		if ((mech.getCockpitType() == Mech.COCKPIT_STANDARD)
				|| (mech.getCockpitType() == Mech.COCKPIT_INDUSTRIAL)) {
			model.put("cockpitType", mech.getRawSystemName(Mech.SYSTEM_COCKPIT));
		} else {
			model.put("cockpitType", Mech.getCockpitDisplayString(mech.getCockpitType()));
		}
		model.put("cockpitMass", NumberFormat.getInstance().format(testMech.getWeightCockpit()));
		String atName = formatArmorType(mech, true);
		if (atName.length() > 0) {
			model.put("armorType", " (" + atName + ")");
		} else {
			model.put("armorType", "");
		}
		model.put("armorFactor", mech.getTotalOArmor());
		model.put("armorMass", NumberFormat.getInstance().format(testMech.getWeightArmor()));
		if (mech.isOmni()) {
			addFixedOmni(mech);
		}
		if (mech.hasETypeFlag(Entity.ETYPE_LAND_AIR_MECH)) {
			final LandAirMech lam = (LandAirMech) mech;
			model.put("lamConversionMass", testMech.getWeightMisc());
			if (lam.getLAMType() == LandAirMech.LAM_STANDARD) {
				model.put("airmechCruise", lam.getAirMechCruiseMP());
				model.put("airmechFlank", lam.getAirMechFlankMP());
			} else {
				model.put("airmechCruise", "N/A");
				model.put("airmechFlank", "N/A");
			}
			lam.setConversionMode(LandAirMech.CONV_MODE_FIGHTER);
			model.put("safeThrust", lam.getWalkMP());
			model.put("maxThrust", lam.getRunMP());
		} else if (mech.hasETypeFlag(Entity.ETYPE_QUADVEE)) {
			final QuadVee qv = (QuadVee) mech;
			qv.setConversionMode(QuadVee.CONV_MODE_VEHICLE);
			model.put("qvConversionMass", testMech.getWeightMisc());
			model.put("qvType", Messages.getString("MovementType." + qv.getMovementModeAsString()));
			model.put("qvCruise", qv.getWalkMP());
			model.put("qvFlank", qv.getRunMPasString());
		}
		model.put("rightArmActuators", countArmActuators(mech, Mech.LOC_RARM));
		model.put("leftArmActuators", countArmActuators(mech, Mech.LOC_LARM));
	}
	
	private String countArmActuators(Mech mech, int location) {
		StringJoiner sj = new StringJoiner(", ");
		for (int act = Mech.ACTUATOR_SHOULDER; act <= Mech.ACTUATOR_HAND; act++) {
			if (mech.hasSystem(act, location)) {
				sj.add(mech.getRawSystemName(act));
			}
		}
		return sj.toString();
	}
	
	private void addVehicleData(Tank tank) {
		model.put("formatArmorRow", new FormatTableRowMethod(new int[] { 20, 10, 10},
				new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER }));
		model.put("formatEquipmentRow", new FormatTableRowMethod(new int[] { 30, 12, 12},
				new Justification[] { Justification.LEFT, Justification.CENTER, Justification.CENTER,
						Justification.CENTER, Justification.CENTER}));
		addBasicData(tank);
		addArmorAndStructure(tank);
		addEquipment(tank);
		addVehicleFluff(tank);
		model.put("isOmni", tank.isOmni());
		model.put("isVTOL", tank.hasETypeFlag(Entity.ETYPE_VTOL));
		model.put("isSuperheavy", tank.isSuperHeavy());
		model.put("isSupport", tank.isSupportVehicle());
		model.put("hasTurret", !tank.hasNoTurret());
		model.put("hasTurret2", !tank.hasNoDualTurret());
		model.put("moveType", Messages.getString("MovementType." + tank.getMovementModeAsString()));
		TestTank testTank = new TestTank(tank, verifier.mechOption, null);
		model.put("isMass", NumberFormat.getInstance().format(testTank.getWeightStructure()));
		model.put("engineName", stripNotes(tank.getEngine().getEngineName()));
		model.put("engineMass", NumberFormat.getInstance().format(testTank.getWeightEngine()));
		model.put("walkMP", tank.getWalkMP());
		model.put("runMP", tank.getRunMPasString());
		if (tank.getJumpMP() > 0) {
			model.put("jumpMP", tank.getJumpMP());
		}
		model.put("hsCount", testTank.getCountHeatSinks());
		model.put("hsMass", NumberFormat.getInstance().format(testTank.getWeightHeatSinks()));
		model.put("controlMass", testTank.getWeightControls());
		model.put("liftMass", testTank.getTankWeightLifting());
		model.put("amplifierMass", testTank.getWeightPowerAmp());
		model.put("turretMass", testTank.getTankWeightTurret());
		model.put("turretMass2", testTank.getTankWeightDualTurret());
		String atName = formatArmorType(tank, true);
		if (atName.length() > 0) {
			model.put("armorType", " (" + atName + ")");
		} else {
			model.put("armorType", "");
		}
		model.put("armorFactor", tank.getTotalOArmor());
		model.put("armorMass", NumberFormat.getInstance().format(testTank.getWeightArmor()));
		if (tank.isOmni()) {
			addFixedOmni(tank);
		}
	}
	
	private void addEntityFluff(Entity entity) {
		model.put("year", String.valueOf(entity.getYear()));
		model.put("cost", NumberFormat.getInstance().format(entity.getCost(false)));
		model.put("techRating", entity.getFullRatingName());
		if (entity.getFluff().getOverview().length() > 0) {
			model.put("fluffOverview", entity.getFluff().getOverview());
		}
		if (entity.getFluff().getCapabilities().length() > 0) {
			model.put("fluffCapabilities", entity.getFluff().getCapabilities());
		}
		if (entity.getFluff().getDeployment().length() > 0) {
			model.put("fluffDeployment", entity.getFluff().getDeployment());
		}
		if (entity.getFluff().getHistory().length() > 0) {
			model.put("fluffHistory", entity.getFluff().getHistory());
		}
	}
	
	private void addMechVeeAeroFluff(Entity entity) {
		addEntityFluff(entity);
		model.put("massDesc", (int) entity.getWeight()
				+ Messages.getString("TROView.tons"));
		// Prefix engine manufacturer
		model.put("engineDesc", stripNotes(entity.getEngine().getEngineName()));
		model.put("cruisingSpeed", entity.getWalkMP() * 10.8);
		model.put("maxSpeed", entity.getRunMP() * 10.8);
		model.put("armorDesc", formatArmorType(entity, false));
		Map<String, Integer> weaponCount = new HashMap<>();
		double podSpace = 0.0;
		for (Mounted m : entity.getEquipment()) {
			if (m.isOmniPodMounted()) {
				podSpace += m.getType().getTonnage(entity, m.getLocation());
			} else if (m.getType() instanceof WeaponType) {
				weaponCount.merge(m.getType().getName(), 1, Integer::sum);
			}
		}
		List<String> armaments = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : weaponCount.entrySet()) {
			armaments.add(String.format("%d %s", entry.getValue(), entry.getKey()));
		}
		if (podSpace > 0) {
			armaments.add(String.format(Messages.getString("TROView.podspace.format"), podSpace));
		}
		model.put("armamentList", armaments);
	}
	
	private void addMechFluff(Mech mech) {
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
		model.put("chassisDesc", chassisDesc);
		model.put("jjDesc", formatJJDesc(mech));
		model.put("jumpCapacity", mech.getJumpMP() * 30);
	}
	
	private void addVehicleFluff(Tank tank) {
		addMechVeeAeroFluff(tank);
		if (tank.getJumpMP() > 0) {
			model.put("jjDesc", Messages.getString("TROView.jjVehicle"));
			model.put("jumpCapacity", tank.getJumpMP() * 30);
		}
	}

	private String formatTechBase(Entity entity) {
		StringBuilder sb = new StringBuilder();
		if (entity.isMixedTech()) {
			sb.append(Messages.getString("TROView.Mixed"));
		} else if (entity.isClan()) {
			sb.append(Messages.getString("TROView.Clan"));
		} else {
			sb.append(Messages.getString("TROView.InnerSphere"));
		}
		sb.append(" (").append(entity.getStaticTechLevel().toString()).append(")");
		return sb.toString();
	}
	
	private String formatArmorType(Entity entity, boolean trim) {
		if (entity.hasPatchworkArmor()) {
			return EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PATCHWORK);
		}
		// Some types do not have armor on the first location, and others have only a single location
		int at = entity.getArmorType(Math.min(1, entity.locations() - 1));
		if (trim && (at == EquipmentType.T_ARMOR_STANDARD)) {
			return "";
		}
		String name = EquipmentType.getArmorTypeName(at);
		if (trim) {
			name = name.replace("-Fibrous", "").replace("-Aluminum", "");
		}
		return name;
	}
	private static final int[][] MECH_ARMOR_LOCS = {
			{Mech.LOC_HEAD}, {Mech.LOC_CT}, {Mech.LOC_RT, Mech.LOC_LT},
			{Mech.LOC_RARM, Mech.LOC_LARM}, {Mech.LOC_RLEG, Mech.LOC_CLEG, Mech.LOC_LLEG}
	};
	
	private static final int[][] MECH_ARMOR_LOCS_REAR = {
			{Mech.LOC_CT}, {Mech.LOC_RT, Mech.LOC_LT}
	};
	
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
	
	private void addArmorAndStructure(Mech mech) {
		model.put("structureValues", addArmorStructureEntries(mech,
				(en, loc) -> en.getOInternal(loc),
				MECH_ARMOR_LOCS));
		model.put("armorValues", addArmorStructureEntries(mech,
				(en, loc) -> en.getOArmor(loc),
				MECH_ARMOR_LOCS));
		model.put("rearArmorValues", addArmorStructureEntries(mech,
				(en, loc) -> en.getOArmor(loc, true),
				MECH_ARMOR_LOCS_REAR));
	}
	
	private void addArmorAndStructure(Tank tank) {
		if (tank.hasETypeFlag(Entity.ETYPE_SUPER_HEAVY_TANK)) {
			model.put("structureValues", addArmorStructureEntries(tank,
					(en, loc) -> en.getOInternal(loc),
					SH_TANK_ARMOR_LOCS));
			model.put("armorValues", addArmorStructureEntries(tank,
					(en, loc) -> en.getOArmor(loc),
					SH_TANK_ARMOR_LOCS));
		} else {
			model.put("structureValues", addArmorStructureEntries(tank,
					(en, loc) -> en.getOInternal(loc),
					TANK_ARMOR_LOCS));
			model.put("armorValues", addArmorStructureEntries(tank,
					(en, loc) -> en.getOArmor(loc),
					TANK_ARMOR_LOCS));
		}
	}
	
	/**
	 * Convenience method to format armor and structure values, consolidating right/left values into a single
	 * entry. In most cases the right and left armor values are the same, in which case only a single value
	 * is used. If the values do not match they are both (or all, in the case of tripod mech legs) given
	 * separated by slashes.
	 * 
	 * @param entity    The entity to collect structure or armor values for
	 * @param provider  The function that retrieves the armor or structure value for the entity and location
	 * @param locSets   An two-dimensional array that groups locations that should appear on the same line.
	 * 					Any location that is not legal for the unit (e.g. center leg on non-tripods) is
	 *                	ignored. If the first location in a group is illegal, the entire group is skipped.
	 * @return			A {@link Map} with the armor/structure value mapped to the abbreviation of each
	 * 					of the location keys.
	 */
	private Map<String, String> addArmorStructureEntries(Entity entity,
			BiFunction<Entity, Integer, Integer> provider, int[][] locSets) {
		Map<String, String> retVal = new HashMap<>();
		for (int[] locs : locSets) {
			if ((locs.length == 0) || (locs[0] >= entity.locations())) {
				continue;
			}
			String val = null;
			if (locs.length > 1) {
				for (int i = 1; i < locs.length; i++) {
					if ((locs[i] < entity.locations())
							&& (provider.apply(entity,  locs[i]) != provider.apply(entity, locs[0]))) {
						val = Arrays.stream(locs)
								.mapToObj(l -> String.valueOf(provider.apply(entity, l)))
								.collect(Collectors.joining("/"));
						break;
					}
				}
			}
			if (null == val) {
				val = String.valueOf(provider.apply(entity, locs[0]));
			}
			for (int loc : locs) {
				if (loc < entity.locations()) {
					retVal.put(entity.getLocationAbbr(loc), val);
				}
			}
		}
		return retVal;
	}
	
	private void addEquipment(Entity entity) {
		final int structure = entity.getStructureType();
		final Map<Integer, Map<EquipmentType, Integer>> equipment = new HashMap<>();
		for (Mounted m : entity.getEquipment()) {
			if (m.getLocation() < 0) {
				continue;
			}
			if (!m.getType().isHittable()) {
				if ((structure != EquipmentType.T_STRUCTURE_UNKNOWN)
						&& (EquipmentType.getStructureType(m.getType()) == structure)) {
					continue;
				}
				if (entity.getArmorType(m.getLocation()) == EquipmentType.getArmorType(m.getType())) {
					continue;
				}
			}
			if (m.isOmniPodMounted() || !entity.isOmni()) {
				equipment.putIfAbsent(m.getLocation(), new HashMap<>());
				equipment.get(m.getLocation()).merge(m.getType(), 1, Integer::sum);
			}
		}
		final List<Map<String, Object>> eqList = new ArrayList<>();
		for (Integer loc : equipment.keySet()) {
			for (Map.Entry<EquipmentType, Integer> entry : equipment.get(loc).entrySet()) {
				final EquipmentType eq = entry.getKey();
				final int count = equipment.get(loc).get(eq);
				String name = stripNotes(eq.getName());
				if (eq instanceof AmmoType) {
					name = String.format("%s (%d)", name,
							((AmmoType) eq).getShots() * count);
				} else if (count > 1) {
					name = String.format("%d %ss", count, eq.getName());
				}
				Map<String, Object> fields = new HashMap<>();
				fields.put("name", name);
				fields.put("tonnage", eq.getTonnage(entity) * count);
				if (eq instanceof WeaponType) {
					fields.put("heat", ((WeaponType) eq).getHeat() * count);
				} else {
					fields.put("heat", "-");
				}
				if (eq.isSpreadable()) {
					Map<Integer, Integer> byLoc = getSpreadableLocations(entity, eq);
					final StringJoiner locs = new StringJoiner("/");
					final StringJoiner crits = new StringJoiner("/");
					byLoc.forEach((l, c) -> {
						locs.add(entity.getLocationAbbr(l));
						crits.add(String.valueOf(c));
					});
					fields.put("location", locs.toString());
					fields.put("slots", crits.toString());
				} else {
					fields.put("location", entity.getLocationAbbr(loc));
					fields.put("slots", eq.getCriticals(entity) * count);
				}
				eqList.add(fields);
			}
		}
		model.put("equipment", eqList);
	}
	
	private Map<Integer, Integer> getSpreadableLocations(final Entity entity, final EquipmentType eq) {
		Map<Integer, Integer> retVal = new HashMap<>();
		for (int loc = 0; loc < entity.locations(); loc++) {
			for (int slot = 0; slot < entity.getNumberOfCriticals(loc); slot++) {
				final CriticalSlot crit = entity.getCritical(loc, slot);
				if ((crit != null) && (crit.getMount() != null) && (crit.getMount().getType() == eq)) {
					retVal.merge(loc, 1, Integer::sum);
				}
			}
		}
		return retVal;
	}
	
	private void addFixedOmni(final Entity entity) {
		final List<Map<String, Object>> fixedList = new ArrayList<>();
		for (int loc = 0; loc < entity.locations(); loc++) {
			int remaining = 0;
			Map<String, Integer> fixedCount = new HashMap<>();
			Map<String, Double> fixedWeight = new HashMap<>();
			for (int slot = 0; slot < entity.getNumberOfCriticals(loc); slot++) {
				CriticalSlot crit = entity.getCritical(loc, slot);
				if (null == crit) {
					remaining++;
				} else if ((crit.getType() == CriticalSlot.TYPE_SYSTEM)
						&& showFixedSystem(entity, crit.getIndex(), loc)) {
					fixedCount.merge(getSystemName(entity, crit.getIndex()), 1, Integer::sum);
				} else if (crit.getMount() != null) {
					if (crit.getMount().isOmniPodMounted()) {
						remaining++;
					} else {
						String key = stripNotes(crit.getMount().getType().getName());
						fixedCount.merge(key, 1, Integer::sum);
						fixedWeight.merge(key, crit.getMount().getType().getTonnage(entity), Double::sum);
					}
				}
			}
			Map<String, Object> row;
			if (fixedCount.isEmpty()) {
				row = new HashMap<>();
				row.put("location", entity.getLocationName(loc));
				row.put("equipment", "None");
				row.put("remaining", remaining);
				row.put("tonnage", 0.0);
				fixedList.add(row);
			} else {
				boolean firstLine = true;
				for (Map.Entry<String, Integer> entry : fixedCount.entrySet()) {
					row = new HashMap<>();
					if (firstLine) {
						row.put("location", entity.getLocationName(loc));
						row.put("remaining", remaining);
						firstLine = false;
					} else {
						row.put("location", "");
						row.put("remaining", "");
					}
					if (entry.getValue() > 1) {
						row.put("equipment", entry.getValue() + " " + entry.getKey());
					} else {
						row.put("equipment", entry.getKey());
					}
					row.put("tonnage", fixedWeight.get(entry.getKey()));
					fixedList.add(row);
				}
			}
		}
		model.put("fixedEquipment", fixedList);
	}
	
	private boolean showFixedSystem(Entity entity, int index, int loc) {
		if (entity.hasETypeFlag(Entity.ETYPE_MECH)) {
			return ((index != Mech.SYSTEM_COCKPIT) || (loc != Mech.LOC_HEAD))
					&& ((index != Mech.SYSTEM_SENSORS) || (loc != Mech.LOC_HEAD))
					&& ((index != Mech.SYSTEM_LIFE_SUPPORT) || (loc != Mech.LOC_HEAD))
					&& ((index != Mech.SYSTEM_ENGINE) || (loc != Mech.LOC_CT))
					&& (index != Mech.SYSTEM_GYRO)
					&& (index != Mech.ACTUATOR_SHOULDER)
					&& (index != Mech.ACTUATOR_UPPER_ARM)
					&& (index != Mech.ACTUATOR_LOWER_ARM)
					&& (index != Mech.ACTUATOR_HAND)
					&& (index != Mech.ACTUATOR_HIP)
					&& (index != Mech.ACTUATOR_UPPER_LEG)
					&& (index != Mech.ACTUATOR_LOWER_LEG)
					&& (index != Mech.ACTUATOR_FOOT);
		}
		return true;
	}
	
	private String getSystemName(Entity entity, int index) {
		if (entity.hasETypeFlag(Entity.ETYPE_MECH)) {
			// Here we're only concerned with engines that take extra critical slots in the side torso
			if (index == Mech.SYSTEM_ENGINE) {
				StringBuilder sb = new StringBuilder();
				if (entity.getEngine().hasFlag(Engine.LARGE_ENGINE)) {
					sb.append("Large ");
				}
				switch (entity.getEngine().getEngineType()) {
				case Engine.XL_ENGINE:
					sb.append("XL");
					break;
				case Engine.LIGHT_ENGINE:
					sb.append("Light");
					break;
				case Engine.XXL_ENGINE:
					sb.append("XXL");
					break;
				}
				sb.append(" Engine");
				return sb.toString();
			} else {
				return ((Mech) entity).getRawSystemName(index);
			}
		}
		return "Unknown System";
	}
	
	private String formatJJDesc(Mech mech) {
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
	
	enum Justification {
		LEFT ((str, w) -> String.format("%-" + w + "s", str)),
		CENTER ((str, w) -> {
			if (w > str.length()) {
				int rightPadding = Math.max(0, (w - str.length()) / 2);
				if (rightPadding > 0) {
					str = String.format("%-" + (w - rightPadding) + "s", str);
				}
				return String.format("%" + w + "s", str);
			}
			return str;
		}),
		RIGHT ((str, w) -> String.format("%" + w + "s", str));
		
		final private BiFunction<String, Integer, String> pad;
		private Justification(BiFunction<String, Integer, String> pad) {
			this.pad = pad;
		}
		
		public String padString(String str, int fieldWidth) {
			if (fieldWidth > 0) {
				return pad.apply(str, fieldWidth);
			} else {
				return str;
			}
		}
	};

	/**
	 * Removes parenthetical and bracketed notes from a String
	 * @param str The String to process
	 * @return    The same String with notes removed
	 */
	private String stripNotes(String str) {
		return str.replaceAll("\\s+\\[.*?\\]", "")
				.replaceAll("\\s+\\(.*?\\)", "");
	}

	static class FormatTableRowMethod implements TemplateMethodModelEx {
		final private int[] colWidths;
		final private Justification[] justification;
		
		public FormatTableRowMethod(int[] widths, Justification[] justify) {
			colWidths = new int[widths.length];
			justification = new Justification[widths.length];
			for (int i = 0; i < widths.length; i++) {
				colWidths[i] = widths[i];
				if (i < justify.length) {
					justification[i] = justify[i];
				} else {
					justification[i] = Justification.LEFT;
				}
			}
			System.arraycopy(widths, 0, colWidths, 0, widths.length);
		}
		
		@Override
		public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
			StringBuilder sb = new StringBuilder();
			int col = 0;
			for (Object o : arguments) {
				if (col < colWidths.length) {
					sb.append(justification[col].padString(o.toString(), colWidths[col]));
				} else {
					sb.append(o);
				}
				col++;
			}
			return sb.toString();
		}
		
	}
}
