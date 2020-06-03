
// Oracle Select

out{3} = 0;
out{4} = "OK";

try {
  String _{node.id}_query = in{2};
  //System.out.println("Query : "+_{node.id}_query); 

  _{node.id}_query = _{node.id}_query.replace("\\n", "\n");
  //System.out.println("Query : "+_{node.id}_query); 
  
  PreparedStatement ps = in{1}.prepareStatement(_{node.id}_query);
  
  out{1} = ps.executeQuery(); 
  //ResultSetMetaData _{node.id}_rsmd = out{1}.getMetaData(); 

  //out{4} = in{1}; // Connection
  
} catch (SQLException e) { 
  out{3} = e.getErrorCode(); 
  out{4} = e.getMessage();
  //System.out.println(out{4}); 
}

//System.out.println("code = "+out{3}); 

if (out{3} == 0) {
  // Select successful
  exec{0} 
} else {
  // Select error
  exec{2} 
}
