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

package uk.ac.lancs.polydoclot.html;

import java.net.URI;
import java.util.Locale;
import java.util.stream.IntStream;

import javax.lang.model.element.Element;

/**
 * 
 * 
 * @author simpsons
 */
abstract class FilterUpmarker extends Upmarker {
    protected final Upmarker base;

    protected FilterUpmarker(Upmarker base) {
        this.base = base;
    }

    @Override
    String tagName() {
        return base.tagName();
    }

    @Override
    Upmarker parent() {
        return base;
    }

    @Override
    public Upmarker closeElement() {
        return base.closeElement();
    }

    @Override
    public Upmarker closeElement(String name) {
        return base.closeElement(name);
    }

    @Override
    void write(IntStream codePoints) {
        base.write(codePoints);
    }

    @Override
    public Upmarker cdata(IntStream text) {
        base.cdata(text);
        return this;
    }

    @Override
    public boolean canMarkUp() {
        return base.canMarkUp();
    }

    @Override
    public boolean isInCode() {
        return base.isInCode();
    }

    @Override
    public boolean canLink() {
        return base.canLink();
    }

    @Override
    public Element element() {
        return base.element();
    }

    @Override
    public URI location() {
        return base.location();
    }

    @Override
    public Locale locale() {
        return base.locale();
    }
}
