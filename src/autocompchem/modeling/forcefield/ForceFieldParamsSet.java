package autocompchem.modeling.forcefield;

/*   
 *   Copyright (C) 2016  Marco Foscato 
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.AtomType;

/**
 * Object representing a collection of consistent force field parameters, often
 * referred as to "the force field parameters set", or simply "the force field".
 *
 * @author Marco Foscato
 */

public class ForceFieldParamsSet implements Serializable
{
    /**
	 * Version ID
	 */
	private static final long serialVersionUID = -5183573228916122504L;

	/**
     * Reference name
     */
    private String name = "noname";

    /**
     * Atom types 
     */
    private Map<String,AtomType> atmTyps = new HashMap<String,AtomType>();

    /**
     * Atom classes (i.e., a group of closely related 'atom types' is an 'atom
     * class'). To retain consistency with CDK the AtomType object is used
    * to represent atom classes.
     */
    private Map<String,AtomType> atmClasses = new HashMap<String,AtomType>();

    /**
     * Force Field Parameters Map by parameter type
     */
    private Map<String,ArrayList<ForceFieldParameter>> params = 
                           new HashMap<String,ArrayList<ForceFieldParameter>>();

    /**
     * Properties
     */
    private Map<String,Object> props = new HashMap<String,Object>();


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty ForceFieldParamsSet
     */

    public ForceFieldParamsSet()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a named yet empty ForceFieldParamsSet
     * @param name the reference name on this parameter
     */

    public ForceFieldParamsSet(String name)
    {
        this.name = name;
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a ForceFieldParamsSet with defintition of its values
     * @param name the reference name on this parameter
     * @param atmTyps the set of atom types used in this force field. If the
     * given atom types specify also the atom class, then the list of atom
     * classes is also created.
     * @param params the map of force field parameters collected by interaction
     * type
     * @param props the map of force field properties
     */

    public ForceFieldParamsSet(String name, Set<AtomType> atmTyps,
                              Map<String,ArrayList<ForceFieldParameter>> params,
                                                       Map<String,Object> props)
    {
        this.name = name;
        addAllAtomTypes(atmTyps);
        this.params = params;
        this.props = props;
    }

//------------------------------------------------------------------------------

    /**
     * Set the name 
     * @param name the name
     */
    
    public void setName(String name)
    {
	this.name = name;
    }

//------------------------------------------------------------------------------

    /**
     * Append an atom type to the existing list of atom types
     * @param at the atom type to add
     */

    public void addAtomType(AtomType at)
    {
        atmTyps.put(at.getAtomTypeName(),at);
	Object atCls = at.getProperty(ForceFieldConstants.ATMCLSINT);
	if (atCls!=null && !atmClasses.containsKey(atCls.toString()))
	{
	    //NB: to retain consistency with CDK the AtomType object is used
	    //to represent atom classes.
	    AtomType ac = new AtomType("X");
	    ac.setProperty(ForceFieldConstants.ATMCLSSTR,atCls.toString());
	    this.atmClasses.put(atCls.toString(),ac);
	}
    }

//------------------------------------------------------------------------------

    /**
     * Append all atom types to the existing list of atom types
     * @param atmTyps the list of atom types to add
     */

    public void addAllAtomTypes(Set<AtomType> atmTyps)
    {
	for (AtomType at : atmTyps)
	{
	    addAtomType(at);
	}
    }

//------------------------------------------------------------------------------

    /**
     * Append a force field parameter of a specific type of force field 
     * component.
     * The {@link ForceFieldParameter} object will be classified ONLY
     * according to the <code>type</code> given as argument, no matter 
     * no matter what is the type defined within the object.
     * @param ffPar the parameter to be added
     * @param type the type of force field component
     */

    public void addParameter(ForceFieldParameter ffPar, String type)
    {
        if (params.containsKey(type))
	{
	    params.get(type).add(ffPar);
	}
	else
	{
	    ArrayList<ForceFieldParameter> list = 
					   new ArrayList<ForceFieldParameter>();
	    list.add(ffPar);
	    params.put(type,list);
	}
    }

//------------------------------------------------------------------------------

    /**
     * Append a force field parameter to this parameters set
     * @param ffPar the parameter to be added
     */

    public void addParameter(ForceFieldParameter ffPar)
    {
	addParameter(ffPar, ffPar.getType());
    }

//------------------------------------------------------------------------------

    /**
     * Get the name of this force field parameter
     * @return the name
     */

    public String getName()
    {
        return name;
    }

//------------------------------------------------------------------------------

    /**
     * Return the atom class with the given reference name.
     * If no such atom type exists an exception is trown.
     * @param acRef the reference of the wanted atom class
     * @return the <code>AtomType</code> object representing the atom class
     * @throws Throwable when the reqest cannot be satisfied
     */

    public AtomType getAtomClass(String acRef) throws Throwable
    {
	if (atmClasses.containsKey(acRef))
	{
	    return atmClasses.get(acRef);
	}
	else
	{
            throw new Throwable("Atom class '" + acRef + "' not found.");
	}
    }

//------------------------------------------------------------------------------

    /**
     * Return the atom type with the given integer ID. The integer ID is stored
     * into the property {@value ForceFieldConstants#ATMTYPINT}, but may or may
     * not correspond to the atom type name, which is returned by the 
     * AtomType.getAtomTypeName() method.
     * If no such atom type
     * exists an exception is trown.
     * @param iat the integer ID of the wanted atom type
     * @return the atom type 
     * @throws Throwable when the reqest cannot be satisfied
     */

    public AtomType getAtomType(int iat) throws Throwable
    {
        for (Map.Entry<String,AtomType> e : atmTyps.entrySet())
        {
	    AtomType at = e.getValue();
	    Object pObj = at.getProperty(ForceFieldConstants.ATMTYPINT);
	    if (pObj == null)
	    {
	        continue;
	    }
            int id = Integer.parseInt(pObj.toString());
            if (id == iat)
            {
                return at;
            }
        }
        throw new Throwable("Atom type with integer ID=" + iat + " not found.");
    }

//------------------------------------------------------------------------------

    /** 
     * Return the list of atom types defined in this force field parameters set.
     * @return the map of AtomTypes
     */
 
    public  Map<String,AtomType> getAtomTypes()
    {
	return atmTyps;
    }

//------------------------------------------------------------------------------

    /**
     * Return the list of atom classes defined in this set
     * @return the map of atom classes (as AtomType objects)
     */

    public  Map<String,AtomType> getAtomClasses()
    {
        return atmClasses;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of force field parameters types
     * @return the list of force field parameters types
     */

    public Set<String> getParamTypes()
    {
        return params.keySet();
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of force field parameters of the given type
     * @param type the type of parameters to be returned
     * @return the list of force field parameters of the given type
     */

    public ArrayList<ForceFieldParameter> getParamsOfType(String type)
    {
	return params.get(type);
    }

//------------------------------------------------------------------------------

    /** 
     * Returns the list of FF-property keys sorted alphabetically.
     * @return the list of keys of force field properties
     */

    public ArrayList<String> getPropertyKeys()
    {
	ArrayList<String> sorted = new ArrayList<String>();
	sorted.addAll(props.keySet());
	Collections.sort(sorted);
	return sorted;
    } 

//------------------------------------------------------------------------------

    /**
     * Checks if the given property is already defined
     * @param propName the string identifying the property
     * @return <code>true</code> if this object contains a property with the 
     * given name
     */

    public boolean hasProperty(String propName)
    {
	return props.containsKey(propName);
    }

//------------------------------------------------------------------------------

    /**
     * Set the value of a property
     * @param propName the string identifying the property to set
     * @param propValue the new value of the property
     */

    public void setProperty(String propName, Object propValue)
    {
        props.put(propName,propValue);
    }

//------------------------------------------------------------------------------

    /**
     * Get the value of a property
     * @param propName the string identifying the property to recover
     * @return the value of the property
     */

    public Object getProperty(String propName)
    {
        return props.get(propName);
    }

//------------------------------------------------------------------------------

}
