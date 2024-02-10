package autocompchem.molecule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/*   
 *   Copyright (C) 2018  Marco Foscato 
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import autocompchem.atom.AtomUtils;
import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.geometry.DistanceMatrix;
import autocompchem.modeling.AtomSpecificStringGenerator;
import autocompchem.run.Job;
import autocompchem.worker.DummyWorker;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerConstants;

/**
 * Unit Test for {@link AtomContainerInputProcessor}.
 * 
 * @author Marco Foscato
 */

public class AtomContainerInputProcessorTest 
{

	private static IChemObjectBuilder chemBuilder = 
    		DefaultChemObjectBuilder.getInstance();
	
	private static Task READTASK = Task.make("readAtomContainers", true);
	
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
        ps.setParameter(WorkerConstants.PARTASK, READTASK.ID);        
    	
    	NamedDataCollector results = new NamedDataCollector();
    	
        // Reading all from param storage
        ps.setParameter(new NamedData(ChemSoftConstants.PARGEOM, iacs));
        IACInputTester tester = new IACInputTester();
    	tester.setParameters(ps);
    	tester.setDataCollector(results);
    	tester.initialize();
    	tester.processInput();
    	
    	assertEquals(3, results.size());
    	
        // Reading only a specific one from param storage
        ps.setParameter(ChemSoftConstants.PARMULTIGEOMID, "1");
        
        results.clear();
    	tester = new IACInputTester();
    	tester.setParameters(ps);
    	tester.setDataCollector(results);
    	tester.initialize();
    	tester.processInput();
    	
    	assertEquals(1, results.size());
    	assertEquals(2, ((IAtomContainer)
    			results.getNamedData(READTASK.ID+0).getValue()).getAtomCount());
    	
	}
	
//------------------------------------------------------------------------------

	private class IACInputTester extends AtomContainerInputProcessor
    {
	    @Override
	    public Set<Task> getCapabilities() {
	        return Collections.unmodifiableSet(new HashSet<Task>(
	                Arrays.asList(READTASK)));
	    }

		public IACInputTester() {}

		@Override
		public void performTask() {}

		@Override
		public Worker makeInstance(Job job) {
			return new IACInputTester();
		}

		@Override
		public void processOneAtomContainer(IAtomContainer iac) {
			output.add(iac);
		}

		@Override
		public void prepareOutputToExpose() {
			for (int i=0; i<output.size(); i++)
			{
				IAtomContainer iac = (IAtomContainer) output.get(i);
				exposeOutputData(new NamedData(READTASK.ID+i,iac));
			}
		}
    }

//------------------------------------------------------------------------------

}
