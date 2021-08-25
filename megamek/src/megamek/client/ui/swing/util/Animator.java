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

import java.awt.event.ActionListener;
import javax.swing.Timer;

/**
 * Incrementally updates a value over a specific interval.
 *
 * Animators transition a starting value to a final value via a number
 * of intermediate steps. The number of steps taken depends on the
 * length the given total interval and the given step length.
 *
 * This class is designed to animate Swing class properties, and hence
 * uses a Swing timer as its internal clock. As such step updates will
 * be calculated and executed on the Event Dispatch Thread (EDT).
 *
 * Concrete subclasses provide implementations for specific types, see
 * those for details.
 */
public abstract class Animator<T> {


    /**
     * Functional interface for objects to be notified of updated values.
     */
    public interface AnimationStep<T> {

        /**
         * Called by an animator when the next step has been calculated.
         */
        void step(T value);

    }


    private int intervalLength;
    private int stepLength;
    private int stepsRemaining;
    private Timer clock;
    private boolean autoStop;
    private ActionListener adaptor = (e) -> step();
    private AnimationStep handler;


    /**
     * Creates an animator driven using an external clock.
     *
     * The animator will register itself with the clock and hence
     * immediately start emitting steps if the clock is already
     * running. Once the animation is complete, the animator will
     * un-register itself, but not implicitly stop the clock.
     *
     * The clock's delay will be used to calculate the step increment
     * and count at construction time - altering the clock's delay
     * will not change the behaviour of the animator, leading to
     */
    protected Animator(int intervalLength,
                       Timer clock,
                       AnimationStep<T> handler) {
        this.intervalLength = intervalLength;
        this.stepLength = clock.getDelay();
        this.stepsRemaining = (int) (intervalLength / this.stepLength);
        this.clock = clock;
        this.handler = handler;
        clock.addActionListener(this.adaptor);
    }

    /**
     * Creates an animator driven by an internal clock.
     *
     * The animator can be started by calling {@link start} given
     * handled if the given clock is already running.
     */
    protected Animator(int intervalLength,
                       int stepLength,
                       AnimationStep<T> handler) {
        this(intervalLength, new Timer(stepLength, null), handler);
    }

    /**
     * Stops the animator running, as if it has completed normally.
     */
    public void cancel() {
        this.clock.removeActionListener(this.adaptor);
        this.stepsRemaining = 0;
    }

    /**
     * Specifies if the animator has completed all required steps.
     */
    public boolean isComplete() {
        return (this.stepsRemaining == 0);
    }

    /**
     * Returns the animation interval, in milliseconds.
     */
    public int getIntervalLength() {
        return this.intervalLength;
    }

    /**
     * Returns the animation interval, in milliseconds.
     */
    public int getStepLength() {
        return this.stepLength;
    }

    /**
     * Returns the number of steps remaining in the animation.
     */
    public int getStepsRemaining() {
        return this.stepsRemaining;
    }

    /**
     * Returns the clock being used by this animator.
     *
     * Altering clock's delay will not change the behaviour of the
     * animator, leading to unexpected results.
     */
    public Timer getClock() {
        return this.clock;
    }

    /**
     * Returns the caller's handler.
     */
    protected AnimationStep<T> getHandler() {
        return this.handler;
    }

    /**
     * Performs a step in the animation.
     */
    protected boolean step() {
        if (this.stepsRemaining > 0) {
            this.stepsRemaining--;
        }
        var cont = true;
        if (this.stepsRemaining == 0) {
            cont = false;
            cancel();
        }
        return cont;
    }


}
