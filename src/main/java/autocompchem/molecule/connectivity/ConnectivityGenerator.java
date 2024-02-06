package autocompchem.molecule.connectivity;

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
import java.io.File;

import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.io.SDFIterator;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.Task;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/** 
 * Tool generating molecular connectivity from Cartesian coordinates by
 * comparing the interatomic distances with the sum of van der Waals
 * radii.
 *          
 * @author Marco Foscato
 */


public class ConnectivityGenerator extends Worker
{
	
    //Files we work with
    private File inFile;
    private File outFile;
    private File refFile;

    //Reporting flag
    private int verbosity = 0;

    //Default bond order
    private String defBO = "SINGLE";

    //Tolerance with respect to vdW radii
    private double tolerance = 0.10;
    private double tolerance2ndShell = 0.05;

    //Target elements on which bonds are to be added
    private String targetEl = "";
    
    private File templatePathName;

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
                Arrays.asList(Task.make("ricalculateConnectivity"),
                		Task.make("addBondsForSingleElement"),
                		Task.make("imposeConnectionTable"),
                		Task.make("checkBondLengths"))));
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

        //Define verbosity
        String vStr = params.getParameter("VERBOSITY").getValue().toString();
        this.verbosity = Integer.parseInt(vStr);

        if (verbosity > 0)
            System.out.println(" Adding parameters to ConnectivityGenerator");

        //Get and check the input file (which has to be an SDF file)
        String pathname = params.getParameter("INFILE").getValueAsString();
        this.inFile = new File(pathname);
        FileUtils.foundAndPermissions(pathname,true,false,false);

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

      @SuppressWarnings("incomplete-switch")
      @Override
      public void performTask()
      {
          switch (task.ID)
            {
            case "RICALCULATECONNECTIVITY":
            	ricalculateConnectivity();
                break;
            case "ADDBONDSFORSINGLEELEMENT":
            	addBondsOnSingleElement();
                break;
            case "IMPOSECONNECTIONTABLE":
            	imposeConnectionTable();
                break;
            case "CHECKBONDLENGTHS":
            	checkBondLengthsAgainstConnectivity();
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
       * Uses the connectivity of a given reference container to identify pairs 
       * of connected atoms and checks whether the interatomic distances (i.e.,
       * bond length) is consistent with that given in reference geometry. This
       * method is useful to evaluate potential changes of connectivity 
       * occurring after any molecular modeling that does not take into account 
       * connectivity in its input (e.g., any quantum mechanical driven 
       * molecular modeling engine).
       */

      public void checkBondLengthsAgainstConnectivity()
      {
          List<IAtomContainer> refMols = new ArrayList<IAtomContainer>();
          try 
          { 
        	  refMols = IOtools.readMultiMolFiles(refFile);
          } catch (Throwable t) {
              Terminator.withMsgAndStatus("ERROR! Exception returned by "
                      + "SDFIterator while reading " + refFile, -1);
          }
          
          //TODO: possibility of allowing one different reference for each entry

          if (refMols.size() > 1)
          {
        	  System.out.println("WARNING: multiple references found in '" 
        			  + refFile + "'. We'll use only the first one.");
          }
          IAtomContainer ref = refMols.get(0); 
          
          try {
              SDFIterator sdfItr = new SDFIterator(inFile);
              int i = 0;
              while (sdfItr.hasNext())
              {
            	  i++;
            	  if (verbosity > 0)
            		  System.out.println("Checking bond lengths in mol #"+i);
            	  
                  IAtomContainer mol = sdfItr.next();
                  ConnectivityUtils.compareBondDistancesWithReference(mol, 
                		  ref, tolerance, verbosity);
              }
              sdfItr.close();
          } catch (Throwable t) {
              Terminator.withMsgAndStatus("ERROR! Exception returned by "
                  + "SDFIterator while reading " + inFile, -1);
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
        {
            System.out.println(" ConnectivityGenerator starts to work on "
                                + inFile);
        }
        
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

        List<IAtomContainer> tmpl = IOtools.readSDF(templatePathName);
        
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
