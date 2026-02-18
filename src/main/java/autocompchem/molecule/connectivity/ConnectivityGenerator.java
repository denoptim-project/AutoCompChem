package autocompchem.molecule.connectivity;

import java.io.File;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.atom.AtomUtils;
import autocompchem.datacollections.NamedData;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/** 
 * Tool generating molecular connectivity from Cartesian coordinates by
 * comparing the interatomic distances with the sum of van der Waals
 * radii.
 *          
 * @author Marco Foscato
 */


public class ConnectivityGenerator extends AtomContainerInputProcessor
{
    //Tolerance with respect to vdW radii
    private double tolerance = 0.33;
    private double tolerance2ndShell = 0.05;

    //Target elements on which bonds are to be added
    private String targetEl = "";
    
    private File templatePathName;
    
    /**
     * String defining the task of recalculating connectivity table
     */
    public static final String RICALCULATECONNECTIVITYTASKNAME = 
            "ricalculateConnectivity";

    /**
     * Task about recalculating connectivity table
     */
    public static final Task RICALCULATECONNECTIVITYTASK;
    static {
        RICALCULATECONNECTIVITYTASK = 
                Task.make(RICALCULATECONNECTIVITYTASKNAME);
    }
    
    /**
     * String defining the task of adding bonds on a specific element
     */
    public static final String ADDBONDSFORSINGLEELEMENTTASKNAME = 
            "addBondsForSingleElement";

    /**
     * Task about adding bonds on a specific element
     */
    public static final Task ADDBONDSFORSINGLEELEMENTTASK;
    static {
        ADDBONDSFORSINGLEELEMENTTASK = 
                Task.make(ADDBONDSFORSINGLEELEMENTTASKNAME);
    }
    
    /**
     * String defining the task of imposing a connectivity table to a set of 
     * atoms
     */
    public static final String IMPOSECONNECTIONTABLETASKNAME = 
            "imposeConnectionTable";

    /**
     * Task about imposing a connectivity table to a set of 
     * atoms
     */
    public static final Task IMPOSECONNECTIONTABLETASK;
    static {
        IMPOSECONNECTIONTABLETASK = Task.make(IMPOSECONNECTIONTABLETASKNAME);
    }
    
    /**
     * String defining the task of checking consistency between bond lengths 
     * and connectivity table
     */
    public static final String CHECKBONDLENGTHSTASKNAME = "checkBondLengths";

    /**
     * Task about checking consistency between bond lengths 
     * and connectivity table
     */
    public static final Task CHECKBONDLENGTHSTASK;
    static {
        CHECKBONDLENGTHSTASK = Task.make(CHECKBONDLENGTHSTASKNAME);
    }
    
    /**
     * String defining the task about producing the map of nearest neighbors
     */
    public static final String NEARESTNEIGHBORMAPTASKNAME = "makeNearestNeighborMap";

    /**
     * Task about about producing the map of nearest neighbors
     */
    public static final Task NEARESTNEIGHBORMAPTASK;
    static {
    	NEARESTNEIGHBORMAPTASK = Task.make(NEARESTNEIGHBORMAPTASKNAME);
    }

//------------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public ConnectivityGenerator()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
                Arrays.asList(RICALCULATECONNECTIVITYTASK,
                        ADDBONDSFORSINGLEELEMENTTASK,
                        IMPOSECONNECTIONTABLETASK, 
                        CHECKBONDLENGTHSTASK,
                        NEARESTNEIGHBORMAPTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/ConnectivityGenerator.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new ConnectivityGenerator();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
        super.initialize();
        
        //Define tolerance
        if (params.contains("TOLERANCE"))
        {
            String ts = params.getParameter("TOLERANCE").getValueAsString();
            this.tolerance = Double.parseDouble(ts);
        }

        //Define tolerance
        if (params.contains("TOLERANCE2NDSHELL"))
        {
            String ts = 
                params.getParameter("TOLERANCE2NDSHELL").getValueAsString();
            this.tolerance2ndShell = Double.parseDouble(ts);
        }

        //Define target element
        if (params.contains("TARGETELEMENT"))
        {
            String ts = params.getParameter("TARGETELEMENT").getValueAsString();
            task = ADDBONDSFORSINGLEELEMENTTASK;
            this.targetEl = ts;
        }
        
        if (params.contains("TEMPLATE"))
        {
            this.templatePathName = getNewFile(
                    params.getParameter("TEMPLATE").getValueAsString());
        }
        
        if (params.contains("REFERENCE"))
        {
            String str = params.getParameter("REFERENCE").getValueAsString();
            this.templatePathName = getNewFile(str);
            FileUtils.foundAndPermissions(str,true,false,false);
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
    public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i) 
    {
        if (task.equals(RICALCULATECONNECTIVITYTASK))
        {
        	ricalculateConnectivity(iac, i);
        } else if (task.equals(ADDBONDSFORSINGLEELEMENTTASK)) {
        	addBondsOnSingleElement(iac);
        } else if (task.equals(IMPOSECONNECTIONTABLETASK)) {
        	imposeConnectionTable(iac);
        } else if (task.equals(CHECKBONDLENGTHSTASK)) {
        	checkBondLengthsAgainstConnectivity(iac, i);
        } else if (task.equals(NEARESTNEIGHBORMAPTASK)) {
        	makeNearestNeighborMap(iac, i);
        } else {
        	dealWithTaskMismatch();
        }
        return iac;
    }

//------------------------------------------------------------------------------

    /**
     * Uses the connectivity of a given reference container to identify pairs 
     * of connected atoms and checks whether the interatomic distances (i.e.,
     * bond length) is consistent with that given in reference geometry. This
     * method is useful to evaluate potential changes of connectivity 
     * occurring after any molecular modeling that does not take into account 
     * connectivity in its input (e.g., any quantum mechanical driven 
     * molecular modeling engine).
     * @param iac the atom container to alter.
     * @param i the index used to identify the given atom container in log 
     * messages.
     */

    public void checkBondLengthsAgainstConnectivity(IAtomContainer iac, int i)
    {
        List<IAtomContainer> refMols = new ArrayList<IAtomContainer>();
        try 
        {
            refMols = IOtools.readMultiMolFiles(templatePathName);
        } catch (Throwable t) {
            throw new RuntimeException("Exception returned while "
                    + "reading " + templatePathName, t);
        }
          
        //TODO: possibility of allowing one different reference for each entry

        if (refMols.size() > 1)
        {
            logger.warn("WARNING: multiple references found in '" 
                    + templatePathName + "'. We'll use only the first one.");
        }
        IAtomContainer ref = refMols.get(0); 
         
        boolean result = ConnectivityUtils.compareBondDistancesWithReference(
                        iac, ref, tolerance, logger);
        
        if (exposedOutputCollector != null)
        {
            String molID = "mol-"+i;
            exposeOutputData(new NamedData(molID, result));
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Generates the map of nearest neighbor atoms according to the connectivity
     * defined in the given atom container. Does not alter or change the 
     * connectivity in the given atom container.
     * @param iac the atom container to alter.
     * @param i the index used to identify the given atom container in log 
     * messages.
     */
    public void makeNearestNeighborMap(IAtomContainer iac, int i)
    {
    	NearestNeighborMap nnm = new NearestNeighborMap(iac);
    	if (exposedOutputCollector != null)
        {
            String molID = NEARESTNEIGHBORMAPTASKNAME.toUpperCase() + "_mol-"+i;
            exposeOutputData(new NamedData(molID, nnm));
        }
    }
      
//------------------------------------------------------------------------------

    /**
     * Add bonds on all the target elements of the given molecule according to
     * the relation between interatomic distance and sum of Van der Waals radii.
     * @param iac the atom container to be edited.
     */

    public void addBondsOnSingleElement(IAtomContainer iac)
    {   
        addConnectionsByVDWRadius(iac, 
                targetEl, 
                tolerance,
                tolerance2ndShell);
    }
    
//------------------------------------------------------------------------------

    /**
     * Add connections between atoms if distance is below sum of vdW radii
     * Does not remove existing bonds.
     * @param mol the atom container under evaluation (may be modified)
     * @param el the symbol of the central atom around which interatomic 
     * distances are evaluated
     * @param tolerance a new bond is added only when the distance between the 
     * central atom and a neighbor is below the sum of their v.d.W. radii
     * minus the tolerance (as percentage). Values between 0.0 and 1.0 should 
     * be used. For atoms connected to atoms directly connected to the central
     * one the tolerance is increased by an extra factor.
     * @param extraTollSecShell additional tolerance for atoms connected to
     * atoms already directly connected to the central one
     */

    public void addConnectionsByVDWRadius(IAtomContainer mol, String el, 
                      double tolerance, double extraTollSecShell)
    {
        logger.trace("Evaluating Connections of '" 
                        + el + "' atoms using van der Waals radii (Tolerance: "
                        + (tolerance * 100) + "% (sec. shell +" 
                        + (extraTollSecShell * 100) + "%).");

        for (IAtom atmA : mol.atoms())
        {
            if (!atmA.getSymbol().equals(el))
                continue;

            for (IAtom atmB : mol.atoms())
            {
                if (atmB.equals(atmA))
                    continue;
                ricalculateConnectivity(atmA, mol, atmB);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Recalculate connectivity for the given pair of atoms belonging to 
     * the given container.
     */
    public void ricalculateConnectivity(IAtom atmA, IAtomContainer mol, IAtom atmB)
    {
    	if (atmB.equals(atmA))
            return;
    	if (!mol.contains(atmA) || !mol.contains(atmB))
    		return;

        //get van der Waals radius
        String sA = atmA.getSymbol();
        double rA = AtomUtils.getVdwRradius(sA);
        String sB = atmB.getSymbol();
        double rB = AtomUtils.getVdwRradius(sB);
        
        List<IAtom> nbrsA = mol.getConnectedAtomsList(atmA);
        if (nbrsA.contains(atmB))
        	return;
        
        List<IAtom> secShell = new ArrayList<IAtom>();
        for (IAtom nbr : nbrsA)
        {
            List<IAtom> nbrsOfNbr = mol.getConnectedAtomsList(nbr);
            for (IAtom nbrOfNbr : nbrsOfNbr)
            {
                if (nbrOfNbr.equals(atmA))
                    continue;
                secShell.add(nbrOfNbr);
            }
        }
        
        double dist = MolecularUtils.calculateInteratomicDistance(atmA,atmB);
        double refDist = rA + rB;

        if (secShell.contains(atmB))
        {
            refDist = refDist - (refDist * (tolerance + tolerance2ndShell));
        } else {
            refDist = refDist - (refDist * tolerance);
        }
        if (dist < refDist)
        {

            logger.trace("Adding bond between atom '" 
                                        + MolecularUtils.getAtomRef(atmA,mol)
                                        + "' and '"
                                        + MolecularUtils.getAtomRef(atmB,mol)
                                        + "' (dist=" + dist + " - refDist="
                                        + refDist + "). ");
            //TODO: guess bond order
            IBond b = new Bond(atmA, atmB, IBond.Order.valueOf("SINGLE"));
            mol.addBond(b);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Recalculate molecular connectivity for the given atom container.
     * This method works on all the atoms of the molecule (ignores any given 
     * target element symbol), but used the tolerance configured in this worker.
     * @param iac the atom container to alter.
     * @param i the index used to identify the given atom container in log 
     * messages.
     */

    public void ricalculateConnectivity(IAtomContainer iac, int i)
    {
        ricalculateConnectivity(iac, tolerance);
    }

//------------------------------------------------------------------------------

    /**
     * Recalculate connectivity 
     * @param mol the atom container to modify
     * @param tolerance the tolerance (% as 0.0-1.0) in comparing vdW radii 
     * radii
     */

    private void ricalculateConnectivity(IAtomContainer mol, double tolerance)
    {
        String molName = MolecularUtils.getNameOrID(mol);
        int nAtms = mol.getAtomCount();
        logger.debug("Recalculating connectivity for " + molName);

        for (int i=0; i<nAtms; i++)
        {
            IAtom a = mol.getAtom(i);
            for (int j=(i+1); j<nAtms; j++)
            {
                IAtom b = mol.getAtom(j);
            	ricalculateConnectivity(a, mol, b);
            }
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Uses the loaded parameters to impose a given connection table to the
     * molecules in the input.
     */
    
    private void imposeConnectionTable(IAtomContainer iac)
    {
        List<IAtomContainer> tmpl = IOtools.readMultiMolFiles(templatePathName);
        //TODO: what to do when there is more than one template?
        ConnectivityUtils.importConnectivityFromReference(iac, tmpl.get(0));
    }

//------------------------------------------------------------------------------

}
