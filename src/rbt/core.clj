(ns rbt.core
  (:require [rbt.processor :as processor]
            [rbt.reader :as reader])
  (:gen-class))


(defn -main [& args]
  (let [project-file (-> "reqs.edn" slurp read-string)
        src-dir (or (:src project-file) "src")
        document (or (:document project-file) "document.md")
        files-list (map #(str src-dir java.io.File/separator %) (:files project-file))
        md-files (doall (map reader/read-md-file files-list))
        result (processor/process-md-files md-files)]
    (if (:errors result)
      (do (println (:errors result))
          (System/exit -1))
      (spit document (:result result)))))
