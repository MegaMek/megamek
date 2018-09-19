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
import java.util.EnumMap;
import java.util.Map;

import megamek.common.annotations.Nullable;

/**
 * Extracted from Entity
 * 
 * @author cwspain
 *
 */
public class EntityFluff implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -8018098140016149185L;
    
    public enum Component {
    	CHASSIS, ENGINE, ARMOR, JUMPJET, COMMUNICATIONS, TARGETING
    }
    
    private String capabilities = "";
    private String overview = "";
    private String deployment = "";
    private String history = "";

    private String manufacturer = "";
    private String primaryFactory = "";
    private Map<Component, String> componentManufacturers = new EnumMap<>(Component.class);
    private Map<Component, String> componentModels = new EnumMap<>(Component.class);
    
    private String mmlImageFilePath = "";

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
     * @param comp The system component
     * @return     The name of the manufacturer, or an empty string if it has not been set.
     */
    public String getComponentManufacturer(Component comp) {
    	return componentManufacturers.getOrDefault(comp, "");
    }
    
    /**
     * Sets the name of the manufacturer of a particular system component.
     * 
     * @param comp The system component
     * @param manu The name of the manufacturer, or {@code null} or an empty string to remove the entry.
     */
    public void setComponentManufacturer(Component comp, @Nullable String manu) {
    	if ((null != manu) && (manu.length() > 0)) {
    		componentManufacturers.put(comp, manu);
    	} else {
    		componentManufacturers.remove(comp);
    	}
    }
    
    /**
     * Retrieves the manufacturer of particular system component
     * 
     * @param comp The system component
     * @return     The name of the manufacturer, or an empty string if it has not been set.
     */
    public String getComponentModel(Component comp) {
    	return componentModels.getOrDefault(comp, "");
    }
    
    /**
     * Sets the model name of a particular system component.
     * 
     * @param comp  The system component
     * @param model The model name, or {@code null} or an empty string to remove the entry.
     */
    public void setComponentModel(Component comp, @Nullable String model) {
    	if ((null != model) && (model.length() > 0)) {
    		componentModels.put(comp, model);
    	} else {
    		componentModels.remove(comp);
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


}

