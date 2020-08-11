package autocompchem.chemsoftware;


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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import autocompchem.chemsoftware.nwchem.NWChemConstants;
import autocompchem.run.Terminator;
import autocompchem.text.TextAnalyzer;

/**
 * This object represents a single directive that consists of
 * a name, and may include a list of {@link Keyword}s, 
 * a list of subordinate {@link Directive}s, 
 * and some tabled {@link DirectiveData}. 
 * All fields are optional and may be empty.
 *
 * @author Marco Foscato
 */

public class Directive
{
    /**
     * Directive name.
     */
    private String name = "#noname";

    /**
     * List of keywords.
     */
    private ArrayList<Keyword> keywords;

    /**
     * List of subordinate directives.
     */
    private ArrayList<Directive> subDirectives;

    /**
     * Data attached directly to this directive.
     */
    private ArrayList<DirectiveData> dirData;

//-----------------------------------------------------------------------------

    /**
     * Constructor for an empty directive.
     */

    public Directive()
    {
        keywords = new ArrayList<Keyword>();
        subDirectives = new ArrayList<Directive>();        
        dirData = new ArrayList<DirectiveData>();
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for an empty directive with a name.
     * @param name the name if the directive.
     */

    public Directive(String name)
    {
        this.name = name;
        keywords = new ArrayList<Keyword>();
        subDirectives = new ArrayList<Directive>();
        dirData = new ArrayList<DirectiveData>();
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for directive with specific content.
     * @param name the name of the directive.
     * @param keywords the list of keywords.
     * @param subDirectives the list of sub directives.
     * @param dirData the list of data blocks.
     */

    public Directive(String name, ArrayList<Keyword> keywords, 
                                       ArrayList<Directive> subDirectives,
                                         ArrayList<DirectiveData> dirData)
    {
        this.name = name;
        this.keywords = keywords;
        this.subDirectives = subDirectives;
        this.dirData = dirData;
    }

//-----------------------------------------------------------------------------

    /**
     * Return the name of this directive.
     * @return the name of the directive.
     */

    public String getName()
    {
        return name;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns all sub directive.
     * @return the list of sub directives.
     */

    public ArrayList<Directive> getAllSubDirectives()
    {
        return subDirectives;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the sub directive with the given name. 
     * @param name the name of the sub directive to get.
     * @return the sub directive with the given name or null it it doesn't 
     * exist.
     */

    public Directive getSubDirective(String name)
    {
        for (Directive subDir : subDirectives)
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
     * Remove a sub directive of this directive.
     * @param dir the directive that has to be removed.
     */

    public void deleteSubDirective(Directive dir)
    {
        subDirectives.remove(dir);
    }

//-----------------------------------------------------------------------------

    /**
     * Returns all keywords.
     * @return the list of keywords.
     */

    public ArrayList<Keyword> getAllKeywords()
    {
        return keywords;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the keyword with the given name. Only keyword belonging to
     * this directive can be returned. Keywords of sub directives are ignored. 
     * @param name the name of the keyword to get.
     * @return the keyword with the given name or null if such keyword
     * does not exist.
     */

    public Keyword getKeyword(String name)
    {
        for (Keyword kw : keywords)
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
     * @return the list of data blocks.
     */

    public ArrayList<DirectiveData> getAllDirectiveDataBlocks()
    {
        return dirData;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the data block having the specified name. Data of subordinate
     * directives are ignored.
     * @param name the name of the data block to get.
     * @return the data blocks having the specified name or null if such data
     * block does not exist.
     */

    public DirectiveData getDirectiveData(String name)
    {
        for (DirectiveData data : dirData)
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
     * Add a subordinate directive.
     * @param subDirective the subordinate directive to add.
     */

    public void addSubDirective(Directive subDirective)
    {
        subDirectives.add(subDirective);
    }

//-----------------------------------------------------------------------------

    /**
     * Add a keyword to the list of keywords.
     * @param kw the keyword to add.
     */

    public void addKeyword(Keyword kw)
    {
        keywords.add(kw);
    }

//-----------------------------------------------------------------------------

    /**
     * Add a new block of data to this directive.
     * @param data the block to be added.
     */

    public void addDirectiveData(DirectiveData data)
    {
        dirData.add(data);
    }

//-----------------------------------------------------------------------------

    /**
     * Overwrites the list of subordinate directives.
     * @param subDirectives the new list of subordinate directives .
     */

    public void setAllSubDirectives(ArrayList<Directive> subDirectives)
    {
        this.subDirectives = subDirectives;
    }

//-----------------------------------------------------------------------------

    /**
     * Set or overwrites a single sub directive of this directive. Methods
     * calling this method must make sure the new sub directive is not a
     * parent directive of <code>this</code>, otherwise a loop is created.
     * @param dir the new directive.
     * @param owKeys if <code>true</code> makes this method overwrite the
     * keywords of the existing directive. 
     * @param owSubDirs if <code>true</code> makes this method overwrite the
     * sub-directives of the existing directive.
     * @param owData if <code>true</code> makes this method overwrite the
     * data of the existing directive. 
     */

    public void setSubDirective(Directive dir, boolean owKeys, 
                                              boolean owSubDirs, boolean owData)
    {
        Directive oldDir = getSubDirective(dir.getName());
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
     * Overwrites the list of keywords.
     * @param keywords the new list of keywords.
     */

    public void setAllKeywords(ArrayList<Keyword> keywords)
    {
        this.keywords = keywords;
    }

//-----------------------------------------------------------------------------

    /**
     * Set or overwrites a single keyword of this directive.
     * @param kw the new keywords.
     */

    public void setKeyword(Keyword kw)
    {
        Keyword oldKw = getKeyword(kw.getName());
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
     * Overwrites the list of tabled data.
     * @param dirData the new list of data.
     */

    public void setAllDirectiveDataBlocks(ArrayList<DirectiveData> dirData)
    {
        this.dirData = dirData;
    }

//-----------------------------------------------------------------------------

    /**
     * Set or overwrites a single block of data of this directive.
     * @param dd the new data block.
     */

    public void setDataDirective(DirectiveData dd)
    {
        DirectiveData oldDd = getDirectiveData(dd.getName());
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
     * size of the lists of keywords, sub directives, and data blocks.
     * @param other the directive to compare with this one
     * @return <code>true</code> if the two objects are equal
     */

    @Override
    public boolean equals(Object other)
    {
        boolean res = false;
        if (other instanceof Directive)
        {
            if (this.getName().equals(((Directive) other).getName()) 
                && this.getAllKeywords().size() 
                           == ((Directive) other).getAllKeywords().size()
                && this.getAllSubDirectives().size()
                      == ((Directive) other).getAllSubDirectives().size()
                && this.getAllDirectiveDataBlocks().size() 
                == ((Directive) other).getAllDirectiveDataBlocks().size())
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
     * @return the list of lines for a NWChem job-details file
     */

    public ArrayList<String> toLinesJobDetails()
    {
        ArrayList<String> lines = new ArrayList<String>();
        String root = ChemSoftConstants.JDLABDIRECTIVE + name + " ";
        
        // Keywords are appended in the same line as the directive's name
        for (Keyword k : keywords)
        {
            lines.add(root + k.toStringJobDetails());
        }
        
        //Then add the data section
        for (DirectiveData data : dirData)
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
        for (Directive subDir : subDirectives)
        {
            for (String dirLine : subDir.toLinesJobDetails())
            {
                if (dirLine.startsWith(ChemSoftConstants.JDLABDIRECTIVE))
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
