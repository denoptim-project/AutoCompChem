package autocompchem.wiro.chem.gaussian.legacy;

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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import autocompchem.wiro.chem.gaussian.GaussianConstants;

/**
 * 
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
                throw new IllegalArgumentException("Trying to submit a "
                        + "multistep input to a single Gaussian Step.");
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
