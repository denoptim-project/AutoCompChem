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

/**
 * Condition satisfied if a string is matched
 *
 * @author Marco Foscato
 */

public class MatchText extends Circumstance
{
    /**
     * Pattern to match
     */
    private String pattern = "";

    /**
     * Negation flag: if true then the condition is negated
     */
    protected boolean negation = false;

//------------------------------------------------------------------------------

    /**
     * Constructs an empty MatchText
     */

    public MatchText()
    {
        this("", false, null);
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a MatchText defining the pattern to match.
     * @param pattern the pattern to be matches.
     */

    public MatchText(String pattern)
    {
        this(pattern, false, null);
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a MatchText defining the pattern to match and the information 
     * channel where to search for it.
     * @param pattern the pattern to be matches
     * @param ict the type of information channel where to search for this loop
     * counter.
     */

    public MatchText(String pattern, InfoChannelType ict)
    {
        this(pattern, false, ict);
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a MatchText defining the pattern to match
     * @param pattern the pattern to be matches
     * @param negation if true the condition is satisfied if the pattern is 
     * not matched.
     */

    public MatchText(String pattern, boolean negation)
    {
        this(pattern, negation, null);
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a MatchText defining the pattern to match
     * @param pattern the pattern to be matches
     * @param negation if true the condition is satisfied if the pattern is
     * not matched.
     * @param channel the information channel where to search for this loop
     * counter.
     */

    public MatchText(String pattern, boolean negation, 
                                              InfoChannelType ict)
    {
        super(ict);
        this.hasTxtQuery = true;
        this.pattern = pattern;
        this.negation = negation;
    }

//------------------------------------------------------------------------------

    /**
     * Return the pattern
     * @return the pattern
     */
    public String getPattern()
    {
        return pattern;
    }

//------------------------------------------------------------------------------

    /**
     * Calculate the satisfaction score. A real value between 0.0 and 1.0
     * where 0.0 means "conditions not satisfied" and 1.0 means
     * "condition fully satisfied".
     * @param input an object that contains all information
     * needed to calculate the satisfaction score. Can be null if not needed.
     * @return numerical score
     */

    public double calculateScore(List<String> matches)
    {
        double score = 0.0;
        if (negation)
        {
            if (matches.size() == 0)
            {
                score = 1.0;
            }
        }
        else
        {
            //TODO: here we can make the score dependent on #matches 
            if (matches.size() > 0)
            {
                score = 1.0;
            }
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
        sb.append("MatchText [pattern:").append(pattern);
        sb.append("; channel:").append(super.getChannelType());
        sb.append("; negation:").append(negation);
        sb.append("]]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
