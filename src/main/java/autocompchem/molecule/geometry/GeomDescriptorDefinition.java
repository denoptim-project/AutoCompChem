package autocompchem.molecule.geometry;

import java.util.Arrays;
import java.util.List;

import autocompchem.modeling.atomtuple.AtomTupleConstants;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;


/**
 * Class representing the formatted definition of a rule to define 
 * {@link GeomDescriptor} from a given chemical structure.
 */

public class GeomDescriptorDefinition extends AtomTupleMatchingRule
{
	/**
	 * Root of name used to identify any instance of this class.
	 */
	public static final String BASENAME = "GeomDescRule-";

    /**
     * Keyword used to identify the name of this descriptor
     */
    public static final String KEYNAME = "NAME";
    
	/**
	 * Keywords that expect values and are used to annotate geometric descriptors.
	 */
	public static final List<String> DEFAULTVALUEDKEYS = Arrays.asList(KEYNAME);

	/**
	 * Keywords that do not expect values and are used to annotate geometric descriptors.
	 */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/MolecularMeter.json.
	public static final List<String> DEFAULTVALUELESSKEYS = Arrays.asList(
        AtomTupleConstants.KEYUSECURRENTVALUE,
        AtomTupleConstants.KEYONLYBONDED,
        AtomTupleConstants.KEYONLYINTERMOLECULAR);

//------------------------------------------------------------------------------

    /**
     * Constructor for a rule by parsing a formatted string of text. 
     * Default keywords that are interpreted to parse specific input
     * instructions are defined by
     * {@link GeomDescriptorDefinition#DEFAULTVALUEDKEYS} and 
     * {@link GeomDescriptorDefinition#DEFAULTVALUELESSKEYS}.
     * There defaults are added to the defaults of {@link AtomTupleMatchingRule}
     * namely,
     * {@link AtomTupleConstants#DEFAULTVALUEDKEYS} and 
     * {@link AtomTupleConstants#DEFAULTVALUELESSKEYS}.
     * @param txt the string to be parsed
     * @param i a unique integer used to identify the rule. Is used to build
     * the reference name of the generated rule.
     */

    public GeomDescriptorDefinition(String txt, int i)
    {
    	super(txt+" "+AtomTupleConstants.KEYUSECURRENTVALUE, BASENAME+i, 
    			DEFAULTVALUEDKEYS, DEFAULTVALUELESSKEYS);
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the flag defining if this rule applied only to
     * connected tuples of atoms.
     * @return <code>true</code> if the geometric descriptor defined by this rule 
     * is applied only to tuples of atoms that are connected.
     */
  	public boolean limitToBonded() 
  	{
  		return hasValuelessAttribute(AtomTupleConstants.KEYONLYBONDED);
  	}
    
//------------------------------------------------------------------------------

    /**
     * Returns the flag defining if this rule applied only to atoms that belong 
     * to different molecules (i.e., sets of continuously connected atoms).
     * @return <code>true</code> if the geometric descriptor defined by this rule 
     * is applied only to atoms that belong to different molecules.
     */
    public boolean limitToIntermolecular() 
    {
        return hasValuelessAttribute(AtomTupleConstants.KEYONLYINTERMOLECULAR);
    }
      
//------------------------------------------------------------------------------

}
