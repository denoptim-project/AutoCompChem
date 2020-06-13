package autocompchem.chemsoftware.qmmm;

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
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;

import autocompchem.io.*;
import autocompchem.run.Terminator;

/**
 * Object representing a single QMMM job. Currently QMMM can perform only single
 * single-step jobs.
 * An <code>QMMMJob</code> can be constructed from
 * a text file (the so-called JOBDETAILS file).
 * <p>
 * The general structure of a QMMM ".dat" file is as follows:</p>
 * <pre>
 * 1st_SECTION_NAME 
 *   switch1
 *   keyword1 valueOfKeyword1
 *   keyword2 valueForKeyword2
 *   SUBSECTION1 
 *      data1.1
 *      data1.2
 *   END
 *   SUBSECTION2
 *      switch2.1
 *      keyword2.1 valueOfKeyword2.1
 *      SUBSECTION2.1
 *         data2.1
 *         data2.2
 *         data3.1
 *      END
 *   END
 * 
 * 2nd_SECTION
 *   swhitch
 *   keyword
 *   ...
 * </pre>
 * According to this formalism the JOBDETAILS file 
 * adheres to a syntax that allows a
 * combined human- and computer-readable access to specific information.
 * Each single piece of information (i.e., the method to be used in the QM
 * calculation, for example, HF) is to be provided as follows:
 * <br><br>Syntax:<br><code>
 * {@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABSECTION}sectionName
 * &lt;typeLabel&gt;&lt;content&gt;
 * </code><br>
 * Example (ignore double quotes; note that the <code>QMKey</code> section is 
 * a subsection of <code>*QM/MM</code>):<br><code>
 * {@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABSECTION}{@value autocompchem.chemsoftware.qmmm.QMMMConstants#QMMMSEC} {@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABSECTION}QMKey {@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABLOUDKEY}method{@value autocompchem.chemsoftware.qmmm.QMMMConstants#KEYVALSEPARATOR}HF</code>
 * <br><br>
 * where &lt;typeLabel&gt; can be
 * <ul>
 * <li>{@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABMUTEKEY} for a
 * "mute" keyword, that is, a "<code>keyword value</code>" paired information
 * where only <code>value</code> is to be
 * written in the QMMM input file, while the <code>keyword</code> is only used
 * by AutoCompChem to identify the bit of information.
 * The {@value autocompchem.chemsoftware.qmmm.QMMMConstants#KEYVALSEPARATOR}
 * sign (without double quotes) is used as separator between
 * <code>typeLabel</code> and <code>content</code> in the JOBDETAILS syntax.
 * The result is:
 * <code>
 * {@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABMUTEKEY}&lt;keyword&gt;{@value autocompchem.chemsoftware.qmmm.QMMMConstants#KEYVALSEPARATOR}&lt;value&gt;
 * </code>
 * </li>
 * <li>{@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABLOUDKEY} for a
 * "loud" keyword, that is, a "<code>keyword value</code>" paired information
 * where both <code>keyword</code> and <code>value</code> are
 * written in the QMMM input file.
 * The {@value autocompchem.chemsoftware.qmmm.QMMMConstants#KEYVALSEPARATOR}
 * sign (without double quotes) is used as separator between
 * <code>typeLabel</code> and <code>content</code> in the JOBDETAILS syntax.
 * The result is:
 * <code>
 * {@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABLOUDKEY}&lt;keyword&gt;{@value autocompchem.chemsoftware.qmmm.QMMMConstants#KEYVALSEPARATOR}&lt;value&gt;
 * </code>
 * </li>
 * <li>{@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABDATA} for a
 * generic block of text-like information identified by a reference name. The
 * reference name is used only by AutoCompChem and it's not written in QMMM
 * input file.
 * The {@value autocompchem.chemsoftware.qmmm.QMMMConstants#DATAVALSEPARATOR}
 * sign (without double quotes) is used as separator betweem 
 * the reference name from
 * the actual data (i.e., text that is going to be written in the QMMM input)
 * in the JOBDETAILS syntax.
 * The result is:
 * <code>
 * {@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABDATA}&lt;refName&gt;{@value autocompchem.chemsoftware.qmmm.QMMMConstants#DATAVALSEPARATOR}&lt;data&gt;
 * </code>
 * </li>
 * <li>{@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABSECTION} for e
 * sub-section. Sub-sections are defined with the very same syntax
 * independently on their level (i.e., recursion).
 * </li>
 * </ul>
 * The value of keywords or the section data can be specified in more than one
 * line of text using the labels
 * {@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABOPENBLOCK} and
 * {@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABCLOSEBLOCK} to begin
 * and end the multi line block of information.
 * For example (all without double quotes):
 * <br><br>
 * <pre>
 * {@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABSECTION}basis {@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABDATA}basisSetData{@value autocompchem.chemsoftware.qmmm.QMMMConstants#DATAVALSEPARATOR}{@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABOPENBLOCK} oxygen library cc-pvdz
 * hydrogen library cc-pvdz
 * nitrogen library cc-pvdz
 * {@value autocompchem.chemsoftware.qmmm.QMMMConstants#LABCLOSEBLOCK}</pre>
 *
 * @author Marco Foscato
 */

public class QMMMJob
{

    /**
     * List of sections of the input file
     */
    private ArrayList<QMMMSection> sections;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty QMMM job
     */

    public QMMMJob()
    {
        sections = new ArrayList<QMMMSection>();
    }

//------------------------------------------------------------------------------

    /**
     * Construct a QMMM Job object from a formatted file (JobDetails) with
     * instructions and parameters for the whole calculation. 
     * @param inFile formatted JobDetails' file to be read
     */

    public QMMMJob(String inFile)
    {
        this(IOtools.readTXT(inFile));
    }

//------------------------------------------------------------------------------

    /**
     * Construct a QMMMJob object from a formatted text divided
     * in lines.
     * @param lines array of lines to be read
     */

    public QMMMJob(ArrayList<String> lines)
    {
        // Split text by section
        Map<String,ArrayList<String>> secsAsTxt = 
                                        new HashMap<String,ArrayList<String>>();
        Iterator<String> it = lines.iterator();
        while (it.hasNext())
        {
            String line = it.next();

            //Skip if not a section
            if (!line.toUpperCase().startsWith(QMMMConstants.LABSECTION))
            {
                continue;
            }

            //Get section name
            line = line.substring(QMMMConstants.LABSECTION.length());
            String[] parts = line.trim().split("\\s+",2);
            String secName = parts[0].toUpperCase();
            String secContent = "";
            if (parts.length > 1)
            {
                secContent = parts[1];
            }

            //Deal with multiline blocks
            if (line.contains(QMMMConstants.LABOPENBLOCK) 
                && !line.contains(QMMMConstants.LABCLOSEBLOCK))
            {
                boolean blockIsOpen = true;
                while (it.hasNext())
                {
                    String innerLine = it.next();
                    if (innerLine.toUpperCase().contains(
                                                 QMMMConstants.LABCLOSEBLOCK))
                    {
                        blockIsOpen = false;
                        break;
                    }
                    secContent = secContent 
                                 + System.getProperty("line.separator")
                                 + innerLine;
                }
                if (blockIsOpen)
                {
                    Terminator.withMsgAndStatus("ERROR! Multiline block opened "
                                       + "but never closed. Opened block is: '"
				       + secContent + "'",-1);
                }
            }    

            //Collect lines of each section
            if (secsAsTxt.containsKey(secName))
            {
                secsAsTxt.get(secName).add(secContent);
            }
            else
            {
                ArrayList<String> secLines = new ArrayList<String>();
                secLines.add(secContent);
                secsAsTxt.put(secName,secLines);
            }
        }

        //Make and store sections
        sections = new ArrayList<QMMMSection>();
        for (String dKey : secsAsTxt.keySet())
        {
            QMMMSection sec = new QMMMSection(dKey,secsAsTxt.get(dKey));
            sections.add(sec);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Set the title, which is stored in a dedicated list
     * @param title the new title
     */

    public void setTitle(String title)
    {
        ArrayList<String> secPath = new ArrayList<String>(Arrays.asList(
                                                    QMMMConstants.MULTIGENSEC));
        ArrayList<String> lines = new ArrayList<String>();
        String[] words = title.split(System.getProperty("\\s+"));
        int totLen = 2 * (QMMMConstants.SUBSECTIONINDENT.length());
        String line = "";
        for (int i=0; i<words.length; i++)
        {
            String word = words[i];
            if ((totLen + word.length() + 1) < QMMMConstants.MAXLINELENGTH)
            {
                line = line + " " + word;
            }
            else
            {
                lines.add(line);
                totLen = 2 * (QMMMConstants.SUBSECTIONINDENT.length()) 
								+ word.length();
                line = word;
            }
        }

	if (lines.size() > QMMMConstants.MAXTITLELINES)
	{
	    Terminator.withMsgAndStatus("ERROR! Title is too long (more than "
					+ QMMMConstants.MAXTITLELINES 
					+ " lines); use a shorter title",-1);
	}

        QMMMList lst = new QMMMList(QMMMConstants.TITLELIST,lines);
        setList(secPath,lst);
    } 

//------------------------------------------------------------------------------

    /**
     * Set the total charge of the system
     * @param charge the total charge
     */

     public void setCharge(int charge)
     {
        ArrayList<String> secPath = new ArrayList<String>(Arrays.asList(
                                                    QMMMConstants.MULTIGENSEC));
        QMMMKeyword k = new QMMMKeyword(QMMMConstants.CHARGEKEY,true,
                new ArrayList<String>(Arrays.asList(Integer.toString(charge))));
        setKeyword(secPath,k);
     }

//------------------------------------------------------------------------------

    /**
     * Set the spin multiplicity.
     * @param sm the spin multiplicity
     */

     public void setSpinMultiplicity(int sm)
     {
        ArrayList<String> secPath = new ArrayList<String>(Arrays.asList(
                                                    QMMMConstants.MULTIGENSEC));
        QMMMKeyword k = new QMMMKeyword(QMMMConstants.SPINMLTKEY,true,
                    new ArrayList<String>(Arrays.asList(Integer.toString(sm))));
        setKeyword(secPath,k);
    }

//------------------------------------------------------------------------------

    /**
     * Set the number of atoms
     * @param n the total number of atoms (might include the link atoms)
     */

     public void setNatoms(int n)
     {
        ArrayList<String> secPath = new ArrayList<String>(Arrays.asList(
                                                    QMMMConstants.MULTIGENSEC));
        QMMMKeyword k = new QMMMKeyword(QMMMConstants.NATOMSKEY,true,
                    new ArrayList<String>(Arrays.asList(Integer.toString(n))));
        setKeyword(secPath,k);
    }

//------------------------------------------------------------------------------

    /**
     * Sets a section defining its content: keywords, subsections, lists.
     * This method can be used to create sections that are sub-sections with
     * a specific path.
     * @param secPath the path to the section to edit. Section's names
     * are read so that entry 0 is the name of outermost section. The name 
     * of the section to create or alter <b>must not be listed</b> 
     * in the path.
     * @param newDir the section to create or overwrite.
     * @param owKeys if <code>true</code> makes this method overwrite the 
     * keywords of the existing section. If the section doe not exist, this
     * flag has no effect.
     * @param owSubDirs if <code>true</code> makes this method overwrite the
     * sub-sections of the existing section. 
     * If the section does not exist, this flag has no effect
     * flag has no effect.
     * @param owList if <code>true</code> makes this method overwrite the
     * list of data in the existing section. If the section doesn't exist, this
     * flag has no effect.  
     */

    public void setSection(ArrayList<String> secPath, QMMMSection newDir,
                              boolean owKeys, boolean owSubDirs, boolean owList)
    {
        QMMMSection parDir = null;
        for (int i=0; i<secPath.size(); i++)
        {
            // Get the proper section
            String parDirName = secPath.get(i);
            if (i == 0)
            {
                parDir = getSection(parDirName);
                if (parDir == null)
                {
                    parDir = new QMMMSection(parDirName);
                    sections.add(parDir);
                }
            }
            else
            {
                parDir = parDir.getSubSection(parDirName);
            }

            // prevent loops of sections
            if (parDir.equals(newDir))
            {
                Terminator.withMsgAndStatus("ERROR! You cannot make section '"
                        + newDir.getName() + "' a subsection of itself or "
                        + "of any of its descendent subsections. "
                        + "Check section path '"
                        + secPath + "'.",-1);
            }
        }

        QMMMSection oldDir = parDir.getSubSection(newDir.getName());
        if (oldDir == null)
        {
            parDir.addSubSection(newDir);
        }
        else
        {
            if (owKeys)
            {
                oldDir.setAllKeywords(newDir.getAllKeywords());
            }
            if (owSubDirs)
            {
                oldDir.setAllSubSections(newDir.getAllSubSections());
            }
            if (owList)
            {
                oldDir.setAllLists(newDir.getAllLists());
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Sets a keyword belonging to a specific section or subsection.
     * @param secPath the path to the section to edit. Section's names
     * are read so that entry 0 is the name of outermost section.
     * @param kw the keyword to set or overwrite
     */

    public void setKeyword(ArrayList<String> secPath, QMMMKeyword kw)
    {
        QMMMSection sec = null;
        for (int i=0; i<secPath.size(); i++)
        {
            // Get the proper section
            String secName = secPath.get(i);
            if (i == 0)
            {
                sec = getSection(secName);
                if (sec == null)
                {
                    sec = new QMMMSection(secName);
                    sections.add(sec);
                }
            }
            else
            {
                sec = sec.getSubSection(secName);
            }
        }

        sec.addKeyword(kw);
    }

//------------------------------------------------------------------------------

    /**
     * Sets a list of data belonging to a specific section or subsection.
     * @param secPath the path to the section to edit. Section's names
     * are read so that entry 0 is the name of outermost section.
     * @param list the list to set or overwrite
     */

    public void setList(ArrayList<String> secPath, QMMMList list)
    {
        QMMMSection sec = null;
        for (int i=0; i<secPath.size(); i++)
        {
            // Get the proper section
            String secName = secPath.get(i);
            if (i == 0)
            {
                sec = getSection(secName);
                if (sec == null)
                {
                    sec = new QMMMSection(secName);
                    sections.add(sec);
                }
            }
            else
            {
                sec = sec.getSubSection(secName);
            }
        }

        sec.addList(list);
    }

//------------------------------------------------------------------------------

    /**
     * Get a section of this task.
     * @param name the name of the section to search
     * @return the section with the given name or <code>null</code> if no 
     * such directory is found.
     */

    public QMMMSection getSection(String name)
    {
        for (QMMMSection sec : sections)
        {
            if (sec.getName().equals(name.toUpperCase()))
            {
                return sec;
            }
        }
        return null;
    }

//------------------------------------------------------------------------------

    /**
     * Return the theoretical model (uppercase). Default is "QM"
     * @return the content of the theoretical model keyword
     */

    public String getModel()
    {
        QMMMSection multgen = getSection(QMMMConstants.MULTIGENSEC);
        if (multgen == null)
        {
            Terminator.withMsgAndStatus("ERROR! " + QMMMConstants.MULTIGENSEC 
				     + " section not found in the QMMMJob. "
                                     + "The " + QMMMConstants.MULTIGENSEC
				     + " section must be defined.",-1);
        }

        //Default 
        String model = "QM";
        QMMMKeyword modelKey = multgen.getKeyword(QMMMConstants.MODELKEY);
        if (modelKey != null)
        {
            model = modelKey.getValue().get(0).toUpperCase();
        }

        return model;
    }

//------------------------------------------------------------------------------

    /**
     * Return the type of embedding model (uppercase). Default is "NONE"
     * @return the content of the type of embedding keyword
     */

    public String getEmbedding()
    {
        QMMMSection qmmmSec = getSection(QMMMConstants.QMMMSEC);
        if (qmmmSec == null)
        {
	    return "NONE";
        }

        //Default
        String type = "ELECTRIC";
        QMMMKeyword embedKey = qmmmSec.getKeyword(QMMMConstants.EMBEDDINGKEY);
        if (embedKey != null)
        {
            type = embedKey.getValue().get(0).toUpperCase();
        }

        return type;
	}

//------------------------------------------------------------------------------

    /**
     * Test recursive alteration of sections, keywords, and lists of data.
     * This method is intended for testing part of this class' code. In
     * particular the setSection, setKeyword, and setList methods.
     */

    private void testRecursiveAlterations()
    {
        System.out.println("-------------- TESTING CODE --------------");
        QMMMKeyword kp = new QMMMKeyword("key1",true,
                    new ArrayList<String>(Arrays.asList("val1","val2","var3")));
        QMMMKeyword kp2 = new QMMMKeyword("key2",true,
          new ArrayList<String>(Arrays.asList("\"  w/ spc \"","1.09e+02","2")));
        QMMMKeyword kp3 = new QMMMKeyword("key",false,
                     new ArrayList<String>(Arrays.asList("value_of_mute_key")));

        //adding keyword on existing task
        setKeyword(new ArrayList<String>(Arrays.asList("TASK")),kp);
        setKeyword(new ArrayList<String>(Arrays.asList("TASK")),kp2);
        setKeyword(new ArrayList<String>(Arrays.asList("TASK")),kp3);

        //adding keyword in nonexisting task
        setKeyword(new ArrayList<String>(Arrays.asList("TEST","P1")),kp);
        setKeyword(new ArrayList<String>(Arrays.asList("TEST","P1","P2")),kp2);
        setKeyword(new ArrayList<String>(Arrays.asList("TEST","P1","P2")),kp3);


        QMMMSection sec = new QMMMSection("M1",
        new ArrayList<QMMMKeyword>(Arrays.asList(new QMMMKeyword("kw1",true,
                       new ArrayList<String>(Arrays.asList("1","prova","2"))))),
        new ArrayList<QMMMSection>(),
        new ArrayList<QMMMList>());
        QMMMSection sec2 = new QMMMSection("M2",
        new ArrayList<QMMMKeyword>(Arrays.asList(new QMMMKeyword("kw1",true,
                       new ArrayList<String>(Arrays.asList("1","prova","2"))))),
        new ArrayList<QMMMSection>(),
        new ArrayList<QMMMList>());

        //set a new section 
        setSection(new ArrayList<String>(
                                Arrays.asList("TEST","P2")),sec,true,true,true);
        setSection(new ArrayList<String>(
                          Arrays.asList("TEST","P2","M1")),sec2,true,true,true);

        setKeyword(new ArrayList<String>(Arrays.asList("TEST","P2","M1")),kp);

        // Check for attempt to create loop of sections: a section that is
        // sub-section of itself
        // Uncomment this two lines to creale looping section, which should
        // lead to an error.
/*
        setSection(new ArrayList<String>(
                           Arrays.asList("TEST","P2","M1")),sec,true,true,true);
*/

        //Set a keyword in a newly generated section
        setKeyword(new ArrayList<String>(Arrays.asList("TEST","P2","M1")),kp);
        QMMMList dd = new QMMMList("myList",
        new ArrayList<String>(Arrays.asList("this is line 1",
                        "this is line 2","this is line 3","this is line 4")));
        //Set the list on non-existing section
        setList(new ArrayList<String>(Arrays.asList("T2","PR2","RIPR2")),dd);
        //Set list in existing section
        setList(new ArrayList<String>(Arrays.asList("TEST","P2","M1")),dd);
    }

//------------------------------------------------------------------------------

    /**
     * Produced the text representation of this object as QMMM input file.
     * The text is returned as a list of strings, which are the
     * @return the list of lines of text
     */

    public ArrayList<String> toLinesInput()
    {
        Collections.sort(sections, new QMMMSectionComparator());
        ArrayList<String> lines = new ArrayList<String>();
        for (QMMMSection qmmmDir : sections)
        {
            lines.addAll(qmmmDir.toLinesInput());
	    lines.add(" ");
        }
        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * TODO: finish implementation
     * Produced a text representation of this object following the format of
     * autocompchem's JobDetail text file.
     * This method ignored molecular specification sections because they are
     * not part of the constructor of QMMMJob.
     * @return the list of lines
     */

/*
    public ArrayList<String> toLinesJobDetails()
    {
        Collections.sort(sections, new QMMMSectionComparator());
        ArrayList<String> lines = new ArrayList<String>();
        for (QMMMSection qmmmDir : sections)
        {
            lines.addAll(qmmmDir.toLinesJobDetails());
        }
        return lines;
    }
*/

//------------------------------------------------------------------------------

}
