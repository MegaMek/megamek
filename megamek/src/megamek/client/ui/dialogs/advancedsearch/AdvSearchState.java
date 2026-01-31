package megamek.client.ui.dialogs.advancedsearch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import megamek.client.ui.Messages;
import megamek.common.alphaStrike.ASDamage;
import megamek.common.alphaStrike.ASUnitType;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.units.UnitRole;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.table.TableRowSorter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.formdev.flatlaf.extras.components.FlatTriStateCheckBox.State;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class AdvSearchState {

    public static final ObjectMapper MAPPER = JsonMapper.builder()
          .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .serializationInclusion(JsonInclude.Include.NON_DEFAULT)
          .enable(SerializationFeature.INDENT_OUTPUT)
          .build();

    public static AdvSearchState fromJson(File file) throws IOException {
        return MAPPER.readValue(file, AdvSearchState.class);
    }

    public static void save(File file, AdvSearchState state) throws IOException {
        MAPPER.writeValue(file, state);
    }

    // === The search state ===

    public int schemaVersion = 1;

    public String name = "Unnamed";
    public TwState twState;
    public AsState asState;

    // === END ===

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class TwState {

        @JsonInclude(JsonInclude.Include.ALWAYS)
        public int schemaVersion = 1;

        public UnitTypeState unitTypeState;
        public TransportsState transportsState;
        public QuirksState quirksState;
        public MiscState miscState;
        public EquipmentState equipmentState;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class UnitTypeState {

        @JsonInclude(JsonInclude.Include.ALWAYS)
        public int schemaVersion = 1;

        public State ProtoMek = State.UNSELECTED;
        public State Mek = State.UNSELECTED;
        public State BipedMek = State.UNSELECTED;
        public State LAM = State.UNSELECTED;
        public State Tripod = State.UNSELECTED;
        public State Quad = State.UNSELECTED;
        public State QuadVee = State.UNSELECTED;
        public State Aero = State.UNSELECTED;
        public State FixedWingSupport = State.UNSELECTED;
        public State ConvFighter = State.UNSELECTED;
        public State SmallCraft = State.UNSELECTED;
        public State Dropship = State.UNSELECTED;
        public State Jumpship = State.UNSELECTED;
        public State Warship = State.UNSELECTED;
        public State SpaceStation = State.UNSELECTED;
        public State Infantry = State.UNSELECTED;
        public State AerospaceFighter = State.UNSELECTED;
        public State BattleArmor = State.UNSELECTED;
        public State Tank = State.UNSELECTED;
        public State VTOL = State.UNSELECTED;
        public State SupportVTOL = State.UNSELECTED;
        public State GunEmplacement = State.UNSELECTED;
        public State SupportTank = State.UNSELECTED;
        public State LargeSupportTank = State.UNSELECTED;
        public State SuperHeavyTank = State.UNSELECTED;
        public State Omni = State.UNSELECTED;
        public State Military = State.UNSELECTED;
        public State Industrial = State.UNSELECTED;
        public State MountedInfantry = State.UNSELECTED;
        public State WaterOnly = State.UNSELECTED;
        public State SupportVehicle = State.UNSELECTED;
        public State DoomedOnGround = State.UNSELECTED;
        public State DoomedInAtmosphere = State.UNSELECTED;
        public State DoomedInSpace = State.UNSELECTED;
        public State DoomedInExtremeTemp = State.UNSELECTED;
        public State DoomedInVacuum = State.UNSELECTED;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class AsState {

        @JsonInclude(JsonInclude.Include.ALWAYS)
        public int schemaVersion = 1;

        public boolean unitTypeUse = false;
        public boolean unitRoleUse = false;
        public List<ASUnitType> unitTypeSelected = new ArrayList<>();
        public List<UnitRole> unitRoleSelected = new ArrayList<>();

        public boolean sizeUse = false;
        public List<Integer> sizeSelected = new ArrayList<>();

        public boolean tmmUse = false;
        public List<Integer> tmmSelected = new ArrayList<>();

        public boolean ovUse = false;
        public List<Integer> ovSelected = new ArrayList<>();

        public boolean armorUse = false;
        public String armorFromText = "";
        public String armorToText = "";

        public boolean structureUse = false;
        public String structureFromText = "";
        public String structureToText = "";

        public boolean thresholdUse = false;
        public String thresholdFromText = "";
        public String thresholdToText = "";

        public boolean damageSUse = false;
        public ASDamage damageSFromValue = ASDamage.ZERO;
        public ASDamage damageSToValue = ASDamage.ZERO;

        public boolean damageMUse = false;
        public ASDamage damageMFromValue = ASDamage.ZERO;
        public ASDamage damageMToValue = ASDamage.ZERO;

        public boolean damageLUse = false;
        public ASDamage damageLFromValue = ASDamage.ZERO;
        public ASDamage damageLToValue = ASDamage.ZERO;

        public boolean damageEUse = false;
        public ASDamage damageEFromValue = ASDamage.ZERO;
        public ASDamage damageEToValue = ASDamage.ZERO;

        public boolean pvUse = false;
        public String pvFromText = "";
        public String pvToText = "";

        public boolean mvUse = false;
        public String mvModeText = "";
        public String mvFromText = "";
        public String mvToText = "";

        public boolean ability1Use = false;
        public BattleForceSUA ability1Value = BattleForceSUA.UNKNOWN;

        public boolean ability2Use = false;
        public BattleForceSUA ability2Value = BattleForceSUA.UNKNOWN;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class TransportsState {

        @JsonInclude(JsonInclude.Include.ALWAYS)
        public int schemaVersion = 1;

        public String tStartTroopSpace = "";
        public String tEndTroopSpace = "";
        public String tStartASFBays = "";
        public String tEndASFBays = "";
        public String tStartASFDoors = "";
        public String tEndASFDoors = "";
        public String tStartASFUnits = "";
        public String tEndASFUnits = "";
        public String tStartSmallCraftBays = "";
        public String tEndSmallCraftBays = "";
        public String tStartSmallCraftDoors = "";
        public String tEndSmallCraftDoors = "";
        public String tStartSmallCraftUnits = "";
        public String tEndSmallCraftUnits = "";
        public String tStartMekBays = "";
        public String tEndMekBays = "";
        public String tStartMekDoors = "";
        public String tEndMekDoors = "";
        public String tStartMekUnits = "";
        public String tEndMekUnits = "";
        public String tStartHeavyVehicleBays = "";
        public String tEndHeavyVehicleBays = "";
        public String tStartHeavyVehicleDoors = "";
        public String tEndHeavyVehicleDoors = "";
        public String tStartHeavyVehicleUnits = "";
        public String tEndHeavyVehicleUnits = "";
        public String tStartLightVehicleBays = "";
        public String tEndLightVehicleBays = "";
        public String tStartLightVehicleDoors = "";
        public String tEndLightVehicleDoors = "";
        public String tStartLightVehicleUnits = "";
        public String tEndLightVehicleUnits = "";
        public String tStartProtomekBays = "";
        public String tEndProtomekBays = "";
        public String tStartProtomekDoors = "";
        public String tEndProtomekDoors = "";
        public String tStartProtomekUnits = "";
        public String tEndProtomekUnits = "";
        public String tStartBattleArmorBays = "";
        public String tEndBattleArmorBays = "";
        public String tStartBattleArmorDoors = "";
        public String tEndBattleArmorDoors = "";
        public String tStartBattleArmorUnits = "";
        public String tEndBattleArmorUnits = "";
        public String tStartInfantryBays = "";
        public String tEndInfantryBays = "";
        public String tStartInfantryDoors = "";
        public String tEndInfantryDoors = "";
        public String tStartInfantryUnits = "";
        public String tEndInfantryUnits = "";
        public String tStartSuperHeavyVehicleBays = "";
        public String tEndSuperHeavyVehicleBays = "";
        public String tStartSuperHeavyVehicleDoors = "";
        public String tEndSuperHeavyVehicleDoors = "";
        public String tStartSuperHeavyVehicleUnits = "";
        public String tEndSuperHeavyVehicleUnits = "";
        public String tStartDropShuttleBays = "";
        public String tEndDropShuttleBays = "";
        public String tStartDropShuttleDoors = "";
        public String tEndDropShuttleDoors = "";
        public String tStartDropShuttleUnits = "";
        public String tEndDropShuttleUnits = "";
        public String tStartDockingCollars = "";
        public String tEndDockingCollars = "";
        public String tStartBattleArmorHandles = "";
        public String tEndBattleArmorHandles = "";
        public String tStartCargoBayUnits = "";
        public String tEndCargoBayUnits = "";
        public String tStartNavalRepairFacilities = "";
        public String tEndNavalRepairFacilities = "";
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class TriStateItemListState {
        public Map<String, State> items = new HashMap<>();
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class QuirksState {
        public int chassisInclude = 0;
        public int chassisExclude = 1;
        public int weaponInclude = 0;
        public int weaponExclude = 1;
        public TriStateItemListState chassisQuirks = new TriStateItemListState();
        public TriStateItemListState weaponQuirks = new TriStateItemListState();
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class MiscState {

        @JsonInclude(JsonInclude.Include.ALWAYS)
        public int schemaVersion = 1;

        public String tStartWalk = "";
        public String tEndWalk = "";
        public String tStartJump = "";
        public String tEndJump = "";
        public int cArmor = 0;
        public int cOfficial = 0;
        public int cCanon = 0;
        public int cPatchwork = 0;
        public int cInvalid = 0;
        public int cFailedToLoadEquipment = 0;
        public int cClanEngine = 0;
        public String tStartTankTurrets = "";
        public String tEndTankTurrets = "";
        public String tStartLowerArms = "";
        public String tEndLowerArms = "";
        public String tStartHands = "";
        public String tEndHands = "";
        public String tStartYear = "";
        public String tEndYear = "";
        public String tStartTons = "";
        public String tEndTons = "";
        public String tStartBV = "";
        public String tEndBV = "";
        public String tSource = "";
        public String tMULId = "";

        public TriStateItemListState listCockpitType = new TriStateItemListState();
        public TriStateItemListState listArmorType = new TriStateItemListState();
        public TriStateItemListState listInternalsType = new TriStateItemListState();
        public TriStateItemListState listEngineType = new TriStateItemListState();
        public TriStateItemListState listGyroType = new TriStateItemListState();
        public TriStateItemListState listTechLevel = new TriStateItemListState();
        public TriStateItemListState listTechBase = new TriStateItemListState();
        public TriStateItemListState listMoveMode = new TriStateItemListState();
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class EquipmentState {

        @JsonInclude(JsonInclude.Include.ALWAYS)
        public int schemaVersion = 1;

        public List<FilterToken> filterTokens = new ArrayList<>();

    }

    public final class JsonPopupFactory {

        /**
         * Creates a popup menu containing up to 10 most recently modified JSON files in the given folder. Each menu
         * item uses the JSON field "name" as its label and calls the provided handler with the corresponding file when
         * clicked.
         */
        public static JPopupMenu createPopupMenu(Path folder, Consumer<Path> onFileSelected) {
            JPopupMenu popup = new JPopupMenu();

            if (!Files.isDirectory(folder)) {
                return popup;
            }

            List<Path> recentJsonFiles;
            try (Stream<Path> stream = Files.list(folder)) {
                recentJsonFiles = stream
                      .filter(Files::isRegularFile)
                      .filter(p -> p.toString().toLowerCase().endsWith(".json"))
                      .sorted(Comparator.comparingLong(JsonPopupFactory::lastModified).reversed())
                      .limit(10)
                      .toList();
            } catch (IOException e) {
                // optionally log
                return popup;
            }

            for (Path file : recentJsonFiles) {
                String name = readNameField(file);
                if (name == null || name.isBlank()) {
                    continue;
                }

                JMenuItem item = new JMenuItem(name);
                item.addActionListener(e -> onFileSelected.accept(file));
                popup.add(item);
            }

            return popup;
        }

        private static long lastModified(Path p) {
            try {
                return Files.getLastModifiedTime(p).toMillis();
            } catch (IOException e) {
                return 0L;
            }
        }

        private static String readNameField(Path file) {
            try {
                JsonNode root = MAPPER.readTree(file.toFile());
                JsonNode nameNode = root.get("name");
                return nameNode != null && nameNode.isTextual()
                      ? nameNode.asText()
                      : null;
            } catch (IOException e) {
                return null;
            }
        }
    }

}
