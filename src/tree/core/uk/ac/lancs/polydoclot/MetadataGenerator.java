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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Properties;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;

import uk.ac.lancs.polydoclot.imports.DocImport;
import uk.ac.lancs.polydoclot.util.MacroFormatter;

/**
 * Generates the metadata that other doclets use to form links to
 * components of the generated documentation. From the output document
 * root (see {@link Configuration#outputDirectory}), three files are
 * created:
 * 
 * <dl>
 * 
 * <dt><samp>{@value DocImport#ELEMENT_LIST_NAME}</samp>
 * 
 * <dd>Encoded in UTF-8 (does any specification say otherwise?), each
 * line identifies a package or module documented within the
 * installation. Module lines are prefixed with
 * <samp>{@value DocImport#MODULE_LINE_PREFIX}</samp>, and package lines
 * have no prefix.
 * 
 * <dt><samp>{@value DocImport#PACKAGE_LIST_NAME}</samp>
 * 
 * <dd>Encoded in UTF-8, each line identifies a package documented
 * within the installation. This is provided for legacy doclets that are
 * module-unaware. It contains no more information than
 * <samp>{@value DocImport#ELEMENT_LIST_NAME}</samp>, so module-aware
 * doclets should look for that first.
 * 
 * <dt><samp>{@value DocImport#PROPERTIES_NAME}</samp>
 * 
 * <dd>This is an XML-encoded {@link Properties} object describing other
 * aspects of the installation. Only one property is defined:
 * 
 * <dl>
 * 
 * <dt><samp>{@value Polydoclot#SCHEME_PROPERTY_NAME}</samp>
 * 
 * <dd>This string, compatible with {@link MacroFormatter}, describes
 * how to link to an element such as a module, package, class or member.
 * This doclet uses the following scheme:
 * 
 * <pre>
 * {@value Polydoclot#POLYDOCLOT_SCHEME}
 * </pre>
 * 
 * </dl>
 * 
 * <p>
 * These files are additional generated in the offline directory, if
 * specified with <kbd>{@linkplain Polydoclot#getSupportedOptions()
 * -do}</kbd>.
 * 
 * </dl>
 * 
 * @author simpsons
 */
public final class MetadataGenerator {
    private final Configuration config;

    /**
     * Create a generator for a given configuration.
     * 
     * @param config the configuration
     */
    public MetadataGenerator(Configuration config) {
        this.config = config;
    }

    private void report(String key, Exception ex, Object... ctxt) {
        Object[] newCtxt = new Object[ctxt.length + 1];
        newCtxt[0] = ex.getMessage();
        System.arraycopy(ctxt, 0, newCtxt, 1, ctxt.length);
        config.report(Kind.ERROR, key, newCtxt);
    }

    private void writeOfflineFiles(Path dir,
                                   Collection<PackageElement> pkgList,
                                   Collection<ModuleElement> modList) {
        Path oldFile = dir.resolve(DocImport.PACKAGE_LIST_NAME);
        try {
            writeList(oldFile, pkgList, null);
        } catch (IOException ex) {
            report("output.package-list.failure.write", ex, oldFile);
        }

        Path newFile = dir.resolve(DocImport.ELEMENT_LIST_NAME);
        try {
            writeList(newFile, pkgList, modList);
        } catch (IOException ex) {
            report("output.element-list.failure.write", ex, newFile);
        }

        Properties docProps = new Properties();
        docProps.setProperty(Polydoclot.SCHEME_PROPERTY_NAME,
                             Polydoclot.POLYDOCLOT_SCHEME
                                 + config.hypertextLinkSuffix);
        Path propsFile = dir.resolve(DocImport.PROPERTIES_NAME);
        try (OutputStream out = Files.newOutputStream(propsFile)) {
            docProps.storeToXML(out, "Polyglot Doclet");
        } catch (IOException ex) {
            report("output.meta-data.failure.write", ex, propsFile);
        }
    }

    /**
     * Generate the metadata used by referring doclets.
     */
    public void run() {
        Collection<PackageElement> pkgList =
            ElementFilter.packagesIn(config.env.getIncludedElements());
        pkgList.removeAll(config.excludedElements);
        Collection<ModuleElement> modList =
            ElementFilter.modulesIn(config.env.getIncludedElements());
        modList.removeAll(config.excludedElements);

        writeOfflineFiles(config.outputDirectory, pkgList, modList);
        if (config.offlineDirectory != null)
            writeOfflineFiles(config.offlineDirectory, pkgList, modList);

        Path defStyles = config.outputDirectory.resolve(DEFAULT_STYLES_NAME);
        try (InputStream in =
            getClass().getResourceAsStream("default-styles.css")) {
            try {
                Files.copy(in, defStyles);
            } catch (IOException ex) {
                report("output.default-stylesheet.failure.copy", ex,
                       defStyles);
            }
        } catch (IOException ex) {
            report("output.default-stylesheet.failure.open", ex,
                   "default-styles.css", getClass().getPackage().getName());
        }

        if (config.styleSource != null) {
            Path copiedStyles =
                config.outputDirectory.resolve(COPIED_STYLES_NAME);
            try {
                Files.copy(config.styleSource, copiedStyles);
            } catch (IOException ex) {
                report("user-content.stylesheet.copied.failure", ex,
                       config.styleSource, copiedStyles);
            }
        }

        {
            Path contentTypesFile =
                config.outputDirectory.resolve(CONTENT_TYPES_NAME);
            try (
                PrintWriter out = new PrintWriter(new OutputStreamWriter(Files
                    .newOutputStream(contentTypesFile), StandardCharsets.UTF_8))) {
                out.printf("DEFAULT: t=text/plain c=UTF-8\n");
                out.printf(".xml: t=application/xml\n");
                out.printf(".css: t=text/css c=UTF-8\n");
                out.printf("%s: t=text/html\n", config.hypertextFileSuffix);
                for (SliceSpecification spec : config.sliceSpecs) {
                    out.printf("%s: c=%s l=%s\n",
                               spec.suffix.isEmpty() ? "DEFAULT"
                                   : spec.suffix,
                               spec.charset,
                               spec.locale.toString().replace('_', '-'));
                }
            } catch (IOException ex) {
                report("output.meta-data.failure.write", ex,
                       contentTypesFile);
            }
        }
    }

    /**
     * The name of the file relative to the document base into which
     * content-negotiation configuration should be written
     */
    public static final String CONTENT_TYPES_NAME = "content-types.tab";

    /**
     * The name of the file relative to the document base into which the
     * doclet's default styles should be added
     */
    public static final String DEFAULT_STYLES_NAME = "default-styles.css";

    /**
     * The name of the file relative to the document base into which
     * copied user styles should be written
     */
    public static final String COPIED_STYLES_NAME = "copied-styles.css";

    private void writeList(Path file,
                           Collection<? extends PackageElement> pkgList,
                           Collection<? extends ModuleElement> modList)
        throws IOException {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(Files
            .newOutputStream(file), StandardCharsets.UTF_8))) {
            for (PackageElement pkg : pkgList)
                out.printf("%s\n", pkg.getQualifiedName());
            if (modList != null) for (ModuleElement mod : modList)
                if (!mod.isUnnamed())
                    out.printf("%s%s\n", DocImport.MODULE_LINE_PREFIX,
                               mod.getQualifiedName());
        }
    }
}
