package autocompchem.molecule;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.molecule.AtomContainerInputProcessor.MultiGeomMode;
import autocompchem.wiro.chem.ChemSoftConstants;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;
import autocompchem.worker.WorkerFactory;

/**
 * Unit Test for {@link AtomContainerInputProcessor}.
 * 
 * @author Marco Foscato
 */

public class AtomContainerInputProcessorTest 
{

	private static IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();
	
//------------------------------------------------------------------------------
	
	@Test
	public void testProcessInput() throws Exception
	{
		IAtomContainer molA = chemBuilder.newAtomContainer();
    	molA.addAtom(new Atom("H"));
    	molA.addAtom(new Atom("C"));
    	molA.addAtom(new Atom("O"));
    	molA.addAtom(new Atom("Ru"));
    	molA.addAtom(new PseudoAtom("Du"));
    	molA.addAtom(new PseudoAtom("Xx"));

		IAtomContainer molB = chemBuilder.newAtomContainer();
    	molB.addAtom(new Atom("He"));
    	molB.addAtom(new Atom("Si"));

		IAtomContainer molC = chemBuilder.newAtomContainer();
    	molC.addAtom(new Atom("W"));
    	molC.addAtom(new Atom("Cl"));
    	molC.addAtom(new Atom("Au"));
    	molC.addAtom(new Atom("P"));
    	
    	List<IAtomContainer> iacs = new ArrayList<IAtomContainer>();
    	iacs.add(molA);
    	iacs.add(molB);
    	iacs.add(molC);
    	
    	ParameterStorage ps = new ParameterStorage();
    	String taskId = AtomContainerInputProcessor.READIACSTASK.ID;
        ps.setParameter(WorkerConstants.PARTASK, taskId);
        ps.setParameter(new NamedData(ChemSoftConstants.PARGEOM, iacs));   
    	
    	NamedDataCollector results = new NamedDataCollector();
    	
        // Reading all from param storage        
        Worker tester = WorkerFactory.createWorker(ps, null);
    	tester.setDataCollector(results);
    	tester.performTask();
    	
    	assertEquals(3, results.size());
    	
        // Reading only a specific one from param storage
        ps.setParameter(ChemSoftConstants.PARMULTIGEOMID, "1");
        
        results.clear();
        tester = WorkerFactory.createWorker(ps, null);
    	tester.setDataCollector(results);
    	tester.performTask();
    	
    	assertEquals(1, results.size());
    	assertEquals(2, ((IAtomContainer)
    			results.getNamedData(taskId+1).getValue()).getAtomCount());
    	

    	// Use multigeom mode
        ps.setParameter(ChemSoftConstants.PARMULTIGEOMMODE, 
        		MultiGeomMode.ALLINONEJOB.toString());
        ps.removeData(ChemSoftConstants.PARMULTIGEOMID);
        
        results.clear();
        tester = WorkerFactory.createWorker(ps, null);
    	tester.setDataCollector(results);
    	tester.performTask();
    	
    	assertEquals(3, results.size());
    	assertEquals(6, ((IAtomContainer)
    			results.getNamedData(taskId+0).getValue()).getAtomCount());
    	assertEquals(2, ((IAtomContainer)
    			results.getNamedData(taskId+1).getValue()).getAtomCount());
    	assertEquals(4, ((IAtomContainer)
    			results.getNamedData(taskId+2).getValue()).getAtomCount());
    	
    	// Overwriting of multigeom mode
        ps.setParameter(ChemSoftConstants.PARMULTIGEOMID, "1");
        
        results.clear();
        tester = WorkerFactory.createWorker(ps, null);
    	tester.setDataCollector(results);
    	tester.performTask();
    	
    	assertEquals(1, results.size());
    	assertEquals(2, ((IAtomContainer)
    			results.getNamedData(taskId+1).getValue()).getAtomCount());
	}

//------------------------------------------------------------------------------

}
