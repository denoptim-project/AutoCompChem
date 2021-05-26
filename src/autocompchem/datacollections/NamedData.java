package autocompchem.datacollections;

import java.io.File;
import java.util.ArrayList;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.AtomContainer;

import autocompchem.molecule.MolecularUtils;
import autocompchem.molecule.intcoords.zmatrix.ZMatrix;
import autocompchem.molecule.vibrations.NormalMode;
import autocompchem.molecule.vibrations.NormalModeSet;
import autocompchem.run.Action;
import autocompchem.run.Terminator;
import autocompchem.text.TextBlock;

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
        FILE,
        BASISSET, 
        ZMATRIX, 
        LISTOFDOUBLES, 
        LISTOFINTEGERS,
        NORMALMODE,
        NORMALMODESET, 
        ACTION};

//------------------------------------------------------------------------------

    /**
     * Constructor of an empty (un)named data.
     */

    public NamedData()
    {
    }
    
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
     * Corresponds to getValue().toString().
     * @return the value of this data.
     */

    public String getValueAsString()
    {
        return value.toString();
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
     * Looks at the object and find out what class it is an instance of.
     * @param o the object to evaluate
     * @return the class
     */
    
    private static NamedDataType detectType(Object o)
    {
    	NamedDataType tp = NamedDataType.UNDEFINED;
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
        	
        default:
            cVal = value.toString();
            break;
        }
    	NamedData nd = new NamedData(this.getReference(),type, cVal);
    	return nd;
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

}

