
out{4} = 0;
out{5} = "OK";

try {
    Iterator _{node.id}_iterator;
    int _{node.id}_set = 0;
    int _{node.id}_where = 0;
    //int _{node.id}_n = in{3}.size();
    //String[] _{node.id}_headers = new String[_{node.id}_n];
    //Object[] _{node.id}_values = new String[_{node.id}_n];
    String _{node.id}_where_cond = "";
    String _{node.id}_sql = "UPDATE "+in{2}+" SET ";
    
    // Headers
    
    for (_{node.id}_iterator = in{3}.keySet().iterator(); _{node.id}_iterator.hasNext();) {
        String _{node.id}_key = (String) _{node.id}_iterator.next();
        Object _{node.id}_value = in{3}.get(_{node.id}_key);

        if (_{node.id}_value instanceof String) {
          String _{node.id}_s = (String) _{node.id}_value;

          if (_{node.id}_s.length() >= 4 && _{node.id}_s.substring(0,4).equalsIgnoreCase("ORA:"))
            _{node.id}_value = _{node.id}_s.substring(4);
          else
            _{node.id}_value = "'"+_{node.id}_s+"'"; 
        }
        
        // Check if current field is part of the key
        
        boolean _{node.id}_isKey = false;
        
        for (int _{node.id}_i = 5; _{node.id}_i < {count.in}; _{node.id}_i++) {
          if (_{node.id}_key.equalsIgnoreCase((String) _{node.id}_in [_{node.id}_i] )) {
            // Yes, it's part of the key
          
            if (_{node.id}_where > 0)
              _{node.id}_where_cond += " AND ";
              
            _{node.id}_where_cond += _{node.id}_key + " = " + _{node.id}_value;
            
            _{node.id}_where ++;
            _{node.id}_isKey = true;
          }
        }
        
        if (!_{node.id}_isKey) {
          // Not key
          
          if (_{node.id}_set > 0)
            _{node.id}_sql += ", ";
            
          _{node.id}_sql += _{node.id}_key + " = " + _{node.id}_value;
         
          _{node.id}_set ++;
        }
    }
    
    _{node.id}_sql += " WHERE " + _{node.id}_where_cond;

        
    System.out.println(_{node.id}_sql);

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
