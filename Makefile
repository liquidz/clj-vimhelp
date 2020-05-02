.PHONY: test ancient repl uberjar native-image install uninstall release clean

test:
	clojure -A:dev:test

ancient:
	clojure -A:dev:ancient

repl:
	iced repl --force-clojure-cli -A:dev

pom.xml: deps.edn
	clojure -Spom

target/vimhelp.jar: pom.xml
	clojure -A:uberjar

uberjar: target/vimhelp.jar

target/vimhelp: target/vimhelp.jar
	mkdir -p target
	$(GRAALVM_HOME)/bin/native-image \
		-jar target/vimhelp.jar \
		-H:Name=target/vimhelp \
		-H:+ReportExceptionStackTraces \
		-J-Dclojure.spec.skip-macros=true \
		-J-Dclojure.compiler.direct-linking=true \
		"-H:IncludeResources=version" \
		--initialize-at-build-time  \
		--report-unsupported-elements-at-runtime \
		-H:Log=registerResource: \
		--verbose \
		--no-fallback \
		--no-server \
		$(GRAAL_EXTRA_OPTION) \
		"-J-Xmx3g"

native-image: target/vimhelp

install: target/vimhelp
	\cp -pf target/vimhelp /usr/local/bin

uninstall: /usr/local/bin/vimhelp
	\rm -f /usr/local/bin/vimhelp

release:
	./script/release

clean:
	\rm -rf .cpcache classes target

lint-clj-kondo:
	clj-kondo --lint src:test
lint-cljstyle:
	cljstyle check
lint: lint-clj-kondo lint-cljstyle

.vim-iced:
	git clone https://github.com/liquidz/vim-iced .vim-iced
demo-clj: .vim-iced
	./script/demo clojure -m vimhelp.core
demo-native-image: .vim-iced target/vimhelp
	./script/demo ./target/vimhelp
