/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import java.awt.Image;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import megamek.client.ui.Base64Image;
import megamek.common.annotations.Nullable;

/**
 * Tracks fluff details for entities.
 *
 * @author Neoancient
 */
public class EntityFluff implements Serializable {
    private static final long serialVersionUID = -8018098140016149185L;

    public enum System {
        CHASSIS, ENGINE, ARMOR, JUMPJET, COMMUNICATIONS, TARGETING;

        public static @Nullable System parse(String string) {
            if (null != string) {
                for (final System c : values()) {
                    if (c.toString().equals(string.toUpperCase())) {
                        return c;
                    }
                }
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
    private String notes = "";

    private Base64Image fluffImage = new Base64Image();

    // For aerospace vessels
    private String use = "";
    private String length = "";
    private String width = "";
    private String height = "";

    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String newCapabilities) {
        capabilities = Objects.requireNonNullElse(newCapabilities, "");
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String newOverview) {
        overview = Objects.requireNonNullElse(newOverview, "");
    }

    public String getDeployment() {
        return deployment;
    }

    public void setDeployment(String newDeployment) {
        deployment = Objects.requireNonNullElse(newDeployment, "");
    }

    public void setHistory(String newHistory) {
        history = Objects.requireNonNullElse(newHistory, "");
    }

    public String getHistory() {
        return history;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = Objects.requireNonNullElse(manufacturer, "");
    }

    public String getPrimaryFactory() {
        return primaryFactory;
    }

    public void setPrimaryFactory(String primaryFactory) {
        this.primaryFactory = Objects.requireNonNullElse(primaryFactory, "");
    }

    /**
     * Retrieves the manufacturer of particular system component
     *
     * @param system The system component
     *
     * @return The name of the manufacturer, or an empty string if it has not been set.
     */
    public String getSystemManufacturer(System system) {
        return systemManufacturers.getOrDefault(system, "");
    }

    /**
     * Sets the name of the manufacturer of a particular system component.
     *
     * @param system       The system component
     * @param manufacturer The name of the manufacturer, or {@code null} or an empty string to remove the entry.
     */
    public void setSystemManufacturer(System system, @Nullable String manufacturer) {
        if ((null != manufacturer) && !manufacturer.isBlank()) {
            systemManufacturers.put(system, manufacturer);
        } else {
            systemManufacturers.remove(system);
        }
    }

    /**
     * Retrieves the manufacturer of particular system component
     *
     * @param system The system component
     *
     * @return The name of the manufacturer, or an empty string if it has not been set.
     */
    public String getSystemModel(System system) {
        return systemModels.getOrDefault(system, "");
    }

    /**
     * Sets the model name of a particular system component.
     *
     * @param system The system component
     * @param model  The model name, or {@code null} or an empty string to remove the entry.
     */
    public void setSystemModel(System system, @Nullable String model) {
        if ((null != model) && !model.isBlank()) {
            systemModels.put(system, model);
        } else {
            systemModels.remove(system);
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = Objects.requireNonNullElse(notes, "");
    }

    public String getUse() {
        return use;
    }

    public void setUse(String use) {
        this.use = Objects.requireNonNullElse(use, "");
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = Objects.requireNonNullElse(length, "");
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = Objects.requireNonNullElse(width, "");
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = Objects.requireNonNullElse(height, "");
    }

    /**
     * Used for writing the system manufacturers to a unit file
     *
     * @return A list of all system manufacturers formatted as "system:manufacturer"
     */
    public List<String> createSystemManufacturersList() {
        return systemManufacturers.entrySet().stream().filter(e -> !e.getValue().isBlank())
              .map(e -> e.getKey().toString() + ":" + e.getValue()).collect(Collectors.toList());
    }

    /**
     * Used for writing the system models to a unit file
     *
     * @return A list of all system models formatted as "system:model"
     */
    public List<String> createSystemModelsList() {
        return systemModels.entrySet().stream().filter(e -> !e.getValue().isBlank())
              .map(e -> e.getKey().toString() + ":" + e.getValue()).collect(Collectors.toList());
    }

    /** Sets the encoded form of the fluff image to the given String. */
    public void setFluffImage(String fluffImage64) {
        fluffImage = new Base64Image(fluffImage64);
    }

    /** @return The unit's fluff image, if a fluff image was stored in the unit file; null otherwise. */
    public @Nullable Image getFluffImage() {
        return fluffImage.getImage();
    }

    /** @return True if a fluff image is part of the unit, i.e. stored in the unit file or set in MML. */
    public boolean hasEmbeddedFluffImage() {
        return !fluffImage.isEmpty();
    }

    /** @return The Base64Image holding an embedded fluff image. Empty if no fluff image was stored in the unit file. */
    public Base64Image getBase64FluffImage() {
        return fluffImage;
    }
}
