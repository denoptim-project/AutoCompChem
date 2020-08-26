package autocompchem.molecule.connectivity;

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

import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.SDFIterator;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Terminator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/** 
 * Tool generating molecular connectivity from Cartesian coordinates by
 * comparing the interatomic distances with the sum of van der Waals
 * radii.
 * Parameters needed by methods of this class include:
 * <ul>
 * <li>
 * <b>INFILE</b>: path/name of the SDF file containing the structure
 * (only SDF files with ONE molecule are acceptable for the moment)
 * </li>
 * <li>
 * <b>TOLERANCE</b>: a new bond is added only when the distance between 
 *  two atoms is below the sum of their v.d.W. radii
 * minus the tolerance (as percentage). Values between 0.0 and 1.0 should 
 * be used.
 * </li>
 * <li>
 * <b>OUTFILE</b>: path or name of the SDF file where the result is to be written
 * </li>
 * <li>
 * <b>VERBOSITY</b>: verbosity level.
 * </li>
 * </ul>
 * Optional parameters used by some method are:
 * <ul>
 * <li>
 * <b>TOLERANCE2NDSHELL</b>: additional contribution to the 
 * tollerance
 * of atoms in 1-3 relation (i.e., when evaluating the distance r_AC 
 * in A-B-C) 
 * </li>
 * <li>
 * <b>TARGETELEMENT</b>: element symbol of atoms to which 
 * bonds are to be 
 * added
 * </li>
 * <li>
 * <b>TEMPLATE</b>: a complete definition of connectivity that is meant to be 
 * imposed to the molecule/s in the INFILE. When this is used, all the others
 * parameters are ignored (apart from INFILE and OUTFILE).
 * </li>
 * </ul>
 *          
 * @author Marco Foscato
 */


public class ConnectivityGenerator extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.RICALCULATECONNECTIVITY,
                    		TaskID.ADDBONDSFORSINGLEELEMENT,
                    		TaskID.IMPOSECONNECTIONTABLE)));
	
    //Filenames
    private String inFile;
    private String outFile;

    //Reporting flag
    private int verbosity = 0;

    //Recursion flag for reporting infos
    private int recNum = 1;

    //Default bond order
    private String defBO = "SINGLE";

    //Tolerance with respect to vdW radii
    private double tolerance = 0.10;
    private double tolerance2ndShell = 0.05;

    //Target elements on which bonds are to be added
    private String targetEl = "";
    
    private String templatePathName = "";
    private boolean useTemplate = false;

//------------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {

        //Define verbosity
        String vStr = params.getParameter("VERBOSITY").getValue().toString();
        this.verbosity = Integer.parseInt(vStr);

        if (verbosity > 0)
            System.out.println(" Adding parameters to ConnectivityGenerator");

        //Get and check the input file (which has to be an SDF file)
        this.inFile = params.getParameter("INFILE").getValue().toString();
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

        //Define tolerance
        if (params.contains("TOLERANCE"))
        {
            String ts = params.getParameter("TOLERANCE").getValue().toString();
            this.tolerance = Double.parseDouble(ts);
        }

        //Define tolerance
        if (params.contains("TOLERANCE2NDSHELL"))
        {
            String ts = 
                params.getParameter("TOLERANCE2NDSHELL").getValue().toString();
            this.tolerance2ndShell = Double.parseDouble(ts);
        }

        //Define target element
        if (params.contains("TARGETELEMENT"))
        {
            String ts = 
                    params.getParameter("TARGETELEMENT").getValue().toString();
            this.targetEl = ts;
        }
        
        if (params.contains("TEMPLATE"))
        {
            String templatePathName =
                    params.getParameter("TEMPLATE").getValue().toString();
            this.templatePathName = templatePathName;
            this.useTemplate = true;
        }


        //Get and check output file
        this.outFile = params.getParameter("OUTFILE").getValue().toString();
        FileUtils.mustNotExist(this.outFile);
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
            case RICALCULATECONNECTIVITY:
            	ricalculateConnectivity();
                break;
            case ADDBONDSFORSINGLEELEMENT:
            	addBondsOnSingleElement();
                break;
            case IMPOSECONNECTIONTABLE:
            	imposeConnectionTable();
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
     * Add bonds on all the target elements of all the molecules according to
     * the relation between interatomic distance and sum of van der Waals radii.
     */

    public void addBondsOnSingleElement()
    {
        if (verbosity > 1)
            System.out.println(" ConnectivityGenerator starts to work on "
                                + inFile);

        try {
            SDFIterator sdfItr = new SDFIterator(inFile);
            while (sdfItr.hasNext())
            {
                //Get the molecule
                IAtomContainer mol = sdfItr.next();

                //Recalculate connectivity of molecule
                ConnectivityUtils.addConnectionsByVDWRadius(mol, 
                                                            targetEl, 
                                                            tolerance,
                                                            tolerance2ndShell, 
                                                            verbosity);

                //Store output
                IOtools.writeSDFAppend(outFile,mol,true);
            }
            sdfItr.close();
        } catch (Throwable t) {
            Terminator.withMsgAndStatus("ERROR! Exception returned by "
                + "SDFIterator while reading " + inFile, -1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Recalculate molecular connectivity for all molecules in the SDF file.
     * This method works on all the atoms of the molecule (ignores any given 
     * target element symbol)
     */

    public void ricalculateConnectivity()
    {
        if (verbosity > 1)
            System.out.println(" ConnectivityGenerator starts to work on "  
                                + inFile);

        try {
            SDFIterator sdfItr = new SDFIterator(inFile);
            while (sdfItr.hasNext())
            {
                //Get the molecule
                IAtomContainer mol = sdfItr.next();

                //Recalculate connectivity of molecule
                ricalculateConnectivity(mol, tolerance);                

                //Store output
                IOtools.writeSDFAppend(outFile,mol,true);
            }
            sdfItr.close();
        } catch (Throwable t) {
            Terminator.withMsgAndStatus("ERROR! Exception returned by "
                + "SDFIterator while reading " + inFile, -1);
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
    
    private void imposeConnectionTable()
    {
        if (verbosity > 1)
            System.out.println(" Imposing connectivity on file " + inFile);

        ArrayList<IAtomContainer> tmpl = IOtools.readSDF(templatePathName);
        
        //TODO: what to do when there is more than one template?
        
        try {
            SDFIterator sdfItr = new SDFIterator(inFile);
            while (sdfItr.hasNext())
            {
                IAtomContainer mol = sdfItr.next();
                
                ConnectivityUtils.importConnectivityFromReference(mol, 
                		tmpl.get(0));

                IOtools.writeSDFAppend(outFile,mol,true);
            }
            sdfItr.close();
        } catch (Throwable t) {
            Terminator.withMsgAndStatus("ERROR! Exception returned while "
                + "iterating over SDF file to impose connection tables."
                + " I was reading file " + inFile, -1);
        }
    }

//------------------------------------------------------------------------------

}
