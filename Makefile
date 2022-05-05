# GraalVM {{{
PLATFORM := $(shell uname -s | tr '[:upper:]' '[:lower:]')
GRAAL_ROOT ?= /tmp/.graalvm
GRAAL_VERSION ?= 22.0.0.2
GRAAL_HOME ?= $(GRAAL_ROOT)/graalvm-ce-java11-$(GRAAL_VERSION)
GRAAL_ARCHIVE := graalvm-ce-java11-$(PLATFORM)-amd64-$(GRAAL_VERSION).tar.gz

ifeq ($(PLATFORM),darwin)
	GRAAL_HOME := $(GRAAL_HOME)/Contents/Home
	GRAAL_EXTRA_OPTION :=
else
	GRAAL_EXTRA_OPTION := "--static"
endif

$(GRAAL_ROOT)/fetch/$(GRAAL_ARCHIVE):
	@mkdir -p $(GRAAL_ROOT)/fetch
	curl --location --output $@ https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-$(GRAAL_VERSION)/$(GRAAL_ARCHIVE)

$(GRAAL_HOME): $(GRAAL_ROOT)/fetch/$(GRAAL_ARCHIVE)
	tar -xz -C $(GRAAL_ROOT) -f $<

$(GRAAL_HOME)/bin/native-image: $(GRAAL_HOME)
	$(GRAAL_HOME)/bin/gu install native-image

.PHONY: graalvm
graalvm: $(GRAAL_HOME)/bin/native-image
# }}}

.PHONY: test
test:
	clojure -M:dev:test

.PHONY: outdated
outdated:
	clojure -M:outdated

.PHONY: repl
repl:
	iced repl --force-clojure-cli -A:dev

target/clj-vimhelp-standalone.jar:
	clojure -T:build uberjar

.PHONY: uberjar
uberjar: target/clj-vimhelp-standalone.jar

target/vimhelp: graalvm uberjar
	$(GRAAL_HOME)/bin/native-image \
		-jar target/clj-vimhelp-standalone.jar \
		-H:Name=target/vimhelp \
		-H:+ReportExceptionStackTraces \
		-J-Dclojure.spec.skip-macros=true \
		-J-Dclojure.compiler.direct-linking=true \
		"-H:IncludeResources=version" \
		--report-unsupported-elements-at-runtime \
		-H:Log=registerResource: \
		--verbose \
		--no-fallback \
		$(GRAAL_EXTRA_OPTION) \
		"-J-Xmx3g"

.PHONY: native-image
native-image: clean target/vimhelp

.PHONY: install
install: target/vimhelp
	\cp -pf target/vimhelp /usr/local/bin

.PHONY: uninstall
uninstall: /usr/local/bin/vimhelp
	\rm -f /usr/local/bin/vimhelp

.PHONY: release
release:
	./script/release

.PHONY: lint
lint:
	clj-kondo --lint src:test
	cljstyle check

.vim-iced:
	git clone https://github.com/liquidz/vim-iced .vim-iced
.PHONY: demo-clj
demo-clj: .vim-iced
	./script/demo clojure -M -m vimhelp.core
.PHONY: demo-native-image
demo-native-image: .vim-iced target/vimhelp
	./script/demo ./target/vimhelp

.PHONY: clean
clean:
	\rm -rf .cpcache classes target .vim-iced

# vim:fdl=0:fdm=marker:
