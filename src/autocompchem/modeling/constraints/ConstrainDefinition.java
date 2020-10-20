package autocompchem.modeling.constraints;

import java.util.ArrayList;

import autocompchem.run.Terminator;
import autocompchem.smarts.SMARTS;
import autocompchem.utils.NumberUtils;

public class ConstrainDefinition 
{
	/**
	 * Parameter used to flag the request of considering only tuples of atoms 
	 * that are bonded in the order given by the tuple.
	 */
    private static final Object PARONLYBONDED = "ONLYBONDED";

	/**
     * Reference name 
     */
    private String refName = "noname";

    /**
     * Types of rules
     */
    protected enum RuleType {SMARTS, ID, UNDEFINED}
    
    /**
     * Rule type
     */
    private RuleType type = RuleType.UNDEFINED;

    /**
     * The rule's SMARTS query
     */
    private ArrayList<SMARTS> smarts;
    
    /**
     * Type rules IDs
     */
    private ArrayList<Integer> ids;
    
	/**
	 * A given value for this constraint
	 */
	private double value;
	
	/**
	 * Flag signaling that this rule defines value-based constraints
	 */
	private boolean hasValue = false;
	
	/**
	 * Flag specifying that the tupla of atoms must be a bonded set
	 */
	private boolean onlyBonded = false;

//------------------------------------------------------------------------------

    /**
     * Constructor for a rule by parsing a formatted string of text. 
     * @param txt the string to be parsed
     * @param i a unique integer used to identify the rule. Is used to build
     * the reference name of the generated rule.
     */

    public ConstrainDefinition(String txt, int i)
    {
        String[] p = txt.split("\\s+");
        String msg = "ERROR! The following string does not look like a "
        		+ "properly formatted rule for constraints generation. ";
        
        if (p.length < 1)
        {
            Terminator.withMsgAndStatus(msg + "Not enough words to make a "
            		+ " constraint defining rule. Check line " + txt,-1);
        }
        
        this.refName = "CnstrRule-"+i;
        if (NumberUtils.isNumber(p[0]))
        {
        	this.type = RuleType.ID;
        	this.ids = new ArrayList<Integer>();
            for (int j=0; j<p.length; j++)
            {
            	this.ids.add(Integer.parseInt(p[j]));
            }
        } else {
        	this.type = RuleType.SMARTS;
        	this.smarts = new ArrayList<SMARTS>();
        	boolean endOfSmarts = false;
            for (int j=0; j<p.length; j++)
            {
            	if (NumberUtils.isNumber(p[j]))
            	{
            		// WARNING! For now we expect only one numerical value
            		// So, the last numerical we find is going to be the value.
            		endOfSmarts = true;
            		this.hasValue = true;
            		this.value = Double.parseDouble(p[j]);
            	} else if (PARONLYBONDED.equals(p[j].toUpperCase())) {
            		endOfSmarts = true;
            		this.onlyBonded = true;
            	} else {
            		if (!endOfSmarts)
            		{
            			this.smarts.add(new SMARTS(p[j]));
            		}
            	}
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Returns the reference name
     * @return the reference name
     */

    public String getRefName()
    {
        return refName;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the type of this rule
     * @return the type
     */

    public RuleType getType()
    {
        return type;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the list of SMARTS included in this rule
     */
    
    public ArrayList<SMARTS> getSMARTS()
    {
    	return smarts;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the list of atom IDs included in this rule
     */
    
    public ArrayList<Integer> getAtomIDs()
    {
    	return ids;
    }
    
//------------------------------------------------------------------------------

  	public boolean limitToBonded() 
  	{
  		return onlyBonded;
  	}
  	
//------------------------------------------------------------------------------

  	public Constraint makeConstraintFromIDs(ArrayList<Integer> arrayList) 
  			throws Exception 
  	{
  		if (!hasValue)
  			return Constraint.buildConstraint(arrayList);
  		else 
  			return Constraint.buildConstraint(arrayList, value);
  	}

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(refName).append(" [");
        sb.append(type).append(" = ");
        if (type == RuleType.SMARTS)
        {
        	sb.append(smarts).append("] ");
        } else {
        	sb.append(ids).append("] ");
        }
        return sb.toString();
    }

//------------------------------------------------------------------------------

	

}
