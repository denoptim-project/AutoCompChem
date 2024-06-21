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

import org.apache.logging.log4j.Level;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
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

public class MolecularComparator extends AtomContainerInputProcessor
{
    /**
     * To atom container we conmpare to, i.e., our reference
     */
	private IAtomContainer referenceMol;
	
    //Filenames
    private File rotatedFile;

    //SMARTS query identifying target atoms
    private String targetAtoms;
    
    /**
     * String defining the task of comparing to atom containers
     */
    public static final String COMPARETWOMOLECULESTASKNAME = 
    		"compareTwoMolecules";

    /**
     * Task about comparing to atom containers
     */
    public static final Task COMPARETWOMOLECULESTASK;
    static {
    	COMPARETWOMOLECULESTASK = Task.make(COMPARETWOMOLECULESTASKNAME);
    }
    /**
     * String defining the task of comparing relative atom positions
     */
    public static final String COMPARETWOGEOMETRIESTASKNAME = 
    		"compareTwoGeometries";

    /**
     * Task about comparing relative atom positions
     */
    public static final Task COMPARETWOGEOMETRIESTASK;
    static {
    	COMPARETWOGEOMETRIESTASK = Task.make(COMPARETWOGEOMETRIESTASKNAME);
    }
    /**
     * String defining the task of comparing connectivity tables
     */
    public static final String COMPARETWOCONNECTIVITIESTASKNAME = 
    		"compareTwoConnectivities";

    /**
     * Task about comparing connectivity tables
     */
    public static final Task COMPARETWOCONNECTIVITIESTASK;
    static {
    	COMPARETWOCONNECTIVITIESTASK = Task.make(COMPARETWOCONNECTIVITIESTASKNAME);
    }
    
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
                Arrays.asList(COMPARETWOMOLECULESTASK,
                		COMPARETWOGEOMETRIESTASK,
                		COMPARETWOCONNECTIVITIESTASK)));
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
    	super.initialize();

        //Get and check the reference file
    	if (params.contains("REFERENCE"))
        {
	        File refFile = new File(
	        		params.getParameter("REFERENCE").getValueAsString());
	        FileUtils.foundAndPermissions(refFile,true,false,false);
	        List<IAtomContainer> lst = IOtools.readMultiMolFiles(refFile);
	        if (lst.size()>1)
            {
                logger.warn("WARNING: Found " + lst.size() 
                + " reference molecules, but we'll use only the first one.");
            }
	        referenceMol = lst.get(0);
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

    @Override
    public void performTask()
    {
    	processInput();
    }
    
//------------------------------------------------------------------------------

	@Override
	public void processOneAtomContainer(IAtomContainer iac, int i) 
	{
    	if (task.equals(COMPARETWOMOLECULESTASK))
    	{
    		runComparisonOfMoleculesBySuperposition(iac, i);
    	} else if (task.equals(COMPARETWOGEOMETRIESTASK)) {	
    		compareTwoGeometries(iac, i);
    	} else if (task.equals(COMPARETWOCONNECTIVITIESTASK)) {
    		compareTwoConnectivities(iac, i);
    	} else {
    		dealWithTaskMismatch();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Run comparison of two connectivity matrices as from the parameters given
     * in construction of this comparator
     */
//TODO-gg make private
    public void compareTwoConnectivities(IAtomContainer iac, int i)
    {
        ConnectivityUtils cu = new ConnectivityUtils();
        boolean consistentConnectivity = cu.compareWithReference(iac, 
        		referenceMol, logger);
        
        if (!consistentConnectivity)
        {
            logger.info("Inconsistent adjacency between molecules "
                                 + MolecularUtils.getNameOrID(iac)
                                 + " and "
                                 + MolecularUtils.getNameOrID(referenceMol));
        } else {
            logger.info("Consistent connectivity");
        }
        
        if (exposedOutputCollector != null)
        {
    	    String molID = "mol-"+i;
	        exposeOutputData(new NamedData(task.ID + molID,
	      		NamedDataType.BOOLEAN, consistentConnectivity));
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Run comparison of two geometries as from the parameters given in
     * construction of this comparator
     */

    public void compareTwoGeometries(IAtomContainer iac, int i)
    {
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
        SMARTSAllInOne.put("center", targetAtoms);

        //For First molecule
        logger.trace("Trying to identify the target atom in '"
                + MolecularUtils.getNameOrID(iac) + "'.");
        ManySMARTSQuery msq = new ManySMARTSQuery(iac, SMARTSAllInOne,0);
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
            logger.warn("WARNING! Problems in using SMARTS queries. " + cause
            		+ NL + "Matches: "+msq.getNumMatchesMap());
        } 
        if (msq.getTotalMatches() < 1) {
            Terminator.withMsgAndStatus("ERROR! Unable to find the central "
                + "atom '" + targetAtoms + "' in molecule " 
                + MolecularUtils.getNameOrID(iac) + ".", -1);
        } 
        else if (msq.getTotalMatches() > 1) 
        {
            Terminator.withMsgAndStatus("ERROR! More than one atom matches the "
                + "given query (" + targetAtoms + "). Unable to unambiguously "
                + "identify the central atom to be analysed",-1);
        }

        int centerID =  msq.getMatchingIdxsOfSMARTS("center").get(0).get(0);
        IAtom inAtm = iac.getAtom(centerID);

        //For second molecule
        logger.trace(" Trying to identify the target atom in '"
                + MolecularUtils.getNameOrID(referenceMol) + "'.");
        ManySMARTSQuery msqR = new ManySMARTSQuery(referenceMol, SMARTSAllInOne);
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
            logger.warn("WARNING! Problems in using SMARTS queries. " + cause
            		+ NL + "Matches: "+msqR.getNumMatchesMap());
        } 
        if (msqR.getTotalMatches() < 1) {
            Terminator.withMsgAndStatus("ERROR! Unable to find the central "
                + "atom '" + targetAtoms + "' in molecule "
                + MolecularUtils.getNameOrID(referenceMol) + ".", -1);
        } 
        else if (msqR.getTotalMatches() > 1) 
        {
            Terminator.withMsgAndStatus("ERROR! More than one atom matches the "
                + "given query (" + targetAtoms + "). Unable to unambiguously "
                + "identify the central atom to be analysed",-1);
        }
        int centerIDR = msqR.getMatchingIdxsOfSMARTS("center").get(0).get(0);
        IAtom refAtm = referenceMol.getAtom(centerIDR);

        double mad = compareTwoGeometries(inAtm, iac, refAtm, referenceMol);
        
        if (exposedOutputCollector != null)
        {
    	    String molID = "mol-"+i;
	        exposeOutputData(new NamedData(task.ID + molID, 
	      		NamedDataType.DOUBLE, mad));
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Run comparison of the geometries of two atoms in two molecules
     * @param atmA atom of which the geometry has to be compared with the 
     * reference
     * @param molA the first molecule
     * @param atmR atom in the reference molecule (reference geometry)
     * @param molR the reference molecule
     * @return the mean absolute deviation of the two atom geometries.
     */

    public double compareTwoGeometries(IAtom atmA, IAtomContainer molA, 
                                     IAtom atmR, IAtomContainer molR)
    {
        //Make geometries for the two atoms
        List<IAtom> lsA = molA.getConnectedAtomsList(atmA);
        List<IAtom> lsR = molR.getConnectedAtomsList(atmR);
        String msg = "Generating CoordinationGeometry 'gA'";
        for (int ia=0; ia<lsA.size(); ia++)
        {
        	msg = msg + " " + ia + " atom " 
            		+ MolecularUtils.getAtomRef(lsA.get(ia),molA) + NL;
        }
        msg = msg + "Generating CoordinationGeometry 'gB'" + NL;
        for (int ir=0; ir<lsR.size(); ir++)
        {
        	msg = msg +" " + ir + " atom " 
            		+ MolecularUtils.getAtomRef(lsR.get(ir),molR) + NL;
        }
        logger.debug(msg);
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
                                                                logger);

        //Build result string
        String summary = MolecularUtils.getAtomRef(atmA,molA) 
                        + "-" + MolecularUtils.getNameOrID(molA)
                        + " " + MolecularUtils.getAtomRef(atmR,molR)
                        + "-" + MolecularUtils.getNameOrID(molR) 
                        + " MAD= " + mad;

        //Compare both against reference geometries with same CN
        
        // TODO: check why we get null if CoordinationGeometryReferences is accessed as static
        CoordinationGeometryReferences cgRefs = 
        		new CoordinationGeometryReferences();
        List<CoordinationGeometry> allReference = 
        		cgRefs.getReferenceGeometryForCN(cnA);
        for (CoordinationGeometry gRef : allReference)
        {
            double madA =
                CoordinationGeometryUtils.calculateMeanAngleDifference(
                                                                gA,
                                                                gRef,
                                                                logger);
            double madR =
                CoordinationGeometryUtils.calculateMeanAngleDifference(
                                                                gR,
                                                                gRef,
                                                                logger);
            String nStd = gRef.getName();
            String report = String.format(Locale.ENGLISH," MAD_mol;" + nStd + " %1.2f", madA);
            report = report + String.format(Locale.ENGLISH," MAD_ref;" + nStd + " %1.2f",madR);
            double diff = (madA - madR);
            report = report + String.format(Locale.ENGLISH," D-MAD(" + nStd + ")= %1.2f",diff);
            summary = summary + report;
        }

        //In case of debug print the matrix of all-vs-all MAD for the standard gc
        if (logger.getLevel().isMoreSpecificThan(Level.DEBUG))
        {
            for (int i=2; i<7; i++)
            {
                List<CoordinationGeometry> a = cgRefs.getReferenceGeometryForCN(i);
                CoordinationGeometryUtils.printAllVsAllMAD(a, logger);
            }
        }
        logger.info("Comparison on two geometries: " + NL + summary);
        
        return mad;
    }

//------------------------------------------------------------------------------

    /**
     * Run comparator as from the parameters given in construction 
     * of the comparator
     */

    public void runComparisonOfMoleculesBySuperposition(IAtomContainer iac, int i)
    {
        ComparatorOfGeometries cog = new ComparatorOfGeometries();
        cog.compareGeometryBySuperposition(iac, referenceMol);

        if (exposedOutputCollector != null)
        {
    	    String molID = "mol-"+i;
	        exposeOutputData(new NamedData(task.ID + molID, 
	      		NamedDataType.DOUBLE, cog.getAlignementScore()));
    	}
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
        ComparatorOfGeometries cog = new ComparatorOfGeometries();
        return cog.getAtomMapping(molA,molB);
    }

//------------------------------------------------------------------------------

}
