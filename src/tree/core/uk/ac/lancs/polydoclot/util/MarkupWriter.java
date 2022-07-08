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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Escapes special HTML characters as they are written. This writer has
 * an internal boolean state set by {@link #inAttribute(boolean)}. When
 * true, the double-quote character U+0022 is also escaped. When false,
 * only <samp>&lt;</samp>, <samp>&gt;</samp> and <samp>&amp;</samp> are
 * escaped.
 * 
 * @author simpsons
 */
public class MarkupWriter extends FilterWriter {
    /**
     * Create a mark-up writer.
     * 
     * @param out the destination for characters
     */
    public MarkupWriter(Writer out) {
        super(out);
    }

    private boolean inAttribute;

    /**
     * Set whether double quotes are to be escaped.
     * 
     * @param newState {@code true} if double quotes are to be escaped
     */
    public void inAttribute(boolean newState) {
        this.inAttribute = newState;
    }

    /**
     * Write characters from an array to the underlying destination,
     * escaping for HTML on the way.
     * 
     * @param cbuf the source buffer of characters
     * 
     * @param off the index of the first character to be written
     * 
     * @param len the number of characters to be written
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        final int end = off + len;
        int last = off;
        while (off < end) {
            switch (cbuf[off]) {
            case '"':
                if (inAttribute) {
                    if (off > last) {
                        super.write(cbuf, last, off - last);
                        last = off + 1;
                    }
                    super.write("&quot;");
                }
                break;

            case '>':
                if (off > last) {
                    super.write(cbuf, last, off - last);
                    last = off + 1;
                }
                super.write("&gt;");
                break;

            case '<':
                if (off > last) {
                    super.write(cbuf, last, off - last);
                    last = off + 1;
                }
                super.write("&lt;");
                break;

            case '&':
                if (off > last) {
                    super.write(cbuf, last, off - last);
                    last = off + 1;
                }
                super.write("&amp;");
                break;
            }
            off++;
        }
        if (off > last) super.write(cbuf, last, off - last);
    }

    /**
     * Write a single character to the destination, escaping if
     * necessary.
     * 
     * @param c the character to be written
     * 
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void write(int c) throws IOException {
        switch (c) {
        case '"':
            if (inAttribute) {
                super.write("&quot;");
                break;
            }
            // Fall through.
        default:
            super.write(c);
            break;

        case '<':
            super.write("&lt;");
            break;

        case '>':
            super.write("&gt;");
            break;

        case '&':
            super.write("&amp;");
            break;
        }
    }
}
