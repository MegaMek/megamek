/*
 * Copyright (C) 2021 - The MegaMek Team. All Rights Reserved.
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
 */

package megamek.client.ui.swing.util;

import javax.swing.Timer;

/**
 * An animator for <code>float</code> number values.
 *
 * Animators transition a starting value to a final value via a number
 * of intermediate steps.
 *
 * Float animators transform an initial value to a target value at
 * each step.
 */
public class FloatAnimator extends Animator<Float> {


    private float step;
    private float current;
    private float target;


    public FloatAnimator(int intervalMillis,
                         Timer clock,
                         float initial,
                         float target,
                         AnimationStep<Float> handler) {
        super(intervalMillis, clock, handler);
        this.current = initial;
        this.target = target;

        var nSteps = getStepsRemaining();
        this.step = (target - initial) / nSteps;
    }

    public FloatAnimator(int intervalMillis,
                         int stepMillis,
                         float initial,
                         float target,
                         AnimationStep<Float> handler) {
        this(intervalMillis, new Timer(stepMillis, null), initial, target, handler);
    }

    @Override
    protected boolean step() {
        var nSteps = getStepsRemaining();
        if (nSteps > 1) {
            this.current += this.step;
            getHandler().step(this.current);
        } else if (nSteps == 1) {
            getHandler().step(this.target);
        }
        return super.step();
    }

}
