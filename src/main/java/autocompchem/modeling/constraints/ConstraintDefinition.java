package autocompchem.modeling.constraints;

import java.util.Arrays;
import java.util.List;

import autocompchem.modeling.atomtuple.AtomTupleConstants;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;


/**
 * Class representing the formatted definition of a rule to define 
 * {@link Constraint} from a given chemical structure.
 */

public class ConstraintDefinition extends AtomTupleMatchingRule
{
	/**
	 * Root of name used to identify any instance of this class.
	 */
	public static final String BASENAME = "CnstrRule-";

    /**
     * Keyword used to identify values
     */
    public static final String KEYVALUES = "VALUE";
    
    /**
     * Keyword used to flag the request to decouple the list of center 
     * identifiers
     * from the definition of an internal coordinate. When this is found the
     * constraint (whatever it is) will be applied to all atoms, and not to
     * the internal coordinate type defined by the number of identifiers (e,g,
     * distance from two ids, angle from 3, and so on).
     * For example, the constraint could be a potential applies to displacements 
     * from the initial position of the identified atoms.
     */
    public static final String KEYNOINTCOORD = "NOTANIC";
    
	/**
	 * Keywords that expect values and are used to annotate constraints.
	 */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/ConstraintsGenerator.json.
	public static final List<String> DEFAULTVALUEDKEYS = Arrays.asList(
			KEYVALUES, AtomTupleConstants.KEYPREFIX, 
			AtomTupleConstants.KEYSUFFIX);

	/**
	 * Keywords that do not expect values and are used to annotate constraints.
	 */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/ConstraintsGenerator.json.
	public static final List<String> DEFAULTVALUELESSKEYS = Arrays.asList(
			KEYNOINTCOORD);

//------------------------------------------------------------------------------

    /**
     * Constructor for a rule by parsing a formatted string of text. 
     * Default keywords that are interpreted to parse specific input
     * instructions are defined by
     * {@link ConstraintDefinition#DEFAULTVALUEDKEYS} and 
     * {@link ConstraintDefinition#DEFAULTVALUELESSKEYS}.
     * There defaults are added to the defaults of {@link AtomTupleMatchingRule}
     * namely,
     * {@link AtomTupleConstants#DEFAULTVALUEDKEYS} and 
     * {@link AtomTupleConstants#DEFAULTVALUELESSKEYS}.
     * @param txt the string to be parsed
     * @param i a unique integer used to identify the rule. Is used to build
     * the reference name of the generated rule.
     */

    public ConstraintDefinition(String txt, int i)
    {
    	super(txt, BASENAME+i, DEFAULTVALUEDKEYS, DEFAULTVALUELESSKEYS);
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the value associated to constraints defined by this rule
     * @return the value
     */

    public double getValue()
    {
    	return Double.parseDouble(getValueOfAttribute(KEYVALUES));
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string collecting prefix
     */

    public String getPrefix()
    {
        return getValueOfAttribute(AtomTupleConstants.KEYPREFIX);
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string collecting suffix
     */

    public String getSuffix()
    {
        return getValueOfAttribute(AtomTupleConstants.KEYSUFFIX);
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the flag defining if this rule applied only to linearly
     * connected tuples of atoms.
     * @return <code>true</code> if this constraints defined by this rule 
     * are applied only to tuples of atoms that are linearly connected.
     */
  	public boolean limitToBonded() 
  	{
  		return hasValuelessAttribute(AtomTupleConstants.KEYONLYBONDED);
  	}
    	
//------------------------------------------------------------------------------

    /**
     * Returns the flag defining if this rule makes use of the a value.
     * @return <code>true</code> if this constraints defined by this rule use
     * a value.
     */
  	public boolean hasValue()
  	{
  		return hasValuelessAttribute(AtomTupleConstants.KEYUSECURRENTVALUE)
  				|| getValueOfAttribute(AtomTupleConstants.KEYCURRENTVALUE)!=null
  				|| getValueOfAttribute(KEYVALUES)!=null;
  	}
  	
//------------------------------------------------------------------------------

    /**
     * Returns the flag defining if this rule makes use of the current value
     * as the value set in the constraint.
     * @return <code>true</code> if this constraints defined by this rule use
     * as value of the generated constraints the value found in the system to 
     * which this rule is applied.
     */
  	public boolean usesCurrentValue()
  	{
  		return hasValuelessAttribute(AtomTupleConstants.KEYUSECURRENTVALUE);
  	}

//------------------------------------------------------------------------------

}
