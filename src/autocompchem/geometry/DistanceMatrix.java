package autocompchem.geometry;

/*
 *   Copyright (C) 2014  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * DistanceMatrix is a matrix in which each entry contains a number representing
 * the distance (as a concept) between two objects that are identifyed by the 
 * row and column indexes.
 *
 * @author Marco Foscato
 */ 

public class DistanceMatrix
{
    //data
    private  Map<Integer,Map<Integer,Double>> dm = 
				     new HashMap<Integer,Map<Integer,Double>>();

    //details
    private double maxDist;
    private double minDist;

//------------------------------------------------------------------------------

    /**
     * Construct an empty DistanceMatrix
     */

    public DistanceMatrix()
    {
	this.maxDist = - Double.MAX_VALUE;
	this.minDist = Double.MAX_VALUE;
    }

//------------------------------------------------------------------------------

    /**
     * Put a value in the matrix
     * @param idr row index of the entry to set
     * @param idc column index of the entry to set
     * @param dist the value of the distance
     */

    public void put(int idr, int idc, double dist)
    {
	if (dm.keySet().contains(idr))
	{
	    Map<Integer,Double> row = dm.get(idr);
	    row.put(idc, dist);
	} else {
	    Map<Integer,Double> newRow = new HashMap<Integer,Double>();
	    newRow.put(idc, dist);
	    dm.put(idr, newRow);
	}

	if (dist > maxDist)
	    this.maxDist = dist;
	if ((dist < minDist) && (dist > 0.0D))
	    this.minDist = dist;
    }

//------------------------------------------------------------------------------

    /**
     * Check the existence of an entry in the matrix
     * @param idr row index
     * @param idc column index
     * @return <code>true</code> if the entry exists
     */

    public boolean exists(int idr, int idc)
    {
        if (dm.keySet().contains(idr))
        {
            Map<Integer,Double> row = dm.get(idr);
            if (row.keySet().contains(idc))
            {
		return true;
	    }
	}
	return false;
    }

//------------------------------------------------------------------------------

    /**
     * Return the vector with all the distances involving the object with
     * the given row index
     * @param index of the object to be considered
     * @return the list of distances
     */

    public ArrayList<Double> getAllDistsOnRow(int index)
    {
	ArrayList<Double> allDists = new ArrayList<Double>();
        for (Integer idc : dm.get(index).keySet())
        {
	    allDists.add(dm.get(index).get(idc));
        }
	return allDists;
    }

//------------------------------------------------------------------------------

    /**
     * Return the vector with all the distances involving the object with
     * the given column index
     * @param index of the object to be considered
     * @return the list of distances
     */

    public ArrayList<Double> getAllDistsOnColumn(int index)
    {
        ArrayList<Double> allDists = new ArrayList<Double>();
        for (Integer idr : dm.keySet())
        {
            allDists.add(dm.get(idr).get(index));
        }
        return allDists;
    }

//------------------------------------------------------------------------------

    /**
     * Return the value of a specified entry
     * @param idr row index of the required entry
     * @param idc column index of the required entry
     * @return the valie of the specified entry
     */

    public double get(int idr, int idc)
    {
        Map<Integer,Double> row = dm.get(idr);
	return row.get(idc);
    }

//------------------------------------------------------------------------------

    /**
     * Return the total number of entries
     * @return the total number of entries
     */

    public int getNumEntries()
    {
	int tot = 0;
	for (Integer idr : dm.keySet())
	{
            Map<Integer,Double> row = dm.get(idr);
	    tot = tot + row.keySet().size();
	}
        return tot;
    }

//------------------------------------------------------------------------------

    /**
     * Return the maximum distance along one row
     * @param i the row index
     * @return the largest distance
     */

    public double getMaxDistOnRow(int i)
    {
	double max = - Double.MAX_VALUE;
	for (Double d : getAllDistsOnRow(i))
	{
	    if (d>max)
		max=d;
	}
        return max;
    }

//------------------------------------------------------------------------------

    /**
     * Return the minimum distance along one row
     * @param i the row index
     * @return the smallest distance
     */

    public double getMinDistOnRow(int i)
    {
        double min =  Double.MAX_VALUE;
        for (Double d : getAllDistsOnRow(i))
        {
            if (d<min)
                min=d;
        }
        return min;
    }

//------------------------------------------------------------------------------

    /**
     * Return the maximum distance aalong one column
     * @param i the column index
     * @return the largest distance
     */

    public double getMaxDistOnColumn(int i)
    {
        double max = - Double.MAX_VALUE;
        for (Double d : getAllDistsOnColumn(i))
        {
            if (d>max)
                max=d;
        }
        return max;
    }

//------------------------------------------------------------------------------

    /**
     * Return the minimum distance along one column
     * @param i the column index
     * @return the smallest distance
     */

    public double getMinDistOnColumn(int i)
    {
        double min =  Double.MAX_VALUE;
        for (Double d : getAllDistsOnColumn(i))
        {
            if (d<min)
                min=d;
        }
        return min;
    }

//------------------------------------------------------------------------------

    /**
     * Return the maximum distance
     * @return the largest distance
     */

    public double getMaxDist()
    {
	return maxDist;
    }

//------------------------------------------------------------------------------

    /**
     * Return the minimum distance greater than zero
     * @return the shortest distance
     */

    public double getMinDist()
    {
        return minDist;
    }

//------------------------------------------------------------------------------

}
