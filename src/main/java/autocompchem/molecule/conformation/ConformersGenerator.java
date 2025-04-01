package autocompchem.molecule.conformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.AtomContainerSet;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.IOtools;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.MolecularReorderer;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixAtom;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixHandler;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.utils.ListOfListsCombinations;
import autocompchem.utils.StringUtils;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;


/**
 * Tool to generate conformers. Note that no energy computation is performed 
 * here: conformers are just generated based on the input geometry and 
 * changes in internal coordinates.
 * 
 * @author Marco Foscato
 */

public class ConformersGenerator extends AtomContainerInputProcessor
{
    /**
     * String defining the task of generating conformers
     */
    public static final String GENERATECONFORMERSTASKNAME = 
    		"generateConformers";

    /**
     * Task about generating a definition of conformational 
     * space
     */
    public static final Task GENERATECONFORMERSTASK;
    static {
    	GENERATECONFORMERSTASK = 
    			Task.make(GENERATECONFORMERSTASKNAME);
    }

//-----------------------------------------------------------------------------
	
    /**
     * Constructor.
     */
    public ConformersGenerator()
    {}
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(GENERATECONFORMERSTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/ConformersGenerator.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new ConformersGenerator();
    }
    
//------------------------------------------------------------------------------

	@Override
	public IAtomContainer processOneAtomContainer(IAtomContainer iac, int i) 
	{
		AtomContainerSet conformers = new AtomContainerSet();
    	if (task.equals(GENERATECONFORMERSTASK))
    	{
    		// Reorder atom list to ensure generation of ZMatrix
    		MolecularReorderer reorderer = new MolecularReorderer();
    		IAtomContainer orderedIAC = reorderer.reorderContainer(iac);
    		
    		// Generate the conformational space definition
    		ParameterStorage confSpaceParams = params.clone();
    		confSpaceParams.setParameter(WorkerConstants.PARTASK, 
    				ConformationalSpaceGenerator.GENERATECONFORMATIONALSPACETASKNAME);
    		confSpaceParams.removeData(WorkerConstants.PAROUTFILE);
    		confSpaceParams.removeData(WorkerConstants.PAROUTFORMAT);
    		ConformationalSpaceGenerator csg;
			try {
				csg = (ConformationalSpaceGenerator) 
						WorkerFactory.createWorker(confSpaceParams, myJob);
			} catch (ClassNotFoundException e) {
				// If this happen thing are seriously broken
				throw new IllegalStateException(e);
			}
    		ConformationalSpace cs = csg.createConformationalSpace(iac);
    		
    		// Adjust to reordered atom list
    		Map<Integer, Integer> reorderingMap = 
    				MolecularReorderer.getAtomReorderingMap(orderedIAC);
    		cs.applyReorderingMap(reorderingMap);
    		
    		// Generate the actual conformers
    		conformers = generateConformers(orderedIAC, cs, logger);
    		
    		if (outFile != null)
            {
            	outFileAlreadyUsed = true;
            	IOtools.writeAtomContainerSetToFile(outFile, conformers, 
            			outFormat, true);
            }
            
            if (exposedOutputCollector != null)
        	{
    			String molID = "mol-"+i;
    	        exposeOutputData(new NamedData(
    	        		GENERATECONFORMERSTASK.ID + "-" + molID, conformers));
        	}
    	} else {
    		dealWithTaskMismatch();
        }

    	if (conformers.getAtomContainerCount() == 1)
    	{
    		return conformers.getAtomContainer(0);
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
	 * Given a molecule and a conformational space, produces all the
	 * conformers resulting from the exhaustive combination of all the
	 * degrees of freedom defined in the conformational space.
	 * @param iac the initial geometry to work with
	 * @param confSpace the definition of the degrees of freedom to consider
	 * @return the list of non-optimized conformers.
	 */
	public static AtomContainerSet generateConformers(IAtomContainer iac, 
			ConformationalSpace confSpace, Logger logger) 
	{
		AtomContainerSet conformers = new AtomContainerSet();
		
		String msg = "Generating conformers for " + MolecularUtils.getNameOrID(
				iac) + NL + confSpace.toPrintableString();
		logger.info(msg);
		
		// Make the initial ZMatrix which we can then use to produce conformers
		ParameterStorage zmahParams = new ParameterStorage();
		zmahParams.setParameter(WorkerConstants.PARTASK, 
				ZMatrixHandler.CONVERTTOZMATNAME);
		ZMatrixHandler zmh;
		try {
			//TODO should try to prioritize bonds in the conf.space.
			zmh = (ZMatrixHandler) WorkerFactory.createWorker(zmahParams, null);
		} catch (ClassNotFoundException e) {
			// This should never happen unless things are deeply broken
			throw new IllegalStateException(e);
		}
		ZMatrix originalZMat = zmh.makeZMatrix(iac, null);
		
		// Convert conformational coordinates into torsional steps
		// WARNING: assumption that we have only torsional degrees of freedom!
		if (!confSpace.containsOnlyTorsions())
		{
			String msgTors = "Can only generate conformers using "
					+ "torsional degree offreedom. The given conformational "
					+ "includes: ";
			for (ConformationalCoordinate cc : confSpace)
		    {
				msgTors = msgTors + NL + cc.getType();
		    }
					
			Terminator.withMsgAndStatus(msgTors, -1);
		}
        List<List<Double>> listsOfConfSteps = new ArrayList<List<Double>>();
        List<ConformationalCoordinate> sortedCoords = 
        		new ArrayList<ConformationalCoordinate>();
        for (ConformationalCoordinate coord : confSpace)
        {
        	int fold = coord.getFold();
        	List<Double> stepsOnThisCoord = new ArrayList<Double>();
        	if (fold == 0)
        	{
        		// Just in case that for some reason we have coordinates that 
        		// are actually not doing anything.
        		stepsOnThisCoord.add(0.0);
        	} else {
	        	for (int step = 0; step < fold; step++)
	        	{
	        		stepsOnThisCoord.add(360.0 * step / fold);
	        	}
        	}
        	listsOfConfSteps.add(stepsOnThisCoord);
        	sortedCoords.add(coord);
        }
        
        // Generate modified geometries
        Iterator<List<Double>> iterator = new ListOfListsCombinations<Double>(
        		listsOfConfSteps);
        int counter = -1;
        while (iterator.hasNext())
        {
        	counter++;
        	List<Double> steps = iterator.next();
        	
        	logger.debug("Generating conformer for alteration " + counter 
        			+ ": " + StringUtils.mergeListToString(steps, "   ", true));
        	
        	// Make ZmatMove
        	ZMatrix editedZMat = originalZMat.clone();
        	int i=0;
        	for (ConformationalCoordinate coord : confSpace)
            {
        		double step = steps.get(i);
        		// WARNING assumption we have only two atoms
        		int atmA = coord.getAtomIDs().get(0);
        		int atmB = coord.getAtomIDs().get(1);
        		for (ZMatrixAtom za : editedZMat.findAllTorsions(atmA, atmB))
        		{
        			za.getIC(2).setValue(za.getIC(2).getValue() + step);
        		}
        		for (ZMatrixAtom za : editedZMat.findAllTorsions(atmB, atmA))
        		{
        			za.getIC(2).setValue(za.getIC(2).getValue() + step);
        		}
        		i++;
            }
        	
        	// Store conformer
        	try {
        		IAtomContainer conformer = zmh.convertZMatrixToIAC(editedZMat, 
        				iac);
        		conformers.addAtomContainer(conformer);
			} catch (Throwable e) {
				e.printStackTrace();
				Terminator.withMsgAndStatus("Could not convert ZMatrix "
						+ "representation to XYZ.", -1, e);
			}
        }
        
		return conformers;
	}

//------------------------------------------------------------------------------

}
