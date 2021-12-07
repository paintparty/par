(ns par.core
  (:require [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [clojure.walk :as walk]
            [zprint.core :as zprint])
  #?(:clj  (:require [io.aviso.ansi :as ansi]))
  #?(:cljs (:require-macros [par.core :refer [? !? ?+ !?+]])))

(def lib-defs (set '(def defn defrecord defstruct defprotocol defmulti deftype defmethod defstyles defstyle)))

(defn zp [s]
  #?(:cljs
     (try (zprint/zprint-str s
                             {:parse-string-all? true
                              :style [:community :justified :all-hang :hiccup :map-nl]
                              :width 50
                              :vector {:indent 1}})
          (catch js/Object e "zprint-error" )))
  #?(:clj
     (try (zprint/zprint-str s
                             {:parse-string-all? true
                              :style [:community :justified :all-hang :hiccup :map-nl]
                              :width 50
                              :vector {:indent 1}})
          (catch Exception e "zprint-error" ))))

(defn format-val [x]
  (cond (string? x) (str "\"" x "\"")
        (nil? x) 'nil
        :else x))

(defn find-index [pred coll]
  (first
   (keep-indexed
    (fn [i x]
      (when (pred x) i))
    coll)))

(defn anonymize [x]
  (if (and (list? x) (= 'fn* (first x)))
    (let [vars    (second x)
          var-set (into #{} vars)
          ret*    (cons 'fn* (nth x 2))]
      (walk/postwalk
       #(if (contains? var-set %)
          (symbol
           (str "%"
                (when (> (count vars) 1)
                  (inc (find-index (fn [x] (= x %)) vars)))))
          %)
       ret*))
    x))

(defn formatted-str
  "If called from ?, use zprint.
   If called from ?+, use pprint."
  [x* ?+?]
  (let [x (if (coll? x*) (walk/postwalk anonymize x*) x*)]
    (string/replace
     (if ?+?
       (string/replace (with-out-str (pprint (format-val x))) #"\n$" "")
       (let [ret (zp (str (format-val x)))]
         (if (re-find #"^clojure\.lang\..*\n@.*$" ret)
          "zprint-error"
          ret)))
     #"\(fn\* "
     "#(")))

(defn long-form? [x]
  (when x
    (or (re-find #"\n" x)
        (> (count x) 30))))

(defn fat-arrow
  [fat-arrow* browser? long-ret? form]
  (let [c (when browser? "%c")]
    (str
     (when
      (or long-ret?
          (or (nil? form)
              (long-form? form)))
       "\n ")
     (str c fat-arrow* c " "))))

(defn ?*
  [{:keys [v
           qv
           file-info
           fat-arrow*
           ?+?
           label
           clj-c?
           browser?
           form?]
    :as m}]
  #_(pprint m)
  (let [seqy?     (seq? qv)
        def?      (and seqy? (contains? lib-defs (first qv)))
        form      (when form? (str (formatted-str qv ?+?) " "))
        ret*      (if def?
                    (symbol (str "#'" (namespace ::x) "/" (second qv)))
                    (formatted-str v ?+?))
        long-ret? (long-form? ret*)
        ret       (if long-ret? (str "\n" ret*) ret*)
        fat-arrow (fat-arrow fat-arrow* browser? long-ret? form)]
    #_(pprint {:label label
             :clj-c? clj-c?
             :long-ret? long-ret?
             :form form
             :ret* ret*
             :ret ret
             :fat-arrow* fat-arrow*
             :fat-arrow fat-arrow})
    (string/join
     [(if label (str "\n" label (when form "\n")) (when form "\n"))
      form
      fat-arrow
      ret
      (when-not browser? (str "\n" file-info))
      "\n"])))

#?(:clj
   (defmacro !? [v] `(do ~v)))
#?(:clj
   (defmacro !?+ [v] `(do ~v)))

(defn helper [form-meta]
  #?(:clj
     (let [{:keys [line column]} form-meta
           file*       (str (ns-name *ns*) ":" line ":" column)
           clj-c?       (contains? #{"clj" "cljc"} (last (string/split (str *file*) #"\.")))
           file-info   (if clj-c?
                         (str ansi/cyan-font file* ansi/reset-font)
                         file*)
                         arrow-char "=>"
           fat-arrow* (if clj-c?
                        (str ansi/bold-magenta-font arrow-char ansi/reset-font)
                        arrow-char)]
       {:file-info  file-info
        :fat-arrow* fat-arrow*
        :clj-c? clj-c?})))

(defn browser-test []
  #?(:cljs
     (let [isBrowser# (js/Function. "try {return this===window;}catch(e){ return false;}")]
       (isBrowser#))))

(defn label
  [label*]
  #?(:clj (when label* (ansi/italic (str ansi/cyan-font label* ansi/reset-font))))
  #?(:cljs (when label* label*)))

(defn logger
  [{{:keys [warning
            file-info
            label
            ?+?]}   :opts
    logstring         :logstring}]
  #?(:clj
     (if (re-find #"zprint-error" logstring)
       (println (if-not ?+?
                  (str ansi/bold-red-font
                       "\nError: "
                       ansi/reset-font
                       ansi/bold-font
                       "par.core/?\n"
                       ansi/reset-font
                       "at "
                       file-info "\n"
                       "If you are trying to use par.core/? from within a defmacro (or supporting function), please try par.core/?+ instead.")
                  (str "Error: par.core/?+, " file-info " ")))
       (let [{:keys [warning fatal?]} warning]
         (when warning
           (println
            (str ansi/bold-magenta-font
                 "\nWARNING!: "
                 ansi/reset-font
                 ansi/bold-font
                 warning
                 ansi/bold-font
                 " : "
                 file-info)))
         (when-not fatal? (println logstring)))))
  #?(:cljs
     (.apply (.-log  js/console)
             js/console
             (into-array
              (remove nil?
                      (list
                       logstring
                       (when label "color:rgb(10, 140, 183);font-style:italic")
                       (when label "color:inherit;font-style:normal")
                       "color:magenta;font-weight:bold"
                       "color:inherit;font-weight:normal"))))))

(defn opts* [args form]
  #?(:clj
     (let [numargs (count args)
           label*  (when (> numargs 1) (first args))
           form?   (or (= numargs 1)
                       (and (= numargs 3) (= (second args) :form)))
           v       (cond
                     (= 1 numargs) (first args)
                     (= 2 numargs) (second args)
                     (= 3 numargs) (nth args 2))
           m       (helper (meta form))
           label   (when (and label*
                              (not (when (string? label*) (string/blank? label*)))
                              (not (nil? label*)))
                     (if (:clj? m)
                       (ansi/italic (str ansi/cyan-font "; " label* ansi/reset-font))
                       (str "%c; " label* "%c")))
           warning (cond
                     (= numargs 0)
                     {:warning "par.core/?+ expects at least 1 arg"
                      :fatal? true}

                     (and (= numargs 3)
                          (not= (second args) :form))
                     {:warning "Did you mean to pass `:form` to par.core/?+, (as the 2nd arg)?"})
           opts* (merge m {:label label
                           :form? form?
                           :warning warning})]
       {:numargs numargs
        :form form
        :label* label*
        :v v
        :m m
        :opts* opts*})))

#?(:clj
   (defmacro ?+
     [& args]
     (let [{:keys [v opts*]} (opts* args &form)]
       `(do
          (let [opts# (merge ~opts*
                             {:v ~v
                              :qv (quote ~v)
                              :?+? true
                              :browser? (par.core/browser-test)})
                logstring# (?* opts#)]
            (logger {:logstring logstring# :opts opts#}))
          ~v))))

#?(:clj
   (defmacro ?
     [& args]
     (let [{:keys [v opts*]} (opts* args &form)]
       `(do
          (let [opts# (merge ~opts*
                             {:v ~v
                              :qv (quote ~v)
                              :?+? false
                              :browser? (par.core/browser-test)})
                logstring# (?* opts#)]
            (logger {:logstring logstring# :opts opts#}))
          ~v))))

