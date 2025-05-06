package megamek.common;

import megamek.common.equipment.WeaponMounted;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArtilleryTrackerTest {

    protected ArtilleryTracker artilleryTracker = new ArtilleryTracker();
    protected Tank tank;
    protected WeaponType sniperType = (WeaponType) EquipmentType.get("IS Sniper");

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws LocationFullException {
        tank = new Tank();
        tank.addEquipment(sniperType, Tank.LOC_FRONT);
        tank.aTracker = artilleryTracker;
    }

    /**
     * No-op when no weapons are registered
     */
    @Test
    void test_clearHitHexMods_with_empty_artillery_tracker() {
        // This should be a no-op and not fail
        artilleryTracker.clearHitHexMods();
    }

    /**
     * No-op when there are no mods
     */
    @Test
    void test_clearHitHexMods_with_no_mods_on_one_weapon() {
        WeaponMounted weapon = tank.getWeapon(0);
        artilleryTracker.addWeapon(weapon);

        // Should report one weapon with no mods
        assertEquals(1, artilleryTracker.getSize());
        assertEquals(0, artilleryTracker.getWeaponModifiers(weapon).size());

        artilleryTracker.clearHitHexMods();

        // No change is expected
        assertEquals(1, artilleryTracker.getSize());
        assertEquals(0, artilleryTracker.getWeaponModifiers(weapon).size());
    }

    /**
     * Test to show that standard to-hit mods are not removed
     */
    @Test
    void test_clearHitHexMods_with_standard_mod_on_one_weapon() {
        WeaponMounted weapon = tank.getWeapon(0);
        artilleryTracker.addWeapon(weapon);

        artilleryTracker.setModifier(7, new Coords(8, 9));

        // Should report one weapon with base mod
        assertEquals(1, artilleryTracker.getSize());
        assertEquals(1, artilleryTracker.getWeaponModifiers(weapon).size());

        artilleryTracker.clearHitHexMods();

        // No change is expected
        assertEquals(1, artilleryTracker.getSize());
        assertEquals(1, artilleryTracker.getWeaponModifiers(weapon).size());
    }

    /**
     * Test to show that normal mods are left in place after automatic hit mods are removed
     */
    @Test
    void test_clearHitHexMods_with_standard_and_autohit_mods_on_one_weapon() {
        WeaponMounted weapon = tank.getWeapon(0);
        artilleryTracker.addWeapon(weapon);

        artilleryTracker.setModifier(7, new Coords(8, 9));
        artilleryTracker.setModifier(TargetRoll.AUTOMATIC_SUCCESS, new Coords(12, 15));

        // Should report one weapon with base mod and autohit mod
        assertEquals(1, artilleryTracker.getSize());
        assertEquals(2, artilleryTracker.getWeaponModifiers(weapon).size());

        artilleryTracker.clearHitHexMods();

        // Autohit mod is removed
        assertEquals(1, artilleryTracker.getSize());
        assertEquals(1, artilleryTracker.getWeaponModifiers(weapon).size());
    }

    /**
     * This test shows that any automatic hit mods are removed from _all_ weapons at the same
     * time.
     * @throws LocationFullException
     */
    @Test
    void test_clearHitHexMods_with_standard_and_autohit_mods_on_two_weapons() throws LocationFullException {
        tank.addEquipment(sniperType, Tank.LOC_REAR);
        WeaponMounted weapon1 = tank.getWeapon(0);
        WeaponMounted weapon2 = tank.getWeapon(0);
        artilleryTracker.addWeapon(weapon1);
        artilleryTracker.addWeapon(weapon2);

        artilleryTracker.setModifier(7, new Coords(8, 9));
        artilleryTracker.setModifier(TargetRoll.AUTOMATIC_SUCCESS, new Coords(12, 15));

        // Should report two weapons with base mod and autohit mod each
        assertEquals(2, artilleryTracker.getSize());
        assertEquals(2, artilleryTracker.getWeaponModifiers(weapon1).size());
        assertEquals(2, artilleryTracker.getWeaponModifiers(weapon2).size());

        artilleryTracker.clearHitHexMods();

        // Autohit mod is removed from both weapons
        assertEquals(2, artilleryTracker.getSize());
        assertEquals(1, artilleryTracker.getWeaponModifiers(weapon1).size());
        assertEquals(1, artilleryTracker.getWeaponModifiers(weapon2).size());
    }
}
