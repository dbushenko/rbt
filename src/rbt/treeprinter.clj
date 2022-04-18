(ns rbt.treeprinter
  (:import [hu.webarticum.treeprinter SimpleTreeNode]
           [hu.webarticum.treeprinter.printer.listing ListingTreePrinter]))

(definterface HasParent
  (parent []))

(defn make-node [ text ]
  (proxy
      [SimpleTreeNode HasParent]
      [^String text]
    (parent [] nil)))

(defn make-node-with-parent [parent-node text]
  (proxy
      [SimpleTreeNode HasParent]
      [^String text]
    (parent [] parent-node)))

(defn add-child! [parent-node text]
  (let [n (make-node-with-parent parent-node text)]
    (.addChild parent-node n)
    n))

(defn children [node]
  (.children node))

(defn content [node]
  (.content node))

(defn parent [node]
  (.parent node))

;; starts from the first parent, ends with last parent
(defn all-parents
  ([node] (all-parents node []))
  ([node acc]
   (let [p (parent node)]
     (if (nil? p)
       acc
       (recur p (conj acc p))))))

(defn print-tree [ node ]
  (.print (ListingTreePrinter.) node))




(defn build-tree!
  ([deps-to-map root] (build-tree! deps-to-map root (atom #{})))
  ([deps-to-map root acc]
   (let [rid (content root)
         deps (get deps-to-map rid)]
     (if (empty? deps)
       @acc
       
       (loop [d (first deps)
              ndeps (next deps)
              ancestors (into #{} (map content (conj (all-parents root) root)))]
         (if (nil? d)
           @acc
           
           (let [nd (add-child! root d)]
             (swap! acc #(conj % d))
             
             (if-not (contains? ancestors d) (build-tree! deps-to-map nd acc) root)
             
             (recur (first ndeps)
                    (next ndeps)
                    (into #{} (conj ancestors d)) ))))))))

