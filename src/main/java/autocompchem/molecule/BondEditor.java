package autocompchem.molecule;

/*   
 *   Copyright (C) 2024  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AtomTupleGenerator;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
import autocompchem.run.Job;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Tool to add, modify, or remove one or more bonds.
 * 
 * @author Marco Foscato
 */

public class BondEditor extends AtomTupleGenerator
{
    /**
     * String defining the task of editing bonds.
	 * Does include adding and removing.
     */
    public static final String EDITBONDSTASKNAME = "editBonds";

    /**
     * Task about editing bonds
     */
    public static final Task EDITBONDSTASK;
    static {
    	EDITBONDSTASK = Task.make(EDITBONDSTASKNAME);
    }

	/**
     * String defining the task of removing bonds.
     * Redundant, but defined to ensure visibility of the task.
     */
	public static final String REMOVEBONDSTASKNAME = "removeBonds";

	/**
	 * Task about removing atoms
	 */
	public static final Task REMOVEBONDSTASK;
	static {
		REMOVEBONDSTASK = Task.make(REMOVEBONDSTASKNAME);
	}

	/**
	 * String defining the task of adding bonds
	 * Redundant, but defined to ensure visibility of the task.
	 */
	public static final String ADDBONDSTASKNAME = "addBonds";

	/**
	 * Task about adding an atom
	 */
	public static final Task ADDBONDSTASK;
	static {
		ADDBONDSTASK = Task.make(ADDBONDSTASKNAME);
	}

	/**
	 * Root of name used to identify any instance of this class.
	 */
	public static final String BASENAME = "BondEditRule-";

    /**
     * Keyword used in the {@link AtomTupleMatchingRule} to identify the imposed bond order value.
	 */
	public static final String KEYORDER = "ORDER";
	
    /**
     * Keyword used in the {@link AtomTupleMatchingRule} to identify bonds to remove.
	 */
	public static final String KEYREMOVE = "REMOVE"; 

	/**
     * List of mutually exclusive attributes.
     */
	public static final List<String> MUTUALLYEXCLUSIVEATTRIBUTES = Arrays.asList(
		KEYORDER, KEYREMOVE);

//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public BondEditor()
    {
        ruleRoot = BASENAME;
        valuedKeywords.add(KEYORDER);
        valuelessKeywords.add(KEYREMOVE);
	}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(EDITBONDSTASK, ADDBONDSTASK, REMOVEBONDSTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/BondEditor.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new BondEditor();
    }
    
//------------------------------------------------------------------------------

	@Override
	public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i) 
	{		
        // Redundancy of task name is meant to ensure visibility of all tasks
        // in the list AutoCompChem tasks.
      	if (task.equals(EDITBONDSTASK) || task.equals(ADDBONDSTASK) 
			|| task.equals(REMOVEBONDSTASK))
      	{
      		editBonds(rules, iac);
      	} else {
      		dealWithTaskMismatch();
        }
      	return iac;
    }

//------------------------------------------------------------------------------

	/**
	 * Edit bonds in a container.
	 * @param iac the container to edit.
	 * @param rules the rules applied to the container to create the tuples of 
	 * atoms that define the bonds to edit.
	 * The attributes of the tuples define the behavior in this way: 
	 * - if the attribute {@link #KEYORDER} is present, the bonds defined by the
	 * atoms in the tuple are created or edited to impose the bond order 
	 * specified by the attribute value (i.e., {@link IBond.Order}).
	 * - if the attribute {@link #KEYREMOVE} is present, bonds defined by the 
	 * atoms in the tuple are removed.
	 * The three cases are mutually exclusive. An exception is thrown if more 
	 * than one of the three attributes is present. 
	 */

	public static void editBonds(List<AtomTupleMatchingRule> rules, IAtomContainer iac)
	{

        List<AnnotatedAtomTuple> tuples = createTuples(iac, rules, null,
            AtomTupleGenerator.Mode.TUPLES);

		editBonds(iac, tuples);
	}

//------------------------------------------------------------------------------

    /**
     * Edit bonds in a container.
     * @param iac the container to edit.
     * @param tuples the tuple of atoms that define what and where to edit bonds. 
     * The attributes of the tuples define the details in this way: 
     * - if the attribute {@link #KEYORDER} is present, the bonds defined by the
     * atoms in the tuple are created or edited to impose the bond order 
	 * specified by the attribute value (i.e., {@link IBond.Order}).
     * - if the attribute {@link #KEYREMOVE} is present, bonds defined by the 
	 * atoms in the tuple are removed.
	 * The three cases are mutually exclusive. An exception is thrown if more 
     * than one of the three attributes is present. 
     */

    public static void editBonds(IAtomContainer iac, List<AnnotatedAtomTuple> tuples)
    {
    	Logger logger = LogManager.getLogger();
    	
    	for (AnnotatedAtomTuple tuple : tuples)
    	{
			// Check preconditions for validity of tuple and its attributes
            int numExclusiveAttributes = 0;
            for (String attribute : MUTUALLYEXCLUSIVEATTRIBUTES)
            {
                if (tuple.hasValuelessAttribute(attribute))
                {
                    numExclusiveAttributes++;
                }
            }
            if (numExclusiveAttributes > 1)
            {
                throw new IllegalArgumentException("More than one mutually "
                    + "exclusive attribute is present in tuple '" 
                    + tuple.toString() + "'. This is not allowed.");
            }
			if (tuple.getNumberOfIDs() != 2)
			{
				throw new IllegalArgumentException("Tuple '" + tuple.toString() 
					+ "' defines " + tuple.getNumberOfIDs() + " instead of 2 atoms. "
					+ "This is not yet supported. If you have reasons to request "
					+ "this, please contact the developers of AutoCompChem.");
			}

			// Process the tuple
			IAtom atm0 = iac.getAtom(tuple.getAtomIDs().get(0));
			IAtom atm1 = iac.getAtom(tuple.getAtomIDs().get(1));
			IBond bond = iac.getBond(atm0, atm1);
			String bondRef = MolecularUtils.getAtomRef(atm0, iac)
			+ " ~ " + MolecularUtils.getAtomRef(atm1, iac);
			if (bond == null && tuple.hasValuedAttribute(KEYORDER))
			{
				// Create the defautl bond, possibly edited afterwards
				IBond.Order bo = IBond.Order.SINGLE;
				bond = new Bond(atm0, atm1, bo);
				iac.addBond(bond);
			}

			// Process the bond,, if any existed or has been created, but we might still
			// have a null bond in case the tuple did not require creation and did not 
			// match an existing bond, but it did match a tuple of atoms.
			if (bond != null )
			{
				if (tuple.hasValuelessAttribute(KEYREMOVE))
				{
					iac.removeBond(bond);
					String msg = "Removed bond " + bondRef;
					logger.debug(msg);
				} else if (tuple.hasValuedAttribute(KEYORDER)) 
				{
					String value = tuple.getValueOfAttribute(KEYORDER).toUpperCase();
					IBond.Order order = IBond.Order.valueOf(value.toUpperCase());
					bond.setOrder(order);
					String msg = "Set bond order of " + bondRef + " to " + order;
					logger.debug(msg);
				} else {
					throw new IllegalArgumentException("Expecting one of '"
						+ MUTUALLYEXCLUSIVEATTRIBUTES.toString() 
						+ "' but found none in tuple '" 
						+ tuple.toString() + "'. This is not allowed.");
				}
			}
    	}
    }

//------------------------------------------------------------------------------

}
