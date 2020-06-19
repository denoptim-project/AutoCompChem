package autocompchem.io;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.XYZReader;
import org.openscience.cdk.io.XYZWriter;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.intcoords.zmatrix.ZMatrixConstants;
import autocompchem.run.Terminator;
import autocompchem.text.TextAnalyzer;
import autocompchem.utils.StringUtils;


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
     * @param filename file to be read
     * @return all the lines into an <code>ArrayList</code>
     */

    public static ArrayList<String> readTXT(String filename)
    {
	return readTXT(filename, false);
    }

//------------------------------------------------------------------------------

    /**
     * Reads TXT files (Suitable for small files - do NOT use this for huge 
     * files!)
     * @param filename file to be read
     * @param escape set tu <code>true</code> to escape special characters
     * @return all the lines into an <code>ArrayList</code>
     */

    public static ArrayList<String> readTXT(String filename, boolean escape)
    {
        ArrayList<String> allLines = new ArrayList<String>();
        BufferedReader buffRead = null;
        try {
            buffRead = new BufferedReader(new FileReader(filename));
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
            System.err.println("File Not Found: " + filename);
            System.err.println(fnf.getMessage());
            System.exit(-1);
        } catch (IOException ioex) {
            System.err.println(ioex.getMessage());
            System.exit(-1);
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (IOException ioex2) {
                System.err.println(ioex2.getMessage());
                System.exit(-1);
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
     * @return all the lines beginning with the <code>keyword</code> into an 
     * <code>ArrayList</code>
     */

    public static ArrayList<String> readTXTKeyword(String filename,
                                                                String keyword)
    {
        ArrayList<String> allLines = new ArrayList<String>();
        BufferedReader buffRead = null;
        try {
            buffRead = new BufferedReader(new FileReader(filename));
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
            Terminator.withMsgAndStatus("ERROR! File " + filename 
                                + " not found!", -1);
        } catch (IOException ioex) {
            Terminator.withMsgAndStatus("ERROR! IOException while reading "
                                + filename, -1);
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (IOException ioex2) {
                Terminator.withMsgAndStatus("ERROR! IOException while "
                                + "reading " + filename, -1);
            }
        }
        return allLines;
    }

//------------------------------------------------------------------------------

    /**
     * Extract '<code>key|separator|value</code>' field in a properly formatted
     * text file.
     *
     * @param filename file to be read
     * @param separator string splitting the <code>key</code> from the 
     * <code>value</code>
     * @param commentLab string excluding test from being read (commented out)
     * @param start string identifying the beginning of a block of lines to be
     * read
     * @param end string identifying the end of a block of lines to be read
     * @return a table with all strings extracted according to the formatting
     * labels.
     */

    public static ArrayList<ArrayList<String>> readFormattedText(
                        String filename,
                        String separator, String commentLab,
                        String start, String end)
    {
        //Read file line by line
        ArrayList<String> lines = IOtools.readTXT(filename);

        //Start interpretation of the formatted text
        ArrayList<ArrayList<String>> filledForm = readFormattedText(
                                                        filename,
                                                        lines, 
                                                        separator, 
                                                        commentLab, 
                                                        start, 
                                                        end);
        return filledForm;        
    }

//------------------------------------------------------------------------------

    /**
     * Extract '<code>key|separator|value</code>' field in a properly formatted
     * text. This method is capable of handling single- and multi-line 
     * records. For multiline records, two special labels are use: 
     * <code>start</code> to identify the beginning of a multiline record
     * '<code>key|separator|value</code>', and <code>end</code> for the end.
     * Text in between these two labels will be d as belonging to a 
     * single '<code>key|separator|value</code>' where the 'key' must be 
     * following <code>start</code> label in the same line.
     * <br>
     * For example, A single line '<code>key|separator|value</code>' is: 
     * <\br><\br>
     * <code>thisIsAKey: this is the value</code>
     * <\br><\br>
     * For example, a multiline '<code>key|separator|value</code>' is:
     * (Using $START and $END as labels)
     * <br><br>
     * <code>$STARTthisIsAKey: this is part of the value<br>
     * this is still part of the value<br>
     * and also this<br>
     * $END</code>
     * <br><br>
     * In addition, a label is defined for the commented out lines.
     * All the labels must be in the very beginning of the line! 
     * This method cannot handle nested blocks.
     *
     * @param filename name of the file to read (used only to report errors)
     * @param lines content of the file
     * @param separator string defining the separator in 
     * '<code>key|separator|value</code>'
     * @param commentLab string identifying the comments
     * @param start string identifying the beginning of a multiline block
     * @param end string identifying the end of a multiline block
     * @return a table with all strings extracted according to the formatting
     * labels.
     */

    public static ArrayList<ArrayList<String>> readFormattedText(
                        String filename,
                        ArrayList<String> lines, 
                        String separator, String commentLab, 
                        String start, String end)
    {
	ArrayList<ArrayList<String>> filledForm = TextAnalyzer.readKeyValue(
                                                        lines,
                                                        separator,
                                                        commentLab,
                                                        start,
                                                        end);
        return filledForm;

/*
//TODO: del moved to TextAnalyzer
        //Start interpretation of the formatted text
        ArrayList<ArrayList<String>> filledForm = 
                                new ArrayList<ArrayList<String>>();
        for (int i=0; i<lines.size(); i++)
        {
            String line = lines.get(i);
	    line = StringUtils.escapeSpecialChars(line);

            //Check if its a multiple line block
            if (line.startsWith(start))
            {
                //Read multiline block
                int indexKVSep = line.indexOf(separator);
                //Check the value of the index
                if (indexKVSep <= 0)
                {
                    Terminator.withMsgAndStatus("ERROR ReadFormattedText-1! "
                        + "Check file " + filename
                        + " (line " + (i+1) +")",-1);
                }

                //define parameter's key and possibly value
                String key = line.substring(start.length(),indexKVSep);
                String value = "";
                if (indexKVSep < line.length())
                    value = line.substring(indexKVSep + 1);

                //Read other lines
                boolean goon = true;
                while (goon)
                {
                    if ((i+1) >= lines.size())
                    {
                        Terminator.withMsgAndStatus("ERROR"
                            + " ReadFormattedText-2a! Check file " + filename
                            + ". Unclosed multiline block. ",-1);
                    }
                    i++;
                    String otherLine = lines.get(i);
                    if (otherLine.contains(end))
                    {
                        int indexOfEnd = otherLine.indexOf(end);
                        //Check the value of the index
                        if (indexOfEnd < 0) 
                        {
                            Terminator.withMsgAndStatus("ERROR"
                            + " ReadFormattedText-2! Check file " + filename
                            + " (line " + (i+1) +")",-1);
                        }

                        //append the initial portion of the line
                        String partialLine = 
                                        otherLine.substring(0,indexOfEnd);
                        value = value + newline + partialLine;
                        goon = false;
                    } else {
                        value = value + newline + otherLine;
                    }
                }

                //Prepare key and value
                key = key.toUpperCase();
                value = value.trim();
		value = StringUtils.deescapeSpecialChars(value);

                //Check parameter
                if ((key.equals("")) || (value.equals("")))
                {
                    Terminator.withMsgAndStatus("ERROR ReadFormattedText-3! "
                        + "Check file " + filename
                        + " (line " + (i+1) +": key'" + key + "', value '"
                        + value + "')",-1);
                }

                //Store
                ArrayList<String> singleBlock = new ArrayList<String>();
                singleBlock.add(key);
                singleBlock.add(value);
                filledForm.add(singleBlock);

            } else {
                //Skip commented out
                if (line.startsWith(commentLab))
                    continue;

                //Read Single line parameter
                int indexKVSep = line.indexOf(separator);
                //Check the value of the index
                if (indexKVSep <= 0)
                {
                    Terminator.withMsgAndStatus("ERROR ReadFormattedText-4! "
                        + "Check file " + filename
                        + " (line " + (i+1) +": '" + line + "'). "
                        + "There seem to be no key.",-1);
                }
                if (indexKVSep == line.length())
                {
                    Terminator.withMsgAndStatus("ERROR ReadFormattedText-5! "
                        + "Check file " + filename
                        + " (line " + (i+1) +"). "
                        + "There seem to be no value.",-1);
                }

                String key = line.substring(0,indexKVSep);
                String value = line.substring(indexKVSep + 1);

                key = key.toUpperCase();
                value = value.trim();

                //Check
                if ((key.equals("")) || (value.equals("")))
                {
                    Terminator.withMsgAndStatus("ERROR ReadFormattedText-6! "
                        + "Check file " + filename
                        + " (line " + (i+1) +": key'" + key + "', value '" 
                        + value + "')",-1);
                }

                //Store
                ArrayList<String> singleBlock = new ArrayList<String>();
                singleBlock.add(key);
                singleBlock.add(value);
                filledForm.add(singleBlock);
           }
        } //End of loop over lines

        return filledForm;
*/
    }

//------------------------------------------------------------------------------

    /**
     * Extract text from a file starting from line <code>start</code> and till
     * line <code>stop</code>
     * @param filename name of the file to be read
     * @param start line number from which to extract lines
     * @param stop line number at which to stop the extraction
     * @return the array with the tail of the text file.
     */

    public static ArrayList<String> extractFromTo(String filename, int start, 
                                                                       int stop)
    {
        ArrayList<String> lines = new ArrayList<String>();

        int num = 0;
        BufferedReader buffRead = null;
        boolean badTermination = false;
        String msg = "";
        try {
            buffRead = new BufferedReader(new FileReader(filename));
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
     * @param filename name of the file to be read
     * @param start line number from which to extract lines
     * @return the array with the tail of the text file.
     */

    public static ArrayList<String> tailFrom(String filename, int start)
    {
        ArrayList<String> lines = new ArrayList<String>();

        int num = 0;
        BufferedReader buffRead = null;
        boolean badTermination = false;
        String msg = "";
        try {
            buffRead = new BufferedReader(new FileReader(filename));
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
     * @param filename SDF file to be read
     * @return all the chemical objects into an <code>ArrayList</code>
     */

    public static ArrayList<IAtomContainer> readSDF(String filename)
    {
        MDLV2000Reader mdlreader = null;
        ArrayList<IAtomContainer> lstContainers = 
                                                new ArrayList<IAtomContainer>();
        try {
            mdlreader = new MDLV2000Reader(new FileReader(new File(filename)));
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
     * @param filename the pathname of the destination file
     * @param append set to <code>true</code> to append rather than ovewrite
     */

    public static void writeZMatAppend(String filename, ZMatrix zmat,
								 boolean append)
    {
        ArrayList<String> txt = zmat.toLinesOfText(false,false);
        txt.add(0,"Molecule: " + zmat.getTitle());
        txt.add(ZMatrixConstants.ZMATMOLSEP); //separator
        IOtools.writeTXTAppend(filename,txt,append);
    }

//------------------------------------------------------------------------------

    /**
     * Reads ZMatrix file (ACC format). 
     * Suitable for small files - do NOT use this for huge files.
     * @param filename file to be read
     * @return all the ZMatrixes
     */

    public static ArrayList<ZMatrix> readZMatrixFile(String filename)
    {
        ArrayList<ZMatrix> allZMats = new ArrayList<ZMatrix>();
        BufferedReader buffRead = null;
        try {
            buffRead = new BufferedReader(new FileReader(filename));
            String line = null;
	    String title = "";
	    ArrayList<String> oneBlock = new ArrayList<String>();
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
                                            + "in file '" + filename + "'.",-1);
	    }
        } catch (FileNotFoundException fnf) {
            Terminator.withMsgAndStatus("ERROR! File '" + filename 
                                                           + "' not found.",-1);
        } catch (IOException ioex) {
            System.err.println(ioex.getMessage());
            Terminator.withMsgAndStatus("ERROR! While reading '" + filename 
                                      + "'. Message: " + ioex.getMessage(), -1);
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (IOException ioex2) {
                System.err.println(ioex2.getMessage());
                Terminator.withMsgAndStatus("ERROR! While reading '" + filename 
                                      + "'. Message: " + ioex2.getMessage(),-1);
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
     * @return all the chemical objects into an <code>ArrayList</code>
     */

    public static ArrayList<IAtomContainer> readXYZ(String filename)
    {
        XYZReader reader = null;
        ArrayList<IAtomContainer> lstContainers = 
                                                new ArrayList<IAtomContainer>();
        try {
            reader = new XYZReader(new FileReader(new File(filename)));
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
     * Accepta SDF or XYZ files
     * @param filename file to be read
     * @return all the chemical objects into an <code>ArrayList</code>
     */
    public static ArrayList<IAtomContainer> readMultiMolFiles(String filename)
    {
        ArrayList<IAtomContainer> mols = new ArrayList<IAtomContainer>();
        if (filename.endsWith(".sdf"))
        {
            mols = IOtools.readSDF(filename);
        }
        else if (filename.endsWith(".xyz"))
        {
            mols = IOtools.readXYZ(filename);
        }
        else
        {
            Terminator.withMsgAndStatus("ERROR! Cannot understand format of '"
                        + filename + "'. In this version, you can read multiple "
                        + "chemical entities only from SDF or XYZ files.",-1);
        }
        return mols;
    }

//------------------------------------------------------------------------------

    /**
     * Writes on a new  
     * <a href="http://en.wikipedia.org/wiki/Chemical_table_file#SDF">SDF</a>
     * file or appends to an existing one.
     * @param filename target SDF file (new or existing)
     * @param mol atom container to be written on the SDF file
     * @param append <code>true</code> to append to existing file
     */

    public static void writeSDFAppend(String filename, IAtomContainer mol,
                                                                 boolean append)
    {
        SDFWriter sdfWriter = null;
        try {
            sdfWriter = new SDFWriter(new FileWriter(new File(filename), 
                                                                       append));
            sdfWriter.write(mol);
        } 
        catch (CDKException e) 
        {
            if (e.getMessage().contains("For input string: \"#\""))
            {
                System.err.println("CDK unable to write MDL file " + filename);
            }
            
        } 
        catch (Throwable t2) 
        {
            System.err.println("Failure in writing SDF: " + t2);
            System.exit(-1);
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
                System.err.println("Error in writing: " + ioe);
                System.exit(-1);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes on a new XYZ 
     * file or appends to an existing one.
     * @param filename target XYZ file (new or existing)
     * @param iac atom container to be written on the XYZ file
     * @param append <code>true</code> to append to existing file
     */

    public static void writeXYZAppend(String filename, IAtomContainer iac,
                                                                 boolean append)
    {
        //Need to get Molecule for backwards compatibility with CDK classes
        IMolecule mol = new Molecule(iac);

        XYZWriter xyzWriter = null;
        try {
            xyzWriter = new XYZWriter(new FileWriter(new File(filename),
                                                                       append));
            xyzWriter.write(mol);
        }
        catch (CDKException e)
        {
            e.printStackTrace();
            System.err.println("CDK unable to write XYZ file " + filename);
        }
        catch (Throwable t2)
        {
            t2.printStackTrace();
            System.err.println("Failure in writing XYZ: " + t2);
            System.exit(-1);
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
                System.err.println("Error in writing: " + ioe);
                System.exit(-1);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes a list of molecules on a new 
     * <a href="http://en.wikipedia.org/wiki/Chemical_table_file#SDF">SDF</a>
     * file or appends to an existing file.
     * @param filename target SDF file (new or existing)
     * @param mols set of atom containers to be written on the SDF file
     * @param append <code>true</code> to append to existing file
     */

    public static void writeSDFAppendSet(String filename, 
                                                         IAtomContainerSet mols,
                                                                 boolean append)
    {
        SDFWriter sdfWriter = null;
        try {
            sdfWriter = new SDFWriter(new FileWriter(new File(filename), 
                                                                       append));
            sdfWriter.write(mols);
        } catch (Throwable t2) {
            System.err.println("Failure in writing SDF: " + t2);
            System.exit(-1);
        } finally {
             try {
                 if(sdfWriter != null)
                     sdfWriter.close();
             } catch (IOException ioe) {
                 System.err.println("Error in writing: " + ioe);
                 System.exit(-1);
             }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes on a new XYZ
     * file or appends to an existing one.
     * @param filename target XYZ file (new or existing)
     * @param acs set of atom containers to be written on the XYZ file
     * @param append <code>true</code> to append to existing file
     */

    public static void writeXYZAppendSet(String filename, IAtomContainerSet acs,
                                                                 boolean append)
    {
        //There is currently no way to write a set to XYZ files so we force it
        XYZWriter xyzWriter = null;
        try {
            xyzWriter = new XYZWriter(new FileWriter(new File(filename),
                                                                       append));
            IMolecule mol = new Molecule(acs.getAtomContainer(0));
            xyzWriter.write(mol);
        }
        catch (CDKException e)
        {
            e.printStackTrace();
            System.err.println("CDK unable to write XYZ file " + filename);
        }
        catch (Throwable t2)
        {
            t2.printStackTrace();
            System.err.println("Failure in writing XYZ: " + t2);
            System.exit(-1);
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
                System.err.println("Error in writing: " + ioe);
                System.exit(-1);
            }
        }
        XYZWriter xyzWriterAppend = null;
        try {
            xyzWriterAppend = new XYZWriter(new FileWriter(new File(filename),
                                                                         true));
            for (int i=1; i<acs.getAtomContainerCount(); i++)
            {
                IMolecule mol2 = new Molecule(acs.getAtomContainer(i));
                xyzWriterAppend.write(mol2);
            }
        }
        catch (CDKException e)
        {
            e.printStackTrace();
            System.err.println("CDK unable to append XYZ file " + filename);
        }
        catch (Throwable t2)
        {
            t2.printStackTrace();
            System.err.println("Failure in appending to XYZ: " + t2);
            System.exit(-1);
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
                System.err.println("Error in writing: " + ioe);
                System.exit(-1);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes a single line to the end of a TXT file
     * @param filename name of the target file
     * @param txt the line to be written
     * @param append set to <code>true</code> to append instead of overwrite the
     * target file
     */

    public static void writeTXTAppend(String filename, String txt,
                                      boolean append)
    {
        FileWriter writer = null;
        try
        {
            writer = new FileWriter(filename,append);
            writer.write(txt + newline);
        } catch (Throwable t) {
            System.err.println("Failure in writing TXT: " + t);
            System.exit(-1);
        } finally {
             try {
                 if(writer != null)
                     writer.close();
             } catch (IOException ioe) {
                 System.err.println("Error in writing: " + ioe);
                 System.exit(-1);
             }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes a list of lines on a TXT file
     * @param filename name of the target file
     * @param txt the list of lines to be written
     * @param append set to <code>true</code> to append instead of overwrite 
     * the target file
     */

    public static void writeTXTAppend(String filename, ArrayList<String> txt,
                                                                 boolean append)
    {
        FileWriter writer = null;
        try
        {
            writer = new FileWriter(filename,append);
            for (int i=0; i<txt.size(); i++)
            {
                writer.write(txt.get(i) + newline);
            }
        } catch (Throwable t) {
            System.err.println("Failure in writing ArrayList<String>: " + txt 
                                                                           + t);
            System.exit(-1);
        } finally {
             try {
                 if(writer != null)
                     writer.close();
             } catch (IOException ioe) {
                 System.err.println("Error in writing: " + ioe);
                 System.exit(-1);
             }
        }
    }


//------------------------------------------------------------------------------

    /**
     * Copy the content of text files into other files
     * @param inFile path and name of the source file
     * @param outFile path and name of the target file
     * @param append if <code>true</code> required to append the content of the
     * input file at the end of the output file
     */

    public static void copyFile(String inFile, String outFile, boolean append)
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
            System.err.println("\nERROR in copying file "+inFile+": "+t);
            System.exit(0);
        } 
    }

//------------------------------------------------------------------------------

    /**
     * Delete a file 
     * @param fileName path and name of the file to remove
     */

    public static void deleteFile(String fileName)
    {
        File f = new File(fileName);
        if (f.exists())
            if (f.canWrite())
                f.delete();
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

    public static void filterSDasTXT(String inFile, String outFile, 
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
            System.err.println("File Not Found: " + inFile);
            System.err.println(fnf.getMessage());
            System.exit(0);
        } catch (IOException ioex) {
            System.err.println(ioex.getMessage());
            System.exit(0);
        } finally {
            try {
                if (buffRead != null)
                    buffRead.close();
            } catch (IOException ioex2) {
                System.err.println(ioex2.getMessage());
                System.exit(0);
            }
        }

    }

//------------------------------------------------------------------------------

    /**
     * Stop execution, Ask the user to pres RETURN key and move on
     */

    public static void pause()
    {
        System.err.println("Press <RETURN> to continue");
        try
        {
            @SuppressWarnings("unused")
			int inchar = System.in.read();
        }
        catch (IOException e)
        {
            System.err.println("Error reading from user");
        }
    }

//------------------------------------------------------------------------------

}
