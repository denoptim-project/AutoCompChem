package autocompchem.modeling.forcefield;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.openscience.cdk.AtomType;

import autocompchem.chemsoftware.tinker.TinkerForceFieldHandler;
import autocompchem.chemsoftware.vibmodule.VibModuleOutputReader;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileUtils;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.SMARTS;
import autocompchem.utils.NumberAwareStringComparator;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;
import autocompchem.worker.WorkerFactory;


/**
 * ForceFieldEditor is a toolbox for modifying force field parameters.
 *
 * @author Marco Foscato
 */

public class ForceFieldEditor extends Worker
{
    /**
     * The pathname of the input force field file
     */
    private File iFFFile;

    /**
     * The pathname of the output force field file
     */
    private File oFFFile;

    /**
     * The pathname/s of the molecular structure files
     */
    private List<String> molFiles = new ArrayList<String>();

    /**
     * The pathname/s of the vibrational analysis files
     */
    private List<String> vaFiles = new ArrayList<String>();

    /**
     * The format of vibrational analysis file
     */
    private String vaFormat = ForceFieldConstants.VIBANLVIBMODULE;

    /**
     * Flag controlling no-output mode
     */
    private boolean noOutput = false;

    /**
     * The format for input force field file
     */
    private String inFFFormat = ForceFieldConstants.FFFILETNKFORMAT;

    /**
     * The format for output
     */
    @SuppressWarnings("unused")
        private String outFormat;

    /**
     * Storage of SMARTS queries
     */
    private Map<String,String> smarts = new HashMap<String,String>();

    /**
     * Storage of options associated with SMARTS queries
     */
    private Map<String,ArrayList<String>> smartsOpts =
                                        new HashMap<String,ArrayList<String>>();

    /**
     * Label used to identify single-atom smarts in the smarts reference name.
     * WARNING! Must be consistent with that of VibModuleOutputHandler.
     */
    private static final String SUBRULELAB = "_p";

    /**
     * Root of the smarts reference names.
     * WARNING! Must be consistent with that of VibModuleOutputHandler.
     */
    private static final String MSTRULEROOT = "smarts ";

    /**
     * Unique counter for SMARTS reference names
     */
    private final AtomicInteger iNameSmarts = new AtomicInteger(0);

    /**
     * Storage of SMARTS-to-atom type rules
     */
    private Map<String,Map<String,String>> smartsToAtmTyp = 
                                       new HashMap<String,Map<String,String>>();

    /**
     * Loaded force field parameters set
     */
    private ForceFieldParamsSet ff = new ForceFieldParamsSet();
    
    /**
     * String defining the task of parametrizing force field parameters
     */
    public static final String PARAMETRIZEFORCEFIELDTASKNAME = 
    		"parametrizeForceField";

    /**
     * Task about parameterizing force field parameters
     */
    public static final Task PARAMETRIZEFORCEFIELDTASK;
    static {
    	PARAMETRIZEFORCEFIELDTASK = Task.make(PARAMETRIZEFORCEFIELDTASKNAME);
    }

//------------------------------------------------------------------------------
    
    /**
     * Constructor.
     */
    public ForceFieldEditor()
    {}
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(PARAMETRIZEFORCEFIELDTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/ForceFieldEditor.json";
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new ForceFieldEditor();
    }
//-----------------------------------------------------------------------------

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    public void initialize()
    {
    	super.initialize();

        //Get and check initial force field file
        this.iFFFile = new File(
        		params.getParameter("INPFFFILE").getValue().toString());
        FileUtils.foundAndPermissions(this.iFFFile,true,false,false);

        //Get force field format
        if (params.contains("INFFFORMAT"))
        {
            this.inFFFormat =
                        params.getParameter("INFFFORMAT").getValue().toString();
        }

        //Get the force field parameters set
        importForceField(this.iFFFile,this.inFFFormat);


        //Get and check molecular structure file (or files)
        if (params.contains("MOLFILES"))
        {
            String lst = params.getParameter("MOLFILES").getValue().toString();
            String[] files = lst.split("\\r?\\n|\\r");
            for (int i=0; i<files.length; i++)
            {
                FileUtils.foundAndPermissions(files[i],true,false,false);
                this.molFiles.add(files[i]);
            }
        }

        //Get and check vibrational analysis files
        if (params.contains("VAFILES"))
        {
            String lst = params.getParameter("VAFILES").getValue().toString();
            String[] files = lst.split("\\r?\\n|\\r");
            for (int i=0; i<files.length; i++)
            {
                FileUtils.foundAndPermissions(files[i],true,false,false);
                this.vaFiles.add(files[i]);
            }
        }

        //Import smarts
        if (params.contains("INTCOORDBYSMARTS"))
        {
            String all =
                  params.getParameter("INTCOORDBYSMARTS").getValue().toString();
            this.smarts.putAll(getNamedICSMARTS(all));
            this.smartsOpts.putAll(getOptsForNamedICSMARTS(all,this.smarts));
        }

        //Optional parameters
        if (params.contains("OUTFFFILE"))
        {
            //Get and check output file
            this.oFFFile = new File(
            		params.getParameter("OUTFFFILE").getValue().toString());
            FileUtils.mustNotExist(this.oFFFile);
        } else {
            noOutput=true;
        }

        //Get force field format
        if (params.contains("OUTFORMAT"))
        {
            if (noOutput)
            {
                String cause = "ERROR! ForceFieldEditor: OUTFORMAT defined"
                                + " while running in no-output mode";
                Terminator.withMsgAndStatus("ERROR! " + cause,-1);
            }
            this.outFormat = 
                        params.getParameter("OUTFORMAT").getValue().toString();
        }

        if (params.contains("SMARTSTOATOMTYPEMAP"))
        {
            String[] lines = params.getParameter("SMARTSTOATOMTYPEMAP").
              getValue().toString().split("\\r?\\n|\\r");
            for (int i=0; i<lines.length; i++)
            {
                String l = lines[i].trim();
                if (l.equals(""))
                {
                    continue;
                }
                String[] words = l.split("\\s+");
                if (words.length < 2)
                {
                    Terminator.withMsgAndStatus("ERROR! Expecting more than "
                         + "one space-separated words in line '" + l + "'.",-1);
                }
                if (!SMARTS.isSingleAtomSMARTS(words[0]))
                {
                    Terminator.withMsgAndStatus("ERROR! Expecting SMARTS in "
                                                      + "line '" + l + "'.",-1);
                }
                Map<String,String> opts = new HashMap<String,String>();
                for (int j=1; j<words.length; j++)
                {
                    String[] kv = words[j].split("=");
                    if (kv.length != 2)
                    {
                        Terminator.withMsgAndStatus("ERROR! Expecting syntax "
                                      + " 'key=value' in line '" + l + "'.",-1);
                    }
                    if (kv[0].toUpperCase().equals(
                            ForceFieldConstants.ATMTYPSTRMAP)
                        || kv[0].toUpperCase().equals(
                            ForceFieldConstants.ATMCLSSTRMAP))
                    {
                        opts.put(kv[0].toUpperCase(),kv[1]);
                    }
                    else
                    {
                        opts.put(kv[0],kv[1]);
                    }
                }
                smartsToAtmTyp.put(words[0],opts);
            }
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
    	if (task.equals(PARAMETRIZEFORCEFIELDTASK))
    	{
    		logger.warn("WARNING! this method is still experimental "
    				+ "and can produce unpredictable results.");
    		includeFFParamsFromVibModule();
    	} else {
    		dealWithTaskMismatch();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Imports a force field parameter set from file.
     * @param file the force field file
     * @param format the format of the force field file
     */
    private void importForceField(File file, String format)
    {
        switch (format)
        {
            case ForceFieldConstants.FFFILETNKFORMAT:
                ff = TinkerForceFieldHandler.readFromFile(file);
                break;

            default:
                Terminator.withMsgAndStatus("ERROR! Force field file format '" 
                                            + format + "' in not known. Cannot "
                                            + "import the force field.",-1);
        }        
    }

//------------------------------------------------------------------------------

    /**
     * Export the currently loaded force field parameter set into file.
     * @param file the force field file
     * @param format the format of the force field file
     */
    private void exportForceField(File file, String format)
    {
        switch (format)
        {
            case ForceFieldConstants.FFFILETNKFORMAT:
                TinkerForceFieldHandler.writeForceFieldFile(ff,file);
                break;

            default:
                Terminator.withMsgAndStatus("ERROR! Force field file format '"
                                            + format + "' in not known. Cannot "
                                            + "export the force field.",-1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Imports force constants and equilibrium values from the 
     * vibrational analysis of given molecules into a force field. Settings
     * from the constructor.
     */

    public void includeFFParamsFromVibModule()
    {
        // Check consistency
        String msg = "";
        if (molFiles.size() == 0 || vaFiles.size() == 0
            || molFiles.size() != vaFiles.size())
        {
            msg = "Inconsistent number of structures (" + molFiles.size() 
                  + ") or vibrational analysis files (" + vaFiles.size() + "). "
                  + "Check input parameters!";
            Terminator.withMsgAndStatus("ERROR! " + msg,-1);
        }
        else
        {
        	logger.debug("Importing force field parameters from "
                                   + vaFiles.size() + " vibrational analysis.");
        }

        // Get new (averaged) force field parameters from vibrational analysis
        ForceFieldParameterStats ffParStats = new ForceFieldParameterStats();
        switch (vaFormat)
        {
            case ForceFieldConstants.VIBANLVIBMODULE:
                for (int imol=0; imol<molFiles.size(); imol++)
                {
                	ParameterStorage ps = new ParameterStorage();
                	ps.setParameter("MOLFILE", molFiles.get(imol));
                	ps.setParameter("VMFILE", vaFiles.get(imol));
                	ps.setParameter(params.getParameter("INTCOORDBYSMARTS"));
                	
                	Worker w;
					try {
						w = WorkerFactory.createWorker(ps, this.getMyJob());
					} catch (ClassNotFoundException e) {
						throw new Error("Unable to make worker "
								+ "VibModuleOutputHandler");
					}
                	VibModuleOutputReader vmoh = (VibModuleOutputReader) w;
                	
                	/*
                	//TODO del
                    VibModuleOutputHandler vmoh = new VibModuleOutputHandler(
                                                             molFiles.get(imol),
                                                              vaFiles.get(imol),
                                                                         smarts,
                                                                     smartsOpts,
                                                                     verbosity);
                    */
                    //TODO-gg reactivate 
                	//ffParStats.addAllFFParams(vmoh.getFFParams());
                }
                break;

            default:
                msg = "Format '" + vaFormat + "' is not among the known "
                      + "formats of vibrational analysis files";
                Terminator.withMsgAndStatus("ERROR! " + msg,-1);
                break;
        }

        //Assign atom type/class
        String paramsLog = " Mean force field parameters (over all " 
                            + molFiles.size() + " mols): " + NL;
        for (ForceFieldParameter ffp : ffParStats.getMeanFFPar())
        {
            for (AtomType at : ffp.getAtomTypes())
            {
                String smartsKey = at.getProperty(
                            ForceFieldConstants.SMARTSQUERYATMTYP).toString();
                if (!smartsToAtmTyp.keySet().contains(smartsKey))
                {
                    logger.warn(" WARNING! No atom type/class definition for "
                    		+ "SMARTS '"
                            + smartsKey + "'. Cannot assign force field "
                            + "parameter " + ffp.toSimpleString());
                    ffp.setProperty("IGNORE","IGNORE");
                }
                else
                {
                    Map<String,String> atOpts = smartsToAtmTyp.get(smartsKey);
                    if (atOpts.keySet().contains(
                        ForceFieldConstants.ATMTYPSTRMAP))
                    {
                        at.setProperty(ForceFieldConstants.ATMTYPSTR,
                            atOpts.get(ForceFieldConstants.ATMTYPSTRMAP));
                    }
                    if (atOpts.keySet().contains(
                        ForceFieldConstants.ATMCLSSTRMAP))
                    {
                        at.setProperty(ForceFieldConstants.ATMCLSSTR,
                            atOpts.get(ForceFieldConstants.ATMCLSSTRMAP));
                    }
                }
                if (ffp.hasProperty("IGNORE"))
                {
                    break;
                }
            }
            if (!ffp.hasProperty("IGNORE"))
            {
            	paramsLog = paramsLog + "  -> "+ffp.toSimpleString() + NL;
            }
        }
        logger.info(paramsLog);
        
        //Import new parameter into the loaded FF parameters' set
        logger.fatal("ERROR: implementation ot complete! Use at your own risk!");


        //Write results
        if (!noOutput)
        {
            exportForceField(this.oFFFile,this.inFFFormat);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Reads SMARTS for defining internal coordinates.
     * This methos collects all non-single-atom SMARTS strings found in a
     * given string. The string is
     * assumend to contain one or more lines (i.e., newline-character separated)
     * and each line to contain from 2 to 4 space-separated single-atom SMARTS.
     * @param allLines the string collecting all lines and including newline
     * characters
     * @return the map of all named smarts. Naming is based on incremental and
     * unique indexing where a major index is used to identify sets of SMARTS
     * red from the same line, and anothe idex is used to identify the
     * order of the SMARTS red in the same line.
     */

//TODO: move this method to utils for SMARTS, and replace it in other classes
// line NWChemInputWriter

    private Map<String,String> getNamedICSMARTS(String allLines)
    {
        Map<String,String> map = new HashMap<String,String>();
        logger.debug("Importing SMARTS to identify ICs");
        String[] lines = allLines.split("\\r?\\n");
        int ii = -1;
        for (int i=0; i<lines.length; i++)
        {
            if (lines[i].trim().equals(""))
            {
                continue;
            }
            // This allows to retrace the exact orded in which lines are
            // given, yet without using the line number as index and allowing
            // to store multiple blocks of SMARTS queries in the same map
            ii = iNameSmarts.getAndIncrement();
            String masterName = MSTRULEROOT + ii;

            String[] parts = lines[i].split("\\s+");
            int jj = -1;
            for (int j=0; j<parts.length; j++)
            {
                String singleSmarts = parts[j].trim();

                // Ignore any string that is not a single-atom SMARTS
                if (singleSmarts.equals("") ||
                    !SMARTS.isSingleAtomSMARTS(singleSmarts))
                {
                    continue;
                }

                if (jj > 3)
                {
                    Terminator.withMsgAndStatus("ERROR! More than 4 atomic "
                               + "SMARTS for IC-defining SMARTS rule "
                               + ii + " (last SMARTS:" + singleSmarts + "). "
                               + "These rules must identify N-tuples of "
                               + "atoms, where N=2,3,4. Check the input.",-1);
                }
                jj++;
                String childName = masterName + SUBRULELAB + jj;
                map.put(childName,singleSmarts);
            }
            if (jj < 1)
            {
                Terminator.withMsgAndStatus("ERROR! Less than 2 atomic "
                               + "SMARTS for IC-defining SMARTS rule "
                               + ii + ". These rules must identify N-tuples of "
                               + "atoms, where N=2,3,4. Check input.",-1);
            }
        }
        return map;
    }

//------------------------------------------------------------------------------

    /**
     * Read options and values associated with SMARTS queries.
     * This methos collects all non-single-atom SMARTS strings found in a
     * given string. The string is
     * assumend to contain one or more lines (i.e., newline-character separated)
     * and each line to contain from 2 to 4 space-separated single-atom SMARTS.
     * @param allLines the string collecting all lines and including newline
     * characters
     * @param smarts the map of previously red SMARTS queries for which this
     * method is collecting the options
     * @return the map of all named details. Naming is based on incremental and
     * unique indexing where an integer index is used to identify the list of
     * strings red from the same line.
     */

//TODO: move this method to utils for SMARTS, and replace it in other classes
// line NWChemInputWriter

    private Map<String,ArrayList<String>> getOptsForNamedICSMARTS(
                                     String allLines, Map<String,String> smarts)
    {
        ArrayList<String> sortedMasterNames = getSortedSMARTSRefNames(smarts);
        Map<String,ArrayList<String>> map =
        		new HashMap<String,ArrayList<String>>();
        logger.debug("Importing options for IC-identifying SMARTS");
        String[] lines = allLines.split("\\r?\\n");
        int ii=-1;
        for (int i=0; i<lines.length; i++)
        {
            if (lines[i].trim().equals(""))
            {
                continue;
            }
            ii++;
            String[] parts = lines[i].split("\\s+");
            ArrayList<String> lstDetails = new ArrayList<String>();
            for (int j=0; j<parts.length; j++)
            {
                String str = parts[j].trim();

                // Ignore single-atom SMARTS
                if (str.equals("") || SMARTS.isSingleAtomSMARTS(str))
                {
                    continue;
                }

                lstDetails.add(str);
            }
            map.put(sortedMasterNames.get(ii),lstDetails);
        }
        return map;
    }

//------------------------------------------------------------------------------

    /**
     * get the sorted list of master names
     */

    private ArrayList<String> getSortedSMARTSRefNames(
                                                      Map<String,String> smarts)
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

//-----------------------------------------------------------------------------

}
