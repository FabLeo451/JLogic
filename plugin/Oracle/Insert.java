
// Oracle Insert

out{4} = 0;
out{5} = "OK";

try {
    Iterator _{node.id}_iterator;
    int _{node.id}_i = 0;
    //int _{node.id}_n = in{3}.size();
    //String[] _{node.id}_headers = new String[_{node.id}_n];
    //Object[] _{node.id}_values = new String[_{node.id}_n];
    String _{node.id}_sql = "INSERT INTO "+in{2}+" (";
    
    // Headers
    
    for (_{node.id}_iterator = in{3}.keySet().iterator(); _{node.id}_iterator.hasNext();) {
        String _{node.id}_key = (String) _{node.id}_iterator.next();
        //System.out.println(_{node.id}_key + " " + in{3}.get(_{node.id}_key));
        //_{node.id}_headers[_{node.id}_i] = (String) _{node.id}_iterator.next();
        //_{node.id}_values[_{node.id}_i] = in{3}.get(_{node.id}_key);
        
        _{node.id}_sql += (_{node.id}_i > 0 ? ", " : "") + _{node.id}_key;
        
        _{node.id}_i ++;
    }
    
    // Values
    
    _{node.id}_sql += ") VALUES (";

    _{node.id}_i = 0;
    
    for (_{node.id}_iterator = in{3}.keySet().iterator(); _{node.id}_iterator.hasNext();) {
        String _{node.id}_key = (String) _{node.id}_iterator.next();
        Object _{node.id}_value = in{3}.get(_{node.id}_key);
        
        if (_{node.id}_value instanceof String) {
          String _{node.id}_s = (String) _{node.id}_value;
          //System.out.println(((String) _{node.id}_value).substring(0,3));
          
          if (_{node.id}_s.length() >= 4 && _{node.id}_s.substring(0,4).equalsIgnoreCase("ORA:"))
            _{node.id}_value = _{node.id}_s.substring(4);
          else
            _{node.id}_value = "'"+_{node.id}_s+"'"; 
        }
        
        _{node.id}_sql += (_{node.id}_i > 0 ? ", " : "") + _{node.id}_value;
        _{node.id}_i ++;
    }
    
    _{node.id}_sql += ")";
        
    //System.out.println(_{node.id}_sql);

    Statement _{node.id}_statement = in{1}.createStatement();

    out{2} = _{node.id}_statement.executeUpdate(_{node.id}_sql);

    if (in{4})
      in{1}.commit();

} catch (SQLException e) {
    out{4} = e.getErrorCode(); 
    out{5} = e.getMessage();
} /*catch (Exception e) {
    out{4} = 1;
    out{5} = e.getMessage();
}*/

if (out{4} == 0) {
  // Success
  exec{0} 
} else {
  // Error
  exec{3} 
}
