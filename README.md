## Developing

```
docker extension enable
docker extension validate slimslenderslacks/watch-images:latest
docker extension install slimslenderslacks/watch-images
```

### Repl Env for a cljs Desktop Extension

Install the first version of the extension in the normal way but when you want to debug the ui react component, do the following:

```
cd ui
npm ci
npm run watch
docker extension dev ui-source slimslenderslacks/watch-images http://localhost:8280
```

Now close the desktop console and re-open it.  Click on the extension and it will load the app served from http://localhost:8280 and provides a js runtime for repl websocket to connect.  That worked for me.

### Using Mui Material components

They're just regular components so we use the standard reagent pattern.

```clojure
(defn view []
  [Card {:sx {:minWidth 275}}
    [CardContent
      [Typography {:variant "h3" :sx {:fontSize 14} :color "text.secondary"} 
        "recent events"]]])
```

There are three symbols `Card`, `CardContent`, and `Typography` in the above view. We use some tricks that took some time to figure out what they should reference.  So worth documenting here.

You build one by requiring it and then adapting it to reagent.

```clojure
(ns whatever
  (:require ["@mui/material/Typography" :as MuiTypography]))

(def Typography (doto
                 (r/adapt-react-class (.-default MuiTypography))
                 ((fn [m] (set! (.-displayName m) "typography")))))
```

It's a little ceremony, but once it's complete, the views are just data again.

* the `require` of the npm module uses the standard shadow-cljs method.
* you need to know that this is ES6 so the actual exports are in `(.-default MuiTypography)`
* The standard reagent adapter should work everywhere that doesn't pass Components as properties (I still don't understand why that's really ever needed - shouldn't that always be constrained to child nodes?)
* I just set the `displayName` so things look nice in debuggers.

