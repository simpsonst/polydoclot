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
import java.util.stream.IntStream;

/**
 * Escapes streams of code points in some implementation-defined manner.
 * 
 * @author simpsons
 */
public interface Escaper {
    /**
     * Escape a codepoint stream.
     * 
     * @param in the source stream of codepoints
     * 
     * @return an escaped stream
     */
    IntStream escape(IntStream in);

    /**
     * Escape a character sequence.
     * 
     * @default A stream of the input's code points is escaped using
     * {@link #escape(IntStream)}, with the results appended a
     * {@link StringBuilder}, which generates the result.
     * 
     * @param in the source sequence
     * 
     * @return the escaped string
     */
    default String escape(CharSequence in) {
        StringBuilder result = new StringBuilder();
        escape(in.codePoints())
            .forEachOrdered(cp -> result.appendCodePoint(cp));
        return result.toString();
    }

    /**
     * Get a view of properties with escaped values.
     * 
     * @default A new {@link Properties} object is created, with
     * {@link Properties#getProperty(String)} and
     * {@link Properties#getProperty(String, String)} being overridden
     * to escape the results of calling the superclass. Access using
     * other methods might not result in escaped content.
     * 
     * @param in the original properties
     * 
     * @return a view of the properties with escaped values
     */
    default Properties escape(Properties in) {
        Properties out = new Properties(in) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getProperty(String key) {
                String value = in.getProperty(key);
                if (value == null) return null;
                return escape(value);
            }

            @Override
            public String getProperty(String key, String defaultValue) {
                String value = in.getProperty(key, defaultValue);
                if (value == null) return null;
                return escape(value);
            }
        };
        return out;
    }

    /**
     * An escaper that does nothing
     */
    static Escaper IDENTITY = new Escaper() {
        @Override
        public IntStream escape(IntStream in) {
            return in;
        }
    };
}
