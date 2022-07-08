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

package uk.ac.lancs.polydoclot.imports;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import uk.ac.lancs.polydoclot.util.HttpSyntax;
import uk.ac.lancs.polydoclot.util.Utils;

/**
 * Identifies an external documentation installation. This class merely
 * records that an installation is at a given location, that at least
 * its meta-data exists at a potentially different and more convenient
 * location, and that the installation documents a number of modules not
 * mentioned in its meta-data. This class does not access the meta-data
 * until required (by calling
 * {@link #install(Map, Map, DocMappingFactory)}), which records the
 * result of part of that access as a {@link DocReference}, and then
 * populates a mapping from package/module name to that record. After
 * that, this class is redundant, and no more accesses to the remote
 * installation are required.
 * 
 * <p>
 * The constants {@link #PROPERTIES_NAME}, {@link #ELEMENT_LIST_NAME}
 * and {@value #PACKAGE_LIST_NAME} identify resources at the
 * installation's location containing its meta-data, and should be used
 * by doclets in generating their meta-data.
 * {@value #MODULE_LINE_PREFIX} specifies the prefix that module-aware
 * doclets should use in the resource
 * <samp>{@value #ELEMENT_LIST_NAME}</samp> to distinguish lines
 * specifying module names from those specifying package names.
 * 
 * @author simpsons
 */
public final class DocImport {
    /**
     * The URI prefix of the external installation
     */
    public final URI location;

    private final URI cache;

    private final Collection<String> manualMods;

    /**
     * This naming scheme is used by the standard doclet since about
     * JDK7. At this point, the doclet started using
     * <code>&lt;div id="<var>fragment</var>"&gt;</code> to mark
     * documentation on class members, allowing the old scheme
     * <code>&lt;a name="<var>fragment</var>"&gt;</code> to be
     * effectively deprecated. While this is arguably a more "proper"
     * way to address portions of an (X)HTML file, it has a problem in
     * that there are more restrictions on what can serve as a fragment
     * id. In particular, brackets, commas and spaces used in signatures
     * of executable members are forbidden. Identifiers could also be
     * considered case-insensitive.
     * 
     * <p>
     * This scheme is the same as {@link #OLD_JDK_SCHEME} for classes
     * and packages, but generates URI fragment identifiers compatible
     * with XML ids. A method's or constructor's signature is now
     * expressed by first appending a dash to the element's simple name.
     * For each parameter, the type is appended, with an <samp>:A</samp>
     * for each array dimension, and then a dash.
     * 
     * @todo Just noticed that JDK9 is also using the unerased
     * signature, even though it's not necessary, and could make things
     * harder to express if done completely (and there's no point in
     * doing it partially). Are they preparing to allow overloads in
     * source that don't differ after erasure? Anyway, a new macro
     * format is in order.
     * 
     * @resume The naming scheme used by the standard doclet since, erm,
     * about JDK7
     */
    public static final String JDK_SCHEMEx = "{?PACKAGE:{${PACKAGE}:\\\\.:/}"
        + "{?CLASS:/{CLASS}.html" + "{?FIELD:#{FIELD}}" + "{?EXEC:#{EXEC}"
        + "({@PARAMETER:I:{PARAMETER.{I}}" + "{?PARAMETER.{I}.VARARG:"
        + "{.\\:A:{PARAMETER.{I}.VARARG}}" + "...:"
        + "{?PARAMETER.{I}.DIMS:{.\\:A:{PARAMETER.{I}.DIMS}}}" + "}:,})}"
        + ":/package-summary.html}:{MODULE}-summary.html}";

    /**
     * This naming scheme is used by the standard doclet since about
     * JDK11. The main change is that every module has its own
     * subdirectory, and its members are in it.
     * 
     * @resume The naming scheme used by the standard doclet since, erm,
     * about JDK11
     */
    public static final String MODULE_JDK_SCHEME =
        "{?MODULE:{MODULE}/}" + "{?PACKAGE:{${PACKAGE}:\\\\.:/}"
            + "{?CLASS:/{CLASS}.html" + "{?FIELD:#{FIELD}}" + "{?EXEC:#{EXEC}"
            + "({@PARAMETER:I:{PARAMETER.{I}}" + "{?PARAMETER.{I}.VARARG:"
            + "{.\\:A:{PARAMETER.{I}.VARARG}}" + "...:"
            + "{?PARAMETER.{I}.DIMS:{.\\:A:{PARAMETER.{I}.DIMS}}}" + "}:,})}"
            + ":/package-summary.html}:module-summary.html}";

    /**
     * @resume The original naming scheme used by the standard doclet
     * until, erm, about JDK7
     */
    public static final String OLD_JDK_SCHEME =
        "{${PACKAGE}:\\\\.:/}" + "{?CLASS:/{CLASS}.html" + "{?FIELD:#{FIELD}}"
            + "{?EXEC:#{EXEC}" + "({@PARAMETER:I:{PARAMETER.{I}}"
            + "{?PARAMETER.{I}.DIMS:{.[]:{PARAMETER.{I}.DIMS}}}:,%20})}"
            + ":/package-summary.html}";

    /**
     * Create a reference to an external installation, with another
     * location as a (local) mirror of its meta-data. The original
     * installation will not be accessed, but its address will be used
     * to generate URIs referencing its packages and classes.
     * 
     * @param location the URI prefix of the external installation
     * 
     * @param cache the URI prefix of the local mirror
     */
    public DocImport(URI location, URI cache) {
        this(location, cache, Collections.emptySet());
    }

    /**
     * Create a reference to an external installation, with another
     * location as a (local) mirror of its meta-data. The original
     * installation will not be accessed, but its address will be used
     * to generate URIs referencing its packages and classes.
     * 
     * @param location the URI prefix of the external installation
     * 
     * @param cache the URI prefix of the local mirror
     * 
     * @param manualMods a set of module names to install with the
     * import
     */
    public DocImport(URI location, URI cache,
                     Collection<? extends String> manualMods) {
        this.location = Utils.assumeDirectory(location);
        this.cache = Utils.assumeDirectory(cache);
        this.manualMods = new HashSet<>(manualMods);
    }

    /**
     * Create a reference to an external installation. The remote site
     * will be accessed to obtain its meta-data.
     * 
     * @param location the URI prefix of the external installation
     */
    public DocImport(URI location) {
        this(location, location);
    }

    private Properties loadProperties() throws IOException {
        Properties docProps = new Properties();

        URLConnection conn =
            cache.resolve(PROPERTIES_NAME).toURL().openConnection();
        Properties connProps = new Properties();
        String rawType = conn.getContentType();
        if (rawType == null) return docProps;
        String type = new HttpSyntax.Tokenizer(rawType)
            .parseMediaTypeParameters(connProps);

        try {
            InputStream decoded = getDecodedStream(conn);

            if (type.equalsIgnoreCase("text/plain")) {
                String charset = connProps.getProperty("charset");
                if (charset == null)
                    docProps.load(decoded);
                else
                    docProps.load(new InputStreamReader(decoded, charset));
            } else if (type.equalsIgnoreCase("application/xml") ||
                type.equalsIgnoreCase("text/xml")) {
                docProps.loadFromXML(decoded);
            }
        } catch (FileNotFoundException ex) {
            /* Ignore - it never existed. */
        }
        return docProps;
    }

    private Reader openTextFile(String name) throws IOException {
        URI loc = cache.resolve(name);
        URLConnection conn = loc.toURL().openConnection();

        /* Check the type and extract the character encoding. */
        String rawType = conn.getContentType();
        if (rawType == null) rawType = "content/unknown";
        Properties connProps = new Properties();
        String type = new HttpSyntax.Tokenizer(rawType)
            .parseMediaTypeParameters(connProps);
        if (!type.equals("text/plain") && !type.equals("content/unknown"))
            throw new IOException("content is not plain text but " + type
                + " at " + loc);

        InputStream decoded = getDecodedStream(conn);
        String charset =
            connProps.getProperty("charset", Charset.defaultCharset().name());
        Reader streamReader = new InputStreamReader(decoded, charset);
        return streamReader;
    }

    private Reader openPackageList() throws IOException {
        try {
            return openTextFile(ELEMENT_LIST_NAME);
        } catch (IOException ex) {
            return openTextFile(PACKAGE_LIST_NAME);
        }
    }

    /**
     * The prefix of lines identifying modules, rather than packages, in
     * the file
     * <samp><var>docRoot</var>{@value #ELEMENT_LIST_NAME}</samp>
     */
    public static final String MODULE_LINE_PREFIX = "module:";

    /**
     * Access the installation's meta-data to determine what modules and
     * packages it documents, and how to link to them and their
     * elements. First, the file <samp>{@value #PROPERTIES_NAME}</samp>
     * is resolved against {@link #location}, and its content is fetched
     * and interpreted as an XML {@link Properties}. A mapping from Java
     * element to the URI of its documentation is created by passing the
     * properties and the installtion's location to the
     * {@link DocMappingFactory}, and using the result, the properties
     * and the location to create a {@link DocReference}. Finally, the
     * list of packages and modules documented by the installation
     * (provided by the files <samp>{@value #ELEMENT_LIST_NAME}</samp>
     * and <samp>{@value #PACKAGE_LIST_NAME}</samp>, or through
     * {@link #DocImport(URI, URI, Collection)}) is used to populate
     * mappings from package/module name to the {@link DocReference}.
     * 
     * @param into the package mapping to be populated
     * 
     * @param modMap the module mapping to be populated
     * 
     * @param mappingFactory a way to map program elements into URIs
     * 
     * @throws IOException if an error occurs in accessing the
     * installation
     */
    public void install(Map<? super String, ? super DocReference> into,
                        Map<? super String, ? super DocReference> modMap,
                        DocMappingFactory mappingFactory)
        throws IOException {
        Properties docProps = loadProperties();
        DocMapping mapping =
            mappingFactory.createMapping(location, cache, docProps);
        DocReference ref = new DocReference(location, docProps, mapping);

        /* For Java 9 installations that don't list their own modules,
         * use the manually provided list. */
        for (String modName : manualMods)
            modMap.put(modName, ref);
        manualMods.clear();

        /* Load the package-list file from the cache location. */
        try (BufferedReader reader = new BufferedReader(openPackageList())) {
            for (String line = reader.readLine(); line != null;
                 line = reader.readLine()) {
                line = line.trim();
                if (line.startsWith(MODULE_LINE_PREFIX)) {
                    modMap.put(line.substring(MODULE_LINE_PREFIX.length()),
                               ref);
                } else {
                    into.put(line, ref);
                }
            }
        }
    }

    private static InputStream getDecodedStream(URLConnection conn)
        throws IOException {
        return decode(conn.getInputStream(), conn.getContentEncoding());
    }

    private static InputStream decode(InputStream in, String compression)
        throws IOException {
        String[] encodings =
            compression == null ? new String[0] : commaSep.split(compression);
        Collections.reverse(Arrays.asList(encodings));

        for (String code : encodings) {
            if (code.equals("identity"))
                ;
            else if (code.equals("x-gzip") || code.equals("gzip"))
                in = new java.util.zip.GZIPInputStream(in);
            /* else if (code.equals("x-compress") ||
             * code.equals("compress")) ; */
            else if (code.equals("deflate"))
                in = new java.util.zip.DeflaterInputStream(in);
            else
                throw new IOException("Unknown encoding " + code);
        }
        return in;
    }

    /**
     * The name of the file relative to the installation root that lists
     * packages defined by the installation
     */
    public static final String PACKAGE_LIST_NAME = "package-list";

    /**
     * The name of the file relative to the installation root that lists
     * modules defined by the installation
     */
    public static final String ELEMENT_LIST_NAME = "element-list";

    /**
     * The name of the file relative to the installation root that
     * defines the properties of the installation
     */
    public static final String PROPERTIES_NAME = "doc-properties.xml";

    private static final Pattern commaSep = Pattern.compile("(\\s*,)+\\s*");
}
