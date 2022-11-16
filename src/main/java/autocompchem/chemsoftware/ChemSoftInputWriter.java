package autocompchem.chemsoftware;

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

import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Terminator;
import autocompchem.worker.Worker;

/**
 * Core components of any tool writing input files for software packages.
 *
 * @author Marco Foscato
 */

public abstract class ChemSoftInputWriter extends Worker
{
    
    /**
     * Molecular geometries input file. One or more geometries depending on the
     * kind of computational chemistry job. 
     */
    private String inGeomFile;
    
    /**
     * List of molecular systems considered as input. This can either be
     * the list of molecules for which we want to make the input, or the list
     * of geometries used to make a multi-geometry input file.
     */
    private ArrayList<IAtomContainer> inpGeom = new ArrayList<IAtomContainer>();

    /**
     * Definition of how to use multiple geometries
     */
    private enum MultiGeomMode {SingleGeom,ReactProd,ReactTSProd,Path};
    
    /**
     * Chosen mode of handling multiple geometries.
     */
    private MultiGeomMode multiGeomMode = MultiGeomMode.SingleGeom;

    /**
     * Geometry names
     */
    private ArrayList<String> geomNames = new ArrayList<String>(
                                                     Arrays.asList("geometry"));

    /**
     * Input format identifier.
     */
    private String inFileFormat = "nd";

    /**
     * Pathname root for output files (input for comp.chem. software).
     */
    private String outFileNameRoot;
    
    /**
     * Output name (input for comp.chem. software).
     */
    private String outFileName;

    /**
     * Output job details name.
     */
    private String outJDFile;
    
    /**
     * Default value for integers
     */
    private final int def = -999999;

    /**
     * charge of the whole system
     */
    private int charge = def;
    
    /**
     * Spin multiplicity of the whole system
     */
    private int spinMult = def;
    
    /**
     * The computational chemistry job we want to prepare the input for.
     */
    protected CompChemJob ccJob;

    /** 
     * Verbosity level
     */
    protected int verbosity = 0;
    
    /**
     * Default extension of the chem.soft. input file
     */
    protected String inpExtrension;

    /**
     * Default extension of the chem.soft output file
     */
    protected String outExtension;

    private final String NL = System.getProperty("line.separator");
    

//-----------------------------------------------------------------------------

    public ChemSoftInputWriter()
    {
    	super("inputdefinition/ChemSoftInputWriter.json");
    }
    
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters provided in the 
     * collection of input parameters.
     */

    @Override
    public void initialize()
    {
        if (params.contains(ChemSoftConstants.PARVERBOSITY))
        {
            String str = params.getParameter(
                    ChemSoftConstants.PARVERBOSITY).getValueAsString();
            this.verbosity = Integer.parseInt(str);

            if (verbosity > 0)
                System.out.println(" Adding parameters to chemical software "
                		+ "input writer.");
        }

        if (params.contains(ChemSoftConstants.PARGEOMFILE))
        {
            this.inGeomFile = params.getParameter(
                    ChemSoftConstants.PARGEOMFILE).getValueAsString();
            
            //TODO: use automated detection of file type
            
            if (inGeomFile.endsWith(".sdf"))
            {
                inFileFormat = "SDF";
            }
            else if (inGeomFile.endsWith(".xyz"))
            {
                inFileFormat = "XYZ";
            }
            else if (inGeomFile.endsWith(".out"))
            {
                //TODO: identify the kind of cc-software that produced that file
                Terminator.withMsgAndStatus("ERROR! Format of file '"
                        + inGeomFile + "' not recognized!",-1);
            }
            else
            {
                Terminator.withMsgAndStatus("ERROR! Format of file '" 
                        + inGeomFile + "' not recognized!",-1);
            }
            FileUtils.foundAndPermissions(this.inGeomFile,true,false,false);
            
            //TODO: make and use a general molecular structure reader
            switch (inFileFormat) 
            {
            case "SDF":
                inpGeom = IOtools.readSDF(inGeomFile);
                break;

            case "XYZ":
                inpGeom = IOtools.readXYZ(inGeomFile);
                break;

            default:
                Terminator.withMsgAndStatus("ERROR! " 
                    + this.getClass().getName()
                    + " can read multi-geometry input files "
                    + "only when starting from XYZ of SDF files. Make "
                    + "sure file '" + inGeomFile + "' has proper "
                    + "format and extension.", -1);
            }
        }

        if (params.contains(ChemSoftConstants.PARMULTIGEOMMODE))
        {
            String value = 
                    params.getParameter(ChemSoftConstants.PARMULTIGEOMMODE)
                    .getValueAsString();
            this.multiGeomMode = MultiGeomMode.valueOf(value);
        }

        if (params.contains(ChemSoftConstants.PARJOBDETAILSFILE))
        {
            String jdFile = params.getParameter(
                    ChemSoftConstants.PARJOBDETAILSFILE).getValueAsString();
            if (verbosity > 0)
            {
                System.out.println(" Job details from JD file '" 
                        + jdFile + "'.");
            }
            FileUtils.foundAndPermissions(jdFile,true,false,false);
            this.ccJob = new CompChemJob(jdFile);
        }
        else if (params.contains(ChemSoftConstants.PARJOBDETAILS))
        {
            String jdLines = params.getParameter(
                    ChemSoftConstants.PARJOBDETAILS).getValueAsString();
            if (verbosity > 0)
            {
                System.out.println(" Job details from nested parameter block.");
            }
            ArrayList<String> lines = new ArrayList<String>(Arrays.asList(
                    jdLines.split("\\r?\\n")));
            this.ccJob = new CompChemJob(lines);
        }
        else 
        {
            Terminator.withMsgAndStatus("ERROR! Unable to get job details. "
                    + "Neither '" + ChemSoftConstants.PARJOBDETAILSFILE
                    + "' nor '" + ChemSoftConstants.PARJOBDETAILS 
                    + "'found in parameters.",-1);
        }

        if (params.contains(ChemSoftConstants.PAROUTFILEROOT))
        {
            outFileNameRoot = params.getParameter(
                    ChemSoftConstants.PAROUTFILEROOT).getValueAsString();
            outFileName = outFileNameRoot + inpExtrension;
            outJDFile = outFileName + ChemSoftConstants.JDEXTENSION;
        } else {
            outFileNameRoot = FileUtils.getRootOfFileName(inGeomFile);
            outFileName = outFileNameRoot + outExtension;
            outJDFile = outFileNameRoot + ChemSoftConstants.JDEXTENSION;
            if (verbosity > 0)
            {
                System.out.println(" No '" + ChemSoftConstants.PAROUTFILEROOT
                        + "' parameter found. " + NL
                        + "Root of any output file name set to '" 
                        + outFileNameRoot + "'.");
            }
        }

        if (params.contains(ChemSoftConstants.PARCHARGE))
        {
            charge = Integer.parseInt(params.getParameter(
                    ChemSoftConstants.PARCHARGE).getValueAsString());
        } 

        if (params.contains(ChemSoftConstants.PARSPINMULT))
        {
            spinMult = Integer.parseInt(params.getParameter(
                    ChemSoftConstants.PARSPINMULT).getValueAsString());
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

        if (multiGeomMode.equals(MultiGeomMode.SingleGeom))
        {
            printInputForEachMol();
        } else {
            printInputWithMultipleGeometry();
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
    
    private void printInputForEachMol()
    {
    	if (inpGeom.size() == 1)
    	{
    		printInputForOneMol(inpGeom.get(0), outFileName, outFileNameRoot);
    	} else {
    		
            // TODO: what about files other than the .inp?
            // For instance, the XYZ file for neb-ts jobs.
            // In some cases (i.e., Orca) we can just wrote their filename 
    		// in input file, but in other cases we might need to give separate 
    		// geom files
        
	        for (int molId = 0; molId<inpGeom.size(); molId++)
	        {
	            IAtomContainer mol = inpGeom.get(molId);
	            
	            if (verbosity > 0)
	            {
	                System.out.println(" Writing input file for molecule #" 
	                        + (molId+1) + ": " 
	                		+ MolecularUtils.getNameOrID(mol));
	            }
	            
	            printInputForOneMol(mol,outFileNameRoot + "-" + molId + 
	            		inpExtrension, outFileNameRoot + "-" + molId);
	        }
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Write the input for a single chemical entity.
     * @param mol the chemical entity.
     * @param outFileName the pathname where to write.
     */
    protected abstract void printInputForOneMol(IAtomContainer mol, 
    		String outFileName, String outFileNameRoot);
    
//------------------------------------------------------------------------------
    
    /**
     * Writes the input file meant to deal with a collection of chemical 
     * entities.
     */
    private void printInputWithMultipleGeometry()
    {
        Terminator.withMsgAndStatus(" ERROR! Running "
                + "printInputWithMultipleGeometry, which should have been"
                + " overwritten by devellpers.",-1);
    }

//------------------------------------------------------------------------------

}
