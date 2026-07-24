/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.battlefieldSupport;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.HitData;
import megamek.common.MPCalculationSetting;
import megamek.common.Report;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.AimingMode;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.SkillLevel;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.interfaces.ITechnology;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.UnitRole;
import megamek.common.units.UnitType;

/**
 * The {@link Entity} realization of a Battlefield Support Asset - a simplified unit governed by the Battlefield Support
 * rules rather than by full construction/combat rules. This is the runtime object the rest of the program works with;
 * the whole codebase deals with this entity (and its {@code MekSummary}), not with the {@link BattlefieldSupportAssetData}
 * stat block, which exists only at the {@code .bfs} save/load boundary.
 * <p>
 * An asset is built from a {@link BattlefieldSupportAssetData} (see {@link #BattlefieldSupportAsset(BattlefieldSupportAssetData)}
 * and {@link #setFromData}) and can produce one for saving via {@link #toAssetData()}. A parameterless constructor is
 * provided for tests and for creating a brand-new asset in MegaMekLab. Once built, all stat values live on this entity
 * (chassis, model and role reuse the base {@link Entity} fields); MegaMekLab edits them here directly.
 * <p>
 * A single carrier class represents every asset category ({@link BFSAssetType}); behavior that differs by category or
 * movement mode is derived from the stored values. Assets have no hit locations - the whole unit is a single location.
 */
public class BattlefieldSupportAsset extends Entity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Battle Value per Battlefield Support Point. */
    public static final int BV_PER_BSP = 20;

    /**
     * The Regular/Veteran choice for an asset instance is carried by the crew's Gunnery skill (so it persists through
     * the normal Crew/MUL machinery): a gunnery at or below {@link #GUNNERY_VETERAN} means Veteran, otherwise Regular.
     * These representative values are used when the UI toggles the level.
     */
    public static final int GUNNERY_REGULAR = 4;
    public static final int GUNNERY_VETERAN = 3;

    /**
     * A current Destroy Check strictly below this value means the asset has been destroyed by persistent damage (and is
     * therefore shown as {@link #DMG_CRIPPLED}). A destroyed asset is generally recorded with a Destroy Check of 0.
     */
    public static final int DESTROYED_DESTROY_CHECK = 2;

    /** The em dash shown on the card for n/a values. */
    private static final String NA_DISPLAY = "\u2014";

    /** The single, whole-unit location. Assets do not use hit locations. */
    public static final int LOC_ASSET = 0;
    private static final String[] LOCATION_NAMES = new String[] { "Asset" };
    private static final String[] LOCATION_ABBREVIATIONS = new String[] { "AST" };
    private static final int[] NUM_OF_SLOTS = new int[] { 0 };

    private static final TechAdvancement ADVANCEMENT = new TechAdvancement(TechBase.ALL)
          .setAdvancement(ITechnology.DATE_NONE, ITechnology.DATE_NONE, ITechnology.DATE_PS)
          .setStaticTechLevel(SimpleTechLevel.STANDARD)
          .setTechRating(TechRating.A)
          .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A);

    // Battlefield Support stat values. Chassis, model, role, movement mode and MP reuse the base Entity fields.
    private BFSAssetType assetType = BFSAssetType.VEHICLE;
    private String cardTitle;
    private String cardSubtitle;
    private int tmm;
    private BFSRange range = BFSRange.KEYWORD;
    private int skill = 6;
    private Integer veteranSkill;
    private BFSDamage damage = BFSDamage.NONE;
    /** The as-constructed Destroy Check target (the designer's value, stored in the {@code .bfs}). */
    private int oDestroyCheck = 7;
    /**
     * The current Destroy Check target. Equal to {@link #oDestroyCheck} for an undamaged asset; persistent damage
     * lowers it during play (tracked in {@code .mul} force files, not the {@code .bfs} definition), mirroring how a
     * unit's current {@code armor} is lowered from its as-constructed {@code oArmor}.
     */
    private int destroyCheck = 7;
    private int threshold = 5;
    private int cost;
    private Integer veteranCost;
    private List<BFSSpecial> specials = new ArrayList<>();

    /** The unit-file UUID of the base unit this asset is linked to, or {@code null}/blank when standalone. */
    private String linkedUnitId;

    /** Creates an empty asset with default values (for tests and for a brand-new asset in MegaMekLab). */
    public BattlefieldSupportAsset() {
        super();
        // Default identity to empty strings (not null) so name-based lookups (e.g. fluff art) don't NPE on a
        // brand-new, not-yet-named asset.
        setChassis("");
        setModel("");
        setMovementMode(EntityMovementMode.TRACKED);
        // Assets are always tech level Standard; default to Inner Sphere in the current era.
        setYear(3151);
        setTechLevel(TechConstants.T_IS_TW_NON_BOX);
    }

    /**
     * Creates an asset from the given stat block.
     *
     * @param data the stat block to build this asset from
     */
    public BattlefieldSupportAsset(BattlefieldSupportAssetData data) {
        super();
        setFromData(data);
    }

    // region Build from / save to the serialization DTO

    /**
     * Copies every value from the given stat block onto this entity. Identity, role, movement mode and MP use the base
     * {@link Entity} fields. Used when loading a {@code .bfs} file. Movement mode is applied before MP so that the MP is
     * stored against the correct (walk vs jump) allowance.
     *
     * @param data the stat block to copy from
     */
    public void setFromData(BattlefieldSupportAssetData data) {
        if ((data.getUuid() != null) && !data.getUuid().isBlank()) {
            setUnitFileUUID(data.getUuid().trim());
        }
        linkedUnitId = blankToNull(data.getLinkedUnitId());
        setChassis(data.getChassis());
        setModel(data.getModel());
        setUnitRole(data.getRole());
        setYear(data.getYear());
        setSource(data.getSource());
        setAssetTechBase(data.getTechBase());
        assetType = data.getAssetType();
        cardTitle = data.getCardTitle();
        cardSubtitle = data.getCardSubtitle();
        setMovementMode(data.getMovementMode());
        setMp(data.getMp());
        tmm = data.getTmm();
        range = data.getRange();
        skill = data.getSkill();
        veteranSkill = data.getVeteranSkill();
        damage = data.getDamage();
        // The .bfs stores the as-constructed value; a freshly loaded asset is undamaged (current == original).
        oDestroyCheck = data.getDestroyCheck();
        destroyCheck = oDestroyCheck;
        threshold = data.getThreshold();
        cost = data.getCost();
        veteranCost = data.getVeteranCost();
        specials = new ArrayList<>(data.getSpecials());
        if (data.getFluffImageEncoded() != null) {
            getFluff().setFluffImage(data.getFluffImageEncoded());
        }
        if (data.getIconEncoded() != null) {
            setIcon(data.getIconEncoded());
        }
    }

    /**
     * @return a new stat block snapshot of this asset, suitable for writing to a {@code .bfs} file
     */
    public BattlefieldSupportAssetData toAssetData() {
        BattlefieldSupportAssetData data = new BattlefieldSupportAssetData();
        data.setUuid(getUnitFileUUID());
        data.setLinkedUnitId(linkedUnitId);
        data.setChassis(getChassis());
        data.setModel(getModel());
        data.setRole(getRole());
        data.setYear(getYear());
        data.setSource(getSource());
        data.setTechBase(getAssetTechBase());
        data.setAssetType(assetType);
        data.setCardTitle(cardTitle);
        data.setCardSubtitle(cardSubtitle);
        data.setMp(getMp());
        data.setMovementMode(getMovementMode());
        data.setTmm(tmm);
        data.setRange(range);
        data.setSkill(skill);
        data.setVeteranSkill(veteranSkill);
        data.setDamage(damage);
        // The .bfs is the unit definition: write the as-constructed value, not the damage-lowered current one.
        data.setDestroyCheck(oDestroyCheck);
        data.setThreshold(threshold);
        data.setCost(cost);
        data.setVeteranCost(veteranCost);
        data.setSpecials(new ArrayList<>(specials));
        if (getFluff().hasEmbeddedFluffImage()) {
            data.setFluffImageEncoded(getFluff().getBase64FluffImage().getBase64String());
        }
        if (hasEmbeddedIcon()) {
            data.setIconEncoded(getBase64Icon().getBase64String());
        }
        return data;
    }

    /**
     * @return the unit-file UUID of the base unit this asset is linked to, or {@code null} when the asset is standalone
     *       (not linked to a base unit)
     */
    public @Nullable String getLinkedUnitId() {
        return linkedUnitId;
    }

    /**
     * Sets the base unit this asset is linked to, by the base unit's unit-file UUID.
     *
     * @param linkedUnitId the base unit's UUID, or {@code null}/blank to make this a standalone asset
     */
    public void setLinkedUnitId(@Nullable String linkedUnitId) {
        this.linkedUnitId = blankToNull(linkedUnitId);
    }

    /** @return whether this asset is linked to a base unit (has a non-blank {@link #getLinkedUnitId()}) */
    public boolean isLinkedToBaseUnit() {
        return linkedUnitId != null;
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return ((value == null) || value.isBlank()) ? null : value.trim();
    }

    /**
     * Applies a {@code .bfs} tech-base value (IS/Clan/Mixed) to this asset. Assets are always tech level Standard, so
     * this sets only the base (via the Standard tech-level constant) and the mixed-tech flag.
     *
     * @param techBase one of {@link BattlefieldSupportAssetData#TECH_BASE_IS}/{@code _CLAN}/{@code _MIXED}
     */
    public void setAssetTechBase(String techBase) {
        switch (techBase == null ? BattlefieldSupportAssetData.TECH_BASE_IS : techBase) {
            case BattlefieldSupportAssetData.TECH_BASE_CLAN -> {
                setTechLevel(TechConstants.T_CLAN_TW);
                setMixedTech(false);
            }
            case BattlefieldSupportAssetData.TECH_BASE_MIXED_CLAN -> {
                setTechLevel(TechConstants.T_CLAN_TW);
                setMixedTech(true);
            }
            case BattlefieldSupportAssetData.TECH_BASE_MIXED_IS -> {
                setTechLevel(TechConstants.T_IS_TW_NON_BOX);
                setMixedTech(true);
            }
            default -> {
                setTechLevel(TechConstants.T_IS_TW_NON_BOX);
                setMixedTech(false);
            }
        }
    }

    /** @return this asset's tech base as a {@code .bfs} value (IS/Clan/Mixed (IS Chassis)/Mixed (Clan Chassis)). */
    public String getAssetTechBase() {
        if (isMixedTech()) {
            return isClan() ? BattlefieldSupportAssetData.TECH_BASE_MIXED_CLAN
                  : BattlefieldSupportAssetData.TECH_BASE_MIXED_IS;
        }
        return isClan() ? BattlefieldSupportAssetData.TECH_BASE_CLAN : BattlefieldSupportAssetData.TECH_BASE_IS;
    }

    // endregion

    // region Entity identity

    @Override
    public boolean isBattlefieldSupportAsset() {
        return true;
    }

    @Override
    public long getEntityType() {
        return ETYPE_BATTLEFIELD_SUPPORT_ASSET;
    }

    @Override
    public int getUnitType() {
        return UnitType.BATTLEFIELD_SUPPORT_ASSET;
    }

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return ADVANCEMENT;
    }

    // endregion

    // region Battlefield Support stats

    public BFSAssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(BFSAssetType assetType) {
        this.assetType = assetType;
    }

    public @Nullable String getCardTitle() {
        return cardTitle;
    }

    public void setCardTitle(@Nullable String cardTitle) {
        this.cardTitle = cardTitle;
    }

    public @Nullable String getCardSubtitle() {
        return cardSubtitle;
    }

    public void setCardSubtitle(@Nullable String cardSubtitle) {
        this.cardSubtitle = cardSubtitle;
    }

    /** @return the title to print on the card: {@link #getCardTitle()} if set, otherwise the chassis */
    public String getEffectiveCardTitle() {
        return (cardTitle != null && !cardTitle.isBlank()) ? cardTitle : getChassis();
    }

    /** @return the subtitle to print on the card: {@link #getCardSubtitle()} if set, otherwise the model */
    public String getEffectiveCardSubtitle() {
        return (cardSubtitle != null) ? cardSubtitle : getModel();
    }

    /**
     * @return the asset's single Movement Point allowance for its declared movement mode - the jump MP for jump modes,
     *       otherwise the walk MP
     */
    public int getMp() {
        return isJumpMode() ? getJumpMP() : getWalkMP();
    }

    /**
     * Sets the asset's single Movement Point allowance against its declared movement mode - the jump MP for jump modes,
     * otherwise the walk MP. Set the movement mode before calling this so the value is stored against the right
     * allowance.
     *
     * @param mp the movement allowance
     */
    public void setMp(int mp) {
        if (isJumpMode()) {
            setOriginalJumpMP(mp);
        } else {
            setOriginalWalkMP(mp);
        }
    }

    /** @return true if this asset's movement mode is a jumping mode (its MP is a jump allowance) */
    public boolean isJumpMode() {
        return getMovementMode() == EntityMovementMode.INF_JUMP;
    }

    /** @return the card display form of movement, for example {@code 8H} */
    public String getMovementDisplay() {
        return getMp() + movementCardLetter(getMovementMode());
    }

    /**
     * @param mode a movement mode
     *
     * @return the single-letter code shown on the Asset card for the given mode (for example {@code H} for hover)
     */
    public static String movementCardLetter(@Nullable EntityMovementMode mode) {
        if (mode == null) {
            return "";
        }
        return switch (mode) {
            case NONE -> "";
            case HOVER -> "H";
            case WHEELED -> "W";
            case TRACKED -> "T";
            case VTOL -> "V";
            case INF_LEG -> "F";
            case INF_JUMP -> "J";
            case INF_MOTORIZED -> "M";
            case WIGE -> "G";
            default -> mode.toString().isEmpty() ? "?" : mode.toString().substring(0, 1).toUpperCase();
        };
    }

    public int getTmm() {
        return tmm;
    }

    public void setTmm(int tmm) {
        this.tmm = tmm;
    }

    /**
     * @return the card display form of the TMM, always signed (for example {@code +3}). Immobile assets append a
     *       {@code *} (for example {@code +0*}) to indicate that, although the printed TMM is as shown, the -4 Immobile
     *       to-hit modifier applies so the effective TMM is -4.
     */
    public String getTmmDisplay() {
        String base = (tmm >= 0 ? "+" : "") + tmm;
        return isImmobileAsset() ? base + "*" : base;
    }

    /** @return true if this asset has the Immobile Special (its TMM is shown with a {@code *} and it cannot move). */
    public boolean isImmobileAsset() {
        return hasSpecial(BFSSpecialType.IMMOBILE);
    }

    public BFSRange getRange() {
        return range;
    }

    public void setRange(BFSRange range) {
        this.range = range;
    }

    /** @return the card display form of the range, using a keyword label derived from Specials where applicable */
    public String getRangeDisplay() {
        return range.displayString(getRangeKeywordLabel());
    }

    /**
     * @return the keyword label to show for a keyword range - the artillery type (for example {@code Long Tom}) when
     *       an Artillery Special is present, else {@code Arrow IV} for an Arrow Special, else an em dash - or
     *       {@code null} for a numeric range
     */
    public @Nullable String getRangeKeywordLabel() {
        if (!range.isKeyword()) {
            return null;
        }
        if (hasSpecial(BFSSpecialType.ARTILLERY)) {
            BFSArtilleryType artilleryType = getArtilleryType();
            return (artilleryType != null) ? artilleryType.displayName() : BFSSpecialType.ARTILLERY.displayName();
        }
        if (hasSpecial(BFSSpecialType.ARROW_IV)) {
            return BFSSpecialType.ARROW_IV.displayName();
        }
        return NA_DISPLAY;
    }

    public int getSkill() {
        return skill;
    }

    public void setSkill(int skill) {
        this.skill = skill;
    }

    public @Nullable Integer getVeteranSkill() {
        return veteranSkill;
    }

    public void setVeteranSkill(@Nullable Integer veteranSkill) {
        this.veteranSkill = veteranSkill;
    }

    /** @return the card display form of the skill, for example {@code 6(5)} */
    public String getSkillDisplay() {
        return valuePairDisplay(skill, veteranSkill);
    }

    public BFSDamage getDamage() {
        return damage;
    }

    public void setDamage(BFSDamage damage) {
        this.damage = damage;
    }

    /** @return the card display form of the damage, for example {@code 5x4} */
    public String getDamageDisplay() {
        return damage.displayString();
    }

    /**
     * @return the current Destroy Check target (lowered by persistent damage; equal to {@link #getODestroyCheck()} for
     *       an undamaged asset). This is the value shown on the card and used in play.
     */
    public int getDestroyCheck() {
        return destroyCheck;
    }

    /**
     * Sets the current Destroy Check target only (persistent damage), leaving the as-constructed value unchanged —
     * mirroring {@link megamek.common.units.Entity#setArmor}. Use {@link #setODestroyCheck(int)} to set the
     * as-constructed value.
     *
     * @param destroyCheck the current Destroy Check target
     */
    public void setDestroyCheck(int destroyCheck) {
        this.destroyCheck = destroyCheck;
    }

    /** @return the as-constructed Destroy Check target (the designer's value, stored in the {@code .bfs}). */
    public int getODestroyCheck() {
        return oDestroyCheck;
    }

    /**
     * Sets the as-constructed Destroy Check target and resets the current value to it (an undamaged unit), mirroring
     * {@link megamek.common.units.Entity#initializeArmor}. Used when defining/editing the asset. Persistent damage is
     * applied afterward via {@link #setDestroyCheck(int)}.
     *
     * @param oDestroyCheck the as-constructed Destroy Check target
     */
    public void setODestroyCheck(int oDestroyCheck) {
        this.oDestroyCheck = oDestroyCheck;
        this.destroyCheck = oDestroyCheck;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public @Nullable Integer getVeteranCost() {
        return veteranCost;
    }

    public void setVeteranCost(@Nullable Integer veteranCost) {
        this.veteranCost = veteranCost;
    }

    /** @return the card display form of the cost in BSP, for example {@code 23(27)} */
    public String getCostDisplay() {
        return valuePairDisplay(cost, veteranCost);
    }

    /** @return the standard cost in Battlefield Support Points */
    public int getBsp() {
        return cost;
    }

    /** @return the BSP cost for the current crew level: the Veteran BSP when Veteran is selected, else the Regular BSP. */
    public int getEffectiveBsp() {
        return isVeteranCrew() ? veteranCost : cost;
    }

    /** @return the Veteran cost in Battlefield Support Points, or {@code null} if there is no Veteran variant */
    public @Nullable Integer getVeteranBsp() {
        return veteranCost;
    }

    /** @return the standard cost in Battle Value ({@link #getBsp()} times {@value #BV_PER_BSP}) */
    public int getBv() {
        return cost * BV_PER_BSP;
    }

    /** @return the Veteran cost in Battle Value, or {@code null} if there is no Veteran variant */
    public @Nullable Integer getVeteranBv() {
        return (veteranCost == null) ? null : veteranCost * BV_PER_BSP;
    }

    /** @return true if this asset defines a Veteran variant (a Veteran cost/skill), so Veteran can be selected. */
    public boolean hasVeteranProfile() {
        return veteranCost != null;
    }

    /**
     * @param gunnery a crew Gunnery skill
     *
     * @return true if the given Gunnery skill encodes a Veteran asset crew (see {@link #GUNNERY_VETERAN})
     */
    public static boolean isVeteranGunnery(int gunnery) {
        return gunnery <= GUNNERY_VETERAN;
    }

    /**
     * @return true if this asset's current crew is set to the Veteran level and it has a Veteran variant. When there is
     *       no Veteran variant the asset is always treated as Regular regardless of the crew skill.
     */
    public boolean isVeteranCrew() {
        return hasVeteranProfile() && (getCrew() != null) && isVeteranGunnery(getCrew().getGunnery());
    }

    /**
     * @return the crew grade as a {@link SkillLevel}: {@link SkillLevel#VETERAN} when the crew is Veteran, otherwise
     *       {@link SkillLevel#REGULAR}. Assets use only these two grades (there are no gunnery/piloting numbers in play);
     *       this is the label form used wherever a crew's skill would normally be shown.
     */
    public SkillLevel getCrewSkillLevel() {
        return isVeteranCrew() ? SkillLevel.VETERAN : SkillLevel.REGULAR;
    }

    /**
     * Sets the crew's Regular/Veteran level, carried by the Gunnery skill. Requires a crew to be present.
     *
     * @param veteran true for Veteran, false for Regular
     */
    public void setVeteranCrew(boolean veteran) {
        if (getCrew() != null) {
            int gunnery = veteran ? GUNNERY_VETERAN : GUNNERY_REGULAR;
            getCrew().setGunnery(gunnery, getCrew().getCrewType().getGunnerPos());
        }
    }

    /** @return the Battle Value for the current crew level: the Veteran BV when Veteran is selected, else the Regular BV. */
    public int getEffectiveBv() {
        return isVeteranCrew() ? getVeteranBv() : getBv();
    }

    @Override
    protected int doBattleValueCalculation(boolean ignoreC3, boolean ignoreSkill, CalculationReport calculationReport) {
        // Asset BV is a designer-set cost (BSP x 20), not an equipment/skill computation. The Regular/Veteran crew
        // level selects between the two static costs; ignoreSkill forces the Regular (base) value.
        return (!ignoreSkill && isVeteranCrew()) ? getVeteranBv() : getBv();
    }

    public List<BFSSpecial> getSpecials() {
        return specials;
    }

    public void setSpecials(List<BFSSpecial> specials) {
        this.specials = (specials == null) ? new ArrayList<>() : specials;
    }

    public void addSpecial(BFSSpecial special) {
        specials.add(special);
    }

    /**
     * @param type the known Special type to look for
     *
     * @return the first Special on this asset whose code matches the given registry entry, or empty if none
     */
    public Optional<BFSSpecial> findSpecial(BFSSpecialType type) {
        return specials.stream().filter(special -> special.knownType().orElse(null) == type).findFirst();
    }

    /**
     * @param type the known Special type to look for
     *
     * @return true if this asset carries a Special whose code matches the given registry entry
     */
    public boolean hasSpecial(BFSSpecialType type) {
        return findSpecial(type).isPresent();
    }

    /**
     * @return the artillery type of this asset's Artillery Special (from its value, for example {@code LT}), or
     *       {@code null} if the asset has no Artillery Special or its type is unrecognized
     */
    public @Nullable BFSArtilleryType getArtilleryType() {
        return findSpecial(BFSSpecialType.ARTILLERY)
              .map(BFSSpecial::value)
              .map(BFSArtilleryType::fromString)
              .orElse(null);
    }

    /** @return the card display form of all Specials, comma-separated, or an em dash when there are none */
    public String getSpecialsDisplay() {
        if (specials.isEmpty()) {
            return NA_DISPLAY;
        }
        return specials.stream().map(BFSSpecial::displayString).collect(Collectors.joining(", "));
    }

    // endregion

    // region Entity contract (whole-unit, no hit locations)

    @Override
    public int locations() {
        return LOCATION_NAMES.length;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    @Override
    public String[] getLocationAbbreviations() {
        return LOCATION_ABBREVIATIONS;
    }

    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    @Override
    public void autoSetInternal() {
        initializeInternal(0, LOC_ASSET);
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode, int cover) {
        return new HitData(LOC_ASSET);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return new HitData(LOC_ASSET);
    }

    @Override
    public HitData getTransferLocation(HitData hit) {
        return new HitData(Entity.LOC_DESTROYED);
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return false;
    }

    @Override
    public boolean isValidSecondaryFacing(int dir) {
        return false;
    }

    @Override
    public int clipSecondaryFacing(int dir) {
        return Compute.ARC_FORWARD;
    }

    @Override
    public int getWeaponArc(int weaponNumber) {
        return Compute.ARC_360;
    }

    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        return false;
    }

    @Override
    public String getMovementString(EntityMovementType movementType) {
        return getMovementMode().toString();
    }

    @Override
    public String getMovementAbbr(EntityMovementType movementType) {
        return movementCardLetter(getMovementMode());
    }

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        // Assets have a single flat MP allowance; they do not gain a running bonus.
        return getWalkMP(mpCalculationSetting);
    }

    @Override
    public int getMaxElevationChange() {
        return 1;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        return roll;
    }

    @Override
    public int getEngineHits() {
        return 0;
    }

    @Override
    public boolean isCrippled() {
        return isDestroyed() || isDoomed() || isDestroyedByDamage();
    }

    @Override
    public boolean isCrippled(boolean checkCrew) {
        return isCrippled();
    }

    /**
     * @return {@code true} if this asset has been destroyed by persistent damage - its current Destroy Check has fallen
     *       below {@link #DESTROYED_DESTROY_CHECK}. An asset can no longer be damaged past destruction; a destroyed
     *       asset is generally recorded with a Destroy Check of 0. This is the only condition under which an asset is
     *       shown as {@link #DMG_CRIPPLED}.
     */
    public boolean isDestroyedByDamage() {
        return getDestroyCheck() < DESTROYED_DESTROY_CHECK;
    }

    /** @return how far the current Destroy Check has been lowered from the as-constructed value (never negative). */
    private int destroyCheckLoss() {
        return Math.max(0, getODestroyCheck() - getDestroyCheck());
    }

    /**
     * @return the Destroy-Check loss at which this asset becomes {@link #DMG_MODERATE}. The band widens for tougher
     *       assets: a loss of 2 for an as-constructed check of 9 or less, otherwise 3.
     */
    private int moderateDamageThreshold() {
        return (getODestroyCheck() <= 9) ? 2 : 3;
    }

    /**
     * @return the Destroy-Check loss at which this asset becomes {@link #DMG_HEAVY}. The band widens for tougher assets:
     *       3 for an as-constructed check of 9 or less, 4 for 10-11, and 5 for 12 or more.
     */
    private int heavyDamageThreshold() {
        int original = getODestroyCheck();
        if (original <= 9) {
            return 3;
        } else if (original <= 11) {
            return 4;
        } else {
            return 5;
        }
    }

    @Override
    public boolean isDmgHeavy() {
        return destroyCheckLoss() >= heavyDamageThreshold();
    }

    @Override
    public boolean isDmgModerate() {
        return destroyCheckLoss() >= moderateDamageThreshold();
    }

    @Override
    public boolean isDmgLight() {
        return destroyCheckLoss() >= 1;
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return 0;
    }

    @Override
    public int getGenericBattleValue() {
        return getBv();
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> report = new Vector<>();
        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        report.addElement(r);
        return report;
    }

    // endregion

    /**
     * Formats a standard value with an optional Veteran value as {@code standard(veteran)}, or just {@code standard}
     * when there is no Veteran value. Used for the Skill and Cost card display forms.
     *
     * @param standard the standard value
     * @param veteran  the Veteran value, or {@code null}
     *
     * @return the display form
     */
    private static String valuePairDisplay(int standard, @Nullable Integer veteran) {
        return (veteran == null) ? Integer.toString(standard) : standard + "(" + veteran + ")";
    }
}
