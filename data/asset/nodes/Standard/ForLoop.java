
int _start_{node.id} = in{1};
int _end_{node.id} = in{2};
int _delta_{node.id} = in{1} <= in{2} ? 1 : -1;

for (out{1} = _start_{node.id}; (_start_{node.id} <= _end_{node.id} ? out{1} <= _end_{node.id} : out{1} >= _end_{node.id}) && !in{3}; out{1} += _delta_{node.id}) {
  exec{0}
}

exec{2}

