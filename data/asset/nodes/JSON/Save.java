
out{2} = "OK";

try (FileWriter file = new FileWriter(in{1})) {
    file.write(in{2}.toString());
    file.flush();
    file.close();
} catch (IOException e) {
    out{2} = "IOException :"+e.getMessage();
}

if (out{2}.equals("OK")) {
  exec{0}
} else {
  exec{1}
}
