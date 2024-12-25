/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.client.bot.duchess.ai.utility.tw.profile;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import megamek.ai.utility.DecisionScoreEvaluator;
import megamek.ai.utility.Profile;
import megamek.common.Entity;

import java.util.List;

@JsonTypeName("TWProfile")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TWProfile extends Profile<Entity, Entity> {
    @JsonCreator
    public TWProfile(
        @JsonProperty("id") int id,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("decisionScoreEvaluator") List<DecisionScoreEvaluator<Entity, Entity>> decisionScoreEvaluator)
    {
        super(id, name, description, decisionScoreEvaluator);
    }
}
