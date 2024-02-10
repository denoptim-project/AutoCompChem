package autocompchem.molecule;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.EnumUtils;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.IValueContainer;
import autocompchem.chemsoftware.ChemSoftConstants.CoordsType;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.files.FileUtils;
import autocompchem.io.SDFIterator;
import autocompchem.modeling.atomtuple.AnnotatedAtomTuple;
import autocompchem.modeling.atomtuple.AnnotatedAtomTupleList;
import autocompchem.modeling.atomtuple.AtomTupleGenerator;
import autocompchem.modeling.atomtuple.AtomTupleMatchingRule.RuleType;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.smarts.SMARTS;
import autocompchem.text.TextBlock;
import autocompchem.utils.ListOfListsCombinations;
import autocompchem.utils.StringUtils;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;


/**
 * Framework for workers that process molecular definition as input,
 * 
 * @author Marco Foscato
 */

public abstract class AtomContainerInputProcessor extends Worker
{
    
    /**
     * The input file, i.e., any molecular structure file
     */
    protected File inFile;

    /**
     * The input molecules
     */
    protected List<IAtomContainer> inMols;
    
    /**
     * The list of resulting data
     */
    protected List<Object> output = new ArrayList<Object>();
    
    /**
     * The index of the chosen geometry to work with when input consists of 
     * multiple geometries.
     */
    private Integer chosenGeomIdx;

    /**
     * String defining the task of generating tuples of atoms
     */
    public static final String READIACSTASKNAME = 
    		"readAtomContainers";

    /**
     * Task about generating tuples of atoms
     */
    public static final Task READIACSTASK;
    static {
    	READIACSTASK = Task.make(READIACSTASKNAME);
    }
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
                Arrays.asList(READIACSTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
    	//TODO-gg
        return "inputdefinition/AtomContainerInputProcessor.json";
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialize the worker according to the parameters loaded by constructor.
     */

	@Override
    public void initialize()
    {
        if (params.contains(ChemSoftConstants.PARINFILE))
        {
            inFile = new File(params.getParameter(ChemSoftConstants.PARINFILE)
            		.getValueAsString());
            FileUtils.foundAndPermissions(this.inFile,true,false,false);
        }
        if (params.contains(ChemSoftConstants.PARGEOMFILE))
        {
            inFile = new File(params.getParameter(ChemSoftConstants.PARGEOMFILE)
            		.getValueAsString());
            FileUtils.foundAndPermissions(this.inFile,true,false,false);
        }
        if (params.contains(ChemSoftConstants.PARGEOM))
        {
            inMols = (List<IAtomContainer>) params.getParameter(
            		ChemSoftConstants.PARGEOM).getValue();
            if (params.contains("INFILE"))
            {
            	//TODO: logging
            	System.out.println("WARNING: found both INFILE and "
            			+ ChemSoftConstants.PARGEOM + ". Using geometries from "
            			+ ChemSoftConstants.PARGEOM + " as input for "
            			+ this.getClass().getSimpleName() + ".");
            	this.inFile = null;
            }
        }
        
        if (params.contains(ChemSoftConstants.PARMULTIGEOMID))
        {
        	chosenGeomIdx = Integer.parseInt(params.getParameter(
        			ChemSoftConstants.PARMULTIGEOMID).getValueAsString());
        }
    }
   
//------------------------------------------------------------------------------

    /**
     * Goes through the input according to the settings available to this
     * instance.
     * @param outputStorage the collector of output data
     */
    protected void processInput()
    {
        if (inFile==null && inMols==null)
        {
            Terminator.withMsgAndStatus("ERROR! Missing parameter defining the "
            		+ "input geometries (" + ChemSoftConstants.PARGEOM + ") or "
            		+ "an input file to read geometries from. "
            		+ "Cannot perform task " + task + " from "
            		+ this.getClass().getSimpleName() + ".", -1);
        }

        boolean breakAfterThis = false;
        if (inFile!=null)
        {
        	SDFIterator sdfItr = null;
	        try {
	        	sdfItr = new SDFIterator(inFile);
	            int i = -1;
	            while (sdfItr.hasNext())
	            {
	            	i++;
	            	IAtomContainer mol = sdfItr.next();
	            	if (chosenGeomIdx!=null)
		            {
	            		if (i==chosenGeomIdx)
	            		{
			        		breakAfterThis = true;
	            		} else {
	            			continue;
	            		}
		            }
	            	processOneAtomContainer(mol);
	            	if (breakAfterThis)
	            		break;
	            }
	            sdfItr.close();
	        } catch (Throwable t) {
	            t.printStackTrace();
	            Terminator.withMsgAndStatus("ERROR! Exception returned by "
	                + sdfItr.getClass().getSimpleName() 
	                + " while reading " + inFile, -1);
	        }
        } else {
        	for (int i=0; i<inMols.size(); i++)
            {
            	if (chosenGeomIdx!=null)
	            {
            		if (i==chosenGeomIdx)
            		{
		        		breakAfterThis = true;
            		} else {
            			continue;
            		}
	            }
            	processOneAtomContainer(inMols.get(i));
            	if (breakAfterThis)
            		break;
            }
        }

        if (exposedOutputCollector != null)
    	{
        	prepareOutputToExpose();
    	}
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Do whatever with one atom container and place the results in the output 
     * data storage.
     */
    public abstract void processOneAtomContainer(IAtomContainer iac);
    
//------------------------------------------------------------------------------
    
    /**
     * Process the stored output to make it available external collectors.
     */
    public abstract void prepareOutputToExpose(); 
    
//-----------------------------------------------------------------------------

}
