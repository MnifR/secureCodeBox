---
title: "Screenshooter"
category: "scanner"
type: "Application"
state: "not released"
usecase: "Takes Screenshots of websites"
---
![firefox logo](https://3u26hb1g25wn1xwo8g186fnd-wpengine.netdna-ssl.com/files/2019/10/logo-firefox.svg)

This integration takes screenshots of websites. This can be extremely helpful when you are using the secureCodeBox to scan numerous services and want to get a quick visual overview of each service.

## Deployment

The scanType can be deployed via helm.

```bash
helm upgrade --install screenshooter ./scanners/screenshooter/
```

### Configuration

You have to provide only the URL to the screenshooter. Be careful, the protocol is mandatory:
* `https://secureCodeBox.io`
* **not** `secureCodeBox.io` or `www.secureCodeBox.io`
