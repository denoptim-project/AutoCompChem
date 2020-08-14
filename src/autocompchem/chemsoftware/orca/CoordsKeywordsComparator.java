package autocompchem.chemsoftware.orca;

import java.util.Comparator;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.Keyword;

/**
 * Sorts keywords consistently with the expectations of the %coords input
 * block: first type of coordinates, then charge, then spin multiplicity.
 */
class CoordsKeywordsComparator implements Comparator<Keyword>
{
    @Override
    public int compare(Keyword a, Keyword b)
    {           
        String aName = a.getName();
        String bName = b.getName();
        int intA = 0;
        int intB = 0;
        
        if (aName.toUpperCase().equals(ChemSoftConstants.PARCOORDTYPE))
        {
        	intA = -3;
        }
        if (bName.toUpperCase().equals(ChemSoftConstants.PARCOORDTYPE))
        {
        	intB = -3;
        }
        
        if (aName.toUpperCase().equals(ChemSoftConstants.PARCHARGE))
        {
        	intA = -2;
        }
        if (bName.toUpperCase().equals(ChemSoftConstants.PARCHARGE))
        {
        	intB = -2;
        }
        
        if (aName.toUpperCase().equals(ChemSoftConstants.PARSPINMULT))
        {
        	intA = -1;
        }
        if (bName.toUpperCase().equals(ChemSoftConstants.PARSPINMULT))
        {
        	intB = -1;
        }
        
        //add any other priority rules go here... 
        //but now there seems to be no more.
        
        return Integer.compare(intA,intB);
    }
}