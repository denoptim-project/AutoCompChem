package autocompchem.smarts;

import java.util.ArrayList;
import java.util.List;

import org.openscience.cdk.isomorphism.Mappings;

/**
 * Class for collecting 0-based indexes of atoms mapped by SMARTS-query searches.
 * This class is a simplified version of {@link Mappings}.
 */
public class MatchingIdxs extends ArrayList<List<Integer>>
{

	/**
	 * Checks if any set of indexes contains more than one entry, meaning that
	 * a substructure larger than a single atom has been matched.
	 * @return <code>true</code> is any of the set of indexes contains more
	 * than one index.
	 */
	public boolean hasMultiCenterMatches() 
	{
		for (List<Integer> nestedLst : this)
		{
			if (nestedLst.size()>1)
				return true;
		}
		return false;
	}

}
