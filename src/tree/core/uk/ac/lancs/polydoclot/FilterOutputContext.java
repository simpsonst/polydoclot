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

import java.net.URI;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeVariable;

import uk.ac.lancs.polydoclot.util.Escaper;

/**
 * Delegates to another output context with the ability to override some
 * fields.
 * 
 * @author simpsons
 */
public class FilterOutputContext extends OutputContext {
    /**
     * The base context, delegated to by default
     */
    protected final OutputContext base;

    /**
     * Create a filtered output context.
     * 
     * @param base a base context to delegate to by default
     */
    protected FilterOutputContext(OutputContext base) {
        super(base.types);
        this.base = base;
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link OutputContext#isStatic()} on {@link #base}.
     */
    @Override
    public boolean isStatic() {
        return base.isStatic();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link OutputContext#location()} on {@link #base}.
     */
    @Override
    public URI location() {
        return base.location();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link OutputContext#element()} on {@link #base}.
     */
    @Override
    public Element element() {
        return base.element();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link OutputContext#escaper()} on {@link #base}.
     */
    @Override
    public Escaper escaper() {
        return base.escaper();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link OutputContext#canLink()} on {@link #base}.
     */
    @Override
    public boolean canLink() {
        return base.canLink();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link OutputContext#canMarkUpInline()} on {@link #base}.
     */
    @Override
    public boolean canMarkUpInline() {
        return base.canMarkUpInline();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link OutputContext#canMarkUpBlock()} on {@link #base}.
     */
    @Override
    public boolean canMarkUpBlock() {
        return base.canMarkUpBlock();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link OutputContext#canMarkAsCode()} on {@link #base}.
     */
    @Override
    public boolean canMarkAsCode() {
        return base.canMarkAsCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link OutputContext#isAccountedFor(TypeVariable)} on
     * {@link #base}.
     */
    @Override
    public boolean isAccountedFor(TypeVariable tv) {
        return base.isAccountedFor(tv);
    }
}
