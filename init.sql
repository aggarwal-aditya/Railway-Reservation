create table trains( train_id integer primary key, name varchar(256) not null);
create table available_trains(train_id integer, date date, num_AC integer not null, num_SL integer not null, primary key(train_id, date), foreign key(train_id) references trains(train_id));
create table coach(coach_type char(2), berth_num integer, berth_type char(2), primary key(coach_type, berth_num));
-- Probably check for hash collisions (Maybe)


--Release Train
CREATE OR REPLACE FUNCTION releaseTrain(trainID integer,day date,count_AC integer,count_SL integer)
RETURNS NULL
AS
$$
BEGIN
	INSERT INTO available_trains VALUES(trainID,day,count_AC,count_SL);
END;
$$
language plpgsql;


-- Get Train Composition
CREATE OR REPLACE FUNCTION getSeatMatrix(trainID integer, day date, coachType char(2))
RETURNS table(coach_num integer, berth integer)
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
	else
		SELECT INTO num
		num_SL FROM available_trains
		WHERE train_id = trainID AND date = day;
	end if;
	for i in 1..num loop
		for j in SELECT c.berth_num FROM coach c WHERE coach_type = coachType loop
			coach_num :=i;
			berth := j;
			return next;
		end loop;
	end loop;
END;
$$
language plpgsql;


-- Trigger to create booking and empty seats table on release of a new train. 
CREATE OR REPLACE FUNCTION _on_train_release()
RETURNS TRIGGER
AS
$$
DECLARE 
seat record;
BEGIN
	EXECUTE format('CREATE TABLE %I (pnr bigint primary key);', 'bookings_' || NEW.train_id::text || '_' || to_char(NEW.date, 'yyyy_mm_dd'));
	EXECUTE format('CREATE TABLE %I (coach_num integer, berth integer, primary key(coach_num, berth));', 'empty_ac_seats_' || NEW.train_id::text || '_' || to_char(NEW.date, 'yyyy_mm_dd'));
	EXECUTE format('CREATE TABLE %I (coach_num integer, berth integer, primary key(coach_num, berth));', 'empty_sl_seats_' || NEW.train_id::text || '_' || to_char(NEW.date, 'yyyy_mm_dd'));
	for seat in execute format('select * from getSeatMatrix(%L, %L, %L);', NEW.train_id, NEW.date, 'AC') loop
		execute format('INSERT INTO %I VALUES(%L, %L)',  'empty_ac_seats_' || NEW.train_id::text || '_' || to_char(NEW.date, 'yyyy_mm_dd'), seat.coach_num, seat.berth);
	end loop;
	for seat in execute format('select * from getSeatMatrix(%L, %L, %L);', NEW.train_id, NEW.date, 'SL') loop
		execute format('INSERT INTO %I VALUES(%L, %L)',  'empty_sl_seats_' || NEW.train_id::text || '_' || to_char(NEW.date, 'yyyy_mm_dd'), seat.coach_num, seat.berth);
	end loop;
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
	passenger_name varchar(256)[],
	passenger_age integer[],
	passenger_gender char(1)[]
	
)
RETURNS text
AS 
$$
declare 
count_empty integer;
empty_seats record;
pnr bigint;
i integer=1;
BEGIN 
	-- check bookings_train_no_date(yyyy_mm_dd)
	EXECUTE FORMAT('select count(*) from %I','empty_'||lower(ct)||'_seats_'||trainID::text||'_'||to_char(day,'yyyy_mm_dd')) into count_empty;
	if count_empty < num_seats then
	raise exception '% seats not available', num_seats;
	else
	for empty_seats in EXECUTE FORMAT('select * from %I LIMIT %L','empty_'||lower(ct)||'_seats_'||trainID::text||'_'||to_char(day,'yyyy_mm_dd'),num_seats)
	loop
	if pnr IS NULL then
		pnr=generateUniquePNR(trainID,day,ct,empty_seats.coach_num,empty_seats.berth);
		EXECUTE FORMAT('CREATE TABLE %I (name varchar(256), age integer, gender char(1), trainID integer, coach char(5), berth integer);', 'ticket_' || pnr::text);
	END IF;
		EXECUTE FORMAT('INSERT INTO %I VALUES(%L, %L, %L, %L, %L, %L);', 'ticket_' || pnr::text, passenger_name[i], passenger_age[i], passenger_gender[i], trainID,ct||empty_seats.coach_num::text, empty_seats.berth);
		EXECUTE FORMAT ('DELETE FROM %I c where c.coach_num=%L AND c.berth=%L','empty_'||lower(ct)||'_seats_'||trainID::text||'_'||to_char(day,'yyyy_mm_dd'),empty_seats.coach_num, empty_seats.berth);
		i=i+1;
	end loop;
	end if;
	RETURN PNR;
END;
$$
language plpgsql;



CREATE OR REPLACE FUNCTION generateUniquePNR(trainID integer, day date, coach_type char(2), coach integer, berth integer)
RETURNS bigint
AS 
$$
DECLARE 
hashed_value text;
arr char(1)[32];
concated_string text;
mod bigint := 1610612741;
pnr bigint;
f char(1);
ascii_value bigint;
pow bigint;
BEGIN
	concated_string=trainID::text||to_char(day,'yyyy_mm_dd')||coach_type||coach::text||berth::text;
	select MD5 (concated_string) into hashed_value;
	SELECT string_to_array(hashed_value::text, NULL) into arr;
	pow=1;
	pnr=0;
	foreach f in ARRAY arr
	loop
	select * from ascii(f) into ascii_value;
	ascii_value=ascii_value*pow;
	pnr=pnr+ascii_value;
	pnr=pnr%mod;
	pow=pow*16;
	pow=pow%mod;
	end loop;
	return pnr;
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
