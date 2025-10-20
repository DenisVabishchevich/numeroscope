--liquibase formatted sql

--changeset DenisV:002-insert-dish-recipe-test-data
INSERT INTO dish_recipe (description, image_url, price, unique_name, recipe, ingredients)
VALUES
('Delicious Pasta', 'https://numero-bot-images.eu-central-1.linodeobjects.com/pngtree-funny-smile-icon-image-png-image_14976892.png', 1500, 'pasta-carbonara', 'Boil pasta, cook bacon, mix with egg and cheese.', '{"pasta": "200g", "bacon": "100g", "eggs": "2", "parmesan": "50g"}'),
('Classic Pizza', 'https://numero-bot-images.eu-central-1.linodeobjects.com/pngtree-funny-smile-icon-image-png-image_14976892.png', 1200, 'margherita-pizza', 'Make dough, add sauce, cheese, and basil, then bake.', '{"dough": "1", "tomato_sauce": "100g", "mozzarella": "150g", "basil": "fresh"}');
