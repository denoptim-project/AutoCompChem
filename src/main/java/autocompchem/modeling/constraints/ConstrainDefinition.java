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

import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AtomTupleConstants;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
import autocompchem.molecule.conformation.ConformationalCoordinate;
import autocompchem.run.Terminator;
import autocompchem.smarts.SMARTS;
import autocompchem.utils.NumberUtils;


/**
 * Class representing the formatted definition of a rule to define 
 * {@link Constraint} from a given chemical structure.
 */

//TODO-gg rename to ConstraintDefinition (NB: the "t"!!!!!)

public class ConstrainDefinition extends AtomTupleMatchingRule
{
	/**
	 * Root of name used to identify any instance of this class.
	 */
	public static final String BASENAME = "CnstrRule-";

    /**
     * Keyword used to identify values
     */
    public static final String KEYVALUES = "VALUE";
    
    //TODO.gg is notAnIC needed? So far it avoid assigning the type to 
    // any among distance/angle/dihedral/inproperdihedral
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
    // at the resource inputdefinition/ConstraintsGenerator.json.
	public static final List<String> DEFAULTVALUEDKEYS = Arrays.asList(
			KEYVALUES, KEYPREFIX, KEYSUFFIX);

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
     * {@link ConstrainDefinition#DEFAULTVALUEDKEYS} and 
     * {@link ConstrainDefinition#DEFAULTVALUELESSKEYS}.
     * There defaults are added to the defaults of {@link AtomTupleMatchingRule}
     * namely,
     * {@link AtomTupleConstants#DEFAULTVALUEDKEYS} and 
     * {@link AtomTupleConstants#DEFAULTVALUELESSKEYS}.
     * @param txt the string to be parsed
     * @param i a unique integer used to identify the rule. Is used to build
     * the reference name of the generated rule.
     */

    public ConstrainDefinition(String txt, int i)
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
