
// Fetch as JSON

Object _{node.id}_val = null;

//out{1} = in{1};

try {
  ResultSetMetaData _{node.id}_rsmd = in{1}.getMetaData();
  String _{node.id}_key;
  int _{node.id}_type;
  
  int _{node.id}_nCols = _{node.id}_rsmd.getColumnCount();
  //System.out.println("_{node.id}_nCols: "+_{node.id}_nCols); 
  
  while (in{1}.next()) {
    //System.out.println("Fetch iteration...");
  
    out{1} = new JSONObject();

    for (int _{node.id}_i=1; _{node.id}_i <= _{node.id}_nCols; _{node.id}_i++) {
      _{node.id}_key = _{node.id}_rsmd.getColumnName(_{node.id}_i);
      _{node.id}_type = _{node.id}_rsmd.getColumnType(_{node.id}_i);
      //System.out.println("key: "+_{node.id}_key); 
      
      switch (_{node.id}_type) {
        case Types.BIGINT:
        case Types.BOOLEAN:
        case Types.CHAR:
        case Types.DECIMAL:
        case Types.DOUBLE:
        case Types.FLOAT:
        case Types.INTEGER:
        case Types.LONGNVARCHAR:
        case Types.LONGVARCHAR:
        case Types.NCHAR:
        case Types.NULL:
        case Types.NUMERIC:
        case Types.NVARCHAR:
        case Types.REAL:
        case Types.SMALLINT:
        case Types.TINYINT:
        case Types.VARCHAR:
          _{node.id}_val = in{1}.getObject(_{node.id}_i);
          break;

        case Types.CLOB:
          Clob clob = in{1}.getClob(_{node.id}_i);
          
          if (clob != null)
            // TODO: https://www.tutorialspoint.com/how-to-convert-a-clob-type-to-string-in-java
            _{node.id}_val = (String) clob.getSubString(1, (int) clob.length());
          else
            _{node.id}_val = null;
          break;
          
          
        default:
          _{node.id}_val = in{1}.getString(_{node.id}_i);
          break;
      }
      
      out{1}.put(_{node.id}_key, _{node.id}_val);
      
      //System.out.println(_{node.id}_key+" = "+_{node.id}_val); 
    }
    
    exec{0}
  }
} catch (SQLException e) {
  logger.error(e.getMessage());
}

// Fetch as JSON completed

exec{2}

