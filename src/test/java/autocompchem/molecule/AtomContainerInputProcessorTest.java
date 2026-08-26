package autocompchem.molecule;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.IOtools;
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

	private final String SEP = System.getProperty("file.separator");

	@TempDir
	File tempDir;
	
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
        ps.setParameter(ParameterConstants.VERBOSITY, 0); 
    	
    	NamedDataCollector results = new NamedDataCollector();
    	
        // Reading all from param storage        
        Worker tester = WorkerFactory.createWorker(ps, null);
    	tester.setDataCollector(results);
    	tester.performTask();
    	
    	// Since we deal with the geoms one by one only the last one will be 
    	// present in the main exposed geometry, but all three will be added to
    	// task-specific exposed output. So 4 items in total
    	assertEquals(4, results.size());
    	
        // Reading only a specific one from param storage
        ps.setParameter(ChemSoftConstants.PARMULTIGEOMID, "1");
        
        results.clear();
        tester = WorkerFactory.createWorker(ps, null);
    	tester.setDataCollector(results);
    	tester.performTask();
    	
    	// Since we deal with the mols all in one the main exposed geometry
    	// contains all three mols, but the chosen geometry is also saved as one
    	// task-specific exposed output. So 2 items.
    	assertEquals(2, results.size());
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
    	
    	// Since we deal with the geoms one by one only the last one will be 
    	// present in the main exposed geometry, but all three will be added to
    	// task-specific exposed output. So 4 items in total
    	assertEquals(4, results.size());
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

    	// Since we deal with the mols all in one the main exposed geometry
    	// contains all three mols, but the chosen geometry is also saved as one
    	// task-specific exposed output. So 2 items.
    	assertEquals(2, results.size());
    	assertEquals(2, ((IAtomContainer)
    			results.getNamedData(taskId+1).getValue()).getAtomCount());
	}

//------------------------------------------------------------------------------

	@Test
	public void testProcessManyInputFilesParameter() throws Exception
	{
		IAtomContainer molA = chemBuilder.newAtomContainer();
		molA.setTitle("mol_A");
		molA.addAtom(new Atom("H"));

		IAtomContainer molB = chemBuilder.newAtomContainer();
		molB.setTitle("mol_B");
		molB.addAtom(new Atom("C"));
		molB.addAtom(new Atom("O"));

		IAtomContainer molC = chemBuilder.newAtomContainer();
		molC.setTitle("mol_C");
		molC.addAtom(new Atom("N"));

		File file1 = new File(tempDir.getAbsolutePath() + SEP + "file1.xyz");
		File file2 = new File(tempDir.getAbsolutePath() + SEP + "file2.xyz");
		IOtools.writeXYZAppend(file1, molA, false);
		IOtools.writeXYZAppend(file1, molB, true);
		IOtools.writeXYZAppend(file2, molC, false);

		ParameterStorage ps = new ParameterStorage();
		ps.setParameter(WorkerConstants.PARTASK,
				AtomContainerInputProcessor.READIACSTASK.ID);
		ps.setParameter(WorkerConstants.PARMANYINFILE,
				file1.getAbsolutePath() + " " + file2.getAbsolutePath());
		ps.setParameter(ChemSoftConstants.PARMULTIGEOMMODE,
				MultiGeomMode.ALLINONEJOB.toString());
		ps.setParameter(ParameterConstants.VERBOSITY, 0);

		AtomContainerInputProcessor worker =
				(AtomContainerInputProcessor) WorkerFactory.createWorker(ps, null);
		assertEquals(3, worker.inMols.size());
		assertEquals("mol_A", worker.inMols.get(0).getTitle());
		assertEquals("mol_B", worker.inMols.get(1).getTitle());
		assertEquals("mol_C", worker.inMols.get(2).getTitle());
	}

//------------------------------------------------------------------------------

}
