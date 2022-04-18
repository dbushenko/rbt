(ns rbt.core
  (:require [rbt.processor :as processor]
            [rbt.reader :as reader]
            [rbt.treeprinter :as tp]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(def cli-options
  [
   ["-d" "--dependent REQUIREMENT" "Show dependent requirement"]
   ["-p" "--project FILE" "Project file"
    :default "reqs.edn"]
   ["-h" "--help"]])

(defn print-help [summary]
  (println "No arguments -- build project.\n")
  (println "The 'rbt' usage options:")
  (println summary))


(defn process-project [project-file-name]
  (let [project-file (-> project-file-name slurp read-string)
        src-dir (or (:src project-file) "src")
        document (or (:document project-file) "document.md")
        files-list (map #(str src-dir java.io.File/separator %) (:files project-file))
        md-files (doall (map reader/read-md-file files-list))
        result (processor/process-md-files md-files)]
    (if (:errors result)
      (do 
        (dorun (map println (:errors result)))
        (System/exit -1))
      {:result result
       :document document})))

;;;;;;  

(defn build-project [project-file-name]
  (let [processed (process-project project-file-name)
        result (:result processed)
        document (:document processed)]
    (spit document (:result result))))
  
(defn build-dependent-tree [project-file-name requirements]
  (let [processed (process-project project-file-name)
        result (:result processed)
        traces-to (:traces-to result)]
    (dorun
     (map (fn [req]
            (let [node (tp/make-node req)
                  deps (tp/build-tree! traces-to node)]
              (println "Requirement" req)
              (println "~~~~~~~~~~~~~~~~~~~~")
              (println "Dependency tree:\n")
              (tp/print-tree node)
              (println "\nDependent requirements list:")
              (println (reduce #(str %1 %2 " " ) "" (sort deps)) "\n")))
          (clojure.string/split requirements #",")))))

(defn -main [& args]
  (let [parsed (parse-opts args cli-options)
        opts (:options parsed)]
    (cond
      (:dependent opts) (build-dependent-tree (:project opts) (:dependent opts))
      (:help opts) (print-help (:summary parsed))
      (empty? (:arguments parsed)) (build-project (:project opts))
      :else (print-help (:summary parsed)))))
