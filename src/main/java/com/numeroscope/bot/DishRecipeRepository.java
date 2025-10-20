package com.numeroscope.bot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DishRecipeRepository extends JpaRepository<DishRecipeEntity, Long> {
    Optional<DishRecipeEntity> findByUniqueName(String uniqueName);

    @Query("select dr.uniqueName from DishRecipeEntity dr")
    List<String> findAllUniqueNames();
}
