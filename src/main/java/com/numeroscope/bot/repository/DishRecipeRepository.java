package com.numeroscope.bot.repository;

import com.numeroscope.bot.model.DishRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DishRecipeRepository extends JpaRepository<DishRecipe, Long> {
    Optional<DishRecipe> findByUniqueName(String uniqueName);

    @Query("select dr.uniqueName from DishRecipe dr")
    List<String> findAllUniqueNames();
}
