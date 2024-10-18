package autocompchem.molecule.dummyobjects;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import java.util.Set;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.openscience.cdk.Bond;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.atom.AtomConstants;
import autocompchem.atom.AtomUtils;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.utils.NumberUtils;
import autocompchem.utils.ThreeDimensionalSpaceUtils;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Tool to handle dummy objects, such as, dummy atoms 
 * and bonds, within molecular representations. 
 * 
 * @author Marco Foscato
 */


public class DummyObjectsHandler extends AtomContainerInputProcessor
{
    /**
     * Pathname to output file
     */
    private File outFile;

    /**
     * Pathname to file containing the template
     */
    private File tmplFile;

    /**
     * List of source atoms to dummy-related action
     */
    private List<Integer> activeSrcAtmIds = new ArrayList<Integer>();

    /**
     * Template system
     */
    private IAtomContainer template;
    
    /**
     * Known types of dummy atoms
     */
    public enum DummyAtomType {LINEARITY,MULTIHAPTO,ANY};

    /**
     * Flag controlling action on multihapto systems
     */
    private boolean doMultihapto = false;

    /**
     * Flag controlling action on linearities
     */
    private boolean doLinearities = false;

    /**
     * Flag controlling action on planar atoms
     */
    private boolean doPlanarities = false;

    //Elemental symbol of dummy atoms to deal with
    private String elm = null;
    
    /**
     * String defining the task of adding dummy atoms
     */
    public static final String ADDDUMMYATOMSTASKNAME = "addDummyAtoms";

    /**
     * Task about adding dummy atoms
     */
    public static final Task ADDDUMMYATOMSTASK;
    static {
    	ADDDUMMYATOMSTASK = Task.make(ADDDUMMYATOMSTASKNAME);
    }
    /**
     * String defining the task of removing dummy atoms
     */
    public static final String REMOVEDUMMYATOMSTASKNAME = "removeDummyAtoms";

    /**
     * Task about removing dummy atoms
     */
    public static final Task REMOVEDUMMYATOMSTASK;
    static {
    	REMOVEDUMMYATOMSTASK = Task.make(REMOVEDUMMYATOMSTASKNAME);
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor.
     */
    public DummyObjectsHandler()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
                Arrays.asList(ADDDUMMYATOMSTASK,
                		REMOVEDUMMYATOMSTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/DummyObjectsHandler.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new DummyObjectsHandler();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
    	super.initialize();

        // Get options
        if (params.contains("LINEARITIES"))
        {
            this.doLinearities = true;
        }
        if (params.contains("PLANARITIES"))
        {
            this.doPlanarities = true;
        }
        if (params.contains("MULTIHAPTO"))
        {
            this.doMultihapto = true;
        }
        if (params.contains("DUSYMBOL"))
        {
            String e = params.getParameter("DUSYMBOL").getValue().toString(); 
            this.elm = e;
        }

        if (params.contains("SOURCEATOMS"))
        {
            String l = params.getParameter("SOURCEATOMS").getValue().toString();
            String[] words = l.trim().split("\\s+");
            for (int i=0; i<words.length; i++)
            {
                String w = words[i];
                if (NumberUtils.isNumber(w))
                //NOTE: we assume 1-based indexing
                this.activeSrcAtmIds.add(Integer.parseInt(w)-1);
            }
        }

//TODO: check consistency between use of template, or list of sources, or doLinear, etc.

        //Get and check the input file (which has to be an SDF file)
        if (params.contains("TEMPLATE"))
        {
            this.tmplFile = new File(
            		params.getParameter("TEMPLATE").getValueAsString());
            FileUtils.foundAndPermissions(this.inFile,true,false,false);
            List<IAtomContainer> inTmpls = IOtools.readSDF(this.tmplFile);
            if (inTmpls.size() != 1)
            {
                Terminator.withMsgAndStatus("ERROR! Can only accept a single "
                                + "template for adding dummy atoms. Check '" 
                                + this.tmplFile + "'",-1);
            }
            this.template = inTmpls.get(0);
        }

        //Get and check the output file name
        if (params.contains("OUTFILE"))
        {
	        this.outFile =  new File(
	        		params.getParameter("OUTFILE").getValueAsString());
	        FileUtils.mustNotExist(this.outFile);
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
    	if (task.equals(ADDDUMMYATOMSTASK))
    	{
    		addDummyAtoms(iac, i);
    	} else if (task.equals(REMOVEDUMMYATOMSTASK)){
    		removeDummyAtoms(iac, i);
    	} else {
    		dealWithTaskMismatch();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Add dummy atoms according to parameters given to constructor.
     */

    private void addDummyAtoms(IAtomContainer iac, int i)
    {
	    if (template != null)
	    {
	        copypasteDummyAtoms(iac,template);
	    }
	    else
	    {
	        if (doLinearities)
	        {
	            includeLinearities(iac);
	        }
	
	        if (doPlanarities)
	        {
	            includePlanarities(iac);
	        }
	        if (doMultihapto)
	        {
	            //TODO:
	            Terminator.withMsgAndStatus("ERROR! Code for adding dummy "
	            		+ "atoms to multihapto systems is not implemented yet.",
	            		-1);
	        }
	
	        if (0 < activeSrcAtmIds.size())
	        {
	            addDummiedOnSources(iac, this.activeSrcAtmIds);
	            this.activeSrcAtmIds.clear();
	        }
	    }

        if (outFile!=null)
        	IOtools.writeSDFAppend(outFile, iac, true);
        
        if (exposedOutputCollector != null)
        {
    	    String molID = "mol-"+i;
	        exposeOutputData(new NamedData(molID, iac));
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Add linearity- and planarity-breaking dummy atoms according to a given 
     * template. WARNING! Assumes consistent atom list. Therefore, the dummies
     * must be all at end of the atom list of the template.
     * @param mol the molecule to work with
     * @param tmpl the template
     */

    private static void copypasteDummyAtoms(IAtomContainer mol, 
    		IAtomContainer tmpl)
    {
        for (IAtom du : tmpl.atoms())
        {
            List<IAtom> nbrs = tmpl.getConnectedAtomsList(du);
            
            if (!AtomUtils.isAccDummy(du) || nbrs.size()!=1)
            {
                continue;
            }

            IAtom src = nbrs.get(0);
            int srcId = tmpl.indexOf(src);
            nbrs = tmpl.getConnectedAtomsList(src);
            if (nbrs.size() < 3)
            {
                    Terminator.withMsgAndStatus("ERROR! Source atom " 
                        + MolecularUtils.getAtomRef(src,tmpl) + " is only "
                        + "connected to too few atoms for dummy "
                        + MolecularUtils.getAtomRef(du,tmpl) + " to represent "
                        + "a linearity-breaking point.", -1); 
            }

                    //TODO: if possible (linearity can prevent it) place the Du
                    // in a position that is closely related to the relative
                    // position of the Du and the src and nbrs atoms
                    // When not possible fall back on general behaviour.

            IAtom srcInMol = mol.getAtom(srcId);
            IAtom duAtm = getDummyInSafePlace(mol,srcInMol);
            mol.addAtom(duAtm);
            IBond dummyBnd = new Bond(duAtm,srcInMol);
            mol.addBond(dummyBnd);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Remove dummy atoms and alter connectivity accordingly to the stored
     * parameters
     */

    private void removeDummyAtoms(IAtomContainer iac, int i)
    {
        if (doLinearities)
        {
        	removeSpecialDummyAtoms(iac, elm, DummyAtomType.LINEARITY);
        }
/*
//TODO: discriminate between linear an planar systems. Now, they are not distinguishable in DummyAtomHandler
                if (doPlanarities)
                {
                    
                }
*/
        if (doMultihapto)
        {
        	removeSpecialDummyAtoms(iac, elm, DummyAtomType.MULTIHAPTO);
        }

        if (outFile!=null)
        	IOtools.writeSDFAppend(outFile, iac, true);
        
        if (exposedOutputCollector != null)
        {
    	    String molID = "mol-"+i;
	        exposeOutputData(new NamedData(molID, iac));
    	}
    }

//-----------------------------------------------------------------------------

    /**
     * Append dummy atoms to the atom list and in proximity of linear systems.
     * The placement of the dummy aim to allow definition
     * reasonable internal coordinates
     * @param mol the molecular object to be modified
     */

    public void addDummiesOnLinearities(IAtomContainer mol)
    {
        includeLinearities(mol);
        addDummiedOnSources(mol,activeSrcAtmIds);
    }

//-----------------------------------------------------------------------------

    /**
     * Append dummy atoms to the atom list and in proximity of each trigonal
     * planar system.
     * The placement of the dummy aims to allow definition of
     * reasonable internal coordinates
     * @param mol the molecular object to be modified
     */

    public void addDummiesOnPlanarities(IAtomContainer mol)
    {
        includePlanarities(mol);
        addDummiedOnSources(mol,activeSrcAtmIds);
    }

//-----------------------------------------------------------------------------

    /**
     * Append the atom sources matching the definition of lineality.
     * @param mol the molecular object to be modified
     */

    private void includeLinearities(IAtomContainer mol)
    { 
        for (IAtom srcAtm : mol.atoms())
        {
//TODO replace with method from AtomGeometryDetector
            List<IAtom> nbrs = mol.getConnectedAtomsList(srcAtm);
            for(int i=0; i<nbrs.size(); i++)
            {
                IAtom atmL = nbrs.get(i);
                for (int j=i+1; j<nbrs.size(); j++)
                {
                    IAtom atmR = nbrs.get(j);
                    double ang = MolecularUtils.calculateBondAngle(atmL,srcAtm,
                                                                          atmR);
                    logger.trace("Checking for linearity of: "
                        + MolecularUtils.getAtomRef(atmL,mol)
                        + "-" + MolecularUtils.getAtomRef(srcAtm,mol)
                        + "-" + MolecularUtils.getAtomRef(atmR,mol)
                        + " angle: " + ang);
                    if (DummyObjectsConstants.LINEARANGLE < ang)
                    {
                        logger.trace("Adding linearity-breaking "
                                + "dummy atom on: "  
                                + MolecularUtils.getAtomRef(srcAtm,mol));
                        this.activeSrcAtmIds.add(mol.indexOf(srcAtm));
                    }
                }
            }
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Append the atom sources matching the definition of planarity.
     * @param mol the molecular object to be modified
     */

    private void includePlanarities(IAtomContainer mol)
    {
        for (IAtom srcAtm : mol.atoms())
        {
//TODO replace with method from AtomGeometryDetector
            List<IAtom> nbrs = mol.getConnectedAtomsList(srcAtm);
            if (nbrs.size() != 3)
            {
                continue;
            }
            double dih = MolecularUtils.calculateTorsionAngle(nbrs.get(0),
                                                              srcAtm,
                                                              nbrs.get(1),
                                                              nbrs.get(2));
            logger.trace("For trigonal "  
                        + MolecularUtils.getAtomRef(srcAtm,mol) 
                        + "(" + MolecularUtils.getAtomRef(nbrs.get(0),mol) 
                        + ")(" + MolecularUtils.getAtomRef(nbrs.get(1),mol)
                        + ")" + MolecularUtils.getAtomRef(nbrs.get(2),mol)
                        + " dih: " + dih);
            if (DummyObjectsConstants.LINEARANGLE < Math.abs(dih))
            {
                logger.debug("Adding planarity-breaking du on: " 
                                       + MolecularUtils.getAtomRef(srcAtm,mol));
                this.activeSrcAtmIds.add(mol.indexOf(srcAtm));
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Add dummy atoms to all atoms in a given set of atoms
     * @param mol the existing atom list
     * @param srcs the list of atoms to which we want to add dummies
     */

    public void addDummiedOnSources(IAtomContainer mol, List<Integer> srcs)
    {
        for (int i=0; i< srcs.size(); i++)
        {
            int atmId = srcs.get(i);
            if (0>atmId || (mol.getAtomCount()-1)<atmId)
            {
                Terminator.withMsgAndStatus("ERROR! Atom index out of range "
                    + "while adding dummy atoms. Requesting atom (1-based) '" 
                    + (atmId+1) + "' on molecule with " + mol.getAtomCount()
                    + " atoms.", -1);
            }
            IAtom srcAtm = mol.getAtom(atmId);
            IAtom duAtm = getDummyInSafePlace(mol,srcAtm);
            mol.addAtom(duAtm);
            IBond dummyBnd = new Bond(duAtm,srcAtm);
            mol.addBond(dummyBnd);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Chose a position of a dummy atom. 
     * @param mol the existing atom list
     * @param src the atom to which the dummy is meant to be bound
     */

    private static IAtom getDummyInSafePlace(IAtomContainer mol, IAtom src)
    {
        // Define forbidden zones: places where we cannot place the dummy atom 
        ArrayList<Vector3d> allForbiddenDirs = new ArrayList<Vector3d>();
        // Define candidate location: perpendicular to a bond and all around it
        ArrayList<Vector3d> allCandidateDirs = new ArrayList<Vector3d>();
        for (IAtom nbr : mol.getConnectedAtomsList(src))
        {
            // Find versor towards bonded neighbour to forbidden direction
            Vector3d v = MolecularUtils.getVectorFromTo(src,nbr);
            v.normalize();
                //TODO: check if already existing before adding

            allForbiddenDirs.add(v);
            // Find versor on inverted direction to forbidden direction
            Vector3d vOpposite = new Vector3d(-v.x,-v.y,-v.z);
            vOpposite.normalize();
                //TODO: check if already existing before adding
            allForbiddenDirs.add(vOpposite);

            // Find a perpendicular versor
            Vector3d vPerp = ThreeDimensionalSpaceUtils.getNormalDirection(v);
            vPerp.normalize();
            // Find many other perpendicular versors by rotation of vPerp
            double angle = 33.0;
            int nSteps = (int) (360.0/angle);
            for (int i=0; i<nSteps; i++)
            {
                Vector3d vCandidate = 
                       ThreeDimensionalSpaceUtils.rotatedVectorWAxisAngle(vPerp,
                                                                     v,angle*i);
                //TODO: check if already existing before adding
                allCandidateDirs.add(vCandidate);
            }
            // Find many other versors by rotation of 45deg rotated vPerp
            vPerp.add(v);
            vPerp.normalize();
            for (int i=0; i<nSteps; i++)
            {
                Vector3d vCandidate = 
                       ThreeDimensionalSpaceUtils.rotatedVectorWAxisAngle(vPerp,
                                                                     v,angle*i);
                allCandidateDirs.add(vCandidate);
            }
        }

        //TODO: 
        // add forbidden directions based on criteria that are external to
        // the current geometry of the system, and that are define outside

        // Search for the candidate versor that is farther apart from any
        // forbidded versor
        
        double bestVal = 1.0;
        int bestId = -1;
        Point3d srcP3d = AtomUtils.getCoords3d(src);
        for (int i=0; i<allCandidateDirs.size(); i++)
        {
            double worstVal = 0.0;
            for (int j=0; j<allForbiddenDirs.size(); j++)
            {
                double val = Math.abs(
                         allForbiddenDirs.get(j).dot(allCandidateDirs.get(i)));
                if (worstVal < val)
                {
                    worstVal = val;
                }
                if (bestVal < val)
                {
                    break;
                }
            }
            if (worstVal < bestVal)
            {
                bestVal = worstVal;
                bestId = i;
            }
        }

        Vector3d bestCandidate = allCandidateDirs.get(bestId);
        bestCandidate.scale(Math.sqrt(DummyObjectsConstants.DUBNDLENGTH));
        Point3d duP3d = new Point3d(bestCandidate.x + srcP3d.x,
                                    bestCandidate.y + srcP3d.y,
                                    bestCandidate.z + srcP3d.z);
        IAtom duAtm = new PseudoAtom(AtomConstants.DUMMYATMLABEL,duP3d);
        duAtm.setProperty(AtomConstants.DUMMYATMLABEL, AtomConstants.DUMMYATMLABEL);
        
        return duAtm;
    }
    
//------------------------------------------------------------------------------
    
    private void removeSpecialDummyAtoms(IAtomContainer mol, String elm, 
    		DummyAtomType type)
    {
        List<IAtom> dummiesList = new ArrayList<IAtom>();
        List<Integer> dummiesIdxs = new ArrayList<Integer>();
        
        //Identify the target atoms to be treated
        for (IAtom atm : mol.atoms())
        {
            String symbol = atm.getSymbol();

            if (type.equals(DummyAtomType.MULTIHAPTO))
            {
	            // Du in hapto MUST have more than one connected neighbour
	            if (mol.getConnectedBondsCount(atm) <= 1)
	            {
	                continue;
	            }
            }
            else if (type.equals(DummyAtomType.LINEARITY)) 
            {
            	if (mol.getConnectedBondsCount(atm) > 1)
                {
                    continue;
                }
            }

            if (elm == null)
            {
                if (AtomUtils.isAccDummy(atm))
                {
                    dummiesList.add(atm);
                    dummiesIdxs.add(mol.indexOf(atm));
                }
            }
            else
            {
                if (symbol.equals(elm))
                {
                    dummiesList.add(atm);
                    dummiesIdxs.add(mol.indexOf(atm));
                }
                else
                {
                	if (AtomUtils.isPseudoAtmWithLabel(atm,elm))
                	{
                		dummiesList.add(atm);
                        dummiesIdxs.add(mol.indexOf(atm));
                	}
                }
            }
        }

        String msg = ("#Candidate dummy atoms (" + type + "): "
            		+ dummiesList.size()) + NL;
        for (IAtom du : dummiesList)
        {
           msg = msg + " -> Atom "+ MolecularUtils.getAtomRef(du,mol);
        }
        logger.debug(msg);

        //Delete dummy atoms and change connectivity
        for (IAtom du : dummiesList)
        {
            //Remove Du-[*] Bonds
            List<IAtom> nbrOfDu = mol.getConnectedAtomsList(du);
            int numOfTerms = nbrOfDu.size();
            for (IAtom nbr : nbrOfDu)
            {
                mol.removeBond(du,nbr);
            }
            
            // Here we change the connectivity between multihapto ligand and central atom
            if ((type.equals(DummyAtomType.MULTIHAPTO) 
            		|| type.equals(DummyAtomType.ANY))
            		&& nbrOfDu.size()>1)
            {
            	//Identify atoms of ligand in mupltihapto system
	            logger.trace("Fixing connectivity for: " 
	                		+ MolecularUtils.getAtomRef(du,mol)
	                        + " #neighbours: "+nbrOfDu.size());
	
	            List<Boolean> found = getFlagsVector(numOfTerms);
	            List<Set<IAtom>> goupsOfTerms = new ArrayList<Set<IAtom>>();
	            List<Integer> hapticity = new ArrayList<Integer>();
	            for (int i=0; i<numOfTerms; i++)
	            {
	                //Was it found already?
	                if (found.get(i))
	                    continue;
	
	                //Look for ligand starting from this term
	                IAtom nbrI = nbrOfDu.get(i);
	                Set<IAtom> ligOfNbrI = exploreConnectedToAtom(
	                		nbrI,nbrOfDu,mol,found);
	                if (!ligOfNbrI.isEmpty())
	                {
	                    goupsOfTerms.add(ligOfNbrI);
	                    hapticity.add(ligOfNbrI.size());
	                }
	
	            } //end of loop over neighbours of Dummy
	
	            String msg2 = "#Groups of terms: " + goupsOfTerms.size() + NL;
                for (int i=0; i<goupsOfTerms.size(); i++)
                {
                    Set<IAtom> s = goupsOfTerms.get(i);
                    msg2 = msg2 +" Group " + i 
                                     + " - Hapticity: " + hapticity.get(i) 
                                     + " => " ;
                    for (IAtom sa : s)
                    {
                    	msg2 = msg2 + (mol.indexOf(sa)+1) + sa.getSymbol()+" ";
                    }
                    msg2 = msg2 + (" ");
	            }
                logger.debug(msg2);
	
	            // If Du is in between groups, connectivity has to be fixed
	            if (goupsOfTerms.size() > 1)
	            {
	                //Identify the ligand corresponding to Du
	                int ligandID = -1;
	                boolean ligandFound = false;
	                List<Point3d> allCandidates = new ArrayList<Point3d>();
	                for (int i=0; i<goupsOfTerms.size(); i++)
	                {
	                    Set<IAtom> grp = goupsOfTerms.get(i);
	
	                    //Identify center of the group of terms
	                    Point3d candidateDuP3d = new Point3d();
	                    for (IAtom atm : grp)
	                    {
	                        try 
	                        {
	                            Point3d ligP3d = atm.getPoint3d();
	                            candidateDuP3d.x = candidateDuP3d.x + ligP3d.x;
	                            candidateDuP3d.y = candidateDuP3d.y + ligP3d.y;
	                            candidateDuP3d.z = candidateDuP3d.z + ligP3d.z;
	                        } 
	                        catch (Throwable t) 
	                        {
	                            Point2d ligP2d = atm.getPoint2d();
	                            candidateDuP3d.x = candidateDuP3d.x + ligP2d.x;
	                            candidateDuP3d.y = candidateDuP3d.y + ligP2d.y;
	                            candidateDuP3d.z = 0.0000; 
	                        }
	                    }
	                    allCandidates.add(candidateDuP3d);
	                    candidateDuP3d.x = candidateDuP3d.x 
	                    		/ (double) hapticity.get(i);
	                    candidateDuP3d.y = candidateDuP3d.y 
	                    		/ (double) hapticity.get(i);
	                    candidateDuP3d.z = candidateDuP3d.z 
	                    		/ (double) hapticity.get(i);
	
	                    //Get coords of du
	                    Point3d dummyP3d = new Point3d();
	                    try 
	                    {
	                        Point3d du3d = du.getPoint3d();
	                        dummyP3d.x = du3d.x;
	                        dummyP3d.y = du3d.y;
	                        dummyP3d.z = du3d.z;
	                    } 
	                    catch (Throwable t) 
	                    {
	                        Point2d du2d = du.getPoint2d();
	                        dummyP3d.x = du2d.x;
	                        dummyP3d.y = du2d.y;
	                        dummyP3d.z = 0.0000;
	                    }
	
	                    //Check if Du is the center of this group of terms
	                    double dist = candidateDuP3d.distance(dummyP3d);
	                    if (dist < 0.002)
	                    {
	                        if (ligandFound)
	                        {
	                            String msg3 = "More then one group of atoms may "
	                                         + "correspond to the ligand. Not "
	                                         + "able  to identify the ligand!";
	                            Terminator.withMsgAndStatus(msg3, -1);
	                        }
	                        ligandID = i;
	                        ligandFound = true;
	                    }
	                }
	
	                //In case of no matching return the error
	                if (!ligandFound)
	                {
	                    String msg3 = "Dummy atom does not seem to be placed at "
	                                 + "the centroid of a multihapto ligand. "
	                                 + "Du: " + du
	                                 + "Candidates: " + allCandidates
	                                 + "See current molecule in 'error.sdf'";
	                    IOtools.writeSDFAppend(new File("error.sdf"),mol,false);
	                    Terminator.withMsgAndStatus(msg3, -1);
	                }
	                
	                //Connect every atom from the multihapto ligand with
	                // the central atom/atoms
	                Set<IAtom> ligand = goupsOfTerms.get(ligandID);
	                for (int i=0; i<goupsOfTerms.size(); i++)
	                {
	                    if (i == ligandID)
	                        continue;
	
	                    Set<IAtom> grp = goupsOfTerms.get(i);
	
	                    for (IAtom centralAtm : grp)
	                    {
	                        for (IAtom ligandAtm : ligand)
	                        {
	                            logger.trace("Making a bond " 
	                                		+ MolecularUtils.getAtomRef(
	                                				ligandAtm,mol)
	                                		+ " - " 
	                                		+ MolecularUtils.getAtomRef(
	                                				centralAtm,mol));
	                            IBond bnd = new Bond(ligandAtm,centralAtm);
	                            mol.addBond(bnd);
	                        }
	                    }
	                }
	            }
            } // end if multihapto

            //Remove Du atom
            mol.removeAtom(du);

        } //end loop over Du
    }

//------------------------------------------------------------------------------
    
    /**
     * Explore connected systems in a list of atoms and returns all the atoms
     * that can be reached starting from the seed atom by moving only along
     * connections between atoms in the initial list
     * @param seed the atom from which to start
     * @param inList list of atoms to be considered
     * @param mol the molecular object containing all atoms in inList
     * @param doneFlag vector of boolean flags
     * @return the group of atoms reachable via bonds between atoms in inList
     */
    private Set<IAtom> exploreConnectedToAtom(IAtom seed, 
                                                     List<IAtom> inList, 
                                                     IAtomContainer mol, 
                                                     List<Boolean> doneFlag)
    {
        Set<IAtom> outSet = new HashSet<IAtom>();

        //Deal with the seed
        int idx = inList.indexOf(seed);
        doneFlag.set(idx,true);
        outSet.add(seed);

        //Look for other atoms reachable from here
        List<IAtom> connToSeed = mol.getConnectedAtomsList(seed);
        connToSeed.retainAll(inList);
        for (IAtom nbr : connToSeed)
        {
            int idx2 = inList.indexOf(nbr);
            if (!doneFlag.get(idx2))
            {
                Set<IAtom> recursiveOut = 
                            exploreConnectedToAtom(nbr,inList,mol,doneFlag);
                for (IAtom recNbr : recursiveOut)
                {
                    outSet.add(recNbr);
                }
            }
        }
        return outSet;
    }

//------------------------------------------------------------------------------
    
    /**
     * Generates a vector of boolean flags. The size of the vector equals the 
     * number of atoms in the <code>IAtomContainer<code/>. 
     * All flags are initialized to <code>false<code/>.
     * @param size of vector of flags has to be generated.
     * @return a vector of flags.
     */
    private List<Boolean> getFlagsVector(int size)
    {
        //create a vector with false entries
        int atoms = size;
        List<Boolean> flg = new ArrayList<Boolean>();
        for (int i = 0; i<atoms; i++)
                flg.add(false);

        return flg;
    }
    
//------------------------------------------------------------------------------    
}
