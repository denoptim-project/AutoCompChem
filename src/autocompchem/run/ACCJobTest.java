package autocompchem.run;

import static org.junit.jupiter.api.Assertions.assertFalse;

/*   
 *   Copyright (C) 2018  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.io.IOtools;


/**
 * Unit Test for the ACCJob. 
 * 
 * @author Marco Foscato
 */

public class ACCJobTest 
{

    private final String SEP = System.getProperty("file.separator");
    private final ByteArrayOutputStream outRec = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @TempDir 
    File tempDir;
    
//------------------------------------------------------------------------------

    @Test
    public void testRunACCJob() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");

        //Define pathnames
        String sdfFile = tempDir.getAbsolutePath() + SEP + "mol.sdf";

        try 
        {
        	String label = "DISTLABELTOMATCH";
        	
        	// Prepare the parameters for task
        	ParameterStorage params = new ParameterStorage();
        	params.setParameter(
        			new Parameter(ParameterConstants.RUNNABLEAPPIDKEY,
        					NamedDataType.STRING, Job.RunnableAppID.ACC));
        	params.setParameter(new Parameter("TASK", 
        			NamedDataType.STRING, "MeasureGeomDescriptors"));
        	params.setParameter(new Parameter("VERBOSITY",
        			NamedDataType.INTEGER,1));
        	params.setParameter(new Parameter("INFILE",
        			NamedDataType.STRING,sdfFile));
        	params.setParameter(new Parameter("ATOMINDEXES",
        			NamedDataType.STRING, label+" 1 2"));
        	
        	// Prepare structure
        	IAtomContainer mol = new AtomContainer();
        	mol.addAtom(new Atom("C", new Point3d(0.0, 0.0, 0.0)));
        	mol.addAtom(new Atom("O", new Point3d(2.0, 0.0, 0.0)));
        	mol.addAtom(new Atom("N", new Point3d(1.0, 1.0, 0.0)));
        	mol.addAtom(new Atom("P", new Point3d(1.0, 0.0, 2.0)));
        	IOtools.writeSDFAppend(sdfFile, mol, false);

            // Create job
            Job job = JobFactory.createJob(Job.RunnableAppID.ACC);
            job.setParameters(params);
            
            // Run redirecting output to readable stream (only for the job)
            System.setOut(new PrintStream(outRec));
            job.run();
            System.setOut(originalOut);
            
            // Red the record of stdout to find the result, if any
            String outStr = outRec.toString();
            assertTrue(outStr.contains(label),
            		"Result not found in STDOUT");
            outStr = outStr.substring(outStr.indexOf(label));
            outStr = outStr.substring(outStr.indexOf("=")+1).trim();
            double dist = 0.0;
			try {
				dist = Double.parseDouble(outStr);
			} catch (Exception e) {
				assertTrue(false,"Found numerical result");
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
