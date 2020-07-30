
out{1} = in{1}.queryToJSON(QueryBuilders.matchAllQuery(), in{2});

if (out{1} != null) {
  exec{0}
} else {
  out{3} = in{1}.getMessage();
  exec{2}
}
