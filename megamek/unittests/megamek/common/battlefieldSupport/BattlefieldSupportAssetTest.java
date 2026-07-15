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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.client.ui.clientGUI.calculationReport.DummyCalculationReport;
import megamek.common.enums.SkillLevel;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitRole;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BattlefieldSupportAssetTest {

    private static final String EM_DASH = "\u2014";

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    /** Builds a Maxim-like data block matching the example card. */
    private static BattlefieldSupportAssetData maximData() {
        BattlefieldSupportAssetData data = new BattlefieldSupportAssetData();
        data.setChassis("Maxim Heavy Hover Transport");
        data.setModel("");
        data.setAssetType(BFSAssetType.VEHICLE);
        data.setCardTitle("Maxim");
        data.setCardSubtitle("Hover Transport");
        data.setMp(8);
        data.setMovementMode(EntityMovementMode.HOVER);
        data.setTmm(3);
        data.setRange(new BFSRange(3, 6, 9));
        data.setSkill(6);
        data.setVeteranSkill(5);
        data.setDamage(new BFSDamage(5, 4));
        data.setDestroyCheck(7);
        data.setThreshold(5);
        data.setCost(23);
        data.setVeteranCost(27);
        data.setSpecials(List.of(BFSSpecial.of("APC", 1), BFSSpecial.of("IF", 2)));
        data.setRole(UnitRole.SCOUT);
        return data;
    }

    @Test
    void identifiesAsBattlefieldSupportAsset() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        assertTrue(asset.isBattlefieldSupportAsset());
        assertEquals(UnitType.BATTLEFIELD_SUPPORT_ASSET, asset.getUnitType());
        assertEquals(Entity.ETYPE_BATTLEFIELD_SUPPORT_ASSET, asset.getEntityType());
    }

    @Test
    void baseEntityDefaultsToNotAnAsset() {
        assertFalse(new HandheldWeapon().isBattlefieldSupportAsset());
    }

    @Test
    void loadingSetsBothOriginalAndCurrentDestroyCheck() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset(maximData());
        // A freshly loaded asset is undamaged: current == original == the .bfs value.
        assertEquals(7, asset.getODestroyCheck());
        assertEquals(7, asset.getDestroyCheck());
    }

    @Test
    void persistentDamageLowersCurrentDestroyCheckButNotTheOriginal() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset(maximData());
        asset.setDestroyCheck(5);
        assertEquals(5, asset.getDestroyCheck(), "current is lowered by damage");
        assertEquals(7, asset.getODestroyCheck(), "as-constructed value is unchanged");
        // The .bfs definition round-trips the original (as-constructed) value, not the damage-lowered current one.
        assertEquals(7, asset.toAssetData().getDestroyCheck());
    }

    @Test
    void settingTheOriginalDestroyCheckResetsTheCurrent() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset(maximData());
        asset.setDestroyCheck(4);
        asset.setODestroyCheck(9);
        assertEquals(9, asset.getODestroyCheck());
        assertEquals(9, asset.getDestroyCheck(), "editing the definition resets current to the original");
    }

    @Test
    void hasSingleWholeUnitLocation() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        assertEquals(1, asset.locations());
        assertEquals("Asset", asset.getLocationNames()[BattlefieldSupportAsset.LOC_ASSET]);
    }

    @Test
    void builtFromDataCopiesEveryValueOntoTheEntity() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset(maximData());
        assertEquals("Maxim Heavy Hover Transport", asset.getChassis());
        assertEquals("", asset.getModel());
        assertEquals(UnitRole.SCOUT, asset.getRole());
        assertEquals(BFSAssetType.VEHICLE, asset.getAssetType());
        assertEquals(8, asset.getMp());
        assertEquals(EntityMovementMode.HOVER, asset.getMovementMode());
        assertEquals(3, asset.getTmm());
        assertEquals(new BFSRange(3, 6, 9), asset.getRange());
        assertEquals(6, asset.getSkill());
        assertEquals(5, asset.getVeteranSkill());
        assertEquals(new BFSDamage(5, 4), asset.getDamage());
        assertEquals(23, asset.getCost());
        assertEquals(27, asset.getVeteranCost());
        assertEquals(2, asset.getSpecials().size());
    }

    @Test
    void toAssetDataRoundTripsThroughTheEntity() {
        BattlefieldSupportAsset original = new BattlefieldSupportAsset(maximData());
        BattlefieldSupportAsset restored = new BattlefieldSupportAsset(original.toAssetData());

        assertEquals(original.getChassis(), restored.getChassis());
        assertEquals(original.getRole(), restored.getRole());
        assertEquals(original.getAssetType(), restored.getAssetType());
        assertEquals(original.getMp(), restored.getMp());
        assertEquals(original.getMovementMode(), restored.getMovementMode());
        assertEquals(original.getTmm(), restored.getTmm());
        assertEquals(original.getRange(), restored.getRange());
        assertEquals(original.getSkill(), restored.getSkill());
        assertEquals(original.getVeteranSkill(), restored.getVeteranSkill());
        assertEquals(original.getDamage(), restored.getDamage());
        assertEquals(original.getCost(), restored.getCost());
        assertEquals(original.getVeteranCost(), restored.getVeteranCost());
        assertEquals(original.getSpecials(), restored.getSpecials());
    }

    @Test
    void displayFormsMatchCard() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset(maximData());
        assertEquals("8H", asset.getMovementDisplay());
        assertEquals("+3", asset.getTmmDisplay());
        assertEquals("3/6/9", asset.getRangeDisplay());
        assertEquals("6(5)", asset.getSkillDisplay());
        assertEquals("5x4", asset.getDamageDisplay());
        assertEquals("23(27)", asset.getCostDisplay());
        assertEquals("APC1, IF2", asset.getSpecialsDisplay());
        assertEquals("Maxim", asset.getEffectiveCardTitle());
        assertEquals("Hover Transport", asset.getEffectiveCardSubtitle());
    }

    @Test
    void bvIsBspTimesTwenty() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset(maximData());
        assertEquals(23, asset.getBsp());
        assertEquals(23 * 20, asset.getBv());
        assertEquals(23 * 20, asset.getGenericBattleValue());
        assertEquals(27 * 20, asset.getVeteranBv());
    }

    @Test
    void calculateBattleValueReflectsRegularVeteranCrew() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset(maximData());
        asset.setCrew(new Crew(asset.defaultCrewType()));

        // A default crew (gunnery 4) is Regular.
        assertFalse(asset.isVeteranCrew());
        assertEquals(23 * 20, asset.calculateBattleValue());
        assertEquals(23 * 20, asset.getEffectiveBv());
        assertEquals(23, asset.getEffectiveBsp());

        asset.setVeteranCrew(true);
        assertTrue(asset.isVeteranCrew());
        assertEquals(27 * 20, asset.calculateBattleValue());
        assertEquals(27 * 20, asset.getEffectiveBv());
        assertEquals(27, asset.getEffectiveBsp());
        assertEquals(27 * 20,
              asset.getBvCalculator().calculateBV(false, false, new DummyCalculationReport()));
        assertEquals(23 * 20,
              asset.getBvCalculator().calculateBV(false, true, new DummyCalculationReport()));

        asset.setVeteranCrew(false);
        assertFalse(asset.isVeteranCrew());
        assertEquals(23 * 20, asset.calculateBattleValue());
    }

    @Test
    void getCrewSkillLevelReflectsGrade() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset(maximData());
        asset.setCrew(new Crew(asset.defaultCrewType()));

        // A default crew (gunnery 4) is Regular.
        assertEquals(SkillLevel.REGULAR, asset.getCrewSkillLevel());

        asset.setVeteranCrew(true);
        assertEquals(SkillLevel.VETERAN, asset.getCrewSkillLevel());

        asset.setVeteranCrew(false);
        assertEquals(SkillLevel.REGULAR, asset.getCrewSkillLevel());
    }

    @Test
    void getCrewSkillLevelIsRegularWithoutVeteranProfile() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setCost(10);
        asset.setCrew(new Crew(asset.defaultCrewType()));

        // No Veteran variant: always Regular even when Veteran gunnery is encoded.
        assertFalse(asset.hasVeteranProfile());
        asset.setVeteranCrew(true);
        assertEquals(SkillLevel.REGULAR, asset.getCrewSkillLevel());
    }

    private static BattlefieldSupportAsset assetWithDestroyCheck(int original, int current) {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setODestroyCheck(original);
        asset.setDestroyCheck(current);
        return asset;
    }

    @Test
    void damageLevelBandsForStandardAsset() {
        // oDestroyCheck <= 9: light/moderate/heavy at a loss of 1/2/3.
        assertEquals(Entity.DMG_NONE, assetWithDestroyCheck(7, 7).getDamageLevel());
        assertEquals(Entity.DMG_LIGHT, assetWithDestroyCheck(7, 6).getDamageLevel());
        assertEquals(Entity.DMG_MODERATE, assetWithDestroyCheck(7, 5).getDamageLevel());
        assertEquals(Entity.DMG_HEAVY, assetWithDestroyCheck(7, 4).getDamageLevel());
        // A check of 2 is heavily damaged but not yet destroyed.
        assertEquals(Entity.DMG_HEAVY, assetWithDestroyCheck(7, 2).getDamageLevel());
    }

    @Test
    void damageLevelBandsForToughAsset() {
        // 9 < oDestroyCheck <= 11: light/moderate/heavy at a loss of 1/3/4.
        assertEquals(Entity.DMG_NONE, assetWithDestroyCheck(10, 10).getDamageLevel());
        assertEquals(Entity.DMG_LIGHT, assetWithDestroyCheck(10, 9).getDamageLevel());
        assertEquals(Entity.DMG_LIGHT, assetWithDestroyCheck(10, 8).getDamageLevel());
        assertEquals(Entity.DMG_MODERATE, assetWithDestroyCheck(10, 7).getDamageLevel());
        assertEquals(Entity.DMG_HEAVY, assetWithDestroyCheck(10, 6).getDamageLevel());
    }

    @Test
    void damageLevelBandsForToughestAsset() {
        // oDestroyCheck > 11: light/moderate/heavy at a loss of 1/3/5.
        assertEquals(Entity.DMG_LIGHT, assetWithDestroyCheck(12, 11).getDamageLevel());
        assertEquals(Entity.DMG_LIGHT, assetWithDestroyCheck(12, 10).getDamageLevel());
        assertEquals(Entity.DMG_MODERATE, assetWithDestroyCheck(12, 9).getDamageLevel());
        assertEquals(Entity.DMG_MODERATE, assetWithDestroyCheck(12, 8).getDamageLevel());
        assertEquals(Entity.DMG_HEAVY, assetWithDestroyCheck(12, 7).getDamageLevel());
    }

    @Test
    void assetIsCrippledOnlyWhenDestroyed() {
        // Destroyed means a current Destroy Check below 2; crippled is reserved for that.
        assertFalse(assetWithDestroyCheck(7, 2).isDestroyedByDamage());
        assertTrue(assetWithDestroyCheck(7, 1).isDestroyedByDamage());
        assertTrue(assetWithDestroyCheck(7, 0).isDestroyedByDamage());
        assertEquals(Entity.DMG_CRIPPLED, assetWithDestroyCheck(7, 1).getDamageLevel());
        assertEquals(Entity.DMG_CRIPPLED, assetWithDestroyCheck(7, 0).getDamageLevel());
    }

    @Test
    void assetWithoutVeteranProfileIsAlwaysRegular() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setCost(10);
        asset.setCrew(new Crew(asset.defaultCrewType()));

        assertFalse(asset.hasVeteranProfile());
        asset.setVeteranCrew(true); // encodes Veteran gunnery, but there is no Veteran variant
        assertFalse(asset.isVeteranCrew());
        assertEquals(10 * 20, asset.calculateBattleValue());
    }

    @Test
    void veteranValuesNullAndDisplayWithoutParensWhenUnset() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setCost(10);
        asset.setSkill(6);
        assertNull(asset.getVeteranBsp());
        assertNull(asset.getVeteranBv());
        assertEquals("6", asset.getSkillDisplay());
        assertEquals("10", asset.getCostDisplay());
    }

    @Test
    void negativeTmmIsSigned() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setTmm(-1);
        assertEquals("-1", asset.getTmmDisplay());
    }

    @Test
    void effectiveCardNameFallsBackToRealIdentity() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset(maximData());
        asset.setCardTitle(null);
        asset.setCardSubtitle(null);
        assertEquals("Maxim Heavy Hover Transport", asset.getEffectiveCardTitle());
        assertEquals("", asset.getEffectiveCardSubtitle());
    }

    @Test
    void keywordRangeUsesArtilleryTypeLabel() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setRange(BFSRange.KEYWORD);
        asset.setSpecials(List.of(BFSSpecial.of("Artillery", "LT"), BFSSpecial.of("No Turret")));
        assertEquals(BFSArtilleryType.LONG_TOM, asset.getArtilleryType());
        assertEquals("Long Tom", asset.getRangeDisplay());
    }

    @Test
    void keywordRangeWithNoRelevantSpecialIsEmDash() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setRange(BFSRange.KEYWORD);
        assertEquals(EM_DASH, asset.getRangeDisplay());
        assertNull(asset.getArtilleryType());
    }

    @Test
    void emptySpecialsDisplayIsEmDash() {
        assertEquals(EM_DASH, new BattlefieldSupportAsset().getSpecialsDisplay());
    }

    @Test
    void movementStringsComeFromMode() {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setMovementMode(EntityMovementMode.HOVER);
        assertEquals("Hover", asset.getMovementString(null));
        assertEquals("H", asset.getMovementAbbr(null));
    }
}
