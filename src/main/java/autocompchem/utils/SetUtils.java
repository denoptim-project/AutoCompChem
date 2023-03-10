package autocompchem.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Toolbox for sets.
 * 
 * @author Marco Foscato
 */

public class SetUtils
{

//------------------------------------------------------------------------------

    /**
     * Return the intersection of two sets. This method is OK for mid-to-large 
     * size sets up to tens of million of entries. Does not modify either of the
     * given sets.
     * @param <E>
     * @param setA one set to consider
     * @param setB the other set to consider
     * @return the intersection, i.e., the set of entries that are present in 
     * both sets.
     */

    public static <E> Set<E> getIntersection(Set<E> setA, Set<E> setB)
    {
        Set<E> intersection = new HashSet<E>(setA);
        intersection.retainAll(setB);
        return intersection;
    }
    
//------------------------------------------------------------------------------     

}
