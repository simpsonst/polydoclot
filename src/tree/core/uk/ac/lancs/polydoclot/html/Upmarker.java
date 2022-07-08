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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import javax.lang.model.element.Element;

import uk.ac.lancs.polydoclot.util.Utils;

/**
 * Writes mark-up while exposing the context of its content. Retained
 * content includes URI and fragment identifier, natural language, and
 * Java element.
 * 
 * @author simpsons
 */
public abstract class Upmarker {
    private static <T> Collection<T>
        frozenCollection(Collection<T> into,
                         @SuppressWarnings("unchecked") T... members) {
        into.addAll(Arrays.asList(members));
        return Collections.unmodifiableCollection(into);
    }

    private static Collection<String> frozenStringSet(String... members) {
        return frozenCollection(new TreeSet<>(String.CASE_INSENSITIVE_ORDER),
                                members);
    }

    private static final Collection<String> EMPTY_ELEMENTS =
        frozenStringSet("br", "img", "link", "meta");

    @SuppressWarnings("serial")
    private static final Map<String, Collection<String>> IMPLICITLY_CLOSED_ELEMENTS =
        Collections
            .unmodifiableMap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER) {
                private void putMulti(String key, String... members) {
                    put(key, frozenStringSet(members));
                }

                {
                    putMulti("div", "p", "span", "code", "samp", "b", "u",
                             "strong", "em");
                }
            });

    private static boolean isEmptyElement(String tagName) {
        return EMPTY_ELEMENTS.contains(tagName);
    }

    private static boolean implicitlyCloses(String newTagName,
                                            String existingTagName) {
        return IMPLICITLY_CLOSED_ELEMENTS
            .getOrDefault(newTagName, Collections.emptySet())
            .contains(existingTagName);
    }

    final void write(CharSequence s) {
        write(s.codePoints());
    }

    final void write(char c) {
        write(IntStream.of(c));
    }

    abstract void write(IntStream codePoints);

    /**
     * Write character data presented as a stream of codepoints. Special
     * characters will be escaped according to context.
     * 
     * @param text the text to be written
     * 
     * @return this object
     */
    public abstract Upmarker cdata(IntStream text);

    /**
     * Write character data presented as a character sequence. Special
     * characters will be escaped according to context. This method
     * invokes {@link CharSequence#codePoints()} on its argument, and
     * passes the result to {@link #cdata(IntStream)}.
     * 
     * @param text the text to be written
     * 
     * @return this object
     */
    public final Upmarker cdata(CharSequence text) {
        return cdata(text.codePoints());
    }

    /**
     * Determine whether mark-up is possible in this context.
     * 
     * @return {@code true} if mark-up is possible; {@code false} inside
     * attribute values, <code>&lt;title&gt;</code> elements, or in
     * plain-text contexts
     */
    public abstract boolean canMarkUp();

    /**
     * Determine whether the context is already in a
     * <code>&lt;pre&gt;</code>, <code>&lt;code&gt;</code> or
     * <code>&lt;samp&gt;</code> element. This could be used to avoid
     * superfluous wrapping in such elements.
     * 
     * @return {@code true} if the context is already in a code element
     */
    public abstract boolean isInCode();

    /**
     * Determine whether a link can be marked up in the current context.
     * This could be used to avoid attempting to nest one link inside
     * another, but also detects contexts where linking is not permitted
     * in general.
     * 
     * @return {@code false} if the context is already in a link, or
     * otherwise does not permit linking (e.g., in an attribute, a
     * <code>&lt;title&gt;</code> element, or a plain-text context)
     */
    public abstract boolean canLink();

    /**
     * Get an upmarker in which a program element is being documented.
     * 
     * @param elem the element being documented
     * 
     * @return the new upmarker
     */
    public abstract Upmarker withElement(Element elem);

    /**
     * Determine which program element is being documented, if any.
     * 
     * @return the element being documented, or {@code null} if none
     */
    public abstract Element element();

    /**
     * Determine the URI relative to the document base of the document
     * being generated. This is affected by <code>id</code> attributes,
     * but only insofar as the fragment identifier being changed.
     * 
     * @return the URI of the generated document
     */
    public abstract URI location();

    /**
     * Get the parent context of this context.
     * 
     * @return the parent context, or {@code null} if there is none
     * 
     * @default This implementation returns {@code null}.
     */
    Upmarker parent() {
        return null;
    }

    /**
     * Get the fragment identifier of this context.
     * 
     * @return the fragment identifier, or {@code null} if not set
     */
    final String fragment() {
        URI loc = location();
        if (loc == null) return null;
        return loc.getFragment();
    }

    /**
     * Determine whether the context is in the identified fragment.
     * 
     * @param fragId the fragment identifier
     * 
     * @return {@code true} if the context is in the identified
     * fragment, even if also in more narrowly scoped mark-up elements
     */
    public final boolean isInFragment(String fragId) {
        String req = fragment();
        if (req == null) return fragId == null;
        if (req.equals(fragId)) return true;
        Upmarker parent = parent();
        if (parent == null) return false;
        return parent.isInFragment(fragId);
    }

    /**
     * Get the locale of the current context.
     * 
     * @return the locale of the current context
     */
    public abstract Locale locale();

    /**
     * Determine whether a given locale contained the locale of the
     * current context.
     * 
     * @param cand the candidate locale
     * 
     * @return {@code true} if the candidate locale contains the current
     * locale
     */
    public final boolean isCompatible(Locale cand) {
        return Utils.isCompatible(locale(), cand);
    }

    /**
     * Get the nearest enclosing mark-up element type.
     * 
     * @return the enclosing tag's name, or {@code null} if there is
     * none
     */
    abstract String tagName();

    /**
     * Start a new element.
     * 
     * @param name the element name
     * 
     * @return an object for populating the element's start tag with
     * attributes
     */
    public final StartTag openElement(String name) {
        Upmarker self = this;
        while (implicitlyCloses(name, self.tagName())) {
            self = self.closeElement();
        }
        return self.new StartTag(name);
    }

    /**
     * Close the current element.
     * 
     * @return the surrounding context
     */
    public abstract Upmarker closeElement();

    /**
     * Close all remaining elements.
     */
    public final void complete() {
        Upmarker level = this;
        while (level != null)
            level = closeElement();
    }

    /**
     * Close a named enclosing element. Other elements that need to be
     * closed before reaching the named element will first be closed.
     * 
     * @param name the name of the nearest element to close
     * 
     * @return the context surrounding the closed element
     */
    public abstract Upmarker closeElement(String name);

    /**
     * Switch to a context in which only attribute values can be
     * written.
     * 
     * @return the in-attribute context
     */
    abstract Upmarker inAttr();

    /**
     * Switch to a context in which a link is already in effect.
     * 
     * @return the in-link context
     */
    abstract Upmarker inLink();

    /**
     * Switch to a context in which only plain text can be written, but
     * otherwise with the same context.
     * 
     * @param out the destination for the plain text
     * 
     * @return the plain context
     */
    final Upmarker intoStream(Consumer<? super IntStream> out) {
        return new PlainUpmarker(out).setLocale(locale())
            .withElement(element());
    }

    final Upmarker intoString(Consumer<? super CharSequence> out) {
        Consumer<? super IntStream> is = s -> {
            s.forEach(cp -> out
                .accept(String.valueOf(Character.toChars(cp))));
        };
        return intoStream(is);
    }

    /**
     * Switch to a context in which a different locale applies.
     * 
     * @param locale the new locale
     * 
     * @return the new context
     */
    abstract Upmarker setLocale(Locale locale);

    abstract Upmarker setFragment(String id);

    /**
     * Push an element type.
     * 
     * @param name the new element type
     * 
     * @return the new context nested in the new element type
     */
    abstract Upmarker push(String name);

    /**
     * Gathers attributes of an opening or empty tag before writing
     * them. This allows it to detect changes in natural language and
     * fragment identifier.
     * 
     * @author simpsons
     */
    public final class StartTag {
        private final String name;

        private final Map<String, Consumer<? super Upmarker>> attrs =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private final Map<String, Predicate<? super Upmarker>> flags =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        StartTag(String name) {
            this.name = name;
        }

        /**
         * Set a boolean attribute.
         * 
         * @param name the attribute name
         * 
         * @param value the determinant of whether to set the attribute
         * 
         * @return this object
         */
        public StartTag setFlag(String name,
                                Predicate<? super Upmarker> value) {
            flags.put(name, value);
            attrs.remove(name);
            return this;
        }

        /**
         * Set a boolean attribute.
         * 
         * @param name the attribute name
         * 
         * @param value {@code false} if nothing is to be set
         * 
         * @return this object
         */
        public StartTag setFlag(String name, boolean value) {
            return setFlag(name, mo -> value);
        }

        /**
         * Set an attribute to a constant.
         * 
         * @param name the attribute name
         * 
         * @param value the constant value
         * 
         * @return this object
         */
        public StartTag setAttribute(String name, CharSequence value) {
            return setAttribute(name, mo -> mo.cdata(value));
        }

        /**
         * Set an attribute.
         * 
         * @param name the attribute name
         * 
         * @param provider the provider of the atrribute's value
         * 
         * @return this object
         */
        public StartTag setAttribute(String name,
                                     Consumer<? super Upmarker> provider) {
            attrs.put(name, provider);
            flags.remove(name);
            return this;
        }

        /**
         * Write out the start of the opening tag, but don't close it.
         * 
         * @return the context for the element's content
         */
        private Upmarker writeOut() {
            Upmarker ctxt = Upmarker.this.inAttr();
            Upmarker innerCtxt = Upmarker.this.push(name);

            /* Look for a language setting, and modify the contexts of
             * both the attributes and the element content. */
            if (attrs.containsKey("lang")) {
                StringBuilder tmp = new StringBuilder();
                attrs.get("lang").accept(ctxt.intoString(tmp::append));
                Locale newLocale = Locale.forLanguageTag(tmp.toString());
                ctxt = ctxt.setLocale(newLocale);
                innerCtxt = innerCtxt.setLocale(newLocale);
            }

            /* Look for an identifier setting, and modify the contexts
             * of both the attributes and the element content. */
            if (attrs.containsKey("id")) {
                StringBuilder tmp = new StringBuilder();
                attrs.get("id").accept(ctxt.intoString(tmp::append));
                ctxt = ctxt.setFragment(tmp.toString());
                innerCtxt = innerCtxt.setFragment(tmp.toString());
            }

            /* Detect a link being made, and modify the context of the
             * element content. */
            if (name.equalsIgnoreCase("a")) innerCtxt = innerCtxt.inLink();

            /* If mark-up is possible, write the mark-up to open the
             * start tag. */
            if (canMarkUp()) {
                write('<');
                write(name);
                for (Map.Entry<String, Consumer<? super Upmarker>> entry : attrs
                    .entrySet()) {
                    write(' ');
                    write(entry.getKey());
                    write("=\"");
                    /* TODO: Write into a string, and check whether
                     * quotes are needed. */
                    entry.getValue().accept(ctxt);
                    write('"');
                }
                for (Map.Entry<String, Predicate<? super Upmarker>> entry : flags
                    .entrySet()) {
                    if (!entry.getValue().test(ctxt)) continue;
                    write(' ');
                    write(entry.getKey());
                }
            }

            /* Provide the context for the element's content. */
            return innerCtxt;
        }

        /**
         * Write an empty element with the current set of attributes.
         * 
         * @return the original context in which the tag was created
         */
        public Upmarker close() {
            if (canMarkUp()) {
                writeOut();

                write('>');
                if (!isEmptyElement(name)) {
                    write("</");
                    write(name);
                    write('>');
                }
            }
            return Upmarker.this;
        }

        /**
         * Write a start tag with the current set of attributes.
         * 
         * @return the context within the new element, or the context
         * surrounding the tag if the element type is implicitly empty
         */
        public Upmarker leaveOpen() {
            if (isEmptyElement(name)) return close();
            Upmarker result = writeOut();
            if (canMarkUp()) write('>');
            return result;
        }
    }

    /**
     * Close elements on an inner upmarker until it matches this object.
     * 
     * @param inner the inner upmarker to be repeatedly closed
     * 
     * @return this object
     */
    public Upmarker reclaim(Upmarker inner) {
        while (inner != null && inner != this)
            inner = inner.closeElement();
        return this;
    }

    /**
     * Create a root hypertext upmarker at the document root.
     * 
     * @param location the URI-reference of the generated content
     * 
     * @param out the destination to write generated content
     * 
     * @return the root upmarker
     * 
     * @constructor
     */
    public static Upmarker toHypertext(TextDestination out, URI location) {
        return new HypertextUpmarker(out.out, location);
    }

    /**
     * Create a plain-text upmarker.
     * 
     * @param out the destination to write generated content
     * 
     * @return the plain-text upmarker
     * 
     * @constructor
     */
    public static Upmarker toPlainText(TextDestination out) {
        return new PlainUpmarker(out.out);
    }
}
