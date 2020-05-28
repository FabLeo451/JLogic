
boolean _{node.id}_ls_result = true;
Vector _{node.id}_ls_vec = null;

try {
    if (in{2} != null)
      _{node.id}_ls_vec = in{1}.ls(".");
    else
      _{node.id}_ls_vec = in{1}.ls(in{2});
      
    out{2} = new ChannelSftp.LsEntry[_{node.id}_ls_vec.size()];
    _{node.id}_ls_vec.toArray(out{2});
    
    _{node.id}_ls_result = true;
} catch (SftpException e) {
    out{4} = e.getMessage();
    _{node.id}_ls_result = false;
} catch (ArrayStoreException e) {
    out{4} = e.getMessage();
    _{node.id}_ls_result = false;
} catch (NullPointerException e) {
    out{4} = e.getMessage();
    _{node.id}_ls_result = false;
}

if (_{node.id}_ls_result) {
  exec{0}
} else {
  exec{3}
}

