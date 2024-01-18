-- Add all your SQL setup statements here.
-- When we test your submission, you can assume that the following base
-- tables have been created and loaded with data.  However, before testing
-- your own code, you will need to create and populate them on your
-- SQLServer instance
--
-- Do not alter the following tables' contents or schema in your code.
-- CREATE TABLE FLIGHTS(fid int primary key,
--         month_id int,        -- 1-12
--         day_of_month int,    -- 1-31
--         day_of_week_id int,  -- 1-7, 1 = Monday, 2 = Tuesday, etc
--         carrier_id varchar(7),
--         flight_num int,
--         origin_city varchar(34),
--         origin_state varchar(47),
--         dest_city varchar(34),
--         dest_state varchar(46),
--         departure_delay int, -- in mins
--         taxi_out int,        -- in mins
--         arrival_delay int,   -- in mins
--         canceled int,        -- 1 means canceled
--         actual_time int,     -- in mins
--         distance int,        -- in miles
--         capacity int,
--         price int            -- in $
--         )
-- CREATE TABLE CARRIERS(
--   cid varchar(7) primary key,
--   name varchar(83)
--   );
-- CREATE TABLE MONTHS (
--   mid int primary key,
--   month varchar(9)
--   );
-- CREATE TABLE WEEKDAYS(
--   did int primary key,
--   day_of_week varchar(9)
--   );
CREATE TABLE Users_lshibly (
  Username VARCHAR(255) PRIMARY KEY,
  Password VARBINARY(255),
  LoggedIn int,
  Balance REAL
);

CREATE TABLE Reservations_lshibly (
  ReservationID int PRIMARY KEY,
  Username VARCHAR(255) REFERENCES Users_lshibly(Username),
  fid1 int REFERENCES FLIGHTS(fid),
  fid2 int NULL REFERENCES FLIGHTS(fid),
  status int -- 'Paid' or 'Unpaid' 0 is unpaid and 1 is paid
);