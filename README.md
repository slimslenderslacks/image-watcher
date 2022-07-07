## Repl Env for a cljs Desktop Extension

Install the first version of the extension in the normal way but when you want to debug the ui react component, do the following:

```
cd ui
npm ci
npm run watch
docker extension dev ui-source slimslenderslacks/watch-images http://localhost:8280
```

Now close the desktop console and re-open it.  Click on the extension and it will load the app served from http://localhost:8280 and provides a js runtime for repl websocket to connect.  That worked for me.

