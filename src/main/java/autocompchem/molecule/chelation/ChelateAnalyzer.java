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

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.atom.AtomUtils;
import autocompchem.files.FileUtils;
import autocompchem.io.SDFIterator;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * ChelateAnalyzer is a tool for the characterization of chelating systems.
 * Chelates are identified by remotion of the target atom and 
 * evaluation of the remaining connectivity.
 * 
 * @author Marco Foscato
 */


public class ChelateAnalyzer extends Worker
{   
    /**
     * Name of the input file
     */
    private File inFile;

    /**
     * Name of the output file
     */
    private File outFile;

    /**
     * Flag controlling the production of an output file
     */
    private boolean makeout = false;

    /**
     * List of named SMARTS queries used to identify the chelating systems 
     */
    private Map<String,String> targetsmarts = new HashMap<String,String>();

    /**
     * Containers for the results
     */
    private ArrayList<ArrayList<Chelant>> results = 
    		new ArrayList<ArrayList<Chelant>>();

    /**
     * Flag defining to work on a selection of atoms
     */
    private boolean onlyTargetAtms = false;

    /**
     * Flag recording previous run
     */
    private boolean alreadyRun = false;

    /**
     * Flag recording the type of run
     */
    private boolean standalone = true;

    /**
     * Verbosity level
     */
    private int verbosity = 1;

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
             Arrays.asList(Task.make("analyzeChelates"))));
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
        standalone = true;

        //Define verbosity
        String vStr = params.getParameter("VERBOSITY").getValue().toString();
        this.verbosity = Integer.parseInt(vStr);

        if (verbosity > 0)
            System.out.println(" Adding parameters to ChelateAnalyzer");

        //Get and check the input file (which has to be an SDF file)
        this.inFile = new File(
        		params.getParameter("INFILE").getValueAsString());
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

        //Get optional parameter
        //Get and check output file
        if (params.contains("OUTFILE"))
        {
            this.outFile = new File(
            		params.getParameter("OUTFILE").getValueAsString());
            FileUtils.mustNotExist(this.outFile);
            this.makeout = true;
        }

        //Get the list of SMARTS to be matched
        if (params.contains("TARGETSMARTS"))
        {
            onlyTargetAtms = true;
            String allSamrts = 
                params.getParameter("TARGETSMARTS").getValue().toString();
            if (verbosity > 2)
            {
                System.out.println(" Importing SMARTS queries ");
            }
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

    @SuppressWarnings("incomplete-switch")
    @Override
    public void performTask()
    {
        switch (task.ID)
          {
          case "ANALYZECHELATES":
        	  analyzeChelates();
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
     * Performs the analysis of the chelation motif according to the parameters
     * provided to the constructor: it assumes that the ChelateAnalyzer is
     * used in a stand alone fashion (reading structures from files).
     */

    public void analyzeChelates()
    {
        int i = -1;
        try {
            SDFIterator sdfItr = new SDFIterator(inFile);
            while (sdfItr.hasNext())
            {
                //Get the molecule
                i++;
                IAtomContainer mol = sdfItr.next();

                //Perform analysis
                analyzeChelation(mol);

/*
//TODO
                if (makeout)
                {
                    //store results in output SDF
                }
*/
            } //end loop over molecules
            sdfItr.close();
        } catch (Throwable t) {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Exception returned by "
                + "SDFIterator while reading " + inFile, -1);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Perform the analysis of the chelation system
     * according to the parameters provided to the constructor. If no target 
     * atom is given, all D-block elements are considered as the chelated
     * objects
     * The characterization of the chelation systems is reported to standard 
     * output depending on the verbosity level are stored as object so
     * it can be obtained with the 
     * {@link #getAllResults getAllResults} and 
     * {@link #getSingleResults getSingleResults}.
     * @param mol the molecule to analyze
     */

    public void analyzeChelation(IAtomContainer mol) 
    {
        ArrayList<Chelant> chelates = new ArrayList<Chelant>();

        if (verbosity > 0)
        {
            System.out.println(" Analysis of chelation in "
                                         + MolecularUtils.getNameOrID(mol));
        }

        //We modify a copy of the molecule
        IAtomContainer wMol = new AtomContainer();
        try {        
            wMol = (IAtomContainer) mol.clone();
        } catch (Throwable t) {
            Terminator.withMsgAndStatus("ERROR! ChelateAnalyzer cannot make "
                + " clone of molecule.", -1); 
        }

        //Identify target atoms
        ArrayList<IAtom> targets = new ArrayList<IAtom>();
        ArrayList<IBond> bndsToTrg = new ArrayList<IBond>();
        String chelCntrLabed = "CHELATEDTRGID";
        if (onlyTargetAtms)
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

        if (verbosity > 1)
        {
            System.out.println(" Searching chelating ligands on " 
                                + targets.size() + " centers");
            if (verbosity > 3)
            {
                System.out.println("List of centers: " + targets);
            }
        }

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

        //Report
        if (verbosity > 0)
        {
            String record = " Ligands: " + chelates.size() + " Denticity: ";
            for (Chelant c : chelates)
            {
                record = record + c.getDenticity() + " ";
            }
            System.out.println(record);
        }

        //store
        results.add(chelates);
    }

//-----------------------------------------------------------------------------

    /**
     * Return the results for all molecules.
     * @return the results for all input molecules
     */

    public ArrayList<ArrayList<Chelant>> getAllResults()
    {
        if (!alreadyRun && standalone)
            this.analyzeChelates();

        return results;
    }

//-----------------------------------------------------------------------------
    
    /**
     * Return the results for a single molecule.
     * The molecule is identified by the position (0-n) in the input.
     * @param molID the index of the molecule for which the result is required
     * @return the results for the given molecule
     */

    public ArrayList<Chelant> getSingleResults(int molID)
    {
        if (!alreadyRun && standalone)
            this.analyzeChelates();

        ArrayList<Chelant> ac = results.get(molID);
        return ac;
    }

//-----------------------------------------------------------------------------

    /**
     * Sets the verbosity level
     * @param level the verbosity level
     */

    public void setVerbosity(int level)
    {
        this.verbosity = level;
    }

//-----------------------------------------------------------------------------
}
