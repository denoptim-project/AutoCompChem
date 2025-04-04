package autocompchem.wiro.chem;

public interface IValueContainer extends IDirectiveComponent 
{

//-----------------------------------------------------------------------------
    
    /**
     * Sets the value of this component
     */
    public void setValue(Object value);

//-----------------------------------------------------------------------------

    /**
     * Gets the value of this component
     * @return the value of this container.
     */
    public Object getValue();
    
//-----------------------------------------------------------------------------
    
    /**
     * Removes the value of this component
     */
    public void removeValue();
    
//-----------------------------------------------------------------------------

}
