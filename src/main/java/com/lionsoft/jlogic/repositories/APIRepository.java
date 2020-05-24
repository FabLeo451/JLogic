package com.lionsoft.jlogic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
 
@Repository
public interface APIRepository extends JpaRepository<APIEntity, String> {
  List<APIEntity> findAll();
  Optional<APIEntity> findById(String id);
  Optional<APIEntity> findByName(String name);
}
