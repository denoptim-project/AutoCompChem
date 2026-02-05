package autocompchem.utils;

import java.text.DecimalFormat;

/*   
 *   Copyright (C) 2017  Marco Foscato 
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

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import autocompchem.constants.ACCConstants;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;

/**
 * Toolbox for numbers
 * 
 * @author Marco Foscato
 */

public class NumberUtils
{

//------------------------------------------------------------------------------

    /**
     * Checks if a string is a representation of a number. This method considers
     * Fortran scientific notation as a number, although you'll have to convert
     * the "D" into an "E" before parsing to a number.
     * @param str the string
     * @return <code>true</code> if the string can be converted into a number
     */

    public static boolean isNumber(String str)
    {
        str = formatScientificNotation(str);
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        ParsePosition position = new ParsePosition(0);
        Number num = format.parse(str, position);
        if (str.length() == position.getIndex() || num!=null)
        {
            return true;
        }
        return false;
    }


//------------------------------------------------------------------------------

    /**
     * Ensures that a string representation of a Fortran double/single 
     * precision numbers is reported in a Java compatible way. In practice,
     * we replace "D" with "E".
     * @param str the string to modify
     * @return the modified string with "E" in stead of "D"
     */

    public static String formatScientificNotation(String str)
    {
        String outStr = str;
        if (outStr.toUpperCase().contains("D"))
        {
            outStr = outStr.toUpperCase().replace("D","E");
        }
        if (outStr.toUpperCase().contains("E+00"))
        {
            outStr = outStr.toUpperCase().replace("E+00","");
        }
        if (outStr.toUpperCase().contains("E-00"))
        {
            outStr = outStr.toUpperCase().replace("E-00","");
        }
        if (outStr.toUpperCase().contains("E+"))
        {
            outStr = outStr.toUpperCase().replace("E+","E");
        }
        return outStr;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if the given string is parsable to an integer.
     * @param s the candidate string.
     * @return <code>true</code> if the string can be parsed into an integer.
     */
    public static boolean isParsableToInt(String s)
    {	
    	final String Digits     = "(\\p{Digit}+)";
    	final String iRegex    =
    	      ("[\\x00-\\x20]*"+  // Optional leading "whitespace"
    	       "[+-]?" +
    	       Digits +
    	       "[\\x00-\\x20]*");// Optional trailing "whitespace"

    	return Pattern.matches(iRegex, s);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if the given string is parsable to a double
     * @param s the candidate string
     * @return <code>true</code> if the string can be parsed into a double
     */
    public static boolean isParsableToDouble(String s)
    {
    	// As suggested in JavaDoc
    	
    	final String Digits     = "(\\p{Digit}+)";
    	final String HexDigits  = "(\\p{XDigit}+)";
    	// an exponent is 'e' or 'E' followed by an optionally
    	// signed decimal integer.
    	final String Exp        = "[eE][+-]?"+Digits;
    	final String fpRegex    =
    	      ("[\\x00-\\x20]*"+  // Optional leading "whitespace"
    	       "[+-]?(" + // Optional sign character
    	       "NaN|" +           // "NaN" string
    	       "Infinity|" +      // "Infinity" string

    	       // A decimal floating-point string representing a finite positive
    	       // number without a leading sign has at most five basic pieces:
    	       // Digits . Digits ExponentPart FloatTypeSuffix
    	       //
    	       // Since this method allows integer-only strings as input
    	       // in addition to strings of floating-point literals, the
    	       // two sub-patterns below are simplifications of the grammar
    	       // productions from section 3.10.2 of
    	       // The Java Language Specification.

    	       // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
    	       "((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+

    	       // . Digits ExponentPart_opt FloatTypeSuffix_opt
    	       "(\\.("+Digits+")("+Exp+")?)|"+

    	       // Hexadecimal strings
    	       "((" +
    	        // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
    	        "(0[xX]" + HexDigits + "(\\.)?)|" +

    	        // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
    	        "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

    	        ")[pP][+-]?" + Digits + "))" +
    	       "[fFdD]?))" +
    	       "[\\x00-\\x20]*");// Optional trailing "whitespace"

    	return Pattern.matches(fpRegex, s);
    }

//------------------------------------------------------------------------------

    /**
     * Detects the precision of a float number reported as a string.
     * @param str the string representing the number
     * @return the number of significant digits
     */

    public static int getPrecision(String str)
    {
        int p = 0;
        boolean count = false;
        for (int i = 0; i<str.length(); i++)
        {
            char c = str.charAt(i);
            // Ignore leading chars (including 0)
            if (!count && "123456789".indexOf(c) == -1)
            {
                continue;
            }
            // Stop when anything else than a digit, comma, or point is found
            if (count && "0123456789.,".indexOf(c) == -1)
            {
                break;
            }
            // Count digits (including tailing 0)
            count = true;
            if ("0123456789".indexOf(c) != -1)
            {
                p++;
            }
        }
        // Deal with zeros (i.e., 0.000 vs 0.000000)
        if (p==0 && str.indexOf('0')!=-1)
        {
            p = 1;
            for (int i = 0; i<str.length(); i++)
            {
                char c = str.charAt(i);
                // Ignore leading chars (including 0)
                if (!count && ".".indexOf(c) == -1)
                {
                    continue;
                }
                // Stop on anything else than a digit, comma, or point
                if (count && "0.".indexOf(c) == -1)
                {
                    break;
                }
                // Count digits (including tailing 0)
                count = true;
                if ("0".indexOf(c) != -1)
                {
                    p++;
                }
            }
        }
        return p;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Formats a decimal number using the given pattern but with English format
     * as for separators.
     * @param pattern the pattern to use. Example "###.#"
     * @param decimals minimum number of decimal digits to print. Overwrites the
     * specific defined by the pattern.
     * @param value the value to format
     * @return the formatted string
     */
    public static String getEnglishFormattedDecimal(String pattern, 
            int decimals, double value)
    {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern(pattern);
        df.setMinimumFractionDigits(decimals);
        return df.format(value);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Formats a decimal number using the given pattern but with English format
     * as for separators. Imposes 4 as the minimum number of fractional digits.
     * @param pattern the pattern to use. Example "###.####"
     * @param value the value to format
     * @return the formatted string
     */
    public static String getEnglishFormattedDecimal(String pattern, double value)
    {
        return getEnglishFormattedDecimal(pattern,4,value);
    }

//------------------------------------------------------------------------------
    
    /**
     * Produces the list of indexes complementary to the given ones. Considers
     * only 0-based positive indexes.
     * @param ids the list of ids for which we want the complementary ones.
     * @param size defines the maximum index in an indirect way: it is the
     * size of the complete list of indexes.
     * @return the set of complementary indexes.
     */
    public static Set<Integer> getComplementaryIndexes(Collection<Integer> ids,
    		int size)
    {
    	Set<Integer> chosenIDs = new HashSet<Integer>();
    	for (int i=0; i<size; i++)
    		chosenIDs.add(i);
    	chosenIDs.removeAll(ids);
    	return chosenIDs;
    } 
    
//------------------------------------------------------------------------------
    
    /**
     * Utility to compare two floating point values against the default
     * threshold.
     * @param v1 a value to compare.
     * @param v2 another value to compare.
     * @param thrld the threshold.
     * @return
     */
    public static boolean closeEnough(Double v1, Double v2)
    {
    	return closeEnough(v1, v2, ACCConstants.DOUBLEPRECISION);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Utility to compare two floating point values against a threshold.
     * @param v1 a value to compare.
     * @param v2 another value to compare.
     * @param thrld the threshold.
     * @return
     */
    public static boolean closeEnough(Double v1, Double v2, Double thrld)
    {
    	return Math.abs(v1-v2) < thrld;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Utility to remove any non-numerical character from a string. Typical
     * use case is to get rid of units in a string that reports a numerical 
     * value with units.
     * @param s string possibly containing a number with its units
     * @return a string where the characters of the units have been removed.
     */
    public static String stripUnits(String s)
    {
    	return s.replaceFirst("^[^0-9^-]*","").replaceFirst("[^0-9^-]*$","");
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Check if the units are places in front or after the numerical value.
     * @param valueAndUnits the string to analyze.
     * @param units the units that we expect to find in the string to analyze.
     * @return <code>true</code> if the units are reported before any digits, or
     * <code>false</code> if units are after the first digit or are missing.
     */
    public static boolean unitsAreInFront(String valueAndUnits, String units)
    {
    	int idxUnits = valueAndUnits.indexOf(units);
    	if (idxUnits<0)
    		return false;
    	
    	String[] parts = valueAndUnits.trim().split("[0-9]");
    	
    	// Case of only digits in string
    	if (parts.length==0)
    		return false;
    	return parts[0].indexOf(units) > -1; 
    }
    
    
//------------------------------------------------------------------------------
    
    /**
     * Detects the type of formatting of a number when reported in a string.
     * We assume {@link Locale.ENGLISH}, hence dot as decimal separator.
     * @param s the string to analyze
     * @return the decimal format, which is always based on {@link Locale.ENGLISH}
     */
    public static DecimalFormat detectDecimalFormat(String s)
    {    	
    	s = s.replace("-", "");
    	
    	DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(
    			Locale.ENGLISH);
    	df.setGroupingUsed(false);
    	
    	// Deal with exponent in case of scientific notation
    	String sientificExponent = "";
    	String[] splitAtE = s.trim().split("E");
    	if (splitAtE.length > 1)
    	{
    		sientificExponent = "E";
    		String exponent = splitAtE[1].replace("-", "");
    		for (int ie=0; ie<exponent.length(); ie++)
    			sientificExponent = sientificExponent + "0";
    	}
    	
    	String pattern = "";
    	
    	String[] splitAtDot = splitAtE[0].split("\\.");
    	String integerPart = splitAtDot[0];
    	
    	// deal with integer part
    	String[] splitAtComma = integerPart.split("\\,");
		for (int id=0; id<splitAtComma[0].length(); id++)
		{
			pattern = pattern + "#";
		}
		// This to avoid formats like "-.123"
    	if (pattern.equals("#") && splitAtComma.length == 1)
    		pattern = "0";
    	if (splitAtComma.length > 1)
    	{
    		// We have one or more commas as grouping separator
    		for (int ic=1; ic<splitAtComma.length; ic++)
    		{
    			pattern = pattern + ",";
    			for (int id=0; id<splitAtComma[ic].length(); id++)
    				pattern = pattern + "#";
    		}
    	}
    	
    	// If the integer part is just an integer, then the patter is still empty
    	if (pattern.isBlank())
    		pattern = "0";
    		
    	// Deal with fractional part
    	if (splitAtDot.length > 1)
    	{
    		// We have a decimal part
    		pattern = pattern + ".";
    		String fractionalPart = splitAtDot[1];
    		for (int id=0; id<fractionalPart.length(); id++)
    			pattern = pattern + "0";
    	}
    	
    	pattern = pattern + sientificExponent;
    	
    	df.applyPattern(pattern);
    	return df;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Check if there is a space between units/digits and digits/units.
     * @param valueAndUnits the string to analyze.
     * @param units the units that we expect to find in the string to analyze.
     * @return <code>true</code> if there is any number of blank characters
     * between the two substrings, or <code>false</code> otherwise or if the 
     * units are not found.
     */
    public static boolean spaceBetweenValueAndUnits(String valueAndUnits, 
    		String units)
    {
    	int idxUnits = valueAndUnits.indexOf(units);
    	if (idxUnits<0)
    		return false;
    	
    	String[] parts = valueAndUnits.trim().split("[0-9]");
    	// Case of no units
    	if (parts.length==0)
    		return false;
    	// Case of units are BEFORE any digit
    	if (parts[0].indexOf(units) > -1 && parts[0].endsWith(" "))
    		return true;
    	// Case of units are AFTER any digit
    	if (parts[parts.length-1].indexOf(units) > -1 
    			&& parts[parts.length-1].startsWith(" "))
    		return true;
    	return false;
    }
    
//------------------------------------------------------------------------------
  	
  	/**
  	 * Use a given expression to change an old value into a new one.
  	 * @param expr the expression defining how to calculate the new value.
  	 * @param expFact the factory generating expression language objects.
  	 * @param oldValue the initial value from which we are meant to calculate
  	 * the new value.
  	 * @return the new value with the same units of the old value.
  	 */
  	public static String calculateNewValueWithUnits(String expr, 
  			ExpressionFactory expFact, 
  			String oldValue)
  	{
  		String oldNoUnts = stripUnits(oldValue);
  		DecimalFormat df = detectDecimalFormat(oldNoUnts);
		String units = oldValue.replace(oldNoUnts, "").trim();
		String spacer = spaceBetweenValueAndUnits(oldValue,units) ? " " : "";
		
  		String newVal = df.format(calculateNewValue(expr, expFact, 
  				Double.parseDouble(oldNoUnts)));
  		
  		String newValAndUnits = "";
  		if (unitsAreInFront(oldValue, units))
  		{
  			newValAndUnits = units + spacer + newVal;
  		} else {
  			newValAndUnits = newVal + spacer + units;
  		}
  		return newValAndUnits;
  	}
    
//------------------------------------------------------------------------------
  	
  	/**
  	 * Use a given expression to change an old value into a new one.
  	 * @param expr the expression defining how to calculate the new value.
  	 * @param expFact the factory generating expression language objects.
  	 * @param oldValue the initial value from which we are meant to calculate
  	 * the new value.
  	 * @return the new value.
  	 */
  	public static Double calculateNewValue(String expr, 
  			ExpressionFactory expFact, 
  			Double oldValue)
  	{
  		// Make context for interpreting expression that define how to 
  		// alter the old variable
  		ELContext context = new ELContext() {
  			VariableMapper vm = new VariableMapper() {
                  @Override
                  public ValueExpression resolveVariable(String varName) 
                  {
                      ValueExpression ve = new ValueExpression() 
                      {   
                          /**
                           * Version ID
                           */
                          private static final long serialVersionUID = 1L;

                          @SuppressWarnings("unchecked")
						  @Override
                          public Object getValue(ELContext c) {
                              return oldValue;
                          }

                          @Override
                          public void setValue(ELContext c, Object v) {
                          }

                          @Override
                          public boolean isReadOnly(ELContext c) {
                              return true;
                          }

                          @Override
                          public Class<?> getType(ELContext c) {
                              return Double.class;
                          }

                          @Override
                          public Class<?> getExpectedType() {
                              return Double.class;
                          }

                          @Override
                          public String getExpressionString() {
                              return null;
                          }

                          @Override
                          public boolean equals(Object obj) {
                              return false;
                          }

                          @Override
                          public int hashCode() {
                              //Dummy hashcode
                              return 0;
                          }

                          @Override
                          public boolean isLiteralText() {
                              return false;
                          }
                      };
                      return ve;
                  }

                  @Override
                  public ValueExpression setVariable(String variable, 
                          ValueExpression expression) 
                  {
                      return null;
                  }
              };

  			@Override
  			public ELResolver getELResolver() {
  				return null;
  			}

  			@Override
  			public FunctionMapper getFunctionMapper() {
  				return null;
  			}

  			@Override
  			public VariableMapper getVariableMapper() {
  				return vm;
  			}
  			
  		};
  		
  		// Create the actual function for editing the old value
  		ValueExpression valEditingFunct = expFact.createValueExpression(
  				context, expr, Double.class);
  		
  		// Do the actual calculation of the new value
  		Double newValue = null;
  		Object result = valEditingFunct.getValue(context);
        if (result != null) 
        {
            if (result instanceof Double)
            {
            newValue = (Double) result;
            } else {
                throw new IllegalArgumentException("Evaluation of "
                        + "expression '" + expr + "' "
                        + "returned '" + result.getClass() + "'). "
                        + "Check expression.");
            }
        } else {
            throw new IllegalArgumentException("Evaluation of "
                + "Expression Language returned null. "
                    + "Check expression.");
        }
        
        return newValue;
  	}
  	
//------------------------------------------------------------------------------
  	
  	/**
  	 * Parses strings like '[int, int, int, ...]' into the corresponding array
  	 * of int.
  	 * @param string
  	 * @return the corresponding int[].
  	 */
  	
  	public static int[] parseArrayOfInt(String string)
  	{
  		String noSpaces = string.replaceAll("\\s", "");
  		if  ("[]".equals(noSpaces))
  		{
  			return new int[] {};
  		}
  		String[] items = noSpaces.replaceAll("\\[", "")
    			.replaceAll("\\]", "").split(",");
    	int[] array = new int[items.length];
    	for (int i = 0; i < items.length; i++) 
		{
    		array[i] = Integer.parseInt(items[i]);
    	}
  		return array;
  	}
  	
//------------------------------------------------------------------------------
  	
  	/**
  	 * Parses strings like '[double, double, double, ...]' into the 
  	 * corresponding array of double.
  	 * @param string
  	 * @return the corresponding double[].
  	 */
  	public static double[] parseArrayOfDouble(String string)
  	{
  		String noSpaces = string.replaceAll("\\s", "");
  		if  ("[]".equals(noSpaces))
  		{
  			return new double[] {};
  		}
  		String[] items = noSpaces.replaceAll("\\[", "")
    			.replaceAll("\\]", "").replaceAll("\\s", "").split(",");
    	double[] array = new double[items.length];
    	for (int i = 0; i < items.length; i++) 
		{
    		array[i] = Double.parseDouble(items[i]);
    	}
  		return array;
  	}

//------------------------------------------------------------------------------    
    
}
