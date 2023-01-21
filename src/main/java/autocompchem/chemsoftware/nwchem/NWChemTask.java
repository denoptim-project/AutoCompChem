package autocompchem.chemsoftware.nwchem;

import java.util.ArrayList;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import autocompchem.datacollections.ParameterStorage;
import autocompchem.run.Terminator;

/**
 * Object representing a single task for NWChem. 
 * An <code>NWChemTask</code> can be defined from
 * a formatted text file (the TASKDETAILS file). A sequence of TASKS
 * defines a JOB, thus, a sequence of the TASKDETAILS text blocks,
 *  separated using the
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#TASKSEPARATORJD}
 * string, defines a JOBDETAILS file (see {@link NWChemJob}).
 * The format of the an NWChem's input follows a data structure based on 
 * directives:
 * <br><br><pre>
 * 1st_DIRECTIVE_NAME keyword1 keyword2 listOfValuesForKeyword2
 * 2nd_DIRECTIVE_NAME keyword1
 *    SUBDIRECTIVE keyword1 keyword2 listOfValuesForKeyword2
 *       1st_level_subdirective_data
 *       SUBDIRECTIVE 
 *          2nd_level_subdirective_data
 *       END
 *    END
 * END
 * 3rd_DIRECTIVE_NAME
 * 4th_DIRECTIVE_NAME keyword
 * TASK model calculation</pre>
 * In particular, 
 * note that the "TASK" directive has two keywords: the definition of the 
 * model (i.e., scf, dft), and that of the type of calculation 
 * (i.e., energy, optimize, saddle, etc.)
 * According to this formalism the TASKDETAILS file (as well as any of the 
 * TASKDETAIL text blocks in a JOBDETAILS file) adhere to a syntax that allows a
 * combined human- and computer-readable access to specific information.
 * Each single piece of information (i.e., the method to be used in the QM 
 * calculation, for example, HF) is to be provided as follows:
 * <br><br>Syntax:<br><code>
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABDIRECTIVE}directiveName
 * &lt;typeLabel&gt;&lt;content&gt;
 * </code><br>
 * Example (ignore double quotes):<br><code>
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABDIRECTIVE}task {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABLOUDKEY}method{@value autocompchem.chemsoftware.nwchem.NWChemConstants#KEYVALSEPARATOR}HF</code>
 * <br><br>
 * where &lt;typeLabel&gt; can be
 * <ul>
 * <li>{@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABMUTEKEY} for a 
 * "mute" keyword, that is, a "<code>keyword value</code>" paired information
 * where only <code>value</code> is to be
 * written in the NWChem input file, while the <code>keyword</code> is only used
 * by AutoCompChem to identify the bit of information.
 * The {@value autocompchem.chemsoftware.nwchem.NWChemConstants#KEYVALSEPARATOR}
 * sign (without double quotes) is used to separe 
 * <code>typeLabel</code> and <code>content</code> in the JOBDETAILS syntax.
 * The result is:
 * <code>
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABMUTEKEY}&lt;keyword&gt;{@value autocompchem.chemsoftware.nwchem.NWChemConstants#KEYVALSEPARATOR}&lt;value&gt;
 * </code>
 * </li>
 * <li>{@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABLOUDKEY} for a
 * "loud" keyword, that is, a "<code>keyword value</code>" paired information
 * where both <code>keyword</code> and <code>value</code> are 
 * written in the NWChem input file. 
 * The {@value autocompchem.chemsoftware.nwchem.NWChemConstants#KEYVALSEPARATOR} 
 * sign (without double quotes) is used to separe 
 * <code>typeLabel</code> and <code>content</code> in the JOBDETAILS syntax.
 * The result is:
 * <code>
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABLOUDKEY}&lt;keyword&gt;{@value autocompchem.chemsoftware.nwchem.NWChemConstants#KEYVALSEPARATOR}&lt;value&gt;
 * </code>
 * </li> 
 * <li>{@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABDATA} for a 
 * generic block of text-like information identified by a reference name. The
 * reference name is used only by AutoCompChem and it's not written in NWChem
 * input file.
 * The {@value autocompchem.chemsoftware.nwchem.NWChemConstants#DATAVALSEPARATOR}
 * sign (without double quotes) is used to separe the reference name from 
 * the actual data (i.e., text that is going to be written in the NWChem input)
 * in the JOBDETAILS syntax.
 * The result is:
 * <code>
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABDATA}&lt;refName&gt;{@value autocompchem.chemsoftware.nwchem.NWChemConstants#DATAVALSEPARATOR}&lt;data&gt;
 * </code>
 * </li> 
 * <li>{@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABDIRECTIVE} for e 
 * sub-directive. Sub-directives are defined with the very same syntax 
 * independently on their level (i.e., recursion).
 * </li>
 * </ul>
 * The value of keywords or the directive data can be specified in more than one
 * line of text using the labels 
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABOPENBLOCK} and
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABCLOSEBLOCK} to begin 
 * and end the multi line block of information.
 * For example (all without double quotes):
 * <br><br>
 * <pre>
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABDIRECTIVE}basis {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABDATA}basisSetData{@value autocompchem.chemsoftware.nwchem.NWChemConstants#DATAVALSEPARATOR}{@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABOPENBLOCK} oxygen library cc-pvdz
 * hydrogen library cc-pvdz
 * nitrogen library cc-pvdz
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABCLOSEBLOCK}</pre>
 * results in an NWChem input file containing this:
 * <br><br>
 * <pre>BASIS
 *   oxygen library cc-pvdz
 *   hydrogen library cc-pvdzv
 *   nitrogen library cc-pvdz
 * END</pre>
 * A task can also include task-specific parameters for AutoCompChem. Such
 * parameters are active only within the specific task and are not perceived by
 * NWChem. Examples are the identification of atoms to freeze in a specific 
 * task, 
 * and also the alteration of the charge and spin multiplicity. The complete 
 * list of supported task-specific parameters is reported at
 * {@link autocompchem.chemsoftware.nwchem.NWChemInputWriter}.
 * A block with
 * task-specific parameters is opened by the label
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABPARAMS} and remains
 * open until a NWChem directive is defined, thus until the '
 * {@value autocompchem.chemsoftware.nwchem.NWChemConstants#LABDIRECTIVE}' label
 * is encountered, or until the end of the task block.
 *
 * @author Marco Foscato
 */

public class NWChemTask
{
    /**
     * List of directives
     */
    private List<NWChemDirective> directives;

    /**
     * List of actions and parameters specific to this task
     */
    private ParameterStorage params;

//------------------------------------------------------------------------------

    /**
     * Contruct an empty NWChem task/step
     */

    public NWChemTask()
    {
        directives = new ArrayList<NWChemDirective>();
        params = new ParameterStorage();
    }

//------------------------------------------------------------------------------

    /**
     * Construct a NWChem Step object from a formatted text divided
     * in lines. Molecular details like coordinates are not read by this method.
     * @param lines array of lines with formatted text
     */

    public NWChemTask(List<String> lines)
    {
        //Initialize
        directives = new ArrayList<NWChemDirective>();
        params = new ParameterStorage();

        //Check that there is no task separator
        for (int i=0; i<lines.size(); i++)
        {
            String line = lines.get(i);
            if (line.toUpperCase().contains(NWChemConstants.TASKSEPARATORJD))
            {
                Terminator.withMsgAndStatus("ERROR! Trying to feed a multistep "
                                          + "input to a single NWChemTask.",-1);
            }
        }

        // Split text by directive
        Map<String,ArrayList<String>> dirsAsTxt = 
                                        new HashMap<String,ArrayList<String>>();
        Iterator<String> it = lines.iterator();
        while (it.hasNext())
        {
            String line = it.next();
            //Skip if not a directive
            String lineUp = line.toUpperCase();
            if (!lineUp.startsWith(NWChemConstants.LABDIRECTIVE))
            {
                if (lineUp.startsWith(NWChemConstants.LABPARAMS))
                {
                    //Collect task-specific parameters lines
                    ArrayList<String> paramsLines = new ArrayList<String>();
                    paramsLines.add(
                            line.substring(NWChemConstants.LABPARAMS.length()));
                    while (it.hasNext())
                    {
                        line  = it.next();
                        lineUp = line.toUpperCase();
                        if (lineUp.startsWith(NWChemConstants.LABDIRECTIVE))
                        {
                            break;
                        }
                        else if (lineUp.startsWith(
                                               NWChemConstants.TASKSEPARATORJD))
                        {
                            Terminator.withMsgAndStatus("ERROR! Trying to feed "
                                                     + "a multitask input to a "
                                                     + "single NWChemTask.",-1);
                        }
                        else if (lineUp.startsWith(NWChemConstants.LABPARAMS))
                        {
                            params.importParametersFromLines(
                                "with NWChemTask-specific params", paramsLines);
                            paramsLines.clear();
                            paramsLines.add(line.substring(
                                           NWChemConstants.LABPARAMS.length()));
                        }
                        else
                        {
                            paramsLines.add(line);
                        }
                    }
                    //Add params
                    params.importParametersFromLines(
                                "with NWChemTask-specific params", paramsLines);

                    //covers cases where params block terminates task block
                    if (!it.hasNext())
                    {
                        break;
                    }
                }
                else
                {
                    continue;
                }
            }

            //Get directive name
            line = line.substring(NWChemConstants.LABDIRECTIVE.length());
            String[] parts = line.trim().split("\\s+",2);
            //String dirName = parts[0].toUpperCase();
            String dirName = parts[0];
            String dirContent = "";
            if (parts.length > 1)
            {
                dirContent = parts[1];
            }

            //Deal with multiline blocks
            if (line.contains(NWChemConstants.LABOPENBLOCK) 
                && !line.contains(NWChemConstants.LABCLOSEBLOCK))
            {
                boolean blockIsOpen = true;
                while (it.hasNext())
                {
                    String innerLine = it.next();
                    if (innerLine.toUpperCase().contains(
                                                 NWChemConstants.LABCLOSEBLOCK))
                    {
                        blockIsOpen = false;
                        break;
                    }
                    dirContent = dirContent 
                                 + System.getProperty("line.separator")
                                 + innerLine;
                }
                if (blockIsOpen)
                {
                    Terminator.withMsgAndStatus("ERROR! Multiline block opened "
                                                      + "but never closed.",-1);
                }
            }    

            //Collect lines of each directive
            if (dirsAsTxt.containsKey(dirName))
            {
                dirsAsTxt.get(dirName).add(dirContent);
            }
            else
            {
                ArrayList<String> dirLines = new ArrayList<String>();
                dirLines.add(dirContent);
                dirsAsTxt.put(dirName,dirLines);
            }
        }

        //Make and store directives
        for (String dKey : dirsAsTxt.keySet())
        {
            NWChemDirective dir = new NWChemDirective(dKey,dirsAsTxt.get(dKey));
            directives.add(dir);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Imposes the components of a given template task (i.e., all directives, 
     * keywords, and data blocks of the template) onto this task. Only the 
     * components defined in the template NWChemTask are modified in 
     * <code>this</code>. 
     * @param templateTsk the template task
     */

    public void impose(NWChemTask templateTsk)
    {
        for (NWChemDirective tmplDir : templateTsk.getAllDirectives())
        {
            ArrayList<String> path = new ArrayList<String>();
            String errMsg = recursiveImpose(path,tmplDir);
            if (errMsg.length() > 0)
            {
                Terminator.withMsgAndStatus("ERROR! Unable to impose "
                 + "NWChemDirective '" + tmplDir.getName() + "': " + errMsg,-1);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Recursive method to explore all components of a <i>template directive</i>
     * and impose their content into the respective directives of 
     * <code>this</code> NWChemTask.
     * @param path the path of the template NWChemTask. NB: the path includes
     * all the directive names from the <i>root directive</i> to the 
     * <i>parent directive</i> that contains 
     * the <i>template directive</i> given in the
     * <code>tmplDir</code> parameter.
     * The name of the <i>template directive</i>
     * is not contained in the path.
     * @param tmplDir the <i>template directive</i>.
     * @return an error message if anything went wrong.
     */

    private String recursiveImpose(ArrayList<String> path, 
                                                        NWChemDirective tmplDir)
    {
        //Not used now: maybe we'll need to report possible errors
        String errMsg = "";

        //Define path to the inside of the tmplDir
        ArrayList<String> subPath = new ArrayList<String>();
        subPath.addAll(path);
        subPath.add(tmplDir.getName());

        //Handle directive itself
        if (!this.hasDirective(new ArrayList<String>(),tmplDir))
        {
            this.setDirective(new ArrayList<String>(),tmplDir,true,true,true);
        }
        
        //Handle keywords
        for (NWChemKeyword tmplKey : tmplDir.getAllKeywords())
        {
            setKeyword(subPath,tmplKey);
        }

        //Handle data blocks
        for (NWChemDirectiveData tmplData : tmplDir.getAllDirectiveDataBlocks())
        {
            setDataDirective(subPath,tmplData);
        }

        //Handle subDirectives
        for (NWChemDirective subDir : tmplDir.getAllSubDirectives())
        {
            if (this.hasDirective(subPath,subDir))
            {
                errMsg = recursiveImpose(subPath,subDir);
            }
            else
            {
                setDirective(subPath,subDir,true,true,true);
            }
        }

        return errMsg;
    }

//------------------------------------------------------------------------------

    /**
     * Extracts selected entities (i.e., subdirectives, keywords, data blocks)
     * from this NWChemTask according to a given mask.
     * The mask is a NWChemTask that contains all the wanted entities either as
     * fully defined objects (e.g., both keyword names and values) 
     * or as empty objects (e.g., a keyword with a name but no value).
     * If <code>this</code> contains an object corresponding (i.e., same name 
     * directives path) to one of those in the mask, the object of 
     * <code>this</code> is copied into the return object of this method.
     * @param mask the NWChemTask acting as mask.
     * @return a new NWChemTask with the extracted objects
     */

     public NWChemTask extract(NWChemTask mask)
     {
        NWChemTask collectorTask = new NWChemTask();
        for (NWChemDirective mskDir : mask.getAllDirectives())
        {
            ArrayList<String> pathDir = new ArrayList<String>();
            if (this.hasDirective(pathDir,mskDir))
            {
                String errMsg = recursiveExtract(pathDir,mskDir,collectorTask);
                if (errMsg.length() > 0)
                {
                    Terminator.withMsgAndStatus("ERROR! Unable to extract from "
                                + "NWChemDirective '" + mskDir.getName() + "': "
                                                                   + errMsg,-1);
                }
            }
        }
        return collectorTask;
     }

//------------------------------------------------------------------------------

    /**
     * Recursive method to extract selected components of <code>this</code> 
     * NWChemTask. This method assumens that <code>this</code> has a
     * NWChemDirective corresponding to <code>mskDir</code>.
     * @param path the path of the currently active NWChemDirective of the mask.
     * NB: the path includes 
     * all the directive names from the <i>root directive</i> to the
     * <i>parent directive</i> that contains
     * the <i>active directive</i> given in the
     * <code>mskDir</code> parameter.
     * The name of the <i>active directive</i>
     * is not contained in the path.
     * @param mskDir the <i>active directive</i>.
     * @param resultTask a collector where all the extracted components are
     * collected as copyes (i.e., new objects, not references).
     * @return an error message if anything went wrong.
     */

    private String recursiveExtract(ArrayList<String> path,
                                  NWChemDirective mskDir, NWChemTask resultTask)
    {
        //Not used now: maybe we'll need to report errors
        String errMsg = "";

        //Get active directive of 'this'
        NWChemDirective locDir = this.getDirective(path,mskDir.getName(),false);

        //Ensure directive is created even if it's empty
        resultTask.setDirective(path,new NWChemDirective(mskDir.getName()),
                                                             false,false,false);

        //Define path to the inside of the mskDir
        ArrayList<String> subPath = new ArrayList<String>();
        subPath.addAll(path);
        subPath.add(mskDir.getName());

        //Handle keywords
        for (NWChemKeyword mskKey : mskDir.getAllKeywords())
        {
            NWChemKeyword locKey = locDir.getKeyword(mskKey.getName());
            if (locKey != null)
            {
                resultTask.setKeyword(subPath,new NWChemKeyword(
                                                  locKey.toStringJobDetails()));
            }
        }

        //Handle data blocks
        for (NWChemDirectiveData mskData : mskDir.getAllDirectiveDataBlocks())
        {
            NWChemDirectiveData locData = locDir.getDirectiveData(
                                                             mskData.getName());
            if (locData != null)
            {
                resultTask.setDataDirective(subPath,new NWChemDirectiveData(
                                       locData.getName(),locData.getContent()));
            }
        }

        //Handle subDirectives
        for (NWChemDirective subDir : mskDir.getAllSubDirectives())
        {
            if (this.hasDirective(subPath,subDir))
            {
                errMsg = recursiveExtract(subPath,subDir,resultTask);
            }
        }

        return errMsg;
    }

//------------------------------------------------------------------------------

    /**
     * Set the title, which is reported as keyword of the TITLE directive
     * @param title the new title
     */

    public void setTitle(String title)
    {
        NWChemKeyword k = new NWChemKeyword("title",false,
                                  new ArrayList<String>(Arrays.asList(title)));
        setKeyword(new ArrayList<String>(Arrays.asList(
                                                  NWChemConstants.TITLEDIR)),k);
    }

//------------------------------------------------------------------------------

    /**
     * Get the title
     * @return the content of the title directive
     */

    public String getTitle()
    {
        StringBuilder sb = new StringBuilder();
        if (this.getDirective(NWChemConstants.TITLEDIR) != null)
        {
            for (String s : getDirective(NWChemConstants.TITLEDIR).getKeyword(
                                                            "title").getValue())
            {
                sb.append(s).append(" ");
            }
        }
        return sb.toString();
    } 

//------------------------------------------------------------------------------

    /**
     * Set the prefix, which is reported as keyword of the START directive
     * @param prefix the new prefix
     */
   
    public void setPrefix(String prefix)
    {
        NWChemKeyword k = new NWChemKeyword("file_prefix",false,
                                  new ArrayList<String>(Arrays.asList(prefix)));
        setKeyword(new ArrayList<String>(Arrays.asList(
                                                  NWChemConstants.STARTDIR)),k);
    }

//------------------------------------------------------------------------------

    /**
     * Set the total charge of the system
     * @param charge the total charge
     */

     public void setCharge(int charge)
     {
        String theory = getTaskTheory();
        switch (theory.toUpperCase())
        {
            case "PYTHON":
                // nothing to do
                break;

            default:
                NWChemKeyword k = new NWChemKeyword("read_charge",false,
                new ArrayList<String>(Arrays.asList(Integer.toString(charge))));
                setKeyword(new ArrayList<String>(Arrays.asList(
                                                 NWChemConstants.CHARGEDIR)),k);
         }
     }

//------------------------------------------------------------------------------

    /**
     * Set the spin multiplicity. Note that the behavior of this method is
     * determined by the calculation method. For now, only SDF (i.e., HF) and 
     * DFT are supported.
     * @param sm the spin multiplicity
     */

     public void setSpinMultiplicity(int sm)
     {
        String smStr = "";
        if (sm < NWChemConstants.SCFSPINMULT.size())
        {
            smStr = NWChemConstants.SCFSPINMULT.get(sm-1);
        }
        else
        {
            smStr = "NOPEN " + sm;
        }
       
        String theory = getTaskTheory();
        switch (theory.toUpperCase()) 
        {
            case "SCF":
                ArrayList<String> pathSpDir = new ArrayList<String>(
                                                          Arrays.asList("SCF"));
                for (String sp : NWChemConstants.SCFSPINMULT)
                {
                    NWChemDirective oldSpDir = getDirective(pathSpDir,sp,false);
                    if (oldSpDir != null)
                    {
                        deleteDirective(pathSpDir,oldSpDir);
                    }
                }

// NOTE: the SINGLET, DOUBLET, etc. are subdirectives of the SCF directive. 
// In some place in the NWChem documentations they are called "keywords".
                NWChemDirective spDir = new NWChemDirective(smStr); 
                setDirective(pathSpDir,spDir,true,true,true);
                break;

            case "DFT":
//TODO: here we assume that the MULT is a keyword, though
// the documentation indicates (but not explicitly) that these are directive
                NWChemKeyword kdft = new NWChemKeyword("mult",true,
                    new ArrayList<String>(Arrays.asList(Integer.toString(sm))));
                setKeyword(new ArrayList<String>(Arrays.asList("DFT")),kdft);
                break;

            case "PYTHON":
                // nothing to do
                break;

            default:
                Terminator.withMsgAndStatus("ERROR! Automated setting of spin "
                + " multiplicity not supported for theory '" + theory + "'",-1);
        }

    }

//------------------------------------------------------------------------------

    /**
     * Sets a directive specifying all its content: keywords, subdirectives, 
     * and data blocks.
     * This method can be used to create directives that are sub-directives with
     * a specific path.
     * @param dirPath the path to the directive to edit. Directive's names
     * are read so that entry 0 is the name of outermost directive. The name 
     * of the directive to create or alter <b>must not be listed</b> 
     * in the path.
     * @param newDir the directive to create or overwrite.
     * @param owKeys if <code>true</code> makes this method overwrite the 
     * keywords of the existing directive, otherwise it appends.
     * If the directive does not exist, this
     * flag has no effect.
     * @param owSubDirs if <code>true</code> makes this method overwrite the
     * sub-directives of the existing directive, otherwise it appends.
     * If the directive does not exist, this flag has no effect.
     * @param owData if <code>true</code> makes this method overwrite the
     * data of the existing directive, otherwise it appends.
     * If the directive does not exist, this
     * flag has no effect.  
     */

    public void setDirective(ArrayList<String> dirPath, NWChemDirective newDir,
                              boolean owKeys, boolean owSubDirs, boolean owData)
    {
        if (dirPath.size() == 0)
        {
            NWChemDirective oldDir = getDirective(newDir.getName());
            if (oldDir == null)
            {
                directives.add(newDir);
            }
            else
            {
                if (owKeys)
                {
                    oldDir.setAllKeywords(newDir.getAllKeywords());
                }
                else
                {
                    for (NWChemKeyword k : newDir.getAllKeywords())
                    {
                        oldDir.addKeyword(k);
                    }
                }
                if (owSubDirs)
                {
                    oldDir.setAllSubDirectives(newDir.getAllSubDirectives());
                }
                else
                {
                    for (NWChemDirective d : newDir.getAllSubDirectives())
                    {
                        oldDir.addSubDirective(d);
                    }
                }
                if (owData)
                {
                    oldDir.setAllDirectiveDataBlocks(
                                            newDir.getAllDirectiveDataBlocks());
                }
                else
                {
                    for (NWChemDirectiveData dd : 
                                             newDir.getAllDirectiveDataBlocks())
                    {
                        oldDir.addDirectiveData(dd);
                    }
                }
            } 
        }
        else
        {
            ArrayList<String> parentPath = new ArrayList<String>();
            String parentName = dirPath.get(dirPath.size()-1);
            for (int i=0; i<(dirPath.size()-1); i++)
            {
                parentPath.add(dirPath.get(i));
            }
            NWChemDirective parentDir = getDirective(parentPath, parentName,
                                                                          true);
            parentDir.setSubDirective(newDir,owKeys,owSubDirs,owData);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Sets a keyword belonging to a specific directive or subdirective.
     * In order to set the keyword, all parent NWChemDirectives and the
     * NWChemDirective containing the keywords are created if they do not
     * exist already.
     * @param dirPath the path to the directive to edit. Directive's names
     * are read so that entry 0 is the name of outermost directive. Since
     * keywords must be contained in a directive the path cannot be empty.
     * @param kw the keyword to set or overwrite
     */

    public void setKeyword(ArrayList<String> dirPath, NWChemKeyword kw)
    {
        ArrayList<String> parentPath = new ArrayList<String>();
        String dirName = dirPath.get(dirPath.size()-1); 
        for (int i=0; i<(dirPath.size()-1); i++)
        {
            parentPath.add(dirPath.get(i));
        }
        NWChemDirective dir = getDirective(parentPath,dirName,true);
        dir.setKeyword(kw);
    }

//------------------------------------------------------------------------------

    /**
     * Sets a data block belonging to a specific directive or subdirective.
     * @param dirPath the path to the directive to edit. Directive's names
     * are read so that entry 0 is the name of outermost directive.
     * @param data the block of data to set or overwrite
     */

    public void setDataDirective(ArrayList<String> dirPath, 
                                                       NWChemDirectiveData data)
    {
        ArrayList<String> parentPath = new ArrayList<String>();
        String dirName = dirPath.get(dirPath.size()-1);
        for (int i=0; i<(dirPath.size()-1); i++)
        {
            parentPath.add(dirPath.get(i));
        }
        NWChemDirective dir = getDirective(parentPath,dirName,true);
        dir.setDataDirective(data);
    }

//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if this task has task-specific AutoCompChem 
     * parameters
     */

    public boolean hasACCParams()
    {
        return !params.isEmpty();
    }

//------------------------------------------------------------------------------

    /**
     * @return the ParameterStorage specific to this task
     */

    public ParameterStorage getTaskSpecificParams()
    {
        return params;
    }

//------------------------------------------------------------------------------

    /**
     * Evaluates whether this task has a specific directive
     * @param path the path to the directive. NB: the path includes
     * all the directive names from the <i>root directive</i> to the
     * <i>parent directive</i> that contains
     * the <i>specific directive</i> given in the
     * <code>dir</code> parameter.
     * The name of the <i>specific directive</i>
     * is not contained in the path.
     * @param dir the <i>specific directive</i>.
     * @return <code>true</code> if this task has a directive with the name
     * equal to <code>dir</code> and is located in the same <code>path</code>.
     * NB: the matching directive contained in this task and <code>dir</code>
     * are <b>not required to be neither <i>same</i> nor <i>equal</i></b>.
     */

    public boolean hasDirective(ArrayList<String> path, NWChemDirective dir)
    {
        boolean found = false;
        if (path.size() == 0)
        {
            if (getDirective(dir.getName()) != null)
            {
                found = true;
            }        
        }
        else
        {
            NWChemDirective parentDir = null;
            for (int i=0; i<path.size(); i++)
            {
                String parentDirName = path.get(i);
                if (i == 0)
                {
                    parentDir = getDirective(parentDirName);
                }
                else
                {
                    parentDir = parentDir.getSubDirective(parentDirName);
                }
                if (parentDir == null)
                {
                    break;
                }
            }
            if (parentDir != null)
            {
                if (parentDir.getSubDirective(dir.getName()) != null)
                {
                    found = true;
                }
            }
        }
        return found;
    }

//------------------------------------------------------------------------------

    /**
     * Count the directive (only first layer)
     * @return the number of directives
     */

    public int getDirectivesCount()
    {
        return directives.size();
    }

//------------------------------------------------------------------------------

    /**
     * Get a directive of this task
     * @param i the index of the directive
     * @return the required directive
     */

    public NWChemDirective getDirective(int i)
    {
        return directives.get(i);
    }

//------------------------------------------------------------------------------

    /**
     * Get a directive of this task. Looks only in first layer of directives:
     * subdirectives are not searched.
     * @param name the name of the directive to search
     * @return the directive with the given name or <code>null</code> if no 
     * such directory is found.
     */

    public NWChemDirective getDirective(String name)
    {
        for (NWChemDirective dir : directives)
        {
            if (dir.getName().toUpperCase().equals(name.toUpperCase()))
            {
                return dir;
            }
        }
        return null;
    }

//------------------------------------------------------------------------------

    /**
     * Get a directive or a subdirective of this task. This method can be used
     * to get any subdirective of any layer, not only in the first layer.
     * @param dPath the path to the target directive. NB: the path includes
     * all the directive names from the <i>root directive</i> to the
     * <i>parent directive</i> that contains
     * the <i>target directive</i>, of which name is given in the
     * <code>dName</code> parameter.
     * The name of the <i>target directive</i>
     * is not contained in the path.
     * @param dName the name of the target directive.
     * @param canMake if <code>true</code> requires the method to make the
     * searched directive and all missing parent directives.
     * @return the directive with the given name or <code>null</code> if no
     * such directory is found and <code>canMake</code> is <code>false</code>.
     */

    public NWChemDirective getDirective(ArrayList<String> dPath, String dName,
                                                                boolean canMake)
    {
        if (dPath.size() == 0)
        {
            for (NWChemDirective dir : directives)
            {
                if (dir.getName().toUpperCase().equals(dName.toUpperCase()))
                {
                    return dir;
                }
            }
            if (canMake)
            {
                NWChemDirective dir = new NWChemDirective(dName);
                directives.add(dir);
                return dir;
            }
            return null;
        }

        NWChemDirective parentDir = null;
        for (int i=0; i<dPath.size(); i++)
        {
            String parentDirName = dPath.get(i);
            if (i == 0)
            {
                parentDir = getDirective(parentDirName);
            }
            else
            {
                parentDir = parentDir.getSubDirective(parentDirName);
            }
            if (parentDir == null)
            {
                if (canMake)
                {
                    parentDir = new NWChemDirective(parentDirName);
                    directives.add(parentDir);
                }
                else
                {
                    break;
                }
            }
        }
        if (parentDir != null)
        {
            NWChemDirective dir = parentDir.getSubDirective(dName);
            if (dir != null)
            {
                return dir;
            }
            if (canMake)
            {
                dir = new NWChemDirective(dName);
                parentDir.addSubDirective(dir);
                return dir;
            }
        }
        return null;
    }

//------------------------------------------------------------------------------

    /**
     * Get all directives of this task
     * @return the list of directives
     */

    public List<NWChemDirective> getAllDirectives()
    {
        return directives;
    }

//------------------------------------------------------------------------------

    /**
     * Removes the innermost directives of a given template task from this task.
     * Only the innermost directive of a directives tree is removed. And
     * if is not found in <code>this</code> nothing is done.
     * @param templateTsk the template task
     */

    public void delete(NWChemTask templateTsk)
    {
        for (NWChemDirective tmplDir : templateTsk.getAllDirectives())
        {
            ArrayList<String> path = new ArrayList<String>();
            String errMsg = recursiveDeleteInnermost(path,tmplDir);
            if (errMsg.length() > 0)
            {
                Terminator.withMsgAndStatus("ERROR! Unable to remove "
                 + "NWChemDirective '" + tmplDir.getName() + "': " + errMsg,-1);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Recursive method to explore a <i>template directive</i>
     * and delete the innermost directives from
     * <code>this</code> NWChemTask.
     * @param path the path of the template NWChemTask. NB: the path includes
     * all the directive names from the <i>root directive</i> to the
     * <i>parent directive</i> that contains
     * the <i>template directive</i> given in the
     * <code>tmplDir</code> parameter.
     * The name of the <i>template directive</i>
     * is not contained in the path.
     * @param tmplDir the <i>template directive</i>.
     * @return an error message if anything went wrong.
     */

    private String recursiveDeleteInnermost(ArrayList<String> path,
                                                        NWChemDirective tmplDir)
    {
        //Not used now: maybe we'll need to report possible errors
        String errMsg = "";

        //Define path to the inside of the tmplDir
        ArrayList<String> subPath = new ArrayList<String>();
        subPath.addAll(path);
        subPath.add(tmplDir.getName());

        //Handle subDirectives
        if (tmplDir.getAllSubDirectives().size()>0)
        {
            for (NWChemDirective subDir : tmplDir.getAllSubDirectives())
            {
                if (this.hasDirective(subPath,subDir))
                {
                    errMsg = recursiveDeleteInnermost(subPath,subDir);
                }
            }
        }
        else
        {
            deleteDirective(path,tmplDir.getName());
        }

        return errMsg;
    }


//-----------------------------------------------------------------------------

    /**
     * Remove a directive from the data structure rooted on this task.
     * The directive to remove can be a subdirective (of any level) of a
     * directive contained in this task. If the target directive does not exist
     * nothing will be done.
     * @param path path of the directive to remove. The path must contain the
     * name of the parent directives starting from the outermost. The name of
     * directive to remove <b>must not be listed </b> in the path.
     * @param name the name of directive that will be removed
     */

    public void deleteDirective(ArrayList<String> path, String name)
    {
        NWChemDirective dir = getDirective(path,name,false);
        if (dir != null)
        {
            deleteDirective(path,dir);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Remove a directive from the data structure rooted on this task. 
     * The directive to remove can be a subdirective (of any level) of a 
     * directive contained in this task. If the target directive does not exist
     * nothing will be done.
     * @param path path of the directive to remove. The path must contain the
     * name of the parent directives starting from the outermost. The name of
     * directive to remove <b>must not be listed </b> in the path.
     * @param dir the directive that will be removed
     */

    public void deleteDirective(ArrayList<String> path, NWChemDirective dir)
    {
        if (path.size() == 0)
        {
            directives.remove(dir);
        }
        else
        {
            //Get parent directive
            ArrayList<String> parentPath = new ArrayList<String>();
            parentPath.addAll(path);
            parentPath.remove(parentPath.size()-1);
            NWChemDirective parentDir = getDirective(parentPath,path.get(
                                                          path.size()-1),false);
            if (parentDir != null)
            {
                parentDir.deleteSubDirective(dir);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Looks for the theory defined in this task
     * @return the content of the theory keyword
     */

    public String getTaskTheory()
    {
        NWChemDirective dirTsk = getDirective(NWChemConstants.TASKDIR);
        if (dirTsk == null)
        {
            Terminator.withMsgAndStatus("ERROR! TASK directive not found. "
                                     + "Cannot define spin multiplicity if "
                                     + "the TASK directive is not defined. "
                                     + "Check task: " + this.toString(),-1);
        }

        //Default in NWChem is SCF
        String theory = "SCF";

        NWChemKeyword theoryKey = dirTsk.getKeyword(NWChemConstants.THEORYKW);
        if (theoryKey != null)
        {
            theory = theoryKey.getValue().get(0);
        }

        return theory;
    }

//------------------------------------------------------------------------------

    /**
     * Test resursive alteration of directives, keywords, and data blocks.
     * This method is intended for testing part of this class' code. In
     * particular the setDirective, setKeyword, and setDataDirective methods.
     */

    @SuppressWarnings("unused")
        private void testRecursiveAlterations()
    {
        System.out.println("-------------- TESTING CODE --------------");
        NWChemKeyword kp = new NWChemKeyword("key1",true,
                    new ArrayList<String>(Arrays.asList("val1","val2","var3")));
        NWChemKeyword kp2 = new NWChemKeyword("key2",true,
          new ArrayList<String>(Arrays.asList("\"  w/ spc \"","1.09e+02","2")));
        NWChemKeyword kp3 = new NWChemKeyword("key",false,
                     new ArrayList<String>(Arrays.asList("value_of_mute_key")));

        //adding keyword on existing task
        setKeyword(new ArrayList<String>(Arrays.asList("TASK")),kp);
        setKeyword(new ArrayList<String>(Arrays.asList("TASK")),kp2);
        setKeyword(new ArrayList<String>(Arrays.asList("TASK")),kp3);

        //adding keyword in nonexisting task
        setKeyword(new ArrayList<String>(Arrays.asList("TEST","P1")),kp);
        setKeyword(new ArrayList<String>(Arrays.asList("TEST","P1","P2")),kp2);
        setKeyword(new ArrayList<String>(Arrays.asList("TEST","P1","P2")),kp3);


        NWChemDirective dir = new NWChemDirective("M1",
        new ArrayList<NWChemKeyword>(Arrays.asList(new NWChemKeyword("kw1",true,
                       new ArrayList<String>(Arrays.asList("1","prova","2"))))),
        new ArrayList<NWChemDirective>(),
        new ArrayList<NWChemDirectiveData>());
        NWChemDirective dir2 = new NWChemDirective("M2",
        new ArrayList<NWChemKeyword>(Arrays.asList(new NWChemKeyword("kw1",true,
                       new ArrayList<String>(Arrays.asList("1","prova","2"))))),
        new ArrayList<NWChemDirective>(),
        new ArrayList<NWChemDirectiveData>());

        //set a new directive 
        setDirective(new ArrayList<String>(
                                    Arrays.asList("TEST","P2")),dir,true,true,true);
        setDirective(new ArrayList<String>(
                          Arrays.asList("TEST","P2","M1")),dir2,true,true,true);

        setKeyword(new ArrayList<String>(Arrays.asList("TEST","P2","M1")),kp);

        // Check for attempt to create loop of directives: a directive that is
        // sub-directive of itself
        // Uncomment this two lines to creale looping directive, which should
        // lead to an error.
/*
        setDirective(new ArrayList<String>(
                           Arrays.asList("TEST","P2","M1")),dir,true,true,true);
*/

        //Set a keyword in a newly generated directive
        setKeyword(new ArrayList<String>(Arrays.asList("TEST","P2","M1")),kp);
        NWChemDirectiveData dd = new NWChemDirectiveData("myData",
        new ArrayList<String>(Arrays.asList("this is line 1",
                          "this is line 2","this is line 3","this is line 4")));
        //Set the data block on non-existing directive
        setDataDirective(new ArrayList<String>(
                                         Arrays.asList("T2","PR2","RIPR2")),dd);
        //Set data block in existing directive
        setDataDirective(new ArrayList<String>(
                                           Arrays.asList("TEST","P2","M1")),dd);
    }

//------------------------------------------------------------------------------

    /**
     * Produced the text representation of this object as NWChem input file.
     * The text is returned as a list of strings, which are the
     * lines of the NWChem input file.
     * @return the lines of text ready to print this task into an input file 
     * for NWChem
     */

    public ArrayList<String> toLinesInput()
    {
        Collections.sort(directives, new NWChemDirectiveComparator());
        ArrayList<String> lines = new ArrayList<String>();
        for (NWChemDirective nwcDir : directives)
        {
            lines.addAll(nwcDir.toLinesInput());
        }
        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Produced a text representation of this object following the format of
     * autocompchem's JobDetail text file.
     * This method ignored molecular specification sections because they are
     * not part of the constructor of NWChemJob.
     * @return the lines of text for printing this task into a jobdetails file
     */

    public ArrayList<String> toLinesJobDetails()
    {
        Collections.sort(directives, new NWChemDirectiveComparator());
        ArrayList<String> lines = new ArrayList<String>();
        for (NWChemDirective nwcDir : directives)
        {
            lines.addAll(nwcDir.toLinesJobDetails());
        }
        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * @return the string representation
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[NWChemTask [");
        for (String line : toLinesJobDetails())
        {
            sb.append(line).append(", ");
        }
        sb.append("]]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
