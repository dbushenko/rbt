(ns rbt.compare
  (:require [rbt.reader :as reader]
            [rbt.processor :as processor]
            [clojure.string :as s]
            [clojure.set])
  (:import [org.bitbucket.cowwoc.diffmatchpatch DiffMatchPatch])
  (:gen-class))


;; Collects requirements into the map {ID->requirement}
(defn collect-requirements
  ([file-text]
   (let [lines (filter #(not (empty? %)) (s/split-lines file-text))]
     (collect-requirements (first lines) (next lines) nil nil "" {})))
  
  ([line lines current-level current-id acc reqs]
   (if (nil? line)
     (if-not (nil? current-id)
       (assoc reqs current-id acc)
       reqs)
     
     (let [req-id (reader/extract-head-id line)
           hdr (re-find processor/*LEVEL-PATTERN* line)
           level (if (not (nil? hdr)) (count hdr))]
       (cond

         ;; Skip the lines of the document without requirements
         (and (nil? current-id) (nil? req-id))
         (collect-requirements (first lines) (next lines) nil nil nil reqs)

         ;; New requirement defined
         (and (nil? current-id) (not (nil? req-id)))
         (collect-requirements (first lines) (next lines) level req-id "" reqs)
         
         ;; Lines belonging to the same requirement, not heading defenition
         (and (not (nil? current-id)) (nil? hdr))
         (collect-requirements (first lines) (next lines) current-level current-id (str acc line "\n") reqs)

         ;; Child heading defenition without requirement ID definition
         (and (not (nil? current-id)) (not (nil? hdr)) (nil? req-id) (< current-level level))
         (collect-requirements (first lines) (next lines) current-level current-id (str acc line "\n") reqs)

         ;; Sibling or new parent heading defenition without requirement ID definition
         (and (not (nil? current-id)) (not (nil? hdr)) (nil? req-id) (>= current-level level))
         (collect-requirements (first lines) (next lines) nil nil nil (assoc reqs current-id acc))

         ;; New requirement definition, old requirement exists
         (and (not (nil? current-id)) (not (nil? req-id)))
         (collect-requirements (first lines) (next lines) level req-id nil (assoc reqs current-id acc))

         ;; New requirement definition, no old requirement exists
         (and (nil? current-id) (not (nil? req-id)))
         (collect-requirements (first lines) (next lines) level req-id nil reqs)

         :else (throw (RuntimeException. (str "Comparing unknown case for line: " line)))
         )))))


(defn find-different-reqs
  ([old-reqs new-reqs core-keys]  (find-different-reqs old-reqs new-reqs (next core-keys) (first core-keys) []))
  ([old-reqs new-reqs core-keys curr acc]
   (if (nil? curr)
     acc
     (let [dmp (DiffMatchPatch.)
           res (.diffMain dmp (get old-reqs curr) (get new-reqs curr))
           not-equals (filter #(not (s/starts-with? % "Diff(EQUAL")) (map #(.toString %) res))]
       (if (not (empty? not-equals))
         (find-different-reqs old-reqs new-reqs (next core-keys) (first core-keys) (conj acc curr))
         (find-different-reqs old-reqs new-reqs (next core-keys) (first core-keys) acc) )))))


(defn compare-md-files [old new]
  ;; 1. Go through each file
  ;;    a. Collect a map of requirements (ID -> requirement). Collect each till the header of same or upper level
  ;; 2. Go through the keys of each requirement and compare the body of the requirement
  ;; 3. Print the diff between the requirements
  ;; 4. Print the list of req ids which differ in those specs

  (let [old-reqs (collect-requirements old)
        new-reqs (collect-requirements new)
        old-keys (into #{} (keys old-reqs))
        new-keys (into #{} (keys new-reqs))
        core-keys (clojure.set/intersection old-keys new-keys)
        different-keys (find-different-reqs old-reqs new-reqs core-keys)]
    (println "Removed requirements:")
    (println (into [] (clojure.set/difference old-keys core-keys)))

    (println "\nAdded requirements:")
    (println (into [] (clojure.set/difference new-keys core-keys)))

    (println "\nDifferent requirements:")
    (println different-keys)))
