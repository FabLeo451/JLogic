
// Oracle Connect

String __user__ = in{5}; 
out{3} = 0;
out{4} = "OK";

switch (in{1}) { 
  case 1: 
    __user__ += " as sysoper"; 
    break;
    
  case 2: 
    __user__ += " as sysdba"; 
    break;
    
  default:
    break; 
} 

try { 
    Class.forName("oracle.jdbc.OracleDriver");
    out{1} = DriverManager.getConnection("jdbc:oracle:thin:@"+in{2}+":"+in{3}+":"+in{4}, __user__, in{6});
    out{1}.setAutoCommit(false);
} catch (SQLException e) { 
    out{3} = e.getErrorCode(); 
    out{4} = e.getMessage(); 
} catch (ClassNotFoundException e) { 
    out{3} = 1; 
    out{4} = e.getMessage(); 
} 

if (out{3} == 0) { 
  exec{0} 
} else { 
  exec{2} 
}
