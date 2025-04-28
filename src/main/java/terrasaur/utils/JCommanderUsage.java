/*
 * The MIT License
 * Copyright © 2025 Johns Hopkins University Applied Physics Laboratory
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package terrasaur.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.Strings;
import com.beust.jcommander.WrappedParameter;
import com.beust.jcommander.internal.Lists;

public class JCommanderUsage {

  private final JCommander jc;
  private final String programName;
  private int m_columnSize = 100;

  private Comparator<? super ParameterDescription> m_parameterDescriptionComparator =
      new Comparator<ParameterDescription>() {
        @Override
        public int compare(ParameterDescription p0, ParameterDescription p1) {
          return p0.getLongestName().compareTo(p1.getLongestName());
        }
      };

  private Comparator<? super ParameterDescription> m_parameterOrderComparator =
      new Comparator<ParameterDescription>() {
        @Override
        public int compare(ParameterDescription p0, ParameterDescription p1) {

          // compare by order
          Parameter orderp0 = p0.getParameterAnnotation();
          Parameter orderp1 = p1.getParameterAnnotation();
          return orderp0.order() - orderp1.order();
          // return p0.getLongestName().compareTo(p1.getLongestName());
        }
      };

  public JCommanderUsage(JCommander jc) {
    this(jc, "");
  }

  public JCommanderUsage(JCommander jc, String programName) {
    this.jc = jc;
    this.programName = programName;
  }

  /**
   * Display the usage for this command.
   */
  public void usage(String commandName) {
    StringBuilder sb = new StringBuilder();
    usage(commandName, sb);
    JCommander.getConsole().println(sb.toString());
  }

  /**
   * Store the help for the command in the passed string builder.
   */
  public void usage(String commandName, StringBuilder out) {
    usage(commandName, out, "");
  }

  /**
   * Store the help for the command in the passed string builder, indenting every line with
   * "indent".
   */
  public void usage(String commandName, StringBuilder out, String indent) {
    String description = null;
    try {
      description = jc.getCommandDescription(commandName);
    } catch (ParameterException e) {
      // Simplest way to handle problem with fetching descriptions for the main command.
      // In real implementations we would have done this another way.
    }

    if (description != null) {
      out.append(indent).append(description);
      out.append("\n");
    }
    // PROBLEM : JCommander jcc = jc.findCommandByAlias(commandName); // Wops, not public!
    JCommander jcc = findCommandByAlias(commandName);
    if (jcc != null) {
      jcc.usage(out, indent);
    }
  }

  private JCommander findCommandByAlias(String commandOrAlias) {
    // Wops, not public and return ProgramName class that neither is public.
    // No way to get around this.
    // PROBLEM : JCommander.ProgramName progName = jc.findProgramName(commandOrAlias);

    // So, then it turns out we cannot mimic the functionality implemented in usage for
    // printing command usage :(
    /*
     * if(progName == null) { return null; } else { JCommander jc = this.findCommand(progName);
     * if(jc == null) { throw new IllegalStateException(
     * "There appears to be inconsistency in the internal command database.  This is likely a bug. Please report."
     * ); } else { return jc; } }
     */
    // Lets go for the solution which is available to us and ignore the logic implemented in
    // JCommander for this lookup.
    return jc.getCommands().get(commandOrAlias);
  }

  /**
   * Allow user to pre-pend a command description before showing the usage and all the options.
   * Indent is an integer count of the number of spaces of indentation desired for the Parameter
   * arguments.
   * 
   * @param out
   * @param indent
   * @param commandDesc
   */
  public void usage(StringBuilder out, int indent, String commandDesc) {

    out.append(commandDesc);
    usage(out, s(indent));

  }

  public void usage(StringBuilder out, String indent) {

    // Why is this done on this stage of the process?
    // Looks like something that should have been done earlier on?
    // Anyway the createDescriptions() method is private and not possible to
    // trigger from the outside.
    // I haven't spend time on considering the consequences of not executing the method.
    /* PROBLEM : if (m_descriptions == null) createDescriptions(); */

    boolean hasCommands = !jc.getCommands().isEmpty();

    //
    // First line of the usage
    //

    // The JCommander does not provide a getProgramName() method and therefore
    // makes it impossible for other usage implementations to use it.
    // My first idea was to use the reflection api to change the access level of
    // the m_programName attribute, but then I saw that the ProgramName class is
    // private as well :(
    // Of course it is possible to set an alternative program name in the usage
    // implementation, but that is kind of a second best solution.

    /*
     * PROBLEM : String programName = m_programName != null ? m_programName.getDisplayName() :
     * "<main class>";
     */
    // String programName = "<who knows>";
    //
    // out.append(indent).append("Usage: ").append(programName).append(" [options]");
    // if (hasCommands) {
    // out.append(indent).append(" [command] [command options]");
    // }
    if (jc.getMainParameterDescription() != null) {
      out.append(" ").append(jc.getMainParameterDescription());
    }
    out.append("\n");

    //
    // Align the descriptions at the "longestName" column
    //
    int longestName = 0;
    List<ParameterDescription> sorted = Lists.newArrayList();
    for (ParameterDescription pd : jc.getParameters()) {
      if (!pd.getParameter().hidden()) {
        sorted.add(pd);
        // + to have an extra space between the name and the description
        int length = pd.getNames().length() + 2;
        if (length > longestName) {
          longestName = length;
        }
      }
    }

    //
    // Sort the options
    //
    // Collections.sort(sorted, getParameterDescriptionComparator());

    Collections.sort(sorted, getParameterOrderComparator());

    //
    // Display all the names and descriptions
    //
    int descriptionIndent = 0;
    if (sorted.size() > 0) {
      // out.append(indent).append(" Options:\n");
      out.append("  Options:\n");
    }
    for (ParameterDescription pd : sorted) {
      WrappedParameter parameter = pd.getParameter();
      // if (!pd.getNames().contains("--")) {
      // namePad = " ";
      // }
      out.append(indent)
          // .append(" ")
          // .append(parameter.required() ? "* " : " ")
          .append(pd.getNames());
      // .append(" ")
      // .append(indent)
      // .append(s(descriptionIndent));
      // int indentCount = indent.length() + descriptionIndent;

      int indentCount = longestName;
      indentCount = pd.getNames().length();
      if (pd.getNames().length() < longestName) {
        indentCount = longestName - pd.getNames().length() - 1;
      }
      // wrapDescription(out, indentCount, pd.getDescription());
      wrapDescriptionInLine(out, indentCount, longestName + 4, pd.getDescription());
      Object def = pd.getDefault();
      if (pd.isDynamicParameter()) {
        out.append("\n").append(s(indentCount + 1)).append("Syntax: ").append(parameter.names()[0])
            .append("key").append(parameter.getAssignment()).append("value");
      }
      if (def != null) {
        String displayedDef =
            Strings.isStringEmpty(def.toString()) ? "<empty string>" : def.toString();
        out.append("\n")
            // .append(s(indentCount + 1))
            .append(s(longestName + 4)).append("Default: ")
            .append(parameter.password() ? "********" : displayedDef);
      }
      out.append("\n");
      out.append("\n");
    }

    //
    // If commands were specified, show them as well
    //
    if (hasCommands) {
      out.append("  Commands:\n");
      // The magic value 3 is the number of spaces between the name of the option
      // and its description
      for (Map.Entry<String, JCommander> commands : jc.getCommands().entrySet()) {
        Object arg = commands.getValue().getObjects().get(0);
        Parameters p = arg.getClass().getAnnotation(Parameters.class);
        // I'm not sure why, but this simply doesn't work in my test project.
        // But this is not important in this POC
        // if (!p.hidden()) {
        String dispName = commands.getKey();
        out.append(indent).append("    " + dispName); // + s(spaceCount) +
                                                      // getCommandDescription(progName.name) +
                                                      // "\n");

        // Options for this command
        usage(dispName, out, "      ");
        out.append("\n");
        // }
      }
    }
  }

  private void wrapDescription(StringBuilder out, int indent, String description) {
    int max = getColumnSize();
    String[] words = description.split(" ");

    // indent before adding any words.
    // out.append(s(indent));
    int current = indent;
    int i = 0;
    while (i < words.length) {
      String word = words[i];
      if (word.length() > max || current + word.length() <= max) {
        out.append(" ").append(word);
        current += word.length() + 1;
      } else {
        out.append("\n").append(s(indent + 1)).append(word);
        current = indent;
      }
      i++;
    }
  }

  /**
   * Wrap the description for the case where the user wants the description to start in-line with
   * the command-line argument.
   * 
   * @param out
   * @param firstIndent
   * @param indent
   * @param description
   */
  private void wrapDescriptionInLine(StringBuilder out, int firstIndent, int indent,
      String description) {
    int max = getColumnSize();
    description = description.replaceAll("\\n", " .CR.");
    String[] words = description.split(" ");

    // first indent before adding any words.
    out.append(s(firstIndent));
    int current = indent;
    int i = 0;
    while (i < words.length) {
      String word = words[i];
      if (word.equals(".CR.")) {
        out.append("\n").append(s(indent - 1));
        current = indent;
      } else {
        if (word.length() > max || current + word.length() <= max) {
          out.append(" ").append(word);
          current += word.length() + 1;
        } else {
          out.append("\n").append(s(indent)).append(word);
          current = indent;
        }
      }
      i++;
    }
  }

  private Comparator<? super ParameterDescription> getParameterDescriptionComparator() {
    return m_parameterDescriptionComparator;
  }

  private Comparator<? super ParameterDescription> getParameterOrderComparator() {
    return m_parameterOrderComparator;
  }

  public void setParameterDescriptionComparator(Comparator<? super ParameterDescription> c) {
    m_parameterDescriptionComparator = c;
  }

  public void setColumnSize(int m_columnSize) {
    this.m_columnSize = m_columnSize;
  }

  public int getColumnSize() {
    return m_columnSize;
  }

  /**
   * @return n spaces
   */
  private String s(int count) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < count; i++) {
      result.append(" ");
    }

    return result.toString();
  }
}
