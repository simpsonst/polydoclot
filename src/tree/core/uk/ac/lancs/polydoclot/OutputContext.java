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
import java.util.function.Consumer;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;
import uk.ac.lancs.polydoclot.util.Escaper;
import uk.ac.lancs.polydoclot.util.Utils;

/**
 * Identifies how rich content can be, and how to relativize references,
 * in a given output context.
 * 
 * <p>
 * For example, an internal link from <samp>foo/bar/baz.html</samp> to
 * <samp>foo/quux.html</samp> (both relative to the documentation root),
 * can be expressed as <samp>../quux.html</samp>. The method
 * {@link #location()} provides the context for this
 * (<samp>foo/bar/baz.html</samp> in this example).
 * 
 * <p>
 * A link to the method <code>Foo.call()</code> from <code>Foo</code>
 * could be expressed simply as <code>call()</code>, whereas from class
 * <code>Bar</code> it would have to be expressed more fully as
 * <code>Foo.call()</code>. {@link #element()} provides the context for
 * this situation, being either <code>Foo</code> or <code>Bar</code> in
 * this example.
 * 
 * <p>
 * This class also tracks type variables that have already been
 * expressed, so that recursively defined ones do not result in infinite
 * recursion. {@link #isAccountedFor(TypeVariable)} tests for an already
 * expressed variable, and {@link #accountFor(TypeVariable)} creates a
 * new context where one has been expressed.
 * 
 * @author simpsons
 */
public abstract class OutputContext {
    /**
     * Type utilities to be used
     */
    protected final Types types;

    /**
     * Create a context to use some type utilities.
     * 
     * @param types the type utilities
     */
    OutputContext(Types types) {
        this.types = types;
    }

    /**
     * Get the location that content is being written to. This can be
     * used to relativize URIs.
     * 
     * @return the destination location
     */
    public abstract URI location();

    /**
     * Get the element that is being documented, if any. This can be
     * used to decide whether the text of a reference to (say) a class
     * member should include the containing class.
     * 
     * @return the documented element, or {@code null} if the generated
     * documentation does not correspond to any element
     */
    public abstract Element element();

    /**
     * Get the escaper for character data.
     * 
     * @return the escaper to be used for character data
     */
    public abstract Escaper escaper();

    /**
     * Escape a string using the main escaper. This method simply
     * invokes {@link Escaper#escape(CharSequence)} on
     * {@link #escaper()} as a convenience.
     * 
     * @param in the string to be escaped
     * 
     * @return the escaped string
     */
    public final String escape(CharSequence in) {
        return escaper().escape(in);
    }

    /**
     * Escape a string using the attribute escaper. This method simply
     * invokes {@link Escaper#escape(CharSequence)} on
     * {@link #attributeEscaper()} as a convenience.
     * 
     * @param in the string to be escaped
     * 
     * @return the escaped string
     */
    public final String escapeAttribute(CharSequence in) {
        return attributeEscaper().escape(in);
    }

    /**
     * Get the escaper for attributes.
     * 
     * @return the escaper to be used for attributes
     * 
     * @default This method returns the result of calling
     * {@link #escaper()}.
     */
    public Escaper attributeEscaper() {
        return escaper();
    }

    /**
     * Determine whether the given type variable has already been
     * accounted for in this scope. This is to prevent infinite
     * recursion when expressing recursively defined type parameters.
     * 
     * @param tv the type variable to test
     * 
     * @default This method returns {@code false}.
     * 
     * @return {@code true} if the type variable has been accounted for;
     * {@code false} otherwise
     */
    public boolean isAccountedFor(TypeVariable tv) {
        return false;
    }

    /**
     * Create a context in which a type variable has been accounted for.
     * 
     * @param tv the type variable to account for
     * 
     * @return the new context
     */
    public final OutputContext accountFor(TypeVariable tv) {
        return new FilterOutputContext(this) {
            @Override
            public boolean isAccountedFor(TypeVariable tv2) {
                return types.isSameType(tv, tv2) || super.isAccountedFor(tv2);
            }
        };
    }

    /**
     * Determine whether the reference is to the class statically or as
     * an instance. The main purpose of this method is to express a
     * containing type as a prefix to a static member, implying that any
     * type parameters on it should not be displayed.
     * 
     * @return {@code true} if the element is referenced with respect to
     * a static member
     */
    public boolean isStatic() {
        return false;
    }

    /**
     * Determine whether links can be marked up.
     * 
     * @return {@code true} if links can be marked up
     */
    public abstract boolean canLink();

    /**
     * Determine whether Java code should be marked up as such.
     * 
     * @return {@code true} if Java code should be marked up
     */
    public abstract boolean canMarkAsCode();

    /**
     * Determine whether in-line elements can be marked up.
     * 
     * @return {@code true} if in-line elements can be marked up
     */
    public abstract boolean canMarkUpInline();

    /**
     * Determine whether block elements can be marked up.
     * 
     * @return {@code true} if block elements can be marked up
     */
    public abstract boolean canMarkUpBlock();

    /**
     * Get a subcontext for pure character data within this context. All
     * tags are to be reduced, and the attribute escaper is made the
     * same as the main escaper.
     * 
     * @return the requested context
     */
    public final OutputContext forPureCharacterData() {
        return new FilterOutputContext(this) {
            @Override
            public boolean canLink() {
                return false;
            }

            @Override
            public boolean canMarkUpInline() {
                return false;
            }

            @Override
            public boolean canMarkUpBlock() {
                return false;
            }

            @Override
            public Escaper attributeEscaper() {
                return base.escaper();
            }
        };
    }

    /**
     * Get a subcontext for a reference with respect to a static member.
     * 
     * @return the requested context
     */
    public final OutputContext forStatic() {
        if (isStatic()) return this;
        return new FilterOutputContext(this) {
            @Override
            public boolean isStatic() {
                return true;
            }
        };
    }

    /**
     * Get a subcontext for an element documented within this context.
     * 
     * @param elem the element being documented
     * 
     * @return the requested context
     */
    public final OutputContext inElement(Element elem) {
        return new FilterOutputContext(this) {
            @Override
            public Element element() {
                return elem;
            }
        };
    }

    /**
     * Get a context for attributes in this context. All mark-up
     * elements are to be reduced, and the original attribute escaper is
     * used as both escapers.
     * 
     * @return the requested context
     */
    public final OutputContext inAttribute() {
        return new FilterOutputContext(this) {
            @Override
            public boolean canLink() {
                return false;
            }

            @Override
            public boolean canMarkUpInline() {
                return false;
            }

            @Override
            public boolean canMarkUpBlock() {
                return false;
            }

            @Override
            public Escaper escaper() {
                return base.attributeEscaper();
            }
        };
    }

    /**
     * Write a URI as an attribute relative to this context. The
     * provided URI is relativized against {@link #location()},
     * converted to an ASCII string, escaped as an attribute value, and
     * then written.
     * 
     * @param out the place to write the URI
     * 
     * @param loc the URI to write
     */
    public final void writeAttribute(Consumer<? super String> out, URI loc) {
        out.accept(escapeAttribute(Utils.relativize(location(), loc)
            .toASCIIString()));
    }

    /**
     * Get a context for link content in this context. Links and block
     * elements are reduced.
     * 
     * @return the requested context
     */
    public final OutputContext inLink() {
        return new FilterOutputContext(this) {
            @Override
            public boolean canLink() {
                return false;
            }

            @Override
            public boolean canMarkUpBlock() {
                return false;
            }
        };
    }

    /**
     * Get a context for text wrapped in <code>&lt;code&lt;</code>.
     * 
     * @return the requested context
     */
    public final OutputContext inCode() {
        return new FilterOutputContext(this) {
            @Override
            public boolean canMarkAsCode() {
                return false;
            }
        };
    }

    /**
     * Get a context for text in an in-line context.
     * 
     * @return the requested context
     */
    public final OutputContext inline() {
        return new FilterOutputContext(this) {
            @Override
            public boolean canMarkUpBlock() {
                return false;
            }
        };
    }

    /**
     * Get a block-level context.
     * 
     * @param types type utilities to be used
     * 
     * @param location the destination location
     * 
     * @param element the element being documented, if any
     * 
     * @param escaper the escaper used for most content
     * 
     * @param attrEscaper the escaper used for attributes
     * 
     * @return the requested context
     * 
     * @constructor
     */
    public static OutputContext forBlock(Types types, URI location,
                                         Element element, Escaper escaper,
                                         Escaper attrEscaper) {
        return new OutputContext(types) {
            @Override
            public boolean canLink() {
                return true;
            }

            @Override
            public boolean canMarkUpInline() {
                return true;
            }

            @Override
            public boolean canMarkUpBlock() {
                return true;
            }

            @Override
            public Escaper attributeEscaper() {
                return attrEscaper;
            }

            @Override
            public Escaper escaper() {
                return escaper;
            }

            @Override
            public URI location() {
                return location;
            }

            @Override
            public Element element() {
                return element;
            }

            @Override
            public boolean canMarkAsCode() {
                return true;
            }
        };
    }

    /**
     * Get a minimal context from this one.
     * 
     * @return the requested context
     * 
     * @constructor
     */
    public final OutputContext plain() {
        return OutputContext.plain(this.types, this.location(), this.element());
    }

    /**
     * Get a minimal context.
     * 
     * @param types type utilities to be used
     * 
     * @param element the element being documented, if any
     * 
     * @param location the destination location
     * 
     * @return the requested minimal context
     * 
     * @constructor
     */
    public static OutputContext plain(Types types, URI location,
                                      Element element) {
        return new OutputContext(types) {
            @Override
            public boolean canLink() {
                return false;
            }

            @Override
            public boolean canMarkUpInline() {
                return false;
            }

            @Override
            public boolean canMarkUpBlock() {
                return false;
            }

            @Override
            public Escaper escaper() {
                return Escaper.IDENTITY;
            }

            @Override
            public URI location() {
                return location;
            }

            @Override
            public Element element() {
                return element;
            }

            @Override
            public boolean canMarkAsCode() {
                return false;
            }
        };
    }
}
