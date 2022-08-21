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

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import jdk.javadoc.doclet.Reporter;
import uk.ac.lancs.polydoclot.util.HypertextEscaper;

/**
 * Generates the overview page listing modules and packages, and
 * providing any over-all distribution documentation.
 * 
 * @author simpsons
 */
public final class OverviewGenerator {
    private final Slice slice;

    private final Configuration config;

    private final Reporter reporter;

    /**
     * Prepare to generate the overview page.
     * 
     * @param slice the slice to be generated
     */
    public OverviewGenerator(Slice slice) {
        this.slice = slice;
        this.config = this.slice.config;
        this.reporter = this.config.reporter;
    }

    /**
     * Generate the overview page.
     */
    public void run() {
        config.diagnostic("output.overview");

        /* TODO: Delete this block. */
        if (false) {
            /* Invert the mapping from group to its module members. */
            Map<String, Object> moduleToGroup = new HashMap<>();
            for (Map.Entry<Object,
                           Collection<String>> entry : config.moduleGroupings
                               .entrySet()) {
                for (String name : entry.getValue())
                    moduleToGroup.put(name, entry.getKey());
            }

            /* Identify all modules to be documented. */
            Map<Object, List<ModuleElement>> groupedModules = new HashMap<>();
            List<ModuleElement> nakedModules = new ArrayList<>();
            for (ModuleElement mod : ElementFilter
                .modulesIn(config.env.getIncludedElements())) {
                /* We never document the unnamed module, even if the
                 * packages being documented are effectively partially
                 * defining it. */
                if (mod.isUnnamed()) continue;

                /* Skip modules explicitly marked as not to be
                 * documented. This step could be redundant, as modules
                 * and packages should never really be
                 * marked @undocumented. */
                if (config.excludedElements.contains(mod)) continue;

                /* Decide which group this module belongs in. */
                Object groupKey =
                    moduleToGroup.get(mod.getQualifiedName().toString());
                if (groupKey == null)
                    nakedModules.add(mod);
                else
                    groupedModules
                        .computeIfAbsent(groupKey, k -> new ArrayList<>())
                        .add(mod);
            }

            /* Invert the mapping from group to its package members. */
            Map<String, Object> packageToGroup = new HashMap<>();
            for (Map.Entry<Object,
                           Collection<String>> entry : config.packageGroupings
                               .entrySet()) {
                for (String name : entry.getValue())
                    packageToGroup.put(name, entry.getKey());
            }

            /* Identify packages not belonging to modules, and put them
             * into the appropriate groups if necessary. Packages
             * belonging to documented modules don't need to be listed
             * here, because they will be listed in their respective
             * module's documentation. */
            Map<Object, List<PackageElement>> groupedPackages = new HashMap<>();
            List<PackageElement> nakedPackages = new ArrayList<>();
            for (PackageElement pkgDef : ElementFilter
                .packagesIn(config.env.getIncludedElements())) {
                /* Skip packages explicitly marked as not to be
                 * documented. This step could be redundant, as modules
                 * and packages should never really be
                 * marked @undocumented. */
                if (config.excludedElements.contains(pkgDef)) continue;

                /* Skip packages belonging to a named module, whose
                 * documentation will list it separately. */
                ModuleElement mod = config.elements.getModuleOf(pkgDef);
                if (mod != null && !mod.isUnnamed()) continue;

                /* Decide which group this package belongs in. */
                Object groupKey =
                    packageToGroup.get(pkgDef.getQualifiedName().toString());
                if (groupKey == null)
                    nakedPackages.add(pkgDef);
                else
                    groupedPackages
                        .computeIfAbsent(groupKey, k -> new ArrayList<>())
                        .add(pkgDef);
            }
        }

        List<? extends DocTree> anonGroupTitle =
            slice.getTreeContent("group.unnamed"
                + (config.groupedPackages.size() == 1 ? ".sole" : ""));

        final Path ovFile = config.outputDirectory.resolve("overview-summary"
            + config.hypertextFileSuffix + slice.spec.suffix);
        final URI ovLoc = config.outputDirectory.toUri()
            .resolve("overview-summary" + config.hypertextLinkSuffix);
        final OutputContext blockCtxt = OutputContext
            .forBlock(config.types,
                      ovLoc,
                      null,
                      HypertextEscaper.forCData(slice.spec.charset),
                      HypertextEscaper.forAttributes(slice.spec.charset));
        final SourceContext inCtxt = SourceContext.EMPTY;

        List<? extends DocTree> summary = null;
        if (config.overviewDoc != null) {
            /* Try to find a @resume tag. */
            if (summary == null || summary.isEmpty()) {
                for (DocTree tag : config.overviewDoc.getBlockTags()) {
                    if (tag.getKind() != DocTree.Kind.UNKNOWN_BLOCK_TAG)
                        continue;
                    UnknownBlockTagTree ubtt = (UnknownBlockTagTree) tag;
                    if (!ubtt.getTagName().equals("summary") &&
                        !ubtt.getTagName().equals("resume")) continue;
                    summary = ubtt.getContent();
                    break;
                }
            }

            /* Use the first sentence if provided. */
            if (summary == null || summary.isEmpty()) {
                summary = config.overviewDoc.getFirstSentence();
            }
        }

        try (PrintWriter out = slice.openHypertextFile(ovFile)) {
            out.printf("<html lang=\"%s\">\n",
                       slice.spec.locale.toString().replaceAll("_", "-"));
            out.printf("<head>\n");
            slice.writeHypertextMeta(out, blockCtxt, false);
            out.print("<title>");
            slice.toHypertext(out::append,
                              inCtxt,
                              blockCtxt.forPureCharacterData(),
                              config.title);
            out.print("</title>\n");
            if (summary != null && !summary.isEmpty()) {
                out.printf("<meta name=\"description\" content=\"");
                slice.toHypertext(out::append,
                                  inCtxt,
                                  blockCtxt.inAttribute(),
                                  summary);
                out.printf("\">\n");
            }
            config.writeStyleLinks(out, blockCtxt);
            out.printf("</head>\n");
            out.printf("<body class=\"javadoc overview\">\n");

            slice.writeUserSection(out,
                                   blockCtxt,
                                   "forematter",
                                   "forematter",
                                   null);
            out.printf("<div class=\"javadoc-matter\">\n");

            out.printf("<div class=\"javadoc-head\">\n");
            out.print("<h1><span>");
            slice.toHypertext(out::append, inCtxt, blockCtxt, config.title);
            out.print("</span></h1>\n");

            /* Write out the summary if provided. */
            if (summary != null && !summary.isEmpty()) {
                out.printf("<div class=\"javadoc-purpose\">");
                slice.toHypertext(out::append, inCtxt, blockCtxt, summary);
                out.printf("</div>\n");
            }
            out.printf("</div>\n");

            if (!config.groupedPackages.isEmpty() ||
                !config.groupedModules.isEmpty()) {
                out.print("<table class=\"javadoc-package-list\" summary=\"");
                slice
                    .toHypertext(out::append,
                                 SourceContext.EMPTY,
                                 blockCtxt.inAttribute(),
                                 slice
                                     .getTreeContent("package-list.table-summary"));
                out.print("\">\n");
                if (false) out
                    .printf("<caption>%s</caption>\n",
                            blockCtxt.escape(slice
                                .getContent("section.heading.overview.modules-packages")));
                for (Map.Entry<Object,
                               List<? extends DocTree>> entry : config.groupTitles
                                   .entrySet()) {
                    List<? extends DocTree> title = entry.getValue();
                    if (title == null) title = anonGroupTitle;
                    List<PackageElement> pkgList =
                        new ArrayList<>(config.groupedPackages
                            .getOrDefault(entry.getKey(),
                                          Collections.emptySet()));
                    List<ModuleElement> modList =
                        new ArrayList<>(config.groupedModules
                            .getOrDefault(entry.getKey(),
                                          Collections.emptySet()));
                    if (pkgList.isEmpty() && modList.isEmpty()) continue;
                    pkgList.sort((a, b) -> String.CASE_INSENSITIVE_ORDER
                        .compare(a.getQualifiedName().toString(),
                                 b.getQualifiedName().toString()));
                    modList.sort((a, b) -> String.CASE_INSENSITIVE_ORDER
                        .compare(a.getQualifiedName().toString(),
                                 b.getQualifiedName().toString()));
                    out.print("<tbody>\n");
                    out.print("<tr class=\"heading\">");
                    out.print("<th colspan=\"3\">");
                    slice.toHypertext(out::append, inCtxt, blockCtxt, title);
                    out.print("</th>\n");
                    out.print("</tr>\n");
                    boolean even = true;
                    for (PackageElement pkgDef : pkgList) {
                        SourceContext srcCtxt =
                            SourceContext.forElement(pkgDef);
                        out.printf("<tr class=\"package item %s%s\">\n",
                                   even ? "even" : "odd",
                                   config.deprecatedElements
                                       .containsKey(pkgDef) ? " deprecated" :
                                           "");
                        even = !even;
                        out.printf("<td class=\"kind\">%s</td>\n",
                                   blockCtxt.escape(slice
                                       .getContent("module-package.kind.package")));
                        out.print("<td class=\"name\">");
                        slice.writeElementReference(out::append,
                                                    blockCtxt,
                                                    pkgDef,
                                                    LinkContent.NORMAL);
                        out.print("</td>\n");
                        out.print("<td class=\"purpose\">");
                        if (!slice.writeSummary(out::append, blockCtxt, pkgDef))
                            slice
                                .toHypertext(out::append,
                                             srcCtxt,
                                             blockCtxt,
                                             slice
                                                 .getSyntheticDescription(pkgDef));

                        out.print("</td>\n");
                        out.print("</tr>\n");
                    }
                    for (ModuleElement mod : modList) {
                        SourceContext srcCtxt = SourceContext.forElement(mod);
                        out.printf("<tr class=\"module %s\">\n",
                                   even ? "even" : "odd");
                        even = !even;
                        out.printf("<td class=\"kind\">%s</td>\n",
                                   blockCtxt.escape(slice
                                       .getContent("module-package.kind.module")));
                        out.print("<td class=\"name\">");
                        slice.writeElementReference(out::append,
                                                    blockCtxt,
                                                    mod,
                                                    LinkContent.NORMAL);
                        out.print("</td>\n");
                        out.print("<td class=\"purpose\">");
                        if (!slice.writeSummary(out::append, blockCtxt, mod))
                            slice
                                .toHypertext(out::append,
                                             srcCtxt,
                                             blockCtxt,
                                             slice
                                                 .getSyntheticDescription(mod));

                        out.print("</td>\n");
                        out.print("</tr>\n");
                    }
                    out.print("</tbody>\n");
                }
                out.print("</table>\n");
            }

            out.printf("<div class=\"javadoc-description\">\n");
            if (DocUtils.hasDescription(config.overviewDoc, false)) {
                out.printf("<h2><span>%s</span></h2>\n",
                           blockCtxt.escape(slice
                               .getContent("section.heading.overview.description")));

                out.printf("<div class=\"body\">\n");
                out.printf("<p>%s\n",
                           slice.toHypertext(inCtxt,
                                             blockCtxt,
                                             config.overviewDoc.getFullBody()));
                out.printf("</div>\n");
            }
            slice.writeSeeSection(out, inCtxt, blockCtxt, config.overviewDoc);
            out.printf("</div>\n");

            {
                /* Identify the roots of the hierarchy, i.e., those
                 * types not listed as subtypes. */
                Collection<TypeElement> hierRoots = new HashSet<>(ElementFilter
                    .typesIn(config.env.getIncludedElements()));
                hierRoots.removeAll(config.excludedElements);
                for (Collection<TypeElement> subs : config.knownDirectSubtypes
                    .values())
                    hierRoots.removeAll(subs);
                slice.writeClassHierarchy(out, blockCtxt, hierRoots);
            }

            if (!config.referencedAuthors.isEmpty()) {
                out.print("<div class=\"javadoc-authors\">\n");
                out.printf("<h2><span>%s</span></h2>\n",
                           blockCtxt.escape(slice
                               .getContent("section.heading.authors")));
                out.print("<div class=\"definitions\">\n");
                for (String authorKey : config.referencedAuthors)
                    slice.writeAuthor(out,
                                      inCtxt,
                                      blockCtxt,
                                      config.authors.get(authorKey));
                out.print("</div>\n");
                out.print("</div>\n");
            }

            if (true && config.deprecatedElements.size() > 0) {
                out.printf("<div class=\"deprecated\">");
                out.printf("<h2><span>%s</span></h2>\n",
                           blockCtxt.escape(slice
                               .getContent("section.heading.deprecated")));
                List<Element> elems = config.deprecatedElements.entrySet()
                    .stream().filter(e -> e.getValue().isExplicit())
                    .map(Map.Entry::getKey).sorted(this::compareElements)
                    .collect(Collectors.toList());

                // List<Element> elems =
                // new ArrayList<>(config.deprecatedElements);
                Collections.sort(elems, this::compareElements);
                out.printf("<ul>\n");
                for (Element elem : elems) {
                    out.print("\n<li>");
                    slice.writeElementReference(out::append,
                                                blockCtxt,
                                                elem,
                                                LinkContent.NORMAL);
                }
                out.printf("\n</ul>\n");
                out.printf("</div>\n");
            }

            if (config.listUndocumented &&
                slice.countUndocumentedElements() > 0) {
                out.printf("<div class=\"javadoc-undocumented\">\n");
                out.printf("<h2><span>%s</span></h2>\n",
                           blockCtxt.escape(slice
                               .getContent("section.heading.undocumented")));
                List<Element> elems = new ArrayList<>();
                slice.getUndocumentedElements(elems);
                Collections.sort(elems, this::compareElements);
                out.printf("<ul>\n");
                for (Element elem : elems) {
                    out.print("\n<li>");
                    slice.writeElementReference(out::append,
                                                blockCtxt,
                                                elem,
                                                LinkContent.NORMAL);
                }
                out.printf("</ul>\n");
                out.printf("\n</div>\n");
            }

            out.printf("</div>\n");

            slice.writeUserSection(out,
                                   blockCtxt,
                                   "aftmatter",
                                   "aftmatter",
                                   null);

            out.printf("</body>\n");
            out.printf("</html>\n");
        } catch (FileNotFoundException e) {
            reporter.print(Kind.ERROR, "file not found: " + ovFile);
            return;
        } catch (IOException e1) {
            reporter.print(Kind.ERROR,
                           "I/O error (" + e1.getMessage() + "): " + ovFile);
            return;
        }
    }

    private static TypeElement typeOf(Element elem) {
        switch (elem.getKind()) {
        case CLASS:
        case ENUM:
        case INTERFACE:
        case ANNOTATION_TYPE:
            return (TypeElement) elem;

        default:
            return (TypeElement) elem.getEnclosingElement();
        }
    }

    private int compareElements(Element a, Element b) {
        {
            ModuleElement ax = config.elements.getModuleOf(a);
            ModuleElement bx = config.elements.getModuleOf(b);
            int diff = String.CASE_INSENSITIVE_ORDER
                .compare(ax.getQualifiedName().toString(),
                         bx.getQualifiedName().toString());
            if (diff != 0) return diff;
            if (a == ax) {
                if (b != bx) return -1;
            } else if (b == bx) {
                return +1;
            }
        }

        {
            PackageElement ax = config.elements.getPackageOf(a);
            PackageElement bx = config.elements.getPackageOf(b);
            int diff = String.CASE_INSENSITIVE_ORDER
                .compare(ax.getQualifiedName().toString(),
                         bx.getQualifiedName().toString());
            if (diff != 0) return diff;
            if (a == ax) {
                if (b != bx) return -1;
            } else if (b == bx) {
                return +1;
            }
        }

        {
            TypeElement ax = typeOf(a);
            TypeElement bx = typeOf(b);
            int diff = String.CASE_INSENSITIVE_ORDER
                .compare(ax.getQualifiedName().toString(),
                         bx.getQualifiedName().toString());
            if (diff != 0) return diff;
            if (a == ax) {
                if (b != bx) return -1;
            } else if (b == bx) {
                return +1;
            }
        }

        {
            int diff = String.CASE_INSENSITIVE_ORDER
                .compare(a.getSimpleName().toString(),
                         b.getSimpleName().toString());
            if (diff != 0) return diff;
        }

        return 0;
    }
}
