{
  "types": [ { "id":1, "name":"HttpURLConnection", "color":"darkgreen" } ],
  "nodes": [
    {
      "name": "HTTP Create Connection",
      "type": 4,
      "version": 1,
      "import": ["java.io.IOException","java.net.HttpURLConnection","javax.net.ssl.HttpsURLConnection","java.net.URL",
        "java.security.SecureRandom",
        "java.security.cert.CertificateException",
        "java.security.cert.X509Certificate",
        "javax.net.ssl.HostnameVerifier",
        "javax.net.ssl.HttpsURLConnection",
        "javax.net.ssl.KeyManager",
        "javax.net.ssl.SSLContext",
        "javax.net.ssl.SSLSession",
        "javax.net.ssl.TrustManager",
        "javax.net.ssl.X509TrustManager"
      ],
      "options": { "javaInputArray": true },
      "java":"@Connect.java",
      "input": [
        { "type": "Exec", "label": "" },
        { "type": "String", "label": "URL", "value": "http://localhost:8080", "single_line":true }
      ],
      "output": [
        { "type": "Exec", "label": "Success" },
        { "type": "HttpURLConnection", "label": "Connection", "java":{"references":{"variable":"httpConn"}} },
        { "type": "Exec", "label": "Error" },
        { "type": "String", "label": "Message", "java":{"references":{"variable":"msg"}} }
      ]
    },
    {
      "name": "HTTP Disconnect",
      "type": 4,
      "version": 1,
      "import": ["java.net.HttpURLConnection","java.net.URL"],
      "java":"in{1}.disconnect();",
      "input": [
        { "type": "Exec", "label": "" },
        { "type": "HttpURLConnection", "label": "Connection" }
      ],
      "output": [
        { "type": "Exec", "label": "" }
      ]
    },
    {
      "name": "REST Request",
      "type": 4,
      "version": 1,
      "import": ["java.io.BufferedReader","java.io.IOException","java.io.InputStream", "java.io.InputStreamReader",
                 "java.io.OutputStream","java.net.HttpURLConnection","java.net.URL","org.json.simple.JSONObject"],
      "jar": ["lib/json-simple-1.1.1.jar"],
      "options": { "javaInputArray": true },
      "java":"@RESTRequest.java",
      "input": [
        { "type": "Exec", "label": "" },
        { "type": "Integer", "label": "Method", "value": 0, "enum": [ "GET", "PUT", "POST", "DELETE" ] },
        { "type": "HttpURLConnection", "label": "Connection" },
        { "type": "JSONObject", "label": "Data" }
      ],
      "output": [
        { "type": "Exec", "label": "Success" },
        { "type": "Integer", "label": "Status", "java":{"references":{"variable":"status"}} },
        { "type": "String", "label": "Response", "java":{"references":{"variable":"response"}} },
        { "type": "Exec", "label": "Error" },
        { "type": "String", "label": "Message", "java":{"references":{"variable":"msg"}} }
      ]
    }
  ]
}
