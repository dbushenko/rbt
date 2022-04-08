(ns rbt.processor
  (:require [rbt.reader :as reader]
            [clojure.string :as s])
  (:gen-class))

(def ^:dynamic *LEVEL-PATTERN* (re-pattern "^#+"))
(def ^:dynamic *REF* (re-pattern "(#ref)\\:([a-zA-Z0-9-_\\.\\/]+)"))
(def ^:dynamic *REF-ID* (re-pattern "(#refId)\\:([a-zA-Z0-9-_\\.\\/]+)"))
(def ^:dynamic *REF-TEXT* (re-pattern "(#refText)\\:([a-zA-Z0-9-_\\.\\/]+)"))
(def ^:dynamic *REFS-TO* (re-pattern "(#refsTo)\\:([a-zA-Z0-9-_\\.\\/]+)"))
(def ^:dynamic *REFS-FROM* (re-pattern "(#refsFrom)\\:([a-zA-Z0-9-_\\.\\/]+)"))

;; Supported types of references
;;
;; #refId:ID -- refrence text will be its ID
;; #refText:ID -- refrence text will be its text
;; #ref:ID -- reference text will be the combination of its ID and text: '[ID] Text'.
;; #refsTo:ID -- all references to this requirement
;; #refsFrom:ID -- all references from this requirement

(defn add-ids-to-unique-set [fl acc-start]
  (loop [ids (next (-> fl :ids))
         id (first (-> fl :ids))
         acc acc-start]
    (if (empty? id)
      acc
      (if (contains? acc id)
        (throw (IllegalStateException. (str "ID '" id "' mentioned in " (:name fl) " already defined!")))
        (recur (next ids) (second ids) (conj acc id))))))

(defn verify-id-uniqueness
  ([md-files] (verify-id-uniqueness md-files #{}))
  
  ([md-files acc]
   (let [fl (first md-files)]
     (if (empty? fl)
       acc
       (recur (next md-files) (add-ids-to-unique-set fl acc))))))

;;-------------

(defn add-ids-to-map [fl acc-start]
  (loop [ids (:ids fl)
         id (first ids)
         acc acc-start]
    (if (nil? id)
      acc
      (recur (next ids) (second ids) (assoc acc (:id id) id)))))

(defn make-ids-map
  ([md-files] (make-ids-map md-files {}))
  
  ([md-files acc-start]
   (loop [fls md-files
          fl (first fls)
          acc acc-start]
     (if (nil? fl)
       acc
       (recur (next fls) (second fls) (add-ids-to-map fl acc))))))


;;-------------

(defn check-ref [r line-num file-name rs]
  (if-not (contains? rs r)
    {:line-num line-num
     :file-name file-name
     :reference r}))

;; Check that each reference leads to existing requirement
(defn validate-refs [refs line-num file-name ids-map]
  (let [rs (into #{} (keys ids-map))]
    (doall (filter #(not (nil? %))
                   (map #(check-ref % line-num file-name rs) refs)))))

;;-------------


(defn extract-header [line]
  (if (nil? line)
    [nil nil]
  (let [hid (reader/extract-head-id line)
        level (if-not (nil? hid ) (count (re-find *LEVEL-PATTERN* line)))]
    [hid (if (= level 0) nil level)])))

(defn extract-with-matcher [matcher line]
  (let [m (re-matcher matcher line)]
    (loop [r (re-find m)
           acc []]
      (if (nil? r)
        acc
        (recur (re-find m) (conj acc (last r)))))))

(defn extract-refs [line]
  (concat (extract-with-matcher *REF* line)
          (extract-with-matcher *REF-ID* line)
          (extract-with-matcher *REF-TEXT* line)))

(defn conj-traces-from [acc hid refs]
  (if (nil? hid)
    acc
    (let [old-refs (get acc hid)]
      (assoc acc hid (into #{} (concat old-refs refs))))))

(defn make-traces-from
  ([file ids-map acc] (make-traces-from (s/split-lines (:markdown file)) 1 nil 0 ids-map file {} [] acc))
  ([lines line-num current-id current-level ids-map file traces-from-acc errors-acc acc]
   (let [line (first lines)]
     (if (nil? line)
       {:traces-from (conj (or (:traces-from acc) {}) traces-from-acc)
        :errors (concat (:errors acc) errors-acc)}
       (let [[hid hlevel] (extract-header line)
             next-id (or hid current-id)
             refs (extract-refs line)
             errors (validate-refs refs line-num (:name file) ids-map)]
         (recur (next lines)
                (inc line-num)
                (or hid current-id)
                (or hlevel current-level)
                ids-map
                file
                (conj-traces-from traces-from-acc next-id refs)
                (concat errors-acc errors)
                acc))))))

(defn make-trace-map-from 
  ;; 1. Go through each file preserving previously defined requirement
  ;; 2. Find each reference and store it into the associated map
  ;; 3. Build the reverse map
  ([md-files ids-map] (make-trace-map-from md-files ids-map {}))
  ([md-files ids-map acc]
   (if (nil? (first md-files))
     acc
     (recur (next md-files)
            ids-map
            (make-traces-from (first md-files) ids-map acc)))))

;;-------------

(defn append-trace-to
  ([traces-from k acc]
   (let [pairs (into [] traces-from)]
     (append-trace-to (first pairs) (next pairs) k acc)))
  
  ([curr-pair pairs k acc]
   (if (nil? curr-pair)
     acc
     (let [curr-acc (or (get acc k) #{})]
       (if (contains? (second curr-pair) k)
         (append-trace-to (first pairs) (next pairs) k (assoc acc k (conj curr-acc (first curr-pair))))
         (append-trace-to (first pairs) (next pairs) k acc))))))

(defn make-trace-map-to
  ([traces-from] (make-trace-map-to traces-from (first (keys traces-from)) (next (keys traces-from)) {}))
  ([traces-from k ks acc]
   (if (nil? k)
     acc
     (recur traces-from (first ks) (next ks) (append-trace-to traces-from k acc)))))

;;-------------

(defn clean-traces-from [traces-set ids-map]
  (into #{}
  (filter (fn [e] (not (nil? e)))
          (map #(let [v (get ids-map %)]
                  (if-not (nil? v)
                    (:ref-to v)))
               traces-set))))

;;-------------

(defn process-header [line]
  (let [hid (reader/extract-head-id line)]
    (if-not (nil? hid)
      (str line " {#" hid "}")
      line)))

(defn process-local-id-def [line]
  (s/replace line reader/*MIDDLE-ID* "$1"))

(defn process-ref
  ([line ids-map]
   (let [m (re-matcher *REF* line)]
     (process-ref (re-find m) m line ids-map)))

  ([result matcher line ids-map]
   (if (nil? result)
     line
     (let [id-key (last result)
           id-ref (:ref-to (get ids-map id-key))
           id-text (:text (get ids-map id-key))
           line2 (s/replace line (first result) (str "[" id-key ": " id-text "](#" id-ref ")"))
           matcher2 (re-matcher *REF* line2)]
       (recur (re-find matcher)
              matcher2
              line2
              ids-map)))))

(defn process-ref-id
  ([line ids-map]
   (let [m (re-matcher *REF-ID* line)]
     (process-ref-id (re-find m) m line ids-map)))

  ([result matcher line ids-map]
   (if (nil? result)
     line
     (let [id-key (last result)
           id-ref (:ref-to (get ids-map id-key))
           line2 (s/replace line (first result) (str "[" id-key "](#" id-ref ")"))
           matcher2 (re-matcher *REF-ID* line2)]
       (recur (re-find matcher)
              matcher2
              line2
              ids-map)))))


(defn process-ref-text
  ([line ids-map]
   (let [m (re-matcher *REF-TEXT* line)]
     (process-ref-text (re-find m) m line ids-map)))

  ([result matcher line ids-map]
   (if (nil? result)
     line
     (let [id-key (last result)
           id-ref (:ref-to (get ids-map id-key))
           id-text (:text (get ids-map id-key))
           line2 (s/replace line (first result) (str "[" id-text "](#" id-ref ")"))
           matcher2 (re-matcher *REF-TEXT* line2)]
       (recur (re-find matcher)
              matcher2
              line2
              ids-map)))))

(defn process-refs-to [line ids-map traces-to]
  (let [found (re-find *REFS-TO* line)]
    (if (nil? found)
      line
      (let [lexema (first found)
            id-full (last found)
            hid (:ref-to (get ids-map id-full))
            traces (get traces-to hid)
            res (reduce #(str %1 "[" %2 "](#" %2 "), ") "" traces)
            res2 (if (> (count res) 2) (subs res 0 (- (count res) 2)) res)]
        (recur (s/replace line lexema res2)
               ids-map
               traces-to)))))

(defn process-refs-from [line ids-map traces-from]
  (let [found (re-find *REFS-FROM* line)]
    (if (nil? found)
      line
      (let [lexema (first found)
            id-full (last found)
            hid (:ref-to (get ids-map id-full))
            traces (get traces-from hid)
            res (reduce #(str %1 "[" %2 "](#" %2 "), ") "" traces)
            res2 (if (> (count res) 2) (subs res 0 (- (count res) 2)) res)]
        (recur (s/replace line lexema res2)
               ids-map
               traces-from)))))


(defn process-line [line ids-map traces-to traces-from]
  ;; 1. If this is header with ID -- add header marker.
  ;; 2. If line contains #id:ID -- local ID definitions, -- substitute it with normal text.
  ;; 3. If line contains refs -- substitute them with links.
  ;; 4. If line contains traceability -- list traces.
  (-> line
      process-header
      process-local-id-def
      (process-ref ids-map)
      (process-ref-id ids-map)
      (process-ref-text ids-map)
      (process-refs-to ids-map traces-to)
      (process-refs-from ids-map traces-from)
      ))

(defn process-file
  ([file ids-map traces-to traces-from]
   (let [lines (s/split-lines (:markdown file))]
     (process-file (first lines) (next lines) ids-map traces-to traces-from [])))
  ([line lines ids-map traces-to traces-from acc]
   (if (nil? line)
     (reduce #(str %1 "\n" %2) acc)
     (recur (first lines)
            (next lines)
            ids-map
            traces-to
            traces-from
            (conj acc (process-line line ids-map traces-to traces-from))))))


;;;;;;;;;
;; API

(defn process-md-files [md-files]
  (let [ids-set (verify-id-uniqueness md-files)
        ids-map (make-ids-map md-files)
        tracing-result (make-trace-map-from md-files ids-map) ;; might return list of errors
        traces-from (update-vals (:traces-from tracing-result) #(clean-traces-from % ids-map))]

    (if-not (empty? (:errors tracing-result))
      {:result nil
       :errors (:errors tracing-result)}

    (let [trace-to (make-trace-map-to traces-from)
          processed (map #(process-file % ids-map trace-to traces-from) md-files)]
      {:result (reduce #(str %1 "\n\n" %2) processed)
       :errors nil}))))
