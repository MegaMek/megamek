package megamek.server.totalwarfare;

import megamek.common.*;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.Option;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.DamageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

class TWDamageManagerTest {

    private final String resourcesPath = "testresources/megamek/common/units/";

    private TWGameManager gameMan = new TWGameManager();
    private TWDamageManager oldMan;
    private TWDamageManagerNew newMan;
    private Game game;

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        // Set up game with options
        game = gameMan.getGame();
        GameOptions gOp = new GameOptions();
        game.setOptions(gOp);

        // DamageManagers will throw if uninitialized at use
        oldMan = new TWDamageManager(gameMan, game);
        newMan = new TWDamageManagerNew(gameMan, game);
    }

    Entity loadEntityFromFile(String filename) throws FileNotFoundException {
        File file;
        MekFileParser mfParser;
        Entity e;

        try {
            file = new File(resourcesPath + filename);
            mfParser = new MekFileParser(file);
            e = mfParser.getEntity();
        } catch (Exception ex) {
            fail(ex.getMessage());
            return null;
        }

        return e;
    }

    BattleArmor loadBA(String filename) throws FileNotFoundException {
        BattleArmor battleArmor = (BattleArmor) loadEntityFromFile(filename);
        battleArmor.setId(game.getNextEntityId());
        game.addEntity(battleArmor);
        return battleArmor;
    }

    BipedMek loadMek(String filename) throws FileNotFoundException {
        BipedMek mek = (BipedMek) loadEntityFromFile(filename);
        mek.setId(game.getNextEntityId());
        game.addEntity(mek);
        return mek;
    }

    AeroSpaceFighter loadASF(String filename) throws FileNotFoundException {
        AeroSpaceFighter asf = (AeroSpaceFighter) loadEntityFromFile(filename);
        asf.setId(game.getNextEntityId());
        game.addEntity(asf);
        return asf;
    }

    @Test
    void damageBAComparison() throws FileNotFoundException {
        String unit = "Elemental BA [Laser] (Sqd5).blk";
        BattleArmor battleArmor = loadBA(unit);

        // Validate starting armor
        assertEquals(10, battleArmor.getArmor(BattleArmor.LOC_TROOPER_1));

        // Deal damage with original method
        HitData hit = new HitData(BattleArmor.LOC_TROOPER_1);
        DamageInfo damageInfo = new DamageInfo(battleArmor, hit, 5);
        oldMan.damageEntity(damageInfo);
        assertEquals(5, battleArmor.getArmor(BattleArmor.LOC_TROOPER_1));

        // Reset for new damage method
        BattleArmor battleArmor2 = (BattleArmor) loadEntityFromFile(unit);

        // Validate starting armor
        assertEquals(10, battleArmor2.getArmor(BattleArmor.LOC_TROOPER_1));

        // Deal damage with new method
        hit = new HitData(BattleArmor.LOC_TROOPER_1);
        damageInfo = new DamageInfo(battleArmor2, hit, 5);
        newMan.damageEntity(damageInfo);
        assertEquals(5, battleArmor2.getArmor(BattleArmor.LOC_TROOPER_1));
    }

    @Test
    void damageReactiveArmorBA() throws FileNotFoundException {
        String unit = "Black Wolf BA (ER Pulse) (Sqd5).blk";
        BattleArmor battleArmor = loadBA(unit);

        // Validate starting armor
        assertEquals(11, battleArmor.getArmor(BattleArmor.LOC_TROOPER_1));

        // Deal "20" points of damage (should fill 10 circles)
        HitData hit = new HitData(BattleArmor.LOC_TROOPER_1);
        // All AmmoWeaponHandlers (including artillery) use Ballistic damage type
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        // Set areaSatArty so all suits take damage
        DamageInfo damageInfo = new DamageInfo(battleArmor, hit, 20, false, DamageType.NONE,
              false, true);
        newMan.damageEntity(damageInfo);

        assertEquals(1, battleArmor.getArmor(BattleArmor.LOC_TROOPER_1));
    }

    @Test
    void damageMekHardenedArmorNoPSR() throws FileNotFoundException {
        String unit = "Hachiwara HCA-6P.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor (33 points of hardened ~= 66 points standard)
        assertEquals(33, mek.getArmor(BipedMek.LOC_CT));

        // Deal "39" points of damage (should fill 19 circles and half of 1)
        HitData hit = new HitData(BipedMek.LOC_CT);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 39);
        newMan.damageEntity(damageInfo);

        assertEquals(14, mek.getArmor(BipedMek.LOC_CT));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // Show old system incorrectly causing a PSR
        BipedMek mek2 = loadMek(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CT);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 39);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekHardenedArmorWithPSR() throws FileNotFoundException {
        String unit = "Hachiwara HCA-6P.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor (33 points of hardened ~= 66 points standard)
        assertEquals(33, mek.getArmor(BipedMek.LOC_CT));

        // Deal "40" points of damage (should fill 20 circles and cause a PSR)
        HitData hit = new HitData(BipedMek.LOC_CT);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 40);
        newMan.damageEntity(damageInfo);

        assertEquals(13, mek.getArmor(BipedMek.LOC_CT));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekHardenedArmorNoCritAP() throws FileNotFoundException {
        String unit = "Hachiwara HCA-6P.mtf";
        BipedMek mek = loadMek(unit);

        // Deal "20" points of AP damage (should fill 20 circles against Hardened)
        HitData hit = new HitData(BipedMek.LOC_CT, false, 0, false,
              -1, true,true, HitData.DAMAGE_ARMOR_PIERCING, 0);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 20);
        newMan.damageEntity(damageInfo);

        assertEquals(13, mek.getArmor(BipedMek.LOC_CT));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekStandardArmorCritFromAP() throws FileNotFoundException {
        String unit = "Cyclops CP-10-Z.mtf";
        BipedMek mek = loadMek(unit);
        mek.setOwner(new Player(1, "Test")); // Needed in case of pilot ejection due to crits

        // Deal "20" points of AP damage (should fill 20 circles against Standard)
        HitData hit = new HitData(BipedMek.LOC_CT, false, 0, false,
              -1, true,true, HitData.DAMAGE_ARMOR_PIERCING, 0);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 20);
        newMan.damageEntity(damageInfo);

        // Don't check for exact remaining armor as AP may produce fatal crits, but PSRs will remain
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekBallisticReinforcedArmorNoPSR() throws FileNotFoundException {
        String unit = "Dervish DV-11DK.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor (25 points of BRA ~= 50 - (1xhits) points standard against some damage types)
        assertEquals(25, mek.getArmor(BipedMek.LOC_CT));

        // Deal "39" points of damage (should fill 19 circles)
        HitData hit = new HitData(BipedMek.LOC_CT);
        hit.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 39);
        newMan.damageEntity(damageInfo);

        assertEquals(6, mek.getArmor(BipedMek.LOC_CT));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // Show old system incorrectly causing a PSR
        BipedMek mek2 = loadMek(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CT);
        hit2.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 39);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekBallisticReinforcedArmorWithPSR() throws FileNotFoundException {
        String unit = "Dervish DV-11DK.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor (25 points of BRA ~= 50 - (1xhits) points standard against some damage types)
        assertEquals(25, mek.getArmor(BipedMek.LOC_CT));

        // Deal "40" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CT);
        hit.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 40);
        newMan.damageEntity(damageInfo);

        assertEquals(5, mek.getArmor(BipedMek.LOC_CT));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekImpactResistantArmorNoPSR() throws FileNotFoundException {
        // Takes 2 damage per 3 full damage dealt
        String unit = "Storm Raider STM-R4.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(17, mek.getArmor(BipedMek.LOC_CT));

        // Deal "24" points of damage (should fill 16 circles)
        HitData hit = new HitData(BipedMek.LOC_CT);
        hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 24);
        newMan.damageEntity(damageInfo);

        assertEquals(1, mek.getArmor(BipedMek.LOC_CT));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // Show old system incorrectly causing a PSR
        BipedMek mek2 = loadMek(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CT);
        hit2.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 24);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekImpactResistantArmorWithPSR() throws FileNotFoundException {
        // Takes 2 damage per 3 full damage dealt
        String unit = "Storm Raider STM-R4.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(17, mek.getArmor(BipedMek.LOC_CT));

        // Deal "30" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CT);
        hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 30);
        newMan.damageEntity(damageInfo);

        assertEquals(IArmorState.ARMOR_DESTROYED, mek.getArmor(BipedMek.LOC_CT));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekImpactResistantArmorNonphysicalWithPSR() throws FileNotFoundException {
        // Takes normal damage when not physical damage
        String unit = "Storm Raider STM-R4.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(17, mek.getArmor(BipedMek.LOC_CT));

        // Deal 20 points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CT);
        hit.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 20);
        newMan.damageEntity(damageInfo);

        assertEquals(IArmorState.ARMOR_DESTROYED, mek.getArmor(BipedMek.LOC_CT));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekFerroLamellorArmorNoPSR() throws FileNotFoundException {
        String unit = "Charger C.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(40, mek.getArmor(BipedMek.LOC_CT));

        // Deal "24" points of damage (should fill 19? circles)
        HitData hit = new HitData(BipedMek.LOC_CT);
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 24);
        newMan.damageEntity(damageInfo);

        assertEquals(21, mek.getArmor(BipedMek.LOC_CT));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // Show old system incorrectly causing a PSR
        BipedMek mek2 = loadMek(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CT);
        hit2.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 24);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekFerroLamellorArmorWithPSR() throws FileNotFoundException {
        String unit = "Charger C.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(40, mek.getArmor(BipedMek.LOC_CT));

        // Deal "25" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CT);
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 25);
        newMan.damageEntity(damageInfo);

        assertEquals(20, mek.getArmor(BipedMek.LOC_CT));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageAeroFerroLamellorArmorCritChecksChecks() throws FileNotFoundException {
        String unit = "Slayer SL-CX1.blk";
        AeroSpaceFighter asf = loadASF(unit);

        // Validate starting armor
        assertEquals(80, asf.getArmor(AeroSpaceFighter.LOC_NOSE));

        // Deal "12" points of damage (should fill 8 circles)
        HitData hit = new HitData(AeroSpaceFighter.LOC_NOSE);
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo = new DamageInfo(asf, hit, 10);
        newMan.damageEntity(damageInfo);

        assertEquals(72, asf.getArmor(AeroSpaceFighter.LOC_NOSE));
        assertFalse(asf.wasCritThresh());

        // Show that new system does show a check is required for more damage than the threshold
        AeroSpaceFighter asf2 = loadASF(unit);
        HitData hit2 = new HitData(AeroSpaceFighter.LOC_NOSE);
        hit2.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo2 = new DamageInfo(asf2, hit2, 12);
        newMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(asf2));
    }

    @Test
    void damageMekCowlDamageCowlOnly() throws FileNotFoundException {
        String unit = "Cyclops CP-10-Z.mtf";
        BipedMek mek = loadMek(unit);

        // Configure game for Quirks
        IOption quirkOption = game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS);
        quirkOption.setValue(true);

        // Validate starting armor
        assertEquals(9, mek.getArmor(BipedMek.LOC_HEAD));

        // Deal 3 points of damage (should fill 3 circles but leave 9 still)
        HitData hit = new HitData(BipedMek.LOC_HEAD);
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 3);
        newMan.damageEntity(damageInfo);

        assertEquals(9, mek.getArmor(BipedMek.LOC_HEAD));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // reset quirk value
        quirkOption.setValue(false);
    }

    @Test
    void damageMekCowlDamageAllHeadArmor() throws FileNotFoundException {
        String unit = "Cyclops CP-10-Z.mtf";
        BipedMek mek = loadMek(unit);

        // Configure game for Quirks
        IOption quirkOption = game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS);
        quirkOption.setValue(true);

        // Validate starting armor
        assertEquals(9, mek.getArmor(BipedMek.LOC_HEAD));
        assertEquals(3, mek.getInternal(BipedMek.LOC_HEAD));

        // Deal 12 points of damage (should fill 12 total (3 + 9) leaving armor at 0 but no crits)
        HitData hit = new HitData(BipedMek.LOC_HEAD);
        hit.setGeneralDamageType(HitData.DAMAGE_BALLISTIC);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 12);
        newMan.damageEntity(damageInfo);

        // Armor is _gone_ but not _destroyed)
        assertEquals(0, mek.getArmor(BipedMek.LOC_HEAD));
        assertEquals(3, mek.getInternal(BipedMek.LOC_HEAD));
        assertFalse(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekReactiveArmorNoPSR() throws FileNotFoundException {
        String unit = "Warwolf A.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(35, mek.getArmor(BipedMek.LOC_CT));

        // Deal "39" points of damage (should fill 19 circles)
        HitData hit = new HitData(BipedMek.LOC_CT);
        hit.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 39);
        newMan.damageEntity(damageInfo);

        assertEquals(16, mek.getArmor(BipedMek.LOC_CT));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // Show old system incorrectly causing a PSR
        BipedMek mek2 = loadMek(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CT);
        hit2.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 24);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekReactiveArmorWithPSR() throws FileNotFoundException {
        String unit = "Warwolf A.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(35, mek.getArmor(BipedMek.LOC_CT));

        // Deal "40" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CT);
        hit.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 40);
        newMan.damageEntity(damageInfo);

        assertEquals(15, mek.getArmor(BipedMek.LOC_CT));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageMekReflectiveArmorNoPSR() throws FileNotFoundException {
        String unit = "Flashman FLS-10E.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(30, mek.getArmor(BipedMek.LOC_CT));

        // Deal "39" points of damage (should fill 19 circles)
        HitData hit = new HitData(BipedMek.LOC_CT);
        hit.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 39);
        newMan.damageEntity(damageInfo);

        assertEquals(11, mek.getArmor(BipedMek.LOC_CT));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // Show old system incorrectly causing a PSR
        BipedMek mek2 = loadMek(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CT);
        hit2.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 24);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekReflectiveArmorWithPSR() throws FileNotFoundException {
        String unit = "Flashman FLS-10E.mtf";
        BipedMek mek = loadMek(unit);

        // Validate starting armor
        assertEquals(30, mek.getArmor(BipedMek.LOC_CT));

        // Deal "40" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CT);
        hit.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 40);
        newMan.damageEntity(damageInfo);

        assertEquals(10, mek.getArmor(BipedMek.LOC_CT));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }

    @Test
    void damageAeroReflectiveArmorCritChecks() throws FileNotFoundException {
        String unit = "Seydlitz C.blk";
        AeroSpaceFighter asf = loadASF(unit);

        // Validate starting armor
        assertEquals(18, asf.getArmor(AeroSpaceFighter.LOC_NOSE));

        // Deal "4" points of damage (should fill 2 circles)
        HitData hit = new HitData(AeroSpaceFighter.LOC_NOSE);
        hit.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo = new DamageInfo(asf, hit, 4);
        newMan.damageEntity(damageInfo);

        assertEquals(16, asf.getArmor(AeroSpaceFighter.LOC_NOSE));
        assertFalse(asf.wasCritThresh());

        // Show that new system does show a check is required for more damage than the threshold
        AeroSpaceFighter asf2 = loadASF(unit);
        HitData hit2 = new HitData(AeroSpaceFighter.LOC_NOSE);
        hit2.setGeneralDamageType(HitData.DAMAGE_ENERGY);
        DamageInfo damageInfo2 = new DamageInfo(asf2, hit2, 5);
        newMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(asf2));
    }

}
