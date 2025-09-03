package autocompchem.perception.infochannel;

import java.io.File;
import java.io.FileReader;

/*
 *   Copyright (C) 2018  Marco Foscato
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

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import autocompchem.datacollections.ParameterStorage;
import autocompchem.files.FileUtils;
import autocompchem.perception.circumstance.MatchText;
import autocompchem.run.Job;
import autocompchem.run.Terminator;
import autocompchem.run.jobediting.JobEditType;
import autocompchem.utils.StringUtils;
import autocompchem.wiro.chem.CompChemJob;

/**
 * File as a source of information
 *
 * @author Marco Foscato
 */

public class FileAsSource extends ReadableIC
{
    /**
     * Pathname
     */
    private String pathName = "";
    
    /**
     * JSON name of the pathname field
     */
    public static final String JSONPATHNAME = "pathName";

//------------------------------------------------------------------------------

    /**
     * Constructs an empty FileAsSource
     */

    public FileAsSource()
    {
        super();
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a FileAsSource and specify the pathname. Absolute or relative
     * pathnames should be expected. No assumption of pathname.
     */

    public FileAsSource(String pathName)
    {
        super();
        this.pathName = pathName;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructs a FileAsSource and specify the pathname. Absolute or relative
     * pathnames should be expected. No assumption of pathname.
     */

    public FileAsSource(String pathName, InfoChannelType ict)
    {
        super();
        this.pathName = pathName;
        setType(ict);
    }

//------------------------------------------------------------------------------

    /**
     * Returns the pathname of the file that is the source of information
     * @return the pathname
     */

    public String getPathName()
    {
        return pathName;
    }
    
//------------------------------------------------------------------------------

  	@Override
  	public boolean canBeRead() 
  	{
  		File f = new File(pathName);
  		return f.exists() && f.canRead();
  	}

//------------------------------------------------------------------------------

    /**
     * Returns the Reader of the source file.
     * The stream is typically closed outside of the information channel, by
     * whatever reads the Reader and defined that the Reader is no longer
     * needed.
     * @return a reader for reading the character-info from the source
     */

    public Reader getSourceReader()
    {
        try
        {
            super.reader = new FileReader(pathName);
        }
        catch (Exception e)
        {
            Terminator.withMsgAndStatus("ERROR! File "+ pathName
            		+ " not found!.",-1);
        }
        return super.reader;
    }
    
//------------------------------------------------------------------------------

    public static class FileAsSourceSerializer 
    implements JsonSerializer<FileAsSource>
    {
        @Override
        public JsonElement serialize(FileAsSource fas, Type typeOfSrc,
              JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty(JSONICIMPLEMENTATION, 
            		InfoChannelImplementation.valueOf(
            				fas.getClass().getSimpleName().toUpperCase())
            		.toString());
            jsonObject.addProperty(JSONINFOCHANNELTYPE, 
            		fas.getType().toString());
            
            if (!fas.pathName.isEmpty())
            	jsonObject.add("pathName", context.serialize(fas.pathName));

            return jsonObject;
        }
    }
    
//------------------------------------------------------------------------------

    public static class FileAsSourceDeserializer 
    implements JsonDeserializer<FileAsSource>
    {
        @Override
        public FileAsSource deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonObject = json.getAsJsonObject();
            
            if (!jsonObject.has(JSONICIMPLEMENTATION))
            {
                String msg = "Missing '" + JSONICIMPLEMENTATION + "': found a "
                        + "JSON string that cannot be converted into any "
                        + "InfoChannel subclass.";
                throw new JsonParseException(msg);
            }   

            InfoChannelImplementation impl = context.deserialize(
            		jsonObject.get(JSONICIMPLEMENTATION),
            		InfoChannelImplementation.class);
            if (this.getClass().getSimpleName().toUpperCase().equals(
            		impl.toString()))
            {
            	String msg = "Cannot to deserialize '" + impl + "' into "
            			+ this.getClass().getSimpleName() + ".";
                throw new JsonParseException(msg);
            }

            String pathName = context.deserialize(
            		jsonObject.get(JSONPATHNAME), String.class);

            InfoChannelType type = context.deserialize(
            		jsonObject.get(JSONINFOCHANNELTYPE), InfoChannelType.class);
            
            FileAsSource fas = new FileAsSource(pathName, type);
        	
        	return fas;
        }
    }
            
//------------------------------------------------------------------------------

    /**
     * Return a human readable description
     * @return a string
     */

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("FileAsSource [ICType:").append(super.getType());
        sb.append("; pathName:").append(pathName);
        sb.append("]");
        return sb.toString();
    }
    
//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (o == null)
            return false;
        
        if (o == this)
            return true;
        
        if (o.getClass() != getClass())
            return false;
         
        FileAsSource other = (FileAsSource) o;
         
        if (!this.pathName.equals(other.pathName))
            return false;
        
        return super.equals(other);
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(pathName, super.hashCode());
    }

//-----------------------------------------------------------------------------
      
	@Override
	public List<InfoChannel> getSpecific(Path wdir)
	{
		List<InfoChannel>  results = new ArrayList<InfoChannel>();
		// NB: Any REGEX for a file in the pwd must allow for the dirname part, 
		// hence, we add the ".*" if not there.
		String regex = pathName;
		if (!pathName.startsWith(".*"))
		{
			regex = ".*" + pathName; 
		}
		if (StringUtils.isValidRegex(regex))
		{
			List<File> files = FileUtils.findByREGEX(wdir.toFile(), regex, 1, false);
			for (File file : files)
			{
				FileAsSource specIC = new FileAsSource(file.getAbsolutePath(), 
						getType());
				results.add(specIC);
			}
		} else {
			results.add(this);
		}
		return results;
	}

//------------------------------------------------------------------------------

}
