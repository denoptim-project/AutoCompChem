package autocompchem.modeling.forcefield;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

import org.openscience.cdk.AtomType;

/**
 * Object representing a force field parameter
 *
 * @author Marco Foscato
 */

public class ForceFieldParameter implements Serializable
{
    /**
         * Version ID
         */
        private static final long serialVersionUID = -586719556948482942L;

        /**
     * Reference name
     */
    private String name = "noname";

    /**
     * Type of force field term
     */
    private String type = "notype";

    /**
     * Atom types
     */
    private ArrayList<AtomType> atmTyps = new ArrayList<AtomType>();

    /**
     * Force constants
     */
    private ArrayList<ForceConstant> forceKsts = new ArrayList<ForceConstant>();

    /**
     * Equilibrium values
     */
    private ArrayList<EquilibriumValue> eqVals = 
                                              new ArrayList<EquilibriumValue>();

    /**
     * Properties
     */
    private Map<String,Object> properties = new HashMap<String,Object>();


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty ForceFieldParameter
     */

    public ForceFieldParameter()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a ForceFieldParameter with defintition of some fields
     * @param name the reference name on this parameter
     * @param type the type of force field term
     */

    public ForceFieldParameter(String name, String type)
    {
        this.name = name;
        this.type = type;
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a ForceFieldParameter with defintition of its values
     * @param name the reference name on this parameter
     * @param type the type of force field term 
     * @param atmTyps the list of atom types
     * @param forceKsts the list of force costants
     * @param eqVals the list of equilibrium values
     */

    public ForceFieldParameter(String name, String type, 
                                                    ArrayList<AtomType> atmTyps,
                                             ArrayList<ForceConstant> forceKsts,
                                             ArrayList<EquilibriumValue> eqVals)
    {
        this.name = name;
        this.type = type;
        this.atmTyps = atmTyps;
        this.forceKsts = forceKsts;
        this.eqVals = eqVals;
    }

//------------------------------------------------------------------------------

    /**
     * Append an atom type to the existing list of atom types
     * @param at the atom type to add
     */

    public void addAtomType(AtomType at)
    {
        atmTyps.add(at);
    }

//------------------------------------------------------------------------------

    /**
     * Append a force constant to the existing list of force constants
     * @param dfk the value of the force constant to add
     */

    public void addForceConstant(double dfk)
    {
        ForceConstant fk = new ForceConstant();
        fk.setValue(dfk);
        forceKsts.add(fk);
    }

//------------------------------------------------------------------------------

    /**
     * Append a force constant to the existing list of force constants
     * @param fk the force constant to add
     */

    public void addForceConstant(ForceConstant fk)
    {
        forceKsts.add(fk);
    }

//------------------------------------------------------------------------------

    /**
     * Append an equilibrium value to the existing list of values
     * @param ev the equilibrium value to add
     */

    public void addEquilibriumValue(EquilibriumValue ev)
    {
        eqVals.add(ev);
    }

//------------------------------------------------------------------------------

    /**
     * Append an equilibrium value to the existing list of values
     * @param dev the value to add
     */

    public void addEquilibriumValue(double dev)
    {
        EquilibriumValue ev = new EquilibriumValue();
        ev.setValue(dev);
        eqVals.add(ev);
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
     * Get the type of this force field parameter
     * @return the type
     */

    public String getType()
    {
        return type;
    }

//------------------------------------------------------------------------------

    /**
     * Get the list of atom types
     * @return the list of atom types
     */

    public ArrayList<AtomType> getAtomTypes()
    {
        return atmTyps;
    }

//------------------------------------------------------------------------------

    /**
     * Get the list of force constants
     * @return the list of force constants
     */

    public ArrayList<ForceConstant> getForceConstants()
    {
        return forceKsts;
    }

//------------------------------------------------------------------------------

    /**
     * Get the list of equilibrium values
     * @return the list of equilibrium values
     */

    public ArrayList<EquilibriumValue> getEqValues()
    {
        return eqVals;
    }

//------------------------------------------------------------------------------

    /**
     * Checks if the given property is already defined
     * @param propName the string identifying the property
     * @return <code>true</code> true is this object contains a property with
     * the given name.
     */

    public boolean hasProperty(String propName)
    {
        return properties.containsKey(propName);
    }

//------------------------------------------------------------------------------

    /**
     * Set the value of a property
     * @param propName the string identifying the property to set
     * @param propValue the new value of the property
     */

    public void setProperty(String propName, Object propValue)
    {
        properties.put(propName,propValue);
    }

//------------------------------------------------------------------------------

    /**
     * Get the value of a property
     * @param propName the string identifying the property to recover
     * @return the value of the property
     */

    public Object getProperty(String propName)
    {
        return properties.get(propName);
    }

//------------------------------------------------------------------------------

    /**
     * Compare some field of this an another force field parameter. It allows to
     * decide whether this and the other parameter
     *  can be threated as analogues that refer to
     * the same type of force field term. 
     * The <code>name</code> can be different, 
     * but the <code>type</code> must be equal,
     * and the list of atom types must be the same 
     * or (TODO: to be implemented) a compatible permulation.
     * @param other the parameter to compare with this one
     * @return <code>true</code> is the two correspond to the same type of
     * force field term.
     */

    public boolean isAnalogueTo(ForceFieldParameter other)
    {
        boolean res = true;        
        if (!this.getType().equals(other.getType())
            || this.atmTyps.size() != other.getAtomTypes().size())
        {
            return false;
        }

        for (int i=0; i<this.atmTyps.size(); i++)
        {
            AtomType tAT = this.atmTyps.get(i);
            AtomType oAT = other.atmTyps.get(i);
            if (!tAT.getProperty(ForceFieldConstants.SMARTSQUERYATMTYP).equals(
                 oAT.getProperty(ForceFieldConstants.SMARTSQUERYATMTYP)))
            {
                return false;
            }
        }

        //TODO: check compatible permutations of atom types

        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Generate a deep copy of this ForceFiledParameter. Exploits serialization
     * to make a brand new object equal to this, but not the same as this. 
     * @return the deep copy
     * @throws Throwable in case of any failure in serialization-based deep copy
     * process.
     */

    public ForceFieldParameter deepCopy() throws Throwable
    {        
        //serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this);
        oos.flush();
        oos.close();
        baos.close();
        byte[] bytes = baos.toByteArray();
        //deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ForceFieldParameter newFFP = 
                 (ForceFieldParameter) new ObjectInputStream(bais).readObject();
        return newFFP;
    }

//------------------------------------------------------------------------------

    /**
     * Return a simplified string representing this object
     * @return a string representation
     */

    public String toSimpleString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("FF-Parameter [Name:").append(name).append(", ");
        sb.append("Type:").append(type).append(", ");
        sb.append("K:");
        for (ForceConstant k : forceKsts)
        {
            sb.append("(" + String.format("%f",k.getValue()) + ")");
        }
        sb.append(", ");
        sb.append("eq:");
        for (EquilibriumValue e : eqVals)
        {
            sb.append("(" + String.format("%f",e.getValue()) + ")");
        }
        sb.append("] ");
        return sb.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Return a string representation of this object
     * @return a string representation
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ForceFieldParameter [name:").append(name).append(", ");
        sb.append("type:").append(type).append(", ");
        sb.append("atmTyps:").append(atmTyps.toString()).append(", ");
        sb.append("forceKsts:").append(forceKsts.toString()).append(", ");
        sb.append("eqVals:").append(eqVals.toString()).append(", ");
        sb.append("properties:").append(properties.toString()).append("] ");
        return sb.toString();
    }

//------------------------------------------------------------------------------
     
}
