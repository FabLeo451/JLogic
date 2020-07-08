
DocumentBuilderFactory _{node.id}_factory = DocumentBuilderFactory.newInstance();
boolean _{node.id}_xmlParseOK = false;

try {
  DocumentBuilder _{node.id}_builder = _{node.id}_factory.newDocumentBuilder();
  out{1} = _{node.id}_builder.parse(new InputSource(new StringReader(in{1})));
  _{node.id}_xmlParseOK = true;
} catch(Exception e) {
//e.printStackTrace();
}

if (_{node.id}_xmlParseOK) {
  exec{0}
} else {
  exec{2}
}
