package autocompchem.perception.infochannel;


import java.lang.reflect.Type;
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

import autocompchem.run.Job;
import autocompchem.wiro.chem.CompChemJob;


/**
 * Class projecting the details of a {@link CompChemJob} into an information
 * channel.
 *
 * @author Marco Foscato
 */

public class JobDetailsAsSource extends InfoChannel
{
    /**
     * Text organized by lines
     */
    public final CompChemJob job;

//------------------------------------------------------------------------------

    /**
     * Constructs an empty ShortTextAsSource
     */

    public JobDetailsAsSource(CompChemJob job)
    {
        super();
        this.job = job;
    }
    
//------------------------------------------------------------------------------

    public static class JobDetailsAsSourceSerializer 
    implements JsonSerializer<JobDetailsAsSource>
    {
        @Override
        public JsonElement serialize(JobDetailsAsSource jdas, Type typeOfSrc,
              JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty(JSONICIMPLEMENTATION, 
            		InfoChannelImplementation.valueOf(
            				jdas.getClass().getSimpleName().toUpperCase())
            		.toString());
            jsonObject.addProperty(JSONINFOCHANNELTYPE, 
            		jdas.getType().toString());
            
            jsonObject.add("job", context.serialize(jdas.job));

            return jsonObject;
        }
    }
    
//------------------------------------------------------------------------------

    public static class JobDetailsAsSourceDeserializer 
    implements JsonDeserializer<JobDetailsAsSource>
    {
        @Override
        public JobDetailsAsSource deserialize(JsonElement json, Type typeOfT,
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

			CompChemJob job = (CompChemJob) context.deserialize(
					jsonObject.get("job"), Job.class);

            InfoChannelType type = context.deserialize(
            		jsonObject.get(JSONINFOCHANNELTYPE), InfoChannelType.class);
            
            JobDetailsAsSource jdas = new JobDetailsAsSource(job);
            jdas.setType(type);
        	
        	return jdas;
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
        sb.append("JobDetailsAsSource [ICType:").append(super.getType());
        sb.append(", job:").append(job.getId());
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
         
      JobDetailsAsSource other = (JobDetailsAsSource) o;
         
      if (!this.job.equals(other.job))
          return false;
        
      return super.equals(other);
  }
    
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(job, super.hashCode());
    }
    
//------------------------------------------------------------------------------

}
