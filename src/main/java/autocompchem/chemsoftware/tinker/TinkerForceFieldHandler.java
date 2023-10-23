package autocompchem.chemsoftware.tinker;

import java.io.File;

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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.openscience.cdk.AtomType;

import autocompchem.atom.AtomUtils;
import autocompchem.files.FileUtils;
import autocompchem.io.IOtools;
import autocompchem.modeling.forcefield.ForceConstant;
import autocompchem.modeling.forcefield.ForceFieldConstants;
import autocompchem.modeling.forcefield.ForceFieldParameter;
import autocompchem.modeling.forcefield.ForceFieldParamsSet;
import autocompchem.run.Terminator;


/**
 * Reader for Tinker force field files.
 * This class is only a quick and dirty fix of
 * the lack of a proper reader.
 *
 * WARNING: tested only with MMFF force field.
 * 
 * @author Marco Foscato
 */

public class TinkerForceFieldHandler
{

    /**
     * Verbosity level
     */
    private static int verbosity = 0;

//------------------------------------------------------------------------------

    /**
     * Read a Tinker ForceField file and produces the force field object.
     * @param file the file to read
     * @return the force field
     */

    public static ForceFieldParamsSet readFromFile(File file)
    {
        ForceFieldParamsSet ff = new ForceFieldParamsSet();

        // Add the 'any atom type' that is implicit in Tinker force field file
        AtomType anyAT = new AtomType("ANY","X");
        anyAT.setProperty(ForceFieldConstants.ATMTYPINT,"0");
        anyAT.setProperty(ForceFieldConstants.ATMCLSSTR,"0");
        anyAT.setProperty(ForceFieldConstants.ATMCLSINT,"0");
        anyAT.setProperty(ForceFieldConstants.ATMTYPSTR,"ANY");
        anyAT.setProperty(ForceFieldConstants.ATMTYPTXT,"ANY ATOM TYPE");
        ff.addAtomType(anyAT);
        for (String line : IOtools.readTXT(file))
        {
            String l = line.trim();
            if (l.equals("") || l.startsWith(TinkerConstants.FFKEYIGNORE))
            {
                continue;
            }
            String lUC = l.toUpperCase();
            String[] w = l.split("\\s+");
            String[] wUC = lUC.split("\\s+");

            if (verbosity > 2)
            {
                System.out.println("Reading line: " + l);
                System.out.println("Keyword: " + wUC[0]);
            }

            // select the type of line depending on the first keyword
            if (wUC[0].equals(TinkerConstants.FFKEYNAME))
            {
                ff.setName(w[1]);
                if (verbosity > 2)
                {
                    System.out.println("Force field name imported: " + w[1]);
                }
            }
            else if (wUC[0].equals(TinkerConstants.FFKEYATMTYP))
            {
                AtomType at = getAtomTypeFromLine(mergeAllButFirstWord(w));
                ff.addAtomType(at);
                if (verbosity > 2)
                {
                    System.out.println("AtomType imported: " + at);
                }
            }
            else if (TinkerConstants.FFKEYALLFFPAR.contains(wUC[0]))
            {
                ForceFieldParameter p = new ForceFieldParameter("noName",
                                                                        wUC[0]);
                try
                {
                    switch (wUC[0])
                    {
                        case TinkerConstants.FFKEYMMFFVDW:
                            p.setProperty(ForceFieldConstants.ATMCLSINT,
                                                        Integer.parseInt(w[1]));
                            p.addEquilibriumValue(Double.parseDouble(w[2]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFVDWA,
                                                      Double.parseDouble(w[3]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFVDWN,
                                                      Double.parseDouble(w[4]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFVDWG,
                                                      Double.parseDouble(w[5]));
                            if (w.length>6)
                            {
                                p.setProperty(TinkerConstants.FFPRPROPMMFFVDWDA,
                                                                          w[6]);
                            }
                            ff.addParameter(p);
                            break;

                        case TinkerConstants.FFKEYMHILVDW:
                            p.setProperty(ForceFieldConstants.ATMCLSINT,
                                                        Integer.parseInt(w[1]));
                            p.addEquilibriumValue(Double.parseDouble(w[2]));
                            p.addForceConstant(Double.parseDouble(w[3]));
                            ff.addParameter(p);
                            break;

                        case TinkerConstants.FFKEYMMFFSTR:
                            p.addAtomType(ff.getAtomClass(w[1]));
                            p.addAtomType(ff.getAtomClass(w[2]));
                            p.addForceConstant(Double.parseDouble(w[3]));
                            p.addEquilibriumValue(Double.parseDouble(w[4]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFSTRBT,
                                                        Integer.parseInt(w[5]));
                            ff.addParameter(p);
                            break;

                        case TinkerConstants.FFKEYMMFFEMPSTR:
                            p.setProperty(TinkerConstants.FFPRPROPMMFFEMPSTRNA,
                                                        Integer.parseInt(w[1]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFEMPSTRNB,
                                                        Integer.parseInt(w[2]));
                            p.addEquilibriumValue(Double.parseDouble(w[3]));
                            p.addForceConstant(Double.parseDouble(w[4]));
                            ff.addParameter(p);
                            break;

                        case TinkerConstants.FFKEYMMFFEMPRAD:
                            p.setProperty(TinkerConstants.FFPRPROPMMFFEMPAN,
                                                        Integer.parseInt(w[1]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFEMPRAD,
                                                      Double.parseDouble(w[2]));
                            if (w.length>3)
                            {
                                p.setProperty(TinkerConstants.FFPRPROPMMFFEMPEL,
                                                      Double.parseDouble(w[3]));
                            }
                            ff.addParameter(p);
                            break;

                        case TinkerConstants.FFKEYMMFFBND:
                            p.addAtomType(ff.getAtomClass(w[1]));
                            p.addAtomType(ff.getAtomClass(w[2]));
                            p.addAtomType(ff.getAtomClass(w[3]));
                            p.addForceConstant(Double.parseDouble(w[4]));
                            p.addEquilibriumValue(Double.parseDouble(w[5]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFANGTYP,
                                                        Integer.parseInt(w[6]));
                            ff.addParameter(p);
                            break;

                        case TinkerConstants.FFKEYMMFFSBN:
                            p.addAtomType(ff.getAtomClass(w[1]));
                            p.addAtomType(ff.getAtomClass(w[2]));
                            p.addAtomType(ff.getAtomClass(w[3]));
                            p.addForceConstant(Double.parseDouble(w[4]));
                            p.addForceConstant(Double.parseDouble(w[5]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFSBNTYP,
                                                        Integer.parseInt(w[6]));
                            ff.addParameter(p);
                            break;

                        case TinkerConstants.FFKEYMMFFDEFSBN:
                            p.setProperty(TinkerConstants.FFPRPROPMMFFEMPSTBIR,
                                                        Integer.parseInt(w[1]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFEMPSTBJR,
                                                        Integer.parseInt(w[2]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFEMPSTBKR,
                                                        Integer.parseInt(w[3]));
                            p.setProperty(
                                         TinkerConstants.FFPRPROPMMFFEMPSTBKIJK,
                                                      Double.parseDouble(w[4]));
                            p.setProperty(
                                         TinkerConstants.FFPRPROPMMFFEMPSTBKKJI,
                                                      Double.parseDouble(w[5]));
                            ff.addParameter(p);
                            break;

                        case TinkerConstants.FFKEYMMFFOOP:
                            p.addAtomType(ff.getAtomClass(w[1]));
                            p.addAtomType(ff.getAtomClass(w[2]));
                            p.addAtomType(ff.getAtomClass(w[3]));
                            p.addAtomType(ff.getAtomClass(w[4]));
                            p.addForceConstant(Double.parseDouble(w[5]));
                            ff.addParameter(p);
                            break;

                        case TinkerConstants.FFKEYMMFFTOR:
                            p.addAtomType(ff.getAtomClass(w[1]));
                            p.addAtomType(ff.getAtomClass(w[2]));
                            p.addAtomType(ff.getAtomClass(w[3]));
                            p.addAtomType(ff.getAtomClass(w[4]));
                            p.addForceConstant(Double.parseDouble(w[5]));
                            p.addForceConstant(Double.parseDouble(w[8]));
                            p.addForceConstant(Double.parseDouble(w[11]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFTORPH,
                                           new ArrayList<Double>(Arrays.asList(
                                                       Double.parseDouble(w[6]),
                                                       Double.parseDouble(w[9]),
                                                   Double.parseDouble(w[12]))));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFTORNS,
                                          new ArrayList<Integer>(Arrays.asList(
                                                       Integer.parseInt(w[7]),
                                                       Integer.parseInt(w[10]),
                                                     Integer.parseInt(w[13]))));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFTORTYP,
                                                                         w[14]);
                            ff.addParameter(p);
                            break;

                        case TinkerConstants.FFKEYMMFFBCI:
                            p.addAtomType(ff.getAtomClass(w[1]));
                            p.addAtomType(ff.getAtomClass(w[2]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFBCI,
                                                      Double.parseDouble(w[3]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFBCIBT,
                                                        Integer.parseInt(w[4]));
                            ff.addParameter(p);
                            break;

                        case TinkerConstants.FFKEYMMFFPBCI:
                            p.addAtomType(ff.getAtomClass(w[1]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFBCIP,
                                                      Double.parseDouble(w[2]));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFBCIF,
                                                      Double.parseDouble(w[3]));
                            ff.addParameter(p);
                            break;

                        case TinkerConstants.FFKEYMMFFATEQ:
                            AtomType ateq = ff.getAtomType(Integer.parseInt(
                                                                         w[6]));
                            // WARNING! In the list of AtomTypes only the first
                            // is actually an atom type, while the rest are 
                            // atom CLASSES represented by AtomType objects
                            p.addAtomType(ateq);
                            if (!w[1].equals(ateq.getProperty(
                                     ForceFieldConstants.ATMCLSINT).toString()))
                            {
                                Terminator.withMsgAndStatus("ERROR! "
                                    + "Inconsistent atom class '" + w[1] 
                                    + "' in atom class equivalency rule '" + l 
                                    + "'. Expected '" + ateq.getProperty(
                                      ForceFieldConstants.ATMCLSINT).toString()
                                    + "'",-1);
                            }
                            p.addAtomType(ff.getAtomClass(w[2]));
                            p.addAtomType(ff.getAtomClass(w[3]));
                            p.addAtomType(ff.getAtomClass(w[4]));
                            p.addAtomType(ff.getAtomClass(w[5]));
                            ff.addParameter(p);
                            break;

                        case TinkerConstants.FFKEYMMFFATPROP:
                            if (w.length < 10)
                            {
                                Terminator.withMsgAndStatus("ERROR! Unknown "
                                          + "syntax for MMFF propety line '"
                                          + l + "'.",-1); 
                            }
                            AtomType ac = new AtomType("X");
                            try 
                            {
                                ac = ff.getAtomClass(w[1]);
                            }
                            catch (Throwable t)
                            {
                                Terminator.withMsgAndStatus("ERROR! Atom class "
                                          + "'" + w[1] + "' is defined in the "
                                          + "MMFF properties section, but must "
                                          + "be first defined using the '"
                                          + TinkerConstants.FFKEYATMTYP + "' "
                                          + "keyword.",-1);  
                            }
                            ac.setAtomicNumber(Integer.parseInt(w[2]));
                            ac.setFormalNeighbourCount(
                                                        Integer.parseInt(w[3]));
                            ac.setValency(Integer.parseInt(w[4]));
                            ac.setProperty(TinkerConstants.FFPRPROPMMFFACPILP,
                                                                          w[5]);
                            ac.setProperty(TinkerConstants.FFPRPROPMMFFACMLTB,
                                                                          w[6]);
                            ac.setProperty(TinkerConstants.FFPRPROPMMFFACAROM,
                                                                          w[7]);
                            ac.setProperty(TinkerConstants.FFPRPROPMMFFACLIN,
                                                                          w[8]);
                            ac.setProperty(TinkerConstants.FFPRPROPMMFFACSBMB,
                                                                          w[9]);
                            break;

                        case TinkerConstants.FFKEYMMFFAROM:
                            p.addAtomType(ff.getAtomType(Integer.parseInt(
                                                                        w[1])));
                            p.setProperty(TinkerConstants.FFPRPROPMMFFARMRS,
                                                                          w[2]);
                            p.setProperty(TinkerConstants.FFPRPROPMMFFARML,
                                                                          w[3]);
                            p.setProperty(TinkerConstants.FFPRPROPMMFFARMIM,
                                                                          w[4]);
                            p.setProperty(TinkerConstants.FFPRPROPMMFFARMCP,
                                                                          w[5]);
                            p.setProperty(TinkerConstants.FFPRPROPMMFFARMOR,
                                                                          w[6]);
                            ff.addParameter(p);
                            break;

                        default:
                            Terminator.withMsgAndStatus("ERROR! Parsing of "
                                + "parameter '" + wUC[0]+ "' not implemented "
                                + "yet. Check TinkerForceFieldReader.", -1);
                    }
                }
                catch (Throwable t)
                {
                    t.printStackTrace();
                    Terminator.withMsgAndStatus("ERROR! Cannot import force "
                                + "field parameter from line '" + l + "'. ",-1);
                }
                if (verbosity > 2)
                {
                    System.out.println("Parameter imported: " + p);
                }
            }
            else
            {
                if (ff.hasProperty(w[0]) || ff.hasProperty(wUC[0]))
                {
                    Terminator.withMsgAndStatus("ERROR! Unexpected repetition "
                        + "of possible keyword '" + w[0] 
                        + "' in Tinker force field "
                        + "file. Please comment out (i.e., add '"
                        + TinkerConstants.FFKEYIGNORE + "' at the "
                        + "beginning of the line) all literature references, "
                        + "comments, and other lines that do not contain "
                        + "force field parameters, then try again. "
                        + "Alternatuvely, check the "
                        + "implementation of TinkerForceFieldReader.",-1);
                }
                ff.setProperty(w[0],mergeAllButFirstWord(w));
                if (verbosity > 2)
                {
                    System.out.println("Force field property imported: " 
                                                        + ff.getProperty(w[0]));
                }
            }

        }
        return ff;
    }

//------------------------------------------------------------------------------

    /**
     * Writes a Tinker force field file. The file must not exist.
     * @param ff the force field parameters set to be converted to a 
     * Tinker force field file
     * @param file the file to be written
     */

    public static void writeForceFieldFile(ForceFieldParamsSet ff, File file)
    {
        // WARNING! to reduce the memory requirement the text to write is
        // divided into blocks, each one is created, writted to file and cleared
        // Search for the IOtools.write... statements
        
        FileUtils.mustNotExist(file);

        // get utilities used below
        ArrayList<String> lines = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        String c = TinkerConstants.FFKEYIGNORE;
        String nl = System.getProperty("line.separator");

        // Generalities of the force field
        sb.append(c+nl);
        sb.append(c).append(" Tinker Force Field Definition").append(nl);
        sb.append(c).append(" Generated by AutoCompChem on ");
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        sb.append(dateFormat.format(date)).append(nl);
        sb.append(c+nl);
        sb.append(TinkerConstants.FFKEYNAME).append(" ");
        sb.append(ff.getName()).append(nl);

        // Special lines: constants and general parameters
        sb.append(nl);
        for (String pKey : ff.getPropertyKeys())
        {
            sb.append(pKey.toUpperCase()).append(" ").append(
                                    ff.getProperty(pKey).toString()).append(nl);
        }

        // Definition of Atom Types and Classes
        sb.append(nl);
        sb.append(c).append(" Atom Type Definitions").append(nl);
        sb.append("#    Typ. Class. RefName Comment Z Mass Valence").append(nl);
        for (Map.Entry<String,AtomType> e : ff.getAtomTypes().entrySet())
        {
            AtomType at = e.getValue();
            if (at.getProperty(ForceFieldConstants.ATMTYPINT).toString().equals(
                                                                           "0"))
            {
                continue;
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append(TinkerConstants.FFKEYATMTYP).append("   ").
               append(String.format(Locale.ENGLISH," %4s",
                     at.getProperty(ForceFieldConstants.ATMTYPINT).toString())).
               append(String.format(Locale.ENGLISH," %4s",
                     at.getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
               append(String.format(Locale.ENGLISH," %6s",
                     at.getProperty(ForceFieldConstants.ATMTYPSTR).toString())).
               append(String.format(Locale.ENGLISH," %30s",
                     at.getProperty(ForceFieldConstants.ATMTYPTXT).toString())).
               append(String.format(Locale.ENGLISH," %4s",at.getAtomicNumber())).
               append(String.format(Locale.ENGLISH," %7.3f",at.getExactMass())).
               append(String.format(Locale.ENGLISH," %3d",at.getValency()));
            lines.add(sb2.toString());
        }
        Collections.sort(lines);
        for (String l : lines)
        {
            sb.append(l).append(nl);
        }
        lines.clear();

        // Atom Class equivalencies
        if (ff.getParamTypes().contains(TinkerConstants.FFKEYMMFFATEQ))
        {
            sb.append(nl);
            sb.append("# Atom Class equivalencies").append(nl);
            sb.append("# 4*Atm.Classes  Atm.Type # Atm.Typ.Name  ").append(nl);
            for (ForceFieldParameter p : ff.getParamsOfType(
                                                 TinkerConstants.FFKEYMMFFATEQ))
            {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(TinkerConstants.FFKEYMMFFATEQ).append("   ");
                //The first is the atom type the rest are atom classes
                for (int i=1; i<p.getAtomTypes().size(); i++)
                {
                   AtomType atmCls = p.getAtomTypes().get(i);
                   sb2.append(String.format(Locale.ENGLISH,"%5s",atmCls.getProperty(
                                    ForceFieldConstants.ATMCLSSTR).toString()));
                }
                sb2.append(
                        String.format(Locale.ENGLISH,"%6s",p.getAtomTypes().get(0).getProperty(
                                    ForceFieldConstants.ATMTYPINT).toString())).
                    append(" # ").
                    append(
                        String.format(Locale.ENGLISH,"%6s",p.getAtomTypes().get(0).getProperty(
                                    ForceFieldConstants.ATMTYPSTR).toString()));
                lines.add(sb2.toString());
            }
            Collections.sort(lines);
            for (String l : lines)
            {
                sb.append(l).append(nl);
            }
            lines.clear();
        }       

        // Atom Class Properties
        sb.append(nl);
        sb.append("# Atom Class Properties").append(nl);
        sb.append("# class aspec crd val pilp mltb arom lin sbmb").append(nl);
        for (Map.Entry<String,AtomType> e : ff.getAtomClasses().entrySet())
        {
            AtomType atmCls = e.getValue();
            // Ignore the 'any' class
            if (atmCls.getProperty(
                          ForceFieldConstants.ATMCLSSTR).toString().equals("0"))
            {
                continue;
            }
            // Ignore classes that have do not have the properties
            Object pro = atmCls.getProperty(TinkerConstants.FFPRPROPMMFFACPILP);
            if (pro == null)
            {
                continue;
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append(TinkerConstants.FFKEYMMFFATPROP).append("   ").
               append(String.format(Locale.ENGLISH," %4s",atmCls.getProperty(
                                    ForceFieldConstants.ATMCLSSTR).toString())).
               append(String.format(Locale.ENGLISH," %4s",atmCls.getAtomicNumber())).
               append(String.format(Locale.ENGLISH," %4s",atmCls.getFormalNeighbourCount())).
               append(String.format(Locale.ENGLISH," %4s",atmCls.getProperty(
                               TinkerConstants.FFPRPROPMMFFACPILP).toString())).
               append(String.format(Locale.ENGLISH," %4s",atmCls.getProperty(
                               TinkerConstants.FFPRPROPMMFFACMLTB).toString())).
               append(String.format(Locale.ENGLISH," %4s",atmCls.getProperty(
                               TinkerConstants.FFPRPROPMMFFACAROM).toString())).
               append(String.format(Locale.ENGLISH," %4s",atmCls.getProperty(
                                TinkerConstants.FFPRPROPMMFFACLIN).toString())).
               append(String.format(Locale.ENGLISH," %4s",atmCls.getProperty(
                               TinkerConstants.FFPRPROPMMFFACSBMB).toString()));
            lines.add(sb2.toString());
        }
        Collections.sort(lines);
        for (String l : lines)
        {
            sb.append(l).append(nl);
        }
        lines.clear();

        //Properties of aromatic ionic rings
        if (ff.getParamTypes().contains(TinkerConstants.FFKEYMMFFATEQ))
        {
            sb.append(nl);
            sb.append("# Ionic Aromatic Rings").append(nl);
            sb.append("# AtmTyp RingSize l5 Imi(+) n5(-) occuring").append(nl);
            for (ForceFieldParameter p : ff.getParamsOfType(
                                                 TinkerConstants.FFKEYMMFFAROM))
            {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(TinkerConstants.FFKEYMMFFAROM).append("   ").
                   append(String.format(Locale.ENGLISH," %4s",
                                            p.getAtomTypes().get(0).getProperty(
                                    ForceFieldConstants.ATMTYPINT).toString())).
                   append(String.format(Locale.ENGLISH," %4s",p.getProperty(
                                TinkerConstants.FFPRPROPMMFFARMRS).toString())).
                   append(String.format(Locale.ENGLISH," %4s",p.getProperty(
                                 TinkerConstants.FFPRPROPMMFFARML).toString())).
                   append(String.format(Locale.ENGLISH," %4s",p.getProperty(
                                TinkerConstants.FFPRPROPMMFFARMIM).toString())).
                   append(String.format(Locale.ENGLISH," %4s",p.getProperty(
                                TinkerConstants.FFPRPROPMMFFARMCP).toString())).
                   append(String.format(Locale.ENGLISH," %4s",p.getProperty(
                                TinkerConstants.FFPRPROPMMFFARMOR).toString()));
                lines.add(sb2.toString());
            }
            Collections.sort(lines);
            for (String l : lines)
            {
                sb.append(l).append(nl);
            }
            lines.clear();
        }

        // Write this block into the file and clear tmp
        IOtools.writeTXTAppend(file,sb.toString(),false);
        sb = new StringBuilder();

        //Van der Vaals
        if (ff.getParamTypes().contains(TinkerConstants.FFKEYMMFFVDW))
        {
            sb.append(nl);
            sb.append("# van der Waals properties").append(nl);
            sb.append("# atmCls R*ii alpha-i N-i G-i da").append(nl);
            for (ForceFieldParameter p : ff.getParamsOfType(
                                                  TinkerConstants.FFKEYMMFFVDW))
            {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(TinkerConstants.FFKEYMMFFVDW).append("   ").
                   append(String.format(Locale.ENGLISH," %4s",p.getProperty(
                                    ForceFieldConstants.ATMCLSINT).toString())).
                   append(String.format(Locale.ENGLISH," %13.9f",
                                            p.getEqValues().get(0).getValue())).
                   append(String.format(Locale.ENGLISH," %7.3f",p.getProperty(
                                 TinkerConstants.FFPRPROPMMFFVDWA))).
                   append(String.format(Locale.ENGLISH," %7.3f",p.getProperty(
                                 TinkerConstants.FFPRPROPMMFFVDWN))).
                   append(String.format(Locale.ENGLISH," %7.3f",p.getProperty(
                                 TinkerConstants.FFPRPROPMMFFVDWG)));
                   if (p.getProperty(TinkerConstants.FFPRPROPMMFFVDWDA) != null)
                   {
                       sb2.append(String.format(Locale.ENGLISH," %3s",p.getProperty(
                                TinkerConstants.FFPRPROPMMFFVDWDA).toString()));
                   }
                lines.add(sb2.toString());
            }
            Collections.sort(lines);
            for (String l : lines)
            {
                sb.append(l).append(nl);
            }
            lines.clear();
        }
        if (ff.getParamTypes().contains(TinkerConstants.FFKEYMHILVDW))
        {
            sb.append(nl);
            sb.append("# Multihapto Inter Ligand LJ-style").append(nl);
            sb.append("# atmCls r eps").append(nl);
            for (ForceFieldParameter p : ff.getParamsOfType(
                                                  TinkerConstants.FFKEYMHILVDW))
            {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(TinkerConstants.FFKEYMHILVDW).append("   ").
                   append(String.format(Locale.ENGLISH," %4s",p.getProperty(
                                    ForceFieldConstants.ATMCLSINT).toString())).
                   append(String.format(Locale.ENGLISH," %7.3f",
                                            p.getEqValues().get(0).getValue())).
                   append(String.format(Locale.ENGLISH," %7.3f",
                                      p.getForceConstants().get(0).getValue()));
                lines.add(sb2.toString());
            }
            Collections.sort(lines);
            for (String l : lines)
            {
                sb.append(l).append(nl);
            }
            lines.clear();
        }

        // Append this block to the file and clear tmp
        IOtools.writeTXTAppend(file,sb.toString(),true);
        sb = new StringBuilder();

        // Bond stretching and related parameters
        if (ff.getParamTypes().contains(TinkerConstants.FFKEYMMFFSTR))
        {
            sb.append(nl);
            sb.append("# Bond stretching parameters").append(nl);
            sb.append("# Cls1 Cls2 kb r0 bt ").append(nl);
            for (ForceFieldParameter p : ff.getParamsOfType(
                                                  TinkerConstants.FFKEYMMFFSTR))
            {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(TinkerConstants.FFKEYMMFFSTR).append("   ").
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(0).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(1).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %7.3f",
                                      p.getForceConstants().get(0).getValue())).
                   append(String.format(Locale.ENGLISH," %7.3f",
                                            p.getEqValues().get(0).getValue())).
                   append(String.format(Locale.ENGLISH," %4d",p.getProperty(
                                TinkerConstants.FFPRPROPMMFFSTRBT)));
                lines.add(sb2.toString());
            }
            Collections.sort(lines);
            for (String l : lines)
            {
                sb.append(l).append(nl);
            }
            lines.clear();
        }
        if (ff.getParamTypes().contains(TinkerConstants.FFKEYMMFFEMPSTR))
        {
            sb.append(nl);
            sb.append("# Empirical rules for bond stretching").append(nl);
            sb.append("# AtmNum1 ATmNum2 r0-ref kb-ref").append(nl);
            for (ForceFieldParameter p : ff.getParamsOfType(
                                               TinkerConstants.FFKEYMMFFEMPSTR))
            {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(TinkerConstants.FFKEYMMFFEMPSTR).append("   ").
                   append(String.format(Locale.ENGLISH," %4d",p.getProperty(
                                TinkerConstants.FFPRPROPMMFFEMPSTRNA))).
                   append(String.format(Locale.ENGLISH," %4d",p.getProperty(
                                TinkerConstants.FFPRPROPMMFFEMPSTRNB))).
                   append(String.format(Locale.ENGLISH," %7.3f",
                                            p.getEqValues().get(0).getValue())).
                   append(String.format(Locale.ENGLISH," %7.3f",
                                      p.getForceConstants().get(0).getValue()));
                lines.add(sb2.toString());
            }
            Collections.sort(lines);
            for (String l : lines)
            {
                sb.append(l).append(nl);
            }
            lines.clear();
        }
        if (ff.getParamTypes().contains(TinkerConstants.FFKEYMMFFEMPRAD))
        {
            sb.append(nl);
            sb.append("# Empirical rules for bond stretching").append(nl);
            sb.append("# AtmNum CovRad PaulingElectroneg").append(nl);
            for (ForceFieldParameter p : ff.getParamsOfType(
                                               TinkerConstants.FFKEYMMFFEMPRAD))
            {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(TinkerConstants.FFKEYMMFFEMPRAD).append("   ").
                   append(String.format(Locale.ENGLISH," %4d",p.getProperty(
                                           TinkerConstants.FFPRPROPMMFFEMPAN))).
                   append(String.format(Locale.ENGLISH," %6.3f",p.getProperty(
                                          TinkerConstants.FFPRPROPMMFFEMPRAD)));
                if (p.getProperty(TinkerConstants.FFPRPROPMMFFEMPEL) != null)
                {
                    sb2.append(String.format(Locale.ENGLISH," %6.3f",p.getProperty(
                                           TinkerConstants.FFPRPROPMMFFEMPEL)));
                }
                lines.add(sb2.toString());
            }
            Collections.sort(lines);
            for (String l : lines)
            {
                sb.append(l).append(nl);
            }
            lines.clear();
        }

        // Append this block to the file and clear tmp
        IOtools.writeTXTAppend(file,sb.toString(),true);
        sb = new StringBuilder();

        // Angle bending and related parameters
        if (ff.getParamTypes().contains(TinkerConstants.FFKEYMMFFBND))
        {
            sb.append(nl);
            sb.append("# Angle bending parameters").append(nl);
            sb.append("# Cls1 Cls2 Cls3 ka theta0 at ").append(nl);
            for (ForceFieldParameter p : ff.getParamsOfType(
                                                  TinkerConstants.FFKEYMMFFBND))
            {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(TinkerConstants.FFKEYMMFFBND).append("   ").
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(0).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(1).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(2).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %7.3f",
                                      p.getForceConstants().get(0).getValue())).
                   append(String.format(Locale.ENGLISH," %8.3f",
                                            p.getEqValues().get(0).getValue())).
                   append(String.format(Locale.ENGLISH," %4d",p.getProperty(
                                          TinkerConstants.FFPRPROPMMFFANGTYP)));
                lines.add(sb2.toString());
            }
            Collections.sort(lines);
            for (String l : lines)
            {
                sb.append(l).append(nl);
            }
            lines.clear();
        }

        // Append this block to the file and clear tmp
        IOtools.writeTXTAppend(file,sb.toString(),true);
        sb = new StringBuilder();

        // Stretch-bend and related parameters
        if (ff.getParamTypes().contains(TinkerConstants.FFKEYMMFFSBN))
        {
            sb.append(nl);
            sb.append("# Stretch-bend  parameters").append(nl);
            sb.append("# Cls1 Cls2 Cls3 kbsIJK kbsKJI sbt").append(nl);
            for (ForceFieldParameter p : ff.getParamsOfType(
                                                  TinkerConstants.FFKEYMMFFSBN))
            {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(TinkerConstants.FFKEYMMFFSBN).append("   ").
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(0).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(1).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(2).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %7.3f",
                                      p.getForceConstants().get(0).getValue())).
                   append(String.format(Locale.ENGLISH," %7.3f",
                                      p.getForceConstants().get(1).getValue())).
                   append(String.format(Locale.ENGLISH," %4d",p.getProperty(
                                          TinkerConstants.FFPRPROPMMFFSBNTYP)));
                lines.add(sb2.toString());
            }
            Collections.sort(lines);
            for (String l : lines)
            {
                sb.append(l).append(nl);
            }
            lines.clear();
        }
        if (ff.getParamTypes().contains(TinkerConstants.FFKEYMMFFDEFSBN))
        {
            sb.append(nl);
            sb.append("# Epirical rules for stretch-bend params").append(nl);
            sb.append("# PTabRow1 PTabRow2 PTabRow3 kbaIJK kbaKJI").append(nl);
            for (ForceFieldParameter p : ff.getParamsOfType(
                                            TinkerConstants.FFKEYMMFFDEFSBN))
            {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(TinkerConstants.FFKEYMMFFDEFSBN).append("   ").
                   append(String.format(Locale.ENGLISH," %4d",p.getProperty(
                                        TinkerConstants.FFPRPROPMMFFEMPSTBIR))).
                   append(String.format(Locale.ENGLISH," %4d",p.getProperty(
                                        TinkerConstants.FFPRPROPMMFFEMPSTBJR))).
                   append(String.format(Locale.ENGLISH," %4d",p.getProperty(
                                        TinkerConstants.FFPRPROPMMFFEMPSTBKR))).
                   append(String.format(Locale.ENGLISH," %7.3f",p.getProperty(
                                      TinkerConstants.FFPRPROPMMFFEMPSTBKIJK))).
                   append(String.format(Locale.ENGLISH," %7.3f",p.getProperty(
                                      TinkerConstants.FFPRPROPMMFFEMPSTBKKJI)));
                lines.add(sb2.toString());
            }
            Collections.sort(lines);
            for (String l : lines)
            {
                sb.append(l).append(nl);
            }
            lines.clear();
        }

        // Append this block to the file and clear tmp
        IOtools.writeTXTAppend(file,sb.toString(),true);
        sb = new StringBuilder();

        // Out-of-plane bending and related parameters
        if (ff.getParamTypes().contains(TinkerConstants.FFKEYMMFFOOP))
        {
            sb.append(nl);
            sb.append("# Out-of-plane bending parameters").append(nl);
            sb.append("# ClsI ClsJ ClsK ClsL  koop").append(nl);
            for (ForceFieldParameter p : ff.getParamsOfType(
                                                  TinkerConstants.FFKEYMMFFOOP))
            {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(TinkerConstants.FFKEYMMFFOOP).append("   ").
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(0).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(1).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(2).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(3).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %7.3f",
                                      p.getForceConstants().get(0).getValue()));
                lines.add(sb2.toString());
            }
            Collections.sort(lines);
            for (String l : lines)
            {
                sb.append(l).append(nl);
            }
            lines.clear();
        }

        // Append this block to the file and clear tmp
        IOtools.writeTXTAppend(file,sb.toString(),true);
        sb = new StringBuilder();

        // Bond torsion and related parameters
        if (ff.getParamTypes().contains(TinkerConstants.FFKEYMMFFTOR))
        {
            sb.append(nl);
            sb.append("# Bond torsion parameters").append(nl);
            sb.append("# ClsI ClsJ ClsK ClsL n(ktor phas mult) typ").append(nl);
            for (ForceFieldParameter p : ff.getParamsOfType(
                                                  TinkerConstants.FFKEYMMFFTOR))
            {
                StringBuilder sb2 = new StringBuilder();
                ArrayList<ForceConstant> kt = p.getForceConstants();
                @SuppressWarnings("unchecked")
                ArrayList<Double> ph = (ArrayList<Double>) p.getProperty(
                                             TinkerConstants.FFPRPROPMMFFTORPH);
                @SuppressWarnings("unchecked")
                ArrayList<Integer> ns = (ArrayList<Integer>) p.getProperty(
                                             TinkerConstants.FFPRPROPMMFFTORNS);
                sb2.append(TinkerConstants.FFKEYMMFFTOR).append("   ").
                   append(String.format(Locale.ENGLISH,"%4s",p.getAtomTypes().get(0).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH,"%4s",p.getAtomTypes().get(1).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH,"%4s",p.getAtomTypes().get(2).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH,"%4s",p.getAtomTypes().get(3).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString()));
                for (int ik=0;
                     ik<Math.min(Math.min(kt.size(),ph.size()),ns.size()); ik++)
                {
                    sb2.append(String.format(Locale.ENGLISH," %7.3f",kt.get(ik).getValue())).
                                     append(String.format(Locale.ENGLISH," %.0f",ph.get(ik))).
                                       append(String.format(Locale.ENGLISH," %d",ns.get(ik)));
                }
                sb2.append(String.format(Locale.ENGLISH,"  %s",p.getProperty(
                                          TinkerConstants.FFPRPROPMMFFTORTYP)));
                lines.add(sb2.toString());
            }
            Collections.sort(lines);
            for (String l : lines)
            {
                sb.append(l).append(nl);
            }
            lines.clear();
        }

        // Append this block to the file and clear tmp
        IOtools.writeTXTAppend(file,sb.toString(),true);
        sb = new StringBuilder();

        // Charge-related parameters
        if (ff.getParamTypes().contains(TinkerConstants.FFKEYMMFFBCI))
        {
            sb.append(nl);
            sb.append("# Bond Charge Increments").append(nl);
            sb.append("# Cls1 Cls2 bci bt").append(nl);
            for (ForceFieldParameter p : ff.getParamsOfType(
                                                  TinkerConstants.FFKEYMMFFBCI))
            {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(TinkerConstants.FFKEYMMFFBCI).append("   ").
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(0).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(1).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %8.4f",p.getProperty(
                                          TinkerConstants.FFPRPROPMMFFBCI))).
                   append(String.format(Locale.ENGLISH," %4d",p.getProperty(
                                          TinkerConstants.FFPRPROPMMFFBCIBT)));
                lines.add(sb2.toString());
            }
            Collections.sort(lines);
            for (String l : lines)
            {
                sb.append(l).append(nl);
            }
            lines.clear();
        }
        if (ff.getParamTypes().contains(TinkerConstants.FFKEYMMFFPBCI))
        {
            sb.append(nl);
            sb.append("# Empirical rule paramd for missing BCI").append(nl);
            sb.append("# Cls pbci fcadj").append(nl);
            for (ForceFieldParameter p : ff.getParamsOfType(
                                                 TinkerConstants.FFKEYMMFFPBCI))
            {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(TinkerConstants.FFKEYMMFFPBCI).append("   ").
                   append(String.format(Locale.ENGLISH," %4s",p.getAtomTypes().get(0).
                        getProperty(ForceFieldConstants.ATMCLSSTR).toString())).
                   append(String.format(Locale.ENGLISH," %8.4f",p.getProperty(
                                          TinkerConstants.FFPRPROPMMFFBCIP))).
                   append(String.format(Locale.ENGLISH," %8.4f",p.getProperty(
                                          TinkerConstants.FFPRPROPMMFFBCIF)));
                lines.add(sb2.toString());
            }
            Collections.sort(lines);
            for (String l : lines)
            {
                sb.append(l).append(nl);
            }
            lines.clear();
        }

        // Append this block to the file and clear tmp
        IOtools.writeTXTAppend(file,sb.toString(),true);
        sb = new StringBuilder();
    }

//------------------------------------------------------------------------------

    /**
     * Returns a string with all but the first entry of the array
     */
    
    private static String mergeAllButFirstWord(String[] lst)
    {
        StringBuilder sb = new StringBuilder();
        for (int i=1; i<lst.length; i++)
        {
            if (i<(lst.length))
            {
                sb.append(" ");
            }
            sb.append(lst[i]);
        }
        return sb.toString();
    }
 
//------------------------------------------------------------------------------

   /**
    * Converts Tinker FF file atom type definition into an AtomType object.
    */

    private static AtomType getAtomTypeFromLine(String line)
    {
        String msg = "ERROR! Unexpected syntax in atom type "
                     + "definition '" + line + "'. I was expecting "
                     + "'INT INT STRING \"doubleQuotedSTRING\" INT DOUBLE INT'";
        int part = 0;
        AtomType at = new AtomType("X");
        try
        {
            String[] p = line.trim().split("\"");
            String line2 = p[0]+"\""+p[1].replaceAll("\\s+","_@_")+"\""+p[2];
            String[] w = line2.split("\\s+");
            part++;
            at = new AtomType(w[0],
                          AtomUtils.getElementalSymbol(Integer.parseInt(w[4])));
            part++;
            at.setProperty(ForceFieldConstants.ATMTYPINT,w[0]);
            part++;
            at.setProperty(ForceFieldConstants.ATMCLSSTR,w[1]);
            part++;
            at.setProperty(ForceFieldConstants.ATMCLSINT,w[1]);
            part++;
            at.setProperty(ForceFieldConstants.ATMTYPSTR,w[2]);
            part++;
            at.setProperty(ForceFieldConstants.ATMTYPTXT,
                                                    w[3].replaceAll("_@_"," "));
            part++;
            at.setAtomicNumber(Integer.parseInt(w[4]));
            part++;
            at.setExactMass(Double.parseDouble(w[5]));
            part++;
            at.setValency(Integer.parseInt(w[6]));
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            Terminator.withMsgAndStatus(msg + " Chack part: " + part,-1);
        }
        return at;
    }

//------------------------------------------------------------------------------

}
