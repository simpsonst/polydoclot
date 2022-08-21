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

import com.sun.source.doctree.AuthorTree;
import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import java.lang.annotation.Documented;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;

/**
 * Holds miscellaneous utilies that are doclet-specific, but require no
 * doclet environment.
 * 
 * @author simpsons
 */
public final class DocUtils {
    private DocUtils() {}

    /**
     * Write CSS classes to describe an element's modifiers.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the output context
     * 
     * @param mods the set of modifiers
     */
    public static void
        writeModifierStyleClasses(Consumer<? super String> out,
                                  OutputContext outCtxt,
                                  Collection<? extends Modifier> mods) {
        for (Modifier mod : mods) {
            out.accept(" mod-");
            out.accept(outCtxt.escapeAttribute(mod.name().toLowerCase()));
        }
    }

    /**
     * Compares classes by longish name.
     */
    public static final Comparator<TypeElement> ENCLOSED_NAME_ORDER =
        new Comparator<TypeElement>() {
            @Override
            public int compare(TypeElement arg0, TypeElement arg1) {
                return String.CASE_INSENSITIVE_ORDER
                    .compare(getSortingName(arg0), getSortingName(arg1));
            }
        };

    /**
     * Determine whether a documentation comment has a description worth
     * showing. It's not if there's only a first sentence, and it has
     * already been shown as the summary.
     * 
     * @param doc the comment
     * 
     * @param checkForDefault {@code true} if a
     * <code>&#64;default</code> block tag should also be checked for
     * 
     * @return {@code true} if a first sentence is present, and either a
     * body or a <code>&#64;resume</code> or <code>&#64;summary</code>
     * block tag
     */
    public static boolean hasDescription(DocCommentTree doc,
                                         boolean checkForDefault) {
        if (doc == null) return false;
        if (doc.getFirstSentence().isEmpty()) return false;
        if (!doc.getBody().isEmpty()) return true;
        if (DocUtils.getUnknownBlockTags(doc, "resume").iterator().hasNext())
            return true;
        if (DocUtils.getUnknownBlockTags(doc, "summary").iterator().hasNext())
            return true;
        if (checkForDefault && (DocUtils.getUnknownBlockTags(doc, "default")
            .iterator().hasNext() ||
            DocUtils.getUnknownBlockTags(doc, "apiNote").iterator().hasNext() ||
            DocUtils.getUnknownBlockTags(doc, "implNote").iterator()
                .hasNext() ||
            DocUtils.getUnknownBlockTags(doc, "implSpec").iterator().hasNext()))
            return true;
        return false;
    }

    /**
     * Get the longish name of a class. This includes enclosing classes,
     * but not package names.
     * 
     * @param elem the class
     * 
     * @return the class's long name
     */
    public static String getSortingName(TypeElement elem) {
        StringBuilder result = new StringBuilder(elem.getSimpleName());
        do {
            Element enc = elem.getEnclosingElement();
            switch (enc.getKind()) {
            case ANNOTATION_TYPE:
            case CLASS:
            case INTERFACE:
            case ENUM:
                result.insert(0, '.');
                result.insert(0, enc.getSimpleName());
                elem = (TypeElement) enc;
                break;

            default:
                return result.toString();
            }
        } while (true);
    }

    /**
     * Get the CSS classes used to describe an element.
     * 
     * @param refed the element to be described
     * 
     * @return the classes as a space-separated string
     */
    public static String getStyleClass(Element refed) {
        switch (refed.getKind()) {
        case ENUM:
            return "class enum";

        case INTERFACE:
            return "class iface";

        case ANNOTATION_TYPE:
            return "class annot";

        case CLASS:
            return "class plain";

        case CONSTRUCTOR:
            return "member exec constr";

        case METHOD:
            return "member exec method";

        case ENUM_CONSTANT:
            return "member field const";

        case PACKAGE:
            return "pkg";

        case MODULE:
            return "module";

        case FIELD:
            return "member field";

        case TYPE_PARAMETER:
            return "type-param";

        default:
            /* It shouldn't be possible to reference anything else. */
            throw new AssertionError("unnecessary");
        }
    }

    /**
     * Find a tag in a Javadoc comment for a particular parameter.
     * 
     * @param all the source comment
     * 
     * @param name the parameter name
     * 
     * @param type true if a type parameter is sought, rather than a
     * method/constructor parameter
     * 
     * @return the matching tag, or {@code null} if none found
     */
    public static ParamTree findParameter(DocCommentTree all, CharSequence name,
                                          boolean type) {
        if (all == null) return null;
        for (ParamTree node : getBlockTags(all, ParamTree.class)) {
            if (node.isTypeParameter() != type) continue;
            if (node.getName().getName().contentEquals(name)) return node;
        }
        return null;
    }

    /**
     * Get all extension block tags from a Javadoc comment.
     * 
     * @param comm the source comment
     * 
     * @return an iteration over all extension tags
     */
    public static Iterable<UnknownBlockTagTree>
        getUnknownBlockTags(DocCommentTree comm) {
        return DocUtils.getBlockTags(comm, UnknownBlockTagTree.class,
                                     t -> true);
    }

    /**
     * Get all extension block tags with a given tag name from a Javadoc
     * comment.
     * 
     * @param comm the source comment
     * 
     * @param names tag names, e.g., <samp>foo</samp> to match
     * <code>&#64;foo</code> tags
     * 
     * @return an iteration over all matching extension tags
     */
    public static Iterable<UnknownBlockTagTree>
        getUnknownBlockTags(DocCommentTree comm, String... names) {
        Collection<String> nameSet = new HashSet<>(Arrays.asList(names));
        return DocUtils.getBlockTags(comm, UnknownBlockTagTree.class,
                                     t -> nameSet.contains(t.getTagName()));
    }

    /**
     * Get the first block tag from a Javadoc comment matching a filter
     * and having a specific class type.
     * 
     * @param comm the source comment
     * 
     * @param type the tag class type
     * 
     * @param filter a predicate selecting tags to include
     * 
     * @return the first matching tag, or {@code null} if none found
     */
    public static <T extends BlockTagTree> T
        getBlockTag(DocCommentTree comm, Class<T> type,
                    Predicate<? super T> filter) {
        for (T tag : getBlockTags(comm, type, filter))
            return tag;
        return null;
    }

    /**
     * Get all block tags from a Javadoc comment that have a specific
     * class type.
     * 
     * @param comm the source comment
     * 
     * @param type the tag class type
     * 
     * @return an iteration over all matching tags, cast to the
     * requested type
     */
    public static <T extends BlockTagTree> Iterable<T>
        getBlockTags(DocCommentTree comm, Class<T> type) {
        return getBlockTags(comm, type, t -> true);
    }

    /**
     * Get all block tags from a Javadoc comment that match a filter and
     * have a specific class type.
     * 
     * @param comm the source comment
     * 
     * @param type the tag class type
     * 
     * @param filter a predicate selecting tags to include
     * 
     * @return an iteration over all matching tags, cast to the
     * requested type
     */
    public static <T extends BlockTagTree> Iterable<T>
        getBlockTags(DocCommentTree comm, Class<T> type,
                     Predicate<? super T> filter) {
        if (comm == null) return Collections.emptySet();
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    final Iterator<? extends DocTree> inner =
                        comm.getBlockTags().iterator();

                    T next;

                    boolean ensure() {
                        while (next == null && inner.hasNext()) {
                            DocTree tt = inner.next();
                            if (!type.isInstance(tt)) continue;
                            T bt = type.cast(tt);
                            if (!filter.test(bt)) continue;
                            next = bt;
                        }
                        return next != null;
                    }

                    @Override
                    public boolean hasNext() {
                        return ensure();
                    }

                    @Override
                    public T next() {
                        if (ensure()) {
                            T result = next;
                            next = null;
                            return result;
                        }
                        throw new NoSuchElementException();
                    }
                };
            }
        };
    }

    /**
     * Get all <code>&#64;author</code> tags from a Javadoc comment.
     * 
     * @param comm the source comment
     * 
     * @return an iteration over all <code>&#64;author</code> tags
     */
    public static Iterable<AuthorTree> getAuthorTags(DocCommentTree comm) {
        return getBlockTags(comm, AuthorTree.class,
                            t -> t.getTagName().equals("author"));
    }

    /**
     * Get the first <code>&#64;return</code> tag from a Javadoc
     * comment.
     * 
     * @param comm the source comment
     * 
     * @return the first matching tag, or {@code null} if none present
     */
    public static ReturnTree getReturnTag(DocCommentTree comm) {
        Iterator<ReturnTree> list =
            getBlockTags(comm, ReturnTree.class,
                         t -> t.getTagName().equals("return")).iterator();
        if (list.hasNext()) return list.next();
        return null;
    }

    /**
     * Get all <code>&#64;see</code> tags from a Javadoc comment.
     * 
     * @param comm the source comment
     * 
     * @return an iteration over all <code>&#64;see</code> tags
     */
    public static Iterable<SeeTree> getSeeTags(DocCommentTree comm) {
        return getBlockTags(comm, SeeTree.class,
                            t -> t.getTagName().equals("see"));
    }

    /**
     * Get all <code>&#64;throws</code> and <code>&#64;exception</code>
     * tags from a Javadoc comment.
     * 
     * @param comm the source comment
     * 
     * @return an iteration over all <code>&#64;throws</code> and
     * <code>&#64;exception</code> tags
     */
    public static Iterable<ThrowsTree> getThrowsTags(DocCommentTree comm) {
        return getBlockTags(comm, ThrowsTree.class,
                            t -> t.getTagName().equals("throws") ||
                                t.getTagName().equals("exception"));
    }

    /**
     * Determine whether an HTML attribute contains a URI.
     * 
     * @param elemName the element name
     * 
     * @param attrName the attribute name
     * 
     * @return {@code true} if the attribute contains a URI
     */
    public static boolean isURIAttribute(String elemName, String attrName) {
        elemName = elemName.toLowerCase();
        attrName = attrName.toLowerCase();
        switch (attrName) {
        case "action":
            switch (elemName) {
            case "blockquote":
            case "del":
            case "ins":
            case "form":
            case "q":
                return true;
            }
            return false;

        case "href":
            switch (elemName) {
            case "a":
                return true;
            }
            return false;

        case "codebase":
            switch (elemName) {
            case "applet":
                return true;
            }
            return false;

        case "data":
            switch (elemName) {
            case "object":
                return true;
            }
            return false;

        case "usemap":
            switch (elemName) {
            case "img":
            case "input":
            case "object":
                return true;
            }
            return false;

        case "poster":
            switch (elemName) {
            case "video":
                return true;
            }
            return false;

        case "src":
            switch (elemName) {
            case "audio":
            case "embed":
            case "iframe":
            case "img":
            case "input":
            case "script":
            case "source":
            case "track":
            case "video":
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * Get the list of documented annotations applied to an element.
     * 
     * @param elem the element whose annotations are requested
     * 
     * @return the selected annotations
     */
    public static List<? extends AnnotationMirror>
        getDocumentedAnnotationMirrors(AnnotatedConstruct elem) {
        return elem.getAnnotationMirrors().stream()
            .filter(am -> am.getAnnotationType().asElement()
                .getAnnotation(Documented.class) != null)
            .collect(Collectors.toList());
    }

    /**
     * Get the long name for a type, ignoring annotations. The doclet
     * API seems to lack a way to strip annotations from a type, and
     * {@link TypeMirror#toString()} includes the annotations.
     * 
     * @param type the type whose string representation is required
     * 
     * @return the long name for a type, without annotations
     */
    public static String getLongName(TypeMirror type) {
        switch (type.getKind()) {
        case ARRAY:
            return getLongName(((ArrayType) type).getComponentType()) + "[]";

        case BOOLEAN:
            return "boolean";

        case BYTE:
            return "byte";

        case CHAR:
            return "char";

        case DECLARED:
            DeclaredType dt = (DeclaredType) type;
            TypeElement te = (TypeElement) dt.asElement();
            return te.getQualifiedName().toString();

        case DOUBLE:
            return "double";

        case ERROR:
            return "<error>";

        case FLOAT:
            return "float";

        case INT:
            return "int";

        case INTERSECTION:
            IntersectionType it = (IntersectionType) type;
            return String.join(" & ", it.getBounds().stream()
                .map(DocUtils::getLongName).collect(Collectors.toList()));

        case LONG:
            return "long";

        case NONE:
            return "<none>";

        case NULL:
            return "<null>";

        case OTHER:
            return "<other>";

        case SHORT:
            return "short";

        case UNION:
            UnionType ut = (UnionType) type;
            return String.join(" | ", ut.getAlternatives().stream()
                .map(DocUtils::getLongName).collect(Collectors.toList()));

        case VOID:
            return "void";

        case TYPEVAR:
            TypeVariable tv = (TypeVariable) type;
            String tvr = tv.asElement().getSimpleName().toString();
            if (tv.getLowerBound() != null)
                tvr += " super " + getLongName(tv.getLowerBound());
            if (tv.getUpperBound() != null)
                tvr += " extends " + getLongName(tv.getUpperBound());
            return tvr;

        case WILDCARD:
            WildcardType wt = (WildcardType) type;
            String wtr = "?";
            if (wt.getSuperBound() != null)
                wtr += " super " + getLongName(wt.getSuperBound());
            if (wt.getExtendsBound() != null)
                wtr += " extends " + getLongName(wt.getExtendsBound());
            return wtr;

        default:
            throw new IllegalArgumentException("kind: " + type.getKind());
        }
    }
}
