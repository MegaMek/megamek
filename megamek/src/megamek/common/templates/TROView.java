/*
* MegaMek -
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.common.templates;

import freemarker.template.Template;
import freemarker.template.TemplateMethodModelEx;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.Quirks;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.verifier.BayData;
import megamek.common.verifier.EntityVerifier;
import org.apache.logging.log4j.LogManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Fills in a template to produce a unit summary in TRO format.
 *
 * @author Neoancient
 */
public class TROView {

    private Template template;
    private final Map<String, Object> model = new HashMap<>();
    private final EntityVerifier verifier = EntityVerifier
            .getInstance(new MegaMekFile(Configuration.unitsDir(), EntityVerifier.CONFIG_FILENAME).getFile());

    private boolean includeFluff = true;

    protected TROView() {
    }

    public static TROView createView(Entity entity, boolean html) {
        TROView view;
        if (entity.hasETypeFlag(Entity.ETYPE_MECH)) {
            view = new MechTROView((Mech) entity);
        } else if (entity.hasETypeFlag(Entity.ETYPE_PROTOMECH)) {
            view = new ProtomechTROView((Protomech) entity);
        } else if (entity.hasETypeFlag(Entity.ETYPE_SUPPORT_TANK)
                || (entity.hasETypeFlag(Entity.ETYPE_SUPPORT_VTOL))) {
            view = new SupportVeeTROView((Tank) entity);
        } else if (entity.hasETypeFlag(Entity.ETYPE_TANK)) {
            view = new VehicleTROView((Tank) entity);
        } else if (entity.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            view = new SmallCraftDropshipTROView((SmallCraft) entity);
        } else if (entity.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
            view = new CapitalShipTROView((Jumpship) entity);
        } else if (entity.hasETypeFlag(Entity.ETYPE_AERO)) {
            view = new AeroTROView((Aero) entity);
        } else if (entity.hasETypeFlag(Entity.ETYPE_BATTLEARMOR)) {
            view = new BattleArmorTROView((BattleArmor) entity);
        } else if (entity.hasETypeFlag(Entity.ETYPE_INFANTRY)) {
            view = new InfantryTROView((Infantry) entity);
        } else {
            view = new TROView();
        }
        if (null != view.getTemplateFileName(html)) {
            try {
                view.template = TemplateConfiguration.getInstance()
                        .getTemplate("tro/" + view.getTemplateFileName(html));
            } catch (final IOException e) {
                LogManager.getLogger().error("", e);
            }
            view.initModel(view.verifier);
        }
        return view;
    }

    protected @Nullable String getTemplateFileName(boolean html) {
        return null;
    }

    protected void setModelData(String key, Object data) {
        model.put(key, data);
    }

    protected Object getModelData(String key) {
        return model.get(key);
    }

    protected void initModel(EntityVerifier verifier) {

    }

    /**
     * Uses the template and supplied {@link Entity} to generate a TRO document
     *
     * @return The generated document. Returns {@code null} if there was an error
     *         that prevented the document from being generated. Check logs for
     *         reason.
     */
    @Nullable
    public String processTemplate() {
        if (null != template) {
            model.put("includeFluff", includeFluff);
            try (final ByteArrayOutputStream os = new ByteArrayOutputStream();
                 final Writer out = new OutputStreamWriter(os)) {
                template.process(model, out);
                return os.toString();
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        }
        return null;
    }

    protected void addBasicData(Entity entity) {
        model.put("formatBasicDataRow", new FormatTableRowMethod(new int[] { 30, 20, 5 },
                new Justification[] { Justification.LEFT, Justification.LEFT, Justification.RIGHT }));
        model.put("fullName", entity.getShortNameRaw());
        model.put("chassis", entity.getChassis());
        model.put("techBase", formatTechBase(entity));
        model.put("tonnage", NumberFormat.getInstance().format(entity.getWeight()));
        model.put("battleValue", NumberFormat.getInstance().format(entity.calculateBattleValue()));
        model.put("cost", NumberFormat.getInstance().format(entity.getCost(false)));

        final StringJoiner quirksList = new StringJoiner(", ");
        final Quirks quirks = entity.getQuirks();
        for (final Enumeration<IOptionGroup> optionGroups = quirks.getGroups(); optionGroups.hasMoreElements();) {
            final IOptionGroup group = optionGroups.nextElement();
            if (quirks.count(group.getKey()) > 0) {
                for (final Enumeration<IOption> options = group.getOptions(); options.hasMoreElements();) {
                    final IOption option = options.nextElement();
                    if ((option != null) && option.booleanValue()) {
                        quirksList.add(option.getDisplayableNameWithValue());
                    }
                }
            }
        }
        if (quirksList.length() > 0) {
            model.put("quirks", quirksList.toString());
        }

    }

    protected void addEntityFluff(Entity entity) {
        model.put("year", String.valueOf(entity.getYear()));
        model.put("techRating", entity.getFullRatingName());
        if (!entity.getFluff().getOverview().isBlank()) {
            model.put("fluffOverview", entity.getFluff().getOverview());
        }

        if (!entity.getFluff().getCapabilities().isBlank()) {
            model.put("fluffCapabilities", entity.getFluff().getCapabilities());
        }

        if (!entity.getFluff().getDeployment().isBlank()) {
            model.put("fluffDeployment", entity.getFluff().getDeployment());
        }

        if (!entity.getFluff().getHistory().isBlank()) {
            model.put("fluffHistory", entity.getFluff().getHistory());
        }

        if (!entity.getFluff().getManufacturer().isBlank()) {
            model.put("manufacturerDesc", entity.getFluff().getManufacturer());
        }

        if (!entity.getFluff().getPrimaryFactory().isBlank()) {
            model.put("factoryDesc", entity.getFluff().getPrimaryFactory());
        }
    }

    /**
     * Builds the fluff name for a system component.
     *
     * @param system
     *            The system component
     * @param fluff
     *            The {@link Entity}'s fluff object
     * @param altText
     *            Alternate text that will be used if neither fluff field is set.
     * @return The fluff display name, which consists of the manufacturer and the
     *         model separated by a space. If either is missing it is left out.
     */
    public static String formatSystemFluff(EntityFluff.System system, EntityFluff fluff, Supplier<String> altText) {
        final StringJoiner sj = new StringJoiner(" ");
        if (!fluff.getSystemManufacturer(system).isBlank()) {
            sj.add(fluff.getSystemManufacturer(system));
        }

        if (!fluff.getSystemModel(system).isBlank()) {
            sj.add(fluff.getSystemModel(system));
        }

        return sj.toString().isBlank() ? altText.get() : sj.toString();
    }

    protected void addMechVeeAeroFluff(Entity entity) {
        addEntityFluff(entity);
        model.put("massDesc", NumberFormat.getInstance().format(entity.getWeight())
                + Messages.getString(entity.getWeight() == 1.0 ? "TROView.ton" : "TROView.tons"));
        if (entity.hasEngine()) {
            model.put("engineDesc", formatSystemFluff(EntityFluff.System.ENGINE, entity.getFluff(),
                    () -> stripNotes(entity.getEngine().getEngineName())));
        } else {
            model.put("engineDesc", "None");
        }
        if (!entity.isAero()) {
            model.put("cruisingSpeed", entity.getWalkMP() * 10.8);
            model.put("maxSpeed", entity.getRunMP() * 10.8);
        }
        if (entity.isMek() || (entity.isProtoMek() && entity.getOriginalJumpMP() > 0)) {
            model.put("jumpDesc", formatSystemFluff(EntityFluff.System.JUMPJET, entity.getFluff(),
                    () -> Messages.getString("TROView.Unknown")));
        }
        model.put("armorDesc",
                formatSystemFluff(EntityFluff.System.ARMOR, entity.getFluff(), () -> formatArmorType(entity, false)));
        final Map<String, Integer> weaponCount = new HashMap<>();
        double podSpace = 0.0;
        for (final Mounted m : entity.getEquipment()) {
            if (m.isOmniPodMounted()) {
                podSpace += m.getTonnage();
            } else if (m.getType() instanceof WeaponType) {
                weaponCount.merge(m.getName(), 1, Integer::sum);
            }
        }
        final List<String> armaments = new ArrayList<>();
        for (final Map.Entry<String, Integer> entry : weaponCount.entrySet()) {
            armaments.add(String.format("%d %s", entry.getValue(), entry.getKey()));
        }
        if (podSpace > 0) {
            armaments.add(String.format(Messages.getString("TROView.podspace.format"), podSpace));
        }
        model.put("armamentList", armaments);
        model.put("communicationDesc", formatSystemFluff(EntityFluff.System.COMMUNICATIONS, entity.getFluff(),
                () -> Messages.getString("TROView.Unknown")));
        model.put("targetingDesc", formatSystemFluff(EntityFluff.System.TARGETING, entity.getFluff(),
                () -> Messages.getString("TROView.Unknown")));
    }

    private String formatTechBase(Entity entity) {
        final StringBuilder sb = new StringBuilder();
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

    public static String formatArmorType(Entity entity, boolean trim) {
        if (entity.hasETypeFlag(Entity.ETYPE_SUPPORT_TANK) || entity.hasETypeFlag(Entity.ETYPE_SUPPORT_VTOL)
                || entity.hasETypeFlag(Entity.ETYPE_FIXED_WING_SUPPORT)) {
            return "BAR " + entity.getBARRating(Tank.LOC_FRONT);
        }
        if (entity.hasPatchworkArmor()) {
            return EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_PATCHWORK);
        }
        // Some types do not have armor on the first location, and others have only a
        // single location
        final int at = entity.getArmorType(Math.min(1, entity.locations() - 1));
        if (trim && (at == EquipmentType.T_ARMOR_STANDARD)) {
            return "";
        }
        String name = EquipmentType.getArmorTypeName(at);
        if (trim) {
            name = name.replace("-Fibrous", "").replace("-Aluminum", "");
            if (at == EquipmentType.T_ARMOR_EDP) {
                name = "EDP";
            }
        }
        return name;
    }

    protected String formatArmorType(int at, boolean trim) {
        // Some types do not have armor on the first location, and others have only a
        // single location
        if (trim && (at == EquipmentType.T_ARMOR_STANDARD)) {
            return "";
        }
        String name = EquipmentType.getArmorTypeName(at);
        if (trim) {
            name = name.replace("-Fibrous", "").replace("-Aluminum", "");
        }
        return name;
    }

    /**
     * Convenience method to format armor and structure values, consolidating
     * right/left values into a single entry. In most cases the right and left armor
     * values are the same, in which case only a single value is used. If the values
     * do not match they are both (or all, in the case of tripod mech legs) given
     * separated by slashes.
     *
     * @param entity
     *            The entity to collect structure or armor values for
     * @param provider
     *            The function that retrieves the armor or structure value for the
     *            entity and location
     * @param locSets
     *            A two-dimensional array that groups locations that should appear
     *            on the same line. Any location that is not legal for the unit
     *            (e.g. center leg on non-tripods) is ignored. If the first location
     *            in a group is illegal, the entire group is skipped.
     * @return A {@link Map} with the armor/structure value mapped to the
     *         abbreviation of each of the location keys.
     */
    protected Map<String, String> addArmorStructureEntries(Entity entity, BiFunction<Entity, Integer, Integer> provider,
            int[][] locSets) {
        final Map<String, String> retVal = new HashMap<>();
        for (final int[] locs : locSets) {
            if ((locs.length == 0) || (locs[0] >= entity.locations())) {
                continue;
            }
            String val = null;
            if (locs.length > 1) {
                for (int i = 1; i < locs.length; i++) {
                    if ((locs[i] < entity.locations())
                            && ((!provider.apply(entity, locs[i]).equals(provider.apply(entity, locs[0])))
                                    || !entity.hasETypeFlag(Entity.ETYPE_MECH))) {
                        val = Arrays.stream(locs).filter(l -> l < entity.locations())
                                .mapToObj(l -> String.valueOf(provider.apply(entity, l)))
                                .collect(Collectors.joining("/"));
                        break;
                    }
                }
            }
            if (null == val) {
                val = String.valueOf(provider.apply(entity, locs[0]));
            }
            for (final int loc : locs) {
                if (loc < entity.locations()) {
                    retVal.put(entity.getLocationAbbr(loc), val);
                }
            }
        }
        return retVal;
    }

    protected Map<String, String> addPatchworkATs(Entity entity, int[][] locSets) {
        final Map<String, String> retVal = new HashMap<>();
        for (final int[] locs : locSets) {
            if ((locs.length == 0) || (locs[0] >= entity.locations())) {
                continue;
            }
            String val = null;
            if (locs.length > 1) {
                for (int i = 1; i < locs.length; i++) {
                    if ((locs[i] < entity.locations())
                            && (entity.getArmorType(locs[i]) != entity.getArmorType(locs[0]))) {
                        val = Arrays.stream(locs).mapToObj(l -> formatArmorType(entity.getArmorType(l), true))
                                .collect(Collectors.joining("/"));
                        break;
                    }
                }
            }
            if (null == val) {
                val = formatArmorType(entity.getArmorType(locs[0]), true);
            }
            for (final int loc : locs) {
                if (loc < entity.locations()) {
                    retVal.put(entity.getLocationAbbr(loc), val);
                }
            }
        }
        return retVal;
    }

    protected int addEquipment(Entity entity) {
        return addEquipment(entity, true);
    }

    /**
     * Test for whether the mount should be included in the equipment inventory section.
     *
     * @param mount        The equipment mount
     * @param includeAmmo  Whether to include ammo in the list
     * @return             Whether to list the equipment in the inventory section
     */
    protected boolean skipMount(Mounted mount, boolean includeAmmo) {
        return mount.getLocation() < 0
                || mount.isWeaponGroup()
                || (!includeAmmo && (mount.getType() instanceof AmmoType));
    }

    protected int addEquipment(Entity entity, boolean includeAmmo) {
        final int structure = entity.getStructureType();
        final Map<String, Map<EquipmentKey, Integer>> equipment = new HashMap<>();
        int nameWidth = 20;
        EquipmentKey eqk;
        for (final Mounted m : entity.getEquipment()) {
            if (skipMount(m, includeAmmo)) {
                continue;
            }
            // Skip armor and structure
            if (!m.getType().isHittable() && (m.getLocation() >= 0)) {
                if ((structure != EquipmentType.T_STRUCTURE_UNKNOWN)
                        && (EquipmentType.getStructureType(m.getType()) == structure)) {
                    continue;
                }
                if ((m.getLocation() >= 0)
                        && (entity.getArmorType(m.getLocation()) == EquipmentType.getArmorType(m.getType()))) {
                    continue;
                }
            }
            if (m.isOmniPodMounted() || !entity.isOmni()) {
                final String loc = formatLocationTableEntry(entity, m);
                equipment.putIfAbsent(loc, new HashMap<>());
                eqk = new EquipmentKey(m.getType(), m.getSize(), m.isArmored(), m.isInternalBomb());
                equipment.get(loc).merge(eqk,1, Integer::sum);
            }
        }
        final List<Map<String, Object>> eqList = new ArrayList<>();
        for (final String loc : equipment.keySet()) {
            for (final Map.Entry<EquipmentKey, Integer> entry : equipment.get(loc).entrySet()) {
                final EquipmentType eq = entry.getKey().getType();
                final int count = equipment.get(loc).get(entry.getKey());
                String name = stripNotes(entry.getKey().name());
                if (entry.getKey().isArmored()) {
                    name += " (Armored)";
                }
                if (entry.getKey().internalBomb) {
                    name += " (Int. Bay)";
                }
                if (eq instanceof AmmoType) {
                    name = String.format("%s (%d)", name, ((AmmoType) eq).getShots() * count);
                } else if (count > 1) {
                    name = String.format("%d %s", count, name);
                }
                final Map<String, Object> fields = new HashMap<>();
                fields.put("name", name);
                if (name.length() >= nameWidth) {
                    nameWidth = name.length() + 1;
                }
                fields.put("tonnage", eq.getTonnage(entity, entry.getKey().getSize()) * count);
                if (eq instanceof WeaponType) {
                    fields.put("heat", eq.getHeat());
                    fields.put("srv", (int) ((WeaponType) eq).getShortAV());
                    fields.put("mrv", (int) ((WeaponType) eq).getMedAV());
                    fields.put("lrv", (int) ((WeaponType) eq).getLongAV());
                    fields.put("erv", (int) ((WeaponType) eq).getExtAV());
                } else {
                    fields.put("heat", "-");
                    fields.put("srv", "-");
                    fields.put("mrv", "-");
                    fields.put("lrv", "-");
                    fields.put("erv", "-");
                }
                if (eq.isSpreadable()) {
                    final Map<Integer, Integer> byLoc = getSpreadableLocations(entity, eq);
                    final StringJoiner locs = new StringJoiner("/");
                    final StringJoiner crits = new StringJoiner("/");
                    byLoc.forEach((l, c) -> {
                        locs.add(entity.getLocationAbbr(l));
                        crits.add(String.valueOf(c));
                    });
                    fields.put("location", locs.toString());
                    fields.put("slots", crits.toString());
                } else {
                    fields.put("location", loc);
                    fields.put("slots", eq.getCriticals(entity, entry.getValue()) * count);
                }
                eqList.add(fields);
            }
        }
        model.put("equipment", eqList);
        return nameWidth;
    }

    private Map<Integer, Integer> getSpreadableLocations(final Entity entity, final EquipmentType eq) {
        final Map<Integer, Integer> retVal = new HashMap<>();
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

    protected void addFixedOmni(final Entity entity) {
        double fixedTonnage = 0.0;
        final List<Map<String, Object>> fixedList = new ArrayList<>();
        for (int loc = 0; loc < entity.locations(); loc++) {
            if (entity.isAero() && (loc == Aero.LOC_WINGS)) {
                break;
            }
            int remaining = 0;
            final Map<String, Integer> fixedCount = new HashMap<>();
            final Map<String, Double> fixedWeight = new HashMap<>();
            for (int slot = 0; slot < entity.getNumberOfCriticals(loc); slot++) {
                final CriticalSlot crit = entity.getCritical(loc, slot);
                if (null == crit) {
                    remaining++;
                } else if ((crit.getType() == CriticalSlot.TYPE_SYSTEM)
                        && showFixedSystem(entity, crit.getIndex(), loc)) {
                    fixedCount.merge(getSystemName(entity, crit.getIndex()), 1, Integer::sum);
                } else if (crit.getMount() != null) {
                    if (crit.getMount().isOmniPodMounted()) {
                        remaining++;
                    } else if (!crit.getMount().isWeaponGroup()) {
                        final String key = stripNotes(crit.getMount().getName());
                        fixedCount.merge(key, 1, Integer::sum);
                        fixedWeight.merge(key, crit.getMount().getTonnage(), Double::sum);
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
                row.put("heat", "-");
                row.put("srv", "-");
                row.put("mrv", "-");
                row.put("lrv", "-");
                row.put("erv", "-");
                fixedList.add(row);
            } else {
                boolean firstLine = true;
                for (final Map.Entry<String, Integer> entry : fixedCount.entrySet()) {
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

                    if (fixedWeight.containsKey(entry.getKey())) {
                        // Not valid for mech systems
                        row.put("tonnage", fixedWeight.get(entry.getKey()));
                        fixedTonnage += fixedWeight.get(entry.getKey());
                    } else {
                        row.put("tonnage", "");
                    }

                    // FIXME : I am not properly implemented, this is a temporary fix for testing
                    // FIXME : and needs to be fixed.
                    row.put("heat", "-");
                    row.put("srv", "-");
                    row.put("mrv", "-");
                    row.put("lrv", "-");
                    row.put("erv", "-");

                    fixedList.add(row);
                }
            }
        }
        model.put("fixedEquipment", fixedList);
        model.put("fixedTonnage", fixedTonnage);
    }

    protected void addTransportBays(Entity entity) {
        final List<Map<String, Object>> bays = new ArrayList<>();
        for (final Bay bay : entity.getTransportBays()) {
            if (bay.isQuarters()) {
                continue;
            }
            final BayData bayData = BayData.getBayType(bay);
            if (null != bayData) {
                final Map<String, Object> bayRow = new HashMap<>();
                bayRow.put("name", bayData.getDisplayName());
                if (bayData.isCargoBay()) {
                    bayRow.put("size", bay.getCapacity() + Messages.getString("TROView.tons"));
                } else {
                    bayRow.put("size", (int) bay.getCapacity());
                }
                bayRow.put("doors", bay.getDoors());
                bays.add(bayRow);
            } else {
                LogManager.getLogger().warn("Could not determine bay type for " + bay);
            }
        }
        setModelData("bays", bays);
    }

    /**
     * Used to determine whether system crits should be shown when detailing fixed
     * equipment in an omni unit. By default this is false, but mechs override it to
     * show some systems.
     *
     * @param entity
     *            The unit the TRO is for
     * @param index
     *            The system index of the critical slot
     * @param loc
     *            The slot location
     * @return Whether to show this as a fixed system in an omni configuration
     */
    protected boolean showFixedSystem(Entity entity, int index, int loc) {
        return false;
    }

    /**
     * Used to show the name of fixed system critical slots in an omni unit. This is
     * only used for Mechs, and returns a default value of "Unknown System" for
     * other units.
     *
     * @param entity
     *            The unit the TRO is for
     * @param index
     *            The system index of the critical slot
     * @return The name of the system to display in the fixed equipment table
     */
    protected String getSystemName(Entity entity, int index) {
        return "Unknown System";
    }

    /**
     * Formats displayable location name for use in equipment table. The format of
     * the name can vary by unit type due to available space based on number of
     * columns, and in some cases the official TROs have different location names
     * than the ones used by MM.
     *
     * @param entity
     *            The entity the TRO is created for
     * @param mounted
     *            The mounted equipment
     * @return The location name to use in the table.
     */
    protected String formatLocationTableEntry(Entity entity, Mounted mounted) {
        // Default: location abbreviation
        return entity.getLocationAbbr(mounted.getLocation());
    }

    /**
     * Formats {@link Transporter} to display as a row in an equipment table. Any
     * other than bays and troop space are skipped to avoid showing BA handles and
     * such.
     *
     * @param transporter
     *            The transporter to show.
     * @param loc
     *            The location name to display on the table.
     * @return A map of values used by the equipment tables (omni fixed and
     *         pod/non-omni). Returns {@code null} for a type of {@link Transporter}
     *         that should not be shown.
     */
    protected @Nullable Map<String, Object> formatTransporter(Transporter transporter, String loc) {
        final Map<String, Object> retVal = new HashMap<>();
        if (transporter instanceof TroopSpace) {
            retVal.put("name", Messages.getString("TROView.TroopSpace"));
            retVal.put("tonnage", transporter.getUnused());
        } else if (transporter instanceof Bay) {
            final BayData bayType = BayData.getBayType((Bay) transporter);
            retVal.put("name", bayType.getDisplayName());
            retVal.put("tonnage", bayType.getWeight() * transporter.getUnused());
        } else {
            return null;
        }
        retVal.put("equipment", retVal.get("name"));
        retVal.put("location", loc);
        retVal.put("slots", 1);
        retVal.put("heat", "-");
        return retVal;
    }

    protected enum Justification {
        LEFT((str, w) -> String.format("%-" + w + "s", str)), CENTER((str, w) -> {
            if (w > str.length()) {
                final int rightPadding = Math.max(0, (w - str.length()) / 2);
                if (rightPadding > 0) {
                    str = String.format("%-" + (w - rightPadding) + "s", str);
                }
                return String.format("%" + w + "s", str);
            }
            return str;
        }), RIGHT((str, w) -> String.format("%" + w + "s", str));

        final private BiFunction<String, Integer, String> pad;

        Justification(BiFunction<String, Integer, String> pad) {
            this.pad = pad;
        }

        public String padString(String str, int fieldWidth) {
            if (fieldWidth > 0) {
                return pad.apply(str, fieldWidth);
            } else {
                return str;
            }
        }
    }

    /**
     * Removes parenthetical and bracketed notes from a String, with the exception
     * of parenthetical notes that begin with a digit. These are assumed to be
     * a marker of equipment size and left intact.
     *
     * @param str The String to process
     * @return     The same String with notes removed
     */
    protected String stripNotes(String str) {
        return str.replaceAll("\\s+\\[.*?]", "")
                .replaceAll("\\s+\\([^\\d].*?\\)", "");
    }

    protected static class FormatTableRowMethod implements TemplateMethodModelEx {
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
        public Object exec(List arguments) {
            final StringBuilder sb = new StringBuilder();
            int col = 0;
            for (final Object o : arguments) {
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

    /**
     * Sets whether to include the fluff section when processing the template
     *
     * @param includeFluff
     *            Whether to include the fluff section
     */
    public void setIncludeFluff(boolean includeFluff) {
        this.includeFluff = includeFluff;
    }

    /**
     *
     * @return Whether the fluff section will be included when processing the
     *         template
     */
    public boolean getIncludeFluff() {
        return includeFluff;
    }

    /**
     * Tuple composed of EquipmentType and size, used for map keys
     */
    static final class EquipmentKey {
        private final EquipmentType etype;
        private final double size;
        private final boolean armored;
        private final boolean internalBomb;

        EquipmentKey(EquipmentType etype, double size) {
            this(etype, size, false, false);
        }

        EquipmentKey(EquipmentType etype, double size, boolean armored, boolean internal) {
            this.etype = etype;
            this.size = size;
            this.armored = armored;
            this.internalBomb = internal;
        }

        String name() {
            return etype.getName(size);
        }

        EquipmentType getType() {
            return etype;
        }

        double getSize() {
            return size;
        }

        boolean isArmored() {
            return armored;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof EquipmentKey)
                    && (etype.equals(((EquipmentKey) o).etype))
                    && (size == ((EquipmentKey) o).size)
                    && (armored == ((EquipmentKey) o).armored);
        }

        @Override
        public int hashCode() {
            return Objects.hash(etype, size, armored);
        }
    }
}
