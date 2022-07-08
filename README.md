# Purpose

This is a Javadoc doclet which demonstrates a few features I wanted to try out:

- multilingual tags and content-negotiable pages

- post-processing with [HTMLTidy](https://www.html-tidy.org/)

- metadata in the overview

- miscellaneous extra tags

## Installation

You need [Jardeps](https://github.com/simpsonst/jardeps) installed.
Then you can run:

```
make
sudo make install
```

This installs in `$(PREFIX)`, which is `/usr/local` by default.
You can override this by setting it on the command line, or in `config.mk` adjacent to `Makefile`, or in `polydoclot-env.mk` in GNU Make's search path, which is extended with `-Idir`.
GNU Make also takes arguments from `$MAKEFLAGS`.

## Invocation

Invoke `javadoc` with `-docletpath` to point to `polydoclot.jar`, and with `-doclet uk.ac.lancs.polydoclot.Polydoclot` to select the doclet.

- `-d dir` specifies the output directory, which must exist, although subdirectories will be created within it as necessary.
  The default is (probably) `.`.

- `-link rempfx` adds a documentation location that the current documentation can reference.
  Files such as `package-list`, `element-list` and `doc-properties.xml` will be pulled from the URI prefix `rempfx` in order to construct URIs referencing specific elements of the documentation at `rempfx`.
  
- `-linkoffline rempfx locpfx` is like `-link`, but pulls the metadata from the URI prefix `locpfx`, which can also be a directory in the local file system.
  This allows links to the remote documentation to be constructed without having to contact the remote server, provided `locpfx` contains a copy of the metadata files.
  Use this with `-do` on the construction of the remote documentation to create a local copy in addition to the main documentation.

- `-linkmods rempfx mod1,mod2,...` adds a documentation location at `rempfx`, and lists the modules available at `rempfx`, should it not provide that information through `element-list`, as is the case for the standard doclet in JDK9 and earlier.

- `-linkofflinemods rempfx locpfx mod1,mod2,...` is like `-linkoffline`, but also lists the modules available there.

- `-z zf` overrides `-d`, and specifies that a zip file `zf` will be created or modified instead.
  Remove the file prior to invocation to ensure it contains only the latest content.
  
- `-do dir` or `--offline dir` specifies a directory in which to write copies of `package-list`, `element-list` and `doc-properties.xml`.
  Other invocations that reference the current documentation can use this directory as the second argument to `-linkoffline`, so that links to this documentation in its public location can be derived without having to contact that location.

- `-dd dir` or `-diagnostics dir` sets the diagnostic directory, which will contain HTMLTidy errors and pre-tidy output.
  Line numbers in the former refer to the latter.
  The directory must exist before execution, but subdirectories under it will be created as necessary.
  
- `-jardirs foo tree1:tree2:...` specifies that classes in the specified source trees contribute to `foo.jar`.
  This information will be reported in each class's page.
  
- `--tidy prog` specifies that `prog` is the executable for HTMLTidy.
  The default is `tidy`, but this switch allows you to specify (say) a full path.
  
- `--no-tidy` specifies that HTMLTidy is not to be applied.

- `-stylesheet f` or `-stylesheetfile f` specifies that that file `f` is to be copied into the destination directory or zip under the name `copied-styles.css`.
  These styles are applied after the defaults, but before the external styles.
  
- `-stylesheeturi uri` causes generated pages to transclude the styles at `uri`, and apply them after the default and copied styles.

- `-nosuffix` removes the `.html` suffix in internal links within the generated documentation.
  Note that the files still have the suffix, but will refer to each other without.
  This usually means that the content can only be properly accessed by serving it with content negotiation enabled.

## Features

### Multilingual pages

The in-line `@lang` tag surrounds text only to be included when generating a page in a specific language:

```
{@lang en Submit a task for execution.}
{@lang eo Liveru taskon por plenumado.}
```

`@select` selects the first matching element:

```
{@select {@lang eo Liveru taskon por plenumado.}
{@else Submit a task for execution.}}
```

In the overview file, you can indicate which languages should be generated:

```
@slice en-GB,.en-GB,UTF-8
@slice eo,.eo,iso8859-e
```

The above configuration causes each page to be generated twice, once selecting for `en-GB` (also matching `en`) and with a suffix of `.en-GB.html`, and once selecting for `eo`, and with a suffix of `.eo.html`.
The language code is required.
The suffix defaults an empty string.
The third field is the character encoding to use, and defaults to `UTF-8`.

### Software title

You can specify the title of the documentation in the overview file:

```
@title Zarquon Dinglebat Library
@shortTitle zdlib
```

The `@title` string appears in the overview page.
`@shortTitle` appears in the title of every page, and defaults to the `@title` string.
These strings can contain other tags, like `@lang`.

### Authors

The overview can contain author details to be referenced from other pages:

```
@pname jsmith John Smith
@paddr jsmith Example Corp., 1004 Demo St, Gotham
@pemail jsmith j.smith@example.com
@ptel jsmith +1 555 1234
@plink jsmith https://staff.example.com/~jsmith/
```

When an `@author jsmith` is encountered, it will expand to the specified details.
Alternatively, if `@pdesc jsmith` is present in the overview source, `@author jsmith` will expand to a compact link to the author's details in the overview page.

`@palias jsmith john johnny jo-bo` defines aliases that `@author` tags can refer to `jsmith` by.

### Summaries

The latest standard doclet recognizes `{@summary ...}` as an override for the first sentence as a short description for an element.
Polydoclot instead recognizes a block tag `@resume` as the short description.

### Headings

Since the doclet uses some heading itself, the author-supplied content should use the following construct for headings, so they have the right level with respect to the generated content:

```
{@h1 How to prepare for this call}
```

`@h1`, `@h2` and `@h3` are defined.

### Default behaviour

The block tags `@default`, `@apiNote`, `@implSpec` and `@implNote` define details of a method description that will not be inherited.
`@default` blocks appear first, with no header.
`@apiNote` blocks appear next, then `@implSpec`, then `@implNote`, each block with its own header.

### Listing factory methods with constructors

Tagging a method with `@constructor` causes it to be listed with the real constructors of the method's return type (or the element type thereof, if it's an array type).

### Package groups

Although packages can be grouped on the command line, the preferred way is to define the groups in the overview.
The following defines a group called `core`, and sets its title (which can include language-selection tags):

```
@group core Core packages
```

(Groups are listed by `@group`-tag order.)

Packages and modules can then be assigned to a group:

```
@package core org.example
@package core org.example.server
@module core org.example
```



### Undocumented elements

Elements can be marked `@undocumented`, even though they would normally be included.
This tag has probably been obviated by `{@value}` being allowed to reference private constants.

The switch `-undocumented` enables the generation of a section in the overview listing elements that are to be documented, but lack explicit documentation.

### External referencing schema

In addition to `package-info` and `element-info` generated at the root of the documentation, the following are also generated:

- `content-types.tab` &ndash; This contains content-negotiation information.
  Each line has the format:
  ```
  sfx: attr1=val2 attr2=val2 ...
  ```
  Each file with a name component matching `sfx` has the listed attributes.
  A suffix of `DEFAULT` applies the attributes to all files.
  The attributes are:
  
  - `t` &ndash; plain MIME type, e.g., `text/html`
  
  - `c` &ndash; character encoding, e.g., `UTF-8`
  
  - `l` &ndash; natural language, e.g., `en-GB`
  
  For example:
  
  ```
  DEFAULT: t=text/plain c=UTF-8
  .xml: t=application/xml
  .css: t=text/css c=UTF-8
  .html: t=text/html
  .en-GB: c=UTF-8 l=en-GB
  ```
  
  (I have Bash and PHP scripts which will read this from zipped documentation.)

- `doc-properties.xml` &ndash; These Java properties describe programmatically how to link to elements within the documentation.
  The property `link.scheme` holds a string that can be read by `MacroFormatter`, and subsequently be used to generate URIs to specific elements.
