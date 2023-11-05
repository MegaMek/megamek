/*
 * MegaMek -
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import java.util.List;
import java.util.StringJoiner;

/**
 * Stats for beast mounted infantry units. See TO:AU&E, p. 106
 */
public class InfantryMount {
    public enum BeastSize {
        LARGE(1, 21, 0, 0, true, true, 0, "BeastSize.large"),
        VERY_LARGE(2, 7, -1, 2, true, false, 1, "BeastSize.very_large"),
        MONSTROUS(4, 2, -2, 3, false, false, 2, "BeastSize.monstrous");

        /** Maximum number of troopers that can be mounted on each beast. For values > 2,
         * each creature is a separate squad. */
        public final int troopsPerCreature;
        /** Maximum number of creatures allowed in a single platoon */
        public final int creaturesPerPlatoon;
        /** Modifer to attack rolls against the beast-mounted infantry due to size */
        public final int toHitMod;
        /** Maximum number of support weapons allowed per creature. Divide weapon crew needs by 2, rounding up */
        public final int supportWeaponsPerCreature;
        /** Whether the infantry unit is permitted to make anti-mech leg attacks */
        public final boolean canMakeLegAttacks;
        /** Whether the infantry unit is permitted to make anti-=mech swarm attacks */
        public final boolean canMakeSwarmAttacks;
        /** Additional MP required to enter a building hex. The building takes twice this much CF damage. */
        public final int buildingMP;
        private final String messageId;

        BeastSize(int troopsPerCreature, int creaturesPerPlatoon, int toHitMod,
                  int supportWeaponsPerCreature, boolean canMakeLegAttacks,
                  boolean canMakeSwarmAttacks, int buildingMP, String messageId) {
            this.troopsPerCreature = troopsPerCreature;
            this.creaturesPerPlatoon = creaturesPerPlatoon;
            this.toHitMod = toHitMod;
            this.supportWeaponsPerCreature = supportWeaponsPerCreature;
            this.canMakeLegAttacks = canMakeLegAttacks;
            this.canMakeSwarmAttacks = canMakeSwarmAttacks;
            this.buildingMP = buildingMP;
            this.messageId = messageId;
        }

        /**
         * @return The amount of CF damage done to a building when entering its hex.
         */
        public int buildingDamage() {
            return buildingMP * 2;
        }

        public String displayName() {
            return Messages.getString(messageId);
        }
    }

    private final String name;
    private final BeastSize size;
    private final double weight;
    private final int movementPoints;
    private final EntityMovementMode movementMode;
    private final int burstDamage;
    private final int vehicleDamage;
    private final double damageDivisor;
    private final int maxWaterDepth;
    private final int secondaryGroundMP;
    private final int uwEndurance;
    private final boolean custom;

    private InfantryMount(String name, BeastSize size, double weight, int movementPoints,
                         EntityMovementMode movementMode, int burstDamage,
                         int vehicleDamage, double damageDivisor, int maxWaterDepth,
                         int secondaryGroundMP, int uwEndurance, boolean custom) {
        this.name = name;
        this.size = size;
        this.weight = weight;
        this.movementPoints = movementPoints;
        this.movementMode = movementMode;
        this.burstDamage = burstDamage;
        this.vehicleDamage = vehicleDamage;
        this.damageDivisor = damageDivisor;
        this.maxWaterDepth = maxWaterDepth;
        this.secondaryGroundMP = secondaryGroundMP;
        this.uwEndurance = uwEndurance;
        this.custom = custom;
    }

    public InfantryMount(String name, BeastSize size, double weight, int movementPoints,
                          EntityMovementMode movementMode, int burstDamage,
                          int vehicleDamage, double damageDivisor, int maxWaterDepth,
                          int secondaryGroundMP, int uwEndurance) {
        this(name, size, weight, movementPoints, movementMode, burstDamage, vehicleDamage, damageDivisor,
                maxWaterDepth, secondaryGroundMP, uwEndurance, true);
    }


    /**
     * @return The name of the beast.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The size class of the beast.
     */
    public BeastSize getSize() {
        return size;
    }

    /**
     * @return The weight of each beast in tons. Add 2t per trooper to get total weight.
     */
    public double getWeight() {
        return weight;
    }

    /**
     * @return The number of movement points using the primary movement node.
     */
    public int getMP() {
        return movementPoints;
    }

    /**
     * @return The primary movement mode.
     */
    public EntityMovementMode getMovementMode() {
        return movementMode;
    }

    /**
     *
     * @return The number of damage dice to use as burst damage against conventional infantry
     *         in the same hex.
     */
    public int getBurstDamageDice() {
        return burstDamage;
    }

    /**
     * @return The amount of additonal damage done to vehicles in the same hex.
     */
    public int getVehicleDamage() {
        return vehicleDamage;
    }

    /**
     * @return The number used to divide any damage received in combat.
     */
    public double getDamageDivisor() {
        return damageDivisor;
    }

    /**
     * @return The maximum depth of water the unit may enter.
     */
    public int getMaxWaterDepth() {
        return maxWaterDepth;
    }

    /**
     * @return For units with a primary movement mode other than ground, this is
     *         the number of ground MP available.
     */
    public int getSecondaryGroundMP() {
        return secondaryGroundMP;
    }

    /**
     * @return For creatures with underwater movement, this is the number of turns they
     *         can stay underwater before needing to resurface.
     */
    public int getUWEndurance() {
        return uwEndurance;
    }

    @Override
    public String toString() {
        if (custom) {
            StringJoiner sj = new StringJoiner(",");
            sj.add(name).add(size.name()).add(String.valueOf(weight)).add(String.valueOf(movementPoints))
                    .add(movementMode.name()).add(String.valueOf(burstDamage)).add(String.valueOf(vehicleDamage))
                    .add(String.valueOf(damageDivisor)).add(String.valueOf(maxWaterDepth))
                    .add(String.valueOf(secondaryGroundMP)).add(String.valueOf(uwEndurance));
            return "Beast:Custom:" + sj;
        } else {
            return "Beast:" + name;
        }
    }

    public static InfantryMount parse(String str) {
        final String toParse = str.trim().replace("Beast:", "");
        if (toParse.startsWith("Custom:")) {
            // Provide some decent information about which field is causing the problem
            String[] fields = toParse.replace("Custom:", "").split(",");
            if (fields.length < 11) {
                throw new IllegalArgumentException("Infantry mount string " + str + " does not have enough fields.");
            }
            BeastSize size;
            try {
                size = BeastSize.valueOf(fields[1]);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Could not parse BeastSize " + fields[1]);
            }
            double weight;
            try {
                weight = Double.parseDouble(fields[2]);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Could not parse InfantryMount movementMode " + fields[4]);
            }
            EntityMovementMode mode;
            try {
                mode = EntityMovementMode.valueOf(fields[4]);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Could not parse InfantryMount movementMode " + fields[4]);
            }
            double divisor;
            try {
                divisor = Double.parseDouble(fields[7]);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Could not parse InfantryMount damageDivisor " + fields[7]);
            }
            return new InfantryMount(fields[0], size, weight, parseIntField(fields[3], "movementPoints"),
                    mode, parseIntField(fields[5], "burstDamage"), parseIntField(fields[6], "vehicleDamage"),
                    divisor, parseIntField(fields[8], "maxWaterDepth"), parseIntField(fields[9], "secondaryGroundMP"),
                    parseIntField(fields[10], "uwEndurance"));
        } else {
            return sampleMounts.stream().filter(it -> it.name.equals(toParse)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Could not parse beast mount " + toParse));
        }
    }

    private static int parseIntField(String field, String fieldName) {
        try {
            return Integer.parseInt(field);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not parse InfantryMount field " + fieldName + " value " + field);
        }
    }

    public static final InfantryMount DONKEY = new InfantryMount("Donkey", BeastSize.LARGE,
            0.15, 2, EntityMovementMode.INF_LEG, 0, 0, 1.0,
            0, 0, 0, false);

    public static final InfantryMount COVENTRY_KANGAROO = new InfantryMount("Coventry Kangaroo", BeastSize.LARGE,
            0.11, 3, EntityMovementMode.INF_LEG, 1, 1, 1.0,
            0, 0, 0, false);

    public static final InfantryMount HORSE = new InfantryMount("Horse", BeastSize.LARGE,
            0.5, 3, EntityMovementMode.INF_LEG, 0, 0, 1.0,
            0, 0, 0, false);

    public static final InfantryMount CAMEL = new InfantryMount("Camel", BeastSize.LARGE,
            0.65, 2, EntityMovementMode.INF_LEG, 0, 0, 1.0,
            0, 0, 0, false);

    public static final InfantryMount BRANTH = new InfantryMount("Branth", BeastSize.LARGE,
            0.72, 6, EntityMovementMode.VTOL, 2, 1, 1.0,
            0, 0, 0, false);

    public static final InfantryMount ODESSAN_RAXX = new InfantryMount("Odessan Raxx", BeastSize.LARGE,
            2.4, 2, EntityMovementMode.INF_LEG, 1, 1, 1.0,
            0, 0, 0, false);

    public static final InfantryMount TABIRANTH = new InfantryMount("Tabiranth", BeastSize.LARGE,
            0.25, 2, EntityMovementMode.INF_LEG, 1, 1, 1.0,
            0, 0, 0, false);

    public static final InfantryMount TARIQ = new InfantryMount("Tariq", BeastSize.LARGE,
            0.51, 5, EntityMovementMode.INF_LEG, 0, 0, 1.0,
            0, 0, 0, false);

    public static final InfantryMount ELEPHANT = new InfantryMount("Elephant", BeastSize.VERY_LARGE,
            6.0, 2, EntityMovementMode.INF_LEG, 1, 1, 2.0,
            1, 0, 0, false);

    public static final InfantryMount ORCA = new InfantryMount("Orca", BeastSize.VERY_LARGE,
            7.2, 5, EntityMovementMode.SUBMARINE, 2, 1, 2.0,
            Integer.MAX_VALUE, 0, 180, false);

    public static final InfantryMount HIPPOSAUR = new InfantryMount("Hipposaur", BeastSize.MONSTROUS,
            35.5, 2, EntityMovementMode.SUBMARINE, 10, 4, 4.0,
            Integer.MAX_VALUE, 1, 2, false);

    public static final List<InfantryMount> sampleMounts = List.of(DONKEY, COVENTRY_KANGAROO, HORSE, CAMEL, BRANTH,
            ODESSAN_RAXX, TABIRANTH, TARIQ, ELEPHANT, ORCA, HIPPOSAUR);
}
