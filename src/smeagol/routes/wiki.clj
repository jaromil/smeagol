(ns smeagol.routes.wiki
  (:use clojure.walk)
  (:require [compojure.core :refer :all]
            [noir.io :as io]
            [noir.response :as response]
            [smeagol.layout :as layout]
            [smeagol.util :as util]))

(defn process-source
  "Process `source-text` and save it to the specified `file-path`, finally redirecting to wiki-page"
  [file-path source-text request]
  (let [params (keywordize-keys (:params request))
        content (or (:content params) "Introduction")]
    (spit file-path source-text)
    (response/redirect (str "wiki?" content))
  ))

(defn edit-page
  "Render a page in a text-area for editing. This could have been done in the same function as wiki-page,
  and that would have been neat, but I couldn't see how to establish security if that were done."
  [request]
  (let [params (keywordize-keys (:params request))
        src-text (:src params)
        content (:content params)
        file-name (str "/content/" content ".md")
        file-path (str (io/resource-path) file-name)
        exists? (.exists (clojure.java.io/as-file file-path))]
    (cond src-text (process-source file-path src-text request)
          true
          (layout/render "edit.html"
                   {:title content
                    :left-bar (util/md->html "/content/_edit-left-bar.md")
                    :header (util/md->html "/content/_header.md")
                    :content (if exists? (io/slurp-resource file-name) "")}))))

(defn local-links
  [html-src]
  (clojure.string/replace html-src #"\[\[[^\[\]]*\]\]"
                          #(let [text (clojure.string/replace %1 #"[\[\]]" "")]
                             (str "<a href='wiki?content=" text "'>" text "</a>"))))

(defn wiki-page
  "Render the markdown page specified in this `request`, if any. If none found, redirect to edit-page"
  [request]
  (let [params (keywordize-keys (:params request))
        content (or (:content params) "Introduction")
        file-name (str "/content/" content ".md")
        file-path (str (io/resource-path) file-name)
        exists? (.exists (clojure.java.io/as-file file-path))]
    (cond exists?
      (layout/render "wiki.html"
                   {:title content
                    :left-bar (util/md->html "/content/_left-bar.md")
                    :header (util/md->html "/content/_header.md")
                    :content (local-links (util/md->html file-name))})
          true (response/redirect (str "edit?content=" content)))))


(defn about-page []
  (layout/render "about.html"))

(defroutes wiki-routes
  (GET "/wiki" request (wiki-page request))
  (GET "/" request (wiki-page request))
  (GET "/edit" request (edit-page request))
  (POST "/edit" request (edit-page request))
  (GET "/about" [] (about-page)))