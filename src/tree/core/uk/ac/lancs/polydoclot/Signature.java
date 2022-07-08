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
 *  * Neither the name of Lancaster University nor the names of
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
            Matcher m = paramPattern.matcher(text);
            if (!m.matches()) throw new IllegalArgumentException(text);
            this.varargs = m.group("ellipsis") != null;
            this.type = m.group("type");
            this.dims = m.group("dims").length() / 2;
        }
    }

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
        Matcher m = referencePattern.matcher(text);
        if (!m.matches()) throw new IllegalArgumentException(text.toString());

        /* At least one part of the pattern must be present. */
        String elemText = m.group("elem");
        String memberText = m.group("member");
        String modText = m.group("mod");
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
        String argText = m.group("args");
        if (argText == null) {
            this.args = null;
            return;
        }

        /* An executable member is referenced. Split and parse the
         * arguments. */
        if (argText.trim().isEmpty()) {
            this.args = Collections.emptyList();
        } else {
            String[] args = argText.split("\\s*,\\s*");
            List<Parameter> result = new ArrayList<>(args.length);
            for (String arg : args)
                result.add(new Parameter(arg));
            this.args = Collections.unmodifiableList(result);
        }
    }

    /* Uh-oh! Now I have 6 problems! */

    private static final String IDENT =
        "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";

    private static final Pattern paramPattern =
        Pattern.compile("^(?<type>(?:" + IDENT + "\\.)*" + IDENT
            + ")(?<dims>(?:\\[\\])*)" + "(?<ellipsis>\\.\\.\\.)?$");

    private static final Pattern referencePattern = Pattern.compile("^(?:"

        + "(?:(?<mod>(?:" + IDENT + ")(?:\\." + IDENT + ")+)/)?"

        + "(?<elem>(?:" + IDENT + "\\.)*" + IDENT + "))?"

        + "(?:\\#(?<member>" + IDENT + ")(?:\\((?<args>(?:(?:" + IDENT
        + "\\.)*" + IDENT + "(?:\\[\\])*(?:\\s*,\\s*(?:" + IDENT + "\\.)*"
        + IDENT + "(?:\\[\\])*(?:\\.\\.\\.)?)*)?)\\))?)?$");

}
