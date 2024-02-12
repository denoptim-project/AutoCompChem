package autocompchem.molecule.geometry;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.EnumUtils;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.ChemSoftConstants.CoordsType;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.modeling.constraints.ConstraintsGenerator;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.molecule.AtomContainerInputProcessor;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixConstants;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixHandler;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;


/**
 * Tool to handle molecular geometry representations.
 * 
 * @author Marco Foscato
 */

public class MolecularGeometryHandler extends AtomContainerInputProcessor
{
    
    /**
     * Type of coordinate representation
     */
    private CoordsType coordsType = CoordsType.XYZ;
    
    /**
     * Verbosity level
     */
    protected int verbosity = 0;

    /**
     * String defining the task of generating tuples of atoms
     */
    public static final String GETMOLECULARGEOMETRYTASKNAME = 
    		"getMolecularGeometry";

    /**
     * Task about generating tuples of atoms
     */
    public static final Task GETMOLECULARGEOMETRYTASK;
    static {
    	GETMOLECULARGEOMETRYTASK = Task.make(GETMOLECULARGEOMETRYTASKNAME);
    }

//-----------------------------------------------------------------------------
	
    /**
     * Constructor.
     */
    public MolecularGeometryHandler()
    {}
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(GETMOLECULARGEOMETRYTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/MolecularGeometryHandler.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new MolecularGeometryHandler();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialize the worker according to the parameters loaded by constructor.
     */

	@Override
    public void initialize()
    {
		super.initialize();
		
        if (params.contains(ChemSoftConstants.PARCOORDTYPE))
        {
        	coordsType = EnumUtils.getEnumIgnoreCase(CoordsType.class,
        			params.getParameter(ChemSoftConstants.PARCOORDTYPE)
        			.getValueAsString());
        }
        
        /*
         * TODO: replace this functionality with the use of AtomLabelsGenerator
        if (params.contains(ChemSoftConstants.PARUSEATMTAGS))
        {
        	useAtomTags = params.getParameter(
        			ChemSoftConstants.PARUSEATMTAGS).getValueAsString();
        }
        */
    }
    
//-----------------------------------------------------------------------------

    /**
     * Performs any of the registered tasks according to how this worker
     * has been initialised.
     */

    @Override
    public void performTask()
    {
    	if (task.equals(GETMOLECULARGEOMETRYTASK))
    	{
    		createMolGeometryDefinitions();
    	} else {
    		dealWithTaskMistMatch(); //TODO-gg fix typo mismatch
        }
    }

//------------------------------------------------------------------------------

    /**
     * Create list of molecular geometry definitions with the settings available
     *  in this instance.
     */
    public void createMolGeometryDefinitions()
    {
    	processInput();
    }
    
//------------------------------------------------------------------------------

	@Override
	public void processOneAtomContainer(IAtomContainer iac, int i) 
	{
		switch (coordsType)
    	{    
        	case ZMAT:
        	{
        		ParameterStorage zmatMakerTask = new ParameterStorage();
        		zmatMakerTask.setParameter(WorkerConstants.PARTASK, 
        				ZMatrixHandler.PRINTZMATRIXTASK.ID);
        		zmatMakerTask.setParameter("MOL", //TODO-gg use ChemSoftConstants.PARGEOM
        				NamedDataType.IATOMCONTAINER, 
        				iac);
                Worker w = null;
				try {
					w = WorkerFactory.createWorker(zmatMakerTask,myJob);
				} catch (ClassNotFoundException e1) {
					//Cannot happen!
					e1.printStackTrace();
				}
                ZMatrixHandler zmh = (ZMatrixHandler) w;
                ZMatrix zmat = zmh.makeZMatrix();
                
                if (params.contains(ZMatrixConstants.SELECTORMODE))
                {
                	ParameterStorage cnstMakerTask = params.clone();
                	cnstMakerTask.setParameter(WorkerConstants.PARTASK, 
                			ConstraintsGenerator.GENERATECONSTRAINTSTASK.ID);

                    ConstraintsGenerator cnstrg = null;
					try {
						cnstrg = (ConstraintsGenerator)
								WorkerFactory.createWorker(cnstMakerTask, myJob);
					} catch (ClassNotFoundException e1) {
						//Cannot happen!
						e1.printStackTrace();
					}
                	ConstraintsSet cs = new ConstraintsSet();
                	try {
    					cs = cnstrg.createConstraints(iac);
    				} catch (Exception e) {
    					e.printStackTrace();
    					Terminator.withMsgAndStatus("ERROR! "
    							+ "Unable to create constraints. "
    							+ "Exception from the "
    							+ ConstraintsGenerator.class.getSimpleName() 
    							+ ".", -1);
    				}
                	String mode = params.getParameterValue(
                			ZMatrixConstants.SELECTORMODE);
                	switch (mode.toUpperCase())
                	{
                	case ZMatrixConstants.SELECTORMODE_CONSTANT:
                    	zmat.setConstants(cs);
                		break;

                	case ZMatrixConstants.SELECTORMODE_VARIABLES:
                    	zmat.setVariables(cs);
                		break;
                	}
                }
                
        		output.add(zmat);
        		break;
        	}
        	
        	case XYZ:
        	default:
        	{
//        		if (useAtomTags)
//        		{
//        			mol = MolecularUtils.makeSimpleCopyWithAtomTags(mol);
//        		}
        		output.add(iac);
        		break;
        	}
    	}
		
		if (exposedOutputCollector != null)
    	{
			exposeOutputData(new NamedData(task.ID+i, output.get(i)));
    	}
	}
    
//-----------------------------------------------------------------------------

}
