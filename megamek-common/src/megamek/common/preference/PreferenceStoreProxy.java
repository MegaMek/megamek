/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.preference;

public abstract class PreferenceStoreProxy implements IPreferenceStore {

    protected IPreferenceStore store;

    public boolean getDefaultBoolean(String name) {
        return store.getDefaultBoolean(name);
    }

    public int getDefaultInt(String name) {
        return store.getDefaultInt(name);
    }

    public long getDefaultLong(String name) {
        return store.getDefaultLong(name);
    }

    public String getDefaultString(String name) {
        return store.getDefaultString(name);
    }

    public double getDefaultDouble(String name) {
        return store.getDefaultDouble(name);
    }

    public float getDefaultFloat(String name) {
        return store.getDefaultFloat(name);
    }

    public boolean getBoolean(String name) {
        return store.getBoolean(name);
    }

    public int getInt(String name) {
        return store.getInt(name);
    }

    public long getLong(String name) {
        return store.getLong(name);
    }

    public float getFloat(String name) {
        return store.getFloat(name);
    }

    public double getDouble(String name) {
        return store.getDouble(name);
    }

    public String getString(String name) {
        return store.getString(name);
    }

    public void putValue(String name, String value) {
        store.putValue(name, value);
    }

    public void setDefault(String name, boolean value) {
        store.setDefault(name, value);
    }

    public void setDefault(String name, int value) {
        store.setDefault(name, value);
    }

    public void setDefault(String name, long value) {
        store.setDefault(name, value);
    }

    public void setDefault(String name, float value) {
        store.setDefault(name, value);
    }

    public void setDefault(String name, double value) {
        store.setDefault(name, value);
    }

    public void setDefault(String name, String value) {
        store.setDefault(name, value);
    }

    public void setValue(String name, boolean value) {
        store.setValue(name, value);
    }

    public void setValue(String name, int value) {
        store.setValue(name, value);
    }

    public void setValue(String name, long value) {
        store.setValue(name, value);
    }

    public void setValue(String name, float value) {
        store.setValue(name, value);
    }

    public void setValue(String name, double value) {
        store.setValue(name, value);
    }

    public void setValue(String name, String value) {
        store.setValue(name, value);
    }

    public void addPreferenceChangeListener(IPreferenceChangeListener listener) {
        store.addPreferenceChangeListener(listener);
    }

    public void removePreferenceChangeListener(
            IPreferenceChangeListener listener) {
        store.removePreferenceChangeListener(listener);
    }
}
