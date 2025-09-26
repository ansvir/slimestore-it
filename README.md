## SlimeStore Integration Testing Showcase

SlimeStore is an online store that sells.. slimes.

Simple implementation of order management and showcase of integration tests using h2 and kafka as test container.

### What is architecture of project?
#### Database
This is monolith with one primary entity - Orders, and Product entity that is related to Orders as Many-To-One and Outbox table that is subject of transactional outbox pattern.
The schema of DB is next:
