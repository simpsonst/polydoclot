/*
 * Copyright 2018,2019, Lancaster University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 * 
 *  * Neither the name of the copyright holder nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.polydoclot.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Converts properties into a string according to some scheme specified
 * as a line of text containing macros.
 * <p>
 *
 * Special characters in the scheme are braces and colons. A backslash
 * is used to escape.
 *
 * <dl>
 *
 * <dt><code>&#123;<var>NAME</var>&#125;</code></dt>
 *
 * <dd>Expands to the value of the named property.</dd>
 *
 *
 * <dt><code>&#123;?<var>NAME</var>:<var>TRUTH</var>:<var>FALSEHOOD</var>&#125;</code></dt>
 *
 * <dd>Expands to <var>TRUTH</var> if the named property exists, or to
 * <var>FALSEHOOD</var> otherwise. These default to 1 and 0 if neither
 * are specified, or <var>FALSEHOOD</var> defaults to an empty string if
 * only <var>TRUTH</var> is specified.</dd>
 *
 *
 * <dt><code>&#123;#<var>PREFIX</var>&#125;</code></dt>
 *
 * <dd>Looks for properties called <var>PREFIX</var>.0,
 * <var>PREFIX</var>.1, etc, and yields the total count of consecutive
 * entries.</dd>
 *
 *
 * <dt><code>&#123;$<var>TEXT</var>:<var>PATTERN</var>:<var>REPLACEMENT</var>&#125;</code></dt>
 *
 * <dd>Applies <code><var>TEXT</var>.{@linkplain
 * String#replaceAll(String,String) replace}(<var>PATTERN</var>,
 * <var>REPLACEMENT</var>)</code>.</dd>
 *
 *
 * <dt><code>&#123;.<var>TEXT</var>:<var>TIMES</var>&#125;</code></dt>
 *
 * <dd>Generates <var>TEXT</var> <var>TIMES</var> times.</dd>
 *
 *
 * <dt><code>&#123;@<var>PREFIX</var>:<var>NAME</var>:<var>EXPANSION</var>:<var>SEPARATOR</var>&#125;</code></dt>
 *
 * <dd>Enumerates over the numeric arguments of the form
 * <var>PREFIX</var>.0, <var>PREFIX</var>.1, etc, yielding
 * <var>EXPANSION</var> with the property <var>NAME</var> temporarily
 * containing 0, 1, 2, etc. The optional separator is also generated
 * between each expansion.</dd>
 *
 * </dl>
 *
 * @author simpsons
 */
public class MacroFormatter {
    /**
     * The default character used to escape literal characters of
     * significance
     */
    public static final char ESCAPE_CHAR = '\\';

    /**
     * The default character used to open a macro sequence
     */
    public static final char OPEN_CHAR = '{';

    /**
     * The default character used to close a macro sequence
     */
    public static final char CLOSE_CHAR = '}';

    /**
     * The default character used to separate arguments within a macro
     * sequence
     */
    public static final char ARG_SEP = ':';

    /**
     * The default character used to signify an enumeration
     */
    public static final char ENUM_CMD = '@';

    /**
     * The default character used to signify a repetition
     */
    public static final char TIMES_CMD = '.';

    /**
     * The default character used to signify a regexp replacement
     */
    public static final char REPLACE_CMD = '$';

    /**
     * The default character used to signify a property count
     */
    public static final char COUNT_CMD = '#';

    /**
     * The default character used to signify a test for existence
     */
    public static final char IF_CMD = '?';

    /**
     * Specifies the characters used to parse a macro format string.
     * 
     * @author simpsons
     */
    public static class Syntax {
        char escapeChar = ESCAPE_CHAR, openChar = OPEN_CHAR,
            closeChar = CLOSE_CHAR, argSep = ARG_SEP, enumCmd = ENUM_CMD,
            timesCmd = TIMES_CMD, replaceCmd = REPLACE_CMD,
            countCmd = COUNT_CMD, ifCmd = IF_CMD;

        private Syntax() {}

        /**
         * Set the escape character.
         * 
         * @param c the new character
         * 
         * @return this object
         */
        public Syntax escapeOn(char c) {
            this.escapeChar = c;
            return this;
        }

        /**
         * Set the opening character.
         * 
         * @param c the new character
         * 
         * @return this object
         */
        public Syntax openOn(char c) {
            this.openChar = c;
            return this;
        }

        /**
         * Set the closing character.
         * 
         * @param c the new character
         * 
         * @return this object
         */
        public Syntax closeOn(char c) {
            this.closeChar = c;
            return this;
        }

        /**
         * Set the argument separator.
         * 
         * @param c the new character
         * 
         * @return this object
         */
        public Syntax separateOn(char c) {
            this.argSep = c;
            return this;
        }

        /**
         * Set the enumeration character.
         * 
         * @param c the new character
         * 
         * @return this object
         */
        public Syntax enumerateOn(char c) {
            this.enumCmd = c;
            return this;
        }

        /**
         * Set the repetition character.
         * 
         * @param c the new character
         * 
         * @return this object
         */
        public Syntax repeatOn(char c) {
            this.timesCmd = c;
            return this;
        }

        /**
         * Set the replacment character.
         * 
         * @param c the new character
         * 
         * @return this object
         */
        public Syntax replaceOn(char c) {
            this.replaceCmd = c;
            return this;
        }

        /**
         * Set the counting character.
         * 
         * @param c the new character
         * 
         * @return this object
         */
        public Syntax countOn(char c) {
            this.closeChar = c;
            return this;
        }

        /**
         * Set the test character.
         * 
         * @param c the new character
         * 
         * @return this object
         */
        public Syntax testOn(char c) {
            this.ifCmd = c;
            return this;
        }

        /**
         * Create the formatter with the current syntax and a pattern.
         * 
         * @param format the pattern to use
         * 
         * @return the new formatter
         * 
         * @constructor
         */
        public MacroFormatter create(String format) {
            return new MacroFormatter(this, format);
        }
    }

    /**
     * Create a default syntax.
     * 
     * @return a fresh default syntax
     * 
     * @constructor
     */
    public static Syntax syntax() {
        return new Syntax();
    }

    private final char escapeChar, openChar, closeChar, argSep, enumCmd,
        timesCmd, replaceCmd, countCmd, ifCmd;

    /**
     * The raw format
     */
    public final String format;

    private final Element rootElement;

    /**
     * Create a macro formatter using the default syntax.
     *
     * @param format the scheme to use
     */
    public MacroFormatter(String format) {
        this(new Syntax(), format);
    }

    MacroFormatter(Syntax syntax, String format) {
        this.escapeChar = syntax.escapeChar;
        this.openChar = syntax.openChar;
        this.closeChar = syntax.closeChar;
        this.argSep = syntax.argSep;
        this.enumCmd = syntax.enumCmd;
        this.timesCmd = syntax.timesCmd;
        this.replaceCmd = syntax.replaceCmd;
        this.countCmd = syntax.countCmd;
        this.ifCmd = syntax.ifCmd;
        this.format = format;
        List<Element> rootSequence = new ArrayList<Element>(10);
        parseSequence(rootSequence, format, true);
        rootElement = optimize(rootSequence);
    }

    private List<Escaper> escapers = new ArrayList<>();

    /**
     * Add an additional level of escaping to literal content.
     * 
     * @param escaper the escaping to be applied
     */
    public void escape(Escaper escaper) {
        escapers.add(escaper);
    }

    private static Element optimize(List<? extends Element> elements) {
        switch (elements.size()) {
        case 1:
            return elements.get(0);

        default:
            return new ConcatenationElement(elements);
        }
    }

    private int nextWord(CharSequence format, boolean outer,
                         StringBuilder result) {
        final int length = format.length();
        boolean esc = false;

        for (int index = 0; index < length; index++) {
            char c = format.charAt(index);
            if (esc) {
                result.append(c);
                esc = false;
                continue;
            }
            if (c == escapeChar) {
                esc = true;

            } else if (c == argSep || c == closeChar) {
                if (outer) {
                    result.append(c);
                } else {
                    return index;
                }
            } else if (c == openChar) {
                return index;

            } else {
                result.append(c);
            }

            /* switch (c) { case escapeChar: esc = true; break; case
             * argSep: case closeChar: if (outer) { result.append(c);
             * break; } return index; case openChar: return index;
             * default: result.append(c); break; } */
        }
        return length;
    }

    /**
     * Keep adding elements until we reach a ':', '}', or the end of the
     * string. Do not consume the ':' or '}'. Return number of
     * characters consumed.
     */
    private int parseSequence(List<? super Element> elements,
                              CharSequence format, boolean outer) {
        CharSequence original = format;

        while (format.length() > 0) {
            /* Try to get literals. */
            StringBuilder unescaped = new StringBuilder(100);
            int done = nextWord(format, outer, unescaped);
            format = format.subSequence(done, format.length());
            if (unescaped.length() > 0) {
                elements.add(new LiteralElement(unescaped.toString()));
            }

            if (format.length() == 0) {
                if (outer) return original.length() - format.length();
                throw new IllegalArgumentException("Unterminated pattern: "
                    + original);
            }

            char c = format.charAt(0);
            if (c == argSep || c == closeChar) {
                /* That's the end of this sequence. */
                assert (!outer);
                return original.length() - format.length();
            } else if (c == openChar) {
                done =
                    parseMacro(elements, format.subSequence(1, format.length()))
                        + 1;
                format = format.subSequence(done, format.length());
            }

            /* switch (format.charAt(0)) { case argSep: case closeChar:
             * /* That's the end of this sequence. assert (!outer);
             * return original.length() - format.length();
             * 
             * case openChar: done = parseMacro(elements,
             * format.subSequence(1, format.length())) + 1; format =
             * format.subSequence(done, format.length()); break; } */
        }

        if (!outer) throw new IllegalArgumentException("Unterminated pattern: "
            + original);

        return original.length();
    }

    /**
     * Parse each colon-terminated argument, until you get one ending
     * with a closing brace. Consume that brace. Return the number of
     * consumed characters. Add each argument as an element.
     */
    private int parseArguments(List<? super Element> elements,
                               CharSequence format) {
        CharSequence original = format;

        boolean more;
        do {
            List<Element> sequence = new ArrayList<Element>(4);
            int done = parseSequence(sequence, format, false);
            format = format.subSequence(done, format.length());
            elements.add(optimize(sequence));

            if (format.length() == 0)
                throw new IllegalArgumentException("Unterminated pattern: "
                    + format);
            more = format.charAt(0) == argSep;
            format = format.subSequence(1, format.length());
        } while (more);

        return original.length() - format.length();
    }

    private int parseMacro(List<? super Element> elements,
                           CharSequence format) {
        if (format.length() == 0)
            throw new IllegalArgumentException("Incomplete macro: " + format);

        /* Compute a list of arguments. */
        List<Element> args = new ArrayList<Element>(4);
        final int done;

        char c = format.charAt(0);
        if (c == ifCmd) {
            done = 1
                + parseArguments(args, format.subSequence(1, format.length()));
            elements.add(new QueryElement(args));
        } else if (c == countCmd) {
            done = 1
                + parseArguments(args, format.subSequence(1, format.length()));
            elements.add(new CountElement(args.get(0)));
        } else if (c == replaceCmd) {
            done = 1
                + parseArguments(args, format.subSequence(1, format.length()));
            elements.add(new RegexElement(args));
        } else if (c == enumCmd) {
            done = 1
                + parseArguments(args, format.subSequence(1, format.length()));
            elements.add(new IterateElement(args));
        } else if (c == timesCmd) {
            done = 1
                + parseArguments(args, format.subSequence(1, format.length()));
            elements.add(new RepeatElement(args));
        } else {
            done = parseArguments(args, format);
            elements.add(new ValueElement(args.get(0)));
        }

        /* switch (format.charAt(0)) { case ifCmd: done = 1 +
         * parseArguments(args, format.subSequence(1, format.length()));
         * elements.add(new QueryElement(args)); break; case countCmd:
         * done = 1 + parseArguments(args, format.subSequence(1,
         * format.length())); elements.add(new
         * CountElement(args.get(0))); break; case replaceCmd: done = 1
         * + parseArguments(args, format.subSequence(1,
         * format.length())); elements.add(new RegexElement(args));
         * break; case enumCmd: done = 1 + parseArguments(args,
         * format.subSequence(1, format.length())); elements.add(new
         * IterateElement(args)); break; case timesCmd: done = 1 +
         * parseArguments(args, format.subSequence(1, format.length()));
         * elements.add(new RepeatElement(args)); break; default: done =
         * parseArguments(args, format); elements.add(new
         * ValueElement(args.get(0))); break; } */

        return done;
    }

    private interface Element {
        CharSequence expand(Properties params);
    }

    private class LiteralElement implements Element {
        private final CharSequence data;

        public LiteralElement(CharSequence data) {
            this.data = data;
        }

        public CharSequence expand(Properties params) {
            CharSequence cs = data;
            for (Escaper e : escapers)
                cs = e.escape(cs);
            return cs;
        }
    }

    private static class ConcatenationElement implements Element {
        private final List<? extends Element> components;

        public ConcatenationElement(List<? extends Element> components) {
            this.components = components;
        }

        public CharSequence expand(Properties params) {
            StringBuilder result = new StringBuilder(100);
            for (Element e : components)
                result.append(e.expand(params));
            return result;
        }
    }

    private static class CountElement implements Element {
        private final Element name;

        public CountElement(Element name) {
            this.name = name;
        }

        public CharSequence expand(Properties params) {
            String expandedName = name.expand(params).toString();
            int i = 0;
            while (params.getProperty(expandedName + '.' + i) != null)
                i++;
            return "" + i;
        }
    }

    private class QueryElement implements Element {
        private final Element name, truth, falsehood;

        public QueryElement(List<? extends Element> args) {
            switch (args.size()) {
            case 0:
                throw new IllegalArgumentException("At least one"
                    + " argument needed for query");

            case 1:
                name = args.get(0);
                truth = new LiteralElement("1");
                falsehood = new LiteralElement("0");
                break;

            case 2:
                name = args.get(0);
                truth = args.get(1);
                falsehood = new LiteralElement("");
                break;

            default:
                name = args.get(0);
                truth = args.get(1);
                falsehood = args.get(2);
                break;
            }
        }

        public CharSequence expand(Properties params) {
            String expandedName = name.expand(params).toString();
            return params.getProperty(expandedName) != null ||
                params.getProperty(expandedName + ".0") != null ?
                    truth.expand(params) : falsehood.expand(params);
        }
    }

    private static class ValueElement implements Element {
        private final Element name;

        public ValueElement(Element name) {
            this.name = name;
        }

        public CharSequence expand(Properties params) {
            String expandedName = name.expand(params).toString();
            String result = params.getProperty(expandedName);
            if (result == null) return "";
            return result;
        }
    }

    private static class RegexElement implements Element {
        private final Element text, pattern, replacement;

        public RegexElement(List<? extends Element> args) {
            switch (args.size()) {
            default:
                throw new IllegalArgumentException("One, three or more"
                    + " arguments needed for regex");

            case 3:
                text = args.get(0);
                pattern = args.get(1);
                replacement = args.get(2);
                break;
            }
        }

        public CharSequence expand(Properties params) {
            String expandedText = text.expand(params).toString();
            String expandedPattern = pattern.expand(params).toString();
            String expandedReplacement = replacement.expand(params).toString();
            return expandedText.replaceAll(expandedPattern,
                                           expandedReplacement);
        }
    }

    private static class RepeatElement implements Element {
        private final Element text, count;

        public RepeatElement(List<? extends Element> args) {
            switch (args.size()) {
            case 0:
            case 1:
                throw new IllegalArgumentException("Two or more"
                    + " arguments needed for repetition");

            default:
                text = args.get(0);
                count = args.get(1);
                break;
            }
        }

        public CharSequence expand(Properties params) {
            String expandedText = text.expand(params).toString();
            String expandedCount = count.expand(params).toString();
            StringBuilder result = new StringBuilder(100);
            try {
                int n = Integer.parseInt(expandedCount);
                for (int i = 0; i < n; i++)
                    result.append(expandedText);
            } catch (NumberFormatException ex) {
                /* Ignore silently. */
            }
            return result;
        }
    }

    private class IterateElement implements Element {
        private final Element name, var, expansion, separator;

        public IterateElement(List<? extends Element> args) {
            switch (args.size()) {
            default:
                throw new IllegalArgumentException("Three or more"
                    + " arguments needed for iteration");

            case 3:
                name = args.get(0);
                var = args.get(1);
                expansion = args.get(2);
                separator = new LiteralElement("");
                break;

            case 4:
                name = args.get(0);
                var = args.get(1);
                expansion = args.get(2);
                separator = args.get(3);
                break;
            }
        }

        public CharSequence expand(Properties params) {
            String expandedName = name.expand(params).toString();
            String expandedVar = var.expand(params).toString();

            StringBuilder result = new StringBuilder(100);

            Properties temps = new Properties(params);
            String sep = "";
            for (int i = 0; params.getProperty(expandedName + '.' + i) != null;
                 i++) {
                temps.setProperty(expandedVar, "" + i);
                result.append(sep);
                result.append(expansion.expand(temps));
                sep = separator.expand(temps).toString();
            }
            return result;
        }
    }

    /**
     * Convert named parameters into a string, according to this
     * object's scheme.
     * 
     * @param params the set of parameters to use in the expansion
     * 
     * @return the pattern expanded with the parameters
     */
    public String format(Properties params) {
        return rootElement.expand(params).toString();
    }
}
