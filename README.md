## Repl Env for a cljs Desktop Extension

Install the first version of the extension in the normal way but when you want to debug the ui react component, do the following:

```
cd ui
npm ci
npm run watch
docker extension dev ui-source slimslenderslacks/watch-images http://localhost:8280
```

Now close the desktop console and re-open it.  Click on the extension and it will load the app served from http://localhost:8280 and provides a js runtime for repl websocket to connect.  That worked for me.

## Using Mui Material components

They're just regular components so we use the standard pattern.

```clojure
(defn view []
  [Card {:sx {:minWidth 275}}
    [CardContent
      [Typography {:variant "h3" :sx {:fontSize 14} :color "text.secondary"} 
        "recent events"]]])
```

However, I created three symbols `Card`, `CardContent`, and `Typography` using some tricks that took me a bit of time to figure out so perhaps worth sharing.

I require the mui components in the normal way.

```clojure
(ns whatever
  (:require ["@mui/material/Typography" :as MuiTypography]))

(def Typography (doto
                 (r/adapt-react-class (.-default MuiTypography))
                 ((fn [m] (set! (.-displayName m) "typography")))))
```

So you have to do a little ceremony to get the Symbol that you'll put in the function.  But then it's back to just functions producing data again.

* do the require of the npm module
* remember that this is ES6 so the actual exports are in `(.-default MuiTypography)`
* The standard reagent adapter should work everywhere that doesn't pass Components as properties (I still don't understand why that's really ever needed - shouldn't that always be constrained to child nodes?)
* I just set the `displayName` so things look nice in debuggers.


