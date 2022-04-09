/*
 * MegaMek - Copyright (C) 2018 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.common.annotations.Nullable;

/**
 * Tracks fluff details for entities.
 *
 * @author Neoancient
 *
 */
public class EntityFluff implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -8018098140016149185L;

    public enum System {
        CHASSIS, ENGINE, ARMOR, JUMPJET, COMMUNICATIONS, TARGETING;

        public static @Nullable System parse(String string) {
            if (null != string) {
                return Arrays.stream(values()).filter(c -> c.toString().equals(string.toUpperCase())).findFirst().orElse(null);
            }
            return null;
        }
    }

    private String capabilities = "";
    private String overview = "";
    private String deployment = "";
    private String history = "";

    private String manufacturer = "";
    private String primaryFactory = "";
    private final Map<System, String> systemManufacturers = new EnumMap<>(System.class);
    private final Map<System, String> systemModels = new EnumMap<>(System.class);

    private String mmlImageFilePath = "";
    private String notes = "";

    // For aerospace vessels
    private String use = "";
    private String length = "";
    private String width = "";
    private String height = "";

    public EntityFluff() {
        // Constructor
    }

    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String newCapabilities) {
        if (null != newCapabilities) {
            capabilities = newCapabilities;
        } else {
            capabilities = "";
        }
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String newOverview) {
        if (null != newOverview) {
            overview = newOverview;
        } else {
            overview = "";
        }
    }

    public String getDeployment() {
        return deployment;
    }

    public void setDeployment(String newDeployment) {
        if (null != newDeployment) {
            deployment = newDeployment;
        } else {
            deployment = "";
        }
    }

    public void setHistory(String newHistory) {
        if (null != newHistory) {
            history = newHistory;
        } else {
            history = "";
        }
    }

    public String getHistory() {
        return history;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        if (null != manufacturer) {
            this.manufacturer = manufacturer;
        } else {
            this.manufacturer = "";
        }
    }

    public String getPrimaryFactory() {
        return primaryFactory;
    }

    public void setPrimaryFactory(String primaryFactory) {
        if (null != primaryFactory) {
            this.primaryFactory = primaryFactory;
        } else {
            this.primaryFactory = "";
        }
    }

    /**
     * Retrieves the manufacturer of particular system component
     *
     * @param system
     *            The system component
     * @return The name of the manufacturer, or an empty string if it has not been
     *         set.
     */
    public String getSystemManufacturer(System system) {
        return systemManufacturers.getOrDefault(system, "");
    }

    /**
     * Sets the name of the manufacturer of a particular system component.
     *
     * @param system
     *            The system component
     * @param manu
     *            The name of the manufacturer, or {@code null} or an empty string
     *            to remove the entry.
     */
    public void setSystemManufacturer(System system, @Nullable String manu) {
        if ((null != manu) && (!manu.isEmpty())) {
            systemManufacturers.put(system, manu);
        } else {
            systemManufacturers.remove(system);
        }
    }

    /**
     * Retrieves the manufacturer of particular system component
     *
     * @param system
     *            The system component
     * @return The name of the manufacturer, or an empty string if it has not been
     *         set.
     */
    public String getSystemModel(System system) {
        return systemModels.getOrDefault(system, "");
    }

    /**
     * Sets the model name of a particular system component.
     *
     * @param system
     *            The system component
     * @param model
     *            The model name, or {@code null} or an empty string to remove the
     *            entry.
     */
    public void setSystemModel(System system, @Nullable String model) {
        if ((null != model) && (!model.isEmpty())) {
            systemModels.put(system, model);
        } else {
            systemModels.remove(system);
        }
    }

    public String getMMLImagePath() {
        return mmlImageFilePath;
    }

    public void setMMLImagePath(String filePath) {
        if (null != filePath) {
            mmlImageFilePath = filePath;
        } else {
            mmlImageFilePath = "";
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        if (null != notes) {
            this.notes = notes;
        } else {
            this.notes = "";
        }
    }

    public String getUse() {
        return use;
    }

    public void setUse(String use) {
        if (null != use) {
            this.use = use;
        } else {
            this.use = "";
        }
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        if (null != length) {
            this.length = length;
        } else {
            this.length = "";
        }
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        if (null != width) {
            this.width = width;
        } else {
            this.width = "";
        }
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        if (null != height) {
            this.height = height;
        } else {
            this.height = "";
        }
    }

    /**
     * Used for writing the system manufacturers to a unit file
     *
     * @return A list of all system manufacturers formatted as "system:manufacturer"
     */
    public List<String> createSystemManufacturersList() {
        return systemManufacturers.entrySet().stream().filter(e -> !e.getValue().isEmpty())
                .map(e -> e.getKey().toString() + ':' + e.getValue()).collect(Collectors.toList());
    }

    /**
     * Used for writing the system models to a unit file
     *
     * @return A list of all system models formatted as "system:model"
     */
    public List<String> createSystemModelsList() {
        return systemModels.entrySet().stream().filter(e -> !e.getValue().isEmpty())
                .map(e -> e.getKey().toString() + ":" + e.getValue()).collect(Collectors.toList());
    }
}
