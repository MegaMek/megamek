package megamek.server.totalwarfare;

import megamek.common.BattleArmor;
import megamek.common.DamageInfo;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Game;
import megamek.common.HitData;
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
}
