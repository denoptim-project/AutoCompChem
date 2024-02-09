package autocompchem.modeling.atomtuple;

import autocompchem.modeling.AtomLabelsGenerator;

/**
 * Constants useful in the manipulation of tuple of atoms.
 * @author Marco Foscato
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
	// WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/ConstraintsGenerator.json and
	// inputdefinition/AtomTupleGenerator.jsonv
	public static final String KEYUSECURRENTVALUE = "GETCURRENTVALUE";

	/**
	 * Key of value-less attribute  of {@link AtomTupleMatchingRule} that 
	 * require to restrict the generation of annotated tuples of atoms 
	 * to those that contain atoms bonded in the order given by the tuple.
	 */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/ConstraintsGenerator.json and
	// inputdefinition/AtomTupleGenerator.jsonv
	public static final String KEYONLYBONDED = "ONLYBONDED";

	/**
	 * Key of value-less attribute of {@link AtomTupleMatchingRule} that 
	 * require to append atom labels (from {@link AtomLabelsGenerator}) to
	 * annotated tuples of atoms. The parameters of {@link AtomLabelsGenerator}
	 * are used to generate the labels.
	 */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/ConstraintsGenerator.json and
	// inputdefinition/AtomTupleGenerator.jsonv
	public static final String KEYGETATOMLABELS = "GETATOMLABELS";
	
    /**
     * Keyword used to identify prefixes
     */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/ConstraintsGenerator.json and
	// inputdefinition/AtomTupleGenerator.jsonv
    public static final String KEYPREFIX = "PREFIX";
    
    /**
     * Keyword used to identify suffix
     */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/ConstraintsGenerator.json and
	// inputdefinition/AtomTupleGenerator.jsonv
    public static final String KEYSUFFIX= "SUFFIX";
	
    /**
     * List of default valued keywords recognized in text-like definition of 
     * atom tuple matching rules.
     */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/ConstraintsGenerator.json and
	// inputdefinition/AtomTupleGenerator.jsonv
    public static final String[] DEFAULTVALUEDKEYS = {KEYPREFIX,KEYSUFFIX};
    
    /**
     * List of default value-less keywords recognized in text-like definition of 
     * atom tuple matching rules.
     */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/ConstraintsGenerator.json and
	// inputdefinition/AtomTupleGenerator.jsonv
    public static final String[] DEFAULTVALUELESSKEYS = {
    		KEYONLYBONDED, KEYUSECURRENTVALUE, KEYGETATOMLABELS};
}
