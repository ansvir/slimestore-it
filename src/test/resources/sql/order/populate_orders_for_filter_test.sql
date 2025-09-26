INSERT INTO products (id, name) VALUES (1, 'Galaxy Slime');
INSERT INTO products (id, name) VALUES (2, 'Cloud Slime');
INSERT INTO products (id, name) VALUES (3, 'Fluffy Slime');
INSERT INTO products (id, name) VALUES (4, 'Butter Slime');

INSERT INTO orders (id, customer_name) VALUES (1001, 'Alice');
INSERT INTO orders (id, customer_name) VALUES (1002, 'Bob');
INSERT INTO orders (id, customer_name) VALUES (1003, 'Charlie');
INSERT INTO orders (id, customer_name) VALUES (1004, 'Dave');

INSERT INTO order_products (order_id, product_id, quantity) VALUES (1001, 1, 1);
INSERT INTO order_products (order_id, product_id, quantity) VALUES (1002, 2, 2);
INSERT INTO order_products (order_id, product_id, quantity) VALUES (1003, 3, 3);
INSERT INTO order_products (order_id, product_id, quantity) VALUES (1004, 2, 1);