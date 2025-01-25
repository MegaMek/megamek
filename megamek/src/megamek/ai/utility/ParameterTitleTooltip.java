/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */

package megamek.ai.utility;

import megamek.client.ui.Messages;

/**
 * This class is used to dynamically load the localized title and tooltip for a Consideration parameter.
 * It will look for the keys {@code "Consideration." + messageKey + ".title"} and {@code }"Consideration." + messageKey + ".tooltip"}
 * Load tem on construction and keep it in memory.
 * @author Luana Coppio
 */
public class ParameterTitleTooltip {
    private final String messageKey;
    private final static String prefix = "Consideration.param.";
    private final static String titleSuffix = ".title";
    private final static String tooltipSuffix = ".tooltip";
    /**
     * Constructor for the ParameterTitleTooltip class.
     * @param messageKey the main part of the key of the message to be loaded.
     */
    public ParameterTitleTooltip(String messageKey) {
        if (messageKey == null || messageKey.isBlank()) {
            throw new IllegalArgumentException("Message key cannot be null or blank");
        }
        var title = prefix + messageKey + titleSuffix;
        if (Messages.getString(title).equals("!" + title + "!")) {
            throw new IllegalArgumentException("Title message key not found: " + title);
        }
        var toolTip = prefix + messageKey + tooltipSuffix;
        if (Messages.getString(toolTip).equals("!" + toolTip +"!")) {
            throw new IllegalArgumentException("Tooltip message key not found: " + toolTip);
        }
        this.messageKey = messageKey;
    }

    public String getTitle() {
        return Messages.getString(prefix + messageKey + titleSuffix);
    }

    public String getTooltip() {
        return Messages.getString(prefix + messageKey + tooltipSuffix);
    }
}
