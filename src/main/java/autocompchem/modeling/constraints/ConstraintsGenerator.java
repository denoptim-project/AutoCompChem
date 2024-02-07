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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.io.SDFIterator;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AtomTupleGenerator;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;


/**
 * Facility to generate constraints given a list of matching rules.
 * 
 * @author Marco Foscato
 */


public class ConstraintsGenerator extends AtomTupleGenerator
{
    
    /**
     * Results
     */
    private ArrayList<ConstraintsSet> output = new ArrayList<ConstraintsSet>();
    
    /**
     * String defining the task of generating constraints
     */
    public static final String GENERATECONSTRAINTSTASKNAME = 
    		"generateConstraints";

    /**
     * Task about generating constraints
     */
    public static final Task GENERATECONSTRAINTSTASK;
    static {
    	GENERATECONSTRAINTSTASK = Task.make(GENERATECONSTRAINTSTASKNAME);
    }

//-----------------------------------------------------------------------------
	
    /**
     * Constructor.
     */
    public ConstraintsGenerator()
    {
    	ruleRoot = ConstraintDefinition.BASENAME;
    }
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(GENERATECONSTRAINTSTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/ConstraintsGenerator.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new ConstraintsGenerator();
    }

//------------------------------------------------------------------------------

    /**
     * Parses the formatted text defining {@link ConstraintDefinition} and adds
     * the resulting rules to this instance of atom tuple generator.
     * @param lines the lines of text to be parsed into 
     * {@link AtomTupleMatchingRule}s.
     */

    @Override
    public void parseAtomTupleMatchingRules(List<String> lines)
    {
        for (String line : lines)
        {
        	rules.add(new ConstraintDefinition(line, ruleID.getAndIncrement()));
        }
    }
    
//-----------------------------------------------------------------------------

    /**
     * Performs any of the registered tasks according to how this worker
     * has been initialized.
     */

    @Override
    public void performTask()
    {
    	if (task.equals(GENERATECONSTRAINTSTASK))
    	{
    		createConstrains();
    	} else {
    		dealWithTaskMistMatch();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Define constraints for all structures found in the structures
     * from the input file.
     */

    public void createConstrains()
    {
        if (inFile.getName().equals("noInFile"))
        {
            Terminator.withMsgAndStatus("ERROR! Missing input file parameter. "
                + " Cannot generate constraints.",-1);
        }

        try {
            SDFIterator sdfItr = new SDFIterator(inFile);
            while (sdfItr.hasNext())
            {
                //Get the molecule
                IAtomContainer mol = sdfItr.next();

                //Assign Constraints
                ConstraintsSet cs = createConstraints(mol);
                
                if (verbosity > 0)
                {
                	System.out.println("# " + MolecularUtils.getNameOrID(mol));
                	cs.printAll();
                }
                output.add(cs);
                
            } //end loop over molecules
            sdfItr.close();
        } catch (Throwable t) {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Exception returned by "
                + "SDFIterator while reading " + inFile, -1);
        }
        
        if (exposedOutputCollector != null)
    	{
	    	int ii = 0;
	    	for (ConstraintsSet cs : output)
	    	{
	    		ii++;
	    		if (cs != null)
	    		{
	    			String molID = "mol-"+ii;
	  		        exposeOutputData(new NamedData(molID, 
	  		        		NamedDataType.CONSTRAINTSSET, cs));
	    		}
	    	}
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Define constraints in a given molecule and using the currently loaded 
     * atom tuple defining rules.
     * @param mol the molecular system we create constraints for.
     * @return the set of constraints.
     */

    public ConstraintsSet createConstraints(IAtomContainer mol)
    {
    	return createConstraints(mol, rules);
    }
    
//------------------------------------------------------------------------------

    /**
     * Define constraints in a given molecule and using the a given set of
     * atom tuple defining rules.
     * @param mol the molecular system we create constraints for.
     * @return the set of constraints.
     */

    public static ConstraintsSet createConstraints(IAtomContainer mol, 
    		List<AtomTupleMatchingRule> rules)
    {
    	ConstraintsSet cLst = new ConstraintsSet();
    	
    	List<AnnotatedAtomTuple> tuples = createTuples(mol, rules);
    	for (AnnotatedAtomTuple tuple : tuples)
    	{
    		cLst.add(new Constraint(tuple));
    	}
        return cLst;
    }

//-----------------------------------------------------------------------------

}
