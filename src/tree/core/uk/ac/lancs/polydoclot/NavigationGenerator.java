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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.ElementKindVisitor9;
import javax.tools.Diagnostic.Kind;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.source.doctree.DocTree;

import uk.ac.lancs.polydoclot.util.Utils;

/**
 * Generates a
 * <a href="http://www.standard-navigation.org/">Standard-Navigation</a>
 * out-of-band sitemap file.
 * 
 * @author simpsons
 */
public final class NavigationGenerator {
    private static final String STANDARD_SITEMAP_NS_URI =
        "http://standard-sitemap.org/2007/ns";

    private static final String XLINK_NS_URI = "http://www.w3.org/1999/xlink";

    private final Slice slice;

    /**
     * Prepare to generate out-of-band navigation.
     * 
     * @param the slice to be generated
     */
    public NavigationGenerator(Slice slice) {
        this.slice = slice;
    }

    private static String caseless(CharSequence in) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c == '_' || Character.isUpperCase(c)) out.append('_');
            out.append(c);
        }
        return out.toString();
    }

    private String identify(List<? extends VariableElement> params) {
        StringBuilder result = new StringBuilder();
        for (VariableElement param : params) {
            TypeMirror type = slice.config.types.erasure(param.asType());
            int dims = 0;
            while (type.getKind() == TypeKind.ARRAY) {
                dims++;
                type = ((ArrayType) type).getComponentType();
            }

            result.append('-').append(dims);
            switch (type.getKind()) {
            case BOOLEAN:
                result.append("boolean");
                break;
            case BYTE:
                result.append("byte");
                break;
            case CHAR:
                result.append("char");
                break;
            case SHORT:
                result.append("short");
                break;
            case INT:
                result.append("int");
                break;
            case LONG:
                result.append("long");
                break;
            case FLOAT:
                result.append("float");
                break;
            case DOUBLE:
                result.append("double");
                break;
            case DECLARED:
                DeclaredType dt = (DeclaredType) type;
                TypeElement dte = (TypeElement) dt.asElement();
                result.append(dte.getQualifiedName());
                break;
            default:
                throw new AssertionError("unreachable");
            }
        }
        return result.toString();
    }

    private String identify(javax.lang.model.element.Element elem) {
        switch (elem.getKind()) {
        case MODULE:
            return "module-"
                + caseless(((ModuleElement) elem).getQualifiedName());

        case PACKAGE:
            return "package-"
                + caseless(((PackageElement) elem).getQualifiedName());

        case CLASS:
        case ENUM:
        case INTERFACE:
        case ANNOTATION_TYPE:
            return "class-"
                + caseless(((TypeElement) elem).getQualifiedName());

        case CONSTRUCTOR:
            return "constr-"
                + caseless(((TypeElement) elem.getEnclosingElement())
                    .getQualifiedName())
                + identify(((ExecutableElement) elem).getParameters());

        case METHOD:
            return "method-"
                + caseless(((TypeElement) elem.getEnclosingElement())
                    .getQualifiedName())
                + '-' + elem.getSimpleName()
                + identify(((ExecutableElement) elem).getParameters());

        case FIELD:
        case ENUM_CONSTANT:
            return "field-"
                + caseless(((TypeElement) elem.getEnclosingElement())
                    .getQualifiedName())
                + '-' + elem.getSimpleName();

        default:
            throw new AssertionError("unreachable: " + elem.getKind());
        }
    }

    private void
        populate(Document doc, Element container,
                 List<? extends javax.lang.model.element.Element> membs) {
        container.setAttribute("order", "lexical");
        for (javax.lang.model.element.Element memb : membs) {
            Element ref =
                doc.createElementNS(STANDARD_SITEMAP_NS_URI, "external");
            container.appendChild(ref);
            ref.setAttributeNS(XLINK_NS_URI, "xlink:href",
                               "#" + identify(memb));
        }
    }

    private void
        addGroup(Document doc, Element container,
                 List<? extends javax.lang.model.element.Element> membs,
                 String key) {
        if (!membs.isEmpty()) {
            Element group =
                doc.createElementNS(STANDARD_SITEMAP_NS_URI, "group");
            container.appendChild(group);
            group
                .setAttribute("name",
                              slice.getContent("class-members.group." + key));
            populate(doc, group, membs);
        }
    }

    /**
     * Generate the out-of-band sitemap file.
     */
    public void run() {
        slice.config.diagnostic("output.navigation");

        URI navLoc = URI.create("standard-sitemap.xml");
        Path navFile = slice.config.outputDirectory
            .resolve("standard-sitemap.xml" + slice.spec.suffix);

        try {
            /* Start with a blank XML document. */
            final Document doc;
            {
                DocumentBuilderFactory dbf =
                    DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                DocumentBuilder db = dbf.newDocumentBuilder();
                doc = db.newDocument();
                doc.setXmlStandalone(true);
                doc.setXmlVersion("1.0");
            }

            /* Create and position the root element. */
            Element root =
                doc.createElementNS(STANDARD_SITEMAP_NS_URI, "sitemap");
            doc.appendChild(root);
            final String langCode =
                slice.spec.locale.toString().replaceAll("_", "-");
            root.setAttribute("lang", langCode);
            root.setAttributeNS(XMLConstants.XML_NS_URI, "lang", langCode);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                                "xmlns:xlink", XLINK_NS_URI);

            /* Create the element defining how to change HTML stylesheet
             * classes dynamically depending on how persistently the
             * sitemap is displayed. */
            Element changeElem =
                doc.createElementNS(STANDARD_SITEMAP_NS_URI, "change");
            root.appendChild(changeElem);
            changeElem.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                                      "xmlns:html",
                                      "http://www.w3.org/1999/xhtml");
            changeElem.setAttribute("elem", "/html:html/html:body");
            changeElem.setAttribute("attr", "class");
            changeElem.setAttribute("prefix", "stdmap");

            /* Create the top-level elements. */
            Element home =
                doc.createElementNS(STANDARD_SITEMAP_NS_URI, "item");
            root.appendChild(home);
            home.setAttribute("role", "home");
            home.setAttribute("name",
                              slice.getContent("section.heading.overview"));

            List<? extends DocTree> anonGroupTitle =
                slice.getTreeContent("group.unnamed"
                    + (slice.config.groupedPackages.size() == 1 ? ".sole"
                        : ""));
            for (Map.Entry<Object, List<? extends DocTree>> entry : slice.config.groupTitles
                .entrySet()) {
                List<? extends DocTree> title = entry.getValue();
                if (title == null) title = anonGroupTitle;
                List<PackageElement> pkgList =
                    new ArrayList<>(slice.config.groupedPackages
                        .getOrDefault(entry.getKey(),
                                      Collections.emptySet()));
                List<ModuleElement> modList =
                    new ArrayList<>(slice.config.groupedModules
                        .getOrDefault(entry.getKey(),
                                      Collections.emptySet()));
                if (pkgList.isEmpty() && modList.isEmpty()) continue;
                pkgList.sort((a, b) -> String.CASE_INSENSITIVE_ORDER
                    .compare(a.getQualifiedName().toString(),
                             b.getQualifiedName().toString()));
                modList.sort((a, b) -> String.CASE_INSENSITIVE_ORDER
                    .compare(a.getQualifiedName().toString(),
                             b.getQualifiedName().toString()));
                Element cont =
                    doc.createElementNS(STANDARD_SITEMAP_NS_URI, "group");
                root.appendChild(cont);
                StringBuilder titleText = new StringBuilder();
                slice.toHypertext(titleText::append, SourceContext.EMPTY,
                                  OutputContext.plain(slice.config.types,
                                                      null, null),
                                  title);
                cont.setAttribute("name", titleText.toString());
                if (!modList.isEmpty()) {
                    Element grp =
                        doc.createElementNS(STANDARD_SITEMAP_NS_URI, "group");
                    cont.appendChild(grp);
                    grp.setAttribute("name", slice
                        .getContent("section.heading.overview.modules"));
                    populate(doc, grp, modList);
                }
                if (!pkgList.isEmpty()) {
                    Element grp =
                        doc.createElementNS(STANDARD_SITEMAP_NS_URI, "group");
                    cont.appendChild(grp);
                    grp.setAttribute("name", slice
                        .getContent("section.heading.overview.packages"));
                    populate(doc, grp, pkgList);
                }
            }

            /* Create a dummy item to contain multiply referenced
             * items. */
            Element unlisted =
                doc.createElementNS(STANDARD_SITEMAP_NS_URI, "item");
            root.appendChild(unlisted);
            unlisted.setAttribute("tree", "exclude");

            /* Create elements to describe modules. */
            for (javax.lang.model.element.Element item : slice.config.env
                .getIncludedElements()) {
                if (slice.config.excludedElements.contains(item)) continue;
                if (item instanceof ModuleElement
                    && ((ModuleElement) item).isUnnamed()) continue;
                if (item instanceof PackageElement
                    && ((PackageElement) item).isUnnamed()) continue;

                URI loc = slice.config.locateElement(item);
                OutputContext outCtxt = slice.getPlainContext(loc, item);
                SourceContext itemCtxt = SourceContext.forElement(item);

                Element elem =
                    doc.createElementNS(STANDARD_SITEMAP_NS_URI, "item");
                unlisted.appendChild(elem);
                elem.setAttributeNS(XMLConstants.XML_NS_URI, "id",
                                    identify(item));
                elem.setAttributeNS(XLINK_NS_URI, "xlink:href", Utils
                    .relativize(navLoc, loc).toASCIIString());

                StringBuilder descr = new StringBuilder();
                if (!slice.writeSummary(descr::append, outCtxt, item))
                    slice.toHypertext(descr::append, itemCtxt, outCtxt,
                                      slice.getSyntheticDescription(item));
                elem.setAttribute("description",
                                  descr.toString().replaceAll("\n", ""));

                StringBuilder name = new StringBuilder();
                slice.writeElementReference(name::append, outCtxt, item,
                                            LinkContent.NORMAL);
                elem.setAttribute("name", name.toString());

                ElementVisitor<Void, Void> visitor =
                    new ElementKindVisitor9<>() {
                        @Override
                        public Void visitModule(ModuleElement e, Void p) {
                            List<PackageElement> membs = new ArrayList<>();
                            membs.addAll(ElementFilter
                                .packagesIn(e.getEnclosedElements()));
                            membs.sort((a, b) -> String.CASE_INSENSITIVE_ORDER
                                .compare(a.getQualifiedName().toString(),
                                         b.getQualifiedName().toString()));

                            populate(doc, elem, membs);
                            return null;
                        }

                        @Override
                        public Void visitPackage(PackageElement e, Void p) {
                            List<TypeElement> membs = new ArrayList<>();
                            membs.addAll(ElementFilter
                                .typesIn(e.getEnclosedElements()));
                            membs.sort((a, b) -> String.CASE_INSENSITIVE_ORDER
                                .compare(a.getQualifiedName().toString(),
                                         b.getQualifiedName().toString()));

                            populate(doc, elem, membs);
                            return null;
                        }

                        @Override
                        public Void visitType(TypeElement e, Void p) {
                            List<TypeElement> nestedTypes =
                                new ArrayList<>(ElementFilter
                                    .typesIn(e.getEnclosedElements()));
                            nestedTypes
                                .sort((a, b) -> String.CASE_INSENSITIVE_ORDER
                                    .compare(a.getSimpleName().toString(),
                                             b.getSimpleName().toString()));
                            List<TypeElement> innerTypes =
                                nestedTypes.stream()
                                    .filter(t -> !t.getModifiers()
                                        .contains(Modifier.STATIC))
                                    .collect(Collectors.toList());
                            nestedTypes.removeAll(innerTypes);

                            List<VariableElement> consts =
                                new ArrayList<>(ElementFilter
                                    .fieldsIn(e.getEnclosedElements()));
                            consts
                                .sort((a, b) -> String.CASE_INSENSITIVE_ORDER
                                    .compare(a.getSimpleName().toString(),
                                             b.getSimpleName().toString()));
                            List<VariableElement> fields = consts.stream()
                                .filter(f -> !f.getModifiers()
                                    .contains(Modifier.STATIC))
                                .collect(Collectors.toList());
                            consts.removeAll(fields);
                            List<VariableElement> globals = consts.stream()
                                .filter(f -> !f.getModifiers()
                                    .contains(Modifier.FINAL))
                                .collect(Collectors.toList());
                            consts.removeAll(globals);

                            List<ExecutableElement> constrs =
                                new ArrayList<>(ElementFilter
                                    .constructorsIn(e.getEnclosedElements()));
                            constrs
                                .sort((a, b) -> String.CASE_INSENSITIVE_ORDER
                                    .compare(a.toString(), b.toString()));

                            List<ExecutableElement> instanceMethods =
                                new ArrayList<>(ElementFilter
                                    .methodsIn(e.getEnclosedElements()));
                            instanceMethods
                                .sort((a, b) -> String.CASE_INSENSITIVE_ORDER
                                    .compare(a.toString(), b.toString()));
                            List<ExecutableElement> classMethods =
                                instanceMethods.stream()
                                    .filter(m -> m.getModifiers()
                                        .contains(Modifier.STATIC))
                                    .collect(Collectors.toList());
                            instanceMethods.removeAll(classMethods);

                            addGroup(doc, elem, nestedTypes,
                                     "static-classes");
                            addGroup(doc, elem, innerTypes, "inner-classes");
                            addGroup(doc, elem, consts, "constants");
                            addGroup(doc, elem, globals, "static-fields");
                            addGroup(doc, elem, fields, "fields");
                            addGroup(doc, elem, classMethods,
                                     "static-methods");
                            addGroup(doc, elem, constrs, "constructors");
                            addGroup(doc, elem, instanceMethods, "methods");

                            return null;
                        }
                    };
                item.accept(visitor, null);
            }

            /* Write out the full document. */
            Transformer tf =
                TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            try (OutputStream out = Files.newOutputStream(navFile)) {
                tf.transform(new DOMSource(doc), new StreamResult(out));
            } catch (IOException | TransformerException ex) {
                slice.config.report(Kind.ERROR, "output.stdmap.failure.write",
                                    ex, navFile);
            }
        } catch (ParserConfigurationException
            | TransformerConfigurationException ex) {
            slice.config.report(Kind.ERROR, "output.stdmap.failure.config",
                                ex);
        }
    }
}
