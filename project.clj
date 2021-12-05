(defproject org.clojars.paintparty/par "1.0.0"
  :description "Print-and-return macros for Clojure(Script)"
  :url "https://github.com/paintparty/par"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [io.aviso/pretty "1.1"]
                 [zprint "1.2.0"]]
  :repl-options {:init-ns par.core}
  :deploy-repositories [["releases"  {:sign-releases false :url "https://repo.clojars.org"}]])
