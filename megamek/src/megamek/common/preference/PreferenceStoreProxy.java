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

    @Override
    public boolean getDefaultBoolean(String name) {
        return store.getDefaultBoolean(name);
    }

    @Override
    public int getDefaultInt(String name) {
        return store.getDefaultInt(name);
    }

    @Override
    public long getDefaultLong(String name) {
        return store.getDefaultLong(name);
    }

    @Override
    public String getDefaultString(String name) {
        return store.getDefaultString(name);
    }

    @Override
    public double getDefaultDouble(String name) {
        return store.getDefaultDouble(name);
    }

    @Override
    public float getDefaultFloat(String name) {
        return store.getDefaultFloat(name);
    }

    @Override
    public boolean getBoolean(String name) {
        return store.getBoolean(name);
    }

    @Override
    public int getInt(String name) {
        return store.getInt(name);
    }

    @Override
    public long getLong(String name) {
        return store.getLong(name);
    }

    @Override
    public float getFloat(String name) {
        return store.getFloat(name);
    }

    @Override
    public double getDouble(String name) {
        return store.getDouble(name);
    }

    @Override
    public String getString(String name) {
        return store.getString(name);
    }

    @Override
    public void putValue(String name, String value) {
        store.putValue(name, value);
    }

    @Override
    public void setDefault(String name, boolean value) {
        store.setDefault(name, value);
    }

    @Override
    public void setDefault(String name, int value) {
        store.setDefault(name, value);
    }

    @Override
    public void setDefault(String name, long value) {
        store.setDefault(name, value);
    }

    @Override
    public void setDefault(String name, float value) {
        store.setDefault(name, value);
    }

    @Override
    public void setDefault(String name, double value) {
        store.setDefault(name, value);
    }

    @Override
    public void setDefault(String name, String value) {
        store.setDefault(name, value);
    }

    @Override
    public void setValue(String name, boolean value) {
        store.setValue(name, value);
    }

    @Override
    public void setValue(String name, int value) {
        store.setValue(name, value);
    }

    @Override
    public void setValue(String name, long value) {
        store.setValue(name, value);
    }

    @Override
    public void setValue(String name, float value) {
        store.setValue(name, value);
    }

    @Override
    public void setValue(String name, double value) {
        store.setValue(name, value);
    }

    @Override
    public void setValue(String name, String value) {
        store.setValue(name, value);
    }

    @Override
    public void addPreferenceChangeListener(IPreferenceChangeListener listener) {
        store.addPreferenceChangeListener(listener);
    }

    @Override
    public void removePreferenceChangeListener(
            IPreferenceChangeListener listener) {
        store.removePreferenceChangeListener(listener);
    }
}
