package autocompchem.perception.situation;


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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import autocompchem.io.ACCJson;
import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.concept.Concept;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.run.jobediting.Action;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;


/**
 * A situation is a concept in a context. 
 * The context is defined by a satisfied set of circumstances. 
 * A situation, once verified, can trigger a response or reaction.
 *
 * @author Marco Foscato
 */

public class Situation extends Concept implements Cloneable
{   
    /**
     * Details describing this situation
     */
    private String description = "no detail";

    /**
     * The context that characterize this situation is defined by a set 
     * of circumstances
     */
     private List<ICircumstance> context = new ArrayList<ICircumstance>();

    /**
     * Logical expression for evaluating overall context
     */
    private String logicalExpression;

    /**
     * The reaction triggered by this situation.
     */
    private Action reaction;

//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public Situation()
    {
        super();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a Situation with given type and context.
     * @param conceptType the type of situation (i.e., error, warning,...).
     * @param refName the name of the situation (i.e., Error-1.3C).
     * @param context the context as a list of circumstances.
     */

    public Situation(String conceptType, String refName)
    {
        this(conceptType, refName, null, null);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a Situation with given type and context.
     * @param conceptType the type on situation to construct
     * @param context the context as a list of circumstances
     */

    public Situation(String conceptType, List<ICircumstance> context)
    {
        this(conceptType, "noRefName", context, null);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a Situation with given type and context.
     * @param conceptType the type of situation (i.e., error, warning,...).
     * @param refName the name of the situation (i.e., Error-1.3C).
     * @param context the context as a list of circumstances.
     */

    public Situation(String conceptType, String refName,
            List<ICircumstance> context)
    {
        this(conceptType, refName, context, null);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a Situation with given type and context.
     * @param conceptType the type of situation (i.e., error, warning,...).
     * @param refName the name of the situation (i.e., Error-1.3C).
     * @param context the context as a list of circumstances.
     * @param reaction the action to be triggered by the perception of this
     * situation.
     */

    public Situation(String conceptType, String refName,
            List<ICircumstance> context, Action reaction)
    {
        super(conceptType,refName);
        if (context!=null)
            this.context = context;
        this.reaction = reaction;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Converts a JSON string into a Situation.
     * @param json the JSON string to deserialize.
     * @return the Situation defined in the JSON string.
     */
    public static Situation fromJSON(String json)
    {
    	return ACCJson.getReader().fromJson(json, Situation.class);
    }
  
//------------------------------------------------------------------------------

    /**
     * Append description or additional information as a string.
     * @param txt the description
     */
    public void setDescription(String txt)
    {
        this.description = txt;
    }

//------------------------------------------------------------------------------

    /**
     * Append a circumstance to the context
     * @param circumstance to be added
     */

    public void addCircumstance(ICircumstance circumstance)
    {
        context.add(circumstance);
    }

//------------------------------------------------------------------------------

    /**
     * Return the list of circumstances that identify the context surrounding 
     * the concept of this situation
     * @return the circumstances
     */

    public List<ICircumstance> getCircumstances()
    {
        return context;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the logical expression for drawing conclusions from the satisfaction
     * of the circumstances.
     */
 
    public void setLogicalExpression(String el)
    {
        this.logicalExpression = el;
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the reaction triggered by the manifesting of this situation.
     * @param reaction the reaction to this situation.
     */

    public void setReaction(Action reaction)
    {
        this.reaction = reaction;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if this known situation can trigger a reaction.
     * @return <code>true</code> a reaction to this situation is defined.
     */
    public boolean hasReaction()
    {
        return reaction != null;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the expected reaction to this situation, or null if no reaction
     * is known.
     * @return the behavior in response of the occurrence of this situation
     */
    
    public Action getReaction()
    {
        return reaction;
    }

//------------------------------------------------------------------------------

    /**
     * Extract all the information channel types involved in the definition of
     * this situation's context
     * @return all relevant information channel types
     */

    public Set<InfoChannelType> getInfoChannelTypes()
    {
        Set<InfoChannelType> allIct = new HashSet<InfoChannelType>();
        for (ICircumstance ic : context)
        {
            allIct.add(ic.getChannelType());
        }
        return allIct;
    }

//------------------------------------------------------------------------------

    /**
     * Checks if all circumstances are satisfied. This method evaluates 
     * the logical expression that puts in relation all the 
     * circumstances and returns the boolean result.
     * @param fingerprint is the list of satisfaction flags one for each 
     * circumstance in the same order of the circumstances.
     * @return the result of the logical expression.
     */

    public boolean isOccurring(ArrayList<Boolean> fingerprint) throws Exception
    {
        boolean res = false;
        if (fingerprint.size() != context.size())
        {
            //TODO move to ERROR
            throw new Exception("ERROR! Number of satisfaction flags ("
                                 + fingerprint.size() + ") "
                                 + "differs from the number of "
                                 + "circumstances (" + context.size() + ")");
        }

        HashMap<String,Object> vars = new HashMap<String,Object>();
        for (int i=0; i<fingerprint.size(); i++)
        {
            vars.put("v" + Integer.toString(i), fingerprint.get(i));
        }
        
        ExpressionFactory expFactory = ExpressionFactory.newInstance();
        ELContext ncc = new ELContext() {
            VariableMapper vm = new VariableMapper() {
                @Override
                public ValueExpression resolveVariable(String varName) 
                {
                    ValueExpression ve = new ValueExpression() 
                    {
                        Object value;
                        
                        /**
                         * Version ID
                         */
                        private static final long serialVersionUID = 1L;

                        @SuppressWarnings("unchecked")
						@Override
                        public Object getValue(ELContext context) {
                            if (vars.containsKey(varName))
                            {
                                value = vars.get(varName);
                            } else {
                                throw new ELException("Variable '" + varName
                                        + "' cannot be resolved.");
                            }
                            return value;
                        }

                        // This should not make sense since this is read-only
                        @Override
                        public void setValue(ELContext context, Object value) {
                            this.value = value;
                        }

                        @Override
                        public boolean isReadOnly(ELContext context) {
                            return true;
                        }

                        @Override
                        public Class<?> getType(ELContext context) {
                            return value.getClass();
                        }

                        @Override
                        public Class<?> getExpectedType() {
                            return Boolean.class;
                        }

                        @Override
                        public String getExpressionString() {
                            return null;
                        }

                        @Override
                        public boolean equals(Object obj) {
                            return false;
                        }

                        @Override
                        public int hashCode() {
                            return Objects.hash(value);
                        }

                        @Override
                        public boolean isLiteralText() {
                            return false;
                        }
                    };
                    return ve;
                }

                // Read-only
                @Override
                public ValueExpression setVariable(String variable, 
                        ValueExpression expression) 
                {
                    return null;
                }
            };
       
            @Override
            public ELResolver getELResolver() {
                //None
                return null;
            }

            @Override
            public FunctionMapper getFunctionMapper() {
                // None
                return null;
            }

            @Override
            public VariableMapper getVariableMapper() {
                return vm;
            }
        };
        
        if (logicalExpression==null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("${");
            for (int i=0; i<fingerprint.size(); i++)
            {
                sb.append("v" + i );
                if (i<(fingerprint.size()-1))
                {
                    sb.append(" && ");
                }
            }
            sb.append("}");
            logicalExpression = sb.toString();
        }
        
        try
        {
            ValueExpression ve = expFactory.createValueExpression(ncc, 
                    logicalExpression, Boolean.class);
            Object value = ve.getValue(ncc);

            if (value != null) 
            {
                if (value instanceof Boolean)
                {
                    res = ((Boolean) value).booleanValue();
                } else {
                    throw new Exception("Evaluation of Expression "
                            + "'" + logicalExpression + "' "
                            + "returned '" + value.getClass() + "'). "
                                    + "Check expression.");
                }
            }
            else
            {
                //TODO error?
                throw new Exception("Evaluation of Expression Language "
                        + "returned null instead of Boolean. "
                        + "Check expression.");
            }
        }
        catch (Throwable t)
        {
            //TODO error or warning?
            throw new Exception("ERROR in Expression Language! " 
                    + "Expression '" + logicalExpression + "' triggers " 
                    + t, t);
        }

        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Return a human readable representation of this Situation
     * @return a string
     */

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Situation [").append(super.toString());
        sb.append("; Context [");
        boolean first = true;
        for (ICircumstance c : context)
        {
            if (first)
            {
                sb.append(c.toString());
                first = false;
            }
            else
            {
                sb.append("; ").append(c.toString());
            }
        }
        sb.append("]; Impulse [");
        if (reaction != null)
        {
            sb.append(reaction.toString());
        } else
        {
            sb.append("none");
        }
        sb.append("]]");
        return sb.toString();
    }
  	
//-----------------------------------------------------------------------------

  	@Override
  	public Situation clone()
  	{
  		// Shortcut via json serialization to avoid implementing Cloneable
  		// in all implementations of ICircumstance
  		Situation clone = ACCJson.getReader().fromJson(
  				ACCJson.getWriter().toJson(this), Situation.class);
  		return clone;
  	}

//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (o == null)
            return false;
        
        if (o == this)
            return true;
        
        if (o.getClass() != getClass())
        	return false;
         
        Situation other = (Situation) o;
         
        if (!this.description.equals(other.description))
            return false;

        if (this.logicalExpression!=null 
        		&& !this.logicalExpression.equals(other.logicalExpression))
            return false;
       
        if (this.context.size()!=other.context.size())
            return false;
       
        for (int i=0; i<this.context.size(); i++)
            if (!this.context.get(i).equals(other.context.get(i)))
                return false;
                
        if ((this.reaction!=null && other.reaction==null) 
        	|| (this.reaction!=null && other.reaction==null))
        	return false;
        
        if (this.reaction!=null && other.reaction!=null
        	&& !this.reaction.equals(other.reaction))
        	return false;
        
        return super.equals(other);
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public int hashCode()
    {
    	return Objects.hash(description, logicalExpression, context, reaction,
    			super.hashCode());
    }
    
//------------------------------------------------------------------------------

}
