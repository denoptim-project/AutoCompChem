package autocompchem.run;

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

import java.io.File;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.io.IOtools;
import autocompchem.modeling.atomtuple.AtomTupleConstants;
import autocompchem.molecule.MolecularMeter;
import autocompchem.molecule.geometry.GeomDescriptorDefinition;
import autocompchem.worker.WorkerConstants;


/**
 * Unit Test for the ACCJob. 
 * 
 * @author Marco Foscato
 */

public class ACCJobTest 
{
    private final String SEP = System.getProperty("file.separator");

    @TempDir 
    File tempDir;
    
//------------------------------------------------------------------------------

    @Test
    public void testRunACCJob() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");

        //Define pathnames
        File sdfFile = new File(tempDir.getAbsolutePath() + SEP + "mol.sdf");

        try 
        {
        	String label = "DISTLABELTOMATCH";
        	
        	// Prepare the parameters for task
        	ParameterStorage params = new ParameterStorage();
        	params.setParameter(ParameterConstants.RUNNABLEAPPIDKEY,
        			SoftwareId.ACC.toString());
        	params.setParameter(WorkerConstants.PARTASK, 
        			MolecularMeter.MEASUREGEOMDESCRIPTORSTASK.ID);
        	params.setParameter(WorkerConstants.PARINFILE,
        			sdfFile.getAbsolutePath());
        	params.setParameter(ParameterConstants.VERBOSITY, "7");
        	params.setParameter(AtomTupleConstants.KEYRULETYPEATOMIDS, 
        			"0 1 " + GeomDescriptorDefinition.KEYNAME + "=" + label);
        	
        	// Prepare structure
        	IAtomContainer mol = new AtomContainer();
        	mol.addAtom(new Atom("C", new Point3d(0.0, 0.0, 0.0)));
        	mol.addAtom(new Atom("O", new Point3d(2.0, 0.0, 0.0)));
        	mol.addAtom(new Atom("N", new Point3d(1.0, 1.0, 0.0)));
        	mol.addAtom(new Atom("P", new Point3d(1.0, 0.0, 2.0)));
        	IOtools.writeSDFAppend(sdfFile, mol, false);

            // Create job
            Job job = JobFactory.createJob(SoftwareId.ACC);
            job.setParameters(params);
            
            // Run the job
            job.run();
            
            // Check that results are present
            NamedDataCollector results = job.getOutputCollector();
            assertEquals(2, results.size());
            
            // Get the right result
            String key = "";
            for (String candKey : results.getAllNamedData().keySet())
            	if (candKey.contains(label))
            		key = candKey;
            
            // Check the actual value
            String valueStr = results.getNamedData(key).getValueAsLines().get(0);
            double dist = 0.0;
			try {
				dist = Double.parseDouble(valueStr);
			} catch (Exception e) {
				assertTrue(false, "Found numerical result");
			}
            assertTrue(Math.abs(dist-2.0) < 0.0001, "Correct numerical output");
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            assertFalse(true, "Unable to work with tmp files.");
        }
    }

//------------------------------------------------------------------------------

}
