package autocompchem.perception.infochannel;


/*
 *   Copyright (C) 2018  Marco Foscato
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


import autocompchem.chemsoftware.CompChemJob;


/**
 * Class projecting the details of a {@link CompChemJob} into an information
 * channel.
 *
 * @author Marco Foscato
 */

public class JobDetailsAsSource extends InfoChannel
{
    /**
     * Text organized by lines
     */
    public final CompChemJob job;

//------------------------------------------------------------------------------

    /**
     * Constructs an empty ShortTextAsSource
     */

    public JobDetailsAsSource(CompChemJob job)
    {
        super();
        this.job = job;
    }

//------------------------------------------------------------------------------

    /**
     * Return a human readable description
     * @return a string
     */

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("JobDetailsAsSource [ICType:").append(super.getType());
        sb.append(", job:").append(job.getId());
        sb.append("]");
        return sb.toString();
    }
    
//------------------------------------------------------------------------------

}
