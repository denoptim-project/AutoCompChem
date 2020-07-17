package autocompchem.molecule.sorting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FilesManager;
import autocompchem.io.IOtools;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Terminator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * MolecularSorter is a tool to sort molecules.
 * Parameters needed by the MolecularSorter:
 * TODO
 * <ul>
 * <li> 
 * <b>INFILE</b> path or name of the SDF file containing the structures
 * Multiple structures can be provided (better if mols have different 
 * name/title)
 * </li>
 * <li>
 * <b>OUTFILE</b> name of SDF output file
 * </li>
 * <li>
 * <b>SDFPROPERTY</b> name of the SDF property to be used to sort
 * </li>
 * <li>
 * <b>VERBOSITY</b>  verbosity level
 * </li>
 * </ul>
 * 
 * @author Marco Foscato
 */


public class MolecularSorter extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.SORTSDFMOLECULES)));
    
    //Filenames
    private String inFile;
    private String outFile;

    //Property
    private String prop;

    //Verbosity level
    private int verbosity = 1;

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
            System.out.println(" Adding parameters to MolecularSorter");


        //Get and check the input file (which has to be an SDF file)
        this.inFile = params.getParameter("INFILE").getValue().toString();
        FilesManager.foundAndPermissions(this.inFile,true,false,false);

        //Get and check the output file name 
        this.outFile = params.getParameter("OUTFILE").getValue().toString();
        FilesManager.mustNotExist(this.outFile);

        //Get SDF Property
        this.prop = params.getParameter("SDFPROPERTY").getValue().toString();

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
          case SORTSDFMOLECULES:
        	  writeSortedSDF(); //TODO; change keyword from distinguishing case where we wabt to write results or just calculate nad share output date
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
     * Writes the sorted list into the output file
     */

    public void writeSortedSDF()
    {
        //Get input
        ArrayList<IAtomContainer> mols = IOtools.readSDF(inFile);
        ArrayList<SortableMolecule> smols = new ArrayList<SortableMolecule>();
        for (IAtomContainer mol : mols)
        {
            if (!MolecularUtils.hasProperty(mol,prop))
            {
                String err = "Molecule " + MolecularUtils.getNameOrID(mol)
                             + " has no field named '" + prop + "'.";
                Terminator.withMsgAndStatus("ERROR! " + err, -1);
            }

            SortableMolecule sm = new SortableMolecule(mol,mol.getProperty(
                                                                         prop));
            smols.add(sm);
        }
        mols.clear();

        //Sort
        Collections.sort(smols, new SortableMoleculeComparator());

        //write output
        for (int i=0; i<smols.size(); i++)
        {
            IOtools.writeSDFAppend(outFile,smols.get(i).getIAtomContainer(),
                                                                          true);
        }
    }

//-----------------------------------------------------------------------------

}
