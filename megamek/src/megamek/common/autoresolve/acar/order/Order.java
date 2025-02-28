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

package megamek.common.autoresolve.acar.order;

import megamek.common.autoresolve.acar.SimulationContext;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * An Order is a
 *
 * @author Luana Coppio
 */
public class Order {
    private final int ownerId;
    private final int priority;
    private final OrderType orderType;
    private final int targetId;
    private final int formationId;
    private final int orderDelay;
    private final Condition condition;
    private boolean concluded = false;

    private transient boolean eligible;
    private transient int lastRoundEligible;

    private Order(int ownerId, int priority, OrderType orderType, int targetId, int formationId, int orderDelay, Condition condition) {
        this.ownerId = ownerId;
        this.priority = priority;
        this.orderType = orderType;
        this.targetId = targetId;
        this.formationId = formationId;
        this.orderDelay = orderDelay;
        this.condition = condition;
        this.eligible = false;
        this.lastRoundEligible = -1;
    }

    public void reset() {
        this.eligible = false;
        this.lastRoundEligible = -1;
        this.concluded = false;
    }

    public boolean isEligible(SimulationContext context) {
        if (context.getCurrentRound() != lastRoundEligible) {
            eligible = getCondition().isMet(context);
            lastRoundEligible = context.getCurrentRound();
        }

        return !isConcluded() && (context.getCurrentRound() - getOrderDelay() >= 0) && eligible;
    }

    public boolean isConcluded() {
        return concluded;
    }

    public void setConcluded(boolean concluded) {
        this.concluded = concluded;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public int getPriority() {
        return priority;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public int getTargetId() {
        return targetId;
    }

    public int getFormationId() {
        return formationId;
    }

    public int getOrderDelay() {
        return orderDelay;
    }

    public Condition getCondition() {
        return condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Order order)) return false;

        return new EqualsBuilder()
            .append(getOwnerId(), order.getOwnerId())
            .append(getPriority(), order.getPriority())
            .append(getTargetId(), order.getTargetId())
            .append(getFormationId(), order.getFormationId())
            .append(getOrderDelay(), order.getOrderDelay())
            .append(getOrderType(), order.getOrderType())
            .append(getCondition(), order.getCondition())
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(getOwnerId()).append(getPriority()).append(getOrderType()).append(getTargetId())
            .append(getFormationId()).append(getOrderDelay()).append(getCondition()).toHashCode();
    }


    public static final class OrderBuilder {
        private final int ownerId;
        private int priority = 0;
        private final OrderType orderType;
        private int targetId = -1;
        private int formationId = -1;
        private int orderDelay = -1;
        private Condition condition = Condition.alwaysTrue();

        public OrderBuilder(int ownerId, OrderType orderType) {
            this.orderType = orderType;
            this.ownerId = ownerId;
        }

        public OrderBuilder(Order other) {
            this.ownerId = other.ownerId;
            this.priority = other.priority;
            this.orderType = other.orderType;
            this.targetId = other.targetId;
            this.formationId = other.formationId;
            this.orderDelay = other.orderDelay;
            this.condition = other.condition;
        }

        public static OrderBuilder anOrder(int ownerId, OrderType orderType) {
            return new OrderBuilder(ownerId, orderType);
        }

        public OrderBuilder withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public OrderBuilder withTargetId(int targetId) {
            this.targetId = targetId;
            return this;
        }

        public OrderBuilder withFormationId(int formationId) {
            this.formationId = formationId;
            return this;
        }

        public OrderBuilder withOrderDelay(int orderDelay) {
            this.orderDelay = orderDelay;
            return this;
        }

        public OrderBuilder withCondition(Condition condition) {
            this.condition = condition;
            return this;
        }

        public OrderBuilder but() {
            return anOrder(ownerId, orderType)
                    .withPriority(priority)
                    .withTargetId(targetId)
                    .withFormationId(formationId)
                    .withOrderDelay(orderDelay)
                    .withCondition(condition);
        }

        public Order build() {
            return new Order(ownerId, priority, orderType, targetId, formationId, orderDelay, condition);
        }
    }
}
