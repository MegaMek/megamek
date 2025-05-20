package megamek.server.totalwarfare;

import megamek.common.BattleArmor;
import megamek.common.BipedMek;
import megamek.common.DamageInfo;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Game;
import megamek.common.HitData;
import megamek.common.IArmorState;
import megamek.common.MekFileParser;
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

    @BeforeEach
    void setUp() {
        // noop for now
        game = gameMan.getGame();
        // DamageManagers will throw if uninitialized at use
        oldMan = new TWDamageManager(gameMan, game);
        newMan = new TWDamageManagerNew(gameMan, game);
    }

    @Test
    void damageBAComparison() throws FileNotFoundException {
        String unit = "Elemental BA [Laser] (Sqd5).blk";
        BattleArmor battleArmor = (BattleArmor) loadEntityFromFile(unit);

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
    void damageMekHardenedArmorNoPSR() throws FileNotFoundException {
        String unit = "Hachiwara HCA-6P.mtf";
        BipedMek mek = (BipedMek) loadEntityFromFile(unit);

        // Validate starting armor (33 points of hardened ~= 66 points standard)
        assertEquals(33, mek.getArmor(BipedMek.LOC_CT));

        // Deal "39" points of damage (should fill 19 circles and half of 1)
        HitData hit = new HitData(BipedMek.LOC_CT);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 39);
        newMan.damageEntity(damageInfo);

        assertEquals(14, mek.getArmor(BipedMek.LOC_CT));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // Show old system incorrectly causing a PSR
        BipedMek mek2 = (BipedMek) loadEntityFromFile(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CT);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 39);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekHardenedArmorWithPSR() throws FileNotFoundException {
        String unit = "Hachiwara HCA-6P.mtf";
        BipedMek mek = (BipedMek) loadEntityFromFile(unit);

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
    void damageMekBallisticReinforcedArmorNoPSR() throws FileNotFoundException {
        String unit = "Dervish DV-11DK.mtf";
        BipedMek mek = (BipedMek) loadEntityFromFile(unit);

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
        BipedMek mek2 = (BipedMek) loadEntityFromFile(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CT);
        hit2.setGeneralDamageType(HitData.DAMAGE_MISSILE);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 39);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekBallisticReinforcedArmorWithPSR() throws FileNotFoundException {
        String unit = "Dervish DV-11DK.mtf";
        BipedMek mek = (BipedMek) loadEntityFromFile(unit);

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
        BipedMek mek = (BipedMek) loadEntityFromFile(unit);

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
        BipedMek mek2 = (BipedMek) loadEntityFromFile(unit);
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
        BipedMek mek = (BipedMek) loadEntityFromFile(unit);
        mek.setGame(game);

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
    void damageMekFerroLamellorArmorNoPSR() throws FileNotFoundException {
        String unit = "Charger C.mtf";
        BipedMek mek = (BipedMek) loadEntityFromFile(unit);

        // Validate starting armor
        assertEquals(40, mek.getArmor(BipedMek.LOC_CT));

        // Deal "24" points of damage (should fill 19? circles)
        HitData hit = new HitData(BipedMek.LOC_CT);
        hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 24);
        newMan.damageEntity(damageInfo);

        assertEquals(21, mek.getArmor(BipedMek.LOC_CT));
        assertFalse(gameMan.checkForPSRFromDamage(mek));

        // Show old system incorrectly causing a PSR
        BipedMek mek2 = (BipedMek) loadEntityFromFile(unit);
        HitData hit2 = new HitData(BipedMek.LOC_CT);
        hit2.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
        DamageInfo damageInfo2 = new DamageInfo(mek2, hit2, 24);
        oldMan.damageEntity(damageInfo2);
        assertTrue(gameMan.checkForPSRFromDamage(mek2));
    }

    @Test
    void damageMekFerroLamellorArmorWithPSR() throws FileNotFoundException {
        String unit = "Charger C.mtf";
        BipedMek mek = (BipedMek) loadEntityFromFile(unit);

        // Validate starting armor
        assertEquals(40, mek.getArmor(BipedMek.LOC_CT));

        // Deal "25" points of damage (should fill 20 circles)
        HitData hit = new HitData(BipedMek.LOC_CT);
        hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
        DamageInfo damageInfo = new DamageInfo(mek, hit, 25);
        newMan.damageEntity(damageInfo);

        assertEquals(20, mek.getArmor(BipedMek.LOC_CT));
        assertTrue(gameMan.checkForPSRFromDamage(mek));
    }
}
