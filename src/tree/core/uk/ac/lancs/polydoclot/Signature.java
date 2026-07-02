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

package uk.ac.lancs.polydoclot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a structural element reference derived from a textual
 * signature. This class makes no attempt to determine what components
 * of a dot-separated string identify a package versus a class. It also
 * does not attempt to resolve components against any context.
 * 
 * @author simpsons
 */
public final class Signature {
    /**
     * Identifies a parameter type in a reference signature.
     * 
     * @author simpsons
     */
    public static class Parameter {
        /**
         * The basic and erased parameter type
         */
        public final String type;

        /**
         * Note that this does not include the extra array dimension
         * implied by {@link #varargs}.
         * 
         * @resume The number of array dimensions
         */
        public final int dims;

        /**
         * Whether this parameter can be supplied with an array whose
         * elements can be listed as trailing arguments
         */
        public final boolean varargs;

        private Parameter(String type, int dims, boolean varargs) {
            this.type = type;
            this.dims = dims;
            this.varargs = varargs;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 41 * hash + Objects.hashCode(this.type);
            hash = 41 * hash + this.dims;
            hash = 41 * hash + (this.varargs ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Parameter other = (Parameter) obj;
            if (this.dims != other.dims) return false;
            if (this.varargs != other.varargs) return false;
            return Objects.equals(this.type, other.type);
        }

        /**
         * Create a new parameter with the same number of dimensions as
         * this one.
         * 
         * @param resolvedType the replacement type
         * 
         * @return the new parameter
         */
        public Parameter resolveTo(String resolvedType, boolean varargs) {
            return new Parameter(resolvedType, dims, varargs);
        }

        Parameter(String text) {
            Matcher m = PARAM_PATTERN.matcher(text);
            if (!m.matches()) throw new IllegalArgumentException(text);
            this.varargs = m.group(ELLIPSIS_GROUP) != null;
            this.type = m.group(TYPE_GROUP);
            this.dims = m.group(DIMS_GROUP).length() / 2;
        }
    }

    private static String removeWhiteSpace(CharSequence txt) {
        if (txt == null) return null;
        StringBuilder r = new StringBuilder(txt);
        Matcher m = WHITE_SPACE.matcher(r);
        int pos = 0;
        while (m.find(pos)) {
            r.delete(m.start(), m.end());
            pos = m.start();
        }
        return r.toString();
    }

    private static final Pattern WHITE_SPACE = Pattern.compile("\\s+");

    /**
     * The module identifier, or {@code null} if not specified
     */
    public final String module;

    /**
     * The combined package/class identifier, or {@code null} if not
     * specified
     */
    public final String packageClass;

    /**
     * The field, constructor or method identifier, or {@code null} if
     * referencing a class, package or module
     */
    public final String member;

    /**
     * The list of parameter types, or {@code null} if not referencing
     * an executable member
     */
    public final List<Parameter> args;

    /**
     * Create an element reference from a signature.
     * 
     * @param text the signature
     */
    public Signature(CharSequence text) {
        Matcher m = REFERENCE_PATTERN.matcher(removeWhiteSpace(text));
        if (!m.matches()) throw new IllegalArgumentException(text.toString());

        /* At least one part of the pattern must be present. */
        String elemText = m.group(CLASS_GROUP);
        String memberText = m.group(MEMBER_GROUP);
        String modText = m.group(MODULE_GROUP);
        if (elemText == null && memberText == null && modText == null)
            throw new IllegalArgumentException(text.toString());

        /* For the module, package/class and member, store exactly what
         * the regex provides. */
        this.module = modText;
        this.packageClass = elemText;
        this.member = memberText;

        /* For the argument list, provide none if a package, class or
         * field is referenced. */
        if (this.member == null) {
            this.args = null;
            return;
        }
        String argText = m.group(PARAMS_GROUP);
        if (argText == null) {
            this.args = null;
            return;
        }

        /* An executable member is referenced. Split and parse the
         * arguments. */
        if (argText.trim().isEmpty()) {
            this.args = Collections.emptyList();
        } else {
            String[] args = splitOnCommas(argText);
            List<Parameter> result = new ArrayList<>(args.length);
            for (String arg : args)
                result.add(new Parameter(arg));
            this.args = Collections.unmodifiableList(result);
        }
    }

    /* Uh-oh! Now I have 6 problems! */

    private static final Pattern COMMAS = Pattern.compile("\\s*,\\s*");

    private static String[] splitOnCommas(CharSequence text) {
        if (text.isEmpty()) return new String[0];
        return COMMAS.split(text);
    }

    /**
     * Matches an unqualified identifier. For example, <samp>Foo</samp>
     * is matched, but not <samp>pkg.Foo</samp>.
     */
    private static final String IDENT_TEXT =
        "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";

    /**
     * Matches a potentially qualified identifier. For example,
     * <samp>Foo</samp>, <samp>pkg.Foo</samp> and
     * <samp>a.very.long.pkg.Foo</samp> are all matched. White space is
     * permitted around the dots, but no leading or trailing white space
     * is permitted.
     */
    private static final String QUAL_IDENT_TEXT =
        "(?:" + IDENT_TEXT + "\\s*\\.\\s*)*" + IDENT_TEXT;

    /**
     * Matches a qualified identifier with optional dimensions. For
     * example, <samp>Foo</samp>, <samp>Foo[]</samp>,
     * <samp>some.enclosing.pkg.Foo[][][]</samp> are all matched.
     */
    private static final String PARAM_TEXT =
        "(?:" + QUAL_IDENT_TEXT + ")(?:\\s*\\[\\s*\\])*";

    private static final String TYPE_GROUP = "type";

    private static final String DIMS_GROUP = "dims";

    private static final String ELLIPSIS_GROUP = "dots";

    /**
     * Matches a parameter in an element reference. The entire text must
     * be the parameter, i.e., no leading or trailing characters that
     * are not part of the parameter (even white space) are permitted.
     * The identifier can be extracted as the group
     * {@value #TYPE_GROUP}, the dimensions as {@value #DIMS_GROUP}, and
     * the ellipsis as {@value #ELLIPSIS_GROUP}. Dimensions and the
     * ellipsis can be empty strings, and may contain white space. To
     * count the dimensions, first strip all white space, and divide the
     * remaining length by 2.
     */
    private static final Pattern PARAM_PATTERN = Pattern.compile("^(?<"
        + TYPE_GROUP + ">" + QUAL_IDENT_TEXT + ")(?<" + DIMS_GROUP
        + ">(?:\\s*\\[\\s*\\])*)(?<" + ELLIPSIS_GROUP + ">\\s*\\.\\.\\.)?$");

    /**
     * Matches a non-empty parameter list excluding the surrounding
     * brackets. At least one parameter is required, and parameters are
     * comma-separated. White space is permitted around the commas.
     * Finally, an optional ellipsis is permitted.
     */
    private static final String PARAMS_TEXT =
        PARAM_TEXT + "(?:\\s*,\\s*" + PARAM_TEXT + ")*(?:\\s*\\.\\.\\.)?";

    private static final String PARAMS_GROUP = "params";

    /**
     * Matches a parameter list in brackets. The contents within the
     * brackets can be extracted as the group {@value #PARAMS_GROUP},
     * and may be empty. White space may appear just inside the
     * brackets, but does not appear in the group.
     */
    private static final String BRACKETED_PARAMS_TEXT =
        "\\(\\s*(?<" + PARAMS_GROUP + ">(?:" + PARAMS_TEXT + ")?)\\s*\\)";

    private static final String MODULE_GROUP = "mod";

    private static final String CLASS_GROUP = "class";

    private static final String MEMBER_GROUP = "memb";

    private static final String REFERENCE_TEXT = "^"

        /* The element identifier is optional. */
        + "(?:"

        /* Detect an optional module specification, i.e., a qualified
         * name and a slash prefix of the class identifier; and give it
         * a named group. */
        + "(?:(?<" + MODULE_GROUP + ">" + QUAL_IDENT_TEXT + ")\\s*/\\s*)?"

        /* Put the class name (unqualified by module) into a named
         * group. */
        + "(?<" + CLASS_GROUP + ">" + QUAL_IDENT_TEXT + ")"

        + ")?" // end of optional class identifier

        /* The member specifier is optional. */
        + "(?:"

        /* The member name is preceded by a hash, and the name itself is
         * an unqualified identifier, which is placed in a named
         * group. */
        + "\\s*\\#\\s*(?<" + MEMBER_GROUP + ">" + IDENT_TEXT + ")"

        /* The parameter specifier exists for callable members, and is
         * absent for fields/constants. */
        + "(?:" + BRACKETED_PARAMS_TEXT + ")?"

        + ")?" // end of the optional member specifier

        + "$";

    /**
     * Matches a reference string, as used in <code>&#64;see</code>,
     * <code>&#64;value</code>, <code>&#64;link</code> and
     * <code>&#64;linkplain</code> tags. The group
     * {@value #MODULE_GROUP}, if present, identifies the module, and
     * {@value #CLASS_GROUP} identifies the class. Group
     * {@value #MEMBER_GROUP} is the plain name of the member, if
     * specified. {@value #PARAMS_GROUP} gives the contents of the
     * brackets, and should be split on commas, and each element
     * trimmed, and parsed using {@link #PARAM_PATTERN}.
     * 
     * The full pattern is {@value #REFERENCE_TEXT}.
     */
    private static final Pattern REFERENCE_PATTERN =
        Pattern.compile(REFERENCE_TEXT);

    private static void verifyField(String text, String moduleName,
                                    String className, String memberName) {
        Matcher m = REFERENCE_PATTERN.matcher(removeWhiteSpace(text));
        assert m.matches() : "match: " + text;
        assert Objects.equals(moduleName,
                              removeWhiteSpace(m.group(MODULE_GROUP))) :
            "module " + moduleName + ": " + text;
        assert Objects.equals(className,
                              removeWhiteSpace(m.group(CLASS_GROUP))) :
            "class " + className + ": " + text;
        assert Objects.equals(memberName,
                              removeWhiteSpace(m.group(MEMBER_GROUP))) :
            "member" + memberName + ": " + text;
        var paramsText = m.group(PARAMS_GROUP);
        assert paramsText == null : "field: " + text;
    }

    private static void verifyMethod(String text, String moduleName,
                                     String className, String memberName,
                                     Parameter... expParams) {
        Matcher m = REFERENCE_PATTERN.matcher(removeWhiteSpace(text));
        assert m.matches() : "match: " + text;
        assert Objects.equals(moduleName,
                              removeWhiteSpace(m.group(MODULE_GROUP))) :
            "module " + moduleName + ": " + text;
        assert Objects.equals(className,
                              removeWhiteSpace(m.group(CLASS_GROUP))) :
            "class " + className + ": " + text;
        assert Objects.equals(memberName,
                              removeWhiteSpace(m.group(MEMBER_GROUP))) :
            "member" + memberName + ": " + text;
        var paramsText = m.group(PARAMS_GROUP);
        assert paramsText != null : "field: " + text;
        String[] paramsTexts = splitOnCommas(paramsText);
        List<Parameter> params = new ArrayList<>(paramsTexts.length);
        for (var paramText : paramsTexts)
            params.add(new Parameter(paramText));
        assert params.size() == expParams.length : "got: " + params.size()
            + "; exp: " + expParams.length + "; " + text;
        for (int i = 0; i < expParams.length; i++)
            assert params.get(i).equals(expParams[i]) : "[" + i + "]: " + text;
    }

    /**
     * @undocumented
     */
    public static void main(String[] args) {
        verifyField("Foo", null, "Foo", null);
        verifyField("clang.dog/Foo", "clang.dog", "Foo", null);
        verifyField("clang.dog  /  Foo", "clang.dog", "Foo", null);
        verifyField("Foo#bar", null, "Foo", "bar");
        verifyField("Foo  # bar", null, "Foo", "bar");
        verifyField("#bar", null, null, "bar");
        verifyMethod("#bar(  )", null, null, "bar");
        verifyMethod("#bar( java.lang.String )", null, null, "bar",
                     new Parameter("java.lang.String", 0, false));
        verifyMethod("#bar( java . lang.String )", null, null, "bar",
                     new Parameter("java.lang.String", 0, false));
        verifyMethod("#bar( java . lang.String ...)", null, null, "bar",
                     new Parameter("java.lang.String", 0, true));
        verifyMethod("#bar( java . lang.String[][] ...)", null, null, "bar",
                     new Parameter("java.lang.String", 2, true));
        verifyMethod("#bar( java . lang.String [ ] [] ...)", null, null, "bar",
                     new Parameter("java.lang.String", 2, true));
        verifyMethod("#bar( int, long, java . lang.String [ ] [] ...)", null,
                     null, "bar", new Parameter("int", 0, false),
                     new Parameter("long", 0, false),
                     new Parameter("java.lang.String", 2, true));
        verifyMethod("#acceptEncodings(CharSequence...)", null, null,
                     "acceptEncodings", new Parameter("CharSequence", 0, true));

        new Signature("#acceptEncodings(CharSequence...)");
    }
}
