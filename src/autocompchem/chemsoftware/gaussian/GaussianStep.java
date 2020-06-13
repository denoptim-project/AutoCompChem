package autocompchem.chemsoftware.gaussian;

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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import autocompchem.run.Terminator;

/**
 * Object representing a single step Gaussian input. 
 * The object <code>GaussianStep</code> can be defined directly from
 * a formatted text file (the STEPDETAILS file). A sequence of STEPS
 * defines a JOB, thus a sequence of the STEPDETAILS files defines 
 * a JOBDETAILS file (see {@link GaussianJob}).
 * The format of a STEPDETAILS follows a key-value structure:<br><br>
 * <code>[key][value]</code><br><br>
 * The <code>key</code> is one of the following (without quotes):
 * <ul>
 * <li>
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#KEYLINKSEC}
 * used to provide values related to the Link0 section of Gaussian.
 * See <a href="http://www.gaussian.com/g_tech/g_ur/k_link0.htm">Link0</a>.
 * </li> 
 * <li>
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#KEYROUTESEC}
 * used for the Route section.
 * See <a href="http://www.gaussian.com/g_tech/g_ur/m_input.htm">Gaussian</a>.
 * </li> 
 * <li>
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#KEYTITLESEC}
 * used for the title/comment
 * </li>
 * <li>
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#KEYOPTSSEC}
 * used for the Options section.
 * See <a href="http://www.gaussian.com/g_tech/g_ur/m_input.htm">Gaussian</a>.
 * </li> 
 * </ul>
 * <br>
 * The <code>value</code> is a label-value pair (basically an inner key-value
 * pair). Label and value of this pair are separated by '='.
 * In lines pertaining to the
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#KEYROUTESEC}
 * sections,
 * the <code>label</code> (or second level key) must begin with one the 
 * following prefixes:
 * <ul>
 * <li>
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#LABFREE}
 * when the value is to be treated as free text. The use of this label
 * allows maximum flexibility, but does not allow any routine to interpret
 * or alter the instruction given to Gaussian, i.e., when resubmitting
 * a Gaussian job such instruction cannot be changed.
 * </li> 
 * <li>
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#LABLOUDKEY}
 * is used for a <i>loud keyword</i>, that is, a Gaussian keyword
 * that can have an option or be effective without any option.
 * Examples include 'geom', 'guess', 'scf', ect.
 * </li> 
 * <li>
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#LABMUTEKEY}
 * is used for <i>mute keyword</i>, that is, a keyword that is implicit in
 * the Gaussian input file, thus will not be written in the input. The
 * use of <i>mute keyword</i> allows the routines of this library to
 * read, understand, and write instructions for Gaussian without having to
 * store and manage the whole list of Gaussian keywords.
 * For example, Gaussian does not define any keyword for the type of
 * job but many keywords, one per each job type ('sp', 'opt', 'freq', etc.)
 * To identify the portion of a Gaussian input file that defines the type of
 * job we use a <i>mute keyword</i> that is 
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#SUBKEYJOBTYPE} 
 * (without quotes). Mute keywords include (without quotes):
 * <ul>
 * <li>
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#SUBKEYJOBTYPE}
 * defines type of job. Examples of values are 'sp', 'opt', and 'freq'.
 * </li>
 * <li>
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#SUBKEYMODELBASISET}
 * defines the basis set.
 * </li>
 * <li>
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#SUBKEYMODELMETHOD}
 * defines the model chemistry.
 * </li>
 * <li>
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#SUBKEYPRINT}
 * for the print specification: the very first instruction of the route section.
 * See <a href="http://www.gaussian.com/g_tech/g_ur/k_route.htm">pound sign</a>.
 * </li>
 * </ul></li>
 * </ul>
 * For each second level key, the corresponding second level value can be:
 * <ul>
 * <li>a string, which can contain spaces,</li>
 * <li>a multi line block (opened by the
 * {@value autocompchem.parameters.ParameterConstants#STARTMULTILINE}, and closed by
 * {@value autocompchem.parameters.ParameterConstants#ENDMULTILINE}, see also
 * {@link autocompchem.parameters.ParameterStorage})</li>
 * <li> or a block of information to be interpreted
 * by AutoCompChem (i.e.,{@link autocompchem.parameters.Parameter}), 
 * for instance, a customized, chemical context-specific definition of the 
 * basis set.
 * Values that are to be interpreted as AutoCompChem 
 * {@link autocompchem.parameters.Parameter} 
 * must begin with the 
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#LABPARAMS} label; 
 * all the text following such label is interpreted ad a 
 * {@link autocompchem.parameters.Parameter} until one othe patterns is recognized:
 * a first-level key (
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#KEYLINKSEC},
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#KEYROUTESEC},
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#KEYTITLESEC},
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#KEYOPTSSEC}),
 * or a job step separator (
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#STEPSEPARATOR}),
 * or the end of the text file.</li>
 * </ul>
 * In case of sub-options, that is when a Gaussian keywords (i.e., scf) 
 * can be defined 
 * with a number of specific options, some taking also parameters, the 
 * sub-options is defined with the same format described above and the resulting 
 * string is preceded by the string of the Gaussian keywords with the 
 * {@value autocompchem.chemsoftware.gaussian.GaussianConstants#LABLOUDKEY}
 * label. This allows to generate the following structure:
 * <br>
 * <code>keydors=(sub-option1, sub-option2, etc.)</code>
 * <br>
 * To illustrate, what follows is an example of a formatted text that can
 * be handled easily by library:
 * <br>
 * <br>
 * <code> 
 * ----- beginning of text file -----<br>
 * lnkSection# chk=myname<br>
 * lnkSection# Nprocshared=8<br>
 * lnkSection# mem=28GB<br>
 * rutSection# $MTprint=P<br>
 * rutSection# $MTmodel_Method=OLYP<br>
 * rutSection# $MTmodel_BasisSet=LANL2DZ 5d 7f<br>
 * rutSection# $KVgeom=check<br>
 * rutSection# $KVguess=read<br>
 * rutSection# $MTjobType=opt<br>
 * rutSection# $KVscf_$KVVShift=500<br>
 * rutSection# $KVscf_$MTSymm=nosym<br>
 * rutSection# $KVscf_$MTVarAcc=novaracc<br>
 * rutSection# $KVscf_$KVMaxCycle=980<br>
 * rutSection# $KVopt_$MTforceConstants=ReadFC<br>
 * rutSection# $KVopt_$KVMaxCycle=320<br>
 * rutSection# $MTiop=iop(1/19=11)<br>
 * titSection# my name is myname<br>
 * ----- end of text file -----<br>
 * </code>
 * <br>
 * The result is the preparation of an input file with the following header: 
 * <br>
 * <br>
 * <code>
 * %NPROCSHARED=8<br>
 * %CHK=myname<br>
 * %MEM=28GB<br>
 * #P<br>
 * # OLYP/LANL2DZ 5D 7F<br>
 * # GEOM=CHECK<br>
 * # GUESS=READ<br>
 * # IOP(1/19=11)<br>
 * # SCF=(VSHIFT=500,NOSYM,NOVARACC,MAXCYCLE=980) <br>
 * # OPT=(READFC,MAXCYCLE=320) <br>
 * </code>
 *
 * @author Marco Foscato
 */

public class GaussianStep
{
    /**
     * Link 0 Command section
     */
    private GaussianLinkCommandsSection gLink;

    /**
     * Route Sections
     */
    private GaussianRouteSection gRoute;

    /**
     * Title/comments
     */
    private String comment = "";

    /**
     * Molecular specification section
     */
    private GaussianMolSpecification gMol;

    /**
     * Options
     */
    private GaussianOptionsSection gOpts;


//------------------------------------------------------------------------------

    /**
     * Contructs an empty Gaussian step
     */

    public GaussianStep()
    {
	this.gLink = new GaussianLinkCommandsSection();
	this.gRoute = new GaussianRouteSection();
	this.gMol = new GaussianMolSpecification();
	this.gOpts = new GaussianOptionsSection();
    }

//------------------------------------------------------------------------------

    /**
     * Construct a Gaussian Step object from a formatted text divided
     * in lines. Molecular specification section is not read by this method. Use
     * <code>setAllSpinMultiplicity</code> or <code>setMolSpecification</code>
     * to import the molecular specification.
     * @param lines array of lines with formatted text
     */

    public GaussianStep(ArrayList<String> lines)
    {
	Map<String,ArrayList<String>> sections = 
					new HashMap<String,ArrayList<String>>();
        for (int i=0; i<lines.size(); i++)
        {
	    String line = lines.get(i);
	    String lineUp = line.toUpperCase();

	    //Check thet there is no multi step array of lines
	    if (lineUp.contains(GaussianConstants.STEPSEPARATOR.toUpperCase()))
	    {
		Terminator.withMsgAndStatus("ERROR! Trying to submit a "
			+ "multistep input to a single Gaussian Step.",-1);
	    }

	    //Identify the section this line belongs to
	    for (String key : GaussianConstants.JOBDETAILSKEYWORDS)
	    {
		if (lineUp.startsWith(key))
		{
		    if (sections.keySet().contains(key))
		    {
			sections.get(key).add(line);
		    } else {
			ArrayList<String> linesInSection = 
						new ArrayList<String>();
                        linesInSection.add(line);
                        sections.put(key,linesInSection);
		    }
		    break;
		}
	    } //end loop over keywords for section
	} //end loop over lines

	//Construct the objects
	if (sections.keySet().contains(GaussianConstants.KEYLINKSEC))
	{
            gLink = new GaussianLinkCommandsSection(
				sections.get(GaussianConstants.KEYLINKSEC));
	} else {
	    gLink = new GaussianLinkCommandsSection();
	}

	if (sections.keySet().contains(GaussianConstants.KEYROUTESEC))
	{
	    gRoute = new GaussianRouteSection(
	                        sections.get(GaussianConstants.KEYROUTESEC));
	} else {
	    gRoute = new GaussianRouteSection();
	}

	if (sections.keySet().contains(GaussianConstants.KEYTITLESEC))
	{
	    for (String part : sections.get(GaussianConstants.KEYTITLESEC))
	    {
		part = part.substring(GaussianConstants.KEYTITLESEC.length());
	        comment = comment + " " + part;
	    }
	} else {
	    comment = "No comment";
	}

        if (sections.keySet().contains(GaussianConstants.KEYMOLSEC))
        {
            gMol = new GaussianMolSpecification(
                                sections.get(GaussianConstants.KEYMOLSEC));
        } else {
            gMol = new GaussianMolSpecification();
        }

        if (sections.keySet().contains(GaussianConstants.KEYOPTSSEC))
        {
            gOpts = new GaussianOptionsSection(
                                   sections.get(GaussianConstants.KEYOPTSSEC));
        } else {
            gOpts = new GaussianOptionsSection();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Return Link 0 section
     * @return the link 0 section
     */

    public GaussianLinkCommandsSection getLinkCommand()
    {
        return gLink;
    }

//------------------------------------------------------------------------------

    /**
     * Set the Link 0 section 
     * @param newGJCS the new link0 section
     */

    public void setLinkCommands(GaussianLinkCommandsSection newGJCS)
    {
	gLink = newGJCS;
    }

//------------------------------------------------------------------------------

    /**
     * Return Route sections
     * @return the route section
     */

    public GaussianRouteSection getRouteSection()
    {
        return gRoute;
    }

//------------------------------------------------------------------------------

    /**
     * Set the Route sections 
     * @param newGJRS the new route section
     */

    public void setRouteSection(GaussianRouteSection newGJRS)
    {
	gRoute = newGJRS;
    }

//------------------------------------------------------------------------------

    /**
     * Return comment 
     * @return the title/comment
     */

    public String getComment()
    {
        return comment;
    }

//------------------------------------------------------------------------------

    /**
     * Set comment 
     * @param newComment the new comment
     */

    public void setComment(String newComment)
    {
	comment = newComment;
    }

//------------------------------------------------------------------------------

    /**
     * Return Options section 
     * @return the options section
     */

    public GaussianOptionsSection getOptionSection()
    {
        return gOpts;
    }

//------------------------------------------------------------------------------

    /**
     * Set all Options section 
     * @param newGJOS ordered list of all options to be set
     */

    public void setOptionsSection(GaussianOptionsSection newGJOS)
    {
        gOpts = newGJOS;
    }

//------------------------------------------------------------------------------

    /**
     * Returns Molecular Specification 
     * @return the molecular specification section
     */

    public GaussianMolSpecification getMolSpec()
    {
        return gMol;
    }

//------------------------------------------------------------------------------

    /**
     * Set the molecular specification 
     * @param newGJMS the new molecular specification section
     */

    public void setMolSpecification(GaussianMolSpecification newGJMS)
    {
	gMol = newGJMS;
    }  

//------------------------------------------------------------------------------

    /**
     * Returns <code>true</code> if this step requires the definition of
     * a geometry in the molecular specification.
     * @return <code>true</code> if this step requires the definition of a
     * geometry in the molecular specification. 
     */

    public boolean needsGeometry()
    {
	if (gRoute.containsKey(GaussianConstants.LABLOUDKEY + 
						  GaussianConstants.GAUKEYGEOM))
	{
	    String opt = gRoute.getValue(GaussianConstants.LABLOUDKEY +
				    GaussianConstants.GAUKEYGEOM).toUpperCase();
	    if (opt.startsWith(GaussianConstants.GAUKEYGEOMCHK) ||
		opt.startsWith(GaussianConstants.GAUKEYGEOMCHECK) ||
		opt.startsWith(GaussianConstants.GAUKEYGEOMALLCHK) ||
		opt.startsWith(GaussianConstants.GAUKEYGEOMSTEP) ||
		opt.startsWith(GaussianConstants.GAUKEYGEOMNGEOM) ||
		opt.startsWith(GaussianConstants.GAUKEYGEOMMOD))
	    {
	        return false;
	    }
	}
        return true;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a list of lines with all the information contained in this object
     * The format corresponds to Gaussian's input file (file.inp)
     * @return the list of lines ready to print a Gaussian input file
     */

    public ArrayList<String> toLinesInp()
    {
        ArrayList<String> lines = new ArrayList<String>();
	
	lines.addAll(gLink.toLinesInp());
	lines.addAll(gRoute.toLinesInp());

	lines.add(comment);
        lines.add("");  //blackline terminated section

        lines.addAll(gMol.toLinesInp());

	lines.addAll(gOpts.toLinesInp());

        return lines;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a list of lines with the information contained in this object.
     * The format corresponds to the format of autocompchem's JobDetail formatted
     * text file and 
     * can be used as input in the constructor of this object.
     * @return the lines of text ready to printa jobDetail file
     */

    public ArrayList<String> toLinesJob()
    {
        ArrayList<String> lines = new ArrayList<String>();

        lines.addAll(gLink.toLinesJob());
        lines.addAll(gRoute.toLinesJob());
	String commentAsJob = GaussianConstants.KEYTITLESEC + comment;
        lines.add(commentAsJob);
        lines.addAll(gMol.toLinesJob());
	lines.addAll(gOpts.toLinesJob());

        return lines;
    }

//------------------------------------------------------------------------------

}
