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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.files.FileUtils;
import autocompchem.io.SDFIterator;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AtomTupleConstants;
import autocompchem.modeling.atomtuple.AtomTupleGenerator;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule;
import autocompchem.molecule.MolecularMeter;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.SMARTS;
import autocompchem.worker.TaskID;
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
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.GENERATECONFORMATIONALSPACE)));
    
    /**
     * Results
     */
    private ArrayList<ConformationalSpace> output = 
    		new ArrayList<ConformationalSpace>();

//-----------------------------------------------------------------------------
	
    /**
     * Constructor.
     */
    public ConformationalSpaceGenerator()
    {
    	//TODO-gg create file
    	//super("inputdefinition/ConformationalSpaceGenerator.json");
    	ruleRoot = ConformationalCoordDefinition.BASENAME;
    }

//------------------------------------------------------------------------------

    /**
     * Parses the formatted text defining {@link ConstrainDefinition} and adds
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

    @SuppressWarnings("incomplete-switch")
    @Override
    public void performTask()
    {
        switch (task)
          {
          case GENERATECONFORMATIONALSPACE:
        	  createConformationalSpaces();
              break;
          }

        if (exposedOutputCollector != null)
        {
/*
//TODO
            String refName = "";
            exposeOutputData(new NamedData(refName,
                  NamedDataType.DOUBLE, ));
*/
        }
    }

//------------------------------------------------------------------------------

    /**
     * Define conformational spaces for all structures found in the input file.
     */

    public void createConformationalSpaces()
    {
        if (inFile.equals("noInFile"))
        {
            Terminator.withMsgAndStatus("ERROR! Missing input file parameter. "
                + " Cannot generate conformational spaces.",-1);
        }

        try {
            SDFIterator sdfItr = new SDFIterator(inFile);
            while (sdfItr.hasNext())
            {
                //Get the molecule
                IAtomContainer mol = sdfItr.next();

                //Assign Constraints
                ConformationalSpace cs = createConformationalSpace(mol);
                
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
