(ns rbt.reader
  (:require [clojure.string :as s])
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
;; ({:id "Ucs-02", :ref-to "Ucs-02", :text "Use case second"}
;;  {:id "UCS-01", :ref-to "UCS-01", :text "Use case first"}
;;  {:id "UCS-01/01", :ref-to "UCS-01", :text "My button"}
;;  {:id "UCS-01/02", :ref-to "UCS-01", :text "My button 2"}),
;; :name "filename.md"
;; :markdown
;; "# Use cases \n\n## [UCS-01] Use case first\n\n ...
;;
;; Some comments on the structure.
;; Since the ID may be complex, consisting of two parts (UCS-01/01), we need to retain both parts.
;; So :id -- the fully qualified ID mad of two parts: parent and child. It should appear on reference title.
;; The :ref-to key specifies the requirement where the reference leads. This should always be a section ID.
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
                 (conj acc {:id (str pid id), :ref-to parent-id, :text text})))))))

(defn- build-ids [lines parent-id acc]
  (if (nil? (first lines))
    acc
    ;; Firstly try extracting parent (head) id
    (let [line (first lines)
          hid (extract-head-id line)
          pid (or hid parent-id)]
      (if-not (nil? hid)
        (build-ids (next lines) pid (conj acc {:id hid
                                               :ref-to hid
                                               :text (s/trim (subs line (inc (s/index-of line "]"))))}))

        ;; Next -- try find child ids
        (let [cids (extract-child-ids line parent-id)]
          (if (> (count cids) 0)
            (build-ids (next lines) parent-id (concat acc cids))

            ;; If no ids found -- continue searching
            (build-ids (next lines) parent-id acc)))))))


;;;;;;;;;;;;
;; API

;; Should return a map with Markdown text and extracted IDs
(defn read-md-file [file-name]
  (let [file (slurp file-name)
        lines (s/split-lines file)]
    {:ids (build-ids lines nil [])
     :markdown file
     :name file-name}))

