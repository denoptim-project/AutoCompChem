package autocompchem.wiro.chem;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Class representing the location of a component in a directive structure, as
 * a path from the outermost directive (first item) to its parent container 
 * (last item).
 * An empty list means "." or "here" and it reflect the outermost directive, 
 * which is reachable directly from the {@link CompChemJob#getDirective(String)}
 * method.
 * 
 * A wild-card string for component names exists and is in field 
 * {@link #ANYNAME} (value {@value #ANYNAME}). 
 * 
 * A wild-card string for component type exists and is 
 * {@link DirectiveComponentType#ANY}.
 */
public class DirComponentAddress implements Iterable<DirComponentTypeAndName>,
	Cloneable
{
	private List<DirComponentTypeAndName> path = 
			new ArrayList<DirComponentTypeAndName> ();
	
	private static final String JSONFIELD = "Path";
	private static final String TYPENAMESEPARATOR = ":";
	private static final String PLACESEPARATOR = "|";
	
	/**
	 * The wild-card for component name.
	 */
	public static final String ANYNAME = "<*acc_anyname*>";
	
//------------------------------------------------------------------------------

	/**
	 * Appends a location to the end of this path (i.e., in the innermost level)
	 * @param location the definition of the location to add to this path.
	 */
	public void addStep(DirComponentTypeAndName location)
	{
		path.add(location);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Appends a location to the end of this path (i.e., in the innermost level)
	 * @param name the name of the component representing the location to add 
	 * to this path.
	 * @param type the type of the component representing the location to add 
	 * to this path.
	 */
	public void addStep(String name, DirectiveComponentType type)
	{
		addStep(new DirComponentTypeAndName(name, type));
	}
	
	
//------------------------------------------------------------------------------

	/**
	 * Appends a location to the end of this path (i.e., in the innermost level)
	 * @param name the name of the component representing the location to add 
	 * to this path.
	 * @param typeInShort the short form of type specification for the component
	 * representing the location to add to this path. 
	 * See {@link DirectiveComponentType#getShortForms()} for possible values.
	 */
	public void addStep(String name, String typeInShort)
	{
		DirectiveComponentType typ = DirectiveComponentType.getEnum(typeInShort);
		if (typ==null)
		{
			throw new IllegalArgumentException("String '" + typeInShort + "' "
					+ "cannot be converted to " 
					+ DirectiveComponentType.class.getName());
		}
		addStep(name, typ);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Returns the number of locations specified by this path.
	 * @return the size of the path.
	 */
	public int size()
	{
		return path.size();
	}
	
//------------------------------------------------------------------------------

	/**
	 * Returns the locations sat the given index 
	 * @return the size of the path.
	 * @throws {@link IndexOutOfBoundsException} if the index is out of range 
	 * (index < 0 || index >= size())
	 */
	public DirComponentTypeAndName get(int i)
	{
		return path.get(i);
	}
		
//------------------------------------------------------------------------------

	/**
	 * Get the last component in the path, i.e., the innermost component, i.e.,
	 * the right-most component. If the address is empty, no parent exist, thus
	 * this method returns <code>null</code>.
	 * @return the last component or null
	 */
	public DirComponentTypeAndName getLast()
	{
		if (path.size()==0)
			return null;
		return path.get(path.size()-1);
	}
  
//------------------------------------------------------------------------------

	/**
	 * Defines the address to the component that contains the last component in
	 * this address.
	 * @return the address to the parent component.
	 */
	public DirComponentAddress getParent() 
	{
		DirComponentAddress parentAddress = new DirComponentAddress();
		if (path.size()>1)
		{
			path.subList(0, path.size()-1).stream()
				.forEach(l -> parentAddress.addStep(l));
		}
		return parentAddress;
	}
  
//------------------------------------------------------------------------------
	
	/**
	 * Gets the iterator over locations which start from the outermost and ends
	 * with the innermost.
	 * @return the iterator over locations.
	 */
	public Iterator<DirComponentTypeAndName> iterator()
	{
		return path.iterator();
	}
	
//------------------------------------------------------------------------------

	/**
	 * Returns a customized string meant to be short and human readable, while 
	 * easy to parse and compatible with JSON serialization.
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<path.size(); i++)
		{
			DirComponentTypeAndName place = path.get(i);
			sb.append(place.type.shortString).append(TYPENAMESEPARATOR)
				.append(place.name);
			if (i<(path.size()-1))
				sb.append(PLACESEPARATOR);
		}
		return sb.toString();
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * Parses the string obtained by {@link #toString()} method into an 
	 * instance.
	 * @return the instance.
	 */
	public static DirComponentAddress fromString(String path)
	{
		DirComponentAddress address = new DirComponentAddress();
		if (path.equals("."))
		{
			return address;
		} else if (path.trim().equals(""))
		{
			return address;
		}
        String[] places = path.trim().split("\\"+PLACESEPARATOR);
        for (int i=0; i<places.length; i++)
        {
        	String[] parts = places[i].trim().split("\\"+TYPENAMESEPARATOR);
        	if (ANYNAME.equals(parts[1]))
        	{
            	address.addStep(new DirComponentTypeAndName(ANYNAME, 
            			DirectiveComponentType.getEnum(parts[0])));
        	} else {
	        	address.addStep(new DirComponentTypeAndName(parts[1], 
	        			DirectiveComponentType.getEnum(parts[0])));
        	}
        }
        return address;
	}
	
//------------------------------------------------------------------------------

    @Override	
    public DirComponentAddress clone()
    {
    	DirComponentAddress clone = new DirComponentAddress();
    	Iterator<DirComponentTypeAndName> iter = this.iterator();
    	while (iter.hasNext())
    	{
    		DirComponentTypeAndName step = iter.next();
    		clone.addStep(step.name, step.type);
    	}
    	return clone;
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
 	    
 	    DirComponentAddress other = (DirComponentAddress) o;
 	   
 	    if (this.path.size()!=other.path.size())
 	    	return false;
 	    
 	    for (int i=0; i<this.path.size(); i++)
 	    {
 	    	if (!this.path.get(i).equals(other.path.get(i)))
 	    		return false;
 	    }
 	    
 	    return true;
    }

//------------------------------------------------------------------------------

    public static class DirComponentAddressSerializer 
    implements JsonSerializer<DirComponentAddress>
    {
        @Override
        public JsonElement serialize(DirComponentAddress address, Type typeOfSrc,
              JsonSerializationContext context)
        {
            JsonPrimitive s = new JsonPrimitive(address.toString());
            return s;
        }
    }
    
//------------------------------------------------------------------------------

    public static class DirComponentAddressDeserializer 
    implements JsonDeserializer<DirComponentAddress>
    {
        @Override
        public DirComponentAddress deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException
        {   
            String path = context.deserialize(json, String.class);
            return fromString(path);
        }
    }
  
//------------------------------------------------------------------------------
	
}
