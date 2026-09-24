DROP TABLE IF EXISTS menu_items;

CREATE TABLE menu_items (
                            id BIGINT PRIMARY KEY,
                            restaurant_id BIGINT NOT NULL,
                            dish_name VARCHAR(255) NOT NULL,
                            price BIGINT NOT NULL
);