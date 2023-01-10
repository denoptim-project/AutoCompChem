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
	 * Keyword used to flag the request of considering only tuples of atoms 
	 * that are bonded in the order given by the tuple.
	 */
    private static final Object PARONLYBONDED = "ONLYBONDED";
    
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
    private static final String PARNOINTCOORD = "NOTANIC";
    
    /**
     * Keyword requiring to set the value of the constraint to the value 
     * detected in the given system (i.e., the current value).
     */
    private static final String PARCURRENTVALUE = "USECURRENTVALUE";
    
    /**
     * Keyword used to flag the identification of options.
     */
    private static final String KEYOPTIONS = "OPTIONS:";

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
	 * Flag signaling that this rule defines value-based constraints
	 */
	private boolean hasValue = false;
	
	/**
	 * Flag signaling that this rule used the value found in the system as the 
	 * value of the constraint.
	 */
	private boolean useCurrentValue = false;
	
	/**
	 * Flag specifying that the tuple of atoms must be a bonded set
	 */
	private boolean onlyBonded = false;
	
	/**
	 * Flag specifying that this tuple in not really a tuple, but an unordered
	 * collection that does not define an internal coordinate.
	 */
	private boolean notAnIC = false;
	
	/**
	 * A given optional setting for the constraint. Examples are the options
	 * telling the comp. chem. software what to do with this constraints, i.e.,
	 * Gaussian's "A" for activate (remove constraint) and "F" for freeze 
	 * (add constraint).
	 */
	private String options;

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
        	boolean readOpts = false;
            for (int j=0; j<p.length; j++)
            {
            	if (NumberUtils.isParsableToInt(p[j]) && !readOpts)
            	{
            		// Reading atom IDs
            		this.idsQry.add(Integer.parseInt(p[j]));
            	} else {
	            	if (NumberUtils.isParsableToDouble(p[j]) && !readOpts)
	            	{
	            		// Reading optional value
	            		// WARNING! For now we expect only one double value
	            		this.hasValue = true;
	            		this.value = Double.parseDouble(p[j]);
	            	} else if (PARONLYBONDED.equals(p[j].toUpperCase()) 
	            			&& !readOpts) {
	            		this.onlyBonded = true;
	            	} else if (PARNOINTCOORD.equals(p[j].toUpperCase()) 
	            			&& !readOpts) {
	            		this.notAnIC = true;
	            	} else if (PARCURRENTVALUE.equals(p[j].toUpperCase()) 
	            			&& !readOpts) {
	            		this.hasValue = true;
	            		this.useCurrentValue = true;
	            	} else {
	            		// Anything else is interpreted as an additional option,
	            		// i.e., the "A" or "F" of Gaussian constraints
	            		String s =  p[j];
	            		if (p[j].toUpperCase().startsWith(KEYOPTIONS))
	            			s = s.substring(KEYOPTIONS.length());
	            		if (this.options!=null)
	            		{
	            			this.options = this.options + " " + s;
	            		} else {
	            			this.options = s;
	            		}
	            	}
            	}
            }
        } else {
        	this.type = RuleType.SMARTS;
        	this.smartsQry = new ArrayList<SMARTS>();
        	boolean endOfSmarts = false;
        	boolean readOpts = false;
            for (int j=0; j<p.length; j++)
            {
            	if (NumberUtils.isNumber(p[j]) && !readOpts)
            	{
            		// WARNING! For now we expect only one numerical value
            		// So, the last numerical we find is going to be the value.
            		endOfSmarts = true;
            		this.hasValue = true;
            		this.value = Double.parseDouble(p[j]);
            	} else if (PARONLYBONDED.equals(p[j].toUpperCase()) && !readOpts) 
            	{
            		endOfSmarts = true;
            		this.onlyBonded = true;
            	} else if (PARNOINTCOORD.equals(p[j].toUpperCase()) && !readOpts) 
            	{
            		endOfSmarts = true;
            		this.notAnIC = true;
            	} else if (PARCURRENTVALUE.equals(p[j].toUpperCase()) && !readOpts) 
            	{
            		endOfSmarts = true;
            		this.hasValue = true;
            		this.useCurrentValue = true;
            	} else {
            		String s = p[j];
            		if (p[j].toUpperCase().startsWith(KEYOPTIONS) || readOpts)
            		{
            			endOfSmarts = true;
            			readOpts = true;
            			if (p[j].toUpperCase().startsWith(KEYOPTIONS))
            				s = s.substring(KEYOPTIONS.length());
            			if (this.options!=null)
	            		{
	            			this.options = this.options + " " + s;
	            		} else {
	            			this.options = s;
	            		}
            		}
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

    /**
     * Returns the flag defining if this rule applied only to linearly
     * connected tuples of atoms.
     * @return <code>true</code> if this constraints defined by this rule 
     * are applied only to tuples of atoms that are linearly connected.
     */
  	public boolean limitToBonded() 
  	{
  		return onlyBonded;
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
  		return useCurrentValue;
  	}
  	
//------------------------------------------------------------------------------

  	public Constraint makeConstraint(boolean areLinearlyConnected) 
  			throws Exception 
  	{
  		return makeConstraintFromIDs(idsQry, areLinearlyConnected, null);
  	}
  	
//------------------------------------------------------------------------------

  	/**
  	 * Creates a constraint from atom indexes.
  	 * @param ids the list of indexes
  	 * @param areLinearlyConnected use <code>true</code> is the given IDs 
	 * represent a set of centers that are connected in the order given, e.g.
	 * i-j-k-l.
  	 * @param currentValue the value of the internal coordinate defined by the
  	 * given set of IDs. Use null to ignore this parameter.
  	 * @return the constraint constructed from the given parameters and 
  	 * according to the present constrain definition.
  	 * @throws Exception
  	 */
  	//TODO-gg private?
  	public Constraint makeConstraintFromIDs(ArrayList<Integer> idsList, 
  			boolean areLinearlyConnected, Double currentValue) 
  			throws Exception 
  	{
  		if (currentValue==null && useCurrentValue)
  			throw new IllegalArgumentException("Request to use current value "
  					+ "to make constraints, but given current value is null");
  		
  		if (notAnIC)
  		{
  			// type will be undefined
  			Constraint c = new Constraint();
  			int[] ids = new int[idsList.size()];
  			for (int i=0; i<idsList.size(); i++)
  			{
  				ids[i] = idsList.get(i);
  			}
  			c.setAtomIDs(ids);
  			if (hasValue)
  			{
  				if (useCurrentValue)
  					c.setValue(currentValue.doubleValue());
  				else
  					c.setValue(value);
  			}
  			return c;
  		} else {
	  		if (!hasValue)
	  		{
	  			return Constraint.buildConstraint(idsList, null, options,
	  					areLinearlyConnected);
	  		} else {
	  			if (useCurrentValue)
	  			{
	  				return Constraint.buildConstraint(idsList, 
	  						currentValue.doubleValue(), options,
	  						areLinearlyConnected);
	  			} else {
	  				return Constraint.buildConstraint(idsList, value, options,
		  					areLinearlyConnected);
	  			}
	  		}
  		}
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
        sb.append(", [options = ").append(options).append("]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

	

}
