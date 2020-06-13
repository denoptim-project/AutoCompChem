package autocompchem.chemsoftware.tinker;

/*   
 *   Copyright (C) 2014  Marco Foscato 
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

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;


/**
 * Storage of predefined constants for Tinker related tools.
 *
 * @author Marco Foscato
 */

public class TinkerConstants
{
   /**
    * Keyword for comment line
    */
   public final static String FFKEYIGNORE ="#";

   /**
    * Keyword for force field title/name
    */
   public final static String FFKEYNAME ="FORCEFIELD";

   /**
    * Keyword for atom type definition
    */
   public final static String FFKEYATMTYP ="ATOM";

   /**
    * MMFF keyword for van der Waals parameters
    */
   public final static String FFKEYMMFFVDW ="MMFFVDW";

   /**
    * Non-standard keyword for multy-hapto interligand van der Waals parameters
    */
   public final static String FFKEYMHILVDW ="MHILVDW";

   /**
    * MMFF keyword for bond stretching
    */
   public final static String FFKEYMMFFSTR ="MMFFBOND";

   /**
    * MMFF keyword for empirical bond stretching
    */
   public final static String FFKEYMMFFEMPSTR ="MMFFBONDER";

   /**
    * MMFF keyword for empirical covalent radius
    */
   public final static String FFKEYMMFFEMPRAD ="MMFFCOVRAD";

   /**
    * MMFF keyword for angle bending
    */
   public final static String FFKEYMMFFBND ="MMFFANGLE";

   /**
    * MMFF keyword for stretchbend
    */
   public final static String FFKEYMMFFSBN ="MMFFSTRBND";

   /**
    * MMFF keyword for out-of-plane
    */
   public final static String FFKEYMMFFOOP ="MMFFOPBEND";

   /**
    * MMFF keyword for bond torsion
    */
   public final static String FFKEYMMFFTOR ="MMFFTORSION";

   /**
    * MMFF keyword for bond charge increment
    */
   public final static String FFKEYMMFFBCI ="MMFFBCI";

   /**
    * MMFF keyword for empirical bond charge increment
    */
   public final static String FFKEYMMFFPBCI ="MMFFPBCI";

   /**
    * MMFF keyword for atom class equivalency
    */
   public final static String FFKEYMMFFATEQ ="MMFFEQUIV";

   /**
    * MMFF keyword for default stretch bend
    */
   public final static String FFKEYMMFFDEFSBN ="MMFFDEFSTBN";

   /**
    * MMFF keyword for atom type properties
    */
   public final static String FFKEYMMFFATPROP ="MMFFPROP";

   /**
    * MMFF keyword for ionic aromatic rings/bonds
    */
   public final static String FFKEYMMFFAROM ="MMFFAROM";

   /**
    * All keywords for force field parameters. Keywords that are NOT listed here
    * are intended for special settings and properties that do not fit into
    * the standard data structure of force field parameters.
    */
   public final static Set<String> FFKEYALLFFPAR = new HashSet<>(Arrays.asList(
                                           FFKEYMMFFVDW,
					   FFKEYMHILVDW,
                                           FFKEYMMFFSTR,
                                           FFKEYMMFFEMPSTR,
                                           FFKEYMMFFEMPRAD,
                                           FFKEYMMFFBND,
                                           FFKEYMMFFSBN,
                                           FFKEYMMFFOOP,
                                           FFKEYMMFFTOR,
                                           FFKEYMMFFBCI,
                                           FFKEYMMFFPBCI,
                                           FFKEYMMFFATEQ,
                                           FFKEYMMFFDEFSBN,
                                           FFKEYMMFFATPROP,
                                           FFKEYMMFFAROM));

   /**
    * Name of FF parameter's property used to store value of alpha-i for MMFF
    * van der Waals parametrs
    */
   public final static String FFPRPROPMMFFVDWA = "alpha-i";

   /**
    * Name of FF parameter's property used to store value of N-i for MMFF
    * van der Waals parametrs
    */
   public final static String FFPRPROPMMFFVDWN = "N-i";

   /**
    * Name of FF parameter's property used to store value of G-i for MMFF
    * van der Waals parametrs
    */
   public final static String FFPRPROPMMFFVDWG = "G-i";

   /**
    * Name of FF parameter's property used to store flag for MMFF
    * donor/acceptor property in van der Waals parametrs
    */
   public final static String FFPRPROPMMFFVDWDA = "da";

   /**
    * Name of FF parameter's property used to store MMFF bond stretching type
    */
   public final static String FFPRPROPMMFFSTRBT = "bt";

   /**
    * Name of FF parameter's property used to store atomic number 1 in
    * MMFF empirical rule for stretching
    */
   public final static String FFPRPROPMMFFEMPSTRNA = "an1";

   /**
    * Name of FF parameter's property used to store atomic number 2 in
    * MMFF empirical rule for stretching
    */
   public final static String FFPRPROPMMFFEMPSTRNB = "an2";

   /**
    * Name of FF parameter's property used to store atomic number 
    * in MMFF epirical rule for stretching
    */
   public final static String FFPRPROPMMFFEMPAN = "A";

   /**
    * Name of FF parameter's property used to store empirical atom radius
    * in MMFF epirical rule for stretching
    */
   public final static String FFPRPROPMMFFEMPRAD = "covRad";

   /**
    * Name of FF parameter's property used to store empirical atom radius
    * in MMFF epirical rule for stretching
    */
   public final static String FFPRPROPMMFFEMPEL = "PaulingElectroneg";

   /**
    * Name of FF parameter's property used to store MMFF angle type
    */
   public final static String FFPRPROPMMFFANGTYP = "at";

   /**
    * Name of FF parameter's property used to store MMFF stretch-bend type
    */
   public final static String FFPRPROPMMFFSBNTYP = "sbt";

   /**
    * Name of FF parameter's property used to store first periodic table row
    * in defaults stretch-bend parameters
    */
   public final static String FFPRPROPMMFFEMPSTBIR = "IR";

   /**
    * Name of FF parameter's property used to store second periodic table row
    * in defaults stretch-bend parameters
    */
   public final static String FFPRPROPMMFFEMPSTBJR = "JR";

   /**
    * Name of FF parameter's property used to store third periodic table row
    * in defaults stretch-bend parameters
    */
   public final static String FFPRPROPMMFFEMPSTBKR = "KR";

   /**
    * Name of FF parameter's property used to store force constant IJK
    */
   public final static String FFPRPROPMMFFEMPSTBKIJK = "kbaIJK";

   /**
    * Name of FF parameter's property used to store force constant KJI
    */
   public final static String FFPRPROPMMFFEMPSTBKKJI = "kbaKJI";

   /**
    * Name of FF parameter's property used to store phase factors list 
    * in MMFF torsion parameter
    */
   public final static String FFPRPROPMMFFTORPH = "phase";

   /**
    * Name of FF parameter's property used to store fold numbers list
    * in MMFF torsion parameter
    */
   public final static String FFPRPROPMMFFTORNS = "fold";

   /**
    * Name of FF parameter's property used to store torsion type
    * in MMFF torsion parameter
    */
   public final static String FFPRPROPMMFFTORTYP = "tt";

   /**
    * Name of FF parameter's property used to store MMFF bond charge increment
    */
   public final static String FFPRPROPMMFFBCI = "bci";

   /**
    * Name of FF parameter's property used to store bond type in MMFF bond
    * charge increment
    */
   public final static String FFPRPROPMMFFBCIBT = "bt";

   /**
    * Name of FF parameter's property used to store empirical bond charge
    * increment in MMFF empirical rule
    */
   public final static String FFPRPROPMMFFBCIP = "pbci";

   /**
    * Name of FF parameter's property used to store empirical factor FCADJ
    * setting bond charge increment in MMFF empirical rule
    */
   public final static String FFPRPROPMMFFBCIF = "fcadj";

   /**
    * Name of FF parameter's property used to store atom class property PILP
    * that are used in the empirical rules
    */
   public final static String FFPRPROPMMFFACPILP = "pilp";

   /**
    * Name of FF parameter's property used to store atom class property MLTB
    * that are used in the empirical rules
    */
   public final static String FFPRPROPMMFFACMLTB = "mltb";

   /**
    * Name of FF parameter's property used to store atom class property AROM
    * that are used in the empirical rules
    */
   public final static String FFPRPROPMMFFACAROM = "arom";

   /**
    * Name of FF parameter's property used to store atom class property LIN
    * that are used in the empirical rules
    */
   public final static String FFPRPROPMMFFACLIN = "lin";

   /**
    * Name of FF parameter's property used to store atom class property SBMB
    * that are used in the empirical rules
    */
   public final static String FFPRPROPMMFFACSBMB = "sbmb";

   /**
    * Name of FF parameter's property used to store atomatic atom type setting
    * with respect to ring size
    */
   public final static String FFPRPROPMMFFARMRS = "ringSize";

   /**
    * Name of FF parameter's property used to store atomatic atom type setting
    * with respect to fraction of charge to distribute
    */
   public final static String FFPRPROPMMFFARML = "L5";

   /**
    * Name of FF parameter's property used to store atomatic atom type setting
    * with respect to imidazol cation type of ring
    */
   public final static String FFPRPROPMMFFARMIM = "ImCation";

   /**
    * Name of FF parameter's property used to store atomatic atom type setting
    * with respect to 5-membered anion ring
    */
   public final static String FFPRPROPMMFFARMCP = "n5Anion";

   /**
    * Name of FF parameter's property used to store atomatic atom type setting
    * with respect to occurrence
    */
   public final static String FFPRPROPMMFFARMOR = "occurring";


//   /**
//    * Name of FF parameter's property used to store
//    */
//   public final static String FFPRPROPMMFF = "";


}
