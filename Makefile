all::

FIND=find
XARGS=xargs
PRINTF=printf
SED=sed
GETVERSION=git describe
M4=m4

PREFIX=/usr/local

VERSION:=$(shell $(GETVERSION) 2> /dev/null)

## Provide a version of $(abspath) that can cope with spaces in the
## current directory.
myblank:=
myspace:=$(myblank) $(myblank)
MYCURDIR:=$(subst $(myspace),\$(myspace),$(CURDIR)/)
MYABSPATH=$(foreach f,$1,$(if $(patsubst /%,,$f),$(MYCURDIR)$f,$f))

-include $(call MYABSPATH,config.mk)
-include polydoclot-env.mk

SELECTED_JARS += polydoclot
trees_polydoclot += core
roots_core += uk.ac.lancs.polydoclot.Polydoclot
files_core/uk.ac.lancs.polydoclot += default-styles.css
files_core/uk.ac.lancs.polydoclot += icons.properties
files_core/uk.ac.lancs.polydoclot += hypertextents.properties
files_core/uk.ac.lancs.polydoclot += dynamic.properties
dlps_core += uk.ac.lancs.polydoclot.Headings
dlps_core += uk.ac.lancs.polydoclot.Messages

JARDEPS_OUTDIR=out
JARDEPS_SRCDIR=src/tree
JARDEPS_MERGEDIR=src/merge

include jardeps.mk
-include jardeps-install.mk

DOC_PKGS += uk.ac.lancs.polydoclot
DOC_PKGS += uk.ac.lancs.polydoclot.html
DOC_PKGS += uk.ac.lancs.polydoclot.util
DOC_PKGS += uk.ac.lancs.polydoclot.imports

DOC_OVERVIEW=src/overview.html
DOC_CLASSPATH += $(jars:%=$(JARDEPS_OUTDIR)/%.jar)
DOC_SRC=$(call jardeps_srcdirs4jars,$(SELECTED_JARS))
DOC_CORE=polydoclot$(DOC_CORE_SFX)

ifneq ($(VERSION),)
prepare-version::
	@$(MKDIR) tmp/
	@$(ECHO) $(VERSION) > tmp/VERSION

tmp/VERSION: | prepare-version
VERSION: tmp/VERSION
	@$(CMP) -s '$<' '$@' || $(CP) '$<' '$@'
endif

## Embed the SVN revision number into the code.
#svnrevision=$(notdir $(subst :,/,$(shell svnversion)))
detect-revision:
	@$(MKDIR) tmp
	@printf > 'tmp/revision' \
	  'rcs=SVN\nrevision=%s\n' '$(shell svnversion)'
$(call jardeps_files,core,uk.ac.lancs.polydoclot,dynamic.properties): \
	$(call jardeps_files,core,uk.ac.lancs.polydoclot,dynamic.properties).m4 \
	VERSION
	@$(PRINTF) '[version props %s]\n' "$(file <VERSION)"
	@$(MKDIR) '$(@D)'
	@$(M4) -DVERSION='`$(file <VERSION)'"'" < '$<' > '$@'

all:: installed-jars

installed-jars:: $(SELECTED_JARS:%=$(JARDEPS_OUTDIR)/%.jar)
installed-jars:: $(SELECTED_JARS:%=$(JARDEPS_OUTDIR)/%-src.zip)

install:: install-jars

install-jars:: $(SELECTED_JARS:%=install-jar-%)

version_polydoclot=$(patsubst v%,%,$(file <VERSION))

install-jar-%::
	@$(call JARDEPS_INSTALL,$(PREFIX)/share/java,$*,$(version_$*))

tidy::
	@$(PRINTF) "Removing editor back-ups and core dumps\n"
	@-$(FIND) . \( -name "*~" -o \( -type f -name "core" \) \) | $(XARGS) $(RM) -f

clean:: tidy

blank:: clean
	@$(PRINTF) "Blanking\n"
	@-$(RM) -r $(JARDEPS_OUTDIR)

distclean:: blank
	$(RM) VERSION

YEARS=2018,2019

update-licence:
	$(FIND) . -name '.git' -prune -or -type f -print0 | $(XARGS) -0 \
	$(SED) -i 's/Copyright\s\+[0-9,]\+\sLancaster University/Copyright $(YEARS), Lancaster University/g'
