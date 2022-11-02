create table trains( train_id integer primary key, name varchar(256) not null);
create table available_trains(train_id integer, date date, num_AC integer not null, num_SL integer not null, primary key(train_id, date), foreign key(train_id) references trains(train_id));
create table coach(coach_type char(2), berth_num integer, berth_type char(2), primary key(coach_type, berth_num));



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
	num_Tickets integer,
	ct char(2),
	passenger_name varchar(256)[],
	passenger_age integer[],
	passenger_gender char(1)[]

)
RETURNS text
AS 
$$
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE
BEGIN 
	BEGIN TRANSACTION myTran;
	RETURN (SELECT current_setting('transaction_isolation'));
	COMMIT TRANSACTION myTran ;
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
