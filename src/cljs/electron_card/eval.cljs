(ns electron-card.eval
  (:require [cljs.js :as cljs]
            [cljs.core.async :refer [promise-chan put!]]
            [clojure.string :as str]
            [cljs.analyzer :as ana]))

(set! js/cljs.user #js{})

(def vm (js/require "vm"))
(def st (cljs/empty-state))

(set! *target* "nodejs")

(defn- node-eval [warnings]
  (fn [{:keys [name source]}]
    (if (seq @warnings)
      nil
      ; HACK: provide is added whenever a require is used in the source, but we don't want that
      (let [source (str/replace source #"goog\.provide\([^)]+\)" "")]
        (.runInThisContext vm source (str (munge name) ".js"))))))

(defn- requires
  "Return required symbols given compiler state and namespace: a map of
  `{ns ns, another-ns another-ns, ...}`.
  Note that `import` also adds something to the AST's `:requires` key of
  the requirer-ns, see `replumb.ast/dissoc-import`.
  You need a `require` in the requirer-ns namespace for this to return
  something."
  [state requirer-ns]
  {:pre [(symbol? requirer-ns)]}
  (get-in state [:cljs.analyzer/namespaces requirer-ns :requires]))

(defn- imports
  "Return imported symbols given compiler state and a namespace: a map
  of `{symbol1 ns, symbol2 ns, ...}`.
  Note that an `import` symbol is the final segment only, so `User` in
  the `foo.bar.User`
  You need a `import` in the requirer-ns namespace for this to return
  something."
  [state requirer-ns]
  {:pre [(symbol? requirer-ns)]}
  (get-in state [:cljs.analyzer/namespaces requirer-ns :imports]))

(defn- symbols
  "Return referred/used symbols given compiler state and a namespace: a
  map of `{symbol1 ns, symbol2 ns, ...}`.
  You need a `:refer` in the requirer-ns namespace for this to return
  something."
  [state requirer-ns]
  {:pre [(symbol? requirer-ns)]}
  (get-in state [:cljs.analyzer/namespaces requirer-ns :uses]))

(defn- macros
  "Return all the macro symbols given compiler state and namespace: a
  map of `{macro-symbol1 macro-ns, macro-symbol2 macro-ns}`.
  You need a `:refer` in the requirer-ns namespace for this to return
  something."
  [state requirer-ns]
  {:pre [(symbol? requirer-ns)]}
  (get-in state [:cljs.analyzer/namespaces requirer-ns :use-macros]))

(defn- macro-requires
  "Return all the required macro namespaces given compiler state and a
  namespace: a map of `{macro-ns1 macro-ns1, macro-ns2 macro-ns2, ...}`.
  You need a `require-macros` in the requirer-ns namespace for this to
  return something."
  [state requirer-ns]
  {:pre [(symbol? requirer-ns)]}
  (get-in state [:cljs.analyzer/namespaces requirer-ns :require-macros]))

(defn- require-of-ns?
  "Yields true when the symbol belongs to ns.
  This typically works on the output of either `replumb.ast/requires` or
  `replumb.ast/macro-requires`."
  [ns sym]
  (= ns sym))

(defn- symbol-of-ns?
  "Yields true when the symbol belongs to ns.
  This typically works on the output of `replumb.ast/symbols`."
  [ns sym]
  (= ns sym))

(defn- import-of-ns?
  "Yields true when sym belongs to ns.
  This typically works on the output of `replumb.ast/imports`."
  [ns sym]
  (goog.string.caseInsensitiveContains (str sym) (str ns)))

(defn- macro-of-ns?
  "Yields true when the sym belongs to ns.
  This typically works on the output of `replumb.ast/macros`."
  [ns sym]
  (= ns sym))

(defn- dissoc-ns
  "Dissoc the namespace symbol from the compiler state."
  [state ns]
  {:pre [(symbol? ns)]}
  (update-in state [:cljs.analyzer/namespaces] dissoc ns))

(defn- dissoc-symbol
  "Dissoc symbol from the compiler state given the symbol of the
  namespace where `require` (or `use`) was called from."
  [state requirer-ns sym]
  {:pre [(symbol? requirer-ns) (symbol? sym)]}
  (update-in state [:cljs.analyzer/namespaces requirer-ns :uses] dissoc sym))

(defn- dissoc-import
  "Dissoc the imported symbol from the compiler state."
  [state requirer-ns sym]
  {:pre [(symbol? requirer-ns) (symbol? sym)]}
  (-> state
      (update-in [:cljs.analyzer/namespaces requirer-ns :requires] dissoc sym)
      (update-in [:cljs.analyzer/namespaces requirer-ns :imports] dissoc sym)))

(defn- dissoc-macro
  "Dissoc a macro symbol from the compiler state given the symbol of the
  namespace where `require-macros` (or `use-macros`) was called from."
  [state requirer-ns sym]
  {:pre [(symbol? requirer-ns) (symbol? sym)]}
  (update-in state [:cljs.analyzer/namespaces requirer-ns :use-macros] dissoc sym))

(defn- dissoc-require
  "Dissoc the required-ns from requirer-ns.
  For instance after:
  ```
  (in-ns 'cljs.user)        ;; requirer-ns
  (require 'clojure.string) ;; required-ns
  ```
  You can use the following to clean the compiler state:
  ```
  (dissoc-require repl/st 'cljs.user 'clojure.string)
  ```
  This util function does not remove aliases. See
  `replumb.ast/dissoc-all`."
  [state requirer-ns required-ns]
  {:pre [(symbol? requirer-ns) (symbol? required-ns)]}
  (update-in state [:cljs.analyzer/namespaces requirer-ns :requires] dissoc required-ns))

(defn- dissoc-macro-require
  "Dissoc the macro required-ns from requirer-ns.
  For instance after:
  ```
  (in-ns 'cljs.user)          ;; requirer-ns
  (require-macros 'cljs.test) ;; required-ns
  ```
  You can use the following to clean the compiler state:
  ```
  (dissoc-macro-require repl/st 'cljs.user 'cljs.test)
  ```
  This util function does not remove aliases. See
  `replumb.ast/dissoc-all`."
  [state requirer-ns required-ns]
  {:pre [(symbol? requirer-ns) (symbol? required-ns)]}
  (update-in state [:cljs.analyzer/namespaces requirer-ns :require-macros] dissoc required-ns))

(defn- dissoc-all
  "Dissoc all the required-ns symbols from requirer-ns.
  There are five types of symbol in replumb jargon, which loosely map to
  `cljs.js` ones. These optionally go in the type parameter as keyword:
  - `:symbol`, the default, for instance my-sym in `(def my-sym 3)`
  - `:macro`, which comes from a `(defmacro ...)`
  - `:import`, for instance User in `(import 'foo.bar.User)`
  - `:require`, which is the namespace symbol in a `(require ...)`
  - `:macro-require`, which is the namespace symbol in a `(require-macros ...)`
  This is the only function in the lot that also reliably clears
  namespace aliases."
  ([state requirer-ns required-ns]
   (dissoc-all state requirer-ns required-ns :symbol))
  ([state requirer-ns required-ns type]
   (let [[get-fn pred dissoc-fn] (case type
                                   :require [requires #(require-of-ns? required-ns (second %)) dissoc-require]
                                   :macro-require [macro-requires #(require-of-ns? required-ns (second %)) dissoc-macro-require]
                                   :symbol [symbols #(symbol-of-ns? required-ns (second %)) dissoc-symbol]
                                   :macro [macros #(macro-of-ns? required-ns (second %)) dissoc-macro]
                                   :import [imports #(import-of-ns? required-ns (second %)) dissoc-import])]
     (let [syms (get-fn state requirer-ns)
           required-syms (map first (filter pred syms))]
       (reduce #(dissoc-fn %1 requirer-ns %2) state required-syms)))))

(defn- purge-required-ns!
  "Remove all the references to the given namespace in the compiler
  state."
  [required-ns]
  (let [required-macro-ns (symbol (str required-ns "$macros"))]
    (swap! st #(-> %
                   (dissoc-ns required-ns)
                   (dissoc-ns required-macro-ns)))
    (swap! cljs.js/*loaded* #(->> (-> %
                                      (disj required-ns)
                                      (disj required-macro-ns))
                                  (remove (partial import-of-ns? required-ns))
                                  (into #{})))))

(defn- purge-symbols!
  "Get rid of all the compiler state references to required-ns macros
  namespaces and symbols from requirer-ns."
  [requirer-ns required-ns]
  (swap! st #(-> %
                 (dissoc-all requirer-ns required-ns :require)
                 (dissoc-all requirer-ns required-ns :macro-require)
                 (dissoc-all requirer-ns required-ns :macro)
                 (dissoc-all requirer-ns required-ns :symbol)
                 (dissoc-all requirer-ns required-ns :import))))

(defn- purge-namespaces!
  "Remove all the namespace references, symbols included, required from
  inside the input requirer-ns namespace.
  For instance after evaluating:
  (in-ns 'cljs.user)         ;; requirer-ns
  (require 'clojure.string)  ;; required-ns
  You can eval the following to clean the compiler state:
  (replumb.repl/purge-require 'cljs.user 'clojure.string).
  Note that doing this manually is tricky, as, for instance,
  clojure.string has the following dependencies to clear: goog.string
  goog.string.StringBuffer."
  [requirer-ns namespaces]
  (doseq [ns namespaces]
    (purge-required-ns! ns)
    (purge-symbols! requirer-ns ns)))

(defn- purge-cljs-user!
  "Remove all the namespace references required from inside cljs.user
  from the compiler state.
  The 0-arity version cleans namespaces in cljs.js/*loaded*."
  ([]
   (purge-namespaces! 'cljs.user @cljs.js/*loaded*))
  ([namespaces]
   (purge-namespaces! 'cljs.user namespaces)))

(defn- load-fn [{:keys [name macros path] :as thing} cb]
  (println "would load" thing)
  (cb {:lang :js :source ""}))

(defn- normalize-output [warnings {:keys [value error]}]
  (cond
    error
    {:errors (->> error ex-cause .-message (conj warnings))}

    (seq warnings)
    {:errors warnings}

    :default
    {:value value}))

(defn- handler [warnings]
  (fn [warning-type env extra]
    (when (warning-type ana/*cljs-warnings*)
      (when-let [s (ana/error-message warning-type extra)]
        (swap! warnings conj (ana/message env s))))))

(defn eval [source]
  (let [ret (promise-chan)
        warnings (atom [])]
    (purge-cljs-user!)
    (binding [ana/*cljs-warning-handlers* [(handler warnings)]]
      (cljs/eval-str st source 'cljs.user
                     {:eval (node-eval warnings) :load load-fn :ns 'cljs.user}
                     #(put! ret (normalize-output @warnings %))))
    ret))

