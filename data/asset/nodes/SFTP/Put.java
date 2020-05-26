
// SFTP Put

boolean _{node.id}_putOK = true;

try {
    in{1}.put(in{2}, in{3});
    _{node.id}_putOK = true;
} catch (SftpException e) {
    out{3} = e.getMessage();
    _{node.id}_putOK = false;
}

if (_{node.id}_putOK) {
  exec{0}
} else {
  exec{2}
}

