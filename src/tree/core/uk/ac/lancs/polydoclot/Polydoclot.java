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
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import uk.ac.lancs.polydoclot.imports.DocImport;
import uk.ac.lancs.polydoclot.util.MacroFormatter;
import uk.ac.lancs.polydoclot.util.Utils;

/**
 * Generates HTML documentation from Javadoc comments, supporting
 * multiple simultaneous language-encoding-suffix slices, and various
 * extra tags.
 * 
 * <p>
 * See {@link #getSupportedOptions()} for command-line switches.
 * 
 * {@h1 Recognized tags}
 * 
 * <p>
 * The following additional tags are recognized:
 * 
 * <dl>
 * 
 * <dt><code>&#123;&#64;plainvalue <var>ref</var> <var>fmt</var>&#125;</code>
 * 
 * <dd>
 * <p>
 * Expands to the value of a constant, but without any mark-up or
 * escaping. If <var>ref</var> is omitted or <code>'.'</code>, the
 * documented element's value is expanded to. <var>fmt</var> is a format
 * specifier for {@link String#format(String, Object...)}, and defaults
 * to <samp>s</samp> if omitted.
 * 
 * <p>
 * Note that an IDE will likely not recognize that the first argument of
 * this tag is an element reference, so it won't be able to
 * auto-complete it, nor rename it.
 * 
 * <dt><code>&#64;resume</code>
 * <dt><code>&#64;summary</code> (prior to JDK10)
 * 
 * <dd>
 * <p>
 * Specify an alternative to the “first sentence” implicitly delimited
 * by the first full stop followed by white space. This should primarily
 * be used where the short description should describe what an element
 * is, rather than what it does.
 * 
 * <p>
 * Note that <code>&#64;summary</code> won't work if you use
 * <kbd>javadoc</kbd> from JDK10, as it will mistake it for the new
 * standard in-line tag <code>&#123;&#64;summary&#125;</code>, even
 * though they are syntactically distinct.
 * 
 * <dt><code>&#64;title <var>title</var></code> (overview only)</dt>
 * <dt><code>&#64;shortTitle <var>short-title</var></code> (overview
 * only)</dt>
 * 
 * <dd>
 * <p>
 * Set the name of the software being documented. <var>title</var>
 * appears in the overview, and as the “Distribution” on each page.
 * <var>short-title</var> appears in the title of each page.
 * 
 * <dt><code>&#64;slice <var>lang</var>[,<var>suffix</var>[,<var>charset</var>]]</code>
 * (overview only)</dt>
 * 
 * <dd>
 * <p>
 * Generate a slice of the documentation with the given language, suffix
 * and character encoding. (The run-time encoding is used by default,
 * and the suffix is an empty string by default.) The language choice
 * affects the various headings and labels that appear in the content
 * not supplied in the Javadoc comments.
 * 
 * <p>
 * Multiple slices can be specified, and require distinct suffixes.
 * 
 * <dt><code>&#123;&#64;select <var>lang-else-content</var>&#125;</code></dt>
 * <dt><code>&#123;&#64;lang <var>lang</var> <var>content</var>&#125;</code></dt>
 * <dt><code>&#123;&#64;else <var>content</var>&#125;</code></dt>
 * 
 * <dd>
 * <p>
 * When expanded, <code>&#64;select</code> consults its content for
 * <code>&#64;lang</code> and <code>&#64;else</code> tags, ignoring
 * content not in such tags. The first tag matching the environment in
 * which the <code>&#64;select</code> tag is being expanded is chosen,
 * and its content is expanded. <code>&#64;lang</code> matches if its
 * language specification <var>lang</var> matches the slice being
 * generated. <code>&#64;else</code> matches unconditionally. This
 * allows multilingual comments to be expressed, e.g.,
 * <code>&#123;&#64;select &#123;&#64;lang fr Bonjour!&#125; &#123;&#64;lang eo Saluton!&#125; &#123;&#64;else Hello!&#125;&#125;</code>.</dd>
 * 
 * <dt><code>&#64;undocumented</code> (any class or member)</dt>
 * 
 * <dd>
 * <p>
 * Exclude the documented element from generated documentation. This
 * also results in anything it contains being excluded, and anything
 * extending or implementing it. Its main use is to hide
 * <code>main</code> application methods, and constants of
 * primitive/string type, so their values can be referenced with
 * <code>&#123;&#64;value&#125;</code>.</dd>
 * 
 * <dt><code class=
 * "java">&#64;group <var>id</var> <var>title</var></code> (overview
 * only)</dt>
 * <dt><code class=
 * "java">&#64;package <var>id</var> <var>package-name</var>...</code>
 * (overview only)</dt>
 * <dt><code class=
 * "java">&#64;module <var>id</var> <var>module-name</var>...</code>
 * (overview only)</dt>
 * 
 * <dd>
 * <p>
 * <code>&#64;group</code> defines a new group with an internal
 * identifier <var>id</var> and title <var>title</var>.
 * <code>&#64;package</code> and <code>&#64;module</code> add the
 * specified package or module to the identified group. The generated
 * documentation overview will list modules and packages in these
 * groups, in the order that their ids are first mentioned, headed by
 * their corresponding titles. Packages belonging to modules are not
 * listed in the overview, even if assigned to a group.
 * 
 * <p>
 * The <kbd>-group</kbd> switch overrides these declarations, but the
 * embedded tags are preferable because they allow the group details to
 * be kept with the source. <var>title</var> may also contain HTML and
 * in-line Javadoc tags.</dd>
 * 
 * <dt><code>&#64;default</code> (any non-static method)</dt>
 * <dt><code>&#64;apiNote</code> (any non-static method)</dt>
 * <dt><code>&#64;implSpec</code> (any non-static method)</dt>
 * <dt><code>&#64;implNote</code> (any non-static method)</dt>
 * 
 * <dd>
 * <p>
 * The content is displayed after the method's detailed description. It
 * appears to be part of that content, but cannot be copied using
 * <code>&#123;&#64;inheritDoc&#125;</code>
 * 
 * <p>
 * <code>&#64;default</code> blocks appear first, with no header.
 * <code>&#64;apiNote</code> blocks appear next, then
 * <code>&#64;implSpec</code> blocks, then <code>&#64;implNote</code>
 * blocks, each block with its own header.
 * 
 * <dt><code>&#64;constructor</code> (any method returning
 * non-<code>void</code>)</dt>
 * 
 * <dd>
 * <p>
 * This causes the method to appear in the constructor list for its
 * return type (provided they are part of the same documentation, of
 * course). If the return type is an array, the base element type is
 * used.</dd>
 * 
 * <dt><code>&#64;pname <var>id</var> <var>name</var></code> (overview
 * only)</dt>
 * <dt><code>&#64;paddr <var>id</var> <var>snail-mail-addr</var></code>
 * (overview only)</dt>
 * <dt><code>&#64;pemail <var>id</var> <var>email-addr</var></code>
 * (overview only)</dt>
 * <dt><code>&#64;ptel <var>id</var> <var>phone-num</var></code>
 * (overview only)</dt>
 * <dt><code>&#64;plink <var>id</var> <var>uri</var></code> (overview
 * only)</dt>
 * 
 * <dd>
 * <p>
 * These associate a contributor's details with an identifier. An
 * <code>&#64;author <var>id</var></code> tag will then expand to these
 * details on other pages. Each contributor will also appear in the
 * overview.</dd>
 * 
 * <dt><code>&#64;palias <var>id</var> <var>alias</var>...</code>
 * (overview only)</dt>
 * 
 * <dd>
 * <p>
 * This identifies a contributor by additional aliases.</dd>
 * 
 * <dt><code>&#64;pdesc <var>id</var>...</code> (overview only)</dt>
 * 
 * <dd>
 * <p>
 * <code>&#64;author <var>id</var></code> tags for these ids expand into
 * a smaller link to the detail in the overview, rather than into the
 * detail directly.</dd>
 * 
 * <dt><code>&#123;&#64;keyword $<var>rcs-tag</var>: <var>value</var>$&#125;</code></dt>
 * 
 * <dd>
 * <p>
 * This expands to <var>value</var>, but allows the revision-control
 * keyword to be stripped out.
 * 
 * <dt><code>&#123;&#64;h1 <var>content</var>&#125;</code></dt>
 * <dt><code>&#123;&#64;h2 <var>content</var>&#125;</code></dt>
 * <dt><code>&#123;&#64;h3 <var>content</var>&#125;</code></dt>
 * 
 * <dd>
 * <p>
 * These expand to headings appropriate for the context in which they
 * are expanded.</dd>
 * 
 * </dl>
 * 
 * <div id="meta-data">
 * 
 * {@h1 Generated meta-data}
 * 
 * <p>
 * The generated documentation includes the following meta-data files
 * (relative to the installation prefix):
 * 
 * <dl>
 * 
 * <dt><samp>{@value MetadataGenerator#CONTENT_TYPES_NAME}</samp>
 * 
 * <dd>
 * <p>
 * This file lists content meta-data for various suffixes. Each line has
 * the format:
 * 
 * <pre>
 * <var>suffix</var>: <var>attribute</var>=<var>value</var> ...
 * </pre>
 * 
 * <p>
 * Each file with a name component matching a specified suffix has the
 * listed attributes. A suffix of <samp>DEFAULT</samp> applies to all
 * files. The attributes are:
 * 
 * <dl>
 * 
 * <dt><samp>t</samp>
 * 
 * <dd>Plain content type (e.g., <samp>text/html</samp>)
 * 
 * <dt><samp>c</samp>
 * 
 * <dd>Character encoding (e.g., <samp>UTF-8</samp>)
 * 
 * <dt><samp>l</samp> (el)
 * 
 * <dd>Natural language (e.g., <samp>en-GB</samp>)
 * 
 * </dl>
 * 
 * <p>
 * A typical example is as follows:
 * 
 * <pre>
 * DEFAULT: t=text/plain c=UTF-8
 * .xml: t=application/xml
 * .css: t=text/css c=UTF-8
 * .html: t=text/html
 * .en-GB: c=UTF-8 l=en-GB
 * </pre>
 * 
 * <p>
 * This meta-data is intended for scripts that access an installation as
 * an aggregate file such as a zip, and perform content negotiation on
 * the embedded files, something that is not available to (say) Apache
 * HTTPd natively. Frequently generated package installations can then
 * be updated atomically as single files, ensuring that redundant files
 * will not be left behind by previous installations.
 * 
 * <p>
 * The switch <kbd>{@linkplain Polydoclot#getSupportedOptions()
 * -z}</kbd> allows a zipfile to be generated directly. <samp><a href=
 * "https://scc-forge.lancaster.ac.uk/svn-repos/utils/ss-scripts/trunk/webzip.cgi">webzip.cgi</a></samp>
 * and <samp><a href=
 * "https://scc-forge.lancaster.ac.uk/svn-repos/utils/ss-scripts/trunk/webzip.php">webzip.php</a></samp>
 * are CGI and PHP scripts that can automatically read the meta-data and
 * serve content according to <samp>content-types.tab</samp> from a
 * zipfile.
 * 
 * 
 * <dt><samp>{@value DocImport#PACKAGE_LIST_NAME}</samp>
 * 
 * <dd>
 * <p>
 * This file lists packages documented by this installation, one per
 * line. The encoding is UTF-8 (but is that specified anywhere?).
 * 
 * <dt><samp>{@value DocImport#ELEMENT_LIST_NAME}</samp>
 * 
 * <dd>
 * <p>
 * This file lists packages and modules documented by this installation,
 * one per line. The encoding is UTF-8 (but is that specified
 * anywhere?). Each line identifying a module is prefixed with
 * <samp>{@value DocImport#MODULE_LINE_PREFIX}</samp>.
 * 
 * <dt><samp>{@value DocImport#PROPERTIES_NAME}</samp>
 * 
 * <dd>
 * <p>
 * This file can be loaded by
 * {@link Properties#loadFromXML(java.io.InputStream)}, and contains the
 * following properies:
 * 
 * <dl>
 * 
 * <dt><samp>{@value #SCHEME_PROPERTY_NAME}</samp>
 * 
 * <dd>
 * <p>
 * This holds a string compatible with {@link MacroFormatter} that can
 * be expanded against a set of properties to generate the URI relative
 * to the installation prefix of any module, package, class or member
 * documented by this installation.
 * {@link Configuration#setElementProperties(Properties, javax.lang.model.element.Element)}
 * sets the properties for a given element.
 * 
 * </dl>
 * 
 * </dl>
 * 
 * </div>
 * 
 * @author simpsons
 */
public class Polydoclot implements Doclet {
    /**
     * Get the name of this doclet, to use in documentation.
     * 
     * @return the string <samp>Polydoclot</samp>
     */
    @Override
    public String getName() {
        return "Polydoclot";
    }

    /* Fields gathered from arguments and overview file */
    private Locale locale;

    private ResourceBundle messageBundle;

    private Reporter reporter;

    private String overviewFile;

    private Path outputZipFile = null;

    private Path offlineDirectory = null;

    private Path outputDirectory = Paths.get(System.getProperty("user.dir"));

    private Path diagnosticsDirectory;

    private List<DocImport> imports = new ArrayList<>();

    private String hypertextLinkSuffix = ".html";

    private boolean listUndocumented = false;

    private Path styleSource;

    private URI style;

    private final Map<Path, String> dirToJar = new HashMap<>();

    private final Map<String, String> jarToVersion = new HashMap<>();

    private List<SliceSpecification> sliceSpecs = new ArrayList<>();

    private String rawTitle;

    private String rawShortTitle;

    private int nextGroupKey = 0;

    private String tidyProgram = "tidy";

    private Map<Object, Collection<String>> groupings = new HashMap<>();

    private Map<Object, String> rawGroupTitles = new LinkedHashMap<>();

    /**
     * This is the naming scheme used by the Polydoclot doclet,
     * compatible with {@link MacroFormatter}. It avoids including any
     * strings that could be misinterpreted as suffixes used by
     * webservers to determine content type, encoding, language, etc,
     * including the trailing HTML suffix (which you can safely add to
     * this string before parsing it).
     * 
     * @resume The scheme used by Polydoclot
     */
    public static final String POLYDOCLOT_SCHEME =
        "{?PACKAGE:{${PACKAGE}:\\\\.:/}" + "{?CLASS:/{${CLASS}:\\\\.:\\\\$}"
            + "{?FIELD:-field-{FIELD}:{?EXEC:-{?CONSTR:constr:method-{EXEC}}"
            + "{@PARAMETER:I:" + "/{?PARAMETER.{I}.DIMS:{PARAMETER.{I}.DIMS}:0}"
            + "{${PARAMETER.{I}}:\\\\.:\\\\$}}"
            + "}}:/package-summary}:{${MODULE}:\\\\.:\\\\$}-module}";

    /**
     * @resume The scheme used by the now defunct SSDoclet
     * 
     * @undocumented
     */
    public static final String OLD_SSDOC_SCHEME =
        "{${PACKAGE}:\\\\.:/}" + "{?CLASS:/{CLASS}.html" + "{?FIELD:#{FIELD}}"
            + "{?EXEC:#{EXEC}" + "\\:{@PARAMETER:I:{PARAMETER.{I}}"
            + "{?PARAMETER.{I}.DIMS:-{PARAMETER.{I}.DIMS}}\\::}}"
            + ":/package-summary.html}";

    /**
     * List the options recognized by this doclet.
     * 
     * <p>
     * The following doclet-specific switches are recognized:
     * 
     * <dl>
     * 
     * <dt><kbd>-group <var>title</var>
     * <var>package-name</var>:...</kbd></dt>
     * 
     * <dd>
     * <p>
     * Place the listed packages in a group with the specified title.
     * This grouping appears only in the generated overview file.
     * 
     * <p>
     * This switch is provided for compatibility with the standard
     * doclet. However, the Javadoc tags <code>&#64;group</code>,
     * <code>&#64;package</code> and <code>&#64;module</code> should be
     * used instead, as they allow modules to be grouped as well as
     * packages, and group titles may contain in-line tags. They are
     * also embedded in the source (in the overview source file), so
     * they can stay with the installation they apply to. If
     * <kbd>-group</kbd> is used, it disables the tags.
     * 
     * <dt><kbd>-undocumented</kbd></dt>
     * 
     * <dd>
     * <p>
     * Generate an additional section in the overview listing all
     * exposed elements that lack documentation. This is intended to
     * allow the author to quickly locate missed items.
     * 
     * <dt><kbd>-nosuffix</kbd></dt>
     * 
     * <dd>
     * <p>
     * The doclet normally generates internal links to HTML pages with
     * the suffix <samp>.html</samp>. This switch disables that suffix
     * in the referring links (but not in the file names), so that the
     * URIs are decoupled from the underlying technology delivering the
     * content.
     * 
     * <p>
     * For example, it means that the class <code>org.example.Foo</code>
     * will be linked to as <samp>org/example/Foo</samp>, rather than
     * <samp>org/example/Foo.html</samp>.
     * 
     * <dt><kbd>-link <var>remote-prefix</var></kbd></dt>
     * <dt><kbd>-linkoffline <var>remote-prefix</var>
     * <var>local-prefix</var></kbd></dt>
     * <dt><kbd>-linkmods <var>remote-prefix</var>
     * <var>module-name</var>,...</kbd></dt>
     * <dt><kbd>-linkofflinemods <var>remote-prefix</var>
     * <var>local-prefix</var></kbd></dt>
     * 
     * <dd>
     * <p>
     * These switches (the first two compatible with the standard
     * doclet) allow the generated documentation to refer to modules,
     * packages, classes and members of a remote documentation
     * installation, and can be used multiple times to link to multiple
     * installations.
     * 
     * <p>
     * For each remote installation, two URI prefixes can be provided.
     * <var>remote-prefix</var> is the actual location of the
     * documentation, and the generated documentation will therefore
     * contains URIs with this prefix. <var>local-prefix</var> is a
     * partial copy of the remote documentation, and could be a local
     * directory, but it defaults to <var>remote-prefix</var>. When a
     * remote installation is specified, various resources whose URIs
     * are prefixed by <var>local-prefix</var> are fetched to obtain
     * meta-data about the remote installation. Only the meta-data files
     * are required at <var>local-prefix</var>, not the full
     * installation. These meta-data files include the following:
     * 
     * <ul>
     * 
     * <li>The resource
     * <samp><var>local-prefix</var>{@value DocImport#ELEMENT_LIST_NAME}</samp>
     * tells this doclet which packages and modules are provided by the
     * installation.
     * 
     * <li>The resource
     * <samp><var>local-prefix</var>{@value DocImport#PACKAGE_LIST_NAME}</samp>,
     * is fetched if <samp>{@value DocImport#ELEMENT_LIST_NAME}</samp>
     * is unavailable, and lacks module information. This is the case
     * for the standard doclet in JDK9 and earlier (even though modules
     * are introduced in 9). Use <kbd>-linkmods</kbd> and
     * <kbd>-linkofflinemods</kbd> to provide the module list separately
     * for JDK9 installations that are module-aware but don't provide
     * the module list themselves.
     * 
     * <li>The resource
     * <samp><var>local-prefix</var>{@value DocImport#PROPERTIES_NAME}</samp>,
     * if available, tells this doclet how to map specific modules,
     * packages, classes and members to their URIs within the
     * installation. If absent, a JDK9 scheme is assumed.
     * 
     * </ul>
     * 
     * <p>
     * They are described in the section
     * <a href="#meta-data"><cite>Generated meta-data</cite></a>.
     * 
     * <p>
     * Note that URI prefixes can be presented without a trailing slash,
     * but are normalized by appending one if not present.
     * 
     * <dt><kbd>-title <var>title</var></kbd></dt>
     * <dt><kbd>-windowTitle <var>short-title</var></kbd></dt>
     * 
     * <dd>
     * <p>
     * <kbd>-title</kbd> specifies the full distribution title as it
     * appears in the generated overview and in the main content of most
     * pages. The short title is only used in the
     * <code>&lt;title&gt;</code> elements of each page, and defaults to
     * the full title.
     * 
     * <p>
     * These switches are provided for standard doclet compatibility.
     * Instead, you should use <code>&#64;title</code> and
     * <code>&#64;shortTitle</code> in the source overview file. This
     * keeps the information with the corresponding source, and these
     * tags can contain in-line tags too.
     * 
     * <dt><kbd>-slice
     * <var>lang</var>[,<var>suffix</var>,[<var>charset</var>]]</kbd></dt>
     * 
     * <dd>
     * <p>
     * Generate a slice of the documentation with the given language,
     * filename suffix (defaults to an empty string) and character
     * encoding (defaults to run-time locale). The language choice
     * affects the headers and labels of generated content not present
     * in the Javadoc comments, and the expansion of
     * <code>&#123;&#64;select&#125;</code> tags.
     * 
     * <p>
     * Multiple slices can be specified, and require distinct suffixes
     * so their files don't overwrite each other.
     * 
     * <p>
     * Slice information is better expressed in the overview source
     * file, using the <code>&#64;slice</code> tag, as it is better kept
     * with the source code it applies to. Specification of a slice with
     * <kbd>-slice</kbd> suppressed the embedded slice specifications.
     * 
     * <dt><kbd>-stylesheet <var>file</var></kbd></dt>
     * <dt><kbd>-stylesheetfile <var>file</var></kbd></dt>
     * 
     * <dd>
     * <p>
     * The content of the specified file is copied to
     * <samp>{@value MetadataGenerator#COPIED_STYLES_NAME}</samp> in the
     * output directory, and linked as a stylesheet in generated HTML
     * files. These styles are applied after the built-in defaults, but
     * before external styles.
     * 
     * <dt><kbd>--tidy <var>executable</var></kbd></dt>
     * <dt><kbd>--no-tidy</kbd></dt>
     * 
     * <dd>
     * <p>
     * Specify the executable for
     * <a href="https://www.w3.org/People/Raggett/tidy/">HTMLTidy</a>.
     * The default is <kbd>tidy</kbd>. <kbd>--no-tidy</kbd> turns off
     * tidying.
     * 
     * <dt><kbd>-stylesheeturi <var>uri</var></kbd></dt>
     * 
     * <dd>
     * <p>
     * Generated HTML links to this URI as a stylesheet. Its styles are
     * applied after the copied ones and the built-in defaults.
     * 
     * <dt><kbd>-d <var>dir</var></kbd></dt>
     * 
     * <dd>
     * <p>
     * Specify the output directory. The directory must exist; it is not
     * created, but necessary subdirectories are.
     * 
     * <dt><kbd>-do <var>dir</var></kbd></dt>
     * <dt><kbd>--offline <var>dir</var></kbd></dt>
     * 
     * <dd>
     * <p>
     * Specify the optional offline output directory. The directory must
     * exist; it is not created. Only the files
     * <samp>{@value DocImport#PACKAGE_LIST_NAME}</samp>,
     * <samp>{@value DocImport#ELEMENT_LIST_NAME}</samp> and
     * <samp>{@value DocImport#PROPERTIES_NAME}</samp> will be written
     * here. This allows a lightweight copy of the documentation to be
     * accessed offline with the likes of <kbd>-linkoffline</kbd>.
     * 
     * <dt><kbd>-z <var>zipfile</var></kbd></dt>
     * 
     * <dd>
     * <p>
     * Write output to a zipfile instead of a directory. The file will
     * be created if necessary, but will not be emptied. Ensure the file
     * does not exist to ensure that it will contain only
     * doclet-generated content.
     * 
     * <dt><kbd>-dd <var>dir</var></kbd></dt>
     * <dt><kbd>--diagnostics <var>dir</var></kbd></dt>
     * 
     * <dd>
     * <p>
     * Specify the diagnostic directory. Certain output is sent here,
     * such as HTMLTidy errors and pre-tidied HTML (UTF-8 encoded,
     * regardless of the slice's charset, as it's simpler to call
     * HTMLTidy consistently using UTF-8). Line numbers of HTMLTidy
     * diagnostics will refer to lines of these files. The directory
     * must exist; it is not created, but necessary subdirectories are.
     * 
     * <dt><kbd>-jardirs <var>jar-name</var>
     * <var>tree</var>:...</kbd></dt>
     * 
     * <dd>
     * <p>
     * Specify that the listed Java source trees contribute to the named
     * jar. Jar information is then listed alongside packages. This is
     * primarily for integration with <a href=
     * "https://www.lancaster.ac.uk/~simpsons/software/pkg-jardeps">Jardeps</a>.
     * 
     * <dt><kbd>--overview-file <var>file</var></kbd></dt>
     * <dt><kbd>-overview <var>file</var></kbd></dt>
     * <dt><kbd>-o <var>file</var></kbd></dt>
     * 
     * <dd>
     * <p>
     * Specify the location of the source overview file.
     * 
     * </dl>
     * 
     * @return a set of descriptions of the available options
     */
    @Override
    public Set<? extends Option> getSupportedOptions() {
        Option[] opts = { new Option() {
            @Override
            public int getArgumentCount() {
                return 1;
            }

            @Override
            public String getDescription() {
                return format("tidy.program.option.meaning");
            }

            @Override
            public Option.Kind getKind() {
                return Option.Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("--tidy");
            }

            @Override
            public String getParameters() {
                return format("tidy.program.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                tidyProgram = arguments.get(0);
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 0;
            }

            @Override
            public String getDescription() {
                return format("tidy.no-program.option.meaning");
            }

            @Override
            public Option.Kind getKind() {
                return Option.Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("--no-tidy");
            }

            @Override
            public String getParameters() {
                return "";
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                tidyProgram = null;
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 2;
            }

            @Override
            public String getDescription() {
                return format("groups.option.meaning");
            }

            @Override
            public Option.Kind getKind() {
                return Option.Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("-group");
            }

            @Override
            public String getParameters() {
                return format("groups.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                int key = nextGroupKey++;
                rawGroupTitles.put(key, arguments.get(0));
                groupings.put(key, Arrays.asList(arguments.get(1).split(":")));
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 0;
            }

            @Override
            public String getDescription() {
                return format("missing-docs.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Option.Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("-undocumented");
            }

            @Override
            public String getParameters() {
                return "";
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                listUndocumented = true;
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 0;
            }

            @Override
            public String getDescription() {
                return format("multiviews.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Option.Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("-nosuffix");
            }

            @Override
            public String getParameters() {
                return "";
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                hypertextLinkSuffix = "";
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 1;
            }

            @Override
            public String getDescription() {
                return format("import.no-cache.no-modules.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Option.Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("-link");
            }

            @Override
            public String getParameters() {
                return format("import.no-cache.no-modules.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                URI loc = Utils.absoluteFileOrURI(arguments.get(0));
                imports.add(new DocImport(loc));
                reporter.print(Diagnostic.Kind.NOTE,
                               format("import.report.location", loc));
                reporter.print(Diagnostic.Kind.NOTE,
                               format("import.report.cache", loc));
                reporter.print(Diagnostic.Kind.NOTE,
                               format("import.report.module-count", 0));
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 2;
            }

            @Override
            public String getDescription() {
                return format("import.cache.no-modules.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Option.Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("-linkoffline");
            }

            @Override
            public String getParameters() {
                return format("import.cache.no-modules.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                URI cache = Utils.absoluteFileOrURI(arguments.get(1));
                URI loc = Utils.assumeDirectory(URI.create(arguments.get(0)));
                imports.add(new DocImport(loc, cache));
                reporter.print(Diagnostic.Kind.NOTE,
                               format("import.report.location", loc));
                reporter.print(Diagnostic.Kind.NOTE,
                               format("import.report.cache", cache));
                reporter.print(Diagnostic.Kind.NOTE,
                               format("import.report.module-count", 0));
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 2;
            }

            @Override
            public String getDescription() {
                return format("import.no-cache.modules.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Option.Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("-linkmods");
            }

            @Override
            public String getParameters() {
                return format("import.no-cache.modules.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                Collection<String> modList =
                    Arrays.asList(arguments.get(1).split(",+"));
                URI loc = Utils.absoluteFileOrURI(arguments.get(0));
                imports.add(new DocImport(loc, loc, modList));
                reporter.print(Diagnostic.Kind.NOTE,
                               format("import.report.location", loc));
                reporter.print(Diagnostic.Kind.NOTE,
                               format("import.report.cache", loc));
                reporter.print(Diagnostic.Kind.NOTE,
                               format("import.report.module-count",
                                      modList.size()));
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 3;
            }

            @Override
            public String getDescription() {
                return format("import.cache.modules.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Option.Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("-linkofflinemods");
            }

            @Override
            public String getParameters() {
                return format("import.cache.modules.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                Collection<String> modList =
                    Arrays.asList(arguments.get(2).split(",+"));
                URI cache = Utils.absoluteFileOrURI(arguments.get(1));
                URI loc = Utils.assumeDirectory(URI.create(arguments.get(0)));
                imports.add(new DocImport(loc, cache, modList));
                reporter.print(Diagnostic.Kind.NOTE,
                               format("import.report.location", loc));
                reporter.print(Diagnostic.Kind.NOTE,
                               format("import.report.cache", cache));
                reporter.print(Diagnostic.Kind.NOTE,
                               format("import.report.module-count",
                                      modList.size()));
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 1;
            }

            @Override
            public String getDescription() {
                return format("user-content.title.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("-title");
            }

            @Override
            public String getParameters() {
                return format("user-content.title.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                rawTitle = arguments.get(0);
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 1;
            }

            @Override
            public String getDescription() {
                return format("user-content.short-title.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("-windowtitle");
            }

            @Override
            public String getParameters() {
                return format("user-content.short-title.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                rawShortTitle = arguments.get(0);
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 1;
            }

            @Override
            public String getDescription() {
                return format("slice.option.meaning");
            }

            @Override
            public Option.Kind getKind() {
                return Option.Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("-slice");
            }

            @Override
            public String getParameters() {
                return format("slice.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                Matcher m =
                    Configuration.SLICE_PATTERN.matcher(arguments.get(0));
                if (!m.matches()) {
                    reporter
                        .print(Diagnostic.Kind.ERROR,
                               format("slice.format.error", arguments.get(0)));
                    return false;
                }
                Locale locale = Locale.forLanguageTag(m.group("locale"));
                String suffix = m.group("suffix");
                Charset charset = m.group("charset") == null ? null :
                    Charset.forName(m.group("charset"));
                sliceSpecs.add(new SliceSpecification(locale, charset, suffix));
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 1;
            }

            @Override
            public String getDescription() {
                return format("user-content.stylesheet.linked.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Arrays.asList("-stylesheet", "-stylesheetfile");
            }

            @Override
            public String getParameters() {
                return format("user-content.stylesheet.linked.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                styleSource = Paths.get(arguments.get(0));
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 1;
            }

            @Override
            public String getDescription() {
                return format("offline-output.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections
                    .unmodifiableList(Arrays.asList("-do", "--offline"));
            }

            @Override
            public String getParameters() {
                return format("offline-output.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                offlineDirectory = Paths.get(arguments.get(0));
                return true;
            }

        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 1;
            }

            @Override
            public String getDescription() {
                return format("output.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("-d");
            }

            @Override
            public String getParameters() {
                return format("output.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                outputDirectory = Paths.get(arguments.get(0));
                outputZipFile = null;
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 1;
            }

            @Override
            public String getDescription() {
                return format("zip-output.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("-z");
            }

            @Override
            public String getParameters() {
                return format("zip-output.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                outputDirectory = null;
                outputZipFile = Paths.get(arguments.get(0));
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 1;
            }

            @Override
            public String getDescription() {
                return format("diagnostics.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Arrays.asList("--diagnostics", "-dd");
            }

            @Override
            public String getParameters() {
                return format("diagnostics.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                diagnosticsDirectory = Paths.get(arguments.get(0));
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 2;
            }

            @Override
            public String getDescription() {
                return format("user-content.jar-assignment.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Collections.singletonList("-jardirs");
            }

            @Override
            public String getParameters() {
                return format("user-content.jar-assignment.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                String[] jarNameParts = arguments.get(0).split(":");
                String jarName = jarNameParts[0];
                if (jarNameParts.length > 1)
                    jarToVersion.put(jarName, jarNameParts[1]);
                String[] treeNames = arguments.get(1).split(":");
                for (String tree : treeNames)
                    dirToJar.put(Paths.get(tree), jarName);
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 1;
            }

            @Override
            public String getDescription() {
                return format("user-content.stylesheet.linked.option.meaning");
            }

            @Override
            public Kind getKind() {
                return Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Arrays.asList("-stylesheeturi");
            }

            @Override
            public String getParameters() {
                return format("user-content.stylesheet.linked.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                style = URI.create(arguments.get(0));
                return true;
            }
        }, new Option() {
            @Override
            public int getArgumentCount() {
                return 1;
            }

            @Override
            public String getDescription() {
                return format("user-content.overview.option.meaning");
            }

            @Override
            public Option.Kind getKind() {
                return Option.Kind.STANDARD;
            }

            @Override
            public List<String> getNames() {
                return Arrays.asList("-overview", "--overview-file", "-o");
            }

            @Override
            public String getParameters() {
                return format("user-content.overview.option.format");
            }

            @Override
            public boolean process(String option, List<String> arguments) {
                overviewFile = arguments.get(0);
                return true;
            }
        } };
        return new HashSet<>(Arrays.asList(opts));
    }

    /**
     * Get the latest source version supported by this doclet.
     * 
     * @return {@link SourceVersion#RELEASE_9}
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_9;
    }

    /**
     * Initialize this doclet.
     * 
     * @param locale the locale for diagnostic messages
     * 
     * @param a place to report errors, warnings and other information
     */
    @Override
    public void init(Locale locale, Reporter reporter) {
        this.locale = locale;
        this.messageBundle = ResourceBundle
            .getBundle(getClass().getPackage().getName() + ".Messages",
                       this.locale);
        this.reporter = reporter;
    }

    private String format(String key, Object... args) {
        String pattern = messageBundle.getString(key);
        return new MessageFormat(pattern, messageBundle.getLocale())
            .format(args, new StringBuffer(), null).toString();
    }

    private FileSystem getFileSystem() throws IOException {
        if (outputZipFile != null) {
            Map<String, Object> zipOpts = new HashMap<>();
            zipOpts.put("create", "true");
            outputDirectory = null;
            FileSystem outfs = FileSystems.newFileSystem(URI.create("jar:"
                + outputZipFile.toAbsolutePath().toUri().toASCIIString()),
                                                         zipOpts);
            outputDirectory = outfs.getPath("/");
            return outfs;
        } else {
            return new FileSystem() {
                @Override
                public Set<String> supportedFileAttributeViews() {
                    throw new UnsupportedOperationException("dummy");
                }

                @Override
                public FileSystemProvider provider() {
                    throw new UnsupportedOperationException("dummy");
                }

                @Override
                public WatchService newWatchService() throws IOException {
                    throw new UnsupportedOperationException("dummy");
                }

                @Override
                public boolean isReadOnly() {
                    throw new UnsupportedOperationException("dummy");
                }

                @Override
                public boolean isOpen() {
                    throw new UnsupportedOperationException("dummy");
                }

                @Override
                public UserPrincipalLookupService
                    getUserPrincipalLookupService() {
                    throw new UnsupportedOperationException("dummy");
                }

                @Override
                public String getSeparator() {
                    throw new UnsupportedOperationException("dummy");
                }

                @Override
                public Iterable<Path> getRootDirectories() {
                    throw new UnsupportedOperationException("dummy");
                }

                @Override
                public PathMatcher getPathMatcher(String syntaxAndPattern) {
                    throw new UnsupportedOperationException("dummy");
                }

                @Override
                public Path getPath(String first, String... more) {
                    throw new UnsupportedOperationException("dummy");
                }

                @Override
                public Iterable<FileStore> getFileStores() {
                    throw new UnsupportedOperationException("dummy");
                }

                @Override
                public void close() throws IOException {}
            };
        }
    }

    /**
     * Run the doclet on the supplied environment.
     * 
     * @param environment the collection of elements to be documented,
     * their documentation comments, and related utilities
     * 
     * @return {@code true} if the run was successful
     */
    @Override
    public boolean run(DocletEnvironment environment) {
        try (FileSystem outfs = getFileSystem()) {
            Configuration config =
                new Configuration(environment, reporter, messageBundle,
                                  rawTitle, rawShortTitle, overviewFile,
                                  imports, rawGroupTitles, outputDirectory,
                                  offlineDirectory, diagnosticsDirectory,
                                  hypertextLinkSuffix, tidyProgram, styleSource,
                                  style, dirToJar, jarToVersion, sliceSpecs,
                                  listUndocumented);
            if (!config.isOkay()) return false;

            /* Generate slice-independent files. */
            new MetadataGenerator(config).run();

            /* Generate each slice. */
            for (SliceSpecification sliceSpec : config.sliceSpecs) {
                Slice slice = new Slice(config, sliceSpec);
                config.diagnostic("slice.start", slice.spec.suffix,
                                  slice.spec.locale
                                      .getDisplayLanguage(config.getLocale()),
                                  slice.spec.charset);

                /* Generate this slice. */
                new NavigationGenerator(slice).run();
                new ModuleGenerator(slice).run();
                new PackageGenerator(slice).run();
                new ClassGenerator(slice).run();

                config.diagnostic("output.undocumented-elements",
                                  slice.countUndocumentedElements());
                config.diagnostic("output.deprecated-elements",
                                  config.deprecatedElements.size());
                new OverviewGenerator(slice).run();
            }

            config.executor.shutdown();
            do {
                System.err.println(format("job.wait"));
                config.executor.awaitTermination(10, TimeUnit.SECONDS);
            } while (!config.executor.isTerminated());
            return true;
        } catch (IOException e) {
            reporter.print(Kind.ERROR, format("zip-output.failure",
                                              e.getMessage(), outputZipFile));
            return false;
        } catch (InterruptedException e) {
            reporter.print(Kind.ERROR, format("job.failure"));
            return false;
        }
    }

    /**
     * The field in an installation's
     * <samp>{@value DocImport#PROPERTIES_NAME}</samp> specifying a
     * macro format mapping elements to URIs of their documentation
     */
    public static final String SCHEME_PROPERTY_NAME = "link.scheme";
}
