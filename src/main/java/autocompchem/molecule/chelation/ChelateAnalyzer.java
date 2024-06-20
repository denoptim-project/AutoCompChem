package autocompchem.molecule.chelation;

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

import org.apache.logging.log4j.Logger;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.atom.AtomUtils;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.files.FileUtils;
import autocompchem.io.SDFIterator;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Tool for the characterization of chelating systems.
 * Chelates are identified by remotion of the target atom and 
 * evaluation of the remaining connectivity.
 * 
 * @author Marco Foscato
 */


public class ChelateAnalyzer extends AtomContainerInputProcessor
{
    /**
     * Name of the output file
     */
    private File outFile;

    /**
     * List of named SMARTS queries used to identify the chelating systems 
     */
    private Map<String,String> targetsmarts;
    
    /**
     * String defining the task of analyzing chelation patterns
     */
    public static final String ANALYZECHELATESTASKNAME = "analyzeChelates";

    /**
     * Task about analyzing chelation patterns
     */
    public static final Task ANALYZECHELATESTASK;
    static {
    	ANALYZECHELATESTASK = Task.make(ANALYZECHELATESTASKNAME);
    }

//------------------------------------------------------------------------------
    /**
     * Constructor.
     */
    public ChelateAnalyzer()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(ANALYZECHELATESTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/ChelateAnalyzer.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new ChelateAnalyzer();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
    	super.initialize();

        //Get optional parameter
        //Get and check output file
        if (params.contains("OUTFILE"))
        {
            this.outFile = new File(
            		params.getParameter("OUTFILE").getValueAsString());
            FileUtils.mustNotExist(this.outFile);
        }

        //Get the list of SMARTS to be matched
        if (params.contains("TARGETSMARTS"))
        {
        	this.targetsmarts = new HashMap<String,String>();
            String allSamrts = 
                params.getParameter("TARGETSMARTS").getValue().toString();
            String[] lines = allSamrts.split("\\r?\\n");
            int k = 0;
            for (int i=0; i<lines.length; i++)
            {
                String[] parts = lines[i].split("\\s+");
                for (int j=0; j<parts.length; j++)
                {
                    String singleSmarts = parts[j];
                    if (singleSmarts.equals(""))
                        continue;
                    k++;
                    String key2 = "target_" + Integer.toString(k);
                    this.targetsmarts.put(key2,singleSmarts);
                }
            }
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
    	processInput();
    }
    
//------------------------------------------------------------------------------

	@Override
	public void processOneAtomContainer(IAtomContainer iac, int i) 
	{
    	if (task.equals(ANALYZECHELATESTASK))
    	{
    		List<Chelant> chelants = analyzeChelation(iac);
    		
            String record = " Ligands: " + chelants.size() + " Denticity: ";
            for (Chelant c : chelants)
            {
                record = record + c.getDenticity() + " ";
            }
            logger.info(record);
            
    		//TODO-gg expose output
            /*
            if (exposedOutputCollector != null)
	    	{
				String molID = "mol-"+i;
				for (int j=0; j<chelants.size(); j++)
				{
			        exposeOutputData(new NamedData(
			        		ANALYZECHELATESTASK.ID + "_" + molID + "-" + j, 
			        		NamedDataType.CHELANT, chelants.get(j)));
				}
	    	}
            */
    	} else {
    		dealWithTaskMismatch();
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Perform the analysis of the chelation system
     * according to the parameters provided to the constructor. If no target 
     * atom is given, all D-block elements are considered as the chelated
     * objects.
     * @param mol the molecule to analyze
     */
    public List<Chelant> analyzeChelation(IAtomContainer mol) 
    {
    	return analyzeChelation(mol, targetsmarts, logger);
    }
    
//-----------------------------------------------------------------------------

    /**
     * 
     * @param mol
     * @param targetsmarts
     * @param loggign tool
     * @return
     */
    public static List<Chelant> analyzeChelation(IAtomContainer mol, 
    		Map<String,String> targetsmarts, Logger logger) 
    {
        ArrayList<Chelant> chelates = new ArrayList<Chelant>();

        //We modify a copy of the molecule
        IAtomContainer wMol = new AtomContainer();
        try {        
            wMol = (IAtomContainer) mol.clone();
        } catch (Throwable t) {
            Terminator.withMsgAndStatus("ERROR! ChelateAnalyzer cannot make "
                + " clone of molecule.", -1); 
        }

        //Identify target atoms
        List<IAtom> targets = new ArrayList<IAtom>();
        List<IBond> bndsToTrg = new ArrayList<IBond>();
        String chelCntrLabed = "CHELATEDTRGID";
        if (targetsmarts!=null)
        {
//TODO
            Terminator.withMsgAndStatus("ERROR! Use of SMARTS in "
                        + "ChelateAnalyzer is not implemented yet", -1);
/*
            ManySMARTSQuery msq = new ManySMARTSQuery(mol,
                                                      targetsmarts,
                                                      verbosity);
            if (msq.hasProblems())
            {
                String cause = msq.getMessage();
                if (cause.contains("The AtomType") &&
                                        cause.contains("could not be found"))
                {
                    Terminator.withMsgAndStatus("ERROR! " + cause
                        + " To solve the problem try to move this "
                        + "element to \"Du\" an try again.",-1);
                }
                System.err.println("\nWARNING! Problems in using SMARTS queries. "
                                + cause);
                System.out.println("Matches: "+msq.getNumMatchesMap());
            }
            
            if (msq.getTotalMatches() > 0)
            {
                for (String key : targetsmarts.keySet())
                {
                    if (!msq.hasMatches(key))
                    {
                        continue;
                    }
                    ArrayList<IAtom> atomsMatched = new ArrayList<IAtom>();
                    List<List<Integer>> allMatches = msq.getMatchesOfSMARTS(key);
                    for (List<Integer> innerList : allMatches)
                    {
                        for (Integer iAtm : innerList)
                        {
                            IAtom targetAtm = mol.getAtom(iAtm);
                            targets.add(targetAtm);
                        }
                    }
                }
            }
*/

        } else {
            int centerID=0;
            for (IAtom atm : wMol.atoms())
            {
                if (AtomUtils.isMetalDblock(atm.getSymbol(),0))
                {
                    targets.add(atm);
                    List<IBond> bndsToAtm = wMol.getConnectedBondsList(atm);
                    for (IBond bnd : bndsToAtm)
                    {
                        bndsToTrg.add(bnd);
                        for (IAtom atmInBnd : bnd.atoms())
                        {
                            if (atmInBnd != atm)
                            {
                                atmInBnd.setProperty(chelCntrLabed,centerID);
                            }
                        }
                    }
                    centerID++;
                }
            }
        }

        logger.debug("Searching chelating ligands on " + targets.size() 
        	+ " centers");
        logger.trace("List of centers: " + targets);

        //Chelates are identifyed by remotion of the target atom and 
        // evaluation of the remaining connectivity

        //Remove target atom
        for (IAtom a : targets)
        {
            wMol.removeAtom(a);
        }
        for (IBond bnd : bndsToTrg)
        {
            wMol.removeBond(bnd);
        }

        //Isolate ligands
        String label = "LIGANDID";
        ConnectivityUtils.identifyContinuoslyConnected(wMol,label);
        Set<Object> labSet = new HashSet<Object>();
        for (IAtom a : wMol.atoms())
        {
            Object lab = a.getProperty(label);
            if (lab == null)
            {
                Terminator.withMsgAndStatus("ERROR! Found unlabeled atom "
                        + "where there should be none in ChelateAnalyzer. "
                        + "Please report this bug to the author.", -1);
            }
            labSet.add(lab);
        }

        for (Object lab : labSet)
        {
            IAtomContainer ligand = new AtomContainer();
            try
            {
                ligand = (IAtomContainer) wMol.clone();
            }
            catch (Throwable t)
            {
                Terminator.withMsgAndStatus("ERROR! ChelateAnalyzer cannot make "
                + " clone of ligand.", -1);
            }

            // remove all atoms with different ligand ID
            ArrayList<IAtom> atmsToDel = new ArrayList<IAtom>();
            ArrayList<IBond> bndsToDel = new ArrayList<IBond>();
            for (IAtom atm : ligand.atoms())
            {
                Object prop = atm.getProperty(label);
                if (prop == null)
                {
                    Terminator.withMsgAndStatus("ERROR! Label '" + label + "' "
                        + "is null for " + MolecularUtils.getAtomRef(atm,wMol)
                        + ". Please report this bug to the authors.",-1);
                }

                if (!prop.equals(lab))
                {
                    atmsToDel.add(atm);
                    List<IBond> bndsToAtm = ligand.getConnectedBondsList(atm);
                    for (IBond bnd : bndsToAtm)
                    {
                        for (IAtom atmInBnd : bnd.atoms())
                        {
                            if (atmInBnd == atm)
                            {
                                bndsToDel.add(bnd);
                            }
                        }
                    }
                }
            }
            for (IAtom atm : atmsToDel)
            {
                ligand.removeAtom(atm);
            }
            for (IBond bnd : bndsToDel)
            {
                ligand.removeBond(bnd);
            }

            int denticity = 0;
            Set<Object> chelCntrId = new HashSet<Object>();
            for (IAtom atm : ligand.atoms())
            {
                Object prop = atm.getProperty(chelCntrLabed);
                if (prop == null)
                {
                    continue;
                }
                chelCntrId.add(prop);
                denticity++;
                if (chelCntrId.size() > 1)
                {
                    Terminator.withMsgAndStatus("ERROR! Cannot yet handle ligands "
                        + "coordinating different centers (i.e., bridges between "
                        + "different metal centers)", -1);
                }
                
            }

            //Store results
            String chelantRef = ligand.getAtom(0).getProperty(label).toString();
            Chelant c = new Chelant(chelantRef,ligand,denticity);
            chelates.add(c);

        }

        return chelates;
    }

//-----------------------------------------------------------------------------
    
}
