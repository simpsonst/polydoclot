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

import java.net.URI;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import javax.lang.model.element.Element;

/**
 * Writes hypertext mark-up.
 * 
 * @author simpsons
 */
final class HypertextUpmarker extends Upmarker {
    private abstract class With extends FilterUpmarker {
        With(Upmarker base) {
            super(base);
        }

        @Override
        Upmarker inAttr() {
            return new InAttr(this);
        }

        @Override
        Upmarker inLink() {
            return new InLink(this);
        }

        @Override
        public Upmarker withElement(Element elem) {
            return new WithElement(this, elem);
        }

        @Override
        Upmarker setLocale(Locale locale) {
            return new WithLocale(this, locale);
        }

        @Override
        Upmarker setFragment(String id) {
            return new WithFragment(this, id);
        }

        @Override
        Upmarker push(String name) {
            return new WithPush(this, name);
        }
    }

    private class InAttr extends With {
        InAttr(Upmarker base) {
            super(base);
        }

        @Override
        public Upmarker closeElement() {
            throw new NoSuchElementException();
        }

        @Override
        public Upmarker closeElement(String name) {
            throw new NoSuchElementException();
        }

        @Override
        String tagName() {
            return null;
        }

        @Override
        Upmarker parent() {
            return null;
        }

        @Override
        public Upmarker cdata(IntStream text) {
            write(text.flatMap(c -> {
                switch (c) {
                default:
                    return IntStream.of(c);
                case '<':
                    return "&lt;".codePoints();
                case '>':
                    return "&gt;".codePoints();
                case '&':
                    return "&amp;".codePoints();
                case '"':
                    return "&quot;".codePoints();
                }
            }));
            return this;
        }

        @Override
        public boolean canMarkUp() {
            return false;
        }

        @Override
        public boolean isInCode() {
            return true;
        }

        @Override
        public boolean canLink() {
            return false;
        }
    }

    private class InLink extends With {
        InLink(Upmarker base) {
            super(base);
        }

        @Override
        public boolean canLink() {
            return false;
        }
    }

    private class WithFragment extends With {
        private final URI location;

        WithFragment(Upmarker base, String id) {
            super(base);
            location = base.location().resolve("#" + id);
        }

        @Override
        public URI location() {
            return location;
        }
    }

    private class WithLocale extends With {
        private final Locale locale;

        WithLocale(Upmarker base, Locale locale) {
            super(base);
            this.locale = locale;
        }

        @Override
        public Locale locale() {
            return locale;
        }
    }

    private class WithPush extends With {
        private final String name;

        WithPush(Upmarker base, String name) {
            super(base);
            this.name = name;
        }

        @Override
        String tagName() {
            return name;
        }
    }

    private class WithElement extends With {
        private final Element elem;

        WithElement(Upmarker base, Element elem) {
            super(base);
            this.elem = elem;
        }

        @Override
        public Element element() {
            return elem;
        }
    }

    private final Consumer<? super IntStream> out;
    private final URI location;

    HypertextUpmarker(Consumer<? super IntStream> out, URI location) {
        this.out = out;
        this.location = location;
    }

    @Override
    void write(IntStream codePoints) {
        out.accept(codePoints);
    }

    @Override
    public Upmarker cdata(IntStream text) {
        write(text.flatMap(c -> {
            switch (c) {
            default:
                return IntStream.of(c);
            case '<':
                return "&lt;".codePoints();
            case '>':
                return "&gt;".codePoints();
            case '&':
                return "&amp;".codePoints();
            }
        }));
        return this;
    }

    @Override
    public boolean canMarkUp() {
        return true;
    }

    @Override
    public boolean isInCode() {
        return false;
    }

    @Override
    public boolean canLink() {
        return true;
    }

    @Override
    public Upmarker withElement(Element elem) {
        return new WithElement(this, elem);
    }

    @Override
    public Element element() {
        return null;
    }

    @Override
    public URI location() {
        return location;
    }

    @Override
    public Locale locale() {
        return null;
    }

    @Override
    String tagName() {
        return null;
    }

    @Override
    public Upmarker closeElement() {
        throw new NoSuchElementException();
    }

    @Override
    public Upmarker closeElement(String name) {
        throw new NoSuchElementException();
    }

    @Override
    Upmarker inAttr() {
        return new InAttr(this);
    }

    @Override
    Upmarker inLink() {
        return new InLink(this);
    }

    @Override
    Upmarker setLocale(Locale locale) {
        return new WithLocale(this, locale);
    }

    @Override
    Upmarker setFragment(String id) {
        return new WithFragment(this, id);
    }

    @Override
    Upmarker push(String name) {
        return new WithPush(this, name);
    }
}
