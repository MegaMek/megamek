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
    // Note that any changes in any of the states will likely break saved searches which would be very annoying for
    // everyone using them. The only exception is adding a new data field with a default value (public
    // isSuperHeavy = false;) - the saved searches only contain non-default data and missing fields use their default
    // value when a search is loaded. Don't rename fields. If a field becomes obsolete, consider
    // leaving it here (instead of removing it) and ignoring it in the UI code (applyState() methods).

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public String content = CONTENT;

    public String name = "Unnamed";
    public TwState twState;
    public AsState asState;

    // === END ===

    // === Substates ===

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class TwState {

        public UnitTypeState unitTypeState;
        public TransportsState transportsState;
        public QuirksState quirksState;
        public MiscState miscState;
        public EquipmentState equipmentState;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class UnitTypeState {

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

        public List<FilterToken> filterTokens = new ArrayList<>();
    }
}
