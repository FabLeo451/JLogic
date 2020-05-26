
// JSON.Parse

JSONParser _{node.id}_jsonParser = new JSONParser();
out{1} = null;

try {
  out{1} = (JSONObject) _{node.id}_jsonParser.parse(in{1});
} catch (ParseException e) {
  exec{2}
}

exec{0}

