package autocompchem.modeling.basisset;

import java.util.HashMap;

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

import java.util.Map;

/**
 * Storage of predefined constants related to quantum mechanical basis sets
 *
 * @author Marco Foscato
 */

public class BasisSetConstants
{
    /**
     * Keyword requiring generation of atom specific basis set
     */
    public final static String ATMSPECBS = "ATOMSPECIFICBASISSET";

    /**
     * Keyword allowing partial assigniation of basis sets
     */
    public final static String ALLOWPARTIALMATCH = "ALLOWPARTIALMATCH";

    /**
     * Keyword defining SMARTS-based type of atom matching rule
     */
    public final static String ATMMATCHBYSMARTS = "SMARTS";

    /**
     * Keyword defining elemental symbol-based type of atom matching rule
     */
    public final static String ATMMATCHBYSYMBOL = "ELEMENT";

    /**
     * Keyword defining the source of a basis set as its bare name
     */
    public final static String BSSOURCENAME = "NAME";

    /**
     * Keyword defining link to external file containing single atom basis set
     */
    public final static String BSSOURCELINK = "LINK";

    /**
     * Nameroot of the atomproperty used to store tha reference to a basis set
     */
    public final static String BSATMPROP = "BSAUTOCOMPCHEM";

    /**
     * Map for converting angular momentum label to integer
     */
    @SuppressWarnings("serial")
        public final static Map<String,Integer> ANGMOMSTRTOINT = 
                                                   new HashMap<String,Integer>()
    {
        {
            put("S",0);
            put("P",1);
            put("D",2);
            put("F",3);
            put("G",4);
            put("H",5);
            put("I",6);
        };
    };

    /**
     * Map for converting angular momentum integer to string label
     */
    @SuppressWarnings("serial")
        public final static Map<Integer,String> ANGMOMINTTOSTR =
                                                   new HashMap<Integer,String>()
    {
        {
            put(0,"S");
            put(1,"P");
            put(2,"D");
            put(3,"F");
            put(4,"G");
            put(5,"H");
            put(6,"I");
        };
    };

}
