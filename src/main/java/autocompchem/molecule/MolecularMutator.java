package autocompchem.molecule;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

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

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.SDFIterator;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Mutates the identity of specific atoms. 
 * This tool allows to change the elemental symbol of
 * atoms identified by SMARTS strings.
 * 
 * @author Marco Foscato
 */


public class MolecularMutator extends Worker
{   
    /**
     * Flag indicating the input is from file
     */
    @SuppressWarnings("unused")
        private boolean inpFromFile = false;

    /**
     * Name of the input file
     */
    private File inFile;

    /**
     * Flag indicating the output is to be written to file
     */
    private boolean outToFile = false;

    /**
     * Name of the output file
     */
    private File outFile;

    /**
     * List (with string identifier) of smarts queries
     */
    private Map<String,String> smarts = new HashMap<String,String>();

    /**
     * List (with string identifier) of new elemental symbols
     */
    private Map<String,String> newElms = new HashMap<String,String>();

    /**
     * Verbosity level
     */
    private int verbosity = 0;

//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public MolecularMutator()
    {}

  //------------------------------------------------------------------------------

    @Override
    public Set<TaskID> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<TaskID>(
             Arrays.asList(TaskID.MUTATEATOMS)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/MolecularMutator.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new MolecularMutator();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {

        //Define verbosity
        if (params.contains("VERBOSITY"))
        {
            String v = params.getParameter("VERBOSITY").getValue().toString();
            this.verbosity = Integer.parseInt(v);
        }

        if (verbosity > 0)
        {
            System.out.println(" Reading parameters in MolecularMutator");
        }

        //Get and check the input file (which has to be an SDF file)
        if (params.contains("INFILE"))
        {
            inpFromFile = true;
            this.inFile = new File(
            		params.getParameter("INFILE").getValue().toString());
            FileUtils.foundAndPermissions(this.inFile,true,false,false);
        }

        //Get and check output file
        if (params.contains("OUTFILE"))
        {
            outToFile = true;
            this.outFile = new File(
            		params.getParameter("OUTFILE").getValue().toString());
            FileUtils.mustNotExist(this.outFile);
        }

        //Get the list of SMARTS to be matched
        String allSamrts = 
                         params.getParameter("SMARTSMAP").getValue().toString();
        if (verbosity > 0)
        {
            System.out.println(" Importing SMARTS queries ");
        }

        // NB: the REGEX makes this compatible with either new-line character
        String[] lines = allSamrts.split("\\r?\\n|\\r");
        for (int i=0; i<lines.length; i++)
        {
            String line = lines[i];
            if (line.equals(""))
                continue;
            String[] words = line.split("\\s+");
            String refName = "mutDef" + Integer.toString(i);
            this.smarts.put(refName,words[0]);
            this.newElms.put(refName,words[1]);
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
          switch (task)
            {
            case MUTATEATOMS:
            	mutateAll();
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
     * Mutate all atom lists taken from the input file 
     * according to the current settings.
     */

    public void mutateAll()
    {
        int i = 0;
        try 
        {
            SDFIterator sdfItr = new SDFIterator(inFile);
            while (sdfItr.hasNext())
            {
                i++;
                if (verbosity > 0)
                {
                    System.out.println(" Mutating atom container #" + i);
                }
                IAtomContainer mol = sdfItr.next();
                
                Map<String,ArrayList<IAtom>> targets = 
                                                   identifyMutatingCenters(mol);

                Map<IAtom,String> targetsMap = new HashMap<IAtom,String>();
                for (String k : targets.keySet())
                {
                    for (IAtom atm : targets.get(k))
                    {
                        targetsMap.put(atm,newElms.get(k));
                    }
                }

                mol = mutateContainer(mol,targetsMap, verbosity);

                if (outToFile)
                {
                    IOtools.writeSDFAppend(outFile,mol,true);
                }
            } 
            sdfItr.close();
        } catch (Throwable t) {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Exception returned by "
                + "SDFIterator while reading " + inFile + ": " + t, -1);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Look for the atoms to mutate according to 
     * a customized list of SMARTS queries to be used only within this
     * method. 
     * @param mol the molecule to work with
     * @param tmpSmarts the list of smarts queries with reference names
     * @return the list of matched atoms grouped by reference name of the 
     * given SMARTS queries
     */

    public Map<String,ArrayList<IAtom>> identifyMutatingCenters(
                                                             IAtomContainer mol,
                                                   Map<String,String> tmpSmarts)
    {
        //make backup of SMARTS queries
        Map<String,String> bkpSmarts = new HashMap<String,String>();
        for (String k : smarts.keySet())
        {
            bkpSmarts.put(k,smarts.get(k));
        }

        //now overwrite the queries
        smarts = tmpSmarts;

        //spply standard method
        Map<String,ArrayList<IAtom>> targets = identifyMutatingCenters(mol);

        //restore
        smarts = bkpSmarts;

        return targets;
    }

//-----------------------------------------------------------------------------

    /**
     * Identify the atoms to be mutated using the SMARTS queries loaded in this
     * MolecularMutator.
     * @param mol the molecule to work with
     * @return the map or atoms to mutate grouped by the reference name of the
     * loaded map of SMARTS queries
     */

    public  Map<String,ArrayList<IAtom>> identifyMutatingCenters(
                                                             IAtomContainer mol)
    {
        Map<String,ArrayList<IAtom>> targets = 
                                         new HashMap<String,ArrayList<IAtom>>();
        ManySMARTSQuery msq = new ManySMARTSQuery(mol,smarts,verbosity);
        if (msq.hasProblems())
        {
            String cause = msq.getMessage();
            Terminator.withMsgAndStatus("ERROR! Cannot mutate atoms: "
                + "attempt to use the given SMARTS returns an error. "
                + "Details: " + cause,-1);
        }
        if (msq.getTotalMatches() == 0)
        {
            Terminator.withMsgAndStatus("ERROR! No atom matches the given "
                + "SMARTS queries. "
                + "Make sure at least one SMARTS query matches at least one "
                + "atom per molecule.",-1);
        }
  
        for (String k : smarts.keySet())
        {
            if (msq.getNumMatchesOfQuery(k) == 0)
            {
                if (verbosity > 0)
                {
                    System.out.println("WARNING: SMARTS query '" + k 
                                                  +"' did not match any atom.");
                }
                continue;
            }

            MatchingIdxs allMatches = msq.getMatchingIdxsOfSMARTS(k);
            for (List<Integer> innerList : allMatches)
            {
                for (Integer iAtm : innerList)
                {
                    IAtom targetAtm = mol.getAtom(iAtm);

                    // Check consistency of user request: mutating atoms must be
                    // uniquely identified
                    for (String k2 : smarts.keySet())
                    {
                        if (k2.equals(k))
                        {
                            continue;
                        }
                        if (targets.containsKey(k2))
                        {
                            for (IAtom atm2 : targets.get(k2))
                            {
                                if (targetAtm == atm2)
                                {
                                    Terminator.withMsgAndStatus("ERROR! Atom "
                                        + MolecularUtils.getAtomRef(targetAtm,
                                                                           mol) 
                                        + " matched by SMARTS query '"
                                        + k + "'=" + smarts.get(k) + " and '" 
                                        + k2 + "'=" + smarts.get(k2) + ". "
                                        + "Mutations are permitted only upon "
                                        + "unambiguous identification of the "
                                        + "atoms to mutate",-1);
                                }
                            }
                        }
                    }

                    // Store the matched atom
                    if (targets.containsKey(k))
                    {
                        targets.get(k).add(targetAtm);
                    }
                    else
                    {
                        ArrayList<IAtom> lst = new ArrayList<IAtom>();
                        lst.add(targetAtm);
                        targets.put(k,lst);
                    }
                }
            }
        }

        return targets;
    }

//------------------------------------------------------------------------------


    /**
     * Mutate atoms in a container. 
     * @param iac the complete container
     * @param targetsMap the atoms to mutate and the new elemental symbol
     * @param verbosity the verbosity level
     * @return a new container.
     */

    public static IAtomContainer mutateContainer(IAtomContainer iac, 
                                    Map<IAtom,String> targetsMap, int verbosity)
    {
        IAtomContainer mutatedIAC = new AtomContainer();
        for (IAtom origAtm : iac.atoms())
        {
            IAtom newAtm = new Atom();
            try
            {
                newAtm = (IAtom) origAtm.clone();
                mutatedIAC.addAtom(newAtm);
            }
            catch (CloneNotSupportedException cnse)
            {
                Terminator.withMsgAndStatus("ERROR! Cannot clone atom "
                    + MolecularUtils.getAtomRef(origAtm,iac) + ". Please, "
                    + "report this error to the authors.",-1);
            }
            if (targetsMap.containsKey(origAtm))
            {
                if (verbosity > 1)
                {
                    System.out.println(" Mutating atom "  
                        + MolecularUtils.getAtomRef(origAtm,iac) + " into " 
                        + targetsMap.get(origAtm));
                }
                IAtom newEl = new Atom(targetsMap.get(origAtm));
                newAtm.setSymbol(newEl.getSymbol());
                newAtm.setAtomicNumber(newEl.getAtomicNumber());
                newAtm.setExactMass(newEl.getExactMass());
                newAtm.setMassNumber(newEl.getMassNumber());
                newAtm.setNaturalAbundance(newEl.getNaturalAbundance());
            }
        }
        for (int i=0; i<iac.getBondCount(); i++)
        {
            IBond oldBnd = iac.getBond(i);
            IAtom oldAtm1 = oldBnd.getAtom(0);
            IAtom oldAtm2 = oldBnd.getAtom(1);
            if (oldBnd.getAtomCount() != 2)
            {
                Terminator.withMsgAndStatus("ERROR! Managment of "
                              + "multicenter bonds is not yet implemented.",-1);
            }
            int atmId1 = iac.indexOf(oldAtm1);
            int atmId2 = iac.indexOf(oldAtm2);
            try
            {
                IBond.Order order = oldBnd.getOrder();
                IBond.Stereo stereo = oldBnd.getStereo();
                IBond newBnd = new Bond(mutatedIAC.getAtom(atmId1),
                                        mutatedIAC.getAtom(atmId2), 
                                        order, 
                                        stereo);
            mutatedIAC.addBond(newBnd);
            }
            catch (Throwable t)
            {
                Terminator.withMsgAndStatus("ERROR! Cannot copy bond between"
                    + MolecularUtils.getAtomRef(oldAtm1,iac) + " and "
                    + MolecularUtils.getAtomRef(oldAtm2,iac) + ". Please, "   
                    + "report this error to the authors.",-1);
            }
        }
            
        return mutatedIAC;
    }

//------------------------------------------------------------------------------

}
