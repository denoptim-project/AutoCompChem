package autocompchem.modeling.basisset;

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

import autocompchem.run.Terminator;

/**
 * Object representing the chemical context-based rule to assign a basis set to
 * specific atoms. There are currently two types of rules to identify atoms:
 * <ul>
 *   <li>Element symbol</li>
 *   <li>SMARTS</li>
 * </ul>
 * Text can be parsed provided it respects the following syntax:
 * <br>
 * <pre>RuleType key BasisSource Basis</pre>
 * where:
 * <ul>
 *  <li><code>RuleType</code> is either
 * {@value autocompchem.modeling.basisset.BasisSetConstants#ATMMATCHBYSMARTS} or
 * {@value autocompchem.modeling.basisset.BasisSetConstants#ATMMATCHBYSYMBOL}</li>
 *  <li><code>Key</code> is the actual SMARTS string of element symbol</li>
 *  <li><code>BasisSource</code> define the type of source of the basis set;
 * sources can of two types:
 *  <ul>
 *    <li>links (use keyword
 * {@value autocompchem.modeling.basisset.BasisSetConstants#BSSOURCELINK})
 * to text files containing the basis set for a single atom type </li>
 *    <li>names of basis sets (use keyword
 * {@value autocompchem.modeling.basisset.BasisSetConstants#BSSOURCENAME} in the
 * format recognized by
 * quantum chemistry packages (e.g., cc-pVDZ, LANL2DZ)</li>
 *  </ul></li>
 *  <li><code>Basis</code> the actual pathname or basis set name</li>
 * </ul>
 *
 * @author Marco Foscato
 */

public class BSMatchingRule
{
    /**
     * Reference name 
     */
    private String refName = "noname";

    /**
     * Rule type
     */
    private String type = "notype";

    /**
     * The rule's key: SMARTS query or elemental symbol
     */
    private String smarts = "";

    /**
     * Type of source
     */
    private String srcType = "";

    /**
     * Source as a basis set name or pathname of a file
     */
    private String source = "";


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty rule
     */

    public BSMatchingRule()
    {}

//------------------------------------------------------------------------------

    /**
     * Constructor for a rule from its parameters
     * @param refName reference name
     * @param type type of rule
     * @param query the SMARTS query or elemental symbol
     * @param bsSrcTyp the type of source from which to take the basis set
     * @param bsSurce the name or pathname of the source of the basis set
     */

    public BSMatchingRule(String refName, String type, String query,
    		String bsSrcTyp, String bsSurce)
    {
        this.refName = refName;
        this.type = type;
        this.smarts = query;
        this.srcType = bsSrcTyp;
        this.source = bsSurce;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a rule by parsing a formatted string of text. The syntax
     * is defined above.
     * @param txt the string to be parsed
     * @param i a unique integer used to identify the rule. Is used to build the
     * reference name of the generated rule by first word in txt with i.
     */

    public BSMatchingRule(String txt, int i)
    {
    	this(txt, txt.split("\\s+")[0]+i);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a rule by parsing a formatted string of text. The syntax
     * is defined above.
     * @param txt the string to be parsed
     * @param refName the reference name of this rule to build.
     */

    public BSMatchingRule(String txt, String refName)
    {
        String[] p = txt.split("\\s+");
        String msg = "ERROR! The following string does not look like a "
                             + "properly formatted rule for basis set "
                             + "generation. ";
        if (p.length < 4)
        {
            Terminator.withMsgAndStatus(msg + "Wrong number of words. "
                                                     + " Check line " + txt,-1);
        }
        if (!p[0].toUpperCase().equals(BasisSetConstants.ATMMATCHBYSMARTS) &&
                 !p[0].toUpperCase().equals(BasisSetConstants.ATMMATCHBYSYMBOL))
        {
            Terminator.withMsgAndStatus(msg + "Unknown rule type. "
                                             + p[0] + ". Check line " + txt,-1);
        }

        if (!p[2].toUpperCase().equals(BasisSetConstants.BSSOURCENAME) &&
                     !p[2].toUpperCase().equals(BasisSetConstants.BSSOURCELINK))
        {
            Terminator.withMsgAndStatus(msg + "Unknown basis set source "
                                 + "type: " + p[2] + ".  Check line " + txt,-1);
        }
        String pp = "";
        for (int j=3; j<p.length; j++)
        {
            pp = pp + p[j];
            if (p.length>4 && j<(p.length-1))
            {
                pp = pp + " ";
            }
        }
        this.refName = refName;
        this.type = p[0];
        if (p[1].equals("null"))
        	this.smarts = null;
        else
        	this.smarts = p[1];
        this.srcType = p[2];
        this.source = pp;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the reference name
     * @return the reference name
     */

    public String getRefName()
    {
        return refName;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the type of this rule
     * @return the type
     */

    public String getType()
    {
        return type;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the key of this rule: a SMARTS query or an elemental symbol
     * @return the SMARTS or elemental symbol
     */

    public String getKey()
    {
        return smarts;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the type of source
     * @return the type of source
     */

    public String getSourceType()
    {
        return srcType;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the source
     * @return the source of the basis se as its name or pathname
     */

    public String getSource()
    {
        return source;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a string suitable to be parsed back by this object's constructor.
     * @return the string
     */

    public String toParsableString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(" ");
        sb.append(smarts).append(" ");
        sb.append(srcType).append(" ");
        sb.append(source);
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
