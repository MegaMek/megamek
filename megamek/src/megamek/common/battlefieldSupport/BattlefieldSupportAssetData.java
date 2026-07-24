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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitRole;

/**
 * A serialization data-transfer object for a Battlefield Support Asset, mirroring the {@code .bfs} YAML schema. This
 * type exists only at the save/load boundary: a {@link BattlefieldSupportAsset} entity is built from an instance of
 * this class (and can produce one via {@link BattlefieldSupportAsset#toAssetData()}), but the rest of the program works
 * with the {@link BattlefieldSupportAsset} entity, not with this stat block.
 * <p>
 * Because Assets have no construction rules, no validation is performed here. Fields are plain values; the entity is
 * responsible for any derived/display forms and Battle Value.
 */
public class BattlefieldSupportAssetData implements Serializable {

    /** This asset's own unit-file UUID (the {@code uuid} field, mirrored to {@link megamek.common.units.Entity}). */
    private String uuid;
    /** The unit-file UUID of the base unit this asset is linked to, or {@code null}/blank for a standalone asset. */
    private String linkedUnitId;

    private String chassis = "";
    private String model = "";
    private BFSAssetType assetType = BFSAssetType.VEHICLE;

    private String cardTitle;
    private String cardSubtitle;

    /** Design (introduction) year. Assets are always tech level Standard, so no tech level is stored. */
    private int year = 3151;
    /** Tech base: {@code IS}, {@code Clan}, {@code Mixed (IS Chassis)} or {@code Mixed (Clan Chassis)}. */
    private String techBase = TECH_BASE_IS;
    /** Optional sourcebook, as for other units. */
    private String source = "";

    private int mp;
    private EntityMovementMode movementMode = EntityMovementMode.TRACKED;
    private int tmm;

    private BFSRange range = BFSRange.KEYWORD;

    private int skill = 6;
    private Integer veteranSkill;

    private BFSDamage damage = BFSDamage.NONE;
    private int destroyCheck = 7;
    private int threshold = 5;

    private int cost;
    private Integer veteranCost;

    private List<BFSSpecial> specials = new ArrayList<>();

    private UnitRole role = UnitRole.UNDETERMINED;
    /** Optional embedded fluff art as a Base64-encoded image string (as in {@code .blk}/{@code .mtf} files). */
    private String fluffImageEncoded;
    /** Optional embedded top-down sprite/icon art as a Base64-encoded image string. */
    private String iconEncoded;

    /** Tech base value: an Inner Sphere asset. */
    public static final String TECH_BASE_IS = "IS";
    /** Tech base value: a Clan asset. */
    public static final String TECH_BASE_CLAN = "Clan";
    /** Tech base value: an IS-chassis mixed-tech asset. */
    public static final String TECH_BASE_MIXED_IS = "Mixed (IS Chassis)";
    /** Tech base value: a Clan-chassis mixed-tech asset. */
    public static final String TECH_BASE_MIXED_CLAN = "Mixed (Clan Chassis)";

    public String getChassis() {
        return chassis;
    }

    /** @return this asset's own unit-file UUID, or {@code null} if none has been assigned/loaded */
    public @Nullable String getUuid() {
        return uuid;
    }

    public void setUuid(@Nullable String uuid) {
        this.uuid = uuid;
    }

    /** @return the base unit's UUID this asset links to, or {@code null}/blank for a standalone asset */
    public @Nullable String getLinkedUnitId() {
        return linkedUnitId;
    }

    public void setLinkedUnitId(@Nullable String linkedUnitId) {
        this.linkedUnitId = linkedUnitId;
    }

    public void setChassis(String chassis) {
        this.chassis = (chassis == null) ? "" : chassis;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = (model == null) ? "" : model;
    }

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

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    /**
     * @return the tech base: {@link #TECH_BASE_IS}, {@link #TECH_BASE_CLAN}, {@link #TECH_BASE_MIXED_IS} or
     *       {@link #TECH_BASE_MIXED_CLAN}
     */
    public String getTechBase() {
        return techBase;
    }

    public void setTechBase(String techBase) {
        this.techBase = (techBase == null || techBase.isBlank()) ? TECH_BASE_IS : techBase;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = (source == null) ? "" : source;
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        this.mp = mp;
    }

    public EntityMovementMode getMovementMode() {
        return movementMode;
    }

    public void setMovementMode(EntityMovementMode movementMode) {
        this.movementMode = movementMode;
    }

    public int getTmm() {
        return tmm;
    }

    public void setTmm(int tmm) {
        this.tmm = tmm;
    }

    public BFSRange getRange() {
        return range;
    }

    public void setRange(BFSRange range) {
        this.range = range;
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

    public BFSDamage getDamage() {
        return damage;
    }

    public void setDamage(BFSDamage damage) {
        this.damage = damage;
    }

    public int getDestroyCheck() {
        return destroyCheck;
    }

    public void setDestroyCheck(int destroyCheck) {
        this.destroyCheck = destroyCheck;
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

    public List<BFSSpecial> getSpecials() {
        return specials;
    }

    public void setSpecials(List<BFSSpecial> specials) {
        this.specials = (specials == null) ? new ArrayList<>() : specials;
    }

    public UnitRole getRole() {
        return role;
    }

    public void setRole(UnitRole role) {
        this.role = (role == null) ? UnitRole.UNDETERMINED : role;
    }

    /** @return the embedded fluff art as a Base64-encoded image string, or {@code null} if none is embedded */
    public @Nullable String getFluffImageEncoded() {
        return fluffImageEncoded;
    }

    public void setFluffImageEncoded(@Nullable String fluffImageEncoded) {
        this.fluffImageEncoded = fluffImageEncoded;
    }

    /** @return the embedded top-down sprite/icon art as a Base64-encoded image string, or {@code null} if none */
    public @Nullable String getIconEncoded() {
        return iconEncoded;
    }

    public void setIconEncoded(@Nullable String iconEncoded) {
        this.iconEncoded = iconEncoded;
    }

    @Override
    public String toString() {
        return (chassis + " " + model).strip() + " [Battlefield Support Asset data]";
    }
}
