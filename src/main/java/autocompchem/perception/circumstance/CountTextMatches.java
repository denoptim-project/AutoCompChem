package autocompchem.perception.circumstance;

/*
 *   Copyright (C) 2018  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.List;

import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.perception.situation.Situation;


/**
 * Condition satisfied if a string is matched in a number of lines of a single 
 * source. The number of matches can be a constrained with minimum or maximum
 * or within/outside a range, or exactly equal to a number. Different
 * constructors are used to specify each of these constraints.
 *
 * @author Marco Foscato
 */

public class CountTextMatches extends MatchText
{
    /**
     * Minimum number of matches
     */
    private int min;

    /**
     * Max number of matches
     */
    private int max;

    /**
     * Required number of matches
     */
    private int num;

    /**
     * The chosen type of constraints to calculate the score
     */
    private  ConstrainType cnstrType = ConstrainType.EXACT;

    /**
     * All the known types of constraints
     */
    private enum ConstrainType
    {
        MIN, MAX, RANGE, EXACT;
    } 

//------------------------------------------------------------------------------

    /**
     * Constructs a CountTextMatches defining the pattern to match and  
     * a given number of matches required to satisfy this circumstance.
     * @param pattern the pattern to be matches
     * @param ict the type of information channel where to search for this loop
     * counter.
     * @param num the number of matches
     */

    public CountTextMatches(String pattern, int num, InfoChannelType ict)
    {
        this(pattern, num, ict, false);
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a CountTextMatches defining the pattern to match and
     * a given number of matches required to satisfy this circumstance.
     * @param pattern the pattern to be matches
     * @param num the number of matches
     * @param ict the type of information channel where to search for this loop
     * counter.
     * @param negation if true the condition is satisfied if the count is not
     * equal to the given number.
     */

    public CountTextMatches(String pattern, int num, InfoChannelType ict, 
                                                              boolean negation)
    {
        super(pattern,ict);
        this.num = num;
        this.cnstrType = ConstrainType.EXACT; //not needed, but doesn't hurt
        super.negation = negation;
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a CountTextMatches defining the pattern to match and
     * the min/max number of matches required to satisfy this circumstance.
     * Negation is not supported for this method.
     * @param pattern the pattern to be matches
     * @param minOrMax the numerical definition of the limited number of matches
     * found (the limit is always included in the acceptable range. I.e., we use
     * &le; or &ge;)
     * @param pol the polatiry, meaning is <code>true</code> minOrMax will be
     * used to set the minimum, otherwise is the maximum number of matches
     */

    public CountTextMatches(String pattern, int minOrMax, boolean pol, 
                                                            InfoChannelType ict)
    {
        super(pattern,ict);
        if (pol)
        {
            this.min = minOrMax;
            this.cnstrType = ConstrainType.MIN;
        }
        else
        {
            this.max = minOrMax;
            this.cnstrType = ConstrainType.MAX;
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructs a CountTextMatches defining the pattern to match and
     * the range (min and max, included) within which number of matches
     * required to satisfy this circumstance.
     * @param pattern the pattern to be matches
     * @param min the minimum number of matches (acceptable range is &ge; this)
     * @param max the maximum number of matches (acceptable range is &le; this)
     * @param channel the information channel where to search for this loop
     * counter.
     */

    public CountTextMatches(String pattern, int min, int max,
            InfoChannelType ict)
    {
        this(pattern, min, max, ict, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructs a CountTextMatches defining the pattern to match and
     * the range (min and max, included) within which number of matches
     * required to satisfy this circumstance.
     * @param pattern the pattern to be matches
     * @param min the minimum number of matches (acceptable range is &ge; this)
     * @param max the maximum number of matches (acceptable range is &le; this)
     * @param channel the information channel where to search for this loop
     * counter.
     * @param negation if true the condition is satisfied if the count is 
     * outside the given range.
     */

    public CountTextMatches(String pattern, int min, int max,
            InfoChannelType ict, boolean negation)
    {
        super(pattern,ict);
        if (min!=-1)
            this.min = min;
        if (max!=-1)
            this.max = max;
        this.cnstrType = ConstrainType.RANGE;
        super.negation = negation;
    }

//------------------------------------------------------------------------------

    /**
     * Calculate the satisfaction score. A real value 
     * where 0.0 means "conditions not satisfied" and 1.0 means
     * "condition fully satisfied".
     * @param matches the list of matches
     * @return numerical score
     */

    @Override
    public double calculateScore(List<String> matches)
    {
        double score = 0.0;
        int numMatches = matches.size();

        switch (cnstrType)
        {
            case MIN:
                if (numMatches >= min)
                {
                    score = 1.0;
                }
                break;

            case MAX:
                if (numMatches <= max)
                {
                    score = 1.0;
                }
                break;

            case RANGE:
                if ((numMatches >= min) && (numMatches <= max))
                {
                    score = 1.0;
                    if (negation)
                    {
                        score = 0.0;
                    }
                }
                else
                {
                    if (negation)
                    {
                        score = 1.0;
                    }
                }
                break;

            case EXACT:
                if (numMatches == num)
                {
                    score = 1.0;
                    if (negation)
                    {
                        score = 1.0;
                    }
                }
                else
                {
                    if (negation)
                    {
                        score = 1.0;
                    }
                }
                break;

        }
        return score;
    }

//------------------------------------------------------------------------------

    /**
     * Return a human readable representation
     * @return a string
     */

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CountTextMatches [pattern:").append(super.getPattern());
        sb.append("; min: ").append(min);
        sb.append("; max: ").append(max);
        sb.append("; num: ").append(num);
        sb.append("; cnstrType: ").append(cnstrType);
        sb.append("; channel:").append(super.getChannelType());
        sb.append("; negation:").append(super.negation);
        sb.append("]]");
        return sb.toString();
    }
    
//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (o== null)
            return false;
        
        if (o == this)
            return true;
        
        if (o.getClass() != getClass())
            return false;
         
        CountTextMatches other = (CountTextMatches) o;
         
        if (this.min != other.min)
            return false;
        
        if (this.max != other.max)
           return false;
       
        if (this.num != other.num)
           return false;
        
        if (!this.cnstrType.equals(other.cnstrType))
        	return false;
        
        return super.equals(other);
    }

//------------------------------------------------------------------------------

}
