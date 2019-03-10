.PHONY: test ancient repl native-image install uninstall clean

test:
	clojure -A:dev:test

ancient:
	clojure -A:dev:ancient

repl:
	iced repl --force-clojure-cli -A:dev

target/vimhelp:
	mkdir -p target && clojure -A:native-image

native-image: target/vimhelp

install: target/vimhelp
	\cp -pf target/vimhelp /usr/local/bin

uninstall: /usr/local/bin/vimhelp
	\rm -f /usr/local/bin/vimhelp

clean:
	\rm -rf .cpcache classes target

.vim-iced:
	git clone https://github.com/liquidz/vim-iced .vim-iced

demo: .vim-iced
	mkdir -p target
	clojure -m vimhelp.core \
		./.vim-iced/doc/*.txt \
		--title vim-iced \
		--css "//fonts.googleapis.com/css?family=Roboto+Mono" \
		--css "//cdn.rawgit.com/necolas/normalize.css/master/normalize.css" \
		--css "//cdn.rawgit.com/milligram/milligram/master/dist/milligram.min.css" \
		--style "body { font-family: 'Roboto Mono', monospace; } p { margin: 0; } .section-header .section-link { visibility: hidden; margin-left: -1.5rem; padding-right: 0.5rem; } .section-header:hover .section-link { visibility: visible; } .constant { text-decoration: underline; }" \
		--copyright "(c) Masashi Iizuka" \
		--output=target \
		--verbose
