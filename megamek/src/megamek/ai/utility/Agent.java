/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.ai.utility;

import megamek.client.bot.BotClient;
import megamek.client.bot.princess.Princess;
import megamek.common.IGame;

public interface Agent {

    // IAUS - Infinite Axis Utility System
    // Atomic actions
    // Each has one or many considerations (axis)
    // They describe why you would want to take this action
    // Considerations have Input
    // parameters
    // Curve type
    // - Linear
    // - Parabolic
    // - Logistic
    // - Logit
    // 4 parameter values
    // - m, k, b, c
    // Inputs are normalized
    // if not normalized we can clamp between 0-1

    // Infinite Axis utility system
    // Modular influence maps
    // Content tagging


    // Agent is just a character
    // They have properties
    // some are defined
    // some are calculated

    // Series of records
    // Each is a piece of data about the world as perceived by this agent
    // Each piece of data has a bunch of attributes

    // Attributes have name
    // They must come from somewhere - equations? agents?
    // Validations - its a range, tag, enumeration?
    //
    // Prefab equations - path finding, influence maps, distances, etc

    IGame getContext();
    Princess getCharacter();

}
