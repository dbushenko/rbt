(ns rbt.core
  (:require [rbt.processor :as processor]
            [rbt.compare :as cmpr]
            [rbt.reader :as reader]
            [rbt.treeprinter :as tp]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(def cli-options
  [["-c" "--compare FILE" "Compare your spec to this version of spec. \n\t\t\t\t\t Use -p to specify old spec and -c to specify new spec."
    :validate [string?]]
   ["-d" "--dependent REQUIREMENT" "Show dependent requirement"]
   ["-v" "--verify" "Verify project whether it contains errors"]
   ["-p" "--project FILE" "Project file"
    :default "reqs.edn"]
   ["-h" "--help"]])

(defn print-help [summary]
  (println "No arguments -- build project.\n")
  (println "The 'rbt' usage options:")
  (println summary))

(defn get-base-dir [project-file]
  (let [nm (.getName project-file)
        ap (.getAbsolutePath project-file)
        p (.substring ap 0 (- (.length ap) (.length nm)))]
    p))

(defn process-project [project-file-name]
  (let [project-file (-> project-file-name slurp read-string)
        base-dir (get-base-dir (java.io.File. project-file-name))
        src-dir (str base-dir (or (:src project-file) "src"))
        document (str base-dir (or (:document project-file) "document.md"))
        files-list (map #(str src-dir java.io.File/separator %) (:files project-file))
        md-files (doall (map reader/read-md-file files-list))
        result (processor/process-md-files md-files)]
    (if (:errors result)
      (do 
        (dorun (map println (:errors result)))
        (System/exit -1))

      (let [document-dir-path (get-base-dir (java.io.File. document))
            document-dir (java.io.File. document-dir-path)]

        (if-not (.exists document-dir)
          (.mkdirs document-dir))
        
        {:result result
         :document document}))))

;;;;;;  

(defn build-project [project-file-name]
  (let [processed (process-project project-file-name)
        result (:result processed)
        document (:document processed)]
    (spit document (:result result))))

;;;;;;

(defn compare-projects [old-project-file-name new-project-file-name]
  (let [project-file1 (-> old-project-file-name slurp read-string)
        base-dir1 (get-base-dir (java.io.File. old-project-file-name))
        src-dir1 (str base-dir1 (or (:src project-file1) "src"))
        files-list1 (map #(str src-dir1 java.io.File/separator %) (:files project-file1))
        md-files1 (doall (map slurp files-list1))

        project-file2 (-> new-project-file-name slurp read-string)
        base-dir2 (get-base-dir (java.io.File. new-project-file-name))
        src-dir2 (str base-dir2 (or (:src project-file2) "src"))
        files-list2 (map #(str src-dir2 java.io.File/separator %) (:files project-file2))
        md-files2 (doall (map slurp files-list2))
        ]
    (cmpr/compare-md-files (reduce #(str %1 "\n" %2) md-files1)
                           (reduce #(str %1 "\n" %2) md-files2))))


(defn verify-project [project-file-name]
  (let [processed (process-project project-file-name)
        result (:result processed)
        document (:document processed)]
    (println "OK")))

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
        opts (:options parsed)
        errors (:errors parsed)]
    (cond
      (not (empty? errors)) (println errors)
      (:compare opts) (compare-projects (:project opts) (:compare opts))
      (:verify opts) (verify-project (:project opts))
      (:dependent opts) (build-dependent-tree (:project opts) (:dependent opts))
      (:help opts) (print-help (:summary parsed))
      (empty? (:arguments parsed)) (build-project (:project opts))
      :else (print-help (:summary parsed)))))

