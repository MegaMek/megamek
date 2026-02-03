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
package megamek.client.ui.dialogs.advancedsearch;

import com.fasterxml.jackson.annotation.JsonInclude;
import megamek.common.alphaStrike.ASDamage;
import megamek.common.alphaStrike.ASUnitType;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.units.UnitRole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.formdev.flatlaf.extras.components.FlatTriStateCheckBox.State;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
class AdvSearchState {

    static final String CONTENT = "ADVANCED_SEARCH_STATE";

    // === The search state ===

    // === DO NOT EDIT CARELESSLY ===
    // Note that any changes in any of the states may easily break saved searches which would be very annoying for
    // everyone using them. The only exception is adding a new data field with a default value ("public
    // isSuperHeavy = false;") - the saved searches only contain non-default data and missing fields use their default
    // value when a search is loaded. Don't rename fields. If a field becomes obsolete, consider
    // leaving it here (instead of removing it) and ignoring it in the UI code (applyState() methods).

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public String content = CONTENT;

    public String name = "Unnamed";
    public TwState twState = new TwState();
    public AsState asState = new AsState();

    // === END ===

    // === Substates ===

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class TwState {

        public UnitTypeState unitTypeState = new UnitTypeState();
        public TransportsState transportsState = new TransportsState();
        public QuirksState quirksState = new QuirksState();
        public MiscState miscState = new MiscState();
        public EquipmentState equipmentState = new EquipmentState();
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class UnitTypeState {

        public State protoMek = State.UNSELECTED;
        public State mek = State.UNSELECTED;
        public State bipedMek = State.UNSELECTED;
        public State lam = State.UNSELECTED;
        public State tripod = State.UNSELECTED;
        public State quad = State.UNSELECTED;
        public State quadVee = State.UNSELECTED;
        public State aero = State.UNSELECTED;
        public State fixedWingSupport = State.UNSELECTED;
        public State convFighter = State.UNSELECTED;
        public State smallCraft = State.UNSELECTED;
        public State dropship = State.UNSELECTED;
        public State jumpship = State.UNSELECTED;
        public State warship = State.UNSELECTED;
        public State spaceStation = State.UNSELECTED;
        public State infantry = State.UNSELECTED;
        public State aerospaceFighter = State.UNSELECTED;
        public State battleArmor = State.UNSELECTED;
        public State tank = State.UNSELECTED;
        public State vtol = State.UNSELECTED;
        public State supportVTOL = State.UNSELECTED;
        public State gunEmplacement = State.UNSELECTED;
        public State supportTank = State.UNSELECTED;
        public State largeSupportTank = State.UNSELECTED;
        public State superHeavyTank = State.UNSELECTED;
        public State omni = State.UNSELECTED;
        public State military = State.UNSELECTED;
        public State industrial = State.UNSELECTED;
        public State mountedInfantry = State.UNSELECTED;
        public State waterOnly = State.UNSELECTED;
        public State supportVehicle = State.UNSELECTED;
        public State doomedOnGround = State.UNSELECTED;
        public State doomedInAtmosphere = State.UNSELECTED;
        public State doomedInSpace = State.UNSELECTED;
        public State doomedInExtremeTemp = State.UNSELECTED;
        public State doomedInVacuum = State.UNSELECTED;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class AsState {

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

        public String startTroopSpace = "";
        public String endTroopSpace = "";
        public String startASFBays = "";
        public String endASFBays = "";
        public String startASFDoors = "";
        public String endASFDoors = "";
        public String startASFUnits = "";
        public String endASFUnits = "";
        public String startSmallCraftBays = "";
        public String endSmallCraftBays = "";
        public String startSmallCraftDoors = "";
        public String endSmallCraftDoors = "";
        public String startSmallCraftUnits = "";
        public String endSmallCraftUnits = "";
        public String startMekBays = "";
        public String endMekBays = "";
        public String startMekDoors = "";
        public String endMekDoors = "";
        public String startMekUnits = "";
        public String endMekUnits = "";
        public String startHeavyVehicleBays = "";
        public String endHeavyVehicleBays = "";
        public String startHeavyVehicleDoors = "";
        public String endHeavyVehicleDoors = "";
        public String startHeavyVehicleUnits = "";
        public String endHeavyVehicleUnits = "";
        public String startLightVehicleBays = "";
        public String endLightVehicleBays = "";
        public String startLightVehicleDoors = "";
        public String endLightVehicleDoors = "";
        public String startLightVehicleUnits = "";
        public String endLightVehicleUnits = "";
        public String startProtomekBays = "";
        public String endProtomekBays = "";
        public String startProtomekDoors = "";
        public String endProtomekDoors = "";
        public String startProtomekUnits = "";
        public String endProtomekUnits = "";
        public String startBattleArmorBays = "";
        public String endBattleArmorBays = "";
        public String startBattleArmorDoors = "";
        public String endBattleArmorDoors = "";
        public String startBattleArmorUnits = "";
        public String endBattleArmorUnits = "";
        public String startInfantryBays = "";
        public String endInfantryBays = "";
        public String startInfantryDoors = "";
        public String endInfantryDoors = "";
        public String startInfantryUnits = "";
        public String endInfantryUnits = "";
        public String startSuperHeavyVehicleBays = "";
        public String endSuperHeavyVehicleBays = "";
        public String startSuperHeavyVehicleDoors = "";
        public String endSuperHeavyVehicleDoors = "";
        public String startSuperHeavyVehicleUnits = "";
        public String endSuperHeavyVehicleUnits = "";
        public String startDropShuttleBays = "";
        public String endDropShuttleBays = "";
        public String startDropShuttleDoors = "";
        public String endDropShuttleDoors = "";
        public String startDropShuttleUnits = "";
        public String endDropShuttleUnits = "";
        public String startDockingCollars = "";
        public String endDockingCollars = "";
        public String startBattleArmorHandles = "";
        public String endBattleArmorHandles = "";
        public String startCargoBayUnits = "";
        public String endCargoBayUnits = "";
        public String startNavalRepairFacilities = "";
        public String endNavalRepairFacilities = "";
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

        public String startWalk = "";
        public String endWalk = "";
        public String startJump = "";
        public String endJump = "";
        public int armor = 0;
        public int official = 0;
        public int canon = 0;
        public int patchwork = 0;
        public int invalid = 0;
        public int failedToLoadEquipment = 0;
        public int clanEngine = 0;
        public String startTankTurrets = "";
        public String endTankTurrets = "";
        public String startLowerArms = "";
        public String endLowerArms = "";
        public String startHands = "";
        public String endHands = "";
        public String startYear = "";
        public String endYear = "";
        public String startTons = "";
        public String endTons = "";
        public String startBV = "";
        public String endBV = "";
        public String source = "";
        public String mulId = "";

        public TriStateItemListState cockpitType = new TriStateItemListState();
        public TriStateItemListState armorType = new TriStateItemListState();
        public TriStateItemListState internalsType = new TriStateItemListState();
        public TriStateItemListState engineType = new TriStateItemListState();
        public TriStateItemListState gyroType = new TriStateItemListState();
        public TriStateItemListState techLevel = new TriStateItemListState();
        public TriStateItemListState techBase = new TriStateItemListState();
        public TriStateItemListState moveMode = new TriStateItemListState();
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class EquipmentState {

        public List<FilterToken> filterTokens = new ArrayList<>();
    }
}
