package autocompchem.chemsoftware.qmmm;

import java.util.ArrayList;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import autocompchem.run.Terminator;

/**
 * This object represents a single QMMM section that consists of
 * a name, a list of {@link QMMMKeyword}s, 
 * a list of subordinate {@link QMMMSection}s, 
 * and some information organized in the form of a {@link QMMMList}. 
 * All fields are optional and may be empty.
 * This object can be converted to a formatted string according to the syntax
 * of QMMM input files. 
 *
 * @author Marco Foscato
 */

public class QMMMSection
{
    /**
     * Section name
     */
    private String name = "#unnamedsection";

    /**
     * List of defined keywords
     */
    private ArrayList<QMMMKeyword> keywords;

    /**
     * List of subordinate sections
     */
    private ArrayList<QMMMSection> subSections;

    /**
     * Attached list
     */
    private ArrayList<QMMMList> secLists;

//-----------------------------------------------------------------------------

    /**
     * Constructor for an empty section
     */

    public QMMMSection()
    {
        keywords = new ArrayList<QMMMKeyword>();
        subSections = new ArrayList<QMMMSection>();        
        secLists = new ArrayList<QMMMList>();
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for an empty section with a name
     * @param name the name if the section
     */

    public QMMMSection(String name)
    {
        this.name = name;
        keywords = new ArrayList<QMMMKeyword>();
        subSections = new ArrayList<QMMMSection>();
        secLists = new ArrayList<QMMMList>();
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for section from fields
     * @param name the name if the section
     * @param keywords the list of keywords
     * @param subSections the list of subsections
     * @param secLists the list of lists
     */

    public QMMMSection(String name, ArrayList<QMMMKeyword> keywords, 
                                       ArrayList<QMMMSection> subSections,
                                         ArrayList<QMMMList> secLists)
    {
        this.name = name;
        this.keywords = keywords;
        this.subSections = subSections;
        this.secLists = secLists;
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for a section from formatted text (i.e., from jobDetails)
     * @param  name the name of the section to create
     * @param lines the formatted text to be parsed
     */

    public QMMMSection(String name, ArrayList<String> lines)
    {
        this.name = name;
        keywords = new ArrayList<QMMMKeyword>();
        subSections = new ArrayList<QMMMSection>();
        secLists = new ArrayList<QMMMList>();

        Map<String,ArrayList<String>> subDirs = 
                                        new HashMap<String,ArrayList<String>>();
        for (String line : lines)
        {
            String upLine = line.toUpperCase();
            if (upLine.trim().length() == 0)
            {
                //no content is a possible scenario
            }
            else if (upLine.startsWith(QMMMConstants.LABLOUDKEY)
                     || upLine.startsWith(QMMMConstants.LABMUTEKEY))
            {
                QMMMKeyword kw = new QMMMKeyword(line);
                addKeyword(kw);
            }
            else if (upLine.startsWith(QMMMConstants.LABDATA))
            {
                QMMMList list = new QMMMList(line);
                addList(list);
            }
            else if (upLine.startsWith(QMMMConstants.LABSECTION))
            {
                line = line.substring(QMMMConstants.LABSECTION.length());
                String[] parts = line.split("\\s+",2);
                String subDirName = parts[0].toUpperCase();
                if (subDirs.containsKey(subDirName))
                {
                    subDirs.get(subDirName).add(parts[1]);
                }
                else
                {
                    ArrayList<String> subDirLines = new ArrayList<String>(
                                                       Arrays.asList(parts[1]));
                    subDirs.put(subDirName,subDirLines);
                }
            }
            else
            {
                Terminator.withMsgAndStatus("ERROR! Unable to understand the "
                            + "content of section '" + name 
                            + "'. The problem is this line: '" + line + "'",-1);
            } 
        }
        for (String subDirName : subDirs.keySet())
        {
            QMMMSection subDir = new QMMMSection(subDirName,subDirs.get(
                                                                   subDirName));
            subSections.add(subDir);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Return the name
     * @return the name of the section
     */

    public String getName()
    {
        return name;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns all subsection.
     * @return the list of subsections
     */

    public ArrayList<QMMMSection> getAllSubSections()
    {
        return subSections;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the subsection with the given name. If no such section exists
     * then a new one is created
     * @param name the name of the subsection
     * @return the subsection with the given name 
     */

    public QMMMSection getSubSection(String name)
    {
        for (QMMMSection subDir : subSections)
        {
            if (subDir.getName().equals(name))
            {
                return subDir;
            }
        }
        QMMMSection newSubDir = new QMMMSection(name);
        subSections.add(newSubDir);
        return newSubDir;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns all keywords.
     * @return the list of keywords
     */

    public ArrayList<QMMMKeyword> getAllKeywords()
    {
        return keywords;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the keyword with the given name. Only keyword belonging to
     * this section can be returned. Keywords of sub sections are ignored. 
     * @param name the name of the keyword
     * @return the keyword with the given name or null if such keyword
     * does not exist
     */

    public QMMMKeyword getKeyword(String name)
    {
        for (QMMMKeyword kw : keywords)
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
     * Returns all lists contained in this section.
     * @return the list of lists
     */

    public ArrayList<QMMMList> getAllLists()
    {
        return secLists;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list having the specified name. Lists belonging to
     * subordinate sections are ignored.
     * @param name the name of the list to get
     * @return the list having the specified name or null if such list
     * block does not exist
     */

    public QMMMList getList(String name)
    {
        for (QMMMList list : secLists)
        {
            if (list.getName().toUpperCase().equals(name.toUpperCase()))
            {
                return list;
            }
        }
        return null;
    }

//-----------------------------------------------------------------------------

    /**
     * Add a subordinate section
     * @param subSection the subordinate section to add
     */

    public void addSubSection(QMMMSection subSection)
    {
        subSections.add(subSection);
    }

//-----------------------------------------------------------------------------

    /**
     * Add a keyword to the list of keywords. If the keyword exists alredy then
     * its value is overwritten.
     * @param kw the keyword to add
     */

    public void addKeyword(QMMMKeyword kw)
    {
        QMMMKeyword oldKw = getKeyword(kw.getName());
        if (oldKw == null)
        {
             keywords.add(kw);
        }
        else
        {
             oldKw.setValue(kw.getValue());
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Add a new block of list to this section. If the list exists already than
     * its content is overwritten.
     * @param list the block to be added
     */

    public void addList(QMMMList list)
    {
        QMMMList oldList = getList(list.getName());
        if (oldList == null)
        {
             secLists.add(list);
        }
        else
        {
             oldList.setContent(list.getContent());
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Overwrites the list of subordinate sections
     * @param subSections the new list of subordinate sections 
     */

    public void setAllSubSections(ArrayList<QMMMSection> subSections)
    {
        this.subSections = subSections;
    }

//-----------------------------------------------------------------------------

    /**
     * Overwrites the list of keywords
     * @param keywords the new list of keywords
     */

    public void setAllKeywords(ArrayList<QMMMKeyword> keywords)
    {
        this.keywords = keywords;
    }

//-----------------------------------------------------------------------------

    /**
     * Overwrites the list of tabled list
     * @param secLists the new list of list
     */

    public void setAllLists(ArrayList<QMMMList> secLists)
    {
        this.secLists = secLists;
    }

//-----------------------------------------------------------------------------

    /**
     * Custom equality method. Only checks the name of the section and the
     * size of the lists of keywords, subsections, and lists.
     * @param other the section to compare with this one
     * @return <code>true</code> if the two objects are equal
     */

    @Override
    public boolean equals(Object other)
    {
        boolean res = false;
        if (other instanceof QMMMSection)
        {
            if (this.getName().equals(((QMMMSection) other).getName()) 
                && this.getAllKeywords().size() 
                           == ((QMMMSection) other).getAllKeywords().size()
                && this.getAllSubSections().size()
                      == ((QMMMSection) other).getAllSubSections().size()
                && this.getAllLists().size() 
                == ((QMMMSection) other).getAllLists().size())
            {
                res = true;
            }
        }
        return res;
    }

//-----------------------------------------------------------------------------

    /**
     * Produces a formatted block of text (i.e., list of lines)
     * according to the syntax of QMMM input sections.
     * @return the list of lines for a QMMM input file
     */

    public ArrayList<String> toLinesInput()
    {
        ArrayList<String> lines = new ArrayList<String>();

        // section name (nothing in same line as section name)
        lines.add(name.toUpperCase());

        // keywords 
        for (QMMMKeyword k : keywords)
        {
            lines.add(QMMMConstants.SUBSECTIONINDENT + k.toStringInput());
        }

        // lists of data
        for (QMMMList list : secLists)
        {
            for (String listLine : list.toLinesInput())
            {
                lines.add(QMMMConstants.SUBSECTIONINDENT + listLine);
            }
        }

        // sub-sections
        for (QMMMSection subDir : subSections)
        {
            for (String secLine : subDir.toLinesInput())
            {
                lines.add(QMMMConstants.SUBSECTIONINDENT + secLine);
            }
        }

        //Finally end the section
        if (lines.size() > 1 
            && !QMMMConstants.MASTERSECLST.contains(name.toUpperCase()))
        {
            lines.add("END");
        }

        return lines;
    }

//-----------------------------------------------------------------------------

    /**
     * Produces a formatted block of text (i.e., list of lines)
     * according to the syntax of QMMM input sections.
     * @return the list of lines for a QMMM inout file
     */

    public ArrayList<String> toLinesJobDetails()
    {
        ArrayList<String> lines = new ArrayList<String>();
        String root = QMMMConstants.LABSECTION + name + " ";

        // keywords are ppended in the same line as the section's name
        for (QMMMKeyword k : keywords)
        {
            lines.add(root + k.toStringJobDetails());
        }

        //Then add the list section
        for (QMMMList list : secLists)
        {
            ArrayList<String> listLines = list.toLinesJobDetails();
            lines.add(root + listLines.get(0));
            if (listLines.size() > 1)
            { 
                for (int i=1; i<listLines.size(); i++)
                {
                    lines.add(listLines.get(i));
                }
            }
        }

        //Finally add the sub-sections
        for (QMMMSection subDir : subSections)
        {
            for (String secLine : subDir.toLinesInput())
            {
                if (secLine.startsWith(QMMMConstants.LABSECTION))
                {
                    lines.add(root + secLine);
                }
                else
                {
                    lines.add(secLine);
                }
            }
        }

        return lines;
    }

//-----------------------------------------------------------------------------
 
}
