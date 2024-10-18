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

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
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

/**
 * Tool for changing the order of atoms in an atom container. 
 * This tool allows to reorganize list atoms 
 * according to given criteria. If no optional setting is given the list of
 * atoms will be reorganized starting from the first atom and 
 * following the connectivity.
 * 
 * @author Marco Foscato
 */

public class MolecularReorderer extends AtomContainerInputProcessor
{
    /**
     * Name of the output file
     */
    private File outFile;

    /**
     * Flag indicating SMARTS-controlled reorganisation (default: NO)
     */
    private boolean useSmarts = false;

    /**
     * List (with string identifier) of smarts queries
     */
    private Map<String,SMARTS> smarts = new HashMap<String,SMARTS>();

    /**
     * Name of the atom property used to stamp a visited atom
     */
    private static final String visitedFlag = "VISITEDBYREORDERER";

    /**
     * Name of the atom property used to record the order of the visits
     */
    private static final String ordCounter = "ORDERLABEL";
    
    /**
     * String defining the task of reordering atom list
     */
    public static final String REORDERATOMLISTTASKNAME = "reorderAtomList";

    /**
     * Task about reordering atom list
     */
    public static final Task REORDERATOMLISTTASK;
    static {
    	REORDERATOMLISTTASK = Task.make(REORDERATOMLISTTASKNAME);
    }


//------------------------------------------------------------------------------

    /**
     * Constructor.
     */
    public MolecularReorderer()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
                Arrays.asList(REORDERATOMLISTTASK
                		)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/MolecularReorderer.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new MolecularReorderer();
    }

//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
    	super.initialize();
    	
        //Get and check output file
        if (params.contains("OUTFILE"))
        {
            this.outFile =  new File(
            		params.getParameter("OUTFILE").getValueAsString());
            FileUtils.mustNotExist(this.outFile);
        }

        //Get the list of SMARTS to be matched
        if (params.contains("SOURCESMARTS"))
        {
            useSmarts = true;
            String allSamrts = 
                      params.getParameter("SOURCESMARTS").getValue().toString();
            String[] parts = allSamrts.split("\\s+");
            for (int i=0; i<parts.length; i++)
            {
                String singleSmarts = parts[i];
                if (singleSmarts.equals(""))
                    continue;
                this.smarts.put(Integer.toString(i), new SMARTS(singleSmarts));
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
    	if (task.equals(REORDERATOMLISTTASK))
    	{
    		List<IAtom> sources = identifySourceAtoms(iac);

            IAtomContainer reordered = reorderContainer(iac, sources);

            if (outFile!=null)
            {
            	IOtools.writeSDFAppend(outFile, reordered, true);
            }
            
            if (exposedOutputCollector != null)
            {
	    	    String molID = "mol-"+i;
		        exposeOutputData(new NamedData(molID, reordered));
            }
    	} else {
    		dealWithTaskMismatch();
        }
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

    public List<IAtom> identifySourceAtoms(IAtomContainer mol,
    		Map<String,SMARTS> tmpSmarts)
    {
        //make backup of SMARTS queries
        Map<String,SMARTS> bkpSmarts = new HashMap<String,SMARTS>();
        for (String k : smarts.keySet())
        {
            bkpSmarts.put(k,smarts.get(k));
        }

        //now overwrite the queries
        smarts = tmpSmarts;

        //spply standard method
        List<IAtom> sources = identifySourceAtoms(mol);

        //restore
        smarts = bkpSmarts;

        return sources;
    }

//-----------------------------------------------------------------------------

    /**
     * Look for the source atoms according to the criteria given to the 
     * constructor. For now, only
     * SMARTS-based selection is implemented
     * @param mol the molecule to work with
     * @return the list of source atoms
     */

    public List<IAtom> identifySourceAtoms(IAtomContainer mol)
    {
        List<IAtom> sources = new ArrayList<IAtom>();

        if (useSmarts)
        {
            ManySMARTSQuery msq = new ManySMARTSQuery(mol,smarts);
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
    
                MatchingIdxs allMatches = msq.getMatchingIdxsOfSMARTS(k);
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
    		List<IAtom> sources)
    {
        //Try using source atoms to reorder the container
        int n=0;
        for (IAtom src : sources)
        {
            n++;
            logger.trace("Attempt to reorder from source-"+n+": " + 
            		MolecularUtils.getAtomRef(src,iac));

            if (src.getProperty(visitedFlag) != null)
            {
                logger.trace("Skip: previously visited src atom.");
                continue;
            }

            //This comparator defined the priority that will be given to
            //seed atoms during the exploration of the connectivity
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
        String record = "";
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
        
            record = record + NL + s + " frg:" + frgId + " ord:" + newId;
       
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
        logger.debug(record);

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
