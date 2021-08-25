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

import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;

import javax.swing.SwingUtilities;

/**
 * Makes a component transparent to input by forwarding it to another.
 */
public class InputForwarder {


    private Component destination;

    private KeyListener key = null;
    private MouseListener mouse = null;
    private MouseWheelListener wheel = null;


    public InputForwarder(Component destination) {
        this.destination = destination;
    }

    public void forwardAllFrom(Component source) {
        source.addKeyListener(getKeyListener());
        source.addMouseListener(getMouseListener());
        source.addMouseWheelListener(getMouseWheelListener());
    }

    public void unforwardAllFrom(Component source) {
        source.removeMouseListener(getMouseListener());
        source.removeMouseWheelListener(getMouseWheelListener());
        source.removeKeyListener(getKeyListener());
    }

    public KeyListener getKeyListener() {
        if (this.key == null) {
            this.key = new KeyListener() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        dispatch(e);
                    }
                    @Override
                    public void keyReleased(KeyEvent e) {
                        dispatch(e);
                    }
                    @Override
                    public void keyTyped(KeyEvent e) {
                        dispatch(e);
                    }
                };
        }
        return this.key;
    }

    public MouseListener getMouseListener() {
        if (this.mouse == null) {
            this.mouse = new MouseListener() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        dispatch(retargetMouseEvent(e));
                    }
                    @Override
                    public void mousePressed(MouseEvent e) {
                        dispatch(retargetMouseEvent(e));
                    }
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        dispatch(retargetMouseEvent(e));
                    }
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        // XXX to implement this we would need to
                        // determine if the source and dest partially
                        // overlap and if so, only fire if the
                        // retargetted point is within the dest, which
                        // is too much work to do without more coffee.
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        var retargetted = retargetMouseEvent(e);
                        if (!InputForwarder.this.destination.contains(retargetted.getPoint())) {
                            dispatch(retargetted);
                        }
                    }
                };
        }
        return this.mouse;
    }

    public MouseWheelListener getMouseWheelListener() {
        if (this.wheel == null) {
            this.wheel = new MouseWheelListener() {
                    @Override
                    public void mouseWheelMoved​(MouseWheelEvent e) {
                        dispatch(retargetMouseEvent(e));
                    }
                };
        }
        return this.wheel;
    }

    protected void dispatch(InputEvent e) {
        this.destination.dispatchEvent(e);
        e.consume();
    }

    private final MouseEvent retargetMouseEvent(MouseEvent e) {
        return SwingUtilities.convertMouseEvent(
            (Component) e.getSource(), e, this.destination
        );
    }

}
