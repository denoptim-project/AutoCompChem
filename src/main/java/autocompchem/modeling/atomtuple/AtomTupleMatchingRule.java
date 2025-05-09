package autocompchem.modeling.atomtuple;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.run.Terminator;
import autocompchem.smarts.SMARTS;
import autocompchem.utils.NumberUtils;

/**
 * Class representing any rule that defines how to generate tuples of atom 
 * identifiers and associate them with custom attributes.
 * 
 * @author Marco Foscato
 */

public class AtomTupleMatchingRule 
{
    /**
     * List of keywords defining which attributes may be found in the 
     * formatted string defining this atom matching rule.
     */
    private List<String> attributeKeywords = new ArrayList<String>();
    
    /**
     * Attribute-defining keywords that have no value as their own existence 
     * is meaningful.
     */
    private Set<String> valuelessAttributes = new HashSet<String>();
    
    /**
     * Attribute-defining keywords that take values and are thus mapped to their
     * value.
     */
    private Map<String,String> valuedAttributes = new HashMap<String,String>();
    
	/**
     * Reference name 
     */
    private String refName = "noname";

    /**
     * Types of rules
     */
    public enum RuleType {SMARTS, ID, UNDEFINED}
    
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

//------------------------------------------------------------------------------

    /**
     * Constructor for a SMARTS-based rule. Any attribute has to be added after
     * constructions.
     * @param name the reference name of this rule.
     * @param smarts the list of SMARTS for matching atoms.
     */
    public AtomTupleMatchingRule(String name, SMARTS[] smarts)
    {
    	this.refName = name;
    	this.type = RuleType.SMARTS;
    	this.smartsQry = new ArrayList<SMARTS>(Arrays.asList(smarts));
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an ID-based rule. Any attribute has to be added after
     * constructions.
     * @param name the reference name of this rule.
     * @param ids the atom indexes (0-based).
     */
    public AtomTupleMatchingRule(String name, int[] ids)
    {
    	this.refName = name;
    	this.type = RuleType.ID;
    	this.idsQry = new ArrayList<Integer>();
    	for (int i=0; i< ids.length; i++)
    		this.idsQry.add(ids[i]);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a rule by parsing a formatted string of text. 
     * Default keywords that are recognized in the syntax are defined in
     * {@link AtomTupleConstants#DEFAULTVALUEDKEYS} and 
     * {@link AtomTupleConstants#DEFAULTVALUELESSKEYS}.
     * @param txt the string to be parsed.
     * @param ruleName a unique name used to identify this rule. We do not check
     * for uniqueness.
     */

    public AtomTupleMatchingRule(String txt, String ruleName)
    {
    	this(txt, ruleName, new ArrayList<String>(), new ArrayList<String>());
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a rule by parsing a formatted string of text. 
     * Default keywords that are recognized in the syntax are defined in
     * {@link AtomTupleConstants#DEFAULTVALUEDKEYS} and 
     * {@link AtomTupleConstants#DEFAULTVALUELESSKEYS}.
     * @param txt the string to be parsed.
     * @param ruleName a unique name used to identify this rule. We do not check
     * for uniqueness.
     * @param valuedKeywords list of keywords expected to have a value.
     * @param booleanKeywords list of keywords expected to have no value, 
     * i.e., their presence is sufficient to convey meaning.
     */

    public AtomTupleMatchingRule(String txt, String ruleName, 
    		List<String> valuedKeywords, List<String> booleanKeywords)
    {
    	this(txt, ruleName, valuedKeywords, booleanKeywords, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a rule by parsing a formatted string of text. 
     * @param txt the string to be parsed.
     * @param ruleName a unique name used to identify this rule. We do not check
     * for uniqueness.
     * @param valuedKeywords list of keywords expected to have a value.
     * @param booleanKeywords list of keywords expected to have no value, 
     * i.e., their presence is sufficient to convey meaning.
     * @param excludeDefaultKeys use <code>true</code> to exclude the default
     * keywords that are defined in
     * {@link AtomTupleConstants#DEFAULTVALUEDKEYS} and 
     * {@link AtomTupleConstants#DEFAULTVALUELESSKEYS}. 
     */

    public AtomTupleMatchingRule(String txt, String ruleName, 
    		List<String> valuedKeywords, List<String> booleanKeywords, 
    		boolean excludeDefaultKeys)
    {
        this.refName = ruleName;
        
        // These are default attribute keywords that we always have
        if (!excludeDefaultKeys)
        {
	        for (String defValuedKey : AtomTupleConstants.DEFAULTVALUEDKEYS)
	        {
	        	if (!attributeKeywords.contains(defValuedKey))
	    			attributeKeywords.add(defValuedKey.toUpperCase());
	        }
        }
        if (valuedKeywords!=null)
        {
        	for (String key : valuedKeywords)
        	{
        		if (!attributeKeywords.contains(key))
        			attributeKeywords.add(key.toUpperCase());
        	}
        }
        
        Map<String,Boolean> booleanAttributes = new HashMap<String,Boolean>();
    	// These are default keywords that we always have
        if (!excludeDefaultKeys)
        {
	        for (String defValuelessKey : 
	        	AtomTupleConstants.DEFAULTVALUELESSKEYS)
	        {
	        	if (!attributeKeywords.contains(defValuelessKey))
	        	{
		        	booleanAttributes.put(defValuelessKey.toUpperCase(), false);
		            attributeKeywords.add(defValuelessKey.toUpperCase());
	        	}
	        }
        }
        if (booleanKeywords!=null)
        {
        	for (String key : booleanKeywords)
        	{
        		if (!attributeKeywords.contains(key))
		        {
		        	attributeKeywords.add(key.toUpperCase());
		        	booleanAttributes.put(key.toUpperCase(), false);
		        }
        	}
        }
        
        String[] p = txt.trim().split("\\s+");
        if (p.length < 1)
        {
            Terminator.withMsgAndStatus("ERROR! Not enough words to make a "
            		+ "rule for matching atom tuples. Check line '" + txt 
            		+ "'.", -1);
        }
        List<String> parts = new ArrayList<String>(Arrays.asList(p));
        
    	Iterator<String> partsReader = parts.iterator();
    	boolean readingIDs = false;
    	boolean readingSMARTS = false;
    	String activeAttribute = null;
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
    	
    	LoopOverWords:
    	while (partsReader.hasNext())
    	{
    		String word = partsReader.next();
    		for (String key : attributeKeywords)
    		{
				if (word.toUpperCase().startsWith(key.toUpperCase()))
	        	{
	        		readingIDs = false;
	        		readingSMARTS = false;
	        		if (booleanAttributes.containsKey(key))
	        		{
	        			booleanAttributes.put(key, true);
	        			continue LoopOverWords;
	        		} else {
	        			activeAttribute = key;
		        		word = word.substring(key.length());
		        		word = word.trim();
		        		if (word.startsWith(":") || word.startsWith("="))
		        		{
		        			word = word.substring(1).trim();
		        		}
		        		if (word.length()==0)
		        			continue LoopOverWords;
		        		break;
	        		}
	        	}
    		}
    		// check for consistency with expectation: we are either reading an
    		// atom identifier (SMARTS or index) or a keyword.
    		if (readingIDs && !NumberUtils.isParsableToInt(word)) {
				Terminator.withMsgAndStatus("Wrong syntax in "
        				+ "line '" + txt + "'. Word '" + word 
        				+ "' is unexpected.", -1);
            }
    		
        	if (readingIDs)
        	{
        		this.idsQry.add(Integer.parseInt(word));
        	} else if (readingSMARTS)
        	{
        		this.smartsQry.add(new SMARTS(word));
        	} else if (activeAttribute!=null)
        	{
        		if (valuedAttributes.containsKey(activeAttribute))
        		{
        			valuedAttributes.put(activeAttribute, 
        					valuedAttributes.get(activeAttribute) + " " + word);
        		} else {
        			valuedAttributes.put(activeAttribute, word);
        		}
        	}
    	}
    	
    	for (String key : booleanAttributes.keySet())
    	{
    		if (booleanAttributes.get(key))
    			valuelessAttributes.add(key);
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Returns the name of this rule.
     * @return the reference name.
     */

    public String getRefName()
    {
        return refName;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the type of this rule: whether it is based on SMARTS or atom 
     * indexes.
     * @return the type.
     */

    public RuleType getType()
    {
        return type;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the SMARTS this rule used to identify atoms.
     * @return the list of SMARTS queries of this rule. <code>null</code> 
     * if this rule does not use SMARTS.
     */
    
    public ArrayList<SMARTS> getSMARTS()
    {
    	return smartsQry;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the atom indexes this rule used to identify atoms.
     * @return the list of atom IDs queries of this rule. <code>null</code> 
     * if this rule does not uses indexes.
     */
    
    public ArrayList<Integer> getAtomIDs()
    {
    	return idsQry;
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets a value-less attribute in this rule.
     * @param key the name of the attribute to set.
     */

    public void setValuelessAttribute(String key)
    {
    	valuelessAttributes.add(key);
    }
    
//------------------------------------------------------------------------------

    /**
     * Remove a value-less attribute in this rule.
     * @param key the name of the attribute to remove
     */

    public void removeValuelessAttribute(String key)
    {
    	valuelessAttributes.remove(key);
    }
    
//------------------------------------------------------------------------------

    /**
     * @param key the name of the attribute to get.
     * @return <code>true</code> if the keyword was found, <code>false</code> if
     * it was not found.
     */

    public boolean hasValuelessAttribute(String key)
    {
        return valuelessAttributes.contains(key);
    }

//------------------------------------------------------------------------------

    /**
     * Set an attribute with a value. Overwrites any existing attribute with the 
     * same keyword.
     * @param key the keyword of the attribute to add.
     * @param value the value of the attribute.
     */

    public void setValuedAttribute(String key, String value)
    {
    	valuedAttributes.put(key, value);
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the value associated to an attribute of this rule.
     * @param key the name of the attribute to get.
     * @return the value or <code>null</code> if the attribute was not found or 
     * it is not associated with any value (i.e., if it is boolean attribute,
     * then you can use 
     * {@link AtomTupleMatchingRule#isBooleanAttributeFound(String)}).
     */

    public String getValueOfAttribute(String key)
    {
        return valuedAttributes.get(key);
    }
  	
//------------------------------------------------------------------------------

  	/**
  	 * Creates an atom tuple combining the attributes defined in this rule with 
  	 * the given list of atom. Note that new instances of each 
  	 * attribute and value are created to be assigned to the tuple instance.
  	 * @param atoms the tuple of atoms. Only the index value is taken.
  	 * @param mol the container of the atoms in the tuple.
  	 * @return the atom tuple decorated by the attributes defined in this rule.
  	 */
    
  	public AnnotatedAtomTuple makeAtomTupleFromIDs(List<IAtom> atoms,
  			IAtomContainer mol) 
  	{	
  		Set<String> myValueless = new HashSet<String>(valuelessAttributes);
		
  		Map<String, String> myValued = new HashMap<String, String>();
		for (String key : valuedAttributes.keySet())
			myValued.put(key.toUpperCase(), valuedAttributes.get(key));
		
		return new AnnotatedAtomTuple(atoms, null, mol, myValueless, myValued);
  	}

//------------------------------------------------------------------------------	

}
