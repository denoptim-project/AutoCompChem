package autocompchem.chemsoftware;

import java.lang.reflect.Type;
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
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import autocompchem.chemsoftware.gaussian.GaussianConstants;
import autocompchem.datacollections.NamedData;
import autocompchem.datacollections.Parameter;
import autocompchem.datacollections.ParameterStorage;
import autocompchem.run.ACCJob;
import autocompchem.run.EvaluationJob;
import autocompchem.run.Job;
import autocompchem.run.MonitoringJob;
import autocompchem.run.ShellJob;
import autocompchem.run.Terminator;
import autocompchem.text.TextBlock;

/**
 * This object represents a block of text-based data.
 *
 * @author Marco Foscato
 */

public class DirectiveData extends NamedData implements IDirectiveComponent
{
    /**
     * Parameters defining task embedded in this directive.
     */
    private ParameterStorage accTaskParams;

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
        
        extractTask(new ArrayList<String>(Arrays.asList(dataLines)));
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor from content
     * @param name the name of this block of data
     * @param lines the list of lines representing the contained data
     */

    public DirectiveData(String name, List<String> lines)
    {
    	super();
    	super.setReference(name);
    	super.setValue(new TextBlock(lines)); 
        extractTask(lines);       
    }
    
//-----------------------------------------------------------------------------
    
    private void extractTask(List<String> lines)
    {
	    if (hasACCTask())
	    {
			// WARNING! Here we assume that the entire content of the 
			// directive data, is about the ACC task. Thus, we add the 
			// multiline start/end labels so that the getACCTaskParams
			// method will keep read all the lines as one.
			if (lines.size()>1)
			{
				lines.set(0, ChemSoftConstants.JDOPENBLOCK + lines.get(0));
				lines.set(lines.size()-1, lines.get(lines.size()-1) 
						+ ChemSoftConstants.JDCLOSEBLOCK);
			}
			accTaskParams = Directive.getACCTaskParams(lines);
			accTaskParams.setParameter(new Parameter(ChemSoftConstants.JDACCTASK,
					accTaskParams.getParameterValue(
							ChemSoftConstants.JDLABACCTASK)));
			accTaskParams.removeData(ChemSoftConstants.JDLABACCTASK);
			//TODO-gg uncomment once handling of tasks is finished
			//super.removeValue();
	    }
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
    		if (super.getValue() instanceof TextBlock)
    		{
    			return (ArrayList<String>) 
        				((TextBlock) super.getValue());
    		} else if (super.getValue() instanceof ArrayList)
    		{
    			return (ArrayList<String>) super.getValue();
    		}
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
     * @return the parameters defining the ACC task embedded in this directive.
     */
	public ParameterStorage getTaskParams() 
	{
		return accTaskParams;
	}
	
//-----------------------------------------------------------------------------

    /**
     * Sets the parameters defining the ACC task embedded in this directive.
     * @param params
     */
	public void setTaskParams(ParameterStorage params) 
	{
		accTaskParams=params;
	}

//-----------------------------------------------------------------------------
    
    /**
     * Checks if there is any ACC task definition within this directive data.
     * @return <code>true</code> if there is at least one ACC task definition.
     */
    
	@SuppressWarnings("unchecked")
	public boolean hasACCTask() 
	{
    	if (accTaskParams!=null)
    		return true;
    	
		if (this.getType().equals(NamedDataType.TEXTBLOCK))
		{
			for (String l : (Iterable<String>) this.getValue())
			{
				if (l.contains(ChemSoftConstants.JDLABACCTASK)
						|| l.contains(GaussianConstants.LABPARAMS))
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
      		Object value = this.getValue();
      		TextBlock lines = null;
      		if (value instanceof TextBlock)
      		{
      			lines= (TextBlock) this.getValue();
      		} else if (value instanceof ArrayList){
      			lines= new TextBlock((ArrayList<String>) this.getValue());
      		} else {
      			throw new IllegalStateException("DirectiveData contains a value "
      					+ "with unexpected type: " + value.getClass());
      		}
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

    public static class DirectiveDataSerializer 
    implements JsonDeserializer<DirectiveData>
    {
        @Override
        public DirectiveData deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException
        {
        	 JsonObject jsonObject = json.getAsJsonObject();
             
             if (!jsonObject.has("type"))
             {
                 String msg = "Missing NamedDataType: found a "
                         + "JSON string that cannot be converted into a "
                         + "DirectiveData.";
                 throw new JsonParseException(msg);
             }       


             DirectiveData dd = new DirectiveData();
             String reference = context.deserialize(jsonObject.get("reference"),
            		 String.class);
             dd.setReference(reference);
             
             NamedDataType typ = context.deserialize(jsonObject.get("type"),
            		 NamedDataType.class);
             dd.setType(typ);
             
             if (jsonObject.has("value") && typ==NamedDataType.TEXTBLOCK)
             { 
            	 ArrayList<String> lines = context.deserialize(
 						jsonObject.get("value"),
 	                    new TypeToken<ArrayList<String>>(){}.getType());
            	 dd = new DirectiveData(reference, lines);
             }

             if (jsonObject.has("accTaskParams"))
             { 
            	 ParameterStorage ps = context.deserialize(
                		 jsonObject.get("accTaskParams"), 
                		 ParameterStorage.class);
				dd.accTaskParams = ps;
             }
         	
         	return dd;
        	
        }
    }
//-----------------------------------------------------------------------------

}
