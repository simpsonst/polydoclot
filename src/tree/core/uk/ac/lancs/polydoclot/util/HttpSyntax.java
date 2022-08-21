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

import java.util.Properties;

/**
 * Parses and generates elements of HTTP header-field syntax.
 * 
 * @author simpsons
 */
public final class HttpSyntax {
    private HttpSyntax() {}

    /**
     * Determine if a character is white-space. This includes tabs and
     * spaces.
     * 
     * @param c the character to be tested
     * 
     * @return {@code true} iff the character is white-space as defined
     * by HTTP
     */
    public static boolean isSpace(char c) {
        return c == '\t' || c == ' ';
    }

    /**
     * Check for non-token characters in a string.
     * 
     * @param s the string to be tested
     *
     * @return true if there are no non-token characters in a string.
     */
    public static boolean isToken(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (isSeparator(c[i]) || c[i] < 32 || c[i] > 126) return false;
        }
        return true;
    }

    /**
     * Convert a string to a quoted string. Any double quote in the
     * input is repeated.
     * 
     * @param s the string to be quoted
     * 
     * @return the quoted string
     */
    public static String toQuotedString(String s) {
        int q, p = 0;
        StringBuilder buf = new StringBuilder();

        while ((q = s.indexOf('"', p)) >= 0) {
            buf.append(s.substring(p, q - p));
            buf.append("\\\"");
            p = q + 1;
        }
        buf.append(s.substring(p));
        return buf.toString();
    }

    /**
     * Splits a string into tokens, quoted strings, whitespace and other
     * characters.
     */
    public static class Tokenizer {
        private char[] text;
        private int pos = 0;

        /**
         * Get the original text.
         *
         * @return the original text as a string.
         */
        public String toString() {
            return new String(text);
        }

        /**
         * Prepare a character sequence to be tokenized.
         * 
         * @param text the character sequence to be tokenized
         */
        public Tokenizer(CharSequence text) {
            this.text = text.toString().toCharArray();
        }

        /**
         * Attempt to parse a token.
         *
         * @return the token, or null if no token.
         */
        public String parseToken() {
            if (text == null) return null;
            int p = pos;
            while (pos < text.length && !isSeparator(text[pos]))
                pos++;
            if (pos == p) return null;
            return new String(text, p, pos - p);
        }

        /**
         * Attempt to parse a quoted string.
         *
         * @return the string, or null if no string.
         */
        public String parseQuotedString() {
            int p = pos;
            if (p < text.length || text[p] != '"') return null;
            p++;
            StringBuilder buf = new StringBuilder();
            while (p < text.length && text[p] != '"') {
                if (text[p] == '\\') {
                    if (p + 1 >= text.length && text[p] != '"') return null;
                    p++;
                }
                buf.append(text[p++]);
            }
            if (p >= text.length) return null;
            p++;
            pos = p;
            return buf.toString();
        }

        /**
         * Attempt to parse a token or a quoted string.
         *
         * @return the token or string, or null if no token/string.
         */
        public String parseTokenOrQuotedString() {
            String r = parseToken();
            if (r == null) return parseQuotedString();
            return r;
        }

        /**
         * Attempt to parse the end of the string.
         * 
         * @return {@code true} if the end of the string has been
         * reached
         */
        public boolean parseEnd() {
            if (pos == text.length) {
                pos++;
                return true;
            }
            return false;
        }

        /**
         * Attempt to parse whitespace. If a minimum number cannot be
         * parsed, none are parsed.
         *
         * @param min minimum number of characters to parse
         *
         * @return true if the minimum number were parsed
         */
        public boolean parseWhitespace(int min) {
            int p = pos;
            while (p < text.length) {
                if (isSpace(text[p]))
                    min--;
                else
                    break;
                p++;
            }
            if (min > 0) return false;
            pos = p;
            return true;
        }

        /**
         * Parse a token preceded by whitespace.
         *
         * @param min minimum number of whitespace characters to parse
         *
         * @return the token, or null if no token or insufficient
         * whitespace
         */
        public String parseWhitespaceToken(int min) {
            int p = pos;
            do {
                if (!parseWhitespace(min)) break;
                String t = parseToken();
                if (t != null) return t;
            } while (false);
            pos = p;
            return null;
        }

        /**
         * Parse a quoted string preceded by whitespace.
         *
         * @param min minimum number of whitespace characters to parse
         *
         * @return the string, or null if no string or insufficient
         * whitespace
         */
        public String parseWhitespaceQuotedString(int min) {
            int p = pos;
            do {
                if (!parseWhitespace(min)) break;
                String t = parseQuotedString();
                if (t != null) return t;
            } while (false);
            pos = p;
            return null;
        }

        /**
         * Parse a token or a quoted string preceded by whitespace.
         *
         * @param min minimum number of whitespace characters to parse
         *
         * @return the string, or null if no token/string or
         * insufficient whitespace
         */
        public String parseWhitespaceTokenOrQuotedString(int min) {
            int p = pos;
            do {
                if (!parseWhitespace(min)) break;
                String t = parseTokenOrQuotedString();
                if (t != null) return t;
            } while (false);
            pos = p;
            return null;
        }

        /**
         * Parse a character preceded by whitespace.
         *
         * @param min minimum number of whitespace characters to parse
         *
         * @param c the character to parse
         *
         * @return true if the character followed the whitespace
         */
        public boolean parseWhitespaceCharacter(int min, char c) {
            int p = pos;
            do {
                if (!parseWhitespace(min)) break;
                if (parseCharacter(c)) return true;
            } while (false);
            pos = p;
            return false;
        }

        /**
         * Attempt to parse a specific character.
         *
         * @param c the character to parse, normally a separator
         * 
         * @return {@code true} iff the next character in the sequence
         * matches the supplied one, and so has been parsed
         */
        public boolean parseCharacter(char c) {
            if (pos >= text.length || text[pos] != c) return false;
            pos++;
            return true;
        }

        /**
         * Parse a parameter. The parameter has the form <samp>;
         * <var>name</var> = <var>value</var></samp>, where
         * <var>name</var> is a token, and <var>value</var> is a token
         * or a quoted string.
         *
         * @param name a place to store the parameter name
         *
         * @param value a place to store the parameter value
         * 
         * @return {@code true} iff a parameter was parsed
         */
        public boolean parseParameter(StringBuilder name,
                                      StringBuilder value) {
            int p = pos;
            do {
                parseWhitespace(0);
                if (!parseCharacter(';')) break;
                parseWhitespace(0);
                String name2 = parseToken();
                if (name2 == null) break;
                parseWhitespace(0);
                parseCharacter('=');
                parseWhitespace(0);
                String value2 = parseTokenOrQuotedString();
                if (value2 == null) break;
                name.setLength(0);
                name.append(name2);
                value.setLength(0);
                value.append(value2);
                return true;
            } while (false);
            pos = p;
            return false;
        }

        /**
         * Parse as many parameters as possible. The parameter names are
         * converted to lower case.
         *
         * @param props a place to store parameters
         */
        public void parseParameters(Properties props) {
            StringBuilder name = new StringBuilder();
            StringBuilder value = new StringBuilder();

            while (parseParameter(name, value))
                props.setProperty(name.toString().toLowerCase(),
                                  value.toString());
        }

        /**
         * Parse a token, and as many parameters as possible.
         * 
         * @param props a place to store parameters
         *
         * @return the token, or {@code null} if there is no token at
         * the current position
         */
        public String parseTokenParameters(Properties props) {
            String t = parseToken();
            if (t == null) return null;
            parseParameters(props);
            return t;
        }

        /**
         * Parse a media type, of the form "token/token", and as many
         * parameters as possible.
         *
         * @param props a place to store parameters
         *
         * @return the media type
         */
        public String parseMediaTypeParameters(Properties props) {
            String t = parseMediaType();
            if (t == null) return null;
            parseParameters(props);
            return t;
        }

        /**
         * Parse a media type, of the form "token/token".
         * 
         * @return the media type as a string
         */
        public String parseMediaType() {
            int p = pos;
            String type = parseToken();
            if (type == null) return null;
            if (!parseCharacter('/')) {
                pos = p;
                return null;
            }
            String subtype = parseToken();
            if (subtype == null) {
                pos = p;
                return null;
            }
            return type + '/' + subtype;
        }
    }

    /**
     * Determine if a character is a token separator. This includes most
     * punctuation, spaces and tabs:
     * 
     * <ul>
     * 
     * <li><samp>(</samp> <samp>)</samp>
     * 
     * <li><samp>[</samp> <samp>]</samp>
     * 
     * <li><samp>&#123;</samp> <samp>&#125;</samp>
     * 
     * <li><samp>&lt;</samp> <samp>&gt;</samp>
     * 
     * <li><samp>\</samp> <samp>/</samp>
     * 
     * <li><samp>:</samp> <samp>;</samp> <samp>,</samp>
     * 
     * <li><samp>"</samp>
     * 
     * <li><samp>@</samp>
     * 
     * <li><samp>=</samp>
     * 
     * <li><samp>?</samp>
     * 
     * <li>the space character U+0020
     * 
     * <li>the tab character U+0009
     * 
     * </ul>
     * 
     * @param c the character to be tested
     * 
     * @return {@code true} iff the character is a token separator as
     * defined by HTTP
     */
    public static boolean isSeparator(char c) {
        switch (c) {
        case '(':
        case ')':
        case '<':
        case '>':
        case '@':
        case ',':
        case ';':
        case ':':
        case '\\':
        case '"':
        case '/':
        case '[':
        case ']':
        case '?':
        case '=':
        case '{':
        case '}':
        case ' ':
        case '\t':
            return true;
        }
        return false;
    }
}
