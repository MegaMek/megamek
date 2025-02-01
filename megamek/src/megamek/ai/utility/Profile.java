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

package megamek.ai.utility;

import com.fasterxml.jackson.annotation.*;
import megamek.client.bot.caspar.ai.utility.tw.profile.TWProfile;

import java.util.List;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TWProfile.class, name = "TWProfile"),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Profile <A,B> implements NamedObject {
    @JsonProperty("id")
    private final int id;
    @JsonProperty("name")
    private final String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("decisions")
    private final List<Decision<A, B>> decisions;

    @JsonCreator
    public Profile(int id, String name, String description, List<Decision<A, B>> decisions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.decisions = decisions;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public List<Decision<A, B>> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<Decision<A, B>> decisions) {
        this.decisions.clear();
        this.decisions.addAll(decisions);
    }
}
