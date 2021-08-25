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

import java.awt.Color;
import javax.swing.Timer;

/**
 * An animator for colour objects.
 *
 * Animators transition a starting value to a final value via a number
 * of intermediate steps.
 *
 * Colour animators transform an initial color to a target color,
 * updating all four RGBA components at each step.
 */
public class ColorAnimator extends Animator<Color> {


    private float[] step;
    private float[] current;
    private Color target;


    public ColorAnimator(int intervalMillis,
                         Timer clock,
                         Color initial,
                         Color target,
                         AnimationStep<Color> handler) {
        super(intervalMillis, clock, handler);
        this.current = initial.getComponents(null);
        this.target = target;
        this.step = new float[4];

        var nSteps = getStepsRemaining();
        var targetComps = target.getComponents(null);
        this.step[0] = (targetComps[0] - this.current[0]) / nSteps;
        this.step[1] = (targetComps[1] - this.current[1]) / nSteps;
        this.step[2] = (targetComps[2] - this.current[2]) / nSteps;
        this.step[3] = (targetComps[3] - this.current[3]) / nSteps;
    }

    public ColorAnimator(int intervalMillis,
                         int stepMillis,
                         Color initial,
                         Color target,
                         AnimationStep<Color> handler) {
        this(intervalMillis, new Timer(stepMillis, null), initial, target, handler);
    }

    @Override
    protected boolean step() {
        var nSteps = getStepsRemaining();
        if (nSteps > 1) {
            this.current[0] += this.step[0];
            this.current[1] += this.step[1];
            this.current[2] += this.step[2];
            this.current[3] += this.step[3];

            getHandler().step(
                new Color(
                    this.current[0],
                    this.current[1],
                    this.current[2],
                    this.current[3]
                )
            );
        } else if (nSteps == 1) {
            getHandler().step(this.target);
        }
        return super.step();
    }

}
