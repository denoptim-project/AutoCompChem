package autocompchem.perception.situation;

/*
 *   Copyright (C) 2018  Marco Foscato
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
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import autocompchem.perception.concept.Concept;
import autocompchem.perception.circumstance.ICircumstance;
import autocompchem.perception.infochannel.InfoChannelType;
import autocompchem.workflow.task.IAction;

import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.FunctionMapper;
import javax.servlet.jsp.el.VariableResolver;

import org.apache.commons.el.ExpressionEvaluatorImpl;


/**
 * A situation is a concept in a context, which is defined by a satisfied set 
 * of circumstances, and may trigger a response/impusle.
 *
 * @author Marco Foscato
 */

public class Situation extends Concept
{
    /**
     * Details describing this situation
     */
    private String description = "no detail";

    /**
     * The context that characterize this situation is defined by a set 
     * of circumstances
     */
     private ArrayList<ICircumstance> context = new ArrayList<ICircumstance>();

    /**
     * Default logical expression for evaluating overall context
     */
    private final String DEFLOGICAL = "none";

    /**
     * Logical expression for evaluating overall context
     */
    private String logicalExpression = DEFLOGICAL;

    /**
     * The impulse or response triggered by this situation
     */
    private ArrayList<IAction> impulse = new ArrayList<IAction>();

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
     * Constructor for a Situation with given type
     * @param conceptType the type of Situation
     */

    public Situation(String conceptType)
    {
        super(conceptType);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a Situation with given type and context.
     * @param conceptType the type on situation to construct
     * @param context the context as a list of circumstances
     */

    public Situation(String conceptType, ArrayList<ICircumstance> context)
    {
        super(conceptType);
        this.context = context;
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
     * Return the list of circumstances that identify the context surrouding 
     * the concept of this situation
     * @return the circumstances
     */

    public ArrayList<ICircumstance> getCircumstances()
    {
        return context;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the logical expression for drawing conclusions from the satisfation
     * of the circumstances.
     */
 
    public void setLogicalExpression(String el)
    {
        this.logicalExpression = el;
    }

//------------------------------------------------------------------------------

    /**
     * Extract all the information channel types involved in the cefinition of
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
     * Map of variables that links (i.e., resolves) the name of a variable 
     * with its value, whathever that might be.
     */

    private class MyVariableResolver implements VariableResolver 
    {
        private HashMap vars;
        public MyVariableResolver(HashMap vars)
        {
            this.vars = vars;
        }

        /**
         * @return the value of a given variable, whayever that might be
         */

        public Object resolveVariable(String pName)
        {
            return vars.get(pName);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Checks if all circumstances are satisfied. This method evaluates 
     * the logical expression that puts in relation all the 
     * circumstances and returns the boolean result.
     * @param fingerprint is the list of satisfation flags one for each 
     * circumstance in the same order of the circumstances.
     * @return the result of the logical expression.
     */

    public boolean isOccurring(ArrayList<Boolean> fingerprint) throws Exception
    {
        boolean res = false;
        if (fingerprint.size() != context.size())
        {
            //TODO move to ERROR
            throw new Exception("ERROR! Number of satisfation flags ("
                                 + fingerprint.size() + ") "
                                 + "differs from the number of "
                                 + "circumstances (" + context.size() + ")");
        }


        HashMap<String,Object> vars = new HashMap<String,Object>();
        for (int i=0; i<fingerprint.size(); i++)
        {
            vars.put("v" + Integer.toString(i),fingerprint.get(i));
        }

        VariableResolver variableResolver = new MyVariableResolver(vars);
        ExpressionEvaluator evaluator = new ExpressionEvaluatorImpl();

        if (logicalExpression.equals(DEFLOGICAL))
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
            Object value = evaluator.evaluate(logicalExpression,
                           String.class,  //class of the returned value
                           variableResolver, //variable names-to-values map
                           null); // name-to-function map (not needed)

            if (value != null) 
            {
                String[] words = ((String) value).split("\\s+");
                if (words.length > 1)
                {
                    //TODO warning?
                    throw new Exception("Evaluation of Expression "
                            + "'" + logicalExpression + "' "
                            + "returned more than one word (i.e., '" 
                            + value + "'). Check expression.");
                }
                else
                {
                    switch (words[0])
                    {
                        case ("true"):
                            res = true;
                            break;
                        case ("false"):
                            res = false;
                            break;
                        default:
                            //TODO warning?
                            throw new Exception("Evaluation of Expression "
                            + "'" + logicalExpression 
                            + "' did not return a Boolean (i.e., '"
                            + value + "'). Check expression.");
                    }
                }
            }
            else
            {
            //TODO warning?
                 throw new Exception("Evaluation of Expression Language "
                   + "returned null instead of boolean. Check expression.");
            }
        }
        catch (Throwable t)
        {
            //TODO error or warning?
            throw new Exception("ERROR in Expression Language! " 
                    + "Expression '" + logicalExpression + "' triggers " 
                    + t);
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
        sb.append("]; Impusle [");
        first = true;
        for (IAction a : impulse)
        {
            if (first)
            {
                sb.append(a.toString());
                first = false;
            }
            else
            {
                sb.append(";").append(a.toString());
            }

        }
        sb.append("]]");
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
