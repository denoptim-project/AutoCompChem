package autocompchem.molecule.conformation;

import java.util.Arrays;
import java.util.List;

import autocompchem.modeling.atomtuple.AtomTupleConstants;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
import autocompchem.utils.StringUtils;

/**
 * Class representing the formatted definition of a rule to define 
 * {@link ConformationalCoordinate} from a given chemical structure.
 */

public class ConformationalCoordDefinition extends AtomTupleMatchingRule
{
	/**
	 * Root of name used to identify any instance of this class.
	 */
	public static final String BASENAME = "ConfCoord-";

    /**
     * Keyword used to identify numerical values other than the fold number.
     */
    public static final String KEYVALUES = "VALUES";
   
    /**
     * Keyword used to identify the fold number.
     */
    public static final String KEYFOLD = "FOLD";
    
    /**
     * Keyword used to identify prefixes
     */
    public static final String KEYPREFIX = "PREFIX";
    
    /**
     * Keyword used to identify suffix
     */
    public static final String KEYSUFFIX= "SUFFIX";
    
	/**
	 * Keywords that expect values and are used to annotate constraints.
	 */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/ConformationalSpaceGenerator.json.
	public static final List<String> DEFAULTVALUEDKEYS = Arrays.asList(
			KEYVALUES, KEYFOLD, KEYPREFIX, KEYSUFFIX);

	/**
	 * Keywords that do not expect values and are used to annotate constraints.
	 */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/ConformationalSpaceGenerator.json.
	public static final List<String> DEFAULTVALUELESSKEYS = Arrays.asList();

//------------------------------------------------------------------------------

    /**
     * Constructor for a rule by parsing a formatted string of text. 
     * Default keywords that are interpreted to parse specific input
     * instructions are defined by
     * {@link ConformationalCoordDefinition#DEFAULTVALUEDKEYS} and 
     * {@link ConformationalCoordDefinition#DEFAULTVALUELESSKEYS}.
     * There defaults are added to the defaults of {@link AtomTupleMatchingRule}
     * namely,
     * {@link AtomTupleConstants#DEFAULTVALUEDKEYS} and 
     * {@link AtomTupleConstants#DEFAULTVALUELESSKEYS}.
     * @param txt the string to be parsed
     * @param i a unique integer used to identify the rule. Is used to build
     * the reference name of the generated rule.
     */

    public ConformationalCoordDefinition(String txt, int i)
    {
    	super(txt, BASENAME+i, DEFAULTVALUEDKEYS, DEFAULTVALUELESSKEYS);
    	this.setValuelessAttribute(AtomTupleConstants.KEYONLYBONDED);
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the value associated to constraints defined by this rule
     * @return the value
     */

    public double[] getValues()
    {
    	return StringUtils.parseArrayOfDoubles(getValueOfAttribute(KEYVALUES),
    			"\\s+");
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string collecting prefix
     */

    public String getPrefix()
    {
        return getValueOfAttribute(KEYPREFIX);
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string collecting suffix
     */

    public String getSuffix()
    {
        return getValueOfAttribute(KEYSUFFIX);
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
