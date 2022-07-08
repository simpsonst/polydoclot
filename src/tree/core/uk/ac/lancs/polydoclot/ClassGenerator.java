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

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import jdk.javadoc.doclet.DocletEnvironment;
import uk.ac.lancs.polydoclot.util.Escaper;
import uk.ac.lancs.polydoclot.util.HypertextEscaper;

/**
 * Generates documentation for class files and their members.
 * 
 * @author simpsons
 */
public final class ClassGenerator {
    private final Slice slice;

    private final Configuration config;

    private final DocletEnvironment env;

    /**
     * Create a class generator for a slice.
     * 
     * @param slice the slice to be generated
     */
    public ClassGenerator(Slice slice) {
        this.slice = slice;
        this.config = this.slice.config;
        this.env = this.config.env;
    }

    /**
     * Generate documentation for all classes.
     */
    public void run() {
        for (TypeElement typeDef : ElementFilter
            .typesIn(env.getIncludedElements())) {
            if (config.excludedElements.contains(typeDef)) continue;

            /* Find out where to write the file documenting this class,
             * what its URI is, what to call it when the package context
             * is already known. */
            final String leafName, shortName;
            final PackageElement pkg;
            final Path typeFile;
            final URI typeLoc = config.locateElement(typeDef);
            {
                StringBuilder leafNameBuf =
                    new StringBuilder(typeDef.getSimpleName());
                Element container = typeDef;
                for (;;) {
                    container = container.getEnclosingElement();
                    if (container.getKind() == ElementKind.PACKAGE) break;
                    leafNameBuf.insert(0, '.');
                    leafNameBuf.insert(0, container.getSimpleName());
                }
                shortName = leafNameBuf.toString();
                leafName = shortName.replace('.', '$');

                assert container.getKind() == ElementKind.PACKAGE;
                pkg = (PackageElement) container;

                /* Get the directory for this package. */
                Path pos = config.outputDirectory;
                for (String component : pkg.getQualifiedName().toString()
                    .split("\\.")) {
                    pos = pos.resolve(component);
                }

                /* Append the full class name with the type and slice
                 * suffixes. */
                typeFile = pos.resolve(leafName + config.hypertextFileSuffix
                    + slice.spec.suffix);
            }

            DocCommentTree typeDoc = config.docTrees.getDocCommentTree(typeDef);

            config.diagnostic("output.class.item", typeDef);

            try {
                Files.createDirectories(typeFile.getParent());
            } catch (IOException ex) {
                config.report(Kind.ERROR, typeDef,
                              "output.class.failure.mkdirs", ex, typeFile);
                return;
            }

            final Escaper escaper =
                HypertextEscaper.forCData(slice.spec.charset);
            final Escaper attrEscaper =
                HypertextEscaper.forAttributes(slice.spec.charset);
            final OutputContext blockContext = OutputContext
                .forBlock(config.types, typeLoc, typeDef, escaper, attrEscaper);
            final SourceContext typeContext = SourceContext.forElement(typeDef);

            /* Set properties for this element. */
            Properties typeDefProps = new Properties();
            config.setElementProperties(typeDefProps, typeDef);
            typeDefProps
                .setProperty("TITLE.SHORT", slice
                    .toHypertext(SourceContext.EMPTY,
                                 OutputContext.plain(config.types, typeLoc,
                                                     typeDef),
                                 config.shortTitle));

            /* Write out the file. */
            try (PrintWriter out = slice.openHypertextFile(typeFile)) {
                out.printf("<html lang=\"%s\">\n",
                           slice.spec.locale.toString().replaceAll("_", "-"));
                out.printf("<head>\n");
                slice.writeHypertextMeta(out, blockContext);
                slice
                    .writeElementMetaLink("index", out::append, blockContext,
                                          config.elements.getModuleOf(typeDef));
                do {
                    Element enc = typeDef.getEnclosingElement();
                    if (enc == null) break;
                    switch (enc.getKind()) {
                    case ENUM:
                    case CLASS:
                    case INTERFACE:
                    case ANNOTATION_TYPE:
                        slice.writeElementMetaLink("index up", out::append,
                                                   blockContext, enc);
                        slice.writeElementMetaLink("index", out::append,
                                                   blockContext, config.elements
                                                       .getPackageOf(typeDef));
                        break;

                    case PACKAGE:
                        slice.writeElementMetaLink("index up", out::append,
                                                   blockContext, enc);
                        break;

                    default:
                    }
                } while (false);
                out.printf("<title>%s</title>\n",
                           slice.macroFormat("page.title.class", typeDefProps,
                                             blockContext.escaper()));
                slice.writeSummaryAsHypertextDescription(out::append,
                                                         blockContext
                                                             .inAttribute(),
                                                         typeDef);
                config.writeStyleLinks(out, blockContext);

                out.printf("</head>\n");
                out.printf("<body class=\"javadoc class %s\">\n",
                           typeDef.getKind() == ElementKind.ANNOTATION_TYPE ?
                               "annot" :
                               typeDef.getKind() == ElementKind.INTERFACE ?
                                   "iface" :
                               typeDef.getKind() == ElementKind.ENUM ? "enum" :
                               "plain");

                slice.writeUserSection(out, blockContext, "forematter",
                                       "forematter", typeDef);
                out.printf("<div class=\"javadoc-matter\">\n");

                out.printf("<div class=\"javadoc-head\">\n");
                out.printf("<h1><span>");
                slice.toHypertext(out::append, typeContext, blockContext, slice
                    .macroFormatDoc("page.heading.class", blockContext
                        .attributeEscaper().escape(typeDefProps)));
                out.printf("</span></h1>\n");
                out.printf("<div class=\"javadoc-purpose\"><span>");
                slice.writeSummaryOrSyntheticDescription(out::append,
                                                         blockContext,
                                                         typeContext, typeDef);
                out.printf("</span></div>\n");

                slice.writeElementQualities(out, blockContext, typeDef);
                out.printf("</div>\n");

                out.printf("<div class=\"javadoc-context\">\n");
                out.printf("<pre class=\"java\">\n");
                Consumer<String> core = indent -> {
                    out.print(blockContext.escape(indent));
                    writeDeclaration(out, blockContext.inCode(), typeDef,
                                     indent, 2);
                    out.print(blockContext.escape(";\n"));
                };
                writeDeclarationContext(out, blockContext.inCode(),
                                        typeDef.getEnclosingElement(), core);
                out.printf("</pre>\n");
                out.printf("</div>\n");

                /* Describe type parameters. */
                writeTypeParams(out, blockContext, typeDef);

                out.printf("<div class=\"javadoc-description\">\n");
                if (DocUtils.hasDescription(typeDoc, false)) {
                    out.printf("<h2><span>%s</span></h2>\n", escaper
                        .escape(slice
                            .getContent("section.heading.class.description")));
                    out.printf("<div class=\"body\">\n");
                    out.printf("<p>%s\n",
                               slice.toHypertext(typeContext, blockContext,
                                                 typeDoc.getFullBody()));
                    out.printf("</div>\n");
                }
                slice.writeSeeSection(out, typeContext, blockContext, typeDoc);
                out.printf("</div>\n");

                /* Catalogue all members. */
                List<TypeElement> staticClasses = new ArrayList<>();
                List<TypeElement> innerClasses = new ArrayList<>();
                List<VariableElement> instanceFields = new ArrayList<>();
                List<VariableElement> staticFields = new ArrayList<>();
                List<VariableElement> constants = new ArrayList<>();
                List<ExecutableElement> constructors = new ArrayList<>();
                List<ExecutableElement> staticMethods = new ArrayList<>();
                List<ExecutableElement> instanceMethods = new ArrayList<>();
                for (Element memb : config.elements.getAllMembers(typeDef)) {
                    /* Skip invisible and excluded elements. */
                    if (!memb.getModifiers().contains(Modifier.PUBLIC) &&
                        !memb.getModifiers().contains(Modifier.PROTECTED))
                        continue;
                    if (config.excludedElements.contains(memb)) continue;

                    switch (memb.getKind()) {
                    case CLASS:
                    case INTERFACE:
                    case ANNOTATION_TYPE:
                    case ENUM:
                        (memb.getModifiers().contains(Modifier.STATIC) ?
                            staticClasses : innerClasses)
                                .add((TypeElement) memb);
                        break;

                    case ENUM_CONSTANT:
                        constants.add((VariableElement) memb);
                        break;

                    case FIELD:
                        (memb.getModifiers().contains(Modifier.STATIC) ?
                            memb.getModifiers().contains(Modifier.FINAL) ?
                                constants : staticFields :
                            instanceFields).add((VariableElement) memb);
                        break;

                    case CONSTRUCTOR:
                        constructors.add((ExecutableElement) memb);
                        break;

                    case METHOD:
                        (memb.getModifiers().contains(Modifier.STATIC) ?
                            staticMethods : instanceMethods)
                                .add((ExecutableElement) memb);
                        break;

                    default:
                        break;
                    }
                }

                List<Element> producers = new ArrayList<>(config.producers
                    .getOrDefault(typeDef, Collections.emptySet()));
                List<Element> consumers = new ArrayList<>(config.consumers
                    .getOrDefault(typeDef, Collections.emptySet()));
                List<Element> xforms = new ArrayList<>(config.transformers
                    .getOrDefault(typeDef, Collections.emptySet()));

                /* Include as constructors the methods that are tagged
                 * as constructors. TODO: Or maybe they should be listed
                 * twice...? */
                final Collection<ExecutableElement> pseudoConstructors;
                {
                    pseudoConstructors = config.pseudoConstructors
                        .getOrDefault(typeDef, Collections.emptySet());
                    constructors.addAll(pseudoConstructors);
                }

                instanceMethods.removeAll(pseudoConstructors);
                staticMethods.removeAll(pseudoConstructors);
                producers.removeAll(pseudoConstructors);
                xforms.removeAll(pseudoConstructors);
                producers.removeAll(instanceMethods);
                producers.removeAll(staticFields);
                consumers.removeAll(instanceMethods);
                xforms.removeAll(instanceMethods);

                out.print("<table class=\"javadoc-members\" summary=\"");
                slice
                    .toHypertext(out::append, SourceContext.EMPTY,
                                 blockContext.inAttribute(), slice
                                     .getTreeContent("class-members.table-summary"));
                out.print("\">\n");
                out.printf("<caption>%s</caption>\n", escaper
                    .escape(slice.getContent("class-members.caption")));
                /* Columns are: modifiers and type variables; return
                 * type; name; parameters. */
                writeMemberList(typeDef, "classes static", "static-classes",
                                out, blockContext, staticClasses,
                                EnumSet.of(Modifier.STATIC),
                                pseudoConstructors);
                writeMemberList(typeDef, "fields static final", "constants",
                                out, blockContext, constants,
                                EnumSet.of(Modifier.STATIC, Modifier.FINAL),
                                pseudoConstructors);
                writeMemberList(typeDef, "fields static", "static-fields", out,
                                blockContext, staticFields,
                                EnumSet.of(Modifier.STATIC),
                                pseudoConstructors);
                writeMemberList(typeDef, "methods static", "static-methods",
                                out, blockContext, staticMethods,
                                EnumSet.of(Modifier.STATIC),
                                pseudoConstructors);
                writeMemberList(typeDef, "constructors", "constructors", out,
                                blockContext, constructors,
                                Collections.emptySet(), pseudoConstructors);
                writeMemberList(typeDef, "classes instance", "inner-classes",
                                out, blockContext, innerClasses,
                                Collections.emptySet(), pseudoConstructors);
                writeMemberList(typeDef, "fields instance", "fields", out,
                                blockContext, instanceFields,
                                Collections.emptySet(), pseudoConstructors);
                writeMemberList(typeDef, "methods instance", "methods", out,
                                blockContext, instanceMethods,
                                Collections.emptySet(), pseudoConstructors);
                writeMemberList(typeDef, "producers", "acquisition", out,
                                blockContext, producers, Collections.emptySet(),
                                pseudoConstructors);
                writeMemberList(typeDef, "transformers", "transformation", out,
                                blockContext, xforms, Collections.emptySet(),
                                pseudoConstructors);
                writeMemberList(typeDef, "consumers", "consumption", out,
                                blockContext, consumers, Collections.emptySet(),
                                pseudoConstructors);
                out.printf("</table>\n");

                slice.writeClassHierarchy(out, blockContext,
                                          Collections.singleton(typeDef));

                slice.writeAuthorSection(out, typeContext, blockContext,
                                         typeDoc);

                out.printf("</div>\n");
                slice.writeUserSection(out, blockContext, "aftmatter",
                                       "aftmatter", typeDef);

                out.printf("</body>\n");
                out.printf("</html>\n");
            } catch (IOException ex) {
                config.report(Kind.ERROR, typeDef, "output.class.failure.write",
                              ex, typeFile);
                return;
            }

            /* Iterate over non-class members. */
            for (Element memb : typeDef.getEnclosedElements()) {
                writeMemberDocumentation(leafName, typeFile, memb);
            }
        }
    }

    private void writeMemberDocumentation(final String leafName,
                                          final Path typeFile, Element memb) {
        /* Skip over private and package-private members. */
        if (!memb.getModifiers().contains(Modifier.PUBLIC) &&
            !memb.getModifiers().contains(Modifier.PROTECTED)) return;
        if (config.excludedElements.contains(memb)) return;

        /* Skip over non-class members. */
        final ExecutableElement execMemb;
        @SuppressWarnings("unused")
        final VariableElement varMemb;
        switch (memb.getKind()) {
        default:
            return;

        case METHOD:
        case CONSTRUCTOR:
            execMemb = (ExecutableElement) memb;
            varMemb = null;
            break;

        case FIELD:
        case ENUM_CONSTANT:
            execMemb = null;
            varMemb = (VariableElement) memb;
            break;
        }

        DocCommentTree membDoc = config.docTrees.getDocCommentTree(memb);

        /* Start building the leaf name. */
        final StringBuilder membLeafName = new StringBuilder(leafName);

        /* Distinguish constructors, methods and fields/constants, as
         * they could all have the same name. */
        switch (memb.getKind()) {
        default:
            throw new AssertionError("unreachable");

        case FIELD:
        case ENUM_CONSTANT:
            membLeafName.append("-field");
            break;

        case CONSTRUCTOR:
            membLeafName.append("-constr");
            break;

        case METHOD:
            membLeafName.append("-method");
            break;
        }

        /* Incorporate the member name. Constructurs need no name. */
        switch (memb.getKind()) {
        default:
            break;

        /* Files for most members begin with their simple name. */
        case METHOD:
        case FIELD:
        case ENUM_CONSTANT:
            membLeafName.append('-').append(memb.getSimpleName());
            break;

        /* Constructors' simple names are always <init>, so we use
         * instead case CONSTRUCTOR:
         * membLeafName.append('-').append(typeDef.getSimpleName ());
         * break; */
        }

        /* Incorporate erased parameter types. */
        switch (memb.getKind()) {
        default:
            break;

        case CONSTRUCTOR:
        case METHOD:
            for (VariableElement var : execMemb.getParameters()) {
                /* Get the parameter type, and count array dimensions.
                 * Get the type's corresponding element (if any) so we
                 * can get its simple name. Finally, erase it. */
                TypeMirror varType = var.asType();
                int dims = 0;
                while (varType.getKind() == TypeKind.ARRAY) {
                    dims++;
                    varType = ((ArrayType) varType).getComponentType();
                }
                varType = config.types.erasure(varType);

                membLeafName.append('/').append(dims).append(DocUtils
                    .getLongName(varType).replaceAll("\\.", "\\$"));
            }
            break;
        }

        /* Distinguish this as a hypertext file of a particular
         * slice. */
        membLeafName.append(config.hypertextFileSuffix)
            .append(slice.spec.suffix);

        final URI memberLoc = config.locateElement(memb);
        final Path membFile =
            typeFile.getParent().resolve(membLeafName.toString());
        final Escaper escaper = HypertextEscaper.forCData(slice.spec.charset);
        final Escaper attrEscaper =
            HypertextEscaper.forAttributes(slice.spec.charset);

        OutputContext membOutCtxt = OutputContext
            .forBlock(config.types, memberLoc, memb, escaper, attrEscaper);

        final Properties memberProps = new Properties();
        config.setElementProperties(memberProps, memb);
        memberProps.setProperty("TITLE.SHORT",
                                slice.toHypertext(SourceContext.EMPTY,
                                                  membOutCtxt.plain(),
                                                  config.shortTitle));

        SourceContext membContext = SourceContext.forElement(memb);

        try {
            Files.createDirectories(membFile.getParent());
        } catch (IOException ex) {
            config.report(Kind.ERROR, memb, "output.member.failure.mkdirs",
                          ex.getMessage(), membFile);
            return;
        }

        try (PrintWriter out = slice.openHypertextFile(membFile)) {
            out.printf("<html lang=\"%s\">\n",
                       slice.spec.locale.toString().replaceAll("_", "-"));
            out.printf("<head>\n");
            slice.writeHypertextMeta(out, membOutCtxt);
            slice.writeElementMetaLink("index", out::append, membOutCtxt,
                                       config.elements.getModuleOf(memb));
            slice.writeElementMetaLink("index", out::append, membOutCtxt,
                                       config.elements.getPackageOf(memb));
            slice.writeElementMetaLink("index up", out::append, membOutCtxt,
                                       memb.getEnclosingElement());
            out.printf("<title>%s</title>\n",
                       slice.macroFormat("page.title.member", memberProps,
                                         membOutCtxt.escaper()));
            slice.writeSummaryAsHypertextDescription(out::append,
                                                     membOutCtxt.inAttribute(),
                                                     memb);
            config.writeStyleLinks(out, membOutCtxt);
            out.printf("</head>\n");
            out.printf("<body class=\"javadoc member ");
            out.printf("\">\n");

            slice.writeUserSection(out, membOutCtxt, "forematter", "forematter",
                                   memb);
            out.printf("<div class=\"javadoc-matter\">\n");

            out.printf("<div class=\"javadoc-head\">\n");
            out.printf("<h1><span>");
            slice.toHypertext(out::append, membContext, membOutCtxt, slice
                .macroFormatDoc("page.heading.member",
                                membOutCtxt.escaper().escape(memberProps)));
            out.printf("</span></h1>\n");
            out.printf("<div class=\"javadoc-purpose\"><span>");
            slice.writeSummaryOrSyntheticDescription(out::append, membOutCtxt,
                                                     membContext, memb);
            out.printf("</span></div>\n");

            slice.writeElementQualities(out, membOutCtxt, memb);
            out.printf("</div>\n");

            out.printf("<div class=\"javadoc-context\">\n");
            out.printf("<pre class=\"java\">\n");
            Consumer<String> core = indent -> {
                out.print(membOutCtxt.escape(indent));
                writeDeclaration(out, membOutCtxt.inCode(), memb, indent, 2);
                out.print(membOutCtxt.escape(";\n"));
            };
            writeDeclarationContext(out, membOutCtxt.inCode(),
                                    memb.getEnclosingElement(), core);
            out.printf("</pre>\n");
            out.printf("</div>\n");

            /* Describe type parameters. */
            writeTypeParams(out, membOutCtxt, memb);

            out.printf("<div class=\"javadoc-description\">\n");
            if (DocUtils
                .hasDescription(membDoc,
                                !memb.getModifiers()
                                    .contains(Modifier.STATIC) &&
                                    memb.getKind() == ElementKind.METHOD)) {
                out.printf("<h2><span>%s</span></h2>\n", membOutCtxt
                    .escape(slice
                        .getContent("section.heading.member.description")));
                out.printf("<div class=\"body\">\n");
                out.printf("<p>%s\n",
                           slice.toHypertext(membContext, membOutCtxt,
                                             membDoc.getFullBody()));
                switch (memb.getKind()) {
                default:
                    break;

                case METHOD:
                    if (!memb.getModifiers().contains(Modifier.STATIC)) {
                        for (UnknownBlockTagTree def : DocUtils
                            .getUnknownBlockTags(membDoc, "default")) {
                            out.printf("<p>%s\n",
                                       slice.toHypertext(membContext,
                                                         membOutCtxt,
                                                         def.getContent()));
                        }
                        for (UnknownBlockTagTree def : DocUtils
                            .getUnknownBlockTags(membDoc, "apiNote")) {
                            out.printf("<div class=\"api-note\">\n");
                            out.printf("<h3><span>");
                            slice
                                .toHypertext(out::append, membContext,
                                             membOutCtxt,
                                             slice
                                                 .macroFormatDoc("section.heading.api-note",
                                                                 membOutCtxt
                                                                     .escaper()
                                                                     .escape(memberProps)));
                            out.printf("</span></h3>\n");
                            out.printf("<p>%s\n",
                                       slice.toHypertext(membContext,
                                                         membOutCtxt,
                                                         def.getContent()));
                            out.printf("</div>\n");
                        }
                        for (UnknownBlockTagTree def : DocUtils
                            .getUnknownBlockTags(membDoc, "implSpec")) {
                            out.printf("<div class=\"impl-spec\">\n");
                            out.printf("<h3><span>");
                            slice
                                .toHypertext(out::append, membContext,
                                             membOutCtxt,
                                             slice
                                                 .macroFormatDoc("section.heading.impl-spec",
                                                                 membOutCtxt
                                                                     .escaper()
                                                                     .escape(memberProps)));
                            out.printf("</span></h3>\n");
                            out.printf("<p>%s\n",
                                       slice.toHypertext(membContext,
                                                         membOutCtxt,
                                                         def.getContent()));
                            out.printf("</div>\n");
                        }
                        for (UnknownBlockTagTree def : DocUtils
                            .getUnknownBlockTags(membDoc, "implNote")) {
                            out.printf("<div class=\"impl-note\">\n");
                            out.printf("<h3><span>");
                            slice
                                .toHypertext(out::append, membContext,
                                             membOutCtxt,
                                             slice
                                                 .macroFormatDoc("section.heading.impl-note",
                                                                 membOutCtxt
                                                                     .escaper()
                                                                     .escape(memberProps)));
                            out.printf("</span></h3>\n");
                            out.printf("<p>%s\n",
                                       slice.toHypertext(membContext,
                                                         membOutCtxt,
                                                         def.getContent()));
                            out.printf("</div>\n");
                        }
                    }
                    break;
                }
                out.printf("</div>\n");
            }
            slice.writeSeeSection(out, membContext, membOutCtxt, membDoc);
            out.printf("</div>\n");

            /* Describe parameters, return type and exceptions. */
            while (execMemb != null) {
                /* Get the exception documentation, and eliminate
                 * entries that cannot be generated. Also, keep track of
                 * declared exceptions accounted for, so that remaining
                 * ones can be mark as undocumented. */
                List<ThrowsTree> exceptionDocs = new ArrayList<>();
                DocUtils.getThrowsTags(membDoc).forEach(exceptionDocs::add);
                Collection<TypeMirror> thrown =
                    new HashSet<>(execMemb.getThrownTypes());
                for (Iterator<ThrowsTree> exIter = exceptionDocs.iterator();
                     exIter.hasNext();) {

                    /* Determine what type is thrown. */
                    ThrowsTree tt = exIter.next();
                    ReferenceTree exName = tt.getExceptionName();
                    Element exElem = config
                        .resolveSignature(execMemb, exName.getSignature());
                    if (exElem == null) {
                        config.report(Kind.NOTE, memb,
                                      "output.member.not-thrown",
                                      exName.getSignature());
                        exIter.remove();
                        continue;
                    }
                    TypeMirror exType = exElem.asType();

                    /* Only Throwable types can be thrown. */
                    if (!config.types.isSubtype(exType,
                                                config.javaLangThrowable)) {
                        config.report(Kind.WARNING, memb,
                                      "output.member.unthrowable", exType);
                        exIter.remove();
                        continue;
                    }

                    /* Only subtypes of RuntimeExceptions, Errors, and
                     * listed exceptions can be thrown. */
                    if (!config.types.isSubtype(exType, config.javaLangError) &&
                        !config.types
                            .isSubtype(exType,
                                       config.javaLangRuntimeException) &&
                        thrown.stream().noneMatch(ttt -> config.types
                            .isSubtype(exType, ttt))) {
                        exIter.remove();
                        continue;
                    }

                    /* Eliminate declared types that are non-strict
                     * subtypes of this one, as they are now accounted
                     * for. */
                    thrown.removeIf(th -> config.types.isSubtype(th, exType));
                }

                /* Don't bother creating the table if there's nothing
                 * left to document. */
                if (exceptionDocs.isEmpty() && thrown.isEmpty() &&
                    execMemb.getParameters().isEmpty() &&
                    (execMemb.getReturnType().getKind() == TypeKind.NONE ||
                        execMemb.getReturnType().getKind() == TypeKind.VOID))
                    break;

                out.print("<table class=\"javadoc-terminals\" summary=\"");
                slice
                    .toHypertext(out::append, SourceContext.EMPTY,
                                 membOutCtxt.inAttribute(), slice
                                     .getTreeContent("terminals.table-summary"));
                out.print("\">\n");
                if (!execMemb.getParameters().isEmpty()) {
                    out.print("<tbody class=\"params\">\n");
                    out.print("<tr class=\"heading\">\n");
                    out.printf("<th colspan=\"3\">%s</th>\n", membOutCtxt
                        .escape(slice.getContent("terminals.heading.params")));
                    out.print("</tr>\n");
                    final List<? extends VariableElement> params =
                        execMemb.getParameters();
                    final int paramCount = params.size();
                    final int vararg =
                        execMemb.isVarArgs() ? paramCount - 1 : paramCount;
                    for (int pos = 0; pos < paramCount; pos++) {
                        VariableElement param = params.get(pos);
                        String name = param.getSimpleName().toString();
                        ParamTree pDoc =
                            DocUtils.findParameter(membDoc, name, false);
                        out.printf("<tr class=\"item %s\">\n",
                                   pos % 2 == 0 ? "even" : "odd");
                        out.print("<td class=\"type\">");
                        out.print("<pre class=\"java\">\n");
                        List<? extends AnnotationMirror> typeAnnots = DocUtils
                            .getDocumentedAnnotationMirrors(param.asType());
                        slice.writeAnnotationMirrors(out, membOutCtxt.inCode(),
                                                     typeAnnots, "", 2);
                        if (pos == vararg) {
                            slice
                                .writeTypeReference(out::append,
                                                    membOutCtxt.inCode(),
                                                    ((ArrayType) param.asType())
                                                        .getComponentType(),
                                                    LinkContent.NORMAL);
                            out.print(membOutCtxt.escape("..."));
                        } else {
                            slice.writeTypeReference(out::append,
                                                     membOutCtxt.inCode(),
                                                     param.asType(),
                                                     LinkContent.NORMAL);
                        }
                        out.print("\n</pre>");
                        out.print("</td>\n");

                        List<? extends AnnotationMirror> parAnnots =
                            DocUtils.getDocumentedAnnotationMirrors(param);
                        out.print("<td class=\"name\">");
                        out.print("<pre" + " class=\"java\">\n");
                        slice.writeAnnotationMirrors(out, membOutCtxt.inCode(),
                                                     parAnnots, "", 2);
                        out.print("<span class=\"name\">");
                        out.print(membOutCtxt.escape(name));
                        out.print("</span>");
                        out.print("\n</pre></td>\n");

                        out.print("<td class=\"descr\">");
                        if (pDoc == null) {
                            /* Try to {@inheritDoc} before checking for
                             * a synthetic method. */
                            if (!slice.writeInheritedParamDocs(out::append,
                                                               execMemb, pos,
                                                               membOutCtxt,
                                                               true))
                                slice
                                    .toHypertext(out::append,
                                                 SourceContext.EMPTY,
                                                 membOutCtxt, slice
                                                     .getSyntheticDescription(param));
                        } else {
                            out.print("<p>");
                            slice.toHypertext(out::append,
                                              membContext.inParamTag(pos),
                                              membOutCtxt,
                                              pDoc.getDescription());
                        }
                        out.print("</td>\n");
                        out.print("</tr>\n");
                    }
                    out.print("</tbody>\n");
                }
                if (execMemb.getReturnType().getKind() != TypeKind.VOID &&
                    execMemb.getReturnType().getKind() != TypeKind.NONE) {
                    out.printf("<!-- kind=%s -->\n",
                               execMemb.getReturnType().getKind());
                    out.print("<tbody class=\"return\">\n");
                    out.print("<tr class=\"heading\">\n");
                    out.printf("<th colspan=\"3\">%s</th>\n", membOutCtxt
                        .escape(slice.getContent("terminals.heading.return")));
                    out.print("</tr>\n");
                    ReturnTree returnTag = DocUtils.getReturnTag(membDoc);
                    out.print("<tr class=\"item even\">\n");
                    out.print("<td class=\"type\" colspan=\"2\">");
                    out.print("<pre class=\"java\">\n");
                    List<? extends AnnotationMirror> annots =
                        DocUtils.getDocumentedAnnotationMirrors(execMemb
                            .getReturnType());
                    slice.writeAnnotationMirrors(out, membOutCtxt.inCode(),
                                                 annots, "", 2);
                    slice.writeTypeReference(out::append, membOutCtxt.inCode(),
                                             execMemb.getReturnType(),
                                             LinkContent.NORMAL);
                    out.print("</pre>");
                    out.print("</td>\n");
                    out.print("<td class=\"descr\">");
                    if (returnTag == null) {
                        /* Try to {@inheritDoc} before checking for a
                         * synthetic method. */
                        if (!slice
                            .writeInheritedReturnDocs(out::append, execMemb,
                                                      membOutCtxt, true)) {
                            slice
                                .toHypertext(out::append, SourceContext.EMPTY,
                                             membOutCtxt,
                                             slice
                                                 .getSyntheticDescription(execMemb,
                                                                          DocTree.Kind.RETURN));
                        }
                    } else {
                        out.print("<p>");
                        slice.toHypertext(out::append,
                                          membContext.inReturnTag(),
                                          membOutCtxt,
                                          returnTag.getDescription());
                    }
                    out.print("</td>\n");
                    out.print("</tr>\n");
                    out.print("</tbody>\n");
                }
                if (!exceptionDocs.isEmpty() | !thrown.isEmpty()) {
                    out.print("<tbody class=\"throws\">\n");
                    out.print("<tr class=\"heading\">\n");
                    out.printf("<th colspan=\"3\">%s</th>\n", membOutCtxt
                        .escape(slice.getContent("terminals.heading.throws")));
                    out.print("</tr>\n");

                    boolean even = true;
                    for (ThrowsTree exDoc : exceptionDocs) {
                        TypeMirror undoc =
                            config.resolveSignature(memb,
                                                    exDoc.getExceptionName()
                                                        .getSignature())
                                .asType();
                        SourceContext srcCtxt = membContext.inThrowsTag(undoc);
                        out.printf("<tr class=\"item %s\">\n",
                                   even ? "even" : "odd");
                        even = !even;
                        out.print("<td class=\"type\" colspan=\"2\">");
                        out.print("<pre class=\"java\">");
                        List<? extends AnnotationMirror> annots =
                            DocUtils.getDocumentedAnnotationMirrors(undoc);
                        slice.writeAnnotationMirrors(out, membOutCtxt.inCode(),
                                                     annots, "", 2);
                        slice.writeTypeReference(out::append,
                                                 membOutCtxt.inCode(), undoc,
                                                 LinkContent.NORMAL);
                        out.print("</pre>");
                        out.print("</td>\n");
                        out.print("<td class=\"descr\">");
                        out.print("<p>");
                        slice.toHypertext(out::append, srcCtxt, membOutCtxt,
                                          exDoc.getDescription());
                        out.print("</td>\n");
                        out.print("</tr>\n");
                    }
                    for (TypeMirror undoc : thrown) {
                        out.printf("<tr class=\"item %s\">\n",
                                   even ? "even" : "odd");
                        even = !even;
                        out.print("<td class=\"type\" colspan=\"2\">");
                        out.print("<pre class=\"java\">");
                        List<? extends AnnotationMirror> annots =
                            DocUtils.getDocumentedAnnotationMirrors(undoc);
                        slice.writeAnnotationMirrors(out, membOutCtxt.inCode(),
                                                     annots, "", 2);
                        slice.writeTypeReference(out::append,
                                                 membOutCtxt.inCode(), undoc,
                                                 LinkContent.NORMAL);
                        out.print("</pre>");
                        out.print("</td>\n");
                        out.print("<td class=\"descr\">");
                        /* Try to {@inheritDoc} before checking for a
                         * synthetic method. */
                        if (!slice.writeInheritedThrowsDocs(out::append,
                                                            execMemb, undoc,
                                                            membOutCtxt,
                                                            true)) {
                            slice
                                .toHypertext(out::append, SourceContext.EMPTY,
                                             membOutCtxt, slice
                                                 .getTreeContent("synth.unspecified"));
                            slice.recordUndocumentedElement(memb);
                        }
                        out.print("</td>\n");
                        out.print("</tr>\n");
                    }
                    out.print("</tbody>\n");
                }
                out.print("</table>\n");
                break;
            }

            slice.writeAuthorSection(out, membContext, membOutCtxt, membDoc);

            out.printf("</div>\n");
            slice.writeUserSection(out, membOutCtxt, "aftmatter", "aftmatter",
                                   memb);

            out.printf("</body>\n");
            out.printf("</html>\n");
        } catch (IOException ex) {
            config.report(Kind.ERROR, memb, "output.member.failure.write",
                          ex.getMessage(), membFile);
            return;
        }
    }

    private void
        writeMemberList(TypeElement typeDef, String markupClasses,
                        String headingKey, PrintWriter out,
                        OutputContext outCtxt, List<? extends Element> members,
                        Collection<? extends Modifier> redundantMods,
                        Collection<? extends ExecutableElement> pseudocons) {
        if (!members.isEmpty()) {
            if (false) {
                int longestName = 0;
                for (Element cand : members) {
                    String name = slice
                        .getElementReference(SourceContext.EMPTY, OutputContext
                            .plain(config.types, null, outCtxt.element()), cand,
                                             LinkContent.NORMAL.withoutPackage()
                                                 .withoutParameters());
                    int len = name.length();
                    if (len > longestName) longestName = len;
                }
            }

            Collections.sort(members,
                             (a, b) -> String.CASE_INSENSITIVE_ORDER
                                 .compare(a.getSimpleName().toString(),
                                          b.getSimpleName().toString()));
            out.printf("<tbody class=\"group %s\">\n", markupClasses);
            out.printf("<tr class=\"heading\">\n");
            out.printf("<th colspan=\"2\"><!-- empty --></th>\n");
            out.printf("<th colspan=\"2\">%s</th>\n", outCtxt
                .escape(slice.getContent("class-members.group." + headingKey)));
            out.printf("</tr>\n");
            boolean even = true;
            for (Element memb : members) {
                TypeElement definer = (TypeElement) memb.getEnclosingElement();
                boolean inherited = !config.types
                    .isSameType(typeDef.asType(), definer.asType()) &&
                    config.types.isSubtype(typeDef.asType(), definer.asType());
                @SuppressWarnings("unused")
                SourceContext membCtxt = SourceContext.forElement(memb);
                out.printf("<tr class=\"item %s%s%s\">\n",
                           even ? "even" : "odd",
                           config.deprecatedElements.containsKey(memb) ?
                               " deprecated" : "",
                           inherited ? " inherited" : "");
                out.printf("<td class=\"modifiers generics\">");
                out.printf("<code class=\"java\">");
                for (Modifier mod : memb.getModifiers()) {
                    if (redundantMods.contains(mod)) continue;
                    switch (mod) {
                    case NATIVE:
                    case SYNCHRONIZED:
                    case STRICTFP:
                        continue;

                    default:
                        break;
                    }
                    out.printf("%s ", outCtxt.escape(mod.toString()));
                }
                switch (memb.getKind()) {
                case CONSTRUCTOR:
                case METHOD:
                    ExecutableElement execMemb = (ExecutableElement) memb;
                    List<? extends TypeParameterElement> tps =
                        execMemb.getTypeParameters();
                    if (!tps.isEmpty()) {
                        out.print(outCtxt.escape("<"));
                        String sep = "";
                        for (TypeParameterElement tp : tps) {
                            out.print(outCtxt.escape(sep));
                            sep = ", ";

                            out.print(outCtxt.escape(tp.getSimpleName()));
                        }
                        out.print(outCtxt.escape(">"));
                    }
                    break;

                default:
                    break;
                }
                out.printf("</code>");
                out.printf("</td>\n");

                out.printf("<td class=\"return-type\">");
                switch (memb.getKind()) {
                default:
                    break;

                case METHOD:
                    ExecutableElement exec = (ExecutableElement) memb;
                    slice.writeTypeReference(out::append, outCtxt,
                                             exec.getReturnType(),
                                             LinkContent.NORMAL);
                    break;

                case FIELD:
                    VariableElement var = (VariableElement) memb;
                    slice.writeTypeReference(out::append, outCtxt, var.asType(),
                                             LinkContent.NORMAL);
                    break;
                }
                out.printf("</td>\n");

                out.printf("<td class=\"name-purpose\">");
                out.print("<div class=\"sig\">");
                out.print("<pre class=\"java name\">");

                /* If a static method is being printed out where static
                 * is redundant, it could be a pseudo-constructor in the
                 * same class, and it helps to mention the class name.
                 * Include the container if a static field or method is
                 * referenced. TODO: Add an intermediate level of detail
                 * showing one container. */
                LinkContent detail = LinkContent.NORMAL.withoutParameters();
                if (!pseudocons.contains(memb))
                    detail = detail.withoutNonessentialContainers();
                if (false) {
                    if (!redundantMods.contains(Modifier.STATIC) &&
                        memb.getModifiers().contains(Modifier.STATIC) &&
                        memb.getKind() == ElementKind.METHOD) {
                        ;
                    } else {
                        detail = detail.withoutNonessentialContainers();
                    }
                }

                slice.writeElementReference(out::append, outCtxt.inCode(), memb,
                                            detail);
                out.print("</pre>");

                switch (memb.getKind()) {
                default:
                    break;

                case METHOD:
                case CONSTRUCTOR:
                    ExecutableElement exec = (ExecutableElement) memb;
                    int longestParamName = 0;
                    for (VariableElement param : exec.getParameters()) {
                        int len = param.getSimpleName().length();
                        if (len > longestParamName) longestParamName = len;
                    }
                    out.print("<div class=\"params\">");
                    out.print("<pre class=\"java params\">");
                    out.print(outCtxt.escape("("));
                    List<? extends VariableElement> params =
                        exec.getParameters();
                    final int plen = params.size();
                    final int vararg = exec.isVarArgs() ? plen - 1 : plen;
                    for (int i = 0; i < plen; i++) {
                        VariableElement param = params.get(i);
                        if (i != 0) {
                            out.print(",</pre>\n");
                            out.print("<pre class=\"java params\">\n ");
                        }

                        /* Breaking with Java syntax, we write the
                         * parameter name first, padded so the names and
                         * types line up. */
                        out.print("<span class=\"param\">");
                        out.printf("%" + longestParamName + "s",
                                   outCtxt.escape(param.getSimpleName()));
                        out.print("</span>");

                        out.print(outCtxt.escape(" : "));
                        if (i == vararg) {
                            slice
                                .writeTypeReference(out::append,
                                                    outCtxt.inCode(),
                                                    ((ArrayType) param.asType())
                                                        .getComponentType(),
                                                    LinkContent.NORMAL);
                            out.print(outCtxt.inCode().escape("..."));
                        } else {
                            slice.writeTypeReference(out::append,
                                                     outCtxt.inCode(),
                                                     param.asType(),
                                                     LinkContent.NORMAL);
                        }
                    }
                    out.print(outCtxt.escape(")"));
                    out.print("</pre>");
                    out.printf("</div>\n");
                    break;
                }
                out.printf("</div>\n");

                if (!slice
                    .writeSummary(out::append, outCtxt, memb,
                                  () -> out.print("<div class=\"purpose\"><p>"),
                                  () -> out.printf("</div>"))) {
                    if (!inherited) {
                        out.print("<div class=\"purpose\"><p>");
                        slice.toHypertext(out::append, SourceContext.EMPTY,
                                          outCtxt,
                                          slice.getSyntheticDescription(memb));
                        out.printf("</div>");
                    }
                }
                out.printf("</td>\n");
                out.printf("</tr>\n");

                even = !even;
            }
            out.printf("</tbody>\n");
        }
    }

    void writeTypeParams(PrintWriter out, OutputContext outCtxt, Element elem) {
        final List<? extends TypeParameterElement> tps;
        switch (elem.getKind()) {
        case CLASS:
        case ENUM:
        case INTERFACE:
        case ANNOTATION_TYPE:
            tps = ((TypeElement) elem).getTypeParameters();
            break;

        case CONSTRUCTOR:
        case METHOD:
            tps = ((ExecutableElement) elem).getTypeParameters();
            break;

        default:
            return;
        }
        if (tps.isEmpty()) return;

        DocCommentTree doc = config.docTrees.getDocCommentTree(elem);

        out.print("<table class=\"javadoc-type-params\" summary=\"");
        slice.toHypertext(out::append, SourceContext.EMPTY,
                          outCtxt.inAttribute(),
                          slice.getTreeContent("type-param.table-summary"));
        out.print("\">\n");
        if (false) out.printf("<caption>%s</caption>\n", outCtxt.escape(slice
            .getContent("section.heading.member.type-parameters")));
        out.printf("<thead>\n");
        out.printf("<tr>");
        out.printf("<th class=\"name\">%s</th>\n",
                   outCtxt.escape(slice.getContent("type-param.heading.name")));
        out.printf("<th class=\"purpose\">%s</th>\n", outCtxt
            .escape(slice.getContent("type-param.heading.meaning")));
        out.printf("</tr>");
        out.printf("</thead>\n");
        out.printf("<tbody>");
        boolean even = true;
        SourceContext elemCtxt = SourceContext.forElement(elem);
        for (TypeParameterElement tp : tps) {
            ParamTree tag =
                DocUtils.findParameter(doc, tp.getSimpleName(), true);
            SourceContext srcCtxt = elemCtxt.inBlockTag();
            out.printf("<tr class=\"item %s\">", even ? "even" : "odd");
            even = !even;
            out.printf("<td class=\"name\"><code class=\"javadoc\">");
            OutputContext defCtxt = outCtxt.inCode();
            Slice.writeCamelText(out::append, defCtxt, tp.getSimpleName());
            defCtxt = defCtxt.accountFor((TypeVariable) tp.asType());
            String sep = " extends ";
            for (TypeMirror b : tp.getBounds()) {
                if (config.types.isSameType(b, config.javaLangObject)) continue;
                out.print(defCtxt.escape(sep));
                sep = " & ";
                slice.writeTypeReference(out::append, defCtxt, b,
                                         LinkContent.NORMAL);
            }
            /* slice.writeElementReference(out::append,
             * outCtxt.inCode().inLink(), tp, LinkContent.NORMAL); */
            out.printf("</code></td> ");
            out.printf("<td class=\"purpose\"><p>");
            if (tag == null || tag.getDescription() == null ||
                tag.getDescription().isEmpty()) {
                slice.toHypertext(out::append, SourceContext.EMPTY, outCtxt,
                                  slice.getSyntheticDescription(tp));
            } else {
                slice.toHypertext(out::append, srcCtxt, outCtxt,
                                  tag.getDescription());
            }
            out.printf("</td>");
            out.printf("</tr>\n");
        }
        out.printf("</tbody>");
        out.printf("</table>\n");
    }

    private void writeDeclaration(PrintWriter out, OutputContext ctxt,
                                  Element elem, String indent,
                                  int annotDetail) {
        /* Write annotations on new lines. */
        slice.writeAnnotationMirrors(out, ctxt,
                                     DocUtils
                                         .getDocumentedAnnotationMirrors(elem),
                                     indent, annotDetail);

        Counter c = new Counter();
        {
            String mods = "";
            if (elem.getModifiers().contains(Modifier.PUBLIC))
                mods += "public ";
            if (elem.getModifiers().contains(Modifier.PROTECTED))
                mods += "protected ";
            if (elem.getModifiers().contains(Modifier.STATIC))
                mods += "static ";
            if (elem.getModifiers().contains(Modifier.FINAL)) mods += "final ";
            if (elem.getModifiers().contains(Modifier.ABSTRACT))
                mods += "abstract ";
            if (elem.getModifiers().contains(Modifier.DEFAULT))
                mods += "default ";
            if (elem.getModifiers().contains(Modifier.TRANSIENT))
                mods += "transient ";
            out.print(ctxt.escape(mods));
            c.sum += mods.length();
        }

        /* Get specialized views of the element. */
        final ExecutableElement execElem;
        final VariableElement varElem;
        final TypeElement typeElem;
        switch (elem.getKind()) {
        case CLASS:
        case ENUM:
        case ANNOTATION_TYPE:
        case INTERFACE:
            typeElem = (TypeElement) elem;
            execElem = null;
            varElem = null;
            break;

        case ENUM_CONSTANT:
        case FIELD:
            varElem = (VariableElement) elem;
            execElem = null;
            typeElem = null;
            break;

        case METHOD:
        case CONSTRUCTOR:
            execElem = (ExecutableElement) elem;
            varElem = null;
            typeElem = null;
            break;

        default:
            execElem = null;
            varElem = null;
            typeElem = null;
            break;
        }

        /* Write out type parameters of constructors and methods. */
        switch (elem.getKind()) {
        default:
            break;

        case METHOD:
        case CONSTRUCTOR:
            List<? extends TypeParameterElement> tps =
                execElem.getTypeParameters();
            if (!tps.isEmpty()) {
                out.print(ctxt.escape("<"));
                c.sum++;
                String sep = "";
                for (TypeParameterElement tp : tps) {
                    out.print(ctxt.escape(sep));
                    c.sum += sep.length();
                    sep = ", ";

                    out.print(ctxt.escape(tp.getSimpleName()));
                    c.sum += tp.getSimpleName().length();
                }
                out.print(ctxt.escape("> "));
                c.sum++;
            }
            break;
        }

        final String kwd;
        switch (elem.getKind()) {
        case METHOD:
            String rtaIndent =
                indent + String.join("", Collections.nCopies(c.sum, " "));
            slice.writeAnnotationMirrors(out, ctxt, DocUtils
                .getDocumentedAnnotationMirrors(execElem.getReturnType()),
                                         rtaIndent, 2);
            slice.writeTypeReference(c::accept, OutputContext
                .plain(config.types, ctxt.location(), ctxt.element()),
                                     execElem.getReturnType(),
                                     LinkContent.NORMAL.withoutContainers());
            slice.writeTypeReference(out::append, ctxt,
                                     execElem.getReturnType(),
                                     LinkContent.NORMAL.withoutContainers());
            kwd = " ";
            break;

        case ENUM_CONSTANT:
        case FIELD:
            slice.writeTypeReference(c::accept, OutputContext
                .plain(config.types, ctxt.location(), ctxt.element()),
                                     varElem.asType(),
                                     LinkContent.NORMAL.withoutContainers());
            slice.writeTypeReference(out::append, ctxt, varElem.asType(),
                                     LinkContent.NORMAL.withoutContainers());
            kwd = " ";
            break;

        case CLASS:
            kwd = "class ";
            break;

        case ENUM:
            kwd = "enum ";
            break;

        case INTERFACE:
            kwd = "interface ";
            break;

        case ANNOTATION_TYPE:
            kwd = "@interface ";
            break;

        default:
            kwd = "";
            break;
        }

        out.print(ctxt.escape(kwd));
        c.sum += kwd.length();

        slice.writeElementReference(c::accept, OutputContext
            .plain(config.types, ctxt.location(), ctxt.element()), elem,
                                    LinkContent.NORMAL.withoutContainers()
                                        .withoutParameters());
        slice.writeElementReference(out::append, ctxt, elem, LinkContent.NORMAL
            .withoutContainers().withoutParameters());

        switch (elem.getKind()) {
        default:
            break;

        case CONSTRUCTOR:
        case METHOD: {
            String newIndent =
                indent + String.join("", Collections.nCopies(c.sum + 1, " "));
            List<? extends VariableElement> params = execElem.getParameters();
            out.print(ctxt.escape("("));
            String sep = "";
            final int plen = params.size();
            final int vararg = execElem.isVarArgs() ? plen - 1 : plen;
            for (int i = 0; i < plen; i++) {
                VariableElement param = params.get(i);
                out.print(ctxt.escape(sep));
                sep = ",\n" + newIndent;

                slice.writeAnnotationMirrors(out, ctxt, DocUtils
                    .getDocumentedAnnotationMirrors(param.asType()), newIndent,
                                             2);
                if (i == vararg) {
                    slice.writeTypeReference(out::append, ctxt,
                                             ((ArrayType) param.asType())
                                                 .getComponentType(),
                                             LinkContent.NORMAL);
                    out.print(ctxt.escape("... " + param.getSimpleName()));
                } else {
                    slice.writeTypeReference(out::append, ctxt, param.asType(),
                                             LinkContent.NORMAL);
                    out.print(ctxt.escape(" " + param.getSimpleName()));
                }
            }
            out.print(ctxt.escape(")"));
        }
            break;

        case ENUM:
        case CLASS: {
            if (true || !config.types.isSameType(typeElem.getSuperclass(),
                                                 config.javaLangObject)) {
                out.print(ctxt.escape("\n" + indent + "  extends "));
                slice.writeTypeReference(out::append, ctxt,
                                         typeElem.getSuperclass(),
                                         LinkContent.NORMAL);
            }
            String sep = "\n" + indent + "  implements ";
            for (TypeMirror impl : typeElem.getInterfaces()) {
                out.print(ctxt.escape(sep));
                sep = "\n" + indent + "             ";
                slice.writeTypeReference(out::append, ctxt, impl,
                                         LinkContent.NORMAL);
            }
        }
            break;

        case ANNOTATION_TYPE:
        case INTERFACE: {
            String sep = "\n" + indent + "  extends ";
            for (TypeMirror impl : typeElem.getInterfaces()) {
                out.print(ctxt.escape(sep));
                sep = "\n" + indent + "          ";
                slice.writeTypeReference(out::append, ctxt, impl,
                                         LinkContent.NORMAL);
            }
        }
            break;
        }
    }

    private void writeDeclarationContext(PrintWriter out, OutputContext ctxt,
                                         Element elem,
                                         Consumer<? super String> core) {
        switch (elem.getKind()) {
        default:
            return;

        case PACKAGE:
            slice.writeAnnotationMirrors(out, ctxt, DocUtils
                .getDocumentedAnnotationMirrors(elem), "", 1);
            out.print("package ");
            slice.writeElementReference(out::append, ctxt, elem,
                                        LinkContent.NORMAL);
            out.print(";\n\n");
            core.accept("");
            return;

        case CLASS:
        case ENUM:
        case INTERFACE:
        case ANNOTATION_TYPE:
            break;
        }
        Consumer<String> myCore = indent -> {
            out.print(ctxt.escape(indent));
            writeDeclaration(out, ctxt, elem, indent, 1);
            out.print(ctxt.escape(" {\n"));
            core.accept(indent + "    ");
            out.print(ctxt.escape(indent));
            out.print(ctxt.escape("}\n"));
        };
        writeDeclarationContext(out, ctxt, elem.getEnclosingElement(), myCore);
    }
}
