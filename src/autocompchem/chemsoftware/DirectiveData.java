package autocompchem.chemsoftware;

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
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;

import autocompchem.datacollections.NamedData;
import autocompchem.run.Terminator;
import autocompchem.text.TextBlock;

/**
 * This object represents a block of text-based data.
 *
 * @author Marco Foscato
 */

public class DirectiveData extends NamedData implements IDirectiveComponent
{

//-----------------------------------------------------------------------------

    /**
     * Constructor for an empty data block
     */

    public DirectiveData()
    {
    	super();
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor from formatted text (i.e., job details line)
     * @param jdLine the text to parse
     */

    public DirectiveData(String jdLine)
    {
    	super();
    	
        if (jdLine.toUpperCase().startsWith(ChemSoftConstants.JDLABDATA))
        {
            jdLine = jdLine.substring(ChemSoftConstants.JDLABDATA.length());
        }
        String[] parts = jdLine.split(ChemSoftConstants.JDDATAVALSEPARATOR,2);
        super.setReference(parts[0]);
        
        if (parts.length < 2)
        {
            Terminator.withMsgAndStatus("ERROR! Cannot discriminate between "
            		+ "reference name and block of data in '"
            		+  jdLine + "'. Check jobdetails file.",-1);
        }
        String block = parts[1];
        if (block.toUpperCase().startsWith(System.getProperty(
                                                             "line.separator")))
        {
            block = block.substring(System.getProperty(
                                                    "line.separator").length());
        }
        String[] dataLines = block.split(System.getProperty("line.separator"));
        
        super.setValue(new TextBlock(Arrays.asList(dataLines)));
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor from content
     * @param name the name of this block of data
     * @param lines the list of lines representing the contained data
     */

    public DirectiveData(String name, ArrayList<String> lines)
    {
    	super();
    	super.setReference(name);
    	super.setValue(new TextBlock(lines));        
    }

//-----------------------------------------------------------------------------

    /**
     * Return the name of this block of data
     * @return the name of this block of data
     */

    public String getName()
    {
        return super.getReference();
    }

//-----------------------------------------------------------------------------
 
    /**
     * @return the kind of directive component this is.
     */
    
	public DirectiveComponentType getComponentType() 
	{
		return DirectiveComponentType.DIRECTIVEDATA;
	}
	
//-----------------------------------------------------------------------------

    /**
     * Return the content of this block of data
     * @return the content of this block of data
     */

	public ArrayList<String> getLines()
    {
    	ArrayList<String> list = new ArrayList<String>();
    	// TODO: improve. This is done to retain compatibility with legacy code
    	if (this.getType().equals(NamedDataType.TEXTBLOCK))
    	{
    		return (ArrayList<String>) 
    				((TextBlock) super.getValue());
    	} 
    	else if ((this.getType().equals(NamedDataType.STRING)))
    	{
    		list.add(this.getValueAsString());
    		return list;
    	}
    	list.add("Could not get lines out of " + this.getType().toString());
        return list;
    }

//-----------------------------------------------------------------------------
    
    /**
     * Checks if there is any ACC task definition within this directive.
     * @return <code>true</code> if there is at least one ACC task definition.
     */
    
	public boolean hasACCTask() 
	{
		if (this.getType().equals(NamedDataType.TEXTBLOCK))
		{
			for (String l : (TextBlock) this.getValue())
			{
				if (l.contains(ChemSoftConstants.JDLABACCTASK))
					return true;
			}
		}
		return false;
	}
	
//-----------------------------------------------------------------------------

    /**
     * Produces a formatted block of text (i.e., list of lines)
     * according to the syntax of ACC's job details files.
     * @return the list of lines for a job details file
     */

    public ArrayList<String> toLinesJobDetails()
    {
      	ArrayList<String> toJD = new ArrayList<String>();
      	
      	if (this.getType().equals(NamedDataType.TEXTBLOCK))
		{
      		TextBlock lines = (TextBlock) this.getValue();
	        if (lines.size() > 1)
	        {
	        	toJD.add(ChemSoftConstants.JDLABDATA + getReference() 
	            		+ ChemSoftConstants.JDDATAVALSEPARATOR
	            		+ ChemSoftConstants.JDOPENBLOCK + lines.get(0));
	        	for (int i=1; i<lines.size(); i++)
	        	{
	        		toJD.add(lines.get(i));
	        	}
	            toJD.add(ChemSoftConstants.JDCLOSEBLOCK);
	        } 
	        else
	        {
	        	toJD.add(ChemSoftConstants.JDLABDATA + getReference() 
	            		+ ChemSoftConstants.JDDATAVALSEPARATOR + lines.get(0));
	        }
		} 
      	else 
      	{
			toJD.add(ChemSoftConstants.JDLABDATA + getReference() 
				+ ChemSoftConstants.JDDATAVALSEPARATOR 
				+ this.getValue());
		}
        
        return toJD;
    }

//-----------------------------------------------------------------------------

}
