package autocompchem.run.jobediting;

import java.lang.reflect.Type;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * A rule defining what to do with data saved on files. Typically, we
 * want to archive some data (i.e., keep a copy of the files just to avoid 
 * overwriting the files), copy (i.e., keep a snapshot of previous data),
 * rename a copy,
 * or delete previous data. This class only collects the information about what to do,
 * it does not perform the action. The implementation if in {@link ActionApplier}.
 */
public class DataArchivingRule 
{
	/**
	 * Placeholder for sequential index in the pattern (e.g., "*_IDX.dat"
	 * will results in patterns like "*_0.dat", "*_1.dat", etc.)
	 */
	public static final String INDEX_PLHLD = "IDX";

	/*
	 * Placeholder for the base name of the file.
	 */
	public static final String BASENAME_PLHLD = "BASENAME";

	/**
	 * Defines if a data archiving rule is meant for moving, copying or removing
	 * data.
	 */
	public static enum ArchivingTaskType {MOVE, COPY, DELETE, 
		RENAME_COPY_BASENAME_IDX,
		RENAME_COPY_LAST_SEQUENTIAL};

    //--------------------------------------------------------------------------
  	
  	public static class ArchivingTaskTypeDeserializer 
  	implements JsonDeserializer<ArchivingTaskType>
  	{
		@Override
		public ArchivingTaskType deserialize(JsonElement json, 
				Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException 
		{
			// JSON is case sensitive, but we want to
	    	// allow some flexibility on the case of the strings meant to represent
	    	// enums, so we allow case-insensitive string-like enums.
			return ArchivingTaskType.valueOf(json.getAsString().toUpperCase());
		}
  	}
  	
    //--------------------------------------------------------------------------
  	
	/**
	 * The {@link ArchivingTaskType} of this rule.
	 */
	private ArchivingTaskType type;
	
	/**
	 * The pattern for identifying filenames (not pathnames!) that contain data
	 * subject to this data archiving rule.  The pattern
	 * can have the form <code>*string</code>, <code>string*</code>, or 
	 * <code>*string*</code> depending on whether the string is expected to be 
	 * at the end, the beginning, or in the middle of the the last component of 
	 * the pathname.
	 */
	private String pattern;

	/**
	 * The new name for the file.
	 */
	private String newName;
	
//------------------------------------------------------------------------------
	
	/**
	 * Construct a rule that defines what to do and applies to any data found 
	 * in files with names matching the given pattern.
	 * @param type defines what tape of action we are expected to do upon 
	 * request to archive data matched by this rule.
	 * @param pattern string identifying filenames (not pathnames!) that contain
	 * data subject to this data archiving rule.  The pattern
	 * can have the form <code>*string</code>, <code>string*</code>, or 
	 * <code>*string*</code> depending on whether the string is expected to be 
	 * at the end, the beginning, or in the middle of the the last component of 
	 * the pathname.
	 */
	public DataArchivingRule(ArchivingTaskType type, String pattern)
	{
		this(type, pattern, null);
		if (type == ArchivingTaskType.RENAME_COPY_LAST_SEQUENTIAL
			|| type == ArchivingTaskType.RENAME_COPY_BASENAME_IDX
		)
			throw new IllegalArgumentException("New name is required for " 
			    + ArchivingTaskType.RENAME_COPY_LAST_SEQUENTIAL + ", "
				+ ArchivingTaskType.RENAME_COPY_BASENAME_IDX + " types.");
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Construct a rule that defines what to do and applies to any data found 
	 * in files with names matching the given pattern.
	 * @param type defines what tape of action we are expected to do upon 
	 * request to archive data matched by this rule.
	 * @param pattern string identifying filenames (not pathnames!) that contain
	 * data subject to this data archiving rule.  The pattern
	 * can have the form <code>*string</code>, <code>string*</code>, or 
	 * <code>*string*</code> depending on whether the string is expected to be 
	 * at the end, the beginning, or in the middle of the the last component of 
	 * the pathname.
	 * @param newName the new name for the file. Used only if the type is 
	 * {@link ArchivingTaskType#RENAME_COPY_LAST_SEQUENTIAL} or
	 * {@link ArchivingTaskType#RENAME_COPY_BASENAME_IDX}
	 */
	public DataArchivingRule(ArchivingTaskType type, String pattern, String newName)
	{
		this.type = type;
		this.pattern = pattern;
		if (type == ArchivingTaskType.RENAME_COPY_LAST_SEQUENTIAL
			 && !pattern.contains(INDEX_PLHLD))
		{
			throw new IllegalArgumentException("Pattern '" + pattern 
			    + "' does not contain '" 
			    + INDEX_PLHLD + "' as placeholder for the sequential index. "
			    + "Please, include '" + INDEX_PLHLD 
				+ "' as placeholder in the pattern.");
		}
		if (type == ArchivingTaskType.RENAME_COPY_BASENAME_IDX
			&& (!pattern.contains(BASENAME_PLHLD) || !pattern.contains(INDEX_PLHLD)))
		{
			throw new IllegalArgumentException("Pattern '" + pattern 
			    + "' does not contain '" + BASENAME_PLHLD + "' as placeholder for the base name and '" 
			    + INDEX_PLHLD + "' as placeholder for the sequential index. "
			    + "Please, include '" + BASENAME_PLHLD + "' and '" 
				+ INDEX_PLHLD + "' as placeholders in the pattern.");
		}
		this.newName = newName;
	}

//------------------------------------------------------------------------------

	/**
	 * @return the type of data archiving task.
	 */
	public ArchivingTaskType getType() {
		return type;
	}

//------------------------------------------------------------------------------

	/**
	 * @return the pattern for matching filenames.
	 */
	public String getPattern() {
		return pattern;
	}

//------------------------------------------------------------------------------

	/**
	 * @return the new name for the file.
	 */
	public String getNewName() {
		return newName;
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
 	    
 	   DataArchivingRule other = (DataArchivingRule) o;
 	    
 	    if (!this.type.equals(other.type))
 	    	return false;
 	    
 	    if (!this.pattern.equals(other.pattern))
 	    	return false;
 	    
 	    if (this.type == ArchivingTaskType.RENAME_COPY_LAST_SEQUENTIAL
			|| this.type == ArchivingTaskType.RENAME_COPY_BASENAME_IDX)
 	    	return this.newName.equals(other.newName);
 	    
 	    return true;
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(type, pattern, newName);
    }
  
//------------------------------------------------------------------------------

  	/**
  	 * @return a string representation of this object
  	 */
    @Override
  	public String toString() 
	{
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName()).append(" [");
		sb.append(type.toString()).append(" ").append(pattern);
		if (type == ArchivingTaskType.RENAME_COPY_LAST_SEQUENTIAL 
			|| type == ArchivingTaskType.RENAME_COPY_BASENAME_IDX)
		{
			sb.append(" to ").append(newName);
		}
		sb.append("]");
		return sb.toString();
  	}

//------------------------------------------------------------------------------
	
}
