
// Create client

out{1} = new ElasticsearchClient((in{1} == 0 ? "http" : "https"), in{2}, in{3}, in{4}, in{5}, false, null);

// Ping

if (out{1}.ping()) { 
  exec{0} 
} else { 
  out{3} = out{1}.getMessage(); exec{2} 
}

