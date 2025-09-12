package autocompchem.molecule.conformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Comparator that imposes that no pair of coordinates acts on the same 
 * bond torsion, prioritized atom flips, and bond torsion involving the lowest
 * atom indexes. Like {@link ConformationalCoordinate}, this class assumes
 * that {@link ConformationalCoordinate} can be defined only 
 * by 1, 2, or 4 centers.
 */

public class ConfCoordComparator implements Comparator<ConformationalCoordinate>
{

	@Override
	public int compare(ConformationalCoordinate o1, 
			ConformationalCoordinate o2) 
	{	
		if (o1.getType()!=o2.getType())
			return Integer.compare(typePriority(o1), typePriority(o2));
		
		if (o1.getNumberOfIDs()==1 && o2.getNumberOfIDs()==1)
			return Integer.compare(o1.getAtomIDs().get(0), 
					o2.getAtomIDs().get(0));
		else if (o1.getNumberOfIDs()==1 && o2.getNumberOfIDs()!=1)
			return Integer.compare(1,2);
		else if (o1.getNumberOfIDs()!=1 && o2.getNumberOfIDs()==1)
			return Integer.compare(2,1);
		
		// Here we should have only size 2 or 4
		if (!(o1.getNumberOfIDs()==2 || o1.getNumberOfIDs()==4))
		{
			throw new IllegalArgumentException("Unexpected number of "
					+ "atom IDs (" + o1.getNumberOfIDs() + "). "
					+ "Cannot compare this conformational "
					+ "coordinate: " + o1);
		}
		if (!(o2.getNumberOfIDs()==2 || o2.getNumberOfIDs()==4))
		{
			throw new IllegalArgumentException("Unexpected number of "
					+ "atom IDs (" + o2.getNumberOfIDs() + "). "
					+ "Cannot compare this conformational "
					+ "coordinate: " + o2);
		}
		
		List<Integer> pair1 = getRelevantPair(o1);
		List<Integer> pair2 = getRelevantPair(o2);
		
		if (pair1.get(0)!=pair2.get(0))
			return Integer.compare(pair1.get(0), pair2.get(0));
		
		return Integer.compare(pair1.get(1), pair2.get(1));
	}

//------------------------------------------------------------------------------

	  
	private int typePriority(ConformationalCoordinate coord)
	{
		int result = 100;
		switch (coord.getType()) 
		{
		case FLIP:
			result = 1;
			break;
		case TORSION:
			result = 2;
			break;
		case UNDEFINED:
			result = 3;
			break;
		default:
			break;
		}
		return result;
	}

//------------------------------------------------------------------------------

	  
	/**
	 * We should never call this with coord of size different that 2 or 4.
	 */
	private List<Integer> getRelevantPair(ConformationalCoordinate coord)
	{
		List<Integer> pair = new ArrayList<Integer>(2);
		switch (coord.getNumberOfIDs()) 
		{
		case 2:
			pair.add(coord.getAtomIDs().get(0));
			pair.add(coord.getAtomIDs().get(1));
			break;
		case 4:
			pair.add(coord.getAtomIDs().get(1));
			pair.add(coord.getAtomIDs().get(2));
			break;
		}
		Collections.sort(pair);
		return pair;
	}
}