(defproject paintparty/par "0.1.0-SNAPSHOT"
  :description "Print-and-return macros for Clojure(Script)"
  :url "http://github.com/paintparty/par"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]]
  :repl-options {:init-ns par.core}
  :deploy-repositories [["clojars"  {:sign-releases false
                                     :url "https://clojars.org/paintparty/par"
                                     :username :env/CLOJARS_USERNAME
                                     :password :env/CLOJARS_PASSWORD}]])
