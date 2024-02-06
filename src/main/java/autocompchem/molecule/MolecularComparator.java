package autocompchem.molecule;

import java.io.File;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.molecule.coordinationgeometry.CoordinationGeometry;
import autocompchem.molecule.coordinationgeometry.CoordinationGeometryReferences;
import autocompchem.molecule.coordinationgeometry.CoordinationGeometryUtils;
import autocompchem.molecule.geometry.ComparatorOfGeometries;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;


/**
 * Compare molecular structures.
 * 
 * @author Marco Foscato
 */

public class MolecularComparator extends Worker
{
    
    //Filenames
    private File inFile;
    private File refFile;
    private File rotatedFile;
    private File outFile;

    //SMARTS query identifying target atoms
    private String targetAtoms;

    //Verbosity level
    private int verbosity = 1;
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public MolecularComparator()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
                Arrays.asList(Task.make("compareTwoMolecules"),
                		Task.make("compareTwoGeometries"),
                		Task.make("compareTwoConnectivities"))));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/MolecularComparator.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new MolecularComparator();
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
            System.out.println(" Adding parameters to MolecularComparator");

        //Get and check the input file (which has to be an SDF file)
        this.inFile = new File(
        		params.getParameter("INFILE").getValueAsString());
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

        //Get and check the reference file (which has to be an SDF file)
        this.refFile = new File(
        		params.getParameter("REFERENCE").getValueAsString());
        FileUtils.foundAndPermissions(this.refFile,true,false,false);

        //Get and check output file
        if (params.contains("OUTFILE"))
        {
            this.outFile =  new File(
            		params.getParameter("OUTFILE").getValueAsString());
            FileUtils.mustNotExist(this.outFile);
        }

        //Get and check optional file for rotated output
        if (params.contains("ROTATEDOUT")) 
        {
            this.rotatedFile = new File(
                        params.getParameter("ROTATEDOUT").getValueAsString());
            FileUtils.mustNotExist(this.rotatedFile);
        }

        //Get the SMARTS query identifying target atoms
        if (params.contains("TARGETATOMSQUERY")) 
        {
            this.targetAtoms =
                  params.getParameter("TARGETATOMSQUERY").getValue().toString();
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
          case "COMPARETWOMOLECULES":
        	  runComparisonOfMoleculesBySuperposition();
              break;
          case "COMPARETWOGEOMETRIES":
        	  compareTwoGeometries();
              break;
          case "COMPARETWOCONNECTIVITIES":
        	  compareTwoConnectivities();
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
     * Run comparison of two connectivity matrices as from the parameters given
     * in construction of this comparator
     */

    public void compareTwoConnectivities()
    {
        //Get the molecules
        List<IAtomContainer> inMols = IOtools.readSDF(inFile);
        if (inMols.size() != 1)
        {
            Terminator.withMsgAndStatus("ERROR! MoleculeComparator requires "
                + "SDF files with only one structure. Check file "
                + inFile ,-1);
        }
        IAtomContainer inMol = inMols.get(0);

        List<IAtomContainer> refMols = IOtools.readSDF(refFile);
        if (refMols.size() != 1)
        {
            Terminator.withMsgAndStatus("ERROR! MoleculeComparator requires "
                + "SDF files with only one structure. Check file "
                + refFile ,-1);
        }
        IAtomContainer refMol = refMols.get(0);

        ConnectivityUtils cu = new ConnectivityUtils();
        boolean consistentConnectivity = cu.compareWithReference(inMol,refMol);
        if (!consistentConnectivity)
        {
            if (verbosity > 0)
            {
                System.out.println(" Inconsistent adjacency between molecules "
                                 + MolecularUtils.getNameOrID(inMol)
                                 + " and "
                                 + MolecularUtils.getNameOrID(refMol));
            }
        } else {
            if (verbosity > 0)
            {
                System.out.println(" Consistent connectivity");
                if (!outFile.equals("") && outFile != null)
                {
                    IOtools.writeSDFAppend(outFile,inMol,false);
                }
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Run comparison of two geometries as from the parameters given in
     * construction of this comparator
     */

    public void compareTwoGeometries()
    {
        //Get the molecules
        List<IAtomContainer> inMols = IOtools.readSDF(inFile);
        if (inMols.size() != 1)
        {
            Terminator.withMsgAndStatus("ERROR! MoleculeComparator requires "
                + "SDF files with only one structure. Check file "
                + inFile ,-1);
        }
        IAtomContainer inMol = inMols.get(0);

        List<IAtomContainer> refMols = IOtools.readSDF(refFile);
        if (refMols.size() != 1)
        {
            Terminator.withMsgAndStatus("ERROR! MoleculeComparator requires "
                + "SDF files with only one structure. Check file "
                + refFile ,-1);
        }
        IAtomContainer refMol = refMols.get(0);

        //Get the atoms
        if (targetAtoms.equals("") || targetAtoms == null)
        {
            Terminator.withMsgAndStatus("ERROR! MoleculeComparator requires "
                + "a SMARTS query to identify the target atom of which the "
                + "geometry is to be compared with a reference structure. "
                + "It seems that the SMARTS was not define. Please provide "
                + "a SMARTS string by means of the keyword:value pair "
                + "TARGETATOMSQUERY: <SMARTS_of_the_central_atom>.", -1);
        }

        //Get the atoms of which geometry have to be compared
        Map<String,String> SMARTSAllInOne = new HashMap<String,String>();
        SMARTSAllInOne.put("center",targetAtoms);

        //For First molecule
        if (verbosity > 2)
        {    
            System.out.println(" Trying to identify the target atom in '"
                + MolecularUtils.getNameOrID(inMol) + "'.");
        }    
        ManySMARTSQuery msq = new ManySMARTSQuery(inMol,SMARTSAllInOne,verbosity);
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
        if (msq.getTotalMatches() < 1) {
            Terminator.withMsgAndStatus("ERROR! Unable to find the central "
                + "atom '" + targetAtoms + "' in molecule " 
                + MolecularUtils.getNameOrID(inMol) + ".", -1);
        } 
        else if (msq.getTotalMatches() > 1) 
        {
            Terminator.withMsgAndStatus("ERROR! More than one atom matches the "
                + "given query (" + targetAtoms + "). Unable to unambiguously "
                + "identify the central atom to be analysed",-1);
        }

        int centerID =  msq.getMatchingIdxsOfSMARTS("center").get(0).get(0);
        IAtom inAtm = inMol.getAtom(centerID);

        //For second molecule
        if (verbosity > 2)
        {
            System.out.println(" Trying to identify the target atom in '"
                + MolecularUtils.getNameOrID(refMol) + "'.");
        }
        ManySMARTSQuery msqR = new ManySMARTSQuery(refMol,SMARTSAllInOne,verbosity);
        if (msqR.hasProblems())
        {
            String cause = msqR.getMessage();
            if (cause.contains("The AtomType") &&
                                        cause.contains("could not be found"))
            {           
                Terminator.withMsgAndStatus("ERROR! " + cause 
                        + " To solve the problem try to move this "
                        + "element to \"Du\" an try again.",-1);
            }   
            System.err.println("\nWARNING! Problems in using SMARTS queries. " 
                                + cause);
            System.out.println("Matches: "+msqR.getNumMatchesMap());
        } 
        if (msqR.getTotalMatches() < 1) {
            Terminator.withMsgAndStatus("ERROR! Unable to find the central "
                + "atom '" + targetAtoms + "' in molecule "
                + MolecularUtils.getNameOrID(refMol) + ".", -1);
        } 
        else if (msqR.getTotalMatches() > 1) 
        {
            Terminator.withMsgAndStatus("ERROR! More than one atom matches the "
                + "given query (" + targetAtoms + "). Unable to unambiguously "
                + "identify the central atom to be analysed",-1);
        }
        int centerIDR = msqR.getMatchingIdxsOfSMARTS("center").get(0).get(0);
        IAtom refAtm = refMol.getAtom(centerIDR);

        //RunComparison
        compareTwoGeometries(inAtm, inMol, refAtm, refMol);
    }

//------------------------------------------------------------------------------

    /**
     * Run comparison of the geometries of two atoms in two molecules
     * @param atmA atom of which the geometry has to be compared with the 
     * reference
     * @param molA the first molecule
     * @param atmR atom in the reference molecule (reference geometry)
     * @param molR the reference molecule
     */

    public void compareTwoGeometries(IAtom atmA, IAtomContainer molA, 
                                     IAtom atmR, IAtomContainer molR)
    {
        //Make geometries for the two atoms
        List<IAtom> lsA = molA.getConnectedAtomsList(atmA);
        List<IAtom> lsR = molR.getConnectedAtomsList(atmR);
        if (verbosity > 1)
        {
            System.out.println("Generating CoordinationGeometry 'gA'");
            for (int ia=0; ia<lsA.size(); ia++)
            {
                System.out.println(" " + ia + " atom " + MolecularUtils.getAtomRef(lsA.get(ia),molA));
            }
            System.out.println("Generating CoordinationGeometry 'gB'");
            for (int ir=0; ir<lsR.size(); ir++)
            {
                System.out.println(" " + ir + " atom " + MolecularUtils.getAtomRef(lsR.get(ir),molR));
            }        
        }        
        CoordinationGeometry gA = new CoordinationGeometry("gA", atmA, 
                                              molA.getConnectedAtomsList(atmA));
        CoordinationGeometry gR = new CoordinationGeometry("gR", atmR, 
                                              molR.getConnectedAtomsList(atmR));

        //Compare CN
        int cnA = gA.getConnectionNumber();
        int cnR = gR.getConnectionNumber();

        if (cnA != cnR)
        {
            Terminator.withMsgAndStatus("ERROR! The geometries of target atom '"
                        + MolecularUtils.getAtomRef(atmA,molA) 
                        + "' in molecule "
                        + MolecularUtils.getNameOrID(molA)
                        + " and atom '"
                        + MolecularUtils.getAtomRef(atmR,molR)
                        + "' in molecule "
                        + MolecularUtils.getNameOrID(molR)
                        + " have different CN (" + cnA + ", " + cnR + ").",-1);
        }


        //Compare the two geometries
        double mad = CoordinationGeometryUtils.calculateMeanAngleDifference(
                                                                gA,
                                                                gR,
                                                                verbosity);

        //Build result string
        String summary = MolecularUtils.getAtomRef(atmA,molA) 
                        + "-" + MolecularUtils.getNameOrID(molA)
                        + " " + MolecularUtils.getAtomRef(atmR,molR)
                        + "-" + MolecularUtils.getNameOrID(molR) 
                        + " MAD= " + mad;

        //Compare both against reference geometries with same CN
        
        // TODO: check why we get null if CoordinationGeometryReferences is accessed as static
        CoordinationGeometryReferences cgRefs = new
                                               CoordinationGeometryReferences();
        List<CoordinationGeometry> allReference = 
                                          cgRefs.getReferenceGeometryForCN(cnA);
        for (CoordinationGeometry gRef : allReference)
        {
            double madA =
                CoordinationGeometryUtils.calculateMeanAngleDifference(
                                                                gA,
                                                                gRef,
                                                                0);
            double madR =
                CoordinationGeometryUtils.calculateMeanAngleDifference(
                                                                gR,
                                                                gRef,
                                                                0);
            String nStd = gRef.getName();
            String report = String.format(Locale.ENGLISH," MAD_mol;" + nStd + " %1.2f", madA);
            report = report + String.format(Locale.ENGLISH," MAD_ref;" + nStd + " %1.2f",madR);
            double diff = (madA - madR);
            report = report + String.format(Locale.ENGLISH," D-MAD(" + nStd + ")= %1.2f",diff);
//            System.out.println("->" + gRef.getName() + ": MadA = " + madA
//                                + " MadR = " +madR);
//            System.out.println("D-MAD("+ gRef.getName() + ") = " + (madA-madR));
            
//   summary = summary + " D-MAD("+ gRef.getName() + ")= " + (madA-madR);
            summary = summary + report;
        }

        //In case of debug print the matrix of all-vs-all MAD for the standard gc
        if (verbosity > 2)
        {
            for (int i=2; i<7; i++)
            {
                List<CoordinationGeometry> a = cgRefs.getReferenceGeometryForCN(i);
                CoordinationGeometryUtils.printAllVsAllMAD(a);
            }
        }

        //Report results
        System.out.println(" ");
        System.out.println(" Comparison on two geometries: ");
        System.out.println(summary);
        if (!outFile.equals("") && outFile != null)
        {
            IOtools.writeTXTAppend(outFile,summary,false);
            System.out.println(" Results reported also in file " + outFile);
        }
        
    }

//------------------------------------------------------------------------------

    /**
     * Run comparator as from the parameters given in construction 
     * of the comparator
     */

    public void runComparisonOfMoleculesBySuperposition()
    {
        List<IAtomContainer> inMols = IOtools.readSDF(inFile);
        if (inMols.size() != 1)
        {
            Terminator.withMsgAndStatus("ERROR! MoleculeComparator requires "
                + "SDF files with only one structure. Check file "
                + inFile ,-1);
        }
        IAtomContainer inMol = inMols.get(0);

        List<IAtomContainer> refMols = IOtools.readSDF(refFile);
        if (refMols.size() != 1)
        {
            Terminator.withMsgAndStatus("ERROR! MoleculeComparator requires "
                + "SDF files with only one structure. Check file "
                + refFile ,-1);
        }
        IAtomContainer refMol = refMols.get(0);

        ComparatorOfGeometries cog = new ComparatorOfGeometries(verbosity);
        cog.compareGeometryBySuperposition(inMol,refMol);

//TODO: print ourput?

    }

//------------------------------------------------------------------------------

    /**
     * Calculates and return the best atom mapping between two structures:
     * a map of which atom in the first structure corresponds to an atom in the
     * second structure. The result is reported with 0-based indexes in the atom
     * array of each structure, so that
     * the keys are the indexes of atoms in the second (reference)
     * structure while the values are the indexes in the first structure.
     * @param molA the first molecule
     * @param molB the second molecule
     * @return the atom map where keys are atom indexes in the second molecule
     * and values are atom indexes in the first molecule
     */
    public Map<Integer,Integer> getGeometryAwareAtomMapping(IAtomContainer molA,
                                                            IAtomContainer molB)
    {
        ComparatorOfGeometries cog = new ComparatorOfGeometries(verbosity);
        return cog.getAtomMapping(molA,molB);
    }

//------------------------------------------------------------------------------

}
