package autocompchem.chemsoftware.orca;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.Directive;
import autocompchem.chemsoftware.DirectiveComponentType;
import autocompchem.chemsoftware.DirectiveData;
import autocompchem.chemsoftware.Keyword;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.datacollections.Parameter;
import autocompchem.chemsoftware.ChemSoftConstants.CoordsType;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.molecule.MolecularUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.utils.NumberAwareStringComparator;
import autocompchem.utils.StringUtils;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Writes input files for software ORCA.
 *
 * @author Marco Foscato
 */

//TODO: write doc

public class OrcaInputWriter extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.PREPAREINPUTORCA)));
    
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
     * Unique counter for SMARTS reference names
     */
    private final AtomicInteger iNameSmarts = new AtomicInteger(0);

    /**
     * Storage of SMARTS queries
     */
    private Map<String,String> smarts = new HashMap<String,String>();

    /**
     * Label used to identify single-atom smarts in the smarts reference name
     */
    private static final String SUBRULELAB = "_p";

    /**
     * Root of the smarts reference names
     */
    private static final String MSTRULEROOT = "smarts ";
    
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
     * Object containing the details on the Orca job
     */
    private CompChemJob ccJob;

    /** 
     * Verbosity level
     */
    private int verbosity = 1;

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
            		ChemSoftConstants.PARVERBOSITY).getValue().toString();
            this.verbosity = Integer.parseInt(str);

            if (verbosity > 0)
                System.out.println(" Adding parameters to OrcaInputWriter");
        }

        if (params.contains(ChemSoftConstants.PARGEOMFILE))
        {
	        this.inGeomFile = params.getParameter(
	        		ChemSoftConstants.PARGEOMFILE).getValue().toString();
	        
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
	        	Terminator.withMsgAndStatus("ERROR! Format of file '" + inGeomFile 
	        			+ "' not recognized!",-1);
	        }
	        else
	        {
	        	Terminator.withMsgAndStatus("ERROR! Format of file '" + inGeomFile 
	        			+ "' not recognized!",-1);
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
                Terminator.withMsgAndStatus("ERROR! OrcaInputWriter "
                    + "can read multi-geometry input files "
                    + "only when starting from XYZ of SDF files. Make "
                    + "sure file '" + inGeomFile +"' has proper "
                    + "format and extension.", -1);
            }
        }

        if (params.contains(ChemSoftConstants.PARMULTIGEOMMODE))
        {
        	String value = 
                    params.getParameter(ChemSoftConstants.PARMULTIGEOMMODE
                    		).getValue().toString();
            this.multiGeomMode = MultiGeomMode.valueOf(value);
        }

        if (params.contains(ChemSoftConstants.PARJOBDETAILS))
        {
            String jdFile = params.getParameter(
            		ChemSoftConstants.PARJOBDETAILS).getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Job details from JD file '" 
                		+ jdFile + "'.");
            }
            FileUtils.foundAndPermissions(jdFile,true,false,false);
            this.ccJob = new CompChemJob(jdFile);
        } 
        else 
        {
            Terminator.withMsgAndStatus("ERROR! Unable to get job details. "
            		+ "No 'JOBDETAILS' found in parameters.",-1);
        }

        if (params.contains(ChemSoftConstants.PAROUTFILEROOT))
        {
            outFileName = params.getParameter(
            		ChemSoftConstants.PAROUTFILEROOT).getValue().toString();
            outJDFile = outFileName + ChemSoftConstants.JDEXTENSION;
        } else {
            outFileNameRoot = FileUtils.getRootOfFileName(inGeomFile);
            outFileName = outFileNameRoot + OrcaConstants.INPEXTENSION;
            outJDFile = outFileNameRoot + ChemSoftConstants.JDEXTENSION;
            if (verbosity > 0)
            {
                System.out.println(" No '" + ChemSoftConstants.PAROUTFILEROOT
                		+ "' parameter found. "
                        + "Root of any output file name set to '" 
                		+ outFileNameRoot + "'.");
            }
        }

        if (params.contains(ChemSoftConstants.PARCHARGE))
        {
            charge = Integer.parseInt(params.getParameter(
            		ChemSoftConstants.PARCHARGE).getValue().toString());
        } 

        if (params.contains(ChemSoftConstants.PARSPINMULT))
        {
            spinMult = Integer.parseInt(params.getParameter(
            		ChemSoftConstants.PARSPINMULT).getValue().toString());
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
          case PREPAREINPUTORCA:
        	  printInput();
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
     * get the sorted list of master names 
     */

    private ArrayList<String> getSortedSMARTSRefNames(Map<String,String> smarts)
    {
        ArrayList<String> sortedMasterNames = new ArrayList<String>();
        for (String k : smarts.keySet())
        {
            String[] p = k.split(SUBRULELAB);
            if (!sortedMasterNames.contains(p[0]))
            {
                sortedMasterNames.add(p[0]);
            }
        }
        Collections.sort(sortedMasterNames, new NumberAwareStringComparator());
        return sortedMasterNames;
    }

//------------------------------------------------------------------------------

    /**
     * Write input file according to any settings that have been given to this
     * input writer.
     */

    private void printInput()
    {
        if (multiGeomMode.equals(MultiGeomMode.SingleGeom))
        {
        	printInputForEachMol();
        }
        else
        {
        	printInputWithMultipleGeometry();
        }
    }
    
//------------------------------------------------------------------------------
    
    private void printInputForEachMol()
    {
    	// TODO: what about files other than the .inp?
    	// For instance, the XYZ file for neb-ts jobs
    	
    	for (int molId = 0; molId<inpGeom.size(); molId++)
    	{
    		IAtomContainer mol = inpGeom.get(molId);
    		
    		CompChemJob molSpecJob = ccJob.clone();
    		
    		//TODO logging msg
    		
    		Parameter pathnamePar = new Parameter(
    				ChemSoftConstants.PAROUTFILEROOT,outFileNameRoot);
    		if (molId>0)
    		{
    			outFileName = outFileNameRoot + "-" + molId
    					+ OrcaConstants.INPEXTENSION;
    			pathnamePar = new Parameter(
        				ChemSoftConstants.PAROUTFILEROOT,
        				outFileNameRoot + "-" + molId);
    		}
    		molSpecJob.setParameter(pathnamePar);
    		for (Job subJob : molSpecJob.getSteps())
    		{
    			subJob.setParameter(pathnamePar);
    		}
    		
    		// These take care also of the sub-jobs/directives
    		molSpecJob.processDirectives(mol);
    		molSpecJob.sortDirectivesBy(new OrcaDirectiveComparator());
    		
        	// WARNING: for now we are not considering the possibility of having
        	// both directives AND sub jobs. So it's either one or the other
    		
    		if (molSpecJob.getNumberOfSteps()>0)
    		{
    			for (int i=0; i<molSpecJob.getNumberOfSteps(); i++)
    			{
    				CompChemJob step = (CompChemJob) molSpecJob.getStep(i);
    				ArrayList<String> lines = new ArrayList<String>();
    				lines.addAll(getTextForInput(step));
    				if (i<(molSpecJob.getNumberOfSteps()-1))
    				{
    					lines.add(OrcaConstants.JOBSEPARATOR);
    				}
    				IOtools.writeTXTAppend(outFileName, lines, true);
    			}
    		} else {
				ArrayList<String> lines = new ArrayList<String>();
				lines.addAll(getTextForInput(molSpecJob));
				IOtools.writeTXTAppend(outFileName, lines, true);
    		}
    	}
    }
    
//------------------------------------------------------------------------------
    
    private ArrayList<String> getTextForInput(CompChemJob job)
    {
    	ArrayList<String> lines = new ArrayList<String>();
		Iterator<Directive> it = job.directiveIterator();
		while (it.hasNext())
		{
			Directive d = it.next();
			lines.addAll(getTextForInput(d,true));
		}
    	return lines;
    }
    
//------------------------------------------------------------------------------
    
    private ArrayList<String> getTextForInput(Directive d, boolean outmost)
    {	
    	ArrayList<String> lines = new ArrayList<String>();
    	
    	String dirName = d.getName();
    	if (dirName.startsWith("#"))
    	{
			// This is a comment line for the Orca input.
			// There can be one or more such lines, each strictly filling
			// ONE single line.
			for (Keyword k : d.getAllKeywords())
			{
				if (k.isLoud())
				{
					lines.add("#" + k.getName() + " "
						+ StringUtils.mergeListToString(k.getValue()," "));
				} else
				{
					lines.add("#" + StringUtils.mergeListToString(
							k.getValue()," "));	
				}
			}
			// Sub directives and DirectiveData are not suitable for Orca's
			// "keyword line", so we do not expect them
			String errTail = "found within a comment line. This "
					+ "is unexpected, but if you see an use for it, "
					+ "then, please, implement the conversion to "
					+ "input file text lines.";
			if (d.getAllSubDirectives().size()>0)
			{
				Terminator.withMsgAndStatus("ERROR! Unexpected sub "
						+ "directives "+errTail, -1);
			}
			if (d.getAllDirectiveDataBlocks().size()>0)
			{
				Terminator.withMsgAndStatus("ERROR! Unexpected directive "
						+ "data blocks "+errTail, -1);
			}
			return lines;
    	}
    	
    	// Purge dirName from useless parts
    	if (dirName.startsWith("%"))
    	{
    		//OK, I see what you want to do, but for further processing 
    		// we get rid of the '%'. We put it back anyway for all standard
    		// directives
    		dirName = dirName.substring(1).trim();
    	} 
    	
    	
		switch (dirName.toUpperCase())
		{
			case ("!"):
			{
				// This is called "the keyword line" in ORCA manual.
				// There can be one or more such lines, each strictly filling
				// ONE single line.
				String line = "!";
				for (Keyword k : d.getAllKeywords())
				{
					if (k.isLoud())
					{
						line = line + " " + k.getName() + " "
							+ StringUtils.mergeListToString(k.getValue()," ");
					} else
					{
						line = line + " " + StringUtils.mergeListToString(
								k.getValue()," ");
					}
				}
				// Sub directives and DirectiveData are not suitable for Orca's
				// "keyword line", so we do not expect them
				String errTail = "found in Orca's job details within the '!' "
						+ "keyword line. This is unexpected, "
						+ "but if you see an use for it, "
						+ "then, please, implement the conversion to "
						+ "input file text lines.";
				if (d.getAllSubDirectives().size()>0)
				{
					Terminator.withMsgAndStatus("ERROR! Unexpected sub "
							+ "directives "+errTail, -1);
				}
				if (d.getAllDirectiveDataBlocks().size()>0)
				{
					Terminator.withMsgAndStatus("ERROR! Unexpected directive "
							+ "data blocks "+errTail, -1);
				}
				lines.add(line);
				break;
			}
			
			case ("*"):
			{
				lines.addAll(getTextForCoordsBlock(d,true));
				break;
			}
			
			case ("COORDS"):
			{
				lines.addAll(getTextForCoordsBlock(d,false));
				break;
			}
			
			default:
			{
				boolean needEnd = false;
				String line = "";
				if (outmost)
				{
					line = "%";
				}
				line = line + dirName + " ";
				
				for (Keyword k : d.getAllKeywords())
				{
					if (k.isLoud())
					{
						line = line + " " + k.getName() + " "
							+ StringUtils.mergeListToString(k.getValue()," ");
					} else {
						line = line + " "
							+ StringUtils.mergeListToString(k.getValue()," ");
					}
				}
				lines.add(line);
				
				for (Directive sd1 : d.getAllSubDirectives())
				{
					for (String innerLine : getTextForInput(sd1,false))
					{
						lines.add(OrcaConstants.INDENT + innerLine);
						needEnd = true;
					}
				}
				
				for (DirectiveData dd : d.getAllDirectiveDataBlocks())
				{
					for (String innerLine : dd.getLines())
					{
						lines.add(OrcaConstants.INDENT + innerLine);
						needEnd = true;
					}
				}
				if (needEnd)
				{
					lines.add("end");
				}
			}
		}
    	return lines;
    }
    
//------------------------------------------------------------------------------
    
    private ArrayList<String> getTextForCoordsBlock(Directive d, 
    		boolean useStar)
    {
    	ArrayList<String> lines = new ArrayList<String>();
		String line = "";
		if (useStar)
		{
			line = "*";
		} else {
			line = "%coords";
		}
		
		// Ensure we have the right keyword 
		if (d.hasComponent(ChemSoftConstants.DIRDATAGEOMETRY, 
				DirectiveComponentType.DIRECTIVEDATA))
		{
			DirectiveData dd = d.getDirectiveData(
					ChemSoftConstants.DIRDATAGEOMETRY);
			switch (dd.getType())
			{
				//TODO:
				/*
				case ZMATRIX:
				{
					break;
				}
				*/
				case IATOMCONTAINER:
				default:
				{
					d.setKeyword(new Keyword(ChemSoftConstants.PARCOORDTYPE,
			        		false, CoordsType.XYZ.toString()));
					break;
				}
			}
		}
		
		d.sortKeywordsBy(new ComparatorKeysCoordDirective());
		for (Keyword k : d.getAllKeywords())
		{
			if (k.isLoud())
			{
				line = line + " " + k.getName() + " "
					+ StringUtils.mergeListToString(k.getValue()," ");
			} else {
				line = line + " "
					+ StringUtils.mergeListToString(k.getValue()," ");
			}
		}
		lines.add(line);
		
		for (Directive sd1 : d.getAllSubDirectives())
		{
			for (String innerLine : getTextForInput(sd1,false))
			{
				lines.add(OrcaConstants.INDENT + innerLine);
			}
		}
		
		for (DirectiveData dd : d.getAllDirectiveDataBlocks())
		{
			if (dd.getName().equals(ChemSoftConstants.DIRDATAGEOMETRY))
			{
				Object o = dd.getValueAsObjectSubclass();
				switch (dd.getType())
				{
					case IATOMCONTAINER:
					{
						IAtomContainer mol = (IAtomContainer) o;
						for (IAtom atm : mol.atoms())
						{
							Point3d p3d = MolecularUtils.getCoords3d(atm);
							lines.add(OrcaConstants.INDENT 
									+ String.format(" %3s",atm.getSymbol())
									+ String.format(" %10.5f",p3d.x)
									+ String.format(" %10.5f",p3d.y)
									+ String.format(" %10.5f",p3d.z));
						}
						break;
					}
					
					case ZMATRIX:
					{
						//TODO
						Terminator.withMsgAndStatus("ERROR! Writing of "
								+ "ZMatrix in Orca input file is not yet"
								+ "implemented... sorry!",-1);
						break;
					}
					
					default:
					{	
						Terminator.withMsgAndStatus("ERROR! Unable to "
								+ "understand type of geometry '" + 
								dd.getType() + "' in OrcaInputWriter.",-1);
						break;
					}
				}
				
			} else {
				for (String innerLine : dd.getLines())
				{
					lines.add(OrcaConstants.INDENT + innerLine);
				}
			}
		}

		if (useStar)
		{
			lines.add("*");
		} else {
			lines.add("end");
		}
    	return lines;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sorts keywords consistently with the expectations of the %coords input
     * block: first type of coordinates, then charge, then spin multiplicity.
     */
    private class ComparatorKeysCoordDirective implements Comparator<Keyword>
    {
        @Override
        public int compare(Keyword a, Keyword b)
        {           
            String aName = a.getName();
            String bName = b.getName();
            int intA = 0;
            int intB = 0;
            
            if (aName.toUpperCase().equals(ChemSoftConstants.PARCOORDTYPE))
            {
            	intA = -3;
            }
            if (bName.toUpperCase().equals(ChemSoftConstants.PARCOORDTYPE))
            {
            	intB = -3;
            }
            
            if (aName.toUpperCase().equals(ChemSoftConstants.PARCHARGE))
            {
            	intA = -2;
            }
            if (bName.toUpperCase().equals(ChemSoftConstants.PARCHARGE))
            {
            	intB = -2;
            }
            
            if (aName.toUpperCase().equals(ChemSoftConstants.PARSPINMULT))
            {
            	intA = -1;
            }
            if (bName.toUpperCase().equals(ChemSoftConstants.PARSPINMULT))
            {
            	intB = -1;
            }
            
            //add any other priority rules go here... 
            //but now there seems to be no more.
            
            return Integer.compare(intA,intB);
        }
    }
    
//------------------------------------------------------------------------------
    
    private void printInputWithMultipleGeometry()
    {
    	Terminator.withMsgAndStatus(" ERROR! Running "
    			+ "printInputWithMultipleGeometry, which should have been"
    			+ " overwritten by devellpers.",-1);
    }

//------------------------------------------------------------------------------

    /**
     * Return true if the charge or the spin are overwritten according to the
     * IAtomContainer properties "CHARGE" and "SPIN_MULTIPLICITY"
     * @param mol the molecule from which we get charge and spin
     */

    private void chargeOrSpinFromMol(IAtomContainer mol)
    {
        boolean res = false;

        String str = " Using molecular structure file to set charge and "
        		+ "spin multiplicity." + System.getProperty("line.separator")
        		+ " From c = " + charge + " and s.m. = " + spinMult;

        if (MolecularUtils.hasProperty(mol, ChemSoftConstants.PARCHARGE))
        {
            res = true;
            charge = Integer.parseInt(mol.getProperty(
            		ChemSoftConstants.PARCHARGE).toString());
        }

        if (MolecularUtils.hasProperty(mol, ChemSoftConstants.PARSPINMULT))
        {
            res = true;
            spinMult = Integer.parseInt(mol.getProperty(
            		ChemSoftConstants.PARSPINMULT).toString());
        }

        if (verbosity > 0)
        {
            if (res)
            {
                System.out.println(str + " to c = " + charge + " and s.m. = "
                                + spinMult);
            }
        }
    }

//------------------------------------------------------------------------------

}
