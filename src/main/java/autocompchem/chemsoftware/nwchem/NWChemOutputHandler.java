package autocompchem.chemsoftware.nwchem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;

import autocompchem.chemsoftware.errorhandling.ErrorManager;
import autocompchem.chemsoftware.errorhandling.ErrorMessage;
import autocompchem.constants.ACCConstants;
import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.modeling.compute.CompChemComputer;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.run.Terminator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Reader and analyzer of NWChem Output files. 
 * 
 * @author Marco Foscato
 */

public class NWChemOutputHandler extends Worker
{
    /**
     * Name of the output file from NWChem: the input of this class
     */
    private String inFile;

    /**
     * Name of the output file for tasks run by this worker
     */
    private String outFile;

    /**
     * Format for molecular structure output
     */
    private String outFormat = "XYZ";

    /**
     * Flag imposing to print last geometry
     */
    private boolean printLastGeom = false;

    /**
     * Flag requesting to print vibrational modes
     */
    private boolean printVibModes = false;

    /**
     * Selected vibrational modes
     */
    private List<Integer> selectedVibModes;

    /**
     * Pathname where to print vibrational modes
     */
    private String outFileVibModes;

    /**
     * List of known errors messages
     */
    private List<ErrorMessage> errorDef;

    /**
     * Verbosity level
     */
    private int verbosity = 1;

    /**
     * Total number Steps found (1-to-n) in the output from NWChem
     */
    private int numSteps;

    /**
     * Flag recording normal termination of the NWChem job
     */
    private boolean normalTerminated;

    /**
     * Flag recording whether the error is understood
     */
    private boolean errorIsDecoded;

    /**
     * The class corresponding to the error found in the NWChem file
     */
    private ErrorMessage actualEM;

    /**
     * Counters found and current value
     */
    private Map<String,Integer> counters = new HashMap<String,Integer>();

    /**
     * Flag requiring extraction of the energy
     */
    private boolean analyseEnergy = false;

    /**
     * Flag requiring analysis of imaginary frequencies
     */
    private boolean checkTS = false;

    /**
     * Flag requiring collection of all vibrational modes 
     */
    private boolean readFrequencies = false;

    /**
     * Data structure containing the vibrational modes as a list
     */
    private List<List<Double>> vibModes;

    /**
     * Flag requiring calculation of vibrational S by quasi-harmonic approx.
     */
    private boolean calcQHarmVibS = false;

    /**
     * Array of vibrational modes read from NWChem output
     */
    private List<Double> projFrequencies = new ArrayList<Double>();

    /**
     * Lowest value for non-zero frequencies
     */
    private double lowestFreq = NWChemConstants.MINFREQ;

    /**
     * Lowest value for non-zero frequencies
     */
    private double qhThrsh = 0.0d;

    /**
     * Lowest value for imaginary modes
     */
    private double imThrsh = 0.0d;

    /**
     * Results of analysis
     */
    private String analysisResult = "";

    /**
     * Template for connectivity
     */
    private IAtomContainer connectivityTemplate;

    /**
     * Flag controlling definition of connectivity from template
     */
    private boolean useTemplateConnectivity = false;

//------------------------------------------------------------------------------
    /**
     * Constructor.
     */
    public NWChemOutputHandler()
    {}


  //------------------------------------------------------------------------------

        @Override
        public Set<TaskID> getCapabilities() {
            return Collections.unmodifiableSet(new HashSet<TaskID>(
                 Arrays.asList()));
        }

  //------------------------------------------------------------------------------

        @Override
        public Worker makeInstance(Object... args) {
            return new NWChemOutputHandler();
        }
        
      //------------------------------------------------------------------------------

        @Override
        public String getKnownInputDefinition() {
            return "";
        }
//-----------------------------------------------------------------------------

//TODO move to class doc
    /**
     * Constructs an NWChemOutputHandler specifying the parameters taken from 
     * formatted file or generated by other tools.
     * All the parameters must be in the form specified as follows:
     * <ul>
     * <li>
     * <b>INFILE</b>: path or name of the output from NWChem 
     * (i.e., name.out).
     * </li>
     * <li>
     * (optional)<b>NWCHEMERRORS</b>: path to the folder storing NWChem known
     *  errors.
     * </li>
     * <li>
     * (optional) <b>JOBDETAILS</b>: formatted text file defining all
     * the details of a {@link NWChemJob}. Usually the jobdetails txt
     * file is the file used to generate the input file (name.nw) for
     * NWChem (see {@link NWChemInputWriter2}).
     * </li>
     * <li>
     * (optional) <b>OUTFILE</b>: name of the file where output molecular
     * structure will be saved. If multiple output file are produced the
     * string given via this option will be used a root of all output file names
     * </li>
     * <li>
     * (optional) <b>OUTFORMAT</b>: (default XYZ): 
     * format for molecular structure file.
     * Currently available options: XYZ, SDF, both XYZ and SDF.
     * </li>
     * <li>
     * (optional) <b>PRINTLASTGEOMETRY</b>: imposes to print the last geometry
     * after evaluation of an output file.
     * </li>
     * <li>
     * (optional) <b>PRINTVIBMODES</b>: request to extract vibrational modes.
     * The value contains first the list of modes to print (0-based intenger),
     * and last the pathname of the XYZ file to be used for this purpose.
     * </li>
     * <li>
     * (optional) <b>TEMPLATECONNECTIVITY</b>: pathname of an SDF file that
     * will be used as template to define the connectivity when writing the
     * last geometry to an SDF file. This copies all the connectivity table
     * without considering the actual geometry in the NWChem job.
     * (optional) <b>ANALYSE</b>: types of analysis to perform. Acceptable 
     * values are:
     * <ul>
     * <li><i>ENERGY</i>: extracts the energy and, if available, thermochemical
     * corrections;
     * </li>
     * <li><i>IMGFREQ</i>: checks for imaginary frequencies (up to 5) and
     * identifyes the state as minimum, transition state, or higher order
     * saddle point.
     * </li>
     * </ul>
     * <li>
     * (optional) <b>LOWESTFREQ</b>: overwrites the default lowest (cm-1) 
     * frequencies accepted: values below the one given will be considered zero.
     * </li>
     * <li>
     * <b>VERBOSITY</b>: verbosity level.
     * </li>
     * </ul>
     * 
     * @param params object <code>ParameterStorage</code> containing all the
     * parameters needed
     */
/*
    public NWChemOutputHandler(ParameterStorage params) 
    {
    */

    /**
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
        //Define verbosity
        String vStr = params.getParameter("VERBOSITY").getValue().toString();
        this.verbosity = Integer.parseInt(vStr);

        if (verbosity > 0)
            System.out.println(" Adding parameters to NWChemOutputHandler");

        //Get and check the input file (which is an output from NWChem)
        this.inFile = params.getParameter("INFILE").getValue().toString();
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

        //Get and check the list of known errors
        if (params.contains("NWCHEMERRORS"))
        {
            String errDefPath = 
                      params.getParameter("NWCHEMERRORS").getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Importing known errors from "+ errDefPath);
            }
            FileUtils.foundAndPermissions(errDefPath,true,false,false);
            this.errorDef = ErrorManager.getAll(errDefPath);
            if (verbosity > 0)
            {
                System.out.println(" Imported " + this.errorDef.size() 
                                                             + " known errors");
            }
        }

        //Get and check the output file
        if (params.contains("OUTFILE"))
        {
            this.outFile = 
                         params.getParameter("OUTFILE").getValue().toString();
            FileUtils.mustNotExist(this.outFile);
        } 
        else
        {
            this.outFile = FileUtils.getRootOfFileName(this.inFile);
        }

        if (params.contains("OUTFORMAT"))
        {
            this.outFormat =
                         params.getParameter("OUTFORMAT").getValue().toString();
        }

        if (params.contains("PRINTLASTGEOMETRY"))
        {
            this.printLastGeom = true;
        }

        if (params.contains("PRINTVIBMODES"))
        {
            this.printVibModes = true;
            String s = 
                params.getParameter("PRINTVIBMODES").getValue().toString();
            String[] p = s.split("\\s+");
            this.selectedVibModes = new ArrayList<Integer>();
            for (int i=0; i<(p.length-1); i++)
            {
                String w = p[i];
                int val = 0;
                try
                {
                    val = Integer.parseInt(w);
                } 
                catch (Throwable t)
                {
                    Terminator.withMsgAndStatus("ERROR! Cannot convert '" + w 
                        + "' to an integer. Check selection of vibrational "
                        + "modes in PRINTVIBMODES.", -1); 
                }
                if (val < 0)
                {
                    Terminator.withMsgAndStatus("ERROR! Negative number "
                        + "cannot be used to select a vibrational mode.",-1);
                }
                this.selectedVibModes.add(val);
            }
            this.outFileVibModes = p[p.length-1];
            FileUtils.mustNotExist(this.outFileVibModes);
        }

        if (params.contains("TEMPLATECONNECTIVITY"))
        {
            this.useTemplateConnectivity = true;
            String pathname =
              params.getParameter("TEMPLATECONNECTIVITY").getValue().toString();
            File fileWithTplt = new File(pathname);
            FileUtils.foundAndPermissions(pathname,true,false,false);
                this.connectivityTemplate = IOtools.readSDF(fileWithTplt).get(0);
        }

        if (params.contains("ANALYSE"))
        {
            String s = params.getParameter("ANALYSE").getValue().toString();
            String[] p = s.split("\\s+");
            for (String w : p)
            {
                if (w.equals("ENERGY"))
                {
                    this.analyseEnergy = true;
                }
                if (w.equals("IMGFREQ"))
                {
                    this.readFrequencies = true;
                    this.checkTS = true;
                }
            }
        }

        if (params.contains("LOWESTFREQ"))
        {
            this.lowestFreq = Double.parseDouble(
                       params.getParameter("LOWESTFREQ").getValue().toString());
        }
        
        if (params.contains("QUASIHARM"))
        {
            this.readFrequencies = true;
            this.calcQHarmVibS = true;
            this.qhThrsh = Double.parseDouble(
                       params.getParameter("QUASIHARM").getValue().toString());
        }        

        if (params.contains("IGNOREIM"))
        {
            this.imThrsh = Double.parseDouble(
                        params.getParameter("IGNOREIM").getValue().toString());
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
          case ANALYSENWCHEMOUTPUT:
        	  evaluateOutputNWChem();
              break;
              
          case EXTRACTOPTGEOMSFROMNWCHEMOUTPUT:
        	  printOptTrajectory();
              break;
              
          case EXTRACTTRAJECTORYFROMNWCHEMOUTPUT:
        	  printTrajectory();
              break;
              
          case EXTRACTLASTGEOMETRYFROMNWCHEMOUTPUT:
        	  printLastOutputGeometry();
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
     * Analyses the input file and identify its properties: 
     * termination status, number
     * steps, type of error, and so on.
     * In case of normal termination it can extract the last geometry. To this
     * end you must provide the OUTFILE and OUTFORMAT options 
     * when generating the handler.
     */

    public void evaluateOutputNWChem()
    {
        //Read the outfile and collect counts and line numbers
        List<String> patterns = new ArrayList<String>();
        patterns.add(NWChemConstants.OUTINITSTR);
        patterns.add(NWChemConstants.OUTNORMALENDSTR);
        List<List<Integer>> countsAndLineNum = 
                                           FileAnalyzer.count(inFile,patterns);
        int indexOfCounts = countsAndLineNum.size() - 1;
        List<Integer> counts = countsAndLineNum.get(indexOfCounts);
        List<List<Integer>> lineNums = 
                                            new ArrayList<List<Integer>>();
        //Keep all but the last array (which is the count)
        for (int i=0; i<(countsAndLineNum.size() - 1); i++)
            lineNums.add(countsAndLineNum.get(i));

        int preNumSteps = counts.get(0);
        int numCitationMsgs = counts.get(1);
        if (numCitationMsgs > 0)
        {
            numSteps = preNumSteps - 1;
            normalTerminated = true;
            if (verbosity > -1)
            {
                System.out.println(" NWChem output file contains: "
                                                         + numSteps + " steps");
                System.out.println(" The NWChem job has terminated normally!");
            }
            if (printLastGeom)
            {
                printLastOutputGeometry();
            }
        }
        else
        {
            numSteps = preNumSteps;
            normalTerminated = false;
            if (verbosity > -1)
            {
                System.out.println(" NWChem output file contains: "
                                                         + numSteps + " steps");
                System.out.println(" The job did not terminate normally ");
            }
        }

        //Get the last part of the .out file (to get a smaller array)
        int lastInit = 0;
        if (numSteps > 0)
        {
            lastInit = lineNums.get(0).get(numSteps - 1 );
        }
        if (verbosity > 2 ) 
        {
            System.out.println(" Analyzing tail (start at line "+lastInit+")");
        }
        List<String> tail = IOtools.tailFrom(new File(inFile),lastInit);

        //Error identififcation
        if (!normalTerminated && errorDef != null)
        {
            if (verbosity > 0)
            {
                System.out.println(" Attempting identification of error");
                if (verbosity > 1)
                {
                    System.out.println(" Tail (i.e., last task): "+tail.size());
                }
            }

            //Compare with known errors
            identifyErrorMessage(tail);

            //Report
            if (verbosity > 0)
            {
                if (errorIsDecoded)
                {
                    System.out.println(" NWChem Error Recognized: Step "  
                                        + numSteps
                                        + " returned Error Message: "
                                        + actualEM.getName());
                } else {
                    System.out.println(" NWChem Error NOT Recognized. Please "
                                + "identify the error by hand and then add a "
                                + "new ErrorMessage to the list of known "
                                + "errors.");
                }
            }
        } 
        else 
        {
            patterns = new ArrayList<String>();
            if (analyseEnergy)
            {
                patterns.add(NWChemConstants.OUTTOTDFTENERGY);
                patterns.add(NWChemConstants.OUTCORRH);
                patterns.add(NWChemConstants.OUTTOTS);
                patterns.add(NWChemConstants.OUTTEMP);
                if (calcQHarmVibS)
                {
                    patterns.add(NWChemConstants.OUTPROJFREQ);
                    patterns.add(NWChemConstants.OUTTRAS);
                    patterns.add(NWChemConstants.OUTROTS);
                    patterns.add(NWChemConstants.OUTVIBS);
                }
            }

            if (checkTS)
            {
                patterns.add(NWChemConstants.OUTPROJFREQ);
            }

//TODO use method in IOtools
// Also, now the condition is always satisfied, but there might be cases
// chere we do not need to do it

            if (analyseEnergy || checkTS)
            {
                Map<String,List<Integer>> matchesMap =
                                       new HashMap<String,List<Integer>>();
                for (int i=0; i<tail.size(); i++)
                {
                    String line = tail.get(i);
                    for (String pattern : patterns)
                    {
                        if (verbosity > 3)
                        {
                            System.out.println("Comparing _" + pattern + "_:_" 
                                                + line+"_");
                        }
                        if (line.matches(pattern))
                        {
                            if (verbosity > 2)
                            {
                                System.out.println("Matched: " + pattern  +" _" 
                                                + line+"_");
                            }
                            if (matchesMap.containsKey(pattern))
                            {
                                matchesMap.get(pattern).add(i);
                            }
                            else
                            {
                                List<Integer> lst = 
                                                       new ArrayList<Integer>();
                                lst.add(i);
                                matchesMap.put(pattern,lst);
                            }
                            break;
                        }
                    }
                }

                if (verbosity > 2)
                {
                    System.out.println("Analysis has found lines: "+matchesMap);
                }
                String res = "Analysis: ";

                List<Double> imgFreqs = new ArrayList<Double>();
                if (readFrequencies)
                {
                    String key = NWChemConstants.OUTPROJFREQ;
                    if (matchesMap.containsKey(key))
                    {
                        vibModes = new ArrayList<List<Double>>();
                        int iBlock = -1; // the count of blocks of freqs
                        for (Integer i : matchesMap.get(key))
                        {
                            // Read the frequency (eigenvalue)
                            String line = tail.get(i);
                            String[] p = line.split("\\s+");
                            for (int j=2; j<(p.length); j++)
                            {
                                double freq = Double.parseDouble(p[j]);
                                if (freq < 0.0
                                    && Math.abs(freq) >= lowestFreq 
                                    && Math.abs(freq) >= imThrsh)
                                {
                                    if (verbosity > 2)
                                    {
                                        System.out.println("Found img.freq.: "
                                                                        + freq);
                                    }
                                    imgFreqs.add(freq);
                                }
                                projFrequencies.add(freq);

                                // Here we only add an empty mode: will be filled later
                                List<Double> mode = new ArrayList<Double>();
                                vibModes.add(mode);
                            }

                            // Read also the vibrational mode (eigenvector)
                            iBlock++;
                            boolean goon = true;
                            int r=i+1; //NB: we skip the first empty line
                            while (goon)
                            {
                                r++;
                                String linevm = tail.get(r);
                                String[] pv = linevm.split("\\s+");
                                if (pv.length < 2)
                                {
                                    goon = false;
                                    break;
                                }
                                for (int j=2; j<(pv.length); j++)
                                {
                                    int vmID = iBlock*6 + (j-2);
                                    double val = 0.0;
                                    try
                                    {
                                        val = Double.parseDouble(pv[j]);
                                    } catch (Throwable t)
                                    {
                                        Terminator.withMsgAndStatus("ERROR! "
                                        + "String '" + pv[j] + "' cannot be "
                                        + "converted to a double. Error "
                                        + "while readinf vibrational modes.",
                                        -1);
                                    }
                                    vibModes.get(vmID).add(val);
                                }
                            }
                        }

                        if (printVibModes)
                        {
                            printVibrationalModes();
                        }
                    }
                    else
                    {
                        res = res + " No frequency found! ";        
                    }
                }

                if (analyseEnergy)
                {
                    if (matchesMap.containsKey(NWChemConstants.OUTTOTDFTENERGY))
                    {
                        double temp = 0.0d;
                        double e = 0.0d;
                        double corrH = 0.0d;
                        double corrS = 0.0d;
                        double traS = 0.0d;
                        double rotS = 0.0d;
                        double vibS = 0.0d;
                        String line = tail.get(
                                 matchesMap.get(
                                  NWChemConstants.OUTTOTDFTENERGY).get(
                                      matchesMap.get(
                                    NWChemConstants.OUTTOTDFTENERGY).size()-1));
                        String[] p = line.split("\\s+");
                        e = Double.parseDouble(p[5]);
                        res = "DFT Energy: " + e + " ";

                        if (matchesMap.containsKey(NWChemConstants.OUTTOTS))
                        {
                            line = tail.get(
                                     matchesMap.get(
                                      NWChemConstants.OUTCORRH).get(
                                       matchesMap.get(
                                        NWChemConstants.OUTCORRH).size()-1));
                            p = line.split("\\s+");
                            corrH = Double.parseDouble(p[9]);
                            res = res + corrH + " ";

                            line = tail.get(
                                       matchesMap.get(
                                        NWChemConstants.OUTTEMP).get(
                                         matchesMap.get(
                                          NWChemConstants.OUTTEMP).size()-1));
                            p = line.split("\\s+");
                            temp = Double.parseDouble(p[3].replaceAll("K",""));
                            res = res + temp + " ";

                            line = tail.get(
                                     matchesMap.get(
                                      NWChemConstants.OUTTOTS).get(
                                       matchesMap.get(
                                        NWChemConstants.OUTTOTS).size()-1));
                            p = line.split("\\s+");
                            corrS = Double.parseDouble(p[4]);
                            res = res + corrS + " ";

                            if (calcQHarmVibS)
                            {
                                line = tail.get(
                                         matchesMap.get(
                                          NWChemConstants.OUTTRAS).get(
                                           matchesMap.get(
                                            NWChemConstants.OUTTRAS).size()-1));
                                p = line.split("\\s+");
                                traS = Double.parseDouble(p[4]);
    
                                line = tail.get(
                                         matchesMap.get(
                                          NWChemConstants.OUTROTS).get(
                                           matchesMap.get(
                                            NWChemConstants.OUTROTS).size()-1));
                                p = line.split("\\s+");
                                rotS = Double.parseDouble(p[4]);
                            
                                line = tail.get(
                                         matchesMap.get(
                                          NWChemConstants.OUTVIBS).get(
                                           matchesMap.get(
                                            NWChemConstants.OUTVIBS).size()-1));
                                p = line.split("\\s+");
                                vibS = Double.parseDouble(p[4]);

                                vibS = CompChemComputer.vibrationalEntropyCorr(
                                                                projFrequencies,
                                                                temp,
                                                                qhThrsh,
                                                                imThrsh,
                                                                0.01,
                                                                verbosity);
                                corrS = traS + rotS + vibS;
                                res = res + String.format(Locale.ENGLISH,
                                		"%.2f ", corrS); 
                            }
                        }
                    }
                    else
                    {
                        res = res + " No dft energy found! ";
                    }
                }

                if (checkTS)
                {
                    if (matchesMap.containsKey(NWChemConstants.OUTPROJFREQ))
                    {
                        switch (imgFreqs.size())
                        {
                            case 0:
                                res = res + " MINIMUM ";
                                break;
                            case 1:
                                res = res + " TRANSITION STATE ";
                                break;
                            default:
                                res = res + " SADDLE POINT (order " 
                                                        + imgFreqs.size()+ ") ";
                                break;
                        }
                    }
                }
                
                analysisResult = res;
            }
        }
        
        if (verbosity > 0)
        {
            System.out.println(" " + getResultsAsString());
        }
    }

//------------------------------------------------------------------------------

    /**
     * Extract the last geometry from the NWChem output loaded in the handler
     * @return the last geometry
     */

    public IAtomContainer extractLastOutputGeometry()
    {
        List<IAtomContainer> allGeoms = getAllGeometries();
        if (allGeoms.size() == 0)
        {
            Terminator.withMsgAndStatus("ERROR! No geometry found in '" 
                                                           + inFile + "'.", -1);
        }
        IAtomContainer mol = allGeoms.get(allGeoms.size()-1);
        return mol;
    }

//------------------------------------------------------------------------------

    /**
     * Extract the last geometry from an NWChem output file 
     * @param nwcOutFile pathname of the NWChem output file 
     * @return the last geometry
     */

    public IAtomContainer extractLastOutputGeometry(String nwcOutFile)
    {
        this.inFile = nwcOutFile;
        return extractLastOutputGeometry();
    }

//------------------------------------------------------------------------------

    /**
     * Prints all the geometries found in the loaded NWChem output file.
     * All geometries
     * of all tasks are considered.
     */

    public void printTrajectory()
    {
        printTrajectory(false);
    }

//------------------------------------------------------------------------------

    /**
     * Prints all the optimized geometries found in the loaded NWChem output 
     * file.
     * All geometries
     * of all tasks are considered.
     */

    public void printOptTrajectory()
    {
        printTrajectory(true);
    }

//------------------------------------------------------------------------------

    /**
     * Prints geometries found in the loaded NWChem output file. 
     * All geometries
     * of all tasks are considered.
     * @param onlyOpt set to true to restrict to optimized geometries
     */

    public void printTrajectory(boolean onlyOpt)
    {
        List<IAtomContainer> allGeoms;
        if (onlyOpt)
        {
            allGeoms = getAllOptGeometries();
        }
        else
        {
            allGeoms = getAllGeometries();
        }
        IAtomContainerSet mols = new AtomContainerSet();
        for (IAtomContainer iac : allGeoms)
        {
            mols.addAtomContainer(iac);
        }
        switch (outFormat.toUpperCase())
        {
            case "SDF":
                if (!outFile.endsWith(".sdf"))
                {
                    outFile = outFile + ".sdf";
                }
                IOtools.writeSDFAppendSet(new File(outFile),mols,false);
                break;

            case "XYZ":
                if (!outFile.endsWith(".xyz"))
                {
                    outFile = outFile + ".xyz";
                }
                IOtools.writeXYZAppendSet(new File(outFile),mols,false);
                break;

            case "SDFXYZ":
                IOtools.writeXYZAppendSet(new File(outFile + ".xyz"),mols,false);
                IOtools.writeSDFAppendSet(new File(outFile + ".sdf"),mols,false);
                break;

            default:
                Terminator.withMsgAndStatus("ERROR! Format '" + outFormat
                    + "' cannot be use for output in this context. Try SDF,"
                    + " XYZ, or SDFXYZ (will print both).",-1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Extract the last geometry from the last task found in the loaded NWChem
     * output file and prints it to file according to the parameters defined by 
     * constructor.
     */

    public void printLastOutputGeometry()
    {
        IAtomContainer mol = extractLastOutputGeometry();
        switch (outFormat.toUpperCase())
        {
            case "SDF":
                if (!outFile.endsWith(".sdf"))
                {
                    outFile = outFile + ".sdf";
                }
                if (useTemplateConnectivity)
                {
                    ConnectivityUtils.importConnectivityFromReference(
                                                mol,connectivityTemplate);
                }
                IOtools.writeSDFAppend(new File(outFile),mol,false);
                break;

            case "XYZ":
                if (!outFile.endsWith(".xyz"))
                {
                    outFile = outFile + ".xyz";
                }
                IOtools.writeXYZAppend(new File(outFile),mol,false);
                break;

            case "SDFXYZ":
                IOtools.writeXYZAppend(new File(outFile + ".xyz"),mol,false);
                IOtools.writeSDFAppend(new File(outFile + ".sdf"),mol,false);
                break;

            default:
                Terminator.withMsgAndStatus("ERROR! Format '" + outFormat 
                    + "' cannot be use for output in this context. Try SDF,"
                    + " XYZ, or SDFXYZ (will print both).",-1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Print selected vibrational modes projected in Cartesian coordinates
     * according to the parameters defined by constructor.
     */

    private void printVibrationalModes()
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int vmId : selectedVibModes)
        {
            if (first)
            {
                first = false;
            } else {
                sb.append(">").append(System.getProperty("line.separator"));
            }
            List<Double> mode = vibModes.get(vmId);
            for (int atmId=0; atmId<(mode.size()/3); atmId++)
            {
                sb.append(String.format(Locale.ENGLISH," %5.5f",mode.get(atmId*3)));
                sb.append(String.format(Locale.ENGLISH," %5.5f",mode.get(atmId*3+1)));
                sb.append(String.format(Locale.ENGLISH," %5.5f",mode.get(atmId*3+2)));
                sb.append(System.getProperty("line.separator"));
            }
        }
        IOtools.writeTXTAppend(new File(outFileVibModes),sb.toString(),false);
        if (verbosity > 1)
        {
            System.out.println(" Vibrational mode " + selectedVibModes 
                + " written to '" + outFileVibModes + "'.");
        }
    }

//------------------------------------------------------------------------------

    /**
     * Analize the output and identify the error (if this is known)
     * @param tail the portion of NWChem output file corresponding to the last
     * step
     */

    private void identifyErrorMessage(List<String> tail)
    {
        errorIsDecoded = false;
        for (ErrorMessage em : errorDef)
        {
            //Get the error message of this candidate error
            List<String> emLines = em.getErrorMessage();

            if (verbosity > 1)
                System.out.println(" -> Trying with "+em.getName()+" <-");

            //Read .out file from bottom to top  looking for the error messages
             List<Integer> counts = new ArrayList<Integer>();
            for ( int il=0; il<emLines.size(); il++)
            {
                counts.add(0);
            }
            for (int i=(tail.size() - 1); i>-1; i--)
            {
                String line = tail.get(i);
                line = line.trim();
                for (int il=0; il<emLines.size(); il++)
                {
                    String emLine = emLines.get(il);
                    if (verbosity > 4)
                    {
                        System.out.println("Comparing line: \n_"
                                            + line
                                            + "_\n_"
                                            + emLine + "_");
                    }

                    if (line.contains(emLine))
                    {
                        counts.set(il,counts.get(il)+1);
                        if (verbosity > 3)
                        {
                            System.out.println("Error message '" + emLine 
                                                                + "' found");
                        }
                        break;
                    }
                }

                if (!counts.contains(0))
                {
                    errorIsDecoded = true;
                    actualEM = em;
                    break;
                }
            } //end loop over tail lines

            if (errorIsDecoded)
            {
                //Are there other conditions this .out has to fulfill?
                List<String> conditions = em.getConditions();
                int numberOfConditions = conditions.size();
                if (numberOfConditions != 0)
                {
                    int numTrue = 0;
                    for (String cond : conditions)
                    {
                        String[] p = cond.split("\\s+");
                        String task = p[0];
                        task = task.toUpperCase();

                        if (verbosity > 3)
                        {
                            System.out.println("Task: "+task);
                        }
                        
                        boolean isSatisfied = false;

                        switch (task)
                        {
                            case "MATCH" :
                            {
                                String pattern = cond.substring(task.length());
                                String[] pp = pattern.split("\"");
                                pattern = pp[1];
                                pattern = pattern.toUpperCase();
                                for (int it=0; it<tail.size(); it++)
                                {
                                    String line = tail.get(it);
                                    line = line.toUpperCase();
//                                    System.out.println("Comparing= __"+line+"__with__"+pattern);
//                                    System.out.println("Result: "+(line.contains(pattern)));
                                    if (line.contains(pattern))
                                    {
                                        isSatisfied = true;
                                        if (verbosity > 3)
                                        {
                                            System.out.println("Match condition satisfied by "
                                                + "text '" + line + "'");
                                        }
                                        break;
                                    }
                                }
//                                System.out.println("isSatisfied= "+isSatisfied);
                                break;
                            }
                            case "NOTMATCH" :
                            {
                                boolean patternFound = false;
                                String pattern = cond.substring(task.length());
                                String[] pp = pattern.split("\"");
                                pattern = pp[1];
                                pattern = pattern.toUpperCase();
                                for (int it=0; it<tail.size(); it++)
                                {
                                    String line = tail.get(it);
                                    line = line.toUpperCase();
                                    if (line.contains(pattern))
                                    {
                                        patternFound = true;
                                        if (verbosity > 3)
                                        {
                                            System.out.println("notMatch condition can NOT be "
                                                        + "satisfied. Pattern found in text '" 
                                                        + line + "'");
                                        }
                                        break;
                                    }
                                }
                                if (!patternFound)
                                {
                                    isSatisfied = true;
                                    if (verbosity > 3)
                                    {
                                        System.out.println("notMatch condition satisfied.");
                                    }
                                }
                                break;
                            }

                            case "CHECK_COUNTER":  
                                //This condition is meant for loops with a named index 

                                //Get identifier and limit of counter
                                String counterName = cond.substring(task.length());
                                String[] pp = counterName.split("\"");
                                counterName = pp[1];
                                counterName = counterName.toUpperCase();
                                int counterLimit = -1;
                                try {
                                    counterLimit = Integer.parseInt(p[2]);
                                } catch (Throwable t) {
                                    Terminator.withMsgAndStatus("ERROR! "
                                        + "Unable to read loop limit in error " 
                                        + em.getName(),-1);
                                }

                                //Get the current value of the counter
                                int counterValue = -1;
                                for (int it=0; it<tail.size(); it++)
                                {
                                    String line = tail.get(it);
                                    line = line.toUpperCase();
                                    if (line.contains(counterName))
                                    {
                                        if (verbosity > 3)
                                        {
                                            System.out.println("Counter " 
                                                + counterName + " found!");
                                        }
                                        String[] words = line.split("\\s+");
                                        for (int iw=0; iw<words.length; iw++)
                                        {
                                            String w = words[iw];
                                            if (w.contains(counterName))
                                            {
                                                String[] ppp = w.split("=");
                                                try {
                                                    counterValue = 
                                                       Integer.parseInt(ppp[1]);
                                                } catch (Throwable t) {
                                                        Terminator.withMsgAndStatus(
                                                        "ERROR! Unable to read "
                                                        + "value of counter "
                                                        + counterName + " in "
                                                        + w,-1);
                                                }
                                                break;
                                            }
                                        }

                                        //Make sure we got the number
                                        if (counterValue < 0)
                                        {
                                            Terminator.withMsgAndStatus("ERROR! "
                                                + "Unable to read loop value in "
                                                + line,-1);
                                        }
                                    
                                        break;
                                    } 
                                }

                                //Check condition
                                if (counterValue >= counterLimit)
                                {
                                    Terminator.withMsgAndStatus("Maximum "
                                    + "number of cycles reaches for " 
                                    + em.getName() + ". " + counterName
                                    + " = " + counterValue + " (limit="
                                    + counterLimit +").",-1);
                                } else if (counterValue > 0) {
                                    if (verbosity > 1)
                                    {
                                        System.out.println(" Counter '" 
                                        + counterName
                                        + "' is still within the limit (next "
                                        + "iteration will have index "
                                        + (counterValue + 1) + " <= " 
                                        + counterLimit + ").");
                                    }
                                    counters.put(counterName,counterValue);
                                    isSatisfied = true;
                                } else {
                                    if (verbosity > 1)
                                    {
                                        System.out.println(" Check counter "
                                            + "condition is not verified: " 
                                            + "counter "
                                            + counterName + " not found.");
                                    }
                                }
                                break;
//TODO
/*
                            case "MATCH_DIRECTIVE":
                            {
                                if (noJobDetails)
                                {
                                    System.out.println(" ");
                                    System.out.println("WARNING! "
                                        + "Task cannot be "
                                        + "executed without providing the "
                                        + "JOBDETAILS. Skipping");
                                    System.out.println(" ");
                                    break;
                                }

                                //Get key and value to be evaluated 
                                String kv = cond.substring(task.length());
                                kv = kv.toUpperCase();
                                String key = kv.substring(0,kv.indexOf("="));
                                key = key.trim();
                                String value = kv.substring(kv.indexOf("=")+1);
                                value = value.trim();

                                //Get Route section from nwcJob
                                NWChemTask failStep = 
                                                nwcJob.getStep(numSteps - 1);
                                NWChemRouteSection route = 
                                                     failStep.getRouteSection();

                                //WARNING! Use numSteps - 1 because 
                                // NWChemOutputHandler counts steps 
                                // from 1 to N

                                //Look for requested keyword
                                if (route.keySet().contains(key))
                                {
                                    String originalValue = route.getValue(key);

                                    //evaluate value of keyword
                                    if (originalValue.equals(value))
                                    {
                                        isSatisfied = true;
                                        if (verbosity > 3)
                                        {
                                            System.out.println(
                                                "MATCH_ROUTEKEYWORD "
                                                + "condition "
                                                + "satisfied for keyword '"
                                                + key + "' which has value '"
                                                + originalValue + "'.");
                                        }
                                    } else {
                                        if (verbosity > 3)
                                        {
                                            System.out.println(
                                                "MATCH_ROUTEKEYWORD "
                                                + "condition is NOT"
                                                + " satisfied for keyword '"
                                                + key + "' which has value '"
                                                + originalValue + "' instead "
                                                + "of '" + value + "'.");
                                        }
                                    }
                                } else {
                                    if (verbosity > 1)
                                    {
                                        System.out.println(" Condition "
                                            + "MATCH_ROUTEKEYWORD cannot be "
                                            + "satisfied. Keyword '"
                                            + key + "' "
                                            + "not found in route section.");
                                    }
                                }
                                break;
                            }
*/

/*
TODO add other tasks here
                            case "":
                                ...do something...
                                break;

*/

                            default:
                                Terminator.withMsgAndStatus("ERROR! Condition "
                                        + task + " not known! "
                                        + "Check definition of error " 
                                        + em.getName(),-1);
                        }
                                        
                        if (!isSatisfied)
                            break;
                        else 
                            numTrue++;
                    }

                    if (numberOfConditions != numTrue)
                        errorIsDecoded = false;
                }

                //Nothing else to do if the error is identified
                if (errorIsDecoded)
                    break;
            }
        } //end loop over known errors

    }

//------------------------------------------------------------------------------

    /**
     * Method clarifying whether the error behind a not-normally terminated job
     * has been identified by this NWChemOutputHandler or not.
     * @return <code>true</code> if NWChem error message has been understood
     */

    public boolean isErrorUnderstood()
    {
        return errorIsDecoded;
    }

//------------------------------------------------------------------------------

    /**
     * Method returning the {@link ErrorMessage} of a not-normally terminated
     * job.
     * @return the ErrorMessage identified from NWChem .out file
     */

    public ErrorMessage getErrorMessage()
    {
        return actualEM;
    }

//------------------------------------------------------------------------------

    /**
     * Method returning the number of {@link NWChemTask}s represented in the
     * NWChem output provided.
     * @return the number of steps identified in NWChem .out file, which is 
     * the index (1 to n) of the last step.
     */

    public int getNumberOfSteps()
    {
        return numSteps;
    }

//------------------------------------------------------------------------------

    /**
     * Method returning AutoCompChem counters stored in NWChem output file.
     * @return the map of counters with counter names and values
     */

    public Map<String,Integer> getCounters()
    {
        return counters;
    }

//------------------------------------------------------------------------------

    /**
     * Search the initial set of coordinates in a NWChem output. For initial
     * coordinates we intend the Cartesian coordinates provided to NWChem  by
     * means of *.nw file and NOT taken from checkpoint file.
     * @return molecular definition as atoms and coords (no connectivity)
     */

    public IAtomContainer getInitialGeometry()
    {
        IAtomContainer mol = new AtomContainer();

        List<String> lines = FileAnalyzer.extractTxtWithDelimiters(inFile,
                                                NWChemConstants.OUTSTARTINITXYZ,
                                                NWChemConstants.OUTENDINITXYZ,
                                                                        false);

        //Read skipping first lines 
        for (int i=3; i<lines.size(); i++)
        {
            String[] parts = lines.get(i).trim().split("\\s+");
            String el = parts[0];
            Point3d p3d = new Point3d(Double.parseDouble(parts[1]),
                                      Double.parseDouble(parts[2]),
                                      Double.parseDouble(parts[3]));
            IAtom atm = new Atom(el,p3d);
            mol.addAtom(atm);
        }

        return mol;
    }

//------------------------------------------------------------------------------

    /**
     * Search for all optimized geometries
     * @return the set of molecules defined as atoms and coords
     * (no connectivity)
     */

    public List<IAtomContainer> getAllOptGeometries()
    {
        TreeMap<String,List<String>> mapBlocks =
                      FileAnalyzer.extractMapOfTxtBlocksWithDelimiters(inFile,
                                          new ArrayList<String>(Arrays.asList(
                   NWChemConstants.OUTSTARTXYZ,
                   NWChemConstants.OUTENDCONVGEOMOPTSTEP)),
                                          new ArrayList<String>(Arrays.asList(
                   NWChemConstants.OUTENDXYZ,
                   NWChemConstants.OUTENDGEOMOPTSTEP)),
                                                                          false,
                                                                          true);

        List<IAtomContainer> molList = new ArrayList<IAtomContainer>();
        for (Map.Entry<String,List<String>> entry : mapBlocks.entrySet())
        {
            // We look only for keys identifying a converged geometry
            String key = entry.getKey();
            String[] subKeys = key.split("_");
            if (!subKeys[1].equals("1"))
            {
                continue;
            }

            // WARNING! Assuming that the block before the current one is 
            // a block of Cartesian coordinates
            List<String> xyzBlock = mapBlocks.lowerEntry(key).getValue();
            molList.add(getIAtomContainerFromXYZblock(xyzBlock));
        }

        return molList;
    }

//------------------------------------------------------------------------------

    /**
     * Search for all geometries
     * @return the set of molecules defined as atoms and coords 
     * (no connectivity)
     */

    public List<IAtomContainer> getAllGeometries()
    {
        List<List<String>> blocks = 
                       FileAnalyzer.extractMultiTxtBlocksWithDelimiters(inFile,
                                          new ArrayList<String>(Arrays.asList(
                   NWChemConstants.OUTSTARTXYZ,NWChemConstants.OUTHESSTARTXYZ)),
                                          new ArrayList<String>(Arrays.asList(
                       NWChemConstants.OUTENDXYZ,NWChemConstants.OUTHESENDXYZ)),
                                                                          false,
                                                                          true);

        List<IAtomContainer> molList = new ArrayList<IAtomContainer>();
        for (List<String> singleBlock : blocks)
        {
            molList.add(getIAtomContainerFromXYZblock(singleBlock));
        }

        return molList;
    }

//------------------------------------------------------------------------------

    /**
     * Reads a Cartesian coordinates table, in the format from NWChem output 
     * file (including header) and return the corresponding molecular 
     * representation (without connectivity). Only the syntax deployed in the
     * output of geometry optimization and Hessian/frequency calculation tasks
     * is recognized.
     * @param lines the list of lines corresponding to the XYZ table
     * @return the molecule obtained
     */

    public IAtomContainer getIAtomContainerFromXYZblock(List<String> lines)
    {
        IAtomContainer mol = new AtomContainer();
        if (lines.size() < 3)
        {
            Terminator.withMsgAndStatus("ERROR! Cannot find XYZ coords in text "
            + "block '" + lines + "'. Unable to read NWChemOutput.",-1);
        }
        String firstLine = lines.get(0);
        if (firstLine.matches(NWChemConstants.OUTSTARTXYZ))
        {
            //Read skipping first two lines and last
            for (int i=2; i<(lines.size()-1); i++)
            {
                String[] parts = lines.get(i).trim().split("\\s+");
                String el = parts[1];
                String elNoTag = el.replaceAll("[^A-Za-z]+","");
                if (elNoTag.length() == 0)
                {
                    System.out.println(" WARNING! Atom tag '" + el + "' could "
                      + " not be understood. Using dummy element symbol 'Du'.");
                    el = "Du";
                }
                else
                {
                    el = elNoTag;
                }
                Point3d p3d = new Point3d(Double.parseDouble(parts[3]),
                                          Double.parseDouble(parts[4]),
                                          Double.parseDouble(parts[5]));
                IAtom atm = new Atom(el,p3d);
                mol.addAtom(atm);
            }
        }
        else if (firstLine.matches(NWChemConstants.OUTHESSTARTXYZ))
        {
            //Read skipping first two lines and last two lines
            for (int i=2; i<(lines.size()-2); i++)
            {
                String[] parts = lines.get(i).trim().split("\\s+");
                String el = parts[0];
                String elNoTag = el.replaceAll("[^A-Za-z]+","");
                if (elNoTag.length() == 0)
                {
                    System.out.println(" WARNING! Atom tag '" + el + "' could "
                      + " not be understood. Using dummy element symbol 'Du'.");
                    el = "Du";
                }
                else
                {
                    el = elNoTag;
                }
                Point3d p3d = new Point3d(
                                  Double.valueOf(parts[2].replaceAll("D","E"))
                                             / ACCConstants.ANGSTOMTOBOHR,
                                  Double.valueOf(parts[3].replaceAll("D","E"))
                                             / ACCConstants.ANGSTOMTOBOHR,
                                  Double.valueOf(parts[4].replaceAll("D","E"))
                                             / ACCConstants.ANGSTOMTOBOHR);
                IAtom atm = new Atom(el,p3d);
                mol.addAtom(atm);
            }
        }
        else
        {
            Terminator.withMsgAndStatus("ERROR! Cannot understant syntax in "
                                        + "XYZ coords block. Please report to "
                                        + "the author.",-1);
        }

        return mol;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a string summarizing the results of the evaluation 
     * @return the summary as a string
     */

    public String getResultsAsString()
    {
        String str = "File:" + inFile + " Steps:" + numSteps + " NormalTerm:"
                        + normalTerminated;
        if (!normalTerminated)
        {
            str = str + " Error:";
            if (errorIsDecoded)
                str = str + actualEM.getName();
            else
                str = str + "Not Known";
        }
        else
        {
            str = str + "\n " + analysisResult;
        }

        return str;
    }

//------------------------------------------------------------------------------

}
