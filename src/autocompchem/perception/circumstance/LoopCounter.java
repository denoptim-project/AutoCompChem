package autocompchem.perception.circumstance;

/*
 *   Copyright (C) 2018  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import autocompchem.perception.infochannel.InfoChannelType;

/**
 * Circumstance verified if a loop counter is matched ans is found
 * still within a given range.
 *
 * @author Marco Foscato
 */

public class LoopCounter extends MatchText
{
    /**
     * Minimum of valid range
     */
    private double minCounter = 0.0;

    /**
     * Maximum of valid range
     */
    private double maxCounter = 0.0;

//------------------------------------------------------------------------------

    /**
     * Constructs an empty LoopCounter
     */

    public LoopCounter()
    {
	super();
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a LoopCounter defining the counter ID, and the acceptable 
     * counter range
     * @param counterId the counter ID
     * @param minCounter the minimum
     * @param maxCounter the maximum
     */

    public LoopCounter(String counterId, double minCounter, double maxCounter)
    {
	super(counterId);
	this.minCounter = minCounter;
	this.maxCounter = maxCounter;
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a LoopCounter defining the counter ID, and the acceptable
     * counter range, in addition to the information channel type
     * @param counterId the counter ID
     * @param minCounter the minimum
     * @param maxCounter the maximum
     * @param ict the type of information channel where to search for this loop
     * counter.
     */

    public LoopCounter(String counterId, double minCounter, double maxCounter,
					              InfoChannelType ict)
    {
	super(counterId, ict);
        this.minCounter = minCounter;
        this.maxCounter = maxCounter;
    }

//------------------------------------------------------------------------------

    /**
     * Return the counter Id
     * @return the counter Id
     */

    public String getCounterID()
    {
	return super.getPattern();
    }

//------------------------------------------------------------------------------

    /**
     * Return a human readable description
     * @return a string
     */

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("LoopCounter [counterId:").append(getCounterID());
        sb.append("; channel:").append(super.getChannelType());
        sb.append("; minCounter:").append(minCounter);
        sb.append("; maxCounter:").append(maxCounter);
        sb.append("]]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
