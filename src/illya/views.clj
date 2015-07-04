;; Copyright (c) 2015, Dmitry Kozlov <kozlov.dmitry.a@gmail.com>
;; Code is published under BSD 2-clause license

(ns illya.views
  (:require [hiccup.core :as hc :refer [html]]
            [spyscope.core :as spy]))

(defn render [view]
  (hc/html view))

(defn menu-template [div-style]
  (letfn [(menu-item [url shortcut title]
            [div-style
             \[ [:a {:href url} shortcut] \] title])]
    [(menu-item "/board/a" "a" "utism")
     (menu-item "/board/c" "c" "omputer science")
     (menu-item "/faq" "f" "aq")]))

(defn index-template []
  (let [page-title "プリズマ☆イリヤちゃん"]
    (letfn [(menu-item [url shortcut title]
              [:div.front-menu-item
               \[ [:a {:href url} shortcut] \] title])]
      [:html
       [:head
        [:title page-title]
        [:link {:rel "stylesheet" :href "/static/theme.css"}]]
       [:body
        [:center
         [:h3.front page-title]
         [:img {:src "/static/front.jpg"
                :width "500px"}]
         [:hr.front]
         (into [:div.front-menu] (menu-template :div.front-menu-item))]]])))

(defn header-template []
  [:div.header
   [:div.header-title [:h3 "プリズマ☆イリヤちゃん"]]
   (into [:div.header-menu] (menu-template :div.header-menu-item))
   [:hr.header]])

(defn posting-template [{post-url :post-url post-header :post-header}]
  [:div.posting
   [:h3.posting post-header]
   [:form.posting-form
    {:method "POST" :action post-url}
    [:div.posting-title
     [:div.posting-title-label "Title:"]
     [:input.posting-title-input {:type "text" :name "title"}]]
    [:div.posting-text
     [:div.posting-text-label "Text:"]
     [:textarea.posting-text-area {:name "text"}]]
    [:div.posting-submit
     [:input.posting-submit-button {:type "submit" :value "Ｐ Ｏ Ｓ Ｔ"}]]]])

(defn footer-template []
  [:div.footer
   [:hr.footer]
   [:div.footer-text
    "プリズマ☆イリヤちゃん"]])

(defn post-header-template [tag reply args]
  (let [control (:control args)
        {number :number
         date :created-date
         title :title} reply]
    [tag
     [:div.post-header-number number]
     [:div.post-header-name (str "[" title "]")]
     ;;[:div.post-header-user u-id]
     [:div.post-header-date (str "(" date ")")]
     [:div.post-header-menu
      (if (:permitted control)
        [:div.post-header-menu-item
         "["
         [:a {:href (str "/board/"
                         (name (:board args))
                         "/control/hide/" number
                         "?control=" (:key control)
                         "&redirect=" (:thread args))} "hide"]
         "]"])]]))

(defn thread-reply-template [reply args]
  [:div-thread-post
   (post-header-template :div.thread-post-header reply args)
   [:div.thread-post-html (:contents reply)]])

(defn thread-contents-template [data args]
  (let [{original-post :message
         replies :replies} data]
    [:div.thread
     [:div.thread-op-post
      (post-header-template :div.thread-op-post-header original-post args)
      [:div.thread-op-post-html (:contents original-post)]]
     (for [reply replies]
       [:div.thread-reply
        (post-header-template :div.thread-reply-header reply args)
        [:div.thread-reply-html (:contents reply)]])]))

(defn thread-template [data args]
  [:html
   [:head
    [:title (:title (:message data))]
    [:link {:rel "stylesheet" :href "/static/theme.css"}]]
   [:body
    (header-template)
    (posting-template args)
    (thread-contents-template data args)
    (footer-template)]])

(defn board-thread-header-template [message url args]
  (let [control (:control args)
        {number :number
         date :created-date
         title :title} message]
    [:div.board-thread-header
     [:div.board-thread-header-number number]
     [:div.board-thread-header-name (str "[" title "]")]
     ;;[:div.board-thread-header-user u-id]
     [:div.board-thread-header-date (str "(" date ")")]
     [:div.board-thread-header-menu
      (if (:permitted control)
        [:div.board-thread-header-menu-item
         "["
         [:a {:href (str "/board/"
                         (:board args)
                         "/control/hide/" number "?control=" (:key control))} "hide"]
         "]"])
      [:div.board-thread-header-menu-item
       "["
       [:a {:href url} "view"]
       "]"]]]))

(defn board-contents-template [data args]
  [:div.board
   (for [thread-info data]
     (let [message (:message thread-info)]
       [:div.board-thread
        [:div.board-thread-op-post
         (board-thread-header-template
          message
          (str "/board/" (:board args) "/thread/" (:number message))
          args)
         [:div.board-thread-op-post-html (:contents message)]]
        [:div.board-thread-footer]]))])

(defn board-template [data args]
  [:html
   [:head
    [:title (str "「" (:board args) "」プリズマ☆イリヤちゃん")]
    [:link {:rel "stylesheet" :href "/static/theme.css"}]]
   [:body
    (header-template)
    (posting-template args)
    (board-contents-template data args)
    (footer-template)]])

(defn qa-template [q a]
  [:p
   [:div.faq-q q]
   [:div.faq-a a]])

(defn faq-template []
  [:html
   [:head
    [:title "「ＦＡＱ」プリズマ☆イリヤちゃん"]
    [:link {:rel "stylesheet" :href "/static/theme.css"}]]
   [:body
    (header-template)
    [:div.faq
     [:div.faq-body
      [:div.faq-header [:b "「ＦＡＱ」プリズマ☆イリヤちゃん"]]
      [:div.faq-contents [:center "Coming Soon"]]]]
    (footer-template)]])

(defn not-found-template []
  [:html
   [:head
    [:title "404"]]
   [:body
    [:center
     [:h3 "ＨＴＴＰ４０４何もありません"]]]])

(defn authorization-template [{key :key}]
  [:html
   [:head
    [:title "「ＡＵＴＨ」プリズマ☆イリヤちゃん"]
    [:link {:rel "stylesheet" :href "/static/theme.css"}]]
   [:body
    [:div.authorization
     [:h3.authorization "Authorization Required"]
     [:form.authorization-form
      {:method "POST" :action (str "/auth?control=" key)}
      [:div.authorization-group
       [:div.authorization-label "User"]
       [:input.authorization-input {:type "text" :name "user"}]]
      [:div.authorization-group
       [:div.authorization-label "Pass"]
       [:input.authorization-input {:type "text" :name "pass"}]]
      [:div.authorization-group
       [:input.authorization-submit-button {:type "submit" :value "Ａ Ｕ Ｔ Ｈ"}]]]]]])

