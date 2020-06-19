package autocompchem.chemsoftware.nwchem;

import java.util.ArrayList;

/*
 *   Copyright (C) 2016  Marco Foscato
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
import java.util.Map;

import autocompchem.run.Terminator;

/**
 * This object represents a single NWChem directive that consists of
 * a name, a list of {@link NWChemKeyword}s, 
 * a list of subordinate {@link NWChemDirective}s, 
 * and some tabled {@link NWChemDirectiveData}. 
 * All fields are optional and may be empty.
 * This object can be converted to a formatted string according to the syntax
 * of NWChem input files. In particular, keywords are reported in the same
 * line of this directive's name or in concatenated lines, 
 * and subDirectives are listed in an indented block of text.
 *
 * @author Marco Foscato
 */

public class NWChemDirective
{
    /**
     * Directive name
     */
    private String name = "#noname";

    /**
     * List of defined keywords
     */
    private ArrayList<NWChemKeyword> keywords;

    /**
     * List of subordinate directives
     */
    private ArrayList<NWChemDirective> subDirectives;

    /**
     * Attached data
     */
    private ArrayList<NWChemDirectiveData> dirData;

//-----------------------------------------------------------------------------

    /**
     * Constructor for an empty directive
     */

    public NWChemDirective()
    {
        keywords = new ArrayList<NWChemKeyword>();
        subDirectives = new ArrayList<NWChemDirective>();        
        dirData = new ArrayList<NWChemDirectiveData>();
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for an empty directive with a name
     * @param name the name if the directive
     */

    public NWChemDirective(String name)
    {
        this.name = name;
        keywords = new ArrayList<NWChemKeyword>();
        subDirectives = new ArrayList<NWChemDirective>();
        dirData = new ArrayList<NWChemDirectiveData>();
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for directive from fields
     * @param name the name of the directive
     * @param keywords the list of keywords
     * @param subDirectives the list of subdirectives
     * @param dirData the list of data blocks
     */

    public NWChemDirective(String name, ArrayList<NWChemKeyword> keywords, 
                                       ArrayList<NWChemDirective> subDirectives,
                                         ArrayList<NWChemDirectiveData> dirData)
    {
        this.name = name;
        this.keywords = keywords;
        this.subDirectives = subDirectives;
        this.dirData = dirData;
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for a directive from formatted text (i.e., from jobDetails)
     * @param name the name of the directive
     * @param lines the formatted text to be parsed
     */

    public NWChemDirective(String name, ArrayList<String> lines)
    {
        this.name = name;
        keywords = new ArrayList<NWChemKeyword>();
        subDirectives = new ArrayList<NWChemDirective>();
        dirData = new ArrayList<NWChemDirectiveData>();

        Map<String,ArrayList<String>> subDirs = 
                                        new HashMap<String,ArrayList<String>>();
        for (String line : lines)
        {
            String upLine = line.toUpperCase();
            if (upLine.trim().length() == 0)
            {
                //no content is a possible scenario
            }
            else if (upLine.startsWith(NWChemConstants.LABLOUDKEY)
                     || upLine.startsWith(NWChemConstants.LABMUTEKEY))
            {
                NWChemKeyword kw = new NWChemKeyword(line);
                keywords.add(kw);
            }
            else if (upLine.startsWith(NWChemConstants.LABDATA))
            {
                NWChemDirectiveData data = new NWChemDirectiveData(line);
                dirData.add(data);
            }
            else if (upLine.startsWith(NWChemConstants.LABDIRECTIVE))
            {
                line = line.substring(NWChemConstants.LABDIRECTIVE.length());
                String[] parts = line.split("\\s+",2);
                String subDirName = parts[0].toUpperCase();
                if (subDirs.containsKey(subDirName))
                {
                    if (parts.length > 1)
                    {
                        subDirs.get(subDirName).add(parts[1]);
                    }
                }
                else
                {
                    ArrayList<String> subDirLines = new ArrayList<String>();
                    if (parts.length > 1)
                    {
                        subDirLines = new ArrayList<String>(
                                                       Arrays.asList(parts[1]));
                    }
                    subDirs.put(subDirName,subDirLines);
                }
            }
            else
            {
                Terminator.withMsgAndStatus("ERROR! Unable to understand the "
                            + "content of directive '" + name 
                            + "'. The problem is this line: '" + line + "'",-1);
            } 
        }
        for (String subDirName : subDirs.keySet())
        {
            NWChemDirective subDir = new NWChemDirective(subDirName,subDirs.get(
                                                                   subDirName));
            subDirectives.add(subDir);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Return the name
     * @return the name of the directive
     */

    public String getName()
    {
        return name;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns all subdirective.
     * @return the list of subdirectives
     */

    public ArrayList<NWChemDirective> getAllSubDirectives()
    {
        return subDirectives;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the subdirective with the given name. 
     * @param name the name of the subdirective to get
     * @return the subdirective with the given name or null it it does nto exist
     */

    public NWChemDirective getSubDirective(String name)
    {
        for (NWChemDirective subDir : subDirectives)
        {
            if (subDir.getName().toUpperCase().equals(name.toUpperCase()))
            {
                return subDir;
            }
        }
        return null;
    }

//-----------------------------------------------------------------------------

    /**
     * Remove a subdirective of this directive.
     * @param dir the directive that has to be removed
     */

    public void deleteSubDirective(NWChemDirective dir)
    {
        subDirectives.remove(dir);
    }

//-----------------------------------------------------------------------------

    /**
     * Returns all keywords.
     * @return the list of keywords
     */

    public ArrayList<NWChemKeyword> getAllKeywords()
    {
        return keywords;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the keyword with the given name. Only keyword belonging to
     * this directive can be returned. Keywords of sub directives are ignored. 
     * @param name the name of the keyword to get
     * @return the keyword with the given name or null if such keyword
     * does not exist
     */

    public NWChemKeyword getKeyword(String name)
    {
        for (NWChemKeyword kw : keywords)
        {
            if (kw.getName().toUpperCase().equals(name.toUpperCase()))
            {
                return kw;
            }
        }
        return null;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns all data blocks contained in this directive.
     * @return the list of data blocks
     */

    public ArrayList<NWChemDirectiveData> getAllDirectiveDataBlocks()
    {
        return dirData;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the data block having the specified name. Data of subordinate
     * directives are ignored.
     * @param name the name of the data block to get
     * @return the data blocks having the specified name or null if such data
     * block does not exist
     */

    public NWChemDirectiveData getDirectiveData(String name)
    {
        for (NWChemDirectiveData data : dirData)
        {
            if (data.getName().toUpperCase().equals(name.toUpperCase()))
            {
                return data;
            }
        }
        return null;
    }

//-----------------------------------------------------------------------------

    /**
     * Add a subordinate directive
     * @param subDirective the subordinate directive to add
     */

    public void addSubDirective(NWChemDirective subDirective)
    {
        subDirectives.add(subDirective);
    }

//-----------------------------------------------------------------------------

    /**
     * Add a keyword to the list of keywords
     * @param kw the keyword to add
     */

    public void addKeyword(NWChemKeyword kw)
    {
        keywords.add(kw);
    }

//-----------------------------------------------------------------------------

    /**
     * Add a new block of data to this directive
     * @param data the block to be added
     */

    public void addDirectiveData(NWChemDirectiveData data)
    {
        dirData.add(data);
    }

//-----------------------------------------------------------------------------

    /**
     * Overwrites the list of subordinate directives
     * @param subDirectives the new list of subordinate directives 
     */

    public void setAllSubDirectives(ArrayList<NWChemDirective> subDirectives)
    {
        this.subDirectives = subDirectives;
    }

//-----------------------------------------------------------------------------

    /**
     * Set or overwrites a single subdirective of this directive. Methods
     * calling this method must make sure the new subdirective is not a
     * parent directive of <code>this</code>, otherwise a loop is created.
     * @param dir the new directive
     * @param owKeys if <code>true</code> makes this method overwrite the
     * keywords of the existing directive. 
     * @param owSubDirs if <code>true</code> makes this method overwrite the
     * sub-directives of the existing directive.
     * @param owData if <code>true</code> makes this method overwrite the
     * data of the existing directive. 
     */

    public void setSubDirective(NWChemDirective dir, boolean owKeys, 
                                              boolean owSubDirs, boolean owData)
    {
        NWChemDirective oldDir = getSubDirective(dir.getName());
        if (oldDir == null)
        {
            this.addSubDirective(dir);
        }
        else
        {
            if (owKeys)
            {
                oldDir.setAllKeywords(dir.getAllKeywords());
            }
            if (owSubDirs)
            {
                oldDir.setAllSubDirectives(dir.getAllSubDirectives());
            }
            if (owData)
            {
                oldDir.setAllDirectiveDataBlocks(
                                               dir.getAllDirectiveDataBlocks());
            }
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Overwrites the list of keywords
     * @param keywords the new list of keywords
     */

    public void setAllKeywords(ArrayList<NWChemKeyword> keywords)
    {
        this.keywords = keywords;
    }

//-----------------------------------------------------------------------------

    /**
     * Set or overwrites a single keyword of this directive.
     * @param kw the new keywords.
     */

    public void setKeyword(NWChemKeyword kw)
    {
        NWChemKeyword oldKw = getKeyword(kw.getName());
        if (oldKw == null)
        {
             addKeyword(kw);
        }
        else
        {
             oldKw.setValue(kw.getValue());
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Overwrites the list of tabled data
     * @param dirData the new list of data
     */

    public void setAllDirectiveDataBlocks(
					 ArrayList<NWChemDirectiveData> dirData)
    {
        this.dirData = dirData;
    }

//-----------------------------------------------------------------------------

    /**
     * Set or overwrites a single block of data of this directive.
     * @param dd the new data block.
     */

    public void setDataDirective(NWChemDirectiveData dd)
    {
        NWChemDirectiveData oldDd = getDirectiveData(dd.getName());
        if (oldDd == null)
        {
             addDirectiveData(dd);
        }
        else
        {
             oldDd.setContent(dd.getContent());
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Custom equality method. Only checks the name of the directive and the
     * size of the lists of keywords, subdirectives, and data blocks.
     * @param other the directive to compare with this one
     * @return <code>true</code> if the two objects are equal
     */

    @Override
    public boolean equals(Object other)
    {
        boolean res = false;
        if (other instanceof NWChemDirective)
        {
            if (this.getName().equals(((NWChemDirective) other).getName()) 
                && this.getAllKeywords().size() 
                           == ((NWChemDirective) other).getAllKeywords().size()
                && this.getAllSubDirectives().size()
                      == ((NWChemDirective) other).getAllSubDirectives().size()
                && this.getAllDirectiveDataBlocks().size() 
                == ((NWChemDirective) other).getAllDirectiveDataBlocks().size())
            {
                res = true;
            }
        }
        return res;
    }

//-----------------------------------------------------------------------------

    /**
     * Produces a formatted block of text (i.e., list of lines)
     * according to the syntax of NWChem input directives. 
     * Note: the SET and UNSET directives in NWChem are reported with different
     * syntax in accordance to NWChem documentation.
     * @return the list of lines for a NWChem input file
     */

    public ArrayList<String> toLinesInput()
    {
        ArrayList<String> lines = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();

	// deal with the name
        String[] parts = name.split(NWChemConstants.SPACEINDIRNAME);
        sb.append(parts[0]);
        for (int i=1; i<parts.length; i++)
        {
            sb.append(" ").append(parts[i]);
        }
        sb.append(" ");

        // keywords are appended in the same line as the directive's name
	Collections.sort(keywords, new NWChemKeywordComparator());
	int ik = 0;
        for (NWChemKeyword k : keywords)
        {
	    ik++;

            String kStr = k.toStringInput();
            int totalLength = sb.length() + kStr.length() + 1;
            //when it gets too long use backslash to concatenate lines
            if (totalLength > NWChemConstants.MAXLINELENGTH) 
            {
                // WARNING!
                // Here we assume the deepest possible directive is in layer
                // ten, but there is no actual limit to the number of layers.
                // The assumption is bases on the most common NWChem input files
                // not having more than 3-4 layers of directives, so a maximum
                // estimate of 10 should be safe.
                int singleKeyLenght = kStr.length() + 10; 
                if (singleKeyLenght > NWChemConstants.MAXLINELENGTH)
                {
                    String[] words = kStr.split("\\s+");
                    for (int i=0; i<words.length; i++)
                    {
			String word = words[i];
			int expectedLength = sb.length() + word.length() + 1;
			if (expectedLength > NWChemConstants.MAXLINELENGTH) 
			{
                            // store the line up to this point
                            String arcLine = sb.toString() + "\\";
                            lines.add(arcLine);
                            // start a new line from scratch
                            sb.delete(0,sb.length());
                            // append indent due to this directive's name
                            for (int j=0; j<name.length(); j++)
                            {
                                sb.append(" ");
                            }
			}
			sb.append(word).append(" ");
                    }
                }
                else
                {
                    // store the line up to this point
                    String arcLine = sb.toString() + "\\";
                    lines.add(arcLine);
                    // start a new line from scratch
                    sb.delete(0,sb.length());
                    // append indent due to this directive's name
                    for (int i=0; i<name.length(); i++)
                    {
                        sb.append(" ");
                    }
                    // append the whole keyword+value string 
                    sb.append(kStr).append(" ");
                }
            }
            else
            {
                sb.append(kStr).append(" ");
            }

	    // Deal with inconsistent syntax of SET and UNSET directives
	    // Yes, for some reason these two directives are witten differently.
	    if (ik<keywords.size()  &&
		(name.toUpperCase().equals("SET") || 
		 name.toUpperCase().equals("UNSET"))) 
	    {
		sb.append(System.getProperty("line.separator"));
		sb.append(name).append(NWChemConstants.SUBDIRECTIVEINDENT);
	    }       
        }
        lines.add(sb.toString());

	//Check against maximum allowed
	if (lines.size() > NWChemConstants.MAXCONCATLINES)
	{
	    //TODO: we can try to reduce length by replasing those parts of 
            // the title that are/were added by AutoCompChem with shorter
	    // words or abbreviations
	    Terminator.withMsgAndStatus("ERROR! Keyword section of directive "
		+ this.getName() + " is more than " 
		+ NWChemConstants.MAXCONCATLINES 
		+ " lines long, but shortening protocol is not "
		+ "implemented in this version of autocompchem. You should use a "
		+ "directive's data block ('" + NWChemConstants.LABDATA
		+ "' in jobDetails file) rather than a "
		+ "keyword.",-1);
	}

        //Then add the data section
        for (NWChemDirectiveData data : dirData)
        {
            for (String dataLine : data.toLinesInput())
            {
                lines.add(NWChemConstants.SUBDIRECTIVEINDENT + dataLine);
            }

            // Deal with inconsistent syntax of SET and UNSET directives
            if (name.toUpperCase().equals("SET") || 
		name.toUpperCase().equals("UNSET"))
            {
		Terminator.withMsgAndStatus("ERROR! Unexpected use of data "
		    + "block inside a '" + name + "' directive. Current NWChem "
		    + "does not support like possibility",-1);
            }
        }

        //Then add the sub-directives
        for (NWChemDirective subDir : subDirectives)
        {
            for (String dirLine : subDir.toLinesInput())
            {
                lines.add(NWChemConstants.SUBDIRECTIVEINDENT + dirLine);
            }
            // Deal with inconsistent syntax of SET and UNSET directives
            if (name.toUpperCase().equals("SET") ||
                name.toUpperCase().equals("UNSET"))
            {
                Terminator.withMsgAndStatus("ERROR! Unexpected subdirective "
                    + "inside a '" + name + "' directive. Current NWChem "
                    + "does not support like possibility",-1);
            }
        }

        //Finally end the directive, unless its a SET/UNSET
	if (!name.toUpperCase().equals("SET") &&
                !name.toUpperCase().equals("UNSET"))
	{
	    // NWChem bugs make NWChem input module expect the END label for 
            // DFT and GEOMETRY directives even when there are only keywords 
            if (subDirectives.size() > 0 || dirData.size() > 0
		|| name.toUpperCase().equals("DFT")
                || name.toUpperCase().equals(NWChemConstants.GEOMDIR))
            {
                lines.add("END");
            }
	}

        return lines;
    }

//-----------------------------------------------------------------------------

    /**
     * Produces a formatted block of text (i.e., list of lines)
     * according to the syntax of NWChem input directives.
     * @return the list of lines for a NWChem job-details file
     */

    public ArrayList<String> toLinesJobDetails()
    {
        ArrayList<String> lines = new ArrayList<String>();
        String root = NWChemConstants.LABDIRECTIVE;
	// Make sure directive's name contains no spaces
	String[] parts = name.split("\\s+");
	root = root + parts[0];
	for (int i=1; i<parts.length; i++)
	{
	    root = root + NWChemConstants.SPACEINDIRNAME + parts[i];
	}
        root = root + " ";
        // Keywords are appended in the same line as the directive's name
        for (NWChemKeyword k : keywords)
        {
            lines.add(root + k.toStringJobDetails());
        }
        //Then add the data section
        for (NWChemDirectiveData data : dirData)
        {
            ArrayList<String> dataLines = data.toLinesJobDetails();
            lines.add(root + dataLines.get(0));
            if (dataLines.size() > 1)
            { 
                for (int i=1; i<dataLines.size(); i++)
                {
                    lines.add(dataLines.get(i));
                }
            }
        }
        //Finally add the sub-directives
        for (NWChemDirective subDir : subDirectives)
        {
            for (String dirLine : subDir.toLinesJobDetails())
            {
                if (dirLine.startsWith(NWChemConstants.LABDIRECTIVE))
                {
                    lines.add(root + dirLine);
                }
                else
                {
                    lines.add(dirLine);
                }
            }
        }
        //Ensure that also empty directives are reported
        if (lines.size() == 0)
        {
            lines.add(root);
        }

        return lines;
    }

//-----------------------------------------------------------------------------
 
}
