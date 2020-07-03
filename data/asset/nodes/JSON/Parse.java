
// JSON.Parse

JSONParser _{node.id}_jsonParser = new JSONParser();
out{1} = null;
out{2} = null;
out{3} = false;
boolean _{node.id}_parse_success = false;

try {
  Object _{node.id}_obj = _{node.id}_jsonParser.parse(in{1});
  
  if (_{node.id}_obj instanceof JSONObject)
    out{1} = (JSONObject) _{node.id}_obj;
  else {
    out{2} = (JSONArray) _{node.id}_obj;
    out{3} = true;
  }

  _{node.id}_parse_success = true;

} catch (ParseException e) {
  _{node.id}_parse_success = false;
}

if (_{node.id}_parse_success) {
  exec{0}
} else {
  exec{4}
}
