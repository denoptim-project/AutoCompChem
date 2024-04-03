package autocompchem.chemsoftware.vibmodule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.AtomType;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond.Order;

import com.google.common.math.StatsAccumulator;

import autocompchem.atom.AtomUtils;
import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.ChemSoftInputWriter;
import autocompchem.chemsoftware.ChemSoftOutputReader;
import autocompchem.chemsoftware.orca.OrcaConstants;
import autocompchem.constants.ACCConstants;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.files.FileAnalyzer;
import autocompchem.files.FileFingerprint;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.modeling.forcefield.EquilibriumValue;
import autocompchem.modeling.forcefield.ForceConstant;
import autocompchem.modeling.forcefield.ForceFieldConstants;
import autocompchem.modeling.forcefield.ForceFieldParameter;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.intcoords.InternalCoord;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.smarts.ManySMARTSQuery;
import autocompchem.smarts.MatchingIdxs;
import autocompchem.smarts.SMARTS;
import autocompchem.utils.NumberAwareStringComparator;
import autocompchem.worker.Task;
import autocompchem.worker.Worker;

/**
 * Reader for VibModule output data files. This class implements all
 * the software specific features that are needed to extract information
 * from the output of VibModule. 
 * The rest of the functionality is in the superclass
 * {@link ChemSoftOutputReader}.
 * 
 * @author Marco Foscato
 */
public class VibModuleOutputReader extends ChemSoftOutputReader
{

    /**
     * Storage of SMARTS queries
     */
    private Map<String,String> smarts = new HashMap<String,String>();

    /**
     * Storage of options associated with SMARTS queries
     */
    private Map<String,List<String>> smartsOpts = 
    		new HashMap<String,List<String>>();

    /**
     * Label used to identify single-atom smarts in the smarts reference name
     */
    private static final String SUBRULELAB = "_p";

    /**
     * Root of the smarts reference names
     */
    private static final String MSTRULEROOT = "smarts ";

    /**
     * Unique counter for SMARTS reference names
     */
    private final AtomicInteger iNameSmarts = new AtomicInteger(0);
    
    /**
     * Counters of intermal coordinates by type
     */
    private Map<String,Integer> vmICcounter = new HashMap<String,Integer>();

    /**
     * List of internal coordinates in VibModule file
     */
    private  List<InternalCoord> vmICs = new ArrayList<InternalCoord>();

    /**
     * List of force field constants in VibModule file
     */
    private  List<Double> vmFFKs = new ArrayList<Double>();

    /**
     * List of force field parameters generated from VibModule file
     */
    private  List<ForceFieldParameter> vmFFPars = 
    		new ArrayList<ForceFieldParameter>();
   
    /**
     * String defining the task of analyzing VibModule output files.
     */
    public static final String EXTRACTVIBMODULEFORCECONSTANTSTASKNAME = 
    		"extractVibModuleForceConstants";

    /**
     * Task about analyzing VibModule output files/folders.
     */
    public static final Task EXTRACTVIBMODULEFORCECONSTANTSTASK;
    static {
    	EXTRACTVIBMODULEFORCECONSTANTSTASK = Task.make(
    			EXTRACTVIBMODULEFORCECONSTANTSTASKNAME);
    }
    
//------------------------------------------------------------------------------

    @Override
    public Set<Task> getCapabilities() {
        return Collections.unmodifiableSet(new HashSet<Task>(
             Arrays.asList(EXTRACTVIBMODULEFORCECONSTANTSTASK)));
    }

//------------------------------------------------------------------------------

    @Override
    public Worker makeInstance(Job job) {
        return new VibModuleOutputReader();
    }

//------------------------------------------------------------------------------

    /**
     * This override is to process the input to detect the model name
     * when there is only one model to analyze.
     */

    public void initialize()
    {
    	super.initialize();

        if (params.contains(VibModuleConstants.PARINTCOORDBYSMARTS))
        {
            String all = params.getParameter(
            		VibModuleConstants.PARINTCOORDBYSMARTS).getValue().toString();
            this.smarts.putAll(getNamedICSMARTS(all));
            this.smartsOpts.putAll(getOptsForNamedICSMARTS(all,this.smarts));
        }
        
        // We remove any task that the ChemSoftware would add since we used the
        // CONNECTIVITYTEMPLATE keyword
        analysisGlobalTasks.clear();
    }
    
//------------------------------------------------------------------------------

    @Override
    public String getKnownInputDefinition() {
        return "inputdefinition/VibModuleOutputReader.json";
    }
    
//------------------------------------------------------------------------------
    /**
     * Read options and values associated with SMARTS queries.
     * This method collects all non-single-atom SMARTS strings found in a
     * given string. The string is
     * assumed to contain one or more lines (i.e., newline-character separated)
     * and each line to contain from 2 to 4 space-separated single-atom SMARTS.
     * @param allLines the string collecting all lines and including newline
     * characters
     * @param smarts the map of previously red SMARTS queries for which this
     * method is collecting the options
     * @return the map of all named details. Naming is based on incremental and
     * unique indexing where an integer index is used to identify the list of
     * strings red from the same line.
     */

     private Map<String,List<String>> getOptsForNamedICSMARTS(
    		  String allLines, Map<String,String> smarts)
     {
         List<String> sortedMasterNames = getSortedSMARTSRefNames(smarts);

         Map<String,List<String>> map = new HashMap<String,List<String>>();
         if (verbosity > 1)
         {
             System.out.println(" Importing options for IC-identifying SMARTS");
         }
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

    private Map<String,String> getNamedICSMARTS(String allLines)
    {
        Map<String,String> map = new HashMap<String,String>();
        if (verbosity > 1)
        {
            System.out.println(" Importing SMARTS to identify ICs");
        }
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
     * Performs any of the analysis tasks set upon initialization.
     */

    @Override
    public void performTask()
    {   
    	// Effectively, we just read the log of a VibModule job
      	analyzeFiles();
      	
      	// Extract and process desired force field parameters
      	extractForceFonstants();
      	
        if (exposedOutputCollector != null)
        {
        	exposeOutputData(new NamedData(MATCHESTOTEXTQRYSFORPERCEPTION, 
        			perceptionTQMatches));
        	exposeOutputData(new NamedData(ChemSoftConstants.JOBOUTPUTDATA, 
        			stepsData));
        	exposeOutputData(new NamedData(ChemSoftConstants.SOFTWAREID, 
        			getSoftwareID()));
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
     * We parse the log from VibModule.
     */
    
    @Override
    protected void readLogFile(LogReader reader) throws Exception
    {
        String linee = null;
        Integer lineNum = -1;
        while ((linee = reader.readLine()) != null)
        {
        	lineNum++;
        	if (linee.matches(".*" + VibModuleConstants.NORMALCOMPLSTATUS+ ".*"))
        	{
                normalTerminated = true;
        	}
        	
        	if (linee.matches(".*" + VibModuleConstants.TITSTRSEC+ ".*"))
        	{
        		lineNum = lineNum + readBlockOfIntCoors(reader, 
        				VibModuleConstants.TYPSTR);
        	}
        	
        	if (linee.matches(".*" + VibModuleConstants.TITBENDSEC+ ".*"))
        	{
        		lineNum = lineNum + readBlockOfIntCoors(reader, 
        				VibModuleConstants.TYPBND);
        	}
        	
        	if (linee.matches(".*" + VibModuleConstants.TITOOPSEC+ ".*"))
        	{
        		lineNum = lineNum + readBlockOfIntCoors(reader, 
        				VibModuleConstants.TYPOOP);
        	}
        	
        	if (linee.matches(".*" + VibModuleConstants.TITTORSEC+ ".*"))
        	{
        		lineNum = lineNum + readBlockOfIntCoors(reader, 
        				VibModuleConstants.TYPTOR);
        	}
        	
        	if (linee.matches(".*" + VibModuleConstants.TITFFPARAMS+ ".*"))
        	{
        		lineNum = lineNum + readBlockOfIntCoors(reader, 
        				VibModuleConstants.PARAMDATA);
            }
        }
        
        if (verbosity>0)
        {
	        for (String key : vmICcounter.keySet())
	        {
	        	if (key.equals(VibModuleConstants.PARAMDATA))
	        		continue;
	        	System.out.println(" Found " + vmICcounter.get(key) 
	        		+ " ICs of type " + key);  
	        }
	        System.out.println(" Total number of parsed parameters: " 
	        		+ vmICcounter.get(VibModuleConstants.PARAMDATA));
	        System.out.println(" Total internal coordinates: " 
	        		+ vmICcounter.size());
	        System.out.println(" Total force field parameters: " 
	        		+ vmFFKs.size());
        }
    }
    
//------------------------------------------------------------------------------
    
    private int readBlockOfIntCoors(LogReader reader, String type) 
    		throws IOException
    {
    	int lineNum = 0;
		int skipped = -1;
		String line = null;
		while ((line = reader.readLine()) != null)
        {
        	lineNum++;
        	skipped++;
        	if (skipped<1)
        	{
        		continue;
        	}
        	if (line.trim().equals(""))
        	{
        		break;
        	} else {
        		registerIntCoordFoundInFile(line, type);
        	}
        }
		return lineNum;
    }
    
//------------------------------------------------------------------------------
    
    private void registerIntCoordFoundInFile(String line, String type)
    {
        String lineUp = line.toUpperCase();
        String[] words = lineUp.trim().split("\\s+");
        String msg = "";
    	switch (type)
        {
            case VibModuleConstants.TYPSTR:
            {
                if (words.length != 3)
                {
                    msg = "Unexpected format in " + type 
                          + "-type line '" + line + "'.";
                    Terminator.withMsgAndStatus("ERROR! " + msg,-1);
                }
                double value = 0.0;
                try 
                {
                    value = Double.parseDouble(words[2]);
                } 
                catch (Throwable t)
                {
                    msg = "Unable to convert '" + words[2] + "' to "
                          + "a double. Check line '" + line + "'.";
                    Terminator.withMsgAndStatus("ERROR! " + msg,-1);
                }
                String[] p = words[1].split(VibModuleConstants.BNDSTR);
                if (p.length != 2)
                {
                    msg = "Unexpected atom index format in " + type
                          + "-type line '" + line + "'.";
                    Terminator.withMsgAndStatus("ERROR! " + msg,-1);
                }
                // WARNING! Here we change from 1- to 0-based atom index
                int idA = Integer.parseInt(
                                     p[0].replaceAll("[^\\d.]","")) - 1;
                int idB = Integer.parseInt(
                                     p[1].replaceAll("[^\\d.]","")) - 1;
                // WARNING! the name of the IC remains 1-based!!!!!
                InternalCoord ic = new InternalCoord(words[0],value,
                         new ArrayList<Integer>(Arrays.asList(idA,idB)),
                                                                  type);
                
                vmICs.add(ic);
                increaseCountOfICs(VibModuleConstants.TYPSTR);

                if (verbosity > 1)
                {
                    System.out.println("Found IC: "+ic);
                }
                break;
            }

            case VibModuleConstants.TYPBND:
            {
                words = line.trim().replaceAll(
                           VibModuleConstants.BNDSTR," ").split("\\s+");
                if (words.length != 5)
                {
                    msg = "Unexpected format in " + type
                          + "-type line '" + line + "'.";
                    Terminator.withMsgAndStatus("ERROR! " + msg,-1);
                }
                double value = 0.0;
                try
                {
                    value = Double.parseDouble(words[4]);
                }
                catch (Throwable t)
                {
                    msg = "Unable to convert '" + words[4] + "' to "
                          + "a double. Check line '" + line + "'.";
                    Terminator.withMsgAndStatus("ERROR! " + msg,-1);
                }
                // WARNING! Here we change from 1- to 0-based atom index
                int idA = Integer.parseInt(
                                 words[1].replaceAll("[^\\d.]","")) - 1;
                int idB = Integer.parseInt(
                                 words[2].replaceAll("[^\\d.]","")) - 1;
                int idC = Integer.parseInt(
                                 words[3].replaceAll("[^\\d.]","")) - 1;
                InternalCoord ic = new InternalCoord(words[0],value,
                     new ArrayList<Integer>(Arrays.asList(idA,idB,idC)),
                                                                  type);
                
                vmICs.add(ic);
                increaseCountOfICs(VibModuleConstants.TYPBND);

                if (verbosity > 1)
                {
                    System.out.println("Found IC: "+ic);
                }
                break;
            }
            case VibModuleConstants.TYPOOP:
            {
                if (words.length != 7)
                {
                    msg = "Unexpected format in " + type
                          + "-type line '" + line + "'.";
                    Terminator.withMsgAndStatus("ERROR! " + msg,-1);
                }
                // WARNING! Here we change from 1- to 0-based atom index
                int idA = Integer.parseInt(
                                 words[1].replaceAll("[^\\d.]","")) - 1;
                int idB = Integer.parseInt(
                                 words[2].replaceAll("[^\\d.]","")) - 1;
                int idC = Integer.parseInt(
                                 words[3].replaceAll("[^\\d.]","")) - 1;
                int idD = Integer.parseInt(
                                 words[6].replaceAll("[^\\d.]","")) - 1;
                //WARNING! Internal convention: the first three IDs
                // are sorted and the 4th is the central atom
                ArrayList<Integer> atmIDs = new ArrayList<Integer>(
                                            Arrays.asList(idA,idB,idC));
                Collections.sort(atmIDs);
                atmIDs.add(idD);
                InternalCoord ic = new InternalCoord(words[0], 0.0,
                                                          atmIDs, type);
                
                vmICs.add(ic);
                increaseCountOfICs(VibModuleConstants.TYPOOP);

                if (verbosity > 1)
                {
                    System.out.println("Found IC: "+ic);
                }
                break;
            }
            case VibModuleConstants.TYPTOR:
            {
                String icID = words[0];
                double value = 0.0;
                try
                {
                    value = Double.parseDouble(words[words.length-1]);
                }
                catch (Throwable t)
                {
                    msg = "Unable to convert '" + words[words.length-1]
                          + "' to a double. Check line '" + line + "'.";
                    Terminator.withMsgAndStatus("ERROR! " + msg,-1);
                }
                increaseCountOfICs(VibModuleConstants.TYPTOR);
                String allIDs = "";
                for (int j=1; j<(words.length-1); j++)
                {
                    allIDs = allIDs + " " +words[j];
                }
                String[] sides = allIDs.trim().split(
                                             VibModuleConstants.BNDSTR);
                String[] leftSide = sides[0].split("\\s+");
                String[] rightSide = sides[1].trim().split("\\s+");
                int idB = Integer.parseInt(leftSide[
                       leftSide.length-1].replaceAll("[^\\d.]","")) - 1;
                int idC = Integer.parseInt(
                             rightSide[0].replaceAll("[^\\d.]","")) - 1;
                for (int li=0; li<(leftSide.length-1); li++)
                {
                    for (int ri=1; ri<(rightSide.length); ri++)
                    {
                        // WARNING! Here we change from 1- to 0-based
                        int idA = Integer.parseInt(
                             leftSide[li].replaceAll("[^\\d.]","")) - 1;
                        int idD = Integer.parseInt(
                            rightSide[ri].replaceAll("[^\\d.]","")) - 1;

                        InternalCoord ic = new InternalCoord(icID,value,
                                                 new ArrayList<Integer>(
                                        Arrays.asList(idA,idB,idC,idD)),
                                                                  type);
                        
                        vmICs.add(ic);
                        increaseCountOfICs("Generated-" 
                        		+ VibModuleConstants.TYPTOR);

                        if (verbosity > 1)
                        {
                            System.out.println("Found IC: "+ic);
                        }
                    }
                }
                break;
            }
            case VibModuleConstants.PARAMDATA:
            {
                if (words.length < 2 || words.length > 6)
                {
                    msg = "Unexpected format in force field params "
                          + " line '" + line + "'.";
                    Terminator.withMsgAndStatus("ERROR! " + msg,-1);
                }
                for (int ip=1; ip<words.length; ip+=2)
                {
                    double value = 0.0;
                    try 
                    {
                        value = Double.parseDouble(words[ip]);
                    }
                    catch (Throwable t)
                    {
                        msg = "Unable to convert '" + words[ip] + "' "
                          + "into a double. Check line '" + line + "'.";
                        Terminator.withMsgAndStatus("ERROR! " + msg,-1);
                    }
                    int icID = Integer.parseInt(words[ip-1]);
                    if (icID != (vmFFKs.size()+1))
                    {
                        msg = "Possible unsequential read of params. "
                              + "Report this bug to the authors.";
                        Terminator.withMsgAndStatus("ERROR! " + msg,-1);
                    }
                    vmFFKs.add(value);
                    increaseCountOfICs(VibModuleConstants.PARAMDATA);
                }
            }
        }
    }
    
//------------------------------------------------------------------------------
    
    private void increaseCountOfICs(String type)
    {
    	if (vmICcounter.containsKey(type))
    	{
    		vmICcounter.put(type, vmICcounter.get(type)+1);
    	} else {
    		vmICcounter.put(type, 1);
    	}
    }

//------------------------------------------------------------------------------
      
    public void extractForceFonstants()
    {
    	// Preliminary read the topology of the chemical system
    	if (connectivityTemplate==null)
    		Terminator.withMsgAndStatus("No Conenctivity templete provided.", -1);
        IAtomContainer mol = connectivityTemplate;

        //Identify selected internal coordinates
        ManySMARTSQuery msq = new ManySMARTSQuery(mol, smarts, verbosity);
        if (msq.hasProblems()) 
        {
            String cause = msq.getMessage();
            Terminator.withMsgAndStatus("ERROR! " + cause,-1);
        }

        // Reorganize lists to group single-atom smarts of the same IC
        Map<String,List<List<Integer>>> allIcMatched =
                            new HashMap<String,List<List<Integer>>>();
        for (String icRuleName : getSortedSMARTSRefNames(smarts))
        {
            boolean skipRule =false;
            String preStr = icRuleName + SUBRULELAB;
            List<List<Integer>> componentsIcRule = 
            		new ArrayList<List<Integer>>(4);
            for (int i=0; i<4; i++)
            {
                componentsIcRule.add(null);
            }
            for (String key : smarts.keySet())
            {
                if (!key.startsWith(preStr))
                {
                    continue;
                }
                int pos = Integer.parseInt(key.substring(preStr.length()));
                if (msq.getNumMatchesOfQuery(key) == 0)
                {
                    skipRule = true;
                    if (verbosity > 0)
                    {
                        System.out.println("WARNING! No match for SMARTS query "
                         + smarts.get(key) + " in molecule "
                         + MolecularUtils.getNameOrID(mol) + ". Skipping "
                         + "Int. Coord. definition '" + icRuleName + "'.");
                    }
                    break;
                }

                //Get matches for this SMARTS query
                MatchingIdxs matches =  msq.getMatchingIdxsOfSMARTS(key);
                
                // Collect all matches
                List<Integer> allAtmsIds = new ArrayList<Integer>();
                for (List<Integer> innerList : matches)
                {
                	allAtmsIds.addAll(innerList);
                }
                componentsIcRule.set(pos,allAtmsIds);
            }
            if (skipRule)
            {
                continue;
            }
            allIcMatched.put(icRuleName,componentsIcRule);
        }
        

        // Identify the tuples of atom indeces that correspond to the selected
        // types of force field terms
        if (verbosity > 0)
        {
            System.out.println(" IC matched by SMARTS queries:");
        }
        Map<String,ArrayList<InternalCoord>> selectedICs = 
                                 new HashMap<String,ArrayList<InternalCoord>>();
        for (String rulKey : allIcMatched.keySet())
        {
            //OOPs are dealt with elsewhere
            if (smartsOpts.get(rulKey).get(0).equals(VibModuleConstants.TYPOOP))
            {
                continue;
            }
            List<List<Integer>> componentsIcRule = allIcMatched.get(rulKey);
            for (Integer idA : componentsIcRule.get(0))
            {
                IAtom atmA = mol.getAtom(idA);
                for (Integer idB : componentsIcRule.get(1))
                {
                    IAtom atmB = mol.getAtom(idB);
                    if (!mol.getConnectedAtomsList(atmA).contains(atmB)
                        || atmA == atmB)
                    {
                        continue;
                    }
                    if (componentsIcRule.get(2) != null)
                    {
                        for (Integer idC : componentsIcRule.get(2))
                        {
                            IAtom atmC = mol.getAtom(idC);
                            if (!mol.getConnectedAtomsList(atmB).contains(atmC)
                                || atmA == atmC || atmB == atmC)
                            {
                                continue;
                            }
                            if (componentsIcRule.get(3) != null)
                            {
                                for (Integer idD : componentsIcRule.get(3))
                                {
                                    IAtom atmD = mol.getAtom(idD);
                                    if (atmA == atmD || atmB == atmD
                                         || atmC == atmD)
                                    {
                                        continue;
                                    }

                                    if(!mol.getConnectedAtomsList(
                                                           atmB).contains(atmD)
                                        && !mol.getConnectedAtomsList(
                                                         atmC).contains(atmD))
                                    {
                                        continue;
                                    }

                                    processSingleCombinationOfIDs(rulKey, 
                                                                    selectedICs,
                                                         new ArrayList<Integer>(
                                               Arrays.asList(idA,idB,idC,idD)));
                                }
                            }
                            else
                            {
                                processSingleCombinationOfIDs(rulKey, 
                                                                    selectedICs,
                                                         new ArrayList<Integer>(
                                                   Arrays.asList(idA,idB,idC)));
                            }
                        }
                    }
                    else
                    {
                        processSingleCombinationOfIDs(rulKey, selectedICs,
                                                         new ArrayList<Integer>(
                                                       Arrays.asList(idA,idB)));
                    }
                }
            }
        }
        for (String rulKey : allIcMatched.keySet())
        {
            //only for OOPs
            if (!smartsOpts.get(rulKey).get(0).equals(
                                                     VibModuleConstants.TYPOOP))
            {
                continue;
            }
            List<List<Integer>> componentsIcRule = allIcMatched.get(rulKey);
            if (componentsIcRule.get(3) != null)
            {
                for (Integer idA : componentsIcRule.get(0))
                {
                    IAtom atmA = mol.getAtom(idA);
                    for (Integer idB : componentsIcRule.get(1))
                    {
                        IAtom atmB = mol.getAtom(idB);
                        if (atmA == atmB)
                        {
                            continue;
                        }
                        for (Integer idC : componentsIcRule.get(2))
                        {
                            IAtom atmC = mol.getAtom(idC);
                            if (atmA == atmC || atmB == atmC)
                            {
                                continue;
                            }
                            for (Integer idD : componentsIcRule.get(3))
                            {
                                IAtom atmD = mol.getAtom(idD);
                                if (atmA == atmD || atmB == atmD || atmC==atmD)
                                {
                                    continue;
                                }
                                List<IAtom> nbrsD = mol.getConnectedAtomsList(
                                                                          atmD);
                                if (nbrsD.contains(atmA) && nbrsD.contains(atmB)
                                    && nbrsD.contains(atmC))
                                {
                                    //WARNING! Internal convention: the first
                                    // three IDs are sorted and the 4th is the
                                    // central atom
                                    ArrayList<Integer> atmIDs = 
                                                         new ArrayList<Integer>(
                                                    Arrays.asList(idA,idB,idC));
                                    Collections.sort(atmIDs);
                                    atmIDs.add(idD);
                                    processSingleCombinationOfIDs(rulKey,
                                                                    selectedICs,
                                                                        atmIDs);
                                }
                            }
                        }
                    }
                }
            }
            else
            {
                String msg = "Rules meant for out-of-plane must match 4 atoms. "
                		+ "Check rule '" + rulKey + "'.";
                Terminator.withMsgAndStatus("ERROR! " + msg,-1);
            }
        }

        //Extract the force field parameters and get statistics
        for (String termName : selectedICs.keySet())
        {
            //Get the force constant and eq. value
            ArrayList<InternalCoord> ics = selectedICs.get(termName);
            StatsAccumulator saFrcKst = new StatsAccumulator();
            StatsAccumulator saEqVal = new StatsAccumulator();
            String type = "";
            int usedVals = 0;
            for (InternalCoord ic : ics)
            {
                //Compare the IDs of the IC matched by the SMARTS (ic)
                // with the IC for which VibModule got a force const. (icff)
                for (InternalCoord icff : vmICs)
                {
                    if (icff.compareIDs(ic.getIDs()))
                    {
                        // WARNING the name of the IC is the index+1
                        int icIndex = Integer.parseInt(icff.getName()) - 1;
                        double fk = vmFFKs.get(icIndex);
                        type = ic.getType();
                        if (verbosity > 1) 
                        {
                             System.out.println(" Force constant for "  
                                + type + " term " + ic.getIDs() 
                                + " taken from VibModule IC " + (icIndex+1));
                        }
                        saFrcKst.add(fk);
                        saEqVal.add(icff.getValue());
                        usedVals++;
                    }
                }
            }

            if (usedVals == 0)
            {
                if (verbosity > 0)
                {
                    System.out.println(" WARNING: no internal coord. found in "
                                   + "vibrational analysis with data for term '"
                                   + termName + "="
                                   + getAllSmartsWithRef(termName) + "'. ");
                }
                continue;
            }

            if (verbosity > 1)
            {
                StringBuilder sb = new StringBuilder();
                sb.append(" Term '").append(termName).append("' \n");
                sb.append(String.format(Locale.ENGLISH,
                		"  Min.     K: %f Eq. %f \n",
                                                 saFrcKst.min(),saEqVal.min()));
                sb.append(String.format(Locale.ENGLISH,
                		"  Max.     K: %f Eq. %f \n",
                                                 saFrcKst.max(),saEqVal.max()));
                sb.append(String.format(Locale.ENGLISH,
                		"  Mean     K: %f Eq. %f \n",
                                               saFrcKst.mean(),saEqVal.mean()));
                sb.append(String.format(Locale.ENGLISH,
                		"  Std.Dev. K: %f Eq. %f \n",
                                        saFrcKst.populationStandardDeviation(),
                                        saEqVal.populationStandardDeviation()));
                System.out.println(sb.toString());
            }
            
            //Assemble the FF parameter
            ForceFieldParameter ffPar = new ForceFieldParameter(termName,type);
            ffPar.addForceConstant(new ForceConstant(termName,"notype",
                                                    saFrcKst.mean(),"nounits"));
            ffPar.addEquilibriumValue(new EquilibriumValue(termName,
                                                     saEqVal.mean(),"nounits"));
            for (int i=0; i<4; i++)
            {
                String atSmartKey = termName + SUBRULELAB + i;
                if (smarts.keySet().contains(atSmartKey))
                {
                    AtomType at = new AtomType("X");
                    at.setProperty(ForceFieldConstants.SMARTSQUERYATMTYP,
                                                        smarts.get(atSmartKey));
                    ffPar.addAtomType(at);
                }
            }

            //Finally store the force field parameter
            vmFFPars.add(ffPar);
        }
        
        //Print summary of results
        if (verbosity > -1)
        {
            System.out.println(" FF-Parameters extracted:");
            for (ForceFieldParameter ffPar : vmFFPars)
            {
                System.out.println(" -> " + ffPar.toSimpleString());
            }
            System.out.println(" ");
        }
    }

  //------------------------------------------------------------------------------

      /**
       * Process a single set of atom IDs that represent a candidate internal
       * coordinate. For the n-tupla of atom indexes, we evaluate if the reordered
       * list of has been used before and, if not, we add it to the list of
       * matched internal coordinates.
       * @param rule name of the IC-matching rule
       * @param selectedICs map of selected int. coords. grouped according to
       * the name of the matching rule
       * @param tupla the tupla of atom indexes
       */

      private void processSingleCombinationOfIDs(String name, 
                                 Map<String,ArrayList<InternalCoord>> selectedICs,
                                                         ArrayList<Integer> tupla)
      {
          boolean alreadyUsed = false;
          String icType = smartsOpts.get(name).get(0);
          for (Map.Entry<String,ArrayList<InternalCoord>> entry : 
                                                           selectedICs.entrySet())
          {
              for (InternalCoord oldIc : entry.getValue())
              {
                  if (oldIc.compareIDs(tupla))
                  {
                      alreadyUsed = true;
                      break;
                  }
              }
              if (alreadyUsed)
              {
                  break;
              }
          }
          if (!alreadyUsed)
          {
              InternalCoord newIc = new InternalCoord("noname",0.0,tupla,icType);
              if (verbosity > 0)
              {
                  System.out.println(" -> " + icType + "-type IC defined by "
                                     + tupla.size() + "-tupla " + tupla
                                     + " matched rule '" + name + "'.");
              }
              if (selectedICs.containsKey(name))
              {
                  selectedICs.get(name).add(newIc);
              }
              else
              {
                  ArrayList<InternalCoord> lst = new ArrayList<InternalCoord>();
                  lst.add(newIc);
                  selectedICs.put(name,lst);
              }
          }
          else
          {
              if (verbosity > 1)
              {
                  System.out.println(" " + tupla.size() + "-tupla " + tupla 
                                              + " matches rule '" + name + "'"
                                              + " but is already used by another"
                                              + " IC.");
              }
          }
      }

    

  //------------------------------------------------------------------------------

      /**
       * get the smarts combination corrsponding to the reference name
       */

      private String getAllSmartsWithRef(String refName)
      {
          StringBuilder sb = new StringBuilder();
          for (int i=0; i<4; i++)
          {
              String k = refName+SUBRULELAB+i;
              if (smarts.get(k) != null)
              {
                  sb.append(smarts.get(k)).append(" ");
              }
          }
          return sb.toString();
      }

//------------------------------------------------------------------------------

	@Override
	protected Set<FileFingerprint> getOutputFingerprint() 
	{
		Set<FileFingerprint> conditions = new HashSet<FileFingerprint>();
		conditions.add(new FileFingerprint(".", 5, "^ VobModule version.*$"));
		return conditions;
	}

//------------------------------------------------------------------------------

	@Override
	protected String getSoftwareID() {
		return "VibModule";
	}

//------------------------------------------------------------------------------

	@Override
	protected ChemSoftInputWriter getChemSoftInputWriter() {
		return null;
	}
		
//-----------------------------------------------------------------------------
    
}
