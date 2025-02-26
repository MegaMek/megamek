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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CargoBayTest {

    static GameOptions mockGameOptions = mock(GameOptions.class);
    static ClientGUI cg = mock(ClientGUI.class);
    static Client client = mock(Client.class);
    static Game game = new Game();

    static Team team1 = new Team(0);
    static Team team2 = new Team(1);
    static Player player1 = new Player(0, "Test1");
    static Player player2 = new Player(1, "Test2");
    static AmmoType mockLTAmmoType = (AmmoType) EquipmentType.get("ISLongTom Ammo");
    static AmmoType mockSniperAmmoType = (AmmoType) EquipmentType.get("ISSniper Ammo");
    static AmmoType mockBombHEAmmoType = (AmmoType) EquipmentType.get("HEBomb");
    static AmmoType mockBombFAEAmmoType = (AmmoType) EquipmentType.get("FABombSmall Ammo");

    @BeforeAll
    static void setUpAll() {
        // Need equipment initialized
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
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

    @ParameterizedTest()
    @EnumSource(names = {"INF_LEG", "INF_MOTORIZED", "INF_JUMP", "INF_UMU", "HOVER", "SUBMARINE", "TRACKED", "VTOL", "WHEELED"})
    void testCargoBayCanLoadInfantry(EntityMovementMode mode) {
        CargoBay bay = new CargoBay(100.0, 1, 1);
        Infantry unit = createInfantry(mode.name(), "", "John Q. Test");
        unit.setMovementMode(mode);
        assertTrue(bay.canLoad(unit));
    }

    @ParameterizedTest()
    @EnumSource(names = {"INF_LEG", "INF_MOTORIZED", "INF_JUMP", "INF_UMU", "HOVER", "SUBMARINE", "TRACKED", "VTOL", "WHEELED"})
    void testCargoBayCanUnLoadInfantry(EntityMovementMode mode) {
        CargoBay bay = new CargoBay(100.0, 1, 1);
        Infantry unit = createInfantry(mode.name(), "", "John Q. Test");
        unit.setMovementMode(mode);
        assertTrue(bay.canUnloadUnits());
    }

    @Test
    void testCargoBayCannotLoadMek() {
        CargoBay bay = new CargoBay(100.0, 1, 1);
        Mek mek = createMek("TST-01", "Testor", "Alyce B. Carlos");
        assertFalse(bay.canLoad(mek));
    }

    @Test
    void testCargoBayCannotLoadASF() {
        CargoBay bay = new CargoBay(100.0, 1, 1);
        AeroSpaceFighter asf = createASF("TST-02", "Testor", "Alyce B. Carlos");
        assertFalse(bay.canLoad(asf));
    }
}
