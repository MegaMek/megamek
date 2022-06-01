/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.util;

/**
 * @author Ryan McConnell (oscarmm)
 * @since May 22, 2005
 */
public class CircularIntegerBuffer {

    private int begin;
    private int end;
    private int[] buffer;

    /**
     * Creates the new buffer
     * 
     * @param size required size
     */
    public CircularIntegerBuffer(int size) {
        buffer = new int[size];
        clear();
    }

    /**
     * Returns the length of this buffer
     * 
     * @return length of this buffer
     */
    public int length() {
        return buffer.length;
    }

    /**
     * Clears the buffer
     */
    public void clear() {
        begin = buffer.length - 1;
        end = -1;
    }

    /**
     * Adds new value
     * 
     * @param value value to add
     */
    public void push(int value) {
        end++;
        if (end > begin) {
            begin++;
            if (begin == buffer.length) {
                begin = 0;
            }
        }

        if (end == buffer.length) {
            end = 0;
        }
        buffer[end] = value;
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();

        int indexBegin = begin + 1;
        if (indexBegin == buffer.length) {
            indexBegin = 0;
        }

        if (indexBegin <= end) {
            for (int i = indexBegin; i <= end; i++) {
                result.append(buffer[i]).append(" ");
            }
        } else {
            for (int i = indexBegin; i < buffer.length; i++) {
                result.append(buffer[i]).append(" ");
            }
            for (int i = 0; i <= end; i++) {
                result.append(buffer[i]).append(" ");
            }
        }
        return result.toString();
    }
}
