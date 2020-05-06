(ns par.core
  (:require [clojure.string])
  #?(:cljs (:require-macros [par.core :refer [!? ? ?c]])))

(def lib-defs (set '(def defn defrecord defstruct defprotocol defmulti deftype defmethod defstyles defstyle)))

(defn ?* [v qv]
  (let [seqy? (seq? qv)
        def? (and seqy? (contains? lib-defs (first qv)))
        f (fn [x] (if (string? x) (str "\"" x "\"") x))]
     ["\n"
      (f qv)
      "\n"
      "\n"
      "=>"
      (if def?
        (symbol (str "#'" (namespace ::x) "/" (second qv)))
        (f v))
      "\n\n"]))

#?(:clj
   (defmacro !? [v] ))

#?(:clj
   (defmacro ? [v]
     `(apply js/console.log (?* ~v (quote ~v)))))

#?(:clj
   (defmacro ?c [v]
   `(apply println (?* ~v (quote ~v)))))

