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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.logging.log4j.Logger;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.AtomContainerSet;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import autocompchem.datacollections.NamedData;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.smarts.SMARTS;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;

/**
 * Tool for extracting molecules (i.e., continuously connected set of atoms)
 * from atom containers.
 * 
 * @author Marco Foscato
 */

public class MoleculeExtractor extends AtomContainerInputProcessor
{

    /**
     * List (with string identifier) of smarts queries that are required for
     * a molecule to be part of the output.
     */
    private Map<String,SMARTS> requiredSmarts = null;
    
    /**
     * List (with string identifier) of smarts queries that cause exclusion
     * of a molecule from the output.
     */
    private Map<String,SMARTS> excludedSmarts = null;
    
    /**
     * Flag controlling whether we put all resulting molecules in one file 
     * (default), or each in a dedicated file.
     */
    private boolean singleMolOutput = false;
    
    /**
     * String defining the task of reordering atom list
     */
    public static final String EXTRACTMOLECULESTASKNAME = "extractMolecules";

    /**
     * Task about reordering atom list
     */
    public static final Task EXTRACTMOLECULESTASK;
    static {
    	EXTRACTMOLECULESTASK = Task.make(EXTRACTMOLECULESTASKNAME);
    }


//------------------------------------------------------------------------------

    /**
     * Constructor.
     */
    public MoleculeExtractor()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() 
    {
        return Collections.unmodifiableSet(new HashSet<Task>(
                Arrays.asList(EXTRACTMOLECULESTASK
                		)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
    	//TODO
        return "inputdefinition/MoleculeExtractor.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new MoleculeExtractor();
    }

//------------------------------------------------------------------------------

    /**
     * Initialize the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
    	super.initialize();
    	
        if (params.contains("REQUIREDSMARTS"))
        {
            String txt = params.getParameter("REQUIREDSMARTS").getValueAsString()
            		.replace("\n", "\\n").replace("\r", "\\r");
            String[] parts = txt.split("\\s+");
            requiredSmarts = new HashMap<String,SMARTS>();
            for (int i=0; i<parts.length; i++)
            {
                String singleSmarts = parts[i];
                if (singleSmarts.isBlank())
                    continue;
                this.requiredSmarts.put("REQUIRED-"+Integer.toString(i), 
                		new SMARTS(singleSmarts));
            }
        }
        
        if (params.contains("EXCLUDEDSMARTS"))
        {
            String txt = params.getParameter("EXCLUDEDSMARTS").getValueAsString();
            String[] parts = txt.split("\\s+");
            excludedSmarts = new HashMap<String,SMARTS>();
            for (int i=0; i<parts.length; i++)
            {
                String singleSmarts = parts[i];
                if (singleSmarts.isBlank())
                    continue;
                this.excludedSmarts.put("EXCLUDED-"+Integer.toString(i), 
                		new SMARTS(singleSmarts));
            }
        }
        
        if (params.contains("SINGLEMOLOUTPUT"))
        {
        	this.singleMolOutput = true;
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Performs any of the registered tasks according to how this worker
     * has been initialised.
     */

    @Override
    public void performTask()
    {
    	processInput();
    }
    
//------------------------------------------------------------------------------

	@Override
	public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i) 
	{
		AtomContainerSet molecules = new AtomContainerSet();
    	if (task.equals(EXTRACTMOLECULESTASK))
    	{
    		List<IAtomContainer> mols = isolateMolecules(iac);
    		for (IAtomContainer keptMol : filterMolecules(mols, requiredSmarts,
    				excludedSmarts, logger))
    			molecules.addAtomContainer(keptMol);
    		
    		if (outFile != null)
            {
    			if (singleMolOutput)
    			{
        			// We set even if we write singe-frag files to avoid producing
        			// also a file that collects all fragments in the super class
                	outFileAlreadyUsed = true;
    				for (int j=0; j< molecules.getAtomContainerCount(); j++)
    				{
    					File singleMolOutFile = new File
    							(FileUtils.getIdSpecPathName(outFile, i+"_"+j));
    					IAtomContainer mol = molecules.getAtomContainer(j);
    	            	IOtools.writeAtomContainerToFile(singleMolOutFile, 
    	            			mol, outFormat, true);
    	            	logger.info("Writing " + mol.getTitle() + " to '" 
    	            			+ singleMolOutFile.getAbsolutePath() + "'");
    				}
    			} else {
                	outFileAlreadyUsed = true;
	            	IOtools.writeAtomContainerSetToFile(outFile, molecules, 
	            			outFormat, true);
    			}
            }
            
            if (exposedOutputCollector != null)
        	{
    			String molID = "mol-"+i;
    	        exposeOutputData(new NamedData(
    	        		EXTRACTMOLECULESTASK.ID + "-" + molID, molecules));
        	}
    	} else {
    		dealWithTaskMismatch();
        }
    	
    	if (molecules.getAtomContainerCount() == 1)
    	{
    		return molecules.getAtomContainer(0);
    	} else {
    		if (outFile==null)
    		{
    			logger.warn("Multiple resulting geometries are exposed as "
    					+ "'" + task.ID + "' data. The initial geometry is "
    					+ "exposed as main output. You can save the multiple "
    					+ "geometries to file by using parameter '" 
    					+ WorkerConstants.PAROUTFILE + "'.");
    		}
    		return iac;
    	}
    }

//------------------------------------------------------------------------------

	/**
	 * Puts each set of atoms that belong to a continuously connected set 
	 * (i.e., a Molecule). into 
	 * an independent atom container and collects all such containers.
	 * @param iac the initial container of atoms from which we clone-out the
	 * isolated molecules.
	 * @return the list of molecules.
	 */
    public static List<IAtomContainer> isolateMolecules(IAtomContainer iac) 
    {
    	List<IAtomContainer> mols = ConnectivityUtils.getConnectedFrags(iac);
    	for (int i=0; i<mols.size(); i++)
    	{
    		mols.get(i).setTitle("Fragment-"+i);
    	}
    	return mols;
	}

//------------------------------------------------------------------------------


	/**
     * Filter molecules according to the given SMARTS-based criteria
     * @param requiredSmarts SMARTS that must be matched for keeping a molecule
     * @param excludedSmarts SMARTS that cause a molecule to be removed from the
     * output.
     * @param logger tool for logging. Can be null for no logging.
     * @return the set of molecules that pass the given filtering criteria.
     */

    public static List<IAtomContainer> filterMolecules(List<IAtomContainer> mols,
    		Map<String,SMARTS> requiredSmarts, Map<String,SMARTS> excludedSmarts,
    		Logger logger)
    {
    	List<IAtomContainer> requiredMols = new ArrayList<IAtomContainer>();
    	if (requiredSmarts!=null && requiredSmarts.size()>0)
    	{
	    	for (IAtomContainer mol : mols)
	    	{
	            ManySMARTSQuery reqMsq = new ManySMARTSQuery(mol, requiredSmarts);
	            if (reqMsq.hasProblems())
	            {
	                String cause = reqMsq.getMessage();
	                Terminator.withMsgAndStatus("ERROR! Unable to use required "
	               		+ "SMARTS. Details: " + cause,-1);
	            }
	            if (reqMsq.getTotalMatches() > 0)
	            {
	            	requiredMols.add(mol);
	            	if (logger!=null)
	            	{
	            		Map<String, Integer> matchesCounts = 
	            				reqMsq.getNumMatchesMap();
		            	for (String key : matchesCounts.keySet())
		                {
		                    if (matchesCounts.get(key) > 0)
		                    {
		                    	logger.info(mol.getTitle() + " (" 
		                    			+ MolecularUtils.getMolecularFormula(mol)
		                    			+ ") matches '" + key +"'.");
		                        break;
		                    }
		                }
	            	}
	            }
	        }
    	} else {
    		requiredMols = mols;
    	}
  
    	List<IAtomContainer> keptMols = new ArrayList<IAtomContainer>();
    	if (excludedSmarts!=null && excludedSmarts.size()>0)
    	{
	    	for (IAtomContainer mol : requiredMols)
	    	{
	            ManySMARTSQuery exclMsq = new ManySMARTSQuery(mol, excludedSmarts);
	            if (exclMsq.hasProblems())
	            {
	                String cause = exclMsq.getMessage();
	                Terminator.withMsgAndStatus("ERROR! Unable to use required "
	                		+ "SMARTS. Details: " + cause,-1);
	            }
	            if (exclMsq.getTotalMatches() == 0)
	            {
	            	 keptMols.add(mol);
	            } else {
	            	if (logger!=null)
	            	{
		            	Map<String, Integer> matchesCounts = 
		            			exclMsq.getNumMatchesMap();
		            	for (String key : matchesCounts.keySet())
		                {
		                    if (matchesCounts.get(key) > 0)
		                    {
		                    	logger.info(mol.getTitle() + " (" 
		                    			+ MolecularUtils.getMolecularFormula(mol)
		                    			+ ") matches '" + key +"'.");
		                        break;
		                    }
		                }
	            	}
	            }
	        }
    	} else {
    		keptMols = requiredMols;
    	}

        return keptMols;
    }
    
//------------------------------------------------------------------------------

}
