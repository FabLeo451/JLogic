out{1} = null;

try {
  out{1} = new String (Files.readAllBytes(Paths.get(in{1})));
}
catch (NoSuchFileException e) {
  out{3} = "File not found: "+e.getMessage();
} catch (IOException e) {
  out{3} = "IOException :"+e.getMessage();
}

if (out{1} != null) {
  exec{0}
} else {
  exec{2}
}
