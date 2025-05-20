package autocompchem.molecule.connectivity;

import java.util.Arrays;
import java.util.List;

import org.openscience.cdk.interfaces.IBond;

import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
import autocompchem.molecule.BondEditor;
import autocompchem.molecule.MolecularUtils;
import autocompchem.smarts.SMARTS;
import autocompchem.utils.NumberUtils;


/**
 * Class adapting the general functionality of {@link AtomTupleMatchingRule}
 * to parse definition of bond editing task
 */
public class BondEditingRule extends AtomTupleMatchingRule
{

	/**
	 * Root of name used to identify any instance of this class.
	 */
	public static final String BASENAME = "BondEditRule-";
   
    /**
     * Keyword used to identify the imposed bond order value.
     */
    public static final String KEYORDER = "ORDER";
   
    /**
     * Keyword used to identify the stereochemistry descriptor value.
     */
    public static final String KEYSTEREO = "STEREO";
   
    /**
     * Value-less keyword used to identify bonds to remove.
     */
    public static final String KEYREMOVE = "REMOVE";
 
	/**
	 * Keywords that expect values and are used to annotate constraints.
	 */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/BondEditor.json.
	public static final List<String> DEFAULTVALUEDKEYS = Arrays.asList(
			KEYORDER, KEYSTEREO);

	/**
	 * Keywords that do not expect values and are used to annotate constraints.
	 */
    // WARNING: if you change this list you must update also the documentation
    // at the resource inputdefinition/BondEditor.json.
	public static final List<String> DEFAULTVALUELESSKEYS = Arrays.asList(
			KEYREMOVE);
	
//--------------------------------------------------------------------------

    /**
     * Constructor for a SMARTS -based rule defining what to do to the matched
     * bonds. Only one among <code>newOrder</code>, <code>newStereo</code>, and 
     * <code>remove</code> can be not-null or <code>true</code>.
	 * @param smarts the list of SMARTS identifying the bond or the atoms this
	 * rule intends to operate on.
	 * @param newOrder the new bond order to impose to matched bonds. Use
	 * <code>null</code> to ignore this parameter.
	 * @param newStereo the new stereo flag to impose to matched bonds. Use
	 * <code>null</code> to ignore this parameter.
	 * @param remove use <code>true</code> to request removal of matched bonds.
     * @param i a unique integer used to identify the rule. Is used to build
     * the reference name of the generated rule.
	 */

    public BondEditingRule(SMARTS[] smarts, IBond.Order newOrder, 
    		IBond.Stereo newStereo, boolean remove, int i)
    {
    	super(BASENAME+i, smarts);
    	if (newOrder!=null)
    	{
    		setValuedAttribute(KEYORDER, newOrder.toString());
    	}
    	if (newStereo!=null)
    	{
    		setValuedAttribute(KEYSTEREO, newStereo.toString());
    	}
    	if (remove)
    	{
    		setValuelessAttribute(KEYREMOVE);
    	}
    }
    
//--------------------------------------------------------------------------

    /**
     * Constructor for a index-based rule defining what to do to the matched
     * bonds. Only one among <code>newOrder</code>, <code>newStereo</code>, and 
     * <code>remove</code> can be not-null or <code>true</code>.
	 * @param ids the atom indexed identifying the bond or the atoms this
	 * rule intends to operate on.
	 * @param newOrder the new bond order to impose to matched bonds. Use
	 * <code>null</code> to ignore this parameter.
	 * @param newStereo the new stereo flag to impose to matched bonds. Use
	 * <code>null</code> to ignore this parameter.
	 * @param remove use <code>true</code> to request removal of matched bonds.
     * @param i a unique integer used to identify the rule. Is used to build
     * the reference name of the generated rule.
	 */

    public BondEditingRule(int[] ids, IBond.Order newOrder, 
    		IBond.Stereo newStereo, boolean remove, int i)
    {
    	super(BASENAME+i, ids);
    	if (newOrder!=null)
    	{
    		setValuedAttribute(KEYORDER, newOrder.toString());
    	}
    	if (newStereo!=null)
    	{
    		setValuedAttribute(KEYSTEREO, newStereo.toString());
    	}
    	if (remove)
    	{
    		setValuelessAttribute(KEYREMOVE);
    	}
    }
	
//--------------------------------------------------------------------------

    /**
     * Constructor for a rule by parsing a formatted string of text. 
     * Default keywords that are interpreted to parse specific input
     * instructions are defined by
     * {@link BondEditor#DEFAULTVALUEDKEYS} and 
     * {@link BondEditor#DEFAULTVALUELESSKEYS}.
     * @param txt the string to be parsed
     * @param i a unique integer used to identify the rule. Is used to build
     * the reference name of the generated rule.
     */

    public BondEditingRule(String txt, int i)
    {
    	super(txt, BASENAME+i, DEFAULTVALUEDKEYS, DEFAULTVALUELESSKEYS, 
    			true);
    }
    
//--------------------------------------------------------------------------
    
    /**
     * @return the objective of the editing task, i.e., the intended result 
     * on the bonds matched by this rule, or null, if no known objective is 
     * associated to this rule.
     */
    public Object getObjective()
    {
    	String bndOrderObjective = getValueOfAttribute(KEYORDER);
    	if (bndOrderObjective!=null)
    	{
    		if (NumberUtils.isParsableToInt(bndOrderObjective))
    		{
        		return MolecularUtils.intToBondOrder(
        				Integer.parseInt(bndOrderObjective));
    		} else {
    			return IBond.Order.valueOf(bndOrderObjective.toUpperCase());
    		}
    	}
    	
    	String stereoDscrpObjective = getValueOfAttribute(KEYSTEREO);
    	if (stereoDscrpObjective!=null)
    	{
    		return IBond.Stereo.valueOf(stereoDscrpObjective.toUpperCase());
    	}
    	
    	if (hasValuelessAttribute(KEYREMOVE))
    	{
    		return KEYREMOVE;
    	}
    	
    	return null;
    }

//--------------------------------------------------------------------------

}
