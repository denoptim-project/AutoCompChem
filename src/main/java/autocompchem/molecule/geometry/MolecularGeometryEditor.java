package autocompchem.molecule.geometry;

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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point3d;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.tools.periodictable.PeriodicTable;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileUtils;
import autocompchem.geometry.DistanceMatrix;
import autocompchem.io.IOtools;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixAtom;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixHandler;
import autocompchem.smarts.SMARTS;
import autocompchem.smarts.SMARTSUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;


/**
 * Tool for editing molecular geometries along direction in multidimensional 
 * space.
 *
 * @author Marco Foscato
 */


public class MolecularGeometryEditor extends AtomContainerInputProcessor
{
    /**
     * The SDF file with reference substructure
     */
    private File refFile;

    /**
     * Molecular representation of the reference substructure
     */
    private IAtomContainer refMol;

    /**
     * Cartesian Move
     */
    private List<Point3d> crtMove = new ArrayList<Point3d>();

    /**
     * ZMatrix Move
     */
    private ZMatrix zmtMove;

	/**
	 * SMARTS bond to stretch
	 */
	private SMARTS smartsBondToStretch;

    /**
     * Scale factor for Move
     */
    private List<Double> scaleFactors = new ArrayList<Double>(
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
     * Default maximum overall displacement for one atom (opt. scaling factors)
     */
    private static double defMaxDispl = 5.0;
    
    /**
     * Maximum overall displacement of a single atom
     */
    private double maxDispl = defMaxDispl;
    
    /**
     * Default tolerance for interatomic distances (opt. scaling factors)
     */
    private static double defTolInteratmDist = 0.03;
    
    /**
     * Default tolerance w.r.t the sum covalent radii (opt. scaling factors)
     */
    private static double defTolCovRadSum = 0.05;
    
    /**
     * Default convergence criteria (opt. scaling factors)
     */
    private static double defConvCrit = 0.0001;
    
    /**
     * Default maximum step length (opt. scaling factors)
     */
    private static double defMaxStep = 1.0;
    
    private final String NL =System.getProperty("line.separator"); 
    
    /**
     * String defining the task of altering a geometry
     */
    public static final String MODIFYGEOMETRYTASKNAME = "modifyGeometry";
    
    /**
     * String defining the task of altering stretching a bond
     */
    public static final String STRETCHBONDTASKNAME = "stretchBond";

    /**
     * Task about altering a geometry
     */
    public static final Task MODIFYGEOMETRYTASK;
    static {
    	MODIFYGEOMETRYTASK = Task.make(MODIFYGEOMETRYTASKNAME);
    }

    /**
     * Task about altering stretching a bond
     */
    public static final Task STRETCHBONDTASK;
    static {
    	STRETCHBONDTASK = Task.make(STRETCHBONDTASKNAME);
    }

//-----------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public MolecularGeometryEditor()
    {}

//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
                Arrays.asList(MODIFYGEOMETRYTASK, STRETCHBONDTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/MolecularGeometryEditor.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new MolecularGeometryEditor();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
    	super.initialize();

        // Get the Cartesian move
        if (params.contains("CARTESIANMOVE"))
        {
            File crtFile = getNewFile(
                     params.getParameter("CARTESIANMOVE").getValueAsString());
            FileUtils.foundAndPermissions(crtFile,true,false,false);
            List<String> all = IOtools.readTXT(crtFile);
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
        if (params.contains("SCALINGFACTORS"))
        {
            String line = params.getParameter("SCALINGFACTORS")
                                                         .getValueAsString();
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
                    .getValueAsString();
        	optimizeScalingFactors = true;
            scaleFactors.clear();
            String[] words = line.trim().split("\\s+");
            if (words.length != 4 && words.length != 5)
            {
            	Terminator.withMsgAndStatus("ERROR! Cannot understand value '"
                        + line + "'. Expecting either of these syntaxes: " + NL  
                        + "  <word> <integer> <real> <real> " + NL
                        + "  <word> <integer> <real> <real> <real>.",-1);
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
            if (words.length >= 5)
            {
	            num = words[4];
	            try {
	            	maxDispl = Double.parseDouble(num);
	            } catch (Throwable t) {
	            	Terminator.withMsgAndStatus("ERROR! Cannot understand '" 
	                        + num + "' as the maximum displacement for a "
	                        + "single atom. Expecting a double.",-1);
	            }
            }
        }

        // Get the Cartesian move
        if (params.contains("ZMATRIXMOVE"))
        {
            File zmtFile = getNewFile(
                       params.getParameter("ZMATRIXMOVE").getValueAsString());
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
            this.refFile =  getNewFile(
            		params.getParameter("REFERENCESUBSTRUCTUREFILE")
            		.getValueAsString());
            FileUtils.foundAndPermissions(this.refFile,true,false,false);
            List<IAtomContainer> refMols = IOtools.readSDF(this.refFile);
            if (refMols.size() != 1)
            {
                Terminator.withMsgAndStatus("ERROR! MoleculeGeometryEditor "
                        + "requires SDF files with one reference structure. "
                        + "Check file " + refFile, -1);
            }
            this.refMol = refMols.get(0);
        }

		// Get the bond to stretch
		if (params.contains("SMARTSBONDTOSTRETCH"))
		{
			this.smartsBondToStretch = new SMARTS(params.getParameter(
				"SMARTSBONDTOSTRETCH").getValueAsString());
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
		AtomContainerSet result = null;
    	if (task.equals(MODIFYGEOMETRYTASK))
    	{
    		if (zmtMove!=null && zmtMove.getZAtomCount()>0)
            {
                logger.info("Applying ZMatrix move");
                try
                {
                	result = applyZMatrixMove(iac);
                }
                catch (Throwable t)
                {
                    Terminator.withMsgAndStatus("ERROR! Exception while "
                        + "altering the ZMatrix. You might need dummy "
                        + "atoms to define an healthy ZMatrix. Cause of the "
                        + "exception: " + t.getMessage(), -1);
                }
            }
            else if (crtMove!=null && crtMove.size()>0)
            {
                logger.info("Applying Cartesian move");
                result = applyCartesianMove(iac);
            } else {
                Terminator.withMsgAndStatus("ERROR! Choose and provide either "
                        + "a Cartesian or a ZMatrix move.", -1);
            }
    		
    		if (exposedOutputCollector != null)
            {
	    	    String molID = "mol-"+i;
		        exposeOutputData(new NamedData(task.ID + molID, 
		        		result));
        	}
    		
    		tryWritingToOutfile(result);
    		outFileAlreadyUsed = true;
    	} else if (task.equals(STRETCHBONDTASK)) {
    		result = stretchBond(iac);
    		
    		if (exposedOutputCollector != null)
            {
	    	    String molID = "mol-"+i;
		        exposeOutputData(new NamedData(task.ID + molID, 
		        		result));
        	}
    		
    		tryWritingToOutfile(result);
    		outFileAlreadyUsed = true;
    	} else {
    		dealWithTaskMismatch();
        }
    	
    	if (result.getAtomContainerCount()==1)
    	{
    		return result.getAtomContainer(0);
    	} else {
    		if (outFile==null)
    		{
    			logger.warn("Multiple resulting geometries are exposed as "
    					+ "'" + task.ID + "' data. The initial geometry is "
    					+ "exposed as main output. You can save the multiple "
    					+ "geometries to file by using parameter '" 
    					+ WorkerConstants.PAROUTFILE + "'.");
    		}
    		return iac;
    	}
    }
   
//------------------------------------------------------------------------------

    /**
     * Apply the geometric modification as defined by the constructor.
     * @return 
     */

    private AtomContainerSet applyCartesianMove(IAtomContainer iac)
    {
        if (crtMove == null || crtMove.size() == 0)
        {
            Terminator.withMsgAndStatus("ERROR! No Cartesian move defined. "
                + "Please, try using 'CARTESIANMOVE' to provide a Cartesian "
                + "move.",-1);
        }

        String outMolBasename = MolecularUtils.getNameOrID(iac);
        
        // Identify the actual Cartesian move, possibly using reference 
        // substructure to identify the atom to be moved
        ArrayList<Point3d> actualMove = new ArrayList<Point3d>();
        for (int i=0; i<iac.getAtomCount(); i++)
        {
            actualMove.add(new Point3d());
        }
        if (this.refMol != null && this.refMol.getAtomCount() > 0)
        {
            logger.info("Matching reference sub-structure");
            if (this.refMol.getAtomCount() != crtMove.size())
            {
                Terminator.withMsgAndStatus("ERROR! Reference structure (size "
                     + this.refMol.getAtomCount() + ") and Cartesian step ("
                     + "size " + crtMove.size() + ") have different size. ",-1);
            }
            
            GeometryAlignment alignment = null;
			try {
				alignment = GeometryAligner.alignGeometries(refMol, iac);
			} catch (IllegalArgumentException | CloneNotSupportedException e) {
				 Terminator.withMsgAndStatus("ERROR! Could not match reference "
				 		+ "substructure in geometry to edit. "
				 		+ "Cannot perform Cartesian move.", -1, e);
			}
            Map<Integer,Integer> refToInAtmMap = alignment.getMappingIndexes();

            for (Integer refAtId : refToInAtmMap.keySet())
            {
                actualMove.set(refToInAtmMap.get(refAtId),crtMove.get(refAtId));
            }

            //Get the rototranslate actualMol as to make it consistent with the
            // reference's Cartesian move
            iac = alignment.getSecondIAC().iac;
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
        	scaleFactors = optimizeScalingFactors(iac, actualMove,
        			numScalingFactors,
        			scalingFactorDistribution,
        			percOfNeg,
        			percOfPos,
        			defTolInteratmDist, 
        			defTolCovRadSum,
        			maxDispl, 
        			defConvCrit, 
        			defMaxStep,
        			logger);
        }
        
        String msg = "Actual Cartesian move: " + NL;
        for (int i=0; i<actualMove.size(); i++)
        {
            Point3d pt = actualMove.get(i);
            msg = msg + "  " + i + ": " + pt + NL;
        }
        msg = msg + "Scaling factors: " + scaleFactors;
        logger.info(msg);

        // Produce the transformed geometry/ies
    	AtomContainerSet results = new AtomContainerSet();
        for (int j=0; j<scaleFactors.size(); j++)
        {
            logger.info("Generating results for scaling factor " 
            		+ scaleFactors.get(j));

            // Here we finally generate one new geometry
            IAtomContainer outMol = getGeomFromCartesianMove(iac,
            		actualMove,scaleFactors.get(j));
            String outMolName = outMolBasename + " - moved by " 
            		+ scaleFactors.get(j) + "x(Cartesian_move)";
            outMol.setProperty(CDKConstants.TITLE,outMolName);
            results.addAtomContainer(outMol); 
        }
        
        return results;
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
  				defMaxDispl, defConvCrit, defMaxStep, LogManager.getLogger());
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
  	 * @return the optimised list of scaling factors
  	 */
	public static ArrayList<Double> optimizeScalingFactors(IAtomContainer mol, 
    		ArrayList<Point3d> move, int numSfSteps, String distributionKind,
    		double percentualNeg, double percentualPos) 
	{
  		return optimizeScalingFactors(mol, move, numSfSteps, distributionKind,
  				percentualNeg, percentualPos,
  				defTolInteratmDist, defTolCovRadSum, defMaxDispl, defConvCrit, 
  				defMaxStep, LogManager.getLogger());
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
  	 * @return the optimised list of scaling factors
  	 */
	public static ArrayList<Double> optimizeScalingFactors(IAtomContainer mol, 
    		ArrayList<Point3d> move, int numSfSteps, String distributionKind) 
	{
  		return optimizeScalingFactors(mol,move,numSfSteps,distributionKind,
  				1.0, 1.0,
  				defTolInteratmDist, defTolCovRadSum, defMaxDispl, defConvCrit, 
  				defMaxStep, LogManager.getLogger());
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
  	 * @param logger logging tool
  	 * @return the optimised list of scaling factors
  	 */
	public static ArrayList<Double> optimizeScalingFactors(IAtomContainer mol, 
    		ArrayList<Point3d> move, int numSfSteps, String distributionKind,
    		double tolInteractDist, double tolCovRadSum,
    		double stretchLimit, double convergence, double maxStep, 
    		Logger logger) 
	{
		return optimizeScalingFactors(mol,move,numSfSteps,distributionKind,
  				1.0, 1.0, tolInteractDist, tolCovRadSum, stretchLimit,
  				convergence, maxStep, logger);
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
  	 * as valid (for negative scaling factors).
  	 * @param percentualPos the amount of the possible path to consider 
  	 * as valid (for positive scaling factors).
  	 * @param tolInteractDist permits down to this % of the initial 
  	 * interatomic distance.
  	 * @param tolCovRadSum permits down to this % of the sum of covalent 
  	 * radii.
  	 * @param stretchLimit permits up to this displacement for a single atom.
  	 * @param convergence stop search when step falls below this value.
  	 * @param maxStep maximum allowed step.
  	 * @param logger logging tool
  	 * @return the optimised list of scaling factors.
  	 */
	public static ArrayList<Double> optimizeScalingFactors(IAtomContainer mol, 
    		ArrayList<Point3d> move, int numSfSteps, String distributionKind,
    		double percentualNeg, double percentualPos,
    		double tolInteractDist, double tolCovRadSum,
    		double stretchLimit, double convergence, double maxStep, 
    		Logger logger)
	{
		logger.info("Optimizing Scaleing Factors");
        
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
			
			String tabTxt = "   Sign  MinDist    MaxDispl     Guess"
			    		+ "      Dir     LastStep  ok/old smallest!OK  "
			    		+ "largestOK Notes";
			
			// Get interatomic distances in initial geometry (for later)
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
					tabTxt = tabTxt + "CONVERGED!!! |"+step
							+"| < |"+convergence+"|" 
							+ System.getProperty("line.separator");
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
					tabTxt = tabTxt + String.format(
					    		"Attempt to use scaling factor %-11.3e stopped by "
					    		+ "max displacement: %-11.3e (>%-11.3e)",
					    		guessSF,maxDisplacement,stretchLimit);
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
            	tabTxt = tabTxt + 
            			String.format(Locale.ENGLISH,
            					"%+7.1f %-11.3e %-11.3e %-11.3e "
				    		+ "%-7.1f %-11.3e %1.1B(%1.1B) %-11.3e  %-11.3e %s",
						sign,lowestDist,maxDisplacement,guessSF,direction,step,
						isOK,lastWasOK,smallestToNotOK,largestToOK,reason);
				
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
			logger.info(tabTxt);
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
		
		String result = "Optimized extremes: " + optimalExtremes[0] + " " 
		+ optimalExtremes[1] + System.getProperty("line.separator");
        result = result + "Optimized Scaling factors: " + chosenScalingFactors;
        logger.info(result);
        
		return chosenScalingFactors;
	}

//------------------------------------------------------------------------------

    /**
     * Stretch a bond as defined by the SMARTS bond to stretch.
     * @param iac the atom container to stretch the bond in
     * @return the atom container with the stretched bond
     */
    private AtomContainerSet stretchBond(IAtomContainer iac)
    {
		String outMolBasename = MolecularUtils.getNameOrID(iac);

		if (smartsBondToStretch == null || smartsBondToStretch.getString().isEmpty())
		{
			Terminator.withMsgAndStatus("ERROR! No SMARTS bond to stretch defined. "
				+ "Please, try using 'SMARTSBONDTOSTRETCH' to provide a bond "
				+ "to stretch.", -1);
		}

		// Identify the bond to stretch
		Map<String, List<IBond>> bondsToStretch = SMARTSUtils.identifyBondsBySMARTS(
			iac, Map.of("bondToStretch", smartsBondToStretch));
		if (bondsToStretch.size() == 0)
		{
			Terminator.withMsgAndStatus("ERROR! No match for SMARTS '" 
			+ smartsBondToStretch.getString() + "' found in molecule '"
			+ outMolBasename + "'.", -1);
		} else if (bondsToStretch.size() > 1)
		{
			Terminator.withMsgAndStatus("ERROR! Multiple matches for SMARTS '" 
			+ smartsBondToStretch.getString() + "' found in molecule '"
			+ outMolBasename + "'. Can't "
			+ "stretch multiple bonds at once.", -1);
		}
		IBond bondToStretch = bondsToStretch.get("bondToStretch").get(0);

		// Make a suitable Zmatrix representation of the molecule
        ParameterStorage locPar = params.copy();
        locPar.setParameter(WorkerConstants.PARTASK, 
        		ZMatrixHandler.CONVERTTOZMATTASK.ID);
        locPar.removeData(WorkerConstants.PAROUTFILE);
        locPar.setParameter(WorkerConstants.PARNOOUTFILEMODE);
        Worker w;
		try {
			w = WorkerFactory.createWorker(locPar, this.getMyJob());
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
        ZMatrixHandler zmh = (ZMatrixHandler) w;
        ZMatrix zmatMol = zmh.makeZMatrix(iac);
        String msg = "Original ZMatrix: " + NL;
        List<String> txt = zmatMol.toLinesOfText(false,false);
        for (int i=0; i<txt.size(); i++)
        {
        	msg = msg + "  Line-" + i + ": " + txt.get(i) + NL;
        }
        logger.debug(msg);

		// Identify the internal coordinate corresponding to the bond to stretch
		ZMatrixAtom atomToMove = zmatMol.findBondDistance(
			iac.indexOf(bondToStretch.getBegin()), 
			iac.indexOf(bondToStretch.getEnd()));
		if (atomToMove == null)
		{
			Terminator.withMsgAndStatus("ERROR! No internal coordinate "
				+ "corresponding to the bond to stretch ("
				+ MolecularUtils.getAtomRef(bondToStretch.getBegin(), iac)
				+ "-" + MolecularUtils.getAtomRef(bondToStretch.getEnd(), iac)
				+ ") found.", -1);
		}
		int indexOfZAtomToAlter = zmatMol.zatoms().indexOf(atomToMove);
		double initialValue = zmatMol.getZAtom(indexOfZAtomToAlter).getIC(0).getValue();
		
		// produce modified geometries
		AtomContainerSet results = new AtomContainerSet();
		for (double scaleFactor : scaleFactors)
		{
			double newValue = initialValue * scaleFactor;
			zmatMol.getZAtom(indexOfZAtomToAlter).getIC(0).setValue(newValue);

			logger.info("Bond stretched by " + scaleFactor + "x(SMARTSBondToStretch: " 
				+ initialValue + ") (Current value: " + newValue + ")");

			IAtomContainer outMol = null;
			try {
				outMol = zmh.convertZMatrixToIAC(zmatMol, iac);
			} catch (Throwable e) {
				Terminator.withMsgAndStatus("ERROR! Exception while "
					+ "converting ZMatrix to IAC. Cause of the exception: "
					+ e.getMessage(), -1);
			}
			outMol.setProperty(CDKConstants.TITLE, outMolBasename 
				+ " - stretched by " + scaleFactor );
			results.addAtomContainer(outMol);
		}
		return results;
    }

//------------------------------------------------------------------------------

    /**
     * Apply the geometric modification as defined by the constructor.
     * @throws Throwable when there are issues converting the ZMatrix and it 
     * is preferable to redefine the ZMatrix, possibly adding dummy atoms
     */

    private AtomContainerSet applyZMatrixMove(IAtomContainer iac) throws Throwable
    {
        if (zmtMove == null || zmtMove.getZAtomCount() == 0)
        {
            Terminator.withMsgAndStatus("ERROR! No ZMatrix move defined. "
                + "Please, try using 'ZMATRIXMOVE' to provide a geometrical "
                + "modification as changes of internal coordinates.",-1);
        }

        String outMolBasename = MolecularUtils.getNameOrID(iac);
        
        // Get the ZMatrix of the molecule to work with
        ParameterStorage locPar = params.copy();
        locPar.setParameter(WorkerConstants.PARTASK, 
        		ZMatrixHandler.CONVERTTOZMATTASK.ID);
        locPar.removeData(WorkerConstants.PAROUTFILE);
        locPar.setParameter(WorkerConstants.PARNOOUTFILEMODE);
        
        Worker w = WorkerFactory.createWorker(locPar, this.getMyJob());
        
        ZMatrixHandler zmh = (ZMatrixHandler) w;
        ZMatrix inZMatMol = zmh.makeZMatrix(iac);
        String msg = "Original ZMatrix: " + NL;
        List<String> txt = inZMatMol.toLinesOfText(false,false);
        for (int i=0; i<txt.size(); i++)
        {
        	msg = msg + "  Line-" + i + ": " + txt.get(i) + NL;
        }
        logger.debug(msg);

        // Identify the actual move in terms of ZMatrix
        ZMatrix actualZMatMove = new ZMatrix();
        if (iac.getAtomCount() != zmtMove.getZAtomCount())
        {
            logger.fatal("TODO: what if only some IC is modified?");
            Terminator.withMsgAndStatus("ERROR! Still in development",-1);
        }
        else
        {
            actualZMatMove = new ZMatrix(zmtMove.toLinesOfText(false,false));
        }
        String msg2 = "Actual ZMatrixMove: " + NL;
        List<String> txt2 = actualZMatMove.toLinesOfText(false,false);
        for (int i=0; i<txt2.size(); i++)
        {
        	msg2 = msg2 + "  Line-" + i + ": " + txt.get(i) + NL;
        }
        msg2 = msg2 + "Scaling factors: " + scaleFactors;
        logger.debug(msg2);

        // Produce the transformed geometry/ies
    	AtomContainerSet results = new AtomContainerSet();
        for (int j=0; j<scaleFactors.size(); j++)
        {
            logger.info("Generating results for scaling factor " 
            		+ scaleFactors.get(j));

            // Apply ZMatrixMove
            ZMatrix modZMat = zmh.modifyZMatrix(inZMatMol,actualZMatMove,
                                                           scaleFactors.get(j));

            // Convert to Cartesian coordinates
            IAtomContainer outMol = zmh.convertZMatrixToIAC(modZMat, iac);

            // Prepare and print output to file
            String outMolName = outMolBasename + " - moved by " 
            		+ scaleFactors.get(j) + "x(ZMatrixMove)";
            outMol.setProperty(CDKConstants.TITLE,outMolName);
            results.addAtomContainer(outMol);
        }
        
        return results;
    }
 
//------------------------------------------------------------------------------

}
