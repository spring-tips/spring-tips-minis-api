create table if not exists stb_spring_tip_bites
(
    id        serial primary key,
    scheduled timestamp not null unique ,
    uid      text      not null unique,
    tweet     text      not null,
    title     text      not null,
    code      text      not null
);

create table if not exists stb_users
(
    id       serial primary key,
    username varchar(255) not null,
    password text         not null
);