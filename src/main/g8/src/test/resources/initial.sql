drop table if exists ACCOUNT_BALANCE;
create table ACCOUNT_BALANCE
(
    ACCOUNT_I int not null,
    BALANCE_F float8,
    FROM_Z timestamp not null,
    THRU_Z timestamp not null,
    IN_Z timestamp not null,
    OUT_Z timestamp not null
);
alter table ACCOUNT_BALANCE add constraint ACCOUNT_BALANCE_PK primary key (ACCOUNT_I, THRU_Z, OUT_Z);

drop table if exists ALL_TYPES;
create table ALL_TYPES
(
    ID int not null,
    BOOL_COL boolean not null,
    BYTE_COL smallint not null,
    SHORT_COL int2 not null,
    CHAR_COL varchar(1) not null,
    INT_COL int not null,
    LONG_COL bigint not null,
    FLOAT_COL float4 not null,
    DOUBLE_COL float8 not null,
    DATE_COL date not null,
    TIMESTAMP_COL timestamp not null,
    STRING_COL varchar(50) not null,
    BYTE_ARRAY_COL bytea not null,
    NULL_BYTE_COL smallint,
    NULL_SHORT_COL int2,
    NULL_CHAR_COL varchar(1),
    NULL_INT_COL int,
    NULL_LONG_COL bigint,
    NULL_FLOAT_COL float4,
    NULL_DOUBLE_COL float8,
    NULL_DATE_COL date,
    NULL_TIMESTAMP_COL timestamp,
    NULL_STRING_COL varchar(50),
    NULL_BYTE_ARRAY_COL bytea
);
alter table ALL_TYPES add constraint ALL_TYPES_PK primary key (ID);

drop table if exists CUSTOMER;
create table CUSTOMER
(
    CUSTOMER_I int not null,
    NAME_C varchar(64) not null,
    COUNTRY_I varchar(48) not null
);
alter table CUSTOMER add constraint CUSTOMER_PK primary key (CUSTOMER_I);

drop table if exists CUSTOMER_ACCOUNT;
create table CUSTOMER_ACCOUNT
(
    ACCOUNT_I int not null,
    CUSTOMER_I int not null,
    ACCOUNT_NAME_C varchar(48) not null,
    ACCOUNT_TYPE_C varchar(16) not null
);
alter table CUSTOMER_ACCOUNT add constraint CUSTOMER_ACCOUNT_PK primary key (ACCOUNT_I);

drop table if exists EMPLOYEE;
create table EMPLOYEE
(
    EMPLOYEE_ID int not null,
    FIRST_NAME varchar(255) not null,
    LAST_NAME varchar(255) not null,
    AGE int not null,
    FROM_Z timestamp not null,
    THRU_Z timestamp not null,
    IN_Z timestamp not null,
    OUT_Z timestamp not null
);
alter table EMPLOYEE add constraint EMPLOYEE_PK primary key (EMPLOYEE_ID, THRU_Z, OUT_Z);

drop table if exists OBJECT_SEQUENCE;
create table OBJECT_SEQUENCE
(
    SEQUENCE_NAME varchar(64) not null,
    NEXT_VALUE bigint
);
alter table OBJECT_SEQUENCE add constraint OBJECT_SEQUENCE_PK primary key (SEQUENCE_NAME);

drop table if exists PERSON;
create table PERSON
(
    PERSON_ID int not null,
    NAME varchar(64) not null,
    COUNTRY varchar(48) not null,
    AGE int not null
);
alter table PERSON add constraint PERSON_PK primary key (PERSON_ID);

drop table if exists TASK;
create table TASK
(
    TASK_ID int not null,
    NAME varchar(64) not null,
    STATUS varchar(32) not null,
    IN_Z timestamp not null,
    OUT_Z timestamp not null
);

alter table TASK add constraint TASK_PK primary key (TASK_ID, OUT_Z);
