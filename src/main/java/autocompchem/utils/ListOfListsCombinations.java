package autocompchem.utils;

import java.text.DecimalFormat;

/*   
 *   Copyright (C) 2017  Marco Foscato 
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

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import autocompchem.constants.ACCConstants;

/**
 * Iterator over the combinations among elements contained in a list of lists,
 * where each position in the combination can be occupied only by items defined
 * in the inner list located at the same position. For example, position i can
 * only be occupied by items collected in list i of the given list of lists.
 * 
 * @author Marco Foscato
 */

public class ListOfListsCombinations<T> implements Iterator<List<T>> 
{
	/**
	 * The list of list collecting all elements that can be used in each 
	 * position.
	 */
	private final List<List<T>> listOfLists;
	
	/**
	 * The indexes of the next iteration
	 */
    private int[] nextIndixes;

    /**
     * Flag signaling the next combination is available.
     */
    private boolean hasNext = false;
    
//------------------------------------------------------------------------------

    /**
     * Constructor defining which list of list to iterate over.
     * @param listOfLists the list of lists on which we iterate.
     */
    public ListOfListsCombinations(List<List<T>> listOfLists) 
    {
        this.listOfLists = listOfLists;
        this.nextIndixes = new int[listOfLists.size()];
        if (listOfLists.size()!=0)
        {
        	for (List<T> inner : listOfLists)
        	{
        		if (inner.size()>0)
        		{
        			this.hasNext = true;
        			break;
        		}
        	}
        }	
    }

//------------------------------------------------------------------------------
    
	@Override
	public boolean hasNext() 
	{
		return hasNext;
	}

//------------------------------------------------------------------------------

	@Override
	public List<T> next() 
	{
		// This creates the current combination
        List<T> combination = new ArrayList<T>(listOfLists.size());
        for (int i=0; i<listOfLists.size(); i++) 
        {
        	combination.add(listOfLists.get(i).get(nextIndixes[i]));
        }
        
        // This defined the next combination by increasing the indexes.
        for (int i=listOfLists.size()-1; i>=0; i--) 
        {
        	int candidateIndex = nextIndixes[i] + 1;
        	int maxIndex = listOfLists.get(i).size();
        	if (candidateIndex >= maxIndex) 
            {
        		if (i==0)
        		{
        			// End of all combinations
        			hasNext = false;
        			break;
        		}
        		nextIndixes[i] = 0;
            } else {
            	nextIndixes[i] = candidateIndex;
            	break;
            }
        }
        return combination;
	}

//------------------------------------------------------------------------------


//------------------------------------------------------------------------------
    
}
