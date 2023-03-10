package autocompchem.run.jobediting;

/**
 * A rule defining what to do with data saved on files. Typically, we
 * want to archive some data (i.e., keep a copy of the files just to avoid 
 * overwriting the files), copy (i.e., keep a snapshot of previous data),
 * or delete previous data.
 */
public class DataArchivingRule 
{
	/**
	 * Defines if a data archiving rule is meant for moving, copying or removing
	 * data.
	 */
	public static enum Type {MOVE, COPY, DELETE};
	
	/**
	 * The {@link Type} of this rule.
	 */
	private Type type;
	
	/**
	 * The pattern for identifying filenames (not pathnames!) that contain data
	 * subject to this data archiving rule.  The pattern
	 * can have the form <code>*string</code>, <code>string*</code>, or 
	 * <code>*string*</code> depending on whether the string is expected to be 
	 * at the end, the beginning, or in the middle of the the last component of 
	 * the pathname.
	 */
	private String pattern;
	
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
	public DataArchivingRule(Type type, String pattern)
	{
		this.type = type;
		this.pattern = pattern;
	}
	
//------------------------------------------------------------------------------

	/**
	 * Construct a rule that asks to move any data found 
	 * in files with names matching the given pattern into an archive folder.
	 * @param pattern string identifying filenames (not pathnames!) that contain
	 * data subject to this data archiving rule.  The pattern
	 * can have the form <code>*string</code>, <code>string*</code>, or 
	 * <code>*string*</code> depending on whether the string is expected to be 
	 * at the end, the beginning, or in the middle of the the last component of 
	 * the pathname.
	 */
	public static DataArchivingRule makeMoveRule(String pattern)
	{
		return new DataArchivingRule(Type.MOVE, pattern);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Construct a rule that asks to copy any data found 
	 * in files with names matching the given pattern into an archive folder
	 * while keeping a copy in the original location.
	 * @param pattern string identifying filenames (not pathnames!) that contain
	 * data subject to this data archiving rule.  The pattern
	 * can have the form <code>*string</code>, <code>string*</code>, or 
	 * <code>*string*</code> depending on whether the string is expected to be 
	 * at the end, the beginning, or in the middle of the the last component of 
	 * the pathname.
	 */
	public static DataArchivingRule makeCopyRule(String pattern)
	{
		return new DataArchivingRule(Type.COPY, pattern);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Construct a rule that asks to remove any data found 
	 * in files with names matching the given pattern.
	 * @param pattern string identifying filenames (not pathnames!) that contain
	 * data subject to this data archiving rule. The pattern
	 * can have the form <code>*string</code>, <code>string*</code>, or 
	 * <code>*string*</code> depending on whether the string is expected to be 
	 * at the end, the beginning, or in the middle of the the last component of 
	 * the pathname.
	 */
	public static DataArchivingRule makeDeleteRule(String pattern)
	{
		return new DataArchivingRule(Type.DELETE, pattern);
	}

//------------------------------------------------------------------------------

	/**
	 * @return the type of data archiving task.
	 */
	public Type getType() {
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
 	    
 	    return this.pattern.equals(other.pattern);
    }
  
//------------------------------------------------------------------------------

  	/**
  	 * @return a string representation of this object
  	 */
    @Override
  	public String toString() {
  		return type.toString() + " " + pattern;
  	}

//------------------------------------------------------------------------------
	
}
