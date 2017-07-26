(ns server.db)

(defonce db (atom {}))

(defn store [k v]
  (swap! db assoc k v))

(defn retrieve [k]
  (get @db k))

(defn matches [v]
  (map first (filter #(= (second %) v) @db)))

(defn env [name]
  {:pre [(string? name)]}
  (aget js/process "env" name))
