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

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.SDFIterator;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
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
    //TODO: document code
    
    //Files we work with
    private File outFile;
    private File refFile;

    //Default bond order
    private String defBO = "SINGLE";

    //Tolerance with respect to vdW radii
    private double tolerance = 0.10;
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
                        CHECKBONDLENGTHSTASK)));
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
            String ts = 
                    params.getParameter("TARGETELEMENT").getValueAsString();
            this.targetEl = ts;
        }
        
        if (params.contains("TEMPLATE"))
        {
            this.templatePathName = new File(
                    params.getParameter("TEMPLATE").getValueAsString());
        }
        
        if (params.contains("REFERENCE"))
        {
            String str = params.getParameter("REFERENCE").getValueAsString();
            this.refFile = new File(str);
            FileUtils.foundAndPermissions(str,true,false,false);
        }

        if (params.contains("OUTFILE"))
        {
             String str = params.getParameter("OUTFILE").getValueAsString();
            this.outFile = new File(str);
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
        if (task.equals(RICALCULATECONNECTIVITYTASK))
          {
            ricalculateConnectivity(iac, i);
        } else if (task.equals(ADDBONDSFORSINGLEELEMENTTASK)) {
            addBondsOnSingleElement(iac, i);
        } else if (task.equals(IMPOSECONNECTIONTABLETASK)) {
            imposeConnectionTable(iac, i);
        } else if (task.equals(CHECKBONDLENGTHSTASK)) {
            checkBondLengthsAgainstConnectivity(iac, i);
        } else {
              dealWithTaskMismatch();
        }
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
     */

    public void checkBondLengthsAgainstConnectivity(IAtomContainer iac, int i)
    {
        List<IAtomContainer> refMols = new ArrayList<IAtomContainer>();
        try 
        {
            refMols = IOtools.readMultiMolFiles(refFile);
        } catch (Throwable t) {
            Terminator.withMsgAndStatus("ERROR! Exception returned while "
                    + "reading " + refFile, -1);
        }
          
        //TODO: possibility of allowing one different reference for each entry

        if (refMols.size() > 1)
        {
            System.out.println("WARNING: multiple references found in '" 
                    + refFile + "'. We'll use only the first one.");
        }
        IAtomContainer ref = refMols.get(0); 
         
        boolean result = ConnectivityUtils.compareBondDistancesWithReference(
                        iac, ref, tolerance, verbosity);
        
        if (exposedOutputCollector != null)
        {
            String molID = "mol-"+i;
            exposeOutputData(new NamedData(molID,
                    NamedDataType.BOOLEAN, result));
        }
    }
      
//------------------------------------------------------------------------------

    /**
     * Add bonds on all the target elements of the given molecule according to
     * the relation between interatomic distance and sum of van der Waals radii.
     */

    public void addBondsOnSingleElement(IAtomContainer iac, int i)
    {   
        ConnectivityUtils.addConnectionsByVDWRadius(iac, 
                targetEl, 
                tolerance,
                tolerance2ndShell, 
                verbosity);

        if (outFile!=null)
            IOtools.writeSDFAppend(outFile, iac, true);
        
        if (exposedOutputCollector != null)
        {
            String molID = "mol-"+i;
            exposeOutputData(new NamedData(molID, 
                  NamedDataType.ATOMCONTAINERSET, iac));
        }
    }

//------------------------------------------------------------------------------

    /**
     * Recalculate molecular connectivity for all molecules in the SDF file.
     * This method works on all the atoms of the molecule (ignores any given 
     * target element symbol)
     */

    public void ricalculateConnectivity(IAtomContainer iac, int i)
    {
        ricalculateConnectivity(iac, tolerance); 
        
        if (outFile!=null)
            IOtools.writeSDFAppend(outFile, iac, true);
        
        if (exposedOutputCollector != null)
        {
            String molID = "mol-"+i;
            exposeOutputData(new NamedData(molID, 
                  NamedDataType.ATOMCONTAINERSET, iac));
        }
    }

//------------------------------------------------------------------------------

    /**
     * Recalculate connectivity 
     * @param mol the molecular object to modify
     * @param tolerance the tolerance (% as 0.0-1.0) in comparing vdW radii 
     * radii
     */

    private void ricalculateConnectivity(IAtomContainer mol, double tolerance)
    {
        String molName = MolecularUtils.getNameOrID(mol);
        int nAtms = mol.getAtomCount();
        if (verbosity > 1)
            System.out.println(" Recalculating connectivity for " + molName);

        for (int i=0; i<nAtms; i++)
        {
            IAtom a = mol.getAtom(i);
            List<IAtom> nbrs = mol.getConnectedAtomsList(a);
            for (int j=(i+1); j<nAtms; j++)
            {
                IAtom b = mol.getAtom(j);
                double ab = MolecularUtils.calculateInteratomicDistance(a, b);
                double minNBD = ConnectivityUtils.getMinNonBondedDistance(
                                                        a.getSymbol(),
                                                        b.getSymbol(),
                                                        tolerance);

                if ((ab < minNBD) && (!nbrs.contains(b)))
                {
                    if (verbosity > 2)
                    {
                        System.out.println(" Adding bond between atom '" 
                                                + MolecularUtils.getAtomRef(a,mol)
                                                + "' and '"
                                                + MolecularUtils.getAtomRef(b,mol)
                                                + "' (dist=" + ab + " - minNBD="
                                                + minNBD + "). ");
                    }

                    //TODO: add attempt to guess bond order

                    IBond newBnd = new Bond(a,b,IBond.Order.valueOf(defBO));
                    mol.addBond(newBnd);
                }
            }
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Uses the loaded parameters to impose a given connection table to the
     * molecules in the input.
     */
    
    private void imposeConnectionTable(IAtomContainer iac, int i)
    {
        List<IAtomContainer> tmpl = IOtools.readMultiMolFiles(templatePathName);
        
        //TODO: what to do when there is more than one template?

        ConnectivityUtils.importConnectivityFromReference(iac, tmpl.get(0));

        if (outFile!=null)
            IOtools.writeSDFAppend(outFile, iac, true);
        
        if (exposedOutputCollector != null)
        {
            String molID = "mol-"+i;
            exposeOutputData(new NamedData(molID, 
                  NamedDataType.IATOMCONTAINER, iac));
        }
    }

//------------------------------------------------------------------------------

}
