
boolean _{node.id}_restOK = false;

try {
  //Class.forName("{className}$DefaultTrustManager");

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

  if (!in{2}.isEmpty() && !in{3}.isEmpty()) {
    String _{node.id}_auth = in{2} + ":" + in{3};
    byte[] _{node.id}_encodedAuth = Base64.getEncoder().encode(_{node.id}_auth.getBytes(StandardCharsets.UTF_8));

    String _{node.id}_authHeaderValue = "Basic " + new String(_{node.id}_encodedAuth);

    out{1}.setRequestProperty("Authorization", _{node.id}_authHeaderValue);
  }

  _{node.id}_restOK = true;

} catch (IOException e) {
    out{3} = e.getMessage();
} catch (NoSuchAlgorithmException e) {
    out{3} = e.getMessage();
} catch (KeyManagementException e) {
    out{3} = e.getMessage();
} /*catch (ClassNotFoundException e) {
    out{3} = "Class not found: "+e.getMessage();
}*/

if (_{node.id}_restOK) {
  exec{0}
} else {
  exec{2}
}
