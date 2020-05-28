
// SFTP Put

boolean _{node.id}_getOK = true;

try {
    if (in{3} != null)
      in{1}.get(in{2}, in{3});
    else
      in{1}.get(in{2});
    
    _{node.id}_getOK = true;
} catch (SftpException e) {
    out{3} = e.getMessage();
    _{node.id}_getOK = false;
}

if (_{node.id}_getOK) {
  exec{0}
} else {
  exec{2}
}

