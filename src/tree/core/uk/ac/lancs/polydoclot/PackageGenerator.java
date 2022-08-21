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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;

import com.sun.source.doctree.DocCommentTree;

import jdk.javadoc.doclet.DocletEnvironment;
import uk.ac.lancs.polydoclot.util.Escaper;
import uk.ac.lancs.polydoclot.util.HypertextEscaper;

/**
 * Generates documentation for packages in a slice.
 * 
 * @author simpsons
 */
public final class PackageGenerator {
    private final Slice slice;
    private final Configuration config;
    private final DocletEnvironment env;

    /**
     * Prepare to generate documentation.
     * 
     * @param the slice to be generated
     */
    public PackageGenerator(Slice ctxt) {
        this.slice = ctxt;
        this.config = this.slice.config;
        this.env = this.config.env;
    }

    /**
     * Generate documentation.
     */
    public void run() {
        for (PackageElement pkgDef : ElementFilter
            .packagesIn(env.getIncludedElements())) {
            if (config.excludedElements.contains(pkgDef)) continue;

            Name pkgName = pkgDef.getQualifiedName();
            config.diagnostic("output.package.item", pkgName);
            final URI pkgLoc = config.locateElement(pkgDef);
            final Path pkgFile;

            {
                /* Get the directory for this package. */
                Path pos = config.outputDirectory;
                for (String component : pkgDef.getQualifiedName().toString()
                    .split("\\.")) {
                    pos = pos.resolve(component);
                }
                pos = pos.resolve("package-summary"
                    + config.hypertextFileSuffix + slice.spec.suffix);
                pkgFile = pos;
            }

            DocCommentTree pkgDoc = config.docTrees.getDocCommentTree(pkgDef);

            try {
                Files.createDirectories(pkgFile.getParent());
            } catch (IOException e1) {
                config.report(Kind.ERROR, pkgDef,
                              "output.package.failure.mkdir", e1.getMessage(),
                              pkgFile);
                return;
            }

            final Escaper escaper =
                HypertextEscaper.forCData(slice.spec.charset);
            final Escaper attrEscaper =
                HypertextEscaper.forAttributes(slice.spec.charset);
            final OutputContext blockCtxt = OutputContext
                .forBlock(config.types, pkgLoc, pkgDef, escaper, attrEscaper);
            final SourceContext pkgCtxt = SourceContext.forElement(pkgDef);

            /* Set properties for this element. */
            Properties pkgDefProps = new Properties();
            config.setElementProperties(pkgDefProps, pkgDef);
            pkgDefProps
                .setProperty("TITLE.SHORT", slice
                    .toHypertext(SourceContext.EMPTY,
                                 OutputContext.plain(config.types, pkgLoc,
                                                     pkgDef),
                                 config.shortTitle));

            /* Write out the file. */
            try (PrintWriter out = slice.openHypertextFile(pkgFile)) {
                out.printf("<html lang=\"%s\">\n",
                           slice.spec.locale.toString().replaceAll("_", "-"));
                out.printf("<head>\n");
                slice.writeHypertextMeta(out, blockCtxt);
                slice.writeElementMetaLink("index", out::append, blockCtxt,
                                           config.elements
                                               .getModuleOf(pkgDef));
                out.printf("<title>%s</title>\n",
                           slice.macroFormat("page.title.package",
                                             pkgDefProps,
                                             blockCtxt.escaper()));
                slice.writeSummaryAsHypertextDescription(out::append,
                                                         blockCtxt
                                                             .inAttribute(),
                                                         pkgDef);
                config.writeStyleLinks(out, blockCtxt);
                out.printf("</head>\n");
                out.printf("<body class=\"javadoc package\">\n");

                slice.writeUserSection(out, blockCtxt, "forematter",
                                       "forematter", pkgDef);
                out.printf("<div class=\"javadoc-matter\">\n");

                out.printf("<div class=\"javadoc-head\">\n");
                out.printf("<h1><span>%s</span></h1>\n",
                           slice.toHypertext(pkgCtxt, blockCtxt, slice
                               .macroFormatDoc("page.heading.package",
                                               blockCtxt.escaper()
                                                   .escape(pkgDefProps))));
                out.printf("<div class=\"javadoc-purpose\"><span>");
                slice.writeSummaryOrSyntheticDescription(out::append,
                                                         blockCtxt, pkgCtxt,
                                                         pkgDef);
                out.printf("</span></div>\n");

                slice.writeElementQualities(out, blockCtxt, pkgDef);
                out.printf("</div>\n");

                /* Write applied annotations. */
                List<? extends AnnotationMirror> annots =
                    DocUtils.getDocumentedAnnotationMirrors(pkgDef);
                if (!annots.isEmpty()) {
                    out.printf("<div class=\"javadoc-context\">\n");
                    out.printf("<pre class=\"java\">\n");
                    slice.writeAnnotationMirrors(out, blockCtxt, annots, "",
                                                 2);
                    out.print("package ");
                    out.print(blockCtxt.escape(pkgDef.getQualifiedName()));
                    out.print(";");
                    out.printf("</pre>\n");
                    out.printf("</div>\n");
                }

                out.printf("<div class=\"javadoc-description\">\n");
                if (DocUtils.hasDescription(pkgDoc, false)) {
                    out.printf("<h2><span>%s</span></h2>\n", escaper
                        .escape(slice
                            .getContent("section.heading.package.description")));
                    out.printf("<div class=\"body\">\n");
                    out.print("<p>");
                    slice.toHypertext(out::append, pkgCtxt, blockCtxt,
                                      pkgDoc.getFullBody());
                    out.print("\n");
                    out.printf("</div>\n");
                }
                slice.writeSeeSection(out, pkgCtxt, blockCtxt, pkgDoc);
                out.printf("</div>\n");

                /* List the classes to be documented. Recursively
                 * investigate nested classes. */
                List<TypeElement> classList = new ArrayList<>(ElementFilter
                    .typesIn(pkgDef.getEnclosedElements()));
                List<TypeElement> containers = classList;
                for (;;) {
                    List<TypeElement> additionals = new ArrayList<>();
                    for (TypeElement container : containers)
                        additionals.addAll(ElementFilter
                            .typesIn(container.getEnclosedElements()));
                    if (additionals.isEmpty()) break;
                    classList.addAll(additionals);
                    containers = additionals;
                }

                /* Eliminate excluded classes, and sort. */
                classList.retainAll(env.getIncludedElements());
                classList.removeAll(config.excludedElements);
                classList.sort(TYPE_ORDER);

                if (false) {
                    /* Split out the checked and unchecked
                     * exceptions. */
                    List<TypeElement> checkedList = new ArrayList<>();
                    List<TypeElement> uncheckedList = new ArrayList<>();
                    for (Iterator<TypeElement> iter =
                        classList.iterator(); iter.hasNext();) {
                        TypeElement cur = iter.next();
                        if (config.types.isSubtype(cur.asType(),
                                                   config.javaLangError)
                            || config.types
                                .isSubtype(cur.asType(),
                                           config.javaLangRuntimeException)) {
                            iter.remove();
                            uncheckedList.add(cur);
                            continue;
                        }
                        if (config.types
                            .isSubtype(cur.asType(),
                                       config.javaLangException)) {
                            iter.remove();
                            checkedList.add(cur);
                            continue;
                        }
                    }
                }

                out.print("<table class=\"javadoc-class-list\" summary=\"");
                slice
                    .toHypertext(out::append, SourceContext.EMPTY,
                                 blockCtxt.inAttribute(), slice
                                     .getTreeContent("class-list.table-summary"));
                out.print("\">\n");
                out.printf("<thead>\n");
                out.printf("<tr><th class=\"kind\">%s</th>"
                    + " <th class=\"name\">%s</th>"
                    + " <th class=\"purpose\">%s</th></tr>\n",
                           blockCtxt.escape(slice
                               .getContent("class-list.heading.kind")),
                           blockCtxt.escape(slice
                               .getContent("class-list.heading.type")),
                           blockCtxt.escape(slice
                               .getContent("class-list.heading.purpose")));
                out.printf("</thead>\n");
                out.printf("<tbody>\n");
                boolean even = true;
                for (TypeElement memb : classList) {
                    SourceContext membCtxt = SourceContext.forElement(memb);
                    out.printf("<tr class=\"item %s%s\">\n",
                               even ? "even" : "odd",
                               config.deprecatedElements.containsKey(memb)
                                   ? " deprecated" : "");
                    even = !even;

                    out.printf("<td class=\"kind\">");
                    String kindKey = "unknown";
                    switch (memb.getKind()) {
                    case INTERFACE:
                        kindKey = "iface";
                        break;
                    case ENUM:
                        kindKey = "enum";
                        break;
                    case ANNOTATION_TYPE:
                        kindKey = "annot";
                        break;
                    case CLASS:
                        if (config.types.isSubtype(memb.asType(),
                                                   config.javaLangError))
                            kindKey = "error";
                        else if (config.types
                            .isSubtype(memb.asType(),
                                       config.javaLangRuntimeException))
                            kindKey = "exception.unchecked";
                        else if (config.types
                            .isSubtype(memb.asType(),
                                       config.javaLangException))
                            kindKey = "exception.checked";
                        else
                            kindKey = "class";
                        break;
                    default:
                        break;
                    }
                    slice.toHypertext(out::append, SourceContext.EMPTY,
                                      blockCtxt,
                                      slice.getTreeContent("class.kind."
                                          + kindKey));
                    out.printf("</td>\n");

                    out.printf("<td class=\"name\">");
                    slice.writeElementReference(out::append, blockCtxt, memb,
                                                LinkContent.NORMAL);
                    out.printf("</td>\n");

                    out.printf("<td class=\"purpose\">");
                    if (!slice.writeSummary(out::append, blockCtxt, memb))
                        slice
                            .toHypertext(out::append, membCtxt, blockCtxt,
                                         slice.getSyntheticDescription(memb));

                    out.printf("</td>\n");

                    out.printf("</tr>\n");
                }
                out.printf("</tbody>\n");
                out.printf("</table>\n");

                slice.writeAuthorSection(out, pkgCtxt, blockCtxt, pkgDoc);

                out.printf("</div>\n");
                slice.writeUserSection(out, blockCtxt, "aftmatter",
                                       "aftmatter", pkgDef);

                out.printf("</body>\n");
                out.printf("</html>\n");
            } catch (IOException e1) {
                config.report(Kind.ERROR, pkgDef,
                              "output.package.failure.write", e1.getMessage(),
                              pkgFile);
                return;
            }

            do {
                Path srcFile = config.locateSource(pkgDef);
                if (srcFile == null) break;
                Path staticsIn = srcFile.getParent().resolve("doc-files");
                if (!Files.isDirectory(staticsIn)) break;
                Path staticsOut = pkgFile.getParent().resolve("doc-files");
                try {
                    System.err.printf("Copying files...%n");
                    FileVisitor<Path> action = new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult
                            visitFile(Path file, BasicFileAttributes attrs)
                                throws IOException {
                            Path out = staticsOut
                                .resolve(staticsIn.relativize(file));
                            try {
                                Files.createDirectories(out.getParent());
                                Files
                                    .copy(file, out,
                                          StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                config.report(Kind.ERROR,
                                              "doc-files.failure.io",
                                              e.getMessage(), pkgDef);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    };
                    Files.walkFileTree(staticsIn, action);
                } catch (IOException e) {
                    config.report(Kind.ERROR, "doc-files.failure.io",
                                  e.getMessage(), pkgDef);
                }
            } while (false);
        }
    }

    private static final Comparator<TypeElement> TYPE_ORDER =
        (a, b) -> String.CASE_INSENSITIVE_ORDER
            .compare(a.getQualifiedName().toString(),
                     b.getQualifiedName().toString());
}
