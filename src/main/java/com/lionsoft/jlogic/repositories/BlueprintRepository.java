package com.lionsoft.jlogic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
 
@Repository
public interface BlueprintRepository extends JpaRepository<BlueprintEntity, Long> {
  Optional<BlueprintEntity> findById(String id);
  Optional<BlueprintEntity> findByName(String name);
  
  @Query("SELECT t FROM BlueprintEntity t WHERE t.name = ?1 AND t.program = ?2")
  Optional<BlueprintEntity> findByNameAndProgram(String name, ProgramEntity program);
}
