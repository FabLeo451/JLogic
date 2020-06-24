boolean _{node.id}_exeOK = false;

List<String> _{node.id}_args = new ArrayList<String>();
_{node.id}_args.add(in{2});

ProcessBuilder _{node.id}_processBuilder = new ProcessBuilder();

_{node.id}_processBuilder.command(_{node.id}_args);

if (!in{1}.isEmpty())
  _{node.id}_processBuilder.directory(new File(in{1}));

try {
  Process _{node.id}_process = _{node.id}_processBuilder.start();

  StringBuilder _{node.id}_output = new StringBuilder();

  BufferedReader _{node.id}_outReader = new BufferedReader(new InputStreamReader(_{node.id}_process.getInputStream()));
  BufferedReader _{node.id}_errReader = new BufferedReader(new InputStreamReader(_{node.id}_process.getErrorStream()));

  String _{node.id}_line;

  while ((_{node.id}_line = _{node.id}_outReader.readLine()) != null)
	  _{node.id}_output.append(_{node.id}_line + "\n");

  while ((_{node.id}_line = _{node.id}_errReader.readLine()) != null)
	  _{node.id}_output.append(_{node.id}_line + "\n");

  out{1} = _{node.id}_output.toString();
  out{3} = _{node.id}_process.waitFor();

  _{node.id}_exeOK = out{3} == 0 : true : false;
}
catch (IOException e) {
  out{1} = e.getMessage();
  _{node.id}_exeOK = false;
}
catch (InterruptedException e) {
  out{1} = e.getMessage();
  _{node.id}_exeOK = false;
}

if (_{node.id}_exeOK) {
  exec{0}
} else {
  exec{2}
}
