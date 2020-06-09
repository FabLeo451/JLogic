
boolean _{node.id}_bulk_success = false;

if (in{1}.bulkInsert(in{2}, in{3})) {

  _{node.id}_bulk_success = true;
  
  BulkResponse bulkResponse = in{1}.getBulkResponse();

  out{1} = 0; // Created
  out{2} = 0; // Updated
  out{3} = 0; // Errors
  out{4} = bulkResponse.hasFailures();
  
  for (BulkItemResponse bulkItemResponse : bulkResponse) { 
      DocWriteResponse itemResponse = bulkItemResponse.getResponse(); 

      switch (bulkItemResponse.getOpType()) {
        case INDEX:    
        case CREATE:
            //System.out.println("CREATE");
            IndexResponse indexResponse = (IndexResponse) itemResponse;
            
            if (indexResponse.status() == RestStatus.CREATED)
              out{1} ++;
            else if (indexResponse.status() == RestStatus.OK)
              out{2} ++;
            else
              out{3} ++;
            break;
        /*   
        case UPDATE:
            System.out.println("UPDATE");
            UpdateResponse updateResponse = (UpdateResponse) itemResponse;
            
            if (updateResponse.status() == RestStatus.OK)
              nUpd ++;
            else
              nErr ++;
            break;
        
        case DELETE:   
            DeleteResponse deleteResponse = (DeleteResponse) itemResponse;
        }*/
        default:
          break;
    }    
  }
}

if (_{node.id}_bulk_success) {
  exec{0}
} else { 
  out{6} = in{1}.getMessage();
  exec{5} 
}
