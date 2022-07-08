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

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.AuthorTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocRootTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.InheritDocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.doctree.ValueTree;
import com.sun.source.doctree.VersionTree;
import com.sun.source.util.SimpleDocTreeVisitor;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.ProcessBuilder.Redirect;
import java.lang.StackWalker.StackFrame;
import java.lang.annotation.Documented;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.FormatFlagsConversionMismatchException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleElementVisitor9;
import javax.lang.model.util.SimpleTypeVisitor9;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import uk.ac.lancs.polydoclot.util.Escaper;
import uk.ac.lancs.polydoclot.util.HypertextEscaper;
import uk.ac.lancs.polydoclot.util.MacroFormatter;
import uk.ac.lancs.polydoclot.util.Utils;

/**
 * Provides context for a specific slice.
 * 
 * @author simpsons
 */
public final class Slice {
    /**
     * The slice-independent configuration
     */
    public final Configuration config;

    /**
     * The slice parameters
     */
    public final SliceSpecification spec;

    private final ResourceBundle contentBundle;

    /**
     * Create a slice context.
     * 
     * @param config the slice-independent configuration
     * 
     * @param spec the slice parameters
     */
    public Slice(Configuration config, SliceSpecification spec) {
        this.config = config;
        this.spec = spec;
        this.contentBundle = ResourceBundle
            .getBundle(getClass().getPackage().getName() + ".Headings",
                       this.spec.locale,
                       getClass().getClassLoader());
    }

    /**
     * Format properties into a localized string.
     * 
     * @param key the key to look up the localized format
     * 
     * @param props the set of properties to plug into the format
     * 
     * @param escaper an escaper for literal content in the format
     * 
     * @return the formatted string
     */
    public String macroFormat(String key, Properties props, Escaper escaper) {
        String text = contentBundle.getString(key);
        MacroFormatter fmt = new MacroFormatter(text);
        fmt.escape(escaper);
        return fmt.format(props);
    }

    /**
     * Get a localized macro format consisting of Javadoc tags and HTML,
     * apply properties to it, then interpret as Javadoc. The macro
     * format uses square brackets as delimiters instead of braces,
     * which would clash with Javadoc tags.
     * 
     * @param key the key identifying the format
     * 
     * @param props the unescaped properties
     * 
     * @return a sequence of Javadoc trees
     */
    public List<? extends DocTree> macroFormatDoc(String key,
                                                  Properties props) {
        String text = contentBundle.getString(key);
        MacroFormatter fmt =
            MacroFormatter.syntax().openOn('[').closeOn(']').create(text);
        String raw = "<body>" + fmt.format(props) + "</body>";
        FileObject file = new FileObject() {
            @Override
            public URI toUri() {
                throw new UnsupportedOperationException("unnecessary");
            }

            @Override
            public Writer openWriter() throws IOException {
                throw new UnsupportedOperationException("unnecessary");
            }

            @Override
            public Reader openReader(boolean ignoreEncodingErrors)
                throws IOException {
                return new StringReader(raw);
            }

            @Override
            public OutputStream openOutputStream() throws IOException {
                throw new UnsupportedOperationException("unnecessary");
            }

            @Override
            public InputStream openInputStream() throws IOException {
                throw new UnsupportedOperationException("unnecessary");
            }

            @Override
            public String getName() {
                return "foo.html";
            }

            @Override
            public long getLastModified() {
                throw new UnsupportedOperationException("unnecessary");
            }

            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors)
                throws IOException {
                return raw;
            }

            @Override
            public boolean delete() {
                throw new UnsupportedOperationException("unnecessary");
            }
        };
        DocCommentTree tree = config.docTrees.getDocCommentTree(file);
        return tree.getFullBody();
    }

    /**
     * Format arguments into localized content.
     * 
     * @param key the name of the template to use, loaded from the
     * <samp>Headings</samp> resource bundle using the slice's locale
     * 
     * @param args the arguments to be inserted into the template
     * 
     * @return the expanded content
     */
    public String format(String key, Object... args) {
        String text = contentBundle.getString(key);
        MessageFormat fmt = new MessageFormat(text, spec.locale);
        return fmt.format(args, new StringBuffer(), null).toString();
    }

    /**
     * Get the raw text of localized content.
     * 
     * @param key the content key
     * 
     * @return the content
     */
    public String getContent(String key) {
        return contentBundle.getString(key);
    }

    /**
     * Write tags as hypertext to a string.
     * 
     * @param inCtxt the documentation context for resolving relative
     * references
     * 
     * @param outCtxt the permitted richness of the generated content
     * 
     * @param doc the document sequence
     * 
     * @return the generated string
     */
    public String toHypertext(SourceContext inCtxt, OutputContext outCtxt,
                              Iterable<? extends DocTree> doc) {
        StringBuilder result = new StringBuilder();
        toHypertext(result::append, inCtxt, outCtxt, doc);
        return result.toString();
    }

    /**
     * Covert an iteration over Javadoc tags into a new one where
     * adjacent simple tags are resolved into a single text tag. Simple
     * tags are text tags, entity tags matching known entities, and
     * keyword tags.
     * 
     * @param in the source iteration
     * 
     * @return the output iteration
     */
    public Iterable<DocTree> resolveTextTags(Iterable<? extends DocTree> in) {
        return new Iterable<DocTree>() {
            @Override
            public Iterator<DocTree> iterator() {
                return new Iterator<DocTree>() {
                    final Iterator<? extends DocTree> source = in.iterator();

                    DocTree fetched;

                    final StringBuilder cache = new StringBuilder();

                    /* Ensure that we have the next non-text node lined
                     * up. */
                    boolean ensureFetched() {
                        while (fetched == null) {
                            if (!source.hasNext()) return false;
                            fetched = source.next();
                            DocTreeVisitor<Boolean, Void> action =
                                new SimpleDocTreeVisitor<>(false) {
                                    @Override
                                    public Boolean visitEntity(EntityTree node,
                                                               Void p) {
                                        final int codePoint = HypertextEscaper
                                            .codePointForEntity(node.getName());
                                        if (codePoint < 0) return false;
                                        cache.appendCodePoint(codePoint);
                                        return true;
                                    }

                                    @Override
                                    public Boolean
                                        visitUnknownInlineTag(UnknownInlineTagTree node,
                                                              Void p) {
                                        switch (node.getTagName()) {
                                        case "keyword":
                                            Matcher m = keywordPattern
                                                .matcher(config.treeText(node
                                                    .getContent()));
                                            if (m.matches()) {
                                                cache.append(m.group(1));
                                                return true;
                                            }
                                            return false;
                                        }

                                        return false;
                                    }

                                    @Override
                                    public Boolean visitText(TextTree node,
                                                             Void p) {
                                        cache.append(node.getBody());
                                        return true;
                                    }
                                };
                            if (!fetched.accept(action, null)) break;
                            fetched = null;
                        }
                        return true;
                    }

                    @Override
                    public boolean hasNext() {
                        ensureFetched();
                        if (cache.length() > 0) return true;
                        if (fetched != null) return true;
                        return source.hasNext();
                    }

                    @Override
                    public DocTree next() {
                        ensureFetched();
                        if (cache.length() > 0) {
                            DocTree result = config.docTreeFactory
                                .newTextTree(cache.toString());
                            cache.delete(0, cache.length());
                            return result;
                        }
                        if (fetched != null) {
                            DocTree result = fetched;
                            fetched = null;
                            return result;
                        }
                        assert !source.hasNext();
                        throw new NoSuchElementException();
                    }
                };
            }
        };
    }

    /**
     * Write a document sequence to a formatter.
     * 
     * @param out the destination
     * 
     * @param inCtxt the documentation context for resolving relative
     * references
     * 
     * @param outCtxt the permitted richness of the generated content
     * 
     * @param doc the document sequence
     */
    public void toHypertext(Consumer<? super CharSequence> out,
                            SourceContext inCtxt, OutputContext outCtxt,
                            Iterable<? extends DocTree> doc) {
        doc = resolveTextTags(doc);
        DocTreeVisitor<Void, Void> visitor = new SimpleDocTreeVisitor<>() {
            @Override
            public Void visitDocRoot(DocRootTree node, Void p) {
                out.accept(outCtxt.escape(
                                          Utils
                                              .relativize(outCtxt.location(),
                                                          config.outputDirectory
                                                              .toUri())
                                              .toASCIIString()));
                return null;
            }

            @Override
            public Void visitEntity(EntityTree node, Void p) {
                out.accept("&");
                out.accept(node.getName());
                out.accept(";");
                return null;
            }

            @Override
            public Void visitEndElement(EndElementTree node, Void p) {
                switch (node.getName().toString()) {
                case "div":
                case "p":
                case "address":
                case "h1":
                case "h2":
                case "h3":
                case "h4":
                case "h5":
                case "h6":
                case "ul":
                case "ol":
                case "dl":
                case "pre":
                case "blockquote":
                case "form":
                case "fieldset":
                case "hr":
                case "noscript":
                case "table":
                    if (!outCtxt.canMarkUpBlock()) return null;
                    break;

                case "a":
                    if (!outCtxt.canLink()) return null;
                    // Fall through
                default:
                    if (!outCtxt.canMarkUpInline()) return null;
                    break;
                }
                out.accept("</");
                out.accept(node.getName());
                out.accept(">");
                return null;
            }

            @Override
            public Void visitInheritDoc(InheritDocTree node, Void p) {
                /* Create a sequence of extended classes or overridden
                 * elements to examine. */
                final Collection<TypeElement> candTypes;
                final TypeElement typeCtxt;
                final ExecutableElement methCtxt;
                switch (inCtxt.element().getKind()) {
                case CLASS:
                case ENUM:
                case INTERFACE:
                case ANNOTATION_TYPE:
                    typeCtxt = (TypeElement) inCtxt.element();
                    methCtxt = null;
                    candTypes = config.getInheritanceOrder(typeCtxt);
                    // config.addSupertypes(candTypes, typeCtxt);
                    break;

                case METHOD:
                    if (inCtxt.element().getModifiers()
                        .contains(Modifier.STATIC)) {
                        config
                            .report(Kind.WARNING,
                                    inCtxt.element(),
                                    "output.tag.inheritDoc.failure.unsupported");
                        return null;
                    }
                    methCtxt = (ExecutableElement) inCtxt.element();
                    typeCtxt =
                        (TypeElement) inCtxt.element().getEnclosingElement();
                    candTypes = config.getInheritanceOrder(typeCtxt);
                    // config.addSupertypes(candTypes, typeCtxt);
                    break;

                default:
                    config.report(Kind.WARNING,
                                  inCtxt.element(),
                                  "output.tag.inheritDoc.failure.unsupported");
                    return null;
                }

                /* If we're in a block tag, scan the supertypes for
                 * corresponding documentation in overridden methods. */
                if (inCtxt.returnTag()) {
                    /* We're in a @return tag. */
                    try {
                        writeInheritedReturnDocs(out,
                                                 (ExecutableElement) inCtxt
                                                     .element(),
                                                 outCtxt,
                                                 false);
                        return null;
                    } catch (IllegalArgumentException ex) {}
                    config.report(Kind.WARNING,
                                  inCtxt.element(),
                                  "output.tag.inheritDoc"
                                      + ".failure.unsupported");
                    return null;
                }

                if (inCtxt.summaryTag()) {
                    /* We're in a @resume tag. */
                    if (methCtxt != null) {
                        /* If we're in a method, scan methods in
                         * supertypes that we override. */
                        for (TypeElement cand : candTypes) {
                            /* Find all methods of this class that we
                             * override. */
                            for (ExecutableElement membCand : ElementFilter
                                .methodsIn(cand.getEnclosedElements())) {
                                if (!config.elements
                                    .overrides(methCtxt, membCand, typeCtxt))
                                    continue;
                                @SuppressWarnings("unused")
                                SourceContext newCtxt = SourceContext
                                    .forElement(membCand).inSummaryTag();
                                if (writeSummary(out, outCtxt, membCand))
                                    return null;
                            }
                        }
                    } else {
                        /* Scan supertypes. */
                        for (TypeElement cand : candTypes) {
                            @SuppressWarnings("unused")
                            SourceContext newCtxt =
                                SourceContext.forElement(cand).inSummaryTag();
                            if (writeSummary(out, outCtxt, cand)) return null;
                        }
                    }
                    return null;
                }

                if (inCtxt.paramTag() >= 0) {
                    try {
                        writeInheritedParamDocs(out,
                                                (ExecutableElement) inCtxt
                                                    .element(),
                                                inCtxt.paramTag(),
                                                outCtxt,
                                                false);
                        return null;
                    } catch (IllegalArgumentException ex) {}
                    config.report(Kind.WARNING,
                                  inCtxt.element(),
                                  "output.tag.inheritDoc"
                                      + ".failure.unsupported");
                    return null;
                }

                if (inCtxt.throwsTag() != null) {
                    /* We're in a @throws tag for this type. */
                    try {
                        writeInheritedThrowsDocs(out,
                                                 (ExecutableElement) inCtxt
                                                     .element(),
                                                 inCtxt.throwsTag(),
                                                 outCtxt,
                                                 false);
                        return null;
                    } catch (IllegalArgumentException ex) {
                        config.report(Kind.WARNING,
                                      inCtxt.element(),
                                      "output.tag.inheritDoc"
                                          + ".failure.unsupported");
                    }
                    return null;
                }

                if (inCtxt.blockTag()) {
                    /* We're in another block tag for which
                     * {@inheritDoc} has no meaning. */
                    config.report(Kind.WARNING,
                                  inCtxt.element(),
                                  "output.tag.inheritDoc.failure.unsupported");
                    return null;
                }
                /* We're in the body of the member documentation. */

                if (methCtxt != null) {
                    /* We're in the main body of a method. Scan
                     * supertypes for overridden methods. */
                    for (TypeElement cand : candTypes) {
                        /* Find all methods of this class that we
                         * override. */
                        for (ExecutableElement membCand : ElementFilter
                            .methodsIn(cand.getEnclosedElements())) {
                            if (!config.elements
                                .overrides(methCtxt, membCand, typeCtxt))
                                continue;
                            DocCommentTree dct =
                                config.docTrees.getDocCommentTree(membCand);
                            if (dct == null) continue;
                            List<? extends DocTree> docs =
                                inCtxt.firstSentence() ?
                                    dct.getFirstSentence() : dct.getFullBody();
                            if (docs == null) continue;
                            if (docs.isEmpty()) continue;
                            SourceContext newCtxt =
                                SourceContext.forElement(membCand);
                            toHypertext(out, newCtxt, outCtxt, docs);
                        }
                    }
                    return null;
                }

                /* We're in the main body of a class. Scan
                 * supertypes. */
                for (TypeElement cand : candTypes) {
                    DocCommentTree dct =
                        config.docTrees.getDocCommentTree(cand);
                    if (dct == null) continue;
                    List<? extends DocTree> docs = inCtxt.firstSentence() ?
                        dct.getFirstSentence() : dct.getFullBody();
                    if (docs == null) continue;
                    if (docs.isEmpty()) continue;
                    toHypertext(out,
                                SourceContext.forElement(cand),
                                outCtxt,
                                docs);
                    break;
                }
                return null;
            }

            @Override
            public Void visitStartElement(StartElementTree node, Void p) {
                switch (node.getName().toString()) {
                case "div":
                case "p":
                case "address":
                case "h1":
                case "h2":
                case "h3":
                case "h4":
                case "h5":
                case "h6":
                case "ul":
                case "ol":
                case "dl":
                case "pre":
                case "blockquote":
                case "form":
                case "fieldset":
                case "hr":
                case "noscript":
                case "table":
                    if (!outCtxt.canMarkUpBlock()) return null;
                    break;

                case "a":
                    if (!outCtxt.canLink()) return null;
                    // Fall through
                default:
                    if (!outCtxt.canMarkUpInline()) return null;
                    break;
                }

                out.accept("<");
                out.accept(node.getName());
                for (DocTree dattr : node.getAttributes()) {
                    AttributeTree attr = (AttributeTree) dattr;
                    out.accept(" ");
                    out.accept(attr.getName());
                    List<? extends DocTree> value = attr.getValue();
                    /* If the author writes the likes of <a href="foo">,
                     * he likely assumes a package- or class-relative
                     * context, but that won't work in general because
                     * class members are not output to the same file as
                     * the class itself. We have to work out what the
                     * author originally meant, and re-relativize it
                     * against what it is now. */
                    if (DocUtils.isURIAttribute(node.getName().toString(),
                                                attr.getName().toString())) {
                        /* Get the referenced location. */
                        URI loc = URI.create(toHypertext(inCtxt,
                                                         outCtxt.plain(),
                                                         value));

                        /* Find out what the author thinks it means in
                         * absolute terms. */
                        loc = config.getAuthorAssumedLocation(inCtxt.element())
                            .resolve(loc);

                        /* Re-relativize it against where it's actually
                         * going. */
                        loc = Utils.relativize(outCtxt.location(), loc);

                        /* Convert back to tree. */
                        value = config.newTextTree(loc.toASCIIString());
                    }
                    switch (attr.getValueKind()) {
                    case EMPTY:
                        break;

                    case UNQUOTED:
                        out.accept("=");
                        toHypertext(out, inCtxt, outCtxt, value);
                        break;

                    case SINGLE:
                    case DOUBLE:
                        out.accept("=\"");
                        toHypertext(out, inCtxt, outCtxt.inAttribute(), value);
                        out.accept("\"");
                        break;
                    }
                }
                if (node.isSelfClosing()) out.accept("/");
                out.accept(">");
                return null;
            }

            @Override
            public Void visitLink(LinkTree node, Void p) {
                /* Find out what entity we're dealing with, and check
                 * for errors. */
                final boolean plain =
                    node.getTagName().contentEquals("linkplain");
                final List<? extends DocTree> label =
                    config.unflattenDoc(node.getLabel());
                final String sig = node.getReference().getSignature();
                final Element refed =
                    config.resolveSignature(inCtxt.element(), sig);
                if (refed == null) {
                    config.report(Kind.WARNING,
                                  inCtxt.element(),
                                  "output.tag.link.failure.missing",
                                  sig);
                    if (outCtxt.canMarkUpInline())
                        out.accept("<strong class=\"error\">");
                    out.accept(outCtxt.escape("[BAD LINK " + sig + "]"));
                    if (outCtxt.canMarkUpInline()) out.accept("</strong>");
                    return null;
                }
                OutputContext newCtxt = plain ? outCtxt.inCode() : outCtxt;
                writeElementReference(out,
                                      newCtxt,
                                      refed,
                                      LinkContent.forLabel(inCtxt, label)
                                          .withoutNonessentialContainers());
                return null;
            }

            @Override
            public Void visitLiteral(LiteralTree node, Void p) {
                switch (node.getTagName()) {
                case "code":
                    if (outCtxt.canMarkUpInline())
                        out.accept("<code class=\"javadoc\">");
                    out.accept(outCtxt.escape(node.getBody().getBody()));
                    if (outCtxt.canMarkUpInline()) out.accept("</code>");
                    break;

                default:
                    out.accept(outCtxt.escape(node.getBody().getBody()));
                    break;
                }
                return null;
            }

            @Override
            public Void visitText(TextTree node, Void p) {
                out.accept(outCtxt.escape(node.getBody()));
                return null;
            }

            @Override
            public Void visitUnknownInlineTag(UnknownInlineTagTree node,
                                              Void p) {
                /* For some reason, unknown in-line tags' content is not
                 * re-parsed, even though the API permits it. */
                List<? extends DocTree> content = config.unflattenDoc(node);

                switch (node.getTagName()) {
                default:
                    config.report(Kind.WARNING,
                                  inCtxt.element(),
                                  "output.tag.failure.unknown",
                                  node.getTagName());
                    if (outCtxt.canMarkUpInline())
                        out.accept("<span class=\"unknown-tag tag-"
                            + node.getTagName() + "\">");
                    toHypertext(out, inCtxt, outCtxt, content);
                    if (outCtxt.canMarkUpInline()) out.accept("</span>");
                    break;

                case "plainvalue": {
                    List<DocTree> rem = new ArrayList<>();
                    String sig = config.extractLabel(content, rem);
                    final Element refed;
                    List<DocTree> rem2 = new ArrayList<>();
                    final String fmt = config.extractLabel(rem, rem2);
                    if (sig == null || ".".equals(sig)) {
                        refed = inCtxt.element();
                        if (refed == null ||
                            !refed.getModifiers().contains(Modifier.STATIC) ||
                            !refed.getModifiers().contains(Modifier.FINAL)) {
                            config
                                .report(Kind.ERROR,
                                        "output.tag.value.failure.outside-constant");
                            if (outCtxt.canMarkUpInline())
                                out.accept("<strong class=\"error\">");
                            out.accept(outCtxt.escape("[BAD VALUE]"));
                            if (outCtxt.canMarkUpInline())
                                out.accept("</strong>");
                            return null;
                        }
                    } else {
                        refed = config.resolveSignature(inCtxt.element(), sig);
                        if (refed == null) {
                            if (inCtxt.element() == null)
                                config.report(Kind.ERROR,
                                              "output.tag.link.failure.missing",
                                              sig);
                            else
                                config.report(Kind.ERROR,
                                              inCtxt.element(),
                                              "output.tag.link.failure.missing",
                                              sig);
                            if (outCtxt.canMarkUpInline())
                                out.accept("<strong class=\"error\">");
                            out.accept(outCtxt
                                .escape("[BAD VALUE FOR " + sig + "]"));
                            if (outCtxt.canMarkUpInline())
                                out.accept("</strong>");
                            return null;
                        }
                    }

                    switch (refed.getKind()) {
                    default:
                        if (inCtxt.element() == null)
                            config
                                .report(Kind.ERROR,
                                        "output.tag.value.failure.outside-constant");
                        else
                            config
                                .report(Kind.ERROR,
                                        inCtxt.element(),
                                        "output.tag.value.failure.outside-constant");
                        if (outCtxt.canMarkUpInline())
                            out.accept("<strong class=\"error\">");
                        out.accept(outCtxt.escape("[BAD VALUE]"));
                        if (outCtxt.canMarkUpInline()) out.accept("</strong>");
                        return null;

                    case FIELD:
                        VariableElement var = (VariableElement) refed;
                        Object constant = var.getConstantValue();
                        if (constant == null) {
                            if (inCtxt.element() == null)
                                config
                                    .report(Kind.ERROR,
                                            "output.tag.value.failure.outside-constant");
                            else
                                config
                                    .report(Kind.ERROR,
                                            inCtxt.element(),
                                            "output.tag.value.failure.outside-constant");
                            if (outCtxt.canMarkUpInline())
                                out.accept("<strong class=\"error\">");
                            out.accept(outCtxt.escape("[BAD VALUE]"));
                            if (outCtxt.canMarkUpInline())
                                out.accept("</strong>");
                            return null;
                        }

                        /* TODO: Use output context locale to format the
                         * string. */
                        try {
                            String exp =
                                String.format("%" + (fmt == null ? "s" : fmt),
                                              constant);
                            out.accept(outCtxt.escape(exp));
                        } catch (FormatFlagsConversionMismatchException ex) {
                            config.report(Kind.ERROR,
                                          inCtxt.element(),
                                          "output.tag.value.failure.bad-format",
                                          ex.getConversion(),
                                          ex.getFlags());
                            if (outCtxt.canMarkUpInline())
                                out.accept("<strong class=\"error\">");
                            out.accept(outCtxt.escape("[BAD FORMAT]"));
                            if (outCtxt.canMarkUpInline())
                                out.accept("</strong>");
                            return null;
                        }
                    }
                    break;
                }

                case "keyword":
                    config.report(Kind.WARNING,
                                  inCtxt.element(),
                                  "output.tag.keyword.failure.unresolved",
                                  node.getTagName());
                    if (outCtxt.canMarkUpInline())
                        out.accept("<strong class=\"error\">");
                    out.accept(outCtxt
                        .escape("[UNRESOLVED {@" + node.getTagName() + "}]"));
                    if (outCtxt.canMarkUpInline()) out.accept("</strong>");
                    break;

                case "h1":
                case "h2":
                case "h3": {
                    final int level =
                        2 + Integer.parseInt(node.getTagName().substring(1));
                    if (outCtxt.canMarkUpBlock()) {
                        out.accept("\n<h" + level);
                        out.accept("><span>");
                        toHypertext(out, inCtxt, outCtxt, content);
                        out.accept("</span></h" + level);
                        out.accept(">\n");
                    } else {
                        out.accept("\n");
                        out.accept(String
                            .join("", Collections.nCopies(level - 2, "=")));
                        out.accept(" ");
                        toHypertext(out, inCtxt, outCtxt, content);
                        out.accept(" ");
                        out.accept(String
                            .join("", Collections.nCopies(level - 2, "=")));
                        out.accept("\n");
                    }
                }
                    break;

                case "select": {
                    /* Find the first matching {@lang} node. Ignore
                     * everything else. */
                    for (DocTree sub : content) {
                        if (sub.accept(new SimpleDocTreeVisitor<Boolean,
                                                                Void>(false) {
                            @Override
                            public Boolean
                                visitUnknownInlineTag(UnknownInlineTagTree node,
                                                      Void p) {
                                if (node.getTagName().equals("else")) {
                                    toHypertext(out,
                                                inCtxt,
                                                outCtxt,
                                                config.unflattenDoc(node));
                                    return true;
                                }
                                if (!node.getTagName().equals("lang"))
                                    return false;
                                List<DocTree> rem = new ArrayList<>();
                                String offerText =
                                    config.extractFlatLabel(node, rem);
                                Locale offer = Locale.forLanguageTag(offerText);
                                /* TODO: Get required locale from output
                                 * context. */
                                Locale req = spec.locale;
                                if (!Utils.isCompatible(req, offer))
                                    return false;
                                toHypertext(out, inCtxt, outCtxt, rem);
                                return true;
                            }
                        }, null)) break;
                    }
                }
                    break;

                case "lang": {
                    List<DocTree> rem = new ArrayList<>();
                    String offerText = config.extractFlatLabel(node, rem);
                    Locale offer = Locale.forLanguageTag(offerText);
                    /* TODO: Get required locale from output context. */
                    Locale req = spec.locale;
                    if (Utils.isCompatible(req, offer)) {
                        toHypertext(out, inCtxt, outCtxt, rem);
                    }
                }
                    break;

                case "content": {
                    /* Get the resource key, look it up, and return the
                     * content parsed as Javadoc. */
                    List<DocTree> rem = new ArrayList<>();
                    String key = config.extractFlatLabel(node, rem);
                    String flat = contentBundle.getString(key);
                    toHypertext(out,
                                inCtxt,
                                outCtxt,
                                config.unflattenDoc(flat));
                }
                    break;
                }

                return null;
            }

            @Override
            public Void visitValue(ValueTree node, Void p) {
                final Element refed;

                if (node.getReference() == null) {
                    /* This must be a plain {@value} tag inside a
                     * field's documentation. */
                    refed = inCtxt.element();
                    if (refed == null ||
                        !refed.getModifiers().contains(Modifier.STATIC) ||
                        !refed.getModifiers().contains(Modifier.FINAL)) {
                        config
                            .report(Kind.ERROR,
                                    "output.tag.value.failure.outside-constant");
                        if (outCtxt.canMarkUpInline())
                            out.accept("<strong class=\"error\">");
                        out.accept(outCtxt.escape("[BAD VALUE]"));
                        if (outCtxt.canMarkUpInline()) out.accept("</strong>");
                        return null;
                    }
                } else {
                    /* Extract the signature. */
                    final String sig = node.getReference().getSignature();

                    /* Find out what element, if any, it refers to. */
                    refed = config.resolveSignature(inCtxt.element(), sig);
                    if (refed == null) {
                        if (inCtxt.element() == null)
                            config.report(Kind.ERROR,
                                          "output.tag.link.failure.missing",
                                          sig);
                        else
                            config.report(Kind.ERROR,
                                          inCtxt.element(),
                                          "output.tag.link.failure.missing",
                                          sig);
                        if (outCtxt.canMarkUpInline())
                            out.accept("<strong class=\"error\">");
                        out.accept(outCtxt
                            .escape("[BAD VALUE FOR " + sig + "]"));
                        if (outCtxt.canMarkUpInline()) out.accept("</strong>");
                        return null;
                    }
                }

                /* Only field elements have values. */
                switch (refed.getKind()) {
                default:
                    if (inCtxt.element() == null)
                        config
                            .report(Kind.ERROR,
                                    "output.tag.value.failure.outside-constant");
                    else
                        config
                            .report(Kind.ERROR,
                                    inCtxt.element(),
                                    "output.tag.value.failure.outside-constant");
                    if (outCtxt.canMarkUpInline())
                        out.accept("<strong class=\"error\">");
                    out.accept(outCtxt.escape("[BAD VALUE]"));
                    if (outCtxt.canMarkUpInline()) out.accept("</strong>");
                    return null;

                case FIELD:
                    VariableElement var = (VariableElement) refed;
                    Object constant = var.getConstantValue();
                    if (constant == null) {
                        if (inCtxt.element() == null)
                            config
                                .report(Kind.ERROR,
                                        "output.tag.value.failure.outside-constant");
                        else
                            config
                                .report(Kind.ERROR,
                                        inCtxt.element(),
                                        "output.tag.value.failure.outside-constant");
                        if (outCtxt.canMarkUpInline())
                            out.accept("<strong class=\"error\">");
                        out.accept(outCtxt.escape("[BAD VALUE]"));
                        if (outCtxt.canMarkUpInline()) out.accept("</strong>");
                        return null;
                    }

                    final String rep;
                    if (constant instanceof String) {
                        StringBuilder buf = new StringBuilder("\"");
                        constant.toString().codePoints().flatMap(c -> {
                            switch (c) {
                            case '\t':
                                return IntStream.builder().add('\\').add('t')
                                    .build();

                            case '\f':
                                return IntStream.builder().add('\\').add('f')
                                    .build();

                            case '\r':
                                return IntStream.builder().add('\\').add('r')
                                    .build();

                            case '\n':
                                return IntStream.builder().add('\\').add('n')
                                    .build();

                            case '\b':
                                return IntStream.builder().add('\\').add('b')
                                    .build();

                            case '"':
                                return IntStream.builder().add('\\').add('"')
                                    .build();

                            default:
                                return IntStream.builder().add(c).build();
                            }
                        }).collect(() -> buf,
                                   StringBuilder::appendCodePoint,
                                   StringBuilder::append);
                        buf.append('"');
                        rep = buf.toString();
                    } else if (constant instanceof Character) {
                        char c = ((Character) constant).charValue();
                        switch (c) {
                        case '\t':
                            rep = "'\\t'";
                            break;

                        case '\b':
                            rep = "'\\b'";
                            break;

                        case '\n':
                            rep = "'\\n'";
                            break;

                        case '\r':
                            rep = "'\\r'";
                            break;

                        case '\f':
                            rep = "'\\f'";
                            break;

                        case '\'':
                            rep = "'\\''";
                            break;

                        default:
                            rep = "'" + c + "'";
                            break;
                        }
                    } else if (constant instanceof Long) {
                        rep = constant.toString() + "L";
                    } else if (constant instanceof Float) {
                        rep = constant.toString() + "F";
                    } else if (constant == null) {
                        rep = "null";
                    } else {
                        rep = constant.toString();
                    }
                    if (outCtxt.canMarkUpInline()) out.accept("<code>");
                    out.accept(outCtxt.escape(rep));
                    if (outCtxt.canMarkUpInline()) out.accept("</code>");
                    break;
                }

                return null;
            }
        };

        for (DocTree dt : doc) {
            dt.accept(visitor, null);
        }
    }

    /**
     * Write a detailed author description.
     * 
     * @param out the destination for content
     * 
     * @param inCtxt the context for resolving source
     * 
     * @param blockCtxt the context in which the content is generated
     * 
     * @param author the author's details
     */
    public void writeAuthor(PrintWriter out, SourceContext inCtxt,
                            OutputContext blockCtxt, Author author) {
        out.printf("<div class=\"javadoc-author javadoc-author-%s\""
            + " id=\"javadoc-author-%s\">\n",
                   blockCtxt.escapeAttribute(author.id),
                   blockCtxt.escapeAttribute(author.id));

        if (author.name != null) {
            out.print("<div class=\"name\">");
            if (author.site != null) {
                out.print("<a href=\"");
                toHypertext(out::append,
                            inCtxt,
                            blockCtxt.inAttribute(),
                            author.site);
                out.print("\">");
            }
            toHypertext(out::append, inCtxt, blockCtxt, author.name);
            if (author.site != null) out.print("</a>");
            out.print("</div>\n");
        }

        if (author.email != null) {
            out.print("<div class=\"email\"><samp>");
            out.print("<a href=\"mailto:");
            toHypertext(out::append,
                        inCtxt,
                        blockCtxt.inAttribute(),
                        author.email);
            out.print("\">");
            toHypertext(out::append, inCtxt, blockCtxt, author.email);
            out.print("</a>");
            out.print("</samp></div>\n");
        }

        if (author.telephone != null) {
            out.print("<div class=\"tel\">");
            String num =
                toHypertext(inCtxt, blockCtxt.inAttribute(), author.telephone)
                    .replaceAll("\\s", "");
            out.printf("<a href=\"tel:%s\">", num);
            toHypertext(out::append, inCtxt, blockCtxt, author.telephone);
            out.print("</a>");
            out.print("</div>\n");
        }

        if (author.address != null) {
            out.print("<address class=\"addr\">\n");
            toHypertext(out::append,
                        inCtxt,
                        blockCtxt.inline(),
                        author.address);
            out.print("</address>\n");
        }

        out.print("</div>");
    }

    private static int
        getSubtypeDepth(Map<? extends TypeElement,
                            ? extends Collection<? extends TypeElement>> subtypes,
                        Collection<? extends TypeElement> roots) {
        if (roots == null || roots.isEmpty()) return 0;
        int max = 0;
        for (TypeElement root : roots) {
            int cand = getSubtypeDepth(subtypes, subtypes.get(root));
            if (cand > max) max = cand;
        }
        return 1 + max;
    }

    /**
     * Write a table showing a class hierarchy with the given roots.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the context for generating context
     * 
     * @param roots the set of classes to appear as the roots of the
     * hierarchy
     */
    public void writeClassHierarchy(PrintWriter out, OutputContext blockCtxt,
                                    Collection<? extends TypeElement> roots) {
        final int depth = getSubtypeDepth(config.knownDirectSubtypes, roots);

        out.printf("<table class=\"javadoc-hierarchy\" summary=\"");
        toHypertext(out::append,
                    SourceContext.EMPTY,
                    blockCtxt.inAttribute(),
                    getTreeContent("class-hierarchy.table-summary"));
        out.printf("\">\n");
        out.printf("<caption>%s</caption>\n",
                   blockCtxt
                       .escape(getContent("section.heading.class-hierarchy")));
        out.printf("<tbody>\n");
        writeSubtypes(out,
                      blockCtxt,
                      config.knownDirectSubtypes,
                      depth,
                      new AtomicBoolean(false),
                      0,
                      roots);
        out.printf("</tbody>\n");
        out.printf("</table>\n");
    }

    private void
        writeSubtypes(PrintWriter out, OutputContext blockCtxt,
                      Map<? extends TypeElement,
                          ? extends Collection<? extends TypeElement>> subtypes,
                      int maxDepth, AtomicBoolean odd, int depth,
                      Collection<? extends TypeElement> roots) {
        if (roots == null) return;
        if (roots.isEmpty()) return;
        for (TypeElement root : roots.stream()
            .sorted(DocUtils.ENCLOSED_NAME_ORDER)
            .collect(Collectors.toList())) {
            out.printf("<tr class=\"item %s%s\">",
                       odd.get() ? "odd" : "even",
                       config.deprecatedElements.containsKey(root) ?
                           " deprecated" : "");
            if (depth > 1) out
                .printf("<td class=\"pad\" colspan=\"%d\"></td>\n", depth - 1);
            if (depth > 0) out.printf("<td class=\"bullet\">\u2ba1</td>\n");
            odd.set(!odd.get());
            out.printf("<td class=\"link\" align=\"left\"" + " colspan=\"%d\">",
                       maxDepth - depth);
            writeElementReference(out::append,
                                  blockCtxt,
                                  root,
                                  LinkContent.NORMAL);
            out.printf("</td>");
            out.printf(" <td class=\"purpose\">");
            writeSummary(out::append, blockCtxt, root);
            out.printf("</td></tr>\n");
            writeSubtypes(out,
                          blockCtxt,
                          subtypes,
                          maxDepth,
                          odd,
                          depth + 1,
                          subtypes.get(root));
        }
    }

    /**
     * Write an element's summary or its synthetic description.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the context for determining the level of detail of
     * the content
     * 
     * @param srcCtxt the documentation context for resolving relative
     * references
     * 
     * @param elem the element to be summarized
     */
    public void
        writeSummaryOrSyntheticDescription(Consumer<? super CharSequence> out,
                                           OutputContext outCtxt,
                                           SourceContext srcCtxt,
                                           Element elem) {
        if (!this.writeSummary(out, outCtxt, elem))
            this.toHypertext(out,
                             srcCtxt,
                             outCtxt,
                             this.getSyntheticDescription(elem));
    }

    /**
     * Write a summary of an elements qualities.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the context for generating context
     * 
     * @param elem the element to document
     */
    public void writeElementQualities(PrintWriter out, OutputContext outCtxt,
                                      Element elem) {
        sourceLog(out::append, outCtxt);
        final PackageElement pkg = config.elements.getPackageOf(elem);
        final ModuleElement mod = config.elements.getModuleOf(elem);
        final Element typeContainer = elem.getEnclosingElement();
        final boolean deprecated = config.deprecatedElements.containsKey(elem);
        final DocCommentTree comm = config.docTrees.getDocCommentTree(elem);

        out.printf("<table class=\"javadoc-qualities %s%s\" summary=\"",
                   outCtxt.escape(DocUtils.getStyleClass(elem)),
                   deprecated ? " deprecated" : "");
        toHypertext(out::append,
                    SourceContext.EMPTY,
                    outCtxt.inAttribute(),
                    getTreeContent("quality.table-summary"));
        out.print("\">\n");

        out.printf("<tr class=\"summary\"><td class=\"name\">%s</td>"
            + " <td class=\"value\">",
                   outCtxt.escape(this.getContent("quality.summary")));
        SourceContext srcCtxt = SourceContext.forElement(elem);
        if (!this.writeSummary(out::append, outCtxt, elem)) {
            toHypertext(out::append,
                        srcCtxt,
                        outCtxt,
                        getSyntheticDescription(elem));
        }
        out.printf("</td></tr>\n");

        out.printf("<tr class=\"dist\"><td class=\"name\">%s</td>"
            + " <td class=\"value\"><a href=\"%s\">%s</a></td></tr>\n",
                   outCtxt.escape(this.getContent("quality.distribution")),
                   outCtxt.escapeAttribute(Utils
                       .relativize(outCtxt.location(),
                                   config.outputDirectory.toUri()
                                       .resolve("overview-summary"
                                           + config.hypertextLinkSuffix))
                       .toASCIIString()),
                   this.toHypertext(SourceContext.EMPTY,
                                    outCtxt,
                                    config.title));
        Path elemFile = config.locateSource(elem);
        while (elemFile != null) {
            String jar = config.dirToJar.get(elemFile);
            if (jar != null) {
                out.printf("<tr class=\"jar\"><td class=\"name\">%s</td>"
                    + " <td class=\"value\"><samp>%s</samp></td></tr>\n",
                           outCtxt.escape(this.getContent("quality.archive")),
                           outCtxt.escape(jar));
                break;
            }
            elemFile = elemFile.getParent();
        }

        if (mod != null && mod != elem) {
            out.printf("<tr class=\"module\"><td class=\"name\">%s</td>",
                       outCtxt.escape(this
                           .getContent("quality.containing-module")));
            out.print(" <td class=\"value\">");
            if (mod.isUnnamed()) {
                out.print(outCtxt.escape(this.getContent("module.unnamed")));
            } else {
                writeSummary(out::append, outCtxt, mod);
            }
            out.printf("</td>\n");
            out.printf("</tr>\n");
        }

        if (pkg != null && pkg != elem) {
            out.printf("<tr class=\"package\"><td class=\"name\">%s</td>",
                       outCtxt.escape(this
                           .getContent("quality.containing-package")));
            out.printf(" <td class=\"value\">");
            if (pkg.isUnnamed()) {
                out.print(outCtxt.escape(this.getContent("package.unnamed")));
            } else {
                writeElementReference(out::append,
                                      outCtxt,
                                      pkg,
                                      LinkContent.NORMAL);
                out.print("<br/>");
                writeSummary(out::append, outCtxt, pkg);
            }
            out.printf("</td>\n");
            out.printf("</tr>\n");
        }

        for (VersionTree tag : DocUtils.getBlockTags(comm, VersionTree.class)) {
            out.printf("<tr class=\"version\"><td class=\"name\">%s</td>",
                       outCtxt.escape(this.getContent("quality.version")));
            out.printf(" <td class=\"value\">");
            toHypertext(out::append,
                        SourceContext.forElement(elem),
                        outCtxt,
                        tag.getBody());
            out.printf("</td>\n");
            out.printf("</tr>\n");
        }

        for (SinceTree tag : DocUtils.getBlockTags(comm, SinceTree.class)) {
            out.printf("<tr class=\"since\"><td class=\"name\">%s</td>",
                       outCtxt.escape(this.getContent("quality.since")));
            out.printf(" <td class=\"value\">");
            toHypertext(out::append,
                        SourceContext.forElement(elem),
                        outCtxt,
                        tag.getBody());
            out.printf("</td>\n");
            out.printf("</tr>\n");
        }

        if (deprecated) {
            out.printf("<tr class=\"deprecation\"><td class=\"name\">%s</td>",
                       outCtxt.escape(this.getContent("quality.deprecation")));
            out.printf(" <td class=\"value\"><p>");
            DeprecatedTree tag =
                DocUtils.getBlockTag(comm, DeprecatedTree.class, t -> true);
            if (tag == null || tag.getBody().isEmpty()) {
                Collection<Element> cause = config.deprecators.get(elem);
                if (cause == null || cause.isEmpty()) {
                    out.print(outCtxt
                        .escape(this.getContent("deprecation.unspecified")));
                } else {
                    out.print(outCtxt
                        .escape(this.getContent("deprecation.implied")));
                    out.printf("<ul>");
                    for (Element c : cause) {
                        out.printf("<li>");
                        writeElementReference(out::append,
                                              outCtxt,
                                              c,
                                              LinkContent.NORMAL
                                                  .withoutNonessentialContainers());
                    }
                    out.printf("</ul>");
                }
            } else {
                toHypertext(out::append,
                            SourceContext.forElement(elem),
                            outCtxt,
                            tag.getBody());
            }
            out.printf("</td>\n");
            out.printf("</tr>\n");
        }

        switch (typeContainer.getKind()) {
        case CLASS:
        case ENUM:
        case INTERFACE:
        case ANNOTATION_TYPE:
            out.printf("<tr class=\"class\"><td class=\"name\">%s</td>",
                       outCtxt.escape(this
                           .getContent("quality.containing-class")));
            out.print(" <td class=\"value\">");
            this.writeElementReference(out::append,
                                       outCtxt,
                                       typeContainer,
                                       LinkContent.NORMAL);
            out.print("<br/>");
            this.writeSummary(out::append, outCtxt, typeContainer);
            out.print("</td></tr>\n");
            break;

        default:
            break;
        }

        out.printf("</table>\n");
    }

    /**
     * Write a hypertext reference to an element into a string.
     * 
     * @param inCtxt the context for resolving source
     * 
     * @param outCtxt the context for writing content
     * 
     * @param refed the element to link to
     * 
     * @param detail the content of the link
     * 
     * @return the generated hypertext
     */
    public String getElementReference(SourceContext inCtxt,
                                      OutputContext outCtxt, Element refed,
                                      LinkContent detail) {
        StringBuilder result = new StringBuilder();
        writeElementReference(result::append, outCtxt, refed, detail);
        return result.toString();
    }

    /**
     * Write a user-defined section searching from a specific member out
     * to the overview.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the context for writing content
     * 
     * @param label the label to use in the CSS class
     * 
     * @param tagName the Javadoc block tag from which to get the
     * content
     * 
     * @param elem the element to start searching for the content from
     * 
     * @return {@code true} if some content was eventually found
     */
    public boolean writeUserSection(PrintWriter out, OutputContext outCtxt,
                                    String label, String tagName,
                                    Element elem) {
        for (DocWithContext dwc : config.docChain(elem)) {
            for (UnknownBlockTagTree ub : DocUtils
                .getUnknownBlockTags(dwc.doc, tagName)) {
                out.printf("<div class=\"javadoc-%s\">\n",
                           outCtxt.escapeAttribute(label));
                toHypertext(out::append, dwc.ctxt, outCtxt, ub.getContent());
                out.print("</div>\n");
                return true;
            }
        }
        return false;
    }

    /**
     * Write a reference section to output <code>&#64;see</code>
     * content.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the context for writing content
     * 
     * @param inCtxt the source context for the provided comment
     * 
     * @param doc the source documentation whose <code>&#64;see</code>
     * tags are to be displayed
     */
    public void writeSeeSection(PrintWriter out, SourceContext inCtxt,
                                OutputContext outCtxt, DocCommentTree doc) {
        List<SeeTree> seeTags = new ArrayList<>();
        DocUtils.getSeeTags(doc).forEach(seeTags::add);
        if (seeTags.isEmpty()) return;
        out.print("<div class=\"javadoc-see-also\">\n");
        out.printf("<h2><span>%s</span></h2>\n",
                   outCtxt.escape(getContent("section.heading.see-also")));
        out.print("<ul>\n");
        for (SeeTree tag : seeTags) {
            List<? extends DocTree> parts = tag.getReference();
            if (parts.isEmpty() ||
                parts.get(0).getKind() != DocTree.Kind.REFERENCE) {
                out.print("<li class=\"plain\">");
                toHypertext(out::append, inCtxt, outCtxt, parts);
                out.print("</li>\n");
            } else {
                String sig = ((ReferenceTree) parts.get(0)).getSignature();
                Element elem = config.resolveSignature(inCtxt.element(), sig);
                out.printf("<li class=\"javadoc%s\">",
                           elem == null ? " failed" : "");
                if (elem == null)
                    toHypertext(out::append,
                                inCtxt,
                                outCtxt,
                                parts.subList(1, parts.size()));
                else
                    writeElementReference(out::append,
                                          outCtxt,
                                          elem,
                                          LinkContent
                                              .forLabel(inCtxt,
                                                        parts
                                                            .subList(1,
                                                                     parts
                                                                         .size())));
                out.print("</li>\n");
            }
        }
        out.print("</ul>\n");
        out.print("</div>\n");
    }

    /**
     * Write a contributor section to output <code>&#64;author</code>
     * content.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the context for writing content
     * 
     * @param inCtxt the source context for the provided comment
     * 
     * @param doc the source documentation whose
     * <code>&#64;author</code> tags are to be displayed
     */
    public void writeAuthorSection(PrintWriter out, SourceContext inCtxt,
                                   OutputContext outCtxt, DocCommentTree doc) {
        List<AuthorTree> authors = new ArrayList<>();
        DocUtils.getAuthorTags(doc).forEach(authors::add);
        if (authors.isEmpty()) return;

        /* Work out what to do with each author. */
        List<Author> referencedAuthors = new ArrayList<>();
        List<Author> hereAuthors = new ArrayList<>();
        List<AuthorTree> unknownAuthors = new ArrayList<>();
        for (AuthorTree tag : authors) {
            String authorKey =
                toHypertext(inCtxt,
                            OutputContext.plain(config.types,
                                                outCtxt.location(),
                                                outCtxt.element()),
                            resolveTextTags(tag.getName()));
            Author author = config.authors.get(authorKey);
            if (config.referencedAuthors.contains(authorKey)) {
                assert author != null;
                referencedAuthors.add(author);
                continue;
            }
            if (author != null) {
                hereAuthors.add(author);
                continue;
            }
            unknownAuthors.add(tag);
        }

        out.print("<div class=\"javadoc-authors\">\n");
        out.printf("<h2><span>%s</span></h2>\n",
                   outCtxt.escape(getContent("section.heading.authors")));
        if (!referencedAuthors.isEmpty() || !unknownAuthors.isEmpty()) {
            out.print("<ul class=\"authors\">\n");

            URI overviewLoc = config.locateOverview();
            for (Author author : referencedAuthors) {
                URI authorLoc =
                    overviewLoc.resolve("#javadoc-author-" + author.id);
                URI relLoc = Utils.relativize(outCtxt.location(), authorLoc);
                out.printf("<li class=\"ref\"><a href=\"%s\">",
                           outCtxt.escapeAttribute(relLoc.toASCIIString()));
                toHypertext(out::append,
                            SourceContext.EMPTY,
                            outCtxt.inLink(),
                            author.name);
                out.print("</a></li>\n");
            }
            for (AuthorTree tag : unknownAuthors) {
                out.print("<li class=\"unknown\">");
                toHypertext(out::append, inCtxt, outCtxt, tag.getName());
                out.print("</li>\n");
            }
            out.print("</ul>\n");
        }
        if (!hereAuthors.isEmpty()) {
            out.print("<div class=\"definitions\">\n");
            for (Author author : hereAuthors)
                writeAuthor(out, inCtxt, outCtxt, author);
            out.printf("</div>\n");
        }
        out.print("</div>\n");
    }

    /**
     * Check that the element is a non-static method.
     * 
     * @param execElem the element to check
     * 
     * @param silent {@code true} if a value is to be returned, instead
     * of throwing an exception
     * 
     * @return {@code true} if the element is an instance method
     * 
     * @throws IllegalArgumentException if the element is not an
     * instance method
     */
    private boolean ensureInstanceMethod(Element execElem, boolean silent) {
        switch (execElem.getKind()) {
        case METHOD:
            if (!execElem.getModifiers().contains(Modifier.STATIC)) return true;
            // Fall through.
        default:
            if (silent) return false;
            throw new IllegalArgumentException("context is not an"
                + " instance method: " + execElem);
        }
    }

    /**
     * Find <code>&#64;param</code> documentation in supertypes of the
     * current context, and write it out.
     * 
     * @param out the destination for content
     * 
     * @param execElem the method whose parameter documentation is
     * required
     * 
     * @param pos the position of the parameter whose documentation is
     * required
     * 
     * @param outCtxt the output context
     * 
     * @param silent {@code true} to prevent an exception from being
     * thrown
     * 
     * @return {@code true} if documentation was found
     * 
     * @throws IllegalArgumentException if the source context cannot
     * inherit documentation
     */
    public boolean writeInheritedParamDocs(Consumer<? super CharSequence> out,
                                           ExecutableElement execElem, int pos,
                                           OutputContext outCtxt,
                                           boolean silent) {
        /* Only return tags in methods can use {@inheritDoc}. */
        if (!ensureInstanceMethod(execElem, silent)) return false;

        // final Collection<TypeElement> candTypes = new
        // LinkedHashSet<>();
        final TypeElement execType =
            (TypeElement) execElem.getEnclosingElement();
        final Collection<TypeElement> candTypes =
            config.getInheritanceOrder(execType);
        // config.addSupertypes(candTypes, execType);

        for (TypeElement cand : candTypes) {
            /* Find all methods of this class that we override. */
            for (ExecutableElement membCand : ElementFilter
                .methodsIn(cand.getEnclosedElements())) {
                if (!config.elements.overrides(execElem, membCand, execType))
                    continue;
                DocCommentTree dct =
                    config.docTrees.getDocCommentTree(membCand);
                if (dct == null) continue;
                ParamTree from = config.getParameterDoc(membCand, pos);
                /* ParamTree from = DocUtils .findParameter(dct,
                 * node.getName().getName(), node.isTypeParameter()); */
                if (from == null) continue;
                List<? extends DocTree> docs = from.getDescription();
                if (docs == null) continue;
                if (docs.isEmpty()) continue;
                SourceContext newCtxt =
                    SourceContext.forElement(membCand).inParamTag(pos);
                toHypertext(out, newCtxt, outCtxt, docs);
                return true;
            }
        }
        return false;
    }

    /**
     * Find <code>&#64;throws</code> documentation in supertypes of the
     * current context, and write it out.
     * 
     * @param out the destination for content
     * 
     * @param execElem the method whose exception documentation is
     * required
     * 
     * @param thrown the type whose documentation of throwing from the
     * method is required
     * 
     * @param outCtxt the output context
     * 
     * @param silent {@code true} to prevent an exception from being
     * thrown
     * 
     * @return {@code true} if documentation was found
     * 
     * @throws IllegalArgumentException if the source context cannot
     * inherit documentation
     */
    public boolean
        writeInheritedThrowsDocs(Consumer<? super CharSequence> out,
                                 ExecutableElement execElem, TypeMirror thrown,
                                 OutputContext outCtxt, boolean silent) {
        /* Only @throws tags in instance methods can use
         * {@inheritDoc}. */
        if (!ensureInstanceMethod(execElem, silent)) return false;

        // final Collection<TypeElement> candTypes = new
        // LinkedHashSet<>();
        final TypeElement execType =
            (TypeElement) execElem.getEnclosingElement();
        final Collection<TypeElement> candTypes =
            config.getInheritanceOrder(execType);
        // config.addSupertypes(candTypes, execType);

        for (TypeElement cand : candTypes) {
            /* Find all methods of this class that we override. */
            for (ExecutableElement membCand : ElementFilter
                .methodsIn(cand.getEnclosedElements())) {
                if (!config.elements.overrides(execElem, membCand, execType))
                    continue;
                DocCommentTree dct =
                    config.docTrees.getDocCommentTree(membCand);
                if (dct == null) continue;
                ThrowsTree from = config.getThrowsDoc(membCand, thrown);
                /* ThrowsTree from = config.findThrows(execElem, dct,
                 * thrownType); */
                if (from == null) continue;
                List<? extends DocTree> docs = from.getDescription();
                if (docs == null) continue;
                if (docs.isEmpty()) continue;
                SourceContext newCtxt =
                    SourceContext.forElement(membCand).inThrowsTag(thrown);
                toHypertext(out, newCtxt, outCtxt, docs);
                return true;
            }
        }
        return false;
    }

    /**
     * Find <code>&#64;return</code> documentation in supertypes of the
     * current context, and write it out.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the output context
     * 
     * @param execElem the method whose return documentation is required
     * 
     * @param silent {@code true} to prevent an exception from being
     * thrown
     * 
     * @return {@code true} if documentation was found
     * 
     * @throws IllegalArgumentException if the source context cannot
     * inherit documentation
     */
    public boolean writeInheritedReturnDocs(Consumer<? super CharSequence> out,
                                            ExecutableElement execElem,
                                            OutputContext outCtxt,
                                            boolean silent) {
        /* Only return tags in methods can use {@inheritDoc}. */
        if (!ensureInstanceMethod(execElem, silent)) return false;

        // final Collection<TypeElement> candTypes = new
        // LinkedHashSet<>();
        final TypeElement execType =
            (TypeElement) execElem.getEnclosingElement();
        final Collection<TypeElement> candTypes =
            config.getInheritanceOrder(execType);
        // config.addSupertypes(candTypes, execType);

        for (TypeElement cand : candTypes) {
            /* Find all methods of this class that we override. */
            for (ExecutableElement membCand : ElementFilter
                .methodsIn(cand.getEnclosedElements())) {
                if (!config.elements.overrides(execElem, membCand, execType))
                    continue;
                DocCommentTree dct =
                    config.docTrees.getDocCommentTree(membCand);
                if (dct == null) continue;
                ReturnTree from = DocUtils.getReturnTag(dct);
                if (from == null) continue;
                List<? extends DocTree> docs = from.getDescription();
                if (docs == null) continue;
                if (docs.isEmpty()) continue;
                SourceContext newCtxt =
                    SourceContext.forElement(membCand).inReturnTag();
                toHypertext(out, newCtxt, outCtxt, docs);
                return true;
            }
        }
        return false;
    }

    private static OutputStream tee(OutputStream out, OutputStream copy) {
        return new FilterOutputStream(out) {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
                copy.write(b);
            }

            @Override
            public void close() throws IOException {
                out.close();
                copy.close();
            }

            @Override
            public void flush() throws IOException {
                out.flush();
                copy.flush();
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
                copy.write(b, off, len);
            }
        };
    }

    private void convertCharset(Executor executor, InputStream bytesIn,
                                Charset charsetIn, OutputStream bytesOut,
                                Charset charsetOut) {
        executor.execute(() -> {
            try (Reader in = new InputStreamReader(bytesIn, charsetIn);
                 Writer out = new OutputStreamWriter(bytesOut, charsetOut)) {
                char[] buf = new char[4096];
                int got;
                while ((got = in.read(buf)) >= 0) {
                    out.write(buf, 0, got);
                }
            } catch (IOException e) {
                /* TODO? */
                throw new UnsupportedOperationException("unimplemented", e);
            }
        });
    }

    /**
     * Open an HTML file for writing. The provided stream writes into a
     * pipeline of conversions, including a potential HTMLTidy process,
     * and a final conversion from UTF-8 to the slice's character
     * encoding. Additionally, an HTML4.01 doctype is pre-written to the
     * stream.
     * 
     * @param file the name of the file
     * 
     * @return a character stream writing to the file
     * 
     * @throws IOException if there was an I/O error
     */
    public PrintWriter openHypertextFile(Path file) throws IOException {
        final Path logDir;
        final String leafname;
        if (config.diagnosticsDirectory != null) {
            logDir = config.diagnosticsDirectory.resolve(config.outputDirectory
                .relativize(file.getParent()).toString());
            String tmpLeafname = file.getParent().relativize(file).toString();
            leafname = tmpLeafname
                .substring(0,
                           tmpLeafname.length()
                               - config.hypertextFileSuffix.length());
            if (false) System.err.printf("HTML: %s%nLog dir: %s%nLeaf: %s%n",
                                         file,
                                         logDir,
                                         leafname);
        } else {
            logDir = null;
            leafname = null;
        }

        /* Describe a pipeline to work on the output. */
        List<ProcessBuilder> steps = new ArrayList<>();
        Path tidyLog = logDir == null ? null :
            logDir.resolve(leafname + spec.suffix + ".tidy.log");
        if (config.tidyProgram == null) {
            /* Add an identity transformation so that the pipeline isn't
             * empty. */
            steps.add(new ProcessBuilder("cat"));
        } else {
            /* Insert an HTMLTidy process. */
            List<String> tidyArgs =
                new ArrayList<>(Arrays.asList(config.tidyProgram,
                                              "-i",
                                              "-q",
                                              "-utf8",
                                              "--wrap",
                                              "0"));
            if (logDir != null) {
                tidyArgs.add("-f");
                tidyArgs.add(tidyLog.toString());
                Files.createDirectories(logDir);
            }
            steps.add(new ProcessBuilder(tidyArgs));
        }

        /* Create the pipeline. */
        steps.forEach(pb -> pb.redirectError(Redirect.INHERIT));
        List<Process> pipeline = ProcessBuilder.startPipeline(steps);

        /* Ensure we wait for the last process to exit. */
        if (config.tidyProgram != null) {
            Process p = pipeline.get(pipeline.size() - 1);
            config.executor.execute(() -> {
                try {
                    int rc = p.waitFor();
                    switch (rc) {
                    case 0:
                        break;

                    case 1:
                        if (tidyLog != null)
                            config.report(Kind.WARNING,
                                          "tidy.warning.see",
                                          tidyLog);
                        else
                            config.report(Kind.WARNING, "tidy.warning");
                        break;

                    default:
                        if (tidyLog != null)
                            config.report(Kind.ERROR,
                                          "tidy.failure.see",
                                          tidyLog);
                        else
                            config.report(Kind.ERROR, "tidy.failure");
                        break;
                    }
                } catch (InterruptedException e) {
                    config.report(Kind.ERROR, "tidy.interrupt", file);
                }
            });
        }

        /* Create a thread to convert the end of the pipeline to the
         * specified charset, and write out to the file. */
        convertCharset(config.executor,
                       pipeline.get(pipeline.size() - 1).getInputStream(),
                       StandardCharsets.UTF_8,
                       Files.newOutputStream(file),
                       spec.charset);

        /* Prepare to write to the pipeline. */
        OutputStream byteOut = pipeline.get(0).getOutputStream();
        if (logDir != null) {
            Files.createDirectories(logDir);
            Path rawFile = logDir
                .resolve(leafname + config.hypertextFileSuffix + spec.suffix);
            byteOut = tee(byteOut, Files.newOutputStream(rawFile));
        }
        PrintWriter out =
            new PrintWriter(new OutputStreamWriter(byteOut,
                                                   StandardCharsets.UTF_8));
        out.printf("<!DOCTYPE html PUBLIC"
            + " \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n");
        return out;
    }

    /**
     * Write a meta-data link to an element.
     * 
     * @param relType the link relation types, space-separated
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the output context
     * 
     * @param refed the element to reference
     */
    public void writeElementMetaLink(String relType,
                                     Consumer<? super CharSequence> out,
                                     OutputContext outCtxt, Element refed) {
        if (refed == null) return;
        if (refed instanceof ModuleElement &&
            ((ModuleElement) refed).isUnnamed()) return;
        if (config.excludedElements.contains(refed)) return;
        final URI location = config.locateElement(refed);
        if (location == null || location.equals(outCtxt.location())) return;
        out.accept("<link rel=\"");
        out.accept(outCtxt.escapeAttribute(relType));
        out.accept("\" title=\"");
        Properties titleProps = new Properties();
        config.setElementProperties(titleProps, refed);
        out.accept(Slice.this.macroFormat("link.title.element",
                                          titleProps,
                                          outCtxt.attributeEscaper()));
        out.accept("\" href=\"");
        outCtxt.writeAttribute(out, location);
        out.accept("\">\n");
    }

    private ElementVisitor<Void, Void>
        elementNamer(Consumer<? super CharSequence> out, OutputContext outCtxt,
                     boolean makeLink, LinkContent detail) {
        final OutputContext inLink = makeLink ? outCtxt.inLink() : outCtxt;
        return new SimpleElementVisitor9<Void, Void>() {
            @Override
            protected Void defaultAction(Element e, Void p) {
                throw new UnsupportedOperationException("unnecessary");
            }

            @Override
            public Void visitModule(ModuleElement e, Void p) {
                if (detail.hasLabel()) {
                    toHypertext(out,
                                detail.sourceContext(),
                                inLink,
                                detail.label());
                } else {
                    writeCamelText(out,
                                   inLink,
                                   e.getQualifiedName().toString());
                }
                return null;
            }

            @Override
            public Void visitPackage(PackageElement e, Void p) {
                if (detail.hasLabel()) {
                    toHypertext(out,
                                detail.sourceContext(),
                                inLink,
                                detail.label());
                } else {
                    writeCamelText(out,
                                   inLink,
                                   e.getQualifiedName().toString());
                }
                return null;
            }

            @Override
            public Void visitType(TypeElement e, Void p) {
                if (detail.hasLabel()) {
                    toHypertext(out,
                                detail.sourceContext(),
                                inLink,
                                detail.label());
                    return null;
                }

                /* If the class's enclosing class is not the one we're
                 * in, write the enclosing class name followed by a
                 * dot. */
                Element execType = e.getEnclosingElement();
                OutputContext encCtxt =
                    e.getModifiers().contains(Modifier.STATIC) ?
                        inLink.forStatic() : inLink;
                switch (execType.getKind()) {
                case PACKAGE:
                    if (detail.isShowingPackage()) {
                        writeElementReference(out,
                                              encCtxt,
                                              execType,
                                              LinkContent.NORMAL);
                        if (inLink.canMarkUpInline())
                            out.accept(HYPERTEXT_WORD_BREAK);
                        out.accept(inLink.escape("."));
                    }
                    break;

                case CLASS:
                case ENUM:
                case INTERFACE:
                case ANNOTATION_TYPE:
                    if (detail.isShowingContainers() || (detail
                        .isShowingNecessaryContainers() &&
                        !config.enclosesOrSame(outCtxt.element(), execType))) {
                        writeElementReference(out,
                                              encCtxt,
                                              execType,
                                              LinkContent.NORMAL);
                        if (inLink.canMarkUpInline())
                            out.accept(HYPERTEXT_WORD_BREAK);
                        out.accept(inLink.escape("."));
                    }
                    break;

                default:
                    throw new AssertionError("unreachable");
                }

                writeCamelText(out, inLink, e.getSimpleName());

                if (!inLink.isStatic()) {
                    /* Write out short type parameters. */
                    List<? extends TypeParameterElement> tps =
                        e.getTypeParameters();
                    if (!tps.isEmpty()) {
                        if (inLink.canMarkUpInline())
                            out.accept(HYPERTEXT_WORD_BREAK);
                        out.accept(inLink.escape("<"));
                        String sep = "";
                        for (TypeParameterElement tp : tps) {
                            out.accept(inLink.escape(sep));
                            sep = ", ";
                            out.accept(inLink.escape(tp.getSimpleName()));
                        }
                        out.accept(inLink.escape(">"));
                    }
                }
                return null;
            }

            @Override
            public Void visitExecutable(ExecutableElement e, Void p) {
                if (detail.hasLabel()) {
                    toHypertext(out,
                                detail.sourceContext(),
                                inLink,
                                detail.label());
                    return null;
                }

                /* If the member's class is not the one we're in, write
                 * the member's class name followed by a dot. */
                Element execType = e.getEnclosingElement();
                OutputContext encCtxt =
                    e.getModifiers().contains(Modifier.STATIC) ?
                        inLink.forStatic() : inLink;
                if (detail.isShowingContainers() ||
                    (detail.isShowingNecessaryContainers() &&
                        !config.enclosesOrSame(outCtxt.element(), execType))) {
                    writeElementReference(out,
                                          encCtxt,
                                          execType,
                                          LinkContent.NORMAL);
                    if (inLink.canMarkUpInline())
                        out.accept(HYPERTEXT_WORD_BREAK);
                    out.accept(inLink.escape("."));
                }

                if (e.getKind() == ElementKind.CONSTRUCTOR) {
                    /* Write the constructor's class's name. */
                    writeCamelText(out,
                                   inLink,
                                   e.getEnclosingElement().getSimpleName());
                } else {
                    /* Write the method's name. */
                    writeCamelText(out, inLink, e.getSimpleName());
                }

                if (detail.isShowingParameters()) {
                    /* Write out the parameters. */
                    out.accept(inLink.escape("("));
                    String sep = "";
                    List<? extends VariableElement> params = e.getParameters();
                    final int plen = params.size();
                    final int vararg = e.isVarArgs() ? plen - 1 : plen;
                    for (int i = 0; i < plen; i++) {
                        VariableElement ve = params.get(i);
                        out.accept(inLink.escape(sep));
                        sep = ", ";

                        if (i == vararg) {
                            writeTypeReference(out,
                                               inLink,
                                               ((ArrayType) ve.asType())
                                                   .getComponentType(),
                                               LinkContent.NORMAL);
                            out.accept(inLink.escape("..."));
                        } else {
                            writeTypeReference(out,
                                               inLink,
                                               ve.asType(),
                                               LinkContent.NORMAL);
                        }
                    }
                    out.accept(inLink.escape(")"));
                }

                return null;
            }

            @Override
            public Void visitVariable(VariableElement e, Void p) {
                if (detail.hasLabel()) {
                    toHypertext(out,
                                detail.sourceContext(),
                                inLink,
                                detail.label());
                    return null;
                }

                Element execType = e.getEnclosingElement();
                OutputContext encCtxt =
                    e.getModifiers().contains(Modifier.STATIC) ?
                        inLink.forStatic() : inLink;

                /* If the member's class is not the one we're in, write
                 * the member's class name followed by a dot. */
                if (detail.isShowingContainers() ||
                    (detail.isShowingNecessaryContainers() &&
                        !config.enclosesOrSame(outCtxt.element(), execType))) {
                    writeElementReference(out,
                                          encCtxt,
                                          execType,
                                          LinkContent.NORMAL);
                    if (outCtxt.canMarkUpInline())
                        out.accept(HYPERTEXT_WORD_BREAK);
                    out.accept(".");
                }

                /* Write the field's name. */
                writeCamelText(out, inLink, e.getSimpleName());

                return null;
            }

            @Override
            public Void visitTypeParameter(TypeParameterElement e, Void p) {
                if (detail.hasLabel()) {
                    toHypertext(out,
                                detail.sourceContext(),
                                inLink,
                                detail.label());
                    return null;
                }

                out.accept(outCtxt.escape(e.getSimpleName()));
                List<? extends TypeMirror> bounds = e.getBounds();
                String sep = " extends ";
                for (TypeMirror tm : bounds) {
                    if (config.types.isSameType(tm, config.javaLangObject))
                        continue;
                    out.accept(outCtxt.escape(sep));
                    sep = " & ";
                    writeTypeReference(out, inLink, tm, LinkContent.NORMAL);
                }

                return null;
            }
        };
    }

    /**
     * Write a hypertext link to an element.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the output context
     * 
     * @param refed the element to reference
     * 
     * @param origDetail what to write as the content of the link
     */
    public void writeElementReference(Consumer<? super CharSequence> out,
                                      OutputContext outCtxt, Element refed,
                                      LinkContent origDetail) {
        if (origDetail == null) throw new NullPointerException("detail");

        final OutputContext enclosedOutCtxt;
        final boolean putInCodeBlock =
            outCtxt.canMarkAsCode() && outCtxt.canMarkUpInline();
        if (putInCodeBlock) {
            out.accept("<code class=\"java\">");
            enclosedOutCtxt = outCtxt.inCode();
        } else {
            enclosedOutCtxt = outCtxt;
        }

        URI location = config.locateElement(refed);
        final LinkContent detail =
            location == null ? origDetail.withPackageIfNoLabel() : origDetail;
        final boolean makeWrap = enclosedOutCtxt.canMarkUpInline();
        final boolean redundantLink =
            location != null && location.equals(outCtxt.location());
        final boolean makeLink = makeWrap && location != null &&
            !redundantLink && enclosedOutCtxt.canLink();
        final String wrapper = makeLink ? "a" : "span";

        if (makeWrap) {
            out.accept("<" + wrapper + " class=\"javadoc ");
            if (redundantLink) out.accept("redundant-link ");
            out.accept(enclosedOutCtxt
                .escapeAttribute(DocUtils.getStyleClass(refed)));
            DocUtils.writeModifierStyleClasses(out,
                                               enclosedOutCtxt.inAttribute(),
                                               refed.getModifiers());
            out.accept("\" title=\"");
            Properties titleProps = new Properties();
            config.setElementProperties(titleProps, refed);
            out.accept(Slice.this
                .macroFormat("link.title.element",
                             titleProps,
                             enclosedOutCtxt.attributeEscaper()));
            if (makeLink) {
                out.accept("\"");
                out.accept(" href=\"");
                enclosedOutCtxt.writeAttribute(out, location);
            }
            out.accept("\">");
        }

        refed.accept(elementNamer(out, enclosedOutCtxt, makeLink, detail),
                     null);

        if (makeWrap) {
            out.accept("</" + wrapper + ">");
        }

        if (putInCodeBlock) out.accept("</code>");
    }

    /**
     * Write a reference to another type.
     * 
     * @param out the destination for the generated content
     * 
     * @param outCtxt a context for resolving destination-related
     * references
     * 
     * @param type the type to be expressed
     * 
     * @param label alternative text to be used instead of a textual
     * expression of the type
     */
    public void writeTypeReference(Consumer<? super CharSequence> out,
                                   OutputContext outCtxt, TypeMirror type,
                                   LinkContent detail) {
        if (detail == null) throw new NullPointerException("detail");
        // System.err.printf("writing type reference for %s%n", type);
        final OutputContext enclosedOutCtxt;
        final boolean putInCodeBlock =
            outCtxt.canMarkAsCode() && outCtxt.canMarkUpInline();
        if (putInCodeBlock) {
            out.accept("<code class=\"java\">");
            enclosedOutCtxt = outCtxt.inCode();
        } else {
            enclosedOutCtxt = outCtxt;
        }

        type.accept(new SimpleTypeVisitor9<Void, Void>() {
            @Override
            protected Void defaultAction(TypeMirror e, Void p) {
                throw new UnsupportedOperationException("unnecessary");
            }

            @Override
            public Void visitPrimitive(PrimitiveType t, Void p) {
                if (detail.hasLabel()) {
                    toHypertext(out,
                                detail.sourceContext(),
                                enclosedOutCtxt,
                                detail.label());
                } else {
                    out.accept(enclosedOutCtxt.escape(DocUtils.getLongName(t)));
                }
                return null;
            }

            @Override
            public Void visitArray(ArrayType t, Void p) {
                if (detail.hasLabel()) {
                    toHypertext(out,
                                detail.sourceContext(),
                                enclosedOutCtxt,
                                detail.label());
                } else {
                    writeTypeReference(out,
                                       enclosedOutCtxt,
                                       t.getComponentType(),
                                       LinkContent.NORMAL);
                    out.accept(enclosedOutCtxt.escape("[]"));
                }
                return null;
            }

            @Override
            public Void visitDeclared(DeclaredType t, Void p) {
                TypeElement elem = (TypeElement) t.asElement();
                URI elemLoc = config.locateElement(elem);
                if (elemLoc == null) {
                    if (detail.hasLabel())
                        config.report(Kind.WARNING,
                                      detail.sourceContext().element(),
                                      "output.type-reference.failure.missing",
                                      elem);
                    else
                        config.report(Kind.WARNING,
                                      "output.type-reference.failure.missing",
                                      elem);
                }
                final boolean makeWrap = enclosedOutCtxt.canMarkUpInline();
                final boolean redundantLink = elemLoc != null &&
                    elemLoc.equals(enclosedOutCtxt.location());
                final boolean makeLink = makeWrap && elemLoc != null &&
                    !redundantLink && enclosedOutCtxt.canLink();
                final String wrapper = makeLink ? "a" : "span";
                final OutputContext inLink =
                    makeLink ? enclosedOutCtxt.inLink() : enclosedOutCtxt;

                if (makeWrap) {
                    out.accept("<" + wrapper + " class=\"javadoc ");
                    if (redundantLink) out.accept("redundant-link ");
                    out.accept(enclosedOutCtxt
                        .escapeAttribute(DocUtils.getStyleClass(elem)));
                    DocUtils.writeModifierStyleClasses(out,
                                                       enclosedOutCtxt
                                                           .inAttribute(),
                                                       elem.getModifiers());
                    out.accept("\" title=\"");
                    Properties titleProps = new Properties();
                    config.setElementProperties(titleProps, elem);
                    out.accept(Slice.this
                        .macroFormat("link.title.element",
                                     titleProps,
                                     enclosedOutCtxt.attributeEscaper()));
                    if (makeLink) {
                        out.accept("\"");
                        out.accept(" href=\"");
                        enclosedOutCtxt.writeAttribute(out, elemLoc);
                    }
                    out.accept("\">");
                }

                if (detail.hasLabel()) {
                    toHypertext(out,
                                detail.sourceContext(),
                                inLink,
                                detail.label());
                } else {
                    if (detail.isShowingPackage() || elemLoc == null) {
                        /* Since we can't provide a link, or we have
                         * been specifically asked to, print the full
                         * package too. */
                        writeElementReference(out,
                                              inLink,
                                              config.elements
                                                  .getPackageOf(t.asElement()),
                                              LinkContent.NORMAL);
                        if (inLink.canMarkUpInline())
                            out.accept(HYPERTEXT_WORD_BREAK);
                        out.accept(".");
                    }

                    TypeMirror encloser = t.getEnclosingType();
                    if (encloser.getKind() != TypeKind.NONE) {
                        writeTypeReference(out,
                                           inLink,
                                           encloser,
                                           LinkContent.NORMAL);
                        if (inLink.canMarkUpInline())
                            out.accept(HYPERTEXT_WORD_BREAK);
                        out.accept(".");
                    }
                    writeCamelText(out, inLink, elem.getSimpleName());
                }
                if (makeWrap) {
                    out.accept("</" + wrapper + ">");
                }

                if (!detail.hasLabel()) {
                    /* Describe type arguments. TODO: Allow caller to
                     * disable them, or reduce detail. */
                    List<? extends TypeMirror> tas = t.getTypeArguments();
                    if (tas.isEmpty()) return null;
                    if (enclosedOutCtxt.canMarkUpInline())
                        out.accept(HYPERTEXT_WORD_BREAK);
                    out.accept(enclosedOutCtxt.escape("<"));
                    String sep = "";
                    for (TypeMirror ta : tas) {
                        out.accept(enclosedOutCtxt.escape(sep));
                        sep = ", ";
                        writeTypeReference(out,
                                           enclosedOutCtxt,
                                           ta,
                                           LinkContent.NORMAL);
                    }
                    out.accept(enclosedOutCtxt.escape(">"));
                }

                return null;
            }

            @Override
            public Void visitIntersection(IntersectionType t, Void p) {
                if (detail.hasLabel()) {
                    toHypertext(out,
                                detail.sourceContext(),
                                enclosedOutCtxt,
                                detail.label());
                } else {
                    String sep = "";
                    for (TypeMirror b : t.getBounds()) {
                        out.accept(enclosedOutCtxt.escape(sep));
                        sep = " | ";
                        writeTypeReference(out,
                                           enclosedOutCtxt,
                                           b,
                                           LinkContent.NORMAL);
                    }
                }
                return null;
            }

            @Override
            public Void visitTypeVariable(TypeVariable t, Void p) {
                if (detail.hasLabel()) {
                    toHypertext(out,
                                detail.sourceContext(),
                                enclosedOutCtxt,
                                detail.label());
                } else {
                    TypeParameterElement tpe =
                        (TypeParameterElement) t.asElement();
                    Element tpeCont = tpe.getEnclosingElement();
                    URI elemLoc = config.locateElement(tpeCont);
                    final boolean redundantLink = elemLoc != null &&
                        elemLoc.equals(enclosedOutCtxt.location());
                    final boolean makeWrap = enclosedOutCtxt.canMarkUpInline();
                    final boolean makeLink = makeWrap && elemLoc != null &&
                        !redundantLink && enclosedOutCtxt.canLink();
                    final String wrapper = makeLink ? "a" : "span";
                    @SuppressWarnings("unused")
                    final OutputContext inLink =
                        makeLink ? enclosedOutCtxt.inLink() : enclosedOutCtxt;

                    if (makeWrap) {
                        out.accept("<" + wrapper + " class=\"javadoc ");
                        if (redundantLink) out.accept("redundant-link ");
                        out.accept(enclosedOutCtxt
                            .escapeAttribute(DocUtils.getStyleClass(tpeCont)));
                        DocUtils
                            .writeModifierStyleClasses(out,
                                                       enclosedOutCtxt
                                                           .inAttribute(),
                                                       tpeCont.getModifiers());
                        out.accept("\" title=\"");
                        Properties titleProps = new Properties();
                        config.setElementProperties(titleProps, tpeCont);
                        out.accept(Slice.this
                            .macroFormat("link.title.element",
                                         titleProps,
                                         enclosedOutCtxt.attributeEscaper()));
                        if (makeLink) {
                            out.accept("\" href=\"");
                            enclosedOutCtxt.writeAttribute(out, elemLoc);
                        }
                        out.accept("\">");
                    }

                    Slice.writeCamelText(out,
                                         enclosedOutCtxt,
                                         tpe.getSimpleName());
                    // out.accept(enclosedOutCtxt.escape(t.toString()));

                    if (makeWrap) {
                        out.accept("</" + wrapper + ">");
                    }

                    if (false && !enclosedOutCtxt.isAccountedFor(t)) {
                        OutputContext tvCtxt = enclosedOutCtxt.accountFor(t);

                        /* TODO: What is meant by these bounds? Are some
                         * of them just mirrors of the bounds on the
                         * corresponding TypeParameterElement? */
                        TypeMirror lb = t.getLowerBound();
                        assert lb != null;
                        if (lb.getKind() != TypeKind.NULL) {
                            out.accept(tvCtxt.escape(" super "));
                            writeTypeReference(out,
                                               tvCtxt,
                                               lb,
                                               LinkContent.NORMAL);
                        }

                        TypeMirror ub = t.getUpperBound();
                        if (!config.types.isSameType(ub,
                                                     config.javaLangObject)) {
                            out.accept(tvCtxt.escape(" extends "));
                            writeTypeReference(out,
                                               tvCtxt,
                                               ub,
                                               LinkContent.NORMAL);
                        }
                    }
                }
                return null;
            }

            @Override
            public Void visitWildcard(WildcardType t, Void p) {
                if (detail.hasLabel()) {
                    toHypertext(out,
                                detail.sourceContext(),
                                enclosedOutCtxt,
                                detail.label());
                } else {
                    out.accept(enclosedOutCtxt.escape("?"));

                    TypeMirror lb = t.getSuperBound();
                    if (lb != null) {
                        out.accept(enclosedOutCtxt.escape(" super "));
                        writeTypeReference(out,
                                           enclosedOutCtxt,
                                           lb,
                                           LinkContent.NORMAL);
                    }

                    TypeMirror ub = t.getExtendsBound();
                    if (ub != null) {
                        out.accept(enclosedOutCtxt.escape(" extends "));
                        writeTypeReference(out,
                                           enclosedOutCtxt,
                                           ub,
                                           LinkContent.NORMAL);
                    }
                }
                return null;
            }

            @Override
            public Void visitNoType(NoType t, Void p) {
                if (detail.hasLabel()) {
                    toHypertext(out,
                                detail.sourceContext(),
                                enclosedOutCtxt,
                                detail.label());
                } else {
                    out.accept(enclosedOutCtxt.escape(t.toString()));
                }
                return null;
            }
        }, null);

        if (putInCodeBlock) out.accept("</code>");
    }

    /**
     * Interpret localized content as a Javadoc comment.
     * 
     * @param key the key identifying the content
     * 
     * @return a sequence of documentation trees representing the
     * content
     */
    public final List<? extends DocTree> getTreeContent(String key) {
        String rawText = "<body>" + contentBundle.getString(key) + "</body>";
        FileObject file = new FileObject() {
            @Override
            public URI toUri() {
                throw new UnsupportedOperationException("unnecessary");
            }

            @Override
            public Writer openWriter() throws IOException {
                throw new UnsupportedOperationException("unnecessary");
            }

            @Override
            public Reader openReader(boolean ignoreEncodingErrors)
                throws IOException {
                return new StringReader(rawText);
            }

            @Override
            public OutputStream openOutputStream() throws IOException {
                throw new UnsupportedOperationException("unnecessary");
            }

            @Override
            public InputStream openInputStream() throws IOException {
                throw new UnsupportedOperationException("unnecessary");
            }

            @Override
            public String getName() {
                return "content-" + key + ".html";
            }

            @Override
            public long getLastModified() {
                throw new UnsupportedOperationException("unnecessary");
            }

            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors)
                throws IOException {
                return rawText;
            }

            @Override
            public boolean delete() {
                throw new UnsupportedOperationException("unnecessary");
            }
        };
        DocCommentTree tree = config.docTrees.getDocCommentTree(file);
        return tree.getFullBody();
    }

    /**
     * Write a summary for an element into an HTML meta-data
     * description. Nothing is written if no summary is found.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the context for determining the level of detail of
     * the content
     * 
     * @param elem the element to be summarized
     * 
     * @return {@code true} if a summary was found
     */
    public boolean
        writeSummaryAsHypertextDescription(Consumer<? super CharSequence> out,
                                           OutputContext outCtxt,
                                           Element elem) {
        return writeSummary(out,
                            outCtxt.inAttribute(),
                            elem,
                            () -> out.accept("<meta name=\"description\""
                                + " content=\""),
                            () -> out.accept("\">\n"));
    }

    /**
     * Write basic meta-data in the <code>&lt;head&gt;</code> element of
     * an HTML page, including a link to the start page.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the context for determining the level of detail of
     * the content
     */
    public void writeHypertextMeta(PrintWriter out, OutputContext outCtxt) {
        writeHypertextMeta(out, outCtxt, true);
    }

    /**
     * Write basic meta-data in the <code>&lt;head&gt;</code> element of
     * an HTML page.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the context for determining the level of detail of
     * the content
     * 
     * @param withStart whether to include a link to the start page
     */
    public void writeHypertextMeta(PrintWriter out, OutputContext outCtxt,
                                   boolean withStart) {
        out.printf("<meta name=\"generator\" lang=\"en\""
            + " content=\"Polydoclot %s r%s\">\n",
                   Configuration.buildProperties.get("rcs"),
                   Configuration.buildProperties.get("revision"));
        out.printf("<meta http-equiv=\"Content-Type\""
            + " content=\"text/html; charset=%s\">\n",
                   outCtxt.escapeAttribute(spec.charset.toString()));
        out.printf("<meta name=\"schema.ssm\" content=\"%s\">\n",
                   "http://standard-sitemap.org/2007/ns");
        out.printf("<link rel=\"ssm.location\" href=\"%s\">\n",
                   Utils.relativize(outCtxt.location(),
                                    URI.create("standard-sitemap.xml")));
        if (withStart)
            out.printf("<link rel=\"start\" href=\"%s\">\n",
                       Utils.relativize(outCtxt.location(),
                                        URI.create("overview-summary"
                                            + config.hypertextLinkSuffix)));
    }

    /**
     * Embed the caller's name in hypertext output for diagnostic
     * purposes.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the context for determining the level of detail of
     * the content
     */
    public void sourceLog(Consumer<? super CharSequence> out,
                          OutputContext outCtxt) {
        if (!outCtxt.canMarkUpInline()) return;
        StackWalker walker =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        StackFrame frame = walker.walk(f -> f.skip(1).findFirst()).get();
        String message =
            String.format("%s %d", frame.getFileName(), frame.getLineNumber());
        out.accept("<!-- ");
        out.accept(outCtxt.escape(message));
        out.accept(" -->");
    }

    /**
     * Write a summary for an element by looking for a
     * <code>@resume</code> tag, or defaulting to the first sentence.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the context for determining the level of detail of
     * the content
     * 
     * @param elem the element to be summarized
     * 
     * @return {@code true} if a summary was found
     */
    public boolean writeSummary(Consumer<? super CharSequence> out,
                                OutputContext outCtxt, Element elem) {
        return writeSummary(out, outCtxt, elem, null, null);
    }

    private boolean writeDirectSummary(Consumer<? super CharSequence> out,
                                       OutputContext outCtxt, Element elem,
                                       Runnable beforeOkay,
                                       Runnable afterOkay) {
        assert elem != null;
        DocCommentTree all = config.docTrees.getDocCommentTree(elem);
        if (all == null) return false;

        /* Find a @resume tag. */
        for (UnknownBlockTagTree summaryTag : DocUtils
            .getUnknownBlockTags(all, "resume", "summary")) {
            List<? extends DocTree> content = summaryTag.getContent();
            if (content == null || content.isEmpty()) continue;
            SourceContext tagCtxt =
                SourceContext.forElement(elem).inSummaryTag();
            if (beforeOkay != null) beforeOkay.run();
            toHypertext(out, tagCtxt, outCtxt, content);
            if (afterOkay != null) afterOkay.run();
            return true;
        }

        /* Use the first sentence. */
        List<? extends DocTree> fs = all.getFirstSentence();
        if (fs == null) return false;
        if (fs.isEmpty()) return false;
        if (beforeOkay != null) beforeOkay.run();
        toHypertext(out,
                    SourceContext.forElement(elem).inFirstSentence(),
                    outCtxt,
                    fs);
        if (afterOkay != null) afterOkay.run();
        return true;
    }

    /**
     * Write an encapsulated summary for an element by looking for a
     * <code>&#64;resume</code> tag, or defaulting to the first
     * sentence.
     * 
     * @param out the destination for content
     * 
     * @param outCtxt the context for determining the level of detail of
     * the content
     * 
     * @param elem the element to be summarized
     * 
     * @param beforeOkay an action to perform before writing the summary
     * if one is found
     * 
     * @param afterOkay an action to perform after writing the summary
     * if one is found
     * 
     * @return {@code true} if a summary was found
     */
    public boolean writeSummary(Consumer<? super CharSequence> out,
                                OutputContext outCtxt, Element elem,
                                Runnable beforeOkay, Runnable afterOkay) {
        if (elem == null) return false;

        /* Try to get the summary directly. */
        if (writeDirectSummary(out, outCtxt, elem, beforeOkay, afterOkay))
            return true;

        /* Inherit. We need to know whether we're looking at an instance
         * method or its class. Anything else can't inherit. */
        final TypeElement typeElem;
        final ExecutableElement execElem;
        switch (elem.getKind()) {
        case INTERFACE:
        case CLASS:
        case ENUM:
        case ANNOTATION_TYPE:
            /* If the element is class, investigate base classes and
             * interfaces. */
            typeElem = (TypeElement) elem;
            execElem = null;
            break;

        case METHOD:
            /* If the element is an instance method, investigate
             * overridden methods. */
            if (elem.getModifiers().contains(Modifier.STATIC)) return false;
            execElem = (ExecutableElement) elem;
            typeElem = (TypeElement) execElem.getEnclosingElement();
            break;

        default:
            /* Other element kinds can't inherit. */
            return false;
        }

        /* Get all superclasses to try. */
        final Collection<TypeElement> cands =
            config.getInheritanceOrder(typeElem);
        // List<TypeElement> cands = new ArrayList<>();
        // config.addSupertypes(cands, typeElem);

        if (execElem != null) {
            /* We are searching for overridden methods. Scan each
             * superclass for an overridden method. */
            for (TypeElement candType : cands) {
                assert candType != null;
                for (ExecutableElement candMeth : ElementFilter
                    .methodsIn(candType.getEnclosedElements())) {
                    if (!config.elements
                        .overrides(execElem, candMeth, typeElem)) continue;
                    if (writeDirectSummary(out,
                                           outCtxt,
                                           candMeth,
                                           beforeOkay,
                                           afterOkay))
                        return true;
                    break;
                }
            }
        } else {
            /* We are searching for a superclass with documentation. */
            for (TypeElement candType : cands) {
                if (writeDirectSummary(out,
                                       outCtxt,
                                       candType,
                                       beforeOkay,
                                       afterOkay))
                    return true;
            }
        }

        return false;
    }

    /**
     * Insert optional line breaks that are aesthetic for class and
     * package names. Breaks are inserted before dots and sequences of
     * underscores, and between a lower-case letter and an upper-case
     * one.
     * 
     * @param out the place to write the modified text
     * 
     * @param outCtxt the context in which the text is being written
     * 
     * @param text the source text
     */
    public static void writeCamelText(Consumer<? super CharSequence> out,
                                      OutputContext outCtxt,
                                      CharSequence text) {
        if (!outCtxt.canMarkUpInline()) {
            out.accept(outCtxt.escape(text));
            return;
        }

        int start = 0;
        int end = start;
        boolean lastLower = false;
        boolean lastSep = true;
        while (end < text.length()) {
            char c = text.charAt(end);
            boolean upper = Character.isUpperCase(c);
            boolean lower = Character.isLowerCase(c);
            boolean sep = "_.".indexOf(c) >= 0;
            if ((sep && !lastSep) || (upper && lastLower)) {
                out.accept(outCtxt.escape(text.subSequence(start, end)));
                start = end;
                out.accept(HYPERTEXT_WORD_BREAK);
            }
            end++;
            lastSep = sep;
            lastLower = lower;
        }
        if (start < end)
            out.accept(outCtxt.escape(text.subSequence(start, end)));
    }

    /**
     * Get a synthetic description for a whole element if available.
     * This is used for default constructors, and some generated
     * enumeration methods.
     * 
     * @param elem the element in question
     * 
     * @return synthetic documentation, or {@code null} if not available
     */
    public List<? extends DocTree> getSyntheticDescription(Element elem) {
        return getSyntheticDescription(elem, null);
    }

    /**
     * Get a synthetic description for an element or part of it if
     * available. This is used for default constructors, and some
     * generated enumeration methods.
     * 
     * @param elem the element in question
     * 
     * @param kind the part of the element in question, or {@code null}
     * if it's for the whole element
     * 
     * @return synthetic documentation, or {@code null} if not available
     */
    public List<? extends DocTree> getSyntheticDescription(Element elem,
                                                           DocTree.Kind kind) {
        switch (elem.getKind()) {
        default:
            break;

        case PARAMETER: {
            VariableElement paramElem = (VariableElement) elem;
            if (config.isEnumValueOf(paramElem.getEnclosingElement())) {
                return getTreeContent("synth.enum.value-of.name");

            }
        }
            break;

        case METHOD: {
            if (config.isEnumValueOf(elem)) {
                if (kind == null) return getTreeContent("synth.enum.value-of");
                switch (kind) {
                default:
                    break;

                case RETURN:
                    return getTreeContent("synth.enum.value-of.return");
                }
            } else if (config.isEnumValues(elem)) {
                if (kind == null) return getTreeContent("synth.enum.values");
                switch (kind) {
                default:
                    break;

                case RETURN:
                    return getTreeContent("synth.enum.values.return");
                }
            }
        }
            break;

        case CONSTRUCTOR: {
            ExecutableElement execElem = (ExecutableElement) elem;
            if (execElem.getParameters().isEmpty())
                return getTreeContent("synth.default-constr");
        }
            break;

        }

        recordUndocumentedElement(elem);
        return getTreeContent("synth.unspecified");
    }

    private final Collection<Element> undocumentedElements = new HashSet<>();

    /**
     * Get the number of elements lacking documentation.
     * 
     * @return the count so far
     */
    public int countUndocumentedElements() {
        return undocumentedElements.size();
    }

    /**
     * Record an element as lacking documentation.
     * 
     * @param elem the element in question
     */
    public void recordUndocumentedElement(Element elem) {
        if (!config.env.isIncluded(elem)) return;
        undocumentedElements.add(elem);
    }

    /**
     * Get all elements lacking documentation.
     * 
     * @param into the destination for elements
     */
    public void getUndocumentedElements(Collection<? super Element> into) {
        into.addAll(undocumentedElements);
    }

    private static final Pattern keywordPattern =
        Pattern.compile("^\\$(?:[^:]*:\\s*)([^$]+)\\$\\s*$");

    /**
     * The hypertext string used to break words, namely
     * <samp>{@value}</samp>
     */
    public static final String HYPERTEXT_WORD_BREAK =
        "<span class=\"wbr\"><!-- empty --></span>";

    /**
     * Write applied annotations in a preformatted context.
     * 
     * @param out the destination for content
     * 
     * @param ctxt the context for detail level
     * 
     * @param annots the list of annotations
     * 
     * @param indent the indentation to apply after each newline
     * 
     * @param annotDetail 0 means print nothing; 1 means print only the
     * annotation type name; 2 means also print values.
     */
    public void writeAnnotationMirrors(PrintWriter out, OutputContext ctxt,
                                       List<? extends AnnotationMirror> annots,
                                       String indent, int annotDetail) {
        if (annotDetail < 1) return;
        OutputContext plainCtxt =
            OutputContext.plain(config.types, ctxt.location(), ctxt.element());
        LinkContent shortContent =
            LinkContent.NORMAL.withoutContainers().withoutPackage();
        for (AnnotationMirror annot : annots) {
            TypeMirror type = annot.getAnnotationType();
            TypeElement annotElem = (TypeElement) config.types.asElement(type);
            if (annotElem.getAnnotation(Documented.class) == null)
                throw new IllegalArgumentException("annotation " + type
                    + " is not documented");

            Counter amc = new Counter();
            out.print(ctxt.escape("@"));
            amc.sum++;
            writeTypeReference(out::append, ctxt, type, shortContent);
            writeTypeReference(amc::accept, plainCtxt, type, shortContent);
            if (annotDetail > 1) {
                String annotIndent =
                    indent + String.join("", Collections.nCopies(amc.sum, " "));
                writeAnnotationValues(out, ctxt, annot, annotIndent);
            }
            out.print(ctxt.escape("\n" + indent));
        }
    }

    /**
     * Write the values of an applied annotation, if any, in a
     * preformatted context. Nothing is printed if there are no values
     * to print. Otherwise, an opening bracket is printed, followed by
     * each value's field name, and equals sign, the value. Each value
     * that precedes another is also followed by a comma, a newline, and
     * the provided indent (which is also used to determine the indent
     * on nested elements). If there is only one value, the name and
     * equals sign are omitted.
     * 
     * @param out the destination for content
     * 
     * @param ctxt the context for detail level
     * 
     * @param annot the annotation whose values are to be printed
     * 
     * @param indent the indentation to apply after each newline
     */
    public void writeAnnotationValues(PrintWriter out, OutputContext ctxt,
                                      AnnotationMirror annot, String indent) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
            annot.getElementValues();
        if (values.isEmpty()) return;
        String sep = "";
        out.print(ctxt.escape("("));
        LinkContent nameContent =
            LinkContent.NORMAL.withoutContainers().withoutParameters();
        OutputContext nameCtxt =
            OutputContext.plain(config.types, ctxt.location(), ctxt.element());
        for (Map.Entry<? extends ExecutableElement,
                       ? extends AnnotationValue> entry : values.entrySet()) {
            ExecutableElement exec = entry.getKey();
            AnnotationValue value = entry.getValue();

            out.print(ctxt.escape(sep));
            sep = ",\n " + indent;

            Counter nameCounter = new Counter();
            if (values.size() > 1) {
                writeElementReference(out::append, ctxt, exec, nameContent);
                writeElementReference(nameCounter::accept,
                                      nameCtxt,
                                      exec,
                                      nameContent);
                out.print(ctxt.escape("="));
                nameCounter.sum++;
            }

            String valueIndent = indent
                + String.join("", Collections.nCopies(nameCounter.sum, " "));
            writeAnnotationValue(out, ctxt, value, valueIndent);
        }
        out.print(ctxt.escape(")"));
    }

    /**
     * Write an annotation value in a preformatted context. Characters
     * are printed as character literals, strings as string literals,
     * bytes as unsigned hex, shorts with an <samp>h</samp> suffix,
     * longs with <samp>l</samp>, floats with <samp>f</samp>.
     * 
     * @param out the destination for content
     * 
     * @param ctxt the context for detail level
     * 
     * @param value the value to print
     * 
     * @param indent the indentation to apply after each newline in
     * complex values
     */
    public void writeAnnotationValue(PrintWriter out, OutputContext ctxt,
                                     AnnotationValue value, String indent) {
        value.accept(new AnnotationValueVisitor<Void, Void>() {
            @Override
            public Void visit(AnnotationValue av, Void p) {
                throw new UnsupportedOperationException("unnecessary");
            }

            @Override
            public Void visitAnnotation(AnnotationMirror a, Void p) {
                writeAnnotationValues(out, ctxt, a, indent);
                return null;
            }

            @Override
            public Void visitArray(List<? extends AnnotationValue> vals,
                                   Void p) {
                String newIndent = indent + "  ";
                out.print(ctxt.escape("{"));
                for (int i = 0; i < vals.size(); i++) {
                    if (i != 0) out.print(ctxt.escape(",\n" + newIndent));
                    writeAnnotationValue(out, ctxt, vals.get(i), newIndent);
                }
                out.print(ctxt.escape("}"));
                return null;
            }

            @Override
            public Void visitBoolean(boolean b, Void p) {
                out.print(ctxt.escape(Boolean.toString(b)));
                return null;
            }

            @Override
            public Void visitByte(byte b, Void p) {
                out.print(ctxt.escape("0x" + Integer.toString(16, b & 0xff)));
                return null;
            }

            @Override
            public Void visitChar(char c, Void p) {
                String text;
                switch (c) {
                default:
                    text = Character.toString(c);
                    break;

                case '\\':
                    text = "\\\\";
                    break;

                case '\'':
                    text = "\\'";
                    break;

                case '\f':
                    text = "\\f";
                    break;

                case '\r':
                    text = "\\r";
                    break;

                case '\n':
                    text = "\\n";
                    break;

                case '\b':
                    text = "\\b";
                    break;

                case '\t':
                    text = "\\t";
                    break;
                }
                out.print(ctxt.escape("'" + text + "'"));
                return null;
            }

            @Override
            public Void visitDouble(double d, Void p) {
                out.print(ctxt.escape(Double.toString(d)));
                return null;
            }

            @Override
            public Void visitEnumConstant(VariableElement c, Void p) {
                writeElementReference(out::append,
                                      ctxt,
                                      c,
                                      LinkContent.NORMAL.withoutContainers());
                return null;
            }

            @Override
            public Void visitFloat(float f, Void p) {
                out.print(ctxt.escape(Float.toString(f) + "f"));
                return null;
            }

            @Override
            public Void visitInt(int i, Void p) {
                out.print(ctxt.escape(Integer.toString(i)));
                return null;
            }

            @Override
            public Void visitLong(long i, Void p) {
                out.print(ctxt.escape(Long.toString(i) + "l"));
                return null;
            }

            @Override
            public Void visitShort(short s, Void p) {
                out.print(ctxt.escape(Short.toString(s) + "h"));
                return null;
            }

            @Override
            public Void visitString(String s, Void p) {
                StringBuilder escaped = new StringBuilder();
                escaped.append('"');
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    switch (c) {
                    default:
                        escaped.append(c);
                        break;

                    case '\r':
                        escaped.append("\\r");
                        break;

                    case '\n':
                        escaped.append("\\n");
                        break;

                    case '\t':
                        escaped.append("\\t");
                        break;

                    case '\f':
                        escaped.append("\\f");
                        break;

                    case '\b':
                        escaped.append("\\b");
                        break;

                    case '\\':
                        escaped.append("\\\\");
                        break;

                    case '"':
                        escaped.append("\\\"");
                        break;
                    }
                }
                escaped.append('"');
                out.print(ctxt.escape(escaped));
                return null;
            }

            @Override
            public Void visitType(TypeMirror t, Void p) {
                writeTypeReference(out::append, ctxt, t, LinkContent.NORMAL);
                out.print(".class");
                return null;
            }

            @Override
            public Void visitUnknown(AnnotationValue av, Void p) {
                out.print(ctxt.escape("?"));
                return null;
            }
        }, null);
    }

    /**
     * Create a hypertext block context.
     * 
     * @param location the location for the context
     * 
     * @param elem the element context
     * 
     * @return the requested context
     */
    public OutputContext getBlockContext(URI location, Element elem) {
        /* TODO: Use in package/class/member generation. */
        return OutputContext
            .forBlock(config.types,
                      location,
                      elem,
                      HypertextEscaper.forCData(spec.charset),
                      HypertextEscaper.forAttributes(spec.charset));
    }

    /**
     * Create a plain context.
     * 
     * @param location the location for the context
     * 
     * @param elem the element context
     * 
     * @return the requested context
     */
    public OutputContext getPlainContext(URI location, Element elem) {
        /* TODO: Use in package/class/member generation. */
        return OutputContext.plain(config.types, location, elem);
    }
}
