.PHONY: test
test:
	clojure -M:dev:test

.PHONY: outdated
outdated:
	clojure -M:outdated

.PHONY: repl
repl:
	iced repl --force-clojure-cli -A:dev

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

.PHONY: clean
clean:
	\rm -rf .cpcache classes target .vim-iced

# vim:fdl=0:fdm=marker:
