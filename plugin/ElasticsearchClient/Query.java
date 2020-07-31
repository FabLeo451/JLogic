

switch (in{3}) {
  case 1:
    out{1} = in{1}.queryToJSON(QueryBuilders.termQuery(in{4}, in{5}), in{2});
    break;

  case 2:
    out{1} = in{1}.queryToJSON(QueryBuilders.matchQuery(in{4}, in{5}), in{2});
    break;

  default:
    out{1} = in{1}.queryToJSON(QueryBuilders.matchAllQuery(), in{2});
    break;
}

if (out{1} != null) {
  exec{0}
} else {
  out{3} = in{1}.getMessage();
  exec{2}
}
