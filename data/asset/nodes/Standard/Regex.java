List<String> _{node.id}_allMatches = new ArrayList<String>();
Matcher _{node.id}_m = Pattern.compile(in{1}).matcher(in{2});
boolean _{node.id}_b = false; //_{node.id}_m.matches();

while (_{node.id}_m.find()) {
  _{node.id}_b = true;

  for (int _{node.id}_j = 0; _{node.id}_j <= _{node.id}_m.groupCount(); _{node.id}_j++) {
    //System.out.println(_{node.id}_m.group(_{node.id}_j));
    _{node.id}_allMatches.add(_{node.id}_m.group(_{node.id}_j));
  }
}

if (_{node.id}_b) {
  out{1} = _{node.id}_allMatches.toArray(new String[0]);
  exec{0}
}
else {
  exec{2}
}
