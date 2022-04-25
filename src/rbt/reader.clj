(ns rbt.reader
  (:require [rbt.specs]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [orchestra.spec.test :as st]
            [expound.alpha :as expound])
  (:gen-class))

;; Supported types of defined
;; 1) Header IDs in square brackets:
;;
;;       ## [MY-ID] My requirement header
;;
;;    In this case the ID of the requirement will by 'MY-ID'.
;;
;; 2) Complex format:
;;
;;   #id:{01:Button title}
;;
;; Example of output:
;;
;;{:ids
;; ({:reader/id "Ucs-02", :reader/ref-to "Ucs-02", :reader/text "Use case second"}
;;  {:reader/id "UCS-01", :reader/ref-to "UCS-01", :reader/text "Use case first"}
;;  {:reader/id "UCS-01/01", :reader/ref-to "UCS-01", :reader/text "My button"}
;;  {:reader/id "UCS-01/02", :reader/ref-to "UCS-01", :reader/text "My button 2"}),
;; :reader/name "filename.md"
;; :reader/markdown
;; "# Use cases \n\n## [UCS-01] Use case first\n\n ...
;;
;; Some comments on the structure.
;; Since the ID may be complex, consisting of two parts (UCS-01/01), we need to retain both parts.
;; So :reader/id -- the fully qualified ID mad of two parts: parent and child. It should appear on reference title.
;; The :reader/ref-to key specifies the requirement where the reference leads. This should always be a section ID.
;; In case of complex ID it should lead to the parent ID.


(def ^:dynamic *HEAD-ID* (re-pattern "^#+\\s+\\[([a-zA-Z0-9-_]+)\\].+"))
(def ^:dynamic *MIDDLE-ID* (re-pattern "#id\\:\\{([a-zA-Z0-9-_]+)\\:?([\\p{Alnum}\\p{Blank}-_]*)\\}"))

(defn extract-head-id [line]
  (second (re-find *HEAD-ID* line)))

(defn extract-child-ids [line parent-id]
  (let [m (re-matcher *MIDDLE-ID* line)
        pid (if (nil? parent-id) "" (str parent-id "/"))]
    (loop [r (re-find m) ;; We need to find all the occurences of the id-pattern in this line.
           acc []]
      (let [id (second r)
            text (last r)]
        (if (nil? id)
          acc
          (recur (re-find m)
                 (conj acc {:reader/id (str pid id), :reader/ref-to parent-id, :reader/text text})))))))

;;;;;;;;;;

(s/fdef build-ids
  :args (s/cat :lines (s/nilable (s/coll-of string?)), :parent-id (s/nilable string?), :acc (s/coll-of :reader/ids-elem))
  :ret (s/coll-of :reader/ids-elem))

(defn build-ids [lines parent-id acc]
  (if (nil? (first lines))
    acc
    ;; Firstly try extracting parent (head) id
    (let [line (first lines)
          hid (extract-head-id line)
          pid (or hid parent-id)]
      (if-not (nil? hid)
        (build-ids (next lines) pid (conj acc {:reader/id hid
                                               :reader/ref-to hid
                                               :reader/text (str/trim (subs line (inc (str/index-of line "]"))))}))

        ;; Next -- try find child ids
        (let [cids (extract-child-ids line parent-id)]
          (if (> (count cids) 0)
            (build-ids (next lines) parent-id (concat acc cids))

            ;; If no ids found -- continue searching
            (build-ids (next lines) parent-id acc)))))))


;;;;;;;;;;;;
;; API

;; Should return a map with Markdown text and extracted IDs

(s/fdef read-md-file
  :args (s/cat :file-name string?)
  :ret :reader/file-result)

(defn read-md-file [file-name]
  (let [file (slurp file-name)
        lines (str/split-lines file)]
    {:reader/ids (build-ids lines nil [])
     :reader/markdown file
     :reader/name file-name}))


(st/instrument)
(set! s/*explain-out* expound/printer)
