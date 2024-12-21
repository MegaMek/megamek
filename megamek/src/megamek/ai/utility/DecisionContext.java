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

import megamek.common.Entity;
import megamek.common.IGame;

import java.util.Optional;

public  class DecisionContext {

    private final Agent agent;
    private final IGame worldState;

    public DecisionContext(Agent agent, IGame game) {
        this.agent = agent;
        this.worldState = game;
    }

    public IGame getWorldState() {
        return worldState;
    }

    public Agent getAgent() {
        return agent;
    }

    public Optional<Entity> getTarget() {
        // TODO implement this correctly
        return agent.getCharacter().getEnemyEntities().stream().findAny();
    }

    public Optional<Entity> getFiringUnit() {
        // TODO implement this correctly
        return Optional.ofNullable(agent.getCharacter().getEntityToFire(agent.getCharacter().getFireControlState()));
    }

    public Optional<Entity> getCurrentUnit() {
        // TODO implement this correctly
        return getFiringUnit();
    }

    // Decision Identifier - enum of what you are trying to do?
    // Link to intelligence controller object - who is asking?
    // Link to content data with parameters - what do you need?

    /*
     * Example input - MyHealth
     * class ConsiderationMyHealth extends Consideration {
     * public float Score(DecisionContext context) {
     *     var intelligence = context.getIntelligence();
     *     var character = intelligence.getCharacter();
     *     return character.getHealth() / character.getMaxHealth();
     * }
     */

}
