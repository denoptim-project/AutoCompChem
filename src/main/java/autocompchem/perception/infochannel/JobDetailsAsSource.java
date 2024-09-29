package autocompchem.perception.infochannel;


import autocompchem.wiro.chem.CompChemJob;


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
