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

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

/**
 * Delegates to another source context with the ability to override some
 * fields.
 * 
 * @author simpsons
 */
public class FilterSourceContext extends SourceContext {
    /**
     * The base context, delegated to by default
     */
    protected final SourceContext base;

    /**
     * Created a filtered source context.
     * 
     * @param base a base context to delegate to by default
     */
    protected FilterSourceContext(SourceContext base) {
        this.base = base;
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link SourceContext#element()} on {@link #base}.
     */
    @Override
    public Element element() {
        return base.element();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link SourceContext#blockTag()} on {@link #base}.
     */
    @Override
    public boolean blockTag() {
        return base.blockTag();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link SourceContext#returnTag()} on {@link #base}.
     */
    @Override
    public boolean returnTag() {
        return base.returnTag();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link SourceContext#paramTag()} on {@link #base}.
     */
    @Override
    public int paramTag() {
        return base.paramTag();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link SourceContext#throwsTag()} on {@link #base}.
     */
    @Override
    public TypeMirror throwsTag() {
        return base.throwsTag();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link SourceContext#firstSentence()} on {@link #base}.
     */
    @Override
    public boolean firstSentence() {
        return base.firstSentence();
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes
     * {@link SourceContext#summaryTag()} on {@link #base}.
     */
    @Override
    public boolean summaryTag() {
        return base.summaryTag();
    }
}
