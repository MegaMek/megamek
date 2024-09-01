/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.jacksonadapters;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSetter;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.PrincessException;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.*;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
public class PrincessSettingsBuilder {

    private static final MMLogger LOGGER = MMLogger.create(PrincessSettingsBuilder.class);

    private static final String PRINCESS_SELF_PRESERVATION = "selfpreservation";
    private static final String PRINCESS_FALL_SHAME = "fallshame";
    private static final String PRINCESS_AGGRESSION = "hyperaggression";
    private static final String PRINCESS_HERDING = "herdmentality";
    private static final String PRINCESS_DESTINATION = "destination";
    private static final String PRINCESS_RETREAT = "retreat";
    private static final String PRINCESS_FLEE = "flee";
    private static final String PRINCESS_FORCED_WITHDRAW = "forcedwithdraw";

    @JsonAlias(PRINCESS_SELF_PRESERVATION)
    private int selfPreservation = -1;

    @JsonAlias(PRINCESS_FALL_SHAME)
    private int fallShame = -1;

    @JsonAlias(PRINCESS_AGGRESSION)
    private int hyperAgression = -1;

    @JsonAlias(PRINCESS_HERDING)
    private int herdMentality = -1;

    private int bravery = -1;

    private boolean hasFleeValue = false;

    @JsonAlias(PRINCESS_FLEE)
    private boolean doFlee;

    @JsonAlias(PRINCESS_DESTINATION)
    private CardinalEdge fleeDestinationEdge;

    private boolean hasForcedWithdrawValue = false;

    @JsonAlias(PRINCESS_FORCED_WITHDRAW)
    private boolean useForcedWithdraw;

    @JsonAlias(PRINCESS_RETREAT)
    private CardinalEdge withdrawEdge;

    private String description = null;

    public PrincessSettingsBuilder selfPreservation(int selfPreservation) {
        this.selfPreservation = selfPreservation;
        return this;
    }

    public PrincessSettingsBuilder fallShame(int fallShame) {
        this.fallShame = fallShame;
        return this;
    }

    public PrincessSettingsBuilder hyperAgression(int hyperAgression) {
        this.hyperAgression = hyperAgression;
        return this;
    }

    public PrincessSettingsBuilder herdMentality(int herdMentality) {
        this.herdMentality = herdMentality;
        return this;
    }

    public PrincessSettingsBuilder bravery(int bravery) {
        this.bravery = bravery;
        return this;
    }

    @JsonSetter("doFlee")
    public PrincessSettingsBuilder flee(boolean flee) {
        doFlee = flee;
        hasFleeValue = true;
        return this;
    }

    @JsonSetter("useForcedWithdraw")
    public PrincessSettingsBuilder useWithdraw(boolean useWithdraw) {
        useForcedWithdraw = useWithdraw;
        hasForcedWithdrawValue = true;
        return this;
    }

    public PrincessSettingsBuilder description(String description) {
        if ((description == null) || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        this.description = description;
        return this;
    }

    /**
     * Returns new BehaviorSettings based on the given settings. Settings that are present in this builder
     * overwrite the previous settings, others are untouched.
     *
     * @param previousSettings Settings to base the new settings on
     * @return New BehaviorSettings that incorporate the settings of this builder
     */
    public BehaviorSettings build(@Nullable BehaviorSettings previousSettings) {
        BehaviorSettings settings = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR;
        if (previousSettings != null) {
            try {
                settings = previousSettings.getCopy();
            } catch (PrincessException ex) {
                LOGGER.error("Could not obtain copy of given princess settings", ex);
                // stay with the default settings
            }
        }
        if (selfPreservation != -1) {
            settings.setSelfPreservationIndex(selfPreservation);
        }
        if (fallShame != -1) {
            settings.setFallShameIndex(fallShame);
        }
        if (hyperAgression != -1) {
            settings.setHyperAggressionIndex(hyperAgression);
        }
        if (herdMentality != -1) {
            settings.setHerdMentalityIndex(herdMentality);
        }
        if (bravery != -1) {
            settings.setBraveryIndex(bravery);
        }
        if (hasFleeValue) {
            settings.setAutoFlee(doFlee);
        }
        if (fleeDestinationEdge != null) {
            settings.setDestinationEdge(fleeDestinationEdge);
        }
        if (hasForcedWithdrawValue) {
            settings.setForcedWithdrawal(useForcedWithdraw);
        }
        if (withdrawEdge != null) {
            settings.setRetreatEdge(withdrawEdge);
        }

        if ((description == null) || description.isBlank()) {
            description = "(No description)";
        }
        try {
            settings.setDescription(description);
        } catch (PrincessException ex) {
            // ignore, description has been made sure to not be empty
        }

        return settings;
    }

    /**
     * Returns new BehaviorSettings based on the Princess default settings. Settings that are present in this
     * builder overwrite the default settings, others are untouched.
     *
     * @return New BehaviorSettings that incorporate the settings of this builder
     */
    public BehaviorSettings build() {
        return build(null);
    }
}
