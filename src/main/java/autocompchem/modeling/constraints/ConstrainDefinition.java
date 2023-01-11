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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import autocompchem.run.Terminator;
import autocompchem.smarts.SMARTS;
import autocompchem.utils.NumberUtils;

public class ConstrainDefinition 
{

    /**
     * Keyword used to identify values
     */
    private static final String KEYVALUES = "VALUE:";
    
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
     * Keyword used to identify prefixes
     */
    private static final String KEYPREFIX = "PREFIX:";
    
    /**
     * Keyword used to identify suffix
     */
    private static final String KEYSUFFIX= "SUFFIX:";

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
	 * A given optional string that is not marked to be a prefix or a suffix.
	 * Examples are the options
	 * telling the comp. chem. software what to do with this constraints, i.e.,
	 * Gaussian's "A" for activate (remove constraint) and "F" for freeze 
	 * (add constraint). 
	 */
	private String options;
	
	/**
	 * A given optional string that is marked to be a prefix.
	 * Examples are the keywords that need to be written before the tuple of
	 * atom pointers.
	 */
	private String prefix;
	
	/**
	 * A given optional string that is marked to be a suffix.
	 * Examples are the keywords that need to be written after the tuple of
	 * atom pointers.
	 */
	private String suffix;

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
        List<String> parts = new ArrayList<String>(Arrays.asList(p));
        String msg = "ERROR! The following string does not look like a "
        		+ "properly formatted rule for constraints generation. ";
        
        if (p.length < 1)
        {
            Terminator.withMsgAndStatus(msg + "Not enough words to make a "
            		+ " constraint defining rule. Check line " + txt, -1);
        }
        
        this.refName = "CnstrRule-"+i;
        
    	Iterator<String> partsReader = parts.iterator();
    	boolean readingIDs = false;
    	boolean readingSMARTS = false;
    	boolean readingValues = false;
    	boolean readingOpts = false;
    	boolean readingPrefix = false;
    	boolean readingSuffix = false;
    	
        //The first string distinguished between SMARTS (i.e., a alphanumeric
        // string) and atom IDs (i.e., an integer).
    	if (NumberUtils.isNumber(p[0]))
        {
    		this.type = RuleType.ID;
         	this.idsQry = new ArrayList<Integer>();
         	readingIDs = true;
        } else {
        	this.type = RuleType.SMARTS;
        	this.smartsQry = new ArrayList<SMARTS>();
        	readingSMARTS = true;
        }
    	
    	while (partsReader.hasNext())
    	{
    		String word = partsReader.next();
			if (word.toUpperCase().startsWith(KEYVALUES))
        	{
        		readingIDs = false;
        		readingSMARTS = false;
            	readingValues = true;
            	readingOpts = false;
            	readingPrefix = false;
            	readingSuffix = false;
        		this.hasValue = true;
        		word = word.substring(KEYVALUES.length());
        		word = word.trim();
        		if (word.length()==0)
        			continue;
        	} else if (word.toUpperCase().startsWith(KEYOPTIONS))
        	{
        		readingIDs = false;
        		readingSMARTS = false;
            	readingValues = false;
            	readingOpts = true;
            	readingPrefix = false;
            	readingSuffix = false;
            	word = word.substring(KEYOPTIONS.length());
        		word = word.trim();
        		if (word.length()==0)
        			continue;
        	} else if (word.toUpperCase().startsWith(KEYPREFIX))
        	{
        		readingIDs = false;
        		readingSMARTS = false;
            	readingValues = false;
            	readingOpts = false;
            	readingPrefix = true;
            	readingSuffix = false;
            	word = word.substring(KEYPREFIX.length());
        		word = word.trim();
        		if (word.length()==0)
        			continue;
        	} else if (word.toUpperCase().startsWith(KEYSUFFIX))
        	{
        		readingIDs = false;
        		readingSMARTS = false;
            	readingValues = false;
            	readingOpts = false;
            	readingPrefix = false;
            	readingSuffix = true;
            	word = word.substring(KEYSUFFIX.length());
        		word = word.trim();
        		if (word.length()==0)
        			continue;
        	} else if (PARONLYBONDED.equals(word.toUpperCase()))
    		{
        		readingIDs = false;
        		readingSMARTS = false;
        		this.onlyBonded = true;
        		continue;
    		} else if (PARNOINTCOORD.equals(word.toUpperCase()))
    		{
        		readingIDs = false;
        		readingSMARTS = false;
        		this.notAnIC = true;
        		continue;
    		} else if (PARCURRENTVALUE.equals(word.toUpperCase()))
    		{
        		readingIDs = false;
        		readingSMARTS = false;
        		this.hasValue = true;
        		this.useCurrentValue = true;
        		continue;
    		} else if (readingIDs && !NumberUtils.isParsableToInt(word)) {
    			if (word.contains(":"))
    			{
    				Terminator.withMsgAndStatus("Wrong syntax in line '" 
    						+ txt + "'. Word '" + word + "' is unexpected "
            				+ "and contains ':'. "
            				+ "Perhaps a space is missing after ':'?", -1);
    			} else {
    				Terminator.withMsgAndStatus("Wrong syntax in "
            				+ "line '" + txt + "'. Word '" + word 
            				+ "' is unexpected.", -1);
    			}
            }
    		
        	if (readingIDs)
        	{
        		this.idsQry.add(Integer.parseInt(word));
        	} else if (readingSMARTS)
        	{
        		this.smartsQry.add(new SMARTS(word));
        	} else if (readingValues)
        	{
        		this.value = Double.parseDouble(word);
        	} else if (readingOpts)
        	{
        		if (this.options!=null)
        		{
        			this.options = this.options + " " + word;
        		} else {
        			this.options = word;
        		}
        	} else if (readingPrefix )
        	{
        		if (this.prefix!=null)
        		{
        			this.prefix = this.prefix + " " + word;
        		} else {
        			this.prefix = word;
        		}
        	} else if (readingSuffix)
        	{
        		if (this.suffix!=null)
        		{
        			this.suffix = this.suffix + " " + word;
        		} else {
        			this.suffix = word;
        		}
        	} else {
        		Terminator.withMsgAndStatus("Wrong syntax in "
        				+ "line '" + txt + "'. Word '" + word 
        				+ "' could not be interpreted.", -1);
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
     * @return the list of SMARTS queries of this rule. Null if none define or
     * rule does not uses SMARTS.
     */
    
    public ArrayList<SMARTS> getSMARTS()
    {
    	return smartsQry;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the list of atom IDs queries of this rule. Null if none define or
     * rule does not uses indexes.
     */
    
    public ArrayList<Integer> getAtomIDs()
    {
    	return idsQry;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the value associated to constraints defined by this rule
     * @return the value
     */

    public double getValue()
    {
        return value;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string collecting options
     */

    public String getOpts()
    {
        return options;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string collecting prefix
     */

    public String getPrefix()
    {
        return prefix;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string collecting suffix
     */

    public String getSuffix()
    {
        return suffix;
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
     * Returns the flag defining if this rule makes use of the a value.
     * @return <code>true</code> if this constraints defined by this rule use
     * a value.
     */
  	public boolean hasValue()
  	{
  		return hasValue;
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

    /**
     * Returns the flag defining if this rule is meant to match tuples that
     * may not correspond to internal coordinates.
     * @return <code>true</code> if this rule is meant to match tuples that
     * may not correspond to internal coordinates.
     */
  	public boolean notAnIC()
  	{
  		return notAnIC;
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
 	
  		if (!hasValue)
  		{
  			return Constraint.buildConstraint(idsList, null, options,
  					areLinearlyConnected, prefix, suffix, notAnIC);
  		} else {
  			if (useCurrentValue)
  			{
  				return Constraint.buildConstraint(idsList, 
  						currentValue.doubleValue(), options,
  						areLinearlyConnected, prefix, suffix, notAnIC);
  			} else {
  				return Constraint.buildConstraint(idsList, value, options,
	  					areLinearlyConnected, prefix, suffix, notAnIC);
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
