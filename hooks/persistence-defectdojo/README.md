---
title: "DefectDojo"
category: "hook"
type: "persistenceProvider"
state: "released"
usecase: "Publishes all Scan Reports to OWASP DefectDojo."
---

## About

The DefectDojo hook imports the reports from scans automatically into [OWASP DefectDojo](https://www.defectdojo.org/).
The hook uses the import scan [API from DefectDojo](https://defectdojo.readthedocs.io/en/latest/api-v2-docs.html) to import the scan results.

This means that only scan types are supported by the hook which are both supported by the secureCodeBox and DefectDojo.
These are:

- Nmap
- ZAP (Baseline, API Scan and Full Scan)
- SSLyze
- Trivy
- Gitleaks

:::caution

Nikto is currently **not** supported even though it's supported by the secureCodeBox and DefectDojo as the secureCodeBox
uses the Nikto JSON format while DefectDojo uses the XML format.

:::

After uploading the results to DefectDojo, it will use the findings parsed by DefectDojo to overwrite the
original secureCodeBox findings identified by the parser. This lets you access the finding metadata like the false
positive and duplicate status from DefectDojo in further ReadOnly hooks, e.g. send out Slack notification
for non-duplicate & non-false positive findings only.

:::caution

Be careful when using the DefectDojo Hook in combination with other ReadAndWrite hooks. The secureCodeBox currently has
no way to guarantee that one ReadAndWrite hook gets executed before another ReadAndWrite hook. This can lead to
"lost update" problems as the DefectDojo hook will overwrite all findings, which disregards the results of previously
run ReadAndWrite hooks.
ReadOnly hooks work fine with the DefectDojo hook as they are always executed after ReadAndWrite Hooks.
:::

## Runtime Configuration

The hook will automatically import the scan results into an engagement in DefectDojo.
If the engagement doesn't exist the hook will create the engagement (CI/CD engagement) and all objects required for it
(product & product type).

You don't need any configuration for that to work, the hook will infer engagement & product names from the scan name.
If you want more control over the names or add additional meta information like the version of the tested software you
can add these via annotation to the scan. See examples below.

| Scan Annotation                                                    | Description                | Default if not set                                                   | Notes                                                                                 |
| ------------------------------------------------------------------ | -------------------------- | -------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| `defectdojo.securecodebox.io/product-type-name`                    | Name of the Product Type   | Product Type with ID 1 (typically "Research and Development")        | Product Type will be automatically created if no Product Type under that name exists  |
| `defectdojo.securecodebox.io/product-name`                         | Name of the Product        | ScheduledScan Name if Scheduled, Scan Name if it's a standalone Scan | Product will be automatically created if no Product under that name exists            |
| `defectdojo.securecodebox.io/product-description`                  | Description of the Product | Empty String                                                         | Only used when creating the Product not used for updating                             |
| `defectdojo.securecodebox.io/product-tags`                         | Product Tags               | Nothing                                                              | Only used when creating the Product not used for updating                             |
| `defectdojo.securecodebox.io/engagement-name`                      | Name of the Engagement     | Scan Name                                                            | Will be automatically created if no *engagement* with that name **and** version exists |
| `defectdojo.securecodebox.io/engagement-version`                   | Engagement Version         | Nothing                                                              |                                                                                       |
| `defectdojo.securecodebox.io/engagement-deduplicate-on-engagement` | Deduplicate On Engagement  | false                                                                | Only used when creating the Engagement not used for updating                          |
| `defectdojo.securecodebox.io/engagement-tags`                      | Engagement Tags            | Nothing                                                              | Only used when creating the Engagement not used for updating                          |
| `defectdojo.securecodebox.io/test-title`                           | Test Title                 | Scan Name                                                            |                                                                                       |

### Simple Example Scans

This will import the results daily into an engagements called: "zap-juiceshop-$UNIX_TIMESTAMP" (Name of the Scan created daily by the ScheduledScan), in a Product called: "zap-juiceshop" in the default DefectDojo product type.

```yaml
apiVersion: "execution.securecodebox.io/v1"
kind: ScheduledScan
metadata:
  name: "zap-juiceshop"
spec:
  interval: 24h
  scanSpec:
    scanType: "zap-full-scan"
    parameters:
      - "-t"
      - "http://juice-shop.demo-apps.svc:3000"
```

### Complete Example Scan

This will import the results into engagement, product and product type following the labels.
The engagement will be reused by the hook for the daily scans / imports until the engagement version is increased.

```yaml
apiVersion: "execution.securecodebox.io/v1"
kind: ScheduledScan
metadata:
  name: "zap-full-scan-juiceshop"
  annotations:
    defectdojo.securecodebox.io/product-type-name: "OWASP"
    defectdojo.securecodebox.io/product-name: "Juice Shop"
    defectdojo.securecodebox.io/product-description: |
      OWASP Juice Shop is probably the most modern and sophisticated insecure web application!
      It can be used in security trainings, awareness demos, CTFs and as a guinea pig for security tools!
      Juice Shop encompasses vulnerabilities from the entire OWASP Top Ten along with many other security flaws found in real-world applications!
    defectdojo.securecodebox.io/product-tags: vulnerable,appsec,owasp-top-ten,vulnapp
    defectdojo.securecodebox.io/engagement-name: "Juice Shop"
    defectdojo.securecodebox.io/engagement-version: "v12.6.1"
    defectdojo.securecodebox.io/engagement-tags: "automated,daily"
    defectdojo.securecodebox.io/engagement-deduplicate-on-engagement: "true"
    defectdojo.securecodebox.io/test-title: "Juice Shop - v12.6.1"
spec:
  interval: 24h
  scanSpec:
    scanType: "zap-full-scan"
    parameters:
      - "-t"
      - "http://juice-shop.demo-apps.svc:3000"
```

## Deployment

Installing the DefectDojo persistenceProvider hook will add a _ReadOnly Hook_ to your namespace.

```bash
kubectl create secret generic defectdojo-credentials --from-literal="username=admin" --from-literal="apikey=08b7..."

helm upgrade --install dd secureCodeBox/persistence-defectdojo \
    --set="defectdojo.url=https://defectdojo-django.default.svc"
```

## Chart Configuration

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| defectdojo.authentication.apiKeyKey | string | `"apikey"` | Name of the apikey key in the `userSecret` secret. Use this if you already have a secret with different key / value pairs |
| defectdojo.authentication.userSecret | string | `"defectdojo-credentials"` | Link a pre-existing generic secret with `username` and `apikey` key / value pairs |
| defectdojo.authentication.usernameKey | string | `"username"` | Name of the username key in the `userSecret` secret. Use this if you already have a secret with different key / value pairs |
| defectdojo.syncFindingsBack | bool | `true` | Syncs back (two way sync) all imported findings from DefectDojo to SCB Findings Store, set to false to only import the findings to DefectDojo (one way sync). |
| defectdojo.url | string | `"http://defectdojo-django.default.svc"` | Url to the DefectDojo Instance |
| image.pullPolicy | string | `"IfNotPresent"` | Image pull policy. One of Always, Never, IfNotPresent. Defaults to Always if :latest tag is specified, or IfNotPresent otherwise. More info: https://kubernetes.io/docs/concepts/containers/images#updating-images |
| image.repository | string | `"docker.io/securecodebox/persistence-defectdojo"` | Hook image repository |
| image.tag | string | `nil` | Container image tag |
