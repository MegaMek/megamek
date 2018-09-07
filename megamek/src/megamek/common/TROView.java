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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import megamek.client.ui.Messages;
import megamek.common.annotations.Nullable;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.util.MegaMekFile;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestMech;

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

	public TROView(Entity entity) {
		String templateFileName = null;
		if (entity.hasETypeFlag(Entity.ETYPE_MECH)) {
			templateFileName = "mech_tro.ftlh";
			addMechData((Mech) entity);
		}
		if (null != templateFileName) {
			try {
				template = TemplateConfiguration.getInstance().getTemplate(templateFileName);
			} catch (IOException e) {
				DefaultMmLogger.getInstance().error(getClass(), "TROView(Entity)", e);
			}
		}
	}
	
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
		model.put("fullName", entity.getShortNameRaw());
		model.put("chassis", entity.getChassis());
		model.put("techBase", formatTechBase(entity));
		model.put("tonnage", NumberFormat.getInstance().format(entity.getWeight())
				+ Messages.getString("TROView.tons"));
		model.put("battleValue", NumberFormat.getInstance().format(entity.calculateBattleValue()));
	}


	private void addMechData(Mech mech) {
		addBasicData(mech);
		addArmorAndStructure(mech);
		addEquipment(mech);
		model.put("isOmni", mech.isOmni());
		model.put("isQuad", mech.entityIsQuad());
		model.put("isTripod", mech.hasETypeFlag(Entity.ETYPE_TRIPOD_MECH));
		TestMech testMech = new TestMech(mech, verifier.mechOption, null);
		model.put("structureName", mech.getStructureType() == EquipmentType.T_STRUCTURE_STANDARD?
				"" : EquipmentType.getStructureTypeName(mech.getStructureType()));
		model.put("isMass", NumberFormat.getInstance().format(testMech.getWeightStructure()));
		model.put("engineName", mech.getEngine().getEngineName().replaceAll("\\s?\\(.*\\)", ""));
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
		model.put("armorFactor", mech.getTotalOArmor());
		model.put("armorMass", NumberFormat.getInstance().format(testMech.getWeightArmor()));
		if (mech.isOmni()) {
			addFixedOmni(mech);
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
	
	private void addArmorAndStructure(Entity entity) {
		Map<String, Number> structure = new HashMap<>();
		Map<String, Number> armor = new HashMap<>();
		Map<String, Number> rearArmor = new HashMap<>();
		for (int loc = 0; loc < entity.locations(); loc++) {
			structure.put(entity.getLocationAbbr(loc), entity.getOInternal(loc));
			armor.put(entity.getLocationAbbr(loc), entity.getOArmor(loc));
			if (entity.hasRearArmor(loc)) {
				rearArmor.put(entity.getLocationAbbr(loc), entity.getOArmor(loc, true));
			}
		}
		model.put("structureValues", structure);
		model.put("armorValues", armor);
		model.put("rearArmorValues", rearArmor);
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
				String name = eq.getName().replaceAll("\\s?\\[.*\\]", "");
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
						fixedCount.merge(crit.getMount().getType().getName().replaceAll("\\[.*\\]", ""),
								1, Integer::sum);
					}
				}
			}
			Map<String, Object> row;
			if (fixedCount.isEmpty()) {
				row = new HashMap<>();
				row.put("location", entity.getLocationName(loc));
				row.put("equipment", "None");
				row.put("remaining", remaining);
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
}
