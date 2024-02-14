package autocompchem.molecule.conformation;

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

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.io.SDFIterator;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AtomTupleGenerator;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.modeling.constraints.ConstraintDefinition;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;


/**
 * Tool to generate the definition of a conformational spaces based on a given 
 * set rules meant to define {@link ConformationalCoordinate}s.
 * 
 * @author Marco Foscato
 */

public class ConformationalSpaceGenerator extends AtomTupleGenerator
{
    /**
     * String defining the task of generating a definition of conformational 
     * space
     */
    public static final String GENERATECONFORMATIONALSPACETASKNAME = 
    		"generateConformationalSpace";

    /**
     * Task about generating a definition of conformational 
     * space
     */
    public static final Task GENERATECONFORMATIONALSPACETASK;
    static {
    	GENERATECONFORMATIONALSPACETASK = 
    			Task.make(GENERATECONFORMATIONALSPACETASKNAME);
    }

//-----------------------------------------------------------------------------
	
    /**
     * Constructor.
     */
    public ConformationalSpaceGenerator()
    {
    	// NB: this workers inherits all from the super class
    	ruleRoot = ConformationalCoordDefinition.BASENAME;
    }
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(GENERATECONFORMATIONALSPACETASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/ConformationalSpaceGenerator.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new ConformationalSpaceGenerator();
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
        	rules.add(new ConformationalCoordDefinition(line, 
        			ruleID.getAndIncrement()));
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
    	processInput();
    }
    
//------------------------------------------------------------------------------

	@Override
	public void processOneAtomContainer(IAtomContainer iac, int i) 
	{
    	if (task.equals(GENERATECONFORMATIONALSPACETASK))
    	{
    		ConformationalSpace cs = createConformationalSpace(iac);
            
    		if (verbosity > 0)
	        	cs.printAll();
            
            if (exposedOutputCollector != null)
        	{
    			String molID = "mol-"+i;
    	        exposeOutputData(new NamedData(
    	        		GENERATECONFORMATIONALSPACETASK.ID + "-" + molID, 
    	        		NamedDataType.CONFORMATIONALSPACE, cs));
        	}
    	} else {
    		dealWithTaskMismatch();
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Define conformational space in a given molecule using the currently  
     * loaded set of rules for defining {@link ConformationalCoordinate}s.
     * @param mol the molecular system we create a conformational space for.
     * @return the resulting conformational space.
     */

    public ConformationalSpace createConformationalSpace(IAtomContainer mol)
    {
    	return createConformationalSpace(mol, rules);
    }
    
//------------------------------------------------------------------------------

    /**
     * Define conformational space in a given molecule and using the a given set
     * rules for defining {@link ConformationalCoordinate}s.
     * @param mol the molecular system we create a conformational space for.
     * @return the resulting conformational space.
     */

    public static ConformationalSpace createConformationalSpace(
    		IAtomContainer mol, 
    		List<AtomTupleMatchingRule> rules)
    {
    	ConformationalSpace confSpace = new ConformationalSpace();
    	List<AnnotatedAtomTuple> tuples = createTuples(mol, rules);
    	for (AnnotatedAtomTuple tuple : tuples)
    	{
    		confSpace.add(new ConformationalCoordinate(tuple));
    	}
        return confSpace;
    }

//-----------------------------------------------------------------------------

}
