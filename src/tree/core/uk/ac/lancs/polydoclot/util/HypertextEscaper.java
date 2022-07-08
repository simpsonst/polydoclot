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

package uk.ac.lancs.polydoclot.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import uk.ac.lancs.polydoclot.Slice;

/**
 * Escapes text for HTML, given a character encoding. Characters not
 * representable in this encoding will be escaped as numeric character
 * entities.
 * 
 * @author simpsons
 */
public final class HypertextEscaper implements Escaper {
    private final CharsetEncoder encoder;
    private final boolean doubleQuotes;

    private HypertextEscaper(Charset charset, boolean doubleQuotes) {
        this.encoder = charset.newEncoder();
        this.doubleQuotes = doubleQuotes;
    }

    /**
     * Create an escaper for a given character encoding, ignoring double
     * quotes.
     * 
     * @param charset the encoding
     * 
     * @return a fresh escaper for the given character encoding
     * 
     * @constructor
     */
    public static HypertextEscaper forCData(Charset charset) {
        return new HypertextEscaper(charset, false);
    }

    /**
     * Create an escaper for a given character encoding, including
     * double quotes.
     * 
     * @param charset the encoding
     * 
     * @return a fresh attribute escaper for the given character
     * encoding
     * 
     * @constructor
     */
    public static HypertextEscaper forAttributes(Charset charset) {
        return new HypertextEscaper(charset, true);
    }

    @Override
    public IntStream escape(IntStream in) {
        return in.flatMap(cp -> {
            switch (cp) {
            case '"':
                if (!doubleQuotes) break;
                return "&quot;".codePoints();
            case '&':
                return "&amp;".codePoints();
            case '<':
                return "&lt;".codePoints();
            case '>':
                return "&gt;".codePoints();
            }

            /* Convert the code point to a string, usually of just one
             * character, but sometimes with surrogates. */
            String s = new String(Character.toChars(cp));

            /* Check whether the charset supports this string. If so,
             * make no change. */
            if (encoder.canEncode(s)) return s.codePoints();

            /* Use a numeric character entity. */
            return ("&#" + cp + ';').codePoints();
        });
    }

    private static final Pattern hexEnt =
        Pattern.compile("^(?:(?:#[xX](?<xcode>[a-fA-F0-9]+))"
            + "|(?:#(?<code>[0-9]+))" + "|(?<ref>.*))$");

    private static final Properties hypertextEntityReferences =
        new Properties();

    static {
        try {
            hypertextEntityReferences.load(Slice.class
                .getResourceAsStream("hypertextents.properties"));
        } catch (IOException e) {
            throw new AssertionError("unreachable");
        }
    }

    /**
     * Decode an HTML entity/character reference.
     * 
     * @param text the text between the <samp>&amp;</samp> and the
     * <samp>;</samp>
     * 
     * @return the code point represented by the reference, or
     * {@code -1} if not recognized
     */
    public static int codePointForEntity(CharSequence text) {
        Matcher m = hexEnt.matcher(text);
        if (!m.matches()) return -1;

        /* Try decoding the hexadecimal part. */
        String hexText = m.group("xcode");
        if (hexText != null) return Integer.parseInt(hexText, 16);

        /* Try decoding the decimal part. */
        String decText = m.group("code");
        if (decText != null) return Integer.parseInt(decText, 10);

        String value = hypertextEntityReferences.getProperty(m.group("ref"));
        if (value == null) return -1;

        return Integer.parseInt(value);
    }
}
