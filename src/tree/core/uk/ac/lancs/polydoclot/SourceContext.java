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
 * Helps resolve relative references in source documentation.
 * 
 * <p>
 * For example <code>&#123;link foo.bar&#125;</code> could refer to a
 * package <code>foo.bar</code>, a class <code>bar</code> in package
 * <code>foo</code>, or a class known locally as <code>foo.bar</code>
 * because of imports or class nesting. Such references can be resolved
 * given the element which the source documentation applies to, hence
 * {@link #element()} to identify this context.
 * 
 * <p>
 * Another example if <code>&#123;@inheritDoc&#125;</code>. Not only
 * does this depend on the element whose documentation it is part of,
 * but on the block tag too, as it could refer to a specific parameter,
 * exception or the return value. {@link #blockTag()} provides this
 * additional context, but may be {@code null} when not required.
 * 
 * @author simpsons
 */
public abstract class SourceContext {
    SourceContext() {}

    /**
     * Determine which element provides the documentation source
     * 
     * @return the element whose documentation is being interpreted, or
     * {@code null} if the documentation does not come from an element
     */
    public abstract Element element();

    /**
     * Determine whether this is the context of some other tag.
     * 
     * @return {@code true} iff in some other tag
     */
    public abstract boolean blockTag();

    /**
     * Determine whether this is the context of a
     * <code>&#64;return</code> tag.
     * 
     * @return {@code true} iff in a <code>&#64;return</code> tag
     */
    public abstract boolean returnTag();

    /**
     * Determine by parameter position which <code>&#64;param</code> is
     * the current position.
     * 
     * @return the position of the parameter being documented by the
     * source in this context, or {@code -1} if not in a
     * <code>&#64;param</code> context
     */
    public abstract int paramTag();

    /**
     * Get the thrown type mentioned in the <code>&#64;throws</code>
     * context.
     * 
     * @return the thrown type, or {@code null} if not in a
     * <code>&#64;throws</code> context
     */
    public abstract TypeMirror throwsTag();

    /**
     * Determine whether this is the context of a
     * <code>&#64;summary</code> tag.
     * 
     * @return {@code true} iff in a <code>&#64;summary</code> tag
     */
    public abstract boolean summaryTag();

    /**
     * Determine whether the source is considered to be inside a first
     * sentence.
     * 
     * @return {@code true} if the source is inside a first sentence
     */
    public abstract boolean firstSentence();

    /**
     * An empty context, where everything must be specified in full
     */
    public static final SourceContext EMPTY = new SourceContext() {
        @Override
        public boolean blockTag() {
            return false;
        }

        @Override
        public Element element() {
            return null;
        }

        @Override
        public boolean firstSentence() {
            return false;
        }

        @Override
        public boolean returnTag() {
            return false;
        }

        @Override
        public int paramTag() {
            return -1;
        }

        @Override
        public TypeMirror throwsTag() {
            return null;
        }

        @Override
        public boolean summaryTag() {
            return false;
        }
    };

    /**
     * Get a subcontext for a block tag that is not
     * <code>&#64;summary</code>, <code>&#64;return</code>,
     * <code>&#64;param</code> or <code>&#64;throws</code>
     * 
     * @return the requested context
     */
    public final SourceContext inBlockTag() {
        return new FilterSourceContext(this) {
            @Override
            public boolean blockTag() {
                return true;
            }

            @Override
            public boolean firstSentence() {
                return false; // TODO: Possibly delegate?
            }

            @Override
            public boolean returnTag() {
                return false;
            }

            @Override
            public int paramTag() {
                return -1;
            }

            @Override
            public TypeMirror throwsTag() {
                return null;
            }

            @Override
            public boolean summaryTag() {
                return false;
            }
        };
    }

    /**
     * Lose the block tag from the context.
     * 
     * @return a new context for the same element, but with no tag
     * context
     */
    public final SourceContext inElement() {
        return new FilterSourceContext(this) {
            @Override
            public boolean blockTag() {
                return false;
            }

            @Override
            public boolean firstSentence() {
                return false; // TODO: Possible delegate?
            }

            @Override
            public boolean returnTag() {
                return false;
            }

            @Override
            public int paramTag() {
                return -1;
            }

            @Override
            public TypeMirror throwsTag() {
                return null;
            }

            @Override
            public boolean summaryTag() {
                return false;
            }
        };
    }

    /**
     * Create a context for being in a first sentence.
     * 
     * @return the requested context
     */
    public final SourceContext inFirstSentence() {
        return new FilterSourceContext(this) {
            @Override
            public boolean firstSentence() {
                return true;
            }
        };
    }

    /**
     * Create a subcontext for being in a <code>&#64;return</code> tag.
     * 
     * @return the requested context
     */
    public final SourceContext inReturnTag() {
        if (element() == null) throw new IllegalStateException();
        return new FilterSourceContext(this) {
            @Override
            public boolean blockTag() {
                return false;
            }

            @Override
            public boolean returnTag() {
                return true;
            }

            @Override
            public int paramTag() {
                return -1;
            }

            @Override
            public TypeMirror throwsTag() {
                return null;
            }

            @Override
            public boolean firstSentence() {
                return false; // TODO: Possible delegate?
            }

            @Override
            public boolean summaryTag() {
                return false;
            }
        };
    }

    /**
     * Create a subcontext for being in a <code>&#64;summary</code> tag.
     * 
     * @return the requested context
     */
    public final SourceContext inSummaryTag() {
        if (element() == null) throw new IllegalStateException();
        return new FilterSourceContext(this) {
            @Override
            public boolean blockTag() {
                return false;
            }

            @Override
            public boolean returnTag() {
                return false;
            }

            @Override
            public int paramTag() {
                return -1;
            }

            @Override
            public TypeMirror throwsTag() {
                return null;
            }

            @Override
            public boolean firstSentence() {
                return false; // TODO: Possible delegate?
            }

            @Override
            public boolean summaryTag() {
                return true;
            }
        };
    }

    /**
     * Create a subcontext for being in a <code>&#64;throws</code> tag
     * with a particular type.
     * 
     * @param type the type whose throwing is documented by the tag
     * 
     * @return the requested context
     */
    public final SourceContext inThrowsTag(TypeMirror type) {
        if (element() == null) throw new IllegalStateException();
        return new FilterSourceContext(this) {
            @Override
            public boolean blockTag() {
                return false;
            }

            @Override
            public boolean returnTag() {
                return false;
            }

            @Override
            public int paramTag() {
                return -1;
            }

            @Override
            public TypeMirror throwsTag() {
                return type;
            }

            @Override
            public boolean firstSentence() {
                return false; // TODO: Possible delegate?
            }

            @Override
            public boolean summaryTag() {
                return false;
            }
        };
    }

    /**
     * Create a subcontext for being in a <code>&#64;param</code> tag
     * for a positional parameter of a method/constructor.
     * 
     * @param pos the position of the parameter in its
     * method/constructor
     * 
     * @return the requested context
     */
    public final SourceContext inParamTag(int pos) {
        if (pos < 0) throw new IllegalArgumentException("negative pos");
        if (element() == null) throw new IllegalStateException();
        return new FilterSourceContext(this) {
            @Override
            public boolean blockTag() {
                return false;
            }

            @Override
            public boolean returnTag() {
                return false;
            }

            @Override
            public int paramTag() {
                return pos;
            }

            @Override
            public TypeMirror throwsTag() {
                return null;
            }

            @Override
            public boolean firstSentence() {
                return false; // TODO: Possible delegate?
            }

            @Override
            public boolean summaryTag() {
                return false;
            }
        };
    }

    /**
     * Create a context for a complete element.
     * 
     * @param elem the element whose documentation is being extracted
     * 
     * @return a context for the specific element, but no specific block
     * tag
     * 
     * @constructor
     */
    public static SourceContext forElement(Element elem) {
        return new SourceContext() {
            @Override
            public Element element() {
                return elem;
            }

            @Override
            public boolean blockTag() {
                return false;
            }

            @Override
            public boolean firstSentence() {
                return false;
            }

            @Override
            public boolean returnTag() {
                return false;
            }

            @Override
            public int paramTag() {
                return -1;
            }

            @Override
            public TypeMirror throwsTag() {
                return null;
            }

            @Override
            public boolean summaryTag() {
                return false;
            }
        };
    }
}
