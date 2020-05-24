package com.lionsoft.jlogic;

import java.util.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);
  Optional<User> findById(Long id);
  List<User> findAll();
  void deleteById(Long id);
/*    
  @Query("SELECT username FROM User")
  List<User> getUsers();*/
}

