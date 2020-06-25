
JSONParser _{node.id}_jsonParser = new JSONParser();
out{3} = "OK";

try {
  String _{node.id}_text = new String (Files.readAllBytes(Paths.get(in{1})));
  out{1} = (JSONObject) _{node.id}_jsonParser.parse(_{node.id}_text);
} catch (IOException e) {
  out{3} = e.toString();
} catch (ParseException e) {
  out{3} = e.getMessage();
}

if (out{3}.equals("OK")) {
  exec{0}
} else {
  exec{2}
}
