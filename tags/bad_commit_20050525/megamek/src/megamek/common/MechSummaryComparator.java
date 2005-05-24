/*
 * MechSummaryComparator.java - Copyright (C) 2002 Josh Yockey
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
 
 package megamek.common;
 
 import com.sun.java.util.collections.Comparator;
 
 public class MechSummaryComparator implements Comparator
 {
    public static final int T_CHASSIS = 0;
    public static final int T_MODEL = 1;
    public static final int T_WEIGHT = 2;
    public static final int T_BV = 3;
    public static final int T_YEAR = 4;
    
    private int m_nType;
    
    public MechSummaryComparator(int nType)
    {
        m_nType = nType;
    }
    
    public int compare(Object o1, Object o2)
    {
        MechSummary ms1 = (MechSummary)o1;
        MechSummary ms2 = (MechSummary)o2;
        
        switch (m_nType) {
            case T_CHASSIS :
                return ms1.getChassis().compareTo(ms2.getChassis());
            case T_MODEL :
                return ms1.getModel().compareTo(ms2.getModel());
            case T_WEIGHT :
                return numCompare(ms1.getTons(), ms2.getTons());
            case T_BV :
                return numCompare(ms1.getBV(), ms2.getBV());
            case T_YEAR :
                return numCompare(ms1.getYear(), ms2.getYear());
            default :
                return 0;
        }
    }
    
    private int numCompare(int n1, int n2)
    {
        if (n1 == n2) {
            return 0;
        }
        else if (n1 > n2) { 
            return -1; 
        }
        else {
            return 1;
        }
    }
}