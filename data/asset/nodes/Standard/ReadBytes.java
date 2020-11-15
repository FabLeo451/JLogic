if (in{3})
  _47a1aad0_bytesReadOffset = 0; 
  
out{2} = 0;

if (_47a1aad0_bytesReadOffset < in{1}.length-1) {
  int _{node.id}_l = _47a1aad0_bytesReadOffset + in{2};
  
  if (_47a1aad0_bytesReadOffset + _{node.id}_l > in{1}.length)
    _{node.id}_l = in{1}.length - _47a1aad0_bytesReadOffset;
  
  out{1} = Arrays.copyOfRange(in{1}, _47a1aad0_bytesReadOffset, _47a1aad0_bytesReadOffset + _{node.id}_l); 
  out{2} = _{node.id}_l;

  // Update offset
  _47a1aad0_bytesReadOffset += _{node.id}_l;
}
