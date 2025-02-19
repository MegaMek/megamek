package megamek.common;

import megamek.client.Client;
import megamek.client.ui.swing.ClientGUI;
import megamek.common.options.GameOptions;
import megamek.common.options.Option;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComputeTest {

    static GameOptions mockGameOptions = mock(GameOptions.class);
    static ClientGUI cg = mock(ClientGUI.class);
    static Client client = mock(Client.class);
    static Game game;

    static Team team1 = new Team(0);
    static Team team2 = new Team(1);
    static Player player1 = new Player(0, "Test1");
    static Player player2 = new Player(1, "Test2");
    static WeaponType mockAC5 = (WeaponType) EquipmentType.get("ISAC5");
    static AmmoType mockAC5AmmoType = (AmmoType) EquipmentType.get("ISAC5 Ammo");
    static AmmoType mockLTAmmoType = (AmmoType) EquipmentType.get("ISLongTom Ammo");

    @BeforeAll
    static void setUpAll() {
        // Need equipment initialized
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        when(cg.getClient()).thenReturn(client);
        when(cg.getClient().getGame()).thenReturn(game);
        game.setOptions(mockGameOptions);

        when(mockGameOptions.booleanOption(eq(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL))).thenReturn(false);
        when(mockGameOptions.stringOption(OptionsConstants.ALLOWED_TECHLEVEL)).thenReturn("Experimental");
        when(mockGameOptions.booleanOption(OptionsConstants.ALLOWED_ERA_BASED)).thenReturn(true);
        when(mockGameOptions.booleanOption(OptionsConstants.ALLOWED_SHOW_EXTINCT)).thenReturn(false);
        Option mockTrueBoolOpt = mock(Option.class);
        Option mockFalseBoolOpt = mock(Option.class);
        when(mockTrueBoolOpt.booleanValue()).thenReturn(true);
        when(mockFalseBoolOpt.booleanValue()).thenReturn(false);
        when(mockGameOptions.getOption(anyString())).thenReturn(mockTrueBoolOpt);
        when(mockGameOptions.intOption(OptionsConstants.ALLOWED_YEAR)).thenReturn(3151);

        team1.addPlayer(player1);
        team2.addPlayer(player2);
        game.addPlayer(0, player1);
        game.addPlayer(1, player2);
    }

    @AfterEach
    void tearDown() {
    }

    Mek createMek(String chassis, String model, String crewName) {
        // Create a real Mek with some mocked fields
        Mek mockMek = new BipedMek();
        mockMek.setGame(game);
        mockMek.setChassis(chassis);
        mockMek.setModel(model);

        Crew mockCrew = mock(Crew.class);
        PilotOptions pOpt = new PilotOptions();
        when(mockCrew.getName(anyInt())).thenCallRealMethod();
        when(mockCrew.getNames()).thenReturn(new String[] { crewName });
        when(mockCrew.getOptions()).thenReturn(pOpt);
        mockMek.setCrew(mockCrew);

        return mockMek;
    }

    Infantry createInfantry(String chassis, String model, String crewName) {
        // Create a real Infantry unit with some mocked fields
        Infantry mockInfantry = new Infantry();
        mockInfantry.setGame(game);
        mockInfantry.setChassis(chassis);
        mockInfantry.setModel(model);

        Crew mockCrew = mock(Crew.class);
        PilotOptions pOpt = new PilotOptions();
        when(mockCrew.getName(anyInt())).thenCallRealMethod();
        when(mockCrew.getNames()).thenReturn(new String[] { crewName });
        when(mockCrew.getOptions()).thenReturn(pOpt);
        mockInfantry.setCrew(mockCrew);

        return mockInfantry;
    }

    AeroSpaceFighter createASF(String chassis, String model, String crewName) {
        // Create a real AeroSpaceFighter unit with some mocked fields
        AeroSpaceFighter mockAeroSpaceFighter = new AeroSpaceFighter();
        mockAeroSpaceFighter.setGame(game);
        mockAeroSpaceFighter.setChassis(chassis);
        mockAeroSpaceFighter.setModel(model);

        Crew mockCrew = mock(Crew.class);
        PilotOptions pOpt = new PilotOptions();
        when(mockCrew.getName(anyInt())).thenCallRealMethod();
        when(mockCrew.getNames()).thenReturn(new String[] { crewName });
        when(mockCrew.getOptions()).thenReturn(pOpt);
        mockAeroSpaceFighter.setCrew(mockCrew);

        return mockAeroSpaceFighter;
    }

    @Test
    void noPointBlankShotMek2MekNoHiddenSameOwner() {
        // Basic test: can't PBS from non-hidden unit at own teammate
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        target.setOwnerId(player1.getId());
        target.setId(2);

        assertFalse(Compute.canPointBlankShot(attacker, target));
    }

    @Test
    void noPointBlankShotMek2MekNoHiddenDifferentOwner() {
        // Basic test: can't PBS when not hidden
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        target.setOwnerId(player2.getId());
        target.setId(2);

        assertFalse(Compute.canPointBlankShot(attacker, target));
    }

    @Test
    void noPointBlankShotMek2MekHiddenDifferentOwnerTooFar() {
        // Basic test: Can't PBS except when at exactly 1 distance from ground target
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0,0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(0, 2);
        target.setPosition(targetCoords);

        assertFalse(Compute.canPointBlankShot(attacker, target));
    }

    @Test
    void canPointBlankShotMek2MekHiddenDifferentOwner() {
        // Basic test: Can PBS an adjacent enemy while hidden
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0,0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(0, 1);
        target.setPosition(targetCoords);

        assertTrue(Compute.canPointBlankShot(attacker, target));
    }

    @Test
    void canPointBlankShotInf2MekHiddenDifferentOwner() {
        // Basic test: Infantry can PBS an adjacent enemy while hidden
        Infantry attacker = createInfantry("Attacker Guys", "with pewpews", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0,0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(0, 1);
        target.setPosition(targetCoords);

        assertTrue(Compute.canPointBlankShot(attacker, target));
    }

    @Test
    void noPointBlankShotInf2AeroHiddenDifferentOwner() {
        // Basic test: Basic Infantry can't fire on aircraft at all!
        Infantry attacker = createInfantry("Attacker Guys", "with pewpews", "Alice");
        AeroSpaceFighter target = createASF("Target ASF", "TGA-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0,0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(0, 2);
        target.setPosition(targetCoords);
        target.setAltitude(3);

        assertFalse(Compute.canPointBlankShot(attacker, target));
    }

    @Test
    void canPointBlankShotAAInf2AeroHiddenDifferentOwner() throws LocationFullException {
        // Basic test: AA-capable Infantry can fire on aircraft directly overhead!
        Infantry attacker = createInfantry("Attacker Guys", "with AA pewpews", "Alice");
        AeroSpaceFighter target = createASF("Target ASF", "TGA-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0,0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        attacker.addEquipment(mockAC5AmmoType, Infantry.LOC_FIELD_GUNS);
        attacker.addEquipment(mockAC5, Infantry.LOC_FIELD_GUNS);

        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(0, 0);
        target.setPosition(targetCoords);
        target.setAltitude(3);

        assertTrue(Compute.canPointBlankShot(attacker, target));
    }

    @Test
    void allEnemiesOutsideBlastLongTomDirectlyOnMek() {
        // Basic test: Artillery at target's hex
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0,0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        attacker.setDeployed(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(5, 5);
        target.setPosition(targetCoords);
        target.setDeployed(true);

        game.addEntities(List.of(attacker, target));

        assertFalse(Compute.allEnemiesOutsideBlast(target, attacker, mockLTAmmoType, true, false, false, game));
    }

    @Test
    void allEnemiesOutsideBlastLongTomAdjacentToMek() {
        // Basic test: Artillery at target's hex
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0,0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        attacker.setDeployed(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(5, 5);
        target.setPosition(targetCoords);
        target.setDeployed(true);

        game.addEntities(List.of(attacker, target));

        HexTarget hTarget = new HexTarget(new Coords(6,5), HexTarget.TYPE_HEX_ARTILLERY);
        assertFalse(Compute.allEnemiesOutsideBlast(hTarget, attacker, mockLTAmmoType, true, false, false, game));
    }

    @Test
    void allEnemiesOutsideBlastLongTomTwoFromMek() {
        // Basic test: Artillery at target's hex
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0,0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        attacker.setDeployed(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(5, 5);
        target.setPosition(targetCoords);
        target.setDeployed(true);

        game.addEntities(List.of(attacker, target));

        HexTarget hTarget = new HexTarget(new Coords(6,4), HexTarget.TYPE_HEX_ARTILLERY);
        assertFalse(Compute.allEnemiesOutsideBlast(hTarget, attacker, mockLTAmmoType, true, false, false, game));
    }

    @Test
    void allEnemiesOutsideBlastLongTomThreeFromMek() {
        // Basic test: Artillery at target's hex
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0,0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        attacker.setDeployed(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(5, 5);
        target.setPosition(targetCoords);
        target.setDeployed(true);

        game.addEntities(List.of(attacker, target));

        HexTarget hTarget = new HexTarget(new Coords(8,6), HexTarget.TYPE_HEX_ARTILLERY);
        assertTrue(Compute.allEnemiesOutsideBlast(hTarget, attacker, mockLTAmmoType, true, false, false, game));
    }

    @Test
    void allEnemiesOutsideBlastLongTomTwoUnderMek() {
        // Basic test: Artillery at target's hex
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0,0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        attacker.setDeployed(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(5, 5);
        target.setPosition(targetCoords);
        target.setElevation(2);
        target.setDeployed(true);

        game.addEntities(List.of(attacker, target));

        HexTarget hTarget = new HexTarget(new Coords(5,5), HexTarget.TYPE_HEX_ARTILLERY);
        assertFalse(Compute.allEnemiesOutsideBlast(hTarget, attacker, mockLTAmmoType, true, false, false, game));
    }

    @Test
    void allEnemiesOutsideBlastLongTomThreeUnderMek() {
        // Basic test: Artillery at target's hex
        Mek attacker = createMek("Attacker", "ATK-1", "Alice");
        Mek target = createMek("Target", "TGT-2", "Bob");

        attacker.setOwnerId(player1.getId());
        attacker.setId(1);
        Coords attackerCoords = new Coords(0, 0);
        attacker.setPosition(attackerCoords);
        attacker.setHidden(true);
        attacker.setDeployed(true);
        target.setOwnerId(player2.getId());
        target.setId(2);
        Coords targetCoords = new Coords(5, 5);
        target.setPosition(targetCoords);
        target.setElevation(3);
        target.setDeployed(true);

        game.addEntities(List.of(attacker, target));

        HexTarget hTarget = new HexTarget(new Coords(5, 5), HexTarget.TYPE_HEX_ARTILLERY);
        assertTrue(Compute.allEnemiesOutsideBlast(hTarget, attacker, mockLTAmmoType, true, false, false, game));
    }
}
