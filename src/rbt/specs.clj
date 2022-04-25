(ns rbt.specs
  (:require [clojure.spec.alpha :as s]))

(s/def :reader/id string?)
(s/def :reader/ref-to string?)
(s/def :reader/text string?)

(s/def :reader/ids-elem (s/keys :req [:reader/id :reader/ref-to :reader/text]))
(s/def :reader/ids (s/coll-of :reader/ids-elem))
(s/def :reader/markdown string?)
(s/def :reader/name string?)

(s/def :reader/file-result (s/keys :req [:reader/ids :reader/markdown :reader/name]))

(s/def :processor/errors (s/nilable (s/coll-of map?)))
(s/def :processor/result string?)
(s/def :processor/ids-map (s/map-of string? :reader/ids-elem))
(s/def :processor/traces-from (s/map-of string? (s/coll-of string?)))
(s/def :processor/traces-to (s/map-of string? (s/coll-of string?)))

(s/def :processor/process-result (s/keys :req [:processor/result :processor/errors]
                                         :opt [:processor/ids-map :processor/traces-from :processor/traces-to]))
