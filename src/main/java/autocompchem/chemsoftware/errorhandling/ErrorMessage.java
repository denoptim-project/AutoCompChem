package autocompchem.chemsoftware.errorhandling;

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
import java.util.List;
import java.util.Map;

import autocompchem.run.Terminator;

/**
 * Object representing an error message obtained from a software and the 
 * most rated attempt to solve the problem leading to the error.
 * An error is defined as an event that does not correspond to the normal
 * result of a computation. The error is identified by a unique name, which is
 * an alphanumerical string, and is characterized by the simultaneous 
 * verification of a predetermined set of conditions. The set of conditions
 * is organized in this way:
 * <ul type=disc>
 * <li>the pure <b>message</b>: a string of characters (including spaces) 
 * that correspond to the classical concept of human readable error message.
 * </li>
 * <li>a set of <b>further conditions</b> stores as an array of strings in 
 * which each entry (a string) represents a separate condition. The content
 * of the string is only relevant when the array of conditions is deployed 
 * by methods capable of interpreting software output and sets of 
 * <code>ErrorMessage</code>. See the documentation of the routines
 * handling the output of a specific software under the 
 * <code>autocompchem.chemsoftware</code> packages for a list of further conditions 
 * implemented
 * </ul>
 * The <code>ErrorMessage</code> can be accompanied by an 
 * <b>error fixing action</b>. Once the error is understood, that is we 
 * are dealing with an event that is known because it can be recognized by 
 * the verified set of conditions, if a systematic solution exists, that 
 * solution is codified into the action field of this 
 * <code>ErrorMessage</code>. The action is defined by a type (field 
 * <code>typeOfAction</code>) and the details stored into the 
 * <code>actionDetails</code>. The content of the details filed is not 
 * interpreted by <code>ErrorMessage</code> which only stores the information.
 * <br>
 * All this information can be provided to the constructors as objects or
 * a as text organized into an array. See the documentation of the constructor.
 * 
 * @author Marco Foscato
 */

public class ErrorMessage
{
    //Reference name
    private String name;

    //Error messages
    private List<String> errMsg;

    //Further conditions
    private List<String> conditions;

    //Resolution action plan
    private String typeOfAction;

    //STring representation of the action's details
    private Map<String,String> actionDetails;

//------------------------------------------------------------------------------

    /**
     * Construct an empty ErrorMessage
     */

    public ErrorMessage()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Construct an <code>ErrorMessage</code> from formatted text given as an
     * <code>List</code> in which each item is a line of formatted text to
     * be interpreted. The lines are expected to be ordered only for 
     * Labelled blocks (see below), that is, the beginning of the block must
     * come before the end.
     * The format of the text is divided into three categories:
     * <ol>
     * <li><b>Keywords</b>: a single line starting with a keyword (no 
     * leading spaces) followed by a separator (<code>:</code>), and continuing
     *  with one or more arguments.</li>
     * <li><b>Labeled Blocks</b>: one or more lines identified by a defined 
     * beginning (label <code>$START</code>), a keyword (a single word 
     * immediately following the beginning label), and an well defined end 
     * (label <code>$END</code>). Labeled blocks are used to provide complex 
     * information, such as the details for error fixing actions.</li>
     * <li><b>Comments</b>: each line starting with <code>#</code> is ignored
     * unless is part of a labelled block.</li>
     * </ol>
     * According to this scheme the following keywords are required to define
     *  <code>ErrorMessage</code> from a text file:
     * <ul>
     * <li><b>ReferenceName</b>: the argument of this keywords has to be a 
     * string identifying the error; mainly useful for logging purposes.</li>
     * <li><b>ErrorMessage</b>: the argument of this keyword is a phrase or 
     * a word that is written by a software in a log/output file to report
     * the error event.</li>
     * </ul>
     * Optionally, error fixing actions may be defined as follows:
     * <ul>
     * <li><b>Action</b>: a single word defining the type of action to be 
     * undertaken in case the error is found and recognised.</li>
     * <li>detailed information needed to perform the error fixing action
     *  is provided in the form of labelled blocks according to the needs
     *  of the specific case.</li>
     * </ul>
     * <br>
     * In practice all this information can be stored in a formatted
     *  text file. An example is reported below.<br>
     * <br>
     * <code> 
     * ----- beginning of text file -----
     * <br>
     * # this is a comment because it starts with '#'<br>
     * <b>ReferenceName</b>: here_goes_the_name_of_this_error<br>
     * <b>ErrorMessage</b>: this is the actual message printed in the 
     * output/log of by some software<br>
     * <b>Condition</b>: type_of_action  argument_for_the_action<br>
     * <b>Condition</b>: 2nd_type_of_action  argument_for_the_2nd_action<br>
     * <b>Action</b>: type_of_error_fixing_action
     * #Note where id the keyword for the following labelled block: right 
     * after the $START label.<br>
     * <b>$STARTthis_is_the_keyword</b><br>
     * ... bla bla, details for error fixing action...<br>
     * # By the way, this is NOT a comment as it belong to a labelled block
     *  of text. Thus it will be transmitted to the routines performing the
     *  error fixing action.<br>
     * ... bla bla, some other details for error fixing action...<br>
     * <b>$END</b><br>
     * <b>$STARTthis_is_the_2nd_keyword</b><br>
     * ... bla bla, second block of details for error fixing action...<br>
     * ... bla bla, still for the 2nd block of details...<br>
     * <b>$END</b><br>
     * ----- end of text file ----- <br>
     * </code>
     *
     * @param form the table of formatted text
     */

    public ErrorMessage(List<List<String>> form)
    {
        String refName = "";
        String action = "";
        List<String> errLines = new ArrayList<String>();
        List<String> conditions = new ArrayList<String>();
        Map<String,String> details = new HashMap<String,String>();
        boolean refNameFound = false;
        boolean actionFound = false;
        for (int i=0; i<form.size(); i++)
        {
            List<String> signleBlock = form.get(i);
            String key = signleBlock.get(0).toUpperCase();
            String value = signleBlock.get(1);
            switch (key)
            {
                case "REFERENCENAME":
                    //Reference name of the error
                    if (!refNameFound)
                    {
                        refNameFound = true;
                        refName = value;
                        if (refName.equals(""))
                        {
                            Terminator.withMsgAndStatus("ERROR! Empty "
                                    + " 'REFERENCENAME' while defining"
                                    + " ErrorMessage.", -1);
                        }
                    } else {
                        Terminator.withMsgAndStatus("ERROR! Multiple "
                                    + "'REFERENCENAME' while defining" 
                                    + " ErrorMessage.", -1);
                    }
                    break;
                 case "ERRORMESSAGE":
                    //Lines of the error message
                    value = value.trim();
                    errLines.add(value);
                    break;

                case "ACTION":
                    //Action
                    if (!actionFound)
                    {
                        actionFound = true;
                        action = value;
                        if (action.equals(""))
                        {
                            Terminator.withMsgAndStatus("ERROR! Empty "
                                    + " 'ACTION' while defining" 
                                    + " ErrorMessage.", -1);
                        }
                    } else {
                        Terminator.withMsgAndStatus("ERROR! Multiple "
                                    + "'ACTION' while defining" 
                                    + " ErrorMessage.", -1);
                    }
                    break;
                 case "CONDITION":
                    conditions.add(value);
                    break;
                default:
                    //Other details added as string with label
                    details.put(key,value);
            } //end of switch
        } //end of loop on array of pairs key:value

        this.name = refName;
        this.errMsg = errLines;
        this.conditions = conditions;
        this.typeOfAction = action;
        this.actionDetails = details;
    }

//------------------------------------------------------------------------------

    /**
     * Construct an ErrorMessage from reference name and error message
     * @param name the reference name of the error to be constructed
     * @param errMsg the array of lines representing the error message
     */

    public ErrorMessage(String name, List<String> errMsg)
    {
        this.name = name;
        this.errMsg = errMsg;
        this.conditions = new ArrayList<String>();
        this.typeOfAction = "";
        this.actionDetails = new HashMap<String,String>();
    }

//------------------------------------------------------------------------------

    /**
     * Construct an ErrorMessage with error-solving action detail
     * @param name the reference name of the error to be constructed
     * @param errMsg the array of lines representing the error message
     * @param conditions the array of conditions to be satisfied
     * @param action type of action to solve the problem leading to this error
     * @param details string representation of the object needed to perform the
     * action
     */

    public ErrorMessage(String name, List<String> errMsg,
                                List<String> conditions, String action, 
                                Map<String,String> details)
    {
        this.name = name;
        this.errMsg = errMsg;
        this.conditions = conditions;
        this.typeOfAction = action;
        this.actionDetails = details;
    }

//------------------------------------------------------------------------------

    /**
     * Add a line in the error message characterising this error
     * @param errLine the line to add
     */

    public void addErrorMessageLine(String errLine)
    {
        this.errMsg.add(errLine);
    }

//------------------------------------------------------------------------------

    /**
     * Get the reference name of this error
     * @return the name of this error
     */

    public String getName()
    {
        return this.name;
    }

//------------------------------------------------------------------------------

    /**
     * Get the whole list of lines characterising the error message
     * @return the list of lines of text characterising this error message
     */

    public List<String> getErrorMessage()
    {
        return this.errMsg;
    }

//------------------------------------------------------------------------------

    /**
     * Get the map of conditions to be verified by this error
     * @return the list of additional conditions (as strings)
     */

    public List<String> getConditions()
    {
        return conditions;
    }

//------------------------------------------------------------------------------

    /**
     * Get the string representing the type of protocol to fix the problem
     * @return the error fixing action (as string)
     */

    public String getErrorFixingAction()
    {
        return typeOfAction;
    }

//------------------------------------------------------------------------------

    /**
     * Get the string representing the type of protocol to fix the problem 
     * @return the map of details for the error fixing action
     */

    public Map<String,String> getErrorFixingActionDetails()
    {
        return actionDetails;
    }

//------------------------------------------------------------------------------

    /**
     * Set the reference name of this error to a new value
     * @param name the new reference name
     */

    public void setName(String name)
    {
        this.name = name;
    }

//------------------------------------------------------------------------------

    /**
     * Set the whole list of error messages characterising that are used to 
     * identify this <code>ErrorMessage</code>
     * @param errMsg the list of lines of text identifying this error message
     */

    public void setErrorMessage(List<String> errMsg)
    {
        this.errMsg = errMsg;
    }

//------------------------------------------------------------------------------

    /**
     * Set the map of conditions for this error
     * @param conditions the list of additional conditions as strings
     */

    public void setConditions(List<String> conditions)
    {
        this.conditions = conditions;
    }

//------------------------------------------------------------------------------

    /**
     * Set the string representing the type of protocol to fix the problem
     * @param typeOfAction string identifying the action
     */

    public void setErrorFixingAction(String typeOfAction)
    {
        this.typeOfAction = typeOfAction;
    }

//------------------------------------------------------------------------------

    /**
     * Set the string representation of the object containing all the details 
     * for 
     * defining the solution of the problem, or at least trying to solve the 
     * problem.
     * @param details the map containing all the details
     */

    public void setErrorFixingActionDetails(Map<String,String> details)
    {
        this.actionDetails = details;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a string representation of this error
     * @return the string representation
     */

    public String toString()
    {
        String str = "ReferenceName: " + name + "\n";
        for (String s : errMsg)
            str = str + "ErrorMessage: " + s + "\n";
        for (String value : conditions)
        {
            if (value.contains("\n"))
                str = str + "$START" + "Condition: " + value + "\n$END\n";
            else
                str = str + "Condition: " + value + "\n";
        }

        str = str + "Action: " + typeOfAction + "\n";
        for (String key : actionDetails.keySet())
        {
            String value = actionDetails.get(key);
            if (value.contains("\n"))
                str = str + "$START" + key + ": " + value + "\n$END\n";
            else
                str = str + key + ": " + value + "\n";
        }

        return str;
    }

//------------------------------------------------------------------------------

}
