package autocompchem.modeling.constraints;

/*
 *   Copyright (C) 2016  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    private ArrayList<SMARTS> smartsQry;
    
    /**
     * The rules atom IDs query
     */
    private ArrayList<Integer> idsQry;
    
	/**
	 * A given value for this constraint
	 */
	private double value;
	
	/**
	 * Flag signalling that this rule defines value-based constraints
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
        String[] p = txt.trim().split("\\s+");
        String msg = "ERROR! The following string does not look like a "
        		+ "properly formatted rule for constraints generation. ";
        
        if (p.length < 1)
        {
            Terminator.withMsgAndStatus(msg + "Not enough words to make a "
            		+ " constraint defining rule. Check line " + txt,-1);
        }
        
        this.refName = "CnstrRule-"+i;
        
        //The first string distinguished between SMARTS (i.e., a alphanumeric
        // string) and atom IDs (i.e., an integer).
        
        if (NumberUtils.isNumber(p[0]))
        {
        	this.type = RuleType.ID;
        	this.idsQry = new ArrayList<Integer>();
            boolean endOfIDs = false;
            for (int j=0; j<p.length; j++)
            {
            	if (!NumberUtils.isParsableToInt(p[j]) 
            			&& NumberUtils.isParsableToDouble(p[j]))
            	{
            		// WARNING! For now we expect only one double value
            		// So, the last double we find is going to be The one.
            		endOfIDs = true;
            		this.hasValue = true;
            		this.value = Double.parseDouble(p[j]);
            	} else if (PARONLYBONDED.equals(p[j].toUpperCase())) {
            		endOfIDs = true;
            		this.onlyBonded = true;
            	} else {
            		//TODO: deal with wildcards
            		if (!endOfIDs)
            		{
            			this.idsQry.add(Integer.parseInt(p[j]));
            		}
            	}
            }
        } else {
        	this.type = RuleType.SMARTS;
        	this.smartsQry = new ArrayList<SMARTS>();
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
            			this.smartsQry.add(new SMARTS(p[j]));
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
     * @return the list of SMARTS queries of this rule
     */
    
    public ArrayList<SMARTS> getSMARTS()
    {
    	return smartsQry;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the list of atom IDs queries of this rule
     */
    
    public ArrayList<Integer> getAtomIDs()
    {
    	return idsQry;
    }
    
//------------------------------------------------------------------------------

  	public boolean limitToBonded() 
  	{
  		return onlyBonded;
  	}

//------------------------------------------------------------------------------

  	public Constraint makeConstraint() throws Exception 
  	{
  		return makeConstraintFromIDs(idsQry);
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
        	sb.append(smartsQry).append("] ");
        } else {
        	sb.append(idsQry).append("]");
        }
        if (hasValue)
        {
        	sb.append(", [value = ").append(value).append("]");
        }
        return sb.toString();
    }

//------------------------------------------------------------------------------

	

}
