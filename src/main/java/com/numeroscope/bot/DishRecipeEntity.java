package com.numeroscope.bot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dish_recipe")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishRecipeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "unique_name", unique = true, nullable = false)
    private String uniqueName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recipe;

    @Column(nullable = false, columnDefinition = "JSONB")
    private String ingredients; // Stored as JSON string
}
