package autocompchem.modeling.basisset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import autocompchem.utils.NumberUtils;

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


/**
 * Object representing a single primitive function. 
 * Type and angular momentum can be used to characterize this function, but
 * there is no internal check that the information in the <code>type</code>
 * and that in of the <code>angMmnt</code> are consistent.
 *
 * @author Marco Foscato
 */

public class Primitive
{
    /**
     * Primitive type (S, P, D, SP, SPD, F, G, ...)
     */
    private String type = "notype";

    /**
     * Angular momentum
     */
    private int angMmnt = -1;

    /**
     * Coefficients. Can be more than one in accordance to the use of hybrid
     * primitives that mix functions with different angular momentum into one
     * single line. For instance, see how 6-31G basis set for C is reported in
     * the basis set exchange website.
     */
    private List<Double> coefficient = new ArrayList<Double>();

    /**
     * Exponent
     */
    private double exponent = 0.0;

    /**
     * Precision of the coefficients
     */
    private int precCoeff = 0;

    /**
     * Precision of the exponent
     */
    private int precExp = 0;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty Primitive
     */

    public Primitive()
    {}

//------------------------------------------------------------------------------

    /**
     * Constructor for a Primitive with definition of all the fields
     * @param type the string-representation of the type of primitive
     * @param angMmnt the angular momentum
     * @param coeff the coefficient 
     * @param exp the exponent
     * @param precCoeff the precision of the coefficient
     * @param precExp the precision of the exponent
     */

    public Primitive(String type, int angMmnt, List<Double> coeff, double exp, 
                                                     int precCoeff, int precExp)
    {
        this.type = type;
        this.angMmnt = angMmnt;
        this.coefficient = coeff;
        this.exponent = exp;
        this.precCoeff = precCoeff;
        this.precExp = precExp;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a Primitive with definition of all the fields
     * @param type the string-representation of the type of primitive
     * @param angMmnt the angular momentum
     * @param coeff the coefficient 
     * @param exp the exponent
     * @param precCoeff the precision of the coefficient
     * @param precExp the precision of the exponent
     */

    public Primitive(String type, int angMmnt, double coeff, double exp, 
                                                     int precCoeff, int precExp)
    {
        this.type = type;
        this.angMmnt = angMmnt;
        this.coefficient = new ArrayList<Double>(Arrays.asList(coeff));
        this.exponent = exp;
        this.precCoeff = precCoeff;
        this.precExp = precExp;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the type of function. The type can be a string representation of 
     * the angular momentum, but the value of the field <code>type</code>
     * is disjoint from that of the angular momentum.
     * @param type the type of this function
     */

    public void setType(String type)
    {
        this.type = type;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the angular momentum
     * @param l the angular momentum quantum number (L)
     */

    public void setAngularMomentum(int l)
    {
        this.angMmnt = l;
    }

//------------------------------------------------------------------------------
 
    /**
     * Sets the coefficient.
     * @param c the coefficient.
     */

    public void setCoefficient(double c)
    {
        this.coefficient = new ArrayList<Double>(Arrays.asList(c));
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the coefficients.
     * @param c the coefficient.
     */

    public void setCoefficients(List<Double> coefs)
    {
        this.coefficient = coefs;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Append a coefficient
     * @param c the coefficient
     */

    public void appendCoefficient(double c)
    {
        this.coefficient.add(c);
    }

//------------------------------------------------------------------------------

    /**
     * Sets the exponent
     * @param e the exponent
     */

    public void setExponent(double e)
    {
        this.exponent = e;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the precision of the coefficient
     * @param p the precision
     */

    public void setCoeffPrecision(int p)
    {
        this.precCoeff = p;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the precision of the exponent
     * @param p the precision
     */

    public void setExpPrecision(int p)
    {
        this.precExp = p;
    } 

//------------------------------------------------------------------------------

    /**
     * Returns the angular moment
     * @return the angular momentum
     */
 
    public int getAngMmnt()
    {
        return angMmnt;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the coefficient
     * @return the coefficient
     */

    public List<Double> getCoeff()
    {
        return coefficient;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the exponent
     * @return the exponent
     */

    public double getExp()
    {
        return exponent;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the precision for the coefficient
     * @return the precision 
     */

    public int getCoeffPrecision()
    {
        return precCoeff;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the precision for the exponent
     * @return the precision
     */

    public int getExpPrecision()
    {
        return precExp;
    }

//------------------------------------------------------------------------------

    /**
     * Deep-clone this primitive
     * @return a new object with the same content as this one
     */

     public Primitive clone()
     {
        return new Primitive(type, angMmnt, coefficient, exponent, precCoeff,
                                                                       precExp);
     }
     
//------------------------------------------------------------------------------

     @Override
     public boolean equals(Object o)
     {
    	 if (!(o instanceof Primitive))
    		 return false;
    	 Primitive other = (Primitive) o;
    	 
    	 if (this.coefficient.size() != other.coefficient.size())
    		 return false;
    				 
		 for (int i=0; i<this.coefficient.size(); i++)
		 {
			 if (!NumberUtils.closeEnough(this.coefficient.get(i), 
					 other.coefficient.get(i)))
				 return false;
		 }
    	 
    	 return this.type.equals(other.type) 
    			 && this.angMmnt == other.angMmnt 
    			 && this.exponent == other.exponent 
    			 && this.precCoeff == other.precCoeff
    			 && this.precExp == other.precExp;
     }
     
//-----------------------------------------------------------------------------
     
     @Override
     public int hashCode()
     {
     	return Objects.hash(coefficient, type, angMmnt, exponent, precCoeff, 
     			precExp);
     }
     
//------------------------------------------------------------------------------

    /**
     * Return a string representation of this object
     * @return a string representation of this primitive
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Primitive [type:").append(type).append(", ");
        sb.append("L:").append(angMmnt).append(", ");
        sb.append("coeff:").append(coefficient).append(", ");
        sb.append("exp:").append(exponent).append(", ");
        sb.append("precCoeff:").append(precCoeff).append(", ");
        sb.append("precExp:").append(precExp).append("] ");
        return sb.toString();
    }

//------------------------------------------------------------------------------
     
}
