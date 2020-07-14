boolean _{node.id}_exeOK = false;

List<String> _{node.id}_args = new ArrayList<String>();

// Add command and arguments (from in{3} on...)
for (int _{node.id}_i = 3; _{node.id}_i < {count.in}; _{node.id}_i++) {
  _{node.id}_args.add((String) _{node.id}_in [_{node.id}_i]);
}

ProcessBuilder _{node.id}_processBuilder = new ProcessBuilder();

_{node.id}_processBuilder.command(_{node.id}_args);

if (!in{1}.isEmpty())
  _{node.id}_processBuilder.directory(new File(in{1}));

try {
  Process _{node.id}_process = _{node.id}_processBuilder.start();

  // stdin
  if (!in{2}.isEmpty()) {
    BufferedWriter _{node.id}_writer = new BufferedWriter(new OutputStreamWriter(_{node.id}_process.getOutputStream()));
    _{node.id}_writer.write(in{2});
    _{node.id}_writer.flush();
    _{node.id}_writer.close();
  }

  // stdout and stderr
  BufferedReader _{node.id}_outReader = new BufferedReader(new InputStreamReader(_{node.id}_process.getInputStream()));
  BufferedReader _{node.id}_errReader = new BufferedReader(new InputStreamReader(_{node.id}_process.getErrorStream()));

  StringBuilder _{node.id}_output = new StringBuilder();
  String _{node.id}_line;

  while ((_{node.id}_line = _{node.id}_outReader.readLine()) != null)
	  _{node.id}_output.append(_{node.id}_line + "\n");

  while ((_{node.id}_line = _{node.id}_errReader.readLine()) != null)
	  _{node.id}_output.append(_{node.id}_line + "\n");

  out{1} = _{node.id}_output.toString();
  out{3} = _{node.id}_process.waitFor();

  _{node.id}_exeOK = out{3} == 0 ? true : false;
}
catch (IOException e) {
  out{1} = e.getMessage();
  out{3} = 1;
  _{node.id}_exeOK = false;
}
catch (InterruptedException e) {
  out{1} = e.getMessage();
  out{3} = 1;
  _{node.id}_exeOK = false;
}

if (_{node.id}_exeOK) {
  exec{0}
} else {
  exec{2}
}
