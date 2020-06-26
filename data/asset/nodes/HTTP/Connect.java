
boolean _{node.id}_restOK = false;

try {
  SSLContext _{node.id}_ctx = SSLContext.getInstance("TLS");
  _{node.id}_ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
  SSLContext.setDefault(_{node.id}_ctx);

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
} catch (NoSuchAlgorithmException e) {
    out{3} = e.getMessage();
} catch (KeyManagementException e) {
    out{3} = e.getMessage();
}

if (_{node.id}_restOK) {
  exec{0}
} else {
  exec{2}
}
