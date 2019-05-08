create table if not exists users
(
    id       integer identity primary key,
    userName varchar(50),
    password varchar(50),
    landTime timestamp
);

create table if not exists localInfos
(
    id       integer identity primary key,
    serverIP varchar(20),
    localIP  varchar(20)
);