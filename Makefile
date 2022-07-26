all::

FIND=find
XARGS=xargs
PRINTF=printf
SED=sed
M4=m4

PREFIX=/usr/local

VWORDS:=$(shell src/getversion.sh --prefix=v MAJOR MINOR PATCH)
VERSION:=$(word 1,$(VWORDS))
LONG_VERSION:=$(word 2,$(VWORDS))

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

ifneq ($(LONG_VERSION),)
CMPCP=$(CMP) -s '$1' '$2' || $(CP) '$1' '$2'
.PHONY: prepare-version
tmp/LONG_VERSION: prepare-version
	@$(MKDIR) tmp/
	@$(ECHO) $(VERSION) > tmp/VERSION
	@$(ECHO) $(LONG_VERSION) > $@.tmp
	@$(call CMPCP,tmp/VERSION,VERSION)
	@$(call CMPCP,$@.tmp,$@)
endif

## Embed the revision number into the code.
$(call jardeps_files,core,uk.ac.lancs.polydoclot,dynamic.properties): \
	$(call jardeps_files,core,uk.ac.lancs.polydoclot,dynamic.properties).m4 \
	VERSION tmp/LONG_VERSION
	@$(PRINTF) '[version props %s]\n' "$(file <tmp/LONG_VERSION)"
	@$(MKDIR) '$(@D)'
	@$(M4) -DVERSION='`$(file <tmp/LONG_VERSION)'"'" < '$<' > '$@'

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
