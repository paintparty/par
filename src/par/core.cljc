(ns par.core
  (:require [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [clojure.walk :as walk])
  #?(:clj  (:require [io.aviso.ansi :as ansi]))
  #?(:cljs (:require-macros [par.core :refer [? !?]])))

(def lib-defs (set '(def defn defrecord defstruct defprotocol defmulti deftype defmethod defstyles defstyle)))

(def browser-secondary-color "rgb(10, 136, 179)")

(defn format-val [x]
  ;; Take this out when swapping in new pretty-print
  (if (nil? x)
    :____nil____
    x))

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

(defn print-nil [s]
  (string/replace s #":____nil____" "nil"))

(defn formatted-str
  [x*]
  (let [x (if (coll? x*) (walk/postwalk anonymize x*) x*)
        formatted  (format-val x)
        ret* (-> formatted pprint with-out-str (string/replace #"\n$" ""))
        ret (-> ret* print-nil (string/replace #"\(fn\* " "#("))]
    ret))

(defn long-form? [x]
  (when (string? x)
    (or (re-find #"\n" x)
        (> (count x) 30))))

(defn fat-arrow
  [fat-arrow* browser?]
  (let [c (when browser? "%c")]
    (str " " c fat-arrow* c " ")))

(defn ?*
  [{:keys [v
           qv
           file-info
           fat-arrow*
           label
           js-console?
           clj-c?
           browser?
           form?]
    :as m}]
  (let [seqy?     (seq? qv)
        def?      (and seqy? (contains? lib-defs (first qv)))
        form      (when form? (str (formatted-str qv)))
        ret*      (if def?
                    (symbol (str "#'" (namespace ::x) "/" (second qv)))
                    (formatted-str v))
        long-ret? (long-form? ret*)
        ret       (if long-ret? (str "\n" ret*) ret*)
        fat-arrow (fat-arrow fat-arrow* browser?)]
    (string/join
     [(str (if browser? (str "\n%c" file-info "%c") (str "\n" file-info)))
      (if label (str "\n" label (when form "\n")) (when form "\n"))
      form
      fat-arrow
      ret
      "\n"])))

#?(:clj
   (defmacro !? [& args]
     (let [v (last args)]
       `~v)))

#?(:clj
   (defmacro !?j [& args]
     (let [v (last args)]
       `~v)))

(defn helper [form-meta]
  #?(:clj
     (let [{:keys [line column]} form-meta
           file*       (str (ns-name *ns*) ":" line ":" column)
           clj-c?       (contains? #{"clj" "cljc"} (last (string/split (str *file*) #"\.")))
           file-info   (if clj-c?
                         (ansi/italic (str ansi/cyan-font file* ansi/reset-font))
                         file*)
           arrow-char "=>"
           fat-arrow* (if clj-c?
                        (str ansi/red-font arrow-char ansi/reset-font)
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
            qv
            label
            ?+?
            js-console?
            form?] :as opts} :opts
    logstring                :logstring
    v                        :v}]
  #?(:clj
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
       (when-not fatal? (println logstring))))
  #?(:cljs
     (cond
       js-console?
       (if-not (and (list? qv) (= 'keyed (first qv)))
         (.apply (.-log  js/console)
                 js/console
                 (into-array
                  (remove nil?
                          (let [label (some-> label
                                              (subs 2)
                                              drop-last
                                              drop-last
                                              string/join)
                                long-form? (long-form? (str v))]
                            (list
                             (str " " file-info "\n")
                             label
                             (when (and label form?) "\n")
                             (when form? qv)
                             "=>"
                             (when long-form? "\n")
                             v)))))
         (do (prn "2")
             (js/console.log v)))
       :else
       (.apply (.-log  js/console)
               js/console
               (into-array
                (remove nil?
                        (list
                         logstring
                         "color:#9e9e9e;font-style:italic;line-height:1.5;"
                         (when label "color:inherit;font-style:normal;line-height:1.5;")
                         (when label "color:#42aae1;font-weight:normal;line-height:1.5;")
                         "color:inherit;font-weight:normal;line-height:1.5;"
                         "color:#58c958;font-weight:normal;line-height:1.5;"
                         "color:inherit;font-weight:normal;line-height:1.5;")))))))

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
                     (if (:clj-c? m)
                       (ansi/italic (str ansi/cyan-font label* ansi/reset-font))
                       (str "%c" label* "%c")))
           warning (cond
                     (= numargs 0)
                     {:warning "par.core/? expects at least 1 arg"
                      :fatal?  true}

                     (and (= numargs 3)
                          (not= (second args) :form))
                     {:warning "Did you mean to pass `:form` to par.core/?, (as the 2nd arg)?"})
           opts*   (merge m {:label   label
                             :form?   form?
                             :warning warning})]
       {:numargs numargs
        :form    form
        :label*  label*
        :v       v
        :m       m
        :opts*   opts*})))

#?(:clj
   (defmacro ?j
     [& args]
     (let [{:keys [v opts*]} (opts* args &form)]
       `(do
          (let [opts# (merge ~opts*
                             {:v ~v
                              :qv (quote ~v)
                              :?+? false
                              :js-console? true
                              :browser? (par.core/browser-test)})
                logstring# (?* opts#)]
            (logger {:logstring logstring# :opts opts# :v ~v}))
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

