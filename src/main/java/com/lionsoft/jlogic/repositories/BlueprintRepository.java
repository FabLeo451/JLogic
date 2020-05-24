package com.lionsoft.jlogic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
 
@Repository
public interface BlueprintRepository extends JpaRepository<BlueprintEntity, Long> {
  Optional<BlueprintEntity> findById(String id);
  Optional<BlueprintEntity> findByName(String name);
}
