package autocompchem.chemsoftware.gaussian;

import java.util.ArrayList;

/*   
 *   Copyright (C) 2014  Marco Foscato 
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

import autocompchem.atom.AtomUtils;
import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.CompChemJob;
import autocompchem.chemsoftware.errorhandling.ErrorManager;
import autocompchem.chemsoftware.errorhandling.ErrorMessage;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedData.NamedDataType;
import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.modeling.compute.CompChemComputer;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.run.Terminator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Reader and analyser of Gaussian Output files. 
 * 
 * @author Marco Foscato
 */

public class GaussianOutputHandler extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
                    Collections.unmodifiableSet(new HashSet<TaskID>(
                                    Arrays.asList(TaskID.EVALUATEGAUSSIANOUTPUT)));
    
    /**
     * Name of the .out file from Gaussian (the input of this class)
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
     * Name of the jobdetails file
     */
    private GaussianJob gaussJob;

    /**
     * Flag recording the existence of a jobDetails file
     */
    private boolean noJobDetails = false;

    /**
     * List of known error messages
     */
    private ArrayList<ErrorMessage> errorDef;

    /**
     * Verbosity level
     */
    private int verbosity = 1;

    /**
     * Total number Steps found (1-to-n) in the gaussian output
     */
    private int numSteps;

    /**
     * Flag recording normal termination of the Gaussian job
     */
    private boolean normalTerminated;

    /**
     * Flag recording identification of the error in the Gaussian job
     */
    private boolean errorIsDecoded;

    /**
     * Class of error found in the Gaussian output
     */
    private ErrorMessage actualEM;

    /**
     * Counters found in the Gaussian output and their current value
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
     * Flag requiring calculation of vibrational S by quasi-harmonic approx.
     */
    private boolean calcQHarmVibS = false;

    /**
     * Array of vibrational modes read from Gaussian output
     */
    private ArrayList<Double> projFrequencies = new ArrayList<Double>();

    /**
     * Lowest value for non-zero frequencies
     */
    private double lowestFreq = GaussianConstants.MINFREQ;

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
     * Constructs a GaussianOutputHandler specifying the parameters taken from 
     * formatted file or generated by other tools.
     * All the parameters must be in the form specified as follows:
     * <ul>
     * <li>
     * <b>INFILE</b>: path or name of the output from Gaussian 
     * (i.e., name.out).
     * </li>
     * <li>
     * <b>GAUSSIANERRORS</b>: path to the folder storing Gaussian known
     *  errors.
     * </li>
     * <li>
     * <b>JOBDETAILSFILE</b>: formatted text file defining all the details of 
     *            {@link GaussianJob}
     *            the JOBDETAILS txt file is the file used to generate the 
     *            input file (name.inp) for Gaussian.
     * </li>
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
     * <li>
     * (optional) <b>TEMPLATECONNECTIVITY</b>: pathname of an SDF file that
     * will be used as template to define the connectivity when writing the
     * last geometry to an SDF file. This copies all the connectivity table
     * without considering the actual geometry in the Gaussian job.
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
    public GaussianOutputHandler(ParameterStorage params) 
    {
    	*/

//-----------------------------------------------------------------------------

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
            System.out.println(" Adding parameters to GaussianOutputHandler");

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
            this.gaussJob = new GaussianJob(jdFile);
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
            this.gaussJob = new GaussianJob(lines);
        }
        else 
        {
        	if (verbosity > 0)
            {
                System.out.println(" ");
                System.out.println(" WARNING! No JOBDETAILS provided. Some "
                + "errors cannot be identified without JOBDETAILS file.");
                System.out.println(" ");
            }
            noJobDetails = true;
        }

        //Get and check the input file (which is an output from Gaussian)
        this.inFile = params.getParameter("INFILE").getValue().toString();
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

        //Get and check the list of known errors
        if (params.contains("GAUSSIANERRORS"))
        {
            String errDefPath = 
                params.getParameter("GAUSSIANERRORS").getValue().toString();
            if (verbosity > 0)
            {
                System.out.println(" Importing known errors from " + errDefPath);
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

        if (params.contains("TEMPLATECONNECTIVITY"))
        {
            this.useTemplateConnectivity = true;
            String fileWithTplt =
              params.getParameter("TEMPLATECONNECTIVITY").getValue().toString();
            FileUtils.foundAndPermissions(fileWithTplt,true,false,false);
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
        case EVALUATEGAUSSIANOUTPUT:
        	evaluateGaussianOutput();
                break;
        }

        if (exposedOutputCollector != null)
        {
            String refName = "AnalysisOfGaussianOutput";
            exposeOutputData(new NamedData(refName,
                        NamedDataType.STRING, analysisResult));
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Analyses the input file and identify its properties: 
     * termination status, number
     * steps, type of error, and so on.
     */

    public void evaluateGaussianOutput()
    {
        //Read the outfile and collect counts and line numbers
        ArrayList<String> patterns = new ArrayList<String>();
        patterns.add("Initial command:");
        patterns.add("Normal termination");
        ArrayList<ArrayList<Integer>> countsAndLineNum = 
                                           FileAnalyzer.count(inFile,patterns);
        int indexOfCounts = countsAndLineNum.size() - 1;
        ArrayList<Integer> counts = countsAndLineNum.get(indexOfCounts);
        ArrayList<ArrayList<Integer>> lineNums = 
                                            new ArrayList<ArrayList<Integer>>();
        //Keep all but the last array (which is count)
        for (int i=0; i<(countsAndLineNum.size() - 1); i++)
            lineNums.add(countsAndLineNum.get(i));

        numSteps = counts.get(0);
        int numNormTerm = counts.get(1);

            if (numSteps > 0)
            {
                if (verbosity > 0)
                {
                    System.out.println(" Gaussian output file contains: " 
                                          + numSteps + " steps");
                }
                if (printLastGeom)
                {
                    printLastOutputGeometry();
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
        ArrayList<String> tail = IOtools.tailFrom(inFile,lastInit);

        //Error identififcation
        if (numSteps != numNormTerm)
        {
            normalTerminated = false;

            if (verbosity > 0)
            {
                System.out.println(" The job did not terminate normally ");
                System.out.println(" Attempting identification of error...");
            }

                //Compare with known errors
                identifyErrorMessage(tail);

            //Report
            if (verbosity > 0)
            {
                if (errorIsDecoded)
                {
                    System.out.println(" Gaussian Error Recognized: Step "  
                                        + numSteps
                                        + " returned Error Message: "
                                        + actualEM.getName());
                } else {
                    System.out.println(" Gaussian Error NOT Recognized. Please "
                                + "identify the error by hand and then add a "
                            + "new ErrorMessage to the list of known "
                            + "errors.");
                }
            }
        } 
        else 
        {
            normalTerminated = true;
            patterns = new ArrayList<String>();
            if (analyseEnergy)
            {
                patterns.add(GaussianConstants.OUTTOTDFTENERGY);
                patterns.add(GaussianConstants.OUTCORRH);
                patterns.add(GaussianConstants.OUTTOTS);
                patterns.add(GaussianConstants.OUTTEMP);
                if (calcQHarmVibS)
                {
                    patterns.add(GaussianConstants.OUTPROJFREQ);
                    patterns.add(GaussianConstants.OUTTRAS);
                    patterns.add(GaussianConstants.OUTROTS);
                    patterns.add(GaussianConstants.OUTVIBS);
                }
            }

            if (checkTS)
            {
                patterns.add(GaussianConstants.OUTPROJFREQ);
            }

    // Also, now the condition is always satisfied, but there might be cases
    // chere we do not need to do it

            if (analyseEnergy || checkTS)
            {

    //TODO use method in IOtools
                Map<String,ArrayList<Integer>> matchesMap =
                                       new HashMap<String,ArrayList<Integer>>();
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
                                ArrayList<Integer> lst = 
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

                ArrayList<Double> imgFreqs = new ArrayList<Double>();
                if (readFrequencies)
                {
                    String key = GaussianConstants.OUTPROJFREQ;
                    if (matchesMap.containsKey(key))
                    {
                        for (Integer i : matchesMap.get(key))
                        {
                            String line = tail.get(i);
                            String[] p = line.trim().split("\\s+");
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
                            }
                        }
                    }
                    else
                    {
                        res = res + " No frequency found! ";        
                    }
                }

                if (analyseEnergy)
                {
                    if (matchesMap.containsKey(GaussianConstants.OUTTOTDFTENERGY))
                    {
                        double temp = 0.0d;
                        double e = 0.0d;
                        //double corrZP = 0.0d;
                        double corrH = 0.0d;
                        double corrS = 0.0d;
                        double traS = 0.0d;
                        double rotS = 0.0d;
                        double vibS = 0.0d;
                        double vibSGau = 0.0d;
                        String line = tail.get(
                                 matchesMap.get(
                                  GaussianConstants.OUTTOTDFTENERGY).get(
                                      matchesMap.get(
                                    GaussianConstants.OUTTOTDFTENERGY).size()-1));
                        String[] p = line.trim().split("\\s+");
                        e = Double.parseDouble(p[4]);
                        res = "DFT Energy: " + e + " ";

                        if (matchesMap.containsKey(GaussianConstants.OUTTOTS))
                        {
                            line = tail.get(
                                     matchesMap.get(
                                      GaussianConstants.OUTCORRH).get(
                                       matchesMap.get(
                                        GaussianConstants.OUTCORRH).size()-1));
                            p = line.trim().split("\\s+");
                            corrH = Double.parseDouble(p[4]);
                            res = res + corrH + " ";

                            line = tail.get(
                                       matchesMap.get(
                                        GaussianConstants.OUTTEMP).get(
                                         matchesMap.get(
                                          GaussianConstants.OUTTEMP).size()-1));
                            p = line.trim().split("\\s+");
                            temp = Double.parseDouble(p[1].replaceAll("K",""));
                            res = res + temp + " ";

                            line = tail.get(
                                     matchesMap.get(
                                      GaussianConstants.OUTTOTS).get(
                                       matchesMap.get(
                                        GaussianConstants.OUTTOTS).size()-1));
                            p = line.trim().split("\\s+");
                            corrS = Double.parseDouble(p[3]);
                            res = res + corrS + " ";

                            if (verbosity > 2)
                            {
                                System.out.println("Corrections extracted: ");
                                System.out.println("H: "+corrH);
                                System.out.println("S: "+corrS);
                            }

                            if (calcQHarmVibS)
                            {
                                line = tail.get(
                                         matchesMap.get(
                                          GaussianConstants.OUTTRAS).get(
                                           matchesMap.get(
                                            GaussianConstants.OUTTRAS).size()-1));
                                p = line.trim().split("\\s+");
                                traS = Double.parseDouble(p[3]);

                                line = tail.get(
                                         matchesMap.get(
                                          GaussianConstants.OUTROTS).get(
                                           matchesMap.get(
                                            GaussianConstants.OUTROTS).size()-1));
                                p = line.trim().split("\\s+");
                                rotS = Double.parseDouble(p[3]);
                            
                                line = tail.get(
                                         matchesMap.get(
                                          GaussianConstants.OUTVIBS).get(
                                           matchesMap.get(
                                            GaussianConstants.OUTVIBS).size()-1));
                                p = line.trim().split("\\s+");
                                vibS = Double.parseDouble(p[3]);
                                vibSGau = Double.parseDouble(p[3]);

                                vibS = CompChemComputer.vibrationalEntropyCorr(
                                                            projFrequencies,
                                                            temp,
                                                            qhThrsh,
                                                            imThrsh,
                                                            0.01,
                                                            verbosity);
                                corrS = traS + rotS + vibS;

                                if (verbosity > 2)
                                {
                                    System.out.println("Recalculation of entropy:");
                                    System.out.println("S_TOT(recalculated): "+corrS);
                                    System.out.println("S_VIB(original): "+vibSGau);
                                    System.out.println("S_VIB(recalc):   "+vibS);
                                    System.out.println("S_TRS: "+traS);
                                    System.out.println("S_ROT: "+rotS); 
                                }

                                res = res + String.format("%.2f ",corrS); 
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
                    if (matchesMap.containsKey(GaussianConstants.OUTPROJFREQ))
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
            } // end if do analysis block
        }  //end if NOT normal termiation block

        if (verbosity > 0)
        {
            System.out.println(" " + getResultsAsString());
        }

    }

//------------------------------------------------------------------------------

    /**
     * Extract the last geometry from the Gaussian output loaded in the handler
     * @return the last geometry
     */

    public IAtomContainer extractLastOutputGeometry()
    {
        ArrayList<IAtomContainer> allGeoms = getAllGeometries();
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
     * Extract the last geometry from an Gaussian output file 
     * @param nwcOutFile pathname of the Gaussian output file 
     * @return the last geometry
     */

    public IAtomContainer extractLastOutputGeometry(String nwcOutFile)
    {
        this.inFile = nwcOutFile;
        return extractLastOutputGeometry();
    }

//------------------------------------------------------------------------------

    /**
     * Prints all the geometries found in the loaded Gaussian output file.
     * All geometries
     * of all tasks are considered.
     */

    public void printTrajectory()
    {
        printTrajectory(false);
    }

//------------------------------------------------------------------------------

    /**
     * Prints all the optimized geometries found in the loaded Gaussian output 
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
     * Prints geometries found in the loaded Gaussian output file. 
     * All geometries
     * of all tasks are considered.
     * @param onlyOpt set to true to restrict to optimized geometries
     */

    public void printTrajectory(boolean onlyOpt)
    {
        ArrayList<IAtomContainer> allGeoms;
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
                IOtools.writeSDFAppendSet(outFile,mols,false);
                break;

            case "XYZ":
                if (!outFile.endsWith(".xyz"))
                {
                    outFile = outFile + ".xyz";
                }
                IOtools.writeXYZAppendSet(outFile,mols,false);
                break;

            case "SDFXYZ":
                IOtools.writeXYZAppendSet(outFile + ".xyz",mols,false);
                IOtools.writeSDFAppendSet(outFile + ".sdf",mols,false);
                break;

            default:
                Terminator.withMsgAndStatus("ERROR! Format '" + outFormat
                    + "' cannot be use for output in this context. Try SDF,"
                    + " XYZ, or SDFXYZ (will print both).",-1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Extract the last geometry from the last task found in the loaded Gaussian
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
                IOtools.writeSDFAppend(outFile,mol,false);
                break;

            case "XYZ":
                if (!outFile.endsWith(".xyz"))
                {
                    outFile = outFile + ".xyz";
                }
                IOtools.writeXYZAppend(outFile,mol,false);
                break;

            case "SDFXYZ":
                IOtools.writeXYZAppend(outFile + ".xyz",mol,false);
                IOtools.writeSDFAppend(outFile + ".sdf",mol,false);
                break;

            default:
                Terminator.withMsgAndStatus("ERROR! Format '" + outFormat 
                    + "' cannot be use for output in this context. Try SDF,"
                    + " XYZ, or SDFXYZ (will print both).",-1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Analize the output and identify the error (if this is known)
     * @param tail the portion of Gaussian output file corresponding to the last
     * step
     */

    private void identifyErrorMessage(ArrayList<String> tail)
    {
        errorIsDecoded = false;
        for (ErrorMessage em : errorDef)
        {
            //Get the error message of this error
            ArrayList<String> emLines = em.getErrorMessage();
            int numParts = emLines.size();

            if (verbosity > 1)
                System.out.println(" -> Trying with "+em.getName()+" <-");

            //Read .out file from bottom to top        looking for the error messages
            int numFound = 0;
            for (int i=(tail.size() - 1); i>-1; i--)
            {
                String line = tail.get(i);
                line = line.trim();
                for (String emLine : emLines)
                {
                    if (verbosity > 4)
                    {
                        System.out.println("Comparing line: \n_"
                                            + line
                                            + "_\n_"
                                            + emLine + "_");
                    }

                    if (line.contains(emLine))
                    {
                        numFound++;
                        if (verbosity > 3)
                        {
                            System.out.println("Error message '" + emLine 
                                                                + " 'found");
                        }
                        break;
                    }
                }
                if (numFound == numParts)
                {
                    errorIsDecoded = true;
                    actualEM = em;
                    break;
                }
            } //end loop over tail lines

            if (errorIsDecoded)
            {
                //Are there other conditions this .out has to fulfill?
                ArrayList<String> conditions = em.getConditions();
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

                            case "MATCH_ROUTEKEYWORD":
                            {
                                if (noJobDetails)
                                {
                                    System.out.println(" ");
                                    System.out.println("WARNING! "
                                        + "Task cannot be "
                                        + "executed without providing "
                                        + "job details. Skipping");
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

                                //Get Route section from gaussJob
                                GaussianStep failStep = 
                                                gaussJob.getStep(numSteps - 1);
                                GaussianRouteSection route = 
                                                     failStep.getRouteSection();

                                //WARNING! Use numSteps - 1 because 
                                // GaussianOutputHandler counts steps 
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
     * has been identified by this GaussianOutputHandler or not.
     * @return <code>true</code> if Gaussian error message has been understood
     */

    public boolean isErrorUnderstood()
    {
        return errorIsDecoded;
    }

//------------------------------------------------------------------------------

    /**
     * Method returning the {@link ErrorMessage} of a not-normally terminated
     * job.
     * @return the ErrorMessage identified from Gaussian .out file
     */

    public ErrorMessage getErrorMessage()
    {
        return actualEM;
    }

//------------------------------------------------------------------------------

    /**
     * Method returning the number of {@link GaussianStep}s represented in the
     * Gaussian output provided.
     * @return the number of steps identified in Gaussian .out file, which is 
     * the index (1 to n) of the last step.
     */

    public int getNumberOfSteps()
    {
        return numSteps;
    }

//------------------------------------------------------------------------------

    /**
     * Method returning AutoCompChem counters stored in Gaussian output file.
     * @return the map of counters with counter names and values
     */

    public Map<String,Integer> getCounters()
    {
        return counters;
    }

//------------------------------------------------------------------------------

    /**
     * Search the initial set of coordinates in a Gaussian output. For initial
     * coordinates we intend the Cartesian coordinates provided to Gaussian by
     * means of *.inp file and NOT taken from checkpoint file.
     * @return molecular definition as atoms and coords (no connectivity)
     */

    public IAtomContainer getInitialGeometry()
    {
        IAtomContainer mol = new AtomContainer();

        ArrayList<String> lines = FileAnalyzer.extractTxtWithDelimiters(
                                                        inFile,
                                                        "^ Symbolic Z-matrix:",
                                                        "^\\s*$",
                                                        false);

        //Read skipping first line (is: Charge =...Multiplicity )
        for (int i=1; i<lines.size(); i++)
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

    public ArrayList<IAtomContainer> getAllOptGeometries()
    {
        TreeMap<String,ArrayList<String>> mapBlocks =
                      FileAnalyzer.extractMapOfTxtBlocksWithDelimiters(inFile,
                                          new ArrayList<String>(Arrays.asList(
                   GaussianConstants.OUTSTARTXYZ,
                   GaussianConstants.OUTENDCONVGEOMOPTSTEP)),
                                          new ArrayList<String>(Arrays.asList(
                   GaussianConstants.OUTENDXYZ,
                   GaussianConstants.OUTENDGEOMOPTSTEP)),
                                                                          false,
                                                                          true);

        ArrayList<IAtomContainer> molList = new ArrayList<IAtomContainer>();
        for (Map.Entry<String,ArrayList<String>> entry : mapBlocks.entrySet())
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
            ArrayList<String> xyzBlock = mapBlocks.lowerEntry(key).getValue();
            molList.add(getIAtomContainerFromXYZblock(xyzBlock));
        }

        return molList;
    }

//------------------------------------------------------------------------------

    /**
     * Search for all geometries. This method ignores geometry given as
     * as repetition of the input or confirm of the readout from checkpoint 
     * file.
     * @return the set of molecules defined as atoms and coords 
     * (no connectivity)
     */

    public ArrayList<IAtomContainer> getAllGeometries()
    {
        ArrayList<ArrayList<String>> blocks = 
                       FileAnalyzer.extractMultiTxtBlocksWithDelimiters(inFile,
                                          new ArrayList<String>(Arrays.asList(
               GaussianConstants.OUTSTARTXYZ)),
                                          new ArrayList<String>(Arrays.asList(
                   GaussianConstants.OUTENDXYZ)),
                                                                          false,
                                                                          true);

        ArrayList<IAtomContainer> molList = new ArrayList<IAtomContainer>();
        for (ArrayList<String> singleBlock : blocks)
        {
            molList.add(getIAtomContainerFromXYZblock(singleBlock));
        }

        return molList;
    }

//------------------------------------------------------------------------------

    /**
     * Reads a Cartesian coordinates table, in the format from Gaussian output 
     * file (including header!) and returns the corresponding molecular 
     * representation (without connectivity).
     * @param lines the list of lines corresponding to the XYZ table
     * @return the molecule obtained
     */

    public IAtomContainer getIAtomContainerFromXYZblock(ArrayList<String> lines)
    {
        IAtomContainer mol = new AtomContainer();
        if (lines.size() < 3)
        {
            Terminator.withMsgAndStatus("ERROR! Cannot find XYZ coords in text "
            + "block '" + lines + "'. Unable to read Gaussian Output.",-1);
        }
        String firstLine = lines.get(0);
        if (firstLine.matches(GaussianConstants.OUTSTARTXYZ))
        {
            //Read skipping first three lines and last
            for (int i=3; i<(lines.size()-2); i++)
            {
                String[] parts = lines.get(i).trim().split("\\s+");
            	if (lines.get(i).matches("^ -----------------.*$"))
            	{
            		break;
            	}
                String el = AtomUtils.getElementalSymbol(Integer.parseInt(parts[1]));
                Point3d p3d = new Point3d(Double.parseDouble(parts[3]),
                                          Double.parseDouble(parts[4]),
                                          Double.parseDouble(parts[5]));
                IAtom atm = new Atom(el,p3d);
                mol.addAtom(atm);
            }
        }
        else
        {
            Terminator.withMsgAndStatus("ERROR! Cannot understant syntax in "
                                        + "XYZ coords block. Please report to "
                                        + "the authors.",-1);
        }

        return mol;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a string summarizing the results of the evaluation 
     * @return a summary of the results of the analysis of the output
     */

    public String getResultsAsString()
    {
        String str = "File:" + inFile + " Steps:" + numSteps + " NormalTerm:"
                        + normalTerminated + " Error:";
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
