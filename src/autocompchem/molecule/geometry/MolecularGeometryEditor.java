package autocompchem.molecule.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point3d;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.periodictable.PeriodicTable;

import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileUtils;
import autocompchem.geometry.DistanceMatrix;
import autocompchem.io.IOtools;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixHandler;
import autocompchem.run.Terminator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerFactory;


/**
 * Tool for editing molecular geometries. 
 * 
 * @author Marco Foscato
 */


public class MolecularGeometryEditor extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.MODIFYGEOMETRY)));
    
    /**
     * Flag indicating the input is from file
     */
    private boolean inpFromFile = false;

    /**
     * Name of the molecular structure input file
     */
    private String inFile;

    /**
     * Molecular representation of the initial system
     */
    private IAtomContainer inMol;

    /**
     * Flag indicating the output is to be written to file
     */
    private boolean outToFile = false;

    /**
     * Name of the output file
     */
    private String outFile;

    /**
     * Molecular representation of the final systems
     */
    private ArrayList<IAtomContainer> outMols = new ArrayList<IAtomContainer>();

    /**
     * Pathname to SDF file with reference substructure
     */
    private String refFile;

    /**
     * Molecular representation of the reference substructure
     */
    private IAtomContainer refMol;

    /**
     * Cartesian Move
     */
    private ArrayList<Point3d> crtMove = new ArrayList<Point3d>();

    /**
     * ZMatrix Move
     */
    private ZMatrix zmtMove;

    /**
     * Scale factor for Move
     */
    private ArrayList<Double> scaleFactors = new ArrayList<Double>(
                                                            Arrays.asList(1.0));
    
    /**
     * Flag controlling optimisation of scaling factors
     */
    private boolean optimizeScalingFactors = false;
    
    /**
     * Kind of distribution for automatically generated scaling factors
     */
    private String scalingFactorDistribution = "BALANCED";
    
    /**
     * Number of automatically generated scaling factors 
     */
    private int numScalingFactors = 15;
    
    /**
     * Percentage of the possible negative path to consider 
     * (opt. scaling factors)
     */
    private double percOfNeg = 1.0;
    
    /**
     * Percentage of the possible positive path to consider 
     * (opt. scaling factors)
     */
    private double percOfPos = 1.0;
    
    /**
     * Default tolerance for interatomic distances (opt. scaling factors)
     */
    private static double defTolInteratmDist = 0.03;
    
    /**
     * Default tolerance w.r.t the sum covalent radii (opt. scaling factors)
     */
    private static double defTolCovRadSum = 0.05;
    
    /**
     * Default maximum overall displacement for one atom (opt. scaling factors)
     */
    private static double defMaxDispl = 5.0;
    
    /**
     * Default convergence criteria (opt. scaling factors)
     */
    private static double defConvCrit = 0.0001;
    
    /**
     * Default maximum step length (opt. scaling factors)
     */
    private static double defMaxStep = 1.0;
    
    /**
     * Flag reporting that all tasks are done
     */
    private boolean allDone = false;

    /**
     * Verbosity level
     */
    private int verbosity = 0;


//------------------------------------------------------------------------------

    //TODO move to class doc
    /**
     * Constructs a MolecularGeometryEditor specifying the parameters with a
     * {@link ParameterStorage}. 
     * <ul>
     * <li>
     * <b>INFILE</b> pathname of the SDF file containing the molecular
     * structure.
     * (only SDF files with ONE molecule are acceptable!).
     * </li>
     * <li>
     * <b>OUTFILE</b> pathname of the SDF file where results are to be
     * written.
     * </li>
     * <li>
     * <b>CARTESIANMOVE</b> (optional) pathname to a file defining 
     * the Cartesian components of the geometrical change. The file format
     * consists of a list of translation vectors in Cartesian coordinates, 
     * where each vector is reported as a space-separated  (X Y Z) tupla.
     * The Cartesian move is orientation-dependent. Unless a
     * reference substructure is also provided (see keyword 
     * REFERENCESUBSTRUCTURE), it is assumed that the 1-to-N vectors refer to
     * the 1-to-N atoms in the molecular system provided as input and that the
     * orientation of the molecule is consistent with that of the Cartesian 
     * move. When a reference substructure is provided (see parameter
     * REFERENCESUBSTRUCTURE), the orientation of the Cartesian move is assumed
     * to be consistent with that of the reference substructure.
     * </li>
     * <li>
     * <b>OPTIMIZESCALINGFACTORS</b> (optional) requests the optimisation of 
     * scaling factors to Cartesian mode and specifies i) the kind of distribution
     *  to be produced (one string - acceptable kinds are <code>EVEN</code> and 
     *  <code>BALANCED</code>), and ii) the number of scaling factors to generate
     *  (one integer), iii) the percent of the possible negative path to consider, 
     *  and iv) the percent of the possible positive path to consider.</li>
     * <li>
     * <b>CARTESIANSCALINGFACTORS</b> (optional) one or more scaling factors
     * (real numbers) to be applied to the Cartesian move. The Cartesian
     * move is applies on the initial structure per each given scaling factor.
     * The default is 1.0).
     * <li>
     * <b>REFERENCESUBSTRUCTUREFILE</b> (optional) pathname to SDF file with 
     * the definition of the substructure to which the geometrical change has 
     * to be applied. The atom list is used to assign the Cartesian moves to
     * the atoms of the molecular system given via the INFILE keyword.
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
        {
            System.out.println(
                              " Reading parameters in MolecularGeometryEditor");
        }

        //Get and check the input file (which has to be an SDF file)
        if (params.contains("INFILE"))
        {
            inpFromFile = true;
            this.inFile = params.getParameter("INFILE").getValue().toString();
            FileUtils.foundAndPermissions(this.inFile,true,false,false);
            ArrayList<IAtomContainer> inMols = IOtools.readSDF(inFile);
            if (inMols.size() != 1)
            {
                Terminator.withMsgAndStatus("ERROR! MoleculeGeometryEditor "
                        + "requires  SDF files with only one structure. "
                        + "Check file " + inFile, -1);
            }
            this.inMol = inMols.get(0);
        }

        //Get and check output file
        if (params.contains("OUTFILE"))
        {
            outToFile = true;
            this.outFile = params.getParameter("OUTFILE").getValue().toString();
            FileUtils.mustNotExist(this.outFile);
        }

        // Get the Cartesian move
        if (params.contains("CARTESIANMOVE"))
        {
            String crtFile = 
                     params.getParameter("CARTESIANMOVE").getValue().toString();
            FileUtils.foundAndPermissions(crtFile,true,false,false);
            ArrayList<String> all = IOtools.readTXT(crtFile);
            for (String line : all)
            {
                if (line.trim().length() == 0)
                {
                    continue;
                }
                Point3d v = new Point3d();
                try
                {
                    String[] partsB = line.trim().split("\\s+");
                    v = new Point3d(Double.parseDouble(partsB[0]),
                                    Double.parseDouble(partsB[1]),
                                    Double.parseDouble(partsB[2]));
                }
                catch (Throwable t)
                {
                    Terminator.withMsgAndStatus("ERROR! Cannot convert line '"
                        + line + "' into a 3-tupla of Cartesian coordinates. "
                        + "Check file '" + crtFile+ "'.",-1);
                }
                this.crtMove.add(v);
            }
        }

        // Get sequence of scaling factors
        if (params.contains("CARTESIANSCALINGFACTORS"))
        {
            String line = params.getParameter("CARTESIANSCALINGFACTORS")
                                                         .getValue().toString();
            scaleFactors.clear();
            String[] parts = line.trim().split("\\s+");
            for (int i=0; i<parts.length; i++)
            {
                double d = 1.0;
                try
                {
                    d = Double.parseDouble(parts[i]);
                }
                catch (Throwable t)
                {
                    Terminator.withMsgAndStatus("ERROR! Cannot understand scale "
                        + "factor '" + parts[i] + "'. Expecting double.",-1);
                }
                scaleFactors.add(d);
            }
        }
        
        // Guess scaling factors
        if (params.contains("OPTIMIZESCALINGFACTORS"))
        {
        	String line = params.getParameter("OPTIMIZESCALINGFACTORS")
                    .getValue().toString();
        	optimizeScalingFactors = true;
            scaleFactors.clear();
            String[] words = line.trim().split("\\s+");
            if (words.length != 4)
            {
            	Terminator.withMsgAndStatus("ERROR! Cannot understand value '"
                        + line + "'. Expecting one word and one integer.",-1);
            }
            String kind = words[0];
            if (!kind.toUpperCase().equals("EVEN") && 
            		!kind.toUpperCase().equals("BALANCED"))
            {
            	Terminator.withMsgAndStatus("ERROR! Cannot understand '" + kind
                        + "' as a scaling factor distribution kind.",-1);
            }
            scalingFactorDistribution = kind;
            String num = words[1];
            try {
            	numScalingFactors = Integer.parseInt(num);
            } catch (Throwable t) {
            	Terminator.withMsgAndStatus("ERROR! Cannot understand '" + num
                        + "' as the number of scaling factor to generate. "
                        + "Expecting an integer.",-1);
            }
            num = words[2];
            try {
            	percOfNeg = Double.parseDouble(num);
            } catch (Throwable t) {
            	Terminator.withMsgAndStatus("ERROR! Cannot understand '" + num
                        + "' as the percentage of the possible negative path. "
                        + "Expecting a double.",-1);
            }
            num = words[3];
            try {
            	percOfPos = Double.parseDouble(num);
            } catch (Throwable t) {
            	Terminator.withMsgAndStatus("ERROR! Cannot understand '" + num
                        + "' as the percentage of the possible positive path. "
                        + "Expecting a double.",-1);
            } 
        }

        // Get the Cartesian move
        if (params.contains("ZMATRIXMOVE"))
        {
            String zmtFile =
                       params.getParameter("ZMATRIXMOVE").getValue().toString();
            FileUtils.foundAndPermissions(zmtFile,true,false,false);
            if (IOtools.readZMatrixFile(zmtFile).size() != 1)
            {
                Terminator.withMsgAndStatus("ERROR! Found multiple ZMatrices "
                                             + "in file '" + zmtFile + "'.",-1);
            }
            this.zmtMove = IOtools.readZMatrixFile(zmtFile).get(0);
        }

        // Get the reference substructure
        if (params.contains("REFERENCESUBSTRUCTUREFILE"))
        {
            this.refFile = params.getParameter("REFERENCESUBSTRUCTUREFILE")
                                                         .getValue().toString();
            FileUtils.foundAndPermissions(this.refFile,true,false,false);
            ArrayList<IAtomContainer> refMols = IOtools.readSDF(this.refFile);
            if (refMols.size() != 1)
            {
                Terminator.withMsgAndStatus("ERROR! MoleculeGeometryEditor "
                        + "requires SDF files with one reference structure. "
                        + "Check file " + refFile, -1);
            }
            this.refMol = refMols.get(0);
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
          case MODIFYGEOMETRY:
        	  applyMove();
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
     * Apply the geometrical modification as defined by the constructor.
     */

    public void applyMove()
    {
        if (zmtMove!=null && zmtMove.getZAtomCount()>0)
        {
                if (verbosity > 0)
                {
                    System.out.println(" Applying ZMatrix move");
                }
                try
                {
                    applyZMatrixMove();
                }
                catch (Throwable t)
                {
                    Terminator.withMsgAndStatus("ERROR! Exception while "
                        + "altering the ZMatrix. You might need dummy "
                        + "atoms to define an healthier ZMatrix. Cause of the "
                        + "exception: " + t.getMessage(), -1);
                }
        }
        else if (crtMove!=null && crtMove.size()>0)
        {
            if (verbosity > 0)
                {
                    System.out.println(" Applying Cartesian move");
                }
                applyCartesianMove();
        }
        else
            {
            Terminator.withMsgAndStatus("ERROR! Choose and provide either "
                    + "a Cartesian or a ZMatrix move.", -1);
        }
    }
   

//------------------------------------------------------------------------------

    /**
     * Apply the geometrical modification as defined by the constructor.
     */

    public void applyCartesianMove()
    {
        // Consistency check
        if (inMol == null || inMol.getAtomCount() == 0)
        {
            Terminator.withMsgAndStatus("ERROR! No input molecule defined. "
            + "Please, try using 'INFILE' to import a molecule from file.",-1);
        }
        if (crtMove == null || crtMove.size() == 0)
        {
            Terminator.withMsgAndStatus("ERROR! No Cartesian move defined. "
                + "Please, try using 'CARTESIANMOVE' to provide a Cartesian "
                + "move.",-1);
        }

        // Identify the actual Cartesian move, possibly using reference 
        // substructure to identify the atom to be moved
        ArrayList<Point3d> actualMove = new ArrayList<Point3d>();
        IAtomContainer actualMol = inMol;
        for (int i=0; i<actualMol.getAtomCount(); i++)
        {
            actualMove.add(new Point3d());
        }
        if (this.refMol != null && this.refMol.getAtomCount() > 0)
        {
            if (verbosity > 0)
            {
                System.out.println(" Matching reference sub-structure");
            }
            if (this.refMol.getAtomCount() != crtMove.size())
            {
                Terminator.withMsgAndStatus("ERROR! Reference structure (size "
                     + this.refMol.getAtomCount() + ") and Cartesian step ("
                     + "size " + crtMove.size() + ") have different size. ",-1);
            }
            ComparatorOfGeometries mc = new ComparatorOfGeometries(verbosity-1);
            Map<Integer,Integer> refToInAtmMap = mc.getAtomMapping(actualMol,
                                                                        refMol);
            for (Integer refAtId : refToInAtmMap.keySet())
            {
                actualMove.set(refToInAtmMap.get(refAtId),crtMove.get(refAtId));
            }

            //Get the rototranslate actualMol as to make it consistent with the
            // reference's Cartesian move
            actualMol = mc.getFirstMolAligned();
        }
        else
        {
            for (int i=0; i<crtMove.size(); i++)
            {
                actualMove.set(i,crtMove.get(i));
            }
        }
        
        if (optimizeScalingFactors)
        {
        	scaleFactors = optimizeScalingFactors(actualMol,actualMove,
        			numScalingFactors,
        			scalingFactorDistribution,
        			percOfNeg,
        			percOfPos,
        			defTolInteratmDist, 
        			defTolCovRadSum,
        			defMaxDispl, 
        			defConvCrit, 
        			defMaxStep,
        			verbosity-1);
        }
        
        if (verbosity > 0)
        {
            System.out.println(" Actual Cartesian move: ");
            for (int i=0; i<actualMove.size(); i++)
            {
                Point3d pt = actualMove.get(i);
                System.out.println("  " + i + ": " + pt);
            }
            System.out.println(" Scaling factors: " + scaleFactors);
        }

        // Produce the transformed geometry/ies
        for (int j=0; j<scaleFactors.size(); j++)
        {
            if (verbosity > 0)
            {
                System.out.println(" Generating results for scaling " 
                                            + " factor " + scaleFactors.get(j));
            }

            // Here we finally generate one new geometry
            IAtomContainer outMol = getGeomFromCartesianMove(actualMol,
            		actualMove,scaleFactors.get(j));
            String outMolName = MolecularUtils.getNameOrID(inMol);
            outMolName = outMolName + " - moved by " + scaleFactors.get(j) 
                                                        + "x(Cartesian_move)";
            outMol.setProperty(CDKConstants.TITLE,outMolName);
            outMols.add(outMol); 
        }
        
        if (!outToFile && verbosity > 0)
        {
            System.out.println(" ");
            System.out.println(" WARNING! No output file produced (use OUTFILE "
                + " to write the results into a new SDF file");
        }
        
        if (outToFile)
        {
            IOtools.writeSDFAppend(outFile,outMols,false);
        }
        
        allDone = true;
    }
    
//-------------------------------------------------------------------------------
    
    /**
     * This is the actual method generating new geometries by applying the 
     * Cartesian move.
     * @param mol the molecule to modify
     * @param actualMove The Cartesian Move
     * @param sf the scaling factor that multiplied the Cartesian move
     * @return the modified geometry
     */
    public static IAtomContainer getGeomFromCartesianMove(IAtomContainer mol, 
    		ArrayList<Point3d> actualMove, double sf)
    {
        IAtomContainer outMol = new AtomContainer();
        try
        {
            outMol = (IAtomContainer) mol.clone();
        }
        catch (Throwable t)
        {
            Terminator.withMsgAndStatus("ERROR! Cannot clone molecule.",-1);
        }
        for (int i=0; i<outMol.getAtomCount(); i++)
        {
            Point3d newPt3d = new Point3d(outMol.getAtom(i).getPoint3d());
            Point3d newMv = new Point3d(actualMove.get(i));
            newMv.scale(sf);
            newPt3d.add(newMv);
            outMol.getAtom(i).setPoint3d(newPt3d);
        }
        return outMol;
    }

  //-------------------------------------------------------------------------------
  	/**
  	 * Searches for optimal scaling factors that do not generate atom clashes or 
  	 * exploded systems. Uses default values for all settings of the search 
  	 * algorithm.
  	 * @param mol the atom container with the unmodified geometry
  	 * @param move the Cartesian move
  	 * @param numSfSteps divide the optimised range of scaling factors in this 
  	 * number
  	 * @return the optimised list of scaling factors
  	 */
  	public static ArrayList<Double> optimizeScalingFactors(IAtomContainer mol, 
      		ArrayList<Point3d> move, int numSfSteps) 
  	{
  		return optimizeScalingFactors(mol,move,numSfSteps,"EVEN",
  				defTolInteratmDist, defTolCovRadSum,
  				defMaxDispl, defConvCrit, defMaxStep,0);
  	}
    
//-------------------------------------------------------------------------------
  	/**
  	 * Searches for optimal scaling factors that do not generate atom clashes or 
  	 * exploded systems. Uses default values for all settings of the search 
  	 * algorithm.
  	 * @param mol the atom container with the unmodified geometry
  	 * @param move the Cartesian move
  	 * @param numSfSteps divide the optimised range of scaling factors in this 
  	 * number  
  	 * @param distributionKind specifies how to spear scaling factors between
  	 * the optimised extremes. Possible values are:
  	 * <ul>
  	 * <li><code>EVEN</code>: 
  	 * to spread factors evenly at <code>(range)/#steps</code>.</li>
  	 * <li><code>BALANCED</code>: 
  	 * to put half of the points on each side of the 0.0 scaling.</li>
  	 * </ul>
  	 * @return the optimised list of scaling factors
  	 */
  	public static ArrayList<Double> optimizeScalingFactors(IAtomContainer mol, 
      		ArrayList<Point3d> move, int numSfSteps, String distributionKind) 
  	{
  		return optimizeScalingFactors(mol,move,numSfSteps,distributionKind,
  				defTolInteratmDist, defTolCovRadSum, defMaxDispl, defConvCrit, 
  				defMaxStep, 0);
  	}
  	
//-------------------------------------------------------------------------------

  	/**
  	 * Searches for optimal scaling factors that do not generate atom clashes or 
  	 * exploded systems.
  	 * @param mol the atom container with the unmodified geometry
  	 * @param move the Cartesian move
  	 * @param numSfSteps divide the optimised range of scaling factors in this 
  	 * number  
  	 * of steps
  	 * @param distributionKind specifies how to spear scaling factors between
  	 * the optimised extremes. Possible values are:
  	 * <ul>
  	 * <li><code>EVEN</code>: 
  	 * to spread factors evenly at <code>(range)/#steps</code>.</li>
  	 * <li><code>BALANCED</code>: 
  	 * to put half of the points on each side of the 0.0 scaling.</li>
  	 * </ul>
  	 * @param percentualNeg the amount of the possible path to consider 
  	 * as valid (for negative scaling factors)
  	 * @param percentualPos the amount of the possible path to consider 
  	 * as valid (for positive scaling factors)
  	 * @param verbosity amount of log to stdout
  	 * @return the optimised list of scaling factors
  	 */
	public static ArrayList<Double> optimizeScalingFactors(IAtomContainer mol, 
    		ArrayList<Point3d> move, int numSfSteps, String distributionKind,
    		double percentualNeg, double percentualPos, int verbosity) 
	{
  		return optimizeScalingFactors(mol,move,numSfSteps,distributionKind,
  				percentualNeg, percentualPos,
  				defTolInteratmDist, defTolCovRadSum, defMaxDispl, defConvCrit, 
  				defMaxStep, 0);
  	}

//-------------------------------------------------------------------------------

  	/**
  	 * Searches for optimal scaling factors that do not generate atom clashes or 
  	 * exploded systems.
  	 * @param mol the atom container with the unmodified geometry
  	 * @param move the Cartesian move
  	 * @param numSfSteps divide the optimised range of scaling factors in this 
  	 * number  
  	 * of steps
  	 * @param distributionKind specifies how to spear scaling factors between
  	 * the optimised extremes. Possible values are:
  	 * <ul>
  	 * <li><code>EVEN</code>: 
  	 * to spread factors evenly at <code>(range)/#steps</code>.</li>
  	 * <li><code>BALANCED</code>: 
  	 * to put half of the points on each side of the 0.0 scaling.</li>
  	 * </ul>
  	 * @param verbosity amount of log to stdout
  	 * @return the optimised list of scaling factors
  	 */
	public static ArrayList<Double> optimizeScalingFactors(IAtomContainer mol, 
    		ArrayList<Point3d> move, int numSfSteps, String distributionKind,
    		 int verbosity) 
	{
  		return optimizeScalingFactors(mol,move,numSfSteps,distributionKind,
  				1.0, 1.0,
  				defTolInteratmDist, defTolCovRadSum, defMaxDispl, defConvCrit, 
  				defMaxStep, 0);
  	}

//-------------------------------------------------------------------------------

  	/**
  	 * Searches for optimal scaling factors that do not generate atom clashes or 
  	 * exploded systems.
  	 * @param mol the atom container with the unmodified geometry
  	 * @param move the Cartesian move
  	 * @param numSfSteps divide the optimised range of scaling factors in this 
  	 * number  
  	 * of steps
  	 * @param distributionKind specifies how to spear scaling factors between
  	 * the optimised extremes. Possible values are:
  	 * <ul>
  	 * <li><code>EVEN</code>: 
  	 * to spread factors evenly at <code>(range)/#steps</code>.</li>
  	 * <li><code>BALANCED</code>: 
  	 * to put half of the points on each side of the 0.0 scaling.</li>
  	 * </ul>
  	 * @param tolInteractDist permits down to this % of the initial 
  	 * interatomic distance
  	 * @param tolCovRadSum permits down to this % of the sum of covalent 
  	 * radii
  	 * @param stretchLimit permits up to this displacement for a single atom.
  	 * @param convergence stop search when step falls below this value
  	 * @param maxStep maximum allowed step
  	 * @param verbosity amount of log to stdout
  	 * @return the optimised list of scaling factors
  	 */
	public static ArrayList<Double> optimizeScalingFactors(IAtomContainer mol, 
    		ArrayList<Point3d> move, int numSfSteps, String distributionKind,
    		double tolInteractDist, double tolCovRadSum,
    		double stretchLimit, double convergence, double maxStep, 
    		int verbosity) 
	{
		return optimizeScalingFactors(mol,move,numSfSteps,distributionKind,
  				1.0, 1.0, tolInteractDist, tolCovRadSum, stretchLimit,
  				convergence, maxStep,verbosity);
	}
	
//-------------------------------------------------------------------------------

  	/**
  	 * Searches for optimal scaling factors that do not generate atom clashes or 
  	 * exploded systems.
  	 * @param mol the atom container with the unmodified geometry
  	 * @param move the Cartesian move
  	 * @param numSfSteps divide the optimised range of scaling factors in this 
  	 * number  
  	 * of steps
  	 * @param distributionKind specifies how to spear scaling factors between
  	 * the optimised extremes. Possible values are:
  	 * <ul>
  	 * <li><code>EVEN</code>: 
  	 * to spread factors evenly at <code>(range)/#steps</code>.</li>
  	 * <li><code>BALANCED</code>: 
  	 * to put half of the points on each side of the 0.0 scaling.</li>
  	 * </ul>
  	 * @param percentualNeg the amount of the possible path to consider 
  	 * as valid (for negative scaling factors)
  	 * @param percentualPos the amount of the possible path to consider 
  	 * as valid (for positive scaling factors)
  	 * @param tolInteractDist permits down to this % of the initial 
  	 * interatomic distance
  	 * @param tolCovRadSum permits down to this % of the sum of covalent 
  	 * radii
  	 * @param stretchLimit permits up to this displacement for a single atom.
  	 * @param convergence stop search when step falls below this value
  	 * @param maxStep maximum allowed step
  	 * @param verbosity amount of log to stdout
  	 * @return the optimised list of scaling factors
  	 */
	public static ArrayList<Double> optimizeScalingFactors(IAtomContainer mol, 
    		ArrayList<Point3d> move, int numSfSteps, String distributionKind,
    		double percentualNeg, double percentualPos,
    		double tolInteractDist, double tolCovRadSum,
    		double stretchLimit, double convergence, double maxStep, 
    		int verbosity) 
	{
		if (verbosity > 0)
        {
		    System.out.println(" Optimizing Scaleing Factors");
        }
		
		//Run optimisation twice:
		//either maximise (+1) or minimise (-1) scaling factor.
		double[] signs = new double[] {-1.0,1.0};
		//Initialise the results
		double[] optimalExtremes = new double[] {0.0,0.0};
		for (int ig=0; ig<signs.length; ig++)
		{
			double sign = signs[ig];
			// sign of the step in the |scaling factor| scale
			double direction = +1.0;
			// the initial step length
			double step = 1.0;       
			// initial guess for |scaling factor| (sign is given by 'sign')
			double guessSF = 1.0;
			double oldGuessSF = guessSF;
			// the non-OK scaling factor closest to divide
			double smallestToNotOK = Double.MAX_VALUE;
			// the OK scaling factor closest to divide
			double largestToOK = 0.0; //NB: we always have >0
			// whether the current scaling factor produced an OK geometry
			boolean isOK = true;      
			// whether the previous scaling factor produced an OK geometry
			boolean lastWasOK = true;  
			boolean foundFirstNonOK = false;
			String reason = ""; // for logging
			double lowestDist = Double.MAX_VALUE;
			
			if (verbosity > 0)
            {
			    System.out.println("   Sign  MinDist    MaxDispl     Guess"
			    		+ "      Dir     LastStep  ok/old smallest!OK  "
			    		+ "largestOK Notes");
            }
			
			// Get interatorim distances in initial geometry (for later)
			DistanceMatrix initialDM = 
					MolecularUtils.getInteratormicDistanceMatrix(mol);
			
			boolean goon = true;
			while (goon)
			{	
				// keep trace of old result
				lastWasOK = isOK;
				
				lowestDist = Double.MAX_VALUE;
				if (Math.abs(step) < Math.abs(convergence))
				{
					if (verbosity > 0)
		            {
					    System.out.println("CONVERGED!!! |"+step
							+"| < |"+convergence+"|");
		            }
					goon = false;
					break;
				}
				
				// Evaluate the use of the the new guess scaling factor:
				// A) largest atomic movement has reached the given limit,
				// B) compare interatomic distance against the max between  
				// B.1) the sum of the covalent radii, or
				// B.2) the distance in the initial geometry.
				
				//A) largest atomic movement
				double maxDisplacement = -Double.MAX_VALUE;
				for (Point3d p : move)
				{
		            Point3d newP = new Point3d(p);
		            newP.scale(guessSF*sign);
		            double displ = newP.distance(new Point3d());
		            if (displ > maxDisplacement)
		            {
		            	maxDisplacement = displ;
		            }
				}
				if (maxDisplacement > stretchLimit)
				{
					isOK = false;
					goon = false;
					if (verbosity > 0)
		            {
					    System.out.println(String.format(
					    		"Attempt to use scaling factor %-11.3e stopped by "
					    		+ "max displacement: %-11.3e (>%-11.3e)",
					    		guessSF,maxDisplacement,stretchLimit));
		            }
					break;
				}
				
				// B) shortest interatomic distance against sum of cov.radii
				isOK = true;
				
				// Generate the geometry
				IAtomContainer outMol = getGeomFromCartesianMove(mol,
	            		move,guessSF*sign);
				
				// Measure interatomic distances
				loopOnAtoms:
				for (int i=0; i<outMol.getAtomCount(); i++)
			    {
			    	IAtom atmI = outMol.getAtom(i);
			    	double covRadI = PeriodicTable.getCovalentRadius(
			    			atmI.getSymbol());
			    	for (int jj=i+1; jj<outMol.getAtomCount(); jj++)
			        {
			        	IAtom atmJ = outMol.getAtom(jj);
			            double covRadJ = PeriodicTable.getCovalentRadius(
			            		atmJ.getSymbol());
			        	double dist = 
			        			MolecularUtils.calculateInteratomicDistance(
			        					atmI, atmJ);
			        	
			        	double tollInitDist = initialDM.get(i, jj)*(1.0-tolInteractDist);
			        	double tollCovRadii = (covRadI+covRadJ)*(1.0-tolCovRadSum);
			        	double minInteratDist;
			        	String kk = "";
			        	if (tollInitDist < tollCovRadii)
			        	{
			        		minInteratDist = tollInitDist;
			        		kk = "initDist";
			        	}
			        	else
			        	{
			        		minInteratDist = tollCovRadii;
					        kk = "covRad";
			        	}
			            
			            if (dist < minInteratDist)
			            {
				        	lowestDist = dist;
				        	reason = MolecularUtils.getAtomRef(atmI, outMol) 
				        	+ ":" + MolecularUtils.getAtomRef(atmJ, outMol)
				            + " " + covRadI + " " + covRadJ + " " + dist + "(<"
				        	+ minInteratDist + " " + kk + ")";
			            	   
			            	// Here we found a pair of atoms that are moved 
			            	// too close to each other.
			            	// The guess scaling factor (guessSF) is "not OK"
			            	// and we keep track of it.
				            isOK = false;
				            // We record the smallest |scaling factor| 
				            // that led to a non-OK geometry
				            if (guessSF < smallestToNotOK)
				            {
				            	smallestToNotOK = guessSF;
				            }
				            foundFirstNonOK = true;
				            break loopOnAtoms;
			            }
			        }
			    }
				
            	// Keep the last good scaling factor
            	if (isOK)
            	{
    				oldGuessSF = guessSF;
            	    
            		// Keep the largest |scaling factor| that led to an OK geom
            		if(guessSF > largestToOK)
	                {
	            	    largestToOK = guessSF;
	                }
	            }
            	
            	// Finished with this step
				if (verbosity > 0)
	            {
				    String msg = String.format("%+7.1f %-11.3e %-11.3e %-11.3e "
				    		+ "%-7.1f %-11.3e %1.1B(%1.1B) %-11.3e  %-11.3e %s",
						sign,lowestDist,maxDisplacement,guessSF,direction,step,
						isOK,lastWasOK,smallestToNotOK,largestToOK,reason);
				    System.out.println(msg);
	            }
				
				// Prepare next step
				if (!foundFirstNonOK)
				{
					// first we search for one extreme that is not OK, to set 
					// an upper limit to the search
					guessSF = guessSF + step; // this is only |scaling factor|
				}
				else
				{
					// Once we have an upper and lower limits we can do dichotomic search
					
					// Step length is given by half of the unmapped range 
					// between the two known points closest to divide
					// NB: smallestToNotOK > largestToOK Thus 'step' > 0
					step = Math.min(maxStep, (smallestToNotOK - largestToOK)*0.5);
					
					// The direction in which we take the step changes every 
					// time we cross the divide
					if (isOK && !lastWasOK)
					{
						direction = (-1.0)*direction;
					}
					else
					{
						if (!isOK && lastWasOK)
							direction = (-1.0)*direction;
					}
					guessSF = guessSF + step*direction;
				}
			}
			optimalExtremes[ig] = oldGuessSF*sign;
	    }
		
		// Apply the percentage of neg/pos possible path
		double newNegExtreme = optimalExtremes[0]*percentualNeg;
		double newPosExtreme = optimalExtremes[1]*percentualPos;
		optimalExtremes[0] = newNegExtreme;
		optimalExtremes[1] = newPosExtreme;
		
		// Finally generate distribution of scaling factors
		ArrayList<Double> chosenScalingFactors = new ArrayList<Double>();
		switch (distributionKind.toUpperCase())
		{
		    case "EVEN":
				double sfStep = (optimalExtremes[1]-optimalExtremes[0])/numSfSteps;
				for (int i=0; i<(numSfSteps+1); i++)
				{
					chosenScalingFactors.add(optimalExtremes[0]+i*sfStep);	
				}
				break;
				
		    case "BALANCED":
		    	int halfPoints = numSfSteps/2;
		    	double sfStepMinus = (0.0-optimalExtremes[0])/halfPoints;
		    	double sfStepPlus = (optimalExtremes[1]-0.0)/halfPoints;
		    	for (int i=halfPoints; i>0; i--)
				{
					chosenScalingFactors.add(-i*sfStepMinus);	
				}
		    	for (int i=0; i<(halfPoints+1); i++)
				{
					chosenScalingFactors.add(0.0+i*sfStepPlus);	
				}
				break;
				
			//TODO: add GAUSSIAN distribution, 
			// or other with continuous change in step length
		
		}
		
		if (verbosity > 0)
        {
            System.out.println("Optimized extremes: " 
            		+ optimalExtremes[0] + " " + optimalExtremes[1]);
            System.out.println("Optimized Scaling factors: " 
            		+ chosenScalingFactors);
        }
		return chosenScalingFactors;
	}

//------------------------------------------------------------------------------

    /**
     * Apply the geometric modification as defined by the constructor.
     * @throws Throwable when there are issues converting the ZMatrix and it 
     * is preferable to redefine the ZMatrix, possibly adding dummy atoms
     */

    public void applyZMatrixMove() throws Throwable
    {
        // Consistency check
        if (inMol == null || inMol.getAtomCount() == 0)
        {
            Terminator.withMsgAndStatus("ERROR! No input molecule defined. "
            + "Please, try using 'INFILE' to import a molecule from file.",-1);
        }
        if (zmtMove == null || zmtMove.getZAtomCount() == 0)
        {
            Terminator.withMsgAndStatus("ERROR! No ZMatrix move defined. "
                + "Please, try using 'ZMATRIXMOVE' to provide a geometrical "
                + "modification as changes of internal coordinates.",-1);
        }

        // Get the ZMatrix of the molecule to work with
        ParameterStorage locPar = params.clone();
        locPar.setParameter(new Parameter("TASK",NamedDataType.STRING,
        		"PRINTZMATRIX"));
        Worker w = WorkerFactory.createWorker(locPar);
        ZMatrixHandler zmh = (ZMatrixHandler) w;
        ZMatrix inZMatMol = zmh.makeZMatrix();
        if (verbosity > 1)
        {
            System.out.println(" Original ZMatrix: ");
            ArrayList<String> txt = inZMatMol.toLinesOfText(false,false);
            for (int i=0; i<txt.size(); i++)
            {
                System.out.println("  Line-" + i + ": " + txt.get(i));
            }
        }

        // Identify the actual move in terms of ZMatrix
        ZMatrix actualZMatMove = new ZMatrix();
        if (inMol.getAtomCount() != zmtMove.getZAtomCount())
        {
            // TODO
            if (verbosity > -1)
            {
                System.out.println(" TODO: what if only some IC is modified?");
                Terminator.withMsgAndStatus("ERROR! Still in development",-1);
            }
        }
        else
        {
            actualZMatMove = new ZMatrix(zmtMove.toLinesOfText(false,false));
        }
        if (verbosity > 0)
        {
            System.out.println(" Actual ZMatrixMove: ");
            ArrayList<String> txt = actualZMatMove.toLinesOfText(false,false);
            for (int i=0; i<txt.size(); i++)
            {
                System.out.println("  Line-" + i + ": " + txt.get(i));
            }
            System.out.println(" Scaling factors: " + scaleFactors);
        }

        // Produce the transformed geometry/ies
        for (int j=0; j<scaleFactors.size(); j++)
        {
            if (verbosity > 0)
            {
                System.out.println(" Generating results for scaling " 
                                            + " factor " + scaleFactors.get(j));
            }

            // Apply ZMatrixMove
            ZMatrix modZMat = zmh.modifyZMatrix(inZMatMol,actualZMatMove,
                                                           scaleFactors.get(j));

            // Convert to Cartesian coordinates
            IAtomContainer outMol = zmh.convertZMatrixToIAC(modZMat,inMol);

            // Prepare and print output to file
            String outMolName = MolecularUtils.getNameOrID(inMol);
            outMolName = outMolName + " - moved by " + scaleFactors.get(j) 
                                                             + "x(ZMatrixMove)";
            outMol.setProperty(CDKConstants.TITLE,outMolName);
            outMols.add(outMol);
            if (outToFile)
            {
                IOtools.writeSDFAppend(outFile,outMol,true);
            }
        }
        
        if (!outToFile && verbosity > 0)
        {
            System.out.println(" ");
            System.out.println(" WARNING! No output file produced (use OUTFILE "
                + " to write the results into a new SDF file");
        }
        allDone = true;
    }
 
//------------------------------------------------------------------------------

}
