/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.mekview;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import megamek.MMConstants;
import megamek.client.ui.ViewFormatting;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.eras.Era;
import megamek.common.eras.Eras;
import megamek.common.modifiers.*;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.util.DiscordFormat;
import megamek.common.verifier.TestEntity;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;

import static megamek.client.ui.mekview.MekViewUiTexts.uiString;

/**
 * A utility class for retrieving unit information in a formatted string.
 *
 * The information is encoded in a series of classes that implement a common {@link ViewElement} interface, which can format the element
 * either in html or in plain text.
 *
 * @author Ryan McConnell
 * @since January 20, 2003
 */
public class MekView {

    /**
     * Provides common interface for various ways to present data that can be
     * formatted
     * either as HTML or as plain text.
     *
     * @see SingleLine
     * @see LabeledElement
     * @see TableElement
     * @see ItemList
     * @see Title
     * @see EmptyElement
     */
    interface ViewElement {
        String toPlainText();

        String toHTML();

        String toDiscord();
    }

    private static final Pattern numberPattern = Pattern.compile("\\b\\d+\\b");

    private static String highlightNumbersForDiscord(String original) {
        return numberPattern.matcher(original).replaceAll(DiscordFormat.NUMBER_COLOR + "$0" + DiscordFormat.WHITE);
    }

    private final Entity entity;
    private final boolean isMek;
    private final boolean isInf;
    private final boolean isBA;
    private final boolean isVehicle;
    private final boolean isProto;
    private final boolean isGunEmplacement;
    private final boolean isAero;
    private final boolean isConvFighter;
    @SuppressWarnings("unused")
    private final boolean isFixedWingSupport;
    private final boolean isSquadron;
    private final boolean isSmallCraft;
    private final boolean isJumpship;
    @SuppressWarnings("unused")
    private final boolean isSpaceStation;

    private final List<ViewElement> sHead = new ArrayList<>();
    private final List<ViewElement> sBasic = new ArrayList<>();
    private final List<ViewElement> sLoadout = new ArrayList<>();
    private final List<ViewElement> sFluff = new ArrayList<>();
    private final List<ViewElement> sQuirks = new ArrayList<>();
    private final List<ViewElement> sInvalid = new ArrayList<>();

    private final ViewFormatting formatting;

    /**
     * Compiles information about an {@link Entity} useful for showing a summary of
     * its abilities.
     * Produced output formatted in html.
     *
     * @param entity     The entity to summarize
     * @param showDetail If true, shows individual weapons that make up weapon bays.
     */
    public MekView(Entity entity, boolean showDetail) {
        this(entity, showDetail, false, ViewFormatting.HTML);
    }

    /**
     * Compiles information about an {@link Entity} useful for showing a summary of
     * its abilities.
     * Produced output formatted in html.
     *
     * @param entity           The entity to summarize
     * @param showDetail       If true, shows individual weapons that make up weapon
     *                         bays.
     * @param useAlternateCost If true, uses alternate cost calculation. This
     *                         primarily provides an
     *                         equipment-only cost for conventional infantry for
     *                         MekHQ.
     */
    public MekView(Entity entity, boolean showDetail, boolean useAlternateCost) {
        this(entity, showDetail, useAlternateCost, ViewFormatting.HTML);
    }

    /**
     * Compiles information about an {@link Entity} useful for showing a summary of
     * its abilities.
     *
     * @param entity           The entity to summarize
     * @param showDetail       If true, shows individual weapons that make up weapon
     *                         bays.
     * @param useAlternateCost If true, uses alternate cost calculation. This
     *                         primarily provides an
     *                         equipment-only cost for conventional infantry for
     *                         MekHQ.
     * @param formatting       Which formatting style to use: HTML, Discord, or None
     *                         (plaintext)
     */
    public MekView(final Entity entity, final boolean showDetail, final boolean useAlternateCost,
            final ViewFormatting formatting) {
        this(entity, showDetail, useAlternateCost, (entity.getCrew() == null), formatting);
    }

    /**
     * Compiles information about an {@link Entity} useful for showing a summary of
     * its abilities.
     *
     * @param entity           The entity to summarize
     * @param showDetail       If true, shows individual weapons that make up weapon
     *                         bays.
     * @param useAlternateCost If true, uses alternate cost calculation. This
     *                         primarily provides an
     *                         equipment-only cost for conventional infantry for
     *                         MekHQ.
     * @param ignorePilotBV    If true then the BV calculation is done without
     *                         including the pilot
     *                         BV modifiers
     * @param formatting       Which formatting style to use: HTML, Discord, or None
     *                         (plaintext)
     */
    public MekView(final Entity entity, final boolean showDetail, final boolean useAlternateCost,
            final boolean ignorePilotBV, final ViewFormatting formatting) {
        this.entity = entity;
        this.formatting = formatting;
        isMek = entity instanceof Mek;
        isInf = entity instanceof Infantry;
        isBA = entity instanceof BattleArmor;
        isVehicle = entity instanceof Tank;
        isProto = entity instanceof ProtoMek;
        isGunEmplacement = entity instanceof GunEmplacement;
        isAero = entity instanceof Aero;
        isConvFighter = entity instanceof ConvFighter;
        isFixedWingSupport = entity instanceof FixedWingSupport;
        isSquadron = entity instanceof FighterSquadron;
        isSmallCraft = entity instanceof SmallCraft;
        isJumpship = entity instanceof Jumpship;
        isSpaceStation = entity instanceof SpaceStation;

        List<ViewElement> weapons = getWeapons(showDetail);
        if (!weapons.isEmpty()) {
            sLoadout.add(new SingleLine());
            sLoadout.addAll(getWeapons(showDetail));
        }

        if (showAmmoBlock(showDetail)) {
            sLoadout.add(new SingleLine());
            sLoadout.add(getAmmo());
        }

        if (entity instanceof IBomber) {
            List<ViewElement> bombs = getBombs();
            if (!bombs.isEmpty()) {
                sLoadout.add(new SingleLine());
                sLoadout.addAll(getBombs());
            }
        }

        List<ViewElement> miscEquipment = getMisc();
        if (!miscEquipment.isEmpty()) {
            sLoadout.addAll(getMisc()); // has to occur before basic is processed
        }

        ViewElement failedEquipment = getFailed();
        if (!(failedEquipment instanceof EmptyElement)) {
            sLoadout.add(new SingleLine());
            sLoadout.add(failedEquipment);
        }

        if (isInf) {
            Infantry inf = (Infantry) entity;
            if (inf.getSpecializations() > 0) {
                ItemList specList = new ItemList("Infantry Specializations");
                for (int i = 0; i < Infantry.NUM_SPECIALIZATIONS; i++) {
                    int spec = 1 << i;
                    if (inf.hasSpecialization(spec)) {
                        specList.addItem(Infantry.getSpecializationName(spec));
                    }
                }
                sLoadout.add(specList);
            }

            if (inf.getCrew() != null) {
                ArrayList<String> augmentations = new ArrayList<>();
                for (Enumeration<IOption> e = inf.getCrew().getOptions(PilotOptions.MD_ADVANTAGES); e
                        .hasMoreElements();) {
                    final IOption o = e.nextElement();
                    if (o.booleanValue()) {
                        augmentations.add(o.getDisplayableName());
                    }
                }

                if (!augmentations.isEmpty()) {
                    ItemList augList = new ItemList("Augmentations");
                    for (String aug : augmentations) {
                        augList.addItem(aug);
                    }
                    sLoadout.add(augList);
                }
            }
        }

        sHead.add(new Title(entity.getShortNameRaw()));
        String techLevel = entity.getStaticTechLevel().toString();
        if (entity.isMixedTech()) {
            if (entity.isClan()) {
                techLevel += uiString("MixedClan");
            } else {
                techLevel += uiString("MixedIS");
            }
        } else {
            if (entity.isClan()) {
                techLevel += uiString("Clan");
            } else {
                techLevel += uiString("IS");
            }
        }
        sHead.add(new LabeledElement(uiString("BaseTechLevel"), techLevel));
        if (!entity.isDesignValid()) {
            sHead.add(new SingleLine(uiString("DesignInvalid")));
        }

        TableElement tpTable = new TableElement(3);
        String tableSpacer = "     ";
        tpTable.setColNames(uiString("Level"), tableSpacer,
                uiString("Era"));
        tpTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_LEFT);

        String eraText = entity.getExperimentalRange()
                + eraText(entity.getPrototypeDate(), entity.getProductionDate());
        tpTable.addRow(TechConstants.getSimpleLevelName(TechConstants.T_SIMPLE_EXPERIMENTAL),
                tableSpacer, eraText);

        eraText = entity.getAdvancedRange()
                + eraText(entity.getProductionDate(), entity.getCommonDate());
        tpTable.addRow(TechConstants.getSimpleLevelName(TechConstants.T_SIMPLE_ADVANCED),
                tableSpacer, eraText);

        eraText = entity.getStandardRange()
                + eraText(entity.getCommonDate(), ITechnology.DATE_NONE);
        tpTable.addRow(TechConstants.getSimpleLevelName(TechConstants.T_SIMPLE_STANDARD),
                tableSpacer, eraText);

        String extinctRange = entity.getExtinctionRange();
        if (extinctRange.length() > 1) {
            tpTable.addRow(uiString("Extinct"), tableSpacer, extinctRange);
        }
        sHead.add(tpTable);

        sHead.add(new LabeledElement(uiString("TechRating"), entity.getFullRatingName()));
        sHead.add(new SingleLine());

        if (!isInf) {
            sHead.add(new LabeledElement(uiString("Weight"),
                    Math.round(entity.getWeight()) + uiString("tons")));
        }
        DecimalFormatSymbols unusualSymbols = new DecimalFormatSymbols();
        unusualSymbols.setDecimalSeparator('.');
        unusualSymbols.setGroupingSeparator(',');
        DecimalFormat dFormatter = new DecimalFormat("#,###.##", unusualSymbols);
        sHead.add(new LabeledElement(uiString("BV"),
                dFormatter.format(entity.calculateBattleValue(false, ignorePilotBV))));
        double cost = entity.getCost(false);
        if (useAlternateCost && entity.getAlternateCost() > 0) {
            cost = entity.getAlternateCost();
        }
        sHead.add(new LabeledElement(uiString("Cost"),
                dFormatter.format(cost) + " C-bills"));
        String source = entity.getSource();
        if (!source.isBlank()) {
            if (source.contains(MMConstants.SOURCE_TEXT_SHRAPNEL)) {
                sHead.add(new HyperLinkElement(uiString("Source"), MMConstants.BT_URL_SHRAPNEL,
                        source));
            } else {
                sHead.add(new LabeledElement(uiString("Source"), source));
            }
        } else {
            sHead.add(new LabeledElement(uiString("Source"), uiString("unknown")));
        }

        if (entity.hasRole()) {
            sHead.add(new LabeledElement("Role", entity.getRole().toString()));
        }

        // We may have altered the starting mode during configuration, so we save the
        // current one here to restore it
        int originalMode = entity.getConversionMode();
        entity.setConversionMode(0);
        if (!isGunEmplacement) {
            sBasic.add(new SingleLine());
            StringBuilder moveString = new StringBuilder();
            moveString.append(entity.getWalkMP()).append("/")
                    .append(entity.getRunMPasString());
            if (entity.getJumpMP() > 0) {
                moveString.append("/")
                        .append(entity.getJumpMP());
            }
            if (entity.damagedJumpJets() > 0) {
                moveString.append("<font color='red'> (").append(entity.damagedJumpJets())
                        .append(" damaged jump jets)</font>");
            }
            if (entity.getAllUMUCount() > 0) {
                // Add in Jump MP if it wasn't already printed
                if (entity.getJumpMP() == 0) {
                    moveString.append("/0");
                }
                moveString.append("/")
                        .append(entity.getActiveUMUCount());
                if ((entity.getAllUMUCount() - entity.getActiveUMUCount()) != 0) {
                    moveString.append("<font color='red'> (")
                            .append(entity.getAllUMUCount() - entity.getActiveUMUCount())
                            .append(" damaged UMUs)</font>");
                }
            }
            if (isVehicle) {
                moveString.append(" (").append(Messages
                        .getString("MovementType." + entity.getMovementModeAsString()))
                        .append(")");
                if ((((Tank) entity).getMotiveDamage() > 0)
                        || (((Tank) entity).getMotivePenalty() > 0)) {
                    moveString.append(" ").append(warningStart())
                            .append("(motive damage: -")
                            .append(((Tank) entity).getMotiveDamage())
                            .append("MP/-")
                            .append(((Tank) entity).getMotivePenalty())
                            .append(" piloting)")
                            .append(warningEnd());
                }
            }
            if ((entity instanceof Infantry infantry) && infantry.getMount() != null) {
                moveString.append(" (").append(infantry.getMount().getName()).append(")");
            }

            // TODO : Add STOL message as part of the movement line
            if (isConvFighter && ((Aero) entity).isVSTOL()) {
                sBasic.add(new LabeledElement(uiString("Movement"),
                        moveString.toString().concat(
                                String.format(" (%s)", uiString("VSTOL")))));
            } else {
                sBasic.add(new LabeledElement(uiString("Movement"), moveString.toString()));
            }
        }
        if (isBA && ((BattleArmor) entity).isBurdened()) {
            sBasic.add(new SingleLine(italicsStart()
                    + uiString("Burdened")
                    + italicsEnd()));
        }
        if (isBA && ((BattleArmor) entity).hasDWP()) {
            sBasic.add(new SingleLine(italicsStart()
                    + uiString("DWPBurdened")
                    + italicsEnd()));
        }
        if (entity instanceof QuadVee) {
            entity.setConversionMode(QuadVee.CONV_MODE_VEHICLE);
            sBasic.add(new LabeledElement(Messages.getString("MovementType."
                    + entity.getMovementModeAsString()),
                    entity.getWalkMP() + "/" + entity.getRunMPasString()));
            entity.setConversionMode(originalMode);
        } else if (entity instanceof LandAirMek) {
            if (((LandAirMek) entity).getLAMType() == LandAirMek.LAM_STANDARD) {
                sBasic.add(new LabeledElement(Messages.getString("MovementType.AirMek"),
                        ((LandAirMek) entity).getAirMekWalkMP() + "/"
                                + ((LandAirMek) entity).getAirMekRunMP() + "/"
                                + ((LandAirMek) entity).getAirMekCruiseMP() + "/"
                                + ((LandAirMek) entity).getAirMekFlankMP()));
            }

            entity.setConversionMode(LandAirMek.CONV_MODE_FIGHTER);
            sBasic.add(new LabeledElement(Messages.getString("MovementType.Fighter"),
                    entity.getWalkMP() + "/" + entity.getRunMP()));
            entity.setConversionMode(originalMode);
        }

        if (isMek || isVehicle
                || (isAero && !isSmallCraft && !isJumpship && !isSquadron)) {
            String engineName = entity.hasEngine() ? entity.getEngine().getShortEngineName() : "(none)";
            if (entity.getEngineHits() > 0) {
                engineName += " " + warningStart() + "(" + entity.getEngineHits()
                        + " hits)" + warningEnd();
            }
            if (isMek && entity.hasArmoredEngine()) {
                engineName += " (armored)";
            }
            sBasic.add(new LabeledElement(uiString("engine"), engineName));
        }
        if (!entity.hasPatchworkArmor() && entity.hasBARArmor(1)) {
            sBasic.add(new LabeledElement(uiString("BARRating"),
                    String.valueOf(entity.getBARRating(0))));
        }

        if (isAero && !isConvFighter) {
            Aero a = (Aero) entity;
            StringBuilder hsString = new StringBuilder(String.valueOf(a.getHeatSinks()));
            if (a.getPodHeatSinks() > 0) {
                hsString.append(" (").append(a.getPodHeatSinks()).append(" ")
                        .append(uiString("Pod")).append(")");
            }
            if (!a.formatHeat().equals(Integer.toString(a.getHeatSinks()))) {
                hsString.append(" [")
                        .append(a.formatHeat()).append("]");
            }
            if (a.getHeatSinkHits() > 0) {
                hsString.append(warningStart()).append(" (").append(a.getHeatSinkHits())
                        .append(" damaged)").append(warningEnd());
            }
            sBasic.add(new LabeledElement(uiString("HeatSinks"), hsString.toString()));

            sBasic.add(new LabeledElement(uiString("cockpit"),
                    a.getCockpitTypeString()));
        }

        if (isMek) {
            Mek aMek = (Mek) entity;
            StringBuilder hsString = new StringBuilder();
            hsString.append(aMek.heatSinks());
            if (!aMek.formatHeat().equals(Integer.toString(aMek.heatSinks()))) {
                hsString.append(" [").append(aMek.formatHeat()).append("]");
            }
            if (aMek.hasRiscHeatSinkOverrideKit()) {
                hsString.append(" w/ RISC Heat Sink Override Kit");
            }
            if (aMek.damagedHeatSinks() > 0) {
                hsString.append(" ").append(warningStart()).append("(")
                        .append(aMek.damagedHeatSinks())
                        .append(" damaged)").append(warningEnd());
            }
            sBasic.add(new LabeledElement(aMek.getHeatSinkTypeName() + "s", hsString.toString()));
            sBasic.add(new LabeledElement(uiString("cockpit"),
                    aMek.getCockpitTypeString()
                            + (aMek.hasArmoredCockpit() ? " (armored)" : "")));

            String gyroString = aMek.getGyroTypeString();
            if (aMek.getGyroHits() > 0) {
                gyroString += " " + warningStart() + "(" + aMek.getGyroHits()
                        + " hits)" + warningEnd();
            }
            if (aMek.hasArmoredGyro()) {
                gyroString += " (armored)";
            }
            sBasic.add(new LabeledElement(uiString("gyro"), gyroString));
        }

        if (isAero) {
            Aero a = (Aero) entity;
            if (!a.getCritDamageString().isEmpty()) {
                sBasic.add(new LabeledElement(uiString("SystemDamage"),
                        warningStart() + a.getCritDamageString() + warningEnd()));
            }

            String fuel = String.valueOf(a.getCurrentFuel());
            if (a.getCurrentFuel() < a.getFuel()) {
                fuel += "/" + a.getFuel();
            }
            sBasic.add(new LabeledElement(uiString("FuelPoints"),
                    String.format(uiString("Fuel.format"), fuel, a.getFuelTonnage())));

            // Display Strategic Fuel Use for Small Craft and up
            if (isSmallCraft || isJumpship) {
                sBasic.add(new LabeledElement(uiString("TonsPerBurnDay"),
                        String.format("%2.2f", a.getStrategicFuelUse())));
            }
        }
        if (!isGunEmplacement) {
            sBasic.add(new SingleLine());
            if (isSquadron) {
                sBasic.addAll(getArmor());
            } else if (isAero) {
                sBasic.addAll(getSIandArmor());
            } else {
                sBasic.addAll(getInternalAndArmor());
            }
        }

        Game game = entity.getGame();

        // Unit and Weapon Quirks
        if ((game == null) || game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            List<String> activeUnitQuirksNames = entity.getQuirks().activeQuirks().stream()
                    .map(IOption::getDisplayableNameWithValue)
                    .toList();

            if (!activeUnitQuirksNames.isEmpty()) {
                sQuirks.add(new SingleLine());
                ItemList list = new ItemList(uiString("Quirks"));
                activeUnitQuirksNames.forEach(list::addItem);
                sQuirks.add(list);
            }

            List<String> wpQuirksList = new ArrayList<>();
            for (Mounted<?> weapon : entity.getWeaponList()) {
                List<String> activeWeaponQuirksNames = weapon.getQuirks().activeQuirks().stream()
                    .map(IOption::getDisplayableNameWithValue)
                    .toList();
                if (!activeWeaponQuirksNames.isEmpty()) {
                    String wq = weapon.getDesc() + " (" + entity.getLocationAbbr(weapon.getLocation()) + "): ";
                    wq += String.join(", ", activeWeaponQuirksNames);
                    wpQuirksList.add(wq);
                }
            }
            if (!wpQuirksList.isEmpty()) {
                sQuirks.add(new SingleLine());
                ItemList list = new ItemList(uiString("WeaponQuirks"));
                wpQuirksList.forEach(list::addItem);
                sQuirks.add(list);
            }
        }

        // Equipment and Unit modifiers (salvage quality etc)
        List<String> equipmentModifierList = new ArrayList<>();
        for (Mounted<?> equipment : entity.getEquipment()) {
            List<String> weaponModifiers = getEquipmentModifiers(equipment);
            if (!weaponModifiers.isEmpty()) {
                String wq = equipment.getDesc() + " (" + entity.getLocationAbbr(equipment.getLocation()) + "): ";
                wq += String.join(", ", weaponModifiers);
                equipmentModifierList.add(wq);
            }
        }

        List<String> unitModifiers = getEquipmentModifiers(entity);
        if (!unitModifiers.isEmpty()) {
            equipmentModifierList.add(uiString("systems") + ": " + String.join(", ", unitModifiers));
        }

        if (entity.hasEngine()) {
            List<String> engineModifiers = getEquipmentModifiers(entity.getEngine());
            if (!engineModifiers.isEmpty()) {
                equipmentModifierList.add(uiString("engine") + ": " + String.join(", ", engineModifiers));
            }
        }

        if (!equipmentModifierList.isEmpty()) {
            sQuirks.add(new SingleLine());
            ItemList list = new ItemList(uiString("equipmentmods"));
            equipmentModifierList.forEach(list::addItem);
            sQuirks.add(list);
        }

        sFluff.addAll(sQuirks);
        if (!entity.getFluff().getOverview().isEmpty()) {
            sFluff.add(new SingleLine());
            sFluff.add(new LabeledElement("Overview", entity.getFluff().getOverview()));
        }
        if (!entity.getFluff().getCapabilities().isEmpty()) {
            sFluff.add(new SingleLine());
            sFluff.add(new LabeledElement("Capabilities", entity.getFluff().getCapabilities()));
        }
        if (!entity.getFluff().getDeployment().isEmpty()) {
            sFluff.add(new SingleLine());
            sFluff.add(new LabeledElement("Deployment", entity.getFluff().getDeployment()));
        }
        if (!entity.getFluff().getHistory().isEmpty()) {
            sFluff.add(new SingleLine());
            sFluff.add(new LabeledElement("History", entity.getFluff().getHistory()));
        }

        StringBuffer sb = new StringBuffer();
        TestEntity testEntity = TestEntity.getEntityVerifier(entity);

        if (testEntity != null) {
            testEntity.correctEntity(sb);
            if (!sb.isEmpty()) {
                sInvalid.add(new SingleLine());
                String[] errorLines = sb.toString().split("\n");
                String label = entity.hasQuirk(OptionsConstants.QUIRK_NEG_ILLEGAL_DESIGN)
                        ? uiString("InvalidButIllegalQuirk")
                        : uiString("InvalidReasons");
                ItemList errorList = new ItemList(label);
                Arrays.stream(errorLines).forEach(errorList::addItem);
                sInvalid.add(errorList);
            }
        }
    }

    private List<String> getEquipmentModifiers(Modifiable modifiable) {
        return modifiable.getModifiers().stream().map(this::equipmentModifierText).toList();
    }

    private String equipmentModifierText(EquipmentModifier modifier) {
        if (modifier instanceof AbstractSystemModifier systemModifier) {
            return "%s %s (%s)".formatted(modifierReason(modifier), modifierSystem(systemModifier), modifierText(modifier));
        } else {
            return "%s (%s)".formatted(modifierReason(modifier), modifierText(modifier));
        }
    }

    private String modifierSystem(AbstractSystemModifier modifier) {
        return switch (modifier.system()) {
            case CONTROLS -> uiString("controls");
            case AVIONICS -> uiString("avionics");
            case LIFE_SUPPORT -> uiString("lifesupport");
            case GYRO -> uiString("gyro");
            case COCKPIT -> uiString("cockpit");
            case NONE -> uiString("unknown");
        };
    }

    private String modifierReason(EquipmentModifier modifier) {
        return switch (modifier.reason()) {
            case SALVAGE_QUALITY -> uiString("SalvageQuality");
            case PARTIAL_REPAIR -> uiString("PartialRepair");
            case DAMAGED -> uiString("damaged");
            case UNKNOWN -> uiString("unknown");
        };
    }

    private String modifierText(EquipmentModifier modifier) {
        if (modifier instanceof HeatModifier heatModifier) {
            return heatModifier.formattedDeltaHeat() + " heat";
        } else if (modifier instanceof ToHitModifier toHitModifier) {
            return toHitModifier.formattedToHitModifier() + " to-hit";
        } else if (modifier instanceof DamageModifier damageModifier) {
            return damageModifier.formattedDamageModifier() + " damage";
        } else if (modifier instanceof WeaponJamModifier) {
            return "weapon may jam";
        } else if (modifier instanceof NoTwistModifier) {
            return "unit cannot twist";
        } else if (modifier instanceof WalkMPEquipmentModifier walkMPModifier) {
            return "%s %s MP".formatted(walkMPModifier.formattedMPModifier(), walkEquivalentText());
        } else if (modifier instanceof RunMPEquipmentModifier runMPModifier) {
            return "%s %s MP".formatted(runMPModifier.formattedMPModifier(), runEquivalentText());
        } else {
            return uiString("unknown");
        }
    }

    private String walkEquivalentText() {
        if (entity instanceof Mek || entity instanceof Infantry || entity instanceof ProtoMek) {
            return uiString("walk");
        } else if (entity.isAero()) {
            return uiString("safethrust");
        } else {
            return uiString("cruise");
        }
    }

    private String runEquivalentText() {
        if ((entity instanceof Mek) || (entity instanceof Infantry) || (entity instanceof ProtoMek)) {
            return uiString("run");
        } else if (entity.isAero()) {
            return uiString("maxthrust");
        } else {
            return uiString("flank");
        }
    }

    /** @return True when the unit requires an ammo block. */
    private boolean showAmmoBlock(boolean showDetail) {
        return (!entity.usesWeaponBays() || !showDetail) && !entity.getAmmo().stream().allMatch(this::hideAmmo);
    }

    private String eraText(int startYear, int endYear) {
        String eraText = "";
        if (startYear != ITechnology.DATE_NONE) {
            Era startEra = Eras.getEra(startYear);
            Era endEra = Eras.getEra(endYear - 1);
            eraText = " (" + startEra.name();
            if (endYear == ITechnology.DATE_NONE) {
                eraText += " -";
            } else if (!endEra.equals(startEra)) {
                eraText += " to " + endEra.name();
            }
            eraText += ")";
        }
        return eraText;
    }

    /**
     * Converts a list of {@link ViewElement}s to a String using the selected
     * format.
     *
     * @param section The elements to format.
     * @return The formatted data.
     */
    private String getReadout(List<ViewElement> section) {
        Function<ViewElement, String> mapper = switch (formatting) {
            case HTML -> ViewElement::toHTML;
            case NONE -> ViewElement::toPlainText;
            case DISCORD -> ViewElement::toDiscord;
        };
        return section.stream().map(mapper).collect(Collectors.joining());
    }

    /**
     * The head section includes the title (unit name), tech level and availability,
     * tonnage, bv, and cost.
     *
     * @return The data from the head section.
     */
    public String getMekReadoutHead() {
        return getReadout(sHead);
    }

    /**
     * The basic section includes general details such as movement, system equipment
     * (cockpit, gyro, etc.)
     * and armor.
     *
     * @return The data from the basic section
     */
    public String getMekReadoutBasic() {
        return getReadout(sBasic);
    }

    /**
     * The invalid section includes reasons why the unit is invalid
     *
     * @return The data from the invalid section
     */
    public String getMekReadoutInvalid() {
        return getReadout(sInvalid);
    }

    /**
     * The loadout includes weapons, ammo, and other equipment broken down by
     * location.
     *
     * @return The data from the loadout section.
     */
    public String getMekReadoutLoadout() {
        return getReadout(sLoadout);
    }

    /**
     * The fluff section includes fluff details like unit history and deployment
     * patterns
     * as well as quirks.
     *
     * @return The data from the fluff section.
     */
    public String getMekReadoutFluff() {
        if (formatting == ViewFormatting.DISCORD) {
            // The rest of the fluff often doesn't fit in a Discord message
            return getReadout(sQuirks);
        }
        return getReadout(sFluff);
    }

    /**
     * @return A summary including all four sections.
     */
    public String getMekReadout() {
        return getMekReadout(null);
    }

    /**
     * @return A summary including all four sections.
     */
    public String getMekReadout(@Nullable String fontName) {
        String docStart = "";
        String docEnd = "";

        if (formatting == ViewFormatting.HTML && (fontName != null)) {
            docStart = "<div style=\"font-family:" + fontName + ";\">";
            docEnd = "</div>";
        } else if (formatting == ViewFormatting.DISCORD) {
            docStart = "```ansi\n";
            docEnd = "```";
        }
        return docStart + getMekReadoutHead()
                + getMekReadoutBasic() + getMekReadoutLoadout()
                + getMekReadoutFluff() + getMekReadoutInvalid() + docEnd;
    }

    private List<ViewElement> getInternalAndArmor() {
        List<ViewElement> retVal = new ArrayList<>();

        int maxArmor = (entity.getTotalInternal() * 2) + 3;
        if (isInf && !isBA) {
            Infantry inf = (Infantry) entity;
            retVal.add(new LabeledElement(uiString("Men"),
                    entity.getTotalInternal()
                            + " (" + inf.getSquadSize() + "/" + inf.getSquadCount()
                            + ")"));
        } else {
            String internal = String.valueOf(entity.getTotalInternal());
            if (isMek) {
                internal += uiString(EquipmentType.getStructureTypeName(entity.getStructureType()));
            }
            retVal.add(new LabeledElement(uiString("Internal"),
                    internal));
        }

        if (isInf && !isBA) {
            Infantry inf = (Infantry) entity;
            retVal.add(new LabeledElement(uiString("Armor"),
                    inf.getArmorDesc()));
        } else {
            String armor = String.valueOf(entity.getTotalArmor());
            if (isMek) {
                armor += "/" + maxArmor;
            }
            if (!isInf && !isProto && !entity.hasPatchworkArmor()) {
                armor += " (" + ArmorType.forEntity(entity).getName() + ")";
            }
            if (isBA) {
                armor += " " + EquipmentType.getArmorTypeName(entity.getArmorType(1)).trim();
            }
            retVal.add(new LabeledElement(uiString("Armor"), armor));

        }
        // Walk through the entity's locations.

        if (!(isInf && !isBA)) {
            TableElement locTable = new TableElement(5);
            locTable.setColNames("", "Internal", uiString("Armor"), "", "");
            // last two columns are patchwork armor and location damage
            locTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_CENTER,
                    TableElement.JUSTIFIED_CENTER, TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_LEFT);
            for (int loc = 0; loc < entity.locations(); loc++) {
                // Skip empty sections.
                if (entity.getInternal(loc) == IArmorState.ARMOR_NA) {
                    continue;
                }
                // Skip nonexistent turrets by vehicle type, as well as the
                // body location.
                if (isVehicle) {
                    if (loc == Tank.LOC_BODY) {
                        continue;
                    }
                    if ((loc == ((Tank) entity).getLocTurret())
                            && ((Tank) entity).hasNoTurret()) {
                        continue;
                    }

                }
                String[] row = { entity.getLocationName(loc),
                        renderArmor(entity.getInternalForReal(loc), entity.getOInternal(loc), formatting),
                        "", "", "" };

                if (IArmorState.ARMOR_NA != entity.getArmorForReal(loc)) {
                    row[2] = renderArmor(entity.getArmorForReal(loc),
                            entity.getOArmor(loc), formatting);
                }
                if (entity.hasPatchworkArmor()) {
                    row[3] = ArmorType.forEntity(entity, loc).getName();
                }
                if (!entity.getLocationDamage(loc).isEmpty()) {
                    row[4] = warningStart() + entity.getLocationDamage(loc) + warningEnd();
                }
                locTable.addRow(row);
                if (entity.hasRearArmor(loc)) {
                    row = new String[] { entity.getLocationName(loc) + " (rear)", "",
                            renderArmor(entity.getArmorForReal(loc, true),
                                    entity.getOArmor(loc, true), formatting),
                            "", "" };
                    locTable.addRow(row);
                }
            }
            retVal.add(locTable);
        }
        return retVal;
    }

    private List<ViewElement> getSIandArmor() {
        Aero a = (Aero) entity;

        List<ViewElement> retVal = new ArrayList<>();

        retVal.add(new LabeledElement(uiString("SI"),
                renderArmor(a.getSI(), a.get0SI(), formatting)));

        // if it is a jumpship get sail and KF integrity
        if (isJumpship) {
            Jumpship js = (Jumpship) entity;

            // TODO: indicate damage.
            if (js.hasSail()) {
                retVal.add(new LabeledElement(uiString("SailIntegrity"),
                        String.valueOf(js.getSailIntegrity())));
            }

            if (js.getDriveCoreType() != Jumpship.DRIVE_CORE_NONE) {
                retVal.add(new LabeledElement(uiString("KFIntegrity"),
                        String.valueOf(js.getKFIntegrity())));
            }
        }

        String armor = String.valueOf(entity.isCapitalFighter() ? a.getCapArmor() : a.getTotalArmor());
        if (isJumpship) {
            armor += uiString("CapitalArmor");
        }
        if (!entity.hasPatchworkArmor()) {
            armor += " " + ArmorType.forEntity(entity).getName();
        }
        retVal.add(new LabeledElement(uiString("Armor"),
                armor));

        // Walk through the entity's locations.
        if (!entity.isCapitalFighter()) {
            TableElement locTable = new TableElement(3);
            locTable.setColNames("", "Armor", "");
            locTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_CENTER,
                    TableElement.JUSTIFIED_LEFT);
            for (int loc = 0; loc < entity.locations(); loc++) {

                // Skip empty sections.
                if (IArmorState.ARMOR_NA == entity.getInternal(loc)) {
                    continue;
                }
                // skip broadsides on warships
                if (isJumpship && (loc >= Jumpship.LOC_HULL)) {
                    continue;
                }
                if (isSmallCraft && (loc >= SmallCraft.LOC_HULL)) {
                    continue;
                }
                // skip the "Wings" location
                if (!a.isLargeCraft() && (loc >= Aero.LOC_WINGS)) {
                    continue;
                }
                String[] row = { entity.getLocationName(loc), "", "" };
                if (IArmorState.ARMOR_NA != entity.getArmor(loc)) {
                    row[1] = renderArmor(entity.getArmor(loc),
                            entity.getOArmor(loc), formatting);
                }
                if (entity.hasPatchworkArmor()) {
                    row[2] = uiString(EquipmentType.getArmorTypeName(entity.getArmorType(loc)).trim());
                    if (entity.hasBARArmor(loc)) {
                        row[2] += uiString("BARRating") + entity.getBARRating(loc);
                    }
                }
                locTable.addRow(row);
            }
            retVal.add(locTable);
        }

        return retVal;
    }

    private List<ViewElement> getArmor() {
        FighterSquadron fs = (FighterSquadron) entity;

        List<ViewElement> retVal = new ArrayList<>();

        retVal.add(new LabeledElement(uiString("Armor"),
                String.valueOf(fs.getTotalArmor())));

        retVal.add(new LabeledElement(uiString("ActiveFighters"),
                String.valueOf(fs.getActiveSubEntities().size())));

        return retVal;
    }

    private List<ViewElement> getWeapons(boolean showDetail) {

        List<ViewElement> retVal = new ArrayList<>();

        if (isInf && !isBA) {
            Infantry inf = (Infantry) entity;
            retVal.add(new LabeledElement("Primary Weapon",
                    (null != inf.getPrimaryWeapon()) ? inf.getPrimaryWeapon().getDesc() : "None"));
            retVal.add(new LabeledElement("Secondary Weapon",
                    (null != inf.getSecondaryWeapon()) ? inf.getSecondaryWeapon().getDesc()
                            + " (" + inf.getSecondaryWeaponsPerSquad() + ")" : "None"));
            retVal.add(new LabeledElement("Damage per trooper",
                    String.format("%3.3f", inf.getDamagePerTrooper())));
            retVal.add(new SingleLine());
        }

        if (entity.getWeaponList().isEmpty()) {
            return retVal;
        }

        TableElement wpnTable = new TableElement(4);
        wpnTable.setColNames("Weapons  ", "  Loc  ", entity.isOmni() ? "  Omni  " : "");
        wpnTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_CENTER, TableElement.JUSTIFIED_CENTER);
        for (WeaponMounted mounted : entity.getWeaponList()) {
            String[] row = { mounted.getDesc() + quirkAndModMarker(mounted),
                    entity.joinLocationAbbr(mounted.allLocations(), 3), "", "" };
            WeaponType wtype = mounted.getType();

            if (entity.isClan() && (mounted.getType().getTechBase() == ITechnology.TECH_BASE_IS)) {
                row[0] += uiString("IS");
            }
            if (!entity.isClan() && (mounted.getType().getTechBase() == ITechnology.TECH_BASE_CLAN)) {
                row[0] += uiString("Clan");
            }

            int damagedWeaponsInBay = 0;
            if (wtype instanceof BayWeapon) {
                for (WeaponMounted m : mounted.getBayWeapons()) {
                    if (m.isDestroyed()) {
                        damagedWeaponsInBay++;
                    }
                }
            }

            if (entity.isOmni()) {
                row[2] = Messages.getString(mounted.isOmniPodMounted() ? "MekView.Pod" : "MekView.Fixed");
            } else if (damagedWeaponsInBay > 0 && !showDetail) {
                row[2] = warningStart() + uiString("WeaponDamage") + ")" + warningEnd();
            }
            if (mounted.isDestroyed()) {
                if (mounted.isRepairable()) {
                    wpnTable.addRowWithColor("yellow", row);
                } else {
                    wpnTable.addRowWithColor("red", row);
                }
            } else {
                wpnTable.addRow(row);
            }

            // if this is a weapon bay, then cycle through weapons and ammo
            if ((wtype instanceof BayWeapon) && showDetail) {
                for (WeaponMounted m : mounted.getBayWeapons()) {
                    row = new String[] { m.getDesc(), "", "", "" };

                    if (entity.isClan() && (mounted.getType().getTechBase() == ITechnology.TECH_BASE_IS)) {
                        row[0] += uiString("IS");
                    }
                    if (!entity.isClan() && (mounted.getType().getTechBase() == ITechnology.TECH_BASE_CLAN)) {
                        row[0] += uiString("Clan");
                    }
                    if (m.isDestroyed()) {
                        if (m.isRepairable()) {
                            wpnTable.addRowWithColor("yellow", row);
                        } else {
                            wpnTable.addRowWithColor("red", row);
                        }
                    } else {
                        wpnTable.addRow(row);
                    }
                }
                for (AmmoMounted m : mounted.getBayAmmo()) {
                    // Ignore ammo for one-shot launchers
                    if ((m.getLinkedBy() != null)
                            && m.getLinkedBy().isOneShot()) {
                        continue;
                    }
                    if (mounted.getLocation() != Entity.LOC_NONE) {
                        row = new String[] { m.getName(), String.valueOf(m.getBaseShotsLeft()), "", "" };
                        if (m.isDestroyed()) {
                            wpnTable.addRowWithColor("red", row);
                        } else if (m.getUsableShotsLeft() < 1) {
                            wpnTable.addRowWithColor("yellow", row);
                        } else {
                            wpnTable.addRow(row);
                        }
                    }
                }
            }
        }
        retVal.add(wpnTable);
        return retVal;
    }

    private String quirkAndModMarker(Mounted<?> mounted) {
        List<String> markers = new ArrayList<>();
        if (mounted.countQuirks() > 0) {
            markers.add("Q");
        }
        if (mounted.isModified()) {
            markers.add("M");
        }
        return markers.isEmpty() ? "" : " (%s)".formatted(String.join(", ", markers));
    }

    private boolean hideAmmo(Mounted<?> mounted) {
        return ((mounted.getLinkedBy() != null) && mounted.getLinkedBy().isOneShot())
                || (mounted.getSize() == 0) || (mounted.getLocation() == Entity.LOC_NONE);
    }

    private ViewElement getAmmo() {
        TableElement ammoTable = new TableElement(4);
        ammoTable.setColNames("Ammo", "Loc", "Shots", entity.isOmni() ? "Omni" : "");
        ammoTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_CENTER,
                TableElement.JUSTIFIED_CENTER, TableElement.JUSTIFIED_CENTER);

        for (Mounted<?> mounted : entity.getAmmo()) {
            if (hideAmmo(mounted)) {
                continue;
            }

            String[] row = { mounted.getName(), entity.getLocationAbbr(mounted.getLocation()),
                    String.valueOf(mounted.getBaseShotsLeft()), "" };
            if (entity.isOmni()) {
                row[3] = Messages.getString(mounted.isOmniPodMounted() ? "MekView.Pod" : "MekView.Fixed");
            }

            if (mounted.isDestroyed()) {
                ammoTable.addRowWithColor("red", row);
            } else if (mounted.getUsableShotsLeft() < 1) {
                ammoTable.addRowWithColor("yellow", row);
            } else {
                ammoTable.addRow(row);
            }
        }
        if (entity.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            for (Mounted<?> mounted : entity.getWeaponList()) {
                String[] row = { mounted.getName(),
                        entity.getLocationAbbr(mounted.getLocation()),
                        String.valueOf((int) mounted.getSize() * ((InfantryWeapon) mounted.getType()).getShots()),
                        "" };
                if (entity.isOmni()) {
                    row[3] = mounted.isOmniPodMounted() ? uiString("Pod")
                            : uiString("Fixed");
                }
                int shotsLeft = 0;
                for (Mounted<?> current = mounted.getLinked(); current != null; current = current.getLinked()) {
                    shotsLeft += current.getUsableShotsLeft();
                }
                if (mounted.isDestroyed()) {
                    ammoTable.addRowWithColor("red", row);
                } else if (shotsLeft < 1) {
                    ammoTable.addRowWithColor("yellow", row);
                } else {
                    ammoTable.addRow(row);
                }
            }
        }
        return ammoTable;
    }

    private List<ViewElement> getBombs() {
        List<ViewElement> retVal = new ArrayList<>();
        IBomber b = (IBomber) entity;
        int[] choices = b.getIntBombChoices();
        for (int type = 0; type < BombType.B_NUM; type++) {
            if (choices[type] > 0) {
                retVal.add(new SingleLine(BombType.getBombName(type) + " (" + choices[type] + ") [Int. Bay]"));
            }
        }
        choices = b.getExtBombChoices();
        for (int type = 0; type < BombType.B_NUM; type++) {
            if (choices[type] > 0) {
                retVal.add(new SingleLine(BombType.getBombName(type) + " (" + choices[type] + ")"));
            }
        }
        return retVal;
    }

    private List<ViewElement> getMisc() {
        List<ViewElement> retVal = new ArrayList<>();

        TableElement miscTable = new TableElement(3);
        miscTable.setColNames("Equipment", "Loc", entity.isOmni() ? "Omni" : "");
        miscTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_CENTER,
                TableElement.JUSTIFIED_CENTER);
        int nEquip = 0;
        for (Mounted<?> mounted : entity.getMisc()) {
            String name = mounted.getName();
            if ((((mounted.getLocation() == Entity.LOC_NONE)
                    // Meks can have zero-slot equipment in LOC_NONE that needs to be shown.
                    && (!isMek || mounted.getCriticals() > 0)))
                    || name.contains("Jump Jet")
                    || (name.contains("CASE")
                            && !name.contains("II")
                            && entity.isClan())
                    || (name.contains("Heat Sink")
                            && !name.contains("Radical"))
                    || EquipmentType.isArmorType(mounted.getType())
                    || EquipmentType.isStructureType(mounted.getType())) {
                // These items are displayed elsewhere, so skip them here.
                continue;
            }
            nEquip++;

            String[] row = { mounted.getDesc(), entity.joinLocationAbbr(mounted.allLocations(), 3), "" };
            if (entity.isClan()
                    && (mounted.getType().getTechBase() == ITechnology.TECH_BASE_IS)) {
                row[0] += uiString("IS");
            }
            if (!entity.isClan()
                    && (mounted.getType().getTechBase() == ITechnology.TECH_BASE_CLAN)) {
                row[0] += uiString("Clan");
            }

            if (entity.isOmni()) {
                row[2] = Messages.getString(mounted.isOmniPodMounted() ? "MekView.Pod" : "MekView.Fixed");
            }

            if (mounted.isDestroyed()) {
                miscTable.addRowWithColor("red", row);
            } else {
                miscTable.addRow(row);
            }
        }

        if (nEquip > 0) {
            retVal.add(new SingleLine());
            retVal.add(miscTable);
        }

        String transportersString = entity.getUnusedString(formatting);
        if (!transportersString.isBlank()) {
            retVal.add(new SingleLine());
            // Reformat the list to a table to keep the formatting similar between blocks
            TableElement transportTable = new TableElement(1);
            transportTable.setColNames(uiString("CarryingCapacity"));
            transportTable.setJustification(TableElement.JUSTIFIED_LEFT);
            String separator = formatting == ViewFormatting.HTML ? "<br>" : "\n";
            String[] transportersLines = transportersString.split(separator);
            for (String line : transportersLines) {
                transportTable.addRow(line);
            }
            retVal.add(transportTable);
        }

        if (isSmallCraft || isJumpship) {
            Aero a = (Aero) entity;

            TableElement crewTable = new TableElement(2);
            crewTable.setColNames(uiString("Crew"), "");
            crewTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_RIGHT);
            crewTable.addRow(uiString("Officers"), String.valueOf(a.getNOfficers()));
            crewTable.addRow(uiString("Enlisted"),
                    String.valueOf(Math.max(a.getNCrew()
                            - a.getBayPersonnel() - a.getNGunners() - a.getNOfficers(), 0)));
            crewTable.addRow(uiString("Gunners"), String.valueOf(a.getNGunners()));
            crewTable.addRow(uiString("BayPersonnel"), String.valueOf(a.getBayPersonnel()));
            if (a.getNPassenger() > 0) {
                crewTable.addRow(uiString("Passengers"), String.valueOf(a.getNPassenger()));
            }
            if (a.getNMarines() > 0) {
                crewTable.addRow(uiString("Marines"), String.valueOf(a.getNMarines()));
            }
            if (a.getNBattleArmor() > 0) {
                crewTable.addRow(uiString("BAMarines"), String.valueOf(a.getNBattleArmor()));
            }
            retVal.add(new SingleLine());
            retVal.add(crewTable);
        }
        if (isVehicle && ((Tank) entity).getExtraCrewSeats() > 0) {
            retVal.add(new SingleLine(uiString("ExtraCrewSeats")
                    + ((Tank) entity).getExtraCrewSeats()));
        }
        return retVal;
    }

    private ViewElement getFailed() {
        Iterator<String> eFailed = entity.getFailedEquipment();
        if (eFailed.hasNext()) {
            ItemList list = new ItemList("The following equipment slots failed to load:");
            while (eFailed.hasNext()) {
                list.addItem(eFailed.next());
            }
            return list;
        }
        return new EmptyElement();
    }

    private static String renderArmor(int nArmor, int origArmor, ViewFormatting formatting) {
        double percentRemaining = ((double) nArmor) / ((double) origArmor);
        String armor = Integer.toString(nArmor);

        String warnBegin;
        String warnEnd;
        String cautionBegin;
        String cautionEnd;

        switch (formatting) {
            case HTML:
                warnBegin = "<FONT " + UIUtil.colorString(GUIPreferences.getInstance().getWarningColor()) + '>';
                warnEnd = "</FONT>";
                cautionBegin = "<FONT " + UIUtil.colorString(GUIPreferences.getInstance().getCautionColor()) + '>';
                cautionEnd = "</FONT>";
                break;
            case NONE:
                warnBegin = "";
                warnEnd = "";
                cautionBegin = "";
                cautionEnd = "";
                break;
            case DISCORD:
                warnBegin = DiscordFormat.RED.toString();
                warnEnd = DiscordFormat.RESET.toString();
                cautionBegin = DiscordFormat.YELLOW.toString();
                cautionEnd = DiscordFormat.RESET.toString();
                break;
            default:
                throw new IllegalStateException("Impossible");
        }

        if (percentRemaining < 0) {
            return warnBegin + 'X' + warnEnd;
        } else if (percentRemaining <= .25) {
            return warnBegin + armor + warnEnd;
        } else if (percentRemaining < 1.00) {
            return cautionBegin + armor + cautionEnd;
        } else {
            return armor;
        }
    }

    /**
     * Used when an element is expected but the unit has no data for it. Outputs an
     * empty string.
     */
    private static class EmptyElement implements ViewElement {

        @Override
        public String toPlainText() {
            return "";
        }

        @Override
        public String toHTML() {
            return "";
        }

        @Override
        public String toDiscord() {
            return "";
        }

    }

    /**
     * Basic one-line entry consisting of a label, a colon, and a value. In html and
     * discord the label is bold.
     *
     */
    private static class LabeledElement implements ViewElement {
        private final String label;
        private final String value;

        LabeledElement(String label, String value) {
            this.label = label;
            this.value = value;
        }

        @Override
        public String toPlainText() {
            String htmlCleanedText = value.replaceAll("<[Bb][Rr]> *", "\n")
                    .replaceAll("<[Pp]> *", "\n\n")
                    .replaceAll("</[Pp]> *", "\n")
                    .replaceAll("<[^>]*>", "");
            return label + ": " + htmlCleanedText + '\n';
        }

        @Override
        public String toHTML() {
            return "<b>" + label + "</b>: " + value + "<br>";
        }

        @Override
        public String toDiscord() {
            String htmlCleanedText = value.replaceAll("<[Bb][Rr]> *", "\n")
                    .replaceAll("<[Pp]> *", "\n\n")
                    .replaceAll("</[Pp]> *", "\n")
                    .replaceAll("<[^>]*>", "");
            return DiscordFormat.BOLD + label + DiscordFormat.RESET + ": " + highlightNumbersForDiscord(htmlCleanedText)
                    + '\n';
        }
    }

    /**
     * Data laid out in a table with named columns. The columns are left-justified
     * by default,
     * but justification can be set for columns individually. Plain text output
     * requires a monospace
     * font to line up correctly. For HTML and discord output the background color
     * of an individual row can be set.
     *
     */
    private static class TableElement implements ViewElement {

        static final int JUSTIFIED_LEFT = 0;
        static final int JUSTIFIED_CENTER = 1;
        static final int JUSTIFIED_RIGHT = 2;

        private final int[] justification;
        private final String[] colNames;
        private final List<String[]> data = new ArrayList<>();
        private final Map<Integer, Integer> colWidth = new HashMap<>();
        private final Map<Integer, String> colors = new HashMap<>();

        TableElement(int colCount) {
            justification = new int[colCount];
            colNames = new String[colCount];
            Arrays.fill(colNames, "");
        }

        void setColNames(String... colNames) {
            Arrays.fill(this.colNames, "");
            System.arraycopy(colNames, 0, this.colNames, 0,
                    Math.min(colNames.length, this.colNames.length));
            colWidth.clear();
            for (int i = 0; i < colNames.length; i++) {
                colWidth.put(i, colNames[i].length());
            }
        }

        void setJustification(int... justification) {
            Arrays.fill(this.justification, JUSTIFIED_LEFT);
            System.arraycopy(justification, 0, this.justification, 0,
                    Math.min(justification.length, this.justification.length));
        }

        void addRow(String... row) {
            data.add(row);
            for (int i = 0; i < row.length; i++) {
                colWidth.merge(i, row[i].length(), Math::max);
            }
        }

        void addRowWithColor(String color, String... row) {
            addRow(row);
            colors.put(data.size() - 1, color);
        }

        private String leftPad(String s, int fieldSize) {
            if (fieldSize > 0) {
                return String.format("%" + fieldSize + "s", s);
            } else {
                return "";
            }
        }

        private String rightPad(String s, int fieldSize) {
            if (fieldSize > 0) {
                return String.format("%-" + fieldSize + "s", s);
            } else {
                return "";
            }
        }

        private String center(String s, int fieldSize) {
            int rightPadding = Math.max(fieldSize - s.length(), 0) / 2;
            return rightPad(leftPad(s, fieldSize - rightPadding), fieldSize);
        }

        private String justify(int justification, String s, int fieldSize) {
            if (justification == JUSTIFIED_CENTER) {
                return center(s, fieldSize);
            } else if (justification == JUSTIFIED_LEFT) {
                return rightPad(s, fieldSize);
            } else {
                return leftPad(s, fieldSize);
            }
        }

        @Override
        public String toPlainText() {
            final String COL_PADDING = "  ";
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < colNames.length; col++) {
                sb.append(justify(justification[col], colNames[col], colWidth.get(col)));
                if (col < colNames.length - 1) {
                    sb.append(COL_PADDING);
                }
            }
            sb.append("\n");
            if (colNames.length > 0) {
                int w = sb.length() - 1;
                sb.append("-".repeat(Math.max(0, w)));
                sb.append("\n");
            }
            for (String[] row : data) {
                for (int col = 0; col < row.length; col++) {
                    sb.append(justify(justification[col], row[col], colWidth.get(col)));
                    if (col < row.length - 1) {
                        sb.append(COL_PADDING);
                    }
                }
                sb.append("\n");
            }
            return sb.toString();
        }

        @Override
        public String toHTML() {
            StringBuilder sb = new StringBuilder("<table cellspacing=\"0\" cellpadding=\"2\" border=\"0\">");
            if (colNames.length > 0) {
                sb.append("<tr>");
                for (int col = 0; col < colNames.length; col++) {
                    if (justification[col] == JUSTIFIED_RIGHT) {
                        sb.append("<th align=\"right\">");
                    } else if (justification[col] == JUSTIFIED_CENTER) {
                        sb.append("<th align=\"center\">");
                    } else {
                        sb.append("<th align=\"left\">");
                    }
                    if (justification[col] != JUSTIFIED_LEFT) {
                        sb.append("&nbsp;&nbsp;");
                    }
                    sb.append(colNames[col]);
                    if (justification[col] != JUSTIFIED_RIGHT) {
                        sb.append("&nbsp;&nbsp;");
                    }
                    sb.append("</th>");
                }
                sb.append("</tr>\n");
            }
            for (int r = 0; r < data.size(); r++) {
                if (colors.containsKey(r)) {
                    sb.append("<tr color=\"").append(colors.get(r)).append("\">");
                } else {
                    sb.append("<tr>");
                }
                final String[] row = data.get(r);
                for (int col = 0; col < row.length; col++) {
                    if (justification[col] == JUSTIFIED_RIGHT) {
                        sb.append("<td align=\"right\">");
                    } else if (justification[col] == JUSTIFIED_CENTER) {
                        sb.append("<td align=\"center\">");
                    } else {
                        sb.append("<td align=\"left\">");
                    }
                    if (justification[col] != JUSTIFIED_LEFT) {
                        sb.append("&nbsp;&nbsp;");
                    }
                    sb.append(row[col]);
                    if (justification[col] != JUSTIFIED_RIGHT) {
                        sb.append("&nbsp;&nbsp;");
                    }
                    sb.append("</td>");
                }
                sb.append("</tr>\n");
            }
            sb.append("</table>\n");
            return sb.toString();
        }

        @Override
        public String toDiscord() {
            final String COL_PADDING = "  ";
            StringBuilder sb = new StringBuilder();
            sb.append(DiscordFormat.UNDERLINE).append(DiscordFormat.ROW_SHADING);
            for (int col = 0; col < colNames.length; col++) {
                sb.append(justify(justification[col], colNames[col], colWidth.get(col)));
                if (col < colNames.length - 1) {
                    sb.append(COL_PADDING);
                }
            }
            sb.append(DiscordFormat.RESET);
            sb.append("\n");
            for (int r = 0; r < data.size(); r++) {
                final String[] row = data.get(r);
                if (r % 2 == 1) {
                    sb.append(DiscordFormat.ROW_SHADING);
                }
                for (int col = 0; col < row.length; col++) {
                    sb.append(highlightNumbersForDiscord(justify(justification[col], row[col], colWidth.get(col))));
                    if (col < row.length - 1) {
                        sb.append(COL_PADDING);
                    }
                }
                sb.append(DiscordFormat.RESET).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Displays a label (bold for html and discord output) followed by a column of
     * items
     *
     */
    private static class ItemList implements ViewElement {
        private final String heading;
        private final List<String> data = new ArrayList<>();

        ItemList(String heading) {
            this.heading = heading;
        }

        void addItem(String item) {
            data.add(item);
        }

        @Override
        public String toPlainText() {
            StringBuilder sb = new StringBuilder();
            if (null != heading) {
                sb.append(heading).append("\n");
                sb.append("-".repeat(heading.length()));
                sb.append("\n");
            }
            for (String item : data) {
                sb.append(item).append("\n");
            }
            return sb.toString();
        }

        @Override
        public String toHTML() {
            StringBuilder sb = new StringBuilder();
            if (null != heading) {
                sb.append("<b>").append(heading).append("</b><br/>\n");
            }
            for (String item : data) {
                sb.append(item).append("<br/>\n");
            }
            return sb.toString();
        }

        @Override
        public String toDiscord() {
            StringBuilder sb = new StringBuilder();
            if (null != heading) {
                sb.append(DiscordFormat.BOLD).append(heading).append(DiscordFormat.RESET).append('\n');
            }
            boolean evenLine = false;
            for (String item : data) {
                if (evenLine) {
                    sb.append(DiscordFormat.ROW_SHADING);
                }
                sb.append(highlightNumbersForDiscord(item)).append("\n");
                if (evenLine) {
                    sb.append(DiscordFormat.RESET);
                }
                evenLine = !evenLine;
            }
            return sb.toString();
        }
    }

    /**
     * Displays a single line of text. The default constructor is used to insert a
     * new line.
     */
    private static class SingleLine implements ViewElement {

        private final String value;

        SingleLine(String value) {
            this.value = value;
        }

        SingleLine() {
            this("");
        }

        @Override
        public String toPlainText() {
            return value + "\n";
        }

        @Override
        public String toHTML() {
            return value + "<br/>\n";
        }

        @Override
        public String toDiscord() {
            return toPlainText();
        }
    }

    /**
     * Displays a hyperlink. Does not add a line break after itself.
     */
    private static class HyperLinkElement implements ViewElement {

        private final String label;
        private final String address;
        private final String displayText;

        HyperLinkElement(String address, String displayText) {
            label = "";
            this.address = address;
            this.displayText = displayText;
        }

        HyperLinkElement(String label, String address, String displayText) {
            this.label = (label == null) ? "" : label;
            this.address = address;
            this.displayText = displayText;
        }

        @Override
        public String toPlainText() {
            String result = label.isBlank() ? "" : label + ": ";
            return result + displayText + "\n";
        }

        @Override
        public String toHTML() {
            String result = label.isBlank() ? "" : "<B>" + label + "</B>: ";
            return result + "<A HREF=" + address + ">" + displayText + "</A><BR>";
        }

        @Override
        public String toDiscord() {
            String result = label.isBlank() ? "" : DiscordFormat.BOLD + label + ": " + DiscordFormat.RESET;
            return result + displayText + "\n";
        }
    }

    /**
     * Displays a single line in bold in a larger font in html. In plain text simply
     * displays a single line.
     */
    private static class Title implements ViewElement {

        private final String title;

        Title(String title) {
            this.title = title;
        }

        @Override
        public String toPlainText() {
            return title + "\n";
        }

        @Override
        public String toHTML() {
            return "<font size=\"+1\"><b>" + title + "</b></font><br/>\n";
        }

        @Override
        public String toDiscord() {
            return DiscordFormat.BOLD.toString() + DiscordFormat.UNDERLINE + DiscordFormat.CYAN + title
                    + DiscordFormat.RESET + '\n';
        }
    }

    /**
     * Marks warning text; in html the text is displayed in red. In plain text it is
     * preceded and followed
     * by an asterisk.
     *
     * @return A String that is used to mark the beginning of a warning.
     */
    private String warningStart() {
        return switch (formatting) {
            case HTML -> "<font color=\"red\">";
            case NONE -> "*";
            case DISCORD -> DiscordFormat.RED.toString();
        };
    }

    /**
     * Returns the end element of the warning text.
     *
     * @return A String that is used to mark the end of a warning.
     */
    private String warningEnd() {
        return switch (formatting) {
            case HTML -> "</font>";
            case NONE -> "*";
            case DISCORD -> DiscordFormat.RESET.toString();
        };
    }

    /**
     * Marks the beginning of a section of italicized text if using html output. For
     * plain text
     * returns an empty String.
     *
     * @return The starting element for italicized text.
     */
    private String italicsStart() {
        return switch (formatting) {
            case HTML -> "<i>";
            case NONE -> "";
            case DISCORD -> DiscordFormat.UNDERLINE.toString();
        };
    }

    /**
     * Marks the end of a section of italicized text.
     *
     * @return The ending element for italicized text.
     */
    private String italicsEnd() {
        return switch (formatting) {
            case HTML -> "</i>";
            case NONE -> "";
            case DISCORD -> DiscordFormat.RESET.toString();
        };
    }
}
