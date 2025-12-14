/*
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.entityreadout;

import static megamek.client.ui.entityreadout.TableElement.JUSTIFIED_CENTER;
import static megamek.client.ui.entityreadout.TableElement.JUSTIFIED_LEFT;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.SourceBooks;
import megamek.common.annotations.Nullable;
import megamek.common.enums.TechBase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.game.Game;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.IBomber;
import megamek.common.units.Mek;
import megamek.common.verifier.TestEntity;

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

    private boolean initialized = false;

    private final DecimalFormat dFormatter;
    private final List<ViewElement> headerSection = new ArrayList<>();
    private final List<ViewElement> techLevel = new ArrayList<>();
    private final List<ViewElement> techSection = new ArrayList<>();
    private final List<ViewElement> sourceSection = new ArrayList<>();
    private final List<ViewElement> baseSection = new ArrayList<>();
    private final List<ViewElement> systemsSection = new ArrayList<>();
    private final List<ViewElement> loadoutSection = new ArrayList<>();
    private final List<ViewElement> quirksSection = new ArrayList<>();
    private final List<ViewElement> fluffSection = new ArrayList<>();
    private final List<ViewElement> invalidSection = new ArrayList<>();

    /**
     * Compiles information about an {@link Entity} useful for showing a summary of its abilities.
     *
     * @param entity           The entity to summarize
     * @param showDetail       If true, shows individual weapons that make up weapon bays.
     * @param useAlternateCost If true, uses alternate cost calculation. This primarily provides an equipment-only cost
     *                         for conventional infantry for MekHQ.
     * @param ignorePilotBV    If true then the BV calculation is done without including the pilot BV modifiers
     */
    protected GeneralEntityReadout(Entity entity, boolean showDetail, boolean useAlternateCost,
          boolean ignorePilotBV) {

        this.entity = entity;
        this.showDetail = showDetail;
        this.useAlternateCost = useAlternateCost;
        this.ignorePilotBV = ignorePilotBV;

        DecimalFormatSymbols unusualSymbols = new DecimalFormatSymbols();
        unusualSymbols.setDecimalSeparator('.');
        unusualSymbols.setGroupingSeparator(',');
        dFormatter = new DecimalFormat("#,###", unusualSymbols);
    }

    protected List<ViewElement> createHeaderSection() {
        List<ViewElement> result = new ArrayList<>();
        result.add(new UnitName(entity.getShortNameRaw()));
        result.add(new PlainLine(EntityReadoutUnitType.unitTypeAsString(entity)));
        return result;
    }

    private List<ViewElement> createTechLevel() {
        List<ViewElement> result = new ArrayList<>();
        result.add(new PlainLine());
        result.add(createTechLevelElement());
        return result;
    }

    private List<ViewElement> createTechSection() {
        List<ViewElement> result = new ArrayList<>();
        result.add(createDesignInvalidElement());
        result.addAll(createTechTable(entity));
        return result;
    }

    protected List<ViewElement> createBaseSection() {
        List<ViewElement> result = new ArrayList<>();
        result.add(new PlainLine());
        result.add(createWeightElement());
        result.add(createBVElement());
        result.add(createRoleElement());
        return result;
    }

    protected List<ViewElement> createSourceSection() {
        List<ViewElement> result = new ArrayList<>();
        result.add(new PlainLine());
        result.add(createCostElement());
        result.add(createSourceElement());
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
            result.add(new FluffTextLine("Overview", entity.getFluff().getOverview()));
        }
        if (!entity.getFluff().getCapabilities().isEmpty()) {
            result.add(new PlainLine());
            result.add(new FluffTextLine("Capabilities", entity.getFluff().getCapabilities()));
        }
        if (!entity.getFluff().getDeployment().isEmpty()) {
            result.add(new PlainLine());
            result.add(new FluffTextLine("Deployment", entity.getFluff().getDeployment()));
        }
        if (!entity.getFluff().getHistory().isEmpty()) {
            result.add(new PlainLine());
            result.add(new FluffTextLine("History", entity.getFluff().getHistory()));
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
        // Note that this has apparently no effect outside a game
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
        return new LabeledLine(Messages.getString("MekView.BaseTechLevel"), techLevel);
    }

    protected ViewElement createRoleElement() {
        return entity.hasRole() ? new LabeledLine("Role", entity.getRole().toString()) : new EmptyElement();
    }

    protected ViewElement createCostElement() {
        double cost = (useAlternateCost && entity.getAlternateCost() > 0)
              ? entity.getAlternateCost()
              : entity.getCost(false);
        return new LabeledLine(Messages.getString("MekView.Cost"), dFormatter.format(cost) + " C-bills");
    }

    protected ViewElement createBVElement() {
        return new LabeledLine(
              Messages.getString("MekView.BV"),
              dFormatter.format(entity.calculateBattleValue(false, ignorePilotBV)));
    }

    protected ViewElement createSourceElement() {
        String source = entity.getSource();
        String sourceLabel = Messages.getString("MekView.Source");
        var sourcebooks = new SourceBooks();
        var book = sourcebooks.loadSourceBook(source);
        if (book.isPresent()) {
            if (book.get().getMul_url() != null) {
                return new HyperLinkLine(sourceLabel, book.get().getMul_url(), book.get().getTitle());
            } else if (book.get().getTitle() != null) {
                source = Objects.requireNonNullElse(book.get().getTitle(), "");
            }
        }

        if (source.isBlank()) {
            return new LabeledLine(sourceLabel, Messages.getString("MekView.Unknown"));
        } else if (source.contains(MMConstants.SOURCE_TEXT_SHRAPNEL)) {
            return new HyperLinkLine(sourceLabel, MMConstants.BT_URL_SHRAPNEL, source);
        } else {
            return new LabeledLine(sourceLabel, source);
        }
    }

    protected ViewElement createWeightElement() {
        return new LabeledLine(Messages.getString("MekView.Weight"),
              Math.round(entity.getWeight()) + Messages.getString("MekView.tons"));
    }

    protected List<ViewElement> createMovementElements() {
        List<ViewElement> result = new ArrayList<>();
        // Temporarily change the conversion mode to get a consistent result
        int originalMode = entity.getConversionMode();
        entity.setConversionMode(0);

        result.add(new PlainLine());
        result.add(new LabeledLine(Messages.getString("MekView.Movement"), createMovementString()));
        result.addAll(createMiscMovementElements());
        result.addAll(createConversionModeMovementElements());

        entity.setConversionMode(originalMode);
        return result;
    }

    protected ViewElement createMovementString() {
        JoinedViewElement result = new JoinedViewElement();
        result.add(entity.getWalkMP() + "/" + entity.getRunMPasString());
        if (entity.getJumpMP() > 0) {
            result.add("/" + entity.getJumpMP());
            if (entity.damagedJumpJets() > 0) {
                result.add(new DamagedElement(" (%d damaged jump jets)".formatted(entity.damagedJumpJets())));
            }
        }
        if (entity instanceof Mek mek) {
            int mekMechanicalJumpMP = mek.getMechanicalJumpBoosterMP();
            if (mekMechanicalJumpMP > 0) {
                if (entity.getJumpMP() == 0) {
                    result.add("/" + mekMechanicalJumpMP);
                } else {
                    result.add(" (%d)".formatted(mekMechanicalJumpMP));
                }
            }
        }

        if (entity.getAllUMUCount() > 0) {
            // Add in Jump MP if it wasn't already printed
            if (entity.getJumpMP() == 0) {
                result.add("/0");
            }
            result.add("/" + entity.getActiveUMUCount());
            if ((entity.getAllUMUCount() - entity.getActiveUMUCount()) != 0) {
                result.add(new DamagedElement(
                      " (%d damaged UMUs)".formatted(entity.getAllUMUCount() - entity.getActiveUMUCount())));
            }
        }
        return result;
    }

    protected List<ViewElement> createConversionModeMovementElements() {
        return Collections.emptyList();
    }

    protected List<ViewElement> createMiscMovementElements() {
        return Collections.emptyList();
    }

    protected List<ViewElement> createSystemsSection() {
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
        String engineText = entity.hasEngine() ? entity.getEngine().getShortEngineName() : "(none)";
        if (entity.hasArmoredEngine()) {
            engineText += " (armored)";
        }
        ViewElement engine = new PlainElement(engineText);
        if (entity.getEngineHits() > 0) {
            engineText += " (%d hits)".formatted(entity.getEngineHits());
            engine = new DamagedElement(engineText);
        }
        return new LabeledLine(Messages.getString("MekView.Engine"), engine);
    }

    protected List<ViewElement> createSystemsElements() {
        return Collections.emptyList();
    }

    protected List<ViewElement> createFuelElements() {
        return Collections.emptyList();
    }

    protected List<ViewElement> createTechTable(Entity entity) {
        List<ViewElement> result = new ArrayList<>();
        result.add(new ToolTippedLabelledLine(
              Messages.getString("MekView.TechRating"),
              Messages.getString("MekView.TechRating.tooltip"),
              entity.getFullRatingName()));

        result.add(new ToolTippedLabelledLine(
              Messages.getString("MekView.EarliestTechDate"),
              Messages.getString("MekView.EarliestTechDate.tooltip"),
              entity.getEarliestTechDateAndEra()));

        TableElement tpTable = new TableElement(2);

        tpTable.setColNames(
              Messages.getString("MekView.Availability"),
              Messages.getString("MekView.Era"));
        tpTable.setJustification(JUSTIFIED_LEFT, JUSTIFIED_LEFT);

        tpTable.addRow(new ToolTippedElement(
                    Messages.getString("MekView.Prototype"),
                    Messages.getString("MekView.Prototype.tooltip")),
              new DateRangeElement(entity.getPrototypeRangeDate()));

        tpTable.addRow(new ToolTippedElement(
                    Messages.getString("MekView.Production"),
                    Messages.getString("MekView.Production.tooltip")),
              new DateRangeElement(entity.getProductionDateRange()));

        tpTable.addRow(new ToolTippedElement(
                    Messages.getString("MekView.Common"),
                    Messages.getString("MekView.Common.tooltip")),
              new DateRangeElement(entity.getCommonDateRange()));

        if (entity.getExtinctionRange().length() > 1) {
            tpTable.addRow(new ToolTippedElement(
                        Messages.getString("MekView.Extinct"),
                        Messages.getString("MekView.Extinct.tooltip")),
                  new DateRangeElement(entity.getExtinctionRange(), true));
        }
        result.add(new PlainLine());
        result.add(tpTable);

        return result;
    }

    /** @return True when the unit requires an ammo block. */
    private boolean showAmmoBlock(boolean showDetail) {
        return (!entity.usesWeaponBays() || !showDetail) && !entity.getAmmo().stream().allMatch(this::hideAmmo);
    }

    @Override
    public String getReadout(String fontName, ViewFormatting formatting, Collection<ReadoutSections> sectionsToShow) {
        initialize();

        String docStart = "";
        String docEnd = "";

        if (formatting == ViewFormatting.HTML && (fontName != null)) {
            docStart = "<div style=\"font-family:" + fontName + ";\">";
            docEnd = "</div>";
        } else if (formatting == ViewFormatting.DISCORD) {
            docStart = "```ansi\n";
            docEnd = "```";
        }

        String formattedSections = Arrays.stream(ReadoutSections.values())
              .filter(sectionsToShow::contains)
              .map(this::sectionFor)
              .map(section -> formatSection(section, formatting))
              .collect(Collectors.joining());

        return docStart + formattedSections + docEnd;
    }

    public String getReadout(String fontName, ViewFormatting formatting, ReadoutSections... sectionsToShow) {
        return getReadout(fontName, formatting, Arrays.asList(sectionsToShow));
    }

    private List<ViewElement> sectionFor(ReadoutSections section) {
        return switch (section) {
            case HEADLINE -> headerSection;
            case TECH_LEVEL -> techLevel;
            case AVAILABILITY -> techSection;
            case COST_SOURCE -> sourceSection;
            case BASE_DATA -> baseSection;
            case SYSTEMS -> systemsSection;
            case LOADOUT -> loadoutSection;
            case QUIRKS -> quirksSection;
            case FLUFF -> fluffSection;
            case INVALID -> invalidSection;
        };
    }

    @Override
    public String getFullReadout(@Nullable String fontName, ViewFormatting formatting) {
        return getReadout(fontName, formatting, ReadoutSections.values());
    }

    protected ViewElement createTotalInternalElement() {
        String internal = String.valueOf(entity.getTotalInternal());
        return new LabeledLine(Messages.getString("MekView.Internal"), internal);
    }

    protected ViewElement createTotalArmorElement() {
        String armor = String.valueOf(entity.getTotalArmor());
        if (!entity.hasPatchworkArmor()) {
            armor += " (" + ArmorType.forEntity(entity).getName() + ")";
        }
        return new LabeledLine(Messages.getString("MekView.Armor"), armor);
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

            ViewElement[] row = new ViewElement[5];
            row[0] = new PlainElement(entity.getLocationName(loc));
            row[1] = ReadoutUtils.renderArmorAsViewElement(entity.getInternalForReal(loc), entity.getOInternal(loc));

            if (IArmorState.ARMOR_NA != entity.getArmorForReal(loc)) {
                row[2] = ReadoutUtils.renderArmorAsViewElement(entity.getArmorForReal(loc), entity.getOArmor(loc));
            }
            if (entity.hasPatchworkArmor()) {
                row[3] = new PlainElement(ArmorType.forEntity(entity, loc).getName());
            }
            String locationDamage = entity.getLocationDamage(loc);
            if (!locationDamage.isBlank()) {
                row[4] = new DamagedElement(locationDamage);
            }
            locTable.addRow(row);

            if (entity.hasRearArmor(loc)) {
                ViewElement rearArmor = ReadoutUtils.renderArmorAsViewElement(
                      entity.getArmorForReal(loc, true),
                      entity.getOArmor(loc, true)
                );
                row = new ViewElement[5];
                row[0] = new PlainElement(entity.getLocationName(loc) + " (rear)");
                row[2] = rearArmor;
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
        return ReadoutUtils.getWeapons(entity, showDetail);
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

            ViewElement[] row = { new PlainElement(mounted.getName()),
                                  new PlainElement(entity.getLocationAbbr(mounted.getLocation())),
                                  new PlainElement(mounted.getBaseShotsLeft()),
                                  new EmptyElement() };
            if (entity.isOmni()) {
                row[3] = new PlainElement(Messages.getString(mounted.isOmniPodMounted() ?
                      "MekView.Pod" :
                      "MekView.Fixed"));
            }

            if (mounted.isDestroyed() || (mounted.getUsableShotsLeft() < 1)) {
                row[2] = new DestroyedElement(mounted.getBaseShotsLeft());
            } else if (mounted.getUsableShotsLeft() < mounted.getOriginalShots()) {
                row[2] = new DamagedElement(mounted.getBaseShotsLeft());
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

            String name = mounted.getDesc();
            if (entity.isClan() && (mounted.getType().getTechBase() == TechBase.IS)) {
                name += Messages.getString("MekView.IS");
            }

            if (!entity.isClan() && (mounted.getType().getTechBase() == TechBase.CLAN)) {
                name += Messages.getString("MekView.Clan");
            }
            ViewElement[] row = { new PlainElement(name),
                                  new PlainElement(entity.joinLocationAbbr(mounted.allLocations(), 3)),
                                  new EmptyElement() };
            if (entity.isConventionalInfantry()) {
                // don't display the location on CI
                row[1] = new EmptyElement();
            }

            if (entity.isOmni()) {
                row[2] = new PlainElement(Messages.getString(mounted.isOmniPodMounted() ?
                      "MekView.Pod" :
                      "MekView.Fixed"));
            }

            if (mounted.isDestroyed()) {
                row[0] = new DestroyedElement(name);
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

        ItemList transportsList = ReadoutUtils.createTransporterList(entity);
        if (!transportsList.isEmpty()) {
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
    public String getBasicSection(ViewFormatting formatting) {
        initialize();
        return formatSection(systemsSection, formatting);
    }

    @Override
    public String getLoadoutSection(ViewFormatting formatting) {
        initialize();
        return formatSection(loadoutSection, formatting);
    }

    /**
     * Converts a list of {@link ViewElement}s to a String using the selected format.
     *
     * @param section The elements to format.
     *
     * @return The formatted data.
     */
    private String formatSection(List<ViewElement> section, ViewFormatting formatting) {
        Function<ViewElement, String> mapper = switch (formatting) {
            case HTML -> ViewElement::toHTML;
            case NONE -> ViewElement::toPlainText;
            case DISCORD -> ViewElement::toDiscord;
        };
        return section.stream().map(mapper).collect(Collectors.joining());
    }

    /**
     * Constructs the contents of this readout, if they have not been constructed yet. Otherwise, does nothing.
     */
    private void initialize() {
        if (!initialized) {
            initialized = true;
            headerSection.addAll(createHeaderSection());
            techLevel.addAll(createTechLevel());
            techSection.addAll(createTechSection());
            sourceSection.addAll(createSourceSection());
            baseSection.addAll(createBaseSection());
            systemsSection.addAll(createSystemsSection());
            loadoutSection.addAll(createLoadoutBlock());
            quirksSection.addAll(createQuirksBlock());
            // legacy -- I dont know why these were not kept separate
            //            fluffSection.addAll(quirksSection);
            fluffSection.addAll(createFluffBlock());
            invalidSection.addAll(createInvalidBlock());
        }
    }
}
