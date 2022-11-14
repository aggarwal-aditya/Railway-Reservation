create table trains( train_id integer primary key, name varchar(256) not null);
create table available_trains(train_id integer, date date, num_AC integer not null, num_SL integer not null, primary key(train_id, date), foreign key(train_id) references trains(train_id));
create table coach(coach_type char(2), berth_num integer, berth_type char(2), primary key(coach_type, berth_num));
--create all ticket log with pnr number date train id


--Release Train
CREATE OR REPLACE FUNCTION releaseTrain(trainID integer,day date,count_AC integer,count_SL integer)
RETURNS integer
AS
$$
BEGIN
	INSERT INTO available_trains VALUES(trainID,day,count_AC,count_SL);
	return 1;
END;
$$
language plpgsql;


-- Get Train Composition
CREATE OR REPLACE FUNCTION getSeatMatrix(trainID integer, day date, coachType char(2))
RETURNS INTEGER
AS 
$$
DECLARE
num integer;
i integer;
j integer;
BEGIN
	if coachType = 'AC' then
		SELECT INTO num
		num_AC FROM available_trains
		WHERE train_id = trainID AND date = day;
		RETURN (num*18);
	else
		SELECT INTO num
		num_SL FROM available_trains
		WHERE train_id = trainID AND date = day;
		RETURN (num*24);
	end if;
END;
$$
language plpgsql;


-- Trigger to create booking and empty seats table on release of a new train. 
CREATE OR REPLACE FUNCTION _on_train_release()
RETURNS TRIGGER
AS
$$
DECLARE 
seat bigint;
BEGIN
	EXECUTE format('CREATE TABLE %I (pnr varchar(64), passenger_names varchar(256) array, passenger_coach varchar(8) array, passenger_berth integer array);', 'bookings_ac_' || NEW.train_id::text || '_' || to_char(NEW.date, 'yyyy_mm_dd'));
	EXECUTE format('CREATE TABLE %I (pnr varchar(64), passenger_names varchar(256) array, passenger_coach varchar(8) array, passenger_berth integer array);', 'bookings_sl_' || NEW.train_id::text || '_' || to_char(NEW.date, 'yyyy_mm_dd'));
	EXECUTE format('CREATE TABLE %I (total_available bigint, total_filled bigint);', 'filled_ac_seats_' || NEW.train_id::text || '_' || to_char(NEW.date, 'yyyy_mm_dd'));
	EXECUTE format('CREATE TABLE %I (total_available bigint, total_filled bigint);', 'filled_sl_seats_' || NEW.train_id::text || '_' || to_char(NEW.date, 'yyyy_mm_dd'));
	execute format('select * from getSeatMatrix(%L, %L, %L);', NEW.train_id, NEW.date, 'AC') into seat;
	execute format('INSERT INTO %I VALUES(%L, %L)',  'filled_ac_seats_' || NEW.train_id::text || '_' || to_char(NEW.date, 'yyyy_mm_dd'), seat, 0);
	execute format('select * from getSeatMatrix(%L, %L, %L);', NEW.train_id, NEW.date, 'SL') into seat;
	execute format('INSERT INTO %I VALUES(%L, %L)',  'filled_sl_seats_' || NEW.train_id::text || '_' || to_char(NEW.date, 'yyyy_mm_dd'), seat,0);
return new;

END;
$$
language plpgsql;

CREATE TRIGGER on_train_release
AFTER INSERT
ON available_trains
FOR EACH ROW
EXECUTE PROCEDURE _on_train_release();



CREATE OR REPLACE FUNCTION bookTicket(
	trainID integer,
	day date,
	num_seats integer,
	ct char(2),
	passenger_name varchar(256)[]	
)
RETURNS text
AS 
$$
declare 
count_filled bigint;
count_available bigint;
curr_coach integer;
curr_berth integer;
coach_seats integer;
empty_seats integer;
passenger_coach varchar(8)[];
passenger_berth integer[];
type_coach integer;
type_berth varchar(2)[]= ARRAY ['LB','MB','UB','LB','MB','UB','SL','SU','LB','MB','UB','LB','MB','UB','SL','SU','LB','MB','UB','LB','MB','UB','SL','SU','LB','LB','UB','UB', 'SL', 'SU', 'LB', 'LB', 'UB', 'UB', 'SL', 'SU','LB','LB','UB','UB','SL','SU'];
pnr VARCHAR(64);
booking_info TEXT;
i integer=1;
j integer=1;
BEGIN 
	-- check bookings_train_no_date(yyyy_mm_dd)
	EXECUTE FORMAT('select total_filled from %I','filled_'||lower(ct)||'_seats_'||trainID::text||'_'||to_char(day,'yyyy_mm_dd')) into count_filled;
	EXECUTE FORMAT('select total_available from %I','filled_'||lower(ct)||'_seats_'||trainID::text||'_'||to_char(day,'yyyy_mm_dd')) into count_available;
	if count_filled+num_seats>count_available then
	raise exception '% seats not available', num_seats;
	else
		if ct = 'AC' then
			coach_seats=18;
			type_coach=1;
		else
			coach_seats=24;
			type_coach=0;
		end if;
		curr_coach=count_filled/coach_seats;
		if (count_filled%coach_seats)<>0 then
		curr_coach=curr_coach+1;
		curr_berth=count_filled%coach_seats;
		curr_berth=curr_berth+1;
		else
		curr_coach=curr_coach+1;
		curr_berth=1;
		end if;
		-- raise notice 'Value: % %', curr_coach,curr_berth;
		-- ticket stores name of passenger coach,berth
		-- I have bokking table for each train, each booking has PNR array of passenger names, array of coach numbers,array of berths
		EXECUTE FORMAT ('UPDATE %I c SET total_filled=total_filled+%L','filled_'||lower(ct)||'_seats_'||trainID::text||'_'||to_char(day,'yyyy_mm_dd'),num_seats);
		pnr=generateUniquePNR(trainID,day,ct,curr_coach,curr_berth);
		booking_info=pnr||'|';
		for empty_seats in 1..num_seats
	loop
		passenger_coach[i]=ct||curr_coach::text;
		passenger_berth[i]=curr_berth;
		booking_info=booking_info||curr_coach||'|'||curr_berth::text||'|'||type_berth[(type_coach*24)+curr_berth]||'|';
		i=i+1;
		curr_berth=curr_berth+1;
		if curr_berth>coach_seats then
			curr_coach=curr_coach+1;
			curr_berth=1;
			end if;
	end loop;
	EXECUTE FORMAT ('INSERT INTO %I VALUES(%L, %L, %L, %L);', 'bookings_'||lower(ct)||'_'||trainID||'_'|| to_char(day, 'yyyy_mm_dd'), PNR,passenger_name,passenger_coach,passenger_berth);
	end if;
	RETURN booking_info;
END;
$$
language plpgsql;



CREATE OR REPLACE FUNCTION generateUniquePNR(trainID integer, day date, coach_type char(2), coach integer, berth integer)
RETURNS TEXT
AS 
$$
declare PNR_string VARCHAR(256);
type_coach integer;
BEGIN
	if coach_type='AC' then
	type_coach=1;
	else
	type_coach=0;
	end if;
	PNR_string=trainID::text||'_'||to_char(day,'yyyymmdd')||'_'||type_coach||'_'||coach::text||'_'||berth::text;
	return PNR_string;
END;
$$
language plpgsql;

CREATE OR REPLACE FUNCTION getTicket(pnr VARCHAR)
RETURNS TABLE(name varchar(256), trainID integer, journey_date date, coach char(8),berth integer, berth_type char(2))
AS
$$
BEGIN
return query EXECUTE FORMAT ('SELECT c.name,c.trainid,c.journey_date,c.coach,c.berth,d.berth_type FROM %I c JOIN coach d on  ((substr(c.coach,1,2)||c.berth)=(d.coach_type||berth_num))','ticket_'||pnr) ;
END;
$$
language plpgsql;





-- Inserts in coach table
INSERT INTO coach VALUES('AC', 1, 'LB');
INSERT INTO coach VALUES('AC', 2, 'LB');
INSERT INTO coach VALUES('AC', 3, 'UB');
INSERT INTO coach VALUES('AC', 4, 'UB');
INSERT INTO coach VALUES('AC', 5, 'SL');
INSERT INTO coach VALUES('AC', 6, 'SU');
INSERT INTO coach VALUES('AC', 7, 'LB');
INSERT INTO coach VALUES('AC', 8, 'LB');
INSERT INTO coach VALUES('AC', 9, 'UB');
INSERT INTO coach VALUES('AC', 10, 'UB');
INSERT INTO coach VALUES('AC', 11, 'SL');
INSERT INTO coach VALUES('AC', 12, 'SU');
INSERT INTO coach VALUES('AC', 13, 'LB');
INSERT INTO coach VALUES('AC', 14, 'LB');
INSERT INTO coach VALUES('AC', 15, 'UB');
INSERT INTO coach VALUES('AC', 16, 'UB');
INSERT INTO coach VALUES('AC', 17, 'SL');
INSERT INTO coach VALUES('AC', 18, 'SU');
INSERT INTO coach VALUES('SL', 1, 'LB');
INSERT INTO coach VALUES('SL', 2, 'MB');
INSERT INTO coach VALUES('SL', 3, 'UB');
INSERT INTO coach VALUES('SL', 4, 'LB');
INSERT INTO coach VALUES('SL', 5, 'MB');
INSERT INTO coach VALUES('SL', 6, 'UB');
INSERT INTO coach VALUES('SL', 7, 'SL');
INSERT INTO coach VALUES('SL', 8, 'SU');
INSERT INTO coach VALUES('SL', 9, 'LB');
INSERT INTO coach VALUES('SL', 10, 'MB');
INSERT INTO coach VALUES('SL', 11, 'UB');
INSERT INTO coach VALUES('SL', 12, 'LB');
INSERT INTO coach VALUES('SL', 13, 'MB');
INSERT INTO coach VALUES('SL', 14, 'UB');
INSERT INTO coach VALUES('SL', 15, 'SL');
INSERT INTO coach VALUES('SL', 16, 'SU');
INSERT INTO coach VALUES('SL', 17, 'LB');
INSERT INTO coach VALUES('SL', 18, 'MB');
INSERT INTO coach VALUES('SL', 19, 'UB');
INSERT INTO coach VALUES('SL', 20, 'LB');
INSERT INTO coach VALUES('SL', 21, 'MB');
INSERT INTO coach VALUES('SL', 22, 'UB');
INSERT INTO coach VALUES('SL', 23, 'SL');
INSERT INTO coach VALUES('SL', 24, 'SU');