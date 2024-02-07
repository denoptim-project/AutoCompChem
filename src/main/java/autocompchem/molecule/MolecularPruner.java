package autocompchem.molecule;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/*   
 *   Copyright (C) 2014  Marco Foscato 
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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.SDFIterator;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Pruner for molecules: deletes atoms or molecular fragments matching
 * a given SMARTS query.
 * 
 * @author Marco Foscato
 */


public class MolecularPruner extends Worker
{   
    //Filenames
    private File inFile;
    private File outFile;

    //List (with string identifier) of smarts
    private Map<String,String> smarts = new HashMap<String,String>();

    //Verbosity level
    private int verbosity = 1;
    
    /**
     * String defining the task of pruning molecules
     */
    public static final String PRUNEMOLECULESTASKNAME = "pruneMolecules";

    /**
     * Task about pruning molecules
     */
    public static final Task PRUNEMOLECULESTASK;
    static {
    	PRUNEMOLECULESTASK = Task.make(PRUNEMOLECULESTASKNAME);
    }

//------------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public MolecularPruner()
    {}
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(PRUNEMOLECULESTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/MolecularPruner.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new MolecularPruner();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
        //Define verbosity
        String vStr = params.getParameter("VERBOSITY").getValue().toString();
        this.verbosity = Integer.parseInt(vStr);

        if (verbosity > 0)
            System.out.println(" Adding parameters to MolecularPruner");


        //Get and check the input file (which has to be an SDF file)
        this.inFile = new File(
        		params.getParameter("INFILE").getValue().toString());
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

        //Get and check output file
        this.outFile = new File(
        		params.getParameter("OUTFILE").getValue().toString());
        FileUtils.mustNotExist(this.outFile);

        //Get the list of SMARTS to be matched
        String allSamrts = 
                params.getParameter("SMARTS").getValue().toString();
        if (verbosity > 0)
        {
            System.out.println(" Importing SMARTS queries ");
        }
        String[] parts = allSamrts.split("\\s+");
        for (int i=0; i<parts.length; i++)
        {
            String singleSmarts = parts[i];
            if (singleSmarts.equals(""))
                continue;
            this.smarts.put(Integer.toString(i),singleSmarts);
        }

    }
    
//-----------------------------------------------------------------------------

    /**
     * Performs any of the registered tasks according to how this worker
     * has been initialised.
     */

    @Override
    public void performTask()
    {
    	if (task.equals(PRUNEMOLECULESTASK))
    	{
    		pruneAll();
    	} else {
    		dealWithTaskMistMatch();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Prune: delete all matched atoms in all the molecules
     */

    public void pruneAll()
    {
        AtomContainerSet output = new AtomContainerSet();
        try {
            SDFIterator sdfItr = new SDFIterator(inFile);
            while (sdfItr.hasNext())
            {
                //Get the molecule
                IAtomContainer mol = sdfItr.next();

                //Get target atoms                
                ManySMARTSQuery msq = new ManySMARTSQuery(mol,smarts,verbosity);
                if (msq.hasProblems())
                {
                    String cause = msq.getMessage();
                    Terminator.withMsgAndStatus("ERROR! " +cause,-1);
                }

                ArrayList<IAtom> targets = new ArrayList<IAtom>();
                for (String key : smarts.keySet())
                {
                    if (msq.getNumMatchesOfQuery(key) == 0)
                        {
                        continue;
                    }
                    
                    MatchingIdxs allMatches = msq.getMatchingIdxsOfSMARTS(key);
                    for (List<Integer> innerList : allMatches)
                    {
                        for (Integer iAtm : innerList)
                        {
                            IAtom targetAtm = mol.getAtom(iAtm);
                            targets.add(targetAtm);
                        }
                    }
                }

                //Remove atoms
                for (IAtom targetAtm : targets)
                {
                    mol.removeAtom(targetAtm);
                }

                //Store output
                if (exposedOutputCollector != null)
                	output.addAtomContainer(mol);
                if (outFile!=null)
                	IOtools.writeSDFAppend(outFile,mol,true);

            } //end loop over molecules
            sdfItr.close();
        } catch (Throwable t) {
            Terminator.withMsgAndStatus("ERROR! Exception returned by "
                + "SDFIterator while reading " + inFile, -1);
        }
        
        if (exposedOutputCollector != null)
        {
        	int ii = 0;
        	for (IAtomContainer iac : output.atomContainers())
	    	{
	    	    ii++;
	    	    String molID = "mol-"+ii;
		        exposeOutputData(new NamedData(molID, 
		      		NamedDataType.ATOMCONTAINERSET, iac));
	    	}
    	}
    }

//-----------------------------------------------------------------------------

}
