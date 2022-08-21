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

package uk.ac.lancs.polydoclot.html;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Formatter;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * Specifies a destination for streams of Unicode codepoints. A
 * destination can be built from {@link PrintWriter},
 * {@link StringBuilder}, {@link Formatter} or {@link PrintStream}.
 * 
 * @author simpsons
 */
public final class TextDestination {
    Consumer<? super IntStream> out;

    TextDestination(Consumer<? super IntStream> out) {
        this.out = out;
    }

    /**
     * Specify a consumer of codepoint streams as a destination.
     * 
     * @param out the entity to receive codepoint streams
     * 
     * @return the corresponding destination
     * 
     * @constructor
     */
    public static TextDestination
        ofCodepoints(Consumer<? super IntStream> out) {
        return new TextDestination(out);
    }

    /**
     * Specify a consumer of character sequences as a destination.
     * 
     * @param out the entity to receive character sequences
     * 
     * @return the corresponding destination
     * 
     * @constructor
     */
    public static TextDestination
        ofChars(Consumer<? super CharSequence> out) {
        return new TextDestination(s -> s.forEach(cp -> out
            .accept(String.valueOf(Character.toChars(cp)))));
    }

    /**
     * Specify a string builder as a destination.
     * 
     * @param out the string builder to append to
     * 
     * @return the corresponding destination
     * 
     * @constructor
     */
    public static TextDestination of(StringBuilder out) {
        return ofChars(out::append);
    }

    /**
     * Specify a print writer as a destination.
     * 
     * @param out the writer to append to
     * 
     * @return the corresponding destination
     * 
     * @constructor
     */
    public static TextDestination of(PrintWriter out) {
        return ofChars(out::append);
    }

    /**
     * Specify a print stream as a destination.
     * 
     * @param out the stream to append to
     * 
     * @return the corresponding destination
     * 
     * @constructor
     */
    public static TextDestination of(PrintStream out) {
        return ofChars(out::append);
    }

    /**
     * Specify a formatter as a destination.
     * 
     * @param out the formatter to submit character sequences to
     * 
     * @return the corresponding destination
     * 
     * @constructor
     */
    public static TextDestination of(Formatter out) {
        return ofChars(s -> out.format("%s", s));
    }
}
