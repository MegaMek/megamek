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

package megamek.common.equipment;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import megamek.client.ui.Messages;
import megamek.common.units.Entity;

/**
 * Groups the {@link Sensor} types into the families a player actually chooses between.
 *
 * <p>A unit never carries both the Mek and the vehicle version of a sensor, so ranking "Mek Radar" separately from
 * "Vehicle Radar" expresses nothing a player can act on. Each family below collects every {@link Sensor} type that
 * means the same thing to the player, which lets the sensor preference in the client settings be a list of nine rows
 * rather than one row per {@link Sensor} type.</p>
 *
 * <p>The families cover every {@link Sensor} type exactly once. {@link #familyOf(int)} therefore always resolves, and
 * a test asserts the partition so a newly added sensor type cannot be silently left out of the preference.</p>
 */
public enum SensorFamily {

    /**
     * Active probes of every flavour, including Nova CEWS, Electronic Warfare Equipment and the Enhanced Imaging
     * probe. These are the types {@link Sensor#isBAP()} reports as a probe.
     */
    ACTIVE_PROBE("SensorFamily.activeProbe",
          Sensor.TYPE_BAP,
          Sensor.TYPE_BAPP,
          Sensor.TYPE_CLAN_AP,
          Sensor.TYPE_LIGHT_AP,
          Sensor.TYPE_BLOODHOUND,
          Sensor.TYPE_WATCHDOG,
          Sensor.TYPE_NOVA,
          Sensor.TYPE_EW_EQUIPMENT,
          Sensor.TYPE_EI_PROBE),

    /** Infrared sensors. */
    INFRARED("SensorFamily.infrared",
          Sensor.TYPE_MEK_IR,
          Sensor.TYPE_VEE_IR),

    /** Magnetic anomaly detection. */
    MAGSCAN("SensorFamily.magscan",
          Sensor.TYPE_MEK_MAG_SCAN,
          Sensor.TYPE_VEE_MAG_SCAN),

    /** Seismic sensors. */
    SEISMIC("SensorFamily.seismic",
          Sensor.TYPE_MEK_SEISMIC,
          Sensor.TYPE_VEE_SEISMIC),

    /**
     * Active radar, including the combined aero sensor suite and spacecraft radar. The aero suite belongs here
     * because it is the active option a fighter chooses instead of its passive thermal/optical sensors.
     */
    RADAR("SensorFamily.radar",
          Sensor.TYPE_MEK_RADAR,
          Sensor.TYPE_VEE_RADAR,
          Sensor.TYPE_AERO_SENSOR,
          Sensor.TYPE_SPACECRAFT_RADAR),

    /** Passive thermal and optical sensors carried by aerospace units and spacecraft. */
    THERMAL_OPTICAL("SensorFamily.thermalOptical",
          Sensor.TYPE_AERO_THERMAL,
          Sensor.TYPE_SPACECRAFT_THERMAL),

    /** Spacecraft Electronic Support Measures. */
    ESM("SensorFamily.esm",
          Sensor.TYPE_SPACECRAFT_ESM),

    /** Battle armor heat sensors. */
    HEAT_SENSORS("SensorFamily.heatSensors",
          Sensor.TYPE_BA_HEAT),

    /** Battle armor improved sensors. */
    IMPROVED_SENSORS("SensorFamily.improvedSensors",
          Sensor.TYPE_BA_IMPROVED);

    /**
     * The family each {@link Sensor} type belongs to, indexed by sensor type. Built once at class initialization,
     * which also fails fast if the enum above ever stops covering every sensor type.
     */
    private static final SensorFamily[] FAMILY_BY_SENSOR_TYPE = buildFamilyBySensorType();

    private final String displayNameKey;
    private final Set<Integer> sensorTypes;

    SensorFamily(String displayNameKey, Integer... sensorTypes) {
        this.displayNameKey = displayNameKey;
        this.sensorTypes = Set.of(sensorTypes);
    }

    /**
     * @return the localized name shown to the player, for example "Active Probe"
     */
    public String getDisplayName() {
        return Messages.getString(displayNameKey);
    }

    /**
     * @param sensorType a {@link Sensor} type constant
     *
     * @return {@code true} when this family covers that sensor type
     */
    public boolean covers(int sensorType) {
        return sensorTypes.contains(sensorType);
    }

    /**
     * Returns the family a sensor type belongs to. Every valid sensor type belongs to exactly one family.
     *
     * @param sensorType a {@link Sensor} type constant
     *
     * @return the family covering that sensor type
     *
     * @throws IllegalArgumentException when the sensor type is not a valid {@link Sensor} type
     */
    public static SensorFamily familyOf(int sensorType) {
        if ((sensorType < 0) || (sensorType >= Sensor.SIZE)) {
            throw new IllegalArgumentException("Unknown sensor type " + sensorType);
        }
        return FAMILY_BY_SENSOR_TYPE[sensorType];
    }

    /**
     * Returns the family a sensor belongs to.
     *
     * @param sensor the sensor to classify
     *
     * @return the family covering that sensor
     */
    public static SensorFamily familyOf(Sensor sensor) {
        return familyOf(sensor.type());
    }

    /**
     * The order the families are offered in before the player reorders them. Active probes come first because they
     * are the strongest sensor a unit can carry and were already MegaMek's automatic choice. Radar sits below the
     * short-ranged specialist sensors because it is the weakest of the four a Mek or vehicle carries, which is the
     * complaint that prompted this preference.
     *
     * @return the default preference order, most preferred first
     */
    public static List<SensorFamily> defaultOrder() {
        return List.of(ACTIVE_PROBE,
              INFRARED,
              MAGSCAN,
              SEISMIC,
              RADAR,
              THERMAL_OPTICAL,
              ESM,
              HEAT_SENSORS,
              IMPROVED_SENSORS);
    }

    /**
     * Finds the sensor a unit should use under the given preference order.
     *
     * <p>Example: the order runs Active Probe, Infrared, Magscan, Seismic, Radar. A Marauder MAD-3R carries Mek
     * Radar, Mek IR, Mek Magscan and Mek Seismic and no probe, so this returns the index of Mek IR. Ranking Radar
     * first instead would return the index of Mek Radar, which is what MegaMek did before the preference existed.</p>
     *
     * <p>An active probe the unit cannot currently use, because it is destroyed, switched off or jammed, is skipped
     * rather than chosen. The unit would fall back off it anyway, so a lower family wins instead.</p>
     *
     * @param entity          the unit to pick a sensor for
     * @param preferenceOrder the sensor families, most preferred first
     *
     * @return the index into {@link Entity#getSensors()} of the sensor to use, or -1 when the unit has no choice to
     *       make or already uses the preferred sensor
     */
    public static int preferredSensorIndex(Entity entity, List<SensorFamily> preferenceOrder) {
        Vector<Sensor> sensors = entity.getSensors();
        if (sensors.size() < 2) {
            return -1;
        }

        // One pass over the unit's sensors records where each family is found, so the preference list below is a
        // single walk rather than a scan of the sensors per family.
        Map<SensorFamily, Integer> sensorIndexByFamily = new EnumMap<>(SensorFamily.class);
        for (int sensorIndex = 0; sensorIndex < sensors.size(); sensorIndex++) {
            Sensor sensor = sensors.elementAt(sensorIndex);
            if (sensor.isBAP() && !entity.hasBAP(false)) {
                continue;
            }
            sensorIndexByFamily.putIfAbsent(familyOf(sensor), sensorIndex);
        }

        for (SensorFamily family : preferenceOrder) {
            Integer sensorIndex = sensorIndexByFamily.get(family);
            if (sensorIndex == null) {
                continue;
            }
            Sensor currentSensor = entity.getNextSensor();
            Sensor preferredSensor = sensors.elementAt(sensorIndex);
            boolean alreadySet = (currentSensor != null) && (currentSensor.type() == preferredSensor.type());
            return alreadySet ? -1 : sensorIndex;
        }
        return -1;
    }

    private static SensorFamily[] buildFamilyBySensorType() {
        SensorFamily[] families = new SensorFamily[Sensor.SIZE];
        for (SensorFamily family : values()) {
            for (int sensorType : family.sensorTypes) {
                if (families[sensorType] != null) {
                    throw new IllegalStateException("Sensor type " + sensorType + " is claimed by both "
                          + families[sensorType] + " and " + family);
                }
                families[sensorType] = family;
            }
        }
        for (int sensorType = 0; sensorType < families.length; sensorType++) {
            if (families[sensorType] == null) {
                throw new IllegalStateException("Sensor type " + sensorType + " belongs to no SensorFamily");
            }
        }
        return families;
    }

    /**
     * @return an unmodifiable view of the sensor types this family covers, for testing
     */
    Set<Integer> sensorTypes() {
        return Collections.unmodifiableSet(sensorTypes);
    }

    /**
     * @return every sensor type covered by any family, for testing
     */
    static List<Integer> allCoveredSensorTypes() {
        return Arrays.stream(values())
              .flatMap(family -> family.sensorTypes.stream())
              .sorted()
              .toList();
    }
}
