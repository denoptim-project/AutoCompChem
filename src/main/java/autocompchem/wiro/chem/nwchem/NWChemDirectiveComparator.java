package autocompchem.wiro.chem.nwchem;

import java.util.Comparator;

import autocompchem.wiro.chem.Directive;

/**
 * Sorts directives according to NWChem conventions.
 */

public class NWChemDirectiveComparator implements Comparator<Directive> 
{

	@Override
	public int compare(Directive o1, Directive o2) {
        final int FIRST = 1;
        final int EQUAL = 0;
        final int LAST = -1;

        int res = EQUAL;

        String nameA = o1.getName().toUpperCase();
        String nameB = o2.getName().toUpperCase();

        if (nameA.equals(NWChemConstants.TASKDIR))
        {
            res = FIRST;
        }
        else if (nameB.equals(NWChemConstants.TASKDIR))
        {
            res = LAST;
        }
        else
        {
            boolean isStartupA = NWChemConstants.STARTUPTASKS.contains(nameA);
            boolean isStartupB = NWChemConstants.STARTUPTASKS.contains(nameB);

            if (isStartupA && !isStartupB)
            {
                res = LAST;
            }
            else if (!isStartupA && isStartupB)
            {
                res = FIRST;
            }
            else if (isStartupA && isStartupB)
            {
                if (nameA.contains("START"))
                {
                    res = LAST;
                }
                if (nameB.contains("START"))
                {
                    res = FIRST;
                }
            }
            else
            {
                res = nameA.compareTo(nameB);
            }
        }
        return res;
	}

}
