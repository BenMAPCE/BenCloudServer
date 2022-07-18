# BenMAP API Security Scanning

We use the [OWASP Zed Attack Proxy (ZAP)](https://www.zaproxy.org/) to scan the BenMAP API for vulnerabilities.

Once the ZAP tool is installed, you will need to add an HTTP Sender script to simulate the EPA WAM authentication by adding uid and ismemberof headers. You can add the doc/Add WAM Headers.js script in this repo as an "ECMAScript : Oracle Nashorn" script and edit the header values as needed.

Next, import the openapi.yaml file into ZAP and run an attack while the API is running on your localhost.

**Note: When the BenMAP API is not running behind an authentication proxy, it is not secure. It should only be run in this way to facilitate security scanning or development and testing.**