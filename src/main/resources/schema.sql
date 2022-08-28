create table stb_users
(
    id       serial primary key,
    username varchar(255) not null,
    password text         not null
);