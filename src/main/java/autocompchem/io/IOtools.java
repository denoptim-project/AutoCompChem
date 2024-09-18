package autocompchem.io;

/*   
 *   Copyright (C) 2014  Marco Foscato 
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.XYZReader;
import org.openscience.cdk.io.XYZWriter;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import autocompchem.atom.AtomUtils;
import autocompchem.chemsoftware.ChemSoftConstants;
import autocompchem.chemsoftware.ChemSoftOutputReader;
import autocompchem.chemsoftware.ChemSoftReaderWriterFactory;
import autocompchem.datacollections.NamedDataCollector;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.BufferedTranslator;
import autocompchem.files.FileUtils;
import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixConstants;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.text.TextAnalyzer;
import autocompchem.text.TextBlock;
import autocompchem.utils.StringUtils;
import autocompchem.worker.Task;
import autocompchem.worker.WorkerConstants;


/**
 * Tools for Input/Output: reading/writing of files.
 * 
 * @author Marco Foscato
 */

public class IOtools
{
    public static String newline = System.getProperty("line.separator");
    
//------------------------------------------------------------------------------

    /**
     * Reads TXT files (Suitable for small files - do NOT use this for huge
     * files!)
     *
     * @param file to be read
     * @return all the lines as a list.
     */

    public static List<String> readTXT(File file)
    {
        return readTXT(file, false);
    }

//------------------------------------------------------------------------------

    /**
     * Reads TXT files (Suitable for small files - do NOT use this for huge 
     * files!)
     * @param file file to be read
     * @param escape set tu <code>true</code> to escape special characters
     * @return all the lines as a list.
     */

    public static List<String> readTXT(File file, boolean escape)
    {
        List<String> allLines = new ArrayList<String>();
        BufferedReader buffRead = null;
        try {
            buffRead = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = buffRead.readLine()) != null)
            {
                if (escape)
                {
                    allLines.add(StringUtils.escapeSpecialChars(line));
                }
                else
                {
                    allLines.add(line);
                }
            }
        } catch (FileNotFoundException fnf) {
        	Terminator.withMsgAndStatus("File Not Found: " + file, -1);
        } catch (IOException ioex) {
        	Terminator.withMsgAndStatus("Error in reading file '" + file, 
        			-1 , ioex);
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (IOException ioex2) {
            	Terminator.withMsgAndStatus("Error in reading file '" + file, 
            			-1 , ioex2);
            }
        }
        return allLines;
    }

//------------------------------------------------------------------------------

    /**
     * Reads keyword-labelled lines in TXT files (Suitable for small files - do 
     * NOT use this for huge files!)
     *
     * @param filename file to be read
     * @param keyword label identifying the wanted lines
     * @return all the lines beginning with the <code>keyword</code> into a 
     * list.
     */

    public static List<String> readTXTKeyword(File file, String keyword)
    {
        List<String> allLines = new ArrayList<String>();
        BufferedReader buffRead = null;
        try {
            buffRead = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = buffRead.readLine()) != null)
            {
/*                
                String[] words = line.split("\\s+");
                if (words[0].equals(keyword))
*/
                if (line.startsWith(keyword))
                    allLines.add(line);
            }
        } catch (FileNotFoundException fnf) {
            Terminator.withMsgAndStatus("ERROR! File " + file 
                                + " not found!", -1);
        } catch (IOException ioex) {
        	Terminator.withMsgAndStatus("Error in reading file '" + file, 
        			-1 , ioex);
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (IOException ioex2) {
            	Terminator.withMsgAndStatus("Error in reading file '" + file, 
            			-1 , ioex2);
            }
        }
        return allLines;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Reads a JSON file
     * @param file the pathname to the file to read.
     * @param type the expected type.
     * @return the deserialized object.
     * @throws IOException
     */
    public static Object readJsonFile(File file, Type type) throws IOException
    {
    	Object result = null;
    	Reader br = null;
        try
        {
        	br = new BufferedReader(new FileReader(file));
            result = readJson(type, br);
        } catch (JsonSyntaxException jse) {
        	Terminator.withMsgAndStatus("ERROR! JSON file '" + file 
        			+ "' has illegal syntax: " + jse.getMessage(), -1);
        } finally 
        {
            if (br != null)
            {
                br.close();
            }
        }
        return result;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Reads a JSON file upon translation of some of its content.
     * @param file the pathname to the file to read.
     * @param type the expected type of object.
     * @param regexToReplace the pattern to replace, i.e., translate.
     * @param replacement the string to use instead of the matched pattern.
     * @return the object read from the file after translation of any 
     * instance of the pattern.
     * @throws IOException
     */
    public static Object readJsonFile(File file, Type type, 
    		String regexToReplace, String replacement) throws IOException
    {
    	Object result = null;
    	Gson reader = ACCJson.getReader();
    	Reader br = null;
        try
        {
        	br = new BufferedTranslator(new FileReader(file), regexToReplace, 
        			replacement);
            result = reader.fromJson(br, type);
        } catch (JsonSyntaxException jse) {
        	Terminator.withMsgAndStatus("ERROR! JSON file '" + file 
        			+ "' has illegal syntax: " + jse.getMessage(), -1);
        } finally 
        {
            if (br != null)
            {
                br.close();
            }
        }
        return result;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Reads a JSON object from a reader.
     * @param type the expected type.
     * @param reader the reader to read.
     * @return the object in the reader.
     */
    public static Object readJson(Type type, Reader reader) 
    		throws JsonIOException, JsonSyntaxException
    {
    	Gson gsonReader = ACCJson.getReader();
        return gsonReader.fromJson(reader, type);
    }

//------------------------------------------------------------------------------

    /**
     * Extract '<code>key|separator|value</code>' field in a properly formatted
     * text file.
     *
     * @param file the file to be read
     * @param separator string splitting the <code>key</code> from the 
     * <code>value</code>
     * @param commentLab string excluding test from being read (commented out)
     * @param start string identifying the beginning of a block of lines to be
     * read
     * @param end string identifying the end of a block of lines to be read
     * @return a table with all strings extracted according to the formatting
     * labels.
     */

    public static List<List<String>> readFormattedText(File file,
                        String separator, String commentLab,
                        String start, String end)
    {
        //Read file line by line
        List<String> lines = IOtools.readTXT(file);

        //Start interpretation of the formatted text
        List<List<String>> filledForm = TextAnalyzer.readKeyValue(
                lines,
                separator,
                commentLab,
                start,
                end);
        
        return filledForm;        
    }
    
//------------------------------------------------------------------------------

    /**
     * Copy only a portion of a text file into another text file.
     * @param inFile the file to be read
     * @param outFile the file to be written
     * @param start line number from which to extract lines. Note this
     * is zero-based indexing. 
     * @param stop line number at which to stop the extraction. Note this
     * is zero-based indexing. 
     */

    public static void copyPortionOfTxtFile(File inFile, 
    		File outFile, int start, int stop) throws Exception
    {
    	int bufferSize = 0;
    	int bufferMaxSize = 1000; //arbitrary :|
        int lineNum = -1;
        BufferedReader buffRead = new BufferedReader(
        		new FileReader(inFile));
        //NB here we do not append, but later we do
        BufferedWriter buffWriter = new BufferedWriter(
        		new FileWriter(outFile));
        String line = null;
        StringBuilder sb = new StringBuilder();
        while ((line = buffRead.readLine()) != null)
        {
            lineNum++;
            if (lineNum >= start)
            {
                sb.append(line)
                   .append(System.getProperty("line.separator"));
                bufferSize++;
                if (bufferSize > bufferMaxSize) 
                {
                    buffWriter.write(sb.toString());
                    buffWriter.flush();
                    buffWriter.close();
                    buffWriter = new BufferedWriter(
                    		new FileWriter(outFile,true));
                    bufferSize = 0;
                    sb = new StringBuilder();
                }
            }
            if (lineNum >= stop)
            {
            	if (bufferSize>0)
            	{
	            	buffWriter.write(sb.toString());
	                buffWriter.flush();
	                buffWriter.close();
            	}
                break;
            }
        }
        buffRead.close();
    }

//------------------------------------------------------------------------------

    /**
     * Extract text from a file starting from line <code>start</code> and till
     * line <code>stop</code>
     * @param vmFile name of the file to be read
     * @param start line number from which to extract lines
     * @param stop line number at which to stop the extraction
     * @return the array with the tail of the text file.
     */

    public static List<String> extractFromTo(File vmFile, int start, 
    		int stop)
    {
        List<String> lines = new ArrayList<String>();

        int num = 0;
        BufferedReader buffRead = null;
        boolean badTermination = false;
        String msg = "";
        try {
            buffRead = new BufferedReader(new FileReader(vmFile));
            String line = null;
            while ((line = buffRead.readLine()) != null)
            {
                num++;
                if (num >= start)
                {
                    lines.add(line);
                }
                if (num >= stop)
                {
                    lines.add(line);
                    break;
                }
            }
        } catch (Throwable t) {
            badTermination = true;
            msg = t.getMessage();
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (Throwable t2) {
                badTermination = true;
                msg = t2.getMessage();
            }
        }

        if (badTermination)
            Terminator.withMsgAndStatus("ERROR! " + msg,-1);

        return lines;
        }

//------------------------------------------------------------------------------

    /**
     * Extract the tail of a file staring from line <code>start</code> to the 
     * end of the file
     * @param file the file to be read
     * @param start line number from which to extract lines
     * @return the array with the tail of the text file.
     */

    public static List<String> tailFrom(File file, int start)
    {
        List<String> lines = new ArrayList<String>();

        int num = 0;
        BufferedReader buffRead = null;
        boolean badTermination = false;
        String msg = "";
        try {
            buffRead = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = buffRead.readLine()) != null)
            {
                num++;
                if (num >= start)
                    lines.add(line);
            }
        } catch (Throwable t) {
            badTermination = true;
            msg = t.getMessage();
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (Throwable t2) {
                badTermination = true;
                msg = t2.getMessage();
            }
        }

        if (badTermination)
            Terminator.withMsgAndStatus("ERROR! " + msg,-1);

        return lines;
    }
    
//------------------------------------------------------------------------------

    /**
     * Reads SDF files (chemical format - see 
     * <a href="http://en.wikipedia.org/wiki/Chemical_table_file#SDF">MDL format</a>
     * ) and returns all the molecules as an array.
     * @param file the SDF file to be read
     * @return all the chemical objects into an list.
     */

    public static List<IAtomContainer> readSDF(File file)
    {
        MDLV2000Reader mdlreader = null;
        List<IAtomContainer> lstContainers = new ArrayList<IAtomContainer>();
        try {
            mdlreader = new MDLV2000Reader(new FileReader(file));
            ChemFile chemFile = (ChemFile) mdlreader.read((ChemObject) 
                                                                new ChemFile());
            lstContainers.addAll(ChemFileManipulator.getAllAtomContainers(
                                                                     chemFile));
        } catch (Throwable t2) {
            Terminator.withMsgAndStatus("ERROR! Failure in reading SDF: " + t2,
                                                                            -1);
        }

        return lstContainers;
    }

//------------------------------------------------------------------------------

    /**
     * Write a ZMatrix to file with ACC's format
     * @param zmat the ZMatrix to write
     * @param file the destination file
     * @param append set to <code>true</code> to append rather than overwrite
     */

    public static void writeZMatAppend(File file, ZMatrix zmat, boolean append)
    {
        List<String> txt = zmat.toLinesOfText(false,false);
        txt.add(0,"Molecule: " + zmat.getTitle());
        txt.add(ZMatrixConstants.ZMATMOLSEP); //separator
        IOtools.writeTXTAppend(file,txt,append);
    }

//------------------------------------------------------------------------------

    /**
     * Reads ZMatrix file (ACC format). 
     * Suitable for small files - do NOT use this for huge files.
     * @param file file to be read
     * @return all the ZMatrixes
     */

    public static List<ZMatrix> readZMatrixFile(File file)
    {
        List<ZMatrix> allZMats = new ArrayList<ZMatrix>();
        BufferedReader buffRead = null;
        try {
            buffRead = new BufferedReader(new FileReader(file));
            String line = null;
            String title = "";
            List<String> oneBlock = new ArrayList<String>();
            while ((line = buffRead.readLine()) != null)
            {
                line = line.trim();
                if (title.equals(""))
                {
                    title = line;
                    continue;
                }
                if (line.startsWith("$$$$"))
                {
                    ZMatrix zmat = new ZMatrix(oneBlock);
                    zmat.setTitle(title);
                    allZMats.add(zmat);
                    oneBlock.clear();
                    title = "";
                }
                else
                {
                    oneBlock.add(line);
                }
            }
            if (oneBlock.size() != 0)
            {
                Terminator.withMsgAndStatus("ERROR! Unterminated ZMatrix block "
                                            + "in file '" + file + "'.",-1);
            }
        } catch (FileNotFoundException fnf) {
            Terminator.withMsgAndStatus("ERROR! File '" + file + "' not found.",
            		-1);
        } catch (IOException ioex) {
        	Terminator.withMsgAndStatus("Error in reading file '" + file, 
        			-1 , ioex);
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (IOException ioex2) {
            	Terminator.withMsgAndStatus("Error in reading file '" + file, 
            			-1 , ioex2);
            }
        }
        return allZMats;
    }
    
//------------------------------------------------------------------------------

    /**
     * Reads XYZ files (chemical format - see  
     * <a href="http://en.wikipedia.org/wiki/XYZ_file_format">XYZ format</a>
     * ) and returns all the molecules as an array.
     * @param filename XYZ file to be read
     * @return all the chemical objects into an <code>List</code>
     */

    public static List<IAtomContainer> readXYZ(File file)
    {
        XYZReader reader = null;
        List<IAtomContainer> lstContainers = new ArrayList<IAtomContainer>();
        try {
            reader = new XYZReader(new FileReader(file));
            ChemFile chemFile = (ChemFile) reader.read((ChemObject) 
                                                                new ChemFile());
            lstContainers.addAll(ChemFileManipulator.getAllAtomContainers(
                                                                     chemFile));
        } catch (Throwable t2) {
            Terminator.withMsgAndStatus("ERROR! Failure in reading XYZ: " + t2,
                                                                            -1);
        }

        return lstContainers;
    }
    
//------------------------------------------------------------------------------

    /**
     * Read a molecular structure file that might contain multiple structures.
     * Accepts SDF, XYZ files, or any output file that can be analyzed by 
     * any registered implementation of {@link ChemSoftOutputReader}.
     * @param file file to be read
     * @return all the chemical objects into an <code>ArrayList</code>
     */
    public static List<IAtomContainer> readMultiMolFiles(File file)
    {
		ChemSoftReaderWriterFactory builder = 
				ChemSoftReaderWriterFactory.getInstance();
		
        List<IAtomContainer> mols = new ArrayList<IAtomContainer>();
        if (file.getName().endsWith(".sdf"))
        {
            mols = IOtools.readSDF(file);
        } else if (file.getName().endsWith(".xyz"))
        {
            mols = IOtools.readXYZ(file);
        } else {
        	try {
				ChemSoftOutputReader analyzer = builder.makeOutputReaderInstance(file);
				if (analyzer!=null)
				{
					ParameterStorage params = new ParameterStorage();
					params.setParameter(ChemSoftConstants.PARJOBOUTPUTFILE,
							file.getAbsolutePath());
					params.setParameter(WorkerConstants.PARTASK,
							Task.make("analyseOutput").casedID);
					analyzer.setParameters(params);
					analyzer.initialize();
					NamedDataCollector allData = new NamedDataCollector();
					analyzer.setDataCollector(allData);
					analyzer.performTask();
					
					@SuppressWarnings("unchecked")
					Map<Integer, NamedDataCollector> dataByStep =
							(Map<Integer, NamedDataCollector>) 
								allData.getNamedData(ChemSoftConstants
										.JOBOUTPUTDATA).getValue();
					
					for (Integer stepId : dataByStep.keySet())
					{
						NamedDataCollector stepData = dataByStep.get(stepId);
						if (stepData.contains(
								ChemSoftConstants.JOBDATAGEOMETRIES))
						{
							AtomContainerSet acs = (AtomContainerSet) 
									stepData.getNamedData(ChemSoftConstants
											.JOBDATAGEOMETRIES).getValue();
							for (IAtomContainer iac : acs.atomContainers())
								mols.add(iac);
						}
					}
					
					if (mols.size()==0)
					{
						Terminator.withMsgAndStatus("ERROR! No geometry found "
								+ "in '" + file.getName() + "' (analyzed by "
								+ analyzer.getClass().getSimpleName()
								+ ").", -1);
					}
				}
			} catch (FileNotFoundException e) {
				Terminator.withMsgAndStatus("ERROR! File '"
                        + file.getName() + "' not found.", -1);
			}
        }
        
        if (mols.size()==0) 
        {
            Terminator.withMsgAndStatus("ERROR! Cannot understand format of '"
                        + file.getName() + "'."
                        + " In this version, you can read multiple "
                        + "chemical entities only from SDF, XYZ files, or "
                        + "any output from any of these: " 
                        + builder.getRegisteredSoftwareIDs(), -1);
        }
        return mols;
    }
  
//------------------------------------------------------------------------------

    /**
     * Writes on a new  
     * <a href="http://en.wikipedia.org/wiki/Chemical_table_file#SDF">SDF</a>
     * file or appends to an existing one.
     * @param file target SDF file (new or existing)
     * @param mol atom container to be written on the SDF file
     * @param append <code>true</code> to append to existing file
     */

    public static void writeSDFAppend(File file, List<IAtomContainer> mols,boolean append)
    {
        SDFWriter sdfWriter = null;
        try {
            sdfWriter = new SDFWriter(new FileWriter(file, append));
            for (IAtomContainer mol : mols)
            {
                sdfWriter.write(mol);
            }
        }
        catch (Throwable t2) 
        {
        	Terminator.withMsgAndStatus("Error in writing file '" + file, 
        			-1 , t2);
        } 
        finally 
        {
            try 
            {
                if(sdfWriter != null)
                     sdfWriter.close();
            } 
            catch (IOException ioe) 
            {
            	Terminator.withMsgAndStatus("Error in writing file '" + file, 
            			-1 , ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes on a new  
     * <a href="http://en.wikipedia.org/wiki/Chemical_table_file#SDF">SDF</a>
     * file or appends to an existing one.
     * @param file target SDF file (new or existing)
     * @param mol atom container to be written on the SDF file
     * @param append <code>true</code> to append to existing file
     */

    public static void writeSDFAppend(File file, IAtomContainer mol,
    		boolean append)
    {
        SDFWriter sdfWriter = null;
        try {
            sdfWriter = new SDFWriter(new FileWriter(file, append));
            sdfWriter.write(mol);
        } 
        catch (Throwable t2) 
        {
        	Terminator.withMsgAndStatus("Error in writing file '" + file 
        			+ "': " + t2, -1);
        } 
        finally 
        {
            try 
            {
                if(sdfWriter != null)
                     sdfWriter.close();
            } 
            catch (IOException ioe) 
            {
            	Terminator.withMsgAndStatus("Error in writing file '" + file, 
            			-1 , ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes on a new XYZ 
     * file or appends to an existing one.
     * @param file target XYZ file (new or existing)
     * @param iac atom container to be written on the XYZ file
     * @param append <code>true</code> to append to existing file
     */

    public static void writeXYZAppend(File file, IAtomContainer iac,
                                                                 boolean append)
    {
        XYZWriter xyzWriter = null;
        try {
            xyzWriter = new XYZWriter(new FileWriter(file, append));
            xyzWriter.write(iac);
        }
        catch (CDKException e)
        {
        	Terminator.withMsgAndStatus("Error in writing file '" + file, 
        			-1 ,e);
        }
        catch (Throwable t2)
        {
        	Terminator.withMsgAndStatus("Error in writing file '" + file, 
        			-1 ,t2);
        }
        finally
        {
            try
            {
                if(xyzWriter != null)
                     xyzWriter.close();
            }
            catch (IOException ioe)
            {
            	Terminator.withMsgAndStatus("Error in writing file '" + file, 
            			-1 ,ioe);
            }
        }
    }
    
//------------------------------------------------------------------------------
	
	/**
	 * Writes atom containers to file or appends to an existing one.
	 * @param file file where we want to write (new or existing).
	 * @param acs set of atom containers to be written on the file.
	 * @param format the format for writing the atom containers. Acceptable
	 * values are 'SDF' and 'XYZ' (case insensitive).
	 * @param append <code>true</code> to append to existing file. Otherwise,
	 * we overwrite any existing content.
	 */
	
	public static void writeAtomContainerToFile(File file, 
	  		IAtomContainer ac, String format, boolean append)
	{
	  	switch(format.toUpperCase())
	  	{
	  		case "XYZ":
				writeXYZAppend(file, ac, append);
				break;
			
			case "SDF":
				writeSDFAppend(file, ac, append);
				break;
				
			default:
				Terminator.withMsgAndStatus("ERROR! Format '" + format + "' is "
						+ "not a known format for writing atom containers", -1);
		  			break;
	  	}
	}
    
//------------------------------------------------------------------------------
    
    /**
     * Writes atom containers to file
     * file or appends to an existing one.
     * @param filename target file (new or existing).
     * @param acs set of atom containers to be written on the output file.
     * @param format the format to use. Available formats are 'SDF', 'XYZ, and 
     * 'ORCATRAJECTORY'.
     * @param append <code>true</code> to append to existing file
     */

    public static void writeAtomContainerSetToFile(File file, 
    		IAtomContainerSet acs, String format, boolean append)
    {
    	switch(format.toUpperCase())
    	{
    		case "ORCATRAJECTORY":
    			writeOrcaTrj(file, acs, append);
    			break;
    			
    		case "XYZ":
    			writeXYZAppendSet(file, acs, append);
    			break;
    			
    		case "SDF":
    			writeSDFAppendSet(file, acs, append);
    			break;
    		
    		default:
    			Terminator.withMsgAndStatus("ERROR! Format '" + format + "' is "
    					+ "not a known format for writing atom containers", -1);
    			break;
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Writes a list of molecules on a new 
     * <a href="http://en.wikipedia.org/wiki/Chemical_table_file#SDF">SDF</a>
     * file or appends to an existing file.
     * @param file target SDF file (new or existing)
     * @param mols set of atom containers to be written on the SDF file
     * @param append <code>true</code> to append to existing file
     */

    public static void writeSDFAppendSet(File file, IAtomContainerSet mols,
                                                                 boolean append)
    {
        SDFWriter sdfWriter = null;
        try {
            sdfWriter = new SDFWriter(new FileWriter(file, append));
            sdfWriter.write(mols);
        } catch (Throwable t2) {
        	Terminator.withMsgAndStatus("Error in writing file '" + file, 
        			-1 , t2);
        } finally {
             try {
                 if(sdfWriter != null)
                     sdfWriter.close();
             } catch (IOException ioe) {
            	Terminator.withMsgAndStatus("Error in writing file '" + file, 
            			-1 , ioe);
             }
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Writes on a new file with Orca's trajectory format (i.e., multi-molecule
     * XYZ file where each molecule is separated by the next one by a &gt; 
     * symbol.
     * @param file target XYZ file (new or existing)
     * @param acs set of atom containers to be written on the XYZ file
     * @param append <code>true</code> to append to existing file,
     * otherwise we overwrite.
     */
    public static void writeOrcaTrj(File file, IAtomContainerSet acs,
            boolean append)
    {
    	for (int i=0; i<acs.getAtomContainerCount(); i++)
        {
    		IAtomContainer mol = acs.getAtomContainer(i);
    		TextBlock tb = new TextBlock();
    		tb.add(mol.getAtomCount()+"");
    		tb.add(MolecularUtils.getNameOrID(mol));
            for (IAtom a : mol.atoms())
            {
            	tb.add(AtomUtils.getSymbolOrLabel(a)+"  "
            			+a.getPoint3d().x+"  "+a.getPoint3d().y+"  "
            			+a.getPoint3d().z);
            }
            tb.add(">");
            writeTXTAppend(file, tb, true);
        }
    }
    
    
//------------------------------------------------------------------------------
    
    /**
     * Writes the definition of a job in JSON format to a file.
     * @param job the job to serialize into JSON format.
     * @param file the file in which to write. Must not exist.
     */
    public static void writeJobToJSON(Job job, File file)
    {
		FileUtils.mustNotExist(file);
		Gson writer = ACCJson.getWriter();
		IOtools.writeTXTAppend(file, writer.toJson(job), true);
    }

//------------------------------------------------------------------------------

    /**
     * Writes on a new XYZ
     * file or appends to an existing one.
     * @param file target XYZ file (new or existing)
     * @param acs set of atom containers to be written on the XYZ file
     * @param append <code>true</code> to append to existing file,
     * otherwise we overwrite.
     */

    public static void writeXYZAppendSet(File file, IAtomContainerSet acs,
                                                                 boolean append)
    {
        //There is currently no way to write a set to XYZ files so we force it
    	//First, we write one molecule. Then we append the rest.
        XYZWriter xyzWriter = null;
        try {
            xyzWriter = new XYZWriter(new FileWriter(file, append));
            xyzWriter.write(acs.getAtomContainer(0));
        }
        catch (Throwable t2)
        {
        	Terminator.withMsgAndStatus("Error in writing file '" + file, 
        			-1 , t2);
        }
        finally
        {
            try
            {
                if(xyzWriter != null)
                     xyzWriter.close();
            }
            catch (IOException ioe)
            {
            	Terminator.withMsgAndStatus("Error in writing file '" + file, 
            			-1, ioe);
            }
        }
        XYZWriter xyzWriterAppend = null;
        try {
            xyzWriterAppend = new XYZWriter(new FileWriter(file, true));
            for (int i=1; i<acs.getAtomContainerCount(); i++)
            {
                xyzWriterAppend.write(acs.getAtomContainer(i));
            }
        }
        catch (Throwable t2)
        {
        	Terminator.withMsgAndStatus("Error in writing file '" + file, 
        			-1 , t2);
        }
        finally
        {
            try
            {
                if(xyzWriterAppend != null)
                     xyzWriterAppend.close();
            }
            catch (IOException ioe)
            {
            	Terminator.withMsgAndStatus("Error in writing file '" + file, 
            			-1 , ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes lines of text.
     * @param file the file where we write.
     * @param line the line to be written.
     * @param append set to <code>true</code> to append instead of overwrite the
     * target file
     */

    public static void writeTXTAppend(File file, String line,
    		boolean append)
    {
    	writeTXTAppend(file, Arrays.asList(line), append);
    }
    
//------------------------------------------------------------------------------

    /**
     * Writes lines of text.
     * @param outFile the file where we write.
     * @param arrayList the line to be written.
     * @param append set to <code>true</code> to append instead of overwrite the
     * target file
     */

    public static void writeTXTAppend(File file, List<String> lines,
    		boolean append)
    {
        FileWriter writer = null;
        try
        {
            writer = new FileWriter(file, append);
            writer.write(StringUtils.mergeListToString(lines, newline));
        } catch (Throwable t) {
        	Terminator.withMsgAndStatus("Error in writing file '" + file, 
        			-1 , t);
        } finally {
             try {
                 if(writer != null)
                     writer.close();
             } catch (IOException ioe) {
            	Terminator.withMsgAndStatus("Error in writing file '" + file, 
            			-1 , ioe);
             }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Copy the content of text files into other files
     * @param inFile the source file, i.e., where to copy from.
     * @param outFile the target file, i.e., where to copy into.
     * @param append if <code>true</code> required to append the content of the
     * input file at the end of the output file
     */

    public static void copyFile(File inFile, File outFile, boolean append)
    {
        try {
	        InputStream inStr = new FileInputStream(inFile);
	        OutputStream outStr = new FileOutputStream(outFile,append);
	        byte[] buf = new byte[1024];
	        int len;
	        while ((len = inStr.read(buf)) > 0)
	        {
	            outStr.write(buf, 0, len);
	        }
	        inStr.close();
	        outStr.close();
        } catch (Throwable t) {
        	Terminator.withMsgAndStatus("Error in copying file '" + inFile, 
        			-1 , t);
        } 
    }

//------------------------------------------------------------------------------

    /**
     * Delete a file 
     * @param fileName path and name of the file to remove
     */

    public static void deleteFile(File file)
    {
        if (file.exists())
            if (file.canWrite())
            	file.delete();
    }

//------------------------------------------------------------------------------

    /**
     * Read an SD/
     * <a href="http://en.wikipedia.org/wiki/Chemical_table_file#SDF">SDF</a>
     * file as text and make a copy including only selected 
     * molecules
     * @param inFile SD/SDF file containing the initial set of molecules
     * @param outFile SD/SDF file containing only the selected molecules
     * @param filter vector of Booleans defining which molecule has to be kept
     * (<code>true</code> entry)
     */

    public static void filterSDasTXT(File inFile, File outFile,
    		List<Boolean> filter)
    {
        BufferedReader buffRead = null;
        try {
            buffRead = new BufferedReader(new FileReader(inFile));
            String line = null;
            int i = 0;
            while ((line = buffRead.readLine()) != null)
            {
                if (line.startsWith("$$$$"))
                {
                    if (filter.get(i))
                        writeTXTAppend(outFile,line+newline,true);
                    i++;
                    
                } else if (filter.get(i)) {
                    writeTXTAppend(outFile,line+newline,true);
                }
            }
        } catch (FileNotFoundException fnf) {
        	Terminator.withMsgAndStatus("File Not Found: " + inFile, 1);
        } catch (IOException ioe) {
        	Terminator.withMsgAndStatus("Error in reading file '" + inFile, 
        			-1 , ioe);
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (IOException ioex2) {
            	Terminator.withMsgAndStatus("Error in reading file '" + inFile, 
            			-1 , ioex2);
            }
        }

    }

//------------------------------------------------------------------------------

}
