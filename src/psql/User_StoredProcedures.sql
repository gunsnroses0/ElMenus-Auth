--Sign Up--
CREATE OR REPLACE FUNCTION Add_User (_username varchar(100) = NULL, _password VARCHAR (100) = NULL, _user_type VARCHAR (100) = NULL, _salt VARCHAR(255)=NULL)
RETURNS VOID
AS
$BODY$
BEGIN
INSERT INTO Users(
  username,
  password,
  user_type,
  salt
)values(
  _username,
  _password,
  _user_type,
  _salt
);
END;
$BODY$
LANGUAGE 'plpgsql' VOLATILE;

--LOGIN--
CREATE OR REPLACE FUNCTION Login_User(_username varchar(100)=NULL, _password varchar(100)=NULL)
RETURNS refcursor AS
$BODY$
DECLARE
ref refcursor;
BEGIN
OPEN ref FOR
SELECT username, user_type FROM Users
WHERE username = _username
AND password = _password;
RETURN ref;
END;
$BODY$
LANGUAGE 'plpgsql' VOLATILE;

--GET USER--
CREATE OR REPLACE FUNCTION Salt(_username varchar(100) = NULL)
RETURNS refcursor AS
$BODY$
DECLARE
ref refcursor;
BEGIN
OPEN ref FOR SELECT salt FROM Users WHERE username = _username;
RETURN ref;
END;
$BODY$
LANGUAGE 'plpgsql' VOLATILE;
