package autocompchem.chemsoftware.orca;

import java.util.ArrayList;
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

import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.errorhandling.ErrorManager;
import autocompchem.chemsoftware.errorhandling.ErrorMessage;
import autocompchem.constants.ACCConstants;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.modeling.compute.CompChemComputer;
import autocompchem.molecule.connectivity.ConnectivityUtils;
import autocompchem.run.Terminator;
import autocompchem.worker.TaskID;
import autocompchem.worker.Worker;

/**
 * Reader and analyser for computational chemistry software output files.
 * 
 * @author Marco Foscato
 */

public class OrcaOutputHandler extends Worker
{
    /**
     * Declaration of the capabilities of this subclass of {@link Worker}.
     */
    public static final Set<TaskID> capabilities =
            Collections.unmodifiableSet(new HashSet<TaskID>(
                    Arrays.asList(TaskID.EVALUATEORCAOUTPUT)));
    
    /**
     * Name of the output file from comp.chem. software, i.e., 
     * the input for this class.
     */
    private String inFile;

    /**
     * Name of the output file for tasks run by this class.
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
    private ArrayList<Integer> selectedVibModes;

    /**
     * Pathname where to print vibrational modes
     */
    private String outFileVibModes;

    /**
     * Verbosity level
     */
    private int verbosity = 1;

    /**
     * Number steps/jobs/taks found in job under analysis
     */
    private int numSteps;

    /**
     * Flag recording normal termination of job under analysis
     */
    private boolean normalTerminated;

    /**
     * Flag requiring extraction of the energy
     */
    private boolean analyseEnergy = false;

    /**
     * Flag requiring analysis of imaginary frequencies
     */
    private boolean checkImgFreq = false;

    /**
     * Flag requiring collection of all vibrational modes 
     */
    private boolean readFrequencies = false;

    /**
     * Data structure containing the vibrational modes as a list
     */
    private ArrayList<ArrayList<Double>> vibModes;

    /**
     * Flag requiring calculation of vibrational S by quasi-harmonic approx.
     */
    private boolean calcQHarmVibS = false;
    
    /**
     * Lowest value for non-zero frequencies
     */
    private double lowestFreq = ChemSoftConstants.MINFREQ;
    
    /**
     * Lowest value for non-zero frequencies
     */
    private double qhThrsh = ChemSoftConstants.QHTHRSHFREQ;

    /**
     * Lowest value for imaginary modes
     */
    private double imThrsh = ChemSoftConstants.IMGFREQTHRSH;

    /**
     * Array of vibrational frequencies read from output file
     */
    private ArrayList<Double> vibFreq = new ArrayList<Double>();

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
     * Initialise the worker according to the parameters loaded by constructor.
     */

    @Override
    public void initialize()
    {
        //Define verbosity
        String vStr = params.getParameter("VERBOSITY").getValue().toString();
        this.verbosity = Integer.parseInt(vStr);

        if (verbosity > 0)
            System.out.println(" Adding parameters to ChemSoftwareOutputHandler");

        //Get and check the input file (which is an output from a comp.chem. software)
        this.inFile = params.getParameter("INFILE").getValue().toString();
        FileUtils.foundAndPermissions(this.inFile,true,false,false);

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
                    this.checkImgFreq = true;
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
          case EVALUATEORCAOUTPUT:
        	  evaluate();
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
     * Evaluates the given output file.
     */

    private void evaluate()
    {
    	//TODO
    	/*
        //Read the outfile and collect counts and line numbers
        ArrayList<String> patterns = new ArrayList<String>();
        patterns.add(ChemSoftwareConstants.OUTINITSTR);
        patterns.add(ChemSoftwareConstants.OUTNORMALENDSTR);
        ArrayList<ArrayList<Integer>> countsAndLineNum = 
                                           FileAnalyzer.count(inFile,patterns);
        int indexOfCounts = countsAndLineNum.size() - 1;
        ArrayList<Integer> counts = countsAndLineNum.get(indexOfCounts);
        ArrayList<ArrayList<Integer>> lineNums = 
                                            new ArrayList<ArrayList<Integer>>();
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
                System.out.println(" output file file contains: "
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
                System.out.println(" output file file contains: "
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
        ArrayList<String> tail = IOtools.tailFrom(inFile,lastInit);

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
                patterns.add(ChemSoftwareConstants.OUTTOTDFTENERGY);
                patterns.add(ChemSoftwareConstants.OUTCORRH);
                patterns.add(ChemSoftwareConstants.OUTTOTS);
                patterns.add(ChemSoftwareConstants.OUTTEMP);
                if (calcQHarmVibS)
                {
                    patterns.add(ChemSoftwareConstants.OUTPROJFREQ);
                    patterns.add(ChemSoftwareConstants.OUTTRAS);
                    patterns.add(ChemSoftwareConstants.OUTROTS);
                    patterns.add(ChemSoftwareConstants.OUTVIBS);
                }
            }

            if (checkImgFreq)
            {
                patterns.add(ChemSoftwareConstants.OUTPROJFREQ);
            }

//TODO use method in IOtools
// Also, now the condition is always satisfied, but there might be cases
// chere we do not need to do it

            if (analyseEnergy || checkImgFreq)
            {
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
                    String key = ChemSoftwareConstants.OUTPROJFREQ;
                    if (matchesMap.containsKey(key))
                    {
                        vibModes = new ArrayList<ArrayList<Double>>();
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
                                    && Math.abs(freq) >= ChemSoftwareConstants.IMGFREQTHRSH)
                                {
                                    if (verbosity > 2)
                                    {
                                        System.out.println("Found img.freq.: "
                                                                        + freq);
                                    }
                                    imgFreqs.add(freq);
                                }
                                vibFreq.add(freq);

                                // Here we only add an empty mode: will be filled later
                                ArrayList<Double> mode = new ArrayList<Double>();
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
                    if (matchesMap.containsKey(ChemSoftwareConstants.OUTTOTDFTENERGY))
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
                                  ChemSoftwareConstants.OUTTOTDFTENERGY).get(
                                      matchesMap.get(
                                    ChemSoftwareConstants.OUTTOTDFTENERGY).size()-1));
                        String[] p = line.split("\\s+");
                        e = Double.parseDouble(p[5]);
                        res = "DFT Energy: " + e + " ";

                        if (matchesMap.containsKey(ChemSoftwareConstants.OUTTOTS))
                        {
                            line = tail.get(
                                     matchesMap.get(
                                      ChemSoftwareConstants.OUTCORRH).get(
                                       matchesMap.get(
                                        ChemSoftwareConstants.OUTCORRH).size()-1));
                            p = line.split("\\s+");
                            corrH = Double.parseDouble(p[9]);
                            res = res + corrH + " ";

                            line = tail.get(
                                       matchesMap.get(
                                        ChemSoftwareConstants.OUTTEMP).get(
                                         matchesMap.get(
                                          ChemSoftwareConstants.OUTTEMP).size()-1));
                            p = line.split("\\s+");
                            temp = Double.parseDouble(p[3].replaceAll("K",""));
                            res = res + temp + " ";

                            line = tail.get(
                                     matchesMap.get(
                                      ChemSoftwareConstants.OUTTOTS).get(
                                       matchesMap.get(
                                        ChemSoftwareConstants.OUTTOTS).size()-1));
                            p = line.split("\\s+");
                            corrS = Double.parseDouble(p[4]);
                            res = res + corrS + " ";

                            if (calcQHarmVibS)
                            {
                                line = tail.get(
                                         matchesMap.get(
                                          ChemSoftwareConstants.OUTTRAS).get(
                                           matchesMap.get(
                                            ChemSoftwareConstants.OUTTRAS).size()-1));
                                p = line.split("\\s+");
                                traS = Double.parseDouble(p[4]);
    
                                line = tail.get(
                                         matchesMap.get(
                                          ChemSoftwareConstants.OUTROTS).get(
                                           matchesMap.get(
                                            ChemSoftwareConstants.OUTROTS).size()-1));
                                p = line.split("\\s+");
                                rotS = Double.parseDouble(p[4]);
                            
                                line = tail.get(
                                         matchesMap.get(
                                          ChemSoftwareConstants.OUTVIBS).get(
                                           matchesMap.get(
                                            ChemSoftwareConstants.OUTVIBS).size()-1));
                                p = line.split("\\s+");
                                vibS = Double.parseDouble(p[4]);

                                vibS = CompChemComputer.vibrationalEntropyCorr(
                                                                vibFreq,
                                                                temp,
                                                                ChemSoftwareConstants.QHTHRSHFREQ,
                                                                ChemSoftwareConstants.IMGFREQTHRSH,
                                                                0.01,
                                                                verbosity);
                                corrS = traS + rotS + vibS;
                                res = res + String.format("%.2f ",corrS); 
                            }
                        }
                    }
                    else
                    {
                        res = res + " No dft energy found! ";
                    }
                }

                if (checkImgFreq)
                {
                    if (matchesMap.containsKey(ChemSoftwareConstants.OUTPROJFREQ))
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
        */
    	
        if (verbosity > 0)
        {
            System.out.println(" " + getResultsAsString());
        }
    }

//------------------------------------------------------------------------------

    /**
     * Extract the last geometry from the output file loaded in the handler
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
     * Extract the last geometry from an output file.
     * @param outFile pathname of the output file. 
     * @return the last geometry
     */

    public IAtomContainer extractLastOutputGeometry(String outFile)
    {
        this.inFile = outFile;
        return extractLastOutputGeometry();
    }

//------------------------------------------------------------------------------

    /**
     * Prints all the geometries found in the loaded output file.
     * All geometries
     * of all tasks are considered.
     */

    public void printTrajectory()
    {
        printTrajectory(false);
    }

//------------------------------------------------------------------------------

    /**
     * Prints all the optimised geometries found in the loaded output file.
     * All geometries of all tasks/steps/jobs are considered.
     */

    public void printOptTrajectory()
    {
        printTrajectory(true);
    }

//------------------------------------------------------------------------------

    /**
     * Prints geometries found in the loaded output file. 
     * All geometries
     * of all tasks are considered.
     * @param onlyOpt set to true to restrict to optimised geometries
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
     * Extract the last geometry from the last task found in the loaded 
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
            ArrayList<Double> mode = vibModes.get(vmId);
            for (int atmId=0; atmId<(mode.size()/3); atmId++)
            {
                sb.append(String.format(" %5.5f",mode.get(atmId*3)));
                sb.append(String.format(" %5.5f",mode.get(atmId*3+1)));
                sb.append(String.format(" %5.5f",mode.get(atmId*3+2)));
                sb.append(System.getProperty("line.separator"));
            }
        }
        IOtools.writeTXTAppend(outFileVibModes,sb.toString(),false);
        if (verbosity > 1)
        {
            System.out.println(" Vibrational mode " + selectedVibModes 
                + " written to '" + outFileVibModes + "'.");
        }
    }

//------------------------------------------------------------------------------

    /**
     * Analyse the output and identify the error (if this is known)
     * @param tail the portion of output file file corresponding to the last
     * step
     */

    private void identifyErrorMessage(ArrayList<String> tail)
    {
    	//TODO: replace with perceptron
    	
    }

//------------------------------------------------------------------------------

    /**
     * Method returning the number of {@link NWChemTask}s represented in the
     * output file provided.
     * @return the number of steps identified in NWChem .out file, which is 
     * the index (1 to n) of the last step.
     */

    public int getNumberOfSteps()
    {
        return numSteps;
    }

//------------------------------------------------------------------------------

    /**
     * Search the initial set of coordinates in a output file. For initial
     * coordinates we intend the Cartesian coordinates provided to NWChem  by
     * means of *.nw file and NOT taken from checkpoint file.
     * @return molecular definition as atoms and coords (no connectivity)
     */

    public IAtomContainer getInitialGeometry()
    {
        IAtomContainer mol = new AtomContainer();

        //TODO
        /*
        ArrayList<String> lines = FileAnalyzer.extractTxtWithDelimiters(inFile,
                                                ChemSoftwareConstants.OUTSTARTINITXYZ,
                                                ChemSoftwareConstants.OUTENDINITXYZ,
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
*/
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
    	TreeMap<String,ArrayList<String>> mapBlocks = new TreeMap<String,ArrayList<String>>();
    	
    	/*
        TreeMap<String,ArrayList<String>> mapBlocks =
                      FileAnalyzer.extractMapOfTxtBlocksWithDelimiters(inFile,
                                          new ArrayList<String>(Arrays.asList(
                   ChemSoftwareConstants.OUTSTARTXYZ,
                   ChemSoftwareConstants.OUTENDCONVGEOMOPTSTEP)),
                                          new ArrayList<String>(Arrays.asList(
                   ChemSoftwareConstants.OUTENDXYZ,
                   ChemSoftwareConstants.OUTENDGEOMOPTSTEP)),
                                                                          false,
                                                                          true);
*/
        ArrayList<IAtomContainer> molList = new ArrayList<IAtomContainer>();
        /*
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
        }*/

        return molList;
    }

//------------------------------------------------------------------------------

    /**
     * Search for all geometries
     * @return the set of molecules defined as atoms and coords 
     * (no connectivity)
     */

    public ArrayList<IAtomContainer> getAllGeometries()
    {
    	ArrayList<ArrayList<String>> blocks = new ArrayList<ArrayList<String>>();
    	//TODO
    	/*
        ArrayList<ArrayList<String>> blocks = 
                       FileAnalyzer.extractMultiTxtBlocksWithDelimiters(inFile,
                                          new ArrayList<String>(Arrays.asList(
                   ChemSoftwareConstants.OUTSTARTXYZ,ChemSoftwareConstants.OUTHESSTARTXYZ)),
                                          new ArrayList<String>(Arrays.asList(
                       ChemSoftwareConstants.OUTENDXYZ,ChemSoftwareConstants.OUTHESENDXYZ)),
                                                                          false,
                                                                          true);
        */                                                                  
        
        ArrayList<IAtomContainer> molList = new ArrayList<IAtomContainer>();
        /*
        for (ArrayList<String> singleBlock : blocks)
        {
            molList.add(getIAtomContainerFromXYZblock(singleBlock));
        }
        */
        return molList;
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
        	//TODO
        	/*
            str = str + " Error:";
            if (errorIsDecoded)
                str = str + actualEM.getName();
            else
                str = str + "Not Known";
                */
        }
        else
        {
            str = str + "\n " + analysisResult;
        }

        return str;
    }

//------------------------------------------------------------------------------

}
