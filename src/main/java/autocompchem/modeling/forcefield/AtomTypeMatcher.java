package autocompchem.modeling.forcefield;

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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.NamedData;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.smarts.SMARTS;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * AtomTypeMatcher implements the facility to assign atom types according to a
 * given list of matching rules (SMARTS queries).
 * 
 * @author Marco Foscato
 */


public class AtomTypeMatcher extends AtomContainerInputProcessor
{
    /**
     * The name of the atom type map
     */
    private File atMapFile;
    /**
     * List of atom type-matching smarts with string identifiers
     */
    private Map<String,SMARTS> smarts = new HashMap<String,SMARTS>();
    
    /**
     * String defining the task of assigning atom types
     */
    public static final String ASSIGNATOMTYPESTASKNAME = "assignAtomTypes";

    /**
     * Task about assigning atom types
     */
    public static final Task ASSIGNATOMTYPESTASK;
    static {
    	ASSIGNATOMTYPESTASK = Task.make(ASSIGNATOMTYPESTASKNAME);
    }

//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public AtomTypeMatcher()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(ASSIGNATOMTYPESTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/AtomTypeMatcher.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new AtomTypeMatcher();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
    	super.initialize();
    	
        //File with atom types map
        this.atMapFile = new File(
        		params.getParameter( "ATOMTYPESMAP").getValue().toString());
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

        importAtomTypeMap();
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
	public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i) 
	{
		if (task.equals(ASSIGNATOMTYPESTASK))
    	{
            assignAtomTypes(iac);

            if (outFile!=null)
            {
            	outFileAlreadyUsed = true;
            	writeOut(iac);
            }
            
            if (exposedOutputCollector != null)
        	{
            	String molID = task.ID + "mol-"+i;
  		        exposeOutputData(new NamedData(molID, iac));
        	}
    	} else {
    		dealWithTaskMismatch();
        }
		return iac;
	}

//------------------------------------------------------------------------------

    /**
     * Assign atom types to all atoms in the molecule
     * @param mol the molecular system to work with
     */

    public void assignAtomTypes(IAtomContainer mol)
    {
        //Match atoms by the given rules
        ManySMARTSQuery msq = new ManySMARTSQuery(mol,smarts);
        if (msq.hasProblems())
        {
            String cause = msq.getMessage();
            Terminator.withMsgAndStatus("ERROR! " +cause,-1);
        }

        //prepare flags
        ArrayList<Boolean> done = new ArrayList<Boolean>();
        for (int i=0; i<mol.getAtomCount(); i++)
            done.add(false);
        
        //Assign atom types
        for (String at : smarts.keySet())
        {
            if (msq.getNumMatchesOfQuery(at) == 0)
            {
                continue;
            }
            
            //Get matches for this SMARTS query
            MatchingIdxs matches =  msq.getMatchingIdxsOfSMARTS(at);
            for (List<Integer> innerList : matches)
            {
                for (Integer iAtm : innerList)
                {
                    IAtom atm = mol.getAtom(iAtm);

                    //Check already done
                    if (done.get(iAtm))
                    {
                        logger.warn("WARNING! Atom " 
                                        + MolecularUtils.getAtomRef(atm,mol)
                                         + " matches both " 
                                        + at + " and " + atm.getAtomTypeName());
                    }

                    //Set the atom type
                    atm.setAtomTypeName(at);
                    done.set(iAtm,true);
                    logger.info("Setting atom type  '" + at 
                                          + "' to atom " 
                                          + MolecularUtils.getAtomRef(atm,mol));
                }
            }
        }

        //Verify completeness
        for (int i=0; i<mol.getAtomCount(); i++)
        {
            if (!done.get(i))
            {
                IAtom atm = mol.getAtom(i);
                String el = atm.getSymbol();
                
                logger.warn("WARNING! Atom "
                        + MolecularUtils.getAtomRef(mol.getAtom(i),mol) 
                        + " was not identified by Atom Typer! "
                        + "Original atom type '" + atm.getAtomTypeName()
                        + "' moved to '" + el + "'.");
                atm.setAtomTypeName(el);
            }
        }
    }

//-----------------------------------------------------------------------------

    private void importAtomTypeMap()
    {
        List<String> lines = IOtools.readTXT(atMapFile);
        for (int i=0; i<lines.size(); i++)
        {
            String line = lines.get(i);

            //Skip comments
            if (line.startsWith("#"))
                    continue;

            String[] parts = line.split("\\s+");

            //Get Atom type name (and add index)
            String at = parts[0];
            String id = String.format(Locale.ENGLISH,"%04d",i);
            at = id + "_" + at;

            //Note that second field is not used

            //SMARTS query
            String s = parts[2];
            s =  s.substring(1,s.length()-1);

            //Store
            smarts.put(at, new SMARTS(s));
        }

    }

//-----------------------------------------------------------------------------

    private void writeOut(IAtomContainer mol)
    {
        switch (outFormat) {
            case "TXYZ":
                Terminator.withMsgAndStatus("ERROR! Output format 'TXYZ' "
                   + "is still under development. ", -1);
                break;

            case "LIST":
                ArrayList<String> tab = new ArrayList<String>();
                String t = "Molecule " + MolecularUtils.getNameOrID(mol);
                tab.add(t);
                for (int i=0; i<mol.getAtomCount(); i++)
                {
                    IAtom atm = mol.getAtom(i);
                    String s = i + " " + atm.getSymbol()
                                 + " " + atm.getAtomTypeName();
                    tab.add(s);
                }
                IOtools.writeTXTAppend(outFile,tab,true);
                break;

            default:
                Terminator.withMsgAndStatus("ERROR! Format '" + outFormat + "'"
                                + " not known! Check the option OUTFORMAT.",-1);
        }
    }

//-----------------------------------------------------------------------------
}
