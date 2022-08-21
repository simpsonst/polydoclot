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
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;

import com.sun.source.doctree.DocCommentTree;

/**
 * Generates documentation for modules.
 * 
 * @author simpsons
 */
public class ModuleGenerator {
    private final Slice slice;
    private final Configuration config;

    /**
     * Prepare to generate documentation for all modules.
     * 
     * @param slice the slice of the documentation to generate
     */
    public ModuleGenerator(Slice slice) {
        this.slice = slice;
        this.config = this.slice.config;
    }

    /**
     * Generate the documentation for all modules.
     */
    public void run() {
        for (ModuleElement module : ElementFilter
            .modulesIn(config.env.getIncludedElements())) {
            /* Skip explicitly excluded modules, or report that we're
             * working on them. */
            if (module.isUnnamed()) continue;
            if (config.excludedElements.contains(module)) continue;
            config.diagnostic("output.module.item",
                              module.getQualifiedName());

            /* Work out where this module's documentation goes, both
             * internally and externally. */
            final URI moduleLocation = config.locateElement(module);
            final Path moduleFile = config.outputDirectory.resolve(module
                .getQualifiedName().toString().replace('.', '$') + "-module"
                + config.hypertextFileSuffix);
            final OutputContext blockCtxt =
                slice.getBlockContext(moduleLocation, module);

            /* Set properties for the module */
            Properties moduleProps = new Properties();
            config.setElementProperties(moduleProps, module);
            moduleProps
                .setProperty("TITLE.SHORT",
                             slice.toHypertext(SourceContext.EMPTY, slice
                                 .getPlainContext(moduleLocation, module),
                                               config.shortTitle));
            final SourceContext inCtxt = SourceContext.forElement(module);

            DocCommentTree docs = config.docTrees.getDocCommentTree(module);
            try (PrintWriter out = slice.openHypertextFile(moduleFile)) {
                out.printf("<html lang=\"%s\">\n",
                           slice.spec.locale.toString().replaceAll("_", "-"));
                out.printf("<head>\n");
                slice.writeHypertextMeta(out, blockCtxt);
                out.printf("<title>%s</title>\n",
                           slice.macroFormat("page.title.package",
                                             moduleProps,
                                             blockCtxt.escaper()));
                slice.writeSummaryAsHypertextDescription(out::append,
                                                         blockCtxt
                                                             .inAttribute(),
                                                         module);
                config.writeStyleLinks(out, blockCtxt);
                out.printf("</head>\n");
                out.printf("<body class=\"javadoc package\">\n");

                slice.writeUserSection(out, blockCtxt, "forematter",
                                       "forematter", module);
                out.printf("<div class=\"javadoc-matter\">\n");

                out.printf("<div class=\"javadoc-head\">\n");
                out.printf("<h1><span>%s</span></h1>\n",
                           slice.toHypertext(inCtxt, blockCtxt, slice
                               .macroFormatDoc("page.heading.module",
                                               blockCtxt.escaper()
                                                   .escape(moduleProps))));
                out.printf("<div class=\"javadoc-purpose\"><span>");
                slice.writeSummaryOrSyntheticDescription(out::append,
                                                         blockCtxt, inCtxt,
                                                         module);
                out.printf("</span></div>\n");

                slice.writeElementQualities(out, blockCtxt, module);
                out.printf("</div>\n");

                /* Write applied annotations. */
                List<? extends AnnotationMirror> annots =
                    DocUtils.getDocumentedAnnotationMirrors(module);
                if (!annots.isEmpty()) {
                    out.printf("<div class=\"javadoc-context\">\n");
                    out.printf("<pre class=\"java\">\n");
                    slice.writeAnnotationMirrors(out, blockCtxt, annots, "",
                                                 2);
                    out.print("module ");
                    out.print(blockCtxt.escape(module.getQualifiedName()));
                    out.print(";");
                    out.printf("</pre>\n");
                    out.printf("</div>\n");
                }

                out.printf("<div class=\"javadoc-description\">\n");
                if (DocUtils.hasDescription(docs, false)) {
                    out.printf("<h2><span>%s</span></h2>\n", blockCtxt
                        .escape(slice
                            .getContent("section.heading.module.description")));

                    out.printf("<div class=\"body\">\n");
                    out.printf("<p>%s\n", slice
                        .toHypertext(inCtxt, blockCtxt, docs.getFullBody()));
                    out.printf("</div>\n");
                }
                slice.writeSeeSection(out, inCtxt, blockCtxt, docs);
                out.printf("</div>\n");

                /* TODO: List packages. */

                slice.writeAuthorSection(out, inCtxt, blockCtxt, docs);

                out.printf("</div>\n");
                slice.writeUserSection(out, blockCtxt, "aftmatter",
                                       "aftmatter", module);

                out.printf("</body>\n");
                out.printf("</html>\n");
            } catch (IOException e1) {
                config.report(Kind.ERROR, module,
                              "output.module.failure.write", e1.getMessage(),
                              moduleFile);
                return;
            }
        }
    }
}
