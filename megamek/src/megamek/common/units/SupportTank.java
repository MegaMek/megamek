/*
 * Copyright (c) 2000-2003 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.units;

import java.io.Serial;

import megamek.common.Hex;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.board.Coords;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.MiscType;

/**
 * This is a support vehicle
 *
 * @author beerockxs
 */
public class SupportTank extends Tank {
    @Serial
    private static final long serialVersionUID = -9028127010133768714L;

    private final int[] barRating;
    private double fuelTonnage = 0;

    public SupportTank() {
        super();
        barRating = new int[locations()];
    }

    @Override
    public void setBARRating(int rating, int loc) {
        barRating[loc] = rating;
    }

    @Override
    public void setBARRating(int rating) {
        for (int i = 0; i < locations(); i++) {
            barRating[i] = rating;
        }
    }

    @Override
    public int getBARRating(int loc) {
        return (barRating == null) ? 0 : barRating[loc];
    }

    @Override
    public boolean hasBARArmor(int loc) {
        return ArmorType.forEntity(this, loc).hasFlag(MiscType.F_SUPPORT_VEE_BAR_ARMOR);
    }

    @Override
    public boolean hasArmoredChassis() {
        return hasMisc(MiscType.F_ARMORED_CHASSIS);
    }

    private static final TechAdvancement TA_HOVER = new TechAdvancement(TechBase.ALL)
          .setTechRating(TechRating.C).setAdvancement(DATE_PS, DATE_ES, DATE_ES)
          .setAvailability(AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A, AvailabilityValue.A)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_HOVER_LARGE = new TechAdvancement(TechBase.ALL)
          .setTechRating(TechRating.C).setAdvancement(DATE_PS, DATE_ES, DATE_ES)
          .setAvailability(AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B, AvailabilityValue.B)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_NAVAL = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(TechRating.A)
          .setAvailability(AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_NAVAL_LARGE = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(TechRating.A)
          .setAvailability(AvailabilityValue.C, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    private static final TechAdvancement TA_TRACKED = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(TechRating.B)
          .setAvailability(AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B, AvailabilityValue.B)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_TRACKED_LARGE = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(TechRating.B)
          .setAvailability(AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_WHEELED_SMALL = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(TechRating.A)
          .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_WHEELED_MEDIUM = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(TechRating.A)
          .setAvailability(AvailabilityValue.A, AvailabilityValue.B, AvailabilityValue.A, AvailabilityValue.A)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_WHEELED_LARGE = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(TechRating.A)
          .setAvailability(AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B, AvailabilityValue.B)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_WIGE = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_ES, DATE_ES, DATE_ES).setTechRating(TechRating.C)
          .setAvailability(AvailabilityValue.B, AvailabilityValue.C, AvailabilityValue.B, AvailabilityValue.B)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_WIGE_LARGE = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_ES, DATE_ES, DATE_ES).setTechRating(TechRating.C)
          .setAvailability(AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.C, AvailabilityValue.C)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_RAIL = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(TechRating.A)
          .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
          .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    private static final TechAdvancement TA_RAIL_LARGE = new TechAdvancement(TechBase.ALL)
          .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(TechRating.A)
          .setAvailability(AvailabilityValue.C, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return getConstructionTechAdvancement(getMovementMode(), getWeightClass());
    }

    public static TechAdvancement getConstructionTechAdvancement(EntityMovementMode movementMode, int weightClass) {
        /* Support vehicle dates and tech ratings are found in TM 120, 122. DA availability is assumed to
         * be the same as Clan invasion era. */
        switch (movementMode) {
            case HOVER:
                if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                    return TA_HOVER_LARGE;
                } else {
                    return TA_HOVER;
                }
            case NAVAL:
            case HYDROFOIL:
            case SUBMARINE:
                if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                    return TA_NAVAL_LARGE;
                } else {
                    return TA_NAVAL;
                }
            case TRACKED:
                if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                    return TA_TRACKED_LARGE;
                } else {
                    return TA_TRACKED;
                }
            case WHEELED:
                if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                    return TA_WHEELED_LARGE;
                } else if (weightClass == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                    return TA_WHEELED_MEDIUM;
                } else {
                    return TA_WHEELED_SMALL;
                }
            case WIGE:
                if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                    return TA_WIGE_LARGE;
                } else {
                    return TA_WIGE;
                }
            case RAIL:
            case MAGLEV:
                if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                    return TA_RAIL_LARGE;
                } else {
                    return TA_RAIL;
                }
            default:
                return TA_TRACKED; // average
        }
    }

    @Override
    public boolean isLocationProhibited(Coords c, int testBoardId, int currElevation) {
        if (!game.hasBoardLocation(c, testBoardId)) {
            return true;
        }

        Hex hex = game.getHex(c, testBoardId);
        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }

        // Additional restrictions for hidden units
        if (isHidden()) {
            // Can't deploy in paved hexes
            if (hex.containsTerrain(Terrains.PAVEMENT)
                  || hex.containsTerrain(Terrains.ROAD)) {
                return true;
            }
            // Can't deploy on a bridge
            if ((hex.terrainLevel(Terrains.BRIDGE_ELEV) == currElevation)
                  && hex.containsTerrain(Terrains.BRIDGE)) {
                return true;
            }
            // Can't deploy on the surface of water
            if (hex.containsTerrain(Terrains.WATER) && (currElevation == 0)) {
                return true;
            }
        }

        return switch (movementMode) {
            case TRACKED -> (hex.terrainLevel(Terrains.WOODS) > 1)
                  || ((hex.terrainLevel(Terrains.WATER) > 0)
                  && !hex.containsTerrain(Terrains.ICE)
                  && !hasEnvironmentalSealing())
                  || hex.containsTerrain(Terrains.JUNGLE)
                  || (hex.terrainLevel(Terrains.MAGMA) > 1)
                  || (hex.terrainLevel(Terrains.ROUGH) > 1)
                  || (hex.terrainLevel(Terrains.RUBBLE) > 5);
            case WHEELED -> (hex.terrainLevel(Terrains.WOODS) > 1)
                  || ((hex.terrainLevel(Terrains.WOODS) == 1) && !isMonoCycleOrBicycle())
                  || hex.containsTerrain(Terrains.ROUGH)
                  || ((hex.terrainLevel(Terrains.WATER) > 0)
                  && !hex.containsTerrain(Terrains.ICE)
                  && !hasEnvironmentalSealing())
                  || hex.containsTerrain(Terrains.RUBBLE)
                  || hex.containsTerrain(Terrains.MAGMA)
                  || hex.containsTerrain(Terrains.JUNGLE)
                  || (hex.terrainLevel(Terrains.SNOW) > 1)
                  || (hex.terrainLevel(Terrains.GEYSER) == 2);
            case HOVER -> (hex.terrainLevel(Terrains.WOODS) > 1)
                  || ((hex.terrainLevel(Terrains.WOODS) == 1) && !isMonoCycleOrBicycle())
                  || hex.containsTerrain(Terrains.JUNGLE)
                  || (hex.terrainLevel(Terrains.MAGMA) > 1)
                  || (hex.terrainLevel(Terrains.ROUGH) > 1)
                  || (hex.terrainLevel(Terrains.RUBBLE) > 5);
            case NAVAL, HYDROFOIL -> (hex.terrainLevel(Terrains.WATER) <= 0)
                  || hex.containsTerrain(Terrains.ICE);
            case SUBMARINE -> (hex.terrainLevel(Terrains.WATER) <= 0);
            case WIGE -> (hex.containsTerrain(Terrains.WOODS) || (hex
                  .containsTerrain(Terrains.BUILDING)))
                  && !(currElevation > hex
                  .maxTerrainFeatureElevation(game.getBoard(testBoardId)
                        .isLowAltitude()));
            default -> false;
        };
    }

    public boolean isMonoCycleOrBicycle() {
        return hasWorkingMisc(MiscType.F_BICYCLE) || hasWorkingMisc(MiscType.F_MONOCYCLE);
    }

    @Override
    public int getTotalCommGearTons() {
        return getExtraCommGearTons();
    }

    // CONSTRUCTION INFORMATION
    //Support Vee Engine Information
    @Override
    public double getBaseChassisValue() {
        switch (movementMode) {
            case HOVER:
                if (getWeight() < 5) {
                    return 0.2;
                } else if (!isSuperHeavy()) {
                    return 0.25;
                } else {
                    return 0.3;
                }
            case NAVAL:
            case HYDROFOIL:
            case SUBMARINE:
                if (getWeight() < 5) {
                    return 0.12;
                } else {
                    return 0.15;
                }
            case TRACKED:
                if (getWeight() < 5) {
                    return 0.13;
                } else if (!isSuperHeavy()) {
                    return 0.15;
                } else {
                    return 0.25;
                }
            case WHEELED:
                if (getWeight() < 5) {
                    return 0.12;
                } else if (!isSuperHeavy()) {
                    return 0.15;
                } else {
                    return 0.18;
                }
            case WIGE:
                if (getWeight() < 5) {
                    return 0.12;
                } else if (!isSuperHeavy()) {
                    return 0.15;
                } else {
                    return 0.17;
                }
            default:
                return 0;
        }
    }

    @Override
    public double getBaseEngineValue() {
        switch (movementMode) {
            case AIRSHIP:
                if (getWeight() < 5) {
                    return 0.005;
                } else {
                    return 0.008;
                }
            case HOVER:
                if (getWeight() < 5) {
                    return 0.0025;
                } else if (!isSuperHeavy()) {
                    return 0.004;
                } else {
                    return 0.008;
                }
            case NAVAL:
            case HYDROFOIL:
            case SUBMARINE:
                if (getWeight() < 5) {
                    return 0.004;
                } else {
                    return 0.007;
                }
            case TRACKED:
                if (getWeight() < 5) {
                    return 0.006;
                } else if (!isSuperHeavy()) {
                    return 0.013;
                } else {
                    return 0.025;
                }
            case WHEELED:
                if (getWeight() < 5) {
                    return 0.0025;
                } else if (!isSuperHeavy()) {
                    return 0.0075;
                } else {
                    return 0.015;
                }
            case WIGE:
                if (getWeight() < 5) {
                    return 0.003;
                } else if (!isSuperHeavy()) {
                    return 0.005;
                } else {
                    return 0.006;
                }
            case RAIL:
            case MAGLEV:
                if (getWeight() < 5) {
                    return 0.003;
                } else if (!isSuperHeavy()) {
                    return 0.004;
                } else {
                    return 0.005;
                }
            default:
                return 0;
        }
    }

    @Override
    public double getFuelTonnage() {
        return fuelTonnage;
    }

    @Override
    public void setFuelTonnage(double fuel) {
        fuelTonnage = fuel;
    }

    @Override
    public int getTotalSlots() {
        return 5 + (int) Math.floor(getWeight() / 10);
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_TANK | Entity.ETYPE_SUPPORT_TANK;
    }

    @Override
    public boolean isSupportVehicle() {
        return true;
    }

    @Override
    public boolean isTractor() {
        return hasWorkingMisc(MiscType.F_TRACTOR_MODIFICATION);
    }

    @Override
    public boolean isTrailer() {
        return hasWorkingMisc(MiscType.F_TRAILER_MODIFICATION);
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(2.681 + 0.681 * Math.log(getWeight())));
    }

    @Override
    public int getRecoveryTime() {
        int weightClass = getWeightClass();
        if (weightClass == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            return 15;
        } else if (weightClass == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
            return 40;
        } else if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
            return 80;
        } else {
            return 100;
        }
    }
}
