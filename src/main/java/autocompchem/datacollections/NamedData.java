package autocompchem.datacollections;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import autocompchem.modeling.atomtuple.AnnotatedAtomTupleList;
import autocompchem.modeling.basisset.BasisSet;
import autocompchem.modeling.constraints.ConstraintsSet;
import autocompchem.molecule.conformation.ConformationalSpace;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.vibrations.NormalMode;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.run.Action;
import autocompchem.text.TextBlock;
import autocompchem.utils.StringUtils;

/*
 *   Copyright (C) 2014  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * General purpose data structure that can be named with a string.
 * The data is defined by its reference name,
 * its value type, and its actual value (i.e., the data itself).
 * 
 * @author Marco Foscato
 */

public class NamedData implements Cloneable
{
    /**
     * A string used to identify the data.
     */
    private String reference;

    /**
     * The actual data.
     */
    private Object value;
      
    /**
     * The kind of data structure
     */
    private NamedDataType type;

    /**
     * Allowed kinds of data values
     */
    public enum NamedDataType {
    	UNDEFINED,
        STRING,
        TEXTBLOCK,
        DOUBLE,
        INTEGER,
        BOOLEAN,
        IATOMCONTAINER,
        ATOMCONTAINERSET,
        SITUATION,
        SITUATIONBASE,
        INFOCHANNELBASE,
        JOB,
        FILE,
        BASISSET, 
        ZMATRIX, 
        LISTOFDOUBLES, 
        LISTOFINTEGERS,
        NORMALMODE,
        NORMALMODESET, 
        ACTION,
        PARAMETERSTORAGE, 
        CONSTRAINTSSET,
        ANNOTATEDATOMTUPLELIST,
        CONFORMATIONALSPACE};
        
    /**
     * List of types that can be serailized to JSON
     */
    public static final Set<NamedDataType> jsonable = new HashSet<NamedDataType>(
            Arrays.asList(NamedDataType.STRING,
            		NamedDataType.INTEGER,
            		NamedDataType.DOUBLE,
            		NamedDataType.BOOLEAN,
            		NamedDataType.TEXTBLOCK,
            		NamedDataType.PARAMETERSTORAGE,
            		NamedDataType.BASISSET,
            		NamedDataType.IATOMCONTAINER,
            		NamedDataType.CONSTRAINTSSET,
            		NamedDataType.ZMATRIX,
            		NamedDataType.ANNOTATEDATOMTUPLELIST,
            		NamedDataType.CONFORMATIONALSPACE));
    // NB: when extending the above list, remember to add the corresponding case
    // in the NamedDataDeserializer!
    
    /**
     * String use to not that a type could not be serialized to JSON
     */
    public static final String NONJSONABLE = "Type is not JSON-able";

//------------------------------------------------------------------------------

    /**
     * Constructor of an empty (un)named data.
     */

    public NamedData()
    {}
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a named data with a given content.
     * @param reference the name of the data.
     * @param type the type of object.
     * @param value the actual data.
     */

    public NamedData(String reference, Object value)
    {
        this.reference = reference;
        this.setValue(value);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a named data with a given content.
     * @param reference the name of the data.
     * @param type the type of object.
     * @param value the actual data.
     */

    public NamedData(String reference, NamedDataType type, Object value)
    {
        this.reference = reference;
        this.type = type; 
        this.value = value;
    }

//------------------------------------------------------------------------------

    /**
     * Return the reference name of this data.
     * @return the reference name.
     */

    public String getReference()
    {
        return reference;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the reference name of this data
     * @param reference the new name
     */
    
    public void setReference(String reference)
    {
    	this.reference = reference;
    }
    
//------------------------------------------------------------------------------

    /**
     * Set the value of this data.
     * @param value the value to be set to this data.
     */

    public void setValue(Object value)
    {
    	this.type = detectType(value);
        this.value = value;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Erases the value and type of this NamedData.
     */
    public void removeValue()
    {
    	value = null;
    	type = NamedDataType.UNDEFINED;
    }

//------------------------------------------------------------------------------

    /**
     * Return the value of this data
     * @return the value of this data
     */

    public Object getValue()
    {
        return value;
    }

//------------------------------------------------------------------------------

    /**
     * Return the string representation of the value of this data.
     * Corresponds to getValue().toString() or to merging any list of items
     * using the space as separator.
     * @return the value of this data.
     */

    public String getValueAsString()
    {
    	if (value==null)
    		return "null";
    	if (type == NamedDataType.LISTOFDOUBLES 
    			|| type == NamedDataType.LISTOFINTEGERS
    			|| type == NamedDataType.TEXTBLOCK)
    		return StringUtils.mergeListToString(getValueAsLines(), " ", true);
    	
        return value.toString();
    }
    
//------------------------------------------------------------------------------

    /**
     * Return the representation of the value of this data as a text block.
     * @return the value of this data or null it the value cannot be converted 
     * to lines of text.
     */

    @SuppressWarnings("unchecked")
	public List<String> getValueAsLines()
    {
    	if (value==null)
    		return null;

    	List<String> lines = new ArrayList<String>();
    	switch (type)
    	{
		case LISTOFDOUBLES:
			for (Double d : (ListOfDoubles) value)
				lines.add(d.toString());
			break;
		case LISTOFINTEGERS:
			for (Integer d : (ListOfIntegers) value)
				lines.add(d.toString());
			break;
		case STRING:
			lines.addAll(Arrays.asList(((String)value).split(
					System.getProperty("line.separator"))));
			break;
		case TEXTBLOCK:
			for (String l : (ArrayList<String>) value)
				lines.add(l);
			break;
		default:
			return null;
    	}
        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Return the type of this data.
     * @return the type of this data.
     */

    public NamedDataType getType()
    {
        return type;
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the type of this data.
     * @param type the type to specify.
     */

    public void setType(NamedDataType type)
    {
        this.type = type;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Looks at the object and find out what class it is an instance of.
     * @param o the object to evaluate
     * @return the class
     */
    
    private static NamedDataType detectType(Object o)
    {
    	NamedDataType tp = NamedDataType.UNDEFINED;
    	if (o==null)
    		return tp;
    	
       	String className = o.getClass().getName();
    	className = className.substring(className.lastIndexOf(".")+1);

    	switch (className)
    	{
    		case ("Boolean"):
    			tp = NamedDataType.BOOLEAN;
    			break;

    		case ("Double"):
    			tp = NamedDataType.DOUBLE;
    			break;

    		case ("File"):
    			tp = NamedDataType.FILE;
    			break;

    		case ("Molecule"):
    			tp = NamedDataType.IATOMCONTAINER;
    			break;
    			
    		case ("AtomContainer"):
    			tp = NamedDataType.IATOMCONTAINER;
    			break;
    			
    		case ("AtomContainerSet"):
    			tp = NamedDataType.ATOMCONTAINERSET;
    			break;
    			
    		case ("AtomContainer2"):
    			tp = NamedDataType.IATOMCONTAINER;
    			break;

    		case ("Integer"):
    			tp = NamedDataType.INTEGER;
    			break;

    		case ("ArrayList"):
    			// NB: this is meant. I cannot see how to detect the type of
    			// elements, so we take them as strings.
    			tp = NamedDataType.TEXTBLOCK;
    			break;

    		case ("ListOfDoubles"):
    			tp = NamedDataType.LISTOFDOUBLES;
    			break;
    			
    		case ("ListOfIntegers"):
    			tp = NamedDataType.LISTOFINTEGERS;
    			break;
    		
    		case ("TextBlock"):
    			tp = NamedDataType.TEXTBLOCK;
    			break;

    		case ("Situation"):
    			tp = NamedDataType.SITUATION;
    			break;

    		case ("String"):
    			tp = NamedDataType.STRING;
    			break;
    			
    		case ("BasisSet"):
    			tp = NamedDataType.BASISSET;
    			break;
    			
    		case ("ZMatrix"):
    			tp = NamedDataType.ZMATRIX;
    			break;
    			
    		case ("NormalMode"):
    			tp = NamedDataType.NORMALMODE;
    			break;
    			
    		case ("NormalModeSet"):
    			tp = NamedDataType.NORMALMODESET;
    			break;
    			
    		case ("Action"):
    			tp = NamedDataType.ACTION;
    			break;
    		
    		case ("ParameterStorage"):
    			tp = NamedDataType.PARAMETERSTORAGE;
    			break;
    			
    		case ("ConstraintsSet"):
    			tp = NamedDataType.CONSTRAINTSSET;
    			break;
    			
    		case ("AnnotatedAtomTupleList"):
    			tp = NamedDataType.ANNOTATEDATOMTUPLELIST;
    			break;
    			
    		case ("ConformationalSpace"):
    			tp = NamedDataType.CONFORMATIONALSPACE;
    			break;
    		
    		default:
    			tp = NamedDataType.UNDEFINED;
    			break;
    	}
    	return tp;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return a deep copy of this data
     * @throws CloneNotSupportedException 
     */
    
    @Override
    public NamedData clone() throws CloneNotSupportedException
    {
    	Object cVal = null;

    	if (value!=null)
    	{
	        switch (type) 
	        {
	        case DOUBLE:
	            cVal = Double.parseDouble(value.toString());
	            break;
	
	        case INTEGER:
	            cVal = Integer.parseInt(value.toString());
	            break;
	
	        case STRING:
	            cVal = value.toString();
	            break;
	
	        case TEXTBLOCK:
	        {
	            @SuppressWarnings("unchecked") ArrayList<String> lines =
	            (ArrayList<String>) value;
	            cVal = new TextBlock(lines);
	            break;
	        }
	        
	        case LISTOFDOUBLES:
	        {
	            @SuppressWarnings("unchecked") ArrayList<Double> doubles =
	            (ArrayList<Double>) value;
	            ListOfDoubles l = new ListOfDoubles();
	            for (Double d : doubles)
	            {
	            	l.add(d.doubleValue());
	            }
	            cVal = l;
	            break;
	        }
	
	        case LISTOFINTEGERS:
	        {
	            @SuppressWarnings("unchecked") ArrayList<Integer> ints =
	            (ArrayList<Integer>) value;
	            ListOfIntegers l = new ListOfIntegers();
	            for (Integer i : ints)
	            {
	            	l.add(i.intValue());
	            }
	            cVal = l;
	            break;
	        }
	
	        case BOOLEAN:
	            cVal = Boolean.parseBoolean(value.toString());
	            break;
	
	        case IATOMCONTAINER:
	            cVal = ((IAtomContainer) value).clone();
	            break;
	
	        case ATOMCONTAINERSET:
	        	AtomContainerSet cSet = new AtomContainerSet();
	        	for (IAtomContainer iac : ((AtomContainerSet) value).atomContainers())
	        	{
	        		cSet.addAtomContainer(iac.clone());
	        	}
	            cVal = cSet;
	            break;
	
	        case FILE:
	            cVal = new File(((File) value).getAbsolutePath());
	            break;
	
	        case ZMATRIX:
	        	cVal = ((ZMatrix) value).clone();
	            break;
	                        
	        case NORMALMODE:
	        	cVal = ((NormalMode) value).clone();
	        	break;
	
	        case NORMALMODESET:
	        	cVal = ((NormalModeSet) value).clone();
	        	break;
	        	
	        case ACTION:
	        	cVal = ((Action) value).clone();
	        	break;
	        	
	        case PARAMETERSTORAGE:
	            cVal = ((ParameterStorage) value).clone();
	            break;
	            
	        case CONSTRAINTSSET:
	        	cVal = ((ConstraintsSet) value).clone();
	        	break;
	        	
	        case ANNOTATEDATOMTUPLELIST:
	        	cVal = ((AnnotatedAtomTupleList) value).clone();

	        case CONFORMATIONALSPACE:
	        	cVal = ((ConformationalSpace) value).clone();
	        	
	        default:
	        	cVal = value.toString();
	            break;
	        }
    	}
        
    	NamedData nd = new NamedData(this.getReference(), type, cVal);
    	return nd;
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
 	   
 	    NamedData other = (NamedData) o;
 	   
 	    return this.reference.equals(other.reference)
 	    		&& this.type == other.type
 	    		&& this.value.equals(other.value);
    }
    
//------------------------------------------------------------------------------

    /**
     * String representation.
     * @return the string representation.
     */

    public String toString()
    {
        String str = reference + ParameterConstants.SEPARATOR + value;
        return str;
    }

//------------------------------------------------------------------------------

    public static class NamedDataSerializer implements JsonSerializer<NamedData>
    {
		@Override
		public JsonElement serialize(NamedData src, Type typeOfSrc, 
				JsonSerializationContext context) 
		{
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("reference", src.reference);
            if (src.type != NamedDataType.STRING)
            {
                jsonObject.addProperty("type", src.type.toString());	
            }
			if (!jsonable.contains(src.getType()))
			{
	            jsonObject.addProperty("value", NONJSONABLE);
			} else {
				jsonObject.add("value", context.serialize(src.value));
			}
            return jsonObject;
		}
    }
    
//-----------------------------------------------------------------------------

    public static class NamedDataDeserializer 
      implements JsonDeserializer<NamedData>
    {
		@Override
		public NamedData deserialize(JsonElement json, Type typeOfT, 
				JsonDeserializationContext context)
				throws JsonParseException 
		{
			JsonObject jo = json.getAsJsonObject();
			Object joValue = null;
			NamedDataType joType = NamedDataType.STRING;
			if (jo.has("type"))
			{
				joType = NamedDataType.valueOf(jo.get("type").getAsString());
			}
			JsonElement je = jo.get("value");
			
			if (!jsonable.contains(joType))
			{
				return new NamedData(jo.get("reference").getAsString(),
						joType, joValue);
			}
			
			//NB: this list of cases must reflect the content of "jsonable"
			switch (joType)
			{
			case BOOLEAN:
				joValue = context.deserialize(je, Boolean.class);
				break;
			case DOUBLE:
				joValue = context.deserialize(je, Double.class);
				break;
			case INTEGER:
				joValue = context.deserialize(je, Integer.class);
				break;
			case STRING:
				joValue = context.deserialize(je, String.class);
				break;
			case TEXTBLOCK:
				joValue = new TextBlock(context.deserialize(je,
						new TypeToken<ArrayList<String>>(){}.getType()));
				break;
			case PARAMETERSTORAGE:
				joValue = context.deserialize(je, ParameterStorage.class);
				break;
			case BASISSET:
				joValue = context.deserialize(je, BasisSet.class);
				break;
			case IATOMCONTAINER:
				joValue = context.deserialize(je, IAtomContainer.class);
				break;
			case CONSTRAINTSSET:
				joValue = context.deserialize(je, ConstraintsSet.class);
				break;
			case ZMATRIX:
				joValue = context.deserialize(je, ZMatrix.class);
				break;
			case ANNOTATEDATOMTUPLELIST:
				joValue = context.deserialize(je, AnnotatedAtomTupleList.class);
				break;
			case CONFORMATIONALSPACE:
				joValue = context.deserialize(je, ConformationalSpace.class);
			default:
				break;
			}
			
			return new NamedData(jo.get("reference").getAsString(),
					joType, joValue);
		}
    }
    
//-----------------------------------------------------------------------------
}

