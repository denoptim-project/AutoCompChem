package autocompchem.molecule;

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

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.SDFIterator;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.molecule.geometry.ComparatorOfGeometries;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Reorderer for atom lists. This tool allows to reorganize list atoms 
 * according to given criteria. If no optional setting is given the list of
 * atoms will be reorganized starting from the first atom and 
 * following the connectivity.
 * Parameters needed by this class:
 * <ul>
 * <li>
 * <b>INFILE</b> path or name of the SDF file containing the structures to 
 * reorder
 * (only SDF files with ONE molecule are acceptable!).
 * </li>
 * <li>
 * <b>OUTFILE</b> path or name of the SDF file where results are to be
 * written.
 * </li>
 * <li>
 * <b>REFFILE</b> path or name of the SDF file containing the reference 
 * structures: the atom list we'll try to match to 
 * (only SDF files with ONE molecule are acceptable!).
 * </li>
 * <li>
 * <b>SOURCESMARTS</b> (optional)
 * list of SMARTS queries (strings, blank space separated or multiline block)
 * defining the source atoms: the first atoms in the reordered system.
 * Use of this option implies that for each continuously connected networks
 * of atoms (e.g., typically a signle molecule)
 * there is at least one source atom.
 * If no source atom is found for one or more networks
 * the reordering cannot be performed according to the request of the user
 * and the task is terminated with an error message.
 * If more than one atom matches a single SMARTS query, and these candidate
 * source atoms belong to the same network of covalently bonded atoms,
 * the reorganization of the atoms list
 * will start from the atom that, among the candidates, has the lowest index
 * in the original list.
 * If the matched atoms belong to different networks, then they are use each for
 * reorganizing the network (i.e., molecule) they belong to.
 * In case multiple SMARTS queries match atoms of the same molecule,
 * the priority is given according to the order of SMARTS queries specified
 * by the used in this option
 * (i.e., decreasing priority is assumed).
 * </li>
 * <li>
 * <b>VERBOSITY</b> (optional) verbosity level.
 * </li>
 * </ul>
 * 
 * @author Marco Foscato
 */


public class MolecularReorderer extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.REORDERATOMLIST,
                    		TaskID.ALIGNATOMLISTS)));
    
    /**
     * Flag indicating the input is from file
     */
    @SuppressWarnings("unused")
        private boolean inpFromFile = false;

    /**
     * Name of the input file
     */
    private String inFile;

    /**
     * Name of the reference file
     */
    private String refFile;

    /**
     * Flag indicating the output is to be written to file
     */
    private boolean outToFile = false;

    /**
     * Name of the output file
     */
    private String outFile;

    /**
     * Flag indicating SMARTS-controlled reorganisation (default: NO)
     */
    private boolean useSmarts = false;

    /**
     * List (with string identifier) of smarts queries
     */
    private Map<String,String> smarts = new HashMap<String,String>();

    /**
     * FLag reporting that all tasks are done
     */
    @SuppressWarnings("unused")
        private boolean allDone = false;

    /**
     * Name of the atom property used to stamp a visited atom
     */
    private static final String visitedFlag = "VISITEDBYREORDERER";

    /**
     * Name of the atom property used to record the orded of the visits
     */
    private static final String ordCounter = "ORDERLABEL";

    /**
     * Verbosity level
     */
    private int verbosity = 0;


//------------------------------------------------------------------------------

    //TODO move to class doc
    /**
     * Constructs a MolecularReorderer specifying the parameters with a
     * {@link ParameterStorage}. 
     * <ul>
     * <li>
     * <b>INFILE</b> path or name of the SDF file containing the structures to
     * reorder
     * (only SDF files with ONE molecule are acceptable!).
     * </li>
     * <li>
     * <b>OUTFILE</b> path or name of the SDF file where results are to be
     * written.
     * </li>
     * <li>
     * <b>SOURCESMARTS</b> (optional)
     * list of SMARTS queries (strings, blank space separated or multiline 
     * block)
     * defining the source atoms: the first atoms in the reordered system.
     * Use of this option implies that for each continuously connected networks
     * of atoms (e.g., typically a signle molecule)
     * there is at least one source atom.
     * If no source atom is found for one or more networks
     * the reordering cannot be performed according to the request of the user
     * and the task is terminated with an error message.
     * If more than one atom matches a single SMARTS query, and these candidate
     * source atoms belong to the same network of covalently bonded atoms,
     * the reorganization of the atoms list
     * will start from the atom that, among the candidates, has the lowest index
     * in the original list.
     * If the matched atoms belong to different networks, then they are use 
     * each for
     * reorganizing the network (i.e., molecule) they belong to.
     * In case multiple SMARTS queries match atoms of the same molecule,
     * the priority is given according to the order of SMARTS queriest specified
     * by the used in this option
     * (i.e., decreasing priority is assumed).
     * </li>
     * <li>
     * <b>VERBOSITY</b> (optional) verbosity level.
     * </li>
     * </ul>
     *
     * @param params object <code>ParameterStorage</code> containing all the
     * parameters needed
     */

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
            System.out.println(" Reading parameters in MolecularReorderer");

        //Get and check the input file (which has to be an SDF file)
        if (params.contains("INFILE"))
        {
            inpFromFile = true;
            this.inFile = params.getParameter("INFILE").getValue().toString();
            FileUtils.foundAndPermissions(this.inFile,true,false,false);
        }

        //Get and check output file
        if (params.contains("OUTFILE"))
        {
            outToFile = true;
            this.outFile = params.getParameter("OUTFILE").getValue().toString();
            FileUtils.mustNotExist(this.outFile);
        }

        //Get and check the reference file
        if (params.contains("REFFILE"))
        {
            this.refFile = params.getParameter("REFFILE").getValue().toString();
            FileUtils.foundAndPermissions(this.inFile,true,false,false);
        }

        //Get the list of SMARTS to be matched
        if (params.contains("SOURCESMARTS"))
        {
            useSmarts = true;
            String allSamrts = 
                      params.getParameter("SOURCESMARTS").getValue().toString();
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
          case REORDERATOMLIST:
        	  reorderAll();
              break;
          case ALIGNATOMLISTS:
        	  alignAtomList();
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
     * DO NOT USE!!! Work in progress on this method.
     *
     * Reorder all atom lists in the input file trying to match the reference
     * atom list, all according to the current settings.
     */

    public void alignAtomList()
    {

        //Get the reference molecule
        ArrayList<IAtomContainer> mols = new ArrayList<IAtomContainer>();
        mols = IOtools.readSDF(refFile);
        if (mols.size() > 1)
        {
            Terminator.withMsgAndStatus("ERROR! Cannot handle multiple "
                + "reference structures. Please provide a reference file with "
                + "a single entry.",-1);
        }
        IAtomContainer refMol = mols.get(0);

        int i = 0;
        try
        {
            SDFIterator sdfItr = new SDFIterator(inFile);
            while (sdfItr.hasNext())
            {
                i++;
                if (verbosity > 1)
                {
                    System.out.println("Aligning atom container #" + i);
                }
                IAtomContainer mol = sdfItr.next();
                
                ComparatorOfGeometries cog = new ComparatorOfGeometries(
                		verbosity-1);
                Map<Integer,Integer> refToInAtmMap = cog.getAtomMapping(mol,
                		refMol);

                if (refToInAtmMap.size() < mol.getAtomCount())
                {
                    System.out.println("Mapped atoms: "+refToInAtmMap.size());
                    System.out.println("Atom List:    "+mol.getAtomCount());
                    System.out.println("WARNING: some atoms were not matched!");
                    Terminator.withMsgAndStatus("ERROR! Atom map is shorter "
                        + "than atom list. Cannot align atom list.",-1);
                }

                // NB: the information about the new atom order is stored
                // in the properties of each Atom
                ArrayList<Integer> usedTrg = new ArrayList<Integer>();
                for (Map.Entry<Integer, Integer> e : refToInAtmMap.entrySet())
                {
                    IAtom atm = mol.getAtom((Integer) e.getValue());
                    atm.setProperty(ordCounter,(Integer) e.getKey());
                    atm.setProperty(visitedFlag,1);
                    usedTrg.add((Integer) e.getKey());
                }

                // The information in the Atom properties is  now used 
                // to create the new container.
                mol = makeReorderedIAtomContainer(mol);

                if (outToFile)
                {
                    IOtools.writeSDFAppend(outFile,mol,true);
                }
            } //end loop over molecules
            sdfItr.close();
        } catch (Throwable t) {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Exception returned by "
                + "SDFIterator while reading " + inFile + ": " + t, -1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Reorder all atom lists taken from the input file 
     * according to the current settings.
     */

    public void reorderAll()
    {
        int i = 0;
        allDone = false;
        try 
        {
            SDFIterator sdfItr = new SDFIterator(inFile);
            while (sdfItr.hasNext())
            {
                i++;
                if (verbosity > 1)
                {
                    System.out.println("Reordering atom container #" + i);
                }
                IAtomContainer mol = sdfItr.next();
                
                ArrayList<IAtom> sources = identifySourceAtoms(mol);

                mol = reorderContainer(mol,sources);

                if (outToFile)
                {
                    IOtools.writeSDFAppend(outFile,mol,true);
                }
            } //end loop over molecules
            sdfItr.close();
        } catch (Throwable t) {
            t.printStackTrace();
            Terminator.withMsgAndStatus("ERROR! Exception returned by "
                + "SDFIterator while reading " + inFile + ": " + t, -1);
        }
        allDone = true;
    }

//-----------------------------------------------------------------------------

    /**
     * Look for the source atoms according to the criteria given to the 
     * constructor, but
     * imposes a customised list of SMARTS queries to be used only within this
     * method.
     * The priority of SMARTS queries
     * is given by the alphabetic order of the reference names.
     * @param mol the molecule to work with
     * @param tmpSmarts the list of smarts queries
     * @return the list of source atoms
     */

    public ArrayList<IAtom> identifySourceAtoms(IAtomContainer mol, 
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
        ArrayList<IAtom> sources = identifySourceAtoms(mol);

        //restore
        smarts = bkpSmarts;

        return sources;
    }

//-----------------------------------------------------------------------------

    /**
     * Look for the source atoms according to the criteria given to the 
     * constructor. For now only
     * SMARTS-based selection is implemented
     * @param mol the molecule to work with
     * @return the list of source atoms
     */

    public ArrayList<IAtom> identifySourceAtoms(IAtomContainer mol)
    {
        ArrayList<IAtom> sources = new ArrayList<IAtom>();

        if (useSmarts)
        {
            ManySMARTSQuery msq = new ManySMARTSQuery(mol,smarts,verbosity);
            if (msq.hasProblems())
            {
                String cause = msq.getMessage();
                Terminator.withMsgAndStatus("ERROR! Cannot reorganize lists "
                    + "based on SMARTS queries: application of SMARTS returns "
                    + "errors. Details: " + cause,-1);
            }
            if (msq.getTotalMatches() == 0)
            {
                Terminator.withMsgAndStatus("ERROR! No atom matches the given "
                    + "SMARTS queries. "
                    + "Make sure at least one SMARTS query matches at least "
                    + "one atom per molecule.",-1);
            }
  
            ArrayList<String> sortedKeys = new ArrayList<String>();
            sortedKeys.addAll(smarts.keySet());
            Collections.sort(sortedKeys); 
            for (String k : sortedKeys)
            {
                if (msq.getNumMatchesOfQuery(k) == 0)
                {
                    continue;
                }
    
                List<List<Integer>> allMatches = msq.getMatchesOfSMARTS(k);
                for (List<Integer> innerList : allMatches)
                {
                    for (Integer iAtm : innerList)
                    {
                        IAtom sourceAtm = mol.getAtom(iAtm);
                        sources.add(sourceAtm);
                    }
                }
            }
        }
        else
        {
            for (IAtom atm : mol.atoms())
            {
                sources.add(atm); 
            }
        }

        return sources;
    }

//------------------------------------------------------------------------------

    /**
     * Reorder a single list of atoms starting from the first atom.
     * @param iac the complete system to reorder
     * @return a new and reordered object.
     */

    public IAtomContainer reorderContainer(IAtomContainer iac)
    {
        ArrayList<IAtom> source = new ArrayList<IAtom>();
        source.add(iac.getAtom(0));
        return reorderContainer(iac,source); 
    }

//------------------------------------------------------------------------------


    /**
     * Reorder a single list of atoms using the given source atoms. 
     * @param iac the complete system to reorder
     * @param sources the atoms from where to start reordering lister according
     * to decreasing priority.
     * @return a new and reordered object.
     */

    public IAtomContainer reorderContainer(IAtomContainer iac, 
                                                       ArrayList<IAtom> sources)
    {
        //Try using source atoms to reorder the container
        int n=0;
        for (IAtom src : sources)
        {
            n++;
            if (verbosity > 2)
            {
                System.out.println(" Attempt to reorder from source-"+n+": " + 
                                            MolecularUtils.getAtomRef(src,iac));
            }

            if (src.getProperty(visitedFlag) != null)
            {
                if (verbosity > 2)
                {
                    System.out.println(" Skip: previously visited src atom.");
                }
                continue;
            }

            //This comparator defined the priority that will be given to
            //seed atoms during the explroation of the connectivity
            SeedAtomComparator priority = new SeedAtomComparator();

            // This method identifies the fragments/molecules in the container
            // and explores them according to the given comparator of seeds
            ConnectivityUtils.exploreContinuoslyConnectedFromSource(iac, 
                                                                sources, 
                                                            visitedFlag,
                                                             ordCounter, 
                                                               priority);
        }

        //The atom container stores the information leading to the new atom list
        //in the Atom's properties, which are now used to create the new 
        //container with the pre-determined atom list.
        return makeReorderedIAtomContainer(iac);
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a container with the atom list reordered according to the 
     * value of the properties of the Atom objects.
     * @param iac the system with properties set on each Atom.
     * @return a new and reordered object.
     */

    private IAtomContainer makeReorderedIAtomContainer(IAtomContainer iac)
    {
        //Translate property into a map of IDs
        Map<Integer,Map<Integer,Integer>> oldFromNewID = 
                                    new HashMap<Integer,Map<Integer,Integer>>();
        Map<Integer,Map<Integer,Integer>> newFromOldID =
                                    new HashMap<Integer,Map<Integer,Integer>>();
        Map<Integer,Integer> fragFromOldID = new HashMap<Integer,Integer>();
        for (int id=0; id<iac.getAtomCount(); id++)
        {
            IAtom atm = iac.getAtom(id);
            String s = MolecularUtils.getAtomRef(atm,iac);

            if (atm.getProperty(visitedFlag) == null ||
                atm.getProperty(ordCounter) == null)
            {
                s = "ERROR! Atom " + s + " does not belong to any visited "
                    + "network of bonds (i.e., isolated fragment). ";
                if (useSmarts)
                {
                    s = s + "It is likely that no SMARTS query matches any "
                        + "atom of the fragment to with this atom belongs.";
                }
                else
                {
                    s = s + " Please report this potential bug to the authors.";
                }
                Terminator.withMsgAndStatus(s,-1);
            }

            int frgId = (Integer) atm.getProperty(visitedFlag);
            int newId = (Integer) atm.getProperty(ordCounter);
        
            if (verbosity > 1)
            {
                System.out.println(" " + s + " frg:" + frgId + " ord:" + newId);
            }
       
            fragFromOldID.put(id,frgId); 
            if (oldFromNewID.containsKey(frgId))
            {
                oldFromNewID.get(frgId).put(newId,id);
                newFromOldID.get(frgId).put(id,newId);
                
            }
            else
            {
                Map<Integer,Integer> n2oMap = new HashMap<Integer,Integer>();
                Map<Integer,Integer> o2nMap = new HashMap<Integer,Integer>();
                n2oMap.put(newId,id);
                o2nMap.put(id,newId);
                oldFromNewID.put(frgId,n2oMap);
                newFromOldID.put(frgId,o2nMap);
            }
        }

        List<Integer> sortedFrgIds = new ArrayList<Integer>();
        sortedFrgIds.addAll(oldFromNewID.keySet());
        Collections.sort(sortedFrgIds);

        // Build the reordered system
        IAtomContainer orderedIAC = new AtomContainer();
        for (Integer frgId : sortedFrgIds)
        {
            Map<Integer,Integer> n2oMap = oldFromNewID.get(frgId);
            for (int newId=0; newId<n2oMap.keySet().size(); newId++)
            {
                IAtom oldAtm = iac.getAtom(n2oMap.get(newId));
                try
                {
                    IAtom newAtm = (IAtom) oldAtm.clone();
                    orderedIAC.addAtom(newAtm);
                }
                catch (CloneNotSupportedException cnse)
                {
                    Terminator.withMsgAndStatus("ERROR! Cannot clone atom "
                        + MolecularUtils.getAtomRef(oldAtm,iac) + ". Please, "
                        + "report this error to the authors.",-1);
                }
            }
        }
        if (orderedIAC.getAtomCount() != iac.getAtomCount())
        {
            Terminator.withMsgAndStatus("ERROR! Unable to recover all atoms. "
                + "Please, report this bug to the authors.",-1);
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
            int oldAtmId1 = iac.indexOf(oldAtm1);
            int oldAtmId2 = iac.indexOf(oldAtm2);
            int frgId = fragFromOldID.get(oldAtmId1);
            int baseId = 0;
            for (Integer prevFrgId : sortedFrgIds)
            {
                if (frgId == prevFrgId)
                {
                    break;
                }
                baseId = baseId + newFromOldID.get(prevFrgId).size();
            }
            try
            {
                int newAtmId1 = newFromOldID.get(frgId).get(oldAtmId1) + baseId;
                int newAtmId2 = newFromOldID.get(frgId).get(oldAtmId2) + baseId;
                IBond.Order order = oldBnd.getOrder();
                IBond.Stereo stereo = oldBnd.getStereo();
                IBond newBnd = new Bond(orderedIAC.getAtom(newAtmId1),
                                        orderedIAC.getAtom(newAtmId2), 
                                        order, 
                                        stereo);
                orderedIAC.addBond(newBnd);
            }
            catch (Throwable t)
            {
                Terminator.withMsgAndStatus("ERROR! Cannot copy bond between"
                    + MolecularUtils.getAtomRef(oldAtm1,iac) + " and "
                    + MolecularUtils.getAtomRef(oldAtm2,iac) + ". Please, "   
                    + "report this error to the authors.",-1);
            }
        }
            
        return orderedIAC;
    }

//------------------------------------------------------------------------------

}