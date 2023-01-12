package autocompchem.modeling.atomtuple;

/**
 * Constants useful in the manipulation of tuple of atoms.
 * @author Marco Foscat
 *
 */
public class AtomTupleConstants 
{
	/**
	 * Key of attribute used to store current values of tuples corresponding to
	 * internal coordinates, i.e., tuples having 2, 3, or 4 items.
	 */
	public static final String KEYCURRENTVALUE = "CURRENTVALUE";	

	/**
	 * Key of value-less attribute of {@link AtomTupleMatchingRule} that 
	 * require to use current value of internal
	 * coordinates definable from atom tuples. 
	 */
	public static final String KEYUSECURRENTVALUE = "GETCURRENTVALUE";

	/**
	 * Key of value-less attribute  of {@link AtomTupleMatchingRule} that 
	 * require to restrict the generation of annotated tuples of atoms 
	 * to those that contain atoms bonded in the order given by the tuple.
	 */
	public static final String KEYONLYBONDED = "ONLYBONDED";
}
