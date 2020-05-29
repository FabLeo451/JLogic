package com.lionsoft.jlogic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
 
@Repository
public interface ProgramRepository extends CustomRepository<ProgramEntity, Long> {
  List<ProgramEntity> findAll();
  Optional<ProgramEntity> findById(String id);
}
