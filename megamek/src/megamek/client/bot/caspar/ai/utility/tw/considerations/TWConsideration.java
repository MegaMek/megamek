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

package megamek.client.bot.caspar.ai.utility.tw.considerations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import megamek.ai.utility.Consideration;
import megamek.ai.utility.Curve;
import megamek.common.Entity;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class TWConsideration extends Consideration<Entity, Entity> {

    public TWConsideration() {
    }

    @JsonIgnore
    public String getTitle() {
        return "TWConsideration." + this.getClass().getSimpleName() + ".title";
    }

    @JsonIgnore
    public String getDescription() {
        return "TWConsideration." + this.getClass().getSimpleName() + ".description";
    }

    public TWConsideration(String name) {
        super(name);
    }

    public TWConsideration(String name, Curve curve) {
        super(name, curve);
    }

    public TWConsideration(String name, Curve curve, Map<String, Object> parameters) {
        super(name, curve, parameters);
    }

    @Override
    public abstract TWConsideration copy();

    @Override
    public String toString() {
        return getName();
    }
}
