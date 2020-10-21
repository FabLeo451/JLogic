out{1} = null;

try {
  byte[] _{node.id}_bytes = Files.readAllBytes(Paths.get(in{1}));
  
  out{2} = _{node.id}_bytes.length;
  out{1} = new Byte[out{2}];
  
  int _{node.id}_i=0; 
  for(byte _{node.id}_b: _{node.id}_bytes)
     out{1}[_{node.id}_i++] = _{node.id}_b;
}
catch (NoSuchFileException e) {
  out{4} = "File not found: "+e.getMessage();
} catch (IOException e) {
  out{4} = "IOException :"+e.getMessage();
}

if (out{1} != null) {
  exec{0}
} else {
  exec{3}
}
