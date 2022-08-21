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

import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.DocTreeFactory;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SimpleDocTreeVisitor;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import uk.ac.lancs.polydoclot.imports.DocImport;
import uk.ac.lancs.polydoclot.imports.DocMapping;
import uk.ac.lancs.polydoclot.imports.DocMappingFactory;
import uk.ac.lancs.polydoclot.imports.DocReference;
import uk.ac.lancs.polydoclot.imports.MacroDocMapping;
import uk.ac.lancs.polydoclot.imports.MacroDocMappingFactory;
import uk.ac.lancs.polydoclot.util.MacroFormatter;
import uk.ac.lancs.polydoclot.util.Utils;

/**
 * Holds configuration specified by the user or by overview meta-data.
 * 
 * @author simpsons
 */
public final class Configuration {
    /**
     * The environment supplied by Javadoc to the doclet
     */
    public final DocletEnvironment env;

    /**
     * Tools for manipulating and examining elements
     */
    public final Elements elements;

    /**
     * A representation of the type {@link Object}
     */
    public final TypeMirror javaLangObject;

    /**
     * A representation of the type <code>java.lang.Deprecated</code>
     */
    public final TypeMirror javaLangDeprecated;

    /**
     * A representation of the type {@link Throwable}
     */
    public final TypeMirror javaLangThrowable;

    /**
     * A representation of the type {@link Error}
     */
    public final TypeMirror javaLangError;

    /**
     * A representation of the type {@link Exception}
     */
    public final TypeMirror javaLangException;

    /**
     * A representation of the type {@link Exception}
     */
    public final TypeMirror javaLangRuntimeException;

    /**
     * A representation of the type {@link String}
     */
    public final TypeMirror javaLangString;

    /**
     * A representation of the type {@link Enum}
     */
    public final TypeMirror javaLangEnum;

    /**
     * Tools for manipulating and examining types
     */
    public final Types types;

    /**
     * Access to Javadoc comments
     */
    public final DocTrees docTrees;

    /**
     * Tools for creating new comment nodes
     */
    public final DocTreeFactory docTreeFactory;

    /**
     * The Javadoc comment of the overview source
     */
    public final DocCommentTree overviewDoc;

    /**
     * Resources for diagnostics in the Javadoc tool's locale
     */
    public final ResourceBundle messageBundle;

    /**
     * A mechanism for reporting diagnostics affecting the doclet
     * framework
     */
    public final Reporter reporter;

    /**
     * Indicates whether the user wants a list of elements lacking
     * documentation.
     */
    public final boolean listUndocumented;

    /**
     * The text to use in the title of each page
     */
    public final List<? extends DocTree> shortTitle;

    /**
     * The full title of the software distribution
     */
    public final List<? extends DocTree> title;

    /**
     * The location of the user-supplied CSS file to be copied into the
     * output
     */
    public final Path styleSource;

    /**
     * The location of the user-supplied CSS to be linked from each page
     */
    public final URI style;

    /**
     * The output directory for all generated content
     */
    public final Path outputDirectory;

    /**
     * The output directory for offline content
     */
    public final Path offlineDirectory;

    /**
     * The output directory for diagnostics content
     */
    public final Path diagnosticsDirectory;

    /**
     * The path to the HTMLTidy program
     */
    public final String tidyProgram;

    /**
     * The suffix to use for internal links to HTML resources
     */
    public final String hypertextLinkSuffix;

    /**
     * The suffix to use for names of generated HTML files
     */
    public final String hypertextFileSuffix = ".html";

    /**
     * A mapping from each directory in source to a jar name
     */
    public final Map<Path, String> dirToJar;

    /**
     * A version string for each jar name
     */
    public final Map<String, String> jarToVersion;

    /**
     * A mapping from group identifier to the set of packages contained
     * in the group
     */
    public final Map<Object, Collection<String>> packageGroupings;

    /**
     * A mapping from group identifier to the set of modules contained
     * in the group
     */
    public final Map<Object, Collection<String>> moduleGroupings;

    /**
     * A mapping from group identifier to its title
     */
    public final Map<Object, List<? extends DocTree>> groupTitles;

    /**
     * A mapping from group identifier to its contained package elements
     */
    public final Map<Object,
                     Collection<? extends PackageElement>> groupedPackages;

    /**
     * A mapping from group identifier to its containeed module elements
     */
    public final Map<Object,
                     Collection<? extends ModuleElement>> groupedModules;

    /**
     * A mapping from author key to that author's details
     */
    public final Map<String, Author> authors;

    /**
     * The set of authors whose details should appear only in the
     * overview
     */
    public final Collection<String> referencedAuthors;

    /**
     * A mapping from package name to external installation
     */
    public final Map<String, DocReference> imports;

    /**
     * A mapping from module name to external installation
     */
    public final Map<String, DocReference> moduleImports;

    /**
     * The sequence of slice specifications to be generated
     */
    public final List<SliceSpecification> sliceSpecs;

    /**
     * The set of included elements not to be documented
     */
    public final Collection<Element> excludedElements;

    /**
     * The set of included elements that are deprecated, either by
     * annotation or by Javadoc tag
     */
    public final Map<Element, Deprecation> deprecatedElements;

    /**
     * The sets of deprecated elements that cause deprecation of others
     */
    public final Map<Element, Collection<Element>> deprecators;

    /**
     * A set of pseudoconstructors for each class
     */
    public final Map<TypeElement,
                     Collection<ExecutableElement>> pseudoConstructors;

    /**
     * A set of fields of any given type or methods returning any given
     * type
     */
    public final Map<TypeElement, Collection<Element>> producers;

    /**
     * A set of methods accepting any given type
     */
    public final Map<TypeElement, Collection<Element>> consumers;

    /**
     * A set of methods accepting any given type and returning the same
     * type
     */
    public final Map<TypeElement, Collection<Element>> transformers;

    /**
     * A set of subtypes for each class
     */
    public final Map<TypeElement, Collection<TypeElement>> knownSubtypes;

    /**
     * A set of direct subtypes for each class
     */
    public final Map<TypeElement, Collection<TypeElement>> knownDirectSubtypes;

    private boolean okay = false;

    /**
     * Background jobs should be invoked on this executor.
     */
    public final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Get a localized diagnostic message with replaced arguments.
     * 
     * @param key the message pattern key
     * 
     * @param args arguments to replace constructs in the patten
     * 
     * @return the resolved message
     */
    public String formatMessage(String key, Object... args) {
        String pattern = messageBundle.getString(key);
        return new MessageFormat(pattern, messageBundle.getLocale())
            .format(args, new StringBuffer(), null).toString();
    }

    /**
     * Get the locale in which Javadoc is running.
     * 
     * @return the tool locale for diagnostic messages to the user
     */
    public Locale getLocale() {
        return messageBundle.getLocale();
    }

    /**
     * Report a localized diagnostic message to the Java tool
     * environment.
     * 
     * @param kind the diagnostic kind
     * 
     * @param elem an element context
     * 
     * @param key the message pattern key
     * 
     * @param args arguments to replace constructs in the patten
     */
    public void report(Diagnostic.Kind kind, Element elem, String key,
                       Object... args) {
        reporter.print(kind, elem, formatMessage(key, args));
    }

    /**
     * Report a localized diagnostic message to the Java tool
     * environment.
     * 
     * @param kind the diagnostic kind
     * 
     * @param path a path context
     * 
     * @param key the message pattern key
     * 
     * @param args arguments to replace constructs in the patten
     */
    public void report(Diagnostic.Kind kind, DocTreePath path, String key,
                       Object... args) {
        reporter.print(kind, path, formatMessage(key, args));
    }

    /**
     * Report a localized diagnostic message to the Java tool
     * environment.
     * 
     * @param kind the diagnostic kind
     * 
     * @param key the message pattern key
     * 
     * @param args arguments to replace constructs in the patten
     */
    public void report(Diagnostic.Kind kind, String key, Object... args) {
        reporter.print(kind, formatMessage(key, args));
    }

    /**
     * Print a localized message with replaced arguments to the standard
     * error output.
     * 
     * @param key the message pattern key
     * 
     * @param args arguments to replace constructs in the patten
     */
    public void diagnostic(String key, Object... args) {
        System.err.println(formatMessage(key, args));
    }

    /**
     * Determine whether this configuration encountered a fatal error
     * during construction.
     * 
     * @return {@code true} if no error was encountered
     */
    public boolean isOkay() {
        return okay;
    }

    /**
     * Add elements to an index. The index is a map from type element to
     * a collection of other elements. A set of type-element keys is
     * specified to map to a single element. This method iterates over
     * the keys, skipping over those that are not to be documented. Of
     * those that remain, an entry in the index is ensured to exist, and
     * the value is added to that entry's value set.
     * 
     * @param <E> the value type
     * 
     * @param dest the index to extend
     * 
     * @param keys the set of keys that must map to the value
     * 
     * @param value the value that each of the keys must map to
     */
    private <E extends Element> void
        index(Map<? super TypeElement, Collection<E>> dest,
              Collection<? extends TypeElement> keys, E value) {
        for (TypeElement key : keys) {
            /* Don't bother with undocumented return types. */
            if (this.excludedElements.contains(key)) continue;
            if (!env.getIncludedElements().contains(key)) continue;

            /* Add this to the set. */
            dest.computeIfAbsent(key, k -> new HashSet<>()).add(value);
        }
    }

    private <E extends Element> Map<TypeElement, Collection<E>>
        fix(Map<? extends TypeElement, Collection<E>> index) {
        for (Map.Entry<? extends TypeElement, Collection<E>> entry : index
            .entrySet())
            entry
                .setValue(Collections.unmodifiableCollection(entry.getValue()));
        return Collections.unmodifiableMap(index);
    }

    /**
     * Create a configuration from the doclet environment and
     * doclet-specific options.
     * 
     * @param env the doclet environment
     * 
     * @param reporter a mechanism for reporting diagnostics affecting
     * the doclet framework
     * 
     * @param messageBundle a bundle of resources for messages in the
     * language of the environment in which Javadoc is running
     * 
     * @param rawTitle command-line supplied distribution title, or
     * {@code null} to use a title embedded in overview source
     * 
     * @param rawShortTitle command-line supplied distribution short
     * title (for page titles), or {@code null} to use a title embedded
     * in the overview source
     * 
     * @param overviewFile name of the overview file to provide overview
     * documentation and global meta-data
     * 
     * @param rawImports imported Javadoc installations to link to for
     * external program elements
     * 
     * @param rawGroupTitles command-line supplied package/module
     * groups, indexed by arbitrary but distinct keys; empty to use
     * group specifications embedded in overview source
     * 
     * @param outputDirectory the output directory for all files
     * 
     * @param diagnosticsDirectory the output directory for diagnostics,
     * or {@code null} if not required
     * 
     * @param tidyProgram the path to the HTMLTidy program, or
     * {@code null} if not to be used
     * 
     * @param hypertextLinkSuffix the suffix to use on HTML files; empty
     * to encourage content negotiation
     * 
     * @param styleSource file to copy into output to provide
     * user-defined CSS
     * 
     * @param style location of external CSS resource
     * 
     * @param sliceSpecs set of slice specifications to generate; empty
     * to use specifications in overview source
     * 
     * @param listUndocumented {@code true} to enable listing of
     * undocumented elements in generated overview
     */
    public Configuration(DocletEnvironment env, Reporter reporter,
                         ResourceBundle messageBundle, String rawTitle,
                         String rawShortTitle, String overviewFile,
                         List<? extends DocImport> rawImports,
                         Map<? extends Object, ? extends String> rawGroupTitles,
                         Path outputDirectory, Path offlineDirectory,
                         Path diagnosticsDirectory, String hypertextLinkSuffix,
                         String tidyProgram, Path styleSource, URI style,
                         Map<? extends Path, ? extends String> dirToJar,
                         Map<? extends String, ? extends String> jarToVersion,
                         List<? extends SliceSpecification> sliceSpecs,
                         boolean listUndocumented) {
        this.env = env;
        this.elements = this.env.getElementUtils();
        this.types = this.env.getTypeUtils();
        this.javaLangDeprecated =
            this.elements.getTypeElement("java.lang.Deprecated").asType();
        this.javaLangObject =
            this.elements.getTypeElement("java.lang.Object").asType();
        this.javaLangString =
            this.elements.getTypeElement("java.lang.String").asType();
        this.javaLangEnum =
            this.elements.getTypeElement("java.lang.Enum").asType();
        this.javaLangThrowable =
            this.elements.getTypeElement("java.lang.Throwable").asType();
        this.javaLangException =
            this.elements.getTypeElement("java.lang.Exception").asType();
        this.javaLangRuntimeException =
            this.elements.getTypeElement("java.lang.RuntimeException").asType();
        this.javaLangError =
            this.elements.getTypeElement("java.lang.Error").asType();
        this.docTrees = this.env.getDocTrees();
        this.docTreeFactory = this.docTrees.getDocTreeFactory();
        this.reporter = reporter;

        this.messageBundle = messageBundle;

        this.outputDirectory = outputDirectory;
        this.offlineDirectory = offlineDirectory;
        this.diagnosticsDirectory = diagnosticsDirectory;
        this.hypertextLinkSuffix = hypertextLinkSuffix;
        this.tidyProgram = tidyProgram;
        this.styleSource = styleSource;
        this.style = style;
        this.listUndocumented = listUndocumented;

        this.dirToJar = Collections.unmodifiableMap(new HashMap<>(dirToJar));
        this.jarToVersion =
            Collections.unmodifiableMap(new HashMap<>(jarToVersion));

        /* Locate the overview file. */
        if (overviewFile != null) {
            Path f = Paths.get(overviewFile).toAbsolutePath();
            FileObject fo = new FileObject() {
                @Override
                public URI toUri() {
                    return f.toUri();
                }

                @Override
                public Writer openWriter() throws IOException {
                    throw new UnsupportedOperationException("unnecessary");
                }

                @Override
                public Reader openReader(boolean ignoreEncodingErrors)
                    throws IOException {
                    return new InputStreamReader(openInputStream());
                }

                @Override
                public OutputStream openOutputStream() throws IOException {
                    throw new UnsupportedOperationException("unnecessary");
                }

                @Override
                public InputStream openInputStream() throws IOException {
                    return Files.newInputStream(f);
                }

                @Override
                public String getName() {
                    return overviewFile;
                }

                @Override
                public long getLastModified() {
                    try {
                        return Files.getLastModifiedTime(f).toMillis();
                    } catch (IOException e) {
                        return 0;
                    }
                }

                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors)
                    throws IOException {
                    StringBuilder result = new StringBuilder();
                    try (Reader in = openReader(ignoreEncodingErrors)) {
                        char[] buf = new char[1024];
                        do {
                            int got = in.read(buf);
                            if (got < 0) break;
                            result.append(buf, 0, got);
                        } while (true);
                    }
                    return result.toString();
                }

                @Override
                public boolean delete() {
                    throw new UnsupportedOperationException("unnecessary");
                }
            };
            overviewDoc = docTrees.getDocCommentTree(fo);
        } else {
            overviewDoc = null;
        }

        {
            /* Get slice specifications if none have been specified in
             * arguments. */
            List<SliceSpecification> tmpSliceSpecs =
                new ArrayList<>(sliceSpecs);
            this.sliceSpecs = Collections.unmodifiableList(tmpSliceSpecs);
            if (tmpSliceSpecs.isEmpty() && overviewDoc != null) {
                for (UnknownBlockTagTree node : DocUtils
                    .getUnknownBlockTags(overviewDoc, "slice")) {
                    String text = treeText(node.getContent());
                    Matcher m = SLICE_PATTERN.matcher(text);
                    if (!m.matches()) {
                        report(Diagnostic.Kind.ERROR, "slice.format.error",
                               text);
                        continue;
                    }
                    Locale locale = Locale.forLanguageTag(m.group("locale"));
                    String suffix = m.group("suffix");
                    Charset charset = m.group("charset") == null ? null :
                        Charset.forName(m.group("charset"));
                    SliceSpecification spec =
                        new SliceSpecification(locale, charset, suffix);
                    tmpSliceSpecs.add(spec);
                }
            }

            /* If no slices have been specified, synthesize one. */
            if (tmpSliceSpecs.isEmpty()) {
                tmpSliceSpecs.add(new SliceSpecification(Locale.getDefault(),
                                                         null, null));
            }
        }

        /* Check that all slices have distinct suffixes. */
        {
            Collection<String> suffixesSeen = new HashSet<>();
            Collection<String> suffixesReported = new HashSet<>();
            for (SliceSpecification spec : this.sliceSpecs) {
                if (suffixesSeen.contains(spec.suffix)) {
                    if (!suffixesReported.contains(spec.suffix)) {
                        report(Diagnostic.Kind.ERROR, "slice.suffix.duplicate",
                               spec.suffix);
                        suffixesReported.add(spec.suffix);
                        okay = false;
                    }
                } else {
                    suffixesSeen.add(spec.suffix);
                }
            }
        }

        /* List all slices to be produced. */
        {
            int langWidth = 0, charsetWidth = 0, suffixWidth = 5;
            for (SliceSpecification spec : this.sliceSpecs) {
                String lang =
                    spec.locale.getDisplayLanguage(messageBundle.getLocale());
                String charset = spec.charset.name();
                if (lang.length() > langWidth) langWidth = lang.length();
                if (charset.length() > charsetWidth)
                    charsetWidth = charset.length();
                if (spec.suffix.length() > suffixWidth)
                    suffixWidth = spec.suffix.length();
            }
            for (SliceSpecification spec : this.sliceSpecs) {
                String tab = String
                    .format("%" + suffixWidth + "s: %" + langWidth + "s, %"
                        + charsetWidth + "s", spec.suffix,
                            spec.locale
                                .getDisplayLanguage(messageBundle.getLocale()),
                            spec.charset.name());
                System.err.println(formatMessage("slice.summary", tab));
            }
        }

        /* Get unresolved long distribution title. */
        {
            List<? extends DocTree> tmpTitle = null;
            if (rawTitle != null) {
                tmpTitle = newTextTree(rawTitle);
            } else if (overviewDoc != null) {
                for (UnknownBlockTagTree t : DocUtils
                    .getUnknownBlockTags(overviewDoc, "title")) {
                    tmpTitle = t.getContent();
                    break;
                }
            }
            if (tmpTitle == null) {
                /* Choose default title. */
                tmpTitle = Collections.singletonList(docTreeFactory
                    .newUnknownInlineTagTree(elements.getName("content"),
                                             newTextTree("untitled")));
            }
            this.title = tmpTitle;
        }

        /* Get unresolved short distribution title. */
        {
            List<? extends DocTree> tmpTitle = null;
            if (rawShortTitle != null) {
                tmpTitle = newTextTree(rawShortTitle);
            } else if (overviewDoc != null) {
                for (UnknownBlockTagTree t : DocUtils
                    .getUnknownBlockTags(overviewDoc, "shortTitle")) {
                    tmpTitle = t.getContent();
                    break;
                }
            }
            if (tmpTitle == null) {
                /* Choose default title. */
                tmpTitle = this.title;
            }
            this.shortTitle = tmpTitle;
        }

        /* Gather author details. */
        if (overviewDoc == null) {
            this.authors = Collections.emptyMap();
            this.referencedAuthors = Collections.emptySet();
        } else {
            Collection<String> referencedAuthors = new LinkedHashSet<>();
            this.referencedAuthors =
                Collections.unmodifiableCollection(referencedAuthors);
            @SuppressWarnings("serial")
            Map<String, Author> authors = new HashMap<>() {
                @Override
                public Author get(Object key) {
                    String k = key.toString();
                    return computeIfAbsent(k, x -> new Author(k));
                }
            };
            for (UnknownBlockTagTree node : DocUtils
                .getUnknownBlockTags(overviewDoc)) {
                switch (node.getTagName()) {
                case "palias": {
                    /* Each word is a new identifier for an existing
                     * author. Map the new identifier to the same object
                     * in the authors' details. */
                    List<DocTree> rem = new ArrayList<>();
                    String label = extractLabel(node.getContent(), rem);
                    if (label == null) break;
                    while (!rem.isEmpty()) {
                        List<DocTree> in = rem;
                        rem = new ArrayList<>();
                        String newLabel = extractLabel(in, rem);
                        if (newLabel == null) break;
                        authors.put(newLabel, authors.get(label));
                        label = newLabel;
                    }
                }
                    break;

                case "pdesc": {
                    /* Each word is an author identifier. These authors
                     * are to be described in detail only on the
                     * overview page, and exist as mere links (from the
                     * author's name) to the overview. */
                    List<? extends DocTree> in = node.getContent();
                    while (!in.isEmpty()) {
                        List<DocTree> rem = new ArrayList<>();
                        String label = extractLabel(in, rem);
                        if (label == null) break;
                        in = rem;
                        referencedAuthors.add(label);
                    }
                }
                    break;

                case "pname": {
                    List<DocTree> rem = new ArrayList<>();
                    String label = extractLabel(node.getContent(), rem);
                    if (label == null) break;
                    authors.get(label).name = rem;
                }
                    break;

                case "paddr": {
                    List<DocTree> rem = new ArrayList<>();
                    String label = extractLabel(node.getContent(), rem);
                    if (label == null) break;
                    authors.get(label).address = rem;
                }
                    break;

                case "ptel": {
                    List<DocTree> rem = new ArrayList<>();
                    String label = extractLabel(node.getContent(), rem);
                    if (label == null) break;
                    authors.get(label).telephone = rem;
                }
                    break;

                case "pemail": {
                    List<DocTree> rem = new ArrayList<>();
                    String label = extractLabel(node.getContent(), rem);
                    if (label == null) break;
                    authors.get(label).email = rem;
                }
                    break;

                case "plink": {
                    List<DocTree> rem = new ArrayList<>();
                    String label = extractLabel(node.getContent(), rem);
                    if (label == null) break;
                    authors.get(label).site = rem;
                }
                    break;
                }
            }
            this.authors = Collections.unmodifiableMap(new HashMap<>(authors));
        }

        /* Look for elements to be excluded because of @undocumented
         * tags. Look for deprecated elements too. */
        {

            class ElementScanner {
                Map<Element, ElementQualities> cache = new HashMap<>();

                private Map<TypeElement, List<TypeElement>> ancestors =
                    new HashMap<>();

                private List<TypeElement> ensureAncestors(Element elem) {
                    final TypeElement type;
                    switch (elem.getKind()) {
                    case ENUM:
                    case CLASS:
                    case INTERFACE:
                    case ANNOTATION_TYPE:
                        type = (TypeElement) elem;
                        break;

                    case METHOD:
                    case ENUM_CONSTANT:
                    case CONSTRUCTOR:
                    case FIELD:
                        type = (TypeElement) elem.getEnclosingElement();
                        break;

                    default:
                        /* We're not concerned with other language
                         * constructs. */
                        return Collections.emptyList();
                    }

                    /* Ensure we know the ancestor classes. */
                    List<TypeElement> bases = ancestors.get(type);
                    if (bases == null) {
                        bases = new ArrayList<>();
                        ancestors.put(type, bases);
                        TypeMirror supertype = type.getSuperclass();
                        if (supertype != null) {
                            TypeElement superElement =
                                (TypeElement) types.asElement(supertype);
                            if (superElement != null && env
                                .getIncludedElements().contains(superElement))
                                bases.add(superElement);
                        }
                        for (TypeMirror impl : type.getInterfaces()) {
                            TypeElement superElement =
                                (TypeElement) types.asElement(impl);
                            if (superElement != null && env
                                .getIncludedElements().contains(superElement))
                                bases.add(superElement);
                        }
                    }

                    return bases;
                }

                private Deprecation
                    getDeprecation(Element elem,
                                   List<? extends TypeElement> bases,
                                   Collection<Element> deprecators) {
                    /* The element is explicitly deprecated if it has
                     * a @deprecated tag or a @Deprecated annotation. */
                    DocCommentTree docs = docTrees.getDocCommentTree(elem);
                    if (docs != null) {
                        boolean found = false;
                        for (DeprecatedTree node : DocUtils
                            .getBlockTags(docs, DeprecatedTree.class)) {
                            found = true;
                            if (node.getBody().isEmpty()) continue;
                            return Deprecation.ADVISED;
                        }
                        if (found) return Deprecation.MARKED;
                    }
                    if (elem.getAnnotation(Deprecated.class) != null)
                        return Deprecation.MARKED;

                    /* An element whose container is deprecated is also
                     * deprecated. */
                    for (Element container = elem.getEnclosingElement();
                         container != null;
                         container = container.getEnclosingElement()) {
                        ElementQualities containerQualities =
                            getQualities(container);
                        if (containerQualities == null) continue;
                        if (containerQualities.deprecation.isExplicit()) {
                            deprecators.add(container);
                            break;
                        }
                        if (containerQualities.deprecation.isDeprecated()) {
                            deprecators.addAll(containerQualities.deprecators);
                            break;
                        }
                    }

                    switch (elem.getKind()) {
                    case ENUM:
                    case CLASS:
                    case INTERFACE:
                    case ANNOTATION_TYPE:
                        /* Check ancestor classes. If any is deprecated,
                         * we deprecate this one is too. */
                        for (TypeElement supertype : bases) {
                            ElementQualities superquals =
                                getQualities(supertype);
                            if (superquals == null) continue;
                            if (superquals.deprecation.isExplicit()) {
                                deprecators.add(supertype);
                                break;
                            }
                            if (superquals.deprecation.isDeprecated()) {
                                deprecators.addAll(superquals.deprecators);
                                break;
                            }
                        }

                        /* TODO: A class which inherits a deprecated
                         * type, or uses it in its generic definition,
                         * should also be deprecated. */
                        break;

                    case METHOD:
                        /* If we find at least one overridden method
                         * that is deprecated, we must regard this
                         * method as deprecated. */
                        ExecutableElement meth = (ExecutableElement) elem;
                        for (TypeElement ancestor : bases) {
                            for (ExecutableElement cand : ElementFilter
                                .methodsIn(ancestor.getEnclosedElements())) {
                                if (!elements
                                    .overrides(meth, cand,
                                               (TypeElement) elem
                                                   .getEnclosingElement()))
                                    continue;
                                /* meth overrides cand. */

                                ElementQualities overriddenQualities =
                                    getQualities(cand);
                                if (overriddenQualities == null) continue;
                                if (overriddenQualities.deprecation
                                    .isExplicit()) {
                                    deprecators.add(cand);
                                    break;
                                }
                                if (overriddenQualities.deprecation
                                    .isDeprecated()) {
                                    deprecators
                                        .addAll(overriddenQualities.deprecators);
                                    break;
                                }
                            }
                        }

                        break;

                    case CONSTRUCTOR:
                    case FIELD:
                        break;

                    default:
                        throw new AssertionError("unreachable");
                    }

                    if (deprecators.isEmpty()) return Deprecation.UNDEPRECATED;
                    return Deprecation.IMPLIED;
                }

                private boolean isExcluded(Element elem,
                                           List<? extends TypeElement> bases) {
                    /* An element marked with @undocumented is
                     * explicitly excluded. */
                    DocCommentTree docs = docTrees.getDocCommentTree(elem);
                    if (docs != null &&
                        DocUtils.getUnknownBlockTags(docs, "undocumented")
                            .iterator().hasNext())
                        return true;

                    /* An element whose container is excluded is also
                     * excluded. */
                    Element container = elem.getEnclosingElement();
                    if (container != null) {
                        ElementQualities containerQualities =
                            getQualities(container);
                        if (containerQualities != null &&
                            containerQualities.excluded) return true;
                    }

                    switch (elem.getKind()) {
                    case ENUM:
                    case CLASS:
                    case INTERFACE:
                    case ANNOTATION_TYPE:
                        /* Check ancestor classes. If any is
                         * undocumented, we exclude this one. */
                        for (TypeElement supertype : bases) {
                            ElementQualities superquals =
                                getQualities(supertype);
                            if (superquals != null && superquals.excluded)
                                return true;
                        }

                        /* TODO: A class which inherits an excluded
                         * type, or uses it in its generic definition,
                         * should also be excluded. */
                        break;

                    case METHOD:
                        /* If we find at least one overridden method
                         * that is undocumented, we must regard this
                         * method as undocumented. */
                        ExecutableElement meth = (ExecutableElement) elem;
                        for (TypeElement ancestor : bases) {
                            for (ExecutableElement cand : ElementFilter
                                .methodsIn(ancestor.getEnclosedElements())) {
                                if (!elements
                                    .overrides(meth, cand,
                                               (TypeElement) elem
                                                   .getEnclosingElement()))
                                    continue;
                                /* meth overrides cand. */

                                ElementQualities overriddenQualities =
                                    getQualities(cand);
                                if (overriddenQualities != null &&
                                    overriddenQualities.excluded) return true;
                            }
                        }
                        /* TODO: An executable member which has an
                         * excluded type in its signature should also be
                         * excluded. */
                        break;

                    case CONSTRUCTOR:
                    case FIELD:
                        break;

                    default:
                        throw new AssertionError("unreachable");
                    }

                    return false;
                }

                ElementQualities getQualities(Element elem) {
                    /* We don't concern ourselves with excluded
                     * types. */
                    Element container = null;
                    switch (elem.getKind()) {
                    case CLASS:
                    case INTERFACE:
                    case ENUM:
                    case ANNOTATION_TYPE:
                        if (!env.getIncludedElements().contains(elem))
                            return null;
                        break;

                    case METHOD:
                    case FIELD:
                    case CONSTRUCTOR:
                        container = elem.getEnclosingElement();
                        if (!env.getIncludedElements().contains(container))
                            return null;
                        break;

                    default:
                        return null;
                    }

                    /* We don't concern ourselves with invisible
                     * elements. */
                    if (!elem.getModifiers().contains(Modifier.PUBLIC) &&
                        !elem.getModifiers().contains(Modifier.PROTECTED))
                        return null;

                    /* Stop early if we've already determined this
                     * one. */
                    if (cache.containsKey(elem)) return cache.get(elem);

                    /* Get cached information about ancestors. */
                    List<TypeElement> bases = ensureAncestors(elem);

                    /* Determine each of the requested qualities. */
                    ElementQualities result = new ElementQualities();
                    result.excluded = isExcluded(elem, bases);
                    result.deprecation =
                        getDeprecation(elem, bases, result.deprecators);

                    /* Cache and return the result. */
                    cache.put(elem, result);
                    return result;
                }
            }

            /* Find all types and their members marked as
             * undocumented. */
            ElementScanner scanner = new ElementScanner();
            for (TypeElement typeElem : ElementFilter
                .typesIn(env.getIncludedElements())) {
                scanner.getQualities(typeElem);
                for (Element memb : typeElem.getEnclosedElements())
                    scanner.getQualities(memb);
            }
            this.excludedElements =
                Collections.unmodifiableCollection(scanner.cache.entrySet()
                    .stream().filter(e -> e.getValue().excluded)
                    .map(Map.Entry::getKey).collect(Collectors.toSet()));
            this.deprecatedElements = Collections.unmodifiableMap(scanner.cache
                .entrySet().stream().filter(e -> !e.getValue().excluded)
                .filter(e -> e.getValue().deprecation.isDeprecated())
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          e -> e.getValue().deprecation)));
            this.deprecators = scanner.cache.entrySet().stream()
                .filter(e -> e.getValue().deprecation == Deprecation.IMPLIED)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Collections
                    .unmodifiableCollection(e.getValue().deprecators)));
        }

        /* Get package groupings. */
        {
            Map<Object, Collection<String>> packageGroupings = new HashMap<>();
            this.packageGroupings =
                Collections.unmodifiableMap(packageGroupings);

            Map<Object, Collection<String>> moduleGroupings = new HashMap<>();
            this.moduleGroupings = Collections.unmodifiableMap(moduleGroupings);

            Map<Object, List<? extends DocTree>> groupTitles =
                new LinkedHashMap<>();
            this.groupTitles = Collections.unmodifiableMap(groupTitles);

            if (!rawGroupTitles.isEmpty()) {
                /* The user is overriding the embedded groupings. Make
                 * the provided titles look like document comments. */
                diagnostic("groups.selection.command-line");
                for (Map.Entry<? extends Object,
                               ? extends String> entry : rawGroupTitles
                                   .entrySet()) {
                    groupTitles.put(entry.getKey(),
                                    newTextTree(entry.getValue()));
                }
            } else if (overviewDoc != null) {
                /* Scan the overview text for @group and @package
                 * tags. */
                diagnostic("groups.selection.embedded");
                for (UnknownBlockTagTree node : DocUtils
                    .getUnknownBlockTags(overviewDoc)) {
                    switch (node.getTagName()) {
                    case "group": {
                        List<DocTree> rem = new ArrayList<>();
                        String label = extractLabel(node.getContent(), rem);
                        if (label == null) break;
                        groupTitles.put(label, rem);
                    }
                        break;

                    case "package": {
                        List<DocTree> rem = new ArrayList<>();
                        String label = extractLabel(node.getContent(), rem);
                        if (label == null) break;
                        Collection<String> seq = packageGroupings
                            .computeIfAbsent(label, l -> new ArrayList<>());
                        seq.add(treeText(rem));
                    }
                        break;

                    case "module": {
                        List<DocTree> rem = new ArrayList<>();
                        String label = extractLabel(node.getContent(), rem);
                        if (label == null) break;
                        Collection<String> seq = moduleGroupings
                            .computeIfAbsent(label, l -> new ArrayList<>());
                        seq.add(treeText(rem));
                    }
                        break;
                    }
                }
            }

            /* Invert the mapping from group to its module members. */
            Map<String, Object> moduleToGroup = new HashMap<>();
            for (Map.Entry<Object, Collection<String>> entry : moduleGroupings
                .entrySet()) {
                for (String name : entry.getValue())
                    moduleToGroup.put(name, entry.getKey());
            }

            /* Identify all modules to be documented. */
            Map<Object, Collection<ModuleElement>> groupedModules =
                new HashMap<>();
            Collection<ModuleElement> nakedModules = new HashSet<>();
            for (ModuleElement mod : ElementFilter
                .modulesIn(env.getIncludedElements())) {
                /* We never document the unnamed module, even if the
                 * packages being documented are effectively partially
                 * defining it. */
                if (mod.isUnnamed()) continue;

                /* Skip modules explicitly marked as not to be
                 * documented. This step could be redundant, as modules
                 * and packages should never really be
                 * marked @undocumented. */
                if (excludedElements.contains(mod)) continue;

                /* Decide which group this module belongs in. */
                Object groupKey =
                    moduleToGroup.get(mod.getQualifiedName().toString());
                if (groupKey == null)
                    nakedModules.add(mod);
                else
                    groupedModules
                        .computeIfAbsent(groupKey, k -> new HashSet<>())
                        .add(mod);
            }

            /* Invert the mapping from group to its package members. */
            Map<String, Object> packageToGroup = new HashMap<>();
            for (Map.Entry<Object, Collection<String>> entry : packageGroupings
                .entrySet()) {
                for (String name : entry.getValue())
                    packageToGroup.put(name, entry.getKey());
            }

            /* Identify packages not belonging to modules, and put them
             * into the appropriate groups if necessary. Packages
             * belonging to documented modules don't need to be listed
             * here, because they will be listed in their respective
             * module's documentation. */
            Map<Object, Collection<PackageElement>> groupedPackages =
                new HashMap<>();
            List<PackageElement> nakedPackages = new ArrayList<>();
            for (PackageElement pkgDef : ElementFilter
                .packagesIn(env.getIncludedElements())) {
                /* Skip packages explicitly marked as not to be
                 * documented. This step could be redundant, as modules
                 * and packages should never really be
                 * marked @undocumented. */
                if (excludedElements.contains(pkgDef)) continue;

                /* Skip packages belonging to a named module, whose
                 * documentation will list it separately. */
                ModuleElement mod = elements.getModuleOf(pkgDef);
                if (mod != null && !mod.isUnnamed()) continue;

                /* Decide which group this package belongs in. */
                Object groupKey =
                    packageToGroup.get(pkgDef.getQualifiedName().toString());
                if (groupKey == null)
                    nakedPackages.add(pkgDef);
                else
                    groupedPackages
                        .computeIfAbsent(groupKey, k -> new HashSet<>())
                        .add(pkgDef);
            }

            /* Add the naked packages and modules as an automatic group.
             * We don't know the title yet, because we don't have a
             * slice. */
            if (!nakedPackages.isEmpty() || !nakedModules.isEmpty()) {
                groupTitles.put(-1, null);
                groupedPackages.put(-1, nakedPackages);
                groupedModules.put(-1, nakedModules);
            }

            for (Map.Entry<Object,
                           Collection<PackageElement>> entry : groupedPackages
                               .entrySet())
                entry.setValue(Collections
                    .unmodifiableCollection(entry.getValue()));
            this.groupedPackages = Collections.unmodifiableMap(groupedPackages);
            for (Map.Entry<Object,
                           Collection<ModuleElement>> entry : groupedModules
                               .entrySet())
                entry.setValue(Collections
                    .unmodifiableCollection(entry.getValue()));
            this.groupedModules = Collections.unmodifiableMap(groupedModules);
        }

        /* Look for methods tagged as constructors. Look for known
         * subtypes. Look for methods returning each type. Look for
         * fields of each type. */
        {
            Map<TypeElement, Collection<TypeElement>> knownSubtypes =
                new HashMap<>();
            Map<TypeElement, Collection<ExecutableElement>> pseudoConstructors =
                new HashMap<>();
            Map<TypeElement, Collection<Element>> producers = new HashMap<>();
            Map<TypeElement, Collection<Element>> consumers = new HashMap<>();
            Map<TypeElement, Collection<Element>> transformers =
                new HashMap<>();

            for (TypeElement typeElem : ElementFilter
                .typesIn(env.getIncludedElements())) {
                if (this.excludedElements.contains(typeElem)) continue;

                /* Record this type a subtype of its supertypes. */
                {
                    Collection<TypeElement> supertypes = new HashSet<>();
                    this.addSupertypes(supertypes, typeElem);
                    for (TypeElement supertype : supertypes) {
                        if (!env.getIncludedElements().contains(supertype))
                            continue;
                        if (this.excludedElements.contains(supertype)) continue;
                        Collection<TypeElement> set = knownSubtypes
                            .computeIfAbsent(supertype, k -> new HashSet<>());
                        set.add(typeElem);
                    }
                }

                for (VariableElement varElem : ElementFilter
                    .fieldsIn(typeElem.getEnclosedElements())) {
                    /* Skip invisible and excluded elements. */
                    if (!varElem.getModifiers().contains(Modifier.PUBLIC) &&
                        !varElem.getModifiers().contains(Modifier.PROTECTED))
                        continue;
                    if (excludedElements.contains(varElem)) continue;

                    /* Get the field's type, and eliminate array
                     * components and generics. */
                    TypeMirror varType = varElem.asType();
                    while (varType.getKind() == TypeKind.ARRAY)
                        varType = ((ArrayType) varType).getComponentType();
                    varType = types.erasure(varType);

                    /* Only declared types can be classes. */
                    if (varType.getKind() != TypeKind.DECLARED) continue;
                    TypeElement varTypeElem =
                        (TypeElement) ((DeclaredType) varType).asElement();

                    /* Get all types this field's type could be assigned
                     * to. */
                    Collection<TypeElement> supertypes = new HashSet<>();
                    supertypes.add(varTypeElem);
                    addSupertypes(supertypes, varTypeElem);

                    index(producers, supertypes, varElem);
                }

                for (ExecutableElement execElem : ElementFilter
                    .methodsIn(typeElem.getEnclosedElements())) {
                    /* Skip invisible and excluded elements. */
                    if (!execElem.getModifiers().contains(Modifier.PUBLIC) &&
                        !execElem.getModifiers().contains(Modifier.PROTECTED))
                        continue;
                    if (excludedElements.contains(execElem)) continue;

                    /* Get the return type and eliminate any array
                     * dimensions. */
                    TypeMirror returnType = execElem.getReturnType();
                    while (returnType.getKind() == TypeKind.ARRAY) {
                        returnType =
                            ((ArrayType) returnType).getComponentType();
                    }

                    /* Ignore generics. */
                    returnType = types.erasure(returnType);

                    /* Only declared types can be classes. */
                    if (returnType.getKind() == TypeKind.DECLARED) {
                        TypeElement returnElem =
                            (TypeElement) ((DeclaredType) returnType)
                                .asElement();

                        /* Get all types this return type could be
                         * assigned to. */
                        Collection<TypeElement> supertypes = new HashSet<>();
                        supertypes.add(returnElem);
                        addSupertypes(supertypes, returnElem);

                        /* Look for the @constructor tag, and add the
                         * element to the pseudo-constructor index if
                         * found. */
                        DocCommentTree docs =
                            docTrees.getDocCommentTree(execElem);
                        if (DocUtils.getUnknownBlockTags(docs, "constructor")
                            .iterator().hasNext())
                            index(pseudoConstructors, supertypes, execElem);

                        /* Index this method as a producer. */
                        index(producers, supertypes, execElem);
                    }

                    for (VariableElement varElem : execElem.getParameters()) {
                        /* Get the parameter's type, and eliminate array
                         * components and generics. */
                        TypeMirror varType = varElem.asType();
                        while (varType.getKind() == TypeKind.ARRAY)
                            varType = ((ArrayType) varType).getComponentType();
                        varType = types.erasure(varType);

                        /* Only declared types can be classes. */
                        if (varType.getKind() != TypeKind.DECLARED) continue;
                        TypeElement varTypeElem =
                            (TypeElement) ((DeclaredType) varType).asElement();

                        /* Get all types this field's type could be
                         * assigned to. */
                        Collection<TypeElement> supertypes = new HashSet<>();
                        supertypes.add(varTypeElem);
                        addSupertypes(supertypes, varTypeElem);

                        index(consumers, supertypes, execElem);
                    }
                }
            }

            /* Separate transformers from producers and consumers. */
            Collection<TypeElement> xformKeys = new HashSet<>();
            xformKeys.addAll(consumers.keySet());
            xformKeys.addAll(producers.keySet());
            for (TypeElement key : xformKeys) {
                Collection<Element> cons =
                    consumers.getOrDefault(key, Collections.emptySet());
                Collection<Element> prods =
                    producers.getOrDefault(key, Collections.emptySet());
                Collection<Element> xforms = new HashSet<>(cons);
                xforms.retainAll(prods);
                if (xforms.isEmpty()) continue;
                cons.removeAll(xforms);
                prods.removeAll(xforms);
                transformers.computeIfAbsent(key, k -> new HashSet<>())
                    .addAll(xforms);
            }

            /* Create a deep read-only view of the results. */
            this.pseudoConstructors = fix(pseudoConstructors);
            this.knownSubtypes = fix(knownSubtypes);
            this.producers = fix(producers);
            this.consumers = fix(consumers);
            this.transformers = fix(transformers);
        }

        /* Get a class hierarchy by paring down the known subtypes to
         * only direct subtypes. */
        {
            Map<TypeElement, Collection<TypeElement>> directSubtypes =
                new HashMap<>(knownSubtypes.entrySet().stream()
                    .collect(Collectors
                        .toMap(e -> e.getKey(),
                               e -> new HashSet<>(e.getValue()))));
            for (Map.Entry<TypeElement,
                           Collection<TypeElement>> entry : directSubtypes
                               .entrySet())
                entry.getValue()
                    .removeIf(e -> !types.directSupertypes(e.asType())
                        .contains(entry.getKey().asType()));

            /* Present the unmodifiable computed hierarchy. */
            this.knownDirectSubtypes = fix(directSubtypes);
        }

        /* Resolve imports. */
        {
            DocMappingFactory mappingFactory =
                new MacroDocMappingFactory(this::setElementProperties,
                                           Polydoclot.SCHEME_PROPERTY_NAME,
                                           DocImport.MODULE_JDK_SCHEME);
            Map<String, DocReference> imports = new HashMap<>();
            this.imports = Collections.unmodifiableMap(imports);
            Map<String, DocReference> moduleImports = new HashMap<>();
            this.moduleImports = Collections.unmodifiableMap(moduleImports);
            for (DocImport imp : rawImports) {
                try {
                    imp.install(imports, moduleImports, mappingFactory);
                } catch (IOException ex) {
                    report(Diagnostic.Kind.ERROR, "import.failure.read",
                           ex.getMessage(), imp.location);
                    return;
                }
            }

            /* Add our own packages and modules. */
            URI ownRoot = outputDirectory.toUri();
            MacroFormatter fmt = new MacroFormatter(Polydoclot.POLYDOCLOT_SCHEME
                + this.hypertextLinkSuffix);
            DocMapping ownMapping =
                new MacroDocMapping(this::setElementProperties, ownRoot, fmt);
            Properties ownProps = new Properties();
            ownProps.setProperty(Polydoclot.SCHEME_PROPERTY_NAME,
                                 Polydoclot.POLYDOCLOT_SCHEME
                                     + this.hypertextLinkSuffix);
            DocReference ownReference =
                new DocReference(ownRoot, ownProps, ownMapping);
            for (PackageElement pkg : ElementFilter
                .packagesIn(env.getIncludedElements())) {
                imports.put(pkg.getQualifiedName().toString(), ownReference);
            }
            for (ModuleElement mod : ElementFilter
                .modulesIn(env.getIncludedElements())) {
                moduleImports.put(mod.getQualifiedName().toString(),
                                  ownReference);
            }
        }

        /* Ensure that the destination directory is set and exists. */
        if (this.outputDirectory == null) {
            report(Diagnostic.Kind.ERROR, "output.unset");
            okay = false;
            return;
        }
        if (!Files.exists(this.outputDirectory)) {
            report(Diagnostic.Kind.ERROR, "output.non-existant",
                   this.outputDirectory);
            okay = false;
            return;
        }
        if (!Files.isDirectory(this.outputDirectory)) {
            report(Diagnostic.Kind.ERROR, "output.not-directory",
                   this.outputDirectory);
            okay = false;
            return;
        }

        if (this.diagnosticsDirectory != null) {
            /* Ensure that the diagnostics directory exists. */
            if (!Files.isDirectory(this.diagnosticsDirectory)) {
                report(Diagnostic.Kind.ERROR, "diagnostics.non-existant",
                       this.diagnosticsDirectory);
                okay = false;
                return;
            }
            if (!Files.isDirectory(this.diagnosticsDirectory)) {
                report(Diagnostic.Kind.ERROR, "diagnostics.not-directory",
                       this.diagnosticsDirectory);
                okay = false;
                return;
            }
        }
        okay = true;
    }

    private void gatherSupertypes(Map<TypeElement, Integer> distances,
                                  int distance, TypeMirror type) {
        if (type == null) return;
        if (type.getKind() == TypeKind.NONE) return;
        TypeElement elem = (TypeElement) types.asElement(type);
        if (elem == null) return;
        gatherSupertypes(distances, distance, elem);
    }

    private void gatherSupertypes(Map<TypeElement, Integer> distances,
                                  int distance, TypeElement type) {
        if (type == null) return;
        int existing = distances.getOrDefault(type, Integer.MAX_VALUE);
        if (distance >= existing) return;
        distances.put(type, distance);
        gatherSupertypes(distances, distance + 1, type.getSuperclass());
        for (TypeMirror impl : type.getInterfaces())
            gatherSupertypes(distances, distance, impl);
    }

    /**
     * Get all supertypes of a type in a preferred order of inheritance.
     * More specific types appear earlier in the result. Types closer to
     * the original appear earlier.
     * 
     * @param start the starting type, which is not included in the
     * result
     * 
     * @return all supertypes of the starting type, in inheritance order
     */
    public List<TypeElement> getInheritanceOrder(TypeElement start) {
        /* Gather all supertypses together, remembering how near each is
         * to the start. */
        Map<TypeElement, Integer> distances = new HashMap<>();
        gatherSupertypes(distances, 1, start.getSuperclass());
        for (TypeMirror impl : start.getInterfaces())
            gatherSupertypes(distances, 1, impl);

        /* Sort the types, and present as a list. */
        List<TypeElement> result = new ArrayList<>(distances.keySet());
        result.sort((a, b) -> {
            if (types.isSubtype(a.asType(), b.asType())) return -1;
            if (types.isSubtype(b.asType(), a.asType())) return +1;
            return distances.get(a) - distances.get(b);
        });
        return result;
    }

    /**
     * Collect the supertypes of a type in a search order for
     * overriding.
     * 
     * @param into the destination collection
     * 
     * @param start the type whose supertypes are required
     */
    public void addSupertypes(Collection<? super TypeElement> into,
                              TypeElement start) {
        Collection<TypeElement> next = new LinkedHashSet<>();

        {
            TypeMirror supertype = start.getSuperclass();
            if (supertype != null) {
                TypeElement superelem =
                    (TypeElement) types.asElement(supertype);
                if (superelem != null)
                    if (into.add(superelem)) next.add(superelem);
            }
        }

        for (TypeMirror supertype : start.getInterfaces()) {
            TypeElement superelem = (TypeElement) types.asElement(supertype);
            assert superelem != null;
            if (into.add(superelem)) next.add(superelem);
        }

        for (TypeElement n : next) {
            assert n != null;
            addSupertypes(into, n);
        }
    }

    private static class ElementQualities {
        boolean excluded;

        Deprecation deprecation;

        Collection<Element> deprecators = new HashSet<>();

        @Override
        public String toString() {
            return (excluded ? " excluded" : "")
                + (deprecation.isDeprecated() ? " deprecated" : "");
        }
    }

    /**
     * Create a new plain-text tree.
     * 
     * @param text the plain text
     * 
     * @return a singleton of the plain text
     */
    public List<? extends DocTree> newTextTree(String text) {
        return Collections.singletonList(docTreeFactory.newTextTree(text));
    }

    /**
     * Convert a tree sequence to a flat string, without regard for
     * slicing context.
     * 
     * @param in the source list of trees
     * 
     * @return the flat string content of the list
     */
    public String treeText(List<? extends DocTree> in) {
        StringBuilder pkg = new StringBuilder();
        for (DocTree r : in) {
            r.accept(new SimpleDocTreeVisitor<Void, Void>() {
                @Override
                public Void visitLiteral(LiteralTree node, Void p) {
                    pkg.append(node.getBody().getBody());
                    return null;
                }

                @Override
                public Void visitText(TextTree node, Void p) {
                    pkg.append(node.getBody());
                    return null;
                }

            }, null);
        }
        return pkg.toString();
    }

    /**
     * Separate the first word from an unknown block tag's content.
     * 
     * @param node the tag whose content is to be split
     * 
     * @param remaining a place to store the remaining content
     * 
     * @return the first word
     */
    public String extractFlatLabel(UnknownBlockTagTree node,
                                   List<? super DocTree> remaining) {
        return extractLabel(unflattenDoc(node), remaining);
    }

    /**
     * Separate the first word from an unknown in-line tag's content.
     * 
     * @param node the tag whose content is to be split
     * 
     * @param remaining a place to store the remaining content
     * 
     * @return the first word
     */
    public String extractFlatLabel(UnknownInlineTagTree node,
                                   List<? super DocTree> remaining) {
        return extractLabel(unflattenDoc(node), remaining);
    }

    /**
     * Assuming the first tree is a simple text tree, split it at the
     * first space, and return the first word as the label. Write the
     * remaining tags to a new list.
     * 
     * @param in the the source list of trees
     * 
     * @param remaining the remaining trees
     * 
     * @return the extracted label, or {@code null} if the first tree is
     * not text
     */
    public String extractLabel(List<? extends DocTree> in,
                               List<? super DocTree> remaining) {
        if (in.isEmpty()) return null;
        DocTree first = in.get(0);
        if (!(first instanceof TextTree)) {
            remaining.addAll(in);
            return null;
        }
        TextTree firstText = (TextTree) first;
        Matcher m = LABEL_PATTERN.matcher(firstText.getBody());
        if (!m.matches()) {
            remaining.addAll(in);
            return null;
        }
        String remText = m.group("rem");
        String label = m.group("label");
        if (remText != null && !remText.isEmpty())
            remaining.add(docTreeFactory.newTextTree(remText));
        remaining.addAll(in.subList(1, in.size()));
        return label;
    }

    private static final Pattern LABEL_PATTERN =
        Pattern.compile("^\\s*(?<label>[^\\s]+)\\s*(?<rem>.*)$",
                        Pattern.MULTILINE | Pattern.DOTALL);

    /**
     * Write <code>&lt;link&gt;</code> elements for an HTML page to set
     * its stylesheets.
     * 
     * @param out the destination for writing the elements
     * 
     * @param outCtxt the output context for writing the element
     */
    public void writeStyleLinks(PrintWriter out, OutputContext outCtxt) {
        URI defaultLoc = URI.create(MetadataGenerator.DEFAULT_STYLES_NAME);
        out.printf("<link rel=\"stylesheet\"" + " type=\"text/css\""
            + " href=\"%s\">\n",
                   outCtxt.escapeAttribute(Utils
                       .relativize(outCtxt.location(), defaultLoc)
                       .toASCIIString()));
        if (style != null)
            out.printf("<link rel=\"stylesheet\"" + " type=\"text/css\""
                + " href=\"%s\">\n",
                       outCtxt.escapeAttribute(style.toASCIIString()));
        if (styleSource != null) {
            URI copiedLoc = URI.create(MetadataGenerator.COPIED_STYLES_NAME);
            out.printf("<link rel=\"stylesheet\"" + " type=\"text/css\""
                + " href=\"%s\">\n",
                       outCtxt.escapeAttribute(Utils
                           .relativize(outCtxt.location(), copiedLoc)
                           .toASCIIString()));
        }
    }

    /**
     * Resolve a textual reference to a type within an element context.
     * 
     * @param ctxt the context in which the type is referenced
     * 
     * @param typeText the textual reference
     * 
     * @return the structural representation of the referenced type
     */
    private TypeMirror resolveType(Element ctxt, String typeText) {
        /* Detect and handle primitive types. */
        switch (typeText) {
        case "boolean":
            return types.getPrimitiveType(TypeKind.BOOLEAN);

        case "byte":
            return types.getPrimitiveType(TypeKind.BYTE);

        case "char":
            return types.getPrimitiveType(TypeKind.CHAR);

        case "short":
            return types.getPrimitiveType(TypeKind.SHORT);

        case "int":
            return types.getPrimitiveType(TypeKind.INT);

        case "float":
            return types.getPrimitiveType(TypeKind.FLOAT);

        case "long":
            return types.getPrimitiveType(TypeKind.LONG);

        case "double":
            return types.getPrimitiveType(TypeKind.DOUBLE);

        case "void":
            return types.getPrimitiveType(TypeKind.VOID);

        default:
            break;
        }

        /* TODO: Allow a type to be specified by
         * module.name/package.Type? Split the type text by slash, and
         * pass that before it as the second argument. */
        final int sl = typeText.indexOf('/');
        final String mt, pct;
        if (sl < 0) {
            mt = null;
            pct = typeText;
        } else {
            mt = typeText.substring(0, sl);
            pct = typeText.substring(sl + 1);
        }
        List<? extends Element> elems = getModulePackageClass(ctxt, mt, pct);
        if (elems == null) return null;
        for (Element elem : elems) {
            switch (elem.getKind()) {
            case CLASS:
            case ENUM:
            case INTERFACE:
            case ANNOTATION_TYPE:
                return elem.asType();

            default:
                continue;
            }
        }
        return null;
    }

    private PackageElement seekPackage(ModuleElement modCtxt, Element orig,
                                       String packageClassText) {
        /* Look for a package in the same module. */
        if (modCtxt != null) {
            PackageElement inSameMod =
                elements.getPackageElement(modCtxt, packageClassText);
            if (inSameMod != null) return inSameMod;
        }

        /* Look for packages in any module. */
        Collection<? extends PackageElement> pkgs =
            elements.getAllPackageElements(packageClassText);
        switch (pkgs.size()) {
        case 0:
            return null;

        default:
            /* Pick one arbitrarily, after warning the user. */
            report(Kind.WARNING, orig, "link.ambiguity.package",
                   packageClassText, pkgs.stream().map(elements::getModuleOf)
                       .collect(Collectors.toList()));
            // Fall through.
        case 1:
            return pkgs.iterator().next();
        }
    }

    private List<TypeElement> seekType(ModuleElement modCtxt, Element orig,
                                       String packageClassText) {
        if (modCtxt != null) {
            TypeElement inSameMod =
                elements.getTypeElement(modCtxt, packageClassText);
            if (inSameMod != null) return Collections.singletonList(inSameMod);
        }
        Collection<? extends TypeElement> types =
            elements.getAllTypeElements(packageClassText);
        switch (types.size()) {
        case 0:
            return null;

        default:
            /* Pick one arbitrarily, after warning the user. */
            report(Kind.WARNING, orig, "link.ambiguity.class", packageClassText,
                   types.stream().map(elements::getModuleOf)
                       .collect(Collectors.toList()));
            // Fall through.
        case 1:
            return Collections.unmodifiableList(new ArrayList<>(types));
        }
    }

    private TypeElement seekImport(ModuleElement modCtxt, Element topLevelClass,
                                   String packageClassText) {
        /* Split the name into a prefix (everything before the first
         * dot) and a suffix (everything else, including the dot). If
         * there's no dot, the suffix is empty, and the prefix is
         * everything. We seek imports ending with the prefix, and
         * append the suffix to them to see if the combined name
         * exists. */
        final String prefix, suffix;
        final int firstDot = packageClassText.indexOf('.');
        if (firstDot < 0) {
            prefix = packageClassText;
            suffix = "";
        } else {
            prefix = packageClassText.substring(0, firstDot);
            suffix = packageClassText.substring(firstDot);
        }

        for (ImportTree imp : docTrees.getPath(topLevelClass)
            .getCompilationUnit().getImports()) {
            /* Static imports are not for classes (right?), so we skip
             * them. */
            if (imp.isStatic()) continue;

            /* Get the imported name. */
            String text = imp.getQualifiedIdentifier().toString();

            final String candName;
            if (text.endsWith(".*")) {
                /* The import is for a whole package. Handle it by
                 * extracting the package name, appending the first name
                 * component. */
                candName = text.substring(0, text.length() - 1) + prefix;
            } else if (text.endsWith("." + prefix)) {
                /* A single class is imported, and it ends with our
                 * first name component. */
                candName = text;
            } else {
                /* A single class is imported, but does not match our
                 * first name component, so try another. */
                continue;
            }
            final String fullName = candName + suffix;

            /* Look up the candidate name, and return if found. */
            TypeElement cand = elements.getTypeElement(modCtxt, fullName);
            if (cand == null) {
                Collection<? extends TypeElement> cands =
                    elements.getAllTypeElements(fullName);
                switch (cands.size()) {
                case 0:
                    continue;

                case 1:
                    cand = cands.iterator().next();
                    break;

                default:
                    continue;
                }
            }
            if (cand != null) return cand;
        }
        return null;
    }

    private boolean isSuitableContext(Element ctxt) {
        switch (ctxt.getKind()) {
        case CLASS:
        case INTERFACE:
        case ENUM:
        case ANNOTATION_TYPE:
        case PACKAGE:
        case MODULE:
            return true;

        default:
            return false;
        }
    }

    /**
     * Get the class, package or module referred to by a piece of text
     * within a given context.
     * 
     * <p>
     * When module text is provided (even if empty), that module is
     * first sought. If not found, {@code null} is returned. Otherwise,
     * if the second text is {@code null} or empty, the module is
     * returned. Otherwise, it is treated as a fully qualified class
     * name, and sought within the module, and returned if found.
     * Otherwise, it is treated as a package name, sought within the
     * module, and returned, whether {@code null} or not.
     * 
     * <p>
     * When module text is {@code null}, the provided element context is
     * consulted. First, if the context is not a class, package or
     * module, the enclosing elements are walked until it is, or the end
     * of the path is reached.
     * 
     * <p>
     * If the context is a package TODO.
     * 
     * @param ctxt the context in which the reference is made
     * 
     * @param moduleText the text before the forward slash identifying a
     * module, or {@code null} if there was no slash
     * 
     * @param packageClassText the text after the slash, or the whole
     * text if there was no slash
     * 
     * @return the identified class(es), package(s) or module(s), or
     * {@code null} if not found
     */
    private List<? extends Element>
        getModulePackageClass(Element ctxt, String moduleText,
                              String packageClassText) {
        /* When a module is specified, the second piece of text must be
         * a package or a fully qualified class. */
        if (moduleText != null) {
            ModuleElement module = elements.getModuleElement(moduleText);
            if (module == null) return null;
            if (packageClassText == null || packageClassText.isEmpty())
                return Collections.singletonList(module);
            TypeElement type =
                elements.getTypeElement(module, packageClassText);
            if (type != null) return Collections.singletonList(type);
            return Collections.singletonList(elements
                .getPackageElement(module, packageClassText));
        }

        /* Ensure that the context is a class, package or module. If
         * it's a class member, walk to the nearest enclosing class
         * element. If we run out, we fail. (TODO: Do we? Doesn't look
         * like it!) */
        final Element orig = ctxt;
        while (ctxt != null && !isSuitableContext(ctxt))
            ctxt = ctxt.getEnclosingElement();

        /* If no p/c text was specified, the context and its enclosing
         * types are the result. */
        if (packageClassText == null) {
            List<Element> result = new ArrayList<>();
            do {
                result.add(ctxt);
                ctxt = ctxt.getEnclosingElement();
            } while (ctxt != null && ctxt instanceof TypeElement);
            return Collections.unmodifiableList(result);
        }

        /* If no context was provided (It could be the overview, or some
         * synthetic content.), we'll just have to look for absolute
         * class/package names. */
        if (ctxt == null) {
            /* Look up a full class. */
            {
                List<TypeElement> cand = seekType(null, orig, packageClassText);
                if (cand != null) return cand;
            }

            {
                /* Look up a java.lang class. */
                List<TypeElement> cand =
                    seekType(null, orig, "java.lang." + packageClassText);
                if (cand != null) return cand;
            }

            /* No class was found. It must be a package. */
            PackageElement pe = seekPackage(null, orig, packageClassText);
            return pe == null ? null : Collections.singletonList(pe);
        }

        switch (ctxt.getKind()) {
        case PACKAGE: {
            /* The context is a package, so the reference is either to a
             * class in this package, or a package. */
            PackageElement pkgCtxt = (PackageElement) ctxt;
            ModuleElement modCtxt = elements.getModuleOf(pkgCtxt);

            /* Look up a class in this package. */
            {
                String name = pkgCtxt.getQualifiedName().toString() + '.'
                    + packageClassText;
                List<TypeElement> cand = seekType(modCtxt, orig, name);
                if (cand != null) return cand;
            }

            /* Look up a full class. */
            {
                List<TypeElement> cand =
                    seekType(modCtxt, orig, packageClassText);
                if (cand != null) return cand;
            }

            /* Now we look for an import. */
            {
                TypeElement imp = seekImport(modCtxt, ctxt, packageClassText);
                if (imp != null) return Collections.singletonList(imp);
            }

            {
                /* Look up a java.lang class. */
                List<TypeElement> cand =
                    seekType(modCtxt, orig, "java.lang." + packageClassText);
                if (cand != null) return cand;
            }

            /* No class was found. It must be a package. */
            PackageElement pe = seekPackage(modCtxt, orig, packageClassText);
            return pe == null ? null : Collections.singletonList(pe);
        }

        case MODULE: {
            return seekType((ModuleElement) ctxt, orig, packageClassText);
        }

        default:
            break;
        }

        /* The context is a class. We must look for nested classes and
         * cousins, then check imports. */
        PackageElement pkgCtxt = elements.getPackageOf(ctxt);
        ModuleElement modCtxt = elements.getModuleOf(pkgCtxt);

        /* Look for a nested/cousin class from our current position, and
         * work outwards. Keep a note of the top-level class if we reach
         * it, as we need it to look up imports. */
        TypeElement topLevelClass = null;
        assert ctxt != null;
        assert ctxt instanceof TypeElement;
        do {
            topLevelClass = (TypeElement) ctxt;
            TypeElement result = elements
                .getTypeElement(modCtxt,
                                topLevelClass.getQualifiedName().toString()
                                    + '.' + packageClassText);
            if (result != null) return Collections.singletonList(result);
            ctxt = ctxt.getEnclosingElement();
        } while (ctxt != null && ctxt instanceof TypeElement);
        assert topLevelClass != null;

        /* We didn't find a cousin class. Look in the same package. */
        {
            TypeElement inSameMod = elements
                .getTypeElement(modCtxt, pkgCtxt.getQualifiedName().toString()
                    + '.' + packageClassText);
            if (inSameMod != null) return Collections.singletonList(inSameMod);
        }

        /* Now we look for an import. */
        {
            TypeElement imp =
                seekImport(modCtxt, topLevelClass, packageClassText);
            if (imp != null) return Collections.singletonList(imp);
        }

        /* Is the whole thing a fully qualified type name? */
        {
            List<TypeElement> type = seekType(modCtxt, orig, packageClassText);
            if (type != null) return type;
        }

        /* Look for a type in java.lang. */
        {
            List<TypeElement> type =
                seekType(modCtxt, orig, "java.lang." + packageClassText);
            if (type != null) return type;
        }

        /* Is the whole thing a fully qualified package name? */
        PackageElement pe = seekPackage(modCtxt, orig, packageClassText);
        return pe == null ? null : Collections.singletonList(pe);
    }

    private void populateWithSupers(Collection<TypeElement> set) {
        Collection<TypeElement> cands = new ArrayList<>(set);
        Collection<TypeElement> newCands = new LinkedHashSet<>();
        for (;;) {
            for (TypeElement cand : cands) {
                /* Identify supertypes and implemented interfaces. */
                Collection<TypeMirror> sts = new LinkedHashSet<>();
                {
                    TypeMirror stm = cand.getSuperclass();
                    if (stm != null && !(stm instanceof NoType)) sts.add(stm);
                }
                for (TypeMirror stm : cand.getInterfaces())
                    if (stm != null && !(stm instanceof NoType)) sts.add(stm);

                /* Convert each type mirror to a type element, and add
                 * it to the caller-supplied set. If not already present
                 * in that set, add it to the next set of candidates. */
                for (TypeMirror stm : sts) {
                    TypeElement ste = (TypeElement) types.asElement(stm);
                    if (set.add(ste)) newCands.add(ste);
                }
            }
            if (newCands.isEmpty()) return;
            cands = new ArrayList<>(newCands);
            newCands.clear();
        }
    }

    /**
     * Resolve an element signature in a context.
     * 
     * @param ctxt the context in which to resolve the reference
     * 
     * @param sigText the text to be resolved
     * 
     * @return the {@link ModuleElement}, {@link PackageElement},
     * {@link TypeElement}, {@link VariableElement} or
     * {@link ExecutableElement} representing the referenced module,
     * package, class/interface/enumeration type/annotation type, field
     * or method/constructor, respectively
     */
    public Element resolveSignature(Element ctxt, String sigText) {
        /* Parse the signature into its components. */
        Signature sig = new Signature(sigText);

        /* What module, package or class is implied by everything to the
         * left of the hash? If it makes no sense, we fail. */
        List<? extends Element> mpcs =
            getModulePackageClass(ctxt, sig.module, sig.packageClass);
        if (mpcs == null) return null;
        assert !mpcs.isEmpty();

        /* If no member (after the hash) is specified, this element must
         * be the result. */
        if (sig.member == null) return mpcs.get(0);

        /* A member is specified, so the found element must be a
         * class. */
        if (!(mpcs.get(0) instanceof TypeElement)) {
            report(Kind.WARNING, ctxt, "link.member-of-non-class", mpcs.get(0),
                   sig.member);
            return null;
        }

        /* Include supertypes and implemented interfaces in the search
         * path. */
        Collection<TypeElement> searchPath = new LinkedHashSet<>(mpcs.stream()
            .map(e -> (TypeElement) e).collect(Collectors.toList()));
        populateWithSupers(searchPath);

        if (sig.args == null) {
            /* We're looking for a simple field. */
            for (TypeElement mpc : searchPath) {
                assert mpc != null;
                assert mpc instanceof TypeElement;
                for (VariableElement field : ElementFilter
                    .fieldsIn(mpc.getEnclosedElements())) {
                    if (field.getSimpleName().contentEquals(sig.member))
                        return field;
                }
                if (sig.packageClass != null) break;
            }
            return null;
        }

        /* We're looking for a method or constructor. Work out the types
         * of each of the parameters. */
        final List<TypeMirror> resolvedParams =
            new ArrayList<>(sig.args.size());
        for (Signature.Parameter p : sig.args) {
            /* Resolve the base type into a type mirror, then add in the
             * array dimensions. */
            TypeMirror baseType = resolveType(ctxt, p.type);
            if (baseType == null) {
                /* TODO: Get a localized error message. */
                System.err.printf("Failed to resolve %s [%d]%s in %s%n", p.type,
                                  p.dims - (p.varargs ? 1 : 0),
                                  p.varargs ? "..." : "", sigText);
            }
            for (int i = 0; i < p.dims; i++)
                baseType = types.getArrayType(baseType);
            if (p.varargs) baseType = types.getArrayType(baseType);
            baseType = types.erasure(baseType);
            resolvedParams.add(baseType);
        }

        /* A member is specified, so try to find it in the identified
         * type, walking along the enclosing type elements. */
        for (TypeElement mpc : searchPath) {
            assert mpc != null;
            assert mpc instanceof TypeElement;
            /* We could be matching a constructor if this type's name
             * matches the sought member, and it has a parameter
             * list. */
            final boolean containerMemberMatch = sig.args != null &&
                sig.member.equals(mpc.getSimpleName().toString());

            /* See if we can identify a member in each of the listed
             * types. */
            next_cand: for (Element member : mpc.getEnclosedElements()) {
                /* The member name must match, or the member is a
                 * constructor and the type's name is the member we're
                 * looking for. */
                if (!member.getSimpleName().toString().equals(sig.member) &&
                    (!containerMemberMatch ||
                        member.getKind() != ElementKind.CONSTRUCTOR))
                    continue;

                /* Other aspects must match depending on the member
                 * kind. */
                switch (member.getKind()) {
                case ENUM_CONSTANT:
                case FIELD:
                    /* A field or enumeration constant matches if we're
                     * not looking for an executable member. */
                    if (resolvedParams != null) continue;
                    break;

                case CONSTRUCTOR:
                case METHOD:
                    /* A constructor or method matches if it has the
                     * same number of parameters, and all of the same
                     * types. */
                    if (resolvedParams == null) continue;
                    ExecutableElement execMember = (ExecutableElement) member;
                    if (execMember.getParameters().size()
                        != resolvedParams.size()) continue;
                    for (int i = 0; i < resolvedParams.size(); i++) {
                        TypeMirror required = resolvedParams.get(i);
                        TypeMirror got = types.erasure(execMember
                            .getParameters().get(i).asType());
                        if (!types.isSameType(required, got))
                            continue next_cand;
                    }
                    break;

                default:
                    /* Nothing else can match. */
                    continue;
                }

                /* The name and arguments match, so this is it. */
                return member;
            }
        }
        return null;
    }

    private final MacroFormatter assumedLocationFormatter =
        new MacroFormatter(DocImport.MODULE_JDK_SCHEME);

    /**
     * Get the location that the author assumes is the location of his
     * HTML embedded in a Javadoc comment.
     * 
     * @param elem the element whose documentation contains the comment
     * 
     * @return the URI of the element
     */
    public URI getAuthorAssumedLocation(Element elem) {
        if (elem == null) return locateOverview();
        Properties params = new Properties();
        setElementProperties(params, elem);
        return URI.create(assumedLocationFormatter.format(params));
    }

    /**
     * Locate the documentation for a given element. The element's
     * package is looked up in our set of known packages to obtain a
     * document mapping. The element is then passed to this mapping to
     * yield the result.
     * 
     * @param elem the sought element
     * 
     * @return the location of the element's documentation, or
     * {@code null} if not known
     */
    public URI locateElement(Element elem) {
        if (elem == null) throw new NullPointerException();
        switch (elem.getKind()) {
        case MODULE:
            ModuleElement mod = (ModuleElement) elem;
            String modName = mod.getQualifiedName().toString();
            DocReference modRef = moduleImports.get(modName);
            if (modRef == null) return null;
            Properties props = new Properties();
            setElementProperties(props, mod);
            return modRef.mapping.locate(mod);

        default:
            PackageElement pkg = elements.getPackageOf(elem);
            if (pkg == null) return null;
            String pkgName = pkg.getQualifiedName().toString();
            DocReference ref = imports.get(pkgName);
            if (ref == null) return null;
            return ref.mapping.locate(elem);
        }
    }

    /**
     * Get the internal URI of the overview page.
     * 
     * @return the URI of the overview
     * 
     * @default This returns <samp>overview-summary</samp> appended with
     * the value of {@link #hypertextFileSuffix}.
     */
    public URI locateOverview() {
        return URI.create("overview-summary" + hypertextLinkSuffix);
    }

    /**
     * Set properties identifying an element. Some of the following
     * properties will be set:
     * 
     * <dl>
     * 
     * <dt><samp>PACKAGE</samp></dt>
     * 
     * <dd>The full name of the package
     * 
     * <dt><samp>CLASS</samp></dt>
     * 
     * <dd>The name of the class relative to its package, unless a
     * package element is submitted
     * 
     * <dt><samp>ENUM</samp></dt>
     * 
     * <dd>The name of the class relative to its package, if an
     * enumeration type is submitted
     * 
     * <dt><samp>ANNOT</samp></dt>
     * 
     * <dd>The name of the class relative to its package, if an
     * annotation type is submitted
     * 
     * <dt><samp>IFACE</samp></dt>
     * 
     * <dd>The name of the class relative to its package, if an
     * interface type is submitted
     * 
     * <dt><samp>MEMBER</samp></dt>
     * 
     * <dd>The name of the member, if a member element is submitted
     * 
     * <dt><samp>FIELD</samp></dt>
     * 
     * <dd>The name of the field, if a field element is submitted
     * 
     * <dt><samp>CONSTANT</samp></dt>
     * 
     * <dd>The name of the field, if a final static field element is
     * submitted
     * 
     * <dt><samp>CONSTR</samp></dt>
     * 
     * <dd>The name of the constructor, if a constructor element is
     * submitted
     * 
     * <dt><samp>METHOD</samp></dt>
     * 
     * <dd>The name of the method, if a method element is submitted
     * 
     * <dt><samp>EXEC</samp></dt>
     * 
     * <dd>The name of a method or constructor, if such an element is
     * submitted
     * 
     * <dt><samp>PARAMETER.<var>num</var></samp></dt>
     * <dt><samp>PARAMETER.<var>num</var>.SHORT</samp></dt>
     * 
     * <dd>The erased type of parameter <var>num</var> of a submitted
     * method or constructor, excluding array dimensions
     * 
     * <p>
     * Parameters are numbered from zero. <samp>.SHORT</samp> entries
     * exclude enclosing package or class.
     * 
     * <dt><samp>PARAMETER.<var>num</var>.DIMS</samp></dt>
     * <dt><samp>PARAMETER.<var>num</var>.VARARG</samp></dt>
     * 
     * <dd>The number of array dimensions of parameter <var>num</var> of
     * a submitted method or constructor
     * 
     * <p>
     * If the method takes a variable number of arguments the
     * <samp>VARARG</samp> property is set to one fewer dimensions.
     * 
     * </dl>
     * 
     * @param props the properties to be modified
     * 
     * @param elem the element to be identified
     * 
     * @throws IllegalArgumentException if the element is not a class,
     * package, method or field
     */
    public void setElementProperties(Properties props, Element elem) {
        /* Extract parameter types. */
        switch (elem.getKind()) {
        case CONSTRUCTOR:
        case METHOD:
            ExecutableElement exec = (ExecutableElement) elem;
            List<? extends VariableElement> params = exec.getParameters();
            final int plen = params.size();
            final int vararg = exec.isVarArgs() ? plen - 1 : plen;
            for (int pos = 0; pos < plen; pos++) {
                VariableElement var = params.get(pos);

                /* Get the parameter type, and count array dimensions.
                 * Get the type's corresponding element (if any) so we
                 * can get its simple name. Finally, erase it. */
                TypeMirror varType = var.asType();
                int dims = 0;
                while (varType.getKind() == TypeKind.ARRAY) {
                    dims++;
                    varType = ((ArrayType) varType).getComponentType();
                }
                Element varTypeElement = types.asElement(varType);
                varType = types.erasure(varType);

                props.setProperty("PARAMETER." + pos,
                                  DocUtils.getLongName(varType));
                props
                    .setProperty("PARAMETER." + pos + ".SHORT",
                                 varTypeElement == null ?
                                     DocUtils.getLongName(varType) :
                                     varTypeElement.getSimpleName().toString());
                if (dims > 0) props.setProperty("PARAMETER." + pos + ".DIMS",
                                                Integer.toString(dims));

                if (pos == vararg)
                    props.setProperty("PARAMETER." + pos + ".VARARG",
                                      Integer.toString(dims - 1));
            }
            break;

        default:
            break;
        }

        /* Identify static members. */
        if (elem.getModifiers().contains(Modifier.STATIC))
            props.setProperty("STATIC", elem.getSimpleName().toString());

        /* Record details of the member, if specified. */
        switch (elem.getKind()) {
        case CONSTRUCTOR: {
            String className =
                elem.getEnclosingElement().getSimpleName().toString();
            props.setProperty("CONSTR", className);
            props.setProperty("EXEC", className);
            props.setProperty("MEMBER", className);
        }
            break;

        case METHOD: {
            String methodName = elem.getSimpleName().toString();
            props.setProperty("METHOD", methodName);
            props.setProperty("EXEC", methodName);
            props.setProperty("MEMBER", methodName);
        }
            break;

        case ENUM_CONSTANT:
        case FIELD:
            String fieldName = elem.getSimpleName().toString();
            if (elem.getModifiers()
                .containsAll(EnumSet.of(Modifier.FINAL, Modifier.STATIC)))
                props.setProperty("CONSTANT", fieldName);
            props.setProperty("FIELD", fieldName);
            props.setProperty("MEMBER", fieldName);
            elem = elem.getEnclosingElement();
            break;

        default:
            break;
        }

        /* Move to the enclosing class of a member. */
        switch (elem.getKind()) {
        case CONSTRUCTOR:
        case METHOD:
        case ENUM_CONSTANT:
        case FIELD:
            elem = elem.getEnclosingElement();
            break;

        default:
            break;
        }

        /* Record details of the class, if specified. */
        switch (elem.getKind()) {
        case CLASS:
        case ENUM:
        case INTERFACE:
        case ANNOTATION_TYPE:
            StringBuilder cls = new StringBuilder(elem.getSimpleName());
            TypeElement type = (TypeElement) elem;
            ElementKind bottom = type.getKind();
            out: do {
                Element encloser = type.getEnclosingElement();
                switch (encloser.getKind()) {
                case CLASS:
                case ENUM:
                case INTERFACE:
                case ANNOTATION_TYPE:
                    cls.insert(0, '.');
                    cls.insert(0, encloser.getSimpleName());
                    type = (TypeElement) encloser;
                    continue;

                case PACKAGE:
                    elem = encloser;
                    break out;

                default:
                    throw new AssertionError("unreachable");
                }
            } while (true);
            String clsName = cls.toString();
            props.setProperty("CLASS", clsName);
            switch (bottom) {
            case ENUM:
                props.setProperty("ENUM", clsName);
                break;

            case INTERFACE:
                props.setProperty("IFACE", clsName);
                break;

            case ANNOTATION_TYPE:
                props.setProperty("ANNOT", clsName);
                break;

            default:
                if (types.isSubtype(type.asType(), javaLangError))
                    props.setProperty("ERROR", clsName);
                else if (types.isSubtype(type.asType(),
                                         javaLangRuntimeException))
                    props.setProperty("RTEXCEPT", clsName);
                else if (types.isSubtype(type.asType(), javaLangException))
                    props.setProperty("EXCEPT", clsName);
                break;
            }
            break;

        default:
            break;
        }

        /* Record the package name. */
        switch (elem.getKind()) {
        case PACKAGE:
            props.setProperty("PACKAGE", ((PackageElement) elem)
                .getQualifiedName().toString());
            break;

        case MODULE:
        case TYPE_PARAMETER:
            break;

        default:
            throw new IllegalArgumentException("element kind unknown: "
                + elem.getKind() + " for " + elem);
        }

        /* Record the module name. */
        ModuleElement mod = elements.getModuleOf(elem);
        if (!mod.isUnnamed())
            props.setProperty("MODULE", mod.getQualifiedName().toString());
    }

    /**
     * Unflatten the content of an unknown block tag.
     * 
     * @param node the tag to unflatten
     * 
     * @return the tag's unflattened content
     */
    public final List<? extends DocTree>
        unflattenDoc(UnknownBlockTagTree node) {
        return unflattenDoc(node.getContent());
    }

    /**
     * Unflatten the content of an unknown in-line tag.
     * 
     * @param node the tag to unflatten
     * 
     * @return the tag's unflattened content
     */
    public final List<? extends DocTree>
        unflattenDoc(UnknownInlineTagTree node) {
        return unflattenDoc(node.getContent());
    }

    /**
     * Unflatten some content that Javadoc hasn't bothered to fully
     * parse, even though it had to do the work to match opening and
     * closing braces. If the supplied argument is a list of one
     * {@link TextTree}, its content is parsed as a documentation
     * comment.
     * 
     * <p>
     * This functionality is necessary because the new Javadoc doclet
     * API doesn't bother to parse the content of unknown tags, even
     * though it offers a list of parsed elements, e.g.,
     * {@link UnknownBlockTagTree#getContent()}.
     * 
     * @param orig the original documentation nodes
     * 
     * @return the first node interpreted as a text node, and reparsed;
     * else the original nodes
     */
    public final List<? extends DocTree>
        unflattenDoc(List<? extends DocTree> orig) {
        if (orig == null) return null;
        if (orig.size() != 1) return orig;
        DocTree first = orig.get(0);
        if (first instanceof TextTree)
            return unflattenDoc(((TextTree) first).getBody());
        return orig;
    }

    /**
     * Interpret flat text as a documentation comment.
     * 
     * @param text the flat text
     * 
     * @return the text interpreted as a documentation comment
     */
    public final List<? extends DocTree> unflattenDoc(String text) {
        String rawText = "<body>" + text + "</body>";
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
                return "content-" + text + ".html";
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
        DocCommentTree tree = docTrees.getDocCommentTree(file);
        return tree.getFullBody();
    }

    /**
     * Get an iteration of documentation comments over an element and
     * its enclosers.
     * 
     * @param elem the first element
     * 
     * @return an iterable over the first element, its encloser, etc,
     * ending with the overview documentation comment
     */
    public Iterable<DocWithContext> docChain(Element elem) {
        return new Iterable<DocWithContext>() {
            @Override
            public Iterator<DocWithContext> iterator() {
                return new Iterator<DocWithContext>() {
                    DocWithContext next;

                    Element chain = elem;

                    DocCommentTree overview = Configuration.this.overviewDoc;

                    boolean ensureNext() {
                        if (next != null) return true;
                        while (chain != null) {
                            Element enc = chain.getEnclosingElement();
                            DocCommentTree doc =
                                docTrees.getDocCommentTree(chain);
                            if (doc != null) {
                                next = new DocWithContext(doc, SourceContext
                                    .forElement(chain));
                                chain = enc;
                                return true;
                            }
                            chain = enc;
                        }
                        if (overview == null) return false;
                        next =
                            new DocWithContext(overview, SourceContext.EMPTY);
                        overview = null;
                        return true;
                    }

                    @Override
                    public boolean hasNext() {
                        return ensureNext();
                    }

                    @Override
                    public DocWithContext next() {
                        if (!ensureNext()) throw new NoSuchElementException();
                        DocWithContext result = next;
                        next = null;
                        return result;
                    }
                };
            }
        };
    }

    /**
     * Determine whether one element encloses another, or they are the
     * same. The inner element is recursively replaced by its encloser,
     * until it matches the candidate outer element. If either element
     * is {@code null}, the result is false.
     * 
     * @param outer the candidate outer element
     * 
     * @param inner the candidate inner element
     * 
     * @return {@code true} if the candidate outer element encloses the
     * candidate inner, or they are the same
     */
    public boolean enclosesOrSame(Element outer, Element inner) {
        if (outer == null) return false;
        if (inner == null) return false;
        if (outer == inner) return true;
        return enclosesOrSame(outer, inner.getEnclosingElement());
    }

    /**
     * Determine whether an element is an enumeration type.
     * 
     * @param elem the candidate element
     * 
     * @return true if ther erasures of the candidate element's type and
     * {@link Enum} are the same
     */
    public boolean isEnumType(Element elem) {
        return types.isSubtype(types.erasure(elem.asType()),
                               types.erasure(javaLangEnum));
    }

    /**
     * Determine whether an element is the synthetic
     * {@code Enum.valueOf(String)} method of an enumeration type. Only
     * the enclosing type, the return type and the parameter list are
     * checked.
     * 
     * @param elem the candidate element
     * 
     * @return {@code true} if the candidate element is the identified
     * method
     */
    public boolean isEnumValueOf(Element elem) {
        if (elem.getKind() != ElementKind.METHOD) return false;
        ExecutableElement execElem = (ExecutableElement) elem;
        if (!isEnumType(execElem.getEnclosingElement())) return false;
        if (!execElem.getSimpleName().toString().equals("valueOf"))
            return false;
        List<? extends VariableElement> params = execElem.getParameters();
        if (params.size() != 1) return false;
        if (!types.isSameType(javaLangString, params.get(0).asType()))
            return false;
        return true;
    }

    /**
     * Determine whether an element is the synthetic
     * {@code Enum.values()} method of an enumeration type. Only the
     * enclosing type, the return type and the parameter list are
     * checked.
     * 
     * @param elem the candidate element
     * 
     * @return {@code true} if the candidate element is the identified
     * method
     */
    public boolean isEnumValues(Element elem) {
        if (elem.getKind() != ElementKind.METHOD) return false;
        ExecutableElement execElem = (ExecutableElement) elem;
        if (!isEnumType(execElem.getEnclosingElement())) return false;
        if (!execElem.getSimpleName().toString().equals("values")) return false;
        List<? extends VariableElement> params = execElem.getParameters();
        if (!params.isEmpty()) return false;
        return true;
    }

    /**
     * Get the most specific documentation for the throwing of an
     * exception from a method or constructor.
     * 
     * @param elem the method or constructor
     * 
     * @param type the exception type
     * 
     * @return the most specific documentation of the throwing of the
     * type if present, or {@code null} otherwise
     */
    public ThrowsTree getThrowsDoc(ExecutableElement elem, TypeMirror type) {
        if (elem == null) throw new NullPointerException("elem");
        switch (elem.getKind()) {
        case METHOD:
        case CONSTRUCTOR:
            break;

        default:
            throw new IllegalArgumentException("not a method: " + elem);
        }
        DocCommentTree docs = docTrees.getDocCommentTree(elem);
        if (docs == null) return null;
        ThrowsTree bestTag = null;
        TypeMirror bestType = null;
        for (ThrowsTree tag : DocUtils.getThrowsTags(docs)) {
            Element typeElem =
                resolveSignature(elem, tag.getExceptionName().getSignature());
            if (typeElem == null) {
                /* TODO: Issue warning? Or add to set of warnings. */
                continue;
            }
            switch (typeElem.getKind()) {
            case CLASS:
                break;

            default:
                continue;
            }
            TypeMirror thrown = typeElem.asType();
            if (!types.isSubtype(type, thrown)) continue;

            if (bestType == null || types.isSubtype(thrown, bestType)) {
                bestType = thrown;
                bestTag = tag;
            }
        }
        return bestTag;
    }

    /**
     * Get the documentation for an indexed parameter of a method or
     * constructor. The name of the parameter is sought, then a
     * <code>&#64;param</code> tag for that name.
     * 
     * @param elem the method or constructor
     * 
     * @param pos the index, starting at zero
     * 
     * @return the documentation for the parameter if present, or
     * {@code null} otherwise
     */
    public ParamTree getParameterDoc(ExecutableElement elem, int pos) {
        if (elem == null) throw new NullPointerException("elem");
        switch (elem.getKind()) {
        case METHOD:
        case CONSTRUCTOR:
            break;

        default:
            throw new IllegalArgumentException("not a method: " + elem);
        }
        if (pos < 0)
            throw new IllegalArgumentException("negative position " + pos);
        DocCommentTree docs = docTrees.getDocCommentTree(elem);
        if (docs == null) return null;
        List<? extends VariableElement> params = elem.getParameters();
        if (pos >= params.size()) throw new IllegalArgumentException("element "
            + elem + " has no argument " + pos);
        VariableElement param = params.get(pos);
        String name = param.getSimpleName().toString();
        return DocUtils.findParameter(docs, name, false);
    }

    /**
     * Get the source file for an element.
     * 
     * @param elem the element whose source file is sought
     * 
     * @return the name of the source file
     */
    public Path locateSource(Element elem) {
        switch (elem.getKind()) {
        case METHOD:
        case CONSTRUCTOR:
        case FIELD:
        case ENUM_CONSTANT:
            return locateSource(elem.getEnclosingElement());

        case PACKAGE:
        case CLASS:
        case ANNOTATION_TYPE:
        case INTERFACE:
        case ENUM:
            break;

        default:
            throw new IllegalArgumentException("bad kind: " + elem.getKind());
        }

        TreePath tp = docTrees.getPath(elem);
        if (tp == null) return null;
        CompilationUnitTree cut = tp.getCompilationUnit();
        if (cut == null) return null;
        JavaFileObject cu = cut.getSourceFile();
        if (cu == null) return null;
        return Paths.get(cu.getName());
    }

    static final Pattern SLICE_PATTERN = Pattern.compile("^(?<locale>[^,]*)"
        + "(?:,(?<suffix>[^,]*)(?:,(?<charset>[^,]*))?)?$");

    /**
     * A set of properties created when the software was compiled
     */
    public static final Properties buildProperties = new Properties();

    static {
        try {
            buildProperties.load(Configuration.class
                .getResourceAsStream("dynamic.properties"));
        } catch (IOException e) {
            throw new AssertionError("unreachable", e);
        }
    }
}
