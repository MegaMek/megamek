/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.unitreadout;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.*;
import megamek.common.BombType.BombTypeEnum;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.MiscMounted;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.verifier.TestEntity;

import static megamek.client.ui.unitreadout.TableElement.*;

/**
 * The Entity information shown in the unit selector and many other places in MM, MML and MHQ.
 *
 * <p>
 * Goals for the Entity Readout:
 * <UL>
 * <LI> It is not bound to official source formatting such as TROs
 * <LI> Should be adaptable to various output formats, currently HTML, plain text and discord
 * <LI> Should show information sufficient to recreate the unit in MML (while undamaged)
 * <LI> Should show information sufficient to fill in a blank record sheet (while undamaged)
 * <LI> Should highlight damaged and destroyed items and critical hits as well as current values (movement)
 * <LI> Should show current ammo values
 * <LI> Need not show construction details without gameplay effects such as the maximum armor the unit type could carry
 * or the slot number on a mek
 * <LI> Need not show original values for damaged items unless those are relevant for gameplay
 * <LI> Should be organized into blocks that can be retrieved individually if necessary
 * </UL>
 *
 * <p>
 * The information is encoded in a series of classes that implement a common {@link ViewElement} interface, which can
 * format the element in any of the available output formats.
 */
class GeneralEntityReadout implements EntityReadout {

    protected static final String MESSAGE_NONE = Messages.getString("MekView.None");

    private final Entity entity;
    protected final boolean showDetail;
    protected final boolean useAlternateCost;
    protected final boolean ignorePilotBV;
    protected final ViewFormatting formatting;

    private final DecimalFormat dFormatter;
    private final List<ViewElement> sHead = new ArrayList<>();
    private final List<ViewElement> sBasic = new ArrayList<>();
    private final List<ViewElement> sLoadout = new ArrayList<>();
    private final List<ViewElement> sFluff = new ArrayList<>();
    private final List<ViewElement> sQuirks = new ArrayList<>();
    private final List<ViewElement> sInvalid = new ArrayList<>();

    /**
     * Compiles information about an {@link Entity} useful for showing a summary of its abilities.
     *
     * @param entity           The entity to summarize
     * @param showDetail       If true, shows individual weapons that make up weapon bays.
     * @param useAlternateCost If true, uses alternate cost calculation. This primarily provides an equipment-only cost
     *                         for conventional infantry for MekHQ.
     * @param ignorePilotBV    If true then the BV calculation is done without including the pilot BV modifiers
     * @param formatting       Which formatting style to use: HTML, Discord, or None (plaintext)
     */
    protected GeneralEntityReadout(Entity entity, boolean showDetail, boolean useAlternateCost,
                                   boolean ignorePilotBV, ViewFormatting formatting) {

        this.entity = entity;
        this.formatting = formatting;
        this.showDetail = showDetail;
        this.useAlternateCost = useAlternateCost;
        this.ignorePilotBV = ignorePilotBV;

        DecimalFormatSymbols unusualSymbols = new DecimalFormatSymbols();
        unusualSymbols.setDecimalSeparator('.');
        unusualSymbols.setGroupingSeparator(',');
        dFormatter = new DecimalFormat("#,###", unusualSymbols);
    }

    protected List<ViewElement> createHeaderBlock() {
        List<ViewElement> result = new ArrayList<>();
        result.add(new UnitName(entity.getShortNameRaw()));
        result.add(new PlainLine(EntityReadoutUnitType.unitTypeAsString(entity)));

        result.add(new PlainLine());
        result.add(createTechLevelElement());
        result.add(createDesignInvalidElement());
        result.addAll(createTechTable(entity, formatting));

        result.add(new PlainLine());
        result.add(createWeightElement());
        result.add(createBVElement());
        result.add(createCostElement());
        result.add(createSourceElement());
        result.add(createRoleElement());
        return result;
    }

    protected List<ViewElement> createLoadoutBlock() {
        List<ViewElement> result = new ArrayList<>();

        List<ViewElement> weapons = getWeapons(showDetail);
        if (!weapons.isEmpty()) {
            result.add(new PlainLine());
            result.addAll(weapons);
        }

        if (showAmmoBlock(showDetail)) {
            result.add(new PlainLine());
            result.add(getAmmo());
        }

        if (entity instanceof IBomber) {
            List<ViewElement> bombs = getBombs();
            if (!bombs.isEmpty()) {
                result.add(new PlainLine());
                result.addAll(bombs);
            }
        }

        result.addAll(getMisc()); // legacy comment: has to occur before basic is processed

        ViewElement failedEquipment = getFailed();
        if (!(failedEquipment instanceof EmptyElement)) {
            result.add(new PlainLine());
            result.add(failedEquipment);
        }

        return result;
    }

    protected List<ViewElement> createQuirksBlock() {
        List<ViewElement> result = new ArrayList<>();
        Game game = entity.getGame();

        if ((game == null) || game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            List<String> activeUnitQuirksNames = entity.getQuirks().activeQuirks().stream()
                  .map(IOption::getDisplayableNameWithValue)
                  .toList();

            if (!activeUnitQuirksNames.isEmpty()) {
                result.add(new PlainLine());
                var list = new ItemList(Messages.getString("MekView.Quirks"));
                activeUnitQuirksNames.forEach(list::addItem);
                result.add(list);
            }

            List<String> wpQuirksList = new ArrayList<>();
            for (Mounted<?> weapon : entity.getWeaponList()) {
                List<String> activeWeaponQuirksNames = weapon.getQuirks().activeQuirks().stream()
                      .map(IOption::getDisplayableNameWithValue)
                      .collect(Collectors.toList());
                if (!activeWeaponQuirksNames.isEmpty()) {
                    String wq = weapon.getDesc() + " (" + entity.getLocationAbbr(weapon.getLocation()) + "): ";
                    wq += String.join(", ", activeWeaponQuirksNames);
                    wpQuirksList.add(wq);
                }
            }
            if (!wpQuirksList.isEmpty()) {
                result.add(new PlainLine());
                var list = new ItemList(Messages.getString("MekView.WeaponQuirks"));
                wpQuirksList.forEach(list::addItem);
                result.add(list);
            }
        }
        return result;
    }

    protected List<ViewElement> createFluffBlock() {
        List<ViewElement> result = new ArrayList<>();
        if (!entity.getFluff().getOverview().isEmpty()) {
            result.add(new PlainLine());
            result.add(new FluffTextElement("Overview", entity.getFluff().getOverview()));
        }
        if (!entity.getFluff().getCapabilities().isEmpty()) {
            result.add(new PlainLine());
            result.add(new FluffTextElement("Capabilities", entity.getFluff().getCapabilities()));
        }
        if (!entity.getFluff().getDeployment().isEmpty()) {
            result.add(new PlainLine());
            result.add(new FluffTextElement("Deployment", entity.getFluff().getDeployment()));
        }
        if (!entity.getFluff().getHistory().isEmpty()) {
            result.add(new PlainLine());
            result.add(new FluffTextElement("History", entity.getFluff().getHistory()));
        }
        return result;
    }

    protected List<ViewElement> createInvalidBlock() {
        List<ViewElement> result = new ArrayList<>();
        StringBuffer sb = new StringBuffer();
        TestEntity testEntity = TestEntity.getEntityVerifier(entity);

        if (testEntity != null) {
            testEntity.correctEntity(sb);
            if (!sb.isEmpty()) {
                result.add(new PlainLine());
                String[] errorLines = sb.toString().split("\n");
                String label = entity.hasQuirk(OptionsConstants.QUIRK_NEG_ILLEGAL_DESIGN)
                      ? Messages.getString("MekView.InvalidButIllegalQuirk")
                      : Messages.getString("MekView.InvalidReasons");
                var errorList = new ItemList(label);
                Arrays.stream(errorLines).forEach(errorList::addItem);
                result.add(errorList);
            }
        }
        return result;
    }

    protected ViewElement createDesignInvalidElement() {
        // Note that this has apparently no effect outside of a game
        return entity.isDesignValid()
              ? new EmptyElement()
              : new PlainLine(Messages.getString("MekView.DesignInvalid"));
    }

    protected ViewElement createTechLevelElement() {
        String techLevel = entity.getStaticTechLevel().toString();
        if (entity.isMixedTech()) {
            techLevel += Messages.getString(entity.isClan() ? "MekView.MixedClan" : "MekView.MixedIS");
        } else {
            techLevel += Messages.getString(entity.isClan() ? "MekView.Clan" : "MekView.IS");
        }
        return new LabeledElement(Messages.getString("MekView.BaseTechLevel"), techLevel);
    }

    protected ViewElement createRoleElement() {
        return entity.hasRole() ? new LabeledElement("Role", entity.getRole().toString()) : new EmptyElement();
    }

    protected ViewElement createCostElement() {
        double cost = (useAlternateCost && entity.getAlternateCost() > 0)
              ? entity.getAlternateCost()
              : entity.getCost(false);
        return new LabeledElement(Messages.getString("MekView.Cost"), dFormatter.format(cost) + " C-bills");
    }

    protected ViewElement createBVElement() {
        return new LabeledElement(
              Messages.getString("MekView.BV"),
              dFormatter.format(entity.calculateBattleValue(false, ignorePilotBV)));
    }

    protected ViewElement createSourceElement() {
        String source = entity.getSource();
        String sourceLabel = Messages.getString("MekView.Source");

        if (source.isBlank()) {
            return new LabeledElement(sourceLabel, Messages.getString("MekView.Unknown"));
        } else if (source.contains(MMConstants.SOURCE_TEXT_SHRAPNEL)) {
            return new HyperLinkElement(sourceLabel, MMConstants.BT_URL_SHRAPNEL, source);
        } else {
            return new LabeledElement(sourceLabel, source);
        }
    }

    protected ViewElement createWeightElement() {
        return new LabeledElement(Messages.getString("MekView.Weight"),
              Math.round(entity.getWeight()) + Messages.getString("MekView.tons"));
    }

    protected List<ViewElement> createMovementElements() {
        List<ViewElement> result = new ArrayList<>();
        // Temporarily change the conversion mode to get a consistent result
        int originalMode = entity.getConversionMode();
        entity.setConversionMode(0);

        result.add(new PlainLine());
        result.add(new LabeledElement(Messages.getString("MekView.Movement"), createMovementString()));
        result.addAll(createMiscMovementElements());
        result.addAll(createConversionModeMovementElements());

        entity.setConversionMode(originalMode);
        return result;
    }

    protected String createMovementString() {
        StringBuilder moveString = new StringBuilder();
        moveString.append(entity.getWalkMP())
              .append("/")
              .append(entity.getRunMPasString());

        if (entity.getJumpMP() > 0) {
            moveString.append("/").append(entity.getJumpMP());
            if (entity.damagedJumpJets() > 0) {
                moveString.append(ViewElement.warningStart(formatting))
                      .append("(")
                      .append(entity.damagedJumpJets())
                      .append(" damaged jump jets)")
                      .append(ViewElement.warningEnd(formatting));
            }
        }
        if (entity instanceof Mek mek) {
            int mekMechanicalJumpMP = mek.getMechanicalJumpBoosterMP();
            if (mekMechanicalJumpMP > 0) {
                if (entity.getJumpMP() == 0) {
                    moveString.append("/").append(mekMechanicalJumpMP);
                } else {
                    moveString.append(" (%d)".formatted(mekMechanicalJumpMP));
                }
            }
        }
        if (entity.getAllUMUCount() > 0) {
            // Add in Jump MP if it wasn't already printed
            if (entity.getJumpMP() == 0) {
                moveString.append("/0");
            }
            moveString.append("/")
                  .append(entity.getActiveUMUCount());
            if ((entity.getAllUMUCount() - entity.getActiveUMUCount()) != 0) {
                moveString.append(ViewElement.warningStart(formatting)).append("(")
                      .append(entity.getAllUMUCount() - entity.getActiveUMUCount())
                      .append(" damaged UMUs)")
                      .append(ViewElement.warningEnd(formatting));
            }
        }
        return moveString.toString();
    }

    protected List<ViewElement> createConversionModeMovementElements() {
        return Collections.emptyList();
    }

    protected List<ViewElement> createMiscMovementElements() {
        return Collections.emptyList();
    }

    protected List<ViewElement> createBasicBlock() {
        List<ViewElement> result = new ArrayList<>(createMovementElements());
        result.add(createEngineElement());
        result.addAll(createSystemsElements());
        result.addAll(createFuelElements());
        List<ViewElement> armorElements = createArmorElements();
        if (!armorElements.isEmpty()) {
            result.add(new PlainLine());
            result.addAll(armorElements);
        }
        return result;
    }

    protected ViewElement createEngineElement() {
        String engine = entity.hasEngine() ? entity.getEngine().getShortEngineName() : "(none)";
        if (entity.getEngineHits() > 0) {
            engine += " " + ViewElement.warningStart(formatting) + "(" + entity.getEngineHits()
                  + " hits)" + ViewElement.warningEnd(formatting);
        }
        if (entity.hasArmoredEngine()) {
            engine += " (armored)";
        }
        return new LabeledElement(Messages.getString("MekView.Engine"), engine);
    }

    protected List<ViewElement> createSystemsElements() {
        return Collections.emptyList();
    }

    protected List<ViewElement> createFuelElements() {
        return Collections.emptyList();
    }

    protected List<ViewElement> createTechTable(Entity entity, ViewFormatting formatting) {
        List<ViewElement> result = new ArrayList<>();
        result.add(new LabeledElement(ViewElement.textWithTooltip(
              Messages.getString("MekView.TechRating"), Messages.getString("MekView.TechRating.tooltip"), formatting),
              entity.getFullRatingName()));

        result.add(new LabeledElement(ViewElement.textWithTooltip(
              Messages.getString("MekView.EarliestTechDate"),
              Messages.getString("MekView.EarliestTechDate.tooltip"),
              formatting),
              entity.getEarliestTechDateAndEra()));

        TableElement tpTable = new TableElement(3);

        String tableSpacer = ViewFormatting.HTML.equals(formatting) ? "&nbsp;&nbsp;&nbsp;&nbsp;" : "    ";
        tpTable.setColNames(Messages.getString("MekView.Availability"), tableSpacer,
                Messages.getString("MekView.Era"));
        tpTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_LEFT);

        tpTable.addRow(ViewElement.textWithTooltip(Messages.getString("MekView.Prototype"),
              Messages.getString("MekView.Prototype.tooltip"), formatting), tableSpacer,
              ViewElement.splitDateRange(entity.getPrototypeRangeDate(), formatting));
        tpTable.addRow(ViewElement.textWithTooltip(Messages.getString("MekView.Production"),
              Messages.getString("MekView.Production.tooltip"), formatting), tableSpacer,
              ViewElement.splitDateRange(entity.getProductionDateRange(), formatting));
        tpTable.addRow(ViewElement.textWithTooltip(Messages.getString("MekView.Common"),
              Messages.getString("MekView.Common.tooltip"), formatting), tableSpacer,
              ViewElement.splitDateRange(entity.getCommonDateRange(), formatting));
        String extinctRange = ViewElement.splitDateRange(entity.getExtinctionRange(), formatting);
        if (extinctRange.length() > 1) {
            tpTable.addRow(
                  ViewElement.textWithTooltip(
                        Messages.getString("MekView.Extinct"),
                        Messages.getString("MekView.Extinct.tooltip"), formatting),
                  tableSpacer,
                  extinctRange);
        }
        result.add(new PlainLine());
        result.add(tpTable);

        return result;
    }

    /** @return True when the unit requires an ammo block. */
    private boolean showAmmoBlock(boolean showDetail) {
        return (!entity.usesWeaponBays() || !showDetail) && !entity.getAmmo().stream().allMatch(this::hideAmmo);
    }

    /**
     * @return A summary including all four sections.
     */
    public String getReadout(@Nullable String fontName, ViewFormatting formatting) {
        sHead.addAll(createHeaderBlock());
        sBasic.addAll(createBasicBlock());
        sLoadout.addAll(createLoadoutBlock());
        sQuirks.addAll(createQuirksBlock());
        // legacy -- I dont know why these were not kept separate
        sFluff.addAll(sQuirks);
        sFluff.addAll(createFluffBlock());
        sInvalid.addAll(createInvalidBlock());

        String docStart = "";
        String docEnd = "";

        if (formatting == ViewFormatting.HTML && (fontName != null)) {
            docStart = "<div style=\"font-family:" + fontName + ";\">";
            docEnd = "</div>";
        } else if (formatting == ViewFormatting.DISCORD) {
            docStart = "```ansi\n";
            docEnd = "```";
        }
        return docStart + getHeadSection()
                + getBasicSection() + getLoadoutSection()
                + getFluffSection() + getInvalidSection() + docEnd;
    }

    protected ViewElement createTotalInternalElement() {
        String internal = String.valueOf(entity.getTotalInternal());
        return new LabeledElement(Messages.getString("MekView.Internal"), internal);
    }

    protected ViewElement createTotalArmorElement() {
        String armor = String.valueOf(entity.getTotalArmor());
        if (!entity.hasPatchworkArmor()) {
            armor += " (" + ArmorType.forEntity(entity).getName() + ")";
        }
        return new LabeledElement(Messages.getString("MekView.Armor"), armor);
    }

    protected boolean skipArmorLocation(int location) {
        // Skip non-existent sections
        return entity.getInternal(location) == IArmorState.ARMOR_NA;
    }

    protected ViewElement createArmorLocationTable() {
        TableElement locTable = new TableElement(5);
        locTable.setColNames("", "Internal", "Armor", "", ""); // last two columns are patchwork armor and location
        locTable.setJustification(JUSTIFIED_LEFT, JUSTIFIED_CENTER, JUSTIFIED_CENTER, JUSTIFIED_LEFT, JUSTIFIED_LEFT);

        for (int loc = 0; loc < entity.locations(); loc++) {
            if (skipArmorLocation(loc)) {
                continue;
            }

            String[] row = { entity.getLocationName(loc),
                             ReadoutUtils.renderArmor(entity.getInternalForReal(loc), entity.getOInternal(loc)),
                             "", "", "" };

            if (IArmorState.ARMOR_NA != entity.getArmorForReal(loc)) {
                row[2] = ReadoutUtils.renderArmor(entity.getArmorForReal(loc), entity.getOArmor(loc));
            }
            if (entity.hasPatchworkArmor()) {
                row[3] = ArmorType.forEntity(entity, loc).getName();
            }
            if (!entity.getLocationDamage(loc).isEmpty()) {
                row[4] = ViewElement.warningStart(formatting)
                      + entity.getLocationDamage(loc)
                      + ViewElement.warningEnd(formatting);
            }
            locTable.addRow(row);
            if (entity.hasRearArmor(loc)) {
                String rearArmor = ReadoutUtils.renderArmor(
                      entity.getArmorForReal(loc, true),
                      entity.getOArmor(loc, true)
                );
                row = new String[] { entity.getLocationName(loc) + " (rear)", "", rearArmor, "", "" };
                locTable.addRow(row);
            }
        }
        return locTable;
    }

    protected List<ViewElement> createArmorElements() {
        List<ViewElement> result = new ArrayList<>();
        result.add(createTotalInternalElement());
        result.add(createTotalArmorElement());
        result.add(new PlainLine());
        result.add(createArmorLocationTable());
        return result;
    }

    protected List<ViewElement> getWeapons(boolean showDetail) {
        return ReadoutUtils.getWeapons(entity, showDetail, formatting);
    }

    static String quirkMarker(Mounted<?> mounted) {
        return (mounted.countQuirks() > 0) ? " (Q)" : "";
    }

    boolean hideAmmo(Mounted<?> mounted) {
        return ((mounted.getLinkedBy() != null) && mounted.getLinkedBy().isOneShot())
                || (mounted.getSize() == 0) || (mounted.getLocation() == Entity.LOC_NONE);
    }

    protected TableElement getAmmo() {
        TableElement ammoTable = new TableElement(4);
        ammoTable.setColNames("Ammo", "Loc", "Shots", entity.isOmni() ? "Omni" : "");
        ammoTable.setJustification(JUSTIFIED_LEFT, JUSTIFIED_CENTER, JUSTIFIED_CENTER, JUSTIFIED_CENTER);

        for (AmmoMounted mounted : entity.getAmmo()) {
            if (hideAmmo(mounted)) {
                continue;
            }

            String[] row = { mounted.getName(), entity.getLocationAbbr(mounted.getLocation()),
                    String.valueOf(mounted.getBaseShotsLeft()), "" };
            if (entity.isOmni()) {
                row[3] = Messages.getString(mounted.isOmniPodMounted() ? "MekView.Pod" : "MekView.Fixed");
            }

            if (mounted.isDestroyed() || (mounted.getUsableShotsLeft() < 1)) {
                row[2] = ReadoutMarkup.markupDestroyed(row[2]);
            } else if (mounted.getUsableShotsLeft() < mounted.getType().getShots()) {
                row[2] = ReadoutMarkup.markupDamaged(row[2]);
            }
            ammoTable.addRow(row);
        }

        return ammoTable;
    }

    private List<ViewElement> getBombs() {
        List<ViewElement> result = new ArrayList<>();
        IBomber bomber = (IBomber) entity;
        result.addAll(getBombLoadoutBombs(bomber.getIntBombChoices(), " [Int. Bay]"));
        result.addAll(getBombLoadoutBombs(bomber.getExtBombChoices(), ""));
        return result;
    }

    protected List<ViewElement> getBombLoadoutBombs(BombLoadout loadout, String marker) {
        List<ViewElement> result = new ArrayList<>();
        for (Map.Entry<BombTypeEnum, Integer> entry : loadout.entrySet()) {
            BombTypeEnum bombType = entry.getKey();
            int count = entry.getValue();
            if (count > 0) {
                result.add(new PlainLine(bombType.getDisplayName() + " (" + count + ")" + marker));
            }
        }
        return result;
    }

    protected TableElement createMiscTable() {
        TableElement miscTable = new TableElement(3);
        miscTable.setColNames("Equipment", entity.isConventionalInfantry() ? "" : "Loc", entity.isOmni() ? "Omni" : "");
        miscTable.setJustification(JUSTIFIED_LEFT, JUSTIFIED_CENTER, JUSTIFIED_CENTER);
        for (MiscMounted mounted : entity.getMisc()) {
            if (ReadoutUtils.hideMisc(mounted, entity)) {
                continue;
            }

            String[] row = { mounted.getDesc(), entity.joinLocationAbbr(mounted.allLocations(), 3), "" };
            if (entity.isConventionalInfantry()) {
                // don't display the location on CI
                row[1] = "";
            }

            if (entity.isClan() && (mounted.getType().getTechBase() == ITechnology.TechBase.IS)) {
                row[0] += Messages.getString("MekView.IS");
            }

            if (!entity.isClan() && (mounted.getType().getTechBase() == ITechnology.TechBase.CLAN)) {
                row[0] += Messages.getString("MekView.Clan");
            }

            if (entity.isOmni()) {
                row[2] = Messages.getString(mounted.isOmniPodMounted() ? "MekView.Pod" : "MekView.Fixed");
            }

            if (mounted.isDestroyed()) {
                row[0] = ReadoutMarkup.markupDestroyed(row[0]);
            }
            miscTable.addRow(row);
        }
        return miscTable;
    }

    protected List<ViewElement> getMisc() {
        List<ViewElement> result = new ArrayList<>();

        TableElement miscTable = createMiscTable();
        if (!miscTable.isEmpty()) {
            result.add(new PlainLine());
            result.add(miscTable);
        }

        List<ViewElement> specialElements = createSpecialMiscElements();
        if (!specialElements.isEmpty()) {
            result.addAll(specialElements);
        }

        String transportersString = entity.getUnusedString(formatting);
        if (!transportersString.isBlank()) {
            var transportsList = new ItemList(Messages.getString("MekView.CarryingCapacity"));
            String separator = formatting == ViewFormatting.HTML ? "<br>" : "\n";
            Arrays.stream(transportersString.split(separator)).forEach(transportsList::addItem);

            result.add(new PlainLine());
            result.add(transportsList);
        }

        return result;
    }

    protected List<ViewElement> createSpecialMiscElements() {
        return Collections.emptyList();
    }

    private ViewElement getFailed() {
        Iterator<String> eFailed = entity.getFailedEquipment();
        if (eFailed.hasNext()) {
            var list = new ItemList("The following equipment slots failed to load:");
            while (eFailed.hasNext()) {
                list.addItem(eFailed.next());
            }
            return list;
        }
        return new EmptyElement();
    }

    @Override
    public String getHeadSection() {
        return formatSection(sHead);
    }

    @Override
    public String getBasicSection() {
        return formatSection(sBasic);
    }

    @Override
    public String getInvalidSection() {
        return formatSection(sInvalid);
    }

    @Override
    public String getLoadoutSection() {
        return formatSection(sLoadout);
    }

    @Override
    public String getFluffSection() {
        if (formatting == ViewFormatting.DISCORD) {
            // The rest of the fluff often doesn't fit in a Discord message
            return formatSection(sQuirks);
        }
        return formatSection(sFluff);
    }

    /**
     * Converts a list of {@link ViewElement}s to a String using the selected format.
     *
     * @param section The elements to format.
     *
     * @return The formatted data.
     */
    private String formatSection(List<ViewElement> section) {
        Function<ViewElement, String> mapper = switch (formatting) {
            case HTML -> ViewElement::toHTML;
            case NONE -> ViewElement::toPlainText;
            case DISCORD -> ViewElement::toDiscord;
        };
        return section.stream().map(mapper).collect(Collectors.joining());
    }
}
