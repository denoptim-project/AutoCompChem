package autocompchem.molecule.atomclashes;

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

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.atom.AtomUtils;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.SDFIterator;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * AtomClashAnalyzer is a tool that analyzes interatomic distances and report
 * atom clashes. Being r<sub>A</sub> and r<sub>B</sub> the v.d.w radii of two
 * atoms, 
 * and <i>d</i> the interatomic distance,
 * AtomClashAnalyzer defines an atom clash if the <i>overlap</i> between 
 * the two atoms is larger than a specific <i>cutoff</i>. 
 * <br><br>
 * <i>overlap</i> = r<sub>A</sub> + r<sub>B</sub> - <i>d</i> - <i>allowance</i>
 * <br><br>
 * where <i>allowance</i> is a value in angstrom that allows to specify that 
 * some atoms can came closer to each other that the sum of their van der Waals 
 * radii.<br>
 * 
 * @author Marco Foscato
 */


public class AtomClashAnalyzer extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.ANALYZEVDWCLASHES)));
    
    //Filenames
    private File inFile;
    private File outFile;

    //
    private boolean makeout = false;

    //List target atoms
    private Map<String,String> targetsmarts = new HashMap<String,String>();

    //cutoff
    private double cutoff = 0.6;

    //Allowance
    private double allowance = 0.4;
    private double allowance13 = 0.0;
    private double allowance14 = 0.0;
    private ArrayList<VDWAllowance> allowances = new ArrayList<VDWAllowance>();

    //Results
    private ArrayList<ArrayList<AtomClash>> results = 
                                  new ArrayList<ArrayList<AtomClash>>();

    //Work on selection of atoms
    private boolean onlyTargetAtms = false;

    //Status flag
    private boolean alreadyRun = false;

    //Run type
    private boolean standalone = true;

    //Verbosity level
    private int verbosity = 1;

//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public AtomClashAnalyzer()
    {
        super("inputdefinition/AtomClashAnalyzer.json");
    }

//------------------------------------------------------------------------------

    @Override
    public Set<TaskID> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<TaskID>(
             Arrays.asList(TaskID.ANALYZEVDWCLASHES)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/AtomClashAnalyzer.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Object... args) {
        return new AtomClashAnalyzer();
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
            System.out.println(" Adding parameters to AtomClashAnalyzer");

        //Get and check the input file (which has to be an SDF file)
        this.inFile = new File(
        		params.getParameter("INFILE").getValue().toString());
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

        //Get optional parameter
        //Get and check output file
        if (params.contains("OUTFILE"))
        {
            this.outFile = new File(
            		params.getParameter("OUTFILE").getValue().toString());
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

        //Cutoff for atoms overlap
        if (params.contains("CUTOFF"))
        {
            this.cutoff = Double.parseDouble(
                 params.getParameter("CUTOFF").getValue().toString());
        }

        //Allowance for all atoms
        if (params.contains("ALLOWANCE"))
        {
            this.allowance = Double.parseDouble(
                 params.getParameter("ALLOWANCE").getValue().toString());
        }

        //Allowance for atoms in 1-3 relationship
        if (params.contains("ALLOWANCE13"))
        {
            this.allowance13 = Double.parseDouble(
                 params.getParameter("ALLOWANCE13").getValue().toString());
        }

        //Allowance for atoms in 1-4 relationship
        if (params.contains("ALLOWANCE14"))
        {
            this.allowance14 = Double.parseDouble(
                 params.getParameter("ALLOWANCE14").getValue().toString());
        }

        //Get custom allowance
        if (params.contains("CUSTOMALLOWANCE"))
        {
            String allAllowance = 
                params.getParameter("CUSTOMALLOWANCE").getValue().toString();
            if (verbosity > 2)
            {
                System.out.println(" Importing CUSTOM ALLOWANCE");
            }
            String[] lines = allAllowance.split("\\r?\\n");
            for (int i=0; i<lines.length; i++)
            {
                String[] parts = lines[i].split("\\s+");
                if (parts.length != 3)
                {
                    String msg = "Wrong number of arguments in definition of "
                                 + "VDW contact allowance.";
                    Terminator.withMsgAndStatus(msg,-1);
                }

                VDWAllowance newAlw = new VDWAllowance(parts[0],  //SMARTS 1
                                                       parts[1],  //SMARTS 2
                                     Double.parseDouble(parts[2])); //allowance

                this.allowances.add(newAlw);
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
          switch (task)
            {
            case ANALYZEVDWCLASHES:
            	analyzeVDWContacts();
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
     * Performs the analysis of the atom clashes according to the parameters
     * provided to the constructor: it assumes that the AtomClashAnalyzer is
     * used in a standalone fashion (reading structures from files).
     */

    public void analyzeVDWContacts()
    {
        int i = -1;
        int nacmols = 0;
        try {
            SDFIterator sdfItr = new SDFIterator(inFile);
            while (sdfItr.hasNext())
            {
                //Get the molecule
                i++;
                IAtomContainer mol = sdfItr.next();
                String molName = MolecularUtils.getNameOrID(mol);

                //Get target atoms                
                if (verbosity > 0)
                {
                    System.out.println(" Analysing atom clashes in molecule " 
                                        + molName + " (" + i + ")");
                }

                //Perform analysis
                boolean foundClash = analyzeAtomClashes(mol);

                //In case of clashes
                if (foundClash)
                {
                    nacmols++;

                    if (verbosity > 0)
                    {
                        ArrayList<AtomClash> acs = results.get(i);
                        System.out.println(" Found " + acs.size() 
                                                + " atom clashes: ");
                        for (AtomClash ac : acs)
                            System.out.println(ac);
                    }

                    if (makeout)
                    {
                        //store results in output SDF
                        writeAtmClashToSDFFields(mol,i,outFile);
                    }
                }

            } //end loop over molecules
            sdfItr.close();
        } catch (Throwable t) {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Exception returned by "
                + "SDFIterator while reading " + inFile, -1);
        }

        if (verbosity > 0)
        {
            System.out.println(" Found " + nacmols + " mols with one or more "
                                + "atom clashes");
        }

        //Set flag
        alreadyRun = true;
    }

//-----------------------------------------------------------------------------

    /**
     * Perform the analysis of atom clashes on a single molecule. The analysis
     * is performed according to the parameters provided to the constructor.
     * Details such as the actual list of atom clashes and the atoms involved,
     * are stored and can be obtained with the 
     * {@link #getAllResults getAllResults} and 
     * {@link #getSingleResults getSingleResults}.
     * @param mol the molecule to analyze
     * @return <code>true</code> in the molecule contains one or more atom 
     * clashes.
     */

    public boolean analyzeAtomClashes(IAtomContainer mol)
    {
        boolean foundAtomClash = false;
        ArrayList<AtomClash> acList = new ArrayList<AtomClash>();

        //Identify target atoms
        ArrayList<IAtom> targets = new ArrayList<IAtom>();
        if (onlyTargetAtms)
        {
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
                    MatchingIdxs matches =  msq.getMatchingIdxsOfSMARTS(key);
                    for (List<Integer> innerList : matches)
                    {
                        for (Integer iAtm : innerList)
                        {
                            IAtom targetAtm = mol.getAtom(iAtm);
                            targets.add(targetAtm);
                        }
                    }
                }
            }
        } else {
            for (IAtom a : mol.atoms())
                targets.add(a);
        }
        if (verbosity > 1)
        {
            System.out.println(" Searching clashes among " 
                                + targets.size() + " atoms");
        }
        if (verbosity > 3)
        {
            System.out.println("List of atoms: " + targets);
        }

        //Get custom allowances
        Map<String,Double> allowanceMasck = new HashMap<String,Double>();
        if (allowances.size() != 0)
        {
            //make map of smarts
            Map<String,String> smarts = new HashMap<String,String>();

            for (int ikey = 0; ikey<allowances.size(); ikey++)
            {
                VDWAllowance a = allowances.get(ikey);
                String[] atmsmarts = a.getSmarts();
                for (int jkey = 0; jkey<2; jkey++)
                {
                    String key = Integer.toString(ikey) + "_"
                                 + Integer.toString(jkey); 
                    smarts.put(key,atmsmarts[jkey]);
                }
            }

            //identify atoms subject to custom allowance
            ManySMARTSQuery msq = new ManySMARTSQuery(mol,smarts,verbosity);
            if (msq.hasProblems())
            {
                String cause = msq.getMessage();
                Terminator.withMsgAndStatus("ERROR! " +cause,-1);
            }
            for (int iikey=0; iikey<allowances.size(); iikey++)
            {
                String key1 = Integer.toString(iikey) + "_0";
                String key2 = Integer.toString(iikey) + "_1";
                if (msq.getNumMatchesOfQuery(key1) == 0 || 
                            msq.getNumMatchesOfQuery(key2) == 0)
                {
                    continue;
                }
                MatchingIdxs allMatches1 =  msq.getMatchingIdxsOfSMARTS(key1);
                MatchingIdxs allMatches2 =  msq.getMatchingIdxsOfSMARTS(key2);
                for (List<Integer> innerList1 : allMatches1)
                {
                    for (Integer iAtm1 : innerList1)
                    {
                        String pairkey = "";
                        for (List<Integer> innerList2 : allMatches2)
                        {
                            for (Integer iAtm2 : innerList2)
                            {
                                if (iAtm1 == iAtm2)
                                    continue;
                                if (iAtm1 < iAtm2)
                                    pairkey = iAtm1 + "_" + iAtm2;
                                else
                                    pairkey = iAtm2 + "_" + iAtm1;

                                double alwVal = 
                                          allowances.get(iikey).getAllowance();
                                allowanceMasck.put(pairkey,alwVal);
                            }
                        }
                    }
                }
            }
        }

        //Analyze interatomic destances between pairs of target atoms
        for (int i = 0; i<targets.size(); i++)
        {
            IAtom atmI = targets.get(i);
            List<IAtom> nbrsI = mol.getConnectedAtomsList(atmI);
            
            for (int j = i+1; j<targets.size(); j++)
            {
                IAtom atmJ = targets.get(j);
                List<IAtom> nbrsJ = mol.getConnectedAtomsList(atmJ);

                //Ignore bound pairs
                if (nbrsI.contains(atmJ))
                    continue;

                //Evaluate need of 1-N allowance
                boolean found = false;
                double alw = allowance;
                for (IAtom nI : nbrsI)
                {
                    if (allowance13 > 0 && nbrsJ.contains(nI))
                    {
                        // 1-3 relationship
                        alw = allowance13;
                        break;
                    }
                     else if (allowance14 > 0)
                    {
                        List<IAtom> nbrsNI = mol.getConnectedAtomsList(nI);
                        for (IAtom nnI : nbrsNI)
                        {
                            if (nbrsJ.contains(nnI))
                            {
                                // 1-4 relationship
                                alw = allowance14;
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found)
                        break;
                }

                //Evaluate need of custom allowance
                String pairkey = "";
                int iAtm1 = mol.indexOf(atmI);
                int iAtm2 = mol.indexOf(atmJ);
                if (iAtm1 < iAtm2)
                    pairkey = iAtm1 + "_" + iAtm2;
                else
                    pairkey = iAtm2 + "_" + iAtm1;
                if (allowanceMasck.containsKey(pairkey))
                {
                    alw = allowanceMasck.get(pairkey);
                }

                //Get distance 
                double d = MolecularUtils.calculateInteratomicDistance(atmI,
                                                                       atmJ);

                //Get vdw radii sum (corrected with allowance)
                double vdwsum = AtomUtils.getVdwRadius(atmI) 
                                + AtomUtils.getVdwRadius(atmJ);

                double overlap = vdwsum
                                - d 
                                - alw;

                //Check for atom clash
                if (overlap >= cutoff)
                {
                    foundAtomClash = true;
                    String refI = MolecularUtils.getAtomRef(atmI,mol);
                    String refJ = MolecularUtils.getAtomRef(atmJ,mol);
                    AtomClash ac = new AtomClash(atmI,refI,atmJ,refJ,d,vdwsum,alw);
                    acList.add(ac);
                }
            }
        }

        //store
        results.add(acList);

        return foundAtomClash;
    }

//-----------------------------------------------------------------------------

    /**
     * Writes infomration on atom clashes in SDF fields. The output file can
     * be new of existing, in which case the molecule will be appended at the
     * end.
     * @param mol the molecular system
     * @param i index of record in the results storage
     * @param file the output SDF file
     */

    public void writeAtmClashToSDFFields(IAtomContainer mol, int i, 
                                         File file)
    {
        ArrayList<AtomClash> acs = results.get(i);
        
        //find closes
        @SuppressWarnings("unused")
                AtomClash worstac;
        double worstoverlap = 0.0;
        for (AtomClash ac : acs)
        {
            if (ac.getOverlap() > worstoverlap)
            {
                worstoverlap = ac.getOverlap();
                worstac = ac;
            } 
        }

        //prepare fields
        mol.setProperty("WorstOverlap",worstoverlap);
        mol.setProperty("AtomClashes",acs);

        //write
        IOtools.writeSDFAppend(file,mol,true);
    }

//-----------------------------------------------------------------------------

    /**
     * Return the results for all molecules.
     * @return a double-layer list with all the results (inner layer) 
     * for all molecules (outer list)
     */

    public ArrayList<ArrayList<AtomClash>> getAllResults()
    {
        if (!alreadyRun && standalone)
            this.analyzeVDWContacts();

        return results;
    }

//-----------------------------------------------------------------------------
    
    /**
     * Return the results for a single molecule.
     * The molecule is identified by the position (0-n) in the input.
     * @param molID the index of the molecule for which the result is required
     * @return the list of atom clashes in the given molecule
     */

    public ArrayList<AtomClash> getSingleResults(int molID)
    {
        if (!alreadyRun && standalone)
            this.analyzeVDWContacts();

        ArrayList<AtomClash> ac = results.get(molID);
        return ac;
    }

//-----------------------------------------------------------------------------

    /**
     * Sets the SMARTS queries used to identify the target atoms.
     * @param targetsmarts a map is used to provide pairs of key:values
     * where the keys are just reference names to identify the SMARTS
     * and the values are the actual SMARTS queries.
     */

    public void setTargetAtomsSMARTS(Map<String,String> targetsmarts)
    {
        this.targetsmarts = targetsmarts;
    }

//-----------------------------------------------------------------------------

    /**
     * Sets the cutoff value
     * @param cutoff the cutoff value to impose
     */

    public void setCutoff(double cutoff)
    {
        this.cutoff = cutoff;
    }

//-----------------------------------------------------------------------------

    /**
     * Sets the general allowance
     * @param allowance the value to impose to the general allowance
     */

    public void setAllowance(double allowance)
    {
        this.allowance = allowance;
    }

//-----------------------------------------------------------------------------

    /**
     * Sets the allowance for atoms in 1-3 relationship
     * @param allowance13 the value to impose to the allowance for atoms in
     * 1-3 relation
     */

    public void setAllowance13(double allowance13)
    {
        this.allowance13 = allowance13;
    }

//-----------------------------------------------------------------------------

    /**
     * Sets the allowance for atoms in 1-4 relationship
     * @param allowance14 the value to impose to the allowance for atoms in 
     * 1-4 relation
     */

    public void setAllowance14(double allowance14)
    {
        this.allowance14 = allowance14;
    }

//-----------------------------------------------------------------------------

    /**
     * Sets the list of custom allowance
     * @param allowances list of curtom allowances 
     */

    public void setCustomAllowances(ArrayList<VDWAllowance> allowances)
    {
        this.allowances = allowances;
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
