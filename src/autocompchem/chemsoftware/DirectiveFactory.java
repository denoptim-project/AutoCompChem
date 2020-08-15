package autocompchem.chemsoftware;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import autocompchem.run.Terminator;
import autocompchem.text.TextAnalyzer;


/**
 * Factory building Directives.
 * 
 * @author Marco Foscato
 */

public class DirectiveFactory
{

//------------------------------------------------------------------------------

    /**
     * Make all directives defined in lines of formatted text. 
     * The format is expected to be that of job details files.
     * @param lines the lines to parse
     * @return the list of Directives.
     */

    public static ArrayList<Directive> buildAllFromJDText(
    		ArrayList<String> lines)
    {	
        TreeMap<String,ArrayList<String>> dirStrings = 
                new TreeMap<String,ArrayList<String>>();
        
        ArrayList<String> linesPack = 
        		TextAnalyzer.readTextWithMultilineBlocks(lines,
        		ChemSoftConstants.JDCOMMENT, 
        		ChemSoftConstants.JDOPENBLOCK, 
        		ChemSoftConstants.JDCLOSEBLOCK);
        
        for (String line : linesPack)
    	{
    		line = line.trim();
    		String uLine = line.toUpperCase();
    		
    		if (uLine.startsWith(ChemSoftConstants.JDLABDIRECTIVE))
            {
                line = line.substring(
                		ChemSoftConstants.JDLABDIRECTIVE.length());
                uLine = uLine.substring(
                		ChemSoftConstants.JDLABDIRECTIVE.length());
                
                int iKeyM = uLine.indexOf(ChemSoftConstants.JDLABMUTEKEY);
                if (iKeyM == -1)
                	iKeyM = 100000;
                int iKeyL = uLine.indexOf(ChemSoftConstants.JDLABLOUDKEY);
                if (iKeyL == -1)
                	iKeyL = 100000;
                int iDir = uLine.indexOf(ChemSoftConstants.JDLABDIRECTIVE);
                if (iDir == -1)
                	iDir = 100000;
                int iDat = uLine.indexOf(ChemSoftConstants.JDLABDATA);
                if (iDat == -1)
                	iDat = 100000;
                
                int iNxt = Math.min(iKeyM,Math.min(iKeyL,Math.min(iDat,iDir)));
                
                String subDirName = "";
                if (iNxt < 100000)
                {
                	subDirName = line.substring(0, iNxt).trim();
                } else {
                	subDirName = line.trim();
                }
                
                if (dirStrings.containsKey(subDirName))
                {
                	if (iNxt < 100000)
                	{
                		dirStrings.get(subDirName).add(line.substring(iNxt));
                	}
                } else {
                	if (iNxt < 100000)
                	{ 
                		dirStrings.put(subDirName,new ArrayList<String>(
                				Arrays.asList(line.substring(iNxt))));
                	} else {
                		dirStrings.put(subDirName,new ArrayList<String>());
                	}
                }
            } else {
            	System.out.println("WARNING! The following line is not part or "
            			+ "any definition of Directive, but is found while "
            			+ "parsing text into Directives. Ignoring line '" 
            			+ line + "'.");
            }
    	}
        
        ArrayList<Directive> allDirs = new ArrayList<Directive>();
        for (String dirName : dirStrings.keySet())
        {
            Directive dir = DirectiveFactory.buildFromJDText(dirName,
            		dirStrings.get(dirName));
            allDirs.add(dir);
        }
        
    	return allDirs;
    }
    
//------------------------------------------------------------------------------

    /**
     * Build a directive from lines of formatted text. The format is expected
     * to be that of job details files. Any line that pertains any directive or
     * component that is not the first outermost directive found in the text,
     * will be ignored.
     * @param lines the lines to parse.
     * @return the Directive.
     */

    public static Directive buildFromJDText(ArrayList<String> lines)
    {
        ArrayList<String> linesPacked = 
        		TextAnalyzer.readTextWithMultilineBlocks(lines,
        		ChemSoftConstants.JDCOMMENT, 
        		ChemSoftConstants.JDOPENBLOCK, 
        		ChemSoftConstants.JDCLOSEBLOCK);
        
    	String directiveName = "noname";
    	String dirLineHead = ChemSoftConstants.JDLABDIRECTIVE + directiveName;
    	
    	// First we take the directive name from the first acceptable line
    	// and we remove any line that does not pertain components of this
    	// directive.
    	boolean first = true;
    	ArrayList<String> purgedLines = new ArrayList<String>();
    	for (String line : linesPacked)
    	{
    		line = line.trim();
    		
    		if (line.startsWith(ChemSoftConstants.JDCOMMENT))
    			continue;
    		
    		if (line.startsWith(ChemSoftConstants.JDLABDIRECTIVE))
    		{
    			if (first)
    			{
                    String sLine = line.substring(
                    		ChemSoftConstants.JDLABDIRECTIVE.length());
                    String uLine = sLine.toUpperCase();
                    
                    int iKeyM = uLine.indexOf(ChemSoftConstants.JDLABMUTEKEY);
                    if (iKeyM == -1)
                    	iKeyM = 100000;
                    int iKeyL = uLine.indexOf(ChemSoftConstants.JDLABLOUDKEY);
                    if (iKeyL == -1)
                    	iKeyL = 100000;
                    int iDir = uLine.indexOf(ChemSoftConstants.JDLABDIRECTIVE);
                    if (iDir == -1)
                    	iDir = 100000;
                    int iDat = uLine.indexOf(ChemSoftConstants.JDLABDATA);
                    if (iDat == -1)
                    	iDat = 100000;
                    
                    int iNxt = Math.min(iKeyM, Math.min(iKeyL, 
                    		Math.min(iDat, iDir)));
                    
                    if (iNxt < 100000)
                    {
                    	directiveName = sLine.substring(0, iNxt).trim();
                    } else {
                    	directiveName = sLine.trim();
                    }
                    
                    first = false;
    			}
    			
    			dirLineHead = ChemSoftConstants.JDLABDIRECTIVE + directiveName;
    					
				if (line.trim().toUpperCase().startsWith(
						dirLineHead.toUpperCase()))
				{
					line = line.substring(line.indexOf(dirLineHead) 
							+ dirLineHead.length()).trim();
					purgedLines.add(line);
				} else {
					purgedLines.add(line);
				}
    		} else {
				purgedLines.add(line);
			}
    	}
    	
    	return buildFromJDText(directiveName,purgedLines);
    }

//-----------------------------------------------------------------------------
    
    /**
     * Build a directive with a given name from lines of formatted text that 
     * define only the components of the target directive, i.e., each component
     * is assumed to belong to the target directive, so the text must not 
     * include the definition of the target directive as parent object of each
     * component.
     * The format is expected to be that of job details files.
     * @param name the name of the target directive to build.
     * @param lines the lines to parse to build the directive sub-components
     * @return the target Directive.
     */

    public static Directive buildFromJDText(String name, ArrayList<String> lines)
    {
    	Directive d = new Directive(name);

        TreeMap<String,ArrayList<String>> subDirs = 
                                       new TreeMap<String,ArrayList<String>>();
        
        ArrayList<String> linesPacked = 
        		TextAnalyzer.readTextWithMultilineBlocks(lines,
        		ChemSoftConstants.JDCOMMENT, 
        		ChemSoftConstants.JDOPENBLOCK, 
        		ChemSoftConstants.JDCLOSEBLOCK);
        
        for (String line : linesPacked)
        {
            String uLine = line.toUpperCase();
            if (uLine.trim().length() == 0)
            {
                //no content is a possible scenario
            }
            else if (uLine.startsWith(ChemSoftConstants.JDLABLOUDKEY)
                     || uLine.startsWith(ChemSoftConstants.JDLABMUTEKEY))
            {
                Keyword kw = new Keyword(line);
                d.addKeyword(kw);
            }
            else if (uLine.startsWith(ChemSoftConstants.JDLABDATA))
            {
                DirectiveData data = new DirectiveData(line);
                d.addDirectiveData(data);
            }
            else if (uLine.startsWith(ChemSoftConstants.JDLABDIRECTIVE))
            {
                line = line.substring(
                		ChemSoftConstants.JDLABDIRECTIVE.length());
                uLine = uLine.substring(
                		ChemSoftConstants.JDLABDIRECTIVE.length());
                
                int iKeyM = uLine.indexOf(ChemSoftConstants.JDLABMUTEKEY);
                if (iKeyM == -1)
                	iKeyM = 100000;
                int iKeyL = uLine.indexOf(ChemSoftConstants.JDLABLOUDKEY);
                if (iKeyL == -1)
                	iKeyL = 100000;
                int iDir = uLine.indexOf(ChemSoftConstants.JDLABDIRECTIVE);
                if (iDir == -1)
                	iDir = 100000;
                int iDat = uLine.indexOf(ChemSoftConstants.JDLABDATA);
                if (iDat == -1)
                	iDat = 100000;
                
                int iNxt = Math.min(iKeyM,Math.min(iKeyL,Math.min(iDat,iDir)));
                
                String subDirName = "";
                if (iNxt < 100000)
                {
                	subDirName = line.substring(0, iNxt).trim();
                } else {
                	subDirName = line.trim();
                }
                
                if (subDirs.containsKey(subDirName))
                {
                	if (iNxt < 100000)
                	{
                		subDirs.get(subDirName).add(line.substring(iNxt));
                	}
                } else {
                	if (iNxt < 100000)
                	{ 
                		subDirs.put(subDirName,new ArrayList<String>(
                				Arrays.asList(line.substring(iNxt))));
                	} else {
                		subDirs.put(subDirName,new ArrayList<String>());
                	}
                }
            }
            else
            {
                Terminator.withMsgAndStatus("ERROR! Unable to parse line to "
                            + "make content of directive '" + name + "'. "
                            + "The problem is this line: '" + line + "'",-1);
            } 
        }
        
        for (String subDirName : subDirs.keySet())
        {
            Directive subDir = DirectiveFactory.buildFromJDText(subDirName,
            		subDirs.get(subDirName));
            d.addSubDirective(subDir);
        }
        
        return d;
    }
    
//-----------------------------------------------------------------------------

}
