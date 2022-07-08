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
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Escapes content for HTML as characters are written.
 * 
 * @author simpsons
 */
public class MarkupPrintWriter extends PrintWriter {
    private final Writer direct;

    /**
     * The base writer that does not escape
     */
    public final PrintWriter literal;

    /**
     * Create a mark-up print writer based on a mark-up writer.
     * 
     * @param out the destination capable of performing the escaping
     */
    public MarkupPrintWriter(Writer out) {
        super(new MarkupWriter(out));
        this.direct = out;
        this.literal = new PrintWriter(out);
    }

    /**
     * Start writing an opening element tag.
     * 
     * @param name the tag name
     * 
     * @return this object
     */
    public MarkupPrintWriter openElement(String name) {
        try {
            direct.write('<');
            direct.write(name);
        } catch (IOException e) {
            setError();
        }
        return this;
    }

    /**
     * Complete an opening element tag.
     * 
     * @return this object
     */
    public MarkupPrintWriter closeElement() {
        try {
            direct.write('>');
        } catch (IOException e) {
            setError();
        }
        return this;
    }

    /**
     * Complete an empty element tag.
     * 
     * @return this object
     */
    public MarkupPrintWriter emptyElement() {
        try {
            direct.write(">");
        } catch (IOException e) {
            setError();
        }
        return this;
    }

    /**
     * Write a closing element tag.
     * 
     * @param name the tag name
     * 
     * @return this object
     */
    public MarkupPrintWriter endElement(String name) {
        try {
            direct.write("</");
            direct.write(name);
            direct.write('>');
        } catch (IOException e) {
            setError();
        }
        return this;
    }

    /**
     * Write a formatted attribute.
     * 
     * @param name the attribute name
     * 
     * @param fmt the format for the value
     * 
     * @param args arguments to replace parts of the value
     * 
     * @return this object
     */
    public MarkupPrintWriter attribute(String name, String fmt,
                                       Object... args) {
        try {
            direct.write(' ');
            direct.write(name);
            direct.write("=\"");
            ((MarkupWriter) super.out).inAttribute(true);
            super.printf(fmt, args);
            direct.write("\"");
            ((MarkupWriter) super.out).inAttribute(false);
        } catch (IOException e) {
            setError();
        }
        return this;
    }

    /**
     * Write a boolean attribute.
     * 
     * @param name the attribute name
     * 
     * @param enabled {@code true} if the attribute should be present
     * 
     * @return this object
     */
    public MarkupPrintWriter attribute(String name, boolean enabled) {
        if (!enabled) return this;
        try {
            direct.write(' ');
            direct.write(name);
        } catch (IOException e) {
            setError();
        }
        return this;
    }
}
