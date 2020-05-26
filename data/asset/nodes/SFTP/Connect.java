
// SFTP Connect

out{1} = null;

try {
    JSch _{node.id}_jsch = new JSch();
    //_{node.id}_jsch.setKnownHosts("/Users/john/.ssh/known_hosts");
    Session _{node.id}_jschSession = _{node.id}_jsch.getSession(in{3}, in{1});
    _{node.id}_jschSession.setConfig("StrictHostKeyChecking", "no");
    
    if (in{4} == 0)
      _{node.id}_jschSession.setPassword(in{5});
    /*else
      _{node.id}_jschSession.addIdentity(in{6});*/
    
    _{node.id}_jschSession.connect();
    out{1} = (ChannelSftp) _{node.id}_jschSession.openChannel("sftp");
    
    out{1}.connect();
} catch (JSchException e) {
  out{3} = e.getMessage();
  out{1} = null;
}

if (out{1} != null) {
  exec{0}
} else {
  exec{2}
}

