
boolean _{node.id}_restOK = false;

try {
  URL _{node.id}_url = new URL(in{1});


  if (_{node.id}_url.getProtocol().equals("https")) {
    HttpsURLConnection _{node.id}_httpsConn = (HttpsURLConnection) _{node.id}_url.openConnection();

    _{node.id}_httpsConn.setHostnameVerifier(new HostnameVerifier() {
        @Override
        public boolean verify(String arg0, SSLSession arg1) {
            return true;
        }
    });

    out{1} = (HttpURLConnection) _{node.id}_httpsConn;
  }
  else
    out{1} = (HttpURLConnection) _{node.id}_url.openConnection();

  _{node.id}_restOK = true;

} catch (IOException e) {
    out{3} = e.getMessage();
}

if (_{node.id}_restOK) {
  exec{0}
} else {
  exec{2}
}
