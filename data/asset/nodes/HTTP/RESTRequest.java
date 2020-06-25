
boolean _{node.id}_restOK = false;

try {
  /*URL _{node.id}_url = new URL(in{2});
  HttpURLConnection _{node.id}_con = (HttpURLConnection) _{node.id}_url.openConnection();*/

  HttpURLConnection _{node.id}_con = in{2};
  String method = "";

  //System.out.println("Protocol: "+_{node.id}_con.getURL().getProtocol());

  switch (in{1}) {
    case 0:
      method = "GET";
      break;

    case 1:
      method = "POST";
      break;

    case 2:
      method = "PUT";
      break;

    case 3:
      method = "DELETE";
      break;

    default:
      break;
  }

  _{node.id}_con.setRequestMethod(method);
  //_{node.id}_con.setRequestProperty("Ocp-Apim-Subscription-Key", key);
  _{node.id}_con.setRequestProperty("Content-Type", "application/json");
  _{node.id}_con.setRequestProperty("Accept", "application/json");
  HttpURLConnection.setFollowRedirects(true);
  _{node.id}_con.setInstanceFollowRedirects(false);
  _{node.id}_con.setDoOutput(true);

  // Send data
  if (_{node.id}_in[3] != null) {
    OutputStream _{node.id}_out = _{node.id}_con.getOutputStream();
    _{node.id}_out.write(((JSONObject)_{node.id}_in[3]).toString().getBytes());
  }

  InputStream _{node.id}_ip = _{node.id}_con.getInputStream();
  BufferedReader _{node.id}_br1 = new BufferedReader(new InputStreamReader(_{node.id}_ip));

  out{1} = _{node.id}_con.getResponseCode();

  //System.out.println("Response Message:" + con.getResponseMessage());

  StringBuilder _{node.id}_response = new StringBuilder();
  String _{node.id}_responseSingle = null;

  while ((_{node.id}_responseSingle = _{node.id}_br1.readLine()) != null) {
    _{node.id}_response.append(_{node.id}_responseSingle);
  }

  out{2} = _{node.id}_response.toString();
  _{node.id}_restOK = true;

} catch (Exception e) {
    out{4} = e.getMessage();
}

if (_{node.id}_restOK) {
  exec{0}
} else {
  exec{3}
}
